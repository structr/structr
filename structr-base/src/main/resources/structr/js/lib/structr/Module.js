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

const logEnabled = true;
var ws, callbacks = {}, sessionId;

class Module {

	constructor(callback) {
		Module.templates = Module.templates();

		Module.isEnc = (window.location.protocol === 'https:');
		Module.hostname = document.location.hostname;
		Module.port = 8082;
		Module.wsUrl = 'ws' + (Module.isEnc ? 's' : '') + '://' + Module.hostname + ':' + Module.port + '/structr/ws';
		Module.initWebsocket(Module.wsUrl, callback);
	}

	static initWebsocket(wsUrl, callback) {
		ws = new WebSocket(wsUrl, 'structr');
		ws.onmessage = function(event) {//console.log('client message received:', event.data)
			let data = JSON.parse(event.data);
			if (data.callback && callbacks[data.callback]) {
				//console.log(data);
				callbacks[data.callback](data);
				delete callbacks[data.callback];
			}
		};
		ws.onopen = function(event) {
			if (ws.readyState < 2) {
				wsCommand({
					command: 'LOGIN',
					sessionId: document.cookie ? document.cookie.match(/JSESSIONID=[^;]+/)[0].split('=')[1] : '',
					data: {
						username: 'admin',
						password: 'admin'
					}
				}, callback);
			}
		};
	}

	static toHTML(templateName, data) {
		if (!templateName)
			return '';

		let template = Module.templates[templateName];

		//console.log('1: Template', templateName, ': ', template, data);

		// Repeaters
		//let output = template.replace(/\$\[\[([^\]]*)\]\]/g, function(match, group1) {
		//let html = document.querySelector('[data-template="' + templateName+ '"]').outerHTML; console.log(html);
		//console.log(template);
		
		// TODO: Support multi-line repeater template. Problem: Current regex with /s option does only match the last expression.
		
		let output = template.replace(/^\s*<([a-z]+)\s.*(data-template=")([^"]*)".*(<\/\1>)/gmi, function(match, group1, group2, group3) {
			//console.log('Match:', match, group1, group2, group3);
			let out = '';
			let name, key;
			if (group3.split(':').length > 1) {
				name = group3.split(':')[0];
				key = group3.split(':')[1];

				//console.log(key, data, data[key])

				if (key && data && data[key]) {

					if (Array.isArray(data[key])) {
						data[key].forEach(function(item) {
							out += Module.toHTML(group3, item);
						});
					} else {
						out += Module.toHTML(group3, data[key]);
					}
				}

			} else {

				//console.log('no sub key, render object attributes as collection');

				if (data && typeof data === 'object') {
					let out = '';
					Object.keys(data).forEach(function(key) {
						out += Module.toHTML(group3, {'@key': key, '@value': data[key]});
					});
					return out;
				}
			}
			//console.log('Match:', group3, ', key:', key, ', name:', name);


			return out;
		});
		//console.log('2.', output);
		// Templates
		output = output.replace(/\${{([^}]*)\}\}/gi, function(match, group1) {
			return Module.toHTML(group1, data);
		});
		//console.log('3.', output);
		// Object as collection of its attributes
		output = output.replace(/\$\[{([^}]*)\}\]/gi, function(match, group1) {
			if (group1 && group1.length) {
				if (data && typeof data === 'object') {
					let out = '';
					Object.keys(data).forEach(function(key) {
						out += Module.toHTML(group1, {'@key': key, '@value': data[key]});
					});
					return out;
				}
			}
		});
		//console.log('4.', output);
		// Attributes
		output = output.replace(/\${([^}]*)\}/gi, function(match, group1) {
			if (group1 && group1.length) {
				//let type    = group1.split('.')[0];
				let key = group1.split('.')[0];
				let subkey = group1.split('.')[1];
				let subsubkey = group1.split('.')[2];
				//if (type && key && type === data['type']) {
				if (data && key) {
					if (data[key] && typeof data[key] === 'object') {
						if (data[key][subkey] && typeof data[key][subkey] === 'object') {
							return data[key][subkey][subsubkey] || '';
						}
						return data[key][subkey] || '';
					}
					if (key === '@type') {
						return data.type;
					}
					return data[key] || '';
				}
			}
		});
		//console.log('5.', output);
		return output;
	}

	/**
	 * Render the given data object with template and display the resulting HTML
	 * by replacing the content of the element with the matching selector.
	 * @param {Object} data
	 * @param {String} templateName
	 * @param {String} selector
	 * @returns {undefined}
	 */
	static render(data, templateName, selector) {
		document.querySelector(selector).innerHTML = Module.toHTML(templateName, data);
	}

	static append(html, selector) {
		document.querySelector(selector).insertAdjacentHTML('afterBegin', html);
	}

	static prependToLog(text) {
		let logOutputElementSelector = '#overview-middle-column .secondary-content .portlet-body';
		let now = new Date();
		Module.append('<div><small>' + now + ' <b>' + text + '</b></small></div>', logOutputElementSelector);
	}

	log() {
		if (logEnabled) {
			console.log(Object.values(arguments), new Error().stack.substring(5));
		}
	}

	/**
	 * Static function that returns a map of template strings.
	 * @returns {Template object} templates
	 */
	static templates() {
		let templates = {};
		let templateElements = document.querySelectorAll('[data-template]');
		templateElements.forEach(function(templateElement) {
			let name = templateElement.getAttribute('data-template');
			//console.log(name);
			templateElement.removeAttribute('data-template');
			templates[name] = templateElement.outerHTML;
		});

		templateElements.forEach(function(templateElement) {
			// uncomment next line to hide template elements
			templateElement.parentNode.removeChild(templateElement);
		});

		return templates;
	}

	/**
	 * Generic function to save the value of an input field to the backend.
	 * 
	 * @param {object} e - event object
	 * @param {string} outputElementSelector - CSS selector for output DOM element
	 * @param {string} logOutputElementSelector - CSS selector for log output DOM element
	 */
	static saveModifiedValue(e, outputElementSelector, logOutputElementSelector) {
		let id = e.srcElement.closest('[data-id]').getAttribute('data-id');
		let key = e.srcElement.getAttribute('name');
		let value = e.srcElement.value;
		//console.log('Modified object', id, key, value);
		let data = {};
		if (key && value) {
			data[key] = value;
			//console.log(data);
			wsCommand({command: 'UPDATE', id: id, data: data}, function(response) {
				console.log('Response from UPDATE:', response);
				if (response.result[0].parent) {
					Files.updateFolderContent(response.result[0].parent.id, '#overview-middle-column .primary-content .portlet', logOutputElementSelector);
				} else {
					Files.updateRoot('#overview-middle-column .primary-content .portlet', logOutputElementSelector);
				}
			});
		}
	}

}

