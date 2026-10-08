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
import {EamBuilder, nextActionRequest, nextActionResponse, openPage} from './helpers/eam';

/**
 * Event Action Mapping and security: what an action may do on behalf of a visitor.
 *
 * - permissions: an action runs with the visitor's rights, so an anonymous visitor or a signed-in user
 *   without write rights cannot change or delete an object through it, and an object they cannot see
 *   is not touched at all
 * - escaping: texts and values configured in a mapping, and user data rendered into it, arrive in the
 *   browser and in the database exactly as they were written, without breaking the markup
 * - forged requests: the request a visitor sends can be changed before it leaves the browser, so the
 *   server takes the action, the type and the method from the mapping; the target is evaluated when the
 *   page renders and comes from the request, so the permissions of the object are the boundary there
 *
 * All pages are public. Anonymous tests run without a session; the signed-in user authenticates every
 * request with X-User/X-Password headers, so no sign-in page is needed.
 */

let builder;
let grantId;

const ids = {};

// anonymous visitors need NON_AUTH_USER_POST (64) to send an action, signed-in users AUTH_USER_POST (4)
const EVENT_SIGNATURE    = 'DOMElement/_id/event';
const AUTH_USER_POST     = 4;
const NON_AUTH_USER_POST = 64;

const member = { name: 'security-member', password: 'security-member-password-1' };

// values that break an HTML attribute or inject markup if they are not escaped, plus text that must survive unchanged
const QUOTES_AND_MARKUP = `Quote " apostrophe ' & ampersand &amp; entity <script>window.__pwned = 'script';</script> <b>bold</b>`;
const UNICODE_TEXT      = 'Grüße 😀 中文 עברית Ω\nsecond line\twith tab';
const ATTRIBUTE_BREAKER = `" data-injected="yes" onmouseover="window.__pwned = 'attribute'`;
const MARKUP_NAME       = `<img src="x" onerror="window.__pwned = 'notification'">`;

/** A public page holding one button that triggers the given action mapping. */
async function buttonPage(name, buttonId, mapping, parameters = []) {

	const { pageId, bodyId } = await builder.page(name, { publicVisible: true });
	const button             = await builder.element(pageId, bodyId, 'Button', { _html_id: buttonId });

	await builder.text(pageId, button, buttonId);

	const actionMapping = await builder.actionMapping(button, { event: 'click', ...mapping });

	for (const parameter of parameters) {
		await builder.parameter(actionMapping, parameter);
	}

	return { pageId, bodyId, button, actionMapping };
}

/** A constant-value parameter. */
function constant(name, value) {

	return { parameterType: 'constant-value', parameterName: name, constantValue: value };
}

/** Clicks the element and returns the response to the action request it sends. */
async function clickAndWait(page, selector) {

	const response = nextActionResponse(page);

	await page.locator(selector).click();

	return await response;
}

/** Authenticates every request of this browser context as the given user. */
async function actAs(page, user) {

	await page.context().setExtraHTTPHeaders({ 'X-User': user.name, 'X-Password': user.password });
}

/** Changes the next action request before it leaves the browser: the given keys replace or extend the payload frontend.js built. */
async function forgeNextAction(page, changes) {

	await page.route('**/event', async route => {

		const payload = JSON.parse(route.request().postData() ?? '{}');

		await route.continue({ postData: JSON.stringify({ ...payload, ...changes }) });

	}, { times: 1 });
}

/** Whatever an injected script, attribute handler or image error handler wrote into window.__pwned. */
async function pwned(page) {

	return await page.evaluate(() => window['__pwned']);
}

