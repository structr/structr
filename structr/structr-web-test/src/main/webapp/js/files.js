/*
 *  Copyright (C) 2011 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

var files, drop;

$(document).ready(function() {
    Structr.registerModule('files', Files);
});

var Files = {

    icon : 'icon/folder_page_white.png',
    add_icon : 'icon/page_white_add.png',
    delete_icon : 'icon/page_white_delete.png',
	
    init : function() {
    },

    onload : function() {
        if (debug) console.log('onload');

        if (window.File && window.FileReader && window.FileList && window.Blob) {

            main.append('<div id="filesDropArea">Drop files here to add</div>');
            
            drop = $('#filesDropArea');

            drop.on('dragover', function(event) {
                event.originalEvent.dataTransfer.dropEffect = 'copy';
                return false;
            });
            
            drop.on('drop', function(event) {
                var fileList = event.originalEvent.dataTransfer.files;
                $(fileList).each(function(i, file) {
                    if (debug) console.log(file);
                    Files.uploadFile(file);
                });
                return false;
            });
        }

        main.append('<div id="files"></div>');
        files = $('#files');
        
        Files.refresh();
    },

    refresh : function() {
        files.empty();
        if (Files.show()) {
            drop.append(' or click <button class="add_content_icon button"><img title="Add File" alt="Add File" src="' + Files.add_icon + '"> Add File</button>');
            $('.add_content_icon', main).on('click', function() {
                Files.addFile(this);
            });
        }
    },

    show : function() {
        return Entities.showEntities('File');
    },

    appendFileElement : function(content, parentId) {
        var parent;
        if (debug) console.log(parentId);
        if (parentId) {
            parent = $('.' + parentId + '_');
            if (debug) console.log(parent);
        } else {
            parent = files;
        }
        
        parent.append('<div class="nested top content ' + content.id + '_">'
            + '<img class="typeIcon" src="'+ Files.icon + '">'
            + '<b class="name">' + content.name + '</b> <span class="id">' + content.id + '</span>'
            + '</div>');
        var div = $('.' + content.id + '_', parent);
        div.append('<img title="Delete content \'' + content.name + '\'" alt="Delete content \'' + content.name + '\'" class="delete_icon button" src="' + Files.delete_icon + '">');
        $('.delete_icon', div).on('click', function() {
            Files.deleteContent(this, content);
        });
        //        div.append('<img class="add_icon button" title="Add Element" alt="Add Element" src="icon/add.png">');
        //        $('.add_icon', div).on('click', function() {
        //            Resources.addElement(this, resource);
        //        });
        $('b', div).on('click', function() {
            Entities.showProperties(this, content, 'all', $('.' + content.id + '_', files));
        });
        return div;
    },

    addFile : function(button) {
        return Entities.add(button, 'File');
    },

    deleteFile : function(button, file) {
        if (debug) console.log('delete file ' + file);
        deleteNode(button, file);
    },

    uploadFile : function(file) {

        var reader = new FileReader();
        reader.readAsBinaryString(file);

        var binaryContent;

        reader.onload = function(f) {
            console.log('File was read into memory.');
            binaryContent = f.target.result;
            Entities.create($.parseJSON('{ "type" : "File", "name" : "' + file.name + '", "contentType" : "' + file.type + '", "size" : "' + file.size + '" , "binaryContent" : "' + $.quoteString(binaryContent) + '" }'));
        }

    }

};