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

	Graphbrowser.Modules.StatusUndo.prototype.types = ["dataChangedHandler", "startListener", "statusHandler", "resetListener"];

	Graphbrowser.Modules.StatusUndo.prototype.init = function(){
		var self = this;
		_status = 0;
		$(document).on('click', '#statusUndo-controlElement',  function() {
			self.undo();
		});
	};

	Graphbrowser.Modules.StatusUndo.prototype.start = function(){
		var self = this;
		self.onDataChanged();
		_status--;
	};

	Graphbrowser.Modules.StatusUndo.prototype.onDataChanged = function(changeType){
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
		_callbacks.dataChanged('undo');
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