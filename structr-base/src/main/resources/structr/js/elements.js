/*
 * Copyright (C) 2010-2023 Structr GmbH
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
let _Elements = {
	dropBlocked: false,
	inheritVisibilityFlagsKey: 'inheritVisibilityFlags_' + location.port,
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
			focus: 'class'
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
			elements: ['table', 'tbody', 'td', 'template', 'textarea', 'th', 'thead', 'tfoot', 'time', 'title', 'tr', 'track']
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
	appendEntitiesToDOMElement: (entities, domElement) => {

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
	appendElementElement: (entity, refNode, refNodeIsParent) => {

		if (!entity) {
			return false;
		}

		entity = StructrModel.ensureObject(entity);

		let parent;
		if (refNodeIsParent) {
			parent = refNode;
		} else {
			parent = (entity.parent && entity.parent.id) ? Structr.node(entity.parent.id) : null;

			if (parent && parent.length > 0 && !Structr.isExpanded(entity.parent.id)) {
				// if the parent element is not expanded, then we don't need to add the element we want to add,
				// we simply need to expand the parent
				_Entities.ensureExpanded(parent);
				return;
			}
		}

		if (!parent) {
			return false;
		}

		let isRootSharedComponent = (entity.parent === null && entity.pageId === _Pages.shadowPage.id);
		let isRootTrashElement    = (entity.parent === null && entity.pageId === null);
		let hasChildren           = entity.childrenIds && entity.childrenIds.length;

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
		let displayName = _Helpers.getElementDisplayName(entity);

		let html = `
			<div id="id_${id}" class="${elementClasses.join(' ')}" draggable="true">

				${isRootSharedComponent || isRootTrashElement ? '' : '<div class="before-node"></div>'}

				<div class="node-container flex items-center">
					${_Icons.getSvgIconForElementNode(entity)}
					<span class="abbr-ellipsis abbr-pages-tree"><b title="${_Helpers.escapeForHtmlAttributes(displayName)}" class="tag_ name_">${displayName}</b>${_Elements.classIdString(entity)}</span>
					<div class="icons-container flex items-center"></div>
				</div>

				${isRootSharedComponent || isRootTrashElement ? '' : '<div class="after-node"></div>'}
			</div>
		`;

		if (refNode && !refNodeIsParent) {
			refNode.before(html);
		} else {
			let afterNode = parent.children('.after-node');
			if (afterNode.length) {
				parent.children('.after-node').before(html);
			} else {
				parent.append(html);
			}
		}

		let div            = Structr.node(id);
		let nodeContainer  = $('.node-container', div);
		let iconsContainer = $('.icons-container', div);

		_Dragndrop.enableDraggable(entity, div[0], _Dragndrop.dropActions.domElement, true, _Dragndrop.pages.dragStart, _Dragndrop.pages.dragEnd);
		_Dragndrop.pages.enableDroppable(entity, div[0], nodeContainer[0]);

		if (!div) {
			return false;
		}

		_Elements.contextMenu.enableContextMenuOnElement(div[0], entity);
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
	classIdString: (entity) => {
		let idString    = entity._html_id;
		let classString = entity._html_class;

		let htmlIdString    = (idString    ? '#' + idString.replace(/\${.*}/g, '${…}') : '');
		let htmlClassString = (classString ? '.' + classString.replace(/\${.*}/g, '${…}').replace(/ /g, '.') : '');

		// only display tagname if node has a displayname other than its tagname (otherwise the tagname itself is already displayed in the name field)
		let displayName = _Helpers.getElementDisplayName(entity);
		let tagName     = displayName !== entity.type.toLowerCase() ? '&nbsp;&nbsp;' + entity.type.toLowerCase() : '';

		return `<span class="class-id-attrs">${tagName}${htmlIdString}${htmlClassString}</span>`;
	},
	contextMenu: {
		domId: 'context-menu-dialog',
		getContainerDOMNode: () => document.getElementById('custom-context-menu-container'),
		getDOMNode: () => document.getElementById(_Elements.contextMenu.domId),
		init: () => {

			// disable default contextmenu on our contextmenu *once*, so it doesnt fire/register once per element
			_Elements.contextMenu.getContainerDOMNode().addEventListener('contextmenu', (e) => {
				e.stopPropagation();
				e.preventDefault();
			});

			document.addEventListener('mouseup', () => {
				_Elements.contextMenu.remove();
			});
		},
		enableContextMenuOnElement: (div, entity) => {

			_Helpers.disableNormalContextMenuOnElement(div);

			div.addEventListener('mouseup', (e) => {
				if (e.button !== 2) {
					return;
				}
				e.stopPropagation();

				_Elements.contextMenu.activateContextMenu(e, div, entity);
			});
		},
		activateContextMenu: (e, div, entity) => {

			_Elements.contextMenu.remove();

			let menuElements       = _Elements.contextMenu.getContextMenuElements(div, entity);
			let x                  = (e.clientX - 8) + (entity?.offset?.x ?? 0);
			let y                  = (e.clientY - 8) + (entity?.offset?.y ?? 0);
			let windowWidth        = window.innerWidth;
			let leftOrRight        = (e.pageX > (windowWidth / 2)) ? 'right' : 'left';
			let topOrBottom        = 'top';
			let cssPositionClasses = `${leftOrRight} ${topOrBottom}`;

			div.classList.add('contextMenuActive');

			let contextMenuElement = _Helpers.createSingleDOMElementFromHTML(`<div id="${_Elements.contextMenu.domId}"></div>`);
			let container          = _Elements.contextMenu.getContainerDOMNode();

			contextMenuElement.style.left = x + 'px';
			contextMenuElement.style.top  = y + 'px';

			container.appendChild(contextMenuElement);

			let mainMenuList = _Helpers.createSingleDOMElementFromHTML(`<ul class="element-group ${cssPositionClasses}"></ul>`);

			for (let menuEl of menuElements) {
				_Elements.contextMenu.addContextMenuElements({ ul: mainMenuList, element: menuEl, cssPositionClasses, entity });
			}

			contextMenuElement.append(mainMenuList);

			if (Structr.getActiveModule() === _Pages) {

				// prepend suggested elements if present
				_Elements.contextMenu.getSuggestedWidgets(entity, (data) => {

					if (data && data.length) {

						_Elements.contextMenu.addContextMenuElements({ ul: mainMenuList, element: [ '|', { name: 'Suggested Widgets', elements: data } ], prepend: true, cssPositionClasses, entity });
						_Elements.contextMenu.updateMenuGroupVisibility();
					}
				});
			}

			_Elements.contextMenu.updateMenuGroupVisibility();
			_Elements.contextMenu.repositionMenu(x, y);
		},
		addContextMenuElements: ({ ul, element, hidden = false, forcedClickHandler, prepend = false, cssPositionClasses, entity }) => {

			if (hidden) {
				ul.classList.add('hidden');
			}

			if (Array.isArray(element)) {

				for (let el of element) {
					_Elements.contextMenu.addContextMenuElements({ ul, element: el, hidden, forcedClickHandler, prepend, cssPositionClasses, entity });
				}

			} else if (typeof element === 'string') {

				if (element === '|') {

					ul.insertAdjacentHTML((prepend ? 'afterbegin' : 'beforeend'), '<hr>');

				} else {

					let listElement = _Helpers.createSingleDOMElementFromHTML(`<li>${element}</li>`);
					_Elements.contextMenu.registerPlaintextItemHandler(listElement, entity, element, forcedClickHandler);

					ul.insertAdjacentElement((prepend ? 'afterbegin' : 'beforeend'), listElement);
				}

			} else if (Object.prototype.toString.call(element) === '[object Object]') {

				if (element.visible === false) {
					return;
				}

				let menuEntry        = _Helpers.createSingleDOMElementFromHTML('<li class="element-group-switch"></li>');
				let menuEntryContent = _Helpers.createSingleDOMElementFromHTML(`<span class="menu-entry-container items-center">${element.icon || ''}</span>`);

				menuEntry.classList.add(...(element.classes || []));

				if (element.html) {

					let menuEntryHtml = _Helpers.createSingleDOMElementFromHTML(`<span class="menu-entry-html">${element.html}</span>`);
					menuEntryContent.appendChild(menuEntryHtml);

					// if (element.changeHandler) {
					// 	menuEntryField[0].querySelector('span > input').addEventListener('change', element.changeHandler);
					// }

				} else if (element.name) {

					let menuEntryText = _Helpers.createSingleDOMElementFromHTML(`<span class="menu-entry-text">${element.name}</span>`);
					menuEntryContent.appendChild(menuEntryText);
				}

				menuEntry.appendChild(menuEntryContent);

				_Elements.contextMenu.registerItemClickHandler(menuEntry, element);

				ul.insertAdjacentElement((prepend ? 'afterbegin' : 'beforeend'), menuEntry);

				if (element.elements) {

					menuEntryContent.insertAdjacentHTML('beforeend', _Icons.getSvgIcon(_Icons.iconChevronRightFilled, 10, 10, ['icon-grey']));

					let subListElement = _Helpers.createSingleDOMElementFromHTML(`<ul class="element-group ${cssPositionClasses}"></ul>`);
					menuEntry.appendChild(subListElement);
					_Elements.contextMenu.addContextMenuElements({ ul: subListElement, element: element.elements, hidden: true, forcedClickHandler: (forcedClickHandler ? forcedClickHandler : element.forcedClickHandler), prepend, cssPositionClasses, entity });
				}
			}
		},
		appendContextMenuSeparator: (elements) => {
			if (elements[elements.length - 1] !== '|' && elements.length > 0) {
				elements.push('|');
			}
		},
		appendSecurityContextMenuItems: (elements, entity, supportsSubtree) => {

			let securityMenu = {
				icon: _Icons.getMenuSvgIcon(_Icons.iconVisibilityLocked),
				name: 'Security',
				elements: [
					{
						name: 'Access Control and Visibility',
						clickHandler: () => {
							_Entities.showAccessControlDialog(entity);
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
						clickHandler: () => {
							Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', true, false);
						}
					},
					{
						name: 'Make element invisible',
						clickHandler: () => {
							Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', false, false);
						}
					}
				]
			};

			if (supportsSubtree === true) {
				let authUsersSubtreeMenu = [
					'|',
					{
						name: 'Make subtree visible',
						clickHandler: () => {
							Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', true, true);
						}
					},
					{
						name: 'Make subtree invisible',
						clickHandler: () => {
							Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', false, true);
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
						clickHandler: () => {
							Command.setProperty(entity.id, 'visibleToPublicUsers', true, false);
						}
					},
					{
						name: 'Make element invisible',
						clickHandler: () => {
							Command.setProperty(entity.id, 'visibleToPublicUsers', false, false);
						}
					}
				]
			};

			if (supportsSubtree === true) {

				let publicUsersSubtreeMenu = [
					'|',
					{
						name: 'Make subtree visible',
						clickHandler: () => {
							Command.setProperty(entity.id, 'visibleToPublicUsers', true, true);
						}
					},
					{
						name: 'Make subtree invisible',
						clickHandler: () => {
							Command.setProperty(entity.id, 'visibleToPublicUsers', false, true);
						}
					}
				];
				publicUsers.elements = publicUsers.elements.concat(publicUsersSubtreeMenu);
			}

			securityMenu.elements = securityMenu.elements.concat(authUsers).concat(publicUsers);

			elements.push(securityMenu);
		},
		getContextMenuElements: (div, entity) => {

			// 1. dedicated context menu for type
			if (entity.type === 'Widget') {

				return _Widgets.getContextMenuElements(div, entity);

			} else if (entity.type === _Pages.localizations.wrapperTypeForContextMenu) {

				return _Pages.localizations.getContextMenuElements(div, entity);

			} else if (entity.type === _Pages.previews.wrapperTypeForContextMenu) {

				return _Pages.previews.getContextMenuElements(div, entity);
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
		registerItemClickHandler: (menuEntry, contextMenuItem) => {

			menuEntry.addEventListener('mouseup', async (e) => {

				e.stopPropagation();

				let stayOpen = false;

				if (contextMenuItem.clickHandler && (typeof contextMenuItem.clickHandler === 'function')) {
					stayOpen = contextMenuItem.clickHandler(e);
				}

				if (stayOpen !== true) {
					_Elements.contextMenu.remove();
				}
			});
		},
		registerPlaintextItemHandler: (menuEntry, entity, itemText, forcedClickHandler) => {

			menuEntry.addEventListener('mouseup', (e) => {

				e.stopPropagation();

				if (forcedClickHandler && (typeof forcedClickHandler === 'function')) {

					forcedClickHandler(itemText);

				} else {

					let pageId  = (entity.type === 'Page') ? entity.id : entity.pageId;
					let tagName = itemText;

					Command.createAndAppendDOMNode(pageId, entity.id, tagName, _Dragndrop.getAdditionalDataForElementCreation(tagName), _Elements.isInheritVisibilityFlagsChecked(), _Elements.isInheritGranteesChecked());
				}

				_Elements.contextMenu.remove();
			});
		},
		getSuggestedWidgets: (entity, callback) => {

			if (!entity.isPage && !entity.isContent) {

				let classes = entity._html_class && entity._html_class.length ? entity._html_class.split(' ') : [];

				Command.getSuggestions(entity._html_id, entity.name, entity.tag, classes, (result) => {

					let data = (result ?? []).map(r => {
						return {
							id:           r.id,
							name:         r.name,
							source:       r.source,
							clickHandler: () => {
								Command.get(r.id, undefined, (result) => {
									_Widgets.insertWidgetIntoPage(result, entity, entity.pageId);
								});
							}
						}
					});

					callback(data);
				});
			}
		},
		updateMenuGroupVisibility: () => {

			let windowHeight = window.innerHeight;

			for (let groupSwitch of document.querySelectorAll('.element-group-switch')) {

				groupSwitch.addEventListener('mouseover', (e) => {

					let childrenMenu = groupSwitch.querySelector(':scope > .element-group');

					if (childrenMenu) {

						childrenMenu.classList.remove('hidden');

						let boundingRect = childrenMenu.getBoundingClientRect()

						let bottomOfMenu = boundingRect.y + boundingRect.height;
						if (bottomOfMenu > windowHeight) {
							childrenMenu.style.top = (-1 - (bottomOfMenu - windowHeight) - 12) + 'px';
						}
					}
				});

				groupSwitch.addEventListener('mouseout', (e) => {
					groupSwitch.querySelector(':scope > .element-group')?.classList.add('hidden');
				});
			}
		},
		repositionMenu: (x, y) => {

			let menu         = _Elements.contextMenu.getDOMNode();
			let menuWidth    = menu.clientWidth;
			let menuHeight   = menu.clientHeight;
			let windowWidth  = window.innerWidth;
			let windowHeight = window.innerHeight;

			if (windowWidth < (x + menuWidth)) {
				menu.style.left = (x - menuWidth) + 'px';
			}

			if (windowHeight < y + menuHeight) {
				menu.style.top = (windowHeight - menuHeight - 20) + 'px';
			}
		},
		remove: () => {

			_Helpers.fastRemoveElement(_Elements.contextMenu.getDOMNode());

			for (let active of document.querySelectorAll('.contextMenuActive')) {
				active.classList.remove('contextMenuActive');
			}
		},
	},
	isInheritVisibilityFlagsChecked: () => UISettings.getValueForSetting(UISettings.settingGroups.pages.settings.inheritVisibilityFlagsKey),
	isInheritGranteesChecked: () => UISettings.getValueForSetting(UISettings.settingGroups.pages.settings.inheritGranteesKey),
	selectEntity: (entity) => {

		_Elements.unselectEntity();

		_Elements.selectedEntity = entity;

		let node = Structr.node(_Elements.selectedEntity.id);
		if (node) {
			node.addClass('nodeSelectedFromContextMenu');
		}
	},
	unselectEntity: () => {

		if (_Elements.selectedEntity) {

			let node = Structr.node(_Elements.selectedEntity.id);
			if (node) {
				node.removeClass('nodeSelectedFromContextMenu');
			}

			_Elements.selectedEntity = undefined;
		}
	},
	isEntitySelected: (entity) => {
		return (_Elements.selectedEntity && _Elements.selectedEntity.id === entity.id);
	},
	appendContentElement: (entity, refNode, refNodeIsParent) => {

		let parent;

		if (entity.parent && entity.parent.id) {
			parent = Structr.node(entity.parent.id);

			if (parent && parent.length > 0 && !Structr.isExpanded(entity.parent.id)) {
				// if the parent element is not expanded, then we don't need to add the element we want to add,
				// we simply need to expand the parent
				_Entities.ensureExpanded(parent);
				return;
			}

		} else {
			parent = refNode;
		}

		if (!parent) {
			return false;
		}

		let isActiveNode          = entity.isActiveNode();
		let isTemplate            = (entity.type === 'Template');
		let name                  = entity.name;
		let displayName           = _Helpers.getElementDisplayName(entity);
		let nameText              = (name ? `<b title="${_Helpers.escapeForHtmlAttributes(displayName)}" class="tag_ name_">${displayName}</b>` : `<span class="content_">${_Helpers.escapeTags(entity.content) || '&nbsp;'}</span>`);
		let isRootSharedComponent = (entity.parent === null && entity.pageId === _Pages.shadowPage.id);
		let isRootTrashElement    = (entity.parent === null && entity.pageId === null);

		let html = `
			<div id="id_${entity.id}" class="node content ${(isActiveNode ? ' activeNode' : 'staticNode') + (_Elements.isEntitySelected(entity) ? ' nodeSelectedFromContextMenu' : '')}" draggable="true">

				${isRootSharedComponent || isRootTrashElement ? '' : '<div class="before-node"></div>'}

				<div class="node-container flex items-center">
					${_Icons.getSvgIconForContentNode(entity, ['typeIcon', _Icons.typeIconClassNoChildren])}
					<span class="abbr-ellipsis abbr-pages-tree">${nameText}</span>
					<div class="icons-container flex items-center"></div>
				</div>

				${isRootSharedComponent || isRootTrashElement ? '' : '<div class="after-node"></div>'}
			</div>
		`;

		if (refNode && !refNodeIsParent) {
			refNode.before(html);
		} else {
			let afterNode = parent.children('.after-node');
			if (afterNode.length) {
				parent.children('.after-node').before(html);
			} else {
				parent.append(html);
			}
		}

		let div            = Structr.node(entity.id);
		let nodeContainer  = $('.node-container', div);
		let iconsContainer = $('.icons-container', div);

		_Dragndrop.enableDraggable(entity, div[0], _Dragndrop.dropActions.domElement, true, _Dragndrop.pages.dragStart, _Dragndrop.pages.dragEnd);
		_Dragndrop.pages.enableDroppable(entity, div[0], nodeContainer[0]);

		if (isTemplate) {
			let hasChildren = entity.childrenIds && entity.childrenIds.length;
			_Entities.appendExpandIcon(nodeContainer, entity, hasChildren);
		}

		if (entity.hidden === true) {
			div.addClass('is-hidden');
		}

		_Elements.contextMenu.enableContextMenuOnElement(nodeContainer[0], entity);

		_Pages.registerDetailClickHandler(nodeContainer, entity);

		_Entities.setMouseOver(div, undefined, ((entity.syncedNodesIds && entity.syncedNodesIds.length) ? entity.syncedNodesIds : [entity.sharedComponentId]));

		_Entities.appendContextMenuIcon(iconsContainer, entity);
		_Entities.appendNewAccessControlIcon(iconsContainer, entity);

		_Elements.clickOrSelectElementIfLastSelected(div, entity);

		return div;
	},
	openEditContentDialog: (entity) => {

		let { dialogText, dialogMeta } = _Dialogs.custom.openDialog(`Edit content of ${entity?.name ?? entity.id}`, null, ['popup-dialog-with-editor']);

		dialogText.insertAdjacentHTML('beforeend', '<div class="editor h-full"></div>');
		dialogMeta.insertAdjacentHTML('beforeend', `<span class="editor-info"></span>`);

		let dialogSaveButton = _Dialogs.custom.updateOrCreateDialogSaveButton();
		let saveAndClose     = _Dialogs.custom.updateOrCreateDialogSaveAndCloseButton();
		let editorInfo       = dialogMeta.querySelector('.editor-info');
		_Editors.appendEditorOptionsElement(editorInfo);

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

					let disabled = (initialText === editor.getValue());
					_Helpers.disableElements(disabled, dialogSaveButton, saveAndClose);
				},
				saveFn: (editor, entity, close = false) => {

					let text1 = initialText || '';
					let text2 = editor.getValue();

					if (text1 === text2) {
						return;
					}

					Command.patch(entity.id, text1, text2, () => {

						_Dialogs.custom.showAndHideInfoBoxMessage('Content saved.', 'success', 2000, 200);
						_Helpers.disableElements(true, dialogSaveButton, saveAndClose);

						Command.getProperty(entity.id, 'content', (newText) => {
							initialText = newText;
						});

						if (close === true) {
							_Dialogs.custom.clickDialogCancelButton();
						}
					});
				}
			};

			let editor = _Editors.getMonacoEditor(entity, 'source', dialogText.querySelector('.editor'), openEditDialogMonacoConfig);

			Structr.resize();

			dialogSaveButton.addEventListener('click', (e) => {
				e.stopPropagation();

				openEditDialogMonacoConfig.saveFn(editor, entity);
			});

			saveAndClose.addEventListener('click', (e) => {
				e.stopPropagation();

				openEditDialogMonacoConfig.saveFn(editor, entity, true);
			});

			_Editors.resizeVisibleEditors();
		});
	},
	displayCentralEditor: (entity) => {

		let centerPane             = document.querySelector('#center-pane');
		let contentEditorContainer = _Helpers.createSingleDOMElementFromHTML(_Pages.templates.contentEditor());

		centerPane.appendChild(contentEditorContainer);

		Command.get(entity.id, 'content,contentType', (data) => {
			entity.contentType = data.contentType;

			let initialText = data.content;
			let buttonArea  = contentEditorContainer.querySelector('.editor-button-container');
			let infoArea    = contentEditorContainer.querySelector('.editor-info-container');
			let contentBox  = contentEditorContainer.querySelector('.editor');
			let saveButton  = _Helpers.createSingleDOMElementFromHTML('<button id="editorSave" disabled="disabled" class="disabled">Save</button>');

			buttonArea.appendChild(saveButton);

			const availableContentTypes = ['text/plain', 'text/html', 'text/xml', 'text/css', 'text/javascript', 'text/markdown', 'text/textile', 'text/mediawiki', 'text/tracwiki', 'text/confluence', 'text/asciidoc'];
			infoArea.insertAdjacentHTML('beforeend', `
				<label for="contentTypeSelect">Content-Type: </label>
				<select class="contentType_" id="contentTypeSelect">
					${availableContentTypes.map(type => `<option ${(type === entity.contentType ? 'selected' : '')} value="${type}">${type}</option>`).join('')}
				</select>
				<span class="editor-info"></span>
			`);

			let editorInfo = infoArea.querySelector('.editor-info');
			_Editors.appendEditorOptionsElement(editorInfo);

			let contentType = entity.contentType || 'text/plain';

			let centralEditorMonacoConfig = {
				value: initialText || '',
				language: contentType,
				lint: true,
				autocomplete: true,
				forceAllowAutoComplete: true,
				changeFn: (editor, entity) => {

					let disabled = (initialText === editor.getValue());
					_Helpers.disableElements(disabled, saveButton);
				},
				saveFn: (editor, entity) => {

					let text1 = initialText || '';
					let text2 = editor.getValue();

					if (text1 === text2) {
						return;
					}

					Command.patch(entity.id, text1, text2, () => {

						_Dialogs.custom.showAndHideInfoBoxMessage('Content saved.', 'success', 2000, 200);
						_Helpers.disableElements(true, saveButton);

						Command.getProperty(entity.id, 'content', (newText) => {
							initialText = newText;
						});
					});
				}
			};

			let editor = _Editors.getMonacoEditor(entity, 'content', contentBox, centralEditorMonacoConfig);

			Structr.resize();

			if (entity.isFavoritable) {

				let addToFavoritesButton = _Helpers.createSingleDOMElementFromHTML(_Icons.getSvgIcon(_Icons.iconAddToFavorites, 16, 16, ['add-to-favorites']));
				buttonArea.appendChild(addToFavoritesButton);

				addToFavoritesButton.addEventListener('click', (e) => {
					Command.favorites('add', entity.id, () => {
						_Helpers.blinkGreen(e.target.closest('.add-to-favorites'));
					});
				});
			}

			_Editors.enableSpeechToTextForEditor(editor, buttonArea);

			saveButton.addEventListener('click', (e) => {
				e.stopPropagation();

				centralEditorMonacoConfig.saveFn(editor, entity);
			});

			infoArea.querySelector('#contentTypeSelect').addEventListener('change', (e) => {

				contentType = e.target.value;

				entity.setProperty('contentType', contentType, false, () => {

					_Editors.updateMonacoEditorLanguage(editor, contentType, entity);

					_Helpers.blinkGreen(e.target);
				});
			});

			_Editors.focusEditor(editor);
		});
	}
};