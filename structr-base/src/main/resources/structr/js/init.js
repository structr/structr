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

const globalFinalizationRegistry = new FinalizationRegistry(message => console.log('finalized: ' + message));

document.addEventListener("DOMContentLoaded", () => {

	document.body.innerHTML = Structr.templates.mainBody();
	_Icons.preloadSVGIcons();
	_Elements.contextMenu.init();

	Structr.header                   = document.getElementById('header');
	Structr.mainContainer            = document.getElementById('main');
	Structr.functionBar              = document.getElementById('function-bar');
	Structr.mainContainerOffscreen   = document.createElement('div');
	Structr.functionBarOffscreen     = document.createElement('div');
	Structr.dialogContainerOffscreen = document.createElement('div');

	document.querySelector('#logout_').addEventListener('click', (e) => {
		e.stopPropagation();
		Structr.doLogout();
	});

	let isHashReset = false;
	window.addEventListener('hashchange', (e) => {

		if (isHashReset === false) {

			let anchor = new URL(window.location.href).hash.substring(1);
			if (anchor === 'logout' || _Dialogs.loginDialog.isOpen()) {
				return;
			}

			if (anchor.indexOf(':') > -1) {
				return;
			}

			let allow = (new URL(e.oldURL).hash === '') || Structr.requestActivateModule(e, anchor);

			if (allow !== true) {
				isHashReset = true;
				window.location.href = e.oldURL;
			}

		} else {
			isHashReset = false;
		}
	});

	$(document).on('mouseenter', '[data-toggle="popup"]', function() {
		let target = $(this).data("target");
		$(target).addClass('visible');
	});

	$(document).on('mouseleave', '[data-toggle="popup"]', function() {
		let target = $(this).data("target");
		$(target).removeClass('visible');
	});

	StructrWS.init();

	document.body.addEventListener('keyup', (event) => {

		let keyCode = event.keyCode;
		let code    = event.code;

		if (code === 'Escape' || code === 'Esc' || keyCode === 27) {

			if (Structr.ignoreKeyUp) {
				Structr.ignoreKeyUp = false;
				return false;
			}

			_Dialogs.custom.checkSaveOrCloseOnEscape();
		}
		return false;
	});

	document.body.addEventListener('keydown', (event) => {

		let keyCode = event.keyCode;
		let code    = event.code;

		// ctrl-s / cmd-s
		if ((code === 'KeyS' || keyCode === 83) && ((navigator.platform !== 'MacIntel' && event.ctrlKey) || (navigator.platform === 'MacIntel' && event.metaKey))) {
			event.preventDefault();
			_Dialogs.custom.clickSaveButton();
		}

		// Ctrl-Alt-c
		if ((code === 'KeyC' || keyCode === 67) && event.altKey && event.ctrlKey) {
			event.preventDefault();
			_Console.toggleConsole();
		}

		// Ctrl-Alt-f
		if ((code === 'KeyF' || keyCode === 70) && event.altKey && event.ctrlKey) {
			event.preventDefault();
			_Favorites.toggleFavorites();
		}

		// Ctrl-Alt-p
		if ((code === 'KeyP' || keyCode === 80) && event.altKey && event.ctrlKey) {
			event.preventDefault();
			let uuid = prompt('Enter the UUID for which you want to open the properties dialog');
			if (!uuid) {
				// ESC or Cancel
			} else if (_Helpers.isUUID(uuid)) {
				Command.get(uuid, null, (obj) => {
					_Entities.showProperties(obj);
				});
			} else {
				new WarningMessage().text('Given string does not validate as a UUID').show();
			}
		}

		// Ctrl-Alt-m
		if ((code === 'KeyM' || keyCode === 77) && event.altKey && event.ctrlKey) {
			event.preventDefault();
			let uuid = prompt('Enter the UUID for which you want to open the content/template edit dialog');
			if (!uuid) {
				// ESC or Cancel
			} else if (_Helpers.isUUID(uuid)) {
				Command.get(uuid, null, (obj) => {
					_Elements.openEditContentDialog(obj);
				});
			} else {
				new WarningMessage().text('Given string does not validate as a UUID').show();
			}
		}

		// Ctrl-Alt-g
		if ((code === 'KeyG' || keyCode === 71) && event.altKey && event.ctrlKey) {
			event.preventDefault();
			let uuid = prompt('Enter the UUID for which you want to open the access control dialog');
			if (!uuid) {
				// ESC or Cancel
			} else if (_Helpers.isUUID(uuid)) {
				Command.get(uuid, null, (obj) => {
					_Entities.showAccessControlDialog(obj);
				});
			} else {
				new WarningMessage().text('Given string does not validate as a UUID').show();
			}
		}

		// Ctrl-Alt-h
		if ((code === 'KeyH' || keyCode === 72) && event.altKey && event.ctrlKey) {
			event.preventDefault();
			if (Structr.isModuleActive(_Schema)) {
				_Schema.hideSelectedSchemaTypes();
			}
		}

		// Ctrl-Alt-e
		if ((code === 'KeyE' || keyCode === 69) && event.altKey && event.ctrlKey) {
			event.preventDefault();
			let { dialogText } = _Dialogs.custom.openDialog('Bulk Editing Helper (Ctrl-Alt-E)');
			new RefactoringHelper(dialogText).show();
		}

		// ctrl-u / cmd-u: show generated source in schema or code area
		if ((code === 'KeyU' || keyCode === 85) && ((navigator.platform !== 'MacIntel' && event.ctrlKey) || (navigator.platform === 'MacIntel' && event.metaKey))) {

			let sourceCodeTab = document.querySelector('li#tab-source-code');

			if (sourceCodeTab) {

				event.preventDefault();

				sourceCodeTab.dispatchEvent(new Event('click'));
				sourceCodeTab.style.display = null;
			}
		}
	});

	window.addEventListener('resize', Structr.resize);

	live('.dropdown-select', 'click', (e) => {
		e.stopPropagation();
		e.preventDefault();

		Structr.handleDropdownClick(e);

		return false;
	});

	window.addEventListener('click', (e) => {
		e.stopPropagation();

		const menu           = e.target.closest('.dropdown-menu');
		const menuContainer  = menu && menu.querySelector('.dropdown-menu-container');

		Structr.hideOpenDropdownsExcept(menuContainer);

		return false;
	});
});

