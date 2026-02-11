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
import { Page, Locator } from '@playwright/test';

/**
 * Access wrapper for Structr's page tree.
 */
export class Container {

    locator: Locator;

    constructor(locator: Locator) {
        this.locator = locator;
    }

    /**
     * Returns the text node of this container (the one with the tag
     * or text in it), so you can click on it.
     */
    getTextNode(): Locator {
        return this.locator.locator('> div.node-container > span');
    }

    /**
     * Returns the tree node of this container (the one that contains
     * the children), so you can use it to descend the hierarchy.
     * @param name
     * @param index
     */
    getElement(name: string, index: number = 0): Container {
        return new Container(this.locator.getByText(name).nth(index).locator('../../..'));
    }

    getElements(name: string): Locator {
        return this.locator.locator('> div.node > div.node-container > span').filter({ hasText: name });
    }

    async countElements(name: string): Promise<number> {
        return await this.getElements(name).count();
    }

    /**
     * Returns the parent container of this node.
     */
    getParentContainer() {
        return new Container(this.locator.locator('..'));
    }
}

export async function createAndRenamePage(page: Page, whichTemplate, name, wait = 100) {

    await page.locator('#pages-actions .dropdown-select').click();
    await page.locator('#create_page').click();
    await page.waitForTimeout(wait);
    await page.locator(`#template-tiles .app-tile:nth-child(${whichTemplate})`).click();
    await page.waitForTimeout(wait);

    await page.getByText('New Page').click();
    await page.waitForTimeout(wait);
    await page.getByRole('link', {name: 'General'}).click();
    await page.waitForTimeout(wait);
    await page.locator('#name-input').fill(name);
    await page.locator('#pagesTree').click();
    await page.waitForTimeout(wait);
}

/**
 * Returns a locator that is confined to the given page.
 * @param page
 * @param pageName
 * @param wait
 */
export function getPageContainer(page: Page, pageName: string, wait = 100): Container {
    return new Container(page.locator(`#pagesTree > div:has(div:nth-child(1) > span > b[title="${pageName}"])`));
}

export async function expandPageTree(page: Page, pageName, wait = 100) {

    let pageNode = getPageContainer(page, pageName, wait);

    await expandOrCollapseElement(page, pageNode, 'expand');
}

export async function collapsePageTree(page: Page, name, wait = 100) {

    let pageNode = getPageContainer(page, name, wait);

    await expandOrCollapseElement(page, pageNode, 'collapse');
}

export async function expandOrCollapseElement(page: Page, node: Container, action: string, wait = 100) {

    switch (action) {

        case 'expand':
            await useContextMenu(page, node, 'Expand / Collapse', 'Expand subtree recursively');
            break;
        case 'collapse':
            await useContextMenu(page, node, 'Expand / Collapse', 'Collapse subtree');
            break;
    }
}

export async function insertFrontendJs(page: Page, pageName, wait = 100) {

    let pageNode = getPageContainer(page, pageName, wait);

    let head = pageNode.getElement('head');

    await useContextMenu(page, head, 'Suggested HTML element', 'script');

    let script = head.getElement('script');

    await page.waitForTimeout(wait);
    await script.getTextNode().click();

    await page.waitForTimeout(wait);
    await page.getByRole('link', {name: 'HTML'}).click();
    await page.waitForTimeout(wait);
    await page.locator('input[name="_html_type"]').fill('module');
    await page.locator('input[name="_html_src"]').fill('/structr/js/frontend/frontend.js');
}

export async function resizePagesTree(page: Page, offset: number, wait = 100) {

    let resizer = page.locator('.column-resizer-left');

    await resizer.isVisible();

    let box = await resizer.boundingBox();

    if (box && box.x) {

        // resize pages tree flyout
        await resizer.isVisible();
        await resizer.hover();
        await page.waitForTimeout(wait);
        await page.mouse.down();
        await page.waitForTimeout(wait);
        await page.mouse.move(box.x + offset, 0, {steps: 20});
        await page.waitForTimeout(100);
        await page.waitForTimeout(wait);
        await page.mouse.up();
        await page.waitForTimeout(wait);

    } else {

        console.log('box was null!');
    }

}

