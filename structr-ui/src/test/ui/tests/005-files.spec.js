/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
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

  // Clear all files and folders
  await context.delete(process.env.BASE_URL + '/structr/rest/File');
  await context.delete(process.env.BASE_URL + '/structr/rest/Folder');

});

test('files', async ({ page }) => {

  test.setTimeout(120_000);

  //await page.setViewportSize({ width: 3840, height: 2160 });
  await page.goto(process.env.BASE_URL + '/structr/');
  //await page.evaluate('document.body.style.zoom="2.0"');

  await expect(page).toHaveTitle(/Structr/);
  await expect(page.locator('#usernameField')).toBeVisible();
  await expect(page.locator('#passwordField')).toBeVisible();
  await expect(page.locator('#loginButton')).toBeVisible();

  await page.waitForTimeout(1000);

  //await page.screenshot({ path: 'login.png' });

  // Login with admin/admin
  await page.locator('#usernameField').fill('admin');
  await page.locator('#passwordField').fill('admin');
  await page.waitForTimeout(500);
  await page.locator('#loginButton').click();
  await page.waitForTimeout(1000);

  // Files
  await page.locator('#files_').waitFor({ state: 'visible' });
  await page.locator('#files_').click();

  // Wait for Files UI to load all components
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'files.png' });

  // Create new folder
  await page.locator('#add-folder-button').waitFor({ state: 'visible' });
  await page.locator('#add-folder-button').click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'files_create-folder.png' });
  await page.locator('#file-tree-container').getByText('New Folder').first().click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'files_navigated-to-folder.png' });

  // Rename folder
  await page.locator('#file-tree-container').getByText('/').first().click();
  await page.locator('#folder-contents-container').getByText('New Folder').first().click();
  await page.keyboard.type('Renamed folder');
  await page.keyboard.press('Enter');
  await page.locator('#file-tree-container').click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'files_renamed-folder.png' });

  // Logout
  await page.locator('.submenu-trigger').hover();
  await page.waitForTimeout(500);
  await page.locator('#logout_').waitFor({ state: 'visible' });
  await page.locator('#logout_').click();
  await page.locator('#usernameField').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);

});

