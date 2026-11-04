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
import {goToModule} from './helpers/init';
import {login} from './helpers/auth';
import {EamBuilder} from './helpers/eam';

// The Application Map of a small project management application, with the screenshots for its chapter of the
// documentation (screenshots/appmap*.png, see copy-screenshots-to-docs.sh).

test.beforeAll(async ({playwright}) => {

	const builder = await EamBuilder.create(playwright);

	// the data model: projects with milestones and tasks, tasks assigned to employees
	const project   = await builder.schemaType('Project', [{ name: 'description', propertyType: 'String' }], [
		{ name: 'addMilestone', source: "{\n\tconst milestone = $.create('Milestone', { name: $.arguments.name });\n\tmilestone.project = $.this;\n\treturn milestone;\n}" },
		{ name: 'progress',     source: "{\n\tconst tasks = $.find('Task', { project: $.this });\n\treturn tasks.filter(t => t.status === 'done').length / tasks.length;\n}" }
	]);
	const milestone = await builder.schemaType('Milestone', [{ name: 'dueDate', propertyType: 'Date' }]);
	const task      = await builder.schemaType('Task', [{ name: 'status', propertyType: 'String' }], [
		{ name: 'complete', source: "{\n\t$.this.status = 'done';\n\t$.notifyAssignee({ task: $.this });\n}" },
		{ name: 'reassign', source: "{\n\t$.this.assignee = $.find('Employee', $.arguments.employee);\n\t$.notifyAssignee({ task: $.this });\n}" }
	]);
	const employee  = await builder.schemaType('Employee', [{ name: 'eMail', propertyType: 'String' }]);

	const relate = (source, target, type, sourceName, targetName, sourceMultiplicity, targetMultiplicity) => builder.node('SchemaRelationshipNode', {
		sourceNode: source, targetNode: target, relationshipType: type, sourceJsonName: sourceName, targetJsonName: targetName, sourceMultiplicity, targetMultiplicity
	});

	await relate(project, milestone, 'HAS',      'project',  'milestones', '1', '*');
	await relate(project, task,      'HAS',      'project',  'tasks',      '1', '*');
	await relate(milestone, task,    'INCLUDES', 'milestone', 'tasks',     '1', '*');
	await relate(employee, task,     'WORKS_ON', 'assignee', 'tasks',      '1', '*');

	// a service class and the functions that reach beyond Structr: mail and a web service
	await builder.node('SchemaNode', { name: 'ProjectService', isServiceClass: true, schemaMethods: [
		{ name: 'archiveProject', isStatic: true, source: "{\n\tconst project = $.find('Project', $.arguments.id);\n\t$.delete(project.tasks);\n\t$.delete(project);\n}" }
	]});

	await builder.userFunction('notifyAssignee', "{\n\tconst task = $.arguments.task;\n\t$.sendPlaintextMail('projects@example.com', 'Projects', task.assignee.eMail, task.assignee.name, 'Task ' + task.name, 'The task is ' + task.status + '.');\n}");
	await builder.userFunction('importHolidays', "{\n\tconst holidays = JSON.parse($.GET('https://date.nager.at/api/v3/PublicHolidays/2026/DE'));\n\tfor (const day of holidays) {\n\t\t$.create('Milestone', { name: day.localName, dueDate: day.date });\n\t}\n}");
	await builder.userFunction('openTasks', "{\n\treturn $.find('Task', { status: 'open' });\n}");

	// the pages: a public start page, the projects with their tasks, and an error page
	const start = await builder.page('welcome', { publicVisible: true });

	await builder.update('Page', start.pageId, { position: 0 });
	await builder.text(start.pageId, await builder.element(start.pageId, start.bodyId, 'H1'), 'Projects');

	const projects = await builder.page('projects');
	const list     = await builder.element(projects.pageId, projects.bodyId, 'Ul');
	const row      = await builder.element(projects.pageId, list, 'Li', { functionQuery: "find('Project')", dataKey: 'project' });

	await builder.text(projects.pageId, row, '${project.name}');

	const details = await builder.page('project');
	const tasks   = await builder.element(details.pageId, details.bodyId, 'Ul');
	const item    = await builder.element(details.pageId, tasks, 'Li', { functionQuery: "find('Task', { status: 'open' })", dataKey: 'task' });
	const done    = await builder.element(details.pageId, item, 'Button');

	await builder.text(details.pageId, item, '${task.name}');
	await builder.text(details.pageId, done, 'Done');
	await builder.actionMapping(done, { event: 'click', action: 'method', method: 'complete', idExpression: '${task.id}' });

	const form   = await builder.element(details.pageId, details.bodyId, 'Form');
	const name   = await builder.element(details.pageId, form, 'Input', { _html_name: 'name' });
	const create = await builder.element(details.pageId, form, 'Button');

	await builder.text(details.pageId, create, 'Add task');

	const add = await builder.actionMapping(create, { event: 'click', action: 'create', dataType: 'Task' });

	await builder.parameter(add, { parameterType: 'user-input', parameterName: 'name', inputElement: name });

	const missing = await builder.page('not-found', { publicVisible: true });

	await builder.update('Page', missing.pageId, { showOnErrorCodes: '404' });

	// who may do what over REST
	for (const [signature, flags] of [['Project', 17], ['Task', 5], ['Project/addMilestone', 4], ['_login', 64]]) {
		await builder.node('ResourceAccess', { signature, flags });
	}

	await builder.node('Group', { name: 'Project Managers' });

	// some use, for the activity lens
	await new Promise(resolve => setTimeout(resolve, 1000));

	for (let i = 0; i < 12; i++) {
		await builder.context.get(process.env.BASE_URL + '/structr/rest/Project');
	}

	for (let i = 0; i < 4; i++) {
		await builder.context.get(process.env.BASE_URL + '/structr/rest/Task');
	}
});

