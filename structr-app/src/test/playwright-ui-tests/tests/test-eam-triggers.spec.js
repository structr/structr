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
import {EamBuilder, nextActionRequest, nextActionResponse, nextPartialReload, openPage, recordEvents, recordedEvents} from './helpers/eam';

/**
 * Event Action Mapping: what triggers an action and how the options shape it.
 *
 * Covers the DOM events, the confirmation dialog, HTML5 validation before sending, the options
 * (delay, preventDefault, stopPropagation, resetValue) and the pagination actions. Every scenario
 * has its own page; most of them update an Item, so the database shows whether the action ran.
 */

// time to wait before asserting that something did NOT happen
const QUIET_PERIOD = 1500;

let builder;

// per page name: the uuids the tests need (trigger element, items)
const scenarios = {};

/** Records the action requests sent for one trigger element, so a test can count them. */
function recordActionRequests(page, elementId) {

	const requests = [];

	page.on('request', request => {

		if (request.method() === 'POST' && request.url().endsWith(`/structr/rest/DOMElement/${elementId}/event`)) {
			requests.push(request);
		}
	});

	return requests;
}

async function itemName(itemId) {

	return (await builder.get('Item', itemId))?.name;
}

/** A page with one input (name "name") whose action updates an Item with the input's value. */
async function inputUpdatingItem(pageName, event, inputData = {}, mappingData = {}) {

	const { pageId, bodyId } = await builder.page(pageName);

	const itemId    = await builder.node('Item', { name: 'unchanged' });
	const triggerId = await builder.element(pageId, bodyId, 'Input', { _html_id: 'field', _html_name: 'name', ...inputData });

	await builder.actionMapping(triggerId, { event: event, action: 'update', idExpression: itemId, ...mappingData });

	scenarios[pageName] = { itemId, triggerId };
}

/** A page with one button whose action sets the name of an Item to "triggered". */
async function buttonUpdatingItem(pageName, event, mappingData = {}) {

	const { pageId, bodyId } = await builder.page(pageName);

	const itemId    = await builder.node('Item', { name: 'unchanged' });
	const triggerId = await builder.element(pageId, bodyId, 'Button', { _html_id: 'trigger' });

	await builder.text(pageId, triggerId, 'Run');

	const mappingId = await builder.actionMapping(triggerId, { event: event, action: 'update', idExpression: itemId, ...mappingData });

	await builder.parameter(mappingId, { parameterType: 'constant-value', parameterName: 'name', constantValue: 'triggered' });

	scenarios[pageName] = { pageId, bodyId, itemId, triggerId };
}

/**
 * A paged list of Entries with both pagination buttons INSIDE the refreshed section: the server
 * renders the target page number of each button from the current request, so only a re-rendered
 * pager can advance beyond the next page.
 */
async function buildPagination() {

	for (const name of [ 'Entry A', 'Entry B', 'Entry C', 'Entry D', 'Entry E' ]) {
		await builder.node('Entry', { name: name });
	}

	const { pageId, bodyId } = await builder.page('triggers-pagination');

	const pagerId = await builder.element(pageId, bodyId, 'Div', { _html_id: 'entry-pager' });
	const listId  = await builder.element(pageId, pagerId, 'Ul', { _html_id: 'entry-list' });
	const entryId = await builder.element(pageId, listId, 'Li', { functionQuery: "find('Entry', sort('name'), page(request.page!1, 2))", dataKey: 'entry' });

	await builder.text(pageId, entryId, '${entry.name}');

	for (const action of [ 'prev-page', 'next-page' ]) {

		const buttonId  = await builder.element(pageId, pagerId, 'Button', { _html_id: action });
		const mappingId = await builder.actionMapping(buttonId, { event: 'click', action: action, successBehaviour: 'partial-refresh', successPartial: '#entry-pager' });

		await builder.text(pageId, buttonId, action);
		await builder.parameter(mappingId, { parameterType: 'page-param', parameterName: 'page' });
	}
}

