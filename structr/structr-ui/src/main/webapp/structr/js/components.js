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
    Structr.classes.push('component');
});

var _Components = {

    icon : 'icon/package.png',
    add_icon : 'icon/package_add.png',
    delete_icon : 'icon/package_delete.png',

    init : function() {
    //Structr.classes.push('component');
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
	if (Command.list('Component')) {
	    components.append('<button class="add_component_icon button"><img title="Add Component" alt="Add Component" src="' + _Components.add_icon + '"> Add Component</button>');
	    $('.add_component_icon', main).on('click', function() {
		var entity = {};
		entity.type = 'Component';
		Command.create(entity);
	    });
	}
    },

    refreshElements : function() {
	elements.empty();
	if (Command.list('Element')) {
	    elements.append('<button class="add_element_icon button"><img title="Add Element" alt="Add Element" src="' + _Elements.add_icon + '"> Add Element</button>');
	    $('.add_element_icon', main).on('click', function() {
		var entity = {};
		entity.type = 'Element';
		Command.create(entity);
	    });
	}
    },
    
    appendComponentElement : function(component, parentId, resourceId, removeExisting, hasChildren) {
	if (debug) console.log('Components.appendComponentElement: parentId: ' + parentId + ', resourceId: ' + resourceId);

	var parent = Structr.findParent(parentId, resourceId, components);
        
        if (!parent) return false;

	parent.append('<div class="node component ' + component.id + '_">'
	    + '<img class="typeIcon" src="'+ _Components.icon + '">'
	    + '<b class="name_">' + component.structrclass + '</b> <span class="id">' + component.id + '</span>'
	    + '</div>');
	var div = $('.' + component.id + '_', parent);
	div.append('<img title="Delete component \'' + component.structrclass + '\' ' + component.id + '" alt="Delete component \'' + component.structrclass + '\' ' + component.id + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
	$('.delete_icon', div).on('click', function() {
	    _Components.deleteComponent(this, component);
	});

	div.append('<img title="Create Form" alt="Create Form" class="add_form_icon button" src="icon/application_form_add.png">');
	$('.add_form_icon', div).on('click', function() {
	    _Components.createForm(this, component);
	});

	component.resourceId = resourceId;
        
        _Entities.appendExpandIcon(div, entity, hasChildren);
	_Entities.setMouseOver(div);
	_Entities.appendEditPropertiesIcon(div, component);

	div.droppable({
	    accept: '.element',
	    greedy: true,
	    hoverClass: 'nodeHover',
	    drop: function(event, ui) {
		var self = $(this);
		var elementId = getId(ui.draggable);
		var resourceId = getId(self);
		if (!resourceId) resourceId = '*';
		var pos = $('.element', self).length;
		var nodeData = {};
		nodeData.id = elementId;
		var relData = {};
		relData[resourceId] = pos;
		Command.createAndAdd(resourceId, nodeData, relData);
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
		Command.removeSourceFromTarget(element.id, parentId);
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
	    hoverClass: 'nodeHover',
	    drop: function(event, ui) {
		var self = $(this);
		var resource = self.closest( '.resource')[0];
		if (debug) console.log(resource);
		var contentId = getId(ui.draggable);
		var elementId = getId(self);
		var pos = $('.content', self).length;
		if (debug) console.log(pos);
		var resourceId;
		if (resource) {
		    resourceId = getId(resource);
		} else {
		    resourceId = '*';
		}

		var relData = {};
		relData[resourceId] = pos;
		var nodeData = {};
		nodeData.id = contentId;
		Command.createAndAdd(elementId, nodeData, relData);
	    }
	});

	return div;
    },

    deleteComponent : function(button, component) {
	if (debug) console.log('delete component', component);
	_Entities.deleteNode(button, component);
    },

    createForm : function(button, component) {
	console.log('create form', component);

	var form = {};
	form.type = 'Form';
	form.tag = 'form';
	form._html_action = '//' + plural(component.structrclass).toLowerCase();
	form._html_method = 'post';

	var node = $($(button).closest('.node')[0]);

	//var componentId = component.id;
	var resourceId = getId(node.closest('.resource')[0]);

	var componentElement = node.closest('.component');
	var parentElement = componentElement.parent();
	var pos = parentElement.children('.node').size();

	var rel = {};

	//rel[componentId] = pos;
	rel[resourceId] = pos;

	var parentId = getId(parentElement);

	Command.createAndAdd(parentId, form, rel);

    }

};