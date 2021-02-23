/*
 * Copyright (C) 2010-2021 Structr GmbH
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
var reload = false;
var schemaContainer;
var inheritanceTree, inheritanceSlideout;

$(document).ready(function() {
	Structr.registerModule(_Schema);
	Structr.classes.push('schema');

	$(document).on('click', '#inheritance-tree .edit_icon', function(e) {
		var nodeId = $(e.target).closest('li').data('id');
		if (nodeId) {
			_Schema.openEditDialog(nodeId);
		}
	});

	$(document).on('click', '#inheritance-tree .delete_icon', function(e) {
		var nodeId = $(e.target).closest('li').data('id');
		if (nodeId) {
			Structr.confirmation(
				'<h3>Delete schema node \'' + $(e.target).closest('a').text() + '\'?</h3><p>This will delete all incoming and outgoing schema relationships as well,<br> but no data will be removed.</p>',
				function() {
					$.unblockUI({ fadeOut: 25 });
					_Schema.deleteNode(nodeId);
				}
			);
		}
	});
});

var _Schema = {
	_moduleName: 'schema',
	schemaLoading: false,
	schemaLoaded: false,
	nodePositions: undefined,
	availableTypeNames: [],
	hiddenSchemaNodes: [],
	hiddenSchemaNodesKey: 'structrHiddenSchemaNodes_' + port,
	schemaPositionsKey: 'structrSchemaPositions_' + port,
	showSchemaOverlaysKey: 'structrShowSchemaOverlays_' + port,
	showSchemaInheritanceKey: 'structrShowSchemaInheritance_' + port,
	showJavaMethodsKey: 'structrShowJavaMethods_' + port,
	schemaMethodsHeightsKey: 'structrSchemaMethodsHeights_' + port,
	schemaActiveTabLeftKey: 'structrSchemaActiveTabLeft_' + port,
	activeSchemaToolsSelectedTabLevel1Key: 'structrSchemaToolsSelectedTabLevel1_' + port,
	activeSchemaToolsSelectedVisibilityTab: 'activeSchemaToolsSelectedVisibilityTab_' + port,
	schemaZoomLevelKey: localStorageSuffix + 'zoomLevel',
	schemaConnectorStyleKey: localStorageSuffix + 'connectorStyle',
	currentNodeDialogId: null,
	globalLayoutSelector: null,
	showJavaMethods: false,
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
			var obj = node.offset();
			obj.left = (obj.left - canvas.offset().left) / _Schema.ui.zoomLevel;
			obj.top  = (obj.top  - canvas.offset().top)  / _Schema.ui.zoomLevel;
			_Schema.nodePositions[type] = obj;
		});

		LSWrapper.setItem(_Schema.schemaPositionsKey, _Schema.nodePositions);
	},
	clearPositions: function() {
		LSWrapper.removeItem(_Schema.schemaPositionsKey);
		_Schema.reload();
	},
	init: async function(scrollPosition, callback) {

		let html = await Structr.fetchHtmlTemplate('schema/schema-container', {});

		schemaContainer[0].innerHTML = html;

		_Schema.schemaLoading = false;
		_Schema.schemaLoaded = false;
		_Schema.schema = [];
		_Schema.keys = [];

		_Schema.ui.connectorStyle  = LSWrapper.getItem(_Schema.schemaConnectorStyleKey) || 'Flowchart';
		_Schema.ui.zoomLevel       = parseFloat(LSWrapper.getItem(_Schema.schemaZoomLevelKey)) || 1.0;
		_Schema.ui.showInheritance = LSWrapper.getItem(_Schema.showSchemaInheritanceKey, true) || true;
		_Schema.showJavaMethods    = LSWrapper.getItem(_Schema.showJavaMethodsKey, false) || false;

		$('#connector-style').val(_Schema.ui.connectorStyle);
		$('#connector-style').off('change').on('change', function() {
			var newStyle = $(this).val();
			_Schema.ui.connectorStyle = newStyle;
			LSWrapper.setItem(_Schema.schemaConnectorStyleKey, newStyle);
			_Schema.reload();
		});

		$('#zoom-slider').slider({
			min:0.25,
			max:1,
			step:0.05,
			value:_Schema.ui.zoomLevel,
			slide: function( event, ui ) {
				_Schema.ui.zoomLevel = ui.value;
				LSWrapper.setItem(_Schema.schemaZoomLevelKey, _Schema.ui.zoomLevel);
				_Schema.ui.setZoom(_Schema.ui.zoomLevel, instance, [0,0], $('#schema-graph')[0]);
				if (_Schema.ui.selectedNodes.length > 0) {
					_Schema.ui.updateSelectedNodes();
				}
				_Schema.resize();
			}
		});

		$('#schema-show-overlays').off('change').on('change', function() {
			_Schema.ui.updateOverlayVisibility($(this).prop('checked'));
		});
		$('#schema-show-inheritance').off('change').on('change', function() {
			_Schema.ui.updateInheritanceVisibility($(this).prop('checked'));
		});
		$('#schema-tools').off('click').on('click', _Schema.openSchemaToolsDialog);
		$('#global-schema-methods').off('click').on('click', _Schema.methods.showGlobalSchemaMethods);

		_Schema.globalLayoutSelector = $('#saved-layout-selector-main');
		_Schema.updateGroupedLayoutSelector([_Schema.globalLayoutSelector], () => {
			$('#restore-schema-layout').off('click').on('click', () => {
				_Schema.restoreLayout(_Schema.globalLayoutSelector);
			});
		});

		$('#type-name').off('keyup').on('keyup', function(e) {

			if (e.keyCode === 13) {
				e.preventDefault();
				if ($('#type-name').val().length) {
					$('#create-type').click();
				}
				return false;
			}
		});

		$('#create-type').off('click').on('click', function() {
			_Schema.createNode($('#type-name').val());
		});

		jsPlumb.ready(function() {
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

				$('.node').css({zIndex: ++_Schema.ui.maxZ});

				instance.bind('connection', function(info, originalEvent) {

					if (info.connection.scope === 'jsPlumb_DefaultScope') {
						if (originalEvent) {

							Structr.dialog("Create Relationship", function() {
								dialogMeta.show();
							}, function() {
								_Schema.currentNodeDialogId = null;

								dialogMeta.show();
								instance.repaintEverything();
								instance.detach(info.connection);
							}, ['schema-edit-dialog']);

							_Schema.createRelationship(Structr.getIdFromPrefixIdString(info.sourceId, 'id_'), Structr.getIdFromPrefixIdString(info.targetId, 'id_'), dialogHead, dialogText);
						}
					} else {
						new MessageBuilder().warning('Moving existing relationships is not permitted!').title('Not allowed').requiresConfirmation().show();
						_Schema.reload();
					}
				});
				instance.bind('connectionDetached', function(info) {

					if (info.connection.scope !== 'jsPlumb_DefaultScope') {
						new MessageBuilder().warning('Deleting relationships is only possible via the delete button!').title('Not allowed').requiresConfirmation().show();
						_Schema.reload();
					}
				});
				reload = false;

				_Schema.ui.setZoom(_Schema.ui.zoomLevel, instance, [0,0], $('#schema-graph')[0]);

				$('._jsPlumb_connector').click(function(e) {
					e.stopPropagation();
					_Schema.ui.selectRel($(this));
				});

				canvas.off('mousedown').on('mousedown', function(e) {
					if (e.which === 1) {
						_Schema.ui.clearSelection();
						_Schema.ui.selectionStart(e);
					}
				});

				canvas.off('mousemove').on('mousemove', function(e) {
					if (e.which === 1) {
						_Schema.ui.selectionDrag(e);
					}
				});

				canvas.off('mouseup').on('mouseup', function(e) {
					_Schema.ui.selectionStop();
				});

				_Schema.resize();

				Structr.unblockMenu(500);

				var overlaysVisible = LSWrapper.getItem(_Schema.showSchemaOverlaysKey);
				var showSchemaOverlays = (overlaysVisible === null) ? true : overlaysVisible;
				_Schema.ui.updateOverlayVisibility(showSchemaOverlays);

				var inheritanceVisible = LSWrapper.getItem(_Schema.showSchemaInheritanceKey);
				var showSchemaInheritance = (inheritanceVisible === null) ? true : inheritanceVisible;
				_Schema.ui.updateInheritanceVisibility(showSchemaInheritance);

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

	},
	showSchemaRecompileMessage: function() {
		Structr.showNonBlockUILoadingMessage('Schema is compiling', 'Please wait...');
	},
	hideSchemaRecompileMessage:  function() {
		Structr.hideNonBlockUILoadingMessage();
	},
	onload: async function() {

		let html = await Structr.fetchHtmlTemplate('schema/schema', {});

		main[0].innerHTML = html;

		schemaContainer     = $('#schema-container');
		inheritanceSlideout = $('#inheritance-tree');
		inheritanceTree     = $('#inheritance-tree-container');

		var updateCanvasTranslation = function() {
			canvas.css('transform', _Schema.ui.getSchemaCSSTransform());
			_Schema.resize();
		};

		$('#inheritanceTab').off('click').on('click', function() {
			_Pages.leftSlideoutTrigger(this, inheritanceSlideout, [], _Schema.schemaActiveTabLeftKey, function() { inheritanceTree.show(); updateCanvasTranslation(); }, function() { updateCanvasTranslation(); inheritanceTree.hide(); });
		});

		_Schema.init();
		Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('schema'));

		$(window).off('resize').on('resize', function() {
			_Schema.resize();
		});
	},
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
	updateHiddenSchemaNodes: function() {

		return fetch(rootUrl + '/ApplicationConfigurationDataNode/ui?configType=layout').then(function(response) {

			return response.json();

		}).then(function(data) {

			if (data.result.length > 0) {

				return data.result[0].content;

			} else {

				_Schema.hiddenSchemaNodes = [];
				return false;
			}
		});
	},
	loadNodes: function(callback) {

		_Schema.hiddenSchemaNodes = JSON.parse(LSWrapper.getItem(_Schema.hiddenSchemaNodesKey));

		let savedHiddenSchemaNodesNull = (_Schema.hiddenSchemaNodes === null);

		Promise.resolve().then(function() {

			if (!_Schema.hiddenSchemaNodes) {

				return _Schema.updateHiddenSchemaNodes();

			} else {
				return false;
			}

		}).then(function(schemaLayout) {

			if (schemaLayout && !savedHiddenSchemaNodesNull) {

				_Schema.applySavedLayoutConfiguration(schemaLayout);
				return;

			} else {

				return fetch(rootUrl + 'SchemaNode/ui?sort=hierarchyLevel&order=asc').then(function(response) {

					if (response.ok) {
						return response.json();
					} else {
						throw new Error("Loading of Schema nodes failed");
					}

				}).then(handleSchemaNodeData).then(function(data) {
					if (typeof callback === 'function') {
						callback();
					}
				});
			}
		});

		let handleSchemaNodeData = function(data) {

			var entities         = {};
			var inheritancePairs = {};
			var hierarchy        = {};
			var x=0, y=0;

			if (savedHiddenSchemaNodesNull) {
				_Schema.hiddenSchemaNodes = data.result.filter(function(entity) {
					return entity.isBuiltinType;
				}).map(function(entity) {
					return entity.name;
				});
				LSWrapper.setItem(_Schema.hiddenSchemaNodesKey, JSON.stringify(_Schema.hiddenSchemaNodes));
			}

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
					//level += 100;

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

				entities['org.structr.dynamic.' + entity.name] = entity.id;
			});

			data.result.forEach(function(entity) {

				if (entity.extendsClass && entity.extendsClass !== 'org.structr.core.entity.AbstractNode') {

					if (entities[entity.extendsClass]) {
						var target = entities[entity.extendsClass];
						inheritancePairs[entity.id] = target;
					}
				}
			});

			Object.keys(hierarchy).forEach(function(key) {

				hierarchy[key].forEach(function(entity) {

					nodes[entity.id] = entity;

					if (_Schema.hiddenSchemaNodes.length > 0 && _Schema.hiddenSchemaNodes.indexOf(entity.name) > -1) {
						return;
					}

					var id = 'id_' + entity.id;
					canvas.append('<div class="schema node compact'
							+ (entity.isBuiltinType ? ' light' : '')
							+ '" id="' + id + '">'
							+ '<b>' + entity.name + '</b>'
							+ '<i class="icon delete ' + _Icons.getFullSpriteClass((entity.isBuiltinType ? _Icons.delete_disabled_icon : _Icons.delete_icon)) + '" />'
							+ '<i class="icon edit ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" />'
							+ '</div>');

					var node = $('#' + id);

					if (!entity.isBuiltinType) {

						node.children('b').off('click').on('click', function() {
							_Schema.makeAttrEditable(node, 'name');
						});

						node.children('.delete').off('click').on('click', function() {
							Structr.confirmation('<h3>Delete schema node \'' + entity.name + '\'?</h3><p>This will delete all incoming and outgoing schema relationships as well, but no data will be removed.</p>',
									function() {
										$.unblockUI({
											fadeOut: 25
										});
										_Schema.deleteNode(entity.id);
									});
						});
					}

					node.off('mousedown').on('mousedown', function() {
						node.css({zIndex: ++_Schema.ui.maxZ});
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

					$('.edit', node).off('click').on('click', function(e) {

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
							_Schema.ui.nodeDragStartpoint = {
								top:  (nodeOffset.top  - canvasOffset.top ),
								left: (nodeOffset.left - canvasOffset.left)
							};
						},
						drag: function(ui) {

							var $element = $(ui.el);

							if (!$element.hasClass('selected')) {

								_Schema.ui.clearSelection();

							} else {

								var nodeOffset = $element.offset();
								var canvasOffset = canvas.offset();

								var posDelta = {
									top:  (_Schema.ui.nodeDragStartpoint.top  - nodeOffset.top ),
									left: (_Schema.ui.nodeDragStartpoint.left - nodeOffset.left)
								};

								_Schema.ui.selectedNodes.forEach(function(selectedNode) {
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
							_Schema.ui.updateSelectedNodes();
							_Schema.resize();
						}
					});

					x++;
				});

				y++;
				x = 0;
			});

			for (var source of Object.keys(inheritancePairs)) {

				let target = inheritancePairs[source];
				let sourceEntity = nodes[source];
				let targetEntity = nodes[target];

				let i1 = _Schema.hiddenSchemaNodes.indexOf(sourceEntity.name);
				let i2 = _Schema.hiddenSchemaNodes.indexOf(targetEntity.name);

				if (_Schema.hiddenSchemaNodes.length > 0 && (i1 > -1 || i2 > -1)) {
					continue;
				}

				instance.connect({
					source: 'id_' + source,
					target: 'id_' + target,
					endpoint: 'Blank',
					anchors: [
						[ 'Perimeter', { shape: 'Rectangle' } ],
						[ 'Perimeter', { shape: 'Rectangle' } ]
					],
					connector: [ 'Straight', { curviness: 200, cornerRadius: 25, gap: 0 }],
					paintStyle: { lineWidth: 5, strokeStyle: "#dddddd", dashstyle: '2 2' },
					cssClass: "dashed-inheritance-relationship"
				});
			}
		};

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
			}, ['schema-edit-dialog']);

			method(entity, dialogHead, dialogText, targetView);

			_Schema.ui.clearSelection();
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
						connector: [_Schema.ui.connectorStyle, {curviness: 200, cornerRadius: 25, stub: [stub, 30], gap: 6, alwaysRespectStubs: true }],
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
							text: "It is highly advisable to set a relationship type on the relationship! To do this, click the pencil icon to open the edit dialog.<br><br><strong>Note: </strong>Any existing relationships of this type have to be migrated manually.",
							element: $('span', relTypeOverlay),
							customToggleIcon: _Icons.error_icon,
							appendToElement: $('#schema-container')
						});
					}

					$('#rel_' + res.id).parent().off('mouseover').on('mouseover', function() {
						$('#rel_' + res.id + ' .icon').showInlineBlock();
						$('#rel_' + res.id + ' .target-multiplicity').addClass('hover');
					}).off('mouseout').on('mouseout', function() {
						$('#rel_' + res.id + ' .icon').hide();
						$('#rel_' + res.id + ' .target-multiplicity').removeClass('hover');
					});

					$('#rel_' + res.id + ' .edit').off('click').on('click', function() {
						_Schema.openEditDialog(res.id);
					});

					$('#rel_' + res.id + ' .remove').off('click').on('click', function() {
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

		if (!entity.isBuiltinType && (!entity.extendsClass || entity.extendsClass.slice(-1) !== '>') ) {
			headContentDiv.append(' extends <select class="extends-class-select"></select>');
			headContentDiv.append(' <i id="edit-parent-class" class="icon edit ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" title="Edit parent class" />');

			$("#edit-parent-class", headContentDiv).click(function() {

				if (!$(this).hasClass('disabled')) {

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
				}
			});

			if (entity.extendsClass && entity.extendsClass.indexOf('org.structr.dynamic.') === 0) {
				$("#edit-parent-class").removeClass('disabled');
			} else {
				$("#edit-parent-class").addClass('disabled');
			}
		}

		headContentDiv.append('<div id="tabs" style="margin-top:20px;"><ul></ul></div>');
		let mainTabs = $('#tabs', headContentDiv);

		let contentDiv = $('<div class="schema-details"></div>');
		contentEl.append(contentDiv);

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'local', 'Local Attributes', targetView === 'local', function(c) {
			_Schema.properties.appendLocalProperties(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'views', 'Views', targetView === 'views', function(c) {
			_Schema.views.appendViews(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'methods', 'Methods', targetView === 'methods', function(c) {
			_Schema.methods.appendMethods(c, entity, _Schema.filterJavaMethods(entity.schemaMethods));
		}, null, _Schema.methods.refreshEditors);

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'remote', 'Remote Attributes', targetView === 'remote', function(c) {
			let editSchemaObjectLinkHandler = ($el) => {
				_Schema.openEditDialog($el.data('objectId'));
			};
			_Schema.remoteProperties.appendRemote(c, entity, editSchemaObjectLinkHandler);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'builtin', 'Inherited Attributes', targetView === 'builtin', function(c) {
			_Schema.properties.appendBuiltinProperties(c, entity);
		});

		if (!entity.isBuiltinType) {

			headContentDiv.children('b').off('click').on('click', function() {
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

		classSelect.off('change').on('change', function() {
			var obj = {extendsClass: $(this).val()};
			_Schema.storeSchemaEntity('schema_properties', entity, JSON.stringify(obj), function() {

				_Schema.openEditDialog(entity.id);
			});
		});

		Structr.resize();
	},
	createRemotePropertyNameSuggestion: function(typeName, cardinality) {

		if (cardinality === '1') {

			return 'a' + typeName;

		} else if (cardinality === '*') {

			let suggestion = 'many' + typeName;

			if (suggestion.slice(-1) !== 's') {
				suggestion += 's';
			}

			return suggestion;

		} else {

			new MessageBuilder().title('Unsupported cardinality: ' + cardinality).warning('Unable to generate suggestion for remote property name. Unsupported cardinality encountered!').requiresConfirmation().show();
			return typeName;
		}
	},
	createRelationship: async function(sourceId, targetId, headEl, contentEl) {

		let html = await Structr.fetchHtmlTemplate('schema/dialog.relationship', {});
		headEl.append(html);

		_Schema.appendCascadingDeleteHelpText();

		let sourceTypeName = nodes[sourceId].name;
		let targetTypeName = nodes[targetId].name;

		$('#source-type-name').text(sourceTypeName);
		$('#target-type-name').text(targetTypeName);

		$('#edit-rel-options-button').hide();
		let saveButton = $('#save-rel-options-button');
		let cancelButton = $('#cancel-rel-options-button');

		let previousSourceSuggestion = '';
		let previousTargetSuggestion = '';

		let updateSuggestions = () => {

			let currentSourceSuggestion = _Schema.createRemotePropertyNameSuggestion(sourceTypeName, $('#source-multiplicity-selector').val());
			let currentTargetSuggestion = _Schema.createRemotePropertyNameSuggestion(targetTypeName, $('#target-multiplicity-selector').val());

			if (previousSourceSuggestion === $('#source-json-name').val()) {
				$('#source-json-name').val(currentSourceSuggestion);
				previousSourceSuggestion = currentSourceSuggestion;
			}

			if (previousTargetSuggestion === $('#target-json-name').val()) {
				$('#target-json-name').val(currentTargetSuggestion);
				previousTargetSuggestion = currentTargetSuggestion;
			}
		};
		updateSuggestions();

		$('#source-multiplicity-selector').on('change', updateSuggestions);
		$('#target-multiplicity-selector').on('change', updateSuggestions);

		saveButton.off('click').on('click', function(e) {

			let relData = _Schema.getRelationshipDefinitionDataFromForm();
			relData.sourceId = sourceId;
			relData.targetId = targetId;

			if (relData.relationshipType.trim() === '') {

				blinkRed($('#relationship-type-name'));

			} else {

				_Schema.createRelationshipDefinition(relData, function(data) {

					_Schema.openEditDialog(data.result[0]);

					_Schema.reload();

				}, function(data) {

					let additionalInformation = {};

					if (data.responseJSON.errors.some((e) => { return (e.detail.indexOf('duplicate class') !== -1); })) {
						additionalInformation.requiresConfirmation = true;
						additionalInformation.title = 'Error';
						additionalInformation.overrideText = 'You are trying to create a second relationship named <strong>' + relData.relationshipType + '</strong> between these types.<br>Relationship names between types have to be unique.';
					}

					Structr.errorFromResponse(data.responseJSON, null, additionalInformation);
				});
			}
		});

		cancelButton.off('click').on('click', function(e) {
			dialogCancelButton.click();
		});

		Structr.resize();
	},
	appendCascadingDeleteHelpText: function() {
		Structr.appendInfoTextToElement({
			text: '<dl class="help-definitions"><dt>NONE</dt><dd>No cascading delete</dd><dt>SOURCE_TO_TARGET</dt><dd>Delete target node when source node is deleted</dd><dt>TARGET_TO_SOURCE</dt><dd>Delete source node when target node is deleted</dd><dt>ALWAYS</dt><dd>Delete source node if target node is deleted AND delete target node if source node is deleted</dd><dt>CONSTRAINT_BASED</dt><dd>Delete source or target node if deletion of the other side would result in a constraint violation on the node (e.g. notNull constraint)</dd></dl>',
			element: $('#cascading-delete-selector'),
			insertAfter: true
		});
	},
	getRelationshipDefinitionDataFromForm: function() {

		return {
			relationshipType: $('#relationship-type-name').val(),
			sourceMultiplicity: $('#source-multiplicity-selector').val(),
			targetMultiplicity: $('#target-multiplicity-selector').val(),
			sourceJsonName: $('#source-json-name').val(),
			targetJsonName: $('#target-json-name').val(),
			cascadingDeleteFlag: parseInt($('#cascading-delete-selector').val()),
			autocreationFlag: parseInt($('#autocreate-selector').val()),
			permissionPropagation: $('#propagation-selector').val(),
			readPropagation: $('#read-selector').val(),
			writePropagation: $('#write-selector').val(),
			deletePropagation: $('#delete-selector').val(),
			accessControlPropagation: $('#access-control-selector').val(),
			propertyMask: $('#masked-properties').val()
		};

	},
	loadRelationship: async function(entity, headEl, contentEl) {

		let html = await Structr.fetchHtmlTemplate('schema/dialog.relationship', {});
		headEl.append(html);

		$('#relationship-options select, #relationship-options textarea, #relationship-options input').attr('disabled', true);

		_Schema.appendCascadingDeleteHelpText();

		let mainTabs = $('#tabs', headEl);

		let contentDiv = $('<div class="schema-details"></div>');
		contentEl.append(contentDiv);

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'local', 'Local Attributes', true, function(c) {
			_Schema.properties.appendLocalProperties(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'views', 'Views', false, function(c) {
			_Schema.views.appendViews(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'methods', 'Methods', false, function(c) {
			_Schema.methods.appendMethods(c, entity, _Schema.filterJavaMethods(entity.schemaMethods));
		}, null, _Schema.methods.refreshEditors);

		let selectRelationshipOptions = function(rel) {
			$('#source-type-name').text(nodes[rel.sourceId].name).data('objectId', rel.sourceId);
			$('#source-json-name').val(rel.sourceJsonName || rel.oldSourceJsonName);
			$('#target-json-name').val(rel.targetJsonName || rel.oldTargetJsonName);
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

		$('.edit-schema-object', headEl).off('click').on('click', function(e) {
			e.stopPropagation();

			// todo: only navigate if no changes

			_Schema.openEditDialog($(this).data('objectId'));
			return false;
		});

		selectRelationshipOptions(entity);

		let sourceHelpElements = undefined;
		let targetHelpElements = undefined;

		let appendMultiplicityChangeInfo = (el) => {
			return Structr.appendInfoTextToElement({
				text: 'Multiplicity was changed without changing the remote property name - make sure this is correct',
				element: el,
				insertAfter: true,
				customToggleIcon: _Icons.error_icon,
				helpElementCss: {
					fontSize: '9pt'
				}
			});
		};

		let reactToCardinalityChange = () => {

			if (sourceHelpElements) {
				sourceHelpElements.map(e => e.remove());
				sourceHelpElements = undefined;
			}

			if ($('#source-multiplicity-selector').val() !== (entity.sourceMultiplicity || '*') && $('#source-json-name').val() === (entity.sourceJsonName || entity.oldSourceJsonName)) {
				sourceHelpElements = appendMultiplicityChangeInfo($('#source-json-name'));
			}

			if (targetHelpElements) {
				targetHelpElements.map(e => e.remove());
				targetHelpElements = undefined;
			}

			if ($('#target-multiplicity-selector').val() !== (entity.targetMultiplicity || '*') && $('#target-json-name').val() === (entity.targetJsonName || entity.oldTargetJsonName)) {
				targetHelpElements = appendMultiplicityChangeInfo($('#target-json-name'));
			}
		};

		$('#source-multiplicity-selector').on('change', reactToCardinalityChange);
		$('#target-multiplicity-selector').on('change', reactToCardinalityChange);
		$('#source-json-name').on('keyup', reactToCardinalityChange);
		$('#target-json-name').on('keyup', reactToCardinalityChange);

		let editButton = $('#edit-rel-options-button');
		let saveButton = $('#save-rel-options-button').hide();
		let cancelButton = $('#cancel-rel-options-button').hide();

		editButton.off('click').on('click', function(e) {
			e.preventDefault();

			$('#relationship-options select, #relationship-options textarea, #relationship-options input').attr('disabled', false).css('color', '').css('background-color', '');

			editButton.hide();
			saveButton.show();
			cancelButton.show();
		});

		saveButton.off('click').on('click', function(e) {

			let newData = _Schema.getRelationshipDefinitionDataFromForm();
			let relType = newData.relationshipType;

			Object.keys(newData).forEach(function(key) {
				if ( (entity[key] === newData[key])
						|| (key === 'cascadingDeleteFlag' && !(entity[key]) && newData[key] === 0)
						|| (key === 'autocreationFlag' && !(entity[key]) && newData[key] === 0)
						|| (key === 'propertyMask' && !(entity[key]) && newData[key].trim() === '')
				) {
					delete newData[key];
				}
			});

			if (relType.trim() === '') {

				blinkRed($('#relationship-type-name'));

			} else if (Object.keys(newData).length > 0) {

				$('#relationship-options select, #relationship-options textarea, #relationship-options input').attr('disabled', true);

				editButton.show();
				saveButton.hide();
				cancelButton.hide();

				_Schema.updateRelationship(entity, newData, function() {

					Object.keys(newData).forEach(function(attribute) {
						blinkGreen($('#relationship-options [data-attr-name=' + attribute + ']'));
						entity[attribute] = newData[attribute];
					});

				}, function(data) {

					var additionalInformation = {};

					if (data.responseJSON.errors.some((e) => { return (e.detail.indexOf('duplicate class') !== -1); })) {
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

		cancelButton.off('click').on('click', function(e) {

			selectRelationshipOptions(entity);
			$('#relationship-options select, #relationship-options textarea, #relationship-options input').attr('disabled', true);

			editButton.show();
			saveButton.hide();
			cancelButton.hide();
		});

		Structr.resize();
	},
	properties: {
		appendLocalProperties: async function(el, entity, overrides, optionalAfterSaveCallback) {

			let tableConfig = {
				class: 'local schema-props',
				cols: [
					{ class: '', title: 'JSON Name' },
					{ class: '', title: 'DB Name' },
					{ class: '', title: 'Type' },
					{ class: '', title: 'Format/Code' },
					{ class: '', title: 'Notnull' },
					{ class: '', title: 'Comp.' },
					{ class: '', title: 'Uniq.' },
					{ class: '', title: 'Idx' },
					{ class: '', title: 'Default' },
					{ class: 'actions-col', title: 'Action' }
				]
			};

			let html = await Structr.fetchHtmlTemplate('schema/schema-table', tableConfig);

			let propertiesTable = $(html);
			el.append(propertiesTable);
			el.append('<i title="Add local attribute" class="add-icon add-local-attribute ' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" />');
			let tbody = $('tbody', propertiesTable);

			_Schema.sort(entity.schemaProperties);

			let typeOptions = await Structr.fetchHtmlTemplate('schema/type-options', {});
			let typeHintOptions = await Structr.fetchHtmlTemplate('schema/type-hint-options', {});

			$.each(entity.schemaProperties, async function(i, prop) {

				let localProperty = await Structr.fetchHtmlTemplate('schema/property.local', {property: prop, typeOptions: typeOptions, typeHintOptions: typeHintOptions});

				let row = $(localProperty);

				tbody.append(row);

				_Schema.properties.setAttributesInRow(prop, row);
				_Schema.properties.bindRowEvents(prop, row, overrides);
			});

			$('.discard-all', propertiesTable).on('click', () => {
				tbody.find('i.discard-changes').click();
			});

			$('.save-all', propertiesTable).on('click', () => {
				_Schema.properties.bulkSave(el, tbody, entity, optionalAfterSaveCallback);
			});

			$('.add-local-attribute', el).off('click').on('click', async function() {

				let typeOptions = await Structr.fetchHtmlTemplate('schema/type-options', {});
				let newProperty = await Structr.fetchHtmlTemplate('schema/property.new', {typeOptions: typeOptions});

				let tr = $(newProperty);
				tbody.append(tr);

				$('.property-type', tr).off('change').on('change', function() {
					let selectedOption = $('option:selected', this);
					let shouldIndex = selectedOption.data('indexed');
					if (shouldIndex === undefined) {
						shouldIndex = true;
					}
					let indexedCb = $('.indexed', tr);
					if (indexedCb.prop('checked') !== shouldIndex) {
						indexedCb.prop('checked', shouldIndex);

						blink(indexedCb.closest('td'), '#fff', '#bde5f8');
						Structr.showAndHideInfoBoxMessage('Automatically updated indexed flag to default behavior for property type (you can still override this)', 'info', 2000, 200);
					}
				});

				$('.discard-changes', tr).off('click').on('click', function() {
					tr.remove();
					_Schema.properties.tableChanged(propertiesTable);
				});

				_Schema.properties.tableChanged(propertiesTable);
			});
		},
		bulkSave: function(el, tbody, entity, optionalAfterSaveCallback) {

			if (!_Schema.properties.hasUnsavedChanges(tbody.closest('table'))) {
				return;
			}

			let schemaProperties = [];
			let allow = true;
			let counts = {
				update:0,
				delete:0,
				new:0
			};

			tbody.find('tr').each((i, tr) => {

				let row        = $(tr);
				let propertyId = row.data('propertyId');
				let prop       = _Schema.properties.getInfoFromRow(row);

				if (propertyId) {
					if (row.hasClass('to-delete')) {
						// do not add this property to the list
						counts.delete++;
					} else if (row.hasClass('has-changes')) {
						// changed lines
						counts.update++;
						prop.id = propertyId;
						allow = _Schema.properties.validateProperty(prop, row) && allow;
						schemaProperties.push(prop);
					} else {
						// unchanged lines, only transmit id
						prop = { id: propertyId };
						schemaProperties.push(prop);
					}

				} else {
					//new lines
					counts.new++;
					prop.type = 'SchemaProperty';
					allow = _Schema.properties.validateProperty(prop, row) && allow;
					schemaProperties.push(prop);
				}
			});

			if (allow) {

				let message = 'Update properties for ' + entity.name + '?\n\n';
				message += (counts.new > 0 ? 'Create ' + counts.new + ' properties.\n' : '');
				message += (counts.delete > 0 ? 'Delete ' + counts.delete + ' properties. (Note: Builtin properties will be restored in their initial configuration!)\n' : '');
				message += (counts.update > 0 ? 'Update ' + counts.update + ' properties.\n' : '');

				if (confirm(message)) {
					_Schema.showSchemaRecompileMessage();

					fetch(rootUrl + entity.id, {
						dataType: 'json',
						contentType: 'application/json; charset=utf-8',
						method: 'PUT',
						body: JSON.stringify({
							schemaProperties: schemaProperties
						})
					}).then((response) => {

						if (response.ok) {

							Command.get(entity.id, null, function(reloadedEntity) {
								el.empty();
								_Schema.properties.appendLocalProperties(el, reloadedEntity);
								_Schema.hideSchemaRecompileMessage();

								if (optionalAfterSaveCallback) {
									optionalAfterSaveCallback();
								}
							});

						} else {
							response.json().then((data) => {
								Structr.errorFromResponse(data, undefined, {requiresConfirmation: true});
							});
							_Schema.hideSchemaRecompileMessage();
						}
					});
				}
			}
		},
		bindRowEvents: function(property, row, overrides) {

			let propertyInfoChangeHandler = () => {
				_Schema.properties.rowChanged(property, row);
			};

			var protected = false;

			var propertyTypeOption = $('.property-type option[value="' + property.propertyType + '"]', row);
			if (propertyTypeOption) {
				propertyTypeOption.attr('selected', true);
				if (propertyTypeOption.data('protected')) {
					propertyTypeOption.prop('disabled', true);
					propertyTypeOption.closest('select').attr('disabled', true);
					protected = true;
				} else {
					propertyTypeOption.prop('disabled', null);
				}
			} else {
				console.log(property.propertyType, property);
			}

			var typeField = $('.property-type', row);
			$('.property-type option[value=""]', row).remove();

			if (property.propertyType === 'String' && !property.isBuiltinProperty) {
				if (!$('input.content-type', typeField.parent()).length) {
					typeField.after('<input type="text" size="5" class="content-type">');
				}
				$('.content-type', row).off('change').on('change', propertyInfoChangeHandler).prop('disabled', null);
			}

			$('.property-name',    row).off('change').on('change', propertyInfoChangeHandler).prop('disabled', protected);
			$('.property-dbname',  row).off('change').on('change', propertyInfoChangeHandler).prop('disabled', protected);
			$('.caching-enabled',  row).off('change').on('change', propertyInfoChangeHandler).prop('disabled', protected);
			$('.type-hint',        row).off('change').on('change', propertyInfoChangeHandler).prop('disabled', protected);
			$('.property-type',    row).off('change').on('change', propertyInfoChangeHandler).prop('disabled', protected);
			$('.property-format',  row).off('change').on('change', propertyInfoChangeHandler).prop('disabled', protected);
			$('.not-null',         row).off('change').on('change', propertyInfoChangeHandler).prop('disabled', protected);
			$('.compound',         row).off('change').on('change', propertyInfoChangeHandler).prop('disabled', protected);
			$('.unique',           row).off('change').on('change', propertyInfoChangeHandler).prop('disabled', protected);
			$('.indexed',          row).off('change').on('change', propertyInfoChangeHandler).prop('disabled', protected);
			$('.property-default', row).off('change').on('change', propertyInfoChangeHandler).prop('disabled', protected);

			$('.edit-read-function', row).off('click').on('click', function() {
				let unsavedChanges = _Schema.properties.hasUnsavedChanges(row.closest('table'));

				if (!unsavedChanges || confirm("Really switch to code editing? There are unsaved changes which will be lost!")) {

					if (overrides && overrides.editReadWriteFunction) {
						overrides.editReadWriteFunction(property);
					} else {
						_Schema.properties.openCodeEditorForFunctionProperty($(this), property.id, 'readFunction', function() { _Schema.openEditDialog(property.schemaNode.id, 'local'); });
					}
				}
			}).prop('disabled', protected);

			$('.edit-write-function', row).off('click').on('click', function() {
				let unsavedChanges = _Schema.properties.hasUnsavedChanges(row.closest('table'));

				if (!unsavedChanges || confirm("Really switch to code editing? There are unsaved changes which will be lost!")) {
					if (overrides && overrides.editReadWriteFunction) {
						overrides.editReadWriteFunction(property);
					} else {
						_Schema.properties.openCodeEditorForFunctionProperty($(this), property.id, 'writeFunction', function() { _Schema.openEditDialog(property.schemaNode.id, 'local'); });
					}
				}
			}).prop('disabled', protected);


			if (!protected) {

				$('.remove-action', row).off('click').on('click', function() {

					row.addClass('to-delete');
					propertyInfoChangeHandler();

				}).prop('disabled', null);

				$('.discard-changes', row).off('click').on('click', function() {

					_Schema.properties.setAttributesInRow(property, row);

					row.removeClass('to-delete');
					row.removeClass('has-changes');

					propertyInfoChangeHandler();

				}).prop('disabled', null);

			} else {
				$('.remove-action', row).hide();
			}
		},
		getInfoFromRow: function(tr) {

			let obj = {
				name:             $('.property-name', tr).val(),
				dbName:           $('.property-dbname', tr).val(),
				propertyType:     $('.property-type', tr).val(),
				contentType:      $('.content-type', tr).val(),
				format:           $('.property-format', tr).val(),
				notNull:          $('.not-null', tr).is(':checked'),
				compound:         $('.compound', tr).is(':checked'),
				unique:           $('.unique', tr).is(':checked'),
				indexed:          $('.indexed', tr).is(':checked'),
				defaultValue:     $('.property-default', tr).val(),
				isCachingEnabled: $('.caching-enabled', tr).is(':checked'),
				typeHint:         $('.type-hint', tr).val()
			};

			if (obj.typeHint === "null") {
				obj.typeHint = null;
			}

			return obj;
		},
		setAttributesInRow: function(property, tr) {

			$('.property-name', tr).val(property.name);
			$('.property-dbname', tr).val(property.dbName);
			$('.property-type', tr).val(property.propertyType);
			$('.content-type', tr).val(property.contentType);
			$('.property-format', tr).val(property.format);
			$('.not-null', tr).prop('checked', property.notNull);
			$('.compound', tr).prop('checked', property.compound);
			$('.unique', tr).prop('checked', property.unique);
			$('.indexed', tr).prop('checked', property.indexed);
			$('.property-default', tr).val(property.defaultValue);
			$('.caching-enabled', tr).prop('checked', property.isCachingEnabled);
			$('.type-hint', tr).val(property.typeHint || "null");

		},
		hasUnsavedChanges: function (table) {
			let tbody = $('tbody', table);
			return (tbody.find('tr.to-delete').length + tbody.find('tr.has-changes').length) > 0;
		},
		tableChanged: function (table) {

			let unsavedChanges = _Schema.properties.hasUnsavedChanges(table);

			let tfoot = table.find('tfoot');

			if (unsavedChanges) {
				tfoot.removeClass('hidden');
			} else {
				tfoot.addClass('hidden');
			}

		},
		rowChanged: function(property, row) {

			var propertyInfoUI = _Schema.properties.getInfoFromRow(row);
			let hasChanges     = false;

			for (let key in propertyInfoUI) {

				if ((propertyInfoUI[key] === '' || propertyInfoUI[key] === null || propertyInfoUI[key] === undefined) && (property[key] === '' || property[key] === null || property[key] === undefined)) {
					// account for different attribute-sets and fuzzy equality
				} else if (propertyInfoUI[key] !== property[key]) {
					hasChanges = true;
				}
			}

			if (hasChanges) {
				row.addClass('has-changes');
			} else {
				row.removeClass('has-changes');
			}

			_Schema.properties.tableChanged(row.closest('table'));
		},
		validateProperty: function (propertyDefinition, tr) {

			if (propertyDefinition.name.length === 0) {

				blinkRed($('.property-name', tr).closest('td'));
				return false;

			} else if (propertyDefinition.propertyType.length === 0) {

				blinkRed($('.property-type', tr).closest('td'));
				return false;

			} else if (propertyDefinition.propertyType === 'Enum' && propertyDefinition.format.trim().length === 0) {

				blinkRed($('.property-format', tr).closest('td'));
				return false;

			} else if (propertyDefinition.propertyType === 'Enum') {

				var containsSpace = propertyDefinition.format.split(',').some(function (enumVal) {
					return enumVal.trim().indexOf(' ') !== -1;
				});

				if (containsSpace) {
					blinkRed($('.property-format', tr).closest('td'));
					new MessageBuilder().warning('Enum values must be separated by commas and cannot contain spaces<br>See the <a href="' + Structr.getDocumentationURLForTopic('schema-enum') + '" target="_blank">support article on enum properties</a> for more information.').requiresConfirmation().show();
					return false;
				}
			}

			return true;
		},
		openCodeEditorForFunctionProperty: function(btn, id, key, callback) {

			dialogMeta.show();

			Command.get(id, 'id,name,contentType,' + key, function(entity) {

				var title = 'Edit ' + key + ' of ' + entity.name;

				Structr.dialog(title, function() {}, function() {});

				_Schema.properties.editFunctionPropertyCode(btn, entity, key, dialogText, function() {
					window.setTimeout(function() {
						callback();
					}, 250);
				});
			});
		},
		editFunctionPropertyCode: function(button, entity, key, element, callback) {

			var text = entity[key] || '';

			if (Structr.isButtonDisabled(button)) {
				return;
			}
			element.append('<div class="editor"></div>');
			var contentBox = $('.editor', element);
			contentType = contentType ? contentType : entity.contentType;
			var text1, text2;

			// Intitialize editor
			editor = CodeMirror(contentBox.get(0), Structr.getCodeMirrorSettings({
				value: text,
				mode: contentType,
				lineNumbers: true,
				lineWrapping: false,
				extraKeys: {
					"Ctrl-Space": _Contents.autoComplete
				},
				indentUnit: 4,
				tabSize:4,
				indentWithTabs: true
			}));
			_Code.setupAutocompletion(editor, entity.id);

			Structr.resize();

			dialogBtn.append('<button id="editorSave" disabled="disabled" class="disabled">Save</button>');
			dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');

			dialogSaveButton = $('#editorSave', dialogBtn);
			saveAndClose = $('#saveAndClose', dialogBtn);

			saveAndClose.off('click').on('click', function(e) {
				e.stopPropagation();
				dialogSaveButton.click();
				setTimeout(function() {
					dialogSaveButton.remove();
					saveAndClose.remove();
					dialogCancelButton.click();
				}, 500);
			});

			editor.on('change', function(cm, change) {

				let editorText = editor.getValue();

				if (text === editorText) {
					dialogSaveButton.prop("disabled", true).addClass('disabled');
					saveAndClose.prop("disabled", true).addClass('disabled');
				} else {
					dialogSaveButton.prop("disabled", false).removeClass('disabled');
					saveAndClose.prop("disabled", false).removeClass('disabled');
				}

				$('#chars').text(editorText.length);
				$('#words').text((editorText.match(/\S+/g) || []).length);
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

			dialogSaveButton.off('click').on('click', function(e) {
				e.stopPropagation();

				text1 = text;
				text2 = editor.getValue();

				if (!text1)
					text1 = '';
				if (!text2)
					text2 = '';

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

			dialogMeta.append('<span class="editor-info"><label for="lineWrapping">Line Wrapping:</label> <input id="lineWrapping" type="checkbox"' + (Structr.getCodeMirrorSettings().lineWrapping ? ' checked="checked" ' : '') + '></span>');
			$('#lineWrapping').off('change').on('change', function() {
				var inp = $(this);
				Structr.updateCodeMirrorOptionGlobally('lineWrapping', inp.is(':checked'));
				blinkGreen(inp.parent());
				editor.refresh();
			});

			dialogMeta.append('<span class="editor-info">Characters: <span id="chars">' + editor.getValue().length + '</span></span>');
			dialogMeta.append('<span class="editor-info">Words: <span id="chars">' + (editor.getValue().match(/\S+/g) ? editor.getValue().match(/\S+/g).length : 0) + '</span></span>');

			editor.id = entity.id;

			editor.focus();
		},
		appendBuiltinProperties: async function(el, entity) {

			let tableConfig = {
				class: 'builtin schema-props',
				cols: [
					{ class: '', title: 'Declaring Class' },
					{ class: '', title: 'JSON Name' },
					{ class: '', title: 'Type' },
					{ class: '', title: 'Notnull' },
					{ class: '', title: 'Comp.' },
					{ class: '', title: 'Uniq.' },
					{ class: '', title: 'Idx' }
				]
			};

			let html = await Structr.fetchHtmlTemplate('schema/schema-table', tableConfig);

			var propertiesTable = $(html);
			el.append(propertiesTable);

			_Schema.sort(entity.schemaProperties);

			Command.listSchemaProperties(entity.id, 'ui', function(data) {

				// sort by name
				_Schema.sort(data, "declaringClass", "name");

				$.each(data, async function(i, prop) {

					if (prop.declaringClass !== entity.name) {

						let property = {
							name: prop.name,
							propertyType: prop.propertyType,
							isBuiltinProperty: true,
							notNull: prop.notNull,
							compound: prop.compound,
							unique: prop.unique,
							indexed: prop.indexed,
							declaringClass: prop.declaringClass
						};

						let builtinProperty = await Structr.fetchHtmlTemplate('schema/property.builtin', {property: property});
						propertiesTable.append(builtinProperty);
					}
				});
			});
		},
	},
	remoteProperties: {
		cardinalityClasses: {
			'1': 'one',
			'*': 'many'
		},
		appendRemote: async function(el, entity, editSchemaObjectLinkHandler, optionalAfterSaveCallback) {

			let tableConfig = {
				class: 'related-attrs schema-props',
				cols: [
					{ class: '', title: 'JSON Name' },
					{ class: '', title: 'Type, Direction and Remote type' },
					{ class: 'actions-col', title: 'Action' }
				]
			};

			let html = await Structr.fetchHtmlTemplate('schema/schema-table', tableConfig);

			let tbl = $(html);

			let tbody = tbl.find('tbody');
			el.append(tbl);

			if (entity.relatedTo.length === 0 && entity.relatedFrom.length === 0) {
				tbody.append('<td colspan=3 class="no-rels">Type has no relationships...</td></tr>');
			} else {

				entity.relatedTo.forEach(function(target) {
					_Schema.remoteProperties.appendRemoteProperty(tbody, target, true, editSchemaObjectLinkHandler);
				});

				entity.relatedFrom.forEach(function(source) {
					_Schema.remoteProperties.appendRemoteProperty(tbody, source, false, editSchemaObjectLinkHandler);
				});
			}

			$('.discard-all', tbl).on('click', () => {
				tbl.find('i.discard-changes').click();
			});

			$('.save-all', tbl).on('click', () => {
				_Schema.remoteProperties.bulkSave(el, tbody, entity, editSchemaObjectLinkHandler, optionalAfterSaveCallback);
			});
		},
		bulkSave: function(el, tbody, entity, editSchemaObjectLinkHandler, optionalAfterSaveCallback) {

			if (!_Schema.remoteProperties.hasUnsavedChanges(tbody.closest('table'))) {
				return;
			}

			let allow = true;
			let counts = {
				update:0,
				reset:0
			};
			let payload = {
				relatedTo: [],
				relatedFrom: []
			};
			tbody.find('tr').each((i, tr) => {

				let row = $(tr);
				let info = { id: row.data('relationshipId') };

				info[row.data('propertyName')] = $('.property-name', row).val();
				if (info[row.data('propertyName')] === '') {
					info[row.data('propertyName')] = null;
				}

				if (row.hasClass('has-changes')) {
					allow = _Schema.remoteProperties.validate(row) && allow;

					if (info[row.data('propertyName')] === null) {
						counts.reset++;
					} else {
						counts.update++;
					}
				}

				payload[row.data('targetCollection')].push(info);
			});

			if (allow) {

				let message = 'Update remote attribute names for ' + entity.name + '?\n\n';
				message += (counts.update > 0 ? 'Update ' + counts.update + ' remote attribute names.\n' : '');
				message += (counts.reset > 0 ? 'Reset ' + counts.reset + ' remote attribute names.\n' : '');

				if (confirm(message)) {
					_Schema.showSchemaRecompileMessage();

					fetch(rootUrl + entity.id, {
						dataType: 'json',
						contentType: 'application/json; charset=utf-8',
						method: 'PUT',
						body: JSON.stringify(payload)
					}).then((response) => {

						if (response.ok) {

							Command.get(entity.id, null, function(reloadedEntity) {
								el.empty();
								_Schema.remoteProperties.appendRemote(el, reloadedEntity, editSchemaObjectLinkHandler);
								_Schema.hideSchemaRecompileMessage();

								if (optionalAfterSaveCallback) {
									optionalAfterSaveCallback();
								}
							});

						} else {
							_Schema.hideSchemaRecompileMessage();
							response.json().then((data) => {
								Structr.errorFromResponse(data, undefined, {requiresConfirmation: true});
							});
						}
					});
				}
			}
		},
		appendRemoteProperty: function(el, rel, out, editSchemaObjectLinkHandler) {

			let relType = (rel.relationshipType === undefinedRelType) ? '' : rel.relationshipType;
			let relatedNodeId = (out ? rel.targetId : rel.sourceId);
			let attributeName = (out ? (rel.targetJsonName || rel.oldTargetJsonName) : (rel.sourceJsonName || rel.oldSourceJsonName));

			let renderRemoteProperty = async (tplConfig) => {

				let html = await Structr.fetchHtmlTemplate('schema/remote-property', tplConfig);

				let row = $(html);
				el.append(row);

				$('.property-name', row).off('keyup').on('keyup', function() {
					_Schema.remoteProperties.rowChanged(row, attributeName);
				});

				$('.reset-action', row).off('click').on('click', function () {
					$('.property-name', row).val('');
					_Schema.remoteProperties.rowChanged(row, attributeName);
				});

				$('.discard-changes', row).off('click').on('click', function () {
					$('.property-name', row).val(attributeName);
					_Schema.remoteProperties.rowChanged(row, attributeName);
				});

				if (!editSchemaObjectLinkHandler) {
					$('.edit-schema-object', row).removeClass('edit-schema-object');
				} else {

					$('.edit-schema-object', row).off('click').on('click', function(e) {
						e.stopPropagation();

						let unsavedChanges = _Schema.remoteProperties.hasUnsavedChanges(row.closest('table'));

						if (!unsavedChanges || confirm("Really switch to other type? There are unsaved changes which will be lost!")) {
							editSchemaObjectLinkHandler($(this));
						}

						return false;
					});
				}
			};

			let tplConfig = {
				rel: rel,
				relType: relType,
				propertyName: (out ? 'targetJsonName' : 'sourceJsonName'),
				targetCollection: (out ? 'relatedTo' : 'relatedFrom'),
				attributeName: attributeName,
				arrowLeft: (out ? '' : '&#9668;'),
				arrowRight: (out ? '&#9658;' : ''),
				cardinalityClassLeft: _Schema.remoteProperties.cardinalityClasses[(out ? rel.sourceMultiplicity : rel.targetMultiplicity)],
				cardinalityClassRight: _Schema.remoteProperties.cardinalityClasses[(out ? rel.targetMultiplicity : rel.sourceMultiplicity)],
				relatedNodeId: relatedNodeId
			};

			if (!nodes[relatedNodeId]) {
				Command.get(relatedNodeId, 'name', (data) => {
					tplConfig.relatedNodeType = data.name;
					renderRemoteProperty(tplConfig);
				});
			} else {
				tplConfig.relatedNodeType = nodes[relatedNodeId].name;
				renderRemoteProperty(tplConfig);
			}
		},
		hasUnsavedChanges: function (table) {
			let tbody = $('tbody', table);
			return (tbody.find('tr.has-changes').length) > 0;
		},
		tableChanged: function (table) {

			let unsavedChanges = _Schema.remoteProperties.hasUnsavedChanges(table);

			let tfoot = table.find('tfoot');

			if (unsavedChanges) {
				tfoot.removeClass('hidden');
			} else {
				tfoot.addClass('hidden');
			}

		},
		rowChanged: function(row, originalName) {

			let nameInUI = $('.property-name', row).val();
			let hasChanges = (nameInUI !== originalName);

			if (hasChanges) {
				row.addClass('has-changes');
			} else {
				row.removeClass('has-changes');
			}

			_Schema.remoteProperties.tableChanged(row.closest('table'));
		},
		validate: function(row) {
			return true;
		}
	},
	views: {
		appendViews: async function(el, entity, optionalAfterSaveCallback) {

			let tableConfig = {
				class: 'related-attrs schema-props',
				cols: [
					{ class: '', title: 'Name' },
					{ class: '', title: 'Attributes' },
					{ class: 'actions-col', title: 'Action' }
				]
			};

			let html = await Structr.fetchHtmlTemplate('schema/schema-table', tableConfig);

			let viewsTable = $(html);

			el.append(viewsTable);
			el.append('<i title="Add view" class="add-icon add-view ' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" />');

			let tbody = viewsTable.find('tbody');

			_Schema.sort(entity.schemaViews);

			$.each(entity.schemaViews, function(i, view) {
				_Schema.views.appendView(tbody, view, entity);
			});

			$('.discard-all', viewsTable).on('click', () => {
				tbody.find('i.discard-changes').click();
			});

			$('.save-all', viewsTable).on('click', () => {
				_Schema.views.bulkSave(el, tbody, entity, optionalAfterSaveCallback);
			});

			$('.add-view', el).off('click').on('click', async function() {

				let html = await Structr.fetchHtmlTemplate('schema/view.new', {});

				let row = $(html);
				tbody.append(row);

				_Schema.views.tableChanged(viewsTable);

				_Schema.views.appendViewSelectionElement(row, {name: 'new'}, entity, function (chznElement) {

					chznElement.chosenSortable();
				});

				$('.discard-changes', row).off('click').on('click', function() {
					 row.remove();
					_Schema.views.tableChanged(viewsTable);
				});
			});
		},
		bulkSave: function(el, tbody, entity, optionalAfterSaveCallback) {

			if (!_Schema.views.hasUnsavedChanges(tbody.closest('table'))) {
				return;
			}

			let schemaViews = [];
			let allow = true;
			let counts = {
				update: 0,
				delete: 0,
				new: 0
			};

			tbody.find('tr').each((i, tr) => {

				let row    = $(tr);
				let viewId = row.data('viewId');
				let view   = _Schema.views.getInfoFromRow(row, entity);

				if (viewId) {
					if (row.hasClass('to-delete')) {
						// do not add this property to the list
						counts.delete++;
					} else if (row.hasClass('has-changes')) {
						// changed lines
						counts.update++;
						view.id = viewId;
						allow = _Schema.views.validateViewRow(row) && allow;
						schemaViews.push(view);
					} else {
						// unchanged lines, only transmit id
						view = { id: viewId };
						schemaViews.push(view);
					}

				} else {
					//new lines
					counts.new++;
					view.type = 'SchemaView';
					allow = _Schema.views.validateViewRow(row) && allow;
					schemaViews.push(view);
				}
			});

			if (allow) {

				let message = 'Update views for ' + entity.name + '?\n\n';
				message += (counts.new > 0 ? 'Create ' + counts.new + ' views.\n' : '');
				message += (counts.delete > 0 ? 'Delete ' + counts.delete + ' views. (Note: Builtin views will be restored in their initial configuration!)\n' : '');
				message += (counts.update > 0 ? 'Update ' + counts.update + ' views.\n' : '');

				if (confirm(message)) {

					_Schema.showSchemaRecompileMessage();

					fetch(rootUrl + entity.id, {
						dataType: 'json',
						contentType: 'application/json; charset=utf-8',
						method: 'PUT',
						body: JSON.stringify({
							schemaViews: schemaViews
						})
					}).then((response) => {

						if (response.ok) {

							Command.get(entity.id, null, function(reloadedEntity) {
								el.empty();
								_Schema.views.appendViews(el, reloadedEntity);
								_Schema.hideSchemaRecompileMessage();

								if (optionalAfterSaveCallback) {
									optionalAfterSaveCallback();
								}
							});

						} else {
							response.json().then((data) => {
								Structr.errorFromResponse(data, undefined, {requiresConfirmation: true});
							});
							_Schema.hideSchemaRecompileMessage();
						}
					});
				}
			}
		},
		appendView: async function(el, view, entity) {

			let html = await Structr.fetchHtmlTemplate('schema/view', {view: view, type: entity});
			var row = $(html);
			el.append(row);

			_Schema.views.appendViewSelectionElement(row, view, entity, function (chznElement) {

				// store initial configuration for later comparison
				let initialViewConfig = _Schema.views.getInfoFromRow(row, entity);

				chznElement.chosenSortable(function() {
					// sort order changed
					_Schema.views.rowChanged(row, entity, initialViewConfig);
				});

				_Schema.views.bindRowEvents(row, entity, view, initialViewConfig);
			});

		},
		bindRowEvents: function(row, entity, view, initialViewConfig) {

			let viewInfoChangeHandler = (event, params) => {
				_Schema.views.rowChanged(row, entity, initialViewConfig);
			};

			$('.view.property-name', row).off('change').on('change', viewInfoChangeHandler);
			$('.view.property-attrs', row).off('change').on('change', viewInfoChangeHandler);

			$('.discard-changes', row).off('click').on('click', function() {

				var select = $('select', row);

				$('.view.property-name', row).val(view.name);

				Command.listSchemaProperties(entity.id, view.name, function(data) {

					data.forEach(function(prop) {
						$('option[value="' + prop.name + '"]', select).prop('selected', prop.isSelected);
					});

					select.trigger('chosen:updated');

					row.removeClass('to-delete');
					row.removeClass('has-changes');

					_Schema.views.tableChanged(row.closest('table'));
				});
			});

			$('.remove-action', row).off('click').on('click', function() {

				row.addClass('to-delete');
				viewInfoChangeHandler();

			}).prop('disabled', null);
		},
		appendViewSelectionElement: function(row, view, schemaEntity, callback) {

			let propertySelectTd = $('.view-properties-select', row).last();
			propertySelectTd.append('<select class="property-attrs view chosen-sortable" multiple="multiple"></select>');
			let viewSelectElem = $('.property-attrs', propertySelectTd);

			Command.listSchemaProperties(schemaEntity.id, view.name, function(properties) {

				let appendProperty = function(prop) {
					let name       = prop.name;
					let isSelected = prop.isSelected ? ' selected="selected"' : '';
					let isDisabled = (view.name === 'ui' || view.name === 'custom' || prop.isDisabled) ? ' disabled="disabled"' : '';

					viewSelectElem.append('<option value="' + name + '"' + isSelected + isDisabled + '>' + name + '</option>');
				};

				if (view.sortOrder) {
					view.sortOrder.split(',').forEach(function(sortedProp) {

						let prop = properties.filter(function(prop) {
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

				let chzn = viewSelectElem.chosen({
					search_contains: true,
					width: '100%',
					display_selected_options: false,
					hide_results_on_select: false,
					display_disabled_options: false
				});

				callback(chzn);
			});
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
		hasUnsavedChanges: function (table) {
			let tbody = $('tbody', table);
			return (tbody.find('tr.to-delete').length + tbody.find('tr.has-changes').length) > 0;
		},
		tableChanged: function (table) {

			let unsavedChanges = _Schema.views.hasUnsavedChanges(table);

			let tfoot = table.find('tfoot');

			if (unsavedChanges) {
				tfoot.removeClass('hidden');
			} else {
				tfoot.addClass('hidden');
			}

		},
		getInfoFromRow:function(row, schemaNodeEntity) {

			let sortedAttrs = $('.view.property-attrs', row).sortedVals();

			return {
				name: $('.view.property-name', row).val(),
				schemaProperties: _Schema.views.findSchemaPropertiesByNodeAndName(schemaNodeEntity, sortedAttrs),
				nonGraphProperties: _Schema.views.findNonGraphProperties(schemaNodeEntity, sortedAttrs),
				sortOrder: sortedAttrs.join(',')
			};
		},
		rowChanged: function(row, entity, initialViewConfig) {

			var viewInfoInUI = _Schema.views.getInfoFromRow(row, entity);
			let hasChanges   = false;

			for (let key in viewInfoInUI) {

				if (key === 'schemaProperties') {

					let origSchemaProps = initialViewConfig[key].map(p => p.id).sort().join(',');
					let uiSchemaProps = viewInfoInUI[key].map(p => p.id).sort().join(',');

					if (origSchemaProps !== uiSchemaProps) {
						hasChanges = true;
					}

				} else if (viewInfoInUI[key] !== initialViewConfig[key]) {
					hasChanges = true;
				}
			}

			if (hasChanges) {
				row.addClass('has-changes');
			} else {
				row.removeClass('has-changes');
			}

			_Schema.views.tableChanged(row.closest('table'));
		},
		validateViewRow: function (row) {

			if ($('.property-name', row).val().length === 0) {

				blinkRed($('.property-name', row).closest('td'));
				return false;
			}

			if ($('.view.property-attrs', row).sortedVals().length === 0) {

				blinkRed($('.view.property-attrs', row).closest('td'));
				return false;
			}

			return true;
		},
	},
	methods: {
		methodsData: {},
		appendMethods: async function(el, entity, methods, optionalAfterSaveCallback) {

			_Schema.methods.methodsData = {};

			let methodsHtml = await Structr.fetchHtmlTemplate('schema/methods', { class: entity ? 'entity' : 'global' });

			el.append(methodsHtml);

			let tableConfig = {
				class: 'actions schema-props',
				cols: [
					{ class: '', title: 'Name' },
					{ class: 'isstatic-col', title: 'isStatic' },
					{ class: 'actions-col', title: 'Action' }
				]
			};

			let methodsTbl = await Structr.fetchHtmlTemplate('schema/schema-fake-table', tableConfig);

			let methodsFakeTable = $(methodsTbl);
			$('#methods-table-container', el).append(methodsFakeTable);

			let fakeTbody = methodsFakeTable.find('.fake-tbody');

			_Schema.methods.activateUIActions(el, fakeTbody, entity);

			_Schema.sort(methods);

			let isFirst = true;

			methods.forEach(async function(method) {

				let methodHtml = await Structr.fetchHtmlTemplate('schema/method', { method: method });

				let fakeRow = $(methodHtml);
				fakeTbody.append(fakeRow);

				fakeRow.data('type-name', (entity ? entity.name : 'global_schema_method')).data('method-name', method.name);
				$('.property-name', fakeRow).val(method.name);
				$('.property-isStatic', fakeRow).prop('checked', method.isStatic);

				_Schema.methods.methodsData[method.id] = {
					isNew: false,
					id: method.id,
					name: method.name,
					isStatic: method.isStatic,
					source: method.source || '',
					comment: method.comment || '',
					initialName: method.name,
					initialisStatic: method.isStatic,
					initialSource: method.source || '',
					initialComment: method.comment || ''
				};

				_Schema.methods.bindRowEvents(fakeRow, entity, method);

				// auto-edit first method (or last used)
				if (isFirst|| (_Schema.methods.lastEditedMethod && ((_Schema.methods.lastEditedMethod.isNew === false && _Schema.methods.lastEditedMethod.id === method.id) || (_Schema.methods.lastEditedMethod.isNew === true && _Schema.methods.lastEditedMethod.name === method.name)))) {
					isFirst = false;
					$('.edit-action', fakeRow).click();
				}
			});

			$('.discard-all', methodsFakeTable).on('click', () => {
				methodsFakeTable.find('i.discard-changes').click();
			});

			$('.save-all', methodsFakeTable).on('click', () => {
				_Schema.methods.bulkSave(el, fakeTbody, entity, optionalAfterSaveCallback);
			});

			$('#methods-container-right', el).append('<div class="editor-settings"><span><label for="lineWrapping">Line Wrapping:</label> <input id="lineWrapping" type="checkbox"' + (Structr.getCodeMirrorSettings().lineWrapping ? ' checked="checked" ' : '') + '></span></div>');
			$('#lineWrapping', el).off('change').on('change', function() {
				let checkbox = $(this);
				Structr.updateCodeMirrorOptionGlobally('lineWrapping', checkbox.is(':checked'));
				blinkGreen(checkbox.parent());
			});
		},
		bulkSave: function(el, fakeTbody, entity, optionalAfterSaveCallback) {

			if (!_Schema.methods.hasUnsavedChanges(fakeTbody.closest('.fake-table'))) {
				return;
			}

			let methods = [];
			let allow = true;
			let counts = {
				update: 0,
				delete: 0,
				new: 0
			};

			fakeTbody.find('.fake-tr').each((i, tr) => {

				let row        = $(tr);
				let methodId   = row.data('methodId');
				let methodData = _Schema.methods.methodsData[methodId];

				if (methodData.isNew === false) {
					if (row.hasClass('to-delete')) {
						counts.delete++;
						methods.push({
							id: methodId,
							deleteMethod: true
						});
					} else if (row.hasClass('has-changes')) {
						// changed lines
						counts.update++;
						methods.push({
							id:       methodId,
							name:     methodData.name,
							isStatic: methodData.isStatic,
							source:   methodData.source,
							comment:  methodData.comment
						});
						allow = _Schema.methods.validateMethodRow(row) && allow;
					} else {
						if (entity) {

							// unchanged lines, only transmit id
							methods.push({
								id: methodId
							});
						}
					}

				} else {
					//new lines
					counts.new++;
					allow = _Schema.methods.validateMethodRow(row) && allow;
					let method = {
						type:     'SchemaMethod',
						name:     methodData.name,
						isStatic: methodData.isStatic,
						source:   methodData.source,
						comment:  methodData.comment
					};

					methods.push(method);
				}
			});

			if (allow) {

				let message = (entity ? 'Update methods for ' + entity.name + '?\n\n' : 'Update global methods?\n\n');
				message += (counts.new > 0 ? 'Create ' + counts.new + ' methods.\n' : '');
				message += (counts.delete > 0 ? 'Delete ' + counts.delete + ' methods.' + (entity ? '(Note: Builtin methods will be restored in their initial configuration!)\n' : '\n') : '');
				message += (counts.update > 0 ? 'Update ' + counts.update + ' methods.\n' : '');

				if (confirm(message)) {

					let activeMethod = fakeTbody.find('.fake-tr.editing');
					if (activeMethod) {
						_Schema.methods.lastEditedMethod = _Schema.methods.methodsData[activeMethod.data('methodId')];
					} else {
						_Schema.methods.lastEditedMethod = undefined;
					}

					_Schema.showSchemaRecompileMessage();

					let url = rootUrl + ((entity) ? entity.id : 'SchemaMethod');
					let method = (entity) ? 'PUT' : 'PATCH';
					let body = (entity) ? { schemaMethods: methods } : methods;

					fetch(url, {
						dataType: 'json',
						contentType: 'application/json; charset=utf-8',
						method: method,
						body: JSON.stringify(body)
					}).then((response) => {

						if (response.ok) {

							if (entity) {
								Command.get(entity.id, null, function(reloadedEntity) {
									el.empty();
									_Schema.methods.appendMethods(el, reloadedEntity, _Schema.filterJavaMethods(reloadedEntity.schemaMethods));
									_Schema.hideSchemaRecompileMessage();

									if (optionalAfterSaveCallback) {
										optionalAfterSaveCallback();
									}
								});
							} else {
								Command.rest('SchemaMethod?schemaNode=null&sort=name&order=ascending', function (methods) {
									el.empty();
									_Schema.methods.appendMethods(el, null, _Schema.filterJavaMethods(methods));
									_Schema.hideSchemaRecompileMessage();

									if (optionalAfterSaveCallback) {
										optionalAfterSaveCallback();
									}
								});
							}

						} else {
							response.json().then((data) => {
								Structr.errorFromResponse(data, undefined, {requiresConfirmation: true});
							});
							_Schema.hideSchemaRecompileMessage();
						}
					});
				}
			}
		},
		activateUIActions: function(el, fakeTbody, entity) {

			let addedMethodsCounter = 1;

			let getNewMethodTemplateConfig = function(name) {
				return {
					name: _Schema.methods.getFirstFreeMethodName(name),
					methodId: 'new' + (addedMethodsCounter++)
				};
			};

			$('.add-action-button', el).off('click').on('click', function() {
				_Schema.methods.appendEmptyMethod(fakeTbody, getNewMethodTemplateConfig(''));
			});

			if (entity) {

				$('.add-onCreate-button', el).off('click').on('click', function() {
					_Schema.methods.appendEmptyMethod(fakeTbody, getNewMethodTemplateConfig('onCreate'));
				});

				$('.add-afterCreate-button', el).off('click').on('click', function() {
					_Schema.methods.appendEmptyMethod(fakeTbody, getNewMethodTemplateConfig('afterCreate'));
				});

				Structr.appendInfoTextToElement({
					text: "The difference between onCreate an afterCreate is that afterCreate is called after all checks have run and the transaction is committed.<br>Example: There is a unique constraint and you want to send an email when an object is created.<br>Calling 'send_html_mail()' in onCreate would send the email even if the transaction would be rolled back due to an error. The appropriate place for this would be afterCreate.",
					element: $('.add-afterCreate-button', el),
					insertAfter: true
				});

				$('.add-onSave-button', el).off('click').on('click', function() {
					_Schema.methods.appendEmptyMethod(fakeTbody, getNewMethodTemplateConfig('onSave'));
				});
			}

			let contentDiv = $('#methods-container-right', el);

			let activateTab = function(tabName) {
				$('.method-tab-content', contentDiv).hide();
				$('li[data-name]', contentDiv).removeClass('active');
				$('#tabView-' + tabName, contentDiv).show();
				$('li[data-name="' + tabName + '"]', contentDiv).addClass('active');

				if (_Schema.methods.cm && _Schema.methods.cm[tabName]) {
					_Schema.methods.cm[tabName].refresh();
				}
			};

			$('li[data-name]', contentDiv).off('click').on('click', function(e) {
				e.stopPropagation();
				activateTab($(this).data('name'));
			});
			activateTab('source');
			contentDiv.hide();
		},
		refreshEditors: function() {
			if (_Schema.methods.cm) {

				if (_Schema.methods.cm.source) {
					_Schema.methods.cm.source.refresh();
				}

				if (_Schema.methods.cm.comment) {
					_Schema.methods.cm.comment.refresh();
				}
			}
		},
		appendEmptyMethod: async function(fakeTbody, tplConfig) {

			let html = await Structr.fetchHtmlTemplate('schema/method.new', tplConfig);

			let row = $(html);
			fakeTbody.append(row);

			fakeTbody.scrollTop(row.position().top);

			_Schema.methods.methodsData[tplConfig.methodId] = {
				isNew: true,
				name: tplConfig.name,
				isStatic: false,
				source: '',
				comment: ''
			};

			$('.property-name', row).off('keyup').on('keyup', function() {
				_Schema.methods.methodsData[tplConfig.methodId].name = $(this).val();
			});

			$('.property-isStatic', row).off('change').on('change', function() {
				_Schema.methods.methodsData[tplConfig.methodId].isStatic = $(this).prop('checked');
			});

			$('.edit-action', row).off('click').on('click', function() {
				_Schema.methods.editMethod(row);
			});

			$('.discard-changes', row).off('click').on('click', function() {
				if (row.hasClass('editing')) {
					$('#methods-container-right').hide();
				}
				row.remove();
				_Schema.methods.fakeTableChanged(fakeTbody.closest('.fake-table'));
			});

			_Schema.methods.fakeTableChanged(fakeTbody.closest('.fake-table'));

			_Schema.methods.editMethod(row);
		},
		getFirstFreeMethodName: function(prefix) {

			if (!prefix) {
				return '';
			}

			let nextSuffix = 0;

			for (let key in _Schema.methods.methodsData) {

				let name = _Schema.methods.methodsData[key].name;
				if (name.indexOf(prefix) === 0) {
					let suffix = name.slice(prefix.length);

					if (suffix === '') {
						nextSuffix = Math.max(nextSuffix, 1);
					} else {
						let parsed = parseInt(suffix);
						if (!isNaN(parsed)) {
							nextSuffix = Math.max(nextSuffix, parsed + 1);
						}
					}
				}
			}

			return prefix + (nextSuffix === 0 ? '' : (nextSuffix < 10 ? '0' + nextSuffix : nextSuffix));
		},
		bindRowEvents: function(row, entity, method) {

			let methodId = row.data('methodId');
			let methodData = _Schema.methods.methodsData[methodId];

			$('.property-name', row).off('keyup').on('keyup', function() {
				methodData.name = $(this).val();
				_Schema.methods.rowChanged(row, (methodData.name !== methodData.initialName));
			});

			$('.property-isStatic', row).off('change').on('change', function() {
				methodData.isStatic = $(this).prop('checked');
				_Schema.methods.rowChanged(row, (methodData.isStatic !== methodData.initialisStatic));
			});

			$('.edit-action', row).off('click').on('click', function() {
				_Schema.methods.editMethod(row);
			});

			$('.remove-action', row).off('click').on('click', function() {
				row.addClass('to-delete');
				_Schema.methods.rowChanged(row, true);
			});

			$('.discard-changes', row).off('click').on('click', function() {

				if (row.hasClass('to-delete') || row.hasClass('has-changes')) {

					row.removeClass('to-delete');
					row.removeClass('has-changes');

					methodData.name     = methodData.initialName;
					methodData.isStatic = methodData.initialisStatic;
					methodData.source   = methodData.initialSource;
					methodData.comment  = methodData.initialComment;

					$('.property-name', row).val(methodData.name);
					$('.property-isStatic', row).prop('checked', methodData.isStatic);

					if (row.hasClass('editing')) {
						_Schema.methods.editMethod(row);
					}

					_Schema.methods.rowChanged(row, false);
				}
			});
		},
		editMethod: function(row) {

			let contentDiv = $('#methods-container-right').show();

			row.closest('.fake-tbody').find('.fake-tr').removeClass('editing');
			row.addClass('editing');

			let methodId = row.data('methodId');
			let methodData = _Schema.methods.methodsData[methodId];

			if (!_Schema.methods.cm) {
				_Schema.methods.cm = {};
			} else {
				_Schema.methods.cm.source.toTextArea();
				_Schema.methods.cm.comment.toTextArea();
			}

			let sourceTextarea = $('textarea.property-code', contentDiv);

			_Schema.methods.cm.source = CodeMirror.fromTextArea(sourceTextarea[0], Structr.getCodeMirrorSettings({
				lineNumbers: true,
				lineWrapping: false,
				extraKeys: {
					"'.'":        _Contents.autoComplete,
					"Ctrl-Space": _Contents.autoComplete
				},
				indentUnit: 4,
				tabSize: 4,
				indentWithTabs: true
			}));
			$(_Schema.methods.cm.source.getWrapperElement()).addClass('cm-schema-methods');
			_Schema.methods.cm.source.setValue(methodData.source);
			_Schema.methods.cm.source.setOption('mode', _Schema.methods.senseCodeMirrorMode(methodData.source));
			_Schema.methods.cm.source.refresh();
			_Schema.methods.cm.source.clearHistory();
			_Schema.methods.cm.source.on('change', function (cm, changeset) {
				cm.save();
				cm.setOption('mode', _Schema.methods.senseCodeMirrorMode(cm.getValue()));
				$(cm.getTextArea()).trigger('change');

				methodData.source = cm.getValue();

				_Schema.methods.rowChanged(row, (methodData.source !== methodData.initialSource));
			});

			_Code.setupAutocompletion(_Schema.methods.cm.source, methodId, true);

			let commentTextarea = $('textarea.property-comment', contentDiv);

			_Schema.methods.cm.comment = CodeMirror.fromTextArea(commentTextarea[0], Structr.getCodeMirrorSettings({
				lineNumbers: true,
				lineWrapping: false,
				indentUnit: 4,
				tabSize: 4,
				indentWithTabs: true
			}));
			$(_Schema.methods.cm.comment.getWrapperElement()).addClass('cm-schema-methods');
			_Schema.methods.cm.comment.setValue(methodData.comment);
			_Schema.methods.cm.comment.refresh();
			_Schema.methods.cm.comment.clearHistory();
			_Schema.methods.cm.comment.on('change', function (cm, changeset) {
				cm.save();
				$(cm.getTextArea()).trigger('change');

				methodData.comment = cm.getValue();

				_Schema.methods.rowChanged(row, (methodData.comment !== methodData.initialComment));
			});
		},
		senseCodeMirrorMode: function(content) {
			return (content && content.indexOf('{') === 0) ? 'text/javascript' : 'text';
		},
		showGlobalSchemaMethods: function () {

			Command.rest('SchemaMethod?schemaNode=null&sort=name&order=ascending', function (methods) {

				Structr.dialog('Global Schema Methods', function() {
					dialogMeta.show();
				}, function() {
					_Schema.currentNodeDialogId = null;

					dialogMeta.show();
					instance.repaintEverything();
				}, ['schema-edit-dialog', 'global-methods-dialog']);

				dialogMeta.hide();

				var contentEl = dialogText;

				var contentDiv = $('<div id="tabView-methods" class="schema-details"></div>');
				var outerDiv = $('<div class="schema-details"></div>');
				outerDiv.append(contentDiv);
				contentEl.append(outerDiv);

				_Schema.methods.appendMethods(contentDiv, null, methods);
			});
		},
		hasUnsavedChanges: function (fakeTable) {
			let fakeTbody = $('.fake-tbody', fakeTable);
			return (fakeTbody.find('.fake-tr.to-delete').length + fakeTbody.find('.fake-tr.has-changes').length) > 0;
		},
		fakeTableChanged: function (fakeTable) {

			let unsavedChanges = _Schema.methods.hasUnsavedChanges(fakeTable);

			let footer = fakeTable.find('.fake-tfoot');

			if (unsavedChanges) {
				footer.removeClass('hidden');
			} else {
				footer.addClass('hidden');
			}
		},
		rowChanged: function(row, hasChanges) {

			if (hasChanges) {
				row.addClass('has-changes');
			} else {
				row.removeClass('has-changes');
			}

			_Schema.methods.fakeTableChanged(row.closest('.fake-table'));
		},
		validateMethodRow: function (row) {

			if ($('.property-name', row).val().length === 0) {

				blinkRed($('.property-name', row).closest('.fake-td'));
				return false;
			}

			return true;
		},
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

			$('body').css({
				position: 'relative'
			});

			$('html').css({
				background: '#fff'
			});
		}

		$('body').css({
			position: 'relative'
		});

	},
	removeSchemaEntity: function(entity, onSuccess, onError) {

		if (entity && entity.id) {

			_Schema.showSchemaRecompileMessage();

			$.ajax({
				url: rootUrl + entity.id,
				type: 'DELETE',
				dataType: 'json',
				contentType: 'application/json; charset=utf-8',
				statusCode: {
					200: function() {
						_Schema.reload();
						_Schema.hideSchemaRecompileMessage();
						if (onSuccess) {
							onSuccess();
						}
					},
					422: function(data) {
						_Schema.hideSchemaRecompileMessage();
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

							_Schema.showSchemaRecompileMessage();

							$.ajax({
								url: rootUrl + entity.id,
								type: 'PUT',
								dataType: 'json',
								contentType: 'application/json; charset=utf-8',
								data: JSON.stringify(obj),
								statusCode: {
									200: function() {
										_Schema.reload();
										_Schema.hideSchemaRecompileMessage();
										if (onSuccess) {
											onSuccess();
										}
									},
									422: function(data) {
										_Schema.hideSchemaRecompileMessage();
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

			_Schema.showSchemaRecompileMessage();

			$.ajax({
				url: rootUrl + resource,
				type: 'POST',
				dataType: 'json',
				contentType: 'application/json; charset=utf-8',
				data: JSON.stringify(obj),
				statusCode: {
					201: function(result) {
						_Schema.hideSchemaRecompileMessage();
						if (onSuccess) {
							onSuccess(result);
						}
					},
					422: function(data) {
						_Schema.hideSchemaRecompileMessage();
						if (onError) {
							onError(data);
						}
					}
				}
			});
		}
	},
	createNode: function(type) {

		_Schema.showSchemaRecompileMessage();

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
					_Schema.hideSchemaRecompileMessage();
				},
				422: function(data) {
					_Schema.hideSchemaRecompileMessage();
					Structr.errorFromResponse(data.responseJSON, undefined, {requiresConfirmation: true});
				}
			}
		});
	},
	deleteNode: function(id) {

		_Schema.showSchemaRecompileMessage();

		var url = rootUrl + 'schema_nodes/' + id;
		$.ajax({
			url: url,
			type: 'DELETE',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function() {
					_Schema.reload();
					_Schema.hideSchemaRecompileMessage();
				},
				422: function(data) {
					_Schema.hideSchemaRecompileMessage();
					Structr.errorFromResponse(data.responseJSON);
				}
			}
		});
	},
	createRelationshipDefinition: function(data, onSuccess, onError) {

		_Schema.showSchemaRecompileMessage();

		$.ajax({
			url: rootUrl + 'schema_relationship_nodes',
			type: 'POST',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			data: JSON.stringify(data),
			statusCode: {
				201: function(data) {

					_Schema.hideSchemaRecompileMessage();

					if (onSuccess) {
						onSuccess(data);
					}
				},
				422: function(data) {

					_Schema.hideSchemaRecompileMessage();

					if (onError) {
						onError(data);
					}
				}
			}
		});
	},
	removeRelationshipDefinition: function(id) {

		_Schema.showSchemaRecompileMessage();

		$.ajax({
			url: rootUrl + 'schema_relationship_nodes/' + id,
			type: 'DELETE',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function(data, textStatus, jqXHR) {
					_Schema.reload();
					_Schema.hideSchemaRecompileMessage();
				},
				422: function(data) {
					_Schema.hideSchemaRecompileMessage();
					Structr.errorFromResponse(data.responseJSON);
				}
			}
		});
	},
	updateRelationship: function(entity, newData, onSuccess, onError, onNoChange) {

		_Schema.showSchemaRecompileMessage();

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
									_Schema.hideSchemaRecompileMessage();
									_Schema.reload();
								},
								422: function(data) {
									_Schema.hideSchemaRecompileMessage();
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
						_Schema.hideSchemaRecompileMessage();
					}
				}
			}
		});
	},
	askDeleteRelationship: function (resId, name) {
		name = name ? ' \'' + name + '\'' : '';
		Structr.confirmation('<h3>Delete schema relationship' + name + '?</h3>',
				function() {
					$.unblockUI({
						fadeOut: 25
					});
					_Schema.detach(resId);
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
		let saving = false;
		input.off('blur').on('blur', function() {
			if (!id) {
				return false;
			}
			if (!saving) {
				saving = true;
				Command.get(id, 'id', function(entity) {
					_Schema.changeAttr(entity, element, input, key, oldVal, isRel);
				});
			}

			return false;
		});

		input.keypress(function(e) {
			if (e.keyCode === 13 || e.keyCode === 9) {
				e.preventDefault();
				if (!id) {
					return false;
				}
				if (!saving) {
					saving = true;
					Command.get(id, 'id', function(entity) {
						_Schema.changeAttr(entity, element, input, key, oldVal, isRel);
					});
				}
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
	appendSnapshotsDialogToContainer: async function(container) {

		let html = await Structr.fetchHtmlTemplate('schema/snapshots', {});

		container.append(html);

		var table = $('#snapshots');

		var refresh = function() {

			table.empty();

			Command.snapshots('list', '', null, function(result) {

				result.forEach(function(data) {

					var snapshots = data.snapshots;

					snapshots.forEach(function(snapshot, i) {
						table.append('<tr><td class="snapshot-link name-' + i + '"><a href="#">' + snapshot + '</td><td style="text-align:right;"><button id="restore-' + i + '">Restore</button><button id="add-' + i + '">Add</button><button id="delete-' + i + '">Delete</button></td></tr>');

						$('.name-' + i + ' a').off('click').on('click', function() {
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

						$('#restore-' + i).off('click').on('click', function() {
							_Schema.performSnapshotAction('restore', snapshot);
						});
						$('#add-' + i).off('click').on('click', function() {
							_Schema.performSnapshotAction('add', snapshot);
						});
						$('#delete-' + i).off('click').on('click', function() {
							Command.snapshots('delete', snapshot, null, refresh);
						});
					});
				});
			});
		};

		$('#create-snapshot').off('click').on('click', function() {

			var suffix = $('#snapshot-suffix').val();

			var types = [];
			if (_Schema.ui.selectedNodes && _Schema.ui.selectedNodes.length) {
				_Schema.ui.selectedNodes.forEach(function(selectedNode) {
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

		$('#refresh-snapshots').off('click').on('click', refresh);
		refresh();

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
	appendAdminToolsToContainer: async function(container) {

		let html = await Structr.fetchHtmlTemplate('schema/admin-tools', {});

		container.append(html);

		var registerSchemaToolButtonAction = function (btn, target, connectedSelectElement, getPayloadFunction) {

			btn.off('click').on('click', function(e) {
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

		$('#clear-schema').off('click').on('click', function(e) {
			Structr.confirmation('<h3>Delete schema?</h3><p>This will remove all dynamic schema information, but not your other data.</p><p>&nbsp;</p>', function() {
				$.unblockUI({
					fadeOut: 25
				});

				_Schema.showSchemaRecompileMessage();
				Command.snapshots("purge", undefined, undefined, function () {
					_Schema.reload();
					_Schema.hideSchemaRecompileMessage();
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

		let showJavaMethodsCheckbox = $('#show-java-methods-in-schema-checkbox');
		if (showJavaMethodsCheckbox) {
			showJavaMethodsCheckbox.prop("checked", _Schema.showJavaMethods);
			showJavaMethodsCheckbox.on('click', function() {
				_Schema.showJavaMethods = showJavaMethodsCheckbox.prop('checked');
				LSWrapper.setItem(_Schema.showJavaMethodsKey, _Schema.showJavaMethods);
				blinkGreen(showJavaMethodsCheckbox.parent());
			});
		}
	},
	appendLayoutToolsToContainer: async function(container) {

		let html = await Structr.fetchHtmlTemplate('schema/layout-tools', {});

		container.append(html);

		$('#reset-schema-positions', container).off('click').on('click', _Schema.clearPositions);
		var layoutSelector        = $('#saved-layout-selector', container);
		var layoutNameInput       = $('#layout-name', container);
		var createNewLayoutButton = $('#create-new-layout', container);
		var updateLayoutButton    = $('#update-layout', container);
		var restoreLayoutButton   = $('#restore-layout', container);
		var downloadLayoutButton  = $('#download-layout', container);
		var deleteLayoutButton    = $('#delete-layout', container);

		var layoutSelectorChangeHandler = function () {

			let selectedOption = $(':selected:not(:disabled)', layoutSelector);

			if (selectedOption.length === 0) {

				Structr.disableButton(updateLayoutButton);
				Structr.disableButton(restoreLayoutButton);
				Structr.disableButton(downloadLayoutButton);
				Structr.disableButton(deleteLayoutButton);

			} else {

				Structr.enableButton(restoreLayoutButton);
				Structr.enableButton(downloadLayoutButton);

				let optGroup    = selectedOption.closest('optgroup');
				let username    = optGroup.prop('label');
				let isOwnerless = optGroup.data('ownerless') === true;

				if (isOwnerless || username === me.username) {
					Structr.enableButton(updateLayoutButton);
					Structr.enableButton(deleteLayoutButton);
				} else {
					Structr.disableButton(updateLayoutButton);
					Structr.disableButton(deleteLayoutButton);
				}
			}
		};
		layoutSelectorChangeHandler();

		layoutSelector.on('change', layoutSelectorChangeHandler);

		createNewLayoutButton.click(function() {

			var layoutName = layoutNameInput.val();

			if (layoutName && layoutName.length) {

				Command.createApplicationConfigurationDataNode('layout', layoutName, JSON.stringify(_Schema.getSchemaLayoutConfiguration()), function(data) {

					if (!data.error) {

						new MessageBuilder().success("Layout saved").show();

						_Schema.updateGroupedLayoutSelector([layoutSelector, _Schema.globalLayoutSelector], layoutSelectorChangeHandler);
						layoutNameInput.val('');

						blinkGreen(layoutSelector);

					} else {

						new MessageBuilder().error().title(data.error).text(data.message).show();
					}
				});

			} else {
				Structr.error('Schema layout name is required.');
			}
		});

		updateLayoutButton.click(function() {

			var selectedLayout = layoutSelector.val();

			Command.setProperty(selectedLayout, 'content', JSON.stringify(_Schema.getSchemaLayoutConfiguration()), false, function(data) {

				if (!data.error) {

					new MessageBuilder().success("Layout saved").show();

					blinkGreen(layoutSelector);

				} else {

					new MessageBuilder().error().title(data.error).text(data.message).show();
				}
			});
		});

		restoreLayoutButton.click(function() {
			_Schema.restoreLayout(layoutSelector);
		});

		downloadLayoutButton.click(function() {

			var selectedLayout = layoutSelector.val();

			Command.getApplicationConfigurationDataNode(selectedLayout, function(data) {

				var element = document.createElement('a');
				element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(JSON.stringify({name:data.name, content: JSON.parse(data.content)})));
				element.setAttribute('download', selectedLayout + '.json');

				element.style.display = 'none';
				document.body.appendChild(element);

				element.click();
				document.body.removeChild(element);
			});
		});

		deleteLayoutButton.click(function() {

			var selectedLayout = layoutSelector.val();

			Command.deleteNode(selectedLayout, false, function() {
				_Schema.updateGroupedLayoutSelector([layoutSelector, _Schema.globalLayoutSelector], layoutSelectorChangeHandler);
				blinkGreen(layoutSelector);
			});
		});

		_Schema.updateGroupedLayoutSelector([layoutSelector, _Schema.globalLayoutSelector], layoutSelectorChangeHandler);
	},
	updateGroupedLayoutSelector: function(layoutSelectors, callback) {

		Command.getApplicationConfigurationDataNodesGroupedByUser('layout', function(grouped) {

			for (let layoutSelector of layoutSelectors) {

				layoutSelector.empty();
				layoutSelector.append('<option selected value="" disabled>-- Select Layout --</option>');

				if (grouped.length === 0) {
					layoutSelector.append('<option value="" disabled>no layouts available</option>');
				} else {

					grouped.forEach(function(group) {

						let optGroup = $('<optgroup data-ownerless="' + group.ownerless + '" label="' + group.label + '"></optgroup>');
						layoutSelector.append(optGroup);

						group.configs.forEach(function(layout) {

							optGroup.append('<option value="' + layout.id + '">' + layout.name + '</option>');
						});
					});
				}
			}

			if (typeof callback === "function") {
				callback();
			}
		});
	},
	restoreLayout: function (layoutSelector) {

		let selectedLayout = layoutSelector.val();

		if (selectedLayout) {

			Command.getApplicationConfigurationDataNode(selectedLayout, function(data) {
				_Schema.applySavedLayoutConfiguration(data.content);
			});
		}
	},
	applySavedLayoutConfiguration: function (layoutJSON) {

		try {

			var loadedConfig = JSON.parse(layoutJSON);

			if (loadedConfig._version) {

				switch (loadedConfig._version) {
					case 2: {

						_Schema.ui.zoomLevel = loadedConfig.zoom;
						LSWrapper.setItem(_Schema.schemaZoomLevelKey, _Schema.ui.zoomLevel);
						_Schema.ui.setZoom(_Schema.ui.zoomLevel, instance, [0,0], $('#schema-graph')[0]);
						$( "#zoom-slider" ).slider('value', _Schema.ui.zoomLevel);

						_Schema.ui.updateOverlayVisibility(loadedConfig.showRelLabels);

						var hiddenTypes = loadedConfig.hiddenTypes;

						// Filter out types that do not exist in the schema (if types are available already!)
						if (_Schema.availableTypeNames.length > 0) {
							hiddenTypes = hiddenTypes.filter(function(typeName) {
								return (_Schema.availableTypeNames.indexOf(typeName) !== -1);
							});
						}
						_Schema.hiddenSchemaNodes = hiddenTypes;
						LSWrapper.setItem(_Schema.hiddenSchemaNodesKey, JSON.stringify(_Schema.hiddenSchemaNodes));

						// update the list in the visibility table
						$('#schema-options-table input.toggle-type').prop('checked', true);
						_Schema.hiddenSchemaNodes.forEach(function(hiddenType) {
							$('#schema-options-table input.toggle-type[data-structr-type="' + hiddenType + '"]').prop('checked', false);
						});

						var connectorStyle = loadedConfig.connectorStyle;
						$('#connector-style').val(connectorStyle);
						_Schema.ui.connectorStyle = connectorStyle;
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

			LSWrapper.save();

			_Schema.reload();

		} catch (e) {
			console.warn(e);
			Structr.error('Unreadable JSON - please make sure you are using JSON exported from this dialog!', true);
		}

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
			zoom: _Schema.ui.zoomLevel,
			connectorStyle: _Schema.ui.connectorStyle,
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

		var activateTab = function(tabName) {
			$('.tools-tab-content', contentDiv).hide();
			$('li', ul).removeClass('active');
			$('#tabView-' + tabName, contentDiv).show();
			$('li[data-name="' + tabName + '"]', ul).addClass('active');
			LSWrapper.setItem(_Schema.activeSchemaToolsSelectedTabLevel1Key, tabName);
		};

		$('#schema-tools-tabs > li', mainTabs).off('click').on('click', function(e) {
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

		var activeTab = LSWrapper.getItem(_Schema.activeSchemaToolsSelectedTabLevel1Key) || 'admin';
		activateTab(activeTab);
	},
	appendTypeVisibilityOptionsToContainer: function(container) {

		var visibilityTables = [
			{
				caption: "Custom Types",
				filterFn: (node) => { return node.isBuiltinType === false; },
				exact: true
			},
			{
				caption: "Core Types",
				filterFn: (node) => { return node.isBuiltinType === true && node.category === 'core'; },
				exact: false
			},
			{
				caption: "UI Types",
				filterFn: (node) => { return node.isBuiltinType === true && node.category === 'ui'; },
				exact: false
			},
			{
				caption: "HTML Types",
				filterFn: (node) => { return node.isBuiltinType === true && node.category === 'html'; },
				exact: false
			},
			{
				caption: "Uncategorized Types",
				filterFn: (node) => { return node.isBuiltinType === true && node.category === null; },
				exact: true
			}
		];

		var id = "schema-tools-visibility";
		container.append('<div id="' + id + '_head"><div class="data-tabs level-two"><ul id="' + id + '-tabs"></ul></div></div>');
		var ul = $('#' + id + '-tabs', container);
		var contentEl = $('<div id="' + id + '_content"></div>');
		container.append(contentEl);

		var activateTab = function(tabName) {
			$('.tab', container).hide();
			$('li', ul).removeClass('active');
			$('div[data-name="' + tabName + '"]', contentEl).show();
			$('li[data-name="' + tabName + '"]', ul).addClass('active');
			LSWrapper.setItem(_Schema.activeSchemaToolsSelectedVisibilityTab, tabName);
		};

		Command.query('SchemaNode', 2000, 1, 'name', 'asc', {}, function(schemaNodes) {

			visibilityTables.forEach(function(visType) {
				ul.append('<li id="tab" data-name="' + visType.caption + '">' + visType.caption + '</li>');

				var tab = $('<div class="tab" data-name="' + visType.caption + '"></div>');
				contentEl.append(tab);

				var schemaVisibilityTable = $('<table class="props schema-visibility-table"></table>');
				schemaVisibilityTable.append('<tr><th class="" colspan=2>' + visType.caption + '</th></tr>');
				schemaVisibilityTable.append('<tr><th class="toggle-column-header"><input type="checkbox" title="Toggle all" class="toggle-all-types"><i class="invert-all-types invert-icon ' + _Icons.getFullSpriteClass(_Icons.toggle_icon) + '" title="Invert all"></i> Visible</th><th>Type</th></tr>');
				tab.append(schemaVisibilityTable);

				schemaNodes.forEach(function(schemaNode) {

					if (visType.filterFn(schemaNode)) {
						let hidden = _Schema.hiddenSchemaNodes.indexOf(schemaNode.name) > -1;
						schemaVisibilityTable.append('<tr><td><input class="toggle-type" data-structr-type="' + schemaNode.name + '" type="checkbox" ' + (hidden ? '' : 'checked') + '></td><td>' + schemaNode.name + '</td></tr>');
					}
				});
			});

			$('input.toggle-all-types', container).on('click', function() {
				var typeTable = $(this).closest('table');
				var checked = $(this).prop("checked");
				$('.toggle-type', typeTable).each(function(i, checkbox) {
					var inp = $(checkbox);
					inp.prop("checked", checked);
				});
				_Schema.updateHiddenSchemaTypes();
				_Schema.reload();
			});

			$('i.invert-all-types', container).on('click', function() {
				var typeTable = $(this).closest('table');
				$('.toggle-type', typeTable).each(function(i, checkbox) {
					var inp = $(checkbox);
					inp.prop("checked", !inp.prop("checked"));
				});
				_Schema.updateHiddenSchemaTypes();
				_Schema.reload();
			});

			$('td .toggle-type', container).on('click', function(e) {
				e.stopPropagation();
				e.preventDefault();
				return false;
			});

			$('.schema-visibility-table td', container).on('mouseup', function(e) {
				e.stopPropagation();
				e.preventDefault();
				var td = $(this);
				var inp = $('.toggle-type', td.parent());
				inp.prop("checked", !inp.prop("checked"));
				_Schema.updateHiddenSchemaTypes();
				_Schema.reload();
				return false;
			});

			$('#' + id + '-tabs > li', container).off('click').on('click', function(e) {
				e.stopPropagation();
				activateTab($(this).data('name'));
			});

			var activeTab = LSWrapper.getItem(_Schema.activeSchemaToolsSelectedVisibilityTab) || visibilityTables[0].caption;
			activateTab(activeTab);
		}, false, null, 'id,name,isBuiltinType,category');

	},
	updateHiddenSchemaTypes: function() {

		let hiddenTypes = [];

		$('.schema-visibility-table input.toggle-type').each(function(i, checkbox) {
			let inp = $(checkbox);
			var typeName = inp.attr('data-structr-type');
			let visible = inp.is(':checked');

			if (!visible) {
				hiddenTypes.push(typeName);
			}
		});

		_Schema.hiddenSchemaNodes = hiddenTypes;
		LSWrapper.setItem(_Schema.hiddenSchemaNodesKey, JSON.stringify(_Schema.hiddenSchemaNodes));
	},
	hideSelectedSchemaTypes: function () {

		if (_Schema.ui.selectedNodes.length > 0) {

			_Schema.ui.selectedNodes.forEach(function(n) {
				_Schema.hiddenSchemaNodes.push(n.name);
			});

			LSWrapper.setItem(_Schema.hiddenSchemaNodesKey, JSON.stringify(_Schema.hiddenSchemaNodes));
			_Schema.reload();
		}
	},
	hideSingleSchemaType: function (name) {

		if (name) {

			_Schema.hiddenSchemaNodes.push(name);

			LSWrapper.setItem(_Schema.hiddenSchemaNodesKey, JSON.stringify(_Schema.hiddenSchemaNodes));
			_Schema.reload();
		}
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

		var getParentClassName = function (str) {
			if (str.slice(-1) === '>') {
				var res = str.match("([^<]*)<([^>]*)>");
				return getParentClassName(res[1]) + "&lt;" + getParentClassName(res[2]) + "&gt;";

			} else {
				return str.slice(str.lastIndexOf('.') + 1);
			}
		};

		schemaNodes.forEach(function(schemaNode) {

			var classObj = {
				name: schemaNode.name,
				parent: (schemaNode.extendsClass === null ? 'AbstractNode' : getParentClassName(schemaNode.extendsClass))
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
				_Schema.ui.selectedNodes = [$node];
			}
		});

		var searchTimeout;
		$('#search-classes').keyup(function(e) {
			if (e.which === 27) {
				$('#search-classes').val('');
				inheritanceTree.jstree(true).clear_search();
			} else {
				if (searchTimeout) {
					clearTimeout(searchTimeout);
				}

				searchTimeout = setTimeout(function () {
					var query = $('#search-classes').val();
					inheritanceTree.jstree(true).search(query, true, true);
				}, 250);
			}
		});
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
		_Schema.typeInfoCache = {};
	},
	getTypeInfo: function (type, callback) {

		if (_Schema.typeInfoCache[type] && typeof _Schema.typeInfoCache[type] === Object) {

			callback(_Schema.typeInfoCache[type]);

		} else {

			Command.getSchemaInfo(type, function(schemaInfo) {

				var typeInfo = {};
				$(schemaInfo).each(function(i, prop) {
					typeInfo[prop.jsonName] = prop;
				});

				_Schema.typeInfoCache[type] = typeInfo;

				callback(typeInfo);
			});
		}
	},
	getDerivedTypes: function(baseType, blacklist, callback) {

		Command.getByType('SchemaNode', 10000, 1, 'name', 'asc', 'name,extendsClass,isAbstract', false, function(result) {

			var fileTypes = [];
			var depth     = 5;
			var types     = {};

			var collect = function(list, type) {

				list.forEach(function(n) {

					if (n.extendsClass === type) {

						fileTypes.push('org.structr.dynamic.' + n.name);

						if (!n.isAbstract && !blacklist.includes(n.name)) {
							types[n.name] = 1;
						}
					}
				});
			};

			collect(result, baseType);

			for (var i=0; i<depth; i++) {

				fileTypes.forEach(function(type) {

					collect(result, type);
				});
			}

			if (callback && typeof callback === 'function') {
				callback(Object.keys(types));
			}
		});
	},
	ui: {
		showInheritance: true,
		connectorStyle: undefined,
		zoomLevel: undefined,
		selectedRel: undefined,
		relHighlightColor: 'red',
		maxZ: 0,
		selectionInProgress: false,
		mouseDownCoords: {x:0, y:0},
		mouseUpCoords: {x:0, y:0},
		nodeDragStartpoint: undefined,
		selectBox: undefined,
		selectedNodes: [],
		selectRel: function(rel) {
			_Schema.ui.clearSelection();

			_Schema.ui.selectedRel = rel;
			_Schema.ui.selectedRel.css({zIndex: ++_Schema.ui.maxZ});

			if (!rel.hasClass('dashed-inheritance-relationship')) {
				_Schema.ui.selectedRel.nextAll('._jsPlumb_overlay').slice(0, 3).css({zIndex: ++_Schema.ui.maxZ, border: '1px solid ' + _Schema.ui.relHighlightColor, borderRadius:'2px', background: 'rgba(255, 255, 255, 1)'});
			}

			var pathElements = _Schema.ui.selectedRel.find('path');
			pathElements.css({stroke: _Schema.ui.relHighlightColor});
			$(pathElements[1]).css({fill: _Schema.ui.relHighlightColor});
		},
		clearSelection: function() {
			// deselect selected node
			$('.node', canvas).removeClass('selected');
			_Schema.ui.selectionStop();

			// deselect selected Relationship
			if (_Schema.ui.selectedRel) {
				_Schema.ui.selectedRel.nextAll('._jsPlumb_overlay').slice(0, 3).css({border:'', borderRadius:'', background: 'rgba(255, 255, 255, .8)'});
				var pathElements = _Schema.ui.selectedRel.find('path');
				pathElements.css({stroke: '', fill: ''});
				$(pathElements[1]).css('fill', '');
				_Schema.ui.selectedRel = undefined;
			}
		},
		selectionStart: function(e) {
			canvas.addClass('noselect');
			_Schema.ui.selectionInProgress = true;
			var schemaOffset = canvas.offset();
			_Schema.ui.mouseDownCoords.x = e.pageX - schemaOffset.left;
			_Schema.ui.mouseDownCoords.y = e.pageY - schemaOffset.top;
		},
		selectionDrag: function(e) {
			if (_Schema.ui.selectionInProgress === true) {
				var schemaOffset = canvas.offset();
				_Schema.ui.mouseUpCoords.x = e.pageX - schemaOffset.left;
				_Schema.ui.mouseUpCoords.y = e.pageY - schemaOffset.top;
				_Schema.ui.drawSelectElem();
			}
		},
		selectionStop: function() {
			_Schema.ui.selectionInProgress = false;
			if (_Schema.ui.selectBox) {
				_Schema.ui.selectBox.remove();
				_Schema.ui.selectBox = undefined;
			}
			_Schema.ui.updateSelectedNodes();
			canvas.removeClass('noselect');
		},
		updateSelectedNodes: function() {
			_Schema.ui.selectedNodes = [];
			var canvasOffset = canvas.offset();
			$('.node.selected', canvas).each(function(idx, el) {
				var $el = $(el);
				var elementOffset = $el.offset();
				_Schema.ui.selectedNodes.push({
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
			if (!_Schema.ui.selectBox || !_Schema.ui.selectBox.length) {
				canvas.append('<svg id="schema-graph-select-box"><path version="1.1" xmlns="http://www.w3.org/1999/xhtml" fill="none" stroke="#aaa" stroke-width="5"></path></svg>');
				_Schema.ui.selectBox = $('#schema-graph-select-box');
			}
			var cssRect = {
				position: 'absolute',
				top:    Math.min(_Schema.ui.mouseDownCoords.y, _Schema.ui.mouseUpCoords.y)  / _Schema.ui.zoomLevel,
				left:   Math.min(_Schema.ui.mouseDownCoords.x, _Schema.ui.mouseUpCoords.x)  / _Schema.ui.zoomLevel,
				width:  Math.abs(_Schema.ui.mouseDownCoords.x - _Schema.ui.mouseUpCoords.x) / _Schema.ui.zoomLevel,
				height: Math.abs(_Schema.ui.mouseDownCoords.y - _Schema.ui.mouseUpCoords.y) / _Schema.ui.zoomLevel
			};
			_Schema.ui.selectBox.css(cssRect);
			_Schema.ui.selectBox.find('path').attr('d', 'm 0 0 h ' + cssRect.width + ' v ' + cssRect.height + ' h ' + (-cssRect.width) + ' v ' + (-cssRect.height) + ' z');
			_Schema.ui.selectNodesInRect(cssRect);
		},
		selectNodesInRect: function(selectionRect) {
			_Schema.ui.selectedNodes = [];

			$('.node', canvas).each(function(idx, el) {
				var $el = $(el);
				if (_Schema.ui.isElemInSelection($el, selectionRect)) {
					_Schema.ui.selectedNodes.push($el);
					$el.addClass('selected');
				} else {
					$el.removeClass('selected');
				}
			});
		},
		isElemInSelection: function($el, selectionRect) {
			var elPos = $el.offset();
			elPos.top /= _Schema.ui.zoomLevel;
			elPos.left /= _Schema.ui.zoomLevel;
			var schemaOffset = canvas.offset();
			return !(
				(elPos.top) > (selectionRect.top + schemaOffset.top / _Schema.ui.zoomLevel + selectionRect.height) ||
				elPos.left > (selectionRect.left + schemaOffset.left / _Schema.ui.zoomLevel + selectionRect.width) ||
				(elPos.top + $el.innerHeight()) < (selectionRect.top + schemaOffset.top / _Schema.ui.zoomLevel) ||
				(elPos.left + $el.innerWidth()) < (selectionRect.left + schemaOffset.left / _Schema.ui.zoomLevel)
			);
		},
		setZoom: function(zoom, instance, transformOrigin, el) {
			transformOrigin = transformOrigin || [ 0.5, 0.5 ];
			instance = instance || jsPlumb;
			el = el || instance.getContainer();
			var p = [ "webkit", "moz", "ms", "o" ],
				s = _Schema.ui.getSchemaCSSTransform(),
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
			return 'scale(' + _Schema.ui.zoomLevel + ') translate(' + ((inheritanceSlideout.position().left + inheritanceSlideout.outerWidth()) / _Schema.ui.zoomLevel) + 'px)';
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
		updateInheritanceVisibility: function(show) {
			LSWrapper.setItem(_Schema.showSchemaInheritanceKey, show);
			$('#schema-show-inheritance').prop('checked', show);
			if (show) {
				$('.dashed-inheritance-relationship').show();
			} else {
				$('.dashed-inheritance-relationship').hide();
			}
		},
	},
	filterJavaMethods: function(methods) {
		if (!_Schema.showJavaMethods) {
			return methods.filter(m => m.codeType !== 'java');
		}
		return methods;
	}
};