test.beforeAll(async ({ playwright }) => {

	builder = await EamBuilder.create(playwright);

	grantId = await builder.node('ResourceAccess', {
		signature:                   EVENT_SIGNATURE,
		flags:                       AUTH_USER_POST | NON_AUTH_USER_POST,
		visibleToPublicUsers:        true,
		visibleToAuthenticatedUsers: true
	});

	ids.member = await builder.node('User', member);

	await builder.schemaType('Item', [
		{ name: 'note',     propertyType: 'String' },
		{ name: 'priority', propertyType: 'Integer' }
	], [
		{ name: 'describe', source: "{ return 'Item ' + $.this.name; }" }
	]);

	await builder.userFunction('publicGreeting', "{ return 'public greeting'; }");
	await builder.userFunction('privateSecret',  "{ return 'private secret value'; }", { isPrivate: true });

	// give the schema a moment to settle before the pages refer to the new types
	await new Promise(resolve => setTimeout(resolve, 1000));

	// objects for the anonymous visitor: one everybody can read, one nobody but the admin can see
	ids.publicItem    = await builder.node('Item', { name: 'Public item',    visibleToPublicUsers: true, visibleToAuthenticatedUsers: true });
	ids.invisibleItem = await builder.node('Item', { name: 'Invisible item' });

	// objects for the signed-in member: one of its own, one it can only read, one it cannot see
	ids.ownItem       = await builder.node('Item', { name: 'Own item',       owner: ids.member });
	ids.otherOwnItem  = await builder.node('Item', { name: 'Other own item', owner: ids.member });
	ids.readableItem  = await builder.node('Item', { name: 'Readable item',  visibleToAuthenticatedUsers: true });
	ids.hiddenItem    = await builder.node('Item', { name: 'Hidden item' });

	// permissions: update, delete and an instance method for each kind of object
	for (const [key, itemId] of Object.entries({ public: ids.publicItem, invisible: ids.invisibleItem, own: ids.ownItem, readable: ids.readableItem, hidden: ids.hiddenItem })) {

		await buttonPage(`security-update-${key}`, 'update', { action: 'update', idExpression: itemId }, [ constant('name', 'Changed by visitor') ]);
		await buttonPage(`security-delete-${key}`, 'delete', { action: 'delete', idExpression: itemId });
		await buttonPage(`security-method-${key}`, 'method', { action: 'method', idExpression: itemId, method: 'describe' });
	}

	// forged requests: the mappings configure one object, one type and one method
	await buttonPage('security-forged-update', 'update', { action: 'update', idExpression: ids.otherOwnItem }, [ constant('priority', '7') ]);
	await buttonPage('security-forged-create', 'create', { action: 'create', dataType: 'Item' }, [ constant('name', 'Created by visitor') ]);
	await buttonPage('security-forged-method', 'method', { action: 'method', method: 'publicGreeting' });

	// a method name that is a template expression naming the private and the public function, which must not be evaluated
	await buttonPage('security-expression-private', 'method', { action: 'method', method: "${'privateSecret'}" });
	await buttonPage('security-expression-public',  'method', { action: 'method', method: "${'publicGreeting'}" });

	// escaping: a confirmation dialog with title and text that contain quotes, markup and line breaks
	await buttonPage('security-dialog', 'remove', {
		action:       'delete',
		idExpression: ids.invisibleItem,
		dialogType:   'okcancel',
		dialogTitle:  QUOTES_AND_MARKUP,
		dialogText:   ATTRIBUTE_BREAKER + '\n' + UNICODE_TEXT
	});

	// escaping: follow-up selectors and custom dialog selectors that try to close the attribute
	await buttonPage('security-selectors', 'save', {
		action:                          'method',
		method:                          'publicGreeting',
		successBehaviour:                'partial-refresh',
		successPartial:                  '#panel' + ATTRIBUTE_BREAKER,
		failureBehaviour:                'partial-refresh',
		failurePartial:                  '#panel' + ATTRIBUTE_BREAKER,
		successNotifications:            'custom-dialog',
		successNotificationsPartial:     '#dialog' + ATTRIBUTE_BREAKER,
		failureNotifications:            'custom-dialog',
		failureNotificationsPartial:     '#dialog' + ATTRIBUTE_BREAKER
	});

	// escaping: constant values with quotes, markup, unicode and line breaks
	ids.constantsItem = await builder.node('Item', { name: 'Constants item' });

	await buttonPage('security-constants', 'save', { action: 'update', idExpression: ids.constantsItem }, [
		constant('name', QUOTES_AND_MARKUP),
		constant('note', UNICODE_TEXT)
	]);

	// escaping: user input from a text input and a textarea
	{
		ids.inputItem = await builder.node('Item', { name: 'Input item' });

		const { pageId, bodyId, actionMapping } = await buttonPage('security-input', 'save', { action: 'update', idExpression: ids.inputItem });

		const nameId = await builder.element(pageId, bodyId, 'Input', { _html_id: 'name-input', _html_type: 'text' });
		const noteId = await builder.element(pageId, bodyId, 'Textarea', { _html_id: 'note-input' });

		await builder.parameter(actionMapping, { parameterType: 'user-input', parameterName: 'name', inputElement: nameId });
		await builder.parameter(actionMapping, { parameterType: 'user-input', parameterName: 'note', inputElement: noteId });
	}

	// escaping: an inline notification whose text contains user data, evaluated when the page renders
	ids.markupItem = await builder.node('Item', { name: MARKUP_NAME, visibleToPublicUsers: true, visibleToAuthenticatedUsers: true });

	await buttonPage('security-notification-markup', 'save', {
		action:                    'method',
		method:                    'publicGreeting',
		successNotifications:      'inline-text-message',
		successNotificationsText:  'Saved ${current.name}',
		successNotificationsDelay: -1
	});
});

