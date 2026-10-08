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
import {EamBuilder, nextActionRequest, nextActionResponse, openPage, recordedEvents, recordEvents, UUID_PATTERN} from './helpers/eam';

/**
 * The server actions of Event Action Mapping as seen from the browser: a click on a wired element
 * sends POST /structr/rest/DOMElement/<id>/event, and these tests check the payload frontend.js
 * builds, the response, and what the action did to the database or the page.
 */

let builder;

const ids = {};

// a 32 character hex string that is no object's uuid
const MISSING_UUID = '0123456789abcdef0123456789abcdef';

/** A page holding one button that triggers the given action mapping; returns the ids of everything created. */
async function buttonPage(name, buttonId, mapping, parameters = []) {

	const { pageId, bodyId } = await builder.page(name);
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

/** Calls the user-defined function storedKeys over REST: the property names stored on the node with the given uuid, as JSON text. */
async function storedKeys(id) {

	const response = await builder.context.post(process.env.BASE_URL + '/structr/rest/storedKeys', { data: JSON.stringify({ id: id }) });

	expect(response.ok()).toBeTruthy();

	return JSON.stringify((await response.json()).result);
}

test.beforeAll(async ({ playwright }) => {

	builder = await EamBuilder.create(playwright);

	await builder.schemaType('Item', [
		{ name: 'priority', propertyType: 'Integer' },
		{ name: 'markup',   propertyType: 'String' }
	], [
		{ name: 'describe',   source: "{ return 'Item ' + $.this.name + ' / ' + $.arguments.suffix; }" },
		{ name: 'rawSummary', source: '{ return { name: $.this.name, raw: true }; }', returnRawResult: true },
		{ name: 'summarize',  source: '{ return $.arguments; }', isStatic: true }
	]);

	await builder.schemaType('Note', [
		{ name: 'title', propertyType: 'String', notNull: true }
	]);

	// user-defined functions
	await builder.userFunction('greet',         "{ return 'Hello ' + $.arguments.name; }");
	await builder.userFunction('echoArguments', '{ return $.arguments; }');
	await builder.userFunction('returnString',  "{ return 'plain text'; }");
	await builder.userFunction('returnNumber',  '{ return 42; }');
	await builder.userFunction('returnObject',  "{ return { label: 'object', count: 2 }; }");
	await builder.userFunction('returnArray',   "{ return ['a', 'b']; }");
	await builder.userFunction('returnNull',    '{ return null; }');
	await builder.userFunction('storedKeys',    "{ return $.cypher('MATCH (n) WHERE n.id = $id RETURN keys(n) AS keys', { id: $.arguments.id }); }");

	// give the schema a moment to settle before the pages refer to the new types
	await new Promise(resolve => setTimeout(resolve, 1000));

	ids.updateCurrentItem = await builder.node('Item', { name: 'Item to rename' });
	ids.updateLiteralItem = await builder.node('Item', { name: 'Item by uuid', priority: 1 });
	ids.multipleItemA     = await builder.node('Item', { name: 'First of two', priority: 1 });
	ids.multipleItemB     = await builder.node('Item', { name: 'Second of two', priority: 1 });
	ids.deleteItem        = await builder.node('Item', { name: 'Item to delete' });
	ids.instanceItem      = await builder.node('Item', { name: 'Instance item' });
	ids.rawItem           = await builder.node('Item', { name: 'Raw item' });
	ids.insertSource      = await builder.node('Item', { name: 'Insert source',  markup: '<p class="inserted">Inserted markup</p>' });
	ids.replaceSource     = await builder.node('Item', { name: 'Replace source', markup: '<p class="replacement">Replacement markup</p>' });

	// create
	await buttonPage('actions-create', 'create-item', { action: 'create', dataType: 'Item' }, [
		constant('name', 'Created item'),
		constant('priority', '3')
	]);

	await buttonPage('actions-create-dynamic-type', 'create-dynamic', { action: 'create', dataType: 'AbstractNode' }, [
		constant('type', 'Item'),
		constant('name', 'Dynamically typed item')
	]);

	await buttonPage('actions-create-without-type', 'create-without-type', { action: 'create' }, [
		constant('name', 'Never created')
	]);

	await buttonPage('actions-create-invalid', 'create-invalid', { action: 'create', dataType: 'Note' });

	await buttonPage('actions-create-hygiene', 'create-hygiene', { action: 'create', dataType: 'Item' }, [
		constant('name', 'Hygiene item'),
		constant('colour', 'green')
	]);

	// update
	{
		const { pageId, bodyId, actionMapping } = await buttonPage('actions-update-current', 'update-current', { action: 'update', idExpression: '${current.id}' });
		const input = await builder.element(pageId, bodyId, 'Input', { _html_id: 'update-name', _html_value: '${current.name}' });

		await builder.parameter(actionMapping, { parameterType: 'user-input', parameterName: 'name', inputElement: input });
	}

	await buttonPage('actions-update-literal', 'update-literal', { action: 'update', idExpression: ids.updateLiteralItem }, [
		constant('priority', '7')
	]);

	await buttonPage('actions-update-multiple', 'update-multiple', { action: 'update', idExpression: `${ids.multipleItemA},${ids.multipleItemB}` }, [
		constant('priority', '9')
	]);

	await buttonPage('actions-update-missing', 'update-missing', { action: 'update', idExpression: MISSING_UUID }, [
		constant('name', 'Should not exist')
	]);

	// delete
	await buttonPage('actions-delete', 'delete-item', { action: 'delete', idExpression: ids.deleteItem });

	// methods
	await buttonPage('actions-method-function', 'call-function', { action: 'method', method: 'greet' }, [
		constant('name', 'World')
	]);

	{
		const { pageId, bodyId } = await builder.page('actions-method-results');

		for (const name of [ 'returnString', 'returnNumber', 'returnObject', 'returnArray', 'returnNull' ]) {

			const button = await builder.element(pageId, bodyId, 'Button', { _html_id: name });

			await builder.text(pageId, button, name);
			await builder.actionMapping(button, { event: 'click', action: 'method', method: name });
		}
	}

	await buttonPage('actions-method-instance', 'call-instance', { action: 'method', method: 'describe', idExpression: '${current.id}' }, [
		constant('suffix', 'detail')
	]);

	await buttonPage('actions-method-static', 'call-static', { action: 'method', method: 'summarize', idExpression: 'Item' }, [
		constant('note', 'from the page')
	]);

	await buttonPage('actions-method-arguments', 'call-echo', { action: 'method', method: 'echoArguments' }, [
		constant('note', 'from the page')
	]);

	await buttonPage('actions-method-raw', 'call-raw', { action: 'method', method: 'rawSummary', idExpression: '${current.id}' });

	await buttonPage('actions-method-unknown', 'call-unknown', { action: 'method', method: 'doesNotExist' });

	// client-side lifecycle
	await buttonPage('actions-lifecycle', 'lifecycle', { action: 'method', method: 'returnString' });

	// DOM actions, only configurable over REST
	{
		const { pageId, bodyId, actionMapping } = await buttonPage('actions-append-child', 'append-child', { action: 'append-child' });
		const list   = await builder.element(pageId, bodyId, 'Ul', { _html_id: 'append-target' });
		const orphan = await builder.node('Li', { tag: 'li', pageId: pageId, _html_id: 'appended-child' });

		await builder.text(pageId, orphan, 'Appended child');
		await builder.update('ActionMapping', actionMapping, { idExpression: list });
		await builder.parameter(actionMapping, constant('childId', orphan));
	}

	{
		const { pageId, bodyId, actionMapping } = await buttonPage('actions-remove-child', 'remove-child', { action: 'remove-child' });
		const list  = await builder.element(pageId, bodyId, 'Ul', { _html_id: 'remove-target' });
		const child = await builder.element(pageId, list, 'Li', { _html_id: 'removed-child' });

		await builder.text(pageId, child, 'Child to remove');
		await builder.update('ActionMapping', actionMapping, { idExpression: list });
		await builder.parameter(actionMapping, constant('childId', child));
	}

	{
		const { pageId, bodyId, actionMapping } = await buttonPage('actions-insert-html', 'insert-html', { action: 'insert-html' });
		const container = await builder.element(pageId, bodyId, 'Div', { _html_id: 'insert-target' });

		await builder.update('ActionMapping', actionMapping, { idExpression: container });
		await builder.parameter(actionMapping, constant('sourceObject', ids.insertSource));
		await builder.parameter(actionMapping, constant('sourceProperty', 'markup'));
	}

	{
		const { pageId, bodyId, actionMapping } = await buttonPage('actions-replace-html', 'replace-html', { action: 'replace-html' });
		const container = await builder.element(pageId, bodyId, 'Div', { _html_id: 'replace-target' });
		const child     = await builder.element(pageId, container, 'P', { _html_id: 'replaced-child' });

		await builder.text(pageId, child, 'Child to replace');
		await builder.update('ActionMapping', actionMapping, { idExpression: container });
		await builder.parameter(actionMapping, constant('childId', child));
		await builder.parameter(actionMapping, constant('sourceObject', ids.replaceSource));
		await builder.parameter(actionMapping, constant('sourceProperty', 'markup'));
	}
});

test.describe('create', () => {

	// Clicks a create button for type Item with the constants name and priority '3': checks the payload, that the
	// response returns the new Item (id, type, name), and that priority is stored converted to the integer 3.
	test('creates an object of the data type from the parameters and returns it', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-create');

		const request  = nextActionRequest(page);
		const response = await clickAndWait(page, '#create-item');
		const payload  = (await request).postDataJSON();

		// constant values are sent as strings, the server converts them to the property type
		expect(payload.name).toBe('Created item');
		expect(payload.priority).toBe('3');
		expect(payload.structrDataType).toBe('Item');

		expect(response.status()).toBe(200);

		const result = (await response.json()).result;

		expect(result.id).toMatch(UUID_PATTERN);
		expect(result.type).toBe('Item');
		expect(result.name).toBe('Created item');

		const stored = await builder.get('Item', result.id);

		expect(stored.priority).toBe(3);
	});

	// Clicks a create button with data type AbstractNode and a constant type=Item, as the docs describe for dynamic
	// type selection: expects 200, a result of type Item and the Item in the database.
	test('creates the type given in the type parameter when the data type is AbstractNode', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-create-dynamic-type');

		const response = await clickAndWait(page, '#create-dynamic');

		expect(response.status()).toBe(200);
		expect((await response.json()).result.type).toBe('Item');

		await expect.poll(async () => (await builder.find('Item', { name: 'Dynamically typed item' })).length).toBe(1);
	});

	// Clicks a create button whose mapping has no data type: expects no structrDataType in the payload, a 422
	// response and no Item created.
	test('is rejected without a data type', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-create-without-type');

		const request  = nextActionRequest(page);
		const response = await clickAndWait(page, '#create-without-type');

		expect((await request).postDataJSON().structrDataType).toBeUndefined();
		expect(response.status()).toBe(422);
		expect((await response.json()).message).toContain('create');

		expect(await builder.find('Item', { name: 'Never created' })).toHaveLength(0);
	});

	// Clicks a create button for type Note without a value for the notNull property title: expects 422 with a
	// validation error for Note.title and no Note in the database.
	test('returns the validation errors and creates nothing when a required property is missing', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-create-invalid');

		const response = await clickAndWait(page, '#create-invalid');

		expect(response.status()).toBe(422);

		const body = await response.json();

		expect(body.errors.length).toBeGreaterThan(0);
		expect(body.errors[0].type).toBe('Note');
		expect(body.errors[0].property).toBe('title');
		expect(body.errors[0].token).toBeTruthy();

		expect(await builder.find('Note')).toHaveLength(0);
	});

	// Creates an Item with the constants name and colour, then reads the stored property keys: none of the structr*
	// keys the browser sent may be stored, name must be, and the unknown key colour is stored as well.
	test('stores the mapped parameters but none of the internal structr keys of the payload', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-create-hygiene');

		const request  = nextActionRequest(page);
		const response = await clickAndWait(page, '#create-hygiene');
		const payload  = (await request).postDataJSON();

		// the browser does send the element configuration ...
		expect(payload.structrId).toMatch(UUID_PATTERN);
		expect(payload.structrAction).toBe('create');
		expect(payload.structrDataType).toBe('Item');

		expect(response.status()).toBe(200);

		const keys = await storedKeys((await response.json()).result.id);

		// ... but the server strips it before the rest becomes properties
		for (const key of Object.keys(payload).filter(key => key.startsWith('structr'))) {
			expect(keys).not.toContain(`"${key}"`);
		}

		expect(keys).toContain('"name"');

		// a key the type does not know is stored like any other property under the default
		// input validation mode (accept_warn); this pins that behaviour, it is configurable
		expect(keys).toContain('"colour"');
	});
});

