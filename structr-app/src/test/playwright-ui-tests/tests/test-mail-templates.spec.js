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
import {goToModule, initialize} from "./helpers/init";
import {login, logout} from "./helpers/auth";

const fs = require('fs');

test.beforeAll(async ({playwright}) => {
    await initialize(playwright);
});

test('mail-templates', async ({page}, testInfo) => {

    console.log(testInfo.title);

	await login(page);

	await goToModule(page, '#mail-templates_');

    // Wait for Code UI to load all components
    await page.waitForTimeout(1000);

    await page.getByRole('button').nth(2).click();
    await page.getByText('Create default "Self-').click();

	await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/mail-templates.png'});

    await logout(page);
});
