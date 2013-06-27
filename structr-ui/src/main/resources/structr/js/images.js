/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
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

var images, folders, drop;
var fileList;
var chunkSize = 1024*64;
var sizeLimit = 1024*1024*42;
var win = $(window);

$(document).ready(function() {
    Structr.registerModule('images', _Images);
    //    Structr.classes.push('folder');
    Structr.classes.push('image');
    
    win.resize(function() {
        _Images.resize();
    });
    
});

var _Images = {

    icon : 'icon/page_white.png',
    add_file_icon : 'icon/page_white_add.png',
    delete_file_icon : 'icon/page_white_delete.png',
    //    add_folder_icon : 'icon/folder_add.png',
    //    folder_icon : 'icon/folder.png',
    //    delete_folder_icon : 'icon/folder_delete.png',
    download_icon : 'icon/basket_put.png',
	
    init : function() {
        Structr.initPager('Image', 1, 100);
    },
    resize : function() {

        var windowWidth = win.width();
        var windowHeight = win.height();
        var headerOffsetHeight = 82;

        var fw = 0;

        if (folders) {
            fw = Math.max(180, Math.min(windowWidth/2, 360));
            folders.css({
                width: fw + 'px',
                height: windowHeight - headerOffsetHeight + 'px'
            });
        }
        
        if (images) {
            images.css({
                width: Math.max(400, (windowWidth - fw - 36)) + 'px',
                height: windowHeight - headerOffsetHeight + 'px'
            });
        }
    },

    /**
     * The onload method is called whenever this module is activated
     */
    onload : function() {
        
        _Images.init();
        
        if (palette) palette.remove();

        //main.append('<table id="dropArea"><tr><td id="folders"></td><td id="images"></td></tr></table>');
        main.append('<table id="dropArea"><tr><td id="images"></td></tr></table>');
        //folders = $('#folders');
        images = $('#images');
        
        //_Images.refreshFolders();
        _Images.refreshImages();

    //_Images.resize();
    },

    unload : function() {
        $(main.children('table')).remove();
    },
	
    refreshImages : function() {
        _Images.resize();
        images.empty();
        images.append('<h1>Images</h1>');
        
        if (window.File && window.FileReader && window.FileList && window.Blob) {
            
            drop = $('#dropArea');

            drop.on('dragover', function(event) {
                //console.log('dragging over #images area');
                event.originalEvent.dataTransfer.dropEffect = 'copy';
                return false;
            });
            
            drop.on('drop', function(event) {
                
                if (!event.originalEvent.dataTransfer) {
                    log(event);
                    return;
                }
                
                log('dropped something in the #files area')
                
                event.stopPropagation();
                event.preventDefault();
                
                fileList = event.originalEvent.dataTransfer.files;
                log(fileList);
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
                            log(file);
                            if (file) Command.createFile(file);
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
                        log(file);
                        if (file) Command.createFile(file);
                    });

                }

                return false;
            });
        }        
        Structr.addPager(images, 'Image');
    },
	
    refreshFolders : function() {
        folders.empty();
        folders.append('<button class="add_folder_icon button"><img title="Add Folder" alt="Add Folder" src="' + _Images.add_folder_icon + '"> Add Folder</button>');
        if (Structr.addPager(folders, 'Folder')) {

            $('.add_folder_icon', main).on('click', function(e) {
                e.stopPropagation();
                var entity = {};
                entity.type = 'Folder';
                Command.create(entity);
            });
        }
    },

    getIcon : function(file) {
        var icon = viewRootUrl + file.name;
        return icon;
    },

    appendImageElement : function(img) {
        
        log('Images.appendImageElement', img);
        
        //if (!folderId && file.parentFolder) return false;
        
        // suppress images without thumbnails
        if (img.isThumbnail) return false;

        var div;
        var delIcon, newDelIcon;
        div = Structr.node(img.id);
        
        var tn = '/structr/img/ajax-loader.gif';
        
        if (!images) return;
            
        //var tn = '/' + img.tnSmall.id;
        images.append('<div id="id_' + img.id + '" class="node image">'
            + '<div class="wrap"><img class="thumbnail" src="'+ tn + '"></div>'
            + '<b title="' + img.name + '" class="name_">' + fitStringToSize(img.name, 98) + '</b> <span class="id">' + img.id + '</span>'
            + '<div class="icons"></div></div>');
        div = Structr.node(img.id);

            
        var tnSmall = img.tnSmall;
        if (tnSmall) {
            _Images.showThumbnails(img, div);
        } else {
            _Images.reloadThumbnail(img.id, div);
        }
        
        var iconArea = $('.icons', div);

        _Entities.appendAccessControlIcon(iconArea, img);

        delIcon = $('.delete_icon', div);

        newDelIcon = '<img title="Delete ' + img.name + ' \'' + img.name + '\'" alt="Delete ' + img.name + ' \'' + img.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">';
        if (delIcon && delIcon.length) {
            delIcon.replaceWith(newDelIcon);
        } else {
            iconArea.append(newDelIcon);
            delIcon = $('.delete_icon', div);
        } 
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            _Entities.deleteNode(this, img);
        });
        
        div.draggable({
            revert: 'invalid',
            helper: 'clone',
            //containment: '#main',
            stack: 'div',
            stop : function(e,ui) {
                $('#pages_').removeClass('nodeHover').droppable('enable');
            }
        });

        _Entities.appendEditPropertiesIcon(iconArea, img);
        _Images.appendEditFileIcon(iconArea, img);      

        _Entities.setMouseOver(iconArea);
        
        return div;
    },

    showThumbnails : function(img, el) {
        $('.thumbnail', el).attr('src', '/' + img.tnSmall.id);
        $('.thumbnail', el).on('mouseenter', function(e) {
            e.stopPropagation();
            $('.thumbnailZoom', images).remove();
            images.append('<img class="thumbnailZoom" src="/' + img.tnMid.id + '">');
            var tnZoom = $($('.thumbnailZoom', images)[0]);
            
            tnZoom.on('load', function() {
                log(tnZoom, tnZoom.position(), tnZoom.width(), tnZoom.height());
                var pos = el.position();
                
                tnZoom.css({
                    top:  (pos.top+(el.height()-tnZoom.height())/2) + 'px',
                    left: (pos.left+(el.width()-tnZoom.width())/2) + 6 + 'px'
                }).show();
            });
            
            tnZoom.on('mouseleave', function(e) {
                e.stopPropagation();
                $('.thumbnailZoom', images).remove();
            });
            
            tnZoom.on('click', function(e) {
                e.stopPropagation();
                Structr.dialog(img.name, function() {
                    return true;
                }, function() {
                    return true;
                });
                    
                _Images.showImageDetails($(this), img, dialogText);
            });

        });
    },

    reloadThumbnail : function(id, el) {
        // wait 1 sec. and try again
        window.setTimeout(function() {
            headers = {
                'X-StructrSessionToken' : token
            };
            _Crud.crudRead('Image', id, function(img) {
                var tn = img.tnSmall;
                if (!tn) {
                    _Images.reloadThumbnail(id, el);
                } else {
                    _Images.showThumbnails(img, el);
                }
            });
        }, 5000);
    },

    appendFolderElement : function(folder, folderId, hasChildren) {
		
        log('appendFolderElement', folder, folderId, hasChildren);
        
        var parent = Structr.findParent(folderId, null, null, folders);
        
        log('appendFolderElement parent', parent);
        if (!parent) return false;
        
        //parent.append('<div id="_' + folderId + '" class="node element ' + folderId + '_"></div>');
        
        //var div = Structr.node(entity.id, parentId, componentId, pageId, pos);
        //var div = $('#_' + id);
        
        var delIcon, newDelIcon;    
        var div;
        
        var removeExisting = true;
        
        //div = Structr.node(folder.id);
        div = $('#_' + folder.id);
        
        log('appendFolderElement: parent, div', parent, div);
        
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
                + '<img class="typeIcon" src="'+ _Images.folder_icon + '">'
                + '<b title="' + folder.name + '" class="name_">' + folder.name + '</b> <span class="id">' + folder.id + '</span>'
                + '</div>');
        
            //div = Structr.node(folder.id, parent.id);
            div = $('#_' + folder.id);
        }
        
        delIcon = $('.delete_icon', div);
        
        if (folderId) {
            newDelIcon = '<img title="Remove folder ' + folder.name + '\' from folder ' + folderId + '" alt="Remove folder ' + folder.name + '\' from folder" class="delete_icon button" src="' + _Images.delete_folder_icon + '">';
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
                log('fileId, folderId', fileId, folderId);
                if (!(fileId == folderId)) {
                    var nodeData = {};
                    nodeData.id = fileId;
                    addExpandedNode(folderId);
                    log('addExpandedNode(folderId)', addExpandedNode(folderId));
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

        log('removeFolderFromFolder: fileId=' + folderToRemoveId + ', folderId=' + folderId);
    },
    
    removeFileFromFolder : function(fileId, folderId, isImage) {
        log('removeFileFromFolder', fileId, folderId, isImage);
        
        var parentElement, cls;
        if (isImage) {
            parentElement = images;
            cls = 'image';
        } else {
            parentElement = files;
            cls = 'file';
        }
        
        if (!parentElement) return;

        var folder = Structr.node(folderId);
        var file = Structr.node(fileId, folderId);
        
        log(file, folder);
        
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
        log('removeImageFromFolder', imageId, folderId);
        _Images.removeFileFromFolder(imageId, folderId, true);
    },

    createFile : function(fileObj) {
        var entity = {};
        log(fileObj);
        entity.contentType = fileObj.type;
        entity.name = fileObj.name;
        entity.size = fileObj.size;
        entity.type = isImage(entity.contentType) ? 'Image' : 'File';
        Command.create(entity);
    },
    
    uploadFile : function(file) {

        log(fileList);

        $(fileList).each(function(i, fileObj) {

            log(file);

            if (fileObj.name == file.name) {
     
                log(fileObj);
                log('Uploading chunks for file ' + file.id);
                
                var reader = new FileReader();
                reader.readAsBinaryString(fileObj);
                //reader.readAsText(fileObj);

                var chunks = Math.ceil(fileObj.size / chunkSize);
                log('file size: ' + fileObj.size + ', chunk size: ' + chunkSize + ', chunks: ' + chunks);

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
                    
                    log('File was read into memory.');
                    var binaryContent = f.target.result;
                    log('uploadFile: binaryContent', binaryContent);

                    for (var c=0; c<chunks; c++) {
                        
                        var start = c*chunkSize;
                        var end = (c+1)*chunkSize;
                        
                        var chunk = utf8_to_b64(binaryContent.substring(start,end));
                        // TODO: check if we can send binary data directly

                        Command.chunk(file.id, c, chunkSize, chunk);

                    }
                    var typeIcon = Structr.node(file.id).find('.typeIcon');
                    var iconSrc = typeIcon.prop('src');
                    log('Icon src: ', iconSrc);
                    typeIcon.prop('src', iconSrc + '?' + new Date().getTime());

                }
            }

        });

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
                log('content saved')
            }, function() {
                log('cancelled')
            });
            _Images.editImage(this, file, $('#dialogBox .dialogText'));
        });
    },

    showImageDetails : function(button, image, element) {
        element.append('<img class="imageDetail" src="/' + image.id + '"><br><a href="/' + image.id + '">Download</a>');
    },

    editImage : function (button, image, element) {
        log(image);
        element.append('<img src="/' + image.id + '"><br><a href="' + image.id + '">Download</a>');
    }    
};
