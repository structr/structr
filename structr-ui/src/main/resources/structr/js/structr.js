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

function connect() {

    if (token) {
        loggedIn = true;
    }

    try {

        ws = null;

        var isEnc = (window.location.protocol === 'https:');
        var host = document.location.host;
        var wsUrl = 'ws' + (isEnc ? 's' : '') + '://' + host + wsRoot + '?' + location.pathname;

        console.log(wsUrl);
        if ('WebSocket' in window) {

            ws = new WebSocket(wsUrl, 'structr');

        } else if ('MozWebSocket' in window) {

            ws = new MozWebSocket(wsUrl, 'structr');

        } else {

            alert('Your browser doesn\'t support WebSocket.');
            return false;

        }

        //console.log('WebSocket.readyState: ' + ws.readyState, ws);

        ws.onopen = function() {

            //console.log('de-activating reconnect loop', reconn);
            window.clearInterval(reconn);
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

            var data = JSON.parse(message.data);
            console.log('ws.onmessage:', data);
            var command = data.command;

            //console.log('####################################### ', command, ' #########################################');

            if (command === 'PARTIAL') {

                console.log(data.message);
                var pos = data.data.parentPositionPath.split('/');
                var el = document.childNodes[1]; // start with html
                pos.forEach(function(p) {
                  var n = parseInt(p);
                  if (n) {
                    el = getChildElements(el)[n];
                    console.log(el);
                  }
                });
                el.innerHTML = data.message;
            }
        }

    } catch (exception) {
        console.log('Error in connect(): ' + exception);
    }

}

function getChildElements(el) {
  if (!el) return;
  var children = [];
  for (var i=0; i<el.childNodes.length; i++) {
    var c = el.childNodes[i];
    if (c.nodeType === 1) children.push(c);
  }
  return children;
}

connect();