let Structr = {
	rootUrl        : _Helpers.getPrefixedRootUrl('/structr/rest/'),
	csvRootUrl     : _Helpers.getPrefixedRootUrl('/structr/csv/'),
	graphQLRootUrl : _Helpers.getPrefixedRootUrl('/structr/graphql/'),
	viewRootUrl    : _Helpers.getPrefixedRootUrl('/'),
	wsRoot         : _Helpers.getPrefixedRootUrl('/structr/ws'),
	deployRoot     : _Helpers.getPrefixedRootUrl('/structr/deploy'),
	ignoreKeyUp: undefined,
	isInMemoryDatabase: undefined,
	modules: {},
	activeModules: {},
	availableMenuItems: [],
	moduleAvailabilityCallbacks: [],
	keyMenuConfig: 'structrMenuConfig_' + location.port,
	mainModule: undefined,
	subModule: undefined,
	expandedIdsKey: 'structrTreeExpandedIds_' + location.port,
	edition: '',
	classes: [],
	expanded: {},
	msgCount: 0,
	legacyRequestParameters: false,
	diffMatchPatch: undefined,
	abbreviations: {
		visibleToPublicUsers: "Public",
		visibleToAuthenticatedUsers: "Auth. Vis."
	},
	dialogTimeoutId: undefined,
	getDiffMatchPatch: () => {
		if (!Structr.diffMatchPatch) {
			Structr.diffMatchPatch = new diff_match_patch();
		}
		return Structr.diffMatchPatch;
	},
	getRequestParameterName: (key) => (Structr.legacyRequestParameters === true) ? key : `_${key}`,
	setFunctionBarHTML: (html) => {
		_Helpers.setContainerHTML(Structr.functionBar, html);
	},
	setMainContainerHTML: (html) => {
		_Helpers.setContainerHTML(Structr.mainContainer, html);
	},

	moveUIOffscreen: () => {

		let movedOffscreen = false;

		// only move UI offscreen if there is UI to move offscreen
		if (Structr.functionBar.children.length > 0) {

			_Helpers.fastRemoveAllChildren(Structr.functionBarOffscreen);
			Structr.functionBarOffscreen.append(...Structr.functionBar.children);

			movedOffscreen = true;
		}

		if (Structr.mainContainer.children.length > 0) {

			_Helpers.fastRemoveAllChildren(Structr.mainContainerOffscreen);
			Structr.mainContainerOffscreen.append(...Structr.mainContainer.children);

			movedOffscreen = true;
		}

		let dialogBox = _Dialogs.custom.getDialogBoxElement();
		if (dialogBox && dialogBox.offsetParent) {

			let reconnectDialogElement = dialogBox.querySelector('#reconnect-dialog')
			if (!reconnectDialogElement) {

				let parent = dialogBox.parentNode;

				_Helpers.fastRemoveAllChildren(Structr.dialogContainerOffscreen);
				Structr.dialogContainerOffscreen.append(dialogBox);

				movedOffscreen = true;

				_Dialogs.basic.removeBlockerAround(parent);
			}
		}

		return movedOffscreen;
	},
	moveOffscreenUIOnscreen: () => {

		let movedBack = false;

		if (Structr.functionBarOffscreen.children.length > 0) {

			_Helpers.fastRemoveAllChildren(Structr.functionBar);
			Structr.functionBar.append(...Structr.functionBarOffscreen.children);

			movedBack = true;
		}

		if (Structr.mainContainerOffscreen.children.length > 0) {

			_Helpers.fastRemoveAllChildren(Structr.mainContainer);
			Structr.mainContainer.append(...Structr.mainContainerOffscreen.children);

			movedBack = true;
		}

		if (Structr.dialogContainerOffscreen.children.length > 0) {

			_Dialogs.custom.restoreDialog();

			movedBack = true;
		}

		return movedBack;
	},
	init: () => {
		_Helpers.fastRemoveAllChildren(document.querySelector('#errorText'));
	},
	clearMain: () => {

		let newDroppables = new Array();
		$.ui.ddmanager.droppables['default'].forEach(function(droppable, i) {
			if (!droppable.element.attr('id') || droppable.element.attr('id') !== 'graph-canvas') {
			} else {
				newDroppables.push(droppable);
			}
		});
		$.ui.ddmanager.droppables['default'] = newDroppables;

		_Helpers.fastRemoveAllChildren(Structr.mainContainer);
		_Helpers.fastRemoveAllChildren(Structr.functionBar);
		_Helpers.fastRemoveAllChildren(Structr.mainContainerOffscreen);
		_Helpers.fastRemoveAllChildren(Structr.functionBarOffscreen);
		_Helpers.fastRemoveAllChildren(Structr.dialogContainerOffscreen);
		_Dialogs.custom.clearDialogElements();
		_Elements.contextMenu.remove();
	},
	refreshUi: (isLogin = false) => {

		_Dialogs.spinner.show();

		Structr.clearMain();

		Structr.loadInitialModule(isLogin, () => {

			StructrWS.startPing();

			_Dialogs.spinner.hide();

			_Console.initConsole();

			document.querySelector('#header .logo').addEventListener('click', _Console.toggleConsole);

			_Favorites.initFavorites();
		});
	},
	updateUsername: (name) => {
		if (name !== StructrWS.user) {
			StructrWS.user = name;
			$('#logout_').html(`Logout <span class="username">${name}</span>`);
		}
	},
	getSessionId: () => {
		return Cookies.get('JSESSIONID');
	},
	doLogin: (loginData) => {
		Structr.renewSessionId(() => {
			Command.login(loginData);
		});
	},
	doLogout: () => {

		_Favorites.logoutAction();
		_Console.logoutAction();
		LSWrapper.save();

		Structr.mainMenu.reset();

		if (Command.logout(StructrWS.user)) {

			Cookies.remove('JSESSIONID');
			StructrWS.sessionId = '';
			Structr.renewSessionId();
			Structr.clearMain();

			for (let versionInfo of document.querySelectorAll('.structr-version')) {
				versionInfo.textContent = '';
			}

			_Dialogs.loginDialog.show();
			return true;
		}

		StructrWS.close();
		return false;
	},
	renewSessionId: (callback) => {

		fetch(Structr.viewRootUrl).then(response => {

			StructrWS.sessionId = Structr.getSessionId();

			if (!StructrWS.sessionId && location.protocol === 'http:') {

				new WarningMessage()
					.title("Unable to retrieve session id cookie")
					.text("This is most likely due to a pre-existing secure HttpOnly cookie. Please navigate to the HTTPS version of this page (even if HTTPS is inactive) and delete the JSESSIONID cookie. Then return to this page and reload. This should solve the problem.")
					.requiresConfirmation()
					.uniqueClass("http-only-cookie")
					.show();
			}

			if (typeof callback === "function") {
				callback();
			}
		});
	},
	loadInitialModule: (isLogin, callback) => {

		LSWrapper.restore(() => {

			Structr.expanded = JSON.parse(LSWrapper.getItem(Structr.expandedIdsKey));

			Structr.determineModule();

			Structr.mainMenu.lastMenuEntry = ((!isLogin && Structr.mainModule && Structr.mainModule !== 'logout') ? Structr.mainModule : Structr.getActiveModuleName());
			if (!Structr.mainMenu.lastMenuEntry) {
				Structr.mainMenu.lastMenuEntry = Structr.getActiveModuleName() || 'dashboard';
			}
			Structr.updateVersionInfo(0, isLogin);
			Structr.activateModule(Structr.mainMenu.lastMenuEntry);

			callback();
		});
	},
	determineModule: () => {

		const browserUrl   = new URL(window.location.href);
		const anchor       = browserUrl.hash.substring(1);
		const navState     = anchor.split(':');
		Structr.mainModule = navState[0];
		Structr.subModule  = navState.length > 1 ? navState[1] : null;
	},
	focusSearchField: () => {

		let activeModule = Structr.getActiveModule();
		if (activeModule) {
			let searchField = activeModule.searchField;

			if (searchField) {
				searchField.focus();
			}
		}
	},
	prevAnimFrameReqId_resize: undefined,
	resize: () => {

		_Helpers.requestAnimationFrameWrapper(Structr.prevAnimFrameReqId_resize, () => {

			_Dialogs.custom.resizeDialog();

			if (StructrWS.user) {

				// call resize function of active module
				let activeModule = Structr.getActiveModule();
				activeModule.resize?.();
			}
		});
	},
	error: (text, confirmationRequired) => {

		let message = new ErrorMessage().text(text);
		if (confirmationRequired) {
			message.requiresConfirmation();
		}
		message.show();
	},
	getErrorMessageFromResponse: (response, useHtml = true, url) => {

		let errorText = '';

		if (response.errors && response.errors.length) {

			let errorLines = [response.message];

			for (let error of response.errors) {

				let errorMsg = (error.type ? error.type : '');
				if (error.property) {
					errorMsg += `.${error.property}`;
				}
				if (error.value) {
					errorMsg += ` ${error.value}`;
				}
				if (error.token) {
					errorMsg += ` ${error.token}`;
				}
				if (error.detail) {
					if (errorMsg.trim().length > 0) {
						errorMsg += ': ';
					}
					errorMsg += error.detail;
				}

				errorLines.push(errorMsg);
			}

			errorText = errorLines.join((useHtml ? '<br>' : '\n'));

		} else {

			if (url) {
				errorText = url + ': ';
			}

			errorText += response.code + (useHtml ? '<br>' : '\n');

			for (let key in response) {
				if (key !== 'code') {
					if (useHtml) {
						errorText += `<b>${key}</b>: ${response[key]}<br>`;
					} else {
						errorText += `${key}: ${response[key]}`;
					}
				}
			}
		}

		return errorText;
	},
	errorFromResponse: (response, url, additionalParameters) => {

		let errorText = Structr.getErrorMessageFromResponse(response, true, url);

		let messageBuilder = new ErrorMessage().text(errorText);

		if (additionalParameters) {

			if (additionalParameters.requiresConfirmation) {
				messageBuilder.requiresConfirmation();
			}

			if (additionalParameters.statusCode) {
				let title = _Helpers.getErrorTextForStatusCode(additionalParameters.statusCode);
				if (title) {
					messageBuilder.title(title);
				}
			}

			if (additionalParameters.title) {
				messageBuilder.title(additionalParameters.title);
			}

			if (additionalParameters.errorExplanation) {
				messageBuilder.text(`${additionalParameters.errorExplanation}<br><br>${errorText}`);
			}

			if (additionalParameters.overrideText) {
				messageBuilder.text(additionalParameters.overrideText);
			}
		}

		messageBuilder.show();
	},
	requestActivateModule: (event, name) => {
		if (Structr.mainMenu.isBlocked) {
			return false;
		}

		event.stopPropagation();
		if (Structr.getActiveModuleName() !== name || Structr.mainContainer.children.length === 0) {
			return Structr.activateModule(name);
		}

		return true;
	},
	activateModule: (name) => {

		Structr.determineModule();

		if (Structr.modules[name]) {

			let activeModule = Structr.getActiveModule();

			let moduleAllowsNavigation = true;
			if (activeModule && activeModule.unload) {

				let moduleOverride = activeModule.unload();
				if (moduleOverride === false) {
					moduleAllowsNavigation = false;
				}
			}

			if (moduleAllowsNavigation) {

				Structr.clearMain();
				Structr.mainMenu.activateEntry(name);
				Structr.modules[name].onload();
				Structr.adaptUiToAvailableFeatures();
			}

			return moduleAllowsNavigation;

		} else {

			Structr.mainMenu.unblock();
		}

		return true;
	},
	registerModule: (module) => {
		let name = module._moduleName;
		if (!name || name.trim().length === 0) {
			new ErrorMessage().text("Cannot register module without a name - ignoring attempt. To fix this error, please add the '_moduleName' variable to the module.").show();
		} else if (!Structr.modules[name]) {
			Structr.modules[name] = module;
		} else {
			new ErrorMessage().text(`Cannot register module '${name}' a second time - ignoring attempt.`).show();
		}
	},
	getActiveModuleName: () => {
		return Structr.mainMenu.lastMenuEntry || LSWrapper.getItem(Structr.mainMenu.lastMenuEntryKey);
	},
	getActiveModule: () => {
		return Structr.modules[Structr.getActiveModuleName()];
	},
	isModuleActive: (module) => {
		return (module._moduleName === Structr.getActiveModuleName());
	},
	containsNodes: (element) => {
		return (element && Structr.numberOfNodes(element) && Structr.numberOfNodes(element) > 0);
	},
	numberOfNodes: (element, excludeId) => {

		let childNodes = $(element).children('.node');

		if (excludeId) {
			childNodes = childNodes.not(`.${excludeId}_`);
		}

		return childNodes.length;
	},
	node: (id, prefix) => {

		let p    = prefix || '#id_';
		let node = $($(p + id)[0]);

		return (node.length ? node : undefined);
	},
	nodeContainer: (id, prefix) => {

		let node = Structr.node(id, prefix);
		if (node) {
			return node.children('.node-container');
		}

		return undefined;
	},
	entity: (id, parentId) => {

		let entityElement = Structr.node(id, parentId);
		let entity        = Structr.entityFromElement(entityElement);
		return entity;
	},
	getClass: (el) => {

		let c;
		for (let cls of Structr.classes) {
			if (el && $(el).hasClass(cls)) {
				c = cls;
			}
		}

		return c;
	},
	entityFromElement: (element) => {

		let entity = {};
		entity.id = Structr.getId($(element));

		let cls = Structr.getClass(element);
		if (cls) {
			entity.type = _Helpers.capitalize(cls);
		}

		let nameEl = $(element).children('.name_');
		if (nameEl && nameEl.length) {
			entity.name = $(nameEl[0]).text();
		}

		let tagEl = $(element).children('.tag_');
		if (tagEl && tagEl.length) {
			entity.tag = $(tagEl[0]).text();
		}

		return entity;
	},
	slideouts: {
		leftSlideoutTrigger: (triggerEl, slideoutElement, otherSlideouts, openCallback, closeCallback) => {

			let selectedSlideoutWasOpen = slideoutElement.hasClass('open');

			if (selectedSlideoutWasOpen === false) {

				Structr.slideouts.closeLeftSlideOuts(otherSlideouts, closeCallback);
				Structr.slideouts.openLeftSlideOut(triggerEl, slideoutElement, openCallback);

				document.querySelector('.column-resizer-left')?.classList.remove('hidden');

			} else {

				Structr.slideouts.closeLeftSlideOuts([slideoutElement], closeCallback);

				document.querySelector('.column-resizer-left')?.classList.add('hidden');
			}
		},
		openLeftSlideOut: (triggerEl, slideoutElement, callback) => {

			let storedLeftSlideoutWidth = LSWrapper.getItem(Structr.getActiveModule().getLeftResizerKey?.());
			let psw                     = storedLeftSlideoutWidth ? parseInt(storedLeftSlideoutWidth) : (slideoutElement.width());

			triggerEl.classList.add('active');
			slideoutElement.width(psw);

			slideoutElement.animate({ left: 0 }, 100, () => {
				callback?.();
			});

			slideoutElement.addClass('open');
		},
		closeLeftSlideOuts: (slideouts, callback) => {

			for (let slideout of slideouts) {

				let wasOpen = slideout[0].classList.contains('open');
				slideout[0].classList.remove('open');

				if (wasOpen) {

					let slideoutWidth = slideout[0].getBoundingClientRect().width;

					slideout.animate({ left: -slideoutWidth }, 100, () => {
						callback?.();
					}).zIndex(2);
				}
			}

			document.querySelector('.slideout-activator.left.active')?.classList.remove('active');
		},
		rightSlideoutClickTrigger: (triggerEl, slideoutElement, otherSlideouts, openCallback, closeCallback) => {

			let selectedSlideoutWasOpen = slideoutElement.hasClass('open');

			if (selectedSlideoutWasOpen === false) {

				Structr.slideouts.closeRightSlideOuts(otherSlideouts, closeCallback);
				Structr.slideouts.openRightSlideOut(triggerEl, slideoutElement, openCallback);

				document.querySelector('.column-resizer-right')?.classList.remove('hidden');

			} else {

				Structr.slideouts.closeRightSlideOuts([slideoutElement], closeCallback);

				document.querySelector('.column-resizer-right')?.classList.add('hidden');
			}
		},
		openRightSlideOut: (triggerEl, slideoutElement, callback) => {

			let storedRightSlideoutWidth = LSWrapper.getItem(Structr.getActiveModule().getRightResizerKey?.());
			let rsw                      = storedRightSlideoutWidth ? parseInt(storedRightSlideoutWidth) : (slideoutElement.width() + 12);

			triggerEl.classList.add('active');
			slideoutElement.width(rsw);

			slideoutElement.animate({right: 0}, 100, () => {
				callback?.();
			});

			slideoutElement.addClass('open');
		},
		closeRightSlideOuts: (slideouts, callback) => {

			for (let slideout of slideouts) {

				let wasOpen = slideout[0].classList.contains('open');
				slideout[0].classList.remove('open');

				if (wasOpen) {

					let slideoutWidth = slideout[0].getBoundingClientRect().width;

					slideout.animate({ right: -slideoutWidth }, 100, () => {
						callback?.();
					}).zIndex(2);
				}
			}

			document.querySelector('.slideout-activator.right.active')?.classList.remove('active');

			LSWrapper.removeItem(_Pages.activeTabRightKey);
		},
	},
	updateVersionInfo: (retryCount = 0, isLogin = false) => {

		fetch(`${Structr.rootUrl}_env`).then((response) => {

			if (response.ok) {
				return response.json();
			} else {
				throw Error("Unable to read env resource data");
			}

		}).then((data) => {

			let envInfo = data.result;
			if (Array.isArray(envInfo)) {
				envInfo = envInfo[0];
			}

			let dbInfoEl = $('#header .structr-instance-db');

			if (envInfo.databaseService) {

				let driverName = Structr.getDatabaseDriverNameForDatabaseServiceName(envInfo.databaseService);

				Structr.isInMemoryDatabase = (envInfo.databaseService === 'MemoryDatabaseService');

				if (!Structr.isInMemoryDatabase) {

					dbInfoEl.html(`<span>${_Icons.getSvgIcon(_Icons.iconDatabase, 16, 16, [], driverName)}</span>`);

				} else {

					dbInfoEl.html('<span></span>');

					Structr.appendInMemoryInfoToElement($('span', dbInfoEl));

					if (isLogin) {
						new WarningMessage().text(Structr.inMemoryWarningText).requiresConfirmation().show();
					}
				}
			}

			$('#header .structr-instance-name').text(envInfo.instanceName);
			$('#header .structr-instance-stage').text(envInfo.instanceStage);

			Structr.legacyRequestParameters = envInfo.legacyRequestParameters;

			if (true === envInfo.maintenanceModeActive) {
				$('#header .structr-instance-maintenance').text("MAINTENANCE");
			}

			_Helpers.uuidRegexp = new RegExp(envInfo.validUUIDv4Regex);

			let ui = envInfo.components['structr-ui'];
			if (ui) {

				let build       = ui.build;
				let date        = ui.date;
				let versionInfo = `
					<div>
						<a target="_blank" href="https://structr.com/download">${ui.version}</a>
						${(build && date) ? `<span> build </span><a target="_blank" href="https://github.com/structr/structr/commit/${build}">${build}</a><span> (${date})</span>` : ''}
						${(envInfo.edition) ? _Icons.getSvgIcon(_Icons.getIconForEdition(envInfo.edition), 16,16,[], `Structr ${envInfo.edition} Edition`) : ''}
					</div>
				`;

				$('.structr-version').html(versionInfo);

				if (envInfo.edition) {

					Structr.edition = envInfo.edition;

					_Dashboard.tabs['about-structr'].checkLicenseEnd(envInfo, $('.structr-version'), {
						offsetX: -300,
						helpElementCss: {
							color: "black",
							fontSize: "1rem",
							lineHeight: "1.7em"
						}
					});
				}
			}

			Structr.activeModules      = envInfo.modules;
			Structr.availableMenuItems = envInfo.availableMenuItems;

			Structr.adaptUiToAvailableFeatures();

			let userConfigMenu = LSWrapper.getItem(Structr.keyMenuConfig);
			if (!userConfigMenu) {
				userConfigMenu = {
					main: envInfo.mainMenu,
					sub: []
				};
			}

			Structr.mainMenu.update(userConfigMenu);

			if (envInfo.resultCountSoftLimit !== undefined) {
				_Crud.resultCountSoftLimit = envInfo.resultCountSoftLimit;
			}

			// run previously registered callbacks
			let registeredCallbacks = Structr.moduleAvailabilityCallbacks;
			Structr.moduleAvailabilityCallbacks = [];
			registeredCallbacks.forEach((cb) => {
				cb();
			});

		}).catch((e) => {
			if (retryCount < 3) {
				window.setTimeout(() => {
					Structr.updateVersionInfo(++retryCount, isLogin);
				}, 250);
			} else {
				console.log(e);
			}
		});
	},
	mainMenu: {
		lastMenuEntry: undefined,
		lastMenuEntryKey: 'structrLastMenuEntry_' + location.port,
		isBlocked: false,
		update: (menuConfig, updateLS = true) => {

			if (updateLS === true) {
				LSWrapper.setItem(Structr.keyMenuConfig, menuConfig);
			}

			let menu      = document.querySelector('#menu');
			let submenu   = document.querySelector('#submenu');
			let hamburger = document.querySelector('#menu li.submenu-trigger');

			// first move all elements from main menu to submenu
			submenu.append(...menu.querySelectorAll('li[data-name]'));

			// then filter the items by availability in edition
			for (let menuItem of submenu.querySelectorAll('li[data-name]')) {
				let name = menuItem.dataset.name;
				if (!Structr.availableMenuItems.includes(name)) {
					menuItem.classList.add('hidden');
				}
			}

			if (menuConfig) {

				for (let entry of menuConfig.main) {
					hamburger.before(menu.querySelector(`li[data-name="${entry}"]`))
				}

				// sort submenu
				for (let entry of menuConfig.sub) {
					submenu.querySelector('li:last-of-type').after(menu.querySelector(`li[data-name="${entry}"]`))
				}
			}
		},
		block: () => {

			Structr.mainMenu.isBlocked = true;

			for (let menuEntry of document.querySelectorAll('#menu > ul > li > a')) {
				menuEntry.disabled = true;
				menuEntry.classList.add('disabled');
			}
		},
		unblock: (timeoutInMs = 0) => {

			window.setTimeout(() => {

				Structr.mainMenu.isBlocked = false;

				for (let menuEntry of document.querySelectorAll('#menu > ul > li > a')) {
					menuEntry.disabled = false;
					menuEntry.classList.remove('disabled');
				}

			}, timeoutInMs);
		},
		reset: () => {

			Structr.mainMenu.update(null, false);

			let submenu = document.querySelector('#submenu');
			submenu.classList.remove('visible');
		},
		activateEntry: (name) => {

			Structr.mainMenu.block();

			let menuEntry = document.querySelector(`#${name}_`);

			let li = menuEntry.closest('li');
			if (li.classList.contains('active')) {
				return false;
			}

			Structr.mainMenu.lastMenuEntry = name;

			for (let menuEntry of document.querySelectorAll('.menu li')) {
				menuEntry.classList.remove('active');
			}

			li.classList.add('active');

			document.title       = `Structr ${menuEntry.textContent.trim()}`;
			window.location.hash = Structr.mainMenu.lastMenuEntry;

			if (Structr.mainMenu.lastMenuEntry && Structr.mainMenu.lastMenuEntry !== 'logout') {
				LSWrapper.setItem(Structr.mainMenu.lastMenuEntryKey, Structr.mainMenu.lastMenuEntry);
			}
		},
	},
	inMemoryWarningText: "Please note that the system is currently running on an in-memory database implementation. Data is not persisted and will be lost after restarting the instance! You can use the configuration tool to configure a database connection.",
	appendInMemoryInfoToElement: (el) => {

		let config = {
			element: el,
			text: Structr.inMemoryWarningText,
			customToggleIcon: 'database-warning-sign-icon',
			customToggleIconClasses: ['ml-2'],
			width: 20,
			height: 20,
			noSpan: true,
			helpElementCss: {
				'border': '2px solid red',
				'border-radius': '4px',
				'font-weight': 'bold',
				'font-size': '15px',
				'color': '#000'
			}
		};

		_Helpers.appendInfoTextToElement(config);
	},
	getDatabaseDriverNameForDatabaseServiceName: (databaseServiceName) => {
		switch (databaseServiceName) {
			case 'BoltDatabaseService':
				return 'Bolt Database Driver';

			case 'MemoryDatabaseService':
				return 'In-Memory Database Driver';
		}

		return 'Unknown database driver!';
	},
	getId: (element) => {
		let id = Structr.getIdFromPrefixIdString($(element).prop('id'), 'id_') || $(element).data('nodeId');
		return id || undefined;
	},
	getIdFromPrefixIdString: (idString, prefix) => {
		if (!idString || !idString.startsWith(prefix)) {
			return false;
		}
		return idString.substring(prefix.length);
	},
	getComponentId: (element) => {
		return Structr.getIdFromPrefixIdString($(element).prop('id'), 'componentId_') || undefined;
	},
	getActiveElementId: (element) => {
		return Structr.getIdFromPrefixIdString($(element).prop('id'), 'active_') || undefined;
	},
	adaptUiToAvailableFeatures: () => {
		Structr.adaptUiToAvailableModules();
		Structr.adaptUiToEdition();
	},
	adaptUiToAvailableModules: () => {
		$('.module-dependent').each(function(idx, element) {
			let el = $(element);

			if (Structr.isModulePresent(el.data('structr-module'))) {
				if (!el.is(':visible')) el.show();
			} else {
				el.hide();
			}
		});
	},
	adaptUiToEdition: () => {
		$('.edition-dependent').each(function(idx, element) {
			let el = $(element);

			if (Structr.isAvailableInEdition(el.data('structr-edition'))) {
				if (!el.is(':visible')) el.show();
			} else {
				el.hide();
			}
		});
	},
	isModulePresent: (moduleName) => {
		return Structr.activeModules[moduleName] !== undefined;
	},
	isModuleInformationAvailable: () => {
		return (Object.keys(Structr.activeModules).length > 0);
	},
	performModuleDependentAction: (action) => {
		if (Structr.isModuleInformationAvailable()) {
			action();
		} else {
			Structr.registerActionAfterModuleInformationIsAvailable(action);
		}
	},
	registerActionAfterModuleInformationIsAvailable: (cb) => {
		Structr.moduleAvailabilityCallbacks.push(cb);
	},
	isAvailableInEdition: (requiredEdition) => {
		switch (Structr.edition) {
			case 'Enterprise':
				return true;
			case 'Small Business':
				return ['Small Business', 'Basic', 'Community'].indexOf(requiredEdition) !== -1;
			case 'Basic':
				return ['Basic', 'Community'].indexOf(requiredEdition) !== -1;
			case 'Community':
				return ['Community'].indexOf(requiredEdition) !== -1;
		};
	},
	updateMainHelpLink: (newUrl) => {
		let helpLink = document.querySelector('#main-help a');
		if (helpLink) {
			helpLink.setAttribute('href', newUrl);

			if (helpLink.children.length === 0) {
				helpLink.innerHTML = _Icons.getSvgIcon(_Icons.iconInfo, 16, 16);
			}
		}
	},
	addExpandedNode: (id) => {

		if (id) {
			let alreadyStored = Structr.getExpanded()[id];
			if (!alreadyStored) {

				Structr.getExpanded()[id] = true;
				LSWrapper.setItem(Structr.expandedIdsKey, JSON.stringify(Structr.expanded));
			}
		}
	},
	removeExpandedNode: (id) => {

		if (id) {
			Structr.getExpanded()[id] = false;
			LSWrapper.setItem(Structr.expandedIdsKey, JSON.stringify(Structr.expanded));
		}
	},
	isExpanded: (id, defaultValue = false) => {

		if (id) {

			let storedValue = Structr.getExpanded()[id];

			return (storedValue === undefined) ? defaultValue : storedValue;
		}

		return defaultValue;
	},
	getExpanded: () => {

		if (!Structr.expanded) {
			Structr.expanded = JSON.parse(LSWrapper.getItem(Structr.expandedIdsKey));
		}

		if (!Structr.expanded) {
			Structr.expanded = {};
		}
		return Structr.expanded;
	},
	initVerticalSlider: (sliderEl, localstorageKey, minWidth, dragCallback, isRight) => {

		if (typeof dragCallback !== 'function') {
			console.error('dragCallback is not a function!');
			return;
		}

		$(sliderEl).draggable({
			axis: 'x',
			start: function(e, ui) {
				$('.column-resizer-blocker').show();
			},
			drag: function(e, ui) {
				return dragCallback(Structr.getSliderValueForDragCallback(ui.position.left, minWidth, isRight));
			},
			stop: function(e, ui) {
				$('.column-resizer-blocker').hide();
				LSWrapper.setItem(localstorageKey, Structr.getSliderValueForDragCallback(ui.position.left, minWidth, isRight));
			}
		});

	},
	getSliderValueForDragCallback: (leftPos, minWidth, isRight) => {

		let val = (isRight === true) ? Math.max(minWidth, window.innerWidth - leftPos) : Math.max(minWidth, leftPos);

		// If there are two resizer elements, distance between resizers must always be larger than minWidth.
		let leftResizer  = document.querySelector('.column-resizer-left');
		let rightResizer = document.querySelector('.column-resizer-right');

		if (isRight && !leftResizer.classList.contains('hidden')) {
			let leftResizerLeft = leftResizer.getBoundingClientRect().left;
			val = Math.min(val, window.innerWidth - leftResizerLeft - minWidth + leftResizer.getBoundingClientRect().width + 3);
		} else if (!isRight && rightResizer && !rightResizer.classList.contains('hidden')) {
			let rightResizerLeft = rightResizer.getBoundingClientRect().left;
			val = Math.min(val, rightResizerLeft - minWidth);
		} else if (isRight && leftResizer.classList.contains('hidden')) {
			let rightResizerLeft = rightResizer.getBoundingClientRect().left;
			val = Math.min(val, window.innerWidth - minWidth);
		} else if (!isRight && rightResizer && rightResizer.classList.contains('hidden')) {
			val = Math.min(val, window.innerWidth - minWidth);
		}

		// console.log(isRight, leftResizer.classList.contains('hidden'), rightResizer.classList.contains('hidden'), val);
		return val;
	},

	// is only populated while sorting is active - needs to be clear afterwards
	currentlyActiveSortable: undefined,
	refreshPositionsForCurrentlyActiveSortable: () => {

		if (Structr.currentlyActiveSortable) {

			Structr.currentlyActiveSortable.sortable({ refreshPositions: true });

			window.setTimeout(() => {
				Structr.currentlyActiveSortable.sortable({ refreshPositions: false });
			}, 500);
		}
	},
	handleGenericMessage: (data) => {

		let showScheduledJobsNotifications = Importer.isShowNotifications();
		let showScriptingErrorPopups       = UISettings.getValueForSetting(UISettings.settingGroups.global.settings.showScriptingErrorPopupsKey);
		let showResourceAccessGrantPopups  = UISettings.getValueForSetting(UISettings.settingGroups.global.settings.showResourceAccessGrantWarningPopupsKey);
		let showDeprecationWarningPopups   = UISettings.getValueForSetting(UISettings.settingGroups.global.settings.showDeprecationWarningPopupsKey);

		switch (data.type) {

			case "CSV_IMPORT_STATUS":

				if (StructrWS.me.username === data.username) {

					let titles = {
						BEGIN: 'CSV Import started',
						CHUNK: 'CSV Import status',
						END:   'CSV Import finished'
					};

					let texts = {
						BEGIN: 'Started importing CSV data',
						CHUNK: `Finished importing chunk ${data.currentChunkNo} / ${data.totalChunkNo}`,
						END:   `Finished importing CSV data (Time: ${data.duration})`
					};

					let messageBuilder = (data.subtype === 'END') ? new SuccessMessage() : new InfoMessage();

					messageBuilder.title(titles[data.subtype]).uniqueClass('csv-import-status').updatesText().requiresConfirmation().allowConfirmAll().text(texts[data.subtype]).show();
				}
				break;

			case "CSV_IMPORT_WARNING":

				if (StructrWS.me.username === data.username) {
					new WarningMessage().title(data.title).text(data.text).requiresConfirmation().allowConfirmAll().show();
				}
				break;

			case "CSV_IMPORT_ERROR":

				if (StructrWS.me.username === data.username) {
					new ErrorMessage().title(data.title).text(data.text).requiresConfirmation().allowConfirmAll().show();
				}
				break;

			case "FILE_IMPORT_STATUS":

				let fileImportTitles = {
					QUEUED:     'Import added to queue',
					BEGIN:      'Import started',
					CHUNK:      'Import status',
					END:        'Import finished',
					WAIT_ABORT: 'Import waiting to abort',
					ABORTED:    'Import aborted',
					WAIT_PAUSE: 'Import waiting to pause',
					PAUSED:     'Import paused',
					RESUMED:    'Import resumed'
				};

				let fileImportTexts = {
					QUEUED:     `Import of <b>${data.filename}</b> will begin after currently running/queued job(s)`,
					BEGIN:      `Started importing data from <b>${data.filename}</b>`,
					CHUNK:      `Finished importing chunk ${data.currentChunkNo} of <b>${data.filename}</b><br>Objects created: ${data.objectsCreated}<br>Time: ${data.duration}<br>Objects/s: ${data.objectsPerSecond}`,
					END:        `Finished importing data from <b>${data.filename}</b><br>Objects created: ${data.objectsCreated}<br>Time: ${data.duration}<br>Objects/s: ${data.objectsPerSecond}`,
					WAIT_ABORT: `The import of <b>${data.filename}</b> will be aborted after finishing the current chunk`,
					ABORTED:    `The import of <b>${data.filename}</b> has been aborted`,
					WAIT_PAUSE: `The import of <b>${data.filename}</b> will be paused after finishing the current chunk`,
					PAUSED:     `The import of <b>${data.filename}</b> has been paused`,
					RESUMED:    `The import of <b>${data.filename}</b> has been resumed`
				};

				if (showScheduledJobsNotifications && StructrWS.me.username === data.username) {

					let messageBuilder = (data.subtype === 'END') ? new SuccessMessage() : new InfoMessage();
					messageBuilder.title(`${data.jobtype} ${fileImportTitles[data.subtype]}`).text(fileImportTexts[data.subtype]).uniqueClass(`${data.jobtype}-import-status-${data.filepath}`);

					if (data.subtype !== 'QUEUED') {
						messageBuilder.updatesText().requiresConfirmation().allowConfirmAll();
					}

					messageBuilder.show();
				}

				if (Structr.isModuleActive(Importer)) {
					Importer.updateJobTable();
				}
				break;

			case "FILE_IMPORT_EXCEPTION":

				if (showScheduledJobsNotifications && StructrWS.me.username === data.username) {

					let text = data.message;
					if (data.message !== data.stringvalue) {
						text += `<br>${data.stringvalue}`;
					}

					new ErrorMessage().title(`Exception while importing ${data.jobtype}`).text(`File: ${data.filepath}<br>${text}`).requiresConfirmation().allowConfirmAll().show();
				}

				if (Structr.isModuleActive(Importer)) {
					Importer.updateJobTable();
				}
				break;

			case "SCRIPT_JOB_STATUS":

				let scriptJobTitles = {
					QUEUED: 'Script added to queue',
					BEGIN:  'Script started',
					END:    'Script finished'
				};
				let scriptJobTexts = {
					QUEUED: `Script job #${data.jobId} will begin after currently running/queued job(s)`,
					BEGIN:  `Started script job #${data.jobId}${((data.jobName.length > 0) ? ` (${data.jobName})` : '')}`,
					END:    `Finished script job #${data.jobId}${((data.jobName.length > 0) ? ` (${data.jobName})` : '')}`
				};

				if (showScheduledJobsNotifications && StructrWS.me.username === data.username) {

					let messageBuilder = (data.subtype === 'END') ? new SuccessMessage() : new InfoMessage();
					messageBuilder.title(scriptJobTitles[data.subtype]).text(`<div>${scriptJobTexts[data.subtype]}</div>`).uniqueClass(`${data.jobtype}-${data.subtype}`).appendsText();

					if (data.subtype !== 'QUEUED') {
						messageBuilder.requiresConfirmation().allowConfirmAll();
					}

					messageBuilder.show();
				}

				if (Structr.isModuleActive(Importer)) {
					Importer.updateJobTable();
				}
				break;

			case 'DEPLOYMENT_IMPORT_STATUS':
			case 'DEPLOYMENT_DATA_IMPORT_STATUS': {

				let type            = 'Deployment Import';
				let messageCssClass = 'deployment-import';

				if (data.type === 'DEPLOYMENT_DATA_IMPORT_STATUS') {
					type            = 'Data Deployment Import';
					messageCssClass = 'data-deployment-import';
				}

				if (data.subtype === 'BEGIN') {

					let text = `${type} started: ${new Date(data.start)}<br>
						Importing from: <span class="deployment-source">${data.source}</span><br><br>
						Please wait until the import process is finished. Any changes made during a deployment might get lost or conflict with the deployment! This message will be updated during the deployment process.<br><ol class="message-steps"></ol>
					`;

					new InfoMessage().title(`${type} Progress`).uniqueClass(messageCssClass).text(text).requiresConfirmation().updatesText().show();

				} else if (data.subtype === 'PROGRESS') {

					new InfoMessage().title(`${type} Progress`).uniqueClass(messageCssClass).text(`<li>${data.message}</li>`).requiresConfirmation().appendsText('.message-steps').show();

				} else if (data.subtype === 'END') {

					let text = `<br>${type} finished: ${new Date(data.end)}<br>
						Total duration: ${data.duration}<br><br>
						Reload the page to see the new data.`;

					new SuccessMessage().title(`${type} finished`).uniqueClass(messageCssClass).text(text).specialInteractionButton('Reload Page', () => { location.reload(); }, 'Ignore').appendsText().updatesButtons().show();

				}
				break;
			}

			case 'DEPLOYMENT_EXPORT_STATUS':
			case 'DEPLOYMENT_DATA_EXPORT_STATUS': {

				let type            = 'Deployment Export';
				let messageCssClass = 'deployment-export';

				if (data.type === 'DEPLOYMENT_DATA_EXPORT_STATUS') {
					type            = 'Data Deployment Export';
					messageCssClass = 'data-deployment-export';
				}

				if (data.subtype === 'BEGIN') {

					let text = `${type} started: ${new Date(data.start)}<br>
						Exporting to: <span class="deployment-target">${data.target}</span><br><br>
						System performance may be affected during Export.<br><ol class="message-steps"></ol>
					`;

					new InfoMessage().title(`${type} Progress`).uniqueClass(messageCssClass).text(text).requiresConfirmation().updatesText().show();

				} else if (data.subtype === 'PROGRESS') {

					new InfoMessage().title(`${type} Progress`).uniqueClass(messageCssClass).text(`<li>${data.message}</li>`).requiresConfirmation().appendsText('.message-steps').show();

				} else if (data.subtype === 'END') {

					let text = `<br>${type} finished: ${new Date(data.end)}<br>Total duration: ${data.duration}`;

					new SuccessMessage().title(`${type} finished`).uniqueClass(messageCssClass).text(text).appendsText().requiresConfirmation().show();

				}
				break;
			}

			case "SCHEMA_ANALYZE_STATUS":

				if (data.subtype === 'BEGIN') {

					let text = `Schema Analysis started: ${new Date(data.start)}<br>
						Please wait until the import process is finished. This message will be updated during the process.<br>
						<ol class="message-steps"></ol>
					`;

					new InfoMessage().title('Schema Analysis progress').uniqueClass('schema-analysis').text(text).requiresConfirmation().updatesText().show();

				} else if (data.subtype === 'PROGRESS') {

					new InfoMessage().title('Schema Analysis progress').uniqueClass('schema-analysis').text(`<li>${data.message}</li>`).requiresConfirmation().appendsText('.message-steps').show();

				} else if (data.subtype === 'END') {

					let text = `<br>Schema Analysis finished: ${new Date(data.end)}<br>Total duration: ${data.duration}`;

					new SuccessMessage().title("Schema Analysis finished").uniqueClass('schema-analysis').text(text).appendsText().requiresConfirmation().show();

				}
				break;

			case "CERTIFICATE_RETRIEVAL_STATUS":

				if (data.subtype === 'BEGIN') {

					let text = `Process to retrieve a Let's Encrypt certificate via ACME started: ${new Date(data.start)}<br>
						This will take a couple of seconds. This message will be updated during the process.<br>
						<ol class='message-steps'></ol>
					`;

					new InfoMessage().title("Certificate retrieval progress").uniqueClass('cert-retrieval').text(text).requiresConfirmation().updatesText().show();

				} else if (data.subtype === 'PROGRESS') {

					new InfoMessage().title("Certificate retrieval progress").uniqueClass('cert-retrieval').text(`<li>${data.message}</li>`).requiresConfirmation().appendsText('.message-steps').show();

				} else if (data.subtype === 'END') {

					let text = `<br>Certificate retrieval process finished: ${new Date(data.end)}<br>Total duration: ${data.duration}`;

					new SuccessMessage().title("Certificate retrieval finished").uniqueClass('cert-retrieval').text(text).appendsText().requiresConfirmation().show();

				} else if (data.subtype === 'WARNING') {

					new WarningMessage().title("Certificate retrieval progress").text(data.message).uniqueClass('cert-retrieval').requiresConfirmation().allowConfirmAll().show();
				}

				break;

			case "MAINTENANCE":

				let enabled = data.enabled ? 'enabled' : 'disabled';

				new WarningMessage().title(`Maintenance Mode ${enabled}`).text(`Maintenance Mode has been ${enabled}.<br><br> Redirecting...`).allowConfirmAll().show();

				window.setTimeout(() => {
					location.href = data.baseUrl + location.pathname + location.search;
				}, 1500);
				break;

			case "WARNING":
				new WarningMessage().title(data.title).text(data.message).requiresConfirmation().allowConfirmAll().show();
				break;

			case "SCRIPT_JOB_EXCEPTION":
				new WarningMessage().title('Exception in Scheduled Job').text(data.message).requiresConfirmation().allowConfirmAll().show();
				break;

			case "RESOURCE_ACCESS":

				if (showResourceAccessGrantPopups) {

					let builder = new WarningMessage().title(`REST Access to '${data.uri}' denied`).text(data.message).requiresConfirmation().allowConfirmAll();

					builder.specialInteractionButton('Go to Security and create Grant', function (btn) {

						let maskIndex = (data.validUser ? 'AUTH_USER_' : 'NON_AUTH_USER_') + data.method.toUpperCase();
						let flags     = _ResourceAccessGrants.mask[maskIndex] || 0;

						let additionalData = {};

						if (data.validUser === true) {
							additionalData.visibleToAuthenticatedUsers = true;
						} else {
							additionalData.visibleToPublicUsers = true;
						}

						_ResourceAccessGrants.createResourceAccessGrant(data.signature, flags, null, additionalData);

						let resourceAccessKey = 'resource-access';

						let grantPagerConfig = LSWrapper.getItem(_Pager.pagerDataKey + resourceAccessKey);
						if (!grantPagerConfig) {
							grantPagerConfig = {
								id: resourceAccessKey,
								type: resourceAccessKey,
								page: 1,
								pageSize: 25,
								sort: "signature",
								order: "asc"
							};
						} else {
							grantPagerConfig = JSON.parse(grantPagerConfig);
						}
						grantPagerConfig.filters = {
							flags: false,
							signature: data.signature
						};

						LSWrapper.setItem(_Pager.pagerDataKey + resourceAccessKey, JSON.stringify(grantPagerConfig));

						if (Structr.getActiveModule()._moduleName === _Security._moduleName) {
							_Security.selectTab(resourceAccessKey);
						} else {
							LSWrapper.setItem(_Security.securityTabKey, resourceAccessKey);
							window.location.href = '#security';
						}
					}, 'Dismiss');

					builder.show();
				}

				break;

			case "SCRIPTING_ERROR":

				if (showScriptingErrorPopups) {

					if (data.nodeId && data.nodeType) {

						Command.get(data.nodeId, 'id,type,name,content,ownerDocument,schemaNode', function (obj) {

							let name     = data.name.slice(data.name.indexOf('_html_') === 0 ? 6 : 0);
							let property = 'Property';
							let title    = '';

							switch (obj.type) {

								case 'SchemaMethod':
									if (obj.schemaNode && data.isStaticMethod) {
										title = `type "${data.staticType}"`;
										property ='StaticMethod';
									} else if (obj.schemaNode) {
										title = `type "${obj.schemaNode.name}"`;
										property = 'Method';
									} else {
										title = 'global schema method';
										property = 'Method';
									}
									break;

								default:
									if (obj.ownerDocument) {
										if (obj.ownerDocument.type === 'ShadowDocument') {
											title = 'shared component';
										} else {
											title = `page "${obj.ownerDocument.name}"`;
										}
									}
									break;
							}

							let location = `
								<table class="scripting-error-location">
									<tr>
										<th>Element:</th>
										<td class="pl-2">${data.nodeType}[${data.nodeId}]</td>
									</tr>
									<tr>
										<th>${property}:</th>
										<td class="pl-2">${name}</td>
									</tr>
									<tr>
										<th>Row:</th>
										<td class="pl-2">${data.row}</td>
									</tr>
									<tr>
										<th>Column:</th>
										<td class="pl-2">${data.column}</td>
									</tr>
								</table>
								<br>
								${data.message}
							`;

							let builder = new WarningMessage().uniqueClass(`n${data.nodeId}${data.nodeType}${data.row}${data.column}`).incrementsUniqueCount(true).title(`Scripting error in ${title}`).text(location).requiresConfirmation();

							if (data.nodeType === 'SchemaMethod' || data.nodeType === 'SchemaProperty') {

								let pathToOpen = (obj.schemaNode) ? `/root/custom/${obj.schemaNode.id}/methods/${obj.id}` : `/globals/${obj.id}`;

								builder.specialInteractionButton(`Go to ${data.nodeType === 'SchemaMethod' ? 'method' : 'property'}`, () => {

									window.location.href = '#code';

									window.setTimeout(() => {
										_Code.findAndOpenNode(pathToOpen, false);
									}, 1000);

								}, 'Dismiss');

							} else {

								builder.specialInteractionButton('Open in editor', function(btn) {

									switch (data.nodeType) {
										case 'Content':
										case 'Template':
											_Elements.openEditContentDialog(obj);
											break;
										case 'File':
											_Files.editFile(obj);
											break;
										default:
											_Entities.showProperties(obj);
											break;
									}

									_Pages.openAndSelectTreeObjectById(obj.id);

								}, 'Dismiss');
							}

							// show message
							builder.allowConfirmAll().show();
						});

					} else {
						new WarningMessage().title('Server-side Scripting Error').text(data.message).requiresConfirmation().allowConfirmAll().show();
					}
				}
				break;

			case "DEPRECATION": {

				if (showDeprecationWarningPopups) {

					let uniqueClass = 'deprecation-warning-' + data.nodeId;

					let builder = new WarningMessage().uniqueClass(uniqueClass).incrementsUniqueCount(true).title(data.title).text(data.message).requiresConfirmation();

					if (data.subtype === 'EDIT_MODE_BINDING') {

						if (data.nodeId) {

							Command.get(data.nodeId, 'id,type,name,content,ownerDocument', (obj) => {

								let title = '';

								switch (obj.type) {

									default:
										if (obj.ownerDocument) {
											if (obj.ownerDocument.type === 'ShadowDocument') {
												title = 'Shared component';
											} else {
												title = `Page "${obj.ownerDocument.name}"`;
											}
										}
										break;
								}

								if (title != '') {
									builder.text(`${data.message}<br><br>Source: ${title}`);
								}

								builder.specialInteractionButton('Go to element in page tree', function (btn) {

									_Pages.openAndSelectTreeObjectById(obj.id);

								}, 'Dismiss');

								builder.allowConfirmAll().show();
							});
						} else {
							builder.allowConfirmAll().show();
						}
					} else {
						builder.allowConfirmAll().show();
					}
				}
				break;
			}

			default: {

				let text = `
					<p>No handler for generic message of type <b>${data.type}</b> defined - printing complete message data.</p>
					${Object.entries(data).map(([key, value]) => `<b>${key}</b>:${value}`).join('<br>')}
				`;

				new WarningMessage().title("GENERIC_MESSAGE").text(text).requiresConfirmation().show();
			}
		}
	},
	showReconnectDialog: () => {

		let restoreDialogText = '';
		let dialogData = JSON.parse(LSWrapper.getItem(_Dialogs.custom.dialogDataKey));
		if (dialogData && dialogData.text) {
			restoreDialogText = `<div>The dialog</div><b>"${dialogData.text}"</b><div>will be restored after reconnect.</div>`;
		}

		let reconnectDialog = `
			<div id="reconnect-dialog">
				<div class="flex flex-col gap-y-4 items-center justify-center">
					<div class="flex items-center">
						${_Icons.getSvgIcon(_Icons.iconWarningYellowFilled, 16, 16, 'mr-2')}
						<b>Connection lost or timed out.</b>
					</div>

					<div>Don't reload the page!</div>

					${restoreDialogText}

					<div class="flex items-center">
						<span>Trying to reconnect...</span>
						${_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 24, 24, 'ml-2')}
					</div>
				</div>
			</div>
		`;

		_Dialogs.basic.append(reconnectDialog, { padding: '1rem' });
	},
	hideReconnectDialog: () => {
		// remove reconnect dialog
		let reconnectMessage = document.getElementById('reconnect-dialog');
		_Dialogs.basic.removeBlockerAround(reconnectMessage);
	},
	ensureShadowPageExists: () => {

		return new Promise((resolve, reject) => {

			if (_Pages.shadowPage) {

				resolve(_Pages.shadowPage);

			} else {

				// wrap getter for shadow document in listComponents so we're sure that shadow document has been created
				Command.listComponents(1, 1, 'name', 'asc', (result) => {

					Command.getByType('ShadowDocument', 1, 1, null, null, null, true, (entities) => {

						_Pages.shadowPage = entities[0];

						resolve(_Pages.shadowPage);
					});
				});
			}
		});
	},
	handleDropdownClick: (e) => {

		let menu = e.target.closest('.dropdown-menu');

		if (menu) {

			let container = e.target.closest('.dropdown-menu').querySelector('.dropdown-menu-container');

			if (container) {

				let isVisible = (container.dataset['visible'] === 'true');

				if (isVisible) {

					Structr.hideDropdownContainer(container);

				} else {

					Structr.hideOpenDropdownsExcept(container);

					let btn           = e.target.closest('.dropdown-select');
					let btnRect       = btn.getBoundingClientRect();
					let containerRect = container.getBoundingClientRect();

					// apply "fixed" first to prevent container overflow
					if (btn.dataset['wantsFixed'] === 'true') {
						/*
							this is important for the editor tools in a popup which need to break free from the popup dialog
						*/
						container.style.position = 'fixed';  // no top, no bottom, just fixed so that it is positioned automatically but taken out of the document flow
					}

					container.dataset['visible'] = 'true';
					container.style.display      = 'block';

					if (btn.dataset['preferredPositionX'] === 'left') {

						// position dropdown left of button
						container.style.right    = `calc(${window.innerWidth - btnRect.right}px + 2.5rem)`;
					}

					if (btn.dataset['preferredPositionY'] === 'top') {

						// position dropdown over activator button
						container.style.bottom    = `calc(${window.innerHeight - btnRect.top}px + 0.25rem)`;
					}
				}
			}
		}
	},
	hideOpenDropdownsExcept: (exception) => {

		for (let container of document.querySelectorAll('.dropdown-menu-container')) {

			if (container != exception) {
				Structr.hideDropdownContainer(container);
			}
		}
	},
	hideDropdownContainer: (container) => {

		container.dataset['visible'] = null;
		container.style.display      = 'none';
		container.style.position     = null;
		container.style.bottom       = null;
		container.style.top          = null;
	},


	templates: {
		mainBody: config => `
			<div id="info-area"></div>
			<div id="header">

				${_Icons.getSvgIcon(_Icons.iconStructrLogo, 90, 24, ['logo'])}

				<div id="menu" class="menu">
					<ul>
						<li class="submenu-trigger" data-toggle="popup" data-target="#submenu">

							${_Icons.getSvgIcon(_Icons.iconHamburgerMenu, 10, 10, ['icon-white'])}

							<ul id="submenu">
								<li data-name="Dashboard"><a id="dashboard_" href="#dashboard" data-activate-module="dashboard">Dashboard</a></li>
								<li data-name="Graph"><a id="graph_" href="#graph" data-activate-module="graph">Graph</a></li>
								<li data-name="Contents"><a id="contents_" href="#contents" data-activate-module="contents">Contents</a></li>
								<li data-name="Pages"><a id="pages_" href="#pages" data-activate-module="pages">Pages</a></li>
								<li data-name="Files"><a id="files_" href="#files" data-activate-module="files">Files</a></li>
								<li data-name="Security"><a id="security_" href="#security" data-activate-module="security">Security</a></li>
								<li data-name="Schema"><a id="schema_" href="#schema" data-activate-module="schema">Schema</a></li>
								<li data-name="Code"><a id="code_" href="#code" data-activate-module="code">Code</a></li>
								<li data-name="Flows" class="module-dependent" data-structr-module="api-builder"><a id="flows_" href="#flows" data-activate-module="flows">Flows</a></li>
								<li data-name="Data"><a id="crud_" href="#crud" data-activate-module="crud">Data</a></li>
								<li data-name="Importer"><a id="importer_" href="#importer" data-activate-module="importer">Importer</a></li>
								<li data-name="Localization"><a id="localization_" href="#localization" data-activate-module="localization">Localization</a></li>
								<li data-name="Virtual Types" class="module-dependent" data-structr-module="api-builder"><a id="virtual-types_" href="#virtual-types" data-activate-module="virtual-types">Virtual Types</a></li>
								<li data-name="Mail Templates" class="edition-dependent" data-structr-edition="Enterprise"><a id="mail-templates_" href="#mail-templates" data-activate-module="mail-templates">Mail Templates</a></li>
								<li data-name="Login"><a id="logout_" href="javascript:void(0)">Login</a></li>
							</ul>
						</li>
					</ul>
				</div>
				<div class="structr-instance-info">
					<div class="structr-instance">
						<span class="structr-instance-db"></span>
						<span class="structr-instance-name"></span>
						<span class="structr-instance-stage"></span>
						<span class="structr-instance-maintenance"></span>
					</div>
					<div class="structr-version flex items-center h-4"></div>
				</div>
				<div id="main-help">
					<a target="_blank" href="https://support.structr.com/knowledge-graph"></a>
				</div>
			</div>

			<div class="hidden" id="structr-console"></div>
			<div class="hidden" id="structr-favorites"></div>

			<div id="function-bar"></div>
			<div id="main"></div>

			<div id="custom-context-menu-container"></div>
		`,
		defaultDialogMarkup: config => ``,
		loginDialogMarkup: `
			<div id="login" class="dialog p-6 text-left">

				${_Icons.getSvgIcon(_Icons.iconStructrLogo, 90, 24, ['logo-login'])}

				<form id="login-username-password" action="javascript:void(0);">

					<div id="username-password" class="gap-y-2 grid ml-1 mr-4" style="grid-template-columns: 35fr 65fr;">

						<div class="self-center" style="">
							<label for="usernameField">Username:</label>
						</div>

						<div class="self-center">
							<input id="usernameField" type="text" name="username" autocomplete="username" required class="w-full box-border">
						</div>

						<div class="self-center">
							<label for="passwordField">Password:</label>
						</div>

						<div class="self-center">
							<input id="passwordField" type="password" name="password" autocomplete="current-password" required class="w-full box-border">
						</div>

						<div class="self-center col-span-2 mt-2 text-right">
							<button id="loginButton" name="login" class="inline-flex mr-0 items-center hover:bg-gray-100 hover:bg-gray-100 focus:border-gray-666 active:border-green">
								${_Icons.getSvgIcon(_Icons.iconVisibilityKey, 16, 16, ['mr-2'])} Login
							</button>
						</div>
					</div>
				</form>

				<form id="login-two-factor" action="javascript:void(0);" style="display:none;">

					<div id="two-factor" class="gap-y-2 grid ml-1 mr-4" style="grid-template-columns: 35fr 65fr;">

						<div id="two-factor-qr-code" class="col-span-2 text-center" style="display: none;">
							<div>
								<img>
							</div>
							<div class="mt-2 mb-3">Scan this QR Code with a Google Authenticator compatible app to log in.</div>
						</div>

						<div class="self-center">
							<label for="twoFactorTokenField">2FA Code:</label>
						</div>

						<div class="self-center">
							<input id="twoFactorTokenField" type="hidden" name="twoFactorToken">
							<input id="twoFactorCodeField" type="text" name="twoFactorCode" required class="w-full box-border">
						</div>

						<div id="self-center" class="col-span-2 mt-2 text-right">
							<button id="loginButtonTFA" name="login" class="inline-flex mr-0 items-center hover:bg-gray-100 hover:bg-gray-100 focus:border-gray-666 active:border-green">
								${_Icons.getSvgIcon(_Icons.iconVisibilityKey, 16, 16, ['mr-2'])} Login 2FA
							</button>
						</div>
					</div>
				</form>
			</div>
		`
	}
};

