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
import { test, expect } from '@playwright/test';

test.beforeAll(async ({ playwright }) => {

  const context = await playwright.request.newContext({
    extraHTTPHeaders: {
      'Accept': 'application/json',
      'X-User': 'superadmin',
      'X-Password': process.env.SUPERUSER_PASSWORD,
    }
  });

  // Clear all users, groups, resource access and cors settings objects
  await context.delete(process.env.BASE_URL + '/structr/rest/User');
  await context.delete(process.env.BASE_URL + '/structr/rest/Group');
  await context.delete(process.env.BASE_URL + '/structr/rest/ResourceAccess');
  await context.delete(process.env.BASE_URL + '/structr/rest/CorsSetting');

  // Create new admin user
  await context.post(process.env.BASE_URL + '/structr/rest/User',  {
    data: {
      name: 'admin',
      password: 'admin',
      isAdmin: true
    }
  });
});

test('user-group', async ({ page }) => {

  test.setTimeout(240_000);

  //await page.setViewportSize({ width: 3840, height: 2160 });
  await page.goto(process.env.BASE_URL + '/structr/');
  //await page.evaluate('document.body.style.zoom="2.0"');

  await expect(page).toHaveTitle(/Structr/);
  await expect(page.locator('#usernameField')).toBeVisible();
  await expect(page.locator('#passwordField')).toBeVisible();
  await expect(page.locator('#loginButton')).toBeVisible();

  await page.waitForTimeout(1000);

  //await page.screenshot({ path: 'screenshots/login.png' });

  // Login with admin/admin
  await page.locator('#usernameField').fill('admin');
  await page.locator('#passwordField').fill('admin');
  await page.waitForTimeout(500);
  await page.locator('#loginButton').click();
  await page.waitForTimeout(1000);

  // Security
  await page.locator('#security_').waitFor({state: 'visible'});
  await page.locator('#security_').click();

  // Wait for Security UI to load all components
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/security.png' });

  // Create new user
  await page.locator('#add-user-button').waitFor({state: 'visible'});
  await page.locator('#add-user-button').click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/security_create-user.png' });

  // Rename user
  await page.locator('#users').getByText('New User').first().click();
  await page.keyboard.type('Renamed user');
  await page.keyboard.press('Enter');
  await page.locator('#users').click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/security_rename-user.png' });
  await page.locator('#users').getByText('Renamed user').waitFor({state: 'visible'});

  // Change password
  await page.locator('#users').getByText('Renamed user').click({ button: 'right'});
  await page.getByText('General').first().click();
  await page.waitForTimeout(1000);
  await page.locator('input#password-input').dblclick();
  await page.keyboard.type('test123');
  await page.screenshot({ path: 'screenshots/security_change-admin-password.png' });
  await page.getByText('Set Password').first().click();
  await page.waitForTimeout(1000);
  await page.getByRole('button', {name: 'Close', exact: true}).click();

  // Create new group
  await page.locator('#add-group-button').waitFor({state: 'visible'});
  await page.locator('#add-group-button').click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/security_create-group.png' });

  // Rename group
  await page.locator('#groups').getByText('New Group').first().click();
  await page.keyboard.type('Renamed group');
  await page.keyboard.press('Enter');
  await page.locator('#groups').click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/security_renamed-group.png' });
  await page.locator('#groups').getByText('Renamed group').waitFor({state: 'visible'});

  // Drag user into group
  await page.locator('#users').getByText('Renamed user').first().hover();
  await page.mouse.down();
  await page.locator('#groups').getByText('Renamed group').first().hover();
  await page.mouse.up();
  await page.waitForTimeout(1000);
  // Open group
  await page.locator('#groups .expand_icon_svg.svg-collapsed').first().click();
  await page.screenshot({ path: 'screenshots/security_user-dragged-on-group.png' });
});

test('resource-access-before-1', async ({ request }) => {

  // Check access permissions before creating resource access objects
  const response = await request.get(process.env.BASE_URL + '/structr/rest/Project', {
    headers: {
      'Accept': 'application/json',
      'X-User': 'Renamed user',
      'X-Password': 'test123',
    }
  });

  await expect(response.status()).toBe(401);

});

