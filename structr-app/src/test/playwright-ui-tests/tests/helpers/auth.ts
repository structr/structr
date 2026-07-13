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

	let loginForm = page.locator('#login #login-username-password');
	await expect(loginForm).toBeVisible();
	await expect(loginForm.locator('#usernameField')).toBeVisible();
	await expect(loginForm.locator('#passwordField')).toBeVisible();
	await expect(loginForm.locator('#loginButton')).toBeVisible();


	// Login with given credentials
	await loginForm.locator('#usernameField').fill(username);
	await loginForm.locator('#passwordField').fill(password);

	if (screenshot) {
		await page.screenshot({path: 'screenshots/login.png', caret: 'initial'});
	}

    // wait until websocket is ready
    await page.waitForFunction(() => StructrWS.wsReady, {}, { timeout: 10_000 });

	await loginForm.locator('#loginButton').click();

	// wait until we know login succeeded
    await page.waitForFunction(() => StructrWS.userId !== null, {}, { timeout: 10_000 });

	// then allow the URLs to go through their process. if we click during that process, it gets lost
	await page.waitForTimeout(1000);

	await expect(loginForm).toHaveCount(0);
}

export async function logout(page: Page) {

	await page.locator('.submenu-trigger').hover();
	await page.locator('#logout_').waitFor({ state: 'visible' });
	await page.locator('#logout_').click();
	await page.locator('#usernameField').waitFor({ state: 'visible' });
}