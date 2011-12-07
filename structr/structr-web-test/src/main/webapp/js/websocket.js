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

function connect() {

    try {

        ws = new WebSocket("ws://localhost:8080/ws");

        console.log('State: ' + ws.readyState);

        ws.onopen = function() {
            console.log('Open: ' + ws.readyState);
        }

        ws.onmessage = function(message) {
            console.log('Message: ' + message.data);
        }

        ws.onclose = function() {
            console.log('Close: ' + ws.readyState);
        }

    } catch (exception) {
        console.log('Error: ' + exception);
    }
    
}

function send(text) {

    if (!text) {
        console.log('No text to send!');
        return;
    }

    try {

        ws.send(text);
        console.log('Sent: ' + text);

    } catch (exception) {
        console.log('Error: ' + exception);
    }

}