test('add-resource-access-auth-user-get', async ({ page }) => {

  test.setTimeout(240_000);

  //await page.setViewportSize({ width: 3840, height: 2160 });
  await page.goto(process.env.BASE_URL + '/structr/');
  //await page.evaluate('document.body.style.zoom="2.0"');

  await expect(page).toHaveTitle(/Structr/);
  await expect(page.locator('#usernameField')).toBeVisible();
  await expect(page.locator('#passwordField')).toBeVisible();
  await expect(page.locator('#loginButton')).toBeVisible();

  await page.waitForTimeout(1000);

  //await page.screenshot({ path: 'screenshots/login.png' });

  // Login with admin/admin
  await page.locator('#usernameField').fill('admin');
  await page.locator('#passwordField').fill('admin');
  await page.waitForTimeout(500);
  await page.locator('#loginButton').click();
  await page.waitForTimeout(1000);

  // Security
  await page.locator('#security_').waitFor({state: 'visible'});
  await page.locator('#security_').click();

  // Resource Permissions: Create 'User'
  await page.locator('#resourceAccess_').click();
  await page.waitForTimeout(1000);
  await page.locator('#resource-signature').click();
  await page.keyboard.type('Project');
  await page.locator('#resourceAccesses').getByText('Create Permission').first().click();
  await page.waitForTimeout(1000);
  await page.locator('#resourceAccessesTable tr:has-text("Project") input[data-key="AUTH_USER_GET"]').first().click();
  await page.waitForTimeout(1000);
  await page.locator('#resourceAccessesTable tr:has-text("Project") .svg_key_icon').first().click();
  await page.waitForTimeout(1000);
  await page.locator('#dialogBox .visibleToPublicUsers_').first().click();
  await page.waitForTimeout(1000);
  await page.getByRole('button', {name: 'Close', exact: true}).click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/security_create-resource-access-auth-user-get.png' });

});

test('resource-access-after-1', async ({ request }) => {

  // Check access permissions before creating resource access objects
  const response = await request.get(process.env.BASE_URL + '/structr/rest/Project', {
    headers: {
      'Accept': 'application/json',
      'X-User': 'Renamed user',
      'X-Password': 'test123',
    }
  });

  await expect(response.status()).toBe(200);

});

test('resource-access-before-2', async ({ request }) => {

  // Check access permissions before creating resource access objects
  const response = await request.post(process.env.BASE_URL + '/structr/rest/Project', {
    body: '{ name: "test" }',
    headers: {
      'Accept': 'application/json',
      'X-User': 'Renamed user',
      'X-Password': 'test123',
    }
  });

  await expect(response.status()).toBe(401);

});


test('add-resource-access-auth-user-post', async ({ page }) => {

  test.setTimeout(240_000);

  //await page.setViewportSize({ width: 3840, height: 2160 });
  await page.goto(process.env.BASE_URL + '/structr/');
  //await page.evaluate('document.body.style.zoom="2.0"');

  await expect(page).toHaveTitle(/Structr/);
  await expect(page.locator('#usernameField')).toBeVisible();
  await expect(page.locator('#passwordField')).toBeVisible();
  await expect(page.locator('#loginButton')).toBeVisible();

  await page.waitForTimeout(1000);

  //await page.screenshot({ path: 'screenshots/login.png' });

  // Login with admin/admin
  await page.locator('#usernameField').fill('admin');
  await page.locator('#passwordField').fill('admin');
  await page.waitForTimeout(500);
  await page.locator('#loginButton').click();
  await page.waitForTimeout(1000);

  // Security
  await page.locator('#security_').waitFor({state: 'visible'});
  await page.locator('#security_').click();

  // Resource Permissions: Create 'User/_id'
  await page.locator('#resourceAccess_').click();
  await page.waitForTimeout(1000);
  await page.locator('#resource-signature').click();
  await page.keyboard.type('Project/_id');
  await page.locator('#resourceAccesses').getByText('Create Permission').first().click();
  await page.waitForTimeout(1000);
  await page.locator('#resourceAccessesTable tr:has-text("Project") input[data-key="AUTH_USER_POST"]').first().click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/security_create-resource-access-auth-user-post.png' });

  // Logout
  await page.locator('.submenu-trigger').hover();
  await page.waitForTimeout(500);
  await page.locator('#logout_').waitFor({ state: 'visible' });
  await page.locator('#logout_').click();
  await page.locator('#usernameField').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);
});

