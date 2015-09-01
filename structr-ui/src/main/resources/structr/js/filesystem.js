/*
 *  Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
			'core': {
				'animation': 0,
				'plugins': ["contextmenu", "dnd", "search", "state", "types", "wholerow"],
				'state': { 'key' : 'structr-ui' },
				'data': function(obj, cb) {

					switch (obj.id) {

//                        case '#':
//                            var list = [];
//                            list.push({id: 'root', text: 'All files', children: true, icon: '/structr/icon/structr_icon_16x16.png', a_attr: {class: 'bold'}});
//
//                            cb(list);
//                            break;

						case '#':
							//load('folders/ui?sort=name&parent=()', cb);
							load(null, cb);
							break;

						default:
							//load('folders/' + obj.id + '/folders/ui?sort=name', cb);
							load(obj.id, cb);
							break;
					}
				}
			}
		});



		$('#file-tree').on('select_node.jstree', function(evt, data) {

			setWorkingDirectory(data.node.id);
			displayFolderContents(data.node.id);

		});

		$('#file-tree').on('loaded.jstree', function(evt, data) {

			loadAndSetWorkingDir();

		});

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
	fulltextSearch: function(searchString) {

		var content = $('#folder-contents');
		content.empty();

		var url = rootUrl + 'files/ui?loose=1';

		searchString.split(' ').forEach(function(str, i) {
			url = url + '&indexedContent=' + str;
		});

		console.log(url);

		displaySearchResultsForURL(url);

	},
	clearSearch: function() {
		$('.search', main).val('');
		var content = $('#folder-contents');
		content.empty();
	}
};

function loadAndSetWorkingDir() {
	$.get(rootUrl + 'me/ui', function(data) {
		var me = data.result;
		//console.log('working dir', me.workingDirectory);
		$('#file-tree').jstree(true).select_node(me.workingDirectory.id);
	});
}

function load(id, callback) {
	
	if (!id) {
		// list: function(type, rootOnly, pageSize, page, sort, order, properties, callback)
		Command.list('Folder', true, 100, 1, 'name', 'desc', null, function(folders) {

			var list = [];
			
			folders.forEach(function(d) {
				var icon = d.isFolder ? '/structr/icon/folder.png' : '/structr/icon/page_white.png';

				list.push({
					id: d.id,
					text: d.name,
					children: d.isFolder && d.folders.length > 0,
					icon: icon
				});
			});

			callback(list);
			
		});
		
	} else {

		Command.children(id, function(folders) {

			var list = [];
			
			folders.forEach(function(d) {
				var icon = d.isFolder ? '/structr/icon/folder.png' : '/structr/icon/page_white.png';

				list.push({
					id: d.id,
					text: d.name,
					children: d.isFolder && d.folders.length > 0,
					icon: icon
				});
			});

			callback(list);

		});
		
	}

//	$.ajax({
//		url: rootUrl + url,
//		statusCode: {
//			200: function(data) {
//
//				var list = [];
//
//				$.each(data.result, function(i, d) {
//
//					var icon = d.isFolder ? '/structr/icon/folder.png' : '/structr/icon/page_white.png';
//
//					list.push({
//						id: d.id,
//						text: d.name,
//						children: d.isFolder && d.folders.length > 0,
//						icon: icon
//					});
//				});
//
//				callback(list);
//			}
//		}
//	});
}

function setWorkingDirectory(id) {

	var data = JSON.stringify({'workingDirectory': {'id': id}});

	$.ajax({
		url: rootUrl + 'me',
		dataType: 'application/json',
		method: 'PUT',
		type: 'PUT',
		data: data
	});
}

function displayFolderContents(id) {
	
//	var url = rootUrl + 'folders/' + id + '/children/ui?sort=name';
//
//	if (id === 'root') {
//		url = rootUrl + 'files/ui?sort=name&hasParent=false';
//	}
//
//	displayFolderContentsForURL(url, id);
//}
//
//function displayFolderContentsForURL(url, id) {

	var content = $('#folder-contents');
	content.empty();
	content.append('<table id="files-table" class="stripe"><thead><tr><th>Type</th><th>Name</th><th>Size</th><th>Content Type</th><th>Owner</th></tr></thead><tbody id="files-table-body"></tbody></table>');

	var tableBody = $('#files-table-body');
	
	// list: function(type, rootOnly, pageSize, page, sort, order, properties, callback) {
	
	Command.children(id, function(children) {
		
		children.forEach(function(d) {
			
			var files = d.files || [];
			var folders = d.folders || [];
			var size = d.isFolder ? folders.length + files.length : d.size;
			
			var rowId = 'row' + d.id;
			tableBody.append('<tr id="' + rowId + '"></tr>');
			var row = $('#' + rowId);
			
			var icon = d.isFolder ? '/structr/icon/folder.png' : '/structr/icon/page_white.png';
			
			row.append('<td class="file-type"><img src="' + icon + '" alt="' + d.type + '"></td>');
			row.append('<td><a href="' + d.path + '">' + d.name + '</a></td>');
			row.append('<td>' + size + '</td>');
			row.append('<td>' + (d.isFile ? d.contentType : '') + '</td>');
			row.append('<td>' + (d.owner ? d.owner.name : '') + '</td>');
		});

		$('#files-table').DataTable({
			fixedHeader: true
		});
	});
	
//	Command.list('File', false, 10, 1, 'name', 'desc', null, function(files) {
//
//		files.forEach(function(d) {
//			if (!d.isFolder) {
//
//				var rowId = 'row' + d.id;
//				tableBody.append('<tr id="' + rowId + '"></tr>');
//				var row = $('#' + rowId);
//
//				row.append('<td><a href="' + d.path + '">' + d.name + '</a></td>');
//				row.append('<td>' + d.type + '</td>');
//				row.append('<td>' + d.size + '</td>');
//			}
//		});
//
//	});

//	$.ajax({
//		url: url,
//		statusCode: {
//			200: function(data) {
//
//				$.each(data.result, function(i, d) {
//
//					if (!d.isFolder) {
//
//						var rowId = 'row' + d.id;
//						tableBody.append('<tr id="' + rowId + '"></tr>');
//						var row = $('#' + rowId);
//
//						row.append('<td><a href="' + d.path + '">' + d.name + '</a></td>');
//						row.append('<td>' + d.type + '</td>');
//						row.append('<td>' + d.size + '</td>');
//					}
//
//				});
//
//				$('#files-table').DataTable({
//					fixedHeader: true
//				});
//
//			}
//		}
//	});
}

function displaySearchResultsForURL(url) {

	var content = $('#folder-contents');
	content.empty();
	content.append('<div id="search-results"></div>');

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

								div.append('<h2>' + d.name + '<img id="preview' + d.id + '" src="/structr/icon/eye.png" style="margin-left: 6px;" title="' + d.extractedContent + '" /></h2>');

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
}