function getCORS(url, success, user, password) {
	let xhr = new XMLHttpRequest();
	if (!('withCredentials' in xhr)) {
		xhr = new XDomainRequest(); // fix IE8/9
	}
	xhr.open('GET', url);
	if (user && password) {
		xhr.setRequestHeader('X-User', user);
		xhr.setRequestHeader('X-Password', password);
	}
	xhr.onload = success;
	xhr.send();
	return xhr;
}

function wsCommand(queryObj, callback) {
	if (ws.readyState < 2) {
		if (sessionId && !queryObj.sessionId) {
			queryObj = sessionId;
		}
		if (callback) {
			queryObj.callback = uuid.v4();
			callbacks[queryObj.callback] = callback;
		}
		let json = JSON.stringify(queryObj);
		//console.log('sending ws command', json);
		ws.send(json);
	} else {
		//console.log('WS not ready, try again', ws.readyState);
		Module.initWebsocket(Module.wsUrl, function() {
			// try again
			wsCommand(queryObj, callback);
		});
	}
}

function getREST(queryObj, callback) {
	let url = 'http://localhost:8082/structr/rest/' + (queryObj.type ? queryObj.type : '') + (queryObj.id ? ('/' + queryObj.id) : '') + '/ui';

	if (queryObj.properties) {
		url += '?';
		Object.keys(queryObj.properties).forEach(function(key) {
			url += key + '=' + (queryObj.properties[key] || '') + '&';
		});
	}
	//console.log(url);
	let xhr = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject('Microsoft.XMLHTTP');
	xhr.open('GET', url);
	xhr.onreadystatechange = function() {
		if (xhr.readyState > 3 && xhr.status === 200) {
			callback(JSON.parse(xhr.responseText));
		}
	};
	xhr.send();
}

function getGraphQL(query, callback) {
	let url = 'http://localhost:8082/structr/graphql?query=' + query;
	//console.log(url);
	let xhr = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject('Microsoft.XMLHTTP');
	xhr.open('GET', url);
	xhr.onreadystatechange = function() {
		if (xhr.readyState > 3 && xhr.status === 200) {
			callback(JSON.parse(xhr.responseText));
		}
	};
	xhr.send();
}

function addEventListeners() {

	new Files();
	
	// manually trigger event
	window.dispatchEvent(new Event('resize'));

	document.body.querySelectorAll('.vertical-divider').forEach(function(divider) {
		divider.addEventListener('mousedown', function(e) {
			startResizeNavColumn(e, divider);
			return false;
		}, false);
		divider.parentNode.addEventListener('mousemove', function(e) {
			resizeColumns(e, divider);
		});
		divider.parentNode.addEventListener('mouseup', function(e) {
			stopResizeNavColumn(e, divider);
		});
	});
}
function getElementIndex(node) {
	let index = 0;
	while (node && (node = node.previousElementSibling)) {
		index++;
	}
	return index;
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