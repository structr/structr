///
/// Copyright (C) 2010-2026 Structr GmbH
///
/// This file is part of Structr <http://structr.org>.
///
/// Structr is free software: you can redistribute it and/or modify
/// it under the terms of the GNU General Public License as
/// published by the Free Software Foundation, either version 3 of the
/// License, or (at your option) any later version.
///
/// Structr is distributed in the hope that it will be useful,
/// but WITHOUT ANY WARRANTY; without even the implied warranty of
/// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/// GNU General Public License for more details.
///
/// You should have received a copy of the GNU General Public License
/// along with Structr.  If not, see <http://www.gnu.org/licenses/>.
///

// @ts-check
import {APIRequestContext, expect, Page, Request, Response} from '@playwright/test';
import {initialize} from './init';

/**
 * Shared setup for the Event Action Mapping (EAM) end-to-end specs (test-eam-*.spec.js).
 *
 * The test pages are built over REST, not through the Pages editor: that is faster, does not
 * depend on the editor's markup, and lets every spec configure exactly the mapping it tests.
 * The browser then loads the rendered page with frontend.js and drives it like a user would.
 */

export const FRONTEND_JS = '/structr/js/frontend/frontend.js';

export const UUID_PATTERN = /^[0-9a-f]{32}$/;

export class EamBuilder {

	readonly context: APIRequestContext;

	// pages created with { publicVisible: true }: every element in them is made public as well
	private publicPages = new Set<string>();

	constructor(context: APIRequestContext) {
		this.context = context;
	}

	/** Clears the database, creates the admin user and returns a builder that talks REST as superadmin. */
	static async create(playwright): Promise<EamBuilder> {
		return new EamBuilder(await initialize(playwright, null));
	}

	/** POSTs one object and returns its uuid; fails loudly so a broken setup does not surface as a confusing test failure. */
	async node(type: string, data: object = {}): Promise<string> {

		const response = await this.context.post(process.env.BASE_URL + `/structr/rest/${type}`, { data: JSON.stringify(data) });

		if (!response.ok()) {
			throw new Error(`creating ${type} failed with status ${response.status()}: ${await response.text()}`);
		}

		return (await response.json()).result[0];
	}

	/** PUTs the given properties onto an existing object. */
	async update(type: string, id: string, data: object): Promise<void> {

		const response = await this.context.put(process.env.BASE_URL + `/structr/rest/${type}/${id}`, { data: JSON.stringify(data) });

		if (!response.ok()) {
			throw new Error(`updating ${type} ${id} failed with status ${response.status()}: ${await response.text()}`);
		}
	}

	/** One object in the given view, or null if it does not exist (any more). */
	async get(type: string, id: string, view: string = 'all'): Promise<any> {

		const response = await this.context.get(process.env.BASE_URL + `/structr/rest/${type}/${id}/${view}`);

		if (response.status() === 404) {
			return null;
		}

		expect(response.ok()).toBeTruthy();

		return (await response.json()).result;
	}

	/** All objects of a type matching the given (exact) property values, in the "all" view. */
	async find(type: string, query: Record<string, string> = {}): Promise<any[]> {

		const params   = new URLSearchParams(query).toString();
		const response = await this.context.get(process.env.BASE_URL + `/structr/rest/${type}/all` + (params ? '?' + params : ''));

		expect(response.ok()).toBeTruthy();

		return (await response.json()).result;
	}

	/**
	 * Creates a page with the frontend.js runtime and a `.hidden` rule (the show/hide follow-ups and the
	 * custom dialog notifications toggle that class, so the specs can assert real visibility).
	 */
	async page(name: string, options: { publicVisible?: boolean } = {}): Promise<{ pageId: string, headId: string, bodyId: string }> {

		const pageId = await this.node('Page', { name: name, visibleToPublicUsers: !!options.publicVisible, visibleToAuthenticatedUsers: true });

		if (options.publicVisible) {
			this.publicPages.add(pageId);
		}

		const htmlId  = await this.element(pageId, pageId, 'Html');
		const headId  = await this.element(pageId, htmlId, 'Head');
		const styleId = await this.element(pageId, headId, 'Style');

		await this.text(pageId, styleId, '.hidden { display: none !important; }');
		await this.element(pageId, headId, 'Script', { _html_type: 'module', _html_src: FRONTEND_JS });

		const bodyId = await this.element(pageId, htmlId, 'Body');

		return { pageId, headId, bodyId };
	}

