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

		this.boundHandleCheckboxClick = this.handleCheckboxClick.bind(this);
		this.boundHandleSelectChange  = this.handleSelectChange.bind(this);
		this.boundHandleButtonClick   = this.handleButtonClick.bind(this);
		this.boundHandleInputBlur     = this.handleInputBlur.bind(this);

		this.bindEvents();
	}

	resolveData(data) {

		let resolved = {};

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
						resolved[key] = element.value;
					}

				} else if (value.indexOf('json(') === 0 && value[lastIndex] === ')') {

					let json = value.substring(5, lastIndex);
					resolved[key] = JSON.parse(json);

				} else {

					// just copy the value
					resolved[key] = data[key];
				}
			}
		}

		return resolved;
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
						var content = document.createElement('div');
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

	handleButtonClick(event) {

		let button = event.target;
		let data   = button.dataset;
		let id     = data.structrId;

		if (id && id.length === 32) {

			fetch('/structr/rest/Button/' + id + '/event', {
				body: JSON.stringify(this.resolveData(data)),
				method: 'post',
				credentials: 'same-origin'
			})
			.then(response => response.json())
			.then(json     => this.handleResult(button, json.result))
			.catch(error   => this.handleError(button, error));
		}
	}

	handleInputBlur(event) {

		let input = event.target;
		let data  = Object.assign({}, input.dataset);
		let value = input.value;
		let orig  = input.defaultValue;
		let name  = data.name;
		let id    = data.structrId;

		if (id && id.length === 32 && value !== orig) {

			// remove internal properties
			delete data.structrId;
			delete data.name;

			// store property value to be set
			data[name] = value;

			fetch('/structr/rest/Input/' + id + '/event', {
				body: JSON.stringify(data),
				method: 'post',
				credentials: 'same-origin'
			})
			.then(response => response.json())
			.then(json     => this.handleResult(input, json.result))
			.catch(error   => this.handleError(input, error));
		}
	}

	handleCheckboxClick(event) {

		let input = event.target;
		let data  = Object.assign({}, input.dataset);
		let name  = data.name;
		let value = input.checked;
		let id    = data.structrId;

		if (id && id.length === 32) {

			// remove internal properties
			delete data.structrId;
			delete data.name;

			// store property value to be set
			data[name] = value;

			fetch('/structr/rest/Input/' + id + '/event', {
				body: JSON.stringify(data),
				method: 'post',
				credentials: 'same-origin'
			})
			.then(response => response.json())
			.then(json     => this.handleResult(input, json.result))
			.catch(error   => this.handleError(input, error));
		}
	}

        handleSelectChange(event) {

                let select = event.target;
		let data   = Object.assign({}, select.dataset);
                let value  = select.value;
                let name   = data.name;
                let id     = data.structrId;

                if (id && id.length === 32) {

			// remove internal properties
			delete data.structrId;
			delete data.name;

                        // store property value to be set
                        data[name] = value;

                        fetch('/structr/rest/Select/' + id + '/event', {
                                body: JSON.stringify(data),
                                method: 'post',
                                credentials: 'same-origin'
                        })
                        .then(response => response.json())
                        .then(json     => this.handleResult(select, json.result))
                        .catch(error   => this.handleError(select, error));
                }
        }

	bindEvents() {

		// buttons
		document.querySelectorAll('button[data-structr-id]').forEach(btn => {
			btn.removeEventListener('click', this.boundHandleButtonClick);
			btn.addEventListener('click', this.boundHandleButtonClick);
		});

		// text inputs
		document.querySelectorAll('input[type="text"][data-structr-id]').forEach(inp => {
			inp.removeEventListener('blur', this.boundHandleInputBlur);
			inp.addEventListener('blur', this.boundHandleInputBlur);
		});

		// date inputs
		document.querySelectorAll('input[type="date"][data-structr-id]').forEach(inp => {
			inp.removeEventListener('change', this.boundHandleInputBlur);
			inp.addEventListener('change', this.boundHandleInputBlur);
		});

		// number inputs
		document.querySelectorAll('input[type="number"][data-structr-id]').forEach(inp => {
			inp.removeEventListener('blur', this.boundHandleInputBlur);
			inp.addEventListener('blur', this.boundHandleInputBlur);
		});

		// textareas
		document.querySelectorAll('textarea[data-structr-id]').forEach(inp => {
			inp.removeEventListener('blur', this.boundHandleInputBlur);
			inp.addEventListener('blur', this.boundHandleInputBlur);
		});

		// checkboxes
		document.querySelectorAll('input[type="checkbox"][data-structr-id]').forEach(inp => {
			inp.removeEventListener('click', this.boundHandleCheckboxClick);
			inp.addEventListener('click', this.boundHandleCheckboxClick);
		});

		// selects
		document.querySelectorAll('select[data-structr-id]').forEach(inp => {
			inp.removeEventListener('change', this.boundHandleSelectChange);
			inp.addEventListener('change', this.boundHandleSelectChange);
		});

	}
}

window.structrFrontendModule = new Frontend();
