/*
 *  Copyright (C) 2012 Axel Morgner
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

var groups, users;

$(document).ready(function() {
	Structr.registerModule('usersAndGroups', _UsersAndGroups);
	Structr.classes.push('user');
	Structr.classes.push('group');
});

var _UsersAndGroups = {

	init : function() {
	//Structr.classes.push('user');
	//Structr.classes.push('group');
	},
	
	onload : function() {
		//Structr.activateMenuEntry('usersAndGroups');
		if (debug) console.log('onload');
		if (palette) palette.remove();

		main.append('<table><tr><td id="groups"></td><td id="users"></td></tr></table>');
		groups = $('#groups');
		users = $('#users');
		_UsersAndGroups.refreshGroups();
		_UsersAndGroups.refreshUsers();
	},
    
	refreshGroups : function() {
		groups.empty();
		if (_Entities.list('Group')) {
			groups.append('<button class="add_group_icon button"><img title="Add Group" alt="Add Group" src="icon/group_add.png"> Add Group</button>');
			$('.add_group_icon', main).on('click', function() {
				var entity = {};
				entity.type = 'Group';
				return _Entities.create(this, entity);
			});
		}
	},

	refreshUsers : function() {
		users.empty();
		if (_Entities.list('User')) {
			users.append('<button class="add_user_icon button"><img title="Add User" alt="Add User" src="icon/user_add.png"> Add User</button>');
			$('.add_user_icon', main).on('click', function() {
				var entity = {};
				entity.type = 'User';
				return _Entities.create(this, entity);
			});
		}
	},

	addUserToGroup : function(userId, groupId) {

		var group = $('.' + groupId + '_');
		var user = $('.' + userId + '_');

		group.append(user);

		$('.delete_icon', user).remove();
		user.append('<img title="Remove user ' + userId + ' from group ' + groupId + '" '
			+ 'alt="Remove user ' + userId + ' from group ' + groupId + '" class="delete_icon button" src="icon/user_delete.png">');
		$('.delete_icon', user).on('click', function() {
			_UsersAndGroups.removeUserFromGroup(userId, groupId)
		//deleteUser(this, user);
		});
		user.draggable('destroy');

		var numberOfUsers = $('.user', group).size();
		if (debug) console.log(numberOfUsers);
		if (numberOfUsers > 0) {
			disable($('.delete_icon', group)[0]);
		}

	},

	removeUserFromGroup : function(userId, groupId) {

		var group = $('.' + groupId + '_');
		var user = $('.' + userId + '_', group);
		users.append(user);//.animate();
		$('.delete_icon', user).remove();
		user.append('<img title="Delete user ' + user.name + '" '
			+ 'alt="Delete user ' + user.name + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
		$('.delete_icon', user).on('click', function() {
			_UsersAndGroups.deleteUser(this, Structr.entity(userId));
		});
        
		user.draggable({
			revert: 'invalid',
			containment: '#main',
			zIndex: 1
		});

		var numberOfUsers = $('.user', group).size();
		if (debug) console.log(numberOfUsers);
		if (numberOfUsers == 0) {
			enable($('.delete_icon', group)[0]);
		}

		if (debug) console.log('removeUserFromGroup: userId=' + userId + ', groupId=' + groupId);
		_Entities.removeSourceFromTarget(userId, groupId);
	},

	deleteUser : function(button, user) {
		if (debug) console.log('deleteUser ' + user);
		deleteNode(button, user);
	},

	deleteGroup : function(button, group) {
		var data = '{ "type" : "Group" , "name" : "' + group.name + '" , "id" : "' + group.id + '" }';
		deleteNode(button, $.parseJSON(data));
	},

	appendGroupElement : function(group) {
		groups.append('<div class="group ' + group.id + '_">'
			+ '<img class="typeIcon" src="icon/group.png">'
			+ '<b class="name_">' + group.name + '</b> <span class="id">' + group.id + '</span>'
			+ '</div>');
		var div = $('.' + group.id + '_', groups);
		div.append('<img title="Delete Group ' + group.id + '" alt="Delete Group ' + group.id + '" class="delete_icon button" src="icon/group_delete.png">');

		$('.delete_icon', div).on('click', function() {
			_UsersAndGroups.deleteGroup(this, group)
		});
		$('b', div).on('click', function() {
			_Entities.showProperties(this, group, $('#dialogBox .dialogText'));
		});
	
		div.droppable({
			accept: '.user',
			greedy: true,
			hoverClass: 'groupHover',
			drop: function(event, ui) {
				var userId = getIdFromClassString(ui.draggable.attr('class'));
				var groupId = getIdFromClassString($(this).attr('class'));
				var user = {};
				user.id = userId;
				_Entities.createAndAdd(groupId, user);
			}
		});

		return div;
	},

	appendUserElement : function(user, groupId) {
		var div;
		if (groupId) {
			var parent = $('.' + groupId + '_');
			parent.append('<div class="user ' + user.id + '_">'
				+ '<img class="typeIcon" src="icon/user.png">'
				//				+ ' <b class="realName">' + user.realName + '</b> [<span class="id">' + user.id + '</span>]'
				+ ' <b class="name_">' + user.name + '</b> <span class="id">' + user.id + '</span>'
				+ '</div>');
			div = $('.' + user.id + '_', parent)
			div.append('<img title="Remove user \'' + user.name + '\' from group ' + groupId + '" '
				+ 'alt="Remove user ' + user.name + ' from group ' + groupId + '" class="delete_icon button" src="icon/user_delete.png">');
			$('.delete_icon', div).on('click', function() {
				_UsersAndGroups.removeUserFromGroup(user.id, groupId)
			});
			$('b', div).on('click', function() {
				_Entities.showProperties(this, user, $('#dialogBox .dialogText'));
			});
		} else {
			users.append('<div class="user ' + user.id + '_">'
				+ '<img class="typeIcon" src="icon/user.png">'
				//				+ ' <b class="realName">' + user.realName + '</b> [' + user.id + ']'
				+ ' <b class="name_">' + user.name + '</b> ' + user.id + ''
				+ '</div>');
			div = $('.' + user.id + '_', users);
			div.append('<img title="Delete user \'' + user.name + '\'" '
				+ 'alt="Delete user \'' + user.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
			$('.delete_icon', div).on('click', function() {
				_UsersAndGroups.deleteUser(this, user)
			});
			$('b', div).on('click', function() {
				_Entities.showProperties(this, user, $('#dialogBox .dialogText'));
			});
			div.draggable({
				revert: 'invalid',
				containment: '#main',
				zIndex: 1
			});
		}
	}

};