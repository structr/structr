/*
 *  Copyright (C) 2010-2015 Structr GmbH
 *
 *  This file is part of structr <http://structr.org>.
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

var main, fileTree, folderContents;
var fileList;
var chunkSize = 1024 * 64;
var sizeLimit = 1024 * 1024 * 1024;
var win = $(window);
var selectedElements = [];
var counter = 0;
var currentWorkingDir;

$(document).ready(function() {

	Structr.registerModule('filesystem', _Filesystem);
	_Filesystem.resize();
});

var _Filesystem = {
	init: function() {

		log('_Filesystem.init');

		main = $('#main');

		main.append('<div class="searchBox"><input class="search" name="search" placeholder="Fulltext search"><img class="clearSearchIcon" src="icon/cross_small_grey.png"></div>');

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
				height: windowHeight - headerOffsetHeight - 16 + 'px'
			});
		}
		Structr.resize();

	},
	onload: function() {

		_Filesystem.init();

		$('#main-help a').attr('href', 'http://docs.structr.org/frontend-user-guide#Files');

		main.append('<div id="filesystem-main"><div class="fit-to-height" id="file-tree-container"><div id="file-tree"></div></div><div class="fit-to-height" id="folder-contents-container"><div id="folder-contents"></div></div>');

		fileTree = $('#file-tree');
		folderContents = $('#folder-contents');

		$.jstree.defaults.core.themes.dots = false;

		$('#file-tree').jstree({
			'plugins': ["themes", "dnd", "search", "state", "types", "wholerow"],
			'core': {
				'animation': 0,
				'state': {'key': 'structr-ui'},
				'async': true,
				'data': function(obj, cb) {

					switch (obj.id) {

						case '#':

							Command.list('Folder', true, 100, 1, 'name', 'asc', null, function(folders) {

								var children = [];
								var list = [];

								folders.forEach(function(d) {

									children.push({
										id: d.id,
										text: d.name,
										children: d.isFolder && d.folders.length > 0,
										icon: 'fa fa-folder'
									});
								});

								list.push({id: 'root', text: '/', children: true, icon: '/structr/icon/structr_icon_16x16.png', path: '/', state: {opened: true, selected: true}});
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



		$('#file-tree').on('select_node.jstree', function(evt, data) {

			_Filesystem.setWorkingDirectory(data.node.id);
			_Filesystem.displayFolderContents(data.node.id, data.node.parent, data.node.original.path);

		});

		$('#file-tree').on('loaded.jstree', function(evt, data) {

			_Filesystem.loadAndSetWorkingDir();

		});

		_Filesystem.activateUpload();

		win.off('resize');
		win.resize(function() {
			_Filesystem.resize();
		});

		Structr.unblockMenu(100);

		_Filesystem.resize();

	},
	unload: function() {
		$(main.children('table')).remove();
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
						file.parent = currentWorkingDir;
						console.log('create file', file, currentWorkingDir);
						Command.createFile(file);
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
		content.hide();

		var url = rootUrl + 'files/ui?loose=1';

		searchString.split(' ').forEach(function(str, i) {
			url = url + '&indexedContent=' + str;
		});

		//console.log(url);

		_Filesystem.displaySearchResultsForURL(url);

	},
	clearSearch: function() {
		$('.search', main).val('');
		$('#folder-contents').show();
		$('#search-results').remove();
	},
	loadAndSetWorkingDir: function() {
		$.get(rootUrl + 'me/ui', function(data) {
			var me = data.result;
			//console.log('working dir', me.workingDirectory);
			$('#file-tree').jstree(true).select_node(me.workingDirectory.id);
		});

	},
	load: function(id, callback) {

		if (!id) {

			Command.list('Folder', true, 100, 1, 'name', 'asc', null, function(folders) {

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

			Command.query('Folder', 100, 1, 'name', 'asc', {parent: id}, function(folders) {

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

		var data = JSON.stringify({'workingDirectory': {'id': id}});

		$.ajax({
			url: rootUrl + 'me',
			dataType: 'application/json',
			method: 'PUT',
			type: 'PUT',
			data: data,
			success: function() {
				currentWorkingDir = id;
			}
		});
	},
	displayFolderContents: function(id, parentId, name) {

		var content = $('#folder-contents');
		content.empty();
		content.append(
				'<h1>' + name + '</h1>'
				+ '<table id="files-table" class="stripe"><thead><tr><th class="icon">&nbsp;</th><th>Name</th><th>Size</th><th>Content Type</th><th>Owner</th></tr></thead>'
				+ '<tbody id="files-table-body">'
				+ '<tr id="parent-file-link">'
				+ '<td class="file-type"><i class="fa fa-folder"></i></td>'
				+ '<td><a href="#">..</a></td>'
				+ '<td></td>'
				+ '<td></td>'
				+ '<td></td>'
				+ '</tr></tbody></table>'
				);

		$('#parent-file-link').on('click', function(e) {

			if (parentId !== '#') {
				$('#' + parentId + '_anchor').click();
			}
		});

		if (id === 'root') {

			Command.list('Folder', true, 1000, 1, 'name', 'asc', null, function(children) {

				if (children && children.length) {
					children.forEach(_Filesystem.appendFileOrFolderRow);
				}
			});

			Command.list('FileBase', true, 1000, 1, 'name', 'asc', null, function(children) {

				if (children && children.length) {
					children.forEach(_Filesystem.appendFileOrFolderRow);
				}
			});

		} else {

			Command.query('Folder', 1000, 1, 'name', 'asc', {parentId: id}, function(children) {

				if (children && children.length) {
					children.forEach(_Filesystem.appendFileOrFolderRow);
				}
			});

			Command.query('FileBase', 1000, 1, 'name', 'asc', {parentId: id}, function(children) {

				if (children && children.length) {
					children.forEach(_Filesystem.appendFileOrFolderRow);
				}
			});
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

		row.append('<td class="file-type"><i class="fa ' + icon + '"></i></td>');

		if (d.isFolder) {
			row.append('<td><div id="id_' + d.id + '" data-structr_type="folder" class="node folder"><b title="' + d.name + '" class="name_">' + fitStringToWidth(d.name, 200) + '</b> <span class="id">' + d.id + '</span></div></td>');
		} else {
			row.append('<td><div id="id_' + d.id + '" data-structr_type="file" class="node file"><b title="' + d.name + '" class="name_">' + fitStringToWidth(d.name, 200) + '</b> <span class="id">' + d.id + '</span></div></td>');
		}

		row.append('<td class="readonly">' + size + '</td>');
		row.append('<td>' + (d.isFile && d.contentType ? d.contentType : '') + '</td>');
		row.append('<td>' + (d.owner ? d.owner.name : '') + '</td>');

		$('#id_' + d.id + '.folder').on('click', function(e) {
			
			e.preventDefault();
			e.stopPropagation();

			if (d.parent && d.parent.id) {

				$("#file-tree").jstree("open_node", $('#' + d.parent.id), function() {

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
				_Entities.deleteNode(this, d, true);
			});
		} else {
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
		content.hide();
		content.parent().append('<div id="search-results"></div>');

		var searchString = $('.search', main).val();
		var container = $('#search-results');

		$.ajax({
			url: url,
			statusCode: {
				200: function(data) {

					container.append('<h1>Search results</h1>');

					$.each(data.result, function(i, d) {

						$.ajax({
							url: rootUrl + 'files/' + d.id + '/getSearchContext',
							contentType: 'application/json',
							method: 'POST',
							data: JSON.stringify({searchString: searchString, contextLength: 30}),
							statusCode: {
								200: function(data) {

									container.append('<div style="border: 1px solid #a5a5a5; padding: 12px; margin-bottom: 24px; background-color: #fff; border-radius: 3px;" id="results' + d.id + '"></div>');

									var div = $('#results' + d.id);

									div.append('<h2>' + d.path + '<img id="preview' + d.id + '" src="/structr/icon/eye.png" style="margin-left: 6px;" title="' + d.extractedContent + '" /></h2>');

									$.each(data.result.context, function(i, contextString) {

										searchString.split(' ').forEach(function(str) {
											contextString = contextString.replace(new RegExp('(' + str + ')', 'gi'), "<span style='background-color: #c5ff69;'>$1</span>");
										});

										div.append('<div style="font-size: 9pt; margin-right: 20px; margin-bottom: 20px; width: 300px; float: left;">' + contextString + '</div>');

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
