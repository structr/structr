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
import {login, logout} from "./helpers/auth";
import {goToModule, initialize} from "./helpers/init";
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
	setNodeContent,
	useContextMenu
} from "./helpers/pages";

let runTests = [ 1, 2, 3 ];

test.beforeAll(async ({ playwright }) => {
	await initialize(playwright, {
		'SchemaNode': [
			{
				name: 'Project',
				schemaProperties: [
					{name: 'description', propertyType: 'String'},
					{name: 'dueDate', propertyType: 'Date', format: 'yyyy-MM-dd'},
				]
			},
			{name: 'Employee'},
			{name: 'Client'},
			{name: 'Tag'},
			{name: 'Task'}
		],
		'Employee': [{name: 'Employee #1'}, {name: 'Employee #2'}, {name: 'Employee #3'}],
		'Client': [{name: 'Client #1'}, {name: 'Client #2'}, {name: 'Client #3'}],
		'Tag': [{name: 'Tag #1'}, {name: 'Tag #2'}, {name: 'Tag #3'}],
		'Task': [{name: 'Task #1'}, {name: 'Task #2'}, {name: 'Task #3'}],
		'SchemaRelationshipNode': [
			{
				sourceNode: {name: 'Project'},
				targetNode: {name: 'Task'},
				relationshipType: 'HAS_TASK',
				sourceMultiplicity: '1',
				targetMultiplicity: '*',
				sourceJsonName: 'project',
				targetJsonName: 'tasks',
			},
			{
				sourceNode: {name: 'Employee'},
				targetNode: {name: 'Project'},
				relationshipType: 'MANAGES',
				sourceMultiplicity: '1',
				targetMultiplicity: '*',
				sourceJsonName: 'manager',
				targetJsonName: 'project',
			},
			{
				sourceNode: {name: 'Client'},
				targetNode: {name: 'Project'},
				relationshipType: 'HAS_CLIENT',
				sourceMultiplicity: '1',
				targetMultiplicity: '1',
				sourceJsonName: 'client',
				targetJsonName: 'project',
			},
			{
				sourceNode: {name: 'Project'},
				targetNode: {name: 'Tag'},
				relationshipType: 'HAS_TAGS',
				sourceMultiplicity: '*',
				targetMultiplicity: '*',
				sourceJsonName: 'projects',
				targetJsonName: 'tags',
			},
		]
	});
 });

