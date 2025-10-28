/*
 * Copyright (C) 2010-2025 Structr GmbH
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
  await context.delete(process.env.BASE_URL + '/structr/rest/SchemaMethod');

});

test('user-defined-functions', async ({ page }) => {

  test.setTimeout(240_000);

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

  // Code
  await page.locator('#code_').waitFor({state: 'visible'});
  await page.locator('#code_').click();

  // Wait for Code UI to load all components
  await page.waitForTimeout(1000);
  await page.screenshot({path: 'code.png'});

  // Create new user-defined function
  await page.getByText('User-defined functions').first().click();
  await page.locator('#methods-grid-container .dropdown-menu').first().waitFor({state: 'visible'});
  await page.locator('#methods-grid-container .dropdown-menu').first().click();
  await page.waitForTimeout(1000);
  await page.getByText('Add method').first().click();
  await page.waitForTimeout(500);
  await page.locator('#methods-grid-container .property-name').first().click();
  await page.keyboard.type('testMethod');
  await page.locator('.monaco-editor').nth(0).click();
  await page.keyboard.type('( log(\'This is testMethod.\'), (\'testMethod return value\') )');
  await page.waitForTimeout(1000);
  await page.getByText('Save all').first().click();
  await page.waitForTimeout(1000);
  await page.screenshot({path: 'code_create-user-defined-function.png'});

  // Run function
  await page.locator('[data-id="globals"] .jstree-ocl').first().click();
  await page.waitForTimeout(500);
  await page.getByText('testMethod()', { exact: false }).first().click();
  await page.screenshot({path: 'code_run-function.png'});
  await page.waitForTimeout(500);
  await page.getByText('Open run dialog').first().click();
  await page.waitForTimeout(500);
  //await page.locator('#run-method').click();
  await page.getByRole('button', {name: 'Run', exact: true}).click();
  //await page.getByText('Run', { exact: false }).first().click();
  await page.waitForTimeout(1000);
  await expect(page.getByText('"result": "testMethod return value"')).toBeVisible();
  await page.screenshot({path: 'code_run-function-result.png'});
  await page.waitForTimeout(1000);
  await page.getByRole('button', {name: 'Close', exact: true}).click();

  // Add Milestone and Project to OpenAPI
  await page.locator('[data-id="root"] .jstree-ocl').first().click();
  await page.getByRole('link', {name: 'Custom', exact: true}).waitFor({state: 'visible'});
  await page.locator('[data-id="custom"] .jstree-ocl').first().click();
  await page.getByRole('link', {name: 'Project', exact: true}).waitFor({state: 'visible'});
  await page.getByRole('link', {name: 'Project', exact: true}).click();
  await page.waitForTimeout(500);
  await page.getByRole('checkbox', {name: 'Include in OpenAPI output', exact: false}).click();
  await page.locator('#openapi-options > div > div').filter({ hasText: 'Tags'}).getByRole('combobox').click();
  await page.keyboard.type('default');
  await page.keyboard.press('Enter');
  await page.waitForTimeout(500);
  await page.getByRole('button', {name: 'Save', exact: false}).click();
  await page.waitForTimeout(1000);
  await page.screenshot({path: 'code_project-to-openapi.png'});

  // Logout
  await page.locator('.submenu-trigger').hover();
  await page.waitForTimeout(500);
  await page.locator('#logout_').waitFor({ state: 'visible' });
  await page.locator('#logout_').click();
  await page.locator('#usernameField').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);

});
