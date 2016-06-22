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
var win = $(window);
var graphBrowser, mode, colors = [], c = 0;
var nodeIds = [], relIds = [], removedRel;
var activeTabRightGraphKey = 'structrActiveTabRightGraph_' + port;
var activeTabLeftGraphKey = 'structrActiveTabLeftGraph_' + port;
var activeTabLeftGraph, activeTabRightGraph;
var queriesSlideout, displaySlideout, filtersSlideout, nodesSlideout, relationshipsSlideout, graph;
var savedQueriesKey = 'structrSavedQueries_' + port;
var relTypes = {}, nodeTypes = {}, color = {}, relColors = {}, hasDragged, hasDoubleClicked, clickTimeout, doubleClickTime = 250, refreshTimeout;
var filteredNodeTypes = [], hiddenNodeTypes = [], hiddenRelTypes = []; //['OWNS', 'SECURITY'];
var edgeType = 'curvedArrow';
var schemaNodes = {}, schemaRelationships = {}, schemaNodesById = {};
var displayHtmlTypes = false, displayCustomTypes = true, displayCoreTypes = false, displayUiTypes = false, displayLogTypes = false, displayOtherTypes = false;
var maxRels = 100, defaultNodeColor = '#a5a5a5', defaultRelColor = '#cccccc';
var tmpX, tmpY;
var forceAtlas2Config = {
	gravity: 1,
	strongGravityMode: true,
	adjustSizes: true,
	iterationsPerRender: 10,
	barnesHutOptimize: false,
	slowDown: 2
		//outboundAttractionDistribution: true
		//startingIterations: 1000
};

var animating = false;
var timeout   = 0;
var expanded  = {};
var count     = 0;

$(document).ready(function() {
	Structr.registerModule('graph', _Graph);

	$(document.body).on('mousedown', function(e) {
		tmpX = e.clientX;
		tmpY = e.clientY;
	});

	$(document.body).on('mouseup', function(e) {
		hasDragged = (tmpX && tmpY && (tmpX !== e.clientX || tmpY !== e.clientY));
		tmpX = e.clientX;
		tmpY = e.clientY;
	});
        
        
});

