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
import {EamBuilder, nextActionRequest, nextActionResponse, nextPartialReload, openPage} from './helpers/eam';

/**
 * Event Action Mapping beyond the single button: the flow action, drag and drop with data(),
 * one mapping shared by several trigger elements, triggers in repeater rows, pages cloned
 * together with their mappings, and the follow-up that the enclosing component decides.
 */

let builder;

const items = {};

/** Marks the current document, so a later check can tell whether it was replaced by a full reload. */
async function markDocument(page) {

	await page.evaluate(() => { window.__advancedMarker = true; });
}

/** True once the marked document has been replaced by a new one (full reload or navigation). */
async function documentWasReplaced(page) {

	try {
		return await page.evaluate(() => window.__advancedMarker !== true);
	} catch (e) {
		// the execution context is destroyed while the page navigates
		return false;
	}
}

/** A button with the given HTML id and label. */
async function button(pageId, parentId, htmlId, label = htmlId) {

	const buttonId = await builder.element(pageId, parentId, 'Button', { _html_id: htmlId, _html_type: 'button' });

	await builder.text(pageId, buttonId, label);

	return buttonId;
}

/** Clones a page with all its elements and mappings, the way the Pages editor does, and gives the clone a name. */
async function clonePage(pageId, name) {

	const response = await builder.context.post(process.env.BASE_URL + `/structr/rest/Page/${pageId}/cloneNode`, { data: JSON.stringify({ deep: true }) });

	expect(response.ok()).toBeTruthy();

	const cloneId = (await response.json()).result.id;

	await builder.update('Page', cloneId, { name: name });

	return cloneId;
}

/** The element of the given type and HTML id that belongs to the given page. */
async function elementInPage(type, htmlId, pageId) {

	const candidates = await builder.find(type, { _html_id: htmlId });
	const element    = candidates.find(candidate => (candidate.pageId ?? candidate.ownerDocument?.id) === pageId);

	expect(element, `${type} #${htmlId} in page ${pageId}`).toBeTruthy();

	return element;
}

/** A flow that answers 'Hello ' + the flow parameter "name". */
async function greetingFlow(name) {

	const containerId  = await builder.node('FlowContainer', { name: name });
	const parameterId  = await builder.node('FlowParameterDataSource', { key: 'name', flowContainer: containerId });
	const returnNodeId = await builder.node('FlowReturn', { result: "concat('Hello ', data)", dataSource: parameterId, flowContainer: containerId });

	await builder.update('FlowContainer', containerId, { startNode: returnNodeId });

	return containerId;
}

