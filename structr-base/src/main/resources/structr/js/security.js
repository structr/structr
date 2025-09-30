/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
document.addEventListener("DOMContentLoaded", () => {
	Structr.registerModule(_Security);
	Structr.classes.push('user');
	Structr.classes.push('group');
	Structr.classes.push('resourceAccess');
	Structr.classes.push('corsSetting');
});

let _Security = {
	_moduleName: 'security',
	usersPagerId: 'users',
	groupsPagerId: 'groups',
	resourceAccessesPagerId: 'resource-access',
	corsSettingsPagerId: 'cors-settings',
	securityTabKey: 'structrSecurityTab_' + location.port,
	init: () => {
		_Pager.initPager(_Security.usersPagerId,            'User', 1, 25, 'name', 'asc');
		_Pager.initPager(_Security.groupsPagerId,           'Group', 1, 25, 'name', 'asc');
		_Pager.initPager(_Security.resourceAccessesPagerId, 'ResourceAccess', 1, 25, 'signature', 'asc');
		_Pager.initPager(_Security.corsSettingsPagerId,     'CorsSetting', 1, 25, 'requestUri', 'asc');
	},
	onload: () => {

		_Security.init();

		Structr.updateMainHelpLink(_Helpers.getDocumentationURLForTopic('security'));

		Structr.setMainContainerHTML(_Security.templates.main());
		Structr.setFunctionBarHTML(_Security.templates.functions());

		UISettings.showSettingsForCurrentModule();

		let subModule = LSWrapper.getItem(_Security.securityTabKey) || 'users-and-groups';

		for (let tabLink of document.querySelectorAll('#function-bar .tabs-menu li a')) {

			tabLink.addEventListener('click', (e) => {
				e.preventDefault();

				let urlHash = e.target.closest('a').getAttribute('href');
				let subModule = urlHash.split(':')[1];
				window.location.hash = urlHash;

				_Security.selectTab(subModule);
			});

			if (tabLink.closest('a').getAttribute('href') === '#security:' + subModule) {
				tabLink.click();
			}
		}

		Structr.mainMenu.unblock(100);
	},
	getContextMenuElements: (div, entity) => {

		const isUser  = entity.isUser;
		const isGroup = entity.isGroup;
		let elements  = [];

		if (isUser || isGroup) {

			let userOrGroupEl = $(div).closest('.node');
			let parentGroupEl = userOrGroupEl.parent().closest('.group');
			if (parentGroupEl.length) {
				let parentGroupId = _UsersAndGroups.getGroupId(parentGroupEl);
				elements.push({
					name: `Remove ${entity.name} from ${$('.name_', parentGroupEl).attr('title')}`,
					clickHandler: () => {

						Command.removeFromCollection(parentGroupId, 'members', entity.id, async () => {
							await _UsersAndGroups.refreshGroups();
						});
					}
				});
			}

			_Elements.contextMenu.appendContextMenuSeparator(elements);
		}

		if (isUser) {
			elements.push({
				name: 'General',
				clickHandler: () => {
					_Entities.showProperties(entity, 'general');
				}
			});
		}

		elements.push({
			name: 'Advanced',
			clickHandler: () => {
				_Entities.showProperties(entity, 'ui');
			}
		});

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		_Elements.contextMenu.appendSecurityContextMenuItems(elements, entity);

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		elements.push({
			icon: _Icons.getMenuSvgIcon(_Icons.iconTrashcan),
			classes: ['menu-bolder', 'danger'],
			name: `Delete ${entity.type}`,
			clickHandler: () => {
				_Entities.deleteNode(entity);
			}
		});

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		return elements;
	},
	selectTab: (subModule) => {

		LSWrapper.setItem(_Security.securityTabKey, subModule);

		for (let tab of document.querySelectorAll('#function-bar .tabs-menu li')) {
			tab.classList.remove('active');
		}

		let tabLink = document.querySelector('#function-bar .tabs-menu li a[href="#security:' + subModule + '"]');
		if (tabLink) tabLink.closest('li').classList.add('active');

		if (subModule === 'users-and-groups') {

			$('#usersAndGroups').show();
			$('#resourceAccess').hide();
			$('#corsSettings').hide();
			_UsersAndGroups.refreshUsers();
			_UsersAndGroups.refreshGroups();

		} else if (subModule === 'resource-access') {

			$('#resourceAccess').show();
			$('#usersAndGroups').hide();
			$('#corsSettings').hide();
			_ResourceAccessPermissions.refreshResourceAccesses();

		} else if (subModule === 'cors-settings') {

			$('#corsSettings').show();
			$('#resourceAccess').hide();
			$('#usersAndGroups').hide();
			_CorsSettings.refreshCorsSettings();
		}
	},

	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/security.css">

			<div class="main-app-box" id="security">

				<div id="securityTabs" class="tabs-contents">

					<div id="usersAndGroups" class="tab-content">

						<div id="usersAndGroups-inner">
							<div id="users">
								<div id="users-controls"></div>
								<div id="users-list"></div>
							</div>

							<div id="groups">
								<div id="groups-controls"></div>
								<div id="groups-list"></div>
							</div>
						</div>

					</div>

					<div id="resourceAccess" class="tab-content">
						<div id="resourceAccesses"></div>
					</div>

					<div id="corsSettings" class="tab-content">
						<div id="corsSettings-inner"></div>
					</div>
				</div>
			</div>
		`,
		functions: config => `
			<ul id="securityTabsMenu" class="tabs-menu flex-grow">
			  <li><a href="#security:users-and-groups" id="usersAndGroups_"><span>Users and Groups</span></a></li>
			  <li><a href="#security:resource-access" id="resourceAccess_"><span>Resource Permissions</span></a></li>
			  <li><a href="#security:cors-settings" id="corsSettings_"><span>CORS Settings</span></a></li>
			</ul>
		`,
		newGroupButton: config => `
			<div class="flex items-center mb-8">

				<select class="select-create-type hover:bg-gray-100 focus:border-gray-666 active:border-green combined-select-create" id="group-type">
					<option value="Group">Group</option>
					${config.types.map(type => `<option value="${type}">${type}</option>`).join('')}
				</select>

				<button class="action add_group_icon inline-flex items-center combined-select-create" id="add-group-button">
					${_Icons.getSvgIcon(_Icons.iconGroupAdd, 16, 16, 'mr-2')}
					<span>Create</span>
				</button>
			</div>
		`,
		newUserButton: config => `
			<div class="flex items-center mb-8">

				<select class="select-create-type hover:bg-gray-100 focus:border-gray-666 active:border-green combined-select-create" id="user-type">
					<option value="User">User</option>
					${config.types.map(type => `<option value="${type}">${type}</option>`).join('')}
				</select>

				<button class="action add_user_icon inline-flex items-center combined-select-create" id="add-user-button">
					${_Icons.getSvgIcon(_Icons.iconUserAdd, 16, 16, 'mr-2')}
					<span>Create</span>
				</button>
			</div>
		`,
		resourceAccess: config => `
			<div class="flex items-center">
				<div id="add-resource-access-permission" class="flex items-center">
					<input type="text" size="20" id="resource-signature" placeholder="Signature" class="combined-input-create">
					<button class="action add_permission_icon button inline-flex items-center combined-input-create">
						${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['mr-2'])} Create Permission
					</button>
				</div>

				<div id="filter-resource-access-permissions" class="flex items-center">
					<input type="text" class="filter" data-attribute="signature" placeholder="Filter/Search...">
				</div>
			</div>

			<table class="security-module-table" id="resourceAccessesTable">
				<thead>
					<tr>
						<th><div id="resourceAccessesPager"></div></th>
						<th colspan="7" class="center" data-comment="These flags determine the access permissions for authenticated users to the <b>resource</b> described by the signature (<b>not the data</b> behind that resource).<br><br>If a user does not have read rights to the permission object, access to the resource described by the signature is denied. If the user has <b>multiple</b> permissions for the same signature, the flags for these are combined.">Authenticated users</th>
						<th colspan="7" class="center" data-comment="These flags determine the access permissions for public (non-authenticated) users to the <b>resource</b> described by the signature (<b>not the data</b> behind that resource).">Non-authenticated (public) users</th>
						${config.showBitmask         ? '<th></th>' : '' }
						${config.showVisibilityFlags ? '<th colspan="2">Visibility</th>' : '' }
						<th></th>
					</tr>
					<tr>
						<th class="title-cell">Signature</th>
						<th>GET</th>
						<th>PUT</th>
						<th>POST</th>
						<th>DELETE</th>
						<th>OPTIONS</th>
						<th>HEAD</th>
						<th class="pr-12">PATCH</th>
						<th>GET</th>
						<th>PUT</th>
						<th>POST</th>
						<th>DELETE</th>
						<th>OPTIONS</th>
						<th>HEAD</th>
						<th>PATCH</th>
						${config.showBitmask         ? '<th class="pr-8">Bitmask</th>' : '' }
						${config.showVisibilityFlags ? '<th>' + Structr.abbreviations['visibleToAuthenticatedUsers'] +'</th>' : '' }
						${config.showVisibilityFlags ? '<th>' + Structr.abbreviations['visibleToPublicUsers'] +'</th>' : '' }
						<th></th>
					</tr>
				</thead>
				<tbody></tbody>
			</table>
		`,
		corsSettings: config => `
			<div class="flex items-center">
				<div id="add-cors-setting" class="flex items-center">
					<input type="text" size="20" id="cors-setting-request-uri" placeholder="Request URI Path" class="combined-input-create">
					<button class="action add_cors_setting_button button inline-flex items-center combined-input-create">
						${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['mr-2'])} Create CORS Setting
					</button>
				</div>

				<div id="filter-cors-settings" class="flex items-center">
					<input type="text" class="filter" data-attribute="requestUri" placeholder="Filter/Search...">
				</div>
			</div>

			<table class="security-module-table" id="corsSettingsTable">
				<thead>
					<tr>
						<th><div id="corsSettingsPager"></div></th>
						<th></th>
						<th><label data-comment="Comma-separated list of accepted origins, sets the <code>Access-Control-Allow-Origin</code> header.">Accepted Origins</label></th>
						<th><label data-comment="Sets the value of the <code>Access-Control-Max-Age</code> header. Unit is seconds.">Max. Age</label></th>
						<th><label data-comment="Sets the value of the <code>Access-Control-Allow-Methods</code> header. Comma-delimited list of the allowed HTTP request methods.">Allow Methods</label></th>
						<th><label data-comment="Sets the value of the <code>Access-Control-Allow-Headers</code> header.">Allow Headers</label></th>
						<th><label data-comment="Sets the value of the <code>Access-Control-Allow-Credentials</code> header.">Allow Credentials</label></th>
						<th><label data-comment="Sets the value of the <code>Access-Control-Expose-Headers</code> header.">Expose Headers</label></th>
					</tr>
				</thead>
				<tbody></tbody>
			</table>
		`
	}
};

let _UsersAndGroups = {

	userNodeClassPrefix:  'userid_',
	groupNodeClassPrefix: 'groupid_',

	getUserId: (element) => {
		return element.data('userId');
	},
	getGroupId: (element) => {
		return element.data('groupId');
	},

	getUsersListElement: () => document.getElementById('users-list'),
	getGroupsListElement: () => document.getElementById('groups-list'),

	refreshUsers: async () => {

		let types = await _Schema.getDerivedTypeNames('User', []);

		_Helpers.fastRemoveAllChildren(_UsersAndGroups.getUsersListElement());

		let userControls = document.getElementById('users-controls');
		let newUserHtml  = _Security.templates.newUserButton({ types: types });
		_Helpers.fastRemoveAllChildren(userControls);
		userControls.insertAdjacentHTML('beforeend', newUserHtml);

		let userTypeSelect = document.querySelector('select#user-type');
		let addUserButton  = document.getElementById('add-user-button');

		addUserButton.addEventListener('click', async e => {

			let nodeData = {
				type: userTypeSelect.value,
				name: _Helpers.createRandomName(userTypeSelect.value)
			};

			_Crud.creationDialogWithErrorHandling.initializeForEvent(e, nodeData.type, nodeData, (type, newNodeId) => {

				Command.get(newNodeId, null, userData => {
					let userModelObj = StructrModel.create(userData);
					_UsersAndGroups.appendUserToElement(_UsersAndGroups.getUsersListElement(), userModelObj);
				});
			});
		});

		userTypeSelect.addEventListener('change', () => {
			addUserButton.querySelector('span').textContent = `Add ${userTypeSelect.value}`;
		});

		let userPager = _Pager.addPager(_Security.usersPagerId, userControls, true, 'User', 'public', (users) => {
			for (let user of users) {
				let userModelObj = StructrModel.create(user);
				_UsersAndGroups.appendUserToElement(_UsersAndGroups.getUsersListElement(), userModelObj);
			}
		}, null, 'id,isUser,name,eMail,type,isAdmin,blocked', true);

		userPager.cleanupFunction = () => {
			_Helpers.fastRemoveAllChildren(_UsersAndGroups.getUsersListElement());
		};

		userPager.appendFilterElements(`
			<span class="ml-4 mr-1">Filter:</span>
			<input type="text" class="filter" data-attribute="name">
		`);
		userPager.activateFilterElements();
		userPager.setIsPaused(false);
		userPager.refresh();
	},
	createUserElement: (user) => {

		let userElement = $(`
			<div class="node user ${_UsersAndGroups.userNodeClassPrefix}${user.id}" data-user-id="${user.id}">
				<div class="node-container flex items-center overflow-hidden">
					${_Icons.getIconForPrincipal(user)}<b class="name_ flex-grow truncate" data-input-class="flex-grow"></b>
					<div class="icons-container flex items-center"></div>
				</div>
			</div>
		`);

		let nameElement = userElement[0].querySelector('.name_');

		let displayName         = ((user.name) ? user.name : ((user.eMail) ? `[${user.eMail}]` : '[unnamed]'));
		nameElement.textContent = displayName;
		nameElement.title       = displayName;

		_UsersAndGroups.makeDraggable(userElement);

		return userElement;
	},
	prevAnimFrameReqId_updateUserElementAfterModelChange: undefined,
	updateUserElementAfterModelChange: (user) => {

		_Helpers.requestAnimationFrameWrapper(_UsersAndGroups.prevAnimFrameReqId_updateUserElementAfterModelChange, () => {

			// request animation frame is especially important if a name is completely removed!

			for (let userEl of document.querySelectorAll('.' + _UsersAndGroups.userNodeClassPrefix + user.id)) {

				let icon = userEl.querySelector('svg.typeIcon');
				if (icon) {
					_Icons.replaceSvgElementWithRawSvg(icon, _Icons.getIconForPrincipal(user));
				}

				let userName = userEl.querySelector('.name_');
				if (userName) {

					let displayName      = ((user.name) ? user.name : ((user.eMail) ? `[${user.eMail}]` : '[unnamed]'));
					userName.title       = displayName;
					userName.textContent = displayName;
				}
			}
		});
	},
	appendUserToElement: (targetElement, user) => {

		let userDiv = _UsersAndGroups.createUserElement(user);

		targetElement.appendChild(userDiv[0]);

		_Entities.appendContextMenuIcon(userDiv[0].querySelector('.icons-container'), user);
		_Elements.contextMenu.enableContextMenuOnElement(userDiv[0], user);
		_UsersAndGroups.setMouseOver(userDiv, user.id, '.' + _UsersAndGroups.userNodeClassPrefix);

		let dblclickHandler = (e) => {
			_Entities.showProperties(user, LSWrapper.getItem(_Entities.activeEditTabPrefix  + '_' + user.id) || 'general');
		};

		if (userDiv) {
			let node = userDiv[0].closest('.node');
			node.removeEventListener('dblclick', dblclickHandler);
			node.addEventListener('dblclick', dblclickHandler);
		}
	},
	appendMembersToGroup: (members, group, groupDiv) => {

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
	appendMemberToGroup: (member, group, groupEl) => {

		let groupId    = group.id;
		let isExpanded = Structr.isExpanded(groupId);

		_Entities.appendExpandIcon(groupEl.children('.node-container'), group, true, isExpanded, (members) => { _UsersAndGroups.appendMembersToGroup(members, group, groupEl); } );

		if (!isExpanded) {
			return;
		}

		let prefix = '.' + (member.isUser) ? _UsersAndGroups.userNodeClassPrefix : _UsersAndGroups.groupNodeClassPrefix;

		if (member.isUser) {

			groupEl.each(function (idx, grpEl) {

				let memberAlreadyListed = $(prefix + member.id, grpEl).length > 0;
				if (!memberAlreadyListed) {

					let modelObj = StructrModel.obj(member.id);

					_UsersAndGroups.appendUserToElement(grpEl, (modelObj ? modelObj : member));
				}
			});

		} else {

			let alreadyShownInParents = _UsersAndGroups.isGroupAlreadyShown(member, groupEl);
			let alreadyShownInMembers = $(prefix + member.id, groupEl).length > 0;

			if (!alreadyShownInMembers && !alreadyShownInParents) {

				_UsersAndGroups.appendGroupToElement(groupEl, member);
			}
		}
	},
	isGroupAlreadyShown: (group, groupEl) => {

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

		let types = await _Schema.getDerivedTypeNames('Group', []);

		_Helpers.fastRemoveAllChildren(_UsersAndGroups.getGroupsListElement());

		let groupControls = document.getElementById('groups-controls');
		let newGroupHtml  = _Security.templates.newGroupButton({ types: types });
		_Helpers.fastRemoveAllChildren(groupControls);
		groupControls.insertAdjacentHTML('beforeend', newGroupHtml);

		let groupTypeSelect = document.querySelector('select#group-type');
		let addGroupButton  = document.getElementById('add-group-button');

		addGroupButton.addEventListener('click', (e) => {

			let nodeData = {
				type: groupTypeSelect.value,
				name: _Helpers.createRandomName(groupTypeSelect.value)
			};

			_Crud.creationDialogWithErrorHandling.initializeForEvent(e, nodeData.type, nodeData, (type, newNodeId) => {

				Command.get(newNodeId, null, groupData => {
					let groupModelObj = StructrModel.create(groupData);
					_UsersAndGroups.appendGroupToElement($(_UsersAndGroups.getGroupsListElement()), groupModelObj);
				});
			});
		});

		groupTypeSelect.addEventListener('change', () => {
			addGroupButton.querySelector('span').textContent = 'Add ' + groupTypeSelect.value;
		});

		_Pager.forceAddFilters(_Security.groupsPagerId, 'Group', {
			groups: UISettings.getValueForSetting(UISettings.settingGroups.security.settings.showGroupsHierarchicallyKey) ? [] : ''
		});

		let groupPager = _Pager.addPager(_Security.groupsPagerId, groupControls, true, 'Group', 'public', (groups) => {
			for (let group of groups) {
				let groupModelObj = StructrModel.create(group);
				_UsersAndGroups.appendGroupToElement($(_UsersAndGroups.getGroupsListElement()), groupModelObj);
			}
		}, undefined, 'id,isGroup,name,type,members,blocked,isAdmin', true);

		groupPager.cleanupFunction = () => {
			_Helpers.fastRemoveAllChildren(_UsersAndGroups.getGroupsListElement());
		};
		groupPager.appendFilterElements(`
			<span class="ml-4 mr-1">Filter:</span>
			<input type="text" class="filter" data-attribute="name">
		`);
		groupPager.activateFilterElements();
		groupPager.setIsPaused(false);
		groupPager.refresh();
	},
	createGroupElement: (group) => {

		let groupElement = $(`
			<div class="node group ${_UsersAndGroups.groupNodeClassPrefix}${group.id}" data-group-id="${group.id}">
				<div class="node-container flex items-center">
					${_Icons.getIconForPrincipal(group)}
					<b class="name_ flex-grow" data-input-class="flex-grow"></b>
					<div class="icons-container flex items-center"></div>
				</div>
			</div>
		`);

		let nameElement = groupElement[0].querySelector('.name_');

		let displayName         = group?.name ?? '[unnamed]';
		nameElement.title       = displayName;
		nameElement.textContent = displayName;

		groupElement.droppable({
			accept: '.user, .group',
			greedy: true,
			hoverClass: 'nodeHover',
			tolerance: 'pointer',
			drop: function(event, ui) {
				let nodeId;

				if (ui.draggable.hasClass('user')) {
					nodeId = _UsersAndGroups.getUserId(ui.draggable);
				} else if (ui.draggable.hasClass('group')) {
					nodeId = _UsersAndGroups.getGroupId(ui.draggable);
				}

				if (nodeId) {

					if (nodeId !== group.id) {

						Command.appendMember(nodeId, group.id, (group) => {

							let groupModelObj = StructrModel.obj(group.id);
							let userModelObj  = StructrModel.obj(nodeId);

							_Helpers.blinkGreen($('.' + _UsersAndGroups.groupNodeClassPrefix + group.id));

							_UsersAndGroups.appendMemberToGroup(userModelObj, groupModelObj, $(groupElement));
						});

					} else {

						new WarningMessage().title("Warning").text("Prevented adding group as a member of itself").show();
					}
				}
			}
		});

		_UsersAndGroups.makeDraggable(groupElement);

		return groupElement;
	},
	prevAnimFrameReqId_updateGroupElementAfterModelChange: undefined,
	updateGroupElementAfterModelChange: (group) => {

		_Helpers.requestAnimationFrameWrapper(_UsersAndGroups.prevAnimFrameReqId_updateGroupElementAfterModelChange, () => {

			// request animation frame is especially important if a name is completely removed!

			for (let groupEl of document.querySelectorAll(`.${_UsersAndGroups.groupNodeClassPrefix}${group.id}`)) {

				let icon = groupEl.querySelector('svg.typeIcon');
				if (icon) {
					let newIcon = _Icons.replaceSvgElementWithRawSvg(icon, _Icons.getIconForPrincipal(group));

					if (icon.classList.contains(_Icons.typeIconClassNoChildren)) {
						newIcon.classList.add(_Icons.typeIconClassNoChildren);
					}
				}

				let groupName = groupEl.querySelector('.name_');
				if (groupName) {
					let displayName = ((group.name) ? group.name : '[unnamed]');

					groupName.setAttribute('title', displayName);
					groupName.textContent = displayName;
				}
			}
		});
	},
	appendGroupToElement: (targetElement, group) => {

		let hasChildren = group.members && group.members.length;
		let groupDiv    = _UsersAndGroups.createGroupElement(group);
		targetElement.append(groupDiv);

		let appendMembersFn = (members) => {
			_UsersAndGroups.appendMembersToGroup(members, group, groupDiv);
		};

		_Entities.appendExpandIcon(groupDiv.children('.node-container'), group, hasChildren, Structr.isExpanded(group.id), appendMembersFn);

		_Entities.appendContextMenuIcon(groupDiv.children('.node-container').children('.icons-container')[0], group);
		_Elements.contextMenu.enableContextMenuOnElement(groupDiv[0], group);
		_UsersAndGroups.setMouseOver(groupDiv, group.id, '.' + _UsersAndGroups.groupNodeClassPrefix);

		if (hasChildren && Structr.isExpanded(group.id)) {
			// do not directly use group.members (it does not contain all necessary information)
			Command.children(group.id, appendMembersFn);
		}

		let dblclickHandler = (e) => {
			_Entities.showProperties(group);
		};

		if (groupDiv) {
			let node = groupDiv[0].closest('.node');
			node.removeEventListener('dblclick', dblclickHandler);
			node.addEventListener('dblclick', dblclickHandler);
		}

		return groupDiv;
	},
	setMouseOver: (node, id, prefix) => {

		node[0].querySelector(':scope .node-container b.name_')?.addEventListener('click', (e) => {
			e.stopPropagation();
			_Entities.makeAttributeEditable(node, id, 'b.name_', 'name', (el) => {
				_Helpers.blinkGreen(el);
			});
		});

		node[0].addEventListener('mouseover', (e) => {
			e.stopPropagation();
			_UsersAndGroups.activateNodeHover(id, prefix);
		});

		node[0].addEventListener('mouseout', (e) => {
			e.stopPropagation();
			_UsersAndGroups.deactivateNodeHover(id, prefix);
		});
	},
	activateNodeHover: (id, prefix) => {
		for (let el of document.querySelectorAll(prefix + id)) {
			el.classList.add('nodeHover');
		}
	},
	deactivateNodeHover: (id, prefix) => {
		for (let el of document.querySelectorAll(prefix + id)) {
			el.classList.remove('nodeHover');
		}
	},
	makeDraggable: (el) => {
		el.draggable({
			revert: 'invalid',
			helper: 'clone',
			stack: '.node',
			appendTo: '#main',
			zIndex: 99
		});
	}
};

let _ResourceAccessPermissions = {

	defaultResourceAccessPermissionAttributes: 'id,flags,type,signature,visibleToPublicUsers,visibleToAuthenticatedUsers,grantees,isResourceAccess,name',
	getResourceAccessesElement: () => document.querySelector('#resourceAccesses'),

	refreshResourceAccesses: () => {

		if (Structr.isInMemoryDatabase === undefined) {
			// this is loaded from the env resource, thus reload after a bit
			window.setTimeout(() => {
				_ResourceAccessPermissions.refreshResourceAccesses();
			}, 500);

			return;
		}

		_Helpers.fastRemoveAllChildren(_ResourceAccessPermissions.getResourceAccessesElement());

		let resourceAccessHtml = _Security.templates.resourceAccess({
			showVisibilityFlags: UISettings.getValueForSetting(UISettings.settingGroups.security.settings.showVisibilityFlagsInPermissionsTableKey),
			showBitmask:         UISettings.getValueForSetting(UISettings.settingGroups.security.settings.showBitmaskColumnInPermissionsTableKey)
		});
		_ResourceAccessPermissions.getResourceAccessesElement().insertAdjacentHTML('beforeend', resourceAccessHtml);

		_Helpers.activateCommentsInElement(_ResourceAccessPermissions.getResourceAccessesElement());

		let raPager = _Pager.addPager(_Security.resourceAccessesPagerId, _ResourceAccessPermissions.getResourceAccessesElement().querySelector('#resourceAccessesPager'), true, 'ResourceAccess', undefined, undefined, null, 'id,flags,name,type,signature,isResourceAccess,visibleToPublicUsers,visibleToAuthenticatedUsers,grantees', true);

		raPager.cleanupFunction = () => {
			_Helpers.fastRemoveAllChildren(document.querySelector('#resourceAccessesTable tbody'));
		};

		raPager.activateFilterElements(_ResourceAccessPermissions.getResourceAccessesElement());
		raPager.setIsPaused(false);
		raPager.refresh();

		_ResourceAccessPermissions.getResourceAccessesElement().querySelector('.add_permission_icon')?.addEventListener('click', _ResourceAccessPermissions.addResourcePermission);
		_ResourceAccessPermissions.getResourceAccessesElement().querySelector('#resource-signature')?.addEventListener('keyup', (e) => {
			if (e.keyCode === 13) {
				_ResourceAccessPermissions.addResourcePermission();
			}
		});
	},
	addResourcePermission: () => {

		let inp = $('#resource-signature');
		inp.attr('disabled', 'disabled').addClass('disabled').addClass('read-only');
		$('.add_permission_icon', $(_ResourceAccessPermissions.getResourceAccessesElement())).attr('disabled', 'disabled').addClass('disabled').addClass('read-only');

		let reEnableInput = () => {
			$('.add_permission_icon', $(_ResourceAccessPermissions.getResourceAccessesElement())).attr('disabled', null).removeClass('disabled').removeClass('read-only');
			inp.attr('disabled', null).removeClass('disabled').removeClass('readonly');
		};

		let sig = inp.val();
		if (sig) {
			_ResourceAccessPermissions.createResourceAccessPermission(sig, 0, () => {
				reEnableInput();
				inp.val('');
			});
		} else {
			reEnableInput();
			_Helpers.blinkRed(inp);
		}
		window.setTimeout(reEnableInput, 250);
	},
	createResourceAccessPermission: (signature, flags, callback, additionalData) => {

		let permissionData = {
			type: 'ResourceAccess',
			signature: signature,
			flags: flags,
			// no grantees because otherwise admin is a grantee and this messes with the error messages
			grantees: []
		};

		if (additionalData) {
			permissionData = Object.assign(permissionData, additionalData);
		}

		Command.create(permissionData, callback);
	},
	getVerbFromKey: (key = '') => {
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
	appendResourceAccessElement: (resourceAccess, blinkAfterUpdate = true) => {

		if (!_ResourceAccessPermissions.getResourceAccessesElement() || !$(_ResourceAccessPermissions.getResourceAccessesElement()).is(':visible')) {
			return;
		}

		let flags  = parseInt(resourceAccess.flags);
		let trHtml = `<tr id="id_${resourceAccess.id}" class="resourceAccess"><td class="title-cell"><b>${resourceAccess.signature}</b></td>`;

		let noAuthAccessPossible = resourceAccess.visibleToAuthenticatedUsers === false && (!resourceAccess.grantees || resourceAccess.grantees.length === 0);

		let flagWarningTexts = {};
		let flagSanityInfos  = {};
		let hasAuthFlag      = false;
		let hasNonAuthFlag   = false;

		for (let key in _ResourceAccessPermissions.mask) {

			let flagIsSet = (flags & _ResourceAccessPermissions.mask[key]);

			let disabledBecauseNotPublic    = (key.startsWith('NON_AUTH_') && resourceAccess.visibleToPublicUsers === false);
			let disabledBecausePublic       = (key.startsWith('AUTH_')     && resourceAccess.visibleToPublicUsers === true);
			let disabledBecauseNoAuthAccess = (key.startsWith('AUTH_')     && noAuthAccessPossible);

			if (flagIsSet && disabledBecausePublic) {
				// CRITICAL: If the permission is visibleToPublicUsers and includes permissions for authenticated users it can be a security threat! Disable and WARN
				let arr = flagWarningTexts[key] || [];
				arr.push('Should <b>not</b> be set because the permission itself is visible to public users. Every authenticated user can also see the permission which is probably not intended.<br><br>This might have a <b>security impact</b> as the resource is accessible with the <b>' + _ResourceAccessPermissions.getVerbFromKey(key) + '</b> method (<b>but not the data</b> behind that resource)!<br><br><b>Quick Fix</b>: Remove the visibleToPublicUsers flag from this permission.');
				flagWarningTexts[key] = arr;
			}

			if (flagIsSet && disabledBecauseNotPublic) {
				let arr = flagSanityInfos[key] || [];
				arr.push('Active for public users but permission can not be seen by public users.<br><br>This has no security-impact and is probably only misconfigured.');
				flagSanityInfos[key] = arr;
			}

			if (flagIsSet && disabledBecauseNoAuthAccess) {
				let arr = flagSanityInfos[key] || [];
				arr.push('Active for authenticated users but permission can not be seen by authenticated users or a group.<br><br>This has no security-impact and is probably only misconfigured.');
				flagSanityInfos[key] = arr;
			}

			hasNonAuthFlag = hasNonAuthFlag || (flagIsSet && key.startsWith('NON_AUTH_'));
			hasAuthFlag    = hasAuthFlag    || (flagIsSet && key.startsWith('AUTH_'));

			// currently this function is disabled (would grey out checkboxes if permission is not usable)
			let isDisabled = false && (disabledBecauseNotPublic || disabledBecauseNoAuthAccess);

			let additionalClasses = [];
			if (key === 'AUTH_USER_PATCH') { additionalClasses.push('pr-12'); }
			if (key === 'NON_AUTH_USER_PATCH') { additionalClasses.push('pr-12'); }

			trHtml += `<td class="${additionalClasses.join(' ')}"><input type="checkbox" ${(flagIsSet ? 'checked="checked"' : '')}${(isDisabled ? ' disabled' : '')} data-flag="${_ResourceAccessPermissions.mask[key]}" class="resource-access-flag" data-key="${key}"></td>`;
		}

		let showBitmaskColumn   = UISettings.getValueForSetting(UISettings.settingGroups.security.settings.showBitmaskColumnInPermissionsTableKey);
		let showVisibilityFlags = UISettings.getValueForSetting(UISettings.settingGroups.security.settings.showVisibilityFlagsInPermissionsTableKey);

		if (showBitmaskColumn) {
			trHtml += `<td><input type="text" class="bitmask" size="4" value="${flags}"></td>`;
		}

		if (showVisibilityFlags) {

			trHtml += `
				<td><input type="checkbox" ${(resourceAccess.visibleToAuthenticatedUsers ? 'checked="checked"' : '')} name="visibleToAuthenticatedUsers" class="resource-access-visibility"></td>
				<td><input type="checkbox" ${(resourceAccess.visibleToPublicUsers ? 'checked="checked"' : '')} name="visibleToPublicUsers" class="resource-access-visibility"></td>
			`;
		}

		trHtml += '<td class="actions"></td></tr>';

		let tr         = $(trHtml);
		let actionsCol = $('td.actions', tr);
		_ResourceAccessPermissions.appendPrincipalIconOrMargin(actionsCol, resourceAccess);
		_Entities.appendNewAccessControlIcon(actionsCol, resourceAccess, false);

		actionsCol.append(_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'ml-2', 'delete-resource-access']), 'Delete'));

		$('.delete-resource-access', tr).on('click', (e) => {
			e.stopPropagation();
			resourceAccess.name = resourceAccess.signature;
			_Entities.deleteNode(resourceAccess);
		});

		if (hasAuthFlag && hasNonAuthFlag) {
			_Helpers.appendInfoTextToElement({
				text: 'Permission has flags for authenticated and public users. This is probably misconfigured and should be changed or split into two permissions.',
				element: $('.title-cell b', tr),
				customToggleIcon: _Icons.iconWarningYellowFilled,
				customToggleIconClasses: [],
				css: {
					float:'right',
					marginRight: '5px'
				}
			});
		}

		for (let key in flagWarningTexts) {
			_Helpers.appendInfoTextToElement({
				text: flagWarningTexts[key].join('<br><br>'),
				element: $('input[data-key=' + key + ']', tr),
				customToggleIcon: _Icons.iconWarningYellowFilled,
				customToggleIconClasses: [],
				css: { position: 'absolute' },
				insertAfter: true
			});
		}

		for (let key in flagSanityInfos) {
			if (!flagWarningTexts[key]) {
				_Helpers.appendInfoTextToElement({
					text: flagSanityInfos[key].join('<br><br>'),
					element: $('input[data-key=' + key + ']', tr),
					customToggleIcon: _Icons.iconErrorRedFilled,
					customToggleIconClasses: ['icon-red'],
					css: { position: 'absolute' },
					insertAfter: true
				});
			}
		}

		let replaceElement = $('#resourceAccessesTable #id_' + resourceAccess.id);

		if (replaceElement && replaceElement.length) {

			replaceElement.replaceWith(tr);
			if (blinkAfterUpdate) {
				_Helpers.blinkGreen(tr.find('td'));
			}

		} else {

			$('#resourceAccessesTable').append(tr);
		}

		if (showBitmaskColumn) {

			let bitmaskInput = $('.bitmask', tr);
			bitmaskInput.on('blur', function() {
				_ResourceAccessPermissions.updateResourceAccessFlags(resourceAccess.id, $(this).val());
			});

			bitmaskInput.keypress(function(e) {
				if (e.keyCode === 13) {
					$(this).blur();
				}
			});
		}

		let div = Structr.node(resourceAccess.id);

		$('input[type=checkbox].resource-access-flag', tr).on('change', function() {
			let newFlags = 0;
			tr.find('input.resource-access-flag:checked').each(function(i, input) {
				newFlags += parseInt($(input).attr('data-flag'));
			});
			_ResourceAccessPermissions.updateResourceAccessFlags(resourceAccess.id, newFlags);
		});

		$('input[type=checkbox].resource-access-visibility', tr).on('change', function() {

			let visibilityOptionName  = $(this).attr('name');
			let visibilityOptionValue = $(this).prop('checked');

			Command.setProperty(resourceAccess.id, visibilityOptionName, visibilityOptionValue, false, function() {
				_ResourceAccessPermissions.updateResourcesAccessRow(resourceAccess.id);
			});
		});

		return div;
	},
	updateResourceAccessFlags: (id, newFlags) => {

		Command.setProperty(id, 'flags', newFlags, false, () => {
			_ResourceAccessPermissions.updateResourcesAccessRow(id);
		});
	},
	updateResourcesAccessRow: (id, blinkGreen = true) => {
		Command.get(id, _ResourceAccessPermissions.defaultResourceAccessPermissionAttributes, (obj) => {
			_ResourceAccessPermissions.appendResourceAccessElement(obj, blinkGreen);
		});
	},
	appendPrincipalIconOrMargin: (actionsCol, resourceAccess) => {

		let principalCount = resourceAccess.grantees?.length ?? 0;

		if (principalCount === 0) {

			let html = _Icons.getSvgIcon(_Icons.nonExistentEmptyIcon, 16, 16, ['mr-1']);
			actionsCol.append(html);

		} else {

			let x = _Helpers.appendInfoTextToElement({
				text: 'This permission has access rights configured for the following users/groups. Click to open the ACL dialog.<br><br>' + resourceAccess.grantees.map(p => p.name).join('<br>'),
				element: actionsCol,
				customToggleIcon: _Icons.iconSecurityRegularUser,
				customToggleIconClasses: [''],
				width: 16,
				height: 16,
				skipClick: true
			});

			_Entities.bindAccessControl(x[0], resourceAccess);
		}
	}
};

let _CorsSettings = {

	getCorsSettingsElement: () => document.querySelector('#corsSettings'),

	refreshCorsSettings: () => {

		_Helpers.fastRemoveAllChildren(_CorsSettings.getCorsSettingsElement());
		_CorsSettings.getCorsSettingsElement().insertAdjacentHTML('beforeend', _Security.templates.corsSettings());

		_Helpers.activateCommentsInElement(_CorsSettings.getCorsSettingsElement());

		let csPager = _Pager.addPager(_Security.corsSettingsPagerId, _CorsSettings.getCorsSettingsElement().querySelector('#corsSettingsPager'), true, 'CorsSetting', undefined, null, null, 'id,name,type,requestUri,isCorsSetting,acceptedOrigins,maxAge,allowMethods,allowHeaders,allowCredentials,exposeHeaders', true);

		csPager.cleanupFunction = () => {
			_Helpers.fastRemoveAllChildren(document.querySelector('#corsSettingsTable tbody'));
		};

		csPager.activateFilterElements(_CorsSettings.getCorsSettingsElement());
		csPager.setIsPaused(false);
		csPager.refresh();

		_CorsSettings.getCorsSettingsElement().querySelector('.add_cors_setting_button')?.addEventListener('click', _CorsSettings.addCorsSetting);
		_CorsSettings.getCorsSettingsElement().querySelector('#cors-setting-request-uri')?.addEventListener('keyup', (e) => {
			if (e.keyCode === 13) {
				_CorsSettings.addCorsSetting();
			}
		});
	},
	appendCorsSettingToElement: (corsSetting) => {

		let tr = _Helpers.createSingleDOMElementFromHTML(`
			<tr id="id_${corsSetting.id}" class="cors-setting">
				<td class="title-cell"><input type="text" class="cors-request-uri" data-attr-key="requestUri" size="34" value="${corsSetting.requestUri ?? ''}"></td>
				<td>${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'delete-cors-setting']), 'Delete')}</td>
				<td><input type="text" class="cors-accepted-origins" data-attr-key="acceptedOrigins" size="14" value="${corsSetting.acceptedOrigins ?? ''}"></td>
				<td><input type="text" class="cors-max-age" data-attr-key="maxAge" size="4" value="${corsSetting.maxAge ?? ''}"></td>
				<td><input type="text" class="cors-allow-methods" data-attr-key="allowMethods" size="14" value="${corsSetting.allowMethods ?? ''}"></td>
				<td><input type="text" class="cors-allow-headers" data-attr-key="allowHeaders" size="14" value="${corsSetting.allowHeaders ?? ''}"></td>
				<td><input type="text" class="cors-allow-credentials" data-attr-key="allowCredentials" size="14" value="${corsSetting.allowCredentials ?? ''}"></td>
				<td><input type="text" class="cors-expose-headers" data-attr-key="exposeHeaders" size="14" value="${corsSetting.exposeHeaders ?? ''}"></td>
			</tr>
		`);

		document.querySelector('#corsSettingsTable tbody').appendChild(tr);

		tr.querySelector('.delete-cors-setting').addEventListener('click', (e) => {
			e.stopPropagation();
			corsSetting.name = corsSetting.requestUri;
			_Entities.deleteNode(corsSetting);
		});

		for (let inp of tr.querySelectorAll('input[type="text"]')) {
			inp.addEventListener('blur', e => {
				e.stopPropagation();

				let key   = inp.dataset.attrKey;
				let value = inp.value;

				if (key === 'requestUri' && value === '') {

					_Helpers.blinkRed(inp);
					inp.value = corsSetting[key];

					inp.setCustomValidity('Request URI must not be empty');
					inp.reportValidity();

					setTimeout(() => {
						inp.setCustomValidity('');
					}, 2000);

				} else {

					_Entities.setPropertyWithFeedback(corsSetting, key, value, $(inp), inp);
				}
			});
		}

	},
	addCorsSetting: () => {

		let inp = $('#cors-setting-request-uri');
		inp.attr('disabled', 'disabled').addClass('disabled').addClass('read-only');
		$('.add_cors_setting_button', $(_CorsSettings.getCorsSettingsElement())).attr('disabled', 'disabled').addClass('disabled').addClass('read-only');

		let reEnableInput = () => {
			$('.add_cors_setting_button', $(_CorsSettings.getCorsSettingsElement())).attr('disabled', null).removeClass('disabled').removeClass('read-only');
			inp.attr('disabled', null).removeClass('disabled').removeClass('readonly');
		};

		let requestUri = inp.val();
		if (requestUri) {
			_CorsSettings.createCorsSetting(requestUri, (obj) => {
				reEnableInput();
				inp.val('');
				_CorsSettings.appendCorsSettingToElement(obj);
			});
		} else {
			reEnableInput();
			_Helpers.blinkRed(inp);
		}
		window.setTimeout(reEnableInput, 250);
	},
	createCorsSetting: (requestUri, callback, additionalData) => {

		let corsSettingData = {
			type: 'CorsSetting',
			requestUri: requestUri
		};

		if (additionalData) {
			corsSettingData = Object.assign(corsSettingData, additionalData);
		}

		Command.create(corsSettingData, callback);
	}
};