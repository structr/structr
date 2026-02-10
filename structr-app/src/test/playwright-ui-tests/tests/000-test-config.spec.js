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
import {test, expect} from '@playwright/test';
import {setGlobalTimeout} from "./helpers/init";

// no initialize here because we dont need to do anything
test('config', async ({page}) => {

    await page.goto(process.env.BASE_URL + '/structr/config');
    //await page.evaluate('document.body.style.zoom="2.0"');

    await expect(page).toHaveTitle('Structr Configuration Editor');
    await page.screenshot({path: 'screenshots/config_set-superuser-password.png'});

    // this only works the first time the tests are started, do not change
    await expect(page.getByPlaceholder('Enter a superuser password')).toBeVisible();
    await page.getByPlaceholder('Enter a superuser password').fill(process.env.SUPERUSER_PASSWORD);
    await page.getByRole('button', {name: 'Save'}).click();
    await page.waitForTimeout(500);
    await page.screenshot({path: 'screenshots/config_superuser-password-set.png', caret: 'initial'});
    await page.getByRole('button', {name: 'Configure a database connection'}).click();
    await page.waitForTimeout(500);

    /*
    await expect(page.locator('#loginButton')).toBeVisible();
    await page.screenshot({ path: 'screenshots/config_login.png', caret: 'initial' });

    await page.locator('#passwordField').fill(process.env.SUPERUSER_PASSWORD);
    await page.locator('#loginButton').click();
    await page.waitForTimeout(500);
    await expect(page.locator('#configTabs')).toBeVisible();
    await page.screenshot({ path: 'screenshots/config_after-login.png' });

    // Disconnect from database and remove database connection
    await page.getByRole('link', { name: 'Database Connections' }).click();
    await page.waitForTimeout(500);
    await page.getByRole('button', { name: 'Disconnect'}).click();
    await page.waitForTimeout(500);
    await page.getByRole('button', { name: 'Remove'}).click();
    await page.waitForTimeout(500);
  */

    await expect(page.getByRole('button', {name: 'Create new database connection'})).toBeVisible();
    await page.screenshot({path: 'screenshots/config_create-database-connection.png'});
    await page.getByRole('button', {name: 'Create new database connection'}).click();
    await page.waitForTimeout(500);
    await page.screenshot({path: 'screenshots/config_configure-database-connection.png'});
    await page.getByRole('button', {name: 'Set Neo4j defaults'}).click();
    await page.waitForTimeout(500);
    await page.getByPlaceholder('Enter a connection name').fill('neo4j-localhost-7687');
    await page.getByPlaceholder('Enter URL').fill(process.env.DATABASE_URL);
    await page.getByPlaceholder('Enter password').fill(process.env.DATABASE_PASSWORD);
    await page.screenshot({path: 'screenshots/config_database-connection-specified.png'});

    await page.getByRole('button', {name: 'Add connection'}).click();
    await page.screenshot({path: 'screenshots/config_database-connection-wait.png'});
    await page.waitForTimeout(500);
    await page.screenshot({path: 'screenshots/config_database-connection-established.png'});

    // Logout
    await page.getByRole('link', {name: 'Logout'}).click();


});

