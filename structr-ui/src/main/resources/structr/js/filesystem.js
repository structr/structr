/*
 *  Copyright (C) 2010-2015 Structr GmbH
 *
 *  This file is part of Structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

var main, filesystemMain, fileTree, folderContents;
var fileList;
var chunkSize = 1024 * 64;
var sizeLimit = 1024 * 1024 * 1024;
var win = $(window);
var selectedElements = [];
var counter = 0;
var currentWorkingDir;
var folderPageSize = 10000, folderPage = 1;

$(document).ready(function() {

	Structr.registerModule('filesystem', _Filesystem);
	_Filesystem.resize();
	
});

var _Filesystem = {
	init: function() {

		log('_Filesystem.init');

		main = $('#main');
		
		main.append('<div class="searchBox"><input class="search" name="search" placeholder="Search..."><img class="clearSearchIcon" src="icon/cross_small_grey.png"></div>');

		searchField = $('.search', main);
		searchField.focus();

		searchField.keyup(function(e) {

			var searchString = $(this).val();
			if (searchString && searchString.length && e.keyCode === 13) {

				$('.clearSearchIcon').show().on('click', function() {
					_Filesystem.clearSearch();
				});

				_Filesystem.fulltextSearch(searchString);

			} else if (e.keyCode === 27 || searchString === '') {
				_Filesystem.clearSearch();
			}

		});

		Structr.makePagesMenuDroppable();

	},
	resize: function() {

		var windowWidth = win.width();
		var windowHeight = win.height();
		var headerOffsetHeight = 82 + 48;

		if (fileTree) {
			fileTree.css({
				width: Math.max(180, Math.min(windowWidth / 3, 360)) + 'px',
				height: windowHeight - headerOffsetHeight + 'px'
			});
		}

		if (folderContents) {
			folderContents.css({
				width: windowWidth - 400 - 64 + 'px',
				height: windowHeight - headerOffsetHeight - 43 + 'px'
			});
		}
		Structr.resize();

	},
	onload: function() {

		_Filesystem.init();

		$('#main-help a').attr('href', 'http://docs.structr.org/frontend-user-guide#Filesystem');

		main.append('<div id="filesystem-main"><div class="fit-to-height" id="file-tree-container"><div id="file-tree"></div></div><div class="fit-to-height" id="folder-contents-container"><div id="folder-contents"></div></div>');
		filesystemMain = $('#filesystem-main');

		fileTree = $('#file-tree');
		folderContents = $('#folder-contents');

		$('#folder-contents-container').prepend(
				'<button class="add_file_icon button"><img title="Add File" alt="Add File" src="' + _Files.add_file_icon + '"> Add File</button>'
				+ '<button class="pull_file_icon button"><img title="Sync Files" alt="Sync Files" src="' + _Files.pull_file_icon + '"> Sync Files</button>'
				);
		
		$('.add_file_icon', main).on('click', function(e) {
			e.stopPropagation();
			Command.create({ type: 'File', size: 0, parentId: currentWorkingDir ? currentWorkingDir.id : null }, function(f) {
				_Filesystem.appendFileOrFolderRow(f);
			});
		});

		$('#folder-contents-container').prepend('<button class="add_folder_icon button"><img title="Add Folder" alt="Add Folder" src="' + _Files.add_folder_icon + '"> Add Folder</button>');
		$('.add_folder_icon', main).on('click', function(e) {
			e.stopPropagation();
			Command.create({ type: 'Folder', parentId: currentWorkingDir ? currentWorkingDir.id : null }, function(f) {
				_Filesystem.appendFileOrFolderRow(f);
				_Filesystem.refreshTree();
			});
		});

		$.jstree.defaults.core.themes.dots = false;
		
		_Filesystem.loadAndSetWorkingDir(function() {
			_Filesystem.initTree();
			if (!currentWorkingDir) {
				_Filesystem.displayFolderContents('root', null, '/');
			}
		
		});

		fileTree.on('select_node.jstree', function(evt, data) {

			if (data.node.id === 'root') {
				fileTree.jstree('deselect_node', 'root');
				_Filesystem.deepOpen(currentWorkingDir, []);
				//return;
			}

			_Filesystem.setWorkingDirectory(data.node.id);
			_Filesystem.displayFolderContents(data.node.id, data.node.parent, data.node.original.path, data.node.parents);

		});
		
		_Filesystem.activateUpload();

		win.off('resize');
		win.resize(function() {
			_Filesystem.resize();
		});

		Structr.unblockMenu(100);

		_Filesystem.resize();

	},
	deepOpen: function(d, dirs) {
		if (d && d.id) {
			dirs.unshift(d);
			Command.get(d.id, function(dir) {
				if (dir.parent) {
					_Filesystem.deepOpen(dir.parent, dirs);
				} else {
					_Filesystem.open(dirs);
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
			_Filesystem.open(dirs);
		});
	},
	refreshTree: function() {
		fileTree.jstree('refresh');
	},
	initTree: function() {
		fileTree.jstree({
			'plugins': ["themes", "dnd", "search", "state", "types", "wholerow"],
			'core': {
				'animation': 0,
				'state': {'key': 'structr-ui'},
				'async': true,
				'data': function(obj, cb) {

					switch (obj.id) {

						case '#':

							Command.list('Folder', true, folderPageSize, folderPage, 'name', 'asc', null, function(folders) {

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
										text: d.name,
										children: d.isFolder && d.folders.length > 0,
										icon: 'fa fa-folder'
									});
								});

								cb(list);
								
							});
							break;

						case 'root':
							_Filesystem.load(null, cb);
							break;

						default:
							_Filesystem.load(obj.id, cb);
							break;
					}
				}
			}
		});
		
	},
	unload: function() {
		fastRemoveAllChildren(main[0]);
	},
	activateUpload: function() {
		if (window.File && window.FileReader && window.FileList && window.Blob) {

			drop = $('#folder-contents');

			drop.on('dragover', function(event) {
				log('dragging over #files area');
				event.originalEvent.dataTransfer.dropEffect = 'copy';
				return false;
			});

			drop.on('drop', function(event) {

				if (!event.originalEvent.dataTransfer) {
					return;
				}

				log('dropped something in the #files area');

				event.stopPropagation();
				event.preventDefault();

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
						errorText += tooLargeFile.name + ': ' + Math.round(tooLargeFile.size / (1024 * 1024)) + ' Mbytes<br>\n';
					});

					Structr.error(errorText, function() {
						$.unblockUI({
							fadeOut: 25
						});
						$(filesToUpload).each(function(i, file) {
							log(file);
							if (file) {
								Command.createFile(file);
							}
						});
					});

				} else {
					$(filesToUpload).each(function(i, file) {
						//file.parent = { id: currentWorkingDir };
						file.parentId = currentWorkingDir ? currentWorkingDir.id : null;;
						file.hasParent = true;
						Command.createFile(file, function(f) {
							_Filesystem.appendFileOrFolderRow(f);
						});
					});
				}

				return false;
			});
		}		
	},
	uploadFile: function(file) {
		var worker = new Worker('js/upload-worker.js');
		worker.onmessage = function(e) {

			var binaryContent = e.data;
			var chunks = Math.ceil(file.size / chunkSize);

			for (var c = 0; c < chunks; c++) {
				var start = c * chunkSize;
				var end = (c + 1) * chunkSize;
				var chunk = window.btoa(String.fromCharCode.apply(null, new Uint8Array(binaryContent.slice(start, end))));
				Command.chunk(file.id, c, chunkSize, chunk, chunks);
			}
			var typeIcon = Structr.node(file.id).parent().find('.typeIcon');
			var iconSrc = typeIcon.prop('src');
			log('Icon src: ', iconSrc);
			typeIcon.prop('src', iconSrc + '?' + new Date().getTime());
		};

		$(fileList).each(function(i, fileObj) {
			if (fileObj.name === file.name) {
				log('Uploading chunks for file ' + file.id);
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
			url = rootUrl + 'files/ui?indexedWords=' + searchString;
		}

		_Filesystem.displaySearchResultsForURL(url);
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

			Command.list('Folder', true, folderPageSize, folderPage, 'name', 'asc', null, function(folders) {

				var list = [];

				folders.forEach(function(d) {
					list.push({
						id: d.id,
						text: d.name,
						children: d.isFolder && d.folders.length > 0,
						icon: 'fa fa-folder',
						path: d.path
					});
				});

				callback(list);
				
			});

		} else {

			Command.query('Folder', folderPageSize, folderPage, 'name', 'asc', {parent: id}, function(folders) {

				var list = [];

				folders.forEach(function(d) {
					list.push({
						id: d.id,
						text: d.name,
						children: d.isFolder && d.folders.length > 0,
						icon: 'fa fa-folder',
						path: d.path
					});
				});

				callback(list);

			});

		}
		
		
	},
	setWorkingDirectory: function(id) {
		
		if (id === 'root') {
			currentWorkingDir = null;
			return;
		}
		
		currentWorkingDir = { 'id': id };
		var data = JSON.stringify({'workingDirectory': currentWorkingDir});
		
		$.ajax({
			url: rootUrl + 'me',
			dataType: 'application/json',
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
				return '<a class="breadcrumb-entry" data-folder-id="' + parent + '"><i class="fa fa-caret-right"></i> ' + pathNames[idx] + '</span>';
			}).join(' ');
		}
		content.append(
				'<h2>' + path + '</h2>'
				+ '<table id="files-table" class="stripe"><thead><tr><th class="icon">&nbsp;</th><th>Name</th><th>Size</th><th>Type</th><th>Owner</th></tr></thead>'
				+ '<tbody id="files-table-body">'
				+ ((id != 'root') ? '<tr id="parent-file-link"><td class="file-type"><i class="fa fa-folder"></i></td><td><a href="#">..</a></td><td></td><td></td><td></td></tr>' : '')
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

		var handleChildren = function(children) {
			if (children && children.length) {
				children.forEach(_Filesystem.appendFileOrFolderRow);
			}
		};

		if (id === 'root') {

			Command.list('Folder', true, 1000, 1, 'name', 'asc', null, handleChildren);
			Command.list('FileBase', true, 1000, 1, 'name', 'asc', null, handleChildren);

		} else {

			Command.query('Folder', 1000, 1, 'name', 'asc', {parentId: id}, handleChildren);
			Command.query('FileBase', 1000, 1, 'name', 'asc', {parentId: id}, handleChildren);

		}
	},
	appendFileOrFolderRow: function(d) {

		// add folder/file to global model
		StructrModel.createFromData(d, null, false);

		var tableBody = $('#files-table-body');
		var files = d.files || [];
		var folders = d.folders || [];
		var size = d.isFolder ? folders.length + files.length : d.size;

		var rowId = 'row' + d.id;
		tableBody.append('<tr id="' + rowId + '"></tr>');
		var row = $('#' + rowId);
		var icon = d.isFolder ? 'fa-folder' : _Filesystem.getIcon(d.name, d.contentType);

		if (d.isFolder) {
			row.append('<td class="file-type"><i class="fa ' + icon + '"></i></td>');
			row.append('<td><div id="id_' + d.id + '" data-structr_type="folder" class="node folder"><b title="' + d.name + '" class="name_">' + fitStringToWidth(d.name, 200) + '</b> <span class="id">' + d.id + '</span></div></td>');
		} else {
			row.append('<td class="file-type"><a href="' + d.path + '" target="_blank"><i class="fa ' + icon + '"></i></a></td>');
			row.append('<td><div id="id_' + d.id + '" data-structr_type="file" class="node file">'
			+ '<b title="' + d.name + '" class="name_">' + fitStringToWidth(d.name, 200) + '</b>'
			+ '<div class="progress"><div class="bar"><div class="indicator"><span class="part"></span>/<span class="size">' + d.size + '</span></div></div></div><span class="id">' + d.id + '</span></div></td>');
		}

		row.append('<td>' + size + '</td>');
		row.append('<td>' + d.type + (d.isFile && d.contentType ? ' (' + d.contentType + ')' : '') + '</td>');
		row.append('<td>' + (d.owner ? d.owner.name : '') + '</td>');

		// Change working dir by click on folder icon
		$('#id_' + d.id + '.folder').parent().prev().on('click', function(e) {
			
			e.preventDefault();
			e.stopPropagation();

			if (d.parent && d.parent.id) {

				fileTree.jstree('open_node', $('#' + d.parent.id), function() {

					if (d.name === '..') {
						$('#' + d.parent.id + '_anchor').click();
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
			
			// ********** Folders **********

			div.append('<img title="Sync folder \'' + d.name + '\' to remote instance" alt="Sync folder \'' + d.name + '\' to remote instance" class="push_icon button" src="icon/page_white_get.png">');
			div.children('.push_icon').on('click', function() {
				Structr.pushDialog(d.id, true);
				return false;
			});
			
			var newDelIcon = '<img title="Delete folder \'' + d.name + '\'" alt="Delete folder \'' + d.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">';
			if (delIcon && delIcon.length) {
				delIcon.replaceWith(newDelIcon);
			} else {
				div.append(newDelIcon);
			}
			div.children('.delete_icon').on('click', function(e) {
				e.stopPropagation();
				_Entities.deleteNode(this, d, true, function() {
					_Filesystem.refreshTree();
				});
			});
			
			div.draggable({
				revert: 'invalid',
				stack: '.node'
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
					log('fileId, folderId', fileId, folderId);
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

						_Filesystem.refreshTree();
					}
					
					return false;
				}
			});
			

		} else {
			
			// ********** Files **********
			
			if (_Files.isArchive(d)) {
				div.append('<img class="unarchive_icon button" src="icon/compress.png">');
				div.children('.unarchive_icon').on('click', function() {
					log('unarchive', d.id);
					Command.unarchive(d.id);
				});
			}

			div.append('<img title="Sync file \'' + d.name + '\' to remote instance" alt="Sync file \'' + d.name + '\' to remote instance" class="push_icon button" src="icon/page_white_get.png">');
			div.children('.push_icon').on('click', function() {
				Structr.pushDialog(d.id, false);
				return false;
			});

			div.children('.typeIcon').on('click', function(e) {
				e.stopPropagation();
				window.open(file.path, 'Download ' + file.name);
			});
			var newDelIcon = '<img title="Delete file ' + d.name + '\'" alt="Delete file \'' + d.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">';
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
			
			_Filesystem.appendEditFileIcon(div, d);

			div.draggable({
				revert: 'invalid',
				//helper: 'clone',
				containment: 'document',
				//stack: '.node',
				appendTo: '#main',
				//zIndex: 2,
//				start: function(e, ui) {
//					$(this).hide();
//					ui.helper.css({
//						width: files.width() + 'px'
//					});
//				},
				stop: function(e, ui) {
					$(this).show();
					//$('#pages_').droppable('enable').removeClass('nodeHover');
					$(e.toElement).one('click', function(e) {
						e.stopImmediatePropagation();
					});
				},
				helper: function(event) {
					selectedElements = $('.node.selected');
					if (selectedElements.length > 1) {
						selectedElements.removeClass('selected');
						return $('<img class="node-helper" src="icon/page_white_stack.png">')
								.css("margin-left", event.clientX - $(event.target).offset().left);
					}
					return $(this).clone();
				}
			});
			
		}
		_Entities.appendEditPropertiesIcon(div, d);
		_Entities.setMouseOver(div);
		
		
	},
	appendEditFileIcon: function(parent, file) {

		var editIcon = $('.edit_file_icon', parent);

		if (!(editIcon && editIcon.length)) {
			parent.append('<img title="Edit ' + file.name + ' [' + file.id + ']" alt="Edit ' + file.name + ' [' + file.id + ']" class="edit_file_icon button" src="icon/pencil.png">');
		}

		$(parent.children('.edit_file_icon')).on('click', function(e) {
			e.stopPropagation();

			fileContents = {};
			editor = undefined;

			selectedElements = $('.node.selected');
			if (selectedElements.length > 1) {
				selectedElements.removeClass('selected');
			} else {
				selectedElements = parent;
			}

			Structr.dialog('Edit files', function() {
				log('content saved');
			}, function() {
				log('cancelled');
			});

			dialogText.append('<div id="files-tabs" class="files-tabs"><ul></ul></div>');
			$.each(selectedElements, function(i, el) {
				Command.get(Structr.getId(el), function(entity) {

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

					if (entity.id === file.id) {
						_Entities.activateTabs(file.id, '#files-tabs', '#content-tab-' + file.id);
					}
				});

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
							_Filesystem.clearSearch();
						});
						return;
					} else {
						container.append('<h1>' + data.result.length + ' search results:</h1><table class="props"><thead><th class="_type">Type</th><th>Name</th><th>Size</th></thead><tbody></tbody></table>');
						data.result.forEach(function(d) {
							var icon = _Filesystem.getIcon(d.name, d.contentType);
							$('tbody', container).append('<tr><td><i class="fa ' + icon + '"></i> ' + d.type + ' (' + d.contentType + ')</td><td><a href="#results' + d.id + '">' + d.name + '</a></td><td>' + d.size + '</td></tr>');
						
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
									var icon = _Filesystem.getIcon(d.name, d.contentType);
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
	getIcon: function(fileName, contentType) {

		var result = 'fa-file-o';

		if (contentType) {

			switch (contentType) {

				case 'text/plain':
					result = 'fa-file-text-o';
					break;

				case 'application/pdf':
				case 'application/postscript':
					result = 'fa-file-pdf-o';
					break;

				case 'application/x-pem-key':
				case 'application/pkix-cert+pem':
				case 'application/x-iwork-keynote-sffkey':
					result = 'fa-key';
					break;

				case 'application/x-trash':
					result = 'fa-trash-o';
					break;

				case 'application/octet-stream':
					result = 'fa-terminal';
					break;

				case 'application/x-shellscript':
				case 'application/javascript':
				case 'application/xml':
				case 'text/html':
				case 'text/xml':
					result = 'fa-file-code-o';
					break;

				case 'application/java-archive':
				case 'application/zip':
				case 'application/rar':
				case 'application/x-bzip':
					result = 'fa-file-archive-o';
					break;

				case 'application/vnd.openxmlformats-officedocument.wordprocessingml.document':
				case 'application/vnd.oasis.opendocument.text':
				case 'application/msword':
					result = 'fa-file-word-o';
					break;

				case 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet':
				case 'application/vnd.oasis.opendocument.spreadsheet':
				case 'application/vnd.ms-excel':
					result = 'fa-file-excel-o';
					break;

				case 'application/vnd.openxmlformats-officedocument.presentationml.presentation':
					result = 'fa-file-powerpoint-o';
					break;

				case 'image/jpeg':
					result = 'fa-picture-o';
					break;

				case 'application/vnd.oasis.opendocument.chart':
					result = 'fa-line-chart';
					break;

				default:
					if (contentType.startsWith('image/')) {
						result = 'fa-file-image-o';
					} else if (contentType.startsWith('text/')) {
						result = 'fa-file-text-o';
					}
			}

			if (fileName && fileName.contains('.')) {

				var fileExtensionPosition = fileName.lastIndexOf('.') + 1;
				var fileExtension = fileName.substring(fileExtensionPosition);

				// add file extension css class to control colors
				result = fileExtension + ' ' + result;
			}
		}

		return result;
	}

};
