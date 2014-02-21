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

var images, files, folders, drop;
var fileList;
var chunkSize = 1024 * 64;
var sizeLimit = 1024 * 1024 * 70;
var win = $(window);

$(document).ready(function() {
    Structr.registerModule('files', _Files);
    Structr.classes.push('file');
    Structr.classes.push('folder');
    //    Structr.classes.push('image');
    _Files.resize();
    win.resize(function() {
        _Files.resize();
    });
});

var _Files = {
    icon: 'icon/page_white.png',
    add_file_icon: 'icon/page_white_add.png',
    delete_file_icon: 'icon/page_white_delete.png',
    add_folder_icon: 'icon/folder_add.png',
    folder_icon: 'icon/folder.png',
    delete_folder_icon: 'icon/folder_delete.png',
    download_icon: 'icon/basket_put.png',
    init: function() {

        log('_Files.init');

        Structr.initPager('File', 1, 25);
        Structr.initPager('Folder', 1, 25);
        
        Structr.makePagesMenuDroppable();
        
    },
    resize: function() {

        var windowWidth = win.width();
        var windowHeight = win.height();
        var headerOffsetHeight = 82;

        if (folders) {
            folders.css({
                width: Math.max(180, Math.min(windowWidth / 3, 360)) + 'px',
                height: windowHeight - headerOffsetHeight + 'px'
            });
        }

        if (files) {
            files.css({
                width: Math.max(180, Math.min(windowWidth / 3, 360)) + 'px',
                height: windowHeight - headerOffsetHeight + 'px'
            });
        }

        Structr.resize();

    },
    onload: function() {

        _Files.init();

        //main.append('<table id="dropArea"><tr><td id="folders"></td><td id="files"></td><td id="images"></td></tr></table>');
        main.append('<div id="dropArea"><div class="fit-to-height" id="folders"></div><div class="fit-to-height" id="files"></div>');
        //main.append('<table id="dropArea"><tr><<td class="fit-to-height" id="folders"></td><td class="fit-to-height" id="files"></td></tr></table>');
        folders = $('#folders');
        files = $('#files');
        
        // clear images
        if (images) images.length = 0;

        _Files.refreshFolders();
        _Files.refreshFiles();
        
    },
    unload: function() {
        $(main.children('table')).remove();
    },
    refreshFiles: function() {
        files.empty();
        files.append('<button class="add_file_icon button"><img title="Add File" alt="Add File" src="' + _Files.add_file_icon + '"> Add File</button>');
        $('.add_file_icon', main).on('click', function(e) {
            e.stopPropagation();
            Command.create({'type': 'File', 'size': 0});
        });

        if (window.File && window.FileReader && window.FileList && window.Blob) {

            //files.append('<h2>Files</h2>');

            drop = $('#dropArea');

            drop.on('dragover', function(event) {
                log('dragging over #files area');
                event.originalEvent.dataTransfer.dropEffect = 'copy';
                return false;
            });

            drop.on('drop', function(event) {

                if (!event.originalEvent.dataTransfer) {
                    return;
                }

                log('dropped something in the #files area')

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
                            if (file)
                                Command.createFile(file);
                        });
                    });

                } else {
                    $(filesToUpload).each(function(i, file) {
                        Command.createFile(file);
                    });
                }

                return false;
            });
        }
        Structr.addPager(files, true, 'File');
        _Files.resize();
    },
    refreshFolders: function() {
        folders.empty();
        //folders.append('<h2>Folders</h2>');
        folders.append('<button class="add_folder_icon button"><img title="Add Folder" alt="Add Folder" src="' + _Files.add_folder_icon + '"> Add Folder</button>');
        $('.add_folder_icon', main).on('click', function(e) {
            e.stopPropagation();
            Command.create({'type': 'Folder'});
        });
        Structr.addPager(folders, true, 'Folder');
    },
    getIcon: function(file) {
        var icon = _Files.icon; // default
        if (file && file.contentType) {

            if (file.contentType.indexOf('pdf') > -1) {
                icon = 'icon/page_white_acrobat.png';
            } else if (file.contentType.indexOf('text') > -1) {
                icon = 'icon/page_white_text.png';
            } else if (file.contentType.indexOf('xml') > -1) {
                icon = 'icon/page_white_code.png';
            }
        }

        return icon;
    },
    appendFileElement: function(file, add) {

        var icon = _Files.getIcon(file);
        var folderId = file.parent ? file.parent.id : null;

        var parent = Structr.findParent(folderId, null, null, files);

        if (!parent || (parent !== files && !isExpanded(folderId))) {
            return false;
        }
        //if (add) _Entities.ensureExpanded(parent);

        var delIcon, newDelIcon;
        var div = Structr.node(file.id);

        if (div && div.length) {

            var formerParent = div.parent();

            if (!Structr.containsNodes(formerParent)) {
                _Entities.removeExpandIcon(formerParent);
                enable($('.delete_icon', formerParent)[0]);
            }

        } else {

            parent.append('<div id="id_' + file.id + '" class="node file">'
                    + '<img class="typeIcon" src="' + icon + '">'
                    + '<b title="' + file.name + '" class="name_">' + fitStringToWidth(file.name, 200) + '</b> <span class="id">' + file.id + '</span>'
                    + '<div class="progress"><div class="bar"><div class="indicator"><span class="part"></span>/<span class="size">' + file.size + '</span></div></div></div>'
                    + '</div>');
            div = Structr.node(file.id);

        }

        if (!div || !div.length)
            return;

        _Entities.appendAccessControlIcon(div, file);
        
        if (_Files.isArchive(file)) {
            div.append('<img class="unarchive_icon button" src="icon/compress.png">');
            div.children('.unarchive_icon').on('click', function() {
                log('unarchive', file.id)
                Command.unarchive(file.id);
            });
        }

        div.children('.typeIcon').on('click', function(e) {
            e.stopPropagation();
            window.open(viewRootUrl + file.name, 'Download ' + file.name);
        });
        log(folderId, add);

        delIcon = div.children('.delete_icon');

        if (folderId) {
            newDelIcon = '<img title="Remove file \'' + file.name + '\' from folder ' + folderId + '" alt="Remove file \'' + file.name + '\' from folder" class="delete_icon button" src="' + _Files.delete_file_icon + '">';
            if (delIcon && delIcon.length) {
                delIcon.replaceWith(newDelIcon);
            } else {
                div.append(newDelIcon);
                delIcon = div.children('.delete_icon');
            }
            div.children('.delete_icon').on('click', function(e) {
                e.stopPropagation();
                //_Files.removeFileFromFolder(file.id, folderId, isImage);
                Command.removeSourceFromTarget(file.id, folderId);
            });
            disable(parent.children('.delete_icon')[0]);

        } else {
            newDelIcon = '<img title="Delete file ' + file.name + '\'" alt="Delete file \'' + file.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">';
            if (add && delIcon && delIcon.length) {
                delIcon.replaceWith(newDelIcon);
            } else {
                div.append(newDelIcon);
                delIcon = div.children('.delete_icon');
            }
            div.children('.delete_icon').on('click', function(e) {
                e.stopPropagation();
                _Entities.deleteNode(this, file);
            });

        }

        div.draggable({
            revert: 'invalid',
            helper: 'clone',
            //containment: 'document',
            //stack: '.node',
            appendTo: '#main',
            zIndex: 2,
            start: function(e, ui) {
                $(this).hide();
                $(ui)[0].helper.css({
                   width: files.width() + 'px' 
                });
            },
            stop: function(e, ui) {
                $(this).show();
                //$('#pages_').droppable('enable').removeClass('nodeHover');
            }
        });

        _Files.appendEditFileIcon(div, file);
        _Entities.appendEditPropertiesIcon(div, file);

        _Entities.setMouseOver(div);

        return div;
    },
    appendFolderElement: function(folder) {

        log('appendFolderElement', folder, folder.parent);

        var hasParent = folder.parent && folder.parent.id;

        log(folder.name, 'has parent?', hasParent);

        var parentId, parentFolderElement;
        if (folder.parent && folder.parent.id) {
            parentId = folder.parent.id;
            parentFolderElement = Structr.node(parentId);
        }

        var parent = parentFolderElement ? parentFolderElement : folders;

        if (!parent)
            return false;

        parent.append('<div id="id_' + folder.id + '" structr_type="folder" class="node folder">'
                + '<img class="typeIcon" src="' + _Files.folder_icon + '">'
                + '<b title="' + folder.name + '" class="name_">' + fitStringToWidth(folder.name, 200) + '</b> <span class="id">' + folder.id + '</span>'
                + '</div>');

        var div = Structr.node(folder.id);

        if (!div || !div.length)
            return;

        _Entities.appendAccessControlIcon(div, folder);

        var delIcon = div.children('.delete_icon');

        if (parent !== folders) {
            var newDelIcon = '<img title="Remove folder ' + folder.name + '\' from folder ' + folder.parent.id + '" alt="Remove folder ' + folder.name + '\' from folder ' + folder.parent.id + '" class="delete_icon button" src="' + _Files.delete_folder_icon + '">';
            if (delIcon && delIcon.length) {
                delIcon.replaceWith(newDelIcon);
            } else {
                div.append(newDelIcon);
                //delIcon = $('.delete_icon', div);
            }
            div.children('.delete_icon').on('click', function(e) {
                e.stopPropagation();
                Command.removeChild(folder.id);
            });
            //disable($('.delete_icon', parent)[0]);

        } else {
            newDelIcon = '<img title="Delete folder \'' + folder.name + '\'" alt="Delete folder \'' + folder.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">';
            if (delIcon && delIcon.length) {
                delIcon.replaceWith(newDelIcon);
            } else {
                div.append(newDelIcon);
                //delIcon = $('.delete_icon', div);
            }
            div.children('.delete_icon').on('click', function(e) {
                e.stopPropagation();
                _Entities.deleteNode(this, folder);
            });
        }

        var hasChildren = (folder.folders && folder.folders.length) || (folder.files && folder.files.length);

        log(folder.name, 'has children?', hasChildren, 'is expanded?', isExpanded(folder.id));

        _Entities.appendExpandIcon(div, folder, hasChildren);

        div.draggable({
            revert: 'invalid',
            //helper: 'clone',
            //containment: '#main',
            stack: '.node'
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
                if (!(fileId === folderId)) {
                    var nodeData = {};
                    nodeData.id = fileId;
                    addExpandedNode(folderId);
                    //log('addExpandedNode(folderId)', addExpandedNode(folderId));
                    Command.appendFile(fileId, folderId);
                    $(ui.draggable).remove();
                    //Command.createAndAdd(folderId, nodeData);
                }
            }
        });

        _Entities.appendEditPropertiesIcon(div, folder);
        _Entities.setMouseOver(div);

        return div;
    },