let _TreeHelper = {
	initTree: (tree, initFunction, stateKey) => {

		let initializedTree = $(tree).jstree({
			plugins: ["themes", "search", "state", "types", "wholerow"],
			core: {
				animation: 0,
				async: true,
				data: initFunction
			},
			state: {
				key: stateKey
			}
		});

		_TreeHelper.addSvgIconReplacementBehaviorToTree(initializedTree);
	},
	addSvgIconReplacementBehaviorToTree: (tree) => {

		let getSvgIconFromNode = (node) => {
			return node?.data?.svgIcon ?? node?.state?.svgIcon;
		};

		let replaceIconWithSvgIfPresent = (nodeId) => {

			let node = $(tree).jstree().get_node(nodeId);

			// recursively change children icons (if node is opened)
			if (node.state.opened) {
				node.children?.map(replaceIconWithSvgIfPresent);
			}

			let svgIcon = getSvgIconFromNode(node);
			if (svgIcon) {

				let anchor = document.getElementById(node.a_attr.id);
				if (anchor) {
					let icon = anchor.querySelector('.jstree-icon');
					if (icon) {

						icon.style     = null;
						icon.innerHTML = svgIcon;
					}
				}
			}
		};

		let setSvgFolderIcon = (nodeId, newStateIsOpen) => {

			let node   = $(tree).jstree().get_node(nodeId);
			let anchor = document.getElementById(node.a_attr.id);

			if (anchor) {

				let from = _Icons.iconFolderClosed;
				let to   = _Icons.iconFolderOpen;

				let currentIcon = _Icons.getSvgIconFromSvgElement(anchor);

				if (currentIcon === _Icons.iconMountedFolderOpen || currentIcon === _Icons.iconMountedFolderClosed) {
					from = _Icons.iconMountedFolderClosed;
					to   = _Icons.iconMountedFolderOpen;
				} else if (currentIcon !== from && currentIcon !== to) {
//					return;
				}

				if (newStateIsOpen === false) {
					let tmp = to;
					to = from;
					from = tmp;
				}

				_Icons.updateSvgIconInElement(anchor, from, to);

			} else {

				// no anchor found
				if (node.data.svgIcon && newStateIsOpen) {

					if (
						node.data.svgIcon.indexOf(`"#${_Icons.iconFolderOpen}"`) !== -1 ||
						node.data.svgIcon.indexOf(`"#${_Icons.iconFolderClosed}"`) !== -1 ||
						node.data.svgIcon.indexOf(`"#${_Icons.iconMountedFolderOpen}"`) !== -1 ||
						node.data.svgIcon.indexOf(`"#${_Icons.iconMountedFolderClosed}"`) !== -1
					) {
						node.data.svgIcon = _Icons.getSvgIcon(_Icons.iconFolderOpen);
					}
				}
			}
		};

		tree.on('after_open.jstree', (event, data) => {

			if (data.node.id !== 'root') {

				let svgIcon = getSvgIconFromNode(data.node);
				if (svgIcon) {
					setSvgFolderIcon(data.node.id, true);
				}
			}

			data.node.children?.map(replaceIconWithSvgIfPresent);
		});

		tree.on('after_close.jstree', (event, data) => {

			if (data.node.id !== 'root') {
				let svgIcon = getSvgIconFromNode(data.node);
				if (svgIcon) {
					setSvgFolderIcon(data.node.id, false);
				}
			}
		});

		tree.on('redraw.jstree', (event, data) => {
			data.nodes?.map(replaceIconWithSvgIfPresent);
		});
	},
	deepOpen: (tree, element, parentElements, parentKey, selectedNodeId) => {

		if (element && element.id) {

			parentElements = parentElements || [];
			parentElements.unshift(element);

			Command.get(element.id, parentKey, (loadedElement) => {
				if (loadedElement && loadedElement[parentKey]) {
					_TreeHelper.deepOpen(tree, loadedElement[parentKey], parentElements, parentKey, selectedNodeId);
				} else {
					_TreeHelper.open(tree, parentElements, selectedNodeId);
				}
			});
		}
	},
	open: (tree, dirs, selectedNode) => {

		if (dirs.length) {

			tree.jstree('deselect_all');

			let openRecursively = (list) => {

				if (list.length > 0) {

					let first = list.shift();

					tree.jstree('open_node', first.id, () => {
						openRecursively(list);
					});

				} else {
					tree.jstree('select_node', selectedNode);
				}
			};

			openRecursively(dirs);
		}

	},
	refreshTree: (tree, callback) => {
		$(tree).jstree('refresh');

		if (typeof callback === "function") {
			window.setTimeout(callback, 500);
		}
	},
	refreshNode: (tree, node, callback) => {
		$(tree).jstree('refresh_node', node);

		if (typeof callback === "function") {
			window.setTimeout(callback, 500);
		}
	},
	getNode: (tree, node) => {
		return $(tree).jstree('get_node', node);
	},
	isNodeOpened: (tree, node) => {
		let n = _TreeHelper.getNode(tree, node);

		return n?.state.opened;
	},
	makeAllTreeElementsDroppable: (tree, dragndropFunction) => {

		for (let node of tree[0].querySelectorAll('.jstree-node:not(.has-drop-behavior)')) {

			// TODO: ids starting with digits are not allowed, rewriting this to vanilla will hurt (for every tree)
			let el = $('#' + node.id, tree);

			let modelObj = StructrModel.obj(node.id);

			if (modelObj) {
				dragndropFunction?.(modelObj, el[0]);
			} else {
				dragndropFunction?.({ id: node.id, type: 'fake' }, el[0]);
			}

			el[0].classList.add('has-drop-behavior');
		}
	}
};

