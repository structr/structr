var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

var animating = animating || undefined;

(function() {
	'use strict';

	var _s, _callbacks, _infoContainer, _marginX, _marginY, _onAddNodes, _onNodesAdded, _onNodesRemoved, _currentEvent, _timeout, _nodesShape, _newNodeSize, _edgeType, _newEdgesSize;

	var _expanded = {};
	var _hoverMap = {};

	var _active = false;
	var _inNodesLoaded = false;
	var _outNodesLoaded = false;
	var _showHoverInfo = true;

	var _newNodes = [];
	var _newEdges = [];
	var _undoStack = [];

	Graphbrowser.Modules.NodeExpander = function(sigmaInstance, callbacks){
		var self = this;
		self.name = 'nodeExpander';
		_s = sigmaInstance;
		_callbacks = callbacks;
	};

	Graphbrowser.Modules.NodeExpander.prototype.types = ["undoListener", "dataChangedHandler", "saveListener", "restoreListener", "resetListener", "nodeExpander"];

	Graphbrowser.Modules.NodeExpander.prototype.init = function(settings){
		var self = this;

		if(settings){
			settings.container !== undefined ? _infoContainer = settings.container : _infoContainer = '';
			settings.margins.left !== undefined ? _marginX = settings.margins.left : _marginX = 0;
			settings.margins.top !== undefined ? _marginY = settings.margins.top : _marginY = 0;
			settings.onAddNodes !== undefined ? _onAddNodes = settings.onAddNodes : _onAddNodes = undefined;
			settings.onNodesAdded !== undefined ? _onNodesAdded = settings.onNodesAdded : _onNodesAdded = undefined;
			settings.onNodesRemoved !== undefined ? _onNodesRemoved = settings.onNodesRemoved : _onNodesRemoved = undefined;
			settings.newNodesSize !== undefined ? _newNodeSize = settings.newNodesSize : _newNodeSize = 10;
			settings.newEdgesSize !== undefined ? _newEdgesSize = settings.newEdgesSize : _newEdgesSize = 10;
			settings.edgeType !== undefined ? _edgeType = settings.edgeType : _edgeType = "arrow";

			if(settings.nodesShape !== undefined){
				_nodesShape = self.nodesShape;
				CustomShapes.init(_s);
			}
			else{
				_nodesShape = "";
			}
		}
		else
			throw new Error("Graph-Browser-NodeExpander: No settings found!");
		
		if(_infoContainer.charAt(0) !== '#')
			_infoContainer = '#' + _infoContainer;

		_callbacks.sigmaCtrl.bindSigmaEvent('clickNode', self.handleClickNodeEvent.bind(self));
		_callbacks.sigmaCtrl.bindSigmaEvent('doubleClickNode', self.handleDoubleClickNodeEvent.bind(self));
		_callbacks.sigmaCtrl.bindSigmaEvent('hovers', self.handleHoversEvent.bind(self));

		_callbacks.api.expandNode = self.expandNode.bind(self);
		_callbacks.api.collapseNode = self.collapseNode.bind(self);
		
	};

	Graphbrowser.Modules.NodeExpander.prototype.handleClickNodeEvent = function(clickedNode){
		var self = this;
		var node = clickedNode.data.node;
	};

	Graphbrowser.Modules.NodeExpander.prototype.handleDoubleClickNodeEvent = function(clickedNode){
		var self = this;

		var node = clickedNode.data.node;
		var id = node.id;
		$('#graph-info').empty();
		if (_expanded[id] && _expanded[id].state === 'expanded') {
			self.collapseNode(node);
			_callbacks.dataChanged(self.name);

			if(_onNodesRemoved !== undefined && typeof _inNodesRemoved === 'function')
				_inNodesRemoved();
		} else {
			self.onExpandNode(node);
		}
	};

	Graphbrowser.Modules.NodeExpander.prototype.handleHoversEvent = function(e){
		var self = this;
		if(e.data.enter.nodes.length > 0){
			self.handleOverNodeEvent(e.data.enter.nodes[0]);
		}

		else if(e.data.leave.nodes.length > 0){
			self.handleOutNodeEvent(e.data.leave.nodes[0]);
		}

	}

	Graphbrowser.Modules.NodeExpander.prototype.handleOverNodeEvent = function(node){
		var self = this;

		_active = true;	
		_currentEvent = 'overNode';
		_hoverMap = {};
		_newNodes = [];

		window.clearTimeout(_timeout);
		_timeout = 0;
		$(_infoContainer).empty();
		if (_expanded[node.id] && _expanded[node.id].state === 'expanded') {
			return;
		}

		$(_infoContainer).append('<span>Loading...</span>');
		var inNodes = _callbacks.conn.getNodes(node, 'in', self.onNewInNodesLoaded.bind(self));
		var outNodes = _callbacks.conn.getNodes(node, 'out', self.onNewOutNodesLoaded.bind(self));
	};

	Graphbrowser.Modules.NodeExpander.prototype.handleOutNodeEvent = function(node){
		if (!_timeout) {
			_timeout = window.setTimeout(function() {
				if (_timeout) {
					_timeout = 0;
					$(_infoContainer).empty();
					_active = false;
				}
			}, 1000);
		}
	};

	Graphbrowser.Modules.NodeExpander.prototype.onNewOutNodesLoaded = function(result, node){
		var self = this;
		_outNodesLoaded = true;
		var preResult = result;
		result = _callbacks.filterNodes(result, 'out');
		$.each(result, function(i, result){
			if (!_s.graph.nodes(result.targetNode.id)) {
				if (_hoverMap[result.targetNode.type]) {
					_hoverMap[result.targetNode.type]++;
				} else {
					_hoverMap[result.targetNode.type] = 1;
				}
			}
		});
		_newNodes['outNodes'] = result;
		if(_outNodesLoaded && _inNodesLoaded  && _showHoverInfo){
			_inNodesLoaded = false;
			_outNodesLoaded = false;
			self.updateHoverInfo(node, _hoverMap);
		}
	};


	Graphbrowser.Modules.NodeExpander.prototype.onNewInNodesLoaded = function(result, node){
		var self = this;
		_inNodesLoaded = true;
		var preResult = result;
		result = _callbacks.filterNodes(result, 'in');
		$.each(result, function(i, result){
			if (!_s.graph.nodes(result.sourceNode.id)) {
				if (_hoverMap[result.sourceNode.type]) {
					_hoverMap[result.sourceNode.type]++;
				} else {
					_hoverMap[result.sourceNode.type] = 1;
				}
			}
		});
		_newNodes['inNodes'] = result;
		if(_outNodesLoaded && _inNodesLoaded && _showHoverInfo){
			_inNodesLoaded = false;
			_outNodesLoaded = false;
			self.updateHoverInfo(node, _hoverMap);
		}
	};

	Graphbrowser.Modules.NodeExpander.prototype.updateHoverInfo = function(node, hoverMap) {
		var self = this;
		_active = false;
		var graphInfo  = $(_infoContainer);
		var num        = Object.keys(hoverMap).length;
		var i          = 0;
		var size       = node['renderer1:size'];
		var radius     = Math.max(size, 40);
		var x          = Math.floor(node['renderer1:x']);
		var y          = node['renderer1:y'];
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
			graphInfo.append('<button class="nodeExpander-expandbutton" id="' + key + '-button" style="position: absolute; top: 0px; left: 0px; z-index: 10000;" title="' + label + '">' + value + '</button>');
			graphInfo.append('<button class="btn btn-xs nodeExpander-infobutton" style="margin: 4px; color: #000; background-color: ' + color[key] + '">' + label + '</button>');
			var button = $('#' + key + '-button');
			button.css('border', 'none');
			button.css('background-color', color[key]);
			button.css('color', '#000');
			button.css('top', (y + dy + _marginY) + 'px');
			button.css('left', (x + dx + _marginX) + 'px');
			button.on('click', function(e) {
				graphInfo.empty();
				self.onExpandNode(node, key);
				_active = false;
			});
			i++;
		});
	};

	Graphbrowser.Modules.NodeExpander.prototype.collapseNode = function(node) {
		var self = this;
		if(typeof node === "string")
			node = _s.graph.nodes(node);
		if (node) {
			var id = node.id;
			if (_expanded[id] && _expanded[id].state === 'expanded') {
				var edges = _expanded[id].edges;
				var nodes = _expanded[id].nodes;
				$.each(edges, function(i, e) {
					try { 
						_s.graph.dropEdge(e); 
					} catch(x) {}
				});
				$.each(nodes, function(i, n) {
					self.collapseNode(_s.graph.nodes(n));
					try { 
						_s.graph.dropNode(n); 
					} catch(x) {}
				});
				_expanded[id].state = 'collapsed';
			}
		}
		_s.refresh();
	};

	Graphbrowser.Modules.NodeExpander.prototype.onExpandNode = function(node, type) {
		var self = this;

		if(!_newNodes['inNodes'] && !_newNodes['outNodes']){
			return;
		}

		var id = node.id;
		var x  = node.x;
		var y  = node.y;

		var sigmaNodes = [];
		var sigmaEdges = [];

		$.each(_newNodes['outNodes'], function(i, result) {
			//if ((!type || result.targetNode.type === type) && !_s.graph.nodes(result.targetNode.id)) {
			if ((!type || result.targetNode.type === type)) {
				self.addOutNode(sigmaNodes, sigmaEdges, result, x, y);
			}
		});
		$.each(_newNodes['inNodes'], function(i, result) {
			//if ((!type || result.sourceNode.type === type) && !_s.graph.nodes(result.sourceNode.id)) {
			if ((!type || result.sourceNode.type === type)) {
				self.addInNode(sigmaNodes, sigmaEdges, result, x, y);
			}
		});

		self.pickNodes(node, sigmaNodes, sigmaEdges);
		_showHoverInfo = true;
	};

	Graphbrowser.Modules.NodeExpander.prototype.onNodesSelected = function(selectedNodes, parentNode){
		var self = this;

		if(!selectedNodes || selectedNodes.length <= 0){
			return;
		}

		_expanded[parentNode] = {
			state: 'expanded',
			id: 	parentNode,
			nodes: _expanded[parentNode] ? _expanded[parentNode].nodes : [],
			edges: _expanded[parentNode] ? _expanded[parentNode].edges : []
		};

		self.addNodesToGraph(parentNode, selectedNodes, _newEdges);
	}

	Graphbrowser.Modules.NodeExpander.prototype.pickNodes = function (current, nodes, edges){
		var self = this;
		_newNodes = [];
		_newNodes = $.extend(_newNodes, nodes);
		_newEdges = $.extend(_newEdges, edges);
		_callbacks.pickNodes(_newNodes, self.onNodesSelected.bind(self), current.id, current.label);
	}; 

	Graphbrowser.Modules.NodeExpander.prototype.addOutNode = function (nodes, edges, result, x, y) {
		var sourceNode = result.sourceNode;
		var targetNode = result.targetNode;
		var angle = Math.random()* Math.PI * 2;
		var newX = x + (Math.cos(angle) * 200);
		var newY = y + (Math.sin(angle) * 200);
		var size = _newNodeSize;

		nodes.push({id: targetNode.id, label: targetNode.name, size: size, x: newX, y: newY, color: color[targetNode.type], nodeType: targetNode.type});
		edges.push({id: result.id, source: sourceNode.id, target: targetNode.id, size: _newEdgesSize, relType: result.type, label: result.relType, type: _edgeType, color: '#999'});
	};

	Graphbrowser.Modules.NodeExpander.prototype.addInNode = function(nodes, edges, result, x, y) {
		var sourceNode = result.sourceNode;
		var targetNode = result.targetNode;
		var angle = Math.random()* Math.PI * 2;
		var newX = x + (Math.cos(angle) * 200);
		var newY = y + (Math.sin(angle) * 200);
		var size = _newNodeSize;

		nodes.push({id: sourceNode.id, label: sourceNode.name, size: size, x: newX, y: newY, color: color[sourceNode.type], nodeType: sourceNode.type});
		edges.push({id: result.id, source: sourceNode.id, target: targetNode.id, size: _newEdgesSize, relType: result.type, label: result.relType, type: _edgeType, color: '#999'});
	};

	Graphbrowser.Modules.NodeExpander.prototype.addEdge = function (result){  
		var sourceNode = result.sourceNode;
		var targetNode = result.targetNode;
		var id = result.id;
		edges[result.id]     = {id: result.id, source: sourceNode.id, target: targetNode.id, size: _newEdgesSize, relType: result.type, label: result.relType, type: _edgeType, color: '#999'};
	};

	Graphbrowser.Modules.NodeExpander.prototype.addNodesToGraph = function(current, nodes, edges){
		var self = this;
		var added = 0;

		if(_onAddNodes !== undefined && typeof _onAddNodes === 'function')
			_onAddNodes(current);

		$.each(nodes, function(i, node){
			try {
                            _s.graph.addNode({id: node.id, label: node.label, size: node.size, x: node.x, y: node.y, color: node.color, nodeType: node.nodeType, type: _nodesShape});
                            // only add expanded node if addNode() is successful => node did not exist before
                            _expanded[current].nodes.push(node.id);
                            added++;
                        } catch (e) {}
                });

		$.each(edges, function(i, edge){
                    try {
                        _s.graph.addEdge({id: edge.id, source: edge.source, target: edge.target, label: edge.label, type: edge.type, color: edge.color, relType: edge.relType, relName: edge.label, size: edge.size});
                        // only add expanded node if addNode() is successful => node did not exist before
                        _expanded[current].edges.push(edge.id);
                        added++;
                    } catch (e) {}
                });

		_newNodes = [];
		_newEdges = [];

		_callbacks.dataChanged(self.name);  

		if(_onNodesAdded !== undefined && typeof _onNodesAdded === 'function')
			_onNodesAdded();
	};


	Graphbrowser.Modules.NodeExpander.prototype.onUndo = function(){
		var self = this;

		if(_undoStack.length < 1){
			return;
		}

		_undoStack.splice(_undoStack.length -1, 1);
		_expanded = _undoStack[_undoStack.length - 1];

		if(!_expanded){
			_expanded = {};
		}		
	};

	Graphbrowser.Modules.NodeExpander.prototype.onDataChanged = function(changeType){
		var self = this;
		if(changeType === 'undo'){
			return;
		}
		if(changeType === self.name ){
			var exp = jQuery.extend({}, _expanded);
			_undoStack.push(exp);	
		}	
	};

	Graphbrowser.Modules.NodeExpander.prototype.onSave = function(){
		var status = JSON.stringify(_expanded);
		return status;
	};

	Graphbrowser.Modules.NodeExpander.prototype.onRestore = function(data){
		var expanded = JSON.parse(data);
		_expanded = expanded;
	};

	Graphbrowser.Modules.NodeExpander.prototype.onReset = function(){
		_undoStack = [];
		Object.keys(_expanded).forEach(function(key){
			delete _expanded[key]; }
			);
	};

	Graphbrowser.Modules.NodeExpander.prototype.expandNode = function(id){
		var self = this; 
		var node = _s.graph.nodes(id);
		_showHoverInfo = false;
		
		var inNodes = _callbacks.conn.getNodes(node, 'in', self.onNewInNodesLoaded.bind(self));
		var outNodes = _callbacks.conn.getNodes(node, 'out', self.onNewOutNodesLoaded.bind(self));
		window.setTimeout(function(){
			self.onExpandNode(node);
		}, 20);
	};
}).call(this);