test.describe('update', () => {

	// Opens a details page for an Item, changes the name input and clicks update with idExpression ${current.id}:
	// expects the resolved uuid and the new name in the payload and the renamed Item in the database.
	test('updates the current object of a details page from user input', async ({ page }) => {

		await login(page);
		await openPage(page, `actions-update-current/${ids.updateCurrentItem}`);

		await expect(page.locator('#update-name')).toHaveValue('Item to rename');

		await page.locator('#update-name').fill('Renamed item');

		const request  = nextActionRequest(page);
		const response = await clickAndWait(page, '#update-current');
		const payload  = (await request).postDataJSON();

		// ${current.id} is resolved when the page renders, the browser only sees the uuid
		expect(payload.structrIdExpression).toBe(ids.updateCurrentItem);
		expect(payload.name).toBe('Renamed item');

		expect(response.status()).toBe(200);

		await expect.poll(async () => (await builder.get('Item', ids.updateCurrentItem)).name).toBe('Renamed item');
	});

	// Clicks an update button whose idExpression is a literal uuid and sets priority 7: expects the new priority
	// and the unchanged name in the database.
	test('updates the object given by a literal uuid and leaves its other properties alone', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-update-literal');

		const response = await clickAndWait(page, '#update-literal');

		expect(response.status()).toBe(200);

		const stored = await builder.get('Item', ids.updateLiteralItem);

		expect(stored.priority).toBe(7);
		expect(stored.name).toBe('Item by uuid');
	});

	// Clicks an update button whose idExpression lists two uuids separated by a comma: expects priority 9 on both Items.
	test('updates every object of a comma-separated uuid list', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-update-multiple');

		const response = await clickAndWait(page, '#update-multiple');

		expect(response.status()).toBe(200);
		expect((await builder.get('Item', ids.multipleItemA)).priority).toBe(9);
		expect((await builder.get('Item', ids.multipleItemB)).priority).toBe(9);
	});

	// Clicks an update button whose idExpression is a uuid without an object: expects 200 and that no Item is created.
	test('a uuid that matches no object is ignored without an error', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-update-missing');

		const response = await clickAndWait(page, '#update-missing');

		// pins current behaviour: resolveDataTargets skips unknown uuids silently, so the action succeeds with nothing to update
		expect(response.status()).toBe(200);
		expect(await builder.find('Item', { name: 'Should not exist' })).toHaveLength(0);
	});
});