test.beforeAll(async ({ playwright }) => {

	builder = await EamBuilder.create(playwright);

	await builder.schemaType('Item', [ { name: 'done', propertyType: 'Boolean' } ]);
	await builder.schemaType('Entry');

	// DOM events
	await buttonUpdatingItem('triggers-click', 'click');
	await inputUpdatingItem('triggers-change', 'change');
	await inputUpdatingItem('triggers-input', 'input');
	await inputUpdatingItem('triggers-keyup', 'keyup');
	await inputUpdatingItem('triggers-keydown', 'keydown', {}, { options: '{"preventDefault": false}' });
	await inputUpdatingItem('triggers-focusout', 'focusout');
	await buttonUpdatingItem('triggers-several-events', 'click,mouseover');
	await buttonUpdatingItem('triggers-several-events-spaced', 'click, mouseover');

	// submit: the form carries the mapping, the fields come from the form data
	{
		const { pageId, bodyId } = await builder.page('triggers-submit');

		const itemId   = await builder.node('Item', { name: 'unchanged' });
		const formId   = await builder.element(pageId, bodyId, 'Form', { _html_id: 'form' });
		const buttonId = await builder.element(pageId, formId, 'Button', { _html_id: 'submit', _html_type: 'submit' });

		await builder.element(pageId, formId, 'Input', { _html_id: 'field', _html_name: 'name' });
		await builder.text(pageId, buttonId, 'Save');
		await builder.actionMapping(formId, { event: 'submit', action: 'update', idExpression: itemId });

		scenarios['triggers-submit'] = { itemId, triggerId: formId };
	}

	// load: fires when the events are bound; the reload button refreshes an unrelated section
	{
		const { pageId, bodyId } = await builder.page('triggers-load');

		const itemId    = await builder.node('Item', { name: 'unchanged' });
		const probeId   = await builder.element(pageId, bodyId, 'Div', { _html_id: 'load-probe' });
		const probeMap  = await builder.actionMapping(probeId, { event: 'load', action: 'update', idExpression: itemId });
		const areaId    = await builder.element(pageId, bodyId, 'Div', { _html_id: 'reload-area' });
		const reloadId  = await builder.element(pageId, bodyId, 'Button', { _html_id: 'reload' });

		await builder.parameter(probeMap, { parameterType: 'constant-value', parameterName: 'name', constantValue: 'loaded' });
		await builder.text(pageId, areaId, 'Reloaded section');
		await builder.text(pageId, reloadId, 'Reload');
		await builder.actionMapping(reloadId, { event: 'click', action: 'none', successBehaviour: 'partial-refresh', successPartial: '#reload-area' });

		scenarios['triggers-load'] = { itemId, triggerId: probeId };
	}

	// two action mappings on one element
	{
		await buttonUpdatingItem('triggers-two-mappings', 'click');

		const scenario     = scenarios['triggers-two-mappings'];
		const secondItemId = await builder.node('Item', { name: 'unchanged' });
		const secondMap    = await builder.actionMapping(scenario.triggerId, { event: 'click', action: 'update', idExpression: secondItemId });

		await builder.parameter(secondMap, { parameterType: 'constant-value', parameterName: 'name', constantValue: 'triggered' });

		scenario.secondItemId = secondItemId;
	}

	// confirmation dialog: deleting an Item is only allowed after confirming
	{
		const { pageId, bodyId } = await builder.page('triggers-dialog');

		const itemId    = await builder.node('Item', { name: 'to be removed' });
		const triggerId = await builder.element(pageId, bodyId, 'Button', { _html_id: 'trigger' });

		await builder.text(pageId, triggerId, 'Remove');
		await builder.actionMapping(triggerId, {
			event:        'click',
			action:       'delete',
			idExpression: itemId,
			dialogType:   'okcancel',
			dialogTitle:  'Remove item',
			dialogText:   'Remove it from ${page.name}?'
		});

		scenarios['triggers-dialog'] = { itemId, triggerId };
	}

	// HTML5 validation
	await inputUpdatingItem('triggers-validation', 'change', { _html_required: 'required', _html_value: 'valid' });

	// options
	await inputUpdatingItem('triggers-delay', 'input', {}, { options: '{"delay": 500}' });

	{
		const { pageId, bodyId } = await builder.page('triggers-delay-two-elements');

		const firstItemId  = await builder.node('Item', { name: 'unchanged' });
		const secondItemId = await builder.node('Item', { name: 'unchanged' });
		const firstId      = await builder.element(pageId, bodyId, 'Input', { _html_id: 'first', _html_name: 'name' });
		const secondId     = await builder.element(pageId, bodyId, 'Input', { _html_id: 'second', _html_name: 'name' });

		await builder.actionMapping(firstId, { event: 'input', action: 'update', idExpression: firstItemId, options: '{"delay": 500}' });
		await builder.actionMapping(secondId, { event: 'input', action: 'update', idExpression: secondItemId, options: '{"delay": 500}' });

		scenarios['triggers-delay-two-elements'] = { firstItemId, secondItemId };
	}

	await inputUpdatingItem('triggers-checkbox-default', 'click', { _html_type: 'checkbox', _html_name: 'done' });
	await inputUpdatingItem('triggers-checkbox-no-prevent-default', 'click', { _html_type: 'checkbox', _html_name: 'done' }, { options: '{"preventDefault": false}' });
	await buttonUpdatingItem('triggers-stop-propagation-default', 'click');
	await buttonUpdatingItem('triggers-no-stop-propagation', 'click', { options: '{"stopPropagation": false}' });
	await inputUpdatingItem('triggers-reset-value', 'change', { _html_value: 'initial' }, { options: '{"resetValue": true}' });

	await buildPagination();
});

