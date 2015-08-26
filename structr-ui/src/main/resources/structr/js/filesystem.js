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

var fileTree, folderContents;
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

	},
	resize: function() {

		var windowWidth = win.width();
		var windowHeight = win.height();
		var headerOffsetHeight = 82 + 24;

		if (fileTree) {
			fileTree.css({
                width: '400px',
				height: windowHeight - headerOffsetHeight + 'px'
			});
		}

		if (folderContents) {
			folderContents.css({
                width: windowWidth - 400 - 64 + 'px',
				height: windowHeight - headerOffsetHeight - 24 + 'px'
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
                'data': function (obj, cb) {

                    switch (obj.id) {

                        case '#':
                            var list = [];
                            list.push({id: 'root', text: 'All files', children: true, icon: '/structr/icon/structr_icon_16x16.png', a_attr: { class: 'bold' } } );

                            cb(list);
                            break;

                        case 'root':
                            load('folders/ui?sort=name&parent=()', cb);
                            break;

                        default:
                            load('folders/' + obj.id + '/folders/ui?sort=name', cb);
                            break;
                    }
                }
            }});

        $('#file-tree').on('select_node.jstree', function (evt, data) {

            setWorkingDirectory(data.node.id);
            displayFolderContents(data.node.id);

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
	}
};











function load(url, callback) {

  $.ajax({
    url: '/structr/rest/' + url,
      statusCode: {

        200: function(data) {

          var list = [];

          $.each(data.result, function(i, d) {

            var icon = d.isFolder ? '/structr/icon/folder.png' : '/structr/icon/page_white.png';

            list.push( {

              id: d.id,
                text: d.name,
                children: d.isFolder && d.folders.length > 0,
                icon: icon
            });
          });

          callback(list);
        }
      }
  });
}

function setWorkingDirectory(id) {

  var data = JSON.stringify( { 'workingDirectory': { 'id': id } } );

  $.ajax({

    url: '/structr/rest/me',
      dataType: 'application/json',
      method: 'PUT',
      type: 'PUT',
      data: data
  });
}

function displayFolderContents(id) {

  var content = $('#folder-contents');
  content.empty();
  content.append('<table id="files-table" class="stripe"><thead><tr><th>Name</th><th>Type</th><th>Size</th></tr></thead><tbody id="files-table-body"></tbody></table>');

  var tableBody = $('#files-table-body');
  var url   = '/structr/rest/folders/' + id + '/children/ui?sort=name';

  if (id === 'root') {
    url = '/structr/rest/files/ui?sort=name&hasParent=false';
  }

  $.ajax({
    url: url,
      statusCode: {

        200: function(data) {

          $.each(data.result, function(i, d) {

            if (!d.isFolder) {

              var rowId = 'row' + d.id;
              tableBody.append('<tr id="' + rowId + '"></tr>');
              var row = $('#' + rowId);

              row.append('<td>' + d.name + '</td>');
              row.append('<td>' + d.type + '</td>');
              row.append('<td>' + d.size + '</td>');
            }

          });

          $('#files-table').DataTable();

        }
      }
  });
}





