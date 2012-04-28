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

var resources;
var previews, previewTabs, controls, palette;
var selStart, selEnd;
var sel;
var contentSourceId, elementSourceId, rootId;
var textBeforeEditing;
var win = $(window);

var _Resources = {

    icon : 'icon/page.png',
    add_icon : 'icon/page_add.png',
    delete_icon : 'icon/page_delete.png',
    clone_icon : 'icon/page_copy.png',

    init : function() {
    },

    resize : function() {

        var windowWidth = win.width();
        var windowHeight = win.height();
        var headerOffsetHeight = 67;
        var previewOffset = 33;
	
        resources.css({
            width: Math.max(180, Math.min(windowWidth/3, 360)) + 'px',
            height: windowHeight - headerOffsetHeight + 'px'
        });

        palette.css({
            width: Math.min(240, Math.max(360, windowWidth/4)) + 'px',
            height: windowHeight - (headerOffsetHeight+10) + 'px'
        });

        var rw = resources.width() + 12;
        var pw = palette.width() + 60;

        previews.css({
            width: windowWidth-rw-pw + 'px',
            height: win.height() - headerOffsetHeight + 'px'
        });

        $('.previewBox', previews).css({
            width: windowWidth-rw-pw-4 + 'px',
            height: windowHeight - (headerOffsetHeight + previewOffset) + 'px'
        });

        $('.previewBox', previews).find('iframe').css({
            width: $('.previewBox', previews).width() + 'px',
            height: windowHeight - (headerOffsetHeight + previewOffset) + 'px'
        });


    },

    onload : function() {
        activeTab = $.cookie('structrActiveTab');
        if (debug) console.log('value read from cookie', activeTab);

        //Structr.activateMenuEntry('resources');
        if (debug) console.log('onload');
        main.append('<div id="resources"></div><div id="previews"></div><div id="palette"></div><div id="components"></div><div id="elements"></div><div id="contents"></div>');

        resources = $('#resources');
        components = $('#components');
        elements = $('#elements');
        contents = $('#contents');
        previews = $('#previews');
        palette = $('#palette');
        //main.before('<div id="hoverStatus">Hover status</div>');
        $('#controls', main).remove();
        //main.children().first().before('<div id="controls"><input type="checkbox" id="toggleResources">Show Resource Tree <input type="checkbox" id="toggleComponents">Show Components <input type="checkbox" id="toggleElements">Show Elements <input type="checkbox" id="toggleContents">Show Contents</div>');

        previews.append('<ul id="previewTabs"></ul>');
        previewTabs = $('#previewTabs', previews);

        //	if ($.cookie('structrResourcesVisible') == 'false') {
        //	    resources.hide();
        //	} else {
        //	    $('#toggleResources').prop('checked', true);
        //	}
		
        $('#toggleResources').on('click', function() {
            resources.toggle();
        });

        if ($.cookie('structrComponentsVisible') == 'false') {
            components.hide();
        } else {
            $('#toggleComponents').prop('checked', true);
        }
        $('#toggleComponents').on('click', function() {
            components.toggle();
        });

        if ($.cookie('structrElementsVisible') == 'false') {
            elements.hide();
        } else {
            $('#toggleElements').prop('checked', true);
        }
        $('#toggleElements').on('click', function() {
            elements.toggle();
        });

        if ($.cookie('structrContentsVisible') == 'false') {
            contents.hide();
        } else {
            $('#toggleContents').prop('checked', true);
        }
        $('#toggleContents').on('click', function() {
            contents.toggle();
        });

        _Resources.refresh();
        //_Resources.refreshComponents();
        //_Resources.refreshElements();
        _Elements.showPalette();
        //_Contents.refresh();

        previewTabs.append('<li id="import_page" class="button"><img class="add_button icon" src="icon/page_white_put.png"></li>');
        $('#import_page', previewTabs).on('click', function() {
			
            var dialog = $('#dialogBox .dialogText');
            var dialogMsg = $('#dialogMsg');
			
            dialog.empty();
            dialogMsg.empty();

            dialog.append('<table class="props">'
                + '<tr><td><label for="address">Address:</label></td><td><input id="_address" name="address" size="20" value="http://"></td></tr>'
                + '<tr><td><label for="name">Name of new page:</label></td><td><input id="_name" name="name" size="20"></td></tr>'
                + '<tr><td><label for="timeout">Timeout (ms)</label></td><td><input id="_timeout" name="timeout" size="20" value="5000"></td></tr>'
                + '<tr><td><label for="publicVisibilty">Visible to public</label></td><td><input type="checkbox" id="_publicVisible" name="publicVisibility"></td></tr>'
                + '<tr><td><label for="authVisibilty">Visible to authenticated users</label></td><td><input type="checkbox" checked="checked" id="_authVisible" name="authVisibilty"></td></tr>'
                + '</table>');

            var addressField = $('#_address', dialog);

            if (debug) console.log('addressField', addressField);

            addressField.on('blur', function() {
                var addr = $(this).val().replace(/\/+$/, "");
                if (debug) console.log(addr);
                $('#_name', dialog).val(addr.substring(addr.lastIndexOf("/")+1));
            });

            dialog.append('<button id="startImport">Start Import</button>');

            Structr.dialog('Import page from URL', function() {
                return true;
            }, function() {
                return true;
            });
			
            $('#startImport').on('click', function() {

                var address = $('#_address', dialog).val();
                var name    = $('#_name', dialog).val();
                var timeout = $('#_timeout', dialog).val();
                var publicVisible = $('#_publicVisible:checked', dialog).val() == 'on';
                var authVisible = $('#_authVisible:checked', dialog).val() == 'on';

                if (debug) console.log('start');
                return Server.importPage(address, name, timeout, publicVisible, authVisible);
            });
            
        });

        previewTabs.append('<li id="add_resource" class="button"><img class="add_button icon" src="icon/add.png"></li>');
        $('#add_resource', previewTabs).on('click', function() {
            var entity = {};
            entity.type = 'Resource';
            Server.create(entity);
        });

    },

    refresh : function() {
        resources.empty();
        return Server.list('Resource');
    },

    refreshComponents : function() {
        components.empty();
        if (Server.list('Component')) {
            components.append('<button class="add_component_icon button"><img title="Add Component" alt="Add Component" src="' + _Components.add_icon + '"> Add Component</button>');
            $('.add_component_icon', main).on('click', function() {
                var entity = {};
                entity.type = 'Component';
                Server.create(entity);
            });
        }
    },

    refreshElements : function() {
        elements.empty();
        if (Server.list('Element')) {
            elements.append('<button class="add_element_icon button"><img title="Add Element" alt="Add Element" src="' + _Elements.add_icon + '"> Add Element</button>');
            $('.add_element_icon', main).on('click', function() {
                var entity = {};
                entity.type = 'Element';
                Server.create(entity);
            });
        }
    },

    addTab : function(entity) {
        previewTabs.children().last().before(''
            + '<li id="show_' + entity.id + '" class="' + entity.id + '_"></li>');

        var tab = $('#show_' + entity.id, previews);
		
        tab.append('<img class="typeIcon" src="icon/page.png"> <b class="name_">' + entity.name + '</b>');
        tab.append('<img title="Delete resource \'' + entity.name + '\'" alt="Delete resource \'' + entity.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
        tab.append('<a target="_blank" href="' + viewRootUrl + entity.name + '">'
            + '<img class="view_icon button" title="View ' + entity.name + ' in new window" alt="View ' + entity.name + ' in new window" src="icon/eye.png">'
            + '</a></li>');

        var deleteIcon = $('.delete_icon', tab);
        deleteIcon.hide();
        deleteIcon.on('click', function(e) {
            e.stopPropagation();
            _Entities.deleteNode(this, entity);
        });
        deleteIcon.on('mouseover', function(e) {
            var self = $(this);
            self.show();
		
        });

        _Entities.appendAccessControlIcon(tab, entity, true);
        
        return tab;
    },

    resetTab : function(element, name) {

        if (debug) console.log('resetTab', element);
        
//        var id = getId(element);
//        activeTab = id;
//
//        var iframe = $('#preview_' + id);
//        iframe.attr('src', viewRootUrl + name + '?edit');
//        _Resources.reloadPreviews();
        
        element.children('input').hide();
        element.children('.name_').show();
        
        var icons = $('.button', element);
        //icon.hide();

        element.hover(function(e) {
            icons.show();
        },
        function(e) {
            icons.hide();
        });

        element.on('click', function(e) {
            var self = $(this);
            var clicks = e.originalEvent.detail;
            if (clicks == 1) {
                if (debug) console.log('click', self, self.css('z-index'));
                if (self.hasClass('active')) {
                    _Resources.makeTabEditable(self);
                } else {
                    _Resources.activateTab(self);
                }
            }
        });

        if (getId(element) == activeTab) {
            _Resources.activateTab(element);
        }
    },

    activateTab : function(element) {
        
        var name = $.trim(element.children('b.name_').text());
        if (debug) console.log('activateTab', element, name);

        previewTabs.children('li').each(function() {
            $(this).removeClass('active');
        });

        $('.previewBox', previews).each(function() {
            $(this).hide();
        });
        //var id = $(this).attr('id').substring(5);

        var id = getId(element);
        activeTab = id;

        var iframe = $('#preview_' + id);
        if (debug) console.log(iframe);
        iframe.attr('src', viewRootUrl + name + '?edit');
        iframe.parent().show();
        iframe.on('load', function() {
            if (debug) console.log('iframe loaded', $(this));
        });

        element.addClass('active');

    },

    makeTabEditable : function(element) {
        //element.off('dblclick');
        element.off('hover');
        var oldName = $.trim(element.children('b').text());
        //console.log('oldName', oldName);
        element.children('b').hide();
        element.find('.button').hide();
        element.append('<input type="text" size="' + (oldName.length+4) + '" class="newName_" value="' + oldName + '">');

        var input = $('input', element);

        input.focus().select();

        input.on('blur', function() {
            var self = $(this);
            var newName = self.val();
            Server.setProperty(getId(element), "name", newName);
            _Resources.resetTab(element, newName);
        });
        
        input.keypress(function(e) {
            if (e.keyCode == 13) {
                var self = $(this);
                var newName = self.val();
                Server.setProperty(getId(element), "name", newName);
                _Resources.resetTab(element, newName);
            }
        });

        element.off('click');

    },

    appendResourceElement : function(entity, resourceId, rootId) {

        resources.append('<div class="node resource ' + entity.id + '_"></div>');
        var div = $('.' + entity.id + '_', resources);

        entity.resourceId = entity.id;

        _Entities.appendExpandIcon(div, entity);
        _Entities.setMouseOver(div);

        div.append('<img class="typeIcon" src="icon/page.png">'
            + '<b class="name_">' + entity.name + '</b> <span class="id">' + entity.id + '</span>');

        div.append('<img title="Delete resource \'' + entity.name + '\'" alt="Delete resource \'' + entity.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            var self = $(this);
            self.off('click');
            self.off('mouseover');
            _Entities.deleteNode(this, entity);
        });

        div.append('<img title="Link resource \'' + entity.name + '\' to current selection" alt="Link resource \'' + entity.name + '\' to current selection" class="link_icon button" src="' + Structr.link_icon + '">');
        $('.link_icon', div).on('click', function() {
            //console.log(rootId, sourceId);
            if (sourceId && selStart && selEnd) {
                // function(resourceId, sourceId, linkedResourceId, startOffset, endOffset)
                _Resources.linkSelectionToResource(rootId, sourceId, entity.id, selStart, selEnd);
            //$('.link_icon').hide();
            }
        });

        div.append('<img title="Clone resource \'' + entity.name + '\'" alt="Clone resource \'' + entity.name + '\'" class="clone_icon button" src="' + _Resources.clone_icon + '">');
        $('.clone_icon', div).on('click', function(e) {
            e.stopPropagation();
            var self = $(this);
            self.off('click');
            self.off('mouseover');
            Server.cloneResource(entity.id);
        });

        _Entities.appendEditPropertiesIcon(div, entity);

        var tab = _Resources.addTab(entity);

        previews.append('<div class="previewBox"><iframe id="preview_'
            + entity.id + '"></iframe></div><div style="clear: both"></div>');

        _Resources.resetTab(tab, entity.name);

        $('#preview_' + entity.id).hover(function() {
            var self = $(this);
            var elementContainer = self.contents().find('.structr-element-container');
            //console.log(self, elementContainer);
            elementContainer.addClass('structr-element-container-active');
            elementContainer.removeClass('structr-element-container');
        }, function() {
            var self = $(this);
            var elementContainer = self.contents().find('.structr-element-container-active');
            //console.log(elementContainer);
            elementContainer.addClass('structr-element-container');
            elementContainer.removeClass('structr-element-container-active');
        //self.find('.structr-element-container-header').remove();
        });

        $('#preview_' + entity.id).load(function() {

            var offset = $(this).offset();

            //console.log(this);
            var doc = $(this).contents();
            var head = $(doc).find('head');
            if (head) head.append('<style media="screen" type="text/css">'
                + '* { z-index: 0}\n'
                + '.nodeHover { border: 1px dotted red; }\n'
                + '.structr-content-container { display: inline-block; border: none; margin: 0; padding: 0; min-height: 10px; min-width: 10px; }\n'
                //		+ '.structr-element-container-active { display; inline-block; border: 1px dotted #e5e5e5; margin: -1px; padding: -1px; min-height: 10px; min-width: 10px; }\n'
                //		+ '.structr-element-container { }\n'
                + '.structr-element-container-active:hover { border: 1px dotted red ! important; }\n'
                + '.structr-droppable-area { border: 1px dotted red ! important; }\n'
                + '.structr-editable-area { border: 1px dotted orange ! important; }\n'
                + '.structr-editable-area-active { background-color: #ffe; border: 1px solid orange ! important; color: #333; margin: -1px; padding: 1px; }\n'
                //		+ '.structr-element-container-header { font-family: Arial, Helvetica, sans-serif ! important; position: absolute; font-size: 8pt; }\n'
                + '.structr-element-container-header { font-family: Arial, Helvetica, sans-serif ! important; position: absolute; font-size: 8pt; color: #333; border-radius: 5px; border: 1px solid #a5a5a5; padding: 3px 6px; margin: 6px 0 0 0; background-color: #eee; background: -webkit-gradient(linear, left bottom, left top, from(#ddd), to(#eee)) no-repeat; background: -moz-linear-gradient(90deg, #ddd, #eee) no-repeat; filter: progid:DXImageTransform.Microsoft.Gradient(StartColorStr="#eeeeee", EndColorStr="#dddddd", GradientType=0);\n'
                + '.structr-element-container-header img { width: 16px ! important; height: 16px ! important; }\n'
                + '.link-hover { border: 1px solid #00c; }\n'
                + '.edit_icon, .add_icon, .delete_icon, .close_icon, .key_icon {  cursor: pointer; heigth: 16px; width: 16px; vertical-align: top; float: right;  position: relative;}\n'
                + '</style>');
	
            var iframeDocument = $(this).contents();
            var iframeWindow = this.contentWindow;

            var droppables = iframeDocument.find('[structr_element_id]');

            if (droppables.length == 0) {

                //iframeDocument.append('<html structr_element_id="' + entity.id + '">dummy element</html>');
                var html = iframeDocument.find('html');
                html.attr('structr_element_id', entity.id);
                html.addClass('structr-element-container');

            }
            droppables = iframeDocument.find('[structr_element_id]');

            droppables.each(function(i,element) {
                //console.log(element);
                var el = $(element);

                el.droppable({
                    accept: '.element, .content, .component',
                    greedy: true,
                    hoverClass: 'structr-droppable-area',
                    iframeOffset: {
                        'top' : offset.top,
                        'left' : offset.left
                    },
                    drop: function(event, ui) {
                        var self = $(this);
                        var resource = self.closest( '.resource')[0];
                        var resourceId;
                        var pos;
                        var nodeData = {};
    
                        if (resource) {

                            // we're in the main page
                            resourceId = getId(resource);
                            pos = $('.content, .element', self).length;

                        } else {
                            
                            // we're in the iframe
                            resource = self.closest('[structr_resource_id]')[0];
                            resourceId = $(resource).attr('structr_resource_id');
                            pos = $('[structr_element_id]', self).length;
                        }
                        
                        var contentId = getId(ui.draggable);
                        var elementId = getId(self);

                        if (!elementId) elementId = self.attr('structr_element_id');

                        if (!contentId) {
                            // create element on the fly
                            //var el = _Elements.addElement(null, 'element', null);
                            var tag = $(ui.draggable).text();
                            nodeData.type = tag.capitalize();
                        }
						

                        var relData = {};
                        
                        if (resourceId) {
                            relData.resourceId = resourceId;
                            relData[resourceId] = pos;
                        } else {
                            relData['*'] = pos;
                        }

                        nodeData.tag = (tag != 'content' ? tag : '');
                        nodeData.id = contentId;
                        if (debug) console.log(relData);
                        Server.createAndAdd(elementId, nodeData, relData);
                    }
                });

                var structrId = el.attr('structr_element_id');
                var type = el.attr('structr_type');
                var name = el.attr('structr_name');
                var tag  = element.nodeName.toLowerCase();
                if (structrId) {

                    $('.move_icon', el).on('mousedown', function(e) {
                        e.stopPropagation();
                        var self = $(this);
                        var element = self.closest('[structr_element_id]');
                        //var element = self.children('.structr-node');
                        if (debug) console.log(element);
                        var entity = Structr.entity(structrId, element.attr('structr_element_id'));
                        entity.type = element.attr('structr_type');
                        entity.name = element.attr('structr_name');
                        if (debug) console.log('move', entity);
                        //var parentId = element.attr('structr_element_id');
                        self.parent().children('.structr-node').show();
                    });

                    $('b', el).on('click', function(e) {
                        e.stopPropagation();
                        var self = $(this);
                        var element = self.closest('[structr_element_id]');
                        var entity = Structr.entity(structrId, element.attr('structr_element_id'));
                        entity.type = element.attr('structr_type');
                        entity.name = element.attr('structr_name');
                        if (debug) console.log('edit', entity);
                        //var parentId = element.attr('structr_element_id');
                        if (debug) console.log(element);
                        Structr.dialog('Edit Properties of ' + entity.id, function() {
                            if (debug) console.log('save')
                        }, function() {
                            if (debug) console.log('cancelled')
                        });
                        _Entities.showProperties(this, entity, $('#dialogBox .dialogText'));
                    });

                    $('.delete_icon', el).on('click', function(e) {
                        e.stopPropagation();
                        var self = $(this);
                        var element = self.closest('[structr_element_id]');
                        var entity = Structr.entity(structrId, element.attr('structr_element_id'));
                        entity.type = element.attr('structr_type');
                        entity.name = element.attr('structr_name');
                        if (debug) console.log('delete', entity);
                        var parentId = element.attr('structr_element_id');

                        Server.removeSourceFromTarget(entity.id, parentId);
                        _Entities.deleteNode(this, entity);
                    });
                    var offsetTop = -30;
                    var offsetLeft = 0;
                    el.on({
                        mouseover: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            //self.off('click');

                            self.addClass('structr-element-container-active');

                            self.parent().children('.structr-element-container-header').remove();

                            self.append('<div class="structr-element-container-header">'
                                + '<img class="typeIcon" src="/structr/'+ _Elements.icon + '">'
                                + '<b class="name_">' + name + '</b> <span class="id">' + structrId + '</b>'
                                + '<img class="delete_icon structr-container-button" title="Delete ' + structrId + '" alt="Delete ' + structrId + '" src="/structr/icon/delete.png">'
                                + '<img class="edit_icon structr-container-button" title="Edit properties of ' + structrId + '" alt="Edit properties of ' + structrId + '" src="/structr/icon/application_view_detail.png">'
                                + '<img class="move_icon structr-container-button" title="Move ' + structrId + '" alt="Move ' + structrId + '" src="/structr/icon/arrow_move.png">'
                                + '</div>'
                                );

                            var nodes = $('.' + structrId + '_');
                            nodes.parent().removeClass('nodeHover');
                            nodes.addClass('nodeHover');

                            var pos = self.position();
                            var header = self.children('.structr-element-container-header');
                            header.css({
                                position: "absolute",
                                top: pos.top + offsetTop + 'px',
                                left: pos.left + offsetLeft + 'px',
                                cursor: 'pointer'
                            }).show();
                            if (debug) console.log(header);
                        },
                        mouseout: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            self.removeClass('.structr-element-container');
                            var header = self.children('.structr-element-container-header');
                            header.remove();
                            var nodes = $('.' + structrId + '_');
                            nodes.removeClass('nodeHover');
                        }
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
                            $('#hoverStatus').text('Editable content element: ' + self.attr('structr_content_id'));
                            var nodes = $('.' + structrId + '_');
                            nodes.parent().removeClass('nodeHover');
                            nodes.addClass('nodeHover');
                        },
                        mouseout: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            //swapFgBg(self);
                            self.removeClass('structr-editable-area');
                            //self.attr('contenteditable', false);
                            $('#hoverStatus').text('-- non-editable --');
                            var nodes = $('.' + structrId + '_');
                            nodes.removeClass('nodeHover');
                        },
                        click: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            self.removeClass('structr-editable-area');
                            self.addClass('structr-editable-area-active');

                            // Store old text in global var
                            textBeforeEditing = self.contents().first().text();
                            if (debug) console.log("textBeforeEditing", textBeforeEditing);

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
                            contentSourceId = self.attr('structr_content_id');
                            if (debug) console.log(self.contents().first());
                            if (debug) console.log('blur contentSourceId: ' + contentSourceId);
                            //_Resources.updateContent(contentSourceId, textBeforeEditing, self.contents().first().text());
                            Server.patch(contentSourceId, textBeforeEditing, self.contents().first().text());
                            contentSourceId = null;
                            self.attr('contenteditable', false);
                            self.removeClass('structr-editable-area-active');
                        }
                    });
				
                }
            });

            //_Resources.resize();
        });
	
    },

    appendElementElement : function(entity, parentId, resourceId) {
        if (debug) console.log('Resources.appendElementElement');
        var div = _Elements.appendElementElement(entity, parentId, resourceId);
        //console.log(div);
        if (parentId) {

            $('.delete_icon', div).replaceWith('<img title="Remove element \'' + entity.name + '\' from resource ' + parentId + '" '
                + 'alt="Remove element ' + entity.name + ' from ' + parentId + '" class="delete_icon button" src="icon/brick_delete.png">');
            $('.delete_icon', div).on('click', function(e) {
                e.stopPropagation();
                var self = $(this);
                self.off('click');
                self.off('mouseover');
                Server.removeSourceFromTarget(entity.id, parentId);
            });
        }

        _Entities.setMouseOver(div);

        var resource = div.closest( '.resource')[0];
        if (!resource && resources) {
            div.draggable({
                revert: 'invalid',
                containment: '#resources',
                zIndex: 4,
                helper: 'clone'
            });
        }

        var sorting = false;
        var obj = {};
        obj.command = 'SORT';

        div.sortable({
            sortable: '.node',
            containment: '#resources',
            start: function(event, ui) {
                sorting = true;
                var resourceId = getId(ui.item.closest('.resource')[0]);
                obj.id = resourceId;
            },
            update: function(event, ui) {
                if (debug) console.log('---------')
                if (debug) console.log(resourceId);
                var data = {};
                $(ui.item).parent().children('.node').each(function(i,v) {
                    var self = $(this);
                    if (debug) console.log(getId(self), i);
                    data[getId(self)] = i;
                    obj.data = data;
                    _Entities.resetMouseOverState(v);
                });
                sendObj(obj);
                sorting = false;
                _Resources.reloadPreviews();
            },
            stop: function(event, ui) {
                sorting = false;
                _Entities.resetMouseOverState(ui.item);
            }
        });

        div.droppable({
            accept: '.element, .content, .component',
            greedy: true,
            hoverClass: 'nodeHover',
            drop: function(event, ui) {
                if (sorting) {
                    if (debug) console.log('sorting, no drop allowed');
                    return;
                }
                var self = $(this);
                var nodeData = {};
                var resourceId;
                var relData = {};
				
                var resource = self.closest('.resource')[0];

                if (debug) console.log(resource);
                var contentId = getId(ui.draggable);
                var elementId = getId(self);
                var tag;
                if (debug) console.log('contentId', contentId);
                if (!contentId) {
                    tag = $(ui.draggable).text();
                } else {
                    tag = Structr.getClass($(ui.draggable));
                }
                if (debug) console.log($(ui.draggable));
                var pos = $('.node', self).length;
                if (debug) console.log(pos);

                if (resource) {
                    resourceId = getId(resource);
                    relData.resourceId = resourceId;
                    relData[resourceId] = pos;
                } else {
                    relData['*'] = pos;
                }
				
                if (!isExpanded(elementId, null, resourceId)) {
                    _Entities.toggleElement(self.children('.expand_icon'));
                }

                var component = self.closest( '.component')[0];
                if (component) {
                    var componentId = getId(component);
                    relData.componentId = componentId;
                    relData[componentId] = pos;
                }

                nodeData.tag = (tag != 'content' ? tag : '');
                nodeData.type = tag.capitalize();
                nodeData.id = contentId;
                nodeData.targetResourceId = resourceId;

                var sourceResource = ui.draggable.closest('.resource')[0];
                if (sourceResource) {
                    var sourceResourceId = getId(sourceResource);
                    nodeData.sourceResourceId = sourceResourceId;
                }

                console.log('drop event in appendElementElement', elementId, nodeData, relData);
                Server.createAndAdd(elementId, nodeData, relData);
            }
        });

        return div;
    },

    appendComponentElement : function(component, parentId, resourceId) {
        if (debug) console.log('Resources.appendComponentElement');
        var div = _Components.appendComponentElement(component, parentId, resourceId);
        //console.log(div);

        if (parentId) {

            $('.delete_icon', div).replaceWith('<img title="Remove component \'' + component.name + '\' from resource ' + parentId + '" '
                + 'alt="Remove component ' + component.name + ' from ' + parentId + '" class="delete_icon button" src="' + _Components.delete_icon + '">');
            $('.delete_icon', div).on('click', function() {
                Server.removeSourceFromTarget(component.id, parentId);
            });

        }

        var resource = div.closest( '.resource')[0];
        if (!resource && resources) {
            div.draggable({
                revert: 'invalid',
                containment: '#main',
                zIndex: 1,
                helper: 'clone'
            });
        }
	
        var sorting = false;
        var obj = {};
        obj.command = 'SORT';

        div.sortable({
            sortable: '.node',
            containment: 'parent',
            start: function(event, ui) {
                sorting = true;
                var resourceId = getId(ui.item.closest('.resource')[0]);
                obj.id = resourceId;
            },
            update: function(event, ui) {
                if (debug) console.log('---------')
                if (debug) console.log(resourceId);
                var data = {};
                $(ui.item).parent().children('.node').each(function(i,v) {
                    var self = $(this);
                    if (debug) console.log(getId(self), i);
                    data[getId(self)] = i;
                    obj.data = data;
                });
                sendObj(obj);
                sorting = false;
                _Resources.reloadPreviews();
            },
            stop: function(event, ui) {
                sorting = false;
            }
        });

        div.droppable({
            accept: '.element, .content',
            greedy: true,
            hoverClass: 'nodeHover',
            drop: function(event, ui) {

                var self = $(this);

                var node = $(self.closest('.node')[0]);

                var resource = node.closest( '.resource')[0];

                if (debug) console.log(resource);
                var contentId = getId(ui.draggable);
                if (!contentId) {
                    var tag = $(ui.draggable).text();
                }
                var pos = node.parent().children().size();
                if (debug) console.log(pos);
                var relData = {};
                if (resource) {
                    var resourceId = getId(resource);
                    relData[resourceId] = pos;
                    relData.resourceId = resourceId;
                } else {
                    relData['*'] = pos;
                }

                var component = self.closest( '.component')[0];
                if (component) {
                    var componentId = getId(component);
                    relData[componentId] = pos;
                    relData.componentId = componentId;
                }

                var nodeData = {};
                if (!contentId) {
                    nodeData.type = tag.capitalize();
                    nodeData.tag = (tag != 'content' ? tag : '');
                }

                if (debug) console.log('Content or Element dropped on Component', getId(node), nodeData, relData);
                Server.createAndAdd(getId(node), nodeData, relData);
            }
        });

        return div;
    },

    appendContentElement : function(content, parentId, resourceId) {
        if (debug) console.log('Resources.appendContentElement');
		
        var div = _Contents.appendContentElement(content, parentId, resourceId);

        if (parentId) {
            $('.delete_icon', div).replaceWith('<img title="Remove content \'' + content.name + '\' from resource ' + parentId + '" '
                + 'alt="Remove content ' + content.name + ' from element ' + parentId + '" class="delete_icon button" src="' + _Contents.delete_icon + '">');
            $('.delete_icon', div).on('click', function(e) {
                e.stopPropagation();
                var self = $(this);
                self.off('click');
                self.off('mouseover');
                Server.removeSourceFromTarget(content.id, parentId)
            });
        }

        _Entities.setMouseOver(div);

        div.draggable({
            iframeFix: true,
            revert: 'invalid',
            containment: '#resources',
            zIndex: 1,
            helper: 'clone'
        });
        return div;
    },

    removeComponentFromResource : function(componentId, resourceId) {
        if (debug) console.log('Resources.removeComponentFromResource');

        var resource = $('.' + resourceId + '_');
        var component = $('.' + componentId + '_', resource);
        //component.remove();

        var numberOfComponents = $('.component', resource).size();
        if (debug) console.log(numberOfComponents);
        if (numberOfComponents == 0) {
            enable($('.delete_icon', resource)[0]);
        }
        Server.removeSourceFromTarget(componentId, resourceId);

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
        Server.removeSourceFromTarget(elementId, resourceId);

    },
    //
    //    addContentToElement : function(contentId, elementId) {
    //        if (debug) console.log('Resources.addContentToElement');
    //
    //        var element = $('.' + elementId + '_');
    //        var content = $('.' + contentId + '_').clone();
    //
    //        //element.append(content);
    //
    //        $('.delete_icon', content).replaceWith('<img title="Remove content ' + contentId + ' from element ' + elementId + '" '
    //            + 'alt="Remove content ' + contentId + ' from element ' + elementId + '" class="delete_icon button" src="' + _Contents.delete_icon + '">');
    //        $('.delete_icon', content).on('click', function() {
    //            Server.removeElementFromResource(contentId, elementId)
    //        });
    //        content.draggable('destroy');
    //
    //        var numberOfContents = $('.element', element).size();
    //        if (debug) console.log(numberOfContents);
    //        if (numberOfContents > 0) {
    //            disable($('.delete_icon', element)[0]);
    //        }
    //
    //    },

    removeContentFromElement : function(contentId, elementId) {

        var element = $('.' + elementId + '_');
        var content = $('.' + contentId + '_', element);
        content.remove();

        var numberOfContents = $('.element', element).size();
        if (debug) console.log(numberOfContents);
        if (numberOfContents == 0) {
            enable($('.delete_icon', element)[0]);
        }
        Server.removeSourceFromTarget(contentId, elementId);

    },

    addResource : function(button) {
        var entity = {};
        entity.type = 'Resource';
        return Server.create(button, entity);
    },

    addComponent : function(button) {
        var entity = {};
        entity.type = 'Component';
        return Server.create(button, entity);
    },

    addElement : function(button) {
        var entity = {};
        entity.type = 'Element';
        return Server.create(button, entity);
    },
	
    showSubEntities : function(resourceId, entity) {
        var headers = {
            'X-StructrSessionToken' : token
        };
        var follow = followIds(resourceId, entity);
        $(follow).each(function(i, nodeId) {
            if (nodeId) {
                //            console.log(rootUrl + nodeId);
                $.ajax({
                    url: rootUrl + nodeId,
                    dataType: 'json',
                    contentType: 'application/json; charset=utf-8',
                    async: false,
                    headers: headers,
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

    addNode : function(button, type, entity, resourceId) {
        if (isDisabled(button)) return;
        disable(button);
        var pos = $('.' + resourceId + '_ .' + entity.id + '_ > div.nested').length;
        //    console.log('addNode(' + type + ', ' + entity.id + ', ' + entity.id + ', ' + pos + ')');
        var url = rootUrl + type;
        var headers = {
            'X-StructrSessionToken' : token
        };
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
    
    zoomPreviews : function(value) {
        $('.previewBox', previews).each(function() {
            var val = value/100;
            var box = $(this);

            box.css('-moz-transform',    'scale(' + val + ')');
            box.css('-o-transform',      'scale(' + val + ')');
            box.css('-webkit-transform', 'scale(' + val + ')');

            var w = origWidth * val;
            var h = origHeight * val;

            box.width(w);
            box.height(h);

            $('iframe', box).width(w);
            $('iframe', box).height(h);

            if (debug) console.log("box,w,h", box, w, h);

        });

    },

	
    linkSelectionToResource : function(rootResourceId, sourceId, linkedResourceId, startOffset, endOffset) {
        if (debug) console.log('linkResourcesToSelection(' + rootResourceId + ', ' + sourceId + ', ' + linkedResourceId + ', ' + startOffset + ', ' + endOffset + ')');
        var data = '{ "command" : "LINK" , "id" : "' + sourceId + '" , "data" : { "rootResourceId" : "' + rootResourceId + '", "resourceId" : "' + linkedResourceId + '", "startOffset" : ' + startOffset + ', "endOffset" : ' + endOffset + '} }';
        return send(data);
    }

};

$(document).ready(function() {
    Structr.registerModule('resources', _Resources);
    Structr.classes.push('resource');

    win.resize(function() {
        _Resources.resize();
    });
    
});

