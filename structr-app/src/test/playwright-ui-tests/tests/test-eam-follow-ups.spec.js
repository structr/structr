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
import {EamBuilder, nextActionResponse, nextPartialReload, openPage, recordEvents, recordedEvents, UUID_PATTERN} from './helpers/eam';

/**
 * Follow-up actions of Event Action Mappings ("Behavior on Success" / "Behavior on Failure"):
 * what the browser does after the action request has returned, for every behaviour the
 * server renders into data-structr-success-target / data-structr-failure-target.
 *
 * Every scenario has its own page (named followups-*) and its own Item objects, so the tests
 * do not depend on each other's order.
 */

let builder;

// Items whose status the update actions change, one per scenario
const items = {};

/** A button with a text label; type "button" so it never submits anything on its own. */
async function addButton(pageId, parentId, htmlId, data = {}) {

	const buttonId = await builder.element(pageId, parentId, 'Button', { _html_id: htmlId, _html_type: 'button', ...data });

	await builder.text(pageId, buttonId, 'Run');

	return buttonId;
}

/** A div with a text node, e.g. a reload target that shows data the action changes. */
async function addPanel(pageId, parentId, data, content) {

	const panelId = await builder.element(pageId, parentId, 'Div', data);

	await builder.text(pageId, panelId, content);

	return panelId;
}

/** An update action that sets the status of the given item to "done". */
async function addStatusUpdate(triggerId, itemId, data) {

	const actionMappingId = await builder.actionMapping(triggerId, { event: 'click', action: 'update', idExpression: itemId, ...data });

	await builder.parameter(actionMappingId, { parameterType: 'constant-value', parameterName: 'status', constantValue: 'done' });

	return actionMappingId;
}

/** A create action for an Item with the given name. */
async function addItemCreation(triggerId, name, data) {

	const actionMappingId = await builder.actionMapping(triggerId, { event: 'click', action: 'create', dataType: 'Item', ...data });

	await builder.parameter(actionMappingId, { parameterType: 'constant-value', parameterName: 'name', constantValue: name });

	return actionMappingId;
}

/** Marks the current document, so a later check can tell whether the page was loaded again. */
async function markDocument(page) {

	await page.evaluate(() => { window.__followUpsMarker = true; });
}

/** True once the marked document has been replaced by a new one (full reload or navigation). */
async function documentWasReplaced(page) {

	try {
		return await page.evaluate(() => window.__followUpsMarker !== true);
	} catch (e) {
		// the execution context is destroyed while the page navigates
		return false;
	}
}

/**
 * structr-reload does not bubble, so a normal document listener never sees it. A capturing listener
 * on the document does, which lets the test observe the event on the freshly inserted node.
 */
async function recordReloadEvents(page) {

	await page.evaluate(() => {

		window.__capturedReloads = [];
		window.__bubbledReloads  = [];

		document.addEventListener('structr-reload', (event) => window.__capturedReloads.push(event.target.id), true);
		document.addEventListener('structr-reload', (event) => window.__bubbledReloads.push(event.target.id));
	});
}

/** The query parameters of a partial reload request. */
function reloadParameters(request) {

	return new URL(request.url()).searchParams;
}

