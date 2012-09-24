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

var elements;

$(document).ready(function() {
    Structr.registerModule('elements', _Elements);
    Structr.classes.push('element');
});

var _Elements = {

    icon : 'icon/brick.png',
    add_icon : 'icon/brick_add.png',
    delete_icon : 'icon/brick_delete.png',

    elementNames : [

    // Active components
    'Component', 'View', 'RemoteView',

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
        'name' : 'Components',
        'elements' : ['Component', 'View', 'RemoteView']
    },
    {
        'name' : 'Root',
        'elements' : ['html', 'content']
    },
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
    //Structr.classes.push('element');
    },

    onload : function() {
        if (debug) console.log('onload');
        elements = $('#elements', main);
        _Elements.refresh();
    //        _Elements.showPalette();
    },

    showPalette : function() {
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
                    zIndex: 2,
                    helper: 'clone'
                });
            });

        });
    },
    
    refresh : function() {
        elements.empty();

        if (Command.list('Element')) {
            elements.append('<button class="add_element_icon button"><img title="Add Element" alt="Add Element" src="' + _Elements.add_icon + '"> Add Element</button>');

            $('.add_element_icon', main).on('click', function(e) {
                e.stopPropagation();
                var button = $(this);

                buttonClicked = button;
                if (isDisabled(button)) return;

                button.append('<div id="elementNames"></div>');
                var list = $('#elementNames', button);
                $(_Elements.elementNames).each(function(i,v) {
                    //console.log('Element: ', v);
                    list.append('<div id="add_' + v + '">' + v + '</div>');
                    $('#add_' + v).on('click', function(e) {
                        e.stopPropagation();
                        var entity = {};
                        entity.type = v.capitalize();
                        entity.tag = v;
                        Command.create(entity);
                        list.remove();
                    });
                });
                _Entities.addElement(this);
            });
        }
    },

    appendElementElement : function(entity, parentId, componentId, pageId, add, hasChildren, treeAddress) {
        if (debug) console.log('_Elements.appendElementElement', entity, parentId, componentId, pageId, add, hasChildren, treeAddress);
        if (debug) console.log(entity.id);
        var parent;
        
        if (treeAddress) {
            parent = $('#_' + treeAddress);
        } else {
            parent = Structr.findParent(parentId, componentId, pageId, elements);
        }
        
        if (debug) console.log('appendElementElement parent', parent);
        if (!parent) return false;
        
        var parentPath = getElementPath(parent);
        if (debug) console.log(parentPath);
        
        _Entities.ensureExpanded(parent);
        
        var pos = parent.children('.node').length;
        if (debug) console.log(pos);
        
        var id = parentPath + '_' + parent.children('.node').length;
        if (debug) console.log(id);
        
        parent.append('<div id="_' + id + '" class="node element ' + entity.id + '_"></div>');
        
        //var div = Structr.node(entity.id, parentId, componentId, pageId, pos);
        var div = $('#_' + id);
        if (debug) console.log('Element appended (div, parent)', div, parent);
        
        if (!div) return;
        
        entity.pageId = pageId;
        
        var displayName = entity.tag ? entity.tag : '[' + entity.type + ']';

        div.append('<img class="typeIcon" src="'+ _Elements.icon + '">'
            + '<b class="tag_ name_">' + displayName + '</b> <span class="id">' + entity.id + '</span>'
            + (entity._html_id ? '<span class="_html_id_">#' + entity._html_id + '</span>' : '')
            + (entity._html_class ? '<span class="_html_class_">.' + entity._html_class : '</span>')
            + '</div>');

        _Entities.appendExpandIcon(div, entity, hasChildren);

        $('.typeIcon', div).on('mousedown', function(e) {
            e.stopPropagation();
        });

        div.append('<img title="Delete ' + entity.tag + ' element ' + entity.id + '" alt="Delete ' + entity.tag + ' element ' + entity.id + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            _Entities.deleteElement(this, entity);
        });


        div.append('<img title="Wrap in Component" alt="Wrap in Component" class="add_icon button" src="' + _Components.add_icon + '">');
        $('.add_icon', div).on('click', function(e) {
            e.stopPropagation();
            var self = $(this);

            var node = $(self.closest('.node')[0]);

            var pos = node.parent().children('.node').size() - 1;

            var nodeData = {};
            nodeData.type = 'Component';
            nodeData.parentId = getId(node.parent());

            var relData = {};

//            var component = node.closest('.component')[0];
//            if (component) {
//                var componentId = getId(component);
//                relData[componentId] = pos;
//                relData.componentId = componentId;
//            }

            var page = node.closest('.page')[0];
            if (page) {
                var pageId = getId(page);
                relData[pageId] = pos;
                relData.pageId = pageId;
            }
            if (debug) console.log('Wrap element in component', getId(node), nodeData, relData);
            //_Entities.createAndAdd(getId(node), nodeData, relData);
            
            var dialog = $('#dialogBox .dialogText');
            var dialogMsg = $('#dialogMsg');
			
            dialog.empty();
            dialogMsg.empty();
            
            dialog.append('<label for="kind">Component Class:</label></td><td><input id="_kind" name="kind" size="20" value="">');
            dialog.append('<button id="startWrap">Create Component</button>');

            Structr.dialog('Create Component', function() {
                return true;
            }, function() {
                return true;
            });
			
            $('#startWrap').on('click', function(e) {
                e.stopPropagation();

                var kind = $('#_kind', dialog).val();
                
                nodeData.kind = kind;
                
                dialog.empty();
                dialogMsg.empty();
                
                $.unblockUI({
                    fadeOut: 25
                });

                if (debug) console.log('start');
                return Command.wrap(getId(node), nodeData, relData);
            });

            

        });

        _Entities.setMouseOver(div);
        _Entities.appendEditPropertiesIcon(div, entity);
        _Entities.appendAccessControlIcon(div, entity);
        
        if (entity.tag == 'a' || entity.tag == 'link') {
            div.append('<img title="Edit Link" alt="Edit Link" class="link_icon button" src="' + Structr.link_icon + '">');
            $('.link_icon', div).on('click', function(e) {
                e.stopPropagation();
                var dialog = $('#dialogBox .dialogText');
                var dialogMsg = $('#dialogMsg');
			
                dialog.empty();
                dialogMsg.empty();
                
                dialog.append('<p>Click on a page to establish a hyperlink between this element and the target resource.</p>');
                
                var headers = {};
                headers['X-StructrSessionToken'] = token;
                
                $.ajax({
                    url: rootUrl + 'pages?pageSize=100',
                    async: true,
                    dataType: 'json',
                    contentType: 'application/json; charset=utf-8',
                    headers: headers,
                    success: function(data) {
                        
                        $(data.result).each(function(i, res) {
                            
                            //console.log(entity.id, res.linkingElements);
                            
                            dialog.append('<div class="page ' + res.id + '_"><img class="typeIcon" src="icon/page.png">'
                                + '<b class="name_">' + res.name + '</b></div>');
                            
                            var div = $('.' + res.id + '_', dialog);
                            
                            div.on('click', function(e) {
                                e.stopPropagation();
                                Command.link(entity.id, res.id); 
                                $('#dialogBox .dialogText').empty();
                                _Pages.reloadPreviews();
                                $.unblockUI({
                                    fadeOut: 25
                                });                               
                            })
                            .css({
                                cursor: 'pointer'
                            })                            
                            .hover(function() {
                                $(this).addClass('nodeHover');
                            }, function() {
                                $(this).removeClass('nodeHover');
                            });
                            
                            if (isIn(entity.id, res.linkingElements)) {
                                div.addClass('nodeActive');
                            }
                            
                        });

                    }
                });
                
                $.ajax({
                    url: rootUrl + 'files/ui?pageSize=100',
                    async: true,
                    dataType: 'json',
                    contentType: 'application/json; charset=utf-8',
                    headers: headers,
                    success: function(data) {
                        $(data.result).each(function(i, res) {
                            
                            console.log(entity.id, res.name, res);
                            
                            dialog.append('<div class="file ' + res.id + '_"><img class="typeIcon" src="' + _Files.getIcon(res) + '">'
                                + '<b class="name_">' + res.name + '</b></div>');
                            
                            var div = $('.' + res.id + '_', dialog);
                            
                            div.on('click', function(e) {
                                e.stopPropagation();
                                Command.link(entity.id, res.id); 
                                $('#dialogBox .dialogText').empty();
                                _Pages.reloadPreviews();
                                $.unblockUI({
                                    fadeOut: 25
                                });                               
                            })
                            .css({
                                cursor: 'pointer'
                            })                            
                            .hover(function() {
                                $(this).addClass('nodeHover');
                            }, function() {
                                $(this).removeClass('nodeHover');
                            });
                            
                            if (isIn(entity.id, res.linkingElements)) {
                                div.addClass('nodeActive');
                            }
                            
                        });

                    }
                });
                
                Structr.dialog('Link to Resource (Page, File or Image)', function() {
                    return true;
                }, function() {
                    return true;
                });
                
            });
        }

        return div;
    }
};