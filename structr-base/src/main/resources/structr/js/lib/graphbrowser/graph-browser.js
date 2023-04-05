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

var Graphbrowser = Graphbrowser || {};
Graphbrowser.Control = Graphbrowser.Control || {};

(function() {
	'use strict';

	Graphbrowser.Control.SigmaControl = function(callbacks, conf){
		var _s,
			_callbacks,
			_apiMethods,
			_sigmaSettings,
			_sigmaContainer,
			_lassoSettings,
			_dragListener,
			_activeState,
			_lasso,
			_keyboard,
			_select,
			_isActive = false,
			_hasDragged = false,
			_shiftKey = false,
			_ctrlKey = false,
			_altKey = false,
			_keyupHandlers = [],
			_keydownHandlers = [],
			_defaultNodeTypeProperties = {},
			_defaultEdgeTypeProperties = {},
			_callbacks = callbacks;

		// check if there are settings for sigma in the browsers settings. Else use the default values
		conf.sigmaSettings !== undefined ?
			_sigmaSettings = conf.sigmaSettings :
			_sigmaSettings = {
				immutable: false,
				minNodeSize: 1,
				maxNodeSize: 10,
				borderSize: 4,
				defaultNodeBorderColor: '#a5a5a5',
				singleHover: true,
				doubleClickEnabled: false,
				minEdgeSize: 4,
				maxEdgeSize: 4,
				enableEdgeHovering: true,
				edgeHoverColor: 'default',
				edgeHoverSizeRatio: 1.3,
				edgeHoverExtremities: true,
				defaultEdgeColor: '#999',
				defaultEdgeHoverColor: '#888',
				minArrowSize: 4,
				maxArrowSize: 8,
				labelSize: 'proportional',
				labelSizeRatio: 1,
				labelAlignment: 'right',
				nodeHaloColor: 'rgba(236, 81, 72, 0.2)',
				nodeHaloSize: 20
			};
		(conf.graphContainer !== undefined && typeof conf.graphContainer === 'string') ?
			_sigmaContainer = conf.graphContainer : _sigmaContainer = "graph-container";

		conf.lassoSettings !== undefined ? _lassoSettings = conf.lassoSettings :
			_lassoSettings = {
				'strokeStyle': 'rgb(236, 81, 72)',
				'lineWidth': 2,
				'fillWhileDrawing': true,
				'fillStyle': 'rgba(236, 81, 72, 0.2)',
				'cursor': 'crosshair'
			}

		function init(){
			sigma.renderers.def = sigma.renderers.canvas;
			_s = new sigma({
				container: _sigmaContainer,
				settings: _sigmaSettings
			});

			_activeState = sigma.plugins.activeState(_s);
			_keyboard = sigma.plugins.keyboard(_s, _s.renderers[0]);
			_select = sigma.plugins.select(_s, _activeState);
			_dragListener = new sigma.plugins.dragNodes(_s, _s.renderers[0], _activeState);
			_lasso = new sigma.plugins.lasso(_s, _s.renderers[0], _lassoSettings);

			_select.bindKeyboard(_keyboard);
			_select.bindLasso(_lasso);

			activate();

			$(window).keyup(keyUpFunction);
			$(window).on('keydown', keyDownFunction);

			// export the plugin objects and the event binder functions for the modules.
			_callbacks.sigmaCtrl = {};
			_callbacks.sigmaPlugins = {};
			_callbacks.sigmaCtrl.unbindSigmaEvent = unbindEvent;
			_callbacks.sigmaCtrl.bindSigmaEvent = bindEvent;
			_callbacks.sigmaCtrl.addNode = addNode;
			_callbacks.sigmaCtrl.addEdge = addEdge;
			_callbacks.sigmaPlugins.activeState = _activeState;
			_callbacks.sigmaPlugins.keyboard = _keyboard;
			_callbacks.sigmaPlugins.select = _select;
			_callbacks.sigmaPlugins.lasso = _lasso;

			// export for the api
			for(var m in _apiMethods){
				_callbacks.api[m] = _apiMethods[m];
			}

			return _s;
		};

		function kill(){
			$(window).unbind("keyup", keyUpFunction);
			$(window).unbind("keydown", keyDownFunction);

			_keyupHandlers = [];
			_keydownHandlers = [];

			_s.kill();
			_s = undefined;
		};

		function keyUpFunction(e) {
			switch(e.which){
				case 16:
					_shiftKey = false;
					break;
				case 17:
					_ctrlKey = false;
					break;
				case 18:
					_altKey = false;
					break;
				default:
					return false;
			}
			var handler;
			var noKey = _shiftKey || _altKey || _ctrlKey;
			for(var i = 0; i < _keyupHandlers.length; i++){
				_keyupHandlers[i]({shiftKey: _shiftKey, ctrlKey: _ctrlKey, altKey: _altKey, noKey: !noKey});
			}
		};

		function keyDownFunction(e) {
			// This hack prevents FF from closing WS connections on ESC
			if (e.keyCode === 27) {
				e.preventDefault();
			}
			switch(e.which){
				case 16:
					_shiftKey = true;
					break;
				case 17:
					_ctrlKey = true;
					break;
				case 18:
					_altKey = true;
					break;
			}
			var handler;
			var noKey = _shiftKey || _altKey || _ctrlKey;
			for(var i = 0; i < _keydownHandlers.length; i++){
				_keydownHandlers[i]({shiftKey: _shiftKey, ctrlKey: _ctrlKey, altKey: _altKey, noKey: !noKey});
			}
		};

		function activate() {
			_isActive = true;
		};

		function deactivate() {
			_callbacks.refreshSigma();
		};

		function addApiMethod(name, func){
			_apiMethods = _apiMethods || {}
			_apiMethods[name] = func;
		}

		/**
		* Function to unbind a handler function from an event
		*
		* @param  {string}	event       the name of the event
		* @param  {object}   handler 	the function handler that was bound to the event
		*/
		function unbindEvent(event, handler) {
			if(!(typeof event === 'string') || !(typeof handler === 'function'))
				return;

			if(event === 'keyup'){
				var index = _keyupHandlers.indexOf(handler);
				_keyupHandlers.splice(index, 1);
			}
			if(event === 'keydown') {
				var index = _keydownHandlers.indexOf(handler);
				_keydownHandlers.splice(index, 1);
			}

			if(event.includes('drag'))
				_dragListener.unbind(event, handler);
			else
				_s.unbind(event, handler);
			//_callbacks.refreshSigma();
		};
		addApiMethod('unbindEvent', unbindEvent);

		/**
		* Function to bind a handler function to a given event
		*
		* @param  {string}	event       the name of the event
		* @param  {object}   handler 	the function handler that was bound to the event
		*/
		function bindEvent(event, handler) {
			if(!(typeof event === 'string') || !(typeof handler === 'function'))
				return;

			if(event === 'keyup')
				_keyupHandlers.push(handler);
			if(event === 'keydown')
				_keydownHandlers.push(handler);
			if(event.includes('drag'))
				_dragListener.bind(event, handler);
			else
				_s.bind(event, handler);
			//_callbacks.refreshSigma();
		};
		addApiMethod('bindEvent', bindEvent);

		/**
		* Function that adds a node to the graphBrowsers graph
		*
		* @param  {object, string}   node 	the node that should be added to the browser, or just the id that a new node should have.
		*/
		function addNode(node) {
			if(typeof node === 'string'){
				_s.graph.addNode({id: node, label: "", color: _s.settings('defaultNodeColor'), size: _s.settings('defaultNodeSize')});
			} else{

				for(var defaultProp in _defaultNodeTypeProperties[node.nodeType]){
					node[defaultProp] = _defaultNodeTypeProperties[node.nodeType][defaultProp];
				}

				_s.graph.addNode(node);
			}
		};
		addApiMethod('addNode', addNode);

		/**
		* Function that adds a node to the graphBrowsers graph
		*
		* @param  	{object, string}   	node 	the node that should be added to the browser, or just the id the new edge should have
		* @param 	{string}			source	the source node id of the new edge. Only nessesary, when the function is called with the id of the new edge
		* @param 	{string}			target	the target node id of the new edge. Only nessesary, when the function is called with the id of the new edge
		*/
		function addEdge(edge, source, target) {
			if(source !== undefined && target !== undefined && (typeof edge === "string")){
				_s.graph.addEdge({id: edge, source: source, target: target, color: _s.settings('defaultEdgeColor'), size: _s.settings('defaultEdgeSize')})
			} else if(typeof edge === 'object'){

				for(var defaultProp in _defaultEdgeTypeProperties[edge.relType]){
					edge[defaultProp] = _defaultEdgeTypeProperties[edge.relType][defaultProp];
				}

				_s.graph.addEdge(edge);
			}
		};
		addApiMethod('addEdge', addEdge);

		/**
		* Function that drops a node from the graph browsers graph
		*
		* @param  	{object, string}   	node 	the node, or the id of the node that should be dropped
		*/
		function dropNode(node) {
			if(typeof node === "string"){
				if(_s.graph.nodes(node)){
					_s.graph.dropNode(node);
				}
			}
			else{
				if(_s.graph.nodes(node.id)){
					_s.graph.dropNode(node.id);
				}
			}
			_s.refresh();
		};
		addApiMethod('dropNode', dropNode);

		/**
		* Function that drops a collection of nodes from the graph browser
		*
		* @param  	{array}   	nodes 	the array of node ids
		* @param 	{boolean}	keep	if true, all nodes except the ones given in the array will be dropped. If false, all given nodes will be dropped.
		*/
		function dropNodes(nodes, keep){
			if(keep){
				$.each(_s.graph.nodes(), function(i, node){
					if(nodes.indexOf(node.id) < 0){
						_s.graph.dropNode(node);
					}
				});
			}
			else{
				$.each(_s.graph.nodes(), function(i, node){
					if(nodes.indexOf(node.id) >= 0){
						_s.graph.dropNode(node);
					}
				});
			}

			_s.refresh();
		};
		addApiMethod('dropNodes', dropNodes);

		/**
		* Function that drops an edge from the graph browsers graph
		*
		* @param  	{object, string}   	edge 	the edge, or the id of the edge that should be dropped
		*/
		function dropEdge(edge) {
			if(typeof edge === "string"){
				if(_s.graph.edges(edge)){
					_s.graph.dropEdge(edge);
				}
			}
			else{
				if(_s.graph.edges(edge.id)){
					_s.graph.dropEdge(edge.id);
				}
			}

			_s.refresh();
		};
		addApiMethod('dropEdge', dropEdge);

		/**
		* Function that drops a collection of edges from the graph browser
		*
		* @param  	{array}   	nodes 	the array of edge ids
		* @param 	{boolean}	keep	if true, all edges except the ones given in the array will be dropped. If false, all given edges will be dropped.
		*/
		function dropEdges(edges, keep){
			if(keep){
				$.each(_s.graph.edges(), function(i, edge){
					if(edges.indexOf(edge.id) < 0){
						_s.graph.dropEdge(edge);
					}
				});
			}
			else{
				$.each(_s.graph.edges(), function(i, edge){
					if(edges.indexOf(edge.id) >= 0){
						_s.graph.dropEdge(edge);
					}
				});
			}

			_s.refresh();
		};
		addApiMethod('dropEdges', dropEdges);

		/**
		* Function that drops an element from the graph browsers graph
		*
		* @param  	{string}   	element 	the id of the element that should be dropped
		*/
		function dropElement(element) {
			if(_s.graph.nodes(element)){
				_s.graph.dropNode(element);
			}

			else if(_s.graph.edges(element)){
				_s.graph.dropEdge(element);
			}

			_s.refresh();
		};
		addApiMethod('dropElement', dropElement);

		/**
		* Function that updates the properties of a node
		*
		* @param  	{string}   	node 	the id of the node
		* @param 	{object}	obj 	the object with properties wich will override the properties of the given node
		* @param 	{array}		fields	an array of property names that should be copied from the given object to the node. If undefined, all elements will be copied.
		* @param 	{object}	map 	an object that maps one property of the node to an other property.
		*/
		function updateNode(node, obj, fields, map) {
			var gNode = _s.graph.nodes(node);

			if(gNode === undefined)
				return;

			if(fields){
				fields.forEach(function(key, index){
					gNode[key] = obj[key];
				});
			}
			else{
				for(var key in obj){
					gNode[key] = obj[key];
				}
			}

			if(typeof map === 'object'){
				for(var mapPair in map){
					gNode[mapPair] = gNode[map[mapPair]];
				}
			}
			_s.refresh({skipIndexation: false});
		};
		addApiMethod('updateNode', updateNode);

		/**
		* Function that updates the properties of a edge
		*
		* @param  	{string}   	edge 	the id of the edge
		* @param 	{object}	obj 	the object with properties wich will override the properties of the given edge
		* @param 	{array}		fields	an array with the names of the properties that should be copied from the given object to the edge. If undefined, all elements will be copied.
		* @param 	{object}	map 	an object that maps one property of the edge to an other property.
		*/
		function updateEdge(edge, obj, fields, map) {
			var gEdge = _s.graph.edges(edge);

			if(gEdge === undefined)
				return;

			if(fields){
				fields.forEach(function(key, index){
					gEdge[key] = obj[key];
				});
			}
			else{
				for(var key in obj){
					gEdge[key] = obj[key];
				}
			}

			if(typeof map === 'object'){
				for(var mapPair in map){
					gEdge[mapPair] = gEdge[map[mapPair]];
				}
			}
			_s.refresh({skipIndexation: false});
		};
		addApiMethod('updateEdge', updateEdge);

		/**
		* Function that returns the current camera ratio of the graph browser
		*
		* @returns 	{number}   	the current camera ratio
		*/
		function getCameraRatio() {
			return _s.camera.ratio;
		};
		addApiMethod('getCameraRatio', getCameraRatio);

		/**
		* Function that returns the complete set of nodes in the graph browser
		*
		* @returns 	{Array}   	the nodes of the graph browser
		*/
		function getNodes() {
			return _s.graph.nodes();
		};
		addApiMethod('getNodes', getNodes);

		/**
		* Function that returns the complete set of edges in the graph browser
		*
		* @returns 	{Array}   	the edges of the graph browser
		*/
		function getEdges() {
			return _s.graph.edges();
		};
		addApiMethod('getEdges', getEdges);

		/**
		* Function that returns a node
		*
		* @param	{string}	node 	the id of the node
		* @returns 	{object}   	the node that matches the given id
		*/
		function getNode(node) {
			var n;
			if(typeof node === "string")
				n = _s.graph.nodes(node);
			else
				n = _s.graph.nodes(node.id);

			if(n)
				return n;
			else
				return null;
		};
		addApiMethod('getNode', getNode);

		/**
		* Function that returns a node whos property matches the given value
		*
		* @param	{string}	prop 	the property to search
		* @param	{string}	value 	the value to match
		* @returns 	{object}   	the node that matches the given id
		*/
		function getNodeByProperty(prop, value) {
			var match = null;
			_s.graph.nodes().forEach(function(node){
				if(node[prop] === value)
					match = node;
			});
			return match;
		};
		addApiMethod('getNodeByProperty', getNodeByProperty);

		/**
		* Function that returns a edge
		*
		* @param	{string}	edge 	the id of the edge
		* @returns 	{object}   	the edge that matches the given id
		*/
		function getEdge(edge) {
			var e;
			if(typeof edge === "string")
				e = _s.graph.edges(edge);
			else
				e = _s.graph.edges(edge.id);

			if(e)
				return e;
			else
				return null;
		};
		addApiMethod('getEdge', getEdge);

		/**
		* Function that return an array of edges that connect the same nodes
		*
		* @param	{string}	sourceId 	the id of the source node
		* @param	{string}	targedId 	the id of the target nodes
		* @param	{string}	relType 	if set, the function only matches relationships with the given type
		* @returns 	{object}   	the relationships between the nodes
		*/
		function findRelationships(sourceId, targedId, relType) {
	        var edges = [];
	        _s.graph.edges().forEach(function(edge) {
	            if (edge.source === sourceId && edge.target === targedId && (!relType || edge.relType === relType)) {
	                edges.push(edge);
	            }
	        });
	        return edges;
	    };
	    addApiMethod('findRelationships', findRelationships);

		/**
		* Function that will override a given setting of the sigma instance of the graph browser
		*
		* @param 	{string}			setting 	the name of the setting
		* @param 	{number, string}	value		the new value of the sigma setting
		* @returns  {object}   	the edge that matches the given id
		*/
		function changeSigmaSetting(setting, value) {
			if(typeof setting !== 'string')
				return;
			_s.settings(setting, value);
			_s.refresh({skipIndexation: true});
		};
		addApiMethod('changeSigmaSetting', changeSigmaSetting);

		/**
		* Function that will return the sigma object of the graph browser
		*
		* @returns  {object}   	the sigma object of the graph browser
		*/
		function getSigma(setting, value) {
			if(_s)
				return _s;
		};
		addApiMethod('getSigma', getSigma);

		/**
		* Function to add or change the value of a property of a node
		*
		* @param 	{string}	node 		the node to change
		* @param 	{string}	property  	the property to add or change
		* @param 	{string}	value		the value for the property
		* @returns  {object}   	the node that was changed
		*/
		function setNodeProperty(node, property, value) {
			if(typeof node === 'string')
				node = _s.graph.nodes(node);
			node[property] = value;
			_s.refresh({skipIndexation: true});
			return node;
		};
		addApiMethod('setNodeProperty', setNodeProperty);

		/**
		* Function to add or change the value of a property of a edge
		*
		* @param 	{string}	edge 		id of the edge to change
		* @param 	{string}	property  	the property to add or change
		* @param 	{string}	value		the value for the property
		* @returns  {object}   	the edge that was changed
		*/
		function setEdgeProperty(edge, property, value) {
			if(typeof edge === 'string')
				edge = _s.graph.edges(edge);
			edge[property] = value;
			_s.refresh({skipIndexation: true});
			return edge;
		};
		addApiMethod('setEdgeProperty', setEdgeProperty);

		/**
		* Function to add a default value for a node property that is applied to every new node with the given node type when it is added to the graph browser
		*
		* @param 	{string}	nodeType 	specifies to wich node type the default value will be applied
		* @param 	{string}	property  	the property to add or change
		* @param 	{string}	value		the value for the property
		* @param 	{boolean}	applyNow	if true the value will be applied to all current nodes
		*/
		function setDefaultNodeTypeProperty(nodeType, property, value, applyNow) {
			_defaultNodeTypeProperties[nodeType] = _defaultNodeTypeProperties[nodeType] || {};
			_defaultNodeTypeProperties[nodeType][property] = value;

			if(applyNow){
				_s.graph.nodes().forEach(function(node, index){
					for(var defaultProp in _defaultNodeTypeProperties[node.nodeType]){
						node[defaultProp] = _defaultNodeTypeProperties[node.nodeType][defaultProp];
					}
				});
			}
			_s.refresh({skipIndexation: true});
		};
		addApiMethod('setDefaultNodeTypeProperty', setDefaultNodeTypeProperty);

		/**
		* Function to add a default value for a node property that is applied to every new node with the given node type when it is added to the graph browser
		*
		* @param 	{string}			nodeType 	specifies to wich node type the default value will be applied
		* @param 	{string}			property  	the property to add or change
		* @param 	{string}			value		the value for the property
		* @param 	{boolean}	applyNow	if true the value will be applied to all current edges
		*/
		function setDefaultEdgeTypeProperty(edgeType, property, value, applyNow) {
			_defaultEdgeTypeProperties[edgeType] = _defaultEdgeTypeProperties[edgeType] || {};
			_defaultEdgeTypeProperties[edgeType][property] = value;

			if(applyNow){
				_s.graph.edges().forEach(function(edge, index){
					for(var defaultProp in _defaultEdgeTypeProperties[edge.relType]){
						edge[defaultProp] = _defaultEdgeTypeProperties[edge.edgeType][defaultProp];
					}
				});
			}
			_s.refresh({skipIndexation: true});
		};
		addApiMethod('setDefaultEdgeTypeProperty', setDefaultEdgeTypeProperty);

		/**
		* Function that moves the camera to the given node
		*
		* @param 	{string}	node 		the node to go to
		* @param 	{number}	ratio		the new camera ratio
		* @param 	{boolean}	anim  		if true the camera is animated on the way to the node
		*/
		function goToNode(node, ratio, anim) {
			if(typeof node === 'string')
				node = _s.graph.nodes(node);

			if(!node)
				return;

			if(!ratio)
				ratio = getCameraRatio();

			if(!anim){
				sigma.misc.animation.camera(
					_s.camera,
				  	{
				    	x: node[_s.camera.readPrefix + 'x'],
				    	y: node[_s.camera.readPrefix + 'y'],
				    	ratio: ratio
				  	}
				);
			}

			else{
				sigma.misc.animation.camera(
				  	_s.camera,
				  	{
				    	x: node[_s.camera.readPrefix + 'x'],
				    	y: node[_s.camera.readPrefix + 'y'],
				    	ratio: ratio
				  	},
				  	{
				  		duration: _s.settings('animationsTime')
				  	}
				);
			}
		};
		addApiMethod('goToNode', goToNode);

		/**
		* Function that will set the active state of a given node.
		*
		* @param 	{string|object}	node 		the node to change
		* @param 	{boolean}		status		the new status for the node
		*/
		function setNodeStatus(node, status){
			if(typeof node === 'string')
				node = _s.graph.nodes(node);
			_activeState.addNodes(node.id);
		};
		addApiMethod('setNodeStatus', setNodeStatus);

		/**
		* Function that will set the active state of a given node.
		*
		* @param 	{string|object}	edge 		the node to go to
		* @param 	{boolean}		status		the new camera ratio
		*/
		function setEdgeStatus(edge, status){
			if(typeof edge === 'string')
				edge = _s.graph.edges(edge);
			_activeState.addEdges(edge.id);
		};
		addApiMethod('setEdgeStatus', setEdgeStatus);

		this.init = init;
		this.kill = kill;
	};
})();

