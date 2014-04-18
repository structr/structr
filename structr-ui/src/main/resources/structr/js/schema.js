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
var canvas, instance, res, nodes = [], rels = [], localStorageSuffix = '_schema_' + port, undefinedRelType = 'UNDEFINED_RELATIONSHIP_TYPE', initialRelType = undefinedRelType;
var radius = 20, stub = 30, offset = 0;

$(document).ready(function() {
    Structr.registerModule('schema', _Schema);
    Structr.classes.push('schema');
});

var _Schema = {
    type_icon: 'icon/database_table.png',
    schemaLoading: false,
    schemaLoaded: false,
    reload: function() {
        _Schema.storePositions();
        main.empty();
        _Schema.init();
        _Schema.resize();
    },
    storePositions: function() {
        $.each($('#schema-graph .node'), function(i, n) {
            var node = $(n);
            localStorage.setItem(node.attr('id') + localStorageSuffix + 'node-position', JSON.stringify(node.position()));
        });
    },
    getPosition: function(id) {
        return JSON.parse(localStorage.getItem(id + localStorageSuffix + 'node-position'));
    },
    init: function() {

        _Schema.schemaLoading = false;
        _Schema.schemaLoaded = false;
        _Schema.schema = [];
        _Schema.keys = [];

        main.append('<div class="schema-input-container"><input class="schema-input" id="type-name" type="text" size="20" placeholder="New type"><button id="create-type" class="btn"><img src="icon/add.png"> Add Type</button></div>');

        if (true) {
            $('.schema-input-container').append('<input class="schema-input" id="ggist-url" type="text" size="30" placeholder="Enter a GraphGist raw URL"><button id="gg-import" class="btn">Start Import</button>');
            $('#gg-import').on('click', function(e) {
                var btn = $(this);
                var text = btn.text();
                btn.attr('disabled', 'disabled').addClass('disabled').html(text + ' <img src="img/al.gif">');
                e.preventDefault();
                _Schema.importGraphGist($('#ggist-url').val(), text);
            });

            $('.schema-input-container').append('<button class="btn" id="admin-tools"><img src="icon/wrench.png"> Admin Tools</button>');
            $('#admin-tools').on('click', function() {
                _Schema.openAdminTools();
            });
        }

        $('#type-name').focus().on('keyup', function(e) {

            if (e.keyCode === 13) {
                e.preventDefault();
                if ($('#type-name').val().length) {
                    $('#create-type').click();
                }
                return false;
            }

        });
        $('#create-type').on('click', function() {
            _Schema.createNode($('#type-name').val());
        });

        jsPlumb.ready(function() {
            main.append('<div class="canvas" id="schema-graph"></div>');

            canvas = $('#schema-graph');
            _Schema.resize();

            instance = jsPlumb.getInstance({
                //Connector: "StateMachine",
                PaintStyle: {
                    lineWidth: 5,
                    strokeStyle: "#81ce25"
                },
                Endpoint: ["Dot", {radius: 6}],
                EndpointStyle: {
                    fillStyle: "#aaa"
                },
                Container: "schema-graph",
                ConnectionOverlays: [
                    ["PlainArrow", {
                            location: 1,
                            width: 15,
                            length: 12
                        }
                    ]
                ]
            });


            _Schema.loadSchema(function() {
                instance.bind('connection', function(info) {
                    //console.log('Source ID:', getIdFromIdString(info.sourceId));
                    //console.log('Target ID:', getIdFromIdString(info.targetId));
                    _Schema.connect(getIdFromIdString(info.sourceId), getIdFromIdString(info.targetId));
                });
                instance.bind('connectionDetached', function(info) {
                    //console.log('Rel ID:', info.connection.getParameter('id'));
                    //console.log('Target ID:', getIdFromIdString(info.targetId));
                    Structr.confirmation('<h3>Delete schema relationship?</h3>',
                            function() {
                                $.unblockUI({
                                    fadeOut: 25
                                });
                                _Schema.detach(info.connection.getParameter('id'));
                                _Schema.reload();
                            });
                    _Schema.reload();


                });
            });
        });



        $(document).keyup(function(e) {
            if (e.keyCode === 27) {
                dialogCancelButton.click();
            }
        });

        $(window).on('resize', function() {
            _Schema.resize();
        });

    },
    onload: function() {
        _Schema.init();
        $('#main-help a').attr('href', 'http://docs.structr.org/frontend-user-guide#Schema');
    },
    /**
     * Read the schema from the _schema REST resource and call 'callback'
     * after the complete schema is loaded.
     */
    loadSchema: function(callback) {
        // Avoid duplicate loading of schema
        if (_Schema.schemaLoading) {
            return;
        }
        _Schema.schemaLoading = true;

        _Schema.loadNodes(function() {
            //console.log(nodes);
            _Schema.loadRels(callback);
        });

    },
    isSchemaLoaded: function() {
        var all = true;
        if (!_Schema.schemaLoaded) {
            //console.log('schema not loaded completely, checking all types ...');
            $.each(_Schema.types, function(t, type) {
                //console.log('checking type ' + type, (_Types.schema[type] && _Types.schema[type] != null));
                all &= (_Schema.schema[type] && _Schema.schema[type] !== null);
            });
        }
        _Schema.schemaLoaded = all;
        return _Schema.schemaLoaded;
    },
    loadNodes: function(callback) {
        var url = rootUrl + 'schema_nodes';
        $.ajax({
            url: url,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            success: function(data) {

                $.each(data.result, function(i, res) {
                    var id = 'id_' + res.id;
                    nodes[res.id] = res;
                    canvas.append('<div class="schema node" id="' + id + '"><b>' + res.name + '</b><img class="icon" src="icon/cross_small_grey.png">'
                            + '<table class="schema-props"><th>JSON Name</th><th>DB Name</th><th>Type</th><th>Format</th><th>Not null</th><th>Unique</th><th>Default</th></table>'
                            + '</div>');
                    var propertiesTable = $('#' + id + ' .schema-props');

                    var node = $('#' + id);
                    node.children('b').on('click', function() {
                        _Schema.makeNameEditable(node);
                    });

                    node.children('.icon').on('click', function() {
                        Structr.confirmation('<h3>Delete schema node?</h3><p>This will delete all incoming and outgoing schema relatinships as well, but no data will be removed.</p>',
                                function() {
                                    $.unblockUI({
                                        fadeOut: 25
                                    });
                                    _Schema.deleteNode(res.id);
                                });
                    });

                    var storedPosition = _Schema.getPosition(id);
                    node.offset({
                        left: storedPosition ? storedPosition.left : i * 180 + 25,
                        top: storedPosition ? storedPosition.top : i * 180 + 131
                    });

                    $.each(Object.keys(res), function(i, key) {
                        _Schema.appendLocalProperty(propertiesTable, id, res, key);
                    });

                    propertiesTable.append('<tr class="new"><td><input size="15" type="text" class="property-name" placeholder="Enter JSON name"></td>'
                            + '<td><input size="15" type="text" class="property-dbname" placeholder="Enter DB name"></td>'
                            + '<td>' + typeOptions + '</td>'
                            + '<td><input size="15" type="text" class="property-format" placeholder="Enter format"></td>'
                            + '<td><input class="not-null" type="checkbox"></td>'
                            + '<td><input class="unique" type="checkbox"></td>'
                            + '<td><input class="default" size="10" type="text"></td></div>');

                    $('#' + id + ' .new .property-name').on('blur', function() {
                        var name = $('#' + id + ' .new .property-name').val();
                        var dbName = $('#' + id + ' .new .property-dbname').val();
                        var type = $('#' + id + ' .new .property-type').val();
                        var format = $('#' + id + ' .new .property-format').val();
                        var notNull = $('#' + id + ' .new .not-null').is(':checked');
                        var unique = $('#' + id + ' .new .unique').is(':checked');
                        var defaultValue = $('#' + id + ' .new .default').val();

                        if (name && name.length && type) {
                            _Schema.putPropertyDefinition(res.id, ' {"'
                                    + '_' + name + '": "'
                                    + (dbName ? dbName + '|' : '')
                                    + (notNull ? '+' : '')
                                    + (type === 'del' ? null : type)
                                    + (unique ? '!' : '')
                                    + (format ? '(' + format + ')' : '')
                                    + (defaultValue ? ':' + defaultValue : '')
                                    + '"}');
                        }
                    });

                    $('#' + id + ' .new .property-type').on('change', function() {
                        var name = $('#' + id + ' .new .property-name').val();
                        var dbName = $('#' + id + ' .new .property-dbname').val();
                        var type = $('#' + id + ' .new .property-type').val();
                        var format = $('#' + id + ' .new .property-format').val();
                        var notNull = $('#' + id + ' .new .not-null').is(':checked');
                        var unique = $('#' + id + ' .new .unique').is(':checked');
                        var defaultValue = $('#' + id + ' .new .default').val();
//                        console.log('PUT ' + res.id + ' {"'
//                                + '_' + name + '": "'
//                                + (notNull ? '+' : '')
//                                + type
//                                + (unique ? '!' : '')
//                                + (format ? '(' + format + ')' : '')
//                                + '"}');
                        if (name && name.length && type && (type !== 'Enum' || (format && format.length))) {
                            _Schema.putPropertyDefinition(res.id, ' {"'
                                    + '_' + name + '": "'
                                    + (dbName ? dbName + '|' : '')
                                    + (notNull ? '+' : '')
                                    + type
                                    + (unique ? '!' : '')
                                    + (format && format.length ? '(' + format + ')' : '')
                                    + (defaultValue ? ':' + defaultValue : '')
                                    + '"}');
                        }
                    });

                    $('#' + id + ' .new .property-format').on('change', function() {
                        var name = $('#' + id + ' .new .property-name').val();
                        var dbName = $('#' + id + ' .new .property-dbname').val();
                        var type = $('#' + id + ' .new .property-type').val();
                        var notNull = $('#' + id + ' .new .not-null').is(':checked');
                        var unique = $('#' + id + ' .new .unique').is(':checked');
                        var format = $('#' + id + ' .new .property-format').val();
                        var defaultValue = $('#' + id + ' .new .default').val();
//                        console.log('PUT ' + res.id + ' {"'
//                                + '_' + name + '": "'
//                                + (notNull ? '+' : '')
//                                + type
//                                + (unique ? '!' : '')
//                                + (format ? '(' + format + ')' : '')
//                                + '"}');
                        if (name && name.length && type && (type !== 'Enum' || (format && format.length))) {
                            _Schema.putPropertyDefinition(res.id, ' {"'
                                    + '_' + name + '": "'
                                    + (dbName ? dbName + '|' : '')
                                    + (notNull ? '+' : '')
                                    + type
                                    + (unique ? '!' : '')
                                    + (defaultValue ? defaultValue : '')
                                    + (format && format.length ? '(' + format + ')' : '')
                                    + '"}');
                        }
                    });

                    nodes[res.id + '_top'] = instance.addEndpoint(id, {
                        //anchor: [ "Perimeter", { shape: "Square" } ],
                        anchor: "Top",
//                        anchors: [
//                            [0.5, 0, 0, -1, 0, 0, "top"],
//                            [1, 0.5, 1, 0, 0, 0, "right"],
//                            [0.5, 1, 0, 1, 0, 0, "bottom"],
//                            [0, 0.5, -1, 0, 0, 0, "left"]
//                        ],
                        maxConnections: -1,
                        //isSource: true,
                        isTarget: true,
                        deleteEndpointsOnDetach: false
                    });
                    nodes[res.id + '_bottom'] = instance.addEndpoint(id, {
                        //anchor: [ "Perimeter", { shape: "Square" } ],
                        anchor: "Bottom",
//                        anchors: [
//                            [0.5, 0, 0, -1, 0, 0, "top"],
//                            [1, 0.5, 1, 0, 0, 0, "right"],
//                            [0.5, 1, 0, 1, 0, 0, "bottom"],
//                            [0, 0.5, -1, 0, 0, 0, "left"]
//                        ],
                        maxConnections: -1,
                        isSource: true,
                        deleteEndpointsOnDetach: false

                                //isTarget: true
                    });

                    instance.draggable(id, {containment: '#schema-graph', stop: function() {
                            _Schema.storePositions();
                        }});

                    //node.on('dragStop')

                });

                if (callback) {
                    callback();
                }

            }
        });
    },
    loadRels: function(callback) {
        var url = rootUrl + 'schema_relationships';
        $.ajax({
            url: url,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            success: function(data) {

                var sId, tId;
                $.each(data.result, function(i, res) {

                    if (sId === res.sourceId && tId === res.targetId) {
                        radius += 10;
                        stub += 30;
                        offset += .05;
                    } else {
                        radius = 20;
                        stub = 30;
                        offset = 0;
                    }

                    sId = res.sourceId;
                    tId = res.targetId;

                    rels[res.id] = instance.connect({
                        source: nodes[sId + '_bottom'],
                        target: nodes[tId + '_top'],
                        deleteEndpointsOnDetach: false,
                        connector: ["Flowchart", {cornerRadius: radius, stub: stub, gap: 6}],
                        parameters: {'id': res.id},
                        overlays: [
                            ["Label", {
                                    cssClass: "label multiplicity",
                                    label: res.sourceMultiplicity ? res.sourceMultiplicity : '*',
                                    location: .2 + offset,
                                    id: "sourceMultiplicity",
                                    events: {
                                        "click": function(label, evt) {
                                            //console.log(rels[res.id].getOverlay('sourceMultiplicity').label);
                                            var overlay = rels[res.id].getOverlay('sourceMultiplicity');
                                            overlay.setLabel('<input id="source-mult-label" type="text" size="15" id="id_' + res.id + '_sourceMultiplicity" value="' + overlay.label + '">');
                                            $('#source-mult-label').focus().on('blur', function() {
                                                _Schema.setRelationshipProperty(res.id, 'sourceMultiplicity', $(this).val());
                                                overlay.setLabel($(this).val());
                                            });
                                            evt.preventDefault();
                                        }
                                    }
                                }
                            ],
                            ["Label", {
                                    cssClass: "label rel-type",
                                    label: (res.relationshipType === initialRelType ? '&nbsp;' : res.relationshipType),
                                    location: .5 + offset,
                                    id: "label",
                                    events: {
                                        "click": function(label, evt) {
                                            //console.log(rels[res.id].getOverlay('label').label);
                                            var overlay = rels[res.id].getOverlay('label');
                                            overlay.setLabel('<input id="relationship-label" type="text" size="15" id="id_' + res.id + '_relationshipType" value="' + overlay.label + '">');
                                            $('#relationship-label').focus().on('blur', function() {
                                                _Schema.setRelationshipProperty(res.id, 'relationshipType', $(this).val());
                                                overlay.setLabel($(this).val());
                                            });
                                            evt.preventDefault();
                                        }
                                    }
                                }
                            ],
                            ["Label", {
                                    cssClass: "label multiplicity",
                                    label: res.targetMultiplicity ? res.targetMultiplicity : '*',
                                    location: .8 - offset,
                                    id: "targetMultiplicity",
                                    events: {
                                        "click": function(label, evt) {
                                            //console.log(rels[res.id].getOverlay('targetMultiplicity').label);
                                            var overlay = rels[res.id].getOverlay('targetMultiplicity');
                                            overlay.setLabel('<input id="target-mult-label" type="text" size="15" id="id_' + res.id + '_targetMultiplicity" value="' + overlay.label + '">');
                                            $('#target-mult-label').focus().on('blur', function() {
                                                _Schema.setRelationshipProperty(res.id, 'targetMultiplicity', $(this).val());
                                                overlay.setLabel($(this).val());
                                            });
                                            evt.preventDefault();
                                        }
                                    }
                                }
                            ]

                        ]
                    });

                    // Add source property
                    var source = nodes[res.sourceId];

                    _Schema.getPropertyName(source.name, res.relationshipType, true, function(key) {
                        _Schema.appendRelatedProperty($('#id_' + source.id + ' .schema-props'), source.id, res, res.targetJsonName ? res.targetJsonName : key, true);
                        instance.repaintEverything();
                    });


                    // Add target property
                    var target = nodes[res.targetId];

                    _Schema.getPropertyName(target.name, res.relationshipType, false, function(key) {
                        _Schema.appendRelatedProperty($('#id_' + target.id + ' .schema-props'), target.id, res, res.sourceJsonName ? res.sourceJsonName : key, false);
                        instance.repaintEverything();
                    });

                });

                if (callback) {
                    callback();
                }

            }
        });
    },
    resize: function() {

        var w = $(window).width() - 52;
        var h = $(window).height() - 160;

        canvas.css({
            width: w + 'px',
            height: h + 'px',
        });

    },
    appendLocalProperty: function(el, id, res, key) {

        if (key.startsWith('_')) {

            var name = key.substring(1);
            var dbName = '';
            var type;
            if (res[key].indexOf('|') > -1) {
                dbName = res[key].substring(0, res[key].indexOf('|'));
                type = res[key].substring(res[key].indexOf('|') + 1);
            } else {
                type = res[key];
            }

            var notNull = (res[key].indexOf('+') > -1);
            var unique = (res[key].indexOf('!') > -1);

            type = type.replace('+', '').replace('!', '');

            var defaultValue = '';
            if (type.indexOf(':') > -1) {
                defaultValue = (type.substring(type.indexOf(':') + 1));
                type = type.substring(0, type.indexOf(':'));
            }

            var format;
            if (type.indexOf('(') > -1) {
                var parts = type.split('(');
                type = parts[0];
                format = parts[1].replace(')', '');
            }
            el.append('<tr class="' + key + '"><td><input size="15" type="text" class="property-name" value="' + name + '"></td><td>'
                    + '<input size="15" type="text" class="property-dbname" value="' + dbName + '"></td><td>'
                    + typeOptions + '</td><td><input size="15" type="text" class="property-format" value="'
                    + (format ? format : '') + '"></td><td><input class="not-null" type="checkbox"'
                    + (notNull ? ' checked="checked"' : '') + '></td><td><input class="unique" type="checkbox"'
                    + (unique ? ' checked="checked"' : '') + '</td><td>'
                    + '<input type="text" size="10" class="default" value="' + defaultValue + '">' + '</td></div>');

            $('#' + id + ' .' + key + ' .property-type option[value="' + type + '"]').attr('selected', true);

            $('#' + id + ' .' + key + ' .property-type').on('change', function() {
                _Schema.savePropertyDefinition(res.id, key);
            });

            $('#' + id + ' .' + key + ' .property-format').on('blur', function() {
                _Schema.savePropertyDefinition(res.id, key);
            });

            $('#' + id + ' .' + key + ' .not-null').on('change', function() {
                _Schema.savePropertyDefinition(res.id, key);
            });

            $('#' + id + ' .' + key + ' .unique').on('change', function() {
                _Schema.savePropertyDefinition(res.id, key);
            });

        }

    },
    appendRelatedProperty: function(el, id, rel, key, out) {
        var relType = rel.relationshipType;
        relType = relType === undefinedRelType ? '' : relType;

        el.append('<tr class="' + key + '"><td><input size="15" type="text" class="property-name related" value="' + key + '"></td><td></td><td>'
                + (out ? '-' : '&lt;-') + '[:' + relType + ']' + (out ? '-&gt;' : '-') + '</td><td><input size="15" type="text" class="property-format" value="'
                + '' + '"></td><td><input class="not-null" type="checkbox"'
                + '' + '></td><td><input class="unique" type="checkbox"'
                + '' + '</td><td></td></tr>');

        $('#id_' + id + ' .' + key + ' .property-name').on('blur', function() {

            var newName = $(this).val();

            if (newName === '')
                newName = undefined;

            if (id === rel.sourceId) {
                _Schema.setRelationshipProperty(rel.id, 'targetJsonName', newName);
            } else {
                _Schema.setRelationshipProperty(rel.id, 'sourceJsonName', newName);
            }
        });

    },
    savePropertyDefinition: function(entityId, key) {
        var id = 'id_' + entityId;
        var name = $('#' + id + ' .' + key + ' .property-name').val();
        var dbName = $('#' + id + ' .' + key + ' .property-dbname').val();
        var type = $('#' + id + ' .' + key + ' .property-type').val();
        var format = $('#' + id + ' .' + key + ' .property-format').val();
        var notNull = $('#' + id + ' .' + key + ' .not-null').is(':checked');
        var unique = $('#' + id + ' .' + key + ' .unique').is(':checked');
        var defaultValue = $('#' + id + ' .' + key + ' .default').val();
//        console.log('PUT ' + entityId + ' {"'
//                + '_' + name + '": "'
//                + (notNull ? '+' : '')
//                + (type === 'del' ? null : type)
//                + (unique ? '!' : '')
//                + (format ? '(' + format + ')' : '')
//                + '"}');
        if (name && name.length && type) {

            if (type === 'del') {
                _Schema.putPropertyDefinition(entityId, ' {"_' + name + '":null}');
            } else {
                _Schema.putPropertyDefinition(entityId, ' {"'
                        + '_' + name + '": "'
                        + (dbName ? dbName + '|' : '')
                        + (notNull ? '+' : '')
                        + (type === 'del' ? null : type)
                        + (unique ? '!' : '')
                        + (format ? '(' + format + ')' : '')
                        + (defaultValue ? ':' + defaultValue : '')
                        + '"}');
            }
        }
    },
    putPropertyDefinition: function(id, data) {
        //console.log('putPropertyDefinition', id, data);
        $.ajax({
            url: rootUrl + 'schema_nodes/' + id,
            type: 'PUT',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            data: data,
            statusCode: {
                200: function() {
                    _Schema.reload();
                },
                422: function(data) {
                    console.log(data);
                    Structr.errorFromResponse(data.responseJSON);
                }
            }
        });
    },
    createNode: function(type) {
        var url = rootUrl + 'schema_nodes';
        $.ajax({
            url: url,
            type: 'POST',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            data: '{ "name": "' + type + '"}',
            statusCode: {
                201: function() {
                    //console.log('node created');
                    _Schema.reload();
                },
                422: function(data) {
                    //console.log(data);
                    Structr.errorFromResponse(data.responseJSON);
                }
            }

        });
    },
    deleteNode: function(id) {
        var url = rootUrl + 'schema_nodes/' + id;
        $.ajax({
            url: url,
            type: 'DELETE',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            statusCode: {
                200: function() {
                    //console.log('node created');
                    _Schema.reload();
                },
                422: function(data) {
                    //console.log(data);
                    Structr.errorFromResponse(data.responseJSON);
                }
            }

        });
    },
    createRelationshipDefinition: function(sourceId, targetId, relationshipType) {
        var data = '{"sourceId":"' + sourceId + '","targetId":"' + targetId + '"'
                + (relationshipType && relationshipType.length ? ',"relationshipType":"' + relationshipType + '"' : '')
                + ', "sourceMultiplicity" : "*", "targetMultiplicity" : "*"'
                + '}';
        //console.log(data);
        $.ajax({
            url: rootUrl + 'schema_relationships',
            type: 'POST',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            data: data,
            statusCode: {
                201: function() {
                    //console.log('rel created');
                    _Schema.reload();
                },
                422: function(data) {
                    console.log(data);
                    Structr.errorFromResponse(data.responseJSON);
                }
            }
        });
    },
    removeRelationshipDefinition: function(id) {
        $.ajax({
            url: rootUrl + 'schema_relationships/' + id,
            type: 'DELETE',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            statusCode: {
                200: function(data, textStatus, jqXHR) {
                    //console.log('rel removed', data, textStatus, jqXHR);
                    _Schema.reload();
                },
                422: function(data) {
                    console.log(data);
                    Structr.errorFromResponse(data.responseJSON);
                }
            }
        });
    },
    setRelationshipProperty: function(entityId, key, value) {
        $.ajax({
            url: rootUrl + 'schema_relationships/' + entityId,
            type: 'PUT',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            data: '{"' + key + '":' + (value ? '"' + value + '"' : 'null') + '}',
            statusCode: {
                200: function(data, textStatus, jqXHR) {
                    //console.log('rel property set', data, textStatus, jqXHR);
                    _Schema.reload();
                },
                422: function(data) {
                    console.log(data);
                    Structr.errorFromResponse(data.responseJSON);
                }
            }
        });
    },
    connect: function(sourceId, targetId) {
        //Structr.dialog('Enter relationship details');
        _Schema.createRelationshipDefinition(sourceId, targetId, initialRelType);

    },
    detach: function(relationshipId) {
        //Structr.dialog('Enter relationship details');
        _Schema.removeRelationshipDefinition(relationshipId);
    },
    makeNameEditable: function(element) {
        //element.off('dblclick');

        var id = element.prop('id').substring(3);

        element.off('hover');
        element.children('b').hide();
        var oldName = $.trim(element.children('b').text());
        var input = $('input.new-name', element);

        if (!input.length) {
            element.prepend('<input type="text" size="' + (oldName.length + 8) + '" class="new-name" value="' + oldName + '">');
            input = $('input.new-name', element);
        }

        input.show().focus().select();

        input.on('blur', function() {
            _Schema.changeName(id, element, input, oldName);
            return false;
        });

        input.keypress(function(e) {
            if (e.keyCode === 13 || e.keyCode === 9) {
                e.preventDefault();
                _Schema.changeName(id, element, input, oldName);
                return false;
            }
        });
        element.off('click');
    },
    changeName: function(id, element, input, oldName) {
        var newName = input.val();
        input.hide();
        element.children('b').text(newName).show();
        if (oldName !== newName) {
            _Schema.putPropertyDefinition(id, JSON.stringify({name: newName}));
        }
    },
    importGraphGist: function(graphGistUrl, text) {
        $.ajax({
            url: rootUrl + 'maintenance/importGist',
            type: 'POST',
            data: JSON.stringify({'url': graphGistUrl}),
            contentType: 'application/json',
            statusCode: {
                200: function() {
                    var btn = $('#import-ggist');
                    btn.removeClass('disabled').attr('disabled', null);
                    btn.html(text + ' <img src="icon/tick.png">');
                    window.setTimeout(function() {
                        $('img', btn).fadeOut();
                        document.location.reload();
                    }, 1000);
                }
            }
        });
    },
    openAdminTools: function() {
        Structr.dialog('Admin Tools', function() {
        }, function() {
        });

        dialogText.append('<table id="admin-tools-table">');
        $('#admin-tools-table').append('<tr><td><button id="rebuild-index">Rebuild Index</button></td><td><label for"rebuild-index">Rebuild database index for all nodes and relationships</label></td></tr>');
        $('#admin-tools-table').append('<tr><td><button id="clear-schema">Clear Schema</button></td><td><label for"clear-schema">Delete all schema nodes and relationships of dynamic schema</label></td></tr>');
        $('#admin-tools-table').append('<tr><td><select id="node-type-selector"><option value="">-- Select Node Type --</option></select><!--select id="rel-type-selector"><option>-- Select Relationship Type --</option></select--><button id="add-uuids">Add UUIDs</button></td><td><label for"setUuid">Add UUIDs to all nodes of the selected type</label></td></tr>');
        $('#admin-tools-table').append('</table>');

        var nodeTypeSelector = $('#node-type-selector');

        $('#rebuild-index').on('click', function(e) {
            var btn = $(this);
            var text = btn.text();
            btn.attr('disabled', 'disabled').addClass('disabled').html(text + ' <img src="img/al.gif">');
            e.preventDefault();
            $.ajax({
                url: rootUrl + 'maintenance/rebuildIndex',
                type: 'POST',
                data: {},
                contentType: 'application/json',
                statusCode: {
                    200: function() {
                        var btn = $('#rebuild-index');
                        btn.removeClass('disabled').attr('disabled', null);
                        btn.html(text + ' <img src="icon/tick.png">');
                        window.setTimeout(function() {
                            $('img', btn).fadeOut();
                        }, 1000);
                    }
                }
            });
        });

        $('#clear-schema').on('click', function(e) {

            Structr.confirmation('<h3>Delete schema?</h3><p>This will remove all dynamic schema information, but not your other data.</p><p>&nbsp;</p>',
                    function() {
                        $.unblockUI({
                            fadeOut: 25
                        });

                        var btn = $(this);
                        var text = btn.text();
                        btn.attr('disabled', 'disabled').addClass('disabled').html(text + ' <img src="img/al.gif">');
                        e.preventDefault();
                        $.ajax({
                            url: rootUrl + 'schema_relationships',
                            type: 'DELETE',
                            data: {},
                            contentType: 'application/json',
                            statusCode: {
                                200: function() {
                                    _Schema.reload();
                                    $.ajax({
                                        url: rootUrl + 'schema_nodes',
                                        type: 'DELETE',
                                        data: {},
                                        contentType: 'application/json',
                                        statusCode: {
                                            200: function() {
                                                _Schema.reload();
                                                var btn = $('#clear-schema');
                                                btn.removeClass('disabled').attr('disabled', null);
                                                btn.html(text + ' <img src="icon/tick.png">');
                                                window.setTimeout(function() {
                                                    $('img', btn).fadeOut();
                                                }, 1000);
                                            }
                                        }
                                    });

                                }
                            }
                        });
                    });
        });

        Command.list('SchemaNode', true, 100, 1, 'name', 'asc', function(n) {
            $('#node-type-selector').append('<option>' + n.name + '</option>');
        });

        Command.list('SchemaRelationship', true, 100, 1, 'relationshipType', 'asc', function(r) {
            $('#rel-type-selector').append('<option>' + r.relationshipType + '</option>');
        });

        $('#add-uuids').on('click', function(e) {
            var btn = $(this);
            var text = btn.text();
            e.preventDefault();
            var type = nodeTypeSelector.val();
            var relType = $('#rel-type-selector').val();
            if (!type) {
                nodeTypeSelector.addClass('notify');
                nodeTypeSelector.on('change', function() {
                    nodeTypeSelector.removeClass('notify');
                });
                return;
            }
            btn.attr('disabled', 'disabled').addClass('disabled').html(text + ' <img src="img/al.gif">');
            $.ajax({
                url: rootUrl + 'maintenance/setUuid',
                type: 'POST',
                data: JSON.stringify({'type': type, 'relType': relType}),
                contentType: 'application/json',
                statusCode: {
                    200: function() {
                        var btn = $('#add-uuids');
                        nodeTypeSelector.removeClass('notify');
                        btn.removeClass('disabled').attr('disabled', null);
                        btn.html(text + ' <img src="icon/tick.png">');
                        window.setTimeout(function() {
                            $('img', btn).fadeOut();
                        }, 1000);
                    }
                }
            });
        });

    },
    getPropertyName: function(type, relationshipType, out, callback) {
        $.ajax({
            url: rootUrl + '_schema/' + type,
            type: 'GET',
            contentType: 'application/json',
            statusCode: {
                200: function(data) {
                    var properties = data.result[0].views.public;
                    Object.keys(properties).forEach(function(key) {
                        var obj = properties[key];
                        var simpleClassName = obj.className.split('.')[obj.className.split('.').length - 1];
                        if (obj.relatedType && obj.relationshipType) {
                            if (obj.relationshipType === relationshipType && ((simpleClassName.startsWith('EndNode') && out)
                                    || (simpleClassName.startsWith('StartNode') && !out))) {
                                callback(key, obj.isCollection);
                            }

                        }
                    });
                }
            }
        });

    }
};

var typeOptions = '<select class="property-type"><option value="">--Select type--</option>'
        + '<option value="String">String</option>'
        + '<option value="Integer">Integer</option>'
        + '<option value="Long">Long</option>'
        + '<option value="Double">Double</option>'
        + '<option value="Boolean">Boolean</option>'
        + '<option value="Enum">Enum</option>'
        + '<option value="Date">Date</option>'
        + '<option value="del">--DELETE--</option></select>';