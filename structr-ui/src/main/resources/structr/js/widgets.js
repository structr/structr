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

var widgets;
var win = $(window);

$(document).ready(function() {
    Structr.registerModule('widgets', _Widgets);
    Structr.classes.push('widget');
    _Widgets.resize();
    win.resize(function() {
        _Widgets.resize();
    });
});

var _Widgets = {

    icon : 'icon/page_white.png',
    add_file_icon : 'icon/page_white_add.png',
    delete_file_icon : 'icon/page_white_delete.png',
    add_folder_icon : 'icon/folder_add.png',
    folder_icon : 'icon/folder.png',
    delete_folder_icon : 'icon/folder_delete.png',
    download_icon : 'icon/basket_put.png',
	
    init : function() {

        Structr.initPager('Widget', 1, 25);
        
    },

    resize : function() {

        var windowWidth = win.width();
        var windowHeight = win.height();
        var headerOffsetHeight = 82;

        if (widgets) {
            widgets.css({
                width: Math.max(180, Math.min(windowWidth/3, 360)) + 'px',
                height: windowHeight - headerOffsetHeight + 'px'
            });
        }

    },

    onload : function() {
        
        _Widgets.init();
        
        log('onload');
        if (palette) palette.remove();

        main.append('<table id="dropArea"><tr><td id="widgets"></td></tr></table>');
        widgets = $('#widgets');
        
        _Widgets.refreshWidgets();
    },

    unload : function() {
        $(main.children('table')).remove();
    },

    refreshWidgets : function() {
        widgets.empty();
        widgets.append('<button class="add_widgets_icon button"><img title="Add Widget" alt="Add Widget" src="' + _Widgets.getIcon() + '"> Add Widget</button>');
        $('.add_widgets_icon', main).on('click', function(e) {
            e.stopPropagation();
            Command.create({'type':'Widget'});
        });
        Structr.addPager(widgets, 'Widget');
        _Widgets.resize();
    },

    getIcon : function() {
        var icon = _Widgets.icon; // default
        return icon;
    },

    appendWidgetElement : function(widget, add) {

        log('Widgets.appendWidgetElement', widget);
        
        var icon = _Widgets.getIcon(widget);
        var parent = widgets;
        
        var delIcon, newDelIcon;
        var div = Structr.node(widget.id);
        if (div && div.length) {
            
            var formerParent = div.parent();
            
            if (!Structr.containsNodes(formerParent)) {
                _Entities.removeExpandIcon(formerParent);
                enable($('.delete_icon', formerParent)[0]);
            }            
            
        } else {
        
            parent.append('<div id="id_' + widget.id + '" class="node widget">'
                + '<img class="typeIcon" src="'+ icon + '">'
                + '<b title="' + widget.name + '" class="name_">' + fitStringToSize(widget.name, 200) + '</b> <span class="id">' + widget.id + '</span>'
                + '</div>');
            div = Structr.node(widget.id);
            
        }

        _Entities.appendAccessControlIcon(div, widget);

        delIcon = div.children('.delete_icon');

        newDelIcon = '<img title="Delete file ' + widget.name + '\'" alt="Delete file \'' + widget.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">';
        if (add && delIcon && delIcon.length) {
            delIcon.replaceWith(newDelIcon);
        } else {
            div.append(newDelIcon);
            delIcon = div.children('.delete_icon');
        } 
        div.children('.delete_icon').on('click', function(e) {
            e.stopPropagation();
            _Entities.deleteNode(this, widget);
        });
		
        div.draggable({
            revert: 'invalid',
            helper: 'clone',
            //containment: '#main',
            stack: '.node',
            appendTo: '#main',
            stop : function(e,ui) {
                $('#pages_').droppable('enable').removeClass('nodeHover');
            }
        });

        div.append('<img title="Edit Source" alt="Edit Source of ' + widget.id + '" class="edit_icon button" src="icon/pencil.png">');
        $('.edit_icon', div).on('click', function(e) {
            e.stopPropagation();
            var text = widget.source || '';
            Structr.dialog('Edit source of ' + widget.id, function() {
                log('Widget source saved')
            }, function() {
                log('cancelled')
            });
            _Widgets.editContent(this, widget, text, dialogText);
        });
        
        _Entities.appendEditPropertiesIcon(div, widget);

        _Entities.setMouseOver(div);
        
        return div;
    },

    updateWidget : function(widget, text) {
        var chunks = Math.ceil(text.length / chunkSize);
        //console.log(text, text.length, chunks);
        for (var c=0; c<chunks; c++) {
            var start = c*chunkSize;
            var end = (c+1)*chunkSize;
            //console.log(text.substring(start,end));
            var chunk = utf8_to_b64(text.substring(start,end));
            //console.log(chunk);
            // TODO: check if we can send binary data directly
            Command.chunk(widget.id, c, chunkSize, chunk);
        }
    },

    editContent : function (button, entity, text, element) {
        if (isDisabled(button)) return;
        var div = element.append('<div class="editor"></div>');
        log(div);
        var contentBox = $('.editor', element);
        editor = CodeMirror(contentBox.get(0), {
            value: unescapeTags(text),
            mode:  'text/html',
            lineNumbers: true
        });
        editor.focus();
        
        element.append('<button id="editorSave">Save</button>');

        $('#editorSave', element).on('click', function() {
                
            Command.setProperty(entity.id, 'source', editor.getValue(), false, function() {
                dialogMsg.html('<div class="infoBox success">Widget source saved.</div>');
                $('.infoBox', dialogMsg).delay(2000).fadeOut(200);
            });
            
        });
        
        editor.id = entity.id;

    }
};