test.describe('delete', () => {

	// Clicks a delete button for an existing Item: expects 200 and the Item gone from the database.
	test('deletes the object given by its uuid', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-delete');

		expect(await builder.get('Item', ids.deleteItem)).not.toBeNull();

		const response = await clickAndWait(page, '#delete-item');

		expect(response.status()).toBe(200);

		await expect.poll(() => builder.get('Item', ids.deleteItem)).toBeNull();
	});
});

test.describe('method', () => {

	// Clicks a button calling the user-defined function greet with the constant name=World: expects the method name
	// in the payload and 'Hello World' as the result.
	test('calls a user-defined function with the mapped parameters and returns its result', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-method-function');

		const request  = nextActionRequest(page);
		const response = await clickAndWait(page, '#call-function');

		expect((await request).postDataJSON().structrMethod).toBe('greet');
		expect(response.status()).toBe(200);
		expect((await response.json()).result).toBe('Hello World');
	});

	const results = {
		returnString: 'plain text',
		returnNumber: 42,
		returnObject: { label: 'object', count: 2 },
		returnArray:  [ 'a', 'b' ],
		returnNull:   null
	};

	for (const [name, expected] of Object.entries(results)) {

		// For each of the functions returning a string, a number, an object, an array and null: clicks its button and
		// expects 200 with the return value under result.
		test(`wraps the return value of ${name} in the result`, async ({ page }) => {

			await login(page);
			await openPage(page, 'actions-method-results');

			const response = await clickAndWait(page, `#${name}`);

			expect(response.status()).toBe(200);
			expect((await response.json()).result ?? null).toEqual(expected);
		});
	}

	// Opens a details page for an Item and calls its instance method describe via ${current.id} with a constant
	// suffix: expects the method's text built from the Item's name and the suffix.
	test('calls an instance method on the current object', async ({ page }) => {

		await login(page);
		await openPage(page, `actions-method-instance/${ids.instanceItem}`);

		const response = await clickAndWait(page, '#call-instance');

		expect(response.status()).toBe(200);
		expect((await response.json()).result).toBe('Item Instance item / detail');
	});

	// Calls a user-defined function that returns $.arguments, with one constant parameter: expects exactly that
	// parameter and none of the internal keys.
	test('passes only the mapped parameters to a user-defined function', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-method-arguments');

		const response = await clickAndWait(page, '#call-echo');

		expect(response.status()).toBe(200);
		expect((await response.json()).result).toEqual({ note: 'from the page' });
	});

	// Calls the static method Item.summarize (type name as idExpression), which returns $.arguments, with one constant
	// parameter: expects exactly that parameter and none of the internal keys.
	test('passes only the mapped parameters to a static method of the type', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-method-static');

		const request  = nextActionRequest(page);
		const response = await clickAndWait(page, '#call-static');

		// a type name instead of a uuid selects the static method of that type
		expect((await request).postDataJSON().structrIdExpression).toBe('Item');
		expect(response.status()).toBe(200);
		expect((await response.json()).result).toEqual({ note: 'from the page' });
	});

	// Calls an instance method flagged returnRawResult on the current Item: expects the returned object as the
	// whole response body, not wrapped in result.
	test('returns the value of a method with returnRawResult without the result wrapper', async ({ page }) => {

		await login(page);
		await openPage(page, `actions-method-raw/${ids.rawItem}`);

		const response = await clickAndWait(page, '#call-raw');

		expect(response.status()).toBe(200);
		expect(await response.json()).toEqual({ name: 'Raw item', raw: true });
	});

	// Clicks a button calling a method that does not exist: expects 422 with an error message.
	test('an unknown method is answered with 422', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-method-unknown');

		const response = await clickAndWait(page, '#call-unknown');

		expect(response.status()).toBe(422);
		expect((await response.json()).message).toBeTruthy();
	});
});

