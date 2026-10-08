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
import {EamBuilder, nextActionRequest, nextActionResponse, nextPartialReload, openPage, UUID_PATTERN} from './helpers/eam';

/**
 * Event Action Mapping: how parameters reach the server.
 *
 * Covers the parameter types (user input, constant value, script expression), the value of each
 * kind of form control, form submission, repeaters, and which data attributes of the trigger
 * element travel along in the payload. Pagination parameters are covered by the pagination spec.
 */

let builder;

const items = {};

// the data attributes frontend.js applies in the browser and therefore must not send (Frontend.clientOnlyDataKeys)
const CLIENT_ONLY_KEYS = [
	'structrPage', 'structrRenderState', 'structrTemplateId',
	'structrDialogType', 'structrDialogTitle', 'structrDialogText',
	'structrSuccessTarget', 'structrFailureTarget',
	'structrSuccessNotifications', 'structrSuccessNotificationsDelay', 'structrSuccessNotificationsText',
	'structrSuccessNotificationsCssClass', 'structrSuccessNotificationsPartial', 'structrSuccessNotificationsEvent',
	'structrSuccessNotificationsCustomDialogElement',
	'structrFailureNotifications', 'structrFailureNotificationsDelay', 'structrFailureNotificationsText',
	'structrFailureNotificationsCssClass', 'structrFailureNotificationsPartial', 'structrFailureNotificationsEvent',
	'structrFailureNotificationsCustomDialogElement',
	'currentObjectId', 'structrRequestKeys'
];

/** An option with a value and a visible label. */
async function addOption(pageId, selectId, value, label) {

	const optionId = await builder.element(pageId, selectId, 'Option', { _html_value: value });

	await builder.text(pageId, optionId, label);
}

/** A select with the options red, green and blue. */
async function addColorSelect(pageId, parentId, data) {

	const selectId = await builder.element(pageId, parentId, 'Select', data);

	await addOption(pageId, selectId, 'red', 'Red');
	await addOption(pageId, selectId, 'green', 'Green');
	await addOption(pageId, selectId, 'blue', 'Blue');

	return selectId;
}

/** A button that updates the given item, without parameters yet. Returns the action mapping id. */
async function addUpdateButton(pageId, parentId, htmlId, itemId, data = {}) {

	const buttonId = await builder.element(pageId, parentId, 'Button', { _html_id: htmlId, _html_type: 'button' });

	await builder.text(pageId, buttonId, 'Save');

	return await builder.actionMapping(buttonId, { event: 'click', action: 'update', idExpression: itemId, ...data });
}

/**
 * A page with one control per value type, each mapped as a user-input parameter of a save button
 * that updates the given item. Every control has an HTML id except the number input, which is
 * therefore referenced by its uuid.
 */
async function addFieldsPage(name, itemId) {

	const { pageId, bodyId } = await builder.page(name);

	const noteId        = await builder.element(pageId, bodyId, 'Input',    { _html_id: 'note-input', _html_type: 'text' });
	const priorityId    = await builder.element(pageId, bodyId, 'Input',    { _html_type: 'number', _html_class: 'priority-input' });
	const descriptionId = await builder.element(pageId, bodyId, 'Textarea', { _html_id: 'description-input' });
	const colorId       = await addColorSelect(pageId, bodyId, { _html_id: 'color-input' });
	const tagsId        = await addColorSelect(pageId, bodyId, { _html_id: 'tags-input', _html_multiple: 'multiple' });
	const activeId      = await builder.element(pageId, bodyId, 'Input',    { _html_id: 'active-input', _html_type: 'checkbox' });
	const sizeId        = await builder.element(pageId, bodyId, 'Input',    { _html_id: 'size-input', _html_type: 'checkbox', _html_value: 'large' });
	const dueDateId     = await builder.element(pageId, bodyId, 'Input',    { _html_id: 'due-date-input', _html_type: 'datetime-local' });

	const actionMappingId = await addUpdateButton(pageId, bodyId, 'save', itemId);

	const inputs = { note: noteId, priority: priorityId, description: descriptionId, color: colorId, tags: tagsId, active: activeId, size: sizeId, dueDate: dueDateId };

	for (const [parameterName, inputId] of Object.entries(inputs)) {
		await builder.parameter(actionMappingId, { parameterType: 'user-input', parameterName: parameterName, inputElement: inputId });
	}
}

