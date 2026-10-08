/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
// @ts-check
import {expect, test} from '@playwright/test';
import {login} from './helpers/auth';
import {EamBuilder, nextActionResponse, openPage, recordedEvents, recordEvents} from './helpers/eam';

/**
 * Success and failure notifications of Event Action Mapping: every notification mode, the
 * configurable inline message (text, placeholders, CSS class, duration) and the display of
 * validation errors. Each scenario has its own page with a single button "#trigger".
 */

let builder;
let itemId;

const NOTIFICATION = '.structr-event-action-notification';

/** A page "notifications-<name>" with a button #trigger wired to the given mapping; `extend` adds more elements. */
async function addScenario(name, mapping, extend = async () => ({})) {

	const { pageId, bodyId } = await builder.page(`notifications-${name}`);
	const triggerId          = await builder.element(pageId, bodyId, 'Button', { _html_id: 'trigger', _html_type: 'button' });

	await builder.text(pageId, triggerId, 'Run');

	const extra = await extend(pageId, bodyId);
	const amId  = await builder.actionMapping(triggerId, { event: 'click', ...(typeof mapping === 'function' ? mapping(extra) : mapping) });

	return { pageId, bodyId, triggerId, amId, ...extra };
}

/** A div that starts out hidden, used as a custom dialog element. */
async function addDialog(pageId, bodyId, htmlId) {

	const dialogId = await builder.element(pageId, bodyId, 'Div', { _html_id: htmlId, _html_class: 'hidden' });

	await builder.text(pageId, dialogId, `Dialog ${htmlId}`);

	return dialogId;
}

/** Clicks the trigger and waits for the action's response. */
async function runAction(page, selector = '#trigger') {

	const response = nextActionResponse(page);

	await page.locator(selector).click();

	return await response;
}

