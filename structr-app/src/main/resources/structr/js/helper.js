/*
 * Copyright (C) 2010-2025 Structr GmbH
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
	expandNewline: (text) => {
		// Expand literal '\n' to newline, which is encoded as '\\n' in HTML attribute values.
		return text.replace(/\\\\n/g, '<br>');
	},
	capitalize: (string) => {
		return string.charAt(0).toUpperCase() + string.slice(1);
	},
	uuidRegexp: new RegExp('^[a-fA-F0-9]{32}$|^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$'),
	isUUID: (str) => _Helpers.uuidRegexp.test(str),
	formatValueInputField: (key, obj, propertyInfo) => {

		let isPassword  = (propertyInfo?.className === 'org.structr.core.property.PasswordProperty');
		let isReadOnly  = _Entities.readOnlyAttrs.includes(key) || (propertyInfo?.readOnly ?? false);
		let isMultiline = (propertyInfo?.format === 'multi-line');

		if (obj === undefined || obj === null) {

			return _Helpers.formatRegularValueField(key, '', isMultiline, isReadOnly, isPassword);

		} else if (obj.constructor === Object) {

			return _Entities.getRelatedNodeHTML(obj);

		} else if (obj.constructor === Array) {

			return _Helpers.formatArrayValueField(key, obj, propertyInfo);

		} else {

			return _Helpers.formatRegularValueField(key, _Helpers.escapeForHtmlAttributes(obj), isMultiline, isReadOnly, isPassword);
		}
	},
	formatArrayValueField: (key, values, propertyInfo) => {

		let isPassword  = (propertyInfo.className === 'org.structr.core.property.PasswordProperty');
		let isReadOnly  = _Entities.readOnlyAttrs.includes(key) || (propertyInfo.readOnly);
		let isMultiline = (propertyInfo.format === 'multi-line');
		let isBoolean   = (propertyInfo.type === 'Boolean[]');
		let isDate      = (propertyInfo.type === 'Date[]');

		let html           = '';
		let readonlyHTML   = (isReadOnly ? 'readonly' : '');
		let inputTypeHTML  = (isPassword ? 'password' : (isBoolean ? 'checkbox' : 'text'));
		let removeIconHTML = _Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['remove', 'icon-lightgrey']), 'Remove single value');

		let classes = [];

		if (isReadOnly) {
			classes.push('readonly');
		}
		if (isDate) {
			classes.push('input-datetime', 'pl-9');
		}

		for (let value of values) {

			if (isMultiline) {

				html += `<div class="array-attr"><textarea class="${classes.join(' ')}" rows="4" name="${key}" ${readonlyHTML} autocomplete="one-time-code">${value}</textarea>${removeIconHTML}</div>`;

			} else {

				if (isDate) {

					html += `<div class="array-attr relative"><input name="${key}" class="${classes.join(' ')}" type="text" size="26" value="${value}" autocomplete="one-time-code">${_Crud.helpers.getDateTimeIconHTML()}${removeIconHTML}</div>`;

				} else {

					let valueHTML = (isBoolean) ? ((value === true || value === 'true') ? ' checked' : '') : `value="${value}"`;

					html += `<div class="array-attr"><input type="${inputTypeHTML}" name="${key}" ${valueHTML} ${readonlyHTML} autocomplete="one-time-code">${removeIconHTML}</div>`;
				}
			}
		}

		let spacerHTML = '<div style="width:10px;"></div>';

		if (isMultiline) {

			html += `<div class="array-attr"><textarea rows="4" name="${key}" ${readonlyHTML} autocomplete="one-time-code" data-is-new="true"></textarea>${spacerHTML}</div>`;

		} else {

			if (isDate) {

				html += `<div class="array-attr relative"><input name="${key}" class="${classes.join(' ')}" type="text" size="26" autocomplete="one-time-code" data-is-new="true">${_Crud.helpers.getDateTimeIconHTML()}${spacerHTML}</div>`;

			} else {

				let valueHTML = (isBoolean) ? '' : 'value=""';

				html += `<div class="array-attr"><input name="${key}" type="${inputTypeHTML}" ${valueHTML} ${readonlyHTML} autocomplete="one-time-code" data-is-new="true">${spacerHTML}</div>`;
			}
		}

		return html;
	},
	formatRegularValueField: (key, value, isMultiline, isReadOnly, isPassword) => {

		if (isMultiline) {

			return `<textarea rows="4" name="${key}"${isReadOnly ? ' readonly class="readonly"' : ''} autocomplete="one-time-code">${value}</textarea>`;

		} else {

			return `<input name="${key}" type="${isPassword ? 'password' : 'text'}" value="${value}"${isReadOnly ? 'readonly class="readonly"' : ''} autocomplete="one-time-code">`;
		}
	},
	getHTMLTreeElementDisplayName: (entity) => {
		if (entity) {
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
		}
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
	getURLParameter: (name) => {
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
	formatBytes: (a, b = 2) => {

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
		let skipClick          = config?.skipClick ?? false;

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

		if (skipClick === false) {

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
		}

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
						'margin': '0 4px'
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
			case 'dashboard':      return '#docs:4-Admin%20User%20Interface/2-Dashboard.md';
			case 'pages':          return '#docs:4-Admin%20User%20Interface/3-Pages.md';
			case 'files':          return '#docs:4-Admin%20User%20Interface/4-Files.md';
			case 'security':       return '#docs:4-Admin%20User%20Interface/5-Security.md';
			case 'schema':         return '#docs:4-Admin%20User%20Interface/6-Schema.md';
			case 'code':           return '#docs:4-Admin%20User%20Interface/7-Code.md';
			case 'crud':           return '#docs:4-Admin%20User%20Interface/8-Data.md';
			case 'graph':          return '#docs:4-Admin%20User%20Interface/9-Graph.md';
			case 'flows':          return '#docs:4-Admin%20User%20Interface/10-Flows.md';
			case 'schema-enum':    return '#docs:troubleshooting-guide#enum-property';

			case 'contents':
			case 'mail-templates':
			case 'virtual-types':
			case 'localization':   return '#docs:5-Admin%20User%20Interface/13-Localization.md';
			case 'graph':          return '#docs:5-Admin%20User%20Interface/9-Graph.md';
			default:
				return '#docs';
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
	addClasses: (nodeList, classList) => {
		for (let node of nodeList) {
			node.classList.add(classList);
		}
	},
	removeClasses: (nodeList, classList) => {
		for (let node of nodeList) {
			node.classList.remove(classList);
		}
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
	},
	createRandomName: (type = 'object') => `New ${type} ${Math.floor(Math.random() * (999999 - 1))}`,
	softlimit: {
		resultCountSoftLimit: 10000,
		getSoftLimitedPageCount: (pageSize) => Math.ceil(_Helpers.softlimit.getSoftLimitedResultCount() / pageSize),
		getSoftLimitedResultCount: () => _Helpers.softlimit.resultCountSoftLimit,
		getSoftLimitMessage: () => 'Result count exceeds soft limit (' + _Helpers.softlimit.resultCountSoftLimit + '). Page count may be higher than displayed.',
		showSoftLimitAlert: (el) => {
			el.attr('style', 'background-color: #fc0 !important;');
			el.attr('title', _Helpers.softlimit.getSoftLimitMessage());
		},
		showActualResultCount: (el, pageSize) => {
			el.attr('title', 'Result count = ' + pageSize);
		},
	},
	isMac: () => (navigator.platform === 'MacIntel'),
	getHeadersForCustomView: (desiredProperties = []) => {

		let propertiesString = ((desiredProperties.length > 0) ? ` properties=${desiredProperties.join(',')}` : '');

		return {
			Accept: 'application/json; charset=utf-8;' + propertiesString
		}
	},
	getDataCommentAttributeForPropertyFromSchemaInfoHint: (key, typeInfo) => {

		let hint = typeInfo?.[key]?.hint;

		if (hint) {
			return `data-comment="${_Helpers.escapeForHtmlAttributes(hint)}"`;
		}

		return '';
	},
	getSchemaInformationPromise: async () => {
		return new Promise((resolve) => {
			fetch(Structr.rootUrl + '_schema').then(response => response.json()).then(schemaData => {
				resolve(schemaData.result);
			});
		});
	},
	getTimestampWithPrefix: (prefix) => {

		let date    = new Date();
		let year    = date.getFullYear();
		let month   = String(date.getMonth() + 1).padStart(2, '0');
		let day     = String(date.getDate()).padStart(2, '0');
		let hours   = String(date.getHours()).padStart(2, '0');
		let minutes = String(date.getMinutes()).padStart(2, '0');
		let seconds = String(date.getSeconds()).padStart(2, '0');

		return `${prefix}_${year}${month}${day}_${hours}${minutes}${seconds}`;
	},
	downloadFile: (content, name, contentType) => {

		const file = new File(content, name, { type: contentType });
		const link = document.createElement('a');
		const url  = URL.createObjectURL(file);

		link.href = url;
		link.download = file.name;
		document.body.appendChild(link);
		link.click();

		document.body.removeChild(link);
		window.URL.revokeObjectURL(url);
	},
	waitForElement: selector => {
		// Wait for element to appear. Usage example:
		// _Helpers.waitForElement('#main div.foo a.bar').then(el => {
		//     el.classList.add('active');
		// });

		return new Promise(resolve => {
			if (document.querySelector(selector)) {
				return resolve(document.querySelector(selector));
			}

			const observer = new MutationObserver(mutations => {
				if (document.querySelector(selector)) {
					observer.disconnect();
					resolve(document.querySelector(selector));
				}
			});

			observer.observe(document.body, { childList: true, subtree: true });
		});
	},
	waitForNode: id => {

		return new Promise(resolve => {
			let node = Structr.node(id);
			if (node) {
				resolve(node);

			} else {

				const observer = new MutationObserver(mutations => {
					let node = Structr.node(id);
					if (node) {
						observer.disconnect();
						resolve(node);
					}
				});

				observer.observe(document.body, { childList: true, subtree: true });
			}
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
	let _consoleModeKey = 'structrConsoleModeKey_' + location.port;
	let _initialized    = false;
	let _consoleVisible = false;
	let _modes          = ['JavaScript', 'StructrScript', 'Cypher', 'AdminShell', 'REST'];
	let _curMode        = 0;

	// public methods
	this.logoutAction = () => {
		_terminal?.reset();
		_initialized = false;
		_hideConsole();
	};

	this.initConsole = () => {

		if (_initialized) {
			return;
		}

		let storedMode = LSWrapper.getItem(_consoleModeKey, _modes[0]);

		// Get initial mode and prompt from backend
		// If backend sends no mode, use value from local storage
		Command.console(`Console.setMode('${storedMode}')`, storedMode, (data) => {

			let message     = data.message;
			let prompt      = data.data.prompt;
			let versionInfo = data.data.versionInfo;

			_curMode = _modes.indexOf(storedMode ?? data.data.mode) ?? 0;

			let consoleEl = $('#structr-console');
			_terminal = consoleEl.terminal((command, term) => {

				if (command !== '') {

					try {
						_runCommand(command, term);
					} catch (e) {
						term.error(new String(e));
					}

				} else {

					term.echo('');
				}

			}, {
				greetings: `${_getBanner()}\nWelcome to Structr (${versionInfo}). Use <Shift>+<Tab> to switch modes.`,
				name: 'structr-console',
				height: 470,
				prompt: prompt + '> ',
				keydown: (e) => {

					let event = e.originalEvent;

					if (event.shiftKey === true && (event.key === 'Tab' || event.keyCode === 9)) {

						_nextMode();

						return false;

					} else if (_Helpers.isMac() && event.altKey && (event.key === 'ArrowLeft' || event.key === 'ArrowRight')) {

						// allow cursor to jump words via Alt+Left/Right (on Mac)
						let curCmd     = _terminal.get_command();
						let curPos     = _terminal.get_position();
						let jmpIndexes = _getSplitIndexesInCommandForJumps(curCmd);
						let targetPos;

						if (event.key === 'ArrowLeft') {

							jmpIndexes = jmpIndexes.filter(i => i < curPos);
							targetPos  = jmpIndexes.pop() ?? 0;

						} else if (event.key === 'ArrowRight') {

							jmpIndexes = jmpIndexes.filter(i => i > curPos);
							targetPos  = jmpIndexes[0] ?? curCmd.length;
						}

						_terminal.set_position(targetPos);

						return false;

					} else if (_Helpers.isMac() && event.altKey && (event.key === 'Backspace' || event.key === 'Delete')) {

						// allow deletion of words via Alt+Backspace (on Mac)
						let curCmd     = _terminal.get_command();
						let curPos     = _terminal.get_position();
						let jmpIndexes = _getSplitIndexesInCommandForJumps(curCmd);
						let targetPos;
						let newCmd;

						if (event.key === 'Backspace') {

							jmpIndexes = jmpIndexes.filter(i => i < curPos);
							targetPos  = jmpIndexes.pop() ?? 0;
							newCmd     = curCmd.slice(0, targetPos) + curCmd.slice(curPos, curCmd.length);

						} else if (event.key === 'Delete') {

							jmpIndexes = jmpIndexes.filter(i => i > curPos);
							targetPos  = jmpIndexes[0] ?? curCmd.length;
							newCmd     = curCmd.slice(0, curPos) + curCmd.slice(targetPos, curCmd.length);
							targetPos  = curPos;
						}

						_terminal.set_command(newCmd)
						_terminal.set_position(targetPos);

						return false;
					}
				},
				completion: (lineToBeCompleted, callback) => {
					Command.console(lineToBeCompleted, _getCurMode(), (data) => {
						callback(data.data.commands);
					}, true);
				}
			});
			_terminal.consoleMode = _getCurMode();
			_terminal.set_prompt(prompt + '> ');
			_terminal.echo(message);

			_initialized = true;
		});
	};

	this.toggleConsole = () => (_consoleVisible === true) ? _hideConsole() : _showConsole();

	// private methods
	let _getBanner = () => {
		return ''
			+ '        _                          _         \n'
			+ ' ____  | |_   ___   _   _   ____  | |_   ___ \n'
			+ '(  __| | __| |  _| | | | | |  __| | __| |  _|\n'
			+ ' \\ \\   | |   | |   | | | | | |    | |   | |\n'
			+ ' _\\ \\  | |_  | |   | |_| | | |__  | |_  | |\n'
			+ '|____) |___| |_|   |_____| |____| |___| |_|  \n';
	};

	let _getCurMode = () => _modes[_curMode];

	let _nextMode = () => {

		_curMode = (_curMode + 1) % _modes.length;

		let mode = _getCurMode();

		LSWrapper.setItem(_consoleModeKey, _getCurMode());

		_terminal.consoleMode = mode;

		_runCommand(`Console.setMode("${mode}")`, _terminal);
	};

	// splits command by non-alphanumeric characters and returns all indexes to use in jump commands via keyboard navigation
	let _getSplitIndexesInCommandForJumps = (cmd) => [...cmd.matchAll(/[^a-zA-z0-9]/g)].map(match => match.index);

	let _showConsole = () => {
		if (StructrWS.user !== null) {
			_consoleVisible = true;
			_terminal.enable();
			$('#structr-console').slideDown('fast');
		}
	};

	let _hideConsole = () => {
		_consoleVisible = false;
		_terminal?.disable();
		$('#structr-console').slideUp('fast');
	};

	let _runCommand = (command, term) => {

		if (!term) {
			term = _terminal;
		}

		Command.console(command, _getCurMode(), (data) => {
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
	let favoritesDataKey     = 'structr_favorites_' + location.port;
	let favoritesLastUsedKey = 'structrFavoritesLastUsed_' + location.port;

	this.showFavorites = function() {

		if (_favsVisible !== true && StructrWS.user !== null) {

			_favsVisible = true;

			let favoritesIds = this.getFavoritesIds();
			let activeFileId = LSWrapper.getItem(favoritesLastUsedKey, favoritesIds[0]);

			_Files.editFiles(this.getFavoritesIds(), activeFileId, (id) => {
				LSWrapper.setItem(favoritesLastUsedKey, id);
			}, () => {
				_favsVisible = false;
			});
		}
	};

	this.getFavoritesIds = function() {
		return LSWrapper.getItem(favoritesDataKey, []);
	}

	this.getFavoritesList = async function() {

		let ids = this.getFavoritesIds();

		let res = await fetch(Structr.rootUrl + 'File?id=' + ids.join(';'), {
			headers: _Helpers.getHeadersForCustomView(_Files.defaultFileAttributes.split(','))
		});

		if (res.ok) {

			let data = await res.json();
			return data;
		}

		return [];
	};

	this.blinkFavoritesTreeItem = () => {
		_Helpers.blinkGreen(document.querySelector('li#favorites.jstree-node'))
	};

	this.addToFavorites = function(idsToAdd) {

		let idSet = new Set(this.getFavoritesIds());

		for (let id of idsToAdd) {

			idSet.add(id);
		}

		LSWrapper.setItem(favoritesDataKey, [...idSet]);

		this.blinkFavoritesTreeItem();
	};

	this.removeFromFavorites = function(idsToRemove) {

		let ids = LSWrapper.getItem(favoritesDataKey, []);
		let idSet = new Set(ids);

		for (let id of idsToRemove) {

			idSet.delete(id);
		}

		LSWrapper.setItem(favoritesDataKey, [...idSet]);

		this.blinkFavoritesTreeItem();
	};

	this.isFavoriteFile = function(id) {

		let ids = LSWrapper.getItem(favoritesDataKey, []);
		return ids.includes(id);
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
	}

	function _runSingleCallback(cacheId, callback, cacheHit) {

		if (typeof callback === "function") {
			callback(_cache[cacheId].value, cacheHit);
		}
	}
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