test.describe('anonymous visitor', () => {

	// An anonymous visitor clicks an update button for an Item everybody may read but nobody may write:
	// the server answers 403 and the name stays unchanged.
	test('cannot update an object it can only read', async ({ page }) => {

		await openPage(page, 'security-update-public');

		const response = await clickAndWait(page, '#update');

		expect(response.status()).toBe(403);
		expect((await builder.get('Item', ids.publicItem)).name).toBe('Public item');
	});

	// An anonymous visitor clicks a delete button for an Item everybody may read: the server answers 403
	// and the Item still exists.
	test('cannot delete an object it can only read', async ({ page }) => {

		await openPage(page, 'security-delete-public');

		const response = await clickAndWait(page, '#delete');

		expect(response.status()).toBe(403);
		expect(await builder.get('Item', ids.publicItem)).not.toBeNull();
	});

	// An anonymous visitor clicks an update button for an Item it cannot see: the server finds no target,
	// answers 200 like for a uuid without an object (so nothing about the Item leaks) and changes nothing.
	test('cannot update an object it cannot see, and learns nothing about it', async ({ page }) => {

		await openPage(page, 'security-update-invisible');

		const response = await clickAndWait(page, '#update');

		expect(response.status()).toBe(200);
		expect((await builder.get('Item', ids.invisibleItem)).name).toBe('Invisible item');
	});

	// An anonymous visitor clicks a delete button for an Item it cannot see: 200 as for a missing uuid,
	// and the Item still exists.
	test('cannot delete an object it cannot see', async ({ page }) => {

		await openPage(page, 'security-delete-invisible');

		const response = await clickAndWait(page, '#delete');

		expect(response.status()).toBe(200);
		expect(await builder.get('Item', ids.invisibleItem)).not.toBeNull();
	});

	// An anonymous visitor calls the instance method describe on an Item it cannot see: the method does not
	// run, so the response does not contain the Item's name.
	test('cannot call an instance method on an object it cannot see', async ({ page }) => {

		await openPage(page, 'security-method-invisible');

		const response = await clickAndWait(page, '#method');

		expect(response.status()).toBe(200);
		expect(await response.text()).not.toContain('Invisible item');
	});

	// The same instance method on the Item everybody may read runs and returns its name, which shows that
	// the previous test is about visibility and not about a broken mapping.
	test('can call an instance method on an object it can read', async ({ page }) => {

		await openPage(page, 'security-method-public');

		const response = await clickAndWait(page, '#method');

		expect(response.status()).toBe(200);
		expect((await response.json()).result).toBe('Item Public item');
	});
});

test.describe('signed-in user without write rights', () => {

	// The member updates an Item it owns: 200 and the new name is stored, which shows that the header
	// authentication works and the following denials are about rights.
	test('can update an object it owns', async ({ page }) => {

		await actAs(page, member);
		await openPage(page, 'security-update-own');

		const response = await clickAndWait(page, '#update');

		expect(response.status()).toBe(200);
		expect((await builder.get('Item', ids.ownItem)).name).toBe('Changed by visitor');
	});

	// The member updates an Item that signed-in users may read but not write: 403 and the name stays.
	test('cannot update an object it can only read', async ({ page }) => {

		await actAs(page, member);
		await openPage(page, 'security-update-readable');

		const response = await clickAndWait(page, '#update');

		expect(response.status()).toBe(403);
		expect((await builder.get('Item', ids.readableItem)).name).toBe('Readable item');
	});

	// The member deletes an Item it may only read: 403 and the Item still exists.
	test('cannot delete an object it can only read', async ({ page }) => {

		await actAs(page, member);
		await openPage(page, 'security-delete-readable');

		const response = await clickAndWait(page, '#delete');

		expect(response.status()).toBe(403);
		expect(await builder.get('Item', ids.readableItem)).not.toBeNull();
	});

	// The member updates and deletes an Item it cannot see: both answer 200 as for a missing uuid,
	// and the Item is neither changed nor deleted.
	test('cannot update or delete an object it cannot see', async ({ page }) => {

		await actAs(page, member);

		await openPage(page, 'security-update-hidden');
		expect((await clickAndWait(page, '#update')).status()).toBe(200);

		await openPage(page, 'security-delete-hidden');
		expect((await clickAndWait(page, '#delete')).status()).toBe(200);

		const item = await builder.get('Item', ids.hiddenItem);

		expect(item).not.toBeNull();
		expect(item.name).toBe('Hidden item');
	});

	// The event grant is reduced to anonymous visitors only, then the member updates its own Item:
	// the request is rejected with 401, nothing is stored, and the grant is restored afterwards.
	test('without a resource access grant for signed-in users the action is rejected with 401', async ({ page }) => {

		const before = (await builder.get('Item', ids.ownItem)).name;

		await builder.update('ResourceAccess', grantId, { flags: NON_AUTH_USER_POST });

		try {

			await actAs(page, member);
			await openPage(page, 'security-update-own');

			// a known name, so a write that gets through would show
			await builder.update('Item', ids.ownItem, { name: 'Before denied update' });

			const response = await clickAndWait(page, '#update');

			expect(response.status()).toBe(401);
			expect((await builder.get('Item', ids.ownItem)).name).toBe('Before denied update');

		} finally {

			await builder.update('ResourceAccess', grantId, { flags: AUTH_USER_POST | NON_AUTH_USER_POST });
			await builder.update('Item', ids.ownItem, { name: before });
		}
	});
});