test.beforeAll(async ({ playwright }) => {

	builder = await EamBuilder.create(playwright);

	await builder.schemaType('Item', [
		{ name: 'status',   propertyType: 'String' },
		{ name: 'category', propertyType: 'String' }
	], [
		{ name: 'describe', source: "{ return 'Item ' + $.this.name; }" }
	]);

	// receives the JSON of a dragged element (a data() parameter) and marks the Item it refers to
	await builder.userFunction('receiveDrop', "{ let item = $.find('Item', $.arguments.dragged.id); item.status = 'dropped'; return { id: item.id, label: $.arguments.dragged.label }; }");

	await greetingFlow('greetingFlow');

	// give the schema a moment to settle before the pages refer to the new types
	await new Promise(resolve => setTimeout(resolve, 1000));

	items.dragged    = await builder.node('Item', { name: 'Dragged item', status: 'open' });
	items.sharedA    = await builder.node('Item', { name: 'Shared A', status: 'open', category: 'shared-a' });
	items.sharedB    = await builder.node('Item', { name: 'Shared B', status: 'open', category: 'shared-b' });
	items.componentA = await builder.node('Item', { name: 'Component item', status: 'open' });
	items.componentB = await builder.node('Item', { name: 'Uncomponented item', status: 'open' });
	items.clone      = await builder.node('Item', { name: 'Clone item', status: 'open' });

	for (const name of [ 'Row A', 'Row B', 'Row C' ]) {
		items[name] = await builder.node('Item', { name: name, status: 'open', category: 'rows' });
	}

	for (const name of [ 'Delete A', 'Delete B', 'Delete C' ]) {
		items[name] = await builder.node('Item', { name: name, category: 'delete-rows' });
	}

	// flow: one button shows the flow result inline, one navigates with it, one calls a flow that does not exist
	{
		const { pageId, bodyId } = await builder.page('advanced-flow');
		const inputId            = await builder.element(pageId, bodyId, 'Input', { _html_id: 'flow-name', _html_name: 'name', _html_value: 'Alice' });

		for (const [htmlId, mapping] of [
			[ 'flow-inline',   { successNotifications: 'inline-text-message', successNotificationsText: 'Flow: {result}' } ],
			[ 'flow-navigate', { successBehaviour: 'navigate-to-url', successURL: '/advanced-flow-landing?greeting={result}' } ]
		]) {

			const buttonId  = await button(pageId, bodyId, htmlId);
			const mappingId = await builder.actionMapping(buttonId, { event: 'click', action: 'flow', flow: 'greetingFlow', ...mapping });

			await builder.parameter(mappingId, { parameterType: 'user-input', parameterName: 'name', inputElement: inputId });
		}

		const missingId = await button(pageId, bodyId, 'flow-missing');

		await builder.actionMapping(missingId, { event: 'click', action: 'flow', flow: 'missingFlow', failureNotifications: 'inline-text-message', failureNotificationsText: '{message}' });

		const landing = await builder.page('advanced-flow-landing');
		const outId   = await builder.element(landing.pageId, landing.bodyId, 'Div', { _html_id: 'greeting' });

		await builder.text(landing.pageId, outId, '${request.greeting}');
	}

	// drag and drop: the source carries its item uuid and a label, the drop zone sends them with data()
	{
		const { pageId, bodyId } = await builder.page('advanced-drag-drop');
		const sourceId           = await builder.element(pageId, bodyId, 'Div', { _html_id: 'drag-source', _html_draggable: 'true', _html_style: 'width: 120px; height: 40px; background: #ddd;' });
		const sourceMapping      = await builder.actionMapping(sourceId, { event: 'none', action: 'none', idExpression: items.dragged });
		const zoneId             = await builder.element(pageId, bodyId, 'Div', { _html_id: 'drop-zone', _html_style: 'width: 200px; height: 80px; margin-top: 40px; background: #bbb;' });
		const zoneMapping        = await builder.actionMapping(zoneId, { event: 'drop', action: 'method', method: 'receiveDrop' });

		await builder.text(pageId, sourceId, 'Drag me');
		await builder.text(pageId, zoneId, 'Drop here');
		await builder.parameter(sourceMapping, { parameterType: 'constant-value', parameterName: 'label', constantValue: 'dragged label' });
		await builder.parameter(zoneMapping, { parameterType: 'constant-value', parameterName: 'dragged', constantValue: 'data()' });
	}

	// one mapping, two trigger elements, each in a repeater over a different item
	{
		const { pageId, bodyId } = await builder.page('advanced-shared-mapping');
		const triggers           = [];

		for (const [category, htmlId] of [ [ 'shared-a', 'shared-a' ], [ 'shared-b', 'shared-b' ] ]) {

			const sectionId = await builder.element(pageId, bodyId, 'Div', { functionQuery: `find('Item', 'category', '${category}')`, dataKey: 'entry' });

			triggers.push(await button(pageId, sectionId, htmlId));
		}

		const mappingId = await builder.actionMapping(triggers, { event: 'click', action: 'update', idExpression: '${entry.id}' });

		await builder.parameter(mappingId, { parameterType: 'script-expression', parameterName: 'status', scriptExpression: 'done by ${entry.name}' });
	}

	// a repeater whose row button calls an instance method of the row's item
	{
		const { pageId, bodyId } = await builder.page('advanced-repeater-method');
		const listId             = await builder.element(pageId, bodyId, 'Ul');
		const rowId              = await builder.element(pageId, listId, 'Li', { _html_class: 'method-row', functionQuery: "find('Item', 'category', 'rows', sort('name'))", dataKey: 'entry' });
		const rowButton          = await builder.element(pageId, rowId, 'Button', { _html_class: 'describe-row', _html_type: 'button' });

		await builder.text(pageId, rowButton, '${entry.name}');
		await builder.actionMapping(rowButton, {
			event:                    'click',
			action:                   'method',
			method:                   'describe',
			idExpression:             '${entry.id}',
			successNotifications:     'inline-text-message',
			successNotificationsText: '{result}'
		});
	}

	// a repeater whose row button deletes the row's item and refreshes the list
	{
		const { pageId, bodyId } = await builder.page('advanced-repeater-delete');
		const listId             = await builder.element(pageId, bodyId, 'Ul', { _html_id: 'delete-list' });
		const rowId              = await builder.element(pageId, listId, 'Li', { _html_class: 'delete-row', functionQuery: "find('Item', 'category', 'delete-rows', sort('name'))", dataKey: 'entry' });
		const labelId            = await builder.element(pageId, rowId, 'Span', { _html_class: 'delete-label' });
		const rowButton          = await builder.element(pageId, rowId, 'Button', { _html_class: 'delete-button', _html_type: 'button' });

		await builder.text(pageId, labelId, '${entry.name}');
		await builder.text(pageId, rowButton, 'Delete');
		await builder.actionMapping(rowButton, { event: 'click', action: 'delete', idExpression: '${entry.id}', successBehaviour: 'partial-refresh', successPartial: '#delete-list' });
	}

	// component-based follow-up: the enclosing element's component configuration decides
	{
		const { pageId, bodyId } = await builder.page('advanced-component');
		const componentId        = await builder.element(pageId, bodyId, 'Div', { _html_id: 'component' });
		const statusId           = await builder.element(pageId, componentId, 'Div', { _html_id: 'component-status' });
		const insideId           = await button(pageId, componentId, 'component-save');
		const outsideId          = await button(pageId, bodyId, 'outside-save');

		await builder.node('ComponentConfiguration', { domNode: componentId, reload: 'page' });
		await builder.text(pageId, statusId, `\${find('Item', '${items.componentA}').status}`);
		const insideMapping  = await builder.actionMapping(insideId, { event: 'click', action: 'update', idExpression: items.componentA, successBehaviour: 'component-based' });
		const outsideMapping = await builder.actionMapping(outsideId, { event: 'click', action: 'update', idExpression: items.componentB, successBehaviour: 'component-based' });

		await builder.parameter(insideMapping, { parameterType: 'constant-value', parameterName: 'status', constantValue: 'saved' });
		await builder.parameter(outsideMapping, { parameterType: 'constant-value', parameterName: 'status', constantValue: 'saved' });
	}

	// a page with an input, a save button and a linked refresh target, cloned afterwards; and a cloned flow button
	{
		const { pageId, bodyId } = await builder.page('advanced-clone-source');
		const inputId            = await builder.element(pageId, bodyId, 'Input', { _html_id: 'clone-name', _html_name: 'name' });
		const panelId            = await builder.element(pageId, bodyId, 'Div', { _html_id: 'clone-panel' });
		const saveId             = await button(pageId, bodyId, 'clone-save');
		const flowButtonId       = await button(pageId, bodyId, 'clone-flow');

		await builder.text(pageId, panelId, `Name: \${find('Item', '${items.clone}').name}`);

		const saveMapping = await builder.actionMapping(saveId, { event: 'click', action: 'update', idExpression: items.clone, successBehaviour: 'partial-refresh-linked', successTargets: [ panelId ] });
		const flowMapping = await builder.actionMapping(flowButtonId, { event: 'click', action: 'flow', flow: 'greetingFlow', successNotifications: 'inline-text-message', successNotificationsText: '{result}' });

		await builder.parameter(saveMapping, { parameterType: 'user-input', parameterName: 'name', inputElement: inputId });
		await builder.parameter(flowMapping, { parameterType: 'constant-value', parameterName: 'name', constantValue: 'Clone' });

		items.clonePageSource = pageId;
		items.clonePageCopy   = await clonePage(pageId, 'advanced-clone-copy');
	}
});

