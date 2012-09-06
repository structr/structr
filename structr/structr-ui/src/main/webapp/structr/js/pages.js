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

var pages;
var previews, previewTabs, controls, palette;
var selStart, selEnd;
var sel;
var contentSourceId, elementSourceId, rootId;
var textBeforeEditing;
var win = $(window);

$(document).ready(function() {
    Structr.registerModule('pages', _Pages);
    Structr.classes.push('page');

    win.resize(function() {
        _Pages.resize();
    });
});

var _Pages = {

    icon : 'icon/page.png',
    add_icon : 'icon/page_add.png',
    delete_icon : 'icon/page_delete.png',
    clone_icon : 'icon/page_copy.png',

    init : function() {
        //console.log('_Pages.init()');
        _Pages.resize();
    },

    resize : function() {
        
        var windowWidth = win.width();
        var windowHeight = win.height();
        var headerOffsetHeight = 76;
        var previewOffset = 27;
	
        //console.log(pages, palette, previews);
        
        if (pages && palette) {
            
            pages.css({
                width: Math.max(180, Math.min(windowWidth/3, 360)) + 'px',
                height: windowHeight - headerOffsetHeight + 'px'
            });

            var rw = pages.width() + 12;

            palette.css({
                width: Math.min(300, Math.max(360, windowWidth/4)) + 'px',
                height: windowHeight - (headerOffsetHeight+10) + 'px'
            });

            var pw = palette.width() + 60;

            if (previews) previews.css({
                width: windowWidth-rw-pw + 'px',
                height: win.height() - headerOffsetHeight + 'px'
            });

            $('.previewBox', previews).css({
                width: windowWidth-rw-pw-4 + 'px',
                height: windowHeight - (headerOffsetHeight + previewOffset) + 'px'
            });
            
            var iframes = $('.previewBox', previews).find('iframe');
            iframes.css({
                width: $('.previewBox', previews).width() + 'px',
                height: windowHeight - (headerOffsetHeight + previewOffset) + 'px'
            });

        }

    },

    onload : function() {
        activeTab = $.cookie('structrActiveTab');
        if (debug) console.log('value read from cookie', activeTab);

        if (debug) console.log('onload');
        
        main.prepend('<div id="pages"></div><div id="previews"></div><div id="palette"></div>');

        pages = $('#pages');
        previews = $('#previews');
        palette = $('#palette');
        //main.before('<div id="hoverStatus">Hover status</div>');
        $('#controls', main).remove();

        previews.append('<ul id="previewTabs"></ul>');
        previewTabs = $('#previewTabs', previews);

        _Pages.refresh();
        
        
        window.setTimeout('_Pages.resize()', 1000);

    },

    refresh : function() {
        pages.empty();
        previewTabs.empty();
        
        Command.list('Page');
        
        
        _Elements.showPalette();

        previewTabs.append('<li id="import_page" class="button"><img class="add_button icon" src="icon/page_white_put.png"></li>');
        $('#import_page', previewTabs).on('click', function(e) {
            e.stopPropagation();
			
            //var dialog = $('#dialogBox .dialogText');
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

            Structr.dialog('Import Page from URL', function() {
                return true;
            }, function() {
                return true;
            });
			
            $('#startImport').on('click', function(e) {
                e.stopPropagation();

                var address = $('#_address', dialog).val();
                var name    = $('#_name', dialog).val();
                var timeout = $('#_timeout', dialog).val();
                var publicVisible = $('#_publicVisible:checked', dialog).val() == 'on';
                var authVisible = $('#_authVisible:checked', dialog).val() == 'on';

                if (debug) console.log('start');
                return Command.importPage(address, name, timeout, publicVisible, authVisible);
            });
            
        });

        previewTabs.append('<li id="add_page" class="button"><img class="add_button icon" src="icon/add.png"></li>');
        $('#add_page', previewTabs).on('click', function(e) {
            e.stopPropagation();
            //var entity = {};
            //entity.type = 'Page';
            //Command.create(entity);
            Command.createSimplePage();
        });        
        
    },

    refreshComponents : function() {
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

    addTab : function(entity) {
        previewTabs.children().last().before(''
            + '<li id="show_' + entity.id + '" class="page ' + entity.id + '_"></li>');

        var tab = $('#show_' + entity.id, previews);
		
        tab.append('<img class="typeIcon" src="icon/page.png"> <b class="name_">' + entity.name + '</b>');
        tab.append('<img title="Delete page \'' + entity.name + '\'" alt="Delete page \'' + entity.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
        tab.append('<img class="view_icon button" title="View ' + entity.name + ' in new window" alt="View ' + entity.name + ' in new window" src="icon/eye.png">');

        $('.view_icon', tab).on('click', function(e) {
            e.stopPropagation();
            var self = $(this);
            var name = $(self.parent().children('b.name_')[0]).text();
            window.open(viewRootUrl + name);
        });

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
            e.stopPropagation();
            var self = $(this);
            var clicks = e.originalEvent.detail;
            if (clicks == 1) {
                if (debug) console.log('click', self, self.css('z-index'));
                if (self.hasClass('active')) {
                    _Pages.makeTabEditable(self);
                } else {
                    _Pages.activateTab(self);
                }
            }
        });

        if (getId(element) == activeTab) {
            _Pages.activateTab(element);
        }
    },

    activateTab : function(element) {
        
        var name = $.trim(element.children('.name_').text());
        if (debug) console.log('activateTab', element, name);

        previewTabs.children('li').each(function() {
            $(this).removeClass('active');
        });

        $('.previewBox', previews).each(function() {
            $(this).hide();
        });
        //var id = $(this).prop('id').substring(5);

        var id = getId(element);
        activeTab = id;

        _Pages.reloadIframe(id, name);

        element.addClass('active');

        if (debug) console.log('set cookie for active tab', activeTab);
        $.cookie('structrActiveTab', activeTab, {
            expires: 7,
            path: '/'
        });

    },
    
    reloadIframe : function(id, name) {
        var iframe = $('#preview_' + id);
        if (debug) console.log(iframe);
        iframe.prop('src', viewRootUrl + name + '?edit');
        iframe.parent().show();
        iframe.on('load', function() {
            if (debug) console.log('iframe loaded', $(this));
        });
        _Pages.resize();

    },
    
    makeTabEditable : function(element) {
        //element.off('dblclick');
        element.off('hover');
        var oldName = $.trim(element.children('.name_').text());
        //console.log('oldName', oldName);
        element.children('b').hide();
        element.find('.button').hide();
        element.append('<input type="text" size="' + (oldName.length+4) + '" class="newName_" value="' + oldName + '">');

        var input = $('input', element);

        input.focus().select();

        input.on('blur', function() {
            if (debug) console.log('blur');
            var self = $(this);
            var newName = self.val();
            Command.setProperty(getId(element), "name", newName);
            _Pages.resetTab(element, newName);
        });
        
        input.keypress(function(e) {
            if (e.keyCode == 13 || e.keyCode == 9) {
                e.preventDefault(); 
                if (debug) console.log('keypress');
                var self = $(this);
                var newName = self.val();
                Command.setProperty(getId(element), "name", newName);
                _Pages.resetTab(element, newName);
            }
        });

        element.off('click');

    },

    appendPageElement : function(entity, hasChildren) {

        if (debug) console.log('appendPageElement', entity, hasChildren);

        //pages.append('<div id="_' + pages.children('.page').length + '" class="node page ' + entity.id + '_"></div>');
        pages.append('<div id="_' + entity.id + '" class="node page ' + entity.id + '_"></div>');
        var div = $('.' + entity.id + '_', pages);

        entity.pageId = entity.id;
        
        $('.button', div).on('mousedown', function(e) {
            e.stopPropagation();
        });

        div.append('<img class="typeIcon" src="icon/page.png">'
            + '<b class="name_">' + entity.name + '</b> <span class="id">' + entity.id + '</span>');

        _Entities.appendExpandIcon(div, entity, hasChildren);

        div.append('<img title="Delete page \'' + entity.name + '\'" alt="Delete page \'' + entity.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            //var self = $(this);
            //self.off('click');
            //self.off('mouseover');
            _Entities.deleteNode(this, entity);
        });

        div.append('<img title="Clone page \'' + entity.name + '\'" alt="Clone page \'' + entity.name + '\'" class="clone_icon button" src="' + _Pages.clone_icon + '">');
        $('.clone_icon', div).on('click', function(e) {
            e.stopPropagation();
            //var self = $(this);
            //self.off('click');
            //self.off('mouseover');
            Command.clonePage(entity.id);
        });

        _Entities.appendEditPropertiesIcon(div, entity);
        _Entities.appendAccessControlIcon(div, entity);
        _Entities.setMouseOver(div);

        var tab = _Pages.addTab(entity);

        previews.append('<div class="previewBox"><iframe id="preview_'
            + entity.id + '"></iframe></div><div style="clear: both"></div>');

        _Pages.resetTab(tab, entity.name);

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
            //var iframeWindow = this.contentWindow;

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
                        var page = self.closest( '.page')[0];
                        var pageId;
                        var pos;
                        var nodeData = {};
                        
//                        console.log('drop event', self, page);
                        
                        if (page) {

                            // we're in the main page
                            pageId = getId(page);
                            pos = $('.content, .element', self).length;

                        } else {
                            
                            // we're in the iframe
                            page = self.closest('[structr_page_id]')[0];
                            pageId = $(page).attr('structr_page_id');
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
                        
                        if (pageId) {
                            //relData.pageId = pageId;
                            relData[pageId] = pos;
                        } else {
                            relData['*'] = pos;
                        }

                        nodeData.tag = (tag != 'content' ? tag : '');
                        nodeData.id = contentId;
                        if (debug) console.log(relData);
                        // suppress dropping anything in prview iframes for now
                        console.log('suppressed command', 'Command.createAndAdd(', elementId, nodeData, relData, ')');
                        // Command.createAndAdd(elementId, nodeData, relData);
                    }
                });

                var structrId = el.attr('structr_element_id');
                //var type = el.prop('structr_type');
                //  var name = el.prop('structr_name');
                var tag  = element.nodeName.toLowerCase();
                if (structrId) {

                    $('.move_icon', el).on('mousedown', function(e) {
                        e.stopPropagation();
                        var self = $(this);
                        var element = self.closest('[structr_element_id]');
                        //var element = self.children('.structr-node');
                        if (debug) console.log(element);
                        var entity = Structr.entity(structrId, element.prop('structr_element_id'));
                        entity.type = element.prop('structr_type');
                        entity.name = element.prop('structr_name');
                        if (debug) console.log('move', entity);
                        //var parentId = element.prop('structr_element_id');
                        self.parent().children('.structr-node').show();
                    });

                    $('b', el).on('click', function(e) {
                        e.stopPropagation();
                        var self = $(this);
                        var element = self.closest('[structr_element_id]');
                        var entity = Structr.entity(structrId, element.prop('structr_element_id'));
                        entity.type = element.prop('structr_type');
                        entity.name = element.prop('structr_name');
                        if (debug) console.log('edit', entity);
                        //var parentId = element.prop('structr_element_id');
                        if (debug) console.log(element);
                        Structr.dialog('Edit Properties of ' + entity.id, function() {
                            if (debug) console.log('save')
                        }, function() {
                            if (debug) console.log('cancelled')
                        });
                        _Entities.showProperties(entity);
                    });

                    $('.delete_icon', el).on('click', function(e) {
                        e.stopPropagation();
                        var self = $(this);
                        var element = self.closest('[structr_element_id]');
                        var entity = Structr.entity(structrId, element.prop('structr_element_id'));
                        entity.type = element.prop('structr_type');
                        entity.name = element.prop('structr_name');
                        if (debug) console.log('delete', entity);
                        var parentId = element.prop('structr_element_id');

                        Command.removeSourceFromTarget(entity.id, parentId);
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

                            //                            self.parent().children('.structr-element-container-header').remove();
                            //
                            //                            self.append('<div class="structr-element-container-header">'
                            //                                + '<img class="typeIcon" src="/structr/'+ _Elements.icon + '">'
                            //                                + '<b class="name_">' + name + '</b> <span class="id">' + structrId + '</b>'
                            //                                + '<img class="delete_icon structr-container-button" title="Delete ' + structrId + '" alt="Delete ' + structrId + '" src="/structr/icon/delete.png">'
                            //                                + '<img class="edit_icon structr-container-button" title="Edit properties of ' + structrId + '" alt="Edit properties of ' + structrId + '" src="/structr/icon/application_view_detail.png">'
                            //                                + '<img class="move_icon structr-container-button" title="Move ' + structrId + '" alt="Move ' + structrId + '" src="/structr/icon/arrow_move.png">'
                            //                                + '</div>'
                            //                                );

                            var nodes = Structr.node(structrId);
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
                            var nodes = Structr.node(structrId);
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
                            self.prop('contenteditable', true);
                            //$('#hoverStatus').text('Editable content element: ' + self.attr('structr_content_id'));
                            var nodes = Structr.node(structrId);
                            nodes.parent().removeClass('nodeHover');
                            nodes.addClass('nodeHover');
                        },
                        mouseout: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            //swapFgBg(self);
                            self.removeClass('structr-editable-area');
                            //self.prop('contenteditable', false);
                            //$('#hoverStatus').text('-- non-editable --');
                            var nodes = Structr.node(structrId);
                            nodes.removeClass('nodeHover');
                        },
                        click: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            self.removeClass('structr-editable-area');
                            self.addClass('structr-editable-area-active');

                            // Store old text in global var
                            textBeforeEditing = cleanText(self.contents());
                            if (debug) console.log("textBeforeEditing", textBeforeEditing);

                        },
                        blur: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            contentSourceId = self.attr('structr_content_id');
                            var text = cleanText(self.contents());
                            if (debug) console.log('blur contentSourceId: ' + contentSourceId);
                            //_Pages.updateContent(contentSourceId, textBeforeEditing, self.contents().first().text());
                            //Command.patch(contentSourceId, textBeforeEditing, self.contents().first().text());
                            Command.patch(contentSourceId, textBeforeEditing, text);
                            contentSourceId = null;
                            self.attr('contenteditable', false);
                            self.removeClass('structr-editable-area-active');
                            _Pages.reloadPreviews();

                        }
                    });
				
                }
            });

        });

        return div;
	
    },

    appendElementElement : function(entity, parentId, componentId, pageId, add, hasChildren, treeAddress) {
        if (debug) console.log('_Pages.appendElementElement', entity, parentId, componentId, pageId, add, hasChildren, treeAddress);
        
        var div;
        if (entity.type == 'Component') {
            div = _Components.appendComponentElement(entity, parentId, componentId, pageId, add, hasChildren, treeAddress);
        } else {
            div = _Elements.appendElementElement(entity, parentId, componentId, pageId, add, hasChildren, treeAddress);
        }
        
        if (!div) return false;

        if (parentId) {

            $('.delete_icon', div).replaceWith('<img title="Remove ' + entity.type + ' \'' + entity.name + '\' from parent ' + parentId + '" '
                + 'alt="Remove ' + entity.type + ' ' + entity.name + ' from ' + parentId + '" class="delete_icon button" src="icon/brick_delete.png">');
            
            $('.button', div).on('mousedown', function(e) {
                e.stopPropagation();
            });
            
            $('.delete_icon', div).on('click', function(e) {
                
                e.stopPropagation();
                var self = $(this);
                
//                if (debug) console.log('appendElementElement div', div);
//                var pos = self.parent().parent().children('.node').index(self.parent());
//                
//                var pageId = getId(self.closest('.page')[0]);
//                console.log('Command.removeSourceFromTarget(',entity.id, parentId, componentId, pageId, pos,')');
//                
//                Command.removeSourceFromTarget(entity.id, parentId, componentId, pageId, pos);

                _Entities.listContainingNodes(entity, div);
                
            });
        }

        _Entities.setMouseOver(div);

        var page = div.closest( '.page')[0];
        if (!page && pages) {
            div.draggable({
                revert: 'invalid',
                containment: '#pages',
                zIndex: 4,
                helper: 'clone',
                start: function(event, ui) {
                    $(this).draggable(disable);
                }
            });
        }

        var sorting = false;
        var obj = {};
        obj.command = 'SORT';

        div.sortable({
            sortable: '.node',
            containment: '#pages',
            start: function(event, ui) {
                sorting = true;
                var pageId = getId(ui.item.closest('.page')[0]);
                obj.id = pageId;
            },
            update: function(event, ui) {
                if (debug) console.log('---------')
                if (debug) console.log(pageId);
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
                _Pages.reloadPreviews();
            },
            stop: function(event, ui) {
                sorting = false;
                _Entities.resetMouseOverState(ui.item);
            }
        });

        div.droppable({
            accept: '.element, .content, .component, .image, .file',
            greedy: true,
            hoverClass: 'nodeHover',
            tolerance: 'pointer',
            drop: function(event, ui) {
                var self = $(this);

                if (debug) console.log('dropped', event, ui.draggable);
                
                treeAddress = getElementPath(self);
                if (debug) console.log('treeAddress', treeAddress);
                //addExpandedNode(treeAddress);
                _Entities.ensureExpanded(self);
                
                if (sorting) {
                    if (debug) console.log('sorting, no drop allowed');
                    return;
                }
                var nodeData = {};
                var pageId;
                var relData = {};
				
                var page = self.closest('.page')[0];

                if (debug) console.log(page);
                var contentId = getId(ui.draggable);
                var elementId = getId(self);
                
                if (debug) console.log('contentId', contentId);
                if (debug) console.log('elementId', elementId);

                if (contentId == elementId) {
                    if (debug) console.log('drop on self not allowed');
                    return;
                }
                
                var tag, name;
                var cls = Structr.getClass($(ui.draggable));
                
                if (cls == 'image') {
                    contentId = undefined;
                    name = $(ui.draggable).find('.name_').text();
                    if (debug) console.log('Image dropped, creating <img> node', name);
                    nodeData._html_src = name;
                    nodeData.name = name;
                    tag = 'img';
                    
                    Structr.modules['files'].unload();
                    _Pages.makeMenuDroppable();
                    
                } else if (cls == 'file') {
                    name = $(ui.draggable).children('.name_').text();
                    
                    var parentTag = self.children('.tag_').text();
                    if (debug) console.log(parentTag);
                    nodeData.linkable_id = contentId;
                    
                    if (parentTag == 'head') {
                        
                        if (debug) console.log('File dropped in <head>');
                        
                        if (name.endsWith('.css')) {
                            
                            if (debug) console.log('CSS file dropped in <head>, creating <link>');
                            
                            tag = 'link';
                            nodeData._html_href = '/${link.name}';
                            nodeData._html_type = 'text/css';
                            nodeData._html_rel = 'stylesheet';
                            nodeData._html_media = 'screen';
                            
                        } else if (name.endsWith('.js')) {
                            
                            if (debug) console.log('JS file dropped in <head>, creating <script>');
                            
                            tag = 'script';
                            nodeData._html_src = '/${link.name}';
                            nodeData._html_type = 'text/javascript';
                        }
                        
                    } else {
                    
                        if (debug) console.log('File dropped, creating <a> node', name);
                        nodeData._html_href = '/${link.name}';
                        nodeData._html_title = '${link.name}';
                        nodeData.linkable_id = contentId;
                        nodeData.childContent = '${parent.link.name}';
                        tag = 'a';
                    }
                    contentId = undefined;
                    
                    Structr.modules['files'].unload();
                    _Pages.makeMenuDroppable();

                } else {               
                    if (!contentId) {
                        tag = $(ui.draggable).text();

                        if (tag == 'p' || tag == 'h1' || tag == 'h2' || tag == 'h3' || tag == 'h4' || tag == 'h5' || tag == 'h5' || tag == 'li' || tag == 'em' || tag == 'title' || tag == 'b' || tag == 'span' || tag == 'th' || tag == 'td') {
                            nodeData.childContent = 'Initial Content for ' + tag;
                            
                            var pos = self.children('.node').length;
                            if (debug) console.log('new nested child', treeAddress + '_' + pos);
                            // set as expanded in advance
                            addExpandedNode(treeAddress + '_' + pos);
                            
                        }
                        
                        
                    } else {
                        tag = cls;
                    }
                }
                
                if (debug) console.log($(ui.draggable));
                var p = Structr.numberOfNodes(self, contentId);
                if (debug) console.log(p);

                if (page) {
                    pageId = getId(page);
                    //relData.pageId = pageId;
                    relData[pageId] = p;
                } else {
                    relData['*'] = p;
                }
				
                //if (!isExpanded(treeAddress)) {
                //    _Entities.toggleElement(self);
                //}

//                var component = self.closest( '.component')[0];
//                if (component) {
//                    var componentId = getId(component);
//                    relData.componentId = componentId;
//                    relData[componentId] = pos;
//                }

                nodeData.tag = (tag != 'content' ? tag : '');
                nodeData.type = tag.capitalize();
                nodeData.id = contentId;
                nodeData.targetPageId = pageId;

                var sourcePage = ui.draggable.closest('.page')[0];
                if (sourcePage) {
                    var sourcePageId = getId(sourcePage);
                    nodeData.sourcePageId = sourcePageId;
                }

                if (debug) console.log('drop event in appendElementElement', elementId, nodeData, relData);
                Command.createAndAdd(elementId, nodeData, relData);
            }
        });

        return div;
    },

    appendContentElement : function(content, parentId, componentId, pageId, treeAdress) {
        if (debug) console.log('Pages.appendContentElement', content, parentId, componentId, pageId, treeAdress);
		
        var div = _Contents.appendContentElement(content, parentId, componentId, pageId, treeAdress);
        if (!div) return false;

        if (debug) console.log('appendContentElement div', div);
        var pos = Structr.numberOfNodes($(div).parent())-1;
        if (debug) console.log('pos', content.id, pos);

        //var div = Structr.node(content.id, parentId, componentId, pageId, pos);

        if (parentId) {
            
            $('.button', div).on('mousedown', function(e) {
                e.stopPropagation();
            });
            
            $('.delete_icon', div).replaceWith('<img title="Remove content \'' + content.name + '\' from parent ' + parentId + '" '
                + 'alt="Remove content ' + content.name + ' from element ' + parentId + '" class="delete_icon button" src="' + _Contents.delete_icon + '">');
            $('.delete_icon', div).on('click', function(e) {
                e.stopPropagation();
//                var self = $(this);
//                //self.off('click');
//                //self.off('mouseover');
//                var component = self.closest( '.component')[0];
//                var page = self.closest( '.page')[0];
//                if (debug) console.log('Command.removeSourceFromTarget', content.id, parentId, getId(component), getId(page), pos);
//                Command.removeSourceFromTarget(content.id, parentId, getId(component), getId(page), pos)
            //                if (debug) console.log('Command.removeSourceFromTarget', content.id, parentId, componentId, pageId, pos);
            //                Command.removeSourceFromTarget(content.id, parentId, componentId, pageId, pos)
                _Entities.listContainingNodes(content, div);
            });
        }

        _Entities.setMouseOver(div);

        div.draggable({
            iframeFix: true,
            revert: 'invalid',
            containment: '#pages',
            zIndex: 1,
            helper: 'clone'
        });
        return div;
    },

    removeComponentFromPage : function(entityId, parentId, componentId, pageId, pos) {
        if (debug) console.log('Pages.removeComponentFromPage');

        var page = Structr.node(pageId);
        var component = Structr.node(entityId, componentId, componentId, pageId, pos);
        component.remove();
        
        if (!Structr.containsNodes(page)) {
            _Entities.removeExpandIcon(page);
        }
        var numberOfComponents = $('.component', page).size();
        if (debug) console.log(numberOfComponents);
        if (numberOfComponents == 0) {
            enable($('.delete_icon', page)[0]);
        }
    //Command.removeSourceFromTarget(entityId, parentId, componentId, pageId, pos);

    },

    removeFrom : function(entityId, parentId, componentId, pageId, pos) {
        if (debug) console.log('Pages.removeFrom', entityId, parentId, componentId, pageId, pos);

        var parent = Structr.node(parentId, null, componentId, pageId);
        var element = Structr.node(entityId, parentId, componentId, pageId, pos);

        //var parent = $(element).parent();

        if (debug) console.log('parent', parent);
        
        element.remove();

        if (!Structr.containsNodes(parent)) {
            _Entities.removeExpandIcon(parent);
        //enable('.delete_icon', parent);
        }


    },

    showSubEntities : function(pageId, entity) {
        var headers = {
            'X-StructrSessionToken' : token
        };
        var follow = followIds(pageId, entity);
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
                        _Pages.appendElement(result, entity, pageId);
                        _Pages.showSubEntities(pageId, result);
                    }
                });
            }
        });
    },

    reloadPreviews : function() {

        $('iframe', $('#previews')).each(function() {
            var self = $(this);
            var pageId = self.prop('id').substring('preview_'.length);

            if (pageId == activeTab) {
                var name = $(Structr.node(pageId)[0]).children('b.name_').text();
                var doc = this.contentDocument;
                doc.location = name;
                doc.location.reload(true);
            }
            
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
    
    makeMenuDroppable : function() {
        
        $('#pages_').removeClass('nodeHover').droppable('enable');

        $('#pages_').droppable({
            accept: '.element, .content, .component, .file, .image',
            greedy: true,
            hoverClass: 'nodeHover',
            tolerance: 'pointer',
        
            over: function(e, ui) {
            
                e.stopPropagation();
                $('#pages_').droppable('disable');
                console.log('over is off');
            
                Structr.activateMenuEntry('pages');
                document.location = '/structr/#pages'
                Structr.modules['pages'].onload();
            
                _Pages.resize();
            }
        
        });
        
    }

};