test.describe('DOM actions', () => {

	// Clicks an append-child button with a list as target and a parentless li as childId: after reloading the page
	// the li is rendered inside the list.
	test('append-child moves the given node into the target element', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-append-child');

		await expect(page.locator('#appended-child')).toHaveCount(0);

		const response = await clickAndWait(page, '#append-child');

		expect(response.status()).toBe(200);

		// the action changes the stored page, the browser sees it on the next render
		await openPage(page, 'actions-append-child');

		await expect(page.locator('#append-target > #appended-child')).toHaveText('Appended child');
	});

	// Clicks a remove-child button with a list as target and its li as childId: after reloading the page the li is
	// gone and the list is still there.
	test('remove-child removes the given node from the target element', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-remove-child');

		await expect(page.locator('#remove-target > #removed-child')).toHaveCount(1);

		const response = await clickAndWait(page, '#remove-child');

		expect(response.status()).toBe(200);

		await openPage(page, 'actions-remove-child');

		await expect(page.locator('#removed-child')).toHaveCount(0);
		await expect(page.locator('#remove-target')).toHaveCount(1);
	});

	// Clicks an insert-html button whose source is an Item property holding a <p> element: after reloading the page
	// the paragraph is rendered inside the target div.
	test('insert-html appends the markup stored in the source property to the target element', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-insert-html');

		const response = await clickAndWait(page, '#insert-html');

		expect(response.status()).toBe(200);

		await openPage(page, 'actions-insert-html');

		await expect(page.locator('#insert-target > p.inserted')).toHaveText('Inserted markup');
	});

	// Clicks a replace-html button for a paragraph in a div, with markup from an Item property: after reloading the
	// page the old paragraph is gone and the new one is rendered in its place.
	test('replace-html replaces the given child with the markup stored in the source property', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-replace-html');

		await expect(page.locator('#replaced-child')).toHaveText('Child to replace');

		const response = await clickAndWait(page, '#replace-html');

		expect(response.status()).toBe(200);

		await openPage(page, 'actions-replace-html');

		await expect(page.locator('#replaced-child')).toHaveCount(0);
		await expect(page.locator('#replace-target > p.replacement')).toHaveText('Replacement markup');
	});
});