test('pages', async ({page}, testInfo) => {

	console.log(testInfo.title);

	await login(page);

	await goToModule(page, '#pages_');

	await expect(page.locator('#main #pages')).toBeVisible();

	await resizePagesTree(page, -200);

	// import widgets
	await page.locator('#create_page').click();
	const importWidgetSetButton = page.locator('#import-widget-set');
	await importWidgetSetButton.click();
	await expect(importWidgetSetButton).toBeHidden();

	let createParametersAndReturnInputDropzones = async (parameters = []) => {

		let dropzones = [];

		let parameterMappingsContainer = page.locator('.em-parameter-mappings-container');

		for (const [idx, paramName] of parameters.entries()) {

			await page.locator('.em-add-parameter-mapping-button').click();

			let row = parameterMappingsContainer.locator('.em-parameter-mapping').nth(idx);
			await expect(row).toBeVisible();

			await expect(parameterMappingsContainer.locator('.em-parameter-mapping')).toHaveCount(idx+1)

			await row.locator('.parameter-name-input').fill(paramName);
			await row.locator('.parameter-type-select').selectOption('User Input');

			let dropzone = row.locator('.parameter-user-input');
			await expect(dropzone).toBeVisible();

			dropzones.push(dropzone);
		}

		return dropzones;
	};

	// close create page dialog again
	await page.getByRole('button', { name: 'Close' }).click();

	// create projects page
	if (runTests.includes(1)) {

		await createAndRenamePage(page, 2, 'projects');
		await expandPageTree(page, 'projects');
		await insertFrontendJs(page, 'projects');

		let pageContainer = getPageContainer(page, 'projects');

		// remove Initial body text
		let removeNode = pageContainer.getElement('Initial body text');
		await useContextMenu(page, removeNode, 'Remove Node');

		// insert form
		let divContainer = pageContainer.getElement('div');
		await useContextMenu(page, divContainer, 'Insert HTML element', 'e-f', 'form');
		await useContextMenu(page, divContainer, 'Insert div element');

		// insert label with input field and text
		let formContainer = divContainer.getElement('form');
		await insertInputWithLabel(page, formContainer, 'Name', 'input');

		let input = formContainer.getElement('input', 0);
		await configureHTMLAttributes(page, input, { type: 'text', name: 'name' });

		// insert button element
		await useContextMenu(page, formContainer, 'Insert HTML element', 'b', 'button');

		let buttonContainer = formContainer.getElement('Initial text for button');
		await setNodeContent(page, buttonContainer, 'Create Project');

		// build repeater config for projects div
		let repeaterDiv = pageContainer.getElement('div', 1);
		await useContextMenu(page, repeaterDiv, 'Insert content element', '#content');
		let content = repeaterDiv.getElement('#text');
		await setNodeContent(page, content, '${project.name}')

		await configureFunctionQuery(page, repeaterDiv, 'find(\'Project\')', 'project');

		// build event action mapping for form
		await page.locator('span').filter({hasText: 'form'}).click();
		await page.getByRole('link', {name: 'HTML'}).click();
		await page.locator('input[name="_html_method"]').fill('post');
		await page.getByRole('link', {name: 'General'}).click();
		await page.locator('input[name="_html_id"]').fill('create-project-form');
		await page.getByRole('link', {name: 'Events'}).click();
		await page.getByRole('textbox', {name: 'Browser event (click, keydown'}).fill('submit');
		await page.keyboard.press('Tab');
		await page.locator('#action-select').selectOption('Create new object');

		let typeInput = page.locator('#data-type-input.combined-input-select-field');
		await page.getByRole('textbox', {name: 'Custom type or script'}).fill('Project');
		await page.keyboard.press('Tab');

		let dropzones = await createParametersAndReturnInputDropzones([ 'name' ]);

		await page.locator('span').filter({hasText: /^input$/}).hover();
		await page.mouse.down();
		await dropzones[0].hover();
		await page.mouse.up();

		await page.getByLabel('Behaviour on success Define').selectOption('Navigate to a new page');
		await page.keyboard.press('Tab');
		await page.getByRole('textbox', {name: 'Success URL'}).fill('/project/{result.id}');
		await page.keyboard.press('Tab');

		await page.waitForTimeout(1000);
		await page.screenshot({path: 'screenshots/pages_create-form_event-action-mapping-configuration.png'});

		await page.reload();

		// click on a different element to disable highlighting for the element we want to screenshot
		await page.locator('span').filter({hasText: 'body'}).click();
		await page.locator('span').filter({hasText: 'script'}).hover();

		// take a screenshot of the form element
		await page.locator('div.node:has(b[title="form"])').nth(4).screenshot({path: 'screenshots/pages_create-form-element.png'});

		await collapsePageTree(page, 'projects');
	}

	// create project page
	if (runTests.includes(2)) {

		await createAndRenamePage(page, 2, 'project');
		await expandPageTree(page, 'project');
		await insertFrontendJs(page, 'project');

		let pageContainer = getPageContainer(page, 'project');

		// remove Initial body text
		let removeNode = pageContainer.getElement('Initial body text');
		await useContextMenu(page, removeNode, 'Remove Node');

		// change title
		await page.locator('span').filter({hasText: '${capitalize(page.name)}'}).nth(2).click();
		await focusCenterPaneMonacoEditor(page);
		await page.keyboard.press('ControlOrMeta+a');
		await page.keyboard.type('Edit Project "${current.name}"');
		await page.getByRole('button', {name: 'Save'}).click();

		let divContainer = pageContainer.getElement('div');
		await useContextMenu(page, divContainer, 'Insert HTML element', 'e-f', 'form');

		// insert label with input field and text
		let formContainer = divContainer.getElement('form');
		await insertInputWithLabel(page, formContainer, 'Name', 'input');
		await insertInputWithLabel(page, formContainer, 'Description', 'input');
		await insertInputWithLabel(page, formContainer, 'Due date', 'input');

		let input1 = formContainer.getElement('input', 0);
		await configureHTMLAttributes(page, input1, { type: 'text', name: 'name', value: '${current.name}' });

		let input2 = formContainer.getElement('input', 1);
		await configureHTMLAttributes(page, input2, { type: 'text', name: 'description', value: '${current.description}' });

		let input3 = formContainer.getElement('input', 2);
		await configureHTMLAttributes(page, input3, { type: 'date', name: 'dueDate', value: '${dateFormat(current.dueDate, "yyyy-MM-dd")}' });

		// click on different element before screenshotting to avoid ugly spellcheck lines in screenshot (WTF?)
		await page.locator('input[name="_html_autofocus"]').click();


		await page.waitForTimeout(1000);
		await page.screenshot({path: 'screenshots/pages_edit-form_input-configuration.png'});

		// insert button element
		await useContextMenu(page, formContainer, 'Insert HTML element', 'b', 'button');
		let buttonContainer = formContainer.getElement('Initial text for button');
		await setNodeContent(page, buttonContainer, 'Save Project');


		// build event action mapping for form
		await page.locator('span').filter({hasText: 'form'}).click();
		await page.getByRole('link', {name: 'HTML'}).click();
		await page.locator('input[name="_html_method"]').fill('post');
		await page.getByRole('link', {name: 'General'}).click();
		await page.locator('input[name="_html_id"]').fill('save-project-form');
		await page.getByRole('link', {name: 'Events'}).click();
		await page.getByRole('textbox', {name: 'Browser event (click, keydown'}).fill('submit');
		await page.keyboard.press('Tab');
		await page.locator('#action-select').selectOption('Update object');
		await page.getByRole('textbox', {name: 'Custom type or script'}).fill('Project');
		await page.keyboard.press('Tab');
		await page.locator('#id-expression-input').fill('${current.id}');
		await page.keyboard.press('Tab');

		let dropzones = await createParametersAndReturnInputDropzones([ 'name', 'description', 'dueDate' ]);

		// drag inputs to dropzones
		for (var i = 0; i < dropzones.length; i++) {

			await page.locator('span').filter({hasText: 'input'}).nth(i).hover();
			await page.mouse.down();
			await dropzones[i].hover();
			await page.mouse.up();
		}

		await page.getByLabel('Behaviour on success Define').selectOption('Reload the current page');
		await page.keyboard.press('Tab');

		await page.waitForTimeout(1000);
		await page.screenshot({path: 'screenshots/pages_edit-form_event-action-mapping-configuration.png'});

		// reload to un-select the form element
		await page.reload();

		// click on a different element to disable highlighting for the element we want to screenshot
		await page.locator('span').filter({hasText: 'form#save-project-form'}).click();
		await page.locator('span').filter({hasText: 'body'}).click();
		await page.locator('span').filter({hasText: 'head'}).hover();

		// take a screenshot of the form element
		await page.locator('div.node:has(b[title="form"])').nth(4).screenshot({path: 'screenshots/pages_edit-form-element.png'});

		await page.goto(process.env.BASE_URL + '/projects');

		await page.locator('input[name="name"]').fill('Project #1');
		await page.locator('button').click();

		await expect(page.locator('h1')).toHaveText('Edit Project "Project #1"');

		await page.goto(process.env.BASE_URL + '/structr/');
		await expect(page).toHaveTitle(/Structr/);

		await collapsePageTree(page, 'project');
	}

	// next part: advanced example
	if (runTests.includes(3)) {
		await createAndRenamePage(page, 2, 'advanced');
		await expandPageTree(page, 'advanced');
		await insertFrontendJs(page, 'advanced');

		let pageContainer = getPageContainer(page, 'advanced');

		// remove Initial body text
		let removeNode = pageContainer.getElement('Initial body text');
		await useContextMenu(page, removeNode, 'Remove Node');

		// change title
		await page.locator('span').filter({hasText: '${capitalize(page.name)}'}).nth(2).click();
		await focusCenterPaneMonacoEditor(page);
		await page.keyboard.press('ControlOrMeta+a');
		await page.keyboard.type('Edit Project "${current.name}"');
		await page.getByRole('button', {name: 'Save'}).click();

		// collapse some elements for screenshotting
		let head = pageContainer.getElement('head');
		let h1 = pageContainer.getElement('h1');
		await expandOrCollapseElement(page, head, 'collapse');
		await expandOrCollapseElement(page, h1, 'collapse');

		let divContainer = pageContainer.getElement('div');
		await useContextMenu(page, divContainer, 'Insert HTML element', 'e-f', 'form');

		// insert label with input field and text
		let formContainer = divContainer.getElement('form');
		await insertInputWithLabel(page, formContainer, 'Manager', 'select');
		await insertInputWithLabel(page, formContainer, 'Client', 'select');
		await insertInputWithLabel(page, formContainer, 'Tags', 'select');
		await insertInputWithLabel(page, formContainer, 'Tasks', 'select');

		let tagsSelect = formContainer.getElement('select', 2);
		let tasksSelect = formContainer.getElement('select', 3);

		await configureHTMLAttributes(page, tagsSelect, { multiple: 'true' });
		await configureHTMLAttributes(page, tasksSelect, { multiple: 'true' });

		let configs = [
			{ query: 'find(\'Employee\')', dataKey: 'employee', optionText: '${employee.name}', optionValue: '${employee.id}', selectedValues: 'current.manager' },
			{ query: 'find(\'Client\')', dataKey: 'client', optionText: '${client.name}', optionValue: '${client.id}', selectedValues: 'current.client' },
			{ query: 'find(\'Tag\')', dataKey: 'tag', optionText: '${tag.name}', optionValue: '${tag.id}', selectedValues: 'current.tags' },
			{ query: 'find(\'Task\')', dataKey: 'task', optionText: '${task.name}', optionValue: '${task.id}', selectedValues: 'current.tasks', screenshot: true },
		];

		let index = 0;

		for (let config of configs) {

			let labelContainer = formContainer.getElement('label', index);
			let select = labelContainer.getElement('select');
			await useContextMenu(page, select, 'Suggested HTML element', 'option');
			let option = labelContainer.getElement('option');
			await expandOrCollapseElement(page, labelContainer, 'expand');
			await option.getTextNode().click();
			await configureFunctionQuery(page, option, config.query, config.dataKey);
			await configureGeneralAttributes(page, option, { selectedValues: config.selectedValues });

			if (config.screenshot) {
				await page.locator('input#name-input').click();

				await page.waitForTimeout(1000);
				await page.screenshot({path: 'screenshots/pages_advanced-form_option-configuration.png'});
			}

			await configureHTMLAttributes(page, option, { value: config.optionValue });
			let optionText = labelContainer.getElement('Initial text for option');
			await setNodeContent(page, optionText, config.optionText);

			index++;
		}

		// insert button element
		await useContextMenu(page, formContainer, 'Insert HTML element', 'b', 'button');
		let buttonContainer = formContainer.getElement('Initial text for button');
		await setNodeContent(page, buttonContainer, 'Save Project');


		// build event action mapping for form
		await page.locator('span').filter({hasText: 'form'}).click();
		await page.getByRole('link', {name: 'HTML'}).click();
		await page.locator('input[name="_html_method"]').fill('post');
		await page.getByRole('link', {name: 'General'}).click();
		await page.locator('input[name="_html_id"]').fill('save-project-form');
		await page.getByRole('link', {name: 'Events'}).click();
		await page.getByRole('textbox', {name: 'Browser event (click, keydown'}).fill('submit');
		await page.keyboard.press('Tab');
		await page.locator('#action-select').selectOption('Update object');
		await page.getByRole('textbox', {name: 'Custom type or script'}).fill('Project');
		await page.keyboard.press('Tab');
		await page.locator('#id-expression-input').fill('${current.id}');
		await page.keyboard.press('Tab');

		let dropzones = await createParametersAndReturnInputDropzones([ 'manager', 'client', 'tags', 'tasks' ]);

		// drag inputs to dropzones
		for (var i = 0; i < dropzones.length; i++) {

			await page.locator('span').filter({hasText: 'select'}).nth(i).hover();
			await page.mouse.down();
			await dropzones[i].hover();
			await page.mouse.up();
		}

		await page.getByLabel('Behaviour on success Define').selectOption('Reload the current page');
		await page.keyboard.press('Tab');

		await page.waitForTimeout(1000);
		await page.screenshot({path: 'screenshots/pages_advanced-form_event-action-mapping-configuration.png'});

		// reload to un-select the form element
		await page.reload();

		// click on head element to deselect form for screenshot below
		let h = pageContainer.getElement('head');
		await h.getTextNode().hover();
		await h.getTextNode().click();

		// take a screenshot of the form element (background change is necessary because playwright tries to scroll the element into view, which hovers it apparently)
		await formContainer.locator.screenshot({path: 'screenshots/pages_advanced-form-element.png', style: '.nodeHover { background-color: transparent; }' });
	}

	await logout(page);
});
