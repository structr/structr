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

var propertyDefinitions, types, dynamicTypes = [];
var typesTypeKey = 'structrTypesType_' + port;

$(document).ready(function() {
    Structr.registerModule('types', _Types);
    Structr.classes.push('types');

    if (!_Types.type) {
        _Types.restoreType();
    }

    if (!_Types.type) {
        _Types.type = urlParam('type');
    }

    if (!_Types.type) {
        _Types.type = defaultType;
    }

});

var _Types = {
    type_icon: 'icon/database_table.png',
    schemaLoading: false,
    schemaLoaded: false,
    //allTypes : [],

    types: [], //'Page', 'User', 'Group', 'Folder', 'File', 'Image', 'Content', 'PropertyDefinition' ],
    views: ['public', 'all', 'ui'],
    schema: [],
    keys: [],
    type: null,
    pageCount: null,
    view: [],
    sort: [],
    order: [],
    page: [],
    pageSize: [],
    init: function() {

        _Types.schemaLoading = false;
        _Types.schemaLoaded = false;
        _Types.schema = [];
        _Types.keys = [];

        _Types.loadSchema(function() {
            _Types.initTabs();
        });

        main.append('<div id="resourceTabs"><ul id="resourceTabsMenu"></ul></div>');

        $(document).keyup(function(e) {
            if (e.keyCode === 27) {
                dialogCancelButton.click();
            }
        });

        $(window).on('resize', function() {
            _Crud.resize();
        });

    },
    onload: function() {
        _Types.init();
    },
    initTabs: function() {

        $.each(_Types.types, function(t, type) {
            _Types.addTab(type);
        });

        $('#resourceTabs').tabs({
            activate: function(event, ui) {
                //_Types.clearList(_Types.type);
                var newType = ui.newPanel[0].id;
                //console.log('deactivated', _Types.type, 'activated', newType);
                _Types.type = newType;
                _Types.updateUrl(newType);
            }
        });

        var t = $('a[href="#' + _Types.type + '"]');
        t.click();

    },
    /**
     * Read the schema from the _schema REST resource and call 'callback'
     * after the complete schema is loaded.
     */
    loadSchema: function(callback) {
        // Avoid duplicate loading of schema
        if (_Types.schemaLoading) {
            return;
        }
        _Types.schemaLoading = true;

        _Types.loadAccessibleResources(function() {
            $.each(_Types.types, function(t, type) {
                //console.log('Loading type definition for ' + type + '...');
                if (type.startsWith('_')) {
                    return;
                }
                _Types.loadTypeDefinition(type, callback);
            });
        });

    },
    isSchemaLoaded: function() {
        var all = true;
        if (!_Types.schemaLoaded) {
            //console.log('schema not loaded completely, checking all types ...');
            $.each(_Types.types, function(t, type) {
                //console.log('checking type ' + type, (_Types.schema[type] && _Types.schema[type] != null));
                all &= (_Types.schema[type] && _Types.schema[type] !== null);
            });
        }
        _Types.schemaLoaded = all;
        return _Types.schemaLoaded;
    },
    loadAccessibleResources: function(callback) {
        var url = rootUrl + 'resource_access/ui';
        $.ajax({
            url: url,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                var types = [];
                _Types.types.length = 0;
                $.each(data.result, function(i, res) {
                    var type = getTypeFromResourceSignature(res.signature);
                    //console.log(res);
                    if (type && !(type.startsWith('_')) && !isIn(type, _Types.types)) {
                        _Types.types.push(type);
                        types.push({'type': type, 'position': res.position});
                    }
                });
                //console.log(types);
                types.sort(function(a, b) {
                    return a.position - b.position;
                });

                _Types.types.length = 0; // best way to empty array

                $.each(types, function(i, typeObj) {
                    _Types.types.push(typeObj.type);
                });

                if (callback) {
                    callback();
                }

            }
        });
    },
    loadTypeDefinition: function(type, callback) {

        //_Types.schema[type] = [];
        //console.log(type);
        var url = rootUrl + '_schema/' + type.toUnderscore();
        $.ajax({
            url: url,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            statusCode: {
                200: function(data) {

                    $.each(data.result, function(i, res) {
                        //console.log(res);
                        var type = res.type;

                        _Types.schema[type] = res;
                        _Types.view[type] = 'all';
                        //console.log('Type definition for ' + type + ' loaded');
                        //console.log('schema loaded?', _Types.isSchemaLoaded());

                        if (_Types.isSchemaLoaded()) {
                            //console.log('Schema loaded successfully');
                            if (callback) {
                                callback();
                            }
                        }

                    });

                },
                401: function(data) {
                    console.log(data);
                    Structr.errorFromResponse(data.responseJSON, url);
                },
                422: function(data) {
                    Structr.errorFromResponse(data.responseJSON);
                }
            }


        });
    },
    addTab: function(type) {
        var res = _Types.schema[type];
        $('#resourceTabsMenu').append('<li><a href="#' + type + '"><span>' + _Crud.formatKey(type) + '</span></a></li>');
        $('#resourceTabs').append('<div class="resourceBox" id="' + type + '" data-url="' + res.url + '"></div>');
        var typeNode = $('#' + type);
        typeNode.append('<div>Type: ' + res.type + '</div>');
        typeNode.append('<div>URL: <a target="_blank" href="' + rootUrl + res.url.substring(1) + '">' + res.url + '</a></div>');
        typeNode.children('.add_property').on('click', function(e) {
            console.log('clicked on add property, type', type);
            _Types.propertyDialog(type);
            return false;
        });
        typeNode.append('<table class="props"><thead><tr>\n\
<th>Key</th>\n\
<th>JSON name</th>\n\
<th>DB name</th>\n\
<th>Data Type</th>\n\
<th>Related Type</th>\n\
<th>Input Converter</th>\n\
<th>Collection</th>\n\
<th>Read Only</th>\n\
<th>System Property</th>\n\
<th>Default Value</th>\n\
<th>Declaring Class</th>\n\
</tr></thead><tbody></tbody></table>');
        var tb = $('tbody', typeNode);
        var view = res.views[_Types.view[type]];
        if (view) {
            var k = Object.keys(view);
            if (k && k.length) {
                _Types.keys[type] = k;

                $.each(_Types.keys[type], function(k, key) {

                    var raw = view[key].className, cls;

                    if (raw === 'org.structr.core.entity.PropertyDefinition') {

                        //console.log('custom property for kind', type, ': ', key, view[key]);
                        var url = rootUrl + 'property_definitions?kind=' + type + '&name=' + key;
                        $.ajax({
                            url: url,
                            dataType: 'json',
                            contentType: 'application/json; charset=utf-8',
                            //async: false,
                            success: function(data) {
                                var pd = data.result[0];
                                cls = pd.dataType;
                                $('.' + key + ' .dataType').html('<select id="_dataType_' + pd.id + '">\n\
<option>String</option>\n\
<option>Integer</option>\n\
<option>Long</option>\n\
<option>Double</option>\n\
<option>Boolean</option>\n\
<option>Date</option>\n\
<option>Collection</option>\n\
<option>Entity</option>\n\
</select>');

                                $('.' + key + ' .key').html('<input type="text" size="30" id="_key_' + pd.id + '" value="' + key + '">');
                                $('.' + key + ' .readOnly').html('<input type="checkbox" id="_readOnly_' + pd.id + '"' + (pd.readOnlyProperty ? '" checked="checked"' : '') + '>');
                                $('.' + key + ' .system').html('<input type="checkbox" id="_system_' + pd.id + '"' + (pd.systemProperty ? '" checked="checked"' : '') + '>');
                                $('.' + key + ' .defaultValue').html('<input type="text" size="30" id="_defaultValue_' + pd.id + '" value="' + nvl(pd.defaultValue, '') + '">');

                            }
                        });


                    } else {
                        cls = raw.substring(raw.lastIndexOf('.') + 1);
                        cls = cls.substring(0, cls.length - 'Property'.length);
                    }

                    tb.append('<tr class="' + key + '">\n\
<td class="key">' + key + '</td>\n\
<td>' + view[key].jsonName + '</td>\n\
<td>' + view[key].dbName + '</td>\n\
<td class="dataType" title="' + raw + '">' + cls + '</td>\n\
<td>' + nvl(view[key].relatedType, '') + '</td>\n\
<td>' + nvl(view[key].inputConverter, '') + '</td>\n\
<td>' + (view[key].isCollection ? '<img src="icon/accept.png">' : '') + '</td>\n\
<td class="readOnly">' + (view[key].readOnly ? '<img src="icon/accept.png">' : '') + '</td>\n\
<td class="system">' + (view[key].system ? '<img src="icon/accept.png">' : '') + '</td>\n\
<td class="defaultValue">' + nvl(view[key].defaultValue, '') + '</td>\n\
<td>' + nvl(view[key].declaringClass, '') + '</td>\n\
</tr>');
                });
            }
        }

        typeNode.append('<button class="add_property" class="btn"><img class="add_button icon" src="icon/add.png"> Add property</button>');

        _Crud.resize();

    },
    propertyDialog: function(type) {
        console.log(type);
        Structr.dialog('Add property', function() {
            return true;
        }, function() {
            return true;
        });

        dialog.append('<div><label for="attr">Attribute name:</label><input id="_attr" type="text" name="attr"></div>');
        dialog.append('<div><label for="dataType">Data Type of first attribute:</label><select id="_dataType">\n\
<option>String</option>\n\
<option>Integer</option>\n\
<option>Long</option>\n\
<option>Double</option>\n\
<option>Boolean</option>\n\
<option>Date</option>\n\
<option>Collection</option>\n\
<option>Entity</option>\n\
</select></div><br>');
        dialog.append('<button id="addProperty">Add property</button>');

        $('input#_attr').focus();

        $('#addProperty').on('click', function(e) {
            e.stopPropagation();

            var attr = $('#_attr', dialog).val();
            var dataType = $('#_dataType', dialog).val();
            var entity = {};
            entity.type = 'PropertyDefinition';
            entity.kind = type;
            entity.name = attr;
            entity.dataType = dataType;

            Command.create(entity, function() {
                window.location.reload(true);
            });
        });

    },
    addGrants: function(type) {
        Command.create({type: 'ResourceAccess', flags: 255, signature: type, visibleToPublicUsers: true});
        Command.create({type: 'ResourceAccess', flags: 255, signature: type + '/_Ui'});
        Command.create({type: 'ResourceAccess', flags: 255, signature: type + '/_Public'});
        Command.create({type: 'ResourceAccess', flags: 255, signature: type + '/_All'});
        Command.create({type: 'ResourceAccess', flags: 255, signature: '_schema/' + type});
    },
