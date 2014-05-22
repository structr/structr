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

var elements, dropBlocked;

var _Elements = {
    icon: 'icon/brick.png',
    icon_comp: 'icon/package.png',
    icon_repeater: 'icon/bricks.png',
    add_icon: 'icon/brick_add.png',
    delete_icon: 'icon/brick_delete.png',
    elementNames: [
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
    elementGroups: [
        {
            'name': 'Root',
            'elements': ['html', 'content', 'comment']
        },
        {
            'name': 'Metadata',
            'elements': ['head', 'title', 'base', 'link', 'meta', 'style']
        },
        {
            'name': 'Sections',
            'elements': ['body', 'section', 'nav', 'article', 'aside', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hgroup', 'header', 'footer', 'address']
        },
        {
            'name': 'Grouping',
            'elements': ['div', 'p', 'hr', 'ol', 'ul', 'li', 'dl', 'dt', 'dd', 'pre', 'blockquote', 'figure', 'figcaption']
        },
        {
            'name': 'Scripting',
            'elements': ['script', 'noscript']
        },
        {
            'name': 'Tabular',
            'elements': ['table', 'tr', 'td', 'th', 'caption', 'colgroup', 'col', 'tbody', 'thead', 'tfoot']
        },
        {
            'name': 'Text',
            'elements': ['a', 'em', 'strong', 'small', 's', 'cite', 'g', 'dfn', 'abbr', 'time', 'code', 'var', 'samp', 'kbd', 'sub', 'sup', 'i', 'b', 'u', 'mark', 'ruby', 'rt', 'rp', 'bdi', 'bdo', 'span', 'br', 'wbr']
        },
        {
            'name': 'Edits',
            'elements': ['ins', 'del']
        },
        {
            'name': 'Embedded',
            'elements': ['img', 'video', 'audio', 'source', 'track', 'canvas', 'map', 'area', 'iframe', 'embed', 'object', 'param']
        },
        {
            'name': 'Forms',
            'elements': ['form', 'input', 'button', 'select', 'datalist', 'optgroup', 'option', 'textarea', 'fieldset', 'legend', 'label', 'keygen', 'output', 'progress', 'meter']
        },
        {
            'name': 'Interactive',
            'elements': ['details', 'summary', 'command', 'menu']
        }
    ],
    /**
     * Reload widgets
     */
    reloadWidgets: function() {

        widgetsSlideout.find(':not(.compTab)').remove();

        widgetsSlideout.append('<div class="ver-scrollable"><div id="widgets"><h3>Local Widgets</h3></div><div id="remoteWidgets"><h3>Remote Widgets</h3></div></div>');
        widgets = $('#widgets', widgetsSlideout);

        widgets.droppable({
            drop: function(e, ui) {
                e.preventDefault();
                e.stopPropagation();
                dropBlocked = true;
                var sourceEl = $(ui.draggable);
                if (sourceEl.parent().attr('id') === 'widgets') {
                    log('widget dropped on widget area, aborting');
                    return false;
                }
                var sourceId = getId(sourceEl);

                $.ajax({
                    url: viewRootUrl + sourceId + '?edit=3',
                    contentType: 'text/html',
                    statusCode: {
                        200: function(data) {
                            Command.createLocalWidget(sourceId, 'New Widget (' + sourceId + ')', data, function(entity) {

                                //_Widgets.appendWidgetElement(entity, false, localWidgetsArea);

                            });
                        }
                    }
                });
            }

        });


        Command.list('Widget', true, 1000, 1, 'name', 'asc', function(entity) {
            StructrModel.create(entity, null, false);
            _Widgets.appendWidgetElement(entity, false, widgets);
        });

        var remoteWidgetsArea = $('#remoteWidgets', widgetsSlideout);
        _Widgets.getRemoteWidgets(widgetsUrl, function(entity) {

            var obj = StructrModel.create(entity, undefined, false);
            obj.srcUrl = widgetsUrl + '/' + entity.id;
            _Widgets.appendWidgetElement(obj, true, remoteWidgetsArea);

        });

    },
    /**
     * Reload HTML palette
     *
     * @returns {undefined}
     */
    reloadPalette: function() {

        paletteSlideout.find(':not(.compTab)').remove();
        paletteSlideout.append('<div class="ver-scrollable" id="paletteArea"></div>')
        palette = $('#paletteArea', paletteSlideout);

        palette.droppable({
            drop: function(e, ui) {
                e.preventDefault();
                e.stopPropagation();
                return false;
            }
        });

        if (!$('.draggable', palette).length) {

            $(_Elements.elementGroups).each(function(i, group) {
                log(group);
                palette.append('<div class="elementGroup" id="group_' + group.name + '"><h3>' + group.name + '</h3></div>');
                $(group.elements).each(function(j, elem) {
                    var div = $('#group_' + group.name);
                    div.append('<div class="draggable element" id="add_' + elem + '">' + elem + '</div>');
                    $('#add_' + elem, div).draggable({
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
    },
    /**
     * Reload components in component area
     */
    reloadComponents: function() {

        componentsSlideout.find(':not(.compTab)').remove();
        componentsSlideout.append('<div class="ver-scrollable" id="componentsArea"></div>')
        components = $('#componentsArea', componentsSlideout);

        Command.getByType('ShadowDocument', 1, 1, null, null, function(entity) {
            shadowPage = entity;
        });
        components.droppable({
            drop: function(e, ui) {
                e.preventDefault();
                e.stopPropagation();
                dropBlocked = true;
                var sourceEl = $(ui.draggable);
                var sourceId = getId(sourceEl);
                if (!sourceId) return false;
                var obj = StructrModel.obj(sourceId);
                if (obj.type === 'Content' || obj.type === 'Comment') {
                    return false;
                }
                if (obj && obj.syncedNodes && obj.syncedNodes.length || sourceEl.parent().attr('id') === 'componentsArea') {
                    log('component dropped on components area, aborting');
                    return false;
                }
                Command.createComponent(sourceId);
                dropBlocked = false;

            }

        });
        _Dragndrop.makeSortable(components);

        Command.listComponents(1000, 1, 'name', 'asc', function(entity) {

            if (!entity)
                return false;

            var obj = StructrModel.create(entity, null, false);
            var el = _Pages.appendElementElement(obj, components, true);

            if (isExpanded(entity.id)) {
                _Entities.ensureExpanded(el);
            }

        });

    },
    /**
     * Reload unattached nodes in elements area
     */
    reloadUnattachedNodes: function() {

        elementsSlideout.find(':not(.compTab)').remove();
        elementsSlideout.append('<div class="ver-scrollable" id="elementsArea"></div>')
        elements = $('#elementsArea', elementsSlideout);

        elements.append('<button class="btn" id="delete-all-unattached-nodes">Delete all</button>');

        var btn = $('#delete-all-unattached-nodes')
        btn.on('click', function() {
            Structr.confirmation('<p>Delete all DOM elements without parent?</p>',
                    function() {
                        Command.deleteUnattachedNodes();
                        $.unblockUI({
                            fadeOut: 25
                        });
                        _Pages.closeSlideOuts([elementsSlideout]);
                    });
        });

        _Dragndrop.makeSortable(elements);
        Command.listUnattachedNodes(1000, 1, 'name', 'asc', function(entity) {

            if (!entity) {
                return;
            }

            var obj = StructrModel.create(entity, null, false);
            var el = _Pages.appendElementElement(obj, elements, true);

            if (isExpanded(entity.id)) {
                _Entities.ensureExpanded(el);
            }

        });

    },
    componentNode: function(id) {
        return $($('#componentId_' + id)[0]);
    },
    /**
     * Create a DOM node and append to the appropriate parent
     */
    appendElementElement: function(entity, refNode, refNodeIsParent) {
        log('_Elements.appendElementElement', entity);

        if (!entity)
            return false;

        var hasChildren = entity.childrenIds && entity.childrenIds.length;
        var isComponent = entity.sharedComponent || (entity.syncedNodes && entity.syncedNodes.length);

        // store active nodes in special place..
        var isActiveNode = entity.hideOnIndex || entity.hideOnDetail || entity.hideConditions || entity.showConditions || entity.dataKey;

        var parent;
        if (refNodeIsParent) {
            parent = refNode;
        } else {
            parent = entity.parent && entity.parent.id ? Structr.node(entity.parent.id) : elements;
        }

        log('appendElementElement parent, refNode, refNodeIsParent', parent, refNode, refNodeIsParent);
        if (!parent)
            return false;

        _Entities.ensureExpanded(parent);

        var id = entity.id;

        var html = '<div id="id_' + id + '" class="node element' + (entity.tag === 'html' ? ' html_element' : '') + ' ' + (isActiveNode ? ' activeNode' : 'staticNode') + '"></div>';

        if (refNode && !refNodeIsParent) {
            refNode.before(html);
        } else {
            parent.append(html);
        }

        var div = Structr.node(id);

        log('Element appended (div, parent)', div, parent);

        if (!div)
            return false;

        var displayName = entity.name ? entity.name : (entity.tag ? entity.tag : '[' + entity.type + ']');

        var icon = isActiveNode ? _Elements.icon_repeater : isComponent ? _Elements.icon_comp : _Elements.icon;

        div.append('<img class="typeIcon" src="' + icon + '">'
                + '<b title="' + displayName + '" class="tag_ name_">' + displayName + '</b><span class="id">' + entity.id + '</span>'
                + (entity._html_id ? '<span class="_html_id_">#' + entity._html_id.replace(/\${.*}/g, '${…}') + '</span>' : '')
                + (entity._html_class ? '<span class="_html_class_">.' + entity._html_class.replace(/\${.*}/g, '${…}').replace(/ /g, '.') + '</span>' : '')
                + '</div>');

        _Entities.appendExpandIcon(div, entity, hasChildren);

        // Prevent type icon from being draggable
        $('.typeIcon', div).on('mousedown', function(e) {
            e.stopPropagation();
        });

        // Prevent display name
        $('b', div).on('mousedown', function(e) {
            e.stopPropagation();
        });

        // Prevent id from being draggable
        $('#id', div).on('mousedown', function(e) {
            e.stopPropagation();
        });

        // Prevent html class from being draggable
        $('._html_id_', div).on('mousedown', function(e) {
            e.stopPropagation();
        });

        // Prevent html class from being draggable
        $('._html_class_', div).on('mousedown', function(e) {
            e.stopPropagation();
        });

        _Entities.appendAccessControlIcon(div, entity);
        div.append('<img title="Delete ' + entity.tag + ' element ' + entity.id + '" alt="Delete ' + entity.tag + ' element ' + entity.id + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            _Entities.deleteNode(this, entity, function() {
                var synced = entity.syncedNodes;
                if (synced && synced.length) {
                    synced.forEach(function(id) {
                        var el = Structr.node(id);
                        if (el && el.children && el.children.length) {
                            el.children('img.typeIcon').attr('src', _Elements.icon);
                        }

                    });
                }
            });
        });

        _Entities.setMouseOver(div, undefined, ((entity.syncedNodes&&entity.syncedNodes.length)?entity.syncedNodes:[entity.sharedComponent]));
        //_Entities.appendEditSourceIcon(div, entity);
        _Entities.appendEditPropertiesIcon(div, entity);
        //_Entities.appendDataIcon(div, entity);

        if (entity.tag === 'a' || entity.tag === 'link' || entity.tag === 'script' || entity.tag === 'img' || entity.tag === 'video' || entity.tag === 'object') {

            div.append('<img title="Edit Link" alt="Edit Link" class="link_icon button" src="' + Structr.link_icon + '">');
            if (entity.linkable) {
                div.append('<span class="linkable">' + entity.linkable + '</span>');
            }

            $('.linkable', div).on('click', function(e) {
                e.stopPropagation();

                var file = {'name': entity.linkable, 'id': entity.linkableId};

                Structr.dialog('Edit ' + file.name, function() {
                    log('content saved')
                }, function() {
                    log('cancelled')
                });
                _Files.editContent(this, file, $('#dialogBox .dialogText'));

            });

            $('.link_icon', div).on('click', function(e) {
                e.stopPropagation();

                Structr.dialog('Link to Resource (Page, File or Image)', function() {
                    return true;
                }, function() {
                    return true;
                });

                dialog.empty();
                dialogMsg.empty();

                if (entity.tag !== 'img') {

                    dialog.append('<p>Click on a Page, File or Image to establish a hyperlink to this &lt;' + entity.tag + '&gt; element.</p>');

                    dialog.append('<h3>Pages</h3><div class="linkBox" id="pagesToLink"></div>');

                    var pagesToLink = $('#pagesToLink');

                    Structr.addPager(pagesToLink, true, 'Page', function(page) {

                        if (page.type === 'ShadowDocument') {
                            return;
                        }

                        pagesToLink.append('<div class="node page ' + page.id + '_"><img class="typeIcon" src="icon/page.png">'
                                + '<b title="' + page.name + '" class="name_">' + page.name + '</b></div>');

                        var div = $('.' + page.id + '_', pagesToLink);

                        div.on('click', function(e) {
                            e.stopPropagation();
                            Command.link(entity.id, page.id);
                            $('#dialogBox .dialogText').empty();
                            _Pages.reloadPreviews();
                            $.unblockUI({
                                fadeOut: 25
                            });
                        }).css({
                            cursor: 'pointer'
                        }).hover(function() {
                            $(this).addClass('nodeHover');
                        }, function() {
                            $(this).removeClass('nodeHover');
                        });

                        if (isIn(entity.id, page.linkingElements)) {
                            div.addClass('nodeActive');
                        }

                    });

                    dialog.append('<h3>Files</h3><div class="linkBox" id="foldersToLink"></div><div class="linkBox" id="filesToLink"></div>');

                    var filesToLink = $('#filesToLink');
                    var foldersToLink = $('#foldersToLink');

                    Structr.addPager(foldersToLink, true, 'Folder', function(folder) {

                        if (folder.files.length + folder.folders.length === 0) {
                            return;
                        }

                        foldersToLink.append('<div class="node folder ' + folder.id + '_"><img class="typeIcon" src="icon/folder.png">'
                                + '<b title="' + folder.name + '" class="name_">' + folder.name + '</b></div>');

                        var div = $('.' + folder.id + '_', foldersToLink);
                        div.on('click', function(e) {
                            if (!div.children('.node').length) {
                                _Elements.expandFolder(e, entity, folder);
                            } else {
                                div.children('.node').remove();
                            }
                        }).css({
                            cursor: 'pointer'
                        }).hover(function() {
                            $(this).addClass('nodeHover');
                        }, function() {
                            $(this).removeClass('nodeHover');
                        });

                        if (isIn(entity.id, folder.linkingElements)) {
                            //console.log(entity.id, file.linkingElements);
                            div.addClass('nodeActive');
                        }

                    });

                    Structr.addPager(filesToLink, true, 'File', function(file) {

                        filesToLink.append('<div class="node file ' + file.id + '_"><img class="typeIcon" src="' + _Files.getIcon(file) + '">'
                                + '<b title="' + file.name + '" class="name_">' + file.name + '</b></div>');

                        var div = $('.' + file.id + '_', filesToLink);

                        div.on('click', function(e) {
                            e.stopPropagation();
                            Command.link(entity.id, file.id);
                            $('#dialogBox .dialogText').empty();
                            _Pages.reloadPreviews();
                            $.unblockUI({
                                fadeOut: 25
                            });
                        }).css({
                            cursor: 'pointer'
                        }).hover(function() {
                            $(this).addClass('nodeHover');
                        }, function() {
                            $(this).removeClass('nodeHover');
                        });

                        if (isIn(entity.id, file.linkingElements)) {
                            //console.log(entity.id, file.linkingElements);
                            div.addClass('nodeActive');
                        }

                    });

                }

                if (entity.tag === 'img' || entity.tag === 'link' || entity.tag === 'a') {

                    dialog.append('<h3>Images</h3><div class="linkBox" id="imagesToLink"></div>');

                    var imagesToLink = $('#imagesToLink');

                    Structr.addPager(imagesToLink, false, 'Image', function(image) {

                        imagesToLink.append('<div class="node file ' + image.id + '_"><img class="typeIcon" src="' + _Images.getIcon(image) + '">'
                                + '<b title="' + image.name + '" class="name_">' + image.name + '</b></div>');

                        var div = $('.' + image.id + '_', imagesToLink);

                        div.on('click', function(e) {
                            e.stopPropagation();
                            Command.link(entity.id, image.id);
                            $('#dialogBox .dialogText').empty();
                            _Pages.reloadPreviews();
                            $.unblockUI({
                                fadeOut: 25
                            });
                        }).css({
                            cursor: 'pointer'
                        }).hover(function() {
                            $(this).addClass('nodeHover');
                        }, function() {
                            $(this).removeClass('nodeHover');
                        });

                        if (isIn(entity.id, image.linkingElements)) {
                            //console.log(entity.id, file.linkingElements);
                            div.addClass('nodeActive');
                        }

                    });

                }


            });
        }
        return div;
    },
    expandFolder: function(e, entity, folder, callback) {
        if (folder.files.length + folder.folders.length === 0) {
            return;
        }

        var div = $('.' + folder.id + '_');
        div.css({'border': '1px solid #ccc', 'backgroundColor': '#f5f5f5'});

        div.children('b').on('click', function() {
            $(this).siblings('.node.sub').remove();
        });

        $.each(folder.folders, function(i, subFolder) {
            e.stopPropagation();
            $('.' + folder.id + '_').append('<div class="clear"></div><div class="node folder sub ' + subFolder.id + '_"><img class="typeIcon" src="icon/folder.png">'
                    + '<b title="' + subFolder.name + '" class="name_">' + subFolder.name + '</b></div>');
            var subDiv = $('.' + subFolder.id + '_');

            subDiv.on('click', function(e) {
                if (!subDiv.children('.node').length) {
                    e.stopPropagation();
                    Command.get(subFolder.id, function(node) {
                        _Elements.expandFolder(e, entity, node, callback);
                    });
                } else {
                    subDiv.children('.node').remove();
                }
                return false;
            }).css({
                cursor: 'pointer'
            }).hover(function() {
                $(this).addClass('nodeHover');
            }, function() {
                $(this).removeClass('nodeHover');
            });

        });

        $.each(folder.files, function(i, file) {
            $('.' + folder.id + '_').append('<div class="clear"></div><div class="node file sub ' + file.id + '_"><img class="typeIcon" src="' + _Files.getIcon(file) + '">'
                    + '<b title="' + file.name + '" class="name_">' + file.name + '</b></div>');
            var div = $('.' + file.id + '_');
            div.on('click', function(e) {
                e.stopPropagation();
                Command.link(entity.id, file.id);
                $('#dialogBox .dialogText').empty();
                _Pages.reloadPreviews();
                $.unblockUI({
                    fadeOut: 25
                });
            }).css({
                cursor: 'pointer'
            }).hover(function() {
                $(this).addClass('nodeHover');
            }, function() {
                $(this).removeClass('nodeHover');
            });
        });

        return false;
    }
};