test('resource-access-after-2', async ({ request }) => {

  // Check access permissions before creating resource access objects
  const response = await request.post(process.env.BASE_URL + '/structr/rest/Project', {
    body: '{ name: "test" }',
    headers: {
      'Accept': 'application/json',
      'X-User': 'Renamed user',
      'X-Password': 'test123',
    }
  });

  await expect(response.status()).toBe(201);
});

test('cors-settings', async ({ page }) => {

  test.setTimeout(240_000);

  //await page.setViewportSize({ width: 3840, height: 2160 });
  await page.goto(process.env.BASE_URL + '/structr/');
  //await page.evaluate('document.body.style.zoom="2.0"');

  await expect(page).toHaveTitle(/Structr/);
  await expect(page.locator('#usernameField')).toBeVisible();
  await expect(page.locator('#passwordField')).toBeVisible();
  await expect(page.locator('#loginButton')).toBeVisible();

  await page.waitForTimeout(1000);

  //await page.screenshot({ path: 'screenshots/login.png' });

  // Login with admin/admin
  await page.locator('#usernameField').fill('admin');
  await page.locator('#passwordField').fill('admin');
  await page.waitForTimeout(500);
  await page.locator('#loginButton').click();
  await page.waitForTimeout(1000);

  // Security
  await page.locator('#security_').waitFor({state: 'visible'});
  await page.locator('#security_').click();

  // Wait for Security UI to load all components
  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/security_.png' });

  // CORS Settings: Typical Example

  // HTTP/1.1 204 No Content
  // Access-Control-Allow-Origin: *
  // Access-Control-Allow-Methods: GET,HEAD,PUT,PATCH,POST,DELETE
  // Vary: Access-Control-Request-Headers
  // Access-Control-Allow-Headers: Content-Type, Accept
  // Content-Length: 0
  // Date: Fri, 05 Apr 2019 11:41:08 GMT
  // Connection: keep-alive
  await page.getByText('CORS Settings').click();
  await page.waitForTimeout(1000);
  await page.locator('#cors-setting-request-uri').click();
  await page.keyboard.type('/structr/html/projects');
  await page.locator('#corsSettings').getByText('Create CORS Setting').first().click();
  await page.waitForTimeout(1000);

  await page.locator('#corsSettingsTable tbody tr:first-child input[data-attr-key="acceptedOrigins"]').first().click();
  await page.waitForTimeout(250);
  await page.keyboard.type('*');
  await page.keyboard.press('Tab');
  await page.waitForTimeout(500);

  await page.locator('#corsSettingsTable tbody tr:first-child input[data-attr-key="maxAge"]').first().click();
  await page.waitForTimeout(250);
  await page.keyboard.type('3600');
  await page.keyboard.press('Tab');
  await page.waitForTimeout(500);

  await page.locator('#corsSettingsTable tbody tr:first-child input[data-attr-key="allowMethods"]').first().click();
  await page.waitForTimeout(250);
  await page.keyboard.type('HEAD, GET, PUT, POST, OPTIONS');
  await page.keyboard.press('Tab');
  await page.waitForTimeout(500);

  await page.locator('#corsSettingsTable tr input[data-attr-key="allowHeaders"]').first().click();
  await page.waitForTimeout(250);
  await page.keyboard.type('Content-Type, Accept');
  await page.keyboard.press('Tab');
  await page.waitForTimeout(500);

  await page.locator('#corsSettingsTable tr input[data-attr-key="allowCredentials"]').first().click();
  await page.waitForTimeout(250);
  await page.keyboard.type('true');
  await page.keyboard.press('Tab');
  await page.waitForTimeout(500);

  await page.locator('#corsSettingsTable tr input[data-attr-key="exposeHeaders"]').first().click();
  await page.waitForTimeout(250);
  await page.keyboard.type('Allow');
  await page.keyboard.press('Tab');
  await page.waitForTimeout(500);


  await page.waitForTimeout(1000);
  await page.screenshot({ path: 'screenshots/security_create-cors-setting.png' });


  // Logout
  await page.locator('.submenu-trigger').hover();
  await page.waitForTimeout(500);
  await page.locator('#logout_').waitFor({ state: 'visible' });
  await page.locator('#logout_').click();
  await page.locator('#usernameField').waitFor({ state: 'visible' });
  await page.waitForTimeout(1000);
});
