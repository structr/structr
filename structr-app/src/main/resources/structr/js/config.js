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
document.addEventListener('DOMContentLoaded', () => {
	_Config.init();
});

window.addEventListener('load', () => {
	_Config.resize();
});

let _Editors = {
	// fake editors element to allow fastRemove to work normally
};

let _Config = {
	resize: () => {},
	init: () => {

		document.body.addEventListener('keyup', async (event) => {

			let keyCode = event.keyCode;
			let code    = event.code;

			if (code === 'Escape' || code === 'Esc' || keyCode === 27) {

				await _Dialogs.custom.checkSaveOrCloseOnEscapeKeyPressed();
			}

			return false;
		});

		_Icons.preloadSVGIcons();

		window.addEventListener('resize', _Config.resize);

		if (document.body.classList.contains('login')) {

			_Dialogs.configLoginDialog.show();

			_Config.attachActiveSectionStoringBehaviourToForm(document.getElementById('login-form'));

		} else {

			_Config.attachActiveSectionStoringBehaviourToForm(document.getElementById('logout-form'));

			document.querySelector('#new-entry-button')?.addEventListener('click', () => {
				_Config.createNewEntry();
			});

			_Config.databaseConnections.init();

			let configureSuperuserCredentialsFormContainer = document.getElementById('superuser-credentials-form');
			if (configureSuperuserCredentialsFormContainer) {
				configureSuperuserCredentialsFormContainer.innerHTML = _Config.templates.configureSuperuserForm;
			}

			// react to invalid form elements
			let configForm = document.querySelector('form.config-form');
			configForm.noValidate = true;
			configForm.addEventListener('submit', (e) => {

				if (!configForm.checkValidity()) {
					e.preventDefault();

					for (let formGroup of document.querySelectorAll('.form-group')) {
						formGroup.classList.remove('invalid');
					}

					let invalidFields = [...configForm.querySelectorAll(':invalid')];

					for (let invalidField of invalidFields) {

						invalidField.closest('.form-group')?.classList.add('invalid');
						invalidField.closest('.config-group')?.classList.add('invalid');
						invalidField.closest('.tab-content')?.classList.add('invalid');
					}

					let anyVisible = invalidFields.some(f => f.offsetParent);

					if (!anyVisible) {

						let tabId = invalidFields[0].closest('.tab-content').id;
						location.hash = '#' + tabId;

						window.setTimeout(() => {
							configForm.reportValidity();
						}, 100);
					}

					configForm.reportValidity();
				}
			});

			let getActiveTab = () => document.querySelector('#active_section').value;

			for (let resetButton of document.querySelectorAll('.reset-key')) {

				resetButton.addEventListener('click', () => {

					let key = resetButton.dataset['key'];

					window.location.href = `${_Helpers.getPrefixedRootUrl('/structr/config')}?reset=${key}${getActiveTab()}`;
				});
			}

			document.querySelector('#reload-config-button')?.addEventListener('click', () => {
				window.location.href = `${_Helpers.getPrefixedRootUrl('/structr/config')}?reload${getActiveTab()}`;
			});

			let activateHash = (hash) => {

				document.querySelectorAll('#configTabs li').forEach(el => el.classList.remove('active'));
				document.querySelectorAll('.tab-content').forEach(el => {
					el.style.display = null
				});
				document.querySelector(`[href="${hash}"]`)?.parentNode.classList.add('active');
				document.querySelector('#active_section').value = hash;
				document.querySelector(hash).style.display = 'block';
			};

			window.addEventListener('hashchange', (e) => {
				activateHash(location.hash);
			});

			_Helpers.activateCommentsInElement(document, { css: '', helpElementCss: { maxWidth: '700px' } });

			let anchor = (new URL(window.location.href)).hash || '#general';
			if (!document.querySelector(anchor)) {
				// if selected anchor does not exist, activate the first tab
				anchor = document.querySelector('.tabs-menu li a')?.getAttribute('href') ?? '#welcome';
			}
			activateHash(anchor);

			let toggleButtonClicked = (button) => {

				let target = document.querySelector(`#${button.dataset['target']}`);
				if (target) {

					let value = button.dataset['value'];
					let list  = target.value;
					let parts = list.split(" ");

					// remove empty elements
					parts = parts.filter(p => (p.length >= 2));

					let pos = parts.indexOf(value);
					if (pos >= 0) {

						parts.splice(pos, 1);
						button.classList.remove('active');

					} else {

						parts.push(value);
						button.classList.add('active');
					}

					target.value = parts.filter(e => (e && e.length)).join(' ');
				}
			};

			for (let button of document.querySelectorAll('button.toggle-option')) {

				button.addEventListener('click', () => {
					toggleButtonClicked(button);
				});
			}

			_Config.cron.init();
			_Search.init();
		}
	},
	attachActiveSectionStoringBehaviourToForm: (form) => {
		form?.addEventListener('submit', e => {
			e.target.querySelector('[name="active_section"]').value = location.hash;
		});
	},
	createNewEntry: (text = 'Enter a key for the new configuration entry', suffix = '') => {

		_Dialogs.getArbitraryInputFromUser.showPromise(text, async (userInput) => {

			let newKey = userInput.trim() + suffix;

			let validationResult = {
				allow: true,
				value: newKey
			};

			let existing = document.querySelector(`[name="${CSS.escape(newKey)}"]`);

			if (existing) {

				validationResult.allow = false;
				validationResult.invalidMessage = 'A configuration setting with that key already exists.';
			}

			return validationResult;

		}).then(async (name) => {

			let targetTabId = (name.endsWith(_Config.cron.cronSuffix)) ? 'cron' : 'misc';

			location.href = '#' + targetTabId;

			let lastGroupInTargetTab = document.querySelector(`div.tab-content#${targetTabId} .config-group:last-of-type`);

			let newEntry = _Helpers.createSingleDOMElementFromHTML(`
				<div class="form-group">
					<label class="font-bold basis-full sm:basis-auto sm:min-w-128">${name}</label>
					<div class="flex items-center">
						<input type="text" name="${name}">
					</div>
				</div>
			`);

			let input = newEntry.querySelector('input');

			let resetButton = document.querySelector('svg.reset-key')?.cloneNode(true);
			if (resetButton) {
				delete resetButton.dataset.key;
				resetButton.title = 'Remove custom entry';

				input.insertAdjacentElement('afterend', resetButton);

				resetButton.addEventListener('click', e => {
					e.target.closest('.form-group').remove();
				});
			}

			lastGroupInTargetTab.appendChild(newEntry);

			window.setTimeout(() => {
				input.focus();
			}, 100);

			_Config.cron.initPotentialNewCronExpressionSetting(newEntry, name);
		}).catch(e => {
			if (typeof e !== 'string') {
				console.warn(e);
			}
		});
	},
	cron: {
		cronSuffix: '.cronExpression',
		isCronExpressionSetting: (name) => name.endsWith(_Config.cron.cronSuffix),
		getCronInfoText: () => document.querySelector('#cron-info-text').dataset['value'],
		getValidationMessage: () => {

			let validationMessage = _Config.cron.getCronInfoText().replace('<pre>', '');
			validationMessage     = _Helpers.unescapeTags(validationMessage.slice(0, validationMessage.indexOf('</pre>')));

			return validationMessage;
		},
		init: () => {

			let cronElement = document.querySelector('#cron');

			let addNewButton = _Helpers.createSingleDOMElementFromHTML('<button class="active:border-green focus:border-gray-666 hover:bg-gray-100 mr-0 mt-6" type="button">Add Cron expression</button>');
			addNewButton.addEventListener('click', () => {
				_Config.createNewEntry('Enter key for new cron expression (".cronExpression" is appended automatically)', _Config.cron.cronSuffix);
			});

			cronElement?.insertAdjacentElement('beforeend', addNewButton);

			_Config.cron.initExistingCronExpressionSettings();
		},
		initExistingCronExpressionSettings: () => {

			let cronExpressionInputs = document.querySelectorAll(`input[name*="${_Config.cron.cronSuffix}"]`);

			for (let input of cronExpressionInputs) {

				let container = input.closest('.form-group');
				container.querySelector('label').dataset['comment'] = _Config.cron.getCronInfoText();

				_Helpers.activateCommentsInElement(container);

				_Config.cron.addCronSpecificFunctionality(input);
			}
		},
		initPotentialNewCronExpressionSetting: (container, name) => {

			if (_Config.cron.isCronExpressionSetting(name)) {

				container.querySelector('label').dataset['comment'] = _Config.cron.getCronInfoText();

				_Helpers.activateCommentsInElement(container);

				let input = container.querySelector(`input[name="${name}"]`);
				_Config.cron.addCronSpecificFunctionality(input);

				input.reportValidity();
			}
		},
		addCronSpecificFunctionality: (input) => {

			// re-arrange elements (to allow us to add the description element) by moving the current container to the empty space in the newParent
			let newParent = _Helpers.createSingleDOMElementFromHTML(`
				<div class="flex flex-col w-full">

					<div class="mt-1 mb-4 text-sm text-gray-999" data-cron-description></div>
				</div>
			`);

			input.parentElement.parentElement.appendChild(newParent);
			newParent.insertAdjacentElement('afterbegin', input.parentElement);

			input.required = true;
			input.pattern  = '([^ \\\\t]+[ \\\\t]){5}[^ \\\\t]+';

			let validationMessage = _Config.cron.getValidationMessage();

			let showCronInfoText = (text) => {
				newParent.querySelector('[data-cron-description]').textContent = text;
			};

			let describeCronExpression = () => {
				try {
					return window.cronstrue.toString(input.value);
				} catch (e) {
					return e;
				}
			};

			let updateDescription = () => {

				let basicValidity       = !input.validity.patternMismatch && !input.validity.valueMissing;
				let hasUnsupportedChars = (input.value.replaceAll(/[\- 0-9,*/]/g, '').length > 0);

				if (basicValidity && !hasUnsupportedChars) {
					showCronInfoText(describeCronExpression());
				} else {
					showCronInfoText(validationMessage);
				}
			};

			updateDescription();

			input.addEventListener('input', () => {
				updateDescription();
			});
		}
	},
	databaseConnections: {
		loadingMessageId: 'config-database-loading',
		init: () => {

			let createNewConnectionButton = document.querySelector('#create-db-connection-button');
			let showAddConnectionPlusButton = document.querySelector('.show-add-connection');

			createNewConnectionButton?.addEventListener('click', () => {
				showAddConnectionPlusButton.dispatchEvent(new Event('click'))
			});

			document.querySelector('.show-add-connection').addEventListener('click', (event) => {

				let connectionPanel = event.target.closest('.connection');

				let newConnPasswordInput = connectionPanel.querySelector('#password-structr-new-connection');

				let newParent = _Helpers.createSingleDOMElementFromHTML('<div class="relative"></div>');

				newConnPasswordInput.parentNode.appendChild(newParent);
				newParent.appendChild(newConnPasswordInput);

				let showPasswordIcon = `
					<div class="absolute flex h-full items-center right-2 top-0">
						${_Icons.getSvgIcon(_Icons.iconEyeOpen, 22, 22, _Icons.getSvgIconClassesForColoredIcon(['icon-dark-blue', 'cursor-pointer']), 'Show password')}
					</div>
				`;

				newParent.insertAdjacentHTML('beforeend', showPasswordIcon);
				newParent.querySelector('svg').addEventListener('click', (e) => {
					switch (newConnPasswordInput.type) {
						case "text":
							newConnPasswordInput.type = 'password';
							_Icons.updateSvgIconInElement(e.target.closest('svg'), _Icons.iconEyeStrikeThrough, _Icons.iconEyeOpen);
							break;
						case "password":
							newConnPasswordInput.type = 'text';
							_Icons.updateSvgIconInElement(e.target.closest('svg'), _Icons.iconEyeOpen, _Icons.iconEyeStrikeThrough);
							break;
					}
				})

				connectionPanel.classList.remove('collapsed');

				_Helpers.fastRemoveElement(event.target.closest('.show-add-connection'));
			});

			let addConnectionButton = document.querySelector('#add-connection');
			addConnectionButton.addEventListener('click', () => {
				_Config.databaseConnections.addConnection(addConnectionButton);
			});

			document.querySelector('#set-neo4j-defaults').addEventListener('click', _Config.databaseConnections.setNeo4jDefaults);

			for (let deleteButton of document.querySelectorAll('.delete-connection[data-connection-name]')) {

				deleteButton.addEventListener('click', (e) => {
					_Config.databaseConnections.deleteConnection(deleteButton.dataset.connectionName);
				});
			}

			for (let connectButton of document.querySelectorAll('.connect-connection[data-connection-name]')) {

				connectButton.addEventListener('click', (e) => {
					_Config.databaseConnections.connect(connectButton, connectButton.dataset.connectionName);
				});
			}

			for (let disconnectButton of document.querySelectorAll('.disconnect-connection[data-connection-name]')) {

				disconnectButton.addEventListener('click', (e) => {
					_Config.databaseConnections.disconnect(disconnectButton, disconnectButton.dataset.connectionName);
				});
			}
		},
		collectData: (name) => {

			if (!name) {
				name = 'structr-new-connection';
			}

			let nameInput    = $('input#name-' + name);
			let driverSelect = $('select#driver-' + name);
			let urlInput     = $('input#url-' + name);
			let dbNameInput  = $('input#database-' + name);
			let userInput    = $('input#username-' + name);
			let pwdInput     = $('input#password-' + name);
			let nowCheckbox  = $('input#connect-checkbox');

			nameInput.closest('p').removeClass();
			driverSelect.closest('p').removeClass();
			urlInput.closest('p').removeClass();
			userInput.closest('p').removeClass();
			pwdInput.closest('p').removeClass();

			let data = {
				name:     nameInput.val(),
				driver:   driverSelect.val(),
				url:      urlInput.val(),
				database: dbNameInput.val(),
				username: userInput.val(),
				password: pwdInput.val(),
				now:      nowCheckbox && nowCheckbox.is(':checked'),
				active_section: '#databases'
			};

			return data;
		},
		addConnection: (button) => {

			let name = 'structr-new-connection';
			let data = _Config.databaseConnections.collectData();

			button.dataset.text = button.innerHTML;
			button.disabled     = true;

			if (data.now) {
				button.innerHTML = 'Connecting..';
			}

			let status = $('div#status-' + name);
			status.addClass('hidden');
			status.empty();

			if (data.now) {
				_Dialogs.loadingMessage.show('Connection is being established', 'Please wait...', _Config.databaseConnections.loadingMessageId);
			}

			fetch(_Helpers.getPrefixedRootUrl('/structr/config/add'), {
				method: 'POST',
				body: _Config.databaseConnections.convertObjectToFormEncoded(data),
				headers: {
					'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
				}
			}).then(async response => {
				if (response.ok || response.status === 302) {
					_Config.databaseConnections.reload();
				} else {
					await _Config.databaseConnections.handleErrorResponse(name, response, button);
				}
			});
		},
		convertObjectToFormEncoded: (obj) => Object.entries(obj).map(([key, value]) => `${key}=${encodeURIComponent(value)}`).join('&'),
		deleteConnection: (name) => {

			fetch(`${_Helpers.getPrefixedRootUrl('/structr/config/')}${name}/delete`, {
				method: 'POST',
				body: _Config.databaseConnections.convertObjectToFormEncoded({'active_section': '#databases'}),
				headers: {
					'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
				}
			}).then(async response => {
				if (response.ok || response.status === 302) {
					_Config.databaseConnections.reload();
				} else {
					await _Config.databaseConnections.handleErrorResponse(name, response);
				}
			});
		},
		setNeo4jDefaults: () => {

			document.querySelector('#driver-structr-new-connection').value   = 'org.structr.bolt.BoltDatabaseService';
			document.querySelector('#name-structr-new-connection').value     = 'neo4j-localhost-7687';
			document.querySelector('#url-structr-new-connection').value      = 'bolt://localhost:7687';
			document.querySelector('#database-structr-new-connection').value = 'neo4j';
			document.querySelector('#username-structr-new-connection').value = 'neo4j';
			document.querySelector('#password-structr-new-connection').value = 'adminneo4j';
		},
		saveConnection: (name) => {

			let data = _Config.databaseConnections.collectData(name);

			if (data.now) {
				_Dialogs.loadingMessage.show('Database connection is being established', 'Please wait...', _Config.databaseConnections.loadingMessageId);
			}

			fetch(`${_Helpers.getPrefixedRootUrl('/structr/config/')}${name}/use`, {
				method: 'POST',
				body: _Config.databaseConnections.convertObjectToFormEncoded(data),
				headers: {
					'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
				}
			}).then(async response => {
				if (response.ok || response.status === 302) {
					_Config.databaseConnections.reload();
				} else {
					await _Config.databaseConnections.handleErrorResponse(name, response);
				}
			});
		},
		reload: () => {

			_Dialogs.loadingMessage.hide(_Config.databaseConnections.loadingMessageId);

			window.location.href = _Helpers.getPrefixedRootUrl('/structr/config#databases');
			window.location.reload(true);
		},
		connect: (button, name) => {

			button.disabled     = true;
			button.dataset.text = button.innerHTML;
			button.innerHTML    = 'Connecting..';

			let status = $(`div#status-${name}`);
			status.addClass('hidden');
			status.empty();

			_Dialogs.loadingMessage.show('Database connection is being established', 'Please wait...', _Config.databaseConnections.loadingMessageId);

			fetch(`${_Helpers.getPrefixedRootUrl('/structr/config/')}${name}/connect`, {
				method: 'POST',
				body: _Config.databaseConnections.convertObjectToFormEncoded(_Config.databaseConnections.collectData(name)),
				headers: {
					'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
				}
			}).then(async response => {
				if (response.ok || response.status === 302) {
					_Config.databaseConnections.reload();
				} else {
					await _Config.databaseConnections.handleErrorResponse(name, response, button);
				}
			});
		},
		disconnect: (button, name) => {

			button.disabled = true;
			button.dataset.text = button.innerHTML;
			button.innerHTML = 'Disconnecting..';

			let status = $('div#status-' + name);
			status.addClass('hidden');
			status.empty();

			_Dialogs.loadingMessage.show('Database is being disconnected', 'Please wait...', _Config.databaseConnections.loadingMessageId);

			fetch(`${_Helpers.getPrefixedRootUrl('/structr/config/')}${name}/disconnect`, {
				method: 'POST',
				body: _Config.databaseConnections.convertObjectToFormEncoded(_Config.databaseConnections.collectData(name)),
				headers: {
					'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
				}
			}).then(async response => {
				if (response.ok || response.status === 302) {
					_Config.databaseConnections.reload();
				} else {
					await _Config.databaseConnections.handleErrorResponse(name, response, button);
				}
			});
		},
		handleErrorResponse: async (name, response, button) => {

			_Dialogs.loadingMessage.hide(_Config.databaseConnections.loadingMessageId);

			let json = await response.json();

			if (!name) {
				name = 'structr-new-connection';
			}

			if (button) {
				button.disabled  = false;
				button.innerHTML = button.dataset.text;
			}

			switch (response.status) {

				case 422:
					if (json.errors && json.errors.length) {

						json.errors.forEach(t => {
							if (t.property !== undefined && t.token !== undefined) {
								$(`input#${t.property}-${name}`).closest('p').addClass(t.token);
							}
						});

					} else {

						let status = $('div#status-' + name);
						status.empty();
						status.append(json.message);
						status.removeClass('hidden');
					}
					break;

				case 503:
					let status = $('div#status-' + name);
					status.empty();
					status.append(json.message);
					status.removeClass('hidden');
					break;
			}
		},
	},
	templates: {
		configLoginDialogMarkup: `
			<div id="login" class="dialog p-8 text-left" style="max-width: 480px; border: 2px solid #c62828; border-radius: 8px; box-shadow: 0 8px 32px rgba(0,0,0,0.3);">

				<div style="display: flex; align-items: center; gap: 12px; margin-bottom: 16px;">
					${_Icons.getSvgIcon(_Icons.iconStructrLogo, 90, 24, ['logo-login'])}
					<span style="font-size: 11px; font-weight: 600; color: #c62828; text-transform: uppercase; letter-spacing: 0.5px;">System Configuration</span>
				</div>

				<form id="login-form" method="post">

					<div id="username-password" class="gap-y-2 grid ml-1 mr-4 mt-4" style="grid-template-columns: 35fr 65fr;">

						<input name="active_section" type="hidden">

						<div class="self-center">
							<label for="superuserNameField">Username:</label>
						</div>

						<div class="self-center">
							<input id="superuserNameField" type="text" name="superuserName" autocomplete="username" required class="w-full box-border">
						</div>

						<div class="self-center">
							<label for="superuserPasswordField">Password:</label>
						</div>

						<div class="self-center">
							<input id="superuserPasswordField" type="password" name="superuserPassword" autocomplete="current-password" required class="w-full box-border">
						</div>

						<div class="self-center col-span-2 mt-2 text-right">
							<button id="loginButton" name="login" class="inline-flex mr-0 items-center hover:bg-gray-100 focus:border-gray-666 active:border-green" style="font-weight: 600;">
								${_Icons.getSvgIcon(_Icons.iconVisibilityKey, 16, 16, ['mr-2'])} Sign In
							</button>

							<input type="hidden" name="action" value="login">
						</div>

						<div class="col-span-2 mt-2 text-red font-medium ${new URLSearchParams(location.search).has('loginFailed') ? '' : 'hidden'}">
							Invalid username or password.
						</div>
					</div>
				</form>
			</div>
		`,
		configureSuperuserForm: `
			<div>
				<div>
					<div data-comment="Select the superuser name carefully.<br><br>This username shadows any regular user account with the same name. During authentication, the superuser credentials are checked first. If the username matches the configured superuser name but the password does not match the configured password, authentication fails immediately with an “access denied” error, even if a regular user with that username exists and has a different password.<br><br>When set to empty string, the superuser can not be used, not even in this configuration interface.">
						<h3 class="inline-block">Superuser credentials</h3>
					</div>
					<div class="flex">
						<div class="flex flex-col gap-2">
							<input name="superuser.username" type="text" size="40" placeholder="Enter a superuser name" value="superadmin">
							<input name="superuser.password" type="password" size="40" placeholder="Enter a superuser password">
							<div class="flex justify-end">
								<input type="submit" value="Save" class="">
							</div>
						</div>
					</div>
				</div>
			</div>
		`
	}
};