test.beforeAll(async ({ playwright }) => {

	builder = await EamBuilder.create(playwright);

	await builder.schemaType('Item', [
		{ name: 'status',   propertyType: 'String' },
		{ name: 'category', propertyType: 'String' }
	]);

	await builder.userFunction('followUpsSucceed',      "{ return 'ok'; }");
	await builder.userFunction('followUpsScalarResult', "{ return 'alpha'; }");
	await builder.userFunction('followUpsListResult',   "{ return ['first', 'second']; }");

	for (const key of [ 'none', 'fullReload', 'refreshId', 'refreshClass', 'refreshMulti', 'refreshColon', 'reloadEvent', 'linked', 'signOut' ]) {
		items[key] = await builder.node('Item', { name: `followups-${key}`, status: 'open' });
	}

	for (const name of [ 'linked-row-a', 'linked-row-b' ]) {
		await builder.node('Item', { name: name, status: 'open', category: 'linked-rows' });
	}

	for (const name of [ 'scope-row-a', 'scope-row-b', 'scope-row-c' ]) {
		await builder.node('Item', { name: name, category: 'scope-rows' });
	}

	// the page the navigation tests land on: it shows the current object and a request parameter
	{
		const { pageId, bodyId } = await builder.page('followups-target');

		await addPanel(pageId, bodyId, { _html_id: 'target-current' }, '${current.name}');
		await addPanel(pageId, bodyId, { _html_id: 'target-value' }, '${request.value}');
	}

	// none
	{
		const { pageId, bodyId } = await builder.page('followups-none');
		const buttonId           = await addButton(pageId, bodyId, 'followups-none-button');

		await addStatusUpdate(buttonId, items.none, { successBehaviour: 'none' });
	}

	// full page reload
	{
		const { pageId, bodyId } = await builder.page('followups-full-reload');
		const buttonId           = await addButton(pageId, bodyId, 'followups-full-reload-button');

		await addPanel(pageId, bodyId, { _html_id: 'followups-full-reload-status' }, `\${find('Item', '${items.fullReload}').status}`);
		await addStatusUpdate(buttonId, items.fullReload, { successBehaviour: 'full-page-reload' });
	}

	// navigate to a new page
	{
		const { pageId, bodyId } = await builder.page('followups-navigate');

		await addItemCreation(await addButton(pageId, bodyId, 'followups-navigate-id'), 'navigated-item', {
			successBehaviour: 'navigate-to-url',
			successURL:       '/followups-target/{result.id}'
		});

		await builder.actionMapping(await addButton(pageId, bodyId, 'followups-navigate-scalar'), {
			event:            'click',
			action:           'method',
			method:           'followUpsScalarResult',
			successBehaviour: 'navigate-to-url',
			successURL:       '/followups-target?value={result}'
		});

		await builder.actionMapping(await addButton(pageId, bodyId, 'followups-navigate-list'), {
			event:            'click',
			action:           'method',
			method:           'followUpsListResult',
			successBehaviour: 'navigate-to-url',
			successURL:       '/followups-target?value={result}'
		});

		await addItemCreation(await addButton(pageId, bodyId, 'followups-navigate-missing'), 'missing-path-item', {
			successBehaviour: 'navigate-to-url',
			successURL:       '/followups-target?value={result.missing}'
		});

		// ${...} is evaluated when the page renders, {...} when the action has returned
		await addItemCreation(await addButton(pageId, bodyId, 'followups-navigate-template'), 'template-item', {
			successBehaviour: 'navigate-to-url',
			successURL:       '/followups-target/{result.id}?value=${me.name}'
		});
	}

	// partial refresh by id
	{
		const { pageId, bodyId } = await builder.page('followups-refresh-id');
		const buttonId           = await addButton(pageId, bodyId, 'followups-refresh-id-button');

		await addPanel(pageId, bodyId, { _html_id: 'followups-refresh-id-panel' }, `Status: \${find('Item', '${items.refreshId}').status}`);
		await addStatusUpdate(buttonId, items.refreshId, { successBehaviour: 'partial-refresh', successPartial: '#followups-refresh-id-panel' });
	}

	// partial refresh by class, with two matching elements
	{
		const { pageId, bodyId } = await builder.page('followups-refresh-class');
		const buttonId           = await addButton(pageId, bodyId, 'followups-refresh-class-button');

		await addPanel(pageId, bodyId, { _html_class: 'followups-refresh-class-panel' }, `First: \${find('Item', '${items.refreshClass}').status}`);
		await addPanel(pageId, bodyId, { _html_class: 'followups-refresh-class-panel' }, `Second: \${find('Item', '${items.refreshClass}').status}`);
		await addStatusUpdate(buttonId, items.refreshClass, { successBehaviour: 'partial-refresh', successPartial: '.followups-refresh-class-panel' });
	}

	// partial refresh of several comma-separated selectors
	{
		const { pageId, bodyId } = await builder.page('followups-refresh-multi');
		const buttonId           = await addButton(pageId, bodyId, 'followups-refresh-multi-button');

		await addPanel(pageId, bodyId, { _html_id: 'followups-refresh-multi-a' }, `A: \${find('Item', '${items.refreshMulti}').status}`);
		await addPanel(pageId, bodyId, { _html_id: 'followups-refresh-multi-b' }, `B: \${find('Item', '${items.refreshMulti}').status}`);
		await addStatusUpdate(buttonId, items.refreshMulti, { successBehaviour: 'partial-refresh', successPartial: '#followups-refresh-multi-a, #followups-refresh-multi-b' });
	}

	// partial refresh with a selector that contains a colon
	{
		const { pageId, bodyId } = await builder.page('followups-refresh-colon');
		const buttonId           = await addButton(pageId, bodyId, 'followups-refresh-colon-button');

		await addPanel(pageId, bodyId, { _html_id: 'followups-refresh-colon-panel' }, `Status: \${find('Item', '${items.refreshColon}').status}`);
		await addStatusUpdate(buttonId, items.refreshColon, { successBehaviour: 'partial-refresh', successPartial: '#followups-refresh-colon-panel:not(.excluded)' });
	}

	// partial refresh: the result arrives as request parameters
	{
		const { pageId, bodyId } = await builder.page('followups-refresh-params');

		await addPanel(pageId, bodyId, { _html_id: 'followups-refresh-params-panel' }, 'Name: ${request.name}');
		await addItemCreation(await addButton(pageId, bodyId, 'followups-refresh-params-button'), 'reload-parameter-item', {
			successBehaviour: 'partial-refresh',
			successPartial:   '#followups-refresh-params-panel'
		});

		await addPanel(pageId, bodyId, { _html_id: 'followups-refresh-scalar-panel' }, 'Scalar');
		await builder.actionMapping(await addButton(pageId, bodyId, 'followups-refresh-scalar-button'), {
			event:            'click',
			action:           'method',
			method:           'followUpsScalarResult',
			successBehaviour: 'partial-refresh',
			successPartial:   '#followups-refresh-scalar-panel'
		});

		await addPanel(pageId, bodyId, { _html_id: 'followups-refresh-failure-panel' }, 'Failure');
		await builder.actionMapping(await addButton(pageId, bodyId, 'followups-refresh-failure-button'), {
			event:            'click',
			action:           'method',
			method:           'followUpsMissingFunction',
			failureBehaviour: 'partial-refresh',
			failurePartial:   '#followups-refresh-failure-panel'
		});
	}

	// structr-reload event
	{
		const { pageId, bodyId } = await builder.page('followups-reload-event');
		const buttonId           = await addButton(pageId, bodyId, 'followups-reload-event-button');

		await addPanel(pageId, bodyId, { _html_id: 'followups-reload-event-panel' }, `Status: \${find('Item', '${items.reloadEvent}').status}`);
		await addStatusUpdate(buttonId, items.reloadEvent, { successBehaviour: 'partial-refresh', successPartial: '#followups-reload-event-panel' });
	}

	// updateHistory option on a partial refresh
	{
		const { pageId, bodyId } = await builder.page('followups-history');

		await addPanel(pageId, bodyId, { _html_id: 'followups-history-panel' }, 'Name: ${request.name}');
		await addItemCreation(await addButton(pageId, bodyId, 'followups-history-button'), 'history-item', {
			successBehaviour: 'partial-refresh',
			successPartial:   '#followups-history-panel',
			options:          JSON.stringify({ updateHistory: true })
		});
	}

	// partial refresh of a linked element
	{
		const { pageId, bodyId } = await builder.page('followups-linked');
		const buttonId           = await addButton(pageId, bodyId, 'followups-linked-button');
		const panelId            = await addPanel(pageId, bodyId, { _html_class: 'followups-linked-panel' }, `Status: \${find('Item', '${items.linked}').status}`);

		await addStatusUpdate(buttonId, items.linked, { successBehaviour: 'partial-refresh-linked', successTargets: [ panelId ] });
	}

	// partial refresh of a linked element inside a repeater
	{
		const { pageId, bodyId } = await builder.page('followups-linked-repeater');
		const rowId              = await builder.element(pageId, bodyId, 'Div', {
			_html_class:   'followups-linked-row',
			dataKey:       'row',
			functionQuery: "sort(find('Item', 'category', 'linked-rows'), 'name')"
		});

		const labelId  = await builder.element(pageId, rowId, 'Span', { _html_class: 'followups-linked-label' });
		const buttonId = await builder.element(pageId, rowId, 'Button', { _html_class: 'followups-linked-row-button', _html_type: 'button' });

		await builder.text(pageId, labelId, '${row.name}: ${row.status}');
		await builder.text(pageId, buttonId, 'Done');

		await addStatusUpdate(buttonId, '${row.id}', { successBehaviour: 'partial-refresh-linked', successTargets: [ labelId ] });
	}

	// raise a custom event
	{
		const { pageId, bodyId } = await builder.page('followups-event');

		await addItemCreation(await addButton(pageId, bodyId, 'followups-event-success'), 'event-item', {
			successBehaviour: 'fire-event',
			successEvent:     'followups-item-created'
		});

		await builder.actionMapping(await addButton(pageId, bodyId, 'followups-event-failure'), {
			event:            'click',
			action:           'method',
			method:           'followUpsMissingFunction',
			failureBehaviour: 'fire-event',
			failureEvent:     'followups-item-failed'
		});
	}

	// show/hide sections by CSS selector
	{
		const { pageId, bodyId } = await builder.page('followups-show-hide');

		await addPanel(pageId, bodyId, { _html_id: 'followups-show-hide-shown', _html_class: 'hidden' }, 'Shown after the action');
		await addPanel(pageId, bodyId, { _html_id: 'followups-show-hide-hidden' }, 'Hidden after the action');
		await addPanel(pageId, bodyId, { _html_class: 'followups-show-hide-extra' }, 'Extra 1');
		await addPanel(pageId, bodyId, { _html_class: 'followups-show-hide-extra' }, 'Extra 2');

		await builder.actionMapping(await addButton(pageId, bodyId, 'followups-show-hide-button'), {
			event:            'click',
			action:           'method',
			method:           'followUpsSucceed',
			successBehaviour: 'show-hide-section',
			successShow:      '#followups-show-hide-shown',
			successHide:      '#followups-show-hide-hidden, .followups-show-hide-extra'
		});
	}

	// show a section and load it from a URL built from the result
	{
		const { pageId, bodyId } = await builder.page('followups-show-url');

		await addPanel(pageId, bodyId, { _html_id: 'followups-show-url-detail', _html_class: 'hidden' }, '${current.name}|${request.mode}');
		await addItemCreation(await addButton(pageId, bodyId, 'followups-show-url-button'), 'shown-item', {
			successBehaviour: 'show-hide-section',
			successShow:      '#followups-show-url-detail',
			successURL:       '/followups-show-url/{result.id}?mode=created'
		});
	}

	// show/hide restricted to the current repeater element, and the same without the restriction
	for (const scoped of [ true, false ]) {

		const prefix             = scoped ? 'followups-scope' : 'followups-unscoped';
		const { pageId, bodyId } = await builder.page(prefix);
		const rowId              = await builder.element(pageId, bodyId, 'Div', {
			_html_class:   `${prefix}-row`,
			dataKey:       'entry',
			functionQuery: "sort(find('Item', 'category', 'scope-rows'), 'name')"
		});

		await addPanel(pageId, rowId, { _html_class: `${prefix}-detail hidden` }, '${entry.name}');

		const buttonId = await builder.element(pageId, rowId, 'Button', { _html_class: `${prefix}-button`, _html_type: 'button' });

		await builder.text(pageId, buttonId, 'Show');
		await builder.actionMapping(buttonId, {
			event:            'click',
			action:           'method',
			method:           'followUpsSucceed',
			successBehaviour: 'show-hide-section',
			successShow:      `.${prefix}-detail`,
			successScope:     scoped ? 'repeater' : ''
		});
	}

	// show/hide linked elements
	{
		const { pageId, bodyId } = await builder.page('followups-show-hide-linked');
		const shownId            = await addPanel(pageId, bodyId, { _html_class: 'followups-linked-shown hidden' }, 'Linked shown');
		const hiddenId           = await addPanel(pageId, bodyId, { _html_class: 'followups-linked-hidden' }, 'Linked hidden');

		await builder.actionMapping(await addButton(pageId, bodyId, 'followups-show-hide-linked-button'), {
			event:              'click',
			action:             'method',
			method:             'followUpsSucceed',
			successBehaviour:   'show-hide-section-linked',
			successTargets:     [ shownId ],
			successHideTargets: [ hiddenId ]
		});
	}

	// success and failure follow-ups of a failing action
	{
		const { pageId, bodyId } = await builder.page('followups-failure');

		await addPanel(pageId, bodyId, { _html_id: 'followups-failure-ok', _html_class: 'hidden' }, 'Succeeded');
		await addPanel(pageId, bodyId, { _html_id: 'followups-failure-error', _html_class: 'hidden' }, 'Failed');

		await builder.actionMapping(await addButton(pageId, bodyId, 'followups-failure-button'), {
			event:            'click',
			action:           'method',
			method:           'followUpsMissingFunction',
			successBehaviour: 'show-hide-section',
			successShow:      '#followups-failure-ok',
			failureBehaviour: 'show-hide-section',
			failureShow:      '#followups-failure-error'
		});
	}

	// sign out
	{
		const { pageId, bodyId } = await builder.page('followups-sign-out');
		const buttonId           = await addButton(pageId, bodyId, 'followups-sign-out-button');

		await addStatusUpdate(buttonId, items.signOut, { successBehaviour: 'sign-out' });
	}
});

