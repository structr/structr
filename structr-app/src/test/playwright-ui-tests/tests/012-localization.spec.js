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
const fs = require('fs');

test.beforeAll(async ({ playwright }) => {

  const context = await playwright.request.newContext({
    extraHTTPHeaders: {
      'Accept': 'application/json',
      'X-User': 'superadmin',
      'X-Password': process.env.SUPERUSER_PASSWORD,
    }
  });

  // Clear database
  await context.delete(process.env.BASE_URL + '/structr/rest/Localization');
});

test('localization', async ({ page }) => {

  test.setTimeout(240_000);

  //await page.setViewportSize({ width: 3840, height: 2160 });
  await page.goto(process.env.BASE_URL + '/structr/');
  //await page.evaluate('document.body.style.zoom="2.0"');

  await expect(page).toHaveTitle(/Structr/);
  await expect(page.locator('#usernameField')).toBeVisible();
  await expect(page.locator('#passwordField')).toBeVisible();
  await expect(page.locator('#loginButton')).toBeVisible();

  await page.waitForTimeout(1000);

  //await page.screenshot({ path: 'screenshots/login.png' });

  // Login with admin/admin
  await page.locator('#usernameField').fill('admin');
  await page.locator('#passwordField').fill('admin');
  await page.waitForTimeout(500);
  await page.locator('#loginButton').click();
  await page.waitForTimeout(1000);

  await page.locator('.submenu-trigger').hover();
  await page.locator('#localization_').waitFor({state: 'visible'});
  await page.locator('#localization_').click();

  // Wait for Code UI to load all components
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/localization.png' });

  // Create new Localization elements
  await page.getByRole('textbox', { name: 'Enter a key to create' }).click();
  await page.keyboard.type('milestones');
  await page.keyboard.press('Tab');
  await page.keyboard.type('table-header');
  await page.keyboard.press('Tab');
  await page.keyboard.type('en,de');
  await page.getByRole('button', {name: 'Create Localization'}).click();
  await page.waitForTimeout(500);
  await page.locator('.___localizedName').nth(0).click();
  await page.keyboard.type('Meilensteine');
  await page.waitForTimeout(500);
  await page.locator('.___localizedName').nth(1).click();
  await page.keyboard.type('Milestones');
  await page.keyboard.press('Tab');
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/localization_created.png' });

  // Logout
  await page.locator('.submenu-trigger').hover();
  await page.waitForTimeout(500);
  await page.locator('#logout_').waitFor({ state: 'visible' });
  await page.locator('#logout_').click();
  await page.locator('#usernameField').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);

});
