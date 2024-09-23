/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
document.addEventListener("DOMContentLoaded", () => {

	Structr.registerModule(_Graph);

	$(document).on('change', '#nodeFilters input', function(e) {
		_Graph.updateNodeTypes();
		LSWrapper.setItem(_Graph.displayTypeConfigKey, _Graph.getTypeVisibilityConfig());
	});

	// $(document).on('click', '.remove-cypher-parameter', function() {
	// 	$(this).parent().remove();
	// });
});

let _Graph = {
	_moduleName: 'graph',
	displayTypeConfigKey: 'structrGraphDisplayTypes_' + location.port,
	savedQueriesKey: 'structrSavedQueries_' + location.port,
	graphBrowser: undefined,
	animating: undefined,
	edgeType: 'curvedArrow',
	colors: [],
	color: {},
	relColors: {},
	colorCntr: 0,
	defaultRelColor: '#cccccc',
	doubleClickTime: 250,
	tmpX: undefined,
	tmpY: undefined,
	hasDragged: undefined,
	hasDoubleClicked: undefined,
	nodeLabelsHidden: false,
	edgeLabelsHidden: false,
	filteredNodeTypes: [],
	hiddenNodeTypes: [],
	hiddenRelTypes: [],
	clickTimeout: undefined,
	nodeIds: [],
	schemaNodes: {},
	schemaNodesById: {},

	init: () => {

		// Colors created with http://paletton.com
		let palettonColors = [
			'#82CE25', '#1DA353', '#E24C29', '#C22363',
			'#B7ED74', '#61C68A', '#FF967D', '#E26F9E',
			'#9BDD4A', '#3BAF6A', '#F37052', '#D1467E',
			'#63A80F', '#0C853D', '#B93111', '#9E0E48',
			'#498500', '#00692A', '#921C00', '#7D0033',
			'#019097', '#103BA8', '#FAA800', '#FA7300',
			'#3FB0B5', '#5070C1', '#FFC857', '#FFA557',
			'#1CA2A8', '#2E55B7', '#FFB929', '#FF8C29',
			'#017277', '#0B2E85', '#C68500', '#C65B00',
			'#00595D', '#072368', '#9A6800', '#9A4700'
		];
		_Graph.colors.concat(palettonColors);

		let steps = [21, 53, 31];

		for (let i = 50; i < 999; i++) {
			_Graph.colors.push(`rgb(${(steps[i % 3] * i) % 255},${(steps[(i + 1) % 3] * i) % 255},${(steps[(i + 2) % 3] * i) % 255})`);
		}

		_Graph.updateNodeTypes();

		let canvasWidth  = 1000; //$('#graph-canvas').width();
		let canvasHeight = 1000; //canvasWidth / (16 / 9);

		let editDistance = Math.sqrt(Math.pow(canvasWidth, 2) + Math.pow(canvasHeight, 2)) * 0.2;
		let graphBrowserSettings = {
			rootUrl: Structr.rootUrl,
			graphContainer: 'graph-canvas',
			moduleSettings: {
				tooltips : {
					node : {
						cssClass: 'graphBrowserTooltips',
						renderer: _Graph.renderNodeTooltip
					},
					edge : {
						cssClass: 'graphBrowserTooltips',
						renderer: _Graph.renderEdgeTooltip
					}
				},
				nodeExpander: {
					container: 'graph-info',
					newEdgeSize: 40,
					newNodeSize: 20,
					margins: {top: 24, left: 0},
					edgeType: "curvedArrow",
					infoButtonRenderer: _Graph.renderNodeExpanderInfoButton,
					defaultInfoButtonColor: '#e5e5e5',
					expandButtonsTimeout: 1000,
					onNodesAdded: _Graph.onNodesAdded
				},
				selectionTools: {
					container: 'graph-canvas'
				},
				relationshipEditor : {
					incomingRelationsKey: 'shift',
					outgoingRelationsKey: 'ctrl',
					deleteEvent: 'doubleClickEdge',
					onDeleteRelation: undefined,
					maxDistance: editDistance
				},
				currentNodeTypes: {},
				nodeFilter: {}
			},
			sigmaSettings: {
				immutable: false,
				minNodeSize: 1,
				maxNodeSize: 10,
				labelThreshold: 0,
				borderSize: 4,
				defaultNodeBorderColor: '#a5a5a5',
				singleHover: true,
				doubleClickEnabled: false,
				minEdgeSize: 1,
				maxEdgeSize: 4,
				newEdgeSize: 40,
				enableEdgeHovering: true,
				edgeHoverColor: 'default',
				edgeHoverSizeRatio: 1.3,
				edgeHoverExtremities: true,
				autoCurveRatio: 1,
				autoCurveSortByDirection: true,
				defaultEdgeColor: '#cccccc',
				defaultEdgeHoverColor: '#81ce25',
				defaultEdgeLabelActiveColor: '#81ce25',
				defaultEdgeActiveColor: '#81ce25',
				defaultEdgeType: 'arrow',
				newEdgeType: 'curvedArrow',
				minArrowSize: 4,
				maxArrowSize: 8,
				labelSize: 'proportional',
				labelSizeRatio: 1,
				labelAlignment: 'right',
				nodeHaloColor: 'rgba(236, 81, 72, 0.2)',
				nodeHaloSize: 20
			},
			lassoSettings: {
				strokeStyle: 'rgb(129, 206, 37)',
				lineWidth: 5,
				fillWhileDrawing: true,
				fillStyle: 'rgba(129, 206, 37, 0.2)',
				cursor: 'crosshair'
			}
		};

		_Graph.graphBrowser = new GraphBrowser(graphBrowserSettings);
		_Graph.graphBrowser.start();

		_Graph.graphBrowser.bindEvent('clickStage', _Graph.handleClickStageEvent);
		_Graph.graphBrowser.bindEvent('clickNode', _Graph.handleClickNodeEvent);
		_Graph.graphBrowser.bindEvent('doubleClickNode', _Graph.handleDoubleClickNodeEvent);
		_Graph.graphBrowser.bindEvent('drag', _Graph.handleDragNodeEvent);
		_Graph.graphBrowser.bindEvent('startdrag', _Graph.handleStartDragNodeEvent);
		_Graph.graphBrowser.bindEvent('clickEdge', _Graph.handleClickEdgeEvent);
	},

	onload: () => {

		Structr.performActionAfterEnvResourceLoaded(() => {

			_Graph.eventHandlers.register();

			live('#toggleNodeLabels', 'change', (e) => {
				_Graph.nodeLabelsHidden = !_Graph.nodeLabelsHidden;
				_Graph.graphBrowser.changeSigmaSetting('drawLabels', !_Graph.nodeLabelsHidden);
			});

			live('#toggleEdgeLabels', 'change', (e) => {
				_Graph.edgeLabelsHidden = !_Graph.edgeLabelsHidden;
				_Graph.graphBrowser.changeSigmaSetting('drawEdgeLabels', !_Graph.edgeLabelsHidden);
			});

			live('#fruchterman-controlElement', 'click', () => {
				_Graph.graphBrowser.doLayout('fruchtermanReingold');
			});

			live('#dagre-controlElement', 'click', () => {
				_Graph.graphBrowser.doLayout('dagre');
			});

			live('#forceAtlas-controlElement', 'click', (e) => {
				let el = e.target.closest('a#forceAtlas-controlElement');
				el.classList.toggle('active');
				_Graph.graphBrowser.startForceAtlas2();
			});

			Structr.setMainContainerHTML(_Graph.templates.main());
			Structr.setFunctionBarHTML(_Graph.templates.functions());

			Structr.updateMainHelpLink(_Helpers.getDocumentationURLForTopic('graph'));

			// UISettings.showSettingsForCurrentModule();

			document.querySelector('#clear-graph')?.addEventListener('click', _Graph.clearGraph);

			let savedTypeVisibility = LSWrapper.getItem(_Graph.displayTypeConfigKey) || {};
			$('#graphTypeToggleRels').prop('checked', (savedTypeVisibility.rels === undefined ? true : savedTypeVisibility.rels));
			$('#graphTypeToggleCustom').prop('checked', (savedTypeVisibility.custom === undefined ? true : savedTypeVisibility.custom));
			$('#graphTypeToggleCore').prop('checked', (savedTypeVisibility.core === undefined ? true : savedTypeVisibility.core));
			$('#graphTypeToggleHtml').prop('checked', (savedTypeVisibility.html === undefined ? true : savedTypeVisibility.html));
			$('#graphTypeToggleUi').prop('checked', (savedTypeVisibility.ui === undefined ? true : savedTypeVisibility.ui));
			$('#graphTypeToggleLog').prop('checked', (savedTypeVisibility.log === undefined ? true : savedTypeVisibility.log));
			$('#graphTypeToggleOther').prop('checked', (savedTypeVisibility.other === undefined ? true : savedTypeVisibility.other));

			$('#selectionLasso').on('click', function () {
				if (!_Graph.graphBrowser.selectionToolsActive) {
					_Graph.graphBrowser.activateSelectionLasso(true);
				}
			});

			$('#newSelectionGroup').on('click', function () {

				let newId = _Graph.graphBrowser.createSelectionGroup();

				let newSelectionGroupHtml = _Graph.templates.newSelectionGroup({newId: newId});

				$('#selectiontools-selectionTable-groupSelectionItems').append(newSelectionGroupHtml);
				$("input[name='selectedGroup[]']").trigger('click');
				$("input[value='selected." + newId + "']").prop('checked', true);
			});

			$(document).on('click', ".selectionTableRemoveBtn", function () {
				var val = $(this).val();
				_Graph.graphBrowser.dropSelection(val);
			});

			$(document).on('click', "input[name='selectedGroup[]']", function () {
				var self = $(this);

				if (self.is(':checked')) {
					$("input[name='selectedGroup[]']").prop("checked", false);
					self.prop("checked", true);
					var val = self.val().split('.');
					_Graph.graphBrowser.activateSelectionTools();
					_Graph.graphBrowser.activateSelectionGroup(val[1]);
				} else {
					_Graph.graphBrowser.deactivateSelectionTools();
					self.prop("checked", false);
				}
			});

			$(document).on('click', "input[name='Hidden[]']", function () {
				var self = $(this);
				var val = self.val().split('.');

				_Graph.graphBrowser.hideSelectionGroup(val[1], self.is(':checked'));
			});

			$(document).on('click', "input[name='Fixed[]']", function () {
				var self = $(this);
				var val = self.val().split('.');

				_Graph.graphBrowser.fixateSelectionGroup(val[1], self.is(':checked'));
			});

			for (let clearSearchIcon of document.querySelectorAll('.clearSearchIcon')) {
				clearSearchIcon.addEventListener('click', (e) => {
					let svg = e.target.closest('svg');
					_Graph.clearSearch(svg.parentNode.querySelector('.search'));
				});
			}

			$('#exec-rest').on('click', function () {
				var query = $('.search[name=rest]').val();
				if (query && query.length) {
					_Graph.execQuery(query, 'rest');
				}
			});

			$('#exec-cypher').on('click', function () {
				var query = $('.search[name=cypher]').val();
				// var params = {};
				// var names = $.map($('[name="cyphername[]"]'), function(n) {
				// 	return $(n).val();
				// });
				// var values = $.map($('[name="cyphervalue[]"]'), function(v) {
				// 	return $(v).val();
				// });
				//
				// for (var i = 0; i < names.length; i++) {
				// 	params[names[i]] = values[i];
				// }

				if (query && query.length) {
					_Graph.execQuery(query, 'cypher'
						//	, JSON.stringify(params)
					);
				}
			});

			$(document).on('click', '.closeTooltipBtn', () => {
				_Graph.graphBrowser.closeTooltip();
			});

			$(document).on('click', '#tooltipBtnProps', function () {
				var id = $(this).attr("value");
				var type = $(this).data("type");
				_Entities.showProperties({id: id, type: type});
				_Graph.graphBrowser.closeTooltip();
			});

			$(document).on('click', '#tooltipBtnHide', function () {
				var id = $(this).attr("value");
				_Graph.graphBrowser.closeTooltip();
				_Graph.graphBrowser.hideNode(id, true);
			});

			$(document).on('click', '#tooltipBtnDrop', function () {
				let id = $(this).attr("value");
				_Graph.graphBrowser.closeTooltip();
				_Graph.graphBrowser.dropNode(id);
				_Graph.graphBrowser.dataChanged();
				_Graph.updateRelationshipTypes();
			});

			$(document).on('click', '#tooltipBtnDel', function () {

				let self = $(this);
				let id = self.attr("value");

				Command.get(id, 'id,type,name,sourceId,targetId', (entity) => {
					if (_Graph.graphBrowser.getNode(entity.id)) {
						_Entities.deleteNode(entity, false, (entity) => {
							_Graph.graphBrowser.dropNode(entity);
							_Graph.graphBrowser.dataChanged();
							_Graph.updateRelationshipTypes();
						});
					} else {
						_Entities.deleteEdge(entity, false, (entity) => {
							if (_Graph.graphBrowser.getEdge(entity)) {
								_Graph.graphBrowser.dropEdge(entity);
							}
							_Graph.graphBrowser.dataChanged();
							_Graph.updateRelationshipTypes();
						});
					}
				});
				_Graph.graphBrowser.closeTooltip();
			});

			let canvas = $('#graph-canvas');

			canvas.droppable({
				accept: '.node-type',
				drop: function (e, ui) {
					let nodeType = ui.helper.attr('data-node-type');
					Command.create({
						type: nodeType
					}, (obj) => {
						if (obj != null) {
							Command.get(obj.id, 'id,type,name,color,tag', (node) => {
								_Graph.drawNode(node);
							});
						}
					});
				}
			});

			_Graph.init();
			// _Graph.appendCypherParameter($('#cypher-params'));
			//
			// $('#add-cypher-parameter').on('click', function() {
			// 	_Graph.appendCypherParameter($('#cypher-params'));
			// });
			//
			// $(document).on('click', '.remove-cypher-parameter', function() {
			// 	$(this).parent().remove();
			// });

			// _Graph.listSavedQueries();

			let restQueryBox = document.querySelector('.query-box.rest .search');
			let cypherQueryBox = document.querySelector('.query-box.cypher .search');
			restQueryBox.focus();

			let searchKeydownHandler = (e) => {

				// keydown so Enter key is preventable

				let searchString = e.target.value;
				let type = e.target.getAttribute('name');

				if (searchString && searchString.length) {
					e.target.parentNode.querySelector('.clearSearchIcon').style.display = 'block';
				} else {
					_Graph.clearSearch(e.target);
				}

				if (searchString && searchString.length && e.which === 13) {

					if (e.shiftKey === false) {

						e.stopPropagation();
						e.preventDefault();
						_Graph.execQuery(searchString, type);
						return false;
					}

				} else if (e.which === 27) {

					_Graph.clearSearch(e.target);
				}
			};

			restQueryBox.addEventListener('keydown', searchKeydownHandler);
			cypherQueryBox?.addEventListener('keydown', searchKeydownHandler);
			cypherQueryBox?.addEventListener('keyup', () => {
				// keyup so we have the actual value
				cypherQueryBox.setAttribute('rows', cypherQueryBox.value.split('\n').length);
			});

			$('#newSelectionGroup').trigger('click');
			Structr.mainMenu.unblock(100);
		});
	},
	unload: () => {

		_Graph.eventHandlers.unregister();

		_Graph.colors          = [];
		_Graph.nodeIds         = [];
		_Graph.relIds          = [];
		_Graph.hiddenNodeTypes = [];
		_Graph.hiddenRelTypes  = [];

		if (_Graph.graphBrowser) {
			try {
				_Graph.graphBrowser.kill();
				_Graph.graphBrowser = undefined;
			} catch (e) {
				_Graph.graphBrowser = undefined;
			}
		}
	},
	eventHandlers: {
		register: () => {
			document.body.addEventListener('mousedown', _Graph.eventHandlers.mouseDownHandler);
			document.body.addEventListener('mouseup', _Graph.eventHandlers.mouseUpHandler);
		},
		unregister: () => {
			document.body.removeEventListener('mousedown', _Graph.eventHandlers.mouseDownHandler);
			document.body.removeEventListener('mouseup', _Graph.eventHandlers.mouseUpHandler);
		},
		mouseDownHandler: (e) => {
			_Graph.tmpX = e.clientX;
			_Graph.tmpY = e.clientY;
		},
		mouseUpHandler: (e) => {
			_Graph.hasDragged = (_Graph.tmpX && _Graph.tmpY && (_Graph.tmpX !== e.clientX || _Graph.tmpY !== e.clientY));
			_Graph.tmpX = e.clientX;
			_Graph.tmpY = e.clientY;
		},
	},
	execQuery: (query, type, params) => {

		if (query && query.length) {
			if (type === 'cypher') {
				Command.cypher(query.replace(/(\r\n|\n|\r)/gm, ''), params, _Graph.processQueryResults);
				// _Graph.saveQuery(query, 'cypher', params);
			} else {
				Command.rest(query.replace(/(\r\n|\n|\r)/gm, ''), _Graph.processQueryResults);
				// _Graph.saveQuery(query, 'rest');
			}

			// _Graph.listSavedQueries();
		}
	},
	processQueryResults: (results) => {

		for (let entity of results) {
			StructrModel.createSearchResult(entity);
		}

		_Graph.graphBrowser.dataChanged();
		_Graph.updateRelationshipTypes();

	},
	// saveQuery: function(query, type, params) {
	// 	var savedQueries = JSON.parse(LSWrapper.getItem(_Graph.savedQueriesKey)) || [];
	// 	var exists = false;
	// 	$.each(savedQueries, function(i, q) {
	// 		if (q.query === query && q.params === params) {
	// 			exists = true;
	// 		}
	// 	});
	// 	if (!exists) {
	// 		savedQueries.unshift({type: type, query: query, params: params});
	// 		LSWrapper.setItem(_Graph.savedQueriesKey, JSON.stringify(savedQueries));
	// 	}
	// },
	// removeSavedQuery: function(i) {
	// 	var savedQueries = JSON.parse(LSWrapper.getItem(_Graph.savedQueriesKey)) || [];
	// 	savedQueries.splice(i, 1);
	// 	LSWrapper.setItem(_Graph.savedQueriesKey, JSON.stringify(savedQueries));
	// 	_Graph.listSavedQueries();
	// },
	// restoreSavedQuery: function(i, exec) {
	// 	var savedQueries = JSON.parse(LSWrapper.getItem(_Graph.savedQueriesKey)) || [];
	// 	var query = savedQueries[i];
	//
	// 	let element = document.querySelector('.search[name=' + query.type + ']');
	// 	element.value = query.query;
	// 	_Graph.showClearSearchIcon(element);
	//
	// 	$('#cypher-params div.cypher-param').remove();
	// 	if (query.params && query.params.length) {
	// 		var parObj = JSON.parse(query.params);
	// 		$.each(Object.keys(parObj), function(i, key) {
	// 			_Graph.appendCypherParameter($('#cypher-params'), key, parObj[key]);
	// 		});
	// 	} else {
	// 		_Graph.appendCypherParameter($('#cypher-params'));
	// 	}
	// 	if (exec) {
	// 		_Graph.execQuery(query.query, query.type, query.params);
	// 	}
	// },
	// listSavedQueries: function() {
	// 	$('#saved-queries').empty();
	//
	// 	var savedQueries = JSON.parse(LSWrapper.getItem(_Graph.savedQueriesKey)) || [];
	// 	$.each(savedQueries, function(q, query) {
	// 		if (query.type === 'cypher') {
	// 			$('#saved-queries').append('<div class="saved-query cypher-query"><i class="replay ' + _Icons.getFullSpriteClass(_Icons.exec_icon) + '" />' + query.query + '<i class="remove-query ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></div>');
	// 		} else {
	// 			$('#saved-queries').append('<div class="saved-query rest-query"><i class="replay ' + _Icons.getFullSpriteClass(_Icons.exec_blue_icon) + '" />' + query.query + '<i class="remove-query ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></div>');
	// 		}
	// 	});
	//
	// 	$('.saved-query').on('click', function() {
	// 		_Graph.restoreSavedQuery($(this).index());
	// 	});
	//
	// 	$('.replay').on('click', function() {
	// 		_Graph.restoreSavedQuery($(this).parent().index(), true);
	// 	});
	//
	// 	$('.remove-query').on('click', function() {
	// 		_Graph.removeSavedQuery($(this).parent().index());
	// 	});
	// },

	clearSearch: (element) => {

		element.parentNode.querySelector('.clearSearchIcon').style.display = null;
		element.value = '';
		element.focus();
	},

	clearGraph: () => {

		_Graph.colors          = [];
		_Graph.nodeIds         = [];
		_Graph.relIds          = [];
		_Graph.hiddenNodeTypes = [];
		_Graph.hiddenRelTypes  = [];

		_Graph.graphBrowser.hideExpandButtons();
		_Graph.graphBrowser.reset();

		_Graph.updateRelationshipTypes();
	},

	drawNode: function(node, x, y) {

		if (_Helpers.isIn(node.id, _Graph.nodeIds) || _Helpers.isIn(node.type, _Graph.filteredNodeTypes)) {
			return;
		}
		_Graph.nodeIds.push(node.id);
		_Graph.setNodeColor(node);

		try {
			var ratio = _Graph.graphBrowser.getCameraRatio();
			var newX = 0;
			var newY = 0;

			if (ratio > 1) {
				newX = (Math.random(100) / (2*ratio));
				newY = (Math.random(100) / (2*ratio));
			} else {
				newX = (Math.random(100) / (1/ratio));
				newY = (Math.random(100) / (1/ratio));
			}

			_Graph.graphBrowser.addNode({
				id: node.id || node.name,
				label: (node.name || node.tag || node.id.substring(0, 5) + '…'),
				labelForTooltip: (node.name || node.tag || node.id || `Unnamed ${node.type}`),
				x: x || newX,
				y: y || newY,
				size: 20,
				color: _Graph.color[node.type],
				nodeType: node.type,
				name: node.name,
				hidden: _Helpers.isIn(node.type, _Graph.hiddenNodeTypes)
			});

			_Graph.graphBrowser.dataChanged();

		} catch (error) {
//			console.log('Node: ' + node.id + 'already in the graph');
		}
	},

	drawRel: (r) => {

		let existingEdges = _Graph.graphBrowser.findRelationships(r.sourceId, r.targetId);
		let c = existingEdges.length * 15;

		try {
			_Graph.graphBrowser.addEdge({
				id: r.id,
				label: r.relType,
				source: r.sourceId,
				target: r.targetId,
				size: 40,
				color: _Graph.defaultRelColor,
				type: _Graph.edgeType,
				relType: r.type,
				relName: r.relType,
				hidden: _Helpers.isIn(r.relType, _Graph.hiddenRelTypes),
				count: c
			});
			_Graph.updateRelationshipTypes();

		} catch(error) {
//			console.log('Edge: ' + r.id + 'already in the graph');
		}
	},

	resize: () => {

		// var windowHeight = $(window).height();
		// var windowWidth = $(window).width();

		// var ch = windowHeight - graph.offset().top;

		// graph.css({
		// 	height: ch,
		// 	width: windowWidth
		// });
		//
		// $('canvas', graph).css({
		// 	height: ch,
		// 	width: windowWidth
		// });

		// nodeTypes = $('#node-types');
		// var distance = nodeTypes.position().top - 60;
		// var boxHeight = (ch - (3 * distance)) / 2;
		//
		// nodeTypes.css({
		// 	height: boxHeight
		// });
		//
		// $('#relationship-types').css({
		// 	top: nodeTypes.position().top + boxHeight + distance,
		// 	height: boxHeight
		// });
	},

	getTypeVisibilityConfig: () => {

		return {
			rels:   $('#graphTypeToggleRels').prop('checked'),
			custom: $('#graphTypeToggleCustom').prop('checked'),
			core:   $('#graphTypeToggleCore').prop('checked'),
			html:   $('#graphTypeToggleHtml').prop('checked'),
			ui:     $('#graphTypeToggleUi').prop('checked'),
			log:    $('#graphTypeToggleLog').prop('checked'),
			other:  $('#graphTypeToggleOther').prop('checked')
		};

	},
	updateNodeTypes: () => {

		var nodeTypesBox = $('#node-types');
		_Helpers.fastRemoveAllChildren(nodeTypesBox[0]);

		Command.getSchemaInfo(null, (nodes) => {

			var typeVisibility = _Graph.getTypeVisibilityConfig();

			_Graph.filteredNodeTypes = [];

			nodes.sort(function(a, b) {
				var aName = a.name.toLowerCase();
				var bName = b.name.toLowerCase();
				return aName < bName ? -1 : aName > bName ? 1 : 0;
			});

			nodes.forEach(function(node) {

				if (node.isRel === true) {
					return;
				}

				var isRelType     = node.isRel;
				var isDynamicType = node.className.startsWith('org.structr.dynamic');
				var isCoreType    = node.className.startsWith('org.structr.core.entity');
				var isHtmlType    = node.className.startsWith('org.structr.web.entity.html');
				var isUiType      = node.className.startsWith('org.structr.web.entity') && !node.className.startsWith('org.structr.web.entity.html');
				var isLogType     = node.className.startsWith('org.structr.rest.logging.entity');
				var isOtherType   = !(isRelType || isDynamicType || isCoreType || isHtmlType || isUiType || isLogType);

				let hide = false; //(!typeVisibility.rels && isRelType) || (!typeVisibility.custom && isDynamicType) || (!typeVisibility.core && isCoreType) || (!typeVisibility.html && isHtmlType) ||
							//(!typeVisibility.ui && isUiType) || (!typeVisibility.log && isLogType) || (!typeVisibility.other && isOtherType);

				if (hide) {
					_Graph.filteredNodeTypes.push(node.type);
					return;
				}

				_Graph.schemaNodes[node.type] = node;
				_Graph.schemaNodesById[node.id] = node;

				// expand comma-separated list into real collection
				if (_Graph.schemaNodes[node.type].possibleSourceTypes) {
					_Graph.schemaNodes[node.type].possibleSourceTypes = _Graph.schemaNodes[node.type].possibleSourceTypes.split(",");
				}

				// expand comma-separated list into real collection
				if (_Graph.schemaNodes[node.type].possibleTargetTypes) {
					_Graph.schemaNodes[node.type].possibleTargetTypes = _Graph.schemaNodes[node.type].possibleTargetTypes.split(",");
				}

				var nodeType = node.name;

				if (!_Helpers.isIn(nodeType, Object.keys(_Graph.color))) {
					_Graph.color[nodeType] = _Graph.colors[_Graph.colorCntr++];
				}

				nodeTypesBox.append(`<div id="node-type-${nodeType}" class="node-type" data-node-type="${nodeType}"><input type="checkbox" class="toggle-type" checked="checked"> <div class="circle" style="background-color: ${_Graph.color[nodeType]}"></div>${nodeType}</div>`);
				var nt = $('#node-type-' + nodeType, nodeTypesBox);

				if (_Helpers.isIn(nodeType, _Graph.hiddenNodeTypes)) {
					nt.attr('data-hidden', 1);
					nt.addClass('hidden-node-type');
				}
				nt.on('mousedown', function() {
					var nodeTypeEl = $(this);
					nodeTypeEl.css({pointer: 'move'});
				}).on('click', function() {
					// TODO: Query
				}).on('mouseover', function() {
					_Graph.graphBrowser.highlightNodeType(nodeType);
				}).on('mouseout', function() {
					_Graph.graphBrowser.unhighlightNodeType(nodeType);
				}).draggable({
					helper: 'clone'
				});

				$('.toggle-type', nt).on('click', function() {
					var n = $(this);
					if (n.attr('data-hidden')) {
						_Graph.graphBrowser.hideNodeType(nodeType, false);
						n.removeAttr('data-hidden', 1);
					} else {
						_Graph.graphBrowser.hideNodeType(nodeType, true);
						n.attr('data-hidden', 1);
					}
				});
			});
			_Graph.filterNodeTypes(_Graph.filteredNodeTypes);
			Structr.resize();
		});
	},

	filterNodeTypes: (types) => {

		_Graph.graphBrowser.clearFilterNodeTypes();

		for (let type of types) {
			_Graph.graphBrowser.addNodeTypeToFilter(type);
		}

		_Graph.graphBrowser.filterGraph();
	},

	setNodeColor: (node) => {

		if (!_Helpers.isIn(node.type, Object.keys(_Graph.color))) {

			node.color = _Graph.colors[_Graph.color++];
			_Graph.color[node.type] = node.color;

		} else {

			node.color = _Graph.color[node.type];
		}
	},

	setRelationshipColor: (rel) => {

		if (!_Helpers.isIn(rel.relType, Object.keys(_Graph.relColors))) {

			rel.color = _Graph.colors[_Graph.color++];
			_Graph.relColors[rel.relType] = rel.color;

		} else {

			rel.color = _Graph.relColors[rel.relType];
		}
	},

	updateRelationshipTypes: () => {

		let relTypesBox = $('#relationship-types');
		relTypesBox.empty();
		let relTypes = _Graph.graphBrowser.getCurrentRelTypes();

		$.each(relTypes, function(i, relType){

			relTypesBox.append(`<div id="rel-type-${relType}">${relType}</div>`);
			var rt = $('#rel-type-' + relType, relTypesBox);
			if (_Helpers.isIn(relType, _Graph.hiddenRelTypes)) {
				rt.attr('data-hidden', 1);
				rt.addClass('hidden-node-type');
			}
			rt.on('mousedown', function() {
				var relTypeEl = $(this);
				relTypeEl.css({pointer: 'move'});
			}).on('click', function() {
				var n = $(this);
				if (n.attr('data-hidden')) {
					_Graph.graphBrowser.hideRelType(relType, false);
					n.removeAttr('data-hidden', 1);
					n.removeClass('hidden-node-type');
				} else {
					_Graph.graphBrowser.hideRelType(relType, true);
					n.attr('data-hidden', 1);
					n.addClass('hidden-node-type');
				}
			}).on('mouseover', function() {
					_Graph.graphBrowser.highlightRelType(relType);
			}).on('mouseout', function() {
					_Graph.graphBrowser.unhighlightRelType(relType);
			});
		});
	},

	// appendCypherParameter: function(el, key, value) {
	// 	el.append(`
	// 		<div class="cypher-param">
	// 			<i class="remove-cypher-parameter ${_Icons.getFullSpriteClass(_Icons.delete_icon)}"></i>
	// 			<input name="cyphername[]" type="text" placeholder="name" size="10" value="${(key || '')}">
	// 			<input name="cyphervalue[]" type="text" placeholder="value" size="10" value="${(value || '')}">
	// 		</div>
	// 	`);
	// },

	onNodesAdded: function(){
		_Graph.updateRelationshipTypes();
	},

	handleClickStageEvent: () => {
		_Graph.graphBrowser.hideExpandButtons();
		document.querySelectorAll('.dropdown-menu-container').forEach((container) => {
			container.style.display = 'none';
		});
	},
	handleDragNodeEvent: () => {
		_Graph.hasDragged = true;
	},
	handleStartDragNodeEvent: () => {
		_Graph.hasDragged = true;
		_Graph.graphBrowser.hideExpandButtons();
	},
	handleDoubleClickNodeEvent: (clickedNode) => {
		window.clearTimeout(_Graph.clickTimeout);
		_Graph.hasDoubleClicked = true;
		return false;
	},
	handleClickNodeEvent: (clickedNode) => {
		let node = clickedNode.data.node;

		if (_Graph.hasDoubleClicked) {
			return false;
		}

		if (_Graph.hasDragged) {
			_Graph.hasDragged = false;
			return false;
		}

		_Graph.clickTimeout = window.setTimeout(() => {
			_Entities.showProperties(node);
		}, _Graph.doubleClickTime);

		window.setTimeout(() => {
			_Graph.hasDoubleClicked = false;
		}, _Graph.doubleClickTime + 10);
	},
	handleClickEdgeEvent: (clickedEdge) => {

		if (_Graph.hasDragged) {
			_Graph.hasDragged = false;
			return false;
		}

		_Graph.hasDoubleClicked = false;

		_Entities.showProperties(clickedEdge.data.edge);
	},
	renderNodeExpanderInfoButton: (colorKey, label) => {
		return `
			<div class="nodeExpander-infobutton">
				<div class="circle" style="background-color: ${_Graph.color[colorKey] || '#e5e5e5'}"></div>
				${label}
			</div>
		`;
	},
	renderNodeTooltip: (node) => {

		_Graph.graphBrowser.hideExpandButtons();

		return `
			<div class="tooltipArrowUp"></div>
			<div class="graphTooltip">
				<div class="tooltipHeader flex items-center justify-between p-2">
					<span class="truncate" style="max-width: 300px;">${node.labelForTooltip}</span>
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 12, 12, _Icons.getSvgIconClassesForColoredIcon(['closeTooltipBtn', 'icon-grey', 'cursor-pointer']), 'Close')}
				</div>
				<div class="tooltipContent py-1" style="border-top: solid ${_Graph.color[node.nodeType]} 4px;">
					<div class="tooltipBody">
						<table class="tooltipTable">
							<tbody>
								<tr>
									<td class="tooltipTableLabel">Id:</td>
									<td class="graphTooltipSelectable">${node.id}</td>
								</tr>
								<tr>
									<td class="tooltipTableLabel">Type:</td>
									<td class="graphTooltipSelectable">${node.nodeType}</td>
								</tr>
							</tbody>
						</table>
					</div>
					<div id="tooltipFooter${node.id}" class="tooltipFooter pt-2">
						<div>
							<button id="tooltipBtnProps" value="${node.id}" data-type="${node.nodeType}">Properties</button>
							<button id="tooltipBtnDrop" value="${node.id}">Remove</button>
							<button id="tooltipBtnDel" value="${node.id}">Delete</button>
						</div>
					</div>
				</div>
			</div>
		`;
	},
	renderEdgeTooltip: (edge) => {

		return `
			<div class="tooltipArrowUp"></div>
			<div class="graphTooltip">
				<div class="tooltipHeader flex items-center justify-between p-2">
					<span>${edge.label}</span>
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 12, 12, _Icons.getSvgIconClassesForColoredIcon(['closeTooltipBtn', 'icon-grey', 'cursor-pointer']), 'Close')}
				</div>
				<div class="tooltipBody">
					<table class="tooltipTable">
						<tbody>
							<tr>
								<td class="tooltipTableLabel">Id: </td>
								<td class="graphTooltipSelectable">${edge.id}</td>
							</tr>
							<tr>
								<td class="tooltipTableLabel">relType: </td>
								<td class="graphTooltipSelectable">${edge.relType}</td>
							</tr>
							<tr>
								<td class="tooltipTableLabel">Source: </td>
								<td class="graphTooltipSelectable">${edge.source}</td>
							</tr>
							<tr>
								<td class="tooltipTableLabel">Target: </td>
								<td class="graphTooltipSelectable edgeTableEl">${edge.target}</td>
							</tr>
						</tbody>
					</table>
				</div>
				<div id="edgeTooltipFooter" class="tooltipFooter">
					<div>
						<button id="tooltipBtnProps" value="${edge.id}">Properties</button>
						<button id="tooltipBtnDel" value="${edge.id}">Delete</button>
					</div>
				</div>
			</div>
		`;
	},

	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/graph_editor.css">

			<div id="graph-box" class="graphNoSelect">

				<div id="graph-info"></div>

			<!--	<div id="cypher-params">-->
			<!--		<h3>Cypher Parameters</h3>-->
			<!--		${_Icons.getSvgIcon(_Icons.iconAdd, 12, 12, _Icons.getSvgIconClassesForColoredIcon(['icon-green']))} -->
			<!--	</div>-->

			<!--	<div><h3>Saved Queries</h3></div>-->
			<!--	<div id="saved-queries"></div>-->

				<div class="canvas" id="graph-canvas"></div>
			<!--	<div id="node-types" class="graph-object-types"></div>-->
			<!--	<div id="relationship-types" class="graph-object-types"></div>-->

			</div>
		`,
		functions: config => `
			<div class="query-box rest flex mr-4">
				<div class="relative">
					<input class="search" name="rest" size="39" placeholder="REST query" autocomplete="off">
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 12, 12, _Icons.getSvgIconClassesForColoredIcon(['clearSearchIcon', 'icon-lightgrey', 'cursor-pointer']), 'Clear REST')}
				</div>
				<a class="icon" id="exec-rest">
					${_Icons.getSvgIcon(_Icons.iconRunButton, 24, 24, _Icons.getSvgIconClassesNonColorIcon())}
				</a>
			</div>

			${Structr.isInMemoryDatabase ? '' : `
				<div class="query-box cypher flex mr-4">
					<div class="relative max-h-5">
						<textarea class="search min-h-5 min-w-64" name="cypher" cols="39" rows="1" placeholder="Cypher query"></textarea>
						${_Icons.getSvgIcon(_Icons.iconCrossIcon, 12, 12, _Icons.getSvgIconClassesForColoredIcon(['clearSearchIcon', 'icon-lightgrey', 'cursor-pointer']), 'Clear Cypher')}
					</div>
					<a class="icon" id="exec-cypher">
						${_Icons.getSvgIcon(_Icons.iconRunButton, 24, 24, _Icons.getSvgIconClassesNonColorIcon())}
					</a>
				</div>
			`}

			<div class="dropdown-menu dropdown-menu-large">
				<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green">
					${_Icons.getSvgIcon(_Icons.iconSliders, 16, 16, ['mr-2'])} Display Settings
				</button>

				<div class="dropdown-menu-container">
					<div class="heading-row">
						<h3>Display Options</h3>
					</div>
					<div class="row">
						<label class="block"><input ${!_Graph.nodeLabelsHidden ? 'checked' : ''} type="checkbox" id="toggleNodeLabels" name="toggleNodeLabels"> Node labels</label>
					</div>
					<div class="row">
						<label class="block"><input ${!_Graph.edgeLabelsHidden ? 'checked' : ''} type="checkbox" id="toggleEdgeLabels" name="toggleEdgeLabels"> Edge labels</label>
					</div>

					<div class="heading-row">
						<h3>Set Layout</h3>
					</div>
					<div class="row">
						<a id="fruchterman-controlElement" class="block">Fruchterman</a>
					</div>
					<div class="row">
						<a id="dagre-controlElement" class="block">Dagre</a>
					</div>

					<div class="heading-row">
						<h3>Dynamic Layouting On/Off</h3>
					</div>

					<div class="row">
						<a id="forceAtlas-controlElement" class="block ${_Graph.animating ? 'active' : ''}">ForceAtlas2</a>
					</div>

					<!--	<div class="heading-row"><h3>Selection Tools</h3></div>-->
					<!--	<button id="newSelectionGroup">New Selection</button>-->
					<!--	<button id="selectionLasso">Lasso</button>-->
					<!--	<div id="selectionToolsTableContainer">-->
					<!--		<table id="selectionToolsTable" class="graphtable responsive">-->
					<!--			<thead id="selectionToolsTableHead">-->
					<!--			<tr>-->
					<!--				<th>Group</th>-->
					<!--				<th>Fixed</th>-->
					<!--				<th>Hidden</th>-->
					<!--				<th>Remove</th>-->
					<!--			</tr>-->
					<!--			</thead>-->
					<!--			<tbody id="selectiontools-selectionTable-groupSelectionItems">-->
					<!--			<tbody>-->
					<!--		</table>-->
					<!--	</div>-->

				</div>
			</div>

			<button id="clear-graph" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Clear Graph</button>
		`,
		filters: config => `
			<div id="filters" class="">

				<div id="nodeFilters">
					<h3>Node Filters</h3>

					<div><input type="checkbox" id="graphTypeToggleCore"><label for="graphTypeToggleCore"> Core Types</label></div>
					<div><input type="checkbox" id="graphTypeToggleUi"><label for="graphTypeToggleUi"> UI Types</label></div>
					<div><input type="checkbox" id="graphTypeToggleCustom"><label for="graphTypeToggleCustom"> Custom Types</label></div>
					<div><input type="checkbox" id="graphTypeToggleHtml"><label for="graphTypeToggleHtml"> HTML Types</label></div>
					<div><input type="checkbox" id="graphTypeToggleLog"><label for="graphTypeToggleLog"> Log Types</label></div>
					<div><input type="checkbox" id="graphTypeToggleOther"><label for="graphTypeToggleOther"> Other Types</label></div>
				</div>

				<div id="relFilters">
					<h3>Relationship Filters</h3>
				</div>
			</div>
		`,
		newSelectionGroup: config => `
			<tr>
				<td><input type="checkbox" name="selectedGroup[]" value="selected.${config.newId}">${config.newId}</td>
				<td class="text-center"><input type="checkbox" name="Fixed[]" value="fixed.${config.newId}"></td>
				<td class="text-center"><input type="checkbox" name="Hidden[]" value="hidden.${config.newId}"></td>
				<td class="text-center"><button class="selectionTableRemoveBtn" value="${config.newId}">Remove</button></td>
			</tr>
		`,
	}
};