test.describe('DOM events', () => {

	// A button mapped to click updates an Item with the constant name 'triggered';
	// clicking it must send that parameter and change the Item's name.
	test('click on a button sends the action', async ({ page }) => {

		const { itemId } = scenarios['triggers-click'];

		await login(page);
		await openPage(page, 'triggers-click');

		const request = nextActionRequest(page);

		await page.locator('#trigger').click();

		expect((await request).postDataJSON().name).toBe('triggered');
		await expect.poll(() => itemName(itemId)).toBe('triggered');
	});

	// An input mapped to change: typing alone must send nothing for a while,
	// leaving the field must send the typed value and store it on the Item.
	test('change on an input sends its value when the input loses focus', async ({ page }) => {

		const { itemId, triggerId } = scenarios['triggers-change'];

		await login(page);
		await openPage(page, 'triggers-change');

		const requests = recordActionRequests(page, triggerId);

		await page.locator('#field').fill('changed');
		await page.waitForTimeout(QUIET_PERIOD);

		// typing alone is no change event
		expect(requests).toHaveLength(0);

		const request = nextActionRequest(page);

		await page.locator('#field').blur();

		expect((await request).postDataJSON().name).toBe('changed');
		await expect.poll(() => itemName(itemId)).toBe('changed');
	});

	// An input mapped to input: filling it must send the current value right away
	// and store it as the Item's name.
	test('input sends the current value on an input event', async ({ page }) => {

		const { itemId } = scenarios['triggers-input'];

		await login(page);
		await openPage(page, 'triggers-input');

		const request = nextActionRequest(page);

		await page.locator('#field').fill('typed');

		expect((await request).postDataJSON().name).toBe('typed');
		await expect.poll(() => itemName(itemId)).toBe('typed');
	});

	// A form mapped to submit: clicking its submit button must send the named field's value,
	// update the Item, and keep the browser on the page (no native form submission).
	test('submit on a form sends the form fields', async ({ page }) => {

		const { itemId } = scenarios['triggers-submit'];

		await login(page);
		await openPage(page, 'triggers-submit');

		await page.locator('#field').fill('submitted');

		const request = nextActionRequest(page);

		await page.locator('#submit').click();

		expect((await request).postDataJSON().name).toBe('submitted');
		await expect.poll(() => itemName(itemId)).toBe('submitted');

		// the default preventDefault keeps the browser from submitting the form natively
		await expect(page).toHaveURL(/\/triggers-submit$/);
	});

	// An input mapped to keyup: pressing 'k' must send a value that already contains
	// the released key and store it on the Item.
	test('keyup sends the value including the released key', async ({ page }) => {

		const { itemId } = scenarios['triggers-keyup'];

		await login(page);
		await openPage(page, 'triggers-keyup');

		const request = nextActionRequest(page);

		await page.locator('#field').press('k');

		expect((await request).postDataJSON().name).toBe('k');
		await expect.poll(() => itemName(itemId)).toBe('k');
	});

	// An input mapped to keydown with preventDefault false: pressing a key must send
	// exactly one action and still let the character appear in the field.
	test('keydown with preventDefault false sends the action and keeps the typed character', async ({ page }) => {

		const { triggerId } = scenarios['triggers-keydown'];

		await login(page);
		await openPage(page, 'triggers-keydown');

		const requests = recordActionRequests(page, triggerId);

		await page.locator('#field').press('k');

		await expect.poll(() => requests.length).toBe(1);
		await expect(page.locator('#field')).toHaveValue('k');
	});

	// An input mapped to focusout: typing alone must send nothing, leaving the field
	// must send exactly one action with the typed value and update the Item.
	test('focusout sends the action when the input loses focus', async ({ page }) => {

		const { itemId, triggerId } = scenarios['triggers-focusout'];

		await login(page);
		await openPage(page, 'triggers-focusout');

		const requests = recordActionRequests(page, triggerId);

		await page.locator('#field').fill('left');
		await page.waitForTimeout(QUIET_PERIOD);

		expect(requests).toHaveLength(0);

		await page.locator('#field').blur();

		await expect.poll(() => requests.length).toBe(1);
		expect(requests[0].postDataJSON().name).toBe('left');
		await expect.poll(() => itemName(itemId)).toBe('left');
	});

	// A div mapped to load updates an Item: opening the page must send exactly one
	// action and set the Item's name to 'loaded'.
	test('load sends the action once when the page loads', async ({ page }) => {

		const { itemId, triggerId } = scenarios['triggers-load'];

		await login(page);

		const requests = recordActionRequests(page, triggerId);

		await openPage(page, 'triggers-load');

		await expect.poll(() => requests.length).toBe(1);
		await expect.poll(() => itemName(itemId)).toBe('loaded');
	});

	// Same load mapping: after the page has loaded, a partial reload of an unrelated section
	// must not send the load action a second time.
	test('load does not fire again when a partial reload elsewhere rebinds the events', async ({ page }) => {

		const { triggerId } = scenarios['triggers-load'];

		await login(page);

		const requests = recordActionRequests(page, triggerId);

		await openPage(page, 'triggers-load');
		await expect.poll(() => requests.length).toBe(1);

		const reload = page.waitForResponse(response => response.url().includes('/structr/html/'));

		await page.locator('#reload').click();
		await reload;
		await page.waitForTimeout(QUIET_PERIOD);

		expect(requests).toHaveLength(1);
	});

	// A button mapped to 'click,mouseover': hovering must send the action once,
	// and clicking must send it a second time.
	test('each of several comma-separated events triggers the action', async ({ page }) => {

		const { triggerId } = scenarios['triggers-several-events'];

		await login(page);
		await openPage(page, 'triggers-several-events');

		const requests = recordActionRequests(page, triggerId);

		await page.locator('#trigger').hover();
		await expect.poll(() => requests.length).toBe(1);

		await page.locator('#trigger').click();
		await expect.poll(() => requests.length).toBe(2);
	});

	// A button mapped to 'click, mouseover' (space after the comma): hovering and clicking
	// must each send the action, just like without the space.
	test('a space after the comma in the event list does not break the following event', async ({ page }) => {

		const { triggerId } = scenarios['triggers-several-events-spaced'];

		await login(page);
		await openPage(page, 'triggers-several-events-spaced');

		const requests = recordActionRequests(page, triggerId);

		await page.locator('#trigger').hover();
		await expect.poll(() => requests.length).toBe(1);

		await page.locator('#trigger').click();
		await expect.poll(() => requests.length).toBe(2);
	});

	// A button with two click mappings, each updating its own Item: a click must send
	// exactly one request and change exactly one of the two Items.
	// the docs say nothing about several mappings on one element: this pins the current behaviour,
	// where only one of them is rendered and executed
	test('only one of two action mappings on the same element runs', async ({ page }) => {

		const { itemId, secondItemId, triggerId } = scenarios['triggers-two-mappings'];

		await login(page);
		await openPage(page, 'triggers-two-mappings');

		const requests = recordActionRequests(page, triggerId);
		const response = nextActionResponse(page);

		await page.locator('#trigger').click();
		await response;
		await page.waitForTimeout(QUIET_PERIOD);

		expect(requests).toHaveLength(1);

		const names = [ await itemName(itemId), await itemName(secondItemId) ].sort();

		expect(names).toEqual([ 'triggered', 'unchanged' ]);
	});
});