/** Structr renders dates with a +0000 style offset, which Date.parse does not accept everywhere. */
function isoOf(storedDate) {

	return new Date(String(storedDate).replace(/([+-]\d{2})(\d{2})$/, '$1:$2')).toISOString();
}

test.beforeAll(async ({ playwright }) => {

	builder = await EamBuilder.create(playwright);

	await builder.schemaType('Item', [
		{ name: 'note',        propertyType: 'String' },
		{ name: 'description', propertyType: 'String' },
		{ name: 'priority',    propertyType: 'Integer' },
		{ name: 'active',      propertyType: 'Boolean' },
		{ name: 'size',        propertyType: 'String' },
		{ name: 'color',       propertyType: 'String' },
		{ name: 'tags',        propertyType: 'StringArray' },
		{ name: 'dueDate',     propertyType: 'Date' }
	]);

	await builder.schemaType('Entry', [
		{ name: 'note', propertyType: 'String' }
	]);

	await builder.schemaType('Note', [
		{ name: 'title', propertyType: 'String', notNull: true }
	]);

	for (const key of [ 'fields', 'empty', 'constants', 'script', 'form', 'triggerValue', 'clientOnly', 'leakRequest', 'leakRefresh', 'requestNamed' ]) {
		items[key] = await builder.node('Item', { name: `${key} item`, note: 'initial note', priority: 1, active: true });
	}

	// user input from every kind of control
	await addFieldsPage('parameters-fields', items.fields);
	await addFieldsPage('parameters-empty', items.empty);

	// constant values
	{
		const { pageId, bodyId } = await builder.page('parameters-constants');
		const actionMappingId    = await addUpdateButton(pageId, bodyId, 'save', items.constants);

		await builder.parameter(actionMappingId, { parameterType: 'constant-value', parameterName: 'note',        constantValue: 'fixed text' });
		await builder.parameter(actionMappingId, { parameterType: 'constant-value', parameterName: 'priority',    constantValue: 'json(5)' });
		await builder.parameter(actionMappingId, { parameterType: 'constant-value', parameterName: 'active',      constantValue: 'json(false)' });
		await builder.parameter(actionMappingId, { parameterType: 'constant-value', parameterName: 'tags',        constantValue: 'json(["one", "two"])' });
		await builder.parameter(actionMappingId, { parameterType: 'constant-value', parameterName: 'description', constantValue: 'Owner: ${me.name}' });

		// values without a matching property go to an action that stores nothing
		const sendId       = await builder.element(pageId, bodyId, 'Button', { _html_id: 'send', _html_type: 'button' });
		const rawMappingId = await builder.actionMapping(sendId, { event: 'click', action: 'none' });

		await builder.text(pageId, sendId, 'Send');
		await builder.parameter(rawMappingId, { parameterType: 'constant-value', parameterName: 'settings',     constantValue: 'json({"mode": "compact", "levels": [1, 2]})' });
		await builder.parameter(rawMappingId, { parameterType: 'constant-value', parameterName: 'blankValue',   constantValue: '' });
		await builder.parameter(rawMappingId, { parameterType: 'constant-value', parameterName: 'missingValue' });
	}

	// script expressions, on a page that is opened with the item as its current object
	{
		const { pageId, bodyId } = await builder.page('parameters-script');
		const actionMappingId    = await addUpdateButton(pageId, bodyId, 'save', items.script);

		await builder.parameter(actionMappingId, { parameterType: 'script-expression', parameterName: 'note',        scriptExpression: 'Item: ${current.name}' });
		await builder.parameter(actionMappingId, { parameterType: 'script-expression', parameterName: 'description', scriptExpression: '${me.name}' });
	}

	// form submission
	{
		const { pageId, bodyId } = await builder.page('parameters-form');
		const formId             = await builder.element(pageId, bodyId, 'Form', { _html_id: 'item-form' });

		await builder.element(pageId, formId, 'Input',    { _html_id: 'form-note', _html_name: 'note', _html_type: 'text' });
		await builder.element(pageId, formId, 'Input',    { _html_id: 'form-priority', _html_name: 'priority', _html_type: 'number' });
		await builder.element(pageId, formId, 'Textarea', { _html_id: 'form-description', _html_name: 'description' });
		await builder.element(pageId, formId, 'Input',    { _html_id: 'form-active', _html_name: 'active', _html_type: 'checkbox' });
		await builder.element(pageId, formId, 'Input',    { _html_id: 'form-size', _html_name: 'size', _html_type: 'checkbox', _html_value: 'large' });
		await builder.element(pageId, formId, 'Input',    { _html_id: 'form-color-red', _html_name: 'color', _html_type: 'radio', _html_value: 'red' });
		await builder.element(pageId, formId, 'Input',    { _html_id: 'form-color-blue', _html_name: 'color', _html_type: 'radio', _html_value: 'blue' });
		await addColorSelect(pageId, formId, { _html_id: 'form-tags', _html_name: 'tags', _html_multiple: 'multiple' });

		const submitId = await builder.element(pageId, formId, 'Button', { _html_id: 'form-submit', _html_type: 'submit' });
		await builder.text(pageId, submitId, 'Submit');

		await builder.actionMapping(formId, { event: 'submit', action: 'update', idExpression: items.form });
	}

	// the trigger element's own name and value, without any parameter mapping
	{
		const { pageId, bodyId } = await builder.page('parameters-trigger-value');

		const noteId   = await builder.element(pageId, bodyId, 'Input', { _html_id: 'trigger-note', _html_name: 'note', _html_type: 'text' });
		const activeId = await builder.element(pageId, bodyId, 'Input', { _html_id: 'trigger-active', _html_name: 'active', _html_type: 'checkbox', _html_checked: 'checked' });

		await builder.actionMapping(noteId,   { event: 'change', action: 'update', idExpression: items.triggerValue });
		await builder.actionMapping(activeId, { event: 'change', action: 'update', idExpression: items.triggerValue });
	}

	// a trigger configured with every kind of client-side display option
	{
		const { pageId, bodyId } = await builder.page('parameters-client-only');

		await builder.element(pageId, bodyId, 'Div', { _html_id: 'client-only-box' });

		await addUpdateButton(pageId, bodyId, 'save', items.clientOnly, {
			successNotifications:         'inline-text-message',
			successNotificationsText:     'Saved {status}',
			successNotificationsCssClass: 'saved',
			successNotificationsDelay:    -1,
			failureNotifications:         'fire-event',
			failureNotificationsEvent:    'item-failed',
			successBehaviour:             'partial-refresh',
			successPartial:               '#client-only-box',
			failureBehaviour:             'fire-event',
			failureEvent:                 'item-not-saved'
		});
	}

	// a trigger that is its own reload target, so it carries data-request-* and, after a reload, data-last-refresh
	for (const [name, itemKey, htmlId] of [ [ 'parameters-leak-request', 'leakRequest', 'leak-request-save' ], [ 'parameters-leak-refresh', 'leakRefresh', 'leak-refresh-save' ] ]) {

		const { pageId, bodyId } = await builder.page(name);

		await addUpdateButton(pageId, bodyId, htmlId, items[itemKey], { successBehaviour: 'partial-refresh', successPartial: '#' + htmlId });
	}

	// a trigger that is its own reload target and maps a constant parameter whose name starts with "request"
	{
		const { pageId, bodyId } = await builder.page('parameters-request-named');
		const mappingId          = await addUpdateButton(pageId, bodyId, 'request-named-save', items.requestNamed, { successBehaviour: 'partial-refresh', successPartial: '#request-named-save' });

		await builder.parameter(mappingId, { parameterType: 'constant-value', parameterName: 'requestNote', constantValue: 'kept' });
	}

	// a create action that fails validation on a property no parameter is mapped to
	{
		const { pageId, bodyId } = await builder.page('parameters-leak-error');
		const buttonId           = await builder.element(pageId, bodyId, 'Button', { _html_id: 'create-note', _html_type: 'button' });

		await builder.text(pageId, buttonId, 'Create');
		await builder.actionMapping(buttonId, { event: 'click', action: 'create', dataType: 'Note' });
	}

	// a repeater with one row per entry: an input without HTML id, an input with a static HTML id, and a save button
	{
		for (const letter of [ 'A', 'B', 'C' ]) {
			items['entry' + letter] = await builder.node('Entry', { name: `Entry ${letter}`, note: `note ${letter}` });
		}

		const { pageId, bodyId } = await builder.page('parameters-repeater');
		const rowId              = await builder.element(pageId, bodyId, 'Div', { _html_class: 'entry-row', dataKey: 'entry', functionQuery: "sort(find('Entry'), 'name')" });

		const nameInputId = await builder.element(pageId, rowId, 'Input', { _html_class: 'entry-name', _html_type: 'text', _html_value: '${entry.name}' });
		const noteInputId = await builder.element(pageId, rowId, 'Input', { _html_id: 'entry-note', _html_type: 'text', _html_value: '${entry.note}' });
		const buttonId    = await builder.element(pageId, rowId, 'Button', { _html_class: 'entry-save', _html_type: 'button' });

		await builder.text(pageId, buttonId, 'Save');

		const actionMappingId = await builder.actionMapping(buttonId, { event: 'click', action: 'update', idExpression: '${entry.id}' });

		await builder.parameter(actionMappingId, { parameterType: 'user-input', parameterName: 'name', inputElement: nameInputId });
		await builder.parameter(actionMappingId, { parameterType: 'user-input', parameterName: 'note', inputElement: noteInputId });
	}
});

