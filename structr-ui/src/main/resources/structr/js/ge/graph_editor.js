/*
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
var maxDepth = 1;
var maxRels = 50;

var context;
var graph;
var canvas;
var settings, relBox;
var size = 'small';
var hiddenRelTypes = ["OWNS", "SECURITY", "PAGE"];
var headers = {};

function Canvas(parent) {
    this.parent = parent;
    this.element = $("<canvas>")[0];
    $(parent).append(this.element);
    this.setSize = function(w, h) {
        this.w = w;
        this.h = h;
        this.element.width = w;
        this.element.height = h;
    }
}

function reload(id) {
    var url = window.location.pathname + '?id=' + id;
    window.location = url;
}

function drawGraph(id) {
    graph = new Graph($('#graph'));
    canvas = new Canvas(graph.element);
    context = canvas.element.getContext("2d");
    graph.element.append('<div id="settings"><table>'
            + '<tr><td><label for="maxDepth">Max. depth</label></td><td><input id="maxDepth" name="maxDepth" size="3" value="' + maxDepth + '"></td></tr>'
            + '<tr><td><label for="maxRels">Max. rels</label></td><td><input id="maxRels" name="maxRels" size="3" value="' + maxRels + '"></td></tr>'
            + '<tr><td><label for="noOfNodes">Nodes</label></td><td><input id="noOfNodes" name="noOfNodes" size="3"></td></tr>'
            + '<tr><td><label for="noOfRels">Rels</label></td><td><input id="noOfRels" name="noOfRels" size="3"></td></tr></table>'
            + '<ul class="relBox"></ul></div>');
    settings = $('#settings', graph.element);
    relBox = $('.relBox', settings);
    $('input#maxDepth', settings).on('change', function() {
        var inp = $(this);
        maxDepth = inp.val();
        graph.redraw();
    })
    $('input#maxRels', settings).on('change', function() {
        var inp = $(this);
        maxRels = inp.val();
        graph.redraw();
    })
    resizeCanvas();
    $(window).resize(function() {
        resizeCanvas();
    });
//    headers = {
//        'X-StructrSessionToken': token
//    };
    if (id) {
        graph.renderNode(id, 0, [canvas.w / 2, canvas.h / 2]);
    }
}

function Graph(element) {
    this.element = element;
    this.nodes = {};
    this.relationships = {};
    this.nodeIds = [];
    this.relIds = [];
    this.relationshipTypes = [];
    this.hiddenRelationshipTypes = hiddenRelTypes;
    this.addRelType = function(type) {
        graph.relationshipTypes.push(type);
        relBox.append('<li><input type="checkbox" id="toggle_' + type + '" ' + (isIn(type, hiddenRelTypes) ? '' : 'checked="checked"') + '>' + type + '</li>');
        $('#toggle_' + type).change(function() {
            if (!isIn(type, graph.hiddenRelationshipTypes)) {
                graph.hiddenRelationshipTypes.push(type);
                $.each(graph.relationships, function(k, rel) {
                    if (simpleType(rel.type) === type || simpleType(rel.relType) === type) {
                        delete graph.relationships[rel.id];
                    }
                });
            } else {
                graph.hiddenRelationshipTypes = without(type, graph.hiddenRelationshipTypes);
                graph.redraw();
            }
            graph.redrawRelationships();
        });
    };

    this.redraw = function() {
        var tmpNodes = graph.nodes;
        graph.element.children('.node').remove();
        graph.nodeIds = [];
        graph.relIds = [];
        graph.nodes = {};
        graph.relationships = {};
        $.each(tmpNodes, function(i, node) {
            graph.renderNode(node.id, node.depth, node.pos);
        });
        //graph.redrawRelationships();
    };

    this.renderNode = function(nodeId, depth, pos, callback) {
        if (!nodeId || depth > maxDepth || graph.hasNode(nodeId)) {
            return;
        }
        graph.nodeIds.push(nodeId);
        $.ajax({
            //headers: headers,
            url: rootUrl + nodeId + '/ui',
            dataType: "json",
            success: function(data) {
                if (!data || data.length === 0 || !data.result) {
                    return;
                }
                var entity = data.result;
                var node = new Node(graph, entity, size, pos, depth);
                graph.addNode(node);
                $('.label', node.element).html(entity.name ? entity.name : (entity.tag ? entity.tag : entity.type));
                graph.renderRelationships(nodeId, 'out', depth, pos);
                graph.renderRelationships(nodeId, 'in', depth, pos);
                if (callback) {
                    callback();
                }
            }                                                                                                                                                       
        });
    };

    this.addRelationship = function(r) {
        graph.relationships[r.id] = r;
        graph.redrawRelationships();
    };

    this.addNode = function(n) {
        graph.nodes[n.id] = n;
    };

    this.hasRelationship = function(id) {
        return isIn(id, graph.relIds) || graph.relationships.hasOwnProperty(id);
    };

    this.hasNode = function(id) {
        return isIn(id, graph.nodeIds) || graph.nodes.hasOwnProperty(id);
    };

    this.expand = function(node) {
        var el = Structr.node(node.id);
        var pos = [el.position().left, el.position().top];
        el.remove();
        var i = graph.nodeIds.indexOf(node.id);
        graph.nodeIds.splice(i,1);
        delete graph.nodes[node.id];
        $.each(graph.relationships, function(i, rel) {
            if (node.id === rel.sourceId || node.id === rel.targetId) {
                delete graph.relationships[rel.id];
            }
        });
        graph.renderNode(node.id, 0, pos);
    };


    this.redrawRelationships = function() {
        context.clearRect(0, 0, canvas.w, canvas.h);
        //console.log('redraw all relationships', graph.relationships);
        $.each(graph.relationships, function(i, rel) {
            //console.log('redraw rel', rel);
            if (rel) {
                rel.redraw();
            }
        });
        $('#noOfRels').val(Object.keys(graph.relationships).length);
        $('#noOfNodes').val(Object.keys(graph.nodes).length);
    };

    this.renderRelationships = function(nodeId, direction, depth, pos) {
        $.ajax({
            //headers: headers,
            url: rootUrl + nodeId + '/' + direction,
            dataType: "json",
            success: function(data) {
                if (!data || data.length === 0 || !data.result || !data.result.length) {
                    return;
                }
                var results = data.result;
                var count = 0, i = 0;
                while (i < results.length && count < maxRels) {
                    var r = results[i];
                    i++;
                    var type = simpleType(r.relType || r.type);
                    if (!isIn(type, graph.relationshipTypes)) {
                        graph.addRelType(type);
                    }
                    if (!type || graph.hasRelationship(r.id) || isIn(type, graph.hiddenRelationshipTypes)) {
                        //console.log('not rendering rel of type', type, graph.hasRelationship(r.id), isIn(type, graph.hiddenRelationshipTypes))
                        continue;
                    }
                    
                    if (direction === 'out') {
                        if (!(graph.hasNode(r.targetId))) {
                            graph.renderNode(r.targetId, depth + 1, nextPos(pos, canvas.h / (depth + 3)), function() {
                                graph.addRelationship(new Relationship(graph, r.id, type, r.sourceId, r.targetId));
                            });
                        } else {
                            graph.addRelationship(new Relationship(graph, r.id, type, r.sourceId, r.targetId));
                        }
                    } else if (direction === 'in') {
                       if (!(graph.hasNode(r.sourceId))) {
                            graph.renderNode(r.sourceId, depth + 1, nextPos(pos, canvas.h / (depth + 3)), function() {
                                graph.addRelationship(new Relationship(graph, r.id, type, r.sourceId, r.targetId));
                            });
                        } else {
                            graph.addRelationship(new Relationship(graph, r.id, type, r.sourceId, r.targetId));
                        }
                    }
                    count++;
                }
            }
        });
    };
}

function resizeCanvas() {
    var w = $(window).width() - 48;
    var h = $(window).height() - $('#header').height() - 56;
    var g = $('#graph');
    g.width(w);
    g.height(h);
}

function simpleType(combinedType) {
    if (!combinedType) return combinedType;
    if (!combinedType.contains(' ')) {
        return combinedType.toUpperCase();
    }
    return combinedType.split(' ')[1].toUpperCase();
}
