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

    types: [], //'Page', 'User', 'Group', 'Folder', 'File', 'Image', 'Content' ],
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

        $(window).off('resize');
        $(window).on('resize', function() {
            _Types.resize();
        });
        _Types.resize();

        main.append('<div id="resourceTabs"><ul id="resourceTabsMenu"></ul></div>');

        Structr.ensureIsAdmin($('#resourceTabs'), function() {

            _Types.schemaLoading = false;
            _Types.schemaLoaded = false;
            _Types.schema = [];
            _Types.keys = [];

            _Types.loadSchema(function() {
                _Types.initTabs();
            });

        });

    },
    onload: function() {
        
        _Types.init();
        
        $('#main-help a').attr('href', 'http://docs.structr.org/frontend-user-guide#Types');
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

        _Types.resize();


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


                _Types.types.sort();

                if (callback) {
                    callback();
                }

            }
        });
    },
    loadTypeDefinition: function(type, callback) {

        //_Types.schema[type] = [];
        //console.log(type);
        var url = rootUrl + '_schema/' + type;
        $.ajax({
            url: url,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            statusCode: {
                200: function(data) {

                    // no schema entry found?
                    if(data.result_count === 0){

                        console.log("ERROR: loading Schema " + type);
                        //Structr.error("ERROR: loading Schema " + type, true);


                        var typeIndex = _Types.types.indexOf(type);

                        // Delete broken type from list
                        _Types.types.splice(typeIndex, 1);


                        if (_Types.isSchemaLoaded()) {
                            //console.log('Schema loaded successfully');
                            if (callback) {
                                callback();
                            }
                        }
                    }else{

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
                    }

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

                    cls = raw.substring(raw.lastIndexOf('.') + 1);
                    cls = cls.replace('Property', '');

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

        _Types.resize();

    },
    addGrants: function(type) {
        Command.create({type: 'ResourceAccess', flags: 255, signature: type, visibleToPublicUsers: true});
        Command.create({type: 'ResourceAccess', flags: 255, signature: type + '/_Ui'});
        Command.create({type: 'ResourceAccess', flags: 255, signature: type + '/_Public'});
        Command.create({type: 'ResourceAccess', flags: 255, signature: type + '/_All'});
        Command.create({type: 'ResourceAccess', flags: 255, signature: '_schema/' + type});
    },
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
    resize: function() {
        var w = $(window).width();
        var h = $(window).height();

        var ml = 0;
        var mt = 24;

        // Calculate dimensions of dialog
        var dw = Math.min(900, w - ml);
        var dh = Math.min(600, h - mt);
        //            var dw = (w-24) + 'px';
        //            var dh = (h-24) + 'px';

        var l = parseInt((w - dw) / 2);
        var t = parseInt((h - dh) / 2);

//        $('.blockPage').css({
//            width: dw + 'px',
//            height: dh + 'px',
//            top: t + 'px',
//            left: l + 'px'
//        });

        var bw = (dw - 28) + 'px';
        var bh = (dh - 106) + 'px';

        $('#dialogBox .dialogTextWrapper').css({
            width: bw,
            height: bh
        });

        $('#resourceTabs .resourceBox table').css({
            height: h - ($('#resourceTabsMenu').height() + 154) + 'px',
            width:  w - 59 + 'px'
        });

        $('.searchResults').css({
            height: h - 103 + 'px'
        });

    }
};