// Fills every control on the fields page (text, number, textarea, select, multi-select, two checkboxes, datetime-local)
// and clicks save: the payload must carry each value in its client-side form, and the updated Item must
// store it with the property's type (Integer, Boolean, StringArray, Date).
test('user input is read from each kind of control and stored with the property type', async ({ page }) => {

	await login(page);
	await openPage(page, 'parameters-fields');

	await page.locator('#note-input').fill('Changed note');
	await page.locator('.priority-input').fill('7');
	await page.locator('#description-input').fill('A longer\ndescription');
	await page.locator('#color-input').selectOption('green');
	await page.locator('#tags-input').selectOption([ 'red', 'blue' ]);
	await page.locator('#active-input').uncheck();
	await page.locator('#size-input').check();
	await page.locator('#due-date-input').fill('2026-03-04T05:06');

	// datetime-local has no offset: frontend.js reads it in the browser's time zone and sends UTC
	const expectedDueDate = await page.evaluate(value => new Date(value).toISOString(), '2026-03-04T05:06');

	const request  = nextActionRequest(page);
	const response = nextActionResponse(page);

	await page.locator('#save').click();

	const payload = (await request).postDataJSON();

	expect(payload.note).toBe('Changed note');
	expect(payload.priority).toBe('7');
	expect(payload.description).toBe('A longer\ndescription');
	expect(payload.color).toBe('green');
	expect(payload.tags).toEqual([ 'red', 'blue' ]);
	expect(payload.active).toBe(false);
	expect(payload.size).toBe('large');
	expect(payload.dueDate).toBe(expectedDueDate);

	expect((await response).status()).toBe(200);

	const item = await builder.get('Item', items.fields);

	expect(item.note).toBe('Changed note');
	expect(item.priority).toBe(7);
	expect(item.description).toBe('A longer\ndescription');
	expect(item.color).toBe('green');
	expect(item.tags).toEqual([ 'red', 'blue' ]);
	expect(item.active).toBe(false);
	expect(item.size).toBe('large');
	expect(isoOf(item.dueDate)).toBe(expectedDueDate);
});

