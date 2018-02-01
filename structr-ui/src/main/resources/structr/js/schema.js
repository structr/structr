/*
 * Copyright (C) 2010-2018 Structr GmbH
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
		+ '<option value="DoubleArray">Double[]</option>'
		+ '<option value="Boolean">Boolean</option>'
		+ '<option value="BooleanArray">Boolean[]</option>'
		+ '<option value="Enum">Enum</option>'
		+ '<option value="Date">Date</option>'
		+ '<option value="Count">Count</option>'
		+ '<option value="Function" data-indexed="false">Function</option>'
		+ '<option value="Notion">Notion</option>'
		+ '<option value="Join">Join</option>'
		+ '<option value="Cypher" data-indexed="false">Cypher</option>'
		+ '<option value="Thumbnail">Thumbnail</option>',
	currentNodeDialogId:null,
	reload: function(callback) {

		_Schema.clearTypeInfoCache();

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
			schemaInputContainer.append('<button class="btn" id="global-schema-methods"><i class="' + _Icons.getFullSpriteClass(_Icons.book_icon) + '" /> Global schema methods</button>');

			$('#schema-show-overlays').on('change', function() {
				_Schema.updateOverlayVisibility($(this).prop('checked'));
			});
			$('#schema-tools').on('click', _Schema.openSchemaToolsDialog);
			$('#global-schema-methods').on('click', _Schema.showGlobalSchemaMethods);

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

					instance.bind('connection', function(info, originalEvent) {
						if (!originalEvent) {
							_Logger.log(_LogType.SCHEMA, "Ignoring connection event in jsPlumb as it looks like it has been created programmatically");
						} else {
							_Schema.connect(Structr.getIdFromPrefixIdString(info.sourceId, 'id_'), Structr.getIdFromPrefixIdString(info.targetId, 'id_'));
						}
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

			Structr.adaptUiToAvailableFeatures();

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
			var $el = $(el);
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
		Structr.updateMainHelpLink('https://support.structr.com/article/193');

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

		_Schema.clearTypeInfoCache();

		if (Structr.isModuleActive(_Schema)) {

			new MessageBuilder()
					.title("Schema recompiled")
					.info("Another user made changes to the schema. Do you want to reload to see the changes?")
					.specialInteractionButton("Reload", _Schema.reloadSchemaAfterRecompileNotification, "Ignore")
					.uniqueClass('schema')
					.incrementsUniqueCount()
					.show();

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
					var outs    = entity.relatedTo ? entity.relatedTo.length : 0;
					var ins     = entity.relatedFrom ? entity.relatedFrom.length : 0;
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
							var calculatedX = getX();
							if (calculatedX > 1500) {
								y++;
								x = 0;
								calculatedX = getX();
							}
							var calculatedY = getY();
							return { left: calculatedX, top: calculatedY };
						};

						var storedPosition = _Schema.nodePositions[entity.name];
						if (!storedPosition) {

							var calculatedPosition = calculatePosition();
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

		Command.get(id, null, function(entity) {

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
					if (res.targetId !== res.sourceId && existingRels[res.targetId + '-' + res.sourceId]) {
						relCnt[relIndex] += existingRels[res.targetId + '-' + res.sourceId];
					}

					var stub   = 30 + 80 * relCnt[relIndex];
					var offset =     0.2 * relCnt[relIndex];

					rels[res.id] = instance.connect({
						source: nodes[res.sourceId + '_bottom'],
						target: nodes[res.targetId + '_top'],
						deleteEndpointsOnDetach: false,
						scope: res.id,
						connector: [_Schema.connectorStyle, {curviness: 200, cornerRadius: 25, stub: [stub, 30], gap: 6, alwaysRespectStubs: true }],
						paintStyle: { lineWidth: 5, strokeStyle: res.permissionPropagation !== 'None' ? "#ffad25" : "#81ce25" },
						overlays: [
							["Label", {
									cssClass: "label multiplicity",
									label: res.sourceMultiplicity ? res.sourceMultiplicity : '*',
									location: Math.min(.2 + offset, .4),
									id: "sourceMultiplicity"
								}
							],
							["Label", {
									cssClass: "label rel-type",
									label: '<div id="rel_' + res.id + '">' + (res.relationshipType === initialRelType ? '<span>&nbsp;</span>' : res.relationshipType)
											+ ' <i title="Edit schema relationship" class="edit icon ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '"></i>'
											+ ' <i title="Remove schema relationship" class="remove icon ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '"></i></div>',
									location: .5,
									id: "label"
								}
							],
							["Label", {
									cssClass: "label multiplicity",
									label: res.targetMultiplicity ? res.targetMultiplicity : '*',
									location: Math.max(.8 - offset, .6),
									id: "targetMultiplicity"
								}
							]
						]
					});

					if (res.relationshipType === initialRelType) {
						var relTypeOverlay = $('#rel_' + res.id);
						relTypeOverlay.css({
							width: "80px"
						});
						relTypeOverlay.parent().addClass('schema-reltype-warning');


						Structr.appendInfoTextToElement({
							text: "It is highly advisable to set a relationship type on the relationship! To do this, click the pencil icon to open the edit mode.<br><br><strong>Note: </strong>Any existing relationships of this type have to be migrated manually.",
							element: $('span', relTypeOverlay),
							customToggleIcon: _Icons.error_icon,
							appendToElement: $('body')
						});
					}

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

				Command.search(typeName, 'SchemaNode', true, function(results) {
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
			_Schema.appendMethods(c, entity, entity.schemaMethods);
		}, _Schema.getMethodsInitFunction(contentDiv));

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

		Structr.fetchHtmlTemplate('schema/dialog.relationship', {id: id}, function (html) {
			headEl.append(html);

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
				_Schema.appendMethods(c, entity, entity.schemaMethods);
			}, _Schema.getMethodsInitFunction(contentDiv));

			var selectRelationshipOptions = function(rel) {
				$('#source-type-name').text(nodes[rel.sourceId].name).data('objectId', rel.sourceId);
				$('#source-multiplicity-selector').val(rel.sourceMultiplicity || '*');
				$('#relationship-type-name').val(rel.relationshipType === initialRelType ? '' : rel.relationshipType);
				$('#target-multiplicity-selector').val(rel.targetMultiplicity || '*');
				$('#target-type-name').text(nodes[rel.targetId].name).data('objectId', rel.targetId);

				$('#cascading-delete-selector').val(rel.cascadingDeleteFlag || 0);
				$('#autocreate-selector').val(rel.autocreationFlag || 0);
				$('#propagation-selector').val(rel.permissionPropagation || 'None');
				$('#read-selector').val(rel.readPropagation || 'Remove');
				$('#write-selector').val(rel.writePropagation || 'Remove');
				$('#delete-selector').val(rel.deletePropagation || 'Remove');
				$('#access-control-selector').val(rel.accessControlPropagation || 'Remove');
				$('#masked-properties').val(rel.propertyMask);
			};

			$('.edit-schema-object', headEl).on('click', function(e) {
				e.stopPropagation();
				_Schema.openEditDialog($(this).data('objectId'));
				return false;
			});

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

				$('#relationship-options select, #relationship-options textarea, #relationship-options input').attr('disabled', true);

				editButton.show();
				saveButton.hide();
				cancelButton.hide();

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
					}, function(data) {
						var additionalInformation = {};
						var causedByIdenticalRelName = data.responseJSON.errors.some(function (e) {
							return (e.detail.indexOf('duplicate class') !== -1);
						});
						if (causedByIdenticalRelName) {
							additionalInformation.requiresConfirmation = true;
							additionalInformation.title = 'Error';
							additionalInformation.overrideText = 'You are trying to create a second relationship named <strong>' + newData.relationshipType + '</strong> between these types.<br>Relationship names between types have to be unique.';
						}

						Structr.errorFromResponse(data.responseJSON, null, additionalInformation);

						editButton.click();

						Object.keys(newData).forEach(function(attribute) {
							blinkRed($('#relationship-options [data-attr-name=' + attribute + ']'));
						});

					});
				}
			});

			cancelButton.on('click', function(e) {

				selectRelationshipOptions(entity);
				$('#relationship-options select, #relationship-options textarea, #relationship-options input').attr('disabled', true);

				editButton.show();
				saveButton.hide();
				cancelButton.hide();
			});

			Structr.resize();

		});

	},
	appendLocalProperties: function(el, entity) {

		el.append('<table class="local schema-props"><thead><th>JSON Name</th><th>DB Name</th><th>Type</th><th>Format/Code</th><th>Notnull</th><th>Comp.</th><th>Uniq.</th><th>Idx</th><th>Default</th><th class="actions-col">Action</th></thead></table>');
		el.append('<i title="Add local attribute" class="add-icon add-local-attribute ' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" />');

		var propertiesTable = $('.local.schema-props', el);

		_Schema.sort(entity.schemaProperties);

		$.each(entity.schemaProperties, function(i, prop) {
			_Schema.appendLocalProperty(propertiesTable, prop);
		});
		$('.add-local-attribute', el).on('click', function() {

			var rowClass = 'new' + (_Schema.new_attr_cnt++);

			Structr.fetchHtmlTemplate('schema/property.new', {rowClass: rowClass}, function(html) {

				var tr = $(html);
				propertiesTable.append(tr);

				$('.property-type', tr).on('change', function() {
					var selectedOption = $('option:selected', this);
					var shouldIndex = selectedOption.data('indexed');
					if (shouldIndex === undefined) {
						shouldIndex = true;
					}
					var indexedCb = $('.' + rowClass + ' .indexed');
					if (indexedCb.prop('checked') !== shouldIndex) {
						indexedCb.prop('checked', shouldIndex);

						blink(indexedCb.closest('td'), '#fff', '#bde5f8');
						Structr.showAndHideInfoBoxMessage('Automatically updated indexed flag to default behavior for property type (you can still override this)', 'info', 2000, 200);
					}
				});

				$('.remove-property', tr).on('click', function() {
					var self = $(this);
					self.closest('tr').remove();
				});

				$('.create-property', tr).on('click', function() {
					var self = $(this);
					if (!self.data('save-pending')) {
						_Schema.collectAndSaveNewLocalProperty(self, rowClass, tr, entity);
					}
				});
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

			Structr.fetchHtmlTemplate('schema/view', {}, function(html) {
				var tr = $(html);
				viewsTable.append(tr);

				_Schema.activateEditModeForViewRow(tr);
				_Schema.appendViewSelectionElement(tr, {name: 'new'}, entity);

				$('.save-action', tr).on('click', function() {
					_Schema.createOrSaveView(tr, entity);
				});

				$('.cancel-action', tr).on('click', function() {
					tr.remove();
				});
			});
		});
	},
	appendView: function(el, view, entity) {

		Structr.fetchHtmlTemplate('schema/view', {view: view}, function(html) {
			var tr = $(html);
			el.append(tr);

			_Schema.appendViewSelectionElement(tr, view, entity);
			_Schema.initViewRow(tr, entity, view);
		});

	},
	initViewRow: function(tr, entity, view) {

		var activate = function() {
			_Schema.activateEditModeForViewRow(tr);
		};

		_Schema.deactivateEditModeForViewRow(tr);

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

			_Schema.deactivateEditModeForViewRow(tr);
		});

		$('.remove-action', tr).off('click').on('click', function() {
			_Schema.confirmRemoveSchemaEntity(view, $(this).attr('title'), function() { _Schema.openEditDialog(entity.id, 'views'); } );
		});

		_Schema.updateViewPreviewLink(tr, entity.name, view.name);
	},
	updateViewPreviewLink:function(tr, typeName, viewName) {
		$('.preview-action', tr).attr('href', '/structr/rest/' + typeName + '/' + viewName + '?pageSize=1');
	},
	activateEditModeForViewRow: function(tr) {
		$('.hidden-in-edit-mode', tr).addClass('hidden');
		$('.visible-in-edit-mode', tr).removeClass('hidden');
	},
	deactivateEditModeForViewRow: function(tr) {
		$('.hidden-in-edit-mode', tr).removeClass('hidden');
		$('.visible-in-edit-mode', tr).addClass('hidden');
	},
	appendViewSelectionElement: function(tr, view, schemaEntity) {

		var propertySelectTd = $('.view-properties-select', tr).last();
		propertySelectTd.append('<select class="property-attrs view chosen-sortable" multiple="multiple"></select>');
		var viewSelectElem = $('.property-attrs', propertySelectTd);

		Command.listSchemaProperties(schemaEntity.id, view.name, function(properties) {

			var appendProperty = function(prop) {
				var name       = prop.name;
				var isSelected = prop.isSelected ? ' selected="selected"' : '';
				var isDisabled = (view.name === 'ui' || prop.isDisabled) ? ' disabled="disabled"' : '';

				viewSelectElem.append('<option value="' + name + '"' + isSelected + isDisabled + '>' + name + '</option>');
			};

			if (view.sortOrder) {
				view.sortOrder.split(',').forEach(function(sortedProp) {

					var prop = properties.filter(function(prop) {
						return (prop.name === sortedProp);
					});

					if (prop.length) {
						appendProperty(prop[0]);

						properties = properties.filter(function(prop) {
							return (prop.name !== sortedProp);
						});
					}
				});
			}

			properties.forEach(function (prop) {
				appendProperty(prop);
			});

			viewSelectElem.chosen({
				search_contains: true,
				width: '100%',
				display_selected_options: false,
				hide_results_on_select: false,
				display_disabled_options: false
			}).chosenSortable(function() {
				_Schema.activateEditModeForViewRow(tr);
			});
		});
	},
	createOrSaveView: function(tr, entity, view) {

		var name        = $('.view.property-name', tr).val();
		var sortedAttrs = $('.view.property-attrs', tr).sortedVals();

		if (name && name.length) {

			var obj                = {};
			obj.schemaNode         = { id: entity.id };
			obj.schemaProperties   = _Schema.findSchemaPropertiesByNodeAndName(entity, sortedAttrs);
			obj.nonGraphProperties = _Schema.findNonGraphProperties(entity, sortedAttrs);
			obj.name               = name;
			obj.sortOrder          = sortedAttrs.join(',');

			_Schema.storeSchemaEntity('schema_views', (view || {}), JSON.stringify(obj), function(result) {

				if (view) {

					// we saved a view
					blinkGreen(tr);
					_Schema.updateViewPreviewLink(tr, entity.name, name);

					var oldName = view.name;
					view.schemaProperties = obj.schemaProperties;
					view.name             = obj.name;

					tr.removeClass(oldName).addClass(view.name);

					_Schema.deactivateEditModeForViewRow(tr);

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
	appendMethods: function(el, entity, methods) {

		el.append('<table class="actions schema-props"><thead><th>JSON Name</th><th>Code</th><th>Comment</th><th class="actions-col">Action</th></thead></table>');
		var actionsTable = $('.actions.schema-props', el);

		el.append('<button class="add-icon add-action-button"><i class="' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" /> Add method</button>');
		$('.add-action-button', el).on('click', function() {
			_Schema.appendEmptyMethod(actionsTable, entity);
		});

		if (entity) {

			el.append('<button class="add-icon add-onCreate-button"><i class="' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" /> Add onCreate</button>');
			$('.add-onCreate-button', el).on('click', function() {
				_Schema.appendEmptyMethod(actionsTable, entity, _Schema.getFirstFreeMethodName('onCreate'));
			});

			el.append('<button class="add-icon add-onSave-button"><i class="' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" /> Add onSave</button>');
			$('.add-onSave-button', el).on('click', function() {
				_Schema.appendEmptyMethod(actionsTable, entity, _Schema.getFirstFreeMethodName('onSave'));
			});
		}

		_Schema.sort(methods);

		Structr.fetchHtmlTemplate('schema/method.empty', {}, function(html, cacheHit) {

			methods.forEach(function(method) {
				_Schema.appendMethod(html, actionsTable, method, entity);
			});

			if (!cacheHit) {
				var initFunction = _Schema.getMethodsInitFunction(actionsTable);
				initFunction();
			}

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
		});
	},
	appendMethod: function(templateHTML, el, method, entity) {

		el.append(templateHTML);

		// row containing resize handler
		var resizeHandlerRow = $('tr', el).last();

		// row containing method
		var tr = resizeHandlerRow.prev('tr');

		tr.addClass(method.name).data('type-name', (entity ? entity.name : 'global_schema_method')).data('method-name', method.name);
		$('.property-name', tr).val(method.name);
		$('.property-code', tr).text(method.source);
		$('.property-comment', tr).text(method.comment || '');

		_Schema.makeSchemaMethodRowResizable(resizeHandlerRow);
		_Schema.initMethodRow(tr, entity, method);

	},
	appendEmptyMethod: function(actionsTable, entity, optionalName) {

		Structr.fetchHtmlTemplate('schema/method.empty', {}, function(html) {

			actionsTable.append(html);

			// row containing resize handler
			var resizeHandlerRow = $('tr', actionsTable).last();

			// row containing method
			var tr = resizeHandlerRow.prev('tr');

			$('.property-name', tr).val(optionalName);

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


		});

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
			name:    $('.action.property-name', tr).val(),
			source:  $('.action.property-code', tr).val(),
			comment: $('.action.property-comment', tr).val()
		};

		if (entity) {
			obj.schemaNode = { id: entity.id };
		}

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
	showGlobalSchemaMethods: function () {

		Command.rest('SchemaMethod?schemaNode=null&sort=name&order=ascending', function (methods) {

			Structr.dialog('Global Schema Methods', function() {
				dialogMeta.show();
			}, function() {
				_Schema.currentNodeDialogId = null;

				dialogMeta.show();
				instance.repaintEverything();
			});

			dialogMeta.hide();

			var contentEl = dialogText;

			var contentDiv = $('<div id="___global_methods_content" class="schema-details"></div>');
			contentEl.append(contentDiv);

			_Schema.appendMethods(contentDiv, null, methods);
			var initFunction = _Schema.getMethodsInitFunction(contentDiv);
			initFunction();
		});
	},
	appendRemoteProperties: function(el, entity) {

		el.append('<table class="related-attrs schema-props"><thead><th>JSON Name</th><th>Type, Direction and Remote type</th><th class="actions-col">Action</th></thead></table>');

		entity.relatedTo.forEach(function(target) {
			_Schema.appendRelatedProperty($('.related-attrs', el), target, true);
		});

		entity.relatedFrom.forEach(function(source) {
			_Schema.appendRelatedProperty($('.related-attrs', el), source, false);
		});
	},
	appendBuiltinProperties: function(el, entity) {

		el.append('<table class="builtin schema-props"><thead><th>Declaring Class</th><th>JSON Name</th><th>Type</th><th>Notnull</th><th>Comp.</th><th>Uniq.</th><th>Idx</th></thead></table>');

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
						compound: prop.compound,
						unique: prop.unique,
						indexed: prop.indexed,
						declaringClass: prop.declaringClass
					};

					_Schema.appendBuiltinProperty(propertiesTable, property);
				}
			});
		});
	},
	collectAndSaveNewLocalProperty: function(button, rowClass, tr, entity) {

		var name = $('.property-name', tr).val();
		var dbName = $('.property-dbname', tr).val();
		var type = $('.property-type', tr).val();
		var format = $('.property-format', tr).val();
		var notNull = $('.not-null', tr).is(':checked');
		var compound = $('.compound', tr).is(':checked');
		var unique = $('.unique', tr).is(':checked');
		var indexed = $('.indexed', tr).is(':checked');
		var defaultValue = $('.property-default', tr).val();

		if (name.length === 0) {
			blinkRed($('.property-name', tr).closest('td'));

		} else if (type.length === 0) {
			blinkRed($('.property-type', tr).closest('td'));

		} else {

			var obj = {
				schemaNode: { id: entity.id }
			};
			if (name)         { obj.name = name; }
			if (dbName)       { obj.dbName = dbName; }
			if (type)         { obj.propertyType = type; }
			if (format)       { obj.format = format; }
			if (notNull)      { obj.notNull = notNull; }
			if (compound)     { obj.compound = compound; }
			if (unique)       { obj.unique = unique; }
			if (indexed)      { obj.indexed = indexed; }
			if (defaultValue) { obj.defaultValue = defaultValue; }

			if (!_Schema.validatePropertyDefinition(obj)) {
				blinkRed($('.property-type', tr).closest('td'));
				return;
			}

			button.data('save-pending', true);

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

								blinkGreen(tr);

								_Schema.reload();

								_Schema.unbindEvents(name);

								tr.removeClass(rowClass).addClass('local').addClass(name);

								$('.create-property', tr).remove();
								$('.remove-property', tr)
										.removeClass(_Icons.getSpriteClassOnly(_Icons.cross_icon)).addClass(_Icons.getSpriteClassOnly(_Icons.delete_icon))
										.off('click').on('click', function() {
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

				blinkRed(tr);

				button.data('save-pending', false);
			});
		}
	},
	validatePropertyDefinition: function (propertyDefinition) {

		if (propertyDefinition.propertyType === 'Enum') {

			var containsSpace = propertyDefinition.format.split(',').some(function (enumVal) {
				return enumVal.trim().indexOf(' ') !== -1;
			});

			if (containsSpace) {
				new MessageBuilder().warning('Enum values must be separated by commas and cannot contain spaces<br>See the <a href="https://support.structr.com/article/329" target="_blank">support article on enum properties</a> for more information.').requiresConfirmation().show();
				return false;
			}
		}

		return true;
	},
	confirmRemoveSchemaEntity: function(entity, title, callback, hint) {

		Structr.confirmation('<h3>' + title + ' ' + entity.name + '?</h3>' + (hint ? '<p>' + hint + '</p>' : ''),
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
				var $elem = $(elem);
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

		Structr.fetchHtmlTemplate('schema/property.local', {property: property}, function(html) {
			el.append(html);

			_Schema.bindEvents(property);

			if (property.isBuiltinProperty) {
				_Schema.disable(property);
			}
		});

	},
	appendBuiltinProperty: function(el, property) {

		Structr.fetchHtmlTemplate('schema/property.builtin', {property: property}, function(html) {
			el.append(html);
		});
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
		$('.' + key + ' .compound', el).prop('disabled', true);
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
			$('.' + key + ' .content-type', el).on('change', function() {
				_Schema.savePropertyDefinition(property);
			}).prop('disabled', null).val(property.contentType);
		}

		if (property.propertyType === 'Function' && !property.isBuiltinProperty) {
			if (!$('button.edit-read-function', typeField.parent()).length) {
				$('.' + key + ' .property-format', el).replaceWith('<button class="edit-read-function">Read</button><button class="edit-write-function">Write</button>');
			}
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

		$('.' + key + ' .property-format', el).on('change', function() {
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

		$('.' + key + ' .compound', el).on('change', function() {
			_Schema.savePropertyDefinition(property);
		}).prop('disabled', null).val(property.compound);

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
		$('.' + key + ' .property-format', el).off('change').prop('disabled', 'disabled');
		$('.' + key + ' .not-null', el).off('change').prop('disabled', 'disabled');
		$('.' + key + ' .compound', el).off('change').prop('disabled', 'disabled');
		$('.' + key + ' .unique', el).off('change').prop('disabled', 'disabled');
		$('.' + key + ' .indexed', el).off('change').prop('disabled', 'disabled');
		$('.' + key + ' .property-default', el).off('change').prop('disabled', 'disabled');
		$('.' + key + ' .remove-property', el).off('click').prop('disabled', 'disabled');
		$('.' + key + ' button', el).off('click').prop('disabled', 'disabled');
	},
	openCodeEditor: function(btn, id, key, callback) {

		dialogMeta.show();

		Command.get(id, 'id,name,contentType,' + key, function(entity) {

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

			Command.setProperty(entity.id, key, text2, false, function() {

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
	appendRelatedProperty: function(el, rel, out) {
		var relType = (rel.relationshipType === undefinedRelType) ? '' : rel.relationshipType;
		var relatedNodeId = (out ? rel.targetId : rel.sourceId);
		var attributeName = (out ? (rel.targetJsonName || rel.oldTargetJsonName) : (rel.sourceJsonName || rel.oldSourceJsonName));

		var classForCardinality = function (cardinality) {
			switch (cardinality) {
				case '*': return 'many';
				case '1': return 'one';
				default: return 'error';
			}
		};
		var cardinalityTag = function (cardinality) {
			return '<i class="cardinality ' + classForCardinality(cardinality) + '" />';
		};

		var row = $(
			'<tr>' +
				'<td><input size="15" type="text" class="property-name related" value="' + attributeName + '"></td>' +
				'<td>' +
					(out ? '' : '&lt;') + '&mdash;' + cardinalityTag(out ? rel.sourceMultiplicity : rel.targetMultiplicity) + '&mdash;[:<span class="edit-schema-object" data-object-id="' + rel.id + '">' + relType + '</span>]&mdash;' + cardinalityTag(out ? rel.targetMultiplicity : rel.sourceMultiplicity) + '&mdash;' + (out ? '&gt;' : '') +
					' <span class="edit-schema-object" data-object-id="' + relatedNodeId + '">'+ nodes[relatedNodeId].name + '</span>' +
				'</td>' +
				'<td><i title="Reset name to default" class="remove-icon reset-action ' + _Icons.getFullSpriteClass(_Icons.arrow_undo_icon) + '" /></td>' +
			'</tr>');
		el.append(row);

		$('.edit-schema-object', row).on('click', function(e) {
			e.stopPropagation();
			_Schema.openEditDialog($(this).data('objectId'));
			return false;
		});

		var resetNameToDefault = function () {

			var updateAttributeName = function (blink) {
				Command.get(rel.id, (out ? 'oldTargetJsonName' : 'oldSourceJsonName'), function (data) {
					$('.property-name', row).val(data[(out ? 'oldTargetJsonName' : 'oldSourceJsonName')]);
					if (blink) {
						blinkGreen(row);
					}
				});
			};

			_Schema.setRelationshipProperty(rel, (out ? 'targetJsonName' : 'sourceJsonName'), null, function() {
				updateAttributeName(true);
			}, function(data) {
				blinkRed(row);
				Structr.errorFromResponse(data.responseJSON);
			}, function () {
				updateAttributeName(false);
			});
		};

		$('.reset-action', row).on('click', function () {
			resetNameToDefault();
		});

		$('.property-name', row).on('change', function() {

			var newName = $(this).val().trim();

			if (newName === '') {
				resetNameToDefault();
			} else {

				_Schema.setRelationshipProperty(rel, (out ? 'targetJsonName' : 'sourceJsonName'), newName, function() {
					blinkGreen(row);
				}, function(data) {
					blinkRed(row);
					Structr.errorFromResponse(data.responseJSON);
				});
			}
		});
	},
	savePropertyDefinition: function(property) {

		var obj = {
			name:         $('.' + property.name + ' .property-name').val(),
			dbName:       $('.' + property.name + ' .property-dbname').val(),
			propertyType: $('.' + property.name + ' .property-type').val(),
			contentType:  $('.' + property.name + ' .content-type').val(),
			format:       $('.' + property.name + ' .property-format').val(),
			notNull:      $('.' + property.name + ' .not-null').is(':checked'),
			compound:     $('.' + property.name + ' .compound').is(':checked'),
			unique:       $('.' + property.name + ' .unique').is(':checked'),
			indexed:      $('.' + property.name + ' .indexed').is(':checked'),
			defaultValue: $('.' + property.name + ' .property-default').val()
		};

		if (obj.name && obj.name.length && obj.propertyType) {

			if (!_Schema.validatePropertyDefinition(obj)) {
				blinkRed($('.local .' + property.name));
				return;
			}

			_Schema.unbindEvents(property.name);

			_Schema.storeSchemaEntity('schema_properties', property, JSON.stringify(obj), function() {

				if (property.name !== obj.name) {
					$('.local .' + property.name).removeClass(property.name).addClass(obj.name);
				}

				blinkGreen($('.local .' + property.name));

				// accept values into property object
				property.name = obj.name;
				property.dbName = obj.dbName;
				property.propertyType = obj.propertyType;
				property.contentType = obj.contentType;
				property.format = obj.format;
				property.notNull = obj.notNull;
				property.compound = obj.compound;
				property.unique = obj.unique;
				property.indexed = obj.indexed;
				property.defaultValue = obj.defaultValue;

				_Schema.bindEvents(property);

			}, function(data) {

				var additionalInformation = {};

				if (obj.propertyType === 'Enum') {
					additionalInformation.requiresConfirmation = true;
					additionalInformation.title = 'Schema compilation failed';
					additionalInformation.overrideText = 'Error while making changes to an Enum property. See the <a href="https://support.structr.com/article/329">support article on enum properties</a> for possible explanations.';
				}

				Structr.errorFromResponse(data.responseJSON, null, additionalInformation);

				blinkRed($('.local .' + property.name));
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
						if (onError) {
							onError(data);
						}
					}
				}
			});
		}
	},
	storeSchemaEntity: function(resource, entity, data, onSuccess, onError, onNoop) {

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
						if (onError) {
							onError(data);
						}
					}
				}
			});
		}
	},
	createNode: function(type) {
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
					Structr.errorFromResponse(data.responseJSON, undefined, {requiresConfirmation: true});
				}
			}
		});
	},
	deleteNode: function(id) {
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
					Structr.errorFromResponse(data.responseJSON);
				}
			}
		});
	},
	createRelationshipDefinition: function(sourceId, targetId, relationshipType) {
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

					var additionalInformation = {};
					var causedByUndefinedRelName = data.responseJSON.errors.some(function (e) {
						return (e.type.indexOf(undefinedRelType) !== -1);
					});
					if (causedByUndefinedRelName) {
						additionalInformation.requiresConfirmation = true;
						additionalInformation.title = 'Duplicate unnamed relationship';
						additionalInformation.overrideText = 'You are trying to create a second <strong>unnamed</strong> relationship between these types.<br>Please rename the existing unnamed relationship first before creating another relationship.';
					}

					Structr.errorFromResponse(data.responseJSON, null, additionalInformation);

					_Schema.reload();
				}
			}
		});
	},
	removeRelationshipDefinition: function(id) {
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
					Structr.errorFromResponse(data.responseJSON);
				}
			}
		});
	},
	setRelationshipProperty: function(entity, key, value, onSuccess, onError, onNoChange) {
		var data = {};
		data[key] = cleanText(value);
		_Schema.editRelationship(entity, data, onSuccess, onError, onNoChange);
	},
	editRelationship: function(entity, newData, onSuccess, onError, onNoChange) {
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

						$.ajax({
							url: rootUrl + 'schema_relationship_nodes/' + entity.id,
							type: 'PUT',
							dataType: 'json',
							contentType: 'application/json; charset=utf-8',
							data: JSON.stringify(newData),
							statusCode: {
								200: function() {
									if (onSuccess) {
										onSuccess();
									}
									_Schema.reload();
								},
								422: function(data) {
									if (onError) {
										onError(data);
									}
								}
							}
						});

					} else {
						// force a schema-reload so that we dont break the relationships
						if (onNoChange) {
							onNoChange();
						}
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
			Command.get(id, 'id', function(entity) {
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
				Command.get(id, 'id', function(entity) {
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

		Structr.fetchHtmlTemplate('schema/snapshots', {}, function (html) {

			container.append(html);

			var table = $('#snapshots');

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
						var $el = $(el);

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

			$('#refresh-snapshots').on('click', refresh);
			refresh();

		});
	},
	performSnapshotAction: function (action, snapshot) {

		Command.snapshots(action, snapshot, null, function(data) {

			var status = data[0].status;

			if (status === 'success') {
				_Schema.reload();
			} else {
				if (dialogBox.is(':visible')) {
					Structr.showAndHideInfoBoxMessage(status, 'error', 2000, 200);
				}
			}
		});

	},
	appendAdminToolsToContainer: function(container) {

		Structr.fetchHtmlTemplate('schema/admin-tools', {}, function(html) {

			container.append(html);

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
		});
	},
	appendLayoutToolsToContainer: function(container) {

		Structr.fetchHtmlTemplate('schema/layout-tools', {}, function(html) {

			container.append(html);

			$('#reset-schema-positions', container).on('click', _Schema.clearPositions);
			var layoutSelector = $('#saved-layout-selector', container);

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
		});
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

		if (Structr.isModulePresent('cloud')) {
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

		if (Structr.isModulePresent('cloud')) {
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

		container.append('<table class="props" id="schema-options-table"><tr><th class="toggle-column-header"><input type="checkbox" id="toggle-all-types"><i id="invert-all-types" class="invert-icon ' + _Icons.getFullSpriteClass(_Icons.toggle_icon) + '" /> Visible</th><th>Type</th></tr></table>');
		var schemaOptionsTable = $('#schema-options-table');

		Command.list('SchemaNode', false, 1000, 1, 'name', 'asc', null, function(schemaNodes) {
			schemaNodes.forEach(function(schemaNode) {
				schemaOptionsTable.append('<tr><td><input class="toggle-type" data-structr-type="' + schemaNode.name + '" type="checkbox" ' + (_Schema.hiddenSchemaNodes.indexOf(schemaNode.name) > -1 ? '' : 'checked') + '></td><td>' + schemaNode.name + '</td></tr>');
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

		var printClassTree = function($elem, classTree) {
			var classes = Object.keys(classTree).sort();

			if (classes.length > 0) {

				var $newUl = $('<ul></ul>').appendTo($elem);

				classes.forEach(function(classname) {
					var actionsAvailableForClass = !!(classnameToId[classname]);

					var icons = (actionsAvailableForClass ? '<b class="delete_icon icon delete ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" /><b class="edit_icon icon edit ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" />' : '');
					var classId = (actionsAvailableForClass ? ' data-id="' + classnameToId[classname] + '"' : '');

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
	getMethodsInitFunction: function(container) {
		return (function() {
			$('textarea.property-code', container).each(function(i, el) {
				_Schema.initCodeMirrorForMethodCode(el);
			});

			$(' textarea.property-comment', container).each(function(i, el) {
				_Schema.initCodeMirrorForMethodComment(el);
			});
			_Schema.restoreSchemaMethodsRowHeights(container);
		});
	},
	senseCodeMirrorMode: function(contentText) {
		return (contentText.substring(0, 1) === "{") ? 'javascript' : 'none';
	},
	initCodeMirrorForMethodCode: function(el) {
		var existingCodeMirror = $('.CodeMirror', $(el).parent())[0];
		if (!existingCodeMirror) {
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
		}
	},
	initCodeMirrorForMethodComment: function(el) {
		var existingCodeMirror = $('.CodeMirror', $(el).parent())[0];
		if (!existingCodeMirror) {
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
		}
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
	restoreSchemaMethodsRowHeights: function(container) {

		var schemaMethodsHeights = LSWrapper.getItem(_Schema.schemaMethodsHeightsKey);
		if (schemaMethodsHeights) {

			$('tbody tr', container).each(function(i, el) {
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
	},

	typeInfoCache: {},
	clearTypeInfoCache: function () {
		_Logger.log(_LogType.SCHEMA, 'Clear Schema Type Cache');
		_Schema.typeInfoCache = {};
	},
	getTypeInfo: function (type, callback) {

		if (_Schema.typeInfoCache[type] !== undefined) {

			_Logger.log(_LogType.SCHEMA, 'Cache Hit: ', type);
			callback(_Schema.typeInfoCache[type]);

		} else {

			_Logger.log(_LogType.SCHEMA, 'Cache MISS: ', type);

			Command.getSchemaInfo(type, function(schemaInfo) {

				var typeInfo = {};
				$(schemaInfo).each(function(i, prop) {
					typeInfo[prop.jsonName] = prop;
				});

				_Schema.typeInfoCache[type] = typeInfo;

				callback(typeInfo);
			});
		}
	}
};