test.beforeAll(async ({ playwright }) => {

	builder = await EamBuilder.create(playwright);

	await builder.schemaType('Item', [ { name: 'title', propertyType: 'String', notNull: true } ]);

	await builder.userFunction('notificationsOk',            "{ return 'ok'; }");
	await builder.userFunction('notificationsImport',        "{ return 'Imported 4 items'; }");
	await builder.userFunction('notificationsSpecial',       "{ return 'A & B'; }");
	await builder.userFunction('notificationsSummary',       "{ return { count: 3, label: '<b>bold</b>', nested: { name: 'Inner' } }; }");
	await builder.userFunction('notificationsReject',        "{ $.assert(false, 422, 'Quantity must be positive'); }");
	await builder.userFunction('notificationsRejectSpecial', "{ $.assert(false, 422, 'Quantity & price must be positive'); }");
	await builder.userFunction('notificationsCheckQuantity', "{ $.assert(Number($.arguments.quantity) > 0, 422, 'Quantity must be positive'); return 'quantity ' + $.arguments.quantity; }");

	// give the schema a moment to settle before objects of the new type are created
	await new Promise(resolve => setTimeout(resolve, 1000));

	itemId = await builder.node('Item', { title: 'Initial' });

	// none
	await addScenario('none', { action: 'method', method: 'notificationsOk', successNotifications: 'none', failureNotifications: 'none' });

	// system alert
	await addScenario('alert-success',      { action: 'method', method: 'notificationsOk', successNotifications: 'system-alert' });
	await addScenario('alert-failure',      { action: 'method', method: 'notificationsRejectSpecial', failureNotifications: 'system-alert' });
	await addScenario('alert-text',         { action: 'method', method: 'notificationsOk', successNotifications: 'system-alert', successNotificationsText: 'Saved ({status}) by ${me.name}' });
	await addScenario('alert-text-special', { action: 'method', method: 'notificationsSpecial', successNotifications: 'system-alert', successNotificationsText: 'Saved {result}' });

	// inline text message
	await addScenario('inline-default-success', { action: 'method', method: 'notificationsOk', successNotifications: 'inline-text-message', successNotificationsDelay: -1 });
	await addScenario('inline-default-failure', { action: 'method', method: 'notificationsReject', failureNotifications: 'inline-text-message', failureNotificationsDelay: -1 });

	await addScenario('inline-placeholders', {
		action:                    'method',
		method:                    'notificationsSummary',
		successNotifications:      'inline-text-message',
		successNotificationsDelay: -1,
		successNotificationsText:  '{status}: {result.count} items, {result.nested.name}, [{result.missing}], {result.label}'
	});

	await addScenario('inline-scalar-result', { action: 'method', method: 'notificationsImport', successNotifications: 'inline-text-message', successNotificationsDelay: -1, successNotificationsText: 'Import finished: {result}' });
	await addScenario('inline-template',      { action: 'method', method: 'notificationsOk', successNotifications: 'inline-text-message', successNotificationsDelay: -1, successNotificationsText: 'Done for ${me.name}' });
	await addScenario('inline-failure-text',  { action: 'method', method: 'notificationsReject', failureNotifications: 'inline-text-message', failureNotificationsDelay: -1, failureNotificationsText: 'Rejected ({status}): {message}' });
	await addScenario('inline-css-class',     { action: 'method', method: 'notificationsOk', successNotifications: 'inline-text-message', successNotificationsDelay: -1, successNotificationsCssClass: 'item-toast' });
	await addScenario('inline-delay',         { action: 'method', method: 'notificationsOk', successNotifications: 'inline-text-message', successNotificationsDelay: 1000 });
	await addScenario('inline-keep',          { action: 'method', method: 'notificationsOk', successNotifications: 'inline-text-message', successNotificationsDelay: -1 });

	// two triggers on one page: the second message replaces the first
	await addScenario('inline-replace', { action: 'method', method: 'notificationsOk', successNotifications: 'inline-text-message', successNotificationsDelay: -1, successNotificationsText: 'First' }, async (pageId, bodyId) => {

		const secondId = await builder.element(pageId, bodyId, 'Button', { _html_id: 'second-trigger', _html_type: 'button' });

		await builder.text(pageId, secondId, 'Run second');
		await builder.actionMapping(secondId, { event: 'click', action: 'method', method: 'notificationsOk', successNotifications: 'inline-text-message', successNotificationsDelay: -1, successNotificationsText: 'Second' });

		return { secondId };
	});

	// success and failure configured on the same mapping
	const both = await addScenario('both', {
		action:                    'method',
		method:                    'notificationsCheckQuantity',
		successNotifications:      'inline-text-message',
		successNotificationsDelay: -1,
		successNotificationsText:  'Accepted {result}',
		failureNotifications:      'inline-text-message',
		failureNotificationsDelay: -1,
		failureNotificationsText:  'Rejected: {message}'
	}, async (pageId, bodyId) => {

		const quantityId = await builder.element(pageId, bodyId, 'Input', { _html_id: 'quantity', _html_type: 'number' });

		return { quantityId };
	});

	await builder.parameter(both.amId, { parameterType: 'user-input', parameterName: 'quantity', inputElement: both.quantityId });

	// validation errors: the parameter is named like the property, which is how the failed input is found
	const validation = await addScenario('validation', {
		action:                    'update',
		idExpression:              itemId,
		failureNotifications:      'inline-text-message',
		failureNotificationsDelay: -1,
		successNotifications:      'inline-text-message',
		successNotificationsDelay: -1
	}, async (pageId, bodyId) => {

		const titleId = await builder.element(pageId, bodyId, 'Input', { _html_id: 'title', _html_type: 'text' });

		return { titleId };
	});

	await builder.parameter(validation.amId, { parameterType: 'user-input', parameterName: 'title', inputElement: validation.titleId });

	// custom dialogs
	await addScenario('dialog', { action: 'method', method: 'notificationsOk', successNotifications: 'custom-dialog', successNotificationsPartial: '#success-dialog, #second-dialog' }, async (pageId, bodyId) => {

		await addDialog(pageId, bodyId, 'success-dialog');
		await addDialog(pageId, bodyId, 'second-dialog');

		return {};
	});

	await addScenario('dialog-failure', {
		action:                      'method',
		method:                      'notificationsReject',
		successNotifications:        'custom-dialog',
		successNotificationsPartial: '#success-dialog',
		failureNotifications:        'custom-dialog',
		failureNotificationsPartial: '#failure-dialog'
	}, async (pageId, bodyId) => {

		await addDialog(pageId, bodyId, 'success-dialog');
		await addDialog(pageId, bodyId, 'failure-dialog');

		return {};
	});

	await addScenario('dialog-linked', (extra) => ({
		action:                      'method',
		method:                      'notificationsOk',
		successNotifications:        'custom-dialog-linked',
		successNotificationElements: [ extra.dialogId ]
	}), async (pageId, bodyId) => {

		const dialogId = await addDialog(pageId, bodyId, 'linked-dialog');

		return { dialogId };
	});

	// custom events
	await addScenario('event',         { action: 'method', method: 'notificationsOk', successNotifications: 'fire-event', successNotificationsEvent: 'item-saved' });
	await addScenario('event-failure', { action: 'method', method: 'notificationsReject', failureNotifications: 'fire-event', failureNotificationsEvent: 'item-rejected' });
});

