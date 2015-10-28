/*
 *  Copyright (C) 2010-2015 Structr GmbH
 *
 *  This file is part of Structr <http://structr.org>.
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

var animating = false;
var timeout   = 0;
var expanded  = {};
var count     = 0;

$(function() {

  doLayout(20);

  s.bind('clickNode', nodeClicked);
  s.bind('overNode', nodeHovered);
  s.bind('outNode', function(node) {
    if (!timeout) {
      timeout = window.setTimeout(function() {
        if (timeout) {
          timeout = 0;
          $('#graph-info').empty();
        }
      }, 1000);
    }
    s.refresh();
  });
});

function nodeHovered(node) {
  var domainOnly = false;
  if (window.domainOnly && window.domainOnly === true) {
	  domainOnly = true;
  }
  if (animating) {
    return;
  }
  window.clearTimeout(timeout);
  timeout = 0;
  $('#graph-info').empty();
  if (expanded[node.data.node.id] && expanded[node.data.node.id].state === 'expanded') {
    return;
  }
  var hoverMap = {};
  $('#graph-info').append('<span>Loading...</span>');
  $.ajax({
    url: '/structr/rest/' + node.data.node.id + '/out/_structr_graph' + (domainOnly ? '?domainOnly=true' : ''),
    contentType: 'application/json',
    method: 'GET',
    statusCode: {
      200: function(data) {
        $.each(data.result, function(i, result) {
          if (!s.graph.nodes(result.targetNode.id)) {
            if (hoverMap[result.targetNode.type]) {
              hoverMap[result.targetNode.type]++;
            } else {
              hoverMap[result.targetNode.type] = 1;
            }
          }
        });
        $.ajax({
          url: '/structr/rest/' + node.data.node.id + '/in/_structr_graph' + (domainOnly ? '?domainOnly=true' : ''),
          contentType: 'application/json',
          method: 'GET',
          statusCode: {
            200: function(data) {
              $.each(data.result, function(i, result) {
                if (!s.graph.nodes(result.id)) {
                  if (hoverMap[result.sourceNode.type]) {
                    hoverMap[result.sourceNode.type]++;
                  } else {
                    hoverMap[result.sourceNode.type] = 1;
                  }
                }
              });
              updateHoverInfo(node, hoverMap);
            }
          }
        });
      }
    }
  });
}

function updateHoverInfo(node, hoverMap) {
  var graphInfo  = $('#graph-info');
  var num        = Object.keys(hoverMap).length;
  var i          = 0;
  var size       = node.data.node['renderer1:size'];
  var radius     = Math.max(size, 40);
  var x          = Math.floor(node.data.node['renderer1:x']);
  var y          = node.data.node['renderer1:y'];
  var startAngle = 0;
  switch (num) {
    case 2: startAngle = (Math.PI / 8) * 7; break;
    case 3: startAngle = (Math.PI / 4) * 3; break;
    case 4: startAngle = (Math.PI / 8) * 5; break;
    case 5: startAngle = (Math.PI / 8) * 4; break;
    case 6: startAngle = (Math.PI / 8) * 3; break;
    case 7: startAngle = (Math.PI / 8) * 7; break;
    default: startAngle = Math.PI; break;
  }
  if (num < 8) {
    num = 8;
  }
  graphInfo.empty();
  $.each(hoverMap, function(key, value) {
    var label = key + ' (' + value + ')';
    var angle = startAngle + (Math.PI * 2 / num) * i;
    var cos   = Math.cos(angle);
    var sin   = Math.sin(angle);
  	var dx    = (cos * radius) - 15;
  	var dy    = (sin * radius) - 15;
    graphInfo.append('<button class="circle" id="' + key + '-button" style="position: absolute; top: 0px; left: 0px; z-index: 10000;" title="' + label + '">' + value + '</button>');
    graphInfo.append('<button class="btn btn-xs" style="margin: 4px; color: #000; background-color: ' + color[key] + '">' + label + '</button>');
    var button = $('#' + key + '-button');
    button.css('border', 'none');
    button.css('background-color', color[key]);
    button.css('width', '30');
    button.css('height', '30');
    button.css('color', '#000');
    button.css('top', (y + dy + 28) + 'px');	// 28 is the height of the #graph-info div
    button.css('left', (x + dx) + 'px');
    button.on('click', function(e) {
	  node.data.node.size += 5;
      graphInfo.empty();
      expandNode(node, key);
      s.refresh();
    });
    i++;
  });
  s.refresh();
}

function nodeClicked(node) {
  var id = node.data.node.id;
  $('#graph-info').empty();
  if (expanded[id] && expanded[id].state === 'expanded') {
	node.data.node.size -= 5;
    collapseNode(node);
    s.refresh();
  } else {
    node.data.node.size += 5;
    expandNode(node);
    s.refresh();
  }
}

function expandNode(node, type) {
  var domainOnly = false;
  if (window.domainOnly && window.domainOnly === true) {
	  domainOnly = true;
  }
  var id = node.data.node.id;
  var x  = node.data.node.x;
  var y  = node.data.node.y;
  expanded[id] = {
    state: 'expanded',
    nodes: expanded[id] ? expanded[id].nodes : [],
    edges: expanded[id] ? expanded[id].edges : []
  };
  $.ajax({
    url: '/structr/rest/' + node.data.node.id + '/out/_structr_graph' + (domainOnly ? '?domainOnly=true' : ''),
    contentType: 'application/json',
    method: 'GET',
    statusCode: {
      200: function(data) {
        var nodes = {};
        var edges = {};
        $.each(data.result, function(i, result) {
          if (!type || result.targetNode.type === type) {
            addOutNode(nodes, edges, result, x, y);
          }
        });
        update(id, nodes, edges);
      }
    }
  });
  $.ajax({
    url: '/structr/rest/' + node.data.node.id + '/in/_structr_graph' + (domainOnly ? '?domainOnly=true' : ''),
    contentType: 'application/json',
    method: 'GET',
    statusCode: {
      200: function(data) {
        var nodes = {};
        var edges = {};
        $.each(data.result, function(i, result) {
          if (!type || result.sourceNode.type === type) {
            addInNode(nodes, edges, result, x, y);
          }
        });
        update(id, nodes, edges);
      }
    }
  });
}

function collapseNode(node) {
  if (node) {
    var id = node.data ? node.data.node.id : node.id;
    if (expanded[id] && expanded[id].state === 'expanded') {
      var edges = expanded[id].edges;
      var nodes = expanded[id].nodes;
      // remove all edges from previous opening
      $.each(edges, function(i, e) {
        try { s.graph.dropEdge(e); } catch(x) {}
      });
      // remove all nodes from previous opening
      $.each(nodes, function(i, n) {
        collapseNode(s.graph.nodes(n));
        try { s.graph.dropNode(n); } catch(x) {}
      });
      expanded[id].state = 'collapsed';
      s.refresh();
    }
  }
}

function addOutNode(nodes, edges, result, x, y) {
  var sourceNode = result.sourceNode;
  var targetNode = result.targetNode;
  var newX = x + (Math.random() * 100) - 50;
  var newY = y + (Math.random() * 100) - 50;
  var size = 10;
  nodes[targetNode.id] = {id: targetNode.id, label: targetNode.name, size: size, x: newX, y: newY, color: color[targetNode.type]};
  edges[result.id]     = {id: result.id, source: sourceNode.id, target: targetNode.id, label: result.relType, type: 'curvedArrow', color: '#999'};
}

function addInNode(nodes, edges, result, x, y) {
  var sourceNode = result.sourceNode;
  var targetNode = result.targetNode;
  var newX = x + (Math.random() * 100) - 50;
  var newY = y + (Math.random() * 100) - 50;
  var size = 10;
  nodes[sourceNode.id] = {id: sourceNode.id, label: sourceNode.name, size: size, x: newX, y: newY, color: color[sourceNode.type]};
  edges[result.id]     = {id: result.id, source: sourceNode.id, target: targetNode.id, label: result.relType, type: 'curvedArrow', color: '#999'};
}

function update(current, nodes, edges) {
  var added = 0;
  $.each(nodes, function(i, node) {
    try {
      s.graph.addNode(node);
      // only add expanded node if addNode() is successful => node did not exist before
      expanded[current].nodes.push(node.id);
      added++;
    } catch (e) {}
  });
  $.each(edges, function(i, edge) {
    try {
      s.graph.addEdge(edge);
      // only add expanded edge if addEdge() is successful => edge did not exist before
      expanded[current].edges.push(edge.id);
      added++;
    } catch (e) {}
  });
  if (added) {
    s.refresh();
    doLayout(added);
  }
}

function doLayout(num) {
  running = true;
  restartLayout(num);
}

function restartLayout(num) {
  window.setTimeout(function() {
    animating = true;
    sigma.layouts.fruchtermanReingold.start(s, {
      autoArea: false,
      area: 1000000000,
      gravity: 0,
      speed: 0.1,
      iterations: 10
    });
    animating = false;
    if (count++ < num) {
      restartLayout(num);
    } else {
      count = 0;
    }
  }, 20);
}
