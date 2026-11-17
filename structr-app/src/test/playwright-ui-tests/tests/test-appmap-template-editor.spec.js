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

// The editor of a template inside a page on the application map: the page opens with its tree, the template fills the
// screen and its controls open with the editor of its content. The map is a canvas, so the test asks the map where the
// template lies and uses the mouse there. Besides the template the page holds a repeater of a type, which connects it to
// the type (the map leaves out what is connected to nothing), and a repeater that has a data key but no data source, so
// the page carries a warning sign.

const CONTENT = '<p>Card</p>';

let builder;
let ids = {};

test.beforeAll(async ({playwright}) => {

	builder = await EamBuilder.create(playwright);

	await builder.schemaType('Item', [{ name: 'priority', propertyType: 'Integer' }]);

	const card = await builder.page('card');

	ids.page     = card.pageId;
	ids.template = await builder.node('Template', { pageId: card.pageId, parent: card.bodyId, content: CONTENT, contentType: 'text/html' });
	ids.items    = await builder.element(card.pageId, card.bodyId, 'Ul', { functionQuery: "find('Item')", dataKey: 'item' });
	ids.repeater = await builder.element(card.pageId, card.bodyId, 'Div', { dataKey: 'other' });

	// the schema compiles after the type is created
	await new Promise(resolve => setTimeout(resolve, 1000));
});

// ----- the map, asked from inside the page -----
const editing   = page => page.evaluate(() => _AppMap.view.editing?.data?.id ?? null);
const controls  = page => page.locator('#appmap-canvas .appmap-el');
const state     = page => page.locator('#appmap-canvas .appmap-el-state');
const code      = page => page.locator('#appmap-canvas .appmap-el .view-lines');
const question  = page => page.locator('.confirmationText');
const zoom      = page => page.evaluate(() => ({ k: _AppMap.view.transform.k, x: _AppMap.view.transform.x, y: _AppMap.view.transform.y, hi: _AppMap.view.zoom.scaleExtent()[1] }));

async function answer(page, text) {

	await question(page).getByRole('button', { name: text }).click();
	await expect(question(page)).toHaveCount(0);
}

// opens the page on the map with its tree and says where the template lies on the screen
async function openPage(page) {

	await page.waitForFunction(() => _AppMap.view?.layout && !_AppMap.view.layout.settling);

	await page.evaluate(id => {

		const view = _AppMap.view;
		const n    = view.byId.get(id);

		d3.select(view.canvas).interrupt();
		view.glideInto(n, 0);

	}, ids.page);

	await page.waitForFunction(id => [..._AppMap.view.panels.trees].some(([n, tree]) => n.id === id && tree.open), ids.page);

	return templateOnScreen(page);
}

async function templateOnScreen(page) {

	return page.evaluate(id => {

		const view = _AppMap.view;
		const d    = [...view.panels.trees.values()].flatMap(tree => tree.list).find(d => d.data.id === id);
		const p    = view.panels.elementPanel(d, view.transform.k);
		const box  = view.canvas.getBoundingClientRect();

		return { x: box.left + view.transform.applyX(p.x + p.width / 2), y: box.top + view.transform.applyY(p.y + p.height / 2) };

	}, ids.template);
}

// a click on the template glides into it, and its controls open once it fills the screen
async function openTemplate(page) {

	const place = await openPage(page);

	await page.mouse.click(place.x, place.y);
	await expect.poll(() => editing(page)).toBe(ids.template);
	await expect(code(page)).toBeVisible();

	// the glide into place ends first
	await page.waitForTimeout(1200);
}

async function typeIntoTemplate(page, text) {

	await code(page).click();
	await page.keyboard.press('Control+End');
	await page.keyboard.type(text);

	await expect(code(page)).toContainText(text.trim());
	await expect(state(page)).toHaveText('Unsaved changes · Ctrl+S saves · Esc discards');
}

