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
import {goToModule} from './helpers/init';
import {login} from './helpers/auth';
import {EamBuilder} from './helpers/eam';

// The editor of a page that consists of a single template on the application map: the page has no tree of elements, its
// template fills the screen when the page does, and the editor of the Code area opens on it. The template reads a type,
// which connects the page to the type (the map leaves out what is connected to nothing).

const CONTENT = "<!DOCTYPE html>\n<html><body>${size(find('Item'))}</body></html>";

let builder;
let ids = {};

test.beforeAll(async ({playwright}) => {

	builder = await EamBuilder.create(playwright);

	await builder.schemaType('Item', [{ name: 'priority', propertyType: 'Integer' }]);

	ids.page     = await builder.node('Page', { name: 'sheet', visibleToAuthenticatedUsers: true });
	ids.template = await builder.node('Template', { pageId: ids.page, parent: ids.page, content: CONTENT, contentType: 'text/html' });

	// the schema compiles after the type is created
	await new Promise(resolve => setTimeout(resolve, 1000));
});

// ----- the map, asked from inside the page -----
const editing  = page => page.evaluate(() => _AppMap.view.editing?.id ?? null);
const code     = page => page.locator('.appmap-editor .view-lines');
const state    = page => page.locator('.appmap-editor-state');
const question = page => page.locator('.confirmationText');
const zoom     = page => page.evaluate(() => ({ k: _AppMap.view.transform.k, hi: _AppMap.view.zoom.scaleExtent()[1] }));

async function answer(page, text) {

	await question(page).getByRole('button', { name: text }).click();
	await expect(question(page)).toHaveCount(0);
}

// glides into the page, whose template then fills the screen, and waits for the editor on it
async function openSheet(page) {

	await page.waitForFunction(() => _AppMap.view?.layout && !_AppMap.view.layout.settling);

	await page.evaluate(id => {

		const view = _AppMap.view;

		d3.select(view.canvas).interrupt();
		view.glideInto(view.byId.get(id), 0);

	}, ids.page);

	await expect.poll(() => editing(page)).toBe(ids.page);
	await expect(code(page)).toBeVisible();

	// the glide into place ends first
	await page.waitForTimeout(1200);
}

async function typeIntoSheet(page, text) {

	await code(page).click();
	await page.keyboard.press('Control+End');
	await page.keyboard.type(text);

	await expect(code(page)).toContainText(text.trim());
	await expect(state(page)).toContainText('Unsaved changes');
}

// the wheel at the edge of the map, beside the editor, outwards until the map is zoomed out below the given zoom
async function zoomOutBelow(page, k) {

	const edge = await page.evaluate(() => { const box = _AppMap.view.canvas.getBoundingClientRect(); return { x: box.left + 8, y: box.top + box.height / 2 }; });

	await page.mouse.move(edge.x, edge.y);

	await expect.poll(async () => {

		await page.mouse.wheel(0, 600);
		await page.waitForTimeout(300);

		return (await zoom(page)).k;

	}, { timeout: 10_000 }).toBeLessThan(k);

	await page.waitForTimeout(500);
}

// the wheel over the page, inwards, until the template fills the screen again and the editor stands on it
async function zoomBackIn(page) {

	await expect.poll(async () => {

		const place = await page.evaluate(id => {

			const view = _AppMap.view;
			const n    = view.byId.get(id);
			const box  = view.canvas.getBoundingClientRect();

			return { x: box.left + view.transform.applyX(n.x), y: box.top + view.transform.applyY(n.y) };

		}, ids.page);

		await page.mouse.move(place.x, place.y);
		await page.mouse.wheel(0, -300);
		await page.waitForTimeout(300);

		return page.evaluate(id => _AppMap.view.editing?.id === id && !document.querySelector('.appmap-editor')?.hidden, ids.page);

	}, { timeout: 15_000 }).toBe(true);
}

async function savedContent() {

	return (await builder.get('Template', ids.template)).content;
}

test.describe.configure({ mode: 'serial' });

