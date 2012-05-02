/*
 *  Copyright (C) 2012 Axel Morgner
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

var images, files, folders, drop;
var fileList;
var chunkSize = 1024*64;
var sizeLimit = 1024*1024*42;
var win = $(window);

var _Files = {

    icon : 'icon/page_white.png',
    add_file_icon : 'icon/page_white_add.png',
    delete_file_icon : 'icon/page_white_delete.png',
    add_folder_icon : 'icon/folder_add.png',
    folder_icon : 'icon/folder.png',
    delete_folder_icon : 'icon/folder_delete.png',
    download_icon : 'icon/basket_put.png',
	
    init : function() {
    //Structr.classes.push('file');
    //Structr.classes.push('folder');
    //Structr.classes.push('image');
    },
    resize : function() {

        var windowWidth = win.width();
        var windowHeight = win.height();
        var headerOffsetHeight = 82;

        if (folders) {
            folders.css({
                width: Math.max(180, Math.min(windowWidth/3, 360)) + 'px',
                height: windowHeight - headerOffsetHeight + 'px'
            });
        }

        if (files) {
            files.css({
                width: Math.max(180, Math.min(windowWidth/3, 360)) + 'px',
                height: windowHeight - headerOffsetHeight + 'px'
            });
        }

    },

    onload : function() {
        if (debug) console.log('onload');
        if (palette) palette.remove();

        main.append('<table><tr><td id="files"></td><td id="folders"></td><td id="images"></td></tr></table>');
        folders = $('#folders');
        files = $('#files');
        images = $('#images');
        
        _Files.refreshFolders();
        _Files.refreshFiles();
        _Files.refreshImages();

    //	_Files.resize();
    },

    refreshFiles : function() {
        files.empty();
		
        if (window.File && window.FileReader && window.FileList && window.Blob) {

            files.append('<h1>Drop files here</h1>');
            
            drop = $('#files');

            drop.on('dragover', function(event) {
                event.originalEvent.dataTransfer.dropEffect = 'copy';
                return false;
            });
            
            drop.on('drop', function(event) {
                event.stopPropagation()
                event.preventDefault();
                //                console.log(event);
                fileList = event.originalEvent.dataTransfer.files;
                console.log(fileList);
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

                    var errorText = 'The following files are too large (limit ' + sizeLimit/(1024*1024) + ' Mbytes):<br>\n';

                    $(tooLargeFiles).each(function(i, tooLargeFile) {
                        errorText += tooLargeFile.name + ': ' + Math.round(tooLargeFile.size/(1024*1024)) + ' Mbytes<br>\n';
                    });

                    Structr.error(errorText, function() {
                        $.unblockUI({
                            fadeOut: 25
                        });
                        $(filesToUpload).each(function(i, file) {
                            if (debug) console.log(file);
                            if (file) _Files.createFile(file);
                        });
                    })
                } else {
                    $(fileList).each(function(i, file) {
                        if (debug) console.log(file);
                        if (file) _Files.createFile(file);
                    });

                }

                return false;
            });
        }
        Command.list('File');
        _Files.resize();
    },
	
    refreshImages : function() {
        images.empty();
        Command.list('Image');
    },
	
    refreshFolders : function() {
        folders.empty();
        if (Command.list('Folder')) {
            folders.append('<button class="add_folder_icon button"><img title="Add Folder" alt="Add Folder" src="' + _Files.add_folder_icon + '"> Add Folder</button>');
            $('.add_folder_icon', main).on('click', function() {
                var entity = {};
                entity.type = 'Folder';
                Command.create(entity);
            });
        }
    },

    appendFileElement : function(file, folderId, removeExisting, hasChildren, isImage) {

        console.log('Files.appendFileElement', file, folderId, removeExisting, hasChildren, isImage);

        var parentElement, cls;
        if (isImage) {
            parentElement = images;
            cls = 'image';
        } else {
            parentElement = files;
            cls = 'file';
        }

        var icon = _Files.icon; // default
        if (file.contentType && !isImage) {

            if (file.contentType.indexOf('pdf') > -1) {
                icon = 'icon/page_white_acrobat.png';
            } else if (file.contentType.indexOf('text') > -1) {
                icon = 'icon/page_white_text.png';
            } else if (file.contentType.indexOf('xml') > -1) {
                icon = 'icon/page_white_code.png';
            }
        } else {
            icon = viewRootUrl + file.name;
        }
        
        var parent = Structr.findParent(folderId, null, null, parentElement);
        var delIcon, newDelIcon;
        var div = Structr.node(file.id);
        if (removeExisting && div && div.length) {
            parent.append(div.css({
                top: 0,
                left: 0
            }));
            console.log('appended', div, parent);
        } else {
        
            parent.append('<div class="node ' + cls + ' ' + file.id + '_">'
                + '<img class="typeIcon" src="'+ icon + '">'
                + '<b class="name_">' + file.name + '</b> <span class="id">' + file.id + '</span>'
                + '</div>');
            div = Structr.node(file.id, folderId);
        }

        $('.typeIcon', div).on('click', function() {
            window.open(viewRootUrl + file.name, 'Download ' + file.name);
        });
        console.log(folderId, removeExisting);
        
        delIcon = $('.delete_icon', div);

        if (folderId || removeExisting) {
            newDelIcon = '<img title="Remove '+  cls + ' \'' + file.name + '\' from folder ' + folderId + '" alt="Remove '+  cls + ' \'' + file.name + '\' from folder" class="delete_icon button" src="' + _Files.delete_file_icon + '">';
            if (delIcon && delIcon.length) {
                delIcon.replaceWith(newDelIcon);
            } else {
                div.append(newDelIcon);
                delIcon = $('.delete_icon', div);
            }
            $('.delete_icon', div).on('click', function() {
                _Files.removeFileFromFolder(file.id, folderId, isImage);
            });
            disable($('.delete_icon', parent)[0]);
			
        } else {
            newDelIcon = '<img title="Delete ' + file.name + ' \'' + file.name + '\'" alt="Delete ' + file.name + ' \'' + file.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">';
            if (removeExisting && delIcon && delIcon.length) {
                delIcon.replaceWith(newDelIcon);
            } else {
                div.append(newDelIcon);
                delIcon = $('.delete_icon', div);
            } 
            $('.delete_icon', div).on('click', function() {
                _Entities.deleteNode(this, file);
            });
		
        }
        
        div.draggable({
            revert: 'invalid',
            containment: '#main',
            zIndex: 4
        });

        _Entities.appendAccessControlIcon(div, file);
        _Entities.appendEditPropertiesIcon(div, file);
        _Entities.setMouseOver(div);
        
        return div;
    },
	
    appendImageElement : function(file, folderId, removeExisting, hasChildren) {
        console.log('appendImageElement', file, folderId, removeExisting);
        return _Files.appendFileElement(file, folderId, removeExisting, hasChildren, true);
    },
		
    appendFolderElement : function(folder, parentId, removeExisting, hasChildren) {
		
        if (debug) console.log('Folder: ', folder);
        var parent = Structr.findParent(parentId, null, null, folders);
		
        parent.append('<div structr_type="folder" class="node folder ' + folder.id + '_">'
            + '<img class="typeIcon" src="'+ _Files.folder_icon + '">'
            + '<b class="name_">' + folder.name + '</b> <span class="id">' + folder.id + '</span>'
            + '</div>');
        var div = Structr.node(folder.id, parentId);
        div.append('<img title="Delete Content \'' + folder.name + '\'" alt="Delete Content \'' + folder.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function() {
            _Entities.deleteNode(this, folder);
        });

        div.droppable({
            accept: '.file, .image',
            greedy: true,
            hoverClass: 'nodeHover',
            tolerance: 'pointer',
            drop: function(event, ui) {
                var fileId = getId(ui.draggable);
                var folderId = getId($(this));
                var nodeData = {};
                nodeData.id = fileId;
                Command.createAndAdd(folderId, nodeData);
            }
        });

        _Entities.appendAccessControlIcon(div, folder);
        _Entities.appendEditPropertiesIcon(div, folder);
        _Entities.setMouseOver(div);
		
        return div;
    },

    removeFileFromFolder : function(fileId, folderId, isImage) {
        
        var parentElement, cls;
        if (isImage) {
            parentElement = images;
            cls = 'image';
        } else {
            parentElement = files;
            cls = 'file';
        }

        var folder = Structr.node(folderId);
        var file = Structr.node(fileId, folderId);
        _Entities.resetMouseOverState(file);
        parentElement.append(file);
        $('.delete_icon', file).replaceWith('<img title="Delete ' + cls + ' ' + fileId + '" '
            + 'alt="Delete ' + cls + ' ' + fileId + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', file).on('click', function() {
            _Entities.deleteNode(this, Structr.entity(fileId));
        });
        
        file.draggable({
            revert: 'invalid',
            containment: '#main',
            zIndex: 1
        });

        var numberOfFiles = $('.' + cls, folder).size();
        if (debug) console.log(numberOfFiles);
        if (numberOfFiles == 0) {
            enable($('.delete_icon', folder)[0]);
        }

        if (debug) console.log('removeFileFromFolder: fileId=' + fileId + ', folderId=' + folderId);
        Command.removeSourceFromTarget(fileId, folderId);
    },
    
    removeImageFromFolder : function(imageId, folderId) {
        _Files.removeFileFromFolder(imageId, folderId, true);
    },

    createFile : function(fileObj) {
        var entity = {};
        if (debug) console.log(fileObj);
        entity.contentType = fileObj.type;
        entity.name = fileObj.name;
        entity.size = fileObj.size;
        entity.type = isImage(entity.contentType) ? 'Image' : 'File';
        Command.create(entity);
    },

    uploadFile : function(file) {

        if (debug) console.log(fileList);

        $(fileList).each(function(i, fileObj) {

            if (debug) console.log(file);

            if (fileObj.name == file.name) {
     
                if (debug) console.log(fileObj);
                if (debug) console.log('Uploading chunks for file ' + file.id);
                
                var reader = new FileReader();
                reader.readAsBinaryString(fileObj);
                //reader.readAsText(fileObj);

                var chunks = Math.ceil(fileObj.size / chunkSize);
                if (debug) console.log('file size: ' + fileObj.size + ', chunk size: ' + chunkSize + ', chunks: ' + chunks);

                // slicing is still unstable/browser dependent yet, see f.e. http://georgik.sinusgear.com/2011/05/06/html5-api-file-slice-changed/

                //                var blob;
                //                for (var c=0; c<chunks; c++) {
                //
                //                    var start = c*chunkSize;
                //                    var end = (c+1)*chunkSize-1;
                //
                //                    console.log('start: ' + start + ', end: ' + end);
                //
                //                    if (fileObj.webkitSlice) {
                //                        blob = fileObj.webkitSlice(start, end);
                //                    } else if (fileObj.mozSlice) {
                //                        blob = fileObj.mozSlice(start, end);
                //                    }
                //                    setTimeout(function() { reader.readAsText(blob)}, 1000);
                //                }

                reader.onload = function(f) {
                    
                    if (debug) console.log('File was read into memory.');
                    var binaryContent = f.target.result;
                    if (debug) console.log('uploadFile: binaryContent', binaryContent);

                    for (var c=0; c<chunks; c++) {
                        
                        var start = c*chunkSize;
                        var end = (c+1)*chunkSize;
                        
                        var chunk = utf8_to_b64(binaryContent.substring(start,end));
                        // TODO: check if we can send binary data directly

                        Command.chunk(file.id, c, chunkSize, chunk);

                    //                        var obj = {};
                    //                        obj.command = 'CHUNK';
                    //                        obj.id = file.id;
                    //                        var data = {};
                    //                        data.chunkId = c;
                    //                        data.chunkSize = chunkSize;
                    //                        data.chunk = chunk;
                    //                        obj.data = data;
                    //                        //var data = '{ "command" : "CHUNK" , "id" : "' + file.id + '" , "data" : { "chunkId" : ' + c + ' , "chunkSize" : ' + chunkSize + ' , "chunk" : "' + chunk + '" } }';
                    //                        sendObj(obj);

                    }
                    var typeIcon = Structr.node(file.id).find('.typeIcon');
                    var iconSrc = typeIcon.attr('src');
                    if (debug) console.log('Icon src: ', iconSrc);
                    typeIcon.attr('src', iconSrc + '?' + new Date().getTime());

                }
            }

        });

    }

};

$(document).ready(function() {
    Structr.registerModule('files', _Files);
    Structr.classes.push('file');
    Structr.classes.push('folder');
    Structr.classes.push('image');

    win.resize(function() {
        _Files.resize();
    });
});