test.describe('escaping', () => {

	// A confirmation dialog whose title and text contain quotes, markup, an attribute breaker, unicode and a
	// line break: the confirm box shows them exactly, no attribute is injected into the button and no script runs.
	test('dialog title and text are shown exactly and do not break the trigger element', async ({ page }) => {

		await login(page);
		await openPage(page, 'security-dialog');

		const button = page.locator('#remove');

		await expect(button).not.toHaveAttribute('data-injected');
		await expect(button).not.toHaveAttribute('onmouseover');

		// confirm() blocks the click until the dialog is handled, so the handler has to dismiss it
		const messages = [];

		page.once('dialog', async confirm => {
			messages.push(confirm.message());
			await confirm.dismiss();
		});

		await button.click();

		expect(messages).toEqual([ QUOTES_AND_MARKUP + '\n\n' + ATTRIBUTE_BREAKER + '\n' + UNICODE_TEXT ]);

		await button.hover();

		expect(await pwned(page)).toBeUndefined();
	});

	// Follow-up and custom dialog selectors that try to close their attribute: the button gets no injected
	// attribute, and each selector is kept as one attribute value.
	test('follow-up and dialog selectors cannot inject attributes into the trigger element', async ({ page }) => {

		await login(page);
		await openPage(page, 'security-selectors');

		const button = page.locator('#save');

		await expect(button).not.toHaveAttribute('data-injected');
		await expect(button).not.toHaveAttribute('onmouseover');

		await expect(button).toHaveAttribute('data-structr-success-target', '#panel' + ATTRIBUTE_BREAKER);
		await expect(button).toHaveAttribute('data-structr-failure-target', '#panel' + ATTRIBUTE_BREAKER);
		await expect(button).toHaveAttribute('data-structr-success-notifications-partial', '#dialog' + ATTRIBUTE_BREAKER);
		await expect(button).toHaveAttribute('data-structr-failure-notifications-partial', '#dialog' + ATTRIBUTE_BREAKER);

		await button.hover();

		expect(await pwned(page)).toBeUndefined();
	});

	// Constant values with quotes, markup, entities, unicode, emoji, a line break and a tab are sent and
	// stored byte for byte, and the script in one of them never runs.
	test('constant values arrive in the payload and in the database exactly as configured', async ({ page }) => {

		await login(page);
		await openPage(page, 'security-constants');

		const request  = nextActionRequest(page);
		const response = await clickAndWait(page, '#save');
		const payload  = (await request).postDataJSON();

		expect(payload.name).toBe(QUOTES_AND_MARKUP);
		expect(payload.note).toBe(UNICODE_TEXT);

		expect(response.status()).toBe(200);

		const item = await builder.get('Item', ids.constantsItem);

		expect(item.name).toBe(QUOTES_AND_MARKUP);
		expect(item.note).toBe(UNICODE_TEXT);

		expect(await pwned(page)).toBeUndefined();
	});

	// User input with quotes and markup in a text input and unicode with a line break in a textarea is sent
	// and stored byte for byte.
	test('user input arrives in the payload and in the database exactly as typed', async ({ page }) => {

		await login(page);
		await openPage(page, 'security-input');

		await page.locator('#name-input').fill(QUOTES_AND_MARKUP);
		await page.locator('#note-input').fill(UNICODE_TEXT);

		const request  = nextActionRequest(page);
		const response = await clickAndWait(page, '#save');
		const payload  = (await request).postDataJSON();

		expect(payload.name).toBe(QUOTES_AND_MARKUP);
		expect(payload.note).toBe(UNICODE_TEXT);

		expect(response.status()).toBe(200);

		const item = await builder.get('Item', ids.inputItem);

		expect(item.name).toBe(QUOTES_AND_MARKUP);
		expect(item.note).toBe(UNICODE_TEXT);
	});

	// An inline notification text with ${current.name}, for an Item whose name is an image tag with an error
	// handler: the message shows the name as text, contains no element, and the handler never runs.
	test('user data in a render-time notification text is shown as text, not as markup', async ({ page }) => {

		await login(page);
		await openPage(page, `security-notification-markup/${ids.markupItem}`);

		await clickAndWait(page, '#save');

		const notification = page.locator('.structr-event-action-notification');

		await expect(notification).toHaveCount(1);
		await page.waitForTimeout(500);

		expect(await pwned(page)).toBeUndefined();
		expect(await notification.locator('img').count()).toBe(0);
		await expect(notification).toContainText(MARKUP_NAME);
	});
});

