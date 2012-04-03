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

var components, elements;

$(document).ready(function() {
    Structr.registerModule('components', _Components);
});

var _Components = {

    icon : 'icon/package.png',
    add_icon : 'icon/package_add.png',
    delete_icon : 'icon/package_delete.png',

    init : function() {
        Structr.classes.push('component');
    },

    onload : function() {
        //Structr.activateMenuEntry('resources');
        if (debug) console.log('onload');
        if (palette) palette.remove();

        main.append('<table id="resourcesEditor"><tr><td id="components"></td><td id="elements"></td></tr></table>');

        components = $('#components');
        elements = $('#elements');

        _Components.refresh();
        _Components.refreshElements();
    },
    
    refresh : function() {
        components.empty();
        if (_Components.show()) {
            components.append('<button class="add_component_icon button"><img title="Add Component" alt="Add Component" src="' + _Components.add_icon + '"> Add Component</button>');
            $('.add_component_icon', main).on('click', function() {
                _Components.addComponent(this);
            });
        }
    },

    refreshElements : function() {
        elements.empty();
        if (_Elements.show()) {
            elements.append('<button class="add_element_icon button"><img title="Add Element" alt="Add Element" src="' + _Elements.add_icon + '"> Add Element</button>');
            $('.add_element_icon', main).on('click', function() {
                _Components.addElement(this);
            });
        }
    },
    
    show : function() {
        return _Entities.showEntities('Component');
    },

    appendComponentElement : function(component, parentId, resourceId) {
        if (debug) console.log('Components.appendComponentElement: parentId: ' + parentId + ', resourceId: ' + resourceId);

        var parent = Structr.findParent(parentId, resourceId, components);
        
        parent.append('<div class="component ' + component.id + '_">'
            + '<img class="typeIcon" src="'+ _Components.icon + '">'
            + '<b class="name_">' + component.name + '</b> <span class="id">' + component.id + '</span>'
            + '</div>');
        var div = $('.' + component.id + '_', parent);
        div.append('<img title="Delete component \'' + component.name + '\' ' + component.id + '" alt="Delete component \'' + component.name + '\' ' + component.id + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function() {
            _Components.deleteComponent(this, component);
        });
        //        div.append('<img class="add_icon button" title="Add Component" alt="Add Component" src="icon/add.png">');
        //        $('.add_icon', div).on('click', function() {
        //            Resources.addComponent(this, resource);
        //        });
        $('b', div).on('click', function() {
            //_Entities.showProperties(this, component, 'all', $('.' + component.id + '_', components));
	    _Entities.showProperties(this, component, 'all', $('#dialogBox .dialogText'));
        });

        div.droppable({
            accept: '.element',
            greedy: true,
            hoverClass: 'componentHover',
            drop: function(event, ui) {
                var elementId = getIdFromClassString(ui.draggable.attr('class'));
                var resourceId = getIdFromClassString($(this).attr('class'));

                if (!resourceId) resourceId = '*';

                var pos = $('.element', $(this)).length;
                if (debug) console.log(pos);
                var props = '"' + resourceId + '" : "' + pos + '"';
                if (debug) console.log(props);
                _Entities.addSourceToTarget(elementId, resourceId, props);
            }
        });

        return div;
    },

    appendElementElement : function(element, parentId, resourceId) {
        if (debug) console.log('Components.appendElementElement');
        var div = _Elements.appendElementElement(element, parentId, resourceId);
        //console.log(div);
        if (parentId) {

            $('.delete_icon', div).remove();
            div.append('<img title="Remove element \'' + element.name + '\' from resource ' + parentId + '" '
                + 'alt="Remove element ' + element.name + ' from ' + parentId + '" class="delete_icon button" src="icon/brick_delete.png">');
            $('.delete_icon', div).on('click', function() {
                _Entities.removeSourceFromTarget(element.id, parentId);
            });
        }
        //        var elements = element.children;
        //
        //        if (elements && elements.length > 0) {
        //            disable($('.delete_icon', div));
        //            $(elements).each(function(i, child) {
        //                if (child.type == "Element") {
        //                    Resources.appendElementElement(child, element.id);
        //                } else if (child.type == "Content") {
        //                    Resources.appendContentElement(child, element.id);
        //                }
        //            });
        //        }

        div.droppable({
            accept: '.element',
            greedy: true,
            hoverClass: 'elementHover',
            drop: function(event, ui) {
                var resource = $(this).closest( '.resource')[0];
                if (debug) console.log(resource);
                var contentId = getIdFromClassString(ui.draggable.attr('class'));
                var elementId = getIdFromClassString($(this).attr('class'));
                var pos = $('.content', $(this)).length;
                if (debug) console.log(pos);
                var props;
                if (resource) {
                    var resourceId = getIdFromClassString($(resource).attr('class'));
                    props = '"' + resourceId + '" : "' + pos + '"';
                } else {
                    props = '"*" : "' + pos + '"';
                }
                if (debug) console.log(props);
                _Entities.addSourceToTarget(contentId, elementId, props);
            }
        });

        return div;
    },

    addComponent : function(button) {
        return _Entities.add(button, 'Component');
    },

    addElement : function(button) {
        return _Entities.add(button, 'Element');
    },
    
    deleteComponent : function(button, component) {
        if (debug) console.log('delete component ' + component);
        deleteNode(button, component);
    }

};