// the map with the page of the test; where it does not come, the failure says what the map holds instead
async function waitForMap(page) {

	try {

		await page.waitForFunction(id => _AppMap.view?.byId?.has(id), ids.page, { timeout: 30_000 });

	} catch (error) {

		const state = await page.evaluate(id => ({
			mode:    typeof _AppMap === 'undefined' ? null : _AppMap.mode,
			view:    typeof _AppMap !== 'undefined' && !!_AppMap.view,
			nodes:   typeof _AppMap === 'undefined' ? null : _AppMap.view?.nodes?.map(n => `${n.kind}:${n.name}`) ?? null,
			page:    id,
			hash:    location.hash,
			message: [...document.querySelectorAll('.message, .error, .dialog-message')].map(e => e.textContent.trim()).filter(Boolean)
		}), ids.page).catch(e => String(e));

		throw new Error(`The map did not show the page of the test: ${JSON.stringify(state)}`);
	}
}

// turns the wheel outwards until the map is zoomed out below the given zoom; a glide still under way may take a turn
async function zoomOutBelow(page, k) {

	await expect.poll(async () => {

		await page.mouse.wheel(0, 600);
		await page.waitForTimeout(300);

		return (await zoom(page)).k;

	}, { timeout: 10_000 }).toBeLessThan(k);
}

async function savedContent() {

	return (await builder.get('Template', ids.template)).content;
}

test.describe.configure({ mode: 'serial' });

test.beforeEach(async ({page}) => {

	await login(page);
	await goToModule(page, '#appmap_');
	await waitForMap(page);

	// the introduction of a first visit lies above the map
	await page.evaluate(() => document.querySelectorAll('#appmap-intro [data-intro-close]')[0]?.click());
});

test('a warning sign keeps the size of the access badge, also from afar', async ({page}) => {

	// as far out as the map goes, where the sign would otherwise shrink to a dot
	await page.waitForFunction(() => _AppMap.view?.layout && !_AppMap.view.layout.settling);
	await page.evaluate(id => {

		const view = _AppMap.view;
		const n    = view.byId.get(id);
		const k    = view.zoom.scaleExtent()[0];

		d3.select(view.canvas).interrupt().call(view.zoom.transform, d3.zoomIdentity.translate(view.width / 2 - n.x * k, view.height / 2 - n.y * k).scale(k));

	}, ids.page);

	// the repeater without a data source is a warning, no scripting error; the sign is drawn with the next frame
	await page.waitForFunction(id => _AppMap.view.renderer.problemBoxes?.some(([n]) => n.id === id), ids.page);

	const sign = await page.evaluate(id => {

		const view          = _AppMap.view;
		const [n, x0, , x1] = view.renderer.problemBoxes.find(([n]) => n.id === id);

		return { problems: view.panels.problemsOfPage(n), size: (x1 - x0) * view.transform.k, smallest: AppMapRenderer.ACCESS_SMALL };

	}, ids.page);

	expect(sign.problems).toEqual({ errors: 0, warnings: 1 });
	expect(sign.size).toBeGreaterThanOrEqual(sign.smallest - 0.001);
});

test('the problem sign of a page answers the pointer a little beside it, with what is wrong', async ({page}) => {

	await page.waitForFunction(() => _AppMap.view?.layout && !_AppMap.view.layout.settling);
	await page.evaluate(() => _AppMap.view.fitAll());
	await page.waitForFunction(id => _AppMap.view.renderer.problemBoxes?.some(([n]) => n.id === id), ids.page);

	// beside the sign on the left, away from the access badge on its right
	const beside = await page.evaluate(id => {

		const view             = _AppMap.view;
		const [, x0, y0, , y1] = view.renderer.problemBoxes.find(([n]) => n.id === id);
		const box              = view.canvas.getBoundingClientRect();

		return { x: box.left + view.transform.applyX(x0) - 3, y: box.top + view.transform.applyY((y0 + y1) / 2) };

	}, ids.page);

	await page.mouse.move(beside.x, beside.y);

	await expect(page.locator('#appmap-canvas .appmap-tip')).toBeVisible();
	await expect(page.locator('#appmap-canvas .appmap-tip')).toContainText('1 repeater cannot work as configured');
});

