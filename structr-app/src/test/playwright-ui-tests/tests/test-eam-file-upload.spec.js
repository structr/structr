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
import {login} from './helpers/auth';
import {initialize} from './helpers/init';

/**
 * Native file upload through a wired EAM form (frontend.js uploadFormFiles/uploadFile).
 *
 * EAM parameters are JSON and cannot carry file bytes, so the runtime uploads each selected
 * file to the UploadServlet first and passes the new file's uuid to the action, where the
 * server links it like any other node reference. This spec covers the MULTI-FILE path:
 *
 *  1. several files in ONE input[multiple]  -> an ARRAY of uuids, linked to a to-many relation
 *  2. several file INPUTS in one form       -> one payload key per input name
 *  3. a single file in a plain input        -> a SCALAR uuid, linked to a to-ONE relation
 *  4. no file selected                      -> no upload request at all
 *
 * Cardinality is the authoring rule this pins down: the runtime sends an array for an
 * input[multiple] and the bare uuid otherwise, and the server accepts a collection only for a
 * to-many property ("expected a JSON collection", 422). So a to-many target needs the multiple
 * attribute, and a to-one target must not have it. The cases are therefore written against
 * built-in properties of both kinds: Folder.files (to-many) and User.img (to-one, an Image),
 * which keeps the spec free of custom schema.
 *
 * The page is built over REST (rather than by driving the Pages UI) to keep the spec about the
 * upload runtime; the wired element is the FORM with a submit event, which is the only shape
 * for which frontend.js collects form fields at all (see resolveData).
 *
 * Case 2 asserts the PAYLOAD only: a Folder has no second file-valued property to write, so
 * whether the server accepts the extra key is irrelevant there (the request has already been
 * captured either way). Cases 1 and 3 additionally assert that the uuids reached the graph.
 */

let context;
let multiFolderId;
let secondInputFolderId;
let adminUserId;

const FRONTEND_JS = '/structr/js/frontend/frontend.js';

const fileOf = (name, body) => ({ name: name, mimeType: 'text/plain', buffer: Buffer.from(body) });

/**
 * A real 1x1 PNG. The scalar case writes User.img, which is a to-one relation to Image, and the
 * UploadServlet picks the node type from the content type, so this has to be an actual image:
 * a text file would be stored as a File and could not be linked there.
 */
const imageOf = (name) => ({
	name:     name,
	mimeType: 'image/png',
	buffer:   Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC', 'base64')
});

/** POST one node and return its uuid. */
async function createNode(type, data) {

	const response = await context.post(process.env.BASE_URL + `/structr/rest/${type}`, { data: JSON.stringify(data) });

	if (!response.ok()) {
		throw new Error(`creating ${type} failed with status ${response.status()}: ${await response.text()}`);
	}

	return (await response.json()).result[0];
}

/**
 * POST one HTML element and return its uuid. The tag has to be given explicitly: creating an
 * element type over REST does NOT derive it from the type, because the Pages user interface
 * creates elements through createElement(tag) instead. An element whose tag is null renders
 * as nothing at all, itself and its whole subtree, so the page would come out empty.
 */
async function createElement(type, data) {

	return await createNode(type, { tag: type.toLowerCase(), ...data });
}

/** The names of the files currently linked to a folder's built-in "files" relation. */
async function linkedFileNames(folderId) {

	const response = await context.get(process.env.BASE_URL + `/structr/rest/Folder/${folderId}/all`);
	expect(response.ok()).toBeTruthy();

	const files = (await response.json()).result.files || [];

	return files.map(file => file.name).sort();
}

/** The name of the image linked to a user's built-in to-one "img" relation, or null. */
async function linkedImageName(userId) {

	const response = await context.get(process.env.BASE_URL + `/structr/rest/User/${userId}/all`);
	expect(response.ok()).toBeTruthy();

	const img = (await response.json()).result.img;

	return img ? img.name : null;
}

/** The uuid of the user created by initialize(). */
async function adminUser() {

	const response = await context.get(process.env.BASE_URL + '/structr/rest/User?name=admin');
	expect(response.ok()).toBeTruthy();

	return (await response.json()).result[0].id;
}

/** A file input inside the given form. */
async function addFileInput(pageId, formId, name, multiple) {

	const data = { pageId: pageId, parent: formId, _html_type: 'file', _html_name: name };

	if (multiple) {
		data._html_multiple = 'multiple';
	}

	return await createElement('Input', data);
}

/**
 * A form wired to update the given folder, holding a submit button. Returns the form id so
 * the caller can add its file inputs.
 */
async function addWiredForm(pageId, bodyId, formHtmlId, folderId) {

	const formId = await createElement('Form', { pageId: pageId, parent: bodyId, _html_id: formHtmlId });

	await createElement('Button', { pageId: pageId, parent: formId, _html_type: 'submit', _html_id: `${formHtmlId}-submit` });

	// the wired element is the FORM, triggered by "submit": frontend.js only collects a form's
	// fields (and therefore its files) when the event target IS the form
	await createNode('ActionMapping', {
		event:           'submit',
		action:          'update',
		idExpression:    folderId,
		triggerElements: [ formId ]
	});

	return formId;
}