test.beforeEach(async ({page}) => {

	await login(page);
	await goToModule(page, '#appmap_');
	await page.waitForFunction(id => _AppMap.view?.byId?.has(id), ids.page, { timeout: 30_000 });

	// the introduction of a first visit lies above the map
	await page.evaluate(() => document.querySelectorAll('#appmap-intro [data-intro-close]')[0]?.click());
});

test('the page of a single template has no tree, and its template opens in the editor of code', async ({page}) => {

	expect(await page.evaluate(id => _AppMap.view.byId.get(id).template, ids.page)).toBe(ids.template);

	await openSheet(page);

	await expect(code(page)).toContainText('<html>');
});

test('the editor of a single template names the keys that save, close and discard', async ({page}) => {

	await openSheet(page);

	await expect(state(page)).toHaveText('Ctrl+S saves · Esc closes');
	await typeIntoSheet(page, '<!-- named -->');
	await expect(state(page)).toHaveText('Unsaved changes · Ctrl+S saves · Esc discards');

	await page.keyboard.press('Escape');
	await answer(page, 'Yes');

	expect(await savedContent()).toBe(CONTENT);
});

test('saving a single template keeps the editor, and the wheel zooms out of it and into it again', async ({page}) => {

	await openSheet(page);

	const before   = await zoom(page);
	const reloaded = page.waitForResponse(response => response.url().endsWith('/structr/rest/_appmap') && response.request().method() === 'GET');

	await typeIntoSheet(page, '<!-- saved -->');
	await page.keyboard.press('Control+s');
	await reloaded;
	await page.waitForTimeout(1500);

	expect(await savedContent()).toContain('<!-- saved -->');

	// the view stays in the template, with the editor on it
	expect(await editing(page)).toBe(ids.page);
	await expect(code(page)).toBeVisible();
	await expect(state(page)).not.toContainText('Unsaved changes');

	const after = await zoom(page);

	expect(after.k).toBeCloseTo(before.k, 3);
	expect(after.hi).toBeCloseTo(before.hi, 3);

	await zoomOutBelow(page, after.k / 2);
	await zoomBackIn(page);

	await builder.update('Template', ids.template, { content: CONTENT });
});

test('unsaved changes of a single template survive zooming out, and the template can be zoomed into again', async ({page}) => {

	await openSheet(page);
	await typeIntoSheet(page, '<!-- kept -->');

	const inside = (await zoom(page)).k;

	await zoomOutBelow(page, inside / 2);

	// the changes keep the editor with its template
	expect(await editing(page)).toBe(ids.page);

	await zoomBackIn(page);

	await expect(code(page)).toContainText('<!-- kept -->');
	await expect(state(page)).toContainText('Unsaved changes');

	await page.keyboard.press('Escape');
	await answer(page, 'Yes');

	expect(await savedContent()).toBe(CONTENT);
});

test.describe('on a Mac', () => {

	test.beforeEach(async ({page}) => {

		// _Helpers.isMac asks the platform of the user agent data first, then the older navigator.platform
		await page.addInitScript(() => {
			Object.defineProperty(navigator, 'userAgentData', { get: () => ({ platform: 'macOS' }), configurable: true });
			Object.defineProperty(navigator, 'platform', { get: () => 'MacIntel', configurable: true });
		});
		await page.reload();
		await page.waitForFunction(id => _AppMap.view?.byId?.has(id), ids.page, { timeout: 30_000 });

		expect(await page.evaluate(() => _Helpers.isMac())).toBe(true);
	});

	test('the editor of a single template names Cmd+S, and Escape discards its changes', async ({page}) => {

		await openSheet(page);

		await expect(state(page)).toHaveText('Cmd+S saves · Esc closes');
		await typeIntoSheet(page, '<!-- mac -->');

		await page.keyboard.press('Escape');
		await expect(question(page)).toBeVisible();
		await answer(page, 'Yes');

		expect(await savedContent()).toBe(CONTENT);
	});
});
