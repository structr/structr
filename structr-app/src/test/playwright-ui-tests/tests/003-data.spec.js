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
import { test, expect, defineConfig } from '@playwright/test';
//import { setBrowserZoom, testWithZoomExtension as test } from "playwright-zoom";

test.beforeAll(async ({ playwright }) => {

  const context = await playwright.request.newContext({
    extraHTTPHeaders: {
      'Accept': 'application/json',
      'X-User': 'superadmin',
      'X-Password': process.env.SUPERUSER_PASSWORD,
    }
  });

  // Clear data
  await context.delete(process.env.BASE_URL + '/structr/rest/Project');
  await context.delete(process.env.BASE_URL + '/structr/rest/Milestone');

});

test('data', async ({ page }) => {

  test.setTimeout(240_000);

  //await page.setViewportSize({ width: 3840, height: 2160 });
  await page.goto(process.env.BASE_URL + '/structr/');
  //await page.evaluate('document.body.style.zoom="2.0"');
  //await setBrowserZoom(page, 200);

  await expect(page).toHaveTitle(/Structr/);
  await expect(page.locator('#usernameField')).toBeVisible();
  await expect(page.locator('#passwordField')).toBeVisible();
  await expect(page.locator('#loginButton')).toBeVisible();

  await page.waitForTimeout(1000);

  //await page.screenshot({ path: 'login.png' });

  // Login with admin/admin
  await page.locator('#usernameField').fill('admin');
  await page.locator('#passwordField').fill('admin');
  await page.waitForTimeout(1000);
  await page.locator('#loginButton').click();
  await page.waitForTimeout(1000);

  // Data
  await page.locator('#crud_').waitFor({ state: 'visible' });
  await page.locator('#crud_').click();

  // Wait for Schema UI to load all components
  await page.waitForSelector('#crud-types [data-type="Project"]', { state: 'visible', timeout: 60000 });
  await page.screenshot({ path: 'data.png' });

  // Create test data of type 'Project'
  await page.locator('#crud-types [data-type="Project"]').click({ timeout: 30000 });
  await page.waitForTimeout(2000);
  for (let i = 0; i < 3; ++i) {
    await page.getByRole('button', { name: ' Create Project ', exact: true }).click();
    await page.waitForTimeout(1000);
  }

  // Set name
  await page.locator('.crud-table tbody tr:nth-child(1) td[data-key="name"]').click();
  await page.waitForTimeout(500);
  await page.locator('.crud-table tbody tr:nth-child(1) [data-key="name"] textarea').waitFor({ state: 'visible' });
  await page.locator('.crud-table tbody tr:nth-child(1) [data-key="name"] textarea').fill('Project A');

  await page.locator('.crud-table tbody tr:nth-child(2) td[data-key="name"]').click();
  await page.waitForTimeout(500);
  await page.locator('.crud-table tbody tr:nth-child(2) [data-key="name"] textarea').waitFor({ state: 'visible' });
  await page.locator('.crud-table tbody tr:nth-child(2) [data-key="name"] textarea').fill('Project B');

  await page.locator('.crud-table tbody tr:nth-child(3) td[data-key="name"]').click();
  await page.waitForTimeout(500);
  await page.locator('.crud-table tbody tr:nth-child(3) [data-key="name"] textarea').waitFor({ state: 'visible' });
  await page.locator('.crud-table tbody tr:nth-child(3) [data-key="name"] textarea').fill('Project C');

  await page.locator('#query-info').click();
  await page.waitForTimeout(1000);

  // Create test data of type 'Milestone'
  await page.locator('#crud-types [data-type="Milestone"]').click();
  await page.waitForTimeout(1000);
  for (let i = 0; i < 9; ++i) {
    await page.getByRole('button', { name: ' Create Milestone ', exact: true }).click();
    await page.waitForTimeout(1000);
  }

  // Set name
  await page.locator('.crud-table tbody tr:nth-child(1) td[data-key="name"]').click();
  await page.waitForTimeout(1000);
  await page.locator('.crud-table tbody tr:nth-child(1) [data-key="name"] textarea').waitFor({ state: 'visible' });
  await page.locator('.crud-table tbody tr:nth-child(1) [data-key="name"] textarea').fill('Milestone A1');

  await page.locator('.crud-table tbody tr:nth-child(2) td[data-key="name"]').click();
  await page.waitForTimeout(1000);
  await page.locator('.crud-table tbody tr:nth-child(2) [data-key="name"] textarea').waitFor({ state: 'visible' });
  await page.locator('.crud-table tbody tr:nth-child(2) [data-key="name"] textarea').fill('Milestone A2');

  await page.locator('.crud-table tbody tr:nth-child(3) td[data-key="name"]').click();
  await page.waitForTimeout(1000);
  await page.locator('.crud-table tbody tr:nth-child(3) [data-key="name"] textarea').waitFor({ state: 'visible' });
  await page.locator('.crud-table tbody tr:nth-child(3) [data-key="name"] textarea').fill('Milestone A3');

  await page.locator('.crud-table tbody tr:nth-child(4) td[data-key="name"]').click();
  await page.waitForTimeout(1000);
  await page.locator('.crud-table tbody tr:nth-child(4) [data-key="name"] textarea').waitFor({ state: 'visible' });
  await page.locator('.crud-table tbody tr:nth-child(4) [data-key="name"] textarea').fill('Milestone B1');

  await page.locator('.crud-table tbody tr:nth-child(5) td[data-key="name"]').click();
  await page.waitForTimeout(1000);
  await page.locator('.crud-table tbody tr:nth-child(5) [data-key="name"] textarea').waitFor({ state: 'visible' });
  await page.locator('.crud-table tbody tr:nth-child(5) [data-key="name"] textarea').fill('Milestone B2');

  await page.locator('.crud-table tbody tr:nth-child(6) td[data-key="name"]').click();
  await page.waitForTimeout(1000);
  await page.locator('.crud-table tbody tr:nth-child(6) [data-key="name"] textarea').waitFor({ state: 'visible' });
  await page.locator('.crud-table tbody tr:nth-child(6) [data-key="name"] textarea').fill('Milestone B3');

  await page.locator('.crud-table tbody tr:nth-child(7) td[data-key="name"]').click();
  await page.waitForTimeout(1000);
  await page.locator('.crud-table tbody tr:nth-child(7) [data-key="name"] textarea').waitFor({ state: 'visible' });
  await page.locator('.crud-table tbody tr:nth-child(7) [data-key="name"] textarea').fill('Milestone C1');

  await page.locator('.crud-table tbody tr:nth-child(8) td[data-key="name"]').click();
  await page.waitForTimeout(1000);
  await page.locator('.crud-table tbody tr:nth-child(8) [data-key="name"] textarea').waitFor({ state: 'visible' });
  await page.locator('.crud-table tbody tr:nth-child(8) [data-key="name"] textarea').fill('Milestone C2');

  await page.locator('.crud-table tbody tr:nth-child(9) td[data-key="name"]').click();
  await page.waitForTimeout(1000);
  await page.locator('.crud-table tbody tr:nth-child(9) [data-key="name"] textarea').waitFor({ state: 'visible' });
  await page.locator('.crud-table tbody tr:nth-child(9) [data-key="name"] textarea').fill('Milestone C3');

  await page.locator('#query-info').click();
  await page.waitForTimeout(1000);

  await page.screenshot({ path: 'data_objects-created.png' });

  await page.locator('#crud-types [data-type="Project"]').click();
  await page.waitForTimeout(1000);

  // Link Milestones to Projects
  await page.locator('.crud-table tbody tr:nth-child(1) td[data-key="milestones"] svg').click();
  await page.getByText('Milestone A1').click();
  await page.waitForTimeout(1000);
  await page.getByText('Milestone A2').click();
  await page.waitForTimeout(1000);
  await page.getByText('Milestone A3').click();
  await page.waitForTimeout(1000);
  await page.getByRole('button', { name: 'Close', exact: true }).click();
  await page.waitForTimeout(1000);

  await page.locator('.crud-table tbody tr:nth-child(2) td[data-key="milestones"] svg').click();
  await page.getByText('Milestone B1').click();
  await page.waitForTimeout(1000);
  await page.getByText('Milestone B2').click();
  await page.waitForTimeout(1000);
  await page.getByText('Milestone B3').click();
  await page.waitForTimeout(1000);
  await page.getByRole('button', { name: 'Close', exact: true }).click();
  await page.waitForTimeout(1000);

  await page.locator('.crud-table tbody tr:nth-child(3) td[data-key="milestones"] svg').click();
  await page.getByText('Milestone C1').click();
  await page.waitForTimeout(1000);
  await page.getByText('Milestone C2').click();
  await page.waitForTimeout(1000);
  await page.getByText('Milestone C3').click();
  await page.waitForTimeout(1000);
  await page.getByRole('button', { name: 'Close', exact: true }).click();
  await page.waitForTimeout(1000);


  await page.screenshot({ path: 'data_objects-linked.png' });




  // Logout
  await page.locator('.submenu-trigger').hover();
  await page.waitForTimeout(500);
  await page.locator('#logout_').waitFor({ state: 'visible' });
  await page.locator('#logout_').click();
  await page.locator('#usernameField').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);

});

