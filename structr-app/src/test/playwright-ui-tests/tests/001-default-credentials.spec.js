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
import {goToModule, initialize, waitForDialogBoxToClose} from "./helpers/init";
import {ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD, login, logout} from "./helpers/auth";

test.beforeAll(async ({playwright}) => {

	// deliberately the built-in default: this is the one spec that runs on it. Every other spec's
	// initialize() creates the admin with a password of its own, so the warning never appears there.
	await initialize(playwright, null, DEFAULT_ADMIN_PASSWORD);
});

/**
 * The default-credentials warning, and its disappearance.
 *
 * Deliberately numbered 001: it is the only spec that runs against the built-in admin/admin, and it
 * leaves the instance on a password of its own so that every later spec is not covered by a warning it
 * is not testing. The suite runs with --workers=1, so file order is the ordering guarantee.
 */
test('default-credentials-warning', async ({page}, testInfo) => {

	console.log(testInfo.title);

	// ----- still on the default password -----
	await login(page, false, DEFAULT_ADMIN_PASSWORD);

	const warning = page.locator('#info-area .message').filter({hasText: 'default password'});

	await expect(warning, 'logging in on the default password must warn about it').toBeVisible();
	await page.screenshot({path: 'screenshots/default-credentials_login-warning.png'});

	// dismiss it, otherwise it covers what comes next
	await warning.locator('.close-message-button').click();
	await expect(warning).toHaveCount(0);

	// the dashboard says so too, and keeps saying so
	await goToModule(page, '#dashboard_');
	await page.locator('[href="#dashboard:about"]').click();
	await page.locator('[data-module-name="about"]').waitFor({state: 'visible'});

	const dashboardWarning = page.locator('#security-warnings').filter({hasText: 'default password'});

	await expect(dashboardWarning, 'the dashboard must show the warning permanently').toBeVisible();
	await page.screenshot({path: 'screenshots/default-credentials_dashboard-warning.png'});

	// ----- change it, through the UI the admin would actually use -----
	await goToModule(page, '#security_');
	await page.waitForTimeout(1000);

	await page.locator('#users').getByText('admin', {exact: true}).first().click({button: 'right'});
	await page.getByText('General').first().click();
	await page.locator('input#password-input').dblclick();
	await page.keyboard.type(ADMIN_PASSWORD);

	// by id, not by text: the General tab of the user dialog gained more buttons, and a text locator on a
	// dialog that grows is one relabelling away from clicking the wrong thing
	await page.locator('#set-password-button').click();
	await page.getByRole('button', {name: 'Close', exact: true}).click();

	await waitForDialogBoxToClose(page);

	// ----- and it is gone, without a restart -----
	await logout(page);
	await login(page, false, ADMIN_PASSWORD);

	await expect(page.locator('#info-area .message').filter({hasText: 'default password'}),
		'the warning must not appear once the password has been changed').toHaveCount(0);

	await goToModule(page, '#dashboard_');
	await page.locator('[href="#dashboard:about"]').click();
	await page.locator('[data-module-name="about"]').waitFor({state: 'visible'});

	await expect(page.locator('#security-warnings').filter({hasText: 'default password'}),
		'the dashboard warning must clear once the password has been changed').toHaveCount(0);

	await page.screenshot({path: 'screenshots/default-credentials_cleared.png'});

	await logout(page);
});
