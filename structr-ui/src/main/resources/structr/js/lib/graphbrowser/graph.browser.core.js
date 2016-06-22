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
		_controller.sigmaControl = new Graphbrowser.Control.SigmaControl(_callbackMethods, conf.sigmaSettings, conf.graphContainer);
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

	GraphBrowser.prototype.doLayout = function(layout, options){
		if(typeof layout === 'string'){
			_controller.ModuleControl.doLayout(layout, options);
		}
	};

	GraphBrowser.prototype.refresh = function(){
		_controller.ModuleControl.refreshSigma(false, false);
	};

	GraphBrowser.prototype.start = function(settings){
		_controller.ModuleControl.start(settings);
		_controller.ModuleControl.refreshSigma(false, false);
		
	};

	GraphBrowser.prototype.expandNode = function(id){
		_controller.ModuleControl.expandNode(id);
	};

	GraphBrowser.prototype.getSigma = function(){
		return _s;
	};

	//nodes: array of ids or one id
	//keep: delete nodes that are not in nodes
	GraphBrowser.prototype.dropNodes = function(nodes, keep){
		if(keep){
			$.each(_s.graph.nodes(), function(i, node){
				if(typeof nodes === 'string')
					if(node.id !== nodes)
						_s.graph.dropNode(node.id);
					else{
						if(nodes.indexOf(node.id) < 0){
							_s.graph.dropNode(node);
						}		
					}
				});
		}	
		else{
			if(typeof nodes === 'string')
				_s.graph.dropNode(nodes);
			else{
				$.each(_s.graph.nodes(), function(i, node){
					if(nodes.indexOf(node.id) >= 0){
						_s.graph.dropNode(node);
					}					
				});
			}
		}
		_controller.ModuleControl.refreshSigma(false, false);
	};

	GraphBrowser.prototype.status = function(){
		return _controller.ModuleControl.status();
	};

	GraphBrowser.prototype.reset = function(){
		_controller.ModuleControl.reset();
	};

	if (typeof this.GraphBrowser !== 'undefined')
		throw 'An object called GraphBrowser is already in the global scope.';

	this.GraphBrowser = GraphBrowser;

}).call(this);