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
'use strict';

export class Handler {

	constructor(frontendModule) {
		this.frontendModule = frontendModule;
	}

	/**
	 * Resolve a dotted {path} against the given object. Same convention as the
	 * navigate-to-url handler (url.js) so the client-side {result.id} syntax
	 * behaves identically across behaviours.
	 */
	getValue(obj, path) {

		let components = path
			.replace(/\["(\w+)"]/g, '.$1')   // convert ["index"] to .index
			.replace(/\['(\w+)']/g, '.$1')   // convert ['index'] to .index
			.replace(/\[(\d+)]/g, '.$1')     // convert numeric indexes [0] to .0
			.split('.');

		return components.reduce((acc, key) => acc && acc[key], obj);
	}

	/**
	 * Show/hide page sections without a full reload.
	 *
	 * reloadTarget is built server-side as "hide=<sel|sel>;show=<sel|sel>;url=<url>"
	 * (commas in the selector lists were converted to "|" so the value survives the
	 * runtime's comma-split of multiple targets). Sections in "hide" get the `hidden`
	 * class added.
	 *
	 * For "show":
	 *   - With a "url" part, the shown section(s) are loaded as URL-bound partial(s):
	 *     the client-side {...} placeholders are resolved against the action result,
	 *     the assembled URL is decomposed into a `current` data context (path) plus
	 *     request parameters (query), and each shown partial is reloaded with it.
	 *     This differs from "Refresh page section", which reloads with the existing
	 *     URL; here the partial is bound to a new, assembled URL.
	 *   - Without a "url" part, the section is simply un-hidden (CSS only).
	 */
	handleReloadTarget(reloadTarget, element, parameters, status, options) {

		let show  = [];
		let hide  = [];
		let url   = null;
		let scope = null;

		for (const part of reloadTarget.split(';')) {

			const eq = part.indexOf('=');
			if (eq === -1) {
				continue;
			}

			const mode  = part.substring(0, eq).trim();
			const value = part.substring(eq + 1);

			if (mode === 'show') {
				show = value.split('|').map(s => s.trim()).filter(s => s.length > 0);
			} else if (mode === 'hide') {
				hide = value.split('|').map(s => s.trim()).filter(s => s.length > 0);
			} else if (mode === 'url') {
				url = value;
			} else if (mode === 'scope') {
				scope = value.trim();
			}
		}

		// scope=repeater: restrict the selectors to the repeater element containing the
		// triggering element. CSS ids can't repeat and a linked element's data-structr-id
		// is shared across iterations, so repeater sections match every instance; this
		// prefix pins them to the current one. Falls back to page-wide if the trigger is
		// not inside a repeater. (Same convention as resolveData() in frontend.js.)
		let scopePrefix = '';
		if (scope === 'repeater' && element) {
			const repeaterElement = element.closest('[data-repeater-data-object-id]');
			if (repeaterElement) {
				scopePrefix = `[data-repeater-data-object-id="${repeaterElement.dataset.repeaterDataObjectId}"] `;
			}
		}

		// Apply the scope prefix to a single selector. For linked targets the server may
		// already have appended a (render-time) [data-repeater-data-object-id=...] constraint;
		// drop it so the runtime instance prefix is authoritative and we don't double-scope.
		const scopeSelector = (sel) => {
			if (!scopePrefix) {
				return sel;
			}
			return scopePrefix + sel.replace(/\[data-repeater-data-object-id=("[^"]*"|'[^']*')\]/g, '');
		};

		// hide: add the `hidden` class to every matching element
		for (const selector of hide) {
			for (const target of document.querySelectorAll(scopeSelector(selector))) {
				target.classList.add('hidden');
			}
		}

		if (url) {

			// resolve client-side {...} placeholders against the action result
			const resolved = url.replace(/{([^}]+)}/g, (match, path) => this.getValue({ result: parameters }, path));

			// decompose the assembled URL into a partial-reload override
			const override = {};
			try {
				const u    = new URL(resolved, window.location.origin);
				const segs = u.pathname.split('/');

				// In a Structr page URL (/<page>/<currentId>) the current object id is
				// the third path segment; map it to the `current` data context.
				if (segs.length > 2 && segs[2]) {
					override.current = segs[2];
				}
				for (const [key, value] of u.searchParams.entries()) {
					override[key] = value;
				}
			} catch (e) {
				console.error('show-hide-section: could not parse URL', resolved, e);
			}

			// removeHiddenOnLoad: reloadPartial replaces each shown element with a freshly
			// rendered node that still carries the template's `hidden` class; the flag makes
			// replacePartial drop that class once the new node is in the DOM, so the loaded
			// section becomes visible. (Removing it here, before the reload, would be futile.)
			const reloadOptions = Object.assign({}, options, { updateHistory: true, resets: (options && options.resets) ? options.resets : [], removeHiddenOnLoad: true });

			for (const selector of show) {
				this.frontendModule.reloadPartial(scopeSelector(selector), override, element, false, reloadOptions);
			}

		} else {

			// plain CSS show: just remove the `hidden` class, then honor autofocus on the
			// now-visible section (e.g. focus a "new task" input the moment it appears).
			for (const selector of show) {
				for (const target of document.querySelectorAll(scopeSelector(selector))) {
					target.classList.remove('hidden');
					this.frontendModule.focusAutofocusElement(target);
				}
			}
		}
	}
}
