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

test('schema', async ({ page }) => {

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

  //await page.screenshot({ path: 'screenshots/login.png' });

  // Login with admin/admin
  await page.locator('#usernameField').fill('admin');
  await page.locator('#passwordField').fill('admin');
  await page.waitForTimeout(1000);
  await page.locator('#loginButton').click();
  await page.waitForTimeout(1000);

  // Schema
  await page.locator('#schema_').waitFor({ state: 'visible' });
  await page.locator('#schema_').click();

  // Wait for Schema UI to load all components
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/schema.png' });

  // Create new type 'Project'
  await page.getByRole('button', {name: 'Create Data Type'}).waitFor({ state: 'visible' });
  await page.getByRole('button', {name: 'Create Data Type'}).click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/schema_create-type_Project.png' });
  await page.getByPlaceholder('Type Name...').fill('Project');
  await page.getByRole('button', { name: ' Create ', exact: true }).click();
  await page.waitForTimeout(1000);

  // Add custom String property 'projectId'
  await page.getByText('Direct properties', { exact: true }).click();
  await page.getByRole('button', { name: 'Add direct property', exact: true }).click();
  await page.getByPlaceholder('JSON name').click();
  await page.getByPlaceholder('JSON name').fill('projectId');
  await page.locator('.unique').nth(0).check();
  await page.locator('.property-type').selectOption({ value: 'String'} );
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/schema_property-added_projectId.png' });
  await page.getByRole('button', { name: 'Save All', exact: true }).click();
  await page.waitForTimeout(1000);
  await page.getByRole('button', { name: 'Close', exact: true }).click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/schema_type-created_Project.png' });

  // Create new type 'Milestone'
  await page.locator('#create-type').waitFor({ state: 'visible' });
  await page.locator('.non-block-ui-wrapper').waitFor({ state: 'hidden', timeout: 30000 }).catch(() => {});
  await page.locator('#create-type').click({ timeout: 30000 });
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/schema_create-type_Milestone.png' });
  await page.getByPlaceholder('Type Name...').fill('Milestone');
  await page.getByRole('button', { name: ' Create ', exact: true }).click();
  await page.waitForTimeout(1000);

  // Add custom String property 'milestoneId', custom Date property 'dueDate' and custom Function property 'projectId'
  // The Function property is needed for the Importer test (009-importer.spec.js) later on.
  await page.getByText('Direct properties', { exact: true }).click();
  await page.getByRole('button', { name: 'Add direct property', exact: true }).click();
  await page.getByPlaceholder('JSON name').click();
  await page.getByPlaceholder('JSON name').fill('milestoneId');
  await page.locator('.unique').nth(0).check();
  await page.locator('.property-type').selectOption({ value: 'String'} );
  await page.waitForTimeout(500);
  await page.getByRole('button', { name: 'Add direct property', exact: true }).click();
  await page.getByPlaceholder('JSON name').nth(1).click();
  await page.getByPlaceholder('JSON name').nth(1).fill('dueDate');
  await page.locator('.property-type').nth(1).selectOption({ value: 'Date'} );
  await page.waitForTimeout(500);
  await page.getByRole('button', { name: 'Add direct property', exact: true }).click();
  await page.getByPlaceholder('JSON name').nth(2).click();
  await page.getByPlaceholder('JSON name').nth(2).fill('projectId');
  await page.locator('.property-type').nth(2).selectOption({ value: 'Function'} );
  await page.getByRole('button', { name: 'Save All', exact: true }).click();
  await page.waitForTimeout(1000);
  await page.getByRole('button', { name: 'Read' }).click();
  await page.waitForTimeout(500);
  await page.keyboard.type('this.project.projectId');
  await page.getByRole('button', { name: 'Save and Close' }).click();
  await page.waitForTimeout(500);
  await page.getByRole('button', { name: 'Write' }).click();
  await page.waitForTimeout(500);
  await page.keyboard.type('set(this, \'project\', first(find(\'Project\', \'projectId\', value)))');
  await page.getByRole('button', { name: 'Save and Close' }).click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: 'screenshots/schema_added-Milestone-properties.png' });
  await page.getByRole('button', { name: 'Close', exact: true }).click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/schema_type-created_Milestone.png' });

  // Reset layout
  //await page.evaluate('document.body.style.zoom="1.0"');
  //await page.getByRole('button', { name: ' Display ' }).click();
  //await page.getByText(' Reset Layout (apply Auto-Layouting) ').click();

  // Create relationship between 'Project' and 'Milestone'
  await page.locator('.schema.node[data-type="Project"] + div + div').hover();
  await page.mouse.down();
  await page.locator('.schema.node[data-type="Milestone"] + div').hover();
  await page.mouse.up();
  await page.waitForTimeout(3000);
  await page.getByPlaceholder('Relationship Name...').fill('PROJECT_HAS_MILESTONE');
  await page.waitForTimeout(1000);
  await page.locator('#source-multiplicity-selector').selectOption('1');
  await page.locator('#source-json-name').fill('project');
  await page.locator('#target-json-name').fill('milestones');
  await page.getByRole('button', { name: 'Create', exact: true }).click();
  await page.screenshot({ path: 'screenshots/schema_relationship-project-milestone-created.png' });
  await page.getByRole('button', { name: 'Close', exact: true }).click();

  // Create new type 'Task'
  await page.locator('#create-type').waitFor({ state: 'visible' });
  await page.locator('#create-type').click();
  await page.waitForTimeout(500);
  await page.getByPlaceholder('Type Name...').fill('Task');
  await page.getByRole('button', { name: ' Create ', exact: true }).click();
  await page.waitForTimeout(500);

  // Add custom String property 'taskId' and custom Function property 'projectId'
  // The Function property is needed for the Importer test (009-importer.spec.js) later on.
  await page.getByText('Direct properties', { exact: true }).click();
  await page.getByRole('button', { name: 'Add direct property', exact: true }).click();
  await page.getByPlaceholder('JSON name').click();
  await page.getByPlaceholder('JSON name').fill('taskId');
  await page.locator('.unique').nth(0).check();
  await page.locator('.property-type').selectOption({ value: 'String'} );
  await page.waitForTimeout(500);
  await page.getByRole('button', { name: 'Add direct property', exact: true }).click();
  await page.getByPlaceholder('JSON name').nth(1).click();
  await page.getByPlaceholder('JSON name').nth(1).fill('projectId');
  await page.locator('.property-type').nth(1).selectOption({ value: 'Function'} );
  await page.getByRole('button', { name: 'Save All', exact: true }).click();
  await page.waitForTimeout(1000);
  await page.getByRole('button', { name: 'Read' }).click();
  await page.waitForTimeout(500);
  await page.keyboard.type('this.project.projectId');
  await page.getByRole('button', { name: 'Save and Close' }).click();
  await page.waitForTimeout(500);
  await page.getByRole('button', { name: 'Write' }).click();
  await page.waitForTimeout(500);
  await page.keyboard.type('set(this, \'project\', first(find(\'Project\', \'projectId\', value)))');
  await page.getByRole('button', { name: 'Save and Close' }).click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: 'screenshots/schema_added-Task-properties.png' });
  await page.getByRole('button', { name: 'Close', exact: true }).click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/schema_type-created_Project.png' });

  // Create relationship between 'Project' and 'Task'
  await page.locator('.schema.node[data-type="Task"] + div + div').hover();
  await page.mouse.down();
  await page.locator('.schema.node[data-type="Project"] + div').hover();
  await page.mouse.up();
  await page.waitForTimeout(3000);
  await page.getByPlaceholder('Relationship Name...').fill('TASK_BELONGS_TO');
  await page.waitForTimeout(1000);
  await page.locator('#source-multiplicity-selector').selectOption('*');
  await page.locator('#target-multiplicity-selector').selectOption('1');
  await page.locator('#source-json-name').fill('tasks');
  await page.locator('#target-json-name').fill('project');
  await page.getByRole('button', { name: 'Create', exact: true }).click();
  await page.screenshot({ path: 'screenshots/schema_relationship-project-task-created.png' });
  await page.getByRole('button', { name: 'Close', exact: true }).click();

  // Logout
  await page.locator('.submenu-trigger').hover();
  await page.waitForTimeout(250);
  await page.locator('#logout_').waitFor({ state: 'visible' });
  await page.locator('#logout_').click();
  await page.locator('#usernameField').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);

});

