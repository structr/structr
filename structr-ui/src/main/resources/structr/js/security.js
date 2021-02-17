/*
 * Copyright (C) 2010-2021 Structr GmbH
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
	onload: async function() {

		_Security.init();

		Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('security'));

		let html = await Structr.fetchHtmlTemplate('security/main', {});

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

	refreshUsers: async function() {

		let html = await Structr.fetchHtmlTemplate('security/button.user.new', {});

		_Security.users.empty();
		_Security.users.append(html);

		$('.add_user_icon', main).on('click', function(e) {
			e.stopPropagation();
			return Command.create({type: $('select#user-type').val()});
		});

		$('select#user-type').on('change', function() {
			$('#add-user-button', main).find('span').text('Add ' + $(this).val());
		});

		_Schema.getDerivedTypes('org.structr.dynamic.User', [], function(types) {
			var elem = $('select#user-type');
			types.forEach(function(type) {
				elem.append('<option value="' + type + '">' + type + '</option>');
			});
		});

		var userPager = _Pager.addPager('users', _Security.users, true, 'User', 'public');
		userPager.pager.append('<div>Filter: <input type="text" class="filter" data-attribute="name"></th></div>');
		userPager.activateFilterElements();
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

		_UsersAndGroups.makeDraggable(userElement);

		return userElement;
	},
	appendUserToUserList: function (user) {

		if (!_Security.users || !_Security.users.is(':visible')) {
			return;
		}

		var userDiv = _UsersAndGroups.createUserElement(user);
		$('.typeIcon', userDiv).removeClass('typeIcon-nochildren');

		_Security.users.append(userDiv);

		_Entities.appendEditPropertiesIcon(userDiv, user);
		_UsersAndGroups.setMouseOver(userDiv, user.id, '.userid_');
	},
	appendMembersToGroup: function(members, group, groupDiv) {

		for (let member of members) {

			// if model is not yet loaded (can happen for users/groups which are not part of the current page (but which are visible as members of visible groups)
			if (!StructrModel.obj(member.id)) {

				if (member.isGroup) {
					// can have members which are not loaded yet
					// if a user is removed from this group the model will try to filter its members array (which is null atm) which leads to an error, therefor fetch it
					Command.get(member.id, null, (g) => {
						StructrModel.createFromData(g, group.id, true);
					});

				} else {

					// member is a user, no danger
					StructrModel.createFromData(member, group.id, true);
				}

			} else {
				_UsersAndGroups.appendMemberToGroup(member, group, groupDiv);
			}
		}
	},
	appendMemberToGroup: function (member, group, groupEl) {

		let groupId    = group.id;
		let isExpanded = Structr.isExpanded(groupId);

		_Entities.appendExpandIcon(groupEl, group, true, isExpanded);

		if (!isExpanded) {
			return;
		}

		var prefix = (member.isUser) ? '.userid_' : '.groupid_';

		if (member.isUser) {

			groupEl.each(function (idx, grpEl) {

				let memberAlreadyListed = $(prefix + member.id, grpEl).length > 0;
				if (!memberAlreadyListed) {

					var userDiv = _UsersAndGroups.createUserElement(member, group);

					$(grpEl).append(userDiv.css({
						top: 0,
						left: 0
					}));
					userDiv.removeClass('disabled');

					_Entities.appendEditPropertiesIcon(userDiv, member);
					_UsersAndGroups.setMouseOver(userDiv, member.id, prefix);
				}
			});

		} else {

			let alreadyShownInParents = _UsersAndGroups.isGroupAlreadyShown(member, groupEl);
			let alreadyShownInMembers = $(prefix + member.id, groupEl).length > 0;

			if (!alreadyShownInMembers && !alreadyShownInParents) {

				var groupDiv = _UsersAndGroups.createGroupElement(member);

				$('.delete_icon', groupDiv).remove();

				groupDiv.append('<i title="Remove \'' + member.name + '\' from group \'' + group.name + '\'" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.user_delete_icon) + '" />');

				$('.delete_icon', groupDiv).on('click', function(e) {
					e.stopPropagation();
					Command.removeFromCollection(group.id, 'members', member.id, function () {
						_UsersAndGroups.deactivateNodeHover(member.id, prefix);
					});
				});

				groupEl.append(groupDiv.css({
					top: 0,
					left: 0
				}));

				groupDiv.removeClass('disabled');

				_Entities.appendEditPropertiesIcon(groupDiv, member);
				_UsersAndGroups.setMouseOver(groupDiv, member.id, prefix);

				if (member.members === null) {
					Command.get(member.id, null, function (fetchedGroup) {
						_UsersAndGroups.appendMembersToGroup(fetchedGroup.members, member, groupDiv);
					});
				} else if (member.members && member.members.length) {
					_UsersAndGroups.appendMembersToGroup(member.members, member, groupDiv);
				}
			}
		}
	},
	isGroupAlreadyShown: function(group, groupEl) {
		if (groupEl.length === 0) {
			return false;
		}

		let containerGroupId = groupEl.data('groupId');
		if (containerGroupId === group.id) {
			return true;
		}
		return _UsersAndGroups.isGroupAlreadyShown(group, groupEl.parent().closest('.group'));
	},
	deleteUser: function(button, user) {
		_Entities.deleteNode(button, user);
	},
	refreshGroups: async function() {

		let html = await Structr.fetchHtmlTemplate('security/button.group.new', {});

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

		groupElement.droppable({
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
						new MessageBuilder().title("Warning").warning("Prevented adding group as a member of itself").show();
					}
				}
			}
		});

		_UsersAndGroups.makeDraggable(groupElement);

		return groupElement;
	},
	appendGroupElement: function(element, group) {

		var hasChildren = group.members && group.members.length;

		var groupDiv = _UsersAndGroups.createGroupElement(group);
		element.append(groupDiv);

		_Entities.appendExpandIcon(groupDiv, group, hasChildren, Structr.isExpanded(group.id));
		_Entities.appendEditPropertiesIcon(groupDiv, group);
		_UsersAndGroups.setMouseOver(groupDiv, group.id, '.groupid_');

		if (hasChildren) {
			_UsersAndGroups.appendMembersToGroup(group.members, group, groupDiv);
		}

		return groupDiv;
	},
	deleteGroup: function(button, group) {
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
	},
	makeDraggable: function(el) {
		el.draggable({
			revert: 'invalid',
			helper: 'clone',
			stack: '.node',
			appendTo: '#main',
			zIndex: 99
		});
	}
};

var _ResourceAccessGrants = {

	refreshResourceAccesses: async function() {

		_Security.resourceAccesses.empty();

		let html = await Structr.fetchHtmlTemplate('security/resource-access', {});

		_Security.resourceAccesses.append(html);

		Structr.activateCommentsInElement(_Security.resourceAccesses);

		let raPager = _Pager.addPager('resource-access', $('#resourceAccessesPager', _Security.resourceAccesses), true, 'ResourceAccess', undefined, undefined, _ResourceAccessGrants.customPagerTransportFunction, 'id,flags,name,type,signature,isResourceAccess,visibleToPublicUsers,visibleToAuthenticatedUsers,grantees');

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
	},
	customPagerTransportFunction: function(type, pageSize, page, filterAttrs, callback) {
		let filterString = "";
		let presentFilters = Object.keys(filterAttrs);
		if (presentFilters.length > 0) {
			filterString = 'WHERE ' + presentFilters.map(function(key) {
				if (key === 'flags') {
					return (filterAttrs[key] === true) ? 'n.flags > 0' : 'n.flags >= 0';
				} else {
					return 'n.' + key + ' =~ "(?i).*' + filterAttrs[key] + '.*"';
				}
			}).join(' AND ');
		}
		Command.cypher('MATCH (n:ResourceAccess) ' + filterString + ' RETURN DISTINCT n ORDER BY n.' + sortKey[type] + ' ' + sortOrder[type], undefined, callback, pageSize, page);
	},
	addResourceGrant: function(e) {
		e.stopPropagation();

		let inp = $('#resource-signature');
		inp.attr('disabled', 'disabled').addClass('disabled').addClass('read-only');
		$('.add_grant_icon', _Security.resourceAccesses).attr('disabled', 'disabled').addClass('disabled').addClass('read-only');

		let reEnableInput = function () {
			$('.add_grant_icon', _Security.resourceAccesses).attr('disabled', null).removeClass('disabled').removeClass('read-only');
			inp.attr('disabled', null).removeClass('disabled').removeClass('readonly');
		};

		let sig = inp.val();
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
		_Entities.deleteNode(button, resourceAccess);
	},
	getVerbFromKey: function(key = '') {
		return key.substring(key.lastIndexOf('_')+1, key.length);
	},
	appendResourceAccessElement: function(resourceAccess, blinkAfterUpdate = true) {

		if (!_Security.resourceAccesses || !_Security.resourceAccesses.is(':visible')) {
			return;
		}

		let mask = {
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

		let flags = parseInt(resourceAccess.flags);

		let trHtml = '<tr id="id_' + resourceAccess.id + '" class="resourceAccess"><td class="title-cell"><b class="name_">' + resourceAccess.signature + '</b></td>';

		let noAuthAccessPossible = resourceAccess.visibleToAuthenticatedUsers === false && (!resourceAccess.grantees || resourceAccess.grantees.length === 0);

		let flagWarningTexts = {};
		let flagSanityInfos  = {};

		let hasAuthFlag    = false;
		let hasNonAuthFlag = false;

		for (let key in mask) {

			let flagIsSet = (flags & mask[key]);

			let disabledBecauseNotPublic    = (key.startsWith('NON_AUTH_') && resourceAccess.visibleToPublicUsers === false);
			let disabledBecausePublic       = (key.startsWith('AUTH_')     && resourceAccess.visibleToPublicUsers === true);
			let disabledBecauseNoAuthAccess = (key.startsWith('AUTH_')     && noAuthAccessPossible);

			if (flagIsSet && disabledBecausePublic) {
				// CRITICAL: If the grant is visibleToPublicUsers and includes grants for authenticated users it can be a security threat! Disable and WARN
				let arr = flagWarningTexts[key] || [];
				arr.push('Should <b>not</b> be set because the grant itself is visible to public users. Every authenticated user can also see the grant which is probably not intended.<br><br>This might have a <b>security impact</b> as the resource is accessible with the <b>' + _ResourceAccessGrants.getVerbFromKey(key) + '</b> method (<b>but not the data</b> behind that resource)!<br><br><b>Quick Fix</b>: Remove the visibleToPublicUsers flag from this grant.');
				flagWarningTexts[key] = arr;
			}
			if (flagIsSet && disabledBecauseNotPublic) {
				let arr = flagSanityInfos[key] || [];
				arr.push('Active for public users but grant can not be seen by public users.<br><br>This has no security-impact and is probably only misconfigured.');
				flagSanityInfos[key] = arr;
			}
			if (flagIsSet && disabledBecauseNoAuthAccess) {
				let arr = flagSanityInfos[key] || [];
				arr.push('Active for authenticated users but grant can not be seen by authenticated users or a group.<br><br>This has no security-impact and is probably only misconfigured.');
				flagSanityInfos[key] = arr;
			}

			hasNonAuthFlag = hasNonAuthFlag || (flagIsSet && key.startsWith('NON_AUTH_'));
			hasAuthFlag    = hasAuthFlag    || (flagIsSet && key.startsWith('AUTH_'));

			let isDisabled = false && (disabledBecauseNotPublic || disabledBecauseNoAuthAccess);

			let additionalClasses = [];
			if (key === 'AUTH_USER_GET') { additionalClasses.push('bl-1'); }
			if (key === 'AUTH_USER_PATCH') { additionalClasses.push('br-1'); }
			if (key === 'NON_AUTH_USER_PATCH') { additionalClasses.push('br-1'); }

			trHtml += '<td class="' + additionalClasses.join(' ') + '"><input type="checkbox" ' + (flagIsSet ? 'checked="checked"' : '') + (isDisabled ? ' disabled' : '') + ' data-flag="' + mask[key] + '" class="resource-access-flag" data-key="' + key +'"></td>';
		}

		trHtml += '<td><input type="text" class="bitmask" size="4" value="' + flags + '"></td><td>' +
				'<i class="acl-resource-access button ' + _Icons.getFullSpriteClass(_Icons.key_icon) + '" /> ' +
				'<i title="Delete Resource Access ' + resourceAccess.id + '" class="delete-resource-access button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />' +
				'</td></tr>';

		let tr = $(trHtml);

		if (hasAuthFlag && hasNonAuthFlag) {
			Structr.appendInfoTextToElement({
				text: 'Grant has flags for authenticated and public users. This is probably misconfigured and should be changed or split into two grants.',
				element: $('.title-cell b', tr),
				customToggleIcon: _Icons.error_icon,
				css: {
					float:'right',
					marginRight: '5px'
				}
			});
		}

		for (let key in flagWarningTexts) {
			Structr.appendInfoTextToElement({
				text: flagWarningTexts[key].join('<br><br>'),
				element: $('input[data-key=' + key + ']', tr),
				customToggleIcon: _Icons.error_icon,
				css: {position:'absolute'},
				insertAfter: true
			});
		}

		for (let key in flagSanityInfos) {
			if (!flagWarningTexts[key]) {
				Structr.appendInfoTextToElement({
					text: flagSanityInfos[key].join('<br><br>'),
					element: $('input[data-key=' + key + ']', tr),
					customToggleIcon: _Icons.warning_icon,
					css: {position:'absolute'},
					insertAfter: true
				});
			}
		}

		_Entities.bindAccessControl($('.acl-resource-access', tr), resourceAccess);

		let replaceElement = $('#resourceAccessesTable #id_' + resourceAccess.id);

		if (replaceElement && replaceElement.length) {

			replaceElement.replaceWith(tr);
			if (blinkAfterUpdate) {
				blinkGreen(tr.find('td'));
			}

		} else {

			$('#resourceAccessesTable').append(tr);
		}

		let bitmaskInput = $('.bitmask', tr);
		bitmaskInput.on('blur', function() {
			_ResourceAccessGrants.updateResourceAccessFlags(resourceAccess.id, $(this).val());
		});

		bitmaskInput.keypress(function(e) {
			if (e.keyCode === 13) {
				$(this).blur();
			}
		});

		$('.delete-resource-access', tr).on('click', function(e) {
			e.stopPropagation();
			resourceAccess.name = resourceAccess.signature;
			_ResourceAccessGrants.deleteResourceAccess(this, resourceAccess);
		});

		let div = Structr.node(resourceAccess.id);

		$('input[type=checkbox].resource-access-flag', tr).on('change', function() {
			let newFlags = 0;
			tr.find('input:checked').each(function(i, input) {
				newFlags += parseInt($(input).attr('data-flag'));
			});
			_ResourceAccessGrants.updateResourceAccessFlags(resourceAccess.id, newFlags);
		});

		return div;
	},
	updateResourceAccessFlags: function (id, newFlags) {

		Command.setProperty(id, 'flags', newFlags, false, function() {
			_ResourceAccessGrants.updateResourcesAccessRow(id);
		});
	},
	updateResourcesAccessRow: function (id, blinkGreen = true) {
		Command.get(id, 'id,flags,type,signature,visibleToPublicUsers,visibleToAuthenticatedUsers,grantees', function(obj) {
			_ResourceAccessGrants.appendResourceAccessElement(obj, blinkGreen);
		});
	}
};