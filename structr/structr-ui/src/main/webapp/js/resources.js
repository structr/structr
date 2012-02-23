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

var resources;
var previews;
var selStart, selEnd;
var sel;
var contentSourceId, elementSourceId, rootId;

$(document).ready(function() {
    Structr.registerModule('resources', _Resources);
});

var _Resources = {

    icon : 'icon/page.png',
    add_icon : 'icon/page_add.png',
    delete_icon : 'icon/page_delete.png',

    init : function() {
        Structr.classes.push('resource');
    },

    onload : function() {
        //Structr.activateMenuEntry('resources');
        if (debug) console.log('onload');
        main.append('<table id="resourcesEditor"><tr><td id="previews"></td><td id="resources"></td><td id="components"></td><td id="elements"></td><td id="contents"></td></tr></table>');

        resources = $('#resources');
        components = $('#components');
        elements = $('#elements');
        contents = $('#contents');
        previews = $('#previews');
        if (palette) palette.remove();
        main.before('<div id="palette"></div>');
        palette = $('#palette');
        elements = $('#elements', main);
        main.before('<div id="hoverStatus">Hover status</div>');
//        main.before('<div id="offset">Offset</div>');

        _Resources.refresh();
        _Resources.refreshComponents();
        _Resources.refreshElements();
        _Elements.showPalette();
        _Contents.refresh();

    //        main.height($(window).height()-header.height()-palette.height()-24);

    },

    refresh : function() {
        resources.empty();
        if (_Resources.show()) {
            resources.append('<button class="add_resource_icon button"><img title="Add Resource" alt="Add Resource" src="' + _Resources.add_icon + '"> Add Resource</button>');
            $('.add_resource_icon', main).on('click', function() {
                _Resources.addResource(this);
            });
        }
    },

    refreshComponents : function() {
        components.empty();
        if (_Components.show()) {
            components.append('<button class="add_component_icon button"><img title="Add Component" alt="Add Component" src="' + _Components.add_icon + '"> Add Component</button>');
            $('.add_component_icon', main).on('click', function() {
                _Resources.addComponent(this);
            });
        }
    },

    refreshElements : function() {
        _Elements.refresh();
    },

    show : function() {
        return _Entities.showEntities('Resource');

    //        $.ajax({
    //            url: rootUrl + 'resources',
    //            dataType: 'json',
    //            contentType: 'application/json; charset=utf-8',
    //            //headers: { 'X-User' : 457 },
    //            success: function(data) {
    //                if (!data || data.length == 0 || !data.result) return;
    //                if ($.isArray(data.result)) {
    //
    //                    for (var i=0; i<data.result.length; i++) {
    //                        var resource = data.result[i];
    //
    //                        Resources.appendResourceElement(resource);
    //
    //                        var resourceId = resource.id;
    //                        //          $('#resources').append('<div class="editor_box"><div class="resource" id="resource_' + id + '">'
    //                        //                              + '<b>' + resource.name + '</b>'
    //                        //                              //+ ' [' + id + ']'
    //                        //                              + '<img class="add_icon button" title="Add Element" alt="Add Element" src="icon/add.png" onclick="addElement(' + id + ', \'#resource_' + id + '\')">'
    //                        //                              + '<img class="delete_icon button" title="Delete '
    //                        //                              + resource.name + '" alt="Delete '
    //                        //                              + resource.name + '" src="icon/delete.png" onclick="deleteNode(' + id + ', \'#resource_' + id + '\')">'
    //                        //                              + '</div></div>');
    //                        Resources.showSubEntities(resourceId, null);
    //
    //                        $('#previews').append('<a target="_blank" href="' + viewRootUrl + resource.name + '">' + viewRootUrl + resource.name + '</a><br><div class="preview_box"><iframe id="preview_'
    //                            + resourceId + '" src="' + viewRootUrl + resource.name + '?edit"></iframe></div><div style="clear: both"></div>');
    //
    //                        $('#preview_' + resourceId).load(function() {
    //                            //console.log(this);
    //                            var doc = $(this).contents();
    //                            var head = $(doc).find('head');
    //                            head.append('<style type="text/css">'
    //                                + '.structr-editable-area {'
    //                                + 'border: 1px dotted #a5a5a5;'
    //                                + 'margin: 2px;'
    //                                + 'padding: 2px;'
    //                                + '}'
    //                                + '.structr-editable-area-active {'
    //                                + 'border: 1px dotted #orange;'
    //                                + 'margin: 2px;'
    //                                + 'padding: 2px;'
    //                                + '}'
    //                                + '</style>');
    //
    //
    //                            $(this).contents().find('.structr-editable-area').each(function(i,element) {
    //                                //console.log(element);
    //                                $(element).addClass('structr-editable-area');
    //                                $(element).on({
    //                                    mouseenter: function() {
    //                                        var self = $(this);
    //                                        self.attr('contenteditable', true);
    //                                        self.addClass('structr-editable-area-active');
    //                                    },
    //                                    mouseleave: function() {
    //                                        var self = $(this);
    //                                        self.attr('contenteditable', true);
    //                                        self.removeClass('structr-editable-area-active');
    //                                        var id = self.attr('id');
    //                                        id = lastPart(id, '-');
    //                                        Resources.updateContent(id, this.innerHTML);
    //                                    }
    //                                });
    //                            });
    //                        });
    //
    //                    }
    //                }
    //            }
    //        });
    //
    //        return true;
    },

    showElements : function() {
        return _Resources.showEntities('Element');
    },
	
    appendResourceElement : function(resource, resourceId) {
        resources.append('<div class="node resource ' + resource.id + '_"></div>');
        var div = $('.' + resource.id + '_', resources);
		
        div.append('<img title="Expand resource \'' + resource.name + '\'" alt="Expand resource \'' + resource.name + '\'" class="expand_icon button" src="' + Structr.expand_icon + '">');

        $('.expand_icon', div).on('click', function() {
            _Resources.toggleResource(this, resource);
        });


        div.append('<img class="typeIcon" src="icon/page.png">'
            + '<b class="name_">' + resource.name + '</b> <span class="id">' + resource.id + '</span>');
		
        div.append('<img title="Delete resource \'' + resource.name + '\'" alt="Delete resource \'' + resource.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');

        div.append('<img title="Link resource \'' + resource.name + '\' to current selection" alt="Link resource \'' + resource.name + '\' to current selection" class="link_icon button" src="' + Structr.link_icon + '">');
        $('.link_icon', div).on('click', function() {
            console.log(rootId, sourceId);
            if (sourceId && selStart && selEnd) {
                // function(resourceId, sourceId, linkedResourceId, startOffset, endOffset)
                _Resources.linkSelectionToResource(rootId, sourceId, resource.id, selStart, selEnd);
            //$('.link_icon').hide();
            }
        });
		
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            var self = $(this);
            self.off('click');
            self.off('mouseover');
            _Resources.deleteResource(this, resource);
        });
        //        div.append('<img class="add_icon button" title="Add Element" alt="Add Element" src="icon/add.png">');
        //        $('.add_icon', div).on('click', function() {
        //            Resources.addElement(this, resource);
        //        });
        $('b', div).on('click', function() {
            e.stopPropagation();
            var self = $(this);
            self.off('click');
            self.off('mouseover');
            _Entities.showProperties(this, resource, 'all', $('.' + resource.id + '_', resources));
        });
		
        previews.append('<a target="_blank" href="' + viewRootUrl + resource.name + '">'
            + '<button class="view_resource_icon button"><img title="View ' + resource.name + ' in new window" alt="View ' + resource.name + ' in new window" src="icon/eye.png"> ' + resource.name + '</button>'
            + '</a><br><div class="preview_box"><iframe id="preview_'
            + resource.id + '" src="' + viewRootUrl + resource.name + '?edit"></iframe></div><div style="clear: both"></div>');
		
        $('#preview_' + resource.id).load(function() {

            //console.log('Preview ' + resource.id + ' offset: ', $(this).offset());
            var offset = $(this).offset();

            //console.log(this);
            var doc = $(this).contents();
            var head = $(doc).find('head');
            if (head) head.append('<style type="text/css">'
                + '* { z-index: 0}\n'
                + '.structr-content-container { display: inline; border: none; margin: 0; padding: 0; }\n'
                + '.structr-element-container:hover { border: 1px dotted red; }\n'
                + '.structr-droppable-area { border: 1px dotted red; }\n'
                + '.structr-editable-area { border: 1px dotted orange; margin: -1px; padding: 0; }\n'
                + '.structr-editable-area-active { background-color: #ffe; border: 1px solid orange; color: #333; margin: -1px; padding: 0; }'
                + '.link-hover { border: 1px solid #00c }'
                + '</style>');
	
            //var iframe = $(this).contents()[0];
            var iframeWindow = this.contentWindow;

            var droppables = $(this).contents().find('[structr_element_id]');
            //console.log(droppables);

            droppables.each(function(i,element) {
                //console.log(element);
                var el = $(element);

                //var depth = el.parents().length;
                //console.log('depth: ' + depth);
                //console.log('z-index before: ' + el.css('z-index'));
                //el.css('z-index', depth);
                //console.log('z-index after: ' + el.css('z-index'));
                
                el.droppable({
                    accept: '.element, .content',
                    greedy: true,
                    hoverClass: 'structr-droppable-area',
                    iframeOffset: {
                        'top' : offset.top,
                        'left' : offset.left
                        },
                    drop: function(event, ui) {
                        var resource = $(this).closest( '.resource')[0];
                        var resourceId;
                        var pos;

                        if (resource) {

                            // we're in the main page
                            resourceId = getIdFromClassString($(resource).attr('class'));
                            pos = $('.content, .element', $(this)).length;

                        } else {
                            
                            // we're in the iframe
                            resource = $(this).closest('[structr_resource_id]')[0];
                            resourceId = $(resource).attr('structr_resource_id');
                            pos = $('[structr_element_id]', $(this)).length;
                        }
                        
                        console.log('Dropped on: ' , $(this));
                        //console.log('ui: ' , ui);
                        //console.log('Resource: ' , resource);
                        //console.log('ResourceId: ' + resourceId);
                        var contentId = getIdFromClassString(ui.draggable.attr('class'));
                        var elementId = getIdFromClassString($(this).attr('class'));

                        if (!elementId) elementId = $(this).attr('structr_element_id');

                        if (!contentId) {
                            // create element on the fly
                            //var el = _Elements.addElement(null, 'element', null);
                            var tag = $(ui.draggable).text();
                            //var el = _Elements.addElement(null, 'Element', '"tag":"' + tag + '"');
                            //if (debug) console.log(el);
                            //contentId = el.id;
                            //console.log('Created new element on the fly: ' + contentId);
                        }
                        

                        
                        if (resourceId) {
                            props = '"resourceId" : "' + resourceId + '", "' + resourceId + '" : "' + pos + '"';
                        } else {
                            props = '"*" : "' + pos + '"';
                        }

                        var props;

                        if (!contentId) {
                            props += ', "name" : "New ' + tag + ' ' + Math.floor(Math.random() * (999999 - 1)) + '", "type" : "' + tag.capitalize() + '", "tag" : "' + tag + '"';
                        }
                    console.log('Content Id: ' + contentId);
                    console.log('Target Id: ' + elementId);
                    console.log('Position: ' + pos);
                    console.log(props);
                    _Entities.addSourceToTarget(contentId, elementId, props);
                    }
                });

                var structrId = el.attr('structr_element_id');
                if (structrId) {
                    el.on({
                        mouseover: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            //self.addClass('structr-editable-area');
                            //self.attr('contenteditable', true);
                            //if (debug) console.log(self);
                            $('#hoverStatus').text(element.nodeName + ': ' + self.attr('structr_element_id'));
                        },
                        mouseout: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            //self.removeClass('structr-editable-area');
                            //self.attr('contenteditable', false);
                            $('#hoverStatus').text('-- non-editable --');
                        }
                    //                        ,
                    //                        click: function() {
                    //                            var self = $(this);
                    //                            //self.addClass('structr-editable-area-active');
                    //                            sel = iframeWindow.getSelection();
                    //                            if (sel.rangeCount) {
                    //                                selStart = sel.getRangeAt(0).startOffset;
                    //                                selEnd = sel.getRangeAt(0).endOffset;
                    //                                console.log(selStart, selEnd);
                    //                                $('.link_icon').show();
                    //                                //                                sourceId = structrId;
                    //                                elementSourceId = self.attr('structr_element_id');
                    //                                if (debug) console.log('sourceId: ' + elementSourceId);
                    //                                var rootResourceElement = self.closest('html')[0];
                    //                                console.log(rootResourceElement);
                    //                                if (rootResourceElement) {
                    //                                    rootId = $(rootResourceElement).attr('structr_element_id');
                    //                                }
                    //
                    //                            }
                    //                        }
                    });

                }
            });

            $(this).contents().find('[structr_content_id]').each(function(i,element) {
                if (debug) console.log(element);
                var el = $(element);
                var structrId = el.attr('structr_content_id');
                if (structrId) {
                    
                    el.on({
                        mouseover: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            self.addClass('structr-editable-area');
                            self.attr('contenteditable', true);
                            //swapFgBg(self);
                            //self.addClass('structr-editable-area');
                            //self.attr('contenteditable', true);
                            //if (debug) console.log(self);
                            $('#hoverStatus').text('Editable content element: ' + self.attr('structr_content_id'));
                        },
                        mouseout: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            //swapFgBg(self);
                            self.removeClass('structr-editable-area');
                            //self.attr('contenteditable', false);
                            $('#hoverStatus').text('-- non-editable --');
                        },
                        click: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            self.removeClass('structr-editable-area');
                            self.addClass('structr-editable-area-active');

                            //swapFgBg(self);
                            sel = iframeWindow.getSelection();
                            if (sel.rangeCount) {
                                selStart = sel.getRangeAt(0).startOffset;
                                selEnd = sel.getRangeAt(0).endOffset;
                                if (debug) console.log(selStart, selEnd);
                                $('.link_icon').show();
                                //                                sourceId = structrId;
                                contentSourceId = self.attr('structr_content_id');
                                if (debug) console.log('click contentSourceId: ' + contentSourceId);
                                var rootResourceElement = self.closest('html')[0];
                                if (debug) console.log(rootResourceElement);
                                if (rootResourceElement) {
                                    rootId = $(rootResourceElement).attr('structr_content_id');
                                }
								
                            }
                        },
                        blur: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            if (debug) console.log('blur contentSourceId: ' + contentSourceId);
                            _Resources.updateContent(contentSourceId, self.text());
                            contentSourceId = null;
                            self.attr('contenteditable', false);
                            self.removeClass('structr-editable-area-active');
                        //sswapFgBg(self);
                        //$('.link_icon').hide();
                        //Resources.reloadPreviews();
                        }
                    });
                //                    el.droppable({
                //                        //accept: '.resource',
                //                        hoverClass: 'link-hover',
                ////						over: function(event, ui) {
                ////                            var resourceId = getIdFromClassString(ui.draggable.attr('class'));
                ////                            var sourceId = $(this).attr('structr_id');
                ////                            console.log('Resource ' + resourceId + ' dropped onto ', this);
                ////                            //var sel = iframeWindow.getSelection();
                ////                            if (sel && sel.rangeCount) {
                ////                                selStart = sel.getRangeAt(0).startOffset;
                ////                                selEnd = sel.getRangeAt(0).endOffset;
                ////                                console.log(selStart, selEnd);
                ////                            }
                ////						},
                //                        drop: function(event, ui) {
                //                            console.log(event,ui);
                //                            var resourceId = getIdFromClassString(ui.draggable.attr('class'));
                //                            var sourceId = $(this).attr('structr_id');
                //                            console.log('Resource ' + resourceId + ' dropped onto ', this);
                //                            //var sel = iframeWindow.getSelection();
                //                            if (sel && sel.rangeCount) {
                //                                selStart = sel.getRangeAt(0).startOffset;
                //                                selEnd = sel.getRangeAt(0).endOffset;
                //                                console.log(selStart, selEnd);
                //
                //                                Resources.linkSelectionToResource(sourceId, resourceId, selStart, selEnd);
                //
                //                            }
                //                        }
                //                    });
                }
            });
        });
        //        var elements = resource.children;
        //
        //        if (elements && elements.length > 0) {
        //            disable($('.delete_icon', div));
        //            $(elements).each(function(i, child) {
        //
        //                if (debug) console.log("type: " + child.type);
        //                if (child.type == "Element") {
        //                    Resources.appendElementElement(child, resource.id);
        //                } else if (child.type == "Content") {
        //                    Resources.appendContentElement(child, resource.id);
        //                }
        //            });
        //        }

        //        div.draggable({
        //            iframeFix: true,
        //            refreshPositions: true,
        //            revert: 'invalid',
        //            //containment: '#main',
        //            zIndex: 1,
        //			start: function() {
        //				//$(this).data('draggable').offset.click.top += 1000;
        //			}
        //            //helper: 'clone'
        //        //            ,
        //        //            stop : function(event, ui) {
        //        //                console.log(ui);
        //        //            }
        //        });
        
        //        div.droppable({
        //            accept: '.component, .element',
        //            greedy: true,
        //            hoverClass: 'resourceHover',
        //            drop: function(event, ui) {
        //                var componentId = getIdFromClassString(ui.draggable.attr('class'));
        //                var resourceId = getIdFromClassString($(this).attr('class'));
        //
        //                if (!resourceId) resourceId = '*';
        //
        //                var pos = $('.component, .element', $(this)).length;
        //                if (debug) console.log(pos);
        //                var props = '"' + resourceId + '" : "' + pos + '"';
        //                if (debug) console.log(props);
        //                _Entities.addSourceToTarget(componentId, resourceId, props);
        //            }
        //        });

        return div;
    },

    appendElementElement : function(element, parentId, resourceId) {
        if (debug) console.log('Resources.appendElementElement');
        var div = _Elements.appendElementElement(element, parentId, resourceId);
        //console.log(div);
        if (parentId) {

            $('.delete_icon', div).remove();
            div.append('<img title="Remove element \'' + element.name + '\' from resource ' + parentId + '" '
                + 'alt="Remove element ' + element.name + ' from ' + parentId + '" class="delete_icon button" src="icon/brick_delete.png">');
            $('.delete_icon', div).on('click', function(e) {
                e.stopPropagation();
                var self = $(this);
                self.off('click');
                self.off('mouseover');
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

        var resource = div.closest( '.resource')[0];
        if (!resource && resources) {

            div.draggable({
                revert: 'invalid',
                containment: '#main',
                zIndex: 1,
                helper: 'clone'
            });

        }

        div.droppable({
            accept: '.element, .content',
            greedy: true,
            hoverClass: 'elementHover',
            drop: function(event, ui) {

                console.log('appendElementElement', $(this));

                var resource = $(this).closest( '.resource')[0];
                if (debug) console.log(resource);
                var contentId = getIdFromClassString(ui.draggable.attr('class'));
                var elementId = getIdFromClassString($(this).attr('class'));

                if (debug) console.log('Content Id: ' + contentId);
                if (!contentId) {
                    // create element on the fly
                    //var el = _Elements.addElement(null, 'element', null);
                    var tag = $(ui.draggable).text();
                //var el = _Elements.addElement(null, 'Element', '"tag":"' + tag + '"');
                //if (debug) console.log(el);
                //contentId = el.id;
                //if (debug) console.log('Created new element on the fly: ' + contentId);
                }

                var pos = $('.content, .element', $(this)).length;
                if (debug) console.log(pos);
                var props;
                if (resource) {
                    var resourceId = getIdFromClassString($(resource).attr('class'));
                    props = '"resourceId" : "' + resourceId + '", "' + resourceId + '" : "' + pos + '"';
                } else {
                    props = '"*" : "' + pos + '"';
                }

                if (!contentId) {
                    props += ', "name" : "New ' + tag + ' ' + Math.floor(Math.random() * (999999 - 1)) + '", "type" : "' + tag.capitalize() + '", "tag" : "' + tag + '"';
                }

                if (debug) console.log(props);
                _Entities.addSourceToTarget(contentId, elementId, props);
            }
        });

        return div;
    },

    appendComponentElement : function(component, parentId, resourceId) {
        if (debug) console.log('Resources.appendComponentElement');
        var div = _Components.appendComponentElement(component, parentId, resourceId);
        //console.log(div);

        if (parentId) {

            $('.delete_icon', div).remove();
            div.append('<img title="Remove component \'' + component.name + '\' from resource ' + parentId + '" '
                + 'alt="Remove component ' + component.name + ' from ' + parentId + '" class="delete_icon button" src="' + _Components.delete_icon + '">');
            $('.delete_icon', div).on('click', function() {
                _Entities.removeSourceFromTarget(component.id, parentId);
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
        
        var resource = div.closest( '.resource')[0];
        if (!resource && resources) {

            div.draggable({
                revert: 'invalid',
                containment: '#main',
                zIndex: 1,
                helper: 'clone'
            });

        }


        div.droppable({
            accept: '.element',
            greedy: true,
            hoverClass: 'componentHover',
            drop: function(event, ui) {
                var resource = $(this).closest( '.resource')[0];
                if (debug) console.log(resource);
                var contentId = getIdFromClassString(ui.draggable.attr('class'));
                var componentId = getIdFromClassString($(this).attr('class'));
                var pos = $('.element', $(this)).length;
                if (debug) console.log(pos);
                var props;
                if (resource) {
                    var resourceId = getIdFromClassString($(resource).attr('class'));
                    props = '"resourceId" : "' + resourceId + '", "' + resourceId + '" : "' + pos + '"';
                } else {
                    props = '"*" : "' + pos + '"';
                }
                if (debug) console.log(props);
                _Entities.addSourceToTarget(contentId, componentId, props);
            }
        });

        return div;
    },

    appendContentElement : function(content, parentId, resourceId) {
        if (debug) console.log('Resources.appendContentElement');
		
        var div = _Contents.appendContentElement(content, parentId, resourceId);

        if (parentId) {
            $('.delete_icon', div).remove();
            div.append('<img title="Remove element \'' + content.name + '\' from resource ' + parentId + '" '
                + 'alt="Remove content ' + content.name + ' from element ' + parentId + '" class="delete_icon button" src="' + _Contents.delete_icon + '">');
            $('.delete_icon', div).on('click', function(e) {
                e.stopPropagation();
                var self = $(this);
                self.off('click');
                self.off('mouseover');
                _Entities.removeSourceFromTarget(content.id, parentId)
            });
        }

        div.draggable({
            iframeFix: true,
            revert: 'invalid',
            containment: '#main',
            zIndex: 1,
            helper: 'clone'
        });
        return div;
    },

    addComponentToResource : function(componentId, resourceId) {
        if (debug) console.log('Resources.appendComponentToResource');

        var resource = $('.' + resourceId + '_');
        var component = $('.' + componentId + '_', components);

        var existing = $('.' + componentId + '_', resource);

        if (existing.length) return;

        if (debug) console.log(resource, component);

        var div = component.clone();
        resource.append(div);

        $('.delete_icon', div).remove();


        div.append('<img title="Remove component ' + componentId + ' from resource ' + resourceId + '" '
            + 'alt="Remove component ' + componentId + ' from resource ' + resourceId + '" class="delete_icon button" src="' + _Components.delete_icon + '">');
        $('.delete_icon', div).on('click', function() {
            _Resources.removeComponentFromResource(componentId, resourceId);
        });
        //element.draggable('destroy');

        var numberOfComponents = $('.component', resource).size();
        if (debug) console.log(numberOfComponents);
        if (numberOfComponents > 0) {
            disable($('.delete_icon', resource)[0]);
        }

    },

    addElementToResource : function(elementId, parentId) {
        if (debug) console.log('Resources.appendElementToResource');

        var parent = $('.' + parentId + '_', resources);
        var element = $('.' + elementId + '_', elements);

        //var existing = $('.' + elementId + '_', resource);

        //if (existing.length) return;

        if (debug) console.log(parent, element);
        
        var div = element.clone();
        parent.append(div);

        $('.delete_icon', div).remove();

        div.append('<img title="Remove element ' + elementId + ' from resource ' + parentId + '" '
            + 'alt="Remove element ' + elementId + ' from resource ' + parentId + '" class="delete_icon button" src="' + _Elements.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            var self = $(this);
            self.off('click');
            self.off('mouseover');
            _Resources.removeElementFromResource(elementId, parentId);
        });
        //element.draggable('destroy');

        div.droppable({
            accept: '.element, .content',
            greedy: true,
            hoverClass: 'elementHover',
            drop: function(event, ui) {

                console.log('appendElementElement', $(this));

                var resource = $(this).closest( '.resource')[0];
                if (debug) console.log(resource);
                var contentId = getIdFromClassString(ui.draggable.attr('class'));
                var elementId = getIdFromClassString($(this).attr('class'));

                if (debug) console.log('Content Id: ' + contentId);
                if (!contentId) {
                    // create element on the fly
                    //var el = _Elements.addElement(null, 'element', null);
                    var tag = $(ui.draggable).text();
                //var el = _Elements.addElement(null, 'Element', '"tag":"' + tag + '"');
                //if (debug) console.log(el);
                //contentId = el.id;
                //if (debug) console.log('Created new element on the fly: ' + contentId);
                }

                var pos = $('.content, .element', $(this)).length;
                if (debug) console.log(pos);
                var props;
                if (resource) {
                    var resourceId = getIdFromClassString($(resource).attr('class'));
                    props = '"resourceId" : "' + resourceId + '", "' + resourceId + '" : "' + pos + '"';
                } else {
                    props = '"*" : "' + pos + '"';
                }

                if (!contentId) {
                    props += ', "name" : "New ' + tag + ' ' + Math.floor(Math.random() * (999999 - 1)) + '", "type" : "' + tag.capitalize() + '", "tag" : "' + tag + '"';
                }

                if (debug) console.log(props);
                _Entities.addSourceToTarget(contentId, elementId, props);
            }
        });

        var numberOfElements = $('.element', parent).size();
        if (debug) console.log(numberOfElements);
        if (numberOfElements > 0) {
            disable($('.delete_icon', parent)[0]);
        }

    },

    removeComponentFromResource : function(componentId, resourceId) {
        if (debug) console.log('Resources.removeComponentFromResource');

        var resource = $('.' + resourceId + '_');
        var component = $('.' + componentId + '_', resource);
        component.remove();

        var numberOfComponents = $('.component', resource).size();
        if (debug) console.log(numberOfComponents);
        if (numberOfComponents == 0) {
            enable($('.delete_icon', resource)[0]);
        }
        _Entities.removeSourceFromTarget(componentId, resourceId);

    },

    removeElementFromResource : function(elementId, resourceId) {
        if (debug) console.log('Resources.removeElementFromResource');

        var resource = $('.' + resourceId + '_');
        var element = $('.' + elementId + '_', resource);
        element.remove();

        var numberOfElements = $('.element', resource).size();
        if (debug) console.log(numberOfElements);
        if (numberOfElements == 0) {
            enable($('.delete_icon', resource)[0]);
        }
        _Entities.removeSourceFromTarget(elementId, resourceId);

    },

    addContentToElement : function(contentId, elementId) {
        if (debug) console.log('Resources.addContentToElement');

        var element = $('.' + elementId + '_');
        var content = $('.' + contentId + '_').clone();

        element.append(content);

        $('.delete_icon', content).remove();
        content.append('<img title="Remove content ' + contentId + ' from element ' + elementId + '" '
            + 'alt="Remove content ' + contentId + ' from element ' + elementId + '" class="delete_icon button" src="' + _Contents.delete_icon + '">');
        $('.delete_icon', content).on('click', function() {
            _Resources.removeElementFromResource(contentId, elementId)
        });
        content.draggable('destroy');

        var numberOfContents = $('.element', element).size();
        if (debug) console.log(numberOfContents);
        if (numberOfContents > 0) {
            disable($('.delete_icon', element)[0]);
        }

    },

    removeContentFromElement : function(contentId, elementId) {

        var element = $('.' + elementId + '_');
        var content = $('.' + contentId + '_', element);
        content.remove();

        var numberOfContents = $('.element', element).size();
        if (debug) console.log(numberOfContents);
        if (numberOfContents == 0) {
            enable($('.delete_icon', element)[0]);
        }
        _Entities.removeSourceFromTarget(contentId, elementId);

    },

    updateContent : function(contentId, content) {
        //console.log('update ' + contentId + ' with ' + content);
        var url = rootUrl + 'content' + '/' + contentId;
        if (debug) console.log(content);
        var text = content.replace(/\n/g, '<br>');
        if (debug) console.log(text);
        text = $.quoteString(text);
        var data = '{ "content" : ' + text + ' }';
        $.ajax({
            url: url,
            //async: false,
            type: 'PUT',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            data: data,
            success: function(data) {
            //refreshIframes();
            //keyEventBlocked = true;
            //enable(button);
            //console.log('success');
            }
        });
    },

    addResource : function(button) {
        return _Entities.add(button, 'Resource');
    },
	
    addComponent : function(button) {
        return _Entities.add(button, 'Component');
    },

    addElement : function(button) {
        return _Entities.add(button, 'Element');
    },
	
    deleteResource : function(button, resource) {
        if (debug) console.log('delete resource ' + resource);
        deleteNode(button, resource);
    },

    showSubEntities : function(resourceId, entity) {
        var follow = followIds(resourceId, entity);
        $(follow).each(function(i, nodeId) {
            if (nodeId) {
                //            console.log(rootUrl + nodeId);
                $.ajax({
                    url: rootUrl + nodeId,
                    dataType: 'json',
                    contentType: 'application/json; charset=utf-8',
                    async: false,
                    //headers: { 'X-User' : 457 },
                    success: function(data) {
                        if (!data || data.length == 0 || !data.result) return;
                        var result = data.result;
                        //                    console.log(result);
                        _Resources.appendElement(result, entity, resourceId);
                        _Resources.showSubEntities(resourceId, result);
                    }
                });
            }
        });
    },

    toggleResource : function(button, resource) {

        if (debug) console.log('toggle resource ' + resource.id);
        var resourceElement = $('.' + resource.id + '_');
        if (debug) console.log(resourceElement);

        var subs = $('.component, .element', resourceElement);
        subs.each(function(i,el){
            $(el).toggle(50, function() {
                if (button.src.endsWith('icon/tree_arrow_down.png')) {
                    button.src = 'icon/tree_arrow_right.png'
                } else {
                    button.src = 'icon/tree_arrow_down.png'
                }

            });
        });


    },

    appendElement : function(entity, parentEntity, resourceId) {
        //    console.log('appendElement: resourceId=' + resourceId);
        //    console.log(entity);
        //    console.log(parentEntity);
        var type = entity.type.toLowerCase();
        var id = entity.id;
        var resourceEntitySelector = $('.' + resourceId + '_');
        var element = (parentEntity ? $('.' + parentEntity.id + '_', resourceEntitySelector) : resourceEntitySelector);
        //    console.log(element);
        _Entities.appendEntityElement(entity, element);

        if (type == 'content') {
            div.append('<img title="Edit Content" alt="Edit Content" class="edit_icon button" src="' + Structr.edit_icon + '">');
            $('.edit_icon', div).on('click', function() {
                editContent(this, resourceId, id)
            });
        } else {
            div.append('<img title="Add" alt="Add" class="add_icon button" src="' + Structr.add_icon + '">');
            $('.add_icon', div).on('click', function() {
                addNode(this, 'content', entity, resourceId)
            });
        }
        //    //div.append('<img class="sort_icon" src="icon/arrow_up_down.png">');
        div.sortable({
            axis: 'y',
            appendTo: '.' + resourceId + '_',
            delay: 100,
            containment: 'parent',
            cursor: 'crosshair',
            //handle: '.sort_icon',
            stop: function() {
                $('div.nested', this).each(function(i,v) {
                    var nodeId = getIdFromClassString($(v).attr('class'));
                    if (!nodeId) return;
                    var url = rootUrl + nodeId + '/' + 'in';
                    $.ajax({
                        url: url,
                        dataType: 'json',
                        contentType: 'application/json; charset=utf-8',
                        async: false,
                        headers: headers,
                        success: function(data) {
                            if (!data || data.length == 0 || !data.result) return;
                            //                        var rel = data.result;
                            //var pos = rel[parentId];
                            var nodeUrl = rootUrl + nodeId;
                            setPosition(resourceId, nodeUrl, i)
                        }
                    });
                    _Resources.reloadPreviews();
                });
            }
        });
    },


    addNode : function(button, type, entity, resourceId) {
        if (isDisabled(button)) return;
        disable(button);
        var pos = $('.' + resourceId + '_ .' + entity.id + '_ > div.nested').length;
        //    console.log('addNode(' + type + ', ' + entity.id + ', ' + entity.id + ', ' + pos + ')');
        var url = rootUrl + type;
        var resp = $.ajax({
            url: url,
            //async: false,
            type: 'POST',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            headers: headers,
            data: '{ "type" : "' + type + '", "name" : "' + type + '_' + Math.floor(Math.random() * (9999 - 1)) + '", "elements" : "' + entity.id + '" }',
            success: function(data) {
                var getUrl = resp.getResponseHeader('Location');
                $.ajax({
                    url: getUrl + '/all',
                    success: function(data) {
                        var node = data.result;
                        if (entity) {
                            _Resources.appendElement(node, entity, resourceId);
                            _Resources.setPosition(resourceId, getUrl, pos);
                        }
                        //disable($('.' + groupId + '_ .delete_icon')[0]);
                        enable(button);
                    }
                });
            }
        });
    },

    reloadPreviews : function() {
        $('iframe', $('#previews')).each(function() {
            this.contentDocument.location.reload(true);
        });
    },

    linkSelectionToResource : function(rootResourceId, sourceId, linkedResourceId, startOffset, endOffset) {
        console.log('linkResourcesToSelection(' + rootResourceId + ', ' + sourceId + ', ' + linkedResourceId + ', ' + startOffset + ', ' + endOffset + ')');
        var data = '{ "command" : "LINK" , "id" : "' + sourceId + '" , "data" : { "rootResourceId" : "' + rootResourceId + '", "resourceId" : "' + linkedResourceId + '", "startOffset" : ' + startOffset + ', "endOffset" : ' + endOffset + '} }';
        return send(data);
    }

};