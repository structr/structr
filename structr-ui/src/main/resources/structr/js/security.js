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

let _Security = {
	_moduleName: 'security',
	userControls: undefined,
	userList: undefined,
	groupControls: undefined,
	groupList: undefined,
	resourceAccesses: undefined,
	securityTabKey: 'structrSecurityTab_' + location.port,
	init: function() {
		_Pager.initPager('users',           'User', 1, 25, 'name', 'asc');
		_Pager.initPager('groups',          'Group', 1, 25, 'name', 'asc');
		_Pager.initPager('resource-access', 'ResourceAccess', 1, 25, 'signature', 'asc');
	},
	onload: function() {

		_Security.init();

		Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('security'));

		Structr.fetchHtmlTemplate('security/functions', {}, function (html) {

			Structr.functionBar.innerHTML = html;

			UISettings.showSettingsForCurrentModule();

			Structr.fetchHtmlTemplate('security/main', {}, function (html) {

				main.append(html);

				_Security.userControls     = document.getElementById('users-controls');
				_Security.userList         = document.getElementById('users-list');
				_Security.groupControls    = document.getElementById('groups-controls');
				_Security.groupList        = document.getElementById('groups-list');
				_Security.resourceAccesses = $('#resourceAccesses');

				let subModule = LSWrapper.getItem(_Security.securityTabKey) || 'users-and-groups';

				document.querySelectorAll('#function-bar .tabs-menu li a').forEach(function(tabLink) {

					tabLink.addEventListener('click', function(e) {
						e.preventDefault();

						let urlHash = e.target.closest('a').getAttribute('href');
						let subModule = urlHash.split(':')[1];
						window.location.hash = urlHash;

						_Security.selectTab(subModule);
					});

					if (tabLink.closest('a').getAttribute('href') === '#security:' + subModule) {
						tabLink.click();
					}
				});

				Structr.unblockMenu(100);
			});
		});
	},
	getContextMenuElements: function (div, entity) {

		const isUser             = entity.isUser;
		const isGroup            = entity.isGroup;

		let elements = [];

		if (isUser || isGroup) {

			let userOrGroupEl = div.closest('.node');
			let parentGroupEl = userOrGroupEl.parent().closest('.group');
			if (parentGroupEl.length) {
				let parentGroupId = Structr.getGroupId(parentGroupEl);
				elements.push({
					name: 'Remove ' + entity.name + ' from ' + $('.name_', parentGroupEl).attr('title'),
					clickHandler: function () {
						Command.removeFromCollection(parentGroupId, 'members', entity.id, function () {
							_UsersAndGroups.refreshGroups();
						});
						return false;
					}
				});
			}

			_Elements.appendContextMenuSeparator(elements);
		}

		elements.push({
			name: 'Properties',
			clickHandler: function() {
				_Entities.showProperties(entity, 'ui');
				return false;
			}
		});

		_Elements.appendContextMenuSeparator(elements);

		_Elements.appendSecurityContextMenuItems(elements, entity);

		_Elements.appendContextMenuSeparator(elements);

		elements.push({
			icon: _Icons.getSvgIcon('trashcan'),
			classes: ['menu-bolder', 'danger'],
			name: 'Delete ' + entity.type,
			clickHandler: () => {

				_Entities.deleteNode(this, entity);

				return false;
			}
		});

		_Elements.appendContextMenuSeparator(elements);

		return elements;
	},
	selectTab: function(subModule) {

		LSWrapper.setItem(_Security.securityTabKey, subModule);

		document.querySelectorAll('#function-bar .tabs-menu li').forEach((tab) => {
			tab.classList.remove('active');
		});

		let tabLink = document.querySelector('#function-bar .tabs-menu li a[href="#security:' + subModule + '"]');
		if (tabLink) tabLink.closest('li').classList.add('active');

		if (subModule === 'users-and-groups') {

			$('#usersAndGroups').show();
			$('#resourceAccess').hide();
			_UsersAndGroups.refreshUsers();
			_UsersAndGroups.refreshGroups();

		} else if (subModule === 'resource-access') {

			$('#resourceAccess').show();
			$('#usersAndGroups').hide();
			_ResourceAccessGrants.refreshResourceAccesses();
		}
	}
};

