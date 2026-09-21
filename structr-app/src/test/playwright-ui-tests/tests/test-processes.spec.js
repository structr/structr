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
import {test, expect} from '@playwright/test';
import {goToModule, initialize, waitForDialogBoxToClose} from "./helpers/init";
import {login, logout} from "./helpers/auth";
import * as fs from "node:fs";

test.beforeAll(async ({playwright}) => {

	const context = await initialize(playwright, {
		'SchemaNode': [
			{name: 'LeaveRequest'}
		]
	});

	// The editor needs a model with a diagram. Drawing one through the canvas
	// is fragile in a test, so the leave request model from the process module
	// is imported through the same REST method the dropzone in the UI uses.
	const xml      = fs.readFileSync('leave-request.bpmn', 'utf-8');
	const response = await context.post(process.env.BASE_URL + '/structr/rest/BpmnDefinitions/importBpmn', {
		data: JSON.stringify({
			xml:      xml,
			filename: 'leave-request.bpmn'
		})
	});

	if (!response.ok()) {
		throw new Error(`importBpmn failed with status ${response.status()}: ${await response.text()}`);
	}
});

test('processes', async ({page}, testInfo) => {

	console.log(testInfo.title);

	await login(page);

	// Process Definitions
	await goToModule(page, '#processes_');

	const definitionRow = page.locator('#processDefinitionsTable tbody tr').first();
	await definitionRow.waitFor({state: 'visible'});

	// the instance counts are patched in asynchronously
	await expect(definitionRow.locator('.active-instances-cell')).not.toHaveText('…');
	await page.waitForTimeout(500);
	await page.screenshot({path: 'screenshots/processes_definitions.png'});

	// BPMN editor
	await definitionRow.locator('.show-diagram-link').click();

	const editorHost = page.locator('.process-diagram-host');
	await editorHost.locator('g.bpmn-shape').first().waitFor({state: 'visible'});
	await page.locator('.btn-fit').click();
	await page.waitForTimeout(500);
	await page.screenshot({path: 'screenshots/processes_editor.png'});

	// Element tab: select a user task on the canvas
	const reviewTask = editorHost.locator('g.bpmn-shape', {has: page.locator('title', {hasText: 'Review leave request'})});
	await reviewTask.click();
	await page.getByText('Assignee (humanPerformer)').waitFor({state: 'visible'});
	await page.waitForTimeout(500);
	await page.screenshot({path: 'screenshots/processes_editor_user-task.png'});

	// Process tab: set the subject type and save
	await page.locator('.sidepanel-tab[data-tab="process"]').click();

	const subjectTypeSelect = page.locator('.input-process-subject-type').first();
	await subjectTypeSelect.locator('option[value="LeaveRequest"]').waitFor({state: 'attached'});
	await subjectTypeSelect.selectOption('LeaveRequest');

	await expect(page.locator('.btn-save')).toBeEnabled();
	await page.waitForTimeout(500);
	await page.screenshot({path: 'screenshots/processes_editor_process-settings.png'});

	await page.locator('.btn-save').click();
	await expect(page.locator('.btn-save')).toBeDisabled();

	await page.getByRole('button', {name: 'Close', exact: true}).click();
	await waitForDialogBoxToClose(page);

	// Start an instance from the definitions list
	await definitionRow.locator('.start-process').click();
	await page.waitForTimeout(1000);

	// Process Instances
	await page.locator('#processesTabsMenu a[href="#processes:instances"]').click();
	await page.locator('#processInstancesTable tbody tr').first().waitFor({state: 'visible'});
	await page.waitForTimeout(1000);
	await page.screenshot({path: 'screenshots/processes_instances.png'});

	// Task Instances
	await page.locator('#processesTabsMenu a[href="#processes:tasks"]').click();
	await page.locator('#processTasksTable tbody tr').first().waitFor({state: 'visible'});
	await page.waitForTimeout(1000);
	await page.screenshot({path: 'screenshots/processes_tasks.png'});

	await logout(page);
});