test('the tooltip keeps its shape at the right edge of the map', async ({page}) => {

	await page.waitForFunction(() => _AppMap.view?.layout && !_AppMap.view.layout.settling);

	const tip = page.locator('#appmap-canvas .appmap-tip');

	// the page under the pointer, once in the middle of the map and once at its right edge
	const hover = async share => {

		const place = await page.evaluate(([id, share]) => {

			const view = _AppMap.view;
			const n    = view.byId.get(id);
			const k    = view.baseK * 2;
			const box  = view.canvas.getBoundingClientRect();

			d3.select(view.canvas).interrupt().call(view.zoom.transform, d3.zoomIdentity.translate(view.width * share - n.x * k, view.height / 2 - n.y * k).scale(k));

			return { x: box.left + view.width * share, y: box.top + view.height / 2, right: box.right };

		}, [ids.page, share]);

		await page.mouse.move(5, 5);
		await page.mouse.move(place.x, place.y);
		await expect(tip).toBeVisible();

		return { ...(await tip.boundingBox()), right: place.right };
	};

	const middle = await hover(0.5);
	const edge   = await hover(0.98);

	expect(edge.width).toBeCloseTo(middle.width, 0);
	expect(edge.height).toBeCloseTo(middle.height, 0);
	expect(edge.x + edge.width).toBeLessThanOrEqual(edge.right + 0.5);
});

test('the boxes of properties show the icons of the Code area', async ({page}) => {

	const icons = await page.evaluate(() => ({
		string:   AppMapPanels.propertyIcon('String')     === _Icons.iconSchemaPropertyString,
		array:    AppMapPanels.propertyIcon('Integer[]')  === _Icons.iconSchemaPropertyArray,
		date:     AppMapPanels.propertyIcon('Date')       === _Icons.iconSchemaPropertyDate,
		function: AppMapPanels.propertyIcon('Function')   === _Icons.iconSchemaPropertyFunction
	}));

	expect(icons).toEqual({ string: true, array: true, date: true, function: true });

	// the symbol of the sprite becomes an image the canvas can draw
	await expect.poll(() => page.evaluate(() => AppMapPanels.iconImage(_Icons.iconSchemaPropertyString, '#000000', () => {}) instanceof HTMLImageElement)).toBe(true);
});

test('a double click on a template in an open page glides into the template, not to the whole page', async ({page}) => {

	const place = await openPage(page);
	const page0 = await zoom(page);

	await page.mouse.dblclick(place.x, place.y);

	await expect.poll(() => editing(page)).toBe(ids.template);
	await expect.poll(() => page.evaluate(id => {

		const view = _AppMap.view;

		return view.fills([...view.panels.trees.values()].flatMap(tree => tree.list).find(d => d.data.id === id), false);

	}, ids.template)).toBe(true);

	expect((await zoom(page)).k).toBeGreaterThan(page0.k);
});

test('the controls of a template name the keys that save and discard', async ({page}) => {

	await openTemplate(page);

	await expect(state(page)).toHaveText('Ctrl+S saves');
	await typeIntoTemplate(page, '<!-- named -->');

	// discarded again, see the next test
	await page.keyboard.press('Escape');
	await answer(page, 'Yes');
	await expect(state(page)).toHaveText('Ctrl+S saves');
});