test.describe('confirmation dialog', () => {

	// A delete button with an okcancel dialog: the confirm must show title and evaluated text
	// separated by a blank line, and dismissing it must send nothing and keep the Item.
	test('the dialog shows title and text, and Cancel sends nothing', async ({ page }) => {

		const { itemId, triggerId } = scenarios['triggers-dialog'];

		await login(page);
		await openPage(page, 'triggers-dialog');

		const requests = recordActionRequests(page, triggerId);
		const dialogs  = [];

		page.once('dialog', async dialog => {
			dialogs.push({ type: dialog.type(), message: dialog.message() });
			await dialog.dismiss();
		});

		await page.locator('#trigger').click();
		await page.waitForTimeout(QUIET_PERIOD);

		// title and text are separated by a blank line, the text is evaluated when the page renders
		expect(dialogs).toEqual([ { type: 'confirm', message: 'Remove item\n\nRemove it from triggers-dialog?' } ]);
		expect(requests).toHaveLength(0);
		expect(await builder.get('Item', itemId)).not.toBeNull();
	});

	// The same delete button: accepting the confirm dialog must send the action
	// and delete the Item.
	test('confirming the dialog runs the action', async ({ page }) => {

		const { itemId } = scenarios['triggers-dialog'];

		await login(page);
		await openPage(page, 'triggers-dialog');

		page.once('dialog', dialog => dialog.accept());

		const request = nextActionRequest(page);

		await page.locator('#trigger').click();
		await request;

		await expect.poll(() => builder.get('Item', itemId)).toBeNull();
	});
});

