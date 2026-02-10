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

const fs = require('fs');

test.beforeAll(async ({playwright}) => {
    await initialize(playwright);
});

test('configuration-interface', async ({page}) => {

    await page.goto(process.env.BASE_URL + '/structr/config');

    await expect(page.locator('#passwordField')).toBeVisible();
    await expect(page.locator('#loginButton')).toBeVisible();

    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/configuration-interface_login.png'});

    // Login with admin/admin
    await page.locator('#passwordField').fill(process.env.SUPERUSER_PASSWORD);
    await page.waitForTimeout(500);
    await page.locator('#loginButton').click();
    await page.waitForTimeout(1000);

    // Wait for Code UI to load all components
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/configuration-interface.png'});

    // Logout
    await page.getByRole('link', {name: 'Logout'}).click();
    await page.waitForTimeout(1000);

});
