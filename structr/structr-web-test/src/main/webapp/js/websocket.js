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
			alert('Your browser doesn\'t support WebSocket.');
			return;
		}

		log('State: ' + ws.readyState);

		ws.onopen = function() {
			log('Open: ' + ws.readyState);
		}

		ws.onmessage = function(message) {

			log('Message received: ' + message);

			var result = $.parseJSON(message.data);
			var data = result.data;
			var command = result.command;

			console.log(data);

			if (command == 'CREATE') {

				if (data.type == 'User') {

					data.id = result.id;
					//data.command = null;
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

			} else if (command == 'LIST') {

				if (data) $(data.result).each(function(i, entity) {
					appendEntityElement(entity);
				});


			} else if (command == 'DELETE') {

				var elementSelector = '.' + result.id + '_';
				$(elementSelector).hide('blind', {
					direction: "vertical"
				}, 200);
				$(elementSelector).remove();
				//refreshIframes();
				if (buttonClicked) enable(buttonClicked);
			//if (callback) callback();

			} else if (command == 'UPDATE') {

				var element = $( '.' + result.id + '_');
				var input = $('.props tr td.value input', element);
				//console.log(element);

				input.parent().children('.icon').each(function(i, img) {
					$(img).remove();
				});
				input.removeClass('active');
				//                                console.log(element);//.children('.' + key));
				for (key in data) {
					element.children('.' + key).text(data[key]);
					console.log($('.props tr td.' + key + ' input', element));
					$('.props tr td.' + key + ' input', element).val(data[key]);
				}
                
				input.data('changed', false);

			} else {
				console.log('Unknown command: ' + command);
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
	
	log(ws.readyState);

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
	console.log(msg);
	$("#log").append("<br />" + msg);
}
