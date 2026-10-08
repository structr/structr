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
import {EamBuilder, nextActionResponse, nextPartialReload, openPage, recordEvents, recordedEvents} from './helpers/eam';

/**
 * Event Action Mapping: the client-side lifecycle around an action and the lazy rendering modes.
 *
 * Covers the JavaScript API of the frontend module (window.structrFrontendModule.addEventListener for
 * start, success, error and reload), the running state of a trigger element when the request fails
 * on the network or does not answer with JSON, a repeated click while an action is running, and the
 * rendering modes of DOM elements (data-structr-rendering-mode: load, delayed, visible, periodic),
 * which render an element's children with a partial reload after the page has loaded.
 *
 * The running state of a successful and of a rejected (422) action is covered in test-eam-actions.
 */

// time to wait before asserting that something did NOT happen
const QUIET_PERIOD = 1500;

const ACTION_REQUESTS = '**/structr/rest/DOMElement/*/event';

let builder;

// per page name: the uuids the tests need
const scenarios = {};

/** A button calling the given user-defined function, with optional further mapping settings. */
async function addMethodButton(pageId, parentId, htmlId, method, mapping = {}) {

	const buttonId = await builder.element(pageId, parentId, 'Button', { _html_id: htmlId });

	await builder.text(pageId, buttonId, htmlId);
	await builder.actionMapping(buttonId, { event: 'click', action: 'method', method: method, ...mapping });

	return buttonId;
}

/** A section with a text, and a button whose action does nothing but refresh that section. */
async function addUnrelatedReload(pageId, parentId) {

	const areaId   = await builder.element(pageId, parentId, 'Div', { _html_id: 'reload-area' });
	const buttonId = await builder.element(pageId, parentId, 'Button', { _html_id: 'reload' });

	await builder.text(pageId, areaId, 'Unrelated section');
	await builder.text(pageId, buttonId, 'Reload');
	await builder.actionMapping(buttonId, { event: 'click', action: 'none', successBehaviour: 'partial-refresh', successPartial: '#reload-area' });
}

/** A page with one element in the given rendering mode, whose children only arrive with the lazy render. */
async function addLazyPage(name, renderingMode, delayOrInterval, options = {}) {

	const { pageId, bodyId } = await builder.page(name);

	if (options.spacer) {

		// pushes the lazy element far below the viewport
		await builder.element(pageId, bodyId, 'Div', { _html_id: 'spacer', _html_style: 'height: 3000px' });
	}

	const data = { _html_id: 'lazy' };

	if (renderingMode) {
		data['data-structr-rendering-mode'] = renderingMode;
	}

	if (delayOrInterval) {
		data['data-structr-delay-or-interval'] = delayOrInterval;
	}

	const lazyId = await builder.element(pageId, bodyId, 'Div', data);

	await builder.text(pageId, lazyId, 'Lazy content');

	if (options.unrelatedReload) {
		await addUnrelatedReload(pageId, bodyId);
	}

	scenarios[name] = { lazyId };
}

/** Records the partial reload requests for one element, so a test can count them. */
function recordRenderRequests(page, elementId) {

	const requests = [];

	page.on('request', request => {

		if (request.method() === 'GET' && request.url().includes(`/structr/html/${elementId}`)) {
			requests.push(request);
		}
	});

	return requests;
}

/** Holds every action request back until the returned function is called. */
async function holdActionRequests(page) {

	let release;
	const held = new Promise(resolve => release = resolve);

	await page.route(ACTION_REQUESTS, async route => {
		await held;
		await route.continue();
	});

	return release;
}

/**
 * Registers a listener for each given name with the frontend module's own event API and records the
 * calls in window.__apiEvents. The payload is reduced to what can leave the page.
 */
async function recordApiEvents(page, names) {

	await page.evaluate((names) => {

		window.__apiEvents = [];

		for (const name of names) {

			window.structrFrontendModule.addEventListener(name, (payload) => {

				window.__apiEvents.push({
					name:      name,
					target:    payload?.target?.id ?? null,
					connected: payload?.target?.isConnected ?? null,
					status:    payload?.status ?? null,
					dataId:    payload?.data?.structrId ?? null,
					result:    payload?.data?.result ?? null,
					hasEvent:  payload?.event instanceof Event
				});
			});
		}

	}, names);
}

