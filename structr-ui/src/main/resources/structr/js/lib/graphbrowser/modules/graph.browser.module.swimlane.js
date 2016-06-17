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

	Graphbrowser.Modules.Swimlane.prototype.types = ['layout', 'resetListener', 'dataChangedHandler', 'undoListener'];

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
		else
			throw new Error('Graph-Browser-Swimlane: No settings found');

		_callbacks.sigmaCtrl.bindSigmaEvent('startdrag', self.handleSigmaEvent);

		$(document).on('click', '#swimlane-controlElement', self.doLayout);		
	};

	Graphbrowser.Modules.Swimlane.prototype.doLayout = function(options, second){
		var listener = undefined;
		self = this;		

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
					_callbacks.dataChanged(self.name);
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
			self.doLayout(undefined, true);
		}, 100);
	};

	Graphbrowser.Modules.Swimlane.prototype.onReset = function(){
		_newNodeShape =  _standardNodeShape || "";
		this.kill(true);
	};

	Graphbrowser.Modules.Swimlane.prototype.handleSigmaEvent = function(event){
		_newNodeShape =  _standardNodeShape || "";
		this.kill(false);
	};

	Graphbrowser.Modules.Swimlane.prototype.onDataChanged = function(name){		
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

	Graphbrowser.Modules.Swimlane.prototype.onUndo = function(){
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
	 	sigma.layouts.swimlane.kill(_s);

		$.each(_s.graph.nodes(), function(i, node){
			node.type = _newNodeShape;
		});
		_callbacks.refreshSigma(false, false);
		if(clearSelf)			
			_undoStack = [];
	};

}).call(window);