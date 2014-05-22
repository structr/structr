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

var images, folders, drop;
var fileList;
var chunkSize = 1024*64;
var sizeLimit = 1024*1024*70;
var win = $(window);
var timeout, attempts = 0, maxRetry = 10;

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
        Structr.makePagesMenuDroppable();
    },
    resize : function() {

        var windowWidth = win.width();
        var windowHeight = win.height();
        var headerOffsetHeight = 82;

        var fw = 0;

        if (folders && folders.length) {
            fw = Math.max(180, Math.min(windowWidth/2, 360));
            folders.css({
                width: fw + 'px',
                height: windowHeight - headerOffsetHeight + 'px'
            });
        }
        
        if (images && images.length) {
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

        $('#main-help a').attr('href', 'http://docs.structr.org/frontend-user-guide#Images');
        
        main.append('<div id="dropArea"><div class="fit-to-height" id="images"></div></div>');
        images = $('#images');
        
        // clear files and folders
        if (files) files.length = 0;
        if (folders) folders.length = 0;
        
        _Images.refreshImages();
        
        //_Images.resize();
    },
    unload: function() {
        $(main.children('#dropArea')).remove();
    },
    refreshImages : function() {

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
                    
                    $(filesToUpload).each(function(i, file) {
                        if (file) Command.createFile(file);
                    });

                }

                return false;
            });
        }        
        Structr.addPager(images, false, 'Image');
        _Images.resize();
    },

    getIcon : function(file) {
        var icon = file.tnSmall.path;
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
        
        var parent;
        if (img.parent) {
            
            var existingParent = Structr.node(img.parent.id);
            
            if (existingParent) {
                parent = existingParent;
            } else {
                var name = img.parent.name;
                images.append('<div id="id_' + img.parent.id + '" class="image-folder"><b title="' + name + '" class="name_">' + fitStringToWidth(name, 100, 'name_') + '</b> <span class="id">' + img.parent.id + '</span></div>');
                parent = $('#id_' + img.parent.id);
                
                parent.children('b.name_').on('click', function(e) {
                    e.stopPropagation();
                    _Entities.makeNameEditable(parent, 100);
                });
                
            }
            
            
        } else {
            parent = images;
        }
        
            
        //var tn = '/' + img.tnSmall.id;
        parent.append('<div id="id_' + img.id + '" class="node image">'
            + '<div class="wrap"><img class="thumbnail" src="'+ tn + '"></div>'
            + '<b title="' + img.name + '" class="name_">' + fitStringToWidth(img.name, 98) + '</b> <span class="id">' + img.id + '</span>'
            + '<div class="progress"><div class="bar"><div class="indicator"><span class="part"></span>/<span class="size">' + img.size + '</span></div></div></div>'
            + '<div class="icons"></div></div>');
        div = Structr.node(img.id);
        if (!div || !div.length) return;
            
        var tnSmall = img.tnSmall;
        if (tnSmall) {
            _Images.showThumbnails(img, div);
        } else {
            _Images.reloadThumbnail(img.id, div);
        }
        
        var iconArea = $('.icons', div);

        _Entities.appendAccessControlIcon(iconArea, img);

        div.append('<img title="Push image \'' + img.name + '\'" alt="Push image \'' + img.name + '\'" class="push_icon button" src="icon/page_white_get.png">');
        div.children('.push_icon').on('click', function() {
            Structr.pushDialog(img.id, false);
            return false;
        });

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
            appendTo: '#main',
            zIndex: 2,
            start: function(e, ui) {
                //$(this).hide();
            },
            stop: function(e, ui) {
                $(this).show();
                $('#pages_').droppable('enable').removeClass('nodeHover');
            }
        });

        _Entities.appendEditPropertiesIcon(iconArea, img);
        _Images.appendEditFileIcon(iconArea, img);      

        _Entities.setMouseOver(iconArea);
        
        return div;
    },

    showThumbnails : function(img, el) {
        $('.thumbnail', el).attr('src', '/' + img.tnSmall.id);
        $('.thumbnail', el).on('mousedown', function(e) {
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
            
            tnZoom.on('mouseup', function(e) {
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
        timeout = window.setTimeout(function() {
            if (attempts >= maxRetry) {
                window.clearTimeout(timeout);
            } else {
                attempts++;
                _Crud.crudRead('Image', id, function(img) {
                    var tn = img.tnSmall;
                    if (!tn) {
                        _Images.reloadThumbnail(id, el);
                    } else {
                        _Images.showThumbnails(img, el);
                    }
                });
            }
        }, 1000);
    },

    appendEditFileIcon : function(parent, file) {
        
        var editIcon = $('.edit_file_icon', parent);
        
        if (!(editIcon && editIcon.length)) {
            parent.append('<img title="Edit ' + file.name + ' [' + file.id + ']" alt="Edit ' + file.name + ' [' + file.id + ']" class="edit_file_icon button" src="icon/pencil.png">');
        }
        
        $(parent.children('.edit_file_icon')).on('click', function(e) {
            e.stopPropagation();
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
        element.append('Download links: <a href="' + image.path + '">Path</a>&nbsp;|&nbsp;<a href="/' + image.id + '">UUID</a><br><br><img src="/' + image.id + '">');
    }    
};
