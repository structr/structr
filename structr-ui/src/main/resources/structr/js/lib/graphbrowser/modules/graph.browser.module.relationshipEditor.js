var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	var _s, _callbacks, _worker, _schemaInformation, _self, _inKey, _outKey, _pressedKeys, _deleteEvent, _onDeleteRelation, _maxDistance, _bound;

	Graphbrowser.Modules.RelationshipEditor = function(sigmaInstance, callbacks){
		_self = this;
		_self.name = 'relationshipEditor';
		_self.active = true;
		_s = sigmaInstance;
		_callbacks = callbacks;
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.types = ["startListener", "dataChangedHandler"];

	Graphbrowser.Modules.RelationshipEditor.prototype.init = function(settings){
		if(settings){
			settings.incommingRelationsKey !== undefined ? _inKey =	settings.incommingRelationsKey : _inKey = "shiftKey";
			settings.outgoingRelationsKey !== undefined ? _outKey = settings.outgoingRelationsKey : _outKey = "ctrlKey";
			settings.deleteEvent !== undefined ? _deleteEvent = settings.deleteEvent : _deleteEvent = "rightClickEdges";
			settings.onDeleteRelation !== undefined ? _onDeleteRelation = settings.onDeleteRelation : _onDeleteRelation = undefined;
			settings.maxDistance !== undefined ? _maxDistance = settings.maxDistance : _maxDistance = 200;
		}
		else
			throw new Error("Graph-Browser-RelationshipEditor: No settings found!");

		if(typeof self.getRelationshipEditorWorker === undefined)
			throw new Error("Graph-Browser-RelationshipEditor: Worker not found.");
		
		var workerString = _self.getRelationshipEditorWorker();

		if (window.Worker) {
			var blob = new Blob([workerString], {type: 'application/javascript'});
			_worker = new Worker(URL.createObjectURL(blob));
			_worker.onmessage = _self.onmessage;
		}
		else{
			console.log("Graph-Browser-RelationshipEditor: It seems that your browser does not support webworkers.");
		}
		_pressedKeys = {shiftKey: false, ctrlKey: false, altKey: false, noKey: true}
		_bound = false;
		_callbacks.sigmaCtrl.bindSigmaEvent('keyup', _self.handleKeyEvent);
		_callbacks.sigmaCtrl.bindSigmaEvent('keydown', _self.handleKeyEvent);
		_callbacks.sigmaCtrl.bindSigmaEvent(_deleteEvent, _self.handleSigmaEvent);
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.start = function(){
		_callbacks.conn.getSchemaInformation(this.onSchemaInformationLoaded);		
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.active = function(){
		_self.active = true;
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.deactivate = function(){
		_self.active = false;
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.handleKeyEvent = function(keys){
		_pressedKeys = keys;

		if(!_pressedKeys.noKey && !_bound){
			_bound = true;
			_callbacks.sigmaCtrl.bindSigmaEvent('drag', _self.handleSigmaEvent);
			_callbacks.sigmaCtrl.bindSigmaEvent('dragend', _self.handleSigmaEvent);
		}

		else{			
			_bound = false;
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
			case 'drag':
				_worker.postMessage({msg: "handleDrag", dragedNode: event.data.node, keys: _pressedKeys});				
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
				//console.log(event.data.text);
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
		_s.graph.addEdge(edge);
		//_s.refresh({skipIndexation: false});
		_callbacks.refreshSigma();
		//_callbac//ks.refreshSigma();
		_callbacks.refreshSigma();
		_self.onDataChanged();
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.removeNewEdge = function(edge){
		var replaced = edge.replaced;
		var delEdge = _s.graph.edges(edge);
		//console.log('Graph-Browser-RelationshipEditor: Drop edge: '+ edge);
		if(delEdge)
				if(!delEdge.lock)
					_s.graph.dropEdge(edge);
		if (replaced && _s.graph.edges(replaced) !== undefined){
			_s.graph.edges(replaced).hidden = false;
		}
		//_s.refresh({skipIndexation: false});
		_callbacks.refreshSigma();
		_self.onDataChanged();
	};	

	Graphbrowser.Modules.RelationshipEditor.prototype.hideEdge = function(edgeId){
		var edge = _s.graph.edges(edgeId);
		//console.log("hide: " + edge.label + ":" + edge.id);
		edge.hidden = true;
		edge.removed = true;
		//_s.refresh({skipIndexation: true});
		_callbacks.refreshSigma();
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.unhideEdge = function(edgeId){
		var edge = _s.graph.edges(edgeId);
		//console.log("unhide: " + edge.label + ":" + edge.id);
		edge.hidden = false;
		edge.removed = false;
		edge.replaced = undefined;
		//_s.refresh({skipIndexation: true});
		_callbacks.refreshSigma();
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
				//_s.refresh({skipIndexation: false});
				_callbacks.refreshSigma();
				_self.onDataChanged();
			});
		}
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.handleDragend = function(node){		
		var t = 0;
		$.each(_s.graph.edges(), function(n, edge){
			if (edge.added) {
				t++;
				edge.lock = true;
				if(edge.replaced !== undefined){
					_s.graph.dropEdge(edge.replaced);
				}

				_callbacks.conn.createRelationship(edge.source, edge.target, edge.relType, function(rel){
					var temp = edge;
					_s.graph.dropEdge(edge.id);

					temp.id = rel[0];
					temp.color = '#999';
					temp.size = _s.settings("minEdgeSize");
					temp.type = "arrow";
					temp.added = false;
					temp.replaced = undefined;
					temp.lock = false;
					_s.graph.addEdge(temp);
					_s.refresh({skipIndexation: false});
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
		//console.log(_schemaInformation);
		_worker.postMessage({msg: "init", settings: {schema: _schemaInformation, maxDistance: _maxDistance, nodes: _s.graph.nodes(), edges: _s.graph.edges()}});
	};

	Graphbrowser.Modules.RelationshipEditor.prototype.onDataChanged = function(){
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
								size: _s.settings('minEdgeSize'),
								type: 'arrow', color: '#999'
							});
						}
						_s.graph.dropEdge(errorEdgeId);							
						return false;
					}
				})
			});
		}
		else
			_s.graph.dropEdge(errorEdgeId);						
	};

}).call(window);