// ----- the map, asked from inside the page -----
// where an element is on the screen
async function screenOf(page, name) {

	return page.evaluate(name => {

		const view = _AppMap.view;
		const n    = view.nodes.find(n => n.n === name);
		const box  = view.canvas.getBoundingClientRect();

		return { x: box.left + view.transform.applyX(n.x), y: box.top + view.transform.applyY(n.y) };
	}, name);
}

async function settle(page) {

	await page.waitForFunction(() => _AppMap.view?.layout && !_AppMap.view.layout.settling && _AppMap.view.strands.bundled(), null, { timeout: 60_000 });
	await page.waitForTimeout(500);
}

async function overview(page) {

	await page.evaluate(() => _AppMap.view.fitAll());
	await page.mouse.move(5, 300);
	await page.waitForTimeout(500);
}

test('application map', async ({page}) => {

	await login(page);
	await goToModule(page, '#appmap_');
	await settle(page);

	// a first visit opens the introduction
	await expect(page.locator('#appmap-intro')).toBeVisible();
	await page.screenshot({ path: 'screenshots/appmap_introduction.png' });

	await page.locator('#appmap-intro-close').click();
	await overview(page);
	await page.screenshot({ path: 'screenshots/appmap.png' });

	// the pointer on a type shows what it connects to
	const taskType = await screenOf(page, 'Task');

	await page.mouse.move(taskType.x, taskType.y);
	await page.waitForTimeout(500);
	await page.screenshot({ path: 'screenshots/appmap_hover.png' });

	// a click holds it, the names at the ends of its relations lead on
	await page.mouse.click(taskType.x, taskType.y);
	await page.mouse.move(5, 300);
	await page.waitForTimeout(800);
	await page.screenshot({ path: 'screenshots/appmap_explore.png' });

	await page.keyboard.press('Escape');

	// a double click opens a page
	await overview(page);

	const projectPage = await screenOf(page, 'project');

	await page.mouse.dblclick(projectPage.x, projectPage.y);
	await page.waitForTimeout(1500);
	await page.mouse.move(5, 300);
	await page.waitForTimeout(500);
	await page.screenshot({ path: 'screenshots/appmap_page.png' });

	// and a method, whose code a click makes editable
	await overview(page);

	const complete = await screenOf(page, 'Task.complete');

	await page.mouse.dblclick(complete.x, complete.y);
	await page.waitForTimeout(1500);
	await page.waitForFunction(() => [..._AppMap.view.panels.reading].some(n => n.n === 'Task.complete'));
	await page.mouse.move(5, 300);
	await page.waitForTimeout(500);
	await page.screenshot({ path: 'screenshots/appmap_method.png' });

	const field = await page.evaluate(() => {

		const view             = _AppMap.view;
		const n                = view.nodes.find(n => n.n === 'Task.complete');
		const t                = view.transform;
		const box              = view.canvas.getBoundingClientRect();
		const [fx, fy, fw, fh] = AppMapPanels.codeField(view.panels.codePanel(n, t.k));

		return { x: box.left + t.applyX(fx + fw / 2), y: box.top + t.applyY(fy + fh / 2) };
	});

	await page.mouse.click(field.x, field.y);
	await expect(page.locator('#appmap-canvas .appmap-editor .monaco-editor')).toBeVisible();
	await page.waitForTimeout(500);
	await page.screenshot({ path: 'screenshots/appmap_editor.png' });

	await page.keyboard.press('Escape');
	await expect(page.locator('#appmap-canvas .appmap-editor')).toHaveCount(0);

	// what public users see and may do
	await overview(page);
	await page.locator('#appmap-lens').selectOption('public');
	await page.waitForTimeout(500);
	await page.screenshot({ path: 'screenshots/appmap_access.png' });

	await page.locator('#appmap-lens').selectOption('');

	// how much the application is used, and its hotspots
	await page.locator('#appmap-activity').click();
	await page.waitForFunction(() => _AppMap.view.activity?.heat?.size > 0);
	await page.waitForTimeout(500);
	await page.screenshot({ path: 'screenshots/appmap_usage.png' });

	await page.locator('#appmap-spotlight').hover();
	await page.waitForTimeout(500);
	await page.screenshot({ path: 'screenshots/appmap_hotspots.png' });

	await page.mouse.move(5, 300);
	await page.locator('#appmap-activity').click();

	// the dark look
	await page.locator('[data-look="dark"]').click();
	await page.waitForTimeout(500);
	await page.screenshot({ path: 'screenshots/appmap_dark.png' });

	await page.locator('[data-look="light"]').click();
});
