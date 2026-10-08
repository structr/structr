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
import {EamBuilder, nextActionRequest, nextActionResponse, openPage, UUID_PATTERN} from './helpers/eam';

/**
 * Event Action Mapping: the user and session actions (sign-in, sign-out, sign-up, reset-password)
 * and the access of anonymous visitors.
 *
 * Every page here is public and every test runs as an anonymous visitor in its own browser context,
 * so a session established by one test never leaks into the next. Each page shows who the server
 * thinks the visitor is in #current-user, which is how the tests observe the session.
 *
 * Not covered: two-factor sign-in (the 202 redirect needs the two-factor level setting, which cannot
 * be changed over REST), and sign-up or reset-password for an existing account (both send a mail, and
 * the outcome depends on the mail configuration of the test server).
 */

let builder;
let grantId;

// anonymous visitors need NON_AUTH_USER_POST (64) to send an action, signed-in users AUTH_USER_POST (4)
const EVENT_SIGNATURE         = 'DOMElement/_id/event';
const AUTH_USER_POST          = 4;
const NON_AUTH_USER_POST      = 64;
const STANDARD_LOGIN_ERROR    = 'Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!';
const NO_EMAIL_ADDRESS_ERROR  = 'No e-mail address given.';

const users = {
	signIn:        { name: 'signin-user',          password: 'signin-password-1' },
	signInByMail:  { name: 'mail-user',            password: 'mail-password-1', eMail: 'mail-user@example.com' },
	wrongPassword: { name: 'wrong-password-user',  password: 'wrong-password-user-1' },
	noGrant:       { name: 'no-grant-user',        password: 'no-grant-password-1' },
	signOut:       { name: 'signout-user',         password: 'signout-password-1' },
	signOutJson:   { name: 'signout-json-user',    password: 'signout-json-password-1' }
};

/** A public page with the #current-user label, ready for inputs and a trigger button. */
async function authPage(name) {

	const { pageId, bodyId } = await builder.page(name, { publicVisible: true });
	const labelId            = await builder.element(pageId, bodyId, 'Span', { _html_id: 'current-user' });

	await builder.text(pageId, labelId, `\${if(empty(me), 'anonymous', me.name)}`);

	return { pageId, bodyId };
}

async function addInput(pageId, parentId, htmlId, type) {

	return await builder.element(pageId, parentId, 'Input', { _html_id: htmlId, _html_type: type });
}

async function addButton(pageId, parentId, htmlId, label) {

	const buttonId = await builder.element(pageId, parentId, 'Button', { _html_id: htmlId });

	await builder.text(pageId, buttonId, label);

	return buttonId;
}

/** Inline notifications that stay visible, so the tests can read them without racing the timeout. */
function inlineNotifications(successText, failureText) {

	return {
		successNotifications:      'inline-text-message',
		successNotificationsText:  successText,
		successNotificationsDelay: -1,
		failureNotifications:      'inline-text-message',
		failureNotificationsText:  failureText,
		failureNotificationsDelay: -1
	};
}

/** Signs the visitor in through the sign-in page, the way a user would. */
async function signIn(page, user) {

	await openPage(page, 'auth-sign-in');

	await page.locator('#signin-name').fill(user.name);
	await page.locator('#signin-password').fill(user.password);

	const response = nextActionResponse(page);

	await page.locator('#signin-button').click();

	expect((await response).status()).toBe(200);
}

/** Counts browser dialogs (alert, confirm) and dismisses them, so an unexpected one cannot block the test. */
function countDialogs(page) {

	const counter = { count: 0 };

	page.on('dialog', async dialog => {
		counter.count++;
		await dialog.dismiss();
	});

	return counter;
}

