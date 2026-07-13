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
import {goToModule, initialize} from "./helpers/init";
import {login, logout} from "./helpers/auth";
import * as fs from "node:fs";

test.beforeAll(async ({playwright}) => {

	let context = await initialize(playwright, {
		'SchemaNode': [
			{
				name: 'Project',
				schemaProperties: [
					{ name: 'description', propertyType: 'String' },
					{ name: 'dueDate', propertyType: 'Date', format: 'yyyy-MM-dd' },
				]
			},
			{
				name: 'Milestone'
			},
			{
				name: 'Task'
			}
		]
	});

	// Upload import files using multipart/form-data
	let csv = fs.readFileSync('1000-tasks.csv');
	await context.post(process.env.BASE_URL + '/structr/upload', {
		multipart: {
			file: {
				name: '1000-tasks.csv',
				mimeType: 'text/csv',
				buffer: csv
			}
		}
	});

	csv = fs.readFileSync('100-milestones.csv');
	await context.post(process.env.BASE_URL + '/structr/upload', {
		multipart: {
			file: {
				name: '100-milestones.csv',
				mimeType: 'text/csv',
				buffer: csv
			}
		}
	});

	csv = fs.readFileSync('10-projects.csv');
	await context.post(process.env.BASE_URL + '/structr/upload', {
		multipart: {
			file: {
				name: '10-projects.csv',
				mimeType: 'text/csv',
				buffer: csv
			}
		}
	});
});


test('job-queue', async ({page}, testInfo) => {

	console.log(testInfo.title);

	await login(page);

	await goToModule(page, '#job-queue_');

	// Wait for Code UI to load all components
	await page.waitForTimeout(1000);
	await page.screenshot({path: 'screenshots/importer.png'});

	// Create CSV document to import
	await goToModule(page, '#files_');

	await page.locator('#add-file-button').waitFor({state: 'visible'});
	await page.locator('#add-file-button').click();

	await page.waitForTimeout(1000);
	await page.screenshot({path: 'screenshots/importer_created-file.png'});

	await page.getByText('New File').first().click();
	await page.keyboard.type('import-file.csv');
	await page.keyboard.press('Enter');

	await page.waitForTimeout(1000);
	await page.screenshot({path: 'screenshots/importer_renamed-file.png'});

	await page.getByText('import-file.csv').first().click({button: 'right'});
	await page.getByText('General').first().waitFor({state: 'visible'});
	await page.getByText('General').first().click();
	await page.keyboard.press('Tab');
	await page.keyboard.type('text/csv');
	await page.keyboard.press('Enter');

	await page.waitForTimeout(1000);
	await page.screenshot({path: 'screenshots/importer_set-content-type.png'});

	await page.getByRole('button', {name: 'Close'}).click();

	/*
	// Edit file and add CSV lines
	await page.getByText('import-file.csv').first().click({button: 'right'});
	await page.getByText('Edit File').first().waitFor({state: 'visible'});
	await page.getByText('Edit File').first().click();
	await page.keyboard.type(csv);
	await page.getByRole('button', {name: 'Save and Close'}).click();
	await page.screenshot({ path: 'screenshots/importer_typed-csv-text.png' });
  */

	await page.locator('#file-tree-container').getByText('structr_uploads').first().click();
	await page.getByText('10-projects.csv').first().click({button: 'right'});
	await page.getByText('Import CSV').first().waitFor({state: 'visible'});
	await page.getByText('Import CSV').first().click();
	await page.locator('select#target-type-select option[value]:not([disabled])').first().waitFor({state: 'attached', timeout: 10_000});
	await page.locator('select#target-type-select').selectOption({label: 'Project'});
	await page.getByRole('button', {name: 'Start import'}).click();
	await page.getByRole('button', {name: 'Close'}).click();

	await page.getByText('100-milestones.csv').first().click({button: 'right'});
	await page.getByText('Import CSV').first().waitFor({state: 'visible'});
	await page.getByText('Import CSV').first().click();

	await page.locator('select#target-type-select option[value]:not([disabled])').first().waitFor({state: 'attached', timeout: 10_000});
	await page.locator('select#target-type-select').selectOption({label: 'Milestone'}, {timeout: 10_000});
	await page.getByRole('button', {name: 'Start import'}).click();
	await page.getByRole('button', {name: 'Close', exact: true }).isVisible();
	await page.getByRole('button', {name: 'Close', exact: true }).click();

	await page.getByText('1000-tasks.csv').first().click({button: 'right'});
	await page.getByText('Import CSV').first().waitFor({state: 'visible'});
	await page.getByText('Import CSV').first().click();
	await page.locator('select#target-type-select option[value]:not([disabled])').first().waitFor({state: 'attached', timeout: 10_000});
	await page.locator('select#target-type-select').selectOption({label: 'Task'});
	await page.getByRole('button', {name: 'Start import'}).click();
	await page.getByRole('button', {name: 'Close', exact: true}).click();
	await page.locator('#close-all-button').click();


	// Check import processes
	await goToModule(page, '#job-queue_');

	await page.locator('#job-queue-jobs-table').getByText('Job ID').click();
	await page.locator('#job-queue-jobs-table tbody tr').isVisible();

	await page.waitForTimeout(1000);
	await page.screenshot({path: 'screenshots/importer_check-import-process.png'});

	await page.locator('.close-message-button').click();

	await logout(page);
});