// Reads the rendered parameter attributes of the save button: the number input without HTML id is referenced
// as id(<uuid>) and carries data-structr-id, the note input with an HTML id is referenced as css(#note-input).
test('an input without HTML id is referenced by its uuid', async ({ page }) => {

	await login(page);
	await openPage(page, 'parameters-fields');

	const priorityInput = page.locator('.priority-input');
	const reference     = await page.locator('#save').getAttribute('data-priority');
	const inputUuid     = await priorityInput.getAttribute('data-structr-id');

	// the input renders its uuid only because a parameter references it
	expect(inputUuid).toMatch(UUID_PATTERN);
	expect(reference).toBe(`id(${inputUuid})`);

	// an input with an HTML id is referenced by a CSS selector instead
	expect(await page.locator('#save').getAttribute('data-note')).toBe('css(#note-input)');
});

// Clicks save on the fields page without filling anything in: empty inputs must send null, the multi-select an
// empty list, the checkbox without value false, the checkbox with value null, and the select its first option.
test('empty controls send null, an empty list or false', async ({ page }) => {

	await login(page);
	await openPage(page, 'parameters-empty');

	await page.locator('#active-input').uncheck();

	const request  = nextActionRequest(page);
	const response = nextActionResponse(page);

	await page.locator('#save').click();

	const payload = (await request).postDataJSON();

	expect(payload.note).toBeNull();
	expect(payload.priority).toBeNull();
	expect(payload.description).toBeNull();
	expect(payload.tags).toEqual([]);
	expect(payload.dueDate).toBeNull();

	// a checkbox without value attribute is a boolean, one with a value sends nothing usable when unchecked
	expect(payload.active).toBe(false);
	expect(payload.size).toBeNull();

	// a select always has a selected option, the first one by default
	expect(payload.color).toBe('red');

	expect((await response).status()).toBe(200);

	const item = await builder.get('Item', items.empty);

	expect(item.note ?? null).toBeNull();
	expect(item.priority ?? null).toBeNull();
	expect(item.active).toBe(false);
});

