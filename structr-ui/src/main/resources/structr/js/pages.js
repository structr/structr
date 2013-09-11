/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
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

var pages, shadowPage;
var previews, previewTabs, controls, palette, activeTab, activeTabLeft, activeTabRight, components, elements, widgetsSlideout, pagesSlideOut;
var selStart, selEnd;
var sel;
var contentSourceId, elementSourceId, rootId;
var textBeforeEditing;
var activeTabKey = 'structrActiveTab_' + port;
var activeTabRightKey = 'structrActiveTabRight_' + port;
var activeTabLeftKey = 'structrActiveTabLeft_' + port;
var win = $(window);

$(document).ready(function() {
    Structr.registerModule('pages', _Pages);
    Structr.classes.push('page');

    win.resize(function() {
        _Pages.resize();
    });

    _Pages.makeMenuDroppable();
});

var _Pages = {
    icon: 'icon/page.png',
    add_icon: 'icon/page_add.png',
    delete_icon: 'icon/page_delete.png',
    clone_icon: 'icon/page_copy.png',
    init: function() {

        Structr.initPager('Page', 1, 25);
        Structr.initPager('File', 1, 25);

    },
    resize: function(offsetLeft, offsetRight) {

        var windowWidth = win.width(), windowHeight = win.height();
        var headerOffsetHeight = 100,  previewOffset = 22;

        if (pages && palette) {

            pages.css({
                height: windowHeight - headerOffsetHeight - previewOffset + 'px'
            })

            if (previews) {

                if (offsetLeft) {
                    previews.css({
                        marginLeft: '+=' + offsetLeft + 'px'
                    });
                }

                if (offsetRight) {
                    previews.css({
                        marginRight: '+=' + offsetRight + 'px'
                    });
                }

                //console.log(offsetLeft, offsetRight, windowWidth, parseInt(previews.css('marginLeft')), parseInt(previews.css('marginRight')));
                var w = windowWidth - parseInt(previews.css('marginLeft')) - parseInt(previews.css('marginRight')) -15 + 'px';

                previews.css({
                    width: w,
                    height: windowHeight - headerOffsetHeight + 'px'
                });

                $('.previewBox', previews).css({
                    //width: w,
                    height: windowHeight - (headerOffsetHeight + previewOffset) + 'px'
                });

                var iframes = $('.previewBox', previews).find('iframe');
                iframes.css({
                    width: $('.previewBox', previews).width() + 'px',
                    height: windowHeight - (headerOffsetHeight + previewOffset) + 'px'
                });
            }
        }

    },
    onload: function() {

        _Pages.init();

        activeTab = localStorage.getItem(activeTabKey);
        activeTabLeft = localStorage.getItem(activeTabLeftKey);
        activeTabRight = localStorage.getItem(activeTabRightKey);
        log('value read from local storage', activeTab);

        log('onload');

        main.prepend('<div id="pages" class="slideOut slideOutLeft"><div class="compTab" id="pagesTab">Pages Tree View</div></div><div id="previews"></div>'
                + '<div id="widgetsSlideout" class="slideOut slideOutRight"><div class="compTab" id="widgetsTab">Widgets</div></div>'
                + '<div id="palette" class="slideOut slideOutRight"><div class="compTab" id="paletteTab">HTML Palette</div></div>'
                + '<div id="components" class="slideOut slideOutRight"><div class="compTab" id="componentsTab">Reused Components</div></div>'
                + '<div id="elements" class="slideOut slideOutRight"><div class="compTab" id="elementsTab">Orphaned Elements</div></div>');

        pagesSlideOut = $('#pages');
        previews = $('#previews');
        widgetsSlideout = $('#widgetsSlideout');
        palette = $('#palette');
        components = $('#components');
        elements = $('#elements');

        $('#pagesTab').on('click', function() {
            console.log('click on pagesTab')
            if (pagesSlideOut.position().left === -412) {
                _Pages.openLeftSlideOut(pagesSlideOut, this);
            } else {
                _Pages.closeLeftSlideOuts([pagesSlideOut]);
            }
        }).droppable({
            tolerance: 'touch',
            over: function(e, ui) {
                if (pagesSlideOut.position().left === -412) {
                    _Pages.openLeftSlideOut(pagesSlideOut, this);
                } else {
                    _Pages.closeLeftSlideOuts([pagesSlideOut]);
                }
            }
        });

        $('#widgetsTab').on('click', function() {
            if (widgetsSlideout.position().left + 1 === $(window).width()) {
                _Pages.closeSlideOuts([palette, components, elements]);
                _Pages.openSlideOut(widgetsSlideout, this, function() {
                    _Elements.reloadWidgets();
                });
            } else {
                _Pages.closeSlideOuts([widgetsSlideout]);
            }
        });

        $('#paletteTab').on('click', function() {
            if (palette.position().left + 1 === $(window).width()) {
                _Pages.closeSlideOuts([widgetsSlideout, components, elements]);
                _Pages.openSlideOut(palette, this, function() {
                    _Elements.reloadPalette();
                });
            } else {
                _Pages.closeSlideOuts([palette]);
            }
        });

        $('#componentsTab').on('click', function() {
            if (components.position().left + 1 === $(window).width()) {
                _Pages.closeSlideOuts([widgetsSlideout, palette, elements]);
                _Pages.openSlideOut(components, this, function() {
                    _Elements.reloadComponents();
                });
            } else {
                _Pages.closeSlideOuts([components]);
            }
        }).droppable({
            tolerance: 'touch',
            over: function(e, ui) {
                if (components.position().left + 1 === $(window).width()) {
                    _Pages.closeSlideOuts([widgetsSlideout, palette, elements]);
                    _Pages.openSlideOut(components, this, function() {
                        _Elements.reloadComponents();
                    });
                }
            }
        });

        $('#elementsTab').on('click', function() {
            if (elements.position().left + 1 === $(window).width()) {
                $(this).addClass('active');
                _Pages.closeSlideOuts([widgetsSlideout, palette, components]);
                _Pages.openSlideOut(elements, this, function() {
                    _Elements.reloadUnattachedNodes();
                });
            } else {
                _Pages.closeSlideOuts([elements]);
            }

        }).droppable({
            over: function(e, ui) {
            }
        });

        $('#controls', main).remove();

        previews.append('<ul id="previewTabs"></ul>');
        previewTabs = $('#previewTabs', previews);

        _Pages.refresh();

        if (activeTabLeft) {
            $('#' + activeTabLeft).addClass('active').click();
        }

        if (activeTabRight) {
            $('#' + activeTabRight).addClass('active').click();
        }

        //window.setTimeout('_Pages.resize(0,0)', 100);

    },
    openSlideOut: function(slideout, tab, callback) {
        _Pages.resize(0, 425);
        var s = $(slideout);
        var t = $(tab);
        t.addClass('active');
        s.animate({right: '+=425px'}, {duration: 100}).zIndex(1);
        localStorage.setItem(activeTabRightKey, t.prop('id'));
        if (callback) {
            callback();
        }
    },
    closeSlideOuts: function(slideout) {
        var wasOpen = false;
        slideout.forEach(function(w) {
            var s = $(w);
            var l = s.position().left;
            if (l + 1 !== $(window).width()) {
                wasOpen = true;
                //console.log('closing open slide-out', s);
                s.animate({right: '-=425px'}, {duration: 100}).zIndex(2);
                $('.compTab.active', s).removeClass('active');
            }
        });
        if (wasOpen) {
            _Pages.resize(0, -425);
        }
            
        localStorage.removeItem(activeTabRightKey);
    },
    openLeftSlideOut: function(slideout, tab, callback) {
        _Pages.resize(412, 0);
        var s = $(slideout);
        var t = $(tab);
        t.addClass('active');
        s.animate({left: '+=412px'}, {duration: 100}).zIndex(1);
        localStorage.setItem(activeTabLeftKey, t.prop('id'));
        if (callback) {
            callback();
        }
    },
    closeLeftSlideOuts: function(slideout) {
        _Pages.resize(-412, 0);
        slideout.forEach(function(w) {
            var s = $(w);
            var l = s.position().left;
            if (l + 1 !== $(window).width()) {
                s.animate({left: '-=412px'}, {duration: 100}).zIndex(2);
                $('.compTab.active', s).removeClass('active');
            }
        });
        localStorage.removeItem(activeTabLeftKey);
    },
    clearPreviews: function() {

        if (previewTabs && previewTabs.length) {
            previewTabs.children('.page').remove();
        }

    },
    refresh: function() {

        pagesSlideOut.find(':not(.compTab)').remove();
        previewTabs.empty();

        pagesSlideOut.append('<div id="pagesTree"></div>')
        pages = $('#pagesTree', pagesSlideOut);

        Structr.addPager(pages, 'Page');

        previewTabs.append('<li id="import_page" class="button"><img class="add_button icon" src="icon/page_white_put.png"></li>');
        $('#import_page', previewTabs).on('click', function(e) {
            e.stopPropagation();

            Structr.dialog('Import Page', function() {
                return true;
            }, function() {
                return true;
            });

            dialog.empty();
            dialogMsg.empty();

            dialog.append('<h3>Create page from source code ...</h3>'
                    + '<textarea id="_code" name="code" cols="40" rows="10" placeholder="Paste HTML code here"></textarea>');

            dialog.append('<h3>... or fetch page from URL: <input id="_address" name="address" size="40" value="http://"></h3><table class="props">'
                    + '<tr><td><label for="name">Name of new page:</label></td><td><input id="_name" name="name" size="20"></td></tr>'
                    + '<tr><td><label for="timeout">Timeout (ms)</label></td><td><input id="_timeout" name="timeout" size="20" value="5000"></td></tr>'
                    + '<tr><td><label for="publicVisibilty">Visible to public</label></td><td><input type="checkbox" id="_publicVisible" name="publicVisibility"></td></tr>'
                    + '<tr><td><label for="authVisibilty">Visible to authenticated users</label></td><td><input type="checkbox" checked="checked" id="_authVisible" name="authVisibilty"></td></tr>'
                    + '</table>');

            var addressField = $('#_address', dialog);

            log('addressField', addressField);

            addressField.on('blur', function() {
                var addr = $(this).val().replace(/\/+$/, "");
                log(addr);
                $('#_name', dialog).val(addr.substring(addr.lastIndexOf("/") + 1));
            });


            dialog.append('<button id="startImport">Start Import</button>');

            $('#startImport').on('click', function(e) {
                e.stopPropagation();

                var code = $('#_code', dialog).val();
                var address = $('#_address', dialog).val();
                var name = $('#_name', dialog).val();
                var timeout = $('#_timeout', dialog).val();
                var publicVisible = $('#_publicVisible:checked', dialog).val() === 'on';
                var authVisible = $('#_authVisible:checked', dialog).val() === 'on';

                log('start');
                return Command.importPage(code, address, name, timeout, publicVisible, authVisible);
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
        
        //_Pages.resize(0,0)

    },
    addTab: function(entity) {
        previewTabs.children().last().before('<li id="show_' + entity.id + '" class="page ' + entity.id + '_"></li>');

        var tab = $('#show_' + entity.id, previews);

        tab.append('<img class="typeIcon" src="icon/page.png"> <b title="' + entity.name + '" class="name_">' + fitStringToSize(entity.name, 200) + '</b>');
        tab.append('<img title="Delete page \'' + entity.name + '\'" alt="Delete page \'' + entity.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
        tab.append('<img class="view_icon button" title="View ' + entity.name + ' in new window" alt="View ' + entity.name + ' in new window" src="icon/eye.png">');

        $('.view_icon', tab).on('click', function(e) {
            e.stopPropagation();
            var self = $(this);
            //var name = $(self.parent().children('b.name_')[0]).text();
            var link = $.trim(self.parent().children('b.name_').attr('title'));
            window.open(viewRootUrl + link);
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

        return tab;
    },
    resetTab: function(element) {

        log('resetTab', element);

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
            if (clicks === 1) {
                log('click', self, self.css('z-index'));
                if (self.hasClass('active')) {
                    _Pages.makeTabEditable(self);
                } else {
                    _Pages.activateTab(self);
                }
            }
        });

        if (element.prop('id').substring(5) === activeTab) {
            _Pages.activateTab(element);
        }
    },
    activateTab: function(element) {

        //var name = $.trim(element.children('.name_').text());
        var name = $.trim(element.children('b.name_').attr('title'));
        log('activateTab', element, name);

        previewTabs.children('li').each(function() {
            $(this).removeClass('active');
        });

        $('.previewBox', previews).each(function() {
            $(this).hide();
        });

        var id = element.prop('id').substring(5);
        activeTab = id;

        _Pages.reloadIframe(id, name);

        element.addClass('active');

        log('store active tab', activeTab);
        localStorage.setItem(activeTabKey, activeTab);

    },
    reloadIframe: function(id, name) {
        var iframe = $('#preview_' + id);
        log(iframe);
        iframe.prop('src', viewRootUrl + name + '?edit=2');
        iframe.parent().show();
        iframe.on('load', function() {
            log('iframe loaded', $(this));
        });
        _Pages.resize();

    },
    makeTabEditable: function(element) {
        //element.off('dblclick');

        var id = element.prop('id').substring(5);

        element.off('hover');
        //var oldName = $.trim(element.children('.name_').text());
        var oldName = $.trim(element.children('b.name_').attr('title'));
        element.children('b').hide();
        element.find('.button').hide();
        var input = $('input.newName_', element);

        if (!input.length) {
            element.append('<input type="text" size="' + (oldName.length + 4) + '" class="newName_" value="' + oldName + '">');
            input = $('input', element);
        }

        input.show().focus().select();

        input.on('blur', function() {
            log('blur');
            var self = $(this);
            var newName = self.val();
            Command.setProperty(id, "name", newName);
            _Pages.resetTab(element, newName);
        });

        input.keypress(function(e) {
            if (e.keyCode === 13 || e.keyCode === 9) {
                e.preventDefault();
                log('keypress');
                var self = $(this);
                var newName = self.val();
                Command.setProperty(id, "name", newName);
                _Pages.resetTab(element, newName);
            }
        });

        element.off('click');

    },
    appendPageElement: function(entity) {

        var hasChildren = entity.childElements.length;

        pages.append('<div id="id_' + entity.id + '" class="node page"></div>');
        var div = Structr.node(entity.id);

        $('.button', div).on('mousedown', function(e) {
            e.stopPropagation();
        });

        div.append('<img class="typeIcon" src="icon/page.png">'
                + '<b title="' + entity.name + '" class="name_">' + fitStringToSize(entity.name, 200) + '</b> <span class="id">' + entity.id + '</span>');

        _Entities.appendExpandIcon(div, entity, hasChildren);
        _Entities.appendAccessControlIcon(div, entity);

        div.append('<img title="Delete page \'' + entity.name + '\'" alt="Delete page \'' + entity.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            _Entities.deleteNode(this, entity);
        });

        _Entities.appendEditPropertiesIcon(div, entity);
        _Entities.setMouseOver(div);

        var tab = _Pages.addTab(entity);

        previews.append('<div class="previewBox"><iframe id="preview_'
                + entity.id + '"></iframe></div><div style="clear: both"></div>');

        _Pages.resetTab(tab, entity.name);

        $('#preview_' + entity.id).hover(function() {
            var self = $(this);
            var elementContainer = self.contents().find('.structr-element-container');
            elementContainer.addClass('structr-element-container-active');
            elementContainer.removeClass('structr-element-container');
        }, function() {
            var self = $(this);
            var elementContainer = self.contents().find('.structr-element-container-active');
            elementContainer.addClass('structr-element-container');
            elementContainer.removeClass('structr-element-container-active');
            //self.find('.structr-element-container-header').remove();
        });

        $('#preview_' + entity.id).load(function() {

            //var offset = $(this).offset();

            var doc = $(this).contents();
            var head = $(doc).find('head');
            if (head)
                head.append('<style media="screen" type="text/css">'
                        + '* { z-index: 0}\n'
                        + '.nodeHover { border: 1px dotted red; }\n'
                        //+ '.structr-content-container { display: inline-block; border: none; margin: 0; padding: 0; min-height: 10px; min-width: 10px; }\n'
                        + '.structr-content-container { min-height: .25em; min-width: .25em; }\n'
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

            var droppables = iframeDocument.find('[data-structr-el]');

            if (droppables.length === 0) {

                //iframeDocument.append('<html structr_element_id="' + entity.id + '">dummy element</html>');
                var html = iframeDocument.find('html');
                html.attr('data-structr-el', entity.id);
                html.addClass('structr-element-container');

            }
            droppables = iframeDocument.find('[data-structr-el]');

            droppables.each(function(i, element) {
                var el = $(element);

                el.droppable({
                    accept: '.element, .content, .component',
                    greedy: true,
                    hoverClass: 'structr-droppable-area',
// this requires a patched jQuery, which we won't do anymore
// TODO: Find a better solution for dropping elements in the iframe at the right place                    
//                    iframeOffset: { 
//                        'top' : offset.top,
//                        'left' : offset.left
//                    },
                    drop: function(event, ui) {

                        var self = $(this);
                        var page = self.closest('.page')[0];
                        var pageId;
                        var pos;

                        if (page) {

                            // we're in the main page
                            pageId = getId(page);
                            pos = $('.content, .element', self).length;

                            console.log('drop in main page (parent)');

                        } else {

                            // we're in the iframe
                            page = self.closest('[data-structr-page]')[0];
                            pageId = $(page).attr('data-structr-page');
                            pos = $('[data-structr-el]', self).length;

                            console.log('drop in iframe', page, pageId, pos);

                        }

                        var contentId = getId(ui.draggable);
                        var elementId = getId(self);

                        if (!elementId)
                            elementId = self.attr('data-structr-el');

                        console.log('contentId (draggable)', contentId, ', pageId', pageId, ', elementId', elementId);
                        if (!contentId) {
                            tag = $(ui.draggable).text();
                            console.log(tag)
                            Command.createAndAppendDOMNode(pageId, elementId, (tag !== 'content' ? tag : ''), {});
                            return;
                        } else {
                            var baseUrl = 'http://' + remoteHost + ':' + remotePort;

                            var obj = StructrModel.obj(contentId);

                            console.log('widget?', obj.type === 'Widget');

                            if (obj.type === 'Widget') {

                                var source = obj.source;
                                console.log('append widget', source, elementId, pageId, baseUrl);
                                Command.appendWidget(source, elementId, pageId, baseUrl);
                                return;

                            } else {

                                // TODO: handle re-used or orphanded element


                            }
                        }
                    }

                });

                var structrId = el.attr('data-structr-el');
                //var type = el.prop('structr_type');
                //  var name = el.prop('structr_name');
//                var tag  = element.nodeName.toLowerCase();
                if (structrId) {

                    $('.move_icon', el).on('mousedown', function(e) {
                        e.stopPropagation();
                        var self = $(this);
                        var element = self.closest('[data-structr-el]');
                        //var element = self.children('.structr-node');
                        log(element);
                        var entity = Structr.entity(structrId, element.prop('data-structr-el'));
                        entity.type = element.prop('data-structr_type');
                        entity.name = element.prop('data-structr_name');
                        log('move', entity);
                        //var parentId = element.prop('structr_element_id');
                        self.parent().children('.structr-node').show();
                    });

                    //                    $('b', el).on('click', function(e) {
                    //                        e.stopPropagation();
                    //                        var self = $(this);
                    //                        var element = self.closest('[data-structr_element_id]');
                    //                        var entity = Structr.entity(structrId, element.prop('structr_element_id'));
                    //                        entity.type = element.prop('structr_type');
                    //                        entity.name = element.prop('structr_name');
                    //                        log('edit', entity);
                    //                        //var parentId = element.prop('structr_element_id');
                    //                        log(element);
                    ////                        Structr.dialog('Edit Properties of ' + entity.id, function() {
                    ////                            log('save')
                    ////                        }, function() {
                    ////                            log('cancelled')
                    ////                        });
                    //                        _Entities.showProperties(entity);
                    //                    });

                    $('.delete_icon', el).on('click', function(e) {
                        e.stopPropagation();
                        var self = $(this);
                        var element = self.closest('[data-structr-el]');
                        var entity = Structr.entity(structrId, element.prop('data-structr-el'));
                        entity.type = element.prop('data-structr_type');
                        entity.name = element.prop('data-structr_name');
                        log('delete', entity);
                        var parentId = element.prop('data-structr-el');

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

                            var node = Structr.node(structrId);
                            if (node) {
                                node.parent().removeClass('nodeHover');
                                node.addClass('nodeHover');
                            }

                            var pos = self.position();
                            var header = self.children('.structr-element-container-header');
                            header.css({
                                position: "absolute",
                                top: pos.top + offsetTop + 'px',
                                left: pos.left + offsetLeft + 'px',
                                cursor: 'pointer'
                            }).show();
                            log(header);
                        },
                        mouseout: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            self.removeClass('.structr-element-container');
                            var header = self.children('.structr-element-container-header');
                            header.remove();
                            var node = Structr.node(structrId);
                            if (node) {
                                node.removeClass('nodeHover');
                            }
                        }
                    });

                }
            });

            //$(this).contents().find('[data-structr-id]').each(function(i,element) {
            $(this).contents().find('*').each(function(i, element) {

                getComments(element).forEach(function(c) {

                    var inner = $(getNonCommentSiblings(c.textNode));
                    $(getNonCommentSiblings(c.textNode)).remove();
                    $(c.textNode).replaceWith('<div data-structr-id="' + c.id + '" data-structr-raw-content="' + c.rawContent + '">' + c.textNode.nodeValue + '</div>');

                    var el = $(element).children('[data-structr-id="' + c.id + '"]');

                    el.append(inner);

                    $(el).on({
                        mouseover: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            //self.replaceWith('<span data-structr-id="' + id + '">' + c.nodeValue + '</span>');

                            self.addClass('structr-editable-area');
                            //$('#hoverStatus').text('Editable content element: ' + self.attr('data-structr_content_id'));
                            var contentSourceId = self.attr('data-structr-id');
                            var node = Structr.node(contentSourceId);
                            if (node) {
                                node.parent().removeClass('nodeHover');
                                node.addClass('nodeHover');
                            }
                        },
                        mouseout: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            //swapFgBg(self);
                            self.removeClass('structr-editable-area');
                            //self.prop('contenteditable', false);
                            //$('#hoverStatus').text('-- non-editable --');
                            var contentSourceId = self.attr('data-structr-id');
                            var node = Structr.node(contentSourceId);
                            if (node) {
                                node.removeClass('nodeHover');
                            }
                        },
                        click: function(e) {
                            e.stopPropagation();
                            e.preventDefault();
                            var self = $(this);
                            if (self.hasClass('structr-editable-area-active')) {
                                return false;
                            }
                            self.removeClass('structr-editable-area').addClass('structr-editable-area-active').prop('contenteditable', true);

                            // Store old text in global var
                            textBeforeEditing = self.text();//cleanText(self.contents());

                            //var srcText = expandNewline(self.attr('data-structr-raw-content'));
                            var srcText = expandNewline(self.attr('data-structr-raw-content'));
                            // Replace only if it differs (e.g. for variables)
                            if (srcText !== textBeforeEditing) {
                                self.html(srcText);
                                textBeforeEditing = srcText;
                            }
                            return false;

                        },
                        blur: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            var contentSourceId = self.attr('data-structr-id');
                            var text = cleanText(self.html());
                            //Command.patch(contentSourceId, textBeforeEditing, text);
                            Command.setProperty(contentSourceId, 'content', text);
                            contentSourceId = null;
                            self.attr('contenteditable', false);
                            self.removeClass('structr-editable-area-active').removeClass('structr-editable-area');
                            _Pages.reloadPreviews();
                        }
                    });

                });

            });

        });

        div.droppable({
            accept: '#add_html, .html_element',
            greedy: true,
            hoverClass: 'nodeHover',
            tolerance: 'pointer',
            drop: function(event, ui) {
                var self = $(this);
                console.log('dropped onto', self);
                // Only html elements are allowed, and only if none exists

                if (getId(self) === getId(sortParent))
                    return false;

                _Entities.ensureExpanded(self);
                sorting = false;
                sortParent = undefined;

                var nodeData = {};

                var page = self.closest('.page')[0];

                var contentId = getId(ui.draggable);
                var elementId = getId(self);
                console.log('elementId', elementId);

                var source = StructrModel.obj(contentId);
                var target = StructrModel.obj(elementId);

                if (source && getId(page) && source.pageId && getId(page) !== source.pageId) {
                    event.preventDefault();
                    event.stopPropagation();
                    Command.copyDOMNode(source.id, target.id);
                    //_Entities.showSyncDialog(source, target);
                    _Elements.reloadComponents();
                    return;
                } else {
                    log('not copying node');
                }

                if (contentId === elementId) {
                    log('drop on self not allowed');
                    return;
                }

                var tag;
                var cls = Structr.getClass($(ui.draggable));

                if (!contentId) {
                    tag = $(ui.draggable).text();

                    if (tag !== 'html') {
                        return false;
                    }

                    var pageId = (page ? getId(page) : target.pageId);

                    Command.createAndAppendDOMNode(pageId, elementId, (tag !== 'content' ? tag : ''), nodeData);
                    return;

                } else {
                    tag = cls;
                    log('appendChild', contentId, elementId);
                    sorting = false;
                    Command.appendChild(contentId, elementId);
                    //$(ui.draggable).remove();

                    return;
                }
                log('drop event in appendPageElement', getId(page), getId(self), (tag !== 'content' ? tag : ''));
            }
        });

        return div;

    },
    appendElementElement: function(entity, refNode, refNodeIsParent) {
        log('_Pages.appendElementElement(', entity, refNode, refNodeIsParent, ');')
        var div = _Elements.appendElementElement(entity, refNode, refNodeIsParent);

        if (!div) {
            return false;
        }

        var parentId = entity.parent && entity.parent.id;
        if (parentId) {
            $('.delete_icon', div).replaceWith('<img title="Remove" '
                    + 'alt="Remove" class="delete_icon button" src="icon/brick_delete.png">');
            $('.button', div).on('mousedown', function(e) {
                e.stopPropagation();
            });
            $('.delete_icon', div).on('click', function(e) {
                e.stopPropagation();
                Command.removeChild(entity.id);
            });
        }

        _Dragndrop.makeDroppable(div);
        _Dragndrop.makeSortable(div);

        return div;
    },
    reloadPreviews: function() {

        // add a small delay to avoid getting old data in very fast localhost envs
        window.setTimeout(function() {

            $('iframe', $('#previews')).each(function() {
                var self = $(this);
                var pageId = self.prop('id').substring('preview_'.length);

                if (pageId === activeTab) {
                    var doc = this.contentDocument;
                    doc.location.reload(true);
                }

            });
        }, 100);
    },
    zoomPreviews: function(value) {
        $('.previewBox', previews).each(function() {
            var val = value / 100;
            var box = $(this);

            box.css('-moz-transform', 'scale(' + val + ')');
            box.css('-o-transform', 'scale(' + val + ')');
            box.css('-webkit-transform', 'scale(' + val + ')');

            var w = origWidth * val;
            var h = origHeight * val;

            box.width(w);
            box.height(h);

            $('iframe', box).width(w);
            $('iframe', box).height(h);

            log("box,w,h", box, w, h);

        });

    },
    makeMenuDroppable: function() {

        $('#pages_').droppable({
            accept: '.element, .content, .component, .file, .image, .widget',
            greedy: true,
            hoverClass: 'nodeHover',
            tolerance: 'pointer',
            over: function(e, ui) {

                e.stopPropagation();
                $('#pages_').droppable('disable');
                log('over is off');

                Structr.activateMenuEntry('pages');
                window.location.href = '/structr/#pages';

                if (files && files.length)
                    files.hide();
                if (folders && folders.length)
                    folders.hide();
                if (widgets && widgets.length)
                    widgets.hide();

//                _Pages.init();
                Structr.modules['pages'].onload();
                _Pages.resize();
            }

        });

        $('#pages_').removeClass('nodeHover').droppable('enable');
    }
};