test.beforeAll(async ({ playwright }) => {

	builder = await EamBuilder.create(playwright);

	grantId = await builder.node('ResourceAccess', {
		signature:                   EVENT_SIGNATURE,
		flags:                       AUTH_USER_POST | NON_AUTH_USER_POST,
		visibleToPublicUsers:        true,
		visibleToAuthenticatedUsers: true
	});

	for (const user of Object.values(users)) {
		await builder.node('User', user);
	}

	// sign-in by name, result shown in the success notification, no follow-up
	{
		const { pageId, bodyId } = await authPage('auth-sign-in');
		const nameId             = await addInput(pageId, bodyId, 'signin-name', 'text');
		const passwordId         = await addInput(pageId, bodyId, 'signin-password', 'password');
		const buttonId           = await addButton(pageId, bodyId, 'signin-button', 'Sign in');

		const actionMappingId = await builder.actionMapping(buttonId, {
			event:  'click',
			action: 'sign-in',
			...inlineNotifications('Signed in as {result.name}', 'Sign-in failed ({status}): {message}')
		});

		await builder.parameter(actionMappingId, { parameterType: 'user-input', parameterName: 'name', inputElement: nameId });
		await builder.parameter(actionMappingId, { parameterType: 'user-input', parameterName: 'password', inputElement: passwordId });
	}

	// sign-in by e-mail address, followed by a full page reload
	{
		const { pageId, bodyId } = await authPage('auth-sign-in-email');
		const mailId             = await addInput(pageId, bodyId, 'signin-email', 'text');
		const passwordId         = await addInput(pageId, bodyId, 'signin-email-password', 'password');
		const buttonId           = await addButton(pageId, bodyId, 'signin-email-button', 'Sign in');

		const actionMappingId = await builder.actionMapping(buttonId, {
			event:            'click',
			action:           'sign-in',
			successBehaviour: 'full-page-reload'
		});

		await builder.parameter(actionMappingId, { parameterType: 'user-input', parameterName: 'eMail', inputElement: mailId });
		await builder.parameter(actionMappingId, { parameterType: 'user-input', parameterName: 'password', inputElement: passwordId });
	}

	// sign-out, followed by a full page reload
	{
		const { pageId, bodyId } = await authPage('auth-sign-out');
		const buttonId           = await addButton(pageId, bodyId, 'signout-button', 'Sign out');

		await builder.actionMapping(buttonId, {
			event:            'click',
			action:           'sign-out',
			successBehaviour: 'full-page-reload'
		});
	}

	// sign-out with a parameter that is not a string
	{
		const { pageId, bodyId } = await authPage('auth-sign-out-json');
		const buttonId           = await addButton(pageId, bodyId, 'signout-json-button', 'Sign out everywhere');

		const actionMappingId = await builder.actionMapping(buttonId, {
			event:  'click',
			action: 'sign-out',
			...inlineNotifications('Signed out ({status})', 'Sign-out failed ({status})')
		});

		await builder.parameter(actionMappingId, { parameterType: 'constant-value', parameterName: 'everywhere', constantValue: 'json(true)' });
	}

	// reset-password
	{
		const { pageId, bodyId } = await authPage('auth-reset-password');
		const mailId             = await addInput(pageId, bodyId, 'reset-email', 'text');
		const buttonId           = await addButton(pageId, bodyId, 'reset-button', 'Reset password');

		const actionMappingId = await builder.actionMapping(buttonId, {
			event:  'click',
			action: 'reset-password',
			...inlineNotifications('Request received ({status})', 'Request failed ({status}): {message}')
		});

		await builder.parameter(actionMappingId, { parameterType: 'user-input', parameterName: 'eMail', inputElement: mailId });
	}

	// sign-up
	{
		const { pageId, bodyId } = await authPage('auth-sign-up');
		const mailId             = await addInput(pageId, bodyId, 'signup-email', 'text');
		const buttonId           = await addButton(pageId, bodyId, 'signup-button', 'Sign up');

		const actionMappingId = await builder.actionMapping(buttonId, {
			event:  'click',
			action: 'sign-up',
			...inlineNotifications('Registered ({status})', 'Registration failed ({status}): {message}')
		});

		await builder.parameter(actionMappingId, { parameterType: 'user-input', parameterName: 'eMail', inputElement: mailId });
	}
});