test('Escape discards the unsaved changes of a template after asking, and the template shows what is saved', async ({page}) => {

	await openTemplate(page);
	await typeIntoTemplate(page, '<!-- discarded -->');

	// No keeps the changes
	await page.keyboard.press('Escape');
	await expect(question(page)).toBeVisible();
	await answer(page, 'No');
	await expect(code(page)).toContainText('<!-- discarded -->');
	expect(await editing(page)).toBe(ids.template);

	// Yes discards them: the controls open anew with what is saved
	await page.keyboard.press('Escape');
	await answer(page, 'Yes');
	await expect.poll(() => editing(page)).toBe(ids.template);
	await expect(code(page)).toBeVisible();
	await expect(code(page)).not.toContainText('<!-- discarded -->');
	await expect(state(page)).toHaveText('Ctrl+S saves');

	expect(await savedContent()).toBe(CONTENT);
});

test('saving a template keeps the view, the controls stay, and the map zooms in as far as before', async ({page}) => {

	await openTemplate(page);

	const before   = await zoom(page);
	const reloaded = page.waitForResponse(response => response.url().endsWith('/structr/rest/_appmap') && response.request().method() === 'GET');

	await typeIntoTemplate(page, '<!-- saved -->');
	await page.keyboard.press('Control+s');
	await reloaded;

	await expect(state(page)).toHaveText('Ctrl+S saves');
	await expect.poll(() => editing(page)).toBe(ids.template);
	await expect(controls(page)).toBeVisible();

	// the closest zoom is that of the open page, not the one the map falls back to before its elements have their places
	const after = await zoom(page);

	expect(after.k).toBeCloseTo(before.k, 6);
	expect(after.hi).toBeCloseTo(before.hi, 6);

	// the elements come back where the layout remembered them, rounded to a tenth (AppMapLayout.restore), and the template
	// settles in the middle again: the view moves by at most that much at its zoom
	expect(Math.abs(after.x - before.x)).toBeLessThanOrEqual(0.051 * after.k);
	expect(Math.abs(after.y - before.y)).toBeLessThanOrEqual(0.051 * after.k);

	expect(await savedContent()).toContain('<!-- saved -->');

	// zoomed out to the page, the map zooms into the template again
	const edge   = await page.evaluate(() => { const box = _AppMap.view.canvas.getBoundingClientRect(); return { x: box.left + 8, y: box.top + box.height / 2 }; });

	await page.mouse.move(edge.x, edge.y);
	await zoomOutBelow(page, after.k / 2);
	await page.waitForTimeout(500);

	const out = (await zoom(page)).k;

	await page.mouse.wheel(0, -300);
	await expect.poll(async () => (await zoom(page)).k).toBeGreaterThan(out * 1.2);

	await builder.update('Template', ids.template, { content: CONTENT });
});

test('unsaved changes of a template survive zooming out, and the template can be zoomed into again', async ({page}) => {

	await openTemplate(page);
	await typeIntoTemplate(page, '<!-- kept -->');

	const inside = (await zoom(page)).k;
	const edge   = await page.evaluate(() => { const box = _AppMap.view.canvas.getBoundingClientRect(); return { x: box.left + 8, y: box.top + box.height / 2 }; });

	// the wheel at the edge of the map, beside the controls, zooms out until the template no longer fills the screen
	await page.mouse.move(edge.x, edge.y);
	await zoomOutBelow(page, inside / 2);
	await page.waitForTimeout(500);

	// the changes keep the controls with their template, but they do not hold the map
	expect(await editing(page)).toBe(ids.template);

	const out = (await zoom(page)).k;

	await page.mouse.wheel(0, -300);
	await expect.poll(async () => (await zoom(page)).k).toBeGreaterThan(out * 1.2);

	// back in the template, the changes are there
	const place = await templateOnScreen(page);

	await page.mouse.dblclick(place.x, place.y);
	await expect(code(page)).toBeVisible();
	await expect(code(page)).toContainText('<!-- kept -->');
	await expect(state(page)).toHaveText('Unsaved changes · Ctrl+S saves · Esc discards');

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
		await waitForMap(page);

		expect(await page.evaluate(() => _Helpers.isMac())).toBe(true);
	});

	test('the controls of a template name Cmd+S', async ({page}) => {

		await openTemplate(page);

		await expect(state(page)).toHaveText('Cmd+S saves');
	});
});