// Clicks a button whose update action has the follow-up "none" and checks that the item is updated,
// but no partial reload request is sent and the document is not replaced.
test('none leaves the page alone after the action', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-none');
	await markDocument(page);

	const reloads = [];
	page.on('request', request => {
		if (request.url().includes('/structr/html/')) {
			reloads.push(request.url());
		}
	});

	const response = nextActionResponse(page);

	await page.locator('#followups-none-button').click();

	expect((await response).status()).toBe(200);

	await expect.poll(async () => (await builder.get('Item', items.none)).status).toBe('done');

	// give a (wrong) follow-up the chance to happen
	await page.waitForTimeout(1000);

	expect(reloads).toHaveLength(0);
	expect(await documentWasReplaced(page)).toBe(false);
});

// Clicks a button that sets an item's status to "done" with a full page reload as follow-up and checks
// that the document is loaded again and now renders the new status.
test('full page reload shows the changed data', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-full-reload');

	await expect(page.locator('#followups-full-reload-status')).toHaveText('open');

	await markDocument(page);
	await page.locator('#followups-full-reload-button').click();

	await expect.poll(() => documentWasReplaced(page)).toBe(true);
	await expect(page.locator('#followups-full-reload-status')).toHaveText('done');
	await expect(page).toHaveURL(/\/followups-full-reload$/);
});

