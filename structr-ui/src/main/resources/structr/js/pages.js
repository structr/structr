/*
 *  Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
var previews, previewTabs, controls, activeTab, activeTabLeft, activeTabRight, paletteSlideout, elementsSlideout, componentsSlideout, widgetsSlideout, pagesSlideout, activeElementsSlideout, dataBindingSlideout;
var lsw, rsw;
var components, elements;
var selStart, selEnd;
var sel;
var contentSourceId, elementSourceId, rootId;
var textBeforeEditing;
var activeTabKey = 'structrActiveTab_' + port;
var activeTabRightKey = 'structrActiveTabRight_' + port;
var activeTabLeftKey = 'structrActiveTabLeft_' + port;
var selectedTypeKey = 'structrSelectedType_' + port;

var win = $(window);

$(document).ready(function() {
    Structr.registerModule('pages', _Pages);
    Structr.classes.push('page');

    win.resize(function() {
        _Pages.resize();
    });

});

var _Pages = {
    icon: 'icon/page.png',
    add_icon: 'icon/page_add.png',
    delete_icon: 'icon/page_delete.png',
    clone_icon: 'icon/page_copy.png',
    autoRefresh: [],
    init: function() {

        Structr.initPager('Page', 1, 25);
        Structr.initPager('File', 1, 25);
        Structr.initPager('Folder', 1, 25);
        Structr.initPager('Image', 1, 25);

    },
    resize: function(offsetLeft, offsetRight) {

        $('body').css({
            position: 'fixed'
        });

        var windowWidth = win.width(), windowHeight = win.height();
        var headerOffsetHeight = 100, previewOffset = 22;

        $('.ver-scrollable').css({
            height: windowHeight - headerOffsetHeight + 'px'
        });

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
            var w = windowWidth - parseInt(previews.css('marginLeft')) - parseInt(previews.css('marginRight')) - 15 + 'px';

            previews.css({
                width: w,
                height: windowHeight - headerOffsetHeight + 'px'
            });

            $('.previewBox', previews).css({
                width: w,
                height: windowHeight - (headerOffsetHeight + previewOffset) + 'px'
            });

            var iframes = $('.previewBox', previews).find('iframe');
            iframes.css({
                width: w, //$('.previewBox', previews).width() + 'px',
                height: windowHeight - (headerOffsetHeight + previewOffset) + 'px'
            });
        }

    },
    onload: function() {

        _Pages.init();

        $('#main-help a').attr('href', 'http://docs.structr.org/frontend-user-guide#Pages');

        activeTab = localStorage.getItem(activeTabKey);
        activeTabLeft = localStorage.getItem(activeTabLeftKey);
        activeTabRight = localStorage.getItem(activeTabRightKey);
        log('value read from local storage', activeTab);

        log('onload');

        main.prepend(
                '<div id="pages" class="slideOut slideOutLeft"><div class="compTab" id="pagesTab">Pages Tree View</div></div>'
                + '<div id="activeElements" class="slideOut slideOutLeft"><div class="compTab" id="activeElementsTab">Active Elements</div><div class="page inner"></div></div>'
                + '<div id="dataBinding" class="slideOut slideOutLeft"><div class="compTab" id="dataBindingTab">Data Binding</div></div>'
                + '<div id="previews"></div>'
                + '<div id="widgetsSlideout" class="slideOut slideOutRight"><div class="compTab" id="widgetsTab">Widgets</div></div>'
                + '<div id="palette" class="slideOut slideOutRight"><div class="compTab" id="paletteTab">HTML Palette</div></div>'
                + '<div id="components" class="slideOut slideOutRight"><div class="compTab" id="componentsTab">Shared Components</div></div>'
                + '<div id="elements" class="slideOut slideOutRight"><div class="compTab" id="elementsTab">Unused Elements</div></div>');

        pagesSlideout = $('#pages');
        activeElementsSlideout = $('#activeElements');
        dataBindingSlideout = $('#dataBinding');

        previews = $('#previews');

        widgetsSlideout = $('#widgetsSlideout');
        paletteSlideout = $('#palette');
        componentsSlideout = $('#components');
        elementsSlideout = $('#elements');

        lsw = pagesSlideout.width() + 12;
        rsw = widgetsSlideout.width() + 12;

        $('#pagesTab').on('click', function() {
            if (pagesSlideout.position().left === -lsw) {
                Structr.closeLeftSlideOuts([activeElementsSlideout, dataBindingSlideout], activeTabLeftKey);
                Structr.openLeftSlideOut(pagesSlideout, this, activeTabLeftKey);
            } else {
                Structr.closeLeftSlideOuts([pagesSlideout], activeTabLeftKey);
            }
        }).droppable({
            tolerance: 'touch',
            over: function(e, ui) {
                if (pagesSlideout.position().left === -lsw) {
                    Structr.closeLeftSlideOuts([activeElementsSlideout, dataBindingSlideout], activeTabLeftKey);
                    Structr.openLeftSlideOut(pagesSlideout, this, activeTabLeftKey);
                } else {
                    Structr.closeLeftSlideOuts([pagesSlideout], activeTabLeftKey);
                }
            }
        });

        $('#activeElementsTab').on('click', function() {
            if (activeElementsSlideout.position().left === -lsw) {
                Structr.closeLeftSlideOuts([pagesSlideout, dataBindingSlideout], activeTabLeftKey);
                Structr.openLeftSlideOut(activeElementsSlideout, this, activeTabLeftKey, function() {
                });
            } else {
                Structr.closeLeftSlideOuts([activeElementsSlideout], activeTabLeftKey);
            }
        });

        $('#dataBindingTab').on('click', function() {
            if (dataBindingSlideout.position().left === -lsw) {
                Structr.closeLeftSlideOuts([pagesSlideout, activeElementsSlideout], activeTabLeftKey);
                Structr.openLeftSlideOut(dataBindingSlideout, this, activeTabLeftKey, function() {
                    _Pages.reloadDataBindingWizard();
                });
            } else {
                Structr.closeLeftSlideOuts([dataBindingSlideout], activeTabLeftKey);
            }
        });

        $('#widgetsTab').on('click', function() {
            if (widgetsSlideout.position().left === $(window).width()) {
                Structr.closeSlideOuts([paletteSlideout, componentsSlideout, elementsSlideout], activeTabRightKey);
                Structr.openSlideOut(widgetsSlideout, this, activeTabRightKey, function() {
                    _Elements.reloadWidgets();
                });
            } else {
                Structr.closeSlideOuts([widgetsSlideout], activeTabRightKey);
            }
        });

        $('#paletteTab').on('click', function() {
            if (paletteSlideout.position().left === $(window).width()) {
                Structr.closeSlideOuts([widgetsSlideout, componentsSlideout, elementsSlideout], activeTabRightKey);
                Structr.openSlideOut(paletteSlideout, this, activeTabRightKey, function() {
                    _Elements.reloadPalette();
                });
            } else {
                Structr.closeSlideOuts([paletteSlideout], activeTabRightKey);
            }
        });

        $('#componentsTab').on('click', function() {
            if (componentsSlideout.position().left === $(window).width()) {
                Structr.closeSlideOuts([widgetsSlideout, paletteSlideout, elementsSlideout], activeTabRightKey);
                Structr.openSlideOut(componentsSlideout, this, activeTabRightKey, function() {
                    _Elements.reloadComponents();
                });
            } else {
                Structr.closeSlideOuts([componentsSlideout], activeTabRightKey);
            }
        }).droppable({
            tolerance: 'touch',
            over: function(e, ui) {
                if (componentsSlideout.position().left === $(window).width()) {
                    Structr.closeSlideOuts([widgetsSlideout, paletteSlideout, elementsSlideout], activeTabRightKey);
                    Structr.openSlideOut(componentsSlideout, this, activeTabRightKey, function() {
                        _Elements.reloadComponents();
                    });
                }
            }
        });

        $('#elementsTab').on('click', function() {
            if (elementsSlideout.position().left === $(window).width()) {
                $(this).addClass('active');
                Structr.closeSlideOuts([widgetsSlideout, paletteSlideout, componentsSlideout], activeTabRightKey);
                Structr.openSlideOut(elementsSlideout, this, activeTabRightKey, function() {
                    _Elements.reloadUnattachedNodes();
                });
            } else {
                Structr.closeSlideOuts([elementsSlideout], activeTabRightKey);
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
    clearPreviews: function() {

        if (previewTabs && previewTabs.length) {
            previewTabs.children('.page').remove();
        }

    },
    refresh: function() {

        pagesSlideout.find(':not(.compTab)').remove();
        previewTabs.empty();

        pagesSlideout.append('<div class="ver-scrollable" id="pagesTree"></div>')
        pages = $('#pagesTree', pagesSlideout);

        Structr.addPager(pages, true, 'Page');

        previewTabs.append('<li id="import_page" title="Import Template" class="button"><img class="add_button icon" src="icon/page_white_put.png"></li>');
        $('#import_page', previewTabs).on('click', function(e) {
            e.stopPropagation();

            Structr.dialog('Import Template', function() {
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

        $('#import_page', previewTabs).after('<li id="pull_page" title="Sync page from remote instance" class="button"><img class="pull_page_button icon" src="icon/pull_page.png"></li>');
        $('#pull_page', previewTabs).on('click', function(e) {
            e.stopPropagation();
            Structr.pullDialog('Page');
        });

        $('#pull_page', previewTabs).after('<li id="add_page" title="Add page" class="button"><img class="add_button icon" src="icon/add.png"></li>');
        $('#add_page', previewTabs).on('click', function(e) {
            e.stopPropagation();
            Command.createSimplePage();
        });

    },
    addTab: function(entity) {
        previewTabs.children().last().before('<li id="show_' + entity.id + '" class="page ' + entity.id + '_"></li>');

        var tab = $('#show_' + entity.id, previews);

        tab.append('<img class="typeIcon" src="icon/page.png"> <b title="' + entity.name + '" class="name_">' + fitStringToWidth(entity.name, 200) + '</b>');
        tab.append('<input title="Auto-refresh page on changes" alt="Auto-refresh page on changes" class="auto-refresh" type="checkbox"' + (localStorage.getItem(autoRefreshDisabledKey + entity.id) ? '' : ' checked="checked"') + '>');
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

        $('.auto-refresh', tab).on('click', function(e) {
            e.stopPropagation();
            var key = autoRefreshDisabledKey + entity.id;
            var autoRefreshDisabled = localStorage.getItem(key) === '1';
            console.log(localStorage.getItem(key), autoRefreshDisabled);
            if (autoRefreshDisabled) {
                localStorage.removeItem(key);
            } else {
                localStorage.setItem(key, '1');
            }
        });

        return tab;
    },
    resetTab: function(element) {

        log('resetTab', element);

        element.children('input').hide();
        element.children('.name_').show();

        var icons = $('.button', element);
        var autoRefreshSelector = $('.auto-refresh', element);
        //icon.hide();

        element.hover(function(e) {
            icons.show();
            autoRefreshSelector.show()
        },
                function(e) {
                    icons.hide();
                    autoRefreshSelector.hide()
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

        _Pages.refreshActiveElements(id);

    },
    refreshActiveElements: function(id) {
        $('#activeElements div.inner').empty().attr('id', 'id_' + id);
        activeElements = {};

        Command.listActiveElements(id, function(activeElement) {
            _Entities.handleActiveElement(activeElement);
        });
    },
    reloadIframe: function(id) {
        _Pages.clearIframeDroppables();
        Command.get(id, function(obj) {
            var iframe = $('#preview_' + id);
            if (iframe.parent().is(':hidden') || !localStorage.getItem(autoRefreshDisabledKey + id)) {
                iframe.prop('src', viewRootUrl + obj.name + '?edit=2');
                iframe.parent().show();
            }
            _Pages.resize();

            _Pages.refreshActiveElements(id);
        });
    },
    reloadPreviews: function() {
        log('all previews refreshed')
        _Pages.clearIframeDroppables();
        // add a small delay to avoid getting old data in very fast localhost envs
        window.setTimeout(function() {
            $('iframe', $('#previews')).each(function() {
                var self = $(this);
                var pageId = self.prop('id').substring('preview_'.length);
                if (!localStorage.getItem(autoRefreshDisabledKey + pageId) && pageId === activeTab) {

                    _Pages.reloadIframe(pageId);

                    //var doc = this.contentDocument;
                    //doc.location.reload(true);
                }
            });
        }, 200);
    },
    clearIframeDroppables: function() {
        var droppablesArray = [];
        var d = $.ui.ddmanager.droppables.default;
        if (!d)
            return;
        d.forEach(function(d) {
            if (!d.options.iframe) {
                droppablesArray.push(d);
            }
        });
        $.ui.ddmanager.droppables.default = droppablesArray;
    },
    makeTabEditable: function(element) {
        //element.off('dblclick');

        var id = element.prop('id').substring(5);

        element.off('hover');
        //var oldName = $.trim(element.children('.name_').text());
        var oldName = $.trim(element.children('b.name_').attr('title'));
        element.children('b').hide();
        element.find('.button').hide();
        var input = $('input.new-name', element);

        if (!input.length) {
            element.append('<input type="text" size="' + (oldName.length + 4) + '" class="new-name" value="' + oldName + '">');
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

        var hasChildren = entity.children.length;


        pages.append('<div id="id_' + entity.id + '" class="node page"></div>');
        var div = Structr.node(entity.id);

        $('.button', div).on('mousedown', function(e) {
            e.stopPropagation();
        });

        div.append('<img class="typeIcon" src="icon/page.png">'
                + '<b title="' + entity.name + '" class="name_">' + fitStringToWidth(entity.name, 200) + '</b> <span class="id">' + entity.id + '</span>');

        _Entities.appendExpandIcon(div, entity, hasChildren);
        _Entities.appendAccessControlIcon(div, entity);

        div.append('<img title="Delete page \'' + entity.name + '\'" alt="Delete page \'' + entity.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            _Entities.deleteNode(this, entity);
        });

        _Entities.appendEditPropertiesIcon(div, entity);
        _Entities.appendEditSourceIcon(div, entity);

        div.append('<img title="Clone page \'' + entity.name + '\'" alt="Clone page \'' + entity.name + '\'" class="clone_icon button" src="icon/page_copy.png">');
        $('.clone_icon', div).on('click', function(e) {
            e.stopPropagation();
            Command.clonePage(entity.id);
        });

        div.append('<img title="Sync page \'' + entity.name + '\' to remote instance" alt="Sync page \'' + entity.name + '\' to remote instance" class="push_icon button" src="icon/page_white_get.png">');
        div.children('.push_icon').on('click', function() {
            Structr.pushDialog(entity.id, true);
            return false;
        });

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
            var doc = $(this).contents();
            var head = $(doc).find('head');
            if (head)
                head.append('<style media="screen" type="text/css">'
                        + '* { z-index: 0}\n'
                        + '.nodeHover { -moz-box-shadow: 0 0 5px #888; -webkit-box-shadow: 0 0 5px #888; box-shadow: 0 0 5px #888; }\n'
                        //+ '.structr-content-container { display: inline-block; border: none; margin: 0; padding: 0; min-height: 10px; min-width: 10px; }\n'
                        + '.structr-content-container { min-height: .25em; min-width: .25em; }\n'
                        //		+ '.structr-element-container-active { display; inline-block; border: 1px dotted #e5e5e5; margin: -1px; padding: -1px; min-height: 10px; min-width: 10px; }\n'
                        //		+ '.structr-element-container { }\n'
                        + '.structr-element-container-active:hover { -moz-box-shadow: 0 0 5px #888; -webkit-box-shadow: 0 0 5px #888; box-shadow: 0 0 5px #888; }\n'
                        + '.structr-element-container-selected { -moz-box-shadow: 0 0 8px #860; -webkit-box-shadow: 0 0 8px #860; box-shadow: 0 0 8px #860; }\n'
                        + '.structr-element-container-selected:hover { -moz-box-shadow: 0 0 10px #750; -webkit-box-shadow: 0 0 10px #750; box-shadow: 0 0 10px #750; }\n'
                        + '.nodeHover { -moz-box-shadow: 0 0 5px #888; -webkit-box-shadow: 0 0 5px #888; box-shadow: 0 0 5px #888; }\n'
                        + '.structr-editable-area { color: #222; background-color: #ffe; padding: 1px; margin: -1px; -moz-box-shadow: 0 0 5px #888; -webkit-box-shadow: 0 0 5px yellow; box-shadow: 0 0 5px #888; }\n'
                        + '.structr-editable-area-active { background-color: #ffe; border: 1px solid orange ! important; color: #333; margin: -1px; padding: 1px; }\n'
                        + '.link-hover { border: 1px solid #00c; }\n'
                        + '.edit_icon, .add_icon, .delete_icon, .close_icon, .key_icon {  cursor: pointer; heigth: 16px; width: 16px; vertical-align: top; float: right;  position: relative;}\n'
                        + '</style>');

            _Pages.findDroppablesInIframe($(this).contents(), entity.id).each(function(i, element) {
                var el = $(element);

                _Dragndrop.makeDroppable(el, entity.id);

                var structrId = el.attr('data-structr-id');
                if (structrId) {

                    $('.move_icon', el).on('mousedown', function(e) {
                        e.stopPropagation();
                        var self = $(this);
                        var element = self.closest('[data-structr-id]');
                        log(element);
                        var entity = Structr.entity(structrId, element.prop('data-structr-id'));
                        entity.type = element.prop('data-structr_type');
                        entity.name = element.prop('data-structr_name');
                        log('move', entity);
                        self.parent().children('.structr-node').show();
                    });

                    $('.delete_icon', el).on('click', function(e) {
                        e.stopPropagation();
                        var self = $(this);
                        var element = self.closest('[data-structr-id]');
                        var entity = Structr.entity(structrId, element.prop('data-structr-id'));
                        entity.type = element.prop('data-structr_type');
                        entity.name = element.prop('data-structr_name');
                        log('delete', entity);
                        var parentId = element.prop('data-structr-id');

                        Command.removeSourceFromTarget(entity.id, parentId);
                        _Entities.deleteNode(this, entity);
                    });
                    var offsetTop = -30;
                    var offsetLeft = 0;
                    el.on({
                        click: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            var selected = self.hasClass('structr-element-container-selected');
                            self.closest('body').find('.structr-element-container-selected').removeClass('structr-element-container-selected');
                            if (!selected) {
                                self.toggleClass('structr-element-container-selected');
                            }
                            $('.nodeSelected').removeClass('nodeSelected');
                            _Pages.displayDataBinding(structrId);
                            if (!Structr.node(structrId)) {
                                _Pages.expandTreeNode(structrId);
                            } else {
                                var treeEl = Structr.node(structrId);
                                if (treeEl && !selected) {
                                    treeEl.toggleClass('nodeSelected');
                                }
                            }
                            return false;
                        },
                        mouseover: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            self.addClass('structr-element-container-active');
                            _Pages.highlight(structrId);
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
                            _Pages.unhighlight(structrId);
                        }
                    });

                }
            });

            $(this).contents().find('*').each(function(i, element) {

                getComments(element).forEach(function(c) {

                    var inner = $(getNonCommentSiblings(c.textNode));
                    $(getNonCommentSiblings(c.textNode)).remove();
                    $(c.textNode).replaceWith('<span data-structr-id="' + c.id + '" data-structr-raw-content="' + escapeForHtmlAttributes(c.rawContent, true) + '">' + escapeTags(c.textNode.nodeValue) + '</span>');

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
                            _Pages.highlight(contentSourceId);
                        },
                        mouseout: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            //swapFgBg(self);
                            self.removeClass('structr-editable-area');
                            //self.prop('contenteditable', false);
                            //$('#hoverStatus').text('-- non-editable --');
                            var contentSourceId = self.attr('data-structr-id');
                            _Pages.unhighlight(contentSourceId);
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

                            var structrId = self.attr('data-structr-id');
                            _Pages.expandTreeNode(structrId);

                            return false;

                        },
                        blur: function(e) {
                            e.stopPropagation();
                            var self = $(this);
                            var contentSourceId = self.attr('data-structr-id');
                            var text = unescapeTags(cleanText(self.html()));
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
                log('dropped onto', self);
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
                log('elementId', elementId);

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
    findDroppablesInIframe: function(iframeDocument, id) {
        var droppables = iframeDocument.find('[data-structr-id]');
        if (droppables.length === 0) {
            //iframeDocument.append('<html structr_element_id="' + entity.id + '">dummy element</html>');
            var html = iframeDocument.find('html');
            html.attr('data-structr-id', id);
            html.addClass('structr-element-container');
        }
        droppables = iframeDocument.find('[data-structr-id]');
        return droppables;
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
    displayDataBinding: function(id) {
        dataBindingSlideout.children('#data-binding-inputs').remove();
        dataBindingSlideout.append('<div class="inner" id="data-binding-inputs"></div>');

        var el = $('#data-binding-inputs');

        var entity = StructrModel.obj(id);

        el.append('<div id="data-binding-tabs" class="data-tabs"><ul><li class="active" id="tab-binding-rest">REST Query</li><li id="tab-binding-cypher">Cypher Query</li><li id="tab-binding-xpath">XPath Query</li></ul>'
                + '<div id="content-tab-binding-rest"></div><div id="content-tab-binding-cypher"></div><div id="content-tab-binding-xpath"></div></div>');

        _Entities.appendTextarea($('#content-tab-binding-rest'), entity, 'restQuery', 'REST Query', '');
        _Entities.appendTextarea($('#content-tab-binding-cypher'), entity, 'cypherQuery', 'Cypher Query', '');
        _Entities.appendTextarea($('#content-tab-binding-xpath'), entity, 'xpathQuery', 'XPath Query', '');

        _Entities.activateTabs('#data-binding-tabs', '#content-tab-binding-rest');

        _Entities.appendInput(el, entity, 'dataKey', 'Data Key', 'Query results are mapped to this key and can be accessed by ${<i>&lt;dataKey&gt;.&lt;propertyKey&gt;</i>}');

    },
    reloadDataBindingWizard: function() {
        dataBindingSlideout.children('#wizard').remove();
        dataBindingSlideout.prepend('<div class="inner" id="wizard"><select id="type-selector"><option>--- Select type ---</option></select><div id="data-wizard-attributes"></div></div>');
        // Command.list(type, rootOnly, pageSize, page, sort, order, callback) {
        var selectedType = localStorage.getItem(selectedTypeKey);
        Command.list('SchemaNode', false, 1000, 1, 'name', 'asc', function(typeNode) {
            $('#type-selector').append('<option ' + (typeNode.id === selectedType ? 'selected' : '') + ' value="' + typeNode.id + '">' + typeNode.name + '</option>')
        });

        $('#data-wizard-attributes').empty();
        if (selectedType) {
            _Pages.showTypeData(selectedType);
        }

        $('#type-selector').on('change', function() {
            $('#data-wizard-attributes').empty();
            var id = $(this).children(':selected').attr('value');
            _Pages.showTypeData(id);
        });

    },
    showTypeData: function(id) {

        Command.get(id, function(t) {

            var typeKey = t.name.toLowerCase();
            localStorage.setItem(selectedTypeKey, id);

            $('#data-wizard-attributes').append('<div class="clear">&nbsp;</div><p>You can drag and drop the type box onto a block in a page.'
                    + 'The type will be bound to the block which will loop over the result set.</p>');

            $('#data-wizard-attributes').append('<div class="data-binding-type draggable">:' + t.name + '</div>');
            $('.data-binding-type').draggable({
                iframeFix: true,
                revert: 'invalid',
                containment: 'body',
                helper: 'clone',
                appendTo: '#main',
                stack: '.node',
                zIndex: 99
            });

            $('#data-wizard-attributes').append('<h3>Custom Properties</h3><div class="custom"></div><div class="clear">&nbsp;</div><h3>System Properties</h3><div class="system"></div>');

            var subkey = 'name';

            var endNodes = [];
            $.each(t.relatedTo, function(i, endNode) {

                if (isIn(endNode.id, endNodes))
                    return;
                endNodes.push(endNode.id);

                $.ajax({
                    url: rootUrl + '/schema_relationships?sourceId=' + id + '&targetId=' + endNode.id,
                    type: 'GET',
                    contentType: 'application/json',
                    statusCode: {
                        200: function(data) {
                            $.each(data.result, function(i, r) {
                                _Schema.getPropertyName(t.name, r.relationshipType, true, function(key, isCollection) {
                                    $('#data-wizard-attributes .custom').append('<div class="draggable data-binding-attribute ' + key + '" collection="' + isCollection + '" subkey="' + subkey + '">' + typeKey + '.' + key + '</div>');
                                    $('#data-wizard-attributes .custom').children('.' + key).draggable({
                                        iframeFix: true,
                                        revert: 'invalid',
                                        containment: 'body',
                                        helper: 'clone',
                                        appendTo: '#main',
                                        stack: '.node',
                                        zIndex: 99
                                    }).on('click', function() {
                                        //console.log('expand')
                                    });
                                });
                            });
                        }
                    }
                });
            });

            var startNodes = [];
            $.each(t.relatedFrom, function(i, startNode) {

                if (isIn(startNode.id, startNodes))
                    return;
                startNodes.push(startNode.id);

                $.ajax({
                    url: rootUrl + '/schema_relationships?sourceId=' + startNode.id + '&targetId=' + id,
                    type: 'GET',
                    contentType: 'application/json',
                    statusCode: {
                        200: function(data) {
                            $.each(data.result, function(i, r) {
                                _Schema.getPropertyName(t.name, r.relationshipType, false, function(key, isCollection) {
                                    $('#data-wizard-attributes .custom').append('<div class="draggable data-binding-attribute ' + key + '" collection="' + isCollection + '" subkey="' + subkey + '">' + typeKey + '.' + key + '</div>');
                                    $('#data-wizard-attributes .custom').children('.draggable.' + key).draggable({
                                        iframeFix: true,
                                        revert: 'invalid',
                                        containment: 'body',
                                        helper: 'clone',
                                        appendTo: '#main',
                                        stack: '.node',
                                        zIndex: 99
                                    });
                                });
                            });
                        }
                    }
                });
            });

            $.each(Object.keys(t), function(i, key) {
                var type = 'system';
                if (key.startsWith('_')) {

                    key = key.substring(1);
                    type = 'custom'

                } else if (key === 'relatedTo' || key === 'relatedFrom') {
                    // do nothing
                    return;
                }
                var el = $('#data-wizard-attributes .' + type);
                el.append('<div class="draggable data-binding-attribute ' + key + '">' + typeKey + '.' + key + '</div>');
                el.children('.draggable.' + key).draggable({
                    iframeFix: true,
                    revert: 'invalid',
                    containment: 'body',
                    helper: 'clone',
                    appendTo: '#main',
                    stack: '.node',
                    zIndex: 99
                });
            });

            $('#data-wizard-attributes').append('<div class="clear">&nbsp;</div><p>Drag and drop these elements onto the page for data binding.</p>');

        });

    },
    expandTreeNode: function(id, stack) {
        stack = stack || [];
        stack.push(id);
        Command.get(id, function(obj) {
            if (obj.parent) {
                _Pages.expandTreeNode(obj.parent.id, stack);
            } else {
                _Entities.expandAll(stack.reverse());
            }
        });
    },
    highlight: function(id) {
        var node = Structr.node(id);
        if (node) {
            node.parent().removeClass('nodeHover');
            node.addClass('nodeHover');
        }
        var activeNode = Structr.node(id, '#active_');
        if (activeNode) {
            activeNode.parent().removeClass('nodeHover');
            activeNode.addClass('nodeHover');
        }
    },
    unhighlight: function(id) {
        var node = Structr.node(id);
        if (node) {
            node.removeClass('nodeHover');
        }
        var activeNode = Structr.node(id, '#active_');
        if (activeNode) {
            activeNode.removeClass('nodeHover');
        }
    }
};