test.describe('forged requests', () => {

	// The member's update button is configured for one of its Items; the request is changed to name its other Item.
	// The target is evaluated when the page renders (${current.id}, a repeater row) and therefore comes from the
	// request: the permissions of the object are the boundary, and the member may write both of its Items.
	test('a changed target in the request can address another object the visitor may write', async ({ page }) => {

		await actAs(page, member);
		await openPage(page, 'security-forged-update');

		await forgeNextAction(page, { structrIdExpression: ids.ownItem });

		const response = await clickAndWait(page, '#update');

		expect(response.status()).toBe(200);
		expect((await builder.get('Item', ids.ownItem)).priority).toBe(7);
	});

	// The same button, with the request changed to name an Item the member can only read: the permissions of
	// that object reject the update with 403, and the Item stays unchanged.
	test('a changed target in the request cannot address an object the visitor may only read', async ({ page }) => {

		await actAs(page, member);
		await openPage(page, 'security-forged-update');

		await forgeNextAction(page, { structrIdExpression: ids.readableItem });

		const response = await clickAndWait(page, '#update');

		expect(response.status()).toBe(403);
		expect((await builder.get('Item', ids.readableItem)).priority ?? null).toBeNull();
	});

	// The member's update button is changed to request a delete action: the server takes the action from the
	// mapping, so the configured Item is updated and not deleted.
	test('a changed action in the request does not change what the server does', async ({ page }) => {

		await actAs(page, member);
		await openPage(page, 'security-forged-update');

		await forgeNextAction(page, { structrAction: 'delete' });

		const response = await clickAndWait(page, '#update');

		expect(response.status()).toBe(200);

		const item = await builder.get('Item', ids.otherOwnItem);

		expect(item).not.toBeNull();
		expect(item.priority).toBe(7);
	});

	// An anonymous visitor's create button is configured for Item; the request is changed to ask for a User with
	// a password. The server must create the configured type, so no User with that name exists afterwards.
	test('a changed type in the request does not create another type', async ({ page }) => {

		await openPage(page, 'security-forged-create');

		await forgeNextAction(page, { structrDataType: 'User', name: 'forged-user', password: 'forged-password-1' });
		await clickAndWait(page, '#create');

		expect(await builder.find('User', { name: 'forged-user' })).toHaveLength(0);
	});

	// An anonymous visitor's method button is configured for publicGreeting; the request is changed to call the
	// private function privateSecret. The server runs the configured function, so the private value is not in the response.
	test('a changed method name in the request does not call another, private function', async ({ page }) => {

		await openPage(page, 'security-forged-method');

		await forgeNextAction(page, { structrMethod: 'privateSecret' });

		const response = await clickAndWait(page, '#method');

		expect(response.status()).toBe(200);

		const body = await response.text();

		expect(body).toContain('public greeting');
		expect(body).not.toContain('private secret value');
	});

	// The method of an action mapping is a static name: a template expression in it is not evaluated, so the button
	// whose method is ${'privateSecret'} and the one whose method is ${'publicGreeting'} both name no function (422).
	test('a method name is never evaluated as a template expression', async ({ page }) => {

		await openPage(page, 'security-expression-private');

		const privateResponse = await clickAndWait(page, '#method');

		expect(privateResponse.status()).toBe(422);
		expect(await privateResponse.text()).not.toContain('private secret value');

		await openPage(page, 'security-expression-public');

		const publicResponse = await clickAndWait(page, '#method');

		expect(publicResponse.status()).toBe(422);
		expect(await publicResponse.text()).not.toContain('public greeting');
	});
});
