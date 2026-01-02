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
  await context.delete(process.env.BASE_URL + '/structr/rest/FlowNode');

});

test('flows', async ({ page }) => {

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

  // Flows
  await page.locator('.submenu-trigger').hover();
  await page.locator('#flows_').waitFor({state: 'visible'});
  await page.locator('#flows_').click();

  // Wait for Code UI to load all components
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/flows.png' });

  // Create new flow
  await page.getByPlaceholder('Enter flow name').click();
  await page.keyboard.type('testFlow');
  await page.getByRole('button', {name: 'Create'}).click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/flows_created-flow.png' });

  // Add TypeQuery node
  await page.getByText('testFlow').click();
  await page.locator('#nodeEditor').click({ button: 'right'});
  await page.getByText('Data Nodes').waitFor({state: 'visible'});
  await page.getByText('Data Nodes').hover();
  await page.waitForTimeout(100);
  await page.getByText('DataSource', {exact: true}).hover();
  await page.waitForTimeout(100);
  await page.getByText('TypeQuery', {exact: true}).hover();
  await page.waitForTimeout(100);
  await page.getByText('TypeQuery', {exact: true}).click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/flows_added-type-query.png' });

  // Select 'Project' as type and sort by name
  await page.locator('#dataType select').selectOption({ label: 'Project' });
  await page.getByText('Sort').click();
  await page.waitForTimeout(500);
  await page.locator('select.query-key-select').selectOption({ label: 'name' });

  // Create Return node and connect TypeQuery
  await page.locator('#nodeEditor').click({ button: 'right'});
  await page.waitForTimeout(100);
  await page.getByText('Action Nodes').waitFor({state: 'visible'});
  await page.getByText('Action Nodes').hover();
  await page.waitForTimeout(100);
  await page.getByText('Action', {exact: true}).waitFor({state: 'visible'});
  await page.getByText('Action', {exact: true}).hover();
  await page.waitForTimeout(100);
  await page.getByText('Return', {exact: true}).hover();
  await page.waitForTimeout(100);
  await page.getByText('Return', {exact: true}).click();
  await page.screenshot({ path: 'screenshots/flows_created-return-node.png' });

  // Arrange nodes
  await page.getByText('Return').hover();
  await page.mouse.down();
  await page.mouse.move(1100, 200);
  await page.mouse.up();
  await page.waitForTimeout(500);

  await page.getByText('TypeQuery').hover();
  await page.mouse.down();
  await page.mouse.move(580, 300);
  await page.mouse.up();
  await page.waitForTimeout(500);
  await page.screenshot({ path: 'screenshots/flows_arranged-nodes.png' });

  await page.locator('.node.typequery .socket.output.dataTarget').hover();
  await page.mouse.down();
  await page.locator('.node.return .socket.input.dataSource').hover();
  await page.mouse.up();
  await page.waitForTimeout(500);
  await page.screenshot({ path: 'screenshots/flows_connected-nodes.png' });

  await page.getByRole('button', { name: 'Run'}).click();
  await page.waitForTimeout(1000);

  await page.getByText('"result": [').waitFor({state: 'visible', timeout: 120000});
  await page.screenshot({ path: 'screenshots/flows_run-flow.png' });
  await page.locator('#executionResult .close').click();

  // Logout
  await page.locator('.submenu-trigger').hover();
  await page.waitForTimeout(500);
  await page.locator('#logout_').waitFor({ state: 'visible' });
  await page.locator('#logout_').click();
  await page.locator('#usernameField').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);

});