test.describe('lifecycle', () => {

	// Holds the action request back while clicking: the button carries structr-action-running and only
	// structr-action-started has fired; after the response the class is gone and structr-action-finished follows.
	test('marks the element as running until the response arrives and announces start and finish', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-lifecycle');
		await recordEvents(page, [ 'structr-action-started', 'structr-action-finished' ]);

		// hold the action request back to observe the running state
		let release;
		const held = new Promise(resolve => release = resolve);

		await page.route('**/structr/rest/DOMElement/*/event', async route => {
			await held;
			await route.continue();
		});

		await page.locator('#lifecycle').click();

		await expect(page.locator('#lifecycle')).toHaveClass(/structr-action-running/);
		expect((await recordedEvents(page)).map(event => event.type)).toEqual([ 'structr-action-started' ]);

		const response = nextActionResponse(page);

		release();

		expect((await response).status()).toBe(200);

		await expect(page.locator('#lifecycle')).not.toHaveClass(/structr-action-running/);
		await expect.poll(async () => (await recordedEvents(page)).map(event => `${event.type}:${event.target}`)).toEqual([
			'structr-action-started:lifecycle',
			'structr-action-finished:lifecycle'
		]);
	});

	// Clicks a button calling an unknown method (422): the running class is removed and both the started and the
	// finished event fire.
	test('a failed action also ends the running state', async ({ page }) => {

		await login(page);
		await openPage(page, 'actions-method-unknown');
		await recordEvents(page, [ 'structr-action-started', 'structr-action-finished' ]);

		const response = await clickAndWait(page, '#call-unknown');

		expect(response.status()).toBe(422);

		await expect(page.locator('#call-unknown')).not.toHaveClass(/structr-action-running/);
		await expect.poll(async () => (await recordedEvents(page)).map(event => event.type)).toEqual([
			'structr-action-started',
			'structr-action-finished'
		]);
	});
});