class MessageBuilder {

	static types = Object.freeze({
		success: 'success',
		warning: 'warning',
		error: 'error',
		info: 'info'
	});

	constructor (typeClass) {

		if (!MessageBuilder.types[typeClass]) {
			throw new Error('MessageBuilder: Unknown type, please fix');
		}

		this.typeClass = typeClass;

		// defaults
		this.params = {
			text: 'Default message',
			delayDuration: 3000,
			confirmButtonText: 'Confirm',
			allowConfirmAll: false,
			confirmAllButtonText: 'Confirm all...',
			uniqueClass: undefined,
			uniqueCount: 1,
			updatesText: false,
			updatesButtons: false,
			appendsText: false,
			appendSelector: '',
			incrementsUniqueCount: false
		};
	}

	requiresConfirmation(confirmButtonText = this.params.confirmButtonText) {
		this.params.requiresConfirmation = true;
		this.params.confirmButtonText = confirmButtonText;
		return this;
	};

	allowConfirmAll(confirmAllButtonText = this.params.confirmAllButtonText) {
		this.params.allowConfirmAll = true;
		this.params.confirmAllButtonText = confirmAllButtonText;

		return this;
	};

	text(text) {
		this.params.text = text;
		return this;
	};

	title(title) {
		this.params.title = title;
		return this;
	};

