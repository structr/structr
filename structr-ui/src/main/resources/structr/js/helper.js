/*
 * Copyright (C) 2010-2021 Structr GmbH
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
function isIn(s, array) {
	return (s && array && array.indexOf(s) !== -1);
}

function escapeForHtmlAttributes(str, escapeWhitespace) {
	if (!(typeof str === 'string'))
		return str;
	var escapedStr = str
		.replace(/&/g, '&amp;')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;')
		.replace(/"/g, '&quot;')
		.replace(/'/g, '&#39;');

	return escapeWhitespace ? escapedStr.replace(/ /g, '&nbsp;') : escapedStr;
}

function escapeTags(str) {
	if (!str)
		return str;
	return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function unescapeTags(str) {
	if (!str)
		return str;
	return str
			.replace(/&nbsp;/g, ' ')
			.replace(/&amp;/g, '&')
			.replace(/&lt;/g, '<')
			.replace(/&gt;/g, '>')
			.replace(/&quot;/g, '"')
			.replace(/&#39;/g, '\'');
}

function utf8_to_b64(str) {
	return window.btoa(unescape(encodeURIComponent(str)));
}

// function b64_to_utf8(str) {
// 	return decodeURIComponent(escape(window.atob(str)));
// }

// $.fn.reverse = [].reverse;

if (typeof String.prototype.endsWith !== 'function') {
	String.prototype.endsWith = function(pattern) {
		var d = this.length - pattern.length;
		return d >= 0 && this.lastIndexOf(pattern) === d;
	};
}

if (typeof String.prototype.startsWith !== 'function') {
	String.prototype.startsWith = function(str) {
		return this.indexOf(str) === 0;
	};
}

if (typeof String.prototype.capitalize !== 'function') {
	String.prototype.capitalize = function() {
		return this.charAt(0).toUpperCase() + this.slice(1);
	};
}

if (typeof String.prototype.lpad !== 'function') {
	String.prototype.lpad = function(padString, length) {
		var str = this;
		while (str.length < length)
			str = padString + str;
		return str;
	};
}

if (typeof String.prototype.contains !== 'function') {
	String.prototype.contains = function(pattern) {
		return this.indexOf(pattern) > -1;
	};
}

if (typeof String.prototype.splitAndTitleize !== 'function') {
	String.prototype.splitAndTitleize = function(sep) {

		var res = new Array();
		var parts = this.split(sep);
		parts.forEach(function(part) {
			res.push(part.capitalize());
		});
		return res.join(" ");
	};
}

if (typeof String.prototype.extractVal !== 'function') {
	String.prototype.extractVal = function(key) {
		var pattern = '(' + key + '=")(.*?)"';
		var re = new RegExp(pattern);
		var value = this.match(re);
		return value && value[2] ? value[2] : undefined;
	};
}
/**
 * Clean text from contenteditable
 *
 * This function will remove any HTML markup and convert
 * any <br> tag into a line feed ('\n').
 */