// Creates an item and navigates to /followups-target/{result.id}; checks that the URL carries the uuid
// from the response and that the target page renders the created item as its current object.
test('navigate to a new page resolves {result.id} from the created object', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-navigate');

	const response = nextActionResponse(page);

	await page.locator('#followups-navigate-id').click();

	const created = (await (await response).json()).result;

	expect(created.id).toMatch(UUID_PATTERN);

	await expect(page).toHaveURL(new RegExp(`/followups-target/${created.id}$`));
	await expect(page.locator('#target-current')).toHaveText('navigated-item');
});

// Calls a method returning the string "alpha" and navigates to /followups-target?value={result};
// checks the resulting URL and that the target page shows the request parameter.
test('navigate to a new page inserts a scalar method result with {result}', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-navigate');

	await page.locator('#followups-navigate-scalar').click();

	await expect(page).toHaveURL(/\/followups-target\?value=alpha$/);
	await expect(page.locator('#target-value')).toHaveText('alpha');
});

// Calls a method returning ['first', 'second'] and navigates with {result} in the URL; checks that
// the list is inserted as "first,second".
test('navigate to a new page inserts a list of plain values as a comma-separated list', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-navigate');

	await page.locator('#followups-navigate-list').click();

	await expect(page).toHaveURL(/\/followups-target\?value=first,second$/);
	await expect(page.locator('#target-value')).toHaveText('first,second');
});

