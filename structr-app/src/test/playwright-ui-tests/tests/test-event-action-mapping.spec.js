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
import {initialize} from "./helpers/init";
import {
    collapsePageTree,
    configureFunctionQuery,
    configureGeneralAttributes,
    configureHTMLAttributes,
    createAndRenamePage,
    expandOrCollapseElement,
    expandPageTree,
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


test('pages', async ({page}) => {

    let wait = 100;

    await login(page);

    // Pages
    await page.locator('#pages_').waitFor({state: 'visible'});
    await page.locator('#pages_').click();

    // Wait for Pages UI to load all components
    await page.waitForTimeout(wait);

    await resizePagesTree(page, -200);

    await page.waitForTimeout(1000);

    // create projects page
    if (runTests.includes(1)) {

        await createAndRenamePage(page, 4, 'projects');
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
        await page.waitForTimeout(wait);

        // build repeater config for projects div
        let repeaterDiv = pageContainer.getElement('div', 1);
        await useContextMenu(page, repeaterDiv, 'Insert content element', '#content');
        let content = repeaterDiv.getElement('#text');
        await setNodeContent(page, content, '${project.name}')

        await configureFunctionQuery(page, repeaterDiv, 'find(\'Project\')', 'project');

        // build event action mapping for form
        await page.locator('span').filter({hasText: 'form'}).click();
        await page.waitForTimeout(wait);
        await page.getByRole('link', {name: 'HTML'}).click();
        await page.waitForTimeout(wait);
        await page.locator('input[name="_html_method"]').fill('post');
        await page.waitForTimeout(wait);
        await page.getByRole('link', {name: 'General'}).click();
        await page.locator('input[name="_html_id"]').fill('create-project-form');
        await page.waitForTimeout(wait);
        await page.getByRole('link', {name: 'Events'}).click();
        await page.waitForTimeout(wait);
        await page.getByRole('textbox', {name: 'Browser event (click, keydown'}).fill('submit');
        await page.waitForTimeout(wait);
        await page.keyboard.press('Tab');
        await page.waitForTimeout(wait);
        await page.locator('#action-select').selectOption('Create new object');
        await page.waitForTimeout(wait);
        await page.getByRole('textbox', {name: 'Custom type or script'}).fill('Project');
        await page.waitForTimeout(wait);
        await page.keyboard.press('Tab');
        await page.waitForTimeout(wait);
        await page.getByRole('img', {name: 'Add parameter', exact: true}).click();
        await page.waitForTimeout(wait);
        await page.locator('input.parameter-name-input').first().fill('name');
        await page.waitForTimeout(wait);
        await page.getByRole('combobox').nth(5).selectOption('User Input');
        await page.waitForTimeout(wait);
        await page.locator('span').filter({hasText: /^input$/}).hover();
        await page.waitForTimeout(wait);
        await page.mouse.down();
        await page.waitForTimeout(wait);
        await page.getByText('Drag and drop existing form').first().hover();
        await page.waitForTimeout(wait);
        await page.mouse.up();
        await page.getByLabel('Behaviour on success Define').selectOption('Navigate to a new page');
        await page.keyboard.press('Tab');
        await page.waitForTimeout(wait);
        await page.getByRole('textbox', {name: 'Success URL'}).fill('/project/{result.id}');
        await page.keyboard.press('Tab');
        await page.waitForTimeout(2000);
        await page.screenshot({path: 'screenshots/pages_create-form_event-action-mapping-configuration.png'});

        await page.reload();

        // click on a different element to disable highlighting for the element we want to screenshot
        await page.locator('span').filter({hasText: 'body'}).click();
        await page.waitForTimeout(wait);
        await page.locator('span').filter({hasText: 'script'}).hover();
        await page.waitForTimeout(2000);

        // take a screenshot of the form element
        await page.locator('div.node:has(b[title="form"])').nth(4).screenshot({path: 'screenshots/pages_create-form-element.png'});

        await collapsePageTree(page, 'projects');
    }

    // create project page
    if (runTests.includes(2)) {

        await createAndRenamePage(page, 4, 'project');
        await expandPageTree(page, 'project');
        await insertFrontendJs(page, 'project');

        let pageContainer = getPageContainer(page, 'project');

        // remove Initial body text
        let removeNode = pageContainer.getElement('Initial body text');
        await useContextMenu(page, removeNode, 'Remove Node');

        // change title
        await page.locator('span').filter({hasText: '${capitalize(page.name)}'}).nth(2).click();
        await page.waitForTimeout(wait);
        await page.keyboard.down('Control');
        await page.keyboard.press('A');
        await page.keyboard.up('Control');
        await page.keyboard.type('Edit Project "${current.name}"');
        await page.waitForTimeout(200);
        await page.getByRole('button', {name: 'Save'}).click();
        await page.waitForTimeout(wait);

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
        await page.waitForTimeout(wait);

        await page.waitForTimeout(5000);
        await page.screenshot({path: 'screenshots/pages_edit-form_input-configuration.png'});

        // insert button element
        await useContextMenu(page, formContainer, 'Insert HTML element', 'b', 'button');
        let buttonContainer = formContainer.getElement('Initial text for button');
        await setNodeContent(page, buttonContainer, 'Save Project');
        await page.waitForTimeout(wait);


        // build event action mapping for form
        await page.locator('span').filter({hasText: 'form'}).click();
        await page.waitForTimeout(wait);
        await page.getByRole('link', {name: 'HTML'}).click();
        await page.waitForTimeout(wait);
        await page.locator('input[name="_html_method"]').fill('post');
        await page.waitForTimeout(wait);
        await page.getByRole('link', {name: 'General'}).click();
        await page.locator('input[name="_html_id"]').fill('save-project-form');
        await page.waitForTimeout(wait);
        await page.getByRole('link', {name: 'Events'}).click();
        await page.waitForTimeout(wait);
        await page.getByRole('textbox', {name: 'Browser event (click, keydown'}).fill('submit');
        await page.keyboard.press('Tab');
        await page.waitForTimeout(wait);
        await page.locator('#action-select').selectOption('Update object');
        await page.waitForTimeout(wait);
        await page.getByRole('textbox', {name: 'Custom type or script'}).fill('Project');
        await page.waitForTimeout(wait);
        await page.keyboard.press('Tab');
        await page.waitForTimeout(wait);
        await page.locator('#id-expression-input').fill('${current.id}');
        await page.waitForTimeout(wait);
        await page.keyboard.press('Tab');
        await page.waitForTimeout(wait);
        await page.getByRole('img', {name: 'Add parameter', exact: true}).click();
        await page.waitForTimeout(wait);
        await page.getByRole('img', {name: 'Add parameter', exact: true}).click();
        await page.waitForTimeout(wait);
        await page.getByRole('img', {name: 'Add parameter', exact: true}).click();
        await page.waitForTimeout(wait);
        await page.getByRole('textbox', {name: 'Name'}).nth(2).fill('name');
        await page.waitForTimeout(wait);
        await page.getByRole('combobox').nth(5).selectOption('User Input');
        await page.waitForTimeout(wait);
        await page.getByRole('textbox', {name: 'Name'}).nth(3).fill('description');
        await page.waitForTimeout(wait);
        await page.getByRole('combobox').nth(6).selectOption('User Input');
        await page.waitForTimeout(wait);
        await page.getByRole('textbox', {name: 'Name'}).nth(4).fill('dueDate');
        await page.waitForTimeout(wait);
        await page.getByRole('combobox').nth(7).selectOption('User Input');
        await page.waitForTimeout(wait);

        // drag input1 to dropzone1
        await page.locator('span').filter({hasText: 'input'}).first().hover();
        await page.waitForTimeout(wait);
        await page.mouse.down();
        await page.waitForTimeout(wait);
        await page.getByText('Drag and drop existing form').first().hover();
        await page.waitForTimeout(wait);
        await page.mouse.up();

        // drag input2 to dropzone2
        await page.locator('span').filter({hasText: 'input'}).nth(1).hover();
        await page.waitForTimeout(wait);
        await page.mouse.down();
        await page.waitForTimeout(wait);
        await page.getByText('Drag and drop existing form').nth(1).hover();
        await page.waitForTimeout(wait);
        await page.mouse.up();

        // drag input3 to dropzone3
        await page.locator('span').filter({hasText: 'input'}).nth(2).hover();
        await page.waitForTimeout(wait);
        await page.mouse.down();
        await page.waitForTimeout(wait);
        await page.getByText('Drag and drop existing form').nth(2).hover();
        await page.waitForTimeout(wait);
        await page.mouse.up();

        await page.getByLabel('Behaviour on success Define').selectOption('Reload the current page');
        await page.keyboard.press('Tab');
        await page.waitForTimeout(5000);
        await page.screenshot({path: 'screenshots/pages_edit-form_event-action-mapping-configuration.png'});
        await page.waitForTimeout(2000);

        // reload to un-select the form element
        await page.reload();

        // click on a different element to disable highlighting for the element we want to screenshot
        await page.locator('span').filter({hasText: 'form#save-project-form'}).click();
        await page.waitForTimeout(wait);
        await page.locator('span').filter({hasText: 'body'}).click();
        await page.waitForTimeout(wait);
        await page.locator('span').filter({hasText: 'head'}).hover();
        await page.waitForTimeout(2000);

        // take a screenshot of the form element
        await page.locator('div.node:has(b[title="form"])').nth(4).screenshot({path: 'screenshots/pages_edit-form-element.png'});

        await page.goto(process.env.BASE_URL + '/projects');

        await page.locator('input[name="name"]').fill('Project #1');
        await page.waitForTimeout(wait);
        await page.locator('button').click();
        await page.waitForTimeout(1000);

        await expect(page.locator('h1')).toHaveText('Edit Project "Project #1"');

        await page.goto(process.env.BASE_URL + '/structr/');
        await expect(page).toHaveTitle(/Structr/);

        await collapsePageTree(page, 'project');
    }

    // next part: advanced example
    if (runTests.includes(3)) {
        await createAndRenamePage(page, 4, 'advanced');
        await expandPageTree(page, 'advanced');
        await insertFrontendJs(page, 'advanced');

        let pageContainer = getPageContainer(page, 'advanced');

        // remove Initial body text
        let removeNode = pageContainer.getElement('Initial body text');
        await useContextMenu(page, removeNode, 'Remove Node');

        // change title
        await page.locator('span').filter({hasText: '${capitalize(page.name)}'}).nth(2).click();
        await page.waitForTimeout(wait);
        await page.keyboard.down('Control');
        await page.keyboard.press('A');
        await page.keyboard.up('Control');
        await page.keyboard.type('Edit Project "${current.name}"');
        await page.waitForTimeout(200);
        await page.getByRole('button', {name: 'Save'}).click();
        await page.waitForTimeout(wait);

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
                await page.waitForTimeout(5000);
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
        await page.waitForTimeout(wait);


        // build event action mapping for form
        await page.locator('span').filter({hasText: 'form'}).click();
        await page.waitForTimeout(wait);
        await page.getByRole('link', {name: 'HTML'}).click();
        await page.waitForTimeout(wait);
        await page.locator('input[name="_html_method"]').fill('post');
        await page.waitForTimeout(wait);
        await page.getByRole('link', {name: 'General'}).click();
        await page.locator('input[name="_html_id"]').fill('save-project-form');
        await page.waitForTimeout(wait);
        await page.getByRole('link', {name: 'Events'}).click();
        await page.waitForTimeout(wait);
        await page.getByRole('textbox', {name: 'Browser event (click, keydown'}).fill('submit');
        await page.keyboard.press('Tab');
        await page.waitForTimeout(wait);
        await page.locator('#action-select').selectOption('Update object');
        await page.waitForTimeout(wait);
        await page.getByRole('textbox', {name: 'Custom type or script'}).fill('Project');
        await page.waitForTimeout(wait);
        await page.keyboard.press('Tab');
        await page.waitForTimeout(wait);
        await page.locator('#id-expression-input').fill('${current.id}');
        await page.waitForTimeout(wait);
        await page.keyboard.press('Tab');
        await page.waitForTimeout(wait);
        await page.getByRole('img', {name: 'Add parameter', exact: true}).click();
        await page.waitForTimeout(wait);
        await page.getByRole('img', {name: 'Add parameter', exact: true}).click();
        await page.waitForTimeout(wait);
        await page.getByRole('img', {name: 'Add parameter', exact: true}).click();
        await page.waitForTimeout(wait);
        await page.getByRole('img', {name: 'Add parameter', exact: true}).click();
        await page.waitForTimeout(wait);
        await page.getByRole('textbox', {name: 'Name'}).nth(2).fill('manager');
        await page.waitForTimeout(wait);
        await page.getByRole('combobox').nth(5).selectOption('User Input');
        await page.waitForTimeout(wait);
        await page.getByRole('textbox', {name: 'Name'}).nth(3).fill('client');
        await page.waitForTimeout(wait);
        await page.getByRole('combobox').nth(6).selectOption('User Input');
        await page.waitForTimeout(wait);
        await page.getByRole('textbox', {name: 'Name'}).nth(4).fill('tags');
        await page.waitForTimeout(wait);
        await page.getByRole('combobox').nth(7).selectOption('User Input');
        await page.waitForTimeout(wait);
        await page.getByRole('textbox', {name: 'Name'}).nth(5).fill('tasks');
        await page.waitForTimeout(wait);
        await page.getByRole('combobox').nth(8).selectOption('User Input');
        await page.waitForTimeout(wait);

        // drag inputs to dropzone
        for (var i=0; i<4; i++) {

            await page.locator('span').filter({hasText: 'select'}).nth(i).hover();
            await page.waitForTimeout(wait);
            await page.mouse.down();
            await page.waitForTimeout(wait);
            await page.getByText('Drag and drop existing form').nth(i).hover();
            await page.waitForTimeout(wait);
            await page.mouse.up();
        }

        await page.getByLabel('Behaviour on success Define').selectOption('Reload the current page');
        await page.keyboard.press('Tab');
        await page.waitForTimeout(5000);
        await page.screenshot({path: 'screenshots/pages_advanced-form_event-action-mapping-configuration.png'});
        await page.waitForTimeout(2000);

        // reload to un-select the form element
        await page.reload();

        // click on head element to deselect form for screenshot below
        let h = pageContainer.getElement('head');
        await h.getTextNode().hover();
        await h.getTextNode().click();

        await page.waitForTimeout(1000);

        // take a screenshot of the form element (background change is necessary because playwright tries to scroll the element into view, which hovers it apparently)
        await formContainer.locator.screenshot({path: 'screenshots/pages_advanced-form-element.png', style: '.nodeHover { background-color: transparent; }' });

    }

    await logout(page);

});























