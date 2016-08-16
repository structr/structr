var Graphbrowser = Graphbrowser || {};
Graphbrowser.Control = Graphbrowser.Control || {};

var animating = animating || undefined;

(function() {
	'use strict';

	var _s , _callbacks, _sigmaSettings, _sigmaContainer, _lassoSettings, _dragListener, _activeState, _lasso, _keyboard, _select;
	var _hasDragged = false, _shiftKey = false, _ctrlKey = false, _altKey = false;
	var _keyupHandlers = [],  _keydownHandlers = [];

	Graphbrowser.Control.SigmaControl = function(callbacks, conf){
		var self = this;
		self.name = 'SigmaControl';
		_callbacks = callbacks;
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
	};

	Graphbrowser.Control.SigmaControl.prototype.kill = function() {
		var self = this;
		$(window).unbind("keyup", self.keyUpFunction);
		$(window).unbind("keydown", self.keyDownFunction);

		_keyupHandlers = [];
		_keydownHandlers = [];

		_s.kill();
		_s = undefined;
	};

	Graphbrowser.Control.SigmaControl.prototype.keyUpFunction = function(e) {
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

	Graphbrowser.Control.SigmaControl.prototype.keyDownFunction = function(e) {
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

	Graphbrowser.Control.SigmaControl.prototype.init = function() {
		var self = this;

		sigma.renderers.def = sigma.renderers.canvas;
		_s = new sigma({
			container: _sigmaContainer,
			settings: _sigmaSettings
		});

		_activeState = sigma.plugins.activeState(_s);

		_keyboard = sigma.plugins.keyboard(_s, _s.renderers[0]);

		_select = sigma.plugins.select(_s, _activeState);
		_select.bindKeyboard(_keyboard);

		_dragListener = new sigma.plugins.dragNodes(_s, _s.renderers[0], _activeState);

		_lasso = new sigma.plugins.lasso(_s, _s.renderers[0], _lassoSettings);

		_select.bindLasso(_lasso);
		self.activate();

		$(window).keyup(self.keyUpFunction);
		$(window).on('keydown', self.keyDownFunction);

		//for other modules who want to listen to sigmaevents or manipulate the behaviour of the plugins
		_callbacks.sigmaCtrl = {};
		_callbacks.sigmaPlugins = {};
		_callbacks.sigmaCtrl.unbindSigmaEvent = self.unbindEvent;
		_callbacks.sigmaCtrl.bindSigmaEvent = self.bindEvent;
		_callbacks.sigmaPlugins.activeState = _activeState;
		_callbacks.sigmaPlugins.keyboard = _keyboard;
		_callbacks.sigmaPlugins.select = _select;
		_callbacks.sigmaPlugins.lasso = _lasso;

		_callbacks.api.bindEvent = self.bindEvent.bind(self);
		_callbacks.api.unbindEvent = self.unbindEvent.bind(self);
		_callbacks.api.addNode = self.addNode.bind(self);
		_callbacks.api.addEdge = self.addEdge.bind(self);
		_callbacks.api.dropNode = self.dropNode.bind(self);
		_callbacks.api.dropNodes = self.dropNodes.bind(self);
		_callbacks.api.dropEdge = self.dropEdge.bind(self);
		_callbacks.api.dropEdges = self.dropEdges.bind(self);
		_callbacks.api.dropElement = self.dropElement.bind(self);
		_callbacks.api.updateNode = self.updateNode.bind(self);
		_callbacks.api.updateEdge = self.updateEdge.bind(self);
		_callbacks.api.getCameraRatio = self.getCameraRatio.bind(self);
		_callbacks.api.getNode = self.getNode.bind(self);
		_callbacks.api.getEdge = self.getEdge.bind(self);
		_callbacks.api.getNodes = self.getNodes.bind(self);
		_callbacks.api.getEdges = self.getEdges.bind(self);
		_callbacks.api.findRelationships = self.findRelationships.bind(self);
		_callbacks.api.changeSigmaSetting = self.changeSigmaSetting.bind(self);

		return _s;
	};

	Graphbrowser.Control.SigmaControl.prototype.activate = function() {
		var self = this;
		self.isActive = true;
	};

	Graphbrowser.Control.SigmaControl.prototype.deactivate = function() {
		_callbacks.refreshSigma();
	};


	/**
	* Function to unbind a handler function from an event
	*
	* @param  {string}	event       the name of the event
	* @param  {object}   handler 	the function handler that was bound to the event
	*/
	Graphbrowser.Control.SigmaControl.prototype.unbindEvent = function(event, handler) {
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

	/**
	* Function to bind a handler function to a given event
	*
	* @param  {string}	event       the name of the event
	* @param  {object}   handler 	the function handler that was bound to the event
	*/
	Graphbrowser.Control.SigmaControl.prototype.bindEvent = function(event, handler) {
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

	/**
	* Function that adds a node to the graphBrowsers graph
	*
	* @param  {object, string}   node 	the node that should be added to the browser, or just the id that a new node should have.
	*/
	Graphbrowser.Control.SigmaControl.prototype.addNode = function(node) {
		if(typeof node === 'string'){
			_s.graph.addNode({id:node, label: "", color: _s.settings('defaultNodeColor'), size: _s.settings('defaultNodeSize')});
		}
		else{
			_s.graph.addNode(node);
		}
		_s.refresh();
	};

	/**
	* Function that adds a node to the graphBrowsers graph
	*
	* @param  	{object, string}   	node 	the node that should be added to the browser, or just the id the new edge should have
	* @param 	{string}			source	the source node id of the new edge. Only nessesary, when the function is called with the id of the new edge
	* @param 	{string}			target	the target node id of the new edge. Only nessesary, when the function is called with the id of the new edge
	*/
	Graphbrowser.Control.SigmaControl.prototype.addEdge = function(edge, source, target) {
		if(source !== undefined && target !== undefined && (typeof edge === "string")){
			_s.graph.addEdge({id: edge, source: source, target: target, color: _s.settings('defaultEdgeColor'), size: _s.settings('defaultEdgeSize')})
		}
		else if(typeof edge === 'object'){
			_s.graph.addEdge(edge);
		}
		_s.refresh();

	};

	/**
	* Function that drops a node from the graph browsers graph
	*
	* @param  	{object, string}   	node 	the node, or the id of the node that should be dropped
	*/
	Graphbrowser.Control.SigmaControl.prototype.dropNode = function(node) {
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

	/**
	* Function that drops a collection of nodes from the graph browser
	*
	* @param  	{array}   	nodes 	the array of node ids
	* @param 	{boolean}	keep	if true, all nodes except the ones given in the array will be droppen. If false, all given nodes will be dropped.
	*/
	Graphbrowser.Control.SigmaControl.prototype.dropNodes = function(nodes, keep){
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

	/**
	* Function that drops an edge from the graph browsers graph
	*
	* @param  	{object, string}   	edge 	the edge, or the id of the edge that should be dropped
	*/
	Graphbrowser.Control.SigmaControl.prototype.dropEdge = function(edge) {
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

	/**
	* Function that drops a collection of edges from the graph browser
	*
	* @param  	{array}   	nodes 	the array of edge ids
	* @param 	{boolean}	keep	if true, all edges except the ones given in the array will be droppen. If false, all given edges will be dropped.
	*/
	Graphbrowser.Control.SigmaControl.prototype.dropEdges = function(edges, keep){
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

	/**
	* Function that drops an element from the graph browsers graph
	*
	* @param  	{string}   	element 	the id of the element that should be dropped
	*/
	Graphbrowser.Control.SigmaControl.prototype.dropElement = function(element) {
		if(_s.graph.nodes(element)){
			_s.graph.dropNode(element);
		}

		else if(_s.graph.edges(element)){
			_s.graph.dropEdge(element);
		}

		_s.refresh();
	};

	/**
	* Function that updates the properties of a node
	*
	* @param  	{string}   	node 	the id of the node
	* @param 	{object}	obj 	the object with properties wich will override the properties of the given node
	* @param 	{array}		fields	an optional array of properties that should be added to the properties of the given node
	* @param 	{object}	map 	an object that maps one property of the node to an other property.
	*/
	Graphbrowser.Control.SigmaControl.prototype.updateNode = function(node, obj, fields, map) {
		var gNode = _s.graph.nodes(node);

		if(gNode === undefined)
			return;

		if(obj.id)
			gNode.id = obj.id;
		if(obj.label)
			gNode.label = obj.label;
		if(obj.color)
			gNode.color = obj.color;
		if(obj.size)
			gNode.size = obj.size;
		if(obj.nodeType)
			gNode.nodeType = obj.nodeType;
		if(obj.x)
			gNode.x = obj.x;
		if(obj.y)
			gNode.y = obj.y;

		if($.isArray(fields)){
			for(var i = 0; i < fields.length; i++){
				gNode[fields[i]] = obj[fields[i]];
			}
		}

		if(typeof map === 'object'){
			for(var mapPair in map){
				gNode[mapPair] = gNode[map[mapPair]];
			}
		}
		_s.refresh({skipIndexation: false});
	};

	/**
	* Function that updates the properties of a edge
	*
	* @param  	{string}   	edge 	the id of the edge
	* @param 	{object}	obj 	the object with properties wich will override the properties of the given edge
	* @param 	{array}		fields	an optional array of properties that should be added to the properties of the given edge
	* @param 	{object}	map 	an object that maps one property of the edge to an other property.
	*/
	Graphbrowser.Control.SigmaControl.prototype.updateEdge = function(edge, obj, fields, map) {
		var gEdge = _s.graph.edges(edge);

		if(gEdge === undefined)
			return;

		if(obj.id)
			gEdge.id = obj.id;
		if(obj.source)
			gEdge.source = obj.source;
		if(obj.target)
			gEdge.target = obj.target;
		if(obj.size)
			gEdge.size = obj.size;
		if(obj.type)
			gEdge.type = obj.type;
		if(obj.relType)
			gEdge.relType = obj.relType;
		if(obj.relName)
			gEdge.relName = obj.relName;
		if(obj.color)
			gEdge.color = obj.color;
		if(obj.label)
			gEdge.label = obj.label;

		if($.isArray(fields)){
			for(var i = 0; i < fields.length; i++){
				gEdge[fields[i]] = obj[fields[i]];
			}
		}

		if(typeof map === 'object'){
			for(var mapPair in map){
				gEdge[mapPair] = gNode[map[mapPair]];
			}
		}
		_s.refresh({skipIndexation: false});
	};

	/**
	* Function that returns the current camera ratio of the graph browser
	*
	* @returns 	{number}   	the current camera ratio
	*/
	Graphbrowser.Control.SigmaControl.prototype.getCameraRatio = function() {
		return _s.camera.ratio;
	};

	/**
	* Function that returns the complete set of nodes in the graph browser
	*
	* @returns 	{Array}   	the nodes of the graph browser
	*/
	Graphbrowser.Control.SigmaControl.prototype.getNodes = function() {
		return _s.graph.nodes();
	};

	/**
	* Function that returns the complete set of edges in the graph browser
	*
	* @returns 	{Array}   	the edges of the graph browser
	*/
	Graphbrowser.Control.SigmaControl.prototype.getEdges = function() {
		return _s.graph.edges();
	};

	/**
	* Function that returns a node
	*
	* @param	{string}	node 	the id of the node
	* @returns 	{object}   	the node that matches the given id
	*/
	Graphbrowser.Control.SigmaControl.prototype.getNode = function(node) {
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

	/**
	* Function that returns a edge
	*
	* @param	{string}	edge 	the id of the edge
	* @returns 	{object}   	the edge that matches the given id
	*/
	Graphbrowser.Control.SigmaControl.prototype.getEdge = function(edge) {
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

	/**
	* Function that return an array of edges that connect the same nodes
	*
	* @param	{string}	sourceId 	the id of the source node
	* @param	{string}	targedId 	the id of the target nodes
	* @param	{string}	relType 	if set, the function only matches relationships with the given type
	* @returns 	{object}   	the relationships between the nodes
	*/
	Graphbrowser.Control.SigmaControl.prototype.findRelationships = function(sourceId, targedId, relType) {
        var edges = [];
        _s.graph.edges().forEach(function(edge) {
            if (edge.source === sourceId && edge.target === targedId && (!relType || edge.relType === relType)) {
                edges.push(edge);
            }
        });
        return edges;
    },


	/**
	* Function that will override a given setting of the sigma instance of the graph browser
	*
	* @param 	{string}			setting 	the name of the setting
	* @param 	{number, string}	value		the new value of the sigma setting
	* @returns  {object}   	the edge that matches the given id
	*/
	Graphbrowser.Control.SigmaControl.prototype.changeSigmaSetting = function(setting, value) {
		if(typeof setting !== 'string')
			return;
		_s.settings(setting, value);
		_s.refresh({skipIndexation: true});
	};

}).call(window);

var Graphbrowser = Graphbrowser || {};
Graphbrowser.Control = Graphbrowser.Control || {};

(function() {
	'use strict';

	var _callbackMethods;

	Graphbrowser.Control.ConnectionControl = function(callbackMethods){
		var self = this;
		self.name = 'ConnectionControl';
		_callbackMethods = callbackMethods;
	};

	Graphbrowser.Control.ConnectionControl.prototype.init =  function(){
		var self = this;

		_callbackMethods.conn = {
			getNodes: self.getNodes.bind(self),
			getData: self.getData.bind(self),
			saveGraphView: self.saveGraphView.bind(self),
			getGraphViews: self.getGraphViews.bind(self),
			getGraphViewData: self.getGraphViewData.bind(self),
			getSchemaInformation: self.getSchemaInformation.bind(self),
			createRelationship: self.createRelationship.bind(self),
			deleteRelationship: self.deleteRelationship.bind(self),
			getStructrPage: self.getStructrPage.bind(self),
			getRelationshipsOfType: self.getRelationshipsOfType.bind(self)
		};
	};

	Graphbrowser.Control.ConnectionControl.prototype.sendRequest = function(url, method, data, contentType){
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

	Graphbrowser.Control.ConnectionControl.prototype.restRequest = function(ajax, onResult, attr, onError){
		ajax.done(function(data, textStatus) {
			onResult(data.result, attr);
		}).fail(function(jqXHR, textStatus, errorThrown){
			console.log("Graph-Browser-Rest: Status " + jqXHR.status + " - ERROR: " + errorThrown);
			if(onError)	onError(attr, jqXHR.responseJSON.message);
		});
	};

	Graphbrowser.Control.ConnectionControl.prototype.htmlRequest = function(ajax, onResult, attr, onError){
		ajax.done(function(html, textStatus) {
			onResult(html, attr);
		}).fail(function(jqXHR, textStatus, errorThrown){
			console.log("Graph-Browser-Rest: Status " + jqXHR.status + " - ERROR: " + errorThrown);
			if(onError)	onError(attr, jqXHR.responseJSON.message);
		});
	};

	Graphbrowser.Control.ConnectionControl.prototype.getNodes = function(node, inOrOut, onResult, view){
		var self = this;
		if(!view) view = "_graph"
		var url = "/structr/rest/" + node.id + "/" + inOrOut + "/" + view;
		var ajax = self.sendRequest(url, "GET");
		self.restRequest(ajax, onResult, node);
	};

	Graphbrowser.Control.ConnectionControl.prototype.getData = function(data, nodeOrEdge, onResult, view){
		var self = this;
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

		var ajax = self.sendRequest(url, method, method === "POST" ? data : undefined);
		self.restRequest(ajax, onResult, nodeOrEdge);
	};

	Graphbrowser.Control.ConnectionControl.prototype.getGraphViews = function(currentId, view, onResult){
		var self = this;
		var url = '/structr/rest/GraphView/' + view + '/?viewSource=' + currentId;
		var ajax = self.sendRequest(url, "GET");
		self.restRequest(ajax, onResult);
	};

	Graphbrowser.Control.ConnectionControl.prototype.getGraphViewData = function(id, view, onResult){
		var self = this;
		var url = '/structr/rest/GraphView/' + view + '/' + id;
		var ajax = self.sendRequest(url, "GET");
		self.restRequest(ajax, onResult);
	};

	Graphbrowser.Control.ConnectionControl.prototype.saveGraphView = function(name, graph, data, viewSource, onFinished){
		var self = this;
		var newGraphview = {name: name, graph: graph, data: data, viewSource: viewSource};
		var url = '/structr/rest/GraphView'
		var ajax = self.sendRequest(url, "POST", newGraphview, 'application/json; charset=utf-8');
		self.restRequest(ajax, onFinished);
	};

	Graphbrowser.Control.ConnectionControl.prototype.getSchemaInformation = function(onResult){
		var self = this;
		var url = '/structr/rest/_schema/';
		var ajax = self.sendRequest(url, "GET");
		self.restRequest(ajax, onResult);
	};

	Graphbrowser.Control.ConnectionControl.prototype.getRelationshipsOfType = function(relType, onResult, onError){
		var self = this;
		var url = '/structr/rest/' + relType;
		var ajax = self.sendRequest(url, "GET");
		self.restRequest(ajax, onResult, undefined, onError);
	};

	Graphbrowser.Control.ConnectionControl.prototype.deleteRelationship = function(relId, onResult){
		var self = this;
		var url = '/structr/rest/' + relId;
		var ajax = self.sendRequest(url, "DELETE");
		self.restRequest(ajax, onResult);
	};

	Graphbrowser.Control.ConnectionControl.prototype.createRelationship = function(source, target, relType, onResult, attr, onError){
		var self = this;
		var newRel = {sourceId: source, targetId: target};
		var url = '/structr/rest/' + relType;
		var ajax = self.sendRequest(url, "POST", newRel);
		self.restRequest(ajax, onResult, attr, onError);
	};

	Graphbrowser.Control.ConnectionControl.prototype.getStructrPage = function(page, currentId, onResult, onError){
		var self = this;
		var url = "/" + page + "/" + currentId;
		var ajax = self.sendRequest(url, "GET");
		self.htmlRequest(ajax, onResult, currentId, onError);
	};
}).call(window);
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Control = Graphbrowser.Control || {};

(function() {
	'use strict';

	var _s = undefined;
	var _moduleSettings = undefined;
	var _attachedModules = {};
	var _refreshTimeout = undefined;
	var _callbackMethods = {};
	var _eventEmitter = {};
	var _browserEventHandlers = {};

	Graphbrowser.Control.ModuleControl = function(callbackMethods, moduleSettings, sigmaInstance){
		var self = this;
		self.name = 'ModuleControl';

		_s = sigmaInstance;
		_callbackMethods = callbackMethods;
		_refreshTimeout = 0;
		_moduleSettings = moduleSettings;

		_eventEmitter[_s.id] = {};
		sigma.classes.dispatcher.extend(_eventEmitter[_s.id]);

		_callbackMethods.eventEmitter = _eventEmitter;

		_eventEmitter[_s.id].bind('dataChanged', self.refreshSigma);
		_eventEmitter[_s.id].bind('prepareSaveGraph', self.onSave);
		_eventEmitter[_s.id].bind('restoreGraph', self.onRestore);
		_eventEmitter[_s.id].bind('pickNodes', self.onPickNodes);
		_callbackMethods.bindBrowserEvent 	= self.bindBrowserEvent;
		_callbackMethods.refreshSigma 		= self.refreshSigma.bind(self);
		_callbackMethods.filterNodes 		= self.onFilterNodes.bind(self);
		_callbackMethods.undo				= self.onUndo.bind(self);
		_callbackMethods.doLayout			= self.doLayout.bind(self);

		_callbackMethods.api.dataChanged = self.onDataChanged.bind(self);
		_callbackMethods.api.doLayout = self.doLayout.bind(self);
	};

	Graphbrowser.Control.ModuleControl.prototype.init = function(){
		var self = this;

		if(Graphbrowser.Modules){
			$.each(Graphbrowser.Modules, function(i, module){
				var newModule = new module(_s, _callbackMethods);
				newModule.init(_moduleSettings[newModule.name]);
				_attachedModules[newModule.name] = newModule;
			});
		}
	};

	Graphbrowser.Control.ModuleControl.prototype.bindBrowserEvent = function(event, handler) {
		_browserEventHandlers[event] = _browserEventHandlers[event] || [];
		_browserEventHandlers[event].push(handler);
	};

	Graphbrowser.Control.ModuleControl.prototype.kill = function() {
		_eventEmitter[_s.id].unbind('dataChanged', self.refreshSigma);
		_eventEmitter[_s.id].unbind('prepareSaveGraph', self.onSave);
		_eventEmitter[_s.id].unbind('restoreGraph', self.onRestore);
		_eventEmitter[_s.id].unbind('pickNodes', self.onPickNodes);
		//_eventEmitter[_s.id].dispatchEvent('reset');
		_eventEmitter[_s.id].dispatchEvent('kill');
		window.clearTimeout(_refreshTimeout);
		for(var m = 0; m < _attachedModules.length; m++){
			_attachedModules[m] = undefined;
		}
	};

	Graphbrowser.Control.ModuleControl.prototype.onDataChanged = function() {
		_eventEmitter[_s.id].dispatchEvent('dataChanged');
	};

	Graphbrowser.Control.ModuleControl.prototype.start = function(settings) {
		var self = this;

		$.each(_browserEventHandlers['start'], function(i, func){
			func(settings);
		});
	};

	Graphbrowser.Control.ModuleControl.prototype.refreshSigma = function(){
		window.clearTimeout(_refreshTimeout);
		_refreshTimeout = window.setTimeout(function(){
			_s.refresh({skipIndexation: false});
		}, 50);
	};

	Graphbrowser.Control.ModuleControl.prototype.onPickNodes = function(event){
		var self = this;

		var nodesToSelect = event.data.nodesToSelect;
		var onNodesSelected = event.data.onNodesSelected;
		var parentNode = event.data.parentNode;
		var parentTitle = event.data.parentTitle;

		var noNodePicker = true;

		if(!_browserEventHandlers['pickNodes']){
			onNodesSelected(nodesToSelect, parentNode);
			return;
		}

		$.each(_browserEventHandlers['pickNodes'], function(i, func){
			func(nodesToSelect, onNodesSelected, parentNode, parentTitle);
		});

	};


	Graphbrowser.Control.ModuleControl.prototype.onFilterNodes = function(nodesToFilter, inOrOut){
		var self = this;
		$.each(_browserEventHandlers['filterNodes'], function(i, func){
			nodesToFilter = func(nodesToFilter, inOrOut);
		});
		return nodesToFilter;
	};

	Graphbrowser.Control.ModuleControl.prototype.onUndo = function(){
		var self = this;
		$.each(_browserEventHandlers['undo'], function(i, func){
			func();
		});
	};

	Graphbrowser.Control.ModuleControl.prototype.doLayout = function(layout, options){
		var self = this;
		if(!(typeof layout === 'string'))
			return;
		$.each(_browserEventHandlers['doLayout'], function(i, func){
			func(layout, options);
		});
	};

	Graphbrowser.Control.ModuleControl.prototype.onSave = function(event){
		var self = this;
		var nameOfView = event.data.saveAsName;

		var data = {'name': nameOfView};
		$.each(_browserEventHandlers['saveGraph'], function(i, func){
			data[module.name] = func();
		});

		_eventEmitter.dispatchEvent('saveGraph', data);
	};

	Graphbrowser.Control.ModuleControl.prototype.onRestore = function(event){
		var self = this;
		var data = event.data.data

		$.each(_browserEventHandlers['restoreGraph'], function(i, func){
			func(data);
		});
	};

	Graphbrowser.Control.ModuleControl.prototype.status = function(){
		var self = this;
		var status = {};

		$.each(_browserEventHandlers['status'], function(i, func){
			status[module.name] = func();
		});

		return status;
	};

	Graphbrowser.Control.ModuleControl.prototype.reset = function(){
		var self = this;
		$.each(_browserEventHandlers['reset'], function(i, func){
			func();
		});
	};

}).call(window);
;(function(undefined) {
	'use strict';

	Graphbrowser = Graphbrowser || {};

	var _s, _controller = {}, _callbackMethods = {};

	var GraphBrowser = function(conf){
		var self = this;

		if(conf === undefined)
			throw new Error("Graph-Browser: No settings specified!");
		if(conf.sigmaSettings === undefined)
			console.log("Graph-Browser: No settings for Sigma.js found. Starting with default.");
		if(conf.moduleSettings === undefined)
			throw new Error("Graph-Browser: Settings for Graph-Browser-Modules are missing!");
		_callbackMethods.api = _callbackMethods.api || {};
		if(conf)
		_controller.sigmaControl = new Graphbrowser.Control.SigmaControl(_callbackMethods, conf);
		_s = _controller.sigmaControl.init();
		_controller.connectionControl = new Graphbrowser.Control.ConnectionControl(_callbackMethods);
		_controller.connectionControl.init();
		//modules at last because they need sigmaControl and connectionControl
		_controller.ModuleControl = new Graphbrowser.Control.ModuleControl(_callbackMethods, conf.moduleSettings, _s);
		_controller.ModuleControl.init();

		for(var o in _callbackMethods.api){
			self[o] = _callbackMethods.api[o];
		}
	};

	GraphBrowser.prototype.kill = function(){
		_controller.ModuleControl.kill();
		_controller.sigmaControl.kill();
		delete _controller.sigmaControl;
		delete _controller.ModuleControl;
	};

	GraphBrowser.prototype.refresh = function(){
		_controller.ModuleControl.refreshSigma(false, false);
	};

	GraphBrowser.prototype.start = function(settings){
		_controller.ModuleControl.start(settings);
		_controller.ModuleControl.refreshSigma(false, false);

	};

	GraphBrowser.prototype.getSigma = function(){
		if(_controller)
			return _s;
	};

	GraphBrowser.prototype.status = function(){
		if(_controller)
			return _controller.ModuleControl.status();
	};

	GraphBrowser.prototype.reset = function(){
		if(_controller)
			_controller.ModuleControl.reset();
	};

	if (typeof this.GraphBrowser !== 'undefined')
		throw 'An object called GraphBrowser is already in the global scope.';

	this.GraphBrowser = GraphBrowser;

}).call(window);


/****

Relationships sind übereinander.
expand buttons ausblenden, wenn node gezogen wird.
editing geht nicht.


*****/
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

	Graphbrowser.Modules.CurrentNodeTypes.prototype.init = function(settings){
		var self = this;

		if(settings){
			settings.container !== undefined ? self.createView(settings.container) :  _noView = true;
			settings.classes !== undefined ? _classes = settings.classes : _classes = '';
			_callbacks.api.getCurrentNodeTypes = self.getCurrentNodeTypes.bind(self);
			_callbacks.api.getCurrentRelTypes = self.getCurrentRelTypes.bind(self);
			_callbacks.api.highlightNodeType = self.highlightNodeType.bind(self);
			_callbacks.api.unhighlightNodeType = self.unhighlightNodeType.bind(self);
			_callbacks.api.highlightRelType = self.highlightRelType.bind(self);
			_callbacks.api.unhighlightRelType = self.unhighlightRelType.bind(self);

			_callbacks.eventEmitter[_s.id].bind('dataChanged', self.onDataChanged.bind(self));
			_callbacks.eventEmitter[_s.id].bind('start', self.start.bind(self));
		}
		else
			_noView = true;

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

	Graphbrowser.Modules.CurrentNodeTypes.prototype.onDataChanged = function(event){

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
				_callbacks.eventEmitter.dispatchEvent('hideNodeType', {nodeType: nodeType, value: true});
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
				_callbacks.eventEmitter.dispatchEvent('hideNodeType', {nodeType: nodeType});
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
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	var _s, _callbacks, _callbackOnSuccess, _callbackOnError;

	Graphbrowser.Modules.Exporter = function (sigmaInstance, callbacks){
		var self = this;
		self.name = "exporter";
		_s = sigmaInstance;
		_callbacks = callbacks;
	};

	Graphbrowser.Modules.Exporter.prototype.init = function(settings){
		var self = this;

		if(!settings)
			return;
		else{
			(settings.onSuccess !== undefined && typeof settings.onSuccess === 'function' ) ? _callbackOnSuccess = settings.onSuccess : _callbackOnSuccess = undefined;
			(settings.onError !== undefined && typeof settings.onError === 'function') ? _callbackOnError = settings.onError : _callbackOnError = undefined;

			_callbacks.eventEmitter[_s.id].bind('saveGraph', self.onSave.bind(self));
		}

		$(document).on('click', '#exporter-saveGraph-form-submit',  function(){
			self.triggerSave(true);
		});
	};

	Graphbrowser.Modules.Exporter.prototype.exportJson = function(){
		var _nodeAttributes = ['fr', 'fr_x', 'fr_y'];
		var _edgeAttributes = ['color'];

		var jsonString = _s.toJSON({removeNodeAttributes: _nodeAttributes, removeEdgeAttributes: _edgeAttributes});
	};

	Graphbrowser.Modules.Exporter.prototype.exportGefx = function(){
		var gexfString = _s.toGEXF({
			download: false,
			nodeAttributes: 'data',
			edgeAttributes: 'data',
			renderer: _s.renderers[0],
			creator: 'OP',
		});
	};

	Graphbrowser.Modules.Exporter.prototype.triggerSave = function(nameEntered){
		var self = this;
		var name = $('#exporter-saveGraph-form-name').val();
		if(name && name.length !== 0){
			_callbacks.eventEmitter.dispatchEvent('prepareSaveGraph', {saveAsName: name});
		}
		else{
			if(_callbackOnError !== undefined)
				_callbackOnError();
		}
	};

	Graphbrowser.Modules.Exporter.prototype.onSave = function(event){
		var self = this;

		var data = event.data.data;

		data['mode'] = "all";
		var _nodeAttributes = ['fr', 'fr_x', 'fr_y'];
		var _edgeAttributes = ['color'];

		var jsonGraphString = _s.toJSON({removeNodeAttributes: _nodeAttributes, removeEdgeAttributes: _edgeAttributes});
		var jsonDataString = JSON.stringify(data);
		_callbacks.conn.saveGraphView(data.name, jsonGraphString, jsonDataString, currentViewId, self.onGraphSaved.bind(self), false);
	};

	Graphbrowser.Modules.Exporter.prototype.onGraphSaved = function(){
		if(_callbackOnSuccess !== undefined)
			_callbackOnSuccess();
	};

}).call(window);
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

var animating = animating || undefined;

(function() {
	'use strict';
	var _s, _callbacks;

	var _count = 0;
	var _timeout = 0;

	Graphbrowser.Modules.Fruchtermann = function(sigmaInstance, callbacks){
		var self = this;
		self.name = 'fruchtermanReingold';
		_callbacks = callbacks;
		_s = sigmaInstance;
	};

	Graphbrowser.Modules.Fruchtermann.prototype.init = function(){
		var self = this;
		_callbacks.bindBrowserEvent('doLayout', self.doLayout.bind(self));
		$(document).on('click', '#fruchterman-controlElement',  function() {
			self.doLayout(1);
		});
	};

	Graphbrowser.Modules.Fruchtermann.prototype.start = function(){
		var self = this;
		self.doLayout(1);
	};

	Graphbrowser.Modules.Fruchtermann.prototype.doLayout = function(layout, num) {
		var self = this;

		if(layout !== self.name)
			return;

		if(num){
			self.restartLayout(num);
		}
		else{
			self.restartLayout(20);
		}
	};

	Graphbrowser.Modules.Fruchtermann.prototype.restartLayout = function(num) {
		var self = this;

		var config = {
			autoArea: false,
			area: 1000000000,
			gravity: 0,
			speed: 0.1,
			iterations: 1500,
			easing: 'quadraticInOut',
			duration: 800
		};

		var listener = sigma.layouts.fruchtermanReingold.configure(_s, config);

		listener.bind('stop', function(event) {
			window.clearTimeout(_timeout);
			_timeout = window.setTimeout(function() {
				if (_timeout) {
					_timeout = 0;
					_callbacks.eventEmitter[_s.id].dispatchEvent('dataChanged');
				}
			}, 50);
		});

		window.setTimeout(function() {
			animating = true;
			sigma.layouts.fruchtermanReingold.start(_s);
			animating = false;
			if (_count++ < num) {
				self.restartLayout(num);
			} else {
				_count = 0;
			}
		}, 40);
	};
}).call(window);
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	var _s, _callbacks, _mode, _callbackOnSuccess, _callbackOnError;
	var _loadedNodes = [];
	var _loadedEdges = [];


	Graphbrowser.Modules.Importer = function (sigmaInstance, callbacks){
		var self = this;
		self.name = "importer";
		_s = sigmaInstance;
		_callbacks = callbacks;
	};

	Graphbrowser.Modules.Importer.prototype.init = function(settings){
		var self = this;

		if(!settings)
			return;

		else{
			(settings.onSuccess !== undefined && typeof settings.onSuccess === 'function') ? _callbackOnSuccess = settings.onSuccess : _callbackOnSuccess = undefined;
			(settings.onError !== undefined && typeof settings.onError === 'function') ? _callbackOnError = settings.onError : _callbackOnError = undefined;
			$(document).on('click', '#importer-controlElement', function(){
				self.loadGraphviewsForCurrent();
			});

			$(document).on('click', '#importer-loadGraph-submit', function(){
				_mode = "all";
				self.loadSelectedView();
			});
		}
	};

	Graphbrowser.Modules.Importer.prototype.loadGraphviewsForCurrent = function(){
		var self = this;
		_callbacks.conn.getGraphViews(currentViewId, "graphBrowserInfo", self.pickGraphView.bind(self));
	};

	Graphbrowser.Modules.Importer.prototype.pickGraphView = function(resultData){
		var self = this;
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

	Graphbrowser.Modules.Importer.prototype.loadSelectedView = function(){
		var self = this;
		var selectedView = $("#importer-loadGraph-graphNames input[type='radio']:checked").val();
		if(selectedView !== undefined){
			_callbacks.conn.getGraphViewData(selectedView, "graphBrowser", self.restoreGraph.bind(self));
		}
	};

	Graphbrowser.Modules.Importer.prototype.restoreGraph = function(result){
		var graph = JSON.parse(result.graph);
		var data = JSON.parse(result.data);
		_s.graph.clear();
		_s.graph.read(graph);
		_callbacks.eventEmitter[_s.id].dispatchEvent('restoreGraph', data);
		_callbacks.eventEmitter[_s.id].dispatchEvent('dataChanged');
		_s.refresh();
		_callbackOnSuccess();
	};

}).call(window);
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	var _s, _callbacks, _onFinished, _nodeList, _parentNode, _parentTitle, _callbackOnFinished, _callbackOnChooseNodes;

	Graphbrowser.Modules.NewNodesPicker = function(sigmaInstance, callbacks){
		var self = this;
		self.name = 'newNodePicker';
		_s = sigmaInstance;
		_callbacks = callbacks;
	}

	Graphbrowser.Modules.NewNodesPicker.prototype.init = function(settings){
		var self = this;

		if(settings){
			(settings.onFinished !== undefined && typeof settings.onFinished === 'function') ? _callbackOnFinished = settings.onFinished : _callbackOnFinished = undefined;
			(settings.onChooseNodes !== undefined && typeof settings.onChooseNodes === 'function') ? _callbackOnChooseNodes = settings.onChooseNodes : _callbackOnChooseNodes = undefined;

			$(document).on('click', '#newnodepicker-chooseNodes-markAllSelected', function(){
				self.onMarkAllSelected();
			});

			$(document).on('click', '#newnodepicker-chooseNodes-reverseSelection', function(){
				self.onReverseSelection();
			});

			$(document).on('click', '#newnodepicker-chooseNodes-submit', function(){
				self.onSelected();
			});

			_callbacks.bindBrowserEvent('pickNodes', self.pickNodes.bind(self));
		}
	};

	Graphbrowser.Modules.NewNodesPicker.prototype.isActive = function(){
		return this.active;
	}

	Graphbrowser.Modules.NewNodesPicker.prototype.pickNodes = function(nodeList, onFinished, parentNode, parentTitle){
		var self = this;
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
					self.onMarkTypeSelected(node.nodeType);
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

	Graphbrowser.Modules.NewNodesPicker.prototype.onSelected = function(){
		var self = this;
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
		self.clear();
	};

	Graphbrowser.Modules.NewNodesPicker.prototype.onMarkAllSelected = function(){
		$("input[name='selectedNodeItems[]']").each(function (){
			if($(this).prop('disabled') === true)
				return true;
			$(this).prop('checked', true);
		});
	};

	Graphbrowser.Modules.NewNodesPicker.prototype.onMarkTypeSelected = function(type){
		$("input[name='selectedNodeItems[]']").each(function (){
			var val = $(this).val().split('.');
			if($(this).prop('disabled') === true)
				return true;
			if(val[0] === type){
				$(this).prop('checked', !$(this).prop('checked'));
			}
		});
	};

	Graphbrowser.Modules.NewNodesPicker.prototype.onReverseSelection = function(){
		$("input[name='selectedNodeItems[]']").each(function (){
			if($(this).prop('disabled') === true)
				return true;
			$(this).prop('checked', !$(this).prop('checked'));
		});
	};

	Graphbrowser.Modules.NewNodesPicker.prototype.clear = function(){
		_onFinished = undefined;
		_nodeList = undefined;
		_parentNode = undefined;
		_parentTitle = undefined;
	};

}).call(window);
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

var animating = animating || undefined;

(function() {
	'use strict';

	var _s, _callbacks, _infoContainer, _marginX, _marginY, _onAddNodes, _onNodesAdded, _onNodesRemoved, _currentEvent, _timeout, _nodesShape, _newNodeSize, _edgeType, _newEdgeSize;

	var _expanded = {};
	var _hoverMap = {};
	var _hoverMapPreventDuplicates = {};

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

	Graphbrowser.Modules.NodeExpander.prototype.init = function(settings){
		var self = this;

		if(settings){
			settings.container !== undefined ? _infoContainer = settings.container : _infoContainer = '';
			settings.margins.left !== undefined ? _marginX = settings.margins.left : _marginX = 0;
			settings.margins.top !== undefined ? _marginY = settings.margins.top : _marginY = 0;
			settings.onAddNodes !== undefined ? _onAddNodes = settings.onAddNodes : _onAddNodes = undefined;
			settings.onNodesAdded !== undefined ? _onNodesAdded = settings.onNodesAdded : _onNodesAdded = undefined;
			settings.onNodesRemoved !== undefined ? _onNodesRemoved = settings.onNodesRemoved : _onNodesRemoved = undefined;
			settings.newNodeSize !== undefined ? _newNodeSize = settings.newNodeSize : _newNodeSize = 10;
			settings.newEdgeSize !== undefined ? _newEdgeSize = settings.newEdgeSize : _newEdgeSize = 10;
			settings.edgeType !== undefined ? _edgeType = settings.edgeType : _edgeType = "arrow";

			if(settings.nodesShape !== undefined){
				_nodesShape = self.nodesShape;
				CustomShapes.init(_s);
			}
			else{
				_nodesShape = "";
			}

			_callbacks.bindBrowserEvent('restoreGraph', self.onRestore.bind(self));
			_callbacks.bindBrowserEvent('saveGraph', self.onSave.bind(self));
			_callbacks.bindBrowserEvent('reset', self.onReset.bind(self));

			_callbacks.eventEmitter[_s.id].bind('dataChanged', self.onDataChanged.bind(self));
			_callbacks.eventEmitter[_s.id].bind('undo', self.onUndo.bind(self));


			self.bindEvents();

			if(_infoContainer.charAt(0) !== '#')
				_infoContainer = '#' + _infoContainer;
		}


		_callbacks.api.expandNode = self.expandNode.bind(self);
		_callbacks.api.collapseNode = self.collapseNode.bind(self);
		_callbacks.api.hideExpandButtons = self.hideExpandButtons.bind(self);
	};

	Graphbrowser.Modules.NodeExpander.prototype.bindEvents = function(){
		var self = this;
		_callbacks.sigmaCtrl.bindSigmaEvent('clickNode', self.handleClickNodeEvent.bind(self));
		_callbacks.sigmaCtrl.bindSigmaEvent('doubleClickNode', self.handleDoubleClickNodeEvent.bind(self));
		_callbacks.sigmaCtrl.bindSigmaEvent('hovers', self.handleHoversEvent.bind(self));
	};

	Graphbrowser.Modules.NodeExpander.prototype.unbindEvents = function(){
		var self = this;
		_callbacks.sigmaCtrl.unbindSigmaEvent('clickNode', self.handleClickNodeEvent.bind(self));
		_callbacks.sigmaCtrl.unbindSigmaEvent('doubleClickNode', self.handleDoubleClickNodeEvent.bind(self));
		_callbacks.sigmaCtrl.unbindSigmaEvent('hovers', self.handleHoversEvent.bind(self));
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
			_callbacks.eventEmitter[_s.id].dispatchEvent('dataChanged', {source: self.name});

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
			if (!_s.graph.nodes(result.targetNode.id) && !_hoverMapPreventDuplicates[result.targetNode.id]) {
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
			self.updateHoverInfo(node, _hoverMap);
		}
	};


	Graphbrowser.Modules.NodeExpander.prototype.onNewInNodesLoaded = function(result, node){
		var self = this;
		_inNodesLoaded = true;
		var preResult = result;
		result = _callbacks.filterNodes(result, 'in');
		$.each(result, function(i, result){
			if(!_s.graph.nodes(result.sourceNode.id) && !_hoverMapPreventDuplicates[result.sourceNode.id]){
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
			self.updateHoverInfo(node, _hoverMap);
		}
	};

	Graphbrowser.Modules.NodeExpander.prototype.updateHoverInfo = function(node, hoverMap) {
		var self = this;
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
		_hoverMapPreventDuplicates = {};
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
		_callbacks.eventEmitter[_s.id].dispatchEvent('pickNodes', {nodesToSelect: _newNodes, onNodesSelected: self.onNodesSelected.bind(self), parentNode: current.id, parentTitle: current.label});
	};

	Graphbrowser.Modules.NodeExpander.prototype.addOutNode = function (nodes, edges, result, x, y) {
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
			label: targetNode.name,
			size: size,
			x: newX,
			y: newY,
			color: color[targetNode.type],
			nodeType: targetNode.type
		});

		edges.push({
			id: result.id,
			source: sourceNode.id,
			target: targetNode.id,
			size: _newEdgeSize,
			relType: result.type,
			label: result.relType,
			type: _edgeType,
			color: _s.settings('defaultEdgeColor')
		});
	};

	Graphbrowser.Modules.NodeExpander.prototype.addInNode = function(nodes, edges, result, x, y) {
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
			label: sourceNode.name,
			size: size,
			x: newX,
			y: newY,
			color: color[sourceNode.type],
			nodeType: sourceNode.type
		});

		edges.push({
			id: result.id,
			source: sourceNode.id,
			target: targetNode.id,
			size: _newEdgeSize,
			relType: result.type,
			label: result.relType,
			type: _edgeType,
			color: _s.settings('defaultEdgeColor')
		});
	};

	Graphbrowser.Modules.NodeExpander.prototype.addEdge = function (result){
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

	Graphbrowser.Modules.NodeExpander.prototype.addNodesToGraph = function(current, nodes, edges){
		var self = this;
		var added = 0;

		if(_onAddNodes !== undefined && typeof _onAddNodes === 'function')
			_onAddNodes(current);

		$.each(nodes, function(i, node){
			try {
                _s.graph.addNode({
                	id: node.id,
                	label: node.label,
                	size: node.size,
                	x: node.x,
                	y: node.y,
                	color: node.color,
                	nodeType: node.nodeType,
                	type: _nodesShape
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
				_s.graph.addEdge({
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

		_callbacks.eventEmitter[_s.id].dispatchEvent('dataChanged', {source: self.name});

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

	Graphbrowser.Modules.NodeExpander.prototype.onDataChanged = function(event){
		var self = this;
		var changeType = event.data.source;
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
		var oldData = data[self.name];
		var expanded = JSON.parse(oldData);
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

	Graphbrowser.Modules.NodeExpander.prototype.hideExpandButtons = function(){
		$(_infoContainer).empty();
	};
}).call(window);
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	var _s, _callbacks, _filterOnStart, _filterType, _filterNodeTypes;
	var _hiddenNodeTypes = {};
	var _hiddenNodes = {};
	var _hiddenRelTypes = {};
	var _hiddenRels = {};

	Graphbrowser.Modules.NodeFilter = function(sigmaInstance, callbacks){
		var self = this;
		self.name = "nodeFilter";
		_s = sigmaInstance;
		_callbacks = callbacks;
	};

	Graphbrowser.Modules.NodeFilter.prototype.init = function(settings){
		var self = this;

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

		_callbacks.eventEmitter[_s.id].bind('hideNodeType', self.hideNodeType.bind(self));
		_callbacks.eventEmitter[_s.id].bind('hideNodes', self.hideNodes.bind(self));

		_callbacks.bindBrowserEvent('filterNodes', self.filterNodes.bind(self));
		_callbacks.bindBrowserEvent('start', self.start.bind(self));

		_callbacks.api.addNodeTypeToFilter = self.addNodeTypeToFilter.bind(self);
		_callbacks.api.removeNodeTypeFromFilter = self.removeNodeTypeFromFilter.bind(self);
		_callbacks.api.setFilterType = self.setFilterType.bind(self);
		_callbacks.api.addNodeTypeToFilter = self.addNodeTypeToFilter.bind(self);
		_callbacks.api.removeNodeTypeFromFilter = self.removeNodeTypeFromFilter.bind(self);
		_callbacks.api.clearFilterNodeTypes = self.clearFilterNodeTypes.bind(self);
		_callbacks.api.filterGraph = self.filterGraph.bind(self);
		_callbacks.api.hideNodes = self.hideNodes.bind(self);
		_callbacks.api.hideNode = self.hideNode.bind(self);
		_callbacks.api.hideNodeType = self.hideNodeType.bind(self);
		_callbacks.api.hideRelType = self.hideRelType.bind(self);
		_callbacks.api.hideRels = self.hideRels.bind(self);
	};

	Graphbrowser.Modules.NodeFilter.prototype.filterNodes = function(nodesToFilter, inOrOut){
		var self = this;
		if(inOrOut === 'in'){
			return self.filterNewInNodes(nodesToFilter);
		}

		if(inOrOut === 'out'){
			return self.filterNewOutNodes(nodesToFilter);
		}
	};

	Graphbrowser.Modules.NodeFilter.prototype.filterNewInNodes = function(newInNodes){

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

	Graphbrowser.Modules.NodeFilter.prototype.filterNewOutNodes = function(newOutNodes){

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


	Graphbrowser.Modules.NodeFilter.prototype.addNodeTypeToFilter = function(nodeType){
		if(nodeType !== undefined)
			_filterNodeTypes.push(nodeType);
	};

	Graphbrowser.Modules.NodeFilter.prototype.setFilterType = function(filterType){
		if(filterType !== undefined){
				if(filterType === "whitelist" || filterType === "blacklist")
					_filterType = filterType;
		}
	};

	Graphbrowser.Modules.NodeFilter.prototype.removeNodeTypeFromFilter = function(nodeType){
		if(nodeType !== undefined)
			_filterNodeTypes.splice(_filterNodeTypes.indexOf(nodeType), 1);
	};

	Graphbrowser.Modules.NodeFilter.prototype.clearFilterNodeTypes = function(){
		_filterNodeTypes = [];
	};

	Graphbrowser.Modules.NodeFilter.prototype.hideNodeType = function(event, value){
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

	Graphbrowser.Modules.NodeFilter.prototype.hideNodes = function(event){

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

	Graphbrowser.Modules.NodeFilter.prototype.hideRelType = function(relName, status){

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

	Graphbrowser.Modules.NodeFilter.prototype.hideRels = function(edges, status){
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

	Graphbrowser.Modules.NodeFilter.prototype.filterGraph = function(){
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

	Graphbrowser.Modules.NodeFilter.prototype.hideNode = function(id, status){
		var node = _s.graph.nodes(id);

		if(node){
			node.hidden = status;
			_s.refresh({skipIndexation: true});
		}
	};

	Graphbrowser.Modules.NodeFilter.prototype.start = function(){
		var self = this;
		if(_filterOnStart && _filterNodeTypes.length > 0)
			self.filterGraph();
	};

}).call(window);
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	var _s, _callbacks, _worker, _schemaInformation, _self, _inKey, _outKey, _pressedKeys, _deleteEvent, _onDeleteRelation, _maxDistance, _bound;

	Graphbrowser.Modules.RelationshipEditor = function(sigmaInstance, callbacks){
		_self = this;
		this.name = 'relationshipEditor';
		_self.active = true;
		_s = sigmaInstance;
		_callbacks = callbacks;
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.init = function(settings){
		var self = this;
		if(settings){
			settings.incommingRelationsKey !== undefined ? _inKey =	settings.incommingRelationsKey : _inKey = "shiftKey";
			settings.outgoingRelationsKey !== undefined ? _outKey = settings.outgoingRelationsKey : _outKey = "ctrlKey";
			settings.deleteEvent !== undefined ? _deleteEvent = settings.deleteEvent : _deleteEvent = "rightClickEdges";
			settings.onDeleteRelation !== undefined ? _onDeleteRelation = settings.onDeleteRelation : _onDeleteRelation = undefined;
			settings.maxDistance !== undefined ? _maxDistance = settings.maxDistance : _maxDistance = 200;

			if(typeof self.getRelationshipEditorWorker === undefined)
				throw new Error("Graph-Browser-RelationshipEditor: Worker not found.");

			var workerString = _self.getRelationshipEditorWorker();

			if (window.Worker && !_worker) {
				var blob = new Blob([workerString], {type: 'application/javascript'});
				_worker = new Worker(URL.createObjectURL(blob));
				_worker.onmessage = _self.onmessage;
				_callbacks.conn.getSchemaInformation(this.onSchemaInformationLoaded);
			}
			else{
				console.log("Graph-Browser-RelationshipEditor: It seems that your browser does not support webworkers.");
			}

			_pressedKeys = {shiftKey: false, ctrlKey: false, altKey: false, noKey: true}
			_bound = false;

			self.active = true;
			self.bindEvents();
		}
		else
			self.active = false
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.bindEvents = function(){
		var self = this;
		_callbacks.sigmaCtrl.bindSigmaEvent('keyup', _self.handleKeyEvent);
		_callbacks.sigmaCtrl.bindSigmaEvent('keydown', _self.handleKeyEvent);
		_callbacks.sigmaCtrl.bindSigmaEvent(_deleteEvent, _self.handleSigmaEvent);

		_callbacks.bindBrowserEvent('start', self.start.bind(self));
		_callbacks.eventEmitter[_s.id].bind('dataChanged', self.onDataChanged.bind(self));
		_callbacks.eventEmitter[_s.id].bind('kill', self.onKill.bind(self));
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.start = function(){
		var self = this;
		if(self.active)
			_callbacks.conn.getSchemaInformation(this.onSchemaInformationLoaded);
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.onKill = function(){
		var self = this;
		_callbacks.sigmaCtrl.unbindSigmaEvent('keyup', _self.handleKeyEvent);
		_callbacks.sigmaCtrl.unbindSigmaEvent('keydown', _self.handleKeyEvent);
		_callbacks.sigmaCtrl.unbindSigmaEvent(_deleteEvent, _self.handleSigmaEvent);

		_callbacks.eventEmitter[_s.id].unbind('kill', self.start.bind(self));
		_callbacks.eventEmitter[_s.id].unbind('dataChanged', self.onDataChanged.bind(self));

		_worker.terminate();
		_worker = undefined;
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.active = function(){
		_self.active = true;
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.deactivate = function(){
		_self.active = false;
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.handleKeyEvent = function(keys){
		var nokey = keys.ctrlKey || keys.shiftKey;
		_pressedKeys = {
			ctrlKey: keys.ctrlKey,
			shiftKey: keys.shiftKey,
			noKey: !nokey
		};

		if(!_pressedKeys.noKey && !_bound){
			_bound = true;
			_callbacks.sigmaCtrl.bindSigmaEvent('startdrag', _self.handleSigmaEvent);
			_callbacks.sigmaCtrl.bindSigmaEvent('drag', _self.handleSigmaEvent);
			_callbacks.sigmaCtrl.bindSigmaEvent('dragend', _self.handleSigmaEvent);
		}

		else{
			_bound = false;
			_callbacks.sigmaCtrl.unbindSigmaEvent('startdrag', _self.handleSigmaEvent);
			_callbacks.sigmaCtrl.unbindSigmaEvent('drag', _self.handleSigmaEvent);
			_callbacks.sigmaCtrl.unbindSigmaEvent('dragend', _self.handleSigmaEvent);
			window.setTimeout(function(){
				_worker.postMessage({msg: "removeNewEdges", edges: _s.graph.edges(), nodes: _s.graph.nodes()});
			}, 50);
		}
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.handleSigmaEvent = function(event){
		if(!_self.active)
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
				_self.handleDragend(event.data.node);
				break;
			case _deleteEvent:
				_self.handleEdgeDeleteEvent(event.data.edge);
				break;
		}
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.onmessage = function(event){
		var eventType = event.data.msg;
		switch(eventType){
			case "add":
				//console.log("Add");
				_self.addNewEdge(event.data.edge, event.data.remove);
				break;
			case "removeNewEdge":
				//console.log("remove");
				_self.removeNewEdge(event.data.edge);
				break;
			case "hideEdge":
				//console.log("hide");
				_self.hideEdge(event.data.edge);
				break;
			case "unhideEdge":
				//console.log("unhide");
				_self.unhideEdge(event.data.edge);
				break;
			case "debug":
				console.log(event.data.text);
				break;
			default:
				//console.log("Graph-Browser-RelationshipEditor: Unknown event");
				break;
		}
	};


	Graphbrowser.Modules.RelationshipEditor.prototype.addNewEdge = function(edge, remove){
		if(remove !== undefined)
			var delEdge = _s.graph.edges(remove);
			if(delEdge)
				if(!delEdge.lock)
					_s.graph.dropEdge(remove);
		if(!_s.graph.edges(edge.id))
			_s.graph.addEdge(edge);
		sigma.canvas.edges.autoCurve(_s);
		_s.refresh({skipIndexation: false});
		_self.onDataChanged();
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.removeNewEdge = function(edge){
		var delEdge = _s.graph.edges(edge);
		//console.log('Graph-Browser-RelationshipEditor: Drop edge: '+ edge);
		if(delEdge)
			if(!delEdge.lock)
				_s.graph.dropEdge(edge);
		sigma.canvas.edges.autoCurve(_s);
		_s.refresh({skipIndexation: false});
		_self.onDataChanged();
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.hideEdge = function(edgeId){
		var edge = _s.graph.edges(edgeId);
		if(!edge.hidden){
			//console.log("hide: " + edge.label + ":" + edge.id);
			edge.hidden = true;
			sigma.canvas.edges.autoCurve(_s);
			_s.refresh({skipIndexation: true});
			//_self.onDataChanged();
		}
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.unhideEdge = function(edgeId){
		var edge = _s.graph.edges(edgeId) || {};
		if(edge.hidden){
			//console.log("unhide: " + edge.label + ":" + edge.id);
			edge.hidden = false;
			sigma.canvas.edges.autoCurve(_s);
			_s.refresh({skipIndexation: true});
			//_self.onDataChanged();
		}
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.handleEdgeDeleteEvent = function(edge, del){
		var sigmaContainer = _s.renderers[0].container;
		$('body').on('contextmenu', sigmaContainer, function(e){return false;});

		if(_onDeleteRelation !== undefined && del === undefined){
			var source = _s.graph.nodes(edge.source);
			var target = _s.graph.nodes(edge.target);
			_onDeleteRelation(edge, source, target, _self.handleEdgeDeleteEvent);
		}
		if(del){
			_callbacks.conn.deleteRelationship(edge.id, function(result){
				console.log("DeleteEdgeEvent: " + edge.id);
				_s.graph.dropEdge(edge.id);
				sigma.canvas.edges.autoCurve(_s);
				_s.refresh({skipIndexation: false});
				_self.onDataChanged();
			});
		}
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.handleDragend = function(node){
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
					_self.onDataChanged();
				}, edge.id, _self.onErrorCreatingRelation);

			}
		});
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.onSchemaInformationLoaded = function(result){
		_schemaInformation = {};
		$.each(result, function(o, res){
			_schemaInformation[res.type] = res;
		});
		_worker.postMessage({msg: "init", settings: {schema: _schemaInformation, maxDistance: _maxDistance, nodes: _s.graph.nodes(), edges: _s.graph.edges(), rendererId: _s.renderers[0].conradId}});
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.onDataChanged = function(event){
		if(_worker)
			_worker.postMessage({msg: "updateGraphInfo", nodes: _s.graph.nodes(), edges: _s.graph.edges()});
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.onErrorCreatingRelation = function(errorEdgeId, errorMessage){
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

}).call(window);
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

}).call(this);
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

var animating = animating || undefined;

(function() {
	'use strict';

	var _s, _callbacks, _currentActiveSelection;
	var _hasDragged = false;
	var _timeout = 0;
	var _activeStateSelections = {};

	Graphbrowser.Modules.SelectionTools = function(sigmaInstance, callbacks){
		var self = this;
		_s = sigmaInstance;
		_callbacks = callbacks;
		_hasDragged = false;
		self.name = 'selectionTools';
		self.isActive = false;
	};

	Graphbrowser.Modules.SelectionTools.prototype.init = function(settings) {
		var self = this;

		_callbacks.api.createSelectionGroup = self.createSelectionGroup.bind(self);
		_callbacks.api.deleteSelectionGroup = self.deleteSelectionGroup.bind(self);
		_callbacks.api.activateSelectionGroup = self.activateSelectionGroup.bind(self);
		_callbacks.api.hideSelectionGroup = self.hideSelectionGroup.bind(self);
		_callbacks.api.fixateSelectionGroup = self.fixateSelectionGroup.bind(self);
		_callbacks.api.clearSelectionGroup = self.clearSelectionGroup.bind(self);
		_callbacks.api.activateSelectionLasso = self.activateSelectionLasso.bind(self);
		_callbacks.api.deactivateSelectionTools = self.deactivateSelectionTools.bind(self);
		_callbacks.api.activateSelectionTools = self.activate.bind(self);
		_callbacks.api.selectionToolsActive = self.isActive;
		_callbacks.api.dropSelection = self.dropSelection.bind(self);

		_callbacks.bindBrowserEvent('restoreGraph', self.onRestore.bind(self));
	};

	Graphbrowser.Modules.SelectionTools.prototype.activate = function() {
		var self = this;
		if(self.isActive)
			return;
		self.isActive = true;

		_s.renderers[0].bind('render', function(e) {
			_s.renderers[0].halo({
				nodes: _callbacks.sigmaPlugins.activeState.nodes()
			});
		});

		_callbacks.sigmaPlugins.lasso.bind('selectedNodes', function (event) {
			window.clearTimeout(_timeout);
			_timeout = setTimeout(function() {

				if(_activeStateSelections[_currentActiveSelection].hidden){
					self.hideSelectionGroup(_currentActiveSelection, false);
				}
				if(_activeStateSelections[_currentActiveSelection].fixed){
					self.fixateSelectionGroup(_currentActiveSelection, false);
				}

				if(event.data.length <= 0)
					return;

				_activeStateSelections[_currentActiveSelection].nodes = event.data;

				self.activateSelectionLasso(false);
				_s.refresh({skipIndexation: true});
				self.updateActiveState();
				_timeout = 0;
			}, 10);
		});

		self.updateActiveState();
		_s.refresh({skipIndexation: true})
	};

	Graphbrowser.Modules.SelectionTools.prototype.createSelectionGroup = function() {
		var newId = (parseInt(Math.random() * (5000 - 1))).toString();
		_activeStateSelections[newId] = {id: newId, nodes: [], hidden: false, fixed: false};
		_currentActiveSelection = newId;
		return newId;
	};

	Graphbrowser.Modules.SelectionTools.prototype.hideSelectionGroup = function(groupId, status) {
		if(status === undefined)
			return;

		if(_activeStateSelections[groupId].hidden !== status){
			_activeStateSelections[groupId].hidden = status;
			_callbacks.eventEmitter[_s.id].dispatchEvent('hideNodes', {nodes: _activeStateSelections[groupId].nodes, status: status});
		}
	};

	Graphbrowser.Modules.SelectionTools.prototype.fixateSelectionGroup = function(groupId, status) {
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

	Graphbrowser.Modules.SelectionTools.prototype.clearSelectionGroup = function(groupId) {
		var self = this;
		self.onHideSelectionGroup(groupId, false);
		self.setSelectionFixed(groupId, false);
		_callbacks.sigmaPlugins.activeState.dropNodes();
		_activeStateSelections[groupId].nodes = undefined;
		_s.refresh({skipIndexation: true});
	};

	Graphbrowser.Modules.SelectionTools.prototype.deleteSelectionGroup = function(groupId) {
		var self = this;
		self.clearSelectionGroup();
		_activeStateSelections.splice(_activeStateSelections.indexof(groupId), 1);
	};

	Graphbrowser.Modules.SelectionTools.prototype.activateSelectionGroup = function(groupId) {
		var self = this;
		if(self.isActive === false || groupId === undefined){
			return;
		}
		_currentActiveSelection = groupId;
		self.updateActiveState();
	};

	Graphbrowser.Modules.SelectionTools.prototype.updateActiveState = function() {
		var self = this;

		if(!self.isActive && _activeStateSelections[_currentActiveSelection].nodes === undefined)
			return;

		_callbacks.sigmaPlugins.activeState.dropNodes();
		var an = [];
		$.each(_activeStateSelections[_currentActiveSelection].nodes, function(i, node) {
			an.push(node.id);
		});
		_callbacks.sigmaPlugins.activeState.addNodes(an);
		_s.refresh({skipIndexation: true});
	};


	Graphbrowser.Modules.SelectionTools.prototype.activateSelectionLasso = function(status) {
		var self = this;
		if((!_callbacks.sigmaPlugins.lasso.isActive) && status === true){
			if(!self.isActive)
				self.activate();
			_callbacks.sigmaPlugins.lasso.activate();
		}
		else{
			_callbacks.sigmaPlugins.lasso.deactivate();
		}
	};

	Graphbrowser.Modules.SelectionTools.prototype.deactivateSelectionTools = function(status) {
		var self = this;

		if(!self.isActive)
			return;

		self.isActive = false;

		if(_callbacks.sigmaPlugins.lasso.isActive){
			self.activateSelectionLasso(false);
		}

		_s.renderers[0].unbind('render');
		_callbacks.sigmaPlugins.activeState.dropNodes();
		_callbacks.refreshSigma(true);
	};

	Graphbrowser.Modules.SelectionTools.prototype.deactivateSelectionTools = function(status) {
		var self = this;
		self.isActive = false;

		if(_callbacks.sigmaPlugins.lasso.isActive){
			self.activateSelectionLasso(false);
		}

		_s.renderers[0].unbind('render');
		_callbacks.sigmaPlugins.activeState.dropNodes();
		_callbacks.refreshSigma(true);
	};

	Graphbrowser.Modules.SelectionTools.prototype.dropSelection = function(groupId){
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

	Graphbrowser.Modules.SelectionTools.prototype.onRestore = function() {
		var self = this;
		_activeStateSelections = [];
		self.createNewSelectionGroup();
		if(self.isActive){
			_callbacks.sigmaPlugins.activeState.dropNodes();
			self.updateActiveState();
		}
	};

}).call(window);
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	var _s, _callbacks, _status;

	var _undoStack = [];

	Graphbrowser.Modules.StatusUndo = function (sigmaInstance, callbacks){
		var self = this;
		self.name = 'statusUndo';

		_s = sigmaInstance;
		_callbacks = callbacks;

	};

	Graphbrowser.Modules.StatusUndo.prototype.init = function(settings){
		var self = this;

		if(settings){
			if(settings.activate){
				_status = 0;
				$(document).on('click', '#statusUndo-controlElement',  function() {
					self.undo();
				});
			}
		}
		_callbacks.eventEmitter[_s.id].bind('dataChanged', self.onDataChanged.bind(self));
		_callbacks.bindBrowserEvent('start', self.start.bind(self));
		_callbacks.bindBrowserEvent('status', self.status.bind(self));
		_callbacks.bindBrowserEvent('reset', self.onReset.bind(self));

		_callbacks.api.undo = self.undo.bind(self);
	};

	Graphbrowser.Modules.StatusUndo.prototype.start = function(){
		var self = this;
		self.onDataChanged();
		_status--;
	};

	Graphbrowser.Modules.StatusUndo.prototype.onDataChanged = function(event){
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

	Graphbrowser.Modules.StatusUndo.prototype.undo = function(){
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
				_s.graph.addNode(newNode);
				_s.graph.nodes(newNode.id).x = newNode.x;
				_s.graph.nodes(newNode.id).y = newNode.y;
			}
		});

		//add the old edges
		$.each(_undoStack[_undoStack.length - 1].graph.edges, function(i, edge){
			_s.graph.addEdge(edge);
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

	Graphbrowser.Modules.StatusUndo.prototype.status = function(){
		return _status;
	};

	Graphbrowser.Modules.StatusUndo.prototype.onReset = function(){
		var self = this;
		_status = 1;
		if(_undoStack.length >= 2)
			_undoStack.splice(2, _undoStack.length - 2);
		self.undo();
	};

}).call(window);
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	var _s, _callbacks, _critX, _critY, _critXType, _critYType, _view, _options, self, _timeout, _nodesShape, _standardNodeShape, _newNodeShape, _critXDbName, _critYDbName;
	var _timeoutAnim = 0;
  	var _pendingNodes = [], _loadedData = [];

  	var _undoStack = [], _undoPending = false;


	Graphbrowser.Modules.Swimlane = function(sigmaInstance, callbacks){
		var self = this;
		self.name = "swimlane";
		_s = sigmaInstance;
		_callbacks = callbacks;
	};

	Graphbrowser.Modules.Swimlane.prototype.init = function(settings){
		var self = this;

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
			settings.standardNodeShape !== undefined ? _standardNodeShape = settings.standardNodeShape : _standardNodeShape = "";

		}

		_callbacks.bindBrowserEvent('reset', self.reset.bind(self));
		_callbacks.bindBrowserEvent('undo', self.undo.bind(self).bind(self));
		_callbacks.bindBrowserEvent('doLayout', self.doLayout.bind(self));

		_callbacks.eventEmitter[_s.id].bind('dataChanged', self.onDataChanged.bind(self));

		_callbacks.sigmaCtrl.bindSigmaEvent('startdrag', self.handleSigmaEvent.bind(self));
	};

	Graphbrowser.Modules.Swimlane.prototype.doLayout = function(layout, options, second){
		var listener = undefined;
		self = this;

		if(layout !== self.name)
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
				_callbacks.conn.getData(node, "node", self.loadDataForLayout.bind(self), _view);
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
								if(data[t] !== undefined)
									node[_critX] = data[t];
								else{
									node[_critX] = '';
								}
								break;
							case 'date':
								if(data[t] !== undefined)
									node[_critX] =  Date.parse(data[t]);
								else{
									node[_critX] = 14400000;
								}
								break;
							default:
								if(data[t] !== undefined)
									node[_critX] = data[t];
								else{
									node[_critX] = 0;
								}
								break;
						}
						switch(_critYType){
							case 'string':
								if(data[u] !== undefined)
									node[_critY] = data[u];
								else{
									node[_critY] = '';
								}
								break;
							case 'date':
								if(data[u] !== undefined)
									node[_critY] =  Date.parse(data[u]);
								else{
									node[_critY] = 14400000;
								}
								break;
							default:
								if(data[u] !== undefined)
									node[_critY] = data[u];
								else{
									node[_critY] = 0;
								}
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
					_callbacks.eventEmitter[_s.id].dispatchEvent('dataChanged', {source: self.name});
					if(_nodesShape){
						CustomShapes.init(_s);
						$.each(_s.graph.nodes(), function(i, node){
							node.type = _nodesShape;
							_callbacks.refreshSigma(true, false);
						});
					}
				}, 50);
			});

			_loadedData = [];
			_timeout = 0;
		}
	};

	Graphbrowser.Modules.Swimlane.prototype.loadDataForLayout = function(result, node){
		var self = this;
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
			self.doLayout(self.name, undefined, true);
		}, 100);
	};

	Graphbrowser.Modules.Swimlane.prototype.reset = function(){
		_newNodeShape =  _standardNodeShape || "";
		this.kill(true);
	};

	Graphbrowser.Modules.Swimlane.prototype.handleSigmaEvent = function(event){
		_newNodeShape =  _standardNodeShape || "";
		this.kill(false);
	};

	Graphbrowser.Modules.Swimlane.prototype.onDataChanged = function(event){
		var name = event.data.source;
		if(name === 'undo')
		 	return;

		 var o, n, p;
		_critY === undefined ? o = "": o = new String(_critY) ;

		if(name !== this.name){
		 	_undoStack.push({self: false, critY: o.toString()});
		 	this.kill();
		}
		else{
			_undoStack.push({self: true, critY: o.toString()});
		}
	};

	Graphbrowser.Modules.Swimlane.prototype.undo = function(){
		var self = this;

		if(_undoStack.length <= 1){
			_newNodeShape = _standardNodeShape;
			self.kill(true);
		}

		else{
			_undoStack.splice(_undoStack.length -1, 1);
			var opCritY = _undoStack[_undoStack.length - 1].critY || "";

			if(_undoStack[_undoStack.length - 1].self === true){
				_newNodeShape	= _nodesShape;
				sigma.layouts.swimlane.showGrid(_s, {critY: opCritY});
				$.each(_s.graph.nodes(), function(i, node){
					node.type = _newNodeShape;
				});
			}
			else{
				_newNodeShape	= _standardNodeShape;
				self.kill(false);
			}
		}
	};

	Graphbrowser.Modules.Swimlane.prototype.kill = function(clearSelf){
		if(!sigma.layouts.swimlane)
			return;

	 	sigma.layouts.swimlane.kill(_s);

		$.each(_s.graph.nodes(), function(i, node){
			node.type = _newNodeShape;
		});
		_callbacks.refreshSigma(false, false);
		if(clearSelf)
			_undoStack = [];
	};

}).call(window);
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	var _s, _callbacks, _tooltip, _page, _onShow, _onError,
		_nodesEnabled = false, _nodeShowOn, _nodeHideOn, _nodeCssClass, _nodePosition, _nodeRenderer,
		_edgesEnabled = false, _edgeShowOn, _edgeHideOn, _edgeCssClass, _edgePosition, _edgeRenderer;

	Graphbrowser.Modules.Toolstips = function (sigmaInstance, callbacks){
		var self = this;
		self.name = "tooltips";
		_s = sigmaInstance;
		_callbacks = callbacks;
	};

	Graphbrowser.Modules.Toolstips.prototype.init = function(settings){
		var self = this;

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

			var config = {};

			if(_nodesEnabled){
				config.node = {
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
				config.edge = {
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

			_tooltip = sigma.plugins.tooltips(_s, _s.renderers[0], config);
		}

		_callbacks.api.closeTooltip = self.closeTooltip.bind(self);
	};

	Graphbrowser.Modules.Toolstips.prototype.closeTooltip = function(){
		_tooltip.close();
	};

	Graphbrowser.Modules.Toolstips.prototype.loadStructrPageContent = function(element){
		var self = this;
		if(_page)
			_callbacks.conn.getStructrPage(_page, element.id, self.onContentLoaded, _onError);
		else if(_nodeRenderer)
			self.onContentLoaded(_nodeRenderer(element), node.id);
	};

	Graphbrowser.Modules.Toolstips.prototype.onContentLoaded = function(content, nodeId){
		$('#tooltip-container').append(content);

		$(document).on('click', '#tooltip-btnCloseTooltip-' + nodeId,  function() {
			_tooltip.close();
		});

		if(_onShow !== undefined)
			_onShow(node);
	};
}).call(window);
var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

var animating = animating || undefined;

(function() {
	'use strict';

	var _s, _callbacks, _marginX, _marginY, _onAddNodes, _onNodesAdded, _onNodesRemoved, _timeout, _newNodeSize, _edgeType, _newEdgesSize;

	var _expanded = {};
	var _hoverMap = {};
	var _inNodesLoaded = false;
	var _outNodesLoaded = false;
	var _newNodes = [];
	var _newEdges = [];
	var _undoStack = [];
	var _currentHoveredNode;
	var _toggleMaxNodeSize = false;

	Graphbrowser.Modules.ZoomExpander = function(sigmaInstance, callbacks){
		var self = this;
		self.name = 'zoomExpander';
		_s = sigmaInstance;
		_callbacks = callbacks;
	};

	Graphbrowser.Modules.ZoomExpander.prototype.init = function(settings){
		var self = this;

		if(!settings)
			return;

		else{
			settings.margins.left !== undefined ? _marginX = settings.margins.left : _marginX = 0;
			settings.margins.top !== undefined ? _marginY = settings.margins.top : _marginY = 0;
			settings.onAddNodes !== undefined ? _onAddNodes = settings.onAddNodes : _onAddNodes = undefined;
			settings.onNodesAdded !== undefined ? _onNodesAdded = settings.onNodesAdded : _onNodesAdded = undefined;
			settings.onNodesRemoved !== undefined ? _onNodesRemoved = settings.onNodesRemoved : _onNodesRemoved = undefined;
			settings.newNodesSize !== undefined ? _newNodeSize = settings.newNodesSize : _newNodeSize = 10;
			settings.newEdgesSize !== undefined ? _newEdgesSize = settings.newEdgesSize : _newEdgesSize = 10;
			settings.edgeType !== undefined ? _edgeType = settings.edgeType : _edgeType = "arrow";

			self.bindEvents();
		}
	};

	Graphbrowser.Modules.ZoomExpander.prototype.bindEvents = function(){
		var self = this;
		_callbacks.sigmaCtrl.bindSigmaEvent('clickNode', self.handleClickNodeEvent.bind(self));
		_callbacks.sigmaCtrl.bindSigmaEvent('doubleClickNode', self.handleDoubleClickNodeEvent.bind(self));
		_callbacks.sigmaCtrl.bindSigmaEvent('hovers', self.handleHoversEvent.bind(self));
		_callbacks.sigmaCtrl.bindSigmaEvent('mousewheel', self.handleZoom.bind(self));
	};

	Graphbrowser.Modules.ZoomExpander.prototype.unbindEvents = function(){
		var self = this;
		_callbacks.sigmaCtrl.unbindSigmaEvent('clickNode', self.handleClickNodeEvent.bind(self));
		_callbacks.sigmaCtrl.unbindSigmaEvent('doubleClickNode', self.handleDoubleClickNodeEvent.bind(self));
		_callbacks.sigmaCtrl.unbindSigmaEvent('hovers', self.handleHoversEvent.bind(self));
		_callbacks.sigmaCtrl.unbindSigmaEvent('mousewheel', self.handleZoom.bind(self));
	};

	Graphbrowser.Modules.ZoomExpander.prototype.handleZoom = function(e){
		var self = this;
		var ratio = _s.camera.ratio;

		if(ratio < 1 && _toggleMaxNodeSize === false){
			_s.settings('maxNodeSize', 50);
			_toggleMaxNodeSize = true;
		}
		else if(ratio >= 1 && _toggleMaxNodeSize === true){
			_s.settings('maxNodeSize', 25);
			_toggleMaxNodeSize = false;
		}
		if(ratio <= 0.0625){
			_undoStack.push(_s.graph);
			//_s.camera.ratio = 1;
			_s.settings('maxNodeSize', 25);
			self.onExpandNode(_currentHoveredNode, e.data.captor.x, e.data.captor.y);
		}
		_s.refresh({skipIndexation: true});
	};

	Graphbrowser.Modules.ZoomExpander.prototype.handleClickNodeEvent = function(clickedNode){
		var self = this;
		var node = clickedNode.data.node;
	};

	Graphbrowser.Modules.ZoomExpander.prototype.handleDoubleClickNodeEvent = function(clickedNode){
		var self = this;
		var node = clickedNode.data.node;
	};

	Graphbrowser.Modules.ZoomExpander.prototype.handleHoversEvent = function(e){
		var self = this;
		if(e.data.enter.nodes.length > 0){
			self.handleOverNodeEvent(e.data.enter.nodes[0]);
		}

		else if(e.data.leave.nodes.length > 0){
			//self.handleOutNodeEvent(e.data.leave.nodes[0]);
		}
	};

	Graphbrowser.Modules.ZoomExpander.prototype.handleOverNodeEvent = function(node){
		var self = this;
		_currentHoveredNode = node;
		_newNodes = {};
		var inNodes = _callbacks.conn.getNodes(node, 'in', self.onNewInNodesLoaded.bind(self));
		var outNodes = _callbacks.conn.getNodes(node, 'out', self.onNewOutNodesLoaded.bind(self));
	};

	Graphbrowser.Modules.ZoomExpander.prototype.handleOutNodeEvent = function(node){

	};

	Graphbrowser.Modules.ZoomExpander.prototype.onNewOutNodesLoaded = function(result, node){
		var self = this;
		_outNodesLoaded = true;
		result = _callbacks.filterNodes(result, 'out');
		_newNodes['outNodes'] = result;
	};

	Graphbrowser.Modules.ZoomExpander.prototype.onNewInNodesLoaded = function(result, node){
		var self = this;
		_inNodesLoaded = true;
		result = _callbacks.filterNodes(result, 'in');
		_newNodes['inNodes'] = result;
	};

	Graphbrowser.Modules.ZoomExpander.prototype.onExpandNode = function(node, x, y) {
		var self = this;

		if(!_newNodes['inNodes'] && !_newNodes['outNodes']){
			return;
		}

		var sigmaNodes = [];
		var sigmaEdges = [];

		$.each(_newNodes['outNodes'], function(i, result) {
			self.addOutNode(sigmaNodes, sigmaEdges, result, 0, 0);
		});

		$.each(_newNodes['inNodes'], function(i, result) {
			self.addInNode(sigmaNodes, sigmaEdges, result, 0, 0);
		});

		self.addNodesToGraph(sigmaNodes, sigmaEdges, x, y);
	};

	Graphbrowser.Modules.ZoomExpander.prototype.addOutNode = function (nodes, edges, result, x, y) {
		var sourceNode = result.sourceNode;
		var targetNode = result.targetNode;
		var angle = Math.random()* Math.PI * 2;
		var newX = x + (Math.cos(angle));
		var newY = y + (Math.sin(angle));
		var size = _newNodeSize;

		nodes.push({id: targetNode.id, label: targetNode.name, size: size, x: newX, y: newY, color: color[targetNode.type], nodeType: targetNode.type});
		edges.push({id: result.id, source: sourceNode.id, target: targetNode.id, size: _newEdgesSize, relType: result.type, label: result.relType, type: _edgeType, color: _s.settings('defaultEdgeColor')});
	};

	Graphbrowser.Modules.ZoomExpander.prototype.addInNode = function(nodes, edges, result, x, y) {
		var sourceNode = result.sourceNode;
		var targetNode = result.targetNode;
		var angle = Math.random()* Math.PI * 2;
		var newX = x + (Math.cos(angle));
		var newY = y + (Math.sin(angle));
		var size = _newNodeSize;

		nodes.push({id: sourceNode.id, label: sourceNode.name, size: size, x: newX, y: newY, color: color[sourceNode.type], nodeType: sourceNode.type});
		edges.push({id: result.id, source: sourceNode.id, target: targetNode.id, size: _newEdgesSize, relType: result.type, label: result.relType, type: _edgeType, color: _s.settings('defaultEdgeColor')});
	};

	Graphbrowser.Modules.ZoomExpander.prototype.addEdge = function (result){
		var sourceNode = result.sourceNode;
		var targetNode = result.targetNode;
		var id = result.id;
		edges[result.id]     = {id: result.id, source: sourceNode.id, target: targetNode.id, size: _newEdgesSize, relType: result.type, label: result.relType, type: _edgeType, color: _s.settings('defaultEdgeColor')};
	};

	Graphbrowser.Modules.ZoomExpander.prototype.addNodesToGraph = function(nodes, edges, x, y){
		var self = this;
		_s.graph.clear();

		$.each(nodes, function(i, node){
			try {
                _s.graph.addNode({id: node.id, label: node.label, size: node.size, x: node.x, y: node.y, color: node.color, nodeType: node.nodeType});
            } catch (e) {console.log(e);}
        });

		_newNodes = [];
		_newEdges = [];
		_s.settings('maxNodeSize', 25);
		_toggleMaxNodeSize = false;

		var anim = sigma.misc.animation.camera(_s.camera,
			{
				x: 0,
	  			y: 0,
	  			ratio: 2
	  		},
	  		{
	  			easing: 'quadraticInOut',
	  			duration: 1000
	  		}
	  	);

		_s.refresh({skipIndexation: true});
		_callbacks.doLayout('swimlane', {view: 'ui', critX: 'name.string', critY: 'nodeType.string.type'});
		_callbacks.eventEmitter[_s.id].dispatchEvent('dataChanged', {source: self.name});
		if(_onNodesAdded !== undefined && typeof _onNodesAdded === 'function')
			_onNodesAdded();
	};

	Graphbrowser.Modules.ZoomExpander.prototype.expandNode = function(id){
		var self = this;
		var node = _s.graph.nodes(id);
		_showHoverInfo = false;

		var inNodes = _callbacks.conn.getNodes(node, 'in', self.onNewInNodesLoaded.bind(self));
		var outNodes = _callbacks.conn.getNodes(node, 'out', self.onNewOutNodesLoaded.bind(self));
		window.setTimeout(function(){
			self.onExpandNode(node);
		}, 20);
	};
}).call(window);