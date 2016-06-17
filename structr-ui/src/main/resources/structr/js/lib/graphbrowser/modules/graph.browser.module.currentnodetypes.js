var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	var _s, _callbacks, _classes, _noView;
	var _nodeTypes = [];
	var _hiddenTypes = [];
	var _relNames = [];

	Graphbrowser.Modules.CurrentNodeTypes = function (sigmaInstance, callbacks){
		var self = this;
		self.name = "currentNodeTypes";

		_s = sigmaInstance;
		_callbacks = callbacks;
		_classes = '';
	};

	Graphbrowser.Modules.CurrentNodeTypes.prototype.types = ["dataChangedHandler", "startListener"];

	Graphbrowser.Modules.CurrentNodeTypes.prototype.init = function(settings){
		var self = this;

		if(settings){
			settings.container !== undefined ? self.createView(settings.container) :  _noView = true;
			settings.classes !== undefined ? _classes = settings.classes : _classes = '';
		}
		else
			_noView = true;

		_callbacks.api.getCurrentNodeTypes = self.getCurrentNodeTypes.bind(self);
		_callbacks.api.getCurrentRelTypes = self.getCurrentRelTypes.bind(self);
		_callbacks.api.highlightNodeType = self.highlightNodeType.bind(self);
		_callbacks.api.unhighlightNodeType = self.unhighlightNodeType.bind(self);
		_callbacks.api.highlightRelType = self.highlightRelType.bind(self);
		_callbacks.api.unhighlightRelType = self.unhighlightRelType.bind(self);
	};

	Graphbrowser.Modules.CurrentNodeTypes.prototype.start = function(){
		var self = this;
		self.onDataChanged();
	};

	Graphbrowser.Modules.CurrentNodeTypes.prototype.createView = function(container){
		var self = this;
		_noView = false;
		if(container.charAt(0) !== '#'){
			container = '#' + container;
		}
		$(container).append('<div id="currentNodeTypes-displayTypes" class="currentNodeTypes-displayTypes"></div>'); 
		
	};

	Graphbrowser.Modules.CurrentNodeTypes.prototype.onDataChanged = function(changeType){

		var self = this;
		_nodeTypes = [];
		_relNames = [];
		var newNodeTypes = [];
		var nodeTypeBox = $('#currentNodeTypes-displayTypes');
		nodeTypeBox.empty();

		//get all types displayed in the graph and check if they are hidden
		$.each(_s.graph.nodes(), function(i, node){
			if(_nodeTypes.length === 0){
				if(node.hidden === true){
					_hiddenTypes.push(node.nodeType);
				}
				_nodeTypes.push(node.nodeType);
			}
			var isAlready = false;
			$.each(_nodeTypes, function(j, nodeType){	
				if(node.nodeType === nodeType){
					isAlready = true;
					return false;
				}
			});
			if(!isAlready){
				if(node.hidden === true){
					_hiddenTypes.push(node.nodeType);
				}
				_nodeTypes.push(node.nodeType);
			}
		});

		$.each(_s.graph.edges(), function(i, edge){
			if(_relNames.length === 0){
				if(edge.hidden === true){
					_hiddenTypes.push(edge.relName);
				}
				_relNames.push(edge.relName);
			}
			var isAlready = false;
			$.each(_relNames, function(j, relName){	
				if(edge.relName === relName){
					isAlready = true;
					return false;
				}
			});
			if(!isAlready){
				if(edge.hidden === true){
					_hiddenTypes.push(edge.relName);
				}
				_relNames.push(edge.relName);
			}
		});

		if(_noView)
			return;

		//build a button for each type and add it to the settings-container
		$.each(_nodeTypes, function(i, nodeType){
			nodeTypeBox.append('<a id="currentNodeTypes-nodeType-btn-' + nodeType + '" class="currentNodeTypesButton ' + _classes + '" style="background-color: ' + color[nodeType] + '">' + nodeType + '</a>');
			var newButton = $('#currentNodeTypes-nodeType-btn-' + nodeType);

			//if the nodes with the type are hidden, change the button accordingly
			if(_hiddenTypes.indexOf(nodeType) > -1){
				newButton.css('text-decoration', 'line-through');
				_callbacks.hideNodeType(nodeType, true);
			}

			//attach the clicklistener for the new button
			newButton.on('click', function(){
				var index = _hiddenTypes.indexOf(nodeType);
				if(index > -1){
					$(this).css('text-decoration', '');
					_hiddenTypes.splice(index, 1);
				}
				else{
					_hiddenTypes.push(nodeType);
					$(this).css('text-decoration', 'line-through');
				}
				_callbacks.hideNodeType(nodeType);
			}).on('mouseover', function(){
				self.highlightNodeType(nodeType);
			}).on('mouseout', function(){
				self.unhighlightNodeType(nodeType);
			});
		});

		//nodeTypeBox.append('<div><p id="currentNodeTypes-nodeType-Btn' + _s.graph.nodes().length + ' Nodes' + '" class="btn btn-xs btn-block" style="margin: 4px; color: #000; background-color: ' + 'ffffff' + '">' + + _s.graph.nodes().length + ' Nodes' + '</p></div>');
		//nodeTypeBox.append('<div><p id="currentNodeTypes-nodeType-Btn' + _s.graph.edges().length + ' Edges' + '" class="btn btn-xs btn-block" style="margin: 4px; color: #000; background-color: ' + 'ffffff' + '">' + _s.graph.edges().length + ' Edges' + '</p></div>');
	};


	Graphbrowser.Modules.CurrentNodeTypes.prototype.getCurrentNodeTypes = function() {
		return _nodeTypes;
	};

	Graphbrowser.Modules.CurrentNodeTypes.prototype.getCurrentRelTypes = function() {
		return _relNames;
	};

	Graphbrowser.Modules.CurrentNodeTypes.prototype.highlightNodeType = function(nodeType) {
		var self = this;

		var nodesForHalo = [];
		var colorForHalo = null;

		_s.graph.nodes().forEach(function(node) {
			if (node.nodeType === nodeType) {
				nodesForHalo.push(node);
				colorForHalo = self.colorLuminance(node.color, -.1);
			}
		});

		_s.renderers[0].bind('render', function(e) {
				_s.renderers[0].halo({
				nodeHaloColor: colorForHalo,
				nodeHaloSize: 10,
				nodes: nodesForHalo
			});
		});
		_s.refresh({skipIndexation: true});
	};

	Graphbrowser.Modules.CurrentNodeTypes.prototype.unhighlightNodeType = function(nodeType) {
		_s.renderers[0].unbind('render');
		_s.refresh({skipIndexation: true});
	};

	Graphbrowser.Modules.CurrentNodeTypes.prototype.highlightRelType = function(relType) {
		_s.graph.edges().forEach(function(edge) {
			if(edge.relName === relType){
				edge.active_color = '#81ce25';
				edge.active = true;
			}
		});
		_s.refresh({skipIndexation: true});
	};

	Graphbrowser.Modules.CurrentNodeTypes.prototype.unhighlightRelType = function(relType) {
		_s.graph.edges().forEach(function(edge) {
			if(edge.relName === relType){
				edge.active_color = _s.settings('defaultEdgeActiveColor');
				edge.active = false;
			}
		});
		_s.refresh({skipIndexation: true});
	};


	Graphbrowser.Modules.CurrentNodeTypes.prototype.colorLuminance = function(hex, lum) {
		var self = this;
		hex = String(hex).replace(/[^0-9a-f]/gi, '');
		lum = lum || 0;
		if (hex.length < 6) {
			hex = hex[0] + hex[0] + hex[1] + hex[1] + hex[2] + hex[2];
		}

		var r = self.convertToDec(hex.substr(0, 2)), g = self.convertToDec(hex.substr(2, 2)), b = self.convertToDec(hex.substr(4, 2));

		var rgb = self.desaturate(r, g, b, .5);

		var newHex = "#", c, i;
		for (i = 0; i < 3; i++) {
			c = rgb[i];
			c = Math.round(Math.min(Math.max(0, c + (c * lum)), 255)).toString(16);
			newHex += ("00" + c).substr(c.length);
		}
		return newHex;
	};

	Graphbrowser.Modules.CurrentNodeTypes.prototype.convertToDec = function(hex) {
		return parseInt(hex, 16);
	};

	Graphbrowser.Modules.CurrentNodeTypes.prototype.desaturate = function(r, g, b, k) {
		var intensity = 0.3 * r + 0.59 * g + 0.11 * b;
		r = Math.floor(intensity * k + r * (1 - k));
		g = Math.floor(intensity * k + g * (1 - k));
		b = Math.floor(intensity * k + b * (1 - k));
		return [r, g, b];
	};
}).call(window);