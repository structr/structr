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
    booleanAttrs: ['visibleToPublicUsers', 'visibleToAuthenticatedUsers', 'hidden', 'deleted', 'blocked', 'frontendUser', 'backendUser', 'hideOnIndex', 'hideOnEdit', 'hideOnNonEdit', 'hideOnDetail', 'renderDetails'],
    numberAttrs: ['position', 'size'],
    dateAttrs: ['createdDate', 'lastModifiedDate', 'visibilityStartDate', 'visibilityEndDate'],
    hiddenAttrs: ['base', 'deleted', 'ownerId', 'owner', 'group', 'categories', 'tag', 'createdBy', 'visibilityStartDate', 'visibilityEndDate', 'parentFolder', 'url', 'path', 'elements', 'components', 'paths', 'parents'],
    readOnlyAttrs: ['lastModifiedDate', 'createdDate', 'id', 'checksum', 'size', 'version', 'relativeFilePath'],
    changeBooleanAttribute: function(attrElement, value) {

        log('Change boolean attribute ', attrElement, ' to ', value);

        if (value === true) {
            attrElement.removeClass('disabled').addClass('enabled').prop('checked', true);
        } else {
            attrElement.removeClass('enabled').addClass('disabled').prop('checked', false);
        }

    },
    reloadChildren: function(id) {
        var el = Structr.node(id);

        log('reloadChildren', el);

        $(el).children('.node').remove();
        _Entities.resetMouseOverState(el);

        Command.children(id);

    },
    deleteNode: function(button, entity) {
        buttonClicked = button;
        if (isDisabled(button))
            return;

        Structr.confirmation('<p>Delete ' + entity.type + ' \'' + entity.name + '\' [' + entity.id + '] ?</p>',
                function() {
                    Command.deleteNode(entity.id);
                    $.unblockUI({
                        fadeOut: 25
                    });
                });
    },
    showSyncDialog: function(source, target) {
        Structr.dialog('Sync between ' + source.id + ' and ' + target.id, function() {
            return true;
        }, function() {
            return true;
        });

        dialog.append('<div><input type="radio" name="syncMode" value="none"><label for="unidir">None</label></div>');
        dialog.append('<div><input type="radio" name="syncMode" value="unidir"><label for="unidir">Uni-directional (master/slave)</label></div>');
        dialog.append('<div><input type="radio" name="syncMode" value="bidir"><label for="unidir">Bi-directional</label></div>');

        $('input[name=syncMode]:radio', dialog).on('change', function() {
            Command.setSyncMode(source.id, target.id, $(this).val());
        });

    },
    showDataDialog: function(entity) {

        Structr.dialog('Edit Data Settings of ' + entity.id, function() {
            return true;
        }, function() {
            return true;
        });

        dialogText.append('<div class="' + entity.id + '_"><button class="switch disabled renderDetails_">Auto-limit to single object</button> If URL ends with an ID, the query result is limited to this object automatically.</div>');
        var detailsSwitch = $('.renderDetails_');
        _Entities.changeBooleanAttribute(detailsSwitch, entity.renderDetails);
        detailsSwitch.on('click', function(e) {
            e.stopPropagation();
            entity.setProperty('renderDetails', detailsSwitch.hasClass('disabled'), false, function() {
                _Entities.changeBooleanAttribute(detailsSwitch, entity.renderDetails);
                blinkGreen(detailsSwitch);
            });
        });

        dialogText.append('<div class="' + entity.id + '_"><button class="switch disabled hideOnIndex_">Hide element in index mode</button> If URL does not end with an ID, this element is hidden.</div>');
        var indexSwitch = $('.hideOnIndex_');
        _Entities.changeBooleanAttribute(indexSwitch, entity.hideOnIndex);
        indexSwitch.on('click', function(e) {
            e.stopPropagation();
            entity.setProperty('hideOnIndex', indexSwitch.hasClass('disabled'), false, function() {
                _Entities.changeBooleanAttribute(indexSwitch, entity.hideOnIndex);
                blinkGreen(indexSwitch);
            });
        });

        dialogText.append('<div class="' + entity.id + '_"><button class="switch disabled hideOnDetail_">Hide element in details mode</button> If URL ends with an ID, this element is hidden.</div>');
        var hideOnDetailSwitch = $('.hideOnDetail_');
        _Entities.changeBooleanAttribute(hideOnDetailSwitch, entity.hideOnDetail);
        hideOnDetailSwitch.on('click', function(e) {
            e.stopPropagation();
            entity.setProperty('hideOnDetail', hideOnDetailSwitch.hasClass('disabled'), false, function() {
                _Entities.changeBooleanAttribute(hideOnDetailSwitch, entity.hideOnDetail);
                blinkGreen(hideOnDetailSwitch);
            });
        });

        dialogText.append('<div class="' + entity.id + '_"><button class="switch disabled hideOnEdit_">Hide element in edit mode</button> Apply to elements which should not be rendered in edit mode.</div>');
        var hideOnEditSwitch = $('.hideOnEdit_');
        _Entities.changeBooleanAttribute(hideOnEditSwitch, entity.hideOnEdit);
        hideOnEditSwitch.on('click', function(e) {
            e.stopPropagation();
            entity.setProperty('hideOnEdit', hideOnEditSwitch.hasClass('disabled'), false, function() {
                _Entities.changeBooleanAttribute(hideOnEditSwitch, entity.hideOnEdit);
                blinkGreen(hideOnEditSwitch);
            });
        });

        dialogText.append('<div class="' + entity.id + '_"><button class="switch disabled hideOnNonEdit_">Hide element in non-edit mode</button> Apply to elements which should not be rendered in default mode.</div>');
        var hideOnNonEditSwitch = $('.hideOnNonEdit_');
        _Entities.changeBooleanAttribute(hideOnNonEditSwitch, entity.hideOnNonEdit);
        hideOnNonEditSwitch.on('click', function(e) {
            e.stopPropagation();
            entity.setProperty('hideOnNonEdit', hideOnNonEditSwitch.hasClass('disabled'), false, function() {
                _Entities.changeBooleanAttribute(hideOnNonEditSwitch, entity.hideOnNonEdit);
                blinkGreen(hideOnNonEditSwitch);
            });
        });

        dialog.append('<div><h3>Data Key</h3><p>Query results are mapped to this key and can be accessed by ${dataKey.propertyKey}</p><input type="text" id="dataKey" value="' + (entity.dataKey ? entity.dataKey : '') + '"><button id="saveDataKey">Save</button></div>');
        $('#saveDataKey', dialog).on('click', function() {
            entity.setProperty('dataKey', $('#dataKey', dialog).val(), false, function() {
                log('Data Key successfully updated!', entity.dataKey);
                blinkGreen($('#dataKey'));
            });
        });


        dialog.append('<div><h3>REST Query</h3><textarea cols="60" rows="2" id="restQuery">' + (entity.restQuery ? entity.restQuery : '') + '</textarea></div>');
        dialog.append('<div><button id="applyRestQuery">Save    </button></div>');
        $('#applyRestQuery', dialog).on('click', function() {
            entity.setProperty('restQuery', $('#restQuery', dialog).val(), false, function() {
                log('REST Query successfully updated!', entity.restQuery);
                blinkGreen($('#restQuery'));
            });
        });

        dialog.append('<div><h3>Cypher Query</h3><textarea cols="60" rows="2" id="cypherQuery">' + (entity.cypherQuery ? entity.cypherQuery : '') + '</textarea></div>');
        dialog.append('<div><button id="applyCypherQuery">Save</button></div>');
        $('#applyCypherQuery', dialog).on('click', function() {
            entity.setProperty('cypherQuery', $('#cypherQuery', dialog).val(), false, function() {
                log('Cypher Query successfully updated!', entity.cypherQuery);
                blinkGreen($('#cypherQuery'));
            });
        });

        dialog.append('<div><h3>XPath Query</h3><textarea cols="60" rows="2" id="xpathQuery">' + (entity.xpathQuery ? entity.xpathQuery : '') + '</textarea></div>');
        dialog.append('<div><button id="applyXpathQuery">Save</button></div>');
        $('#applyXpathQuery', dialog).on('click', function() {
            entity.setProperty('xpathQuery', $('#xpathQuery', dialog).val(), false, function() {
                log('XPath Query successfully updated!', entity.xpathQuery);
                blinkGreen($('#xpathQuery'));
            });
        });

        dialog.append('<div><h3>Types to trigger partial update</h3><input type="text" id="partialUpdateKey" value="' + (entity.partialUpdateKey ? entity.partialUpdateKey : '') + '"><button id="savePartialUpdateKey">Save</button></div>');
        $('#savePartialUpdateKey', dialog).on('click', function() {
            entity.setProperty('partialUpdateKey', $('#partialUpdateKey', dialog).val(), false, function() {
                log('Partial update key successfully updated!', entity.partialUpdateKey);
                blinkGreen($('#partialUpdateKey'));
            });
        });

    },
    showProperties: function(entity) {

        var views;
        var startView = '_html_';

        if (isIn(entity.type, ['Content', 'Page', 'User', 'Group', 'File', 'Folder', 'Widget'])) {
            views = ['ui', 'in', 'out'];
            startView = 'ui';
        } else {
            views = ['_html_', 'ui', 'in', 'out'];
        }

        var tabTexts = [];
        tabTexts._html_ = 'HTML';
        tabTexts.ui = 'Node';
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
                        tabView.append('<table class="props ' + view + ' ' + res['id'] + '_"></table>');

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

                                        if (!res[key] || res[key] === 'null') {
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

                        $('.props tr td.value input', dialog).each(function(i, v) {

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

                                        var newKey = keyInput.val();
                                        var val    = input.val();

                                        // new key
                                        log('new key: Command.setProperty(', objId, newKey, val);
                                        Command.setProperty(objId, newKey, val, false, function() {
                                            blinkGreen(input);
                                            dialogMsg.html('<div class="infoBox success">New property "' + newKey + '" saved with value "' + val + '".</div>');
                                            $('.infoBox', dialogMsg).delay(2000).fadeOut(200);
                                        });


                                    } else {

                                        var key = input.prop('name');
                                        var val    = input.val();

                                        if (input.data('changed')) {
                                            
                                            input.data('changed', false);

                                            // existing key
                                            log('existing key: Command.setProperty(', objId, key, val);
                                            Command.setProperty(objId, key, val, false, function() {
                                                blinkGreen(input);
                                                dialogMsg.html('<div class="infoBox success">Updated property "' + key + '" with value "' + val + '".</div>');
                                                $('.infoBox', dialogMsg).delay(2000).fadeOut(200);

                                            });
                                        }

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
    appendAccessControlIcon: function(parent, entity) {
        
        var protected = !entity.visibleToPublicUsers || !entity.visibleToAuthenticatedUsers;

        var keyIcon = $('.key_icon', parent);
        var newKeyIcon = '<img title="Access Control and Visibility" alt="Access Control and Visibility" class="key_icon button" src="' + Structr.key_icon + '">';
        if (!(keyIcon && keyIcon.length)) {
            parent.append(newKeyIcon);
            keyIcon = $('.key_icon', parent)
            if (protected) {
                keyIcon.show();
                keyIcon.addClass('donthide');
            }

            keyIcon.on('click', function(e) {
                e.stopPropagation();
                Structr.dialog('Access Control and Visibility', function() {
                }, function() {
                });

                _Entities.appendSimpleSelection(dialogText, entity, 'users', 'Owner', 'owner.id');

                dialogText.append('<h3>Visibility</h3><div class="' + entity.id + '_"><button class="switch disabled visibleToPublicUsers_">Public (visible to anyone)</button><button class="switch disabled visibleToAuthenticatedUsers_">Authenticated Users</button></div>');

                if (lastMenuEntry === 'pages' && !(entity.type === 'Content')) {
                    dialogText.append('<div>Apply Recursively? <input id="recursive" type="checkbox" name="recursive"></div>');
                }

                var publicSwitch = $('.visibleToPublicUsers_');
                var authSwitch = $('.visibleToAuthenticatedUsers_');

                //console.log(entity);    

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


                dialogText.append('<h3>Access Rights</h3>');
                dialogText.append('<table class="props" id="principals"><thead><tr><th>Name</th><th>Read</th><th>Write</th><th>Delete</th><th>Access Control</th></tr></thead><tbody></tbody></table');

                var tb = $('#principals tbody', dialogText);
                tb.append('<tr id="new"><td><select id="newPrincipal"><option></option></select></td><td><input id="newRead" type="checkbox" disabled="disabled"></td><td><input id="newRead" type="checkbox" disabled="disabled"></td><td><input id="newRead" type="checkbox" disabled="disabled"></td><td><input id="newRead" type="checkbox" disabled="disabled"></td></tr>');
                Command.getByType('User', 1000, 1, 'name', 'asc', function(user) {
                    $('#newPrincipal').append('<option value="' + user.id + '">' + user.name + '</option>');
                });
                Command.getByType('Group', 1000, 1, 'name', 'asc', function(group) {
                    $('#newPrincipal').append('<option value="' + group.id + '">' + group.name + '</option>');
                });
                $('#newPrincipal').on('change', function() {
                    var sel = $(this);
                    console.log(sel);
                    var pId = sel[0].value;
                    Command.setPermission(entity.id, pId, 'grant', 'read', false);
                    $('#new', tb).selectedIndex = 0;

                    Command.get(pId, function(p) {
                        addPrincipal(entity, p, { 'read': true});
                    });

                });

                var headers = {};
                headers['X-StructrSessionToken'] = token;
                $.ajax({
                    url: rootUrl + '/' + entity.id + '/in',
                    dataType: 'json',
                    contentType: 'application/json; charset=utf-8',
                    headers: headers,
                    success: function(data) {

                        $(data.result).each(function(i, result) {

                            var permissions = {
                                'read': isIn('read', result.allowed),
                                'write': isIn('write', result.allowed),
                                'delete': isIn('delete', result.allowed),
                                'accessControl': isIn('accessControl', result.allowed)
                            };

                            var principalId = result.principalId;
                            if (principalId) {

                                Command.get(principalId, function(p) {
                                    addPrincipal(entity, p, permissions);
                                });

                            }

                        });
                    }
                });

            });
        }
    },
    appendSimpleSelection: function(el, entity, type, title, key) {

        var subKey;
        if (key.contains('.')) {
            subKey = key.substring(key.indexOf('.') + 1, key.length);
            key = key.substring(0, key.indexOf('.'));
            //console.log('object key:', key, ', sub key: ', subKey);
        }

        el.append('<h3>' + title + '</h3><p id="' + key + 'Box"></p>');

        var element = $('#' + key + 'Box');

        element.append('<span class="' + entity.id + '_"><select class="' + key + '_" id="' + key + 'Select"></select></span>');

        var selectElement = $('#' + key + 'Select');

        selectElement.append('<option></option>')

        var headers = {};
        headers['X-StructrSessionToken'] = token;
        $.ajax({
            url: rootUrl + type + '/ui?pageSize=100',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            headers: headers,
            success: function(data) {
                $(data.result).each(function(i, result) {

                    var id = (subKey && entity[key] ? entity[key][subKey] : entity[key]);
                    //console.log(id, result.id);

                    var selected = (id === result.id ? 'selected' : '');

                    selectElement.append('<option ' + selected + ' value="' + result.id + '">' + result.name + '</option>');
                });
            }
        });

        var select = $('#' + key + 'Select', element);
        select.on('change', function() {
            entity.setProperty(key, select.val(), false);
        });
    },
    appendEditPropertiesIcon: function(parent, entity) {

        var editIcon = $('.edit_props_icon', parent);

        if (!(editIcon && editIcon.length)) {
            parent.append('<img title="Edit Properties" alt="Edit Properties" class="edit_props_icon button" src="' + '/structr/icon/application_view_detail.png' + '">');
            editIcon = $('.edit_props_icon', parent);
        }
        editIcon.on('click', function(e) {
            e.stopPropagation();
            log('showProperties', entity);
            _Entities.showProperties(entity);
        });
    },
    appendDataIcon: function(parent, entity) {

        var dataIcon = $('.data_icon', parent);

        if (!(dataIcon && dataIcon.length)) {
            parent.append('<img title="Edit Data Settings" alt="Edit Data Settings" class="data_icon button" src="' + '/structr/icon/database_table.png' + '">');
            dataIcon = $('.data_icon', parent);
        }
        dataIcon.on('click', function(e) {
            e.stopPropagation();
            log('showDataDialog', entity);
            _Entities.showDataDialog(entity);
        });
    },
    appendExpandIcon: function(el, entity, hasChildren, expand) {

        log('_Entities.appendExpandIcon', el, entity, hasChildren, expand);

        var button = $(el.children('.expand_icon').first());
        if (button && button.length) {
            log('Expand icon already existing');
            return;
        }

        if (hasChildren) {
            
            log('appendExpandIcon hasChildren?', hasChildren, 'expand?', expand)

            var typeIcon = $(el.children('.typeIcon').first());
            var icon = expand ? Structr.expanded_icon : Structr.expand_icon;

            typeIcon.css({
                paddingRight: 0 + 'px'
            }).after('<img title="Expand \'' + entity.name + '\'" alt="Expand \'' + entity.name + '\'" class="expand_icon" src="' + icon + '">');

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
    removeExpandIcon: function(el) {
        if (!el)
            return;
        log('removeExpandIcon', el);
        var button = $(el.children('.expand_icon').first());
        button.remove();
        el.children('.typeIcon').css({
            paddingRight: 11 + 'px'
        });
    },
    setMouseOver: function(el, allowClick, syncedNodes) {
        var node = $(el).closest('.node');
        if (!node || !node.children) {
            return;
        }

        if (!allowClick) {
            node.on('click', function(e) {
                e.stopPropagation();
            });
        }

        node.children('b.name_').on('click', function(e) {
            e.stopPropagation();
            _Entities.makeNameEditable(node);
        });

        var nodeId = getId(el), isComponent;
        if (nodeId === undefined) {
            nodeId = getComponentId(el);
            isComponent = true;
        }

        node.on({
            mouseover: function(e) {
                e.stopPropagation();
                var self = $(this);
                $('#componentId_' + nodeId).addClass('nodeHover');
                if (isComponent) $('#id_' + nodeId).addClass('nodeHover');
                
                if (syncedNodes && syncedNodes.length) {
                    syncedNodes.forEach(function(s) {
                        $('#id_' + s).addClass('nodeHover');
                        $('#componentId_' + s).addClass('nodeHover');
                    });
                }
                
                var page = $(el).closest('.page');
                if (page.length) {
                    $('#preview_' + getId(page)).contents().find('[data-structr-id=' + nodeId + ']').addClass('nodeHover');
                }
                self.addClass('nodeHover').children('img.button').show();
                self.children('.icons').children('img.button').show();
            },
            mouseout: function(e) {
                e.stopPropagation();
                $('#componentId_' + nodeId).removeClass('nodeHover');
                if (isComponent) $('#id_' + nodeId).removeClass('nodeHover');

                if (syncedNodes && syncedNodes.length) {
                    syncedNodes.forEach(function(s) {
                        $('#id_' + s).removeClass('nodeHover');
                        $('#componentId_' + s).removeClass('nodeHover');
                    });
                }
                
                _Entities.resetMouseOverState(this);
            }
        });
    },
    resetMouseOverState: function(element) {
        var el = $(element);
        var node = el.closest('.node')
        if (node) {
            node.removeClass('nodeHover');
            node.find('img.button').not('.donthide').hide();
        }
        var page = node.closest('.page');
        if (page.length) {
            $('#preview_' + getId(page)).contents().find('[data-structr-id=' + getId(node) + ']').removeClass('nodeHover');
        }
    },
    ensureExpanded: function(element) {

        var el = $(element);
        var b;
        var src = el.prop('src');

        var id = getId(el);

        //console.log('ensureExpanded: ', el, id);
        addExpandedNode(id);

        b = el.children('.expand_icon').first();
        src = b.prop('src');

        if (!src)
            return;

        if (src.endsWith('icon/tree_arrow_down.png')) {
            return;
        } else {
            log('ensureExpanded: fetch children', el);
            Command.children(id);
            b.prop('src', 'icon/tree_arrow_down.png');
        }
    },
    toggleElement: function(element, expanded) {

        var el = $(element);
        var b;
        var src = el.prop('src');

        var id = getId(el);

        log(el);

        log('toggleElement: ', id);

        b = el.children('.expand_icon').first();
        src = b.prop('src');

        if (!src)
            return;

        if (src.endsWith('icon/tree_arrow_down.png')) {

            $.each(el.children('.node'), function(i, child) {
                $(child).remove();
            });

            b.prop('src', 'icon/tree_arrow_right.png');

            removeExpandedNode(id);
        } else {

            if (!expanded) {
                log('toggleElement: fetch children', id);
                Command.children(id);
                
            }
            b.prop('src', 'icon/tree_arrow_down.png');

            addExpandedNode(id);
        }

    },
    makeNameEditable: function(element) {
        //element.off('dblclick');
        element.off('hover');
        var oldName = $.trim(element.children('b.name_').attr('title'));
        //console.log('oldName', oldName);
        element.children('b.name_').replaceWith('<input type="text" size="' + (oldName.length + 4) + '" class="newName_" value="' + oldName + '">');
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
            if (e.keyCode === 13) {
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

function addPrincipal(entity, principal, permissions) {

    $('#newPrincipal option[value="' + principal.id + '"]').remove();
    $('#new').before('<tr id="_' + principal.id + '"><td><img class="typeIcon" src="' + (principal.type === 'Group' ? 'icon/group.png' : 'icon/user.png') + '"> <span class="name">' + principal.name + '</span></td><tr>');

    var row = $('#_' + principal.id);

    ['read', 'write', 'delete', 'accessControl'].forEach(function(perm) {

        row.append('<td><input class="' + perm + '" type="checkbox"' + (permissions[perm] ? ' checked="checked"' : '') + '"></td>');
        var disabled = false;

        $('.' + perm, row).on('dblclick', function() {
            return false;
        });

        $('.' + perm, row).on('click', function(e) {
            e.preventDefault();
            if (disabled)
                return false;
            var sw = $(this);
            disabled = true;
            sw.prop('disabled', 'disabled');
            window.setTimeout(function() {
                disabled = false;
                sw.prop('disabled', null);
            }, 200);
            //console.log('checked elements', $('input:checked', row).length);
            if (!$('input:checked', row).length) {
                $('#newPrincipal').append('<option value="' + row.attr('id').substring(1) + '">' + $('.name', row).text() + '</option>');
                row.remove();
            }
            Command.setPermission(entity.id, principal.id, permissions[perm] ? 'revoke' : 'grant', perm, false, function() {
                permissions[perm] = !permissions[perm];
                sw.prop('checked', permissions[perm]);
                log('Permission successfully updated!');
                blinkGreen(sw.parent());
                
                
            });
        });
    });
}
