/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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

function connect() {

    if (token) {
        loggedIn = true;
    }

    try {

        var isEnc = (window.location.protocol == 'https:');
        var host = document.location.host;
        var wsUrl = 'ws' + (isEnc ? 's' : '') + '://' + host + wsRoot;

        if (debug) console.log(wsUrl);
        if ('WebSocket' in window) {
            ws = new WebSocket(wsUrl, 'structr');
        }
        else if ('MozWebSocket' in window) {
            ws = new MozWebSocket(wsUrl, 'structr');
        } else {
            alert('Your browser doesn\'t support WebSocket.');
            return;
        }

        log('State: ' + ws.readyState);
		
        var entityId;
        var entity;

        ws.onmessage = function(message) {

            var data = $.parseJSON(message.data);
            if (debug) console.log(data);

            //var msg = $.parseJSON(message);
            var type = data.type;
            var command = data.command;
            var parentId = data.id;
            var entityId = data.data.id;
            var componentId = data.data.componentId;
            var pageId = data.data.pageId;
            var position = data.data.position;
            var msg = data.message;
            var result = data.result;
            var sessionValid = data.sessionValid;
            var code = data.code;
            var callback = data.callback;

            {
                if (debug) console.log('command: ' + command);
                if (debug) console.log('type: ' + type);
                if (debug) console.log('code: ' + code);
                if (debug) console.log('callback: ' + callback);
                if (debug) console.log('sessionValid: ' + sessionValid);
            }
            if (debug) console.log('result: ' + $.toJSON(result));

            if (command == 'LOGIN') { /*********************** LOGIN ************************/
                token = data.token;
                user = data.data.username;
                if (debug) console.log('token', token);
		
                if (sessionValid) {
                    $.cookie('structrSessionToken', token);
                    $.cookie('structrUser', user);
                    $.unblockUI({
                        fadeOut: 25
                    });
                    $('#logout_').html('Logout <span class="username">' + user + '</span>');

                    Structr.loadInitialModule();
					
                } else {
                    $.cookie('structrSessionToken', '');
                    $.cookie('structrUser', '');
                    clearMain();

                    Structr.login();
                }

            } else if (command == 'LOGOUT') { /*********************** LOGOUT ************************/

                $.cookie('structrSessionToken', '');
                $.cookie('structrUser', '');
                clearMain();
                Structr.login();

            } else if (command == 'STATUS') { /*********************** STATUS ************************/
                if (debug) console.log('Error code: ' + code);
				
                if (code == 403) {
                    Structr.login('Wrong username or password!');
                } else if (code == 401) {
                    Structr.login('Session invalid');
                } else {
                    var msgClass;
                    var codeStr = code.toString();
                    if (codeStr.startsWith('20')) {
                        msgClass = 'success';
                        $('#dialogBox .dialogMsg').html('<div class="infoBox ' + msgClass + '">' + msg + '</div>');
                    } else if (codeStr.startsWith('30')) {
                        msgClass = 'info';
                        $('#dialogBox .dialogMsg').html('<div class="infoBox ' + msgClass + '">' + msg + '</div>');
                    } else if (codeStr.startsWith('40')) {
                        msgClass = 'warning';
                        $('#dialogBox .dialogMsg').html('<div class="infoBox ' + msgClass + '">' + msg + '</div>');
                    } else {
                        Structr.error("Error", true);
                        msgClass = 'error';
                        $('#errorBox .errorMsg').html('<div class="infoBox ' + msgClass + '">' + msg + '</div>');
                    }

                }

            } else if (command == 'TREE') { /*********************** TREE ************************/
				
                if (debug) console.log('Render Tree');
                if (debug) console.log(data.root, data.id);
				
                _Entities.renderTree(data.root, data.id);

            } else if (command == 'GET') { /*********************** GET ************************/

                if (debug) console.log('GET:', data);

                var d = data.data.displayElementId;
                if (debug) console.log('displayElementId', d);

                var parentElement;
                if (d != null) {
                    parentElement = $($(d)[0]);
                } else {
                    parentElement = $($('.' + data.id + '_')[0]);
                }

                if (debug) console.log('parentElement', parentElement);
                var key = data.data.key;
                var value = data.data[key];

                var attrElement = $(parentElement.find('.' + key + '_')[0]);
                if (debug) console.log('attrElement', attrElement);
                if (debug) console.log(key, value);

                if (attrElement && value) {

                    if (typeof value == 'boolean') {
                        if (debug) console.log(attrElement, value);
                        _Entities.changeBooleanAttribute(attrElement, value);

                    } else {
                        
                        if (debug) console.log($(attrElement));
                        
                        var tag = $(attrElement).get(0).tagName.toLowerCase();
                        
                        if (debug) console.log('attrElement tagName', tag);
                        
                        if (!(tag == 'select')) {
                            if (debug) console.log('appending ' + value + ' to attrElement', attrElement);
                            attrElement.append(value);
                        }
                        
                        if (debug) console.log('setting ' + value + ' on attrElement', attrElement);
                        
                        attrElement.val(value);
                        attrElement.show();
                    }
                }

            } else if (command == 'CHILDREN') { /*********************** CHILDREN ************************/

                var treeAddress = data.data.treeAddress;

                if (debug) console.log('CHILDREN:', parentId, componentId, pageId);
                if (debug) console.log('CHILDREN');
                if (debug) console.log('parentId', parentId);
                if (debug) console.log('componentId', componentId);
                if (debug) console.log('pageId', pageId);
                if (debug) console.log('Nodes with children', data.nodesWithChildren);
                if (debug) console.log('Tree address', treeAddress);
                
                
                $(result).each(function(i, child) {
                    if (debug) console.log('CHILDREN: ', child, parentId, componentId, pageId, false, isIn(child.id, data.nodesWithChildren), treeAddress);
                    _Entities.appendObj(child, parentId, componentId, pageId, false, isIn(child.id, data.nodesWithChildren), treeAddress);
                });

            } else if (command == 'LIST') { /*********************** LIST ************************/
				
                if (debug) console.log('LIST:', result);
                if (debug) console.log('Nodes with children', data.nodesWithChildren);
                $(result).each(function(i, entity) {
                    if (debug) console.log('LIST: ' + entity.type);
                    
                    if (entity.type != 'Folder' || !entity.parentFolder) {
                        _Entities.appendObj(entity, null, null, null, false, isIn(entity.id, data.nodesWithChildren), treeAddress);
                    } else {
                        console.log(entity);
                    }
                    
                });

            } else if (command == 'DELETE') { /*********************** DELETE ************************/
                var elementSelector = '.' + data.id + '_';
                if (debug) console.log($(elementSelector));
                $(elementSelector).remove();
                if (buttonClicked) enable(buttonClicked);
                _Pages.reloadPreviews();

            } else if (command == 'REMOVE') { /*********************** REMOVE ************************/

                if (debug) console.log(command, data);

                //parent = Structr.node(parentId);
                entity = Structr.node(entityId, parentId, componentId, pageId, position);

                //if (debug) console.log(parent);
                if (debug) console.log(entity);

                //var id = getIdFromClassString(entity.prop('class'));
                //entity.id = id;
                if (entity.hasClass('user')) {
                    if (debug) console.log('remove user from group');
                    _UsersAndGroups.removeUserFromGroup(entityId, parentId, position);

                } else if (entity.hasClass('element') || entity.hasClass('content') || entity.hasClass('component')) {
                    
                    if (debug) console.log('remove element from page', entityId, parentId, componentId, pageId, position);
                    _Pages.removeFrom(entityId, parentId, componentId, pageId, position);
                    _Pages.reloadPreviews();

                } else if (entity.hasClass('file')) {
                    if (debug) console.log('remove file from folder');
                    _Files.removeFileFromFolder(entityId, parentId, position);

                } else if (entity.hasClass('image')) {
                    if (debug) console.log('remove image from folder');
                    _Files.removeImageFromFolder(entityId, parentId, position);

                } else if (entity.hasClass('folder')) {
                    if (debug) console.log('remove folder from folder');
                    _Files.removeFolderFromFolder(entityId, parentId, position);

                } else {
                //if (debug) console.log('remove element');
                //entity.remove();
                }

                _Pages.reloadPreviews();
                if (debug) console.log('Removed ' + entityId + ' from ' + parentId);

            //} else if (command == 'ADD' || command == 'IMPORT') { /*********************** CREATE, ADD, IMPORT ************************/
            } else if (command == 'CREATE' || command == 'ADD' || command == 'IMPORT') { /*********************** CREATE, ADD, IMPORT ************************/
            //} else if (command == 'CREATE' || command == 'IMPORT') { /*********************** CREATE, ADD, IMPORT ************************/
                
                if (debug) console.log(command, result, data, data.data);
                
                //var treeAddress = data.data.treeAddress;
				
                $(result).each(function(i, entity) {
                    
                   if (debug) console.log(command, entity, parentId, componentId, pageId, command == 'ADD', isIn(entity.id, data.nodesWithChildren), treeAddress);
                    
                    //var el = Structr.node(entity.id, parentId, componentId, pageId);
                    var el = Structr.elementFromAddress(treeAddress);
                    if (el) el.remove();
                    
                    //alert(entity.id);
                    
                    _Entities.appendObj(entity, parentId, componentId, pageId, command == 'ADD', isIn(entity.id, data.nodesWithChildren), treeAddress);
                    
                    if (command == 'CREATE' && entity.type == 'Page') {
                        var tab = $('#show_' + entity.id, previews);
                        setTimeout(function() { _Pages.activateTab(tab) }, 200);
                    }
                    
                });

                _Pages.reloadPreviews();
                
            //alert(command);

            } else if (command == 'UPDATE') { /*********************** UPDATE ************************/
                
                if (debug) console.log('UPDATE');
                
                var relData = data.relData;
                if (debug) console.log('relData', relData);
                
                var removedProperties = data.removedProperties;
                var modifiedProperties = data.modifiedProperties;
                
                if (debug) console.log(removedProperties, modifiedProperties);
                
                var isRelOp = false;
                
                if (relData && relData.startNodeId && relData.endNodeId) {
                    isRelOp = true;
                    if (debug) console.log('relationship', relData, relData.startNodeId, relData.endNodeId);
                    
                }
                
                if (modifiedProperties) {
                    if (debug) console.log('modifiedProperties.length', modifiedProperties.length);
                    var resId = modifiedProperties[0];
                    if (debug) console.log('relData[resId]', relData[resId]);
                }
                
                if (relData && removedProperties && removedProperties.length) {
                    if (debug) console.log('removedProperties', removedProperties);
                    _Pages.removeFrom(relData.endNodeId, relData.startNodeId, null, removedProperties[0]);
                    
                } else if (isRelOp && modifiedProperties && modifiedProperties.length) {
                    
                    if (debug) console.log(data);
                    
                    if (debug) console.log('modifiedProperties', modifiedProperties[0]);
                		    
                    var newPageId = modifiedProperties[0];
                    //var pos = relData[newPageId];
                		    
                    var page;
                        
                    if (newPageId != '*') {
                        page   = Structr.node(newPageId);
                    }
                    
                    if (debug) console.log('page', page);
                		    
                    if (page && page.length) {
                                    
                        var entity = Structr.entity(relData.endNodeId, relData.startNodeId);
                        if (debug) console.log('entity', entity, pageId, newPageId);
                        if (entity && newPageId) {
                            
                            parentId = relData.startNodeId;
                            
                            var parent = Structr.entity(parentId);
                            if (debug) console.log('parent type', parent, parent.type);
                            if (!parent.type || parent.type == 'Page') return;
                            
                            var id = entity.id;
                            //_Pages.removeFrom(entity.id, relData.startNodeId, null, newPageId, pos);
                            //_Entities.appendObj(entity, relData.startNodeId, null, newPageId);
                            var el = Structr.node(id, parentId, componentId, newPageId);
                            if (debug) console.log('node already exists?', el);
                            
                            if (id && (!el || !el.length)) {
                                //el.remove();
                            
                                //_Entities.resetMouseOverState(el);
                                _Entities.appendObj(entity, parentId, null, newPageId, true, true);
                            }
                            
                        //_Entities.reloadChildren(relData.startNodeId, componentId, newPageId)
                        
                        //_Pages.refresh();
                        
                        }
                    }
                    
                } else {
                    
                    if (debug) console.log('else');
                
                    var element = $('.' + data.id + '_');
                    var input = $('.props tr td.value input', element);
                    if (debug) console.log(element);

                    // remove save and cancel icons
                    input.parent().children('.icon').each(function(i, img) {
                        $(img).remove();
                    });

                    // make inactive
                    input.removeClass('active');
                    if (debug) console.log(element);

                    // update values with given key
                    for (var key in data.data) {
                    
                        var inputElement = element.children('.props tr td.' + key + ' input');
                        var newValue = data.data[key];
                        if (debug) console.log(key, newValue, typeof newValue);

                        var attrElement = element.children('.' + key + '_');
                    
                        if (attrElement && $(attrElement).length) {
                    
                            var tag = $(attrElement).get(0).tagName.toLowerCase();
                        
                        
                            attrElement.val(value);
                            attrElement.show();
                    
                            if (debug) console.log(attrElement, inputElement);
                    
                        }
                    

                        if (typeof newValue  == 'boolean') {

                            _Entities.changeBooleanAttribute(attrElement, newValue);
                        
                        } else {

                            attrElement.animate({
                                color: '#81ce25'
                            }, 100, function() {
                                $(this).animate({
                                    color: '#333333'
                                }, 200);
                            });
                        
                            if (attrElement && tag == 'select') {
                                attrElement.val(newValue);
                            } else {
                                attrElement.text(newValue);
                            }
                        
                            if (inputElement) {
                                inputElement.val(newValue);
                            }

                            if (key == 'content') {

                                if (debug) console.log(attrElement.text(), newValue);

                                attrElement.text(newValue);

                                // hook for CodeMirror edit areas
                                if (editor && editor.id == data.id) {
                                    if (debug) console.log(editor.id);
                                    editor.setValue(newValue);
                                    //editor.setCursor(editorCursor);
                                }
                            }
                        }
                    
                        if (debug) console.log(key, Structr.getClass(element));
                    
                        if (key == 'name' && Structr.getClass(element) == 'page') {
                            if (debug) console.log('Reload iframe', data.id, newValue);
                            window.setTimeout(function() {
                                _Pages.reloadIframe(data.id, newValue)
                            }, 100);
                        }

                    }
                

                
                }
                
                if (input) {
                    input.data('changed', false);
                }
                
                _Pages.reloadPreviews();
                
            } else if (command == 'WRAP') { /*********************** WRAP ************************/

                if (debug) console.log('WRAP');

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
            //Structr.confirmation('Connection lost or timed out.<br>Reconnect?', Structr.reconnect);
            //Structr.error('Connection lost or timed out. Trying automatic reconnect');
            log('Connection was lost or timed out. Trying automatic reconnect');
            Structr.reconnect();
        }

    } catch (exception) {
        log('Error in connect(): ' + exception);
        Structr.init();
    }

}

function sendObj(obj) {

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

function send(text) {

    log(ws.readyState);

    var obj = $.parseJSON(text);

    return sendObj(obj);
}

function log(msg) {
    if (debug) console.log(msg);
    $("#log").append("<br>" + msg);
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