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
var main, filesystemMain, fileTree, folderContents;
var fileList;
var chunkSize = 1024 * 64;
var sizeLimit = 1024 * 1024 * 1024;
var win = $(window);
var selectedElements = [];
var activeFileId, fileContents = {};
var counter = 0;
var currentWorkingDir;
var folderPageSize = 10000, folderPage = 1;

$(document).ready(function() {

	Structr.registerModule('contents', _Contents);
	_Contents.resize();

});

var _Contents = {

	icon: 'icon/page_white.png',
	add_file_icon: 'icon/page_white_add.png',
	pull_file_icon: 'icon/page_white_put.png',
	delete_file_icon: 'icon/page_white_delete.png',
	add_folder_icon: 'icon/folder_add.png',
	folder_icon: 'icon/folder-o.png',
	delete_folder_icon: 'icon/folder_delete.png',
	download_icon: 'icon/basket_put.png',

	init: function() {

		_Logger.log(_LogType.CONTENTS, '_Contents.init');

		main = $('#main');

		main.append('<div class="searchBox module-dependend" data-module="structr-text-search-module"><input class="search" name="search" placeholder="Search..."><img class="clearSearchIcon" src="icon/cross_small_grey.png"></div>');

		searchField = $('.search', main);
		searchField.focus();

		searchField.keyup(function(e) {

			var searchString = $(this).val();
			if (searchString && searchString.length && e.keyCode === 13) {

				$('.clearSearchIcon').show().on('click', function() {
					_Contents.clearSearch();
				});

				_Contents.fulltextSearch(searchString);

			} else if (e.keyCode === 27 || searchString === '') {
				_Contents.clearSearch();
			}

		});

		Structr.makePagesMenuDroppable();

	},
	resize: function() {

		var windowWidth = win.width();
		var windowHeight = win.height();
		var headerOffsetHeight = 100;

		if (fileTree) {
			fileTree.css({
				width: Math.max(180, Math.min(windowWidth / 3, 360)) + 'px',
				height: windowHeight - headerOffsetHeight + 'px'
			});
		}

		if (folderContents) {
			folderContents.css({
				width: windowWidth - 400 - 64 + 'px',
				height: windowHeight - headerOffsetHeight - 55 + 'px'
			});
		}
		Structr.resize();

	},
	onload: function() {

		_Contents.init();

		$('#main-help a').attr('href', 'https://support.structr.com/knowledge-graph');

		main.append('<div id="contents-main"><div class="fit-to-height" id="file-tree-container"><div id="file-tree"></div></div><div class="fit-to-height" id="folder-contents-container"><div id="folder-contents"></div></div>');
		filesystemMain = $('#contents-main');

		fileTree = $('#file-tree');
		folderContents = $('#folder-contents');

//		$('#folder-contents-container').prepend(
//				'<button class="add_file_icon button"><img title="Add File" alt="Add File" src="' + _Contents.add_file_icon + '"> Add File</button>'
//				+ '<button class="pull_file_icon button module-dependend" data-module="structr-cloud-module"><img title="Sync Files" alt="Sync Files" src="' + _Contents.pull_file_icon + '"> Sync Files</button>'
//				);

		$('#folder-contents-container').prepend(' <select id="add-content-item"><option value="">Add Content Item</option></select>');
		
		var itemTypesSelector = $('#add-content-item', main);
		// query: function(type, pageSize, page, sort, order, properties, callback, exact, view) {
		Command.query('SchemaNode', 1000, 1, 'name', 'asc', { extendsClass: 'org.structr.web.entity.ContentItem' }, function(schemaNodes) {
			//console.log(schemaNodes);
			schemaNodes.forEach(function(schemaNode) {
				var type = schemaNode.name;
				itemTypesSelector.append('<option value="' + type + '">' + type + '</option>');
			});
		}, true);
		
		itemTypesSelector.on('change', function(e) {
			e.stopPropagation();
			var sel = $(this);
			var type = sel.val();
			if (type) {
				Command.create({ type: type, size: 0, parentId: currentWorkingDir ? currentWorkingDir.id : null }, function(f) {
					_Contents.appendFileOrFolderRow(f);
					itemTypesSelector.prop('selectedIndex', 0);
				});
			}
		});

		$('#folder-contents-container').prepend('<select id="add-content-container"><option value="">Add Content Container</option></select>');
		
		var containerTypesSelector = $('#add-content-container', main);
		Command.query('SchemaNode', 1000, 1, 'name', 'asc', { extendsClass: 'org.structr.web.entity.ContentContainer' }, function(schemaNodes) {
			//console.log(schemaNodes);
			schemaNodes.forEach(function(schemaNode) {
				var type = schemaNode.name;
				containerTypesSelector.append('<option value="' + type + '">' + type + '</option>');
			});
		}, true);
		
		containerTypesSelector.on('change', function(e) {
			e.stopPropagation();
			var sel = $(this);
			var type = sel.val();
			if (type) {
				Command.create({ type: type, parentId: currentWorkingDir ? currentWorkingDir.id : null }, function(f) {
					_Contents.appendFileOrFolderRow(f);
					_Contents.refreshTree();
					containerTypesSelector.prop('selectedIndex', 0);
				});
			}
		});

		$.jstree.defaults.core.themes.dots      = false;
		$.jstree.defaults.dnd.inside_pos        = 'last';
		$.jstree.defaults.dnd.large_drop_target = true;

		fileTree.on('ready.jstree', function() {
			var rootEl = $('#root > .jstree-wholerow');
			_Dragndrop.makeDroppable(rootEl);
		});

		_Contents.loadAndSetWorkingDir(function() {
			_Contents.initTree();
			if (!currentWorkingDir) {
				_Contents.displayFolderContents('root', null, '/');
			}

		});

		fileTree.on('select_node.jstree', function(evt, data) {

			if (data.node.id === 'root') {
				_Contents.deepOpen(currentWorkingDir, []);
			}

			_Contents.setWorkingDirectory(data.node.id);
			_Contents.displayFolderContents(data.node.id, data.node.parent, data.node.original.path, data.node.parents);

		});

		win.off('resize');
		win.resize(function() {
			_Contents.resize();
		});

		Structr.unblockMenu(100);

		_Contents.resize();

	},
	deepOpen: function(d, dirs) {
		if (d && d.id) {
			dirs.unshift(d);
			Command.get(d.id, function(dir) {
				if (dir && dir.parent) {
					_Contents.deepOpen(dir.parent, dirs);
				} else {
					_Contents.open(dirs);
				}
			});
		}
	},
	open: function(dirs) {
		if (!dirs.length) return;
		var d = dirs.shift();
		fileTree.jstree('deselect_node', d.id);
		fileTree.jstree('open_node', d.id, function() {
			if (currentWorkingDir) {
				fileTree.jstree('select_node', currentWorkingDir.id);
			}
			_Contents.open(dirs);
		});

	},
	refreshTree: function() {
		fileTree.jstree('refresh');
		window.setTimeout(function() {
			var rootEl = $('#root > .jstree-wholerow');
			_Dragndrop.makeDroppable(rootEl);
		}, 500);
	},
	initTree: function() {
		fileTree.jstree({
			'plugins': ["themes", "dnd", "search", "state", "types", "wholerow"],
			'core': {
				'animation': 0,
				'state': {'key': 'structr-ui'},
				'async': true,
				'data': function(obj, callback) {

					switch (obj.id) {

						case '#':

							Command.list('ContentContainer', true, folderPageSize, folderPage, 'name', 'asc', null, function(folders) {

								var children = [];
								var list = [];

								list.push({
									id: 'root',
									text: '/',
									children: true,
									icon: '/structr/icon/structr_icon_16x16.png',
									path: '/',
									state: {
										opened: true,
										selected: true
									}
								});

								folders.forEach(function(d) {

									children.push({
										id: d.id,
										text: d.name ? d.name : '[unnamed]',
										children: d.isContentContainer && d.items.length > 0,
										icon: 'fa fa-folder-o'
									});
								});

								callback(list);

							});
							break;

						case 'root':
							_Contents.load(null, callback);
							break;

						default:
							_Contents.load(obj.id, callback);
							break;
					}
				}
			}
		});
	},
	unload: function() {
		fastRemoveAllChildren($('.searchBox', main));
		fastRemoveAllChildren($('#contents-main', main));
	},
	fulltextSearch: function(searchString) {
		var content = $('#folder-contents');
		content.children().hide();

		var url;
		if (searchString.contains(' ')) {
			url = rootUrl + 'files/ui?loose=1';
			searchString.split(' ').forEach(function(str, i) {
				url = url + '&indexedWords=' + str;
			});
		} else {
			url = rootUrl + 'files/ui?indexedWords=' + searchString;
		}

		_Contents.displaySearchResultsForURL(url);
	},
	clearSearch: function() {
		$('.search', main).val('');
		$('#search-results').remove();
		$('#folder-contents').children().show();
	},
	loadAndSetWorkingDir: function(callback) {
		Command.rest("/me/ui", function (result) {
			var me = result[0];
			if (me.workingDirectory) {
				currentWorkingDir = me.workingDirectory;
			} else {
				currentWorkingDir = null;
			}

			callback();
		});

	},
	load: function(id, callback) {

		if (!id) {

			Command.list('ContentContainer', true, folderPageSize, folderPage, 'name', 'asc', null, function(folders) {

				var list = [];

				folders.forEach(function(d) {
					list.push({
						id: d.id,
						text:  d.name ? d.name : '[unnamed]',
						children: d.isContentContainer && d.items.length > 0,
						icon: 'fa fa-folder-o',
						path: d.path
					});
				});

				callback(list);

				window.setTimeout(function() {
					list.forEach(function(obj) {
						var el = $('#file-tree #' + obj.id + ' > .jstree-wholerow');
						StructrModel.create({id: obj.id});
						_Dragndrop.makeDroppable(el);
					});
				}, 500);

			}, true);

		} else {

			Command.query('ContentContainer', folderPageSize, folderPage, 'name', 'asc', {parent: id}, function(folders) {

				var list = [];

				folders.forEach(function(d) {
					list.push({
						id: d.id,
						text:  d.name ? d.name : '[unnamed]',
						children: d.isContentContainer && d.items.length > 0,
						icon: 'fa fa-folder-o',
						path: d.path
					});
				});

				callback(list);

				window.setTimeout(function() {
					list.forEach(function(obj) {
						var el = $('#file-tree #' + obj.id + ' > .jstree-wholerow');
						StructrModel.create({id: obj.id});
						_Dragndrop.makeDroppable(el);
					});
				}, 500);

			}, true);
		}

	},
	setWorkingDirectory: function(id) {

		if (id === 'root') {
			currentWorkingDir = null;
		} else {
			currentWorkingDir = { 'id': id };
		}
		var data = JSON.stringify({'workingDirectory': currentWorkingDir});

		$.ajax({
			url: rootUrl + 'me',
			dataType: 'json',
			contentType: 'application/json; UTF-8',
			type: 'PUT',
			data: data
		});
	},
	displayFolderContents: function(id, parentId, nodePath, parents) {
		var content = $('#folder-contents');
		fastRemoveAllChildren(content[0]);
		var path = '';
		if (parents) {
			parents = [].concat(parents).reverse().slice(1);
			var pathNames = nodePath.split('/');
			pathNames[0] = '/';
			path = parents.map(function(parent, idx) {
				return '<a class="breadcrumb-entry" data-folder-id="' + parent + '"><i class="fa fa-caret-right"></i> ' + pathNames[idx] + '</span></a>';
			}).join(' ');
			path += ' <i class="fa fa-caret-right"></i> ' + pathNames.pop();
		}

		var handleChildren = function(children) {
			if (children && children.length) {
				children.forEach(_Contents.appendFileOrFolderRow);
			}
		};

		if (id === 'root') {
			Command.list('ContentContainer', true, 1000, 1, 'name', 'asc', null, handleChildren);
		} else {
			Command.query('ContentContainer', 1000, 1, 'name', 'asc', {parentId: id}, handleChildren, true, 'ui');
		}


		_Pager.initPager('contents-items', 'ContentItem', 1, 25, 'name', 'asc');
		page['ContentItem'] = 1;
//		_Pager.initFilters('contents-items', 'ContentItem', {
//			parentId: ((parentId === '#') ? '' : id),
//			hasParent: (parentId !== '#')
//		});

		var filesPager = _Pager.addPager('contents-items', content, false, 'ContentItem', 'ui', handleChildren);

		filesPager.cleanupFunction = function () {
			var toRemove = $('.node.file', filesPager.el).closest('tr');
			toRemove.each(function(i, elem) {
				fastRemoveAllChildren(elem);
			});
		};

		filesPager.pager.append('Filter: <input type="text" class="filter" data-attribute="name">');
		filesPager.pager.append('<input type="text" class="filter" data-attribute="parentId" value="' + ((parentId === '#') ? '' : id) + '" hidden>');
		filesPager.pager.append('<input type="checkbox" class="filter" data-attribute="hasParent" ' + ((parentId === '#') ? '' : 'checked') + ' hidden>');
		filesPager.activateFilterElements();

		content.append(
				'<h2>' + path + '</h2>'
				+ '<table id="files-table" class="stripe"><thead><tr><th class="icon">&nbsp;</th><th>Title/Name</th><th>Size</th><th>Type</th><th>Owner</th></tr></thead>'
				+ '<tbody id="files-table-body">'
				+ ((id !== 'root') ? '<tr id="parent-file-link"><td class="file-type"><i class="fa fa-folder-o"></i></td><td><a href="#">..</a></td><td></td><td></td><td></td></tr>' : '')
				+ '</tbody></table>'
		);

		$('.breadcrumb-entry').click(function (e) {
			e.preventDefault();

			$('#' + $(this).data('folderId') + '_anchor').click();

		});

		$('#parent-file-link').on('click', function(e) {

			if (parentId !== '#') {
				$('#' + parentId + '_anchor').click();
			}
		});


	},
	appendFileOrFolderRow: function(d) {

		// add folder/file to global model
		StructrModel.createFromData(d, null, false);

		var tableBody = $('#files-table-body');

		$('#row' + d.id, tableBody).remove();

		var files = d.files || [];
		var folders = d.folders || [];
		var size = d.isFolder ? folders.length + files.length : (d.size ? d.size : '-');

		var rowId = 'row' + d.id;
		tableBody.append('<tr id="' + rowId + '"' + (d.isThumbnail ? ' class="thumbnail"' : '') + '></tr>');
		var row = $('#' + rowId);
		var icon = d.isFolder ? 'fa-folder-o' : _Contents.getIcon(d);


		if (d.isContentContainer) {
			row.append('<td class="file-type"><i class="fa ' + icon + '"></i></td>');
			row.append('<td><div id="id_' + d.id + '" data-structr_type="folder" class="node container"><b title="' + d.name + '" class="name_">' + fitStringToWidth(d.name, 200) + '</b> <span class="id">' + d.id + '</span></div></td>');
		} else {
			row.append('<td class="file-type"><a href="' + d.path + '" target="_blank"><i class="fa ' + icon + '"></i></a></td>');
			//row.append('<td class="item-title"><b>' + (d.title ? fitStringToWidth(d.title, 200) : '[no title]') + '</b></td>');
			if (d.title) {
				row.append('<td class="item-title"><div id="id_' + d.id + '" data-structr_type="item" class="node item"><b title="' +  d.title + '" class="title_">' + fitStringToWidth(d.title, 200) + '</b></td>');
			} else {
				row.append('<td><div id="id_' + d.id + '" data-structr_type="item" class="node item"><b title="' +  (d.name ? d.name : '[unnamed]') + '" class="name_">' + (d.name ? fitStringToWidth(d.name, 200) : '[unnamed]') + '</b></td>');
			}
		}

		$('.item-title b', row).on('click', function() {
			_Contents.editItem(d);
		});

		row.append('<td>' + size + '</td>');
		row.append('<td>' + d.type + (d.isThumbnail ? ' thumbnail' : '') + (d.isFile && d.contentType ? ' (' + d.contentType + ')' : '') + '</td>');
		row.append('<td>' + (d.owner ? (d.owner.name ? d.owner.name : '[unnamed]') : '') + '</td>');

		// Change working dir by click on folder icon
		$('#id_' + d.id + '.folder').parent().prev().on('click', function(e) {

			e.preventDefault();
			e.stopPropagation();

			if (d.parentId) {

				fileTree.jstree('open_node', $('#' + d.parentId), function() {

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

		var div = Structr.node(d.id);

		if (!div || !div.length)
			return;

		div.on('remove', function() {
			div.closest('tr').remove();
		});

		_Entities.appendAccessControlIcon(div, d);
		var delIcon = div.children('.delete_icon');
		if (d.isFolder) {

			// ********** Containers **********

			var newDelIcon = '<img title="Delete folder \'' + d.name + '\'" alt="Delete folder \'' + d.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">';
			if (delIcon && delIcon.length) {
				delIcon.replaceWith(newDelIcon);
			} else {
				div.append(newDelIcon);
			}
			div.children('.delete_icon').on('click', function(e) {
				e.stopPropagation();
				_Entities.deleteNode(this, d, true, function() {
					_Contents.refreshTree();
				});
			});

			div.droppable({
				accept: '.folder, .file, .image',
				greedy: true,
				hoverClass: 'nodeHover',
				tolerance: 'pointer',
				drop: function(e, ui) {

					e.preventDefault();
					e.stopPropagation();

					var self = $(this);
					var fileId = Structr.getId(ui.draggable);
					var folderId = Structr.getId(self);
					_Logger.log(_LogType.CONTENTS, 'fileId, folderId', fileId, folderId);
					if (!(fileId === folderId)) {
						var nodeData = {};
						nodeData.id = fileId;
						//addExpandedNode(folderId);

						//selectedElements = $('.node.selected');
						if (selectedElements.length > 1) {

							$.each(selectedElements, function(i, fileEl) {
								var fileId = Structr.getId(fileEl);
								Command.setProperty(fileId, 'parentId', folderId, false, function() {
									$(ui.draggable).remove();
								});

							});
							selectedElements.length = 0;
						} else {
							Command.setProperty(fileId, 'parentId', folderId, false, function() {
								$(ui.draggable).remove();
							});
						}

						_Contents.refreshTree();
					}

					return false;
				}
			});

		} else {

			// ********** Items **********

			div.children('.typeIcon').on('click', function(e) {
				e.stopPropagation();
				window.open(file.path, 'Download ' + file.name);
			});
			var newDelIcon = '<img title="Delete item ' + d.name + '\'" alt="Delete item \'' + d.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">';
			if (delIcon && delIcon.length) {
				delIcon.replaceWith(newDelIcon);
			} else {
				div.append(newDelIcon);
				delIcon = div.children('.delete_icon');
			}
			div.children('.delete_icon').on('click', function(e) {
				e.stopPropagation();
				_Entities.deleteNode(this, d);
			});

			_Contents.appendEditFileIcon(div, d);

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
			//containment: 'body',
			stop: function(e, ui) {
				$(this).show();
				//$('#pages_').droppable('enable').removeClass('nodeHover');
				$(e.toElement).one('click', function(e) {
					e.stopImmediatePropagation();
				});
			},
			helper: function(event) {
				var helperEl = $(this);
				selectedElements = $('.node.selected');
				if (selectedElements.length > 1) {
					selectedElements.removeClass('selected');
					return $('<img class="node-helper" src="icon/page_white_stack.png">');//.css("margin-left", event.clientX - $(event.target).offset().left);
				}
				var hlp = helperEl.clone();
				hlp.find('.button').remove();
				return hlp;
			}
		});

		_Entities.appendEditPropertiesIcon(div, d);
		_Entities.setMouseOver(div);
		_Entities.makeSelectable(div);

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
			_Logger.log(_LogType.CONTENTS, 'content saved');
		}, function() {
			_Logger.log(_LogType.CONTENTS, 'cancelled');
		});

		Command.get(item.id, function(entity) {

			dialogBtn.append('<button id="saveItem" disabled="disabled" class="disabled"> Save </button>');
			dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');

			dialogSaveButton = $('#saveItem', dialogBtn);
			saveAndClose = $('#saveAndClose', dialogBtn);

			Command.query('SchemaNode', 1, 1, 'name', 'asc', { name: entity.type }, function(schemaNodes) {

				schemaNodes[0].schemaProperties.reverse().forEach(function(prop) {

					dialogText.append('<div id="prop-' + prop.id + '" class="prop"><label for="' + prop.id + '"><h3>' + formatKey(prop.name) + '</h3></label></div>');

					var oldVal = entity[prop.name];

					if (prop.propertyType === 'String') {

						if (prop.contentType && prop.contentType === 'text/html') {
							$('#prop-' + prop.id).append('<div class="value-container edit-area">' + (oldVal || '') + '</div>');
							$('#prop-' + prop.id + ' .edit-area').trumbowyg({
								//btns: ['strong', 'em', '|', 'insertImage'],
								//autogrow: true
							}).on('tbwchange', function() {
								Command.get(entity.id, function(newEntity) {
									_Contents.checkValueHasChanged(newEntity[prop.name], $('#prop-' + prop.id + ' .edit-area').trumbowyg('html') || null, [dialogSaveButton, saveAndClose]);
								});
							}).on('tbwpaste', function() {
								Command.get(entity.id, function(newEntity) {
									_Contents.checkValueHasChanged(newEntity[prop.name], $('#prop-' + prop.id + ' .edit-area').trumbowyg('html') || null, [dialogSaveButton, saveAndClose]);
								});
							});

						} else {
							$('#prop-' + prop.id).append('<div class="value-container"><input value="' + (oldVal || '') + '">');
							$('#prop-' + prop.id + ' .value-container').on('keyup', function(e) {
								if (e.keyCode !== 27) {
									Command.get(entity.id, function(newEntity) {
										_Contents.checkValueHasChanged(newEntity[prop.name], $('#prop-' + prop.id + ' .value-container input').val() || null, [dialogSaveButton, saveAndClose]);
									});
								}
							});
						}
					} else if (prop.propertyType === 'Date') {
						$.get(rootUrl + '_schema/' + entity.type + '/ui', function(data) {
							
							var typeInfo = data.result.filter(function(obj) { return obj.jsonName === prop.name; })[0];
							
							console.log(typeInfo);
							$('#prop-' + prop.id).append('<div class="value-container"></div>');
							_Entities.appendDatePicker($('#prop-' + prop.id + ' .value-container'), entity, prop.name, typeInfo.format);
						});
						
					}

				});

			}, true);

			dialogSaveButton.on('click', function(e) {
				
				ignoreKeyUp = false;
				
				e.preventDefault();
				e.stopPropagation();

				Command.query('SchemaNode', 1, 1, 'name', 'asc', { name: entity.type }, function(schemaNodes) {

					schemaNodes[0].schemaProperties.forEach(function(prop) {

						var newVal;
						var oldVal = entity[prop.name];
						
						if (prop.propertyType === 'String') {

							if (prop.contentType && prop.contentType === 'text/html') {
								newVal = $('#prop-' + prop.id + ' .edit-area').trumbowyg('html') || null;
							} else {
								newVal = $('#prop-' + prop.id + ' .value-container input').val() || null;
							}

							//console.log(prop.name, 'Old value:', oldVal, 'New value:', newVal);

							if (newVal !== oldVal) {

								Command.setProperty(entity.id, prop.name, newVal, false, function() {

									oldVal = newVal;
									dialogSaveButton.prop("disabled", true).addClass('disabled');
									saveAndClose.prop("disabled", true).addClass('disabled');

									// update title in list
									if (prop.name === 'title') {
										var f = $('#row' + entity.id + ' .item-title b');
										f.text(fitStringToWidth(newVal, 200));
										blinkGreen(f);
									}
								});
								
							}
						}

					});

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
				}, 500);
			});

		});
		
	},
	appendEditFileIcon: function(parent, item) {

		var editIcon = $('.edit_file_icon', parent);

		if (!(editIcon && editIcon.length)) {
			parent.append('<img title="Edit ' + item.name + ' [' + item.id + ']" alt="Edit ' + item.name + ' [' + item.id + ']" class="edit_file_icon button" src="icon/pencil.png">');
		}

		$(parent.children('.edit_file_icon')).on('click', function(e) {
			e.stopPropagation();

			_Contents.editItem(item);

		});
	},
	displaySearchResultsForURL: function(url) {

		var content = $('#folder-contents');
		$('#search-results').remove();
		content.append('<div id="search-results"></div>');

		var searchString = $('.search', main).val();
		var container = $('#search-results');
		content.on('scroll', function() {
			window.history.pushState('', '', '#filesystem');

		});

		//console.log('Search string:', searchString, url);

		$.ajax({
			url: url,
			statusCode: {
				200: function(data) {

					//console.log(data.result);

					if (!data.result || data.result.length === 0) {
						container.append('<h1>No results for "' + searchString + '"</h1>');
						container.append('<h2>Press ESC or click <a href="#filesystem" class="clear-results">here to clear</a> empty result list.</h2>');
						$('.clear-results', container).on('click', function() {
							_Contents.clearSearch();
						});
						return;
					} else {
						container.append('<h1>' + data.result.length + ' search results:</h1><table class="props"><thead><th class="_type">Type</th><th>Name</th><th>Size</th></thead><tbody></tbody></table>');
						data.result.forEach(function(d) {
							var icon = _Contents.getIcon(d);
							$('tbody', container).append('<tr><td><i class="fa ' + icon + '"></i> ' + d.type + (d.isFile && d.contentType ? ' (' + d.contentType + ')' : '') + '</td><td><a href="#results' + d.id + '">' + d.name + '</a></td><td>' + d.size + '</td></tr>');

						});
					}

					data.result.forEach(function(d) {

						$.ajax({
							url: rootUrl + 'files/' + d.id + '/getSearchContext',
							contentType: 'application/json',
							method: 'POST',
							data: JSON.stringify({searchString: searchString, contextLength: 30}),
							statusCode: {
								200: function(data) {

									if (!data.result) return;

									//console.log(data.result);

									container.append('<div class="search-result collapsed" id="results' + d.id + '"></div>');

									var div = $('#results' + d.id);
									var icon = _Contents.getIcon(d);
									//div.append('<h2><img id="preview' + d.id + '" src="' + icon + '" style="margin-left: 6px;" title="' + d.extractedContent + '" />' + d.path + '</h2>');
									div.append('<h2><i class="fa ' + icon + '"></i> ' + d.name + '<img id="preview' + d.id + '" src="/structr/icon/eye.png" style="margin-left: 6px;" title="' + d.extractedContent + '" /></h2>');
									div.append('<i class="toggle-height fa fa-expand"></i>').append('<i class="go-to-top fa fa-chevron-up"></i>');

									$('.toggle-height', div).on('click', function() {
										var icon = $(this);
										div.toggleClass('collapsed');
										if (icon.hasClass('fa-expand')) {
											icon.removeClass('fa-expand');
											icon.addClass('fa-compress');
										} else {
											icon.removeClass('fa-compress');
											icon.addClass('fa-expand');
										}

									});

									$('.go-to-top', div).on('click', function() {
										content.scrollTop(0);
										window.history.pushState('', '', '#filesystem');
									});

									$.each(data.result.context, function(i, contextString) {

										searchString.split(/[\s,;]/).forEach(function(str) {
											contextString = contextString.replace(new RegExp('(' + str + ')', 'gi'), '<span class="highlight">$1</span>');
										});

										div.append('<div class="part">' + contextString + '</div>');

									});

									div.append('<div style="clear: both;"></div>');
								}
							}
						});

					});
				}
			}

		});
	},
	getIcon: function(file) {
		return (file.isContentContainer ? 'fa-folder-o' : 'fa-file-o');
	},
	updateTextFile: function(file, text) {
		var chunks = Math.ceil(text.length / chunkSize);
		for (var c = 0; c < chunks; c++) {
			var start = c * chunkSize;
			var end = (c + 1) * chunkSize;
			var chunk = utf8_to_b64(text.substring(start, end));
			Command.chunk(file.id, c, chunkSize, chunk, chunks);
		}
	}
};
