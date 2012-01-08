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

var contents;

$(document).ready(function() {
    Structr.registerModule('contents', Contents);
});

var Contents = {

    icon : 'icon/page_white.png',
    add_icon : 'icon/page_white_add.png',
    delete_icon : 'icon/page_white_delete.png',
	
    init : function() {
        Structr.classes.push('content');
    },

    onload : function() {
        if (debug) console.log('onload');
        main.append('<div id="contents"></div>');
        contents = $('#contents');
        Contents.refresh();
    },

    refresh : function() {
        contents.empty();
        if (Contents.show()) {
            contents.append('<button class="add_content_icon button"><img title="Add Content" alt="Add Content" src="' + Contents.add_icon + '"> Add Content</button>');
            $('.add_content_icon', main).on('click', function() {
                Contents.addContent(this);
            });
        }
    },

    show : function() {
        return Entities.showEntities('Content');
    },

    appendContentElement : function(content, parentId, resourceId) {
        if (debug) console.log('Contents.appendContentElement: parentId: ' + parentId + ', resourceId: ' + resourceId);

        var parent = Structr.findParent(parentId, resourceId, contents);
        
        parent.append('<div class="content ' + content.id + '_">'
            + '<img class="typeIcon" src="'+ Contents.icon + '">'
            + '<b class="name_">' + content.name + '</b> <span class="id">' + content.id + '</span>'
            + '</div>');
        var div = $('.' + content.id + '_', parent);
        div.append('<img title="Delete content \'' + content.name + '\'" alt="Delete content \'' + content.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function() {
            Contents.deleteContent(this, content);
        });
        //        div.append('<img class="add_icon button" title="Add Element" alt="Add Element" src="icon/add.png">');
        //        $('.add_icon', div).on('click', function() {
        //            Resources.addElement(this, resource);
        //        });
        $('b', div).on('click', function() {
            Entities.showProperties(this, content, 'all', $('.' + content.id + '_', contents));
        });
        return div;
    },

    addContent : function(button) {
        return Entities.add(button, 'Content');
    },

    deleteContent : function(button, content) {
        if (debug) console.log('delete content ' + content);
        deleteNode(button, content);
    }

};