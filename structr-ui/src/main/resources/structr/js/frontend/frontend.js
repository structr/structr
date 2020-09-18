/*
 * Copyright (C) 2010-2020 Structr GmbH
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

'use strict';

export class Frontend {

	constructor() {

		// Javascript event handlers are called with 'this' bound to the current target,
		// but we need 'this' to be the current class instances, so we create bound
		// functions that have 'this' bound to the class instance. This is necessary
		// because we must remove and re-add event handlers after a partial reload,
		// which is only possible with the exact same function instance..
		this.boundHandleGenericEvent = this.handleGenericEvent.bind(this);
		this.boundHandleDragStart    = this.handleDragStart.bind(this);
		this.boundHandleDragOver     = this.handleDragOver.bind(this);
		this.boundHandleDrag         = this.handleDrag.bind(this);

		this.bindEvents();
	}

	resolveData(event, target) {

		let data     = target.dataset;
		let resolved = {};

		// active input fields with a name
		if (target.name && target.name.length > 0) {

			resolved[target.name] = this.resolveValue(target);
		}

		for (var key in data) {

			let value = data[key];
			if (value) {

				let lastIndex = value.length - 1;

				// css(selector)
				if (value.indexOf('css(') === 0 && value[lastIndex] === ')') {

					// resolve CSS selector
					let selector = value.substring(4, lastIndex);
					let element  = document.querySelector(selector);

					if (element) {

						resolved[key] = this.resolveValue(element);
					}

				} else if (value.indexOf('json(') === 0 && value[lastIndex] === ')') {

					let json = value.substring(5, lastIndex);
					resolved[key] = JSON.parse(json);

				} else if (value.indexOf('data(') === 0 && value[lastIndex] === ')') {

					let data = event.dataTransfer.getData('application/json');
					resolved[key] = JSON.parse(data);

				} else {

					switch (key) {

						// do not resolve internal keys
						case 'structrId':
						case 'structrEvents':
						case 'structrReloadTarget':
							break;

						default:
							// just copy the value
							resolved[key] = data[key];
					}

				}
			}
		}

		return resolved;
	}

	resolveValue(element) {

		if (element.nodeName === 'INPUT' && element.type === 'checkbox') {

			// input[type="checkbox"]
			return element.checked;

		} else {

			// all other node types
			return element.value;
		}
	}

	handleResult(button, result) {

		if (button.dataset.structrReloadTarget) {

			this.reloadPartial(button.dataset.structrReloadTarget);

		} else {

			// what should be the default?
			// Reload, or nothing?
			window.location.reload();
		}
	}

	handleError(button, error) {
		console.log(error);
	}

	reloadPartial(selector) {

		let reloadTargets = document.querySelectorAll(selector);
		if (reloadTargets.length) {

			for (let container of reloadTargets) {

				let data = container.dataset;
				let id   = data.structrId;

				if (id && id.length === 32) {

					fetch('/structr/html/' + id, {
						method: 'GET',
						credentials: 'same-origin'
					}).then(response => {
						if (!response.ok) { throw new Error('Network response was not ok.'); }
						return response.text();
					}).then(html => {
						var content = document.createElement(container.nodeName);
						content.innerHTML = html;
						container.replaceWith(content.children[0]);
						this.bindEvents();
					}).catch(e => {
						console.log(e);
					});

				} else {

					console.log('Container with selector ' + selector + ' has no data-id attribute, will not be reloaded.');
				}
			}

		} else {

			console.log('Container with selector ' + selector + ' not found.');
		}
	}

	handleGenericEvent(event) {

		event.preventDefault();
		event.stopPropagation();

		let target = event.currentTarget;
		let data   = target.dataset;
		let id     = data.structrId;

		if (id && id.length === 32) {

			// store event type in htmlEvent property
			data.htmlEvent = event.type;

			fetch('/structr/rest/DOMElement/' + id + '/event', {
				body: JSON.stringify(this.resolveData(event, target)),
				method: 'post',
				credentials: 'same-origin'
			})
			.then(response => response.json())
			.then(json     => this.handleResult(target, json.result))
			.catch(error   => this.handleError(target, error));
		}
	}

	handleDragStart(event) {

		event.stopPropagation();

		let data = this.resolveData(event, event.currentTarget.dataset);

		// store UUID of structr node explicitly for drag and drop
		data.id = event.currentTarget.dataset.structrTarget;

		// initialize dataTransfer object
		event.dataTransfer.setData("application/json", JSON.stringify(data));
		event.dataTransfer.dropEffect = "move";
	}

	handleDragOver(event) {
		event.preventDefault();
		event.stopPropagation();
		event.dataTransfer.dropEffect = "move";
	}

	handleDrag(event) {
		event.preventDefault();
		event.stopPropagation();
	}

	bindEvents() {

		// buttons
		document.querySelectorAll('*[data-structr-events]').forEach(elem => {

			let source = elem.dataset.structrEvents;
			if (source) {

				let mapping = source.split(",");
				for (let event of mapping) {

					elem.removeEventListener(event, this.boundHandleGenericEvent);
					elem.addEventListener(event, this.boundHandleGenericEvent);

					if (event === 'drop') {

						elem.removeEventListener('dragstart', this.boundHandleDragStart);
						elem.removeEventListener('dragover',  this.boundHandleDragOver);
						elem.removeEventListener('drag',      this.boundHandleDrag);

						elem.addEventListener('dragstart', this.boundHandleDragStart);
						elem.addEventListener('dragover',  this.boundHandleDragOver);
						elem.addEventListener('drag',      this.boundHandleDrag);
					}
				}
			}
		});
	}
}

window.structrFrontendModule = new Frontend();
