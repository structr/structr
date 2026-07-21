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
import {
	goToModule,
	initialize,
	waitUntilAttributeAppearsAndReturnIt,
	waitUntilAttributeChangedAndReturnIt
} from "./helpers/init";
import {login, logout} from "./helpers/auth";
import {
	createAndRenamePage,
	expandPageTree,
	getPageContainer,
	resizePagesTree,
	useContextMenu,
	resizeRightFlyout, waitForPartialReload
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

test('pages', async ({page}, testInfo) => {

	console.log(testInfo.title);

	let timestamp = null;

	await login(page);

	await goToModule(page, '#pages_');

	await expect(page.locator('#main #pages')).toBeVisible();

	await resizePagesTree(page, -200);

	await page.locator('#create_page').click();

	const importWidgetSetButton = page.locator('#import-widget-set')
	await importWidgetSetButton.click();
	await expect(importWidgetSetButton).toBeHidden();

	// close create page dialog again
	await page.getByRole('button', { name: 'Close' }).click();

	await createAndRenamePage(page, 4, 'projects');

	await expandPageTree(page, 'projects');

	let pageContainer = getPageContainer(page, 'projects');
	pageContainer.getTextNode().click();

	let mainContent = pageContainer.getElement('Main Content');

	await useContextMenu(page, mainContent, 'Suggested Widgets', 'Components', 'Table');

	// make screenshot when widget form is visible
	await page.locator('#widget-form').isVisible();

	{
		let widgetForm = page.locator('#widget-form');

		await widgetForm.getByText('No Data Source⏷').click();
		await widgetForm.getByText('Use Existing Data Source').hover();
		await widgetForm.getByText('Custom Types').hover();
		await widgetForm.getByText('All Project Nodes').click();
	}

	await page.screenshot({path: 'screenshots/widgets_insert-table-dialog.png'});

	await page.getByRole('button', { name: 'Append Widget' }).click();

	let table = pageContainer.getElement('Table');
	await table.getTextNode().click();

	await page.locator('#previewTab.slideout-activator').click();

	await resizeRightFlyout(page, -400);

	// wait for list to fully (re)load
	timestamp = await waitUntilAttributeChangedAndReturnIt(page, '#sortable-list', 'data-test-timestamp', timestamp);

	await page.locator('label').filter({ hasText: 'description' }).click();

	// wait for list to fully (re)load
	timestamp = await waitUntilAttributeChangedAndReturnIt(page, '#sortable-list', 'data-test-timestamp', timestamp);

	await page.locator('label').filter({ hasText: 'dueDate' }).click();

	// wait for list to fully (re)load
	timestamp = await waitUntilAttributeChangedAndReturnIt(page, '#sortable-list', 'data-test-timestamp', timestamp);

	let dueDateTemplateDisplay = page.locator('#sortable-list .select-render-template[data-field-name="dueDate"]');
	await dueDateTemplateDisplay.click();
	let dueDateTplInContextMenu = dueDateTemplateDisplay.locator('.render-template-select').getByText('formatted-date', { exact: true });
	await dueDateTplInContextMenu.click();

	// wait for list to fully (re)load
	timestamp = await waitUntilAttributeChangedAndReturnIt(page, '#sortable-list', 'data-test-timestamp', timestamp);

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

	{
		let widgetForm = page.locator('#widget-form');

		await widgetForm.getByText('No Data Source⏷').click();
		await widgetForm.getByText('Use Existing Data Source').hover();
		await widgetForm.getByText('Channels').hover();
		await widgetForm.getByText('current', {exact: true}).click();
	}

	await page.getByRole('button', { name: 'Append Widget' }).click();

	let editForm = pageContainer.getElement('Edit Form');
	await editForm.getTextNode().click();

	// wait for list to fully (re)load
	timestamp = null; // important! new load, previous values would break this.
	timestamp = await waitUntilAttributeChangedAndReturnIt(page, '#sortable-list', 'data-test-timestamp', timestamp);

	await page.locator('label').filter({ hasText: 'description' }).click();

	// wait for list to fully (re)load
	timestamp = await waitUntilAttributeChangedAndReturnIt(page, '#sortable-list', 'data-test-timestamp', timestamp);

	await page.locator('label').filter({ hasText: 'dueDate' }).click();

	// wait for list to fully (re)load
	timestamp = await waitUntilAttributeChangedAndReturnIt(page, '#sortable-list', 'data-test-timestamp', timestamp);

	let descriptionRenderTemplateDisplay = page.locator('#sortable-list .select-render-template[data-field-name="description"]');
	await descriptionRenderTemplateDisplay.click();
	let descriptionRenderTplInContextMenu = descriptionRenderTemplateDisplay.locator('.render-template-select').getByText('textarea', { exact: true });
	await descriptionRenderTplInContextMenu.click();

	// wait for list to fully (re)load
	timestamp = await waitUntilAttributeChangedAndReturnIt(page, '#sortable-list', 'data-test-timestamp', timestamp);

	let dueDateRenderTemplateDisplay = page.locator('#sortable-list .select-render-template[data-field-name="dueDate"]');
	await dueDateRenderTemplateDisplay.click();
	let dueDateRenderTplInContextMenu = dueDateRenderTemplateDisplay.locator('.render-template-select').getByText('datepicker', { exact: true });
	await dueDateRenderTplInContextMenu.click();

	// wait for list to fully (re)load
	timestamp = await waitUntilAttributeChangedAndReturnIt(page, '#sortable-list', 'data-test-timestamp', timestamp);

	// go to projects page, select a project and edit the values
	await page.goto(process.env.BASE_URL + '/projects', { waitUntil: 'domcontentloaded', timeout: 10_000 });

	const componentSelector = 'structr-component[data-channel="project current"]';
	let lastRefresh = await page.getAttribute(componentSelector, 'data-last-refresh');

	await page.getByRole('cell', { name: 'Project #1' }).click();
	await page.getByRole('textbox', { name: 'Description' }).fill('This is a project with a new description');
	await page.getByPlaceholder('Due Date').fill('2027-01-01');
	await page.getByRole('button', { name: 'Save' }).click();

	// save itself must refresh the table
	lastRefresh = await waitForPartialReload(page, componentSelector, lastRefresh);

	await expect(await page.locator('table tr:nth-child(1) td:nth-child(2)')).toHaveText('This is a project with a new description');

	// test ascending sorting
	let nameHeaderLocator = page.getByRole('columnheader', { name: 'Name' });

	await nameHeaderLocator.click();

	lastRefresh = await waitForPartialReload(page, componentSelector, lastRefresh);

	await expect(nameHeaderLocator).toContainClass('descending');

	await expect(await page.locator('table tr:nth-child(1) td:nth-child(1)')).toHaveText('Project #1');
	await expect(await page.locator('table tr:nth-child(1) td:nth-child(2)')).toHaveText('This is a project with a new description');

	await expect(await page.locator('table tr:nth-child(2) td:nth-child(1)')).toHaveText('Project #2');
	await expect(await page.locator('table tr:nth-child(3) td:nth-child(1)')).toHaveText('Project #3');
	await expect(await page.locator('table tr:nth-child(4) td:nth-child(1)')).toHaveText('Project #4');
	await expect(await page.locator('table tr:nth-child(5) td:nth-child(1)')).toHaveText('Project #5');

	// test descending sorting
	await nameHeaderLocator.click();

	lastRefresh = await waitForPartialReload(page, componentSelector, lastRefresh);

	await expect(await page.locator('table tr:nth-child(1) td:nth-child(1)')).toHaveText('Project #5');
	await expect(await page.locator('table tr:nth-child(2) td:nth-child(1)')).toHaveText('Project #4');
	await expect(await page.locator('table tr:nth-child(3) td:nth-child(1)')).toHaveText('Project #3');
	await expect(await page.locator('table tr:nth-child(4) td:nth-child(1)')).toHaveText('Project #2');
	await expect(await page.locator('table tr:nth-child(5) td:nth-child(1)')).toHaveText('Project #1');

	await page.goto(process.env.BASE_URL + '/structr/');

	await logout(page);
});

