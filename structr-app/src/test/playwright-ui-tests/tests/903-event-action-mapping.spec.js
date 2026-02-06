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

  // Clear all data
  await context.delete(process.env.BASE_URL + '/structr/rest/Page');
  await context.delete(process.env.BASE_URL + '/structr/rest/ActionMapping');
  await context.delete(process.env.BASE_URL + '/structr/rest/ParameterMapping');
  await context.delete(process.env.BASE_URL + '/structr/rest/Project');

});

test('pages', async ({ page }) => {

  test.setTimeout(240_000);

  let wait = 100;

  //await page.setViewportSize({ width: 3840, height: 2160 });
  await page.goto(process.env.BASE_URL + '/structr/');
  //await page.evaluate('document.body.style.zoom="2.0"');

  await expect(page).toHaveTitle(/Structr/);
  await expect(page.locator('#usernameField')).toBeVisible();
  await expect(page.locator('#passwordField')).toBeVisible();
  await expect(page.locator('#loginButton')).toBeVisible();

  await page.waitForTimeout(wait);

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
  await page.waitForTimeout(wait);

  // Open import dialog and create a new page
  await page.locator('#pages-actions .dropdown-select').click();
  await page.locator('#create_page').click();
  await page.waitForTimeout(wait);
  await page.locator('#template-tiles .app-tile:nth-child(4)').click();
  await page.waitForTimeout(2000);

  await page.locator('#pagesTree .node-container:nth-child(1)').waitFor({ state: 'visible' });
  await page.locator('#pagesTree .node-container:nth-child(1)').click();
  await page.getByRole('link', { name: 'General' }).click();
  await page.waitForTimeout(wait);
  await page.locator('#name-input').fill('projects');
  await page.locator('#pagesTree').click();
  await page.waitForTimeout(wait);

  // resize pages tree flyout
  await page.locator('.column-resizer').first().hover();
  await page.mouse.down();
  await page.waitForTimeout(wait);
  await page.mouse.move(-200, 0);
  await page.waitForTimeout(wait);
  await page.mouse.up();

  await page.locator('#pagesTree .node-container:nth-child(1)').click({ button: 'right' });
  await page.getByText('Expand / Collapse').waitFor({ state: 'visible' });
  await page.getByText('Expand / Collapse').hover();
  await page.waitForTimeout(wait);
  await page.getByText('Expand subtree recursively').waitFor({ state: 'visible' });
  await page.getByText('Expand subtree recursively').click();
  await page.waitForTimeout(wait);

  // insert frontend.js
  await page.locator('#pagesTree').getByText('head').click({ button: 'right' });
  await page.getByText('Suggested HTML element').waitFor({ state: 'visible' });
  await page.getByText('Suggested HTML element').hover();
  await page.getByRole('listitem').filter({ hasText: /^script$/ }).waitFor({ state: 'visible' });
  await page.getByRole('listitem').filter({ hasText: /^script$/ }).click();
  await page.waitForTimeout(wait);
  await page.locator('#pagesTree').getByText('script').click();
  await page.waitForTimeout(wait);
  await page.getByRole('link', { name: 'HTML' }).click();
  await page.waitForTimeout(wait);
  await page.locator('input[name="_html_type"]').fill('module');
  await page.locator('input[name="_html_src"]').fill('/structr/js/frontend/frontend.js');

  // remove Initial body text content
  await page.locator('#pagesTree').getByText('Initial body text').click({ button: 'right' });
  await page.getByText('Remove Node').waitFor({ state: 'visible' });
  await page.getByText('Remove Node').click();
  await page.waitForTimeout(wait);

  // insert form element
  await page.locator('#pagesTree').getByText('div').click({ button: 'right' });
  await page.getByText('Insert HTML element').waitFor({ state: 'visible' });
  await page.getByText('Insert HTML element').hover();
  await page.getByText('e-f').first().waitFor({ state: 'visible' });
  await page.getByText('e-f').first().hover();
  await page.getByText('form').first().waitFor({ state: 'visible' });
  await page.getByText('form').first().click();
  await page.waitForTimeout(wait);

  // insert div element for projects
  await page.locator('#pagesTree').getByText('div').click({ button: 'right' });
  await page.getByText('Insert div element').first().waitFor({ state: 'visible' });
  await page.getByText('Insert div element').first().click();
  await page.waitForTimeout(wait);

  // insert input element
  await page.locator('#pagesTree').getByText('form').click({ button: 'right' });
  await page.getByText('Suggested HTML element').first().waitFor({ state: 'visible' });
  await page.getByText('Suggested HTML element').first().hover();
  await page.getByRole('listitem').filter({ hasText: /^input$/ }).waitFor({ state: 'visible' });
  await page.getByRole('listitem').filter({ hasText: /^input$/ }).click();
  await page.waitForTimeout(wait);
  await page.locator('span').filter({ hasText: /^input$/ }).click();
  await page.waitForTimeout(wait);
  await page.getByRole('link', { name: 'HTML' }).click();
  await page.locator('input[name="_html_type"]').fill('text');
  await page.locator('input[name="_html_name"]').fill('name');
  await page.waitForTimeout(wait);

  // insert button element
  await page.locator('#pagesTree').getByText('form').click({ button: 'right' });
  await page.getByText('Suggested HTML element').first().waitFor({ state: 'visible' });
  await page.getByText('Suggested HTML element').first().hover();
  await page.getByRole('listitem').filter({ hasText: /^button$/ }).waitFor({ state: 'visible' });
  await page.getByRole('listitem').filter({ hasText: /^button$/ }).click();
  await page.waitForTimeout(wait);
  await page.locator('#pagesTree').getByText('Initial text for button').click();
  await page.waitForTimeout(wait);
  await page.keyboard.down('Control');
  await page.keyboard.press('A');
  await page.keyboard.up('Control');
  await page.keyboard.type('Create Project');
  await page.waitForTimeout(200);
  await page.getByRole('button', { name: 'Save' }).click();
  await page.waitForTimeout(wait);

  // second div, add
  await page.locator('span').filter({ hasText: 'div' }).nth(1).click({ button: 'right' });
  await page.getByText('Insert content element').first().waitFor({ state: 'visible' });
  await page.getByText('Insert content element').first().hover();
  await page.getByRole('listitem').filter({ hasText: /^#content$/ }).waitFor({ state: 'visible' });
  await page.getByRole('listitem').filter({ hasText: /^#content$/ }).click();
  await page.waitForTimeout(wait);
  await page.locator('#pagesTree').getByText('#text').click();
  await page.waitForTimeout(wait);
  await page.keyboard.down('Control');
  await page.keyboard.press('A');
  await page.keyboard.up('Control');
  await page.keyboard.type('${project.name}');
  await page.waitForTimeout(200);
  await page.getByRole('button', { name: 'Save' }).click();
  await page.waitForTimeout(wait);

  // build repeater config for projects div
  await page.locator('span').filter({ hasText: 'div' }).nth(1).click();
  await page.waitForTimeout(wait);
  await page.getByRole('link', { name: 'Repeater' }).click();
  await page.waitForTimeout(wait);
  await page.getByText('Function Query').click();
  await page.waitForTimeout(wait);
  await page.locator('.monaco-editor').nth(0).click();
  await page.keyboard.type('find(\'Project\')');
  await page.waitForTimeout(wait);
  await page.locator('.save-repeater-query').click();
  await page.locator('.repeater-datakey').fill('project');
  await page.waitForTimeout(wait);
  await page.locator('.save-repeater-datakey').click();

  // build event action mapping for form
  await page.locator('span').filter({ hasText: 'form' }).click();
  await page.waitForTimeout(wait);
  await page.getByRole('link', { name: 'HTML' }).click();
  await page.waitForTimeout(wait);
  await page.locator('input[name="_html_method"]').fill('post');
  await page.waitForTimeout(wait);
  await page.getByRole('link', { name: 'General' }).click();
  await page.locator('input[name="_html_id"]').fill('create-project-form');
  await page.waitForTimeout(wait);
  await page.getByRole('link', { name: 'Events' }).click();
  await page.waitForTimeout(wait);
  await page.getByRole('textbox', { name: 'Browser event (click, keydown' }).fill('submit');
  await page.waitForTimeout(wait);
  await page.keyboard.press('Tab');
  await page.waitForTimeout(wait);
  await page.locator('#action-select').selectOption('Create new object');
  await page.waitForTimeout(wait);
  await page.getByRole('textbox', { name: 'Custom type or script' }).fill('Project');
  await page.waitForTimeout(wait);
  await page.keyboard.press('Tab');
  await page.waitForTimeout(wait);
  await page.getByRole('img', { name: 'Add parameter', exact: true }).click();
  await page.waitForTimeout(wait);
  await page.locator('input.parameter-name-input').first().fill('name');
  await page.waitForTimeout(wait);
  await page.getByRole('combobox').nth(5).selectOption('User Input');
  await page.waitForTimeout(wait);
  await page.locator('span').filter({ hasText: /^input$/ }).hover();
  await page.waitForTimeout(wait);
  await page.mouse.down();
  await page.waitForTimeout(wait);
  await page.getByText('Drag and drop existing form').first().hover();
  await page.waitForTimeout(wait);
  await page.mouse.up();
  await page.getByLabel('Behaviour on success Define').selectOption('Navigate to a new page');
  await page.keyboard.press('Tab');
  await page.waitForTimeout(wait);
  await page.getByRole('textbox', { name: 'Success URL' }).fill('/project/{result.id}');
  await page.keyboard.press('Tab');
  await page.waitForTimeout(2000);
  await page.screenshot({ path: 'screenshots/pages_create-form_event-action-mapping-configuration.png' });

  await page.reload();
  await page.waitForTimeout(5000);

  // click on a different element to disable highlighting for the element we want to screenshot
  await page.locator('span').filter({ hasText: 'body' }).click();
  await page.waitForTimeout(wait);
  await page.locator('span').filter({ hasText: 'script' }).hover();
  await page.waitForTimeout(2000);

  // take a screenshot of the form element
  await page.locator('div.node:has(b[title="form"])').nth(4).screenshot({ path: 'screenshots/pages_create-form-element.png' });


  // create a second page for editing
  await page.locator('#pages-actions .dropdown-select').click();
  await page.locator('#create_page').click();
  await page.waitForTimeout(wait);
  await page.locator('#template-tiles .app-tile:nth-child(4)').click();
  await page.waitForTimeout(2000);

  await page.getByText('New Page').click();
  await page.waitForTimeout(wait);
  await page.getByRole('link', { name: 'General' }).click();
  await page.waitForTimeout(wait);
  await page.locator('#name-input').fill('project');
  await page.locator('#pagesTree').click();
  await page.waitForTimeout(wait);

  // collapse previous page
  await page.locator('i').first().click();

  // expand second page
  await page.locator('#pagesTree .node-container:nth-child(2)').click({ button: 'right' });
  await page.getByText('Expand / Collapse').waitFor({ state: 'visible' });
  await page.getByText('Expand / Collapse').hover();
  await page.waitForTimeout(wait);
  await page.getByText('Expand subtree recursively').waitFor({ state: 'visible' });
  await page.getByText('Expand subtree recursively').click();
  await page.waitForTimeout(wait);

    // insert frontend.js
    await page.locator('#pagesTree').getByText('head').click({ button: 'right' });
    await page.getByText('Suggested HTML element').waitFor({ state: 'visible' });
    await page.getByText('Suggested HTML element').hover();
    await page.getByRole('listitem').filter({ hasText: /^script$/ }).waitFor({ state: 'visible' });
    await page.getByRole('listitem').filter({ hasText: /^script$/ }).click();
    await page.waitForTimeout(wait);
    await page.locator('#pagesTree').getByText('script').click();
    await page.waitForTimeout(wait);
    await page.getByRole('link', { name: 'HTML' }).click();
    await page.waitForTimeout(wait);
    await page.locator('input[name="_html_type"]').fill('module');
    await page.locator('input[name="_html_src"]').fill('/structr/js/frontend/frontend.js');

  // remove Initial body text content
  await page.locator('#pagesTree').getByText('Initial body text').click({ button: 'right' });
  await page.getByText('Remove Node').waitFor({ state: 'visible' });
  await page.getByText('Remove Node').click();
  await page.waitForTimeout(wait);

  // change title
  await page.locator('span').filter({ hasText: '${capitalize(page.name)}' }).nth(2).click();
    await page.waitForTimeout(wait);
    await page.keyboard.down('Control');
    await page.keyboard.press('A');
    await page.keyboard.up('Control');
    await page.keyboard.type('Edit Project "${current.name}"');
    await page.waitForTimeout(200);
    await page.getByRole('button', { name: 'Save' }).click();
    await page.waitForTimeout(wait);


    // insert form element
    await page.locator('#pagesTree').getByText('div').click({ button: 'right' });
    await page.getByText('Insert HTML element').waitFor({ state: 'visible' });
    await page.getByText('Insert HTML element').hover();
    await page.getByText('e-f').first().waitFor({ state: 'visible' });
    await page.getByText('e-f').first().hover();
    await page.getByText('form').first().waitFor({ state: 'visible' });
    await page.getByText('form').first().click();
    await page.waitForTimeout(wait);

    // insert input element
    await page.locator('#pagesTree').getByText('form').click({ button: 'right' });
    await page.getByText('Suggested HTML element').first().waitFor({ state: 'visible' });
    await page.getByText('Suggested HTML element').first().hover();
    await page.getByRole('listitem').filter({ hasText: /^input$/ }).waitFor({ state: 'visible' });
    await page.getByRole('listitem').filter({ hasText: /^input$/ }).click();
    await page.waitForTimeout(wait);
    await page.locator('span').filter({ hasText: /^input$/ }).click();
    await page.waitForTimeout(wait);
    await page.getByRole('link', { name: 'HTML' }).click();
    await page.locator('input[name="_html_type"]').fill('text');
    await page.locator('input[name="_html_name"]').fill('name');
    await page.locator('input[name="_html_value"]').fill('${current.name}');
    await page.waitForTimeout(wait);

    // insert input element
    await page.locator('#pagesTree').getByText('form').click({ button: 'right' });
    await page.getByText('Suggested HTML element').first().waitFor({ state: 'visible' });
    await page.getByText('Suggested HTML element').first().hover();
    await page.getByRole('listitem').filter({ hasText: /^input$/ }).waitFor({ state: 'visible' });
    await page.getByRole('listitem').filter({ hasText: /^input$/ }).click();
    await page.waitForTimeout(wait);
    await page.locator('span').filter({ hasText: /^input$/ }).nth(1).click();
    await page.waitForTimeout(wait);
    await page.getByRole('link', { name: 'HTML' }).click();
    await page.locator('input[name="_html_type"]').fill('text');
    await page.locator('input[name="_html_name"]').fill('description');
    await page.locator('input[name="_html_value"]').fill('${current.description}');
    await page.waitForTimeout(wait);

    // insert input element
    await page.locator('#pagesTree').getByText('form').click({ button: 'right' });
    await page.getByText('Suggested HTML element').first().waitFor({ state: 'visible' });
    await page.getByText('Suggested HTML element').first().hover();
    await page.getByRole('listitem').filter({ hasText: /^input$/ }).waitFor({ state: 'visible' });
    await page.getByRole('listitem').filter({ hasText: /^input$/ }).click();
    await page.waitForTimeout(wait);
    await page.locator('span').filter({ hasText: /^input$/ }).nth(2).click();
    await page.waitForTimeout(wait);
    await page.getByRole('link', { name: 'HTML' }).click();
    await page.locator('input[name="_html_type"]').fill('date');
    await page.locator('input[name="_html_name"]').fill('dueDate');
    await page.locator('input[name="_html_value"]').fill('${dateFormat(current.dueDate, "yyyy-MM-dd")}');

    // click on different element before screenshotting to avoid ugly spellcheck lines in screenshot (WTF?)
    await page.locator('input[name="_html_autofocus"]').click();
    await page.waitForTimeout(wait);

    await page.waitForTimeout(5000);
    await page.screenshot({ path: 'screenshots/pages_edit-form_input-configuration.png' });

    // insert button element
    await page.locator('#pagesTree').getByText('form').click({ button: 'right' });
    await page.getByText('Suggested HTML element').first().waitFor({ state: 'visible' });
    await page.getByText('Suggested HTML element').first().hover();
    await page.getByRole('listitem').filter({ hasText: /^button$/ }).waitFor({ state: 'visible' });
    await page.getByRole('listitem').filter({ hasText: /^button$/ }).click();
    await page.waitForTimeout(wait);
    await page.locator('#pagesTree').getByText('Initial text for button').click();
    await page.waitForTimeout(wait);
    await page.keyboard.down('Control');
    await page.keyboard.press('A');
    await page.keyboard.up('Control');
    await page.keyboard.type('Save Project');
    await page.waitForTimeout(200);
    await page.getByRole('button', { name: 'Save' }).click();
    await page.waitForTimeout(wait);


    // build event action mapping for form
    await page.locator('span').filter({ hasText: 'form' }).click();
    await page.waitForTimeout(wait);
    await page.getByRole('link', { name: 'HTML' }).click();
    await page.waitForTimeout(wait);
    await page.locator('input[name="_html_method"]').fill('post');
    await page.waitForTimeout(wait);
    await page.getByRole('link', { name: 'General' }).click();
    await page.locator('input[name="_html_id"]').fill('save-project-form');
    await page.waitForTimeout(wait);
    await page.getByRole('link', { name: 'Events' }).click();
    await page.waitForTimeout(wait);
    await page.getByRole('textbox', { name: 'Browser event (click, keydown' }).fill('submit');
    await page.keyboard.press('Tab');
    await page.waitForTimeout(wait);
    await page.locator('#action-select').selectOption('Update object');
    await page.waitForTimeout(wait);
    await page.getByRole('textbox', { name: 'Custom type or script' }).fill('Project');
    await page.waitForTimeout(wait);
    await page.keyboard.press('Tab');
    await page.waitForTimeout(wait);
    await page.locator('#id-expression-input').fill('${current.id}');
    await page.waitForTimeout(wait);
    await page.keyboard.press('Tab');
    await page.waitForTimeout(wait);
    await page.getByRole('img', { name: 'Add parameter', exact: true }).click();
    await page.waitForTimeout(wait);
    await page.getByRole('img', { name: 'Add parameter', exact: true }).click();
    await page.waitForTimeout(wait);
    await page.getByRole('img', { name: 'Add parameter', exact: true }).click();
    await page.waitForTimeout(wait);
    await page.getByRole('textbox', { name: 'Name' }).nth(2).fill('name');
    await page.waitForTimeout(wait);
    await page.getByRole('combobox').nth(5).selectOption('User Input');
    await page.waitForTimeout(wait);
    await page.getByRole('textbox', { name: 'Name' }).nth(3).fill('description');
    await page.waitForTimeout(wait);
    await page.getByRole('combobox').nth(6).selectOption('User Input');
    await page.waitForTimeout(wait);
    await page.getByRole('textbox', { name: 'Name' }).nth(4).fill('dueDate');
    await page.waitForTimeout(wait);
    await page.getByRole('combobox').nth(7).selectOption('User Input');
    await page.waitForTimeout(wait);

    // drag input1 to dropzone1
    await page.locator('span').filter({ hasText: 'input' }).first().hover();
    await page.waitForTimeout(wait);
    await page.mouse.down();
    await page.waitForTimeout(wait);
    await page.getByText('Drag and drop existing form').first().hover();
    await page.waitForTimeout(wait);
    await page.mouse.up();

    // drag input2 to dropzone2
    await page.locator('span').filter({ hasText: 'input' }).nth(1).hover();
    await page.waitForTimeout(wait);
    await page.mouse.down();
    await page.waitForTimeout(wait);
    await page.getByText('Drag and drop existing form').nth(1).hover();
    await page.waitForTimeout(wait);
    await page.mouse.up();

    // drag input3 to dropzone3
    await page.locator('span').filter({ hasText: 'input' }).nth(2).hover();
    await page.waitForTimeout(wait);
    await page.mouse.down();
    await page.waitForTimeout(wait);
    await page.getByText('Drag and drop existing form').nth(2).hover();
    await page.waitForTimeout(wait);
    await page.mouse.up();

    await page.getByLabel('Behaviour on success Define').selectOption('Reload the current page');
    await page.keyboard.press('Tab');
    await page.waitForTimeout(5000);
    await page.screenshot({ path: 'screenshots/pages_edit-form_event-action-mapping-configuration.png' });
    await page.waitForTimeout(2000);

    // reload to un-select the form element
    await page.reload();
    await page.waitForTimeout(5000);

    // click on a different element to disable highlighting for the element we want to screenshot
    await page.locator('span').filter({ hasText: 'form#save-project-form' }).click();
    await page.waitForTimeout(wait);
    await page.locator('span').filter({ hasText: 'body' }).click();
    await page.waitForTimeout(wait);
    await page.locator('span').filter({ hasText: 'head' }).hover();
    await page.waitForTimeout(2000);

  // take a screenshot of the form element
  await page.locator('div.node:has(b[title="form"])').nth(4).screenshot({ path: 'screenshots/pages_edit-form-element.png' });

  await page.waitForTimeout(1000);
  await page.goto(process.env.BASE_URL + '/projects');
  await page.waitForTimeout(1000);

  await page.locator('input[name="name"]').fill('Project #1');
  await page.waitForTimeout(wait);
  await page.locator('button').click();
  await page.waitForTimeout(1000);
  await page.locator('h1').innerText('Edit Project "Project #1');

  await page.waitForTimeout(1000);
  await page.goto(process.env.BASE_URL + '/projects');
  await page.waitForTimeout(1000);

    //await page.setViewportSize({ width: 3840, height: 2160 });
    await page.goto(process.env.BASE_URL + '/structr/');
    //await page.evaluate('document.body.style.zoom="2.0"');

    await expect(page).toHaveTitle(/Structr/);

  // Logout
  await page.locator('.submenu-trigger').hover();
  await page.waitForTimeout(500);
  await page.locator('#logout_').waitFor({ state: 'visible' });
  await page.locator('#logout_').click();
  await page.locator('#usernameField').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);

});