test.describe('validation before sending', () => {

	// A required input mapped to change: clearing it must not send anything and leave the Item
	// unchanged; entering a valid value afterwards must send exactly one action.
	test('an input that fails HTML5 validation does not send the action', async ({ page }) => {

		const { itemId, triggerId } = scenarios['triggers-validation'];

		await login(page);
		await openPage(page, 'triggers-validation');

		const requests = recordActionRequests(page, triggerId);

		// clearing the required input is a change, but an invalid one
		await page.locator('#field').fill('');
		await page.locator('#field').blur();
		await page.waitForTimeout(QUIET_PERIOD);

		expect(requests).toHaveLength(0);
		expect(await itemName(itemId)).toBe('unchanged');

		await page.locator('#field').fill('accepted');
		await page.locator('#field').blur();

		await expect.poll(() => requests.length).toBe(1);
		await expect.poll(() => itemName(itemId)).toBe('accepted');
	});
});

test.describe('options', () => {

	// An input mapped to input with a 500 ms delay: three quick keystrokes must result
	// in a single request carrying the final value 'abc'.
	test('delay debounces rapid events into one request with the last value', async ({ page }) => {

		const { itemId, triggerId } = scenarios['triggers-delay'];

		await login(page);
		await openPage(page, 'triggers-delay');

		const requests = recordActionRequests(page, triggerId);

		// three input events, each well within the 500 ms delay of the previous one
		await page.locator('#field').pressSequentially('abc', { delay: 50 });

		await expect.poll(() => itemName(itemId)).toBe('abc');
		await page.waitForTimeout(QUIET_PERIOD);

		expect(requests).toHaveLength(1);
		expect(requests[0].postDataJSON().name).toBe('abc');
	});

	// Two inputs, each mapped to input with a 500 ms delay and its own Item: typing into both
	// in quick succession must update both Items.
	test('debounced actions of two elements do not cancel each other', async ({ page }) => {

		const { firstItemId, secondItemId } = scenarios['triggers-delay-two-elements'];

		await login(page);
		await openPage(page, 'triggers-delay-two-elements');

		await page.locator('#first').fill('one');
		await page.locator('#second').fill('two');

		await expect.poll(() => itemName(secondItemId)).toBe('two');
		await expect.poll(() => itemName(firstItemId)).toBe('one');
	});

	// A checkbox mapped to click with default options: the click must send the action,
	// but the checkbox ends up unchecked because preventDefault reverts it.
	test('by default a handled checkbox click is reverted, because preventDefault is called', async ({ page }) => {

		const { triggerId } = scenarios['triggers-checkbox-default'];

		await login(page);
		await openPage(page, 'triggers-checkbox-default');

		const requests = recordActionRequests(page, triggerId);

		await page.locator('#field').click();

		await expect.poll(() => requests.length).toBe(1);
		await expect(page.locator('#field')).not.toBeChecked();
	});

	// A checkbox mapped to click with preventDefault false: the click must send done=true,
	// leave the checkbox checked and store true on the Item.
	test('preventDefault false keeps the checkbox state and sends it', async ({ page }) => {

		const { itemId } = scenarios['triggers-checkbox-no-prevent-default'];

		await login(page);
		await openPage(page, 'triggers-checkbox-no-prevent-default');

		const request = nextActionRequest(page);

		await page.locator('#field').click();

		// a checkbox without a value attribute sends its checked state as a boolean
		expect((await request).postDataJSON().done).toBe(true);
		await expect(page.locator('#field')).toBeChecked();
		await expect.poll(async () => (await builder.get('Item', itemId)).done).toBe(true);
	});

	// A button mapped to click with default options: after the action is sent,
	// no click event must have bubbled up to the document.
	test('by default the handled event does not reach the parent elements', async ({ page }) => {

		await login(page);
		await openPage(page, 'triggers-stop-propagation-default');
		await recordEvents(page, [ 'click' ]);

		const request = nextActionRequest(page);

		await page.locator('#trigger').click();
		await request;

		expect(await recordedEvents(page)).toEqual([]);
	});

	// A button mapped to click with stopPropagation false: the click must bubble
	// up to the document exactly once, with the button as its target.
	test('stopPropagation false lets the handled event bubble', async ({ page }) => {

		await login(page);
		await openPage(page, 'triggers-no-stop-propagation');
		await recordEvents(page, [ 'click' ]);

		const request = nextActionRequest(page);

		await page.locator('#trigger').click();
		await request;

		const clicks = await recordedEvents(page);

		expect(clicks).toHaveLength(1);
		expect(clicks[0].target).toBe('trigger');
	});

	// An input mapped to change with resetValue true: after the action returns, the input must
	// show its initial value again, while the Item stores the changed value.
	test('resetValue restores the value of the trigger element after the result', async ({ page }) => {

		const { itemId } = scenarios['triggers-reset-value'];

		await login(page);
		await openPage(page, 'triggers-reset-value');

		await expect(page.locator('#field')).toHaveValue('initial');

		const response = nextActionResponse(page);

		await page.locator('#field').fill('changed');
		await page.locator('#field').blur();
		await response;

		await expect(page.locator('#field')).toHaveValue('initial');
		await expect.poll(() => itemName(itemId)).toBe('changed');
	});
});

