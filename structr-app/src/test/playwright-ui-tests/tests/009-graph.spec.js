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

test('graph', async ({ page }) => {

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

  // Pages
  await page.locator('#pages_').waitFor({ state: 'visible' });
  await page.locator('#pages_').click();

  // Wait for Pages UI to load all components
  await page.waitForTimeout(1000);
  //await page.screenshot({ path: 'screenshots/pages.png' });

  // Create new page
  await page.locator('#pages-actions .dropdown-select').click();
  await page.locator('#create_page').waitFor({ state: 'visible' });
  await page.locator('#create_page').click();
  await page.waitForTimeout(1000);
  //await page.screenshot({ path: 'screenshots/pages_create-page.png' });
  await page.locator('#template-tiles .app-tile:nth-child(2)').click();
  await page.waitForTimeout(2000);

  await page.locator('.submenu-trigger').hover();
  await page.locator('#graph_').waitFor({state: 'visible'});
  await page.locator('#graph_').click();

  // Wait for Graph UI to load all components
  await page.waitForTimeout(500);
  await page.screenshot({ path: 'screenshots/graph.png' });

  // Enter Cypher query to show all nodes and relationships
  await page.getByPlaceholder('Cypher query').click();
  await page.keyboard.type('MATCH (n)-[r]-(m) RETURN n,r,m');
  await page.locator('#exec-cypher').click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/graph_show-nodes.png' });

  const canvasNode = await page.evaluate('_Graph.graphBrowser.getNodes()[0]');
  const x = parseFloat(canvasNode['renderer1:x']);
  const y = parseFloat(canvasNode['renderer1:y']) + 128;

  // Hover over first node
  await page.mouse.move(x, y);

  await page.waitForTimeout(500);
  await page.screenshot({ path: 'screenshots/graph_node-hover.png' });

  // Click on first node to open properties dialog
  await page.mouse.click(x,y);
  await page.waitForTimeout(500);
  await page.screenshot({ path: 'screenshots/graph_node-properties-opened.png' });

  await page.getByRole('button', {name: 'Close', exact: true}).click();

  // Logout
  await page.locator('.submenu-trigger').hover();
  await page.waitForTimeout(500);
  await page.locator('#logout_').waitFor({ state: 'visible' });
  await page.locator('#logout_').click();
  await page.locator('#usernameField').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);

});
