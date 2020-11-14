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
var header, main;
var sessionId, user;
var lastMenuEntry, menuBlocked;
var dmp;
var editorCursor, ignoreKeyUp;
var dialog, isMax = false;
var dialogBox, dialogMsg, dialogBtn, dialogTitle, dialogMeta, dialogText, dialogHead, dialogCancelButton, dialogSaveButton, saveAndClose, loginBox, dialogCloseButton;
var dialogId;
var pagerType = {}, page = {}, pageSize = {}, sortKey = {}, sortOrder = {}, pagerFilters = {}, pagerExactFilterKeys = {};
var dialogMaximizedKey = 'structrDialogMaximized_' + port;
var expandedIdsKey = 'structrTreeExpandedIds_' + port;
var lastMenuEntryKey = 'structrLastMenuEntry_' + port;
var pagerDataKey = 'structrPagerData_' + port + '_';
var dialogDataKey = 'structrDialogData_' + port;
var dialogHtmlKey = 'structrDialogHtml_' + port;
var scrollInfoKey = 'structrScrollInfoKey_' + port;
var consoleModeKey = 'structrConsoleModeKey_' + port;
var resizeFunction;
var altKey = false, ctrlKey = false, shiftKey = false, eKey = false;

$(function() {

	$.blockUI.defaults.overlayCSS.opacity        = .6;
	$.blockUI.defaults.overlayCSS.cursor         = 'default';
	$.blockUI.defaults.applyPlatformOpacityRules = false;

	header = $('#header');
	main = $('#main');
	loginBox = $('#login');

	dialogBox          = $('#dialogBox');
	dialog             = $('.dialogText', dialogBox);
	dialogText         = $('.dialogText', dialogBox);
	dialogHead         = $('.dialogHeaderWrapper', dialogBox);
	dialogMsg          = $('.dialogMsg', dialogBox);
	dialogBtn          = $('.dialogBtn', dialogBox);
	dialogTitle        = $('.dialogTitle', dialogBox);
	dialogMeta         = $('.dialogMeta', dialogBox);
	dialogCancelButton = $('.closeButton', dialogBox);
	dialogSaveButton   = $('.save', dialogBox);

	$('#loginButton').on('click', function(e) {
		e.stopPropagation();
		var username = $('#usernameField').val();
		var password = $('#passwordField').val();
		Structr.doLogin(username, password);
		return false;
	});
	$('#loginButtonTFA').on('click', function(e) {
		e.stopPropagation();
		var tfaToken = $('#twoFactorTokenField').val();
		var tfaCode  = $('#twoFactorCodeField').val();
		Structr.doTFALogin(tfaCode, tfaToken);
		return false;
	});

	$('#logout_').on('click', function(e) {
		e.stopPropagation();
		Structr.doLogout();
	});

	window.addEventListener('hashchange', (e) => {
		var anchor = getAnchorFromUrl(window.location.href);
		if (anchor === 'logout' || loginBox.is(':visible')) return;
		let allow = Structr.requestActivateModule(e, anchor);

		if (allow === false) {
			window.location.href = e.oldURL;
		}
	});

	$(document).on('mouseenter', '[data-toggle="popup"]', function() {
		var target = $(this).data("target");
		$(target).addClass('visible');
	});

	$(document).on('mouseleave', '[data-toggle="popup"]', function() {
		var target = $(this).data("target");
		$(target).removeClass('visible');
	});

	Structr.connect();

	// Reset keys in case of window switching
	$(window).blur(function(e) {
		altKey = false, ctrlKey = false, shiftKey = false, eKey = false;
	});

	$(window).focus(function(e) {
		altKey = false, ctrlKey = false, shiftKey = false, eKey = false;
	});

	$(window).keyup(function(e) {
		var k = e.which;
		if (k === 16) {
			shiftKey = false;
		}
		if (k === 18) {
			altKey = false;
		}
		if (k === 17) {
			ctrlKey = false;
		}
		if (k === 69) {
			eKey = false;
		}

		if (e.keyCode === 27) {
			if (ignoreKeyUp) {
				ignoreKeyUp = false;
				return false;
			}
			if (dialogSaveButton.length && dialogSaveButton.is(':visible') && !dialogSaveButton.prop('disabled')) {
				ignoreKeyUp = true;
				var saveBeforeExit = confirm('Save changes?');
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
		// This hack prevents FF from closing WS connections on ESC
		if (e.keyCode === 27) {
			e.preventDefault();
		}
		var k = e.which;
		if (k === 16) {
			shiftKey = true;
		}
		if (k === 18) {
			altKey = true;
		}
		if (k === 17) {
			ctrlKey = true;
		}
		if (k === 69) {
			eKey = true;
		}

		let cmdKey = (navigator.platform === 'MacIntel' && e.metaKey);

		// ctrl-s / cmd-s
		if (k === 83 && ((navigator.platform !== 'MacIntel' && e.ctrlKey) || (navigator.platform === 'MacIntel' && cmdKey))) {
			e.preventDefault();
			if (dialogSaveButton && dialogSaveButton.length && dialogSaveButton.is(':visible') && !dialogSaveButton.prop('disabled')) {
				dialogSaveButton.click();
			}
		}
		// Ctrl-Alt-c
		if (k === 67 && altKey && ctrlKey) {
			e.preventDefault();
			_Console.toggleConsole();
		}
		// Ctrl-Alt-f
		if (k === 70 && altKey && ctrlKey) {
			e.preventDefault();
			_Favorites.toggleFavorites();
		}
		// Ctrl-Alt-p
		if (k === 80 && altKey && ctrlKey) {
			e.preventDefault();
			var uuid = prompt('Enter the UUID for which you want to open the properties dialog');
			if (uuid) {
				if (uuid.length === 32) {
					Command.get(uuid, null, function (obj) {
						_Entities.showProperties(obj);
					});
				} else {
					alert('That does not look like a UUID! length != 32');
				}
			}
		}
		// Ctrl-Alt-m
		if (k === 77 && altKey && ctrlKey) {
			e.preventDefault();
			var uuid = prompt('Enter the UUID for which you want to open the content/template edit dialog');
			if (uuid && uuid.length === 32) {
				Command.get(uuid, null, function(obj) {
					_Elements.openEditContentDialog(this, obj);
				});
			} else {
				alert('That does not look like a UUID! length != 32');
			}
		}
		// Ctrl-Alt-g
		if (k === 71 && altKey && ctrlKey) {
		    e.preventDefault();
		    var uuid = prompt('Enter the UUID for which you want to open the access control dialog');
		    if (uuid && uuid.length === 32) {
				Command.get(uuid, null, function(obj) {
					_Entities.showAccessControlDialog(obj);
				});
			} else {
				alert('That does not look like a UUID! length != 32');
		    }
		}
		// Ctrl-Alt-h
		if (k === 72 && altKey && ctrlKey) {
			e.preventDefault();
			if ("schema" === Structr.getActiveModuleName()) {
				_Schema.hideSelectedSchemaTypes();
			}
		}
		// Ctrl-Alt-e
		if (k === 69 && altKey && ctrlKey) {
			e.preventDefault();
			Structr.dialog('Bulk Editing Helper (Ctrl-Alt-E)');
			new RefactoringHelper(dialog).show();
		}

		// Ctrl-Alt-i
		if (k === 73 && altKey && ctrlKey) {
			e.preventDefault();

			let tableData = [];
			Object.keys(_Icons).forEach(function(key) {
				if (typeof _Icons[key] === "string") {
					tableData.push({
						name: key,
						icon: '<i class="' + _Icons.getFullSpriteClass(_Icons[key]) + '" />'
					});
				}
			});

			let html = '<table>' + tableData.map(function(trData) {
				return '<tr><td>' + trData.name + '</td><td>' + trData.icon + '</td></tr>';
			}).join('') + '</table>';

			Structr.dialog('Icons');
			dialogText.html(html);
		}
	});

	resizeFunction = function() {
		Structr.resize();
	};

	$(window).on('resize', resizeFunction);

	dmp = new diff_match_patch();
});

var Structr = {
	modules: {},
	activeModules: {},
	moduleAvailabilityCallbacks: [],
	keyMenuConfig: 'structrMenuConfig_' + port,
	edition: '',
	classes: [],
	expanded: {},
	msgCount: 0,
	currentlyActiveSortable: undefined,
	loadingSpinnerTimeout: undefined,
	keyCodeMirrorSettings: 'structrCodeMirrorSettings_' + port,
	defaultBlockUICss: {
		cursor: 'default',
		border: 'none',
		backgroundColor: 'transparent'
	},
	templateCache: new AsyncObjectCache(function(templateName) {

		Promise.resolve($.ajax('templates/' + templateName + '.html?t=' + (new Date().getTime()))).then(function(templateHtml) {
			Structr.templateCache.addObject(templateHtml, templateName);
		}).catch(function(e) {
			console.log(e.statusText, templateName, e);
		});

	}),

	reconnect: function() {
		Structr.stopPing();
		Structr.stopReconnect();
		reconn = window.setInterval(function() {
			wsConnect();
		}, 1000);
		wsConnect();
	},
	stopReconnect: function() {
		if (reconn) {
			window.clearInterval(reconn);
			reconn = undefined;
			user = undefined;
		}
	},
	init: function() {
		$('#errorText').empty();
		Structr.ping();
		Structr.startPing();
	},
	ping: function(callback) {

		if (ws.readyState !== 1) {
			Structr.reconnect();
		}

		sessionId = Structr.getSessionId();

		if (sessionId) {
			Command.ping(callback);
		} else {
			Structr.renewSessionId(function() {
				Command.ping(callback);
			});
		}
	},
	refreshUi: function(isLogin = false) {
		Structr.showLoadingSpinner();

		Structr.clearMain();
		Structr.loadInitialModule(isLogin, function() {
			Structr.startPing();
			if (!dialogText.text().length) {
				LSWrapper.removeItem(dialogDataKey);
			} else {
				var dialogData = JSON.parse(LSWrapper.getItem(dialogDataKey));
				if (dialogData) {
					Structr.restoreDialog(dialogData);
				}
			}
			Structr.hideLoadingSpinner();
			_Console.initConsole();
			_Favorites.initFavorites();
		});
	},
	updateUsername: function(name) {
		if (name !== user) {
			user = name;
			$('#logout_').html('Logout <span class="username">' + name + '</span>');
		}
	},
	startPing: function() {
		Structr.stopPing();
		if (!ping) {
			ping = window.setInterval(function() {
				Structr.ping();
			}, 1000);
		}
	},
	stopPing: function() {
		if (ping) {
			window.clearInterval(ping);
			ping = undefined;
		}
	},
	getSessionId: function() {
		return Cookies.get('JSESSIONID');
	},
	connect: function() {
		sessionId = Structr.getSessionId();
		if (!sessionId) {
			Structr.renewSessionId(function() {
				wsConnect();
			});
		} else {
			wsConnect();
		}
	},
	login: function(text) {

		if (!loginBox.is(':visible')) {

			fastRemoveAllChildren(main[0]);

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
	clearLoginForm: function() {
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
	toggle2FALoginBox: function(data) {

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
	doLogin: function(username, password) {
		Structr.renewSessionId(function() {
			Command.login({
				username: username,
				password: password
			});
		});
	},
	doTFALogin: function(twoFactorCode, twoFacorToken) {
		Structr.renewSessionId(function() {
			Command.login({
				twoFactorCode: twoFactorCode,
				twoFactorToken: twoFacorToken
			});
		});
	},
	doLogout: function(text) {
		_Favorites.logoutAction();
		_Console.logoutAction();
		LSWrapper.save();
		if (Command.logout(user)) {
			Cookies.remove('JSESSIONID');
			sessionId.length = 0;
			Structr.renewSessionId();
			Structr.clearMain();
			Structr.clearVersionInfo();
			Structr.login(text);
			return true;
		}
		ws.close();
		return false;
	},
	renewSessionId: function(callback) {
		$.get('/').always(function() {
			sessionId = Structr.getSessionId();

			if (!sessionId && location.protocol === 'http:') {

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
	loadInitialModule: function(isLogin, callback) {

		LSWrapper.restore(function() {

			Structr.expanded = JSON.parse(LSWrapper.getItem(expandedIdsKey));

			var browserUrl = window.location.href;
			var anchor = getAnchorFromUrl(browserUrl);
			lastMenuEntry = ((!isLogin && anchor && anchor !== 'logout') ? anchor : Structr.getActiveModuleName());
			if (!lastMenuEntry) {
				lastMenuEntry = Structr.getActiveModuleName() || 'dashboard';
			}
			Structr.updateVersionInfo(0, isLogin);
			Structr.doActivateModule(lastMenuEntry);

			callback();
		});
	},
	clearMain: function() {
		var newDroppables = new Array();
		$.ui.ddmanager.droppables['default'].forEach(function(droppable, i) {
			if (!droppable.element.attr('id') || droppable.element.attr('id') !== 'graph-canvas') {
			} else {
				newDroppables.push(droppable);
			}
		});
		$.ui.ddmanager.droppables['default'] = newDroppables;
		$('iframe').contents().remove();
		fastRemoveAllChildren(main[0]);
		$('#graph-box').hide();
	},
	confirmation: function(text, yesCallback, noCallback) {
		if (text) {
			$('#confirmation .confirmationText').html(text);
		}
		var yesButton = $('#confirmation .yesButton');
		var noButton = $('#confirmation .noButton');

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
	restoreDialog: function(dialogData) {

		window.setTimeout(function() {

			Structr.blockUI(dialogData);
			Structr.resize();

		}, 1000);

	},
	dialog: function(text, callbackOk, callbackCancel, customClasses) {

		if (browser) {

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

			dialogBtn.html('<button class="closeButton">Close</button>');
			dialogCancelButton = $('.closeButton', dialogBox);

			$('.speechToText', dialogBox).remove();

			if (text) {
				dialogTitle.html(text);
			}

			dialogCancelButton.off('click').on('click', function(e) {
				e.stopPropagation();
				dialogText.empty();
				$.unblockUI({
					fadeOut: 25
				});

				dialogBtn.children(':not(.closeButton)').remove();

				Structr.focusSearchField();

				LSWrapper.removeItem(dialogDataKey);

				if (callbackCancel) {
					window.setTimeout(function() {
						callbackCancel();
					}, 100);
				}
			});

			var dimensions = Structr.getDialogDimensions(24, 24);
			Structr.blockUI(dimensions);

			Structr.resize();

			dimensions.text = text;
			LSWrapper.setItem(dialogDataKey, JSON.stringify(dimensions));
		}
	},
	focusSearchField: function() {
		let activeModule = Structr.getActiveModule();
		if (activeModule) {
			let searchField = activeModule.searchField;

			if (searchField) {
				searchField.focus();
			}
		}
	},
	getDialogDimensions: function(marginLeft, marginTop) {

		var winW = $(window).width();
		var winH = $(window).height();

		var width = Math.min(900, winW - marginLeft);
		var height = Math.min(600, winH - marginTop);

		return {
			width: width,
			height: height,
			left: parseInt((winW - width) / 2),
			top: parseInt((winH - height) / 2)
		};

	},
	blockUI: function(dimensions) {

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
	setSize: function(w, h, dw, dh) {

		var l = parseInt((w - dw) / 2);
		var t = parseInt((h - dh) / 2);

		var horizontalOffset = 98;

		// needs to be calculated like this because the elements in the dialogHead (tabs) are floated and thus the .height() method returns 0
		var headerHeight = (dialogText.position().top + dialogText.scrollParent().scrollTop()) - dialogHead.position().top;

		$('#dialogBox .dialogTextWrapper').css({
			width: (dw - 28) + 'px',
			height: (dh - horizontalOffset - headerHeight) + 'px'
		});

		$('.blockPage').css({
			width: dw + 'px',
			//height: dh + 'px',
			top: t + 'px',
			left: l + 'px'
		});

		var codeMirror = $('#dialogBox .CodeMirror');
		if (codeMirror.length) {

			var cmPosition = codeMirror.position();
			var bottomOffset = 24;

			var cmHeight = (dh - horizontalOffset - headerHeight - bottomOffset - cmPosition.top) + 'px';

			$('.CodeMirror:not(.cm-schema-methods)').css({
				height: cmHeight
			});

			$('.CodeMirror:not(.cm-schema-methods) .CodeMirror-gutters').css({
				height: cmHeight
			});

			$('.CodeMirror:not(.cm-schema-methods)').each(function(i, el) {
				el.CodeMirror.refresh();
			});

		}

		$('.fit-to-height').css({
			height: h - 84 + 'px'
		});

	},
	resize: function(callback) {

		isMax = LSWrapper.getItem(dialogMaximizedKey);

		if (isMax) {
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
	maximize: function() {

		// Calculate dimensions of dialog
		if ($('.blockPage').length && !loginBox.is(':visible')) {
			Structr.setSize($(window).width(), $(window).height(), $(window).width() - 24, $(window).height() - 24);
		}

		isMax = true;
		$('#maximizeDialog').hide();
		$('#minimizeDialog').show().off('click').on('click', function() {
			isMax = false;
			LSWrapper.removeItem(dialogMaximizedKey);
			Structr.resize();
		});

		LSWrapper.setItem(dialogMaximizedKey, '1');

	},
	error: function(text, confirmationRequired) {
		var message = new MessageBuilder().error(text);
		if (confirmationRequired) {
			message.requiresConfirmation();
		} else {
			message.delayDuration(2000).fadeDuration(1000);
		}
		message.show();
	},
	errorFromResponse: function(response, url, additionalParameters) {
		var errorText = '';

		if (response.errors && response.errors.length) {

			var errorLines = [response.message];

			response.errors.forEach(function(error) {

				var errorMsg = (error.type ? error.type : '');
				if (error.property) {
					errorMsg += '.' + error.property;
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

			});

			errorText = errorLines.join('<br>');

		} else {

			if (url) {
				errorText = url + ': ';
			}

			errorText += response.code + '<br>';

			Object.keys(response).forEach(function(key) {
				if (key !== 'code') {
					errorText += '<b>' + key.capitalize() + '</b>: ' + response[key] + '<br>';
				}
			});
		}

		var message = new MessageBuilder().error(errorText);

		if (additionalParameters) {
			if (additionalParameters.requiresConfirmation) {
				message.requiresConfirmation();
			}
			if (additionalParameters.statusCode) {
				var title = Structr.getErrorTextForStatusCode(additionalParameters.statusCode);
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
	getErrorTextForStatusCode: function(statusCode) {
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
	loaderIcon: function(element, css) {
		element.append('<img class="loader-icon" alt="Loading..." title="Loading.." src="' + _Icons.getSpinnerImageAsData() + '">');
		var li = $('.loader-icon', element);
		if (css) {
			li.css(css);
		}
		return li;
	},
	updateButtonWithAjaxLoaderAndText: function(btn, html) {
		btn.attr('disabled', 'disabled').addClass('disabled').html(html + ' <img src="' + _Icons.ajax_loader_2 + '">');
	},
	updateButtonWithSuccessIcon: function(btn, html) {
		btn.attr('disabled', null).removeClass('disabled').html(html + ' <i class="tick ' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" />');
		window.setTimeout(function() {
			$('.tick', btn).fadeOut();
		}, 1000);
	},
	tempInfo: function(text, autoclose) {
		window.clearTimeout(dialogId);
		if (text)
			$('#tempInfoBox .infoHeading').html('<i class="' + _Icons.getFullSpriteClass(_Icons.information_icon) + '" /> ' + text);
		if (autoclose) {
			dialogId = window.setTimeout(function() {
				$.unblockUI({
					fadeOut: 25
				});
			}, 3000);
		}
		$('#tempInfoBox .closeButton').on('click', function(e) {
			e.stopPropagation();
			window.clearTimeout(dialogId);
			$.unblockUI({
				fadeOut: 25
			});
			dialogBtn.children(':not(.closeButton)').remove();

			Structr.focusSearchField();
		});

		$.blockUI({
			message: $('#tempInfoBox'),
			css: Structr.defaultBlockUICss
		});
	},
	reconnectDialog: function(text) {
		if (text) {
			$('#tempErrorBox .errorText').html('<i class="' + _Icons.getFullSpriteClass(_Icons.error_icon) + '" /> ' + text);
		}
		$('#tempErrorBox .closeButton').hide();

		$.blockUI({
			message: $('#tempErrorBox'),
			css: Structr.defaultBlockUICss
		});
	},
	blockMenu: function() {
		menuBlocked = true;
		$('#menu > ul > li > a').attr('disabled', 'disabled').addClass('disabled');
	},
	unblockMenu: function(ms) {
		var ms = ms || 0;
		// Wait ms before releasing the main menu
		window.setTimeout(function() {
			menuBlocked = false;
			$('#menu > ul > li > a').removeAttr('disabled', 'disabled').removeClass('disabled');
		}, ms);
	},
	requestActivateModule: function(event, name) {
		if (menuBlocked) return;
		event.stopPropagation();
		if (Structr.getActiveModuleName() !== name || main.children().length === 0) {
			return Structr.doActivateModule(name);
		}

		return true;
	},
	doActivateModule: function(name) {
		if (Structr.modules[name]) {
			var activeModule = Structr.getActiveModule();

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
	activateMenuEntry: function(name) {
		Structr.blockMenu();
		var menuEntry = $('#' + name + '_');
		var li = menuEntry.parent();
		if (li.hasClass('active')) {
			return false;
		}
		lastMenuEntry = name;
		$('.menu li').removeClass('active');
		li.addClass('active');
		$('#title').text('Structr ' + menuEntry.text());
		window.location.hash = lastMenuEntry;
		if (lastMenuEntry && lastMenuEntry !== 'logout') {
			LSWrapper.setItem(lastMenuEntryKey, lastMenuEntry);
		}
	},
	registerModule: function(module) {
		var name = module._moduleName;
		if (!name || name.trim().length === 0) {
			new MessageBuilder().error("Cannot register module without a name - ignoring attempt. To fix this error, please add the '_moduleName' variable to the module.").show();
		} else if (!Structr.modules[name]) {
			Structr.modules[name] = module;
		} else {
			new MessageBuilder().error("Cannot register module '" + name + "' a second time - ignoring attempt.").show();
		}
	},
	getActiveModuleName: function() {
		return lastMenuEntry || LSWrapper.getItem(lastMenuEntryKey);
	},
	getActiveModule: function() {
		return Structr.modules[Structr.getActiveModuleName()];
	},
	isModuleActive: function(module) {
		return (module._moduleName === Structr.getActiveModuleName());
	},
	containsNodes: function(element) {
		return (element && Structr.numberOfNodes(element) && Structr.numberOfNodes(element) > 0);
	},
	numberOfNodes: function(element, excludeId) {
		var childNodes = $(element).children('.node');
		if (excludeId) {
			childNodes = childNodes.not('.' + excludeId + '_');
		}
		var n = childNodes.length;

		return n;
	},
	findParent: function(parentId, componentId, pageId, defaultElement) {
		var parent = Structr.node(parentId, null, componentId, pageId);
		if (!parent) {
			parent = defaultElement;
		}
		return parent;
	},
	parent: function(id) {
		return Structr.node(id) && Structr.node(id).parent().closest('.node');
	},
	node: function(id, prefix) {
		var p = prefix || '#id_';
		var node = $($(p + id)[0]);
		return node.length ? node : undefined;
	},
	entity: function(id, parentId) {
		var entityElement = Structr.node(id, parentId);
		var entity = Structr.entityFromElement(entityElement);
		return entity;
	},
	getClass: function(el) {
		var c;
		$(Structr.classes).each(function(i, cls) {
			if (el && $(el).hasClass(cls)) {
				c = cls;
			}
		});
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

				if (filesMain && filesMain.length) {
					filesMain.hide();
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
	openSlideOut: function(triggerEl, slideoutElement, activeTabKey, callback) {

		var storedRightSlideoutWidth = LSWrapper.getItem(_Pages.rightSlideoutWidthKey);
		var rsw = storedRightSlideoutWidth ? parseInt(storedRightSlideoutWidth) : (slideoutElement.width() + 12);

		var t = $(triggerEl);
		t.addClass('active');
		var slideoutWidth = rsw + 12;
		LSWrapper.setItem(activeTabKey, t.prop('id'));
		slideoutElement.width(rsw);
		slideoutElement.animate({right: 0 + 'px'}, 100, function() {
			if (typeof callback === 'function') {
				callback({sw: slideoutWidth, isOpenAction: true});
			}
		}).zIndex(1);
		slideoutElement.addClass('open');

		t.draggable({
			axis: 'x',
			start: function(e, ui) {
				$('.column-resizer-blocker').show();
				t.addClass('noclick');
			},
			drag: function(e, ui) {
				var w = $(window).width() - ui.offset.left - 20;
				slideoutElement.css({
					width: w + 'px'
				});
				ui.position.top += (ui.helper.width() / 2 - 6);
				ui.position.left = - t.width() / 2 - 20;
				var oldRightSlideoutWidth = slideoutWidth;
				slideoutWidth = w + 12;

				if (typeof callback === 'function') {
					LSWrapper.setItem(_Pages.rightSlideoutWidthKey, slideoutElement.width());
					callback({sw: (slideoutWidth - oldRightSlideoutWidth)});
				}
			},
			stop: function(e, ui) {
				$('.column-resizer-blocker').hide();
				// remove noclick class after 200ms in case the mouseup event is not triggered while over the element (which leads to noclick remaining)
				window.setTimeout(function() {
					t.removeClass('noclick');
				}, 200);
				LSWrapper.setItem(_Pages.rightSlideoutWidthKey, slideoutElement.width());
				t.css({
					left: "",
					top: ""
				});
			}
		});
	},
	openLeftSlideOut: function(triggerEl, slideoutElement, activeTabKey, callback) {
		var storedLeftSlideoutWidth = LSWrapper.getItem(_Pages.leftSlideoutWidthKey);
		var psw = storedLeftSlideoutWidth ? parseInt(storedLeftSlideoutWidth) : (slideoutElement.width() + 12);

		var t = $(triggerEl);
		t.addClass('active');
		var slideoutWidth = psw + 12;
		LSWrapper.setItem(activeTabKey, t.prop('id'));
		slideoutElement.width(psw);
		slideoutElement.animate({left: 0 + 'px'}, 100, function() {
			if (typeof callback === 'function') {
				callback({sw: slideoutWidth, isOpenAction: true});
			}
		}).zIndex(1);
		slideoutElement.addClass('open');

		t.draggable({
			axis: 'x',
			start: function(e, ui) {
				$('.column-resizer-blocker').show();
				$(this).addClass('noclick');
			},
			drag: function(e, ui) {
				var w = ui.position.left - 12;
				slideoutElement.css({
					width: w + 'px'
				});
				ui.position.top += (ui.helper.width() / 2 - 6);
				ui.position.left -= (ui.helper.width() / 2 - 6);
				var oldLeftSlideoutWidth = slideoutWidth;
				slideoutWidth = w + 12;

				if (typeof callback === 'function') {
					LSWrapper.setItem(_Pages.leftSlideoutWidthKey, slideoutElement.width());
					callback({sw: (slideoutWidth - oldLeftSlideoutWidth)});
				}
			},
			stop: function(e, ui) {
				$('.column-resizer-blocker').hide();
				// remove noclick class after 200ms in case the mouseup event is not triggered while over the element (which leads to noclick remaining)
				window.setTimeout(function() {
					t.removeClass('noclick');
				}, 200);
				LSWrapper.setItem(_Pages.leftSlideoutWidthKey, slideoutElement.width());
				t.css({
					left: "",
					top: ""
				});
			}
		});
	},
	closeSlideOuts: function(slideouts, activeTabKey, callback) {
		var wasOpen = false;
		var rsw = 0;

		slideouts.forEach(function(slideout) {
			slideout.removeClass('open');
			var left = slideout.position().left;
			var sw = slideout.width() + 12;

			if (Math.abs($(window).width() - left) >= 3) {
				wasOpen = true;
				rsw = sw;
				slideout.animate({right: '-=' + sw + 'px'}, 100, function() {
					if (typeof callback === 'function') {
						callback(wasOpen, 0, -rsw);
					}
				}).zIndex(2);
				$('.compTab.active', slideout).removeClass('active').draggable('destroy');

				var openSlideoutCallback = slideout.data('closeCallback');
				if (typeof openSlideoutCallback === 'function') {
					openSlideoutCallback();
				}
			}
		});

		LSWrapper.removeItem(activeTabKey);
	},
	closeLeftSlideOuts: function(slideouts, activeTabKey, callback) {
		var wasOpen = false;
		var osw;

		slideouts.forEach(function(slideout) {
			slideout.removeClass('open');
			var left = slideout.position().left;
			var sw = slideout.width() + 12;

			if (Math.abs(left) <= 3) {
				wasOpen = true;
				osw = sw;
				slideout.animate({left: - sw -1 + 'px'}, 100, function() {
					if (typeof callback === 'function') {
						callback(wasOpen, -osw, 0);
					}
				}).zIndex(2);
				$('.compTab.active', slideout).removeClass('active').draggable('destroy');
			}
		});

		LSWrapper.removeItem(activeTabKey);
	},
	updateVersionInfo: function(retryCount = 0, isLogin = false) {

		fetch(rootUrl + '/_env').then(function(response) {

			if (response.ok) {
				return response.json();
			} else {
				throw Error("Unable to read env resource data");
			}

		}).then(function(data) {

			let envInfo = data.result;

			let dbInfoEl = $('#header .structr-instance-db');

			if (envInfo.databaseService) {
				let driverName = Structr.getDatabaseDriverNameForDatabaseServiceName(envInfo.databaseService);
				let icon       = _Icons.database_icon;

				if (envInfo.databaseService === 'MemoryDatabaseService') {
					icon = _Icons.database_error_icon;
				}

				dbInfoEl.html('<span><i class="' + _Icons.getFullSpriteClass(icon) + '" title="' + driverName + '"></span>');

				if (envInfo.databaseService === 'MemoryDatabaseService') {
					Structr.appendInMemoryInfoToElement($('span', dbInfoEl), $('span i', dbInfoEl));

					if (isLogin) {
						new MessageBuilder().warning(Structr.inMemorWarningText).requiresConfirmation().show();
					}
				}
			}

			$('#header .structr-instance-name').text(envInfo.instanceName);
			$('#header .structr-instance-stage').text(envInfo.instanceStage);

			if (true == envInfo.maintenanceModeActive) {
				$('#header .structr-instance-maintenance').text("MAINTENANCE");
			}

			let ui = envInfo.components['structr-ui'];
			if (ui) {

				let version     = ui.version;
				let build       = ui.build;
				let date        = ui.date;
				let versionLink = 'https://structr.com/download';
				let versionInfo = '<a target="_blank" href="' + versionLink + '">' + version + '</a>';
				if (build && date) {
					versionInfo += '<span> build </span><a target="_blank" href="https://github.com/structr/structr/commit/' + build + '">' + build + '</a><span> (' + date + ')</span>';
				}

				if (envInfo.edition) {

					Structr.edition = envInfo.edition;

					var tooltipText = 'Structr ' + envInfo.edition + ' Edition';
					if (envInfo.licensee) {
						tooltipText += '\nLicensed to: ' + envInfo.licensee;
					} else {
						tooltipText += '\nUnlicensed';
					}

					versionInfo += '<i title="' + tooltipText + '" class="edition-icon ' + _Icons.getFullSpriteClass(_Icons.getIconForEdition(envInfo.edition)) + '"></i>';

					$('.structr-version').html(versionInfo);

					_Dashboard.checkLicenseEnd(envInfo, $('.structr-version'), {
						offsetX: -300,
						helpElementCss: {
							color: "black",
							fontSize: "8pt",
							lineHeight: "1.7em"
						}
					});

				} else {
					$('.structr-version').html(versionInfo);
				}
			}

			Structr.activeModules = envInfo.modules;
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
	updateMainMenu: function (menuConfig) {

		LSWrapper.setItem(Structr.keyMenuConfig, menuConfig);

		let menu      = $('#menu');
		let submenu   = $('#submenu');
		let hamburger = $('#menu li.submenu-trigger');

		// first move all elements from main menu to submenu
		$('li[data-name]', menu).appendTo(submenu);

		menuConfig.main.forEach(function(entry) {
			$('li[data-name="' + entry + '"]', menu).insertBefore(hamburger);
		});

		menuConfig.sub.forEach(function(entry) {
			$('#submenu li').last().after($('li[data-name="' + entry + '"]', menu));
		});

	},
	inMemorWarningText:"Please note that the system is currently running on an in-memory database implementation. Data is not persisted and will be lost after restarting the instance! You can use the configuration tool to configure a database connection.",
	appendInMemoryInfoToElement: function(el, optionalToggleElement) {

		let config = {
			element: el,
			text: Structr.inMemorWarningText,
			customToggleIcon: _Icons.database_error_icon,
			helpElementCss: {
				'border': '2px solid red',
				'border-radius': '4px',
				'font-weight': 'bold',
				'font-size': '15px',
				'color': '#000'
			}
		};

		if (optionalToggleElement) {
			config.toggleElement = optionalToggleElement;
		}

		Structr.appendInfoTextToElement(config);
	},
	getDatabaseDriverNameForDatabaseServiceName: function (databaseServiceName) {
		switch (databaseServiceName) {
			case 'BoltDatabaseService':
				return 'Bolt Database Driver';

			case 'MemoryDatabaseService':
				return 'In-Memory Database Driver';
				break;
		}

		return 'Unknown database driver!';
	},
	clearVersionInfo: function() {
		$('.structr-version').html('');
	},
	getId: function(element) {
		var id = Structr.getIdFromPrefixIdString($(element).prop('id'), 'id_') || $(element).data('nodeId');
		return id || undefined;
	},
	getIdFromPrefixIdString: function(idString, prefix) {
		if (!idString || !idString.startsWith(prefix)) {
			return false;
		}
		return idString.substring(prefix.length);
	},
	getComponentId: function(element) {
		return Structr.getIdFromPrefixIdString($(element).prop('id'), 'componentId_') || undefined;
	},
	getUserId: function(element) {
		return element.data('userId');
	},
	getGroupId: function(element) {
		return element.data('groupId');
	},
	getActiveElementId: function(element) {
		return Structr.getIdFromPrefixIdString($(element).prop('id'), 'active_') || undefined;
	},
	adaptUiToAvailableFeatures: function() {
		Structr.adaptUiToAvailableModules();
		Structr.adaptUiToEdition();
	},
	adaptUiToAvailableModules: function() {
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
	isModulePresent: function(moduleName) {
		return Structr.activeModules[moduleName] !== undefined;
	},
	isModuleInformationAvailable: function() {
		return (Object.keys(Structr.activeModules).length > 0);
	},
	performModuleDependendAction: function(action) {
		if (Structr.isModuleInformationAvailable()) {
			action();
		} else {
			Structr.registerActionAfterModuleInformationIsAvailable(action);
		}
	},
	registerActionAfterModuleInformationIsAvailable: function(cb) {
		Structr.moduleAvailabilityCallbacks.push(cb);
	},
	adaptUiToEdition: function() {
		$('.edition-dependend').each(function(idx, element) {
			var el = $(element);

			if (Structr.isAvailableInEdition(el.data('structr-edition'))) {
				if (!el.is(':visible')) el.show();
			} else {
				el.hide();
			}
		});
	},
	isAvailableInEdition: function(requiredEdition) {
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
	updateMainHelpLink: function(newUrl) {
		$('#main-help a').attr('href', newUrl);
	},
	isButtonDisabled: function(button) {
		return $(button).data('disabled');
	},
	disableButton: function(btn) {
		$(btn).addClass('disabled').attr('disabled', 'disabled');
	},
	enableButton: function(btn) {
		$(btn).removeClass('disabled').removeAttr('disabled');
	},
	addExpandedNode: function(id) {

		if (id) {
			var alreadyStored = Structr.getExpanded()[id];
			if (!alreadyStored) {

				Structr.getExpanded()[id] = true;
				LSWrapper.setItem(expandedIdsKey, JSON.stringify(Structr.expanded));

			}
		}
	},
	removeExpandedNode: function(id) {

		if (id) {
			delete Structr.getExpanded()[id];
			LSWrapper.setItem(expandedIdsKey, JSON.stringify(Structr.expanded));
		}
	},
	isExpanded: function(id) {

		if (id) {
			var isExpanded = (Structr.getExpanded()[id] === true) ? true : false;

			return isExpanded;
		}

		return false;
	},
	getExpanded: function() {
		if (!Structr.expanded) {
			Structr.expanded = JSON.parse(LSWrapper.getItem(expandedIdsKey));
		}

		if (!Structr.expanded) {
			Structr.expanded = {};
		}
		return Structr.expanded;
	},
	showAndHideInfoBoxMessage: function(msg, msgClass, delayTime, fadeTime) {
		var newDiv = $('<div class="infoBox ' + msgClass + '"></div>');
		newDiv.text(msg);
		dialogMsg.html(newDiv);
		$('.infoBox', dialogMsg).delay(delayTime).fadeOut(fadeTime);
	},
	initVerticalSlider: function(sliderEl, localstorageKey, minWidth, dragCallback, isRight) {

		if (typeof dragCallback !== 'function') {
			console.error('dragCallback is not a function!');
			return;
		}

		$(sliderEl).draggable({
			axis: 'x',
			start: function(e, ui) {
				$('.column-resizer-blocker').show();
				let left = Math.min(window.innerWidth - minWidth, Math.max(minWidth, ui.position.left));
			},
			drag: function(e, ui) {

				let left = Math.min(window.innerWidth - minWidth, Math.max(minWidth, ui.position.left));

				// If there are two resizer elements, distance between resizers
				// must always be larger than minWidth.
				if ($(this).hasClass('column-resizer-left') && $('.column-resizer-right').length > 0) {
					left = Math.min(left, $('.column-resizer-right').position().left - minWidth);
				} else if ($(this).hasClass('column-resizer-right') && $('.column-resizer-left').length > 0) {
					left = Math.max(left, $('.column-resizer-left').position().left + minWidth);
				}

				ui.position.left = left;
				let val = (isRight === true) ? window.innerWidth - ui.position.left : ui.position.left;
				dragCallback(val);
			},
			stop: function(e, ui) {
				$('.column-resizer-blocker').hide();
				let val = (isRight === true) ? window.innerWidth - ui.position.left : ui.position.left;
				LSWrapper.setItem(localstorageKey, val);
			}
		});

	},
	appendInfoTextToElement: function(config) {

		let element            = config.element;
		let appendToElement    = config.appendToElement || element;
		let text               = config.text || 'No text supplied!';
		let toggleElementCss   = config.css || {};
		let toggleElementClass = config.class || undefined;
		let elementCss         = config.elementCss || {};
		let helpElementCss     = config.helpElementCss || {};
		let customToggleIcon   = config.customToggleIcon || _Icons.information_icon;
		let insertAfter        = config.insertAfter || false;
		let offsetX            = config.offsetX || 0;
		let offsetY            = config.offsetY || 0;

		let createdElements = [];

		let customToggleElement = true;
		let toggleElement = config.toggleElement;
		if (!toggleElement) {
			customToggleElement = false;
			toggleElement = $('<span><i class="' + _Icons.getFullSpriteClass(customToggleIcon) + '"></span>');
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
	refreshPositionsForCurrentlyActiveSortable: function() {

		if (Structr.currentlyActiveSortable) {

			Structr.currentlyActiveSortable.sortable({ refreshPositions: true });

			window.setTimeout(function() {
				Structr.currentlyActiveSortable.sortable({ refreshPositions: false });
			}, 500);
		}
	},
	handleGenericMessage: function(data) {

		let showScheduledJobsNotifications = Importer.isShowNotifications();

		switch (data.type) {

			case "CSV_IMPORT_STATUS":

				if (me.username === data.username) {

					let titles = {
						BEGIN: 'CSV Import started',
						CHUNK: 'CSV Import status',
						END:   'CSV Import finished'
					};

					let texts = {
						BEGIN: 'Started importing CSV data',
						CHUNK: 'Finished importing chunk ' + data.currentChunkNo + ' / ' + data.totalChunkNo,
						END:   'Finished importing CSV data (Time: ' + data.duration + ')'
					};

					new MessageBuilder().title(titles[data.subtype]).uniqueClass('csv-import-status').updatesText().requiresConfirmation().allowConfirmAll().className((data.subtype === 'END') ? 'success' : 'info').text(texts[data.subtype]).show();
				}
				break;

			case "CSV_IMPORT_ERROR":

				if (me.username === data.username) {
					new MessageBuilder()
							.title(data.title)
							.error(data.text)
							.requiresConfirmation()
							.allowConfirmAll()
							.show();
				}
				break;

			case "FILE_IMPORT_STATUS":

				var fileImportTitles = {
					QUEUED: 'Import added to queue',
					BEGIN: 'Import started',
					CHUNK: 'Import status',
					END: 'Import finished',
					WAIT_ABORT: 'Import waiting to abort',
					ABORTED: 'Import aborted',
					WAIT_PAUSE: 'Import waiting to pause',
					PAUSED: 'Import paused',
					RESUMED: 'Import resumed'
				};

				var fileImportTexts = {
					QUEUED: 'Import of <b>' + data.filename + '</b> will begin after currently running/queued job(s)',
					BEGIN: 'Started importing data from <b>' + data.filename + '</b>',
					CHUNK: 'Finished importing chunk ' + data.currentChunkNo + ' of <b>' + data.filename + '</b><br>Objects created: ' + data.objectsCreated + '<br>Time: ' + data.duration + '<br>Objects/s: ' + data.objectsPerSecond,
					END: 'Finished importing data from <b>' + data.filename + '</b><br>Objects created: ' + data.objectsCreated + '<br>Time: ' + data.duration + '<br>Objects/s: ' + data.objectsPerSecond,
					WAIT_ABORT: 'The import of <b>' + data.filename + '</b> will be aborted after finishing the current chunk',
					ABORTED: 'The import of <b>' + data.filename + '</b> has been aborted',
					WAIT_PAUSE: 'The import of <b>' + data.filename + '</b> will be paused after finishing the current chunk',
					PAUSED: 'The import of <b>' + data.filename + '</b> has been paused',
					RESUMED: 'The import of <b>' + data.filename + '</b> has been resumed'
				};

				if (showScheduledJobsNotifications && me.username === data.username) {

					var msg = new MessageBuilder()
							.title(data.jobtype + ' ' + fileImportTitles[data.subtype])
							.className((data.subtype === 'END') ? 'success' : 'info')
							.text(fileImportTexts[data.subtype])
							.uniqueClass(data.jobtype + '-import-status-' + data.filepath);

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

				if (showScheduledJobsNotifications && me.username === data.username) {

					var text = data.message;
					if (data.message !== data.stringvalue) {
						text += '<br>' + data.stringvalue;
					}

					new MessageBuilder()
							.title("Exception while importing " + data.jobtype)
							.error("File: " + data.filepath + "<br>" + text)
							.requiresConfirmation()
							.allowConfirmAll()
							.show();
				}

				if (Structr.isModuleActive(Importer)) {
					Importer.updateJobTable();
				}
				break;

			case "SCRIPT_JOB_STATUS":

				var scriptJobTitles = {
					QUEUED: 'Script added to queue',
					BEGIN: 'Script started',
					END: 'Script finished'
				};
				var scriptJobTexts = {
					QUEUED: 'Script job #' + data.jobId + ' will begin after currently running/queued job(s)',
					BEGIN: 'Started script job #' + data.jobId + ((data.jobName.length === 0) ? '' : ' (' + data.jobName + ')'),
					END: 'Finished script job #' + data.jobId + ((data.jobName.length === 0) ? '' : ' (' + data.jobName + ')')
				};

				if (showScheduledJobsNotifications && me.username === data.username) {

					let msg = new MessageBuilder()
							.title(scriptJobTitles[data.subtype])
							.className((data.subtype === 'END') ? 'success' : 'info')
							.text('<div>' + scriptJobTexts[data.subtype] + '</div>')
							.uniqueClass(data.jobtype + '-' + data.subtype).appendsText();

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
			case 'DEPLOYMENT_DATA_IMPORT_STATUS':

				var type            = 'Deployment Import';
				var messageCssClass = 'deployment-import';

				if (data.type === 'DEPLOYMENT_DATA_IMPORT_STATUS') {
					type            = 'Data Deployment Import';
					messageCssClass = 'data-deployment-import';
				}

				if (data.subtype === 'BEGIN') {

					var text = type + ' started: ' + new Date(data.start) + '<br>'
							+ 'Importing from: ' + data.source + '<br><br>'
							+ 'Please wait until the import process is finished. Any changes made during a deployment might get lost or conflict with the deployment! This message will be updated during the deployment process.<br><ol class="message-steps"></ol>';

					new MessageBuilder().title(type + ' Progress').uniqueClass(messageCssClass).info(text).requiresConfirmation().updatesText().show();

				} else if (data.subtype === 'PROGRESS') {

					new MessageBuilder().title(type + ' Progress').uniqueClass(messageCssClass).info('<li>' + data.message + '</li>').requiresConfirmation().appendsText('.message-steps').show();

				} else if (data.subtype === 'END') {

					var text = "<br>" + type + " finished: " + new Date(data.end)
							+ "<br>Total duration: " + data.duration
							+ "<br><br>Reload the page to see the new data.";

					new MessageBuilder().title(type + " finished").uniqueClass(messageCssClass).success(text).specialInteractionButton('Reload Page', function() { location.reload(); }, 'Ignore').appendsText().updatesButtons().show();

				}
				break;

			case 'DEPLOYMENT_EXPORT_STATUS':
			case 'DEPLOYMENT_DATA_EXPORT_STATUS':

				var type            = 'Deployment Export';
				var messageCssClass = 'deployment-export';

				if (data.type === 'DEPLOYMENT_DATA_EXPORT_STATUS') {
					type            = 'Data Deployment Export';
					messageCssClass = 'data-deployment-export';
				}

				if (data.subtype === 'BEGIN') {

					var text = type + ' started: ' + new Date(data.start) + '<br>'
							+ 'Exporting to: ' + data.target + '<br><br>'
							+ 'System performance may be affected during Export.<br><ol class="message-steps"></ol>';

					new MessageBuilder().title(type + ' Progress').uniqueClass(messageCssClass).info(text).requiresConfirmation().updatesText().show();

				} else if (data.subtype === 'PROGRESS') {

					new MessageBuilder().title(type + ' Progress').uniqueClass(messageCssClass).info('<li>' + data.message + '</li>').requiresConfirmation().appendsText('.message-steps').show();

				} else if (data.subtype === 'END') {

					var text = '<br>'+ type + ' finished: ' + new Date(data.end)
							+ '<br>Total duration: ' + data.duration;

					new MessageBuilder().title(type + ' finished').uniqueClass(messageCssClass).success(text).appendsText().requiresConfirmation().show();

				}
				break;

			case "SCHEMA_ANALYZE_STATUS":

				if (data.subtype === 'BEGIN') {

					var text = "Schema Analysis started: " + new Date(data.start) + "<br>"
							+ "Please wait until the import process is finished. This message will be updated during the process.<br><ol class='message-steps'></ol>";

					new MessageBuilder().title("Schema Analysis progress").uniqueClass('schema-analysis').info(text).requiresConfirmation().updatesText().show();

				} else if (data.subtype === 'PROGRESS') {

					new MessageBuilder().title("Schema Analysis progress").uniqueClass('schema-analysis').info("<li>" + data.message + "</li>").requiresConfirmation().appendsText('.message-steps').show();

				} else if (data.subtype === 'END') {

					var text = "<br>Schema Analysis finished: " + new Date(data.end)
							+ "<br>Total duration: " + data.duration;

					new MessageBuilder().title("Schema Analysis finished").uniqueClass('schema-analysis').success(text).appendsText().requiresConfirmation().show();

				}
				break;

			case "MAINTENANCE":

				let enabled = data.enabled ? 'enabeld' : 'disabled';

				new MessageBuilder().title('Maintenance Mode ' + enabled).warning("Maintenance Mode has been " + enabled + ". Redirecting...").allowConfirmAll().show();
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
				new MessageBuilder().title('REST Access to \'' + data.uri + '\' denied').warning(data.message).requiresConfirmation().allowConfirmAll().show();
				break;

			case "SCRIPTING_ERROR":
				if (data.nodeId && data.nodeType) {
					Command.get(data.nodeId, 'id,type,name,content,ownerDocument,schemaNode', function (obj) {

						let name     = data.name.slice(data.name.indexOf('_html_') === 0 ? 6 : 0);
						let property = 'Property';
						let title    = '';

						switch (obj.type) {

							case 'SchemaMethod':
								if (obj.schemaNode) {
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

						let location = '<table>'
							+ '<tr><th>Element:</th><td style="padding-left:8px;">' + data.nodeType + '[' + data.nodeId + ']</td></tr>'
							+ '<tr><th>' + property + ':</th><td style="padding-left:8px;">' + name + '</td></tr>'
							+ '<tr><th>Row:</th><td style="padding-left:8px;">' + data.row + '</td></tr>'
							+ '<tr><th>Column:</th><td style="padding-left:8px;">' + data.column + '</td></tr>'
							+ '</table>';

						new MessageBuilder()
							.title('Scripting error in ' + title)
							.warning(location + '<br/>' + data.message)
							.requiresConfirmation()
							.specialInteractionButton('Open in editor', function(btn) {
								switch (data.nodeType) {
									case 'Content':
									case 'Template':
										_Elements.openEditContentDialog(btn, obj, {
											extraKeys: { "Ctrl-Space": "autocomplete" },
											gutters: ["CodeMirror-lint-markers"],
											lint: {
												getAnnotations: function(text, callback) {
													_Code.showScriptErrors(obj, text, callback, data.name);
												},
												async: true
											}
										});
									break;
								default:
									_Entities.showProperties(obj);
									break;
							}
						}, 'Dismiss')
						.allowConfirmAll()
						.show();
					});

				} else {
					new MessageBuilder().title('Server-side Scripting Error').warning(data.message).requiresConfirmation().allowConfirmAll().show();
				}
				break;

			default: {

					var text = "<p>No handler for generic message of type <b>" + data.type + "</b> defined - printing complete message data.</p>";
					Object.keys(data).forEach(function(key) {
						text += "<b>" + key + "</b>: " + data[key] + "<br>";
					});

					new MessageBuilder().title("GENERIC_MESSAGE").warning(text).requiresConfirmation().show();

			}
		}
	},
	fetchHtmlTemplate: function(templateName, templateConfig, callback) {

		Structr.templateCache.registerCallback(templateName, templateName, function(templateHtml, cacheHit) {
			var convertTemplateToLiteral = new Function('config', 'return `' + templateHtml + '`;');
			var parameterizedTemplate = convertTemplateToLiteral(templateConfig);
			callback(parameterizedTemplate, cacheHit);
		});
	},
	activateCommentsInElement: function(elem, defaults) {

		$('[data-comment]', elem).each(function(idx, el) {

			let config = {
				text: $(el).data("comment"),
				element: $(el),
				css: {
					"margin": "0 4px",
					"vertical-align": "top"
				}
			};

			let elCommentConfig = $(el).data('commentConfig') || {};

			// base config is overridden by the defaults parameter which is overriden by the element config
			let infoConfig = Object.assign(config, defaults, elCommentConfig);
			Structr.appendInfoTextToElement(infoConfig);
		});

	},
	blockUiGeneric: function(html, timeout) {
		Structr.loadingSpinnerTimeout = window.setTimeout(function() {

			$.blockUI({
				fadeIn: 0,
				fadeOut: 0,
				message: html,
				forceInput: true,
				css: Structr.defaultBlockUICss
			});
		}, timeout || 0);
	},
	unblockUiGeneric: function() {
		window.clearTimeout(Structr.loadingSpinnerTimeout);
		Structr.loadingSpinnerTimeout = undefined;

		$.unblockUI({
			fadeOut: 0
		});
	},
	showLoadingSpinner: function() {
		Structr.blockUiGeneric('<div id="structr-loading-spinner"><img src="' + _Icons.getSpinnerImageAsData() + '"></div>');
	},
	hideLoadingSpinner: function() {
		Structr.unblockUiGeneric();
	},
	showLoadingMessage: function(title, text, timeout) {

		var messageTitle = title || 'Executing Task';
		var messageText  = text || 'Please wait until the operation has finished...';

		$('#tempInfoBox .infoMsg').html('<img src="' + _Icons.getSpinnerImageAsData() + '"> <b>' + messageTitle + '</b><br><br>' + messageText);

		$('#tempInfoBox .closeButton').hide();
		Structr.blockUiGeneric($('#tempInfoBox'), timeout || 500);
	},
	hideLoadingMessage: function() {
		Structr.unblockUiGeneric();
	},

	nonBlockUIBlockerId: 'non-block-ui-blocker',
	nonBlockUIBlockerContentId: 'non-block-ui-blocker-content',
	showNonBlockUILoadingMessage: function(title, text) {

		var messageTitle = title || 'Executing Task';
		var messageText  = text || 'Please wait until the operation has finished...';

		let pageBlockerDiv = $('<div id="' + Structr.nonBlockUIBlockerId +'"></div>');

		let messageDiv = $('<div id="' + Structr.nonBlockUIBlockerContentId +'"></div>');
		messageDiv.html('<img src="' + _Icons.getSpinnerImageAsData() + '"> <b>' + messageTitle + '</b><br><br>' + messageText);

		$('body').append(pageBlockerDiv);
		$('body').append(messageDiv);
	},
	hideNonBlockUILoadingMessage: function() {
		$('#' + Structr.nonBlockUIBlockerId).remove();
		$('#' + Structr.nonBlockUIBlockerContentId).remove();
	},

	getCodeMirrorSettings: function(baseConfig) {

		let savedSettings = LSWrapper.getItem(Structr.keyCodeMirrorSettings) || {};

		if (baseConfig) {
			return Object.assign(baseConfig, savedSettings);
		}

		return savedSettings;
	},

	updateCodeMirrorOptionGlobally: function(optionName, value) {

		let codeMirrorSettings = Structr.getCodeMirrorSettings();

		$('.CodeMirror').each(function(idx, cmEl) {
			cmEl.CodeMirror.setOption(optionName, value);
			codeMirrorSettings[optionName] = value;
			cmEl.CodeMirror.refresh();
		});

		LSWrapper.setItem(Structr.keyCodeMirrorSettings, codeMirrorSettings);
	},
	getDocumentationURLForTopic: function (topic) {
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
			case 'crawler':
			case 'mail-templates':
			case 'virtual-types':
			case 'localization':
			case 'graph':
			default:
				return 'https://docs.structr.com/';
		}
	},
	getShadowPage: function(callback) {

		if (shadowPage) {

			if (callback) {
				callback();
			}

		} else {

			// wrap getter for shadowdocument in listComponents so we're sure that shadow document has been created
			Command.listComponents(1, 1, 'name', 'asc', function(result) {
				Command.getByType('ShadowDocument', 1, 1, null, null, null, true, function(entities) {
					shadowPage = entities[0];

					if (callback) {
						callback();
					}
				});
			});
		}
	}
};

var _TreeHelper = {
	initTree: function(tree, initFunction, stateKey) {
		$(tree).jstree({
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
	makeDroppable: function(tree, list) {
		window.setTimeout(function() {
			list.forEach(function(obj) {
				// only load data necessary for dnd. prevent from loading the complete folder (with its files)
				Command.get(obj.id, 'id,type,isFolder', function(data) {
					StructrModel.createFromData(data, null, false);
					_TreeHelper.makeTreeElementDroppable(tree, obj.id);
				});
			});
		}, 500);
	},
	makeTreeElementDroppable: function(tree, id) {
		var el = $('#' + id + ' > .jstree-wholerow', tree);
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

		var uniqueMessageAlreadyPresented = false;

		if (this.params.uniqueClass) {
			// find existing one
			var existingMsgBuilder = $('#info-area .message.' + this.params.uniqueClass).data('msgbuilder');
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

					var selector = '#info-area .message.' + this.params.uniqueClass + ' .text';
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

			$('#info-area').append(
				'<div class="' + this.params.classNames.join(' ') +  '" id="' + this.params.msgId + '">' +
					(this.params.title ? '<h3 class="title">' + this.params.title + this.getUniqueCountElement() + '</h3>' : this.getUniqueCountElement()) +
					'<div class="text">' + this.params.text + '</div>' +
					(this.params.furtherText ? '<div class="furtherText">' + this.params.furtherText + '</div>' : '') +
					'<div class="message-buttons">' + this.getButtonHtml() + '</div>' +
				'</div>'
			);

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

function isImage(contentType) {
	return (contentType && contentType.indexOf('image') > -1);
}

function isVideo(contentType) {
	return (contentType && contentType.indexOf('video') > -1);
}

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
			LSWrapper.setItem(lastMenuEntryKey, lastMenuEntry);
		}

		// Remove dialog data in case of page reload
		LSWrapper.removeItem(dialogDataKey);
		LSWrapper.save();
	}
});