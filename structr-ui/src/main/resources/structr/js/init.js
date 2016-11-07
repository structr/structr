/*
 * Copyright (C) 2010-2016 Structr GmbH
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
var lastMenuEntry, activeTab, menuBlocked;
var dmp;
var editorCursor, ignoreKeyUp;
var dialog, isMax = false;
var dialogBox, dialogMsg, dialogBtn, dialogTitle, dialogMeta, dialogText, dialogCancelButton, dialogSaveButton, saveAndClose, loginButton, loginBox, dialogCloseButton;
var dialogId;
var pagerType = {}, page = {}, pageSize = {}, sortKey = {}, sortOrder = {}, pagerFilters = {};
var dialogMaximizedKey = 'structrDialogMaximized_' + port;
var expandedIdsKey = 'structrTreeExpandedIds_' + port;
var lastMenuEntryKey = 'structrLastMenuEntry_' + port;
var pagerDataKey = 'structrPagerData_' + port + '_';
var autoRefreshDisabledKey = 'structrAutoRefreshDisabled_' + port;
var detailsObjectId = 'structrDetailsObjectId_' + port;
var dialogDataKey = 'structrDialogData_' + port;
var dialogHtmlKey = 'structrDialogHtml_' + port;
var pushConfigKey = 'structrPushConfigKey_' + port;
var scrollInfoKey = 'structrScrollInfoKey_' + port;
var hideInactiveTabsKey = 'structrHideInactiveTabs_' + port;
var autoHideInactiveTabsKey = 'structrAutoHideInactiveTabs_' + port;

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
	loginButton        = $('#loginButton');

	$('#import_json').on('click', function(e) {
		e.stopPropagation();
		var jsonArray = $.parseJSON($('#json_input').val());
		$(jsonArray).each(function(i, json) {
			createEntity(json);
		});
	});

	loginButton.on('click', function(e) {
		e.stopPropagation();
		var username = $('#usernameField').val();
		var password = $('#passwordField').val();
		Structr.doLogin(username, password);
		return false;
	});
	$('#logout_').on('click', function(e) {
		e.stopPropagation();
		Structr.doLogout();
	});

	$('[data-activate-module]').on('click', function(e) {
		var module = $(this).data('activateModule');
		_Logger.log(_LogType.INIT, 'Activating module ' + module);
		Structr.activateModule(e, module);
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
		if (k === 16)
			shiftKey = false;
		if (k === 18)
			altKey = false;
		if (k === 17)
			ctrlKey = false;
		if (k === 69)
			eKey = false;
		if (navigator.platform === 'MacIntel' && k === 91)
			cmdKey = false;

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
		if (navigator.platform === 'MacIntel' && k === 91)
			cmdKey = true;
		if ((e.ctrlKey && (e.which === 83)) || (navigator.platform === 'MacIntel' && cmdKey && (e.which === 83))) {
			e.preventDefault();
			if (dialogSaveButton && dialogSaveButton.length && dialogSaveButton.is(':visible') && !dialogSaveButton.prop('disabled')) {
				dialogSaveButton.click();
			}
		}
		if (k === 67 && altKey && ctrlKey) {
			e.preventDefault();
			_Console.toggleConsole();
		}
		//console.log(e.which, shiftKey, ctrlKey, altKey, eKey, cmdKey);
	});

	$(window).on('resize', function() {
		Structr.resize();
	});
	dmp = new diff_match_patch();
});

var _Icons = {
	add_icon: 'icon/add.png',
	delete_icon: 'icon/delete.png',
	delete_disabled_icon: 'icon/delete_gray.png',
	delete_brick_icon: 'icon/brick_delete.png',
	edit_icon: 'icon/pencil.png',
	edit_ui_properties_icon: 'icon/wrench.png',
	collapsed_icon: 'icon/tree_arrow_right.png',
	expanded_icon: 'icon/tree_arrow_down.png',
	link_icon: 'icon/link.png',
	key_icon: 'icon/key.png',
	key_add_icon: 'icon/key_add.png',
	cross_icon: 'icon/cross.png',
	tick_icon: 'icon/tick.png',
	grey_cross_icon: 'icon/cross_small_grey.png',
	page_white_stack_icon: 'icon/page_white_stack.png',
	eye_icon: 'icon/eye.png',
	database_icon: 'icon/database.png',
	database_table_icon: 'icon/database_table.png',
	database_add_icon: 'icon/database_add.png',
	view_detail_icon: 'icon/application_view_detail.png',
	calendar_icon: 'icon/calendar.png',
	add_grey_icon: 'icon/add_grey.png',
	page_icon: 'icon/page.png',
	page_white_icon: 'icon/page_white.png',
	button_icon: 'icon/button.png',
	clone_icon: 'icon/page_copy.png',
	group_icon: 'icon/group.png',
	group_add_icon: 'icon/group_add.png',
	user_icon: 'icon/user.png',
	user_add_icon: 'icon/user_add.png',
	user_delete_icon: 'icon/user_delete.png',
	compress_icon: 'icon/compress.png',
	accept_icon: 'icon/accept.png',
	push_file_icon: 'icon/page_white_get.png',
	pull_file_icon: 'icon/page_white_put.png',
	exec_cypher_icon: 'icon/control_play_blue.png',
	exec_rest_icon: 'icon/control_play.png',
	arrow_undo_icon: 'icon/arrow_undo.png',
	information_icon: 'icon/information.png',
	refresh_icon: 'icon/arrow_refresh.png',
	error_icon: 'icon/error.png',
	pull_page_icon: 'icon/pull_page.png',
	wand_icon: 'icon/wand.png',
	toggle_icon: 'icon/arrow_switch.png',
	widget_icon: 'icon/layout.png',
	folder_icon: 'icon/folder.png',
	add_folder_icon: 'icon/folder_add.png',
	add_widget_icon: 'icon/layout_add.png',
	content_icon: 'icon/page_white.png',
	active_content_icon: 'icon/page_yellow.png',
	delete_content_icon: 'icon/page_white_delete.png',
	template_icon: 'icon/layout_content.png',
	active_template_icon: 'icon/layout_yellow.png',
	comp_templ_icon: 'icon/layout_yellow.png',
	icon_shared_template: 'icon/layout_yellow.png',
	comment_icon: 'icon/comment.png',
	repeater_icon: 'icon/bricks.png',
	brick_icon: 'icon/brick.png',
	comp_icon: 'icon/brick_yellow.png',
	microphone_icon: 'img/icon_microphone.svg',
	add_file_icon: 'icon/page_white_add.png',
	add_site_icon: 'icon/page_white_add.png',
	add_page_icon: 'icon/page_add.png',
	ajax_loader_1: 'img/ajax-loader.gif',
	ajax_loader_2: 'img/al.gif',
	structr_logo_small: 'icon/structr_icon_16x16.png',
	minification_dialog_js_icon: 'icon/script_lightning.png',
	minification_dialog_css_icon: 'icon/script_palette.png',
	minification_trigger_icon: 'icon/briefcase.png',
	search_icon: 'icon/zoom.png',
	star_icon: 'icon/star.png',
	star_delete_icon: 'icon/star_delete.png',
	image_icon: 'icon/image.png',
	arrow_up_down: 'icon/arrow_up_down.png'
};

var Structr = {
	modules: {},
	activeModules: {},
	classes: [],
	expanded: {},
	msgCount: 0,
	hideInactiveTabs: false,
	autoHideInactiveTabs: undefined,

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
		showLoadingSpinner();

		Structr.clearMain();
		Structr.loadInitialModule();
		Structr.startPing();
		if (!dialogText.text().length) {
			LSWrapper.removeItem(dialogDataKey);
		} else {
			var dialogData = JSON.parse(LSWrapper.getItem(dialogDataKey));
			//console.log('Dialog data after init', dialogData, dialogText.text().length);
			if (dialogData) {
				Structr.restoreDialog(dialogData);
			}
		}
		hideLoadingSpinner();
		_Console.initConsole();
	},
	updateUsername:function(name) {
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
		}

		Structr.activateMenuEntry('logout');
	},
	doLogin: function(username, password) {
		Structr.renewSessionId(function () {
			Command.login(username, password);
		});
	},
	doLogout: function(text) {
		_Console.logoutAction();
		Structr.saveLocalStorage();
		if (Command.logout(user)) {
			Cookies.remove('JSESSIONID');
			sessionId.length = 0;
			LSWrapper.clear();
			Structr.renewSessionId();
			Structr.clearMain();
			Structr.login(text);
			return true;
		}
		ws.close();
		return false;
	},
	renewSessionId: function (callback) {
		$.get('/').always(function() {
			sessionId = Structr.getSessionId();
			if (typeof callback === "function") {
				callback();
			}
		});
	},
	loadInitialModule: function(isLogin) {

		Structr.restoreLocalStorage(function() {

			Structr.expanded = JSON.parse(LSWrapper.getItem(expandedIdsKey));
			_Logger.log(_LogType.INIT, '######## Expanded IDs after reload ##########', Structr.expanded);

			var browserUrl = window.location.href;
			var anchor = getAnchorFromUrl(browserUrl);
			lastMenuEntry = ((!isLogin && anchor && anchor !== 'logout') ? anchor : LSWrapper.getItem(lastMenuEntryKey));
			if (!lastMenuEntry) {
				lastMenuEntry = LSWrapper.getItem(lastMenuEntryKey) || 'dashboard';
			} else {
				_Logger.log(_LogType.INIT, 'Last menu entry found: ' + lastMenuEntry);
			}
			_Logger.log(_LogType.INIT, 'lastMenuEntry', lastMenuEntry);
			Structr.activateMenuEntry(lastMenuEntry);
			_Logger.log(_LogType.INIT, Structr.modules);
			var module = Structr.modules[lastMenuEntry];
			if (module) {
				//module.init();
				module.onload();
				Structr.adaptUiToPresentModules();
				if (module.resize)
					module.resize();
			}
			Structr.updateVersionInfo();
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
		$('iframe').unload();
		//main.children().not('#graph-box').remove();
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
	saveLocalStorage: function() {
		_Logger.log(_LogType.INIT, "Saving localstorage");
		Command.saveLocalStorage();
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

		// Apply stored dimensions of dialog
		var dw = dialogData.width;
		var dh = dialogData.height;

		var l = dialogData.left;
		var t = dialogData.top;

		window.setTimeout(function() {
			$.blockUI({
				fadeIn: 25,
				fadeOut: 25,
				message: dialogBox,
				css: {
					cursor: 'default',
					border: 'none',
					backgroundColor: 'transparent',
					width: dw + 'px',
					height: dh + 'px',
					top: t + 'px',
					left: l + 'px'
				},
				themedCSS: {
					width: dw + 'px',
					height: dh + 'px',
					top: t + 'px',
					left: l + 'px'
				},
				width: dw + 'px',
				height: dh + 'px',
				top: t + 'px',
				left: l + 'px'
			});

			Structr.resize();
		}, 1000);

	},
	dialog: function(text, callbackOk, callbackCancel) {

		if (browser) {

			dialogHead.empty();
			dialogText.empty();
			dialogMsg.empty();
			dialogMeta.empty();
			//dialogBtn.empty();
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
				//dialogSaveButton.remove();
				//$('#saveProperties').remove();
				if (searchField)
					searchField.focus();

				LSWrapper.removeItem(dialogDataKey);

				if (callbackCancel) {
					window.setTimeout(function() {
						callbackCancel();
					}, 100);
				}
			});

			$.blockUI.defaults.overlayCSS.opacity = .4;
			$.blockUI.defaults.applyPlatformOpacityRules = false;

			var w = $(window).width();
			var h = $(window).height();

			var ml = 24;
			var mt = 24;

			// Calculate dimensions of dialog
			var dw = Math.min(900, w - ml);
			var dh = Math.min(600, h - mt);
			//            var dw = (w-24) + 'px';
			//            var dh = (h-24) + 'px';

			var l = parseInt((w - dw) / 2);
			var t = parseInt((h - dh) / 2);

			$.blockUI({
				fadeIn: 25,
				fadeOut: 25,
				message: dialogBox,
				css: {
					cursor: 'default',
					border: 'none',
					backgroundColor: 'transparent',
					width: dw + 'px',
					height: dh + 'px',
					top: t + 'px',
					left: l + 'px'
				},
				themedCSS: {
					width: dw + 'px',
					height: dh + 'px',
					top: t + 'px',
					left: l + 'px'
				},
				width: dw + 'px',
				height: dh + 'px',
				top: t + 'px',
				left: l + 'px'
			});

			Structr.resize();

			_Logger.log(_LogType.INIT, 'Open dialog', dialog, text, dw, dh, t, l, callbackOk, callbackCancel);
			var dialogData = {'text': text, 'top': t, 'left': l, 'width': dw, 'height': dh};
			LSWrapper.setItem(dialogDataKey, JSON.stringify(dialogData));

		}
	},
	setSize: function(w, h, dw, dh) {

		var l = parseInt((w - dw) / 2);
		var t = parseInt((h - dh) / 2);

		$('.blockPage').css({
			width: dw + 'px',
			height: dh + 'px',
			top: t + 'px',
			left: l + 'px'
		});

		var horizontalOffset = 98;

		var bw = (dw - 28) + 'px';

		var dialogHeaderHeight = $('#dialogBox .dialogTextWrapper').offset().top - $('#dialogBox .dialogHeaderWrapper').offset().top;
		var bh = (dh - horizontalOffset - dialogHeaderHeight) + 'px';

		$('#dialogBox .dialogTextWrapper').css({
			width: bw,
			height: bh
		});

		var tabsHeight = $('.files-tabs ul').height();

		$('.CodeMirror:not(.cm-schema-methods)').css({
			height: (dh - horizontalOffset - 24 - tabsHeight) + 'px'
		});

		$('.CodeMirror:not(.cm-schema-methods) .CodeMirror-gutters').css({
			height: (dh - horizontalOffset - 24 - tabsHeight) + 'px'
		});

		$('.CodeMirror:not(.cm-schema-methods)').each(function(i, el) {
			el.CodeMirror.refresh();
		});

		$('.fit-to-height').css({
			height: h - 74 + 'px'
		});

	},
	resize: function(callback) {

		//LSWrapper.removeItem(dialogMaximizedKey);
		isMax = LSWrapper.getItem(dialogMaximizedKey);

		if (isMax) {
			Structr.maximize();
		} else {

			// Calculate dimensions of dialog
			Structr.setSize($(window).width(), $(window).height(), Math.min(900, $(window).width() - 24), Math.min(600, $(window).height() - 24));

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
	errorFromResponse: function(response, url) {
		var errorText = '';

		if (response.errors && response.errors.length) {

			$.each(response.errors, function(i, err) {

                errorText += err.type+ '.';
                errorText += err.property + ' ';
                errorText += err.token + ': ' ;
                errorText += err.detail;
				errorText += '\n';

			});

		} else {

			errorText += url + ': ' + response.code + ' ' + response.message;
		}

		new MessageBuilder().error(errorText).show();
	},
	loaderIcon: function(element, css) {
		element.append('<img class="loader-icon" alt="Loading..." title="Loading.." src="' + _Icons.ajax_loader_1 + '">');
		var li = $('.loader-icon', element);
		if (css) {
			li.css(css);
		}
		return li;
	},
	tempInfo: function(text, autoclose) {
		window.clearTimeout(dialogId);
		if (text)
			$('#tempInfoBox .infoHeading').html('<img src="' + _Icons.information_icon + '"> ' + text);
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
			$('#tempErrorBox .errorText').html('<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABGdBTUEAAK/INwWK6QAAABl0RVh0U29mdHdhcmUAQWRvYmUgSW1hZ2VSZWFkeXHJZTwAAAIsSURBVDjLpVNLSJQBEP7+h6uu62vLVAJDW1KQTMrINQ1vPQzq1GOpa9EppGOHLh0kCEKL7JBEhVCHihAsESyJiE4FWShGRmauu7KYiv6Pma+DGoFrBQ7MzGFmPr5vmDFIYj1mr1WYfrHPovA9VVOqbC7e/1rS9ZlrAVDYHig5WB0oPtBI0TNrUiC5yhP9jeF4X8NPcWfopoY48XT39PjjXeF0vWkZqOjd7LJYrmGasHPCCJbHwhS9/F8M4s8baid764Xi0Ilfp5voorpJfn2wwx/r3l77TwZUvR+qajXVn8PnvocYfXYH6k2ioOaCpaIdf11ivDcayyiMVudsOYqFb60gARJYHG9DbqQFmSVNjaO3K2NpAeK90ZCqtgcrjkP9aUCXp0moetDFEeRXnYCKXhm+uTW0CkBFu4JlxzZkFlbASz4CQGQVBFeEwZm8geyiMuRVntzsL3oXV+YMkvjRsydC1U+lhwZsWXgHb+oWVAEzIwvzyVlk5igsi7DymmHlHsFQR50rjl+981Jy1Fw6Gu0ObTtnU+cgs28AKgDiy+Awpj5OACBAhZ/qh2HOo6i+NeA73jUAML4/qWux8mt6NjW1w599CS9xb0mSEqQBEDAtwqALUmBaG5FV3oYPnTHMjAwetlWksyByaukxQg2wQ9FlccaK/OXA3/uAEUDp3rNIDQ1ctSk6kHh1/jRFoaL4M4snEMeD73gQx4M4PsT1IZ5AfYH68tZY7zv/ApRMY9mnuVMvAAAAAElFTkSuQmCC"> ' + text);
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
	activateModule: function(event, name) {
		if (menuBlocked) return;
		event.stopPropagation();
		if (LSWrapper.getItem(lastMenuEntryKey) !== name || main.children().length === 0) {
			var activeModule = Structr.modules[LSWrapper.getItem(lastMenuEntryKey)];
			if (activeModule && activeModule.unload) {
				activeModule.unload();
			}
			Structr.clearMain();
			Structr.activateMenuEntry(name);
			Structr.modules[name].onload();
			Structr.adaptUiToPresentModules();
		}
	},
	activateMenuEntry: function(name) {
		Structr.blockMenu();
		var menuEntry = $('#' + name + '_');
		if (menuEntry.hasClass('active')) {
			return false;
		}
		lastMenuEntry = name;
		$('.menu a').each(function(i, v) {
			$(this).removeClass('active').addClass('inactive');
		});
		menuEntry.addClass('active').removeClass('inactive');
		$('#title').text('Structr ' + menuEntry.text());
		window.location.hash = lastMenuEntry;
		if (lastMenuEntry && lastMenuEntry !== 'logout') {
			LSWrapper.setItem(lastMenuEntryKey, lastMenuEntry);
		}
	},
	registerModule: function(name, module) {
		Structr.modules[name] = module;
		_Logger.log(_LogType.INIT, 'Module ' + name + ' registered');
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
			if (el && el.hasClass(cls)) {
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

				if (filesMain && filesMain.length)
					filesMain.hide();
				if (widgets && widgets.length)
					widgets.hide();

//                _Pages.init();
				Structr.modules['pages'].onload();
				Structr.adaptUiToPresentModules();
				_Pages.resize();
				$('a#pages_').removeClass('nodeHover').droppable('enable');
			}

		});
	},
	openSlideOut: function(slideout, tab, activeTabKey, callback) {
		var s = $(slideout);
		var t = $(tab);
		t.addClass('active');
		s.animate({right: '+=' + rsw + 'px'}, {duration: 100}).zIndex(1);
		LSWrapper.setItem(activeTabKey, t.prop('id'));
		if (callback) {
			callback();
		}
		_Pages.resize(0, rsw);
	},
	closeSlideOuts: function(slideouts, activeTabKey) {
		var wasOpen = false;
		slideouts.forEach(function(w) {
			var s = $(w);
			var l = s.position().left;
			if (Math.abs(l - $(window).width()) >= 3) {
				wasOpen = true;
				s.animate({right: '-=' + rsw + 'px'}, {duration: 100}).zIndex(2);
				$('.compTab.active', s).removeClass('active');
			}
		});
		if (wasOpen) {
			_Pages.resize(0, -rsw);
		}
		LSWrapper.removeItem(activeTabKey);
	},
	openLeftSlideOut: function(slideout, tab, activeTabKey, callback, dragCallback) {
		var s = $(slideout);
		var storedLeftSlideoutWidth = LSWrapper.getItem(leftSlideoutWidthKey);
		var psw = storedLeftSlideoutWidth ? parseInt(storedLeftSlideoutWidth) : (s.width() + 12);

		var t = $(tab);
		t.addClass('active');
		var sw = psw + 12;
		LSWrapper.setItem(activeTabKey, t.prop('id'));
		s.width(psw);
		s.animate({left: 0 + 'px'}, 100, function () {
			if (typeof callback === 'function') {
				callback({sw: sw});
			}
		}).zIndex(1);
		t.draggable({
			axis: 'x',
			start: function(e, ui) {
				$(this).addClass('noclick');
			},
			drag: function(e, ui) {
				var w = ui.position.left - 12;
				slideout.css({
					width: w + 'px'
				});
				ui.position.top += (ui.helper.width() / 2 - 6);
				ui.position.left -= (ui.helper.width() / 2 - 6);
				var oldLsw = sw;
				sw = w + 12;
				$('.node.page', s).width(w - 25);

				if (dragCallback) {
					LSWrapper.setItem(leftSlideoutWidthKey, s.width());
					dragCallback({sw: (sw - oldLsw)});
				}
			},
			stop: function(e, ui) {
				LSWrapper.setItem(leftSlideoutWidthKey, s.width());
			}
		});
	},
	closeLeftSlideOuts: function(slideouts, activeTabKey, callback) {
		var wasOpen = false;
		var osw;
		slideouts.forEach(function(w) {
			var s = $(w);
			var l = s.position().left;
			var sw = s.width() + 12;
			if (Math.abs(l) <= 3) {
				wasOpen = true;
				osw = sw;
				s.animate({left: - sw -1 + 'px'}, 100, function () {
					if (typeof callback === 'function') {
						callback(wasOpen, -osw, 0);
					}
				}).zIndex(2);
				$('.compTab.active', s).removeClass('active').draggable("destroy");
			}
		});
		LSWrapper.removeItem(activeTabKey);
	},
	pushDialog: function(id, recursive) {

		var obj = StructrModel.obj(id);

		Structr.dialog('Push node to remote server', function() {
		},
				function() {
				});

		var pushConf = JSON.parse(LSWrapper.getItem(pushConfigKey)) || {};

		dialog.append('Do you want to transfer <b>' + (obj.name || obj.id) + '</b> to the remote server?');

		dialog.append('<table class="props push">'
				+ '<tr><td>Host</td><td><input id="push-host" type="text" length="20" value="' + (pushConf.host || '') + '"></td></tr>'
				+ '<tr><td>Port</td><td><input id="push-port" type="text" length="20" value="' + (pushConf.port || '') + '"></td></tr>'
				+ '<tr><td>Username</td><td><input id="push-username" type="text" length="20" value="' + (pushConf.username || '') + '"></td></tr>'
				+ '<tr><td>Password</td><td><input id="push-password" type="password" length="20" value="' + (pushConf.password || '') + '"></td></tr>'
				+ '</table>'
				+ '<button id="start-push">Start</button>');

		$('#start-push', dialog).on('click', function() {
			var host = $('#push-host', dialog).val();
			var port = parseInt($('#push-port', dialog).val());
			var username = $('#push-username', dialog).val();
			var password = $('#push-password', dialog).val();
			var key = 'key_' + obj.id;

			pushConf = {host: host, port: port, username: username, password: password};
			LSWrapper.setItem(pushConfigKey, JSON.stringify(pushConf));

			Command.push(obj.id, host, port, username, password, key, recursive, function() {
				dialog.empty();
				dialogCancelButton.click();
			});
		});

		return false;
	},
	pullDialog: function(type) {

		Structr.dialog('Sync ' + type.replace(/,/, '(s) or ') + '(s) from remote server', function() {
		},
				function() {
				});

		var pushConf = JSON.parse(LSWrapper.getItem(pushConfigKey)) || {};

		dialog.append('<table class="props push">'
				+ '<tr><td>Host</td><td><input id="push-host" type="text" length="32" value="' + (pushConf.host || '') + '"></td>'
				+ '<td>Port</td><td><input id="push-port" type="text" length="32" value="' + (pushConf.port || '') + '"></td></tr>'
				+ '<tr><td>Username</td><td><input id="push-username" type="text" length="32" value="' + (pushConf.username || '') + '"></td>'
				+ '<td>Password</td><td><input id="push-password" type="password" length="32" value="' + (pushConf.password || '') + '"></td></tr>'
				+ '</table>'
				+ '<button id="show-syncables">Show available entities</button>'
				+ '<table id="syncables" class="props push"><tr><th>Name</th><th>Size</th><th>Last Modified</th><th>Type</th><th>Recursive</th><th>Actions</th></tr>'
				+ '</table>'
				);

		$('#show-syncables', dialog).on('click', function() {

			var syncables = $("#syncables");
			var host = $('#push-host', dialog).val();
			var port = parseInt($('#push-port', dialog).val());
			var username = $('#push-username', dialog).val();
			var password = $('#push-password', dialog).val();
			var key = 'syncables';

			pushConf = {host: host, port: port, username: username, password: password};
			LSWrapper.setItem(pushConfigKey, JSON.stringify(pushConf));

			fastRemoveAllChildren(syncables[0]);
			syncables.append('<tr><th>Name</th><th>Size</th><th>Last Modified</th><th>Type</th><th>Recursive</th><th>Actions</th></tr>');

			Command.listSyncables(host, port, username, password, key, type, function(result) {

				result.forEach(function(syncable) {

					syncables.append(
							'<tr>'
							+ '<td>' + syncable.name + '</td>'
							+ '<td>' + (syncable.size ? syncable.size : "-") + '</td>'
							+ '<td>' + (syncable.lastModifiedDate ? syncable.lastModifiedDate : "-") + '</td>'
							+ '<td>' + syncable.type + '</td>'
							+ '<td><input type="checkbox" id="recursive-' + syncable.id + '"></td>'
							+ '<td><button id="pull-' + syncable.id + '"></td>'
							+ '</tr>'
							);

					var syncButton = $('#pull-' + syncable.id, dialog);

					if (syncable.isSynchronized) {
						syncButton.empty();
						syncButton.append('<img src="' + _Icons.refresh_icon + '" title="Update" alt="Update"> Update');
					} else {
						syncButton.empty();
						syncButton.append('<img src="' + _Icons.pull_file_icon + '" title="Import" alt="Import"> Import');
					}

					syncButton.on('click', function() {

						syncButton.empty();
						syncButton.append('Importing..');

						var recursive = $('#recursive-' + syncable.id, syncables).prop('checked');
						Command.pull(syncable.id, host, port, username, password, 'key-' + syncable.id, recursive, function() {
							// update table cell..
							syncButton.empty();
							syncButton.append('<img src="' + _Icons.refresh_icon + '" title="Update" alt="Update"> Update');
						});
					});
				});

			});
		});

		return false;
	},
	ensureIsAdmin: function(el, callback) {
		Structr.ping(function() {
			if (!isAdmin) {
				Structr.error('You do not have sufficient permissions<br>to access this function.', true);
				el.append('<div class="errorText"><img src="' + _Icons.error_icon + '"> You do not have sufficient permissions to access this function.</div>');
			} else {
				if (callback) {
					callback();
				}
			}
		});
	},
	updateVersionInfo: function() {
		$.get(rootUrl + '_env', function(envInfo) {
			if (envInfo && envInfo.result) {

				$('#header .structr-instance-name').text(envInfo.result.instanceName);
				$('#header .structr-instance-stage').text(envInfo.result.instanceStage);

				var ui = envInfo.result.components['structr-ui'];
				if (ui !== null) {

					var version = ui.version;
					var build = ui.build;
					var date = ui.date;
					var versionLink;
					if (version.endsWith('SNAPSHOT')) {
						versionLink = 'https://oss.sonatype.org/content/repositories/snapshots/org/structr/structr-ui/' + version;
					} else {
						versionLink = 'http://repo1.maven.org/maven2/org/structr/structr-ui/' + version;
					}
					var versionInfo = '<a target="_blank" href="' + versionLink + '">' + version + '</a>';
					if (build && date) {
						versionInfo += ' build <a target="_blank" href="https://github.com/structr/structr/commit/' + build + '">' + build + '</a> (' + date + ')';
					}

					$('.structr-version').html(versionInfo);
				}

				Structr.activeModules = envInfo.result.modules;
				Structr.adaptUiToPresentModules();
			}
		});
	},
	getAutoHideInactiveTabs: function () {
		if (this.autoHideInactiveTabs) {
			return this.autoHideInactiveTabs;
		} else {
			this.autoHideInactiveTabs = (LSWrapper.getItem(autoHideInactiveTabsKey));
			if (!this.autoHideInactiveTabs) {
				this.setAutoHideInactiveTabs(false);
			}
			return this.autoHideInactiveTabs;
		}
	},
	setAutoHideInactiveTabs: function (val) {
		this.autoHideInactiveTabs = val;
		LSWrapper.setItem(autoHideInactiveTabsKey, val);
		if (val) {
			this.doHideInactiveTabs();
			this.doHideSelectAllCheckbox();
		}
	},
	getHideInactiveTabs: function () {
		if (this.hideInactiveTabs) {
			return this.hideInactiveTabs;
		} else {
			this.hideInactiveTabs = (LSWrapper.getItem(hideInactiveTabsKey));
			if (!this.hideInactiveTabs) {
				this.setHideInactiveTabs(false);
			}
			return this.hideInactiveTabs;
		}
	},
	setHideInactiveTabs: function (val) {
		this.hideInactiveTabs = val;
		LSWrapper.setItem(hideInactiveTabsKey, val);

		if (val) {
			this.doHideInactiveTabs();
			this.doHideSelectAllCheckbox();
		} else {
			this.doShowAllTabs();
			this.doShowSelectAllCheckbox();
		}
	},
	doHideInactiveTabs: function () {
		$('#resourceTabsMenu li:not(.last) input[type="checkbox"]').hide();
		$('.ui-state-default.ui-corner-top:not(.ui-tabs-active.ui-state-active):not(.last) input[type="checkbox"]:not(:checked)').closest('li').hide();
	},
	doShowAllTabs: function () {
		$('#resourceTabsMenu input[type="checkbox"]').show();
		$('.ui-state-default.ui-corner-top:not(.ui-tabs-active.ui-state-active)').show();
	},
	doHideSelectAllCheckbox: function () {
		$('#resourceTabsSelectAllWrapper').hide();
	},
	doShowSelectAllCheckbox: function () {
		$('#resourceTabsSelectAllWrapper').show();
	},
	determineSelectAllCheckboxState: function () {
		if ( $('#resourceTabsMenu li:not(.last) input[type="checkbox"]').length === $('#resourceTabsMenu li:not(.last) input[type="checkbox"]:checked').length ) {
			$('#resourceTabsSelectAll').prop('checked', true);
		} else {
			$('#resourceTabsSelectAll').prop('checked', false);
		}
	},
	doSelectAllTabs: function () {
		$('#resourceTabsMenu li:not(.last) input[type="checkbox"]:not(:checked)').click();
	},
	doDeselectAllTabs: function () {
		$('#resourceTabsMenu li:not(.last) input[type="checkbox"]:checked').click();
	},
	doSelectTabs: function (types) {
		types.forEach(function(type) {
			$('#resourceTabsMenu li:not(.last) a[href="#' + type + '"] input[type="checkbox"]:not(:checked)').click();
		});
	},
	doDeselectTabs: function (types) {
		types.forEach(function(type) {
			$('#resourceTabsMenu li:not(.last) a[href="#' + type + '"] input[type="checkbox"]:checked').click();
		});
	},
	getId: function(element) {
		return Structr.getIdFromPrefixIdString($(element).prop('id'), 'id_') || undefined;
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
	getActiveElementId: function(element) {
		return Structr.getIdFromPrefixIdString($(element).prop('id'), 'active_') || undefined;
	},
	adaptUiToPresentModules: function() {
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
	guardExecution:function (callbackToGuard) {
		var didRun = false;

		var guardedFunction = function () {
			if (didRun) {
				return;
			}
			didRun = true;

			if (typeof callbackToGuard === "function") {
				callbackToGuard();
			}
		};

		return guardedFunction;
	}
};

function MessageBuilder () {
	this.params = {
		// defaults
		text: 'Default message',
		delayDuration: 3000,
		fadeDuration: 1000,
		confirmButtonText: 'Confirm'
	};

	this.requiresConfirmation = function (confirmButtonText) {
		this.params.requiresConfirmation = true;

		if (confirmButtonText) {
			this.params.confirmButtonText = confirmButtonText;
		}

		return this;
	};

	this.title = function (title) {
		this.params.title = title;
		return this;
	};

	this.text = function (text) {
		this.params.text = text;
		return this;
	};

	this.error = function (text) {
		this.params.text = text;
		return this.className('error');
	};

	this.warning = function (text) {
		this.params.text = text;
		return this.className('warning');
	};

	this.info = function (text) {
		this.params.text = text;
		return this.className('info');
	};

	this.success = function (text) {
		this.params.text = text;
		return this.className('success');
	};

	this.delayDuration = function (delayDuration) {
		this.params.delayDuration = delayDuration;
		return this;
	};

	this.fadeDuration = function (fadeDuration) {
		this.params.fadeDuration = fadeDuration;
		return this;
	};

	this.className = function (className) {
		this.params.className = className;
		return this;
	};

	this.delayDuration = function (delayDuration) {
		this.params.delayDuration = delayDuration;
		return this;
	};

	this.show = function () {
		this.params.msgId = 'message_' + (Structr.msgCount++);

		$('#info-area').append(
			'<div class="message' + (this.params.className ? ' ' + this.params.className : '') +  '" id="' + this.params.msgId + '"">' +
				(this.params.title ? '<h2>' + this.params.title + '</h2>' : '') +
				this.params.text +
				(this.params.requiresConfirmation ? '<button class="confirm">' + this.params.confirmButtonText + '</button>' : '') +
				(this.params.specialInteractionButton ? '<button class="special">' + this.params.specialInteractionButton.text + '</button>' : '') +
			'</div>'
		);

		var msgBuilder = this;

		if (this.params.requiresConfirmation === true) {

			$('#' + this.params.msgId).find('button.confirm').click(function () {
				msgBuilder.hide();
			});

		} else {

			window.setTimeout(function () {
				msgBuilder.hide();
			}, this.params.delayDuration);

			$('#' + this.params.msgId).click(function () {
				msgBuilder.hide();
			});

		}

		if (this.params.specialInteractionButton) {

			$('#' + this.params.msgId).find('button.special').click(function () {
				if (msgBuilder.params.specialInteractionButton) {
					msgBuilder.params.specialInteractionButton.action();

					msgBuilder.hide();
				}
			});

		}
	};

	this.hide = function () {
		$('#' + this.params.msgId).animate({
			opacity: 0,
			height: 0
		}, {
			duration: this.params.fadeDuration,
			complete: function () {
				$(this).remove();
			}
		});
	};

	this.specialInteractionButton = function (buttonText, callback, confirmButtonText) {
		this.params.requiresConfirmation = true;

		this.params.specialInteractionButton = {
			text: buttonText,
			action: callback
		};

		if (confirmButtonText) {
			this.params.confirmButtonText = confirmButtonText;
		}

		return this;
	};

	return this;
}

function getElementPath(element) {
	var i = -1;
	return $(element).parents('.node').andSelf().map(function() {
		i++;
		var self = $(this);
		// id for top-level element
		if (i === 0)
			return Structr.getId(self);
		return self.prevAll('.node').length;
	}).get().join('_');
}

function swapFgBg(el) {
	var fg = el.css('color');
	var bg = el.css('backgroundColor');
	el.css('color', bg);
	el.css('backgroundColor', fg);

}

function isImage(contentType) {
	return (contentType && contentType.indexOf('image') > -1);
}

function isVideo(contentType) {
	return (contentType && contentType.indexOf('video') > -1);
}

function addExpandedNode(id) {
	_Logger.log(_LogType.INIT, 'addExpandedNode', id);

	if (!id) {
		return;
	}

	var alreadyStored = getExpanded()[id];

	if (alreadyStored !== undefined) {
		return;
	}

	getExpanded()[id] = true;
	LSWrapper.setItem(expandedIdsKey, JSON.stringify(Structr.expanded));

}

function removeExpandedNode(id) {
	_Logger.log(_LogType.INIT, 'removeExpandedNode', id);

	if (!id) {
		return;
	}

	delete getExpanded()[id];
	LSWrapper.setItem(expandedIdsKey, JSON.stringify(Structr.expanded));
}

function isExpanded(id) {
	_Logger.log(_LogType.INIT, 'id, getExpanded()[id]', id, getExpanded()[id]);

	if (!id)
		return false;

	var isExpanded = getExpanded()[id] === true ? true : false;

	_Logger.log(_LogType.INIT, isExpanded);

	return isExpanded;
}

function getExpanded() {
	if (!Structr.expanded) {
		Structr.expanded = JSON.parse(LSWrapper.getItem(expandedIdsKey));
	}

	if (!Structr.expanded) {
		Structr.expanded = {};
	}
	return Structr.expanded;
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

function isDisabled(button) {
	return $(button).data('disabled');
}

function disable(button, callback) {
	var b = $(button);
	b.data('disabled', true);
	b.addClass('disabled');
	if (callback) {
		b.off('click');
		b.on('click', callback);
		b.data('disabled', false);
		//enable(button, callback);
	}
}

function enable(button, func) {
	var b = $(button);
	b.data('disabled', false);
	b.removeClass('disabled');
	if (func) {
		b.off('click');
		b.on('click', func);
	}
}

function setPosition(parentId, nodeUrl, pos) {
	var toPut = {};
	toPut[parentId] = pos;
	$.ajax({
		url: nodeUrl + '/in',
		type: 'PUT',
		async: false,
		dataType: 'json',
		contentType: 'application/json; charset=utf-8',
		data: JSON.stringify(toPut),
		success: function(data) {
			//appendElement(parentId, elementId, data);
		}
	});
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

function showLoadingSpinner() {
	var msg = '<div id="structr-loading-spinner"><img src="/structr/' + _Icons.ajax_loader_1 + '"></div>';
	$.blockUI.defaults.overlayCSS.opacity = .2;
	$.blockUI.defaults.applyPlatformOpacityRules = false;
	$.blockUI({
		fadeIn: 0,
		fadeOut: 0,
		message: msg,
		forceInput: true,
		css: {
			border: 'none',
			backgroundColor: 'transparent'
		}
	});
//	main.append('<div id="structr-loading-spinner"><img src="/structr/' + _Icons.ajax_loader_1 + '"></div>');
}

function hideLoadingSpinner() {
	$.unblockUI({
		fadeOut: 0
	});
//	main.remove('#structr-loading-spinner');
}