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
let _Helpers = {
	fastRemoveElement: (element) => {
		if (!element) {
			return;
		}
		_Helpers.fastRemoveAllChildren(element);
		element.remove();
	},
	fastRemoveAllChildren: (el) => {

		if (!el) return;

		// prevent fastRemove from producing errors in other libraries
		{
			// do not slice up monaco editors
			if (_Editors) {
				_Editors?.nukeEditorsInDomElement?.(el);
			}
		}

		// memory management block
		{
			// destroy select2 and remove event listeners
			if ($().select2) {
				$('select.select2-hidden-accessible', $(el)).select2('destroy').off();
			}

			// clean up droppables
			// TODO: can be removed after migrating to HTML5 dragndrop completely
			if ($().droppable) {
				try {
					$('.ui-droppable', $(el)).droppable("destroy");
				} catch (e) {};
			}
		}

		_Helpers.fastRemoveAllChildrenInner(el);
	},
	fastRemoveAllChildrenInner: (el) => {

		while (el.firstChild) {

			_Helpers.fastRemoveAllChildrenInner(el.firstChild);

			try {
				el.removeChild(el.firstChild);
			} catch (e) {
				// silently ignore errors like "The node to be removed is no longer a child of this node. Perhaps it was moved in a 'blur' event handler?"
				// console.log(e)
			}
		}
	},
	setContainerHTML: (container, html) => {
		_Helpers.fastRemoveAllChildren(container);
		container.insertAdjacentHTML('beforeend', html);
	},
	extractVal: (string, key) => {
		let pattern = `(${key}=")(.*?)"`;
		let re      = new RegExp(pattern);
		let value   = string.match(re);
		return value && value[2] ? value[2] : undefined;
	},
	/**
	 * Clean text from contenteditable
	 *
	 * This function will remove any HTML markup and convert
	 * any <br> tag into a line feed ('\n').
	 */
	cleanText: (input) => {
		if (typeof input !== 'string') {
			return input;
		}
		let output = input
			.replace(/<div><br><\/div>/ig, '\n')
			.replace(/<div><\/div>/g, '\n')
			.replace(/<br(\s*)\/*>/ig, '\n')
			.replace(/(<([^>]+)>)/ig, "")
			.replace(/\u00A0/ig, String.fromCharCode(32));

		return output;
	},
	unescapeTags: (str) => {
		if (!str)
			return str;
		return str
			.replace(/&nbsp;/g, ' ')
			.replace(/&amp;/g, '&')
			.replace(/&lt;/g, '<')
			.replace(/&gt;/g, '>')
			.replace(/&quot;/g, '"')
			.replace(/&#39;/g, '\'');
	},
	escapeTags: (str) => {
		if (!str)
			return str;
		return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
	},
	escapeForHtmlAttributes: (str, escapeWhitespace) => {
		if (!(typeof str === 'string'))
			return str;
		let escapedStr = str
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&#39;');

		return escapeWhitespace ? escapedStr.replace(/ /g, '&nbsp;') : escapedStr;
	},
	utf8_to_b64: (str) => {
		return window.btoa(unescape(encodeURIComponent(str)));
	},
	expandNewline: (text) => {
		// Expand literal '\n' to newline, which is encoded as '\\n' in HTML attribute values.
		return text.replace(/\\\\n/g, '<br>');
	},
	capitalize: (string) => {
		return string.charAt(0).toUpperCase() + string.slice(1);
	},
	uuidRegexp: new RegExp('^[a-fA-F0-9]{32}$|^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$'),
	isUUID: (str) => _Helpers.uuidRegexp.test(str),
	nvl: (value, defaultValue) => {
		let returnValue;
		if (value === false) {
			returnValue = 'false';
		} else if (value === 0) {
			returnValue = '0';
		} else if (!value) {
			returnValue = defaultValue;
		} else {
			returnValue = value;
		}
		return returnValue;
	},
	formatValueInputField: (key, obj, isPassword, isReadOnly, isMultiline) => {

		if (obj === undefined || obj === null) {

			return _Helpers.formatRegularValueField(key, '', isMultiline, isReadOnly, isPassword);

		} else if (obj.constructor === Object) {

			let displayName = _Crud.displayName(obj);
			return `<div title="${_Helpers.escapeForHtmlAttributes(displayName)}" id="_${obj.id}" class="node ${obj.type ? obj.type.toLowerCase() : (obj.tag ? obj.tag : 'element')} ${obj.id}_"><span class="abbr-ellipsis abbr-80">${displayName}</span>${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16, ['remove'])}</div>`;

		} else if (obj.constructor === Array) {

			return _Helpers.formatArrayValueField(key, obj, isMultiline, isReadOnly, isPassword);

		} else {

			return _Helpers.formatRegularValueField(key, _Helpers.escapeForHtmlAttributes(obj), isMultiline, isReadOnly, isPassword);
		}
	},
	formatArrayValueField: (key, values, isMultiline, isReadOnly, isPassword) => {

		let html           = '';
		let readonlyHTML   = (isReadOnly ? ' readonly class="readonly"' : '');
		let inputTypeHTML  = (isPassword ? 'password' : 'text');
		let removeIconHTML = _Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['remove', 'icon-lightgrey']), 'Remove single value');

		for (let value of values) {

			if (isMultiline) {

				html += `<div class="array-attr relative"><textarea rows="4" name="${key}"${readonlyHTML} autocomplete="new-password">${value}</textarea>${removeIconHTML}</div>`;

			} else {

				html += `<div class="array-attr relative"><input name="${key}" type="${inputTypeHTML}" value="${value}"${readonlyHTML} autocomplete="new-password">${removeIconHTML}</div>`;
			}
		}

		if (isMultiline) {

			html += `<div class="array-attr"><textarea rows="4" name="${key}"${readonlyHTML} autocomplete="new-password"></textarea></div>`;

		} else {

			html += `<div class="array-attr"><input name="${key}" type="${inputTypeHTML}" value=""${readonlyHTML} autocomplete="new-password"></div>`;
		}

		return html;
	},
	formatRegularValueField: (key, value, isMultiline, isReadOnly, isPassword) => {

		if (isMultiline) {

			return `<textarea rows="4" name="${key}"${isReadOnly ? ' readonly class="readonly"' : ''} autocomplete="new-password">${value}</textarea>`;

		} else {

			return `<input name="${key}" type="${isPassword ? 'password' : 'text'}" value="${value}"${isReadOnly ? 'readonly class="readonly"' : ''} autocomplete="new-password">`;
		}
	},
	getElementDisplayName: (entity) => {
		if (!entity.name) {
			if (entity.tag === 'option' && entity._html_value) {
				return `${entity.tag}[value="${_Helpers.escapeForHtmlAttributes(entity._html_value)}"]`;
			}
			return (entity?.tag ?? `[${entity.type}]`);
		}
		if ((entity?.name ?? '').trim() === '') {
			return '(blank name)';
		}
		return entity.name;
	},
	getDateTimePickerFormat: (rawFormat) => {

		let dateTimeFormat;
		let obj = {};
		if (rawFormat.indexOf('T') > 0) {
			dateTimeFormat = rawFormat.split("'T'");
		} else {
			dateTimeFormat = rawFormat.split(' ');
		}
		let dateFormat = dateTimeFormat[0];
		obj.dateFormat = DateFormatConverter.convert(moment().toMomentFormatString(dateFormat), DateFormatConverter.momentJs, DateFormatConverter.datepicker);

		let timeFormat = dateTimeFormat.length > 1 ? dateTimeFormat[1] : undefined;
		if (timeFormat) {
			obj.timeFormat = DateFormatConverter.convert(moment().toMomentFormatString(timeFormat), DateFormatConverter.momentJs, DateFormatConverter.timepicker);
		}
		obj.separator = (rawFormat.indexOf('T') > 0) ? 'T' : ' ';
		return obj;
	},
	blinkGreen: (element) => {
		_Helpers.blink($(element), '#6db813', '#81ce25');
	},
	blinkRed: (element) => {
		_Helpers.blink($(element), '#a00', '#faa');
	},
	blink: (element, color, bgColor) => {

		if (!element) {
			return;
		} else if (!element.length) {
			element = $(element);

			if (!element.length) {
				return;
			}
		}

		let fg    = element.prop('data-fg-color');
		let oldFg = fg || element.css('color');
		let bg    = element.prop('data-bg-color');
		let oldBg = bg || element.css('backgroundColor');

		let hadNoForegroundStyle = (element[0].style.color === '');
		let hadNoBackgroundStyle = (element[0].style.backgroundColor === '');

		// otherwise hover states can mess with the restoration of the "previous" colors
		let handleElementsWithoutDirectStyle = () => {
			if (hadNoForegroundStyle) {
				element[0].style.color = '';
			}
			if (hadNoBackgroundStyle) {
				element[0].style.backgroundColor = '';
			}
		};

		if (!fg) {
			element.prop('data-fg-color', oldFg);
		}

		if (!bg) {
			element.prop('data-bg-color', oldBg);
		}

		if (element[0].nodeName === 'SELECT') {

			element.animate({
				color: color
			}, 50, function() {
				$(this).animate({
					color: oldFg
				}, 1000, handleElementsWithoutDirectStyle);
			});

		} else {

			element.animate({
				color: color,
				backgroundColor: bgColor
			}, 50, () => {
				element.animate({
					color: oldFg,
					backgroundColor: oldBg
				}, 1000, handleElementsWithoutDirectStyle);
			});
		}
	},
	isIn: (s, array) => {
		return (s && array && array.indexOf(s) !== -1);
	},
	urlParam: (name) => {
		return new URLSearchParams(location.search).get(name) ?? '';
	},
	formatKey: (text) => {
		// don't format custom 'data-*' attributes
		if (text.startsWith('data-')) {
			return text;
		}
		let result = '';
		for (let i = 0; i < text.length; i++) {
			let c = text.charAt(i);
			if (c === c.toUpperCase()) {
				result += ' ' + c;
			} else {
				result += (i === 0 ? c.toUpperCase() : c);
			}
		}
		return result;
	},
	formatBytes: (a, b= 2) => {

		const sizes = ["Bytes","KB","MB","GB","TB","PB","EB","ZB","YB"];

		if (0 === a) return "0 " + sizes[0];

		const c = (0 > b) ? 0 : b;
		const d = Math.floor(Math.log(a) / Math.log(1024));

		return parseFloat((a/Math.pow(1024,d)).toFixed(c)) + " " + sizes[d]
	},
	getErrorTextForStatusCode: (statusCode) => {
		switch (statusCode) {
			case 400: return 'Bad request';
			case 401: return 'Authentication required';
			case 403: return 'Forbidden';
			case 404: return 'Not found';
			case 422: return 'Unprocessable entity';
			case 500: return 'Internal Error';
			case 503: return 'Service Unavailable';
		}
	},
	appendInfoTextToElement: (config) => {

		let element            = $(config.element);
		let appendToElement    = config.appendToElement || element;
		let text               = config.text || 'No text supplied!';
		let toggleElementCss   = config.css || {};
		let toggleElementClass = config.class || undefined;
		let elementCss         = config.elementCss || {};
		let helpElementCss     = config.helpElementCss || {};
		let customToggleIcon   = config.customToggleIcon || _Icons.iconInfo;
		let customToggleIconClasses = config.customToggleIconClasses || ['icon-blue'];
		let insertAfter        = config.insertAfter || false;
		let offsetX            = config.offsetX || 0;
		let offsetY            = config.offsetY || 0;
		let width              = config.width || 12;
		let height             = config.height || 12;

		let createdElements = [];

		let customToggleElement = true;
		let toggleElement = config.toggleElement;
		if (!toggleElement) {
			customToggleElement = false;
			toggleElement = $(`
				${(config.noSpan) ? '' : '<span>'}
					${_Icons.getSvgIcon(customToggleIcon, width, height, _Icons.getSvgIconClassesForColoredIcon(customToggleIconClasses))}
				${(config.noSpan) ? '' : '</span>'}
			`);

			createdElements.push(toggleElement);
		}

		if (toggleElementClass) {
			toggleElement.addClass(toggleElementClass);
		}
		toggleElement.css(toggleElementCss);
		appendToElement.css(elementCss);

		let helpElement = $(`<span class="context-help-text">${text}</span>`);
		createdElements.push(helpElement);

		toggleElement
			.on("mousemove", function(e) {
				let isPinned = toggleElement[0].dataset['pinned'] ?? false;

				helpElement.show();

				if (!isPinned) {
					helpElement.css({
						left: Math.min(e.clientX + 20 + offsetX, window.innerWidth - helpElement.width() - 50),
						top: Math.min(e.clientY + 10 + offsetY, window.innerHeight - helpElement.height() - 10)
					});
				}
			}).on("mouseout", function(e) {
				let isPinned = toggleElement[0].dataset['pinned'] ?? false;

				if (!isPinned) {
					helpElement.hide();
				}
			});

		toggleElement[0].addEventListener('click', (e) => {
			e.preventDefault();

			let wasPinned = toggleElement[0].dataset['pinned'] ?? false;

			if (wasPinned) {
				delete toggleElement[0].dataset['pinned'];
				helpElement.hide();
				helpElement[0].querySelector('.pinned-info')?.remove();
			} else {
				helpElement.show();
				toggleElement[0].dataset['pinned'] = true;
				helpElement[0].insertAdjacentHTML('afterbegin', `<div class="pinned-info text-right">${_Icons.getSvgIcon('pin', 16, 16, [ '-mr-2', 'mb-1' ], 'Info Box is pinned, click info icon again to unpin.')}</div>`);
			}
		});

		if (insertAfter) {
			if (!customToggleElement) {
				element.after(toggleElement);
			}
			appendToElement.after(helpElement);
		} else {
			if (!customToggleElement) {
				element.append(toggleElement);
			}
			appendToElement.append(helpElement);
		}

		helpElement.css(helpElementCss);

		return createdElements;
	},
	activateCommentsInElement: (elem, defaults) => {

		let elementsWithComment = elem.querySelectorAll('[data-comment]') || [];

		for (let el of elementsWithComment) {

			if (!el.dataset['commentApplied']) {

				el.dataset.commentApplied = 'true';

				let config = {
					text: el.dataset['comment'],
					element: el,
					css: {
						'margin': '0 4px',
						//'vertical-align': 'top'
					}
				};

				let elCommentConfig = {};
				if (el.dataset['commentConfig']) {
					try {
						elCommentConfig = JSON.parse(el.dataset['commentConfig']);
					} catch (e) {
						console.log('Failed parsing comment config');
					}
				}

				// base config is overridden by the defaults parameter which is overridden by the element config
				let infoConfig = Object.assign(config, defaults, elCommentConfig);
				_Helpers.appendInfoTextToElement(infoConfig);
			}
		}
	},
	getDocumentationURLForTopic: (topic) => {

		switch (topic) {
			case 'security':       return 'https://docs.structr.com/docs/security';
			case 'schema-enum':    return 'https://docs.structr.com/docs/troubleshooting-guide#enum-property';
			case 'schema':         return 'https://docs.structr.com/docs/schema';
			case 'pages':          return 'https://docs.structr.com/docs/pages';
			case 'flows':          return 'https://docs.structr.com/docs/flow-engine---editor';
			case 'files':          return 'https://docs.structr.com/docs/files';
			case 'dashboard':      return 'https://docs.structr.com/docs/the-dashboard';
			case 'crud':           return 'https://docs.structr.com/docs/data';

			case 'contents':
			case 'mail-templates':
			case 'virtual-types':
			case 'localization':
			case 'graph':
			default:
				return 'https://docs.structr.com/';
		}
	},
	showAvailableIcons: () => {

		let { dialogText } = _Dialogs.custom.openDialog('Icons');

		dialogText.innerHTML = `<div>
			<h3>SVG Icons</h3>
			<table>
				${[...document.querySelectorAll('body > svg > symbol')].map(el => el.id).sort().map(id =>
				`<tr>
					<td>${id}</td>
					<td>${_Icons.getSvgIcon(id, 24, 24)}</td>
				</tr>`).join('')}
			</table>
		</div>`;
	},
	getPrefixedRootUrl: (rootUrl = '/structr/rest') => {

		let prefix = [];
		const pathEntries = window.location.pathname.split('/')?.filter( pathEntry => pathEntry !== '') ?? [];
		let entry = pathEntries.shift();

		while (entry !== 'structr' && entry !== undefined) {
			prefix.push(entry);
			entry = pathEntries.shift();
		}

		return `${(prefix.length ? '/' : '')}${prefix.join('/')}${rootUrl}`;
	},
	createSingleDOMElementFromHTML: (html) => {
		let elements = _Helpers.createDOMElementsFromHTML(html);
		return elements[0];
	},
	createDOMElementsFromHTML: (html) => {
		// use template element so we can create arbitrary HTML which is not parsed but not rendered (otherwise tr/td and some other elements would not work)
		let dummy = document.createElement('template');
		dummy.insertAdjacentHTML('beforeend', html);

		return dummy.children;
	},

	disableElement: (btn) => {
		btn.classList.add('disabled');
		btn.disabled = true;
	},
	enableElement: (btn) => {
		btn.classList.remove('disabled');
		btn.disabled = false;
	},
	disableElements: (disabled = false, ...elements) => {

		for (let element of elements) {

			if (element) {

				if (disabled === true) {
					_Helpers.disableElement(element);
				} else {
					_Helpers.enableElement(element);
				}
			}
		}
	},
	updateButtonWithSpinnerAndText: (btn, html) => {

		_Helpers.disableElement(btn);

		let icon = _Helpers.createSingleDOMElementFromHTML(_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 20, 20, 'ml-2'));
		btn.innerHTML = html;
		btn.appendChild(icon);
	},
	updateButtonWithSuccessIcon: (btn, html) => {

		let icon = _Helpers.createSingleDOMElementFromHTML(_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, ['tick', 'icon-green', 'ml-2']));
		btn.innerHTML = html;
		btn.appendChild(icon);

		window.setTimeout(() => {
			icon.remove();

			_Helpers.enableElement(btn);
		}, 1000);
	},
	isImage: (contentType) => (contentType && contentType.indexOf('image') > -1),
	isVideo: (contentType) => (contentType && contentType.indexOf('video') > -1),
	requestAnimationFrameWrapper: (key, callback) => {
		if (key) {
			cancelAnimationFrame(key);
		}

		key = requestAnimationFrame(callback);
	},
	sort: (collection, sortKey1, sortKey2) => {

		if (!sortKey1) {
			sortKey1 = "name";
		}

		collection.sort((a, b) => {

			let primarySortResult = ((a[sortKey1] > b[sortKey1]) ? 1 : ((a[sortKey1] < b[sortKey1]) ? -1 : 0));
			if (primarySortResult === 0 && sortKey2) {

				primarySortResult = ((a[sortKey2] > b[sortKey2]) ? 1 : ((a[sortKey2] < b[sortKey2]) ? -1 : 0));
			}

			return primarySortResult;
		});
	},
	debounce: (func, wait, immediate) => {
		var timeout;

		return function() {
			var context = this, args = arguments;
			var later = function() {
				timeout = null;
				if (!immediate) func.apply(context, args);
			};
			var callNow = immediate && !timeout;
			clearTimeout(timeout);
			timeout = setTimeout(later, wait);
			if (callNow) func.apply(context, args);
		};
	},
	disableNormalContextMenuOnElement: (element) => {

		element.addEventListener('contextmenu', (e) => {
			e.stopPropagation();
			e.preventDefault();
		});
	}
};

/**
 * thin wrapper for localStorage with a success-check and error display
 */
let LSWrapper = new (function() {

	let _localStorageObject = {};
	let _localStoragePersistenceKey = 'structrLocalStoragePersistence_';
	let _localStoragePersistenceDateKey = 'structrLocalStoragePersistenceDate_';
	let _localStorageLastSyncToServerKey = 'structrLocalStorageLastSyncToServer_';
	let _persistInterval = 60;

	let _lastLoadTimestamp = null;

	this.save = function (callback) {

		Command.saveLocalStorage(function () {

			localStorage.setItem(_localStorageLastSyncToServerKey, new Date().getTime());

			if (typeof callback === 'function') {
				callback();
			}
		});
	};

	this.restore = function (callback) {

		if (!this.isLoaded()) {

			if (this.isRecent()) {

				let success = this.restoreFromRealLocalStorage();

				if (success) {
					callback();
				} else {
					this.restoreFromServer(callback);
				}

			} else {
				this.restoreFromServer(callback);
			}

		} else {
			callback();
		}
	};

	this.isLoaded = function () {

		return !(!_localStorageObject || (Object.keys(_localStorageObject).length === 0 && _localStorageObject.constructor === Object));
	};

	this.setItem = function(key, value) {

		_localStorageObject[key] = value;

		this.persistToRealLocalStorage();
	};

	this.getItem = function (key, defaultValue = null) {

		let lastPersist = JSON.parse(localStorage.getItem(_localStoragePersistenceDateKey));
		if (_lastLoadTimestamp < lastPersist) {
			this.restoreFromRealLocalStorage();
		}

		return (_localStorageObject[key] === undefined) ? defaultValue : _localStorageObject[key];
	};

	this.removeItem = function (key) {

		delete _localStorageObject[key];

		this.persistToRealLocalStorage();
	};

	this.clear = function () {

		_localStorageObject = {};

		this.persistToRealLocalStorage();
	};

	this.getAsJSON = function () {

		return JSON.stringify(_localStorageObject);
	};

	this.restoreFromServer = function (callback) {

		Command.getLocalStorage(callback);
	};

	this.setAsJSON = function (json) {

		_localStorageObject = JSON.parse(json);
		_lastLoadTimestamp  = new Date().getTime();

		this.persistToRealLocalStorage();
	};

	this.isRecent = function () {

		let lastPersist = JSON.parse(localStorage.getItem(_localStoragePersistenceDateKey));

		if (!lastPersist) {

			return false;

		} else {

			return ((new Date().getTime() - lastPersist) <= (_persistInterval * 1000));
		}
	};

	this.persistToRealLocalStorage = function(retry = true) {

		try {

			let now = new Date().getTime();

			localStorage.setItem(_localStoragePersistenceKey, this.getAsJSON());
			localStorage.setItem(_localStoragePersistenceDateKey, now);

			let lastSyncTime = localStorage.getItem(_localStorageLastSyncToServerKey);

			if (!lastSyncTime || (now - lastSyncTime) > (_persistInterval * 1000)) {
				// send to server
				this.save();
			}

		} catch (e) {

			// localstorage failed. probably quota exceeded. prune and retry once
			if (retry === true) {

				this.prune();
				this.persistToRealLocalStorage(false);

			} else {
				Structr.error(`Failed to save localstorage. The following error occurred: <p>${e}</p>`, true);
			}
		}
	};

	this.restoreFromRealLocalStorage = function () {

		let lsContent = localStorage.getItem(_localStoragePersistenceKey);

		if (!lsContent) {
			return false;
		} else {

			_lastLoadTimestamp = new Date().getTime();

			_localStorageObject = JSON.parse(lsContent);
			return true;
		}
	};

	this.prune = function() {

		// if localstorage save fails, remove the following elements
		let pruneKeys = [
			'structrActiveEditTab',			// last selected properties tab for node
			'structrScrollInfoKey',			// scroll info in editor
			'structrTreeExpandedIds'		// expanded tree info for pages tree
		];

		let lsKeys = Object.keys(_localStorageObject);

		for (let lsKey of lsKeys) {

			for (let pruneKey of pruneKeys) {

				if (lsKey.indexOf(pruneKey) === 0) {
					delete _localStorageObject[lsKey];
				}
			}
		}
	};
});

/**
 * Encapsulated Console object so we can keep error-handling and console-code in one place
 */
let _Console = new (function() {

	// private variables
	let _terminal;
	let _initialized = false;
	let _consoleVisible = false;

	let consoleModeKey = 'structrConsoleModeKey_' + location.port;

	// public methods
	this.logoutAction = function() {
		_terminal?.reset();
		_initialized = false;
		_hideConsole();
	};

	this.initConsole = () => {

		if (_initialized) {
			return;
		}

		let storedMode = LSWrapper.getItem(consoleModeKey, 'JavaScript');

		// Get initial mode and prompt from backend
		// If backend sends no mode, use value from local storage
		Command.console(`Console.setMode('${storedMode}')`, storedMode, function(data) {

			let message = data.message;
			let mode = storedMode || data.data.mode;
			let prompt = data.data.prompt;
			let versionInfo = data.data.versionInfo;
			//console.log(message, mode, prompt, versionInfo);

			let consoleEl = $('#structr-console');
			_terminal = consoleEl.terminal(function(command, term) {
				if (command !== '') {
					try {
						_runCommand(command, mode, term);
					} catch (e) {
						term.error(new String(e));
					}
				} else {
					term.echo('');
				}
			}, {
				greetings: _getBanner() + 'Welcome to Structr (' + versionInfo + '). Use <Shift>+<Tab> to switch modes.',
				name: 'structr-console',
				height: 470,
				prompt: prompt + '> ',
				keydown: function(e) {

					let event = e.originalEvent;

					if (event.key === 'Tab' || event.keyCode === 9) {

						let term = _terminal;

						if (event.shiftKey === true) {

							switch (term.consoleMode) {

								case 'REST':
									mode = 'JavaScript';
									break;

								case 'JavaScript':
									mode = 'StructrScript';
									break;

								case 'StructrScript':
									mode = 'Cypher';
									break;

								case 'Cypher':
									mode = 'AdminShell';
									break;

								case 'AdminShell':
									mode = 'REST';
									break;
							}

							let line = `Console.setMode("${mode}")`;
							term.consoleMode = mode;
							LSWrapper.setItem(consoleModeKey, mode);

							_runCommand(line, mode, term);

							return false;
						}
					}
				},
				completion: function(lineToBeCompleted, callback) {
					Command.console(lineToBeCompleted, mode, (data) => {
						callback(data.data.commands);
					}, true);
				}
			});
			_terminal.consoleMode = mode;
			_terminal.set_prompt(prompt + '> ');
			_terminal.echo(message);

			_initialized = true;
		});
	};

	this.toggleConsole = function() {

		if (_consoleVisible === true) {
			_hideConsole();
		} else {
			_showConsole();
		}
	};

	// private methods
	let _getBanner = function() {
		return ''
		+ '        _                          _         \n'
		+ ' ____  | |_   ___   _   _   ____  | |_   ___ \n'
		+ '(  __| | __| |  _| | | | | |  __| | __| |  _|\n'
		+ ' \\ \\   | |   | |   | | | | | |    | |   | |\n'
		+ ' _\\ \\  | |_  | |   | |_| | | |__  | |_  | |\n'
		+ '|____) |___| |_|   |_____| |____| |___| |_|  \n\n';
	};

	let _showConsole = function() {
		if (StructrWS.user !== null) {
			_consoleVisible = true;
			_terminal.enable();
			$('#structr-console').slideDown('fast');
		}
	};

	let _hideConsole = function() {
		_consoleVisible = false;
		_terminal?.disable();
		$('#structr-console').slideUp('fast');
	};

	let _runCommand = function(command, mode, term) {

		if (!term) {
			term = _terminal;
		}

		Command.console(command, mode, function(data) {
			let prompt = data.data.prompt;
			if (prompt) {
				term.set_prompt(prompt + '> ');
			}
			let result = data.message;

			if (result !== undefined) {

				let isJSON = data.data?.isJSON ?? false;
				if (isJSON) {
					try {

						let data = JSON.parse(result);

						let errorText = Structr.getErrorMessageFromResponse(data, false);

						result = errorText;

					} catch (e) {
						result = new String(result);
					}
				}

				// prevent the output from being formatted and re-interpreted by term. ('[[1,2,3]]' leads to re-interpretation)
				let echoConfig = {
					exec: false,
					raw: true,
					finalize: (div) => {
						div.css('white-space', 'pre-wrap');	// prevent the output from being put on one line but also prevent overflow
						div.text(result);
					}
				};

				term.echo(result, echoConfig);
			}
		});
	};
});

/**
 * Encapsulated Favorites object
 */
let _Favorites = new (function () {

	// private variables
	let _favsVisible = false;
	let container;
	let menu;
	let favoritesTabKey;
	let text = '';

	this.initFavorites = () => {
		favoritesTabKey = 'structrFavoritesTab_' + location.port;
	};

	this.refreshFavorites = async () => {

		let favs = document.getElementById('structr-favorites');

		_Helpers.fastRemoveAllChildren(favs);

		favs.insertAdjacentHTML('beforeend', `
			<div id="favs-tabs" class="favs-tabs flex-grow flex flex-col">
				<ul id="fav-menu"></ul>
			</div>
		`);

		_Favorites.menu      = document.querySelector('#favs-tabs > #fav-menu');
		_Favorites.container = document.querySelector('#favs-tabs');

		let response = await fetch(`${Structr.rootUrl}me/favorites/fav`);

		if (response.ok) {

			let data = await response.json();

			if (data && data.result && data.result.length) {

				let allFavoriteIds = data.result.map(favorite => favorite.id);

				// first "forget" editors for those elements - otherwise we do not get updates
				for (let favoriteId of allFavoriteIds) {
					_Editors.disposeEditor(favoriteId, 'favoriteContent');
					_Editors.disposeEditorModel(favoriteId, 'favoriteContent');
				}

				for (let favorite of data.result) {

					let id = favorite.id;
					_Favorites.menu.insertAdjacentHTML('beforeend',`
						<li id="tab-${id}" data-id="${id}" class="button">${favorite.favoriteContext}&nbsp;&nbsp;
							${_Icons.getSvgIconWithID(`button-close-${id}`, _Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['remove', 'icon-grey']), 'Close')}
						</li>
					`);

					_Favorites.container.insertAdjacentHTML('beforeend',`
						<div class="fav-content hidden flex-col flex-grow" id="content-tab-${id}">
							<div class="fav-editor flex-grow" id="editor-${id}"></div>
							<div class="fav-buttons" id="buttons-${id}">
								<span class="editor-info"></span>
								<button id="saveFile-${id}" disabled="disabled" class="disabled">Save</button>
							</div>
						</div>
					`);

					let editorInfo = _Favorites.container.querySelector(`#buttons-${id} .editor-info`);
					_Editors.appendEditorOptionsElement(editorInfo);

					let dialogSaveButton = document.getElementById('saveFile-' + id);

					let initialText = favorite.favoriteContent || '';

					let favoriteEditorMonacoConfig = {
						preventDisposeForIds: allFavoriteIds,
						value: initialText,
						language: (favorite.type === 'File' ? _Files.getLanguageForFile(favorite) : favorite.favoriteContentType || 'text/plain'),
						lint: true,
						autocomplete: true,
						changeFn: (editor, entity) => {

							let disabled = (initialText === editor.getValue());
							_Helpers.disableElements(disabled, dialogSaveButton);
						},
						saveFn: (editor, entity) => {

							let newText = editor.getValue();
							if (initialText === newText) {
								return;
							}

							Command.setProperty(id, 'favoriteContent', newText, false, () => {

								let tabLink = _Favorites.menu.querySelector(`#tab-${id}`);
								_Helpers.blinkGreen(tabLink);
							});

							initialText = newText;
							_Helpers.disableElements(true, dialogSaveButton);
						}
					};

					let editor = _Editors.getMonacoEditor(favorite, 'favoriteContent', document.getElementById(`editor-${id}`), favoriteEditorMonacoConfig);

					dialogSaveButton.addEventListener('click', (e) => {
						e.preventDefault();
						e.stopPropagation();

						favoriteEditorMonacoConfig.saveFn(editor, favorite);
					});

					// prevent DELETE ajax call without relationship ID
					if (favorite.relationshipId && favorite.relationshipId.length === 32) {

						// close button
						document.getElementById(`button-close-${id}`).addEventListener('click', async (e) => {

							e.preventDefault();
							e.stopPropagation();

							let deleteResponse = await fetch(Structr.rootUrl + favorite.relationshipId, { method: 'DELETE' });

							if (deleteResponse.ok) {

								let tabLink = document.getElementById('tab-' + id);

								if (tabLink.classList.contains('active')) {

									if (tabLink.previousElementSibling) {
										tabLink.previousElementSibling.click();
									} else if (tabLink.nextElementSibling) {
										tabLink.nextElementSibling.click();
									}
								}

								tabLink.remove();
								document.getElementById('content-tab-' + id).remove();
							}

							return false;
						});
					}
				}


				let activeTabId = LSWrapper.getItem(_Favorites.favoritesTabKey);

				if (!activeTabId || !($('#tab-' + activeTabId)).length) {
					activeTabId = Structr.getIdFromPrefixIdString($('li:first-child', _Favorites.menu).prop('id'), 'tab-');
				}

				let activateTab = (tabId) => {

					for (let tabLink of _Favorites.menu.querySelectorAll('li')) {
						tabLink.classList.remove('active');
					}

					for (let contentTab of _Favorites.container.querySelectorAll('.fav-content')) {
						contentTab.classList.remove('flex');
						contentTab.classList.add('hidden');
					}

					_Favorites.menu.querySelector('li#tab-' + tabId).classList.add('active');

					let contentTab = _Favorites.container.querySelector('#content-tab-' + tabId);
					contentTab.classList.remove('hidden');
					contentTab.classList.add('flex');

					LSWrapper.setItem(_Favorites.favoritesTabKey, tabId);

					window.setTimeout(() => {
						_Editors.resizeVisibleEditors();
					}, 250);
				};

				for (let tab of _Favorites.menu.querySelectorAll('li')) {
					tab.addEventListener('click', (e) => {
						e.stopPropagation();

						activateTab(tab.dataset.id);
					});
				}

				activateTab(activeTabId);

			} else {
				_Favorites.container.append(' No favorites found');
			}
		}
	};

	// public methods
	this.logoutAction = function() {
		_Helpers.fastRemoveAllChildren(document.getElementById('structr-favorites'));
		_hideFavorites();
	};

	this.toggleFavorites = function() {
		if (_favsVisible === true) {
			_hideFavorites();
		} else {
			_showFavorites();
		}
	};

	let _showFavorites = function() {
		if (StructrWS.user !== null) {
			_favsVisible = true;
			$('#structr-favorites').slideDown('fast', () => {
				$('#structr-favorites').css('display', 'flex');
				_Favorites.refreshFavorites();
			});
		}
	};

	let _hideFavorites = function() {
		_favsVisible = false;
		$('#structr-favorites').slideUp('fast');
		_Helpers.fastRemoveAllChildren($('#structr-favorites')[0]);
	};
});

/**
 * A cache for async GET requests which ensures that certain requests are only made once - if used correctly.<br>
 * The cache has one argument - the fetch function - which handles fetching a single object.<br>
 * Upon successfully loading the object, it must be added to the cache via the `addObject` method.<br>
 * It is possible to attach callbacks to the ID we ware waiting for. After the object is loaded<br>
 * these callbacks will be executed and any further callbacks will be executed directly.
 *
 * @param {function} fetchFunction The function which handles fetching a single object - must take the ID as single parameter
 * @returns {AsyncObjectCache}*
 */
let AsyncObjectCache = function(fetchFunction) {

	let _cache = {};

	/**
	 * This methods registers a callback for a resource.<br>
	 * If the resource has not been requested before, the fetch function is executed.<br>
	 * If the resource has been requested before, the callback is added to the callbacks list.<br>
	 * If the result for the cacheId is present in the cache, the callback is executed directly.
	 *
	 * @param {object} resource The parameter to pass in the fetchFunction, typically { id: ..., type: ... }
	 * @param {string} cacheId The ID under which to cache the result
	 * @param {function} callback The callback to execute with the fetched object. Needs to take the object as single paramter.
	 */
	this.registerCallback = function(resource, cacheId, callback) {

		if (_cache[cacheId] === undefined) {

			_cache[cacheId] = {
				callbacks: [callback]
			};

			fetchFunction(resource);

		} else if (_cache[cacheId].value === undefined) {

			_cache[cacheId].callbacks.push(callback);

		} else {

			_runSingleCallback(cacheId, callback, true);
		}
	};

	/**
	 * This method adds a fetched object to the cache.<br>
	 * Callbacks associated with that object will be executed afterwards.
	 *
	 * @param {object} obj The object to store in the cache
	 * @param {string} cacheId The cache identifier for the object - usually a UUID, but also used as a URI
	 */
	this.addObject = function(obj, cacheId) {

		if (_cache[cacheId] === undefined) {

			// no registered callbacks - simply set the cache object
			_cache[cacheId] = {
				value: obj
			};

		} else if (_cache[cacheId].value === undefined) {

			// set the cache object and run all registered callbacks
			_cache[cacheId].value = obj;
			_runRegisteredCallbacks(cacheId);
		}
	};

	this.clear = function() {
		_cache = {};
	};

	function _runRegisteredCallbacks (cacheId) {

		if (_cache[cacheId] !== undefined && _cache[cacheId].callbacks) {

			_cache[cacheId].callbacks.forEach(function(callback) {
				_runSingleCallback(cacheId, callback, false);
			});

			_cache[cacheId].callbacks = [];
		}
	};

	function _runSingleCallback(cacheId, callback, cacheHit) {

		if (typeof callback === "function") {
			callback(_cache[cacheId].value, cacheHit);
		}
	};
};


// live binding helper with CSS selector
function live(selector, event, callback, context) {
	addEvent(context || document, event, function(e) {
		var qs = (context || document).querySelectorAll(selector);
		if (qs) {
			var el = e.target || e.srcElement, index = -1;
			while (el && ((index = Array.prototype.indexOf.call(qs, el)) === -1)) el = el.parentElement;
			if (index > -1) callback.call(el, e);
		}
	});
}
// helper for enabling IE 8 event bindings
function addEvent(el, type, handler) {
	if (el.attachEvent) el.attachEvent('on'+type, handler); else el.addEventListener(type, handler);
}
// matches polyfill
this.Element && function(ElementPrototype) {
	ElementPrototype.matches = ElementPrototype.matches ||
		ElementPrototype.matchesSelector ||
		ElementPrototype.webkitMatchesSelector ||
		ElementPrototype.msMatchesSelector ||
		function(selector) {
			var node = this, nodes = (node.parentNode || node.document).querySelectorAll(selector), i = -1;
			while (nodes[++i] && nodes[i] != node);
			return !!nodes[i];
		}
}(Element.prototype);