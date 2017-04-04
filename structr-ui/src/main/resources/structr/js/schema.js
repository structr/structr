/*
 * Copyright (C) 2010-2017 Structr GmbH
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
var canvas, instance, res, nodes = {}, rels = {}, localStorageSuffix = '_schema_' + port, undefinedRelType = 'UNDEFINED_RELATIONSHIP_TYPE', initialRelType = undefinedRelType;
var maxZ = 0, reload = false;
var schemaContainer;
var inheritanceTree, inheritanceSlideout;

$(document).ready(function() {
	Structr.registerModule(_Schema);
	Structr.classes.push('schema');
});

var _Schema = {
	_moduleName: 'schema',
	schemaLoading: false,
	schemaLoaded: false,
	connectorStyle: undefined,
	zoomLevel: undefined,
	nodePositions: undefined,
	new_attr_cnt: 0,
	selectedRel: undefined,
	relHighlightColor: 'red',
	availableTypeNames: [],
	hiddenSchemaNodes: [],
	hiddenSchemaNodesKey: 'structrHiddenSchemaNodes_' + port,
	schemaPositionsKey: 'structrSchemaPositions_' + port,
	showSchemaOverlaysKey: 'structrShowSchemaOverlays_' + port,
	schemaMethodsHeightsKey: 'structrSchemaMethodsHeights_' + port,
	schemaActiveTabLeftKey: 'structrSchemaActiveTabLeft_' + port,
	activeSchemaToolsSelectedTabLevel1Key: 'structrSchemaToolsSelectedTabLevel1_' + port,
	activeSchemaToolsSelectedTabLevel2Key: 'structrSchemaToolsSelectedTabLevel2_' + port,
	schemaZoomLevelKey: localStorageSuffix + 'zoomLevel',
	schemaConnectorStyleKey: localStorageSuffix + 'connectorStyle',
	selectionInProgress: false,
	selectBox: undefined,
	mouseDownCoords: {x:0, y:0},
	mouseUpCoords: {x:0, y:0},
	nodeDragStartpoint: undefined,
	selectedNodes: [],
	typeOptions: '<select class="property-type"><option value="">--Select--</option>'
		+ '<option value="String">String</option>'
		+ '<option value="StringArray">String[]</option>'
		+ '<option value="Integer">Integer</option>'
		+ '<option value="IntegerArray">Integer[]</option>'
		+ '<option value="Long">Long</option>'
		+ '<option value="LongArray">Long[]</option>'
		+ '<option value="Double">Double</option>'
		+ '<option value="Boolean">Boolean</option>'
		+ '<option value="BooleanArray">Boolean[]</option>'
		+ '<option value="Enum">Enum</option>'
		+ '<option value="Date">Date</option>'
		+ '<option value="Count">Count</option>'
		+ '<option value="Function">Function</option>'
		+ '<option value="Notion">Notion</option>'
		+ '<option value="Join">Join</option>'
		+ '<option value="Cypher">Cypher</option>'
		+ '<option value="Thumbnail">Thumbnail</option>',
	currentNodeDialogId:null,
	ignoreNextSchemaRecompileNotification: false,
	reload: function(callback) {
		if (reload) {
			return;
		}
		var x = window.scrollX;
		var y = window.scrollY;
		reload = true;
		//_Schema.storePositions();	/* CHM: don't store positions on every reload, let automatic positioning do its job.. */
		schemaContainer.empty();
		_Schema.init({x: x, y: y}, callback);
		_Schema.resize();

	},
	storePositions: function() {
		$.each($('#schema-graph .node'), function(i, n) {
			var node = $(n);
			var type = node.text();
			var obj = node.position();
			obj.left = (obj.left - canvas.offset().left) / _Schema.zoomLevel;
			obj.top  = (obj.top  - canvas.offset().top)  / _Schema.zoomLevel;
			_Schema.nodePositions[type] = obj;
		});

		LSWrapper.setItem(_Schema.schemaPositionsKey, _Schema.nodePositions);
	},
	clearPositions: function() {
		LSWrapper.removeItem(_Schema.schemaPositionsKey);
		_Schema.reload();
	},
	init: function(scrollPosition, callback) {

		_Schema.schemaLoading = false;
		_Schema.schemaLoaded = false;
		_Schema.schema = [];
		_Schema.keys = [];

		schemaContainer.append('<div class="schema-input-container"></div>');

		var schemaInputContainer = $('.schema-input-container');

		Structr.ensureIsAdmin(schemaInputContainer, function() {

			_Schema.connectorStyle = LSWrapper.getItem(_Schema.schemaConnectorStyleKey) || 'Flowchart';
			_Schema.zoomLevel = parseFloat(LSWrapper.getItem(_Schema.schemaZoomLevelKey)) || 1.0;

			schemaInputContainer.append('<div class="input-and-button"><input class="schema-input" id="type-name" type="text" size="10" placeholder="New type"><button id="create-type" class="btn"><i class="' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" /> Add</button></div>');
			schemaInputContainer.append('<div class="input-and-button"><input class="schema-input" id="ggist-url" type="text" size="20" placeholder="Enter GraphGist URL"><button id="gg-import" class="btn">Start Import</button></div>');
			$('#gg-import').on('click', function(e) {
				var btn = $(this);
				var text = btn.text();
				Structr.updateButtonWithAjaxLoaderAndText(btn, text);
				e.preventDefault();
				_Schema.importGraphGist($('#ggist-url').val(), text);
			});

			schemaInputContainer.append('<select id="connector-style"></select>');
			['Flowchart', 'Bezier', 'StateMachine', 'Straight'].forEach(function(style) {
				$('#connector-style').append('<option value="' + style + '" ' + (style === _Schema.connectorStyle ? 'selected="selected"' : '') + '>' + style + '</option>');
			});
			$('#connector-style').on('change', function() {
				var newStyle = $(this).val();
				_Schema.connectorStyle = newStyle;
				LSWrapper.setItem(_Schema.schemaConnectorStyleKey, newStyle);
				_Schema.reload();
			});

			schemaInputContainer.append('<div id="zoom-slider"></div>');
			$('#zoom-slider').slider({
				min:0.25,
				max:1,
				step:0.05,
				value:_Schema.zoomLevel,
				slide: function( event, ui ) {
					_Schema.zoomLevel = ui.value;
					LSWrapper.setItem(_Schema.schemaZoomLevelKey, _Schema.zoomLevel);
					_Schema.setZoom(_Schema.zoomLevel, instance, [0,0], $('#schema-graph')[0]);
					if (_Schema.selectedNodes.length > 0) {
						_Schema.updateSelectedNodes();
					}
					_Schema.resize();
				}
			});

			schemaInputContainer.append('<input type="checkbox" id="schema-show-overlays" name="schema-show-overlays"><label for="schema-show-overlays"> Show relationship labels</label>');
			schemaInputContainer.append('<button class="btn" id="schema-tools"><i class="' + _Icons.getFullSpriteClass(_Icons.wrench_icon) + '" /> Tools</button>');

			$('#schema-show-overlays').on('change', function() {
				_Schema.updateOverlayVisibility($(this).prop('checked'));
			});
			$('#schema-tools').on('click', _Schema.openSchemaToolsDialog);

			$('#type-name').on('keyup', function(e) {

				if (e.keyCode === 13) {
					e.preventDefault();
					if ($('#type-name').val().length) {
						$('#create-type').click();
					}
					return false;
				}

			});
			$('#create-type').on('click', function() {
				_Schema.createNode($('#type-name').val());
			});

			jsPlumb.ready(function() {
				schemaContainer.append('<div class="canvas" id="schema-graph"></div>');

				canvas = $('#schema-graph');

				instance = jsPlumb.getInstance({
					//Connector: "StateMachine",
					PaintStyle: {
						lineWidth: 5,
						strokeStyle: "#81ce25"
					},
					Endpoint: ["Dot", {radius: 6}],
					EndpointStyle: {
						fillStyle: "#aaa"
					},
					Container: "schema-graph",
					ConnectionOverlays: [
						["PlainArrow", {
								location: 1,
								width: 15,
								length: 12
							}
						]
					]
				});

				_Schema.loadSchema(function() {

					$('.node').css({zIndex: ++maxZ});

					instance.bind('connection', function(info) {
						_Schema.ignoreNextSchemaRecompileNotification = true;
						_Schema.connect(Structr.getIdFromPrefixIdString(info.sourceId, 'id_'), Structr.getIdFromPrefixIdString(info.targetId, 'id_'));
					});
					instance.bind('connectionDetached', function(info) {
						_Schema.askDeleteRelationship(info.connection.scope);
						_Schema.reload();
					});
					reload = false;

					_Schema.setZoom(_Schema.zoomLevel, instance, [0,0], $('#schema-graph')[0]);

					$('._jsPlumb_connector').click(function(e) {
						e.stopPropagation();
						_Schema.selectRel($(this));
					});

					canvas.on('mousedown', function(e) {
						if (e.which === 1) {
							_Schema.clearSelection();
							_Schema.selectionStart(e);
						}
					});

					canvas.on('mousemove', function(e) {
						if (e.which === 1) {
							_Schema.selectionDrag(e);
						}
					});

					canvas.on('mouseup', function(e) {
						_Schema.selectionStop();
					});

					_Schema.resize();

					Structr.unblockMenu(500);

					var overlaysVisible = LSWrapper.getItem(_Schema.showSchemaOverlaysKey);
					var showSchemaOverlays = (overlaysVisible === null) ? true : overlaysVisible;
					_Schema.updateOverlayVisibility(showSchemaOverlays);

					if (scrollPosition) {
						window.scrollTo(scrollPosition.x, scrollPosition.y);
					}

					if (typeof callback === "function") {
						callback();
					}
				});

			});

			_Schema.resize();

			Structr.adaptUiToPresentModules();

		});

	},
	selectRel: function(rel) {
		_Schema.clearSelection();

		_Schema.selectedRel = rel;
		_Schema.selectedRel.css({zIndex: ++maxZ});
		_Schema.selectedRel.nextAll('._jsPlumb_overlay').slice(0, 3).css({zIndex: ++maxZ, borderColor: _Schema.relHighlightColor, background: 'rgba(255, 255, 255, 1)'});
		var pathElements = _Schema.selectedRel.find('path');
		pathElements.css({stroke: _Schema.relHighlightColor});
		$(pathElements[1]).css({fill: _Schema.relHighlightColor});
	},
	clearSelection: function() {
		// deselect selected node
		$('.node', canvas).removeClass('selected');
		_Schema.selectionStop();

		// deselect selected Relationship
		if (_Schema.selectedRel) {
			_Schema.selectedRel.nextAll('._jsPlumb_overlay').slice(0, 3).css({borderColor:'', background: 'rgba(255, 255, 255, .8)'});
			var pathElements = _Schema.selectedRel.find('path');
			pathElements.css({stroke: '', fill: ''});
			$(pathElements[1]).css('fill', '');
			_Schema.selectedRel = undefined;
		}
	},
	selectionStart: function(e) {
		canvas.addClass('noselect');
		_Schema.selectionInProgress = true;
		var schemaOffset = canvas.offset();
		_Schema.mouseDownCoords.x = e.pageX - schemaOffset.left;
		_Schema.mouseDownCoords.y = e.pageY - schemaOffset.top;
	},
	selectionDrag: function(e) {
		if (_Schema.selectionInProgress === true) {
			var schemaOffset = canvas.offset();
			_Schema.mouseUpCoords.x = e.pageX - schemaOffset.left;
			_Schema.mouseUpCoords.y = e.pageY - schemaOffset.top;
			_Schema.drawSelectElem();
		}
	},
	selectionStop: function() {
		_Schema.selectionInProgress = false;
		if (_Schema.selectBox) {
			_Schema.selectBox.remove();
			_Schema.selectBox = undefined;
		}
		_Schema.updateSelectedNodes();
		canvas.removeClass('noselect');
	},
	updateSelectedNodes: function() {
		_Schema.selectedNodes = [];
		var canvasOffset = canvas.offset();
		$('.node.selected', canvas).each(function(idx, el) {
			$el = $(el);
			var elementOffset = $el.offset();
			_Schema.selectedNodes.push({
				nodeId: $el.attr('id'),
				name: $el.children('b').text(),
				pos: {
					top:  (elementOffset.top  - canvasOffset.top ),
					left: (elementOffset.left - canvasOffset.left)
				}
			});
		});
	},
	drawSelectElem: function() {
		if (!_Schema.selectBox || !_Schema.selectBox.length) {
			canvas.append('<svg id="schema-graph-select-box"><path version="1.1" xmlns="http://www.w3.org/1999/xhtml" fill="none" stroke="#aaa" stroke-width="5"></path></svg>');
			_Schema.selectBox = $('#schema-graph-select-box');
		}
		var cssRect = {
			position: 'absolute',
			top:    Math.min(_Schema.mouseDownCoords.y, _Schema.mouseUpCoords.y)  / _Schema.zoomLevel,
			left:   Math.min(_Schema.mouseDownCoords.x, _Schema.mouseUpCoords.x)  / _Schema.zoomLevel,
			width:  Math.abs(_Schema.mouseDownCoords.x - _Schema.mouseUpCoords.x) / _Schema.zoomLevel,
			height: Math.abs(_Schema.mouseDownCoords.y - _Schema.mouseUpCoords.y) / _Schema.zoomLevel
		};
		_Schema.selectBox.css(cssRect);
		_Schema.selectBox.find('path').attr('d', 'm 0 0 h ' + cssRect.width + ' v ' + cssRect.height + ' h ' + (-cssRect.width) + ' v ' + (-cssRect.height) + ' z');
		_Schema.selectNodesInRect(cssRect);
	},
	selectNodesInRect: function(selectionRect) {
		var selectedElements = [];

		$('.node', canvas).each(function(idx, el) {
			var $el = $(el);
			if (_Schema.isElemInSelection($el, selectionRect)) {
				selectedElements.push($el);
				$el.addClass('selected');
			} else {
				$el.removeClass('selected');
			}
		});
	},
	isElemInSelection: function($el, selectionRect) {
		var elPos = $el.offset();
		elPos.top /= _Schema.zoomLevel;
		elPos.left /= _Schema.zoomLevel;
		var schemaOffset = canvas.offset();
		return !(
			(elPos.top) > (selectionRect.top + schemaOffset.top / _Schema.zoomLevel + selectionRect.height) ||
			elPos.left > (selectionRect.left + schemaOffset.left / _Schema.zoomLevel + selectionRect.width) ||
			(elPos.top + $el.innerHeight()) < (selectionRect.top + schemaOffset.top / _Schema.zoomLevel) ||
			(elPos.left + $el.innerWidth()) < (selectionRect.left + schemaOffset.left / _Schema.zoomLevel)
		);
	},
	onload: function() {
		main.append(
			'<div id="inheritance-tree" class="slideOut slideOutLeft"><div class="compTab" id="inheritanceTab">Inheritance Tree</div>Search: <input type="text" id="search-classes"><div id="inheritance-tree-container" class="ver-scrollable"></div></div>'
			+ '<div id="schema-container"></div>'
		);
		schemaContainer = $('#schema-container');
		inheritanceSlideout = $('#inheritance-tree');
		inheritanceTree = $('#inheritance-tree-container');

		var updateCanvasTranslation = function() {
			var windowHeight = $(window).height();
			$('.ver-scrollable').css({
				height: (windowHeight - inheritanceTree.offset().top - 25) + 'px'
			});
			canvas.css('transform', _Schema.getSchemaCSSTransform());
			_Schema.resize();
		};

		$('#inheritanceTab').on('click', function() {
			_Pages.leftSlideoutTrigger(this, inheritanceSlideout, [], _Schema.schemaActiveTabLeftKey, updateCanvasTranslation, updateCanvasTranslation);
		});

		_Schema.init();
		Structr.updateMainHelpLink('http://docs.structr.org/frontend-user-guide#Schema');

		$(window).off('resize');
		$(window).on('resize', function() {
			_Schema.resize();
		});
	},
	/**
	 * Read the schema from the _schema REST resource and call 'callback'
	 * after the complete schema is loaded.
	 */
	loadSchema: function(callback) {
		// Avoid duplicate loading of schema
		if (_Schema.schemaLoading) {
			return;
		}
		_Schema.schemaLoading = true;

		_Schema.loadNodes(function() {
			_Schema.loadRels(callback);
		});

	},
	processSchemaRecompileNotification: function () {

		if (Structr.isModuleActive(_Schema)) {

			if (_Schema.ignoreNextSchemaRecompileNotification === false) {

				new MessageBuilder()
						.title("Schema recompiled")
						.info("Another user made changes to the schema. Do you want to reload to see the changes?")
						.specialInteractionButton("Reload", _Schema.reloadSchemaAfterRecompileNotification, "Ignore")
						.uniqueClass('schema')
						.show();

			} else {

				_Schema.ignoreNextSchemaRecompileNotification = false;

			}

		}

	},
	reloadSchemaAfterRecompileNotification: function () {

		if (_Schema.currentNodeDialogId !== null) {

			// we break the current dialog the hard way (because if we 'click' the close button we might re-open the previous dialog
			$.unblockUI({
				fadeOut: 25
			});

			var currentView = LSWrapper.getItem(_Entities.activeEditTabPrefix  + '_' + _Schema.currentNodeDialogId);

			_Schema.reload(function() {
				_Schema.openEditDialog(_Schema.currentNodeDialogId, currentView);
			});

		} else {

			_Schema.reload();

		}

	},
	isSchemaLoaded: function() {
		var all = true;
		if (!_Schema.schemaLoaded) {
			$.each(_Schema.types, function(t, type) {
				all &= (_Schema.schema[type] && _Schema.schema[type] !== null);
			});
		}
		_Schema.schemaLoaded = all;
		return _Schema.schemaLoaded;
	},
	loadNodes: function(callback) {
		var url = rootUrl + 'SchemaNode/ui?sort=hierarchyLevel&order=asc';
		_Schema.hiddenSchemaNodes = JSON.parse(LSWrapper.getItem(_Schema.hiddenSchemaNodesKey)) || [];
		$.ajax({
			url: url,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			success: function(data) {

				var hierarchy = {};
				var x=0, y=0;

				_Schema.nodePositions = LSWrapper.getItem(_Schema.schemaPositionsKey);
				if (!_Schema.nodePositions) {

					var nodePositions = {};

					// positions are stored the 'old' way => convert to the 'new' way
					data.result.map(function(entity) {
						return entity.name;
					}).forEach(function(typeName) {

						var nodePos = JSON.parse(LSWrapper.getItem(typeName + localStorageSuffix + 'node-position'));
						if (nodePos) {
							nodePositions[typeName] = nodePos.position;

							LSWrapper.removeItem(typeName + localStorageSuffix + 'node-position');
						}
					});

					_Schema.nodePositions = nodePositions;
					LSWrapper.setItem(_Schema.schemaPositionsKey, _Schema.nodePositions);

					// After we have converted all types we try to find *all* outdated type positions and delete them
					Object.keys(JSON.parse(LSWrapper.getAsJSON())).forEach(function(key) {

						if (key.endsWith('node-position')) {
							LSWrapper.removeItem(key);
						}
					});
				}

				_Schema.loadClassTree(data.result);

				_Schema.availableTypeNames = [];

				data.result.forEach(function(entity) {

					_Schema.availableTypeNames.push(entity.name);

					var level   = 0;
					var outs    = entity.relatedTo.length;
					var ins     = entity.relatedFrom.length;
					var hasRels = (outs > 0 || ins > 0);

					if (ins === 0 && outs === 0) {

						// no rels => push down
						level += 100;

					} else {

						if (outs === 0) {
							level += 10;
						}

						level += ins;
					}

					if (entity.isBuiltinType && !hasRels) {
						level += 10;
					}

					if (!hierarchy[level]) { hierarchy[level] = []; }
					hierarchy[level].push(entity);
				});

				Object.keys(hierarchy).forEach(function(key) {

					hierarchy[key].forEach(function(entity) {

						nodes[entity.id] = entity;

						if (_Schema.hiddenSchemaNodes.length > 0 && _Schema.hiddenSchemaNodes.indexOf(entity.name) > -1) {
							return;
						}

						var isBuiltinType = entity.isBuiltinType;
						var id = 'id_' + entity.id;
						canvas.append('<div class="schema node compact'
								+ (isBuiltinType ? ' light' : '')
								+ '" id="' + id + '"><b>' + entity.name + '</b>'
								+ '<i class="icon delete ' + _Icons.getFullSpriteClass((isBuiltinType ? _Icons.delete_disabled_icon : _Icons.delete_icon)) + '" />'
								+ '<i class="icon edit ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" />'
								+ '</div>');

						var node = $('#' + id);

						if (!isBuiltinType) {
							node.children('b').on('click', function() {
								_Schema.makeAttrEditable(node, 'name');
							});

							node.children('.delete').on('click', function() {
								Structr.confirmation('<h3>Delete schema node \'' + entity.name + '\'?</h3><p>This will delete all incoming and outgoing schema relationships as well, but no data will be removed.</p>',
										function() {
											$.unblockUI({
												fadeOut: 25
											});
											_Schema.deleteNode(entity.id);
										});
							});
						}

						node.on('mousedown', function() {
							node.css({zIndex: ++maxZ});
						});

						var getX = function() {
							return (x * 300) + ((y % 2) * 150) + 40;
						};
						var getY = function() {
							return (y * 150) + 50;
						};
						var calculatePosition = function() {
							var calculatedX = getX(x);
							if (calculatedX > 1500) {
								y++;
								x = 0;
								calculatedX = getX(x);
							}
							var calculatedY = getY(y);
							return { left: calculatedX, top: calculatedY };
						};

						var storedPosition = _Schema.nodePositions[entity.name];
						if (!storedPosition) {

							var calculatedPosition = calculatePosition(x, y);
							var count = 0; // prevent endless looping

							while (_Schema.overlapsExistingNodes(calculatedPosition) && count++ < 1000) {
								x++;
								calculatedPosition = calculatePosition();
							}
						}

						node.offset({
							left: storedPosition ? storedPosition.left : calculatedPosition.left,
							top: storedPosition ? storedPosition.top : calculatedPosition.top
						});

						$('.edit', node).on('click', function(e) {

							e.stopPropagation();
							var id = Structr.getId($(this).closest('.schema.node'));

							if (!id) {
								return false;
							}

							_Schema.openEditDialog(id);

							return false;
						});

						nodes[entity.id + '_top'] = instance.addEndpoint(id, {
							anchor: "Top",
							maxConnections: -1,
							isTarget: true,
							deleteEndpointsOnDetach: false
						});
						nodes[entity.id + '_bottom'] = instance.addEndpoint(id, {
							anchor: "Bottom",
							maxConnections: -1,
							isSource: true,
							deleteEndpointsOnDetach: false
						});

						instance.draggable(id, {
							containment: true,
							start: function(ui) {
								var nodeOffset   = $(ui.el).offset();
								var canvasOffset = canvas.offset();
								_Schema.nodeDragStartpoint = {
									top:  (nodeOffset.top  - canvasOffset.top ),
									left: (nodeOffset.left - canvasOffset.left)
								};
							},
							drag: function(ui) {

								var $element = $(ui.el);

								if (!$element.hasClass('selected')) {

									_Schema.clearSelection();

								} else {

									var nodeOffset = $element.offset();
									var canvasOffset = canvas.offset();

									var posDelta = {
										top:  (_Schema.nodeDragStartpoint.top  - nodeOffset.top ),
										left: (_Schema.nodeDragStartpoint.left - nodeOffset.left)
									};

									_Schema.selectedNodes.forEach(function(selectedNode) {
										if (selectedNode.nodeId !== $element.attr('id')) {
											$('#' + selectedNode.nodeId).offset({
												top:  (selectedNode.pos.top  - posDelta.top  > canvasOffset.top ) ? (selectedNode.pos.top  - posDelta.top ) : canvasOffset.top,
												left: (selectedNode.pos.left - posDelta.left > canvasOffset.left) ? (selectedNode.pos.left - posDelta.left) : canvasOffset.left
											});
										}
									});

									instance.repaintEverything();

								}
							},
							stop: function() {
								_Schema.storePositions();
								_Schema.updateSelectedNodes();
								_Schema.resize();
							}
						});

						x++;
					});

					y++;
					x = 0;
				});

				if (typeof callback === 'function') {
					callback();
				}

			}
		});
	},
	openEditDialog: function(id, targetView, callback) {

		targetView = targetView || LSWrapper.getItem(_Entities.activeEditTabPrefix  + '_' + id);

		_Schema.currentNodeDialogId = id;

		dialogMeta.hide();

		Command.get(id, function(entity) {

			var title = 'Edit schema node';
			var method = _Schema.loadNode;

			if (entity.type === "SchemaRelationshipNode") {
				title = 'Edit schema relationship';
				method = _Schema.loadRelationship;
			}

			Structr.dialog(title, function() {
				dialogMeta.show();
			}, function() {
				_Schema.currentNodeDialogId = null;

				if (callback) {
					callback();
				}
				dialogMeta.show();
				instance.repaintEverything();
			});

			method(entity, dialogHead, dialogText, targetView);

			_Schema.clearSelection();
		});

	},
	loadRels: function(callback) {
		var url = rootUrl + 'schema_relationship_nodes';
		$.ajax({
			url: url,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			success: function(data) {

				var existingRels = {};
				var relCnt = {};
				$.each(data.result, function(i, res) {

					if (!nodes[res.sourceId] || !nodes[res.targetId] || _Schema.hiddenSchemaNodes.indexOf(nodes[res.sourceId].name) > -1 || _Schema.hiddenSchemaNodes.indexOf(nodes[res.targetId].name) > -1) {
						return;
					}

					var relIndex = res.sourceId + '-' + res.targetId;
					if (relCnt[relIndex] === undefined) {
						relCnt[relIndex] = 0;
					} else {
						relCnt[relIndex]++;

					}

					existingRels[relIndex] = true;
					if (existingRels[res.targetId + '-' + res.sourceId]) {
						relCnt[relIndex] += existingRels[res.targetId + '-' + res.sourceId];
					}

					var radius = 20 + 10 * relCnt[relIndex];
					var stub   = 30 + 80 * relCnt[relIndex];
					var offset =     0.2 * relCnt[relIndex];

					rels[res.id] = instance.connect({
						source: nodes[res.sourceId + '_bottom'],
						target: nodes[res.targetId + '_top'],
						deleteEndpointsOnDetach: false,
						scope: res.id,
						connector: [_Schema.connectorStyle, {curviness: 200, cornerRadius: radius, stub: [stub, 30], gap: 6, alwaysRespectStubs: true }],
						paintStyle: { lineWidth: 5, strokeStyle: res.permissionPropagation !== 'None' ? "#ffad25" : "#81ce25" },
						overlays: [
							["Label", {
									cssClass: "label multiplicity",
									label: res.sourceMultiplicity ? res.sourceMultiplicity : '*',
									location: .2 + offset,
									id: "sourceMultiplicity"
								}
							],
							["Label", {
									cssClass: "label rel-type",
									label: '<div id="rel_' + res.id + '">' + (res.relationshipType === initialRelType ? '&nbsp;' : res.relationshipType)
											+ ' <i title="Edit schema relationship" class="edit icon ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '"></i>'
											+ ' <i title="Remove schema relationship" class="remove icon ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '"></i></div>',
									location: .5 + offset,
									id: "label"
								}
							],
							["Label", {
									cssClass: "label multiplicity",
									label: res.targetMultiplicity ? res.targetMultiplicity : '*',
									location: .8 - offset,
									id: "targetMultiplicity"
								}
							]
						]
					});

					$('#rel_' + res.id).parent().on('mouseover', function() {
						$('#rel_' + res.id + ' .icon').showInlineBlock();
						$('#rel_' + res.id + ' .target-multiplicity').addClass('hover');
					}).on('mouseout', function() {
						$('#rel_' + res.id + ' .icon').hide();
						$('#rel_' + res.id + ' .target-multiplicity').removeClass('hover');
					});

					$('#rel_' + res.id + ' .edit').on('click', function() {
						_Schema.openEditDialog(res.id);
					});

					$('#rel_' + res.id + ' .remove').on('click', function() {
						_Schema.askDeleteRelationship(res.id, res.relationshipType);
						return false;
					});
				});

				if (callback) {
					callback();
				}
			}
		});
	},
	loadNode: function(entity, headEl, contentEl, targetView) {

		if (!targetView) {
			targetView = 'local';
		}

		var id = '___' + entity.id;
		headEl.append('<div id="' + id + '_head" class="schema-details"></div>');
		var headContentDiv = $('#' + id + '_head');

		headContentDiv.append('<b>' + entity.name + '</b>');

		if (!entity.isBuiltinType) {
			headContentDiv.append(' extends <select class="extends-class-select"></select>');
			headContentDiv.append(' <i id="edit-parent-class" class="icon edit ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" title="Edit parent class" />');

			$("#edit-parent-class", headContentDiv).click(function() {

				if ($(this).hasClass('disabled')) {
					return;
				}

				var typeName = $('.extends-class-select', headContentDiv).val().split('.').pop();

				Command.search(typeName, 'SchemaNode', function(results) {
					if (results.length === 1) {

						_Schema.openEditDialog(results[0].id, undefined, function() {

							window.setTimeout(function() {

								_Schema.openEditDialog(entity.id);

							}, 250);

						});

					} else if (results.length === 0) {

						new MessageBuilder().warning("Can not open entity edit dialog for class '" + typeName + "' - <b>no corresponding</b> schema node found").show();

					} else {

						new MessageBuilder().warning("Can not open entity edit dialog for class '" + typeName + "' - <b>multiple corresponding</b> schema node found").show();

					}
				});

			});

			if (entity.extendsClass && entity.extendsClass.indexOf('org.structr.dynamic.') === 0) {
				$("#edit-parent-class").removeClass('disabled');
			} else {
				$("#edit-parent-class").addClass('disabled');
			}
		}

		headContentDiv.append('<div id="tabs" style="margin-top:20px;"><ul></ul></div>');
		var mainTabs = $('#tabs', headContentDiv);

		contentEl.append('<div id="' + id + '_content" class="schema-details"></div>');
		var contentDiv = $('#' + id + '_content');

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'local', 'Local Attributes', targetView === 'local', function(c) {
			_Schema.appendLocalProperties(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'views', 'Views', targetView === 'views', function(c) {
			_Schema.appendViews(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'methods', 'Methods', targetView === 'methods', function(c) {
			_Schema.appendMethods(c, entity);
		}, _Schema.getMethodsInitFunction());

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'remote', 'Remote Attributes', targetView === 'remote', function(c) {
			_Schema.appendRemoteProperties(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'builtin', 'Inherited Attributes', targetView === 'builtin', function(c) {
			_Schema.appendBuiltinProperties(c, entity);
		});

		if (!entity.isBuiltinType) {

			headContentDiv.children('b').on('click', function() {
				_Schema.makeAttrEditable(headContentDiv, 'name');
			});

		}

		var classSelect = $('.extends-class-select', headEl);
		$.get(rootUrl + '_schema', function(data) {
			var result = data.result;
			var classNames = [];
			$.each(result, function(t, cls) {
				var type = cls.type;
				var fqcn = cls.className;
				if ( cls.isRel || !type || type.startsWith('_') || fqcn.startsWith('org.structr.web.entity.html') || fqcn.endsWith('.' + entity.name) ) {
					return;
				}
				classNames.push(fqcn);
			});

			classNames.sort();

			classNames.forEach( function(classname) {
				classSelect.append('<option ' + (entity.extendsClass === classname ? 'selected="selected"' : '') + ' value="' + classname + '">' + classname + '</option>');
			});

			classSelect.chosen({ search_contains: true, width: '500px' });
		});

		classSelect.on('change', function() {
			var obj = {extendsClass: $(this).val()};
			_Schema.storeSchemaEntity('schema_properties', entity, JSON.stringify(obj), function() {
				// enable or disable the edit button
				if (obj.extendsClass.indexOf('org.structr.dynamic.') === 0) {
					$("#edit-parent-class").removeClass('disabled');
				} else {
					$("#edit-parent-class").addClass('disabled');
				}
			});
		});

		Structr.resize();
	},
	loadRelationship: function(entity, headEl, contentEl) {

		var id = '___' + entity.id;
		headEl.append('<div id="' + id + '_head" class="schema-details">'
			+ '<table id="relationship-options"><tr><td colspan=2 id="basic-options"></td></tr><tr><td id="cascading-options"></td><td id="propagation-options"></td></tr></table>'
			+ '<button id="edit-rel-options-button"><i class="edit icon ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" /> Edit relationship options</button>'
			+ '<button id="save-rel-options-button"><i class="save icon ' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" /> Save changes</button>'
			+ '<button id="cancel-rel-options-button"><i class="' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" /> Discard changes</button>'
			+ '<div id="tabs" style="margin-top:20px;"><ul></ul></div>'
			+ '</div>'
		);

		$('#basic-options').append('<span class="relationship-emphasis"><span id="source-type-name"></span> &#8212;</span>'
			+ '<select disabled id="source-multiplicity-selector" data-attr-name="sourceMultiplicity"><option value="1">1</option><option value="*">*</option></select>'
			+ '<span class="relationship-emphasis">&#8212;[</span>'
			+ '<input disabled id="relationship-type-name" data-attr-name="relationshipType">'
			+ '<span class="relationship-emphasis">]&#8212;</span>'
			+ '<select disabled id="target-multiplicity-selector" data-attr-name="targetMultiplicity"><option value="1">1</option><option value="*">*</option></select>'
			+ '<span class="relationship-emphasis">&#8212;&#9658; <span id="target-type-name"></span></span>'
		);

		$('#cascading-options').append('<h3>Cascading Delete</h3>'
			+ '<p>Direction of automatic removal of related nodes when a node is deleted</p>'
			+ '<select disabled id="cascading-delete-selector" data-attr-name="cascadingDeleteFlag"><option value="0">NONE</option><option value="1">SOURCE_TO_TARGET</option><option value="2">TARGET_TO_SOURCE</option><option value="3">ALWAYS</option><option value="4">CONSTRAINT_BASED</option></select>'
			+ '<h3>Automatic Creation of Related Nodes</h3>'
			+ '<p>Direction of automatic creation of related nodes when a node is created</p>'
			+ '<select disabled id="autocreate-selector" data-attr-name="autocreationFlag"><option value="0">NONE</option><option value="1">SOURCE_TO_TARGET</option><option value="2">TARGET_TO_SOURCE</option><option value="3">ALWAYS</option></select>'
		);

		$('#propagation-options').append('<h3>Permission Resolution</h3>'
			+ '<select disabled id="propagation-selector" data-attr-name="permissionPropagation"><option value="None">NONE</option><option value="Out">SOURCE_TO_TARGET</option><option value="In">TARGET_TO_SOURCE</option><option value="Both">ALWAYS</option></select>'
			+ '<table style="margin: 12px 0 0 0;"><tr id="propagation-table">'
			+ '<td class="selector"><p>Read</p><select disabled id="read-selector" data-attr-name="readPropagation"><option value="Add">Add</option><option value="Keep">Keep</option><option value="Remove">Remove</option></select></td>'
			+ '<td class="selector"><p>Write</p><select disabled id="write-selector" data-attr-name="writePropagation"><option value="Add">Add</option><option value="Keep">Keep</option><option value="Remove">Remove</option></select></td>'
			+ '<td class="selector"><p>Delete</p><select disabled id="delete-selector" data-attr-name="deletePropagation"><option value="Add">Add</option><option value="Keep">Keep</option><option value="Remove">Remove</option></select></td>'
			+ '<td class="selector"><p>AccessControl</p><select disabled id="access-control-selector" data-attr-name="accessControlPropagation"><option value="Add">Add</option><option value="Keep">Keep</option><option value="Remove">Remove</option></select></td>'
			+ '</tr></table>'
			+ '<p style="margin-top:12px">Hidden properties</p><textarea disabled id="masked-properties" data-attr-name="propertyMask" />'
		);

		var mainTabs = $('#' + id + '_head #tabs');

		contentEl.append('<div id="' + id + '_content" class="schema-details"></div>');
		var contentDiv = $('#' + id + '_content');

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'local', 'Local Attributes', true, function(c) {
			_Schema.appendLocalProperties(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'views', 'Views', false, function(c) {
			_Schema.appendViews(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'methods', 'Methods', false, function(c) {
			_Schema.appendMethods(c, entity);
		}, _Schema.getMethodsInitFunction());

		var selectRelationshipOptions = function(rel) {
			$('#source-type-name').text(nodes[rel.sourceId].name);
			$('#source-multiplicity-selector').val(rel.sourceMultiplicity || '*');
			$('#relationship-type-name').val(rel.relationshipType === initialRelType ? '' : rel.relationshipType);
			$('#target-multiplicity-selector').val(rel.targetMultiplicity || '*');
			$('#target-type-name').text(nodes[rel.targetId].name);

			$('#cascading-delete-selector').val(rel.cascadingDeleteFlag || 0);
			$('#autocreate-selector').val(rel.autocreationFlag || 0);
			$('#propagation-selector').val(rel.permissionPropagation || 'None');
			$('#read-selector').val(rel.readPropagation || 'Remove');
			$('#write-selector').val(rel.writePropagation || 'Remove');
			$('#delete-selector').val(rel.deletePropagation || 'Remove');
			$('#access-control-selector').val(rel.accessControlPropagation || 'Remove');
			$('#masked-properties').val(rel.propertyMask);
		};

		selectRelationshipOptions(entity);

		var editButton = $('#edit-rel-options-button');
		var saveButton = $('#save-rel-options-button').hide();
		var cancelButton = $('#cancel-rel-options-button').hide();

		editButton.on('click', function(e) {
			e.preventDefault();

			$('#relationship-options select, #relationship-options textarea, #relationship-options input').attr('disabled', false);
			$('#relationship-options select, #relationship-options textarea, #relationship-options input').css('color', '');
			$('#relationship-options select, #relationship-options textarea, #relationship-options input').css('background-color', '');
			editButton.hide();
			saveButton.show();
			cancelButton.show();
		});

		saveButton.on('click', function(e) {

			var newData = {
				sourceMultiplicity: $('#source-multiplicity-selector').val(),
				relationshipType: $('#relationship-type-name').val(),
				targetMultiplicity: $('#target-multiplicity-selector').val(),
				cascadingDeleteFlag: parseInt($('#cascading-delete-selector').val()),
				autocreationFlag: parseInt($('#autocreate-selector').val()),
				permissionPropagation: $('#propagation-selector').val(),
				readPropagation: $('#read-selector').val(),
				writePropagation: $('#write-selector').val(),
				deletePropagation: $('#delete-selector').val(),
				accessControlPropagation: $('#access-control-selector').val(),
				propertyMask: $('#masked-properties').val()
			};

			Object.keys(newData).forEach(function(key) {
				if (key === 'relationshipType' && newData[key].trim() === '') {
					newData[key] = initialRelType;
				}
				if ( (entity[key] === newData[key])
						|| (key === 'cascadingDeleteFlag' && !(entity[key]) && newData[key] === 0)
						|| (key === 'autocreationFlag' && !(entity[key]) && newData[key] === 0)
						|| (key === 'propertyMask' && !(entity[key]) && newData[key].trim() === '')
				) {
					delete newData[key];
				}
			});

			if (Object.keys(newData).length > 0) {
				_Schema.editRelationship(entity, newData, function() {
					Object.keys(newData).forEach(function(attribute) {
						blinkGreen($('#relationship-options [data-attr-name=' + attribute + ']'));
						entity[attribute] = newData[attribute];
					});
				}, function() {
					Object.keys(newData).forEach(function(attribute) {
						blinkRed($('#relationship-options [data-attr-name=' + attribute + ']'));
					});
				});
			}

			$('#relationship-options select, #relationship-options textarea, #relationship-options input').attr('disabled', true);

			editButton.show();
			saveButton.hide();
			cancelButton.hide();
		});

		cancelButton.on('click', function(e) {

			selectRelationshipOptions(entity);
			$('#relationship-options select, #relationship-options textarea, #relationship-options input').attr('disabled', true);

			editButton.show();
			saveButton.hide();
			cancelButton.hide();
		});

		Structr.resize();
	},
	appendLocalProperties: function(el, entity) {

		el.append('<table class="local schema-props"><thead><th>JSON Name</th><th>DB Name</th><th>Type</th><th>Format/Code</th><th>Notnull</th><th>Uniq.</th><th>Idx</th><th>Default</th><th class="actions-col">Action</th></thead></table>');
		el.append('<i title="Add local attribute" class="add-icon add-local-attribute ' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" />');

		var propertiesTable = $('.local.schema-props', el);

		_Schema.sort(entity.schemaProperties);

		$.each(entity.schemaProperties, function(i, prop) {
			_Schema.appendLocalProperty(propertiesTable, prop);
		});
		$('.add-local-attribute', el).on('click', function() {
			var rowClass = 'new' + (_Schema.new_attr_cnt++);
			propertiesTable.append('<tr class="' + rowClass + '"><td><input size="15" type="text" class="property-name" placeholder="Enter JSON name" autofocus></td>'
					+ '<td><input size="15" type="text" class="property-dbname" placeholder="Enter DB name"></td>'
					+ '<td>' + _Schema.typeOptions + '</td>'
					+ '<td><input size="15" type="text" class="property-format" placeholder="Enter format"></td>'
					+ '<td><input class="not-null" type="checkbox"></td>'
					+ '<td><input class="unique" type="checkbox"></td>'
					+ '<td><input class="indexed" type="checkbox"></td>'
					+ '<td><input class="property-default" size="10" type="text"></td>'
					+ '<td>'
					+ '<i title="Save changes" class="create-icon create-property ' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" />'
					+ '<i title="Discard changes" class="remove-icon remove-property ' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" />'
					+ '</td></div>');

			$('.' + rowClass + ' .remove-property', propertiesTable).on('click', function() {
				var self = $(this);
				self.closest('tr').remove();
			});

			$('.' + rowClass + ' .create-property', propertiesTable).on('click', function() {
				var self = $(this);
				if (!self.data('save-pending')) {
					_Schema.collectAndSaveNewLocalProperty(self, rowClass, propertiesTable, entity);
				}
			});
		});

	},
	appendViews: function(el, entity) {

		el.append('<table class="views schema-props"><thead><th>Name</th><th>Attributes</th><th class="actions-col">Action</th></thead></table>');
		el.append('<i title="Add view" class="add-icon add-view ' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" />');

		var viewsTable = $('.views.schema-props', el);

		_Schema.sort(entity.schemaViews);

		$.each(entity.schemaViews, function(i, view) {
			_Schema.appendView(viewsTable, view, entity);
		});

		$('.add-view', el).on('click', function() {
			viewsTable.append('<tr><td style="width:20%;"><input size="15" type="text" class="view property-name" placeholder="Enter view name"></td>'
					+ '<td class="view-properties-select"></td>'
					+ '<td>'
					+ '<i title="Save changes" class="create-icon save-action ' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" />'
					+ '<i title="Discard changes" class="discard-icon cancel-action ' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" />'
					+ '<i title="Remove view" class="remove-icon remove-action hidden ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />'
					+ '</td></tr>');

			var tr = viewsTable.find('tr').last();
			_Schema.appendViewSelectionElement(tr, {name: 'new'}, entity);

			$('.save-action', tr).on('click', function() {
				_Schema.createOrSaveView(tr, entity);
			});

			$('.cancel-action', tr).on('click', function() {
				tr.remove();
			});
		});
	},
	appendView: function(el, view, entity) {

		el.append('<tr><td style="width:20%;"><input size="15" type="text" class="view property-name" placeholder="Enter view name" value="' + escapeForHtmlAttributes(view.name) + '"' + (view.isBuiltinView ? 'disabled' : '') + '></td>'
				+ '<td class="view-properties-select"></td>'
				+ '<td>'
				+ '<i title="Save changes" class="create-icon save-action ' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" />'
				+ '<i title="Discard changes" class="discard-icon cancel-action ' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" />'
				+ (view.isBuiltinView ? '<i title="Reset built-in view" class="remove-icon remove-action ' + _Icons.getFullSpriteClass(_Icons.arrow_undo_icon) + '" />' : '<i title="Remove view" class="remove-icon remove-action ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />')
				+ '</td></tr>');

		var tr = el.find('tr').last();
		_Schema.appendViewSelectionElement(tr, view, entity);

		_Schema.initViewRow(tr, entity, view);

	},
	initViewRow: function(tr, entity, view) {

		var activate = function() {
			$('.save-action', tr).removeClass('hidden');
			$('.cancel-action', tr).removeClass('hidden');
			$('.remove-action', tr).addClass('hidden');
			$('.reset-action', tr).addClass('hidden');
		};

		var deactivate = function() {
			$('.save-action', tr).addClass('hidden');
			$('.cancel-action', tr).addClass('hidden');
			$('.remove-action', tr).removeClass('hidden');
			$('.reset-action', tr).removeClass('hidden');
		};
		deactivate();

		$('.view.property-name', tr).on('change', activate).on('keyup', activate);
		$('.view.property-attrs', tr).on('change', activate);


		$('.save-action', tr).off('click').on('click', function() {
			_Schema.createOrSaveView(tr, entity, view);
		});

		$('.cancel-action', tr).off('click').on('click', function() {

			var select = $('select', tr);

			$('.view.property-name', tr).val(view.name);

			Command.listSchemaProperties(entity.id, view.name, function(data) {

				data.forEach(function(prop) {
					$('option[value="' + prop.name + '"]', select).prop('selected', prop.isSelected);
				});

				select.trigger('chosen:updated');
			});

			deactivate();

		});

		$('.remove-action', tr).off('click').on('click', function() {
			_Schema.confirmRemoveSchemaEntity(view, $(this).attr('title'), function() { _Schema.openEditDialog(entity.id, 'views'); } );
		});

	},
	appendViewSelectionElement: function(tr, view, schemaEntity) {

		var propertySelectTd = $('.view-properties-select', tr).last();
		propertySelectTd.append('<select class="property-attrs view" multiple="multiple"></select>');
		var viewSelectElem = $('.property-attrs', propertySelectTd);

		Command.listSchemaProperties(schemaEntity.id, view.name, function(properties) {

			properties.forEach(function(prop) {

				var name       = prop.name;
				var isSelected = prop.isSelected ? ' selected="selected"' : '';
				var isDisabled = (view.name === 'ui' || prop.isDisabled) ? ' disabled="disabled"' : '';

				viewSelectElem.append('<option value="' + name + '"' + isSelected + isDisabled + '>' + name + '</option>');
			});

			viewSelectElem.chosen({ search_contains: true, width: '100%' });

		});

	},
	createOrSaveView: function(tr, entity, view) {

		var name  = $('.view.property-name', tr).val();
		var attrs = $('.view.property-attrs', tr).val();

		if (name && name.length) {

			var obj                = {};
			obj.schemaNode         = { id: entity.id };
			obj.schemaProperties   = _Schema.findSchemaPropertiesByNodeAndName(entity, attrs);
			obj.nonGraphProperties = _Schema.findNonGraphProperties(entity, attrs);
			obj.name               = name;

			_Schema.storeSchemaEntity('schema_views', (view || {}), JSON.stringify(obj), function(result) {

				if (view) {

					// we saved a view
					blinkGreen(tr);

					var oldName = view.name;
					view.schemaProperties = obj.schemaProperties;
					view.name             = obj.name;

					tr.removeClass(oldName).addClass(view.name);

					$('.save-action', tr).addClass('hidden');
					$('.cancel-action', tr).addClass('hidden');
					$('.reset-action', tr).removeClass('hidden');
					$('.remove-action', tr).removeClass('hidden');

				} else {

					// we created a view - get the view data
					if (result && result.result) {

						var id = result.result[0];

						$.ajax({
							url: rootUrl + id,
							type: 'GET',
							dataType: 'json',
							contentType: 'application/json; charset=utf-8',
							statusCode: {

								200: function(data) {

									var view = data.result;
									var name = view.name;

									blinkGreen(tr);

									_Schema.reload();

									tr.addClass(name);

									_Schema.initViewRow(tr, entity, view);
								}
							}
						});
					}
				}
			}, function(data) {
				Structr.errorFromResponse(data.responseJSON);
				blinkRed(tr);
			});

		} else {
			blinkRed($('.view.property-name', tr));
		}

	},
	appendMethods: function(el, entity) {

		el.append('<table class="actions schema-props"><thead><th>JSON Name</th><th>Code</th><th>Comment</th><th class="actions-col">Action</th></thead></table>');
		el.append('<button class="add-icon add-action-button"><i class="' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" /> Add method</button>');
		el.append('<button class="add-icon add-onCreate-button"><i class="' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" /> Add onCreate</button>');
		el.append('<button class="add-icon add-onSave-button"><i class="' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" /> Add onSave</button>');

		var actionsTable = $('.actions.schema-props', el);

		_Schema.sort(entity.schemaMethods);

		$.each(entity.schemaMethods, function(i, method) {
			_Schema.appendMethod(actionsTable, method, entity);
		});

		$('.add-action-button', el).on('click', function() {
			_Schema.appendEmptyMethod(actionsTable, entity);
		});

		$('.add-onCreate-button', el).on('click', function() {
			var tr = _Schema.appendEmptyMethod(actionsTable, entity);
			$('.property-name', tr).val(_Schema.getFirstFreeMethodName('onCreate'));
		});

		$('.add-onSave-button', el).on('click', function() {
			var tr = _Schema.appendEmptyMethod(actionsTable, entity);
			$('.property-name', tr).val(_Schema.getFirstFreeMethodName('onSave'));
		});

		var lineWrapping = LSWrapper.getItem(lineWrappingKey);
		el.append('<div class="editor-settings"><span><label for="lineWrapping">Line Wrapping:</label> <input id="lineWrapping" type="checkbox"' + (lineWrapping ? ' checked="checked" ' : '') + '></span></div>');
		$('#lineWrapping', el).on('change', function() {
			var inp = $(this);
			var isLinewrappingOn = inp.is(':checked');
			if  (isLinewrappingOn) {
				LSWrapper.setItem(lineWrappingKey, "1");
			} else {
				LSWrapper.removeItem(lineWrappingKey);
			}
			blinkGreen(inp.parent());

			$('.CodeMirror', actionsTable).each(function(idx, cmEl) {
				cmEl.CodeMirror.setOption('lineWrapping', isLinewrappingOn);
				cmEl.CodeMirror.refresh();
			});
		});
	},
	appendMethod: function(el, method, entity) {

		el.append('<tr class="' + method.name + '" data-type-name="' + entity.name + '" data-method-name="' + method.name + '">'
				+ '<td class="name-col"><div class="abs-pos-helper">'
					+ '<input size="15" type="text" class="action property-name" value="' + escapeForHtmlAttributes(method.name) + '">'
				+ '</div></td>'
				+ '<td><textarea rows="4" class="property-code action">' + escapeForHtmlAttributes(method.source || '') + '</textarea></td>'
				+ '<td><textarea rows="4" class="property-comment action">' + escapeForHtmlAttributes(method.comment || '') + '</textarea></td>'
				+ '<td>'
					+ '<i title="Add to favorites" class="add-to-favorites ' + _Icons.getFullSpriteClass(_Icons.star_icon) + '" />'
					+ '<i title="Save changes" class="create-icon save-action ' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" />'
					+ '<i title="Discard changes" class="remove-icon cancel-action ' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" />'
					+ '<i title="Remove method" class="remove-icon remove-action ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />'
				+ '</td></tr><tr><td title="Drag to resize" class="resize-handle" colspan="4"></td></tr>');

		// row containing resize handler
		var resizeHandlerRow = $('tr', el).last();

		// row containing method
		var tr = resizeHandlerRow.prev('tr');

		_Schema.makeSchemaMethodRowResizable(resizeHandlerRow);
		_Schema.initMethodRow(tr, entity, method);

		$('.add-to-favorites', tr).on('click', function() {
			Command.favorites('add', method.id, function() {
				blinkGreen($('.add-to-favorites', tr));
			});
		});

	},
	appendEmptyMethod: function(actionsTable, entity) {

		actionsTable.append('<tr>'
				+ '<td class="name-col"><div class="abs-pos-helper">'
					+ '<input size="15" type="text" class="action property-name" placeholder="Enter method name">'
				+ '</div></td>'
				+ '<td><textarea rows="4" class="action property-code" placeholder="Enter Code"></textarea></td>'
				+ '<td><textarea rows="4" class="action property-comment" placeholder="Enter comment"></textarea></td>'
				+ '<td>'
					+ '<i title="Add to favorites" class="add-to-favorites hidden ' + _Icons.getFullSpriteClass(_Icons.star_icon) + '" />'
					+ '<i title="Save changes" class="create-icon save-action ' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" />'
					+ '<i title="Discard changes" class="remove-icon cancel-action ' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" />'
					+ '<i title="Remove method" class="remove-icon remove-action hidden ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />'
				+ '</td>'
				+ '</tr><tr><td title="Drag to resize" class="resize-handle" colspan="4"></td></tr>');

		// row containing resize handler
		var resizeHandlerRow = $('tr', actionsTable).last();

		// row containing method
		var tr = resizeHandlerRow.prev('tr');

		_Schema.makeSchemaMethodRowResizable(resizeHandlerRow);

		// Intitialize editor(s)
		$('textarea.property-code', tr).each(function(i, txtarea) {
			_Schema.initCodeMirrorForMethodCode(txtarea);
		});

		$('textarea.property-comment', tr).each(function(i, txtarea) {
			_Schema.initCodeMirrorForMethodComment(txtarea);
		});

		$('.save-action', tr).on('click', function() {
			_Schema.createOrSaveMethod(tr, entity);
		});

		$('.cancel-action', tr).on('click', function() {
			tr.remove();
			resizeHandlerRow.remove();
		});

		return tr;

	},
	getFirstFreeMethodName: function(prefix) {
		var nextSuffix = 0;

		$('#tabView-methods .property-name').each(function(i, el) {
			var name = $(el).val();
			if (name.indexOf(prefix) === 0) {
				var suffix = name.slice(prefix.length);

				if (suffix === '') {
					nextSuffix = Math.max(nextSuffix, 1);
				} else {
					var parsed = parseInt(suffix);
					if (!isNaN(parsed)) {
						nextSuffix = Math.max(nextSuffix, parsed + 1);
					}
				}
			}
		});

		return prefix + (nextSuffix === 0 ? '' : (nextSuffix < 10 ? '0' + nextSuffix : nextSuffix));
	},
	initMethodRow: function(tr, entity, method) {

		var activate = function() {
			$('.save-action', tr).removeClass('hidden');
			$('.cancel-action', tr).removeClass('hidden');
			$('.remove-action', tr).addClass('hidden');
			$('.add-to-favorites', tr).addClass('hidden');
		};

		var deactivate = function() {
			$('.save-action', tr).addClass('hidden');
			$('.cancel-action', tr).addClass('hidden');
			$('.remove-action', tr).removeClass('hidden');
			$('.add-to-favorites', tr).removeClass('hidden');
		};
		deactivate();

		$('.property-name.action', tr).on('change', activate).on('keyup', activate);
		$('.property-code.action', tr).on('change', activate).on('keyup', activate);
		$('.property-comment.action', tr).on('change', activate).on('keyup', activate);

		$('.save-action', tr).off('click').on('click', function() {
			_Schema.createOrSaveMethod(tr, entity, method);
		});

		$('.cancel-action', tr).off('click').on('click', function() {

			$('.action.property-name', tr).val(method.name);
			$('.action.property-code', tr).val(method.source);
			$('.action.property-comment', tr).val(method.comment);
			($('.action.property-code', tr).closest('td').find('.CodeMirror').get(0).CodeMirror).setValue(method.source);
			($('.action.property-comment', tr).closest('td').find('.CodeMirror').get(0).CodeMirror).setValue(method.comment);

			deactivate();
		});

		$('.remove-action', tr).on('click', function() {
			_Schema.confirmRemoveSchemaEntity(method, 'Delete method', function() {
				_Schema.openEditDialog(method.schemaNode.id, 'methods', function() {
					$('li#tab-methods').click();
				});
			});
		});

		$('.add-to-favorites', tr).on('click', function() {
			Command.favorites('add', method.id, function() {
				blinkGreen($('.add-to-favorites', tr));
			});
		});

	},
	createOrSaveMethod: function(tr, entity, method) {

		var obj = {
			schemaNode: { id: entity.id },
			name:    $('.action.property-name', tr).val(),
			source:  $('.action.property-code', tr).val(),
			comment: $('.action.property-comment', tr).val()
		};

		if (obj.name && obj.name.length) {

			_Schema.storeSchemaEntity('schema_methods', (method || {}), JSON.stringify(obj), function(result) {

				if (method) {

					blinkGreen(tr);

					tr.removeClass(method.name).addClass(obj.name);

					$('.save-action', tr).addClass('hidden');
					$('.cancel-action', tr).addClass('hidden');
					$('.remove-action', tr).removeClass('hidden');
					$('.add-to-favorites', tr).removeClass('hidden');

				} else {

					if (result && result.result) {

						var id = result.result[0];

						$.ajax({
							url: rootUrl + id,
							type: 'GET',
							dataType: 'json',
							contentType: 'application/json; charset=utf-8',
							statusCode: {

								200: function(data) {

									blinkGreen(tr);

									var method = data.result;
									_Schema.initMethodRow(tr, entity, method);
								}
							}
						});
					}
				}
			},
			function(data) {
				Structr.errorFromResponse(data.responseJSON);
				blinkRed(tr);
			});

		} else {

			blinkRed($('.action.property-name', tr));

		}

	},
	appendRemoteProperties: function(el, entity) {

		el.append('<table class="related-attrs schema-props"><thead><th>JSON Name</th><th>Type, Direction and Remote type</th></thead></table>');

		entity.relatedTo.forEach(function(target) {
			_Schema.appendRelatedProperty($('.related-attrs', el), target, target.targetJsonName ? target.targetJsonName : target.oldTargetJsonName, true);
		});

		entity.relatedFrom.forEach(function(source) {
			_Schema.appendRelatedProperty($('.related-attrs', el), source, source.sourceJsonName ? source.sourceJsonName : source.oldSourceJsonName, false);
		});

	},
	appendBuiltinProperties: function(el, entity) {

		el.append('<table class="builtin schema-props"><thead><th>Declaring Class</th><th>JSON Name</th><th>Type</th><th>Notnull</th><th>Uniq.</th><th>Idx</th></thead></table>');

		var propertiesTable = $('.builtin.schema-props', el);

		_Schema.sort(entity.schemaProperties);

		Command.listSchemaProperties(entity.id, 'ui', function(data) {

			// sort by name
			_Schema.sort(data, "declaringClass", "name");

			$.each(data, function(i, prop) {

				if (prop.declaringClass !== entity.name) {

					var property = {
						name: prop.name,
						dbName: '',
						propertyType: prop.propertyType,
						isBuiltinProperty: true,
						notNull: prop.notNull,
						unique: prop.unique,
						indexed: prop.indexed,
						declaringClass: prop.declaringClass
					};

					_Schema.appendBuiltinProperty(propertiesTable, property);
				}
			});
		});

	},
	collectAndSaveNewLocalProperty: function(button, rowClass, el, entity) {

		var name = $('.' + rowClass + ' .property-name', el).val();
		var dbName = $('.' + rowClass + ' .property-dbname', el).val();
		var type = $('.' + rowClass + ' .property-type', el).val();
		var format = $('.' + rowClass + ' .property-format', el).val();
		var notNull = $('.' + rowClass + ' .not-null', el).is(':checked');
		var unique = $('.' + rowClass + ' .unique', el).is(':checked');
		var indexed = $('.' + rowClass + ' .indexed', el).is(':checked');
		var defaultValue = $('.' + rowClass + ' .property-default', el).val();

		if (name.length === 0) {

			blinkRed($('.' + rowClass + ' .property-name', el).closest('td'));

		} else if (type.length === 0) {

			blinkRed($('.' + rowClass + ' .property-type', el).closest('td'));

		} else {

			button.data('save-pending', true);

			var obj = {};
			obj.schemaNode   =  { id: entity.id };
			if (name)         { obj.name = name; }
			if (dbName)       { obj.dbName = dbName; }
			if (type)         { obj.propertyType = type; }
			if (format)       { obj.format = format; }
			if (notNull)      { obj.notNull = notNull; }
			if (unique)       { obj.unique = unique; }
			if (indexed)      { obj.indexed = indexed; }
			if (defaultValue) { obj.defaultValue = defaultValue; }

			// store property definition with an empty property object
			_Schema.storeSchemaEntity('schema_properties', {}, JSON.stringify(obj), function(result) {

				if (result && result.result) {

					var id = result.result[0];

					$.ajax({
						url: rootUrl + id,
						type: 'GET',
						dataType: 'json',
						contentType: 'application/json; charset=utf-8',
						statusCode: {

							200: function(data) {

								var property = data.result;
								var name     = property.name;
								var row      = $('.' + rowClass, el);

								blinkGreen(row);

								_Schema.reload();

								_Schema.unbindEvents(name);

								row.removeClass(rowClass).addClass('local').addClass(name);
								row = $('.local.' + name, el);

								$('.create-property', row).remove();
								$('.remove-property', row)
										.off('click')
										.attr('src', _Icons.delete_icon)
										.on('click', function() {
									_Schema.confirmRemoveSchemaEntity(property, 'Delete property', function() { _Schema.openEditDialog(property.schemaNode.id, 'local'); }, 'Property values will not be removed from data nodes.');
								});

								var $el = $("#tabView-views.propTabContent");
								$el.empty();
								_Schema.appendViews($el, entity);
								_Schema.bindEvents(property);
							}
						}
					});
				}

			}, function(data) {
				Structr.errorFromResponse(data.responseJSON);

				blinkRed($('.' + rowClass, el));

				button.data('save-pending', false);
			});
		}
	},
	confirmRemoveSchemaEntity: function(entity, title, callback, hint) {

		Structr.confirmation(
			'<h3>' + title + ' ' + entity.name + '?</h3>' + (hint ? '<p>' + hint + '</p>' : ''),
			function() {
				$.unblockUI({
					fadeOut: 25
				});

				_Schema.removeSchemaEntity(entity, callback);
			},
			callback
		);

	},
	resize: function() {

		Structr.resize();

		if (canvas) {

			var zoom = (instance ? instance.getZoom() : 1);
			var canvasPosition = canvas.position();
			var padding = 100;

			var canvasSize = {
				w: ($(window).width() - canvasPosition.left),
				h: ($(window).height() - canvasPosition.top)
			};

			$('.node').each(function(i, elem) {
				$elem = $(elem);
				canvasSize.w = Math.max(canvasSize.w, (($elem.position().left - canvasPosition.left) / zoom + $elem.width()) + padding);
				canvasSize.h = Math.max(canvasSize.h, (($elem.position().top - canvasPosition.top)  / zoom + $elem.height()) + padding);
			});

			if (canvasSize.w * zoom < $(window).width() - canvasPosition.left) {
				canvasSize.w = ($(window).width()) / zoom - canvasPosition.left + padding;
			}

			if (canvasSize.h * zoom < $(window).height() - canvasPosition.top) {
				canvasSize.h = ($(window).height()) / zoom  - canvasPosition.top + padding;
			}

			canvas.css({
				width: canvasSize.w + 'px',
				height: (canvasSize.h - 1) + 'px'
			});
		}

		$('body').css({
			position: 'relative'
		});

		$('html').css({
			background: '#fff'
		});

	},
	appendLocalProperty: function(el, property) {

		var key = property.name;

		el.append('<tr class="' + key + '"><td><input size="15" type="text" class="property-name" value="' + escapeForHtmlAttributes(property.name) + '"></td><td>'
				+ '<input size="15" type="text" class="property-dbname" value="' + escapeForHtmlAttributes(property.dbName) + '"></td><td>'
				+ _Schema.typeOptions + '</td>'
				+ (property.propertyType !== 'Function' ?  '<td><input size="15" type="text" class="property-format" value="' + (property.format ? escapeForHtmlAttributes(property.format) : '') + '"></td>' : '<td><button class="edit-read-function">Read</button><button class="edit-write-function">Write</button></td>')
				+ '<td><input class="not-null" type="checkbox"'
				+ (property.notNull ? ' checked="checked"' : '') + '></td><td><input class="unique" type="checkbox"'
				+ (property.unique ? ' checked="checked"' : '') + '</td><td><input class="indexed" type="checkbox"'
				+ (property.indexed ? ' checked="checked"' : '') + '</td><td>'
				+ '<input type="text" size="10" class="property-default" value="' + escapeForHtmlAttributes(property.defaultValue) + '">' + '</td><td>'
				+ (property.isBuiltinProperty ? '' : '<i title="Remove property" class="remove-icon remove-property ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />')
				+ '</td></tr>');

		_Schema.bindEvents(property);

		if (property.isBuiltinProperty) {
			_Schema.disable(property);
		}

	},
	appendBuiltinProperty: function(el, property) {

		var key = property.name;

		el.append('<tr class="' + key + '">'
				+ '<td>' + escapeForHtmlAttributes(property.declaringClass) + '</td>'
				+ '<td>' + escapeForHtmlAttributes(property.name) + '</td>'
				+ '<td>' + property.propertyType + '</td>'
				+ '<td>' + '<input class="not-null" type="checkbox" readonly="readonly"' + (property.notNull ? ' checked="checked"' : '') + '></td>'
				+ '<td>' + '<input class="unique" type="checkbox" readonly="readonly"' + (property.unique ? ' checked="checked"' : '') + '</td>'
				+ '<td>' + '<input class="indexed" type="checkbox" readonly="readonly"' + (property.indexed ? ' checked="checked"' : '')+ '</td>'
				+ '</tr>');

		_Schema.bindEvents(property);

		if (property.isBuiltinProperty) {
			_Schema.disable(property);
		}

	},
	disable: function(property) {

		var key = property.name;
		var el  = $('.local.schema-props');

		$('.' + key + ' .property-type option[value="' + property.propertyType + '"]', el).prop('disabled', true);

		if (property.propertyType && property.propertyType !== '') {
			$('.' + key + ' .property-name', el).prop('disabled', true);
			$('.' + key + ' .property-dbname', el).prop('disabled', true);
		}

		$('.' + key + ' .property-type', el).prop('disabled', true);
		$('.' + key + ' .property-format', el).prop('disabled', true);
		$('.' + key + ' button', el).prop('disabled', true);
		$('.' + key + ' .not-null', el).prop('disabled', true);
		$('.' + key + ' .unique', el).prop('disabled', true);
		$('.' + key + ' .indexed', el).prop('disabled', true);
		$('.' + key + ' .property-default', el).prop('disabled', true);
		$('.' + key + ' .remove-property', el).prop('disabled', true);

		$('.local .' + key).css('background-color', '#eee');
	},
	bindEvents: function(property) {

		var key = property.name;
		var el  = $('.local.schema-props');

		$('.' + key + ' .property-type option[value="' + property.propertyType + '"]', el).attr('selected', true).prop('disabled', null);

		var typeField = $('.' + key + ' .property-type', el);
		$('.' + key + ' .property-type option[value=""]', el).remove();

		if (property.propertyType === 'String' && !property.isBuiltinProperty) {
			if (!$('input.content-type', typeField.parent()).length) {
				typeField.after('<input type="text" size="5" class="content-type">');
			}
			$('.' + key + ' .content-type', el).on('blur', function() {
				_Schema.savePropertyDefinition(property);
			}).prop('disabled', null).val(property.contentType);
		}

		if (property.propertyType === 'Function' && !property.isBuiltinProperty) {
			if (!$('button.edit-read-function', typeField.parent()).length) {
				$('.' + key + ' .property-format', el).replaceWith('<button class="edit-read-function">Read</button><button class="edit-write-function">Write</button>');
			}
			$('.' + key + ' .content-type', el).on('blur', function() {
				_Schema.savePropertyDefinition(property);
			}).prop('disabled', null).val(property.contentType);
		}

		if (property.propertyType && property.propertyType !== '') {
			$('.' + key + ' .property-name', el).on('change', function() {
				_Schema.savePropertyDefinition(property);
			}).prop('disabled', null).val(property.name);
			$('.' + key + ' .property-dbname', el).on('change', function() {
				_Schema.savePropertyDefinition(property);
			}).prop('disabled', null).val(property.dbName);
		}

		$('.' + key + ' .property-type', el).on('change', function() {
			_Schema.savePropertyDefinition(property);
		}).prop('disabled', null).val(property.propertyType);

		$('.' + key + ' .property-format', el).on('blur', function() {
			_Schema.savePropertyDefinition(property);
		}).prop('disabled', null).val(property.format);

		$('.' + key + ' .edit-read-function', el).on('click', function() {
			_Schema.openCodeEditor($(this), property.id, 'readFunction', function() { _Schema.openEditDialog(property.schemaNode.id, 'local'); });
		}).prop('disabled', null);

		$('.' + key + ' .edit-write-function', el).on('click', function() {
			_Schema.openCodeEditor($(this), property.id, 'writeFunction', function() { _Schema.openEditDialog(property.schemaNode.id, 'local'); });
		}).prop('disabled', null);

		$('.' + key + ' .not-null', el).on('change', function() {
			_Schema.savePropertyDefinition(property);
		}).prop('disabled', null).val(property.notNull);

		$('.' + key + ' .unique', el).on('change', function() {
			_Schema.savePropertyDefinition(property);
		}).prop('disabled', null).val(property.unique);

		$('.' + key + ' .indexed', el).on('change', function() {
			_Schema.savePropertyDefinition(property);
		}).prop('disabled', null).val(property.indexed);

		$('.' + key + ' .property-default', el).on('change', function() {
			_Schema.savePropertyDefinition(property);
		}).prop('disabled', null).val(property.defaultValue);

		$('.' + key + ' .remove-property', el).on('click', function() {
			_Schema.confirmRemoveSchemaEntity(property, 'Delete property', function() { _Schema.openEditDialog(property.schemaNode.id, 'local'); }, 'Property values will not be removed from data nodes.');
		}).prop('disabled', null);

	},
	unbindEvents: function(key) {

		var el = $('.local.schema-props');

		$('.' + key + ' .property-type', el).off('change').prop('disabled', 'disabled');
		$('.' + key + ' .content-type', el).off('change').prop('disabled', 'disabled');
		$('.' + key + ' .property-format', el).off('blur').prop('disabled', 'disabled');
		$('.' + key + ' .not-null', el).off('change').prop('disabled', 'disabled');
		$('.' + key + ' .unique', el).off('change').prop('disabled', 'disabled');
		$('.' + key + ' .indexed', el).off('change').prop('disabled', 'disabled');
		$('.' + key + ' .property-default', el).off('change').prop('disabled', 'disabled');
		$('.' + key + ' .remove-property', el).off('click').prop('disabled', 'disabled');
		$('.' + key + ' button', el).off('click').prop('disabled', 'disabled');

	},
	openCodeEditor: function(btn, id, key, callback) {

		dialogMeta.show();

		Command.get(id, function(entity) {

			var title = 'Edit ' + key + ' of ' + entity.name;

			Structr.dialog(title, function() {}, function() {});

			_Schema.editCode(btn, entity, key, dialogText, function() {
				window.setTimeout(function() {
					callback();
				}, 250);
			});

		});

	},
	editCode: function(button, entity, key, element, callback) {

		var text = entity[key] || '';

		if (Structr.isButtonDisabled(button)) {
			return;
		}
		var div = element.append('<div class="editor"></div>');
		_Logger.log(_LogType.SCHEMA, div);
		var contentBox = $('.editor', element);
		contentType = contentType ? contentType : entity.contentType;
		var text1, text2;

		var lineWrapping = LSWrapper.getItem(lineWrappingKey);

		// Intitialize editor
		editor = CodeMirror(contentBox.get(0), {
			value: text,
			mode: contentType,
			lineNumbers: true,
			lineWrapping: lineWrapping,
			extraKeys: {
				"'.'":        _Contents.autoComplete,
				"Ctrl-Space": _Contents.autoComplete
			},
			indentUnit: 4,
			tabSize:4,
			indentWithTabs: true
		});

		Structr.resize();

		dialogBtn.append('<button id="editorSave" disabled="disabled" class="disabled">Save</button>');
		dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');

		dialogSaveButton = $('#editorSave', dialogBtn);
		saveAndClose = $('#saveAndClose', dialogBtn);

		saveAndClose.on('click', function(e) {
			e.stopPropagation();
			dialogSaveButton.click();
			setTimeout(function() {
				dialogSaveButton.remove();
				saveAndClose.remove();
				dialogCancelButton.click();
			}, 500);
		});

		editor.on('change', function(cm, change) {

			if (text === editor.getValue()) {
				dialogSaveButton.prop("disabled", true).addClass('disabled');
				saveAndClose.prop("disabled", true).addClass('disabled');
			} else {
				dialogSaveButton.prop("disabled", false).removeClass('disabled');
				saveAndClose.prop("disabled", false).removeClass('disabled');
			}

			$('#chars').text(editor.getValue().length);
			$('#words').text(editor.getValue().match(/\S+/g).length);
		});

		var scrollInfo = JSON.parse(LSWrapper.getItem(scrollInfoKey + '_' + entity.id));
		if (scrollInfo) {
			editor.scrollTo(scrollInfo.left, scrollInfo.top);
		}

		editor.on('scroll', function() {
			var scrollInfo = editor.getScrollInfo();
			LSWrapper.setItem(scrollInfoKey + '_' + entity.id, JSON.stringify(scrollInfo));
		});

		dialogCancelButton.off('click').on('click', function(e) {
			e.stopPropagation();
			e.preventDefault();
			if (callback) {
				callback();
			}
			dialogSaveButton = $('#editorSave', dialogBtn);
			saveAndClose = $('#saveAndClose', dialogBtn);
			dialogSaveButton.remove();
			saveAndClose.remove();
			return false;
		});

		dialogSaveButton.on('click', function(e) {
			e.stopPropagation();

			text1 = text;
			text2 = editor.getValue();

			if (!text1)
				text1 = '';
			if (!text2)
				text2 = '';

			_Logger.consoleLog('text1', text1);
			_Logger.consoleLog('text2', text2);

			if (text1 === text2) {
				return;
			}

			_Schema.ignoreNextSchemaRecompileNotification = true;

			Command.setProperty(entity.id, key, text2, false, function() {

				_Schema.ignoreNextSchemaRecompileNotification = false;

				Structr.showAndHideInfoBoxMessage('Code saved.', 'success', 2000, 200);
				_Schema.reload();
				dialogSaveButton.prop("disabled", true).addClass('disabled');
				saveAndClose.prop("disabled", true).addClass('disabled');
				Command.getProperty(entity.id, key, function(newText) {
					text = newText;
				});
			});

		});

		dialogMeta.append('<span class="editor-info"><label for="lineWrapping">Line Wrapping:</label> <input id="lineWrapping" type="checkbox"' + (lineWrapping ? ' checked="checked" ' : '') + '></span>');
		$('#lineWrapping').on('change', function() {
			var inp = $(this);
			if  (inp.is(':checked')) {
				LSWrapper.setItem(lineWrappingKey, "1");
				editor.setOption('lineWrapping', true);
			} else {
				LSWrapper.removeItem(lineWrappingKey);
				editor.setOption('lineWrapping', false);
			}
			blinkGreen(inp.parent());
			editor.refresh();
		});

		dialogMeta.append('<span class="editor-info">Characters: <span id="chars">' + editor.getValue().length + '</span></span>');
		dialogMeta.append('<span class="editor-info">Words: <span id="chars">' + (editor.getValue().match(/\S+/g) ? editor.getValue().match(/\S+/g).length : 0) + '</span></span>');

		editor.id = entity.id;

		editor.focus();

	},
	appendRelatedProperty: function(el, rel, key, out) {
		var relType = (rel.relationshipType === undefinedRelType) ? '' : rel.relationshipType;
		var relatedNodeId = (out ? rel.targetId : rel.sourceId);

		el.append('<tr class="' + key + '"><td><input size="15" type="text" class="property-name related" value="' + key + '"></td><td>'
				+ (out ? '-' : '&lt;-') + '[:' + relType + ']' + (out ? '-&gt;' : '-') + ' <span class="remote-schema-node" id="relId_' + rel.id + '">'+ nodes[relatedNodeId].name + '</span></td></tr>');

		$('#relId_' + rel.id, el).on('click', function(e) {
			e.stopPropagation();
			_Schema.openEditDialog(relatedNodeId);
			return false;
		});


		$('.' + key + ' .property-name', el).on('blur', function() {

			var newName = $(this).val();

			if (newName === '') {
				newName = undefined;
			}

			_Schema.setRelationshipProperty(rel, (out ? 'targetJsonName' : 'sourceJsonName'), newName, function() {
				blinkGreen($('.' + key, el));
			}, function() {
				blinkRed($('.' + key, el));
			});

		});

	},
	savePropertyDefinition: function(property) {

		var key = property.name;

		_Schema.unbindEvents(key);

		var name = $('.' + key + ' .property-name').val();
		var dbName = $('.' + key + ' .property-dbname').val();
		var type = $('.' + key + ' .property-type').val();
		var contentType = $('.' + key + ' .content-type').val();
		var format = $('.' + key + ' .property-format').val();
		var notNull = $('.' + key + ' .not-null').is(':checked');
		var unique = $('.' + key + ' .unique').is(':checked');
		var indexed = $('.' + key + ' .indexed').is(':checked');
		var defaultValue = $('.' + key + ' .property-default').val();

		if (name && name.length && type) {

			var obj = {};
			obj.name = name;
			obj.dbName = dbName;
			obj.propertyType = type;
			obj.contentType = contentType;
			obj.format = format;
			obj.notNull = notNull;
			obj.unique = unique;
			obj.indexed = indexed;
			obj.defaultValue = defaultValue;

			_Schema.storeSchemaEntity('schema_properties', property, JSON.stringify(obj), function() {

				blinkGreen($('.local .' + key));

				// accept values into property object
				property.name = obj.name;
				property.dbName = obj.dbName;
				property.propertyType = obj.propertyType;
				property.contentType = obj.contentType;
				property.format = obj.format;
				property.notNull = obj.notNull;
				property.unique = obj.unique;
				property.indexed = obj.indexed;
				property.defaultValue = obj.defaultValue;

				// update row class so that consequent changes can be applied
				$('.' + key).removeClass(key).addClass(property.name);
				_Schema.bindEvents(property);

			}, function(data) {

				Structr.errorFromResponse(data.responseJSON);

				blinkRed($('.local .' + key));
				_Schema.bindEvents(property);

			}, function() {

				_Schema.bindEvents(property);
			});
		}
	},
	findSchemaPropertiesByNodeAndName: function(entity, names) {

		var result = [];
		var props  = entity['schemaProperties'];

		if (names && names.length && props && props.length) {

			$.each(names, function(i, name) {

				$.each(props, function(i, prop) {

					if (prop.name === name) {
						result.push( { id: prop.id, name: prop.name } );
					}
				});
			});
		}

		return result;
	},
	findNonGraphProperties: function(entity, names) {

		var result = [];
		var props  = entity['schemaProperties'];

		if (names && names.length && props && props.length) {

			$.each(names, function(i, name) {

				var found = false;

				$.each(props, function(i, prop) {

					if (prop.name === name) {
						found = true;
						return;
					}
				});

				if (!found) {
					result.push(name);
				}
			});

		} else if (names) {

			result = names;
		}

		return result.join(', ');
	},
	removeSchemaEntity: function(entity, onSuccess, onError) {

		if (entity && entity.id) {

			_Schema.ignoreNextSchemaRecompileNotification = true;

			$.ajax({
				url: rootUrl + entity.id,
				type: 'DELETE',
				dataType: 'json',
				contentType: 'application/json; charset=utf-8',
				statusCode: {
					200: function() {
						_Schema.reload();
						if (onSuccess) {
							onSuccess();
						}
					},
					422: function(data) {
						//Structr.errorFromResponse(data.responseJSON);
						_Schema.ignoreNextSchemaRecompileNotification = false;
						if (onError) {
							onError(data);
						}
					}
				}
			});
		}
	},
	storeSchemaEntity: function(resource, entity, data, onSuccess, onError, onNoop) {

		_Schema.ignoreNextSchemaRecompileNotification = true;

		var obj = JSON.parse(data);

		if (entity && entity.id) {

			// store existing property
			$.ajax({
				url: rootUrl + entity.id,
				type: 'GET',
				dataType: 'json',
				contentType: 'application/json; charset=utf-8',
				statusCode: {

					200: function(existingData) {

						var changed = false;
						Object.keys(obj).forEach(function(key) {

							if (existingData.result[key] !== obj[key]) {
								changed |= true;
							}
						});

						if (changed) {

							$.ajax({
								url: rootUrl + entity.id,
								type: 'PUT',
								dataType: 'json',
								contentType: 'application/json; charset=utf-8',
								data: JSON.stringify(obj),
								statusCode: {
									200: function() {
										_Schema.reload();
										if (onSuccess) {
											onSuccess();
										}
									},
									422: function(data) {
										_Schema.ignoreNextSchemaRecompileNotification = false;
										if (onError) {
											onError(data);
										}
									}
								}
							});

						} else {

							if (onNoop) {
								onNoop();
							}

						}

					}
				}
			});

		} else {

			$.ajax({
				url: rootUrl + resource,
				type: 'POST',
				dataType: 'json',
				contentType: 'application/json; charset=utf-8',
				data: JSON.stringify(obj),
				statusCode: {
					201: function(result) {
						if (onSuccess) {
							onSuccess(result);
						}
					},
					422: function(data) {
						_Schema.ignoreNextSchemaRecompileNotification = false;
						if (onError) {
							onError(data);
						}
					}
				}
			});
		}
	},
	createNode: function(type) {
		_Schema.ignoreNextSchemaRecompileNotification = true;
		var url = rootUrl + 'schema_nodes';
		$.ajax({
			url: url,
			type: 'POST',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			data: JSON.stringify({name: type}),
			statusCode: {
				201: function() {
					_Schema.reload();
				},
				422: function(data) {
					_Schema.ignoreNextSchemaRecompileNotification = false;
					Structr.errorFromResponse(data.responseJSON, undefined, {requiresConfirmation: true});
				}
			}

		});
	},
	deleteNode: function(id) {
		_Schema.ignoreNextSchemaRecompileNotification = true;
		var url = rootUrl + 'schema_nodes/' + id;
		$.ajax({
			url: url,
			type: 'DELETE',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function() {
					_Schema.reload();
				},
				422: function(data) {
					_Schema.ignoreNextSchemaRecompileNotification = false;
					Structr.errorFromResponse(data.responseJSON);
				}
			}

		});
	},
	createRelationshipDefinition: function(sourceId, targetId, relationshipType) {
		_Schema.ignoreNextSchemaRecompileNotification = true;
		var data = {
			sourceId: sourceId,
			targetId: targetId,
			sourceMultiplicity: '*',
			targetMultiplicity: '*'
		};
		if (relationshipType && relationshipType.length) {
			data.relationshipType = relationshipType;
		}
		$.ajax({
			url: rootUrl + 'schema_relationship_nodes',
			type: 'POST',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			data: JSON.stringify(data),
			statusCode: {
				201: function() {
					_Schema.reload();
				},
				422: function(data) {
					_Schema.ignoreNextSchemaRecompileNotification = false;
					Structr.errorFromResponse(data.responseJSON);
				}
			}
		});
	},
	removeRelationshipDefinition: function(id) {
		_Schema.ignoreNextSchemaRecompileNotification = true;
		$.ajax({
			url: rootUrl + 'schema_relationship_nodes/' + id,
			type: 'DELETE',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function(data, textStatus, jqXHR) {
					_Schema.reload();
				},
				422: function(data) {
					_Schema.ignoreNextSchemaRecompileNotification = false;
					Structr.errorFromResponse(data.responseJSON);
				}
			}
		});
	},
	setRelationshipProperty: function(entity, key, value, onSuccess, onError) {
		var data = {};
		data[key] = cleanText(value);
		_Schema.editRelationship(entity, data, onSuccess, onError);
	},
	editRelationship: function(entity, newData, onSuccess, onError) {
		$.ajax({
			url: rootUrl + 'schema_relationship_nodes/' + entity.id,
			type: 'GET',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function(existingData) {

					var changed = Object.keys(newData).some(function(key) {
						return (existingData.result[key] !== newData[key]);
					});

					if (changed) {

						_Schema.ignoreNextSchemaRecompileNotification = true;

						$.ajax({
							url: rootUrl + 'schema_relationship_nodes/' + entity.id,
							type: 'PUT',
							dataType: 'json',
							contentType: 'application/json; charset=utf-8',
							data: JSON.stringify(newData),
							statusCode: {
								200: function(data, textStatus, jqXHR) {
									if (onSuccess) {
										onSuccess();
									}
									_Schema.reload();
								},
								422: function(data) {
									_Schema.ignoreNextSchemaRecompileNotification = false;
									if (onError) {
										onError(data);
									}
								}
							}
						});

					} else {
						// force a schema-reload so that we dont break the relationships
						_Schema.reload();
					}
				}
			}
		});
	},
	connect: function(sourceId, targetId) {
		_Schema.createRelationshipDefinition(sourceId, targetId, initialRelType);
	},
	askDeleteRelationship: function (resId, name) {
		name = name ? ' \'' + name + '\'' : '';
		Structr.confirmation('<h3>Delete schema relationship' + name + '?</h3>',
				function() {
					$.unblockUI({
						fadeOut: 25
					});
					_Schema.ignoreNextSchemaRecompileNotification = true;
					_Schema.detach(resId);
					_Schema.reload();
				});
	},
	detach: function(relationshipId) {
		_Schema.removeRelationshipDefinition(relationshipId);
	},
	makeAttrEditable: function(element, key, isRel) {

		// cut off three leading underscores and only use 32 characters (the UUID)
		var id = element.prop('id').substring(3, 35);

		element.off('hover');
		element.children('b').hide();
		var oldVal = $.trim(element.children('b').text());
		var input = $('input.new-' + key, element);

		if (!input.length) {
			element.prepend('<input type="text" size="' + (oldVal.length + 8) + '" class="new-' + key + '" value="' + oldVal + '">');
			input = $('input.new-' + key, element);
		}

		input.show().focus().select();
		input.on('blur', function() {
			if (!id) {
				return false;
			}
			Command.get(id, function(entity) {
				_Schema.changeAttr(entity, element, input, key, oldVal, isRel);
			});
			return false;
		});

		input.keypress(function(e) {
			if (e.keyCode === 13 || e.keyCode === 9) {
				e.preventDefault();
				if (!id) {
					return false;
				}
				Command.get(id, function(entity) {
					_Schema.changeAttr(entity, element, input, key, oldVal, isRel);
				});
				return false;
			}
		});
		element.off('click');
	},
	changeAttr: function(entity, element, input, key, oldVal, isRel) {
		var newVal = input.val();
		input.hide();
		element.children('b').text(newVal).show();
		if (oldVal !== newVal) {
			var obj = {};
			obj[key] = newVal;
			_Schema.storeSchemaEntity('', entity, JSON.stringify(obj), null, function(data) {
				Structr.errorFromResponse(data.responseJSON);
				element.children('b').text(oldVal).show();
				element.children('input').val(oldVal);
			});
		}
	},
	importGraphGist: function(graphGistUrl, text) {
		$.ajax({
			url: rootUrl + 'maintenance/importGist',
			type: 'POST',
			data: JSON.stringify({'url': graphGistUrl}),
			contentType: 'application/json',
			statusCode: {
				200: function() {
					var btn = $('#import-ggist');
					btn.removeClass('disabled').attr('disabled', null);
					btn.html(text + ' <i class="' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" />');
					window.setTimeout(function() {
						$('i', btn).fadeOut();
						document.location.reload();
					}, 1000);
				}
			}
		});
	},
	appendSnapshotsDialogToContainer: function(container) {

		container.append('<h3>Create snapshot</h3>');
		container.append('<p>Creates a new snapshot of the current schema configuration that can be restored later. You can enter an (optional) suffix for the snapshot.</p>');
		container.append('<p><input type="text" name="suffix" id="snapshot-suffix" placeholder="Enter a suffix" length="20" /> <button id="create-snapshot">Create snapshot</button></p>');

		var refresh = function() {

			table.empty();

			Command.snapshots('list', '', null, function(result) {

				result.forEach(function(data) {

					var snapshots = data.snapshots;

					snapshots.forEach(function(snapshot, i) {
						table.append('<tr><td class="snapshot-link name-' + i + '"><a href="#">' + snapshot + '</td><td style="text-align:right;"><button id="restore-' + i + '">Restore</button><button id="add-' + i + '">Add</button><button id="delete-' + i + '">Delete</button></td></tr>');

						$('.name-' + i + ' a').on('click', function() {
							Command.snapshots("get", snapshot, null, function(data) {

								var element = document.createElement('a');
								element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(data.schemaJson));
								element.setAttribute('download', snapshot);

								element.style.display = 'none';
								document.body.appendChild(element);

								element.click();
								document.body.removeChild(element);
							});
						});

						$('#restore-' + i).on('click', function() {
							_Schema.performSnapshotAction('restore', snapshot);
						});
						$('#add-' + i).on('click', function() {
							_Schema.performSnapshotAction('add', snapshot);
						});
						$('#delete-' + i).on('click', function() {
							Command.snapshots('delete', snapshot, null, refresh);
						});
					});

				});

			});
		};

		$('#create-snapshot').on('click', function() {

			var suffix = $('#snapshot-suffix').val();

			var types = [];
			if (_Schema.selectedNodes && _Schema.selectedNodes.length) {
				_Schema.selectedNodes.forEach(function(selectedNode) {
					types.push(selectedNode.name);
				});

				$('.label.rel-type', canvas).each(function(idx, el) {
					$el = $(el);

					var sourceType = $el.children('div').attr('data-source-type');
					var targetType = $el.children('div').attr('data-target-type');

					// include schema relationship if both source and target type are selected
					if (types.indexOf(sourceType) !== -1 && types.indexOf(targetType) !== -1) {
						types.push($el.children('div').attr('data-name'));
					}
				});
			}

			Command.snapshots('export', suffix, types, function(data) {

				var status = data[0].status;
				if (dialogBox.is(':visible')) {

					if (status === 'success') {
						Structr.showAndHideInfoBoxMessage('Snapshot successfully created', 'success', 2000, 200);
					} else {
						Structr.showAndHideInfoBoxMessage('Snapshot creation failed', 'error', 2000, 200);
					}
				}

				refresh();
			});

		});

		container.append('<h3>Available Snapshots</h3>');

		container.append('<table class="props" id="snapshots"></table>');

		var table = $('#snapshots');

		refresh();

		// update button
		container.append('<p style="text-align: right;"><button id="refresh-snapshots">Refresh</button></p>');
		$('#refresh-snapshots').on('click', refresh);

	},
	performSnapshotAction: function (action, snapshot) {

		_Schema.ignoreNextSchemaRecompileNotification = true;

		Command.snapshots(action, snapshot, null, function(data) {

			var status = data[0].status;

			if (status === 'success') {
				_Schema.reload();
			} else {
				_Schema.ignoreNextSchemaRecompileNotification = false;

				if (dialogBox.is(':visible')) {
					Structr.showAndHideInfoBoxMessage(status, 'error', 2000, 200);
				}
			}
		});

	},
	appendAdminToolsToContainer: function(container) {
		container.append('<table id="admin-tools-table"></table>');
		var toolsTable = $('#admin-tools-table');

		toolsTable.append('<tr><td colspan="3"><h3>General</h3></td></tr>');

		toolsTable.append('<tr id="general-operations"></tr>');
		$('#general-operations', toolsTable).append('<td><button id="rebuild-index"><i class="' + _Icons.getFullSpriteClass(_Icons.refresh_icon) + '" /> Rebuild Index</button></td>');
		$('#general-operations', toolsTable).append('<td><button id="flush-caches"><i class="' + _Icons.getFullSpriteClass(_Icons.refresh_icon) + '" /> Flush Caches</button></td>');
		$('#general-operations', toolsTable).append('<td><button id="clear-schema"><i class="' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" /> Clear Schema</button></td>');
		toolsTable.append('<tr><td><label for="rebuild-index">Rebuild database index<br>for all nodes and relationships</label></td><td><label for="flush-caches">Flushes internal caches<br>to refresh schema information</label></td><td><label for="clear-schema">Delete all schema nodes and<br>relationships in custom schema</label></td></tr>');

		toolsTable.append('<tr><td colspan="3"><h3>Nodes</h3></td></tr>');
		toolsTable.append('<tr><td colspan="3" id="node-operations"><select id="node-type-selector"><option selected value="">-- Select Node Type --</option><option disabled>──────────</option><option value="allNodes">All Node Types</option><option disabled>──────────</option></select></td></tr>');
		$('#node-operations', toolsTable).append('<button id="reindex-nodes">Re-Index Nodes</button>');
		$('#node-operations', toolsTable).append('<button id="add-node-uuids">Add UUIDs</button>');
		$('#node-operations', toolsTable).append('<button id="create-labels">Create Labels</button>');

		toolsTable.append('<tr><td colspan="3"><h3>Relationships</h3></td></tr>');
		toolsTable.append('<tr><td colspan="3" id="rel-operations"><select id="rel-type-selector"><option selected value="">-- Select Relationship Type --</option><option disabled>──────────</option><option value="allRels">All Relationship Types</option><option disabled>──────────</option></select></td></tr>');
		$('#rel-operations', toolsTable).append('<button id="reindex-rels">Re-Index Relationships</button>');
		$('#rel-operations', toolsTable).append('<button id="add-rel-uuids">Add UUIDs</button>');

		var registerSchemaToolButtonAction = function (btn, target, connectedSelectElement, getPayloadFunction) {

			btn.on('click', function(e) {
				e.preventDefault();
				var oldHtml = btn.html();

				var transportAction = function (target, payload) {

					Structr.updateButtonWithAjaxLoaderAndText(btn, oldHtml);
					$.ajax({
						url: rootUrl + 'maintenance/' + target,
						type: 'POST',
						data: JSON.stringify(payload),
						contentType: 'application/json',
						statusCode: {
							200: function() {
								Structr.updateButtonWithSuccessIcon(btn, oldHtml);
							}
						}
					});

				};

				if (!connectedSelectElement) {

					transportAction(target, {});

				} else {

					var type = connectedSelectElement.val();
					if (!type) {

						blinkRed(connectedSelectElement);

					} else {

						transportAction(target, ((typeof getPayloadFunction === "function") ? getPayloadFunction(type) : {}));
					}
				}
			});
		};

		registerSchemaToolButtonAction($('#rebuild-index'), 'rebuildIndex');
		registerSchemaToolButtonAction($('#flush-caches'), 'flushCaches');

		$('#clear-schema').on('click', function(e) {
			Structr.confirmation('<h3>Delete schema?</h3><p>This will remove all dynamic schema information, but not your other data.</p><p>&nbsp;</p>', function() {
				$.unblockUI({
					fadeOut: 25
				});

				_Schema.ignoreNextSchemaRecompileNotification = true;

				Command.snapshots("purge", undefined, undefined, function () {
					_Schema.reload();
				});
			});
		});


		var nodeTypeSelector = $('#node-type-selector');
		Command.list('SchemaNode', true, 1000, 1, 'name', 'asc', 'id,name', function(nodes) {
			nodes.forEach(function(node) {
				nodeTypeSelector.append('<option>' + node.name + '</option>');
			});
		});

		registerSchemaToolButtonAction($('#reindex-nodes'), 'rebuildIndex', nodeTypeSelector, function (type) {
			return (type === 'allNodes') ? {'mode': 'nodesOnly'} : {'mode': 'nodesOnly', 'type': type};
		});

		registerSchemaToolButtonAction($('#add-node-uuids'), 'setUuid', nodeTypeSelector, function (type) {
			return (type === 'allNodes') ? {'allNodes': true} : {'type': type};
		});

		registerSchemaToolButtonAction($('#create-labels'), 'createLabels', nodeTypeSelector, function (type) {
			return (type === 'allNodes') ? {} : {'type': type};
		});


		var relTypeSelector = $('#rel-type-selector');
		Command.list('SchemaRelationshipNode', true, 1000, 1, 'relationshipType', 'asc', 'id,relationshipType', function(rels) {
			rels.forEach(function(rel) {
				relTypeSelector.append('<option>' + rel.relationshipType + '</option>');
			});
		});

		registerSchemaToolButtonAction($('#reindex-rels'), 'rebuildIndex', relTypeSelector, function (type) {
			return (type === 'allRels') ? {'mode': 'relsOnly'} : {'mode': 'relsOnly', 'type': type};
		});

		registerSchemaToolButtonAction($('#add-rel-uuids'), 'setUuid', relTypeSelector, function (type) {
			return (type === 'allRels') ? {'allRels': true} : {'relType': type};
		});

	},
	appendLayoutToolsToContainer: function(container) {

		container.append('<h3>General Functions</h3>');
		container.append('<p>Reset the currently stored positions so the automatic layouting algorithm can take effect.</p>');
		container.append('<p><button class="btn" id="reset-schema-positions"><i class="' + _Icons.getFullSpriteClass(_Icons.refresh_icon) + '" /> Reset Node Positions</button></p>');
		$('#reset-schema-positions', container).on('click', _Schema.clearPositions);

		container.append('<h3>Save Layout Configuration</h3>');
		container.append('<p>Save the current layout configuration to backend (available to every backend user). This includes positions, visibility, zoom level, connector style and visibility of relationship labels.</p>');
		container.append('<p><input id="save-layout-filename" placeholder="Enter name for layout"><button id="save-layout-file"><i class="' + _Icons.getFullSpriteClass(_Icons.floppy_icon) + '" /> Save Layout</button></p>');

		container.append('<h3>Available Layout Configurations</h3>');
		container.append('<select id="saved-layout-selector"></select><button id="restore-layout"><i class="' + _Icons.getFullSpriteClass(_Icons.wand_icon) + '" /> Restore</button><button id="download-layout"><i class="' + _Icons.getFullSpriteClass(_Icons.pull_file_icon) + '" /> Download</button><button id="delete-layout"><i class="' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" /> Delete</button>');

		var layoutSelector = $('#saved-layout-selector');

		$('#save-layout-file', container).click(function() {
			var fileName = $('#save-layout-filename').val().replaceAll(/[^\w_\-\. ]+/, '-');

			if (fileName && fileName.length) {

				Command.layouts('add', fileName, JSON.stringify(_Schema.getSchemaLayoutConfiguration()), function() {
					updateLayoutSelector();
					$('#save-layout-filename').val('');

					blinkGreen(layoutSelector);
				});

			} else {
				Structr.error('Schema layout name is required.');
			}
		});

		$('#restore-layout').click(function() {

			var selectedLayout = layoutSelector.val();

			if (selectedLayout && selectedLayout.length) {

				Command.layouts('get', selectedLayout, null, function(result) {

					var loadedConfig;

					try {
						loadedConfig = JSON.parse(result.schemaLayout);

						if (loadedConfig._version) {

							switch (loadedConfig._version) {
								case 2: {

									_Schema.zoomLevel = loadedConfig.zoom;
									LSWrapper.setItem(_Schema.schemaZoomLevelKey, _Schema.zoomLevel);
									_Schema.setZoom(_Schema.zoomLevel, instance, [0,0], $('#schema-graph')[0]);
									$( "#zoom-slider" ).slider('value', _Schema.zoomLevel);

									_Schema.updateOverlayVisibility(loadedConfig.showRelLabels);

									var hiddenTypes = loadedConfig.hiddenTypes;
									hiddenTypes = hiddenTypes.filter(function(typeName) {
										// Filter out types that do not exist in the schema
										return (_Schema.availableTypeNames.indexOf(typeName) !== -1);
									});
									_Schema.hiddenSchemaNodes = hiddenTypes;
									LSWrapper.setItem(_Schema.hiddenSchemaNodesKey, JSON.stringify(_Schema.hiddenSchemaNodes));

									// update the list in the visibility table
									$('#schema-options-table input.toggle-type').prop('checked', true);
									_Schema.hiddenSchemaNodes.forEach(function(hiddenType) {
										$('#schema-options-table input.toggle-type[data-structr-type="' + hiddenType + '"]').prop('checked', false);
									});

									var connectorStyle = loadedConfig.connectorStyle;
									$('#connector-style').val(connectorStyle);
									_Schema.connectorStyle = connectorStyle;
									LSWrapper.setItem(_Schema.schemaConnectorStyleKey, connectorStyle);

									var positions = loadedConfig.positions;
									LSWrapper.setItem(_Schema.schemaPositionsKey, positions);
									_Schema.applyNodePositions(positions);
								}
									break;

								default:
									Structr.error('Cannot restore layout: Unknown layout version - was this layout created with a newer version of structr than the one currently running?');
							}

						} else {

							if (loadedConfig[Object.keys(loadedConfig)[0]].position) {
								// convert old file type
								var schemaPositions = {};
								Object.keys(loadedConfig).forEach(function(type) {
									schemaPositions[type] = loadedConfig[type].position;
								});
								loadedConfig = schemaPositions;
							}

							LSWrapper.setItem(_Schema.schemaPositionsKey, loadedConfig);
							_Schema.applyNodePositions(loadedConfig);

							new MessageBuilder().info("This layout was created using an older version of Structr. To make use of newer features you should delete and re-create it with the current version.").show();
						}

						Structr.saveLocalStorage();

						_Schema.reload();

					} catch (e) {
						Structr.error('Unreadable JSON - please make sure you are using JSON exported from this dialog!', true);
					}

				});

			} else {
				Structr.error('Please select a schema to load.');
			}
		});

		$('#download-layout').click(function() {

			var selectedLayout = layoutSelector.val();

			if (selectedLayout && selectedLayout.length) {

				Command.layouts('get', selectedLayout, null, function(result) {

					var element = document.createElement('a');
					element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(result.schemaLayout));
					element.setAttribute('download', selectedLayout + '.json');

					element.style.display = 'none';
					document.body.appendChild(element);

					element.click();
					document.body.removeChild(element);

				});

			} else {
				Structr.error('Please select a schema to download.');
			}
		});

		$('#delete-layout', container).click(function() {

			var selectedLayout = layoutSelector.val();

			if (selectedLayout && selectedLayout.length) {

				Command.layouts('delete', selectedLayout, null, function() {
					updateLayoutSelector();
					blinkGreen(layoutSelector);
				});

			} else {
				Structr.error('Please select a schema to delete.');
			}
		});

		var updateLayoutSelector = function() {

			Command.layouts('list', '', null, function(result) {

				layoutSelector.empty();
				layoutSelector.append('<option selected value="" disabled>-- Select Layout --</option>');

				result.forEach(function(data) {

					data.layouts.forEach(function(layoutFile) {
						layoutSelector.append('<option>' + layoutFile.slice(0, -5) + '</option>');
					});
				});
			});
		};
		updateLayoutSelector();

	},
	applyNodePositions:function(positions) {
		$('#schema-graph .node').each(function(i, n) {
			var node = $(n);
			var type = node.text();

			if (positions[type]) {
				node.css('top', positions[type].top);
				node.css('left', positions[type].left);
			}
		});
	},
	getSchemaLayoutConfiguration: function() {
		return {
			_version: 2,
			positions: _Schema.nodePositions,
			hiddenTypes: _Schema.hiddenSchemaNodes,
			zoom: _Schema.zoomLevel,
			connectorStyle: _Schema.connectorStyle,
			showRelLabels: $('#schema-show-overlays').prop('checked')
		};
	},
	openSchemaToolsDialog: function() {
		Structr.dialog('Schema Tools', function() {}, function() {});

		var id = "schema-tools";
		dialogHead.append('<div id="' + id + '_head"><div id="tabs" style="margin-top:20px;"><ul id="schema-tools-tabs"></ul></div></div>');
		dialogText.append('<div id="' + id + '_content"></div>');

		var mainTabs = $('#tabs', dialogHead);
		var contentDiv = $('#' + id + '_content', dialogText);

		var ul = mainTabs.children('ul');
		ul.append('<li data-name="admin">Admin</li>');
		ul.append('<li data-name="layout">Layouts</li>');
		ul.append('<li data-name="visibility">Visibility</li>');
		ul.append('<li data-name="snapshots">Snapshots</li>');

		if (Structr.isModulePresent("cloud")) {
			ul.append('<li id="tab" data-name="schema-sync">Schema Sync</li>');
		}

		var activateTab = function(tabName) {
			$('.tools-tab-content', contentDiv).hide();
			$('li', ul).removeClass('active');
			$('#tabView-' + tabName, contentDiv).show();
			$('li[data-name="' + tabName + '"]', ul).addClass('active');
			LSWrapper.setItem(_Schema.activeSchemaToolsSelectedTabLevel1Key, tabName);
		};

		$('#schema-tools-tabs > li', mainTabs).on('click', function(e) {
			e.stopPropagation();
			activateTab($(this).data('name'));
		});

		contentDiv.append('<div class="tab tools-tab-content" id="tabView-admin"></div>');
		_Schema.appendAdminToolsToContainer($('#tabView-admin', contentDiv));

		contentDiv.append('<div class="tab tools-tab-content" id="tabView-layout"></div>');
		_Schema.appendLayoutToolsToContainer($('#tabView-layout', contentDiv));

		contentDiv.append('<div class="tab tools-tab-content" id="tabView-visibility"></div>');
		_Schema.appendTypeVisibilityOptionsToContainer($('#tabView-visibility', contentDiv));

		contentDiv.append('<div class="tab tools-tab-content" id="tabView-snapshots"></div>');
		_Schema.appendSnapshotsDialogToContainer($('#tabView-snapshots', contentDiv));

		if (Structr.isModulePresent("cloud")) {
			contentDiv.append('<div class="tab tools-tab-content" id="tabView-schema-sync"></div>');
			_Schema.appendSyncOptionsToContainer($('#tabView-schema-sync', contentDiv));
		}

		var activeTab = LSWrapper.getItem(_Schema.activeSchemaToolsSelectedTabLevel1Key) || 'admin';
		activateTab(activeTab);
	},
	appendSyncOptionsToContainer: function(container) {

		var id = "schema-tools-sync";
		container.append('<div id="' + id + '_head"><div class="data-tabs level-two"><ul id="schema-tools-sync-tabs"></ul></div></div>');

		var ul = $('#schema-tools-sync-tabs', container);
		ul.append('<li id="tab" data-name="to-remote">Local -> Remote</li>');
		ul.append('<li id="tab" data-name="from-remote">Remote -> Local</li>');

		var activateTab = function(tabName) {
			$('.sync-tab-content', container).hide();
			$('li', ul).removeClass('active');
			$('#tabView-' + tabName, container).show();
			$('li[data-name="' + tabName + '"]', ul).addClass('active');
			LSWrapper.setItem(_Schema.activeSchemaToolsSelectedTabLevel2Key, tabName);
		};

		$('#schema-tools-sync-tabs > li', container).on('click', function(e) {
			e.stopPropagation();
			activateTab($(this).data('name'));
		});

		container.append('<div id="' + id + '_content"></div>');
		var contentEl = $('#' + id + '_content', container);
		contentEl.append('<div class="tab sync-tab-content" id="tabView-to-remote"></div>');
		_Schema.appendSyncToRemoteOptionsToContainer($('#tabView-to-remote', contentEl));

		contentEl.append('<div class="tab sync-tab-content" id="tabView-from-remote"></div>');
		Structr.pullDialog('SchemaNode,SchemaRelationshipNode', $('#tabView-from-remote', contentEl));

		var activeTab = LSWrapper.getItem(_Schema.activeSchemaToolsSelectedTabLevel2Key) || 'to-remote';
		activateTab(activeTab);

	},
	appendSyncToRemoteOptionsToContainer: function(container) {

		var pushConf = JSON.parse(LSWrapper.getItem(pushConfigKey)) || {};

		container.append('To sync <b>all schema nodes and relationships</b> to the remote server, ');
		container.append('enter host, port, username and password of your remote instance and click Start.');

		container.append('<table class="props push">'
				+ '<tr><td>Host</td><td><input id="push-host" type="text" length="20" value="' + (pushConf.host || '') + '"></td></tr>'
				+ '<tr><td>Port</td><td><input id="push-port" type="text" length="20" value="' + (pushConf.port || '') + '"></td></tr>'
				+ '<tr><td>Username</td><td><input id="push-username" type="text" length="20" value="' + (pushConf.username || '') + '"></td></tr>'
				+ '<tr><td>Password</td><td><input id="push-password" type="password" length="20" value="' + (pushConf.password || '') + '"></td></tr>'
				+ '</table>'
				+ '<button id="start-push">Start</button>');

		$('#start-push', container).on('click', function() {
			var host = $('#push-host', container).val();
			var port = parseInt($('#push-port', container).val());
			var username = $('#push-username', container).val();
			var password = $('#push-password', container).val();
			var key = 'key_push_schema';

			pushConf = {host: host, port: port, username: username, password: password};
			LSWrapper.setItem(pushConfigKey, JSON.stringify(pushConf));

			Command.pushSchema(host, port, username, password, key, function() {
				new MessageBuilder().success("Schema pushed successfully").show();
			});
		});

	},
	appendTypeVisibilityOptionsToContainer: function(container) {

		container.append('<table class="props" id="schema-options-table"><tr><th>Type</th><th>Visible <input type="checkbox" id="toggle-all-types"><i id="invert-all-types" class="invert-icon ' + _Icons.getFullSpriteClass(_Icons.toggle_icon) + '" /></button></th></table>');
		var schemaOptionsTable = $('#schema-options-table');

		Command.list('SchemaNode', false, 1000, 1, 'name', 'asc', null, function(schemaNodes) {
			schemaNodes.forEach(function(schemaNode) {
				schemaOptionsTable.append('<tr><td>' + schemaNode.name + '</td><td><input class="toggle-type" data-structr-type="' + schemaNode.name + '" type="checkbox" ' + (_Schema.hiddenSchemaNodes.indexOf(schemaNode.name) > -1 ? '' : 'checked') + '></td></tr>');
			});

			$('#toggle-all-types', schemaOptionsTable).on('click', function() {
				$('.toggle-type', schemaOptionsTable).each(function(i, checkbox) {
					var inp = $(checkbox);
					inp.prop("checked", $('#toggle-all-types', schemaOptionsTable).prop("checked"));
					_Schema.checkIsHiddenSchemaNode(inp);
				});
				_Schema.reload();
			});

			$('#invert-all-types', schemaOptionsTable).on('click', function() {
				$('.toggle-type', schemaOptionsTable).each(function(i, checkbox) {
					var inp = $(checkbox);
					inp.prop("checked", !inp.prop("checked"));
					_Schema.checkIsHiddenSchemaNode(inp);
				});
				_Schema.reload();
			});

			// Suppress click action on checkbox, action is triggered by event bound to underlying td element
			$('td .toggle-type', schemaOptionsTable).on('click', function(e) {
				e.stopPropagation();
				e.preventDefault();
				return false;
			});

			$('td', schemaOptionsTable).on('mouseup', function(e) {
				e.stopPropagation();
				e.preventDefault();
				var td = $(this);
				var inp = $('.toggle-type', td);
				inp.prop("checked", !inp.prop("checked"));
				_Schema.checkIsHiddenSchemaNode(inp);
				_Schema.reload();
				return false;
			});
		});

	},
	checkIsHiddenSchemaNode: function(inp) {
		var typeName = inp.attr('data-structr-type');
		var position = _Schema.hiddenSchemaNodes.indexOf(typeName);
		if (!inp.is(':checked')) {
			if (position === -1) {
				_Schema.hiddenSchemaNodes.push(typeName);
				LSWrapper.setItem(_Schema.hiddenSchemaNodesKey, JSON.stringify(_Schema.hiddenSchemaNodes));
			}
		} else {
			if (position > -1) {
				_Schema.hiddenSchemaNodes.splice(position, 1);
				LSWrapper.setItem(_Schema.hiddenSchemaNodesKey, JSON.stringify(_Schema.hiddenSchemaNodes));
			}
		}
	},
	getPropertyName: function(type, relationshipType, relatedType, out, callback) {
		$.ajax({
			url: rootUrl + '_schema/' + type,
			type: 'GET',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function(data) {
					var properties = data.result[0].views.all;
					Object.keys(properties).forEach(function(key) {
						var obj = properties[key];
						var simpleClassName = obj.className.split('.')[obj.className.split('.').length - 1];
						if (obj.relatedType && obj.relationshipType) {
							if (obj.relatedType.endsWith(relatedType) && obj.relationshipType === relationshipType && ((simpleClassName.startsWith('EndNode') && out)
									|| (simpleClassName.startsWith('StartNode') && !out))) {
								callback(key, obj.isCollection);
							}

						}
					});
				}
			}
		});

	},
	doLayout: function() {

		var nodesToLayout = new Array();
		var relsToLayout = new Array();

		$.each(Object.keys(nodes), function(i, id) {

			if (!id.endsWith('_top') && !id.endsWith('_bottom')) {

				var node = $('#id_' + id);
				nodesToLayout.push(node);
			}
		});

		$.each(Object.keys(rels), function(i, id) {
			relsToLayout.push(rels[id]);
		});

		_Layout.doLayout(nodesToLayout, relsToLayout);
	},
	setZoom: function(zoom, instance, transformOrigin, el) {
		transformOrigin = transformOrigin || [ 0.5, 0.5 ];
		instance = instance || jsPlumb;
		el = el || instance.getContainer();
		var p = [ "webkit", "moz", "ms", "o" ],
			s = _Schema.getSchemaCSSTransform(),
			oString = (transformOrigin[0] * 100) + "% " + (transformOrigin[1] * 100) + "%";

		for (var i = 0; i < p.length; i++) {
			el.style[p[i] + "Transform"] = s;
			el.style[p[i] + "TransformOrigin"] = oString;
		}

		el.style["transform"] = s;
		el.style["transformOrigin"] = oString;

		instance.setZoom(zoom);
		_Schema.resize();
	},
	getSchemaCSSTransform: function() {
	 return _Schema.getSchemaCSSScale() + ' ' + _Schema.getSchemaCSSTranslate();
	},
	getSchemaCSSScale: function() {
		return 'scale(' + _Schema.zoomLevel + ')';
	},
	getSchemaCSSTranslate: function() {
		return 'translate(' + ((inheritanceSlideout.position().left + inheritanceSlideout.outerWidth()) / _Schema.zoomLevel) + 'px)';
	},
	sort: function(collection, sortKey, secondarySortKey) {
		if (!sortKey) {
			sortKey = "name";
		}
		collection.sort(function(a, b) {

			var equal = ((a[sortKey] > b[sortKey]) ? 1 : ((a[sortKey] < b[sortKey]) ? -1 : 0));
			if (equal === 0 && secondarySortKey) {

				equal = ((a[secondarySortKey] > b[secondarySortKey]) ? 1 : ((a[secondarySortKey] < b[secondarySortKey]) ? -1 : 0));
			}

			return equal;
		});
	},
	updateOverlayVisibility: function(show) {
		LSWrapper.setItem(_Schema.showSchemaOverlaysKey, show);
		$('#schema-show-overlays').prop('checked', show);
		if (show) {
			$('.rel-type, .multiplicity').show();
		} else {
			$('.rel-type, .multiplicity').hide();
		}
	},
	loadClassTree: function(schemaNodes) {
		var classTree = {};
		var tmpHierarchy = {};
		var classnameToId = {};

		var insertClassInClassTree = function(classObj, tree) {
			var classes = Object.keys(tree);

			var position = classes.indexOf(classObj.parent);
			if (position !== -1) {

				if (classTree[classObj.name]) {
					tree[classes[position]][classObj.name] = classTree[classObj.name];
					delete(classTree[classObj.name]);
				} else {
					tree[classes[position]][classObj.name] = {};
				}

				return true;

			} else {
				var done = false;
				classes.forEach(function(className) {
					if (!done) {
						done = insertClassInClassTree(classObj, tree[className]);
					}
				});
				return done;
			}

		};

		var actionsAvailableForClass = function(className) {
			return (["AbstractNode", "ContentContainer", "ContentItem"].indexOf(className) === -1);
		};

		var printClassTree = function($elem, classTree) {
			var classes = Object.keys(classTree).sort();

			if (classes.length > 0) {

				var $newUl = $('<ul></ul>').appendTo($elem);

				classes.forEach(function(classname) {

					var icons = (actionsAvailableForClass(classname) ? '<b class="delete_icon icon delete ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" /><b class="edit_icon icon edit ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" />' : '');
					var classId = (actionsAvailableForClass(classname) ? ' data-id="' + classnameToId[classname] + '"' : '');

					var $newLi = $('<li data-jstree=\'{"opened":true}\'' + classId + '>' + classname + icons + '</li>').appendTo($newUl);
					printClassTree($newLi, classTree[classname]);

				});

			}

		};

		schemaNodes.forEach(function(schemaNode) {

			if (!schemaNode.isBuiltinType) {

				var classObj = {
					name: schemaNode.name,
					parent: (schemaNode.extendsClass === null ? 'AbstractNode' : schemaNode.extendsClass.slice(schemaNode.extendsClass.lastIndexOf('.')+1))
				};

				classnameToId[classObj.name] = schemaNode.id;

				var inserted = insertClassInClassTree(classObj, tmpHierarchy);

				if (!inserted) {
					var insertedTmp = insertClassInClassTree(classObj, classTree);

					if (!insertedTmp) {
						if (classTree[classObj.name]) {
							classTree[classObj.parent] = {};
							classTree[classObj.parent][classObj.name] = classTree[classObj.name];
							delete(classTree[classObj.name]);
						} else {
							classTree[classObj.parent] = {};
							classTree[classObj.parent][classObj.name] = {};
						}

					}
				}
			}
		});

		$.jstree.destroy();
		printClassTree(inheritanceTree, classTree);
		inheritanceTree.jstree({
			core: {
				multiple: false,
				themes: {
					dots: false
				}
			},
			plugins: ["search"]
		}).on('changed.jstree', function(e, data) {
			var $node = $('#id_' + data.node.data.id);
			if ($node.length > 0) {
				$('.selected').removeClass('selected');
				$node.addClass('selected');
				selectedElements = [$node];
			}
		});

		$('#search-classes').keyup(function(e) {
			if (e.which === 27) {
				$('#search-classes').val('');
				inheritanceTree.jstree(true).clear_search();
			} else {
				var query = $('#search-classes').val();
				inheritanceTree.jstree(true).search(query, true, true);
			}

			_Schema.enableEditFunctionsInClassTree();
		});

		_Schema.enableEditFunctionsInClassTree();
	},
	enableEditFunctionsInClassTree: function() {
		$('.edit_icon', inheritanceTree).off('click').on('click', function(e) {
			var nodeId = $(this).closest('li').data('id');
			if (nodeId) {
				_Schema.openEditDialog(nodeId);
			}
		});

		$('.delete_icon', inheritanceTree).off('click').on('click', function(e) {
			var nodeId = $(this).closest('li').data('id');
			if (nodeId) {
				Structr.confirmation(
					'<h3>Delete schema node \'' + $(this).closest('a').text() + '\'?</h3><p>This will delete all incoming and outgoing schema relationships as well,<br> but no data will be removed.</p>',
					function() {
						$.unblockUI({ fadeOut: 25 });
						_Schema.deleteNode(nodeId);
					}
				);
			}
		});
	},
	getMethodsInitFunction: function() {
		return Structr.guardExecution(function() {

			$('#tabView-methods textarea.property-code').each(function(i, el) {
				_Schema.initCodeMirrorForMethodCode(el);
			});

			$('#tabView-methods textarea.property-comment').each(function(i, el) {
				_Schema.initCodeMirrorForMethodComment(el);
			});

			_Schema.restoreSchemaMethodsRowHeights();
		});
	},
	senseCodeMirrorMode: function(contentText) {
		return (contentText.substring(0, 1) === "{") ? 'javascript' : 'none';
	},
	initCodeMirrorForMethodCode: function(el) {
		var cm = CodeMirror.fromTextArea(el, {
			lineNumbers: true,
			mode: _Schema.senseCodeMirrorMode($(el).val()),
			lineWrapping: LSWrapper.getItem(lineWrappingKey),
			extraKeys: {
				"'.'":        _Contents.autoComplete,
				"Ctrl-Space": _Contents.autoComplete
			},
			indentUnit: 4,
			tabSize: 4,
			indentWithTabs: true
		});
		$(cm.getWrapperElement()).addClass('cm-schema-methods');
		cm.refresh();

		cm.on('change', function(cm, changeset) {
			cm.save();
			cm.setOption('mode', _Schema.senseCodeMirrorMode(cm.getValue()));
			$(cm.getTextArea()).trigger('change');
		});
	},
	initCodeMirrorForMethodComment: function(el) {
		var cm = CodeMirror.fromTextArea(el, {
			theme: "no-lang",
			lineNumbers: true,
			lineWrapping: LSWrapper.getItem(lineWrappingKey),
			indentUnit: 4,
			tabSize: 4,
			indentWithTabs: true
		});
		$(cm.getWrapperElement()).addClass('cm-schema-methods');
		cm.refresh();

		cm.on('change', function(cm, changeset) {
			cm.save();
			$(cm.getTextArea()).trigger('change');
		});
	},
	makeSchemaMethodRowResizable: function(tr) {
		var initialRowHeight;
		var dragBeginPageY;
		var row;

		$('.resize-handle', tr).draggable({
			axis: 'y',
			start: function(event, ui) {
				dragBeginPageY = event.pageY;
				row = $(ui.helper).closest('tr').prev();
				initialRowHeight = row.height();
			},
			drag: function(event, ui) {
				var newHeight = initialRowHeight + (event.pageY - dragBeginPageY);
				_Schema.setSchemaMethodRowHeight(row, newHeight);
			},
			stop: function(event, ui) {
				var typeName   = row.data('typeName');
				var methodName = row.data('methodName');

				if (typeName && methodName) {
					var finalHeight = initialRowHeight + (event.pageY - dragBeginPageY);

					var schemaMethodsHeights = LSWrapper.getItem(_Schema.schemaMethodsHeightsKey);
					if (!schemaMethodsHeights) {
						schemaMethodsHeights = {};
					}
					if (!schemaMethodsHeights[typeName]) {
						schemaMethodsHeights[typeName] = {};
					}
					schemaMethodsHeights[typeName][methodName] = finalHeight;
					LSWrapper.setItem(_Schema.schemaMethodsHeightsKey, schemaMethodsHeights);
				}

				$(this).attr('style', null);
			}
		});

	},
	setSchemaMethodRowHeight: function($tr, height) {

		if (typeof height === 'number') {
			var tds = $tr.find('td');
			var cms = tds.find('.CodeMirror');

			tds.height( height );
			cms.height( height );

			cms.each(function(idx, cm) {
				cm.CodeMirror.refresh();
			});
		} else {
			console.warn('Stored height is not a number - not using value: ', height);
		}

	},
	restoreSchemaMethodsRowHeights: function() {

		var schemaMethodsHeights = LSWrapper.getItem(_Schema.schemaMethodsHeightsKey);
		if (schemaMethodsHeights) {

			$('#tabView-methods tbody tr').each(function(i, el) {
				var typeName   = $(el).data('typeName');
				var methodName = $(el).data('methodName');

				if (schemaMethodsHeights && schemaMethodsHeights[typeName] && schemaMethodsHeights[typeName][methodName]) {
					_Schema.setSchemaMethodRowHeight($(el), schemaMethodsHeights[typeName][methodName]);
				}
			});

		}

	},
	overlapsExistingNodes: function(position) {
		if (!position) {
			return false;
		}
		var overlaps = false;
		$('.node.schema.compact').each(function(i, node) {
			var offset = $(node).offset();
			overlaps |= (Math.abs(position.left - offset.left) < 20 && Math.abs(position.top - offset.top) < 20);
		});
		return overlaps;
	}
};