/* config search */
let _Search = {
	hitClass: 'search-matches',
	noHitClass: 'no-search-match',
	noHitAtAllClass: 'no-search-results',
	lsSearchStringKey: 'structrConfigSearchKey',
	containsIgnoreCase: (haystack, needle) => {
    	return haystack.toLowerCase().includes(needle.toLowerCase());
    },
	init: () => {

    	let isLogin   = document.getElementById('login');
    	let isWelcome = document.getElementById('welcome');

    	if (!isLogin && !isWelcome) {

			let searchUiHTML = _Helpers.createSingleDOMElementFromHTML(`
				<div id="search-container" class="w-1/3">
					<input id="search-box" autofocus="true" placeholder="Search config..." class="w-full box-border">
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 12, 12, _Icons.getSvgIconClassesForColoredIcon(['clearSearchIcon', 'icon-lightgrey', 'cursor-pointer']), 'Clear Search')}
				</div>
			`);

			document.querySelector('#header svg').insertAdjacentElement('afterend', searchUiHTML);

			let searchBox       = searchUiHTML.querySelector('input#search-box');
			let clearSearchIcon = searchUiHTML.querySelector('.clearSearchIcon');
    		let searchTimeout;

    		let lastSearch = window.localStorage.getItem(_Search.lsSearchStringKey);
    		if (lastSearch) {
    		    searchBox.value = lastSearch;
    		    _Search.doSearch(lastSearch);

    		    clearSearchIcon.classList.add('block');
    		}

    		let clearSearch = () => {
				_Search.clearSearch();
				searchBox.value = '';
				window.localStorage.removeItem(_Search.lsSearchStringKey);

				clearSearchIcon.classList.remove('block');
			};

			clearSearchIcon.addEventListener('click', () => {
				clearSearch();
			});

			searchBox.addEventListener('keyup', (e) => {

				if (e.code === 'Escape' || e.keyCode === 27) {

					clearSearch();

				} else {

					window.clearTimeout(searchTimeout);

					searchTimeout = window.setTimeout(() => {

						let q = searchBox.value;

						if (q.length === 0) {

							clearSearch();

						} else if (q.length >= 2) {

							_Search.doSearch(searchBox.value);
							clearSearchIcon.classList.add('block');
							window.localStorage.setItem(_Search.lsSearchStringKey, searchBox.value);
						}
					}, 250);
				}
			});

    		document.addEventListener("keydown",function (e) {

    			// capture ctrl-f or meta-f (mac) to activate search
				if ((e.code === 'KeyF' || e.keyCode === 70) && ((!_Helpers.isMac() && e.ctrlKey) || (_Helpers.isMac() && e.metaKey))) {
    				e.preventDefault();
    				searchBox.focus();
    			}
    		});
    	}
    },
	clearSearch: () => {

		document.getElementById('main').classList.remove(_Search.noHitAtAllClass);

    	document.querySelectorAll('.' + _Search.hitClass).forEach((node) => {
    		node.classList.remove(_Search.hitClass);
    	});

    	document.querySelectorAll('.' + _Search.noHitClass).forEach((node) => {
    		node.classList.remove(_Search.noHitClass);
    	});
    },
	doSearch: (q) => {

		_Search.clearSearch();

		// all tabs
		document.querySelectorAll('.tabs-menu li a').forEach((tabLink) => {

			let tab = document.querySelector(tabLink.getAttribute('href'));

			let hitInTab = false;

			if (tab.id === 'databases') {

				tab.querySelectorAll('.config-group').forEach((configGroup) => {

					let hitInConfigGroup = false;

					configGroup.querySelectorAll('label').forEach((label) => {
						if (_Search.containsIgnoreCase(label.firstChild.textContent, q)) {
							hitInConfigGroup = true;
							label.classList.add(_Search.hitClass);
						}
					});

					configGroup.querySelectorAll('[type=text]').forEach((input) => {
						if (input.value && _Search.containsIgnoreCase(input.value, q)) {
							hitInConfigGroup = true;
							input.classList.add(_Search.hitClass);
						}
					});

					hitInTab = hitInTab || hitInConfigGroup;
				});

			} else {

				// all form-groups in tab
				tab.querySelectorAll('.form-group').forEach((formGroup) => {

					let hitInFormGroup = false;

					// key
					formGroup.querySelectorAll('label').forEach((label) => {
						if (_Search.containsIgnoreCase(label.firstChild.textContent, q)) {
							hitInFormGroup = true;
							label.classList.add(_Search.hitClass);
						}
					});

					// input
					formGroup.querySelectorAll('[type=text][name]').forEach((input) => {
						if (input.value && _Search.containsIgnoreCase(input.value, q)) {
							hitInFormGroup = true;
							input.classList.add(_Search.hitClass);
						}
					});

					// textarea
					formGroup.querySelectorAll('textarea').forEach((textarea) => {
						if (textarea.value && _Search.containsIgnoreCase(textarea.value, q)) {
							hitInFormGroup = true;
							textarea.classList.add(_Search.hitClass);
						}
					});

					// select
					formGroup.querySelectorAll('select option').forEach((option) => {
						if (_Search.containsIgnoreCase(option.textContent, q)) {
							hitInFormGroup = true;
							option.closest('select').classList.add(_Search.hitClass);
						}
					});

					// button
					formGroup.querySelectorAll('button[data-value]').forEach((button) => {
						if (_Search.containsIgnoreCase(button.dataset.value, q)) {
							hitInFormGroup = true;
							button.classList.add(_Search.hitClass);
						}
					});

					// help text
					formGroup.querySelectorAll('label[data-comment]').forEach((label) => {
						if (_Search.containsIgnoreCase(label.dataset.comment, q)) {
							hitInFormGroup = true;
							label.querySelector('span').classList.add(_Search.hitClass);
						}
					});

					if (!hitInFormGroup) {
						formGroup.classList.add(_Search.noHitClass);
					}

					hitInTab = hitInTab || hitInFormGroup;
				});
			}

			let servicesTable = tab.querySelector('#services-table');
			if (servicesTable) {
				servicesTable.querySelectorAll('td:first-of-type').forEach((td) => {
					if (_Search.containsIgnoreCase(td.textContent, q)) {
						hitInTab = true;
						td.classList.add(_Search.hitClass);
					}
				});
			}

			let liElement = tabLink.parentNode;

			if (hitInTab) {
				liElement.classList.add(_Search.hitClass);
			} else {
				liElement.classList.add(_Search.noHitClass);
				tab.classList.add(_Search.noHitClass);
			}
		});

		// hide everything without search hits
		document.querySelectorAll('.config-group').forEach((configGroup) => {
			let hitsInGroup = configGroup.querySelectorAll('.' + _Search.hitClass).length;
			if (hitsInGroup === 0) {
				configGroup.classList.add(_Search.noHitClass);
			}
		});

		// if any tabs are left, activate the first (if the currently active one is hidden)
		let activeTabs = document.querySelectorAll('.tabs-menu li.active');
		if (activeTabs.length > 0 && activeTabs[0].classList.contains(_Search.noHitClass)) {
			let visibleTabLinks = document.querySelectorAll('.tabs-menu li.' + _Search.hitClass + ' a');
			if (visibleTabLinks.length > 0) {
				visibleTabLinks[0].click();

				// in case a password field got auto-focused by the browser
				document.getElementById('search-box').focus();
			} else {

				document.getElementById('main').classList.add(_Search.noHitAtAllClass);
			}
		}
	}
};