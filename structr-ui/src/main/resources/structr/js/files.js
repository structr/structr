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
var main, filesMain, fileTree, folderContents;
var drop;
var fileList;
var chunkSize = 1024 * 64;
var sizeLimit = 1024 * 1024 * 1024;
var selectedElements = [];
var activeFileId, fileContents = {};
var currentWorkingDir;
var folderPageSize = 10000, folderPage = 1;
var filesViewModeKey = 'structrFilesViewMode_' + port;
var timeout, attempts = 0, maxRetry = 10;
var displayingFavorites = false;
var filesLastOpenFolderKey = 'structrFilesLastOpenFolder_' + port;
var filesResizerLeftKey = 'structrFilesResizerLeftKey_' + port;
var activeFileTabPrefix = 'activeFileTabPrefix' + port;

$(document).ready(function() {
	Structr.registerModule(_Files);
});

var _Files = {
	_moduleName: 'files',
	_viewMode: LSWrapper.getItem(filesViewModeKey) || 'list',
	defaultFolderAttributes: 'id,name,type,owner,isFolder,path,visibleToPublicUsers,visibleToAuthenticatedUsers,ownerId,isMounted,parentId,foldersCount,filesCount',
	searchField: undefined,
	getViewMode: function () {
		return _Files._viewMode || 'list';
	},
	setViewMode: function(viewMode) {
		_Files._viewMode = viewMode;
		LSWrapper.setItem(filesViewModeKey, viewMode);
	},
	isViewModeActive: function(viewMode) {
		return (viewMode === _Files.getViewMode());
	},
	init: function() {

		_Files.setViewMode(LSWrapper.getItem(filesViewModeKey) || 'list');

		_Files.searchField = $('.search', main);

		if (_Files.searchField && _Files.searchField.length > 0) {

			_Files.searchField.focus();

			_Files.searchField.keyup(function(e) {

				var searchString = $(this).val();
				if (searchString && searchString.length && e.keyCode === 13) {

					$('.clearSearchIcon').show().on('click', function() {
						_Files.clearSearch();
					});

					_Files.fulltextSearch(searchString);

				} else if (e.keyCode === 27 || searchString === '') {
					_Files.clearSearch();
				}

			});
		}

		Structr.makePagesMenuDroppable();
		Structr.adaptUiToAvailableFeatures();

	},
	resize: function() {

		_Files.moveResizer();
		Structr.resize();

		$('div.xml-mapping').css({ height: dialogBox.height()- 118 });

	},
	moveResizer: function(left) {

		// throttle
		requestAnimationFrame(() => {
			left = left || LSWrapper.getItem(filesResizerLeftKey) || 300;
			$('.column-resizer', filesMain).css({ left: left });

			fileTree.css({width: left - 14 + 'px'});
		});
	},
	onload: async function() {

		let html = await Structr.fetchHtmlTemplate('files/files', {});

		main[0].innerHTML = html;

		_Files.init();

		Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('files'));

		filesMain = $('#files-main');

		fileTree = $('#file-tree');
		folderContents = $('#folder-contents');

		_Files.moveResizer();
		Structr.initVerticalSlider($('.column-resizer', filesMain), filesResizerLeftKey, 204, _Files.moveResizer);

		let newFileButton = await Structr.fetchHtmlTemplate('files/button.file.new', {});

		$('#folder-contents-container').prepend(newFileButton);

		$('.add_file_icon', main).on('click', function(e) {
			Command.create({ type: $('select#file-type').val(), size: 0, parentId: currentWorkingDir ? currentWorkingDir.id : null });
		});

		$('.mount_folder', main).on('click', _Files.openMountDialog);

		$('.add_folder_icon', main).on('click', function(e) {
			Command.create({ type: $('select#folder-type').val(), parentId: currentWorkingDir ? currentWorkingDir.id : null });
		});

		$('select#file-type').on('change', function() {
			$('#add-file-button', main).find('span').text('Add ' + $(this).val());
		});

		$('select#folder-type').on('change', function() {
			$('#add-folder-button', main).find('span').text('Add ' + $(this).val());
		});

		// list types that extend File
		_Schema.getDerivedTypes('org.structr.dynamic.File', ['CsvFile'], function(types) {
			var elem = $('select#file-type');
			types.forEach(function(type) {
				elem.append('<option value="' + type + '">' + type + '</option>');
			});
		});

		// list types that extend folder
		_Schema.getDerivedTypes('org.structr.dynamic.Folder', ['Trash'], function(types) {
			var elem = $('select#folder-type');
			types.forEach(function(type) {
				elem.append('<option value="' + type + '">' + type + '</option>');
			});
		});

		$.jstree.defaults.core.themes.dots      = false;
		$.jstree.defaults.dnd.inside_pos        = 'last';
		$.jstree.defaults.dnd.large_drop_target = true;

		fileTree.on('ready.jstree', function() {
			_TreeHelper.makeTreeElementDroppable(fileTree, 'root');
			_TreeHelper.makeTreeElementDroppable(fileTree, 'favorites');

			_Files.loadAndSetWorkingDir(function() {

				var lastOpenFolder = LSWrapper.getItem(filesLastOpenFolderKey);

				if (lastOpenFolder === 'favorites') {

					$('#favorites_anchor').click();

				} else if (currentWorkingDir) {

					_Files.deepOpen(currentWorkingDir);

				} else {

					$('#root_anchor').click();
				}
			});
		});

		fileTree.on('select_node.jstree', function(evt, data) {

			if (data.node.id === 'favorites') {

				_Files.displayFolderContents('favorites');

			} else {

				_Files.setWorkingDirectory(data.node.id);
				_Files.displayFolderContents(data.node.id, data.node.parent, data.node.original.path, data.node.parents);
			}
		});

		_TreeHelper.initTree(fileTree, _Files.treeInitFunction, 'structr-ui-filesystem');

		_Files.activateUpload();

		$(window).off('resize').resize(function() {
			_Files.resize();
		});

		Structr.unblockMenu(100);

		_Files.resize();
		Structr.adaptUiToAvailableFeatures();

	},
	deepOpen: function(d, dirs) {

		_TreeHelper.deepOpen(fileTree, d, dirs, 'parent', (currentWorkingDir ? currentWorkingDir.id : 'root'));

	},
	refreshTree: function() {

		let selectedId = fileTree.jstree('get_selected');

		_TreeHelper.refreshTree(fileTree, function() {
			_TreeHelper.makeTreeElementDroppable(fileTree, 'root');
			_TreeHelper.makeTreeElementDroppable(fileTree, 'favorites');

			fileTree.jstree('deselect_all');
			fileTree.jstree('activate_node', selectedId);
		});
	},
	refreshNode: function(nodeId, newName) {

		let node = fileTree.jstree('get_node', nodeId);
		node.text = newName;

		_TreeHelper.refreshNode(fileTree, node);
	},
	treeInitFunction: function(obj, callback) {

		switch (obj.id) {

			case '#':

				var defaultFilesystemEntries = [
					{
						id: 'favorites',
						text: 'Favorite Files',
						children: false,
						icon: _Icons.star_icon
					},
					{
						id: 'root',
						text: '/',
						children: true,
						icon: _Icons.structr_logo_small,
						path: '/',
						state: {
							opened: true,
							selected: true
						}
					}
				];

				callback(defaultFilesystemEntries);
				break;

			case 'root':
				_Files.load(null, callback);
				break;

			default:
				_Files.load(obj.id, callback);
				break;
		}

	},
	unload: function() {
		fastRemoveAllChildren($('.searchBox', main));
		fastRemoveAllChildren($('#files-main', main));
	},
	activateUpload: function() {

		if (window.File && window.FileReader && window.FileList && window.Blob) {

			drop = $('#folder-contents');

			drop.on('dragover', function(event) {
				event.originalEvent.dataTransfer.dropEffect = 'copy';
				return false;
			});

			drop.on('drop', function(event) {

				if (!event.originalEvent.dataTransfer) {
					return;
				}

				event.stopPropagation();
				event.preventDefault();

				if (displayingFavorites === true) {
					(new MessageBuilder()).warning("Can't upload to virtual folder Favorites - please first upload file to destination folder and then drag to favorites.").show();
					return;
				}

				fileList = event.originalEvent.dataTransfer.files;
				var filesToUpload = [];
				var tooLargeFiles = [];

				$(fileList).each(function(i, file) {
					if (file.size <= sizeLimit) {
						filesToUpload.push(file);
					} else {
						tooLargeFiles.push(file);
					}
				});

				if (filesToUpload.length < fileList.length) {

					var errorText = 'The following files are too large (limit ' + sizeLimit / (1024 * 1024) + ' Mbytes):<br>\n';

					$(tooLargeFiles).each(function(i, tooLargeFile) {
						errorText += '<b>' + tooLargeFile.name + '</b>: ' + Math.round(tooLargeFile.size / (1024 * 1024)) + ' Mbytes<br>\n';
					});

					new MessageBuilder().error(errorText).title('File(s) too large for upload').requiresConfirmation().show();
				}

				filesToUpload.forEach(function(fileToUpload) {
					fileToUpload.parentId = currentWorkingDir ? currentWorkingDir.id : null;
					fileToUpload.hasParent = true; // Setting hasParent = true forces the backend to upload the file to the root dir even if parentId is null

					Command.createFile(fileToUpload, (createdFileNode) => {
						fileToUpload.id = createdFileNode.id;
						_Files.uploadFile(createdFileNode);
					});
				});

				return false;
			});
		}
	},
	uploadFile: function(file) {
		let worker = new Worker('js/upload-worker.js');
		worker.onmessage = function(e) {

			let binaryContent = e.data;
			let fileSize      = e.data.byteLength;
			let node          = Structr.node(file.id);
			node.find('.size').text(fileSize);

			let chunks = Math.ceil(fileSize / chunkSize);

			for (let c = 0; c < chunks; c++) {
				let start = c * chunkSize;
				let end = (c + 1) * chunkSize;
				let chunk = window.btoa(String.fromCharCode.apply(null, new Uint8Array(binaryContent.slice(start, end))));
				Command.chunk(file.id, c, chunkSize, chunk, chunks);
			}
		};

		$(fileList).each(function(i, fileObj) {
			if (file.id === fileObj.id) {
				worker.postMessage(fileObj);
			}
		});
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
			url = rootUrl + 'files/ui?loose=1&indexedWords=' + searchString;
		}

		_Files.displaySearchResultsForURL(url);
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

		var displayFunction = function (folders) {

			var list = [];

			folders.forEach(function(d) {
				list.push({
					id: d.id,
					text:  d.name || '[unnamed]',
					children: d.foldersCount > 0,
					icon: 'fa fa-folder',
					path: d.path
				});
			});

			callback(list);

			_TreeHelper.makeDroppable(fileTree, list);
		};

		if (!id) {
			Command.list('Folder', true, folderPageSize, folderPage, 'name', 'asc', _Files.defaultFolderAttributes, displayFunction);
		} else {
			Command.query('Folder', folderPageSize, folderPage, 'name', 'asc', {parent: id}, displayFunction, true, 'public', _Files.defaultFolderAttributes);
		}
	},
	setWorkingDirectory: function(id) {

		if (id === 'root') {
			currentWorkingDir = null;
		} else {
			currentWorkingDir = { id: id };
		}

		$.ajax({
			url: rootUrl + 'me',
			dataType: 'json',
			contentType: 'application/json; UTF-8',
			type: 'PUT',
			data: JSON.stringify({'workingDirectory': currentWorkingDir})
		});
	},
	registerFolderLinks: function() {

		$('.is-folder.file-icon', folderContents).off('click').on('click', function (e) {
			e.preventDefault();
			e.stopPropagation();

			let el = $(this);
			let targetId = el.data('targetId');

			let openTargetNode = () => {
				fileTree.jstree('open_node', targetId, () => {
					fileTree.jstree('activate_node', targetId);
				});
			};

			let parentId = el.data('parentId');

			if (!parentId || fileTree.jstree('is_open', parentId)) {
				openTargetNode();
			} else {
				fileTree.jstree('open_node', parentId, openTargetNode);
			}

		});
	},
	displayFolderContents: function(id, parentId, nodePath, parents) {

		fastRemoveAllChildren(folderContents[0]);

		LSWrapper.setItem(filesLastOpenFolderKey, id);

		displayingFavorites = (id === 'favorites');
		var isRootFolder = (id === 'root');
		var parentIsRoot = (parentId === '#');

		_Files.insertLayoutSwitches(id, parentId, nodePath, parents);

		// store current folder id so we can filter slow requests
		folderContents.data('currentFolder', id);

		let handleChildren = function(children) {

			let currentFolder = folderContents.data('currentFolder');

			if (currentFolder === id) {

				if (children && children.length) {
					children.forEach(_Files.appendFileOrFolder);
				}

				_Files.resize();
				_Files.registerFolderLinks();
			}
		};

		if (displayingFavorites === true) {

			$('#folder-contents-container > button').addClass('disabled').attr('disabled', 'disabled');

			folderContents.append('<h2><i class="' + _Icons.getFullSpriteClass(_Icons.star_icon) + '" /> Favorite Files</h2>');

			if (_Files.isViewModeActive('list')) {

				folderContents.append('<table id="files-table" class="stripe"><thead><tr><th class="icon">&nbsp;</th><th>Name</th><th>Size</th><th>Type</th><th>Owner</th></tr></thead>'
					+ '<tbody id="files-table-body"></tbody></table>');
			}

			$.ajax({
				url: rootUrl + 'me/favorites',
				statusCode: {
					200: function(data) {
						handleChildren(data.result);
					}
				}
			});

		} else {

			$('#folder-contents-container > button').removeClass('disabled').attr('disabled', null);

			if (isRootFolder) {
				Command.list('Folder', true, 1000, 1, 'name', 'asc', _Files.defaultFolderAttributes, handleChildren);
			} else {
				Command.query('Folder', 1000, 1, 'name', 'asc', {parentId: id}, handleChildren, true, null, _Files.defaultFolderAttributes);
			}

			_Pager.initPager('filesystem-files', 'File', 1, 25, 'name', 'asc');
			page['File'] = 1;

			var filterOptions = {
				parentId: (parentIsRoot ? '' : id),
				hasParent: (!parentIsRoot)
			};

			let pagerId = 'filesystem-files';
			_Pager.initFilters(pagerId, 'File', filterOptions, ['parentId', 'hasParent', 'isThumbnail']);

			let filesPager = _Pager.addPager(pagerId, folderContents, false, 'File', 'public', handleChildren, null, 'id,name,type,contentType,isFile,isImage,isThumbnail,tnSmall,tnMid,path,size,owner,visibleToPublicUsers,visibleToAuthenticatedUsers');

			filesPager.cleanupFunction = function () {
				var toRemove = $('.node.file', filesPager.el).closest( (_Files.isViewModeActive('list') ? 'tr' : '.tile') );
				toRemove.each(function(i, elem) {
					fastRemoveAllChildren(elem);
					elem.remove();
				});
			};

			filesPager.pager.append('Filter: <input type="text" class="filter" data-attribute="name">');
			filesPager.pager.append('<input type="text" class="filter" data-attribute="parentId" value="' + (parentIsRoot ? '' : id) + '" hidden>');
			filesPager.pager.append('<input type="checkbox" class="filter" data-attribute="hasParent" ' + (parentIsRoot ? '' : 'checked') + ' hidden>');
			filesPager.activateFilterElements();

			_Files.insertBreadCrumbNavigation(parents, nodePath);

			if (_Files.isViewModeActive('list')) {
				folderContents.append('<table id="files-table" class="stripe"><thead><tr><th class="icon">&nbsp;</th><th>Name</th><th>Size</th><th>Type</th><th>Owner</th></tr></thead>'
					+ '<tbody id="files-table-body">'
					+ (!isRootFolder ? '<tr><td class="is-folder file-icon" data-target-id="' + parentId + '"><i class="fa fa-folder"></i></td><td><a href="#">..</a></td><td></td><td></td><td></td></tr>' : '')
					+ '</tbody></table>');

			} else if (_Files.isViewModeActive('tiles')) {
				if (!isRootFolder) {
					folderContents.append('<div class="tile"><div class="node folder"><div class="is-folder file-icon" data-target-id="' + parentId + '"><i class="fa fa-folder"></i></div><b title="..">..</b></div></div>');
				}
			} else if (_Files.isViewModeActive('img')) {
				if (!isRootFolder) {
					folderContents.append('<div class="tile img-tile"><div class="node folder"><div class="is-folder file-icon" data-target-id="' + parentId + '"><i class="fa fa-folder"></i></div><b title="..">..</b></div></div>');
				}
			}
		}
	},
	insertBreadCrumbNavigation: function(parents, nodePath) {

		if (parents) {

			let path = '';

			parents = [].concat(parents).reverse().slice(1);
			let pathNames = ['/'];
			if (nodePath !== '/') {
				pathNames = pathNames.concat(nodePath.slice(1).split('/'));
			}
			path = parents.map(function(parent, idx) {
				return '<a class="breadcrumb-entry" data-folder-id="' + parent + '"><i class="fa fa-caret-right"></i> ' + pathNames[idx] + '</a>';
			}).join(' ');

			path += ' <i class="fa fa-caret-right"></i> ' + pathNames.pop();

			folderContents.append('<h2>' + path + '</h2>');

			$('.breadcrumb-entry').click(function (e) {
				e.preventDefault();

				$('#' + $(this).data('folderId') + '_anchor').click();
			});
		}
	},
	insertLayoutSwitches: function (id, parentId, nodePath, parents) {

		folderContents.prepend('<div id="switches">'
			+ '<button class="switch ' + (_Files.isViewModeActive('list') ? 'active' : 'inactive') + '" id="switch-list" data-view-mode="list">' + (_Files.isViewModeActive('list') ? '<i class="' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" />' : '') + ' List</button>'
			+ '<button class="switch ' + (_Files.isViewModeActive('tiles') ? 'active' : 'inactive') + '" id="switch-tiles" data-view-mode="tiles">'	+ (_Files.isViewModeActive('tiles') ? '<i class="' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" />' : '') + ' Tiles</button>'
			+ '<button class="switch ' + (_Files.isViewModeActive('img') ? 'active' : 'inactive') + '" id="switch-img" data-view-mode="img">' + (_Files.isViewModeActive('img') ? '<i class="' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" />' : '') + ' Images</button>'
			+ '</div>');

		var listSw  = $('#switch-list');
		var tilesSw = $('#switch-tiles');
		var imgSw   = $('#switch-img');

		var layoutSwitchFunction = function() {
			var state = $(this).hasClass('inactive');
			if (state) {
				var viewMode = $(this).data('viewMode');
				_Files.setViewMode(viewMode);

				_Entities.changeBooleanAttribute(listSw,  _Files.isViewModeActive('list'),  'List',   'List');
				_Entities.changeBooleanAttribute(tilesSw, _Files.isViewModeActive('tiles'), 'Tiles',  'Tiles');
				_Entities.changeBooleanAttribute(imgSw,   _Files.isViewModeActive('img'),   'Images', 'Images');

				_Files.displayFolderContents(id, parentId, nodePath, parents);
			}
		};

		listSw.on('click', layoutSwitchFunction);
		tilesSw.on('click', layoutSwitchFunction);
		imgSw.on('click', layoutSwitchFunction);
	},
	fileOrFolderCreationNotification: function (newFileOrFolder) {
		if ((currentWorkingDir === undefined || currentWorkingDir === null) && newFileOrFolder.parent === null) {
			_Files.appendFileOrFolder(newFileOrFolder);
		} else if ((currentWorkingDir !== undefined && currentWorkingDir !== null) && newFileOrFolder.parent && currentWorkingDir.id === newFileOrFolder.parent.id) {
			_Files.appendFileOrFolder(newFileOrFolder);
		}
	},
	appendFileOrFolder: function(d) {

		if (!d.isFile && !d.isFolder) return;

		StructrModel.createFromData(d, null, false);

		let size = d.isFolder ? (d.foldersCount + d.filesCount) : (d.size ? d.size : '-');
		let icon = d.isFolder ? 'fa-folder' : _Icons.getFileIconClass(d);
		let name = d.name ? d.name : '[unnamed]';

		if (_Files.isViewModeActive('list')) {

			let tableBody = $('#files-table-body');

			$('#row' + d.id, tableBody).remove();

			let rowId = 'row' + d.id;
			tableBody.append('<tr id="' + rowId + '"' + (d.isThumbnail ? ' class="thumbnail"' : '') + '></tr>');
			let row = $('#' + rowId);

			if (d.isFolder) {
				let icon_element = (d.isMounted) ? '<span class="fa-stack"><i class="fa ' + icon + ' fa-stack-2x"></i><i class="fa fa-plug fa-stack-1x"></i></span>' : '<i class="fa ' + icon + '"></i>';
				row.append('<td class="is-folder file-icon" data-target-id="' + d.id + '" data-parent-id="' + d.parentId + '">' + icon_element + '</td>');
				row.append('<td><div id="id_' + d.id + '" class="node folder"><b title="' + escapeForHtmlAttributes(name) + '" class="name_ abbr-ellipsis abbr-75pc">' + name + '</b></div></td>');
			} else {
				row.append('<td class="file-icon"><a href="' + d.path + '" target="_blank"><i class="fa ' + icon + '"></i></a></td>');
				row.append('<td><div id="id_' + d.id + '" class="node file"><b title="' + escapeForHtmlAttributes(name) + '" class="name_ abbr-ellipsis abbr-75pc">' + name + '</b>'
				+ '<div class="progress"><div class="bar"><div class="indicator"><span class="part"></span>/<span class="size">' + d.size + '</span></div></div></div><span class="id">' + d.id + '</span></div></td>');
			}

			row.append('<td class="size">' + size + '</td>');
			row.append('<td>' + d.type + (d.isThumbnail ? ' thumbnail' : '') + (d.isFile && d.contentType ? ' (' + d.contentType + ')' : '') + '</td>');
			row.append('<td>' + (d.owner ? (d.owner.name ? d.owner.name : '[unnamed]') : '') + '</td>');

		} else if (_Files.isViewModeActive('tiles')) {

			let tileId = 'tile' + d.id;
			folderContents.append('<div id="' + tileId + '" class="tile' + (d.isThumbnail ? ' thumbnail' : '') + '"></div>');
			let tile = $('#' + tileId);

			if (d.isFolder) {

				let icon_element = (d.isMounted) ? '<span class="fa-stack"><i class="fa ' + icon + ' fa-stack-1x"></i><i class="fa fa-plug fa-stack-1x"></i></span>' : '<i class="fa ' + icon + '"></i>';

				tile.append('<div id="id_' + d.id + '" class="node folder"><div class="is-folder file-icon" data-target-id="' + d.id + '" data-parent-id="' + d.parentId + '">' + icon_element + '</div><b title="' + escapeForHtmlAttributes(name) + '" class="name_ abbr-ellipsis abbr-75pc">' + name + '</b></div>');

			} else {

				let iconOrThumbnail = d.isImage && !d.isThumbnail && d.tnSmall ? '<img class="tn" src="' + d.tnSmall.path + '">' : '<i class="fa ' + icon + '"></i>';

				tile.append('<div id="id_' + d.id + '" class="node file"><div class="file-icon"><a href="' + d.path + '" target="_blank">' + iconOrThumbnail + '</a></div>'
					+ '<b title="' + escapeForHtmlAttributes(name) + '" class="name_ abbr-ellipsis abbr-75pc">' + name + '</b>'
					+ '<div class="progress"><div class="bar"><div class="indicator"><span class="part"></span>/<span class="size">' + size + '</span></div></div></div><span class="id">' + d.id + '</span></div>');
			}

		} else if (_Files.isViewModeActive('img')) {

			let tileId = 'tile' + d.id;
			folderContents.append('<div id="' + tileId + '" class="tile img-tile' + (d.isThumbnail ? ' thumbnail' : '') + '"></div>');
			let tile = $('#' + tileId);

			if (d.isFolder) {

				let icon_element = (d.isMounted) ? '<span class="fa-stack"><i class="fa ' + icon + ' fa-stack-1x"></i><i class="fa fa-plug fa-stack-1x"></i></span>' : '<i class="fa ' + icon + '"></i>';

				tile.append('<div id="id_' + d.id + '" class="node folder"><div class="is-folder file-icon" data-target-id="' + d.id + '" data-parent-id="' + d.parentId + '">' + icon_element + '</div><b title="' + escapeForHtmlAttributes(name) + '" class="name_ abbr-ellipsis abbr-75pc">' + name + '</b></div>');

			} else {

				let iconOrThumbnail = d.isImage && !d.isThumbnail && d.tnMid ? '<img class="tn" src="' + d.tnMid.path + '">' : '<i class="fa ' + icon + '"></i>';

				tile.append('<div id="id_' + d.id + '" class="node file"><div class="file-icon"><a href="' + d.path + '" target="_blank">' + iconOrThumbnail + '</a></div>'
					+ '<b title="' + escapeForHtmlAttributes(name) + '" class="name_  abbr-ellipsis abbr-75pc">' + name + '</b>'
					+ '<div class="progress"><div class="bar"><div class="indicator"><span class="part"></span>/<span class="size">' + size + '</span></div></div></div><span class="id">' + d.id + '</span></div>');
			}
		}

		let div = Structr.node(d.id);

		if (!div || !div.length)
			return;

		_Entities.setMouseOver(div, true);

		div.on('remove', function() {
			div.closest('tr').remove();
		});

		_Entities.appendAccessControlIcon(div, d);

		if (d.isFolder) {
			_Files.handleFolder(div, d);
		} else {
			_Files.handleFile(div, d);
		}

		div.draggable({
			revert: 'invalid',
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
				let helperEl = $(this);
				selectedElements = $('.node.selected');
				if (selectedElements.length > 1) {
					selectedElements.removeClass('selected');
					return $('<i class="node-helper ' + _Icons.getFullSpriteClass(_Icons.page_white_stack_icon) + '" />');
				}
				let hlp = helperEl.clone();
				hlp.find('.button').remove();
				return hlp;
			}
		});

		_Entities.appendEditPropertiesIcon(div, d);
		_Entities.makeSelectable(div);
	},
	handleFolder: function(div, d) {

		if (Structr.isModulePresent('cloud')) {
			div.append('<i title="Sync folder \'' + d.name + '\' to remote instance" class="push_icon button ' + _Icons.getFullSpriteClass(_Icons.push_file_icon) + '" />');
			div.children('.push_icon').on('click', function() {
				Structr.pushDialog(d.id, true);
				return false;
			});
		}

		var delIcon = $('.delete_icon', div);
		var newDelIcon = $('<i title="Delete folder \'' + d.name + '\'" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />');

		if (delIcon && delIcon.length) {
			delIcon.replaceWith(newDelIcon);
		} else {
			div.append(newDelIcon);
		}
		newDelIcon.on('click', function(e) {
			e.stopPropagation();

			selectedElements = $('.node.selected');
			var selectedCount = selectedElements.length;

			if (selectedCount > 1 && div.hasClass('selected')) {

				var files = [];

				$.each(selectedElements, function(i, el) {
					files.push(Structr.entityFromElement(el));
				});

				_Entities.deleteNodes(this, files, true, function() {
					_Files.refreshTree();
				});

			} else {
				_Entities.deleteNode(this, d, true, function() {
					_Files.refreshTree();
				});
			}
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
				if (!(fileId === folderId)) {
					var nodeData = {};
					nodeData.id = fileId;

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

					_Files.refreshTree();
				}

				return false;
			}
		});
	},
	handleFile: function(div, d) {

		if (Structr.isModulePresent('cloud') && !_Files.isViewModeActive('img')) {
			div.append('<i title="Sync file \'' + d.name + '\' to remote instance" class="push_icon button ' + _Icons.getFullSpriteClass(_Icons.push_file_icon) + '" />');
			div.children('.push_icon').on('click', function() {
				Structr.pushDialog(d.id, false);
				return false;
			});
		}

		if (_Files.isArchive(d)) {
			div.append('<i class="unarchive_icon button ' + _Icons.getFullSpriteClass(_Icons.compress_icon) + '" />');
			$('.unarchive_icon', div).on('click', function() {

				$('#tempInfoBox .infoHeading, #tempInfoBox .infoMsg').empty();
				$('#tempInfoBox .closeButton').hide();
				Structr.loaderIcon($('#tempInfoBox .infoMsg'), { marginBottom: '-6px' });
				$('#tempInfoBox .infoMsg').append(' Unpacking Archive - please stand by...');
				$('#tempInfoBox .infoMsg').append('<p>Extraction will run in the background.<br>You can safely close this popup and work during this operation.<br>You will be notified when the extraction has finished.</p>');

				$.blockUI({
					message: $('#tempInfoBox'),
					css: Structr.defaultBlockUICss
				});

				var closed = false;
				window.setTimeout(function() {
					$('#tempInfoBox .closeButton').show().on('click', function () {
						closed = true;
						$.unblockUI({
							fadeOut: 25
						});
					});
				}, 500);

				Command.unarchive(d.id, currentWorkingDir ? currentWorkingDir.id : undefined, function (data) {
					if (data.success === true) {
						_Files.refreshTree();
						var message = "Extraction of '" + data.filename + "' finished successfully. ";
						if (closed) {
							new MessageBuilder().success(message).requiresConfirmation("Close").show();
						} else {
							$('#tempInfoBox .infoMsg').html('<i class="' + _Icons.getFullSpriteClass(_Icons.accept_icon) + '" /> ' + message);
						}

					} else {
						$('#tempInfoBox .infoMsg').html('<i class="' + _Icons.getFullSpriteClass(_Icons.error_icon) + '" /> Extraction failed');
					}
				});
			});
		}

		if (displayingFavorites === true) {

			_Files.appendRemoveFavoriteIcon(div, d);

		} else {

			div.append('<i title="Delete file \'' + d.name + '\'" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />');
			$('.delete_icon', div).on('click', function(e) {
				e.stopPropagation();

				selectedElements = $('.node.selected');
				var selectedCount = selectedElements.length;

				if (selectedCount > 1 && div.hasClass('selected')) {

					var files = [];

					$.each(selectedElements, function(i, el) {
						files.push(Structr.entityFromElement(el));
					});

					_Entities.deleteNodes(this, files, true, function() {
						_Files.refreshTree();
					});

				} else {
					_Entities.deleteNode(this, d);
				}
			});
		}

		if (_Files.isMinificationTarget(d)) {
			_Files.appendMinificationDialogIcon(div, d);
		} else {
			if (d.isImage) {
				_Files.appendEditImageIcon(div, d);
			} else {
				_Files.appendEditFileIcon(div, d);
			}
		}

		Structr.performModuleDependendAction(function() {
			if (Structr.isModulePresent('csv') && Structr.isModulePresent('api-builder') && d.contentType === 'text/csv') {
				_Files.appendCSVImportDialogIcon(div, d);
			}
		});

		Structr.performModuleDependendAction(function() {
			if (Structr.isModulePresent('xml') && (d.contentType === 'text/xml' || d.contentType === 'application/xml')) {
				_Files.appendXMLImportDialogIcon(div, d);
			}
		});
	},
	appendEditImageIcon: function(parent, image) {

		var viewIcon = $('.view_icon', parent);

		if (!(viewIcon && viewIcon.length)) {
			parent.append('<i title="' + image.name + ' [' + image.id + ']" class="edit_icon button ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" />');
		}

		viewIcon = $('.edit_icon', parent);

		viewIcon.on('click', function(e) {
			e.stopPropagation();
			Structr.dialog('' + image.name, function() {
			}, function() {
			});
			_Files.viewImage(image, $('#dialogBox .dialogText'));
		});
	},
	viewImage: function(image, el) {
		dialogMeta.hide();

		el.append('<div class="image-editor-menubar ">'
			+ '<div><i class="fa fa-crop"></i><br>Crop</div>'
			+ '</div><div><img id="image-editor" class="orientation-' + image.orientation + '" src="' + image.path + '"></div>');

		var x,y,w,h;

		dialogBtn.children('#saveFile').remove();
		dialogBtn.children('#saveAndClose').remove();

		dialogBtn.append('<button id="saveFile" disabled="disabled" class="disabled">Save</button>');
		dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled">Save and close</button>');

		dialogSaveButton = $('#saveFile', dialogBtn);
		saveAndClose = $('#saveAndClose', dialogBtn);

		$('button#saveFile', dialogBtn).on('click', function(e) {
			e.preventDefault();
			e.stopPropagation();
			Command.createConvertedImage(image.id, Math.round(w), Math.round(h), null, Math.round(x), Math.round(y), function() {
				dialogSaveButton.prop("disabled", true).addClass('disabled');
				saveAndClose.prop("disabled", true).addClass('disabled');
			});
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

		$('.fa-crop', el).on('click', function() {

			$('#image-editor').cropper({
			  crop: function(e) {

				x = e.x, y = e.y, w = e.width, h = e.height;

				dialogSaveButton.prop("disabled", false).removeClass('disabled');
				saveAndClose.prop("disabled", false).removeClass('disabled');
			  }
			});
		});
	},
	appendEditFileIcon: function(parent, file) {

		var editIcon = $('.edit_file_icon', parent);

		if (!(editIcon && editIcon.length)) {
			parent.append('<i title="Edit ' + file.name + ' [' + file.id + ']" class="edit_file_icon button ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" />');
		}

		$(parent.children('.edit_file_icon')).on('click', function(e) {
			e.stopPropagation();

			fileContents = {};
			editor = undefined;

			selectedElements = $('.node.selected');
			if (selectedElements.length > 1 && parent.hasClass('selected')) {
				selectedElements.removeClass('selected');
			} else {
				selectedElements = parent;
			}

			Structr.dialog('Edit files', function() {
			}, function() {
			});

			dialogText.append('<div id="files-tabs" class="files-tabs"><ul></ul></div>');

			var selectedCount = selectedElements.length;
			$.each(selectedElements, function(i, el) {
				if (_Files.isMinificationTarget(StructrModel.obj(Structr.getId(el)))) {
					selectedCount--;
				}
			});

			$.each(selectedElements, function(i, el) {

				if (!_Files.isMinificationTarget(StructrModel.obj(Structr.getId(el)))) {

					Command.get(Structr.getId(el), 'id,name,contentType,isTemplate', function(entity) {
						$('#files-tabs ul').append('<li id="tab-' + entity.id + '">' + entity.name + '</li>');
						$('#files-tabs').append('<div id="content-tab-' + entity.id + '"></div>');

						$('#tab-' + entity.id).on('click', function(e) {

							e.stopPropagation();

							// Store current editor text
							if (editor) {
								fileContents[activeFileId] = editor.getValue();
							}

							activeFileId = Structr.getIdFromPrefixIdString($(this).prop('id'), 'tab-');
							$('#content-tab-' + activeFileId).empty();
							_Files.editContent(null, entity, $('#content-tab-' + activeFileId));

							return false;
						});

						if (i+1 === selectedCount) {
							_Entities.activateTabs(file.id, '#files-tabs', '#content-tab-' + file.id, activeFileTabPrefix);
						}
					});
				}
			});
		});
	},
	appendMinificationDialogIcon: function(parent, file) {

		parent.append('<i title="Open minification dialog" class="minify_file_icon button ' + _Icons.getFullSpriteClass(_Icons.getMinificationIcon(file)) + '" />');
		$('.minify_file_icon', parent).on('click', function(e) {
			e.stopPropagation();

			_Minification.showMinificationDialog(file);
		});
	},
	appendCSVImportDialogIcon: function(parent, file) {

		parent.append(' <i class="import_icon button ' + _Icons.getFullSpriteClass(_Icons.import_icon) + '" title="Import this CSV file" />');
		$('.import_icon', parent).on('click', function() {
			Importer.importCSVDialog(file, false);
			return false;
		});
	},
	appendXMLImportDialogIcon: function(parent, file) {

		parent.append(' <i class="import_icon button ' + _Icons.getFullSpriteClass(_Icons.import_icon) + '" title="Import this XML file" />');
		$('.import_icon', parent).on('click', function() {
			Importer.importXMLDialog(file, false);
			return false;
		});
	},
	appendRemoveFavoriteIcon: function (parent, file) {

		parent.append('<i title="Remove from favorites" class="remove_favorite_icon button ' + _Icons.getFullSpriteClass(_Icons.star_delete_icon) + '" />');
		$('.remove_favorite_icon', parent).on('click', function(e) {
			e.stopPropagation();

			Command.favorites('remove', file.id, function() {
				parent.remove();
			});
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

		$.ajax({
			url: url,
			statusCode: {
				200: function(data) {

					if (!data.result || data.result.length === 0) {

						container.append('<h1>No results for "' + searchString + '"</h1>');
						container.append('<h2>Press ESC or click <a href="#filesystem" class="clear-results">here to clear</a> empty result list.</h2>');
						$('.clear-results', container).on('click', function() {
							_Files.clearSearch();
						});

						return;

					} else {

						container.append('<h1>' + data.result.length + ' result' + (data.result.length > 1 ? 's' : '') + ':</h1><table class="props"><thead><th class="_type">Type</th><th>Name</th><th>Size</th></thead><tbody></tbody></table>');
						data.result.forEach(function(d) {

							$('tbody', container).append('<tr><td><i class="fa ' + _Icons.getFileIconClass(d) + '"></i> ' + d.type + (d.isFile && d.contentType ? ' (' + d.contentType + ')' : '') + '</td><td><a href="#results' + d.id + '">' + d.name + '</a></td><td>' + d.size + '</td></tr>');

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

									if (!data.result) {
										return;
									}

									container.append('<div class="search-result collapsed" id="results' + d.id + '"></div>');

									var div = $('#results' + d.id);

									div.append('<h2><i class="fa ' + _Icons.getFileIconClass(d) + '"></i> ' + d.name + '<i id="preview' + d.id + '" class="' + _Icons.getFullSpriteClass(_Icons.eye_icon) + '" style="margin-left: 6px;" title="' + d.extractedContent + '" /></h2>');
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
	updateTextFile: function(file, text, callback) {
		if (text === "") {
			Command.chunk(file.id, 0, chunkSize, "", 1, callback);
		} else {
			var chunks = Math.ceil(text.length / chunkSize);
			for (var c = 0; c < chunks; c++) {
				var start = c * chunkSize;
				var end = (c + 1) * chunkSize;
				var chunk = utf8_to_b64(text.substring(start, end));
				Command.chunk(file.id, c, chunkSize, chunk, chunks, ((c+1 === chunks) ? callback : undefined));
			}
		}
	},
	editContent: function(button, file, element) {
		var url = viewRootUrl + file.id + '?edit=1';

		var text = '';

		var contentType = file.contentType;
		var dataType = 'text';

		if (!contentType) {
			if (file.name.endsWith('.css')) {
				contentType = 'text/css';
			} else if (file.name.endsWith('.js')) {
				contentType = 'text/javascript';
			} else {
				contentType = 'text/plain';
			}
		}

		$.ajax({
			url: url,
			dataType: dataType,
			contentType: contentType,
			success: function(data) {
				text = fileContents[file.id] || data;
				if (Structr.isButtonDisabled(button)) {
					return;
				}
				element.append('<div class="editor"></div><div id="template-preview"><textarea readonly></textarea></div>');
				var contentBox = $('.editor', element);

				CodeMirror.defineMIME("text/html", "htmlmixed-structr");
				editor = CodeMirror(contentBox.get(0), Structr.getCodeMirrorSettings({
					value: text,
					mode: contentType,
					lineNumbers: true,
					lineWrapping: false,
					indentUnit: 4,
					tabSize: 4,
					indentWithTabs: true,
					extraKeys: {
						"Ctrl-Space": "autocomplete"
					}
				}));
				_Code.setupAutocompletion(editor, file.id, false);

				var scrollInfo = JSON.parse(LSWrapper.getItem(scrollInfoKey + '_' + file.id));
				if (scrollInfo) {
					editor.scrollTo(scrollInfo.left, scrollInfo.top);
				}

				editor.on('scroll', function() {
					var scrollInfo = editor.getScrollInfo();
					LSWrapper.setItem(scrollInfoKey + '_' + file.id, JSON.stringify(scrollInfo));
				});

				editor.id = file.id;

				dialogBtn.children('#saveFile').remove();
				dialogBtn.children('#saveAndClose').remove();

				var h = '<span class="editor-info"><label for="lineWrapping">Line Wrapping: <input id="lineWrapping" type="checkbox"' + (Structr.getCodeMirrorSettings().lineWrapping ? ' checked="checked" ' : '') + '></label>&nbsp;&nbsp;'
				+ '<label for="isTemplate">Replace template expressions:</label> <input id="isTemplate" type="checkbox"><label for="showTemplatePreview">Show preview:</label> <input id="showTemplatePreview" type="checkbox"></span>';
				dialogMeta.html(h);

				let isTemplateCheckbox   = $('#isTemplate').prop('checked', file.isTemplate);
				let showPreviewCheckbox  = $('#showTemplatePreview');

				Structr.appendInfoTextToElement({
					text: "Expressions like <pre>Hello ${print(me.name)} !</pre> will be evaluated. To see a preview, tick the adjacent checkbox.",
					element: isTemplateCheckbox,
					insertAfter: true,
					css: {
						"margin-right": "4px"
					}
				});


				let isTemplateCheckboxChangeFunction = function(isTemplate) {
					if (isTemplate) {
						showPreviewCheckbox.attr('disabled', null);
					} else {
						showPreviewCheckbox.attr('disabled', 'disabled');
					}
				};
				isTemplateCheckboxChangeFunction(file.isTemplate);

				$('#lineWrapping').off('change').on('change', function() {
					var inp = $(this);
					Structr.updateCodeMirrorOptionGlobally('lineWrapping', inp.is(':checked'));
					blinkGreen(inp.parent());
					editor.refresh();
				});

				isTemplateCheckbox.on('change', function() {
					var active = isTemplateCheckbox.is(':checked');
					_Entities.setProperty(file.id, 'isTemplate', active, false, function() {
						isTemplateCheckboxChangeFunction(active);
					});
				});

				showPreviewCheckbox.on('change', function() {
					var active = showPreviewCheckbox.is(':checked');
					if (active) {
						_Files.updateTemplatePreview(element, url, dataType, contentType);
					} else {
						var previewArea = $('#template-preview');
						previewArea.hide();
						$('textarea', previewArea).val('');
						$('.editor', element).width('inherit');
					}
				});

				dialogBtn.append('<button id="saveFile" disabled="disabled" class="disabled">Save</button>');
				dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled">Save and close</button>');

				dialogSaveButton = $('#saveFile', dialogBtn);
				saveAndClose = $('#saveAndClose', dialogBtn);

				editor.on('change', function(cm, change) {

					if (text === editor.getValue()) {
						dialogSaveButton.prop("disabled", true).addClass('disabled');
						saveAndClose.prop("disabled", true).addClass('disabled');
					} else {
						dialogSaveButton.prop("disabled", false).removeClass('disabled');
						saveAndClose.prop("disabled", false).removeClass('disabled');
					}
				});

				$('button#saveFile', dialogBtn).on('click', function(e) {

					e.preventDefault();
					e.stopPropagation();

					var newText = editor.getValue();
					if (text === newText) {
						return;
					}

					let saveFileAction = function (callback) {

						_Files.updateTextFile(file, newText, callback);
						text = newText;
						dialogSaveButton.prop("disabled", true).addClass('disabled');
						saveAndClose.prop("disabled", true).addClass('disabled');
					};

					if ($('#isTemplate').is(':checked')) {

						_Entities.setProperty(file.id, 'isTemplate', false, false, function() {

							saveFileAction(function() {

								_Entities.setProperty(file.id, 'isTemplate', true);
							});
						});

					} else {

						saveFileAction();
					}
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

				_Files.resize();
			},
			error: function(xhr, statusText, error) {
				console.log(xhr, statusText, error);
			}
		});
	},
	updateTemplatePreview: function(element, url, dataType, contentType) {
		$.ajax({
			url: url.substr(0, url.indexOf('?')),
			dataType: dataType,
			contentType: contentType,
			success: function(data) {
				var contentBox = $('.editor', element);
				contentBox.width('50%');
				var previewArea = $('#template-preview');
				$('textarea', previewArea).val(data);
				previewArea.show();
			}
		});
	},
	isArchive: function(file) {
		var contentType = file.contentType;
		var extension = file.name.substring(file.name.lastIndexOf('.') + 1);

		var archiveTypes = ['application/zip', 'application/x-tar', 'application/x-cpio', 'application/x-dump', 'application/x-java-archive', 'application/x-7z-compressed', 'application/x-ar', 'application/x-arj'];
		var archiveExtensions = ['zip', 'tar', 'cpio', 'dump', 'jar', '7z', 'ar', 'arj'];

		return isIn(contentType, archiveTypes) || isIn(extension, archiveExtensions);
	},
	isMinificationTarget: function(file) {
		var minifyTypes = [ 'MinifiedCssFile', 'MinifiedJavaScriptFile' ];
		return isIn(file.type, minifyTypes);
	},
	openMountDialog: function() {

		_Schema.getTypeInfo('Folder', async function(typeInfo) {

			let html = await Structr.fetchHtmlTemplate('files/dialog.mount', {typeInfo: typeInfo});

			Structr.dialog('Mount Folder', function(){}, function(){});

			var elem = $(html);

			$('[data-info-text]', elem).each(function(i, el) {
				Structr.appendInfoTextToElement({
					element: $(el),
					text: $(el).data('info-text'),
					css: { marginLeft: "5px" }
				});
			});

			dialogText.append(elem);

			var mountButton = $('<button id="mount-folder">Mount</button>').on('click', function() {

				var mountConfig = {};
				$('.mount-option[type="text"]').each(function(i, el) {
					var val = $(el).val();
					if (val !== "") {
						mountConfig[$(el).data('attributeName')] = val;
					}
				});
				$('.mount-option[type="number"]').each(function(i, el) {
					var val = $(el).val();
					if (val !== "") {
						mountConfig[$(el).data('attributeName')] = parseInt(val);
					}
				});
				$('.mount-option[type="checkbox"]').each(function(i, el) {
					mountConfig[$(el).data('attributeName')] = $(el).prop('checked');
				});

				if (!mountConfig.name) {
					Structr.showAndHideInfoBoxMessage('Must supply name', 'warning', 2000);
				} else if (!mountConfig.mountTarget) {
					Structr.showAndHideInfoBoxMessage('Must supply mount target', 'warning', 2000);
				} else {
					mountConfig.type = 'Folder';
					mountConfig.parentId = currentWorkingDir ? currentWorkingDir.id : null;
					Command.create(mountConfig);

					dialogCancelButton.click();
				}
			});

			dialogBtn.prepend(mountButton);
		});
	}
};
