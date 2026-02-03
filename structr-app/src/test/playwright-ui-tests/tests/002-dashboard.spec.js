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

test.beforeAll(async ({ playwright }) => {

  const context = await playwright.request.newContext({
    extraHTTPHeaders: {
      'Accept': 'application/json',
      'X-User': 'superadmin',
      'X-Password': process.env.SUPERUSER_PASSWORD,
    }
  });

  // Clear database
  await context.post(process.env.BASE_URL + '/structr/rest/maintenance/clearDatabase');

  // Create new admin user
  await context.post(process.env.BASE_URL + '/structr/rest/User',  {
    data: {
      name: 'admin',
      password: 'admin',
      isAdmin: true
    }
  });

});

test('dashboard', async ({ page }) => {

  test.setTimeout(240_000);

  await page.goto(process.env.BASE_URL + '/structr/');
  //await page.evaluate('document.body.style.zoom="2.0"');

  await expect(page).toHaveTitle(/Structr/);
  await expect(page.locator('#usernameField')).toBeVisible();
  await expect(page.locator('#passwordField')).toBeVisible();
  await expect(page.locator('#loginButton')).toBeVisible();

  // Login with admin/admin
  await page.locator('#usernameField').fill('admin');
  await page.locator('#passwordField').fill('admin');
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/login.png', caret: 'initial' });
  await page.locator('#loginButton').click();
  await page.waitForTimeout(1000);

  // Dashboard
  await page.locator('#dashboard_').waitFor({ state: 'visible' });
  await page.locator('#dashboard_').click();
  await page.locator('[href="#dashboard:about-me"]').waitFor({ state: 'visible' });

  // Dashboard -> About Me
  await page.locator('[href="#dashboard:about-me"]').click();
  await page.locator('#dashboard-about-me').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/dashboard_about-me.png' });

  // Dashboard -> About Structr
  await page.locator('[href="#dashboard:about-structr"]').click();
  await page.locator('#dashboard-about-structr').waitFor({ state: 'visible' });

  // Wait for HTTP Access Statistics to load
  await page.waitForTimeout(2000);
  await page.screenshot({ path: 'screenshots/dashboard_about-structr.png' });

  // Dashboard -> Deployment
  await page.locator('[href="#dashboard:deployment"]').click();
  await page.locator('#dashboard-deployment').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/dashboard_deployment.png' });

  // Dashboard -> Methods
  await page.locator('[href="#dashboard:methods"]').click();
  await page.locator('#dashboard-methods').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/dashboard_methods.png' });

  // Dashboard -> Server Log
  await page.locator('[href="#dashboard:server-log"]').click();
  await page.locator('#dashboard-server-log').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/dashboard_server-log.png' });

  // Dashboard -> Event Log
  await page.locator('[href="#dashboard:event-log"]').click();
  await page.locator('#dashboard-event-log').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/dashboard_event-log.png' });

  // Dashboard -> Threads
  await page.locator('[href="#dashboard:running-threads"]').click();
  await page.locator('#dashboard-running-threads').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/dashboard_running-threads.png' });

  // Dashboard -> UI Settings
  await page.locator('[href="#dashboard:ui-config"]').click();
  await page.locator('#dashboard-ui-config').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/dashboard_ui-config.png' });

  // show admin console here
  await page.locator('#terminal-icon > use').click();
  await page.waitForTimeout(200);
  await page.keyboard.type('$.find("Project");');
  await page.keyboard.press('Enter');
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/dashboard_admin-console.png' });
  await page.locator('#terminal-icon > use').click();
  await page.waitForTimeout(200);

  // Logout
  await page.locator('.submenu-trigger').hover();
  await page.waitForTimeout(250);
  await page.locator('#logout_').waitFor({ state: 'visible' });
  await page.locator('#logout_').click();
  await page.locator('#usernameField').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);

});

