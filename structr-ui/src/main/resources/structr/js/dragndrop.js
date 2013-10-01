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

/**
 * Library for all drag & drop actions in Structr
 */

var sorting = false;
var sortParent;

var _Dragndrop = {
    /**
     * Make DOM element a target for drop events
     */
    makeDroppable: function(element, previewId) {
        var el = $(element);
        var tag, iframe = previewId ? $('#preview_' + previewId) : undefined;

        el.droppable({
            iframeFix: true,
            iframe: iframe,
            accept: '.node, .element, .content, .image, .file, .widget',
            greedy: true,
            hoverClass: 'nodeHover',
            //appendTo: 'body',
            //tolerance: 'pointer',
            drop: function(e, ui) {
                
                log('drop event', e, ui);
                
                e.preventDefault();
                e.stopPropagation();

                var self = $(this);
                //if (el.sortable) el.sortable('refresh');

                var sourceId = getId(ui.draggable) || getComponentId(ui.draggable);

                if (!sourceId) {
                    tag = $(ui.draggable).text();
                }

                var targetId = getId(self);

                if (!targetId) {
                    targetId = self.attr('data-structr-el');
                }


                log('dropped onto', self, targetId, getId(sortParent));
                if (targetId === getId(sortParent)) {
                    log('target id == sortParent id', targetId, getId(sortParent));
                    return false;
                }

                _Entities.ensureExpanded(self);
                sorting = false;
                sortParent = undefined;

                var source = StructrModel.obj(sourceId);
                var target = StructrModel.obj(targetId);

                var page = self.closest('.page')[0];
                
                if (!page) {
                    page = self.closest('[data-structr-page]')[0];
                }
                var pageId = (page ? getId(page) : target.pageId);

                if (!pageId) {
                    pageId = $(page).attr('data-structr-page');
                }

                if (!target) {
                    console.log('no target');
                    return;
                }


                if (_Dragndrop.dropAction(source, target, pageId, tag)) {
                    $(ui.draggable).remove();
                    sortParent = undefined;
                }

            }
        });

    },
    /**
     * Enable sorting on DOM element
     */
    makeSortable: function(element) {
        var el = $(element);

        var sortableOptions = {
            iframeFix: true,
            sortable: '.node',
            appendTo: '#main',
            helper: 'clone',
            zIndex: 99,
            containment: 'body',
            start: function(event, ui) {
                sorting = true;
                sortParent = $(ui.item).parent();
            },
            update: function(event, ui) {

                var el = $(ui.item);
                //console.log('### sortable update: sorting?', sorting, getId(el), getId(self), getId(sortParent));
                if (!sorting)
                    return false;

                var id = getId(el);
                if (!id)
                    id = getComponentId(el);

                var nextNode = el.next('.node');
                var refId = getId(nextNode);
                if (!refId)
                    refId = getComponentId(nextNode);

                var parentId = getId(sortParent);
                el.remove();
                Command.insertBefore(parentId, id, refId);
                sorting = false;
                sortParent = undefined;
                //_Pages.reloadPreviews();
            },
            stop: function(event, ui) {
                //$(ui.sortable).remove();
                sorting = false;
                _Entities.resetMouseOverState(ui.item);
            }
        };

        el.sortable(sortableOptions);

    },
    /**
     * Define what happens in case of a source object is dropped onto
     * a target object in the given page.
     * 
     * The optional tag is used to create new elements if the source object
     * is undefined. This is the case if an element was dragged from the
     * HTML elements palette.
     */
    dropAction: function(source, target, pageId, tag) {

        log('dropAction', source, target, pageId, tag);

        if (source && pageId && source.pageId && pageId !== source.pageId) {

            if (shadowPage && source.pageId === shadowPage.id) {

                log('clone component!');
                Command.cloneComponent(source.id, target.id);

            } else {

                Command.appendChild(source.id, target.id, pageId);
                log('dropped', source.id, 'onto', target.id, 'in page', pageId);

            }

            //Command.copyDOMNode(source.id, target.id);
            //_Entities.showSyncDialog(source, target);
            //_Elements.reloadComponents();
            return false;

        }

        // element dropped on itself?
        if (source && target && (source.id === target.id)) {
            log('drop on self not allowed');
            return false;
        }

        if (source && source.type === 'Widget') {

            return _Dragndrop.widgetDropped(source, target, pageId);

        }

        if (source && source.type === 'Image') {

            return _Dragndrop.imageDropped(source, target, pageId);

        }

        if (source && source.type === 'File') {

            return _Dragndrop.fileDropped(source, target, pageId);

        }

        if (!source && tag) {

            return _Dragndrop.htmlElementFromPaletteDropped(tag, target, pageId);

        } else {

            tag = target.tag;
            
            
            if (source && target && source.id && target.id) {
            
                sorting = false;
                log('appendChild', source, target);
                Command.appendChild(source.id, target.id);

                return true;
                
            } else {
                log('unknown drag\'n drop  situation', source, target);
            }
        }

        log('drop event in appendElementElement', pageId, getId(self), (tag !== 'content' ? tag : ''));

    },
    htmlElementFromPaletteDropped: function(tag, target, pageId) {
        var nodeData = {};
        if (tag === 'a' || tag === 'p'
                || tag === 'h1' || tag === 'h2' || tag === 'h3' || tag === 'h4' || tag === 'h5' || tag === 'h5' || tag === 'pre'
                || tag === 'li' || tag === 'em' || tag === 'title' || tag === 'b' || tag === 'span' || tag === 'th' || tag === 'td' || tag === 'button') {
            if (tag === 'a') {
                nodeData._html_href = '/${link.name}';
                nodeData.childContent = '${parent.link.name}';
            } else if (tag === 'title') {
                nodeData.childContent = '${page.name}';
            } else {
                nodeData.childContent = 'Initial text for ' + tag;
            }
        }
        if (target.type === 'Content') {
            if (tag === 'content') {
                log('content element dropped on content, doing nothing');
                return false;
            }
            console.log('wrap content', pageId, target.id, tag);
            Command.wrapContent(pageId, target.id, tag);
        } else {
            Command.createAndAppendDOMNode(pageId, target.id, (tag !== 'content' ? tag : ''), nodeData);
        }
        return false;
    },
    widgetDropped: function(source, target, pageId) {

        var baseUrl = 'http://' + remoteHost + ':' + remotePort;

        Structr.modules['widgets'].unload();
        _Pages.makeMenuDroppable();

        //var pattern = /^\[[a-zA-Z]+\]$/;
        var pattern = /\[[a-zA-Z]+\]/g;
        var text = source.source;
        if (text) {

            var rawMatches = text.match(pattern);

            if (rawMatches) {

                var matches = $.unique(rawMatches);

                if (matches && matches.length) {

                    Structr.dialog('Configure Widget', function() {
                    }, function() {
                    });

                    dialogText.append('<p>Fill out the following parameters to correctly configure the widget.</p><table class="props"></table>');
                    var table = $('table', dialogText);

                    $.each(matches, function(i, match) {

                        var label = _Crud.formatKey(match.replace(/\[/, '').replace(/\]/, ''));
                        table.append('<tr><td><label for="' + label + '">' + label + '</label></td><td><input type="text" id="' + match + '" placeholder="' + label + '"></td></tr>');

                    });

                    dialog.append('<button id="appendWidget">Append Widget</button>');
                    var attrs = {};
                    $('#appendWidget').on('click', function(e) {

                        $.each(matches, function(i, match) {

                            $.each($('input[type="text"]', table), function(i, m) {
                                var key = $(m).prop('id').replace(/\[/, '').replace(/\]/, '')
                                attrs[key] = $(this).val();
                                //console.log(this, match, key, attrs[key]);
                            });

                        });

                        //console.log(source.source, elementId, pageId, attrs);
                        e.stopPropagation();
                        Command.appendWidget(text, target.id, pageId, baseUrl, attrs);

                        dialogCancelButton.click();
                        return false;
                    });

                }

            } else {

                // If no matches, directly append widget
                Command.appendWidget(source.source, target.id, pageId, baseUrl);

            }

        }

        return;

    },
    fileDropped: function(source, target, pageId) {

        var nodeData = {}, tag;

        //name = $(ui.draggable).children('.name_').attr('title');
        name = source.name;

        var parentTag = target.tag;

        //var parentTag = self.children('.tag_').text();
        console.log(source, target, parentTag);
        nodeData.linkableId = source.id;

        if (parentTag === 'head') {

            log('File dropped in <head>');

            if (name.endsWith('.css')) {

                //console.log('CSS file dropped in <head>, creating <link>');

                tag = 'link';
                nodeData._html_href = '/${link.name}';
                nodeData._html_type = 'text/css';
                nodeData._html_rel = 'stylesheet';
                nodeData._html_media = 'screen';


            } else if (name.endsWith('.js')) {

                log('JS file dropped in <head>, creating <script>');

                tag = 'script';
                nodeData._html_src = '/${link.name}';
                nodeData._html_type = 'text/javascript';
            }

        } else {

            log('File dropped, creating <a> node', name);
            nodeData._html_href = '/${link.name}';
            nodeData._html_title = '${link.name}';
            nodeData.childContent = '${parent.link.name}';
            tag = 'a';
        }
        source.id = undefined;

        Structr.modules['files'].unload();
        _Pages.makeMenuDroppable();

        Command.createAndAppendDOMNode(pageId, target.id, tag, nodeData);

        return true;

    },
    imageDropped: function(source, target, pageId) {

        var nodeData = {}, name = source.name, tag;
        console.log('Image dropped, creating <img> node', name);
        nodeData._html_src = '/' + name;
        nodeData.name = name;
        tag = 'img';

        Structr.modules['images'].unload();
        _Pages.makeMenuDroppable();

        Command.createAndAppendDOMNode(pageId, target.id, tag, nodeData);

        return true;
    }

}