test.describe('none', () => {

	// Clicks a trigger whose mapping has notifications set to 'none' and expects a 200 response,
	// no inline message and no browser dialog.
	test('shows neither a message nor an alert', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-none');

		let dialogs = 0;
		page.on('dialog', async (dialog) => {
			dialogs++;
			await dialog.accept();
		});

		const response = await runAction(page);

		expect(response.status()).toBe(200);
		expect(await response.json()).toMatchObject({ result: 'ok' });

		// give a notification the chance to appear before asserting that none did
		await page.waitForTimeout(500);

		await expect(page.locator(NOTIFICATION)).toHaveCount(0);
		expect(dialogs).toBe(0);
	});
});

test.describe('system alert', () => {

	// A successful method call with the 'system-alert' mode opens an alert with the default
	// success text and the status 200.
	test('shows the default success text with the status', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-alert-success');

		const dialog = page.waitForEvent('dialog');

		await page.locator('#trigger').click();

		const alert = await dialog;

		expect(alert.type()).toBe('alert');
		expect(alert.message()).toBe('✅ Operation successful (200)');

		await alert.accept();
	});

	// A method that fails with 422 and a message containing '&' opens an alert with the default
	// failure text, the status and the message as plain text.
	test('shows the default failure text with status and plain-text message', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-alert-failure');

		const dialog = page.waitForEvent('dialog');

		await page.locator('#trigger').click();

		const alert = await dialog;

		expect(alert.message()).toBe('❌ Operation failed (422: Quantity & price must be positive)');

		await alert.accept();
	});

	// An alert with a configured text: {status} is filled from the response and ${me.name}
	// was already replaced with the signed-in user's name when the page rendered.
	test('shows the configured text with {status} and the template expression evaluated at render time', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-alert-text');

		const dialog = page.waitForEvent('dialog');

		await page.locator('#trigger').click();

		const alert = await dialog;

		expect(alert.message()).toBe('✅ Saved (200) by admin');

		await alert.accept();
	});

	// An alert whose configured text contains {result}, for a method returning 'A & B',
	// shows the result unescaped.
	test('shows {result} as plain text', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-alert-text-special');

		const dialog = page.waitForEvent('dialog');

		await page.locator('#trigger').click();

		const alert = await dialog;

		expect(alert.message()).toBe('✅ Saved A & B');

		await alert.accept();
	});
});

