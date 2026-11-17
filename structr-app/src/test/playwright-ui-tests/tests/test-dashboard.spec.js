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

	// the area is one page of sections: About Me, About Structr and Deployment
	await page.locator('[data-module-name="me"]').waitFor({state: 'visible'});
	await page.screenshot({path: 'screenshots/dashboard_about-me.png'});

	await page.locator('[data-module-name="about"]').waitFor({state: 'visible'});
	await page.screenshot({path: 'screenshots/dashboard_about-structr.png'});

	await page.locator('[data-module-name="deployment"]').scrollIntoViewIfNeeded();
	await page.screenshot({path: 'screenshots/dashboard_deployment.png'});

	// the logs, the threads and the HTTP statistics open in the panel at the foot of the window
	await page.locator('#monitor-switches [data-view="logs"]').click();
	await page.locator('#monitor-panel [data-module-name="logs"]').waitFor({state: 'visible'});
	await expect(page.locator('#monitor-panel #dashboard-server-log textarea')).not.toHaveValue('');
	await page.screenshot({path: 'screenshots/dashboard_server-log.png'});

	await page.locator('#monitor-switches [data-view="events"]').click();
	await page.locator('#monitor-panel [data-module-name="events"]').waitFor({state: 'visible'});
	await page.locator('#monitor-panel #event-log-container tr').first().waitFor({state: 'visible'});
	await page.screenshot({path: 'screenshots/dashboard_event-log.png'});

	await page.locator('#monitor-switches [data-view="threads"]').click();
	await page.locator('#monitor-panel [data-module-name="threads"]').waitFor({state: 'visible'});
	await page.locator('#monitor-panel [data-module-name="threads"] tbody tr').first().waitFor({state: 'visible'});
	await page.screenshot({path: 'screenshots/dashboard_running-threads.png'});

	// HTTP statistics: vis.js is async and has no onLoad callback
	await page.locator('#monitor-switches [data-view="stats"]').click();
	await page.locator('#monitor-panel [data-module-name="stats"]').waitFor({state: 'visible'});
	await page.waitForTimeout(2000);
	await page.screenshot({path: 'screenshots/dashboard_http-statistics.png'});

	await page.locator('#monitor-panel-close').click();
	await expect(page.locator('#monitor-panel')).toBeHidden();

	// the settings of the UI: the most used ones in the header menu, all of them in a dialog
	await page.locator('#ui-settings-popup > button').click();
	await page.getByRole('link', {name: 'All UI settings'}).click();
	await page.locator('#dialogBox #dashboard-ui-config').waitFor({state: 'visible'});
	await page.screenshot({path: 'screenshots/dashboard_ui-config.png'});
	await page.getByRole('button', {name: 'Close', exact: true}).click();
	await expect(page.locator('#dialogBox #dashboard-ui-config')).toBeHidden();

	// show admin console here
	await page.locator('#terminal-icon').click();
	let term = page.locator('#structr-console.console-open');
	await expect(term).toBeVisible();
	await term.click();
	await page.keyboard.type('$.find("User")');
	await page.keyboard.press('Enter');
	await page.screenshot({path: 'screenshots/dashboard_admin-console.png'});
	await page.locator('#terminal-icon').click();

	await logout(page);
});