	delayDuration(delayDuration) {
		this.params.delayDuration = delayDuration;
		return this;
	};

	getButtonHtml() {

		return `
			${(this.params.requiresConfirmation ? `<button class="confirm inline-flex items-center hover:border-gray-666">${this.params.confirmButtonText}</button>` : '')}
			${(this.params.requiresConfirmation && this.params.allowConfirmAll ? `<button class="confirmAll inline-flex items-center hover:border-gray-666">${this.params.confirmAllButtonText}</button>` : '')}
			${(this.params.specialInteractionButton ? `<button class="special inline-flex items-center hover:border-gray-666">${this.params.specialInteractionButton.text}</button>` : '')}
		`;
	};

	activateButtons() {

		if (this.params.requiresConfirmation === true) {

			document.querySelector(`#${this.params.msgId} button.confirm`).addEventListener('click', (e) => {
				e.target.closest('button').remove();
				this.dismiss();
			});

			if (this.params.allowConfirmAll === true) {

				document.querySelector(`#${this.params.msgId} button.confirmAll`).addEventListener('click', () => {
					for (let confirmButton of document.querySelectorAll(`#info-area button.confirm`)) {
						confirmButton.click();
					}
				});
			}

		} else {

			window.setTimeout(() => {
				this.dismiss();
			}, this.params.delayDuration);

			document.querySelector(`#${this.params.msgId}`).addEventListener('click', () => {
				this.dismiss();
			});
		}

		if (this.params.specialInteractionButton) {

			document.querySelector(`#${this.params.msgId} button.special`).addEventListener('click', () => {
				this.params.specialInteractionButton.action();

				this.dismiss();
			});
		}
	};

