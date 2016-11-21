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

	init : function() {
		_Pager.initPager('users',           'User', 1, 25, 'name', 'asc');
		_Pager.initPager('groups',          'Group', 1, 25, 'name', 'asc');
		_Pager.initPager('resource-access', 'ResourceAccess', 1, 25, 'signature', 'asc');
	},

	onload : function() {
		_Security.init();

		Structr.updateMainHelpLink('http://docs.structr.org/frontend-user-guide#Users and Groups');
		_Logger.log(_LogType.SECURTIY, 'onload');

		main.append('<div id="securityTabs"><ul id="securityTabsMenu"><li><a id="usersAndGroups_" href="#usersAndGroups"><span>Users and Groups</span></a></li><li><a id="resourceAccess_" href="#resourceAccess"><span>Resource Access Grants</span></a></li></ul><div id="usersAndGroups"></div><div id="resourceAccess"></div></div>');

		$('#usersAndGroups').append('<div><div class="fit-to-height" id="users"></div><div class="fit-to-height" id="groups"></div></div>');
		$('#resourceAccess').append('<div><div class="" id="resourceAccesses"></div></div>');

		_Security.groups = $('#groups');
		_Security.users = $('#users');
		_Security.resourceAccesses = $('#resourceAccesses');

		$('#securityTabs').tabs({
			activate: function(event, ui) {
				var activeTab = ui.newPanel[0].id;
				LSWrapper.setItem(_Security.securityTabKey, activeTab);

				if (activeTab === 'usersAndGroups') {
					_Security.refreshGroups();
					_Security.refreshUsers();
				} else if (activeTab === 'resourceAccess') {
					_Security.refreshResourceAccesses();
				}

			}
		});

		var activeTab = LSWrapper.getItem(_Security.securityTabKey) || 'usersAndGroups';
		if (activeTab === 'usersAndGroups') {
			_Security.refreshGroups();
			_Security.refreshUsers();
		} else {
			$('a[href="#' + activeTab + '"]').click();
		}

		_Security.resize();
		$(window).off('resize');
		$(window).on('resize', function() {
			_Security.resize();
		});

		Structr.unblockMenu(100);

	},

	refreshResourceAccesses : function() {
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
				_Security.addResourceGrant(e);
			});

			$('#resource-signature', _Security.resourceAccesses).on('keyup', function (e) {
				if (e.keyCode === 13) {
					_Security.addResourceGrant(e);
				}
			});

			_Security.resize();
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

	refreshGroups : function() {
		_Security.groups.empty();
		_Security.groups.append('<button class="add_group_icon button"><img title="Add Group" alt="Add Group" src="' + _Icons.group_add_icon + '"> Add Group</button>');
		$('.add_group_icon', main).on('click', function(e) {
			e.stopPropagation();
			return Command.create({'type':'Group'});
		});
		var grpPager = _Pager.addPager('groups', _Security.groups, true, 'Group', 'public');
		grpPager.pager.append('<div>Filter: <input type="text" class="filter" data-attribute="name"></div>');
		grpPager.activateFilterElements();

	},

	refreshUsers : function() {
		_Security.users.empty();
		_Security.users.append('<button class="add_user_icon button"><img title="Add User" alt="Add User" src="' + _Icons.user_add_icon + '"> Add User</button>');
		$('.add_user_icon', main).on('click', function(e) {
			e.stopPropagation();
			return Command.create({'type':'User'});
		});
		var usrPager = _Pager.addPager('users', _Security.users, true, 'User', 'public');
		usrPager.pager.append('<div>Filter: <input type="text" class="filter" data-attribute="name"></th></div>');
		usrPager.activateFilterElements();
	},

	deleteUser : function(button, user) {
		_Logger.log(_LogType.SECURTIY, 'deleteUser ' + user);
		_Entities.deleteNode(button, user);
	},

	deleteGroup : function(button, group) {
		_Logger.log(_LogType.SECURTIY, 'deleteGroup ' + group);
		_Entities.deleteNode(button, group);
	},

	deleteResourceAccess : function(button, resourceAccess) {
		_Logger.log(_LogType.SECURTIY, 'deleteResourceAccess ' + resourceAccess);
		_Entities.deleteNode(button, resourceAccess);
	},

	appendResourceAccessElement : function(resourceAccess, replaceElement) {

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

		if (replaceElement) {
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
			var id = tr.attr('id').substring(3);
			var newFlags = $(this).val();
			Command.setProperty(id, 'flags', newFlags, false, function() {
				Command.get(id, function(obj) {
					_Security.appendResourceAccessElement(obj, tr);
				});
			});
		});

		bitmaskInput.keypress(function(e) {
			if (e.keyCode === 13) {
				var id = tr.attr('id').substring(3);
				var newFlags = $(this).val();
				Command.setProperty(id, 'flags', newFlags, false, function() {
					Command.get(id, function(obj) {
						_Security.appendResourceAccessElement(obj, tr);
					});
				});
			}
		});

		tr.append('<td><img title="Delete Resource Access ' + resourceAccess.id + '" alt="Delete Resource Access    ' + resourceAccess.id + '" class="delete-resource-access button" src="' + _Icons.delete_icon + '"></td>');
		$('.delete-resource-access', tr).on('click', function(e) {
			e.stopPropagation();
			resourceAccess.name = resourceAccess.signature;
			_Security.deleteResourceAccess(this, resourceAccess);
		});

		if (replaceElement) {
			blinkGreen(tr.find('td'));
		}

		var div = Structr.node(resourceAccess.id);

		$('#resourceAccessesTable #id_' + resourceAccess.id + ' input[type=checkbox].resource-access-flag').on('change', function() {
			var inp = $(this);
			var tr = inp.closest('tr');
			var id = tr.attr('id').substring(3);
			var newFlags = 0;
			tr.find('input:checked').each(function(i, input) {
				newFlags += parseInt($(input).attr('data-flag'));
			});
			Command.setProperty(id, 'flags', newFlags, false, function() {
				Command.get(id, function(obj) {
					_Security.appendResourceAccessElement(obj, tr);
				});
			});
		});

		return div;
	},

	appendGroupElement : function(group) {

		if (!_Security.groups || !_Security.groups.is(':visible')) {
			return;
		}

		var hasChildren = group.members && group.members.length;
		_Logger.log(_LogType.SECURTIY, 'appendGroupElement', group, hasChildren);
		_Security.groups.append('<div id="id_' + group.id + '" class="node group">'
			+ '<img class="typeIcon" src="' + _Icons.group_icon + '">'
			+ '<b title="' + group.name + '" class="name_">' + group.name + '</b> <span class="id">' + group.id + '</span>'
			+ '</div>');
		var div = Structr.node(group.id);

		div.append('<img title="Delete Group ' + group.id + '" alt="Delete Group ' + group.id + '" class="delete_icon button" src="' + _Icons.delete_icon + '">');
		$('.delete_icon', div).on('click', function(e) {
			e.stopPropagation();
			_Security.deleteGroup(this, group);
		});

		_Entities.appendExpandIcon(div, group, hasChildren);

		div.droppable({
			accept: '.user',
			greedy: true,
			hoverClass: 'nodeHover',
			tolerance: 'pointer',
			drop: function(event, ui) {
				var self = $(this);
				var userId = Structr.getId(ui.draggable);
				var groupId = Structr.getId(self);
				Structr.addExpandedNode(groupId);
				Command.appendUser(userId, groupId);
				$(ui.draggable).remove();
			}
		});

		_Entities.appendEditPropertiesIcon(div, group);
		_Entities.setMouseOver(div);

		return div;
	},

	appendUserElement : function(user, group) {
		_Logger.log(_LogType.SECURTIY, 'appendUserElement', user);

		if (!_Security.users || !_Security.users.is(':visible')) {
			return;
		}

		if (user.groups && user.groups.length > 0) {
			for (i=0; i < user.groups.length; i++) {
				var groupElement = Structr.node(user.groups[i].id);
				if (groupElement) {
					// at least one of the users groups is visible
					return _Security.appendUserToGroup(user, user.groups[i], groupElement);
				}
			}
		}

		// none of the users groups is visible (or user has no groups)
		return _Security.appendUserToUserList(user);
	},
	appendUserToUserList: function (user) {
		_Security.users.append(_Security.getUserElementMarkup(user));

		var name = _Security.getUserName(user);
		var div = Structr.node(user.id);
		if (!div || !div.length) return;


		var newDelIcon = '<img title="Delete user \'' + name + '\'" alt="Delete user \'' + name + '\'" class="delete_icon button" src="' + _Icons.delete_icon + '">';
		var delIcon = $('.delete_icon', div);

		if (delIcon && delIcon.length) {
			delIcon.replaceWith(newDelIcon);
		} else {
			div.append(newDelIcon);
			delIcon = $('.delete_icon', div);
		}

		delIcon.on('click', function(e) {
			e.stopPropagation();
			_Security.deleteUser(this, user);
		});

		div.draggable({
			revert: 'invalid',
			helper: 'clone',
			stack: '.node',
			appendTo: '#main',
			zIndex: 99
		});

		_Entities.appendEditPropertiesIcon(div, user);
		_Entities.setMouseOver(div);

		return div;
	},
	appendUserToGroup: function (user, group, parent) {
		var delIcon;
		var div = Structr.node(user.id);
		var group = user.groups[0];

		var groupId = group.id;

		if (!Structr.isExpanded(groupId)) {
			return;
		}

		var newDelIcon = '<img title="Remove user \'' + user.name + '\' from group \'' + group.name + '\'" alt="Remove user ' + user.name + ' from group \'' + group.name + '\'" class="delete_icon button" src="' + _Icons.user_delete_icon + '">';

		_Logger.log(_LogType.SECURTIY, 'parent, div', parent, div);

		if (div && div.length) {
			parent.append(div.css({
				top: 0,
				left: 0
			}));
			delIcon = $('.delete_icon', div);
			delIcon.replaceWith(newDelIcon);

			_Logger.log(_LogType.SECURTIY, '################ disable delete icon');

		} else {

			_Logger.log(_LogType.SECURTIY, '### new user, appending to ', parent);

			if (parent) {
				parent.append(_Security.getUserElementMarkup(user));
				div = Structr.node(user.id);
				div.append(newDelIcon);
			} else {
				// group is not visible
				div = _Security.appendUserToUserList(user);
			}

		}
		delIcon = $('.delete_icon', div);
		delIcon.on('click', function(e) {
			e.stopPropagation();
			Command.removeSourceFromTarget(user.id, groupId);
		});

		// disable delete icon on parent
		Structr.disableButton($('.delete_icon', parent)[0]);

		div.removeClass('ui-state-disabled').removeClass('ui-draggable-disabled').removeClass('ui-draggable');

		_Entities.appendEditPropertiesIcon(div, user);
		_Entities.setMouseOver(div);

		return div;
	},
	getUserElementMarkup:function (user) {
		var name = _Security.getUserName(user);
		return '<div id="id_' + user.id + '" class="node user">'
			+ '<img class="typeIcon" src="' + _Icons.user_icon + '">'
			+ ' <b title="' + name + '" class="name_">' + name + '</b> <span class="id">' + user.id + '</span>'
			+ '</div>';
	},
	resize: function() {

		Structr.resize();

		var w = $(window).width();
		var h = $(window).height();

		var ml = 0;
		var mt = 24;

		// Calculate dimensions of dialog
		var dw = Math.min(900, w - ml);
		var dh = Math.min(600, h - mt);
		//            var dw = (w-24) + 'px';
		//            var dh = (h-24) + 'px';

		var l = parseInt((w - dw) / 2);
		var t = parseInt((h - dh) / 2);

		$('#securityTabs #resourceAccess #resourceAccessesTable').css({
			height: h - ($('#securityTabsMenu').height() + 177) + 'px',
			width:  w - 59 + 'px'
		});

		$('#securityTabs #usersAndGroups').css({
			height: h - ($('#securityTabsMenu').height() + 112) + 'px',
			width:  w - 60 + 'px'
		});

		$('.searchResults').css({
			height: h - 103 + 'px'
		});
	},
	getUserName: function(user) {
		return name = user.name ? user.name : user.eMail ? '[' + user.eMail + ']' : '[unnamed]';
	}
};