// Clicks save on the constants page: plain constants arrive as strings, json(...) constants as parsed number,
// boolean and array, ${...} stays literal; the Item is checked for the stored typed values.
test('constant values are sent as configured, json() as parsed JSON', async ({ page }) => {

	await login(page);
	await openPage(page, 'parameters-constants');

	const request  = nextActionRequest(page);
	const response = nextActionResponse(page);

	await page.locator('#save').click();

	const payload = (await request).postDataJSON();

	expect(payload.note).toBe('fixed text');
	expect(payload.priority).toBe(5);
	expect(payload.active).toBe(false);
	expect(payload.tags).toEqual([ 'one', 'two' ]);

	// template expressions are not evaluated in a constant value
	expect(payload.description).toBe('Owner: ${me.name}');

	expect((await response).status()).toBe(200);

	const item = await builder.get('Item', items.constants);

	expect(item.note).toBe('fixed text');
	expect(item.priority).toBe(5);
	expect(item.active).toBe(false);
	expect(item.tags).toEqual([ 'one', 'two' ]);
});

// Clicks the send button (action none) on the constants page: a json() object constant arrives as an object,
// and the parameter with an empty constant value is missing from the payload.
test('a json() constant can carry an object, a blank constant is not sent', async ({ page }) => {

	await login(page);
	await openPage(page, 'parameters-constants');

	const request = nextActionRequest(page);

	await page.locator('#send').click();

	const payload = (await request).postDataJSON();

	expect(payload.settings).toEqual({ mode: 'compact', levels: [ 1, 2 ] });
	expect(payload).not.toHaveProperty('blankValue');
});

// Clicks the send button on the constants page and checks that the parameter without any constantValue
// is sent neither as a value nor as the string "null".
test('a constant value that was never set is not sent as the string "null"', async ({ page }) => {

	await login(page);
	await openPage(page, 'parameters-constants');

	const request = nextActionRequest(page);

	await page.locator('#send').click();

	const payload = (await request).postDataJSON();

	expect([ undefined, null ]).toContain(payload.missingValue);
});

// Opens the script page with an Item as current object, renames the Item afterwards and clicks save: the
// payload must carry the values rendered at page load (${current.name}, ${me.name}), and the Item stores them.
test('script expressions are evaluated when the page renders', async ({ page }) => {

	await login(page);
	await openPage(page, `parameters-script/${items.script}`);

	// renaming the item after rendering must not change what is sent: the value is fixed in the page
	await builder.update('Item', items.script, { name: 'renamed item' });

	const request  = nextActionRequest(page);
	const response = nextActionResponse(page);

	await page.locator('#save').click();

	const payload = (await request).postDataJSON();

	expect(payload.note).toBe('Item: script item');
	expect(payload.description).toBe('admin');

	expect((await response).status()).toBe(200);

	const item = await builder.get('Item', items.script);

	expect(item.note).toBe('Item: script item');
	expect(item.description).toBe('admin');
});