// Creates an item and navigates with {result.missing} in the URL; checks that the missing path
// becomes an empty value instead of "undefined".
test('navigate to a new page replaces a path missing in the result with an empty string', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-navigate');

	await page.locator('#followups-navigate-missing').click();

	await expect(page).toHaveURL(/\/followups-target\?value=$/);
	await expect(page.locator('#target-value')).toHaveText('');
});

// Uses a URL with both ${me.name} and {result.id}: checks that the template expression is already
// resolved in the rendered attribute and the placeholder after the action, in the final URL.
test('navigate to a new page combines template expressions with result placeholders', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-navigate');

	// the template expression is already resolved in the rendered attribute, the placeholder is not
	await expect(page.locator('#followups-navigate-template')).toHaveAttribute('data-structr-success-target', 'url:/followups-target/{result.id}?value=admin');

	const response = nextActionResponse(page);

	await page.locator('#followups-navigate-template').click();

	const created = (await (await response).json()).result;

	await expect(page).toHaveURL(new RegExp(`/followups-target/${created.id}\\?value=admin$`));
	await expect(page.locator('#target-current')).toHaveText('template-item');
	await expect(page.locator('#target-value')).toHaveText('admin');
});

// Updates an item and refreshes the panel #followups-refresh-id-panel; checks that the reload request
// targets that panel's uuid, the panel shows the new status, and the document itself stays.
test('partial refresh by id re-renders the section with the changed data', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-refresh-id');
	await markDocument(page);

	const panel = page.locator('#followups-refresh-id-panel');

	await expect(panel).toHaveText('Status: open');

	const reload = nextPartialReload(page);

	await page.locator('#followups-refresh-id-button').click();

	const panelId = await panel.getAttribute('data-structr-id');

	expect((await reload).url()).toContain(`/structr/html/${panelId}`);

	await expect(panel).toHaveText('Status: done');

	// only the section was replaced, not the document
	expect(await documentWasReplaced(page)).toBe(false);
});

