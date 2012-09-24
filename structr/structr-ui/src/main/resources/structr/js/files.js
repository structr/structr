/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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

        main.append('<table id="dropArea"><tr><td id="folders"></td><td id="files"></td><td id="images"></td></tr></table>');
        folders = $('#folders');
        files = $('#files');
        images = $('#images');
        
        _Files.refreshFolders();
        _Files.refreshFiles();
        _Files.refreshImages();

    //	_Files.resize();
    },

    unload : function() {
        $(main.children('table')).remove();
    },

    refreshFiles : function() {
        files.empty();
        
        if (window.File && window.FileReader && window.FileList && window.Blob) {

            files.append('<h1>Files</h1>');
            
            drop = $('#dropArea');

            drop.on('dragover', function(event) {
                if (debug) console.log('dragging over #files area');
                event.originalEvent.dataTransfer.dropEffect = 'copy';
                return false;
            });
            
            drop.on('drop', function(event) {
                
                if (!event.originalEvent.dataTransfer) {
                    console.log(event);
                    return;
                }
                
                if (debug) console.log('dropped something in the #files area')
                
                event.stopPropagation();
                event.preventDefault();
                
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
                    });
                    
                } else {
                    
                    var dialogMsg = $('#dialogMsg');

                    dialog.empty();
                    dialogMsg.empty();

                    dialog.append('<table class="props"></table>');
                    
                    $(filesToUpload).each(function(i, fileToUpload) {
                        $('.props', dialog).append('<tr><td>' + fileToUpload.name + '</td><td>' + fileToUpload.size + ' bytes</td></tr>');
                    });

                    Structr.dialog('Uploading Files', function() {
                        return true;
                    }, function() {
                        return true;
                    });
                    
                    $(filesToUpload).each(function(i, file) {
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
        images.append('<h1>Images</h1>');
        Command.list('Image');
    },
	
    refreshFolders : function() {
        folders.empty();
        if (Command.list('Folder')) {
            folders.append('<button class="add_folder_icon button"><img title="Add Folder" alt="Add Folder" src="' + _Files.add_folder_icon + '"> Add Folder</button>');
            $('.add_folder_icon', main).on('click', function(e) {
                e.stopPropagation();
                var entity = {};
                entity.type = 'Folder';
                Command.create(entity);
            });
        }
    },

    getIcon : function(file, isImage) {
        var icon = _Files.icon; // default
        if (file && file.contentType && !isImage) {

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
        
        return icon;
    },

    appendFileElement : function(file, folderId, add, hasChildren, isImage) {

        if (debug) console.log('Files.appendFileElement', file, folderId, add, hasChildren, isImage);
        
        if (!folderId && file.parentFolder) return false;
        

        var div;
        var parentElement, cls;
        
        if (isImage) {
            parentElement = images;
            cls = 'image';
        } else {
            parentElement = files;
            cls = 'file';
        }

        var icon = _Files.getIcon(file, isImage);
        
        var parent = Structr.findParent(folderId, null, null, parentElement);
        
        if (parent == files && file.parentFolder) {
            return;
        }
        
        if (add) _Entities.ensureExpanded(parent);
        
        var delIcon, newDelIcon;
        div = Structr.node(file.id);
        if (add && div && div.length) {
            
            var formerParent = div.parent();
            parent.append(div.css({
                top: 0,
                left: 0
            }));
            
            if (!Structr.containsNodes(formerParent)) {
                _Entities.removeExpandIcon(formerParent);
                enable($('.delete_icon', formerParent)[0]);
            }            
            
            if (debug) console.log('appended existing div to parent', div, parent);
            
        } else {
        
            parent.append('<div class="node ' + cls + ' ' + file.id + '_">'
                + '<img class="typeIcon" src="'+ icon + '">'
                + '<b class="name_">' + file.name + '</b> <span class="id">' + file.id + '</span>'
                + '</div>');
            div = Structr.node(file.id, folderId);
            
            if (debug) console.log('appended new div to parent', div, parent);
        }

        $('.typeIcon', div).on('click', function(e) {
            e.stopPropagation();
            window.open(viewRootUrl + file.name, 'Download ' + file.name);
        });
        if (debug) console.log(folderId, add);
        
        delIcon = $('.delete_icon', div);

        if (folderId) {
            newDelIcon = '<img title="Remove '+  cls + ' \'' + file.name + '\' from folder ' + folderId + '" alt="Remove '+  cls + ' \'' + file.name + '\' from folder" class="delete_icon button" src="' + _Files.delete_file_icon + '">';
            if (delIcon && delIcon.length) {
                delIcon.replaceWith(newDelIcon);
            } else {
                div.append(newDelIcon);
                delIcon = $('.delete_icon', div);
            }
            $('.delete_icon', div).on('click', function(e) {
                e.stopPropagation();
                //_Files.removeFileFromFolder(file.id, folderId, isImage);
                Command.removeSourceFromTarget(file.id, folderId);
            });
            disable($('.delete_icon', parent)[0]);
			
        } else {
            newDelIcon = '<img title="Delete ' + file.name + ' \'' + file.name + '\'" alt="Delete ' + file.name + ' \'' + file.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">';
            if (add && delIcon && delIcon.length) {
                delIcon.replaceWith(newDelIcon);
            } else {
                div.append(newDelIcon);
                delIcon = $('.delete_icon', div);
            } 
            $('.delete_icon', div).on('click', function(e) {
                e.stopPropagation();
                _Entities.deleteNode(this, file);
            });
		
        }
        
        div.draggable({
            revert: 'invalid',
            helper: 'clone',
            //containment: '#main',
            zIndex: 4,
            stop : function(e,ui) {
                $('#pages_').removeClass('nodeHover').droppable('enable');
            }
        });

        _Entities.appendAccessControlIcon(div, file);
        _Entities.appendEditPropertiesIcon(div, file);
        _Files.appendEditFileIcon(div, file);      

        _Entities.setMouseOver(div);
        
        return div;
    },
	
    appendImageElement : function(file, folderId, removeExisting, hasChildren) {
        if (debug) console.log('appendImageElement', file, folderId, removeExisting);
        return _Files.appendFileElement(file, folderId, removeExisting, hasChildren, true);
    },
		
    appendFolderElement : function(folder, folderId, hasChildren) {
		
        if (debug) console.log('appendFolderElement', folder, folderId, hasChildren);
        
        var parent = Structr.findParent(folderId, null, null, folders);
        
        if (debug) console.log('appendFolderElement parent', parent);
        if (!parent) return false;
        
        //parent.append('<div id="_' + folderId + '" class="node element ' + folderId + '_"></div>');
        
        //var div = Structr.node(entity.id, parentId, componentId, pageId, pos);
        //var div = $('#_' + id);
        
        var delIcon, newDelIcon;    
        var div;
        
        var removeExisting = true;
        
        //div = Structr.node(folder.id);
        div = $('#_' + folder.id);
        
        if (debug) console.log('appendFolderElement: parent, div', parent, div);
        
        if (div && div.length) {
            
            var formerParent = div.parent();
            
            parent.append(div.css({
                top: 0,
                left: 0
            }));
            
            if (!Structr.containsNodes(formerParent)) {
                _Entities.removeExpandIcon(formerParent);
                enable($('.delete_icon', formerParent)[0]);
            }
            
        } else {
            
            parent.append('<div id="_' + folder.id + '" structr_type="folder" class="node folder ' + folder.id + '_">'
                + '<img class="typeIcon" src="'+ _Files.folder_icon + '">'
                + '<b class="name_">' + folder.name + '</b> <span class="id">' + folder.id + '</span>'
                + '</div>');
        
            //div = Structr.node(folder.id, parent.id);
            div = $('#_' + folder.id);
        }
        
        delIcon = $('.delete_icon', div);
        
        if (folderId) {
            newDelIcon = '<img title="Remove folder ' + folder.name + '\' from folder ' + folderId + '" alt="Remove folder ' + folder.name + '\' from folder" class="delete_icon button" src="' + _Files.delete_folder_icon + '">';
            if (delIcon && delIcon.length) {
                delIcon.replaceWith(newDelIcon);
            } else {
                div.append(newDelIcon);
                delIcon = $('.delete_icon', div);
            }
            $('.delete_icon', div).on('click', function(e) {
                e.stopPropagation();
                Command.removeSourceFromTarget(folder.id, folderId);
            });
            disable($('.delete_icon', parent)[0]);
			
        } else {
            newDelIcon = '<img title="Delete ' + folder.name + ' \'' + folder.name + '\'" alt="Delete ' + folder.name + ' \'' + folder.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">';
            if (removeExisting && delIcon && delIcon.length) {
                delIcon.replaceWith(newDelIcon);
            } else {
                div.append(newDelIcon);
                delIcon = $('.delete_icon', div);
            } 
            $('.delete_icon', div).on('click', function(e) {
                e.stopPropagation();
                _Entities.deleteNode(this, folder);
            });
		
        }
        
        _Entities.appendExpandIcon(div, folder, hasChildren);
        
        div.draggable({
            revert: 'invalid',
            helper: 'clone',
            //containment: '#main',
            zIndex: 4
        });
        
        div.droppable({
            accept: '.folder, .file, .image',
            greedy: true,
            hoverClass: 'nodeHover',
            tolerance: 'pointer',
            drop: function(event, ui) {
                var self = $(this);
                var fileId = getId(ui.draggable);
                var folderId = getId(self);
                if (debug) console.log('fileId, folderId', fileId, folderId);
                if (!(fileId == folderId)) {
                    var nodeData = {};
                    nodeData.id = fileId;
                    addExpandedNode(folderId);
                    if (debug) console.log('addExpandedNode(folderId)', addExpandedNode(folderId));
                    Command.createAndAdd(folderId, nodeData);
                }
            }
        });

        _Entities.appendAccessControlIcon(div, folder);
        _Entities.appendEditPropertiesIcon(div, folder);
        _Entities.setMouseOver(div);
		
        return div;
    },
    
    removeFolderFromFolder : function(folderToRemoveId, folderId) {
        
        var folder = Structr.node(folderId);
        var folderToRemove = Structr.node(folderToRemoveId, folderId);
        _Entities.resetMouseOverState(folderToRemove);
        
        folders.append(folderToRemove);
        
        $('.delete_icon', folderToRemove).replaceWith('<img title="Delete folder ' + folderToRemoveId + '" '
            + 'alt="Delete folder ' + folderToRemoveId + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', folderToRemove).on('click', function(e) {
            e.stopPropagation();
            _Entities.deleteNode(this, Structr.entity(folderToRemoveId));
        });
        
        folderToRemove.draggable({
            revert: 'invalid',
            containment: '#main',
            zIndex: 1
        });

        if (!Structr.containsNodes(folder)) {
            _Entities.removeExpandIcon(folder);
            enable($('.delete_icon', folder)[0]);
        }

        if (debug) console.log('removeFolderFromFolder: fileId=' + folderToRemoveId + ', folderId=' + folderId);
    },
    
    removeFileFromFolder : function(fileId, folderId, isImage) {
        if (debug) console.log('removeFileFromFolder', fileId, folderId, isImage);
        
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
        
        if (debug) console.log(file, folder);
        
        _Entities.resetMouseOverState(file);
        
        parentElement.append(file);
        
        $('.delete_icon', file).replaceWith('<img title="Delete ' + cls + ' ' + fileId + '" '
            + 'alt="Delete ' + cls + ' ' + fileId + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', file).on('click', function(e) {
            e.stopPropagation();
            _Entities.deleteNode(this, Structr.entity(fileId));
        });
        
        file.draggable({
            revert: 'invalid',
            containment: '#main',
            zIndex: 1
        });

        if (!Structr.containsNodes(folder)) {
            _Entities.removeExpandIcon(folder);
            enable($('.delete_icon', folder)[0]);
        }
        
    },
    
    removeImageFromFolder : function(imageId, folderId) {
        if (debug) console.log('removeImageFromFolder', imageId, folderId);
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

                    }
                    var typeIcon = Structr.node(file.id).find('.typeIcon');
                    var iconSrc = typeIcon.prop('src');
                    if (debug) console.log('Icon src: ', iconSrc);
                    typeIcon.prop('src', iconSrc + '?' + new Date().getTime());

                }
            }

        });

    },

    updateTextFile : function(file, text) {

        

        var chunks = Math.ceil(text.length / chunkSize);
        
        //console.log(text, text.length, chunks);
                
        for (var c=0; c<chunks; c++) {
                        
            var start = c*chunkSize;
            var end = (c+1)*chunkSize;
                        
            var chunk = utf8_to_b64(text.substring(start,end));
            // TODO: check if we can send binary data directly

            Command.chunk(file.id, c, chunkSize, chunk);

        }

    },

    appendEditFileIcon : function(parent, file) {
        
        var editIcon = $('.edit_file_icon', parent);
        
        if (!(editIcon && editIcon.length)) {
            parent.append('<img title="Edit ' + file.name + ' [' + file.id + ']" alt="Edit ' + file.name + ' [' + file.id + ']" class="edit_file_icon button" src="icon/pencil.png">');
        }
        
        $(parent.children('.edit_file_icon')).on('click', function(e) {
            e.stopPropagation();
            var self = $(this);
            //var text = self.parent().find('.file').text();
            Structr.dialog('Edit ' + file.name, function() {
                if (debug) console.log('content saved')
            }, function() {
                if (debug) console.log('cancelled')
            });
            _Files.editContent(this, file, $('#dialogBox .dialogText'));
        });
    },

    editContent : function (button, file, element) {
        var headers = {};
        headers['X-StructrSessionToken'] = token;
        var text;
        
        $.ajax({
            url: viewRootUrl + file.name + '?edit',
            async: true,
            //dataType: 'json',
            contentType: 'text/plain',
            headers: headers,
            success: function(data) {
                //console.log(data);
                text = data;
                
                var mode;
                
                if (file.name.endsWith('.css')) {
                    mode = 'text/css';
                } else if (file.name.endsWith('.js')) {
                    mode = 'text/javascript';
                } else {
                    mode = 'text/plain';
                }
                
                if (isDisabled(button)) return;
                var div = element.append('<div class="editor"></div>');
                if (debug) console.log(div);
                var contentBox = $('.editor', element);
                editor = CodeMirror(contentBox.get(0), {
                    value: unescapeTags(text),
                    mode:  mode,
                    lineNumbers: true
                //            ,
                //            onChange: function(cm, changes) {
                //                
                //                var element = $( '.' + entity.id + '_')[0];
                //                
                //                text1 = $(element).children('.content_').text();
                //                text2 = editor.getValue();
                //                
                //                if (!text1) text1 = '';
                //                if (!text2) text2 = '';
                //		
                //                if (debug) console.log('Element', element);
                //                if (debug) console.log(text1);
                //                if (debug) console.log(text2);
                //                
                //                if (text1 == text2) return;
                //                editorCursor = cm.getCursor();
                //                if (debug) console.log(editorCursor);
                //
                //                Command.patch(entity.id, text1, text2);
                //				
                //            }
                });

                editor.id = file.id;
                
                element.parent().children('.dialogBtn').append('<button id="saveFile"> Save </button>');
                $(element.parent().find('button#saveFile').first()).on('click', function(e) {
                    e.stopPropagation();
                    
                    //console.log(editor.getValue());
                    
                    _Files.updateTextFile(file, editor.getValue());
                   
                });
                        
         

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
