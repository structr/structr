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
import fs from 'fs';
import os from 'os';
import path from 'path';
import {pathToFileURL} from 'url';

/**
 * Unit tests for the partial reload logic in frontend.js: the lookup of reload targets by HTML id /
 * CSS class (Frontend.resolveDOMElementId) and the request parameters of the partial reload
 * (Frontend.encodeRequestParameters). No browser and no Structr instance needed: the REST backend
 * is simulated with the search semantics that ReloadTargetLookupTest verifies on the server side
 * (exact match, _inexact=1 means "contains", comma means AND).
 */

const noop       = () => {};
const SHADOW_DOC = 'shadow-document-id';

let activeElementInDocument = null;

// simulated DOMElements (ui view): pageId === null means "in the trash"
let elements = [];
let requests = [];

const fakeFetch = async (url) => {

	requests.push(url);

	const params  = new URLSearchParams(url.substring(url.indexOf('?') + 1));
	const inexact = params.get('_inexact') === '1';
	const result  = elements.filter(e => {

		for (const [key, value] of params) {

			switch (key) {

				case '_inexact':
					break;

				case '_html_class':
					if (!value.split(',').every(part => inexact ? (e._html_class || '').includes(part) : e._html_class === part)) {
						return false;
					}
					break;

				default:
					if (e[key] !== value) {
						return false;
					}
			}
		}

		return true;
	});

	return { ok: true, status: 200, statusText: 'OK', json: async () => ({ result_count: result.length, result: result }) };
};

let frontend;
let originals = {};

/**
 * Returns the source of frontend.js: from the checkout when the tests run next to it, otherwise
 * (Docker image, which only contains the tests) from the Structr instance under test.
 */
async function loadFrontendSource(playwright) {

	const local = path.resolve(__dirname, '../../../main/resources/structr/js/frontend/frontend.js');
	if (fs.existsSync(local)) {

		return fs.readFileSync(local, 'utf8');
	}

	const context  = await playwright.request.newContext();
	const response = await context.get(process.env.BASE_URL + '/structr/js/frontend/frontend.js');

	if (!response.ok()) {

		throw new Error('Cannot load frontend.js from ' + process.env.BASE_URL + ': ' + response.status());
	}

	const source = await response.text();

	await context.dispose();

	return source;
}

test.beforeAll(async ({ playwright }) => {

	const source = await loadFrontendSource(playwright);

	// frontend.js registers itself on window at import time and binds events on document, the
	// globals are restored in afterAll because a worker process can run other spec files afterwards
	originals = { window: globalThis.window, location: globalThis.location, document: globalThis.document, addEventListener: globalThis.addEventListener, fetch: globalThis.fetch };

	globalThis.window   = globalThis;
	globalThis.location = { href: 'http://localhost/page1', search: '' };
	globalThis.document = {
		querySelectorAll: () => [],
		querySelector:    (selector) => (selector === '[data-structr-page]' ? activeElementInDocument : null),
		addEventListener: noop
	};
	globalThis.addEventListener = noop;
	globalThis.fetch            = fakeFetch;

	// the module must be imported as ES module, the nearest package.json says "commonjs"
	const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'structr-frontend-'));
	const copy   = path.join(tmpDir, 'frontend.mjs');

	fs.writeFileSync(copy, source);

	const module = await import(pathToFileURL(copy).href);

	frontend = new module.Frontend();
});

test.afterAll(() => {

	for (const key of Object.keys(originals)) {

		if (originals[key] === undefined) {
			delete globalThis[key];
		} else {
			globalThis[key] = originals[key];
		}
	}
});

test.beforeEach(() => {
	elements                = [];
	requests                = [];
	activeElementInDocument = null;
});

const trigger = (pageId) => ({ dataset: pageId ? { structrPage: pageId } : {} });

test('element with the same HTML id in other pages and in the trash', async () => {

	elements = [
		{ id: 'trash', _html_id: 'content', pageId: null },
		{ id: 'p1',    _html_id: 'content', pageId: 'page1' },
		{ id: 'p2',    _html_id: 'content', pageId: 'page2' }
	];

	expect(await frontend.resolveDOMElementId('#content', trigger('page2'))).toBe('p2');
	expect(requests).toEqual(['/structr/rest/DOMElement/ui?_html_id=content&pageId=page2']);
});

