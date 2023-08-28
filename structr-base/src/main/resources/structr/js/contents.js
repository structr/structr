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
document.addEventListener("DOMContentLoaded", () => {
	Structr.registerModule(_Contents);
});

let _Contents = {
	_moduleName: 'contents',
	searchField: undefined,
	contentsMain: undefined,
	contentTree: undefined,
	contentsContents: undefined,
	currentContentContainer: undefined,
	containerPageSize: 10000,
	containerPage: 1,
	currentContentContainerKey: 'structrCurrentContentContainer_' + location.port,
	contentsResizerLeftKey: 'structrContentsResizerLeftKey_' + location.port,
	selectedElements: [],

	init: () => {
		Structr.adaptUiToAvailableFeatures();
	},
	resize: () => {
		_Contents.moveResizer();
	},
	prevAnimFrameReqId_moveResizer: undefined,
	moveResizer: (left) => {

		_Helpers.requestAnimationFrameWrapper(_Contents.prevAnimFrameReqId_moveResizer, () => {

			left = left || LSWrapper.getItem(_Contents.contentsResizerLeftKey) || 300;
			$('.column-resizer', _Contents.contentsMain).css({left: left});

			_Contents.contentTree.css({width: left - 14 + 'px'});
		});
	},
	onload: () => {

		Structr.setMainContainerHTML(_Contents.templates.contents());

		_Contents.init();

		Structr.updateMainHelpLink(_Helpers.getDocumentationURLForTopic('contents'));

		_Contents.contentsMain     = $('#contents-main');
		_Contents.contentTree      = $('#contents-tree');
		_Contents.contentsContents = $('#contents-contents');

		_Contents.moveResizer();
		Structr.initVerticalSlider($('.column-resizer', _Contents.contentsMain), _Contents.contentsResizerLeftKey, 204, _Contents.moveResizer);

		let initFunctionBar = async () => {

			let contentContainerTypes = await _Schema.getDerivedTypes('org.structr.dynamic.ContentContainer', []);
			let contentItemTypes      = await _Schema.getDerivedTypes('org.structr.dynamic.ContentItem', []);

			Structr.setFunctionBarHTML(_Contents.templates.functions({ containerTypes: contentContainerTypes, itemTypes: contentItemTypes }));

			// UISettings.showSettingsForCurrentModule();

			let itemTypeSelect      = document.querySelector('select#content-item-type');
			let addItemButton       = document.getElementById('add-item-button');
			let containerTypeSelect = document.querySelector('select#content-container-type');
			let addContainerButton  = document.getElementById('add-container-button');

			addItemButton.addEventListener('click', () => {
				let containers = (_Contents.currentContentContainer ? [ { id : _Contents.currentContentContainer.id } ] : null);
				Command.create({ type: itemTypeSelect.value, containers: containers }, (f) => {
					_Contents.appendItemOrContainerRow(f);
				});
			});

			addContainerButton.addEventListener('click', () => {
				let parent = (_Contents.currentContentContainer ? _Contents.currentContentContainer.id : null);
				Command.create({ type: containerTypeSelect.value, parent: parent }, (f) => {
					_Contents.appendItemOrContainerRow(f);
					_Contents.refreshTree();
				});
			});

			itemTypeSelect.addEventListener('change', () => {
				addItemButton.querySelector('span').textContent = `Add ${itemTypeSelect.value}`;
			});

			containerTypeSelect.addEventListener('change', () => {
				addContainerButton.querySelector('span').textContent = `Add ${containerTypeSelect.value}`;
			});

			if (contentItemTypes.length === 0) {
				_Helpers.appendInfoTextToElement({
					text: "It is recommended to create a custom type extending <b>org.structr.web.entity.<u>ContentItem</u></b> to create ContentItems.<br><br>If only one type of ContentItem is required, custom attributes can be added to the type ContentItem in the schema.",
					element: $(itemTypeSelect).parent(),
					after: true,
					css: {
						marginLeft: '-.75rem',
						marginRight: '1rem'
					}
				});
			}
		};
		initFunctionBar(); // run async (do not await) so it can execute while jstree is initialized

		$.jstree.defaults.core.themes.dots      = false;
		$.jstree.defaults.dnd.inside_pos        = 'last';
		$.jstree.defaults.dnd.large_drop_target = true;

		_Contents.contentTree.on('ready.jstree', function() {

			_TreeHelper.makeAllTreeElementsDroppable(_Contents.contentTree, _Dragndrop.contents.enableTreeElementDroppable);

			_Contents.loadAndSetWorkingDir(function() {
				if (_Contents.currentContentContainer) {
					_Contents.deepOpen(_Contents.currentContentContainer);
				}
			});
		});

		_Contents.contentTree.on('select_node.jstree', function(evt, data) {

			_Contents.setCurrentContentContainer(data.node.id);
			_Contents.displayContainerContents(data.node.id, data.node.parent, data.node.original.path, data.node.parents);
		});

		_Contents.contentTree.on('after_open.jstree', (evt, data) => {
			_TreeHelper.makeAllTreeElementsDroppable(_Contents.contentTree, _Dragndrop.contents.enableTreeElementDroppable);
		});

		_Contents.contentTree.on('refresh_node.jstree', (evt, data) => {
			_TreeHelper.makeAllTreeElementsDroppable(_Contents.contentTree, _Dragndrop.contents.enableTreeElementDroppable);
		});

		_TreeHelper.initTree(_Contents.contentTree, _Contents.treeInitFunction, 'structr-ui-contents');

		Structr.mainMenu.unblock(100);

		Structr.resize();
	},
	handleNodeRefresh: (node) => {


		if (node.isContentContainer && node.name) {

			let folderContents = _Contents.contentsContents[0];

			// update breadcrumb
			if (folderContents.dataset['currentContainer'] === node.id) {
				folderContents.querySelector('#current-container-name')?.replaceChildren(node.name);
			}

			// update tree element
			let treeNode = _Contents.contentTree.jstree().get_node(node.id);

			if (treeNode) {
				_Contents.contentTree.jstree().get_node(node.id).text = _Contents.getContainerDisplayName(node);
				_Contents.contentTree.jstree().refresh_node(node.id);
			}
		}
	},
	getContextMenuElements: (div, entity) => {

		let elements             = [];
		const isContentContainer = entity.isContentContainer;
		const isContentItem      = entity.isContentItem;
		let selectedElements     = document.querySelectorAll('.node.selected');

		// there is a difference when right-clicking versus clicking the kebab icon
		let contentNode = div;
		if (contentNode.classList.contains('icons-container')) {
			contentNode = div.closest('.node');
		} else if (!contentNode.classList.contains('node')) {
			contentNode = div.querySelector('.node');
		}

		if (contentNode && !contentNode.classList.contains('selected')) {

			for (let selNode of document.querySelectorAll('.node.selected')) {
				selNode.classList.remove('selected');
			}
			contentNode.classList.add('selected');

			selectedElements = document.querySelectorAll('.node.selected');
		}

		let isMultiSelect = selectedElements.length > 1;

		if (isContentItem && isMultiSelect === false) {

			elements.push({
				icon: _Icons.getMenuSvgIcon(_Icons.iconPencilEdit),
				name: 'Edit',
				clickHandler: () => {
					_Contents.editItem(entity);
				}
			});
		}

		// TODO: Inheriting types do not have containers as UI-view attribute
		if (contentNode) {
			let containers = entity?.containers ?? [entity.parent];
			if (_Contents.currentContentContainer?.id && containers.length > 0) {

				elements.push({
					icon: _Icons.getMenuSvgIcon(_Icons.iconFolderRemove),
					name: 'Remove from container',
					clickHandler: () => {

						let removePromises = [...selectedElements].map(el => new Promise((resolve, reject) => {

							let node = StructrModel.obj(Structr.getId(el));

							if (node.isContentContainer) {
								_Entities.setProperty(node.id, 'parent', null, false, resolve);
							} else {
								_Entities.setProperty(node.id, 'containers', containers.filter(c => c.id !== _Contents.currentContentContainer.id), false, resolve);
							}
						}));

						Promise.all(removePromises).then(values => {
							_Contents.refreshTree();
						});
					}
				});
			}
		}

		elements.push({
			name: 'Properties',
			clickHandler: () => {
				_Entities.showProperties(entity, 'ui');
			}
		});

		if (!isMultiSelect) {

			_Elements.contextMenu.appendContextMenuSeparator(elements);

			_Elements.contextMenu.appendSecurityContextMenuItems(elements, entity);
		}

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		if (contentNode) {

			elements.push({
				icon: _Icons.getMenuSvgIcon(_Icons.iconTrashcan),
				classes: ['menu-bolder', 'danger'],
				name: 'Delete ' + (isMultiSelect ? 'selected' : entity.type),
				clickHandler: () => {

					let nodesToDelete = [...selectedElements].map(el => Structr.entityFromElement(el));

					// [recursive=false] because deleting recursively does not make sense and can not work because it is a n:m relationship
					_Entities.deleteNodes(nodesToDelete, false, () => {
						_Contents.refreshTree();
					});
				}
			});
		}

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		return elements;
	},
	deepOpen: (d, dirs) => {

		_TreeHelper.deepOpen(_Contents.contentTree, d, dirs, 'parent', (_Contents.currentContentContainer ? _Contents.currentContentContainer.id : 'root'));
	},
	refreshTree: () => {

		_TreeHelper.refreshTree(_Contents.contentTree, () =>{
		});
	},
	treeInitFunction: (obj, callback) => {

		switch (obj.id) {

			case '#':

				let defaultEntries = [{
					id: 'root',
					text: '/',
					children: true,
					icon: _Icons.nonExistentEmptyIcon,
					data: {
						svgIcon: _Icons.getSvgIcon(_Icons.iconStructrSSmall, 18, 24)
					},
					path: '/',
					state: {
						opened: true,
						selected: true
					}
				}];

				callback(defaultEntries);

				break;

			case 'root':
				_Contents.load(null, callback);
				break;

			default:
				_Contents.load(obj.id, callback);
				break;
		}
	},
	unload: () => {
	},
	fulltextSearch: (searchString) => {

		_Contents.contentsContents.children().hide();

		_Contents.displaySearchResultsForURL(`${Structr.rootUrl}ContentItem/ui?${Structr.getRequestParameterName('loose')}=1${searchString.split(' ').map((str) => '&name=' + str)}`);
	},
	clearSearch: () => {
		Structr.mainContainer.querySelector('.search').value = '';
		$('#search-results').remove();
		_Contents.contentsContents.children().show();
	},
	loadAndSetWorkingDir: (callback) => {

		_Contents.currentContentContainer = LSWrapper.getItem(_Contents.currentContentContainerKey);
		callback();
	},
	load: (id, callback) => {

		let filter = {
			parent: (id ? id : null)
		};

		let customView = 'id,name,items,isContentContainer,childContainers,path,parent';
		Command.queryPromise('ContentContainer', _Contents.containerPageSize, _Contents.containerPage, 'position', 'asc', filter, true, null, customView).then(containers => {

			return containers.map(d => {

				StructrModel.createOrUpdateFromData(d, null, false);

				return {
					id:       d.id,
					text:     _Contents.getContainerDisplayName(d),
					children: d.isContentContainer && d.childContainers.length > 0,
					icon:     _Icons.nonExistentEmptyIcon,
					data:     { svgIcon: _Icons.getSvgIcon(_Icons.iconFolderClosed, 16, 24) },
					path:     d.path
				};
			});

			return list;

		}).then(callback).catch(e => {
			// silently ignore - this is usually because the tree is being refreshed as a whole while we want to refresh the node
		});
	},
	getContainerDisplayName: (d) => {
		return (d?.name ?? '[unnamed]') + ((d.items && d.items.length > 0) ? ` (${d.items.length})` : '');
	},
	setCurrentContentContainer: (id) => {

		if (id === 'root') {
			_Contents.currentContentContainer = null;
		} else {
			_Contents.currentContentContainer = { id: id };
		}

		LSWrapper.setItem(_Contents.currentContentContainerKey, _Contents.currentContentContainer);
	},
	displayContainerContents: (id, parentId, nodePath, parents) => {

		_Helpers.fastRemoveAllChildren(_Contents.contentsContents[0]);

		let isRootFolder = (id === 'root');
		let parentIsRoot = (parentId === '#');

		// store current folder id so we can filter slow requests
		_Contents.contentsContents[0].dataset['currentContainer'] = id;

		let handleChildren = (children) => {

			let currentFolder = _Contents.contentsContents[0].dataset['currentContainer'];

			if (currentFolder === id) {

				children.map(_Contents.appendItemOrContainerRow);

				Structr.resize();
			}
		};

		let handleContainerChildren = (containers) => {

			handleChildren(containers);

			_Contents.registerFolderLinks();
		};

		Command.query('ContentContainer', 1000, 1, 'position', 'asc', { parent: (isRootFolder ? null : id ) }, handleContainerChildren, true, 'ui');

		_Pager.initPager('contents-items', 'ContentItem', 1, 25, 'position', 'asc');
		_Pager.page['ContentItem'] = 1;
		let filterOptions = {
			containers: [],
		};
		if (!isRootFolder) {
			filterOptions.containers = [id];
		}
		_Pager.initFilters('contents-items', 'ContentItem', filterOptions);

		let itemsPager = _Pager.addPager('contents-items', _Contents.contentsContents[0], false, 'ContentItem', 'ui', handleChildren, undefined, undefined, undefined, true);

		itemsPager.cleanupFunction = () => {
			let toRemove = itemsPager.el.querySelectorAll('.node.item');
			for (let item of toRemove) {
				_Helpers.fastRemoveElement(item.closest('tr'));
			}
		};

		itemsPager.appendFilterElements(`
			<span class="mr-1">Filter:</span>
			<input type="text" class="filter" data-attribute="name">
			<input type="text" class="filter" data-attribute="containers" value="${(parentIsRoot ? '' : id)}" hidden>
		`);
		itemsPager.activateFilterElements();
		itemsPager.setIsPaused(false);
		itemsPager.refresh();

		_Contents.insertBreadCrumbNavigation(parents, nodePath, id);

		_Contents.contentsContents.append(`
			<table id="files-table" class="stripe">
				<thead><tr><th class="icon">&nbsp;</th><th>Name</th><th>Size</th><th>Type</th><th>Owner</th><th>Modified</th></tr></thead>
				<tbody id="files-table-body">
					${(isRootFolder ? '' : `
						<tr id="parent-container-link">
							<td class="is-folder file-icon" data-target-id="${parentId}">${_Icons.getSvgIcon(_Icons.iconFolderClosed, 16, 16)}</td>
							<td>
								<div class="node folder flex items-center justify-between">
									<b class="name_ leading-8 truncate">..</b>
								</div>
							</td>
							<td></td>
							<td></td>
							<td></td>
							<td></td>
						</tr>`
					)}
				</tbody>
			</table>
		`);

		if (!isRootFolder) {

			// allow drop on ".." element
			let parentObj = (parentId === 'root') ? { id: parentId, type: 'fake' } : StructrModel.obj(parentId);

			_Dragndrop.contents.enableDroppable(parentObj, _Contents.contentsContents[0].querySelector('#parent-container-link .node'));
		}
	},
	registerFolderLinks: () => {

		let openTargetNode = (targetId) => {
			_Contents.contentTree.jstree('open_node', targetId, () => {
				_Contents.contentTree.jstree('activate_node', targetId);
			});
		};

		for (let folderLink of _Contents.contentsContents[0].querySelectorAll('.is-folder.file-icon')) {

			folderLink.addEventListener('click', (e) => {
				e.preventDefault();
				e.stopPropagation();

				let targetId = folderLink.dataset['targetId'];
				let parentId = folderLink.dataset['parentId'];

				if (!parentId || _Contents.contentTree.jstree('is_open', parentId)) {
					openTargetNode(targetId);
				} else {
					_Contents.contentTree.jstree('open_node', parentId, openTargetNode);
				}
			});
		}
	},
	insertBreadCrumbNavigation: (parents, nodePath, id) => {

		if (parents) {

			let preventOldFolderNameInBreadcrumbs = () => {
				let modelObj = StructrModel.obj(id);
				if (modelObj && modelObj.path) {
					nodePath = modelObj.path;
				}
			};
			preventOldFolderNameInBreadcrumbs();

			parents = [].concat(parents).reverse().slice(1);

			let pathNames = (nodePath === '/') ? ['/'] : [''].concat(nodePath.slice(1).split('/'));

			_Contents.contentsContents.append(`
				<div class="folder-path">
					${parents.map((parent, idx) => `<a class="breadcrumb-entry" data-folder-id="${parent}">${pathNames[idx]}/</a>`).join('')}<span id="current-container-name">${pathNames.pop()}</span>
					<span class="context-menu-container"></span>
				</div>
			`);

			if (id != 'root') {

				Command.getPromise(id, null).then(obj => {
					let ctxMenuContainer = _Contents.contentsContents[0].querySelector('.context-menu-container');
					_Entities.appendContextMenuIcon($(ctxMenuContainer), obj, true);
				});
			}

			$('.breadcrumb-entry').click(function (e) {
				e.preventDefault();

				$('#' + $(this).data('folderId') + '_anchor').click();
			});
		}
	},
	appendItemOrContainerRow: (d) => {

		// add container/item to global model
		StructrModel.createFromData(d, null, false);

		let tableBody  = $('#files-table-body');

		$(`#row${d.id}`, tableBody).remove();

		let items      = d.items || [];
		let containers = d.containers || [];
		let size       = d.isContentContainer ? containers.length + items.length : (d?.size ?? '-');

		let rowId = `row${d.id}`;
		tableBody.append(`<tr id="${rowId}"${d.isThumbnail ? ' class="thumbnail"' : ''}></tr>`);

		let row   = $(`#${rowId}`);
		let title = (d?.name ?? '[unnamed]');
		let icon  = (d.isContentContainer ? _Icons.iconFolderClosed : _Icons.iconFileTypeEmpty);

		row.append(`
			<td class="${d.isContentContainer ? 'is-folder' : ''} file-icon" data-target-id="${d.id}">${_Icons.getSvgIcon(icon, 16, 16)}</td>
			<td>
				<div id="id_${d.id}" data-structr_type="${(d.isContentContainer ? 'folder' : 'item')}" class="node ${(d.isContentContainer ? 'container' : 'item')} flex items-center justify-between" draggable="true">
					<b title="${_Helpers.escapeForHtmlAttributes(title)}" class="name_ leading-8 truncate">${title}</b>
					<div class="icons-container flex items-center"></div>
				</div>
			</td>
			<td>${size}</td>
			<td class="truncate">${d.type}</td>
			<td class="truncate">${(d.owner ? (d.owner?.name ?? '[unnamed]') : '')}</td>
			<td class="truncate">${moment(d.lastModifiedDate).calendar()}</td>
		`);

		// Change working dir by click on folder icon
		$(`#id_${d.id}.container`).parent().prev().on('click', function(e) {

			e.preventDefault();
			e.stopPropagation();

			if (d.parentId) {

				_Contents.contentTree.jstree('open_node', $(`#${d.parentId}`), () => {

					if (d.name === '..') {
						$(`#${d.parentId}_anchor`).click();
					} else {
						$(`#${d.id}_anchor`).click();
					}
				});

			} else {

				$(`#${d.id}_anchor`).click();
			}

			return false;
		});

		let div            = Structr.node(d.id);
		let iconsContainer = $('.icons-container', div);

		if (!div || !div.length)
			return;

		div.children('b.name_').off('click').on('click', function(e) {
			e.stopPropagation();
			_Entities.makeNameEditable(div);
		});

		// even though it can be a file, enable droppable so we can react
		_Dragndrop.contents.enableDroppable(d, div[0]);

		_Dragndrop.enableDraggable(d, div[0], _Dragndrop.dropActions.contents, false, (e) => {

			let draggedElementIsSelected = div.hasClass('selected');

			if (!draggedElementIsSelected) {
				$('.node.selected').removeClass('selected');
			}

			_Contents.selectedElements = draggedElementIsSelected ? $('.node.selected') : [];

			_Dragndrop.contents.draggedEntityIds = draggedElementIsSelected ? [...document.querySelectorAll('.node.selected')].map(x => Structr.getId(x)) : [d.id];

			let dragIcon       = (draggedElementIsSelected && _Dragndrop.contents.draggedEntityIds.length > 1) ? _Icons.iconFilesStack : icon;
			let dragCnt        = (draggedElementIsSelected && _Dragndrop.contents.draggedEntityIds.length > 1) ? _Dragndrop.contents.draggedEntityIds.length : _Dragndrop.dragEntity.name;
			let dragIconHTML   = _Icons.getSvgIcon(dragIcon, 20, 20, 'mr-1');
			let dragImgElement = _Helpers.createSingleDOMElementFromHTML(`<span class="bg-white inline-flex items-center p-1">${dragIconHTML}<div>${dragCnt}</div></span>`);

			// element must be in document to be displayed...
			Structr.mainContainer.appendChild(dragImgElement);

			let rect = dragImgElement.getBoundingClientRect();

			e.dataTransfer.setDragImage(dragImgElement, rect.width, rect.height);

			// ...remove it in the next animation frame
			requestAnimationFrame(() => {
				dragImgElement.remove();
			});
		}, () => {

			delete _Dragndrop.contents.draggedEntityIds;
		});

		if (!d.isContentContainer) {

			_Contents.appendEditContentItemIcon(iconsContainer, d);

			let dblclickHandler = (e) => {
				_Contents.editItem(d);
			};

			if (div) {
				let node = div[0].closest('.node');
				node.removeEventListener('dblclick', dblclickHandler);
				node.addEventListener('dblclick', dblclickHandler);
			}
		}

		_Entities.appendContextMenuIcon(iconsContainer, d);
		_Entities.appendNewAccessControlIcon(iconsContainer, d);
		_Entities.makeSelectable(div);
		_Elements.contextMenu.enableContextMenuOnElement(div[0], d);
	},
	handleMoveObjectsAction: (targetContainerId, draggedObjectIds) => {

		/**
		 * handles a drop action to a ContentContainer located in the contents area...
		 * and a drop action to a ContentContainer located in the tree
		 *
		 * must take into account the selected elements in the contents area
		 */
		_Contents.moveObjectsToTargetContainer(targetContainerId, draggedObjectIds).then((info) => {

			if (info.skipped.length > 0) {
				let text = `The following containers were not moved to prevent inconsistencies:<br><br>${info.skipped.map(id => StructrModel.obj(id)?.name ?? 'unknown').join('<br>')}`;
				new InfoMessage().title('Skipped some objects').text(text).show();
			}

			// only reload whole tree if we moved a container (the name and count is changed via the model)
			if (info.movedContentContainer) {

				_Contents.refreshTree();
			}
		});
	},
	moveObjectsToTargetContainer: async (targetContainerId, objectIds) => {

		/**
		 * initial promise.all returns a list of moved object ids
		 * skipped objects have the "skipped_" prefix before the uuid
		 * result of the promise is an object telling us the skipped objects, the moved objects, and if there was a folder which has been moved
		 */
		let skippedPrefix = 'skipped_';
		let movePromises = objectIds.map(objectId => new Promise((resolve, reject) => {

			// prevent moving element to self
			if (objectId !== targetContainerId) {

				let nodeToMove = StructrModel.obj(objectId);

				if (nodeToMove.isContentContainer) {
					_Entities.setProperty(nodeToMove.id, 'parent', targetContainerId, false, () => {
						resolve(objectId);
					});
				} else {
					_Entities.addToCollection(nodeToMove.id, targetContainerId, 'containers', () => {
						resolve(objectId);
					});
				}

			} else {
				resolve(skippedPrefix + objectId);
			}
		}));

		return Promise.all(movePromises).then(values => {

			let movedObjectIds = values.filter(v => !v.startsWith(skippedPrefix));

			return {
				movedContentContainer: movedObjectIds.map(id => StructrModel.obj(id)).some(obj => obj?.type === 'ContentContainer'),
				moved: movedObjectIds,
				skipped: values.filter(v => v.startsWith(skippedPrefix)).map(text => text.slice(skippedPrefix.length))
			};
		});
	},
	checkValueHasChanged: (oldVal, newVal, buttons) => {

		_Helpers.disableElements((newVal === oldVal), ...buttons);
	},
	editItem: (item) => {

		let { dialogText } = _Dialogs.custom.openDialog(`Edit ${item.name}`);

		Command.get(item.id, null, (entity) => {

			let saveAndCloseButtonLocal = _Dialogs.custom.updateOrCreateDialogSaveAndCloseButton(['action']);
			let saveButtonLocal         = _Dialogs.custom.updateOrCreateDialogSaveButton(['action']);
			let refreshButton           = _Dialogs.custom.appendCustomDialogButton('<button id="refresh" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Refresh</button>');

			_Entities.getSchemaProperties(entity.type, 'custom', (properties) => {

				let props = Object.values(properties);
				let nonCypherProperties = props.filter(prop => prop.className !== 'org.structr.core.property.CypherQueryProperty');

				for (let prop of nonCypherProperties) {

					let isRelated    = 'relatedType' in prop;
					let key          = prop.jsonName;
					let isCollection = prop.isCollection || false;
					let isReadOnly   = prop.readOnly     || false;
					let isSystem     = prop.system       || false;
					let oldVal       = entity[key];

					let cntr = _Helpers.createSingleDOMElementFromHTML(`<div id="prop-${key}" class="prop"><label for="${key}"><h3>${_Helpers.formatKey(key)}</h3></label></div>`);
					dialogText.appendChild(cntr);

					let div = $(cntr);

					if (prop.type === 'Boolean') {

						div.removeClass('value').append(`<div class="value-container"><input type="checkbox" class="${key}_"></div>`);
						let checkbox = div.find(`input[type="checkbox"].${key}_`);

						Command.getProperty(entity.id, key, (val) => {

							if (val) {
								checkbox.prop('checked', true);
							}

							if ((!isReadOnly || StructrWS.isAdmin) && !isSystem) {

								checkbox.on('change', function() {
									let checked = checkbox.prop('checked');
									_Contents.checkValueHasChanged(oldVal, checked || false, [saveButtonLocal, saveAndCloseButtonLocal]);
								});

							} else {

								checkbox.prop('disabled', 'disabled');
								checkbox.addClass('readOnly');
								checkbox.addClass('disabled');
							}
						});

					} else if (prop.type === 'Date' && !isReadOnly) {

						div.append('<div class="value-container"></div>');
						let valueInput = _Entities.appendDatePicker($('.value-container', div), entity, key, prop.format || "yyyy-MM-dd'T'HH:mm:ssZ");

						valueInput.on('change', function(e) {
							if (e.keyCode !== 27) {
								Command.get(entity.id, key, (newEntity) => {
									_Contents.checkValueHasChanged(newEntity[key], valueInput.val() || null, [saveButtonLocal, saveAndCloseButtonLocal]);
								});
							}
						});

					} else if (isRelated) {

						let relatedNodesList = $(`
							<div class="value-container related-nodes">
								${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['add', 'icon-green', 'cursor-pointer']))}
							</div>
						`);
						div.append(relatedNodesList);

						$(relatedNodesList).children('.add').on('click', function() {

							let { dialogText } = _Dialogs.custom.openDialog(`Add ${prop.type}`, () => {
								_Contents.editItem(item);
							});

							_Entities.displaySearch(entity.id, key, prop.type, $(dialogText), isCollection);
						});

						if (entity[key]) {

							let relatedNodes = $('.related-nodes', div);

							if (!isCollection) {

								let nodeId = entity[key].id || entity[key];

								Command.get(nodeId, 'id,type,tag,isContent,content,name', (node) => {

									_Entities.appendRelatedNode(relatedNodes, node, (nodeEl) => {

										$('.remove', nodeEl).on('click', function(e) {
											e.preventDefault();

											_Entities.setProperty(entity.id, key, null, false, (newVal) => {

												if (!newVal) {

													_Helpers.blinkGreen(relatedNodes);
													_Dialogs.custom.showAndHideInfoBoxMessage(`Related node "${node.name || node.id}" was removed from property "${key}".`, 'success', 2000, 1000);
													nodeEl.remove();

												} else {

													_Helpers.blinkRed(relatedNodes);
												}
											});

											return false;
										});
									});
								});

							} else {

								entity[key].forEach(function(obj) {

									let nodeId = obj.id || obj;

									Command.get(nodeId, 'id,type,tag,isContent,content,name', (node) => {

										_Entities.appendRelatedNode(relatedNodes, node, (nodeEl) => {

											$('.remove', nodeEl).on('click', function(e) {
												e.preventDefault();

												Command.removeFromCollection(entity.id, key, node.id, function() {
													let nodeEl = $('._' + node.id, relatedNodes);
													nodeEl.remove();
													_Helpers.blinkGreen(relatedNodes);
													_Dialogs.custom.showAndHideInfoBoxMessage(`Related node "${node.name || node.id}" was removed from property "${key}".`, 'success', 2000, 1000);
												});
												return false;
											});
										});
									});
								});
							}
						}

					} else {

						if (prop.contentType && prop.contentType === 'text/html') {

							div.append(`<div class="value-container edit-area">${oldVal || ''}</div>`);
							let editArea = $('.edit-area', div);

							editArea.trumbowyg({
								//btns: ['strong', 'em', '|', 'insertImage'],
								//autogrow: true
							}).on('tbwchange', function() {
								Command.get(entity.id, key, (newEntity) => {
									_Contents.checkValueHasChanged(newEntity[key], editArea.trumbowyg('html') || null, [saveButtonLocal, saveAndCloseButtonLocal]);
								});
							}).on('tbwpaste', function() {
								Command.get(entity.id, key, (newEntity) => {
									_Contents.checkValueHasChanged(newEntity[key], editArea.trumbowyg('html') || null, [saveButtonLocal, saveAndCloseButtonLocal]);
								});
							});

						} else {

							div.append('<div class="value-container"></div>');
							let valueContainer = $('.value-container', div);
							let valueInput;

							valueContainer.append(_Helpers.formatValueInputField(key, oldVal, false, prop.readOnly, prop.format === 'multi-line'));
							valueInput = valueContainer.find(`[name=${key}]`);

							valueInput.on('keyup', function(e) {
								if (e.keyCode !== 27) {
									Command.get(entity.id, key, (newEntity) => {
										_Contents.checkValueHasChanged(newEntity[key], valueInput.val() || null, [saveButtonLocal, saveAndCloseButtonLocal]);
									});
								}
							});
						}
					}
				}

			}, true);

			saveButtonLocal.addEventListener('click', (e) => {

				Structr.ignoreKeyUp = false;

				e.preventDefault();
				e.stopPropagation();

				_Entities.getSchemaProperties(entity.type, 'custom', (properties) => {

					let props = Object.values(properties);
					let nonCypherProperties = props.filter(prop => prop.className !== 'org.structr.core.property.CypherQueryProperty');

					for (let prop of nonCypherProperties) {

						let key = prop.jsonName;
						let newVal;
						let oldVal = entity[key];

						if (prop.contentType && prop.contentType === 'text/html') {
							newVal = $(`#prop-${key} .edit-area`).trumbowyg('html') || null;
						} else if (prop.propertyType === 'Boolean') {
							newVal = $(`#prop-${key} .value-container input`).prop('checked') || false;
						} else {
							if (prop.format === 'multi-line') {
								newVal = $(`#prop-${key} .value-container textarea`).val() || null;
							} else {
								newVal = $(`#prop-${key} .value-container input`).val() || null;
							}
						}

						if (!prop.relatedType && newVal !== oldVal) {

							Command.setProperty(entity.id, key, newVal, false, () => {

								oldVal = newVal;

								_Helpers.disableElements(true, saveButtonLocal, saveAndCloseButtonLocal);

								// update title in list
								if (key === 'title') {
									let f = $(`#row${entity.id} .item-title b`);
									f.text(newVal);
									_Helpers.blinkGreen(f);
								}
							});
						}
					}

					window.setTimeout(() => {
						refreshButton.click();
					}, 500);

				}, true);

				_Helpers.disableElements(true, saveButtonLocal, saveAndCloseButtonLocal);
			});

			saveAndCloseButtonLocal.addEventListener('click', (e) => {
				e.stopPropagation();
				saveButtonLocal.click();
				window.setTimeout(() => {
					_Dialogs.custom.clickDialogCancelButton();
				}, 1000);
			});

			refreshButton.addEventListener('click', (e) => {
				e.stopPropagation();
				_Contents.editItem(item);
			});

		}, 'all');
	},
	displaySearchResultsForURL: (url) => {

		$('#search-results').remove();
		_Contents.contentsContents.append('<div id="search-results"></div>');

		let searchString = Structr.functionBar.querySelector('.search').value;
		let container    = $('#search-results');
		_Contents.contentsContents.on('scroll', () => {
			window.history.pushState('', '', '#contents');
		});

		fetch(url).then(async response => {

			let data = await response.json();

			if (response.ok) {

				if (!data.result || data.result.length === 0) {

					container.append(`
						<h1>No results for "${searchString}"</h1>
						<h2>Press ESC or click <a href="#contents" class="clear-results">here to clear</a> empty result list.</h2>
					`);

					$('.clear-results', container).on('click', _Contents.clearSearch);

				} else {

					container.append(`
						<h1>${data.result.length} search results:</h1>

						<table class="props">
							<thead>
								<th class="_type">Type</th>
								<th>Name</th>
								<!--th>Size</th-->
							</thead>
							<tbody>
								${data.result.map(d => `
									<tr>
										<td>${_Icons.getSvgIcon((d.isContentContainer ? _Icons.iconFolderClosed : _Icons.iconFileTypeEmpty), 16, 16)} ${d.type}${(d.isFile && d.contentType ? ` (${d.contentType})` : '')}</td>
										<td><a href="#results${d.id}">${d.name}</a></td>
										<!--td>${d.size}</td-->
									</tr>
								`).join('')}
							</tbody>
						</table>
					`);
				}
			}
		});
	},
	appendEditContentItemIcon: (parent, d) => {

		let iconClass = 'svg_edit_item_icon';

		let icon = $('.' + iconClass, parent);
		if (!(icon && icon.length)) {

			icon = $(_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, _Icons.getSvgIconClassesNonColorIcon([iconClass, 'node-action-icon'])));
			parent.append(icon);

			icon.on('click', (e) => {
				e.stopPropagation();
				_Contents.editItem(d);
			});
		}

		return icon;
	},

	templates: {
		contents: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/contents.css">

			<div class="tree-main" id="contents-main">
				<div class="column-resizer"></div>

				<div class="tree-container" id="content-tree-container">
					<div class="tree" id="contents-tree">

					</div>
				</div>

				<div class="tree-contents-container" id="contents-contents-container">
					<div class="tree-contents tree-contents-with-top-buttons" id="contents-contents">

					</div>
				</div>
			</div>
		`,
		functions: config => `
			<div id="contents-action-buttons" class="flex-grow">

				<div class="inline-flex">

					<select class="select-create-type mr-2 hover:bg-gray-100 focus:border-gray-666 active:border-green" id="content-container-type">
						<option value="ContentContainer">Content Container</option>
						${config.containerTypes.map(type => `<option value="${type}">${type}</option>`).join('')}
					</select>

					<button class="action add_container_icon button inline-flex items-center" id="add-container-button">
						${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['mr-2'])} <span>Add Content Container</span>
					</button>
				</button>

					<select class="select-create-type mr-2 hover:bg-gray-100 focus:border-gray-666 active:border-green" id="content-item-type">
						<option value="ContentItem">Content Item</option>
						${config.itemTypes.map(type => `<option value="${type}">${type}</option>`).join('')}
					</select>

					<button class="action add_item_icon button inline-flex items-center" id="add-item-button">
						${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['mr-2'])} <span>Add Content Item</span>
					</button>
				</div>
			</div>
		`,
	}
};