test.describe('flow', () => {

	// A button with the flow action sends the input value as the flow parameter "name"; the flow's
	// return value 'Hello Alice' is the action result and appears in the inline notification.
	test('runs the flow with the mapped parameter and shows its result', async ({ page }) => {

		await login(page);
		await openPage(page, 'advanced-flow');

		const request  = nextActionRequest(page);
		const response = nextActionResponse(page);

		await page.locator('#flow-inline').click();

		const payload = (await request).postDataJSON();

		expect(payload.structrAction).toBe('flow');
		expect(payload.structrFlow).toBe('greetingFlow');
		expect(payload.name).toBe('Alice');

		expect((await response).status()).toBe(200);
		expect((await (await response).json()).result).toBe('Hello Alice');

		await expect(page.locator('.structr-event-action-notification')).toHaveText('✅ Flow: Hello Alice');
	});

	// The flow result can be used in a follow-up: navigate-to-url with {result} lands on a page that
	// renders the greeting from the request parameter.
	test('passes the flow result to a navigate-to-url follow-up', async ({ page }) => {

		await login(page);
		await openPage(page, 'advanced-flow');

		await page.locator('#flow-name').fill('Bob');
		await page.locator('#flow-navigate').click();

		await expect(page).toHaveURL(/\/advanced-flow-landing\?greeting=Hello(%20| )Bob$/);
		await expect(page.locator('#greeting')).toHaveText('Hello Bob');
	});

	// A mapping naming a flow that does not exist is answered with 422, and the failure
	// notification shows the server's message naming the missing flow.
	test('a flow that does not exist is answered with 422 and a message', async ({ page }) => {

		await login(page);
		await openPage(page, 'advanced-flow');

		const response = nextActionResponse(page);

		await page.locator('#flow-missing').click();

		expect((await response).status()).toBe(422);
		await expect(page.locator('.structr-event-action-notification')).toContainText('missingFlow does not exist');
	});
});