var Graphbrowser = Graphbrowser || {};
Graphbrowser.Control = Graphbrowser.Control || {};

(function() {
	'use strict';

	Graphbrowser.Control.ConnectionControl = function(callbackMethods){
		var name = 'ConnectionControl',
			_callbackMethods = callbackMethods,
			_methods = {};

		function addMethod(name, func){
			_methods = _methods || {};
			_methods[name] = func;
		}

		function init(){
			_callbackMethods.conn = _callbackMethods.conn || {};
			for(var m in _methods){
				_callbackMethods.conn[m] = _methods[m];
			}
		};

		function sendRequest(url, method, data, contentType){
			var dataString = undefined;
			if (window.domainOnly && window.domainOnly === true) {
				url += '?domainOnly=true';
			}
			if(contentType === undefined) contentType = "application/json";
			if(typeof data === "object" || typeof data === "string" || $.isArray(data))
				dataString = JSON.stringify(data);

			return $.ajax({
				url: url,
				data: dataString || data,
				contentType: contentType,
				method: method
			});
		};

		function restRequest(ajax, onResult, attr, onError){
			ajax.done(function(data, textStatus) {
				onResult(data.result, attr);
			}).fail(function(jqXHR, textStatus, errorThrown){
				console.log("Graph-Browser-Rest: Status " + jqXHR.status + " - ERROR: " + errorThrown);
				if(onError)	onError(attr, jqXHR.responseJSON.message);
			});
		};

		function htmlRequest(ajax, onResult, attr, onError){
			ajax.done(function(html, textStatus) {
				onResult(html, attr);
			}).fail(function(jqXHR, textStatus, errorThrown){
				console.log("Graph-Browser-Rest: Status " + jqXHR.status + " - ERROR: " + errorThrown);
				if(onError)	onError(attr, jqXHR.responseJSON.message);
			});
		};

		addMethod('getNodes', function(node, inOrOut, onResult, view){
			if(!view) view = "_graph"
			var url = "/structr/rest/" + (node.nodeType ? node.nodeType + "/" : "") + node.id + "/" + inOrOut + "/" + view;
			var ajax = sendRequest(url, "GET");
			restRequest(ajax, onResult, node);
		});

		addMethod('getData', function(data, nodeOrEdge, onResult, view){
			var url = "/structr/rest/";
			var method;
			if(view === undefined) view = "_graph";

			if($.isArray(data)){
				//resolver cannot handle array of relationships
				url += "resolver";
				method = "POST";
			}
			else{
				url += data.id + "/" + view;
				method = "GET";
			}

			var ajax = sendRequest(url, method, method === "POST" ? data : undefined);
			restRequest(ajax, onResult, nodeOrEdge);
		});

		addMethod('getGraphViews', function(currentId, view, onResult){
			var url = '/structr/rest/GraphView/' + view + '/?viewSource=' + currentId;
			var ajax = sendRequest(url, "GET");
			restRequest(ajax, onResult);
		});

		addMethod('getGraphViewData', function(id, view, onResult){
			var url = '/structr/rest/GraphView/' + view + '/' + id;
			var ajax = sendRequest(url, "GET");
			restRequest(ajax, onResult);
		});

		addMethod('saveGraphView', function(name, graph, data, viewSource, onFinished){
			var newGraphview = {name: name, graph: graph, data: data, viewSource: viewSource};
			var url = '/structr/rest/GraphView'
			var ajax = sendRequest(url, "POST", newGraphview, 'application/json; charset=utf-8');
			restRequest(ajax, onFinished);
		});

		addMethod('getSchemaInformation', function(onResult){
			var url = '/structr/rest/_schema/';
			var ajax = sendRequest(url, "GET");
			restRequest(ajax, onResult);
		});

		addMethod('getRelationshipsOfType', function(relType, onResult, onError){
			var url = '/structr/rest/' + relType;
			var ajax = sendRequest(url, "GET");
			restRequest(ajax, onResult, undefined, onError);
		});

		addMethod('init', function(relType, onResult, onError){
			var url = '/structr/rest/' + relType;
			var ajax = sendRequest(url, "GET");
			restRequest(ajax, onResult, undefined, onError);
		});

		addMethod('deleteRelationship', function(rel, onResult){
			var url = '/structr/rest/' + rel.type + '/ ' + rel.id;
			var ajax = sendRequest(url, "DELETE");
			restRequest(ajax, onResult);
		});

		addMethod('createRelationship', function(source, target, relType, onResult, attr, onError){
			var newRel = {sourceId: source, targetId: target};
			var url = '/structr/rest/' + relType;
			var ajax = sendRequest(url, "POST", newRel);
			restRequest(ajax, onResult, attr, onError);
		});

		addMethod('getStructrPage', function(page, currentId, onResult, onError){
			var url = "/" + page + "/" + currentId;
			var ajax = sendRequest(url, "GET");
			htmlRequest(ajax, onResult, currentId, onError);
		});

		this.init = init;
	};
}).call(window);
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Control = Graphbrowser.Control || {};

