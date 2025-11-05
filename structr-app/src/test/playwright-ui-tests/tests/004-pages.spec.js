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

  // Clear all pages
  await context.delete(process.env.BASE_URL + '/structr/rest/Page');

});

test('pages', async ({ page }) => {

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

  // Pages
  await page.locator('#pages_').waitFor({ state: 'visible' });
  await page.locator('#pages_').click();

  // Wait for Pages UI to load all components
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'pages.png' });

  // Create new page
  await page.locator('#pages-actions .dropdown-select').click();
  await page.locator('#create_page').waitFor({ state: 'visible' });
  await page.locator('#create_page').click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'pages_create-page.png' });
  await page.locator('#template-tiles .app-tile:nth-child(2)').click();
  await page.waitForTimeout(2000);

  await page.locator('#pagesTree .node-container:nth-child(1)').waitFor({ state: 'visible' });
  await page.locator('#pagesTree .node-container:nth-child(1)').click();
  await page.getByRole('link', { name: 'General' }).click();
  await page.waitForTimeout(1000);
  await page.locator('#name-input').fill('projects');
  await page.locator('#pagesTree').click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'pages_page-created.png' });

  await page.locator('#pagesTree .node-container:nth-child(1)').click({ button: 'right' });
  await page.getByText('Expand / Collapse').waitFor({ state: 'visible' });
  await page.getByText('Expand / Collapse').hover();
  await page.waitForTimeout(1000);
  await page.getByText('Expand subtree recursively').waitFor({ state: 'visible' });
  await page.getByText('Expand subtree recursively').click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'pages_page-expanded.png' });

  await page.getByRole('link', { name: 'Preview' }).click();
  //await page.locator('.previewBox').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'pages_page-preview.png' });

  // Drag 'Simple Table' widget onto 'Main Container'
  await page.locator('#widgetsTab').click();
  await page.waitForTimeout(1000);
  await page.getByText('Simple Table', { exact: true }).hover();
  await page.mouse.down();
  await page.locator('#pagesTree').getByText('Main Container', { exact: true }).hover();
  await page.mouse.up();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'pages_simple-table-added.png' });
  await page.locator('#widgetsTab').click();

  // Modify table to display project objects
  const preview = page.frameLocator('.previewBox iframe');
  await preview.getByText('Title', { exact: true }).click();
  await preview.getByText('Title', { exact: true }).fill('${localize("milestones", "table-header")}');
  await page.locator('#pagesTree').click();
  await page.waitForTimeout(1000);

  await preview.getByText('Firstname Lastname', { exact: true }).click();
  await preview.getByText('Firstname Lastname', { exact: true }).fill('${project.name}');
  await page.locator('#pagesTree').click();
  await page.waitForTimeout(1000);

  await preview.getByText('Example Job Title', { exact: true }).click();
  await preview.getByText('Example Job Title', { exact: true }).fill('${join(extract(project.milestones, "name"), ", ")}');
  await page.locator('#pagesTree').click();
  await page.waitForTimeout(1000);

  await preview.getByText('firstname.lastname@example.com', { exact: true }).click({ button: 'right' });
  await page.waitForTimeout(1000);
  await page.locator('#context-menu-dialog .context_menu_icon').click();
  await page.waitForTimeout(1000);
  await page.locator('#context-menu-dialog').getByText('Remove Node', { exact: true }).click();
  await page.waitForTimeout(1000);

  await preview.getByText('Email', { exact: true }).click({ button: 'right' });
  await page.waitForTimeout(1000);
  await page.locator('#context-menu-dialog .context_menu_icon').click();
  await page.waitForTimeout(1000);
  await page.locator('#context-menu-dialog').getByText('Remove Node', { exact: true }).click();
  await page.waitForTimeout(1000);

  await preview.getByText('Example Role', { exact: true }).click({ button: 'right' });
  await page.waitForTimeout(1000);
  await page.locator('#context-menu-dialog .context_menu_icon').click();
  await page.waitForTimeout(1000);
  await page.locator('#context-menu-dialog').getByText('Remove Node', { exact: true }).click();
  await page.waitForTimeout(1000);

  await preview.getByText('Role', { exact: true }).click({ button: 'right' });
  await page.waitForTimeout(1000);
  await page.locator('#context-menu-dialog .context_menu_icon').click();
  await page.waitForTimeout(1000);
  await page.locator('#context-menu-dialog').getByText('Remove Node', { exact: true }).click();
  await page.waitForTimeout(1000);

  await page.screenshot({ path: 'pages_output-expression.png' });

  // Add repeater to display the projects
  await page.locator('#pagesTree div.node:has(> div > span > b[title="tbody"]) div.node b[title="tr"]').click();
  await page.waitForTimeout(1000);
  await page.getByRole('link', { name: 'Repeater' }).click();
  await page.waitForTimeout(1000);
  await page.getByText('Function Query').click();
  await page.waitForTimeout(1000);
  await page.locator('.monaco-editor').nth(0).click();
  await page.keyboard.type('find(\'Project\')');
  await page.waitForTimeout(1000);
  await page.locator('.save-repeater-query').click();
  await page.locator('.repeater-datakey').fill('project');
  await page.waitForTimeout(1000);
  await page.locator('.save-repeater-datakey').click();
  await page.getByRole('link', { name: 'Preview' }).click();
  await page.waitForTimeout(1000);

  await page.screenshot({ path: 'pages_repeater.png' });

  // Logout
  await page.locator('.submenu-trigger').hover();
  await page.waitForTimeout(500);
  await page.locator('#logout_').waitFor({ state: 'visible' });
  await page.locator('#logout_').click();
  await page.locator('#usernameField').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);

});