test.describe('drag and drop', () => {

	// Dragging the source onto the drop zone sends the source's data as the data() parameter: its
	// mapped parameters plus its item uuid as "id"; the function marks that item as dropped.
	test('a drop sends the dragged element data with data() and the action uses it', async ({ page }) => {

		await login(page);
		await openPage(page, 'advanced-drag-drop');

		const request  = nextActionRequest(page);
		const response = nextActionResponse(page);

		await page.locator('#drag-source').dragTo(page.locator('#drop-zone'));

		const payload = (await request).postDataJSON();

		expect(payload.dragged.id).toBe(items.dragged);
		expect(payload.dragged.label).toBe('dragged label');

		expect((await response).status()).toBe(200);
		expect((await (await response).json()).result).toEqual({ id: items.dragged, label: 'dragged label' });

		await expect.poll(async () => (await builder.get('Item', items.dragged)).status).toBe('dropped');
	});

	// The drop zone is the action's trigger: its request goes to the drop zone's element, not to the
	// dragged source, and dragging alone (without dropping) sends nothing.
	test('only the drop sends an action, addressed to the drop zone', async ({ page }) => {

		await login(page);
		await openPage(page, 'advanced-drag-drop');

		const zoneId = await page.locator('#drop-zone').getAttribute('data-structr-id');
		const urls   = [];

		page.on('request', request => {
			if (request.method() === 'POST' && request.url().includes('/event')) {
				urls.push(request.url());
			}
		});

		const response = nextActionResponse(page);

		await page.locator('#drag-source').dragTo(page.locator('#drop-zone'));
		await response;

		expect(urls).toEqual([ expect.stringContaining(`/structr/rest/DOMElement/${zoneId}/event`) ]);
	});
});

test.describe('several triggers', () => {

	// One mapping has two trigger buttons, each in a repeater over a different item: clicking the
	// second button updates only the second item, with the value rendered in its own context.
	test('one mapping with two trigger elements runs in the context of the clicked element', async ({ page }) => {

		await login(page);
		await openPage(page, 'advanced-shared-mapping');

		const request = nextActionRequest(page);

		await page.locator('#shared-b').click();

		const payload = (await request).postDataJSON();

		expect(payload.structrIdExpression).toBe(items.sharedB);
		expect(payload.status).toBe('done by Shared B');

		await expect.poll(async () => (await builder.get('Item', items.sharedB)).status).toBe('done by Shared B');

		expect((await builder.get('Item', items.sharedA)).status).toBe('open');
	});

	// Both trigger elements of the shared mapping send their request to their own element uuid,
	// and both actions work.
	test('each trigger element of a shared mapping sends to its own element', async ({ page }) => {

		await login(page);
		await openPage(page, 'advanced-shared-mapping');

		for (const [htmlId, itemId, name] of [ [ 'shared-a', items.sharedA, 'Shared A' ], [ 'shared-b', items.sharedB, 'Shared B' ] ]) {

			const elementId = await page.locator('#' + htmlId).getAttribute('data-structr-id');
			const request   = nextActionRequest(page);

			await page.locator('#' + htmlId).click();

			expect((await request).url()).toContain(`/structr/rest/DOMElement/${elementId}/event`);
			await expect.poll(async () => (await builder.get('Item', itemId)).status).toBe('done by ' + name);
		}
	});
});