function cleanText(input) {
	if (typeof input !== 'string') {
		return input;
	}
	var output = input
			.replace(/<div><br><\/div>/ig, '\n')
			.replace(/<div><\/div>/g, '\n')
			.replace(/<br(\s*)\/*>/ig, '\n')
			.replace(/(<([^>]+)>)/ig, "")
			.replace(/\u00A0/ig, String.fromCharCode(32));

	return output;

}

/**
 * Expand literal '\n' to newline,
 * which is encoded as '\\n' in HTML attribute values.
 */
function expandNewline(text) {
	var output = text.replace(/\\\\n/g, '<br>');
	return output;
}

var uuidRegexp = new RegExp('[a-fA-F0-9]{32}');
function isUUID (str) {
	return (str.length === 32 && uuidRegexp.test(str));
}

function shorten(uuid) {
	return uuid.substring(0, 8);
}

function urlParam(name) {
	name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
	var regexS = "[\\?&]" + name + "=([^&#]*)";
	var regex = new RegExp(regexS);
	var res = regex.exec(window.location.href);
	return (res && res.length ? res[1] : '');
}

function nvl(value, defaultValue) {
	var returnValue;
	if (value === undefined) {
		returnValue = defaultValue;
	} else if (value === false) {
		returnValue = 'false';
	} else if (value === 0) {
		returnValue = '0';
	} else if (!value) {
		returnValue = defaultValue;
	} else {
		returnValue = value;
	}
	return returnValue;
}

String.prototype.toCamel = function() {
	return this.replace(/(\-[a-z])/g, function(part) {
		return part.toUpperCase().replace('-', '');
	});
};

String.prototype.toUnderscore = function() {
	return this.replace(/([A-Z])/g, function(m, a, offset) {
		return (offset > 0 ? '_' : '') + m.toLowerCase();
	});
};

function formatValue(value) {

	if (value === undefined || value === null) {
		return '';
	}

	if (value.constructor === Object) {

		var out = '';
		Object.keys(value).forEach(function(key) {
			out += key + ': ' + formatValue(value[key]) + '\n';
		});
		return out;

	} else if (value.constructor === Array) {
		var out = '';
		value.forEach(function(val) {
			out += JSON.stringify(val);
		});
		return out;

	} else {
		return value;
	}
}

function blinkGreen(element) {
	blink($(element), '#6db813', '#81ce25');
}

function blinkRed(element) {
	blink($(element), '#a00', '#faa');
}

function blink(element, color, bgColor) {

	if (!element || !element.length) {
		return;
	}

	var fg = element.prop('data-fg-color'), oldFg = fg || element.css('color');
	var bg = element.prop('data-bg-color'), oldBg = bg || element.css('backgroundColor');

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
			}, 1000);
		});
	} else {

		element.animate({
			color: color,
			backgroundColor: bgColor
		}, 50, function() {
			$(this).animate({
				color: oldFg,
				backgroundColor: oldBg
			}, 1000);
		});
	}
}

function getDateTimePickerFormat(rawFormat) {
	var dateTimeFormat, obj = {};
	if (rawFormat.indexOf('T') > 0) {
		dateTimeFormat = rawFormat.split('\'T\'');
	} else {
		dateTimeFormat = rawFormat.split(' ');
	}
	var dateFormat = dateTimeFormat[0], timeFormat = dateTimeFormat.length > 1 ? dateTimeFormat[1] : undefined;
	obj.dateFormat = DateFormatConverter.convert(moment().toMomentFormatString(dateFormat), DateFormatConverter.momentJs, DateFormatConverter.datepicker);
	var timeFormat = dateTimeFormat.length > 1 ? dateTimeFormat[1] : undefined;
	if (timeFormat) {
		obj.timeFormat = DateFormatConverter.convert(moment().toMomentFormatString(timeFormat), DateFormatConverter.momentJs, DateFormatConverter.timepicker);
	}
	obj.separator = (rawFormat.indexOf('T') > 0) ? 'T' : ' ';
	return obj;
}

function getElementDisplayName(entity) {
	if (!entity.name) {
		if (entity.tag === 'option' && entity._html_value) {
			return (entity.tag + '[value="' + entity._html_value + '"]');
		}
		return (entity.tag ? entity.tag : '[' + entity.type + ']');
	}
	if (entity.name && (entity.name+'').trim() === '') {
		return '(blank name)';
	}
	return entity.name;
}

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
				Structr.error('Failed to save localstorage. The following error occurred: <p>' + e + '</p>', true);
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
			'activeFileTabPrefix',			// last selected properties tab for file
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

