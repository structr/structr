/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */

export class Component {

	constructor(name, selector) {
		this.name = name;
		this.selector = selector;
	}

	/**
	 * Register event functions with output and log output DOM element
	 * 
	 * @param {string} event
	 * @param {string|array|object} selector - CSS selector of DOM element(s) to bind function to
	 * @param {function} eventListenerFunction
	 */
	on(event, selector, eventListenerFunction) {
		
		let outputElementSelector = this.selector;
		if (typeof selector === 'object') {
			if (Array.isArray(selector)) {
				selector.forEach(function(sel) {
					live(sel, event, function(e) {
						eventListenerFunction(e, outputElementSelector);
					});
				});
			} else {
				live(null, event, function(e) {
					eventListenerFunction(e, outputElementSelector);
				}, selector);
			}
		} else {
			live(selector, event, function(e) {
				eventListenerFunction(e, outputElementSelector);
			});
		}
		
		return this;
	}

}

// helper for enabling IE 8 event bindings
function addEvent(el, type, handler) {
	if (el.attachEvent) {
		el.attachEvent('on' + type, handler);
	} else {
		el.addEventListener(type, handler);
	}
}

// live binding helper using matches selector
function live(selector, event, callback, context) {
	addEvent(context || document, event, function(e) {
		
		let found, el = e.target || e.srcElement;
		//console.log('live', e, el, selector)
		while (el && el.matches && el !== context && !(found = el.matches(selector))) {
			el = el.parentElement;
		}
		if (found) {
			callback.call(el, e);
		}
	});
}