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

	Graphbrowser.Modules.NodeFilter.prototype.types = ['nodeFilter', 'nodeHider', 'startListener'];

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

		_callbacks.api.addNodeTypeToFilter = self.addNodeTypeToFilter.bind(self);
		_callbacks.api.removeNodeTypeFromFilter = self.removeNodeTypeFromFilter.bind(self);
		_callbacks.api.setFilterType = self.setFilterType.bind(self);
		_callbacks.api.addNodeTypeToFilter = self.addNodeTypeToFilter.bind(self);
		_callbacks.api.removeNodeTypeFromFilter = self.removeNodeTypeFromFilter.bind(self);
		_callbacks.api.clearFilterNodeTypes = self.clearFilterNodeTypes.bind(self);
		_callbacks.api.filterGraph = self.filterGraph.bind(self);
		_callbacks.api.hideNodes = self.hideNodes.bind(self);
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

	Graphbrowser.Modules.NodeFilter.prototype.hideNodeType = function(nodeType, status){

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

	Graphbrowser.Modules.NodeFilter.prototype.hideNodes = function(nodes, status){

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


	Graphbrowser.Modules.NodeFilter.prototype.onDataChanged = function(changedType){
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

	Graphbrowser.Modules.NodeFilter.prototype.start = function(){
		var self = this;
		if(_filterOnStart && _filterNodeTypes.length > 0)
			self.filterGraph();
	};

}).call(window);