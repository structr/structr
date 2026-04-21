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
import {expect, test} from '@playwright/test';
import {initialize} from "./helpers/init";
import {login, logout} from "./helpers/auth";
import {
    collapsePageTree,
    configureFunctionQuery,
    configureGeneralAttributes,
    configureHTMLAttributes,
    createAndRenamePage,
    expandOrCollapseElement,
    expandPageTree,
    focusCenterPaneMonacoEditor,
    getPageContainer,
    insertFrontendJs,
    insertInputWithLabel,
    resizePagesTree,
    sendCtrlPlusA,
    setNodeContent,
    useContextMenu,
    resizeRightFlyout
} from "./helpers/pages";

test.beforeAll(async ({playwright}) => {
    await initialize(playwright, {
        'SchemaNode': [
            {
                name: 'Project',
                schemaProperties: [
                    {name: 'description', propertyType: 'String'},
                    {name: 'dueDate', propertyType: 'Date', format: 'yyyy-MM-dd'},
                ]
            }
        ],
        'Project': [
            { name: 'Project #1', dueDate: '2026-01-01', description: 'This is a project' },
            { name: 'Project #2', dueDate: '2026-02-01', description: 'This is another project' },
            { name: 'Project #3', dueDate: '2026-03-01', description: 'This is the third project' },
            { name: 'Project #4', dueDate: '2026-04-01', description: 'This is the fourth project' },
            { name: 'Project #5', dueDate: '2026-05-01', description: 'This is the fifth project' },
        ]
    });
});

test('pages', async ({page}) => {

    await login(page);

    // Dashboard
    await page.locator('#dashboard_').waitFor({state: 'visible'});
    await page.locator('#dashboard_').click();
    await page.getByRole('link', { name: 'Deployment' }).click();
    await page.waitForTimeout(200);

    await page.locator('label[for="deploy-action-import"]').click();
    await page.locator('label[for="deploy-type-data"]').click();
    await page.locator('label[for="deploy-target-zip"]').click();
    await page.waitForTimeout(200);

    await page.locator('#data-deployment-url-input').fill('http://localhost:8082/structr/widgets.zip');
    await page.waitForTimeout(200);

    await page.locator('#do-data-import-from-zip').click();
    await page.waitForTimeout(200);

    await page.getByRole('button', { name: 'Yes' }).click();

    await page.getByRole('button', { name: 'Reload Page' }).isVisible();
    await page.waitForTimeout(200);
    await page.getByRole('button', { name: 'Reload Page' }).click();

    // Pages
    await page.locator('#pages_').waitFor({state: 'visible'});
    await page.locator('#pages_').click();

    await resizePagesTree(page, -200);

    await createAndRenamePage(page, 4, 'projects');

    await expandPageTree(page, 'projects');

    let pageContainer = getPageContainer(page, 'projects');
    pageContainer.getTextNode().click();

    let mainContent = pageContainer.getElement('Main Content');

    await useContextMenu(page, mainContent, 'Suggested Widgets', 'Components', 'Table');

    await page.screenshot({path: 'screenshots/widgets_insert-table-dialog.png'});

    await page.locator('#dataSource').selectOption('All Project nodes');

    await page.getByRole('button', { name: 'Append Widget' }).click();

    let table = pageContainer.getElement('Table');
    await table.getTextNode().click();

    await page.locator('#previewTab.slideout-activator').click();
    await page.waitForTimeout(2000);

    await resizeRightFlyout(page, -400);

    await page.locator('label').filter({ hasText: 'description' }).click();
    await page.waitForTimeout(200);
    await page.locator('label').filter({ hasText: 'dueDate' }).click();
    await page.waitForTimeout(200);

    await page.getByText('Set template..').nth(2).click();
    await page.getByText('formatted-date').click();

    await page.waitForTimeout(200);
    await page.getByText('dueDate formatted-date ⠿').click();

    // open "Render Template Settings"
    await page.getByText('Render Template Settings').click();

    await page.locator('input[name="dateFormat"]').fill('dd.MM.yyyy');

    await page.screenshot({path: 'screenshots/widgets_table-fields.png'});

    await page.getByRole('button', { name: 'Controller' }).click();
    await page.getByRole('textbox', { name: 'Selection Channel' }).fill('current');

    // click somewhere else to save the value
    await page.locator('#page-size-input').click();

    await page.screenshot({path: 'screenshots/widgets_table_controller.png'});

    // insert edit form
    await useContextMenu(page, mainContent, 'Suggested Widgets', 'Components', 'Edit Form');
    await page.locator('select#dataSource').selectOption('The "current" object');
    await page.getByRole('button', { name: 'Append Widget' }).click();

    let editForm = pageContainer.getElement('Edit Form');
    editForm.getTextNode().click();

    await page.waitForTimeout(5000);

    await page.locator('label').filter({ hasText: 'description' }).click();
    await page.waitForTimeout(5000);
    await page.locator('label').filter({ hasText: 'dueDate' }).click();
    await page.waitForTimeout(5000);

    await page.waitForTimeout(5000);

    await page.getByText('textfield').nth(1).click();
    // wait for context menu to appear
    await page.waitForTimeout(5000);
    await page.getByText('textarea', { exact: true }).click();
    await page.waitForTimeout(5000);

    // after changing the first textfield to a textarea, the next textfield to click is again the one with index 1!
    await page.getByText('textfield').nth(1).click();
    // wait for context menu to appear
    await page.waitForTimeout(5000);

    await page.getByText('datepicker').click();

    // go to projects page, select a project and edit the values
    await page.goto(process.env.BASE_URL + '/projects');

    await page.getByRole('cell', { name: 'Project #1' }).click();
    await page.getByRole('textbox', { name: 'Description' }).fill('This is a project with a new description');
    await page.getByPlaceholder('Due Date').fill('2027-01-01');
    await page.getByRole('button', { name: 'Save' }).click();

    await page.waitForTimeout(200);

    // test ascending sorting
    await page.getByRole('columnheader', { name: 'Name' }).click();

    await expect(await page.locator('table tr:nth-child(1) td:nth-child(1)')).toHaveText('Project #1');
    await expect(await page.locator('table tr:nth-child(1) td:nth-child(2)')).toHaveText('This is a project with a new description');

    await expect(await page.locator('table tr:nth-child(2) td:nth-child(1)')).toHaveText('Project #2');
    await expect(await page.locator('table tr:nth-child(3) td:nth-child(1)')).toHaveText('Project #3');
    await expect(await page.locator('table tr:nth-child(4) td:nth-child(1)')).toHaveText('Project #4');
    await expect(await page.locator('table tr:nth-child(5) td:nth-child(1)')).toHaveText('Project #5');

    // test descending sorting
    await page.getByRole('columnheader', { name: 'Name' }).click();

    await expect(await page.locator('table tr:nth-child(1) td:nth-child(1)')).toHaveText('Project #5');
    await expect(await page.locator('table tr:nth-child(2) td:nth-child(1)')).toHaveText('Project #4');
    await expect(await page.locator('table tr:nth-child(3) td:nth-child(1)')).toHaveText('Project #3');
    await expect(await page.locator('table tr:nth-child(4) td:nth-child(1)')).toHaveText('Project #2');
    await expect(await page.locator('table tr:nth-child(5) td:nth-child(1)')).toHaveText('Project #1');

    await page.goto(process.env.BASE_URL + '/structr/');

    await logout(page);
});

