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

var elements;

var _Elements = {
    icon: 'icon/brick.png',
    icon_comp: 'icon/package.png',
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
            'elements': ['html', 'content']
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
     * Reload HTML palette
     * 
     * @returns {undefined}
     */
    reloadPalette: function() {

        palette.empty();

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
                    helper: 'clone'
                });
            });

        });
    },
    /**
     * Reload components in component area
     */
    reloadComponents: function() {

        components.empty();

        Command.getComponents(1000, 1, 'name', 'asc', function(entity) {

            var el = _Elements.appendElementElement(entity, components, true);

            // expand
            if (isExpanded(entity.id)) {
                _Entities.ensureExpanded(el);
            }

        });

    },
    /**
     * Reload unattached nodes in elements area
     */
    reloadUnattachedNodes: function() {

        elements.empty();

        Command.listUnattachedNodes(1000, 1, 'name', 'asc');

    },
    componentNode: function(id) {
        return $($('#componentId_' + id)[0]);
    },
    /**
     * Create a DOM node and append to the appropriate parent
     */
    appendElementElement: function(entity, refNode, refNodeIsParent) {
        log('_Elements.appendElementElement', entity);

        var hasChildren = entity.childrenIds && entity.childrenIds.length;
        var isComponent = entity.syncedNodes.length;

        var isMasterComponent = (isComponent && hasChildren && refNode !== components);

        var parent;
        if (refNodeIsParent) {
            parent = refNode;
        } else {
            parent = entity.parent && entity.parent.id ? Structr.node(entity.parent.id) : elements;
        }

        log('appendElementElement parent, refNode, redNodeIsParent', parent, refNode, refNodeIsParent);
        if (!parent)
            return false;

        _Entities.ensureExpanded(parent);

        var id = entity.id;

        var html = '<div id="' + (isMasterComponent ? 'componentId_' : 'id_') + id + '" class="node element"></div>';

        if (refNode && !refNodeIsParent) {
            refNode.before(html);
        } else {
            parent.append(html);
        }

        var div = (isMasterComponent ? _Elements.componentNode(id) : Structr.node(id));

        log('Element appended (div, parent)', div, parent);

        if (!div)
            return false;

        var displayName = entity.name ? entity.name : (entity.tag ? entity.tag : '[' + entity.type + ']');

        var icon = isComponent ? _Elements.icon_comp : _Elements.icon;

        div.append('<img class="typeIcon" src="' + icon + '">'
                + '<b title="' + displayName + '" class="tag_ name_">' + displayName + '</b><span class="id">' + entity.id + '</span>'
                + (entity._html_id ? '<span class="_html_id_">#' + entity._html_id.replace(/\${.*}/g, '${…}') + '</span>' : '')
                + (entity._html_class ? '<span class="_html_class_">.' + entity._html_class.replace(/\${.*}/g, '${…}').replace(/ /g, '.') + '</span>' : '')
                + '</div>');

        _Entities.appendExpandIcon(div, entity, !isMasterComponent && hasChildren);

        $('.typeIcon', div).on('mousedown', function(e) {
            e.stopPropagation();
        });

        _Entities.appendAccessControlIcon(div, entity);
        div.append('<img title="Delete ' + entity.tag + ' element ' + entity.id + '" alt="Delete ' + entity.tag + ' element ' + entity.id + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            _Entities.deleteNode(this, entity);
        });

        _Entities.setMouseOver(div);
        _Entities.appendEditPropertiesIcon(div, entity);
        _Entities.appendDataIcon(div, entity);

        if (entity.tag === 'a' || entity.tag === 'link') {
            //console.log(entity);
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
                var dialog = $('#dialogBox .dialogText');
                var dialogMsg = $('#dialogMsg');

                dialog.empty();
                dialogMsg.empty();

                dialog.append('<p>Click on a page to establish a hyperlink between this element and the target resource.</p>');

                var headers = {};
                headers['X-StructrSessionToken'] = token;

                $.ajax({
                    url: rootUrl + 'pages/ui?pageSize=100',
                    async: true,
                    dataType: 'json',
                    contentType: 'application/json; charset=utf-8',
                    headers: headers,
                    success: function(data) {

                        $(data.result).each(function(i, res) {

                            dialog.append('<div class="node page ' + res.id + '_"><img class="typeIcon" src="icon/page.png">'
                                    + '<b title="' + res.name + '" class="name_">' + res.name + '</b></div>');

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
                        $(data.result).each(function(i, file) {

                            dialog.append('<div class="node file ' + file.id + '_"><img class="typeIcon" src="' + _Files.getIcon(file) + '">'
                                    + '<b title="' + file.name + '" class="name_">' + file.name + '</b></div>');

                            var div = $('.' + file.id + '_', dialog);

                            div.on('click', function(e) {
                                e.stopPropagation();
                                Command.link(entity.id, file.id);
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

                            if (isIn(entity.id, file.linkingElements)) {
                                //console.log(entity.id, file.linkingElements);
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