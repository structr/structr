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
import {goToModule, initialize} from './helpers/init';
import {login} from './helpers/auth';

// The editor of the application map: a click on readable code of a method glides so close that the method fills the
// screen, and there the editor of the Code area opens on its field. The map is a canvas, so the test asks the map where a
// method lies and clicks there with the mouse.

const ARCHIVE = '{\n\t$.this.archived = true;\n}';
const RESTORE = '{\n\t$.this.archived = false;\n}';

let context;
let ids = {};

test.beforeAll(async ({playwright}) => {

	context = await initialize(playwright);

	const create = async (type, data) => {

		const response = await context.post(process.env.BASE_URL + `/structr/rest/${type}`, { data: JSON.stringify(data) });

		expect(response.ok()).toBeTruthy();

		return (await response.json()).result[0];
	};

	const item = await create('SchemaNode', { name: 'Item' });

	ids.archive = await create('SchemaMethod', { name: 'archive', schemaNode: item, source: ARCHIVE });
	ids.restore = await create('SchemaMethod', { name: 'restore', schemaNode: item, source: RESTORE });

	// the schema compiles after the methods are created
	await new Promise(resolve => setTimeout(resolve, 1000));
});

// ----- the map, asked from inside the page -----
// zooms so that the code of the method stands readable in the middle of the map, and says where its code field and its
// header are on the screen
async function showCode(page, name) {

	const zoomTo = () => page.evaluate(name => {

		const view   = _AppMap.view;
		const n      = view.nodes.find(n => n.n === name);
		const target = AppMapPanels.CODE_READ * 1.8;

		// an element grows with the zoom, so its size is taken at the zoom looked for
		let k        = view.baseK;

		while (view.panels.codePanel(n, k, view.drawnRadiusAt(n, k)).px < target) {
			k *= 1.1;
		}

		d3.select(view.canvas).interrupt().call(view.zoom.transform, d3.zoomIdentity.translate(view.width / 2 - n.x * k, view.height / 2 - n.y * k).scale(k));
	}, name);

	// a fresh layout settles before the places count
	await page.waitForFunction(() => _AppMap.view?.layout && !_AppMap.view.layout.settling);
	await zoomTo();

	// the size of the field follows the length of the lines, known once the code is read
	await page.waitForFunction(name => _AppMap.view.panels.code.get(_AppMap.view.nodes.find(n => n.n === name))?.lines, name);
	await zoomTo();
	await page.waitForFunction(name => _AppMap.view.panels.reading.has(_AppMap.view.nodes.find(n => n.n === name)), name);

	return placesOf(page, name);
}

async function placesOf(page, name) {

	return page.evaluate(name => {

		const view             = _AppMap.view;
		const n                = view.nodes.find(n => n.n === name);
		const t                = view.transform;
		const box              = view.canvas.getBoundingClientRect();
		const panel            = view.panels.codePanel(n, t.k);
		const [fx, fy, fw, fh] = AppMapPanels.codeField(panel);
		const screen           = (x, y) => ({ x: box.left + t.applyX(x), y: box.top + t.applyY(y) });

		return {
			code:   screen(fx + fw / 2, fy + fh / 2),
			header: screen(n.x, panel.y0 + panel.pad + panel.title * 0.5)
		};
	}, name);
}

const editing  = page => page.evaluate(() => _AppMap.view.editing?.n ?? null);
const pinned   = page => page.evaluate(() => _AppMap.view.pinned?.n ?? null);
const editor   = page => page.locator('#appmap-canvas .appmap-editor');
const state    = page => page.locator('#appmap-canvas .appmap-editor-state');
const question = page => page.locator('.confirmationText');

async function answer(page, text) {

	await question(page).getByRole('button', { name: text }).click();
	await expect(question(page)).toHaveCount(0);
}

async function typeIntoEditor(page, text) {

	await editor(page).locator('.view-lines').click();
	await page.keyboard.press('Control+End');
	await page.keyboard.type(text);
	await expect(state(page)).toHaveText('Unsaved changes · Ctrl+S saves');
}