async function apiEvents(page) {

	return await page.evaluate(() => window.__apiEvents ?? []);
}

test.beforeAll(async ({ playwright }) => {

	builder = await EamBuilder.create(playwright);

	await builder.userFunction('answer', "{ return 'ok'; }");

	// give the schema a moment to settle before the pages refer to the function
	await new Promise(resolve => setTimeout(resolve, 1000));

	// the JavaScript API of the frontend module
	{
		const { pageId, bodyId } = await builder.page('lifecycle-api');
		const panelId            = await builder.element(pageId, bodyId, 'Div', { _html_id: 'lifecycle-panel' });

		await builder.text(pageId, panelId, 'Panel');

		const succeedId = await addMethodButton(pageId, bodyId, 'succeed', 'answer', { successBehaviour: 'partial-refresh', successPartial: '#lifecycle-panel' });
		const failId    = await addMethodButton(pageId, bodyId, 'fail', 'doesNotExist');

		scenarios['lifecycle-api'] = { succeedId, failId };
	}

	// the running state
	{
		const { pageId, bodyId } = await builder.page('lifecycle-running');
		const runId              = await addMethodButton(pageId, bodyId, 'run', 'answer');

		scenarios['lifecycle-running'] = { runId };
	}

	// rendering modes
	await addLazyPage('rendering-eager', null, null);
	await addLazyPage('rendering-load', 'load', null, { unrelatedReload: true });
	await addLazyPage('rendering-delayed', 'delayed', '2000');
	await addLazyPage('rendering-visible', 'visible', null, { spacer: true });
	await addLazyPage('rendering-periodic', 'periodic', '1000', { unrelatedReload: true });
});

test.describe('frontend module events', () => {

	// Registers API listeners for start and success, then clicks a button calling a function that returns 'ok':
	// start comes first with the trigger, its dataset and the DOM event; success follows with status 200 and the response body.
	// the frontend module's addEventListener is not documented: this pins the current payloads
	test('start and success are delivered with the trigger, its data and the response', async ({ page }) => {

		const { succeedId } = scenarios['lifecycle-api'];

		await login(page);
		await openPage(page, 'lifecycle-api');
		await recordApiEvents(page, [ 'start', 'success' ]);

		await page.locator('#succeed').click();

		await expect.poll(async () => (await apiEvents(page)).length).toBe(2);

		const [ start, success ] = await apiEvents(page);

		expect(start).toMatchObject({ name: 'start', target: 'succeed', dataId: succeedId, hasEvent: true });
		expect(success).toMatchObject({ name: 'success', target: 'succeed', status: 200, result: 'ok' });
	});

	// Registers an API listener for error and clicks a button calling a function that does not exist:
	// the listener receives the trigger and status 422, and no success is reported.
	test('error is delivered with the trigger and the error status', async ({ page }) => {

		await login(page);
		await openPage(page, 'lifecycle-api');
		await recordApiEvents(page, [ 'success', 'error' ]);

		const response = nextActionResponse(page);

		await page.locator('#fail').click();

		expect((await response).status()).toBe(422);

		await expect.poll(async () => (await apiEvents(page)).length).toBe(1);

		expect((await apiEvents(page))[0]).toMatchObject({ name: 'error', target: 'fail', status: 422 });
	});

	// Registers an API listener for reload; the success follow-up refreshes #lifecycle-panel, and the listener
	// receives the new panel node as its target once.
	test('reload is delivered with the re-rendered section', async ({ page }) => {

		await login(page);
		await openPage(page, 'lifecycle-api');
		await recordApiEvents(page, [ 'reload' ]);

		const reload = nextPartialReload(page);

		await page.locator('#succeed').click();
		await reload;

		await expect.poll(async () => (await apiEvents(page)).length).toBe(1);

		const [ event ] = await apiEvents(page);

		// the target is the new node in the document, not the replaced one
		expect(event.target).toBe('lifecycle-panel');
		expect(event.connected).toBe(true);
	});

	// Registers two success listeners, removes the first one again and clicks: only the second one is called,
	// and removing a listener that was never registered does not remove the remaining one.
	test('removeEventListener removes exactly the given listener', async ({ page }) => {

		await login(page);
		await openPage(page, 'lifecycle-api');

		await page.evaluate(() => {

			window.__calls = [];

			const first  = () => window.__calls.push('first');
			const second = () => window.__calls.push('second');

			window.structrFrontendModule.addEventListener('success', first);
			window.structrFrontendModule.addEventListener('success', second);
			window.structrFrontendModule.removeEventListener('success', first);
			window.structrFrontendModule.removeEventListener('success', () => {});
		});

		const response = nextActionResponse(page);

		await page.locator('#succeed').click();
		await response;

		await expect.poll(() => page.evaluate(() => window.__calls)).toEqual([ 'second' ]);
	});
});