// An anonymous visitor fills name and password on auth-sign-in and clicks the sign-in button: the payload carries
// both fields, the response is 200 with the user (no password) as result, the inline notification names the user,
// a reload still shows the user as signed in, and no browser dialog appears.
test('sign-in with name and password establishes a session and returns the user', async ({ page }) => {

	const dialogs = countDialogs(page);

	await openPage(page, 'auth-sign-in');
	await expect(page.locator('#current-user')).toHaveText('anonymous');

	await page.locator('#signin-name').fill(users.signIn.name);
	await page.locator('#signin-password').fill(users.signIn.password);

	const request  = nextActionRequest(page);
	const response = nextActionResponse(page);

	await page.locator('#signin-button').click();

	const payload = (await request).postDataJSON();

	expect(payload.name).toBe(users.signIn.name);
	expect(payload.password).toBe(users.signIn.password);

	const signInResponse = await response;
	const body           = await signInResponse.json();

	// the result is the signed-in user, without its password
	expect(signInResponse.status()).toBe(200);
	expect(body.result.name).toBe(users.signIn.name);
	expect(body.result.id).toMatch(UUID_PATTERN);
	expect(body.result.password).toBeUndefined();

	await expect(page.locator('.structr-event-action-notification')).toHaveText(`✅ Signed in as ${users.signIn.name}`);

	// the session survives a reload
	await page.reload();
	await expect(page.locator('#current-user')).toHaveText(users.signIn.name);

	expect(dialogs.count).toBe(0);
});

// An anonymous visitor signs in on auth-sign-in-email with eMail and password instead of a name: the payload
// carries eMail but no name, the response is 200, and the full-page-reload follow-up renders the page for that user.
test('sign-in with the e-mail address followed by a full page reload shows the signed-in user', async ({ page }) => {

	await openPage(page, 'auth-sign-in-email');
	await expect(page.locator('#current-user')).toHaveText('anonymous');

	await page.locator('#signin-email').fill(users.signInByMail.eMail);
	await page.locator('#signin-email-password').fill(users.signInByMail.password);

	const request  = nextActionRequest(page);
	const response = nextActionResponse(page);

	await page.locator('#signin-email-button').click();

	const payload = (await request).postDataJSON();

	expect(payload.eMail).toBe(users.signInByMail.eMail);
	expect(payload.name).toBeUndefined();

	expect((await response).status()).toBe(200);

	// the follow-up reloads the page, which now renders for the signed-in user
	await expect(page.locator('#current-user')).toHaveText(users.signInByMail.name);
});

// A sign-in on auth-sign-in with a wrong password: the response is 401 with the standard login error, the failure
// notification shows status and message, no alert is forced, and after a reload the visitor is still anonymous.
test('a wrong password is rejected with 401, shown in the failure notification and leaves the visitor anonymous', async ({ page }) => {

	const dialogs = countDialogs(page);

	await openPage(page, 'auth-sign-in');

	await page.locator('#signin-name').fill(users.wrongPassword.name);
	await page.locator('#signin-password').fill('not-the-password');

	const response = nextActionResponse(page);

	await page.locator('#signin-button').click();

	const signInResponse = await response;

	expect(signInResponse.status()).toBe(401);
	expect((await signInResponse.json()).message).toBe(STANDARD_LOGIN_ERROR);

	await expect(page.locator('.structr-event-action-notification')).toContainText(`❌ Sign-in failed (401): ${STANDARD_LOGIN_ERROR}`);

	// a 401 is an error like any other: only the configured notification, no forced alert
	expect(dialogs.count).toBe(0);

	await page.reload();
	await expect(page.locator('#current-user')).toHaveText('anonymous');
});

// The event grant is temporarily reduced to signed-in users only, then an anonymous visitor signs in with correct
// credentials: the action is rejected with 401, shown in the failure notification without an alert, no session is
// created, and the grant is restored afterwards.
test('without a resource access grant for anonymous visitors the action is rejected with 401 and shown in the failure notification', async ({ page }) => {

	const dialogs = countDialogs(page);

	// keep the grant for signed-in users only
	await builder.update('ResourceAccess', grantId, { flags: AUTH_USER_POST });

	try {

		await openPage(page, 'auth-sign-in');

		await page.locator('#signin-name').fill(users.noGrant.name);
		await page.locator('#signin-password').fill(users.noGrant.password);

		const response = nextActionResponse(page);

		await page.locator('#signin-button').click();

		const signInResponse = await response;

		expect(signInResponse.status()).toBe(401);

		await expect(page.locator('.structr-event-action-notification')).toContainText('❌ Sign-in failed (401)');
		expect(dialogs.count).toBe(0);

		// the correct credentials did not get through
		await page.reload();
		await expect(page.locator('#current-user')).toHaveText('anonymous');

	} finally {

		await builder.update('ResourceAccess', grantId, { flags: AUTH_USER_POST | NON_AUTH_USER_POST });
	}
});

