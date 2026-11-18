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
import {screenshot} from "./helpers/screenshots";

// The call traces of scripting errors on the application map: a method that fails, called by another method, which a
// REST request calls. The map marks the failing method with the sign of errors, its tooltip names the error and the calls
// that led there, and a click on the sign draws the trace over the map.

let builder;
let item;

test.beforeAll(async ({playwright}) => {

	builder = await EamBuilder.create(playwright);

	await builder.schemaType('Item', [{ name: 'priority', propertyType: 'Integer' }], [
		{ name: 'fail',    source: '{\n\tlet missing = null;\n\treturn missing.name;\n}' },
		{ name: 'process', source: '{\n\treturn $.this.fail();\n}' }
	]);

	// the schema compiles after the type is created
	await new Promise(resolve => setTimeout(resolve, 1000));

	item = await builder.node('Item', { name: 'item' });

	// the request fails; the error is reported once its frame exits, after the response
	const response = await builder.context.post(process.env.BASE_URL + `/structr/rest/Item/${item}/process`, { data: '{}' });

	expect(response.ok()).toBeFalsy();

	await expect.poll(async () => {

		const traces = await builder.context.get(process.env.BASE_URL + '/structr/rest/_appmap/traces');
		const result = (await traces.json()).result;

		return (Array.isArray(result) ? result[0] : result)?.traces?.length ?? 0;

	}, { timeout: 10_000 }).toBeGreaterThan(0);
});

// ----- the map, asked from inside the page -----
const tip = page => page.locator('#appmap-canvas .appmap-tip');

async function failing(page) {

	return page.evaluate(() => _AppMap.view.nodes.find(n => n.name === 'Item.fail')?.id ?? null);
}

// where the sign of errors of the failing method is on the screen, with the map zoomed so that it is large enough
async function signOnScreen(page) {

	return page.evaluate(() => {

		const view = _AppMap.view;
		const n    = view.nodes.find(n => n.name === 'Item.fail');
		const k    = view.baseK * 6;

		d3.select(view.canvas).interrupt().call(view.zoom.transform, d3.zoomIdentity.translate(view.width / 2 - n.x * k, view.height / 2 - n.y * k).scale(k));
		view.renderer.draw();

		const [, x0, y0, x1, y1] = view.renderer.problemBoxes.find(([m]) => m === n);
		const box                = view.canvas.getBoundingClientRect();

		return { x: box.left + view.transform.applyX((x0 + x1) / 2), y: box.top + view.transform.applyY((y0 + y1) / 2) };
	});
}

test.describe.configure({ mode: 'serial' });

test.beforeEach(async ({page}) => {

	await login(page);
	await goToModule(page, '#appmap_');
	await page.waitForFunction(() => _AppMap.view?.nodes?.some(n => n.name === 'Item.fail'), null, { timeout: 30_000 });
	await page.waitForFunction(() => _AppMap.view?.layout && !_AppMap.view.layout.settling);

	// the introduction of a first visit lies above the map
	await page.evaluate(() => document.querySelectorAll('#appmap-intro [data-intro-close]')[0]?.click());
});

test('the failing method has the call trace that led to it, from the method that called it to the request', async ({page}) => {

	await expect.poll(() => page.evaluate(() => {

		const view = _AppMap.view;
		const n    = view.nodes.find(n => n.name === 'Item.fail');

		return (view.tracesOf.get(n) ?? []).map(t => ({ steps: t.steps.map(s => s.node?.name ?? null).slice(0, 2), lines: t.steps.map(s => s.frames[0].row).slice(0, 2), entry: t.steps.at(-1).frames[0].kind }));

	})).toEqual([{ steps: ['Item.fail', 'Item.process'], lines: [3, 2], entry: 'HTTP' }]);
});

test('the sign of errors on the failing method says what failed and the calls that led there', async ({page}) => {

	await expect.poll(() => page.evaluate(() => _AppMap.view.problemsOfElement(_AppMap.view.nodes.find(n => n.name === 'Item.fail')).errors)).toBe(1);

	const sign = await signOnScreen(page);

	await page.mouse.move(sign.x, sign.y);

	await expect(tip(page)).toBeVisible();
	await expect(tip(page)).toContainText('Item.fail:3 ← Item.process:2 ← POST');
	await expect(tip(page)).toContainText('Click the sign to show the call trace on the map');
});

test('a click on the sign shows the call trace on the map, and Escape ends it', async ({page}) => {

	await expect.poll(() => page.evaluate(() => _AppMap.view.problemsOfElement(_AppMap.view.nodes.find(n => n.name === 'Item.fail')).errors)).toBe(1);

	const sign = await signOnScreen(page);

	await page.mouse.click(sign.x, sign.y);

	await expect.poll(() => page.evaluate(() => _AppMap.view.callTrace?.origin?.name ?? null)).toBe('Item.fail');
	expect(await page.evaluate(() => _AppMap.view.pinned?.name)).toBe('Item.fail');
	await expect(page.locator('#appmap-hint')).toContainText('Item.fail:3 ← Item.process:2');
	await expect(page.locator('#appmap-hint')).toContainText('Esc ends');

	// the elements of the trace are on the screen with their shapes, once the map has glided there
	await expect.poll(() => page.evaluate(() => {

		const view = _AppMap.view;
		const t    = view.transform;

		return view.callTrace.steps.filter(s => s.node).every(({ node: n }) => {

			const [w, h] = (AppMapRenderer.boxOf(n) ?? [1, 1]).map(v => v * view.shownRadius(n));

			return t.applyX(n.x - w) >= 0 && t.applyX(n.x + w) <= view.width && t.applyY(n.y - h) >= 0 && t.applyY(n.y + h) <= view.height;
		});

	})).toBe(true);

	await page.mouse.move(5, 300);
	await page.waitForTimeout(300);

	await screenshot(page, { path: 'screenshots/appmap_call-trace.png' });

	await page.keyboard.press('Escape');

	await expect.poll(() => page.evaluate(() => _AppMap.view.callTrace)).toBeNull();
	expect(await page.evaluate(() => _AppMap.view.pinned)).toBeNull();
});

test('saving the failing method acknowledges its error, and the sign goes', async ({page}) => {

	const id = await failing(page);

	await builder.update('SchemaMethod', id, { source: '{\n\treturn null;\n}' });

	// the map loads its scripting errors anew, as after saving in its editor
	await page.evaluate(() => _AppMap.loadProblems());

	await expect.poll(() => page.evaluate(() => _AppMap.view.problemsOfElement(_AppMap.view.nodes.find(n => n.name === 'Item.fail')).errors)).toBe(0);
});