test.describe('running state', () => {

	// Aborts the action request on the network: the trigger must lose structr-action-running and announce
	// structr-action-finished, which the docs promise "regardless of whether it succeeded or failed".
	test('a network failure ends the running state', async ({ page }) => {

		await login(page);
		await openPage(page, 'lifecycle-running');
		await recordEvents(page, [ 'structr-action-started', 'structr-action-finished' ]);

		await page.route(ACTION_REQUESTS, route => route.abort('failed'));

		const failed = page.waitForEvent('requestfailed');

		await page.locator('#run').click();
		await failed;

		await expect(page.locator('#run')).not.toHaveClass(/structr-action-running/);
		await expect.poll(async () => (await recordedEvents(page)).map(event => event.type)).toEqual([
			'structr-action-started',
			'structr-action-finished'
		]);
	});

	// Answers the action request with an HTML error page (502): the trigger must lose structr-action-running
	// and announce structr-action-finished like for any other failed action.
	test('an error response without JSON ends the running state', async ({ page }) => {

		await login(page);
		await openPage(page, 'lifecycle-running');
		await recordEvents(page, [ 'structr-action-started', 'structr-action-finished' ]);

		await page.route(ACTION_REQUESTS, route => route.fulfill({ status: 502, contentType: 'text/html', body: '<html><body>Bad gateway</body></html>' }));

		const response = nextActionResponse(page);

		await page.locator('#run').click();

		expect((await response).status()).toBe(502);

		await expect(page.locator('#run')).not.toHaveClass(/structr-action-running/);
		await expect.poll(async () => (await recordedEvents(page)).map(event => event.type)).toEqual([
			'structr-action-started',
			'structr-action-finished'
		]);
	});

	// Holds the action requests back and clicks the button twice: a second request is sent while the first
	// one is still running, and both are answered with 200.
	// the docs say nothing about repeated triggers while an action is running: this pins the current behaviour
	test('a second click while the action is running sends a second request', async ({ page }) => {

		const { runId } = scenarios['lifecycle-running'];

		await login(page);
		await openPage(page, 'lifecycle-running');

		const requests  = [];
		const responses = [];

		page.on('request', request => { if (request.method() === 'POST' && request.url().endsWith(`/DOMElement/${runId}/event`)) requests.push(request); });
		page.on('response', response => { if (response.request().method() === 'POST' && response.url().endsWith(`/DOMElement/${runId}/event`)) responses.push(response); });

		const release = await holdActionRequests(page);

		await page.locator('#run').click();
		await expect(page.locator('#run')).toHaveClass(/structr-action-running/);

		await page.locator('#run').click();

		await expect.poll(() => requests.length).toBe(2);

		release();

		await expect.poll(() => responses.length).toBe(2);

		expect(responses.map(response => response.status())).toEqual([ 200, 200 ]);
		await expect(page.locator('#run')).not.toHaveClass(/structr-action-running/);
	});
});