var _Graph = {
	icon: 'icon/page.png',
	add_icon: 'icon/page_add.png',
	delete_icon: 'icon/page_delete.png',
	clone_icon: 'icon/page_copy.png',
	init: function() {

		// Colors created with http://paletton.com

		colors.push('#82CE25');
		colors.push('#1DA353');
		colors.push('#E24C29');
		colors.push('#C22363');

		colors.push('#B7ED74');
		colors.push('#61C68A');
		colors.push('#FF967D');
		colors.push('#E26F9E');

		colors.push('#9BDD4A');
		colors.push('#3BAF6A');
		colors.push('#F37052');
		colors.push('#D1467E');

		colors.push('#63A80F');
		colors.push('#0C853D');
		colors.push('#B93111');
		colors.push('#9E0E48');

		colors.push('#498500');
		colors.push('#00692A');
		colors.push('#921C00');
		colors.push('#7D0033');

		colors.push('#019097');
		colors.push('#103BA8');
		colors.push('#FAA800');
		colors.push('#FA7300');

		colors.push('#3FB0B5');
		colors.push('#5070C1');
		colors.push('#FFC857');
		colors.push('#FFA557');

		colors.push('#1CA2A8');
		colors.push('#2E55B7');
		colors.push('#FFB929');
		colors.push('#FF8C29');

		colors.push('#017277');
		colors.push('#0B2E85');
		colors.push('#C68500');
		colors.push('#C65B00');

		colors.push('#00595D');
		colors.push('#072368');
		colors.push('#9A6800');
		colors.push('#9A4700');

		var max = 255, min = 0;

		for (i = 50; i < 999; i++) {
			var col = 'rgb(' + (Math.floor((max - min) * Math.random()) + min) + ',' + (Math.floor((max - min) * Math.random()) + min) + ',' + (Math.floor((max - min) * Math.random()) + min) + ')';
			colors.push(col);
		}

		_Graph.updateNodeTypes();
                
                var graphBrowserSettings = {            
                    graphContainer: 'graph-canvas',
                    moduleSettings: {
                           //'exporter': {'onSuccess': control.cleanUpAfterGraphExport, 'onError': control.error},
                           //'importer': {'onSuccess': control.cleanUpAfterGraphImport, 'onError': control.error},
                           //'newNodePicker': {'onFinished': control.cleanupAfterNewNodesPicked, 'onChooseNodes': control.onChooseNodes}, 
                           'nodeExpander': {container: 'graph-info', newNodesSize: 20, newNodesSize: 20, margins: {top: 28, left: 10}, edgeType: "curvedArrow", onNodesAdded: _Graph.onNodesAdded},
                           'selectionTools': {'container': 'graph-canvas'},
                           'relationshipEditor' : {incommingRelationsKey: 'shift', outgoingRelationsKey: 'ctrl', deleteEvent: 'doubleClickEdge', onDeleteRelation: undefined},
                           'currentNodeTypes': {},
                           'nodeFilter': {}
                    },
                    sigmaSettings: {
				font: 'Open Sans',
				immutable: false,
				minNodeSize: 4,
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
				minArrowSize: 12,
				//maxArrowSize: 12,
				labelSize: 'proportional',
				labelSizeRatio: 1,
				//sideMargin: 100
			}
                }
                graphBrowser = new GraphBrowser(graphBrowserSettings);	
                graphBrowser.start();
                
                graphBrowser.bindEvent('clickNode', _Graph.handleClickNodeEvent);
                graphBrowser.bindEvent('doubleClickNode', _Graph.handleDoubleClickNodeEvent);
                graphBrowser.bindEvent('drag', _Graph.handleDragNodeEvent);
                graphBrowser.bindEvent('startdrag', _Graph.handleStartDragNodeEvent);
	},
	onload: function() {

		$('#main-help a').attr('href', 'http://docs.structr.org/frontend-user-guide#Graph');

		activeTabLeftGraph = LSWrapper.getItem(activeTabRightGraphKey);
		activeTabRightGraph = LSWrapper.getItem(activeTabLeftGraphKey);

		main.prepend(
			'<div id="graph-box"><div id="graph-info"></div><div id="queries" class="slideOut slideOutLeft"><div class="compTab" id="queriesTab">Queries</div><div><button id="clear-graph">Clear Graph</button></div></div>'
			+ '<div id="display" class="slideOut slideOutLeft"><div class="compTab" id="displayTab">Display Options</div></div>'
			+ '<div id="filters" class="slideOut slideOutLeft"><div class="compTab" id="filtersTab">Filters</div><div id="nodeFilters"><h3>Node Filters</h3></div><div id="relFilters"><h3>Relationship Filters</h3></div></div>'
			+ '<div class="canvas" id="graph-canvas"></div>'
			+ '<div id="node-types" class="graph-object-types"></div>'
			+ '<div id="relationship-types" class="graph-object-types"></div>'
			//+ '<div id="nodes" class="slideOut slideOutRight"><div class="compTab" id="nodesTab">Nodes</div></div>'
			//+ '<div id="relationships" class="slideOut slideOutRight"><div class="compTab" id="relationshipsTab">Relationships</div></div>'
			+ '</div>'
			);

		queriesSlideout = $('#queries');
		displaySlideout = $('#display');
		filtersSlideout = $('#filters');

		var nodeFilters = $('#nodeFilters', filtersSlideout);

		nodeFilters.append('<div><input type="checkbox" class="toggle-core-types"' + (displayCoreTypes ? ' checked="checked"' : '') + '> Core types</div>');
		$('.toggle-core-types', nodeFilters).on('click', function() {
			displayCoreTypes = !displayCoreTypes;
			_Graph.updateNodeTypes();
		});

		nodeFilters.append('<div><input type="checkbox" class="toggle-ui-types"' + (displayUiTypes ? ' checked="checked"' : '') + '> UI types</div>');
		$('.toggle-ui-types', nodeFilters).on('click', function() {
			displayUiTypes = !displayUiTypes;
			_Graph.updateNodeTypes();
		});

		nodeFilters.append('<div><input type="checkbox" class="toggle-custom-types"' + (displayCustomTypes ? ' checked="checked"' : '') + '> Custom types</div>');
		$('.toggle-custom-types', nodeFilters).on('click', function() {
			displayCustomTypes = !displayCustomTypes;
			_Graph.updateNodeTypes();
		});

		nodeFilters.append('<div><input type="checkbox" class="toggle-html-types"' + (displayHtmlTypes ? ' checked="checked"' : '') + '> HTML types</div>');
		$('.toggle-html-types', nodeFilters).on('click', function() {
			displayHtmlTypes = !displayHtmlTypes;
			_Graph.updateNodeTypes();
		});

		nodeFilters.append('<div><input type="checkbox" class="toggle-log-types"' + (displayLogTypes ? ' checked="checked"' : '') + '> Log types</div>');
		$('.toggle-log-types', nodeFilters).on('click', function() {
			displayLogTypes = !displayLogTypes;
			_Graph.updateNodeTypes();
		});

		nodeFilters.append('<div><input type="checkbox" class="toggle-other-types"' + (displayOtherTypes ? ' checked="checked"' : '') + '> Other types</div>');
		$('.toggle-other-types', nodeFilters).on('click', function() {
			displayOtherTypes = !displayOtherTypes;
			_Graph.updateNodeTypes();
		});
                
                $('#display').append(
                        '<div id="graphDisplayTab">' +
                            '<div id="graphLayouts">' +
                                '<h3>Layouts</h3>' + 
                                '<button id="fruchterman-controlElement">Fruchterman Layout</button>' + 
                            '</div>' +
                            '<div id="graphSelectionTools">' +
                                '<h3>Selection Tools</h3>' + 
                                '<div class="">' +
                                    '<table id="selectionToolsTable" class="table responsive">' +
                                        '<thead><tr>' +
                                            '<th>Group</th>' + 
                                            '<th>Fixed</th>' +
                                            '<th>Hidden</th>' +
                                        '</tr></thead>' +
                                        '<tbody id="selectiontools-selectionTable-groupSelectionItems">' +
                                        '<tbody>' +
                                    '</table>' + 
                                '</div>' +
                            '</div>' +
                        '</div>'
                    );

		graph = $('#graph-canvas');

		$(document.body).on('selectstart', function(e) {
			e.preventDefault();
			return false;
		});

		graph.droppable({
			accept: '.node-type',
			drop: function(e, ui) {
				var nodeType = ui.helper.attr('data-node-type')
				var x = ui.offset.left;
				var y = ui.offset.top;
				//console.log('Creating node of type', nodeType, x, y);
				Command.create({
					type: nodeType
				}, function(obj) {

					Command.get(obj.id, function(node) {
						_Graph.drawNode(node);
					});

				});

			}
		});

		_Graph.init();

		nodesSlideout = $('#nodes');
		relationshipsSlideout = $('#relationships');

		lsw = queriesSlideout.width() + 12;
		rsw = nodesSlideout.width() + 12;

		$('.slideOut').on('mouseover', function() {
			running = false;
			return true;
		});

		$('.slideOut').on('mouseout', function() {
			running = true;
			return true;
		});

		$('#queriesTab').on('click', function() {
			if (Math.abs(queriesSlideout.position().left + lsw) <= 3) {
				Structr.closeLeftSlideOuts([displaySlideout, filtersSlideout], activeTabLeftGraphKey);
				Structr.openLeftSlideOut(queriesSlideout, this, activeTabLeftGraphKey);
			} else {
				Structr.closeLeftSlideOuts([queriesSlideout], activeTabLeftGraphKey);
			}
		});

		$('#displayTab').on('click', function() {
			if (Math.abs(displaySlideout.position().left + lsw) <= 3) {
				Structr.closeLeftSlideOuts([queriesSlideout, filtersSlideout], activeTabLeftGraphKey);
				Structr.openLeftSlideOut(displaySlideout, this, activeTabLeftGraphKey, function() {
					//console.log('Display options opened');
				});
			} else {
				Structr.closeLeftSlideOuts([displaySlideout], activeTabLeftGraphKey);
			}
		});

		$('#filtersTab').on('click', function() {
			if (Math.abs(filtersSlideout.position().left + lsw) <= 3) {
				Structr.closeLeftSlideOuts([queriesSlideout, displaySlideout], activeTabLeftGraphKey);
				Structr.openLeftSlideOut(filtersSlideout, this, activeTabLeftGraphKey, function() {
					//console.log('Filters opened');
				});
			} else {
				Structr.closeLeftSlideOuts([filtersSlideout], activeTabLeftGraphKey);
			}
		});

//        $('#nodesTab').on('click', function() {
//            if (nodesSlideout.position().left === $(window).width()) {
//                Structr.closeSlideOuts([relationshipsSlideout], activeTabRightGraphKey);
//                Structr.openSlideOut(nodesSlideout, this, activeTabRightGraphKey, function() {
//                    console.log('Nodes opened');
//                });
//            } else {
//                Structr.closeSlideOuts([nodesSlideout], activeTabRightGraphKey);
//            }
//        });
//
//        $('#relationshipsTab').on('click', function() {
//            if (relationshipsSlideout.position().left === $(window).width()) {
//                Structr.closeSlideOuts([nodesSlideout], activeTabRightGraphKey);
//                Structr.openSlideOut(relationshipsSlideout, this, activeTabRightGraphKey, function() {
//                    console.log('Rels opened');
//                });
//            } else {
//                Structr.closeSlideOuts([relationshipsSlideout], activeTabRightGraphKey);
//            }
//        });

		if (activeTabLeftGraph) {
			$('#' + activeTabLeftGraph).addClass('active').click();
		}

		if (activeTabRightGraph) {
			$('#' + activeTabRightGraph).addClass('active').click();
		}

		queriesSlideout.append('<div class="query-box"><textarea class="search" name="rest" cols="39" rows="4" placeholder="Enter a REST query here"></textarea><img class="clearSearchIcon" id="clear-rest" src="icon/cross_small_grey.png">'
			+ '<button id="exec-rest">Execute REST query</button></div>');

		queriesSlideout.append('<div class="query-box"><textarea class="search" name="cypher" cols="39" rows="4" placeholder="Enter a Cypher query here"></textarea><img class="clearSearchIcon" id="clear-cypher" src="icon/cross_small_grey.png">'
			+ '<button id="exec-cypher">Execute Cypher query</button></div>');

		queriesSlideout.append('<div id="cypher-params"><h3>Cypher Parameters</h3><img id="add-cypher-parameter" src="icon/add.png">');
		_Graph.appendCypherParameter($('#cypher-params'));

		$('#clear-graph').on('click', function() {
			_Graph.clearGraph();
		});

		$('#exec-rest').on('click', function() {
			var query = $('.search[name=rest]').val();
			if (query && query.length) {
				_Graph.execQuery(query, 'rest');
			}
		});

		$('#exec-cypher').on('click', function() {
			var query = $('.search[name=cypher]').val();
			var params = {};
			var names = $.map($('[name="cyphername[]"]'), function(n) {
				return $(n).val();
			});
			var values = $.map($('[name="cyphervalue[]"]'), function(v) {
				return $(v).val();
			});

			for (var i = 0; i < names.length; i++) {
				params[names[i]] = values[i];
			}

			if (query && query.length) {
				_Graph.execQuery(query, 'cypher', JSON.stringify(params));
			}
		});

		$('#add-cypher-parameter').on('click', function() {
			_Graph.appendCypherParameter($('#cypher-params'));
		});

		_Graph.activateClearSearchIcon();

		queriesSlideout.append('<div><h3>Saved Queries</h3></div>');
		_Graph.listSavedQueries();

		//_Graph.restoreSavedQuery(0);

		searchField = $('.search', queriesSlideout);
		searchField.focus();
		searchField.keydown(function(e) {
			var rawSearchString = $(this).val();
			var searchString = rawSearchString;

			var self = $(this);
			var type = self.attr('name');
			if (type !== 'cypher') {

				var type;
				var posOfColon = rawSearchString.indexOf(':');
				if (posOfColon > -1) {
					type = rawSearchString.substring(0, posOfColon);
					type = type.capitalize();
					searchString = rawSearchString.substring(posOfColon + 1, rawSearchString.length);
				}
			}
			if (searchString && searchString.length) {
				_Graph.activateClearSearchIcon(type);
			} else {
				_Graph.clearSearch(type);
			}

			if (searchString && searchString.length && e.which === 13) {
				//console.log('Search executed', searchString, type);

				if (!shiftKey) {
					_Graph.execQuery(searchString, type);
					return false;
				}

			} else if (e.which === 27 || rawSearchString === '') {
				_Graph.clearSearch(type);
			}
		});


		win.off('resize');
		win.resize(function() {
			_Graph.resize();
		});

		Structr.unblockMenu(100);

	},
	execQuery: function(query, type, params) {

		//console.log('exec', type, 'query: ', query, ', with parameters.', params);

		if (query && query.length) {

			if (type === 'cypher') {
				Command.cypher(query.replace(/(\r\n|\n|\r)/gm, ''), params, _Graph.processQueryResults);
				_Graph.saveQuery(query, 'cypher', params);
			} else {
				Command.rest(query.replace(/(\r\n|\n|\r)/gm, ''), _Graph.processQueryResults);
				_Graph.saveQuery(query, 'rest');
			}

			_Graph.listSavedQueries();

		}
	},
	processQueryResults: function (results) {

//		console.log('query results: ', results);

		var nodes = [];
		var rels  = [];

		$(results).each(function (i, entity) {
			if (entity.hasOwnProperty('relType')) {
				rels.push(entity);
			} else {
				nodes.push(entity);
			}
		});

		nodes.forEach(function (entity) {
			StructrModel.createSearchResult(entity);
		});

		rels.forEach(function (entity) {
			StructrModel.createSearchResult(entity);
		});
                
                graphBrowser.dataChanged();
                _Graph.updateRelationshipTypes();

	},
	saveQuery: function(query, type, params) {
		var savedQueries = JSON.parse(LSWrapper.getItem(savedQueriesKey)) || [];
		var exists = false;
		$.each(savedQueries, function(i, q) {
			if (q.query === query && q.params === params) {
				exists = true;
			}
		});
		if (!exists) {
			savedQueries.unshift({'type': type, 'query': query, 'params': params});
			LSWrapper.setItem(savedQueriesKey, JSON.stringify(savedQueries));
			Structr.saveLocalStorage();
		}
	},
	removeSavedQuery: function(i) {
		var savedQueries = JSON.parse(LSWrapper.getItem(savedQueriesKey)) || [];
		savedQueries.splice(i, 1);
		LSWrapper.setItem(savedQueriesKey, JSON.stringify(savedQueries));
		_Graph.listSavedQueries();
		Structr.saveLocalStorage();
	},
	restoreSavedQuery: function(i, exec) {
		var savedQueries = JSON.parse(LSWrapper.getItem(savedQueriesKey)) || [];
		var query = savedQueries[i];
		$('.search[name=' + query.type + ']').val(query.query);
		_Graph.activateClearSearchIcon(query.type);
		$('#cypher-params input').remove();
		$('#cypher-params br').remove();
		$('#cypher-params img.remove-cypher-parameter').remove();
		if (query.params && query.params.length) {
			var parObj = JSON.parse(query.params);
			$.each(Object.keys(parObj), function(i, key) {
				_Graph.appendCypherParameter($('#cypher-params'), key, parObj[key]);
			});
		} else {
			_Graph.appendCypherParameter($('#cypher-params'));
		}
		if (exec) {
			_Graph.execQuery(query.query, query.type, query.params);
		}
	},
	listSavedQueries: function() {
		$('#saved-queries').empty();
		queriesSlideout.append('<div id="saved-queries"></div>');
		var savedQueries = JSON.parse(LSWrapper.getItem(savedQueriesKey)) || [];
		$.each(savedQueries, function(q, query) {
			if (query.type === 'cypher') {
				$('#saved-queries').append('<div class="saved-query cypher-query"><img class="replay" alt="Cypher Query" src="icon/control_play_blue.png">' + query.query + '<img class="remove-query" src="icon/cross_small_grey.png"></div>');
			} else {
				$('#saved-queries').append('<div class="saved-query rest-query"><img class="replay" alt="REST Query" src="icon/control_play.png">' + query.query + '<img class="remove-query" src="icon/cross_small_grey.png"></div>');
			}
		});
		$('.saved-query').on('click', function() {
			_Graph.restoreSavedQuery($(this).index());
		});
		$('.replay').on('click', function() {
			_Graph.restoreSavedQuery($(this).parent().index(), true);
		});
		$('.remove-query').on('click', function() {
			_Graph.removeSavedQuery($(this).parent().index());
		});
	},
	activateClearSearchIcon: function(type) {
		var icon = $('#clear-' + type);
		icon.show().on('click', function() {
			$(this).hide();
			$('.search[name=' + type + ']').val('').focus();
		});
	},
	clearSearch: function(type) {
		$('#clear-' + type).hide().off('click');
		$('.search[name=' + type + ']').val('').focus();
	},
	clearGraph: function() {
		//console.log('clearGraph');
		colors = [];
		relTypes = {};
		nodeTypes = {};
		//color = {};
		//relColors = {};
		nodeIds = [];
		relIds = [];
		hiddenNodeTypes = [];
		hiddenRelTypes = [];
		graphBrowser.reset();
                _Graph.updateRelationshipTypes();
	},
	unload: function() {
		//console.log('unload graph');
		graphBrowser.reset();
	},
	findRelationships: function(sourceId, targedId, relType) {
		var edges = [];
		graphBrowser.getSigma().graph.edges().forEach(function(edge) {
			if (edge.source === sourceId && edge.target === targedId && (!relType || edge.relType === relType)) {
				edges.push(edge);
			}
		});
		return edges;
	},
	drawNode: function(node, x, y) {
		if (isIn(node.id, nodeIds) || isIn(node.type, filteredNodeTypes)) {
			return;
		}
		nodeIds.push(node.id);
		_Graph.setNodeColor(node);
		//console.log('drawing node', node, nodeTypes[node.type], isIn(node.type, hiddenNodeTypes));
                                try{
                                    graphBrowser.addNode({
                                            id: node.id || node.name,
                                            label: (node.name || node.tag || node.id.substring(0, 5) + '…') ,
                                            x: x || Math.random(10),
                                            y: y || Math.random(10),
                                            size: 20,
                                            color: color[node.type],
                                            nodeType: node.type,
                                            name: node.name,
                                            hidden: isIn(node.type, hiddenNodeTypes)
                                    });
                                }
                                catch (error){
                                       _Logger.log(_LogType.GRAPH, 'Node: ' + n.id + 'already in the graph');
                                }
                        //_Graph.updateNodeTypes();
	},
	drawRel: function(r) {
                        try{
                            graphBrowser.addEdge({
			id: r.id,
			label: r.relType,
			source: r.sourceId,
			target: r.targetId,
			size: 40,
			color: defaultRelColor,
			type: edgeType,
			relType: r.type,
                                                relName: r.relType,
			hidden: isIn(r.relType, hiddenRelTypes)
		});
		_Graph.updateRelationshipTypes();
                        }
                        catch(error){
                             _Logger.log(_LogType.GRAPH, 'Edge: ' + r.id + 'already in the graph');
                        }
		
	},
	resize: function() {
		Structr.resize();

		var windowHeight = win.height();
		var offsetHeight = 360;

		$('#saved-queries').css({
			height: windowHeight - offsetHeight + 'px'
		});

		var ch = win.height() - 61;

		graph.css({
			height: ch,
			width: win.width()
		});

		$('canvas', graph).css({
			height: ch,
			width: win.width()
		});

		nodeTypes = $('#node-types');
		var distance = nodeTypes.position().top - 61;
		var boxHeight = (ch - (3 * distance)) / 2;

		nodeTypes.css({
			height: boxHeight
		});

		$('#relationship-types').css({
			top: nodeTypes.position().top + boxHeight + distance,
			height: boxHeight
		});

	},
	loadTypeDefinition: function(type, callback) {
		var url = rootUrl + '_schema/' + type;
		$.ajax({
			url: url,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			//async: false,
			statusCode: {
				200: function(data) {
					if (callback) {
						callback(data.result[0]);
					}
				},
				401: function(data) {
					console.log(data);
				},
				404: function(data) {
					console.log(data);
				},
				422: function(data) {
					console.log(data);
				}
			}

		}).always(function(data) {
			if (callback) {
				callback(data.result[0]);
			}
		});
	},
	updateNodeTypes: function() {

		var nodeTypesBox = $('#node-types');
		fastRemoveAllChildren(nodeTypesBox[0]);

		// getByType: function(type, pageSize, page, sort, order, properties, includeDeletedAndHidden, callback) {
		Command.getSchemaInfo(null, function(nodes) {

			nodes.sort(function(a, b) {
				var aName = a.name.toLowerCase();
				var bName = b.name.toLowerCase();
				return aName < bName ? -1 : aName > bName ? 1 : 0;
			});

			nodes.forEach(function(node) {

				var hide = false;

				if (!displayCustomTypes && node.className.startsWith('org.structr.dynamic')) hide = true;
				if (!hide && !displayCoreTypes   && node.className.startsWith('org.structr.core.entity')) hide = true;
				if (!hide && !displayHtmlTypes   && node.className.startsWith('org.structr.web.entity.html')) hide = true;
				if (!hide && !displayUiTypes     && node.className.startsWith('org.structr.web.entity') && !(displayHtmlTypes && node.className.startsWith('org.structr.web.entity.html'))) hide = true;
				if (!hide && !displayLogTypes    && node.className.startsWith('org.structr.rest.logging.entity')) hide = true;
				if (!hide && !displayOtherTypes  && node.className.startsWith('org.structr.xmpp')) hide = true;

				//console.log(hide, node.type);
				if (hide) {
					filteredNodeTypes.push(node.type);
					return;
				} else {
					filteredNodeTypes.splice(filteredNodeTypes.indexOf(node.type), 1);
				}

				//console.log(filteredNodeTypes);

				schemaNodes[node.type] = node;
				schemaNodesById[node.id] = node;

				// expand comma-separated list into real collection
				if (schemaNodes[node.type].possibleSourceTypes) {
					schemaNodes[node.type].possibleSourceTypes = schemaNodes[node.type].possibleSourceTypes.split(",");
				}

				// expand comma-separated list into real collection
				if (schemaNodes[node.type].possibleTargetTypes) {
					schemaNodes[node.type].possibleTargetTypes = schemaNodes[node.type].possibleTargetTypes.split(",");
				}

				var nodeType = node.name;

				if (!isIn(nodeType, Object.keys(color))) {
					color[nodeType] = colors[c++];
				}

				//Object.keys(color).forEach(function (nodeType) {
				nodeTypesBox.append('<div id="node-type-' + nodeType + '" class="node-type" data-node-type="' + nodeType + '"><input type="checkbox" class="toggle-type" checked="checked"> <div class="circle" style="background-color: ' + color[nodeType] + '"></div>' + nodeType + '</div>');
				var nt = $('#node-type-' + nodeType, nodeTypesBox);

				if (isIn(nodeType, hiddenNodeTypes)) {
					nt.attr('data-hidden', 1);
					nt.addClass('hidden-node-type');
					console.log('nodeType is hidden', nodeType);
				}
				nt.on('mousedown', function() {
					var nodeTypeEl = $(this);
					nodeTypeEl.css({pointer: 'move'});
					//_Graph.toggleNodeType(nodeType);
				}).on('click', function() {
					// TODO: Query
				}).on('mouseover', function() {
					graphBrowser.highlightNodeType(nodeType);
				}).on('mouseout', function() {
					graphBrowser.unhighlightNodeType(nodeType);
				}).draggable({
					helper: 'clone'
				});

				$('.toggle-type', nt).on('click', function() {
					var n = $(this);
					if (n.attr('data-hidden')) {
						graphBrowser.hideNodeType(nodeType, false);
                                                n.removeAttr('data-hidden', 1);
					} else {
						graphBrowser.hideNodeType(nodeType, true);
						n.attr('data-hidden', 1);
					}
				});

			});
			_Graph.filterNodeTypes(filteredNodeTypes);
			_Graph.resize();
		});

	},
	filterNodeTypes: function(types) {
                graphBrowser.clearFilterNodeTypes();
		types.forEach(function(type) {
                    graphBrowser.addNodeTypeToFilter(type);
		});
                graphBrowser.filterGraph();
	},
	
	setNodeColor: function(node) {
		if (!isIn(node.type, Object.keys(color))) {
			node.color = colors[color++];
			//console.log(typeDef.type, typeDef.color, color);
			color[node.type] = node.color;
		} else {
			node.color = color[node.type];
		}
	},
	setRelationshipColor: function(rel) {
		//console.log('setRelColor', rel);
		if (!isIn(rel.relType, Object.keys(relColors))) {
			rel.color = colors[color++];
			//console.log(typeDef.type, typeDef.color, color);
			relColors[rel.relType] = rel.color;
		} else {
			rel.color = relColors[rel.relType];
		}
	},
	updateRelationshipTypes: function() {
		var relTypesBox = $('#relationship-types');
		relTypesBox.empty();
		//console.log(relColors);
                var relTypes = graphBrowser.getCurrentRelTypes();
		$.each(relTypes, function(i, relType){
			relTypesBox.append('<div id="rel-type-' + relType + '">' + relType + '</div>');
			var rt = $('#rel-type-' + relType, relTypesBox);
			if (isIn(relType, hiddenRelTypes)) {
				rt.attr('data-hidden', 1);
				rt.addClass('hidden-node-type');
			}
			rt.on('mousedown', function() {
				var relTypeEl = $(this);
				relTypeEl.css({pointer: 'move'});
				//_Graph.toggleNodeType(nodeType);
			}).on('click', function() {
				var n = $(this);
				if (n.attr('data-hidden')) {
					graphBrowser.hideRelType(relType, false)
					n.removeAttr('data-hidden', 1);
					n.removeClass('hidden-node-type');
				} else {
					graphBrowser.hideRelType(relType, true) 
					n.attr('data-hidden', 1);
					n.addClass('hidden-node-type');
				}
			}).on('mouseover', function() {
				graphBrowser.highlightRelType(relType);
			}).on('mouseout', function() {
				graphBrowser.unhighlightRelType(relType);
			});
		});

	},
	
	appendCypherParameter: function(el, key, value) {
		el.append('<div><img class="remove-cypher-parameter" src="icon/delete.png"> <input name="cyphername[]" type="text" placeholder="name" size="10" value="' + (key || '') + '"> <input name="cyphervalue[]" type="text" placeholder="value" size="10" value="' + (value || '') + '"></div>');
		$('.remove-cypher-parameter', el).on('click', function() {
			$(this).parent().remove();
		});
	},
        
        onNodesAdded: function(){
            _Graph.updateRelationshipTypes();            
        },
        
        handleDragNodeEvent: function(){
             hasDragged = true;
        },
        
        handleStartDragNodeEvent: function(){
             hasDragged = false;
        },
        
        handleDoubleClickNodeEvent: function(clickedNode){
            var node = clickedNode.data.node;            
            window.clearTimeout(clickTimeout);
            hasDoubleClicked = true;
            return false;
        },
        
        handleClickNodeEvent: function(clickedNode){
            var node = clickedNode.data.node;
            
            if (hasDoubleClicked) {
                _Logger.log(_LogType.GRAPH, 'double clicked, returning');
                return false;
            }
            
            if (hasDragged) {
                hasDragged = false;
                return false;
            }
            
            _Logger.log(_LogType.GRAPH, 'clickNode');
            
            clickTimeout = window.setTimeout(function() {
                _Entities.showProperties(node);
                //engine.renderers[0].dispatchEvent('outNode', {node: node});
                window.clearTimeout(clickTimeout);
                    
            }, doubleClickTime);
            window.setTimeout(function() {
                hasDoubleClicked = false;
            }, doubleClickTime + 10);
        }
};

function getRandomInt(min, max) {
	return Math.floor(Math.random() * (max - min)) + min;
}