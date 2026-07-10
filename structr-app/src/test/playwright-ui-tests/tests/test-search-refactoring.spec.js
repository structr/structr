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
	focusCenterPaneMonacoEditor,
	getPageContainer,
	setNodeContent
} from "./helpers/pages";

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

	// Navigate to security (to show that automatic navigation to code works)
	await goToModule(page, '#security_');

	let globalSearchActivator = await page.locator('[popovertarget="global-search-popover"]');
	await globalSearchActivator.click();

	let searchInput = await page.locator('#global-search-node-form input[name="queryString"]');
	await searchInput.waitFor({state: 'visible'});

	await searchInput.fill(oldName);

	// wait for results
	let relationshipResultRow = await page.getByRole('cell', { name: 'targetJsonName', exact: true });

	await relationshipResultRow.click();

	// Wait for Code UI to load all components
	await page.waitForTimeout(1000);

	let input = page.locator('input[data-attr-name="targetJsonName"]');

	// click on the backdrop to hide global search popover
	await globalSearchActivator.click();

	await input.fill('');
	await input.pressSequentially(newName);

	await page.locator('[id="action-button-save"]').click();


	let updateFirstOccurrenceOfTextInMonaco = async (oldText, newText) => {

		await focusCenterPaneMonacoEditor(page);

		await page.keyboard.press('ControlOrMeta+f');
		await page.keyboard.type(oldText);
		await page.keyboard.press('Escape');
		await page.keyboard.type(newText);
		await page.keyboard.press('ControlOrMeta+s');
	};

	/**
	 * Update first content element (either column header or data column)
	 */
	await globalSearchActivator.click();

	// update search results
	await searchInput.fill('');
	await expect(page.locator('#global-search-results tbody tr')).toHaveCount(0);
	await searchInput.fill(oldName);
    await page.keyboard.press('Enter');

	await page.waitForTimeout(1000);
    await page.screenshot({ path: 'screenshots/global-search.png' });

	let contentResultRow = await page.getByRole('cell', { name: 'Content', exact: true }).first();
	await contentResultRow.click();

	// click on the backdrop to hide global search popover
	await globalSearchActivator.click();

	await updateFirstOccurrenceOfTextInMonaco(oldName, newName);


	/**
	 * Update second content element (either column header or data column)
	 */
	await globalSearchActivator.click();

	// update search results
	await searchInput.fill('');
	await expect(page.locator('#global-search-results tbody tr')).toHaveCount(0);
	await searchInput.fill(oldName);

    contentResultRow = await page.getByRole('cell', { name: 'Content', exact: true }).first();
	await contentResultRow.click();

	// click on the backdrop to hide global search popover
	await globalSearchActivator.click();

	await updateFirstOccurrenceOfTextInMonaco(oldName, newName);

	// show preview again
	await page.locator('#tabs-menu-preview').click();

	await logout(page);
});