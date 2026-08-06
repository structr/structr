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
import {login, logout} from './helpers/auth';
import {goToModule, initialize} from "./helpers/init";
import {
	createAndRenamePage,
	expandPageTree,
	focusCenterPaneMonacoEditor, focusMonacoEditorInContainer,
	getPageContainer,
	setNodeContent
} from "./helpers/pages";
import {openFileContextMenuFor, createFileWithContentAndContentType} from "./helpers/files";

test.beforeAll(async ({ playwright }) => {
	await initialize(playwright, {
		'SchemaNode': [
			{ name: 'Milestone' },
			{ name: 'Project' }
		],
		'SchemaRelationshipNode': [
			{
				sourceNode: {name: 'Project'},
				targetNode: {name: 'Milestone'},
				relationshipType: 'HAS_MILESTONE',
				sourceMultiplicity: '1',
				targetMultiplicity: '*',
				sourceJsonName: 'project',
				targetJsonName: 'milestones'
			}
		]
	});
});

test('search-and-refactor-code', async ({ page }, testInfo) => {

	let oldName = 'milestones';
	let newName = 'goals';

	console.log(testInfo.title);

	await login(page);

	// test setup
	{
		// set up page
		{
			await goToModule(page, '#pages_');

			// Wait for Pages UI to load all components
			await page.waitForTimeout(1000);
			//await page.screenshot({ path: 'screenshots/pages.png' });

			// import widgets
			await page.locator('#create_page').click();
			await page.locator('#import-widget-set').click();

			await page.waitForTimeout(1000);

			// close create page dialog again
			await page.getByRole('button', { name: 'Close' }).click();

			// create example page
			await createAndRenamePage(page, 2, 'test');

			let pageContainer = getPageContainer(page, 'test');
			await expandPageTree(page, 'test');

			let content1 = pageContainer.getElement('${capitalize(page.name)}');
			let content2 = pageContainer.getElement('Initial body text');

			await setNodeContent(page, content1, 'milestones');
			await setNodeContent(page, content2, 'milestones');
		}

		// set up file
		{
			await goToModule(page, '#files_');

			await createFileWithContentAndContentType(page, 'documentation.md', [`With this app we can define ${oldName} and track progress.`], 'text/plain');
		}

		// set up localization
		{
			await goToModule(page, '#localization_');

			// Create new Localization elements
			await page.getByRole('textbox', {name: 'Enter a key to create'}).click();
			await page.keyboard.type('milestones');
			await page.keyboard.press('Tab');
			await page.keyboard.type('table-header');
			await page.keyboard.press('Tab');
			await page.keyboard.type('en,de');
			await page.getByRole('button', {name: 'Create Localization'}).click();
			await page.locator('.___localizedName').nth(0).click();
			await page.keyboard.type('Meilensteine');
			await page.locator('.___localizedName').nth(1).click();
			await page.keyboard.type('Milestones');
			await page.keyboard.press('Tab');
		}

		// set up mail-template
		{
			await goToModule(page, '#mail-templates_');

			await page.locator('#mail-template-name-preselect').fill('some-mail-template');
			await page.locator('#mail-template-locale-preselect').fill('en');
			await page.locator('button#create-mail-template').click();

			await focusMonacoEditorInContainer(page, '#mail-template-detail-form');
			await page.keyboard.type(`Welcome to the ${oldName} app!`);

			await page.locator('#save-mail-template-content-button').click();
		}

		// Navigate to security (to show that automatic navigation to code works)
		await goToModule(page, '#security_');
	}

	// helper method
	let updateFirstOccurrenceOfTextInActiveMonacoEditor = async (oldText, newText, save = true) => {

		await page.keyboard.press('ControlOrMeta+f');
		await page.keyboard.type(oldText);
		await page.keyboard.press('Escape');
		await page.keyboard.type(newText);

		if (save === true) {
			await page.keyboard.press('ControlOrMeta+s');
		}
	};


	let globalSearchActivator = await page.locator('[popovertarget="global-search-popover"]');
	await globalSearchActivator.click();

	let searchInput = await page.locator('#global-search-node-form input[name="searchString"]');
	await searchInput.waitFor({state: 'visible'});
	await searchInput.fill(oldName);

	let relationshipResultRow = await page.locator('#global-search-results tbody tr[data-type="SchemaRelationshipNode"][data-key="targetJsonName"]').first();
	await relationshipResultRow.click();

	let input = page.locator('input[data-attr-name="targetJsonName"]');

	// click on the backdrop to hide global search popover
	await globalSearchActivator.click();

	await input.fill('');
	await input.pressSequentially(newName);

	await page.locator('[id="action-button-save"]').click();


	/**
	 * Update file content
	 */
	await globalSearchActivator.click();

	// update search results
	await searchInput.fill('');
	await expect(page.locator('#global-search-results tbody tr')).toHaveCount(0);
	await searchInput.fill(oldName);

	let fileResultRow = await page.locator('#global-search-results tbody tr[data-type="File"][data-key="extractedContent"]').first();
	await fileResultRow.click();

	let dialogSaveButton = await page.locator('#dialogSaveButton');
	await expect(dialogSaveButton).toHaveClass(/disabled/);

	await focusMonacoEditorInContainer(page, '#dialogBox #files-tabs');
	await updateFirstOccurrenceOfTextInActiveMonacoEditor(oldName, newName, false);

	await expect(dialogSaveButton).not.toHaveClass(/disabled/);
	await dialogSaveButton.click();
	await expect(dialogSaveButton).toHaveClass(/disabled/);
	await page.locator('.closeButton').click();


	/**
	 * Update localization
	 */
	await globalSearchActivator.click();

	// update search results
	await searchInput.fill('');
	await expect(page.locator('#global-search-results tbody tr')).toHaveCount(0);
	await searchInput.fill(oldName);

	let localizationResultRow = await page.locator('#global-search-results tbody tr[data-type="Localization"][data-key="name"]').first();
	await localizationResultRow.click();

	await page.locator('#localization-key').fill('goals');
	await page.locator('#localization-fields-save').click();

	await page.locator('#localization-detail-table tr.localization').first().locator('.___localizedName').fill('Ziele');
	await page.locator('#localization-detail-table tr.localization').nth(1).locator('.___localizedName').fill('Goals');
	await page.locator('#localization-detail-table tr.localization').nth(1).locator('.___localizedName').blur();


	/**
	 * Update mail template
	 */
	await globalSearchActivator.click();

	// update search results
	await searchInput.fill('');
	await expect(page.locator('#global-search-results tbody tr')).toHaveCount(0);
	await searchInput.fill(oldName);

	let mailTemplateResultRow = await page.locator('#global-search-results tbody tr[data-type="MailTemplate"][data-key="text"]').first();
	await mailTemplateResultRow.click();

	await focusMonacoEditorInContainer(page, '#mail-template-detail-form');
	await updateFirstOccurrenceOfTextInActiveMonacoEditor(oldName, newName);


	/**
	 * Update first content element (either column header or data column)
	 */
	await globalSearchActivator.click();

	// update search results
	await searchInput.fill('');
	await expect(page.locator('#global-search-results tbody tr')).toHaveCount(0);
	await searchInput.fill(oldName);

	await page.waitForTimeout(1000);
	await page.screenshot({ path: 'screenshots/global-search.png' });

	let contentResultRow = page.locator('#global-search-results tbody tr').first();
	let firstNodeId = await contentResultRow.getAttribute('data-id');
	await contentResultRow.click();

	// wait until center pane has correct node id
	await expect(page.locator('#center-pane')).toHaveAttribute('data-element-id', firstNodeId);

	// click on the backdrop to hide global search popover
	await globalSearchActivator.click();

	await focusCenterPaneMonacoEditor(page);
	await updateFirstOccurrenceOfTextInActiveMonacoEditor(oldName, newName);


	/**
	 * Update second content element (either column header or data column)
	 */
	await globalSearchActivator.click();

	// update search results
	await searchInput.fill('');
	await expect(page.locator('#global-search-results tbody tr')).toHaveCount(0);
	await searchInput.fill(oldName);

	contentResultRow = await page.locator('#global-search-results tbody tr').first();
	let secondNodeId = await contentResultRow.getAttribute('data-id');
	await contentResultRow.click();

	// wait until center pane has correct node id
	await expect(page.locator('#center-pane')).toHaveAttribute('data-element-id', secondNodeId);

	// click on the backdrop to hide global search popover
	await globalSearchActivator.click();

	await focusCenterPaneMonacoEditor(page);
	await updateFirstOccurrenceOfTextInActiveMonacoEditor(oldName, newName);

	// show preview again
	await page.locator('#tabs-menu-preview').click();

	await logout(page);
});