async function openEditorFor(page, name) {

	const places = await showCode(page, name);

	await page.mouse.click(places.code.x, places.code.y);
	await expect(editor(page)).toBeVisible();
	await expect(state(page)).toHaveText('Ctrl+S saves · Esc closes');
	expect(await editing(page)).toBe(name);

	return places;
}

async function closeCleanEditor(page) {

	if (await editing(page)) {

		await page.keyboard.press('Escape');

		if (await question(page).count()) {
			await answer(page, 'Yes');
		}

		await expect(editor(page)).toHaveCount(0);
	}
}

test.describe.configure({ mode: 'serial' });

test.beforeEach(async ({page}) => {

	await login(page);
	await goToModule(page, '#appmap_');
	await page.waitForFunction(() => _AppMap.view?.nodes?.some(n => n.n === 'Item.archive'));

	// the introduction of a first visit lies above the map
	await page.evaluate(() => document.querySelectorAll('#appmap-intro [data-intro-close]')[0]?.click());
});

test('a click on readable code opens the editor with the code', async ({page}) => {

	await openEditorFor(page, 'Item.archive');

	await expect(editor(page).locator('.view-lines')).toContainText('archived = true');
	expect(await pinned(page)).toBeNull();

	await page.screenshot({ path: 'screenshots/appmap_editor.png' });
});

test('a click on the header of a readable card opens no editor, the open card does not answer itself', async ({page}) => {

	const places = await showCode(page, 'Item.archive');

	await page.mouse.click(places.header.x, places.header.y);

	await expect(editor(page)).toHaveCount(0);
	expect(await pinned(page)).toBeNull();
});

test('a click on code that is only sketched holds the element and opens no editor', async ({page}) => {

	// close enough to see the shape of the code, too far to read it
	const place = await page.evaluate(() => {

		const view = _AppMap.view;
		const n    = view.nodes.find(n => n.n === 'Item.archive');
		let k      = view.baseK;

		while (view.panels.codePanel(n, k, view.drawnRadiusAt(n, k)).px < (AppMapPanels.CODE_SKETCH + AppMapPanels.CODE_READ) / 2) {
			k *= 1.05;
		}

		d3.select(view.canvas).interrupt().call(view.zoom.transform, d3.zoomIdentity.translate(view.width / 2 - n.x * k, view.height / 2 - n.y * k).scale(k));

		const box = view.canvas.getBoundingClientRect();

		return { x: box.left + view.width / 2, y: box.top + view.height / 2 };
	});

	await page.waitForTimeout(200);
	await page.mouse.click(place.x, place.y);

	await expect(editor(page)).toHaveCount(0);
	expect(await editing(page)).toBeNull();
	expect(await pinned(page)).toBe('Item.archive');
});

test('a click into the open editor keeps it open', async ({page}) => {

	await openEditorFor(page, 'Item.archive');

	await editor(page).locator('.view-lines').click();
	await editor(page).locator('.view-lines').click();

	await expect(editor(page)).toBeVisible();
	expect(await editing(page)).toBe('Item.archive');
});

test('a double click on readable code glides there, and the editor opens where the method fills the screen', async ({page}) => {

	const places    = await showCode(page, 'Item.archive');
	const transform = await page.evaluate(() => _AppMap.view.transform.toString());

	await page.mouse.dblclick(places.code.x, places.code.y);

	await expect(editor(page)).toBeVisible();
	expect(await editing(page)).toBe('Item.archive');
	expect(await page.evaluate(() => _AppMap.view.transform.toString())).not.toBe(transform);
	await expect.poll(() => page.evaluate(() => _AppMap.view.fills(_AppMap.view.nodes.find(n => n.n === 'Item.archive'), false))).toBe(true);
});