test.describe('inline text message', () => {

	// A successful action with 'inline-text-message' inserts one green message with the default
	// success text directly after the trigger element.
	test('shows the default success text right after the trigger, in green', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-inline-default-success');

		await runAction(page);

		const message = page.locator(NOTIFICATION);

		await expect(message).toHaveCount(1);
		await expect(message).toHaveText('✅ Operation successful (200)');
		await expect(message).toHaveCSS('color', 'rgb(0, 128, 0)');

		// placed directly after the element that triggered the action
		expect(await page.locator('#trigger').evaluate(trigger => trigger.nextElementSibling?.classList.contains('structr-event-action-notification'))).toBe(true);
	});

	// A method rejected with 422 shows a red inline message with the default failure text,
	// the status and the server's message.
	test('shows the default failure text with status and message, in red', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-inline-default-failure');

		const response = await runAction(page);

		expect(response.status()).toBe(422);

		const message = page.locator(NOTIFICATION);

		await expect(message).toHaveText('❌ Operation failed (422: Quantity must be positive)');
		await expect(message).toHaveCSS('color', 'rgb(255, 0, 0)');
	});

	// A method returning an object: the configured text resolves {status} and nested {result.*} paths,
	// a missing path becomes empty, and markup in the result is shown as text, not rendered.
	test('fills {status} and {result.path} placeholders, leaves a missing path empty and escapes HTML in the result', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-inline-placeholders');

		await runAction(page);

		const message = page.locator(NOTIFICATION);

		await expect(message).toHaveText('✅ 200: 3 items, Inner, [], <b>bold</b>');

		// the markup from the result is shown as text, not rendered
		await expect(message.locator('b')).toHaveCount(0);
	});

	// A method returning a string: {result} in the configured text inserts that string.
	test('inserts a scalar method result with {result}', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-inline-scalar-result');

		await runAction(page);

		await expect(page.locator(NOTIFICATION)).toHaveText('✅ Import finished: Imported 4 items');
	});

	// A configured text with ${me.name}: the rendered attribute already contains the user's name
	// before any action, and the message shows it after the action.
	test('evaluates template expressions in the configured text when the page renders', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-inline-template');

		// already replaced in the rendered attribute, before any action ran
		await expect(page.locator('#trigger')).toHaveAttribute('data-structr-success-notifications-text', 'Done for admin');

		await runAction(page);

		await expect(page.locator(NOTIFICATION)).toHaveText('✅ Done for admin');
	});

	// A method rejected with 422 and a configured failure text: {status} and {message}
	// are filled from the error response.
	test('fills {status} and {message} in a configured failure text', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-inline-failure-text');

		await runAction(page);

		await expect(page.locator(NOTIFICATION)).toHaveText('❌ Rejected (422): Quantity must be positive');
	});

	// A configured CSS class is set on the inline message, and the built-in inline style is left out.
	test('a configured CSS class replaces the built-in styling', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-inline-css-class');

		await runAction(page);

		const message = page.locator(NOTIFICATION);

		await expect(message).toHaveClass(/(^|\s)item-toast(\s|$)/);
		await expect(message).not.toHaveAttribute('style');
	});

	// With a display duration of 1000 ms the inline message appears and is removed again
	// within a few seconds.
	test('disappears after the configured duration', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-inline-delay');

		await runAction(page);

		await expect(page.locator(NOTIFICATION)).toBeVisible();
		await expect(page.locator(NOTIFICATION)).toHaveCount(0, { timeout: 3000 });
	});

	// With a display duration of -1 the inline message is still visible after 6 seconds,
	// longer than the 5000 ms default.
	test('stays visible with a duration of -1', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-inline-keep');

		await runAction(page);

		// longer than the default duration of 5000 ms
		await page.waitForTimeout(6000);

		await expect(page.locator(NOTIFICATION)).toBeVisible();
	});

	// Two triggers on one page: the message of the second action replaces the first one,
	// so only one inline message remains.
	test('a second action replaces the previous message', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-inline-replace');

		await runAction(page, '#trigger');
		await expect(page.locator(NOTIFICATION)).toHaveText('✅ First');

		await runAction(page, '#second-trigger');

		await expect(page.locator(NOTIFICATION)).toHaveCount(1);
		await expect(page.locator(NOTIFICATION)).toHaveText('✅ Second');
	});

	// One mapping with both success and failure texts: a positive quantity shows the success text
	// with the result, a negative one makes the method fail and shows the failure text with the message.
	test('success and failure configured on the same mapping each show their own text', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-both');

		await page.locator('#quantity').fill('5');

		const success = await runAction(page);

		expect(success.status()).toBe(200);
		await expect(page.locator(NOTIFICATION)).toHaveText('✅ Accepted quantity 5');

		await page.locator('#quantity').fill('-1');

		const failure = await runAction(page);

		expect(failure.status()).toBe(422);
		await expect(page.locator(NOTIFICATION)).toHaveCount(1);
		await expect(page.locator(NOTIFICATION)).toHaveText('❌ Rejected: Quantity must be positive');
	});
});