test.describe('repeater rows', () => {

	// The row button calls the instance method of its own row's item: clicking the second row shows
	// 'Item Row B' in the inline notification.
	test('a row button calls the method on the item of its own row', async ({ page }) => {

		await login(page);
		await openPage(page, 'advanced-repeater-method');

		await expect(page.locator('.describe-row')).toHaveText([ 'Row A', 'Row B', 'Row C' ]);

		const request = nextActionRequest(page);

		await page.locator('.describe-row').nth(1).click();

		expect((await request).postDataJSON().structrIdExpression).toBe(items['Row B']);
		await expect(page.locator('.structr-event-action-notification')).toHaveText('✅ Item Row B');
	});

	// The delete button of the first row deletes only that row's item, and the refreshed list
	// shows the two remaining rows.
	test('a row button deletes the item of its own row and the list refresh drops the row', async ({ page }) => {

		await login(page);
		await openPage(page, 'advanced-repeater-delete');

		await expect(page.locator('.delete-label')).toHaveText([ 'Delete A', 'Delete B', 'Delete C' ]);

		const reload = nextPartialReload(page);

		await page.locator('.delete-button').first().click();
		await reload;

		await expect(page.locator('.delete-label')).toHaveText([ 'Delete B', 'Delete C' ]);

		expect(await builder.get('Item', items['Delete A'])).toBeNull();
		expect(await builder.get('Item', items['Delete B'])).not.toBeNull();
	});
});

test.describe('component-based follow-up', () => {

	// A button inside an element whose component configuration says reload "page" renders the
	// full-page-reload target and reloads the whole page after the action.
	test('the enclosing component with reload "page" reloads the page', async ({ page }) => {

		await login(page);
		await openPage(page, 'advanced-component');

		await expect(page.locator('#component-save')).toHaveAttribute('data-structr-success-target', 'url:');
		await expect(page.locator('#component-status')).toHaveText('open');

		await markDocument(page);

		await page.locator('#component-save').click();

		await expect.poll(() => documentWasReplaced(page)).toBe(true);
		await expect(page.locator('#component-status')).toHaveText('saved');
	});

	// A component-based button without an enclosing component has no follow-up: the action runs,
	// but no success target is rendered and the page stays as it is.
	test('without an enclosing component the action runs without a follow-up', async ({ page }) => {

		await login(page);
		await openPage(page, 'advanced-component');

		await expect(page.locator('#outside-save')).not.toHaveAttribute('data-structr-success-target');

		await markDocument(page);

		const response = nextActionResponse(page);

		await page.locator('#outside-save').click();

		expect((await response).status()).toBe(200);
		await expect.poll(async () => (await builder.get('Item', items.componentB)).status).toBe('saved');

		expect(await documentWasReplaced(page)).toBe(false);
	});
});

test.describe('cloned page', () => {

	// The clone's save button has its own action mapping, whose user input and linked refresh
	// target are the clone's own input and panel, not those of the original page.
	test('the mappings of a cloned page point to the elements of the clone', async () => {

		const sourceSave  = await elementInPage('Button', 'clone-save', items.clonePageSource);
		const copySave    = await elementInPage('Button', 'clone-save', items.clonePageCopy);
		const copyInput   = await elementInPage('Input', 'clone-name', items.clonePageCopy);
		const copyPanel   = await elementInPage('Div', 'clone-panel', items.clonePageCopy);
		const sourceMap   = (await builder.get('Button', sourceSave.id)).triggeredActions[0].id;
		const copyMap     = (await builder.get('Button', copySave.id)).triggeredActions[0].id;
		const copyMapping = await builder.get('ActionMapping', copyMap);
		const parameter   = await builder.get('ParameterMapping', copyMapping.parameterMappings[0].id);

		expect(copyMap).not.toBe(sourceMap);
		expect(parameter.inputElement.id).toBe(copyInput.id);
		expect(copyMapping.successTargets.map(target => target.id)).toEqual([ copyPanel.id ]);
	});

	// In the browser, the cloned page saves the typed name and refreshes its own panel with it.
	test('the cloned page saves the input and refreshes its own panel', async ({ page }) => {

		await login(page);
		await openPage(page, 'advanced-clone-copy');

		await page.locator('#clone-name').fill('Saved from the clone');

		const reload = nextPartialReload(page);

		await page.locator('#clone-save').click();
		await reload;

		await expect(page.locator('#clone-panel')).toHaveText('Name: Saved from the clone');
		expect((await builder.get('Item', items.clone)).name).toBe('Saved from the clone');
	});

	// The cloned flow button runs the same flow as the original: the inline notification shows
	// 'Hello Clone' on both pages.
	test('a cloned flow mapping still runs its flow', async ({ page }) => {

		await login(page);

		for (const pageName of [ 'advanced-clone-source', 'advanced-clone-copy' ]) {

			await openPage(page, pageName);

			const response = nextActionResponse(page);

			await page.locator('#clone-flow').click();

			expect((await response).status(), pageName).toBe(200);
			await expect(page.locator('.structr-event-action-notification')).toHaveText('✅ Hello Clone');
		}
	});
});
