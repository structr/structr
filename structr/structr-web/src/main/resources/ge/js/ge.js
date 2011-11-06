/**************** config parameter **********************************/
var rootUrl = '/structr/api/';
var defaultStartNodeId = 0;
var maxDepth = 1;
var fps = 10; // relationship display update frequency in Hz
/********************************************************************/

var startNodeId = getURLParameter('id');
if (!startNodeId) startNodeId = defaultStartNodeId;

var context;
var graph;
var canvas;

$(document).ready(function() {
  drawGraph(startNodeId);
});

function Canvas(parent) {
  this.parent = parent;
  this.element = $("<canvas>")[0];
  $(parent).append(this.element);
  
  this.setSize = function(w,h) {
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
  
  graph = new Graph($("#editor_box"));
  canvas = new Canvas(graph.parent);
  context = canvas.element.getContext("2d");

  resizeCanvas();

  $(window).resize(function(){
    resizeCanvas();
  });

  graph.render(id, 0, [canvas.w/2, canvas.h/2]);
  
  setInterval(function() {
    graph.redrawRelationships();
  }, 1000/fps);

}


function Graph(element) {
  this.parent = element;
  
  this.nodes = [];
  this.relationships = [];
  
  this.nodeIds = [];
  this.relationshipIds = [];

  var self = this;
  
  this.render = function(nodeId, depth, pos) {
    
    if (depth > maxDepth) return;
    
    if (graph.hasNode(nodeId)) return;
    
    graph.addNodeId(nodeId);
    
    $.ajax({
      url: rootUrl + nodeId + "/all",
      dataType: "json",
      success: function(data) {
        if (!data || data.length == 0 || !data.result) return;
  
        var size = "tiny";
        if (depth == 0) {
          size = "large";
        }
        if (depth == 1) {
          size = "medium";
        }          
        var node = new Node(graph, nodeId, size, pos);
        
        var props = data.result.properties;
          
        var name = 'unknown';
        var html = propertyTableHeader();
        
        for (var j=0; j<props.length; j++) {
          var prop = props[j];
          if (prop.key == "name") name = prop.value;
          html += propertyTableRow(prop.key, prop.value, prop.type) ;
        }
        html += propertyTableFooter();
      
        $('.body', node.element).html(html);
        $('.label', node.element).html(name + ' [' + nodeId + ']');
        $('.body table.props tr td.value input', node.element).each(function(i,v) {
          
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
                  var tick = $('<img class="tick" src="img/tick.png">');
                  tick.insertAfter($(v));
                  console.log($('.tick', $(v).parent()));
                  $('.tick', $(v).parent()).fadeOut('slow', function() { console.log('fade out complete');});
                  //$('.tick', $(v).parent()).fadeOut();
                  //$('.tick', $(v).parent()).remove();
                }
              });
              
            //});
            
          });

        });
    
        
        // outgoing relationships
        $.ajax({
          url: rootUrl + nodeId + "/out",
          dataType: "json",
          success: function(data) {
            if (!data || data.length == 0 || !data.result) return;
            var results = data.result;
            for (var i=0; i<results.length; i++) {
              
              var r = results[i];

              if (graph.hasRelationship(r.id)) continue;

              if (!(graph.hasNode(r.endNodeId))) {
                graph.render(r.endNodeId, depth+1, nextPos(pos, canvas.h/(depth+3)));
              }
            
              var rel = new Relationship(graph, r.id, r.type, node.id, r.endNodeId);
              graph.addRelationship(rel);
              
            }
          }
        });

        
        // incoming relationships
        $.ajax({
          url: rootUrl + nodeId + "/in",
          dataType: "json",
          success: function(data) {
                        if (!data || data.length == 0 || !data.result) return;
            var results = data.result;
            for (var i=0; i<results.length; i++) {
              
              var r = results[i];

              if (graph.hasRelationship(r.id)) continue;

              if (!(graph.hasNode(r.startNodeId))) {
                graph.render(r.startNodeId, depth+1, nextPos(pos, canvas.h/(depth+3)));
              }
              
              var rel = new Relationship(graph, r.id, r.type, r.startNodeId, node.id);
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
    for (var i=0; i<self.relationshipIds.length; i++) {
      self.relationships[self.relationshipIds[i]].redraw();
    }
    $('#noOfRels').val(self.relationshipIds.length);
    $('#noOfNodes').val(self.nodeIds.length);
  }
  
}

function resizeCanvas() {

  var w = $(window).width() - 48;
  var h = $(window).height() - $('#header').height() - 84;
  
  $('#editor_box').width(w);
  $('#editor_box').height(h);

  canvas.setSize(w, h);

  graph.redrawRelationships();
}
