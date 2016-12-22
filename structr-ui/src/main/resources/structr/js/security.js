/*
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
$(document).ready(function() {
	Structr.registerModule('security', _Security);
	Structr.classes.push('user');
	Structr.classes.push('group');
	Structr.classes.push('resourceAccess');
});

var _Security = {
	groups: undefined,
	users: undefined,
	resourceAccesses: undefined,
	securityTabKey: 'structrSecurityTab_' + port,
	init: function() {
		_Pager.initPager('users',           'User', 1, 25, 'name', 'asc');
		_Pager.initPager('groups',          'Group', 1, 25, 'name', 'asc');
		_Pager.initPager('resource-access', 'ResourceAccess', 1, 25, 'signature', 'asc');
	},
	onload: function() {
		_Security.init();

		Structr.updateMainHelpLink('http://docs.structr.org/frontend-user-guide#Users and Groups');
		_Logger.log(_LogType.SECURTIY, 'onload');

		main.append(
			'<div id="securityTabs">' +
				'<ul id="securityTabsMenu"><li><a id="usersAndGroups_" href="#usersAndGroups"><span>Users and Groups</span></a></li><li><a id="resourceAccess_" href="#resourceAccess"><span>Resource Access Grants</span></a></li></ul>' +
				'<div id="usersAndGroups"><div id="users"></div><div id="groups"></div></div><div id="resourceAccess"><div id="resourceAccesses"></div></div>' +
			'</div>'
		);

		_Security.groups = $('#groups');
		_Security.users = $('#users');
		_Security.resourceAccesses = $('#resourceAccesses');

		var activeTab = LSWrapper.getItem(_Security.securityTabKey) || 'usersAndGroups';
		_Security.selectTab(activeTab);

		$('#securityTabs').tabs({
			active: (activeTab === 'usersAndGroups' ? 0 : 1),
			activate: function(event, ui) {
				_Security.selectTab(ui.newPanel[0].id);
			}
		});

		Structr.unblockMenu(100);
	},
	selectTab: function (tab) {

		LSWrapper.setItem(_Security.securityTabKey, tab);

		if (tab === 'usersAndGroups') {
			_UsersAndGroups.refreshUsers();
			_UsersAndGroups.refreshGroups();
		} else if (tab === 'resourceAccess') {
			_ResourceAccessGrants.refreshResourceAccesses();
		}
	}
};

var _UsersAndGroups = {

	refreshUsers: function() {
		_Security.users.empty();
		_Security.users.append('<button class="add_user_icon button"><img title="Add User" alt="Add User" src="' + _Icons.user_add_icon + '"> Add User</button>');
		$('.add_user_icon', main).on('click', function(e) {
			e.stopPropagation();
			return Command.create({type: 'User'});
		});
		var userPager = _Pager.addPager('users', _Security.users, true, 'User', 'public');
		userPager.pager.append('<div>Filter: <input type="text" class="filter" data-attribute="name"></th></div>');
		userPager.activateFilterElements();
	},
	createUserElement:function (user, group) {
		var userName = user.name ? user.name : user.eMail ? '[' + user.eMail + ']' : '[unnamed]';

		var userElement = $(
				'<div class="node user userid_' + user.id + '">'
				+ '<img class="typeIcon" src="' + _Icons.user_icon + '">'
				+ ' <b title="' + userName + '" class="name_">' + userName + '</b> <span class="id">' + user.id + '</span>'
				+ '</div>'
		);
		userElement.data('userId', user.id);

		if (group) {
			userElement.append('<img title="Remove user \'' + userName + '\' from group \'' + group.name + '\'" alt="Remove user ' + userName + ' from group \'' + group.name + '\'" class="delete_icon button" src="' + _Icons.user_delete_icon + '">');

			$('.delete_icon', userElement).on('click', function(e) {
				e.stopPropagation();
				Command.removeFromCollection(group.id, 'members', user.id, function () {
					_UsersAndGroups.deactivateNodeHover(user.id, '.userid_');
				});
			});
		} else {
			userElement.append('<img title="Delete user \'' + userName + '\'" alt="Delete user \'' + userName + '\'" class="delete_icon button" src="' + _Icons.delete_icon + '">');

			$('.delete_icon', userElement).on('click', function(e) {
				e.stopPropagation();
				_UsersAndGroups.deleteUser(this, user);
			});
		}

		return userElement;
	},
	appendUserToUserList: function (user) {

		if (!_Security.users || !_Security.users.is(':visible')) {
			return;
		}

		var userDiv = _UsersAndGroups.createUserElement(user);
		_Security.users.append(userDiv);

		userDiv.draggable({
			revert: 'invalid',
			helper: 'clone',
			stack: '.node',
			appendTo: '#main',
			zIndex: 99
		});

		_Entities.appendEditPropertiesIcon(userDiv, user);
		_UsersAndGroups.setMouseOver(userDiv, user.id, '.userid_');
	},
	appendUserToGroup: function (user, group, groupEl) {

		var groupId = group.id;

		var isExpanded = Structr.isExpanded(groupId);
		_Entities.appendExpandIcon(groupEl, group, true, isExpanded);

		if (!isExpanded) {
			return;
		}

		var userDiv = _UsersAndGroups.createUserElement(user, group);

		groupEl.append(userDiv.css({
			top: 0,
			left: 0
		}));
		userDiv.removeClass('ui-state-disabled').removeClass('ui-draggable-disabled').removeClass('ui-draggable');

		_Entities.appendEditPropertiesIcon(userDiv, user);
		_UsersAndGroups.setMouseOver(userDiv, user.id, '.userid_');
	},
	deleteUser: function(button, user) {
		_Logger.log(_LogType.SECURTIY, 'deleteUser ' + user);
		_Entities.deleteNode(button, user);
	},

	refreshGroups: function() {
		_Security.groups.empty();
		_Security.groups.append('<button class="add_group_icon button"><img title="Add Group" alt="Add Group" src="' + _Icons.group_add_icon + '"> Add Group</button>');
		$('.add_group_icon', main).on('click', function(e) {
			e.stopPropagation();
			return Command.create({type: 'Group'});
		});
		var groupPager = _Pager.addPager('groups', _Security.groups, true, 'Group', 'public');
		groupPager.pager.append('<div>Filter: <input type="text" class="filter" data-attribute="name"></div>');
		groupPager.activateFilterElements();
	},
	createGroupElement: function (group) {
		var groupElement = $(
				'<div class="node group groupid_' + group.id + '">'
				+ '<img class="typeIcon" src="' + _Icons.group_icon + '">'
				+ ' <b title="' + group.name + '" class="name_">' + group.name + '</b> <span class="id">' + group.id + '</span>'
				+ '<img title="Delete Group ' + group.id + '" alt="Delete Group ' + group.id + '" class="delete_icon button" src="' + _Icons.delete_icon + '">'
				+ '</div>'
		);
		groupElement.data('groupId', group.id);

		$('.delete_icon', groupElement).on('click', function(e) {
			e.stopPropagation();
			_UsersAndGroups.deleteGroup(this, group);
		});

		return groupElement;
	},
	appendGroupElement: function(group) {

		if (!_Security.groups || !_Security.groups.is(':visible')) {
			return;
		}

		var hasChildren = group.members && group.members.length;

		_Logger.log(_LogType.SECURTIY, 'appendGroupElement', group, hasChildren);

		var groupDiv = _UsersAndGroups.createGroupElement(group);
		_Security.groups.append(groupDiv);

		_Entities.appendExpandIcon(groupDiv, group, hasChildren, Structr.isExpanded(group.id));
		_Entities.appendEditPropertiesIcon(groupDiv, group);
		_UsersAndGroups.setMouseOver(groupDiv, group.id, '.groupid_');

		groupDiv.droppable({
			accept: '.user',
			greedy: true,
			hoverClass: 'nodeHover',
			tolerance: 'pointer',
			drop: function(event, ui) {
				var userId = Structr.getUserId(ui.draggable);
				Command.appendUser(userId, group.id);
			}
		});

		if (hasChildren) {
			group.members.forEach(function(user) {
				_UsersAndGroups.appendUserToGroup(user, group, groupDiv);
			});
		}

		return groupDiv;
	},
	deleteGroup: function(button, group) {
		_Logger.log(_LogType.SECURTIY, 'deleteGroup ' + group);
		_Entities.deleteNode(button, group);
	},

	setMouseOver: function (node, id, prefix) {
		node.children('b.name_').off('click').on('click', function(e) {
			e.stopPropagation();
			_Entities.makeAttributeEditable(node, id, 'b.name_', 'name');
		});

		node.on({
			mouseover: function(e) {
				e.stopPropagation();
				_UsersAndGroups.activateNodeHover(id, prefix);
			},
			mouseout: function(e) {
				e.stopPropagation();
				_UsersAndGroups.deactivateNodeHover(id, prefix);
			}
		});
	},
	activateNodeHover: function (id, prefix) {
		var nodes = $(prefix + id);
		nodes.each(function (i, el) {
			$(el).addClass('nodeHover').children('img.button').show().css('display', 'inline-block');;
		});
	},
	deactivateNodeHover: function (id, prefix) {
		var nodes = $(prefix + id);
		nodes.each(function (i, el) {
			$(el).removeClass('nodeHover').children('img.button').hide();
		});
	}
};

var _ResourceAccessGrants = {

	refreshResourceAccesses: function() {
		_Security.resourceAccesses.empty();

		Structr.ensureIsAdmin(_Security.resourceAccesses, function() {

			var raPager = _Pager.addPager('resource-access', _Security.resourceAccesses, true, 'ResourceAccess', 'public');

			raPager.cleanupFunction = function () {
				$('#resourceAccesses table tbody tr').remove();
			};

			_Security.resourceAccesses.append('<table id="resourceAccessesTable"><thead><tr><th></th><th colspan="6" class="center">Authenticated users</th><th colspan="6" class="center">Non-authenticated (public) users</th><th colspan="3"></th></tr><tr><th class="title-cell">Signature</th><th>GET</th><th>PUT</th><th>POST</th><th>DELETE</th><th>OPTIONS</th><th>HEAD</th>'
					+ '<th>GET</th><th>PUT</th><th>POST</th><th>DELETE</th><th>OPTIONS</th><th>HEAD</th><th>Bitmask</th><th></th></tr><tr><th><input type="text" class="filter" data-attribute="signature" placeholder="Filter..."></th><th colspan="15"></th></tr></thead></table>');
			_Security.resourceAccesses.append('Signature: <input type="text" size="20" id="resource-signature"> <button class="add_grant_icon button"><img title="Add Resource Grant" alt="Add Grant" src="' + _Icons.key_add_icon + '"> Add Grant</button>');

			raPager.activateFilterElements(_Security.resourceAccesses);

			$('.add_grant_icon', _Security.resourceAccesses).on('click', function (e) {
				_ResourceAccessGrants.addResourceGrant(e);
			});

			$('#resource-signature', _Security.resourceAccesses).on('keyup', function (e) {
				if (e.keyCode === 13) {
					_ResourceAccessGrants.addResourceGrant(e);
				}
			});
		});
	},
	addResourceGrant: function(e) {
		e.stopPropagation();

		var inp = $('#resource-signature');
		inp.prop('disabled', 'disabled').addClass('disabled').addClass('read-only');
		$('.add_grant_icon', _Security.resourceAccesses).prop('disabled', 'disabled').addClass('disabled').addClass('read-only');

		var reEnableInput = function () {
			$('.add_grant_icon', _Security.resourceAccesses).removeProp('disabled').removeClass('disabled').removeClass('readonly');
			inp.removeProp('disabled').removeClass('disabled').removeClass('readonly');
		};

		var sig = inp.val();
		if (sig) {
			Command.create({type: 'ResourceAccess', signature: sig, flags: 0}, function() {
				reEnableInput();
				inp.focus();
			});
		} else {
			reEnableInput();
			blinkRed(inp);
		}
		window.setTimeout(reEnableInput, 250);
	},

	deleteResourceAccess: function(button, resourceAccess) {
		_Logger.log(_LogType.SECURTIY, 'deleteResourceAccess ' + resourceAccess);
		_Entities.deleteNode(button, resourceAccess);
	},
	appendResourceAccessElement: function(resourceAccess) {

		if (!_Security.resourceAccesses || !_Security.resourceAccesses.is(':visible')) {
			return;
		}

		var mask = {
			//FORBIDDEN                   : 0,
			AUTH_USER_GET               : 1,
			AUTH_USER_PUT               : 2,
			AUTH_USER_POST              : 4,
			AUTH_USER_DELETE            : 8,
			AUTH_USER_OPTIONS           : 256,
			AUTH_USER_HEAD              : 1024,

			NON_AUTH_USER_GET           : 16,
			NON_AUTH_USER_PUT           : 32,
			NON_AUTH_USER_POST          : 64,
			NON_AUTH_USER_DELETE        : 128,
			NON_AUTH_USER_OPTIONS       : 512,
			NON_AUTH_USER_HEAD          : 2048
		};

		var flags = parseInt(resourceAccess.flags);
		var trHtml = '<tr id="id_' + resourceAccess.id + '" class="resourceAccess"></tr>';

		var replaceElement = $('#resourceAccessesTable #id_' + resourceAccess.id);

		if (replaceElement && replaceElement.length) {
			replaceElement.replaceWith(trHtml);
		} else {
			$('#resourceAccessesTable').append(trHtml);
		}

		var tr = $('#resourceAccessesTable tr#id_' + resourceAccess.id);

		tr.append('<td class="title-cell"><b title="' + resourceAccess.signature + '" class="name_">' + resourceAccess.signature + '</b></td>');

		Object.keys(mask).forEach(function(key) {
			tr.append('<td><input type="checkbox" ' + (flags & mask[key] ? 'checked="checked"' : '') + ' data-flag="' + mask[key] + '" class="resource-access-flag ' + key + '"></td>');
		});

		tr.append('<td><input type="text" class="bitmask" size="4" value="' + flags + '"></td>');
		var bitmaskInput = $('.bitmask', tr);
		bitmaskInput.on('blur', function() {
			_ResourceAccessGrants.updateResourceAccessFlags(resourceAccess.id, $(this).val());
		});

		bitmaskInput.keypress(function(e) {
			if (e.keyCode === 13) {
				_ResourceAccessGrants.updateResourceAccessFlags(resourceAccess.id, $(this).val());
			}
		});

		tr.append('<td><img title="Delete Resource Access ' + resourceAccess.id + '" alt="Delete Resource Access    ' + resourceAccess.id + '" class="delete-resource-access button" src="' + _Icons.delete_icon + '"></td>');
		$('.delete-resource-access', tr).on('click', function(e) {
			e.stopPropagation();
			resourceAccess.name = resourceAccess.signature;
			_ResourceAccessGrants.deleteResourceAccess(this, resourceAccess);
		});

		if (replaceElement && replaceElement.length) {
			blinkGreen(tr.find('td'));
		}

		var div = Structr.node(resourceAccess.id);

		$('#resourceAccessesTable #id_' + resourceAccess.id + ' input[type=checkbox].resource-access-flag').on('change', function() {
			var newFlags = 0;
			tr.find('input:checked').each(function(i, input) {
				newFlags += parseInt($(input).attr('data-flag'));
			});
			_ResourceAccessGrants.updateResourceAccessFlags(resourceAccess.id, newFlags);
		});

		return div;
	},
	updateResourceAccessFlags: function (id, newFlags) {

		Command.setProperty(id, 'flags', newFlags, false, function() {
			Command.get(id, function(obj) {
				_ResourceAccessGrants.appendResourceAccessElement(obj);
			});
		});

	}

};