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

$(document).ready(function() {
    Structr.registerModule('types', _Types);
    Structr.classes.push('types');
});

var _Types = {
    
    type_icon : 'icon/database_table.png',

    schemaLoading : false,
    schemaLoaded : false,
    
    //allTypes : [],
    
    types : [],//'Page', 'User', 'Group', 'Folder', 'File', 'Image', 'Content', 'PropertyDefinition' ],
    views : [ 'public', 'all', 'ui' ],

    schema : [],
    keys : [],

    type : null,
    pageCount : null,

    view : [],
    sort : [],
    order : [],
    page : [],
    pageSize : [],
    
    init : function() {
        //Structr.initPager('PropertyDefinition', 1, 100);
        
        _Types.schemaLoading = false;
        _Types.schemaLoaded = false;
        _Types.schema = [];
        _Types.keys = [];

        _Types.loadSchema(function() {
            _Types.initTabs();
        });
        
        main.append('<div id="resourceTabs"><ul id="resourceTabsMenu"></ul></div>');

        main.append('<button id="add_type" class="btn"><img class="add_button icon" src="icon/add.png"> Create new custom type</button>');
        $('#add_type').on('click', function(e) {
            _Types.addTypeDialog();
            return false;
        });
        
        $(document).keyup(function(e) {
            if (e.keyCode === 27) {
                dialogCancelButton.click();
            }
        });

        $(window).on('resize', function() {
            _Crud.resize();
        });
        
    },
	
    onload : function() {
        _Types.init();
    },

    initTabs : function() {
        
        $.each(_Types.types, function(t, type) {
            _Types.addTab(type);
        });

        $('#resourceTabs').tabs({
            activate: function(event, ui) {
                //_Types.clearList(_Types.type);
                var newType = ui.newPanel[0].id;
                //console.log('deactivated', _Types.type, 'activated', newType);
                _Types.type = newType;
            }
        });

    },

    /**
     * Read the schema from the _schema REST resource and call 'callback'
     * after the complete schema is loaded.
     */
    loadSchema : function(callback) {
        // Avoid duplicate loading of schema
        if (_Types.schemaLoading) {
            return;
        }
        _Types.schemaLoading = true;
        
        _Types.loadAccessibleResources(function() {
            $.each(_Types.types, function(t, type) {
                //console.log('Loading type definition for ' + type + '...');
                if (type.startsWith('_schema')) {
                    return;
                }
                _Types.loadTypeDefinition(type, callback);
            });
        });
        
    },

    isSchemaLoaded : function() {
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

    loadAccessibleResources : function(callback) {
        var url = rootUrl + 'resource_access/ui';
        $.ajax({
            url: url,
            headers: headers,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                var types = [];
                _Types.types.length = 0;
                $.each(data.result, function(i, res) {
                    var type = getTypeFromResourceSignature(res.signature);
                    //console.log(res);
                    if (type !== '_schema' && !isIn(type, _Types.types)) {
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

    loadTypeDefinition : function(type, callback) {
        
        //_Types.schema[type] = [];
        //console.log(type);
        var url = rootUrl + '_schema/' + type.toUnderscore();
        $.ajax({
            url: url,
            headers: headers,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            //async: false,
            success: function(data) {
                
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
            
            
        });        
    },
    
    addTab : function(type) {
        var res = _Types.schema[type];
        $('#resourceTabsMenu').append('<li><a href="#' +  type + '"><span>' + _Crud.formatKey(type) + '</span></a></li>');
        $('#resourceTabs').append('<div class="resourceBox" id="' + type + '" data-url="' + res.url + '"></div>');
        var typeNode = $('#' + type);
        typeNode.append('<div>Type: ' + res.type + '</div>');
        typeNode.append('<div>URL: <a target="_blank" href="' + rootUrl + res.url.substring(1) + '">' + res.url + '</a></div>');
        typeNode.append('<table class="props"><thead><tr>\n\
<th>Property</th>\n\
<th>Key</th>\n\
<th>JSON name</th>\n\
<th>DB name</th>\n\
<th>Class</th>\n\
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
                    tb.append('<tr>\n\
<td class="' + key + '">' + _Crud.formatKey(key) + '</td>\n\
<td>' + key + '</td>\n\
<td>' + view[key].jsonName + '</td>\n\
<td>' + view[key].dbName + '</td>\n\
<td title="' + view[key].className + '">' + view[key].className.substring(view[key].className.lastIndexOf('.')+1) + '</td>\n\
<td>' + nvl(view[key].relatedType, '') + '</td>\n\
<td>' + nvl(view[key].inputConverter, '') + '</td>\n\
<td>' + (view[key].isCollection ? '<img src="icon/accept.png">' : '') + '</td>\n\
<td>' + (view[key].readOnly ? '<img src="icon/accept.png">' : '') + '</td>\n\
<td>' + (view[key].system ? '<img src="icon/accept.png">' : '') + '</td>\n\
<td>' + nvl(view[key].defaultValue, '') + '</td>\n\
<td>' + nvl(view[key].declaringClass, '') + '</td>\n\
</tr>');
                });
            }
        }
        
        _Crud.resize();
        
    },

    addTypeDialog: function() {

        Structr.dialog('Add custom type', function() {
            return true;
        }, function() {
            return true;
        });

        dialog.append('<div><label for="type">New custom type:</label><input id="_type" type="text" name="type"></div>');
        dialog.append('<div><label for="attr">First attribute:</label><input id="_attr" type="text" name="attr"></div>');
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
        dialog.append('<button id="createType">Create type</button>');
	
        $('input#_type').focus();

        $('#createType').on('click', function(e) {
            e.stopPropagation();

            var type     = $('#_type', dialog).val();
            var attr     = $('#_attr', dialog).val();
            var dataType = $('#_dataType', dialog).val();
            var entity = {};
            entity.type = 'PropertyDefinition';
            entity.kind = type;
            entity.name = attr;
            entity.dataType = dataType;

            _Types.addGrants(entity.kind);
            Command.create(entity, function() {
                window.location.reload(true);
            });
        });

    },
        
    addGrants: function(type) {
        Command.create({type:'ResourceAccess', flags: 255, signature:type, visibleToPublicUsers:true});
        Command.create({type:'ResourceAccess', flags: 255, signature:type + '/_Ui'});
        Command.create({type:'ResourceAccess', flags: 255, signature:type + '/_Public'});
        Command.create({type:'ResourceAccess', flags: 255, signature:type + '/_All'});
        Command.create({type:'ResourceAccess', flags: 255, signature:'_schema/' + type});
    }
        
//    refreshPropertyDefinitions : function() {
//        propertyDefinitions.empty();
//        if (Command.list('PropertyDefinition')) {
//            propertyDefinitions.append('<button class="add_type_icon button"><img title="Add Custom Type" alt="Add Custom Type" src="' + _Types.type_icon + '"> Add PropertyDefinition</button>');
//            $('.add_type_icon', main).on('click', function(e) {
//                e.stopPropagation();
//                var entity = {};
//                entity.type = 'PropertyDefinition';
//                return Command.create(entity);
//            });
//        }
//            
//        Structr.addPager(propertyDefinitions, 'PropertyDefinition');
//    },
    
//    appendPropertyDefinitionElement : function(propertyDefinition) {
//		
//        var p = propertyDefinition;
//        
//        if (p.kind && p.name) {
//            
//            console.log(p.kind, p.name);
//            dynamicTypes[p.kind] = dynamicTypes[p.kind] || {};
//            dynamicTypes[p.kind][p.name] = dynamicTypes[p.kind][p.name] || {};
//        
//            dynamicTypes[p.kind][p.name].dataType                       = p.dataType;
//            dynamicTypes[p.kind][p.name].relKind                        = p.relKind;
//            dynamicTypes[p.kind][p.name].relType                        = p.relType;
//            dynamicTypes[p.kind][p.name].incoming                       = p.incoming;
//            dynamicTypes[p.kind][p.name].systemProperty                 = p.systemProperty;
//            dynamicTypes[p.kind][p.name].readOnlyProperty               = p.readOnlyProperty;
//            dynamicTypes[p.kind][p.name].writeOnceProperty              = p.writeOnceProperty;
//            dynamicTypes[p.kind][p.name].indexedProperty                = p.indexedProperty;
//            dynamicTypes[p.kind][p.name].passivelyIndexedProperty       = p.passivelyIndexedProperty;
//            dynamicTypes[p.kind][p.name].searchableProperty             = p.searchableProperty;
//            dynamicTypes[p.kind][p.name].indexedWhenEmptyProperty       = p.indexedWhenEmptyProperty;
//            
//        }
//        
//        console.log('Collected custom types', dynamicTypes);
//        
//        propertyDefinitions.append('<div id="id_' + propertyDefinition.id + '" class="node propertyDefinition">'
//            + '<img class="typeIcon" src="'+ propertyDefinition.type_icon + '">'
//            + '<b title="' + propertyDefinition.name + '" class="name_">' + fitStringToSize(propertyDefinition.name, 200) + '</b> <span class="id">' + propertyDefinition.id + '</span>'
//            + '</div>');
//        
//        div = Structr.node(propertyDefinition.id);
//        
//        div.append('<img title="Delete Type ' + propertyDefinition.id + '" alt="Delete Type ' + propertyDefinition.id + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
//        $('.delete_icon', div).on('click', function(e) {
//            e.stopPropagation();
//            _Entities.deleteNode(this, propertyDefinition);
//        });
//        
//        _Entities.appendAccessControlIcon(div, propertyDefinition);
//        _Entities.appendEditPropertiesIcon(div, propertyDefinition);
//        _Entities.setMouseOver(div);
//		
//        _Types.updateDynamicTypes();
//                
//        return div;
//    },
//            
//    updateDynamicTypes : function() {
//        Object.keys(dynamicTypes).forEach(function(t) {
//            if (types.children('#_' + t).length) return;
//            types.append('<tr id="_' + t + '"><td>' + t + '</td></tr>');
//        });
//    }
};