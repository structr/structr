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
var fileList, timeout;
var chunkSize = 1024 * 64;
var sizeLimit = 1024 * 1024 * 1024;
var win = $(window);
var selectedElements = [];
var counter = 0;

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

						case '#':
							load(null, cb);
							break;

						default:
							load(obj.id, cb);
							break;
					}
				}
			}
		});



		$('#file-tree').on('select_node.jstree', function(evt, data) {

			setWorkingDirectory(data.node.id);
            displayFolderContents(data.node.id, data.node.parent);

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

		Command.query('Folder', 100, 1, 'name', 'asc', { parent: id }, function(folders) {

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

function displayFolderContents(id, parentId) {

	var content = $('#folder-contents');
	content.empty();
	content.append(
        '<table id="files-table" class="stripe"><thead><tr><th>Type</th><th>Name</th><th>Size</th><th>Content Type</th><th>Owner</th></tr></thead>'
      + '<tbody id="files-table-body"><tr>'
      + '<td class="file-type"><img src="/structr/icon/folder.png" /></td>'
      + '<td><a href="#" id="parent_file_link">..</a></td>'
      + '<td></td>'
      + '<td></td>'
      + '<td></td>'
      + '</tr></tbody></table>'
    );

    $('#parent_file_link').on('click', function(e) {

        if (!timeout && parentId !== '#') {
            timeout = window.setTimeout(function() {
                $('#' + parentId + '_anchor').click();
                timeout = null;
            }, 250);
        }
    });



	Command.query('Folder', 100, 1, 'name', 'asc', { parentId: id }, function(children) {

        if (children && children.length) {
            children.forEach(appendFileOrFolderRow);
        }
	});

	Command.query('FileBase', 100, 1, 'name', 'asc', { parentId: id }, function(children) {

        if (children && children.length) {
    		children.forEach(appendFileOrFolderRow);
        }

        $('#files-table').DataTable({
            fixedHeader: true,
            deferRender: true,
            bProcessing: true,
            bDeferRender: true
        });
	});
}

function appendFileOrFolderRow(d) {

	var tableBody = $('#files-table-body');
    var files = d.files || [];
    var folders = d.folders || [];
    var size = d.isFolder ? folders.length + files.length : d.size;

    var rowId = 'row' + d.id;
    tableBody.append('<tr id="' + rowId + '"></tr>');
    var row = $('#' + rowId);

    var icon = d.isFolder ? '/structr/icon/folder.png' : '/structr/icon/page_white.png';

    row.append('<td class="file-type"><img src="' + icon + '" alt="' + d.type + '"></td>');

    if (d.isFolder) {
        row.append('<td><a href="#" id="' + d.id + '_file_link">' + d.name + '</a></td>');
    } else {
        row.append('<td><a href="' + d.path + '">' + d.name + '</a></td>');
    }

    row.append('<td>' + size + '</td>');
    row.append('<td>' + (d.isFile ? d.contentType : '') + '</td>');
    row.append('<td>' + (d.owner ? d.owner.name : '') + '</td>');

    $('#' + d.id + '_file_link').on('click', function(e) {

        if (d.parent && d.parent.id) {
            $("#file-tree").jstree("open_node", $('#' + d.parent.id));
        }

        if (!timeout) {

            timeout = window.setTimeout(function() {

                if (d.name === '..') {
                    $('#' + d.parent.id + '_anchor').click();
                } else {
                    $('#' + d.id + '_anchor').click();
                }
                timeout = null;
            }, 100);
        }
    });
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