//    removeImageFromFolder: function(imageId, folderId) {
//        log('removeImageFromFolder', imageId, folderId);
//        _Files.removeFileFromFolder(imageId, folderId, true);
//    },
    uploadFile: function(file) {

        $(fileList).each(function(i, fileObj) {

            if (fileObj.name === file.name) {

                log('Uploading chunks for file ' + file.id);

                var reader = new FileReader();
                reader.readAsBinaryString(fileObj);
                //reader.readAsText(fileObj);

                var chunks = Math.ceil(fileObj.size / chunkSize);
                //console.log('file size: ' + fileObj.size + ', chunk size: ' + chunkSize + ', chunks: ' + chunks);

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

                    log('File was read into memory.', f);
                    var binaryContent = f.target.result;
                    log('uploadFile: binaryContent', binaryContent);

                    for (var c = 0; c < chunks; c++) {

                        var start = c * chunkSize;
                        var end = (c + 1) * chunkSize;

                        var chunk = window.btoa(binaryContent.substring(start, end));
                        // TODO: check if we can send binary data directly

                        Command.chunk(file.id, c, chunkSize, chunk, chunks);

                    }

                    var typeIcon = Structr.node(file.id).find('.typeIcon');
                    var iconSrc = typeIcon.prop('src');
                    log('Icon src: ', iconSrc);
                    typeIcon.prop('src', iconSrc + '?' + new Date().getTime());

                }
            }

        });

    },
    updateTextFile: function(file, text) {
        var chunks = Math.ceil(text.length / chunkSize);
        //console.log(text, text.length, chunks);
        for (var c = 0; c < chunks; c++) {
            var start = c * chunkSize;
            var end = (c + 1) * chunkSize;
            //console.log(text.substring(start,end));
            var chunk = utf8_to_b64(text.substring(start, end));
            //console.log(chunk);
            // TODO: check if we can send binary data directly
            Command.chunk(file.id, c, chunkSize, chunk, chunks);
        }
    },
    appendEditFileIcon: function(parent, file) {

        var editIcon = $('.edit_file_icon', parent);

        if (!(editIcon && editIcon.length)) {
            parent.append('<img title="Edit ' + file.name + ' [' + file.id + ']" alt="Edit ' + file.name + ' [' + file.id + ']" class="edit_file_icon button" src="icon/pencil.png">');
        }

        $(parent.children('.edit_file_icon')).on('click', function(e) {
            e.stopPropagation();
            //var self = $(this);
            //var text = self.parent().find('.file').text();
            Structr.dialog('Edit ' + file.name, function() {
                log('content saved')
            }, function() {
                log('cancelled')
            });
            _Files.editContent(this, file, $('#dialogBox .dialogText'));
        });
    },
    editContent: function(button, file, element) {
        //debug = true;
        var url = viewRootUrl + file.id + '?edit=1';
        log('editContent', button, file, element, url);
        var headers = {};
        headers['X-StructrSessionToken'] = token;
        var text;

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
        log(viewRootUrl, url);

        $.ajax({
            url: url,
            //async: false,
            dataType: dataType,
            contentType: contentType,
            headers: headers,
            success: function(data) {
                text = data;
                if (isDisabled(button))
                    return;
                element.append('<div class="editor"></div>');
                var contentBox = $('.editor', element);
                editor = CodeMirror(contentBox.get(0), {
                    value: unescapeTags(text),
                    mode: contentType,
                    lineNumbers: true
                });

                editor.id = file.id;

                dialogBtn.append('<button id="saveFile" disabled="disabled" class="disabled"> Save </button>');
                dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');

                dialogSaveButton = $('#saveFile', dialogBtn);
                var saveAndClose = $('#saveAndClose', dialogBtn);

                text1 = text;

                editor.on('change', function(cm, change) {

                    text2 = editor.getValue();

                    if (text1 === text2) {
                        dialogSaveButton.prop("disabled", true).addClass('disabled');
                        saveAndClose.prop("disabled", true).addClass('disabled');
                    } else {
                        dialogSaveButton.prop("disabled", false).removeClass('disabled');
                        saveAndClose.prop("disabled", false).removeClass('disabled');
                    }
                });

                $('button#saveFile', dialogBtn).on('click', function(e) {
                    e.stopPropagation();
                    var newText = editor.getValue();
                    if (text1 === newText) {
                        return;
                    }
                    _Files.updateTextFile(file, newText);
                    text1 = newText;
                    dialogSaveButton.prop("disabled", true).addClass('disabled');
                    saveAndClose.prop("disabled", true).addClass('disabled');
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

            },
            error: function(xhr, statusText, error) {
                console.log(xhr, statusText, error);
            }
        });



    },
    isArchive : function(file) {
        var contentType = file.contentType;
        var extension = file.name.substring(file.name.lastIndexOf('.')+1);
        
        var archiveTypes = ['application/zip', 'application/x-tar', 'application/x-cpio', 'application/x-dump', 'application/x-java-archive'];
        var archiveExtensions = ['zip', 'tar', 'cpio', 'dump', 'jar'];
        
        return isIn(contentType, archiveTypes) || isIn(extension, archiveExtensions);
    }
};
