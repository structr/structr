/*
 * Copyright (C) 2010-2019 Structr GmbH
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
var header, main, footer;
var sessionId, user;
var lastMenuEntry, menuBlocked;
var dmp;
var editorCursor, ignoreKeyUp;
var dialog, isMax = false;
var dialogBox, dialogMsg, dialogBtn, dialogTitle, dialogMeta, dialogText, dialogHead, dialogCancelButton, dialogSaveButton, saveAndClose, loginBox, dialogCloseButton;
var dialogId;
var pagerType = {}, page = {}, pageSize = {}, sortKey = {}, sortOrder = {}, pagerFilters = {};
var dialogMaximizedKey = 'structrDialogMaximized_' + port;
var expandedIdsKey = 'structrTreeExpandedIds_' + port;
var lastMenuEntryKey = 'structrLastMenuEntry_' + port;
var pagerDataKey = 'structrPagerData_' + port + '_';
var autoRefreshDisabledKey = 'structrAutoRefreshDisabled_' + port;
var detailsObjectIdKey = 'structrDetailsObjectId_' + port;
var dialogDataKey = 'structrDialogData_' + port;
var dialogHtmlKey = 'structrDialogHtml_' + port;
var scrollInfoKey = 'structrScrollInfoKey_' + port;
var consoleModeKey = 'structrConsoleModeKey_' + port;
var resizeFunction;
var altKey = false, ctrlKey = false, shiftKey = false, eKey = false, cmdKey = false;

$(function() {

	header = $('#header');
	main = $('#main');
	footer = $('#footer');
	loginBox = $('#login');

	_Logger.initLogger(urlParam('debug'), urlParam('events'));

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

	$(window).on('hashchange', function(e) {
		var anchor = getAnchorFromUrl(window.location.href);
		if (anchor === 'logout' || loginBox.is(':visible')) return;
		Structr.requestActivateModule(e, anchor);
	});

	$(document).on('mouseenter', '[data-toggle="popup"]', function() {
		var target = $(this).data("target");
		$(target).addClass('visible');
	});

	$(document).on('mouseleave', '[data-toggle="popup"]', function() {
		var target = $(this).data("target");
		$(target).removeClass('visible');
	});

	$(document).on('click', '[data-activate-module]', function(e) {
		var module = $(this).data('activateModule');
		_Logger.log(_LogType.INIT, 'Activating module ' + module);
		Structr.requestActivateModule(e, module);
	});

	Structr.connect();

	// Reset keys in case of window switching
	$(window).blur(function(e) {
		altKey = false, ctrlKey = false, shiftKey = false, eKey = false, cmdKey = false;
	});

	$(window).focus(function(e) {
		altKey = false, ctrlKey = false, shiftKey = false, eKey = false, cmdKey = false;
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
		if (navigator.platform === 'MacIntel' && k === 91) {
			cmdKey = false;
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
		if (navigator.platform === 'MacIntel' && k === 91) {
			cmdKey = true;
		}
		if ((e.ctrlKey && (e.which === 83)) || (navigator.platform === 'MacIntel' && cmdKey && (e.which === 83))) {
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
					Command.get(uuid, null, function(obj) {
						_Entities.showProperties(obj);
					});
				} else {
					alert('That does not look like a UUID! length != 32');
				}
			}
		}
        // Ctrl-Alt-g
        if (k === 71 && altKey && ctrlKey) {
            e.preventDefault();
            var uuid = prompt('Enter the UUID for which you want to open the access control dialog');
            if (uuid) {
                if (uuid.length === 32) {
                    Command.get(uuid, null, function(obj) {
                        _Entities.showAccessControlDialog(obj);
                    });
                } else {
                    alert('That does not look like a UUID! length != 32');
                }
            }
        }
		// Ctrl-Alt-h
		if (k === 72 && altKey && ctrlKey) {
			e.preventDefault();
			if ("schema" === Structr.getActiveModuleName()) {
				_Schema.hideSelectedSchemaTypes();
			}
		}
		// Ctrl-Alt-s
		if (k === 83 && altKey && ctrlKey) {
			e.preventDefault();
			Structr.dialog('Refactoring helper');
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
	edition: '',
	classes: [],
	expanded: {},
	msgCount: 0,
	currentlyActiveSortable: undefined,
	loadingSpinnerTimeout: undefined,
	templateCache: new AsyncObjectCache(function(templateName) {

		Promise.resolve($.ajax('templates/' + templateName + '.html')).then(function(templateHtml) {
			Structr.templateCache.addObject(templateHtml, templateName);
		}).catch(function(e) {
			console.log(e.statusText, templateName, e);
		});

	}),

	reconnect: function() {
		_Logger.log(_LogType.INIT, 'deactivated ping');
		Structr.stopPing();
		_Logger.log(_LogType.INIT, 'activating reconnect loop');
		Structr.stopReconnect();
		reconn = window.setInterval(function() {
			wsConnect();
		}, 1000);
		wsConnect();
		_Logger.log(_LogType.INIT, 'activated reconnect loop', reconn);
	},
	stopReconnect: function() {
		if (reconn) {
			window.clearInterval(reconn);
			reconn = undefined;
			user = undefined;
		}
	},
	init: function() {
		_Logger.log(_LogType.INIT, '###################### Initialize UI ####################');
		$('#errorText').empty();
		_Logger.log(_LogType.INIT, 'user', user);
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
		}
	},
	refreshUi: function() {
		Structr.showLoadingSpinner();

		Structr.clearMain();
		Structr.loadInitialModule(false, function() {
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
		_Logger.log(_LogType.INIT, 'Starting PING');
		Structr.stopPing();
		_Logger.log(_LogType.INIT, 'ping', ping);
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
		_Logger.log(_LogType.INIT, 'connect');
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

			$.blockUI.defaults.overlayCSS.opacity = .6;
			$.blockUI.defaults.applyPlatformOpacityRules = false;
			$.blockUI({
				fadeIn: 25,
				fadeOut: 25,
				message: loginBox,
				forceInput: true,
				css: {
					border: 'none',
					backgroundColor: 'transparent'
				}
			});
		}

		$('#logout_').html('Login');
		if (text) {
			$('#errorText').html(text);
			$('#errorText-two-factor').html(text);
		}

		//Structr.activateMenuEntry('logout');
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
		Structr.saveLocalStorage();
		if (Command.logout(user)) {
			Cookies.remove('JSESSIONID');
			sessionId.length = 0;
			LSWrapper.clear();
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
			if (typeof callback === "function") {
				callback();
			}
		});
	},
	loadInitialModule: function(isLogin, callback) {

		Structr.restoreLocalStorage(function() {

			Structr.expanded = JSON.parse(LSWrapper.getItem(expandedIdsKey));
			_Logger.log(_LogType.INIT, '######## Expanded IDs after reload ##########', Structr.expanded);

			var browserUrl = window.location.href;
			var anchor = getAnchorFromUrl(browserUrl);
			lastMenuEntry = ((!isLogin && anchor && anchor !== 'logout') ? anchor : Structr.getActiveModuleName());
			if (!lastMenuEntry) {
				lastMenuEntry = Structr.getActiveModuleName() || 'dashboard';
			} else {
				_Logger.log(_LogType.INIT, 'Last menu entry found: ' + lastMenuEntry);
			}
			_Logger.log(_LogType.INIT, 'lastMenuEntry', lastMenuEntry);
			Structr.doActivateModule(lastMenuEntry);
			Structr.updateVersionInfo();

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
		$.blockUI.defaults.overlayCSS.opacity = .6;
		$.blockUI.defaults.applyPlatformOpacityRules = false;
		$.blockUI({
			fadeIn: 25,
			fadeOut: 25,
			message: $('#confirmation'),
			css: {
				border: 'none',
				backgroundColor: 'transparent'
			}
		});

	},
	saveLocalStorage: function(callback) {
		_Logger.log(_LogType.INIT, "Saving localstorage");
		Command.saveLocalStorage(callback);
	},
	restoreLocalStorage: function(callback) {
		if (!LSWrapper.isLoaded()) {
			_Logger.log(_LogType.INIT, "Restoring localstorage");
			Command.getLocalStorage(callback);
		} else {
			callback();
		}
	},
	restoreDialog: function(dialogData) {
		_Logger.log(_LogType.INIT, 'restoreDialog', dialogData, dialogBox);
		$.blockUI.defaults.overlayCSS.opacity = .6;
		$.blockUI.defaults.applyPlatformOpacityRules = false;

		window.setTimeout(function() {

			Structr.blockUI(dialogData);
			Structr.resize();

		}, 1000);

	},
	dialog: function(text, callbackOk, callbackCancel) {

		if (browser) {

			dialogHead.empty();
			dialogText.empty();
			dialogMsg.empty();
			dialogMeta.empty();
			dialogBtn.empty();

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
				if (searchField) {
					searchField.focus();
				}

				LSWrapper.removeItem(dialogDataKey);

				if (callbackCancel) {
					window.setTimeout(function() {
						callbackCancel();
					}, 100);
				}
			});

			$.blockUI.defaults.overlayCSS.opacity = .4;
			$.blockUI.defaults.applyPlatformOpacityRules = false;

			var dimensions = Structr.getDialogDimensions(24, 24);
			Structr.blockUI(dimensions);

			Structr.resize();

			dimensions.text = text;
			_Logger.log(_LogType.INIT, 'Open dialog', dialog, dimensions, callbackOk, callbackCancel);
			LSWrapper.setItem(dialogDataKey, JSON.stringify(dimensions));

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
			css: {
				cursor: 'default',
				border: 'none',
				backgroundColor: 'transparent',
				width: dimensions.width + 'px',
				height: dimensions.height + 'px',
				top: dimensions.top + 'px',
				left: dimensions.left + 'px'
			},
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
		Structr.setSize($(window).width(), $(window).height(), $(window).width() - 24, $(window).height() - 24);

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
			if (searchField)
				searchField.focus();
		});
		$.blockUI.defaults.overlayCSS.opacity = .6;
		$.blockUI.defaults.applyPlatformOpacityRules = false;
		$.blockUI({
			message: $('#tempInfoBox'),
			css: {
				border: 'none',
				backgroundColor: 'transparent'
			}
		});
	},
	reconnectDialog: function(text) {
		if (text) {
			$('#tempErrorBox .errorText').html('<i class="' + _Icons.getFullSpriteClass(_Icons.error_icon) + '" /> ' + text);
		}
		$('#tempErrorBox .closeButton').hide();
		$.blockUI.defaults.overlayCSS.opacity = .6;
		$.blockUI.defaults.applyPlatformOpacityRules = false;
		$.blockUI({
			message: $('#tempErrorBox'),
			css: {
				border: 'none',
				backgroundColor: 'transparent'
			}
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
			Structr.doActivateModule(name);
		}
	},
	doActivateModule: function(name) {
		if (Structr.modules[name]) {
			var activeModule = Structr.modules[Structr.getActiveModuleName()];
			if (activeModule && activeModule.unload) {
				activeModule.unload();
			}
			Structr.clearMain();
			Structr.activateMenuEntry(name);
			Structr.modules[name].onload();
			Structr.adaptUiToAvailableFeatures();
		} else {
			_Logger.log(_LogType.INIT, 'Module ' + name + ' does not exist.');
			Structr.unblockMenu();
		}
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
			_Logger.log(_LogType.INIT, 'Module ' + name + ' registered');
		} else {
			new MessageBuilder().error("Cannot register module '" + name + "' a second time - ignoring attempt.").show();
		}
	},
	getActiveModuleName: function() {
		return LSWrapper.getItem(lastMenuEntryKey);
	},
	getActiveModule: function() {
		return Structr.modules[Structr.getActiveModuleName()];
	},
	isModuleActive: function(module) {
		return (module._moduleName === Structr.getActiveModuleName());
	},
	containsNodes: function(element) {
		_Logger.log(_LogType.INIT, element, Structr.numberOfNodes(element), Structr.numberOfNodes(element) > 0);
		return (element && Structr.numberOfNodes(element) && Structr.numberOfNodes(element) > 0);
	},
	numberOfNodes: function(element, excludeId) {
		var childNodes = $(element).children('.node');
		if (excludeId) {
			childNodes = childNodes.not('.' + excludeId + '_');
		}
		var n = childNodes.length;
		_Logger.log(_LogType.INIT, 'children', $(element).children('.node'));
		_Logger.log(_LogType.INIT, 'number of nodes in element', element, n);
		return n;
	},
	findParent: function(parentId, componentId, pageId, defaultElement) {
		var parent = Structr.node(parentId, null, componentId, pageId);
		_Logger.log(_LogType.INIT, 'findParent', parentId, componentId, pageId, defaultElement, parent);
		_Logger.log(_LogType.INIT, 'findParent: parent element from Structr.node: ', parent);
		if (!parent)
			parent = defaultElement;
		_Logger.log(_LogType.INIT, 'findParent: final parent element: ', parent);
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
		_Logger.log(_LogType.INIT, Structr.classes);
		$(Structr.classes).each(function(i, cls) {
			_Logger.log(_LogType.INIT, 'testing class', cls);
			if (el && $(el).hasClass(cls)) {
				c = cls;
				_Logger.log(_LogType.INIT, 'found class', cls);
			}
		});
		return c;
	},
	entityFromElement: function(element) {
		_Logger.log(_LogType.INIT, element);

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
			_Logger.log(_LogType.INIT, 'exception:', err.toString());
		}

		$('#pages_').droppable({
			accept: '.element, .content, .component, .file, .image, .widget',
			greedy: true,
			hoverClass: 'nodeHover',
			tolerance: 'pointer',
			over: function(e, ui) {

				e.stopPropagation();
				$('a#pages_').droppable('disable');
				_Logger.log(_LogType.INIT, 'over is off');

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
	openSlideOut: function(slideout, tab, activeTabKey, callback) {

		var storedRightSlideoutWidth = LSWrapper.getItem(_Pages.rightSlideoutWidthKey);
		var rsw = storedRightSlideoutWidth ? parseInt(storedRightSlideoutWidth) : (slideout.width() + 12);

		slideout.css({
			width: rsw - 12 + 'px'
		});

		slideout.addClass('open');
		var t = $(tab);
		t.addClass('active');
		slideout.animate({right: 0 + 'px'}, {duration: 100}).zIndex(1);

		$('.node', slideout).width(rsw - 25);

		LSWrapper.setItem(activeTabKey, t.prop('id'));
		if (callback) {
			callback();
		}
		_Pages.resize(0, rsw);
	},
	closeSlideOuts: function(slideouts, activeTabKey) {
		var storedRightSlideoutWidth = LSWrapper.getItem(_Pages.rightSlideoutWidthKey);
		var rsw = 0;

		var wasOpen = false;
		slideouts.forEach(function(slideout) {
			slideout.removeClass('open');
			var l = slideout.position().left;
			if (Math.abs(l - $(window).width()) >= 3) {
				wasOpen = true;
				rsw = storedRightSlideoutWidth ? parseInt(storedRightSlideoutWidth) : (slideout.width() + 12);
				slideout.animate({right: '-=' + rsw + 'px'}, {duration: 100}).zIndex(2);
				$('.compTab.active', slideout).removeClass('active');

				var openSlideoutCallback = slideout.data('closeCallback');
				if (typeof openSlideoutCallback === "function") {
					openSlideoutCallback();
				}
			}
		});
		if (wasOpen) {
			_Pages.resize(0, -rsw);
		}

		LSWrapper.removeItem(activeTabKey);
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
				callback({sw: slideoutWidth});
			}
		}).zIndex(1);
		slideoutElement.addClass('open');
		t.draggable({
			axis: 'x',
			start: function(e, ui) {
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
				$('.node.page', slideoutElement).width(w - 25);

				if (typeof callback === 'function') {
					LSWrapper.setItem(_Pages.leftSlideoutWidthKey, slideoutElement.width());
					callback({sw: (slideoutWidth - oldLeftSlideoutWidth)});
				}
			},
			stop: function(e, ui) {
				LSWrapper.setItem(_Pages.leftSlideoutWidthKey, slideoutElement.width());
				t.css({
					left: "",
					top: ""
				});
			}
		});
	},
	closeLeftSlideOuts: function(slideouts, activeTabKey, callback) {
		var wasOpen = false;
		var osw;
		slideouts.forEach(function(w) {
			var s = $(w);
			s.removeClass('open');
			var l = s.position().left;
			var sw = s.width() + 12;
			if (Math.abs(l) <= 3) {
				wasOpen = true;
				osw = sw;
				s.animate({left: - sw -1 + 'px'}, 100, function() {
					if (typeof callback === 'function') {
						callback(wasOpen, -osw, 0);
					}
				}).zIndex(2);
				$('.compTab.active', s).removeClass('active').draggable("destroy");
			}
		});
		LSWrapper.removeItem(activeTabKey);
	},
	updateVersionInfo: function() {
		$.get(rootUrl + '_env', function(data) {
			if (data && data.result) {

				var envInfo = data.result;

				$('#header .structr-instance-name').text(envInfo.instanceName);
				$('#header .structr-instance-stage').text(envInfo.instanceStage);

				var ui = envInfo.components['structr-ui'];
				if (ui) {

					var version = ui.version;
					var build = ui.build;
					var date = ui.date;
					var versionLink = 'https://structr.org/download';
					var versionInfo = '<a target="_blank" href="' + versionLink + '">' + version + '</a>';
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

				var hamburger = $('#menu li.submenu-trigger');
				var subMenu = $('#submenu');
				envInfo.mainMenu.forEach(function(entry) {
					$('li[data-name="' + entry + '"]', subMenu).insertBefore(hamburger);
				});

				Structr.activeModules = envInfo.modules;
				Structr.adaptUiToAvailableFeatures();
			}
		});
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
		_Logger.log(_LogType.INIT, 'addExpandedNode', id);

		if (id) {
			var alreadyStored = Structr.getExpanded()[id];
			if (!alreadyStored) {

				Structr.getExpanded()[id] = true;
				LSWrapper.setItem(expandedIdsKey, JSON.stringify(Structr.expanded));

			}
		}
	},
	removeExpandedNode: function(id) {
		_Logger.log(_LogType.INIT, 'removeExpandedNode', id);

		if (id) {
			delete Structr.getExpanded()[id];
			LSWrapper.setItem(expandedIdsKey, JSON.stringify(Structr.expanded));
		}
	},
	isExpanded: function(id) {
		_Logger.log(_LogType.INIT, 'id, Structr.getExpanded()[id]', id, Structr.getExpanded()[id]);

		if (id) {
			var isExpanded = (Structr.getExpanded()[id] === true) ? true : false;

			_Logger.log(_LogType.INIT, isExpanded);

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
	initVerticalSlider: function(sliderEl, localstorageKey, minWidth, dragCallback) {

		if (typeof dragCallback !== "function") {
			console.error('dragCallback is not a function!');
			return;
		}

		$(sliderEl).draggable({
			axis: 'x',
			drag: function(e, ui) {
				var left = Math.max(minWidth, ui.position.left);
				ui.position.left = left;

				dragCallback(left);
			},
			stop: function(e, ui) {
				LSWrapper.setItem(localstorageKey, ui.position.left);
			}
		});

	},
	appendInfoTextToElement: function(config) {

		var element            = config.element;
		var appendToElement    = config.appendToElement || element;
		var text               = config.text || 'No text supplied!';
		var toggleElementCss   = config.css || {};
		var toggleElementClass = config.class || undefined;
		var elementCss         = config.elementCss || {};
		var helpElementCss     = config.helpElementCss || {};
		var customToggleIcon   = config.customToggleIcon || _Icons.information_icon;
		var insertAfter        = config.insertAfter || false;
		var offsetX            = config.offsetX || 0;
		var offsetY            = config.offsetY || 0;

		var customToggleElement = true;
		var toggleElement = config.toggleElement;
		if (!toggleElement) {
			customToggleElement = false;
			toggleElement = $('<span><i class="' + _Icons.getFullSpriteClass(customToggleIcon) + '"></span>');
		}

		if (toggleElementClass) {
			toggleElement.addClass(toggleElementClass);
		}
		toggleElement.css(toggleElementCss);
		appendToElement.css(elementCss);

		var helpElement = $('<span class="context-help-text">' + text + '</span>');

		toggleElement
				.on("mousemove", function(e) {
					helpElement.show();
					helpElement.css({
						left: e.clientX + 20 + offsetX,
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

		switch (data.type) {

			case "CSV_IMPORT_STATUS":

				if (me.username === data.username) {

					var titles = {
						BEGIN: 'CSV Import started',
						CHUNK: 'CSV Import status',
						END:   'CSV Import finished'
					};

					var texts = {
						BEGIN: 'Started importing CSV data',
						CHUNK: 'Finished importing chunk ' + data.currentChunkNo + ' / ' + data.totalChunkNo,
						END:   'Finished importing CSV data (Time: ' + data.duration + ')'
					};

					new MessageBuilder().title(titles[data.subtype]).info(texts[data.subtype]).uniqueClass('csv-import-status').updatesText().requiresConfirmation().allowConfirmAll().show();
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

				if (me.username === data.username) {

					var msg = new MessageBuilder()
							.title(data.jobtype + ' ' + fileImportTitles[data.subtype])
							.info(fileImportTexts[data.subtype])
							.uniqueClass(data.jobtype + '-import-status-' + data.filepath);

					if (data.subtype !== 'QUEUED') {
						msg.updatesText().requiresConfirmation().allowConfirmAll();
					}

					msg.show();

					if (Structr.isModuleActive(Importer)) {
						Importer.updateJobTable();
					}
				}
				break;

			case "FILE_IMPORT_EXCEPTION":

				if (me.username === data.username) {

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

					if (Structr.isModuleActive(Importer)) {
						Importer.updateJobTable();
					}
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
					BEGIN: 'Started script job #' + data.jobId + ((data.jobName.length === 0) ? '' : '<br>' + data.jobName),
					END: 'Finished script job #' + data.jobId + ((data.jobName.length === 0) ? '' : '<br>' + data.jobName)
				};

				if (me.username === data.username) {

					var msg = new MessageBuilder()
							.title(scriptJobTitles[data.subtype])
							.info(scriptJobTexts[data.subtype])
							.uniqueClass(data.jobtype + '-status-' + data.jobId);

					if (data.subtype !== 'QUEUED') {
						msg.updatesText().requiresConfirmation().allowConfirmAll();
					}

					msg.show();

					if (Structr.isModuleActive(Importer)) {
						Importer.updateJobTable();
					}
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

					new MessageBuilder().title(type + " finished").uniqueClass(messageCssClass).info(text).specialInteractionButton('Reload Page', function() { location.reload(); }, 'Ignore').appendsText().updatesButtons().show();

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

					new MessageBuilder().title(type + ' finished').uniqueClass(messageCssClass).info(text).appendsText().requiresConfirmation().show();

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

					new MessageBuilder().title("Schema Analysis finished").uniqueClass('schema-analysis').info(text).appendsText().requiresConfirmation().show();

				}
				break;

			case "WARNING":
				new MessageBuilder().title(data.title).warning(data.message).requiresConfirmation().allowConfirmAll().show();
				break;

			case "SCRIPT_JOB_EXCEPTION":
				new MessageBuilder().title('Exception in Scheduled Job').warning(data.message).requiresConfirmation().allowConfirmAll().show();
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
			$.blockUI.defaults.overlayCSS.opacity = .2;
			$.blockUI.defaults.applyPlatformOpacityRules = false;
			$.blockUI({
				fadeIn: 0,
				fadeOut: 0,
				message: html,
				forceInput: true,
				css: {
					border: 'none',
					backgroundColor: 'transparent'
				}
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
	}
};

var _TreeHelper = {
	initTree: function(tree, initFunction, stateKey) {
		$(tree).jstree({
			plugins: ["themes", "dnd", "search", "state", "types", "wholerow"],
			core: {
				animation: 0,
				state: {
					key: stateKey
				},
				async: true,
				data: initFunction
			}
		});
	},
	deepOpen: function(tree, element, parentElements, parentKey, selectedNode) {
		if (element && element.id) {

			parentElements = parentElements || [];
			parentElements.unshift(element);

			Command.get(element.id, parentKey, function(loadedElement) {
				if (loadedElement && loadedElement[parentKey]) {
					_TreeHelper.deepOpen(tree, loadedElement[parentKey], parentElements, selectedNode);
				} else {
					_TreeHelper.open(tree, parentElements, selectedNode);
				}
			});
		}
	},
	open: function(tree, dirs, selectedNode) {
		if (dirs.length) {
			var d = dirs.shift();
			tree.jstree('deselect_node', d.id);
			tree.jstree('open_node', d.id, function() {
				tree.jstree('select_node', selectedNode);
			});
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
	makeDroppable: function(tree, list) {
		window.setTimeout(function() {
			list.forEach(function(obj) {
				StructrModel.create({id: obj.id}, null, false);
				_TreeHelper.makeTreeElementDroppable(tree, obj.id);
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

$(window).on('beforeunload', function(event) {
	if (event.target === document) {
		_Logger.log(_LogType.INIT, '########################################### unload #####################################################');
		// Remove dialog data in case of page reload
		LSWrapper.removeItem(dialogDataKey);
		Structr.saveLocalStorage();
	}
});