	show() {

		let uniqueMessageAlreadyPresented = false;
		let allClasses                    = ['message', 'break-word', 'flex', 'rounded-md', 'pt-5', 'pl-5', 'pr-3', 'pb-3', 'm-1', this.typeClass, this.params.uniqueClass];

		if (this.params.uniqueClass) {

			// find existing one
			let existingMessage = document.querySelector(`#info-area .message.${this.params.uniqueClass}`);
			if (existingMessage) {

				uniqueMessageAlreadyPresented = true;
				this.params.msgId             = existingMessage.id;
				this.params.uniqueCount       = existingMessage.dataset['uniqueCount'];

				let titleElement = existingMessage.querySelector('.title');

				existingMessage.querySelector('.message-icon').innerHTML = _Icons.getSvgIcon(_Icons.getSvgIconForMessageClass(this.typeClass));

				if (this.params.incrementsUniqueCount) {
					this.params.uniqueCount++;

					existingMessage.dataset['uniqueCount'] = this.params.uniqueCount;
					existingMessage.querySelector('span.uniqueCount')?.replaceWith(_Helpers.createSingleDOMElementFromHTML(this.getUniqueCountElement()));
				}

				existingMessage.setAttribute('class', allClasses.join(' '));

				if (this.params.updatesText) {

					if (titleElement) {
						titleElement.innerHTML = this.params.title;
					}
					existingMessage.querySelector('.message-text').innerHTML = this.params.text;

				} else if (this.params.appendsText) {

					if (titleElement) {
						titleElement.innerHTML = this.params.title;
					}

					let selector = `.message-text ${((this.params.appendSelector !== '') ? this.params.appendSelector : '')}`;
					existingMessage.querySelector(selector).insertAdjacentHTML('beforeend', this.params.text);
				}

				if (this.params.updatesButtons) {

					let buttonsContainer = existingMessage.querySelector('.message-buttons');
					_Helpers.fastRemoveAllChildren(buttonsContainer);

					buttonsContainer.insertAdjacentHTML('beforeend', this.getButtonHtml());
					this.activateButtons();
				}
			}
		}

		if (uniqueMessageAlreadyPresented === false) {

			this.params.msgId = `message_${Structr.msgCount++}`;

			let message = _Helpers.createSingleDOMElementFromHTML(`
				<div class="${allClasses.join(' ')}" id="${this.params.msgId}" data-unique-count="${this.params.uniqueCount}">
					<div class="message-icon flex-shrink-0 mr-2">
						${_Icons.getSvgIcon(_Icons.getSvgIconForMessageClass(this.typeClass))}
					</div>
					<div class="flex-grow">
						${(this.params.title ? `<div class="mb-1 -mt-1 font-bold text-lg">${this.params.title}${this.getUniqueCountElement()}</div>` : this.getUniqueCountElement())}
						<div class="message-text">
							${this.params.text}
						</div>
						<div class="message-buttons text-right mb-1">
							${this.getButtonHtml()}
						</div>
					</div>
				</div>
			`);

			document.querySelector('#info-area').appendChild(message);

			this.activateButtons();
		}
	};

