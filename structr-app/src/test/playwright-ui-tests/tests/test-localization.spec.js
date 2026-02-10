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
import {test} from '@playwright/test';
import {initialize} from "./helpers/init";
import {login, logout} from "./helpers/auth";

test.beforeAll(async ({playwright}) => {
    await initialize(playwright);
});

test('localization', async ({page}) => {

    await login(page);

    await page.locator('.submenu-trigger').hover();
    await page.locator('#localization_').waitFor({state: 'visible'});
    await page.locator('#localization_').click({force: true});

    // Wait for Code UI to load all components
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/localization.png'});

    // Create new Localization elements
    await page.getByRole('textbox', {name: 'Enter a key to create'}).click();
    await page.keyboard.type('milestones');
    await page.keyboard.press('Tab');
    await page.keyboard.type('table-header');
    await page.keyboard.press('Tab');
    await page.keyboard.type('en,de');
    await page.getByRole('button', {name: 'Create Localization'}).click();
    await page.waitForTimeout(500);
    await page.locator('.___localizedName').nth(0).click();
    await page.keyboard.type('Meilensteine');
    await page.waitForTimeout(500);
    await page.locator('.___localizedName').nth(1).click();
    await page.keyboard.type('Milestones');
    await page.keyboard.press('Tab');
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/localization_created.png'});

    await logout(page);
});
