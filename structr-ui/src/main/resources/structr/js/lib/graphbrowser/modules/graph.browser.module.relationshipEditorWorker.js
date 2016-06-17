(function(undefined) {
  'use strict';

	var Worker = function(settings){

		var _previousEdges = {};
		var _schema, _maxDistance, _nodes, _edges, _newNodes, _newEdges;

		function init(settings){
			settings.schema !== undefined ? _schema = settings.schema : _schema = [];
			settings.maxDistance !== undefined ? _maxDistance = settings.maxDistance : _maxDistance = 200;
			settings.nodes !== undefined ? _nodes = settings.nodes : _nodes = [];
			settings.edges !== undefined ? _edges = settings.edges : _edges = [];
		};

		function updateGraphInfo(nodes, edges, schema) {
			_newNodes = nodes;
			_newEdges = edges;
		}

		function handleDrag(dragedNode, keys) {
			var schemaInfo = _schema[dragedNode.nodeType];

			for(var counter = 0; counter < _nodes.length; counter++){
				if(dragedNode.id === _nodes[counter].id) continue;	
				var nd = nodeDistance(_nodes[counter], dragedNode);
				var _removedRel = undefined;

				if(schemaInfo === undefined){
					return;
				}

				if(keys.shiftKey === true && nd <= _maxDistance){				
					if(schemaInfo.relatedTo !== undefined){
						var dis = _maxDistance / schemaInfo.relatedTo.length;
						var length = (schemaInfo.relatedTo.length - 1), selector;

						while(length > 0){
							if(nd < (length * dis))
								selector = length;
							length--;
						}

						var toRel = schemaInfo.relatedTo[selector];
						var newEdgeId = selector + ":" + dragedNode.id + ":" + _nodes[counter].id;

						if(toRel !== undefined){
							if((toRel.allTargetTypesPossible === true || toRel.possibleTargetTypes.split(',').indexOf(_nodes[counter].nodeType) > -1)){
								if(toRel.sourceMultiplicity === '1') {
									for(var c1 = 0; c1 < _edges.length; c1++){
										if (_edges[c1].target === _nodes[counter].id && _edges[c1].relType === toRel.type && !_edges[c1].added && !_edges[c1].removed) {
											_edges[c1].hidden = true;
											_edges[c1].removed = true;
											_removedRel = _edges[c1].id;
											postMessage({msg: "hideEdge", edge: _edges[c1].id});
										}
									}
								}

								if(toRel.targetMultiplicity === '1') {
									for(var c2 = 0; c2 < _edges.length; c2++){
										if (_edges[c2].source === dragedNode.id && _edges[c2].relType === toRel.type && !_edges[c2].added && !_edges[c2].removed) {
											_edges[c2].hidden = true;
											_edges[c2].removed = true;
											_removedRel = _edges[c2].id;
											postMessage({msg: "hideEdge", edge: _edges[c2].id});
										}
									}
								}

								if(_previousEdges[_nodes[counter].id] === undefined)
									_previousEdges[_nodes[counter].id] = {};
								if(_previousEdges[_nodes[counter].id].id === undefined)
									_previousEdges[_nodes[counter].id].id = "";
								
								if(_previousEdges[_nodes[counter].id].id !== newEdgeId){
									var newEdge = {
										id: newEdgeId,
										label: toRel.relationshipType,
										source: dragedNode.id,
										target: _nodes[counter].id,
										size: 40,
										color: '#81ce25',
										type: 'curvedArrow',
										added: true,
										replaced: _removedRel || _previousEdges[_nodes[counter].id].replaced,
										relType: toRel.type
									};

									if(_previousEdges[_nodes[counter].id].replaced !== undefined && _removedRel === undefined)
										postMessage({msg: "unhideEdge", edge: _previousEdges[_nodes[counter].id].replaced});

									postMessage({msg: "add", edge: newEdge, remove: _previousEdges[_nodes[counter].id].id});
									_previousEdges[_nodes[counter].id].replaced = newEdge.replaced;
									_previousEdges[_nodes[counter].id].id = newEdgeId;
								}							
							}
						}
					}
				}

				else if(keys.ctrlKey === true && nd <= _maxDistance){
					if(schemaInfo.relatedFrom !== undefined){
						var dis = _maxDistance / schemaInfo.relatedFrom.length;
						var length = (schemaInfo.relatedFrom.length - 1), selector;

						while(length >= 0){
							if(nd < (length * dis))
								selector = length;
							length--;
						}

						var newEdgeId = selector + ":" + _nodes[counter].id + ":" + dragedNode.id;
						var fromRel = schemaInfo.relatedFrom[selector];

						if(fromRel !== undefined){
							if ((fromRel.allSourceTypesPossible === true || fromRel.possibleSourceTypes.split(',').indexOf(_nodes[counter].nodeType) > -1)) {
								if (fromRel.sourceMultiplicity === '1') {
									for(var c1 = 0; c1 < _edges.length; c1++){
										if (_edges[c1].target === dragedNode.id && _edges[c1].relType === fromRel.type && !_edges[c1].added && !_edges[c1].removed) {
											_edges[c1].hidden = true;
											_edges[c1].removed = true;
											_removedRel = _edges[c1].id;
											postMessage({msg: "hideEdge", edge: _edges[c1].id});
										}
									}
								}

								if (fromRel.targetMultiplicity === '1') {
									for(var c2 = 0; c2 < _edges.length; c2++){
										if (_edges[c2].source === _nodes[counter].id && _edges[c2].relType === fromRel.type && !_edges[c2].added && !_edges[c2].removed) {
											_edges[c2].hidden = true;
											_edges[c2].removed = true;
											_removedRel = _edges[c2].id;
											postMessage({msg: "hideEdge", edge: _edges[c2].id});
										}
									}
								}

								if(_previousEdges[_nodes[counter].id] === undefined)
									_previousEdges[_nodes[counter].id] = {};
								if(_previousEdges[_nodes[counter].id].id === undefined)
									_previousEdges[_nodes[counter].id].id = "";

								if(_previousEdges[_nodes[counter].id].id !== newEdgeId){
									var newEdge = {
										id: newEdgeId,
										label: fromRel.relationshipType,
										source: _nodes[counter].id,
										target: dragedNode.id,
										size: 40,
										color: '#81ce25',
										type: 'curvedArrow',
										added: true,
										replaced: _removedRel  || _previousEdges[_nodes[counter].id].replaced,
										relType: fromRel.type
									};

									if(_previousEdges[_nodes[counter].id].replaced !== undefined)
										postMessage({msg: "unhideEdge", edge: _previousEdges[_nodes[counter].id].replaced});

									postMessage({msg: "add", edge: newEdge, remove: _previousEdges[_nodes[counter].id].id});
									_previousEdges[_nodes[counter].id].replaced = newEdge.replaced;
									_previousEdges[_nodes[counter].id].id = newEdgeId;
								}
							}
						}
					}
				}

				else{
					for(var c3 = 0; c3 < _edges.length; c3++){
						if ((_edges[c3].source === dragedNode.id && _edges[c3].target === _nodes[counter].id) || (_edges[c3].source === _nodes[counter].id && _edges[c3].target === dragedNode.id)){
							if (_edges[c3].added){
								if(_edges[c3].replaced !== undefined){
									for(var c4 = 0; c4 < _edges.length; c4++){
										if(_edges[c4].id === _edges[c3].replaced){
											_edges[c4].removed = false;
											_edges[c3].hidden = false;
											_edges[c3].replaced = undefined;
											postMessage({msg: "unhideEdge", edge: _edges[c4].id});
										}
									}
								}								
								_previousEdges[_nodes[counter].id] = _previousEdges[_nodes[counter].id] || {};
								_previousEdges[_nodes[counter].id].id = _previousEdges[_nodes[counter].id].id || "";

								postMessage({msg: "removeNewEdge", edge: _edges[c3].id});
								if(_previousEdges[_nodes[counter].id].id === _edges[c3].id)
									_previousEdges[_nodes[counter].id].id = undefined;
							}

						}
					}
				}
			}
			_nodes = _newNodes || _nodes;
			_edges = _newEdges || _edges;
		};

		function removeNewEdges(nodes, edges) {			
			for(var e = 0; e < edges.length; e++){
				if (edges[e].added){
					if(edges[e].replaced !== undefined){
						for(var c4 = 0; c4 < edges.length; c4++){
							if(edges[c4].id === edges[e].replaced){
								edges[c4].removed = false;
								edges[e].hidden = false;
								edges[e].replaced = undefined;
								postMessage({msg: "unhideEdge", edge: edges[c4].id});
								continue;
							}
						}
					}
					postMessage({msg: "removeNewEdge", edge: edges[e].id});
				}
			}
			_previousEdges = {};
		}

		function nodeDistance(n1, n2){
			var x1 = parseFloat(n1['renderer1:x']);
			var y1 = parseFloat(n1['renderer1:y']);
			var x2 = parseFloat(n2['renderer1:x']);
			var y2 = parseFloat(n2['renderer1:y']);
			var nodeDistance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
			return nodeDistance;
		};


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
					handleDrag(event.data.dragedNode, event.data.keys);
					//postMessage({msg: "debug", text: "handleDrag"});
					break;
				case "removeNewEdges":
					removeNewEdges(event.data.nodes, event.data.edges);
					//postMessage({msg: "debug", text: "handleDrag"});
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