test.describe('rendering modes', () => {

	// An element without a rendering mode is rendered with its children right away, and no partial
	// reload is requested for it.
	test('without a rendering mode the children are rendered with the page', async ({ page }) => {

		const { lazyId } = scenarios['rendering-eager'];

		await login(page);

		const requests = recordRenderRequests(page, lazyId);

		await openPage(page, 'rendering-eager');

		await expect(page.locator('#lazy')).toHaveText('Lazy content');
		await page.waitForTimeout(QUIET_PERIOD);

		expect(requests).toHaveLength(0);
	});

	// The page HTML contains the 'load' element without its children; in the browser the element is re-rendered
	// with one partial reload right after loading and then shows its content.
	test('load renders the children with one partial reload after the page has loaded', async ({ page }) => {

		const { lazyId } = scenarios['rendering-load'];

		await login(page);

		const html = await (await page.request.get(process.env.BASE_URL + '/rendering-load')).text();

		expect(html).toContain('data-structr-rendering-mode="load"');
		expect(html).not.toContain('Lazy content');

		const requests = recordRenderRequests(page, lazyId);

		await openPage(page, 'rendering-load');

		await expect(page.locator('#lazy')).toHaveText('Lazy content');
		await page.waitForTimeout(QUIET_PERIOD);

		expect(requests).toHaveLength(1);
	});

	// After the 'load' element has rendered, a partial reload of an unrelated section must not render the
	// lazy element a second time.
	test('load does not render again when a partial reload elsewhere rebinds the events', async ({ page }) => {

		const { lazyId } = scenarios['rendering-load'];

		await login(page);

		const requests = recordRenderRequests(page, lazyId);

		await openPage(page, 'rendering-load');
		await expect(page.locator('#lazy')).toHaveText('Lazy content');
		await expect.poll(() => requests.length).toBe(1);

		const reload = page.waitForResponse(response => response.url().includes('/structr/html/') && !response.url().includes(lazyId));

		await page.locator('#reload').click();
		await reload;
		await page.waitForTimeout(QUIET_PERIOD);

		expect(requests).toHaveLength(1);
	});

	// An element in 'delayed' mode with 2000 ms is still empty shortly after loading and shows its
	// content after the delay, rendered with one partial reload.
	test('delayed renders the children after the configured delay', async ({ page }) => {

		const { lazyId } = scenarios['rendering-delayed'];

		await login(page);

		const requests = recordRenderRequests(page, lazyId);

		await openPage(page, 'rendering-delayed');

		await page.waitForTimeout(500);

		expect(requests).toHaveLength(0);
		await expect(page.locator('#lazy')).toHaveText('');

		await expect(page.locator('#lazy')).toHaveText('Lazy content', { timeout: 6000 });

		expect(requests).toHaveLength(1);
	});

	// An element in 'visible' mode below a 3000px spacer is not rendered while it is out of view; scrolling
	// it into view renders it with one partial reload.
	test('visible renders the children when the element is scrolled into view', async ({ page }) => {

		const { lazyId } = scenarios['rendering-visible'];

		await login(page);

		const requests = recordRenderRequests(page, lazyId);

		await openPage(page, 'rendering-visible');
		await page.waitForTimeout(QUIET_PERIOD);

		expect(requests).toHaveLength(0);

		await page.locator('#lazy').scrollIntoViewIfNeeded();

		await expect(page.locator('#lazy')).toHaveText('Lazy content');

		expect(requests).toHaveLength(1);
	});

	// An element in 'periodic' mode with 1000 ms is rendered right after loading and then once per second:
	// within about 3.5 seconds that are 3 to 5 partial reloads, and the content is shown.
	test('periodic renders the children again in the configured interval', async ({ page }) => {

		const { lazyId } = scenarios['rendering-periodic'];

		await login(page);

		const requests = recordRenderRequests(page, lazyId);

		await openPage(page, 'rendering-periodic');
		await page.waitForTimeout(3500);

		await expect(page.locator('#lazy')).toHaveText('Lazy content');

		expect(requests.length).toBeGreaterThanOrEqual(3);
		expect(requests.length).toBeLessThanOrEqual(5);
	});

	// Counts the periodic reloads over 3 seconds before and after a partial reload of an unrelated section:
	// the interval must stay the same, so the second window has at most one reload more than the first.
	test('periodic keeps its interval when a partial reload elsewhere rebinds the events', async ({ page }) => {

		const { lazyId } = scenarios['rendering-periodic'];

		await login(page);

		const requests = recordRenderRequests(page, lazyId);

		await openPage(page, 'rendering-periodic');
		await expect(page.locator('#lazy')).toHaveText('Lazy content');

		// first window, without any other reload
		let start = requests.length;
		await page.waitForTimeout(3000);
		const before = requests.length - start;

		const reload = page.waitForResponse(response => response.url().includes('/structr/html/') && !response.url().includes(lazyId));

		await page.locator('#reload').click();
		await reload;

		// second window, after the unrelated reload
		start = requests.length;
		await page.waitForTimeout(3000);
		const after = requests.length - start;

		expect(after).toBeLessThanOrEqual(before + 1);
	});
});
