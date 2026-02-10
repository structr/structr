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
import {test} from '@playwright/test';
import {initialize} from "./helpers/init";
import {login, logout} from "./helpers/auth";

test.beforeAll(async ({playwright}) => {
    await initialize(playwright);
});

test('pages', async ({page}) => {

    await login(page);

    // Pages
    await page.locator('#pages_').waitFor({state: 'visible'});
    await page.locator('#pages_').click();

    // Wait for Pages UI to load all components
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/pages.png'});

    // Open import dialog and create a screenshot
    await page.locator('#pages-actions .dropdown-select').click();
    await page.locator('#create_page').waitFor({state: 'visible'});
    await page.locator('#import_page').click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/pages_import-page.png'});
    await page.getByRole('button', {name: 'Close'}).click();
    await page.waitForTimeout(200);

    // Create new page (dropdown stays open from previous action!)
    await page.locator('#create_page').click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/pages_create-page.png'});
    await page.locator('#template-tiles .app-tile:nth-child(2)').click();
    await page.waitForTimeout(2000);

    // Open Access Control dialog to create a screenshot
    await page.getByRole('img', {name: 'Access Control'}).first().click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/pages_access-control-dialog.png'});
    await page.getByRole('button', {name: 'Close'}).click();
    await page.waitForTimeout(1000);

    await page.locator('#pagesTree .node-container:nth-child(1)').waitFor({state: 'visible'});
    await page.locator('#pagesTree .node-container:nth-child(1)').click();
    await page.getByRole('link', {name: 'General'}).click();
    await page.waitForTimeout(1000);
    await page.locator('#name-input').fill('projects');
    await page.locator('#pagesTree').click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/pages_page-created.png'});

    await page.locator('#pagesTree .node-container:nth-child(1)').click({button: 'right'});
    await page.getByText('Expand / Collapse').waitFor({state: 'visible'});
    await page.getByText('Expand / Collapse').hover();
    await page.waitForTimeout(1000);
    await page.getByText('Expand subtree recursively').waitFor({state: 'visible'});
    await page.getByText('Expand subtree recursively').click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/pages_page-expanded.png'});

    // click through the different tabs and take a screenshot of each tab
    await page.getByRole('link', {name: 'Advanced'}).click();
    await page.waitForTimeout(200);
    await page.screenshot({path: 'screenshots/pages_page-details-advanced.png'});
    await page.getByRole('link', {name: 'Security'}).nth(1).click();
    await page.waitForTimeout(200);
    await page.screenshot({path: 'screenshots/pages_page-details-security.png'});
    await page.getByRole('link', {name: 'Active Elements'}).click();
    await page.waitForTimeout(200);
    await page.screenshot({path: 'screenshots/pages_page-details-active-elements.png'});
    await page.getByRole('link', {name: 'URL Routing'}).click();
    await page.waitForTimeout(200);
    await page.screenshot({path: 'screenshots/pages_page-details-url-routing.png'});

    await page.getByRole('link', {name: 'Preview'}).click();
    //await page.locator('.previewBox').waitFor({ state: 'visible' });
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/pages_page-preview.png'});

    // Drag 'Simple Table' widget onto 'Main Container'
    await page.locator('#widgetsTab').click();
    await page.waitForTimeout(1000);
    await page.getByText('Simple Table', {exact: true}).hover();
    await page.mouse.down();
    await page.locator('#pagesTree').getByText('Main Container', {exact: true}).hover();
    await page.mouse.up();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/pages_simple-table-added.png'});
    await page.locator('#widgetsTab').click();

    // Modify table to display project objects
    const preview = page.frameLocator('.previewBox iframe');
    await preview.getByText('Title', {exact: true}).click();
    await preview.getByText('Title', {exact: true}).fill('${localize("milestones", "table-header")}');
    await page.locator('#pagesTree').click();
    await page.waitForTimeout(1000);

    await preview.getByText('Firstname Lastname', {exact: true}).click();
    await preview.getByText('Firstname Lastname', {exact: true}).fill('${project.name}');
    await page.locator('#pagesTree').click();
    await page.waitForTimeout(1000);

    await preview.getByText('Example Job Title', {exact: true}).click();
    await preview.getByText('Example Job Title', {exact: true}).fill('${join(extract(project.milestones, "name"), ", ")}');
    await page.locator('#pagesTree').click();
    await page.waitForTimeout(1000);

    await preview.getByText('firstname.lastname@example.com', {exact: true}).click({button: 'right'});
    await page.waitForTimeout(1000);
    await page.locator('#context-menu-dialog .context_menu_icon').click();
    await page.waitForTimeout(1000);
    await page.locator('#context-menu-dialog').getByText('Remove Node', {exact: true}).click();
    await page.waitForTimeout(1000);

    await preview.getByText('Email', {exact: true}).click({button: 'right'});
    await page.waitForTimeout(1000);
    await page.locator('#context-menu-dialog .context_menu_icon').click();
    await page.waitForTimeout(1000);
    await page.locator('#context-menu-dialog').getByText('Remove Node', {exact: true}).click();
    await page.waitForTimeout(1000);

    await preview.getByText('Example Role', {exact: true}).click({button: 'right'});
    await page.waitForTimeout(1000);
    await page.locator('#context-menu-dialog .context_menu_icon').click();
    await page.waitForTimeout(1000);
    await page.locator('#context-menu-dialog').getByText('Remove Node', {exact: true}).click();
    await page.waitForTimeout(1000);

    await preview.getByText('Role', {exact: true}).click({button: 'right'});
    await page.waitForTimeout(1000);
    await page.locator('#context-menu-dialog .context_menu_icon').click();
    await page.waitForTimeout(1000);
    await page.locator('#context-menu-dialog').getByText('Remove Node', {exact: true}).click();
    await page.waitForTimeout(1000);

    await page.screenshot({path: 'screenshots/pages_output-expression.png'});

    // Add repeater to display the projects
    await page.locator('#pagesTree div.node:has(> div > span > b[title="tbody"]) div.node b[title="tr"]').click();
    await page.waitForTimeout(1000);
    await page.getByRole('link', {name: 'Repeater'}).click();
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
    await page.screenshot({path: 'screenshots/pages_element-details_repeater.png'});
    await page.getByRole('link', {name: 'Preview'}).click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/pages_repeater.png'});
    await page.getByRole('link', {name: 'General'}).click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/pages_element-details_general.png'});
    await page.getByRole('link', {name: 'HTML'}).click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/pages_element-details_html.png'});
    await page.getByRole('link', {name: 'Events'}).click();
    await page.waitForTimeout(1000);
    await page.getByRole('textbox', {name: 'Browser event (click, keydown'}).click();
    await page.keyboard.type('click');
    await page.keyboard.press('Tab');
    await page.waitForTimeout(200);
    await page.locator('#action-select').selectOption('Create new object');
    await page.waitForTimeout(200);
    await page.locator('.m-2 > svg > use').first().click();
    await page.waitForTimeout(200);
    await page.getByRole('textbox', {name: 'Name', exact: true}).fill('name');
    await page.getByRole('combobox').nth(5).selectOption('User Input');
    await page.screenshot({path: 'screenshots/pages_element-details_events.png'});

    await logout(page);
});

