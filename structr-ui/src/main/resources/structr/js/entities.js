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

var buttonClicked;
var activeElements = {};

var _Entities = {
    booleanAttrs: ['visibleToPublicUsers', 'visibleToAuthenticatedUsers', 'isAdmin', 'hidden', 'deleted', 'blocked', 'frontendUser', 'backendUser', 'hideOnIndex', 'hideOnEdit', 'hideOnNonEdit', 'hideOnDetail', 'renderDetails'],
    numberAttrs: ['position', 'size'],
    dateAttrs: ['createdDate', 'lastModifiedDate', 'visibilityStartDate', 'visibilityEndDate'],
    hiddenAttrs: ['base', 'deleted', 'ownerId', 'owner', 'group', 'categories', 'tag', 'createdBy', 'visibilityStartDate', 'visibilityEndDate', 'parentFolder', 'url', 'path', 'elements', 'components', 'paths', 'parents'],
    readOnlyAttrs: ['lastModifiedDate', 'createdDate', 'id', 'checksum', 'size', 'version', 'relativeFilePath'],
    changeBooleanAttribute: function(attrElement, value, activeLabel, inactiveLabel) {

        log('Change boolean attribute ', attrElement, ' to ', value);

        if (value === true) {
            attrElement.removeClass('inactive').addClass('active').prop('checked', true).html('<img src="icon/tick.png">' + (activeLabel ? ' ' + activeLabel : ''));
        } else {
            attrElement.removeClass('active').addClass('inactive').prop('checked', false).text((inactiveLabel ? inactiveLabel : '-'));
        }

    },
    reloadChildren: function(id) {
        var el = Structr.node(id);

        log('reloadChildren', el);

        $(el).children('.node').remove();
        _Entities.resetMouseOverState(el);

        Command.children(id);

    },
    deleteNode: function(button, entity, callback) {
        buttonClicked = button;
        if (isDisabled(button))
            return;

        Structr.confirmation('<p>Delete ' + entity.type + ' \'' + entity.name + '\' [' + entity.id + '] ?</p>',
                function() {
                    Command.deleteNode(entity.id);
                    $.unblockUI({
                        fadeOut: 25
                    });
                    if (callback) {
                        callback();
                    }
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
    dataBindingDialog: function(entity, el) {

        el.append('<table class="props"></table>');
        var t = $('.props', el);

        // General
        _Entities.appendRowWithInputField(entity, t, 'data-structr-id', 'Element ID (set to ${this.id})');
        _Entities.appendRowWithInputField(entity, t, 'data-structr-attr', 'Attribute Key (if set, render input field in edit mode)');
        _Entities.appendRowWithInputField(entity, t, 'data-structr-type', 'Data type (e.g. Date, Boolean; default: String)');
        _Entities.appendRowWithInputField(entity, t, 'data-structr-raw-value', 'Raw value (unformatted value for Date or Number fields)');
        _Entities.appendRowWithInputField(entity, t, 'data-structr-hide', 'Hide [edit|non-edit|edit,non-edit]');

        if (entity.type === 'Button' || entity.type === 'A') {

            // Buttons

            _Entities.appendRowWithInputField(entity, t, 'data-structr-action', 'Action [create:&lt;Type&gt;|edit|delete]');
            _Entities.appendRowWithInputField(entity, t, 'data-structr-attributes', 'Attributes (for create and edit action)');

            t.append('<tr><td class="key">Reload</td><td class="value"id="reload"></td><td></td></tr>');
            _Entities.appendBooleanSwitch($('#reload', t), entity, 'data-structr-reload', '', 'If active, the page will refresh after a successfull action.');

            if (entity['data-structr-action'] === 'delete') {

                // Delete action
                t.append('<tr><td class="key">Confirm on delete?</td><td class="value" id="confirmOnDel"></td><td></td></tr>');
                _Entities.appendBooleanSwitch($('#confirmOnDel', t), entity, 'data-structr-confirm', '', 'If active, a user has to confirm the delete action.');
            }
        } else if (entity.type === 'Input' || entity.type === 'Select' || entity.type === 'Textarea') {
            // Input fields
            _Entities.appendRowWithInputField(entity, t, 'data-structr-name', 'Field name (for create action)');

        }

//        _Entities.appendBooleanSwitch(el, entity, 'hideOnEdit', 'Hide in edit mode', 'If active, this node will not be visible in edit mode.');
//        _Entities.appendBooleanSwitch(el, entity, 'hideOnNonEdit', 'Hide in non-edit mode', 'If active, this node will not be visible in default (non-edit) mode.');

        //_Entities.appendInput(dialog, entity, 'partialUpdateKey', 'Types to trigger partial update', '');

    },
    appendRowWithInputField: function(entity, el, key, label) {
        el.append('<tr><td class="key">' + label + '</td><td class="value"><input class="' + key + '_" name="' + key + '" value="' + formatValue(entity[key]) + '"></td><td><img class="nullIcon" id="null_' + key + '" src="icon/cross_small_grey.png"></td></tr>');
        var inp = $('[name="' + key + '"]', el);
        _Entities.activateInput(inp, entity.id);
        var nullIcon = $('#null_' + key, el);
        nullIcon.on('click', function() {
            Command.setProperty(entity.id, key, null, false, function() {
                inp.val(null);
                blinkGreen(inp);
                dialogMsg.html('<div class="infoBox success">Property "' + key + '" was set to null.</div>');
                $('.infoBox', dialogMsg).delay(2000).fadeOut(1000);
            });
        });

    },
    queryDialog: function(entity, el) {

        el.append('<table class="props"></table>');
        var t = $('.props', el);

        t.append('<tr><td class="key">Query auto-limit</td><td class="value" id="queryAutoLimit"></td></tr>');
        t.append('<tr><td class="key">Hide in index mode</td><td  class="value" id="hideIndexMode"></td></tr>');
        t.append('<tr><td class="key">Hide in details mode</td><td  class="value" id="hideDetailsMode"></td></tr>');

        _Entities.appendBooleanSwitch($('#queryAutoLimit', t), entity, 'renderDetails', ['Query is limited', 'Query is not limited'], 'Limit result to the object with the ID the URL ends with.');
        _Entities.appendBooleanSwitch($('#hideIndexMode', t), entity, 'hideOnIndex', ['Hidden in index mode', 'Visible in index mode'], 'if URL does not end with an ID');
        _Entities.appendBooleanSwitch($('#hideDetailsMode', t), entity, 'hideOnDetail', ['Hidden in details mode', 'Visible in details mode'], 'if URL ends with an ID.');

        el.append('<div id="data-tabs" class="data-tabs"><ul><li class="active" id="tab-rest">REST Query</li><li id="tab-cypher">Cypher Query</li><li id="tab-xpath">XPath Query</li></ul>'
                + '<div id="content-tab-rest"></div><div id="content-tab-cypher"></div><div id="content-tab-xpath"></div></div>');

        _Entities.appendTextarea($('#content-tab-rest'), entity, 'restQuery', 'REST Query', '');
        _Entities.appendTextarea($('#content-tab-cypher'), entity, 'cypherQuery', 'Cypher Query', '');
        _Entities.appendTextarea($('#content-tab-xpath'), entity, 'xpathQuery', 'XPath Query', '');

        _Entities.appendInput(el, entity, 'dataKey', 'Data Key', 'The data key is either a word to reference result objects, or it can be the name of a collection property of the result object.<br>' +
                'You can access result objects or the objects of the collection using ${<i>&lt;dataKey&gt;.&lt;propertyKey&gt;</i>}');



        //_Entities.appendInput(dialog, entity, 'partialUpdateKey', 'Types to trigger partial update', '');

    },
    activateTabs: function(elId, activeId) {
        var el = $(elId);
        var tabs = $('li', el);
        $.each(tabs, function(i, tab) {
            $(tab).on('click', function() {
                var tab = $(this);
                tabs.removeClass('active');
                tab.addClass('active');
                el.children('div').hide();
                var id = tab.prop('id').substring(4);
                var content = $('#content-tab-' + id);
                content.show();
            });
        });
        var id = activeId.substring(9);
        var tab = $('#' + id);
        tab.click();
    },
    editSource: function(entity) {

        Structr.dialog('Edit source of "' + (entity.name ? entity.name : entity.id) + '"', function() {
            log('Element source saved')
        }, function() {
            log('cancelled')
        });

        // Get content in widget mode
        var url = viewRootUrl + entity.id + '?edit=3', contentType = 'text/html';

        $.ajax({
            url: url,
            //async: false,
            contentType: contentType,
            success: function(data) {
                text = data;
                dialog.append('<div class="editor"></div>');
                var contentBox = $('.editor', dialog);
                editor = CodeMirror(contentBox.get(0), {
                    value: unescapeTags(text),
                    mode: contentType,
                    lineNumbers: true
                });

                editor.id = entity.id;

                dialogBtn.append('<button id="saveFile" disabled="disabled" class="disabled"> Save </button>');
                dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');

                dialogSaveButton = $('#saveFile', dialogBtn);
                saveAndClose = $('#saveAndClose', dialogBtn);

                $('.CodeMirror-code .cm-attribute:contains("data-hash")').addClass('data-hash').next().addClass('data-hash');
                editor.on('scroll', function() {
                    $('.CodeMirror-code .cm-attribute:contains("data-hash")').addClass('data-hash').next().addClass('data-hash');
                });
                editor.on('change', function(cm, change) {

                    //text1 = $(contentNode).children('.content_').text();
                    text2 = editor.getValue();

                    if (text === text2) {
                        dialogSaveButton.prop("disabled", true).addClass('disabled');
                        saveAndClose.prop("disabled", true).addClass('disabled');
                    } else {
                        dialogSaveButton.prop("disabled", false).removeClass('disabled');
                        saveAndClose.prop("disabled", false).removeClass('disabled');
                    }

                    $('.CodeMirror-code .cm-attribute:contains("data-hash")').addClass('data-hash').next().addClass('data-hash');
                });

                dialogSaveButton.on('click', function(e) {
                    e.stopPropagation();
                    var text2 = editor.getValue();

                    if (text === text2) {
                        dialogSaveButton.prop("disabled", true).addClass('disabled');
                        saveAndClose.prop("disabled", true).addClass('disabled');
                    } else {
                        dialogSaveButton.prop("disabled", false).removeClass('disabled');
                        saveAndClose.prop("disabled", false).removeClass('disabled');
                    }

                    Command.savePage(text2, entity.id, function() {
                        $.ajax({
                            url: url,
                            contentType: contentType,
                            success: function(data) {
                                editor.setValue(unescapeTags(data));
                            }
                        });

                        dialogSaveButton.prop("disabled", true).addClass('disabled');
                        saveAndClose.prop("disabled", true).addClass('disabled');
                        dialogMsg.html('<div class="infoBox success">Page source saved and rebuilt DOM tree.</div>');
                        $('.infoBox', dialogMsg).delay(2000).fadeOut(200);

                    });

                });

                saveAndClose.on('click', function(e) {
                    e.stopPropagation();
                    dialogSaveButton.click();
                    setTimeout(function() {
                        dialogSaveButton.remove();
                        saveAndClose.remove();
                        dialogCancelButton.click();
                    }, 500);
                });

            },
            error: function(xhr, statusText, error) {
                console.log(xhr, statusText, error);
            }
        });

    },
    showProperties: function(entity) {

        var views, activeView = 'ui';

        if (isIn(entity.type, ['Comment', 'Content', 'Page', 'User', 'Group', 'Image', 'File', 'Folder', 'Widget'])) {
            views = ['ui', 'in', 'out'];
        } else {
            views = ['_html_', 'ui', 'in', 'out'];
            activeView = '_html_';
        }

        var tabTexts = [];
        tabTexts._html_ = 'HTML Attributes';
        tabTexts.ui = 'Node Properties';
        tabTexts.in = 'Incoming Relationships';
        tabTexts.out = 'Outgoing Relationships';

        //dialog.empty();
        Structr.dialog('Edit Properties of ' + (entity.name ? entity.name : entity.id), function() {
            return true;
        }, function() {
            return true;
        });

        dialog.append('<div id="tabs"><ul></ul></div>');
        var mainTabs = $('#tabs', dialog);

        if (!isIn(entity.type, ['Comment', 'Content', 'User', 'Group', 'Image', 'File', 'Folder', 'Widget'])) {

            _Entities.appendPropTab(mainTabs, 'query', 'Query and Data Binding', true, function(c) {
                _Entities.queryDialog(entity, c);
                _Entities.activateTabs('#data-tabs', '#content-tab-rest');
            });

            _Entities.appendPropTab(mainTabs, 'editBinding', 'Edit Mode Binding', false, function(c) {
                _Entities.dataBindingDialog(entity, c);
            });
        }

        _Entities.appendViews(entity, views, tabTexts, mainTabs, activeView);


    },
    appendPropTab: function(el, name, label, active, callback) {
        var ul = el.children('ul');
        ul.append('<li id="tab-' + name + '">' + label + '</li>');
        var tab = $('#tab-' + name + '');
        if (active) {
            tab.addClass('active');
        }
        tab.on('click', function(e) {
            e.stopPropagation();
            var self = $(this);
            $('.propTabContent').hide();
            $('li', ul).removeClass('active');
            var c = $('#tabView-' + name + '');
            c.show().children().remove();
            if (callback) {
                callback(c);
            }
            self.addClass('active');
        });
        el.append('<div class="propTabContent" id="tabView-' + name + '"></div>');
        var content = $('#tabView-' + name);
        if (active) {
            content.show();
        }
        if (callback) {
            callback(content);
        }
        return content;
    },
    appendViews: function(entity, views, texts, tabs, activeView) {

        $(views).each(function(i, view) {

            var tabText = texts[view];

            tabs.children('ul').append('<li id="tab-' + view + '">' + tabText + '</li>');

            tabs.append('<div class="propTabContent" id="tabView-' + view + '"></div>');

            var tab = $('#tab-' + view);

            tab.on('click', function(e) {
                e.stopPropagation();
                var self = $(this);
                tabs.children('div').hide();
                $('li', tabs).removeClass('active');
                self.addClass('active');
                var tabView = $('#tabView-' + view);
                tabView.empty();
                tabView.show();

                $.ajax({
                    url: rootUrl + entity.id + (view ? '/' + view : '') + '?pageSize=10',
                    dataType: 'json',
                    contentType: 'application/json; charset=utf-8',
                    success: function(data) {
                        // Default: Edit node id
                        var id = entity.id;
                        // ID of graph object to edit
                        $(data.result).each(function(i, res) {

                            // reset id for each object group
                            id = entity.id;
                            var keys = Object.keys(res);
                            tabView.append('<table class="props ' + view + ' ' + res['id'] + '_"></table>');

                            var props = $('.props.' + view + '.' + res['id'] + '_', tabView);

                            $(keys).each(function(i, key) {

                                if (view === '_html_') {
                                    if (key !== 'id') {
                                        props.append('<tr><td class="key">' + key.replace(view, '') + '</td>'
                                                + '<td class="value ' + key + '_">' + formatValueInputField(key, res[key]) + '</td><td><img class="nullIcon" id="null_' + key + '" src="icon/cross_small_grey.png"></td></tr>');
                                    }
                                } else if (view === 'in' || view === 'out') {
                                    if (key === 'id') {
                                        // set ID to rel ID
                                        id = res[key];
                                    }
                                    props.append('<tr><td class="key">' + key + '</td><td rel_id="' + id + '" class="value ' + key + '_">' + formatValueInputField(key, res[key]) + '</td><td><img class="nullIcon" id="null_' + key + '" src="icon/cross_small_grey.png"></td></tr>');
                                } else {
                                    if (!key.startsWith('_html_') && !isIn(key, _Entities.hiddenAttrs)) {
                                        if (isIn(key, _Entities.readOnlyAttrs)) {
                                            props.append('<tr><td class="key">' + formatKey(key) + '</td>'
                                                    + '<td class="value ' + key + '_ readonly"><input type="text" class="readonly" readonly value="' + res[key] + '"></td><td></td></tr>');
                                        } else if (isIn(key, _Entities.booleanAttrs)) {
                                            props.append('<tr><td class="key">' + formatKey(key) + '</td><td><input type="checkbox" class="' + key + '_"></td><td></td></tr>');
                                            var checkbox = $(props.find('.' + key + '_'));
                                            checkbox.on('change', function() {
                                                var checked = checkbox.prop('checked');
                                                log('set property', id, key, checked);
                                                Command.setProperty(id, key, checked);
                                            });
                                            Command.getProperty(id, key, function(val) {
                                                if (val)
                                                    checkbox.prop('checked', true);
                                            });
                                        } else if (isIn(key, _Entities.dateAttrs)) {
                                            if (!res[key] || res[key] === 'null') {
                                                res[key] = '';
                                            }
                                            props.append('<tr><td class="key">' + formatKey(key) + '</td><td class="value ' + key + '_"><input class="dateField" name="' + key + '" type="text" value="' + res[key] + '"></td><td><img class="nullIcon" id="null_' + key + '" src="icon/cross_small_grey.png"></td></tr>');
                                            var dateField = $(props.find('.dateField'));
                                            dateField.datetimepicker({
                                                showSecond: true,
                                                timeFormat: 'hh:mm:ssz',
                                                dateFormat: 'yy-mm-dd',
                                                separator: 'T'
                                            });
                                        } else {
                                            props.append('<tr><td class="key">' + formatKey(key) + '</td><td class="value ' + key + '_">' + formatValueInputField(key, res[key]) + '</td><td><img class="nullIcon" id="null_' + key + '" src="icon/cross_small_grey.png"></td></tr>');
                                        }
                                    }
                                }

                                var nullIcon = $('#null_' + key);
                                nullIcon.on('click', function() {
                                    var key = $(this).prop('id').substring(5);
                                    Command.setProperty(id, key, null, false, function() {
                                        var inp = $('.' + key + '_').find('input');
                                        inp.val(null);
                                        blinkGreen(inp);
                                        dialogMsg.html('<div class="infoBox success">Property "' + key + '" was set to null.</div>');
                                        $('.infoBox', dialogMsg).delay(2000).fadeOut(1000);
                                    });
                                });
                            });
                            props.append('<tr><td class="key"><input type="text" class="newKey" name="key"></td><td class="value"><input type="text" value=""></td><td></td></tr>');
                            $('.props tr td.value input', dialog).each(function(i, v) {
                                _Entities.activateInput(v, id);
                            });
                        });
                    }
                });

            });
        });

        $('#tab-' + activeView).click();

    },
    activateInput: function(el, id) {

        var input = $(el);
        var oldVal = input.val();
        var relId = input.parent().attr('rel_id');

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
                    var val = input.val();

                    // new key
                    log('new key: Command.setProperty(', objId, newKey, val);
                    Command.setProperty(objId, newKey, val, false, function() {
                        blinkGreen(input);
                        dialogMsg.html('<div class="infoBox success">New property "' + newKey + '" was added and saved with value "' + val + '".</div>');
                        $('.infoBox', dialogMsg).delay(2000).fadeOut(1000);
                    });


                } else {

                    var key = input.prop('name');
                    var val = input.val();

                    if (input.data('changed')) {

                        input.data('changed', false);

                        // existing key
                        log('existing key: Command.setProperty(', objId, key, val);

                        input.val(oldVal);

                        Command.setProperty(objId, key, val, false, function() {
                            input.val(val);
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

                dialogText.append('<h3>Visibility</h3>');

                //('<div class="' + entity.id + '_"><button class="switch disabled visibleToPublicUsers_">Public (visible to anyone)</button><button class="switch disabled visibleToAuthenticatedUsers_">Authenticated Users</button></div>');

                if (entity.type === 'Folder' || (lastMenuEntry === 'pages' && !(entity.type === 'Content'))) {
                    dialogText.append('<div>Apply visibility switches recursively? <input id="recursive" type="checkbox" name="recursive"></div><br>');
                }

                _Entities.appendBooleanSwitch(dialogText, entity, 'visibleToPublicUsers', ['Visible to public users', 'Not visible to public users'], 'Click to toggle visibility for users not logged-in', '#recursive');
                _Entities.appendBooleanSwitch(dialogText, entity, 'visibleToAuthenticatedUsers', ['Visible to auth. users', 'Not visible to auth. users'], 'Click to toggle visibility to logged-in users', '#recursive');

                dialogText.append('<h3>Access Rights</h3>');
                dialogText.append('<table class="props" id="principals"><thead><tr><th>Name</th><th>Read</th><th>Write</th><th>Delete</th><th>Access Control</th></tr></thead><tbody></tbody></table');

                var tb = $('#principals tbody', dialogText);
                tb.append('<tr id="new"><td><select id="newPrincipal"><option></option></select></td><td><input id="newRead" type="checkbox" disabled="disabled"></td><td><input id="newWrite" type="checkbox" disabled="disabled"></td><td><input id="newDelete" type="checkbox" disabled="disabled"></td><td><input id="newAccessControl" type="checkbox" disabled="disabled"></td></tr>');
                Command.getByType('User', 1000, 1, 'name', 'asc', function(user) {
                    $('#newPrincipal').append('<option value="' + user.id + '">' + user.name + '</option>');
                });
                Command.getByType('Group', 1000, 1, 'name', 'asc', function(group) {
                    $('#newPrincipal').append('<option value="' + group.id + '">' + group.name + '</option>');
                });
                $('#newPrincipal').on('change', function() {
                    var sel = $(this);
                    var pId = sel[0].value;
                    var rec = $('#recursive', dialogText).is(':checked');
                    Command.setPermission(entity.id, pId, 'grant', 'read', rec);
                    $('#new', tb).selectedIndex = 0;

                    Command.get(pId, function(p) {
                        addPrincipal(entity, p, {'read': true});
                    });

                });

                $.ajax({
                    url: rootUrl + '/' + entity.id + '/in',
                    dataType: 'json',
                    contentType: 'application/json; charset=utf-8',
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
    appendTextarea: function(el, entity, key, label, desc) {
        if (!el || !entity) {
            return false;
        }
        el.append('<div><h3>' + label + '</h3><p>' + desc + '</p><textarea class="query-text" id="' + key + '_">' + (entity[key] ? entity[key] : '') + '</textarea></div>');
        el.append('<div><button id="apply_' + key + '">Save</button></div>');
        var btn = $('#apply_' + key, el);
        btn.on('click', function() {
            entity.setProperty(key, $('#' + key + '_', el).val(), false, function() {
                log(key + ' successfully updated!', entity[key]);
                blinkGreen(btn);
                _Pages.reloadPreviews();
            });
        });
    },
    appendInput: function(el, entity, key, label, desc) {
        if (!el || !entity) {
            return false;
        }
        el.append('<div><h3>' + label + '</h3><p>' + desc + '</p><input type="text" id="' + key + '_" value="' + (entity[key] ? entity[key] : '') + '"><button id="save_' + key + '">Save</button></div>');
        var btn = $('#save_' + key, el);
        btn.on('click', function() {
            entity.setProperty('dataKey', $('#' + key + '_').val(), false, function() {
                log(key + ' successfully updated!', entity[key]);
                blinkGreen(btn);
                _Pages.reloadPreviews();
            });
        });
    },
    appendBooleanSwitch: function(el, entity, key, label, desc, recElementId) {
        if (!el || !entity) {
            return false;
        }
        el.append('<div class="' + entity.id + '_"><button class="switch inactive ' + key + '_"></button>' + desc + '</div>');
        var sw = $('.' + key + '_', el);
        _Entities.changeBooleanAttribute(sw, entity[key], label[0], label[1]);
        sw.on('click', function(e) {
            e.stopPropagation();
            entity.setProperty(key, sw.hasClass('inactive'), $(recElementId, el).is(':checked'), function(obj) {
                if (obj.id !== entity.id) {
                    return false;
                }
                _Entities.changeBooleanAttribute(sw, entity[key], label[0], label[1]);
                blinkGreen(sw);
                return true;
            });
        });
    },
    appendSimpleSelection: function(el, entity, type, title, key) {

        var subKey;
        if (key.contains('.')) {
            subKey = key.substring(key.indexOf('.') + 1, key.length);
            key = key.substring(0, key.indexOf('.'));
        }

        el.append('<h3>' + title + '</h3><p id="' + key + 'Box"></p>');
        var element = $('#' + key + 'Box');
        element.append('<span class="' + entity.id + '_"><select class="' + key + '_" id="' + key + 'Select"></select></span>');
        var selectElement = $('#' + key + 'Select');
        selectElement.append('<option></option>')

        $.ajax({
            url: rootUrl + type + '/ui?pageSize=100',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            success: function(data) {
                $(data.result).each(function(i, result) {

                    var id = (subKey && entity[key] ? entity[key][subKey] : entity[key]);
                    var selected = (id === result.id ? 'selected' : '');
                    selectElement.append('<option ' + selected + ' value="' + result.id + '">' + result.name + '</option>');
                });
            }
        });

        var select = $('#' + key + 'Select', element);
        select.on('change', function() {

            var value = select.val();
            if (subKey) {
                entity[key][subKey] = value;
            }

            entity.setProperty(key, value, false, function() {
                blinkGreen(select);
            });
        });
    },
    appendEditSourceIcon: function(parent, entity) {

        var editIcon = $('.edit_icon', parent);

        if (!(editIcon && editIcon.length)) {
            parent.append('<img title="Edit source code" alt="Edit source code" class="edit_icon button" src="' + '/structr/icon/pencil.png' + '">');
            editIcon = $('.edit_icon', parent);
        }
        editIcon.on('click', function(e) {
            e.stopPropagation();
            log('editSource', entity);
            _Entities.editSource(entity);
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
            log('Expand icon already existing', el, button);
            return;
        }

        if (hasChildren) {

            log('appendExpandIcon hasChildren?', hasChildren, 'expand?', expand)

            var typeIcon = $(el.children('.typeIcon').first());
            var icon = expand ? Structr.expanded_icon : Structr.expand_icon;

            typeIcon.css({
                paddingRight: 0 + 'px'
            }).after('<img title="Expand \'' + entity.name + '\'" alt="Expand \'' + entity.name + '\'" class="expand_icon" src="' + icon + '">');

            $(el).on('click', function(e) {
                e.stopPropagation();
                _Entities.toggleElement(this);
            });

            button = $(el.children('.expand_icon').first());

            if (button) {

                button.on('click', function(e) {
                    e.stopPropagation();
                    _Entities.toggleElement($(this).parent('.node'));
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
    makeSelectable: function(el) {
        var node = $(el).closest('.node');
        if (!node || !node.children) {
            return;
        }
        node.on('click', function() {
            $(this).toggleClass('selected');
        });
    },
    setMouseOver: function(el, allowClick, syncedNodes) {
        var node = $(el).closest('.node');
        if (!node || !node.children) {
            return;
        }

        if (!allowClick) {
            node.on('click', function(e) {
                return false;
            });
        }

        node.children('b.name_').on('click', function(e) {
            e.stopPropagation();
            _Entities.makeNameEditable(node);
        });

        var nodeId = getId(el), isComponent;
        if (nodeId === undefined) {
            nodeId = getComponentId(el);
            if (nodeId) {
                isComponent = true;
            } else {
                nodeId = getActiveElementId(el);
            }
        }

        node.on({
            mouseover: function(e) {
                e.stopPropagation();
                var self = $(this);
                $('#componentId_' + nodeId).addClass('nodeHover');
                if (isComponent) {
                    $('#id_' + nodeId).addClass('nodeHover');
                }

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
                if (isComponent) {
                    $('#id_' + nodeId).removeClass('nodeHover');
                }
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
            //$('#preview_' + getId(page)).contents().find('[data-structr-id=' + getId(node) + ']').removeClass('nodeHover');
            $('#preview_' + getId(page)).contents().find('[data-structr-id]').removeClass('nodeHover');
        }
    },
    ensureExpanded: function(element, callback) {

        var el = $(element);
        var b;
        var src = el.prop('src');

        var id = getId(el);

        addExpandedNode(id);

        b = el.children('.expand_icon').first();
        src = b.prop('src');

        if (!src)
            return;

        if (src.endsWith('icon/tree_arrow_down.png')) {
            return;
        } else {
            log('ensureExpanded: fetch children', el);
            Command.children(id, callback);
            b.prop('src', 'icon/tree_arrow_down.png');
        }
    },
    expandAll: function(ids) {
        if (!ids || ids.length === 0) {
            return;
        }

        ids.forEach(function(id) {
            var el = Structr.node(id);
            if (el) {
                $('.nodeSelected').removeClass('nodeSelected');
                el.addClass('nodeSelected');
            }
            _Entities.ensureExpanded(el, function(childNode) {
                var i = ids.indexOf(childNode.id);
                if (i > 1) {
                    ids.slice(i - 1, i);
                }
                _Entities.expandAll(ids);
            });
        });
    },
    toggleElement: function(element, expanded) {

        var el = $(element);
        var b;
        var src = el.prop('src');
        var id = getId(el) || getComponentId(el);

        log('toggleElement: ', el, id);

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
    makeNameEditable: function(element, width) {

        var w = width || 200;
        //element.off('dblclick');
        element.off('hover');
        var oldName = $.trim(element.children('b.name_').attr('title'));
        element.children('b.name_').replaceWith('<input type="text" size="' + (oldName.length + 4) + '" class="new-name" value="' + oldName + '">');
        element.find('.button').hide();

        var input = $('input', element);

        input.focus().select();

        input.on('blur', function() {
            var self = $(this);
            var newName = self.val();
            Command.setProperty(getId(element), "name", newName);
            self.replaceWith('<b title="' + newName + '" class="name_">' + fitStringToWidth(newName, w) + '</b>');
            $('.name_', element).on('click', function(e) {
                e.stopPropagation();
                _Entities.makeNameEditable(element, w);
            });
            _Pages.reloadPreviews();
        });

        input.keypress(function(e) {
            if (e.keyCode === 13) {
                var self = $(this);
                var newName = self.val();
                Command.setProperty(getId(element), "name", newName);
                self.replaceWith('<b title="' + newName + '" class="name_">' + fitStringToWidth(newName, w) + '</b>');
                $('.name_', element).on('click', function(e) {
                    e.stopPropagation();
                    _Entities.makeNameEditable(element, w);
                });
                _Pages.reloadPreviews();
            }
        });

        element.off('click');

    },
    handleActiveElement: function(entity) {

        if (entity) {

            var idString = 'id_' + entity.id;

            if (!activeElements.hasOwnProperty(idString)) {

                activeElements[idString] = entity;

                var parent = $('#activeElements div.inner');
                var id = entity.id;

                if (entity.parentId) {
                    parent = $('#active_' + entity.parentId);
                }

                parent.append('<div id="active_' + id + '" class="node active-element' + (entity.tag === 'html' ? ' html_element' : '') + ' "></div>');

                var div = $('#active_' + id);
                var query = entity.query;
                //var dataKey     = (entity.dataKey.split(',')[entity.recursionDepth] || '');
                var expand = entity.state === 'Query';
                var icon = _Elements.icon;
                var name = '', content = '', action = '';

                switch (entity.state) {
                    case 'Query':
                        icon = 'icon/database_table.png';
                        name = query || entity.dataKey.replace(',', '.');
                        break;
                    case 'Content':
                        icon = _Contents.icon;
                        content = entity.content ? entity.content : entity.type;
                        break;
                    case 'Button':
                        icon = 'icon/button.png';
                        action = entity.action;
                        break;
                    case 'Link':
                        icon = 'icon/link.png';
                        content = entity.action;
                        break;
                    default:
                        content = entity.state;
                }

                div.append('<img class="typeIcon" src="' + icon + '">'
                        + '<b title="' + name + '">' + fitStringToWidth(name, 180, 'slideOut') + '</b>'
                        + '<b class="action">' + action   + '</b    >'
                        + '<span class="content_">' + content + '</span>'
                        + '<span class="id">' + entity.id + '</span>'
//                        + (entity._html_id ? '<span class="_html_id_">#' + entity._html_id.replace(/\${.*}/g, '${…}') + '</span>' : '')
//                        + (entity._html_class ? '<span class="_html_class_">.' + entity._html_class.replace(/\${.*}/g, '${…}').replace(/ /g, '.') + '</span>' : '')
                        );

                _Entities.setMouseOver(div);

                var editIcon = $('.edit_icon', div);

                if (!(editIcon && editIcon.length)) {
                    div.append('<img title="Edit" alt="Edit" class="edit_icon button" src="' + '/structr/icon/pencil.png' + '">');
                    editIcon = $('.edit_icon', div);
                }
                editIcon.on('click', function(e) {
                    e.stopPropagation();
                    
                    switch (entity.state) {
                        case 'Query':
                            _Entities.openQueryDialog(entity.id);
                           break;
                        case 'Content':
                            _Contents.openEditContentDialog(this, entity);
                            break;
                        case 'Button':
                            _Entities.openEditModeBindingDialog(entity.id);
                            break;
                        case 'Link':
                            _Entities.showProperties(entity);
                            break;
                        default:
                            _Entities.showProperties(entity);
                    }
                    
                });

                $('b[title]', div).on('click', function() {
                    _Entities.openQueryDialog(entity.id);
                });

                $('.content_', div).on('click', function() {
                    _Contents.openEditContentDialog(this, entity);
                });

                $('.action', div).on('click', function() {
                    _Entities.openEditModeBindingDialog(entity.id);
                });

                var typeIcon = $(div.children('.typeIcon').first());
                var padding = 0;

                if (!expand) {
                    padding = 11;
                } else {
                    typeIcon.css({
                        paddingRight: padding + 'px'
                    }).after('<img title="Expand \'' + entity.name + '\'" alt="Expand \'' + entity.name + '\'" class="expand_icon" src="' + Structr.expanded_icon + '">');
                }
            }
        }
    },
    openQueryDialog: function(id) {
        Command.get(id, function(obj) {
            
            var entity = StructrModel.create(obj);
            
            Structr.dialog('Query and Data Binding of ' + (entity.name ? entity.name : entity.id), function() {
                return true;
            }, function() {
                return true;
            });

            dialogText.append('<p></p>');

            _Entities.queryDialog(entity, dialogText);

            if (entity.restQuery) {
                _Entities.activateTabs('#data-tabs', '#content-tab-rest');
            } else if (entity.cypherQuery) {
                _Entities.activateTabs('#data-tabs', '#content-tab-cypher');
            } else if (entity.xpathQuery) {
                _Entities.activateTabs('#data-tabs', '#content-tab-xpath');
            } else {
                _Entities.activateTabs('#data-tabs', '#content-tab-rest');
            }
        });
    },
    openEditModeBindingDialog: function(id) {
        Command.get(id, function(obj) {
            
            var entity = StructrModel.create(obj);
            
            Structr.dialog('Edit mode binding for ' + (entity.name ? entity.name : entity.id), function() {
                return true;
            }, function() {
                return true;
            });

            dialogText.append('<p></p>');

            _Entities.dataBindingDialog(entity, dialogText);

        });
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
            if (!$('input:checked', row).length) {
                $('#newPrincipal').append('<option value="' + row.attr('id').substring(1) + '">' + $('.name', row).text() + '</option>');
                row.remove();
            }
            var rec = $('#recursive', dialogText).is(':checked');

            Command.setPermission(entity.id, principal.id, permissions[perm] ? 'revoke' : 'grant', perm, rec, function() {
                permissions[perm] = !permissions[perm];
                sw.prop('checked', permissions[perm]);
                log('Permission successfully updated!');
                blinkGreen(sw.parent());


            });
        });
    });
}