	/**
	 * Creates an HTML element. The tag has to be given explicitly: creating an element type over REST does
	 * not derive it from the type, and an element whose tag is null renders as nothing, subtree included.
	 * Attributes are passed with their `_html_` prefix, e.g. `{ _html_id: 'save' }`.
	 */
	async element(pageId: string, parentId: string, type: string, data: object = {}): Promise<string> {

		return await this.node(type, { tag: type.toLowerCase(), pageId: pageId, parent: parentId, ...this.visibility(pageId), ...data });
	}

	/** A text node; `${...}` in it is evaluated when the page renders. */
	async text(pageId: string, parentId: string, content: string): Promise<string> {

		return await this.node('Content', { pageId: pageId, parent: parentId, content: content, ...this.visibility(pageId) });
	}

	/** An action mapping triggered by the given element(s). */
	async actionMapping(triggerIds: string | string[], data: object): Promise<string> {

		return await this.node('ActionMapping', { triggerElements: [ triggerIds ].flat(), ...data });
	}

	/** A parameter of the given action mapping, e.g. `{ parameterType: 'user-input', parameterName: 'name', inputElement: inputId }`. */
	async parameter(actionMappingId: string, data: object): Promise<string> {

		return await this.node('ParameterMapping', { actionMapping: actionMappingId, ...data });
	}

	/**
	 * A schema type, e.g. `schemaType('Item', [{ name: 'priority', propertyType: 'Integer' }], [{ name: 'describe', source: '{ return $.this.name; }' }])`.
	 * Methods are instance methods unless they carry `isStatic: true`.
	 */
	async schemaType(name: string, properties: object[] = [], methods: object[] = []): Promise<string> {

		return await this.node('SchemaNode', { name: name, schemaProperties: properties, schemaMethods: methods });
	}

	/** A user-defined (global) function. */
	async userFunction(name: string, source: string, data: object = {}): Promise<string> {

		return await this.node('SchemaMethod', { name: name, source: source, ...data });
	}

	private visibility(pageId: string): object {

		return this.publicPages.has(pageId) ? { visibleToPublicUsers: true, visibleToAuthenticatedUsers: true } : {};
	}
}

/** Loads a test page and waits until frontend.js has bound its events. */
export async function openPage(page: Page, path: string): Promise<void> {

	await page.goto(process.env.BASE_URL + '/' + path.replace(/^\//, ''));
	await page.waitForFunction(() => (window as any).structrFrontendModule !== undefined);
}

/** Resolves with the next action request frontend.js sends; register it BEFORE triggering the action. */
export function nextActionRequest(page: Page): Promise<Request> {

	return page.waitForRequest(request => request.url().includes('/event') && request.method() === 'POST');
}

/** Resolves with the response to the next action request; register it BEFORE triggering the action. */
export function nextActionResponse(page: Page): Promise<Response> {

	return page.waitForResponse(response => response.url().includes('/event') && response.request().method() === 'POST');
}

/** Resolves with the next partial reload request (GET /structr/html/<id>...); register it BEFORE triggering the action. */
export function nextPartialReload(page: Page): Promise<Request> {

	return page.waitForRequest(request => request.url().includes('/structr/html/') && request.method() === 'GET');
}

/**
 * Starts recording the given DOM events on the document (they bubble up from the trigger element).
 * Elements in an event's detail are replaced by their id (or tag name), so the detail can leave the page.
 */
export async function recordEvents(page: Page, names: string[]): Promise<void> {

	await page.evaluate((names) => {

		const w = window as any;

		w.__recordedEvents = [];

		for (const name of names) {

			document.addEventListener(name, (event: Event) => {

				const detail = (event as CustomEvent).detail;
				const plain  = detail === undefined || detail === null ? detail : JSON.parse(JSON.stringify(detail, (key, value) => value instanceof Element ? (value.id || value.tagName) : value));

				w.__recordedEvents.push({ type: event.type, target: (event.target as Element)?.id ?? null, detail: plain });
			});
		}

	}, names);
}

/** The events recorded since recordEvents(), in order. */
export async function recordedEvents(page: Page): Promise<{ type: string, target: string | null, detail: any }[]> {

	return await page.evaluate(() => (window as any).__recordedEvents ?? []);
}
