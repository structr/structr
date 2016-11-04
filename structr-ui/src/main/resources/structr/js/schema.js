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
var canvas, instance, res, nodes = {}, rels = {}, localStorageSuffix = '_schema_' + port, undefinedRelType = 'UNDEFINED_RELATIONSHIP_TYPE', initialRelType = undefinedRelType;
var radius = 20, stub = 30, offset = 0, maxZ = 0, reload = false;
var connectorStyle, zoomLevel;
var remotePropertyKeys = [];
var hiddenSchemaNodes = [];
var hiddenSchemaNodesKey = 'structrHiddenSchemaNodes_' + port;
var selectedRel, relHighlightColor = 'red';
var selectionInProgress = false;
var mouseDownCoords = {x:0, y:0};
var mouseUpCoords = {x:0, y:0};
var selectBox, nodeDragStartpoint;
var selectedNodes = [];
var showSchemaOverlaysKey = 'structrShowSchemaOverlays_' + port;
var schemaContainer;
var inheritanceTree, inheritanceSlideout, inheritanceSlideoutOpen = false;

$(document).ready(function() {

	Structr.registerModule('schema', _Schema);
	Structr.classes.push('schema');
});

var _Schema = {
	schemaLoading: false,
	schemaLoaded: false,
	new_attr_cnt: 0,
	typeOptions: '<select class="property-type"><option value="">--Select--</option>'
		+ '<option value="String">String</option>'
		+ '<option value="StringArray">String[]</option>'
		+ '<option value="Integer">Integer</option>'
		+ '<option value="Long">Long</option>'
		+ '<option value="Double">Double</option>'
		+ '<option value="Boolean">Boolean</option>'
		+ '<option value="Enum">Enum</option>'
		+ '<option value="Date">Date</option>'
		+ '<option value="Count">Count</option>'
		+ '<option value="Function">Function</option>'
		+ '<option value="Notion">Notion</option>'
		+ '<option value="Join">Join</option>'
		+ '<option value="Cypher">Cypher</option>'
		+ '<option value="Thumbnail">Thumbnail</option>',
	reload: function() {
		if (reload) {
			return;
		}
		var x = window.scrollX;
		var y = window.scrollY;
		reload = true;
		_Schema.storePositions();
		schemaContainer.empty();
		_Schema.init({x: x, y: y});
		_Schema.resize();

	},
	storePositions: function() {
		$.each($('#schema-graph .node'), function(i, n) {
			var node = $(n);
			var type = node.text();
			var obj = {position: node.position()};
			obj.position.left = (obj.position.left - canvas.offset().left) / zoomLevel;
			obj.position.top  = (obj.position.top  - canvas.offset().top)  / zoomLevel;
			LSWrapper.setItem(type + localStorageSuffix + 'node-position', JSON.stringify(obj));
		});
	},
	getPosition: function(type) {
		var n = JSON.parse(LSWrapper.getItem(type + localStorageSuffix + 'node-position'));
		return n ? n.position : undefined;
	},
	init: function(scrollPosition) {

		_Schema.loadClassTree();

		_Schema.schemaLoading = false;
		_Schema.schemaLoaded = false;
		_Schema.schema = [];
		_Schema.keys = [];

		schemaContainer.append('<div class="schema-input-container"></div>');

		var schemaInputContainer = $('.schema-input-container');

		Structr.ensureIsAdmin(schemaInputContainer, function() {

			connectorStyle = LSWrapper.getItem(localStorageSuffix + 'connectorStyle') || 'Flowchart';
			zoomLevel = parseFloat(LSWrapper.getItem(localStorageSuffix + 'zoomLevel')) || 1.0;

			schemaInputContainer.append('<div class="input-and-button"><input class="schema-input" id="type-name" type="text" size="10" placeholder="New type"><button id="create-type" class="btn"><img src="' + _Icons.add_icon + '"> Add</button></div>');
			schemaInputContainer.append('<div class="input-and-button"><input class="schema-input" id="ggist-url" type="text" size="20" placeholder="Enter GraphGist URL"><button id="gg-import" class="btn">Start Import</button></div>');
			$('#gg-import').on('click', function(e) {
				var btn = $(this);
				var text = btn.text();
				btn.attr('disabled', 'disabled').addClass('disabled').html(text + ' <img src="' + _Icons.ajax_loader_2 + '">');
				e.preventDefault();
				_Schema.importGraphGist($('#ggist-url').val(), text);
			});

			var styles = ['Flowchart', 'Bezier', 'StateMachine', 'Straight'];

			schemaInputContainer.append('<select id="connector-style"></select>');
			$.each(styles, function(i, style) {
				$('#connector-style').append('<option value="' + style + '" ' + (style === connectorStyle ? 'selected="selected"' : '') + '>' + style + '</option>');
			});
			$('#connector-style').on('change', function() {
				var newStyle = $(this).val();
				connectorStyle = newStyle;
				LSWrapper.setItem(localStorageSuffix + 'connectorStyle', newStyle);
				_Schema.reload();
			});

			schemaInputContainer.append('<div id="zoom-slider" style="display:inline-block; width:100px; margin-left:10px"></div>');
			$( "#zoom-slider" ).slider({
				min:0.25,
				max:1,
				step:0.05,
				value:zoomLevel,
				slide: function( event, ui ) {
					var newZoomLevel = ui.value;
					zoomLevel = newZoomLevel;
					LSWrapper.setItem(localStorageSuffix + 'zoomLevel', newZoomLevel);
					_Schema.setZoom(newZoomLevel, instance, [0,0], $('#schema-graph')[0]);
					if (selectedNodes.length > 0) {
						_Schema.updateSelectedNodes();
					}
					_Schema.resize();
				}
			});

			schemaInputContainer.append('<button class="btn" id="admin-tools"><img src="' + _Icons.edit_ui_properties_icon + '"> Tools</button>');
			$('#admin-tools').on('click', function() {
				_Schema.openAdminTools();
			});

			schemaInputContainer.append('<button class="btn module-dependend" data-structr-module="cloud" id="sync-schema"><img src="' + _Icons.push_file_icon + '"> Sync schema</button>');
			$('#sync-schema').on('click', function() {
				_Schema.syncSchemaDialog();
			});

			schemaInputContainer.append('<button class="btn" id="show-snapshots"><img src="' + _Icons.database_icon + '"> Snapshots</button>');
			$('#show-snapshots').on('click', function() {
				_Schema.snapshotsDialog();
			});

			schemaInputContainer.append('<button class="btn" id="schema-display-options"><img src="' + _Icons.edit_icon + '"> Display Options</button>');
			$('#schema-display-options').on('click', function() {
				_Schema.openSchemaDisplayOptions();
			});

			schemaInputContainer.append('<input type="checkbox" id="schema-show-overlays" name="schema-show-overlays" style="margin-left:10px"><label for="schema-show-overlays"> Show relationship labels</label>');
			$('#schema-show-overlays').on('change', function() {
				_Schema.updateOverlayVisibility($(this).prop('checked'));
			});

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
						_Schema.connect(Structr.getIdFromPrefixIdString(info.sourceId, 'id_'), Structr.getIdFromPrefixIdString(info.targetId, 'id_'));
					});
					instance.bind('connectionDetached', function(info) {
						Structr.confirmation('<h3>Delete schema relationship?</h3>',
								function() {
									$.unblockUI({
										fadeOut: 25
									});
									_Schema.detach(info.connection.scope);
									_Schema.reload();
								});
						_Schema.reload();
					});
					reload = false;

					_Schema.setZoom(zoomLevel, instance, [0,0], $('#schema-graph')[0]);

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

					canvas.on('mousemove', function (e) {
						if (e.which === 1) {
							_Schema.selectionDrag(e);
						}
					});

					canvas.on('mouseup', function (e) {
						_Schema.selectionStop();
					});

					_Schema.resize();

					Structr.unblockMenu(500);

					var showSchemaOverlays = LSWrapper.getItem(showSchemaOverlaysKey) === null ? true : LSWrapper.getItem(showSchemaOverlaysKey);
					_Schema.updateOverlayVisibility(showSchemaOverlays);

					if (scrollPosition) {
						window.scrollTo(scrollPosition.x, scrollPosition.y);
					}
				});

			});

			_Schema.resize();

			Structr.adaptUiToPresentModules();

		});

	},
	selectRel: function (rel) {
		_Schema.clearSelection();

		selectedRel = rel;
		selectedRel.css({zIndex: ++maxZ});
		selectedRel.nextAll('._jsPlumb_overlay').slice(0, 3).css({zIndex: ++maxZ, borderColor: relHighlightColor, background: 'rgba(255, 255, 255, 1)'});
		var pathElements = selectedRel.find('path');
		pathElements.css({stroke: relHighlightColor});
		$(pathElements[1]).css({fill: relHighlightColor});
	},
	clearSelection: function() {
		// deselect selected node
		$('.node', canvas).removeClass('selected');
		_Schema.selectionStop();

		// deselect selected Relationship
		if (selectedRel) {
			selectedRel.nextAll('._jsPlumb_overlay').slice(0, 3).css({borderColor:'', background: 'rgba(255, 255, 255, .8)'});
			var pathElements = selectedRel.find('path');
			pathElements.css({stroke: '', fill: ''});
			$(pathElements[1]).css('fill', '');
			selectedRel = undefined;
		}
	},
	selectionStart: function (e) {
		canvas.addClass('noselect');
		selectionInProgress = true;
		var schemaOffset = canvas.offset();
		mouseDownCoords.x = e.pageX - schemaOffset.left;
		mouseDownCoords.y = e.pageY - schemaOffset.top;
	},
	selectionDrag: function (e) {
		if (selectionInProgress === true) {
			var schemaOffset = canvas.offset();
			mouseUpCoords.x = e.pageX - schemaOffset.left;
			mouseUpCoords.y = e.pageY - schemaOffset.top;
			_Schema.drawSelectElem();
		}
	},
	selectionStop: function () {
		selectionInProgress = false;
		if (selectBox) {
			selectBox.remove();
			selectBox = undefined;
		}
		_Schema.updateSelectedNodes();
		canvas.removeClass('noselect');
	},
	updateSelectedNodes: function() {
		selectedNodes = [];
		var canvasOffset = canvas.offset();
		$('.node.selected', canvas).each(function (idx, el) {
			$el = $(el);
			var elementOffset = $el.offset();
			selectedNodes.push({
				nodeId: $el.attr('id'),
				name: $el.children('b').text(),
				pos: {
					top:  (elementOffset.top  - canvasOffset.top ),
					left: (elementOffset.left - canvasOffset.left)
				}
			});
		});
	},
	drawSelectElem: function () {
		if (!selectBox || !selectBox.length) {
			canvas.append('<svg id="schema-graph-select-box"><path version="1.1" xmlns="http://www.w3.org/1999/xhtml" fill="none" stroke="#aaa" stroke-width="5"></path></svg>');
			selectBox = $('#schema-graph-select-box');
		}
		var cssRect = {
			position: 'absolute',
			top:    Math.min(mouseDownCoords.y, mouseUpCoords.y)  / zoomLevel,
			left:   Math.min(mouseDownCoords.x, mouseUpCoords.x)  / zoomLevel,
			width:  Math.abs(mouseDownCoords.x - mouseUpCoords.x) / zoomLevel,
			height: Math.abs(mouseDownCoords.y - mouseUpCoords.y) / zoomLevel
		};
		selectBox.css(cssRect);
		selectBox.find('path').attr('d', 'm 0 0 h ' + cssRect.width + ' v ' + cssRect.height + ' h ' + (-cssRect.width) + ' v ' + (-cssRect.height) + ' z');
		_Schema.selectNodesInRect(cssRect);
	},
	selectNodesInRect: function (selectionRect) {
		var selectedElements = [];

		$('.node', canvas).each(function (idx, el) {
			var $el = $(el);
			if (_Schema.isElemInSelection($el, selectionRect)) {
				selectedElements.push($el);
				$el.addClass('selected');
			} else {
				$el.removeClass('selected');
			}
		});
	},
	isElemInSelection: function ($el, selectionRect) {
		var elPos = $el.offset();
		elPos.top /= zoomLevel;
		elPos.left /= zoomLevel;
		var schemaOffset = canvas.offset();
		return !(
			(elPos.top) > (selectionRect.top + schemaOffset.top / zoomLevel + selectionRect.height) ||
			elPos.left > (selectionRect.left + schemaOffset.left / zoomLevel + selectionRect.width) ||
			(elPos.top + $el.innerHeight()) < (selectionRect.top + schemaOffset.top / zoomLevel) ||
			(elPos.left + $el.innerWidth()) < (selectionRect.left + schemaOffset.left / zoomLevel)
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

		var updateCanvasTranslation = function () {
			var windowHeight = $(window).height();
			$('.ver-scrollable').css({
				height: (windowHeight - inheritanceTree.offset().top - 25) + 'px'
			});
			canvas.css('transform', _Schema.getSchemaCSSTransform());
			_Schema.resize();
		};

		inheritanceSlideoutOpen = false;
		$('#inheritanceTab').on('click', function() {
			if ($(this).hasClass('noclick')) {
				$(this).removeClass('noclick');
				return;
			}

			if (Math.abs(inheritanceSlideout.position().left + inheritanceSlideout.width() + 12) <= 3) {
				inheritanceSlideoutOpen = true;
				Structr.openLeftSlideOut(inheritanceSlideout, $("#inheritanceTab"), activeTabLeftKey, updateCanvasTranslation, updateCanvasTranslation);

			} else {
				inheritanceSlideoutOpen = false;
				Structr.closeLeftSlideOuts([inheritanceSlideout], activeTabLeftKey, _Schema.resize);
				canvas.css('transform', _Schema.getSchemaCSSTransform());

			}
		});

		_Schema.init();
		$('#main-help a').attr('href', 'http://docs.structr.org/frontend-user-guide#Schema');

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
		var url = rootUrl + 'schema_nodes/ui';
		hiddenSchemaNodes = JSON.parse(LSWrapper.getItem(hiddenSchemaNodesKey)) || [];
		$.ajax({
			url: url,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			success: function(data) {

				$.each(data.result, function(i, entity) {

					if (hiddenSchemaNodes.length > 0 && hiddenSchemaNodes.indexOf(entity.id) > -1) { return; }

					var isBuiltinType = entity.isBuiltinType;
					var id = 'id_' + entity.id;
					nodes[entity.id] = entity;
					canvas.append('<div class="schema node compact'
							+ (isBuiltinType ? ' light' : '')
							//+ '" id="' + id + '">' + (entity.icon ? '<img class="type-icon" src="' + entity.icon + '">' : '') + '<b>' + entity.name + '</b>'
							+ '" id="' + id + '"><b>' + entity.name + '</b>'
							+ '<img class="icon delete" src="' + (isBuiltinType ? _Icons.delete_disabled_icon : _Icons.delete_icon) + '">'
							+ '<img class="icon edit" src="' + _Icons.edit_icon + '">'
							+ '</div>');


					var node = $('#' + id);

					if (!isBuiltinType) {
						node.children('b').on('click', function() {
							_Schema.makeAttrEditable(node, 'name');
						});
					}

					node.on('mousedown', function() {
						node.css({zIndex: ++maxZ});
					});

					if (!isBuiltinType) {
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

					var storedPosition = _Schema.getPosition(entity.name);
					node.offset({
						left: storedPosition ? storedPosition.left : i * 100 + 25,
						top: storedPosition ? storedPosition.top : i * 40 + 131
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
						//anchor: [ "Perimeter", { shape: "Square" } ],
						anchor: "Top",
						maxConnections: -1,
						//isSource: true,
						isTarget: true,
						deleteEndpointsOnDetach: false
					});
					nodes[entity.id + '_bottom'] = instance.addEndpoint(id, {
						//anchor: [ "Perimeter", { shape: "Square" } ],
						anchor: "Bottom",
						maxConnections: -1,
						isSource: true,
						deleteEndpointsOnDetach: false
								//isTarget: true
					});

					instance.draggable(id, {
						containment: true,
						start: function (ui) {
							var nodeOffset   = $(ui.el).offset();
							var canvasOffset = canvas.offset();
							nodeDragStartpoint = {
								top:  (nodeOffset.top  - canvasOffset.top ),
								left: (nodeOffset.left - canvasOffset.left)
							};
						},
						drag: function (ui) {

							var $element = $(ui.el);

							if (!$element.hasClass('selected')) {

								_Schema.clearSelection();

							} else {

								var nodeOffset = $element.offset();
								var canvasOffset = canvas.offset();

								var posDelta = {
									top:  (nodeDragStartpoint.top  - nodeOffset.top ),
									left: (nodeDragStartpoint.left - nodeOffset.left)
								};

								selectedNodes.forEach(function (selectedNode) {
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
				});

				if (callback) {
					callback();
				}

			}
		});
	},
	openEditDialog: function(id, targetView, callback) {

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
				if (callback) {
					callback();
				}
				dialogMeta.show();
				instance.repaintEverything();
			});

			method(entity, dialogHead, dialogText, targetView);

		});

	},
	loadRels: function(callback) {
		var url = rootUrl + 'schema_relationship_nodes';
		$.ajax({
			url: url,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			success: function(data) {

				var relCnt = {};
				$.each(data.result, function(i, res) {

					var relIndex = res.sourceId + '-' + res.targetId;
					if (relCnt[relIndex] !== undefined) {
						relCnt[relIndex]++;
					} else {
						relCnt[relIndex] = 0;
					}

					radius = 20 + 10 * relCnt[relIndex];
					stub   = 30 + 80 * relCnt[relIndex];
					offset =     0.1 * relCnt[relIndex];

					if (!(nodes[res.sourceId] && nodes[res.targetId])) {
						return;
					}

					rels[res.id] = instance.connect({
						source: nodes[res.sourceId + '_bottom'],
						target: nodes[res.targetId + '_top'],
						deleteEndpointsOnDetach: false,
						scope: res.id,
						connector: [connectorStyle, {curviness: 200, cornerRadius: radius, stub: [stub, 30], gap: 6, alwaysRespectStubs: true }],
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
											+ ' <img title="Edit schema relationship" alt="Edit schema relationship" class="edit icon" src="' + _Icons.edit_icon + '">'
											+ ' <img title="Remove schema relationship" alt="Remove schema relationship" class="remove icon" src="' + _Icons.delete_icon + '"></div>',
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
						$('#rel_' + res.id + ' .icon').show();
						$('#rel_' + res.id + ' .target-multiplicity').addClass('hover');
					}).on('mouseout', function() {
						$('#rel_' + res.id + ' .icon').hide();
						$('#rel_' + res.id + ' .target-multiplicity').removeClass('hover');
					});

					$('#rel_' + res.id + ' .edit').on('click', function() {
						_Schema.openEditDialog(res.id);
					});

					$('#rel_' + res.id + ' .remove').on('click', function() {
						Structr.confirmation('<h3>Delete schema relationship \'' + res.relationshipType + '\'?</h3>', function() {
							$.unblockUI({
								fadeOut: 25
							});
							_Schema.detach(res.id);
							_Schema.reload();
						});
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

		remotePropertyKeys = [];

		if (!targetView) {
			targetView = 'local';
		}

		var id = '___' + entity.id;
		headEl.append('<div id="' + id + '_head" class="schema-details"></div>');
		var headContentDiv = $('#' + id + '_head');

		headContentDiv.append('<b>' + entity.name + '</b>');

		if (!entity.isBuiltinType) {
			headContentDiv.append(' extends <select class="extends-class-select"></select>');
			headContentDiv.append(' <img id="edit-parent-class" class="icon edit" src="' + _Icons.edit_icon + '" alt="Edit parent class" title="Edit parent class">');

			$("#edit-parent-class", headContentDiv).click(function () {

				if ($(this).hasClass('disabled')) {
					return;
				}

				var typeName = $('.extends-class-select', headContentDiv).val().split('.').pop();

				Command.search(typeName, 'SchemaNode', function (results) {
					if (results.length === 1) {

						_Schema.openEditDialog(results[0].id, null, function () {

							window.setTimeout(function () {

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

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'local', 'Local Attributes', targetView === 'local', function (c) {
			_Schema.appendLocalProperties(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'views', 'Views', targetView === 'views', function (c) {
			_Schema.appendViews(c, 'schema_nodes', entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'methods', 'Methods', targetView === 'methods', function (c) {
			_Schema.appendMethods(c, entity);
		}, _Schema.getMethodsInitFunction());

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'remote', 'Remote Attributes', targetView === 'remote', function (c) {
			_Schema.appendRemoteProperties(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'builtin', 'Inherited Attributes', targetView === 'builtin', function (c) {
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
				if ( !type || type.startsWith('_') || fqcn.startsWith('org.structr.web.entity.html') || fqcn.endsWith('.' + entity.name) ) {
					return;
				}
				classNames.push(fqcn);
			});

			classNames.sort();

			classNames.forEach( function (classname) {
				classSelect.append('<option ' + (entity.extendsClass === classname ? 'selected="selected"' : '') + ' value="' + classname + '">' + classname + '</option>');
			});

			classSelect.chosen({ search_contains: true, width: '500px' });
		});

		classSelect.on('change', function() {
			var obj = {extendsClass: $(this).val()};
			_Schema.storeSchemaEntity('schema_properties', entity, JSON.stringify(obj), function () {
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
			+ '<button id="edit-rel-options-button"><img class="edit icon" src="' + _Icons.edit_icon + '"> Edit relationship options</button>'
			+ '<button id="save-rel-options-button"><img class="save icon" src="' + _Icons.tick_icon + '"> Save changes</button>'
			+ '<button id="cancel-rel-options-button"><img src="' + _Icons.cross_icon + '"> Discard changes</button>'
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

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'local', 'Local Attributes', true, function (c) {
			_Schema.appendLocalProperties(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'views', 'Views', false, function (c) {
			_Schema.appendViews(c, 'schema_relationship_nodes', entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'methods', 'Methods', false, function (c) {
			_Schema.appendMethods(c, entity);
		}, _Schema.getMethodsInitFunction());

		var selectRelationshipOptions = function (rel) {
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

		editButton.on('click', function (e) {
			e.preventDefault();

			$('#relationship-options select, #relationship-options textarea, #relationship-options input').attr('disabled', false);
			$('#relationship-options select, #relationship-options textarea, #relationship-options input').css('color', '');
			$('#relationship-options select, #relationship-options textarea, #relationship-options input').css('background-color', '');
			editButton.hide();
			saveButton.show();
			cancelButton.show();
		});

		saveButton.on('click', function (e) {

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

			Object.keys(newData).forEach(function (key) {
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
				_Schema.editRelationship(entity, newData, function () {
					Object.keys(newData).forEach(function (attribute) {
						blinkGreen($('#relationship-options [data-attr-name=' + attribute + ']'));
						entity[attribute] = newData[attribute];
					});
				}, function () {
					Object.keys(newData).forEach(function (attribute) {
						blinkRed($('#relationship-options [data-attr-name=' + attribute + ']'));
					});
				});
			}

			$('#relationship-options select, #relationship-options textarea, #relationship-options input').attr('disabled', true);

			editButton.show();
			saveButton.hide();
			cancelButton.hide();
		});

		cancelButton.on('click', function (e) {

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
		el.append('<img alt="Add local attribute" class="add-icon add-local-attribute" src="' + _Icons.add_icon + '">');

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
					+ '<img alt="Accept" title="Save changes" class="create-icon create-property" src="' + _Icons.tick_icon + '">'
					+ '<img alt="Cancel" title="Discard changes" class="remove-icon remove-property" src="' + _Icons.cross_icon + '">'
					+ '</td></div>');

			$('.' + rowClass + ' .remove-property', propertiesTable).on('click', function() {
				var self = $(this);
				self.closest('tr').remove();
			});

			$('.' + rowClass + ' .create-property', propertiesTable).on('click', function() {
				_Schema.collectAndSaveNewLocalProperty(rowClass, propertiesTable, entity);
			});
		});

	},
	appendViews: function(el, resource, entity) {

		el.append('<table class="views schema-props"><thead><th>Name</th><th>Attributes</th><th class="actions-col">Action</th></thead></table>');
		el.append('<img alt="Add view" class="add-icon add-view" src="' + _Icons.add_icon + '">');

		var viewsTable = $('.views.schema-props', el);
		var newSelectClass   = 'select-view-attrs-new';

		_Schema.sort(entity.schemaViews);

		$.each(entity.schemaViews, function(i, view) {
			_Schema.appendView(viewsTable, view, resource, entity);
		});

		$('.add-view', el).on('click', function() {
			viewsTable.append('<tr class="new"><td style="width:20%;"><input size="15" type="text" class="view property-name" placeholder="Enter view name"></td>'
					+ '<td class="' + newSelectClass + '"></td><td>'
					+ '<img alt="Accept" title="Save changes" class="create-icon create-view" src="' + _Icons.tick_icon + '">'
					+ '<img alt="Cancel" title="Discard changes" class="remove-icon remove-view" src="' + _Icons.cross_icon + '">'
					+ '</td>'
					+ '</div');

			_Schema.appendViewSelectionElement('.' + newSelectClass, {name: 'new'}, resource, entity);

			$('.new .create-view', el).on('click', function() {
				_Schema.createView(el, entity);
			});

			$('.new .remove-view', el).on('click', function() {
				var self = $(this);
				self.closest('tr').remove();
			});
		});
	},
	appendMethods: function(el, entity) {

		el.append('<table class="actions schema-props"><thead><th>JSON Name</th><th>Code</th><th>Comment</th><th class="actions-col">Action</th></thead></table>');
		el.append('<img alt="Add action" class="add-icon add-action-attribute" src="' + _Icons.add_icon + '">');

		var actionsTable = $('.actions.schema-props', el);

		_Schema.sort(entity.schemaMethods);

		$.each(entity.schemaMethods, function(i, method) {
			_Schema.appendMethod(actionsTable, method);
		});

		$('.add-action-attribute', el).on('click', function() {

			actionsTable.append('<tr class="new">'
					+ '<td class="name-col"><div class="abs-pos-helper">'
						+ '<input size="15" type="text" class="action property-name" placeholder="Enter method name">'
						+ '<img alt="Drag to resize" title="Drag to resize" class="resize-handle" src="' + _Icons.arrow_up_down + '">'
					+ '</div></td>'
					+ '<td><textarea rows="4" class="action property-code" placeholder="Enter Code"></textarea></td>'
					+ '<td><textarea rows="4" class="action property-comment" placeholder="Enter comment"></textarea></td>'
					+ '<td>'
						+ '<img alt="Accept" title="Save changes" class="create-icon create-action" src="' + _Icons.tick_icon + '">'
						+ '<img alt="Cancel" title="Discard changes" class="remove-icon remove-action" src="' + _Icons.cross_icon + '">'
					+ '</td>'
					+ '</tr>');

			var tr = $('tr:last-of-type', el);
			_Schema.makeRowResizable(tr);

			// Intitialize editor(s)
			$('textarea.property-code', tr).each(function (i, txtarea) {
				var cm = CodeMirror.fromTextArea(txtarea, {
					lineNumbers: true,
					mode: "none",
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
				});
			});

			$('textarea.property-comment', tr).each(function (i, txtarea) {
				var cm = CodeMirror.fromTextArea(txtarea, {
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
				});
			});

			$('.new .create-action', el).on('click', function() {
				_Schema.createMethod(el, entity);
			});

			$('.new .remove-action', el).on('click', function() {
				var self = $(this);
				self.closest('tr').remove();
			});
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
	appendRemoteProperties: function(el, entity) {

		el.append('<table class="related-attrs schema-props"><thead><th>JSON Name</th><th>Type, Direction and Remote type</th></thead></table>');

		var url = rootUrl + 'schema_relationship_nodes?sourceId=' + entity.id;
		$.ajax({
			url: url,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			success: function(data) {

				$.each(data.result, function(i, res) {

					_Schema.appendRelatedProperty($('.related-attrs', el), res, res.targetJsonName ? res.targetJsonName : res.oldTargetJsonName, true);
					instance.repaintEverything();

				});

			}
		});

		url = rootUrl + 'schema_relationship_nodes?targetId=' + entity.id;
		$.ajax({
			url: url,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			success: function(data) {

				$.each(data.result, function(i, res) {

					_Schema.appendRelatedProperty($('.related-attrs', el), res, res.sourceJsonName ? res.sourceJsonName : res.oldSourceJsonName, false);
					instance.repaintEverything();

				});

			}
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

				if (!prop.isDynamic) {

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
	collectAndSaveNewLocalProperty: function(rowClass, el, entity) {

		var name = $('.' + rowClass + ' .property-name', el).val();
		var dbName = $('.' + rowClass + ' .property-dbname', el).val();
		var type = $('.' + rowClass + ' .property-type', el).val();
		var format = $('.' + rowClass + ' .property-format', el).val();
		var notNull = $('.' + rowClass + ' .not-null', el).is(':checked');
		var unique = $('.' + rowClass + ' .unique', el).is(':checked');
		var indexed = $('.' + rowClass + ' .indexed', el).is(':checked');
		var defaultValue = $('.' + rowClass + ' .property-default', el).val();

		if (name && name.length && type) {

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
								_Schema.appendViews(
									$el,
									'schema_relationship_nodes',
									entity
								);
								_Schema.bindEvents(property);
							}
						}
					});
				}

			}, function(data) {
				Structr.errorFromResponse(data.responseJSON);

				blinkRed($('.' + rowClass, el));
				//_Schema.bindEvents(entity, type, key);
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
				+ (property.isBuiltinProperty ? '' : '<img alt="Remove" class="remove-icon remove-property" src="' + _Icons.delete_icon + '">')
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
				window.setTimeout(function () {
					callback();
				}, 250);
			});

		});

	},
	editCode: function(button, entity, key, element, callback) {

		var text = entity[key] || '';

		if (isDisabled(button)) {
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

			//var contentNode = Structr.node(entity.id)[0];

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
				dialogMsg.html('<div class="infoBox success">Code saved.</div>');
				$('.infoBox', dialogMsg).delay(2000).fadeOut(200);
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
		remotePropertyKeys.push('_' + key);
		var relType = rel.relationshipType;
		relType = relType === undefinedRelType ? '' : relType;

		el.append('<tr class="' + key + '"><td><input size="15" type="text" class="property-name related" value="' + key + '"></td><td>'
				+ (out ? '-' : '&lt;-') + '[:' + relType + ']' + (out ? '-&gt;' : '-') + '</td></tr>');


		if (out) {

			Command.get(rel.targetId, function(targetSchemaNode) {
				$('.' + key + ' td:nth-child(2)', el).append(' <span class="remote-schema-node" id="target_' + targetSchemaNode.id + '">'+ targetSchemaNode.name + '</span>');

				$('#target_' + targetSchemaNode.id, el).on('click', function(e) {
					e.stopPropagation();
					_Schema.openEditDialog(targetSchemaNode.id);
					return false;
				});
			});

		} else {

			Command.get(rel.sourceId, function(sourceSchemaNode) {
				$('.' + key + ' td:nth-child(2)', el).append(' <span class="remote-schema-node" id="source_' + sourceSchemaNode.id + '">'+ sourceSchemaNode.name + '</span>');

				$('#target_' + sourceSchemaNode.id, el).on('click', function(e) {
					e.stopPropagation();
					_Schema.openEditDialog(sourceSchemaNode.id);
					return false;
				});
			});
		}

		$('.' + key + ' .property-name', el).on('blur', function() {

			var newName = $(this).val();

			if (newName === '') {
				newName = undefined;
			}

			if (out) {
				_Schema.setRelationshipProperty(rel, 'targetJsonName', newName, function() {
					blinkGreen($('.' + key, el));
					remotePropertyKeys.push('_' + newName);
					remotePropertyKeys = without('_' + key, remotePropertyKeys);
				}, function() {
					blinkRed($('.' + key, el));
				});
			} else {
				_Schema.setRelationshipProperty(rel, 'sourceJsonName', newName, function() {
					blinkGreen($('.' + key, el));
					remotePropertyKeys.push('_' + newName);
					remotePropertyKeys = without('_' + key, remotePropertyKeys);
				}, function() {
					blinkRed($('.' + key, el));
				});
			}
		});

	},
	appendMethod: function(el, method) {

		// append default actions
		el.append('<tr class="' + method.name + '">'
				+ '<td class="name-col"><div class="abs-pos-helper">'
					+ '<input size="15" type="text" class="property-name action" value="' + escapeForHtmlAttributes(method.name) + '">'
					+ '<img alt="Drag to resize" title="Drag to resize" class="resize-handle" src="' + _Icons.arrow_up_down + '">'
				+ '</div></td>'
				+ '<td><textarea rows="4" class="property-code action">' + escapeForHtmlAttributes(method.source || '') + '</textarea></td>'
				+ '<td><textarea rows="4" class="property-comment action">' + escapeForHtmlAttributes(method.comment || '') + '</textarea></td>'
				+ '<td>'
					+ '<img alt="Accept" title="Save changes" class="create-icon save-action hidden" src="' + _Icons.tick_icon + '">'
					+ '<img alt="Cancel" title="Discard changes" class="remove-icon cancel-action hidden" src="' + _Icons.cross_icon + '">'
					+ '<img alt="Remove" title="Remove method" class="remove-icon remove-action" src="' + _Icons.delete_icon + '">'
				+ '</td></tr>');

		var tr = $('tr:last-of-type', el);
		_Schema.makeRowResizable(tr);

		var activate = function() {
			$('.save-action', tr).removeClass('hidden');
			$('.cancel-action', tr).removeClass('hidden');
			$('.remove-action', tr).addClass('hidden');
		};

		$('.property-name.action', tr).on('change', activate).on('keyup', activate);
		$('.property-code.action', tr).on('change', activate).on('keyup', activate);
		$('.property-comment.action', tr).on('change', activate).on('keyup', activate);

		$('.save-action', tr).on('click', function() {
			_Schema.saveMethod(method);
		});

		$('.cancel-action', tr).on('click', function() {

			// restore previous values
			$('.action.property-name', tr).val(method.name);
			$('.action.property-code', tr).val(method.source);
			$('.action.property-comment', tr).val(method.comment);

			// also restore CodeMirror text
			($('.action.property-code', tr).closest('td').find('.CodeMirror').get(0).CodeMirror).setValue(method.source);
			($('.action.property-comment', tr).closest('td').find('.CodeMirror').get(0).CodeMirror).setValue(method.comment);

			$('.save-action', tr).addClass('hidden');
			$('.cancel-action', tr).addClass('hidden');
			$('.remove-action', tr).removeClass('hidden');
		});

		$('.remove-action', tr).on('click', function() {
			_Schema.confirmRemoveSchemaEntity(method, 'Delete method', function() {
				_Schema.openEditDialog(method.schemaNode.id, 'methods', function () {
					$('li#tab-methods').click();
				});
			});
		});
	},
	appendView: function(el, view, resource, entity) {

		var key      = view.name;
		var selectId = 'select-' + key;

		el.append('<tr class="' + view.name + '"><td style="width:20%;"><input size="15" type="text" class="property-name view" value="' + escapeForHtmlAttributes(view.name) + '">'
				+ '</td><td id="' + selectId + '"></td><td>'
				+ '<img alt="Save" title="Save changes" class="create-icon save-view hidden" src="' + _Icons.tick_icon + '">'
				+ '<img alt="Cancel" title="Discard changes" class="remove-icon cancel-view hidden" src="' + _Icons.cross_icon + '">'
				+ (view.isBuiltinView ? '<img alt="Reset" title="Reset view" class="remove-icon reset-view" src="' + _Icons.arrow_undo_icon + '">' : '<img alt="Remove" title="Remove view" class="remove-icon remove-view" src="' + _Icons.delete_icon + '">')
				+ '</td></tr>');

		_Schema.appendViewSelectionElement('#' + selectId, view, resource, entity);

		$('.' + key + ' .save-view').on('click', function() {
			_Schema.saveView(view, entity);
		});

		$('.' + key + ' .cancel-view').on('click', function() {

			var tr = $(this).parents('tr');
			var select = $('select', tr);

			// restore previous values
			$('.view.property-name', tr).val(view.name);

			// reset properties to previous state
			Command.listSchemaProperties(entity.id, view.name, function(data) {

				data.forEach(function(prop) {

					$('option[value="' + prop.name + '"]', select).prop('selected', prop.isSelected).trigger('chosen:updated');
				});
			});

			$('.save-view', tr).addClass('hidden');
			$('.cancel-view', tr).addClass('hidden');
			$('.remove-view', tr).removeClass('hidden');

			select.trigger('chosen:updated');
		});

		$('.' + key + ' .remove-view').on('click', function() {
			_Schema.confirmRemoveSchemaEntity(view, 'Delete view', function() { _Schema.openEditDialog(view.schemaNode.id, 'views'); });
		});

		$('.' + key + ' .reset-view').on('click', function() {
			_Schema.confirmRemoveSchemaEntity(view, 'Reset view', function() { _Schema.openEditDialog(view.schemaNode.id, 'views'); });
		});
	},
	appendViewSelectionElement: function(selector, view, resource, schemaEntity) {

		var el = $(selector).last();

		el.append('<select class="property-attrs view" multiple="multiple"></select>');
		var viewSelectElem = $('.property-attrs', el);

		if (view && view.id) {

			viewSelectElem.on('change', function() {

				var tr = $(this).parents('tr');
				$('.save-view', tr).removeClass('hidden');
				$('.cancel-view', tr).removeClass('hidden');
				$('.remove-view', tr).addClass('hidden');
			});

			Command.listSchemaProperties(schemaEntity.id, view.name, function(data) {
				_Schema.appendViewOptions(viewSelectElem, view.name, data);
			});

		} else {

			Command.listSchemaProperties(schemaEntity.id, view.name, function(data) {
				_Schema.appendViewOptions(viewSelectElem, view.name, data);
			});
		}
	},
	appendViewOptions: function(viewSelectElem, viewName, properties) {

		properties.forEach(function(prop) {

			var name       = prop.name;
			var isSelected = prop.isSelected ? ' selected="selected"' : '';
			var isDisabled = (viewName === 'ui' || prop.isDisabled) ? ' disabled="disabled"' : '';

			viewSelectElem.append('<option value="' + name + '"' + isSelected + isDisabled + '>' + name + '</option>');
		});

		viewSelectElem.chosen({ search_contains: true, width: '100%' });

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
	createMethod: function(el, entity) {

		var key = 'new';

		_Schema.unbindEvents(key);

		var name    = $('.' + key + ' .action.property-name').val();
		var source  = $('.' + key + ' .action.property-code').val();
		var comment = $('.' + key + ' .action.property-comment').val();

		if (name && name.length) {

			var obj = {
				schemaNode: { id: entity.id },
				name:    name,
				source:  source,
				comment: comment
			};

			_Schema.storeSchemaEntity('schema_methods', {}, JSON.stringify(obj), function(result) {

				if (result && result.result) {

					var id = result.result[0];

					$.ajax({
						url: rootUrl + id,
						type: 'GET',
						dataType: 'json',
						contentType: 'application/json; charset=utf-8',
						statusCode: {

							200: function(data) {

								var method = data.result;
								var name   = method.name;
								var row    = $('.new', el);

								blinkGreen(row);

								_Schema.reload();

								_Schema.unbindEvents(name);

								row.removeClass('new').addClass('action').addClass(name);
								row = $('.action.' + name, el);

								$('.create-action', row).remove();
								$('.remove-action', row).off('click');

								$('.remove-action', row).on('click', function() {
									_Schema.confirmRemoveSchemaEntity(method, 'Delete method', function() { _Schema.openEditDialog(method.schemaNode.id, 'methods'); });
								});

								$('.' + name + ' .property-name.action').on('blur', function() {
									_Schema.saveMethod(method);
								});

								$('.' + name + ' .property-code.action').on('blur', function() {
									_Schema.saveMethod(method);
								});

								$('.' + name + ' .property-comment.action').on('blur', function() {
									_Schema.saveMethod(method);
								});

								_Schema.bindEvents(method);
							}
						}
					});
				}

			}, function() {

				blinkRed($('.actions .' + key));
				_Schema.bindEvents(method);

			}, function() {

				_Schema.bindEvents(method);
			});
		}

	},
	saveMethod: function(method) {

		var key = method.name;

		_Schema.unbindEvents(key);

		var name    = $('.' + key + ' .action.property-name').val();
		var source  = $('.' + key + ' .action.property-code').val();
		var comment = $('.' + key + ' .action.property-comment').val();

		if (name && name.length) {

			var obj = {
				name:    name,
				source:  source,
				comment: comment
			};

			_Schema.storeSchemaEntity('schema_methods', method, JSON.stringify(obj), function() {

				blinkGreen($('.actions .' + key));

				// accept values into property object
				method.name    = obj.name;
				method.source  = obj.source;
				method.comment = obj.comment;

				// update row class so that subsequent changes can be applied
				$('.' + key).removeClass(key).addClass(method.name);

				_Schema.bindEvents(method);

				// restore button state before removing key
				$('.' + method.name + ' .save-action').addClass('hidden');
				$('.' + method.name + ' .cancel-action').addClass('hidden');
				$('.' + method.name + ' .remove-action').removeClass('hidden');

			}, function(data) {

				Structr.errorFromResponse(data.responseJSON);

				blinkRed($('.actions .' + key));
				_Schema.bindEvents(method);

			}, function() {

				_Schema.bindEvents(method);
			});
		}
	},
	createView: function(el, entity) {

		var key = 'new';

		_Schema.unbindEvents(key);

		var name = $('.' + key + ' .view.property-name').val();
		var attrs = $('.' + key + ' .view.property-attrs').val();

		if (name && name.length) {

			var obj = {};
			obj.schemaNode         = { id: entity.id };
			obj.schemaProperties   = _Schema.findSchemaPropertiesByNodeAndName({}, entity, attrs);
			obj.nonGraphProperties = _Schema.findNonGraphProperties(entity, attrs);
			obj.name               = name;

			_Schema.storeSchemaEntity('schema_views', {}, JSON.stringify(obj), function(result) {

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
								var row  = $('.new', el);

								blinkGreen(row);

								_Schema.reload();

								_Schema.unbindEvents('new');

								row.removeClass('new').addClass('view').addClass(name);
								row = $('.view.' + name, el);

								$('.remove-view', row).off('click');

								$('.remove-view', row).on('click', function() {
									_Schema.confirmRemoveSchemaEntity(view, 'Delete view', function() { _Schema.openEditDialog(view.schemaNode.id, 'views'); } );
								});

								_Schema.bindEvents(view);
							}
						}
					});
				}

			}, function() {

				blinkRed($('.views .' + key));
				_Schema.bindEvents({name: key});

			}, function() {

				_Schema.bindEvents({name: key});
			});
		}
	},
	saveView: function(view, entity) {

		var key = view.name;

		_Schema.unbindEvents(key);

		var name = $('.' + key + ' .view.property-name').val();
		var attrs = $('.' + key + ' .view.property-attrs').val();

		// add disabled attributes as well
		$.each($('.' + key + ' .view.property-attrs').children(), function(i, child) {
			if (child.disabled && child.selected) {
				attrs.push(child.value);
			}
		});

		if (name && name.length) {

			var obj                = {};
			obj.schemaNode         = { id: entity.id };
			obj.schemaProperties   = _Schema.findSchemaPropertiesByNodeAndName(entity, attrs);
			obj.nonGraphProperties = _Schema.findNonGraphProperties(entity, attrs);
			obj.name               = name;

			_Schema.storeSchemaEntity('schema_views', view, JSON.stringify(obj), function() {

				blinkGreen($('.views .' + key));

				// accept values into property object
				view.schemaProperties = obj.schemaProperties;
				view.name             = obj.name;

				// update row class so that consequent changes can be applied
				$('.' + key).removeClass(key).addClass(view.name);

				_Schema.bindEvents(view);

				// restore button state before removing key
				$('.' + view.name + ' .save-view').addClass('hidden');
				$('.' + view.name + ' .cancel-view').addClass('hidden');
				$('.' + view.name + ' .remove-view').removeClass('hidden');

			}, function(data) {

				Structr.errorFromResponse(data.responseJSON);

				blinkRed($('.views .' + key));
				_Schema.bindEvents(view);

			}, function() {

				_Schema.bindEvents(view);
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
						//Structr.errorFromResponse(data.responseJSON);
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
										//Structr.errorFromResponse(data.responseJSON);
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
					Structr.errorFromResponse(data.responseJSON);
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
					Structr.errorFromResponse(data.responseJSON);
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

					var changed = Object.keys(newData).some(function (key) {
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
								200: function(data, textStatus, jqXHR) {
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
						_Schema.reload();
					}
				}
			}
		});
	},
	connect: function(sourceId, targetId) {
		//Structr.dialog('Enter relationship details');
		_Schema.createRelationshipDefinition(sourceId, targetId, initialRelType);

	},
	detach: function(relationshipId) {
		//Structr.dialog('Enter relationship details');
		_Schema.removeRelationshipDefinition(relationshipId);
	},
	makeAttrEditable: function(element, key, isRel) {
		//element.off('dblclick');

		// cut off three leading underscores and only use 32 characters (the UUID)
		var id = element.prop('id').substring(3,35);

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
					btn.html(text + ' <img src="' + _Icons.tick_icon + '">');
					window.setTimeout(function() {
						$('img', btn).fadeOut();
						document.location.reload();
					}, 1000);
				}
			}
		});
	},
	syncSchemaDialog: function() {

		Structr.dialog('Sync schema to remote server', function() {},  function() {});

		var pushConf = JSON.parse(LSWrapper.getItem(pushConfigKey)) || {};

		dialog.append('To sync <b>all schema nodes and relationships</b> to the remote server, ');
		dialog.append('enter host, port, username and password of your remote instance and click Start.');

		dialog.append('<p><button class="btn" id="pull"">Click here</button> if you want to sync your local schema with schema nodes and relationships from the remote server.</p>');

		$('#pull', dialog).on('click', function(e) {
			e.stopPropagation();
			Structr.pullDialog('SchemaNode,SchemaRelationshipNode');
		});

		dialog.append('<table class="props push">'
				+ '<tr><td>Host</td><td><input id="push-host" type="text" length="20" value="' + (pushConf.host || '') + '"></td></tr>'
				+ '<tr><td>Port</td><td><input id="push-port" type="text" length="20" value="' + (pushConf.port || '') + '"></td></tr>'
				+ '<tr><td>Username</td><td><input id="push-username" type="text" length="20" value="' + (pushConf.username || '') + '"></td></tr>'
				+ '<tr><td>Password</td><td><input id="push-password" type="password" length="20" value="' + (pushConf.password || '') + '"></td></tr>'
				+ '</table>'
				+ '<button id="start-push">Start</button>');



		$('#start-push', dialog).on('click', function() {
			var host = $('#push-host', dialog).val();
			var port = parseInt($('#push-port', dialog).val());
			var username = $('#push-username', dialog).val();
			var password = $('#push-password', dialog).val();
			var key = 'key_push_schema';

			pushConf = {host: host, port: port, username: username, password: password};
			LSWrapper.setItem(pushConfigKey, JSON.stringify(pushConf));

			Command.pushSchema(host, port, username, password, key, function() {
				dialog.empty();
				dialogCancelButton.click();
			});
		});

		return false;
	},
	snapshotsDialog: function() {

		Structr.dialog('Schema Snapshots', function() {}, function() {});

		dialog.append('<h3>Create snapshot</h3>');
		dialog.append('<p>Creates a new snapshot of the current schema configuration that can be restored later. You can enter an (optional) suffix for the snapshot.</p>');
		dialog.append('<p><input type="text" name="suffix" id="snapshot-suffix" placeholder="Enter a suffix" length="20" /> <button id="create-snapshot">Create snapshot</button></p>');

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

							Command.snapshots("restore", snapshot, null, function(data) {

								var status = data[0].status;

								if (status === 'success') {
									window.location.reload();
								} else {

									if (dialogBox.is(':visible')) {

										dialogMsg.html('<div class="infoBox error">' + status + '</div>');
										$('.infoBox', dialogMsg).delay(2000).fadeOut(200);
									}
								}
							});
						});
						$('#add-' + i).on('click', function() {

							Command.snapshots('add', snapshot, null, function(data) {

								var status = data[0].status;

								if (status === 'success') {
									window.location.reload();
								} else {

									if (dialogBox.is(':visible')) {

										dialogMsg.html('<div class="infoBox error">' + status + '</div>');
										$('.infoBox', dialogMsg).delay(2000).fadeOut(200);
									}
								}
							});
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
			if (selectedNodes && selectedNodes.length) {
				selectedNodes.forEach(function(selectedNode) {
					types.push(selectedNode.name);
				});

				$('.label.rel-type', canvas).each(function (idx, el) {
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

						dialogMsg.html('<div class="infoBox success">Snapshot successfully created</div>');
						$('.infoBox', dialogMsg).delay(2000).fadeOut(200);

					} else {

						dialogMsg.html('<div class="infoBox error">Snapshot creation failed.</div>');
						$('.infoBox', dialogMsg).delay(2000).fadeOut(200);
					}
				}

				refresh();
			});

		});

		dialog.append('<h3>Available snapshots to restore</h3>');

		dialog.append('<table class="props" id="snapshots"></table>');

		var table = $('#snapshots');

		refresh();

		// update button
		dialog.append('<p style="text-align: right;"><button id="refresh-snapshots">Refresh</button></p>');
		$('#refresh-snapshots').on('click', refresh);

		return false;
	},
	openAdminTools: function() {
		Structr.dialog('Admin Tools', function() {}, function() {});

		dialogText.append('<table id="admin-tools-table"></table>');
		var toolsTable = $('#admin-tools-table');
		toolsTable.append('<tr><td><button id="rebuild-index"><img src="' + _Icons.refresh_icon + '"> Rebuild Index</button></td><td><label for"rebuild-index">Rebuild database index for all nodes and relationships</label></td></tr>');
		toolsTable.append('<tr><td><button id="clear-schema"><img src="' + _Icons.delete_icon + '"> Clear Schema</button></td><td><label for"clear-schema">Delete all schema nodes and relationships of dynamic schema</label></td></tr>');
		toolsTable.append('<tr><td><select id="node-type-selector"><option selected value="">-- Select Node Type --</option><option disabled>──────────</option><option value="allNodes">All Node Types</option><option disabled>──────────</option></select><button id="add-node-uuids">Add UUIDs</button></td><td><label for"setUuid">Add UUIDs to all nodes of the selected type</label></td></tr>');
		toolsTable.append('<tr><td><select id="rel-type-selector"><option selected value="">-- Select Relationship Type --</option><option disabled>──────────</option><option value="allRels">All Relationship Types</option><option disabled>──────────</option></select><button id="add-rel-uuids">Add UUIDs</button></td><td><label for"setUuid">Add UUIDs to all relationships of the selected type</label></td></tr>');

		$('#rebuild-index').on('click', function(e) {
			var btn = $(this);
			var text = btn.text();
			btn.attr('disabled', 'disabled').addClass('disabled').html(text + ' <img src="' + _Icons.ajax_loader_2 + '">');
			e.preventDefault();
			$.ajax({
				url: rootUrl + 'maintenance/rebuildIndex',
				type: 'POST',
				data: {},
				contentType: 'application/json',
				statusCode: {
					200: function() {
						var btn = $('#rebuild-index');
						btn.removeClass('disabled').attr('disabled', null);
						btn.html(text + ' <img src="' + _Icons.tick_icon + '">');
						window.setTimeout(function() {
							$('img', btn).fadeOut();
						}, 1000);
					}
				}
			});
		});

		$('#clear-schema').on('click', function(e) {

			Structr.confirmation('<h3>Delete schema?</h3><p>This will remove all dynamic schema information, but not your other data.</p><p>&nbsp;</p>',
					function() {
						$.unblockUI({
							fadeOut: 25
						});

						var btn = $(this);
						var text = btn.text();
						btn.attr('disabled', 'disabled').addClass('disabled').html(text + ' <img src="' + _Icons.ajax_loader_2 + '">');
						e.preventDefault();
						$.ajax({
							url: rootUrl + 'schema_relationship_nodes',
							type: 'DELETE',
							data: {},
							contentType: 'application/json',
							statusCode: {
								200: function() {
									_Schema.reload();
									$.ajax({
										url: rootUrl + 'schema_nodes',
										type: 'DELETE',
										data: {},
										contentType: 'application/json',
										statusCode: {
											200: function() {
												_Schema.reload();
												var btn = $('#clear-schema');
												btn.removeClass('disabled').attr('disabled', null);
												btn.html(text + ' <img src="' + _Icons.tick_icon + '">');
												window.setTimeout(function() {
													$('img', btn).fadeOut();
												}, 1000);
											}
										}
									});

								}
							}
						});
					});
		});

		var nodeTypeSelector = $('#node-type-selector');
		Command.list('SchemaNode', true, 1000, 1, 'name', 'asc', 'id,name', function(nodes) {
			nodes.forEach(function(node) {
				nodeTypeSelector.append('<option>' + node.name + '</option>');
			});
		});

		$('#add-node-uuids').on('click', function(e) {
			var btn = $(this);
			var text = btn.text();
			e.preventDefault();
			var type = nodeTypeSelector.val();
			if (!type) {
				nodeTypeSelector.addClass('notify');
				nodeTypeSelector.on('change', function() {
					nodeTypeSelector.removeClass('notify');
				});
				return;
			}
			var data;
			if (type === 'allNodes') {
				data = JSON.stringify({'allNodes': true});
			} else {
				data = JSON.stringify({'type': type});
			}
			btn.attr('disabled', 'disabled').addClass('disabled').html(text + ' <img src="' + _Icons.ajax_loader_2 + '">');
			$.ajax({
				url: rootUrl + 'maintenance/setUuid',
				type: 'POST',
				data: data,
				contentType: 'application/json',
				statusCode: {
					200: function() {
						var btn = $('#add-node-uuids');
						nodeTypeSelector.removeClass('notify');
						btn.removeClass('disabled').attr('disabled', null);
						btn.html(text + ' <img src="' + _Icons.tick_icon + '">');
						window.setTimeout(function() {
							$('img', btn).fadeOut();
						}, 1000);
					}
				}
			});
		});

		var relTypeSelector = $('#rel-type-selector');
		Command.list('SchemaRelationshipNode', true, 1000, 1, 'relationshipType', 'asc', 'id,relationshipType', function(rels) {
			rels.forEach(function(rel) {
				relTypeSelector.append('<option>' + rel.relationshipType + '</option>');
			});
		});

		$('#add-rel-uuids').on('click', function(e) {
			var btn = $(this);
			var text = btn.text();
			e.preventDefault();
			var relType = relTypeSelector.val();
			if (!relType) {
				relTypeSelector.addClass('notify');
				relTypeSelector.on('change', function() {
					relTypeSelector.removeClass('notify');
				});
				return;
			}
			var data;
			if (relType === 'allRels') {
				data = JSON.stringify({'allRels': true});
			} else {
				data = JSON.stringify({'relType': relType});
			}
			btn.attr('disabled', 'disabled').addClass('disabled').html(text + ' <img src="' + _Icons.ajax_loader_2 + '">');
			$.ajax({
				url: rootUrl + 'maintenance/setUuid',
				type: 'POST',
				data: data,
				contentType: 'application/json',
				statusCode: {
					200: function() {
						var btn = $('#add-rel-uuids');
						relTypeSelector.removeClass('notify');
						btn.removeClass('disabled').attr('disabled', null);
						btn.html(text + ' <img src="' + _Icons.tick_icon + '">');
						window.setTimeout(function() {
							$('img', btn).fadeOut();
						}, 1000);
					}
				}
			});
		});

		dialogText.append('<h2 class="dialogTitle">Layout Tools</h2>');
		dialogText.append('<table id="layout-tools-table"></table>');
		var layoutsTable = $('#layout-tools-table');
		layoutsTable.append('<tr><td><button id="save-layout-to-database"><img src="' + _Icons.database_icon + '"> Save Schema Layout</button></td><td><label for"save-layout">Save current positions to backend (for your user account only)</label></td></tr>');
		layoutsTable.append('<tr><td><input id="save-layout-filename" placeholder="Enter name for layout"><button id="save-layout-file">Save Layout</button></td><td><label for"export-layout">Save current positions to backend (for every user to load)</label></td></tr>');
		layoutsTable.append('<tr><td><select id="saved-layout-selector"></select><button id="apply-layout"><img src="' + _Icons.wand_icon + '"> Apply</button><button id="delete-layout"><img src="' + _Icons.delete_icon + '"> Delete</button></td><td><label for"import-layout">Load or delete stored layouts.</label></td></tr>');

		var layoutSelector = $('#saved-layout-selector');

		$('#save-layout-to-database', layoutsTable).click(function() {
			Structr.saveLocalStorage();
		});

		$('#save-layout-file', layoutsTable).click(function() {
			var fileName = $('#save-layout-filename').val().replaceAll(/[^\w_\-\. ]+/, '-');

			if (fileName && fileName.length) {

				var url = rootUrl + 'schema_nodes';
				$.ajax({
					url: url,
					dataType: 'json',
					contentType: 'application/json; charset=utf-8',
					success: function(data) {
						var res = {};
						data.result.forEach(function (entity, idx) {
							var pos = _Schema.getPosition(entity.name);
							if (pos) {
								res[entity.name] = {position: pos};
							}
						});

						Command.layouts('add', fileName, JSON.stringify(res), function() {
							refresh();
							$('#save-layout-filename').val('');

							blinkGreen(layoutSelector);
						});
					}
				});

			} else {
				Structr.error('Schema layout name is required.');
			}
		});

		$('#apply-layout').click(function () {

			var selectedLayout = layoutSelector.val();

			if (selectedLayout && selectedLayout.length) {

				Command.layouts('get', selectedLayout, null, function(result) {

					var obj;

					try {
						obj = JSON.parse(result.schemaLayout);
					} catch (e) {
						alert ("Unreadable JSON - please make sure you are using JSON exported from this dialog!");
					}

					if (obj) {
						Object.keys(obj).forEach(function (type) {
							LSWrapper.setItem(type + localStorageSuffix + 'node-position', JSON.stringify(obj[type]));
						});

						$('#schema-graph .node').each(function(i, n) {
							var node = $(n);
							var type = node.text();

							if (obj[type]) {
								node.css('top', obj[type].position.top);
								node.css('left', obj[type].position.left);
							}
						});

						Structr.saveLocalStorage();

						instance.repaintEverything();

						$('#schema-layout-import-textarea').val('Import successful - imported ' + Object.keys(obj).length + ' positions.');

						window.setTimeout(function () {
							$('#schema-layout-import-row').hide();
							$('#schema-layout-import-textarea').val('');
						}, 2000);

					}

				});

			} else {
				Structr.error('Please select a schema to load.');
			}

		});

		$('#delete-layout', layoutsTable).click(function() {

			var selectedLayout = layoutSelector.val();

			if (selectedLayout && selectedLayout.length) {

				Command.layouts('delete', selectedLayout, null, function() {
					refresh();
					blinkGreen(layoutSelector);
				});

			} else {
				Structr.error('Please select a schema to delete.');
			}

		});

		var refresh = function () {

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
		refresh();

	},
	openSchemaDisplayOptions: function() {
		Structr.dialog('Schema Display Options', function() {
		}, function() {
		});

		dialogText.append('<h3>Visibility</h3><table class="props" id="schema-options-table"><tr><th>Type</th><th>Visible <input type="checkbox" id="toggle-all-types"><img class="invert-icon" src="' + _Icons.toggle_icon + '" id="invert-all-types"></button></th></table>');
		var schemaOptionsTable = $('#schema-options-table');

		// list: function(type, rootOnly, pageSize, page, sort, order, properties, callback)
		Command.list('SchemaNode', false, 1000, 1, 'name', 'asc', null, function(schemaNodes) {
			schemaNodes.forEach(function(schemaNode) {
				schemaOptionsTable.append('<tr><td>' + schemaNode.name + '</td><td><input class="toggle-type" data-structr-id="' + schemaNode.id + '" type="checkbox" ' + (hiddenSchemaNodes.indexOf(schemaNode.id) > -1 ? '' : 'checked') + '></td></tr>');
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

			$('#save-options', schemaOptionsTable).on('click', function() {
				Structr.saveLocalStorage();
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
		var key = inp.attr('data-structr-id');
		//console.log(inp, key, inp.is(':checked'));
		if (!inp.is(':checked')) {
			if (hiddenSchemaNodes.indexOf(key) === -1) {
				hiddenSchemaNodes.push(key);
			}
			nodes[key] = undefined;
			LSWrapper.setItem(hiddenSchemaNodesKey, JSON.stringify(hiddenSchemaNodes));
		} else {
			if (hiddenSchemaNodes.indexOf(key) > -1) {
				hiddenSchemaNodes.splice(hiddenSchemaNodes.indexOf(key), 1);
				Command.get(key, function(schemaNode) {
					nodes[key] = schemaNode;
					LSWrapper.setItem(hiddenSchemaNodesKey, JSON.stringify(hiddenSchemaNodes));
				});
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
	getSchemaCSSTransform: function () {
	 return _Schema.getSchemaCSSScale() + ' ' + _Schema.getSchemaCSSTranslate();
	},
	getSchemaCSSScale: function () {
		return 'scale(' + zoomLevel + ')';
	},
	getSchemaCSSTranslate: function () {
		return 'translate(' + (inheritanceSlideoutOpen ? inheritanceSlideout.outerWidth() / zoomLevel : '0') + 'px)';
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
		LSWrapper.setItem(showSchemaOverlaysKey, show);
		$('#schema-show-overlays').prop('checked', show);
		if (show) {
			$('.rel-type, .multiplicity').show();
		} else {
			$('.rel-type, .multiplicity').hide();
		}
	},
	loadClassTree: function () {
		var classTree = {};
		var tmpHierarchy = {};
		var classnameToId = {};

		var insertClassInClassTree = function (classObj, tree) {
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
				classes.forEach(function (className) {
					if (!done) {
						done = insertClassInClassTree(classObj, tree[className]);
					}
				});
				return done;
			}

		};

		var printClassTree = function ($elem, classTree) {
			var classes = Object.keys(classTree).sort();

			if (classes.length > 0) {

				var $newUl = $('<ul></ul>').appendTo($elem);

				classes.forEach(function (classname) {

					var icons = (classname !== 'AbstractNode' ? '<img class="delete_icon icon delete" src="' + _Icons.delete_icon + '"><img class="edit_icon icon edit" src="' + _Icons.edit_icon + '">' : '');
					var classId = (classname !== 'AbstractNode' ? ' data-id="' + classnameToId[classname] + '"' : '');

					var $newLi = $('<li data-jstree=\'{"opened":true}\'' + classId + '>' + classname + icons + '</li>').appendTo($newUl);
					printClassTree($newLi, classTree[classname]);

				});

			}

		};

		$.get(rootUrl + 'SchemaNode?sort=hierarchyLevel&order=asc', function (data) {
			schemaNodes = data.result;
			schemaNodes.forEach(function (schemaNode) {

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
			}).on('changed.jstree', function (e, data) {
				var $node = $('#id_' + data.node.data.id);
				if ($node.length > 0) {
					$('.selected').removeClass('selected');
					$node.addClass('selected');
					selectedElements = [$node];
				}
			});

			$('#search-classes').keyup(function (e) {
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
		});
	},
	enableEditFunctionsInClassTree: function() {
		$('img.edit_icon', inheritanceTree).off('click');
		$('img.edit_icon', inheritanceTree).on('click', function (e) {
			var nodeId = $(this).closest('li').data('id');
			if (nodeId) {
				_Schema.openEditDialog(nodeId);
			}
		});

		$('img.delete_icon', inheritanceTree).off('click');
		$('img.delete_icon', inheritanceTree).on('click', function (e) {
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
	getMethodsInitFunction: function () {
		return Structr.guardExecution(function () {
			// Intitialize editor(s)
			$('#tabView-methods textarea.property-code').each(function (i, el) {
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
			});

			$('#tabView-methods textarea.property-comment').each(function (i, el) {
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
			});
		});
	},
	senseCodeMirrorMode: function (contentText) {
		return (contentText.substring(0, 1) === "{") ? 'javascript' : 'none';
	},
	makeRowResizable: function ($tr) {
		var schemaMethodDrag = {};

		$('.resize-handle', $tr).draggable({
			axis: 'y',
			start: function(event, ui) {
				schemaMethodDrag.initialHeight = $(this).closest('td').height();
				schemaMethodDrag.begin = event.pageY;
			},
			drag: function(event, ui) {
				ui.position.top = Math.max( 0, ui.position.top );

				var newHeight = schemaMethodDrag.initialHeight + (event.pageY - schemaMethodDrag.begin);

				var tds = $(this).closest('tr').find('td');
				var cms = tds.find('.CodeMirror');

				tds.height( newHeight );
				cms.height( newHeight );

				cms.each(function (idx, cm) {
					cm.CodeMirror.refresh();
				});
			},
			stop: function() {
				$(this).attr('style', null);
			}
		});

	}
};