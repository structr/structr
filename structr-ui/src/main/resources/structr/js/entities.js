/*
 *  Copyright (C) 2010-2013 Axel Morgner
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
    hiddenAttrs : ['base', 'deleted', 'ownerId', 'owner', 'group', 'categories', 'tag', 'createdBy', 'visibilityStartDate', 'visibilityEndDate', 'parentFolder', 'url', 'path', 'elements', 'components', 'paths', 'parents'],
    readOnlyAttrs : ['lastModifiedDate', 'createdDate', 'id', 'checksum', 'size', 'version', 'relativeFilePath'],
    
    changeBooleanAttribute : function(attrElement, value) {

        //console.log('Change boolean attribute ', attrElement, ' to ', value);

        if (value == true) {
            attrElement.removeClass('disabled').addClass('enabled').prop('checked', true);
        } else {
            attrElement.removeClass('enabled').addClass('disabled').prop('checked', false);
        }

    },

    reloadChildren : function(id) {
        var el = Structr.node(id);
        
        log('reloadChildren', el);
        
        $(el).children('.node').remove();
        _Entities.resetMouseOverState(el);
        
        Command.children(id);
        
    },

    deleteNode : function(button, entity) {
        buttonClicked = button;
        if (isDisabled(button)) return;
        
        Structr.confirmation('<p>Delete ' + entity.type + ' \'' + entity.name + '\' [' + entity.id + '] ?</p>',
            function() {
                Command.deleteNode(entity.id);
                $.unblockUI({
                    fadeOut: 25
                });
            });
    },

    showSyncDialog : function(source, target) {
        Structr.dialog('Sync between ' + source.id + ' and ' + target.id, function() { return true; }, function() { return true; });
        
        dialog.append('<div><input type="radio" name="syncMode" value="none"><label for="unidir">None</label></div>');
        dialog.append('<div><input type="radio" name="syncMode" value="unidir"><label for="unidir">Uni-directional (master/slave)</label></div>');
        dialog.append('<div><input type="radio" name="syncMode" value="bidir"><label for="unidir">Bi-directional</label></div>');
        
        $('input[name=syncMode]:radio', dialog).on('change', function() {
           Command.setSyncMode(source.id, target.id, $(this).val());
        });
        
    },

    showDataDialog : function(entity) {

        Structr.dialog('Edit Data Settings of ' + entity.id, function() { return true; }, function() { return true; });

        dialogText.append('<div class="' + entity.id + '_"><button class="switch disabled renderDetails_">Render single object only in details mode</button></div>');
        var detailsSwitch = $('.renderDetails_');

        _Entities.changeBooleanAttribute(detailsSwitch, entity.renderDetails);

        detailsSwitch.on('click', function(e) {
            e.stopPropagation();
            entity.setProperty('renderDetails', detailsSwitch.hasClass('disabled'), false, function() {
                _Entities.changeBooleanAttribute(detailsSwitch, entity.renderDetails);
            });
        });
        
        dialogText.append('<div class="' + entity.id + '_"><button class="switch disabled hideOnIndex_">Hide in index mode</button></div>');
        var indexSwitch = $('.hideOnIndex_');

        _Entities.changeBooleanAttribute(indexSwitch, entity.hideOnIndex);

        indexSwitch.on('click', function(e) {
            e.stopPropagation();
            entity.setProperty('hideOnIndex', indexSwitch.hasClass('disabled'), false, function() {
                _Entities.changeBooleanAttribute(indexSwitch, entity.hideOnIndex);
            });
        });

        dialog.append('<div><h3>Data Key</h3><input type="text" id="dataKey" value="' + (entity.dataKey ? entity.dataKey : '') + '"><button id="saveDataKey">Save</button></div>');
        $('#saveDataKey', dialog).on('click', function() {
            entity.setProperty('dataKey', $('#dataKey', dialog).val(), false, function() {
                log('Data Key successfully updated!', entity.dataKey);
            });
        });


        dialog.append('<div><h3>REST Query</h3><textarea cols="80" rows="4" id="restQuery">' + (entity.restQuery ? entity.restQuery : '') + '</textarea></div>');
        dialog.append('<div><button id="applyRestQuery">Apply</button></div>');
        $('#applyRestQuery', dialog).on('click', function() {
            entity.setProperty('restQuery', $('#restQuery', dialog).val(), false, function() {
                log('REST Query successfully updated!', entity.restQuery);
            });
        });
        
        dialog.append('<div><h3>Cypher Query</h3><textarea cols="80" rows="4" id="cypherQuery">' + (entity.cypherQuery ? entity.cypherQuery : '') + '</textarea></div>');
        dialog.append('<div><button id="applyCypherQuery">Apply</button></div>');
        $('#applyCypherQuery', dialog).on('click', function() {
            entity.setProperty('cypherQuery', $('#cypherQuery', dialog).val(), false, function() {
                log('Cypher Query successfully updated!', entity.cypherQuery);
            });
        });
        
        dialog.append('<div><h3>XPath Query</h3><textarea cols="80" rows="4" id="xpathQuery">' + (entity.xpathQuery ? entity.xpathQuery : '') + '</textarea></div>');
        dialog.append('<div><button id="applyXpathQuery">Apply</button></div>');
        $('#applyXpathQuery', dialog).on('click', function() {
            entity.setProperty('xpathQuery', $('#xpathQuery', dialog).val(), false, function() {
                log('XPath Query successfully updated!', entity.xpathQuery);
            });
        });

        dialog.append('<div><h3>Types to trigger partial update</h3><input type="text" id="partialUpdateKey" value="' + (entity.partialUpdateKey ? entity.partialUpdateKey : '') + '"><button id="savePartialUpdateKey">Save</button></div>');
        $('#savePartialUpdateKey', dialog).on('click', function() {
            entity.setProperty('partialUpdateKey', $('#partialUpdateKey', dialog).val(), false, function() {
                log('Partial update key successfully updated!', entity.partialUpdateKey);
            });
        });

        //_Entities.appendSimpleSelection(dialog, entity, 'data_node', 'Data Tree Root Node', 'dataTreeId');
        
        dialog.append('<div><h3>Data Node ID</h3><input type="text" id="dataNodeId" size="32" value="' + (entity.dataNodeId ? entity.dataNodeId : '') + '"><button id="saveDataNodeId">Save</button></div>');
        $('#saveDataNodeId', dialog).on('click', function() {
            log('add Data Node', entity.id, $('#dataNodeId', dialog).val());
            Command.addDataTree(entity.id, $('#dataNodeId', dialog).val());
        });
    },

    showProperties : function(entity) {

        var views;
	var startView = '_html_';
        
        if (entity.type === 'Content' || entity.type === 'Page') {
            views = ['all', 'in', 'out' ];
            startView = 'all';
        } else {
            views = ['_html_', 'all', 'in', 'out'];
        }

        var tabTexts = [];
        tabTexts._html_ = 'HTML';
        tabTexts.all = 'Node';
        tabTexts.in = 'Incoming';
        tabTexts.out = 'Outgoing';

        //dialog.empty();
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
            var tabText = tabTexts[view];
            
            $('ul', tabs).append('<li class="' + (view === startView ? 'active' : '') + '" id="tab-' + view + '">' + tabText + '</li>');

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
            if (view !== startView) {
                tabView.hide();
            }

            var headers = {};
            headers['X-StructrSessionToken'] = token;
            log('headers', headers);
            log('showProperties URL: ' + rootUrl + entity.id + (view ? '/' + view : ''), headers);
            
            $.ajax({
                url: rootUrl + entity.id + (view ? '/' + view : '') + '?pageSize=10',
                async: true,
                dataType: 'json',
                contentType: 'application/json; charset=utf-8',
                headers: headers,
                success: function(data) {
                    //element.append('<div class="sep"></div>');
                    //element.append('<table class="props"></table>');
                    log(data.result);
                    
                    // Default: Edit node id
                    var id = entity.id;
                    // ID of graph object to edit
                    $(data.result).each(function(i, res) {

                        // reset id for each object group
                        id = entity.id;
			
                        var keys = Object.keys(res);

                        log('keys', keys);

                        //			if (view == 'in' || view == 'out') {
                        //			    tabView.append('<br><h3>Relationship ' + res['id']+ '</h3>')
                        //			}
			log('res[id]', res['id']);
                        tabView.append('<table class="props ' + view + ' ' + res['id'] +'_"></table>');

                        var props = $('.props.' + view + '.' + res['id'] + '_', tabView);
				
                        $(keys).each(function(i, key) {

                            if (view === '_html_') {
                                
                                if (key !== 'id') {
                                
                                    props.append('<tr><td class="key">' + key.replace(view, '') + '</td><td class="value ' + key + '_">' + formatValueInputField(key, res[key]) + '</td></tr>');
                                
                                }
                                
                            } else if (view === 'in' || view === 'out') {
                                
                                if (key === 'id') {
                                    // set ID to rel ID
                                    id = res[key];
                                //console.log('Set ID to relationship ID', id);
                                }
                                
                                props.append('<tr><td class="key">' + key + '</td><td rel_id="' + id + '" class="value ' + key + '_">' + formatValueInputField(key, res[key]) + '</td></tr>');
                                                               
                            } else {
                                
                                if (!key.startsWith('_html_') && !isIn(key, _Entities.hiddenAttrs)) {
                                    
                                    if (isIn(key, _Entities.readOnlyAttrs)) {
                                        
                                        props.append('<tr><td class="key">' + formatKey(key) + '</td><td class="value ' + key + '_ readonly"><input type="text" class="readonly" readonly value="' + res[key] + '"></td></tr>');
                                
                                    } else if (isIn(key, _Entities.booleanAttrs)) {
                                    
                                        props.append('<tr><td class="key">' + formatKey(key) + '</td><td><input type="checkbox" class="' + key + '_"></td></tr>');
                                        var checkbox = $(props.find('.' + key + '_'));
                                        checkbox.on('change', function() {
                                            var checked = checkbox.prop('checked');
                                            log('set property', id, key, checked);
                                            Command.setProperty(id, key, checked);
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
                                
                                        props.append('<tr><td class="key">' + formatKey(key) + '</td><td class="value ' + key + '_">' + formatValueInputField(key, res[key]) + '</td></tr>');
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
                                    log('relId', relId);
                                    var objId = relId ? relId : id;
                                    log('set properties of obj', objId);
                                    
                                    var keyInput = input.parent().parent().children('td').first().children('input');
                                    log(keyInput);
                                    if (keyInput && keyInput.length) {
                                    
                                        // new key
                                        log('new key: Command.setProperty(', objId, keyInput.val(), input.val());
                                        Command.setProperty(objId, keyInput.val(), input.val());
                                        
                                        
                                    } else {
                                        
                                        // existing key
                                        log('existing key: Command.setProperty(', objId, input.prop('name'), input.val());
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

                _Entities.appendSimpleSelection(dialogText, entity, 'users', 'Owner', 'ownerId');
                
                dialogText.append('<h3>Visibility</h3><div class="' + entity.id + '_"><button class="switch disabled visibleToPublicUsers_">Public (visible to anyone)</button><button class="switch disabled visibleToAuthenticatedUsers_">Authenticated Users</button></div>');
                dialogText.append('<div>Apply Recursively? <input id="recursive" type="checkbox" name="recursive"></div>');
                
                var publicSwitch = $('.visibleToPublicUsers_');
                var authSwitch = $('.visibleToAuthenticatedUsers_');
                
                _Entities.changeBooleanAttribute(publicSwitch, entity.visibleToPublicUsers);
                _Entities.changeBooleanAttribute(authSwitch, entity.visibleToAuthenticatedUsers);
                
                publicSwitch.on('click', function(e) {
                    e.stopPropagation();
                    entity.setProperty('visibleToPublicUsers', publicSwitch.hasClass('disabled'), $('#recursive', dialogText).is(':checked'), function() {
                        _Entities.changeBooleanAttribute(publicSwitch, entity.visibleToPublicUsers);
                    });
                });

                authSwitch.on('click', function(e) {
                    e.stopPropagation();
                    entity.setProperty('visibleToAuthenticatedUsers', authSwitch.hasClass('disabled'), $('#recursive', dialogText).is(':checked'), function() {
                        _Entities.changeBooleanAttribute(authSwitch, entity.visibleToAuthenticatedUsers);
                    });
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
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            headers: headers,
            success: function(data) {
                $(data.result).each(function(i, result) {
                    //log(result);
                    selectElement.append('<option ' + (entity[key] == result.id ? 'selected' : '') + ' value="' + result.id + '">' + result.name + '</option>');
                });
            }
        });                
                
        element.append('</select></span>');

        var select = $('#' + key + 'Select', element);
        select.on('change', function() {
            entity.setProperty(key, select.val(), false);
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
            log('showProperties', entity);
            _Entities.showProperties(entity);
        });
    },

    appendDataIcon : function(parent, entity) {

        var dataIcon = $('.data_icon', parent);

        if (!(dataIcon && dataIcon.length)) {
            parent.append('<img title="Edit Data Settings" alt="Edit Data Settings" class="data_icon button" src="' + '/structr/icon/database_table.png' + '">');
            dataIcon = $('.data_icon', parent)
        }
        dataIcon.on('click', function(e) {
            e.stopPropagation();
            log('showDataDialog', entity);
            _Entities.showDataDialog(entity);
        });
    },

    appendExpandIcon : function(el, entity, hasChildren, expand) {
        
        log('_Entities.appendExpandIcon', el, entity, hasChildren, expand);
        
        var button = $(el.children('.expand_icon').first());
        if (button && button.length) {
            log('Expand icon already existing');
            return;
        }

        if (hasChildren) {

            var typeIcon = $(el.children('.typeIcon').first());
            var icon = expand ? Structr.expanded_icon : Structr.expand_icon;
            
            typeIcon.css({
                paddingRight: 0 + 'px'
            })
            .after('<img title="Expand \'' + entity.name + '\'" alt="Expand \'' + entity.name + '\'" class="expand_icon" src="' + icon + '">');

            button = $(el.children('.expand_icon').first());

            if (button) {
                
                button.on('click', function(e) {
                    log('expand icon clicked');
                    e.stopPropagation();
                    _Entities.toggleElement($(this).parent('.node'));
                });
                
                $(el).on('click', function(e) {
                    log('node clicked');
                    _Entities.toggleElement(this);
                });

                // Prevent expand icon from being draggable
                button.on('mousedown', function(e) {
                    e.stopPropagation();
                });
                
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
        if (!el) return;
        log('removeExpandIcon', el);
        var button = $(el.children('.expand_icon').first());
        button.remove();
        el.children('.typeIcon').css({
            paddingRight: 11 + 'px'
        });
    },

    setMouseOver : function(el, allowClick) {
        var node = $(el).closest('.node');
        if (!node || !node.children) return;

        if (!allowClick) {
            node.on('click', function(e) {
                e.stopPropagation();
            });
        }

        node.children('b.name_').on('click', function(e) {
            e.stopPropagation();
            _Entities.makeNameEditable(node);
        });

        node.on({
            mouseover: function(e) {
                e.stopPropagation();
                var self = $(this);
                var nodeId = getId(el);
                var page = $(el).closest('.page');
                if (page.length) {
                    var pageId = getId(page);
                    var previewNodes = $('#preview_' + pageId).contents().find('[structr_element_id]');
                    previewNodes.each(function(i,v) {
                        var self = $(v);
                        var sid = self.attr('structr_element_id');
                        if (sid === nodeId) {
                            self.addClass('nodeHover');
                        }
                    });
                }
                self.addClass('nodeHover').children('img.button').show();
                self.children('.icons').children('img.button').show();
            },
            mouseout: function(e) {
                e.stopPropagation();
                _Entities.resetMouseOverState(this);
            }
        });
    },

    resetMouseOverState : function(element) {
        var el = $(element);
        var node = el.closest('.node')
        if (node) {
            node.removeClass('nodeHover');
            node.find('img.button').hide();
        }
        var page = node.closest('.page');
        if (page.length) {
            var resId = getId(page);
            var previewNodes = $('#preview_' + resId).contents().find('[structr_element_id]');
            previewNodes.each(function(i,v) {
                var self = $(v);
                var sid = self.attr('structr_element_id');
                if (sid === nodeId) {
                    log(sid);
                    self.removeClass('nodeHover');
                    log(self);
                }
            });
        }
    },

    ensureExpanded : function(element) {
        
        var el = $(element);
        var b;
        var src = el.prop('src');
        
        var id = getId(element);
        
        log('ensureExpanded: ', el, id);
        addExpandedNode(id);
        
        b = el.children('.expand_icon').first();
        src = b.prop('src');
        
        if (!src) return;
        
        if (src.endsWith('icon/tree_arrow_down.png')) {
            return;
        } else {
            
            Command.children(id);
            b.prop('src', 'icon/tree_arrow_down.png');
            
        }
    },

    toggleElement : function(element, expanded) {

        var el = $(element);
        var b;
        var src = el.prop('src');
        
        var id = getId(el);
        
        log(el);
        
        log('toggleElement: ', id);
        
        b = el.children('.expand_icon').first();
        src = b.prop('src');
        
        if (!src) return;
        
        if (src.endsWith('icon/tree_arrow_down.png')) {
            
            $.each(el.children('.node'), function(i, child) {
               $(child).remove();
            });
            
            b.prop('src', 'icon/tree_arrow_right.png');

            removeExpandedNode(id);
        } else {
            
            if (!expanded) Command.children(id);
            b.prop('src', 'icon/tree_arrow_down.png');

            addExpandedNode(id);
        }

    },

    makeNameEditable : function(element) {
        //element.off('dblclick');
        element.off('hover');
        var oldName = $.trim(element.children('b.name_').attr('title'));
        //console.log('oldName', oldName);
        element.children('b.name_').replaceWith('<input type="text" size="' + (oldName.length+4) + '" class="newName_" value="' + oldName + '">');
        element.find('.button').hide();

        var input = $('input', element);

        input.focus().select();

        input.on('blur', function() {
            var self = $(this);
            var newName = self.val();
            Command.setProperty(getId(element), "name", newName);
            self.replaceWith('<b title="' + newName + '" class="name_">' + fitStringToSize(newName, 200) + '</b>');
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
                self.replaceWith('<b title="' + newName + '" class="name_">' + fitStringToSize(newName, 200) + '</b>');
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
