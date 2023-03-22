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
var ignoreKeyUp;
var dialog, dialogBox, dialogMsg, dialogBtn, dialogTitle, dialogMeta, dialogText, dialogHead, dialogCancelButton, dialogSaveButton, saveAndClose, loginBox, dialogCloseButton;

$(function() {

	document.body.innerHTML = Structr.templates.mainBody();
	_Icons.preloadSVGIcons();

	$.blockUI.defaults.overlayCSS.opacity        = .6;
	$.blockUI.defaults.overlayCSS.cursor         = 'default';
	$.blockUI.defaults.applyPlatformOpacityRules = false;
	$.blockUI.defaults.onBlock = () => {
		_Console.insertHeaderBlocker();
	};
	$.blockUI.defaults.onUnblock = () => {
		_Console.removeHeaderBlocker();
	};

	Structr.header        = document.getElementById('header');
	Structr.mainContainer = document.getElementById('main');
	Structr.functionBar   = document.getElementById('function-bar');
	loginBox              = $('#login');

	Structr.mainContainerOffscreen = document.createElement('div');
	Structr.functionBarOffscreen   = document.createElement('div');

	dialogBox           = $('#dialogBox');
	dialog              = $('.dialogText', dialogBox);
	dialogText          = $('.dialogText', dialogBox);
	dialogHead          = $('.dialogHeaderWrapper', dialogBox);
	dialogMsg           = $('.dialogMsg', dialogBox);
	dialogBtn           = $('.dialogBtn', dialogBox);
	dialogTitle         = $('.dialogTitle', dialogBox);
	dialogMeta          = $('.dialogMeta', dialogBox);
	dialogCancelButton  = $('.closeButton', dialogBox);
	dialogSaveButton    = $('.save', dialogBox);

	document.querySelector('#loginForm').addEventListener('submit', (e) => {
		e.stopPropagation();
		e.preventDefault();

		let username = document.querySelector('#usernameField').value;
		let password = document.querySelector('#passwordField').value;

		Structr.doLogin(username, password);
		return false;
	});

	document.querySelector('#two-factor-form').addEventListener('submit', (e) => {
		e.stopPropagation();
		e.preventDefault();

		let tfaToken = $('#twoFactorTokenField').val();
		let tfaCode  = $('#twoFactorCodeField').val();

		Structr.doTFALogin(tfaCode, tfaToken);
		return false;
	});

	document.querySelector('#logout_').addEventListener('click', (e) => {
		e.stopPropagation();
		Structr.doLogout();
	});

	let isHashReset = false;
	window.addEventListener('hashchange', (e) => {

		if (isHashReset === false) {

			let anchor = new URL(window.location.href).hash.substring(1);
			if (anchor === 'logout' || loginBox.is(':visible')) {
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

	$(window).keyup(function(e) {

		// unwrap jquery event
		let event   = e?.originalEvent ?? e;
		let keyCode = event.keyCode;
		let code    = event.code;

		if (code === 'Escape' || code === 'Esc' || keyCode === 27) {

			if (ignoreKeyUp) {
				ignoreKeyUp = false;
				return false;
			}

			if (dialogSaveButton.length && dialogSaveButton.is(':visible') && !dialogSaveButton.prop('disabled')) {
				ignoreKeyUp = true;
				let saveBeforeExit = confirm('Save changes?');
				if (saveBeforeExit) {
					dialogSaveButton.click();
					setTimeout(function() {
						if (dialogSaveButton && dialogSaveButton.length && dialogSaveButton.is(':visible') && !dialogSaveButton.prop('disabled')) {
							dialogSaveButton.remove();
						}
						if (saveAndClose && saveAndClose.length && saveAndClose.is(':visible') && !saveAndClose.prop('disabled')) {
							saveAndClose.remove();
						}
						if (dialogCancelButton && dialogCancelButton.length && dialogCancelButton.is(':visible') && !dialogCancelButton.prop('disabled')) {
							dialogCancelButton.click();
						}
						return false;
					}, 1000);
				}

				return false;

			} else if (dialogCancelButton.length && dialogCancelButton.is(':visible') && !dialogCancelButton.prop('disabled')) {

				dialogCancelButton.click();
				ignoreKeyUp = false;
				return false;
			}
		}
		return false;
	});

	$(window).on('keydown', function(e) {

		// unwrap jquery event
		let event   = e?.originalEvent ?? e;
		let keyCode = event.keyCode;
		let code    = event.code;

		// ctrl-s / cmd-s
		if ((code === 'KeyS' || keyCode === 83) && ((navigator.platform !== 'MacIntel' && event.ctrlKey) || (navigator.platform === 'MacIntel' && event.metaKey))) {
			event.preventDefault();
			if (dialogSaveButton && dialogSaveButton.length && dialogSaveButton.is(':visible') && !dialogSaveButton.prop('disabled')) {
				dialogSaveButton.click();
			}
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
			if (uuid) {
				if (uuid.length === 32) {
					Command.get(uuid, null, function (obj) {
						_Entities.showProperties(obj);
					});
				} else {
					new MessageBuilder().warning('That does not look like a UUID! length != 32').show();
				}
			}
		}

		// Ctrl-Alt-m
		if ((code === 'KeyM' || keyCode === 77) && event.altKey && event.ctrlKey) {
			event.preventDefault();
			let uuid = prompt('Enter the UUID for which you want to open the content/template edit dialog');
			if (uuid) {
				if (uuid.length === 32) {
					Command.get(uuid, null, (obj) => {
						_Elements.openEditContentDialog(obj);
					});
				} else {
					new MessageBuilder().warning('That does not look like a UUID! length != 32').show();
				}
			}
		}

		// Ctrl-Alt-g
		if ((code === 'KeyG' || keyCode === 71) && event.altKey && event.ctrlKey) {
			event.preventDefault();
			let uuid = prompt('Enter the UUID for which you want to open the access control dialog');
			if (uuid) {
				if (uuid.length === 32) {
					Command.get(uuid, null, (obj) => {
						_Entities.showAccessControlDialog(obj);
					});
				} else {
					new MessageBuilder().warning('That does not look like a UUID! length != 32').show();
				}
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
			Structr.dialog('Bulk Editing Helper (Ctrl-Alt-E)');
			new RefactoringHelper(dialog).show();
		}

		// ctrl-u / cmd-u: show generated source in schema or code area
		if ((code === 'KeyU' || keyCode === 85) && ((navigator.platform !== 'MacIntel' && event.ctrlKey) || (navigator.platform === 'MacIntel' && event.metaKey))) {

			let elements = document.querySelectorAll('.generated-source');

			if (elements.length > 0) {

				event.preventDefault();

				for (let el of elements) {

					if (el.classList.contains('tab')) {
						el.dispatchEvent(new Event('click'));
					}
					if (el.classList.contains('propTabContent')) {
						el.style.display = 'block';
					} else {
						el.style.display = null;
					}
				}
			}
		}
	});

	$(window).on('resize', () => {
		Structr.resize();
	});

	live('.dropdown-select', 'click', (e) => {
		e.stopPropagation();
		e.preventDefault();

		Structr.handleDropdownClick(e);

		return false;
	});

	live('#closeDialog', 'click', (e) => {
		document.querySelector('#dialogBox .closeButton').click();
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
	isInMemoryDatabase: undefined,
	modules: {},
	activeModules: {},
	availableMenuItems: [],
	moduleAvailabilityCallbacks: [],
	keyMenuConfig: 'structrMenuConfig_' + location.port,
	mainModule: undefined,
	subModule: undefined,
	lastMenuEntry: undefined,
	lastMenuEntryKey: 'structrLastMenuEntry_' + location.port,
	menuBlocked: undefined,
	dialogMaximizedKey: 'structrDialogMaximized_' + location.port,
	isMax: false,
	expandedIdsKey: 'structrTreeExpandedIds_' + location.port,
	dialogDataKey: 'structrDialogData_' + location.port,
	edition: '',
	classes: [],
	expanded: {},
	msgCount: 0,
	currentlyActiveSortable: undefined,
	loadingSpinnerTimeout: undefined,
	legacyRequestParameters: false,
	diffMatchPatch: undefined,
	defaultBlockUICss: {
		cursor: 'default',
		border: 'none',
		backgroundColor: 'transparent'
	},
	dialogTimeoutId: undefined,
	getDiffMatchPatch: () => {
		if (!Structr.diffMatchPatch) {
			Structr.diffMatchPatch = new diff_match_patch();
		}
		return Structr.diffMatchPatch;
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
	getRequestParameterName: (key) => {

		if (Structr.legacyRequestParameters === true) {
			// return key itself for legacy usage
			return key;
		} else {
			return '_' + key;
		}
	},

	moveUIOffscreen: () => {

		let movedOffscreen = false;

		// only move UI offscreen if there is UI to move offscreen
		if (Structr.functionBar.children.length > 0) {

			fastRemoveAllChildren(Structr.functionBarOffscreen);
			Structr.functionBarOffscreen.append(...Structr.functionBar.children);

			movedOffscreen = true;
		}

		if (Structr.mainContainer.children.length > 0) {

			fastRemoveAllChildren(Structr.mainContainerOffscreen);
			Structr.mainContainerOffscreen.append(...Structr.mainContainer.children);

			movedOffscreen = true;
		}

		return movedOffscreen;
	},
	moveOffscreenUIOnscreen: () => {

		let movedBack = false;

		if (Structr.functionBarOffscreen.children.length > 0) {

			fastRemoveAllChildren(Structr.functionBar);
			Structr.functionBar.append(...Structr.functionBarOffscreen.children);

			movedBack = true;
		}

		if (Structr.mainContainerOffscreen.children.length > 0) {

			fastRemoveAllChildren(Structr.mainContainer);
			Structr.mainContainer.append(...Structr.mainContainerOffscreen.children);

			movedBack = true;
		}

		return movedBack;
	},
	init: () => {
		$('#errorText').empty();
	},
	refreshUi: (isLogin = false) => {

		Structr.showLoadingSpinner();

		Structr.clearMain();
		Structr.loadInitialModule(isLogin, () => {
			StructrWS.startPing();

			if (!dialogText.text().length) {
				LSWrapper.removeItem(Structr.dialogDataKey);
			} else {
				let dialogData = JSON.parse(LSWrapper.getItem(Structr.dialogDataKey));
				if (dialogData) {
					Structr.restoreDialog(dialogData);
				}
			}

			Structr.hideLoadingSpinner();
			_Console.initConsole();
			document.querySelector('#header .logo').addEventListener('click', _Console.toggleConsole);
			_Favorites.initFavorites();
		});
	},
	updateUsername: (name) => {
		if (name !== StructrWS.user) {
			StructrWS.user = name;
			$('#logout_').html('Logout <span class="username">' + name + '</span>');
		}
	},
	getSessionId: () => {
		return Cookies.get('JSESSIONID');
	},
	login: (text) => {

		if (!loginBox.is(':visible')) {

			_Favorites.logoutAction();
			_Console.logoutAction();

			fastRemoveAllChildren(Structr.mainContainer);
			fastRemoveAllChildren(Structr.functionBar);
			fastRemoveAllChildren(Structr.mainContainerOffscreen);
			fastRemoveAllChildren(Structr.functionBarOffscreen);
			_Elements.removeContextMenu();

			$.blockUI({
				fadeIn: 25,
				fadeOut: 25,
				message: loginBox,
				forceInput: true,
				css: Structr.defaultBlockUICss
			});
		}

		$('#logout_').html('Login');
		if (text) {
			$('#errorText').html(text);
			$('#errorText-two-factor').html(text);
		}
	},
	clearLoginForm: () => {
		loginBox.find('#usernameField').val('');
		loginBox.find('#passwordField').val('');
		loginBox.find('#errorText').empty();

		loginBox.find('#two-factor').hide();
		loginBox.find('#two-factor #two-factor-qr-code').hide();
		loginBox.find('#two-factor img').attr('src', '');

		loginBox.find('#errorText-two-factor').empty();
		loginBox.find('#twoFactorTokenField').val('');
		loginBox.find('#twoFactorCodeField').val('');
	},
	toggle2FALoginBox: (data) => {

		$('#errorText').html('');
		$('#errorText-two-factor').html('');

		$('table.username-password', loginBox).hide();
		$('#two-factor', loginBox).show();

		if (data.qrdata) {
			$('#two-factor #two-factor-qr-code').show();
			$('#two-factor img', loginBox).attr('src', 'data:image/png;base64, ' + data.qrdata);
		}

		$('#twoFactorTokenField').val(data.token);
		$('#twoFactorCodeField').val('').focus();
	},
	doLogin: (username, password) => {
		Structr.renewSessionId(() => {
			Command.login({
				username: username,
				password: password
			});
		});
	},
	doTFALogin: (twoFactorCode, twoFacorToken) => {
		Structr.renewSessionId(() => {
			Command.login({
				twoFactorCode: twoFactorCode,
				twoFactorToken: twoFacorToken
			});
		});
	},
	doLogout: (text) => {

		_Favorites.logoutAction();
		_Console.logoutAction();
		LSWrapper.save();

		if (Command.logout(StructrWS.user)) {

			Cookies.remove('JSESSIONID');
			StructrWS.sessionId = '';
			Structr.renewSessionId();
			Structr.clearMain();
			Structr.clearVersionInfo();
			Structr.login(text);
			return true;
		}

		StructrWS.close();
		return false;
	},
	renewSessionId: (callback) => {

		fetch(Structr.getPrefixedRootUrl('/')).then(response => {

			StructrWS.sessionId = Structr.getSessionId();

			if (!StructrWS.sessionId && location.protocol === 'http:') {

				new MessageBuilder()
					.title("Unable to retrieve session id cookie")
					.warning("This is most likely due to a pre-existing secure HttpOnly cookie. Please navigate to the HTTPS version of this page (even if HTTPS is inactive) and delete the JSESSIONID cookie. Then return to this page and reload. This should solve the problem.")
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

			Structr.lastMenuEntry = ((!isLogin && Structr.mainModule && Structr.mainModule !== 'logout') ? Structr.mainModule : Structr.getActiveModuleName());
			if (!Structr.lastMenuEntry) {
				Structr.lastMenuEntry = Structr.getActiveModuleName() || 'dashboard';
			}
			Structr.updateVersionInfo(0, isLogin);
			Structr.doActivateModule(Structr.lastMenuEntry);

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
	clearMain: () => {
		let newDroppables = new Array();
		$.ui.ddmanager.droppables['default'].forEach(function(droppable, i) {
			if (!droppable.element.attr('id') || droppable.element.attr('id') !== 'graph-canvas') {
			} else {
				newDroppables.push(droppable);
			}
		});
		$.ui.ddmanager.droppables['default'] = newDroppables;

		fastRemoveAllChildren(Structr.mainContainer);
		fastRemoveAllChildren(Structr.functionBar);
		fastRemoveAllChildren(Structr.mainContainerOffscreen);
		fastRemoveAllChildren(Structr.functionBarOffscreen);

		_Elements.removeContextMenu();
	},
	restoreDialog: (dialogData) => {

		window.setTimeout(() => {

			Structr.blockUI(dialogData);
			Structr.resize();

		}, 1000);
	},
	dialog: (text, callbackOk, callbackCancel, customClasses) => {

		dialogHead.empty();
		dialogText.empty();
		dialogMsg.empty();
		dialogMeta.empty();
		dialogBtn.empty();

		dialogBox[0].classList = ["dialog"];
		if (customClasses) {
			for (let customClass of customClasses) {
				dialogBox.addClass(customClass);
			}
		}

		dialogBtn.html('<button class="closeButton hover:bg-gray-100 focus:border-gray-666 active:border-green">Close</button>');
		dialogCancelButton = $('.closeButton', dialogBox);

		$('.speechToText', dialogBox).remove();

		if (text) {
			dialogTitle.html(text);
		}

		dialogCancelButton.off('click').on('click', function(e) {
			e.stopPropagation();
			Structr.dialogCancelBaseAction();

			if (callbackCancel) {
				window.setTimeout(callbackCancel, 100);
			}
		});

		let dimensions = Structr.getDialogDimensions(24, 24);
		Structr.blockUI(dimensions);

		Structr.resize();

		dimensions.text = text;
		LSWrapper.setItem(Structr.dialogDataKey, JSON.stringify(dimensions));
	},
	dialogCancelBaseAction: () => {

		dialogText.empty();
		$.unblockUI({
			fadeOut: 25
		});

		dialogBtn.children(':not(.closeButton)').remove();

		Structr.focusSearchField();

		LSWrapper.removeItem(Structr.dialogDataKey);
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
	getDialogDimensions: (marginLeft, marginTop) => {

		let winW = $(window).width();
		let winH = $(window).height();

		let width = Math.min(900, winW - marginLeft);
		let height = Math.min(600, winH - marginTop);

		return {
			width: width,
			height: height,
			left: parseInt((winW - width) / 2),
			top: parseInt((winH - height) / 2)
		};

	},
	blockUI: (dimensions) => {

		$.blockUI({
			fadeIn: 25,
			fadeOut: 25,
			message: dialogBox,
			css: Object.assign({
				width: dimensions.width + 'px',
				height: dimensions.height + 'px',
				top: dimensions.top + 'px',
				left: dimensions.left + 'px'
			}, Structr.defaultBlockUICss),
			themedCSS: {
				width: dimensions.width + 'px',
				height: dimensions.height + 'px',
				top: dimensions.top + 'px',
				left: dimensions.left + 'px'
			},
			width: dimensions.width + 'px',
			height: dimensions.height + 'px',
			top: dimensions.top + 'px',
			left: dimensions.left + 'px'
		});

	},
	setSize: (w, h, dw, dh) => {

		let l = parseInt((w - dw) / 2);
		let t = parseInt((h - dh) / 2);

		let horizontalOffset = 148;

		// needs to be calculated like this because the elements in the dialogHead (tabs) are floated and thus the .height() method returns 0
		let headerHeight = (dialogText.position().top + dialogText.scrollParent().scrollTop()) - dialogHead.position().top;

		$('#dialogBox .dialogTextWrapper').css('width', 'calc(' + dw + 'px - 3rem)');
		$('#dialogBox .dialogTextWrapper').css('height', dh - horizontalOffset - headerHeight);

		$('.blockPage').css({
			width: dw + 'px',
			//height: dh + 'px',
			top: t + 'px',
			left: l + 'px'
		});

		$('.fit-to-height').css({
			height: h - 84 + 'px'
		});
	},
	resize: (callback) => {

		Structr.isMax = LSWrapper.getItem(Structr.dialogMaximizedKey);

		if (Structr.isMax) {
			Structr.maximize();
		} else {

			// Calculate dimensions of dialog
			if ($('.blockPage').length && !loginBox.is(':visible')) {
				Structr.setSize($(window).width(), $(window).height(), Math.min(900, $(window).width() - 24), Math.min(600, $(window).height() - 24));
			}

			$('#minimizeDialog').hide();
			$('#maximizeDialog').show().off('click').on('click', function() {
				Structr.maximize();
			});
		}

		if (callback) {
			callback();
		}
	},
	maximize: () => {

		// Calculate dimensions of dialog
		if ($('.blockPage').length && !loginBox.is(':visible')) {
			Structr.setSize($(window).width(), $(window).height(), $(window).width() - 24, $(window).height() - 24);
		}

		Structr.isMax = true;
		$('#maximizeDialog').hide();
		$('#minimizeDialog').show().off('click').on('click', function() {
			Structr.isMax = false;
			LSWrapper.removeItem(Structr.dialogMaximizedKey);
			Structr.resize();

			Structr.getActiveModule()?.dialogSizeChanged?.();
		});

		LSWrapper.setItem(Structr.dialogMaximizedKey, '1');

		Structr.getActiveModule()?.dialogSizeChanged?.();
	},
	error: (text, confirmationRequired) => {
		let message = new MessageBuilder().error(text);
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
					errorMsg += '.' + error.property;
				}
				if (error.value) {
					errorMsg += ' ' + error.value;
				}
				if (error.token) {
					errorMsg += ' ' + error.token;
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
						errorText += `<b>${key.capitalize()}</b>: ${response[key]}<br>`;
					} else {
						errorText += `${key.capitalize()}: ${response[key]}`;
					}
				}
			}
		}

		return errorText;
	},
	errorFromResponse: (response, url, additionalParameters) => {

		let errorText = Structr.getErrorMessageFromResponse(response, true, url);

		let message = new MessageBuilder().error(errorText);

		if (additionalParameters) {
			if (additionalParameters.requiresConfirmation) {
				message.requiresConfirmation();
			}
			if (additionalParameters.statusCode) {
				let title = Structr.getErrorTextForStatusCode(additionalParameters.statusCode);
				if (title) {
					message.title(title);
				}
			}
			if (additionalParameters.title) {
				message.title(additionalParameters.title);
			}
			if (additionalParameters.furtherText) {
				message.furtherText(additionalParameters.furtherText);
			}
			if (additionalParameters.overrideText) {
				message.text(additionalParameters.overrideText);
			}
		}

		message.show();
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
	loaderIcon: (element, css) => {
		let icon = $(_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 24, 24));
		element.append(icon);
		if (css) {
			icon.css(css);
		}
		return icon;
	},
	updateButtonWithSpinnerAndText: (btn, html) => {
		btn.attr('disabled', 'disabled').addClass('disabled').html(html + _Icons.getSvgIcon(_Icons.iconWaitingSpinner, 20, 20, 'ml-2'));
	},
	updateButtonWithSuccessIcon: (btn, html) => {
		btn.attr('disabled', null).removeClass('disabled').html(html + _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, 'tick icon-green ml-2'));
		window.setTimeout(() => {
			$('.tick', btn).fadeOut();
		}, 1000);
	},
	reconnectDialog: () => {

		let restoreDialogText = '';
		let dialogData = JSON.parse(LSWrapper.getItem(Structr.dialogDataKey));
		if (dialogData && dialogData.text) {
			restoreDialogText = '<div>The dialog</div><b>"' + dialogData.text + '"</b><div>will be restored after reconnect.</div>';
		}

		let tmpErrorHTML = `
			<div id="tempErrorBox" class="dialog block">
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
				<div class="errorMsg"></div>
				<div class="dialogBtn"></div>
			</div>
		`;

		$.blockUI({
			message: tmpErrorHTML,
			css: Structr.defaultBlockUICss
		});
	},
	blockMenu: () => {
		Structr.menuBlocked = true;
		$('#menu > ul > li > a').attr('disabled', 'disabled').addClass('disabled');
	},
	unblockMenu: (ms) => {
		// Wait ms before releasing the main menu
		window.setTimeout(function() {
			Structr.menuBlocked = false;
			$('#menu > ul > li > a').removeAttr('disabled', 'disabled').removeClass('disabled');
		}, ms || 0);
	},
	requestActivateModule: (event, name) => {
		if (Structr.menuBlocked) {
			return false;
		}

		event.stopPropagation();
		if (Structr.getActiveModuleName() !== name || Structr.mainContainer.children.length === 0) {
			return Structr.doActivateModule(name);
		}

		return true;
	},
	doActivateModule: (name) => {
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
				Structr.activateMenuEntry(name);
				Structr.modules[name].onload();
				Structr.adaptUiToAvailableFeatures();
			}

			return moduleAllowsNavigation;
		} else {
			Structr.unblockMenu();
		}

		return true;
	},
	activateMenuEntry: (name) => {
		Structr.blockMenu();
		let menuEntry = $('#' + name + '_');
		let li = menuEntry.parent();
		if (li.hasClass('active')) {
			return false;
		}
		Structr.lastMenuEntry = name;
		$('.menu li').removeClass('active');
		li.addClass('active');
		$('#title').text('Structr ' + menuEntry.text());
		window.location.hash = Structr.lastMenuEntry;
		if (Structr.lastMenuEntry && Structr.lastMenuEntry !== 'logout') {
			LSWrapper.setItem(Structr.lastMenuEntryKey, Structr.lastMenuEntry);
		}
	},
	registerModule: (module) => {
		let name = module._moduleName;
		if (!name || name.trim().length === 0) {
			new MessageBuilder().error("Cannot register module without a name - ignoring attempt. To fix this error, please add the '_moduleName' variable to the module.").show();
		} else if (!Structr.modules[name]) {
			Structr.modules[name] = module;
		} else {
			new MessageBuilder().error(`Cannot register module '${name}' a second time - ignoring attempt.`).show();
		}
	},
	getActiveModuleName: () => {
		return Structr.lastMenuEntry || LSWrapper.getItem(Structr.lastMenuEntryKey);
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
			childNodes = childNodes.not('.' + excludeId + '_');
		}

		return childNodes.length;
	},
	findParent: (parentId, componentId, pageId, defaultElement) => {

		let parent = Structr.node(parentId, null, componentId, pageId);

		if (!parent) {
			parent = defaultElement;
		}

		return parent;
	},
	parent: (id) => {
		return Structr.node(id) && Structr.node(id).parent().closest('.node');
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
	entity: function(id, parentId) {
		let entityElement = Structr.node(id, parentId);
		let entity        = Structr.entityFromElement(entityElement);
		return entity;
	},
	getClass: function(el) {
		let c;
		for(let cls of Structr.classes) {
			if (el && $(el).hasClass(cls)) {
				c = cls;
			}
		}

		return c;
	},
	entityFromElement: function(element) {

		var entity = {};
		entity.id = Structr.getId($(element));
		var cls = Structr.getClass(element);
		if (cls) {
			entity.type = cls.capitalize();
		}

		var nameEl = $(element).children('.name_');

		if (nameEl && nameEl.length) {
			entity.name = $(nameEl[0]).text();
		}

		var tagEl = $(element).children('.tag_');

		if (tagEl && tagEl.length) {
			entity.tag = $(tagEl[0]).text();
		}

		return entity;
	},
	makePagesMenuDroppable: function() {

		try {
			$('#pages_').droppable('destroy');
		} catch (err) {
			// console.log(err);
		}

		$('#pages_').droppable({
			accept: '.element, .content, .component, .file, .image, .widget',
			greedy: true,
			hoverClass: 'nodeHover',
			tolerance: 'pointer',
			over: function(e, ui) {

				e.stopPropagation();
				$('a#pages_').droppable('disable');

				Structr.activateMenuEntry('pages');
				window.location.href = '/structr/#pages';

				if (_Files.filesMain && _Files.filesMain.length) {
					_Files.filesMain.hide();
				}
				if (_Widgets.widgets && _Widgets.widgets.length) {
					_Widgets.widgets.hide();
				}

				Structr.modules['pages'].onload();
				Structr.adaptUiToAvailableFeatures();
				_Pages.resize();
				$('a#pages_').removeClass('nodeHover').droppable('enable');
			}

		});
	},
	openSlideOut: (triggerEl, slideoutElement, callback) => {

		let storedRightSlideoutWidth = LSWrapper.getItem(_Pages.pagesResizerRigthKey);
		let rsw                      = storedRightSlideoutWidth ? parseInt(storedRightSlideoutWidth) : (slideoutElement.width() + 12);

		let t = $(triggerEl);
		t.addClass('active');
		slideoutElement.width(rsw);

		slideoutElement.animate({right: 0}, 100, function() {
			if (typeof callback === 'function') {
				callback({isOpenAction: true});
			}
		});

		slideoutElement.addClass('open');
	},
	openLeftSlideOut: (triggerEl, slideoutElement, callback) => {

		let storedLeftSlideoutWidth = LSWrapper.getItem(_Pages.pagesResizerLeftKey);
		let psw                     = storedLeftSlideoutWidth ? parseInt(storedLeftSlideoutWidth) : (slideoutElement.width());

		let t = $(triggerEl);
		t.addClass('active');

		slideoutElement.width(psw);

		slideoutElement.animate({ left: 0 }, 100, () => {
			if (typeof callback === 'function') {

				callback({isOpenAction: true});
			}
		});

		slideoutElement.addClass('open');
	},
	closeSlideOuts: (slideouts, callback) => {

		let wasOpen = false;

		for (let slideout of slideouts) {

			slideout.removeClass('open');

			let left          = slideout.position().left;
			let slideoutWidth = slideout[0].getBoundingClientRect().width;

			if ((window.innerWidth - left) > 1) {

				wasOpen = true;
				slideout.animate({ right: -slideoutWidth }, 100, function() {
					if (typeof callback === 'function') {
						callback(wasOpen);
					}
				}).zIndex(2);
				$('.slideout-activator.right.active').removeClass('active');
			}
		}

		LSWrapper.removeItem(_Pages.activeTabRightKey);
	},
	closeLeftSlideOuts: (slideouts, callback) => {

		let wasOpen = false;
		let oldSlideoutWidth;

		for (let slideout of slideouts) {

			slideout.removeClass('open');

			let left          = slideout.position().left;
			let slideoutWidth = slideout[0].getBoundingClientRect().width;

			if (left > -1) {

				wasOpen = true;
				oldSlideoutWidth = slideoutWidth;
				slideout.animate({ left: -slideoutWidth }, 100, function() {
					if (typeof callback === 'function') {
						callback(wasOpen, -oldSlideoutWidth, 0);
					}
				}).zIndex(2);

				$('.slideout-activator.left.active').removeClass('active');
			}
		}
	},
	updateVersionInfo: (retryCount = 0, isLogin = false) => {

		fetch(Structr.rootUrl + '_env').then((response) => {

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
						new MessageBuilder().warning(Structr.inMemoryWarningText).requiresConfirmation().show();
					}
				}
			}

			$('#header .structr-instance-name').text(envInfo.instanceName);
			$('#header .structr-instance-stage').text(envInfo.instanceStage);

			Structr.legacyRequestParameters = envInfo.legacyRequestParameters;

			if (true === envInfo.maintenanceModeActive) {
				$('#header .structr-instance-maintenance').text("MAINTENANCE");
			}

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

			Structr.updateMainMenu(userConfigMenu);

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
				setTimeout(() => {
					Structr.updateVersionInfo(++retryCount, isLogin);
				}, 250);
			} else {
				console.log(e);
			}
		});
	},
	updateMainMenu: (menuConfig) => {

		LSWrapper.setItem(Structr.keyMenuConfig, menuConfig);

		let menu      = $('#menu');
		let submenu   = $('#submenu');
		let hamburger = $('#menu li.submenu-trigger');

		// first move all elements from main menu to submenu
		$('li[data-name]', menu).appendTo(submenu);

		// then filter the items
		$('li[data-name]', submenu).each((i, e) => {
			let name = e.dataset.name;
			if (!Structr.availableMenuItems.includes(name)) {
				e.classList.add('hidden');
			}
		})

		for (let entry of menuConfig.main) {
			$('li[data-name="' + entry + '"]', menu).insertBefore(hamburger);
		}

		// what does this even do?
		for (let entry of menuConfig.sub) {
			$('#submenu li').last().after($('li[data-name="' + entry + '"]', menu));
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

		Structr.appendInfoTextToElement(config);
	},
	getDatabaseDriverNameForDatabaseServiceName: (databaseServiceName) => {
		switch (databaseServiceName) {
			case 'BoltDatabaseService':
				return 'Bolt Database Driver';

			case 'MemoryDatabaseService':
				return 'In-Memory Database Driver';
				break;
		}

		return 'Unknown database driver!';
	},
	clearVersionInfo: () => {
		$('.structr-version').html('');
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
	getUserId: (element) => {
		return element.data('userId');
	},
	getGroupId: (element) => {
		return element.data('groupId');
	},
	getActiveElementId: (element) => {
		return Structr.getIdFromPrefixIdString($(element).prop('id'), 'active_') || undefined;
	},
	adaptUiToAvailableFeatures: () => {
		Structr.adaptUiToAvailableModules();
		Structr.adaptUiToEdition();
	},
	adaptUiToAvailableModules: () => {
		$('.module-dependend').each(function(idx, element) {
			var el = $(element);
			var module = el.data('structr-module');
			if (Structr.isModulePresent(module)) {
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
	performModuleDependendAction: (action) => {
		if (Structr.isModuleInformationAvailable()) {
			action();
		} else {
			Structr.registerActionAfterModuleInformationIsAvailable(action);
		}
	},
	registerActionAfterModuleInformationIsAvailable: (cb) => {
		Structr.moduleAvailabilityCallbacks.push(cb);
	},
	adaptUiToEdition: () => {
		$('.edition-dependend').each(function(idx, element) {
			var el = $(element);

			if (Structr.isAvailableInEdition(el.data('structr-edition'))) {
				if (!el.is(':visible')) el.show();
			} else {
				el.hide();
			}
		});
	},
	isAvailableInEdition: (requiredEdition) => {
		switch(Structr.edition) {
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
	isButtonDisabled: (button) => {
		return $(button).data('disabled');
	},
	disableButton: (btn) => {
		$(btn).addClass('disabled').attr('disabled', 'disabled');
	},
	enableButton: (btn) => {
		$(btn).removeClass('disabled').removeAttr('disabled');
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

			if (storedValue === undefined) {

				return defaultValue;

			} else {

				return storedValue;
			}
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
	showAndHideInfoBoxMessage: (msg, msgClass, delayTime, fadeTime) => {
		let newDiv = $('<div class="infoBox ' + msgClass + '"></div>');
		newDiv.text(msg);
		dialogMsg.html(newDiv);
		$('.infoBox', dialogMsg).delay(delayTime).fadeOut(fadeTime);
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

		let helpElement = $('<span class="context-help-text">' + text + '</span>');
		createdElements.push(helpElement);

		toggleElement
			.on("mousemove", function(e) {
				helpElement.show();
				helpElement.css({
					left: Math.min(e.clientX + 20 + offsetX, window.innerWidth - helpElement.width() - 50),
					top: Math.min(e.clientY + 10 + offsetY, window.innerHeight - helpElement.height() - 10)
				});
			}).on("mouseout", function(e) {
				helpElement.hide();
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
	refreshPositionsForCurrentlyActiveSortable: () => {

		if (Structr.currentlyActiveSortable) {

			Structr.currentlyActiveSortable.sortable({ refreshPositions: true });

			window.setTimeout(function() {
				Structr.currentlyActiveSortable.sortable({ refreshPositions: false });
			}, 500);
		}
	},
	handleGenericMessage: (data) => {

		let showScheduledJobsNotifications = Importer.isShowNotifications();
		let showScriptingErrorPopups       = UISettings.getValueForSetting(UISettings.global.settings.showScriptingErrorPopupsKey);
		let showResourceAccessGrantPopups  = UISettings.getValueForSetting(UISettings.global.settings.showResourceAccessGrantWarningPopupsKey);
		let showDeprecationWarningPopups   = UISettings.getValueForSetting(UISettings.global.settings.showDeprecationWarningPopupsKey);

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

					new MessageBuilder().title(titles[data.subtype]).uniqueClass('csv-import-status').updatesText().requiresConfirmation().allowConfirmAll().className((data.subtype === 'END') ? 'success' : 'info').text(texts[data.subtype]).show();
				}
				break;

			case "CSV_IMPORT_WARNING":

				if (StructrWS.me.username === data.username) {
					new MessageBuilder().title(data.title).warning(data.text).requiresConfirmation().allowConfirmAll().show();
				}
				break;

			case "CSV_IMPORT_ERROR":

				if (StructrWS.me.username === data.username) {
					new MessageBuilder().title(data.title).error(data.text).requiresConfirmation().allowConfirmAll().show();
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

					let msg = new MessageBuilder().title(`${data.jobtype} ${fileImportTitles[data.subtype]}`).className((data.subtype === 'END') ? 'success' : 'info')
						.text(fileImportTexts[data.subtype]).uniqueClass(`${data.jobtype}-import-status-${data.filepath}`);

					if (data.subtype !== 'QUEUED') {
						msg.updatesText().requiresConfirmation().allowConfirmAll();
					}

					msg.show();
				}

				if (Structr.isModuleActive(Importer)) {
					Importer.updateJobTable();
				}
				break;

			case "FILE_IMPORT_EXCEPTION":

				if (showScheduledJobsNotifications && StructrWS.me.username === data.username) {

					let text = data.message;
					if (data.message !== data.stringvalue) {
						text += '<br>' + data.stringvalue;
					}

					new MessageBuilder().title(`Exception while importing ${data.jobtype}`).error(`File: ${data.filepath}<br>${text}`).requiresConfirmation().allowConfirmAll().show();
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

					let msg = new MessageBuilder().title(scriptJobTitles[data.subtype]).className((data.subtype === 'END') ? 'success' : 'info').text(`<div>${scriptJobTexts[data.subtype]}</div>`).uniqueClass(`${data.jobtype}-${data.subtype}`).appendsText();

					if (data.subtype !== 'QUEUED') {
						msg.requiresConfirmation().allowConfirmAll();
					}

					msg.show();
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

					new MessageBuilder().title(`${type} Progress`).uniqueClass(messageCssClass).info(text).requiresConfirmation().updatesText().show();

				} else if (data.subtype === 'PROGRESS') {

					new MessageBuilder().title(`${type} Progress`).uniqueClass(messageCssClass).info(`<li>${data.message}</li>`).requiresConfirmation().appendsText('.message-steps').show();

				} else if (data.subtype === 'END') {

					let text = `<br>${type} finished: ${new Date(data.end)}<br>
						Total duration: ${data.duration}<br><br>
						Reload the page to see the new data.`;

					new MessageBuilder().title(`${type} finished`).uniqueClass(messageCssClass).success(text).specialInteractionButton('Reload Page', () => { location.reload(); }, 'Ignore').appendsText().updatesButtons().show();

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

					new MessageBuilder().title(type + ' Progress').uniqueClass(messageCssClass).info(text).requiresConfirmation().updatesText().show();

				} else if (data.subtype === 'PROGRESS') {

					new MessageBuilder().title(type + ' Progress').uniqueClass(messageCssClass).info(`<li>${data.message}</li>`).requiresConfirmation().appendsText('.message-steps').show();

				} else if (data.subtype === 'END') {

					let text = `<br>${type} finished: ${new Date(data.end)}<br>Total duration: ${data.duration}`;

					new MessageBuilder().title(type + ' finished').uniqueClass(messageCssClass).success(text).appendsText().requiresConfirmation().show();

				}
				break;
			}

			case "SCHEMA_ANALYZE_STATUS":

				if (data.subtype === 'BEGIN') {

					let text = `Schema Analysis started: ${new Date(data.start)}<br>
						Please wait until the import process is finished. This message will be updated during the process.<br>
						<ol class="message-steps"></ol>
					`;

					new MessageBuilder().title('Schema Analysis progress').uniqueClass('schema-analysis').info(text).requiresConfirmation().updatesText().show();

				} else if (data.subtype === 'PROGRESS') {

					new MessageBuilder().title('Schema Analysis progress').uniqueClass('schema-analysis').info(`<li>${data.message}</li>`).requiresConfirmation().appendsText('.message-steps').show();

				} else if (data.subtype === 'END') {

					let text = `<br>Schema Analysis finished: ${new Date(data.end)}<br>Total duration: ${data.duration}`;

					new MessageBuilder().title("Schema Analysis finished").uniqueClass('schema-analysis').success(text).appendsText().requiresConfirmation().show();

				}
				break;

			case "CERTIFICATE_RETRIEVAL_STATUS":

				if (data.subtype === 'BEGIN') {

					let text = `Process to retrieve a Let's Encrypt certificate via ACME started: ${new Date(data.start)}<br>
						This will take a couple of seconds. This message will be updated during the process.<br>
						<ol class='message-steps'></ol>
					`;

					new MessageBuilder().title("Certificate retrieval progress").uniqueClass('cert-retrieval').info(text).requiresConfirmation().updatesText().show();

				} else if (data.subtype === 'PROGRESS') {

					new MessageBuilder().title("Certificate retrieval progress").uniqueClass('cert-retrieval').info(`<li>${data.message}</li>`).requiresConfirmation().appendsText('.message-steps').show();

				} else if (data.subtype === 'END') {

					let text = `<br>Certificate retrieval process finished: ${new Date(data.end)}<br>Total duration: ${data.duration}`;

					new MessageBuilder().title("Certificate retrieval finished").uniqueClass('cert-retrieval').success(text).appendsText().requiresConfirmation().show();

				} else if (data.subtype === 'WARNING') {

					new MessageBuilder().title("Certificate retrieval progress").warning(data.message).uniqueClass('cert-retrieval').requiresConfirmation().allowConfirmAll().show();
				}

				break;

			case "MAINTENANCE":

				let enabled = data.enabled ? 'enabled' : 'disabled';

				new MessageBuilder().title(`Maintenance Mode ${enabled}`).warning(`Maintenance Mode has been ${enabled}.<br><br> Redirecting...`).allowConfirmAll().show();

				window.setTimeout(function() {
					location.href = data.baseUrl + location.pathname + location.search;
				}, 1500);
				break;

			case "WARNING":
				new MessageBuilder().title(data.title).warning(data.message).requiresConfirmation().allowConfirmAll().show();
				break;

			case "SCRIPT_JOB_EXCEPTION":
				new MessageBuilder().title('Exception in Scheduled Job').warning(data.message).requiresConfirmation().allowConfirmAll().show();
				break;

			case "RESOURCE_ACCESS":

				if (showResourceAccessGrantPopups) {

					let builder = new MessageBuilder().title(`REST Access to '${data.uri}' denied`).warning(data.message).requiresConfirmation().allowConfirmAll();

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
								        title = 'type "' + data.staticType + '"';
								        property ='StaticMethod';
								    } else if (obj.schemaNode) {
										title = 'type "' + obj.schemaNode.name + '"';
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
											title = 'page "' + obj.ownerDocument.name  + '"';
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

							let builder = new MessageBuilder().uniqueClass(`n${data.nodeId}${data.nodeType}${data.row}${data.column}`).incrementsUniqueCount(true).title(`Scripting error in ${title}`).warning(location).requiresConfirmation();

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

									// open and select element in tree
									let structrId = obj.id;
									_Entities.deselectAllElements();

									if (!Structr.node(structrId)) {
										_Pages.expandTreeNode(structrId);
									} else {
										let treeEl = Structr.node(structrId);
										if (treeEl) {
											_Entities.highlightElement(treeEl);
										}
									}

									LSWrapper.setItem(_Entities.selectedObjectIdKey, structrId);

								}, 'Dismiss');
							}

							// show message
							builder.allowConfirmAll().show();
						});

					} else {
						new MessageBuilder().title('Server-side Scripting Error').warning(data.message).requiresConfirmation().allowConfirmAll().show();
					}
				}
				break;

			case "DEPRECATION": {

				if (showDeprecationWarningPopups) {

					let uniqueClass = 'deprecation-warning-' + data.nodeId;

					let builder = new MessageBuilder().uniqueClass(uniqueClass).incrementsUniqueCount(true).title(data.title).warning(data.message).requiresConfirmation();

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
												title = 'Page "' + obj.ownerDocument.name + '"';
											}
										}
										break;
								}

								if (title != '') {
									builder.warning(data.message + '<br><br>Source: ' + title);
								}

								builder.specialInteractionButton('Go to element in page tree', function (btn) {

									// open and select element in tree
									let structrId = obj.id;
									_Entities.deselectAllElements();

									if (!Structr.node(structrId)) {
										_Pages.expandTreeNode(structrId);
									} else {
										let treeEl = Structr.node(structrId);
										if (treeEl) {
											_Entities.highlightElement(treeEl);
										}
									}

									LSWrapper.setItem(_Entities.selectedObjectIdKey, structrId);

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

				new MessageBuilder().title("GENERIC_MESSAGE").warning(text).requiresConfirmation().show();

			}
		}
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
				Structr.appendInfoTextToElement(infoConfig);
			}
		}

	},
	blockUiGeneric: (html, timeout) => {
		Structr.loadingSpinnerTimeout = window.setTimeout(() => {

			$.blockUI({
				fadeIn: 0,
				fadeOut: 0,
				message: html,
				forceInput: true,
				css: Structr.defaultBlockUICss
			});
		}, timeout || 0);
	},
	unblockUiGeneric: () => {
		window.clearTimeout(Structr.loadingSpinnerTimeout);
		Structr.loadingSpinnerTimeout = undefined;

		$.unblockUI({
			fadeOut: 0
		});
	},
	showLoadingSpinner: () => {
		Structr.blockUiGeneric('<div id="structr-loading-spinner">' + _Icons.getSvgIcon(_Icons.iconWaitingSpinner, 36, 36) + '</div>');
	},
	hideLoadingSpinner: () => {
		Structr.unblockUiGeneric();
	},
	showLoadingMessage: (title, text, timeout) => {

		let messageTitle = title || 'Executing Task';
		let messageText  = text || 'Please wait until the operation has finished...';

		$('#tempInfoBox .infoMsg').html(`<div class="flex items-center justify-center">${_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 24, 24, 'mr-2')}<b>${messageTitle}</b></div><br>${messageText}`);

		$('#tempInfoBox .closeButton').hide();
		Structr.blockUiGeneric($('#tempInfoBox'), timeout || 500);
	},
	hideLoadingMessage: () => {
		Structr.unblockUiGeneric();
	},

	nonBlockUIBlockerId: 'non-block-ui-blocker',
	nonBlockUIBlockerContentId: 'non-block-ui-blocker-content',
	showNonBlockUILoadingMessage: (title, text) => {

		let messageTitle = title || 'Executing Task';
		let messageText  = text || 'Please wait until the operation has finished...';

		let pageBlockerDiv = $('<div id="' + Structr.nonBlockUIBlockerId +'"></div>');
		let messageDiv     = $('<div id="' + Structr.nonBlockUIBlockerContentId +'"></div>');
		messageDiv.html(`<div class="flex items-center justify-center">${_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 24, 24, 'mr-2')}<b>${messageTitle}</b></div><br>${messageText}`);

		$('body').append(pageBlockerDiv);
		$('body').append(messageDiv);
	},
	hideNonBlockUILoadingMessage: () => {
		$('#' + Structr.nonBlockUIBlockerId).remove();
		$('#' + Structr.nonBlockUIBlockerContentId).remove();
	},

	confirmation: (text, yesCallback, noCallback) => {
		if (text) {
			$('#confirmation .confirmationText').html(text);
		}
		let yesButton = $('#confirmation .yesButton');
		let noButton  = $('#confirmation .noButton');

		if (yesCallback) {
			yesButton.on('click', function(e) {
				e.stopPropagation();
				yesCallback();
				yesButton.off('click');
				noButton.off('click');
			});
		}

		noButton.on('click', function(e) {
			e.stopPropagation();
			$.unblockUI({
				fadeOut: 25
			});
			if (noCallback) {
				noCallback();
			}
			yesButton.off('click');
			noButton.off('click');
		});

		$.blockUI({
			fadeIn: 25,
			fadeOut: 25,
			message: $('#confirmation'),
			css: Structr.defaultBlockUICss
		});
	},
	confirmationPromiseNonBlockUI: (text, defaultOption = true) => {

		return new Promise((resolve, reject) => {

			let pageBlockerDiv = Structr.createSingleDOMElementFromHTML(`<div id="${Structr.nonBlockUIBlockerId}"></div>`);
			let messageDiv     = Structr.createSingleDOMElementFromHTML(`<div id="${Structr.nonBlockUIBlockerContentId}"></div>`);

			let el = document.getElementById('confirmation').cloneNode(true);
			el.id = 'confirmation-new';
			el.classList.remove('dialog');

			el.querySelector('.confirmationText').innerHTML = text;

			messageDiv.appendChild(el);

			let yesButton = el.querySelector('.yesButton');
			let noButton  = el.querySelector('.noButton');

			let answerFunction = (e, response) => {
				e.stopPropagation();

				pageBlockerDiv.remove();
				messageDiv.remove();

				resolve(response);
			};

			yesButton.addEventListener('click', (e) => {
				answerFunction(e, true);
			});

			noButton.addEventListener('click', (e) => {
				answerFunction(e, false);
			});

			messageDiv.addEventListener('keyup', (e) => {
				if (e.key === 'Escape' || e.code === 'Escape' || e.keyCode === 27) {
					answerFunction(e, false);
				}
			});

			let body = document.querySelector('body');
			body.appendChild(pageBlockerDiv);
			body.appendChild(messageDiv);

			if (defaultOption === true) {
				yesButton.focus();
			} else {
				noButton.focus();
			}
		});
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
	ensureShadowPageExists: () => {

		return new Promise((resolve, reject) => {

			if (_Pages.shadowPage) {

				resolve(_Pages.shadowPage);

			} else {

				// wrap getter for shadowdocument in listComponents so we're sure that shadow document has been created
				Command.listComponents(1, 1, 'name', 'asc', (result) => {

					Command.getByType('ShadowDocument', 1, 1, null, null, null, true, (entities) => {

						_Pages.shadowPage = entities[0];

						resolve(_Pages.shadowPage);
					});
				});
			}
		});
	},
	createSingleDOMElementFromHTML: (html) => {
		let elements = Structr.createDOMElementsFromHTML(html);
		return elements[0];
	},
	createDOMElementsFromHTML: (html) => {
		// use template element so we can create arbitrary HTML which is not parsed but not rendered (otherwise tr/td and some other elements would not work)
		let dummy = document.createElement('template');
		dummy.innerHTML = html;

		return dummy.content.children;
	},
	showAvailableIcons: () => {

		Structr.dialog('Icons');

		dialogText.html(`<div>
			<h3>SVG Icons</h3>
			<table>
				${[...document.querySelectorAll('body > svg > symbol')].map(el => el.id).sort().map(id => 
					`<tr>
						<td>${id}</td>
						<td>${_Icons.getSvgIcon(id, 24, 24)}</td>
					</tr>`).join('')}
			</table>
		</div>`);
	},
	isImage: (contentType) => {
		return (contentType && contentType.indexOf('image') > -1);
	},
	isVideo: (contentType) => {
		return (contentType && contentType.indexOf('video') > -1);
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

					container.dataset['visible'] = 'true';

					container.style.display  = 'block';

					let btn     = e.target.closest('.dropdown-select');
					let btnRect = btn.getBoundingClientRect();
					let containerRect = container.getBoundingClientRect();

					if (btn.dataset['preferredPositionY'] === 'top') {

						// position dropdown over activator button
						container.style.bottom    = `calc(${window.innerHeight - btnRect.top}px + 0.25rem)`;
					}

					if (btn.dataset['preferredPositionX'] === 'left') {

						// position dropdown left of button
						container.style.right    = `calc(${window.innerWidth - btnRect.right}px + 2.5rem)`;
					}

					if (btn.dataset['wantsFixed'] === 'true') {
						/*
							this is important for the editor tools in a popup which need to break free from the popup dialog
						*/
						container.style.position = 'fixed';  // no top, no bottom, just fixed so that it is positioned automatically but taken out of the document flow
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

		container.style.display = 'none';
		container.style.position = null;
		container.style.bottom   = null;
		container.style.top      = null;
	},
	requestAnimationFrameWrapper: (key, callback) => {
		if (key) {
			cancelAnimationFrame(key);
		}

		key = requestAnimationFrame(callback);
	},

	templates: {
		mainBody: config => `
			<div id="info-area" class="blockMsg"></div>
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
								<li data-name="Flows" class="module-dependend" data-structr-module="api-builder"><a id="flows_" href="#flows" data-activate-module="flows">Flows</a></li>
								<li data-name="Data"><a id="crud_" href="#crud" data-activate-module="crud">Data</a></li>
								<li data-name="Importer"><a id="importer_" href="#importer" data-activate-module="importer">Importer</a></li>
								<li data-name="Localization"><a id="localization_" href="#localization" data-activate-module="localization">Localization</a></li>
								<li data-name="Virtual Types" class="module-dependend" data-structr-module="api-builder"><a id="virtual-types_" href="#virtual-types" data-activate-module="virtual-types">Virtual Types</a></li>
								<li data-name="Mail Templates" class="edition-dependend" data-structr-edition="Enterprise"><a id="mail-templates_" href="#mail-templates" data-activate-module="mail-templates">Mail Templates</a></li>
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

			<div id="login" class="dialog">

				${_Icons.getSvgIcon(_Icons.iconStructrLogo, 90, 24, ['logo-login'])}

				<form id="loginForm" action="javascript:void(0)">
					<table class="username-password">
						<tr>
							<td><label for="usernameField">Username:</label></td>
							<td><input id="usernameField" type="text" name="username" autofocus autocomplete="username" required></td>
						</tr>
						<tr>
							<td><label for="passwordField">Password:</label></td>
							<td><input id="passwordField" type="password" name="password" autocomplete="current-password" required></td>
						</tr>
						<tr>
							<td colspan="2" class="btn">
								<span id="errorText"></span>
							</td>
						</tr>
						<tr class="username-pw">
							<td colspan="2" class="btn">
								<button id="loginButton" name="login" class="inline-flex items-center hover:bg-gray-100 hover:bg-gray-100 focus:border-gray-666 active:border-green">
									${_Icons.getSvgIcon(_Icons.iconVisibilityKey, 16, 16, ['mr-2'])} Login
								</button>
							</td>
						</tr>
					</table>
				</form>

				<form id="two-factor-form" action="javascript:void(0)">
					<div id="two-factor" style="display:none;">
						<div id="two-factor-qr-code" style="display:none;">
							<div><img></div>
							<div id="two-factor-qr-info">Scan this QR Code with a Google Authenticator compatible app to log in.</div>
						</div>
						<table>
							<tr>
								<td>
									<label for="twoFactorTokenField">Two-Factor Code:</label>
								</td>
								<td>
									<input id="twoFactorTokenField" type="hidden" name="twoFactorToken">
									<input id="twoFactorCodeField" type="text" name="twoFactorCode" required>
								</td>
							</tr>
							<tr>
								<td colspan="2" class="btn">
									<span id="errorText-two-factor"></span>
								</td>
							</tr>
							<tr>
								<td colspan="2" class="btn">
									<button id="loginButtonTFA" name="login" class="inline-flex items-center hover:bg-gray-100 hover:bg-gray-100 focus:border-gray-666 active:border-green">
										${_Icons.getSvgIcon(_Icons.iconVisibilityKey, 16, 16, ['mr-2'])} Login 2FA
									</button>
								</td>
							</tr>
						</table>
					</div>
				</form>
			</div>

			<div id="confirmation" class="dialog">
				<div class="confirmationText mb-4"></div>
				<button class="yesButton hover:bg-gray-100 hover:bg-gray-100 focus:border-gray-666 active:border-green">
					${_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 12, 12, ['icon-green', 'mr-2'])} Yes
				</button>
				<button class="noButton hover:bg-gray-100 hover:bg-gray-100 focus:border-gray-666 active:border-green">
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 12, 12, ['icon-red', 'mr-2'])} No
				</button>
			</div>

			<div id="infoBox" class="dialog">
				<div id="infoText"></div>
				<div class="dialogBtn">
					<button class="closeButton hover:bg-gray-100 focus:border-gray-666 active:border-green">Close</button>
				</div>
			</div>

			<div id="tempInfoBox" class="dialog">
				<div class="infoHeading"></div>
				<div class="infoMsg"></div>
				<div class="dialogBtn">
					<button class="closeButton hover:bg-gray-100 focus:border-gray-666 active:border-green">Close</button>
				</div>
			</div>

			<div id="errorBox" class="dialog">
				<div class="errorText"></div>
				<div class="errorMsg"></div>
				<div class="dialogBtn">
					<button class="closeButton hover:bg-gray-100 focus:border-gray-666 active:border-green">Close</button>
				</div>
			</div>

			<div id="dialogBox" class="dialog">
				<i title="Fullscreen Mode" id="maximizeDialog" class="window-icon minmax">
					${_Icons.getSvgIcon(_Icons.iconMaximizeDialog, 18, 18)}
				</i>
				<i title="Window Mode" id="minimizeDialog" class="window-icon minmax">
					${_Icons.getSvgIcon(_Icons.iconMinimizeDialog, 18, 18)}
				</i>
				<i title="Close" id="closeDialog" class="window-icon close">
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 18, 18)}
				</i>
				<h2 class="dialogTitle"></h2>
				<div class="dialogHeaderWrapper"></div>
				<div class="dialogTextWrapper">
					<div class="dialogText"></div>
				</div>
				<div class="dialogMsg"></div>
				<!--<button id="dialogOkButton">Save</button>-->
				<div class="dialogMeta"></div>
				<div class="dialogBtn flex">
					<button class="closeButton hover:bg-gray-100 focus:border-gray-666 active:border-green">Close</button>
				</div>
			</div>

			<div id="menu-area"></div>
		`
	}
};

Structr.rootUrl        = Structr.getPrefixedRootUrl('/structr/rest/');
Structr.csvRootUrl     = Structr.getPrefixedRootUrl('/structr/csv/');
Structr.graphQLRootUrl = Structr.getPrefixedRootUrl('/structr/graphql/');
Structr.viewRootUrl    = Structr.getPrefixedRootUrl('/');
Structr.wsRoot         = Structr.getPrefixedRootUrl('/structr/ws');
Structr.deployRoot     = Structr.getPrefixedRootUrl('/structr/deploy');

let _TreeHelper = {
	initTree: (tree, initFunction, stateKey) => {

		let initializedTree = $(tree).jstree({
			plugins: ["themes", "dnd", "search", "state", "types", "wholerow"],
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
				if (node.data.svgIcon) {
					if (
						node.data.svgIcon.contains(_Icons.iconFolderOpen) || node.data.svgIcon.contains(_Icons.iconFolderClosed) ||
						node.data.svgIcon.contains(_Icons.iconMountedFolderOpen) || node.data.svgIcon.contains(_Icons.iconMountedFolderClosed)
					) {
						if (newStateIsOpen) {
							node.data.svgIcon = _Icons.getSvgIcon(_Icons.iconFolderOpen);
						}
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
	deepOpen: function(tree, element, parentElements, parentKey, selectedNodeId) {
		if (element && element.id) {

			parentElements = parentElements || [];
			parentElements.unshift(element);

			Command.get(element.id, parentKey, function(loadedElement) {
				if (loadedElement && loadedElement[parentKey]) {
					_TreeHelper.deepOpen(tree, loadedElement[parentKey], parentElements, parentKey, selectedNodeId);
				} else {
					_TreeHelper.open(tree, parentElements, selectedNodeId);
				}
			});
		}
	},
	open: function(tree, dirs, selectedNode) {
		if (dirs.length) {
			tree.jstree('deselect_all');

			let openRecursively = function(list) {

				if (list.length > 0) {

					let first = list.shift();

					tree.jstree('open_node', first.id, function() {
						openRecursively(list);
					});

				} else {
					tree.jstree('select_node', selectedNode);
				}
			};

			openRecursively(dirs);
		}

	},
	refreshTree: function(tree, callback) {
		$(tree).jstree('refresh');

		if (typeof callback === "function") {
			window.setTimeout(function() {
				callback();
			}, 500);
		}
	},
	refreshNode: function(tree, node, callback) {
		$(tree).jstree('refresh_node', node);

		if (typeof callback === "function") {
			window.setTimeout(function() {
				callback();
			}, 500);
		}
	},
	getNode: (tree, node) => {
		return $(tree).jstree('get_node', node);
	},
	isNodeOpened: (tree, node) => {
		let n = _TreeHelper.getNode(tree, node);

		return n?.state.opened;
	},
	makeDroppable: function(tree, list) {
		window.setTimeout(function() {
			list.forEach(function(obj) {
				// only load data necessary for dnd. prevent from loading the complete folder (with its files)
				Command.get(obj.id, 'id,type,isFolder', function(data) {
					StructrModel.createOrUpdateFromData(data, null, false);
					_TreeHelper.makeTreeElementDroppable(tree, obj.id);
				});
			});
		}, 500);
	},
	makeTreeElementDroppable: function(tree, id) {
		let el = $('#' + id + ' > .jstree-wholerow', tree);
		_Dragndrop.makeDroppable(el);
	}
};

function MessageBuilder () {
	this.params = {
		// defaults
		text: 'Default message',
		furtherText: undefined,
		delayDuration: 3000,
		fadeDuration: 1000,
		confirmButtonText: 'Confirm',
		allowConfirmAll: false,
		confirmAllButtonText: 'Confirm all...',
		classNames: ['message'],
		uniqueClass: undefined,
		uniqueCount: 1,
		updatesText: false,
		updatesButtons: false,
		appendsText: false,
		appendSelector: '',
		incrementsUniqueCount: false
	};

	this.requiresConfirmation = function(confirmButtonText) {
		this.params.requiresConfirmation = true;

		if (confirmButtonText) {
			this.params.confirmButtonText = confirmButtonText;
		}

		return this;
	};

	this.allowConfirmAll = function(confirmAllButtonText) {
		this.params.allowConfirmAll = true;

		if (confirmAllButtonText) {
			this.params.confirmAllButtonText = confirmAllButtonText;
		}

		return this;
	};

	this.title = function(title) {
		this.params.title = title;
		return this;
	};

	this.text = function(text) {
		this.params.text = text;
		return this;
	};

	this.furtherText = function(furtherText) {
		this.params.furtherText = furtherText;
		return this;
	};

	this.error = function(text) {
		this.params.text = text;
		return this.className('error');
	};

	this.warning = function(text) {
		this.params.text = text;
		return this.className('warning');
	};

	this.info = function(text) {
		this.params.text = text;
		return this.className('info');
	};

	this.success = function(text) {
		this.params.text = text;
		return this.className('success');
	};

	this.delayDuration = function(delayDuration) {
		this.params.delayDuration = delayDuration;
		return this;
	};

	this.fadeDuration = function(fadeDuration) {
		this.params.fadeDuration = fadeDuration;
		return this;
	};

	this.className = function(className) {
		this.params.classNames.push(className);
		return this;
	};

	this.delayDuration = function(delayDuration) {
		this.params.delayDuration = delayDuration;
		return this;
	};

	this.getButtonHtml = function() {
		return (this.params.requiresConfirmation ? '<button class="confirm">' + this.params.confirmButtonText + '</button>' : '') +
			(this.params.requiresConfirmation && this.params.allowConfirmAll ? '<button class="confirmAll">' + this.params.confirmAllButtonText + '</button>' : '') +
			(this.params.specialInteractionButton ? '<button class="special">' + this.params.specialInteractionButton.text + '</button>' : '');
	};

	this.activateButtons = function(originalMsgBuilder, newMsgBuilder) {

		if (newMsgBuilder.params.requiresConfirmation === true) {

			$('#' + originalMsgBuilder.params.msgId).find('button.confirm').click(function() {
				$(this).remove();
				originalMsgBuilder.hide();
			});

			if (newMsgBuilder.params.allowConfirmAll === true) {

				$('#info-area button.confirmAll').click(function() {
					$('#info-area button.confirm').click();
				});
			}

		} else {

			window.setTimeout(function() {
				originalMsgBuilder.hide();
			}, this.params.delayDuration);

			$('#' + newMsgBuilder.params.msgId).click(function() {
				originalMsgBuilder.hide();
			});

		}

		if (newMsgBuilder.params.specialInteractionButton) {

			$('#' + originalMsgBuilder.params.msgId).find('button.special').click(function() {
				if (newMsgBuilder.params.specialInteractionButton) {
					newMsgBuilder.params.specialInteractionButton.action();

					originalMsgBuilder.hide();
				}
			});
		}
	};

	this.show = function() {

		let uniqueMessageAlreadyPresented = false;

		if (this.params.uniqueClass) {
			// find existing one
			let existingMsgBuilder = $('#info-area .message.' + this.params.uniqueClass).data('msgbuilder');
			if (existingMsgBuilder) {

				uniqueMessageAlreadyPresented = true;

				if (this.params.incrementsUniqueCount) {
					existingMsgBuilder.incrementUniqueCount();
				}

				$('#' + existingMsgBuilder.params.msgId).attr('class', this.params.classNames.join(' '));

				if (this.params.updatesText) {

					$('#info-area .message.' + this.params.uniqueClass + ' .title').html(this.params.title);
					$('#info-area .message.' + this.params.uniqueClass + ' .text').html(this.params.text);

				} else if (this.params.appendsText) {

					$('#info-area .message.' + this.params.uniqueClass + ' .title').html(this.params.title);

					let selector = '#info-area .message.' + this.params.uniqueClass + ' .text';
					if (this.params.appendSelector !== '') {
						selector += ' ' + this.params.appendSelector;
					}
					$(selector).append(this.params.text);

				}

				if (this.params.updatesButtons) {

					$('#info-area .message.' + this.params.uniqueClass + ' .message-buttons').empty().html(this.getButtonHtml());
					this.activateButtons(existingMsgBuilder, this);
				}
			}
		}

		if (uniqueMessageAlreadyPresented === false) {

			this.params.msgId = 'message_' + (Structr.msgCount++);

			$('#info-area').append(`
				<div class="${this.params.classNames.join(' ')}" id="${this.params.msgId}">
					${(this.params.title ? `<h3 class="title">${this.params.title}${this.getUniqueCountElement()}</h3>` : this.getUniqueCountElement())}
					<div class="text">${this.params.text}</div>
					${(this.params.furtherText ? `<div class="furtherText">${this.params.furtherText}</div>` : '')}
					<div class="message-buttons">${this.getButtonHtml()}</div>
				</div>
			`);

			$('#' + this.params.msgId).data('msgbuilder', this);

			this.activateButtons(this, this);
		}
	};

	this.hide = function() {
		$('#' + this.params.msgId).animate({
			opacity: 0,
			height: 0
		}, {
			duration: this.params.fadeDuration,
			complete: function() {
				$(this).remove();
			}
		});
	};

	this.specialInteractionButton = function(buttonText, callback, confirmButtonText) {

		this.params.specialInteractionButton = {
			text: buttonText,
			action: callback
		};

		if (confirmButtonText) {
			return this.requiresConfirmation(confirmButtonText);
		} else {
			this.params.requiresConfirmation = true;
			return this;
		}
	};

	this.uniqueClass = function(className) {
		if (className) {
			className = className.replace(/[\/\. ]/g, "_");
			this.params.uniqueClass = className;
			return this.className(className);
		}
		return this;
	};

	this.incrementsUniqueCount = function() {
		this.params.incrementsUniqueCount = true;
		return this;
	};

	this.updatesText = function() {
		this.params.updatesText = true;
		return this;
	};

	this.updatesButtons = function() {
		this.params.updatesButtons = true;
		return this;
	};

	this.appendsText = function(selector) {
		this.params.appendsText    = true;
		this.params.appendSelector = selector || '';
		return this;
	};

	this.getUniqueCountElement = function() {
		return ' <b class="uniqueCount">' + ((this.params.uniqueCount > 1) ? '(' + this.params.uniqueCount + ') ' : '') + '</b> ';
	};

	this.incrementUniqueCount = function() {
		this.params.uniqueCount++;
		$('#' + this.params.msgId).find('b.uniqueCount').replaceWith(this.getUniqueCountElement());
	};

	return this;
}

let UISettings = {
	getValueForSetting: (setting) => {
		return LSWrapper.getItem(setting.storageKey, setting.defaultValue);
	},
	setValueForSetting: (setting, value, container) => {
		LSWrapper.setItem(setting.storageKey, value);

		if (container) {
			blinkGreen(container);
			setting.onUpdate?.();
		}
	},
	getSettings: (section) => {

		if (!section) {
			// no section given - return all
			return [UISettings.global, UISettings.pages, UISettings.security, UISettings.importer, UISettings.schema];

		} else {

			let settings = UISettings[section];
			if (settings) {
				return settings;
			}
		}

		return null;
	},
	showSettingsForCurrentModule: () => {

		let moduleSettings = UISettings.getSettings(Structr.getActiveModuleName());
		if (moduleSettings) {

			let dropdown = Structr.createSingleDOMElementFromHTML(`<div id="ui-settings-popup" class="dropdown-menu darker-shadow-dropdown dropdown-menu-large">
				<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green" data-preferred-position-x="left">
					${_Icons.getSvgIcon(_Icons.iconUIConfigSettings)}
				</button>
				<div class="dropdown-menu-container" style=display: none;"></div>
			</div>`);

			let container = dropdown.querySelector('.dropdown-menu-container');

			let globalSettings = UISettings.getSettings('global');

			UISettings.appendSettingsSectionToContainer(globalSettings, container);
			UISettings.appendSettingsSectionToContainer(moduleSettings, container);

			Structr.functionBar.appendChild(dropdown);
		}
	},
	appendSettingsSectionToContainer: (section, container) => {

		let sectionDOM = Structr.createSingleDOMElementFromHTML(`<div><div class="font-bold pt-4 pb-2">${section.title}</div></div>`);

		for (let [settingKey, setting] of Object.entries(section.settings)) {
			UISettings.appendSettingToContainer(setting, sectionDOM);
		}

		container.appendChild(sectionDOM);
	},
	appendSettingToContainer: (setting, container) => {

		switch (setting.type) {

			case 'checkbox': {

				let settingDOM = Structr.createSingleDOMElementFromHTML(`<label class="flex items-center p-1"><input type="checkbox"> ${setting.text}</label>`);

				let input = settingDOM.querySelector('input');
				input.checked = UISettings.getValueForSetting(setting);

				input.addEventListener('change', () => {
					UISettings.setValueForSetting(setting, input.checked, input.parentElement);
				});

				container.appendChild(settingDOM);

				break;
			}

			default: {
				console.log('ERROR! Unable to render setting:', setting, container);
			}
		}
	},
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
	schema: {
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
};

function formatKey(text) {
	// don't format custom 'data-*' attributes
	if (text.startsWith('data-')) {
		return text;
	}
	var result = '';
	for (var i = 0; i < text.length; i++) {
		var c = text.charAt(i);
		if (c === c.toUpperCase()) {
			result += ' ' + c;
		} else {
			result += (i === 0 ? c.toUpperCase() : c);
		}
	}
	return result;
}

var keyEventBlocked = true;
var keyEventTimeout;

window.addEventListener('beforeunload', (event) => {
	if (event.target === document) {

		let activeModule = Structr.getActiveModule();
		if (activeModule && activeModule.beforeunloadHandler && typeof activeModule.beforeunloadHandler === "function") {
			let ret = activeModule.beforeunloadHandler();
			if (ret) {
				event.returnValue = ret;
			}
			// persist last menu entry
			LSWrapper.setItem(Structr.lastMenuEntryKey, Structr.lastMenuEntry);
		}

		// Remove dialog data in case of page reload
		LSWrapper.removeItem(Structr.dialogDataKey);
		LSWrapper.save();
	}
});