(function() {
	'use strict';

	Graphbrowser.Control.ModuleControl = function(callbacks, moduleSettings, sigmaInstance){
		var self = this,
			name = 'ModuleControl',
			_s = sigmaInstance,
			_callbacks = callbacks,
			_attachedModules = {},
			_refreshTimeout = 0,
			_moduleSettings = moduleSettings,
			_browserEventHandlers = {},
			_eventEmitter = {};

		_eventEmitter[_s.id] = {};
		sigma.classes.dispatcher.extend(_eventEmitter[_s.id]);

		_callbacks.eventEmitter = _eventEmitter;

		_eventEmitter[_s.id].bind('dataChanged', refreshSigma);
		_eventEmitter[_s.id].bind('doLayout', doLayout);
		_eventEmitter[_s.id].bind('prepareSaveGraph', onSave);
		_eventEmitter[_s.id].bind('restoreGraph', onRestore);
		_eventEmitter[_s.id].bind('pickNodes', onPickNodes);
		_eventEmitter[_s.id].bind('layoutStart', onLayoutStart);

		_callbacks.bindBrowserEvent 	= bindBrowserEvent;
		_callbacks.refreshSigma 		= refreshSigma;
		_callbacks.filterNodes 			= onFilterNodes;
		_callbacks.undo					= onUndo;

		_callbacks.api.dataChanged 		= onDataChanged;
		_callbacks.api.doLayout 		= doLayout;
		_callbacks.api.refresh 			= refreshSigma;
		_callbacks.api.start 			= start;
		_callbacks.api.status 			= status;
		_callbacks.api.reset 			= reset;

		function init(){
			if(Graphbrowser.Modules){
				$.each(Graphbrowser.Modules, function(i, module){
					var newModule = new module(_s, _callbacks);
					newModule.init(_moduleSettings[newModule.name]);
					_attachedModules[newModule.name] = newModule;
				});
			}
		};

		function bindBrowserEvent(event, handler){
			_browserEventHandlers[event] = _browserEventHandlers[event] || [];
			_browserEventHandlers[event].push(handler);
		};

		function kill(){
			_eventEmitter[_s.id].unbind('dataChanged', refreshSigma);
			_eventEmitter[_s.id].unbind('prepareSaveGraph', onSave);
			_eventEmitter[_s.id].unbind('restoreGraph', onRestore);
			_eventEmitter[_s.id].unbind('pickNodes', onPickNodes);
			//_eventEmitter[_s.id].dispatchEvent('reset');
			_eventEmitter[_s.id].dispatchEvent('kill');
			window.clearTimeout(_refreshTimeout);
			for(var m = 0; m < _attachedModules.length; m++){
				_attachedModules[m] = undefined;
			}
		};

		function onDataChanged(){
			_eventEmitter[_s.id].dispatchEvent('dataChanged');
		};

		function start(settings){
			$.each(_browserEventHandlers['start'], function(i, func){
				func(settings);
			});
			refreshSigma();
		};

		function refreshSigma(){
			_s.refresh({skipIndexation: false});
		};

		function onPickNodes(event){
			var nodesToSelect = event.data.nodesToSelect;
			var onNodesSelected = event.data.onNodesSelected;
			var parentNode = event.data.parentNode;
			var parentTitle = event.data.parentTitle;

			var noNodePicker = true;
			var eventhandlers = _browserEventHandlers['pickNodes'];

			if(!eventhandlers){
				onNodesSelected(nodesToSelect, parentNode);
				return;
			}

			$.each(eventhandlers, function(i, func){
				func(nodesToSelect, onNodesSelected, parentNode, parentTitle);
			});
		};

		function onFilterNodes(nodesToFilter, inOrOut){
			var eventhandlers = _browserEventHandlers['filterNodes'];

			$.each(eventhandlers, function(i, func){
				nodesToFilter = func(nodesToFilter, inOrOut);
			});

			return nodesToFilter;
		};

		function onUndo(){
			var eventhandlers = _browserEventHandlers['undo'];

			if(!eventhandlers) return;

			$.each(eventhandlers, function(i, func){
				func();
			});
		};

		function doLayout(layout, conf){
			var eventhandlers = _browserEventHandlers['doLayout'];

			if(!eventhandlers) return;

			$.each(eventhandlers, function(i, func){
				func(layout, conf);
			});
		};

		function onLayoutStart(){
			var eventhandlers = _browserEventHandlers['layoutStart'];

			if(!eventhandlers) return;

			$.each(eventhandlers, function(i, func){
				func();
			});
		};

		function onSave(event){
			var nameOfView = event.data.saveAsName;
			var eventhandlers = _browserEventHandlers['saveGraph'];

			if(!eventhandlers)
				eventhandlers = [];

			var data = {name: nameOfView};

			$.each(eventhandlers, function(i, func){
				var moduleData = func();
				data[moduleData.name] = moduleData.data;
			});

			_eventEmitter[_s.id].dispatchEvent('saveGraph', data);
		};

		function onRestore(event){
			var data = event.data;
			var eventhandlers = _browserEventHandlers['restoreGraph'];

			if(!eventhandlers) return;

			$.each(eventhandlers, function(i, func){
				func(data);
			});
		};

		function status(){
			var status = {};
			var eventhandlers = _browserEventHandlers['status'];
			$.each(eventhandlers, function(i, func){
				status[module.name] = func();
			});

			return status;
		};

		function reset(){
			var eventhandlers = _browserEventHandlers['reset'];

			if(!eventhandlers) return;

			$.each(eventhandlers, function(i, func){
				func();
			});
		};

		//export the functions
		this.init = init;
		this.kill = kill;
	};

})();
;(function(undefined) {
	'use strict';

	Graphbrowser = Graphbrowser || {};

	var GraphBrowser = function(conf){
		var _s,
			_controller = {},
			_callbackMethods = {};

		if(conf === undefined)
			throw new Error("Graph-Browser: No settings specified!");

		if(conf.sigmaSettings === undefined)
			console.log("Graph-Browser: No settings for Sigma.js found. Starting with default.");

		if(conf.moduleSettings === undefined)
			throw new Error("Graph-Browser: Settings for Graph-Browser-Modules are missing!");

		_callbackMethods.api = _callbackMethods.api || {};

		// create and initialize the sigma controller.
		_controller.sigmaControl = new Graphbrowser.Control.SigmaControl(_callbackMethods, conf);
		_s = _controller.sigmaControl.init();

		// create and initialize the connection module for communication with Structr
		_controller.connectionControl = new Graphbrowser.Control.ConnectionControl(_callbackMethods);
		_controller.connectionControl.init();

		// create and initialize the module controller
		_controller.ModuleControl = new Graphbrowser.Control.ModuleControl(_callbackMethods, conf.moduleSettings, _s);
		_controller.ModuleControl.init();


		// append the api functions of eacht module to the graph browser object.
		for(var o in _callbackMethods.api){
			this[o] = _callbackMethods.api[o];
		}

		function kill(){
			_controller.ModuleControl.kill();
			_controller.sigmaControl.kill();
			delete _controller.sigmaControl;
			delete _controller.ModuleControl;
		};

		this.kill = kill;
	};

	this.GraphBrowser = GraphBrowser;
}).call(this);
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	Graphbrowser.Modules.CurrentNodeTypes = function (sigmaInstance, callbacks){
		var _s = sigmaInstance,
			_callbacks = callbacks,
			_name = "currentNodeTypes",
			_apiMethods = {},
			_classes = '',
			_noView = true,
			_nodeTypes = [],
			_hiddenTypes = [],
			_relNames = [];

		function addToApi(name, func){
			_apiMethods = _apiMethods || {};
			_apiMethods[name] = func;
		};

		function init(settings){
			if(settings){
				settings.container !== undefined ? createView(settings.container) :  _noView = true;
				settings.classes !== undefined ? _classes = settings.classes : _classes = '';

				_callbacks.eventEmitter[_s.id].bind('dataChanged', onDataChanged);
				_callbacks.eventEmitter[_s.id].bind('start', start);
			}
			else{
				_noView = true;
			}

			for(var m in _apiMethods){
				_callbacks.api[m] = _apiMethods[m];
			}
		};

		function start(){
			onDataChanged();
		};

		function createView(container){
			_noView = false;
			if(container.charAt(0) !== '#'){
				container = '#' + container;
			}
			$(container).append('<div id="currentNodeTypes-displayTypes" class="currentNodeTypes-displayTypes"></div>');
		};

		function onDataChanged(event){
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
				nodeTypeBox.append('<a id="currentNodeTypes-nodeType-btn-' + nodeType + '" class="currentNodeTypesButton ' + _classes + '" style="background-color: ' + _Graph.color[nodeType] + '">' + nodeType + '</a>');
				var newButton = $('#currentNodeTypes-nodeType-btn-' + nodeType);

				//if the nodes with the type are hidden, change the button accordingly
				if(_hiddenTypes.indexOf(nodeType) > -1){
					newButton.css('text-decoration', 'line-through');
					_callbacks.eventEmitter[_s.id].dispatchEvent('hideNodeType', {nodeType: nodeType, value: true});
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
					_callbacks.eventEmitter[_s.id].dispatchEvent('hideNodeType', {nodeType: nodeType});
				}).on('mouseover', function(){
					highlightNodeType(nodeType);
				}).on('mouseout', function(){
					unhighlightNodeType(nodeType);
				});
			});
		};

		function getCurrentNodeTypes(){
			return _nodeTypes;
		};
		addToApi('getCurrentNodeTypes', getCurrentNodeTypes);

		function getCurrentRelTypes(){
			return _relNames;
		};
		addToApi('getCurrentRelTypes', getCurrentRelTypes);

		function highlightNodeType(nodeType){
			var nodesForHalo = [];
			var colorForHalo = null;

			_s.graph.nodes().forEach(function(node) {
				if (node.nodeType === nodeType) {
					nodesForHalo.push(node);
					colorForHalo = colorLuminance(node.color, -.1);
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
		addToApi('highlightNodeType', highlightNodeType);

		function unhighlightNodeType(nodeType){
			_s.renderers[0].unbind('render');
			_s.refresh({skipIndexation: true});
		};
		addToApi('unhighlightNodeType', unhighlightNodeType);

		function highlightRelType(relType){
			_s.graph.edges().forEach(function(edge) {
				if(edge.relName === relType){
					edge.active_color = '#81ce25';
					edge.active = true;
				}
			});
			_s.refresh({skipIndexation: true});
		};
		addToApi('highlightRelType', highlightRelType);

		function unhighlightRelType(relType){
			_s.graph.edges().forEach(function(edge) {
				if(edge.relName === relType){
					edge.active_color = _s.settings('defaultEdgeActiveColor');
					edge.active = false;
				}
			});
			_s.refresh({skipIndexation: true});
		};
		addToApi('unhighlightRelType', unhighlightRelType);

		function colorLuminance(hex, lum){
			hex = String(hex).replace(/[^0-9a-f]/gi, '');
			lum = lum || 0;
			if (hex.length < 6) {
				hex = hex[0] + hex[0] + hex[1] + hex[1] + hex[2] + hex[2];
			}

			var r = convertToDec(hex.substr(0, 2)), g = convertToDec(hex.substr(2, 2)), b = convertToDec(hex.substr(4, 2));

			var rgb = desaturate(r, g, b, .5);

			var newHex = "#", c, i;
			for (i = 0; i < 3; i++) {
				c = rgb[i];
				c = Math.round(Math.min(Math.max(0, c + (c * lum)), 255)).toString(16);
				newHex += ("00" + c).substr(c.length);
			}
			return newHex;
		};

		function convertToDec(hex){
			return parseInt(hex, 16);
		};

		function desaturate(r, g, b, k){
			var intensity = 0.3 * r + 0.59 * g + 0.11 * b;
			r = Math.floor(intensity * k + r * (1 - k));
			g = Math.floor(intensity * k + g * (1 - k));
			b = Math.floor(intensity * k + b * (1 - k));
			return [r, g, b];
		};

		this.name = _name;
		this.init = init;
		this.start = start;
	};
})();
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	Graphbrowser.Modules.Exporter = function (sigmaInstance, callbacks){
		var _name = "exporter",
			_s = sigmaInstance,
			_callbacks = callbacks,
			_callbackOnSuccess = undefined,
			_callbackOnError = undefined;

		function init(settings){
			if(!settings)
				return;
			else{
				(settings.onSuccess !== undefined && typeof settings.onSuccess === 'function' ) ? _callbackOnSuccess = settings.onSuccess : _callbackOnSuccess = undefined;
				(settings.onError !== undefined && typeof settings.onError === 'function') ? _callbackOnError = settings.onError : _callbackOnError = undefined;

				_callbacks.eventEmitter[_s.id].bind('saveGraph', onSave);
			}

			$(document).on('click', '#exporter-saveGraph-form-submit',  function(){
				triggerSave(true);
			});
		};

		function exportJson(){
			var _nodeAttributes = ['fr', 'fr_x', 'fr_y'];
			var _edgeAttributes = ['color'];
			var jsonString = _s.toJSON({removeNodeAttributes: _nodeAttributes, removeEdgeAttributes: _edgeAttributes});
		};

		function exportGefx(){
			var gexfString = _s.toGEXF({
				download: false,
				nodeAttributes: 'data',
				edgeAttributes: 'data',
				renderer: _s.renderers[0],
				creator: 'OP',
			});
		};

		function triggerSave(nameEntered){
			var name = $('#exporter-saveGraph-form-name').val();
			if(name && name.length !== 0){
				_callbacks.eventEmitter[_s.id].dispatchEvent('prepareSaveGraph', {saveAsName: name});
			}
			else{
				if(_callbackOnError !== undefined)
					_callbackOnError();
			}
		};

		function onSave(event){
			var data = event.data;
			if(!data)
				data = {};
			data['mode'] = "all";
			var _nodeAttributes = ['fr', 'fr_x', 'fr_y'];
			var _edgeAttributes = ['color'];

			var jsonGraphString = _s.toJSON({removeNodeAttributes: _nodeAttributes, removeEdgeAttributes: _edgeAttributes});
			var jsonDataString = JSON.stringify(data);
			_callbacks.conn.saveGraphView(data.name, jsonGraphString, jsonDataString, currentViewId, onGraphSaved, false);
		};

		function onGraphSaved(){
			if(_callbackOnSuccess !== undefined)
				_callbackOnSuccess();
		};

		this.name = _name;
		this.init = init;
		this.onSave = onSave;
	};
})();
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

var animating = undefined;

