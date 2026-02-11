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
import { expect, Page } from '@playwright/test';

export async function login(page: Page, screenshot: boolean = false) {

    let username = 'admin';
    let password = 'admin';

    await page.goto(process.env.BASE_URL + '/structr/');

    await expect(page).toHaveTitle(/Structr/);
    await expect(page.locator('#usernameField')).toBeVisible();
    await expect(page.locator('#passwordField')).toBeVisible();
    await expect(page.locator('#loginButton')).toBeVisible();

    await page.waitForTimeout(1000);

    // Login with given credentials
    await page.locator('#usernameField').fill(username);
    await page.locator('#passwordField').fill(password);

    if (screenshot) {
        await page.screenshot({path: 'screenshots/login.png', caret: 'initial'});
    }

    await page.waitForTimeout(500);
    await page.locator('#loginButton').click();

    await page.waitForTimeout(1000);
}

export async function logout(page: Page) {

    await page.locator('.submenu-trigger').hover();
    await page.waitForTimeout(500);
    await page.locator('#logout_').waitFor({ state: 'visible' });
    await page.locator('#logout_').click();
    await page.locator('#usernameField').waitFor({ state: 'visible' });
    await page.waitForTimeout(1000);
}