/* 
 *  Copyright (C) 2013 Axel Morgner
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
var debug = true;
var wsRoot = '/structr/ws';
var reconn, loggedIn;
var token = '';
var rawResultCount = [], pageCount = [];

$().ready(function() {

    console.log('init');

    connect();

});



function connect() {

    if (token) {
        loggedIn = true;
    }

    try {

        ws = null;

        var isEnc = (window.location.protocol == 'https:');
        var host = document.location.host;
        var wsUrl = 'ws' + (isEnc ? 's' : '') + '://' + host + wsRoot;

        console.log(wsUrl);
        if ('WebSocket' in window) {

            ws = new WebSocket(wsUrl, 'structr');

        } else if ('MozWebSocket' in window) {

            ws = new MozWebSocket(wsUrl, 'structr');

        } else {

            alert('Your browser doesn\'t support WebSocket.');
            return false;

        }

        console.log('WebSocket.readyState: ' + ws.readyState, ws);

        var entity;

        ws.onopen = function() {

            console.log('de-activating reconnect loop', reconn);
            window.clearInterval(reconn);

//            log('logged in? ' + loggedIn);
//            if (!loggedIn) {
//                //log('no');
//                $('#logout_').html('Login');
//                Structr.login();
//            } else {
//                log('Current user: ' + user);
//                $('#logout_').html(' Logout <span class="username">' + (user ? user : '') + '</span>');
//				
//            }
        }

        ws.onclose = function() {

            if (reconn) {
                console.log('Automatic reconnect already active');
                return;
            }
            console.log('activating reconnect loop');
            reconn = window.setInterval(function() {
                connect();
            }, 1000);
            console.log('activated reconnect loop', reconn);

        }

        ws.onmessage = function(message) {

            var data = $.parseJSON(message.data);
            console.log('ws.onmessage:', message);

            //var msg = $.parseJSON(message);
            var type = data.data.type;
            var command = data.command;
            var msg = data.message;
            var result = data.result;
            var sessionValid = data.sessionValid;
            var code = data.code;

            console.log('####################################### ', command, ' #########################################');


            if (command == 'LOGIN') { /*********************** LOGIN ************************/
            } else if (command == 'LOGOUT') { /*********************** LOGOUT ************************/
            } else if (command == 'STATUS') { /*********************** STATUS ************************/
            } else if (command == 'GET_PROPERTY') { /*********************** GET_PROPERTY ************************/
            } else if (command == 'GET_PROPERTIES' || command == 'UPDATE') { /*********************** GET_PROPERTIES / UPDATE ************************/

                console.log('UPDATE', data);

            } else if (command == 'DATA_NODE_PARENT') { /*********************** DATA_NODE_PARENT ************************/
            } else if (command == 'DELETE') { /*********************** DELETE ************************/
            } else if (command == 'INSERT_BEFORE') { /*********************** INSERT_BEFORE ************************/
            } else if (command == 'APPEND_CHILD') { /*********************** APPEND_CHILD ************************/
            } else if (command == 'REMOVE' || command == 'REMOVE_CHILD') { /*********************** REMOVE / REMOVE_CHILD ************************/
            } else if (command == 'CREATE' || command == 'ADD' || command == 'IMPORT') { /*********************** CREATE, ADD, IMPORT ************************/
            } else {
                console.log('Received unknown command: ' + command);
            }
        }

    } catch (exception) {
        console.log('Error in connect(): ' + exception);
    }

}


var Structr = function(restBaseUrl, username, password) {
    this.restBaseUrl = restBaseUrl;
    this.headers = {
        'X-User': username,
        'X-Password': password
    };
};

Structr.prototype.get = function(req, callback) {
    $.ajax({
        url: this.restBaseUrl + '/' + req,
        headers: this.headers,
        type: 'GET',
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        statusCode: {
            200: function(data) {
                if (callback) {
                    callback(data.result);
                }
            },
            204: function() {
            },
            400: function(data, status, xhr) {
                console.log('Bad request: ' + data.responseText);
            },
            401: function(data, status, xhr) {
                console.log('Authentication required: ' + data.responseText);
            },
            403: function(data, status, xhr) {
                console.log('Forbidden: ' + data.responseText);
            },
            404: function(data, status, xhr) {
                _Crud.error('Not found: ' + data.responseText);
            },
            422: function(data, status, xhr) {
                console.log('Syntax error: ' + data.responseText);
            },
            500: function(data, status, xhr) {
                console.log('Internal error: ' + data.responseText);
            }
        }
    });
};