test('while the editor is open, the wheel only zooms out', async ({page}) => {

	await openEditorFor(page, 'Item.archive');

	// the glide into place ends first
	await page.waitForTimeout(1000);

	const k      = await page.evaluate(() => _AppMap.view.transform.k);
	const middle = await page.evaluate(() => { const box = _AppMap.view.canvas.getBoundingClientRect(); return { x: box.left + 8, y: box.top + box.height / 2 }; });

	await page.mouse.move(middle.x, middle.y);
	await page.mouse.wheel(0, -400);
	await page.waitForTimeout(500);

	expect(await page.evaluate(() => _AppMap.view.transform.k)).toBeCloseTo(k, 6);

	await page.mouse.wheel(0, 200);
	await expect.poll(() => page.evaluate(() => _AppMap.view.transform.k)).toBeLessThan(k);
});

test('a click beside the editor closes it, and the element held stays held', async ({page}) => {

	const places = await showCode(page, 'Item.archive');

	await page.evaluate(() => _AppMap.view.pin(_AppMap.view.nodes.find(n => n.n === 'Item.archive')));
	expect(await pinned(page)).toBe('Item.archive');

	await page.mouse.click(places.code.x, places.code.y);
	await expect(editor(page)).toBeVisible();

	// a corner of the map, far from every element
	const corner = await page.evaluate(() => { const box = _AppMap.view.canvas.getBoundingClientRect(); return { x: box.left + 8, y: box.bottom - 8 }; });

	await page.mouse.click(corner.x, corner.y);

	await expect(editor(page)).toHaveCount(0);
	expect(await pinned(page)).toBe('Item.archive');

	// the next click on the empty map ends exploring, as without the editor
	await page.mouse.click(corner.x, corner.y);
	expect(await pinned(page)).toBeNull();
});

test('Escape closes the editor, but not while it closes its own suggestions', async ({page}) => {

	await openEditorFor(page, 'Item.archive');

	await editor(page).locator('.view-lines').click();
	await page.keyboard.press('Control+Space');
	await expect(page.locator('.suggest-widget.visible')).toBeVisible();

	await page.keyboard.press('Escape');
	await expect(page.locator('.suggest-widget.visible')).toHaveCount(0);
	await expect(editor(page)).toBeVisible();

	await page.keyboard.press('Escape');
	await expect(editor(page)).toHaveCount(0);
	expect(await editing(page)).toBeNull();
});

test('a click on the code of another method moves the editor there', async ({page}) => {

	await openEditorFor(page, 'Item.archive');

	const other = await showCode(page, 'Item.restore');

	await page.mouse.click(other.code.x, other.code.y);

	await expect.poll(() => editing(page)).toBe('Item.restore');
	await expect(editor(page)).toHaveCount(1);
	await expect(editor(page).locator('.view-lines')).toContainText('archived = false');
});

test('unsaved changes are kept unless they are discarded, whichever way the editor is left', async ({page}) => {

	await openEditorFor(page, 'Item.archive');
	await typeIntoEditor(page, '\n// changed');

	// Escape asks; No keeps the changes
	await page.keyboard.press('Escape');
	await expect(question(page)).toBeVisible();
	await answer(page, 'No');
	await expect(editor(page)).toBeVisible();
	await expect(state(page)).toHaveText('Unsaved changes · Ctrl+S saves');

	// a click beside the editor asks; Escape in the question cancels it like No and asks nothing more
	const corner = await page.evaluate(() => { const box = _AppMap.view.canvas.getBoundingClientRect(); return { x: box.left + 8, y: box.bottom - 8 }; });

	await page.mouse.click(corner.x, corner.y);
	await expect(question(page)).toHaveCount(1);
	await page.keyboard.press('Escape');
	await expect(question(page)).toHaveCount(0);
	await page.waitForTimeout(300);
	await expect(question(page)).toHaveCount(0);
	await expect(editor(page)).toBeVisible();
	await expect(state(page)).toHaveText('Unsaved changes · Ctrl+S saves');

	// a click on the code of another method asks; No stays, Yes discards and opens the other method
	const other = await showCode(page, 'Item.restore');

	await page.mouse.click(other.code.x, other.code.y);
	await expect(question(page)).toBeVisible();
	await answer(page, 'No');
	expect(await editing(page)).toBe('Item.archive');

	await page.mouse.click(other.code.x, other.code.y);
	await answer(page, 'Yes');
	await expect.poll(() => editing(page)).toBe('Item.restore');

	// nothing was saved
	const response = await context.get(process.env.BASE_URL + `/structr/rest/SchemaMethod/${ids.archive}`);

	expect((await response.json()).result.source).toBe(ARCHIVE);
});

