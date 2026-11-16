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
import {ADMIN_PASSWORD} from "./helpers/auth";

test.beforeAll(async ({playwright}) => {
	await initialize(playwright);
});

test('login', async ({page}, testInfo) => {

	console.log(testInfo.title);

	await page.goto(process.env.BASE_URL + '/structr/');

	// the login is a named dialog, and the user name has the focus
	let dialog = page.getByRole('dialog', {name: 'Log in to Structr'});
	await expect(dialog).toBeVisible();
	await expect(page.locator('#usernameField')).toBeFocused();

	// the fields say what they hold, for password managers and for phones
	await expect(page.locator('#usernameField')).toHaveAttribute('autocomplete', 'username');
	await expect(page.locator('#usernameField')).toHaveAttribute('autocapitalize', 'none');
	await expect(page.locator('#passwordField')).toHaveAttribute('autocomplete', 'current-password');
	await expect(page.locator('#twoFactorCodeField')).toHaveAttribute('autocomplete', 'one-time-code');

	// Tab stays inside the dialog
	await page.locator('#loginButton').focus();
	await page.keyboard.press('Tab');
	await expect(page.locator('#usernameField')).toBeFocused();
	await page.keyboard.press('Shift+Tab');
	await expect(page.locator('#loginButton')).toBeFocused();

	await page.waitForFunction(() => StructrWS.wsReady, {}, {timeout: 10_000});

	// a wrong password: the message is announced and stays, the password is ready to be typed over
	await page.locator('#usernameField').fill('admin');
	await page.locator('#passwordField').fill('not-the-password');
	await page.keyboard.press('Enter');

	let message = page.locator('#login .login-error-message');
	await expect(message).toBeVisible();
	await expect(message).toHaveAttribute('role', 'alert');
	await expect(page.locator('#passwordField')).toBeFocused();

	await page.waitForTimeout(3000);
	await expect(message).toBeVisible();

	// the right password: the message goes with the attempt, the dialog with the login
	await page.keyboard.type(ADMIN_PASSWORD);
	await page.keyboard.press('Enter');

	await page.waitForFunction(() => StructrWS.userId !== null, {}, {timeout: 10_000});
	await expect(dialog).toHaveCount(0);
});