// Refreshes by the class selector .followups-refresh-class-panel, which matches two panels; checks
// that both show the new status.
test('partial refresh by class re-renders every matching section', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-refresh-class');

	const panels = page.locator('.followups-refresh-class-panel');

	await expect(panels).toHaveText([ 'First: open', 'Second: open' ]);

	await page.locator('#followups-refresh-class-button').click();

	await expect(panels).toHaveText([ 'First: done', 'Second: done' ]);
});

// Refreshes two panels given as a comma-separated selector list and checks that both show the new status.
test('partial refresh re-renders all of several comma-separated selectors', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-refresh-multi');

	await page.locator('#followups-refresh-multi-button').click();

	await expect(page.locator('#followups-refresh-multi-a')).toHaveText('A: done');
	await expect(page.locator('#followups-refresh-multi-b')).toHaveText('B: done');
});

// Refreshes a panel with the selector '#...:not(.excluded)' and checks that the item is updated and
// the panel is re-rendered with the new status.
test('partial refresh accepts a selector that contains a colon', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-refresh-colon');

	const panel = page.locator('#followups-refresh-colon-panel');

	await expect(panel).toHaveText('Status: open');

	await page.locator('#followups-refresh-colon-button').click();

	await expect.poll(async () => (await builder.get('Item', items.refreshColon)).status).toBe('done');
	await expect(panel).toHaveText('Status: done');
});

// Creates an item and refreshes a panel that renders ${request.name}; checks that name and id of the
// created item are parameters of the reload request and that the panel shows the name.
test('partial refresh passes the properties of the result as request parameters', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-refresh-params');

	const response = nextActionResponse(page);
	const reload   = nextPartialReload(page);

	await page.locator('#followups-refresh-params-button').click();

	const created    = (await (await response).json()).result;
	const parameters = reloadParameters(await reload);

	expect(parameters.get('name')).toBe('reload-parameter-item');
	expect(parameters.get('id')).toBe(created.id);

	await expect(page.locator('#followups-refresh-params-panel')).toHaveText('Name: reload-parameter-item');
});

// Calls a method returning the string "alpha" with a partial refresh as follow-up and checks that
// the reload request has no per-character parameters 0, 1, 2, ...
test('partial refresh does not turn a scalar method result into index parameters', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-refresh-params');

	const reload = nextPartialReload(page);

	await page.locator('#followups-refresh-scalar-button').click();

	const parameters = reloadParameters(await reload);

	for (const index of [ '0', '1', '2', '3', '4' ]) {
		expect(parameters.has(index), `parameter ${index}`).toBe(false);
	}
});

// Calls a missing method (422) with a partial refresh as failure follow-up and checks that code,
// message and errors of the error response are not passed as reload parameters.
test('a failure refresh does not receive the error body as request parameters', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-refresh-params');

	const response = nextActionResponse(page);
	const reload   = nextPartialReload(page);

	await page.locator('#followups-refresh-failure-button').click();

	expect((await response).status()).toBe(422);

	const parameters = reloadParameters(await reload);

	expect(parameters.has('code')).toBe(false);
	expect(parameters.has('message')).toBe(false);
	expect(parameters.has('errors')).toBe(false);
});

