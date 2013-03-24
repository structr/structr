/**************** config parameter **********************************/
//var rootUrl = '/structr/api/';
var defaultStartNodeId = 670;
var maxDepth = 3;
var fps = 20; // relationship display update frequency in Hz
/********************************************************************/

var startNodeId = getURLParameter('id');
if (!startNodeId)
    startNodeId = defaultStartNodeId;

var context;
var graph;
var canvas;
var relBox;

//$(document).ready(function() {
//  drawGraph(startNodeId);
//});

function Canvas(parent) {
    this.parent = parent;
    this.element = $("<canvas>")[0];
    $(parent).append(this.element);
    $(parent).append('<ul class="relBox"></ul>');

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
    relBox = $('.relBox', canvas.element);

    resizeCanvas();

    $(window).resize(function() {
        resizeCanvas();
    });

    graph.render(id, 0, [canvas.w / 2, canvas.h / 2]);

//    setInterval(function() {
//        graph.redrawRelationships();
//    }, 1000 / fps);

}


function Graph(element) {
    
    this.element = element;

    this.nodes = [];
    this.relationships = [];

    this.nodeIds = [];
    this.relationshipIds = [];
    
    this.relationshipTypes = [];

    var self = this;

    this.updateRelBox = function() {
        
        $.each(this.relationshipTypes, function(i, type) {
            relBox.append('<li>' + type + '</li>');
        });
        
    };

    this.render = function(nodeId, depth, pos) {

        if (depth > maxDepth)
            return;

        if (graph.hasNode(nodeId))
            return;

        graph.addNodeId(nodeId);

        var headers = {
            'X-StructrSessionToken': token
        };

        $.ajax({
            headers: headers,
            url: rootUrl + nodeId + "/all",
            dataType: "json",
            success: function(data) {
                if (!data || data.length == 0 || !data.result)
                    return;

                var size = "medium";
                if (depth > 0) {
                    size = "small";
                }
                var node = new Node(graph, nodeId, size, pos);

                console.log('Node added to graph', graph, nodeId, size, pos);
                
                var entity = data.result;
//                var props = Object.keys(data.result);

//                var name = 'unknown';
//                var html = propertyTableHeader();
//                $.each(props, function(j, key) {
//                    var value = data.result[key];
//                    if (key === "name") {
//                        name = value;
//                    }
//                    html += propertyTableRow(key, value);
//                });
//                html += propertyTableFooter();

//                $('.body', node.element).html(html);
                //$('.label', node.element).html(name + ' [' + nodeId + ']');
                $('.label', node.element).html(entity.name ? entity.name : (entity.tag ? entity.tag : entity.type));
                $('.body table.props tr td.value input', node.element).each(function(i, v) {

                    $(v).focus(function() {
                        $(v).addClass('active');
                        //console.log('onFocus: value: ' + $(v).attr('value'));
                    });

                    $(v).focusout(function() {
                        $(v).removeClass('active');
                        //console.log('onFocusout: value: ' + $(v).attr('value'));
                    });

                    $(v).change(function() {

                        //console.log('onChange: value: ' + $(v).attr('value'));
                        //console.log($('.head .save', node.element));

                        //$('.head .save', node.element).removeClass('hidden');

                        //$('.head .save', node.element).click(function() {

                        var id = node.id;
                        var key = $(v).attr('name');
                        var type = $(v).parent().next().text();
                        var value;

                        switch (type) {

                            case 'Boolean' :
                                value = ($(':checked', $(v).parent()).val() == 'on' ? 'true' : 'false');
                                break;

                            default :
                                value = $(v).attr('value');

                        }

                        console.log('Save clicked. Node id=' + id + ', key=' + key + ', value=' + value);
                        $(this).addClass('hidden');

                        var data = '[ { "key" : "' + key + '", "value" : "' + value + '", "type" : "' + type + '" } ]';

                        console.log('PUT url: ' + rootUrl + nodeId);
                        console.log(data);

                        $.ajax({
                            type: 'PUT',
                            url: rootUrl + nodeId,
                            data: data,
                            dataType: 'json',
                            //contentType: "application/json",
                            success: function() {
                                var tick = $('<img class="tick" src="tick.png">');
                                tick.insertAfter($(v));
                                console.log($('.tick', $(v).parent()));
                                $('.tick', $(v).parent()).fadeOut('slow', function() {
                                    console.log('fade out complete');
                                });
                                //$('.tick', $(v).parent()).fadeOut();
                                //$('.tick', $(v).parent()).remove();
                            }
                        });

                        //});

                    });

                });


                // outgoing relationships
                $.ajax({
                    headers: headers,
                    url: rootUrl + nodeId + "/out",
                    dataType: "json",
                    success: function(data) {
                        if (!data || data.length == 0 || !data.result)
                            return;
                        var results = data.result;

                        for (var i = 0; i < Math.min(10, results.length); i++) {

                            var r = results[i];

                            var type = r.combinedType;
                            
                            if (!isIn(type, graph.relationshipTypes)) {
                                graph.relationshipTypes.push(type);
                                graph.updateRelBox();
                            }

                            if (graph.hasRelationship(r.id) || type.contains('OWNS') || type.contains('SECURITY'))
                                continue;

                            if (!(graph.hasNode(r.endNodeId))) {
                                graph.render(r.endNodeId, depth + 1, nextPos(pos, canvas.h / (depth + 3)));
                            }

                            var rel = new Relationship(graph, r.id, r.combinedType, node.id, r.endNodeId);
                            graph.addRelationship(rel);

                        }
                    }
                });


                // incoming relationships
                $.ajax({
                    headers: headers,
                    url: rootUrl + nodeId + "/in",
                    dataType: "json",
                    success: function(data) {
                        if (!data || data.length == 0 || !data.result)
                            return;
                        var results = data.result;
                        for (var i = 0; i < Math.min(10, results.length); i++) {

                            var r = results[i];
                            
                            var type = r.combinedType;

                            if (!isIn(type, graph.relationshipTypes)) {
                                graph.relationshipTypes.push(type);
                                graph.updateRelBox();
                            }

                            if (graph.hasRelationship(r.id) || type.contains('OWNS') || type.contains('SECURITY'))
                                continue;

                            if (!(graph.hasNode(r.startNodeId))) {
                                graph.render(r.startNodeId, depth + 1, nextPos(pos, canvas.h / (depth + 3)));
                            }

                            var rel = new Relationship(graph, r.id, r.combinedType, r.startNodeId, node.id);
                            graph.addRelationship(rel);

                        }

                    }
                });

            }

        });

    };

    this.addRelationship = function(r) {
        this.relationships[r.id] = r;
        this.relationshipIds.push(r.id);
        graph.redrawRelationships();
    };

    this.addNodeId = function(id) {
        this.nodeIds.push(id);
    };

    this.hasRelationship = function(id) {
        return (this.relationshipIds.indexOf(id) > -1);
    };

    this.hasNode = function(id) {
        return (this.nodeIds.indexOf(id) > -1);
    };

    this.redrawRelationships = function() {
        context.clearRect(0, 0, canvas.w, canvas.h);
        for (var i = 0; i < self.relationshipIds.length; i++) {
            self.relationships[self.relationshipIds[i]].redraw();
        }
        $('#noOfRels').val(self.relationshipIds.length);
        $('#noOfNodes').val(self.nodeIds.length);
    }

}

function resizeCanvas() {

    var w = $(window).width() - 48;
    var h = $(window).height() - $('#header').height() - 72;
    
    canvas.parent.width(w);
    canvas.parent.height(h);

    canvas.setSize(w+24, h+24);

    graph.redrawRelationships();
}