test('the editor waits hidden while the code is too small to read and while the hotspots show', async ({page}) => {

	await openEditorFor(page, 'Item.archive');
	await typeIntoEditor(page, '\n// kept');

	// zoomed out the code is no longer readable
	await page.evaluate(() => _AppMap.view.fitAll());
	await expect(editor(page)).toBeHidden();
	expect(await editing(page)).toBe('Item.archive');

	// zoomed in again it shows with the changes
	await showCode(page, 'Item.archive');
	await expect(editor(page)).toBeVisible();
	await expect(editor(page).locator('.view-lines')).toContainText('// kept');

	// the hotspots grey out the map, the editor waits meanwhile
	await page.locator('#appmap-spotlight').hover();
	await expect(editor(page)).toBeHidden();

	await page.mouse.move(10, 10);
	await expect(editor(page)).toBeVisible();

	await closeCleanEditor(page);
});

test('Ctrl+S saves, the map loads its data anew and the editor stays open where it was', async ({page}) => {

	await openEditorFor(page, 'Item.archive');

	const transform = await page.evaluate(() => _AppMap.view.transform.toString());
	const places    = await page.evaluate(() => _AppMap.view.nodes.map(n => [n.id, n.x, n.y]));
	const reloaded  = page.waitForResponse(response => response.url().endsWith('/structr/rest/_appmap') && response.request().method() === 'GET');

	await typeIntoEditor(page, '\n// saved');
	await page.keyboard.press('Control+s');

	await reloaded;
	await expect(state(page)).toHaveText('Ctrl+S saves · Esc closes');
	await expect(editor(page)).toBeVisible();
	await expect.poll(() => editing(page)).toBe('Item.archive');

	const response = await context.get(process.env.BASE_URL + `/structr/rest/SchemaMethod/${ids.archive}`);

	expect((await response.json()).result.source).toContain('// saved');
	expect(await page.evaluate(() => _AppMap.view.transform.toString())).toBe(transform);

	// the elements keep their places as the layout remembered them, rounded to a tenth (AppMapLayout.restore), so
	// each lies at most half a tenth from where it was, plus what floating point adds
	const after = await page.evaluate(() => _AppMap.view.nodes.map(n => [n.id, n.x, n.y]));

	expect(after.map(([id]) => id)).toEqual(places.map(([id]) => id));

	after.forEach(([id, x, y], i) => {
		expect(Math.abs(x - places[i][1]), `x of ${id}`).toBeLessThanOrEqual(0.051);
		expect(Math.abs(y - places[i][2]), `y of ${id}`).toBeLessThanOrEqual(0.051);
	});

	// saved, it closes without a question
	await page.keyboard.press('Escape');
	await expect(question(page)).toHaveCount(0);
	await expect(editor(page)).toHaveCount(0);
});

test('unsaved changes keep the map open when another module is chosen, unless they are discarded', async ({page}) => {

	await openEditorFor(page, 'Item.restore');
	await typeIntoEditor(page, '\n// left');

	page.once('dialog', dialog => dialog.dismiss());
	await page.evaluate(() => Structr.activateModule('dashboard'));
	await page.waitForTimeout(500);

	await expect(editor(page)).toBeVisible();
	expect(await page.evaluate(() => Structr.getActiveModule()?._moduleName)).toBe('appmap');

	page.once('dialog', dialog => dialog.accept());
	await page.evaluate(() => Structr.activateModule('dashboard'));

	await expect.poll(() => page.evaluate(() => Structr.getActiveModule()?._moduleName)).toBe('dashboard');
	await expect(editor(page)).toHaveCount(0);
});
