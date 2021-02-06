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
var ws;
var loggedIn = false, isAdmin = false;
var user, me, userId;
var reconn, ping;

var rawResultCount = [];
var pageCount = [];
var page = 1;
var pageSize = 25;
var sort = 'name';
var order = 'asc';

var rootUrl = '/structr/rest/';
var csvRootUrl = '/structr/csv/';
var viewRootUrl = '/';
var wsRoot = '/structr/ws';
var port = document.location.port;

function wsConnect() {

	try {

		if (!ws || ws.readyState > 1) {
			// closed websocket

			if (ws) {
				ws.onopen    = undefined;
				ws.onclose   = undefined;
				ws.onmessage = undefined;
				ws = undefined;
			}

			var isEnc = (window.location.protocol === 'https:');
			var host = document.location.host;
			var wsUrl = 'ws' + (isEnc ? 's' : '') + '://' + host + wsRoot;

			if ('WebSocket' in window) {

				try {
					ws = new WebSocket(wsUrl, 'structr');
				} catch (e) {}

			} else if ('MozWebSocket' in window) {

				try {
					ws = new MozWebSocket(wsUrl, 'structr');
				} catch (e) {}

			} else {

				alert('Your browser doesn\'t support WebSocket.');
				return false;

			}
		}

		ws.onopen = function () {

			if ($.unblockUI) {
				$.unblockUI({
					fadeOut: 25
				});
			}

			Structr.stopReconnect();
			Structr.init();
		};

		ws.onclose = function () {

			if (reconn) {
				return;
			}

			// Delay reconnect dialog to prevent it popping up before page reload
			window.setTimeout(function () {

				main.empty();

				var restoreDialogText = '';
				var dialogData = JSON.parse(LSWrapper.getItem(dialogDataKey));
				if (dialogData && dialogData.text) {
					restoreDialogText = '<br><br>The dialog<br><b>"' + dialogData.text + '"</b><br> will be restored after reconnect.';
				}
				Structr.reconnectDialog('<b>Connection lost or timed out.</b><br><br>Don\'t reload the page!' + restoreDialogText + '<br><br>Trying to reconnect... <img class="al" src="' + _Icons.getSpinnerImageAsData() + '">');

				Structr.reconnect();
			}, 100);
		};

		ws.onmessage = function (message) {

			var data = JSON.parse(message.data);
			var type = data.data.type;
			var command = data.command;
			var msg = data.message;
			var result = data.result;
			var sessionValid = data.sessionValid;
			var code = data.code;

			if (command === 'LOGIN' || code === 100) {

				if (command === 'LOGIN' || !userId) {
					Command.rest("/me", function (result) {
						var me = result[0];
						userId = me.id;
					});
				}

				me = data.data;
				isAdmin = data.data.isAdmin;

				if (!sessionValid) {
					Structr.clearMain();
					Structr.login(msg);
				} else if (!user || user !== data.data.username || loginBox.is(':visible')) {
					Structr.updateUsername(data.data.username);
					loginBox.hide();
					Structr.clearLoginForm();
					$('table.username-password', loginBox).show();
					$('table.twofactor', loginBox).hide();
					Structr.refreshUi((command === 'LOGIN'));
				}

				StructrModel.callCallback(data.callback, data.data[data.data['key']]);

			} else if (command === 'GET_LOCAL_STORAGE') {

				if (data.data.localStorageString && data.data.localStorageString.length) {
					LSWrapper.setAsJSON(data.data.localStorageString);
				}

				StructrModel.callCallback(data.callback, data.data);

			} else if (command === 'CONSOLE') {

				StructrModel.callCallback(data.callback, data);

			} else if (command === 'STATUS') {

				if (code === 403) {
					user = null;
					userId = null;
					if (data.data.reason === 'sessionLimitExceeded') {
						Structr.login('Max. number of sessions exceeded.');
					} else {
						Structr.login('Wrong username or password!');
					}
				} else if (code === 401) {
					user = null;
					userId = null;
					if (data.data.reason === 'twofactortoken') {
						Structr.clearLoginForm();
						$('table.username-password', loginBox).show();
						$('table.twofactor', loginBox).hide();
					}
					Structr.login((msg !== null) ? msg : '');

				} else if (code === 202) {
					user = null;
					userId = null;
					Structr.login('');

					Structr.toggle2FALoginBox(data.data);

				} else {

					var codeStr = code ? code.toString() : '';

					if (codeStr === '422') {
						try {
							StructrModel.callCallback(data.callback, null, null, true);
						} catch (e) {}
					}

					var msgClass;
					var requiresConfirmation = false;
					if (codeStr.startsWith('2')) {
						msgClass = 'success';
					} else if (codeStr.startsWith('3')) {
						msgClass = 'info';
					} else if (codeStr.startsWith('4')) {
						msgClass = 'warning';
						requiresConfirmation = true;
					} else {
						msgClass = 'error';
						requiresConfirmation = true;
					}

					if (data.data.requiresConfirmation) {
						requiresConfirmation = data.data.requiresConfirmation;
					}

					if (msg && msg.startsWith('{')) {

						var msgObj = JSON.parse(msg);

						if (dialogBox.is(':visible')) {

							Structr.showAndHideInfoBoxMessage(msgObj.size + ' bytes saved to ' + msgObj.name, msgClass, 2000, 200);

						} else {

							var node = Structr.node(msgObj.id);

							if (node) {

								var progr = node.find('.progress');
								progr.show();

								var size = parseInt(node.find('.size').text());
								var part = msgObj.size;

								node.find('.part').text(part);
								var pw = node.find('.progress').width();
								var w = pw / size * part;

								node.find('.bar').css({width: w + 'px'});

								if (part >= size) {
									blinkGreen(progr);
									window.setTimeout(function () {
										progr.fadeOut('fast');
										_Files.resize();
									}, 1000);
								}
							}
						}

					} else {

						if (codeStr === "404") {

							var msgBuilder = new MessageBuilder().className(msgClass);

							if (requiresConfirmation) {
								msgBuilder.requiresConfirmation();
							}

							if (data.message) {
								msgBuilder.title('Object not found.').text(data.message);
							} else {
								msgBuilder.text('Object not found.');
							}

							msgBuilder.show();

						} else if (data.error && data.error.errors) {

							Structr.errorFromResponse(data.error, null, { requiresConfirmation: true });

						} else {

							var msgBuilder = new MessageBuilder().className(msgClass).text(msg);

							if (requiresConfirmation) {
								msgBuilder.requiresConfirmation();
							}

							msgBuilder.show();
						}
					}
				}

			} else if (command === 'GET_PROPERTY') {

				StructrModel.updateKey(data.id, data.data['key'], data.data[data.data['key']]);
				StructrModel.callCallback(data.callback, data.data[data.data['key']]);

			} else if (command === 'UPDATE' || command === 'SET_PERMISSION') {

				var obj = StructrModel.obj(data.id);

				if (!obj) {
					data.data.id = data.id;
					obj = StructrModel.create(data.data, null, false);
				}

				StructrModel.update(data);

			} else if (command === 'GET' || command === 'GET_RELATIONSHIP' || command === 'GET_PROPERTIES') {

				StructrModel.callCallback(data.callback, result[0]);

			} else if (command.startsWith('GET') || command === 'GET_BY_TYPE' || command === 'GET_SCHEMA_INFO' || command === 'CREATE_RELATIONSHIP') {

				StructrModel.callCallback(data.callback, result);

			} else if (command === 'CHILDREN') {

				if (result.length > 0 && result[0].name) {
					result.sort(function (a, b) {
						return a.name.localeCompare(b.name);
					});
				}

				var refObject = StructrModel.obj(data.id);

				if (refObject && refObject.constructor.name === 'StructrGroup') {
					result.forEach(function (entity) {
						StructrModel.create(entity, data.id);
					});
				} else {
					result.forEach(function (entity) {
						StructrModel.create(entity);
					});
				}
				StructrModel.callCallback(data.callback, result);

			} else if (command.endsWith('CHILDREN')) {

				$(result).each(function (i, entity) {
					StructrModel.create(entity);
				});

				StructrModel.callCallback(data.callback, result);

			} else if (command.startsWith('SEARCH')) {

				$('.pageCount', $('.pager' + type)).val(pageCount[type]);

				StructrModel.callCallback(data.callback, result, data.rawResultCount);

			} else if (command.startsWith('LIST_UNATTACHED_NODES')) {

				StructrModel.callCallback(data.callback, result);

			} else if (command.startsWith('LIST_SCHEMA_PROPERTIES')) {

				// send full result in a single callback
				StructrModel.callCallback(data.callback, result);

			} else if (command.startsWith('LIST_COMPONENTS')) {

				StructrModel.callCallback(data.callback, result);

			} else if (command.startsWith('LIST_SYNCABLES')) {

				StructrModel.callCallback(data.callback, result);

			} else if (command.startsWith('LIST_ACTIVE_ELEMENTS')) {

				StructrModel.callCallback(data.callback, result);

			} else if (command.startsWith('LIST_LOCALIZATIONS')) {

				StructrModel.callCallback(data.callback, result);

			} else if (command.startsWith('SNAPSHOTS')) {

				StructrModel.callCallback(data.callback, result);

			} else if (command.startsWith('LIST')) {

				StructrModel.callCallback(data.callback, result, data.rawResultCount);

			} else if (command.startsWith('QUERY')) {

				StructrModel.callCallback(data.callback, result, data.rawResultCount);

			} else if (command.startsWith('CLONE') || command === 'REPLACE_TEMPLATE') {

				StructrModel.callCallback(data.callback, result, data.rawResultCount);

			} else if (command === 'DELETE') {

				StructrModel.del(data.id);

				StructrModel.callCallback(data.callback, [], 0);

			} else if (command === 'INSERT_BEFORE' || command === 'APPEND_CHILD' || command === 'APPEND_MEMBER') {

				StructrModel.create(result[0], data.data.refId);

				StructrModel.callCallback(data.callback, result[0]);

			} else if (command.startsWith('APPEND_FILE')) {

				//StructrModel.create(result[0], data.data.refId);

			} else if (command === 'REMOVE') {

				var obj = StructrModel.obj(data.id);
				if (obj) {
					obj.remove();
				}

				StructrModel.callCallback(data.callback);

			} else if (command === 'REMOVE_CHILD') {

				var obj = StructrModel.obj(data.id);
				if (obj) {
					obj.remove(data.data.parentId);
				}

				StructrModel.callCallback(data.callback);

			} else if (command === 'CREATE' || command === 'ADD' || command === 'IMPORT') {

				$(result).each(function (i, entity) {
					if (command === 'CREATE' && (entity.isPage || entity.isFolder || entity.isFile || entity.isImage || entity.isVideo || entity.isUser || entity.isGroup || entity.isWidget || entity.isResourceAccess)) {
						StructrModel.create(entity);
					} else {

						if (!entity.parent && shadowPage && entity.pageId === shadowPage.id) {

							entity = StructrModel.create(entity, null, false);
							var el;
							if (entity.isContent || entity.type === 'Template') {
								el = _Elements.appendContentElement(entity, components, true);
							} else {
								el = _Pages.appendElementElement(entity, components, true);
							}

							if (Structr.isExpanded(entity.id)) {
								_Entities.ensureExpanded(el);
							}

							var synced = entity.syncedNodesIds;

							if (synced && synced.length) {

								// Change icon
								$.each(synced, function (i, id) {
									var el = Structr.node(id);
									if (el && el.length) {
										var icon = entity.isTemplate ? _Icons.icon_shared_template : (entity.isContent ? _Icons.active_content_icon : _Icons.comp_icon);
										el.children('.typeIcon').attr('class', 'typeIcon ' + _Icons.getFullSpriteClass(icon));
										_Entities.removeExpandIcon(el);
									}
								});
							}
						}
					}

					if (command === 'CREATE' && entity.isPage && lastMenuEntry === _Pages._moduleName) {
						if (entity.createdBy === userId) {
							setTimeout(function () {
								var tab = $('#show_' + entity.id);
								_Pages.activateTab(tab);
							}, 1000);
						}
					} else if (entity.pageId) {
						_Pages.reloadIframe(entity.pageId);
					}

					StructrModel.callCallback(data.callback, entity);

				});

			} else if (command === 'PROGRESS') {

				if (dialogMsg.is(':visible')) {
					var msgObj = JSON.parse(data.message);
					dialogMsg.html('<div class="infoBox info">' + msgObj.message + '</div>');
				}

			} else if (command === 'FINISHED') {

				StructrModel.callCallback(data.callback, data.data);

			} else if (command === 'AUTOCOMPLETE') {

				StructrModel.callCallback(data.callback, result);

			} else if (command === 'FIND_DUPLICATES') {

				StructrModel.callCallback(data.callback, result);

			} else if (command === 'SCHEMA_COMPILED') {

				_Schema.processSchemaRecompileNotification();

			} else if (command === 'GENERIC_MESSAGE') {

				Structr.handleGenericMessage(data.data);

			} else if (command === 'FILE_IMPORT') {

				StructrModel.callCallback(data.callback, result);

			} else if (command === 'GET_SUGGESTIONS') {

				StructrModel.callCallback(data.callback, result);

			} else if (command === 'SERVER_LOG') {

				StructrModel.callCallback(data.callback, result);

			} else if (command === 'SAVE_LOCAL_STORAGE') {

				StructrModel.callCallback(data.callback, result);

			} else if (command === 'APPEND_WIDGET') {

				StructrModel.callCallback(data.callback, result);

			} else {

				console.log('Received unknown command: ' + command);

				if (sessionValid === false) {
					user = null;
					userId = null;
					clearMain();

					Structr.login();
				}
			}
		};

	} catch (exception) {
		if (ws) {
			ws.close();
			ws.length = 0;
		}
	}
}

function sendObj(obj, callback) {

	if (callback) {
		obj.callback = uuid.v4();
		StructrModel.callbacks[obj.callback] = callback;
	}

	var t = JSON.stringify(obj);

	if (!t) {
		return false;
	}

	try {
		ws.send(t);
	} catch (exception) {
		// console.log('Error in send(): ' + exception);
	}
	return true;
}

function send(text) {

	var obj = JSON.parse(text);
	return sendObj(obj);
}

function getAnchorFromUrl(url) {
	if (url) {
		var pos = url.lastIndexOf('#');
		if (pos > 0) {
			return url.substring(pos + 1, url.length);
		}
	}
	return null;
}