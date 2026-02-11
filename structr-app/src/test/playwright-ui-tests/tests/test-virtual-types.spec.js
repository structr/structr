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
import {initialize} from "./helpers/init";

test.beforeAll(async ({playwright}) => {
    await initialize(playwright);
});

test('virtual-types', async ({page}) => {

    await page.goto(process.env.BASE_URL + '/structr/');

    await expect(page).toHaveTitle(/Structr/);
    await expect(page.locator('#usernameField')).toBeVisible();
    await expect(page.locator('#passwordField')).toBeVisible();
    await expect(page.locator('#loginButton')).toBeVisible();

    await page.waitForTimeout(1000);

    // Login with admin/admin
    await page.locator('#usernameField').fill('admin');
    await page.locator('#passwordField').fill('admin');
    await page.waitForTimeout(500);
    await page.locator('#loginButton').click();
    await page.waitForTimeout(1000);

    await page.locator('.submenu-trigger').hover();
    await page.locator('#virtual-types_').waitFor({state: 'visible'});
    await page.locator('#virtual-types_').click();

    // Wait for Code UI to load all components
    await page.waitForTimeout(1000);
    await page.getByRole('textbox', {name: 'Enter a name for the virtual'}).fill('VirtualProject');
    await page.getByRole('textbox', {name: 'Enter the name of the source'}).fill('Project');
    await page.getByRole('button', {name: 'Create Virtual Type'}).click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/virtual-types.png'});

    // Logout
    await page.locator('.submenu-trigger').hover();
    await page.waitForTimeout(500);
    await page.locator('#logout_').waitFor({state: 'visible'});
    await page.locator('#logout_').click();
    await page.locator('#usernameField').waitFor({state: 'visible'});
    await page.waitForTimeout(1000);

});
