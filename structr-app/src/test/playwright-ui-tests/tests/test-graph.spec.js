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

const fs = require('fs');


test.beforeAll(async ({playwright}) => {
    await initialize(playwright);
});

test('graph', async ({page}) => {

    await login(page);

    // Pages
    await page.locator('#pages_').waitFor({state: 'visible'});
    await page.locator('#pages_').click();

    // Wait for Pages UI to load all components
    await page.waitForTimeout(1000);
    //await page.screenshot({ path: 'screenshots/pages.png' });

    // Create new page
    await page.locator('#pages-actions .dropdown-select').click();
    await page.locator('#create_page').waitFor({state: 'visible'});
    await page.locator('#create_page').click();
    await page.waitForTimeout(1000);
    //await page.screenshot({ path: 'screenshots/pages_create-page.png' });
    await page.locator('#template-tiles .app-tile:nth-child(2)').click();
    await page.waitForTimeout(2000);

    await page.locator('.submenu-trigger').hover();
    await page.locator('#graph_').waitFor({state: 'visible'});
    await page.locator('#graph_').click();

    // Wait for Graph UI to load all components
    await page.waitForTimeout(500);
    await page.screenshot({path: 'screenshots/graph.png'});

    // Enter Cypher query to show all nodes and relationships
    await page.getByPlaceholder('Cypher query').click();
    await page.keyboard.type('MATCH (n)-[r]-(m) RETURN n,r,m');
    await page.locator('#exec-cypher').click();
    await page.waitForTimeout(1000);
    await page.screenshot({path: 'screenshots/graph_show-nodes.png'});

    const canvasNode = await page.evaluate('_Graph.graphBrowser.getNodes()[0]');
    const x = parseFloat(canvasNode['renderer1:x']);
    const y = parseFloat(canvasNode['renderer1:y']) + 128;

    // Hover over first node
    await page.mouse.move(x, y);

    await page.waitForTimeout(500);
    await page.screenshot({path: 'screenshots/graph_node-hover.png'});

    // Click on first node to open properties dialog
    await page.mouse.click(x, y);
    await page.waitForTimeout(500);
    await page.screenshot({path: 'screenshots/graph_node-properties-opened.png'});

    await page.getByRole('button', {name: 'Close', exact: true}).click();

    await logout(page);
});
