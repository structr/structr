/*
 * Copyright (C) 2010-2017 Structr GmbH
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
var dialogBox, dialogMsg, dialogBtn, dialogTitle, dialogMeta, dialogText, dialogHead, dialogCancelButton, dialogSaveButton, saveAndClose, loginButton, loginBox, dialogCloseButton;
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

	loginButton = $('#loginButton');
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
	wrench_icon: 'icon/wrench.png',
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
	microphone_icon: 'icon/icon_microphone.png',
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
	arrow_up_down: 'icon/arrow_up_down.png',
	floppy_icon: 'icon/disk.png',


	getFullSpriteClass: function (key) {

		return 'sprite ' + _Icons.getSpriteClassOnly(key);

	},
	updateSpritasdeClassTo: function (el, newSpriteClass) {
		el.classList.forEach(function(cls) {
			if (cls.indexOf('sprite-') === 0) {
				el.classList.remove(cls);
			}
		});
		el.classList.add(newSpriteClass);
	},
	getSpriteClassOnly: function (key) {

		switch (key) {
			case _Icons.add_icon:                     return 'sprite-add';
			case _Icons.delete_icon:                  return 'sprite-delete';
			case _Icons.delete_disabled_icon:         return 'sprite-delete_gray';
			case _Icons.delete_brick_icon:            return 'sprite-brick_delete';
			case _Icons.edit_icon:                    return 'sprite-pencil';
			case _Icons.wrench_icon:                  return 'sprite-wrench';
			case _Icons.collapsed_icon:               return 'sprite-tree_arrow_right';
			case _Icons.expanded_icon:                return 'sprite-tree_arrow_down';
			case _Icons.link_icon:                    return 'sprite-link';
			case _Icons.key_icon:                     return 'sprite-key';
			case _Icons.key_add_icon:                 return 'sprite-key_add';
			case _Icons.cross_icon:                   return 'sprite-cross';
			case _Icons.tick_icon:                    return 'sprite-tick';
			case _Icons.grey_cross_icon:              return 'sprite-cross_small_grey';
			case _Icons.page_white_stack_icon:        return 'sprite-page_white_stack';
			case _Icons.eye_icon:                     return 'sprite-eye';
			case _Icons.database_icon:                return 'sprite-database';
			case _Icons.database_table_icon:          return 'sprite-database_table';
			case _Icons.database_add_icon:            return 'sprite-database_add';
			case _Icons.view_detail_icon:             return 'sprite-application_view_detail';
			case _Icons.calendar_icon:                return 'sprite-calendar';
			case _Icons.add_grey_icon:                return 'sprite-add_grey';
			case _Icons.page_icon:                    return 'sprite-page';
			case _Icons.page_white_icon:              return 'sprite-page_white';
			case _Icons.button_icon:                  return 'sprite-button';
			case _Icons.clone_icon:                   return 'sprite-page_copy';
			case _Icons.group_icon:                   return 'sprite-group';
			case _Icons.group_add_icon:               return 'sprite-group_add';
			case _Icons.user_icon:                    return 'sprite-user';
			case _Icons.user_add_icon:                return 'sprite-user_add';
			case _Icons.user_delete_icon:             return 'sprite-user_delete';
			case _Icons.compress_icon:                return 'sprite-compress';
			case _Icons.accept_icon:                  return 'sprite-accept';
			case _Icons.push_file_icon:               return 'sprite-page_white_get';
			case _Icons.pull_file_icon:               return 'sprite-page_white_put';
			case _Icons.exec_cypher_icon:             return 'sprite-control_play_blue';
			case _Icons.exec_rest_icon:               return 'sprite-control_play';
			case _Icons.arrow_undo_icon:              return 'sprite-arrow_undo';
			case _Icons.information_icon:             return 'sprite-information';
			case _Icons.refresh_icon:                 return 'sprite-arrow_refresh';
			case _Icons.error_icon:                   return 'sprite-error';
			case _Icons.pull_page_icon:               return 'sprite-pull_page';
			case _Icons.wand_icon:                    return 'sprite-wand';
			case _Icons.toggle_icon:                  return 'sprite-arrow_switch';
			case _Icons.widget_icon:                  return 'sprite-layout';
			case _Icons.folder_icon:                  return 'sprite-folder';
			case _Icons.add_folder_icon:              return 'sprite-folder_add';
			case _Icons.add_widget_icon:              return 'sprite-layout_add';
			case _Icons.content_icon:                 return 'sprite-page_white';
			case _Icons.active_content_icon:          return 'sprite-page_yellow';
			case _Icons.delete_content_icon:          return 'sprite-page_white_delete';
			case _Icons.template_icon:                return 'sprite-layout_content';
			case _Icons.active_template_icon:         return 'sprite-layout_yellow';
			case _Icons.comp_templ_icon:              return 'sprite-layout_yellow';
			case _Icons.icon_shared_template:         return 'sprite-layout_yellow';
			case _Icons.comment_icon:                 return 'sprite-comment';
			case _Icons.repeater_icon:                return 'sprite-bricks';
			case _Icons.brick_icon:                   return 'sprite-brick';
			case _Icons.comp_icon:                    return 'sprite-brick_yellow';
			case _Icons.microphone_icon:              return 'sprite-icon_microphone';
			case _Icons.add_file_icon:                return 'sprite-page_white_add';
			case _Icons.add_site_icon:                return 'sprite-page_white_add';
			case _Icons.add_page_icon:                return 'sprite-page_add';
			case _Icons.structr_logo_small:           return 'sprite-structr_icon_16x16';
			case _Icons.minification_dialog_js_icon:  return 'sprite-script_lightning';
			case _Icons.minification_dialog_css_icon: return 'sprite-script_palette';
			case _Icons.minification_trigger_icon:    return 'sprite-briefcase';
			case _Icons.search_icon:                  return 'sprite-zoom';
			case _Icons.star_icon:                    return 'sprite-star';
			case _Icons.star_delete_icon:             return 'sprite-star_delete';
			case _Icons.image_icon:                   return 'sprite-image';
			case _Icons.arrow_up_down:                return 'sprite-arrow_up_down';
			case _Icons.floppy_icon:                  return 'sprite-disk';

			default:                                  return 'sprite-error';
		}

	},
	getImageOrIcon: function(image) {
		return (image.contentType.startsWith('image/svg') ? _Icons.getImageMarkup(image.path) : (image.tnSmall ? _Icons.getImageMarkup(image.tnSmall.path) : '<i class="icon sprite sprite-image" />'));
	},
	getImageMarkup: function (path) {
		return '<img class="icon" src="' + path + '">';
	},
	getMinificationIcon: function(file) {
		switch(file.type) {
			case 'MinifiedCssFile':
				return _Icons.minification_dialog_css_icon;
			case 'MinifiedJavaScriptFile':
				return _Icons.minification_dialog_js_icon;
			default:
				return _Icons.error_icon;
		}
	},
	getFileIconClass: function(file) {

		var fileName = file.name;
		var contentType = file.contentType;

		var result = 'fa-file-o';

		if (contentType) {

			switch (contentType) {

				case 'text/plain':
					result = 'fa-file-text-o';
					break;

				case 'application/pdf':
				case 'application/postscript':
					result = 'fa-file-pdf-o';
					break;

				case 'application/x-pem-key':
				case 'application/pkix-cert+pem':
				case 'application/x-iwork-keynote-sffkey':
					result = 'fa-key';
					break;

				case 'application/x-trash':
					result = 'fa-trash-o';
					break;

				case 'application/octet-stream':
					result = 'fa-terminal';
					break;

				case 'application/x-shellscript':
				case 'application/javascript':
				case 'application/xml':
				case 'text/html':
				case 'text/xml':
					result = 'fa-file-code-o';
					break;

				case 'application/java-archive':
				case 'application/zip':
				case 'application/rar':
				case 'application/x-bzip':
					result = 'fa-file-archive-o';
					break;

				case 'application/vnd.openxmlformats-officedocument.wordprocessingml.document':
				case 'application/vnd.oasis.opendocument.text':
				case 'application/msword':
					result = 'fa-file-word-o';
					break;

				case 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet':
				case 'application/vnd.oasis.opendocument.spreadsheet':
				case 'application/vnd.ms-excel':
					result = 'fa-file-excel-o';
					break;

				case 'application/vnd.openxmlformats-officedocument.presentationml.presentation':
					result = 'fa-file-powerpoint-o';
					break;

				case 'image/jpeg':
					result = 'fa-picture-o';
					break;

				case 'application/vnd.oasis.opendocument.chart':
					result = 'fa-line-chart';
					break;

				default:
					if (contentType.startsWith('image/')) {
						result = 'fa-file-image-o';
					} else if (contentType.startsWith('text/')) {
						result = 'fa-file-text-o';
					}
			}

			if (fileName && fileName.contains('.')) {

				var fileExtensionPosition = fileName.lastIndexOf('.') + 1;
				var fileExtension = fileName.substring(fileExtensionPosition);

				// add file extension css class to control colors
				result = fileExtension + ' ' + result;
			}
		}

		return result;
	},

	getSpinnerImageAsData: function () {
		return "data:image/gif;base64,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";
	}
};

var Structr = {
	modules: {},
	activeModules: {},
	classes: [],
	expanded: {},
	msgCount: 0,

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
			if (dialogData) {
				Structr.restoreDialog(dialogData);
			}
		}
		hideLoadingSpinner();
		_Console.initConsole();
		_Favorites.initFavorites();
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
		_Favorites.logoutAction();
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
				module.onload();
				Structr.adaptUiToPresentModules();
				if (module.resize) {
					module.resize();
				}
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
	getDialogDimensions: function (marginLeft, marginTop) {

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
	blockUI: function (dimensions) {

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
			height: dh + 'px',
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
			height: h - 74 + 'px'
		});

	},
	resize: function(callback) {

		isMax = LSWrapper.getItem(dialogMaximizedKey);

		if (isMax) {
			Structr.maximize();
		} else {

			// Calculate dimensions of dialog
			if ($('.blockPage').length) {
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

			errorText += response.code + ' ' + response.message;
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
		}

		message.show();
	},
	getErrorTextForStatusCode: function (statusCode) {
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
	updateButtonWithSuccessIcon: function (btn, html) {
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
	getActiveModuleName: function () {
		return LSWrapper.getItem(lastMenuEntryKey);
	},
	isModuleActive: function (module) {
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

				if (filesMain && filesMain.length) {
					filesMain.hide();
				}
				if (_Widgets.widgets && _Widgets.widgets.length) {
					_Widgets.widgets.hide();
				}

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
	openLeftSlideOut: function(triggerEl, slideoutElement, activeTabKey, callback) {
		var storedLeftSlideoutWidth = LSWrapper.getItem(_Pages.leftSlideoutWidthKey);
		var psw = storedLeftSlideoutWidth ? parseInt(storedLeftSlideoutWidth) : (slideoutElement.width() + 12);

		var t = $(triggerEl);
		t.addClass('active');
		var slideoutWidth = psw + 12;
		LSWrapper.setItem(activeTabKey, t.prop('id'));
		slideoutElement.width(psw);
		slideoutElement.animate({left: 0 + 'px'}, 100, function () {
			if (typeof callback === 'function') {
				callback({sw: slideoutWidth});
			}
		}).zIndex(1);
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

		Structr.dialog('Push node to remote server', function() {}, function() {});

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
	pullDialog: function(type, optionalContainer) {

		var container;

		if (optionalContainer) {
			 container = optionalContainer;
			 container.append('<h3>Sync ' + type.replace(/,/, '(s) or ') + '(s) from remote server</h3>');
		} else {
			container = dialog;
			Structr.dialog('Sync ' + type.replace(/,/, '(s) or ') + '(s) from remote server', function() {}, function() {});
		}

		var pushConf = JSON.parse(LSWrapper.getItem(pushConfigKey)) || {};

		container.append('<table class="props push">'
				+ '<tr><td>Host</td><td><input id="push-host" type="text" length="32" value="' + (pushConf.host || '') + '"></td>'
				+ '<td>Port</td><td><input id="push-port" type="text" length="32" value="' + (pushConf.port || '') + '"></td></tr>'
				+ '<tr><td>Username</td><td><input id="push-username" type="text" length="32" value="' + (pushConf.username || '') + '"></td>'
				+ '<td>Password</td><td><input id="push-password" type="password" length="32" value="' + (pushConf.password || '') + '"></td></tr>'
				+ '</table>'
				+ '<button id="show-syncables">Show available entities</button>'
				+ '<table id="syncables" class="props push"><tr><th>Name</th><th>Size</th><th>Last Modified</th><th>Type</th><th>Recursive</th><th>Actions</th></tr>'
				+ '</table>'
				);

		$('#show-syncables', container).on('click', function() {

			var syncables = $("#syncables");
			var host = $('#push-host', container).val();
			var port = parseInt($('#push-port', container).val());
			var username = $('#push-username', container).val();
			var password = $('#push-password', container).val();
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

					var syncButton = $('#pull-' + syncable.id, container);

					if (syncable.isSynchronized) {
						syncButton.empty();
						syncButton.append('<i class="' + _Icons.getFullSpriteClass(_Icons.refresh_icon) + '" /> Update');
					} else {
						syncButton.empty();
						syncButton.append('<i class="' + _Icons.getFullSpriteClass(_Icons.pull_file_icon) + '" /> Import');
					}

					syncButton.on('click', function() {

						syncButton.empty();
						syncButton.append('Importing..');

						var recursive = $('#recursive-' + syncable.id, syncables).prop('checked');
						Command.pull(syncable.id, host, port, username, password, 'key-' + syncable.id, recursive, function() {
							syncButton.empty();
							syncButton.append('<i class="' + _Icons.getFullSpriteClass(_Icons.refresh_icon) + '" /> Update');
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
				el.append('<div class="errorText"><i class="' + _Icons.getFullSpriteClass(_Icons.error_icon) + '" /> You do not have sufficient permissions to access this function.</div>');
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
	getUserId: function (element) {
		return element.data('userId');
	},
	getGroupId: function (element) {
		return element.data('groupId');
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
	},
	updateMainHelpLink: function (newUrl) {
		$('#main-help a').attr('href', newUrl);
	},
	isButtonDisabled: function (button) {
		return $(button).data('disabled');
	},
	disableButton: function (button, newClickHandler) {
		var b = $(button);
		b.data('disabled', true);
		b.addClass('disabled');

		if (newClickHandler) {
			b.off('click');
			b.on('click', newClickHandler);
			b.data('disabled', false);
		}
	},
	enableButton: function (button, newClickHandler) {
		var b = $(button);
		b.data('disabled', false);
		b.removeClass('disabled');

		if (newClickHandler) {
			b.off('click');
			b.on('click', newClickHandler);
		}
	},
	addExpandedNode: function (id) {
		_Logger.log(_LogType.INIT, 'addExpandedNode', id);

		if (id) {
			var alreadyStored = Structr.getExpanded()[id];
			if (!alreadyStored) {

				Structr.getExpanded()[id] = true;
				LSWrapper.setItem(expandedIdsKey, JSON.stringify(Structr.expanded));

			}
		}
	},
	removeExpandedNode: function (id) {
		_Logger.log(_LogType.INIT, 'removeExpandedNode', id);

		if (id) {
			delete Structr.getExpanded()[id];
			LSWrapper.setItem(expandedIdsKey, JSON.stringify(Structr.expanded));
		}
	},
	isExpanded: function (id) {
		_Logger.log(_LogType.INIT, 'id, Structr.getExpanded()[id]', id, Structr.getExpanded()[id]);

		if (id) {
			var isExpanded = (Structr.getExpanded()[id] === true) ? true : false;

			_Logger.log(_LogType.INIT, isExpanded);

			return isExpanded;
		}

		return false;
	},
	getExpanded: function () {
		if (!Structr.expanded) {
			Structr.expanded = JSON.parse(LSWrapper.getItem(expandedIdsKey));
		}

		if (!Structr.expanded) {
			Structr.expanded = {};
		}
		return Structr.expanded;
	},
	showAndHideInfoBoxMessage: function (msg, msgClass, delayTime, fadeTime) {
		dialogMsg.html('<div class="infoBox ' + msgClass + '">' + msg + '</div>');
		$('.infoBox', dialogMsg).delay(delayTime).fadeOut(fadeTime);
	},
	initVerticalSlider: function (sliderEl, localstorageKey, minWidth, dragCallback) {

		if (typeof dragCallback !== "function") {
			console.error('dragCallback is not a function!');
			return;
		}

		sliderEl.draggable({
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
	appendHelpTextToElement: function (helpText, el, css) {

		var toggleElement = $('<span><i class="' + _Icons.getFullSpriteClass(_Icons.information_icon) + '"></span>');
		if (css) {
			toggleElement.css(css);
		}
		var helpElement = $('<span class="context-help-text">' + helpText + '</span>');

		toggleElement.on("mousemove", function(e) {
			helpElement.show();
			helpElement.css({
				left: e.clientX + 20,
				top: e.clientY + 10
			});
		});

		toggleElement.on("mouseout", function(e) {
			helpElement.hide();
		});

		return el.append(toggleElement).append(helpElement);
	}
};

var _TreeHelper = {
	initTree: function (tree, initFunction, stateKey) {
		tree.jstree({
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

			Command.get(element.id, function(loadedElement) {
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
		tree.jstree('refresh');

		if (typeof callback === "function") {
			window.setTimeout(function() {
				callback();
			}, 500);
		}
	},
	makeDroppable: function (tree, list) {
		window.setTimeout(function() {
			list.forEach(function(obj) {
				StructrModel.create({id: obj.id}, null, false);
				_TreeHelper.makeTreeElementDroppable(tree, obj.id);
			});
		}, 500);
	},
	makeTreeElementDroppable: function (tree, id) {
		var el = $('#' + id + ' > .jstree-wholerow', tree);
		_Dragndrop.makeDroppable(el);
	}
};

function MessageBuilder () {
	this.params = {
		// defaults
		text: 'Default message',
		delayDuration: 3000,
		fadeDuration: 1000,
		confirmButtonText: 'Confirm',
		classNames: ['message'],
		uniqueClass: undefined,
		uniqueCount: 1
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
		this.params.classNames.push(className);
		return this;
	};

	this.delayDuration = function (delayDuration) {
		this.params.delayDuration = delayDuration;
		return this;
	};

	this.show = function () {

		var uniqueMessageAlreadyPresented = false;

		if (this.params.uniqueClass) {
			// find existing one
			var existingMsgBuilder = $('#info-area .message.' + this.params.uniqueClass).data('msgbuilder');
			if (existingMsgBuilder) {
				uniqueMessageAlreadyPresented = true;
				existingMsgBuilder.incrementUniqueCount();
			}

		}


		if (uniqueMessageAlreadyPresented === false) {

			this.params.msgId = 'message_' + (Structr.msgCount++);

			$('#info-area').append(
				'<div class="' + this.params.classNames.join(' ') +  '" id="' + this.params.msgId + '">' +
					(this.params.title ? '<h3>' + this.params.title + this.getUniqueCountElement() + '</h3>' : this.getUniqueCountElement()) +
					this.params.text +
					(this.params.requiresConfirmation ? '<button class="confirm">' + this.params.confirmButtonText + '</button>' : '') +
					(this.params.specialInteractionButton ? '<button class="special">' + this.params.specialInteractionButton.text + '</button>' : '') +
				'</div>'
			);

			var msgBuilder = this;
			$('#' + this.params.msgId).data('msgbuilder', msgBuilder);

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

		this.params.specialInteractionButton = {
			text: buttonText,
			action: callback
		};

		return this.requiresConfirmation(confirmButtonText);
	};

	this.uniqueClass = function (className) {
		if (className) {
			this.params.uniqueClass = className;
			return this.className(className);
		}
		return this;
	};

	this.getUniqueCountElement = function () {
		return ' <b class="uniqueCount">' + ((this.params.uniqueCount > 1) ? '(' + this.params.uniqueCount + ') ' : '') + '</b> ';
	};

	this.incrementUniqueCount = function () {
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

function showLoadingSpinner() {
	var msg = '<div id="structr-loading-spinner"><img src="' + _Icons.getSpinnerImageAsData() + '"></div>';
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
}

function hideLoadingSpinner() {
	$.unblockUI({
		fadeOut: 0
	});
}