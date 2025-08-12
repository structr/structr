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

	Structr.determineNotificationAreaVisibility();

	/* Message-area: Hook up "Close All" button */
	document.querySelector('#info-area #close-all-button').addEventListener('click', () => {
		for (let confirmButton of document.querySelectorAll(`#info-area .${MessageBuilder.closeButtonClass}`)) {
			confirmButton.dispatchEvent(new Event('click'));
		}
	});

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

	document.body.addEventListener('keyup', async (event) => {

		let keyCode = event.keyCode;
		let code    = event.code;

		if (code === 'Escape' || code === 'Esc' || keyCode === 27) {

			if (Structr.ignoreKeyUp) {
				Structr.ignoreKeyUp = false;
				return false;
			}

			await _Dialogs.custom.checkSaveOrCloseOnEscapeKeyPressed();
		}

		return false;
	});

	document.body.addEventListener('keydown', (event) => {

		let keyCode = event.keyCode;
		let code    = event.code;

		// ctrl-s / cmd-s
		if ((code === 'KeyS' || keyCode === 83) && ((!_Helpers.isMac() && event.ctrlKey) || (_Helpers.isMac() && event.metaKey))) {
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
			_Favorites.showFavorites();
		}

		// Ctrl-Alt-p
		if ((code === 'KeyP' || keyCode === 80) && event.altKey && event.ctrlKey) {
			event.preventDefault();
			let uuid = prompt('Enter the UUID for which you want to open the properties dialog');
			if (!uuid) {
				// ESC or Cancel
			} else if (_Helpers.isUUID(uuid)) {
				Command.get(uuid, null, (obj) => {
					_Entities.showProperties(obj, null, true);
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
					_Entities.showProperties(obj, 'permissions');
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
	dynamicClassPrefix: 'org.structr.dynamic.',
	getFQCNForDynamicTypeName: name => Structr.dynamicClassPrefix + name,
	ignoreKeyUp: undefined,
	isInMemoryDatabase: undefined,
	modules: {},
	activeModules: {},
	envInfoAvailableCallbacks: [],
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
		visibleToPublicUsers: "Publ. Users",
		visibleToAuthenticatedUsers: "Auth. Users"
	},
	dialogTimeoutId: undefined,
	instanceName: '',
	instanceStage: '',
	getDiffMatchPatch: () => {
		if (!Structr.diffMatchPatch) {
			Structr.diffMatchPatch = new diff_match_patch();
		}
		return Structr.diffMatchPatch;
	},
	getRequestParameterName: (key) => (Structr.legacyRequestParameters === true) ? key : `_${key}`,
	setFunctionBarHTML: (html) => _Helpers.setContainerHTML(Structr.functionBar, html),
	setMainContainerHTML: (html) => _Helpers.setContainerHTML(Structr.mainContainer, html),

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

			let reconnectDialogElement = dialogBox.querySelector('#reconnect-dialog');
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
		});
	},
	updateUsername: (name) => {
		if (name !== StructrWS.user) {
			StructrWS.user = name;
			$('#logout_').html(`Logout <span class="username">${name}</span>`);
		}
	},
	updateDocumentTitle: () => {

		let activeMenuEntry = Structr.mainMenu.getActiveEntry()?.dataset['name'] ?? '';

		let instance = (Structr.instanceName === '' && Structr.instanceStage === '') ? '' : ` - ${Structr.instanceName} (${Structr.instanceStage})`;

		document.title = `Structr ${activeMenuEntry}${instance}`;
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
		let lineJoin  = (useHtml ? '<br>' : '\n');

		if (response.errors && response.errors.length) {

			let errorLines = [response.message];

			let uniqueErrors = Object.values(Object.fromEntries(response.errors.map(e => [JSON.stringify(e), e])));

			for (let error of uniqueErrors) {

				let errorMsg = error.type ?? '';
				if (error.property) {
					errorMsg += `.${error.property}`;
				}
				if (error.value) {
					errorMsg += ` ${error.value}`;
				}
				if (error.token) {
					errorMsg += ` ${error.token}`;
				}
				if (error.detail || error.existingNodeUuid) {
					if (errorMsg.trim().length > 0) {
						errorMsg += ': ';
					}
					errorMsg += error.existingNodeUuid ?? error.detail;
				}

				errorLines.push(errorMsg);
			}

			errorText = errorLines.join(lineJoin);

		} else {

			if (url) {
				errorText = url + ': ';
			}

			errorText += response.code + lineJoin;
			errorText += Object.entries(response).filter(([k, v]) => (k !== 'code' && v && v.length > 0)).map(([k, v]) => (useHtml) ? `<b>${k}</b>: ${v}` : `${k}: ${v}`).join(lineJoin);
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

			if (additionalParameters.delayDuration) {
				messageBuilder.delayDuration(additionalParameters.delayDuration);
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
	node: (id, prefix = '#id_', container = document) => {

		let node = $($(prefix + id, $(container))[0]);

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

			document.querySelector('.column-resizer-left')?.classList.toggle('hidden', selectedSlideoutWasOpen);

			if (selectedSlideoutWasOpen === false) {

				Structr.slideouts.closeLeftSlideOuts(otherSlideouts, closeCallback);
				Structr.slideouts.openLeftSlideOut(triggerEl, slideoutElement, openCallback);

			} else {

				Structr.slideouts.closeLeftSlideOuts([slideoutElement], closeCallback);
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

			document.querySelector('.column-resizer-right')?.classList.toggle('hidden', selectedSlideoutWasOpen);

			if (selectedSlideoutWasOpen === false) {

				Structr.slideouts.closeRightSlideOuts(otherSlideouts, closeCallback);
				Structr.slideouts.openRightSlideOut(triggerEl, slideoutElement, openCallback);

			} else {

				Structr.slideouts.closeRightSlideOuts([slideoutElement], closeCallback);
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

			let deploymentActive = (envInfo.isDeploymentActive ?? false);
			if (deploymentActive === true) {
				Structr.handleGenericMessage({
					type: 'DEPLOYMENT_IMPORT_STATUS',
					subtype: 'ALREADY_RUNNING'
				});
			}

			$('#header .structr-instance-name').text(envInfo.instanceName);
			$('#header .structr-instance-stage').text(envInfo.instanceStage);

			Structr.instanceName  = envInfo.instanceName;
			Structr.instanceStage = envInfo.instanceStage;
			Structr.updateDocumentTitle();

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
						<span>${ui.version}</span>
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

			Structr.activeModules = envInfo.modules;

			Structr.adaptUiToAvailableFeatures();

			let userConfigMenu = Structr.mainMenu.getSavedMenuConfig();

			Structr.mainMenu.update(userConfigMenu);

			_Helpers.softlimit.resultCountSoftLimit = envInfo.resultCountSoftLimit ?? _Helpers.softlimit.resultCountSoftLimit;

			// run previously registered callbacks
			let registeredCallbacks = Structr.envInfoAvailableCallbacks;
			Structr.envInfoAvailableCallbacks = [];
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
		defaultMainMenuItems: ['Dashboard', 'Pages', 'Files', 'Security', 'Schema', 'Code', 'Data'],
		getSavedMenuConfig: () => {

			return LSWrapper.getItem(Structr.keyMenuConfig, {
				main: Structr.mainMenu.defaultMainMenuItems,
				sub: []
			});
		},
		update: (menuConfig, updateLS = true) => {

			if (updateLS === true) {
				LSWrapper.setItem(Structr.keyMenuConfig, menuConfig);
			}

			let menu      = document.querySelector('#menu');
			let submenu   = document.querySelector('#submenu');
			let hamburger = document.querySelector('#menu li.submenu-trigger');

			// first move all elements from main menu to submenu
			submenu.append(...menu.querySelectorAll('li[data-name]'));

			if (menuConfig) {

				for (let entry of menuConfig.main) {
					hamburger.before(menu.querySelector(`li[data-name="${entry}"]`));
				}

				// sort submenu
				for (let entry of menuConfig.sub) {
					let element = submenu.querySelector('li:last-of-type');
					if (element.length > 0) {
						element.after(menu.querySelector(`li[data-name="${entry}"]`));
					}
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

			for (let entry of document.querySelectorAll('.menu li')) {
				entry.classList.remove('active');
			}

			li.classList.add('active');

			Structr.updateDocumentTitle();
			window.location.hash = Structr.mainMenu.lastMenuEntry;

			if (Structr.mainMenu.lastMenuEntry && Structr.mainMenu.lastMenuEntry !== 'logout') {
				LSWrapper.setItem(Structr.mainMenu.lastMenuEntryKey, Structr.mainMenu.lastMenuEntry);
			}
		},
		getActiveEntry: () => {
			return document.querySelector('#menu .active');
		}
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
		return true; // Structr.activeModules[moduleName] !== undefined;
	},
	isModuleInformationAvailable: () => {
		return (Object.keys(Structr.activeModules).length > 0);
	},
	performActionAfterEnvResourceLoaded: (action) => {
		if (Structr.isModuleInformationAvailable()) {
			action();
		} else {
			Structr.registerActionToRunAfterEnvInfoIsAvailable(action);
		}
	},
	registerActionToRunAfterEnvInfoIsAvailable: (cb) => {
		Structr.envInfoAvailableCallbacks.push(cb);
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
		let showResourceAccessPopups       = UISettings.getValueForSetting(UISettings.settingGroups.global.settings.showResourceAccessPermissionWarningPopupsKey);
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

					messageBuilder.title(titles[data.subtype]).uniqueClass('csv-import-status').updatesText().requiresConfirmation().text(texts[data.subtype]).show();
				}
				break;

			case "CSV_IMPORT_WARNING":

				if (StructrWS.me.username === data.username) {
					new WarningMessage().title(data.title).text(data.text).requiresConfirmation().show();
				}
				break;

			case "CSV_IMPORT_ERROR":

				if (StructrWS.me.username === data.username) {
					new ErrorMessage().title(data.title).text(data.text).requiresConfirmation().show();
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
						messageBuilder.updatesText().requiresConfirmation();
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

					new ErrorMessage().title(`Exception while importing ${data.jobtype}`).text(`File: ${data.filepath}<br>${text}`).requiresConfirmation().show();
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
					messageBuilder.title(scriptJobTitles[data.subtype]).text(`<div>${scriptJobTexts[data.subtype]}</div>`).uniqueClass(`${data.jobtype}-${data.subtype}`).prependsText();

					if (data.subtype !== 'QUEUED') {
						messageBuilder.requiresConfirmation();
					}

					messageBuilder.show();
				}

				if (Structr.isModuleActive(Importer)) {
					Importer.updateJobTable();
				}
				break;

			case 'DEPLOYMENT_IMPORT_STATUS':
			case 'DEPLOYMENT_EXPORT_STATUS':
			case 'DEPLOYMENT_DATA_IMPORT_STATUS':
			case 'DEPLOYMENT_DATA_EXPORT_STATUS': {

				let typeTitles      = {
					DEPLOYMENT_IMPORT_STATUS:      'Deployment Import',
					DEPLOYMENT_DATA_IMPORT_STATUS: 'Data Deployment Import',
					DEPLOYMENT_EXPORT_STATUS:      'Deployment Export',
					DEPLOYMENT_DATA_EXPORT_STATUS: 'Data Deployment Export',
				};
				let type            = typeTitles[data.type];
				let messageCssClass = 'deployment-message';
				let isImport        = (data.type === 'DEPLOYMENT_IMPORT_STATUS' || data.type === 'DEPLOYMENT_DATA_IMPORT_STATUS');

				if (data.subtype === 'BEGIN') {

					let text = `${type} started: ${new Date(data.start)}<br>
						${(isImport ? 'Importing from' : 'Exporting to')}: <span class="deployment-path">${(isImport ? data.source : data.target)}</span><br><br>
						System performance may be affected during Deployment.<br>
						Please wait until the process is finished. Any changes made during a deployment might get lost or conflict with the deployment! This message will be updated during the deployment process.<br>
						<ol class="message-steps"></ol>
					`;

					new InfoMessage().title(`${type} Progress`).uniqueClass(messageCssClass).text(text).requiresConfirmation().updatesText().show();

				} else if (data.subtype === 'ALREADY_RUNNING') {

					type = 'Deployment';
					let text = `A deployment was already running before page was loaded.<br><br>
						Please wait until the deployment process is finished. Any changes made during a deployment might get lost or conflict with the deployment! This message will be updated during the deployment process.<br>
						<ol class="message-steps"></ol>
					`;

					new InfoMessage().title(`${type} Progress`).uniqueClass(messageCssClass).text(text).requiresConfirmation().updatesText().show();

				} else if (data.subtype === 'PROGRESS') {

					let chunkInfos     = [
						(data.curChunkTime)  ? `chunk ${(data.curChunkTime  / 1000).toFixed(2)}s` : '',
						(data.meanChunkTime) ? `&oslash; ${(data.meanChunkTime / 1000).toFixed(2)}s` : ''
					].filter(s => (s !== '')).join(', ');
					let timeInfo      = (chunkInfos.length > 0) ? `(${chunkInfos})` : '';
					let truncatedType = (data.typeName) ? `<span class="inline-block align-bottom max-w-64 truncate font-bold" title="${data.typeName}">${data.typeName}</span>` : '';
					let message       = `<li id="${data.messageId ?? ''}">${data.message} ${truncatedType} ${data.progress ?? ''} ${timeInfo}</li>`;

					let infoMessage = new InfoMessage().title(`${type} Progress`).uniqueClass(messageCssClass).text(message).requiresConfirmation();

					if (data.messageId) {

						infoMessage.replacesElement(`#${data.messageId}`, '.message-steps').show();

					} else {

						infoMessage.appendsText('.message-steps').show();
					}

				} else if (data.subtype === 'END') {

					let text = `<br>${type} finished: ${new Date(data.end)}<br>
						Total duration: ${data.duration}
						${isImport ? '<br><br>Reload the page to see the new data.' : ''}`;

					let finalMessage = new SuccessMessage().title(`${type} finished`).uniqueClass(messageCssClass).text(text).appendsText().requiresConfirmation();

					if (isImport) {

						finalMessage.specialInteractionButton('Reload Page', () => { location.reload(); }).updatesButtons();

						Structr.cleanupAfterDeploymentImport();
					}

					finalMessage.show();
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

					new WarningMessage().title("Certificate retrieval progress").text(data.message).uniqueClass('cert-retrieval').requiresConfirmation().show();
				}

				break;

			case "MAINTENANCE":

				let enabled = data.enabled ? 'enabled' : 'disabled';

				new WarningMessage().title(`Maintenance Mode ${enabled}`).text(`Maintenance Mode has been ${enabled}.<br><br> Redirecting...`).show();

				window.setTimeout(() => {
					location.href = data.baseUrl + location.pathname + location.search;
				}, 1500);
				break;

			case "WARNING":
				new WarningMessage().title(data.title).text(data.message).requiresConfirmation().show();
				break;

			case "INFO":
				new InfoMessage().title(data.title).text(data.message).requiresConfirmation().show();
				break;

			case "SCRIPT_JOB_EXCEPTION":
				new WarningMessage().title('Exception in Scheduled Job').text(data.message).requiresConfirmation().show();
				break;

			case "RESOURCE_ACCESS":

				if (showResourceAccessPopups) {

					let builder = new WarningMessage().title(`REST Access to '${data.uri}' denied`).text(data.message).requiresConfirmation();

					let createPermission = (permissionData) => {

						let maskIndex = (data.validUser ? 'AUTH_USER_' : 'NON_AUTH_USER_') + data.method.toUpperCase();
						let flags     = _ResourceAccessPermissions.mask[maskIndex] || 0;

						_ResourceAccessPermissions.createResourceAccessPermission(data.signature, flags, null, permissionData);

						let resourceAccessKey = 'resource-access';

						let permissionPagerConfig = LSWrapper.getItem(_Pager.pagerDataKey + resourceAccessKey);
						if (!permissionPagerConfig) {

							permissionPagerConfig = {
								id: resourceAccessKey,
								type: resourceAccessKey,
								page: 1,
								pageSize: 25,
								sort: "signature",
								order: "asc"
							};

						} else {

							permissionPagerConfig = JSON.parse(permissionPagerConfig);
						}

						permissionPagerConfig.filters = {
							flags: false,
							signature: data.signature
						};

						LSWrapper.setItem(_Pager.pagerDataKey + resourceAccessKey, JSON.stringify(permissionPagerConfig));

						if (Structr.isModuleActive(_Security)) {

							_Security.selectTab(resourceAccessKey);

						} else {

							LSWrapper.setItem(_Security.securityTabKey, resourceAccessKey);
							window.location.href = '#security';
						}
					};

					if (data.validUser === false) {

						builder.specialInteractionButton('Create and show permission for <b>public</b> users', () => { createPermission({ visibleToPublicUsers: true, grantees: [] }) });

					} else {

						if (data.isServicePrincipal === false) {
							builder.specialInteractionButton(`Create and show permission for user <b>${data.username}</b>`, () => { createPermission({ grantees: [{ id: data.userid, allowed: ['read'] }] }) });
						}

						builder.specialInteractionButton('Create and show permission for <b>authenticated</b> users', () => { createPermission({ visibleToAuthenticatedUsers: true, grantees: [] }) });
					}

					builder.show();
				}

				break;

			case "SCRIPTING_ERROR":

				if (showScriptingErrorPopups) {

					if (data.nodeId && data.nodeType) {

						Command.get(data.nodeId, _Code.helpers.getAttributesToFetchForErrorObject(), (obj) => {

							let name     = data.name.slice(data.name.indexOf('_html_') === 0 ? 6 : 0);
							let property = 'Property';
							let title    = '';

							switch (obj.type) {

								case 'SchemaProperty':
									property = 'Property';
									title    = 'Schema Property';
									break;

								case 'SchemaMethod':

									if (obj.schemaNode) {

										title    = `type "${obj.schemaNode.name}"`;
										property = (obj.isStatic === true) ? 'Static Method' : 'Method';

									} else {

										title    = 'user-defined function';
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

								builder.specialInteractionButton(`Go to code`, () => {

									_Code.helpers.navigateToSchemaObjectFromAnywhere(obj);

								});

							} else {

								builder.specialInteractionButton('Open in editor', () => {

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

								});
							}

							// show message
							builder.show();
						});

					} else {
						new WarningMessage().title('Server-side Scripting Error').text(data.message).requiresConfirmation().show();
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

								builder.specialInteractionButton('Go to element in page tree', () => {

									_Pages.openAndSelectTreeObjectById(obj.id);

								});

								builder.show();
							});

						} else {

							builder.show();
						}

					} else {

						builder.show();
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
	cleanupAfterDeploymentImport: () => {

		_Elements.unselectEntity();	// selected entity could be in shadow page

		_Pages.updateShadowPageAfterDeployment();
	},
	showReconnectDialog: () => {

		let restoreDialogText = '';
		let dialogData = JSON.parse(LSWrapper.getItem(_Dialogs.custom.dialogDataKey));
		if (dialogData && dialogData.text) {
			restoreDialogText = `<div>The dialog</div><b>"${dialogData.text}"</b><div>will be restored after reconnect.</div>`;
		}

		let reconnectDialog = `
			<div id="reconnect-dialog" class="dialog">
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

		_Dialogs.basic.append(reconnectDialog);
	},
	getReconnectDialogElement: () => {
		return document.getElementById('reconnect-dialog');
	},
	hideReconnectDialog: () => {
		// remove reconnect dialog
		let reconnectMessage = Structr.getReconnectDialogElement();
		_Dialogs.basic.removeBlockerAround(reconnectMessage);
	},
	dropdownOpenEventName: 'dropdown-opened',
	handleDropdownClick: (e) => {

		let menu = e.target.closest('.dropdown-menu');

		if (menu) {

			let container = menu.querySelector('.dropdown-menu-container');

			Structr.showDropdownContainer(container);
		}
	},
	showDropdownContainer: (container) => {

		if (container) {

			let isVisible = (container.dataset['visible'] === 'true');

			if (isVisible) {

				Structr.hideDropdownContainer(container);

			} else {

				Structr.hideOpenDropdownsExcept(container);

				let btn           = container.closest('.dropdown-menu').querySelector('.dropdown-select');
				let btnRect       = btn.getBoundingClientRect();

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
					let offsetParentRect  = btn.offsetParent.getBoundingClientRect();
					container.style.right = `${(offsetParentRect.width + offsetParentRect.left) - btnRect.right}px`;

				} else {

					// allow positioning to change between openings
					container.style.right = null;
				}

				if (btn.dataset['preferredPositionY'] === 'top') {

					// position dropdown over activator button
					container.style.bottom    = `calc(${window.innerHeight - btnRect.top}px + 0.25rem)`;

				} else {

					// allow positioning to change between openings
					container.style.bottom = null;
				}

				container.dispatchEvent(new CustomEvent(Structr.dropdownOpenEventName, {
					bubbles: true,
					detail: container
				}));
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
	determineNotificationAreaVisibility: () => {

		document.querySelector('#info-area').classList.toggle('hidden', UISettings.getValueForSetting(UISettings.settingGroups.global.settings.hideAllPopupMessagesKey));
	},


	/* basically only exists to get rid of repeating strings. is also used to filter out internal keys from dialogs */
	internalKeys: {
		name: 'name',
		visibleToPublicUsers: 'visibleToPublicUsers',
		visibleToAuthenticatedUsers: 'visibleToAuthenticatedUsers',

		sourceId: 'sourceId',
		targetId: 'targetId',
		sourceNode: 'sourceNode',
		targetNode: 'targetNode',
		internalTimestamp: 'internalTimestamp',
	},

	templates: {
		mainBody: config => `

			<div id="info-area">
				<div id="close-all-button" class="mt-4 mb-2 mx-2 text-right">
					<button class="confirm hover:border-gray-666 bg-white mr-0">Close All</button>
				</div>
				<div id="messages" class="py-1"></div>
			</div>

			<div id="header">

				${_Icons.getSvgIcon(_Icons.iconStructrLogo, 90, 24, ['logo'])}

				<div id="menu" class="menu">
					<ul>
						<li class="submenu-trigger" data-toggle="popup" data-target="#submenu">

							${_Icons.getSvgIcon(_Icons.iconHamburgerMenu, 10, 10, ['icon-white'])}

							<ul id="submenu">
								<li data-name="Dashboard"><a id="dashboard_" href="#dashboard" data-activate-module="dashboard">Dashboard</a></li>
								<li data-name="Graph"><a id="graph_" href="#graph" data-activate-module="graph">Graph</a></li>
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

				<div id="login-sso" class="mx-4 mt-6 hidden">

					<div class="mb-4 w-full text-center">Or continue with:</div>

					${_Dialogs.loginDialog.getOauthProviders().map(({ name, uriPart, iconId })  => `
						<button id="sso-login-${uriPart}" onclick="javascript:document.location='${_Dialogs.loginDialog.getSSOUriForURIPart(uriPart)}';" class="btn w-full mr-0 hover:bg-gray-100 focus:border-gray-666 active:border-green hidden gap-2 items-center justify-center p-3 mb-2">
							${_Icons.getSvgIcon(iconId)}
							${name}
						</button>
					`).join('')}

				</div>

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
		`,
		autoScriptInput: config => `
			<div class="flex ${config.wrapperClassString ?? ''}" title="Auto-script environment">
				<span class="inline-flex items-center bg-gray px-2 w-4 justify-center select-none border border-solid border-gray-input rounded-l">\${</span>
				<input type="text" class="block flex-grow rounded-none px-1.5 py-2 border-0 border-y border-solid border-gray-input ${config.inputClassString ?? ''}" placeholder="${config.placeholder ?? ''}" ${config.inputAttributeString ?? ''}>
				<span class="inline-flex items-center bg-gray px-2 w-4 justify-center select-none border border-solid border-gray-input rounded-r">}</span>
			</div>
		`,
		autoScriptTextArea: config => `
			<div id="${config.wrapperId ?? ''}" class="flex ${config.wrapperClassString ?? ''}" title="Auto-script environment">
				<span class="inline-flex items-center bg-gray px-2 w-4 justify-center select-none border border-solid border-gray-input rounded-l">\${</span>
				<textarea id="${config.textareaId ?? ''}" type="text" class="block flex-grow rounded-none px-1.5 py-2 border-0 border-y border-solid border-gray-input ${config.textareaClassString ?? ''}" placeholder="${config.placeholder ?? ''}" ${config.textareaAttributeString ?? ''}></textarea>
				<span class="inline-flex items-center bg-gray px-2 w-4 justify-center select-none border border-solid border-gray-input rounded-r">}</span>
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

class LifecycleMethods {

	static onlyAvailableInSchemaNodeContext(schemaNode)      { return ((schemaNode ?? null) !== null && schemaNode?.isServiceClass === false); }
	static onlyAvailableWithoutSchemaNodeContext(schemaNode) { return (schemaNode ?? null) === null; }

	// TODO: these functions must be able to detect schemaNodes that inherit from User/File (or any other possible way these lifecycle methods should be available there)
	static onlyAvailableWithUserNodeContext(schemaNode)      { return LifecycleMethods.onlyAvailableInSchemaNodeContext(schemaNode) && (schemaNode.name === 'User'); }
	static onlyAvailableWithFileNodeContext(schemaNode)      { return LifecycleMethods.onlyAvailableInSchemaNodeContext(schemaNode) && (schemaNode.name === 'File'); }

	static methods = [
		/** Global */
		{
			name: 'onStructrLogin',
			available: LifecycleMethods.onlyAvailableWithoutSchemaNodeContext,
			comment: `Is called when a (any) user logs in<br><br><strong>INFO</strong>: Only one such function can exist/is called. If multiple exist, the first being found is being called.
				<br><br>
				<div class="grid grid-cols-2">
					<div class="font-bold">Parameter name</div><div class="font-bold">Description</div>
					<div><code>user</code></div><div>The user logging in</div>
				</div>`,
			isPrefix: false
		},
		{
			name: 'onStructrLogout',
			available: LifecycleMethods.onlyAvailableWithoutSchemaNodeContext,
			comment: `Is called when a (any) user logs out<br><br><strong>INFO</strong>: Only one such function can exist/is called. If multiple exist, the first being found is being called.
				<br><br>
				<div class="grid grid-cols-2">
					<div class="font-bold">Parameter name</div><div class="font-bold">Description</div>
					<div><code>user</code></div><div>The user logging out</div>
				</div>`,
			isPrefix: false
		},
		{
			name: 'onAcmeChallenge',
			available: LifecycleMethods.onlyAvailableWithoutSchemaNodeContext,
			comment: `This method is called when an ACME challenge of type <strong>dns</strong> is triggered, typically by using the maintenance method letsencrypt. The primary use case of this method is creating a DNS TXT record via an external API call to a DNS provider.
				<br><br>
				<div class="grid grid-cols-2">
					<div class="font-bold">Parameter name</div><div class="font-bold">Description</div>
					<div><code>type</code></div><div> The type of the ACME authorisation challenge</div>
					<div><code>domain</code></div><div> The domain the ACME challenge is created for</div>
					<div><code>record</code></div><div> The name of the DNS record including prefix <code>_acme-challenge.</code> and suffix <code>.</code> </div>
					<div><code>digest</code> </div><div> The token string that is probed by the ACME server to validate the challenge </div>
				</div>`,
			isPrefix: false
		},
		/** AbstractNode */
		{
			name: 'onNodeCreation',
			available: LifecycleMethods.onlyAvailableInSchemaNodeContext,
			comment: `
				The <strong>onNodeCreation</strong> method runs immediately after creation of a node, before everything is committed.
				An error in this method (or if a constraint is not met) will still prevent the transaction from being committed successfully.
			`,
			isPrefix: true
		},
		{
			name: 'onCreate',
			available: LifecycleMethods.onlyAvailableInSchemaNodeContext,
			comment: `
				The <strong>onCreate</strong> method runs at the end of the transaction for all nodes created in the current transaction, before everything is committed.
				An error in this method (or if a constraint is not met) will still prevent the transaction from being committed successfully.
			`,
			isPrefix: true
		},
		{
			name: 'afterCreate',
			available: LifecycleMethods.onlyAvailableInSchemaNodeContext,
			comment: 'The difference between <strong>onCreate</strong> and <strong>afterCreate</strong> is that <strong>afterCreate</strong> is called after all checks have run and the transaction is committed successfully. The commit can not be rolled back anymore.<br><br>Example: There is a unique constraint and you want to send an email when an object is created.<br>Calling \'send_html_mail()\' in onCreate would send the email even if the transaction would be rolled back due to an error. The appropriate place for this would be afterCreate.',
			isPrefix: true
		},
		{
			name: 'onSave',
			available: LifecycleMethods.onlyAvailableInSchemaNodeContext,
			comment: 'The <strong>onSave</strong> method runs at the end of the transaction for all nodes saved/updated in the current transaction, before everything is committed. An error in this method (or if a constraint is not met) will still prevent the transaction from being committed successfully.',
			isPrefix: true
		},
		{
			name: 'afterSave',
			available: LifecycleMethods.onlyAvailableInSchemaNodeContext,
			comment: 'The difference between <strong>onSave</strong> and <strong>afterSave</strong> is that <strong>afterSave</strong> is called after all checks have run and the transaction is committed.<br><br>Example: There is a unique constraint and you want to send an email when an object is saved successfully.<br>Calling \'send_html_mail()\' in onSave would send the email even if the transaction would be rolled back due to an error. The appropriate place for this would be afterSave.',
			isPrefix: true
		},
		{
			name: 'onDelete',
			available: LifecycleMethods.onlyAvailableInSchemaNodeContext,
			comment: 'The <strong>onDelete</strong> method runs when a node is being deleted. The deletion can still be stopped by either an error in this method or by validation code.<br><br>The <strong>onDelete</strong> method differs from the other <strong>on****</strong> methods. It runs just when a node is being deleted, so that the node itself is still available and can be used for validation purposes. (Linked nodes can not be accessed. Use <code>retrieve("modifications")</code> for that.)',
			isPrefix: true
		},
		{
			name: 'afterDelete',
			available: LifecycleMethods.onlyAvailableInSchemaNodeContext,
			comment: 'The <strong>afterDelete</strong> method runs after a node has been deleted. The deletion can not be stopped at this point.<br><br>The <code>$.this</code> object is not available anymore but using the keyword $.data, the attributes (not the relationships) of the deleted node can be accessed.',
			isPrefix: true
		},
		/** FILE */
		{
			name: 'onUpload',
			available: LifecycleMethods.onlyAvailableWithFileNodeContext,
			comment: `Is called after a file has been uploaded. This method is being called without parameters.`,
			isPrefix: false
		},
		{
			name: 'onDownload',
			available: LifecycleMethods.onlyAvailableWithFileNodeContext,
			comment: `Is called after a file has been requested for download. This function can not prevent the download of a file. This method is being called without parameters.`,
			isPrefix: false
		},
		/** USER */
		{
			name: 'onOAuthLogin',
			available: LifecycleMethods.onlyAvailableWithUserNodeContext,
			comment: `Is called after a user successfully logs into the system via a configured OAuth provider. This function can not prevent the login of a user. The function is called with two parameters:
				<br><br>
				<div class="grid grid-cols-2">
					<div class="font-bold">Parameter name</div><div class="font-bold">Description</div>
					<div><code>provider</code></div><div>OAuth provider name</div>
					<div><code>userinfo</code></div><div>information pulled from the userinfo endpoint of the OAuth provider</div>
				</div>`,
			isPrefix: false
		},
	];

	static getAvailableLifecycleMethods(schemaNode) {

		return LifecycleMethods.methods.filter(m => m.available(schemaNode)).map((method) => {
			let { name, comment, isPrefix } = method;
			return { name, comment, isPrefix };
		});
	}

	static isLifecycleMethod (method) {

		let isJava = (method.codeType === 'java');

		return !isJava && LifecycleMethods.methods.some(m => {

			let nameMatches = m.isPrefix ? method.name.startsWith(m.name) : (method.name === m.name);

			return nameMatches && m.available(method.schemaNode);
		})
	}
}

class MessageBuilder {

	static types = Object.freeze({
		success: 'success',
		warning: 'warning',
		error: 'error',
		info: 'info'
	});
	static closeButtonClass = 'close-message-button';

	constructor (typeClass) {

		if (!MessageBuilder.types[typeClass]) {
			throw new Error('MessageBuilder: Unknown type, please fix');
		}

		this.typeClass = typeClass;

		// defaults
		this.params = {
			text: 'Default message',
			delayDuration: 3000,
			uniqueClass: undefined,
			uniqueCount: 1,
			updatesText: false,
			updatesButtons: false,
			appendsText: false,
			prependsText: false,
			appendSelector: '',
			prependSelector: '',
			replacesElement: false,
			replacesSelector: '',
			replaceInParentSelector: '',
			incrementsUniqueCount: false,
			specialInteractionButtons: []
		};
	}

	requiresConfirmation() {
		this.params.requiresConfirmation = true;
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

	appendButtons(buttonElement) {

		if (this.params.requiresConfirmation === true) {

			let closeButton = buttonElement.closest('.message').querySelector('.' + MessageBuilder.closeButtonClass);

			closeButton.classList.remove('hidden');
			closeButton.addEventListener('click', (e) => {
				this.dismiss();
			});

		} else {

			window.setTimeout(() => {
				this.dismiss();
			}, this.params.delayDuration);

			buttonElement.closest(`#${this.params.msgId}`).addEventListener('click', () => {
				this.dismiss();
			});
		}

		for (let btn of this.params.specialInteractionButtons) {

			let specialBtn = _Helpers.createSingleDOMElementFromHTML(`<button class="special hover:border-gray-666 mr-0">${btn.text}</button>`);
			buttonElement.appendChild(specialBtn);

			specialBtn.addEventListener('click', () => {

				btn.action?.();

				this.dismiss();
			});
		}
	};

	show() {

		let uniqueMessageAlreadyPresented = false;
		let allClasses                    = ['message', 'relative', 'break-word', 'flex', 'rounded-md', 'p-4', 'm-1', this.typeClass, this.params.uniqueClass];

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

				let messageTextElement = existingMessage.querySelector('.message-text');

				if (this.params.updatesText) {

					if (titleElement) {
						titleElement.innerHTML = this.params.title;
					}

					messageTextElement.innerHTML = this.params.text;

				} else if (this.params.prependsText) {

					if (titleElement) {
						titleElement.innerHTML = this.params.title;
					}

					let prependTarget = (this.params.appendSelector === '') ? messageTextElement : (messageTextElement.querySelector(this.params.prependSelector) ?? messageTextElement);

					prependTarget.insertAdjacentHTML('afterbegin', this.params.text);

				} else if (this.params.appendsText) {

					if (titleElement) {
						titleElement.innerHTML = this.params.title;
					}

					let appendTarget = (this.params.appendSelector === '') ? messageTextElement : (messageTextElement.querySelector(this.params.appendSelector) ?? messageTextElement);

					appendTarget.insertAdjacentHTML('beforeend', this.params.text);

				} else if (this.params.replacesElement) {

					if (titleElement) {
						titleElement.innerHTML = this.params.title;
					}

					let parentElement =  (this.params.replaceInParentSelector === '') ? messageTextElement : (messageTextElement.querySelector(this.params.replaceInParentSelector) ?? messageTextElement);
					let replaceElement = parentElement.querySelector(this.params.replacesSelector);

					if (replaceElement) {

						replaceElement.replaceWith(..._Helpers.createDOMElementsFromHTML(this.params.text));

					} else {

						parentElement.insertAdjacentHTML('beforeend', this.params.text);
					}
				}

				if (this.params.updatesButtons) {

					let buttonsContainer = existingMessage.querySelector('.message-buttons');
					_Helpers.fastRemoveAllChildren(buttonsContainer);

					this.appendButtons(buttonsContainer);
				}
			}
		}

		if (uniqueMessageAlreadyPresented === false) {

			this.params.msgId = `message_${Structr.msgCount++}`;

			let message = _Helpers.createSingleDOMElementFromHTML(`
				<div class="${allClasses.join(' ')}" id="${this.params.msgId}" data-unique-count="${this.params.uniqueCount}">
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 14, 14, _Icons.getSvgIconClassesForColoredIcon([MessageBuilder.closeButtonClass, 'hidden', 'icon-grey', 'cursor-pointer', 'absolute', 'top-3', 'right-3']))}
					<div class="message-icon flex-shrink-0 mr-2">
						${_Icons.getSvgIcon(_Icons.getSvgIconForMessageClass(this.typeClass))}
					</div>
					<div class="flex-grow">
						${(this.params.title ? `<div class="mb-2 -mt-1 font-bold text-lg title">${this.params.title}${this.getUniqueCountElement()}</div>` : this.getUniqueCountElement())}
						<div class="message-text mb-2 overflow-y-auto">
							${this.params.text}
						</div>
						<div class="message-buttons flex gap-2 justify-end"></div>
					</div>
				</div>
			`);

			this.appendButtons(message.querySelector('.message-buttons'));

			document.querySelector('#info-area #messages').appendChild(message);
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

	specialInteractionButton(buttonText, callback) {

		this.params.specialInteractionButtons.push({
			text: buttonText,
			action: callback
		});

		return this.requiresConfirmation();
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

	prependsText(selector = '') {
		this.params.prependsText    = true;
		this.params.prependSelector = selector;
		return this;
	};

	replacesElement(selector, parentSelector) {
		if (!selector) {
			throw new Error("Must provide selector to use replacesElement!");
		}

		this.params.replacesElement         = true;
		this.params.replacesSelector        = selector;
		this.params.replaceInParentSelector = parentSelector;
		return this;
	}

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
	setValueForSetting: (setting, input, value, container) => {

		let changed = (UISettings.getValueForSetting(setting) !== value);

		if (changed) {

			let isValid = UISettings.validateValueForSetting(setting, value);

			if (!isValid) {
				// attempt to fix once
				value = setting.fixValue?.(input, value);

				isValid = UISettings.validateValueForSetting(setting, value);
			}

			if (isValid) {

				LSWrapper.setItem(setting.storageKey, value);

				if (container) {

					_Helpers.blinkGreen(container);
				}

				setting.onUpdate?.();

			} else {

				_Helpers.blinkRed(container);
			}
		}
	},
	validateValueForSetting: (setting, value) => {

		let allow = setting.isValid?.(value) ?? true;

		return allow;
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

		let settingsContainerId = 'ui-settings-popup';

		_Helpers.fastRemoveElement(Structr.functionBar.querySelector('#' + settingsContainerId));

		let dropdown = _Helpers.createSingleDOMElementFromHTML(`
			<div id="${settingsContainerId}" class="dropdown-menu darker-shadow-dropdown dropdown-menu-large">
				<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green" data-preferred-position-x="left">
					${_Icons.getSvgIcon(_Icons.iconUIConfigSettings, 16, 16, ['mr-2'])}
				</button>
				<div class="dropdown-menu-container" style="display: none;"></div>
			</div>
		`);

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

				let settingDOM = _Helpers.createSingleDOMElementFromHTML(`
					<div class="flex items-center">
						<label class="flex items-center p-1"><input type="checkbox"> ${setting.text}</label>
					</div>
				`);

				let input = settingDOM.querySelector('input');
				input.checked = UISettings.getValueForSetting(setting);

				input.addEventListener('change', () => {
					UISettings.setValueForSetting(setting, input, input.checked, input.parentElement);
				});

				if (setting.infoText) {
					settingDOM.dataset['comment'] = setting.infoText;
				}

				container.appendChild(settingDOM);

				break;
			}

			case 'input': {

				let settingDOM = _Helpers.createSingleDOMElementFromHTML(`
					<div class="flex items-center">
						<label class="flex items-center p-1"><input type="${setting.inputType ?? 'text'}" class="mr-2 px-2 py-1 ${setting.inputCssClass ?? ''}" size="${setting.size ?? 5}">${setting.text}</label>
					</div>
				`);

				let input = settingDOM.querySelector('input');
				input.value = UISettings.getValueForSetting(setting);

				input.addEventListener('blur', () => {
					UISettings.setValueForSetting(setting, input, input.value, input.parentElement);
				});

				if (setting.infoText) {
					settingDOM.dataset['comment'] = setting.infoText;
				}

				container.appendChild(settingDOM);

				break;
			}

			case 'select': {

				let settingDOM = _Helpers.createSingleDOMElementFromHTML(`
					<div class="flex items-center">
						<label class="flex items-center p-1">
							${setting.text}
							<select class="ml-2 ${setting.inputCssClass ?? ''}">
								${Object.values(setting.possibleValues).map(option => `<option value="${option.value}">${option.text}</option>`)}
							</select>
						</label>
					</div>
				`);

				let select   = settingDOM.querySelector('select');
				select.value = UISettings.getValueForSetting(setting);

				select.addEventListener('change', () => {
					UISettings.setValueForSetting(setting, select, select.value, select.parentElement);
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
				hideAllPopupMessagesKey: {
					text: 'Hide notifications area',
					storageKey: 'hideNotificationMessages' + location.port,
					defaultValue: false,
					type: 'checkbox',
					infoText: 'Controls visibility of the notification area. Messages will still be appended and can be shown by changing this.',
					onUpdate: () => {
						Structr.determineNotificationAreaVisibility();
					}
				},
				showScriptingErrorPopupsKey: {
					text: 'Show notifications for scripting errors',
					storageKey: 'showScriptingErrorPopups' + location.port,
					defaultValue: true,
					type: 'checkbox'
				},
				showResourceAccessPermissionWarningPopupsKey: {
					text: 'Show notifications for resource access permission warnings',
					storageKey: 'showResourceAccessGrantWarningPopups' + location.port,
					defaultValue: true,
					type: 'checkbox'
				},
				showDeprecationWarningPopupsKey: {
					text: 'Show notifications for deprecation warnings',
					storageKey: 'showDeprecationWarningPopups' + location.port,
					defaultValue: true,
					type: 'checkbox'
				},
			}
		},
		dashboard: {
			title: 'Dashboard',
			settings: {
				useDeploymentWizard: {
					text: 'Use compact deployment UI',
					storageKey: 'dashboardUseDeploymentWizard_' + location.port,
					defaultValue: true,
					type: 'checkbox',
					onUpdate: () => {
						if (Structr.isModuleActive(_Dashboard)) {
							_Dashboard.tabs.deployment.wizard.updateWizardVisibility();
						}
					}
				}
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
				},
				sharedComponentSyncModeKey: {
					text: 'Sync strategy when updating a shared component',
					storageKey: 'sharedComponentSyncMode' + location.port,
					defaultValue: 'ASK',
					type: 'select',
					inputCssClass: 'w-40 truncate',
					possibleValues: {
						ASK:      { value: 'ASK',      text: 'Always ask' },
						ALL:      { value: 'ALL',      text: 'All' },
						BY_VALUE: { value: 'BY_VALUE', text: 'All with same previous value' },
						NONE:     { value: 'NONE',     text: 'Do not sync' }
					}
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
				showVisibilityFlagsInPermissionsTableKey: {
					text: 'Show visibility flags in Resource Access table',
					storageKey: 'showVisibilityFlagsInResourceAccessGrantsTable' + location.port,
					defaultValue: false,
					type: 'checkbox',
					onUpdate: () => {
						if (Structr.isModuleActive(_Security)) {
							_ResourceAccessPermissions.refreshResourceAccesses();
						}
					}
				},
				showBitmaskColumnInPermissionsTableKey: {
					text: 'Show bitmask column in Resource Access permissions table',
					infoText: 'This is an advanced editing feature to quickly set the permission configuration',
					storageKey: 'showBitmaskColumnInResourceAccessGrantsTable' + location.port,
					defaultValue: false,
					type: 'checkbox',
					onUpdate: () => {
						if (Structr.isModuleActive(_Security)) {
							_ResourceAccessPermissions.refreshResourceAccesses();
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
		},
		crud: {
			title: 'Data',
			settings: {
				hideLargeArrayElements: {
					text: 'Only show array attributes contents if less than this',
					infoText: 'The contents of array attributes with more elements will be hidden. The user can click to reveal the contents. This can be disabled by setting this to -1.',
					storageKey: 'hideLargeArrayElementsInData_' + location.port,
					defaultValue: 10,
					type: 'input',
					inputType: 'number',
					inputCssClass: 'w-12',
					onUpdate: () => {
						if (Structr.isModuleActive(_Crud)) {
						}
					},
					isValid: (value) => (parseInt(value) == value),
					fixValue: (input, value) => {

						let fixedValue = parseInt(value);

						if (isNaN(fixedValue)) {
							fixedValue = 0;
						}

						input.value = fixedValue;

						return fixedValue;
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