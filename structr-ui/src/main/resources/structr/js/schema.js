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
var canvas, instance, res, nodes = {}, rels = {}, localStorageSuffix = '_schema_' + location.port;
var reload = false;
var schemaContainer;
var inheritanceTree, inheritanceSlideout;

$(document).ready(function() {
	Structr.registerModule(_Schema);
	Structr.classes.push('schema');

	Command.getApplicationConfigurationDataNodesGroupedByUser('layout', function(grouped) {
		_Schema.storedLayouts = grouped;
	});
});

let _Schema = {
	storedLayouts: [{'foo':'bar'}],
	_moduleName: 'schema',
	undefinedRelType: 'UNDEFINED_RELATIONSHIP_TYPE',
	initialRelType: 'UNDEFINED_RELATIONSHIP_TYPE',
	schemaLoading: false,
	schemaLoaded: false,
	nodePositions: undefined,
	availableTypeNames: [],
	hiddenSchemaNodes: [],
	hiddenSchemaNodesKey: 'structrHiddenSchemaNodes_' + location.port,
	schemaPositionsKey: 'structrSchemaPositions_' + location.port,
	showSchemaOverlaysKey: 'structrShowSchemaOverlays_' + location.port,
	showSchemaInheritanceKey: 'structrShowSchemaInheritance_' + location.port,
	showJavaMethodsKey: 'structrShowJavaMethods_' + location.port,
	schemaMethodsHeightsKey: 'structrSchemaMethodsHeights_' + location.port,
	schemaActiveTabLeftKey: 'structrSchemaActiveTabLeft_' + location.port,
	activeSchemaToolsSelectedTabLevel1Key: 'structrSchemaToolsSelectedTabLevel1_' + location.port,
	activeSchemaToolsSelectedVisibilityTab: 'activeSchemaToolsSelectedVisibilityTab_' + location.port,
	schemaZoomLevelKey: localStorageSuffix + 'zoomLevel',
	schemaConnectorStyleKey: localStorageSuffix + 'connectorStyle',
	currentNodeDialogId: null,
	showJavaMethods: false,
	reload: function(callback) {

		_Schema.clearTypeInfoCache();

		if (reload) {
			return;
		}

		reload = true;
		//_Schema.storePositions();	/* CHM: don't store positions on every reload, let automatic positioning do its job.. */
		//schemaContainer.empty();
		fastRemoveAllChildren(canvas[0]);
		_Schema.init({x: window.scrollX, y: window.scrollY}, callback);
		_Schema.resize();

	},
	storePositions: function() {
		$.each($('#schema-graph .node'), function(i, n) {
			let node = $(n);
			let type = node.children('b').text();
			let obj = node.offset();
			obj.left = (obj.left) / _Schema.ui.zoomLevel;
			obj.top  = (obj.top)  / _Schema.ui.zoomLevel;
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
		_Schema.schemaLoaded  = false;
		_Schema.schema        = [];
		_Schema.keys          = [];

		_Schema.ui.connectorStyle  = LSWrapper.getItem(_Schema.schemaConnectorStyleKey) || 'Flowchart';
		_Schema.ui.zoomLevel       = parseFloat(LSWrapper.getItem(_Schema.schemaZoomLevelKey)) || 1.0;
		_Schema.ui.showInheritance = LSWrapper.getItem(_Schema.showSchemaInheritanceKey, true) || true;
		_Schema.showJavaMethods    = LSWrapper.getItem(_Schema.showJavaMethodsKey, false) || false;

		Structr.functionBar.innerHTML = _Schema.templates.functions();

		UISettings.showSettingsForCurrentModule();

		_Schema.activateDisplayDropdownTools();
		_Schema.activateSnapshotsDialog();
		_Schema.activateAdminTools();

		$('#zoom-slider').slider({
			min: 0.25,
			max: 1,
			step: 0.05,
			value: _Schema.ui.zoomLevel,
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

		$('#global-schema-methods').off('click').on('click', _Schema.methods.showGlobalSchemaMethods);

		$('#type-name').off('keyup').on('keyup', function(e) {

			if (e.keyCode === 13) {
				e.preventDefault();
				if ($('#type-name').val().length) {
					$('#create-type').click();
					$('#type-name').blur();
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
					lineWidth: 4,
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

				let overlaysVisible    = LSWrapper.getItem(_Schema.showSchemaOverlaysKey);
				let showSchemaOverlays = (overlaysVisible === null) ? true : overlaysVisible;
				_Schema.ui.updateOverlayVisibility(showSchemaOverlays);

				let inheritanceVisible    = LSWrapper.getItem(_Schema.showSchemaInheritanceKey);
				let showSchemaInheritance = (inheritanceVisible === null) ? true : inheritanceVisible;
				_Schema.ui.updateInheritanceVisibility(showSchemaInheritance);

				if (scrollPosition) {
					window.scrollTo(scrollPosition.x, scrollPosition.y);
				}

				if (typeof callback === "function") {
					callback();
				}
			});
		});

		Structr.adaptUiToAvailableFeatures();
	},
	showSchemaRecompileMessage: () => {
		Structr.showNonBlockUILoadingMessage('Schema is compiling', 'Please wait...');
	},
	hideSchemaRecompileMessage:  () => {
		Structr.hideNonBlockUILoadingMessage();
	},
	onload: function() {

		main[0].innerHTML = _Schema.templates.main();

		schemaContainer     = $('#schema-container');
		inheritanceSlideout = $('#inheritance-tree');
		inheritanceTree     = $('#inheritance-tree-container');

		let updateCanvasTranslation = function() {
			canvas.css('transform', _Schema.ui.getSchemaCSSTransform());
			_Schema.resize();
		};

		$('#inheritanceTab').off('click').on('click', function() {
			_Pages.leftSlideoutTrigger(this, inheritanceSlideout, [], (params) => {
				LSWrapper.setItem(_Schema.schemaActiveTabLeftKey, $(this).prop('id'));
				inheritanceTree.show();
				updateCanvasTranslation();
			}, function() {
				LSWrapper.removeItem(_Schema.schemaActiveTabLeftKey);
				updateCanvasTranslation();
				inheritanceTree.hide();
			});
		});

		if (LSWrapper.getItem(_Schema.schemaActiveTabLeftKey)) {
			$('#' + LSWrapper.getItem(_Schema.schemaActiveTabLeftKey)).click();
		}

		_Schema.init(() => {
			_Schema.resize();
		});
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

			let currentView = LSWrapper.getItem(_Entities.activeEditTabPrefix  + '_' + _Schema.currentNodeDialogId);

			_Schema.reload(function() {
				_Schema.openEditDialog(_Schema.currentNodeDialogId, currentView);
			});

		} else {

			_Schema.reload();
		}
	},
	isSchemaLoaded: function() {
		let all = true;
		if (!_Schema.schemaLoaded) {
			$.each(_Schema.types, function(t, type) {
				all &= (_Schema.schema[type] && _Schema.schema[type] !== null);
			});
		}
		_Schema.schemaLoaded = all;
		return _Schema.schemaLoaded;
	},
	updateHiddenSchemaNodes: function() {

		return fetch(Structr.rootUrl + 'ApplicationConfigurationDataNode/ui?configType=layout').then(function(response) {

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

				return fetch(Structr.rootUrl + 'SchemaNode/ui?' + Structr.getRequestParameterName('sort') + '=hierarchyLevel&' + Structr.getRequestParameterName('order') + '=asc').then(function(response) {

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

			let entities         = {};
			let inheritancePairs = {};
			let hierarchy        = {};
			let x=0, y=0;

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

				let nodePositions = {};

				// positions are stored the 'old' way => convert to the 'new' way
				let typeNames = data.result.map(entity => entity.name);
				for (let typeName of typeNames) {

					let nodePos = JSON.parse(LSWrapper.getItem(typeName + localStorageSuffix + 'node-position'));
					if (nodePos) {
						nodePositions[typeName] = nodePos.position;

						LSWrapper.removeItem(typeName + localStorageSuffix + 'node-position');
					}
				}

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

			for (let entity of data.result) {

				_Schema.availableTypeNames.push(entity.name);

				let level   = 0;
				let outs    = entity.relatedTo ? entity.relatedTo.length : 0;
				let ins     = entity.relatedFrom ? entity.relatedFrom.length : 0;
				let hasRels = (outs > 0 || ins > 0);

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

				entities[entity.id] = entity.id;
			}

			for (let entity of data.result) {

				if (entity.extendsClass && entity.extendsClass.id && entities[entity.extendsClass.id]) {
					let target = entities[entity.extendsClass.id];
					inheritancePairs[entity.id] = target;
				}
			}

			for (let entitiesAtHierarchyLevel of Object.values(hierarchy)) {

				for (let entity of entitiesAtHierarchyLevel) {

					nodes[entity.id] = entity;

					if (!(_Schema.hiddenSchemaNodes.length > 0 && _Schema.hiddenSchemaNodes.indexOf(entity.name) > -1)) {

						let id = 'id_' + entity.id;
						canvas.append(`<div class="schema node compact${(entity.isBuiltinType ? ' light' : '')}" id="${id}">
							<b>${entity.name}</b>
							<div class="icons-container flex items-center">
								${_Icons.getSvgIcon('pencil_edit', 16, 16, _Icons.getSvgIconClassesNonColorIcon(['node-action-icon', 'edit-type-icon']))}
								${(entity.isBuiltinType ? '' : _Icons.getSvgIcon('trashcan', 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'node-action-icon', 'delete-type-icon'])))}
							</div>
						</div>`);

						let node = $('#' + id);

						if (!entity.isBuiltinType) {

							node.children('b').off('click').on('click', () => {
								_Schema.makeAttrEditable(node, 'name');
							});

							$('.delete-type-icon', node).off('click').on('click', function () {
								Structr.confirmation(
									`<h3>Delete schema node '${entity.name}'?</h3><p>This will delete all incoming and outgoing schema relationships as well, but no data will be removed.</p>`,
									function () {
										$.unblockUI({
											fadeOut: 25
										});
										_Schema.deleteNode(entity.id);
									}
								);
							});
						}

						node.off('mousedown').on('mousedown', function () {
							node.css({zIndex: ++_Schema.ui.maxZ});
						});

						let nodePosition = _Schema.nodePositions[entity.name];

						if (!nodePosition || (nodePosition && nodePosition.left === 0 && nodePosition.top === 0)) {

							nodePosition = _Schema.ui.calculateNodePosition(x, y);

							let count = 0; // prevent endless looping

							while (_Schema.overlapsExistingNodes(nodePosition) && count++ < 1000) {
								x++;
								nodePosition = _Schema.ui.calculateNodePosition(x, y);
							}
						}

						let canvasOffsetTop = canvas.offset().top;

						if (nodePosition.top < canvasOffsetTop) {
							nodePosition.top = canvasOffsetTop;

							let count = 0; // prevent endless looping

							while (_Schema.overlapsExistingNodes(nodePosition) && count++ < 1000) {
								x++;
								nodePosition = _Schema.ui.calculateNodePosition(x, y);
							}
						}

						node.offset(nodePosition);

						$('.edit-type-icon', node).off('click').on('click', function (e) {

							e.stopPropagation();

							_Schema.openEditDialog(entity.id);

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
							start: function (ui) {
								let nodeOffset = $(ui.el).offset();
								let canvasOffset = canvas.offset();

								_Schema.ui.nodeDragStartpoint = {
									top: (nodeOffset.top - canvasOffset.top),
									left: (nodeOffset.left - canvasOffset.left)
								};
							},
							drag: function (ui) {

								requestAnimationFrame(() => {

									let $element = $(ui.el);
									let elementId = $element.attr('id');

									if (!$element.hasClass('selected')) {

										_Schema.ui.clearSelection();

									} else {

										let nodeOffset = $element.offset();
										let canvasOffset = canvas.offset();

										let posDelta = {
											top: (_Schema.ui.nodeDragStartpoint.top - nodeOffset.top),
											left: (_Schema.ui.nodeDragStartpoint.left - nodeOffset.left)
										};

										for (let selectedNode of _Schema.ui.selectedNodes) {
											if (selectedNode.nodeId !== elementId) {
												$('#' + selectedNode.nodeId).offset({
													top: (selectedNode.pos.top - posDelta.top > canvasOffset.top) ? (selectedNode.pos.top - posDelta.top) : canvasOffset.top,
													left: (selectedNode.pos.left - posDelta.left > canvasOffset.left) ? (selectedNode.pos.left - posDelta.left) : canvasOffset.left
												});
											}
										}
									}
									instance.repaintEverything();
								});
							},
							stop: function () {
								_Schema.storePositions();
								_Schema.ui.updateSelectedNodes();
								_Schema.resize();
							}
						});

						x++;
					}
				}

				y++;
				x = 0;
			}

			for (let [source, target] of Object.entries(inheritancePairs)) {

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
					paintStyle: { lineWidth: 4, strokeStyle: "#dddddd", dashstyle: '2 2' },
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

			let title = (entity.type === "SchemaRelationshipNode") ? 'Edit schema relationship' : 'Edit schema node';

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

			if (entity.type === "SchemaRelationshipNode") {
				_Schema.loadRelationship(entity, dialogHead, dialogText, nodes[entity.sourceId], nodes[entity.targetId]);
			} else {
				_Schema.loadNode(entity, dialogHead, dialogText, targetView);
			}

			_Schema.ui.clearSelection();
		});

	},
	loadRels: function(callback) {
		var url = Structr.rootUrl + 'schema_relationship_nodes';
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
						connector: [_Schema.ui.connectorStyle, { curviness: 200, cornerRadius: 20, stub: [stub, 20], gap: 6, alwaysRespectStubs: true }],
						paintStyle: { lineWidth: 4, strokeStyle: res.permissionPropagation !== 'None' ? "#ffad25" : "#81ce25" },
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
									label: `<div id="rel_${res.id}">
												${(res.relationshipType === _Schema.initialRelType ? '<span>&nbsp;</span>' : res.relationshipType)}
												${_Icons.getSvgIcon('pencil_edit', 16, 16, _Icons.getSvgIconClassesNonColorIcon(['mr-1', 'icon', 'edit-relationship-icon']))}
												${_Icons.getSvgIcon('trashcan', 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'icon', 'mr-1', 'delete-relationship-icon']))}
											</div>`,
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

					if (res.relationshipType === _Schema.initialRelType) {
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
						$('#rel_' + res.id + ' .icon').css('display', 'inline-block');
					}).off('mouseout').on('mouseout', function() {
						$('#rel_' + res.id + ' .icon').hide();
					});

					$('#rel_' + res.id + ' .edit-relationship-icon').off('click').on('click', function() {
						_Schema.openEditDialog(res.id);
					});

					$('#rel_' + res.id + ' .delete-relationship-icon').off('click').on('click', function() {
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

		let id = '___' + entity.id;
		headEl.append(`
			<div id="${id}_head" class="schema-details">
				<div id="class-inheritance" class="flex items-center gap-x-2">
					<b>${entity.name}</b>
				</div>
			</div>
		`);
		let headContentDiv = $('#' + id + '_head');
		let inheritanceDiv = $('#class-inheritance', headContentDiv);

		if (!entity.isBuiltinType || entity.extendsClass) {

			inheritanceDiv.append(' extends <select class="extends-class-select"></select>');

			if (entity.extendsClass) {

				inheritanceDiv.append(_Icons.getSvgIcon('pencil_edit', 16, 16, _Icons.getSvgIconClassesNonColorIcon(['ml-2', 'edit-parent-type']), 'Edit parent type'));

				$(".edit-parent-type", inheritanceDiv).click(() => {

					_Schema.openEditDialog(entity.extendsClass.id, undefined, () => {

						window.setTimeout(() => {
							_Schema.openEditDialog(entity.id);
						}, 250);
					});
				});
			}
		}

		headContentDiv.append('<div id="tabs"><ul></ul></div>');
		let mainTabs = $('#tabs', headContentDiv);

		let contentDiv = $('<div class="schema-details"></div>');
		contentEl.append(contentDiv);

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'local', 'Direct properties', targetView === 'local', (c) => {
			_Schema.properties.appendLocalProperties(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'remote', 'Linked properties', targetView === 'remote', (c) => {
			let editSchemaObjectLinkHandler = ($el) => {
				_Schema.openEditDialog($el.data('objectId'));
			};
			_Schema.remoteProperties.appendRemote(c, entity, editSchemaObjectLinkHandler);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'builtin', 'Inherited properties', targetView === 'builtin', (c) => {
			_Schema.properties.appendBuiltinProperties(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'views', 'Views', targetView === 'views', (c) => {
			_Schema.views.appendViews(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'methods', 'Methods', targetView === 'methods', (c) => {
			_Schema.methods.appendMethods(c, entity, _Schema.filterJavaMethods(entity.schemaMethods));
		}, null, _Editors.resizeVisibleEditors);

		if (!entity.isBuiltinType) {

			inheritanceDiv.children('b').off('click').on('click', () => {
				_Schema.makeAttrEditable(inheritanceDiv, 'name');
			});
		}

		let classSelect = $('.extends-class-select', headEl);
		classSelect.append('<optgroup label="Default Type"><option value="">AbstractNode - Structr default base type</option></optgroup>');
		fetch(Structr.rootUrl + 'SchemaNode/ui?' + Structr.getRequestParameterName('sort') + '=name').then(async (response) => {

			let data = await response.json();

			let customTypes  = data.result.filter(cls => ((!cls.category || cls.category !== 'html') && !cls.isAbstract && !cls.isInterface && !cls.isBuiltinType));
			let builtinTypes = data.result.filter(cls => ((!cls.category || cls.category !== 'html') && !cls.isAbstract && !cls.isInterface && cls.isBuiltinType));
			let max          = 60; // max. number of chars of summary to display in select box

			let appendOptions = function(optgroup, list) {

				for (let cls of list) {

					if (cls.id !== entity.id) {

						let name     = cls.name;
						let selected = '';

						if (entity.extendsClass && entity.extendsClass.name && entity.extendsClass.id === cls.id) {
							selected = 'selected="selected"';
						}

						if (cls.summary && cls.summary.length) {

							name += ' - ' + cls.summary.substr(0, Math.min(cls.summary.length, max));
							if (cls.summary.length > max) {
								name += '..';
							}
						}

						optgroup.append(`<option ${selected} value="${cls.id}">${name}</option>`);
					}
				}
			};

			if (customTypes.length) {

				classSelect.append('<optgroup id="for-custom-types" label="Custom Types"></optgroup>');
				appendOptions($('#for-custom-types'), customTypes);
			}

			if (builtinTypes.length) {

				classSelect.append('<optgroup id="for-builtin-types" label="System Types"></optgroup>');
				appendOptions($('#for-builtin-types'), builtinTypes);
			}

			classSelect.chosen({
				search_contains: true,
				width: '500px'
			});
		});

		classSelect.off('change').on('change', () => {
			_Schema.storeSchemaEntity('schema_properties', entity, JSON.stringify({ extendsClass: classSelect.val() }), () => {
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

			new MessageBuilder().title('Unsupported cardinality: ' + cardinality).warning('Unable to generate suggestion for linked property name. Unsupported cardinality encountered!').requiresConfirmation().show();
			return typeName;
		}
	},
	createRelationship: function(sourceId, targetId, headEl, contentEl) {

		let relationshipDialogHtml = _Schema.templates.relationshipDialog();

		headEl.append(relationshipDialogHtml);

		_Schema.appendCascadingDeleteHelpText();

		let sourceTypeName = nodes[sourceId].name;
		let targetTypeName = nodes[targetId].name;

		$('#source-type-name').text(sourceTypeName);
		$('#target-type-name').text(targetTypeName);

		if (Structr.isModuleActive(_Schema)) {
			// move buttons to footer of dialog (in code area they stay put)
			let buttonsContainer = headEl[0].querySelector('#rel-edit-buttons');
			dialogBtn[0].prepend(buttonsContainer);
		}

		$('#edit-rel-options-button').hide();
		let saveButton = $('#save-rel-options-button');
		let cancelButton = $('#cancel-rel-options-button');

		let previousSourceSuggestion = '';
		let previousTargetSuggestion = '';

		let updateSuggestions = () => {

			let currentSourceSuggestion = _Schema.createRemotePropertyNameSuggestion(sourceTypeName, $('#source-multiplicity-selector').val());
			let currentTargetSuggestion = _Schema.createRemotePropertyNameSuggestion(targetTypeName, $('#target-multiplicity-selector').val());

			if (currentSourceSuggestion === currentTargetSuggestion) {
				currentSourceSuggestion += '_source';
				currentTargetSuggestion += '_target';
			}

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
	loadRelationship: function(entity, headEl, contentEl, sourceNode, targetNode, saveSuccessFunction) {

		let relationshipDialogHtml = _Schema.templates.relationshipDialog();

		headEl.append(relationshipDialogHtml);

		$('#relationship-options select, #relationship-options textarea, #relationship-options input').attr('disabled', true);

		_Schema.appendCascadingDeleteHelpText();

		let mainTabs = $('#tabs', headEl);

		let contentDiv = $('<div class="schema-details"></div>');
		contentEl.append(contentDiv);

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'local', 'Direct properties', true, function(c) {
			_Schema.properties.appendLocalProperties(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'views', 'Views', false, function(c) {
			_Schema.views.appendViews(c, entity);
		});

		_Entities.appendPropTab(entity, mainTabs, contentDiv, 'methods', 'Methods', false, function(c) {
			_Schema.methods.appendMethods(c, entity, _Schema.filterJavaMethods(entity.schemaMethods));
		}, null, _Editors.resizeVisibleEditors);

		let selectRelationshipOptions = function(rel) {
			$('#source-type-name').text(sourceNode.name).data('objectId', rel.sourceId);
			$('#source-json-name').val(rel.sourceJsonName || rel.oldSourceJsonName);
			$('#target-json-name').val(rel.targetJsonName || rel.oldTargetJsonName);
			$('#source-multiplicity-selector').val(rel.sourceMultiplicity || '*');
			$('#relationship-type-name').val(rel.relationshipType === _Schema.initialRelType ? '' : rel.relationshipType);
			$('#target-multiplicity-selector').val(rel.targetMultiplicity || '*');
			$('#target-type-name').text(targetNode.name).data('objectId', rel.targetId);
			$('#cascading-delete-selector').val(rel.cascadingDeleteFlag || 0);
			$('#autocreate-selector').val(rel.autocreationFlag || 0);
			$('#propagation-selector').val(rel.permissionPropagation || 'None');
			$('#read-selector').val(rel.readPropagation || 'Remove');
			$('#write-selector').val(rel.writePropagation || 'Remove');
			$('#delete-selector').val(rel.deletePropagation || 'Remove');
			$('#access-control-selector').val(rel.accessControlPropagation || 'Remove');
			$('#masked-properties').val(rel.propertyMask);
		};

		if (Structr.isModuleActive(_Schema)) {
			// move buttons to footer of dialog (in code area they stay put)
			let buttonsContainer = headEl[0].querySelector('#rel-edit-buttons');
			dialogBtn[0].prepend(buttonsContainer);
		}

		if (!saveSuccessFunction) {

			$('.edit-schema-object', headEl).off('click').on('click', function(e) {
				e.stopPropagation();

				// todo: only navigate if no changes

				_Schema.openEditDialog($(this).data('objectId'));
				return false;
			});
		} else {
			$('.edit-schema-object', headEl).off('click').removeClass('edit-schema-object');
		}

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

					if (saveSuccessFunction) {
						saveSuccessFunction();
					} else {
						_Schema.reload();
					}

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
		appendLocalProperties: function(el, entity, overrides, optionalAfterSaveCallback) {

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
				],
				addButtonText: 'Add direct property'
			};

			let schemaTableHtml = _Schema.templates.schemaTable(tableConfig);
			let propertiesTable = $(schemaTableHtml);
			el.append(propertiesTable);
			let tbody = $('tbody', propertiesTable);

			_Schema.sort(entity.schemaProperties);

			let typeOptions       = _Schema.templates.typeOptions();
			let typeHintOptions   = _Schema.templates.typeHintOptions();

			for (let prop of entity.schemaProperties) {

				let localPropertyHtml = _Schema.templates.propertyLocal({ property: prop, typeOptions: typeOptions, typeHintOptions: typeHintOptions });

				let row = $(localPropertyHtml);
				tbody.append(row);

				_Schema.properties.setAttributesInRow(prop, row);
				_Schema.properties.bindRowEvents(prop, row, overrides);

				_Schema.properties.tableChanged(propertiesTable);
			}

			propertiesTable[0].querySelector('.discard-all').addEventListener('click', () => {

				for (let discardIcon of tbody[0].querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
			});

			propertiesTable[0].querySelector('.save-all').addEventListener('click', () => {
				_Schema.properties.bulkSave(el, tbody, entity, optionalAfterSaveCallback);
			});

			el[0].querySelector('button.add-button').addEventListener('click', () => {

				let newPropertyHtml = _Schema.templates.propertyNew({ typeOptions: typeOptions });
				let tr = $(newPropertyHtml);
				tbody.append(tr);

				tr[0].querySelector('.property-type').addEventListener('change', () => {

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

				tr[0].querySelector('.discard-changes').addEventListener('click', () => {
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

				let onlyOneChange = (counts.new + counts.delete + counts.update) === 1;

				if (onlyOneChange || confirm(message)) {

					_Schema.showSchemaRecompileMessage();

					fetch(Structr.rootUrl + entity.id, {
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

			let isProtected = false;

			let propertyTypeOption = $('.property-type option[value="' + property.propertyType + '"]', row);
			if (propertyTypeOption) {
				propertyTypeOption.attr('selected', true);
				if (propertyTypeOption.data('protected')) {
					propertyTypeOption.prop('disabled', true);
					propertyTypeOption.closest('select').attr('disabled', true);
					isProtected = true;
				} else {
					propertyTypeOption.prop('disabled', null);
				}
			}

			let typeField = $('.property-type', row);
			$('.property-type option[value=""]', row).remove();

			if (property.propertyType === 'String' && !property.isBuiltinProperty) {
				if (!$('input.content-type', typeField.parent()).length) {
					typeField.after('<input type="text" size="5" class="content-type">');
				}
				$('.content-type', row).val(property.contentType).on('keyup', propertyInfoChangeHandler).prop('disabled', null);
			}

			$('.property-name',    row).on('keyup', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.property-dbname',  row).on('keyup', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.caching-enabled',  row).on('change', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.type-hint',        row).on('change', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.property-type',    row).on('change', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.property-format',  row).on('keyup', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.not-null',         row).on('change', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.compound',         row).on('change', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.unique',           row).on('change', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.indexed',          row).on('change', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.property-default', row).on('keyup', propertyInfoChangeHandler).prop('disabled', isProtected);

			let readWriteButtonClickHandler = (targetProperty) => {

				if (overrides && overrides.editReadWriteFunction) {

					overrides.editReadWriteFunction(property, targetProperty);

				} else {

					let unsavedChanges = _Schema.properties.hasUnsavedChanges(row.closest('table'));

					if (!unsavedChanges || confirm("Really switch to code editing? There are unsaved changes which will be lost!")) {
						_Schema.properties.openCodeEditorForFunctionProperty(property.id, targetProperty, () => {
							_Schema.openEditDialog(property.schemaNode.id, 'local');
						});
					}
				}
			};

			$('.edit-read-function', row).on('click', () => {
				readWriteButtonClickHandler('readFunction');
			}).prop('disabled', isProtected);

			$('.edit-write-function', row).on('click', () => {
				readWriteButtonClickHandler('writeFunction');
			}).prop('disabled', isProtected);

			$('.edit-cypher-query', row).on('click', () => {

				if (overrides && overrides.editCypherProperty) {

					overrides.editCypherProperty(property);

				} else {

					let unsavedChanges = _Schema.properties.hasUnsavedChanges(row.closest('table'));

					if (!unsavedChanges || confirm("Really switch to code editing? There are unsaved changes which will be lost!")) {
						_Schema.properties.openCodeEditorForCypherProperty(property.id, () => {
							_Schema.openEditDialog(property.schemaNode.id, 'local');
						});
					}
				}

			}).prop('disabled', isProtected);

			if (!isProtected) {

				$('.remove-action', row).on('click', function() {

					row.addClass('to-delete');
					propertyInfoChangeHandler();

				}).prop('disabled', null);

				$('.discard-changes', row).on('click', function() {

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
				$('.discard-all', tfoot).removeClass('disabled').attr('disabled', null);
				$('.save-all', tfoot).removeClass('disabled').attr('disabled', null);
			} else {
				$('.discard-all', tfoot).addClass('disabled').attr('disabled', 'disabled');
				$('.save-all', tfoot).addClass('disabled').attr('disabled', 'disabled');
			}
		},
		rowChanged: function(property, row) {

			let propertyInfoUI = _Schema.properties.getInfoFromRow(row);
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
		openCodeEditorForFunctionProperty: function(id, key, callback) {

			dialogMeta.show();

			Command.get(id, 'id,name,contentType,' + key, function(entity) {

				Structr.dialog('Edit ' + key + ' of ' + entity.name, function() {}, function() {}, ['popup-dialog-with-editor']);

				_Schema.properties.editFunctionPropertyCode(entity, key, dialogText, () => {
					window.setTimeout(() => {
						callback();
					}, 250);
				});
			});
		},
		editFunctionPropertyCode: (entity, key, element, callback) => {

			let initialText = entity[key] || '';

			element.append('<div class="editor h-full"></div>');
			let contentBox = $('.editor', element);

			dialogBtn.append('<button id="editorSave" disabled="disabled" class="disabled">Save</button>');
			dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');

			dialogSaveButton = $('#editorSave', dialogBtn);
			saveAndClose     = $('#saveAndClose', dialogBtn);

			let functionPropertyMonacoConfig = {
				language: 'auto',
				lint: true,
				autocomplete: true,
				changeFn: (editor, entity) => {
					let editorText = editor.getValue();

					if (initialText === editorText) {
						dialogSaveButton.prop("disabled", true).addClass('disabled');
						saveAndClose.prop("disabled", true).addClass('disabled');
					} else {
						dialogSaveButton.prop("disabled", false).removeClass('disabled');
						saveAndClose.prop("disabled", false).removeClass('disabled');
					}
				},
				saveFn: (editor, entity) => {
					let text1 = initialText;
					let text2 = editor.getValue();

					if (text1 === text2) {
						return;
					}

					Command.setProperty(entity.id, key, text2, false, function() {

						Structr.showAndHideInfoBoxMessage('Code saved.', 'success', 2000, 200);
						dialogSaveButton.prop("disabled", true).addClass('disabled');
						saveAndClose.prop("disabled", true).addClass('disabled');
						Command.getProperty(entity.id, key, function(newText) {
							initialText = newText;
						});
					});
				},
				saveFnText: `Save ${key} Function`,
				preventRestoreModel: true,
				isAutoscriptEnv: true
			};

			let editor = _Editors.getMonacoEditor(entity, key, contentBox, functionPropertyMonacoConfig);

			_Editors.resizeVisibleEditors();
			Structr.resize();

			saveAndClose.off('click').on('click', function(e) {
				e.stopPropagation();
				dialogSaveButton.click();

				setTimeout(() => {
					dialogSaveButton.remove();
					saveAndClose.remove();
					dialogCancelButton.click();
				}, 500);
			});

			dialogCancelButton.off('click').on('click', function(e) {
				e.stopPropagation();
				e.preventDefault();
				if (callback) {
					callback();
				}
				dialogSaveButton = $('#editorSave', dialogBtn);
				saveAndClose     = $('#saveAndClose', dialogBtn);
				dialogSaveButton.remove();
				saveAndClose.remove();
				return false;
			});

			dialogSaveButton.off('click').on('click', function(e) {
				e.stopPropagation();

				functionPropertyMonacoConfig.saveFn(editor, entity);
			});

			dialogMeta.append('<span class="editor-info"></span>');

			let editorInfo = dialogMeta[0].querySelector('.editor-info');
			_Editors.appendEditorOptionsElement(editorInfo);

			editor.focus();
		},
		openCodeEditorForCypherProperty: (id, closeCallback) => {

			dialogMeta.show();

			Command.get(id, 'id,name,format', (entity) => {

				Structr.dialog('Edit cypher query of ' + entity.name, () => {}, () => {}, ['popup-dialog-with-editor']);

				_Schema.properties.editCypherPropertyCode(entity, dialogText, () => {
					window.setTimeout(() => {
						closeCallback();
					}, 250);
				});
			});
		},
		editCypherPropertyCode: (entity, element, callback) => {

			let key         = 'format';
			let initialText = entity[key] || '';

			element.append('<div class="editor h-full"></div>');
			let contentBox = $('.editor', element);

			dialogBtn.append('<button id="editorSave" disabled="disabled" class="disabled">Save</button>');
			dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');

			dialogSaveButton = $('#editorSave', dialogBtn);
			saveAndClose     = $('#saveAndClose', dialogBtn);

			let cypherPropertyMonacoConfig = {
				language: 'cypher',
				lint: false,
				autocomplete: false,
				changeFn: (editor, entity) => {
					let editorText = editor.getValue();

					if (initialText === editorText) {
						dialogSaveButton.prop("disabled", true).addClass('disabled');
						saveAndClose.prop("disabled", true).addClass('disabled');
					} else {
						dialogSaveButton.prop("disabled", false).removeClass('disabled');
						saveAndClose.prop("disabled", false).removeClass('disabled');
					}
				},
				saveFn: (editor, entity) => {
					let text1 = initialText;
					let text2 = editor.getValue();

					if (text1 === text2) {
						return;
					}

					_Schema.showSchemaRecompileMessage();

					Command.setProperty(entity.id, key, text2, false, function() {

						_Schema.hideSchemaRecompileMessage();

						Structr.showAndHideInfoBoxMessage('Code saved.', 'success', 2000, 200);
						dialogSaveButton.prop("disabled", true).addClass('disabled');
						saveAndClose.prop("disabled", true).addClass('disabled');
						Command.getProperty(entity.id, key, function(newText) {
							initialText = newText;
						});
					});
				},
				saveFnText: `Save Cypher Property Query`,
				preventRestoreModel: true,
				isAutoscriptEnv: false
			};

			let editor = _Editors.getMonacoEditor(entity, key, contentBox, cypherPropertyMonacoConfig);

			_Editors.resizeVisibleEditors();
			Structr.resize();

			saveAndClose.off('click').on('click', function(e) {
				e.stopPropagation();
				dialogSaveButton.click();

				setTimeout(() => {
					dialogSaveButton.remove();
					saveAndClose.remove();
					dialogCancelButton.click();
				}, 500);
			});

			dialogCancelButton.off('click').on('click', function(e) {
				e.stopPropagation();
				e.preventDefault();
				if (callback) {
					callback();
				}
				dialogSaveButton = $('#editorSave', dialogBtn);
				saveAndClose     = $('#saveAndClose', dialogBtn);
				dialogSaveButton.remove();
				saveAndClose.remove();
				return false;
			});

			dialogSaveButton.off('click').on('click', function(e) {
				e.stopPropagation();

				cypherPropertyMonacoConfig.saveFn(editor, entity);
			});

			dialogMeta.append('<span class="editor-info"></span>');

			let editorInfo = dialogMeta[0].querySelector('.editor-info');
			_Editors.appendEditorOptionsElement(editorInfo);

			editor.focus();
		},
		appendBuiltinProperties: (el, entity) => {

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

			let schemaTableHtml = _Schema.templates.schemaTable(tableConfig);
			let propertiesTable = $(schemaTableHtml);
			el.append(propertiesTable);

			$('tfoot', propertiesTable).addClass('hidden');

			_Schema.sort(entity.schemaProperties);

			Command.listSchemaProperties(entity.id, 'ui', (data) => {

				// sort by name
				_Schema.sort(data, "declaringClass", "name");

				for (let prop of data) {

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

						let builtinPropertyHtml = _Schema.templates.propertyBuiltin({ property: property });
						propertiesTable.append(builtinPropertyHtml);
					}
				}
			});
		},
	},
	remoteProperties: {
		cardinalityClasses: {
			'1': 'one',
			'*': 'many'
		},
		appendRemote: function(el, entity, editSchemaObjectLinkHandler, optionalAfterSaveCallback) {

			let tableConfig = {
				class: 'related-attrs schema-props',
				cols: [
					{ class: '', title: 'JSON Name' },
					{ class: '', title: 'Type, direction and related type' },
					{ class: 'actions-col', title: 'Action' }
				]
			};

			let schemaTableHtml = _Schema.templates.schemaTable(tableConfig);
			let tbl   = $(schemaTableHtml);
			let tbody = tbl.find('tbody');
			el.append(tbl);

			if (entity.relatedTo.length === 0 && entity.relatedFrom.length === 0) {
				tbody.append('<td colspan=3 class="no-rels">Type has no relationships...</td></tr>');
				$('tfoot', tbl).addClass('hidden');
			} else {

				entity.relatedTo.forEach(function(target) {
					_Schema.remoteProperties.appendRemoteProperty(tbody, target, true, editSchemaObjectLinkHandler);
				});

				entity.relatedFrom.forEach(function(source) {
					_Schema.remoteProperties.appendRemoteProperty(tbody, source, false, editSchemaObjectLinkHandler);
				});
			}

			$('.discard-all', tbl).on('click', () => {
				for (let discardIcon of tbl[0].querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
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

				let message = 'Update linked property names for ' + entity.name + '?\n\n';
				message += (counts.update > 0 ? 'Update ' + counts.update + ' linked property names.\n' : '');
				message += (counts.reset > 0 ? 'Reset ' + counts.reset + ' linked property names.\n' : '');

				let onlyOneChange = (counts.reset + counts.update) === 1;

				if (onlyOneChange || confirm(message)) {

					_Schema.showSchemaRecompileMessage();

					fetch(Structr.rootUrl + entity.id, {
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

			let relType = (rel.relationshipType === _Schema.undefinedRelType) ? '' : rel.relationshipType;
			let relatedNodeId = (out ? rel.targetId : rel.sourceId);
			let attributeName = (out ? (rel.targetJsonName || rel.oldTargetJsonName) : (rel.sourceJsonName || rel.oldSourceJsonName));

			let renderRemoteProperty = (tplConfig) => {

				let remotePropertyHtml = _Schema.templates.remoteProperty(tplConfig);
				let row = $(remotePropertyHtml);
				el.append(row);

				$('.property-name', row).on('keyup', () => {
					_Schema.remoteProperties.rowChanged(row, attributeName);
				});

				$('.reset-action', row).on('click', () => {
					$('.property-name', row).val('');
					_Schema.remoteProperties.rowChanged(row, attributeName);
				});

				$('.discard-changes', row).on('click', () => {
					$('.property-name', row).val(attributeName);
					_Schema.remoteProperties.rowChanged(row, attributeName);
				});

				if (!editSchemaObjectLinkHandler) {
					$('.edit-schema-object', row).removeClass('edit-schema-object');
				} else {

					$('.edit-schema-object', row).on('click', (e) => {
						e.stopPropagation();

						let unsavedChanges = _Schema.remoteProperties.hasUnsavedChanges(row.closest('table'));

						if (!unsavedChanges || confirm("Really switch to other type? There are unsaved changes which will be lost!")) {
							editSchemaObjectLinkHandler($(e.target));
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
				$('.discard-all', tfoot).removeClass('disabled').attr('disabled', null);
				$('.save-all', tfoot).removeClass('disabled').attr('disabled', null);
			} else {
				$('.discard-all', tfoot).addClass('disabled').attr('disabled', 'disabled');
				$('.save-all', tfoot).addClass('disabled').attr('disabled', 'disabled');
			}

		},
		rowChanged: (row, originalName) => {

			let nameInUI   = $('.property-name', row).val();
			let hasChanges = (nameInUI !== originalName);

			if (hasChanges) {
				row.addClass('has-changes');
			} else {
				row.removeClass('has-changes');
			}

			_Schema.remoteProperties.tableChanged(row.closest('table'));
		},
		validate: (row) => {
			return true;
		}
	},
	views: {
		appendViews: (el, entity, optionalAfterSaveCallback) => {

			let tableConfig = {
				class: 'related-attrs schema-props',
				cols: [
					{ class: '', title: 'Name' },
					{ class: '', title: 'Properties' },
					{ class: 'actions-col', title: 'Action' }
				],
				addButtonText: 'Add view'
			};

			let schemaTableHtml = _Schema.templates.schemaTable(tableConfig);

			let viewsTable = $(schemaTableHtml);
			el.append(viewsTable);

			let tbody = viewsTable.find('tbody');

			_Schema.sort(entity.schemaViews);

			for (let view of entity.schemaViews) {
				_Schema.views.appendView(tbody, view, entity);
			}

			viewsTable[0].querySelector('.discard-all').addEventListener('click', () => {
				for (let discardIcon of tbody[0].querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
			});

			viewsTable[0].querySelector('.save-all').addEventListener('click', () => {
				_Schema.views.bulkSave(el, tbody, entity, optionalAfterSaveCallback);
			});

			el[0].querySelector('button.add-button', ).addEventListener('click', () => {

				let newViewHtml = _Schema.templates.viewNew();
				let row = $(newViewHtml);
				tbody.append(row);

				_Schema.views.tableChanged(viewsTable);

				_Schema.views.appendViewSelectionElement(row, { name: 'new' }, entity, (chznElement) => {
					chznElement.chosenSortable();
				});

				row[0].querySelector('.discard-changes').addEventListener('click', () => {
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

				let onlyOneChange = (counts.new + counts.delete + counts.update) === 1;

				if (onlyOneChange || confirm(message)) {

					_Schema.showSchemaRecompileMessage();

					fetch(Structr.rootUrl + entity.id, {
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
		appendView: function(el, view, entity) {

			let viewHtml = _Schema.templates.view({view: view, type: entity});
			let row = $(viewHtml);
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
				$('.discard-all', tfoot).removeClass('disabled').attr('disabled', null);
				$('.save-all', tfoot).removeClass('disabled').attr('disabled', null);
			} else {
				$('.discard-all', tfoot).addClass('disabled').attr('disabled', 'disabled');
				$('.save-all', tfoot).addClass('disabled').attr('disabled', 'disabled');
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
		lastEditedMethod: {},
		getLastEditedMethod: (entity) => {
			return _Schema.methods.lastEditedMethod[(entity ? entity.id : 'global')];
		},
		setLastEditedMethod: (entity, method) => {
			_Schema.methods.lastEditedMethod[(entity ? entity.id : 'global')] = method;
		},
		appendMethods: (el, entity, methods, optionalAfterSaveCallback) => {

			_Schema.methods.methodsData = {};

			let methodsHtml = _Schema.templates.methods({ class: (entity ? 'entity' : 'global') });
			el.append(methodsHtml);

			let tableConfig = {
				class: 'actions schema-props',
				cols: [
					{ class: '', title: 'Name' },
					{ class: 'isstatic-col', title: 'isStatic' },
					{ class: 'actions-col', title: 'Action' }
				]
			};

			let fakeTableHtml    = _Schema.templates.fakeTable(tableConfig);
			let methodsFakeTable = $(fakeTableHtml);
			let fakeTbody        = methodsFakeTable.find('.fake-tbody');
			$('#methods-table-container', el).append(methodsFakeTable);

			_Schema.methods.activateUIActions(el, fakeTbody, entity);

			_Schema.sort(methods);

			let lastEditedMethod = _Schema.methods.getLastEditedMethod(entity);

			let rowToActivate = undefined;

			for (let method of methods) {

				let methodHtml = _Schema.templates.method({ method: method });
				let fakeRow    = $(methodHtml);
				fakeTbody.append(fakeRow);

				fakeRow.data('type-name', (entity ? entity.name : 'global_schema_method')).data('method-name', method.name);
				$('.property-name', fakeRow).val(method.name);
				$('.property-isStatic', fakeRow).prop('checked', method.isStatic);

				_Schema.methods.methodsData[method.id] = {
					isNew:           false,
					id:              method.id,
					name:            method.name,
					isStatic:        method.isStatic,
					source:          method.source || '',
					initialName:     method.name,
					initialisStatic: method.isStatic,
					initialSource:   method.source || '',
				};

				_Schema.methods.bindRowEvents(fakeRow, entity);

				// auto-edit first method (or last used)
				if ((rowToActivate === undefined) || (lastEditedMethod && ((lastEditedMethod.isNew === false && lastEditedMethod.id === method.id) || (lastEditedMethod.isNew === true && lastEditedMethod.name === method.name)))) {
					rowToActivate = fakeRow;
				}
			}

			// activate
			if (rowToActivate) {
				rowToActivate[0].querySelector('.edit-action').dispatchEvent(new Event('click'));
			}

			$('.discard-all', methodsFakeTable).on('click', () => {
				for (let discardIcon of methodsFakeTable[0].querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
			});

			$('.save-all', methodsFakeTable).on('click', () => {
				_Schema.methods.bulkSave(el, fakeTbody, entity, optionalAfterSaveCallback);
			});

			$('#methods-container-right', el).append('<div class="editor-info"></div>');

			let editorInfo = el[0].querySelector('#methods-container-right .editor-info');
			_Editors.appendEditorOptionsElement(editorInfo);
		},
		bulkSave: (el, fakeTbody, entity, optionalAfterSaveCallback) => {

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

						counts.update++;
						methods.push({
							id:       methodId,
							name:     methodData.name,
							isStatic: methodData.isStatic,
							source:   methodData.source,
						});
						allow = _Schema.methods.validateMethodRow(row) && allow;
					}

				} else {

					counts.new++;
					allow = _Schema.methods.validateMethodRow(row) && allow;
					let method = {
						type:     'SchemaMethod',
						name:     methodData.name,
						isStatic: methodData.isStatic,
						source:   methodData.source,
					};

					if (entity) {
						method.schemaNode = entity.id;
					}

					methods.push(method);
				}
			});

			if (allow) {

				let message = (entity ? 'Update methods for ' + entity.name + '?\n\n' : 'Update global methods?\n\n');
				message += (counts.new > 0 ? 'Create ' + counts.new + ' methods.\n' : '');
				message += (counts.delete > 0 ? 'Delete ' + counts.delete + ' methods.' + (entity ? '(Note: Builtin methods will be restored in their initial configuration!)\n' : '\n') : '');
				message += (counts.update > 0 ? 'Update ' + counts.update + ' methods.\n' : '');

				let onlyOneChange = (counts.new + counts.delete + counts.update) === 1;

				if (onlyOneChange || confirm(message)) {

					let activeMethod = fakeTbody.find('.fake-tr.editing');
					if (activeMethod) {
						_Schema.methods.setLastEditedMethod(entity, _Schema.methods.methodsData[activeMethod.data('methodId')]);
					} else {
						_Schema.methods.setLastEditedMethod(entity, undefined);
					}

					_Schema.showSchemaRecompileMessage();

					fetch(Structr.rootUrl + 'SchemaMethod', {
						dataType: 'json',
						contentType: 'application/json; charset=utf-8',
						method: 'PATCH',
						body: JSON.stringify(methods)
					}).then((response) => {

						if (response.ok) {

							if (entity) {

								Command.get(entity.id, null, (reloadedEntity) => {
									el.empty();
									_Schema.methods.appendMethods(el, reloadedEntity, _Schema.filterJavaMethods(reloadedEntity.schemaMethods));
									_Schema.hideSchemaRecompileMessage();

									if (optionalAfterSaveCallback) {
										optionalAfterSaveCallback();
									}
								});

							} else {

								Command.rest('SchemaMethod?schemaNode=null&' + Structr.getRequestParameterName('sort') + '=name&' + Structr.getRequestParameterName('order') + '=ascending', function (methods) {
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
		activateUIActions: (el, fakeTbody, entity) => {

			let addedMethodsCounter = 1;

			let getRawNewMethod = (name) => {
				return {
					name: _Schema.methods.getFirstFreeMethodName(name),
					id: 'new' + (addedMethodsCounter++)
				};
			};

			el[0].querySelector('.add-action-button').addEventListener('click', () => {
				_Schema.methods.appendNewMethod(fakeTbody, getRawNewMethod(''), entity);
			});

			if (entity) {

				el[0].querySelector('.add-onCreate-button').addEventListener('click', () => {
					_Schema.methods.appendNewMethod(fakeTbody, getRawNewMethod('onCreate'), entity);
				});

				let addAfterCreateButton = el[0].querySelector('.add-afterCreate-button');

				addAfterCreateButton.addEventListener('click', () => {
					_Schema.methods.appendNewMethod(fakeTbody, getRawNewMethod('afterCreate'), entity);
				});

				Structr.appendInfoTextToElement({
					text: "The difference between onCreate an afterCreate is that afterCreate is called after all checks have run and the transaction is committed.<br>Example: There is a unique constraint and you want to send an email when an object is created.<br>Calling 'send_html_mail()' in onCreate would send the email even if the transaction would be rolled back due to an error. The appropriate place for this would be afterCreate.",
					element: $(addAfterCreateButton),
					insertAfter: true
				});

				el[0].querySelector('.add-onSave-button').addEventListener('click', () => {
					_Schema.methods.appendNewMethod(fakeTbody, getRawNewMethod('onSave'), entity);
				});
			}
		},
		appendNewMethod: (fakeTbody, method, entity) => {

			let newMethodHtml = _Schema.templates.method({ method: method, isNew: true });
			let row           = $(newMethodHtml);
			let rowEl         = row[0];
			fakeTbody.append(row);

			fakeTbody.scrollTop(row.position().top);

			_Schema.methods.methodsData[method.id] = {
				id: method.id,
				isNew: true,
				name: method.name,
				isStatic: method.isStatic || false,
				source: method.source || '',
			};

			let propertyNameInput = rowEl.querySelector('.property-name');
			propertyNameInput.addEventListener('input', () => {
				_Schema.methods.methodsData[method.id].name = propertyNameInput.value;
			});

			let isStaticCheckbox = rowEl.querySelector('.property-isStatic');
			isStaticCheckbox.addEventListener('change', () => {
				_Schema.methods.methodsData[method.id].isStatic = isStaticCheckbox.checked;
			});

			rowEl.querySelector('.edit-action').addEventListener('click', () => {
				_Schema.methods.editMethod(row, entity);
			});

			rowEl.querySelector('.clone-action').addEventListener('click', () => {
				_Schema.methods.appendNewMethod(row.closest('.fake-tbody'), {
					id:       method.id + '_clone_' + (new Date().getTime()),
					name:     _Schema.methods.getFirstFreeMethodName(_Schema.methods.methodsData[method.id].name + '_copy'),
					isStatic: _Schema.methods.methodsData[method.id].isStatic,
					source:   _Schema.methods.methodsData[method.id].source
				}, entity);
			});

			rowEl.querySelector('.discard-changes').addEventListener('click', () => {
				if (row.hasClass('editing')) {
					$('#methods-container-right').hide();
				}
				row.remove();

				_Schema.methods.rowChanged(fakeTbody.closest('.fake-table'));

				_Editors.nukeEditorsById(method.id);
			});

			_Schema.methods.fakeTableChanged(fakeTbody.closest('.fake-table'));

			_Schema.methods.editMethod(row, entity);
		},
		getFirstFreeMethodName: (prefix) => {

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
		bindRowEvents: (row, entity) => {

			let rowEl            = row[0];
			let methodId         = rowEl.dataset['methodId'];
			let methodData       = _Schema.methods.methodsData[methodId];

			let propertyNameInput = rowEl.querySelector('.property-name');
			propertyNameInput.addEventListener('input', () => {
				methodData.name = propertyNameInput.value;
				_Schema.methods.rowChanged(row, (methodData.name !== methodData.initialName));
			});

			let isStaticCheckbox = rowEl.querySelector('.property-isStatic');
			isStaticCheckbox.addEventListener('change', () => {
				methodData.isStatic = isStaticCheckbox.checked;
				_Schema.methods.rowChanged(row, (methodData.isStatic !== methodData.initialisStatic));
			});

			rowEl.querySelector('.edit-action').addEventListener('click', () => {
				_Schema.methods.editMethod(row, entity);
			});

			rowEl.querySelector('.clone-action').addEventListener('click', () => {
				_Schema.methods.appendNewMethod(row.closest('.fake-tbody'), {
					id:       methodId + '_clone_' + (new Date().getTime()),
					name:     _Schema.methods.getFirstFreeMethodName(methodData.name + '_copy'),
					isStatic: methodData.isStatic,
					source:   methodData.source
				}, entity);
			});

			rowEl.querySelector('.remove-action').addEventListener('click', () => {
				row.addClass('to-delete');
				_Schema.methods.rowChanged(row, true);
			});

			rowEl.querySelector('.discard-changes').addEventListener('click', () => {

				if (row.hasClass('to-delete') || row.hasClass('has-changes')) {

					row.removeClass('to-delete');
					row.removeClass('has-changes');

					methodData.name     = methodData.initialName;
					methodData.isStatic = methodData.initialisStatic;
					methodData.source   = methodData.initialSource;

					$('.property-name', row).val(methodData.name);
					$('.property-isStatic', row).prop('checked', methodData.isStatic);

					if (row.hasClass('editing')) {
						_Editors.disposeEditorModel(methodData.id, 'source');
						_Schema.methods.editMethod(row);
					}

					_Schema.methods.rowChanged(row, false);
				}
			});
		},
		saveAndDisposePreviousEditor: (row) => {

			let previouslyActiveRow = row.closest('.fake-tbody').find('.fake-tr.editing');
			if (previouslyActiveRow) {

				let previousMethodId = previouslyActiveRow.data('methodId');

				if (previousMethodId) {
					_Editors.saveViewState(previousMethodId, 'source');
					_Editors.disposeEditor(previousMethodId, 'source');
				}
			}
		},
		editMethod: (row, entity) => {

			_Schema.methods.saveAndDisposePreviousEditor(row);

			$('#methods-container-right').show();

			row.closest('.fake-tbody').find('.fake-tr').removeClass('editing');
			row.addClass('editing');

			let methodId   = row.data('methodId');
			let methodData = _Schema.methods.methodsData[methodId];

			_Schema.methods.setLastEditedMethod(entity, methodData);

			let sourceMonacoConfig = {
				language: 'auto',
				lint: true,
				autocomplete: true,
				changeFn: (editor, entity) => {
					methodData.source = editor.getValue();
					let hasChanges = (methodData.source !== methodData.initialSource) || (methodData.name !== methodData.initialName) || (methodData.isStatic !== methodData.initialisStatic);
					_Schema.methods.rowChanged(row, hasChanges);
				}
			};

			let sourceEditor = _Editors.getMonacoEditor(methodData, 'source', $('.editor', $('#methods-content')), sourceMonacoConfig);
			sourceEditor.focus();

			sourceMonacoConfig.changeFn(sourceEditor);

			_Schema.resizeVisibleEditors();
		},
		showGlobalSchemaMethods: () => {

			Command.rest('SchemaMethod?schemaNode=null&' + Structr.getRequestParameterName('sort') + '=name&' + Structr.getRequestParameterName('order') + '=ascending', (methods) => {

				Structr.dialog('Global Schema Methods', () => {
					dialogMeta.show();
				}, () => {
					_Schema.currentNodeDialogId = null;

					dialogMeta.show();
					instance.repaintEverything();
				}, ['schema-edit-dialog', 'global-methods-dialog']);

				dialogMeta.hide();

				let contentEl  = dialogText;
				let contentDiv = $('<div id="tabView-methods" class="schema-details"></div>');
				let outerDiv   = $('<div class="schema-details"></div>');
				outerDiv.append(contentDiv);
				contentEl.append(outerDiv);

				_Schema.methods.appendMethods(contentDiv, null, methods);
			});
		},
		hasUnsavedChanges: (fakeTable) => {
			let fakeTbody = $('.fake-tbody', fakeTable);
			return (fakeTbody.find('.fake-tr.to-delete').length + fakeTbody.find('.fake-tr.has-changes').length) > 0;
		},
		fakeTableChanged: (fakeTable) => {

			let unsavedChanges = _Schema.methods.hasUnsavedChanges(fakeTable);

			let footer = fakeTable.find('.fake-tfoot');

			if (unsavedChanges) {
				$('.discard-all', footer).removeClass('disabled').attr('disabled', null);
				$('.save-all', footer).removeClass('disabled').attr('disabled', null);
			} else {
				$('.discard-all', footer).addClass('disabled').attr('disabled', 'disabled');
				$('.save-all', footer).addClass('disabled').attr('disabled', 'disabled');
			}
		},
		rowChanged: (row, hasChanges) => {

			if (hasChanges) {
				row.addClass('has-changes');
			} else {
				row.removeClass('has-changes');
			}

			_Schema.methods.fakeTableChanged(row.closest('.fake-table'));
		},
		validateMethodRow: (row) => {

			if ($('.property-name', row).val().length === 0) {

				blinkRed($('.property-name', row).closest('.fake-td'));
				return false;
			}

			return true;
		},
	},
	resize: function() {

		Structr.resize();

		if (canvas) {

			let zoom = (instance ? instance.getZoom() : 1);
			let canvasPosition = canvas.offset();
			let padding = 100;


			let canvasSize = {
				w: ($(window).width() - canvasPosition.left) / zoom,
				h: ($(window).height() - canvasPosition.top) / zoom
			};

			$('.node').each(function(i, elem) {
				let $elem = $(elem);
				canvasSize.w = Math.max(canvasSize.w, (($elem.position().left + $elem.outerWidth() - canvasPosition.left) / zoom));
				canvasSize.h = Math.max(canvasSize.h, (($elem.position().top + $elem.outerHeight() - canvasPosition.top)  / zoom + $elem.outerHeight()));

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
		}
	},
	dialogSizeChanged: () => {
		_Schema.resizeVisibleEditors();
	},
	resizeVisibleEditors: () => {
		_Editors.resizeVisibleEditors();
	},
	removeSchemaEntity: function(entity, onSuccess, onError) {

		if (entity && entity.id) {

			_Schema.showSchemaRecompileMessage();

			$.ajax({
				url: Structr.rootUrl + entity.id,
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

			$.ajax({
				url: Structr.rootUrl + entity.id,
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
								url: Structr.rootUrl + entity.id,
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
				url: Structr.rootUrl + resource,
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

		var url = Structr.rootUrl + 'schema_nodes';
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

		var url = Structr.rootUrl + 'schema_nodes/' + id;
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
			url: Structr.rootUrl + 'schema_relationship_nodes',
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
			url: Structr.rootUrl + 'schema_relationship_nodes/' + id,
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
			url: Structr.rootUrl + 'schema_relationship_nodes/' + entity.id,
			type: 'GET',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function(existingData) {

					var changed = Object.keys(newData).some(function(key) {
						return (existingData.result[key] !== newData[key]);
					});

					if (changed) {

						$.ajax({
							url: Structr.rootUrl + 'schema_relationship_nodes/' + entity.id,
							type: 'PUT',
							dataType: 'json',
							contentType: 'application/json; charset=utf-8',
							data: JSON.stringify(newData),
							statusCode: {
								200: function() {
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
	activateSnapshotsDialog: () => {

		let table = $('#snapshots');

		let refresh = function() {

			table.empty();

			Command.snapshots('list', '', null, function(result) {

				for (let data of result) {

					data.snapshots.forEach(function(snapshot, i) {
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
				}
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
	activateAdminTools: () => {

		let registerSchemaToolButtonAction = function (btn, target, connectedSelectElement, getPayloadFunction) {

			btn.off('click').on('click', function(e) {
				e.preventDefault();
				let oldHtml = btn.html();

				let transportAction = function (target, payload) {

					Structr.updateButtonWithAjaxLoaderAndText(btn, oldHtml);
					$.ajax({
						url: Structr.rootUrl + 'maintenance/' + target,
						type: 'POST',
						data: JSON.stringify(payload),
						contentType: 'application/json',
						statusCode: {
							200: () => {
								Structr.updateButtonWithSuccessIcon(btn, oldHtml);
							}
						}
					});
				};

				if (!connectedSelectElement) {
					transportAction(target, {});

				} else {
					let type = connectedSelectElement.val();
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
	activateDisplayDropdownTools: () => {

		document.getElementById('schema-tools').addEventListener('click', (e) => {
			_Schema.openTypeVisibilityDialog();
		});

		document.getElementById('schema-show-overlays').addEventListener('change', (e) => {
			_Schema.ui.updateOverlayVisibility(e.target.checked);
		});

		document.getElementById('schema-show-inheritance').addEventListener('change', (e) => {
			_Schema.ui.updateInheritanceVisibility(e.target.checked);
		});

		for (let edgeStyleOption of document.querySelectorAll('.edge-style')) {
			edgeStyleOption.addEventListener('click', (e) => {
				e.stopPropagation();
				e.preventDefault();

				const el       = e.target;
				const newStyle = el.innerText.trim();

				_Schema.ui.connectorStyle = newStyle;
				LSWrapper.setItem(_Schema.schemaConnectorStyleKey, newStyle);

				_Schema.reload();

				return false;
			});
		}

		let activateLayoutFunctions = () => {

			let layoutSelector        = $('#saved-layout-selector');
			let layoutNameInput       = $('#layout-name');
			let createNewLayoutButton = $('#create-new-layout');
			let updateLayoutButton    = $('#update-layout');
			let restoreLayoutButton   = $('#restore-layout');
			let deleteLayoutButton    = $('#delete-layout');

			let layoutSelectorChangeHandler = function () {

				let selectedOption = $(':selected:not(:disabled)', layoutSelector);

				if (selectedOption.length === 0) {

					Structr.disableButton(updateLayoutButton);
					Structr.disableButton(restoreLayoutButton);
					Structr.disableButton(deleteLayoutButton);

				} else {

					Structr.enableButton(restoreLayoutButton);

					let optGroup    = selectedOption.closest('optgroup');
					let username    = optGroup.prop('label');
					let isOwnerless = optGroup.data('ownerless') === true;

					if (isOwnerless || username === StructrWS.me.username) {
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

			updateLayoutButton.click(function() {

				let selectedLayout = layoutSelector.val();

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

			deleteLayoutButton.click(function() {

				let selectedLayout = layoutSelector.val();

				Command.deleteNode(selectedLayout, false, () => {
					_Schema.updateGroupedLayoutSelector(layoutSelector, layoutSelectorChangeHandler);
					blinkGreen(layoutSelector);
				});
			});

			createNewLayoutButton.on('click', () => {

				let layoutName = layoutNameInput.val();

				if (layoutName && layoutName.length) {

					Command.createApplicationConfigurationDataNode('layout', layoutName, JSON.stringify(_Schema.getSchemaLayoutConfiguration()), function(data) {

						if (!data.error) {

							new MessageBuilder().success("Layout saved").show();

							_Schema.updateGroupedLayoutSelector(layoutSelector, layoutSelectorChangeHandler);
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

			_Schema.updateGroupedLayoutSelector(layoutSelector, layoutSelectorChangeHandler);
		};
		activateLayoutFunctions();

		document.getElementById('reset-schema-positions').addEventListener('click', (e) => {
			_Schema.clearPositions();
		});
	},
	updateGroupedLayoutSelector: function(layoutSelector, callback) {

		Command.getApplicationConfigurationDataNodesGroupedByUser('layout', function(grouped) {

			layoutSelector.empty();
			layoutSelector.append('<option selected value="" disabled>-- Select Layout --</option>');

			if (grouped.length === 0) {
				layoutSelector.append('<option value="" disabled>no layouts available</option>');
			} else {

				for (let group of grouped) {

					let optGroup = $('<optgroup data-ownerless="' + group.ownerless + '" label="' + group.label + '"></optgroup>');
					layoutSelector.append(optGroup);

					for (let layout of group.configs) {

						optGroup.append('<option value="' + layout.id + '">' + layout.name + '</option>');
					}
				}
			}

			if (typeof callback === "function") {
				callback();
			}
		});
	},
	restoreLayout: function(layoutSelector) {

		let selectedLayout = layoutSelector.val();

		if (selectedLayout) {

			Command.getApplicationConfigurationDataNode(selectedLayout, function(data) {
				_Schema.applySavedLayoutConfiguration(data.content);
			});
		}
	},
	applySavedLayoutConfiguration: function(layoutJSON) {

		try {

			let loadedConfig = JSON.parse(layoutJSON);

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

						let connectorStyle = loadedConfig.connectorStyle;
						$('#connector-style').val(connectorStyle);
						_Schema.ui.connectorStyle = connectorStyle;
						LSWrapper.setItem(_Schema.schemaConnectorStyleKey, connectorStyle);

						let positions = loadedConfig.positions;
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
	applyNodePositions: function(positions) {
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
			_version:       2,
			positions:      _Schema.nodePositions,
			hiddenTypes:    _Schema.hiddenSchemaNodes,
			zoom:           _Schema.ui.zoomLevel,
			connectorStyle: _Schema.ui.connectorStyle,
			showRelLabels:  $('#schema-show-overlays').prop('checked')
		};
	},
	openTypeVisibilityDialog: function() {

		Structr.dialog('', function() {}, function() {});

		let visibilityTables = [
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

		let id = "schema-tools-visibility";
		dialogText.append('<div id="' + id + '_head"><div class="data-tabs level-two"><ul id="' + id + '-tabs"></ul></div></div><div id="' + id + '_content"></div>');
		let ul        = $('#' + id + '-tabs', dialogText);
		let contentEl = $('#' + id + '_content', dialogText);

		let activateTab = (tabName) => {
			$('.tab', contentEl).hide();
			$('li', ul).removeClass('active');
			$('div[data-name="' + tabName + '"]', contentEl).show();
			$('li[data-name="' + tabName + '"]', ul).addClass('active');
			LSWrapper.setItem(_Schema.activeSchemaToolsSelectedVisibilityTab, tabName);
		};

		Command.query('SchemaNode', 2000, 1, 'name', 'asc', {}, function(schemaNodes) {

			for (let visType of visibilityTables) {

				ul.append('<li id="tab" data-name="' + visType.caption + '">' + visType.caption + '</li>');

				let tab = $('<div class="tab" data-name="' + visType.caption + '"></div>');
				contentEl.append(tab);

				let schemaVisibilityTable = $('<table class="props schema-visibility-table"></table>');
				schemaVisibilityTable.append('<tr><th class="toggle-column-header"><input type="checkbox" title="Toggle all" class="toggle-all-types"><i class="invert-all-types invert-icon ' + _Icons.getFullSpriteClass(_Icons.toggle_icon) + '" title="Invert all"></i> Visible</th><th>Type</th></tr>');
				tab.append(schemaVisibilityTable);

				for (let schemaNode of schemaNodes) {

					if (visType.filterFn(schemaNode)) {
						let hidden = _Schema.hiddenSchemaNodes.indexOf(schemaNode.name) > -1;
						schemaVisibilityTable.append('<tr><td><input class="toggle-type" data-structr-type="' + schemaNode.name + '" type="checkbox" ' + (hidden ? '' : 'checked') + '></td><td>' + schemaNode.name + '</td></tr>');
					}
				}
			}

			$('input.toggle-all-types', contentEl).on('click', function() {
				let typeTable = $(this).closest('table');
				let checked   = $(this).prop("checked");
				$('.toggle-type', typeTable).each(function(i, checkbox) {
					let inp = $(checkbox);
					inp.prop("checked", checked);
				});
				_Schema.updateHiddenSchemaTypes();
				_Schema.reload();
			});

			$('i.invert-all-types', contentEl).on('click', function() {
				let typeTable = $(this).closest('table');
				$('.toggle-type', typeTable).each(function(i, checkbox) {
					let inp = $(checkbox);
					inp.prop("checked", !inp.prop("checked"));
				});
				_Schema.updateHiddenSchemaTypes();
				_Schema.reload();
			});

			$('td .toggle-type', contentEl).on('click', function(e) {
				e.stopPropagation();
				e.preventDefault();
				return false;
			});

			$('.schema-visibility-table td', contentEl).on('mouseup', function(e) {
				e.stopPropagation();
				e.preventDefault();
				let td  = $(this);
				let inp = $('.toggle-type', td.parent());
				inp.prop("checked", !inp.prop("checked"));
				_Schema.updateHiddenSchemaTypes();
				_Schema.reload();
				return false;
			});

			$('li', ul).off('click').on('click', function(e) {
				e.stopPropagation();
				activateTab($(this).data('name'));
			});

			let activeTab = LSWrapper.getItem(_Schema.activeSchemaToolsSelectedVisibilityTab) || visibilityTables[0].caption;
			activateTab(activeTab);
		}, false, null, 'id,name,isBuiltinType,category');

	},
	updateHiddenSchemaTypes: function() {

		let hiddenTypes = [];

		$('.schema-visibility-table input.toggle-type').each(function(i, checkbox) {
			let inp       = $(checkbox);
			let typeName = inp.attr('data-structr-type');
			let visible  = inp.is(':checked');

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
		let classTree       = {};
		let tmpHierarchy    = {};
		let classnameToId   = {};
		let schemaNodesById = {};

		let insertClassInClassTree = (classObj, tree) => {
			let classes = Object.keys(tree);

			let position = classes.indexOf(classObj.parent);
			if (position !== -1) {

				if (classTree[classObj.name]) {
					tree[classes[position]][classObj.name] = classTree[classObj.name];
					delete(classTree[classObj.name]);
				} else {
					tree[classes[position]][classObj.name] = {};
				}

				return true;

			} else {
				let done = false;
				for (let className of classes) {
					if (!done) {
						done = insertClassInClassTree(classObj, tree[className]);
					}
				}
				return done;
			}
		};

		let printClassTree = ($elem, classTree) => {

			let classes = Object.keys(classTree).sort();

			if (classes.length > 0) {

				let $newUl = $('<ul></ul>').appendTo($elem);

				for (let classname of classes) {

					let idForClassname = classnameToId[classname];
					let isCustomType   = !schemaNodesById?.[idForClassname]?.isBuiltinType;

					let icons = `
						<div class="flex items-center icons-container absolute right-0 top-1">
							${(idForClassname ? _Icons.getSvgIcon('pencil_edit', 16, 16, _Icons.getSvgIconClassesNonColorIcon(['mr-1', 'node-action-icon', 'edit-type-icon'])) : '')}
							${(idForClassname && isCustomType ? _Icons.getSvgIcon('trashcan', 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'mr-1', 'node-action-icon', 'delete-type-icon'])) : '')}
						</div>
					`;

					let $newLi = $(`<li data-jstree='{"opened":true}' data-id="${classnameToId[classname]}">${classname}${icons}</li>`).appendTo($newUl);

					printClassTree($newLi, classTree[classname]);
				}
			}
		};

		let getParentClassName = function (str) {
			if (str.slice(-1) === '>') {
				let res = str.match("([^<]*)<([^>]*)>");
				return getParentClassName(res[1]) + "&lt;" + getParentClassName(res[2]) + "&gt;";

			} else {
				return str.slice(str.lastIndexOf('.') + 1);
			}
		};

		for (let schemaNode of schemaNodes) {

			schemaNodesById[schemaNode.id] = schemaNode;

			let classObj = {
				name: schemaNode.name,
				parent: schemaNode.extendsClass ? schemaNode.extendsClass.name : 'AbstractNode'
			};

			classnameToId[classObj.name] = schemaNode.id;

			let inserted = insertClassInClassTree(classObj, tmpHierarchy);

			if (!inserted) {
				let insertedTmp = insertClassInClassTree(classObj, classTree);

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
		}).on('ready.jstree', (e, data) => {

			$('#inheritance-tree .edit-type-icon').on('click', function(e) {
				let nodeId = $(e.target).closest('li').data('id');
				if (nodeId) {
					_Schema.openEditDialog(nodeId);
				}
			});

			$('#inheritance-tree .delete-type-icon').on('click', function(e) {
				let nodeId = $(e.target).closest('li').data('id');
				if (nodeId) {
					Structr.confirmation(
						`<h3>Delete schema node '${$(e.target).closest('a').text()}'?</h3><p>This will delete all incoming and outgoing schema relationships as well,<br> but no data will be removed.</p>`,
						() => {
							$.unblockUI({
								fadeOut: 25
							});
							_Schema.deleteNode(nodeId);
						}
					);
				}
			});


		}).on('changed.jstree', function(e, data) {
			let $node = $('#id_' + data.node.data.id);
			if ($node.length > 0) {
				$('.selected').removeClass('selected');
				$node.addClass('selected');
				_Schema.ui.selectedNodes = [$node];
			}
		});

		let searchTimeout;
		$('#search-classes').keyup(function(e) {
			if (e.which === 27) {
				$('#search-classes').val('');
				inheritanceTree.jstree(true).clear_search();

			} else {
				if (searchTimeout) {
					clearTimeout(searchTimeout);
				}

				searchTimeout = setTimeout(function () {
					let query = $('#search-classes').val();
					inheritanceTree.jstree(true).search(query, true, true);
				}, 250);
			}
		});
	},
	overlapsExistingNodes: function(position) {
		if (!position) {
			return false;
		}
		let overlaps = false;
		$('.node.schema.compact').each(function(i, node) {
			let offset = $(node).offset();
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
	getDerivedTypes: async (baseType, blacklist) => {

		// baseType is FQCN
		let response = await fetch(Structr.rootUrl + '_schema');

		if (response.ok) {

			let data = await response.json();

			let result    = data.result;
			let fileTypes = [];
			let depth     = 5;
			let types     = {};

			let collect = function(list, type) {

				list.forEach(function(n) {

					if (n.extendsClass === type) {

						fileTypes.push('org.structr.dynamic.' + n.name);

						if (!n.isAbstract && !blacklist.includes(n.name)) {
							types[n.name] = 1;
						}
					} else {
						// console.log({ ext: n.extendsClass, type: type });
					}
				});
			};

			collect(result, baseType);

			for (let i=0; i<depth; i++) {

				fileTypes.forEach(function(type) {

					collect(result, type);
				});
			}

			return Object.keys(types);

		} else {
			return [];
		}
	},
	ui: {
		showInheritance: true,
		showSchemaOverlays: true,
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
			// return 'scale(' + _Schema.ui.zoomLevel + ') translate(' + ((inheritanceSlideout.position().left + inheritanceSlideout.outerWidth()) / _Schema.ui.zoomLevel) + 'px)';
			return 'scale(' + _Schema.ui.zoomLevel + ')';
		},
		updateOverlayVisibility: function(show) {
			_Schema.ui.showSchemaOverlays = show;
			LSWrapper.setItem(_Schema.showSchemaOverlaysKey, show);
			$('#schema-show-overlays').prop('checked', show);
			if (show) {
				$('.rel-type, .multiplicity').show();
			} else {
				$('.rel-type, .multiplicity').hide();
			}
		},
		updateInheritanceVisibility: function(show) {
			_Schema.ui.showInheritance = show;
			LSWrapper.setItem(_Schema.showSchemaInheritanceKey, show);
			$('#schema-show-inheritance').prop('checked', show);
			if (show) {
				canvas.removeClass('hide-inheritance-arrwows');
			} else {
				canvas.addClass('hide-inheritance-arrwows');
			}
		},
		getNodeXPosition: (x, y) => {
			return (x * 300) + ((y % 2) * 150) + 140;
		},
		getNodeYPosition: (y) => {
			return (y * 150) + 150;
		},
		calculateNodePosition: (x, y) => {
			let calculatedX = _Schema.ui.getNodeXPosition(x, y);
			if (calculatedX > 1500) {
				y++;
				x = 0;
				calculatedX = _Schema.ui.getNodeXPosition(x, y);
			}
			let calculatedY = _Schema.ui.getNodeYPosition(y);
			return {
				left: calculatedX,
				top: calculatedY
			};
		}
	},
	filterJavaMethods: function(methods) {
		if (!_Schema.showJavaMethods) {
			return methods.filter(m => m.codeType !== 'java');
		}
		return methods;
	},

	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/schema.css">
			
			<div class="slideout-activator left" id="inheritanceTab">
				<svg viewBox="0 0 28 28" height="28" width="28" xmlns="http://www.w3.org/2000/svg">
					<g transform="matrix(1.1666666666666667,0,0,1.1666666666666667,0,0)"><path d="M22.5,11.25A5.24,5.24,0,0,0,19.45,6.5,2.954,2.954,0,0,0,16.5,3c-.063,0-.122.015-.185.019a5.237,5.237,0,0,0-8.63,0C7.622,3.015,7.563,3,7.5,3A2.954,2.954,0,0,0,4.55,6.5a5.239,5.239,0,0,0,1.106,9.885A4.082,4.082,0,0,0,12,17.782a4.082,4.082,0,0,0,6.344-1.4A5.248,5.248,0,0,0,22.5,11.25Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M12 8.25L12 23.25" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M12,15q4.5,0,4.5-4.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M12,12A3.543,3.543,0,0,1,8.25,8.25" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path></g>
				</svg>
				Inheri-<br>tance
			</div>
			
			<div id="inheritance-tree" class="slideOut slideOutLeft">
				<div class="inheritance-search">Search: <input type="text" id="search-classes" autocomplete="off"></div>
				<div id="inheritance-tree-container" class="ver-scrollable hidden"></div>
			</div>
			
			<div id="schema-container">
				<div class="canvas" id="schema-graph"></div>
			</div>
		`,
		functions: config => `
			<div class="flex-grow">
				<div class="inline-flex">
			
					<input class="schema-input mr-2" id="type-name" type="text" size="10" placeholder="New type" autocomplete="off">
			
					<button id="create-type" class="action inline-flex items-center">
						${_Icons.getSvgIcon('circle_plus', 16, 16, ['mr-2'])} Add
					</button>
			
					<div class="dropdown-menu dropdown-menu-large">
						<button class="btn dropdown-select" id="global-schema-methods">${_Icons.getSvgIcon('globe-icon', 16, 16, '')} Global Methods</button>
					</div>
			
					<div class="dropdown-menu dropdown-menu-large">
						<button class="btn dropdown-select">${_Icons.getSvgIcon('network-icon', 16, 16, '')} Display</button>
			
						<div class="dropdown-menu-container">
							<div class="row">
								<a title="Open dialog to show/hide the data types" id="schema-tools" class="flex items-center">
									${_Icons.getSvgIcon('eye-in-square', 16, 16, 'mr-2')} Type Visibility
								</a>
							</div>
			
							<div class="separator"></div>
			
							<div class="heading-row">
								<h3>Display Options</h3>
							</div>
							<div class="row">
								<label class="block"><input ${_Schema.ui.showSchemaOverlays ? 'checked' : ''} type="checkbox" id="schema-show-overlays" name="schema-show-overlays"> Relationship labels</label>
							</div>
							<div class="row">
								<label class="block"><input ${_Schema.ui.showInheritance    ? 'checked' : ''} type="checkbox" id="schema-show-inheritance" name="schema-show-inheritance"> Inheritance arrows</label>
							</div>
			
							<div class="separator"></div>
			
							<div class="heading-row">
								<h3>Edge Style</h3>
							</div>
			
							<div class="row">
								<a class="block edge-style ${_Schema.ui.connectorStyle === 'Flowchart'    ? 'active' : ''}"> Flowchart</a>
							</div>
							<div class="row">
								<a class="block edge-style ${_Schema.ui.connectorStyle === 'Bezier'       ? 'active' : ''}"> Bezier</a>
							</div>
							<div class="row">
								<a class="block edge-style ${_Schema.ui.connectorStyle === 'StateMachine' ? 'active' : ''}"> StateMachine</a>
							</div>
							<div class="row">
								<a class="block edge-style ${_Schema.ui.connectorStyle === 'Straight'     ? 'active' : ''}"> Straight</a>
							</div>
			
							<div class="separator"></div>
			
							<div class="heading-row">
								<h3>Saved Layouts</h3>
							</div>
			
							<div class="row">
								<select id="saved-layout-selector"></select>
								<button id="restore-layout">Apply</button>
								<button id="update-layout">Update</button>
								<button id="delete-layout">Delete</button>
							</div>
			
							<div class="row">
								<input id="layout-name" placeholder="Enter name for layout"> <button id="create-new-layout">Save</button>
							</div>
			
							<div class="separator"></div>
			
							<div class="row">
								<a title="Reset the stored node positions and apply an automatic layouting algorithm." id="reset-schema-positions" class="flex items-center">
									${_Icons.getSvgIcon('reset-arrow', 16, 16, 'mr-2')} Reset Layout (apply Auto-Layouting)
								</a>
							</div>
						</div>
					</div>
			
					<div class="dropdown-menu dropdown-menu-large">
						<button class="btn dropdown-select">${_Icons.getSvgIcon('snapshots-icon', 16, 16, '')} Snapshots</button>

						<div class="dropdown-menu-container">
							<div class="heading-row">
								<h3>Create snapshot</h3>
							</div>
							<div class="row">Creates a new snapshot of the current schema configuration that can be restored later.<br>You can enter an (optional) suffix for the snapshot.</div>
							
							<div class="row">
								<input type="text" name="suffix" id="snapshot-suffix" placeholder="Enter a suffix" length="20"> <button id="create-snapshot">Create snapshot</button>
							</div>
							
							<div class="heading-row">
								<h3>Available Snapshots</h3>
							</div>
							
							<table class="props" id="snapshots"></table>
							
							<div class="separator"></div>
							
							<div class="row">
								<a id="refresh-snapshots" class="block">Reload stored snapshots</a>
							</div>
						</div>
					</div>
			
					<div class="dropdown-menu dropdown-menu-large">
						<button class="btn dropdown-select">${_Icons.getSvgIcon('settings-cog', 16, 16, '')} Admin</button>
						
						<div class="dropdown-menu-container">
							<div class="heading-row">
								<h3>Indexing</h3>
							</div>
							<div class="row">
								<select id="node-type-selector">
									<option selected value="">-- Select Node Type --</option>
									<option disabled>──────────</option>
									<option value="allNodes">All Node Types</option>
									<option disabled>──────────</option>
								</select>
								<button id="reindex-nodes" class="inline-flex items-center">Rebuild node index</button>
								<button id="add-node-uuids" class="inline-flex items-center">Add UUIDs</button>
								<button id="create-labels" class="inline-flex items-center">Create Labels</button>
							</div>
							<div class="row">
								<select id="rel-type-selector">
									<option selected value="">-- Select Relationship Type --</option>
									<option disabled>──────────</option>
									<option value="allRels">All Relationship Types</option>
									<option disabled>──────────</option>
								</select>
								<button id="reindex-rels" class="inline-flex items-center">Rebuild relationship index</button>
								<button id="add-rel-uuids" class="inline-flex items-center">Add UUIDs</button>
							</div>
							<div class="row flex items-center">
								<button id="rebuild-index" class="inline-flex items-center">
									${_Icons.getSvgIcon('refresh-arrows', 16, 16, 'mr-2')} Rebuild all indexes
								</button>
								<label for="rebuild-index">Rebuild indexes for entire database (all node and relationship indexes)</label>
							</div>
							<div class="separator"></div>
							<div class="heading-row"><h3>Maintenance</h3></div>
							<!--<div class="inline-info"><div class="inline-info-icon">${_Icons.getSvgIcon('info-icon', 24, 24)}</div>-->
							<!--	<div class="inline-info-text">-->
							<!--		A repeater is a node in the tree that is repeatedly displayed for each element of the result set.<br><br>-->
							<!--		The result set is a list or collection of objects that can be generated via a database query, a flow or a function.<br><br>-->
							<!--		The respective result element can be accessed via a keyword configured further below.-->
							<!--	</div>-->
							<!--</div>-->
							<div class="row flex items-center">
								<button id="flush-caches" class="inline-flex items-center">
									${_Icons.getSvgIcon('refresh-arrows', 16, 16, 'mr-2')} Flush Caches
								</button>
								<label for="flush-caches">Flushes internal caches to refresh schema information</label>
							</div>
							
							<div class="row flex items-center">
								<button id="clear-schema" class="inline-flex items-center">
									${_Icons.getSvgIcon('trashcan', 16, 16, 'mr-2 icon-red')} Clear Schema
								</button>
								<label for="clear-schema">Delete all schema nodes and relationships in custom schema</label>
							</div>
							
							<div class="separator"></div>
							
							<div class="row">
								<label class="block"><input type="checkbox" id="show-java-methods-in-schema-checkbox"> Show Java methods in SchemaNode</label>
							</div>
						</div>
					</div>
				</div>
			</div>
			
			<div id="zoom-slider"></div>
		`,
		relationshipDialog: config => `
			<div class="schema-details">
				<div id="relationship-options">
			
						<div id="basic-options" class="grid grid-cols-5 gap-y-2 items-center">
			
							<div class="text-right pb-2 truncate">
								<span id="source-type-name" class="edit-schema-object relationship-emphasis"></span>
							</div>
			
							<div class="flex items-center justify-around">
								<div class="overflow-hidden whitespace-nowrap">&#8212;</div>
			
								<select id="source-multiplicity-selector" data-attr-name="sourceMultiplicity">
									<option value="1">1</option>
									<option value="*" selected>*</option>
								</select>
			
								<div class="overflow-hidden whitespace-nowrap">&#8212;[</div>
							</div>
			
							<input id="relationship-type-name" data-attr-name="relationshipType" autocomplete="off">
			
							<div class="flex items-center justify-around">
								<div class="overflow-hidden whitespace-nowrap">]&#8212;</div>
			
								<select id="target-multiplicity-selector" data-attr-name="targetMultiplicity">
									<option value="1">1</option>
									<option value="*" selected>*</option>
								</select>
			
								<div class="overflow-hidden whitespace-nowrap">&#8212;&#9658;</div>
							</div>
			
							<div class="text-left pb-2 truncate">
								<span id="target-type-name" class="edit-schema-object relationship-emphasis"></span>
							</div>
			
							<div></div>
							<input id="source-json-name" class="remote-property-name" data-attr-name="sourceJsonName" autocomplete="off">
							<div></div>
							<input id="target-json-name" class="remote-property-name" data-attr-name="targetJsonName" autocomplete="off">
							<div></div>
						</div>
			
						<div class="grid grid-cols-2 gap-x-4">
			
							<div id="cascading-options">
								<h3>Cascading Delete</h3>
								<p>Direction of automatic removal of related nodes when a node is deleted</p>
			
								<select id="cascading-delete-selector" data-attr-name="cascadingDeleteFlag">
									<option value="0">NONE</option>
									<option value="1">SOURCE_TO_TARGET</option>
									<option value="2">TARGET_TO_SOURCE</option>
									<option value="3">ALWAYS</option>
									<option value="4">CONSTRAINT_BASED</option>
								</select>
			
								<h3>Automatic Creation of Related Nodes</h3>
								<p>Direction of automatic creation of related nodes when a node is created</p>
			
								<select id="autocreate-selector" data-attr-name="autocreationFlag">
									<option value="0">NONE</option>
									<option value="1">SOURCE_TO_TARGET</option>
									<option value="2">TARGET_TO_SOURCE</option>
									<option value="3">ALWAYS</option>
								</select>
							</div>
			
							<div id="propagation-options">
			
								<div>
									<h3>Permission Resolution</h3>
									<select id="propagation-selector" data-attr-name="permissionPropagation">
										<option value="None">NONE</option>
										<option value="Out">SOURCE_TO_TARGET</option>
										<option value="In">TARGET_TO_SOURCE</option>
										<option value="Both">ALWAYS</option>
									</select>
								</div>
			
								<div class="mt-4">
									<div id="propagation-table" class="flex">
										<div class="selector">
											<p>Read</p>
											<select id="read-selector" data-attr-name="readPropagation">
												<option value="Add">Add</option>
												<option value="Keep">Keep</option>
												<option value="Remove" selected>Remove</option>
											</select>
										</div>
			
										<div class="selector">
											<p>Write</p>
											<select id="write-selector" data-attr-name="writePropagation">
												<option value="Add">Add</option>
												<option value="Keep">Keep</option>
												<option value="Remove" selected>Remove</option>
											</select>
										</div>
			
										<div class="selector">
											<p>Delete</p>
											<select id="delete-selector" data-attr-name="deletePropagation">
												<option value="Add">Add</option>
												<option value="Keep">Keep</option>
												<option value="Remove" selected>Remove</option>
											</select>
										</div>
			
										<div class="selector">
											<p>AccessControl</p>
											<select id="access-control-selector" data-attr-name="accessControlPropagation">
												<option value="Add">Add</option>
												<option value="Keep">Keep</option>
												<option value="Remove" selected>Remove</option>
											</select>
										</div>
									</div>
								</div>
			
								<p class="mt-4">Hidden properties</p>
								<textarea id="masked-properties" cols="40" rows="2" data-attr-name="propertyMask"></textarea>
							</div>
						</div>
					</div>
				</div>
			
				<div id="rel-edit-buttons">
					<button id="edit-rel-options-button" class="inline-flex items-center">${_Icons.getSvgIcon('pencil_edit', 16, 16, ['icon-grey', 'mr-2'])} Edit relationship options</button>
					<button id="save-rel-options-button" class="inline-flex items-center">${_Icons.getSvgIcon('checkmark_bold', 16, 16, ['icon-green', 'mr-2'])} Save</button>
					<button id="cancel-rel-options-button" class="inline-flex items-center">${_Icons.getSvgIcon('close-dialog-x', 16, 16, ['icon-red', 'mr-2'])} Discard</button>
				</div>
			
				<div id="tabs">
					<ul></ul>
				</div>
			</div>
		`,
		methods: config => `
			<div id="methods-container" class="${config.class} flex">
			
				<div id="methods-container-left">
			
					<div id="methods-table-container"></div>
			
				</div>
			
				<div id="methods-container-right" class="flex flex-col flex-grow">
					<div id="methods-content" class="flex-grow">
						<div class="editor h-full"></div>
					</div>
				</div>
			
			</div>
		`,
		method: config => `
			<div class="fake-tr${(config.isNew ? ' has-changes' : '')}" data-method-id="${config.method.id}">
				<div class="fake-td name-col"><input size="15" type="text" class="action property-name" placeholder="Enter method name" value="${config.method.name}"></div>
				<div class="fake-td isstatic-col"><input type="checkbox" class="action property-isStatic" value="${config.method.isStatic}"></div>
				<div class="fake-td actions-col">
					${_Icons.getSvgIcon('pencil_edit', 16, 16, _Icons.getSvgIconClassesNonColorIcon(['edit-action']))}
					${_Icons.getSvgIcon('duplicate', 16, 16, _Icons.getSvgIconClassesNonColorIcon(['clone-action']))}
					${_Icons.getSvgIcon('close-dialog-x', 16, 16,  _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']))}
					${config.isNew ? '' : _Icons.getSvgIcon('trashcan', 16, 16,    _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-action']))}
				</div>
			</div>
		`,
		propertyBuiltin: config => `
			<tr data-property-name="${config.property.name}">
				<td>${escapeForHtmlAttributes(config.property.declaringClass)}</td>
				<td>${escapeForHtmlAttributes(config.property.name)}</td>
				<td>${config.property.propertyType}</td>
				<td class="centered"><input class="not-null" type="checkbox" disabled="disabled" ${(config.property.notNull ? 'checked' : '')}></td>
				<td class="centered"><input class="compound" type="checkbox" disabled="disabled" ${(config.property.compound ? 'checked' : '')}></td>
				<td class="centered"><input class="unique" type="checkbox" disabled="disabled" ${(config.property.unique ? 'checked' : '')}></td>
				<td class="centered"><input class="indexed" type="checkbox" disabled="disabled" ${(config.property.indexed ? 'checked' : '')}></td>
			</tr>
		`,
		propertyLocal: config => `
			<tr data-property-id="${config.property.id}" >
				<td><input size="15" type="text" class="property-name" value="${escapeForHtmlAttributes(config.property.name)}"></td>
				<td><input size="15" type="text" class="property-dbname" value="${escapeForHtmlAttributes(config.property.dbName)}"></td>
				<td>${config.typeOptions}</td>
				<td>
					${
						(() => {
							switch (config.property.propertyType) {
								case 'Function':
									return `<button class="edit-read-function">Read</button><button class="edit-write-function">Write</button><input id="checkbox-${config.property.id}" class="caching-enabled" type="checkbox" ${(config.property.isCachingEnabled ? 'checked' : '')}><label for="checkbox-${config.property.id}">Cache</label>${config.typeHintOptions}`;
								case 'Cypher':
									return `<button class="edit-cypher-query">Query</button>`;
								default:
									return `<input size="15" type="text" class="property-format" value="${(config.property.format ? escapeForHtmlAttributes(config.property.format) : '')}">`;
							};
						})()
					}
				</td>
				<td class="centered"><input class="not-null" type="checkbox" ${(config.property.notNull ? 'checked' : '')}></td>
				<td class="centered"><input class="compound" type="checkbox" ${(config.property.compound ? 'checked' : '')}></td>
				<td class="centered"><input class="unique" type="checkbox" ${(config.property.unique ? 'checked' : '')}></td>
				<td class="centered"><input class="indexed" type="checkbox" ${(config.property.indexed ? 'checked' : '')}></td>
				<td><input type="text" size="10" class="property-default" value="${escapeForHtmlAttributes(config.property.defaultValue)}"></td>
				<td class="centered actions-col">
					${config.property.isBuiltinProperty ? '' : _Icons.getSvgIcon('close-dialog-x', 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']))}
					${config.property.isBuiltinProperty ? '' : _Icons.getSvgIcon('trashcan', 16, 16,   _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-action']))}
				</td>
			</tr>
		`,
		propertyNew: config => `
			<tr class="has-changes">
				<td><input size="15" type="text" class="property-name" placeholder="Enter JSON name" autofocus></td>
				<td><input size="15" type="text" class="property-dbname" placeholder="Enter DB name"></td>
				<td>${config.typeOptions}</td>
				<td><input size="15" type="text" class="property-format" placeholder="Enter format"></td>
				<td class="centered"><input class="not-null" type="checkbox"></td>
				<td class="centered"><input class="compound" type="checkbox"></td>
				<td class="centered"><input class="unique" type="checkbox"></td>
				<td class="centered"><input class="indexed" type="checkbox"></td>
				<td><input class="property-default" size="10" type="text"></td>
				<td class="centered">
					${_Icons.getSvgIcon('close-dialog-x', 16, 16,  _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']), 'Remove')}
				</td>
			</tr>
		`,
		remoteProperty: config => `
			<tr data-relationship-id="${config.rel.id}" data-property-name="${config.propertyName}" data-target-collection="${config.targetCollection}">
				<td><input size="15" type="text" class="property-name related" value="${config.attributeName}" /></td>
				<td>
					${config.arrowLeft}&mdash;<i class="cardinality ${config.cardinalityClassLeft}"></i>&mdash;[:<span class="edit-schema-object" data-object-id="${config.rel.id}">${config.relType}</span>]&mdash;<i class="cardinality ${config.cardinalityClassRight}"></i>&mdash;${config.arrowRight}
					<span class="edit-schema-object" data-object-id="${config.relatedNodeId}">${config.relatedNodeType}</span>
				</td>
				<td class="centered">
					${_Icons.getSvgIcon('close-dialog-x', 16, 16,  _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']))}
					${_Icons.getSvgIcon('reset-arrow', 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-green', 'reset-action']))}
				</td>
			</tr>
		`,
		fakeTable: config => `
			<div class="fake-table ${config.class}">
				<div class="fake-thead"><div class="fake-tr">${config.cols.reduce((acc, col) => { return acc + '<div class="fake-th ' + col.class + '">' + col.title + '</div>'; }, '')}</div></div>
				<div class="fake-tbody"></div>
				<div class="fake-tfoot">
					<div class="fake-tr">
						<div class="fake-td actions-col flex justify-end">
							<button class="discard-all inline-flex items-center disabled" disabled>${_Icons.getSvgIcon('close-dialog-x', 16, 16, 'icon-red mr-2')} Discard all</button>
							<button class="save-all inline-flex items-center disabled" disabled>${_Icons.getSvgIcon('checkmark_bold', 16, 16, 'icon-green mr-2')} Save all</button>
						</div>
					</div>
				</div>
				<div class="fake-tfoot-buttons">
					<button class="add-icon inline-flex items-center add-action-button">${_Icons.getSvgIcon('circle_plus', 16, 16, 'icon-green mr-2')} Add method</button>
					<button class="add-icon inline-flex items-center add-onCreate-button">${_Icons.getSvgIcon('circle_plus', 16, 16, 'icon-green mr-2')} Add onCreate</button>
					<button class="add-icon inline-flex items-center add-afterCreate-button">${_Icons.getSvgIcon('circle_plus', 16, 16, 'icon-green mr-2')} Add afterCreate</button>
					<button class="add-icon inline-flex items-center add-onSave-button">${_Icons.getSvgIcon('circle_plus', 16, 16, 'icon-green mr-2')} Add onSave</button>
				</div>
			</div>
		`,
		schemaTable: config => `
			<table class="${config.class}">
				<thead><tr>${config.cols.reduce((acc, col) => { return acc + '<th class="' + col.class + '">' + col.title + '</th>'; }, '')}</tr></thead>
				<tbody></tbody>
				<tfoot>
					<th colspan=${config.cols.length} class="actions-col">
						${(config.addButtonText ? '<button class="add-button inline-flex items-center">' + _Icons.getSvgIcon('circle_plus', 16, 16, 'icon-green mr-2') + config.addButtonText + '</button>' : '')}
						<button class="discard-all inline-flex items-center disabled" disabled>${_Icons.getSvgIcon('close-dialog-x', 16, 16, 'icon-red mr-2')} ${(config.discardButtonText ? config.discardButtonText : 'Discard all')}</button>
						<button class="save-all inline-flex items-center disabled" disabled>${_Icons.getSvgIcon('checkmark_bold', 16, 16, 'icon-green mr-2')} ${(config.discardButtonText ? config.saveButtonText : 'Save all')}</button>
					</th>
				</tfoot>
			</table>
		`,
		typeHintOptions: config => `
			<select class="type-hint">
				<optgroup label="Type Hint">
					<option value="null">-</option>
					<option value="boolean">Boolean</option>
					<option value="string">String</option>
					<option value="int">Int</option>
					<option value="long">Long</option>
					<option value="double">Double</option>
					<option value="date">Date</option>
				</optgroup>
			</select>
		`,
		typeOptions: config => `
			<select class="property-type">
				<option value="">--Select--</option>
				<option value="String">String</option>
				<option value="Encrypted">Encrypted</option>
				<option value="StringArray">String[]</option>
				<option value="Integer">Integer</option>
				<option value="IntegerArray">Integer[]</option>
				<option value="Long">Long</option>
				<option value="LongArray">Long[]</option>
				<option value="Double">Double</option>
				<option value="DoubleArray">Double[]</option>
				<option value="Boolean">Boolean</option>
				<option value="BooleanArray">Boolean[]</option>
				<option value="Enum">Enum</option>
				<option value="Date">Date</option>
				<option value="DateArray">Date[]</option>
				<option value="Count">Count</option>
				<option value="Function" data-indexed="false">Function</option>
				<option value="Notion">Notion</option>
				<option value="Join">Join</option>
				<option value="Cypher" data-indexed="false">Cypher</option>
				<option value="Thumbnail">Thumbnail</option>
				<option value="IdNotion" data-protected="true" disabled>IdNotion</option>
				<option value="Custom" data-protected="true" disabled>Custom</option>
				<option value="Password" data-protected="true" disabled>Password</option>
			</select>
		`,
		view: config => `
			<tr data-view-id="${config.view.id}" >
				<td style="width:20%;">
					<input size="15" type="text" class="view property-name" placeholder="Enter view name" value="${(config.view ? escapeForHtmlAttributes(config.view.name) : '')}" ${(config.view && config.view.isBuiltinView ? 'disabled' : '')}>
				</td>
				<td class="view-properties-select"></td>
				<td class="centered actions-col">
					${_Icons.getSvgIcon('close-dialog-x', 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']))}
					${_Icons.getSvgIcon('trashcan', 16, 16,   _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-action']))}
			
					<a href="/structr/rest/${config.type.name}/${config.view.name}" target="_blank">
						${_Icons.getSvgIcon('link_external', 16, 16, _Icons.getSvgIconClassesNonColorIcon(), 'Preview (with pageSize=1)')}
					</a>
				</td>
			</tr>
		`,
		viewNew: config => `
			<tr class="has-changes">
				<td style="width:20%;">
					<input size="15" type="text" class="view property-name" placeholder="Enter view name">
				</td>
				<td class="view-properties-select"></td>
				<td class="centered actions">
					${_Icons.getSvgIcon('close-dialog-x', 16, 16,  _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']), 'Discard changes')}
				</td>
			</tr>
		`,
	}
};