	dismiss() {

		let msgElement = document.querySelector(`#${this.params.msgId}`);

		if (msgElement) {
			msgElement.addEventListener('animationend', () => {
				_Helpers.fastRemoveElement(msgElement);
			});

			msgElement.classList.add('dismissed');
		}
	};

	specialInteractionButton(buttonText, callback, confirmButtonText) {

		this.params.specialInteractionButton = {
			text: buttonText,
			action: callback
		};

		return this.requiresConfirmation(confirmButtonText);
	};

	uniqueClass(className) {
		if (className) {
			className = className.replace(/[\/\. ]/g, "_");
			this.params.uniqueClass = className;
		}
		return this;
	};

	incrementsUniqueCount() {
		this.params.incrementsUniqueCount = true;
		return this;
	};

	updatesText() {
		this.params.updatesText = true;
		return this;
	};

	updatesButtons() {
		this.params.updatesButtons = true;
		return this;
	};

	appendsText(selector = '') {
		this.params.appendsText    = true;
		this.params.appendSelector = selector;
		return this;
	};

	getUniqueCountElement() {
		return `<span class="uniqueCount ml-1 empty:hidden">${(this.params.uniqueCount > 1) ? `(${this.params.uniqueCount})` : ''}</span>`;
	};
}

class SuccessMessage extends MessageBuilder {
	constructor() {
		super(MessageBuilder.types.success);
	}
}

