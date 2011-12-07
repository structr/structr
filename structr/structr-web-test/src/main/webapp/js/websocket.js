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

        var host = document.location.host;
        if ('WebSocket' in window) {
            ws = new WebSocket('ws://' + host + '/structr-web-test/ws/', 'structr');
        } else if ('MozWebSocket' in window) {
            ws = new MozWebSocket('ws://localhost:8080/structr-web-test/ws/', 'structr');
        } else {
            alert('Your browser doesn\'t support WebSocket. Go home!');
            return;
        }

        log('State: ' + ws.readyState);

        ws.onopen = function() {
            log('Open: ' + ws.readyState);
        }

        ws.onmessage = function(message) {

            log('Message received: ' + message);

            var data = $.parseJSON(message.data);

            console.log(data);

            if (data.command == 'CREATE') {

                if (data.type == 'User') {

                    data.command = null;
                    var user = data;
                    groupId = user.groupId;
                    if (groupId) appendUserElement(user, groupId);
                    appendUserElement(user);
                    disable($('.' + groupId + '_ .delete_icon')[0]);
                    if (buttonClicked) enable(buttonClicked);

                } else if (data.type == 'Group') {

                    appendGroupElement(data);
                    if (buttonClicked) enable(buttonClicked);
                    
                }
                else {
                    //appendEntityElement(data, parentElement);
                    appendEntityElement(data);
                    if (buttonClicked) enable(buttonClicked);

                }

            } else if (data.command == 'DELETE') {

                var elementSelector = '.' + data.uuid + '_';
                $(elementSelector).hide('blind', {
                    direction: "vertical"
                }, 200);
                $(elementSelector).remove();
                //refreshIframes();
                if (buttonClicked) enable(buttonClicked);
            //if (callback) callback();

            } else if (data.command == 'UPDATE') {

                console.log(data);
                var element = $( data.uuid + '_');
                var input = $('.props tr td.value input', element);

                input.parent().children('.icon').each(function(i, img) {
                    $(img).remove();
                });
                input.removeClass('active');
                //                                console.log(element);//.children('.' + key));
                $(data.keys).each(function(i, property) {
                    console.log(property);
                    //element.children('.' + key).text(value);
                });
                
                //var tick = $('<img class="icon/tick" src="tick.png">');
                //tick.insertAfter(input);
                //                                console.log('value saved');
                //$('.tick', input.parent()).fadeOut('slow', function() { console.log('fade out complete');});
                //$('.tick', $(v).parent()).fadeOut();
                //$('.tick', $(v).parent()).remove();
                input.data('changed', false);

            } else {
                console.log('Unknown command: ' + data.command);
            }


        }

        ws.onclose = function() {
            log('Close: ' + ws.readyState);
        }

    } catch (exception) {
        log('Error: ' + exception);
    }

}

function send(text) {

    if (!text) {
        log('No text to send!');
        return;
    }

    try {

        ws.send(text);
        log('Sent: ' + text);

    } catch (exception) {
        log('Error: ' + exception);
    }

}

function log(msg) {
    $("#log").append("<br />" + msg);
}
