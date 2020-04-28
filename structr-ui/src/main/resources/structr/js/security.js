/*
 * Copyright (C) 2010-2020 Structr GmbH
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
	Structr.registerModule(_Security);
	Structr.classes.push('user');
	Structr.classes.push('group');
	Structr.classes.push('resourceAccess');
});

var _Security = {
	_moduleName: 'security',
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

		Structr.updateMainHelpLink('https://support.structr.com/article/207');

		Structr.fetchHtmlTemplate('security/main', {}, function (html) {

			main.append(html);

			_Security.groups = $('#groups');
			_Security.users = $('#users');
			_Security.resourceAccesses = $('#resourceAccesses');

			var activeTab = LSWrapper.getItem(_Security.securityTabKey) || 'usersAndGroups';
			_Security.selectTab(activeTab);

			$('#securityTabsMenu > li > a').on('click', function() {
				activeTab = $(this).attr('id').slice(0, -1);
				_Security.selectTab(activeTab);
			});

			Structr.unblockMenu(100);
		});
	},
	selectTab: function(tab) {

		LSWrapper.setItem(_Security.securityTabKey, tab);
		$('#securityTabsMenu > li').removeClass('active');
		$('#' + tab + '_').parent().addClass('active');
		if (tab === 'usersAndGroups') {
			$('#usersAndGroups').show();
			$('#resourceAccess').hide();
			_UsersAndGroups.refreshUsers();
			_UsersAndGroups.refreshGroups();
		} else if (tab === 'resourceAccess') {
			$('#resourceAccess').show();
			$('#usersAndGroups').hide();
			_ResourceAccessGrants.refreshResourceAccesses();
		}
	}
};

var _UsersAndGroups = {

	refreshUsers: function() {

		Structr.fetchHtmlTemplate('security/button.user.new', {}, function (html) {

			_Security.users.empty();
			_Security.users.append(html);

			$('.add_user_icon', main).on('click', function(e) {
				e.stopPropagation();
				return Command.create({type: $('select#user-type').val()});
			});

			$('select#user-type').on('change', function() {
				$('#add-user-button', main).find('span').text('Add ' + $(this).val());
			});

			// list types that extend User
			_Schema.getDerivedTypes('org.structr.dynamic.User', [], function(types) {
				var elem = $('select#user-type');
				types.forEach(function(type) {
					elem.append('<option value="' + type + '">' + type + '</option>');
				});
			});

			var userPager = _Pager.addPager('users', _Security.users, true, 'User', 'public');
			userPager.pager.append('<div>Filter: <input type="text" class="filter" data-attribute="name"></th></div>');
			userPager.activateFilterElements();
		});
	},
	createUserElement:function (user, group) {

		var userName = user.name ? user.name : user.eMail ? '[' + user.eMail + ']' : '[unnamed]';
		var userIcon = user.type === 'LDAPUser' ? _Icons.getFullSpriteClass(_Icons.user_orange_icon) : _Icons.getFullSpriteClass(_Icons.user_icon);

		var userElement = $('<div class="node user userid_' + user.id + '">'
				+ '<i class="typeIcon ' + userIcon + ' typeIcon-nochildren" />'
				+ ' <b title="' + userName + '" class="name_">' + userName + '</b> <span class="id">' + user.id + '</span>'
				+ '</div>'
		);
		userElement.data('userId', user.id);

		if (group) {
			userElement.append('<i title="Remove user \'' + userName + '\' from group \'' + group.name + '\'" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.user_delete_icon) + '" />');

			$('.delete_icon', userElement).on('click', function(e) {
				e.stopPropagation();
				Command.removeFromCollection(group.id, 'members', user.id, function () {
					_UsersAndGroups.deactivateNodeHover(user.id, '.userid_');
				});
			});
		} else {
			userElement.append('<i title="Delete user \'' + userName + '\'" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />');

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
		$('.typeIcon', userDiv).removeClass('typeIcon-nochildren');

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
	appendMemberToGroup: function (member, group, groupEl) {
		var groupId = group.id;
		var prefix = (member.isUser) ? '.userid_' : '.groupid_';

		var isExpanded = Structr.isExpanded(groupId);

		groupEl.each(function (idx, grp) {
			_Entities.appendExpandIcon($(grp), group, true, isExpanded);
		});

		if (!isExpanded) {
			return;
		}

		if (member.isUser) {

			var userDiv = _UsersAndGroups.createUserElement(member, group);

			groupEl.append(userDiv.css({
				top: 0,
				left: 0
			}));
			userDiv.removeClass('disabled');

			_Entities.appendEditPropertiesIcon(userDiv, member);
			_UsersAndGroups.setMouseOver(userDiv, member.id, prefix);

		} else {

			groupEl.each(function (idx, grpEl) {

				var groupDiv = _UsersAndGroups.createGroupElement(member);

				$('.delete_icon', groupDiv).remove();

				groupDiv.append('<i title="Remove \'' + member.name + '\' from group \'' + group.name + '\'" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.user_delete_icon) + '" />');

				$('.delete_icon', groupDiv).on('click', function(e) {
					e.stopPropagation();
					Command.removeFromCollection(group.id, 'members', member.id, function () {
						_UsersAndGroups.deactivateNodeHover(member.id, prefix);
					});
				});

				$(grpEl).append(groupDiv.css({
					top: 0,
					left: 0
				}));

				groupDiv.removeClass('disabled');

				_Entities.appendEditPropertiesIcon(groupDiv, member);
				_UsersAndGroups.setMouseOver(groupDiv, member.id, prefix);

				if (member.members === null) {
					Command.get(member.id, null, function (fetchedGroup) {
						fetchedGroup.members.forEach(function(subMember) {
							_UsersAndGroups.appendMemberToGroup(subMember, member, groupDiv);
						});
					});
				} else if (member.members && member.members.length) {
					member.members.forEach(function(subMember) {
						_UsersAndGroups.appendMemberToGroup(subMember, member, groupDiv);
					});
				}
			});
		}
	},
	deleteUser: function(button, user) {
		_Entities.deleteNode(button, user);
	},
	refreshGroups: function() {

		Structr.fetchHtmlTemplate('security/button.group.new', {}, function (html) {

			_Security.groups.empty();
			_Security.groups.append(html);

			$('.add_group_icon', main).on('click', function(e) {
				e.stopPropagation();
				return Command.create({type: $('select#group-type').val()});
			});

			$('select#group-type').on('change', function() {
				$('#add-group-button', main).find('span').text('Add ' + $(this).val());
			});

			// list types that extend User
			_Schema.getDerivedTypes('org.structr.dynamic.Group', [], function(types) {
				var elem = $('select#group-type');
				types.forEach(function(type) {
					elem.append('<option value="' + type + '">' + type + '</option>');
				});
			});

			var groupPager = _Pager.addPager('groups', _Security.groups, true, 'Group', 'public');
			groupPager.pager.append('<div>Filter: <input type="text" class="filter" data-attribute="name"></div>');
			groupPager.activateFilterElements();
		});
	},
	createGroupElement: function (group) {

		var groupIcon = group.type === 'LDAPGroup' ? _Icons.getFullSpriteClass(_Icons.group_link_icon) : _Icons.getFullSpriteClass(_Icons.group_icon);
		var groupElement = $('<div class="node group groupid_' + group.id + '">'
				+ '<i class="typeIcon ' + groupIcon + ' typeIcon-nochildren" />'
				+ ' <b title="' + group.name + '" class="name_">' + group.name + '</b> <span class="id">' + group.id + '</span>'
				+ '<i title="Delete Group ' + group.id + '" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />'
				+ '</div>'
		);
		groupElement.data('groupId', group.id);

		$('.delete_icon', groupElement).on('click', function(e) {
			e.stopPropagation();
			_UsersAndGroups.deleteGroup(this, group);
		});

		return groupElement;
	},
	appendGroupElement: function(element, group) {

		var hasChildren = group.members && group.members.length;

		_Logger.log(_LogType.SECURTIY, 'appendGroupElement', group, hasChildren);

		var groupDiv = _UsersAndGroups.createGroupElement(group);
		//$('.typeIcon', groupDiv).removeClass('typeIcon-nochildren');
		element.append(groupDiv);

		_Entities.appendExpandIcon(groupDiv, group, hasChildren, Structr.isExpanded(group.id));
		_Entities.appendEditPropertiesIcon(groupDiv, group);
		_UsersAndGroups.setMouseOver(groupDiv, group.id, '.groupid_');

		groupDiv.droppable({
			accept: '.user, .group',
			greedy: true,
			hoverClass: 'nodeHover',
			tolerance: 'pointer',
			drop: function(event, ui) {
				var nodeId;

				if (ui.draggable.hasClass('user')) {
					nodeId = Structr.getUserId(ui.draggable);
				} else if (ui.draggable.hasClass('group')) {
					nodeId = Structr.getGroupId(ui.draggable);
				}

				if (nodeId) {
					if (nodeId !== group.id) {
						Command.appendMember(nodeId, group.id);
					} else {
						new MessageBuilder()
								.title("Warning")
								.warning("Prevented adding group as a member of itself")
								.show();
					}
				} else {
					_Logger.log(_LogType.SECURTIY, 'drop on group -> could not identify node/user', ui.draggable);
				}
			}
		});

		groupDiv.draggable({
			revert: 'invalid',
			helper: 'clone',
			stack: '.node',
			appendTo: '#main',
			zIndex: 99
		});

		if (hasChildren) {
			group.members.forEach(function(member) {
				_UsersAndGroups.appendMemberToGroup(member, group, groupDiv);
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
			$(el).addClass('nodeHover').children('i.button').showInlineBlock();
		});
	},
	deactivateNodeHover: function (id, prefix) {
		var nodes = $(prefix + id);
		nodes.each(function (i, el) {
			$(el).removeClass('nodeHover').children('i.button').hide();
		});
	}
};

var _ResourceAccessGrants = {

	refreshResourceAccesses: function() {
		_Security.resourceAccesses.empty();

		Structr.fetchHtmlTemplate('security/resource-access', {}, function (html) {

			_Security.resourceAccesses.append(html);

			var raPager = _Pager.addPager('resource-access', $('#resourceAccessesPager', _Security.resourceAccesses), true, 'ResourceAccess', 'public');

			raPager.cleanupFunction = function () {
				$('#resourceAccessesTable tbody tr').remove();
			};

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
		inp.attr('disabled', 'disabled').addClass('disabled').addClass('read-only');
		$('.add_grant_icon', _Security.resourceAccesses).attr('disabled', 'disabled').addClass('disabled').addClass('read-only');

		var reEnableInput = function () {
			$('.add_grant_icon', _Security.resourceAccesses).attr('disabled', null).removeClass('disabled').removeClass('read-only');
			inp.attr('disabled', null).removeClass('disabled').removeClass('readonly');
		};

		var sig = inp.val();
		if (sig) {
			Command.create({type: 'ResourceAccess', signature: sig, flags: 0}, function() {
				reEnableInput();
				inp.val('');
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
			AUTH_USER_PATCH             : 4096,

			NON_AUTH_USER_GET           : 16,
			NON_AUTH_USER_PUT           : 32,
			NON_AUTH_USER_POST          : 64,
			NON_AUTH_USER_DELETE        : 128,
			NON_AUTH_USER_OPTIONS       : 512,
			NON_AUTH_USER_HEAD          : 2048,
			NON_AUTH_USER_PATCH         : 8192
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

		tr.append('<td><i title="Delete Resource Access ' + resourceAccess.id + '" class="delete-resource-access button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" /></td>');
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
			Command.get(id, 'id,flags,name,signature', function(obj) {
				_ResourceAccessGrants.appendResourceAccessElement(obj);
			});
		});
	}
};