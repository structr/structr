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
import {Page, Locator, expect} from '@playwright/test';
import {waitForDialogBoxToClose} from "./init";

export async function openFileContextMenuFor(page: Page, name: string) {

    await page.getByText(name).hover();
    await page.locator('tr').filter({hasText: name}).getByRole('img', {name: 'Context-Menu'}).click();
}

export async function createFileWithContentAndContentType(page: Page, name: string, content: string[], contentType: string) {

    // create file, rename it, set content type and open import dialogs to make screenshots
    await page.locator('button[data-test-purpose="create-file"]').click();
    await page.getByText('New File ').click();
    await page.keyboard.type(name);
    await page.keyboard.press('Enter');
    await openFileContextMenuFor(page, name);
    await page.getByText('Edit File').click();
    await page.locator('.view-line').click();

    for (let row of content) {
        await page.keyboard.type(row);
        await page.keyboard.press('Enter');
    }
    await page.getByRole('button', {name: 'Save and close', exact: true}).click();

    await waitForDialogBoxToClose(page);

    // set content type
    await openFileContextMenuFor(page, name);
    await page.getByText('General').click();
    await page.getByRole('textbox', {name: 'Content Type'}).fill(contentType);
    await page.keyboard.press('Enter');
    await page.getByRole('button', {name: 'Close', exact: true}).click();
}