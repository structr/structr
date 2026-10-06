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
import {goToModule, initialize} from "./helpers/init";
import {login} from "./helpers/auth";
import * as fs from "node:fs";

test.beforeAll(async ({playwright}) => {

	let context = await initialize(playwright);

	await context.post(process.env.BASE_URL + '/structr/upload', {
		multipart: {
			file: {
				name: '10-projects.csv',
				mimeType: 'text/csv',
				buffer: fs.readFileSync('10-projects.csv')
			}
		}
	});

	// a saved CSV import configuration for the built-in type Group, in the format the import dialog itself stores
	await context.post(process.env.BASE_URL + '/structr/rest/ApplicationConfigurationDataNode', {
		data: JSON.stringify({
			name: 'Group import',
			configType: 'csv-import',
			content: JSON.stringify({
				errors: [],
				config: {
					targetType: 'Group',
					delimiter: ',',
					quoteChar: '"',
					recordSeparator: 'LF',
					commitInterval: '1000',
					rfc4180Mode: false,
					strictQuotes: false,
					ignoreInvalid: false,
					distinct: false,
					range: '',
					importType: 'node',
					mappings: { name: 'name' },
					transforms: {},
					version: 2
				}
			})
		})
	});
});

test('csv-import-load-configuration-for-builtin-type', async ({page}, testInfo) => {

	// Ticket 1076: the CSV import dialog does not apply saved configurations correctly.
	// The dialog shows only custom types by default. Loading a configuration for a built-in
	// type must switch that filter off, otherwise the target type cannot be selected.

	console.log(testInfo.title);

	await login(page);

	await goToModule(page, '#files_');

	await page.locator('#file-tree-container').getByText('structr_uploads').first().click();
	await page.getByText('10-projects.csv').first().click({button: 'right'});
	await page.getByText('Import CSV').first().waitFor({state: 'visible'});
	await page.getByText('Import CSV').first().click();

	// wait until the dialog has loaded the schema types and the saved configurations
	await page.locator('select#target-type-select option[value]:not([disabled])').first().waitFor({state: 'attached', timeout: 10_000});
	await page.locator('select#load-csv-config-selector option', {hasText: 'Group import'}).waitFor({state: 'attached', timeout: 10_000});

	await page.locator('select#load-csv-config-selector').selectOption({label: 'Group import'});
	await page.locator('#load-csv-config-button').click();

	await expect(page.locator('select#target-type-select')).toHaveValue('Group');
	await expect(page.locator('input#target-type-custom-only')).not.toBeChecked();
	await expect(page.locator('select.attr-mapping[name="name"]')).toHaveValue('name');

	await page.screenshot({path: 'screenshots/csv-import_loaded-configuration.png'});
});
