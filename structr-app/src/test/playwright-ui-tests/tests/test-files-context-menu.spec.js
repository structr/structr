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
import {goToModule, initialize, waitForDialogBoxToClose} from "./helpers/init";
import {login} from "./helpers/auth";
import {openFileContextMenuFor} from "./helpers/files";

test.beforeAll(async ({playwright}) => {

	// created via REST, so the files reach the UI through the folder listing and not as new objects of this session
	await initialize(playwright, {
		'File': [
			{name: 'items.csv', contentType: 'text/plain'},
			{name: 'report.txt', contentType: 'text/plain'}
		],
		'Folder': [
			{name: 'archive'}
		]
	});
});

test('files-context-menu-uses-changed-content-type', async ({page}, testInfo) => {

	console.log(testInfo.title);

	await login(page);

	await goToModule(page, '#files_');

	await page.locator('#file-tree-container').getByText('/').first().click();

	// change the content type in the properties dialog
	await openFileContextMenuFor(page, 'items.csv');
	await page.getByText('General').click();
	await page.getByRole('textbox', {name: 'Content Type'}).fill('text/csv');
	await page.keyboard.press('Enter');
	await page.getByRole('button', {name: 'Close', exact: true}).click();

	await waitForDialogBoxToClose(page);

	// wait until the server has confirmed the change, so only the context menu itself can be stale
	await page.waitForFunction(() => Object.values(StructrModel.objects).some(obj => obj.name === 'items.csv' && obj.contentType === 'text/csv'));

	await openFileContextMenuFor(page, 'items.csv');

	await expect(page.getByText('Import CSV'), 'the context menu must offer the CSV import for the new content type without reloading the folder').toBeVisible();
});

test('files-context-menu-downloads-renamed-file', async ({page}, testInfo) => {

	console.log(testInfo.title);

	await login(page);

	await goToModule(page, '#files_');

	await page.locator('#file-tree-container').getByText('/').first().click();

	// rename the file in the list, to a name with characters that must be encoded in a URL
	await page.locator('#folder-contents-container').getByText('report.txt').first().click();
	await page.keyboard.type('renamed #1.txt');
	await page.keyboard.press('Enter');

	await page.waitForFunction(() => Object.values(StructrModel.objects).some(obj => obj.name === 'renamed #1.txt' && obj.path === '/renamed #1.txt'));

	// only the URL of the download matters, so the request is captured and the page stays where it is
	await page.route(url => url.searchParams.has('_filename'), route => route.abort());

	const downloadRequest = page.waitForRequest(request => new URL(request.url()).searchParams.has('_filename'));

	await openFileContextMenuFor(page, 'renamed #1.txt');
	await page.getByText('Download File').click();

	const url = new URL((await downloadRequest).url());

	expect(url.pathname, 'the download must use the encoded path after the rename').toBe('/renamed%20%231.txt');
	expect(url.searchParams.get('_filename'), 'the download must use the name after the rename, in the parameter the server reads').toBe('renamed #1.txt');
});

test('files-context-menu-of-current-folder', async ({page}, testInfo) => {

	console.log(testInfo.title);

	await login(page);

	await goToModule(page, '#files_');

	await page.locator('#file-tree-container').getByText('archive').first().click();

	await page.locator('#folder-contents .context-menu-container').getByRole('img', {name: 'Context-Menu'}).click();

	await expect(page.locator('#context-menu-dialog').getByText('General', {exact: true}), 'the context menu of the current folder must open').toBeVisible();
});
