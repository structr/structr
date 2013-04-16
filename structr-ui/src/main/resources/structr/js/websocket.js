/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
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
var token;
var loggedIn = false;
var user;
var reconn;
var port = document.location.port;

var rawResultCount = [];
var pageCount = [];
var page = 1;
var pageSize = 25;
var sort = 'name';
var order = 'asc';

var tokenCookieName = 'structrSessionToken_' + port;
var userCookieName = 'structrUser_' + port;

var footer = $('#footer');

function connect() {

    if (token) {
        loggedIn = true;
    }

    try {
        
        ws = null;
        
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
            
	    if ($.unblockUI) {
		$.unblockUI({
		    fadeOut: 25
		});
	    }
            
            log('de-activating reconnect loop', reconn);
            window.clearInterval(reconn);

            log('logged in? ' + loggedIn);
            if (!loggedIn) {
                //log('no');
                $('#logout_').html('Login');
                Structr.login();
            } else {
                log('Current user: ' + user);
                $('#logout_').html(' Logout <span class="username">' + (user ? user : '') + '</span>');
				
                Structr.loadInitialModule();
				
            }
        }

        ws.onclose = function() {
            
            if (reconn) {
                log('Automatic reconnect already active');
                return;
            }
            
            main.empty();
            //Structr.confirmation('Connection lost or timed out.<br>Reconnect?', Structr.silenctReconnect);
            Structr.reconnectDialog('Connection lost or timed out. Trying to reconnect ...');
            //log('Connection was lost or timed out. Trying automatic reconnect');
            log('ws onclose');
            Structr.reconnect();
            
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
            
            console.log('####################################### ', command, ' #########################################');
            
            rawResultCount[type] = data.rawResultCount;
            pageCount[type] = Math.ceil(rawResultCount[type] / pageSize[type]);
            Structr.updatePager(type);

            if (command === 'LOGIN') { /*********************** LOGIN ************************/
                token = data.token;
                user = data.data.username;
                log('token', token);
		
                if (sessionValid) {
                    $.cookie(tokenCookieName, token);
                    $.cookie(userCookieName, user);
                    $.unblockUI({
                        fadeOut: 25
                    });
                    $('#logout_').html('Logout <span class="username">' + user + '</span>');

                    Structr.loadInitialModule();
					
                } else {
                    $.cookie(tokenCookieName, '');
                    $.cookie(userCookieName, '');
                    clearMain();

                    Structr.login();
                }

            } else if (command === 'LOGOUT') { /*********************** LOGOUT ************************/

                $.cookie(tokenCookieName, '');
                $.cookie(userCookieName, '');
                clearMain();
                Structr.login();

            } else if (command === 'STATUS') { /*********************** STATUS ************************/
                //console.log('Error code: ' + code);
				
                if (code === 403) {
                    Structr.login('Wrong username or password!');
                } else if (code === 401) {
                    Structr.login('Session invalid');
                } else {
                    
                    var msgClass;
                    var codeStr = code.toString();
                    
                    if (codeStr.startsWith('20')) {
                        msgClass = 'success';
                        if (dialogBox.is(':visible')){
                            dialogMsg.html('<div class="infoBox ' + msgClass + '">' + msg + '</div>');
                            $('.infoBox', dialogMsg).delay(2000).fadeOut(200);
                        } else {
                            Structr.tempInfo('', true);
                            $('#tempInfoBox .infoMsg').html('<div class="infoBox ' + msgClass + '">' + msg + '</div>');
                        }
                    } else if (codeStr.startsWith('30')) {
                        Structr.dialog('', function() {}, function() {});
                        msgClass = 'info';
                        dialogMsg.html('<div class="infoBox ' + msgClass + '">' + msg + '</div>');
                    } else if (codeStr.startsWith('40')) {
                        //Structr.dialog('', function() {}, function() {});
                        Structr.tempInfo('', true);
                        msgClass = 'warning';
                        $('#tempInfoBox .infoMsg').html('<div class="infoBox ' + msgClass + '">' + msg + '</div>');
                    } else {
                        Structr.error('', function() {}, function() {});
                        msgClass = 'error';
                        $('#errorBox .errorMsg').html('<div class="infoBox ' + msgClass + '">' + msg + '</div>');
                    }

                }

            } else if (command === 'GET_PROPERTY') { /*********************** GET_PROPERTY ************************/
                
                log('GET_PROPERTY', data.data['key']);
                StructrModel.updateKey(id, key, val);
                
            } else if (command === 'GET_PROPERTIES' || command === 'UPDATE') { /*********************** GET_PROPERTIES / UPDATE ************************/
                
                log('UPDATE', data);
                var id = data.id;
                var key = data.data['key'];
                var val = data.data[key];
                log('calling StructrModel.update(id, key, val)', id, key, val);
                StructrModel.update(data);
                
//            } else if (command === 'DATA_NODE_PARENT') { /*********************** DATA_NODE_PARENT ************************/
//                
//                log('DATA_NODE_PARENT', data.id, result);
//                var obj = StructrModel.obj(data.id);
//                obj.parentId = result.length && result[0].id;
//                if (data.callback) {
//                    log('executing callback with id', data.callback);
//                    StructrModel.callbacks[data.callback]();
//                }
//                
            } else if (command.endsWith('CHILDREN')) { /*********************** CHILDREN ************************/
                
                log('CHILDREN', data);
                
                $(result).each(function(i, entity) {
                    
//                    if (entity.type === 'DataNode') {
//                        //console.log('DataNode', entity, data.nodesWithChildren, isIn(entity.id, data.nodesWithChildren));
//                        entity.hasChildren = isIn(entity.id, data.nodesWithChildren);
//                    }

                    StructrModel.create(entity);
                    
                });
                
            } else if (command.startsWith('SEARCH')) { /*********************** SEARCH ************************/
                
                //console.log('SEARCH', result);
                
                $('.pageCount', $('#pager' + type)).val(pageCount[type]);
                
                $(result).each(function(i, entity) {
                    
                    StructrModel.createSearchResult(entity);
                    
                });
                
            } else if (command.startsWith('LIST')) { /*********************** LIST ************************/
                
                log('LIST', result);
                
                $('.pageCount', $('#pager' + type)).val(pageCount[type]);
                
                $(result).each(function(i, entity) {
                    
                    StructrModel.create(entity);
                    
                });
                
            } else if (command === 'DELETE') { /*********************** DELETE ************************/
                
                StructrModel.del(data.id);
                
            } else if (command === 'INSERT_BEFORE') { /*********************** INSERT_BEFORE ************************/
            
                StructrModel.create(result[0], data.data.refId);
                
            } else if (command === 'APPEND_CHILD') { /*********************** APPEND_CHILD ************************/
            
                StructrModel.create(result[0]);

            } else if (command === 'APPEND_USER') { /*********************** APPEND_USER ************************/
            
                StructrModel.create(result[0]);

            } else if (command === 'REMOVE' || command === 'REMOVE_CHILD') { /*********************** REMOVE / REMOVE_CHILD ************************/

                StructrModel.obj(data.id).remove();
                
            } else if (command === 'CREATE' || command === 'ADD' || command === 'IMPORT') { /*********************** CREATE, ADD, IMPORT ************************/
                
                $(result).each(function(i, entity) {
                    
                    if (command === 'CREATE' && (entity.type === 'Page' || entity.type === 'Folder' || entity.type === 'File' || entity.type === 'Image' || entity.type === 'User' || entity.type === 'Group' || entity.type === 'PropertyDefinition')) {
                        StructrModel.create(entity);
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

                _Pages.reloadPreviews();
                
            } else {
                log('Received unknown command: ' + command);

                if (sessionValid === false) {
                    log('invalid session');
                    $.cookie(tokenCookieName, '');
                    $.cookie(userCookieName, '');
                    clearMain();

                    Structr.login();
                }
            }
        }

    } catch (exception) {
        log('Error in connect(): ' + exception);
    }

}

function sendObj(obj, callback) {

    if (token) {
        obj.token = token;
    }
    
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
        return false;
    }
    return true;
}

function send(text) {

    log(ws.readyState);

    var obj = $.parseJSON(text);

    return sendObj(obj);
}

function log(messages) {
    if (debug) {
        console.log(messages);
        $.each(Array.prototype.slice.apply(messages), function(i, msg) {
            if (footer) {
                var div = $('#log', footer);
                div.append('<p>' + msg + '</p>');
                footer.scrollTop(div.height());
            }
            
        });
    }
}

function getAnchorFromUrl(url) {
    if (url) {
        var pos = url.lastIndexOf('#');
        if (pos > 0) {
            return url.substring(pos+1, url.length);
        }
    }
    return null;
}


function utf8_to_b64( str ) {
    return window.btoa(unescape(encodeURIComponent( str )));
    //return window.btoa(str);
}

function b64_to_utf8( str ) {
    return decodeURIComponent(escape(window.atob( str )));
    //return window.atob(str);
}
