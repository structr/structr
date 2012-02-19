/*
 *  Copyright (C) 2011 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


var ws;
var token;
var loggedIn = false;
var user;

function connect() {

    token = $.cookie('structrSessionToken');
    if (debug) console.log('token: ' + token);

    if (token) {
        loggedIn = true;
    }

    try {
        var host = document.location.host;
        var wsUrl = 'ws://' + host + wsRoot;
        if (debug) console.log(wsUrl);
        if ('WebSocket' in window) {
            ws = new WebSocket(wsUrl, 'structr');
        } else if ('MozWebSocket' in window) {
            ws = new MozWebSocket(wsUrl, 'structr');
        } else {
            alert('Your browser doesn\'t support WebSocket.');
            return;
        }

        log('State: ' + ws.readyState);
		
        var parentId, entityId;
        var parent, entity;
		

        ws.onmessage = function(message) {

            var data = $.parseJSON(message.data);
            if (debug) console.log(data);

            //var msg = $.parseJSON(message);
            var type = data.type;
            var command = data.command;
            var result = data.result;
            var sessionValid = data.sessionValid;
            var code = data.code;
            var callback = data.callback;

            if (debug) console.log('command: ' + command);
            if (debug) console.log('type: ' + type);
            if (debug) console.log('code: ' + code);
            if (debug) console.log('callback: ' + callback);
            if (debug) console.log('sessionValid: ' + sessionValid);

            if (debug) console.log('result: ' + $.toJSON(result));

            if (command == 'LOGIN') {
		
                var token = data.token;
                if (debug) console.log('token: ' + token);
		
                if (sessionValid) {
                    $.cookie('structrSessionToken', token);
                    $.cookie('structrUser', user);
                    $.unblockUI();
                    $('#logout_').html('Logout <span class="username">' + user + '</span>');

                    //Structr.init();
					
                } else {
                    $.cookie('structrSessionToken', '');
                    $.cookie('structrUser', '');
                    clearMain();

                    Structr.login();
                }

            } else if (command == 'LOGOUT') {

                $.cookie('structrSessionToken', '');
                $.cookie('structrUser', '');
                clearMain();
                Structr.login();

            } else if (command == 'STATUS') {
                if (debug) console.log('Error code: ' + code);
				
                if (code == 403) {
                    Structr.login('Wrong username or password!');
                } else if (code == 401) {
                    Structr.login('Session invalid');
                }

            } else if (command == 'CREATE') {
				
                $(result).each(function(i, entity) {
                    if (entity.type == 'User') {

                        groupId = entity.groupId;
                        if (groupId) _UsersAndGroups.appendUserElement(entity, groupId);
                        _UsersAndGroups.appendUserElement(entity);
                        disable($('.' + groupId + '_ .delete_icon')[0]);
                        if (buttonClicked) enable(buttonClicked);

                    } else if (entity.type == 'Group') {

                        _UsersAndGroups.appendGroupElement(entity);
                        if (debug) console.log('Group element appended');
                        if (buttonClicked) enable(buttonClicked);

                    } else if (entity.type == 'Resource') {

                        _Resources.appendResourceElement(entity);
                        _Resources.reloadPreviews();
                        if (debug) console.log('Resource element appended');
                        if (buttonClicked) enable(buttonClicked);

                    } else if (entity.type == 'Content') {
                        if (resources) {
                            _Resources.appendContentElement(entity);
                        } else {
                            _Contents.appendContentElement(entity);
                        }
                        if (debug) console.log('Content element appended');
                        if (buttonClicked) enable(buttonClicked);

                    } else if (entity.type == 'Folder') {

                        _Files.appendFolderElement(entity);
                        if (debug) console.log('Folder element appended');
                        if (buttonClicked) enable(buttonClicked);

                    } else if (entity.type == 'File') {

                        _Files.uploadFile(entity);
                        _Files.appendFileElement(entity);
                        if (debug) console.log('File uploaded');
                        if (buttonClicked) enable(buttonClicked);

//                    } else if (entity.type == 'Element') {
                    } else {
                        if (resources) {
                            _Resources.appendElementElement(entity);
                        } else {
                            _Elements.appendElementElement(entity);
                        }
                        if (debug) console.log('Element element appended');
                        if (buttonClicked) enable(buttonClicked);

//                    } else {
//                        //appendEntityElement(data, parentElement);
//                        _Entities.appendEntityElement(entity);
//                        if (buttonClicked) enable(buttonClicked);
                    }
                });

            } else if (command == 'TREE') {
				
                if (debug) console.log('Render Tree');
                if (debug) console.log(data.root, data.id);
				
                _Entities.renderTree(data.root, data.id);

            } else if (command == 'LIST') {
				
                //console.log('List');
                //console.log(result);
				
                $(result).each(function(i, entity) {

//                    if (debug) console.log('LIST: ' + entity.type);

                    if (entity.type == 'User') {
                        var groups = entity.groups;
                        if (!groups || groups.length == 0) {
                            _UsersAndGroups.appendUserElement(entity);
                        }
						
                    } else if (entity.type == 'Group') {
                        var groupElement = _UsersAndGroups.appendGroupElement(entity);
                        var users = entity.users;
                        if (users && users.length > 0) {
                            disable($('.delete_icon', groupElement)[0]);
                            $(users).each(function(i, user) {
                                _UsersAndGroups.appendUserElement(user, entity.id);
                            });
                        }
                        
                    } else if (entity.type == 'Resource') {

                        _Entities.getTree(entity.id);

                    } else if (entity.type == 'Component') {

                        var componentElement = _Resources.appendComponentElement(entity);
                        var elements = entity.elements;
                        if (elements && elements.length > 0) {
                            disable($('.delete_icon', componentElement)[0]);
                            $(elements).each(function(i, element) {
                                if (element.type == 'Element') {
                                    _Resources.appendElementElement(element, entity.id);
                                }
                            });
                        }
                    
                    } else if (entity.type == 'Content') {
                        _Resources.appendContentElement(entity);

                    } else if (entity.type == 'Folder') {
						
                        var folderElement = _Files.appendFolderElement(entity);
                        var folders = entity.folders;
                        if (folders && folders.length > 0) {
                            disable($('.delete_icon', folderElement)[0]);
                            $(folders).each(function(i, folder) {
                                _Files.appendFolderElement(file, entity.id);
                            });
                        }
                        var files = entity.files;
                        if (files && files.length > 0) {
                            disable($('.delete_icon', folderElement)[0]);
                            $(files).each(function(i, file) {
                                _Files.appendFileElement(file, entity.id);
                            });
                        }
						

                    } else if (entity.type == 'File') {
                        var parentFolder = entity.folder;
                        if (!parentFolder || parentFolder.length == 0) {
                            _Files.appendFileElement(entity);
                        }

                    } else {
//                        _Entities.appendEntityElement(entity);
//                    } else if (entity.type == 'Element') {

                        //						Entities.getTree(entity.id);
                        var elementElement = _Resources.appendElementElement(entity);
                        var elements = entity.elements;
                        if (elements && elements.length > 0) {
                            disable($('.delete_icon', elementElement)[0]);
                            $(elements).each(function(i, element) {
                                if (element.type == 'Element') {
                                    _Resources.appendElementElement(element, entity.id);
                                } else if (element.type == 'Content') {
                                    _Resources.appendContentElement(element, entity.id);
                                }
                            });
                        }
                    }
						
                });

            } else if (command == 'DELETE') {
                var elementSelector = '.' + data.id + '_';
                if (debug) console.log($(elementSelector));
                $(elementSelector).remove();
                if (buttonClicked) enable(buttonClicked);
                _Resources.reloadPreviews();

            } else if (command == 'REMOVE') {

                if (debug) console.log(data);

                parentId = data.id;
                entityId = data.data.id;

                parent = $('.' + parentId + '_');
                entity = $('.' + entityId + '_', parent);

                if (debug) console.log(parent);
                if (debug) console.log(entity);

                //var id = getIdFromClassString(entity.attr('class'));
                //entity.id = id;

                if (entity.hasClass('user')) {
                    if (debug) console.log('remove user from group');
                    _UsersAndGroups.removeUserFromGroup(entityId, parentId);

                } else if (entity.hasClass('component')) {
                    if (debug) console.log('remove component from resource');
                    _Resources.removeComponentFromResource(entityId, parentId);
                    _Resources.reloadPreviews();

                } else if (entity.hasClass('element')) {
                    if (debug) console.log('remove element from resource');
                    _Resources.removeElementFromResource(entityId, parentId);
                    _Resources.reloadPreviews();

                } else if (entity.hasClass('content')) {
                    if (debug) console.log('remove content from element');
                    _Resources.removeContentFromElement(entityId, parentId);
                    _Resources.reloadPreviews();

                } else if (entity.hasClass('file')) {
                    if (debug) console.log('remove file from folder');
                    _Files.removeFileFromFolder(entityId, parentId);

                } else {
                //if (debug) console.log('remove element');
                //entity.remove();
                }

                _Resources.reloadPreviews();
                if (debug) console.log('Removed ' + entityId + ' from ' + parentId);

            } else if (command == 'ADD') {

                parentId = data.id;
                entityId = data.data.id;

                parent = $('.' + parentId + '_');
                entity = $('.' + entityId + '_');

                if (debug) console.log('entity, parent');
                if (debug) console.log(entity, parent);

                entity.css('left', 0);
                entity.css('top', 0);

                //parent.append(entity);
                
                if (entity.hasClass('user')) {
                    _UsersAndGroups.addUserToGroup(entityId, parentId);

                } else if (entity.hasClass('component')) {
                    _Resources.addComponentToResource(entityId, parentId);
                    _Resources.reloadPreviews();

                } else if (entity.hasClass('element')) {
                    _Resources.addElementToResource(entityId, parentId);
                    _Resources.reloadPreviews();

                } else if (entity.hasClass('content')) {
                    _Resources.addContentToElement(entityId, parentId);
                    _Resources.reloadPreviews();

                } else if (entity.hasClass('file')) {
                    _Files.addFileToFolder(entityId, parentId);
                }

            } else if (command == 'UPDATE') {
                var element = $( '.' + data.id + '_');
                var input = $('.props tr td.value input', element);
                if (debug) console.log(element);

                input.parent().children('.icon').each(function(i, img) {
                    $(img).remove();
                });
                input.removeClass('active');
                if (debug) console.log(element);//.children('.' + key));
                
                for (key in data.data) {
                    element.children('.' + key + '_').text(data.data[key]);
                    if (debug) console.log($('.props tr td.' + key + ' input', element));
                    $('.props tr td.' + key + ' input', element).val(data.data[key]);
                }

                input.data('changed', false);
                _Resources.reloadPreviews();

            } else {
                if (debug) console.log('Received unknown command: ' + command);

                if (sessionValid == false) {
                    if (debug) console.log('invalid session');
                    $.cookie('structrSessionToken', '');
                    $.cookie('structrUser', '');
                    clearMain();

                    Structr.login();
                }
            }
        }

        ws.onclose = function() {
            Structr.confirmation('Connection lost or timed out.<br>Reconnect?', Structr.init);
        }

    } catch (exception) {
        log('Error in connect(): ' + exception);
    }

}

function send(text) {

    log(ws.readyState);

    var obj = $.parseJSON(text);
    
    if (token) {
        obj.token = token;
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

function log(msg) {
    if (debug) console.log(msg);
    $("#log").append("<br />" + msg);
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
    //return window.btoa(unescape(encodeURIComponent( str )));
    return window.btoa(str);
}

function b64_to_utf8( str ) {
    //return decodeURIComponent(escape(window.atob( str )));
    return window.atob(str);
}