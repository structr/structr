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
  await context.delete(process.env.BASE_URL + '/structr/rest/File');
  await context.delete(process.env.BASE_URL + '/structr/rest/Project');
  await context.delete(process.env.BASE_URL + '/structr/rest/Milestone');
  await context.delete(process.env.BASE_URL + '/structr/rest/Task');



  // Upload import files using multipart/form-data
  let csv = fs.readFileSync('1000-tasks.csv');
  await context.post(process.env.BASE_URL + '/structr/upload', {
    multipart: {
      file: {
        name: '1000-tasks.csv',
        mimeType: 'text/csv',
        buffer: csv
      }
    }
  });

  csv = fs.readFileSync('100-milestones.csv');
  await context.post(process.env.BASE_URL + '/structr/upload', {
    multipart: {
      file: {
        name: '100-milestones.csv',
        mimeType: 'text/csv',
        buffer: csv
      }
    }
  });

  csv = fs.readFileSync('10-projects.csv');
  await context.post(process.env.BASE_URL + '/structr/upload', {
    multipart: {
      file: {
        name: '10-projects.csv',
        mimeType: 'text/csv',
        buffer: csv
      }
    }
  });

});

test('importer', async ({ page }) => {

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

  // Importer
  await page.locator('.submenu-trigger').hover();
  await page.locator('#importer_').waitFor({state: 'visible'});
  await page.locator('#importer_').click();

  // Wait for Code UI to load all components
  await page.waitForTimeout(1000);
  await page.screenshot({path: 'importer.png'});

  // Create CSV document to import
  await page.locator('#files_').waitFor({state: 'visible'});
  await page.locator('#files_').click();
  await page.locator('#add-file-button').waitFor({ state: 'visible' });
  await page.locator('#add-file-button').click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: 'importer_created-file.png' });
  await page.getByText('New File').first().click();
  await page.waitForTimeout(500);
  await page.keyboard.type('import-file.csv');
  await page.keyboard.press('Enter');
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'importer_renamed-file.png' });
  await page.getByText('import-file.csv').first().click({button: 'right'});
  await page.getByText('General').first().waitFor({state: 'visible'});
  await page.getByText('General').first().click();
  await page.waitForTimeout(500);
  await page.keyboard.press('Tab');
  await page.keyboard.type('text/csv');
  await page.keyboard.press('Enter');
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'importer_set-content-type.png' });
  await page.getByRole('button', {name: 'Close'}).click();

  /*
  // Edit file and add CSV lines
  await page.getByText('import-file.csv').first().click({button: 'right'});
  await page.getByText('Edit File').first().waitFor({state: 'visible'});
  await page.getByText('Edit File').first().click();
  await page.waitForTimeout(500);
  await page.keyboard.type(csv);
  await page.waitForTimeout(1000);
  await page.getByRole('button', {name: 'Save and Close'}).click();
  await page.screenshot({ path: 'importer_typed-csv-text.png' });
*/

  await page.locator('#file-tree-container').getByText('structr_uploads').first().click();
  await page.getByText('10-projects.csv').first().click({button: 'right'});
  await page.waitForTimeout(500);
  await page.getByText('Import CSV').first().waitFor({state: 'visible'});
  await page.getByText('Import CSV').first().click();
  await page.waitForTimeout(500);
  await page.locator('select#target-type-select').selectOption({ label: 'Project' });
  await page.waitForTimeout(500);
  await page.getByRole('button', {name: 'Start import'}).click();
  await page.waitForTimeout(500);
  await page.getByRole('button', {name: 'Close'}).click();

  await page.getByText('100-milestones.csv').first().click({button: 'right'});
  await page.waitForTimeout(500);
  await page.getByText('Import CSV').first().waitFor({state: 'visible'});
  await page.getByText('Import CSV').first().click();
  await page.waitForTimeout(500);
  await page.locator('select#target-type-select').selectOption({ label: 'Milestone' });
  await page.waitForTimeout(500);
  await page.getByRole('button', {name: 'Start import'}).click();
  await page.getByRole('button', {name: 'Close'}).click();

  await page.getByText('1000-tasks.csv').first().click({button: 'right'});
  await page.waitForTimeout(500);
  await page.getByText('Import CSV').first().waitFor({state: 'visible'});
  await page.getByText('Import CSV').first().click();
  await page.waitForTimeout(500);
  await page.locator('select#target-type-select').selectOption({ label: 'Task' });
  await page.waitForTimeout(500);
  await page.getByRole('button', {name: 'Start import'}).click();
  await page.getByRole('button', {name: 'Close', exact: true}).click();
  await page.locator('#close-all-button').click();

  // Check import processes
  await page.locator('.submenu-trigger').hover();
  await page.locator('#importer_').waitFor({state: 'visible'});
  await page.locator('#importer_').click();
  await page.locator('#importer-jobs-table').click();
  await page.locator('#importer-jobs-table tbody tr').isVisible();
  await page.screenshot({ path: 'importer_check-import-process.png' });
  await page.locator('.close-message-button').click();

  // Logout
  await page.locator('.submenu-trigger').hover();
  await page.waitForTimeout(500);
  await page.locator('#logout_').waitFor({ state: 'visible' });
  await page.locator('#logout_').click();
  await page.locator('#usernameField').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);

});