test.beforeAll(async ({ playwright }) => {

	context = await initialize(playwright, null);

	multiFolderId       = await createNode('Folder', { name: 'multi-upload-target' });
	secondInputFolderId = await createNode('Folder', { name: 'second-input-target' });
	adminUserId         = await adminUser();

	const pageId = await createNode('Page', { name: 'upload-test' });
	const htmlId = await createElement('Html',  { pageId: pageId, parent: pageId });
	const headId = await createElement('Head',  { pageId: pageId, parent: htmlId });

	// the events runtime that performs the upload and sends the action
	await createElement('Script', { pageId: pageId, parent: headId, _html_type: 'module', _html_src: FRONTEND_JS });

	const bodyId = await createElement('Body', { pageId: pageId, parent: htmlId });

	// form 1: one multi-file input
	const multiFormId = await addWiredForm(pageId, bodyId, 'multi-form', multiFolderId);
	await addFileInput(pageId, multiFormId, 'files', true);

	// form 2: a single plain file input on a to-ONE target, to pin the scalar (non-array) shape
	const scalarFormId = await addWiredForm(pageId, bodyId, 'scalar-form', adminUserId);
	await addFileInput(pageId, scalarFormId, 'img', false);

	// form 3: TWO file inputs, to pin one payload key per input name
	const twoInputFormId = await addWiredForm(pageId, bodyId, 'two-input-form', secondInputFolderId);
	await addFileInput(pageId, twoInputFormId, 'files', true);
	await addFileInput(pageId, twoInputFormId, 'cover', false);
});

test('multiple files in one input are uploaded and linked as an array', async ({ page }) => {

	// sign in first: the frontend page, the uploads and the action all run in this session
	await login(page);

	await page.goto(process.env.BASE_URL + '/upload-test');
	await expect(page.locator('form#multi-form input[type="file"]')).toHaveCount(1);

	await page.locator('form#multi-form input[name="files"]').setInputFiles([
		fileOf('multi-a.txt', 'first file'),
		fileOf('multi-b.txt', 'second file')
	]);

	// capture the action request to assert the payload shape frontend.js produced
	const actionRequest = page.waitForRequest(request => request.url().includes('/event') && request.method() === 'POST');

	await page.locator('#multi-form-submit').click();

	const payload = (await actionRequest).postDataJSON();

	// the multi-file input must send an ARRAY of uuids, one per selected file
	expect(Array.isArray(payload.files)).toBeTruthy();
	expect(payload.files).toHaveLength(2);
	for (const uuid of payload.files) {
		expect(uuid).toMatch(/^[0-9a-f]{32}$/);
	}
	expect(new Set(payload.files).size).toBe(2);

	// both files were really stored, and the array survived into the to-many relation
	await expect.poll(() => linkedFileNames(multiFolderId), { timeout: 15_000 })
		.toEqual([ 'multi-a.txt', 'multi-b.txt' ]);
});

test('a single file in a plain input is sent as a scalar uuid', async ({ page }) => {

	await login(page);

	await page.goto(process.env.BASE_URL + '/upload-test');

	await page.locator('form#scalar-form input[name="img"]').setInputFiles([ imageOf('avatar.png') ]);

	const actionRequest = page.waitForRequest(request => request.url().includes('/event') && request.method() === 'POST');

	await page.locator('#scalar-form-submit').click();

	const payload = (await actionRequest).postDataJSON();

	// an input WITHOUT the multiple attribute sends the uuid itself, not a one-element array
	expect(Array.isArray(payload.img)).toBeFalsy();
	expect(payload.img).toMatch(/^[0-9a-f]{32}$/);

	// and the bare uuid is what the to-one relation needs: a collection would be rejected
	await expect.poll(() => linkedImageName(adminUserId), { timeout: 15_000 })
		.toEqual('avatar.png');
});

test('two file inputs in one form each get their own payload key', async ({ page }) => {

	await login(page);

	await page.goto(process.env.BASE_URL + '/upload-test');

	await page.locator('form#two-input-form input[name="files"]').setInputFiles([
		fileOf('two-a.txt', 'multi input file'),
		fileOf('two-b.txt', 'multi input file 2')
	]);
	await page.locator('form#two-input-form input[name="cover"]').setInputFiles([ fileOf('cover.txt', 'cover file') ]);

	const actionRequest = page.waitForRequest(request => request.url().includes('/event') && request.method() === 'POST');

	await page.locator('#two-input-form-submit').click();

	const payload = (await actionRequest).postDataJSON();

	// each input contributes its OWN key, keeping its own array/scalar shape
	expect(payload.files).toHaveLength(2);
	expect(Array.isArray(payload.cover)).toBeFalsy();
	expect(payload.cover).toMatch(/^[0-9a-f]{32}$/);

	// three distinct uploads, so no uuid is reused across inputs
	expect(new Set([ ...payload.files, payload.cover ]).size).toBe(3);
});

test('a form with no file selected sends no upload and no file key', async ({ page }) => {

	await login(page);

	await page.goto(process.env.BASE_URL + '/upload-test');

	let uploadCalls = 0;
	page.on('request', request => {
		if (request.url().includes('/structr/upload')) {
			uploadCalls++;
		}
	});

	const actionRequest = page.waitForRequest(request => request.url().includes('/event') && request.method() === 'POST');

	await page.locator('#multi-form-submit').click();

	const payload = (await actionRequest).postDataJSON();

	// nothing selected: the upload step is skipped entirely and the action keeps its normal payload
	expect(uploadCalls).toBe(0);
	expect(payload.files).toBeFalsy();
});
