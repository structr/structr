/*
 *  Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

var ws;
var loggedIn = false;
var user;
var reconn, ping;
var port = document.location.port;

var rawResultCount = [];
var pageCount = [];
var page = 1;
var pageSize = 25;
var sort = 'name';
var order = 'asc';

var userKey = 'structrUser_' + port;

var footer = $('#footer');

function wsConnect() {

    log('################ Global connect() ################');

    try {

        ws = undefined;
        localStorage.removeItem(userKey);

        var isEnc = (window.location.protocol === 'https:');
        var host = document.location.host;
        var wsUrl = 'ws' + (isEnc ? 's' : '') + '://' + host + wsRoot;

        log(wsUrl);
        if ('WebSocket' in window) {

            ws = new WebSocket(wsUrl, 'structr');

        } else if ('MozWebSocket' in window) {

            ws = new MozWebSocket(wsUrl, 'structr');

        } else {

            alert('Your browser doesn\'t support WebSocket.');
            return false;

        }

        log('WebSocket.readyState: ' + ws.readyState, ws);

        ws.onopen = function() {

            log('############### WebSocket onopen ###############');

            if ($.unblockUI) {
                $.unblockUI({
                    fadeOut: 25
                });
            }

            log('de-activating reconnect loop', reconn);
            window.clearInterval(reconn);
            reconn = undefined;

            Structr.init();

        }

        ws.onclose = function() {

            log('############### WebSocket onclose ###############', reconn);

            if (reconn) {
                log('Automatic reconnect already active');
                return;
            }

            // Delay reconnect dialog to prevent it popping up before page reload
            window.setTimeout(function() {

                main.empty();
                //Structr.confirmation('Connection lost or timed out.<br>Reconnect?', Structr.silenctReconnect);
                var restoreDialogText = '';
                var dialogData = JSON.parse(localStorage.getItem(dialogDataKey));
                if (dialogData && dialogData.text) {
                    restoreDialogText = '<br><br>The dialog<br><b>"' + dialogData.text + '"</b><br> will be restored after reconnect.';
                }
                Structr.reconnectDialog('<b>Connection lost or timed out.</b><br><br>Don\'t reload the page!' + restoreDialogText + '<br><br>Trying to reconnect... <img class="al" src="data:image/gif;base64,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" alt="">');
                //log('Connection was lost or timed out. Trying automatic reconnect');
                log('ws onclose');
                Structr.reconnect();

            }, 100);

        }

        ws.onmessage = function(message) {

            var data = $.parseJSON(message.data);
            log('ws.onmessage:', data);

            //var msg = $.parseJSON(message);
            var type = data.data.type;
            var command = data.command;
            var msg = data.message;
            var result = data.result;
            var sessionValid = data.sessionValid;
            var code = data.code;

            log('####################################### ', command, ' #########################################');

            if (command === 'LOGIN' || code === 100) { /*********************** LOGIN or response to PING ************************/

                user = data.data.username;
                var oldUser = localStorage.getItem(userKey);

                log(command, code, 'user:', user, ', oldUser:', oldUser, 'session valid:', sessionValid);

                if (!sessionValid) {
                    localStorage.removeItem(userKey);
                    Structr.clearMain();
                    Structr.login();
                } else if (!oldUser || (oldUser && (oldUser !== user)) || loginBox.is(':visible')) {
                    Structr.refreshUi();
                }

            } else if (command === 'LOGOUT') { /*********************** LOGOUT ************************/

                localStorage.removeItem(userKey);
                Structr.clearMain();
                Structr.login();

            } else if (command === 'STATUS') { /*********************** STATUS ************************/
                log('Error code: ' + code, message);

                if (code === 403) {
                    Structr.login('Wrong username or password!');
                } else if (code === 401) {
                    localStorage.removeItem(userKey);
                    Structr.clearMain();
                    Structr.login('Session invalid');
                } else {

                    var msgClass;
                    var codeStr = code.toString();

                    if (codeStr.startsWith('2')) {
                        msgClass = 'success';
                    } else if (codeStr.startsWith('3')) {
                        msgClass = 'info';
                    } else if (codeStr.startsWith('4')) {
                        msgClass = 'warning';
                    } else {
                        msgClass = 'error';
                    }

                    if (msg && msg.startsWith('{')) {

                        var msgObj = JSON.parse(msg);

                        if (dialogBox.is(':visible')) {

                            dialogMsg.html('<div class="infoBox ' + msgClass + '">' + msgObj.size + ' bytes saved to ' + msgObj.name + '</div>');
                            $('.infoBox', dialogMsg).delay(2000).fadeOut(200);

                        } else {

                            var node = Structr.node(msgObj.id);
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
                                window.setTimeout(function() {
                                    progr.fadeOut('fast');
                                }, 1000);
                            }
                        }

                    } else {

                        if (dialogBox.is(':visible')) {
                            dialogMsg.html('<div class="infoBox ' + msgClass + '">' + msg + '</div>');
                            $('.infoBox', dialogMsg).delay(2000).fadeOut(200);
                        } else {
                            Structr.tempInfo('', true);
                            $('#tempInfoBox .infoMsg').html('<div class="infoBox ' + msgClass + '">' + msg + '</div>');
                        }
                    }

                }

            } else if (command === 'GET_PROPERTY') { /*********************** GET_PROPERTY ************************/

                log('GET_PROPERTY', data.id, data.data['key'], data.data[data.data['key']]);
                StructrModel.updateKey(data.id, data.data['key'], data.data[data.data['key']]);
                StructrModel.callCallback(data.callback, data.data[data.data['key']]);
                StructrModel.clearCallback(data.callback);

            } else if (command === 'UPDATE' || command === 'SET_PERMISSION') { /*********************** UPDATE / SET_PERMISSION ************************/

                var obj = StructrModel.obj(data.id);

                if (!obj) {
                    data.data.id = data.id;
                    obj = StructrModel.create(data.data, null, false);
                }

                obj = StructrModel.update(data);

                if (StructrModel.callCallback(data.callback, obj)) {
                    StructrModel.clearCallback(data.callback);
                }

            } else if (command.startsWith('GET') || command === 'GET_BY_TYPE') { /*********************** GET_BY_TYPE ************************/

                log(command, data);

                $(result).each(function(i, entity) {

                    // Don't append a DOM node
                    //var obj = StructrModel.create(entity, undefined, false);

                    StructrModel.callCallback(data.callback, entity);

                });

                StructrModel.clearCallback(data.callback);

            } else if (command.endsWith('CHILDREN')) { /*********************** CHILDREN ************************/

                log('CHILDREN', data);

                $(result).each(function(i, entity) {

                    StructrModel.create(entity);
                    StructrModel.callCallback(data.callback, entity);

                });

                StructrModel.clearCallback(data.callback);

            } else if (command.startsWith('SEARCH')) { /*********************** SEARCH ************************/

                $('.pageCount', $('.pager' + type)).val(pageCount[type]);

                $(result).each(function(i, entity) {

                    StructrModel.createSearchResult(entity);

                });

            } else if (command.startsWith('LIST_UNATTACHED_NODES')) { /*********************** LIST_UNATTACHED_NODES ************************/

                log('LIST_UNATTACHED_NODES', result, data);

                $(result).each(function(i, entity) {

                    StructrModel.callCallback(data.callback, entity);

                });

                StructrModel.clearCallback(data.callback);

            } else if (command.startsWith('LIST_COMPONENTS')) { /*********************** LIST_COMPONENTS ************************/

                log('LIST_COMPONENTS', result, data);

                $(result).each(function(i, entity) {

                    StructrModel.callCallback(data.callback, entity);

                });

                StructrModel.clearCallback(data.callback);

            } else if (command.startsWith('LIST_SYNCABLES')) { /*********************** LIST_SYNCABLES ************************/

                log(data);

                log('LIST_SYNCABLES', result, data);

                $(result).each(function(i, entity) {

                    StructrModel.callCallback(data.callback, entity);

                });

                StructrModel.clearCallback(data.callback);

            } else if (command.startsWith('LIST_ACTIVE_ELEMENTS')) { /*********************** LIST_ACTIVE_ELEMENTS ************************/

                log('LIST_ACTIVE_ELEMENTS', result, data);

                $(result).each(function(i, entity) {

                    StructrModel.callCallback(data.callback, entity);

                });

                StructrModel.clearCallback(data.callback);

            } else if (command.startsWith('LIST')) { /*********************** LIST ************************/

                log('LIST', result, data);

                rawResultCount[type] = data.rawResultCount;
                pageCount[type] = Math.max(1, Math.ceil(rawResultCount[type] / pageSize[type]));
                Structr.updatePager(type, dialog.is(':visible') ? dialog : undefined);

                $('.pageCount', $('.pager' + type)).val(pageCount[type]);

                $(result).each(function(i, entity) {

                    //var obj = StructrModel.create(entity);
                    StructrModel.callCallback(data.callback, entity);

                });

                StructrModel.clearCallback(data.callback);

            } else if (command === 'DELETE') { /*********************** DELETE ************************/

                StructrModel.del(data.id);

            } else if (command === 'INSERT_BEFORE') { /*********************** INSERT_BEFORE ************************/

                StructrModel.create(result[0], data.data.refId);

            } else if (command.startsWith('APPEND_')) { /*********************** APPEND_* ************************/

                StructrModel.create(result[0]);

            } else if (command === 'REMOVE' || command === 'REMOVE_CHILD') { /*********************** REMOVE / REMOVE_CHILD ************************/

                var obj = StructrModel.obj(data.id);
                if (obj) {
                    obj.remove();
                }

            } else if (command === 'CREATE' || command === 'ADD' || command === 'IMPORT') { /*********************** CREATE, ADD, IMPORT ************************/

                $(result).each(function(i, entity) {

                    if (command === 'CREATE' && (entity.type === 'Page' || entity.type === 'Folder' || entity.type === 'File' || entity.type === 'Image' || entity.type === 'User' || entity.type === 'Group' || entity.type === 'PropertyDefinition' || entity.type === 'Widget')) {
                        StructrModel.create(entity);
                    } else {

                        if (!entity.parent && shadowPage && entity.pageId === shadowPage.id) {

                            StructrModel.create(entity, null, false);
                            var el = _Pages.appendElementElement(entity, components, true);

                            if (isExpanded(entity.id)) {
                                _Entities.ensureExpanded(el);
                            }

                            var synced = entity.syncedNodes;

                            if (synced && synced.length) {

                                // Change icon
                                $.each(entity.syncedNodes, function(i, id) {
                                    var el = Structr.node(id);
                                    if (el && el.length) {
                                        el.children('img.typeIcon').attr('src', _Elements.icon_comp);
                                        _Entities.removeExpandIcon(el);
                                    }
                                });

                            }
                        }
                    }

                    if (command === 'CREATE' && entity.type === 'Page') {
                        var tab = $('#show_' + entity.id, previews);
                        setTimeout(function() {
                            _Pages.activateTab(tab)
                        }, 2000);
                    } else if (command === 'CREATE' && (entity.type === 'File' || entity.type === 'Image')) {
                        _Files.uploadFile(entity);
                    }

                });

                if (!localStorage.getItem(autoRefreshDisabledKey + activeTab)) {
                    _Pages.reloadPreviews();
                }
            } else if (command === 'PROGRESS') { /*********************** PROGRESS ************************/

                if (dialogMsg.is(':visible')) {
                    var msgObj = JSON.parse(data.message);
                    dialogMsg.html('<div class="infoBox info">Transferred ' + msgObj.current + ' of ' + msgObj.total + ' objects</div>');
                }

            } else if (command === 'FINISHED') { /*********************** FINISHED ************************/

                StructrModel.callCallback(data.callback);
                StructrModel.clearCallback(data.callback);

            } else {
                console.log('Received unknown command: ' + command);

                if (sessionValid === false) {
                    log('invalid session');
                    localStorage.removeItem(userKey);
                    clearMain();

                    Structr.login();
                }
            }
        }

    } catch (exception) {
        log('Error in connect(): ' + exception);
        ws.close();
    }

}

function sendObj(obj, callback) {

    if (callback) {
        obj.callback = uuid.v4();
        StructrModel.callbacks[obj.callback] = callback;
        log('stored callback', obj.callback, callback);
    }

    text = $.toJSON(obj);

    if (!text) {
        log('No text to send!');
        return false;
    }

    try {
        ws.send(text);
        log('Sent: ' + text);
    } catch (exception) {
        log('Error in send(): ' + exception);
        //Structr.ping();
    }
    return true;
}

function send(text) {

    log(ws.readyState);

    var obj = $.parseJSON(text);

    return sendObj(obj);
}

function log() {
    if (debug) {
        log(arguments);
        var msg = Array.prototype.slice.call(arguments).join(' ');
        var div = $('#log', footer);
        div.append(msg + '<br>');
        footer.scrollTop(div.height());
    }
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


function utf8_to_b64(str) {
    return window.btoa(unescape(encodeURIComponent(str)));
    //return window.btoa(str);
}

function b64_to_utf8(str) {
    return decodeURIComponent(escape(window.atob(str)));
    //return window.atob(str);
}
