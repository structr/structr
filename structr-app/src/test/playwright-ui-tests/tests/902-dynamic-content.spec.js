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

    // Clear all pages and create some projects
    await context.delete(process.env.BASE_URL + '/structr/rest/Page');
    await context.delete(process.env.BASE_URL + '/structr/rest/Project');
    await context.post(process.env.BASE_URL + '/structr/rest/Project', { data: { name: 'Project #1'}});
    await context.post(process.env.BASE_URL + '/structr/rest/Project', { data: { name: 'Project #2'}});
    await context.post(process.env.BASE_URL + '/structr/rest/Project', { data: { name: 'Project #3'}});
});

test('dynamic-content', async ({ page }) => {

	test.setTimeout(240_000);

	await login(page);

    // Pages
    await page.locator('#pages_').waitFor({ state: 'visible' });
    await page.locator('#pages_').click();

    await page.locator('#pages-actions .dropdown-select').click();
    await page.locator('#create_page').waitFor({ state: 'visible' });
    await page.locator('#create_page').click();
    await page.waitForTimeout(1000);
    await page.locator('#template-tiles .app-tile:nth-child(2)').click();
    await page.waitForTimeout(2000);

    // expand new page completely
    await page.locator('#pagesTree .node-container:nth-child(1)').click({ button: 'right' });
    await page.getByText('Expand / Collapse').waitFor({ state: 'visible' });
    await page.getByText('Expand / Collapse').hover();
    await page.waitForTimeout(1000);
    await page.getByText('Expand subtree recursively').waitFor({ state: 'visible' });
    await page.getByText('Expand subtree recursively').click();
    await page.waitForTimeout(1000);

    // Drag 'Simple List' widget onto 'Main Container'
    await page.locator('#widgetsTab').click();
    await page.waitForTimeout(1000);
    await page.getByText('Simple List', { exact: true }).hover();
    await page.mouse.down();
    await page.locator('#pagesTree').getByText('Main Container', { exact: true }).hover();
    await page.mouse.up();
    await page.waitForTimeout(1000);
    await page.locator('#widgetsTab').click();

    await page.getByText('li.px-6.py-').nth(1).click({ button: 'right' });
    await page.getByText('Remove Node').click();
    await page.getByText('li.px-6.py-').nth(1).click({ button: 'right' });
    await page.getByText('Remove Node').click();

    await page.getByText('li.px-6.py-').nth(0).click();
    await page.getByRole('link', { name: 'Repeater' }).click();
    await page.getByRole('button', { name: 'Function Query' }).click();
    await page.waitForTimeout(1000);
    await page.locator('.view-line').click();
    await page.getByRole('textbox', { name: 'Editor content' }).fill('find(\'Project\')');
    await page.getByRole('button', { name: 'Save' }).first().click();
    await page.locator('#center-pane input[type="text"]').click();
    await page.locator('#center-pane input[type="text"]').fill('project');
    await page.getByRole('button', { name: 'Save' }).nth(1).click();
    await page.waitForTimeout(1000);

    await page.getByRole('link', { name: 'Editor' }).click();
    await page.waitForTimeout(100);
    await page.locator('.view-line').click();
    await page.getByRole('textbox').nth(5).fill('${project.name}');
    await page.getByRole('button', { name: 'Save' }).click();
    await page.waitForTimeout(1000);
    await page.getByRole('link', { name: 'Preview' }).click();
    await page.getByText('Header', { exact: true }).click();

    await page.locator('#pagesTree div').filter({ hasText: 'Simple List' }).nth(4).screenshot({ path: 'screenshots/pages_single-repeater-element.png' });

    await page.waitForTimeout(1000);
    await page.waitForTimeout(1000);
    await page.waitForTimeout(1000);
	await page.waitForTimeout(1000);

	await logout(page);
});






























