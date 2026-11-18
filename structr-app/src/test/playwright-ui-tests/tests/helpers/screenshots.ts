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
import {Locator, Page, PageScreenshotOptions} from "@playwright/test";

declare const _Theme: any;

/**
 * Takes a screenshot for the documentation in both colour schemes: the light one at options.path, the dark one
 * under the same name in a dark/ folder next to it (screenshots/x.png and screenshots/dark/x.png). The documentation
 * shows the one that matches the scheme of the UI.
 *
 * The scheme is switched in the page without touching the mouse or the keyboard, so a hover, an open menu or a
 * focused field are in both pictures; the choice is not stored. A page without the colour schemes (the configuration
 * interface, the classic look) and a page that is dark already get the one screenshot at options.path.
 */
export async function screenshot(target: Page | Locator, options: PageScreenshotOptions & { path: string }) {

	await target.screenshot(options);

	const page     = isLocator(target) ? target.page() : target;
	const previous = await page.evaluate(() => {

		if (typeof _Theme === 'undefined' || !_Theme.isNext() || _Theme.isDark()) {
			return null;
		}

		const previous = _Theme.current;

		_Theme.current = { ...previous, scheme: 'dark' };
		_Theme.apply();

		return previous;
	});

	if (!previous) {
		return;
	}

	await settle(page);
	await target.screenshot({ ...options, path: darkPath(options.path) });

	await page.evaluate(previous => {

		_Theme.current = previous;
		_Theme.apply();

	}, previous);

	await settle(page);
}

function isLocator(target: Page | Locator): target is Locator {
	return typeof (target as Locator).page === 'function';
}

function darkPath(path: string): string {

	const slash = path.lastIndexOf('/');

	return path.substring(0, slash + 1) + 'dark/' + path.substring(slash + 1);
}

// the modules with a canvas or an editor repaint on the switch, the map with an animation
async function settle(page: Page) {

	await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
	await page.waitForTimeout(500);
}