test.describe('validation errors', () => {

	// An update that empties the required title fails with 422: the inline message lists the validation
	// error, the mapped input gets a red border and data-error, and the stored title is unchanged.
	test('are listed in the failure message and mark the mapped input', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-validation');

		await page.locator('#title').fill('');

		const response = await runAction(page);

		expect(response.status()).toBe(422);

		const body = await response.json();

		expect(body.errors).toEqual(expect.arrayContaining([ expect.objectContaining({ property: 'title', token: 'must_not_be_empty' }) ]));

		const message = page.locator(NOTIFICATION);

		await expect(message).toContainText('❌ Operation failed (422: Unable to commit transaction, validation failed)');
		await expect(message).toContainText('title must not be empty');

		const title = page.locator('#title');

		await expect(title).toHaveAttribute('data-error', 'must_not_be_empty');
		expect(await title.evaluate(input => input.style.borderColor)).toBe('red');

		// the invalid value was not stored
		expect((await builder.get('Item', itemId)).title).not.toBeNull();
	});

	// After a failed update, a valid title saves successfully and the red border of the input
	// is reset; the new title is stored.
	test('a following success removes the red border', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-validation');

		const title = page.locator('#title');

		await title.fill('');
		await runAction(page);
		await expect(title).toHaveAttribute('data-error', 'must_not_be_empty');

		await title.fill('Corrected');

		const response = await runAction(page);

		expect(response.status()).toBe(200);
		expect(await title.evaluate(input => input.style.borderColor)).toBe('');
		await expect.poll(async () => (await builder.get('Item', itemId)).title).toBe('Corrected');
	});

	// After a failed update, a valid title saves successfully and the input no longer
	// carries the data-error attribute.
	test('a following success removes the data-error attribute', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-validation');

		const title = page.locator('#title');

		await title.fill('');
		await runAction(page);
		await expect(title).toHaveAttribute('data-error', 'must_not_be_empty');

		await title.fill('Corrected again');
		await runAction(page);

		await expect(title).not.toHaveAttribute('data-error');
	});
});

test.describe('custom dialog', () => {

	// A 'custom-dialog' notification with two selectors shows both hidden dialog elements
	// after the action and hides them again after 5 seconds.
	test('shows every element of the selector list and hides it again after 5 seconds', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-dialog');

		await expect(page.locator('#success-dialog')).toBeHidden();
		await expect(page.locator('#second-dialog')).toBeHidden();

		await runAction(page);

		await expect(page.locator('#success-dialog')).toBeVisible();
		await expect(page.locator('#second-dialog')).toBeVisible();

		await expect(page.locator('#success-dialog')).toBeHidden({ timeout: 8000 });
		await expect(page.locator('#second-dialog')).toBeHidden({ timeout: 8000 });
	});

	// A failing action with custom dialogs for both outcomes shows only the failure dialog element.
	test('on failure shows the failure element, not the success element', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-dialog-failure');

		const response = await runAction(page);

		expect(response.status()).toBe(422);

		await expect(page.locator('#failure-dialog')).toBeVisible();
		await expect(page.locator('#success-dialog')).toBeHidden();
	});

	// A 'custom-dialog-linked' notification shows the linked dialog element after the action
	// and hides it again after 5 seconds.
	test('linked elements are shown and hidden again after 5 seconds', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-dialog-linked');

		const dialog = page.locator('#linked-dialog');

		await expect(dialog).toBeHidden();

		await runAction(page);

		await expect(dialog).toBeVisible();
		await expect(dialog).toBeHidden({ timeout: 8000 });
	});
});

test.describe('custom event', () => {

	// A 'fire-event' notification dispatches the configured event, which bubbles up to the document
	// and carries the status 200 and the trigger element in its detail.
	test('is dispatched with the configured name, bubbles and carries status and element', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-event');

		// recorded on the document, so only a bubbling event arrives there
		await recordEvents(page, [ 'item-saved' ]);

		await runAction(page);

		await expect.poll(async () => (await recordedEvents(page)).length).toBe(1);

		const [ event ] = await recordedEvents(page);

		expect(event.type).toBe('item-saved');
		expect(event.target).toBe('trigger');
		expect(event.detail.status).toBe(200);
		expect(event.detail.element).toBe('trigger');
	});

	// The 'fire-event' notification's detail.result holds the method's return value 'ok'.
	test('carries the action result in detail.result', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-event');

		await recordEvents(page, [ 'item-saved' ]);

		await runAction(page);

		await expect.poll(async () => (await recordedEvents(page)).length).toBe(1);

		const [ event ] = await recordedEvents(page);

		expect(event.detail.result).toBe('ok');
	});

	// A failing action dispatches the configured failure event with status 422 and the trigger
	// element in its detail.
	test('on failure is dispatched with the failure event name and the error status', async ({ page }) => {

		await login(page);
		await openPage(page, 'notifications-event-failure');

		await recordEvents(page, [ 'item-rejected' ]);

		await runAction(page);

		await expect.poll(async () => (await recordedEvents(page)).length).toBe(1);

		const [ event ] = await recordedEvents(page);

		expect(event.type).toBe('item-rejected');
		expect(event.detail.status).toBe(422);
		expect(event.detail.element).toBe('trigger');
	});
});
