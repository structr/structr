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
var palette;

$(document).ready(function() {
    Structr.registerModule('elements', _Elements);
});

var _Elements = {

    icon : 'icon/brick.png',
    add_icon : 'icon/brick_add.png',
    delete_icon : 'icon/brick_delete.png',

    elementNames : [

    // The root element
    'html',

    // Document metadata
    'head', 'title', 'base', 'link', 'meta', 'style',

    // Scripting
    'script', 'noscript',

    // Sections
    'body', 'section', 'nav', 'article', 'aside', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hgroup', 'header', 'footer', 'address',

    // Grouping content
    'p', 'hr', 'pre', 'blockquote', 'ol', 'ul', 'li', 'dl', 'dt', 'dd', 'figure', 'figcaption', 'div',

    // Text-level semantics
    'a', 'em', 'strong', 'small', 's', 'cite', 'g', 'dfn', 'abbr', 'time', 'code', 'var', 'samp', 'kbd', 'sub', 'sup',
    'i', 'b', 'u', 'mark', 'ruby', 'rt', 'rp', 'bdi', 'bdo', 'span', 'br', 'wbr',

    // Edits
    'ins', 'del',

    // Embedded content
    'img', 'iframe', 'embed', 'object', 'param', 'video', 'audio', 'source', 'track', 'canvas', 'map', 'area',

    // Tabular data
    'table', 'caption', 'colgroup', 'col', 'tbody', 'thead', 'tfoot', 'tr', 'td', 'th',

    // Forms
    'form', 'fieldset', 'legend', 'label', 'input', 'button', 'select', 'datalist', 'optgroup', 'option', 'textarea', 'keygen', 'output',
    'progress', 'meter',

    // Interactive elements
    'details', 'summary', 'command', 'menu'
    ],

    elementGroups : [
    {
        'name' : 'Metadata',
        'elements' : ['head', 'title', 'base', 'link', 'meta', 'style']
    },
    {
        'name' : 'Sections',
        'elements' : ['body', 'section', 'nav', 'article', 'aside', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hgroup', 'header', 'footer', 'address']
    },
    {
        'name' : 'Grouping',
        'elements' : ['div', 'p', 'hr', 'ol', 'ul', 'li', 'dl', 'dt', 'dd', 'pre', 'blockquote', 'figure', 'figcaption' ]
    },
    {
        'name' : 'Scripting',
        'elements' : ['script', 'noscript']
    },
    {
        'name' : 'Tabular',
        'elements' : ['table', 'tr', 'td', 'th', 'caption', 'colgroup', 'col', 'tbody', 'thead', 'tfoot']
    },
    {
        'name' : 'Text',
        'elements' : ['a', 'em', 'strong', 'small', 's', 'cite', 'g', 'dfn', 'abbr', 'time', 'code', 'var', 'samp', 'kbd', 'sub', 'sup', 'i', 'b', 'u', 'mark', 'ruby', 'rt', 'rp', 'bdi', 'bdo', 'span', 'br', 'wbr']
    },
    {
        'name' : 'Edits',
        'elements' : ['ins', 'del']
    },
    {
        'name' : 'Embedded',
        'elements' : ['img', 'video', 'audio', 'source', 'track', 'canvas', 'map', 'area', 'iframe', 'embed', 'object', 'param']
    },
    {
        'name' : 'Forms',
        'elements' : ['form', 'input', 'button', 'select', 'datalist', 'optgroup', 'option', 'textarea', 'fieldset', 'legend', 'label', 'keygen', 'output', 'progress', 'meter']
    },
    {
        'name' : 'Interactive',
        'elements' : ['details', 'summary', 'command', 'menu']
    }
    ],
	
    init : function() {
        Structr.classes.push('element');
    },

    onload : function() {
        if (debug) console.log('onload');
        main.append('<div id="elements"></div>');
        if (palette) palette.remove();
        main.before('<div id="palette"></div>');
        elements = $('#elements', main);
        palette = $('#palette');
        //_Elements.refresh();
        _Elements.showPalette();
    },

    showPalette : function() {
//        var palette = $('#palette');
        palette.empty();
        $(_Elements.elementGroups).each(function(i,group) {
            if (debug) console.log(group);
            palette.append('<div class="elementGroup" id="group_' + group.name + '"><h3>' + group.name + '</h3></div>');
            $(group.elements).each(function(j,elem) {
                var div = $('#group_' + group.name);
                div.append('<div class="draggable element" id="add_' + elem + '">' + elem + '</div>');
                $('#add_' + elem, div).draggable({
                    iframeFix: true,
                    revert: 'invalid',
                    containment: 'body',
                    zIndex: 1,
                    helper: 'clone'
                });
            });

        });

    },
    
    refresh : function() {
        elements.empty();
        if (_Elements.show()) {
            elements.append('<button class="add_element_icon button"><img title="Add Element" alt="Add Element" src="' + _Elements.add_icon + '"> Add Element</button>');

            $('.add_element_icon', main).on('click', function() {
                var button = $(this);

                buttonClicked = button;
                if (isDisabled(button)) return;

                button.append('<div id="elementNames"></div>');
                var list = $('#elementNames', button);
                $(_Elements.elementNames).each(function(i,v) {
                    //console.log('Element: ', v);
                    list.append('<div id="add_' + v + '">' + v + '</div>');
                    $('#add_' + v).on('click', function() {
                        _Elements.addElement(button, v.capitalize(), '"tag":"' + v + '"');
                        list.remove();
                    });
                });
            //_Elements.addElement(this);
            });
        }
    },

    show : function() {
        return _Entities.showEntities('Element');
    },

    appendElementElement : function(element, parentId, resourceId) {
        if (debug) console.log('Elements.appendElementElement: parentId: ' + parentId + ', resourceId: ' + resourceId);

        var parent = Structr.findParent(parentId, resourceId, elements);
        
        parent.append('<div class="node element ' + element.id + '_">'
            + '<img class="typeIcon" src="'+ _Elements.icon + '">'
            + '<b class="tag_">' + element.tag + '</b> <span class="id">' + element.id + '</span>'
            + (element._html_id ? 'id=' + element._html_id : '')
            + (element._html_class ? 'class=' + element._html_class : '')
            + '</div>');
        var div = $('.' + element.id + '_', parent);
        div.append('<img title="Delete ' + element.tag + ' element ' + element.id + '" alt="Delete ' + element.tag + ' element ' + element.id + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function() {
            _Elements.deleteElement(this, element);
        });
        //        div.append('<img class="add_icon button" title="Add Element" alt="Add Element" src="icon/add.png">');
        //        $('.add_icon', div).on('click', function() {
        //            Resources.addElement(this, resource);
        //        });
        $('b', div).on('click', function(e) {
            e.stopPropagation();
            div.off('click');
            div.off('mouseover');
            div.off('mouseout');
            $(this).off('click');
            $(this).off('mouseover');
            $(this).off('mouseout');

            if (debug) console.log('parent', parent);
            _Entities.showProperties(this, element, '_html_', $('.' + element.id + '_', parent));
        });
		
        div.on('mouseover', function(e) {
            e.stopPropagation();
            _Entities.showNonEmptyProperties(this, element, '_html_', $('.' + element.id + '_', parent));
        });		

        div.on('mouseout', function(e) {
            e.stopPropagation();
            _Entities.hideNonEmptyProperties(this, element, '_html_', $('.' + element.id + '_', parent));
        });		

        return div;
    },

    addElement : function(button, type, props) {
        return _Entities.add(button, type, props);
    },

    deleteElement : function(button, element) {
        if (debug) console.log('delete element ' + element);
        deleteNode(button, element);
    }

};