test('partial in a shared component is found via fallback, trash is never used', async () => {

	elements = [
		{ id: 'trash',  _html_id: 'content', pageId: null },
		{ id: 'shared', _html_id: 'content', pageId: SHADOW_DOC }
	];

	expect(await frontend.resolveDOMElementId('#content', trigger('page1'))).toBe('shared');
	expect(requests).toEqual([
		'/structr/rest/DOMElement/ui?_html_id=content&pageId=page1',
		'/structr/rest/DOMElement/ui?_html_id=content'
	]);
});

test('page of the first active element is used if the trigger has none', async () => {

	elements = [
		{ id: 'p1', _html_id: 'content', pageId: 'page1' },
		{ id: 'p2', _html_id: 'content', pageId: 'page2' }
	];

	activeElementInDocument = trigger('page2');

	expect(await frontend.resolveDOMElementId('#content', trigger(null))).toBe('p2');
	expect(requests).toEqual(['/structr/rest/DOMElement/ui?_html_id=content&pageId=page2']);
});

test('trigger re-rendered in a partial of a shared component falls back to the page of another active element', async () => {

	elements = [
		{ id: 'p1', _html_id: 'content', pageId: 'page1' },
		{ id: 'p2', _html_id: 'content', pageId: 'page2' }
	];

	activeElementInDocument = trigger('page1');

	expect(await frontend.resolveDOMElementId('#content', trigger(SHADOW_DOC))).toBe('p1');
	expect(requests).toEqual([
		'/structr/rest/DOMElement/ui?_html_id=content&pageId=shadow-document-id',
		'/structr/rest/DOMElement/ui?_html_id=content&pageId=page1'
	]);
});

test('only element in the trash is not found', async () => {

	elements = [
		{ id: 'trash', _html_id: 'content', pageId: null }
	];

	expect(await frontend.resolveDOMElementId('#content', trigger('page1'))).toBe(null);
	expect(requests.length).toBe(2);
});

test('class selector uses contains search and matches whole class names only', async () => {

	elements = [
		{ id: 'similar', _html_class: 'btn-primary large', pageId: 'page1' },
		{ id: 'exact',   _html_class: 'large btn other',   pageId: 'page1' }
	];

	expect(await frontend.resolveDOMElementId('.btn.large', trigger('page1'))).toBe('exact');
	expect(requests).toEqual(['/structr/rest/DOMElement/ui?_html_class=btn%2Clarge&_inexact=1&pageId=page1']);
});

test('unsupported selector is rejected without request', async () => {

	expect(await frontend.resolveDOMElementId('div > span', trigger('page1'))).toBe(null);
	expect(requests).toEqual([]);
});

test('current object is taken from the reload target', () => {

	const container = { currentObjectId: 'target-current', requestPage: '2' };

	expect(frontend.encodeRequestParameters(container, {}, {}, trigger('page1'))).toBe('/target-current?page=2');
});

test('current object is taken from the trigger if the reload target has none (selector target)', () => {

	const element = { dataset: { structrPage: 'page1', currentObjectId: 'trigger-current' } };

	expect(frontend.encodeRequestParameters({}, {}, {}, element)).toBe('/trigger-current');
	expect(frontend.encodeRequestParameters({ currentObjectId: 'target-current' }, {}, {}, element)).toBe('/target-current');
});

test('current object from the parameters overrides both', () => {

	const element = { dataset: { currentObjectId: 'trigger-current' } };

	expect(frontend.encodeRequestParameters({ currentObjectId: 'target-current' }, { current: 'override-current', sort: 'name' }, {}, element)).toBe('/override-current?sort=name');
});

test('no current object without target, trigger and parameters', () => {

	expect(frontend.encodeRequestParameters({}, {}, {}, trigger('page1'))).toBe('');
	expect(frontend.encodeRequestParameters({}, {}, {}, undefined)).toBe('');
});
