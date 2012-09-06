/*
 *  Copyright (C) 2010-2012 Axel Morgner
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

var buttonClicked;

var _Entities = {
    
    booleanAttrs : ['visibleToPublicUsers', 'visibleToAuthenticatedUsers', 'hidden', 'deleted', 'blocked', 'frontendUser', 'backendUser'],
    numberAttrs : ['position', 'size'],
    dateAttrs : ['createdDate', 'lastModifiedDate', 'visibilityStartDate', 'visibilityEndDate'],
    hiddenAttrs : ['deleted', 'ownerId', 'owner', 'group', 'categories', 'tag', 'createdBy', 'visibilityStartDate', 'visibilityEndDate', 'parentFolder', 'url', 'relativeFilePath', 'path', 'elements', 'linkingElements', 'components', 'paths', 'parents'],
    readOnlyAttrs : ['lastModifiedDate', 'createdDate', 'id', 'checksum', 'size', 'version'],
    
    changeBooleanAttribute : function(attrElement, value) {

        if (debug) console.log('Change boolean attribute ', attrElement, ' to ', value);

        if (value == true) {
            attrElement.removeClass('disabled');
            attrElement.addClass('enabled');
            attrElement.prop('checked', 'checked');
        } else {
            attrElement.removeClass('enabled');
            attrElement.addClass('disabled');
            attrElement.prop('checked', '');
        }

    },

    renderTree : function(parent, rootId) {
        if (debug) console.log('Entities.renderTree');
        var children = parent.children;
					
        if (children && children.length > 0) {
            $(children).each(function(i,child) {
                if (debug) console.log(child);
                if (child.type == 'Page') {
                    _Pages.appendPageElement(child, parent.id, rootId);
                } else if (child.type == 'Component') {
                    _Pages.appendElementElement(child, parent.id, rootId);
                } else if (child.type == 'Content') {
                    _Pages.appendContentElement(child, parent.id, rootId);
                } else if (child.type == 'Folder') {
                    var entity = child;
                    if (debug) console.log('Render Tree: ' , entity);
                    var folderElement = _Files.appendFolderElement(child, parent.id);
                    var files = entity.files;
                    if (files && files.length > 0) {
                        disable($('.delete_icon', folderElement)[0]);
                        $(files).each(function(i, file) {
                            _Files.appendFileElement(file, entity.id);
                        });
                    }
                } else {
                    _Pages.appendElementElement(child, parent.id, rootId);
                }
				
                _Entities.renderTree(child, rootId);
            });
        }
    },

    reloadChildren : function(id, componentId, pageId) {
        var el = Structr.node(id, null, componentId, pageId);
        
        if (debug) console.log('reloadChildren', el);
        
        $(el).children('.node').remove();
        _Entities.resetMouseOverState(el);
        
        Command.children(id, componentId, pageId);
        
    },

    appendObj : function(entity, parentId, componentId, pageId, add, hasChildren, treeAddress) {

        if (debug) console.log('_Entities.appendObj: ', entity, parentId, componentId, pageId, add, hasChildren, treeAddress);

        var lastAppendedObj;
        var expand = false;

        if (entity.type == 'User') {

            lastAppendedObj = _UsersAndGroups.appendUserElement(entity, parentId, add);
            
        } else if (entity.type == 'Group') {
            
            lastAppendedObj = _UsersAndGroups.appendGroupElement(entity, hasChildren);
            expand = isExpanded(entity.id);

        } else if (entity.type == 'Page') {
            
            lastAppendedObj = _Pages.appendPageElement(entity, hasChildren);
            expand = isExpanded(entity.id);

        } else if (entity.type == 'Component') {

            lastAppendedObj = _Pages.appendElementElement(entity, parentId, componentId, pageId, true, true, treeAddress);
            expand = isExpanded(getElementPath(lastAppendedObj));

        } else if (entity.type == 'Content') {

            if (debug) console.log('appending content element', entity, parentId, componentId, pageId, treeAddress);
            lastAppendedObj = _Pages.appendContentElement(entity, parentId, componentId, pageId, treeAddress);

        } else if (entity.type == 'Folder') {

            lastAppendedObj = _Files.appendFolderElement(entity, parentId, hasChildren);
            expand = isExpanded(entity.id);

        } else if (entity.type == 'Image') {
            
            if (debug) console.log('Image:', entity);
            _Files.uploadFile(entity);
            
            lastAppendedObj = _Files.appendImageElement(entity, parentId, add, hasChildren, true);
            
        } else if (entity.type == 'File') {
            
            if (debug) console.log('File: ', entity);
            _Files.uploadFile(entity);
            
            lastAppendedObj = _Files.appendFileElement(entity, parentId, add, hasChildren, false);
            
        } else if (entity.type == 'TypeDefinition') {
            
            if (debug) console.log('TypeDefinition: ', entity);
            lastAppendedObj = _Types.appendTypeElement(entity);
            
        } else {

            if (debug) console.log('Entity: ', entity);
            lastAppendedObj = _Pages.appendElementElement(entity, parentId, componentId, pageId, add, hasChildren, treeAddress);
            expand = isExpanded(getElementPath(lastAppendedObj));
        }

        if (debug) console.log('lastAppendedObj', lastAppendedObj);

        if (lastAppendedObj) {
            
            var t = getElementPath(lastAppendedObj);
            if (debug) console.log(t);
            
            if (expand) {
                if (debug) console.log('expand', lastAppendedObj);
                _Entities.ensureExpanded(lastAppendedObj);
            }

            var parent = lastAppendedObj.parent();
            if (debug) console.log('lastAppendedObj.parent()', parent);
            if (parent.children('.node') && parent.children('.node').length==1) {
                
                if (debug) console.log('parent of last appended object has children');

                //addExpandedNode(treeAddress);
                var ent = Structr.entityFromElement(parent);
                if (debug) console.log('entity', ent);
                ent.pageId = pageId;
                _Entities.appendExpandIcon(parent, ent, true, true);

            }
        }

    },

    deleteNode : function(button, entity) {
        buttonClicked = button;
        if (isDisabled(button)) return;
        if (debug) console.log('deleteNode');
        Structr.confirmation('<p>Delete ' + entity.type.toLowerCase() + ' \'' + entity.name + '\'?</p>',
            function() {
                if (Command.deleteNode(entity.id)) {
                    $.unblockUI({
                        fadeOut: 25
                    });
                }
            });
    },

    listContainingNodes : function(entity, node) {

        if (debug) console.log('listContainingNodes', entity, getElementPath(node));

        dialog.empty();
        Structr.dialog('Click on element to remove it from the page tree',
            function() {
                return true;
            },
            function() {
                return true;
            });
               
        dialog.append('<p>Hover your mouse over the element instance to detect which node is being removed.</p>')
        var headers = {};
        headers['X-StructrSessionToken'] = token;
        if (debug) console.log('headers', headers);
        if (debug) console.log('showProperties URL: ' + rootUrl + entity.id, headers);
            
        $.ajax({
            url: rootUrl + entity.id,
            async: true,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            headers: headers,
            success: function(data) {
                if (data.result) {
                    dialog.append('<table class="props containingNodes ' + entity.id + '_"></table>');
                    var cont = $('.' + entity.id + '_', dialog);

                    $(data.result.paths).each(function(i, path) {
                        if (debug) console.log(path);

                        //                        var pageId = path.substring(32);
                        
                        var displayName = entity.tag ? entity.tag : '[' + entity.type + ']';
                        
                        cont.append('<tr><td class="' + path + '">'
                            + '<div style="display: inline-block" class="node ' + entity.id + '_">'
                            + '<img style="float: left" class="remove_icon" src="' + Structr.delete_icon +  '"><b class="tag_">' + displayName + '</b></div></td>'
                            + '</tr>');
                                    
                        //Command.getProperty(parentId, 'tag', '.parent_' + n + '_'+ parentId);
                        //Command.getProperty(entity.id, 'name', '.'+ path);
                                    
                                    
                        var node = $('div.' + entity.id + '_', $('.' + path));
                                    
                        node.hover(function() {
                            $('#_' + path).addClass('nodeHover')
                        },
                        function() {
                            $('#_' + path).removeClass('nodeHover')
                        }
                        );
                                    
                        //_Entities.setMouseOver(parent);
                        //_Entities.setMouseOver(node, parentElement);
                                    
                        node.css({
                            'cursor': 'pointer'
                        }).on('click', function(e) {
                            //console.log('Command.removeSourceFromTarget(entity.id, startNodeId, null, key, pos)', entity.id, parentId, null, key, pos);
                            if (debug) console.log(path);
                            Command.remove(entity.id, path);
                                        
                            $('.' + path).parent('tr').remove();
                                        
                        });

                    });
                                  
                }

            }
        });        
    },

    showProperties : function(entity) {

        var views;
	    
        if (entity.type == 'Content') {
            views = ['all', 'in', 'out' ];
        } else {
            views = ['all', 'in', 'out', '_html_'];
        }

        dialog.empty();
        Structr.dialog('Edit Properties of ' + entity.id,
            function() {
                return true;
            },
            function() {
                return true;
            }
            );

        dialog.append('<div id="tabs"><ul></ul></div>');

        $(views).each(function(i, view) {
            var tabs = $('#tabs', dialog);

            var tabText;
            if (view == 'all') {
                tabText = 'Node';
            } else if (view == '_html_') {
                tabText = 'HTML';
            } else if (view == 'in') {
                tabText = 'Incoming';
            } else if (view == 'out') {
                tabText = 'Outgoing';
            } else {
                tabText = 'Other';
            }

            $('ul', tabs).append('<li class="' + (view == 'all' ? 'active' : '') + '" id="tab-' + view + '">' + tabText + '</li>');

            tabs.append('<div id="tabView-' + view + '"><br></div>');

            var tab = $('#tab-' + view);

            tab.on('click', function(e) {
                e.stopPropagation();
                var self = $(this);
                tabs.children('div').hide();
                $('li', tabs).removeClass('active');
                $('#tabView-' + view).show();
                self.addClass('active');
            });

            var tabView = $('#tabView-' + view);
            if (view != 'all') tabView.hide();

            var headers = {};
            headers['X-StructrSessionToken'] = token;
            if (debug) console.log('headers', headers);
            if (debug) console.log('showProperties URL: ' + rootUrl + entity.id + (view ? '/' + view : ''), headers);
            
            $.ajax({
                url: rootUrl + entity.id + (view ? '/' + view : '') + '?pageSize=10',
                async: true,
                dataType: 'json',
                contentType: 'application/json; charset=utf-8',
                headers: headers,
                success: function(data) {
                    //element.append('<div class="sep"></div>');
                    //element.append('<table class="props"></table>');
                    if (debug) console.log(data.result);
                    
                    // Default: Edit node id
                    var id = entity.id;
                    // ID of graph object to edit
                    $(data.result).each(function(i, res) {

                        // reset id for each object group
                        id = entity.id;
			
                        var keys = Object.keys(res);

                        if (debug ) console.log('keys', keys);

                        //			if (view == 'in' || view == 'out') {
                        //			    tabView.append('<br><h3>Relationship ' + res['id']+ '</h3>')
                        //			}
				
                        tabView.append('<table class="props ' + view + '_' + res['id'] +'"></table>');

                        var props = $('.props.' + view + '_' + res['id'], tabView);
				
                        $(keys).each(function(i, key) {

                            if (view == '_html_') {
                                
                                if (key != 'id') {
                                
                                    props.append('<tr><td class="key">' + key.replace(view, '') + '</td><td class="value ' + key + '_">' + formatValue(key, res[key]) + '</td></tr>');
                                
                                }
                                
                            } else if (view == 'in' || view == 'out') {
                                
                                if (key == 'id') {
                                    // set ID to rel ID
                                    id = res[key];
                                //console.log('Set ID to relationship ID', id);
                                }
                                
                                props.append('<tr><td class="key">' + key + '</td><td rel_id="' + id + '" class="value ' + key + '_">' + formatValue(key, res[key]) + '</td></tr>');
                                                               
                            } else {
                                
                                if (!isIn(key, _Entities.hiddenAttrs)) {
                                    
                                    if (isIn(key, _Entities.readOnlyAttrs)) {
                                        
                                        props.append('<tr><td class="key">' + formatKey(key) + '</td><td class="value readonly"><input type="text" class="readonly" readonly value="' + res[key] + '"></td></tr>');
                                
                                    } else if (isIn(key, _Entities.booleanAttrs)) {
                                    
                                        props.append('<tr><td class="key">' + formatKey(key) + '</td><td><input type="checkbox" class="' + key + '_"></td></tr>');
                                        var checkbox = $(props.find('.' + key + '_'));
                                        checkbox.on('change', function() {
                                            if (debug) console.log('set property', id, key, checkbox.attr('checked') == 'checked');
                                            Command.setProperty(id, key, checkbox.attr('checked') == 'checked');
                                        });
                                        Command.getProperty(id, key, '#dialogBox');
                                
                                    //                                } else if (isIn(key, _Entities.numberAttrs)) {
                                    } else if (isIn(key, _Entities.dateAttrs)) {
                                    
                                        if (!res[key] || res[key] == 'null') {
                                            res[key] = '';
                                        }
                                    
                                        props.append('<tr><td class="key">' + formatKey(key) + '</td><td class="value ' + key + '_"><input class="dateField" name="' + key + '" type="text" value="' + res[key] + '"></td></tr>');
                                    
                                        var dateField = $(props.find('.dateField'));
                                        dateField.datetimepicker({
                                            showSecond: true,
                                            timeFormat: 'hh:mm:ssz',
                                            dateFormat: 'yy-mm-dd',
                                            separator: 'T'
                                        });
                                    //dateField.datepicker();
                                    
                                    } else {
                                
                                        props.append('<tr><td class="key">' + formatKey(key) + '</td><td class="value ' + key + '_">' + formatValue(key, res[key]) + '</td></tr>');
                                    }
                                
                                }
                            }

                        });
                        
                        props.append('<tr><td class="key"><input type="text" class="newKey" name="key"></td><td class="value"><input type="text" value=""></td></tr>');

                        $('.props tr td.value input', dialog).each(function(i,v) {
                            
                            var input = $(v);
                            
                            var relId = input.parent().attr('rel_id');
                            //console.log('attaching events for saving attrs of relationship', relId);

                            if (!input.hasClass('readonly') && !input.hasClass('newKey')) {
                            
                                input.on('focus', function() {
                                    input.addClass('active');
                                });

                                input.on('change', function() {
                                    input.data('changed', true);
                                    _Pages.reloadPreviews();
                                });

                                input.on('focusout', function() {
                                    console.log('relId', relId);
                                    var objId = relId ? relId : id;
                                    console.log('set properties of obj', objId);
                                    
                                    var keyInput = input.parent().parent().children('td').first().children('input');
                                    console.log(keyInput);
                                    if (keyInput && keyInput.length) {
                                    
                                        // new key
                                        console.log('new key: Command.setProperty(', objId, keyInput.val(), input.val());
                                        Command.setProperty(objId, keyInput.val(), input.val());
                                        
                                        
                                    } else {
                                        
                                        // existing key
                                        console.log('existing key: Command.setProperty(', objId, input.prop('name'), input.val());
                                        Command.setProperty(objId, input.prop('name'), input.val());
                                        
                                    }
                                    
                                    
                                    input.removeClass('active');
                                    input.parent().children('.icon').each(function(i, img) {
                                        $(img).remove();
                                    });
                                });
                            
                            }

                        });
                    });

                }
            });
            debug = false;
        });

    },

    appendAccessControlIcon: function(parent, entity, hide) {

        var keyIcon = $('.key_icon', parent);
        var newKeyIcon = '<img title="Access Control and Visibility" alt="Access Control and Visibility" class="key_icon button" src="' + Structr.key_icon + '">';
        if (!(keyIcon && keyIcon.length)) {
            parent.append(newKeyIcon);
            keyIcon = $('.key_icon', parent)
            if (hide) keyIcon.hide();
            
            keyIcon.on('click', function(e) {
                e.stopPropagation();
                Structr.dialog('Access Control and Visibility', function() {}, function() {});
                var dt = $('#dialogBox .dialogText');

                _Entities.appendSimpleSelection(dt, entity, 'users', 'Owner', 'ownerId');
                
                dt.append('<h3>Visibility</h3><div class="' + entity.id + '_"><button class="switch disabled visibleToPublicUsers_">Public (visible to anyone)</button><button class="switch disabled visibleToAuthenticatedUsers_">Authenticated Users</button></div>');
                
                dt.append('<div>Apply Recursively? <input id="recursive" type="checkbox" name="recursive"></div>');
                
                var publicSwitch = $('.visibleToPublicUsers_');
                var authSwitch = $('.visibleToAuthenticatedUsers_');

                Command.getProperty(entity.id, 'visibleToPublicUsers', '#dialogBox');
                Command.getProperty(entity.id, 'visibleToAuthenticatedUsers', '#dialogBox');

                if (debug) console.log(publicSwitch);
                if (debug) console.log(authSwitch);
                
                publicSwitch.on('click', function(e) {
                    e.stopPropagation();
                    var rec = $('#recursive', dt).is(':checked');
                    if (debug) console.log('Toggle switch', publicSwitch.hasClass('disabled'))
                    Command.setProperty(entity.id, 'visibleToPublicUsers', publicSwitch.hasClass('disabled'), rec);
                });

                authSwitch.on('click', function(e) {
                    e.stopPropagation();
                    var rec = $('#recursive', dt).is(':checked');
                    if (debug) console.log('Toggle switch', authSwitch.hasClass('disabled'))
                    Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', authSwitch.hasClass('disabled'), rec);
                });

            });
        
            keyIcon.on('mouseover', function(e) {
                var self = $(this);
                self.show();

            });
        }
    },

    appendSimpleSelection : function(el, entity, type, title, key) {
        
        el.append('<h3>' + title + '</h3><p id="' + key + 'Box"></p>');
                
        var element = $('#' + key + 'Box');
                
        element.append('<span class="' + entity.id + '_"><select class="' + key + '_" id="' + key + 'Select">');
                
        var selectElement = $('#' + key + 'Select');
        
        selectElement.append('<option></option>')
        
        var headers = {};
        headers['X-StructrSessionToken'] = token;
        $.ajax({
            url: rootUrl + type + '/all?pageSize=100',
            async: false,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            headers: headers,
            success: function(data) {
                $(data.result).each(function(i, result) {
                    if (debug) console.log(result);
                    selectElement.append('<option value="' + result.id + '">' + result.name + '</option>');
                });
            }
        });                
                
        element.append('</select></span>');

        if (debug) console.log('Command.getProperty(', entity.id, key, '#' + key + 'Box', ')');
        Command.getProperty(entity.id, key, '#' + key + 'Box');
        var select = $('#' + key + 'Select', element);
        select.on('change', function() {
            Command.setProperty(entity.id, key, select.val());
        });          
    },

    appendEditPropertiesIcon : function(parent, entity) {

        var editIcon = $('.edit_props_icon', parent);

        if (!(editIcon && editIcon.length)) {
            parent.append('<img title="Edit Properties" alt="Edit Properties" class="edit_props_icon button" src="' + '/structr/icon/application_view_detail.png' + '">');
            editIcon = $('.edit_props_icon', parent)
        }
        editIcon.on('click', function(e) {
            e.stopPropagation();
            if (debug) console.log('showProperties', entity);
            _Entities.showProperties(entity);
        });
    },

    appendExpandIcon : function(el, entity, hasChildren, expand) {

        var button = $(el.children('.expand_icon').first());
        if (button && button.length) {
            if (debug) console.log('Expand icon already existing');
            return;
        }

        if (hasChildren) {

            var typeIcon = $(el.children('.typeIcon').first());

            var icon = expand ? Structr.expanded_icon : Structr.expand_icon;
            //var icon = Structr.expand_icon;
            if (debug) console.log(icon);
            
            typeIcon.css({
                paddingRight: 0 + 'px'
            })
            .after('<img title="Expand \'' + entity.name + '\'" alt="Expand \'' + entity.name + '\'" class="expand_icon" src="' + icon + '">');

            button = $(el.children('.expand_icon').first());

            if (button) {
                
                button.on('click', function(e) {
                    
                    if (debug) console.log('expand icon clicked');
                    
                    e.stopPropagation();
                    _Entities.toggleElement($(this).parent('.node'));
                    
                });
                
                $(el).on('click', function(e) {
                    
                    if (debug) console.log('node clicked');
                    _Entities.toggleElement(this);
                    
                });

                // Prevent expand icon from being draggable
                button.on('mousedown', function(e) {
                    e.stopPropagation();
                });
                
                var elId = $(el).attr('id');
                
                //var treeAddress = elId ? elId.substring(1) : undefined;
                
                if (expand) {
                    _Entities.ensureExpanded(el);
                }
            }

        } else {
            el.children('.typeIcon').css({
                paddingRight: 11 + 'px'
            });
        }

    },

    removeExpandIcon : function(el) {
        var button = $(el.children('.expand_icon').first());
        button.remove();
        el.children('.typeIcon').css({
            paddingRight: 11 + 'px'
        });
    },

    setMouseOver : function(el, parentElement) {
        if (!el || !el.children) return;

        el.on('click', function(e) {
            e.stopPropagation();
        });

        el.children('b.name_').on('click', function(e) {
            e.stopPropagation();
            _Entities.makeNameEditable(el);
        });

        el.on({
            mouseover: function(e) {
                e.stopPropagation();
                var self = $(this);
                var nodeId = getId(el);
                //window.clearTimeout(timer[nodeId]);
                //		console.log('setMouseOver', nodeId);
                var nodes;
                if (parentElement) {
                    nodes = $('.' + nodeId + '_', parentElement);
                } else {
                    nodes = $('.' + nodeId + '_');
                }
                var page = $(el).closest('.page');
                if (page.length) {
                    var pageId = getId(page);
                    var previewNodes = $('#preview_' + pageId).contents().find('[structr_element_id]');
                    previewNodes.each(function(i,v) {
                        var self = $(v);
                        var sid = self.attr('structr_element_id');
                        if (sid == nodeId) {
                            self.addClass('nodeHover');
                        }
                    });

                //		    _Entities.children(nodeId, resId);

                }
                nodes.addClass('nodeHover');
                self.children('img.button').show();
            },
            mouseout: function(e) {
                e.stopPropagation();
                _Entities.resetMouseOverState(this);
            }
        });
    },

    resetMouseOverState : function(element) {
        var el = $(element);
        el.removeClass('nodeHover');
        el.children('img.button').hide();
        var nodeId = getId(el);
        var nodes = $('.' + nodeId + '_');
        //	timer[nodeId] = window.setTimeout(function() {
        //	    el.children('.node').remove();
        //	}, 1000);
        nodes.removeClass('nodeHover');
        var page = $(el).closest('.page');
        if (page.length) {
            var resId = getId(page);
            //		    console.log('setMouseOver pageId', resId);
            var previewNodes = $('#preview_' + resId).contents().find('[structr_element_id]');
            previewNodes.each(function(i,v) {
                var self = $(v);
                var sid = self.attr('structr_element_id');
                if (sid == nodeId) {
                    if (debug) console.log(sid);
                    self.removeClass('nodeHover');
                    if (debug) console.log(self);
                }
            });
        }
    },

    ensureExpanded : function(element) {
        
        var el = $(element);
        var b;
        var src = el.prop('src');
        
        var elId = el.attr('id');
        
        if (debug) console.log(el);
        
        var treeAddress = elId ? elId.substring(1) : undefined;
        
        if (debug) console.log('ensureExpanded: elId, treeAddress', elId, treeAddress);
        addExpandedNode(treeAddress);
        
        b = el.children('.expand_icon').first();
        src = b.prop('src');
        
        if (!src) return;
        
        if (src.endsWith('icon/tree_arrow_down.png')) {
            return;
        } else {
            
            var id = getId(el);
            var compId = getId(el.closest('.component'));
            var pageId = getId(el.closest('.page'));

            Command.children(id, compId, pageId, treeAddress);
            b.prop('src', 'icon/tree_arrow_down.png');
            
        }
      
        
    },

    toggleElement : function(element, expanded) {

        var el = $(element);
        var b;
        var src = el.prop('src');
        
        var elId = el.attr('id');
        
        if (debug) console.log(el);
        
        var treeAddress = elId ? elId.substring(1) : undefined;
        
        if (debug) console.log('toggleElement: treeAddress', treeAddress);
        
        b = el.children('.expand_icon').first();
        src = b.prop('src');
        
        if (!src) return;
        
        if (src.endsWith('icon/tree_arrow_down.png')) {
            el.children('.node').remove();
            b.prop('src', 'icon/tree_arrow_right.png');

            removeExpandedNode(treeAddress);
        } else {
            
            var id = getId(el);
            var compId = getId(el.closest('.component'));
            var pageId = getId(el.closest('.page'));

            if (!expanded) Command.children(id, compId, pageId, treeAddress);
            b.prop('src', 'icon/tree_arrow_down.png');

            addExpandedNode(treeAddress);
        }

    },

    makeNameEditable : function(element) {
        //element.off('dblclick');
        element.off('hover');
        var oldName = $.trim(element.children('b.name_').first().text());
        //console.log('oldName', oldName);
        element.children('b.name_').replaceWith('<input type="text" size="' + (oldName.length+4) + '" class="newName_" value="' + oldName + '">');
        element.find('.button').hide();

        var input = $('input', element);

        input.focus().select();

        input.on('blur', function() {
            var self = $(this);
            var newName = self.val();
            Command.setProperty(getId(element), "name", newName);
            self.replaceWith('<b class="name_">' + newName + '</b>');
            $('.name_', element).on('click', function(e) {
                e.stopPropagation();
                _Entities.makeNameEditable(element);
            });
            _Pages.reloadPreviews();
        });

        input.keypress(function(e) {
            if (e.keyCode == 13) {
                var self = $(this);
                var newName = self.val();
                Command.setProperty(getId(element), "name", newName);
                self.replaceWith('<b class="name_">' + newName + '</b>');
                $('.name_', element).on('click', function(e) {
                    e.stopPropagation();
                    _Entities.makeNameEditable(element);
                });
                _Pages.reloadPreviews();
            }
        });

        element.off('click');

    }


};