// Refreshes a panel and records structr-reload with a capturing and a bubbling document listener;
// checks that the event fires once on the new panel and does not bubble.
test('a refreshed section dispatches structr-reload on the new node without bubbling', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-reload-event');
	await recordReloadEvents(page);

	await page.locator('#followups-reload-event-button').click();

	await expect(page.locator('#followups-reload-event-panel')).toHaveText('Status: done');
	await expect.poll(() => page.evaluate(() => window.__capturedReloads)).toEqual([ 'followups-reload-event-panel' ]);

	expect(await page.evaluate(() => window.__bubbledReloads)).toEqual([]);
});

// Creates an item with a partial refresh and the option updateHistory; checks that the panel shows
// the reload parameter and that the browser URL now contains name=history-item.
test('the updateHistory option puts the reload parameters into the browser URL', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-history');

	await page.locator('#followups-history-button').click();

	await expect(page.locator('#followups-history-panel')).toHaveText('Name: history-item');
	await expect(page).toHaveURL(/[?&]name=history-item(&|$)/);
});

// Updates an item with partial-refresh-linked pointing at a panel and checks that the panel is
// re-rendered with the new status.
test('partial refresh of a linked element re-renders that element', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-linked');

	const panel = page.locator('.followups-linked-panel');

	await expect(panel).toHaveText('Status: open');

	await page.locator('#followups-linked-button').click();

	await expect(panel).toHaveText('Status: done');
});

// Clicks the button in the second row of a repeater whose linked target is the row's label; checks
// that only that row's label and item change, and the first row still shows its own data.
test('partial refresh of a linked element in a repeater keeps every row in its own context', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-linked-repeater');

	const labels = page.locator('.followups-linked-label');

	await expect(labels).toHaveText([ 'linked-row-a: open', 'linked-row-b: open' ]);

	await page.locator('.followups-linked-row-button').nth(1).click();

	await expect(labels).toHaveText([ 'linked-row-a: open', 'linked-row-b: done' ]);

	const rows = await builder.find('Item', { category: 'linked-rows' });

	expect(rows.find(row => row.name === 'linked-row-a').status).toBe('open');
	expect(rows.find(row => row.name === 'linked-row-b').status).toBe('done');
});

// Creates an item with a fire-event follow-up and records the event on the document; checks target,
// that detail.result equals the result of the response, and the status and element in the detail.
test('raise a custom event passes the result, the status and the element', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-event');
	await recordEvents(page, [ 'followups-item-created' ]);

	const response = nextActionResponse(page);

	await page.locator('#followups-event-success').click();

	const actionResponse = await response;
	const body           = await actionResponse.json();

	await expect.poll(() => recordedEvents(page)).toHaveLength(1);

	const [ event ] = await recordedEvents(page);

	expect(event.target).toBe('followups-event-success');
	expect(event.detail.result).toEqual(body.result);
	expect(event.detail.result.name).toBe('event-item');
	expect(event.detail.status).toBe(actionResponse.status());
	expect(event.detail.element).toBe('followups-event-success');
});

// Calls a missing method with a fire-event failure follow-up and checks that the failure event arrives
// with status 422 and the trigger element in its detail.
test('raise a custom event on failure carries the error status', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-event');
	await recordEvents(page, [ 'followups-item-failed' ]);

	await page.locator('#followups-event-failure').click();

	await expect.poll(() => recordedEvents(page)).toHaveLength(1);

	const [ event ] = await recordedEvents(page);

	expect(event.target).toBe('followups-event-failure');
	expect(event.detail.status).toBe(422);
	expect(event.detail.element).toBe('followups-event-failure');
});

// Runs an action with show-hide-section by CSS selectors and checks that the shown section becomes
// visible and every element of the comma-separated hide list is hidden.
test('show/hide by selector shows and hides the matching sections', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-show-hide');

	const shown  = page.locator('#followups-show-hide-shown');
	const hidden = page.locator('#followups-show-hide-hidden');
	const extras = page.locator('.followups-show-hide-extra');

	await expect(shown).toBeHidden();
	await expect(hidden).toBeVisible();
	await expect(extras).toHaveCount(2);

	await page.locator('#followups-show-hide-button').click();

	await expect(shown).toBeVisible();
	await expect(hidden).toBeHidden();
	await expect(extras.nth(0)).toBeHidden();
	await expect(extras.nth(1)).toBeHidden();
});

