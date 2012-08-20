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
	//Structr.activateMenuEntry('pages');
	if (debug) console.log('onload');
	if (palette) palette.remove();

	main.append('<table id="pagesEditor"><tr><td id="components"></td><td id="elements"></td></tr></table>');

	components = $('#components');
	elements = $('#elements');

	_Components.refresh();
	_Components.refreshElements();
    },
    
    refresh : function() {
	components.empty();
	if (Command.list('Component')) {
	    components.append('<button class="add_component_icon button"><img title="Add Component" alt="Add Component" src="' + _Components.add_icon + '"> Add Component</button>');
	    $('.add_component_icon', main).on('click', function(e) {
                e.stopPropagation();
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
	    $('.add_element_icon', main).on('click', function(e) {
                e.stopPropagation();
		var entity = {};
		entity.type = 'Element';
		Command.create(entity);
	    });
	}
    },
    
    appendComponentElement : function(component, parentId, componentId, pageId, removeExisting, hasChildren, treeAddress) {
        if (debug) console.log('_Components.appendComponentElement', component, parentId, componentId, pageId, removeExisting, hasChildren, treeAddress);

	var parent;
        
        if (treeAddress) {
            if (debug) console.log('tree address', treeAddress);
            parent = $('#_' + treeAddress);
        } else {
            parent = Structr.findParent(parentId, componentId, pageId, components);
        }
        
        if (!parent) return false;
        
        var parentPath = getElementPath(parent);
        
        var id = parentPath + '_' + parent.children('.node').length;
        
        var name = component.name;
        var kind = component.kind;

	parent.append('<div id="_' + id + '" class="node component ' + component.id + '_">'
	    + '<img class="typeIcon" src="'+ _Components.icon + '">'
	    + '<b class="name_">' + (name ? name : '') + '</b> [<b class="kind_">' + (kind ? kind : '') + '</b>] <span class="id">' + component.id + '</span>'
	    + '</div>');
	
        var div = $('#_' + id);
        
        if (!div) return;
        
	div.append('<img title="Delete component \'' + name + '\' ' + component.id + '" alt="Delete component \'' + name + '\' ' + component.id + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
	$('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
	    _Components.deleteComponent(this, component);
	});

//	div.append('<img title="Create Form" alt="Create Form" class="add_form_icon button" src="icon/application_form_add.png">');
//	$('.add_form_icon', div).on('click', function(e) {
//            e.stopPropagation();
//	    _Components.createForm(this, component);
//	});

	component.pageId = pageId;
        
        _Entities.appendExpandIcon(div, component, hasChildren);
	_Entities.setMouseOver(div);
	_Entities.appendEditPropertiesIcon(div, component);
        _Entities.appendAccessControlIcon(div, component);

	div.droppable({
	    accept: '.element',
	    greedy: true,
	    hoverClass: 'nodeHover',
            tolerance: 'pointer',
	    drop: function(event, ui) {
		var self = $(this);
		var elementId = getId(ui.draggable);
		var pageId = getId(self);
		if (!pageId) pageId = '*';
		var pos = $('.element', self).length;
		var nodeData = {};
		nodeData.id = elementId;
		var relData = {};
		relData[pageId] = pos;
		Command.createAndAdd(pageId, nodeData, relData);
	    }
	});

	return div;
    },

    appendElementElement : function(element, parentId, pageId) {
	if (debug) console.log('Components.appendElementElement');
	var div = _Elements.appendElementElement(element, parentId, pageId);
	//console.log(div);
	if (parentId) {

	    $('.delete_icon', div).remove();
	    div.append('<img title="Remove element \'' + element.name + '\' from page ' + parentId + '" '
		+ 'alt="Remove element ' + element.name + ' from ' + parentId + '" class="delete_icon button" src="icon/brick_delete.png">');
	    $('.delete_icon', div).on('click', function(e) {
                e.stopPropagation();
		Command.removeSourceFromTarget(element.id, parentId);
	    });
	}

	div.droppable({
	    accept: '.element',
	    greedy: true,
	    hoverClass: 'nodeHover',
            tolerance: 'pointer',
	    drop: function(event, ui) {
		var self = $(this);
		var page = self.closest( '.page')[0];
		if (debug) console.log(page);
		var contentId = getId(ui.draggable);
		var elementId = getId(self);
		var pos = $('.content', self).length;
		if (debug) console.log(pos);
		var pageId;
		if (page) {
		    pageId = getId(page);
		} else {
		    pageId = '*';
		}

		var relData = {};
		relData[pageId] = pos;
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
	form._html_action = '//' + plural(component.kind).toLowerCase();
	form._html_method = 'post';

	var node = $($(button).closest('.node')[0]);

	//var componentId = component.id;
	var pageId = getId(node.closest('.page')[0]);

	var componentElement = node.closest('.component');
	var parentElement = componentElement.parent();
	var pos = parentElement.children('.node').size();

	var relData = {};

	//rel[componentId] = pos;
	relData[pageId] = pos;

	var parentId = getId(parentElement);

	Command.createAndAdd(parentId, form, relData);

    }

};