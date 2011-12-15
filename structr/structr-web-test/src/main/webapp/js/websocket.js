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

					Structr.init();
					
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
						if (groupId) UsersAndGroups.appendUserElement(entity, groupId);
						UsersAndGroups.appendUserElement(entity);
						disable($('.' + groupId + '_ .delete_icon')[0]);
						if (buttonClicked) enable(buttonClicked);

					} else if (entity.type == 'Group') {

						if (debug) console.log(entity);
						UsersAndGroups.appendGroupElement(entity);
						if (debug) console.log('group element appended');
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
							UsersAndGroups.appendUserElement(entity);
						}
						
					} else if (entity.type == 'Group') {
						var groupElement = UsersAndGroups.appendGroupElement(entity);
						var users = entity.users;
						if (users && users.length > 0) {
							disable($('.delete_icon', groupElement)[0]);
							$(users).each(function(i, user) {
								UsersAndGroups.appendUserElement(user, entity.id);
							});
						}
					} else {
						Entities.appendEntityElement(entity);
					}
						
				});

			} else if (command == 'DELETE') {
				var elementSelector = '.' + data.id + '_';
				if (debug) console.log($(elementSelector));
				$(elementSelector).remove();
				if (buttonClicked) enable(buttonClicked);

			} else if (command == 'REMOVE') {

				if (debug) console.log(data);

				var parentId = data.id;
				var entityId = data.data.id;

				var parent = $('.' + parentId + '_');
				var entity = $('.' + entityId + '_');

				if (debug) console.log(parent);
				if (debug) console.log(entity);

				var isUser = entity.hasClass('user');
				if (isUser) {

					var id = getIdFromClassString(entity.attr('class'));
					entity.id = id;

					users.append(entity);//.animate();
					$('.delete_icon', entity).remove();
					entity.append('<img title="Delete user ' + entityId + '" '
						+ 'alt="Delete user ' + entityId + '" class="delete_icon button" src="icon/delete.png">');
					$('.delete_icon', entity).on('click', function() {
						deleteUser(this, entity);
					});
					entity.draggable({
						revert: 'invalid',
						containment: '#main',
						zIndex: 1
					});

					var numberOfUsers = $('.user', parent).size();
					if (debug) console.log(numberOfUsers);
					if (numberOfUsers == 0) {
						enable($('.delete_icon', parent)[0]);
					}


				} else {
					entity.remove();
				}

				if (debug) console.log('Removed ' + entityId + ' from ' + parentId);

			} else if (command == 'ADD') {

				if (debug) console.log(data);

				var parentId = data.id;
				var entityId = data.data.id;

				var parent = $('.' + parentId + '_');
				var entity = $('.' + entityId + '_');

				if (debug) console.log(parent);
				if (debug) console.log(entity);

				entity.css('left', 0);
				entity.css('top', 0);

				parent.append(entity);
				var isUser = entity.hasClass('user');
				if (isUser) {
					$('.delete_icon', entity).remove();
					entity.append('<img title="Remove user ' + entityId + ' from group ' + parentId + '" '
						+ 'alt="Remove user ' + entityId + ' from group ' + parentId + '" class="delete_icon button" src="icon/user_delete.png">');
					$('.delete_icon', entity).on('click', function() {
						UsersAndGroups.removeUserFromGroup(entityId, parentId)
					//deleteUser(this, user);
					});
					entity.draggable('destroy');

					var numberOfUsers = $('.user', parent).size();
					if (debug) console.log(numberOfUsers);
					if (numberOfUsers > 0) {
						disable($('.delete_icon', parent)[0]);
					}
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

					login();
				}
			}
		}

		ws.onclose = function() {
			login('Connection was closed by server');
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


function getAnchorFromUrl(url) {
	if (url) {
		var pos = url.lastIndexOf('#');
		if (pos > 0) {
			return url.substring(pos+1, url.length);
		}
	}
	return null;
}