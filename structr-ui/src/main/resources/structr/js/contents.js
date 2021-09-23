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
var main;
var selectedElements = [];

$(document).ready(function() {
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

	init: function() {
		Structr.makePagesMenuDroppable();
		Structr.adaptUiToAvailableFeatures();
	},
	resize: function() {
		_Contents.moveResizer();
		Structr.resize();
	},
	moveResizer: function(left) {
		left = left || LSWrapper.getItem(_Contents.contentsResizerLeftKey) || 300;
		$('.column-resizer', _Contents.contentsMain).css({ left: left });

		_Contents.contentTree.css({width: left - 14 + 'px'});
	},
	onload: function() {

		Structr.fetchHtmlTemplate('contents/contents', {}, async (html) => {

			main[0].innerHTML = html;

			_Contents.init();

			Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('contents'));

			_Contents.contentsMain     = $('#contents-main');
			_Contents.contentTree      = $('#contents-tree');
			_Contents.contentsContents = $('#contents-contents');

			_Contents.moveResizer();
			Structr.initVerticalSlider($('.column-resizer', _Contents.contentsMain), _Contents.contentsResizerLeftKey, 204, _Contents.moveResizer);

			let contentContainerTypes = await _Schema.getDerivedTypes('org.structr.dynamic.ContentContainer', []);
			let contentItemTypes      = await _Schema.getDerivedTypes('org.structr.dynamic.ContentItem', []);

			Structr.fetchHtmlTemplate('contents/functions', { containerTypes: contentContainerTypes, itemTypes: contentItemTypes }, async (html) => {

				Structr.functionBar.innerHTML = html;

				UISettings.showSettingsForCurrentModule();

				let itemTypeSelect      = document.querySelector('select#content-item-type');
				let addItemButton       = document.getElementById('add-item-button');
				let containerTypeSelect = document.querySelector('select#content-container-type');
				let addContainerButton  = document.getElementById('add-container-button');

				addItemButton.addEventListener('click', () => {
					let containers = (_Contents.currentContentContainer ? [ { id : _Contents.currentContentContainer.id } ] : null);
					Command.create({ type: itemTypeSelect.value, size: 0, containers: containers }, function(f) {
						_Contents.appendItemOrContainerRow(f);
						_Contents.refreshTree();
					});
				});

				addContainerButton.addEventListener('click', () => {
					let parent = (_Contents.currentContentContainer ? _Contents.currentContentContainer.id : null);
					Command.create({ type: containerTypeSelect.value, parent: parent }, function(f) {
						_Contents.appendItemOrContainerRow(f);
						_Contents.refreshTree();
					});
				});

				itemTypeSelect.addEventListener('change', () => {
					addItemButton.querySelector('span').textContent = 'Add ' + itemTypeSelect.value;
				});

				containerTypeSelect.addEventListener('change', () => {
					addContainerButton.querySelector('span').textContent = 'Add ' + containerTypeSelect.value;
				});

				if (contentItemTypes.length === 0) {
					Structr.appendInfoTextToElement({
						text: "It is recommended to create a custom type extending <b>org.structr.web.entity.<u>ContentItem</u></b> to create ContentItems.<br><br>If only one type of ContentItem is required, custom attributes can be added to the type ContentItem in the schema.",
						element: $(itemTypeSelect).parent(),
						after: true,
						css: { marginLeft: '-1rem', marginRight: '1rem' }
					});
				}

				// if (contentContainerTypes.length === 0) {
				// 	Structr.appendInfoTextToElement({
				// 		text: "You need to create a custom type extending <b>org.structr.web.entity.<u>ContentContainer</u></b> to add ContentContainers",
				// 		element: $(containerTypeSelect).parent(),
				// 		after: true,
				// 		css: { marginLeft: '-1rem', marginRight: '1rem' }
				// 	});
				// }

				// _Contents.searchField = $('.search', $(functionBar));
				// _Contents.searchField.focus();
				//
				// _Contents.searchField.keyup(function(e) {
				//
				// 	var searchString = $(this).val();
				// 	if (searchString && searchString.length && e.keyCode === 13) {
				//
				// 		$('.clearSearchIcon').show().on('click', function() {
				// 			_Contents.clearSearch();
				// 		});
				//
				// 		_Contents.fulltextSearch(searchString);
				//
				// 	} else if (e.keyCode === 27 || searchString === '') {
				// 		_Contents.clearSearch();
				// 	}
				// });
			});

			$.jstree.defaults.core.themes.dots      = false;
			$.jstree.defaults.dnd.inside_pos        = 'last';
			$.jstree.defaults.dnd.large_drop_target = true;

			_Contents.contentTree.on('ready.jstree', function() {
				_TreeHelper.makeTreeElementDroppable(_Contents.contentTree, 'root');

				_Contents.loadAndSetWorkingDir(function() {
					if (_Contents.currentContentContainer) {
						_Contents.deepOpen(_Contents.currentContentContainer);
					}
				});
			});

			_Contents.contentTree.on('select_node.jstree', function(evt, data) {

				_Contents.setWorkingDirectory(data.node.id);
				_Contents.displayContainerContents(data.node.id, data.node.parent, data.node.original.path, data.node.parents);
			});

			_TreeHelper.initTree(_Contents.contentTree, _Contents.treeInitFunction, 'structr-ui-contents');

			$(window).off('resize').resize(function() {
				_Contents.resize();
			});

			Structr.unblockMenu(100);

			_Contents.resize();
		});
	},
	getContextMenuElements: function (div, entity) {

		const isContentContainer = entity.isContentContainer;
		const isContentItem      = entity.isContentItem;

		let elements = [];

		if (isContentItem) {

			elements.push({
				icon: _Icons.getSvgIcon('pencil_edit'),
				name: 'Edit',
				clickHandler: function () {
					_Contents.editItem(entity);
					return false;
				}
			});
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

				if (isContentContainer) {

                    _Entities.deleteNode(this, entity, true, function() {
                        _Contents.refreshTree();
                    });

                } else if (isContentItem) {

                    _Entities.deleteNode(this, entity);
                }
				return false;
			}
		});

		_Elements.appendContextMenuSeparator(elements);

		return elements;
	},
	deepOpen: function(d, dirs) {

		_TreeHelper.deepOpen(_Contents.contentTree, d, dirs, 'parent', (_Contents.currentContentContainer ? _Contents.currentContentContainer.id : 'root'));
	},
	refreshTree: function() {

		_TreeHelper.refreshTree(_Contents.contentTree, function() {
			_TreeHelper.makeTreeElementDroppable(_Contents.contentTree, 'root');
		});
	},
	treeInitFunction: function(obj, callback) {

		switch (obj.id) {

			case '#':

				var defaultEntries = [{
					id: 'root',
					text: '/',
					children: true,
					icon: _Icons.structr_logo_small,
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
	unload: function() {
		fastRemoveAllChildren($('.searchBox', main));
		fastRemoveAllChildren($('#contents-main', main));
	},
	fulltextSearch: function(searchString) {
		_Contents.contentsContents.children().hide();

		var url;
		if (searchString.contains(' ')) {
			url = rootUrl + 'ContentItem/ui?' + Structr.getRequestParameterName('loose') + '=1';
			searchString.split(' ').forEach(function(str, i) {
				url = url + '&name=' + str;
			});
		} else {
			url = rootUrl + 'ContentItem/ui?' + Structr.getRequestParameterName('loose') + '=1&name=' + searchString;
		}

		_Contents.displaySearchResultsForURL(url);
	},
	clearSearch: function() {
		$('.search', main).val('');
		$('#search-results').remove();
		_Contents.contentsContents.children().show();
	},
	loadAndSetWorkingDir: function(callback) {

		_Contents.currentContentContainer = LSWrapper.getItem(_Contents.currentContentContainerKey);
		callback();

	},
	load: function(id, callback) {

		let filter = null;

		if (id) {
			filter = {parent: id};
		}

		Command.query('ContentContainer', _Contents.containerPageSize, _Contents.containerPage, 'position', 'asc', filter, function(folders) {

			let list = [];

			folders.forEach(function(d) {
				let childCount = (d.items && d.items.length > 0) ? ' (' + d.items.length + ')' : '';
				list.push({
					id: d.id,
					text: (d.name ? d.name : '[unnamed]') + childCount,
					children: d.isContentContainer && d.childContainers.length > 0,
					icon: 'fa fa-folder-o',
					path: d.path
				});
			});

			callback(list);

			_TreeHelper.makeDroppable(_Contents.contentTree, list);

		}, true, null, 'id,name,items,isContentContainer,childContainers,path');

	},
	setWorkingDirectory: function(id) {

		if (id === 'root') {
			_Contents.currentContentContainer = null;
		} else {
			_Contents.currentContentContainer = { 'id': id };
		}

		LSWrapper.setItem(_Contents.currentContentContainerKey, _Contents.currentContentContainer);
	},
	displayContainerContents: function(id, parentId, nodePath, parents) {

		fastRemoveAllChildren(_Contents.contentsContents[0]);

		let handleChildren = function(children) {
			if (children && children.length) {
				children.forEach(_Contents.appendItemOrContainerRow);
			}
		};

		if (id === 'root') {
			Command.list('ContentContainer', true, 1000, 1, 'position', 'asc', null, handleChildren);
		} else {
			Command.query('ContentContainer', 1000, 1, 'position', 'asc', {parent: id}, handleChildren, true, 'ui');
		}

		_Pager.initPager('contents-items', 'ContentItem', 1, 25, 'position', 'asc');
		_Pager.page['ContentItem'] = 1;
		_Pager.initFilters('contents-items', 'ContentItem', id === 'root' ? { containers: [] } : { containers: [id] });

		let itemsPager = _Pager.addPager('contents-items', _Contents.contentsContents, false, 'ContentItem', 'ui', handleChildren);

		itemsPager.cleanupFunction = function () {
			let toRemove = $('.node.item', itemsPager.el).closest('tr');
			toRemove.each(function(i, elem) {
				fastRemoveAllChildren(elem);
			});
		};

		itemsPager.pager.append('Filter: <input type="text" class="filter" data-attribute="name">');
		itemsPager.pager.append('<input type="text" class="filter" data-attribute="parentId" value="' + ((parentId === '#') ? '' : id) + '" hidden>');
		itemsPager.pager.append('<input type="checkbox" class="filter" data-attribute="hasParent" ' + ((parentId === '#') ? '' : 'checked') + ' hidden>');
		itemsPager.activateFilterElements();

		_Contents.insertBreadCrumbNavigation(parents, nodePath, id);

		_Contents.contentsContents.append('<table id="files-table" class="stripe"><thead><tr><th class="icon">&nbsp;</th><th>Name</th><th>Size</th><th>Type</th><th>Owner</th><th>Modified</th></tr></thead>'
				+ '<tbody id="files-table-body">'
				+ ((id !== 'root') ? '<tr id="parent-file-link"><td class="file-icon"><i class="fa fa-folder-o"></i></td><td><a href="#" class="folder-up">..</a></td><td></td><td></td><td></td><td></td></tr>' : '')
				+ '</tbody></table>'
		);

		$('#parent-file-link').on('click', function(e) {

			if (parentId !== '#') {
				$('#' + parentId + '_anchor').click();
			}
		});

	},
	insertBreadCrumbNavigation: function(parents, nodePath, id) {

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
			let path      = parents.map((parent, idx) => { return '<a class="breadcrumb-entry" data-folder-id="' + parent + '">' + pathNames[idx] + '/</a>'; }).join('') + pathNames.pop();

			_Contents.contentsContents.append('<div class="folder-path truncate">' + path + '</div>');

			$('.breadcrumb-entry').click(function (e) {
				e.preventDefault();

				$('#' + $(this).data('folderId') + '_anchor').click();
			});
		}
	},
	appendItemOrContainerRow: function(d) {

		// add container/item to global model
		StructrModel.createFromData(d, null, false);

		let tableBody  = $('#files-table-body');

		$('#row' + d.id, tableBody).remove();

		let items      = d.items || [];
		let containers = d.containers || [];
		let size       = d.isContentContainer ? containers.length + items.length : (d.size ? d.size : '-');

		let rowId = 'row' + d.id;
		tableBody.append('<tr id="' + rowId + '"' + (d.isThumbnail ? ' class="thumbnail"' : '') + '></tr>');

		let row   = $('#' + rowId);
		let icon  = d.isContentContainer ? 'fa-folder-o' : _Contents.getIcon(d);
		let title = (d.name ? d.name : '[unnamed]');

		if (d.isContentContainer) {

			row.append('<td class="file-icon"><i class="fa ' + icon + '"></i></td>');
			row.append('<td><div id="id_' + d.id + '" data-structr_type="folder" class="node container flex items-center justify-between"><b title="' + escapeForHtmlAttributes(title) + '" class="name_ leading-8 truncate">' + d.name + '</b></div></td>');

		} else {

			row.append('<td class="file-icon"><a href="javascript:void(0)"><i class="fa ' + icon + '"></i></a></td>');
			row.append('<td><div id="id_' + d.id + '" data-structr_type="item" class="node item flex items-center justify-between"><b title="' + escapeForHtmlAttributes(title) + '" class="name_ leading-8 truncate">' + (d.name ? d.name : '[unnamed]') + '</b></td>');
		}

		row.append('<td>' + size + '</td>');
		row.append('<td class="truncate">' + d.type + '</td>');
		row.append('<td class="truncate">' + (d.owner ? (d.owner.name ? d.owner.name : '[unnamed]') : '') + '</td>');
		row.append('<td class="truncate">' + moment(d.lastModifiedDate).calendar() + '</td>');

		// Change working dir by click on folder icon
		$('#id_' + d.id + '.container').parent().prev().on('click', function(e) {

			e.preventDefault();
			e.stopPropagation();

			if (d.parentId) {

				_Contents.contentTree.jstree('open_node', $('#' + d.parentId), function() {

					if (d.name === '..') {
						$('#' + d.parentId + '_anchor').click();
					} else {
						$('#' + d.id + '_anchor').click();
					}

				});

			} else {

				$('#' + d.id + '_anchor').click();
			}

			return false;
		});

		let div = Structr.node(d.id);

		if (!div || !div.length)
			return;

		div.children('b.name_').off('click').on('click', function(e) {
			e.stopPropagation();
			_Entities.makeNameEditable(div);
		});

		div.on('remove', function() {
			div.closest('tr').remove();
		});

		if (d.isContentContainer) {

			// ********** Containers **********

			// var newDelIcon = '<i title="Delete container \'' + d.name + '\'" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />';
			// if (delIcon && delIcon.length) {
			// 	delIcon.replaceWith(newDelIcon);
			// } else {
			// 	div.append(newDelIcon);
			// }
			// div.children('.delete_icon').on('click', function(e) {
			// 	e.stopPropagation();
			// 	_Entities.deleteNode(this, d, true, function() {
			// 		_Contents.refreshTree();
			// 	});
			// });

			div.droppable({
				accept: '.container, .item',
				greedy: true,
				hoverClass: 'nodeHover',
				tolerance: 'pointer',
				drop: function(e, ui) {

					e.preventDefault();
					e.stopPropagation();

					let self        = $(this);
					let itemId      = Structr.getId(ui.draggable);
					let containerId = Structr.getId(self);

					if (!(itemId === containerId)) {
						let nodeData = {};
						nodeData.id = itemId;

						_Entities.addToCollection(itemId, containerId, 'containers', function() {
							$(ui.draggable).remove();
							_Contents.refreshTree();
						});
					}
					return false;
				}
			});

		} else {

			// ********** Items **********

			let dblclickHandler = (e) => {
				_Contents.editItem(d);
			};

			if (div) {
				let node = div[0].closest('.node');
				node.removeEventListener('dblclick', dblclickHandler);
				node.addEventListener('dblclick', dblclickHandler);
			}
		}

		div.draggable({
			revert: 'invalid',
			//helper: 'clone',
			containment: 'body',
			stack: '.jstree-node',
			appendTo: '#main',
			forceHelperSize: true,
			forcePlaceholderSize: true,
			distance: 5,
			cursorAt: { top: 8, left: 25 },
			zIndex: 99,
			stop: function(e, ui) {
				$(this).show();
				$(e.toElement).one('click', function(e) {
					e.stopImmediatePropagation();
				});
			},
			helper: function(event) {
				var helperEl = $(this);
				selectedElements = $('.node.selected');
				if (selectedElements.length > 1) {
					selectedElements.removeClass('selected');
					return $('<i class="node-helper ' + _Icons.getFullSpriteClass(_Icons.page_white_stack_icon) + '">');
				}
				var hlp = helperEl.clone();
				hlp.find('.button').remove();
				return hlp;
			}
		});

		_Entities.appendContextMenuIcon(div, d);
		_Entities.setMouseOver(div);
		_Entities.makeSelectable(div);
		_Elements.enableContextMenuOnElement(div, d);

	},
	checkValueHasChanged: function(oldVal, newVal, buttons) {

		if (newVal === oldVal) {

			buttons.forEach(function(button) {
				button.prop("disabled", true).addClass('disabled');
			});

		} else {

			buttons.forEach(function(button) {
				button.prop("disabled", false).removeClass('disabled');
			});
		}
	},
	editItem: function(item) {

		Structr.dialog('Edit ' + item.name, function() {
		}, function() {
		});

		Command.get(item.id, null, function(entity) {

			dialogBtn.append('<button id="saveItem" disabled="disabled" class="action disabled"> Save </button>');
			dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="action disabled"> Save and close</button>');
			dialogBtn.append('<button id="refresh"> Refresh</button>');

			dialogSaveButton = $('#saveItem', dialogBtn);
			saveAndClose     = $('#saveAndClose', dialogBtn);
			let refreshBtn   = $('#refresh', dialogBtn);

			_Entities.getSchemaProperties(entity.type, 'custom', function(properties) {

				let props = Object.values(properties);

				//_Contents.sortBySchemaOrder(entity.type, 'custom', properties, function(props) {

					props.forEach(function(prop) {

						let isRelated    = 'relatedType' in prop;
						let key          = prop.jsonName;
						let isCollection = prop.isCollection || false;
						let isReadOnly   = prop.isReadOnly   || false;
						let isSystem     = prop.system       || false;
						let oldVal       = entity[key];

						dialogText.append('<div id="prop-' + key + '" class="prop"><label for="' + key + '"><h3>' + formatKey(key) + '</h3></label></div>');
						let div = $('#prop-' + key);

						if (prop.type === 'Boolean') {

							div.removeClass('value').append('<div class="value-container"><input type="checkbox" class="' + key + '_"></div>');
							let checkbox = div.find('input[type="checkbox"].' + key + '_');
							Command.getProperty(entity.id, key, function(val) {
								if (val) {
									checkbox.prop('checked', true);
								}
								if ((!isReadOnly || StructrWS.isAdmin) && !isSystem) {
									checkbox.on('change', function() {
										var checked = checkbox.prop('checked');
										_Contents.checkValueHasChanged(oldVal, checked || false, [dialogSaveButton, saveAndClose]);
									});
								} else {
									checkbox.prop('disabled', 'disabled');
									checkbox.addClass('readOnly');
									checkbox.addClass('disabled');
								}
							});

						} else if (prop.type === 'Date' && !isReadOnly) {

							div.append('<div class="value-container"></div>');
							_Entities.appendDatePicker($('.value-container', div), entity, key, prop.format || "yyyy-MM-dd'T'HH:mm:ssZ");
							let valueInput = $('.value-container input', div);
							valueInput.on('change', function(e) {
								if (e.keyCode !== 27) {
									Command.get(entity.id, key, function(newEntity) {
										_Contents.checkValueHasChanged(newEntity[key], valueInput.val() || null, [dialogSaveButton, saveAndClose]);
									});
								}
							});

						} else if (isRelated) {

							let relatedNodesList = $('<div class="value-container related-nodes"> <i class="add ' + _Icons.getFullSpriteClass(_Icons.add_grey_icon) + '" /> </div>');
							div.append(relatedNodesList);
							$(relatedNodesList).children('.add').on('click', function() {
								Structr.dialog('Add ' + prop.type, function() {
								}, function() {
									_Contents.editItem(item);
								});
								_Entities.displaySearch(entity.id, key, prop.type, dialogText, isCollection);
							});

							if (entity[key]) {

								let relatedNodes = $('.related-nodes', div);

								if (!isCollection) {

									let nodeId = entity[key].id || entity[key];

									Command.get(nodeId, 'id,type,tag,isContent,content,name', function(node) {

										_Entities.appendRelatedNode(relatedNodes, node, function(nodeEl) {

											$('.remove', nodeEl).on('click', function(e) {
												e.preventDefault();
												_Entities.setProperty(entity.id, key, null, false, function(newVal) {
													if (!newVal) {
														blinkGreen(relatedNodes);
														Structr.showAndHideInfoBoxMessage('Related node "' + (node.name || node.id) + '" was removed from property "' + key + '".', 'success', 2000, 1000);
														nodeEl.remove();
													} else {
														blinkRed(relatedNodes);
													}
												});
												return false;
											});
										});
									});

								} else {

									entity[key].forEach(function(obj) {

										var nodeId = obj.id || obj;

										Command.get(nodeId, 'id,type,tag,isContent,content,name', function(node) {

											_Entities.appendRelatedNode(relatedNodes, node, function(nodeEl) {
												$('.remove', nodeEl).on('click', function(e) {
													e.preventDefault();
													Command.removeFromCollection(entity.id, key, node.id, function() {
														var nodeEl = $('._' + node.id, relatedNodes);
														nodeEl.remove();
														blinkGreen(relatedNodes);
														Structr.showAndHideInfoBoxMessage('Related node "' + (node.name || node.id) + '" was removed from property "' + key + '".', 'success', 2000, 1000);
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
								div.append('<div class="value-container edit-area">' + (oldVal || '') + '</div>');
								let editArea = $('.edit-area', div);
								editArea.trumbowyg({
									//btns: ['strong', 'em', '|', 'insertImage'],
									//autogrow: true
								}).on('tbwchange', function() {
									Command.get(entity.id, key, function(newEntity) {
										_Contents.checkValueHasChanged(newEntity[key], editArea.trumbowyg('html') || null, [dialogSaveButton, saveAndClose]);
									});
								}).on('tbwpaste', function() {
									Command.get(entity.id, key, function(newEntity) {
										_Contents.checkValueHasChanged(newEntity[key], editArea.trumbowyg('html') || null, [dialogSaveButton, saveAndClose]);
									});
								});

							} else {
								div.append('<div class="value-container"></div>');
								let valueContainer = $('.value-container', div);
								let valueInput;

								valueContainer.append(formatValueInputField(key, oldVal, false, prop.readOnly, prop.format === 'multi-line'));
								valueInput = valueContainer.find('[name=' + key + ']');

								valueInput.on('keyup', function(e) {
									if (e.keyCode !== 27) {
										Command.get(entity.id, key, function(newEntity) {
											_Contents.checkValueHasChanged(newEntity[key], valueInput.val() || null, [dialogSaveButton, saveAndClose]);
										});
									}
								});
							}
						}

					});
				//});

			}, true);

			dialogSaveButton.on('click', function(e) {

				ignoreKeyUp = false;

				e.preventDefault();
				e.stopPropagation();

				_Entities.getSchemaProperties(entity.type, 'custom', function(properties) {

					let props = Object.values(properties);

					props.forEach(function(prop) {

						let key = prop.jsonName;

						var newVal;
						var oldVal = entity[key];

						if (true) {

							if (prop.contentType && prop.contentType === 'text/html') {
								newVal = $('#prop-' + key + ' .edit-area').trumbowyg('html') || null;
							} else if (prop.propertyType === 'Boolean') {
								newVal = $('#prop-' + key + ' .value-container input').prop('checked') || false;
							} else {
								if (prop.format === 'multi-line') {
									newVal = $('#prop-' + key + ' .value-container textarea').val() || null;
								} else {
									newVal = $('#prop-' + key + ' .value-container input').val() || null;
								}
							}

							if (!prop.relatedType && newVal !== oldVal) {

								Command.setProperty(entity.id, key, newVal, false, function() {

									oldVal = newVal;
									dialogSaveButton.prop("disabled", true).addClass('disabled');
									saveAndClose.prop("disabled", true).addClass('disabled');

									// update title in list
									if (key === 'title') {
										var f = $('#row' + entity.id + ' .item-title b');
										f.text(newVal);
										blinkGreen(f);
									}
								});
							}
						}
					});

					setTimeout(function() {
						refreshBtn.click();
					}, 500);

				}, true);

				dialogSaveButton.prop("disabled", true).addClass('disabled');
				saveAndClose.prop("disabled", true).addClass('disabled');
			});

			saveAndClose.on('click', function(e) {
				e.stopPropagation();
				dialogSaveButton.click();
				setTimeout(function() {
					dialogSaveButton.remove();
					saveAndClose.remove();
					dialogCancelButton.click();
				}, 1000);
			});

			refreshBtn.on('click', function(e) {
				e.stopPropagation();
				_Contents.editItem(item);
			});

		}, 'all');
	},
	sortBySchemaOrder: function(type, view, properties, callback) {

		let url = rootUrl + '_schema/' + type + '/' + view;
		$.ajax({
			url: url,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function(data) {

					// no schema entry found?
					if (!data || !data.result || data.result_count === 0) {

						//

					} else {

						let sortedProperties = [];

						data.result.forEach(function(prop) {
							sortedProperties.push(prop.jsonName);
						});

						//console.log(sortedProperties, properties);

						properties.sort(function(a, b) {
							return sortedProperties.indexOf(a.name) - sortedProperties.indexOf(b.name);
						});

						//console.log(properties);
					}

					if (callback) {
						callback(properties);
					}
				},
				400: function(data) {
					Structr.errorFromResponse(data.responseJSON, url);
				},
				401: function(data) {
					Structr.errorFromResponse(data.responseJSON, url);
				},
				403: function(data) {
					Structr.errorFromResponse(data.responseJSON, url);
				},
				404: function(data) {
					Structr.errorFromResponse(data.responseJSON, url);
				},
				422: function(data) {
					Structr.errorFromResponse(data.responseJSON, url);
				}
			},
			error:function () {
				console.log("ERROR: loading Schema " + type);
			}
		});
	},
	// appendEditFileIcon: function(parent, item) {
	//
	// 	var editIcon = $('.edit_file_icon', parent);
	//
	// 	if (!(editIcon && editIcon.length)) {
	// 		parent.append('<i title="Edit ' + item.name + ' [' + item.id + ']" class="edit_file_icon button ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" />');
	// 	}
	//
	// 	$(parent.children('.edit_file_icon')).on('click', function(e) {
	// 		e.stopPropagation();
	//
	// 		_Contents.editItem(item);
	// 	});
	// },
	displaySearchResultsForURL: function(url) {

		$('#search-results').remove();
		_Contents.contentsContents.append('<div id="search-results"></div>');

		let searchString = Structr.functionBar.querySelector('.search').value;
		let container    = $('#search-results');
		_Contents.contentsContents.on('scroll', function() {
			window.history.pushState('', '', '#contents');
		});

		$.ajax({
			url: url,
			statusCode: {
				200: function(data) {

					if (!data.result || data.result.length === 0) {
						container.append('<h1>No results for "' + searchString + '"</h1>');
						container.append('<h2>Press ESC or click <a href="#contents" class="clear-results">here to clear</a> empty result list.</h2>');
						$('.clear-results', container).on('click', function() {
							_Contents.clearSearch();
						});
						return;
					} else {
						container.append('<h1>' + data.result.length + ' search results:</h1><table class="props"><thead><th class="_type">Type</th><th>Name</th><!--th>Size</th--></thead><tbody></tbody></table>');
						data.result.forEach(function(d) {
							var icon = _Contents.getIcon(d);
							$('tbody', container).append('<tr><td><i class="fa ' + icon + '"></i> ' + d.type + (d.isFile && d.contentType ? ' (' + d.contentType + ')' : '') + '</td><td><a href="#results' + d.id + '">' + d.name + '</a></td><!--td>' + d.size + '</td--></tr>');

						});
					}

					// data.result.forEach(function(d) {
					//
					// 	$.ajax({
					// 		url: rootUrl + 'contents/' + d.id + '/getSearchContext',
					// 		contentType: 'application/json',
					// 		method: 'POST',
					// 		data: JSON.stringify({searchString: searchString, contextLength: 30}),
					// 		statusCode: {
					// 			200: function(data) {
					//
					// 				if (!data.result) return;
					//
					// 				//console.log(data.result);
					//
					// 				container.append('<div class="search-result collapsed" id="results' + d.id + '"></div>');
					//
					// 				var div = $('#results' + d.id);
					// 				var icon = _Contents.getIcon(d);
					// 				div.append('<h2><i class="fa ' + icon + '"></i> ' + d.name + '<i id="preview' + d.id + '" class="' + _Icons.getFullSpriteClass(_Icons.eye_icon) + '" style="margin-left: 6px;" title="' + d.extractedContent + '" /></h2>');
					// 				div.append('<i class="toggle-height fa fa-expand"></i>').append('<i class="go-to-top fa fa-chevron-up"></i>');
					//
					// 				$('.toggle-height', div).on('click', function() {
					// 					var icon = $(this);
					// 					div.toggleClass('collapsed');
					// 					if (icon.hasClass('fa-expand')) {
					// 						icon.removeClass('fa-expand');
					// 						icon.addClass('fa-compress');
					// 					} else {
					// 						icon.removeClass('fa-compress');
					// 						icon.addClass('fa-expand');
					// 					}
					//
					// 				});
					//
					// 				$('.go-to-top', div).on('click', function() {
					// 					content.scrollTop(0);
					// 					window.history.pushState('', '', '#contents');
					// 				});
					//
					// 				$.each(data.result.context, function(i, contextString) {
					//
					// 					searchString.split(/[\s,;]/).forEach(function(str) {
					// 						contextString = contextString.replace(new RegExp('(' + str + ')', 'gi'), '<span class="highlight">$1</span>');
					// 					});
					//
					// 					div.append('<div class="part">' + contextString + '</div>');
					//
					// 				});
					//
					// 				div.append('<div style="clear: both;"></div>');
					// 			}
					// 		}
					// 	});
					//
					// });
				}
			}
		});
	},
	getIcon: function(file) {
		return (file.isContentContainer ? 'fa-folder-o' : 'fa-file-o');
	}
};