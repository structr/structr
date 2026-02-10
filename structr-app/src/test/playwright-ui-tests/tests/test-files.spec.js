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
    await initialize(playwright, {
        'SchemaNode': [
            {name: 'Project'}
        ]
    });
});

async function openFileContextMenuFor(page, name) {

    await page.getByText(name).hover();
    await page.waitForTimeout(200);
    await page.locator('tr').filter({hasText: name}).getByRole('img', {name: 'Context-Menu'}).click();
    await page.waitForTimeout(200);
}

async function createFileWithContentAndContentType(page, name, content, contentType) {

    // create file, rename it, set content type and open import dialogs to make screenshots
    await page.locator('button[data-test-purpose="create-file"]').click();
    await page.getByText('New File ').click();
    await page.keyboard.type(name);
    await page.keyboard.press('Enter');
    await openFileContextMenuFor(page, name);
    await page.getByText('Edit File').click();
    await page.locator('.view-line').click();

    for (var row of content) {
        await page.keyboard.type(row);
        await page.keyboard.press('Enter');
    }
    await page.getByRole('button', {name: 'Save and close', exact: true}).click();
    await page.waitForTimeout(1000);

    // set content type
    await openFileContextMenuFor(page, name);
    await page.getByText('General').click();
    await page.getByRole('textbox', {name: 'Content Type'}).fill(contentType);
    await page.keyboard.press('Enter');
    await page.waitForTimeout(1000);
    await page.getByRole('button', {name: 'Close', exact: true}).click();
    await page.waitForTimeout(1000);
}

test('files', async ({page}) => {

    await login(page);

    // Files
    await page.locator('#files_').waitFor({state: 'visible'});
    await page.locator('#files_').click();

    // Wait for Files UI to load all components
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/files.png'});

    // Create new folder
    await page.locator('#add-folder-button').waitFor({state: 'visible'});
    await page.locator('#add-folder-button').click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/files_create-folder.png'});
    await page.locator('#file-tree-container').getByText('New Folder').first().click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/files_navigated-to-folder.png'});

    // Rename folder
    await page.locator('#file-tree-container').getByText('/').first().click();
    await page.locator('#folder-contents-container').getByText('New Folder').first().click();
    await page.keyboard.type('Renamed folder');
    await page.keyboard.press('Enter');
    await page.locator('#file-tree-container').click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/files_renamed-folder.png'});

    // create test.csv, open import dialog and create a screenshot
    await createFileWithContentAndContentType(page, 'projects.csv', ['Name;ProjectID', 'Project 1;prj0001', 'Project 2;prj0002', 'Project 3;prj0003'], 'text/csv');
    await openFileContextMenuFor(page, 'projects.csv');
    await page.getByText('Import CSV').click();
    await page.waitForTimeout(1000);
    await page.locator('#delimiter').selectOption(';');
    await page.locator('#target-type-select').selectOption('Project');
    await page.screenshot({path: 'screenshots/files_import-csv-dialog.png'});
    await page.waitForTimeout(200);
    await page.getByRole('button', {name: 'Close', exact: true}).click();

    // create test.xml, open import dialog and create a screenshot
    await createFileWithContentAndContentType(page, 'projects.xml', [
        '<projects>',
        '<project name="Project 1">',
        '<task name="Task 1" />',
        '<task name="Task 2" />',
        '<task name="Task 3" />',
        '</project>',
        '<project name="Project 2">',
        '<task name="Task 4" />',
        '<task name="Task 5" />',
        '<task name="Task 6" />',
        '</project>',
        '<project name="Project 3">',
        '<task name="Task 7" />',
        '<task name="Task 8" />',
        '<task name="Task 9" />',
        '</project>',
        '</projects>',
    ], 'text/xml');

    // click Import CSV in context menu
    await openFileContextMenuFor(page, 'projects.xml');
    await page.getByText('Import XML').click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/files_import-xml-dialog.png'});
    await page.waitForTimeout(1000);
    await page.getByRole('button', {name: 'Close', exact: true}).click();

    await logout(page);
});

