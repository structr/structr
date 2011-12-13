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
        if ('WebSocket' in window) {
            ws = new WebSocket('ws://' + host + '/structr-web-test/ws/', 'structr');
        } else if ('MozWebSocket' in window) {
			console.log(host);
            ws = new MozWebSocket('ws://' + host + '/structr-web-test/ws/', 'structr');
        } else {
            alert('Your browser doesn\'t support WebSocket.');
            return;
        }

        log('State: ' + ws.readyState);

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
                    $.cookie("structrSessionToken", token);
                    $.cookie("structrUser", user);
                    $.unblockUI();
                    $('#logoutLink').html('Logout <span class="username">' + user + '</span>');
                    onload();
                } else {
                    $.cookie('structrSessionToken', '');
                    $.cookie('structrUser', '');
                    clearMain();
                    $('#logoutLink').removeClass('active');
                    $('#logoutLink').addClass('inactive');

                    login();
                }

            } else if (command == 'LOGOUT') {

                $.cookie('structrSessionToken', '');
                $.cookie('structrUser', '');
                clearMain();
                login();

            } else if (command == 'STATUS') {
                if (debug) console.log('Error code: ' + code);
				
                if (code == 403) {
                    login('Wrong username or password!');
                } else if (code == 401) {
                    login('Session invalid');
                }

            } else if (command == 'CREATE') {
				
                $(result).each(function(i, entity) {
                    if (entity.type == 'User') {

                        groupId = entity.groupId;
                        if (groupId) appendUserElement(entity, groupId);
                        appendUserElement(entity);
                        disable($('.' + groupId + '_ .delete_icon')[0]);
                        if (buttonClicked) enable(buttonClicked);

                    } else if (entity.type == 'Group') {

                        if (debug) console.log(entity);
                        appendGroupElement(entity);
						var users = entity.users;
						if (users) {
							$(users).each(function(i, user) {
								appendUserElement(user, entity.id);
							});
						}
                        if (buttonClicked) enable(buttonClicked);

                    }
                    else {
                        //appendEntityElement(data, parentElement);
                        appendEntityElement(entity);
                        if (buttonClicked) enable(buttonClicked);

                    }
				
                });

            } else if (command == 'LIST') {
				
                $(result).each(function(i, entity) {
						
                    if (entity.type == 'User') {
						var groups = entity.groups;
						if (!groups || groups.length == 0) {
							appendUserElement(entity);
						}
						
                    } else if (entity.type == 'Group') {
                        appendGroupElement(entity);
						var users = entity.users;
						if (users) {
							$(users).each(function(i, user) {
								appendUserElement(user, entity.id);
							});
						}						
                    } else {
                        appendEntityElement(entity);
                    }
						
                });



            } else if (command == 'DELETE') {
                var elementSelector = '.' + data.id + '_';
                if (debug) console.log($(elementSelector));
                $(elementSelector).hide('blind', {
                    direction: 'vertical'
                }, 200, function() {
                    $(this).remove()
                    });
                //refreshIframes();
                if (buttonClicked) enable(buttonClicked);
            //if (callback) callback();

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
                    element.children('.' + key).text(data.data[key]);
                    if (debug) console.log($('.props tr td.' + key + ' input', element));
                    $('.props tr td.' + key + ' input', element).val(data.data[key]);
                }

                input.data('changed', false);

            } else {
                if (debug) console.log('Received unknown command: ' + command);

                if (sessionValid == false) {
                    if (debug) console.log('invalid session');
                    $.cookie('structrSessionToken', '');
                    $.cookie('structrUser', '');
                    clearMain();
                    $('#logoutLink').removeClass('active');
                    $('#logoutLink').addClass('inactive');

                    login();

                }

            }


        }

        ws.onclose = function() {
            log('Close: ' + ws.readyState);
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

function login(text) {
    if (text) $('#errorText').html(text);
    $.blockUI.defaults.overlayCSS.opacity = .6;
    $.blockUI.defaults.applyPlatformOpacityRules = false;
    $.blockUI({
        message: $('#login'),
        css: {
            border: 'none',
            backgroundColor: 'transparent'
        }
    });
    $('#logoutLink').addClass('inactive');
}

function doLogin(username, password) {
    if (debug) console.log('doLogin ' + username + ' with ' + password);
    if (send('{ "command":"LOGIN", "data" : { "username" : "' + username + '", "password" : "' + password + '" } }')) {
        user = username;
        return true;
    }
    return false;

}

function doLogout(text) {
    if (debug) console.log('doLogout ' + user);
    $.cookie("structrSessionToken", '');
    $.cookie("structrUser", '');
    if (send('{ "command":"LOGOUT", "data" : { "username" : "' + user + '" } }')) {
        $('#logoutLink').html('Login');
        clearMain();
        login(text);
        return true;
    }
    return false;
}

function clearMain() {
    main.empty();
}

function confirmation(text, callback) {
    if (text) $('#confirmationText').html(text);
    if (callback) $('#yesButton').on('click', function() {
        callback();
    });
    $('#noButton').on('click', function() {
        $.unblockUI();
    });
    $.blockUI.defaults.overlayCSS.opacity = .6;
    $.blockUI.defaults.applyPlatformOpacityRules = false;
    $.blockUI({
        message: $('#confirmation'),
        css: {
            border: 'none',
            backgroundColor: 'transparent'
        }
    });
	
}