class WarningMessage extends MessageBuilder {
	constructor() {
		super(MessageBuilder.types.warning);
	}
}

class InfoMessage extends MessageBuilder {
	constructor() {
		super(MessageBuilder.types.info);
	}
}

class ErrorMessage extends MessageBuilder {
	constructor() {
		super(MessageBuilder.types.error);
	}
}

let UISettings = {
	getValueForSetting: (setting) => {
		return LSWrapper.getItem(setting.storageKey, setting.defaultValue);
	},
	setValueForSetting: (setting, value, container) => {
		LSWrapper.setItem(setting.storageKey, value);

		if (container) {
			_Helpers.blinkGreen(container);
			setting.onUpdate?.();
		}
	},
	getSettings: (section) => {

		if (!section) {
			// no section given - return all
			return Object.values(UISettings.settingGroups);

		} else {

			let settings = UISettings.settingGroups[section];
			if (settings) {
				return settings;
			}
		}

		return null;
	},
	showSettingsForCurrentModule: (...additionalSettingsGroups) => {

		let settingsGroupsToShow = [UISettings.settingGroups.global];

		let moduleSettings = UISettings.getSettings(Structr.getActiveModuleName());
		if (moduleSettings) {
			settingsGroupsToShow.push(moduleSettings);
		}

		settingsGroupsToShow.push(...additionalSettingsGroups);

		let dropdown = _Helpers.createSingleDOMElementFromHTML(`<div id="ui-settings-popup" class="dropdown-menu darker-shadow-dropdown dropdown-menu-large">
			<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green" data-preferred-position-x="left">
				${_Icons.getSvgIcon(_Icons.iconUIConfigSettings)}
			</button>
			<div class="dropdown-menu-container" style="display: none;"></div>
		</div>`);

		let container = dropdown.querySelector('.dropdown-menu-container');

		for (let settingsGroup of settingsGroupsToShow) {
			UISettings.appendSettingsSectionToContainer(settingsGroup, container);
		}

		_Helpers.activateCommentsInElement(container);

		Structr.functionBar.appendChild(dropdown);
	},
	appendSettingsSectionToContainer: (section, container) => {

		let sectionDOM = _Helpers.createSingleDOMElementFromHTML(`<div><div class="font-bold pt-4 pb-2">${section.title}</div></div>`);

		for (let [settingKey, setting] of Object.entries(section.settings)) {
			UISettings.appendSettingToContainer(setting, sectionDOM);
		}

		container.appendChild(sectionDOM);
	},
	appendSettingToContainer: (setting, container) => {

		switch (setting.type) {

			case 'checkbox': {

				let settingDOM = _Helpers.createSingleDOMElementFromHTML(`<label class="flex items-center p-1"><input type="checkbox"> ${setting.text}</label>`);

				let input = settingDOM.querySelector('input');
				input.checked = UISettings.getValueForSetting(setting);

				input.addEventListener('change', () => {
					UISettings.setValueForSetting(setting, input.checked, input.parentElement);
				});

				if (setting.infoText) {
					settingDOM.dataset['comment'] = setting.infoText;
				}

				container.appendChild(settingDOM);

				break;
			}

			default: {
				console.log('ERROR! Unable to render setting:', setting, container);
			}
		}
	},
	settingGroups: {
		global: {
			title: 'Global',
			settings: {
				showScriptingErrorPopupsKey: {
					text: 'Show popups for scripting errors',
					storageKey: 'showScriptinErrorPopups' + location.port,
					defaultValue: true,
					type: 'checkbox'
				},
				showResourceAccessGrantWarningPopupsKey: {
					text: 'Show popups for resource access grant warnings',
					storageKey: 'showResourceAccessGrantWarningPopups' + location.port,
					defaultValue: true,
					type: 'checkbox'
				},
				showDeprecationWarningPopupsKey: {
					text: 'Show popups for deprecation warnings',
					storageKey: 'showDeprecationWarningPopups' + location.port,
					defaultValue: true,
					type: 'checkbox'
				},
			}
		},
		pages: {
			title: 'Pages',
			settings: {
				inheritVisibilityFlagsKey: {
					text: 'Inherit Visibility Flags from parent node (when creating new elements from the context menu)',
					storageKey: 'inheritVisibilityFlags_' + location.port,
					defaultValue: true,
					type: 'checkbox'
				},
				inheritGranteesKey: {
					text: 'Inherit permissions from parent node (when creating new elements from the context menu)',
					storageKey: 'inheritGrantees_' + location.port,
					defaultValue: true,
					type: 'checkbox'
				},
				favorEditorForContentElementsKey: {
					text: 'Always favor editor for content elements in Pages area (otherwise last used is picked)',
					storageKey: 'favorEditorForContentElements' + location.port,
					defaultValue: true,
					type: 'checkbox'
				},
				favorHTMLForDOMNodesKey: {
					text: 'Always favor HTML tab for DOM nodes in Pages area (otherwise last used is picked)',
					storageKey: 'favorHTMLForDOMNodes' + location.port,
					defaultValue: true,
					type: 'checkbox'
				}
			}
		},
		security: {
			title: 'Security',
			settings: {
				showGroupsHierarchicallyKey: {
					text: 'List groups hierarchically',
					infoText: 'If active, groups that are contained in other groups are not shown at top level.<br><br>This impacts filtering on name - only top level elements are filtered',
					storageKey: 'showGroupsHierarchicallyKey' + location.port,
					defaultValue: true,
					type: 'checkbox',
					onUpdate: () => {
						if (Structr.isModuleActive(_Security)) {
							_UsersAndGroups.refreshGroups();
						}
					}
				},
				showVisibilityFlagsInGrantsTableKey: {
					text: 'Show visibility flags in Resource Access Grants table',
					storageKey: 'showVisibilityFlagsInResourceAccessGrantsTable' + location.port,
					defaultValue: false,
					type: 'checkbox',
					onUpdate: () => {
						if (Structr.isModuleActive(_Security)) {
							_ResourceAccessGrants.refreshResourceAccesses();
						}
					}
				}
			}
		},
		importer: {
			title: 'Importer',
			settings: {
				showNotificationsKey: {
					text: 'Show notifications for scheduled jobs',
					storageKey: 'structrImporterShowNotifications_' + location.port,
					defaultValue: true,
					type: 'checkbox'
				}
			}
		},
		schema_code: {
			title: 'Schema/Code',
			settings: {
				showDatabaseNameForDirectProperties: {
					text: 'Show database name for direct properties',
					storageKey: 'showDatabaseNameForDirectProperties' + location.port,
					defaultValue: false,
					type: 'checkbox',
					onUpdate: () => {
					}
				},
				showJavaMethodsForBuiltInTypes: {
					text: 'Show Java methods for built-in types',
					storageKey: 'structrShowJavaMethods_' + location.port,
					defaultValue: false,
					type: 'checkbox',
					infoText: 'Advanced Feature: Shows built-in Java methods but changes are not possible',
					onUpdate: () => {
						if (Structr.isModuleActive(_Code)) {
							_Code.codeTree.jstree().refresh();
						}
					}
				}
			}
		},
		code: {
			title: 'Code',
			settings: {
				showRecentsInCodeArea: {
					text: 'Show recently visited elements',
					storageKey: 'showRecentElementsInCode' + location.port,
					defaultValue: true,
					type: 'checkbox',
					onUpdate: () => {
						if (Structr.isModuleActive(_Code)) {
							_Code.recentElements.updateVisibility();
						}
					}
				}
			}
		}
	}
};

window.addEventListener('beforeunload', (event) => {

	if (event.target === document) {

		let activeModule = Structr.getActiveModule();
		if (activeModule && activeModule.beforeunloadHandler && typeof activeModule.beforeunloadHandler === 'function') {
			let ret = activeModule.beforeunloadHandler();
			if (ret) {
				event.returnValue = ret;
			}
			// persist last menu entry
			LSWrapper.setItem(Structr.mainMenu.lastMenuEntryKey, Structr.mainMenu.lastMenuEntry);
		}

		// Remove dialog data in case of page reload
		LSWrapper.removeItem(_Dialogs.custom.dialogDataKey);
		LSWrapper.save();
	}
});