// Fills the form (text, number, textarea, checkbox, radio group, multi-select) and submits it: the payload
// must hold every named field, omit the unchecked checkbox, the page must not navigate, and the Item stores the values.
test('a submitted form sends all its named fields', async ({ page }) => {

	await login(page);
	await openPage(page, 'parameters-form');

	await page.locator('#form-note').fill('Form note');
	await page.locator('#form-priority').fill('3');
	await page.locator('#form-description').fill('Form description');
	await page.locator('#form-active').check();
	await page.locator('#form-color-blue').check();
	await page.locator('#form-tags').selectOption([ 'red', 'blue' ]);

	const request  = nextActionRequest(page);
	const response = nextActionResponse(page);

	await page.locator('#form-submit').click();

	const payload = (await request).postDataJSON();

	expect(payload.note).toBe('Form note');
	expect(payload.priority).toBe('3');
	expect(payload.description).toBe('Form description');
	expect(payload.active).toBe(true);
	expect(payload.color).toBe('blue');
	expect(payload.tags).toEqual([ 'red', 'blue' ]);

	// an unchecked checkbox is not part of the form data, so its key is missing entirely
	expect(payload).not.toHaveProperty('size');

	expect((await response).status()).toBe(200);

	// submitting must not navigate away
	expect(page.url()).toContain('/parameters-form');

	const item = await builder.get('Item', items.form);

	expect(item.note).toBe('Form note');
	expect(item.priority).toBe(3);
	expect(item.description).toBe('Form description');
	expect(item.active).toBe(true);
	expect(item.color).toBe('blue');
	expect(item.tags).toEqual([ 'red', 'blue' ]);
});

// Changes a named text input and then unchecks a named checkbox, both wired directly as triggers without
// parameter mappings: each request must carry the trigger's own value, and the Item must store it.
test('a trigger element with a name sends its own value', async ({ page }) => {

	await login(page);
	await openPage(page, 'parameters-trigger-value');

	let request = nextActionRequest(page);

	await page.locator('#trigger-note').fill('Typed note');
	await page.locator('#trigger-note').blur();

	expect((await request).postDataJSON().note).toBe('Typed note');

	await expect.poll(async () => (await builder.get('Item', items.triggerValue)).note).toBe('Typed note');

	request = nextActionRequest(page);

	await page.locator('#trigger-active').uncheck();

	expect((await request).postDataJSON().active).toBe(false);

	await expect.poll(async () => (await builder.get('Item', items.triggerValue)).active).toBe(false);
});

// Clicks a save button configured with notifications, follow-ups and a current object: none of the client-only
// dataset keys may appear in the payload, while structrAction and structrIdExpression must.
test('display options of the trigger stay in the browser', async ({ page }) => {

	await login(page);
	await openPage(page, `parameters-client-only/${items.clientOnly}`);

	// precondition: the trigger really carries the display configuration and the current object
	const trigger = page.locator('#save');

	await expect(trigger).toHaveAttribute('data-structr-success-notifications', 'inline-text-message');
	await expect(trigger).toHaveAttribute('data-structr-success-target', '#client-only-box');
	await expect(trigger).toHaveAttribute('data-current-object-id', items.clientOnly);

	const request = nextActionRequest(page);

	await trigger.click();

	const payload = (await request).postDataJSON();

	for (const key of CLIENT_ONLY_KEYS) {
		expect(payload, `client-only key ${key} must not be sent`).not.toHaveProperty(key);
	}

	// the keys the server needs to run the action are sent
	expect(payload.structrAction).toBe('update');
	expect(payload.structrIdExpression).toBe(items.clientOnly);
});

// Opens a page with ?mode=compact whose save button is its own reload target and therefore renders
// data-request-mode; clicking it must not send requestMode, and the Item must not get that property.
test('request parameters rendered on a reload target are not sent as action parameters', async ({ page }) => {

	await login(page);
	await openPage(page, 'parameters-leak-request?mode=compact');

	// precondition: the trigger is a reload target and therefore carries the request parameter
	await expect(page.locator('#leak-request-save')).toHaveAttribute('data-request-mode', 'compact');

	const request  = nextActionRequest(page);
	const response = nextActionResponse(page);

	await page.locator('#leak-request-save').click();

	const payload = (await request).postDataJSON();

	expect(payload).not.toHaveProperty('requestMode');

	expect((await response).status()).toBe(200);

	const item = await builder.get('Item', items.leakRequest);

	expect(item).not.toHaveProperty('requestMode');
});