//    refreshPropertyDefinitions: function() {
//
//        Command.list('PropertyDefinition', undefined, undefined, undefined, undefined, function(entity) {
//            
//            console.log(entity);
//            
//        });
//    },
//    registerPropertyDefinitionElement: function(propertyDefinition) {
//
//        var p = propertyDefinition;
//
//        if (p.kind && p.name) {
//
//            dynamicTypes[p.kind] = dynamicTypes[p.kind] || {};
//            dynamicTypes[p.kind][p.name] = dynamicTypes[p.kind][p.name] || {};
//
//            dynamicTypes[p.kind][p.name].dataType = p.dataType;
//            dynamicTypes[p.kind][p.name].relKind = p.relKind;
//            dynamicTypes[p.kind][p.name].relType = p.relType;
//            dynamicTypes[p.kind][p.name].incoming = p.incoming;
//            dynamicTypes[p.kind][p.name].systemProperty = p.systemProperty;
//            dynamicTypes[p.kind][p.name].readOnlyProperty = p.readOnlyProperty;
//            dynamicTypes[p.kind][p.name].writeOnceProperty = p.writeOnceProperty;
//            dynamicTypes[p.kind][p.name].indexedProperty = p.indexedProperty;
//            dynamicTypes[p.kind][p.name].passivelyIndexedProperty = p.passivelyIndexedProperty;
//            dynamicTypes[p.kind][p.name].searchableProperty = p.searchableProperty;
//            dynamicTypes[p.kind][p.name].indexedWhenEmptyProperty = p.indexedWhenEmptyProperty;
//
//        }
//
//        //_Types.updateDynamicTypes();
//    },
//    updateDynamicTypes: function() {
//        Object.keys(dynamicTypes).forEach(function(type) {
//            if (types.children('#_' + type).length)
//                return;
//            types.append('<tr id="_' + type + '"><td>' + type + '</td></tr>');
//        });
//    }
    updateUrl: function(type) {
        if (type) {
            _Types.storeType();
            window.location.hash = '#types';
        }
        //searchField.focus();
    },
    storeType: function() {
        localStorage.setItem(typesTypeKey, _Types.type);
    },
    restoreType: function() {
        var val = localStorage.getItem(typesTypeKey);
        if (val) {
            _Types.type = val;
        }
    },
};