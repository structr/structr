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
/* Look and colour scheme of the UI. Loaded in the head, so the classes are on <html> before the first paint. */
let _Theme = {

	lookKey:   'structrUiLook' + location.port,
	schemeKey: 'structrUiScheme' + location.port,
	listeners: [],
	media:     window.matchMedia('(prefers-color-scheme: dark)'),

	read: (key, fallback) => {

		try {
			return localStorage.getItem(key) ?? fallback;
		} catch (e) {
			return fallback;
		}
	},
	write: (key, value) => {

		try {
			localStorage.setItem(key, value);
		} catch (e) {
			// private mode: the choice lasts for this page only
		}
	},
	look:   () => (_Theme.current.look === 'classic') ? 'classic' : 'next',
	scheme: () => ['light', 'dark'].includes(_Theme.current.scheme) ? _Theme.current.scheme : 'auto',
	isNext: () => _Theme.look() === 'next',
	isDark: () => _Theme.isNext() && (_Theme.scheme() === 'dark' || (_Theme.scheme() === 'auto' && _Theme.media.matches)),

	/* the classic look has one colour scheme, so the scheme classes exist in the new look only */
	apply: () => {

		const root = document.documentElement;
		const next = _Theme.isNext();
		const dark = _Theme.isDark();

		root.classList.toggle('ui-next', next);
		root.classList.toggle('theme-dark', dark);
		root.classList.toggle('theme-light', next && !dark);

		for (const listener of _Theme.listeners) {
			listener({ next, dark });
		}
	},
	set: (look, scheme) => {

		_Theme.current = { look, scheme };
		_Theme.write(_Theme.lookKey, _Theme.look());
		_Theme.write(_Theme.schemeKey, _Theme.scheme());
		_Theme.apply();
	},
	onChange: (listener) => {
		_Theme.listeners.push(listener);
	},
	/* the value of a colour token as the page resolves it, for canvases and libraries that take no CSS variables */
	color: (token) => getComputedStyle(document.documentElement).getPropertyValue('--' + token).trim()
};

_Theme.current = { look: _Theme.read(_Theme.lookKey, 'next'), scheme: _Theme.read(_Theme.schemeKey, 'auto') };
_Theme.media.addEventListener('change', _Theme.apply);
_Theme.apply();
