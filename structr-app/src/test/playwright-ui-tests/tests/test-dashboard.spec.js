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
import {goToModule, initialize} from "./helpers/init";
import {login, logout} from "./helpers/auth";

test.beforeAll(async ({playwright}) => {
	await initialize(playwright);
});

test('dashboard', async ({page}, testInfo) => {

	console.log(testInfo.title);

	await login(page, true);

	await goToModule(page, '#dashboard_');

	await page.locator('[href="#dashboard:me"]').waitFor({state: 'visible'});

	// Dashboard -> About Me
	await page.locator('[href="#dashboard:me"]').click();
	await page.locator('[data-module-name="me"]').waitFor({state: 'visible'});
	await page.screenshot({path: 'screenshots/dashboard_about-me.png'});

	// Dashboard -> About Structr
	await page.locator('[href="#dashboard:about"]').click();
	await page.locator('[data-module-name="about"]').waitFor({state: 'visible'});

	// Wait for HTTP Access Statistics to load (vis.js is async and has no onLoad callback)
	await page.waitForTimeout(2000);
	await page.screenshot({path: 'screenshots/dashboard_about-structr.png'});

	// Dashboard -> Deployment
	await page.locator('[href="#dashboard:deployment"]').click();
	await page.locator('[data-module-name="deployment"]').waitFor({state: 'visible'});
	await page.screenshot({path: 'screenshots/dashboard_deployment.png'});

	// Dashboard -> Methods
	await page.locator('[href="#dashboard:methods"]').click();
	await page.locator('[data-module-name="methods"]').waitFor({state: 'visible'});
	await page.screenshot({path: 'screenshots/dashboard_methods.png'});

	// Dashboard -> Server Log
	await page.locator('[href="#dashboard:logs"]').click();
	await page.locator('[data-module-name="logs"]').waitFor({state: 'visible'});
	await page.screenshot({path: 'screenshots/dashboard_server-log.png'});

	// Dashboard -> Event Log
	await page.locator('[href="#dashboard:events"]').click();
	await page.locator('[data-module-name="events"]').waitFor({state: 'visible'});
	await page.screenshot({path: 'screenshots/dashboard_event-log.png'});

	// Dashboard -> Threads
	await page.locator('[href="#dashboard:threads"]').click();
	await page.locator('[data-module-name="threads"]').waitFor({state: 'visible'});
	await page.screenshot({path: 'screenshots/dashboard_running-threads.png'});

	// Dashboard -> UI Settings
	await page.locator('[href="#dashboard:ui"]').click();
	await page.locator('[data-module-name="ui"]').waitFor({state: 'visible'});
	await page.screenshot({path: 'screenshots/dashboard_ui-config.png'});

	// show admin console here
	await page.locator('#terminal-icon > use').click();
	let term = page.locator('#structr-console.console-open');
	await expect(term).toBeVisible();
	await term.click();
	await page.keyboard.type('$.find("User")');
	await page.keyboard.press('Enter');
	await page.screenshot({path: 'screenshots/dashboard_admin-console.png'});
	await page.locator('#terminal-icon > use').click();

	await logout(page);
});