// A user signs in through auth-sign-in, then clicks the sign-out button on auth-sign-out: the response is 200, the
// full-page-reload follow-up shows the visitor as anonymous, and other pages see no session either.
test('sign-out ends the session', async ({ page }) => {

	await signIn(page, users.signOut);

	await openPage(page, 'auth-sign-out');
	await expect(page.locator('#current-user')).toHaveText(users.signOut.name);

	const response = nextActionResponse(page);

	await page.locator('#signout-button').click();

	expect((await response).status()).toBe(200);

	// the follow-up reloads the page, which now renders for an anonymous visitor
	await expect(page.locator('#current-user')).toHaveText('anonymous');

	await openPage(page, 'auth-sign-in');
	await expect(page.locator('#current-user')).toHaveText('anonymous');
});

// A signed-in user clicks a sign-out button whose mapping sends the constant json(true): the payload carries a
// boolean, and the sign-out must still succeed with 200, show the success notification and end the session.
test('sign-out with a parameter that is not a string still ends the session', async ({ page }) => {

	await signIn(page, users.signOutJson);

	await openPage(page, 'auth-sign-out-json');
	await expect(page.locator('#current-user')).toHaveText(users.signOutJson.name);

	const request  = nextActionRequest(page);
	const response = nextActionResponse(page);

	await page.locator('#signout-json-button').click();

	// json(...) constants arrive as JSON values, not as strings
	expect((await request).postDataJSON().everywhere).toBe(true);

	expect((await response).status()).toBe(200);
	await expect(page.locator('.structr-event-action-notification')).toHaveText('✅ Signed out (200)');

	await page.reload();
	await expect(page.locator('#current-user')).toHaveText('anonymous');
});

// A reset-password request on auth-reset-password for an address no user has: the payload carries the address,
// the response is a neutral 200 shown in the success notification, and no user is created for the address.
test('reset-password with an unknown e-mail address answers 200 without revealing that the address is unknown', async ({ page }) => {

	const unknownAddress = 'nobody-here@example.com';

	await openPage(page, 'auth-reset-password');

	await page.locator('#reset-email').fill(unknownAddress);

	const request  = nextActionRequest(page);
	const response = nextActionResponse(page);

	await page.locator('#reset-button').click();

	expect((await request).postDataJSON().eMail).toBe(unknownAddress);
	expect((await response).status()).toBe(200);

	await expect(page.locator('.structr-event-action-notification')).toHaveText('✅ Request received (200)');

	// and no account was created for the address
	expect(await builder.find('User', { eMail: unknownAddress })).toHaveLength(0);
});

// A reset-password request with the e-mail input left empty: the payload sends eMail as null, and the server
// answers 422 "No e-mail address given.", shown in the failure notification.
test('reset-password without an e-mail address is rejected with 422', async ({ page }) => {

	await openPage(page, 'auth-reset-password');

	const request  = nextActionRequest(page);
	const response = nextActionResponse(page);

	await page.locator('#reset-button').click();

	// an empty input is sent as null, which the server treats as a missing address
	expect((await request).postDataJSON().eMail).toBeNull();
	expect((await response).status()).toBe(422);

	await expect(page.locator('.structr-event-action-notification')).toContainText(`❌ Request failed (422): ${NO_EMAIL_ADDRESS_ERROR}`);
});

// A sign-up request on auth-sign-up with the e-mail input left empty: the server answers 422 "No e-mail address
// given.", shown in the failure notification, and the number of users stays the same.
test('sign-up without an e-mail address is rejected with 422 and creates no user', async ({ page }) => {

	const usersBefore = (await builder.find('User')).length;

	await openPage(page, 'auth-sign-up');

	const response = nextActionResponse(page);

	await page.locator('#signup-button').click();

	expect((await response).status()).toBe(422);

	await expect(page.locator('.structr-event-action-notification')).toContainText(`❌ Registration failed (422): ${NO_EMAIL_ADDRESS_ERROR}`);

	expect(await builder.find('User')).toHaveLength(usersBefore);
});
