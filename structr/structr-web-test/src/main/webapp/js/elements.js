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

var elements;

$(document).ready(function() {
    Structr.registerModule('elements', Elements);
});

var Elements = {

    icon : 'icon/brick.png',
    delete_icon : 'icon/brick_delete.png',
	
    init : function() {
    },

    onload : function() {
        if (debug) console.log('onload');
        main.append('<div id="elements"></div>');
        elements = $('#elements');
        Elements.refresh();
    },

    refresh : function() {
        elements.empty();
        if (Elements.show()) {
            elements.append('<button class="add_element_icon button"><img title="Add Element" alt="Add Element" src="icon/brick_add.png"> Add Element</button>');
            $('.add_element_icon', main).on('click', function() {
                Elements.addElement(this);
            });
        }
    },

    show : function() {
        return Entities.showEntities('Element');
    },

    appendElementElement : function(element) {
        elements.append('<div class="nested top element ' + element.id + '_">'
            + '<img class="typeIcon" src="'+ Elements.icon + '">'
            + '<b class="name">' + element.name + '</b> <span class="id">' + element.id + '</span>'
            + '</div>');
        var div = $('.' + element.id + '_');
        div.append('<img title="Delete element \'' + element.name + '\'" alt="Delete element \'' + element.name + '\'" class="delete_icon button" src="' + Elements.delete_icon + '">');
        $('.delete_icon', div).on('click', function() {
            Elements.deleteElement(this, element);
        });
//        div.append('<img class="add_icon button" title="Add Element" alt="Add Element" src="icon/add.png">');
//        $('.add_icon', div).on('click', function() {
//            Resources.addElement(this, resource);
//        });
        $('b', div).on('click', function() {
            Entities.showProperties(this, element, 'all', $('.' + element.id + '_', elements));
        });
        return div;
    },

    addElement : function(button) {
        return Entities.add(button, 'Element');
    },

    deleteElement : function(button, element) {
        if (debug) console.log('delete element ' + element);
        deleteNode(button, element);
    }

};