(function() {
	'use strict';

	Graphbrowser.Modules.layouts = function(sigmaInstance, callbacks){
		var _name = 'layouts',
			_callbacks = callbacks,
			_s = sigmaInstance,
			_FRtimeout = 0,

			_FRConfig = undefined,
			_FA2Config = undefined,
			_dagreConfig = undefined,

			_FRListener = undefined,
			_dagreListener = undefined;

		function init(){
			_callbacks.eventEmitter[_s.id].bind('kill', onKill);
			_callbacks.bindBrowserEvent('doLayout', doLayout);

			_callbacks.api.stopForceAtlas2 = stopFA2;
			_callbacks.api.startForceAtlas2 = startFA2;

			_FRConfig = {
				autoArea: false,
				area: 1000000000,
				gravity: 0,
				speed: 0.1,
				iterations: 1500,
				easing: 'quadraticInOut',
				duration: 800
			};

			_FA2Config = {
				linLogMode: false,
				worker: true,
				outboundAttractionDistribution: false,
				adjustSizes: true,
				edgeWeightInfluence: 0,
				scalingRatio: 1,
				strongGravityMode: false,
				gravity: 1,
				barnesHutOptimize: true,
				barnesHutTheta: 0.5,
				slowDown: 1,
				startingIterations: 1,
				iterationsPerRender: 1
			};

			_dagreConfig = {
				directed: true,
				multigraph: true,
				compound: true,
				rankDir: 'TB',
				easing: _s.settings('animationsTime'),
				duration: _s.settings('animationsTime')
			}
		};

		function onKill(){
			if(_FRListener)
				_FRListener.unbind('stop');
			if(_dagreListener)
				_dagreListener.unbind('stop');

			_s.killForceAtlas2();
		}

		function doLayout(layout, config){
			if(animating === true)
				return;
			switch(layout){
				case 'fruchtermanReingold':
					startFR(config);
					break;
				case 'forceAtlas2':
					startFA2(config);
					break;
				case 'dagre':
					startDagre(config);
					break;
			}
		};

		function startFR(conf){
			var config = conf || _FRConfig;
			var _FRListener = sigma.layouts.fruchtermanReingold.configure(_s, config);

			_FRListener.bind('stop', function(event) {
				window.clearTimeout(_FRtimeout);
				_FRtimeout = window.setTimeout(function() {
					if (_FRtimeout) {
						_FRtimeout = 0;
						_callbacks.eventEmitter[_s.id].dispatchEvent('dataChanged');
						animating = false;
					}
				}, 50);
			});

			animating = true;
			_callbacks.eventEmitter[_s.id].dispatchEvent('layoutStart');
			sigma.layouts.fruchtermanReingold.start(_s);
		};

		function startFA2(conf){
			animating = true;
			if(_s.isForceAtlas2Running()){
				stopFA2();
				return;
			}
			var config = conf || _FA2Config;
			_callbacks.eventEmitter[_s.id].dispatchEvent('layoutStart');
			_s.startForceAtlas2(config);
		};

		function stopFA2(){
			_s.stopForceAtlas2();
			_s.killForceAtlas2();
			animating = false;
		};

		function startDagre(config){
			animating = true;
			var conf = config || _dagreConfig;
			_dagreListener = sigma.layouts.dagre.configure(_s, conf);

			_dagreListener.bind('stop', function(event) {
				_callbacks.eventEmitter[_s.id].dispatchEvent('dataChanged');
				animating = false;
			});

			_callbacks.eventEmitter[_s.id].dispatchEvent('layoutStart');
			sigma.layouts.dagre.start(_s);
		};

		this.name = _name;
		this.init = init;
	};
})();
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	Graphbrowser.Modules.Importer = function (sigmaInstance, callbacks){
		var _name = "importer",
			_s = sigmaInstance,
			_callbacks = callbacks,
			_mode = undefined,
			_callbackOnSuccess = undefined,
			_callbackOnError = undefined,
			_loadedNodes = [],
			_loadedEdges = [];

		function init(settings){
			if(!settings)
				return;

			else{
				(settings.onSuccess !== undefined && typeof settings.onSuccess === 'function') ? _callbackOnSuccess = settings.onSuccess : _callbackOnSuccess = undefined;
				(settings.onError !== undefined && typeof settings.onError === 'function') ? _callbackOnError = settings.onError : _callbackOnError = undefined;
				$(document).on('click', '#importer-controlElement', function(){
					loadGraphviewsForCurrent();
				});

				$(document).on('click', '#importer-loadGraph-submit', function(){
					_mode = "all";
					loadSelectedView();
				});
			}
		};

		function loadGraphviewsForCurrent(){
			_callbacks.conn.getGraphViews(currentViewId, "graphBrowserInfo", pickGraphView);
		};

		function pickGraphView(resultData){
			var radioButtons = '';

			if(resultData.length <= 0){
				radioButtons += '<div><span>Keine Ansichten verfügbar</span></div>'
			}

			$.each(resultData, function(i, r){
				radioButtons = radioButtons + '<div class="radiobutton importer-loadGraph-radiobuttons"><label><input type="radio"  name="viewButtons" value="' + r.id +  '">' + r.name + '</label><div>';
			});

			$('#importer-loadGraph-graphNames').append(radioButtons);
			radioButtons = "";
		};

		function loadSelectedView(){
			var selectedView = $("#importer-loadGraph-graphNames input[type='radio']:checked").val();
			if(selectedView !== undefined){
				_callbacks.conn.getGraphViewData(selectedView, "graphBrowser", restoreGraph);
			}
		};

		function restoreGraph(result){
			var graph = JSON.parse(result.graph);
			var data = JSON.parse(result.data);
			_s.graph.clear();
			_s.graph.read(graph);
			_callbacks.eventEmitter[_s.id].dispatchEvent('restoreGraph', data);
			_callbacks.eventEmitter[_s.id].dispatchEvent('dataChanged');
			_s.refresh();
			_callbackOnSuccess();
		};

		this.name = _name;
		this.init = init;
		this.restoreGraph = restoreGraph;
	};

})();
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	Graphbrowser.Modules.NewNodesPicker = function(sigmaInstance, callbacks){
		var _name = 'newNodePicker',
			_s = sigmaInstance,
			_callbacks = callbacks,
			_onFinished = undefined,
			_nodeList = undefined,
			_parentNode = undefined,
			_parentTitle = undefined,
			_callbackOnFinished = undefined,
			_callbackOnChooseNodes = undefined;

		function init(settings){
			if(settings){
				(settings.onFinished !== undefined && typeof settings.onFinished === 'function') ? _callbackOnFinished = settings.onFinished : _callbackOnFinished = undefined;
				(settings.onChooseNodes !== undefined && typeof settings.onChooseNodes === 'function') ? _callbackOnChooseNodes = settings.onChooseNodes : _callbackOnChooseNodes = undefined;

				$(document).on('click', '#newnodepicker-chooseNodes-markAllSelected', function(){
					onMarkAllSelected();
				});

				$(document).on('click', '#newnodepicker-chooseNodes-reverseSelection', function(){
					onReverseSelection();
				});

				$(document).on('click', '#newnodepicker-chooseNodes-submit', function(){
					onSelected();
				});

				_callbacks.bindBrowserEvent('pickNodes', pickNodes);
			}
		};

		function pickNodes(nodeList, onFinished, parentNode, parentTitle){
			if(nodeList.length <= 0){
				return;
			}

			_nodeList = nodeList;
			_onFinished = onFinished;
			_parentNode = parentNode;
			_parentTitle = parentTitle;
			var nodeTypes = [];

			if(parentTitle){
				$('#newnodepicker-chooseNodes-parentNodeTitle').replaceWith('<h3 id="newnodepicker-chooseNodes-parentNodeTitle" class="newnodepicker-chooseNodes-parentNodeTitle">' + parentTitle + '</h3>');
			}

			$('#newnodepicker-chooseNodes-items').empty();

			$.each(_nodeList, function(i, node){
				if(nodeTypes.indexOf(node.nodeType) < 0){
					nodeTypes.push(node.nodeType);
					$('#newnodepicker-chooseNodes-items').append('<div id="newnodepicker-chooseNodes-itemType-'+ node.nodeType + '" class="newnodepicker-chooseNodes-itemType newnodepicker-chooseNodes-itemType-'+ node.nodeType + '">'
					 + '<h4 id="newnodepicker-chooseNodes-itemType-header-'+ node.nodeType + '" class="newnodepicker-chooseNodes-itemType-header newnodepicker-chooseNodes-itemType-header-' + node.nodeType + '">' + node.nodeType + '</h4></div>'
					);

					$('#newnodepicker-chooseNodes-itemType-header-' + node.nodeType).on('click', function(){
						onMarkTypeSelected(node.nodeType);
					});
				}
				var alreadyInTheList = false;
				$("input[name='selectedNodeItems[]']").each(function (){
					if($(this).val() === node.id){
						alreadyInTheList = true;
					}
				});

				if(alreadyInTheList)
					return true;

				else if(_s.graph.nodes(node.id)){
					$('#newnodepicker-chooseNodes-itemType-'+ node.nodeType).append('<div class="checkbox newnodepicker-chooseNodes-item newnodepicker-chooseNodes-item-'+ node.nodeType + '"><label class="newnodepicker-chooseNodes-item-label"><input type="checkbox" disabled name="selectedNodeItems[]" value="' + node.nodeType + '.' + node.id +  '">' + node.label + '</label></div>');
				}

				else if(!alreadyInTheList && !_s.graph.nodes(node.id)){
					$('#newnodepicker-chooseNodes-itemType-'+ node.nodeType).append('<div class="checkbox newnodepicker-chooseNodes-item newnodepicker-chooseNodes-item-'+ node.nodeType + '"><label><input type="checkbox" name="selectedNodeItems[]" value="' + node.nodeType + '.' + node.id +  '">' + node.label + '</label></div>');
				}
			});

			if(_callbackOnChooseNodes != undefined)
				_callbackOnChooseNodes(_parentNode, _parentTitle);
		};

		function onSelected(){
			$("input[name='selectedNodeItems[]']:not(:checked)").each(function (){
				var val = $(this).val().split('.');
				_nodeList = _nodeList.filter(function (el) {
					return el.id !== val[1];
				});
			});

			if(_callbackOnFinished !== undefined)
				_callbackOnFinished(_parentNode, _parentTitle);
			if(_onFinished !== undefined)
				_onFinished(_nodeList, _parentNode);
			clear();
		};

		function onMarkAllSelected(){
			$("input[name='selectedNodeItems[]']").each(function (){
				if($(this).prop('disabled') === true)
					return true;
				$(this).prop('checked', true);
			});
		};

		function onMarkTypeSelected(type){
			$("input[name='selectedNodeItems[]']").each(function (){
				var val = $(this).val().split('.');
				if($(this).prop('disabled') === true)
					return true;
				if(val[0] === type){
					$(this).prop('checked', !$(this).prop('checked'));
				}
			});
		};

		function onReverseSelection(){
			$("input[name='selectedNodeItems[]']").each(function (){
				if($(this).prop('disabled') === true)
					return true;
				$(this).prop('checked', !$(this).prop('checked'));
			});
		};

		function clear(){
			_onFinished = undefined;
			_nodeList = undefined;
			_parentNode = undefined;
			_parentTitle = undefined;
		};

		this.name = _name;
		this.init = init;
	};
})();
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	Graphbrowser.Modules.NodeExpander = function(sigmaInstance, callbacks){
		var _name = 'nodeExpander',
			_s = sigmaInstance,
			_callbacks = callbacks,
			_infoContainer,
			_marginX,
			_marginY,
			_onAddNodes,
			_onNodesAdded,
			_onNodesRemoved,
			_currentEvent,
			_timeout,
			_view,
			_newNodeSize,
			_newNodeLabelProperty,
			_edgeType,
			_newEdgeLabelProperty,
			_newEdgeSize,
			_defaultInfoButtonColor,
			_expandButtonsTimeout,
			_infoButtonRenderer,
			_expanded = {},
			_hoverMap = {},
			_hoverMapPreventDuplicates = {},
			_active = false,
			_inNodesLoaded = false,
			_outNodesLoaded = false,
			_showHoverInfo = true,
			_newNodes = [],
			_newEdges = [],
			_undoStack = [],
			_apiMethods = {};

		function addToApi(name, func){
			_apiMethods = _apiMethods || {};
			_apiMethods[name] = func;
		}

		function init(settings){
			if(settings){
				settings.container !== undefined ? _infoContainer = settings.container : _infoContainer = '';

				settings.margins = settings.margins || {};
				settings.margins.left !== undefined ? _marginX = settings.margins.left : _marginX = 0;
				settings.margins.top !== undefined ? _marginY = settings.margins.top : _marginY = 0;

				settings.infoButtons = settings.infoButtons || {};
				settings.infoButtonRenderer !== undefined ? _infoButtonRenderer = settings.infoButtonRenderer : _infoButtonRenderer = defaultInfoButtonRenderer;
				settings.defaultInfoButtonColor !== undefined ? _defaultInfoButtonColor = settings.defaultInfoButtonColor : _defaultInfoButtonColor = '#fff';

				settings.onAddNodes !== undefined ? _onAddNodes = settings.onAddNodes : _onAddNodes = undefined;
				settings.onNodesAdded !== undefined ? _onNodesAdded = settings.onNodesAdded : _onNodesAdded = undefined;
				settings.onNodesRemoved !== undefined ? _onNodesRemoved = settings.onNodesRemoved : _onNodesRemoved = undefined;
				settings.newNodeSize !== undefined ? _newNodeSize = settings.newNodeSize : _newNodeSize = 10;
				settings.newNodeLabelProperty !== undefined ? _newNodeLabelProperty = settings.newNodeLabelProperty : _newNodeLabelProperty = "name";
				settings.newEdgeSize !== undefined ? _newEdgeSize = settings.newEdgeSize : _newEdgeSize = 10;
				settings.newEdgeLabelProperty !== undefined ? _newEdgeLabelProperty = settings.newEdgeLabelProperty : _newEdgeLabelProperty = "relType";
				settings.edgeType !== undefined ? _edgeType = settings.edgeType : _edgeType = "arrow";
				settings.expandButtonsTimeout !== undefined ? _expandButtonsTimeout = settings.expandButtonsTimeout : _expandButtonsTimeout = 1000;
				settings.restView !== undefined ? _view = settings.restView : _view = "_graph";

				bindEvents();

				if(_infoContainer.charAt(0) !== '#')
					_infoContainer = '#' + _infoContainer;
			}

			for(var m in _apiMethods){
				_callbacks.api[m] = _apiMethods[m];
			}
		};

		function bindEvents(){
			_callbacks.sigmaCtrl.bindSigmaEvent('clickNode', handleClickNodeEvent);
			_callbacks.sigmaCtrl.bindSigmaEvent('doubleClickNode', handleDoubleClickNodeEvent);
			_callbacks.sigmaCtrl.bindSigmaEvent('hovers', handleHoversEvent);

			_callbacks.bindBrowserEvent('restoreGraph', onRestore);
			_callbacks.bindBrowserEvent('saveGraph', onSave);
			_callbacks.bindBrowserEvent('reset', onReset);
			_callbacks.bindBrowserEvent('undo', onUndo);

			_callbacks.eventEmitter[_s.id].bind('dataChanged', onDataChanged);

		};

		function unbindEvents(){
			_callbacks.sigmaCtrl.unbindSigmaEvent('clickNode', handleClickNodeEvent);
			_callbacks.sigmaCtrl.unbindSigmaEvent('doubleClickNode', handleDoubleClickNodeEvent);
			_callbacks.sigmaCtrl.unbindSigmaEvent('hovers', handleHoversEvent);
		};

		function handleClickNodeEvent(clickedNode){
			var node = clickedNode.data.node;
		};

		function handleDoubleClickNodeEvent(clickedNode){
			var node = clickedNode.data.node;
			var id = node.id;
			$('#graph-info').empty();
			if (_expanded[id] && _expanded[id].state === 'expanded') {
				collapseNode(node);
				_callbacks.eventEmitter[_s.id].dispatchEvent('dataChanged', {source: _name});

				if(_onNodesRemoved !== undefined && typeof _onNodesRemoved === 'function')
					_onNodesRemoved();
			} else {
				onExpandNode(node);
			}
		};

		function handleHoversEvent(e){
			if(e.data.enter.nodes.length > 0){
				handleOverNodeEvent(e.data.enter.nodes[0]);
			}

			else if(e.data.leave.nodes.length > 0){
				handleOutNodeEvent(e.data.leave.nodes[0]);
			}
		};

		function handleOverNodeEvent(node){
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
			var inNodes = _callbacks.conn.getNodes(node, 'in', onNewInNodesLoaded, _view);
			var outNodes = _callbacks.conn.getNodes(node, 'out', onNewOutNodesLoaded, _view);
		};

		function handleOutNodeEvent(node){
			if (!_timeout) {
				_timeout = window.setTimeout(function() {
					if (_timeout) {
						_timeout = 0;
						$(_infoContainer).empty();
						_active = false;
					}
				}, _expandButtonsTimeout);
			}
		};

		function onNewOutNodesLoaded(result, node){
			_outNodesLoaded = true;
			var preResult = result;
			result = _callbacks.filterNodes(result, 'out');
			$.each(result, function(i, result){
				if (result.targetNode && !_s.graph.nodes(result.targetNode.id) && !_hoverMapPreventDuplicates[result.targetNode.id]) {
					_hoverMapPreventDuplicates[result.targetNode.id] = true;
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
				updateHoverInfo(node, _hoverMap);
			}
		};

		function onNewInNodesLoaded(result, node){
			_inNodesLoaded = true;
			var preResult = result;
			result = _callbacks.filterNodes(result, 'in');
			$.each(result, function(i, result){
				if(!_s.graph.nodes(result.sourceNode.id) && result.sourceNode && !_hoverMapPreventDuplicates[result.sourceNode.id]){
					_hoverMapPreventDuplicates[result.sourceNode.id] = true;
					if (_hoverMap[result.sourceNode.type]){
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
				updateHoverInfo(node, _hoverMap);
			}
		};

		function updateHoverInfo(node, hoverMap){
			_active = false;
			var rendererId = _s.renderers[0].conradId;
			var graphInfo  = $(_infoContainer);
			var num        = Object.keys(hoverMap).length;
			var i          = 0;
			var size       = node['renderer' + rendererId + ':size'];
			var radius     = Math.max(size, 40);
			var x          = Math.floor(node['renderer' + rendererId + ':x']);
			var y          = node['renderer' + rendererId + ':y'];
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
				graphInfo.append(_infoButtonRenderer(key, label));
				var button = $('#' + key + '-button');
				button.css('border', 'none');
				button.css('background-color', _Graph.color[key]);
				button.css('color', '#000');
				button.css('top', (y + dy + _marginY) + 'px');
				button.css('left', (x + dx + _marginX) + 'px');
				button.on('click', function(e) {
					graphInfo.empty();
					onExpandNode(node, key);
					_active = false;
				});
				i++;
			});
			_hoverMapPreventDuplicates = {};
		};

		function collapseNode(node){
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
						collapseNode(_s.graph.nodes(n));
						try {
							_s.graph.dropNode(n);
						} catch(x) {}
					});
					_expanded[id].state = 'collapsed';
				}
			}
			_s.refresh();
		};
		addToApi('collapseNode', collapseNode)

		function onExpandNode(node, type){
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
					addOutNode(sigmaNodes, sigmaEdges, result, x, y);
				}
			});

			$.each(_newNodes['inNodes'], function(i, result) {
				//if ((!type || result.sourceNode.type === type) && !_s.graph.nodes(result.sourceNode.id)) {
				if ((!type || result.sourceNode.type === type)) {
					addInNode(sigmaNodes, sigmaEdges, result, x, y);
				}
			});

			pickNodes(node, sigmaNodes, sigmaEdges);
			_showHoverInfo = true;
		};

		function onNodesSelected(selectedNodes, parentNode){
			if(!selectedNodes || selectedNodes.length <= 0){
				return;
			}

			_expanded[parentNode] = {
				state: 'expanded',
				id: 	parentNode,
				nodes: _expanded[parentNode] ? _expanded[parentNode].nodes : [],
				edges: _expanded[parentNode] ? _expanded[parentNode].edges : []
			};

			addNodesToGraph(parentNode, selectedNodes, _newEdges);
		};

		function pickNodes(current, nodes, edges){
			_newNodes = [];
			_newNodes = $.extend(_newNodes, nodes);
			_newEdges = $.extend(_newEdges, edges);
			_callbacks.eventEmitter[_s.id].dispatchEvent('pickNodes',
				{
					nodesToSelect: _newNodes,
					onNodesSelected: onNodesSelected,
					parentNode: current.id,
					parentTitle: current.label
				});
		};

		function addOutNode(nodes, edges, result, x, y){
			var sourceNode = result.sourceNode;
			var targetNode = result.targetNode;
			var angle = Math.random()* Math.PI * 2;
			var ratio = _s.camera.ratio;
			var newX = 0;
	        var newY = 0;

	        if(ratio > 1){
	            newX = x + (Math.cos(angle) * (Math.abs(x) / (2*ratio)));
	            newY = y + (Math.sin(angle) * (Math.abs(y) / (2*ratio)));
	        }

	        if(ratio <= 1){
	            newX = x + (Math.cos(angle) * (Math.abs(x) / (1/ratio)));
	            newY = y + (Math.sin(angle) * (Math.abs(y) / (1/ratio)));
	        }
			var size = _newNodeSize;

			nodes.push({
				id: targetNode.id,
				label: targetNode[_newNodeLabelProperty],
				size: size,
				x: newX,
				y: newY,
				color: _Graph.color[targetNode.type],
				nodeType: targetNode.type
			});

			edges.push({
				id: result.id,
				source: sourceNode.id,
				target: targetNode.id,
				size: _newEdgeSize,
				relType: result.type,
				label: result[_newEdgeLabelProperty],
				type: _edgeType,
				color: _s.settings('defaultEdgeColor')
			});
		};

		function addInNode(nodes, edges, result, x, y){
			var sourceNode = result.sourceNode;
			var targetNode = result.targetNode;
			var angle = Math.random()* Math.PI * 2;
			var ratio = _s.camera.ratio;
			var newX = 0;
			var newY = 0;

	        if(ratio > 1){
	            newX = x + (Math.cos(angle) * (Math.abs(x) / (2*ratio)));
	            newY = y + (Math.sin(angle) * (Math.abs(y) / (2*ratio)));
	        }

	        if(ratio <= 1){
	            newX = x + (Math.cos(angle) * (Math.abs(x) / (1/ratio)));
	            newY = y + (Math.sin(angle) * (Math.abs(y) / (1/ratio)));
	        }
			var size = _newNodeSize;

			nodes.push({id: sourceNode.id,
				label: sourceNode[_newNodeLabelProperty],
				size: size,
				x: newX,
				y: newY,
				color: _Graph.color[sourceNode.type],
				nodeType: sourceNode.type
			});

			edges.push({
				id: result.id,
				source: sourceNode.id,
				target: targetNode.id,
				size: _newEdgeSize,
				relType: result.type,
				label: result[_newEdgeLabelProperty],
				type: _edgeType,
				color: _s.settings('defaultEdgeColor')
			});
		};

		function addEdge(result){
			var sourceNode = result.sourceNode;
			var targetNode = result.targetNode;
			var id = result.id;
			edges[result.id]     = {
				id: result.id,
				source: sourceNode.id,
				target: targetNode.id,
				size: _newEdgeSize,
				relType: result.type,
				label: result.relType,
				type: _edgeType,
				color: _s.settings('defaultEdgeColor')
			};
		};

		function addNodesToGraph(current, nodes, edges){
			var added = 0;
			var add = true;

			if(_onAddNodes !== undefined && typeof _onAddNodes === 'function') {
				add = _onAddNodes(current, nodes, edges);
				if(!add) {
					return;
				}
			}

			$.each(nodes, function(i, node){
				try {
	                _callbacks.sigmaCtrl.addNode({
	                	id: node.id,
	                	label: node.label,
	                	size: node.size,
	                	x: node.x,
	                	y: node.y,
	                	color: node.color,
	                	nodeType: node.nodeType
	                });
	                // only add expanded node if addNode() is successful => node did not exist before
	                _expanded[current].nodes.push(node.id);
	                added++;
	            } catch (e) {}
	        });

			$.each(edges, function(i, edge){
				var existingEdges = _callbacks.api.findRelationships(edge.source, edge.target);
				var c = existingEdges.length * 10;
				try {
					 _callbacks.sigmaCtrl.addEdge({
						id: edge.id,
						source: edge.source,
						target: edge.target,
						label: edge.label,
						type: edge.type,
						color: edge.color,
						relType: edge.relType,
						relName: edge.label,
						size: edge.size,
						count: c
					});
					// only add expanded node if addNode() is successful => node did not exist before
					_expanded[current].edges.push(edge.id);
					added++;
				} catch (e) {}
	        });

			_newNodes = [];
			_newEdges = [];

			sigma.canvas.edges.autoCurve(_s);

			_callbacks.eventEmitter[_s.id].dispatchEvent('dataChanged', {source: _name});

			if(_onNodesAdded !== undefined && typeof _onNodesAdded === 'function')
				_onNodesAdded();
		};

		function onUndo(){
			if(_undoStack.length < 1){
				return;
			}

			_undoStack.splice(_undoStack.length -1, 1);
			_expanded = _undoStack[_undoStack.length - 1];

			if(!_expanded){
				_expanded = {};
			}
		};

		function onDataChanged(event){
			var changeType = event.data.source;
			if(changeType === 'undo'){
				return;
			}
			if(changeType === _name ){
				var exp = jQuery.extend({}, _expanded);
				_undoStack.push(exp);
			}
		};

		function onSave(){
			var status = JSON.stringify(_expanded);
			return {name: _name, data: status};
		};

		function onRestore(data){
			var oldData = data[_name];
			if(!oldData)
				return;
			var expanded = JSON.parse(oldData);
			_expanded = expanded;
		};

		function onReset(){
			_undoStack = [];
			Object.keys(_expanded).forEach(function(key){
				delete _expanded[key]; }
			);
		};

		//expands node. Delay is for waiting for the loading of the nodes... This has to be done with promises soon!
		function expandNode(id, delay){
//			var self = this;
			delay = delay || 20;
			var node = _s.graph.nodes(id);
			_showHoverInfo = false;

//			var inNodes = _callbacks.conn.getNodes(node, 'in', onNewInNodesLoaded, _view);
//			var outNodes = _callbacks.conn.getNodes(node, 'out', onNewOutNodesLoaded, _view);
			window.setTimeout(function(){
				onExpandNode(node);
			}, delay);
		};
		addToApi('expandNode', expandNode);

		function hideExpandButtons(){
			$(_infoContainer).empty();
		};
		addToApi('hideExpandButtons', hideExpandButtons);

		function defaultInfoButtonRenderer(colorKey, label){
			return '<button class="btn btn-xs nodeExpander-infobutton" style="margin: 4px; color: #000; background-color: ' + (_Graph.color[colorKey] || _defaultInfoButtonColor) + '">' + label + '</button>'
		};

		this.name = _name;
		this.init = init;
	};
})();
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	Graphbrowser.Modules.NodeFilter = function(sigmaInstance, callbacks){
		var _name = "nodeFilter",
			_s = sigmaInstance,
			_callbacks = callbacks,
			_filterOnStart,
			_filterType,
			_filterNodeTypes,
			_hiddenNodeTypes = {},
			_hiddenNodes = {},
			_hiddenRelTypes = {},
			_hiddenRels = {},
			_apiMethods = {};

		function addToApi(name, func){
			_apiMethods = _apiMethods || {};
			_apiMethods[name] = func;
		};

		function init(settings){
			if(settings){
				if(settings.filterType !== undefined){
					(settings.filterType === 'whitelist' || settings.filterType === "blacklist") ? _filterType = settings.filterType : _filterType = 'blacklist'
				}
				else{
					_filterType = 'blacklist';
				}

				settings.filterOnStart !== undefined ? _filterOnStart = settings.filterOnStart : _filterOnStart = true;
				settings.filterNodeTypes !== undefined ? _filterNodeTypes = settings.filterNodeTypes : _filterNodeTypes = [];
			}
			else {
				_filterType = 'blacklist';
				_filterOnStart = false;
				_filterNodeTypes = [];
			}

			_callbacks.eventEmitter[_s.id].bind('hideNodeType', hideNodeType);
			_callbacks.eventEmitter[_s.id].bind('hideNodes', hideNodes);

			_callbacks.bindBrowserEvent('filterNodes', filterNodes);
			_callbacks.bindBrowserEvent('start', start);

			for(var m in _apiMethods){
				_callbacks.api[m] = _apiMethods[m];
			}
			_callbacks.api.addNodeTypeToFilter = addNodeTypeToFilter;
			_callbacks.api.removeNodeTypeFromFilter = removeNodeTypeFromFilter;
			_callbacks.api.setFilterType = setFilterType;
			_callbacks.api.addNodeTypeToFilter = addNodeTypeToFilter;
			_callbacks.api.removeNodeTypeFromFilter = removeNodeTypeFromFilter;
			_callbacks.api.clearFilterNodeTypes = clearFilterNodeTypes;
			_callbacks.api.filterGraph = filterGraph;
			_callbacks.api.hideNodes = hideNodes;
			_callbacks.api.hideNode = hideNode;
			_callbacks.api.hideNodeType = hideNodeType;
			_callbacks.api.hideRelType = hideRelType;
			_callbacks.api.hideRels = hideRels;
		};

		function filterNodes(nodesToFilter, inOrOut){
			if(inOrOut === 'in'){
				return filterNewInNodes(nodesToFilter);
			}

			if(inOrOut === 'out'){
				return filterNewOutNodes(nodesToFilter);
			}
		};

		function filterNewInNodes(newInNodes){
			if(newInNodes){
				$.each(_filterNodeTypes, function(i, filterNodeType){
					newInNodes = newInNodes.filter(function (el) {
						if(_filterType === 'blacklist')
							return el.sourceNode.type !== filterNodeType;
						else{
							if(el.sourceNode.type !== filterNodeType){
								var stay = false;
								$.each(_filterNodeTypes, function(i, filterType2){
									if(el.sourceNode.type === filterType2)
										stay = true;
								});
								return stay;
							}
							else
								return true;
						}
					});
				});
			}

			return newInNodes;
		};

		function filterNewOutNodes(newOutNodes){
			if(newOutNodes){
				$.each(_filterNodeTypes, function(i, filterNodeType){
					newOutNodes = newOutNodes.filter(function (el) {
						if(_filterType === 'blacklist')
							return el.targetNode.type !== filterNodeType;

						else{
							if(el.targetNode.type !== filterNodeType){
								var stay = false;
								$.each(_filterNodeTypes, function(i, filterType2){
									if(el.targetNode.type === filterType2)
										stay = true;
								});
								return stay;
							}
							else
								return true;
						}
					});
				});
			}

			return newOutNodes;
		};

		function addNodeTypeToFilter(nodeType){
			if(nodeType !== undefined)
				_filterNodeTypes.push(nodeType);
		};
		addToApi('addNodeTypeToFilter', addNodeTypeToFilter);

		function setFilterType(filterType){
			if(filterType !== undefined){
				if(filterType === "whitelist" || filterType === "blacklist")
					_filterType = filterType;
			}
		};
		addToApi('setFilterType', setFilterType);

		function removeNodeTypeFromFilter(nodeType){
			if(nodeType !== undefined)
				_filterNodeTypes.splice(_filterNodeTypes.indexOf(nodeType), 1);
		};
		addToApi('removeNodeTypeFromFilter', removeNodeTypeFromFilter);

		function clearFilterNodeTypes(){
			_filterNodeTypes = [];
		};
		addToApi('clearFilterNodeTypes', clearFilterNodeTypes);

		function hideNodeType(event, value){
			var nodeType, status;
			if(event.data){
				nodeType = event.data.nodeType;
				status = event.data.value;
			}
			else{
				nodeType = event;
				status = value;
			}

			if(status){
				_hiddenNodeTypes[nodeType] = status;
			}
			else{
				if(_hiddenNodeTypes[nodeType]){
					_hiddenNodeTypes[nodeType] = !_hiddenNodeTypes[nodeType];
				}
				else{
					_hiddenNodeTypes[nodeType] = true;
				}
			}

			$.each(_s.graph.nodes(), function(i, node){
				if(node.nodeType === nodeType){
					if(_hiddenNodes[node.id] !== undefined &&  _hiddenNodes[node.id].hidden === true){
						return true;
					}
					else{
						node.hidden = _hiddenNodeTypes[nodeType];
					}
				}
			});

			_s.refresh({skipIndexation: true});
		};
		addToApi('hideNodeType', hideNodeType);

		function hideNodes(event){
			var nodes = event.data.nodes;
			var status = event.data.status;

			$.each(nodes, function(i, nodeToHide){
				$.each(_s.graph.nodes(), function(e, nodeInGraph){
					if(nodeToHide.id === nodeInGraph.id){
						if(_hiddenNodeTypes[nodeToHide.nodeType] === true){
							return true;
						}
						_hiddenNodes[nodeToHide.id] = {};
						_hiddenNodes[nodeToHide.id].hidden = status;
						nodeInGraph.hidden = status;
						return false;
					}
				});
			});

			_s.refresh({skipIndexation: true});
		};
		addToApi('hideNodes', hideNodes);

		function hideRelType(relName, status){
			if(status){
				_hiddenRelTypes[relName] = status;
			}
			else{
				if(_hiddenRelTypes[relName]){
					_hiddenRelTypes[relName] = !_hiddenRelTypes[relName];
				}
				else{
					_hiddenRelTypes[relName] = true;
				}
			}

			$.each(_s.graph.edges(), function(i, edge){
				if(edge.relName === relName){
					if(_hiddenRels[edge.id] !== undefined &&  _hiddenRels[edge.id].hidden === true){
						return true;
					}
					else{
						edge.hidden = _hiddenRelTypes[relName];
					}
				}
			});

			_s.refresh({skipIndexation: true});
		};
		addToApi('hideRelType', hideRelType);

		function hideRels(edges, status){
			$.each(edges, function(i, edgeToHide){
				$.each(_s.graph.edges(), function(e, edgeInGraph){
					if(edgeToHide.id === edgeInGraph.id){
						if(_hiddenRelTypes[edgeToHide.relName] === true){
							return true;
						}
						_hiddenRels[edgeToHide.id] = {};
						_hiddenRels[edgeToHide.id].hidden = status;
						edgeInGraph.hidden = status;
						return false;
					}
				});
			});

			_s.refresh({skipIndexation: true});
		};
		addToApi('hideRels', hideRels);

		function filterGraph(){
			var ids = [];
			$.each(_filterNodeTypes, function(i, filterType){
				$.each(_s.graph.nodes(), function(i, node){
					if(node.nodeType === filterType && _filterType === 'blacklist'){
						ids.push(node.id);
					}
					if(node.nodeType !== filterType && _filterType === 'whitelist'){
						var del = true;

						$.each(_filterNodeTypes, function(i, filterType2){
							if(node.nodeType === filterType2)
								del = false;
						});

						if(del){
							ids.push(node.id);
						}
					}
				});
			});
			$.each(ids, function(i, id){
				_s.graph.dropNode(id);
			});
			_callbacks.refreshSigma(false, true);
		};
		addToApi('filterGraph', filterGraph);

		function hideNode(id, status){
			var node = _s.graph.nodes(id);

			if(node){
				node.hidden = status;
				_s.refresh({skipIndexation: true});
			}
		};
		addToApi('hideNode', hideNode);

		function start(){
			var self = this;
			if(_filterOnStart && _filterNodeTypes.length > 0)
				filterGraph();
		};

		this.name = _name;
		this.init = init;
	};

})();
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	Graphbrowser.Modules.RelationshipEditor = function(sigmaInstance, callbacks){
		var _name = 'relationshipEditor',
			_active = true,
			_s = sigmaInstance,
			_callbacks = callbacks,
			_worker = undefined,
			_schemaInformation = undefined,
			_inKey = undefined,
			_outKey = undefined,
			_pressedKeys = {},
			_deleteEvent = undefined,
			_onDeleteRelation = undefined,
			_maxDistance = 0,
			_bound = false;

		function init(settings){
			if(settings){
				settings.incommingRelationsKey !== undefined ? _inKey =	settings.incommingRelationsKey : _inKey = "shiftKey";
				settings.outgoingRelationsKey !== undefined ? _outKey = settings.outgoingRelationsKey : _outKey = "ctrlKey";
				settings.deleteEvent !== undefined ? _deleteEvent = settings.deleteEvent : _deleteEvent = "rightClickEdges";
				settings.onDeleteRelation !== undefined ? _onDeleteRelation = settings.onDeleteRelation : _onDeleteRelation = undefined;
				settings.maxDistance !== undefined ? _maxDistance = settings.maxDistance : _maxDistance = 200;

				if(typeof this.getRelationshipEditorWorker === undefined)
					throw new Error("Graph-Browser-RelationshipEditor: Worker not found.");

				var workerString = this.getRelationshipEditorWorker();

				if (window.Worker && !_worker) {
					var blob = new Blob([workerString], {type: 'application/javascript'});
					_worker = new Worker(URL.createObjectURL(blob));
					_worker.onmessage = onmessage;
					_callbacks.conn.getSchemaInformation(onSchemaInformationLoaded);
				}
				else{
					console.log("Graph-Browser-RelationshipEditor: It seems that your browser does not support webworkers.");
				}

				_pressedKeys = {shiftKey: false, ctrlKey: false, altKey: false, noKey: true}
				_bound = false;

				_active = true;
				bindEvents();
			}
			else
				_active = false
		};

		function bindEvents(){
			_callbacks.sigmaCtrl.bindSigmaEvent('keyup', handleKeyEvent);
			_callbacks.sigmaCtrl.bindSigmaEvent('keydown', handleKeyEvent);
			_callbacks.sigmaCtrl.bindSigmaEvent(_deleteEvent, handleSigmaEvent);

			_callbacks.bindBrowserEvent('start', start);
			_callbacks.eventEmitter[_s.id].bind('dataChanged', onDataChanged);
			_callbacks.eventEmitter[_s.id].bind('kill', onKill);
		};

		function onKill(){
			_callbacks.sigmaCtrl.unbindSigmaEvent('keyup', handleKeyEvent);
			_callbacks.sigmaCtrl.unbindSigmaEvent('keydown', handleKeyEvent);
			_callbacks.sigmaCtrl.unbindSigmaEvent(_deleteEvent, handleSigmaEvent);

			_callbacks.eventEmitter[_s.id].unbind('kill', start);
			_callbacks.eventEmitter[_s.id].unbind('dataChanged', onDataChanged);

			_worker.terminate();
			_worker = undefined;
		};

		function start(){
			if(_active)
				_callbacks.conn.getSchemaInformation(onSchemaInformationLoaded);
		};

		function handleKeyEvent(keys){
			var nokey = keys.ctrlKey || keys.shiftKey;
			_pressedKeys = {
				ctrlKey: keys.ctrlKey,
				shiftKey: keys.shiftKey,
				noKey: !nokey
			};

			if(!_pressedKeys.noKey && !_bound){
				_bound = true;
				_callbacks.sigmaCtrl.bindSigmaEvent('startdrag', handleSigmaEvent);
				_callbacks.sigmaCtrl.bindSigmaEvent('drag', handleSigmaEvent);
				_callbacks.sigmaCtrl.bindSigmaEvent('dragend', handleSigmaEvent);
			}

			else{
				_bound = false;
				_callbacks.sigmaCtrl.unbindSigmaEvent('startdrag', handleSigmaEvent);
				_callbacks.sigmaCtrl.unbindSigmaEvent('drag', handleSigmaEvent);
				_callbacks.sigmaCtrl.unbindSigmaEvent('dragend', handleSigmaEvent);
				window.setTimeout(function(){
					_worker.postMessage({msg: "removeNewEdges", edges: _s.graph.edges(), nodes: _s.graph.nodes()});
				}, 50);
			}
		};

		function handleSigmaEvent(event){
			if(!_active)
				return;
			switch(event.type){
				case 'startdrag':
					if(_worker)
						_worker.postMessage({msg: "updateGraphInfo", nodes: _s.graph.nodes() , edges: _s.graph.edges()});
				case 'drag':
					if(_worker)
						_worker.postMessage({msg: "handleDrag", dragedNode: event.data.node, keys: _pressedKeys, cameraRatio: _s.camera.ratio});
					break;
				case 'dragend':
					handleDragend(event.data.node);
					break;
				case _deleteEvent:
					handleEdgeDeleteEvent(event.data.edge);
					break;
			}
		};

		function onmessage(event){
			var eventType = event.data.msg;
			switch(eventType){
				case "add":
					//console.log("Add");
					addNewEdge(event.data.edge, event.data.remove);
					break;
				case "removeNewEdge":
					//console.log("remove");
					removeNewEdge(event.data.edge);
					break;
				case "hideEdge":
					//console.log("hide");
					hideEdge(event.data.edge);
					break;
				case "unhideEdge":
					//console.log("unhide");
					unhideEdge(event.data.edge);
					break;
				case "debug":
					console.log(event.data.text);
					break;
				default:
					//console.log("Graph-Browser-RelationshipEditor: Unknown event");
					break;
			}
		};

		function addNewEdge(edge, remove){
			if(remove !== undefined)
				var delEdge = _s.graph.edges(remove);
				if(delEdge)
					if(!delEdge.lock)
						_s.graph.dropEdge(remove);
			if(!_s.graph.edges(edge.id))
				_s.graph.addEdge(edge);
			sigma.canvas.edges.autoCurve(_s);
			_s.refresh({skipIndexation: false});
			onDataChanged();
		};

		function removeNewEdge(edge){
			var delEdge = _s.graph.edges(edge);
			//console.log('Graph-Browser-RelationshipEditor: Drop edge: '+ edge);
			if(delEdge)
				if(!delEdge.lock)
					_s.graph.dropEdge(edge);
			sigma.canvas.edges.autoCurve(_s);
			_s.refresh({skipIndexation: false});
			onDataChanged();
		};

		function hideEdge(edgeId){
			var edge = _s.graph.edges(edgeId);
			if(!edge.hidden){
				//console.log("hide: " + edge.label + ":" + edge.id);
				edge.hidden = true;
				sigma.canvas.edges.autoCurve(_s);
				_s.refresh({skipIndexation: true});
				//onDataChanged();
			}
		};

		function unhideEdge(edgeId){
			var edge = _s.graph.edges(edgeId) || {};
			if(edge.hidden){
				//console.log("unhide: " + edge.label + ":" + edge.id);
				edge.hidden = false;
				sigma.canvas.edges.autoCurve(_s);
				_s.refresh({skipIndexation: true});
				//onDataChanged();
			}
		};

		function handleEdgeDeleteEvent(edge, del){
			var sigmaContainer = _s.renderers[0].container;
			$('body').on('contextmenu', sigmaContainer, function(e){return false;});

			if(_onDeleteRelation !== undefined && del === undefined){
				var source = _s.graph.nodes(edge.source);
				var target = _s.graph.nodes(edge.target);
				_onDeleteRelation(edge, source, target, handleEdgeDeleteEvent);
			}
			if(del){
				_callbacks.conn.deleteRelationship(edge, function(result){
					console.log("DeleteEdgeEvent: " + edge.id);
					_s.graph.dropEdge(edge.id);
					sigma.canvas.edges.autoCurve(_s);
					_s.refresh({skipIndexation: false});
					onDataChanged();
				});
			}
		};

		function handleDragend(node){
			$.each(_s.graph.edges(), function(n, edge){
				if(edge.added) {
					edge.lock = true;
					_worker.postMessage({msg: "commitNewEdge", edge: edge.id});

					_callbacks.conn.createRelationship(edge.source, edge.target, edge.relType, function(rel){
						var temp = edge;
						_s.graph.dropEdge(edge.id);

						temp.id = rel[0];
						temp.color = _s.settings('defaultEdgeColor');
						temp.size = _s.settings("newEdgeSize");
						temp.type = _s.settings("newEdgeType");
						temp.added = false;
						temp.replaced = undefined;
						temp.lock = false;
						temp.cc = undefined;
						_s.graph.addEdge(temp);
						_s.refresh({skipIndexation: false});
						sigma.canvas.edges.autoCurve(_s);
						onDataChanged();
					}, edge.id, onErrorCreatingRelation);

				}
			});
		};

		function onSchemaInformationLoaded(result){
			_schemaInformation = {};
			$.each(result, function(o, res){
				_schemaInformation[res.type] = res;
			});
			_worker.postMessage({
				msg: "init",
				settings: {
					schema: _schemaInformation,
					maxDistance: _maxDistance,
					nodes: _s.graph.nodes(),
					edges: _s.graph.edges(),
					rendererId: _s.renderers[0].conradId
				}
			});
		};

		function onDataChanged(event){
			if(_worker)
				_worker.postMessage({msg: "updateGraphInfo", nodes: _s.graph.nodes(), edges: _s.graph.edges()});
		};

		function onErrorCreatingRelation(errorEdgeId, errorMessage){
			if(errorMessage ==="Relationship already exists"){
				var errorEdge = _s.graph.edges(errorEdgeId);
				_callbacks.conn.getRelationshipsOfType(errorEdge.relType, function(result){
					$.each(result, function(i, rel){
						if(rel.sourceId === errorEdge.source && rel.targetId === errorEdge.target){
							if(_s.graph.edges(rel.id) === undefined){
								_s.graph.addEdge({
									id: rel.id,
									source: rel.sourceId,
									target: rel.targetId,
									relType: rel.relType,
									label: rel.relType,
									size: _s.settings("newEdgeSize"),
									type: _s.settings("newEdgeType"),
									color: _s.settings('defaultEdgeColor'),
									cc: undefined
								});
								sigma.canvas.edges.autoCurve(_s);
							}
							return false;
						}
						if(_s.graph.edges(errorEdgeId))
							_s.graph.dropEdge(errorEdgeId);
					})
				});
			}
			else
			_s.graph.dropEdge(errorEdgeId);
			_callbacks.eventEmitter[_s.id].dispatchEvent('dataChanged');
		};

		this.name = _name;
		this.init = init;
	};
})();
(function() {
  "use strict";

	var Worker = function(settings){

		var _previousEdges = {};
		var _schema;
		var _maxDistance;
		var _nodes;
		var _edges;
		var _newNodes;
		var _newEdges;
		var _rendererId;

		var _edgeReferences = {}

		function init(settings){
			settings.schema !== undefined ? _schema = settings.schema : _schema = [];
			settings.maxDistance !== undefined ? _maxDistance = settings.maxDistance : _maxDistance = 200;
			settings.nodes !== undefined ? _nodes = settings.nodes : _nodes = [];
			settings.edges !== undefined ? _edges = settings.edges : _edges = [];
			settings.rendererId !== undefined ? _rendererId = settings.rendererId : rendererId = 0;
			_newEdges = undefined;
			_newNodes = undefined;
		};

		function updateGraphInfo(nodes, edges, schema) {
			_newNodes = nodes;
			_newEdges = edges;
		}

		function handleDrag(dragedNode, keys, cameraRatio) {
			_nodes = _newNodes || _nodes;
			_edges = _newEdges || _edges;
			if(!_schema)
				return;

			var schemaInfo = _schema[dragedNode.nodeType];

			for(var counter = 0; counter < _nodes.length; counter++){
				if(dragedNode.id === _nodes[counter].id) continue;
				var nd = nodeDistance(_nodes[counter], dragedNode);

				var relatedToOrFrom = "not";
				var allTypesPossible = undefined;
				var possibleTypes = undefined;
				var related = undefined;
				var compareSource = undefined, compareTarget = undefined;
				var triggerDistance = nd;

				if(schemaInfo === undefined)
					return;

				if(cameraRatio > 1){
	                //triggerDistance = (_maxDistance / (1/cameraRatio));
	            	triggerDistance = (nd * cameraRatio);
	            }

	            if(cameraRatio < 1){
	            	//triggerDistance = (_maxDistance / (cameraRatio));
	                triggerDistance = (nd * cameraRatio);
	            }

				if(keys.shiftKey === true && triggerDistance <= _maxDistance)
					relatedToOrFrom = "to";
				else if(keys.ctrlKey === true && triggerDistance <= _maxDistance)
					relatedToOrFrom = "from";

				switch(relatedToOrFrom){
					case "from":
						if(schemaInfo.relatedFrom !== undefined){
							var dis = _maxDistance / schemaInfo.relatedFrom.length;
							var length = (schemaInfo.relatedFrom.length), selector;

							while(length >= 0){
								if(nd < (length * dis))
									selector = length;
								length--;
							}

							if(selector){
								selector -= 1;
								//postMessage({msg: 'debug', text: "Trigger: " + triggerDistance + "  -  nodeDistance: " + nd + "  -  selector: " + selector});
								related = schemaInfo.relatedFrom[selector];
								var newEdgeId = selector + ":" + _nodes[counter].id + ":" + dragedNode.id;
								allTypesPossible = "allSourceTypesPossible";
								possibleTypes = "possibleSourceTypes";
								compareTarget = dragedNode.id;
								compareSource = _nodes[counter].id;
							}
						}
					break
					case "to":
						if(schemaInfo.relatedTo !== undefined){
							var dis = _maxDistance / schemaInfo.relatedTo.length;
							var length = (schemaInfo.relatedTo.length), selector;

							while(length >= 0){
								if(nd < (length * dis))
									selector = length;
								length--;
							}

							if(selector){
								selector -= 1;
								//postMessage({msg: 'debug', text: "Trigger: " + triggerDistance + "  -  nodeDistance: " + nd + "  -  selector: " + selector});
								related = schemaInfo.relatedTo[selector];
								var newEdgeId = selector + ":" + dragedNode.id + ":" + _nodes[counter].id;
								allTypesPossible = "allTargetTypesPossible";
								possibleTypes = "possibleTargetTypes";
								compareTarget = _nodes[counter].id;
								compareSource = dragedNode.id;
							}
						}
					break;
					default:
						for(var c3 = 0; c3 < _edges.length; c3++){
							if ((_edges[c3].source === dragedNode.id && _edges[c3].target === _nodes[counter].id) || (_edges[c3].source === _nodes[counter].id && _edges[c3].target === dragedNode.id)){
								if (_edges[c3].added){

									if(_edgeReferences[_edges[c3].id]){
										for(var c4 = 0; c4 < _edgeReferences[_edges[c3].id].length; c4++){
											unhideEdge(_edgeReferences[_edges[c3].id][c4]);
										}
										_edgeReferences[_edges[c3].id] = undefined;
									}

									_previousEdges[_nodes[counter].id] = _previousEdges[_nodes[counter].id] || {};
									_previousEdges[_nodes[counter].id].id = _previousEdges[_nodes[counter].id].id || "";
									if(!_edges[c3].dropped){
										postMessage({msg: "removeNewEdge", edge: _edges[c3].id});
										_edges[c3].dropped = true;
										if(_previousEdges[_nodes[counter].id].id === _edges[c3].id){
											_previousEdges[_nodes[counter].id].id = undefined;
										}
									}
								}
							}
						}
					break;
				}

				if(related !== undefined){
					related[possibleTypes] = related[possibleTypes] || "";
					if((related[allTypesPossible] === true || related[possibleTypes].split(',').indexOf(_nodes[counter].nodeType) > -1)){
						var add = true;
						var same = false;
						if(related.sourceMultiplicity === '1') {
							for(var c1 = 0; c1 < _edges.length; c1++){
								if(_edges[c1].target === compareTarget && _edges[c1].relType === related.type) {
									if(_edges[c1].source === compareSource){
										add = false;
										same = true;
									}
									else
										add = checkRelation(_edges[c1], newEdgeId);
									if(!add)
										break;
								}
							}
						}
						if(related.targetMultiplicity === '1') {
							for(var c2 = 0; c2 < _edges.length; c2++){
								if(_edges[c2].source === compareSource && _edges[c2].relType === related.type) {
									if(_edges[c2].target === compareTarget){
										add = false;
										same = true;
									}
									else
										add = checkRelation(_edges[c2], newEdgeId);
									if(!add)
										break;
								}
							}
						}

						_previousEdges[_nodes[counter].id] = _previousEdges[_nodes[counter].id] || {};
						_previousEdges[_nodes[counter].id].id = _previousEdges[_nodes[counter].id].id || "";

						if(_previousEdges[_nodes[counter].id].id !== newEdgeId && add){

							var source, target
							switch(relatedToOrFrom){
								case "to":
									source = dragedNode.id;
									target = _nodes[counter].id;
									break;
								case "from":
									source = _nodes[counter].id;
									target = dragedNode.id;
									break;
							}

							var newEdge = {
								id: newEdgeId,
								label: related.relationshipType,
								source: source,
								target: target,
								size: 40,
								color: '#81ce25',
								type: 'curvedArrow',
								added: true,
								relType: related.type
							};

							if(_edgeReferences[_previousEdges[_nodes[counter].id].id]){
								var old = _edgeReferences[_previousEdges[_nodes[counter].id].id];
								for(var o = 0; o < old.length; o++){
									unhideEdge(old[o]);
								}
								delete _edgeReferences[_previousEdges[_nodes[counter].id].id];
							}

							postMessage({msg: "add", edge: newEdge, remove: _previousEdges[_nodes[counter].id].id});
							_previousEdges[_nodes[counter].id].id = newEdgeId;
						}
					}
				}
			}

			if(_newNodes !== undefined && _newEdges !== undefined){
				_edges = _newEdges;
				_nodes = _newNodes;
				_newNodes = undefined;
				_newEdges = undefined;
			}
		};

		function checkRelation(relation, newEdgeId){
			var add = true;
			if(relation.added){
				add = false;
			}
			else if(!relation.hidden){
				if(_edgeReferences[newEdgeId]){
					_edgeReferences[newEdgeId].push(relation);
					add = false;
					hideEdge(relation);
				}
				else{
					_edgeReferences[newEdgeId] = [];
					_edgeReferences[newEdgeId].push(relation);
					add = true;
					hideEdge(relation);
				}
			}
			return add;
		};

		function removeNewEdges(nodes, edges) {
			for(var e = 0; e < edges.length; e++){
				if(edges[e].added){
					if(_edgeReferences[edges[e].id]){
						for(var c4 = 0; c4 < _edgeReferences[edges[e].id].length; c4++){
							unhideEdge(_edgeReferences[edges[e].id][c4]);
						}
					}
					postMessage({msg: "removeNewEdge", edge: edges[e].id});
				}
			}
			_edgeReferences = {};
			_previousEdges = {};
		};

		function nodeDistance(n1, n2){
			var x1 = parseFloat(n1['renderer' + _rendererId + ':x']);
			var y1 = parseFloat(n1['renderer' + _rendererId + ':y']);
			var x2 = parseFloat(n2['renderer' + _rendererId + ':x']);
			var y2 = parseFloat(n2['renderer' + _rendererId + ':y']);
			var nodeDistance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
			return nodeDistance;
		};

		function hideEdge(edge){
			edge.hidden = true;
			postMessage({msg: "hideEdge", edge: edge.id});
		};

		function unhideEdge(edge){
			edge.hidden = false;
			postMessage({msg: "unhideEdge", edge: edge.id});
		};

		function removeNewEdge(edge){
			if(!edge.removed){
				edge.removed = true;
				postMessage({msg: "removeNewEdge", edge: edge.id});
			}
		};

		function commitNewEdge(edgeId){
			if(_edgeReferences[edgeId]){
				for(var i = 0; i < _edgeReferences[edgeId].length; i++){
					removeNewEdge(_edgeReferences[edgeId][i]);
				}
			}
		}

		var listener = function(event){
			var eventType = event.data.msg;
			switch(eventType){
				case "init":
					init(event.data.settings);
					//postMessage({msg: "debug", text: "init"});
					break;
				case "updateGraphInfo":
					updateGraphInfo(event.data.nodes, event.data.edges);
					//postMessage({msg: "debug", text: "updateGraphInfo"});
					break;
				case "handleDrag":
					handleDrag(event.data.dragedNode, event.data.keys, event.data.cameraRatio);
					//postMessage({msg: "debug", text: "handleDrag"});
					break;
				case "removeNewEdges":
					removeNewEdges(event.data.nodes, event.data.edges);
					//postMessage({msg: "debug", text: "handleDrag"});
					break;
				case "commitNewEdge":
					commitNewEdge(event.data.edge);
					break;
				default:
					break;
			}
		};

		this.addEventListener("message", listener);
	};

	function getRelationshipEditorWorker(){
		return ';(' + Worker.toString() + ').call(this);';
	}

	if (typeof Graphbrowser.Modules.RelationshipEditor === 'undefined')
    	throw 'Graphbrowser.Modules.RelationshipEditor is not declared';

    Graphbrowser.Modules.RelationshipEditor.prototype.getRelationshipEditorWorker = getRelationshipEditorWorker;

})();
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	Graphbrowser.Modules.SelectionTools = function(sigmaInstance, callbacks){
		var _name = 'selectionTools',
			_s = sigmaInstance,
			_callbacks = callbacks,
			_active = false,
			_currentActiveSelection,
			_timeout = 0,
			_activeStateSelections = {},
			_apiMethods = {};

		function addToApi(name, func){
			_apiMethods = _apiMethods || {};
			_apiMethods[name] = func;
		}

		function init(settings){
			for(var m in _apiMethods){
				_callbacks.api[m] = _apiMethods[m];
			}

			_callbacks.bindBrowserEvent('restoreGraph', onRestore);
		};

		function activate(){
			if(_active)
				return;
			_active = true;

			_s.renderers[0].bind('render', function(e) {
				_s.renderers[0].halo({
					nodes: _callbacks.sigmaPlugins.activeState.nodes()
				});
			});

			_callbacks.sigmaPlugins.lasso.bind('selectedNodes', function (event) {
				window.clearTimeout(_timeout);
				_timeout = setTimeout(function() {

					if(_activeStateSelections[_currentActiveSelection].hidden){
						hideSelectionGroup(_currentActiveSelection, false);
					}
					if(_activeStateSelections[_currentActiveSelection].fixed){
						fixateSelectionGroup(_currentActiveSelection, false);
					}

					if(event.data.length <= 0)
						return;

					_activeStateSelections[_currentActiveSelection].nodes = event.data;

					activateSelectionLasso(false);
					_s.refresh({skipIndexation: true});
					updateActiveState();
					_timeout = 0;
				}, 10);
			});

			updateActiveState();
			_s.refresh({skipIndexation: true})
		};
		addToApi('activateSelectionTools', activate);

		function createSelectionGroup(){
			var newId = (parseInt(Math.random() * (5000 - 1))).toString();
			_activeStateSelections[newId] = {id: newId, nodes: [], hidden: false, fixed: false};
			_currentActiveSelection = newId;
			return newId;
		};
		addToApi('createSelectionGroup', createSelectionGroup);

		function hideSelectionGroup(groupId, status){
			if(status === undefined)
				return;

			if(_activeStateSelections[groupId].hidden !== status){
				_activeStateSelections[groupId].hidden = status;
				_callbacks.eventEmitter[_s.id].dispatchEvent('hideNodes', {nodes: _activeStateSelections[groupId].nodes, status: status});
			}
		};
		addToApi('hideSelectionGroup', hideSelectionGroup);

		function fixateSelectionGroup(groupId, status){
			if(status === undefined)
				return;
			if(_activeStateSelections[groupId].fixed !== status){
				_activeStateSelections[groupId].fixed = status;
				$.each(_activeStateSelections[groupId].nodes, function(i, node){
					$.each(_s.graph.nodes(), function(key, gnode){
						if(node.id === gnode.id){
							gnode.fixed = _activeStateSelections[groupId].fixed;
							node.fixed = _activeStateSelections[groupId].fixed;
						}
					});
				});
			}
		};
		addToApi('fixateSelectionGroup', fixateSelectionGroup);

		function clearSelectionGroup(groupId){
			onHideSelectionGroup(groupId, false);
			setSelectionFixed(groupId, false);
			_callbacks.sigmaPlugins.activeState.dropNodes();
			_activeStateSelections[groupId].nodes = undefined;
			_s.refresh({skipIndexation: true});
		};
		addToApi('clearSelectionGroup', clearSelectionGroup);

		function deleteSelectionGroup(groupId){
			clearSelectionGroup();
			_activeStateSelections.splice(_activeStateSelections.indexof(groupId), 1);
		};
		addToApi('deleteSelectionGroup', deleteSelectionGroup);

		function activateSelectionGroup(groupId){
			if(_active === false || groupId === undefined){
				return;
			}
			_currentActiveSelection = groupId;
			updateActiveState();
		};
		addToApi('activateSelectionGroup', activateSelectionGroup);

		function updateActiveState(){
			if(!_active && _activeStateSelections[_currentActiveSelection].nodes === undefined)
				return;

			_callbacks.sigmaPlugins.activeState.dropNodes();
			var an = [];
			$.each(_activeStateSelections[_currentActiveSelection].nodes, function(i, node) {
				an.push(node.id);
			});
			_callbacks.sigmaPlugins.activeState.addNodes(an);
			_s.refresh({skipIndexation: true});
		};
		addToApi('updateActiveState', updateActiveState);

		function activateSelectionLasso(status){
			if((!_callbacks.sigmaPlugins.lasso.isActive) && status === true){
				if(!_active)
					activate();
				_callbacks.sigmaPlugins.lasso.activate();
			}
			else{
				_callbacks.sigmaPlugins.lasso.deactivate();
			}
		};
		addToApi('activateSelectionLasso', activateSelectionLasso);

		function deactivateSelectionTools(status){
			_active = false;

			if(_callbacks.sigmaPlugins.lasso.isActive){
				activateSelectionLasso(false);
			}

			_s.renderers[0].unbind('render');
			_callbacks.sigmaPlugins.activeState.dropNodes();
			_callbacks.refreshSigma(true);
		};
		addToApi('deactivateSelectionTools', deactivateSelectionTools);

		function dropSelection(groupId){
			var nodes = _activeStateSelections[groupId].nodes;

			if (nodes !== undefined && nodes.length > 0) {
				_callbacks.sigmaPlugins.activeState.dropNodes();
				$.each(nodes, function(i, node){
					if(_s.graph.nodes(node.id))
						_s.graph.dropNode(node.id);
				});
				_s.refresh();

				$.each(_activeStateSelections, function(i, selection){
					var ids = [];
					$.each(selection.nodes, function(i, node){
						if(!_s.graph.nodes(node.id))
							ids.push(node.id);
					});

					$.each(ids, function(i, id){
						selection.nodes.splice(selection.nodes.indexOf(id), 1);
					});
				});
			}
		};
		addToApi('dropSelection', dropSelection);

		function onRestore(){
			_activeStateSelections = [];
			createSelectionGroup();
			if(_active){
				_callbacks.sigmaPlugins.activeState.dropNodes();
				updateActiveState();
			}
		};

		this.name = _name;
		this.init = init;
	};
})();
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	Graphbrowser.Modules.StatusUndo = function (sigmaInstance, callbacks){
		var _name = 'statusUndo',
			_s = sigmaInstance,
			_callbacks = callbacks,
			_status = undefined,
			_undoStack = [];

		function init(settings){
			if(settings){
				if(settings.active){
					_status = 0;
					$(document).on('click', '#statusUndo-controlElement',  function() {
						undo();
					});
				}
			}
			_callbacks.eventEmitter[_s.id].bind('dataChanged', onDataChanged);
			_callbacks.bindBrowserEvent('start', start);
			_callbacks.bindBrowserEvent('status', status);
			_callbacks.bindBrowserEvent('reset', onReset);

			_callbacks.api.undo = undo;
		};

		function start(){
			onDataChanged();
			_status--;
		};

		function onDataChanged(event){
			var changeType;
			if(!event)
				changeType = "";
			else
				changeType = event.data.source;

			if(changeType === 'undo'){
				return;
			}

			//make a deep copy of the elements, so changes to the parameters of them won't effect the already stored elements

			/*var nodes = $.extend(true, [], _s.graph.nodes());
			var edges = $.extend(true, [], _s.graph.edges());
			var camera = $.extend(true, {}, _s.camera);*/

			var nodes = JSON.parse(JSON.stringify(_s.graph.nodes()));
			var edges = JSON.parse(JSON.stringify(_s.graph.edges()));
			var camera =  JSON.parse(JSON.stringify(_s.camera));

			//push the new status on the stack and delete the stack for redo
			_undoStack.push({graph: {'nodes': nodes, 'edges': edges}, camera: camera});
			_status++;
		};

		function undo(){
			var ids = [];

			if(_undoStack.length <= 1){
				_status = 0;
				return;
			}

			_status--;
			_callbacks.undo();
			_undoStack.splice(_undoStack.length -1, 1);

			//Clear the current Edges
			var edges = _s.graph.edges();
			$.each(edges, function(i, edge){
				_s.graph.dropEdge(edge.id);
			});

			//Check if there are less nodes in the old graph that is being restored
			$.each(_s.graph.nodes(), function(i, oldNode){
				var stillInGraph = false;
				$.each(_undoStack[_undoStack.length - 1].graph.nodes, function(o, newNode){
					if(oldNode.id === newNode.id){
						stillInGraph = true;
						oldNode.to_x = newNode.x;
						oldNode.to_y = newNode.y;
						ids.push(oldNode.id);
					}
				});

				if(!stillInGraph)
					_s.graph.dropNode(oldNode.id);
			});

			//Check if there are more nodes in the old graph that is being restored
			$.each(_undoStack[_undoStack.length - 1].graph.nodes, function(o, newNode){
				var needForPush = true;
				$.each(_s.graph.nodes(), function(i, oldNode){
					if(oldNode.id === newNode.id)
						needForPush = false;
				});

				if(needForPush){
					_callbacks.sigmaCtrl.addNode(newNode);
					_s.graph.nodes(newNode.id).x = newNode.x;
					_s.graph.nodes(newNode.id).y = newNode.y;
				}
			});

			//add the old edges
			$.each(_undoStack[_undoStack.length - 1].graph.edges, function(i, edge){
				_callbacks.sigmaCtrl.addEdge(edge);
			});

			//Move the Camera to the old position.
			/*var anim = sigma.misc.animation.camera(_s.camera,
				{
					x: _undoStack[_undoStack.length - 1].camera.x,
		  			y: _undoStack[_undoStack.length - 1].camera.y,
		  			ratio: _undoStack[_undoStack.length - 1].camera.ratio
		  		},
		  		{
		  			easing: 'quadraticInOut',
		  			duration: 100000
		  		}
		  	);*/

		  	sigma.plugins.animate(_s,
		  		{
		  			x: 'to_x', y: 'to_y'
		  		},
		  		{
		  			'nodes': ids,
		  			onComplete: function(){
		  				_callbacks.refreshSigma(false, false)
		  			},
		  			easing: 'quadraticInOut',
		  			duration: 800
		  		}
		  	);
			_callbacks.eventEmitter[_s.id].dispatchEvent('dataChanged', {source: 'undo'});
		};

		function status(){
			return _status;
		};

		function onReset(){
			_status = 1;
			if(_undoStack.length >= 2)
				_undoStack.splice(2, _undoStack.length - 2);
			undo();
		};

		this.name = _name;
		this.init = init;
		this.status = status;
	};
})();
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	Graphbrowser.Modules.Swimlane = function(sigmaInstance, callbacks){
		var _name = "swimlane",
			_s = sigmaInstance,
			_callbacks = callbacks,
			_critX,
			_critY,
			_critXType,
			_critYType,
			_view,
			_options,
			_timeout,
			_nodesShape,
			_standartNodeShape,
			_newNodeShape,
			_critXDbName,
			_critYDbName,
			_timeoutAnim = 0,
			_pendingNodes = [],
			_loadedData = [],
			_undoStack = [],
			_alteredNodeShapes = {};

		function init(settings){
			if(settings !== undefined){
				if(settings.critX !== undefined && typeof settings.critX === 'string'){
					var tmp =  settings.critX.split('.');
					_critX = tmp[0];
					_critXType = tmp[1];
					_critXDbName = tmp[2];
				}

				if(settings.critY !== undefined && typeof settings.critY === 'string'){
					var tmp =  settings.critY.split('.');
					_critY = tmp[0];
					_critYType = tmp[1];
					_critYDbName = tmp[2];
				}

				settings.view !== undefined ? _view = settings.view : _view = '_graph';
				settings.nodesShape !== undefined ? _nodesShape = settings.nodesShape : _nodesShape = "";
				settings.standartNodeShape !== undefined ? _standartNodeShape = settings.standardNodeShape : _standartNodeShape = "";

				_callbacks.sigmaCtrl.bindSigmaEvent('startdrag', handleSigmaEvent);
			}

			_callbacks.bindBrowserEvent('reset', reset);
			_callbacks.bindBrowserEvent('undo', undo);
			_callbacks.bindBrowserEvent('doLayout', doLayout);
			_callbacks.bindBrowserEvent('layoutStart', kill);
			_callbacks.eventEmitter[_s.id].bind('dataChanged', onDataChanged);
		};

		function doLayout(layout, options, second){
			var listener = undefined;

			if(layout !== _name)
				return;

			if(options !== undefined && typeof options === 'object'){
				_options = options;
				var tmp =  options.critX.split('.');
				_critX = tmp[0];
				_critXType = tmp[1];
				_critXDbName = tmp[2];

				tmp =  options.critY.split('.');
				_critY = tmp[0];
				_critYType = tmp[1];
				_critYDbName = tmp[2];

				if(options.view !== undefined)
					_view = options.view;
				if(options.nodesShape !== undefined)
					_nodesShape = options.nodesShape || '';
			}

			if(!second){
				$.each(_s.graph.nodes(), function(i, node){
					_callbacks.conn.getData(node, "node", loadDataForLayout, _view);
				});
			}

			else{
				$.each(_s.graph.nodes(), function(i, node){
					$.each(_loadedData, function(o, data){
						if(node.id === data.id){
							var t = _critXDbName || _critX;
							var u = _critYDbName || _critY;
							switch(_critXType){
								case 'string':
									data[t] !== undefined ?
										node[_critX] = data[t] :
											node[_critX] = '';
									break;
								case 'date':
									data[t] !== undefined ?
										node[_critX] =  Date.parse(data[t]) :
											node[_critX] = 14400000;
									break;
								default:
									data[t] !== undefined ?
										node[_critX] = data[t] :
											node[_critX] = 0;
									break;
							}
							switch(_critYType){
								case 'string':
									data[u] !== undefined ?
										node[_critY] = data[u] :
											node[_critY] = '';
									break;
								case 'date':
									data[u] !== undefined ?
										node[_critY] =  Date.parse(data[u]) :
											node[_critY] = 14400000;
									break;
								default:
									data[u] !== undefined ?
										node[_critY] = data[u] :
											node[_critY] = 0;
									break;
							}
							return false;
						}
					});
				});

				listener = sigma.layouts.swimlane.start(_s, {critX: _critX, critY: _critY});

				listener.bind('stop', function(event) {
					window.clearTimeout(_timeoutAnim);
					_timeoutAnim = window.setTimeout(function() {
						_callbacks.eventEmitter[_s.id].dispatchEvent('dataChanged', {source: _name});
						if(_nodesShape){
							$.each(_s.graph.nodes(), function(i, node){
								_alteredNodeShapes[node.id] = node.type;
								node.type = _nodesShape;
								node.sw = true;
								_callbacks.refreshSigma(true, false);
							});
						}
					}, 50);
				});

				_loadedData = [];
				_timeout = 0;
			}
		};

		function loadDataForLayout(result, node){
			if(result !== undefined){
				_loadedData.push(result);
			}

			else{
				var newNode = {id: node.id};
				newNode[_critX] = (_critX + " empty");
				newNode[_critY] = (_critY + " empty");
				_loadedData.push(newNode);
			}

			window.clearTimeout(_timeout);
			_timeout = window.setTimeout(function() {
				doLayout(_name, undefined, true);
			}, 100);
		};

		function reset(){
			_newNodeShape =  _standartNodeShape || "";
			kill(true);
		};

		function handleSigmaEvent(event){
			_newNodeShape =  _standartNodeShape || "";
			kill(false);
		};

		function onDataChanged(event){
			var name = event.data.source;
			if(name === 'undo')
			 	return;

			 var o, n, p;
			_critY === undefined ? o = "": o = new String(_critY) ;

			if(name !== _name){
			 	_undoStack.push({self: false, critY: o.toString()});
			 	kill();
			}
			else{
				_undoStack.push({self: true, critY: o.toString()});
			}
		};

		function undo(){
			if(_undoStack.length <= 1){
				_newNodeShape = _standartNodeShape;
				kill(true);
			}

			else{
				_undoStack.splice(_undoStack.length -1, 1);
				var opCritY = _undoStack[_undoStack.length - 1].critY || "";

				if(_undoStack[_undoStack.length - 1].self === true){
					_newNodeShape	= _nodesShape;
					sigma.layouts.swimlane.showGrid(_s, {critY: opCritY});
					$.each(_s.graph.nodes(), function(i, node){
						_alteredNodeShapes[node.id] = node.type;
						node.sw = true;
						node.type = _newNodeShape;
					});
				}
				else{
					_newNodeShape = _standartNodeShape;
					kill(false);
				}
			}
		};

		function kill(clearSelf){
			if(!sigma.layouts.swimlane)
				return;

		 	sigma.layouts.swimlane.kill(_s);

			$.each(_s.graph.nodes(), function(i, node){
				if(_alteredNodeShapes[node.id]){
					node.type = _alteredNodeShapes[node.id];
					delete _alteredNodeShapes[node.id];
				}
				else if(node.sw)
					node.type = _s.settings('defaultNodeShape');
				delete node.sw;
			});

			_callbacks.refreshSigma(false, false);
			if(clearSelf)
				_undoStack = [];
		};

		this.name = _name;
		this.init = init;
	};
})();
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	Graphbrowser.Modules.Toolstips = function (sigmaInstance, callbacks){
		var _name = "tooltips",
			_s = sigmaInstance,
			_callbacks = callbacks,
			_tooltip,
			_page,
			_onShow,
			_onError,
			_config = {},
			_nodesEnabled = false,
			_nodeShowOn,
			_nodeHideOn,
			_nodeCssClass,
			_nodePosition,
			_nodeRenderer,
			_edgesEnabled = false,
			_edgeShowOn,
			_edgeHideOn,
			_edgeCssClass,
			_edgePosition,
			_edgeRenderer;

		function init(settings){
			//Check the settings
			if(settings !== undefined){
				if(settings.node){
					_nodesEnabled = true;
					settings.node.showOn !== undefined ? _nodeShowOn = settings.node.showOn : _nodeShowOn = 'rightClickNode';
					settings.node.hideOn !== undefined ? _nodeHideOn = settings.node.hideOn : _nodeHideOn = 'clickStage';
					settings.node.position !== undefined ? _nodePosition = settings.node.position : _nodePosition = 'bottom';
					settings.node.cssClass !== undefined ? _nodeCssClass = settings.node.cssClass : _nodeCssClass = 'node-tooltip-container';
					(settings.node.renderer !== undefined && typeof settings.node.renderer === 'function') ? _nodeRenderer = settings.node.renderer : _nodeRenderer = undefined;
				}

				if(settings.edge){
					_edgesEnabled = true;
					settings.edge.showOn !== undefined ? _edgeShowOn = settings.edge.showOn : _edgeShowOn = 'rightClickEdge';
					settings.edge.hideOn !== undefined ? _edgeHideOn = settings.edge.hideOn : _edgeHideOn = 'clickStage';
					settings.edge.position !== undefined ? _edgePosition = settings.edge.position : _edgePosition = 'bottom';
					settings.edge.cssClass !== undefined ? _edgeCssClass = settings.edge.cssClass : _edgeCssClass = 'edge-tooltip-container';
					(settings.edge.renderer !== undefined && typeof settings.edge.renderer === 'function') ? _edgeRenderer = settings.edge.renderer : _edgeRenderer = undefined;
				}

				settings.page !== undefined ? _page = settings.page : _page = undefined;
				(settings.onShow !== undefined && typeof settings.onShow === 'function') ? _onShow = settings.onShow : _onShow  = undefined;
				(settings.onError !== undefined && typeof settings.onError === 'function') ? _onError = settings.onError : _onError = undefined;

				if(_nodesEnabled){
					_config.node = {
						show: _nodeShowOn,
						hide: _nodeHideOn,
						position: _nodePosition,
						template: '<div id="node-tooltip-container" class="' + _nodeCssClass + '"></div>',
						renderer: function(node, template) {
							if(_nodeRenderer)
								return _nodeRenderer(node);
							else{
								self.loadStructrPageContent(node);
								return "";
							}
						}
					}
				}

				if(_edgesEnabled){
					_config.edge = {
						show: _edgeShowOn,
						hide: _edgeHideOn,
						position: _edgePosition,
						template: '<div id="edge-tooltip-container" class="' + _edgeCssClass + '"></div>',
						renderer: function(edge, template) {
							if(_edgeRenderer)
								return _edgeRenderer(edge);
							else{
								self.loadStructrPageContent(edge);
								return "";
							}
						}
					}
				}

				_tooltip = sigma.plugins.tooltips(_s, _s.renderers[0], _config);
			}

			_callbacks.api.closeTooltip = closeTooltip;
			_callbacks.api.openTooltip = openTooltip;
		};

		function closeTooltip(){
			_tooltip.close();
		};

		function openTooltip(node){
			if(typeof node === 'string')
				node = _s.graph.nodes(node);

			var prefix = "read_" + _s.renderers[0].camera.prefix;

			if(node){
				_tooltip.open(node, _config.node, node[_s.camera.readPrefix + 'x'], node[_s.camera.readPrefix + 'y']);
			}
		};

		function loadStructrPageContent(element){
			if(_page)
				_callbacks.conn.getStructrPage(_page, element.id,onContentLoaded, _onError);
			else if(_nodeRenderer)
				self.onContentLoaded(_nodeRenderer(element), node.id);
		};

		function onContentLoaded(content, nodeId){
			$('#tooltip-container').append(content);

			$(document).on('click', '#tooltip-btnCloseTooltip-' + nodeId,  function() {
				_tooltip.close();
			});

			if(_onShow !== undefined)
				_onShow(node);
		};

		this.name = _name;
		this.init = init;
	};
})();