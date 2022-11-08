/*
 * Copyright (C) 2010-2022 Structr GmbH
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
$(function() {

	// disable default contextmenu on our contextmenu *once*, so it doesnt fire/register once per element
	$(document).on('contextmenu', '#menu-area', function(e) {
		e.stopPropagation();
		e.preventDefault();
	});

	$(document).on('mouseup', function() {
		_Elements.removeContextMenu();
	});

});

let _Elements = {
	dropBlocked: false,
	inheritVisibilityFlagsKey: 'inheritVisibilityFlags_' + location.port,
	elementGroups: [
		{
			name: 'Root',
			elements: ['html', 'content', 'comment', 'template']
		},
		{
			name: 'Metadata',
			elements: ['head', 'title', 'base', 'link', 'meta', 'style']
		},
		{
			name: 'Sections',
			elements: ['body', 'section', 'nav', 'article', 'aside', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hgroup', 'header', 'footer', 'address', 'main']
		},
		{
			name: 'Grouping',
			elements: ['div', 'p', 'hr', 'ol', 'ul', 'li', 'dl', 'dt', 'dd', 'pre', 'blockquote', 'figure', 'figcaption']
		},
		{
			name: 'Scripting',
			elements: ['script', 'noscript', 'slot', 'canvas']
		},
		{
			name: 'Tabular',
			elements: ['table', 'tr', 'td', 'th', 'caption', 'colgroup', 'col', 'tbody', 'thead', 'tfoot']
		},
		{
			name: 'Text',
			elements: ['a', 'em', 'strong', 'small', 's', 'cite', 'q', 'dfn', 'abbr', 'ruby', 'rt', 'rp', 'data', 'time', 'code', 'var', 'samp', 'kbd', 'sub', 'sup', 'i', 'b', 'u', 'mark', 'bdi', 'bdo', 'span', 'br', 'wbr']
		},
		{
			name: 'Edits',
			elements: ['ins', 'del']
		},
		{
			name: 'Embedded',
			elements: ['picture', 'source', 'img', 'iframe', 'embed', 'object', 'param', 'video', 'audio', 'track', 'map', 'area']
		},
		{
			name: 'Forms',
			elements: ['form', 'input', 'button', 'select', 'datalist', 'optgroup', 'option', 'textarea', 'fieldset', 'legend', 'label', 'keygen', 'output', 'progress', 'meter']
		},
		{
			name: 'Interactive',
			elements: ['dialog', 'details', 'summary', 'command', 'menu']
		}
	],
	mostUsedAttrs: [
		{
			elements: ['div'],
			attrs: ['style']
		},
		{
			elements: ['input', 'textarea'],
			attrs: ['name', 'type', 'checked', 'selected', 'value', 'size', 'multiple', 'disabled', 'autofocus', 'placeholder', 'style', 'rows', 'cols', 'required'],
			focus: 'type'
		},
		{
			elements: ['button'],
			attrs: ['name', 'type', 'checked', 'selected', 'value', 'size', 'multiple', 'disabled', 'autofocus', 'placeholder', 'onclick', 'style', 'title', 'form', 'formaction', 'formmethod']
		},
		{
			elements: ['select'],
			attrs: ['name', 'type', 'checked', 'selected', 'value', 'size', 'multiple', 'disabled', 'autofocus', 'placeholder', 'style', 'required']
		},
		{
			elements: ['option'],
			attrs: ['name', 'type', 'checked', 'selected', 'value', 'size', 'multiple', 'disabled', 'autofocus', 'placeholder', 'style']
		},
		{
			elements: ['optgroup'],
			attrs: ['label', 'disabled', 'style'],
			focus: 'label'
		},
		{
			elements: ['form'],
			attrs: ['action', 'method', 'style']
		},
		{
			elements: ['img'],
			attrs: ['alt', 'title', 'src', 'style'],
			focus: 'src'
		},
		{
			elements: ['script', 'object'],
			attrs: ['type', 'rel', 'href', 'media', 'src', 'style'],
			focus: 'src'
		},
		{
			elements: ['link'],
			attrs: ['type', 'rel', 'href', 'style'],
			focus: 'href'
		},
		{
			elements: ['a'],
			attrs: ['type', 'rel', 'href', 'target', 'style'],
			focus: 'href'
		},
		{
			elements: ['th'],
			attrs: ['abbr', 'colspan', 'rowspan', 'headers', 'scope', 'style']
		},
		{
			elements: ['td'],
			attrs: ['colspan', 'rowspan', 'headers', 'style']
		},
		{
			elements: ['label'],
			attrs: ['for', 'form', 'style'],
			focus: 'for'
		},
		{
			elements: ['style'],
			attrs: ['type', 'media', 'scoped'],
			focus: 'type'
		},
		{
			elements: ['iframe'],
			attrs: ['src', 'width', 'height', 'style'],
			focus: 'src'
		},
		{
			elements: ['source'],
			attrs: ['src', 'type', 'media'],
			focus: 'src'
		},
		{
			elements: ['video'],
			attrs: ['autoplay', 'controls', 'height', 'loop', 'muted', 'poster', 'preload', 'src', 'width'],
			focus: 'controls'
		},
		{
			elements: ['audio'],
			attrs: ['autoplay', 'controls', 'loop', 'muted', 'preload', 'src'],
			focus: 'controls'
		},
		{
			elements: ['slot'],
			attrs: ['name'],
			focus: 'name'
		},
		{
			elements: ['details'],
			attrs: ['open'],
			focus: 'class'
		}
	],
	voidAttrs: ['br', 'hr', 'img', 'input', 'link', 'meta', 'area', 'base', 'col', 'embed', 'keygen', 'menuitem', 'param', 'track', 'wbr'],
	sortedElementGroups: [
		{
			name: 'a',
			elements: ['a', 'abbr', 'address', 'area', 'aside', 'article', 'audio']
		},
		{
			name: 'b',
			elements: ['b', 'base', 'bdi', 'bdo', 'blockquote', 'body', 'br', 'button']
		},
		{
			name: 'c',
			elements: ['canvas', 'caption', 'cite', 'code', 'colgroup', 'col', 'command', 'comment']
		},
		{
			name: 'd',
			elements: ['data', 'datalist', 'dd', 'del', 'details', 'dialog', 'div', 'dfn', 'dl', 'dt']
		},
		{
			name: 'e-f',
			elements: ['em', 'embed', '|', 'fieldset', 'figcaption', 'figure', 'form', 'footer']
		},
		{
			name: 'g-h',
			elements: ['g', '|', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'head', 'header', 'hgroup', 'hr']
		},
		{
			name: 'i-k',
			elements: ['i', 'iframe', 'img', 'input', 'ins', '|', 'kbd', 'keygen']
		},
		{
			name: 'l-m',
			elements: ['label', 'legend', 'li', 'link', '|', 'main', 'map', 'mark', 'menu', 'meta', 'meter']
		},
		{
			name: 'n-o',
			elements: ['nav', 'noscript', '|', 'object', 'ol', 'optgroup', 'option', 'output']
		},
		{
			name: 'p-r',
			elements: ['p', 'param', 'picture', 'pre', 'progress', '|',  'q', '|', 'rp', 'rt', 'ruby']
		},
		{
			name: 's',
			elements: ['s', 'samp', 'script', 'section', 'select', 'slot', 'small', 'source', 'span', 'strong', 'style', 'sub', 'summary', 'sup']
		},
		{
			name: 't',
			elements: ['table', 'tbody', 'td', 'textarea', 'th', 'thead', 'tfoot', 'time', 'title', 'tr', 'track']
		},
		{
			name: 'u-w',
			elements: ['u', 'ul', '|', 'var', 'video', '|', 'wbr']
		},
		'|',
		'custom'
	],
	suggestedElements: {
		html     : [ 'head', 'body' ],
		body     : [ 'header', 'main', 'footer' ],
		head     : [ 'title', 'style', 'base', 'link', 'meta', 'script', 'noscript' ],
		table    : [ 'thead', 'tbody', 'tr', 'tfoot', 'caption', 'colgroup' ],
		colgroup : [ 'col' ],
		thead    : [ 'tr' ],
		tbody    : [ 'tr' ],
		tfoot    : [ 'tr' ],
		tr       : [ 'th', 'td' ],
		ul       : [ 'li' ],
		ol       : [ 'li' ],
		dir      : [ 'li' ],
		dl       : [ 'dt', 'dd' ],
		select   : [ 'option', 'optgroup' ],
		optgroup : [ 'option' ],
		form     : [ 'input', 'textarea', 'select', 'button', 'label', 'fieldset' ],
		fieldset : [ 'legend', 'input', 'textarea', 'select', 'button', 'label', 'fieldset' ],
		figure   : [ 'img', 'figcaption' ],
		frameset : [ 'frame' , 'noframes' ],
		map      : [ 'area' ],
		nav      : [ 'a' ],
		object   : [ 'param' ],
		details  : [ 'summary' ],
		video    : [ 'source', 'track' ],
		audio    : [ 'source' ]
	},
	selectedEntity: undefined,
	appendEntitiesToDOMElement: function (entities, domElement) {

		for (let entity of entities) {

			if (entity) {

				let obj = StructrModel.create(entity, null, false);
				let el  = (obj.isContent) ? _Elements.appendContentElement(obj, domElement, true) : _Pages.appendElementElement(obj, domElement, true);

				if (Structr.isExpanded(entity.id)) {
					_Entities.ensureExpanded(el);
				}
			}
		}
	},
	componentNode: function(id) {
		return $($('#componentId_' + id)[0]);
	},
	appendElementElement: function(entity, refNode, refNodeIsParent) {

		if (!entity) {
			return false;
		}

		entity = StructrModel.ensureObject(entity);

		let parent;
		if (refNodeIsParent) {
			parent = refNode;
		} else {
			parent = (entity.parent && entity.parent.id) ? Structr.node(entity.parent.id) : null;
		}

		if (!parent) {
			return false;
		}

		let hasChildren = entity.childrenIds && entity.childrenIds.length;

		// store active nodes in special place..
		let isActiveNode = entity.isActiveNode();

		let elementClasses = ['node', 'element'];
		elementClasses.push(isActiveNode ? 'activeNode' : 'staticNode');
		if (_Elements.isEntitySelected(entity)) {
			elementClasses.push('nodeSelectedFromContextMenu');
		}
		if (entity.tag === 'html') {
			elementClasses.push('html_element');
		}
		if (entity.hidden === true) {
			elementClasses.push('is-hidden');
		}

		_Entities.ensureExpanded(parent);

		let id          = entity.id;
		let displayName = getElementDisplayName(entity);
		let icon        = _Elements.getElementIcon(entity);

		let html = `
			<div id="id_${id}" class="${elementClasses.join(' ')}">
				<div class="node-container flex items-center">
					<div class="node-selector"></div>
					<i class="typeIcon ${_Icons.getFullSpriteClass(icon)}"></i>
					<span class="abbr-ellipsis abbr-pages-tree"><b title="${escapeForHtmlAttributes(displayName)}" class="tag_ name_">${displayName}</b>${_Elements.classIdString(entity._html_id, entity._html_class)}</span>
					<div class="icons-container flex items-center"></div>
				</div>
			</div>
		`;

		if (refNode && !refNodeIsParent) {
			refNode.before(html);
		} else {
			parent.append(html);
		}

		let div            = Structr.node(id);
		let nodeContainer  = $('.node-container', div);
		let iconsContainer = $('.icons-container', div);

		if (!div) {
			return false;
		}

		_Elements.enableContextMenuOnElement(div, entity);
		_Entities.appendExpandIcon(nodeContainer, entity, hasChildren);

		_Entities.setMouseOver(div, undefined, ((entity.syncedNodesIds && entity.syncedNodesIds.length) ? entity.syncedNodesIds : [entity.sharedComponentId]));

		_Entities.appendContextMenuIcon(iconsContainer, entity);

		if (_Entities.isLinkableEntity(entity)) {

			if (entity.linkableId) {

				Command.get(entity.linkableId, 'id,type,name,isFile,isImage,isPage,isTemplate,path', (linkedEntity) => {

					let linkableText = $(`<span class="linkable${(linkedEntity.isImage ? ' default-cursor' : '')}">${linkedEntity.name}</span>`);
					iconsContainer.before(linkableText);

					if (linkedEntity.isImage) {

						linkableText.on('click', (e) => {
							e.stopPropagation();
							_Files.editImage(linkedEntity);
						});

					} else if (linkedEntity.isFile) {

						linkableText.on('click', (e) => {
							e.stopPropagation();
							_Files.editFile(linkedEntity, $('#dialogBox .dialogText'));
						});
					}
				});
			}
		}

		_Entities.appendNewAccessControlIcon(iconsContainer, entity);

		_Elements.clickOrSelectElementIfLastSelected(div, entity);

		return div;
	},
	clickOrSelectElementIfLastSelected: (div, entity) => {

		let selectedObjectId = LSWrapper.getItem(_Entities.selectedObjectIdKey);

		if (entity.id === selectedObjectId) {

			let isElementBeingEditedCurrently = (_Pages.centerPane.dataset['elementId'] === selectedObjectId);

			if (!isElementBeingEditedCurrently) {
				div.children('.node-container').click();
			} else {
				_Entities.selectElement(div[0], entity);
			}
		}
	},
	classIdString: (idString, classString) => {
		let htmlIdString    = (idString    ? '#' + idString.replace(/\${.*}/g, '${…}') : '');
		let htmlClassString = (classString ? '.' + classString.replace(/\${.*}/g, '${…}').replace(/ /g, '.') : '');

		return `<span class="class-id-attrs">${htmlIdString}${htmlClassString}</span>`;
	},
	enableContextMenuOnElement: function(div, entity) {

		_Elements.disableBrowserContextMenuOnElement(div);

		$(div).on('mouseup', function(e) {
			if (e.button !== 2) {
				return;
			}
			e.stopPropagation();

			_Elements.activateContextMenu(e, div, entity);
		});

	},
	disableBrowserContextMenuOnElement: function (div) {

		$(div).on("contextmenu", function(e) {
			e.stopPropagation();
			e.preventDefault();
		});

	},
	activateContextMenu:function(e, div, entity) {

		let menuElements = _Elements.getContextMenuElements(div, entity);

		let leftOrRight = 'left';
		let topOrBottom = 'top';
		let x = (e.clientX - 8);
		let y = (e.clientY - 8);
		let windowWidth  = $(window).width();
		let windowHeight = $(window).height();

		if (e.pageX > (windowWidth / 2)) {
			leftOrRight = 'right';
		}

		let cssPositionClasses = leftOrRight + ' ' + topOrBottom;

		_Elements.removeContextMenu();

		div.addClass('contextMenuActive');
		$('#menu-area').append('<div id="add-child-dialog"></div>');

		let menu = $('#add-child-dialog');
		menu.css({
			left: x + 'px',
			top: y + 'px'
		});

		let registerContextMenuItemClickHandler = function (el, contextMenuItem) {

			el.on('mouseup', function(e) {
				e.stopPropagation();

				let preventClose = true;

				if (contextMenuItem.clickHandler && (typeof contextMenuItem.clickHandler === 'function')) {
					preventClose = contextMenuItem.clickHandler($(this), contextMenuItem);
				}

				if (!preventClose) {
					_Elements.removeContextMenu();
				}
			});
		};

		let registerPlaintextContextMenuItemHandler = function (el, itemText, forcedClickHandler) {

			el.on('mouseup', function (e) {
				e.stopPropagation();

				if (forcedClickHandler && (typeof forcedClickHandler === 'function')) {
					forcedClickHandler(itemText);
				} else {
					let pageId = (entity.type === 'Page') ? entity.id : entity.pageId;
					let tagName = (itemText === 'content') ? null : itemText;

					Command.createAndAppendDOMNode(pageId, entity.id, tagName, _Dragndrop.getAdditionalDataForElementCreation(tagName), _Elements.isInheritVisibilityFlagsChecked());
				}

				_Elements.removeContextMenu();
			});
		};

		let addContextMenuElements = function (ul, element, hidden, forcedClickHandler, prepend) {

			if (hidden) {
				ul.addClass('hidden');
			}

			if (Object.prototype.toString.call(element) === '[object Array]') {

				element.forEach(function (el) {
					addContextMenuElements(ul, el, hidden, forcedClickHandler, prepend);
				});

			} else if (Object.prototype.toString.call(element) === '[object Object]') {

				if (element.visible !== undefined && element.visible === false) {
					return;
				}

				let menuEntry        = $('<li class="element-group-switch"></li>');
				let menuEntryContent = $('<span class="menu-entry-container">' + (element.icon || '') + '</span>');
				let menuEntryText    = $('<span class="menu-entry-text">' + element.name + '</span>');

				for (let cls of (element.classes || [])) {
					menuEntry.addClass(cls);
				}

				menuEntryContent.append(menuEntryText);
				menuEntry.append(menuEntryContent);

				registerContextMenuItemClickHandler(menuEntry, element);
				if (prepend) {
					ul.prepend(menuEntry);
				} else {
					ul.append(menuEntry);
				}

				if (element.elements) {
					menuEntryContent.append('<i class="fa fa-caret-right pull-right"></i>');

					var subListElement = $('<ul class="element-group ' + cssPositionClasses + '"></ul>');
					menuEntry.append(subListElement);
					addContextMenuElements(subListElement, element.elements, true, (forcedClickHandler ? forcedClickHandler : element.forcedClickHandler), prepend);
				}

			} else if (Object.prototype.toString.call(element) === '[object String]') {

				if (element === '|') {

					if (prepend) {
						ul.prepend('<hr />');
					} else {
						ul.append('<hr />');
					}

				} else {

					var listElement = $('<li>' + element + '</li>');
					registerPlaintextContextMenuItemHandler(listElement, element, forcedClickHandler, prepend);

					if (prepend) {
						ul.prepend(listElement);
					} else {
						ul.append(listElement);
					}
				}
			}
		};

		let updateMenuGroupVisibility = function() {

			$('.element-group-switch').hover(function() {
				let childrenMenu = $(this).children('.element-group');
				if (childrenMenu.length > 0) {
					childrenMenu.removeClass('hidden');

					let bottomOfMenu = childrenMenu.offset().top + childrenMenu.height();
					if (bottomOfMenu > windowHeight) {
						$(this).children('.element-group').css({
							top: (-1 - (bottomOfMenu - windowHeight) - 12) + 'px'
						});
					}
				}

			}, function() {
				$(this).children('.element-group').addClass('hidden');
			});
		};

		let mainMenuList = $('<ul class="element-group ' + cssPositionClasses + '"></ul>');
		$('#add-child-dialog').append(mainMenuList);
		menuElements.forEach(function (mainEl) {
			addContextMenuElements(mainMenuList, mainEl, false);
		});

		if (Structr.getActiveModule() === _Pages) {
			// prepend suggested elements if present
			_Elements.getSuggestedWidgets(entity, function(data) {

				if (data && data.length) {

					addContextMenuElements(mainMenuList, [ '|', { name: 'Suggested Widgets', elements: data } ], false, undefined, true);
					updateMenuGroupVisibility();
				}
			});
		}

		updateMenuGroupVisibility();

		let repositionMenu = function() {

			let menuWidth = menu.width();
			let menuHeight = menu.height();

			if (windowWidth < (x + menuWidth)) {
				menu.css({
					left: (x - menuWidth) + 'px'
				});
            }

			if (windowHeight < y + menuHeight) {
				menu.css({
					top: (windowHeight - menuHeight - 20) + 'px'
				});
			}
		};

		repositionMenu();
	},
	isInheritVisibilityFlagsChecked: function () {
		return UISettings.getValueForSetting(UISettings.pages.settings.inheritVisibilityFlagsKey);
	},
	removeContextMenu: function() {
		$('#add-child-dialog').remove();
		$('.contextMenuActive').removeClass('contextMenuActive');
	},
	getContextMenuElements: function (div, entity) {

		// 1. dedicated context menu for type
		if (entity.type === 'Widget') {
			return _Widgets.getContextMenuElements(div, entity);
		} else if (entity.type === _Pages.localizations.wrapperTypeForContextMenu) {
			return _Pages.localizations.getContextMenuElements(div, entity);
		}

		// 2. dedicated context menu for module
		let activeModule = Structr.getActiveModule();
		if (activeModule) {
			if (activeModule.getContextMenuElements && typeof activeModule.getContextMenuElements === 'function') {
				return activeModule.getContextMenuElements(div, entity);
			}
		}

		// no context menu found
		return [];
	},
	appendContextMenuSeparator: function (elements) {
		if (elements[elements.length - 1] !== '|') {
			elements.push('|');
		}
	},
	appendSecurityContextMenuItems: (elements, entity, supportsSubtree) => {

		let securityMenu = {
			icon: _Icons.getSvgIcon('visibility-lock-locked'),
			name: 'Security',
			elements: [
				{
					name: 'Access Control and Visibility',
					clickHandler: function() {
						_Entities.showAccessControlDialog(entity);
						return false;
					}
				},
				'|'
			]
		};

		let authUsers = {
			name: 'Authenticated Users',
			elements: [
				{
					name: 'Make element visible',
					clickHandler: function() {
						Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', true, false);
						return false;
					}
				},
				{
					name: 'Make element invisible',
					clickHandler: function() {
						Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', false, false);
						return false;
					}
				}
			]
		};

		if (supportsSubtree === true) {
			let authUsersSubtreeMenu = [
				'|',
				{
					name: 'Make subtree visible',
					clickHandler: function() {
						Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', true, true);
						return false;
					}
				},
				{
					name: 'Make subtree invisible',
					clickHandler: function() {
						Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', false, true);
						return false;
					}
				}
			];

			authUsers.elements = authUsers.elements.concat(authUsersSubtreeMenu);
		}

		let publicUsers = {
			name: 'Public Users',
			elements: [
				{
					name: 'Make element visible',
					clickHandler: function() {
						Command.setProperty(entity.id, 'visibleToPublicUsers', true, false);
						return false;
					}
				},
				{
					name: 'Make element invisible',
					clickHandler: function() {
						Command.setProperty(entity.id, 'visibleToPublicUsers', false, false);
						return false;
					}
				}
			]
		};

		if (supportsSubtree === true) {

			let publicUsersSubtreeMenu = [
				'|',
				{
					name: 'Make subtree visible',
					clickHandler: function() {
						Command.setProperty(entity.id, 'visibleToPublicUsers', true, true);
						return false;
					}
				},
				{
					name: 'Make subtree invisible',
					clickHandler: function() {
						Command.setProperty(entity.id, 'visibleToPublicUsers', false, true);
						return false;
					}
				}
			];
			publicUsers.elements = publicUsers.elements.concat(publicUsersSubtreeMenu);
		}

		securityMenu.elements = securityMenu.elements.concat(authUsers).concat(publicUsers);

		elements.push(securityMenu);
	},
	selectEntity: function (entity) {

		_Elements.unselectEntity();

		_Elements.selectedEntity = entity;

		var node = Structr.node(_Elements.selectedEntity.id);
		if (node) {
			node.addClass('nodeSelectedFromContextMenu');
		}
	},
	unselectEntity: function () {
		if (_Elements.selectedEntity) {

			var node = Structr.node(_Elements.selectedEntity.id);
			if (node) {
				node.removeClass('nodeSelectedFromContextMenu');
			}

			_Elements.selectedEntity = undefined;
		}
	},
	isEntitySelected: function (entity) {
		return _Elements.selectedEntity && _Elements.selectedEntity.id === entity.id;
	},
	appendContentElement: function(entity, refNode, refNodeIsParent) {

		let parent;

		if (entity.parent && entity.parent.id) {
			parent = Structr.node(entity.parent.id);
			_Entities.ensureExpanded(parent);
		} else {
			parent = refNode;
		}

		if (!parent) {
			return false;
		}

		let isActiveNode = entity.isActiveNode();
		let isTemplate   = (entity.type === 'Template');
		let name         = entity.name;
		let displayName  = getElementDisplayName(entity);
		let nameText     = (name ? `<b title="${escapeForHtmlAttributes(displayName)}" class="tag_ name_">${displayName}</b>` : `<span class="content_">${escapeTags(entity.content)}</span>`);

		let icon = _Elements.getContentIcon(entity);
		let html = `
			<div id="id_${entity.id}" class="node content ${(isActiveNode ? ' activeNode' : 'staticNode') + (_Elements.isEntitySelected(entity) ? ' nodeSelectedFromContextMenu' : '')}">
				<div class="node-container flex items-center">
					<div class="node-selector"></div>
					<i class="typeIcon ${_Icons.getFullSpriteClass(icon)} typeIcon-nochildren"></i>
					<span class="abbr-ellipsis abbr-pages-tree">${nameText}</span>
					<div class="icons-container flex items-center"></div>
				</div>
			</div>`;


		if (refNode && !refNodeIsParent) {
			refNode.before(html);
		} else {
			parent.append(html);
		}

		let div            = Structr.node(entity.id);
		let nodeContainer  = $('.node-container', div);
		let iconsContainer = $('.icons-container', div);

		_Dragndrop.makeSortable(div);
		_Dragndrop.makeDroppable(div);

		if (isTemplate) {
			let hasChildren = entity.childrenIds && entity.childrenIds.length;
			_Entities.appendExpandIcon(nodeContainer, entity, hasChildren);
		}

		if (entity.hidden === true) {
			div.addClass('is-hidden');
		}

		_Elements.enableContextMenuOnElement(nodeContainer, entity);

		_Pages.registerDetailClickHandler(nodeContainer, entity);

		_Entities.setMouseOver(div, undefined, ((entity.syncedNodesIds && entity.syncedNodesIds.length) ? entity.syncedNodesIds : [entity.sharedComponentId]));

		_Entities.appendContextMenuIcon(iconsContainer, entity);
		_Entities.appendNewAccessControlIcon(iconsContainer, entity);

		_Elements.clickOrSelectElementIfLastSelected(div, entity);

		return div;
	},
	getContentIcon:function(content) {
		let isComment = (content.type === 'Comment');
		let isTemplate = (content.type === 'Template');
		let isComponent = content.sharedComponentId || (content.syncedNodesIds && content.syncedNodesIds.length);
		let isActiveNode = (typeof content.isActiveNode === "function") ? content.isActiveNode() : false;

		return isComment ? _Icons.comment_icon : ((isTemplate && isComponent) ? _Icons.icon_shared_template : (isTemplate ? (isActiveNode ? _Icons.active_template_icon : _Icons.template_icon) : (isComponent ? _Icons.active_content_icon : (isActiveNode ? _Icons.active_content_icon : _Icons.content_icon))));
	},
	getElementIcon:function(element) {
		let isComponent  = element.sharedComponentId || (element.syncedNodesIds && element.syncedNodesIds.length);
		let isActiveNode = (typeof element.isActiveNode === "function") ? element.isActiveNode() : false;

		return (isActiveNode ? _Icons.repeater_icon : (isComponent ? _Icons.comp_icon : _Icons.brick_icon));
	},
	openEditContentDialog: (entity) => {

		Structr.dialog('Edit content of ' + (entity.name ? entity.name : entity.id), function() {}, function() {}, ['popup-dialog-with-editor']);

		dialogBtn.append('<button id="saveFile" disabled="disabled" class="disabled"> Save </button>');
		dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');
		dialog.append('<div class="editor h-full"></div>');

		let dialogSaveButton = dialogBtn[0].querySelector('#saveFile');
		let saveAndClose     = dialogBtn[0].querySelector('#saveAndClose');

		Command.get(entity.id, 'content,contentType', (data) => {

			entity.contentType = data.contentType;

			let initialText = data.content;

			let openEditDialogMonacoConfig = {
				value: initialText,
				language: entity.contentType,
				lint: true,
				autocomplete: true,
				preventRestoreModel: true,
				forceAllowAutoComplete: true,
				changeFn: (editor, entity) => {

					let editorText = editor.getValue();

					if (initialText === editorText) {
						dialogSaveButton.disabled = true;
						dialogSaveButton.classList.add('disabled');
						saveAndClose.disabled = true;
						saveAndClose.classList.add('disabled');
					} else {
						dialogSaveButton.disabled = null;
						dialogSaveButton.classList.remove('disabled');
						saveAndClose.disabled = null;
						saveAndClose.classList.remove('disabled');
					}
				},
				saveFn: (editor, entity) => {

					let text1 = initialText || '';
					let text2 = editor.getValue();

					if (text1 === text2) {
						return;
					}

					Command.patch(entity.id, text1, text2, function () {
						Structr.showAndHideInfoBoxMessage('Content saved.', 'success', 2000, 200);
						dialogSaveButton.disabled = true;
						dialogSaveButton.classList.add('disabled');
						saveAndClose.disabled = true;
						saveAndClose.classList.add('disabled');

						Command.getProperty(entity.id, 'content', (newText) => {
							initialText = newText;
						});
					});
				}
			};

			let editor = _Editors.getMonacoEditor(entity, 'source', dialog[0].querySelector('.editor'), openEditDialogMonacoConfig);

			Structr.resize();

			dialogSaveButton.addEventListener('click', (e) => {
				e.stopPropagation();
				openEditDialogMonacoConfig.saveFn(editor, entity);
			});
			saveAndClose.addEventListener('click', (e) => {
				e.stopPropagation();

				openEditDialogMonacoConfig.saveFn(editor, entity);

				setTimeout(function() {
					dialogSaveButton.remove();
					saveAndClose.remove();
					dialogCancelButton.click();
				}, 500);
			});

			dialogMeta.append(`<span class="editor-info"></span>`);

			let editorInfo = dialogMeta[0].querySelector('.editor-info');
			_Editors.appendEditorOptionsElement(editorInfo);

			Structr.resize();

			_Editors.resizeVisibleEditors();
		});
	},
	displayCentralEditor: (entity) => {

		let previewsContainer      = document.querySelector('#center-pane');
		let contentEditorContainer = document.querySelector('#center-pane .content-editor-container');

		if (contentEditorContainer) {
			previewsContainer.removeChild(contentEditorContainer);
		}

		previewsContainer.insertAdjacentHTML('afterbegin', _Pages.templates.contentEditor());

		contentEditorContainer = document.querySelector('#center-pane .content-editor-container');

		Command.get(entity.id, 'content,contentType', (data) => {
			entity.contentType = data.contentType;
			_Elements.editContentInCentralEditor(entity, data.content, contentEditorContainer);
		});
	},
	editContentInCentralEditor: function (entity, initialText, element) {

		let buttonArea = element.querySelector('.editor-button-container') || dialogBtn;
		let infoArea   = element.querySelector('.editor-info-container')   || dialogMeta;
		let contentBox = element.querySelector('.editor');
		buttonArea.insertAdjacentHTML('beforeend', `<button id="editorSave" disabled="disabled" class="disabled">Save</button>`);
		let saveButton = buttonArea.querySelector('.save-button') || document.querySelector('#editorSave');

		infoArea.insertAdjacentHTML('beforeend', `
			<label for="contentTypeSelect">Content-Type: </label><select class="contentType_" id="contentTypeSelect"></select>
			<span class="editor-info"></span>
		`);

		let contentType = entity.contentType || 'text/plain';

		let centralEditorMonacoConfig = {
			value: initialText || '',
			language: contentType,
			lint: true,
			autocomplete: true,
			forceAllowAutoComplete: true,
			changeFn: (editor, entity) => {

				let editorText = editor.getValue();

				if (initialText === editorText) {
					saveButton.disabled = true;
					saveButton.classList.add('disabled');
				} else {
					saveButton.disabled = null;
					saveButton.classList.remove('disabled');
				}
			},
			saveFn: (editor, entity) => {

				let text1 = initialText || '';
				let text2 = editor.getValue();

				if (text1 === text2) {
					return;
				}

				Command.patch(entity.id, text1, text2, function () {

					Structr.showAndHideInfoBoxMessage('Content saved.', 'success', 2000, 200);
					saveButton.disabled = true;
					saveButton.classList.add('disabled');

					Command.getProperty(entity.id, 'content', function (newText) {
						initialText = newText;
					});
				});
			}
		};

		let editor = _Editors.getMonacoEditor(entity, 'content', contentBox, centralEditorMonacoConfig);

		let editorInfo = infoArea.querySelector('.editor-info');
		_Editors.appendEditorOptionsElement(editorInfo);

		Structr.resize();

		if (entity.isFavoritable) {

			buttonArea.insertAdjacentHTML('beforeend', `<i title="Add to favorites" class="add-to-favorites ${_Icons.getFullSpriteClass(_Icons.star_icon)}"></i>`);
			let addToFavs = buttonArea.querySelector('.add-to-favorites');

			addToFavs.addEventListener('click', () => {
				Command.favorites('add', entity.id, () => {
					blinkGreen(addToFavs);
				});
			});
		}

		_Editors.enableSpeechToTextForEditor(editor, buttonArea);

		saveButton.addEventListener('click', (e) => {
			e.stopPropagation();

			centralEditorMonacoConfig.saveFn(editor, entity);
		});

		const values = ['text/plain', 'text/html', 'text/xml', 'text/css', 'text/javascript', 'text/markdown', 'text/textile', 'text/mediawiki', 'text/tracwiki', 'text/confluence', 'text/asciidoc'];

		let select = infoArea.querySelector('#contentTypeSelect');
		values.forEach((type) => {
			select.insertAdjacentHTML('beforeend', `<option ${(type === entity.contentType ? 'selected' : '')} value="${type}">${type}</option>`);
		});
		select.addEventListener('change', (e) => {

			contentType = select.value;

			entity.setProperty('contentType', contentType, false, function() {

				_Editors.updateMonacoEditorLanguage(editor, contentType);

				blinkGreen(select);
			});
		});

		editor.focus();
	},
	getSuggestedWidgets: function(entity, callback) {

		if (entity.isPage || entity.isContent) {

			// no-op

		} else {

			var clickHandler = function(element, item) {
				Command.get(item.id, undefined, function(result) {
					_Widgets.insertWidgetIntoPage(result, entity, entity.pageId);
				});
			};

			var classes = entity._html_class && entity._html_class.length ? entity._html_class.split(' ') : [];
			var htmlId  = entity._html_id;
			var tag     = entity.tag;

			Command.getSuggestions(htmlId, entity.name, tag, classes, function(result) {
				let data = [];
				if (result && result.length) {
					result.forEach(function (r) {
						data.push({
							id: r.id,
							name: r.name,
							source: r.source,
							clickHandler: clickHandler
						});
					});
				}
				callback(data);
			});
		}
	}
};