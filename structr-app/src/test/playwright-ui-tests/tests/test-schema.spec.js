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

test('schema', async ({page}) => {

    await login(page);

    // Schema
    await page.locator('#schema_').waitFor({state: 'visible'});
    await page.locator('#schema_').click();

    // Wait for Schema UI to load all components
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/schema.png'});

    // Create new type 'Project'
    await page.getByRole('button', {name: 'Create Data Type'}).waitFor({state: 'visible'});
    await page.getByRole('button', {name: 'Create Data Type'}).click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/schema_create-type_Project.png'});
    await page.getByPlaceholder('Type Name...').fill('Project');
    await page.getByRole('button', {name: ' Create ', exact: true}).click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/schema_created-type_Project.png'});

    // Add custom method and take a screenshot
    await page.getByText('Methods', {exact: true}).click();
    await page.locator('button[data-test-purpose="create-schema-method"]').click();
    await page.getByText('Add method', {exact: true}).click();
    await page.getByRole('textbox', {name: 'Enter method name'}).click();
    await page.keyboard.type("sendEMail");
    await page.locator('.monaco-editor').nth(0).click();
    await page.keyboard.type("{");
    await page.keyboard.press("Enter");
    await page.keyboard.type("$.sendPlaintextMail(...);");
    await page.screenshot({path: 'screenshots/schema_method-added_sendEMail.png'});

    // Add custom String property 'projectId'
    await page.getByText('Direct properties', {exact: true}).click();
    await page.getByRole('button', {name: 'Add direct property', exact: true}).click();
    await page.getByPlaceholder('JSON name').click();
    await page.getByPlaceholder('JSON name').fill('projectId');
    await page.locator('.unique').nth(0).check();
    await page.locator('.property-type').selectOption({value: 'String'});
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/schema_property-added_projectId.png'});
    await page.getByRole('button', {name: 'Save All', exact: true}).click();
    await page.waitForTimeout(1000);
    await page.getByRole('button', {name: 'Close', exact: true}).click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/schema_type-created_Project.png'});

    // Create new type 'Milestone'
    await page.locator('#create-type').waitFor({state: 'visible'});
    await page.locator('.non-block-ui-wrapper').waitFor({state: 'hidden', timeout: 30000}).catch(() => {
    });
    await page.locator('#create-type').click({timeout: 30000});
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/schema_create-type_Milestone.png'});
    await page.getByPlaceholder('Type Name...').fill('Milestone');
    await page.getByRole('button', {name: ' Create ', exact: true}).click();
    await page.waitForTimeout(1000);

    // Add custom String property 'milestoneId', custom Date property 'dueDate' and custom Function property 'projectId'
    // The Function property is needed for the JobQueue test (011-job-queue.spec.js) later on.
    await page.getByText('Direct properties', {exact: true}).click();
    await page.getByRole('button', {name: 'Add direct property', exact: true}).click();
    await page.getByPlaceholder('JSON name').click();
    await page.getByPlaceholder('JSON name').fill('milestoneId');
    await page.locator('.unique').nth(0).check();
    await page.locator('.property-type').selectOption({value: 'String'});
    await page.waitForTimeout(500);
    await page.getByRole('button', {name: 'Add direct property', exact: true}).click();
    await page.getByPlaceholder('JSON name').nth(1).click();
    await page.getByPlaceholder('JSON name').nth(1).fill('dueDate');
    await page.locator('.property-type').nth(1).selectOption({value: 'Date'});
    await page.waitForTimeout(500);
    await page.screenshot({path: 'screenshots/schema_property-added_dueDate.png'});
    await page.getByRole('button', {name: 'Add direct property', exact: true}).click();
    await page.getByPlaceholder('JSON name').nth(2).click();
    await page.getByPlaceholder('JSON name').nth(2).fill('projectId');
    await page.locator('.property-type').nth(2).selectOption({value: 'Function'});
    await page.getByRole('button', {name: 'Save All', exact: true}).click();
    await page.waitForTimeout(1000);
    await page.getByRole('button', {name: 'Read'}).click();
    await page.waitForTimeout(500);
    await page.keyboard.type('this.project.projectId');
    await page.getByRole('button', {name: 'Save and Close'}).click();
    await page.waitForTimeout(500);
    await page.getByRole('button', {name: 'Write'}).click();
    await page.waitForTimeout(500);
    await page.keyboard.type('set(this, \'project\', first(find(\'Project\', \'projectId\', value)))');
    await page.getByRole('button', {name: 'Save and Close'}).click();
    await page.waitForTimeout(500);
    await page.screenshot({path: 'screenshots/schema_added-Milestone-properties.png'});
    await page.getByRole('button', {name: 'Close', exact: true}).click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/schema_type-created_Milestone.png'});

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
    await page.getByRole('button', {name: 'Create', exact: true}).click();
    await page.screenshot({path: 'screenshots/schema_relationship-project-milestone-created.png'});
    await page.getByRole('button', {name: 'Close', exact: true}).click();

    // Create new type 'Task'
    await page.locator('#create-type').waitFor({state: 'visible'});
    await page.locator('#create-type').click();
    await page.waitForTimeout(500);
    await page.getByPlaceholder('Type Name...').fill('Task');
    await page.getByRole('button', {name: ' Create ', exact: true}).click();
    await page.waitForTimeout(500);

    // Add custom String property 'taskId' and custom Function property 'projectId'
    // The Function property is needed for the JobQueue test (011-job-queue.spec.js) later on.
    await page.getByText('Direct properties', {exact: true}).click();
    await page.getByRole('button', {name: 'Add direct property', exact: true}).click();
    await page.getByPlaceholder('JSON name').click();
    await page.getByPlaceholder('JSON name').fill('taskId');
    await page.locator('.unique').nth(0).check();
    await page.locator('.property-type').selectOption({value: 'String'});
    await page.waitForTimeout(500);
    await page.getByRole('button', {name: 'Add direct property', exact: true}).click();
    await page.getByPlaceholder('JSON name').nth(1).click();
    await page.getByPlaceholder('JSON name').nth(1).fill('projectId');
    await page.locator('.property-type').nth(1).selectOption({value: 'Function'});
    await page.getByRole('button', {name: 'Save All', exact: true}).click();
    await page.waitForTimeout(1000);
    await page.getByRole('button', {name: 'Read'}).click();
    await page.waitForTimeout(500);
    await page.keyboard.type('this.project.projectId');
    await page.getByRole('button', {name: 'Save and Close'}).click();
    await page.waitForTimeout(500);
    await page.getByRole('button', {name: 'Write'}).click();
    await page.waitForTimeout(500);
    await page.keyboard.type('set(this, \'project\', first(find(\'Project\', \'projectId\', value)))');
    await page.getByRole('button', {name: 'Save and Close'}).click();
    await page.waitForTimeout(500);
    await page.screenshot({path: 'screenshots/schema_added-Task-properties.png'});
    await page.getByRole('button', {name: 'Close', exact: true}).click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/schema_type-created_Project.png'});
    await page.getByText('Project Edit type Delete type').hover();
    await page.getByText('Project Edit type Delete type').locator('svg.edit-type-icon').click();
    await page.getByText('General', {exact: true}).click();
    await page.waitForTimeout(100);
    await page.screenshot({path: 'screenshots/schema_type-edit_Project.png'});
    await page.getByRole('button', {name: 'Close', exact: true}).click();
    await page.waitForTimeout(1000);

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
    await page.getByRole('button', {name: 'Create', exact: true}).click();
    await page.screenshot({path: 'screenshots/schema_relationship-project-task-created.png'});
    await page.getByRole('button', {name: 'Close', exact: true}).click();

    await logout(page);
});

