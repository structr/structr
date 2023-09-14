/*
 * Copyright (C) 2010-2023 Structr GmbH
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
		this.boundHandleFocus        = this.handleFocus.bind(this);
		this.boundHandleBlur         = this.handleBlur.bind(this);
		this.boundHandleDrag         = this.handleDrag.bind(this);
		this.boundHandleRender       = this.handleRender.bind(this);

		// variables
		this.eventListeners          = {};
		this.currentlyFocusedElement = '';
		this.timeout                 = -1;

		// init
		this.bindEvents();
	}

	resolveData(event, target) {

		let data     = target.dataset;
		let resolved = {};

		// active input fields with a name
		if (target.name && target.name.length > 0) {

			resolved[target.name] = this.resolveElementValue(target);
		}

		for (const key in data) {

			let value = data[key];
			if (!value) {
				continue;
			}

			resolved[key] = this.resolveValue(key, value, data, event, target);

		}

		return resolved;
	}

	resolveValue(key, value, data, event, target) {

		// switch (key) {
		//
		// 	// do not resolve internal keys
		// 	case 'structrId':
		// 	case 'structrEvents':
		// 	case 'structrReloadTarget':
		// 		return;
		// }

		let lastIndex = value.length - 1;

		if (value.indexOf('json(') === 0 && value[lastIndex] === ')') {

			let json = value.substring(5, lastIndex);
			return JSON.parse(json);

		} else if (value.indexOf('data(') === 0 && value[lastIndex] === ')') {

			// data() refers to the dataset of the datatransfer object in a drag and drop
			// event, maybe the name of the key needs some more thought..
			let data = event.dataTransfer.getData('application/json');
			if (data && data.length) {

				return JSON.parse(data);
			}
		}

		const elements = this.resolveElements(target, value);

		// support multiple elements
		let values = [];

		elements.forEach(element => {
			let value = this.resolveElementValue(element);
			if (value !== undefined) {
				values.push(value);
			}
		});

		// reduce array of length 1 to single value
		switch (values.length) {
			case 0:
				// just copy the value
				return data[key];
			case 1:
				return values[0];
			default:
				return values;
		}

	}

	resolveElements(target, value) {

		let elements, name, id;
		let lastIndex = value.length - 1;

		if (value.startsWith('css(') && value.endsWith(')')) {

			// resolve CSS selector
			let selector = value.substring(4, lastIndex);
			elements = document.querySelectorAll(selector);

		} else if (value.startsWith('name(') && value.endsWith(')')) {

			// resolve only input elements by name in current page
			name = value.substring(5, lastIndex);
			elements = document.querySelectorAll(`input[name="${name}"]`);

		} else if (value.startsWith('id(') && value.endsWith(')')) {

			// resolve element by data-structr-id in current page
			id = value.substring(3, lastIndex);
			elements = document.querySelectorAll(`[data-structr-id="${id}"]`);

		}

		if (elements && elements.length > 1) {

			// if we are in repeater loop, find single element
			let repeaterElement = target.closest('[data-repeater-data-object-id]');
			if (repeaterElement) {

				let selector;
				if (id) {
					selector = selector = `[data-repeater-data-object-id="${repeaterElement.dataset.repeaterDataObjectId}"] [data-structr-id="${id}"]`;
				} else if (name) {
					selector = `[data-repeater-data-object-id="${repeaterElement.dataset.repeaterDataObjectId}"] input[name="${name}"]`;
				}

				let element = document.querySelector(selector);
				if (element) {
					return [element];
				}

			}
		}

		return elements || [];
	}

	resolveElementValue(element) {

		if (element.nodeName === 'INPUT' && (element.type === 'checkbox' || element.type === 'radio')) {

			// use value if present
			if (element.value) {

				if (element.checked) {
					return element.value;
				}

				// no value please
				return undefined;
			}

			// return boolean if no value is present
			return element.checked;

		} else if (element.nodeName === 'SELECT' && element.multiple) {

			// select[multiple]
			return [].map.call(element.selectedOptions, (e) => { return e.value; });

		} else {

			if (element.value.length) {

				// all other node types
				return element.value;
			}

			return null;
		}
	}

	resetValue(element) {

		if (element.nodeName === 'INPUT' && element.type === 'checkbox') {

			// input[type="checkbox"]
			element.checked = element.defaultChecked;

		} else {

			// all other node types
			element.value = element.defaultValue;
		}
	}

	handleResult(element, json, status, options) {

		switch (status) {

			case 200:
			case 201:
				this.fireEvent('success', { target: element, data: json, status: status });
				this.handleNotifications(element, json, status, options);
				this.processFollowUpActions(element, json.result, status, options);
				break;

			case 400:
			case 401:
			case 403:
			case 404:
			case 405:
			case 422:
			case 500:
			case 503:
				this.fireEvent('error', { target: element, data: json, status: status });
				this.handleNotifications(element, json, status, options);
				this.processFollowUpActions(element, json, status, options);
				break;
		}

		if (options.resetValue === true) {
			this.resetValue(element);
		}
	}

	async handleNotifications(element, parameter, status, options) {

		let mode, statusText, statusHTML, inputElementBorderColor, inputElementBorderWidth;
		let id = element.dataset.structrId;
		const success = this.isSuccess(status);

		if (success) {
			mode = element.dataset.structrSuccessNotifications;
			statusText = '✅ Operation successful with status ' + status + (parameter?.message ? ': ' + parameter.message : '');
			statusHTML = '<div class="structr-event-action-notification" id="notification-for-' + id + '" style="font-size:small;display:inline-block;margin-left:1rem;color:green">' + statusText + '</div>';
			for (let elementWithError of document.querySelectorAll('[data-error]')) {
				elementWithError.style.borderColor = inputElementBorderColor || '';
				elementWithError.style.borderWidth = inputElementBorderWidth || '';
			}
		} else {
			mode = element.dataset.structrFailureNotifications;
			statusText = '❌ Operation failed with status ' + status + (parameter?.message ? ': ' + parameter.message : '');
			statusHTML = '<div class="structr-event-action-notification" id="notification-for-' + id + '" style="font-size:small;display:inline-block;margin-left:1rem;color:red">' + statusText + '<br>';

			if (parameter?.errors?.length) {
				for (const error of parameter.errors) {
					statusHTML += error.property + ' ' + error.token.replaceAll('_', ' ') + '<br>';
					let propertyKey = error.property;

					let propertyInputElement = element.dataset[propertyKey] ? this.resolveElements(element, element.dataset[propertyKey])[0] : element;
					if (propertyInputElement) {
						inputElementBorderColor = propertyInputElement.style.borderColor;
						inputElementBorderWidth = propertyInputElement.style.borderWidth;
						propertyInputElement.style.borderColor = 'red';
						propertyInputElement.style.borderWidth = '1px';
						propertyInputElement.dataset.error = error.token;
					}
				}
			}
			statusHTML += '</div>';
		}

		// none, system-alert, inline-text-message, custom-dialog, custom-dialog-linked
		switch (mode) {

			case 'system-alert':
				window.alert(statusText);
				break;

			case 'inline-text-message':
				// Clear all notification messages
				document.querySelectorAll('.structr-event-action-notification').forEach(el => el.remove());
				element.insertAdjacentHTML('afterend', statusHTML);
				window.setTimeout(() => {
					let notificationElement = document.getElementById('notification-for-' + id);
					if (notificationElement) { notificationElement.remove(); }
				}, 5000);
				break;

			case 'custom-dialog':
				let notificationElementIds = success ? element.dataset.structrSuccessNotificationsPartial : element.dataset.structrFailureNotificationsPartial;
				let partialIds = notificationElementIds.split(',');
				for (let partialId of partialIds) {
					let partialElement = document.querySelector(partialId);
					if (partialElement) {
						partialElement.classList.remove('hidden');
						window.setTimeout(() => {
							partialElement.classList.add('hidden');
						}, 5000);
					}
				}
				break;

			case 'custom-dialog-linked':
				let notificationElementSelectors = success ? element.dataset.structrSuccessNotificationsCustomDialogElement : element.dataset.structrFailureNotificationsCustomDialogElement;
				let partialSelectors = notificationElementSelectors.split(',');
				for (let partialSelector of partialSelectors) {
					let partialElement = document.querySelector(partialSelector);
					if (partialElement) {
						partialElement.classList.remove('hidden');
						window.setTimeout(() => {
							partialElement.classList.add('hidden');
						}, 5000);
					}
				}
				break;

			case 'none':
			default:
				// Default is do nothing
		}

	}

	async processFollowUpActions(element, parameters, status, options) {

		const success = this.isSuccess(status);

		if (success && element.dataset.structrSuccessTarget) {

			let successTargets = element.dataset.structrReloadTarget;

			for (let successTarget of successTargets.split(',').map( t => t.trim() ).filter( t => t.length > 0 )) {

				if (successTarget.indexOf(':') !== -1) {

					let moduleName = successTarget.substring(0, successTarget.indexOf(':'));
					let module     = await import('/structr/js/frontend/modules/' + moduleName + '.js');
					if (module) {

						if (module.Handler) {

							let handler = new module.Handler(this);

							if (handler && handler.handleReloadTarget && typeof handler.handleReloadTarget === 'function') {

								handler.handleReloadTarget(successTarget, element, parameters, status, options);

							} else {

								throw `Handler class for behaviour ${moduleName} has no method "handleReloadTarget".`;
							}

						} else {

							throw `Module for behaviour ${moduleName} has no class "Handler".`;
						}

					} else {

						throw `No module found for behaviour ${moduleName}.`;
					}

				} else if (successTarget === 'none') {

					// do nothing
					return;

				} else if (successTarget === 'sign-out') {

					// sign-out and reload
					fetch('/structr/logout', { method: 'POST' }).then((response) => {
						location.reload();
					});

				} else {

					this.reloadPartial(successTarget, parameters, element);
				}
			}

		} else if (!success && element.dataset.structrFailureTarget) {

			let failureTargets = element.dataset.structrFailureTarget;

			for (let failureTarget of failureTargets.split(',').map( t => t.trim() ).filter( t => t.length > 0 )) {

				if (failureTarget.indexOf(':') !== -1) {

					let moduleName = failureTarget.substring(0, failureTarget.indexOf(':'));
					let module     = await import('/structr/js/frontend/modules/' + moduleName + '.js');
					if (module) {

						if (module.Handler) {

							let handler = new module.Handler(this);

							if (handler && handler.handleReloadTarget && typeof handler.handleReloadTarget === 'function') {

								handler.handleReloadTarget(failureTarget, element, parameters, status, options);

							} else {

								throw `Handler class for behaviour ${moduleName} has no method "handleReloadTarget".`;
							}

						} else {

							throw `Module for behaviour ${moduleName} has no class "Handler".`;
						}

					} else {

						throw `No module found for behaviour ${moduleName}.`;
					}

				} else if (failureTarget === 'sign-out') {

					// sign-out and reload
					fetch('/structr/logout', { method: 'POST' }).then((response) => {
						location.reload();
					});

				} else if (failureTarget === 'none') {

					// do nothing
					return;

				} else {

					this.reloadPartial(failureTarget, parameters, element);
				}
			}

		} else {

			// Default is do nothing
			//window.location.reload();
		}

	}

	isSuccess = status => status < 300;

	displayPartial(selector, parameters, element, dontRebind) {
		let container = document.querySelector(selector);
		container.classList.remove('hidden');
		//this.replacePartial(container, id, element, data, parameters, dontRebind);
	}

	handleNetworkError(element, error, status) {

		console.log(error);

		console.error({element, error, status});

		if (element) {
			this.resetValue(element);
		}

		this.fireEvent('error', { target: element, data: {}, status: status });
	}

	reloadPartial(selector, parameters, element, dontRebind) {

		let reloadTargets = document.querySelectorAll(selector);

		if (!reloadTargets.length) {
			console.log('Container with selector ' + selector + ' not found.');
			return;
		}

		for (let container of reloadTargets) {

			let data = container.dataset;
			let id   = data.structrId;

			if (!id || id.length !== 32) {

				let match = selector.match(/^(.*?)(?:#(.*?))?(?:\\.(.*))?$/gm);
				let attrKey, attrVal;
				if (match[0] && match[0].startsWith('#')) {
					attrKey = '_html_id';
					attrVal = match[0].substring(1);
				} else if (match[0] && match[0].startsWith('.')) {
					attrKey = '_html_class';
					attrVal = match[0].substring(1).replaceAll('.', ' ');
				}

				fetch('/structr/rest/DOMElement?' + attrKey + '=' + attrVal, {
					method: 'GET',
					credentials: 'same-origin'
				}).then(response => {
					//console.log('Found element by ' + attrKey + ' attribute', response);
					return response.json();
				}).then(data => {
					if (data.result && data.result[0]) {
						let id = data.result[0].id;
						this.replacePartial(container, id, element, data, parameters, dontRebind);
					}
				});

			} else {

				this.replacePartial(container, id, element, data, parameters, dontRebind);
			}

		}
	}

	replaceContentInContainer = (container, html) => {
		let content = document.createElement('template');
		content.insertAdjacentHTML('afterbegin', html);
		if (content && content.children && content.children.length) {
			container.replaceWith(content.children[0]);
		} else {
			container.replaceWith('');
		}
	};

	replacePartial(container, id, element, data, parameters, dontRebind) {

		let base   = '/structr/html/' + id;
		let params = this.encodeRequestParameters(data, parameters);
		let uri    = base + params;

		fetch(uri, {
			method: 'GET',
			credentials: 'same-origin'
		}).then(response => {
			if (!response.ok) { throw { status: response.status, statusText: response.statusText } };
			return response.text();
		}).then(html => {
			this.replaceContentInContainer(container, html);
			container.dispatchEvent(new Event('structr-reload'));
			this.fireEvent('reload', {target: container});

			let restoreFocus = container.querySelector('*[name="' + this.focusName + '"][data-structr-id="' + this.focusId + '"][data-structr-target="' + this.focusTarget + '"]');
			if (restoreFocus) {

				if (restoreFocus.focus && typeof restoreFocus.focus === 'function') { restoreFocus.focus(); }
				if (restoreFocus.select && typeof restoreFocus.select === 'function') { restoreFocus.select(); }
			}

			if (!dontRebind) {
				this.bindEvents();
			}

		}).catch(e => {
			this.handleNetworkError(element, e, {});
		});

	}

	loadPartial(uri, container) {

		fetch(uri, {
			method: 'GET',
			credentials: 'same-origin'
		}).then(response => {
			if (!response.ok) { throw { status: response.status, statusText: response.statusText } };
			return response.text();
		}).then(html => {
			this.replaceContentInContainer(container, html);
			this.bindEvents();
		}).catch(e => {
			this.handleNetworkError(container, e, {});
		});
	}

	instantiateTemplate(selector, data, status, options) {

		let templates = document.querySelectorAll(selector);

		if (!templates.length) {
			console.log('Template with selector ' + selector + ' not found.');
			return;
		}

		for (let template of templates) {

			if (template && template.dataset.structrId) {

				let id = template.dataset.structrId;
				if (id) {

					fetch('/structr/html/' + id, {
						body: JSON.stringify({
							status: status,
							data: data
						}),
						contentType: 'application/json',
						method: 'post',
						credentials: 'same-origin'
					})
					.then(response => response.text())
					.then(html => {

						// find existing template element and remove first
						let existing = document.querySelector('*[data-structr-template-id="' + id + '"]');
						if (existing) {

							existing.remove();
						}

						template.parentNode.insertAdjacentHTML('beforeend', html);
						this.bindEvents();

						let fragment = document.querySelector('*[data-structr-template-id="' + id + '"]');
						if (fragment) {

							window.setTimeout(() => fragment.remove(), options.hideTimeout || 2000);
						}
					})
					.catch(error => this.handleNetworkError(template, error, {}));
				}
			}
		}
	}

	/**
	 * Transforms, merges and encodes parameter sets from different objects. All key-value
	 * pairs from fromDataset that are prefixed with "request" are un-prefixed and copied into
	 * the result object. Then all pairs from the override object are copied into the result
	 * object as well. After that, all key-value pairs are URI-encoded and returned
	 *
	 * @param {type} fromDataset
	 * @param {type} override
	 * @returns {String} the URI-encoded objects
	 */
	encodeRequestParameters(fromDataset, override) {

		let params  = {};
		let current = '';

		// current object set?
		if (fromDataset.currentObjectId) {
			current = '/' + fromDataset.currentObjectId;
		}

		// copy all values prefixed with request (data-request-*)
		for (let key of Object.keys(fromDataset)) {

			if (key.indexOf('request') === 0) {

				let restoredRequestKey = key.substring(7);
				restoredRequestKey = restoredRequestKey.substring(0, 1).toLowerCase() + restoredRequestKey.substring(1);
				if (restoredRequestKey.length) {

					params[restoredRequestKey] = fromDataset[key];
				}
			}
		}

		// include render state to reconstruct state of repeaters and dynamic elements
		if (fromDataset.structrRenderState && fromDataset.structrRenderState.length > 0) {
			params['structr-encoded-render-state'] = fromDataset.structrRenderState;
		}

		if (override) {

			// copy all keys from override object into params
			for (let key of Object.keys(override)) {

				let value = override[key];
				if (value === '') {

					delete params[key];

				} else {

					params[key] = value;
				}
			}
		}

		if (params) {

			let result = [];

			for (let key of Object.keys(params)) {

				let value = params[key];
				result.push(key + '=' + encodeURIComponent(value));
			}

			if (result.length) {

				return current + '?' + result.join('&');
			}
		}

		return current;
	}

	handleGenericEvent(event) {

		// event handling defaults
		let preventDefault  = true;
		let stopPropagation = true;

		let target     = event.currentTarget;
		let data       = target.dataset;
		let id         = data.structrId;
		let options    = this.parseOptions(target);
		let delay      = 0;

		// handle options
		if (options.delay) { delay = options.delay; }
		if (options.preventDefault !== undefined) { preventDefault = options.preventDefault; }
		if (options.stopPropagation !== undefined) { stopPropagation = options.stopPropagation; }

		// allow element to override preventDefault and stopPropagation
		if (preventDefault) { event.preventDefault(); }
		if (stopPropagation) { event.stopPropagation(); }

		if (delay === 0) {

			this.doHandleGenericEvent(event, target, data, options);

		} else {

			if (this.timeout) {
				window.clearTimeout(this.timeout);
			}

			this.timeout = window.setTimeout(() => this.doHandleGenericEvent(event, target, data, options), delay);
		}
	}

	doHandleGenericEvent(event, target, data, options) {

		let id = data.structrId;

		// special handling for possible sort keys (can contain multiple values, separated by ,)
		let sortKey = this.getFirst(data.structrTarget);

		// special handling for frontend-only events (pagination, sorting)
		if (sortKey && data[sortKey]) {

			// The above condition is true when the dataset of an element contains a value with the same name as the
			// data-structr-target attribute. This is currently only true for pagination and sorting, where
			// data-structr-target="page" and data-page="1" (which comes from the backend), or
			// data-structr-target="sort" and data-sort="sortKeyName".

			this.handlePagination(event, target, options);

		} else if (id && id.length === 32) {

			this.fireEvent('start', { target: target, data: data, event: event });

			// server-side
			// store event type in htmlEvent property
			data.htmlEvent = event.type;

			fetch('/structr/rest/DOMElement/' + id + '/event', {
				body: JSON.stringify(this.resolveData(event, target)),
				method: 'POST',
				credentials: 'same-origin'
			})
			.then(response => {
				return response.json().then(json => ({ json: json, status: response.status, statusText: response.statusText }))
			})
			.then(response => this.handleResult(target, response.json, response.status, options))
			.catch(error   => this.handleNetworkError(target, error, {}));
		}
	}

	getFirst(csvString) {

		if (csvString && csvString.length) {

			let parts = csvString.split(',');
			return parts[0];
		}

		return csvString;
	}

	handlePagination(event, target, options) {

		//let target       = event.target;
		let data         = target.dataset;
		let selector     = data.structrTarget;
		let reloadTarget = data.structrReloadTarget;

		if (!selector) {
			console.log('Selector not found: ' + selector);
			console.log(target);
			console.log(data);
		 	return;
		}

		let parameters = {};
		let parts      = selector.split(',');
		let sortKey    = selector;
		let orderKey   = 'descending';

		// parse optional order key (default is "descending")
		if (parts.length > 1) {

			sortKey  = parts[0].trim();
			orderKey = parts[1].trim();
		}

		let resolved        = this.resolveData(event, target);
		parameters[sortKey] = resolved[sortKey];

		let reloadTargets = document.querySelectorAll(reloadTarget);
		if (reloadTargets.length) {

			let sortContainer = reloadTargets[0];
			let sortValue     = sortContainer.getAttribute('data-request-' + sortKey);
			let orderValue    = sortContainer.getAttribute('data-request-' + orderKey);

			if (sortValue === resolved[sortKey]) {

				// The values need to be strings because we're
				// parsing them from the request query string.
				if (!orderValue || orderValue === 'false') {

					parameters[orderKey] = 'true';

				} else {

					parameters[orderKey] = '';
				}
			}
		}

		this.handleResult(target, { result: parameters }, 200, options);
	}

	parseQueryString(query) {

		let result = {};

		if (query?.length <= 0) {
			return result;
		}

		for (let part of query.substring(1).split('&')) {

			let keyvalue = part.split('=');
			let key      = keyvalue[0];
			let value    = keyvalue[1];

			if (key && value) {

				result[key] = value;
			}
		}

		return result;
	}

	parseOptions(target) {

		let data = target.dataset;

		// adjust debounce delay if set
		if (data && data.structrOptions) {

			try {

				return JSON.parse(data.structrOptions);

			} catch (e) {
				console.error(e);
			}
		}

		return {};
	}

	handleDragStart(event) {

		event.stopPropagation();

		let data = this.resolveData(event, event.currentTarget);

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

	handleFocus(event) {
		this.focusTarget = event.currentTarget.dataset.structrTarget;
		this.focusId     = event.currentTarget.dataset.structrId;
		this.focusName   = event.currentTarget.name;
	}

	handleBlur(event) {
		this.focusTarget = undefined;
		this.focusName   = undefined;
		this.focusId     = undefined;
	}

	handleRender(el) {
		const id = el.dataset.structrId;
		this.reloadPartial('[data-structr-id="' + id + '"]', null, el, true);
	}

	bindEvents() {

		document.querySelectorAll('*[data-structr-rendering-mode]').forEach(elem => {

			let renderingMode   = elem.dataset.structrRenderingMode;
			let delayOrInterval = elem.dataset.structrDelayOrInterval;
			if (renderingMode.length) {
				this.attachRenderingHandler(elem, this.boundHandleRender, renderingMode, delayOrInterval);
			}
		});

		document.querySelectorAll('*[data-structr-events]').forEach(elem => {

			let source = elem.dataset.structrEvents;
			if (source) {

				let mapping = source.split(",");
				for (let event of mapping) {

					if (event === 'load') {

						// the 'load' event has to be fired right now because we're in it
						const event = new Event('load');
						elem.addEventListener('load',this.boundHandleGenericEvent);
						elem.dispatchEvent(event);
					}

					elem.removeEventListener(event, this.boundHandleGenericEvent);
					elem.addEventListener(event, this.boundHandleGenericEvent);

					// add dragover event listener to support drag and drop
					if (event === 'drop') {

						elem.removeEventListener('dragover',  this.boundHandleDragOver);
						elem.addEventListener('dragover',  this.boundHandleDragOver);
					}

				}
			}

			if (elem.dataset.structrId && elem.dataset.structrTarget && elem.name) {

				// capture focus
				elem.removeEventListener('focus', this.boundHandleFocus);
				elem.addEventListener('focus', this.boundHandleFocus);

				// capture blur
				elem.removeEventListener('blur', this.boundHandleBlur);
				elem.addEventListener('blur', this.boundHandleBlur);
			}
		});

		document.querySelectorAll('*[draggable]').forEach(elem => {

			// add dragstart event listener to support drag and drop
			elem.removeEventListener('dragstart', this.boundHandleDragStart);
			elem.addEventListener('dragstart', this.boundHandleDragStart);
		});
	}

	addEventListener(name, listener) {

		if (!this.eventListeners[name]) {
			this.eventListeners[name] = [];
		}

		this.eventListeners[name].push(listener);
	}

	removeEventListener(name, listener) {

		if (this.eventListeners[name]) {

			let listeners = this.eventListeners[name];

			if (listeners && listeners.length > 0) {

				listener.splice(listeners.indexOf(listener), 1);
			}
		}
	}

	fireEvent(name, data) {

		if (this.eventListeners[name]) {

			let listeners = this.eventListeners[name];

			for (let listener of listeners) {

				listener.apply(null, [data]);
			}
		}
	}

	attachRenderingHandler(element, callback, renderingMode, delayOrInterval) {

		switch (renderingMode) {

			case 'load':
				callback(element);
				break;

			case 'delayed':

				window.setTimeout(() => {
					callback(element);
				}, delayOrInterval|| 1000);
				break;

			case 'visible':

				const observer = new IntersectionObserver((entries, observer) => {
					entries.forEach(entry => {
						if (entry.isIntersecting) {
							callback(element, observer);
						}
					});
				});

				observer.observe(element);

				break;

			case 'periodic':

				callback(element);
				window.setInterval(() => {
					callback(element);
				}, delayOrInterval|| 10000);
				break;

			default:
				callback(element);
				break;
		}

	}
}

window.structrFrontendModule = new Frontend();
