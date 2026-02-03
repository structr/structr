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
import { test, expect } from '@playwright/test';
import { login, logout } from './helpers/auth';

test.beforeAll(async ({ playwright }) => {

	const context = await playwright.request.newContext({
		extraHTTPHeaders: {
			'Accept': 'application/json',
			'X-User': 'superadmin',
			'X-Password': process.env.SUPERUSER_PASSWORD,
		}
	});
});

test('search-and-refactor-code', async ({ page }) => {

	test.setTimeout(240_000);

	let oldName = 'milestones';
	let newName = 'goals';

	await login(page);

	// Navigate to security (to show that automatic navigation to code works)
	await page.locator('#header #security_').waitFor({state: 'visible'});
	await page.locator('#header #security_').click();

	let globalSearchActivator = await page.locator('[popovertarget="global-search-popover"]');
	await globalSearchActivator.click();

	let searchInput = await page.locator('#global-search-node-form input[name="queryString"]');
	await searchInput.waitFor({state: 'visible'});

	await searchInput.fill(oldName);

	// wait for results
	let relationshipResultRow = await page.locator('div[data-key="targetJsonName"]').first();

	await page.screenshot({ path: 'screenshots/global-search.png' });

	await relationshipResultRow.locator('button').click();

	// Wait for Code UI to load all components
	await page.waitForTimeout(1000);

	let input = await page.locator('input[data-attr-name="targetJsonName"]');

	// click on the backdrop to hide global search popover
	await globalSearchActivator.click();

	await input.fill('');
	await input.pressSequentially(newName);

	await page.locator('[id="action-button-save"]').click();

	await page.waitForTimeout(1000);


	let updateFirstOccurrenceOfTextInMonaco = async (oldText, newText) => {

		await page.locator('#center-pane .monaco-editor').first().click();

		await page.keyboard.press(process.platform === 'darwin' ? 'Meta+F' : 'Control+F');
		await page.keyboard.type(oldText);
		await page.keyboard.press(process.platform === 'darwin' ? 'Escape' : 'Escape');
		await page.keyboard.type(newText);
		await page.keyboard.press(process.platform === 'darwin' ? 'Meta+S' : 'Control+S');

		await page.waitForTimeout(1000);
	};

	/**
	 * Update first content element (either column header or data column)
	 */
	await globalSearchActivator.click();

	// update search results
	await searchInput.fill('');
	await searchInput.fill(oldName);

	await page.waitForTimeout(500);

	let contentResultRow = await page.locator('div[data-key="content"]').first();
	await contentResultRow.locator('button').click();

	await page.waitForTimeout(1000);

	// click on the backdrop to hide global search popover
	await globalSearchActivator.click();

	await updateFirstOccurrenceOfTextInMonaco(oldName, newName);


	/**
	 * Update second content element (either column header or data column)
	 */
	await globalSearchActivator.click();

	// update search results
	await searchInput.fill('');
	await searchInput.fill(oldName);

	await page.waitForTimeout(500);

	contentResultRow = await page.locator('div[data-key="content"]').first();
	await contentResultRow.locator('button').click();

	await page.waitForTimeout(1000);

	// click on the backdrop to hide global search popover
	await globalSearchActivator.click();

	await updateFirstOccurrenceOfTextInMonaco(oldName, newName);


	// show preview again
	await page.locator('#tabs-menu-preview').click();

	await page.waitForTimeout(1000);

	await logout(page);
});