// A save button that is its own reload target maps a constant parameter named requestNote: it renders as
// data-request-note like a request parameter would, but it is a parameter and must be sent.
test('a mapped parameter whose name starts with "request" is sent', async ({ page }) => {

	await login(page);
	await openPage(page, 'parameters-request-named');

	const request = nextActionRequest(page);

	await page.locator('#request-named-save').click();

	expect((await request).postDataJSON().requestNote).toBe('kept');
});

// The same page opened with ?note=from-url, whose request attribute data-request-note collides with the
// parameter: the parameter keeps the attribute, is sent with its own value, and the request value is not sent.
test('a mapped parameter keeps its value when a request parameter would render the same attribute', async ({ page }) => {

	await login(page);
	await openPage(page, 'parameters-request-named?note=from-url');

	await expect(page.locator('#request-named-save')).toHaveAttribute('data-request-note', 'kept');

	const request = nextActionRequest(page);

	await page.locator('#request-named-save').click();

	const payload = (await request).postDataJSON();

	expect(payload.requestNote).toBe('kept');
	expect(payload).not.toHaveProperty('note');
});

// Clicks a save button that reloads itself, then clicks the reloaded button again: the second request
// must not carry the lastRefresh value from the data-last-refresh attribute set by the reload.
test('the reload timestamp of a reloaded trigger is not sent as an action parameter', async ({ page }) => {

	await login(page);
	await openPage(page, 'parameters-leak-refresh');

	const reload = nextPartialReload(page);

	await page.locator('#leak-refresh-save').click();
	await reload;

	// precondition: the trigger was replaced by the reload and carries the timestamp now
	await expect(page.locator('#leak-refresh-save')).toHaveAttribute('data-last-refresh', /^\d+$/);

	const request = nextActionRequest(page);

	await page.locator('#leak-refresh-save').click();

	expect((await request).postDataJSON()).not.toHaveProperty('lastRefresh');
});

// Creates a Note without the required title, which marks the trigger itself with data-error, then clicks
// again: the second request must not carry an error parameter.
test('a validation error marker is not sent with the next action', async ({ page }) => {

	await login(page);
	await openPage(page, 'parameters-leak-error');

	const failed = nextActionResponse(page);

	await page.locator('#create-note').click();

	expect((await failed).status()).toBe(422);

	// precondition: no parameter is mapped to "title", so the trigger itself is marked
	await expect(page.locator('#create-note')).toHaveAttribute('data-error', 'must_not_be_empty');

	const request = nextActionRequest(page);

	await page.locator('#create-note').click();

	expect((await request).postDataJSON()).not.toHaveProperty('error');
});

// Edits the name input (no HTML id) in the second of three repeater rows and clicks that row's save button:
// only that row's value and uuid are sent, and only that Entry is renamed.
test('user input in a repeater is read from the current row', async ({ page }) => {

	await login(page);
	await openPage(page, 'parameters-repeater');

	const rows = page.locator('.entry-row');

	await expect(rows).toHaveCount(3);

	await rows.nth(1).locator('.entry-name').fill('Entry B changed');

	const request  = nextActionRequest(page);
	const response = nextActionResponse(page);

	await rows.nth(1).locator('.entry-save').click();

	const payload = (await request).postDataJSON();

	// the input without HTML id is narrowed to the row of the clicked button
	expect(payload.name).toBe('Entry B changed');
	expect(payload.structrIdExpression).toBe(items.entryB);

	expect((await response).status()).toBe(200);

	expect((await builder.get('Entry', items.entryA)).name).toBe('Entry A');
	expect((await builder.get('Entry', items.entryB)).name).toBe('Entry B changed');
	expect((await builder.get('Entry', items.entryC)).name).toBe('Entry C');
});

// Edits the note input that has the same HTML id in every repeater row, in the third row, and clicks that
// row's save button: the payload must carry only that row's note, not an array of all rows.
test('user input with a static HTML id in a repeater is read from the current row', async ({ page }) => {

	await login(page);
	await openPage(page, 'parameters-repeater');

	const rows = page.locator('.entry-row');

	await expect(rows).toHaveCount(3);

	await rows.nth(2).locator('[id="entry-note"]').fill('note C changed');

	const request = nextActionRequest(page);

	await rows.nth(2).locator('.entry-save').click();

	const payload = (await request).postDataJSON();

	expect(payload.note).toBe('note C changed');
	expect(payload.structrIdExpression).toBe(items.entryC);
});