let _UsersAndGroups = {

	refreshUsers: async () => {

		let types = await _Schema.getDerivedTypes('org.structr.dynamic.User', []);

		Structr.fetchHtmlTemplate('security/button.user.new', { types: types }, function (html) {

			_Security.userList.innerHTML     = '';
			_Security.userControls.innerHTML = html;

			let userTypeSelect = document.querySelector('select#user-type');
			let addUserButton  = document.getElementById('add-user-button');

			addUserButton.addEventListener('click', (e) => {
				Command.create({ type: userTypeSelect.value });
			});

			userTypeSelect.addEventListener('change', () => {
				addUserButton.querySelector('span').textContent = 'Add ' + userTypeSelect.value;
			});

			let userPager = _Pager.addPager('users', $(_Security.userControls), true, 'User', 'public', (users) => {
				for (let user of users) {
					let userModelObj = StructrModel.create(user);
					_UsersAndGroups.appendUserToUserList(userModelObj);
				}
			}, null, 'id,isUser,name,type,isAdmin');
			userPager.cleanupFunction = function () {
				_Security.userList.innerHTML = '';
			};
			userPager.pager.append('<div>Filter: <input type="text" class="filter" data-attribute="name"></th></div>');
			userPager.activateFilterElements();
		});
	},
	createUserElement:function (user) {

		let displayName = ((user.name) ? user.name : ((user.eMail) ? '[' + user.eMail + ']' : '[unnamed]'));

		let userElement = $('<div class="node user userid_' + user.id + '">'
				+ '<i class="typeIcon ' + _UsersAndGroups.getIconForPrincipal(user) + '"></i>'
				+ '<b title="' + displayName + '" class="name_ abbr-ellipsis abbr-75pc" data-input-class="max-w-75">' + displayName + '</b>'
				+ '</div>'
		);
		userElement.data('userId', user.id);

		_UsersAndGroups.makeDraggable(userElement);

		return userElement;
	},
	getIconForPrincipal: (principal) => {
		return _Icons.getFullSpriteClass(
			principal.isGroup ?
				((principal.type === 'LDAPGroup') ? _Icons.group_link_icon : _Icons.group_icon)
				:
				((principal.isAdmin === true) ? _Icons.user_red_icon : ((principal.type === 'LDAPUser') ? _Icons.user_orange_icon : _Icons.user_icon))
		);
	},
	updateUserElementAfterModelChange: (user) => {

		requestAnimationFrame(() => {

			// request animation frame is especially important if a name is completely removed!

			for (let userEl of document.querySelectorAll('.userid_' + user.id)) {

				let icon = userEl.querySelector('.typeIcon');
				if (icon) {
					icon.setAttribute('class', 'typeIcon ' + _UsersAndGroups.getIconForPrincipal(user));
				}

				let userName = userEl.querySelector('.name_');
				if (userName) {
					let displayName = ((user.name) ? user.name : ((user.eMail) ? '[' + user.eMail + ']' : '[unnamed]'));

					userName.setAttribute('title', displayName);
					userName.textContent = displayName;
				}
			}
		});
	},
	appendUserToUserList: function (user) {

		let userDiv = _UsersAndGroups.createUserElement(user);

		_Security.userList.appendChild(userDiv[0]);

		_Entities.appendContextMenuIcon(userDiv, user);
		_Elements.enableContextMenuOnElement(userDiv, user);
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
						let groupModelObj = StructrModel.createFromData(g);
						_UsersAndGroups.appendMemberToGroup(groupModelObj, group, groupDiv);
					});

				} else {

					// member is a user, no danger
					let userModelObj = StructrModel.createFromData(member);
					_UsersAndGroups.appendMemberToGroup(userModelObj, group, groupDiv);
				}

			} else {
				let memberModelObj = StructrModel.createFromData(member);
				_UsersAndGroups.appendMemberToGroup(memberModelObj, group, groupDiv);
			}
		}
	},
	appendMemberToGroup: function (member, group, groupEl) {

		let groupId    = group.id;
		let isExpanded = Structr.isExpanded(groupId);

		_Entities.appendExpandIcon(groupEl, group, true, isExpanded, (members) => { _UsersAndGroups.appendMembersToGroup(members, group, groupEl); } );

		if (!isExpanded) {
			return;
		}

		let prefix = (member.isUser) ? '.userid_' : '.groupid_';

		if (member.isUser) {

			groupEl.each(function (idx, grpEl) {

				let memberAlreadyListed = $(prefix + member.id, grpEl).length > 0;
				if (!memberAlreadyListed) {

					let modelObj = StructrModel.obj(member.id);
					let userDiv = _UsersAndGroups.createUserElement(modelObj ? modelObj : member);

					$(grpEl).append(userDiv);
					userDiv.removeClass('disabled');

					_Entities.appendContextMenuIcon(userDiv, member);
					_Elements.enableContextMenuOnElement(userDiv, member);
					_UsersAndGroups.setMouseOver(userDiv, member.id, prefix);
				}
			});

		} else {

			let alreadyShownInParents = _UsersAndGroups.isGroupAlreadyShown(member, groupEl);
			let alreadyShownInMembers = $(prefix + member.id, groupEl).length > 0;

			if (!alreadyShownInMembers && !alreadyShownInParents) {

				let groupDiv = _UsersAndGroups.createGroupElement(member);

				groupEl.append(groupDiv);

				groupDiv.removeClass('disabled');

				_Entities.appendContextMenuIcon(groupDiv, member);
				_Elements.enableContextMenuOnElement(groupDiv, member);
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
	refreshGroups: async () => {

		let types = await _Schema.getDerivedTypes('org.structr.dynamic.Group', []);

		Structr.fetchHtmlTemplate('security/button.group.new', { types: types }, function (html) {

			_Security.groupList.innerHTML     = '';
			_Security.groupControls.innerHTML = html;

			let groupTypeSelect = document.querySelector('select#group-type');
			let addGroupButton  = document.getElementById('add-group-button');

			addGroupButton.addEventListener('click', (e) => {
				Command.create({ type: groupTypeSelect.value });
			});

			groupTypeSelect.addEventListener('change', () => {
				addGroupButton.querySelector('span').textContent = 'Add ' + groupTypeSelect.value;
			});

			let groupPager = _Pager.addPager('groups', $(_Security.groupControls), true, 'Group', 'public', (groups) => {
				for (let group of groups) {
					let groupModelObj = StructrModel.create(group);
					_UsersAndGroups.appendGroupElement($(_Security.groupList), groupModelObj);
				}
			});
			groupPager.cleanupFunction = function () {
				_Security.groupList.innerHTML = '';
			};
			groupPager.pager.append('<div>Filter: <input type="text" class="filter" data-attribute="name"></div>');
			groupPager.activateFilterElements();
		});
	},
	createGroupElement: function (group) {

		let displayName = ((group.name) ? group.name : '[unnamed]');
		let groupElement = $('<div class="node group groupid_' + group.id + '">'
				+ '<i class="typeIcon ' + _UsersAndGroups.getIconForPrincipal(group) + '"></i>'
				+ '<b title="' + displayName + '" class="name_  abbr-ellipsis abbr-75pc" data-input-class="max-w-75">' + displayName + '</b>'
				+ '</div>'
		);
		groupElement.data('groupId', group.id);

		groupElement.droppable({
			accept: '.user, .group',
			greedy: true,
			hoverClass: 'nodeHover',
			tolerance: 'pointer',
			drop: function(event, ui) {
				let nodeId;

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
	updateGroupElementAfterModelChange: (group) => {

		requestAnimationFrame(() => {

			// request animation frame is especially important if a name is completely removed!

			for (let userEl of document.querySelectorAll('.groupid_' + group.id)) {

				let icon = userEl.querySelector('.typeIcon');
				if (icon) {
					icon.setAttribute('class', 'typeIcon ' + _UsersAndGroups.getIconForPrincipal(group));
				}

				let groupName = userEl.querySelector('.name_');
				if (groupName) {
					let displayName = ((group.name) ? group.name : '[unnamed]');

					groupName.setAttribute('title', displayName);
					groupName.textContent = displayName;
				}
			}
		});
	},
	appendGroupElement: function(element, group) {

		let hasChildren = group.members && group.members.length;
		let groupDiv    = _UsersAndGroups.createGroupElement(group);
		element.append(groupDiv);

		_Entities.appendExpandIcon(groupDiv, group, hasChildren, Structr.isExpanded(group.id), (members) => { _UsersAndGroups.appendMembersToGroup(members, group, groupDiv); } );
		_Entities.appendContextMenuIcon(groupDiv, group);
		_Elements.enableContextMenuOnElement(groupDiv, group);
		_UsersAndGroups.setMouseOver(groupDiv, group.id, '.groupid_');

		if (hasChildren && Structr.isExpanded(group.id)) {
			// do not directly use group.members (it does not contain all necessary information)
			Command.children(group.id, (members) => { _UsersAndGroups.appendMembersToGroup(members, group, groupDiv); });
		}

		return groupDiv;
	},
	setMouseOver: function (node, id, prefix) {

		node.children('b.name_').off('click').on('click', function(e) {
			e.stopPropagation();
			_Entities.makeAttributeEditable(node, id, 'b.name_', 'name', (el) => {
				blinkGreen(el);
			});
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
		for (let el of document.querySelectorAll(prefix + id)) {
			el.classList.add('nodeHover');
		}
	},
	deactivateNodeHover: function (id, prefix) {
		for (let el of document.querySelectorAll(prefix + id)) {
			el.classList.remove('nodeHover');
		}
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

let _ResourceAccessGrants = {

	refreshResourceAccesses: function() {

		if (Structr.isInMemoryDatabase === undefined) {
			window.setTimeout(() => {
				_ResourceAccessGrants.refreshResourceAccesses();
			}, 500);
		}

		let pagerTransportFunction = Structr.isInMemoryDatabase ? null : _ResourceAccessGrants.customPagerTransportFunction;
		let pagerCallbackFunction  = Structr.isInMemoryDatabase ? _ResourceAccessGrants.customInMemoryCallbackFunction : null;

		_Security.resourceAccesses.empty();

		Structr.fetchHtmlTemplate('security/resource-access', { showVisibilityFlags: UISettings.getValueForSetting(UISettings.security.settings.showVisibilityFlagsInGrantsTableKey) }, function (html) {

			_Security.resourceAccesses.append(html);

			Structr.activateCommentsInElement(_Security.resourceAccesses);

			let raPager = _Pager.addPager('resource-access', $('#resourceAccessesPager', _Security.resourceAccesses), true, 'ResourceAccess', undefined, undefined, pagerTransportFunction, 'id,flags,name,type,signature,isResourceAccess,visibleToPublicUsers,visibleToAuthenticatedUsers,grantees');

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

		let fetchAllGranteesCallback = (result, count) => {

			let fetchPromises = [];

			for (let r of result) {

				fetchPromises.push(new Promise((resolve, reject) => {
					Command.get(r.id, 'grantees', (grant) => {
						r.grantees = grant.grantees;
						resolve();
					})
				}));
			}

			Promise.all(fetchPromises).then(() => {
				callback(result, count);
			});
		}

		Command.cypher('MATCH (n:ResourceAccess) ' + filterString + ' RETURN DISTINCT n ORDER BY n.' + _Pager.sortKey[type] + ' ' + _Pager.sortOrder[type], undefined, fetchAllGranteesCallback, pageSize, page);
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
			_ResourceAccessGrants.createResourceAccessGrant(sig, 0, function() {
				reEnableInput();
				inp.val('');
			});
		} else {
			reEnableInput();
			blinkRed(inp);
		}
		window.setTimeout(reEnableInput, 250);
	},
	createResourceAccessGrant: function(signature, flags, callback, additionalData) {
		let grantData = {
			type: 'ResourceAccess',
			signature: signature,
			flags: flags
		};

		if (additionalData) {
			grantData = Object.assign(grantData, additionalData);
		}

		Command.create(grantData, callback);
	},
	deleteResourceAccess: function(button, resourceAccess) {
		_Entities.deleteNode(button, resourceAccess);
	},
	getVerbFromKey: function(key = '') {
		return key.substring(key.lastIndexOf('_')+1, key.length);
	},
	mask: {
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
	},
	appendResourceAccessElement: function(resourceAccess, blinkAfterUpdate = true) {

		if (!_Security.resourceAccesses || !_Security.resourceAccesses.is(':visible')) {
			return;
		}

		let flags = parseInt(resourceAccess.flags);

		let trHtml = '<tr id="id_' + resourceAccess.id + '" class="resourceAccess"><td class="title-cell"><b class="name_">' + resourceAccess.signature + '</b></td>';

		let noAuthAccessPossible = resourceAccess.visibleToAuthenticatedUsers === false && (!resourceAccess.grantees || resourceAccess.grantees.length === 0);

		let flagWarningTexts = {};
		let flagSanityInfos  = {};

		let hasAuthFlag    = false;
		let hasNonAuthFlag = false;

		for (let key in _ResourceAccessGrants.mask) {

			let flagIsSet = (flags & _ResourceAccessGrants.mask[key]);

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

			trHtml += '<td class="' + additionalClasses.join(' ') + '"><input type="checkbox" ' + (flagIsSet ? 'checked="checked"' : '') + (isDisabled ? ' disabled' : '') + ' data-flag="' + _ResourceAccessGrants.mask[key] + '" class="resource-access-flag" data-key="' + key +'"></td>';
		}

		trHtml += '<td><input type="text" class="bitmask" size="4" value="' + flags + '"></td>';

		let showVisibilityFlagsInGrantsTable = UISettings.getValueForSetting(UISettings.security.settings.showVisibilityFlagsInGrantsTableKey);

		if (showVisibilityFlagsInGrantsTable) {
			trHtml += '<td class="bl-1"><input type="checkbox" ' + (resourceAccess.visibleToAuthenticatedUsers ? 'checked="checked"' : '') + ' name="visibleToAuthenticatedUsers" class="resource-access-visibility"></td>';
			trHtml += '<td class="br-1"><input type="checkbox" ' + (resourceAccess.visibleToPublicUsers ? 'checked="checked"' : '') + ' name="visibleToPublicUsers" class="resource-access-visibility"></td>';
		}
		trHtml += '<td><i class="acl-resource-access button ' + _Icons.getFullSpriteClass(_Icons.key_icon) + '" /> ' +
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
					customToggleIcon: _Icons.exclamation_icon,
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
			tr.find('input.resource-access-flag:checked').each(function(i, input) {
				newFlags += parseInt($(input).attr('data-flag'));
			});
			_ResourceAccessGrants.updateResourceAccessFlags(resourceAccess.id, newFlags);
		});

		$('input[type=checkbox].resource-access-visibility', tr).on('change', function() {

			let visibilityOptionName  = $(this).attr('name');
			let visibilityOptionValue = $(this).prop('checked');

			Command.setProperty(resourceAccess.id, visibilityOptionName, visibilityOptionValue, false, function() {
				_ResourceAccessGrants.updateResourcesAccessRow(resourceAccess.id);
			});
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