// Creates an item and shows a hidden section loaded from /followups-show-url/{result.id}?mode=created;
// checks the reload path and parameters, the rendered content, visibility and the browser URL.
test('show/hide with a URL loads the shown section with the current object and the query parameters', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-show-url');

	const detail = page.locator('#followups-show-url-detail');

	await expect(detail).toBeHidden();

	const response = nextActionResponse(page);
	const reload   = nextPartialReload(page);

	await page.locator('#followups-show-url-button').click();

	const created = (await (await response).json()).result;
	const request = await reload;

	expect(new URL(request.url()).pathname).toMatch(new RegExp(`/structr/html/[0-9a-f]{32}/${created.id}$`));
	expect(reloadParameters(request).get('mode')).toBe('created');

	await expect(detail).toBeVisible();
	await expect(detail).toHaveText('shown-item|created');

	// the shown section is bound to the assembled URL, so the browser URL follows it
	await expect(page).toHaveURL(new RegExp(`/followups-show-url/${created.id}\\?mode=created$`));
});

// Clicks the button in the second of three repeater rows with scope "repeater" and checks that only
// that row's detail section becomes visible.
test('show/hide restricted to the current repeater element only shows that row', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-scope');

	const details = page.locator('.followups-scope-detail');

	await expect(details).toHaveCount(3);

	await page.locator('.followups-scope-button').nth(1).click();

	await expect(details.nth(1)).toBeVisible();
	await expect(details.nth(1)).toHaveText('scope-row-b');
	await expect(details.nth(0)).toBeHidden();
	await expect(details.nth(2)).toBeHidden();
});

// Control case for the scope test: without the scope, a click in one row shows the detail section
// in all three rows.
test('show/hide without the repeater restriction shows the section in every row', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-unscoped');

	const details = page.locator('.followups-unscoped-detail');

	await expect(details).toHaveCount(3);

	await page.locator('.followups-unscoped-button').nth(1).click();

	for (let i = 0; i < 3; i++) {
		await expect(details.nth(i)).toBeVisible();
	}
});

// Runs an action with show-hide-section-linked and checks that the linked show target becomes visible.
test('show/hide of linked elements shows the linked element', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-show-hide-linked');

	const shown = page.locator('.followups-linked-shown');

	await expect(shown).toBeHidden();

	await page.locator('#followups-show-hide-linked-button').click();

	await expect(shown).toBeVisible();
});

// Runs the same show-hide-section-linked action and checks that the linked hide target is hidden
// while the show target becomes visible.
test('show/hide of linked elements hides the linked element', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-show-hide-linked');

	const hidden = page.locator('.followups-linked-hidden');

	await expect(hidden).toBeVisible();

	await page.locator('#followups-show-hide-linked-button').click();

	await expect(page.locator('.followups-linked-shown')).toBeVisible();
	await expect(hidden).toBeHidden();
});

// Calls a missing method (422) with show/hide follow-ups for success and failure; checks that only
// the failure section is shown.
test('a failing action runs the failure follow-up and not the success follow-up', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-failure');

	const response = nextActionResponse(page);

	await page.locator('#followups-failure-button').click();

	expect((await response).status()).toBe(422);

	await expect(page.locator('#followups-failure-error')).toBeVisible();
	await expect(page.locator('#followups-failure-ok')).toBeHidden();
});

// Signed in as admin, updates an item with the sign-out follow-up; checks that /structr/logout is
// posted, the item is updated, /structr/rest/me answers 401 and the non-public page is gone.
test('sign out ends the session after the action', async ({ page }) => {

	await login(page);
	await openPage(page, 'followups-sign-out');

	expect((await page.request.get(process.env.BASE_URL + '/structr/rest/me')).status()).toBe(200);

	const logout = page.waitForRequest(request => request.url().includes('/structr/logout') && request.method() === 'POST');

	await page.locator('#followups-sign-out-button').click();
	await logout;

	await expect.poll(async () => (await builder.get('Item', items.signOut)).status).toBe('done');
	await expect.poll(async () => (await page.request.get(process.env.BASE_URL + '/structr/rest/me')).status()).toBe(401);

	// the page is not public, so after the reload the signed-out visitor no longer sees it
	await expect(page.locator('#followups-sign-out-button')).toHaveCount(0);
});