function fastRemoveAllChildren(el) {
	if (!el) return;
	while (el.firstChild) {
		el.removeChild(el.firstChild);
	}
}

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

					if (e.which === 9) {

						let term = _terminal;

						if (shiftKey) {

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

							let line = 'Console.setMode("' + mode + '")';
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
			document.body.classList.remove('console-open');
			_Console.removeHeaderBlocker();
			_hideConsole();
		} else {
			document.body.classList.add('console-open');
			_showConsole();
			_Console.insertHeaderBlocker();
		}
	};

	this.insertHeaderBlocker = function () {
		if (_consoleVisible === true && document.querySelector('.blockUI')) {
			Structr.header.appendChild(Structr.createSingleDOMElementFromHTML('<div id="header-blocker"></div>'));
		}
	};

	this.removeHeaderBlocker = function () {
		Structr.header.querySelector('#header-blocker')?.remove();
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

				// prevent the output from being formatted and re-interpreted by term. ('[[1,2,3]]' leads to re-interpretation)
				let echoConfig = {
					exec: false,
					raw: true,
					finalize: (div) => {
						div.css('white-space', 'pre-wrap');	// prevent the output from being put on one line but also prevent overflow
						div.text(result);
					}
				};

				term.echo( new String(result), echoConfig);
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

		fastRemoveAllChildren(favs);

		favs.insertAdjacentHTML('beforeend', `
			<div id="favs-tabs" class="favs-tabs flex-grow flex flex-col">
				<ul id="fav-menu"></ul>
			</div>
		`);

		_Favorites.menu      = document.querySelector('#favs-tabs > #fav-menu');
		_Favorites.container = document.querySelector('#favs-tabs');

		let response = await fetch(rootUrl + 'me/favorites/fav');

		if (response.ok) {

			let data = await response.json();

			if (data && data.result && data.result.length) {

				for (let favorite of data.result) {

					let id = favorite.id;
					_Favorites.menu.insertAdjacentHTML('beforeend',`
						<li id="tab-${id}" data-id="${id}" class="button">${favorite.favoriteContext}&nbsp;&nbsp;
							<i title="Close" id="button-close-${id}" class="${_Icons.getFullSpriteClass(_Icons.grey_cross_icon)}"></i>
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

					let editorElement = $('#editor-' + id);

					let initialText = favorite.favoriteContent || '';

					let favoriteEditorMonacoConfig = {
						value: initialText,
						language: (favorite.type === 'File' ? _Files.getLanguageForFile(favorite) : favorite.favoriteContentType || 'text/plain'),
						lint: true,
						autocomplete: true,
						changeFn: (editor, entity) => {
							if (initialText === editor.getValue()) {
								dialogSaveButton.disabled = true;
								dialogSaveButton.classList.add('disabled');
							} else {
								dialogSaveButton.disabled = false;
								dialogSaveButton.classList.remove('disabled');
							}
						},
						saveFn: (editor, entity) => {
							let newText = editor.getValue();
							if (initialText === newText) {
								return;
							}

							Command.setProperty(id, 'favoriteContent', newText, false, () => {

								let tabLink = _Favorites.menu.querySelector(`#tab-${id}`);
								blinkGreen(tabLink);
							});

							initialText = newText;
							dialogSaveButton.disabled = true;
							dialogSaveButton.classList.add('disabled');
						}
					};

					let editor = _Editors.getMonacoEditor(favorite, 'favoriteContent', editorElement, favoriteEditorMonacoConfig);

					dialogSaveButton.addEventListener('click', (e) => {
						e.preventDefault();
						e.stopPropagation();

						favoriteEditorMonacoConfig.saveFn(editor, favorite);
					});

					// prevent DELETE ajax call without relationship ID
					if (favorite.relationshipId && favorite.relationshipId.length === 32) {

						// close button
						document.getElementById('button-close-' + id).addEventListener('click', async (e) => {

							e.preventDefault();
							e.stopPropagation();

							let deleteResponse = await fetch(rootUrl + favorite.relationshipId, { method: 'DELETE' });

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
		fastRemoveAllChildren(document.getElementById('structr-favorites'));
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
		fastRemoveAllChildren($('#structr-favorites')[0]);
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