test.describe('pagination', () => {

	const entries = page => page.locator('#entry-list li');

	// A paged Entry list (2 per page) with the pager inside the refreshed section: next page must
	// reload the section with page=2, show Entries C and D, put page=2 into the URL, send no action request.
	test('next page loads the following page into the refreshed section and the URL', async ({ page }) => {

		await login(page);
		await openPage(page, 'triggers-pagination');

		await expect(entries(page)).toHaveText([ 'Entry A', 'Entry B' ]);

		let actionRequests = 0;
		page.on('request', request => {
			if (request.method() === 'POST' && request.url().includes('/event')) {
				actionRequests++;
			}
		});

		const reload = nextPartialReload(page);

		await page.locator('#next-page').click();

		// the page number travels as a request parameter of the partial reload, not as an action
		expect(new URL((await reload).url()).searchParams.get('page')).toBe('2');
		await expect(entries(page)).toHaveText([ 'Entry C', 'Entry D' ]);
		await expect(page).toHaveURL(/[?&]page=2(&|$)/);
		expect(actionRequests).toBe(0);
	});

	// Clicking next page twice must reach page 3 (only Entry E), because the re-rendered pager
	// carries the new page number; the URL must show page=3.
	test('repeated next page clicks advance, because the pager is part of the refreshed section', async ({ page }) => {

		await login(page);
		await openPage(page, 'triggers-pagination');

		await page.locator('#next-page').click();
		await expect(entries(page)).toHaveText([ 'Entry C', 'Entry D' ]);

		await page.locator('#next-page').click();
		await expect(entries(page)).toHaveText([ 'Entry E' ]);
		await expect(page).toHaveURL(/[?&]page=3(&|$)/);
	});

	// Starting on page 2, previous page must go back to page 1; clicking it again
	// must reload with page=1 and keep showing Entries A and B.
	test('previous page goes back and does not go below page 1', async ({ page }) => {

		await login(page);
		await openPage(page, 'triggers-pagination?page=2');

		await expect(entries(page)).toHaveText([ 'Entry C', 'Entry D' ]);

		await page.locator('#prev-page').click();
		await expect(entries(page)).toHaveText([ 'Entry A', 'Entry B' ]);
		await expect(page).toHaveURL(/[?&]page=1(&|$)/);

		const reload = nextPartialReload(page);

		await page.locator('#prev-page').click();

		expect(new URL((await reload).url()).searchParams.get('page')).toBe('1');
		await expect(entries(page)).toHaveText([ 'Entry A', 'Entry B' ]);
	});
});
