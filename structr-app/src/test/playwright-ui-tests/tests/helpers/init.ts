///
/// Copyright (C) 2010-2026 Structr GmbH
///
/// This file is part of Structr <http://structr.org>.
///
/// Structr is free software: you can redistribute it and/or modify
/// it under the terms of the GNU General Public License as
/// published by the Free Software Foundation, either version 3 of the
/// License, or (at your option) any later version.
///
/// Structr is distributed in the hope that it will be useful,
/// but WITHOUT ANY WARRANTY; without even the implied warranty of
/// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/// GNU General Public License for more details.
///
/// You should have received a copy of the GNU General Public License
/// along with Structr.  If not, see <http://www.gnu.org/licenses/>.
///

// @ts-check
import {expect, Locator, Page} from "@playwright/test";

export async function initialize(playwright, createData) {

	const context = await playwright.request.newContext({
		extraHTTPHeaders: {
			'Accept': 'application/json',
			'X-User': 'superadmin',
			'X-Password': process.env.SUPERUSER_PASSWORD,
		}
	});

	// clear database
	await context.post(process.env.BASE_URL + '/structr/rest/maintenance/clearDatabase');

	// create admin user
	await context.post(process.env.BASE_URL + '/structr/rest/User', {
		data: JSON.stringify({
			name: 'admin',
			password: 'admin',
			isAdmin: true
		})
	});

	// create everything in createData
	if (createData) {

		for (let type in createData) {

			await context.post(process.env.BASE_URL + `/structr/rest/${type}`, {
				data: JSON.stringify(createData[type]),
			});
		}
	}

	return context;
}

export async function goToModule(page: Page, linkId: string) {

	let link = page.locator(linkId);

	const isInsideSubmenu = await link.evaluate(el => el.closest('#submenu') !== null);

	if (isInsideSubmenu) {
		await page.locator('.submenu-trigger').hover();
	}

	await link.waitFor({ state: 'visible' });

	// depending on module load time (and the internal delay for "isBlocked = false") this can take a moment
	await page.waitForFunction(() => (Structr.mainMenu.isBlocked === false));

	await link.click();
}

export async function waitForDialogBoxToClose(page: Page) {
	await expect(page.locator('#dialogBox')).toHaveCount(0);
}

export async function waitUntilAttributeChangedAndReturnIt(page:Page, selector: string, attributeName: string, prevValue: string) {

    if (prevValue == null) {
        await expect(page.locator(selector)).toHaveAttribute(attributeName, /.+/);
    } else {
        await expect(page.locator(selector)).not.toHaveAttribute(attributeName, prevValue);
    }

    let val = await page.locator(selector).getAttribute(attributeName);
    // console.log(val);
    return val;
}