export async function insertInputWithLabel(page: Page, container: Container, name: string, inputOrSelect: string, wait = 100) {

    await useContextMenu(page, container,'Insert HTML element', 'l-m', 'label');
    await useContextMenu(page, container.getParentContainer(), 'Expand / Collapse', 'Expand subtree recursively');

    let count = await container.countElements('label');
    let labelContainer = container.getElement('label', count - 1);
    let textContainer = labelContainer.getElement('Initial text for label');

    await useContextMenu(page, textContainer, 'Wrap element in...', '... HTML element', 's', 'span');
    await page.waitForTimeout(1000);
    await setNodeContent(page, textContainer, name);

    switch (inputOrSelect) {

        case 'input':
            await useContextMenu(page, labelContainer, 'Insert HTML element', 'i-k', 'input');
            break;
        case 'select':
            await useContextMenu(page, labelContainer, 'Insert HTML element', 's', 'select');
            break;
    }

    await labelContainer.getTextNode().click();

    // make label style display: block;
    await page.getByRole('link', {name: 'HTML'}).click();
    await page.locator('input[name="_html_style"]').fill('display: block;');

    // click on input element to make it configurable
    await labelContainer.getElement(inputOrSelect).getTextNode().click();
}

export async function configureHTMLAttributes(page: Page, node: Container, data: Object, wait = 100) {

    await node.getTextNode().click();
    await page.getByRole('link', {name: 'HTML'}).click();

    for (let key in data) {
        await page.locator(`input[name="_html_${key}"]`).fill(data[key]);
    }

    await page.waitForTimeout(wait);

}

export async function configureGeneralAttributes(page: Page, node: Container, data: Object, wait = 100) {

    await node.getTextNode().click();
    await page.getByRole('link', {name: 'General'}).click();

    for (let key in data) {
        await page.locator(`input[name="${key}"]`).fill(data[key]);
    }

    await page.waitForTimeout(wait);

}

export async function configureFunctionQuery(page: Page, node: Container, query: string, dataKey: string, wait = 100) {

    await node.getTextNode().click();

    await page.waitForTimeout(wait);
    await page.getByRole('link', {name: 'Repeater'}).click();
    await page.waitForTimeout(wait);
    await page.getByText('Function Query', { exact: true }).click();
    await page.waitForTimeout(wait);
    await page.locator('.monaco-editor').nth(0).click();
    await page.keyboard.type(query);
    await page.waitForTimeout(wait);
    await page.locator('.save-repeater-query').click();
    await page.locator('.repeater-datakey').fill(dataKey);
    await page.waitForTimeout(wait);
    await page.locator('.save-repeater-datakey').click();
}

export async function setNodeContent(page: Page, node: Container, content: string, wait = 100) {

    await node.getTextNode().click();
    await page.waitForTimeout(1000);
    await page.keyboard.down('Control');
    await page.keyboard.press('A');
    await page.keyboard.up('Control');
    await page.keyboard.type(content);
    await page.waitForTimeout(200);
    await page.getByRole('button', {name: 'Save'}).click();
    await page.waitForTimeout(wait);
}

export async function useContextMenu(page: Page, container: Container, level1: string, level2?: string, level3?: string, level4?: string) {

    await container.getTextNode().click({button: 'right'});

    let menu = page.locator('#context-menu-dialog');
    let sub = menu.locator('> ul > li').filter({ has: page.getByText(level1, { exact: true }) });

    if (level2) {

        await sub.hover();
        let subsub = sub.locator('> ul > li').filter({ has: page.getByText(level2, { exact: true }) });

        if (level3) {

            await subsub.hover();
            let subsubsub = subsub.locator('> ul > li').filter({ has: page.getByText(level3, { exact: true }) });

            if (level4) {

                await subsubsub.hover();
                let subsubsubsub = subsubsub.locator('> ul > li').filter({ has: page.getByText(level4, { exact: true }) });
                await subsubsubsub.click();

            } else {

                await subsubsub.click();
            }

        } else {

            // direct click on second level
            await subsub.click();
        }

    } else {

        // direct click on first level
        await sub.click();
    }
}