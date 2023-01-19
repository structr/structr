/*
 * Copyright (C) 2010-2022 Structr GmbH
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
$(document).ready(function() {
	Structr.registerModule(_Schema);
	Structr.classes.push('schema');

	Command.getApplicationConfigurationDataNodesGroupedByUser('layout', (grouped) => {
		_Schema.storedLayouts = grouped;
	});
});

let _Schema = {
	_moduleName: 'schema',
	isReloading: false,
	storedLayouts: [],
	undefinedRelType: 'UNDEFINED_RELATIONSHIP_TYPE',
	initialRelType: 'UNDEFINED_RELATIONSHIP_TYPE',
	schemaLoading: false,
	nodeData: {},
	nodePositions: undefined,
	availableTypeNames: [],
	hiddenSchemaNodes: [],
	hiddenSchemaNodesKey: 'structrHiddenSchemaNodes_' + location.port,
	schemaPositionsKey: 'structrSchemaPositions_' + location.port,
	showSchemaOverlaysKey: 'structrShowSchemaOverlays_' + location.port,
	showSchemaInheritanceKey: 'structrShowSchemaInheritance_' + location.port,
	showBuiltinTypesInInheritanceTreeKey: 'showBuiltinTypesInInheritanceTreeKey_' + location.port,
	showJavaMethodsKey: 'structrShowJavaMethods_' + location.port,
	schemaMethodsHeightsKey: 'structrSchemaMethodsHeights_' + location.port,
	schemaActiveTabLeftKey: 'structrSchemaActiveTabLeft_' + location.port,
	activeSchemaToolsSelectedTabLevel1Key: 'structrSchemaToolsSelectedTabLevel1_' + location.port,
	activeSchemaToolsSelectedVisibilityTab: 'activeSchemaToolsSelectedVisibilityTab_' + location.port,
	schemaZoomLevelKey: '_schema_' + location.port + 'zoomLevel',
	schemaConnectorStyleKey: '_schema_' + location.port + 'connectorStyle',
	schemaNodePositionKeySuffix: '_schema_' + location.port + 'node-position',
	currentNodeDialogId: null,
	showJavaMethods: false,
	inheritanceTree: undefined,
	inheritanceSlideout: undefined,
	onload: () => {

		_Code.preloadAvailableTagsForEntities().then(() => {

			Structr.mainContainer.innerHTML = _Schema.templates.main();
			Structr.activateCommentsInElement(Structr.mainContainer);

			_Schema.inheritanceSlideout     = $('#inheritance-tree');
			_Schema.inheritanceTree         = $('#inheritance-tree-container');
			_Schema.ui.canvas               = $('#schema-graph');

			_Schema.ui.canvas[0].addEventListener('mousedown', (e) => {
				if (e.which === 1) {
					_Schema.ui.clearSelection();
					_Schema.ui.selectionStart(e);
				}
			});

			_Schema.ui.canvas[0].addEventListener('mousemove', (e) => {
				if (e.which === 1) {
					_Schema.ui.selectionDrag(e);
				}
			});

			_Schema.ui.canvas[0].addEventListener('mouseup', (e) => {
				_Schema.ui.selectionStop();
			});

			let inheritanceTab = $('#inheritanceTab');
			inheritanceTab.on('click', () => {
				_Pages.leftSlideoutTrigger(inheritanceTab, _Schema.inheritanceSlideout, [], (params) => {
					LSWrapper.setItem(_Schema.schemaActiveTabLeftKey, inheritanceTab.prop('id'));
					_Schema.inheritanceTree.show();
				}, () => {
					LSWrapper.removeItem(_Schema.schemaActiveTabLeftKey);
					_Schema.inheritanceTree.hide();
				});
			});

			if (LSWrapper.getItem(_Schema.schemaActiveTabLeftKey)) {
				$('#' + LSWrapper.getItem(_Schema.schemaActiveTabLeftKey)).click();
			}

			_Schema.init(() => {
				_Schema.resize();
			});

			Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('schema'));

			$(window).off('resize').on('resize', () => {
				_Schema.resize();
			});
		});
	},
	reload: (callback) => {

		_Schema.clearTypeInfoCache();

		if (_Schema.isReloading) {
			return;
		}

		_Schema.isReloading = true;
		//_Schema.storePositions();	/* CHM: don't store positions on every reload, let automatic positioning do its job.. */

		fastRemoveAllChildren(_Schema.ui.canvas[0]);
		_Schema.init({ x: window.scrollX, y: window.scrollY }, callback);
		_Schema.resize();

	},
	storePositions: () => {
		for (let n of _Schema.ui.canvas[0].querySelectorAll('.node')) {
			let node = $(n);
			let type = node.children('b').text();
			let obj = node.offset();
			obj.left = (obj.left) / _Schema.ui.zoomLevel;
			obj.top  = (obj.top)  / _Schema.ui.zoomLevel;
			_Schema.nodePositions[type] = obj;
		}
		LSWrapper.setItem(_Schema.schemaPositionsKey, _Schema.nodePositions);
	},
	clearPositions: () => {
		LSWrapper.removeItem(_Schema.schemaPositionsKey);
		_Schema.reload();
	},
	init: (scrollPosition, callback) => {

		_Schema.schemaLoading = false;
		_Schema.schema        = [];
		_Schema.keys          = [];

		_Schema.ui.connectorStyle     = LSWrapper.getItem(_Schema.schemaConnectorStyleKey) || 'Flowchart';
		_Schema.ui.zoomLevel          = parseFloat(LSWrapper.getItem(_Schema.schemaZoomLevelKey)) || 1.0;
		_Schema.ui.showInheritance    = LSWrapper.getItem(_Schema.showSchemaInheritanceKey, true) || true;
		_Schema.showJavaMethods       = LSWrapper.getItem(_Schema.showJavaMethodsKey, false) || false;
		Structr.functionBar.innerHTML = _Schema.templates.functions();

		//UISettings.showSettingsForCurrentModule();

		_Schema.activateDisplayDropdownTools();
		_Schema.activateSnapshotsDialog();
		_Schema.activateAdminTools();

		let typeNameInput       = document.getElementById('type-name');
		let createNewTypeButton = document.getElementById('create-type');

		createNewTypeButton.addEventListener('click', async () => {
			await _Schema.createNode(typeNameInput.value);
		});

		typeNameInput.addEventListener('keyup', (e) => {

			if (e.keyCode === 13 || e.code === 'Enter') {
				e.preventDefault();
				if (typeNameInput.value.length) {
					createNewTypeButton.click();
					typeNameInput.blur();
				}
				return false;
			}
		});

		document.getElementById('global-schema-methods').addEventListener('click', _Schema.methods.showGlobalSchemaMethods);

		$('#zoom-slider').slider({
			min: 0.25,
			max: 1,
			step: 0.05,
			value: _Schema.ui.zoomLevel,
			slide: (e, ui) => {
				_Schema.ui.zoomLevel = ui.value;
				LSWrapper.setItem(_Schema.schemaZoomLevelKey, _Schema.ui.zoomLevel);
				_Schema.ui.setZoom(_Schema.ui.zoomLevel, _Schema.ui.jsPlumbInstance, [0,0], _Schema.ui.canvas[0]);
				if (_Schema.ui.selectedNodes.length > 0) {
					_Schema.ui.updateSelectedNodes();
				}
				_Schema.resize();
			}
		});

		jsPlumb.ready(async () => {

			if (_Schema.ui.jsPlumbInstance) {
				_Schema.ui.jsPlumbInstance.unbindContainer();
			}

			_Schema.ui.jsPlumbInstance = jsPlumb.getInstance({
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
					["PlainArrow", { location: 1, width: 15, length: 12 }]
				]
			});

			await _Schema.loadSchema();

			$('.node').css({ zIndex: ++_Schema.ui.maxZ });

			_Schema.ui.jsPlumbInstance.bind('connection', (info, originalEvent) => {

				if (info.connection.scope === 'jsPlumb_DefaultScope') {
					if (originalEvent) {

						_Schema.relationships.createRelationship(info);
					}
				} else {
					new MessageBuilder().warning('Moving existing relationships is not permitted!').title('Not allowed').requiresConfirmation().show();
					_Schema.reload();
				}
			});

			_Schema.ui.jsPlumbInstance.bind('connectionDetached', (info) => {

				if (info.connection.scope !== 'jsPlumb_DefaultScope') {
					new MessageBuilder().warning('Deleting relationships is only possible via the delete button!').title('Not allowed').requiresConfirmation().show();
					_Schema.reload();
				}
			});
			_Schema.isReloading = false;

			_Schema.ui.setZoom(_Schema.ui.zoomLevel, _Schema.ui.jsPlumbInstance, [0,0], _Schema.ui.canvas[0]);

			$('._jsPlumb_connector').click(function(e) {
				e.stopPropagation();
				_Schema.ui.selectRel($(this));
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

		Structr.adaptUiToAvailableFeatures();
	},
	showSchemaRecompileMessage: () => {
		Structr.showNonBlockUILoadingMessage('Schema is compiling', 'Please wait...');
	},
	hideSchemaRecompileMessage:  () => {
		Structr.hideNonBlockUILoadingMessage();
	},
	loadSchema: async () => {

		// Avoid duplicate loading of schema
		if (_Schema.schemaLoading) {
			return;
		}
		_Schema.schemaLoading = true;

		await _Schema.nodes.loadNodes();
		await _Schema.relationships.loadRels();
	},
	processSchemaRecompileNotification: () => {

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
	reloadSchemaAfterRecompileNotification: () => {

		if (_Schema.currentNodeDialogId !== null) {

			// we break the current dialog the hard way (because if we 'click' the close button we might re-open the previous dialog
			$.unblockUI({
				fadeOut: 25
			});

			let currentView = LSWrapper.getItem(_Entities.activeEditTabPrefix  + '_' + _Schema.currentNodeDialogId);

			_Schema.reload(() => {
				_Schema.openEditDialog(_Schema.currentNodeDialogId, currentView);
			});

		} else {

			_Schema.reload();
		}
	},
	getFirstSchemaLayoutOrFalse: async () => {

		if (!_Schema.hiddenSchemaNodes) {

			let response = await fetch(Structr.rootUrl + 'ApplicationConfigurationDataNode/ui?configType=layout');
			let data     = await response.json();

			if (data.result.length > 0) {

				return data.result[0].content;

			} else {

				_Schema.hiddenSchemaNodes = [];
			}
		}

		return false;
	},
	openEditDialog: (id, targetView, callback) => {

		targetView = targetView || LSWrapper.getItem(_Entities.activeEditTabPrefix  + '_' + id) || 'basic';

		_Schema.currentNodeDialogId = id;

		dialogMeta.hide();
		Command.get(id, null, (entity) => {

			let title = (entity.type === "SchemaRelationshipNode") ? `(:${_Schema.nodeData[entity.sourceId].name})-[:${entity.relationshipType}]-&gt;(:${_Schema.nodeData[entity.targetId].name})` : entity.name;

			let callbackCancel = () => {
				_Schema.currentNodeDialogId = null;

				if (callback) {
					callback();
				}
				dialogMeta.show();
				_Schema.ui.jsPlumbInstance.repaintEverything();
			};

			Structr.dialog(title, () => {
				dialogMeta.show();
			}, callbackCancel, ['schema-edit-dialog']);

			let tabControls;

			if (entity.type === "SchemaRelationshipNode") {
				tabControls = _Schema.relationships.loadRelationship(entity, dialogHead, dialogText, _Schema.nodeData[entity.sourceId], _Schema.nodeData[entity.targetId], targetView, callbackCancel);
			} else {
				tabControls = _Schema.nodes.loadNode(entity, dialogHead, dialogText, targetView, callbackCancel);
			}

			// remove bulk edit save/discard buttons
			for (let button of dialogText[0].querySelectorAll('.discard-all, .save-all')) {
				button.remove();
			}

			let actionButtons = Structr.createSingleDOMElementFromHTML(_Schema.templates.schemaActionButtons({ saveButtonText: 'Save All', discardButtonText: 'Discard All' }));
			dialogBtn[0].prepend(actionButtons);

			let saveButton   = actionButtons.querySelector('#save-entity-button');
			let cancelButton = actionButtons.querySelector('#discard-entity-changes-button');

			saveButton.addEventListener('click', async (e) => {
				let ok = await _Schema.bulkDialogsGeneral.saveEntityFromTabControls(id, tabControls);

				if (ok) {
					_Schema.openEditDialog(id);
				}
			});

			cancelButton.addEventListener('click', (e) => {
				_Schema.bulkDialogsGeneral.resetInputsViaTabControls(tabControls);
			});

			let disableButtons = () => {
				saveButton.disabled = true;
				cancelButton.disabled = true;

				saveButton.classList.add('disabled');
				cancelButton.classList.add('disabled');
			};
			disableButtons();

			let enableButtons = () => {
				saveButton.removeAttribute('disabled');
				cancelButton.removeAttribute('disabled');

				saveButton.classList.remove('disabled');
				cancelButton.classList.remove('disabled');
			};

			dialogText[0].childNodes[0].addEventListener('bulk-data-change', (e) => {

				e.stopPropagation();

				let changeCount = _Schema.bulkDialogsGeneral.getChangeCountFromBulkInfo(_Schema.bulkDialogsGeneral.getBulkInfoFromTabControls(tabControls, false));
				if (changeCount > 0) {
					enableButtons();
				} else {
					disableButtons();
				}
			});

			_Schema.ui.clearSelection();
		}, 'schema');

	},
	bulkDialogsGeneral: {
		closeWithoutSavingChangesQuestionOpen: false,
		overrideDialogCancel: (mainTabs, additionalCallback) => {

			dialogCancelButton.off('click').on('click', async () => {

				if (_Schema.bulkDialogsGeneral.closeWithoutSavingChangesQuestionOpen === false) {

					let allowNavigation = true;
					let hasChanges      = _Schema.bulkDialogsGeneral.hasUnsavedChangesInTabs(mainTabs);

					if (hasChanges) {

						_Schema.bulkDialogsGeneral.closeWithoutSavingChangesQuestionOpen = true;
						allowNavigation = await Structr.confirmationPromiseNonBlockUI("Really close with unsaved changes?")
					}

					_Schema.bulkDialogsGeneral.closeWithoutSavingChangesQuestionOpen = false;

					if (allowNavigation === true) {

						Structr.dialogCancelBaseAction();

						if (additionalCallback) {
							additionalCallback();
						}
					}
				}
			});
		},
		hasUnsavedChangesInTabs: (mainTabs) => {
			return (mainTabs.find('.has-changes').length > 0);
		},
		hasUnsavedChangesInTable: (table) => {
			return (table[0].querySelectorAll('tbody tr.to-delete, tbody tr.has-changes').length > 0);
		},
		tableChanged: (table) => {
			let unsavedChanges = _Schema.bulkDialogsGeneral.hasUnsavedChangesInTable(table);
			_Schema.bulkDialogsGeneral.dataChanged(unsavedChanges, table, table.find('tfoot'));
		},
		hasUnsavedChangesInFakeTable: (fakeTable) => {
			return (fakeTable[0].querySelectorAll('.fake-tbody .fake-tr.to-delete, .fake-tbody .fake-tr.has-changes').length > 0);
		},
		fakeTableChanged: (fakeTable) => {
			let unsavedChanges = _Schema.bulkDialogsGeneral.hasUnsavedChangesInFakeTable(fakeTable);
			_Schema.bulkDialogsGeneral.dataChanged(unsavedChanges, fakeTable, fakeTable.find('.fake-tfoot'));
		},
		dataChanged: (unsavedChanges, table, tfoot) => {

			let tabContainer = table.closest('.propTabContent');
			if (tabContainer.length > 0) {
				_Schema.bulkDialogsGeneral.dataChangedInTab(tabContainer, unsavedChanges);
			}

			if (unsavedChanges) {

				$('.discard-all', tfoot).removeClass('disabled').attr('disabled', null);
				$('.save-all', tfoot).removeClass('disabled').attr('disabled', null);

			} else {

				$('.discard-all', tfoot).addClass('disabled').attr('disabled', 'disabled');
				$('.save-all', tfoot).addClass('disabled').attr('disabled', 'disabled');
			}
		},
		dataChangedInTab: (tabContainer, unsavedChanges) => {

			let tab = document.querySelector('#' + tabContainer.data('tabId'));

			if (tab) {

				if (unsavedChanges) {
					tab.classList.add('has-changes');
				} else {
					tab.classList.remove('has-changes');
				}
			}

			tabContainer[0].dispatchEvent(new CustomEvent('bulk-data-change', {
				// detail: {
				// 	bar: 'baz'
				// },
				bubbles: true
			}));
		},
		getBulkInfoFromTabControls: (tabControls, doValidate = true) => {
			let data = {};
			for (let key in tabControls) {
				data[key] = tabControls[key].getBulkInfo(doValidate);
			}
			return data;
		},
		isSaveAllowedFromBulkInfo: (bulkInfo) => {

			let allow = true;
			let reasons = [];

			for (let [key, info] of Object.entries(bulkInfo)) {
				if (!info.allow) {
					allow = false;
					reasons.push(info.name);
				}
			}

			return {allow, reasons};
		},
		getChangeCountFromBulkInfo: (bulkInfo) => {
			let total = 0;

			for (let [key, info] of Object.entries(bulkInfo)) {
				for (let [countKey, count] of Object.entries(info.counts)) {
					total += count;
				}
			}

			return total;
		},
		getPayloadFromBulkInfo: (bulkInfo) => {

			let data = {};

			for (let [key, info] of Object.entries(bulkInfo)) {
				if (info.data) {
					Object.assign(data, info.data);
				}
			}

			return data;
		},
		resetInputsViaTabControls: (tabControls) => {
			Object.entries(tabControls).map(e => e[1].reset());
		},
		saveEntityFromTabControls: async (id, tabControls) => {

			let bulkInfo         = _Schema.bulkDialogsGeneral.getBulkInfoFromTabControls(tabControls, true);
			let counts           = _Schema.bulkDialogsGeneral.getChangeCountFromBulkInfo(bulkInfo);
			let {allow, reasons} = _Schema.bulkDialogsGeneral.isSaveAllowedFromBulkInfo(bulkInfo);

			if (counts > 0) {

				if (!allow) {

					new MessageBuilder().warning(`Unable to save. ${reasons.join()} are preventing saving.`).show();

				} else {

					// save data
					_Schema.showSchemaRecompileMessage();
					let data = _Schema.bulkDialogsGeneral.getPayloadFromBulkInfo(bulkInfo);

					let response = await fetch(Structr.rootUrl + id, {
						method: 'PATCH',
						body: JSON.stringify(data)
					});

					_Schema.hideSchemaRecompileMessage();

					if (response.ok) {

						_Code.addAvailableTagsForEntities([data]);

						_Schema.bulkDialogsGeneral.resetInputsViaTabControls(tabControls);

					} else {

						let data = await response.json();

						Structr.errorFromResponse(data);
					}

					return response.ok;
				}
			}

			return false;
		}
	},
	prevAnimFrameReqId_dragNode: undefined,
	nodes: {
		loadNodes: async () => {

			_Schema.hiddenSchemaNodes = JSON.parse(LSWrapper.getItem(_Schema.hiddenSchemaNodesKey));

			let savedHiddenSchemaNodesNull = (_Schema.hiddenSchemaNodes === null);

			let schemaLayout = await _Schema.getFirstSchemaLayoutOrFalse();

			if (schemaLayout && !savedHiddenSchemaNodesNull) {

				_Schema.applySavedLayoutConfiguration(schemaLayout);

			} else {

				let response = await fetch(Structr.rootUrl + 'SchemaNode/ui?' + Structr.getRequestParameterName('sort') + '=hierarchyLevel&' + Structr.getRequestParameterName('order') + '=asc');

				if (response.ok) {

					let data             = await response.json();
					let entities         = {};
					let inheritancePairs = {};
					let hierarchy        = {};
					let x = 0, y = 0;

					if (savedHiddenSchemaNodesNull) {
						_Schema.hiddenSchemaNodes = data.result.filter((entity) => {
							return entity.isBuiltinType;
						}).map((entity) => {
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

							let nodePos = JSON.parse(LSWrapper.getItem(typeName + _Schema.schemaNodePositionKeySuffix));
							if (nodePos) {
								nodePositions[typeName] = nodePos.position;

								LSWrapper.removeItem(typeName + _Schema.schemaNodePositionKeySuffix);
							}
						}

						_Schema.nodePositions = nodePositions;
						LSWrapper.setItem(_Schema.schemaPositionsKey, _Schema.nodePositions);

						// After we have converted all types we try to find *all* outdated type positions and delete them
						for (let key in JSON.parse(LSWrapper.getAsJSON())) {

							if (key.endsWith('node-position')) {
								LSWrapper.removeItem(key);
							}
						}
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
							inheritancePairs[entity.id] = entities[entity.extendsClass.id];
						}
					}

					for (let entitiesAtHierarchyLevel of Object.values(hierarchy)) {

						for (let entity of entitiesAtHierarchyLevel) {

							_Schema.nodeData[entity.id] = entity;

							if (!(_Schema.hiddenSchemaNodes.length > 0 && _Schema.hiddenSchemaNodes.indexOf(entity.name) > -1)) {

								let id = 'id_' + entity.id;
								_Schema.ui.canvas.append(`
									<div class="schema node compact${(entity.isBuiltinType ? ' light' : '')}" id="${id}">
										<b>${entity.name}</b>
										<div class="icons-container flex items-center">
											${_Icons.getSvgIcon('pencil_edit', 16, 16, _Icons.getSvgIconClassesNonColorIcon(['node-action-icon', 'mr-1', 'edit-type-icon']))}
											${(entity.isBuiltinType ? '' : _Icons.getSvgIcon('trashcan', 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'node-action-icon', 'delete-type-icon'])))}
										</div>
									</div>
								`);

								let node = $('#' + id);

								node[0].addEventListener('mousedown', () => {
									node[0].style.zIndex = ++_Schema.ui.maxZ;
								});

								node[0].querySelector('.edit-type-icon').addEventListener('click', (e) => {
									_Schema.openEditDialog(entity.id);
								});

								if (!entity.isBuiltinType) {

									node[0].querySelector('b').addEventListener('click', () => {
										_Schema.makeAttrEditable(node, 'name');
									});

									node[0].querySelector('.delete-type-icon').addEventListener('click', () => {
										Structr.confirmation(
											`<h3>Delete schema node '${entity.name}'?</h3><p>This will delete all incoming and outgoing schema relationships as well, but no data will be removed.</p>`,
											async () => {
												$.unblockUI({ fadeOut: 25 });

												await _Schema.deleteNode(entity.id);
											}
										);
									});
								}

								let nodePosition = _Schema.nodePositions[entity.name];

								if (!nodePosition || (nodePosition && nodePosition.left === 0 && nodePosition.top === 0)) {

									nodePosition = _Schema.ui.calculateNodePosition(x, y);

									let count = 0;

									while (_Schema.overlapsExistingNodes(nodePosition) && count++ < 1000) {
										x++;
										nodePosition = _Schema.ui.calculateNodePosition(x, y);
									}
								}

								let canvasOffsetTop = _Schema.ui.canvas.offset().top;

								if (nodePosition.top < canvasOffsetTop) {
									nodePosition.top = canvasOffsetTop;

									let count = 0;

									while (_Schema.overlapsExistingNodes(nodePosition) && count++ < 1000) {
										x++;
										nodePosition = _Schema.ui.calculateNodePosition(x, y);
									}
								}

								node.offset(nodePosition);

								_Schema.nodeData[entity.id + '_top'] = _Schema.ui.jsPlumbInstance.addEndpoint(id, {
									anchor: "Top",
									maxConnections: -1,
									isTarget: true,
									deleteEndpointsOnDetach: false
								});
								_Schema.nodeData[entity.id + '_bottom'] = _Schema.ui.jsPlumbInstance.addEndpoint(id, {
									anchor: "Bottom",
									maxConnections: -1,
									isSource: true,
									deleteEndpointsOnDetach: false
								});

								let currentCanvasOffset;
								let dragElement;
								let dragElementId;
								let dragElementIsSelected = false;
								let nodeDragStartpoint    = {};
								let dragStopped           = false;

								_Schema.ui.jsPlumbInstance.draggable(id, {
									containment: true,
									start: (ui) => {

										_Schema.ui.updateSelectedNodes();

										dragElement         = $(ui.el);
										dragElementId       = dragElement.attr('id');
										currentCanvasOffset = _Schema.ui.canvas.offset();
										dragStopped         = false;
										let nodeOffset      = dragElement.offset();

										dragElementIsSelected = dragElement.hasClass('selected');
										if (!dragElementIsSelected) {
											_Schema.ui.clearSelection();
										}

										nodeDragStartpoint = {
											top: (nodeOffset.top - currentCanvasOffset.top),
											left: (nodeOffset.left - currentCanvasOffset.left)
										};
									},
									drag: () => {

										if (dragElementIsSelected) {

											Structr.requestAnimationFrameWrapper(_Schema.prevAnimFrameReqId_dragNode, () => {
												if (!dragStopped) {
													let nodeOffset = dragElement.offset();

													let deltaTop  = (nodeDragStartpoint.top - nodeOffset.top);
													let deltaLeft = (nodeDragStartpoint.left - nodeOffset.left);

													for (let selectedNode of _Schema.ui.selectedNodes) {

														if (selectedNode.nodeId !== dragElementId) {
															let newTop  = selectedNode.pos.top - deltaTop;
															if (newTop < currentCanvasOffset.top) {
																newTop = currentCanvasOffset.top;
															}
															let newLeft = selectedNode.pos.left - deltaLeft;
															if (newLeft < currentCanvasOffset.left) {
																newLeft = currentCanvasOffset.left;
															}

															$('#' + selectedNode.nodeId).offset({ top: newTop, left: newLeft });
														}
													}

													_Schema.ui.jsPlumbInstance.repaintEverything();
												}
											});
										}

									},
									stop: () => {
										dragStopped = true;

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

						let sourceEntity = _Schema.nodeData[source];
						let targetEntity = _Schema.nodeData[target];

						let i1 = _Schema.hiddenSchemaNodes.indexOf(sourceEntity.name);
						let i2 = _Schema.hiddenSchemaNodes.indexOf(targetEntity.name);

						if (_Schema.hiddenSchemaNodes.length > 0 && (i1 > -1 || i2 > -1)) {
							continue;
						}

						_Schema.ui.jsPlumbInstance.connect({
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

				} else {
					throw new Error("Loading of Schema nodes failed");
				}
			}
		},
		loadNode: (entity, headEl, contentEl, targetView = 'local', callbackCancel) => {

			headEl.append('<div id="tabs"><ul></ul></div>');
			let mainTabs = $('#tabs', headEl);

			let contentDiv = $('<div class="schema-details h-full relative"></div>');
			contentEl.append(contentDiv);

			let tabControls = {};

			_Entities.appendPropTab(entity, mainTabs, contentDiv, 'basic', 'Basic', targetView === 'basic', (tabContent, entity) => {
				tabControls.basic = _Schema.nodes.appendBasicNodeInfo(tabContent, entity, mainTabs);
			});

			_Entities.appendPropTab(entity, mainTabs, contentDiv, 'local', 'Direct properties', targetView === 'local', (c) => {
				tabControls.schemaProperties = _Schema.properties.appendLocalProperties(c, entity);
			});

			_Entities.appendPropTab(entity, mainTabs, contentDiv, 'remote', 'Linked properties', targetView === 'remote', (c) => {
				let editSchemaObjectLinkHandler = async ($el) => {
					let okToNavigate = !_Schema.bulkDialogsGeneral.hasUnsavedChangesInTabs(mainTabs) || (await Structr.confirmationPromiseNonBlockUI("There are unsaved changes - really navigate to related type?"));
					if (okToNavigate) {
						_Schema.openEditDialog($el.data('objectId'));
					}
				};
				tabControls.remoteProperties = _Schema.remoteProperties.appendRemote(c, entity, editSchemaObjectLinkHandler);
			});

			_Entities.appendPropTab(entity, mainTabs, contentDiv, 'builtin', 'Inherited properties', targetView === 'builtin', (c) => {
				_Schema.properties.appendBuiltinProperties(c, entity);
			});

			_Entities.appendPropTab(entity, mainTabs, contentDiv, 'views', 'Views', targetView === 'views', (c) => {
				tabControls.schemaViews = _Schema.views.appendViews(c, entity);
			});

			_Entities.appendPropTab(entity, mainTabs, contentDiv, 'methods', 'Methods', targetView === 'methods', (c) => {
				tabControls.schemaMethods = _Schema.methods.appendMethods(c, entity, entity.schemaMethods);
			}, _Editors.resizeVisibleEditors);

			_Entities.appendPropTab(entity, mainTabs, contentDiv, 'schema-grants', 'Schema Grants', targetView === 'schema-grants', (c) => {
				tabControls.schemaGrants = _Schema.nodes.schemaGrants.appendSchemaGrants(c, entity);
			});

			if (Structr.isModuleActive(_Code)) {

				// only show the following tabs in the Code area where it is not opened in a popup

				_Entities.appendPropTab(entity, mainTabs, contentDiv, 'working-sets', 'Working Sets', targetView === 'working-sets', (tabContent, entity) => {
					_Schema.nodes.appendWorkingSets(tabContent, entity);
				});

				_Schema.nodes.appendGeneratedSourceCodeTab(entity, mainTabs, contentDiv, targetView);
			}

			_Schema.bulkDialogsGeneral.overrideDialogCancel(mainTabs, callbackCancel);

			Structr.resize();

			return tabControls;
		},
		appendBasicNodeInfo: (tabContent, entity, mainTabs) => {

			tabContent.append(_Schema.templates.typeBasicTab({ type: entity }));

			Structr.activateCommentsInElement(tabContent[0]);

			let updateChangeStatus = () => {

				let typeInfo    = _Schema.nodes.getTypeDefinitionDataFromForm(tabContent[0], entity);
				let changedData = _Schema.nodes.getTypeDefinitionChanges(entity, typeInfo);
				let hasChanges  = Object.keys(changedData).length > 0;

				_Schema.bulkDialogsGeneral.dataChangedInTab(tabContent, hasChanges);
			};

			for (let property of tabContent[0].querySelectorAll('[data-property]')) {
				property.addEventListener('input', updateChangeStatus);
			}

			if (false === entity.isBuiltinType) {

				let classSelect = $('.extends-class-select', tabContent);

				fetch(Structr.rootUrl + 'SchemaNode/ui?' + Structr.getRequestParameterName('sort') + '=name').then(async (response) => {

					let data         = await response.json();
					let customTypes  = data.result.filter(cls => ((!cls.category || cls.category !== 'html') && !cls.isAbstract && !cls.isInterface && !cls.isBuiltinType) && (cls.id !== entity.id));
					let builtinTypes = data.result.filter(cls => ((!cls.category || cls.category !== 'html') && !cls.isAbstract && !cls.isInterface && cls.isBuiltinType) && (cls.id !== entity.id));

					let getOptionsForList = (list) => {
						return list.map(cls => `<option ${((entity.extendsClass && entity.extendsClass.name && entity.extendsClass.id === cls.id) ? 'selected' : '')} value="${cls.id}">${cls.name}</option>`).join('');
					};

					classSelect.append(`
						<optgroup label="Default Type">
							<option value="">AbstractNode - Structr default base type</option>
						</optgroup>
						${(customTypes.length > 0) ? `<optgroup id="for-custom-types" label="Custom Types">${getOptionsForList(customTypes)}</optgroup>` : ''}
						${(builtinTypes.length > 0) ? `<optgroup id="for-builtin-types" label="System Types">${getOptionsForList(builtinTypes)}</optgroup>` : ''}
					`);

					classSelect.select2({
						search_contains: true,
						width: '500px',
						dropdownParent: tabContent
					}).on('change', () => {
						updateChangeStatus();
					});
				});
			}

			if (entity?.extendsClass?.id) {

				tabContent[0].querySelector('.edit-parent-type')?.addEventListener('click', async () => {

					let okToNavigate = !_Schema.bulkDialogsGeneral.hasUnsavedChangesInTabs(mainTabs) || (await Structr.confirmationPromiseNonBlockUI("There are unsaved changes - really navigate to parent type?"));
					if (okToNavigate) {
						_Schema.openEditDialog(entity.extendsClass.id, undefined, () => {

							window.setTimeout(() => {
								_Schema.openEditDialog(entity.id);
							}, 250);
						});
					}
				});
			}

			let apiTab = $('#openapi-options', tabContent);

			$('#tags-select', apiTab).select2({
				tags: true,
				width: '100%'
			}).on('change', () => {
				updateChangeStatus();
			});

			return {
				getBulkInfo: (doValidate) => {

					let typeInfo    = _Schema.nodes.getTypeDefinitionDataFromForm(tabContent[0], entity);
					let allow       = true;
					if (doValidate) {
						allow = _Schema.nodes.validateBasicTypeInfo(typeInfo, tabContent[0], entity);
					}
					let changeCount = Object.keys(_Schema.nodes.getTypeDefinitionChanges(entity, typeInfo)).length;

					return {
						name: 'Basic type attributes',
						data: typeInfo,
						counts: {
							updated: changeCount
						},
						allow: allow
					}
				},
				reset: () => {
					_Schema.nodes.resetTypeDefinition(tabContent[0], entity);
				}
			};
		},
		appendWorkingSets: (tabContent, entity) => {

			tabContent.append(_Schema.templates.workingSets({ type: entity, addButtonText: 'Add Working Set' }));

			// manage working sets
			_WorkingSets.getWorkingSets((workingSets) => {

				let groupSelect = document.querySelector('select#type-groups');

				let createAndAddWorkingSetOption = (set, forceAdd) => {

					let setOption = document.createElement('option');
					setOption.textContent = set.name;
					setOption.dataset['groupId'] = set.id;

					if (forceAdd === true || set.children && set.children.includes(entity.name)) {
						setOption.selected = true;
					}

					groupSelect.appendChild(setOption);
				};

				for (let set of workingSets) {

					if (set.name !== _WorkingSets.recentlyUsedName) {
						createAndAddWorkingSetOption(set);
					}
				}

				let isUnselect = false;
				$(groupSelect).select2({
					search_contains: true,
					width: '100%',
					closeOnSelect: false
				}).on('select2:unselecting', function(e, p) {
					isUnselect = true;

				}).on('select2:opening', function(e, p) {
					if (isUnselect) {
						e.preventDefault();
						isUnselect = false;
					}

				}).on('select2:select', function(e, p) {
					let id = e.params.data.element.dataset['groupId'];

					_WorkingSets.addTypeToSet(id, entity.name, function() {
						_TreeHelper.refreshNode('#code-tree', 'workingsets-' + id);
					});

				}).on('select2:unselect', function(e, p) {
					let id = e.params.data.element.dataset['groupId'];

					_WorkingSets.removeTypeFromSet(id, entity.name, function() {
						_TreeHelper.refreshNode('#code-tree', 'workingsets-' + id);
					});
				});

				tabContent[0].querySelector('.add-button')?.addEventListener('click', () => {

					_WorkingSets.createNewSetAndAddType(entity.name, (ws) => {

						_TreeHelper.refreshNode('#code-tree', 'workingsets');

						createAndAddWorkingSetOption(ws, true);
						$(groupSelect).trigger('change');
					});
				})
			});

		},
		appendGeneratedSourceCodeTab: (entity, mainTabs, contentDiv, targetView) => {

			let generatedSourceTabShowCallback = (tabContent) => {
				_Schema.showGeneratedSource(tabContent[0].querySelector('.generated-source')).then(() => {
					_Editors.resizeVisibleEditors();
				});
			};
			let { tab, tabContent } = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'source-code', 'Source Code', targetView === 'source-code', (tabContent, entity) => {
				tabContent[0].innerHTML = `<div class="generated-source" data-type-name="${entity.type}" data-type-id="${entity.id}"></div>`;
			}, generatedSourceTabShowCallback);

			tab.addClass('tab');
			tab.addClass('generated-source');
			tabContent.addClass('generated-source');

			if (targetView === 'source-code') {
				generatedSourceTabShowCallback(tabContent);
			} else {
				tab.css('display', 'none');
			}
		},
		getTypeDefinitionDataFromForm: (tabContent, entity) => {
			return _Code.collectDataFromContainer(tabContent, entity);
		},
		validateBasicTypeInfo: (typeInfo, container, entity) => {

			let allow = true;

			// builtin types do not transfer their name
			if (false === entity.isBuiltinType) {

				// check type name against regex
				let match = typeInfo.name.match(/^[A-Z][a-zA-Z0-9_]*$/);
				if (match === null) {
					allow = false;
					blinkRed(container.querySelector('[name="name"]'));
				}
			}

			return allow;
		},
		getTypeDefinitionChanges: (entity, newData) => {

			for (let key in newData) {

				let shouldDelete = (entity[key] === newData[key]);

				if (key === 'tags') {
					let prevTags = entity[key];
					let newTags  = newData[key];
					if (!prevTags && newTags.length === 0) {
						shouldDelete = true
					} else if (prevTags.length === newTags.length) {
						let difference = prevTags.filter(t => !newTags.includes(t));
						shouldDelete = (difference.length === 0);
					}
				} else if (key === 'extendsClass') {
					shouldDelete = (entity.extendsClass && entity.extendsClass.id === newData.extendsClass) || (!entity.extendsClass && newData.extendsClass === '');
				}

				if (shouldDelete) {
					delete newData[key];
				}
			}

			return newData;
		},
		resetTypeDefinition: (container, entity) => {
			_Code.revertFormDataInContainer(container, entity);
		},

		schemaGrants: {
			appendSchemaGrants: (tabContent, entity, mainTabs) => {

				let schemaGrantsTableConfig = {
					class: 'schema-grants-table schema-props',
					cols: [
						{ class: '', title: 'Group' },
						{ class: '', title: 'read' },
						{ class: '', title: 'write' },
						{ class: '', title: 'delete' },
						{ class: '', title: 'access control' }
					],
					discardButtonText: 'Discard Schema Grant changes',
					saveButtonText: 'Save Schema Grants'
				};

				let schemaGrantsContainer = Structr.createSingleDOMElementFromHTML(_Schema.templates.schemaGrantsTabContent({ tableMarkup: _Schema.templates.schemaTable(schemaGrantsTableConfig) }));
				tabContent[0].append(schemaGrantsContainer);

				let tbody = schemaGrantsContainer.querySelector('tbody');

				let updateStatus = () => {
					let hasChanges = ((schemaGrantsContainer.querySelectorAll('.changed')).length > 0);

					_Schema.bulkDialogsGeneral.dataChangedInTab(tabContent, hasChanges);
				};

				let schemaGrantsTableChange = (cb, rowConfig) => {

					let property = cb.dataset['property'];
					if (rowConfig[property] !== cb.checked) {
						cb.classList.add('changed');
					} else {
						cb.classList.remove('changed');
					}

					updateStatus();
				};

				let getGrantData = () => {

					let grantData = [];

					for (let row of tbody.querySelectorAll('tr')) {

						let rowConfig = {
							principal:          row.dataset['groupId'],
							schemaNode:         entity.id,
							allowRead:          row.querySelector('input[data-property=allowRead]').checked,
							allowWrite:         row.querySelector('input[data-property=allowWrite]').checked,
							allowDelete:        row.querySelector('input[data-property=allowDelete]').checked,
							allowAccessControl: row.querySelector('input[data-property=allowAccessControl]').checked
						};

						let grantId = row.dataset['grantId'];
						if (grantId) {
							rowConfig.id = grantId;
						}

						let isChanged         = (row.querySelectorAll('.changed').length > 0);
						let atLeastOneChecked = (row.querySelectorAll('input:checked').length > 0);

						if (isChanged || atLeastOneChecked) {
							grantData.push(rowConfig);
						}
					}

					return grantData;
				};

				let grants = {};

				for (let grant of entity.schemaGrants) {
					grants[grant.principal.id] = grant;
				}

				let getGrantConfigForGroup = (group) => {
					return {
						groupId: group.id,
						name: group.name,
						grantId            : (!grants[group.id]) ? '' : grants[group.id].id,
						allowRead          : (!grants[group.id]) ? false : grants[group.id].allowRead,
						allowWrite         : (!grants[group.id]) ? false : grants[group.id].allowWrite,
						allowDelete        : (!grants[group.id]) ? false : grants[group.id].allowDelete,
						allowAccessControl : (!grants[group.id]) ? false : grants[group.id].allowAccessControl
					};
				};

				Command.query('Group', 1000, 1, 'name', 'asc', {}, (groupResult) => {

					for (let group of groupResult) {

						let tplConfig = getGrantConfigForGroup(group);

						let row = Structr.createSingleDOMElementFromHTML(_Code.templates.schemaGrantsRow(tplConfig));
						tbody.appendChild(row);

						for (let cb of row.querySelectorAll('input')) {

							cb.addEventListener('change', (e) => {
								schemaGrantsTableChange(cb, tplConfig);
							});
						}
					}
				});

				return {
					getBulkInfo: (doValidate) => {

						let data = {
							schemaGrants: getGrantData()
						};
						let changeCount = (schemaGrantsContainer.querySelectorAll('.changed')).length;

						return {
							name: 'Basic type attributes',
							data: data,
							counts: {
								updated: changeCount
							},
							allow: true
						}
					},
					reset: () => {

						for (let row of schemaGrantsContainer.querySelectorAll('tbody tr')) {
							let tplConfig = getGrantConfigForGroup({
								id: row.dataset['groupId']
							});

							_Code.revertFormDataInContainer(row, tplConfig);
						}

						for (let cb of tbody.querySelectorAll('.changed')) {
							cb.classList.remove('changed');
						}

						updateStatus();
					}
				};
			}
		},
	},
	relationships: {
		loadRels: async () => {

			let response = await fetch(Structr.rootUrl + 'schema_relationship_nodes');
			let data = await response.json();

			let existingRels = {};
			let relCnt       = {};

			for (let res of data.result) {

				if (!_Schema.nodeData[res.sourceId] || !_Schema.nodeData[res.targetId] || _Schema.hiddenSchemaNodes.indexOf(_Schema.nodeData[res.sourceId].name) > -1 || _Schema.hiddenSchemaNodes.indexOf(_Schema.nodeData[res.targetId].name) > -1) {

					// relationship is not displayed

				} else {

					let relIndex = res.sourceId + '-' + res.targetId;
					if (relCnt[relIndex] === undefined) {
						relCnt[relIndex] = 0;
					} else {
						relCnt[relIndex]++;
					}

					existingRels[relIndex] = true;
					if (res.targetId !== res.sourceId && existingRels[res.targetId + '-' + res.sourceId]) {
						relCnt[relIndex] += existingRels[res.targetId + '-' + res.sourceId];
					}

					let stub   = 30 + 80 * relCnt[relIndex];
					let offset =     0.2 * relCnt[relIndex];

					_Schema.ui.jsPlumbInstance.connect({
						source: _Schema.nodeData[res.sourceId + '_bottom'],
						target: _Schema.nodeData[res.targetId + '_top'],
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
							}],
							["Label", {
								cssClass: "label rel-type",
								label: `<div id="rel_${res.id}" class="flex items-center">
											${(res.relationshipType === _Schema.initialRelType ? '<span>&nbsp;</span>' : res.relationshipType)}
											${_Icons.getSvgIcon('pencil_edit', 16, 16, _Icons.getSvgIconClassesNonColorIcon(['mr-1', 'ml-2', 'edit-relationship-icon']))}
											${(res.isPartOfBuiltInSchema ? '' : _Icons.getSvgIcon('trashcan', 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'mr-1', 'delete-relationship-icon'])))}
										</div>`,
								location: .5,
								id: "label"
							}],
							["Label", {
								cssClass: "label multiplicity",
								label: res.targetMultiplicity ? res.targetMultiplicity : '*',
								location: Math.max(.8 - offset, .6),
								id: "targetMultiplicity"
							}]
						]
					});

					let relTypeOverlay = $('#rel_' + res.id);

					if (res.relationshipType === _Schema.initialRelType) {

						relTypeOverlay.css({
							width: "80px"
						});
						relTypeOverlay.parent().addClass('schema-reltype-warning');

						Structr.appendInfoTextToElement({
							text: "It is highly advisable to set a relationship type on the relationship! To do this, click the pencil icon to open the edit dialog.<br><br><strong>Note: </strong>Any existing relationships of this type have to be migrated manually.",
							element: $('span', relTypeOverlay),
							customToggleIcon: 'warning-sign-icon-filled',
							customToggleIconClasses: ['ml-2'],
							appendToElement: $('#schema-container')
						});
					}

					relTypeOverlay.find('.edit-relationship-icon').on('click', () => {
						_Schema.openEditDialog(res.id);
					});

					relTypeOverlay.find('.delete-relationship-icon').on('click', () => {
						_Schema.relationships.askDeleteRelationship(res.id, res.relationshipType);
						return false;
					});
				}
			}
		},
		loadRelationship: (entity, headEl, contentEl, sourceNode, targetNode, targetView, callbackCancel) => {

			let mainTabs = $('<div id="tabs"><ul></ul></div>');
			headEl.append(mainTabs);

			let contentDiv = $('<div class="schema-details h-full"></div>');
			contentEl.append(contentDiv);

			let tabControls = {};

			_Entities.appendPropTab(entity, mainTabs, contentDiv, 'basic', 'Basic', targetView === 'basic', (tabContent, entity) => {
				tabControls.basic = _Schema.relationships.appendBasicRelInfo(tabContent, entity, sourceNode, targetNode, mainTabs);
			});

			_Entities.appendPropTab(entity, mainTabs, contentDiv, 'local', 'Direct properties', targetView === 'local', (c) => {
				tabControls.schemaProperties = _Schema.properties.appendLocalProperties(c, entity);
			});

			_Entities.appendPropTab(entity, mainTabs, contentDiv, 'views', 'Views', targetView === 'views', (c) => {
				tabControls.schemaViews = _Schema.views.appendViews(c, entity);
			});

			_Entities.appendPropTab(entity, mainTabs, contentDiv, 'methods', 'Methods', targetView === 'methods', (c) => {
				tabControls.schemaMethods = _Schema.methods.appendMethods(c, entity, entity.schemaMethods);
			}, _Editors.resizeVisibleEditors);

			_Schema.bulkDialogsGeneral.overrideDialogCancel(mainTabs, callbackCancel);

			Structr.resize();

			return tabControls;
		},
		appendBasicRelInfo: (tabContent, entity, sourceNode, targetNode, mainTabs) => {

			tabContent[0].insertAdjacentHTML('beforeend', _Schema.templates.relationshipBasicTab());

			_Schema.relationships.appendCascadingDeleteHelpText();

			let updateChangeStatus = () => {

				let changedData = _Schema.relationships.getRelationshipDefinitionChanges(entity);
				let hasChanges  = Object.keys(changedData).length > 0;

				_Schema.bulkDialogsGeneral.dataChangedInTab(tabContent, hasChanges);
			};

			_Schema.relationships.populateBasicRelInfo(entity, sourceNode, targetNode);

			let sourceHelpElements = [];
			let targetHelpElements = [];

			let reactToCardinalityChange = () => {
				_Schema.relationships.reactToCardinalityChange(entity, sourceHelpElements, targetHelpElements);
			};

			updateChangeStatus();

			if (Structr.isModuleActive(_Schema)) {

				for (let editLink of tabContent[0].querySelectorAll('.edit-schema-object')) {

					editLink.addEventListener('click', async (e) => {
						e.stopPropagation();

						let okToNavigate = !_Schema.bulkDialogsGeneral.hasUnsavedChangesInTabs(mainTabs) || (await Structr.confirmationPromiseNonBlockUI("There are unsaved changes - really navigate to related type?"));
						if (okToNavigate) {
							_Schema.openEditDialog(editLink.dataset['objectId']);
						}

						return false;
					});
				}
			} else {
				$('.edit-schema-object', tabContent).removeClass('edit-schema-object');
			}

			let initActions = () => {

				let tabContentEl = tabContent[0];

				tabContentEl.querySelector('#source-json-name').addEventListener('keyup', updateChangeStatus);
				tabContentEl.querySelector('#target-json-name').addEventListener('keyup', updateChangeStatus);

				tabContentEl.querySelector('#source-multiplicity-selector').addEventListener('change', updateChangeStatus);
				tabContentEl.querySelector('#target-multiplicity-selector').addEventListener('change', updateChangeStatus);

				tabContentEl.querySelector('#relationship-type-name').addEventListener('keyup', updateChangeStatus);

				tabContentEl.querySelector('#cascading-delete-selector').addEventListener('change', updateChangeStatus);
				tabContentEl.querySelector('#autocreate-selector').addEventListener('change', updateChangeStatus);
				tabContentEl.querySelector('#propagation-selector').addEventListener('change', updateChangeStatus);
				tabContentEl.querySelector('#read-selector').addEventListener('change', updateChangeStatus);
				tabContentEl.querySelector('#write-selector').addEventListener('change', updateChangeStatus);
				tabContentEl.querySelector('#delete-selector').addEventListener('change', updateChangeStatus);
				tabContentEl.querySelector('#access-control-selector').addEventListener('change', updateChangeStatus);
				tabContentEl.querySelector('#masked-properties').addEventListener('keyup', updateChangeStatus);

				tabContentEl.querySelector('#source-multiplicity-selector').addEventListener('change', reactToCardinalityChange);
				tabContentEl.querySelector('#target-multiplicity-selector').addEventListener('change', reactToCardinalityChange);
				tabContentEl.querySelector('#source-json-name').addEventListener('keyup', reactToCardinalityChange);
				tabContentEl.querySelector('#target-json-name').addEventListener('keyup', reactToCardinalityChange);
			};
			initActions();

			return {
				getBulkInfo: (doValidate) => {

					let relInfo     = _Schema.relationships.getRelationshipDefinitionDataFromForm();
					let allow       = true;
					if (doValidate) {
						allow = _Schema.relationships.validateBasicRelInfo(relInfo, tabContent[0]);
					}
					let changeCount = Object.keys(_Schema.relationships.getRelationshipDefinitionChanges(entity)).length;

					return {
						name: 'Basic relationship attributes',
						data: relInfo,
						counts: {
							updated: changeCount
						},
						allow: allow
					}
				},
				reset: () => {
					_Schema.relationships.populateBasicRelInfo(entity, sourceNode, targetNode);
					reactToCardinalityChange();
					updateChangeStatus();
				},
			};
		},
		populateBasicRelInfo: (rel, sourceNode, targetNode) => {

			$('#source-type-name').text(sourceNode.name);
			document.querySelector('#source-type-name').dataset['objectId'] = rel.sourceId;
			$('#target-type-name').text(targetNode.name);
			document.querySelector('#target-type-name').dataset['objectId'] = rel.targetId;

			$('#source-json-name').val(rel.sourceJsonName || rel.oldSourceJsonName);
			$('#target-json-name').val(rel.targetJsonName || rel.oldTargetJsonName);

			$('#source-multiplicity-selector').val(rel.sourceMultiplicity || '*');
			$('#target-multiplicity-selector').val(rel.targetMultiplicity || '*');

			$('#relationship-type-name').val(rel.relationshipType === _Schema.initialRelType ? '' : rel.relationshipType);

			$('#cascading-delete-selector').val(rel.cascadingDeleteFlag || 0);
			$('#autocreate-selector').val(rel.autocreationFlag || 0);
			$('#propagation-selector').val(rel.permissionPropagation || 'None');
			$('#read-selector').val(rel.readPropagation || 'Remove');
			$('#write-selector').val(rel.writePropagation || 'Remove');
			$('#delete-selector').val(rel.deletePropagation || 'Remove');
			$('#access-control-selector').val(rel.accessControlPropagation || 'Remove');
			$('#masked-properties').val(rel.propertyMask);

			if (rel.isPartOfBuiltInSchema) {
				$('#source-type-name').attr('disabled', 'disabled');
				$('#target-type-name').attr('disabled', 'disabled');
				$('#source-json-name').attr('disabled', 'disabled');
				$('#target-json-name').attr('disabled', 'disabled');
				$('#source-multiplicity-selector').attr('disabled', 'disabled');
				$('#target-multiplicity-selector').attr('disabled', 'disabled');
				$('#relationship-type-name').attr('disabled', 'disabled');
				$('#cascading-delete-selector').attr('disabled', 'disabled');
				$('#autocreate-selector').attr('disabled', 'disabled');
				$('#propagation-selector').attr('disabled', 'disabled');
				$('#read-selector').attr('disabled', 'disabled');
				$('#write-selector').attr('disabled', 'disabled');
				$('#delete-selector').attr('disabled', 'disabled');
				$('#access-control-selector').attr('disabled', 'disabled');
				$('#masked-properties').attr('disabled', 'disabled');
			}
		},
		reactToCardinalityChange: (entity, sourceHelpElements, targetHelpElements) => {

			sourceHelpElements.map(e => e.remove());
			while (sourceHelpElements.length > 0) sourceHelpElements.pop();

			if ($('#source-multiplicity-selector').val() !== (entity.sourceMultiplicity || '*') && $('#source-json-name').val() === (entity.sourceJsonName || entity.oldSourceJsonName)) {
				for (let el of _Schema.relationships.appendCardinalityChangeInfo($('#source-json-name'))) {
					sourceHelpElements.push(el);
				}
			}

			targetHelpElements.map(e => e.remove());
			while (targetHelpElements.length > 0) targetHelpElements.pop();

			if ($('#target-multiplicity-selector').val() !== (entity.targetMultiplicity || '*') && $('#target-json-name').val() === (entity.targetJsonName || entity.oldTargetJsonName)) {
				for (let el of _Schema.relationships.appendCardinalityChangeInfo($('#target-json-name'))) {
					targetHelpElements.push(el);
				}
			}
		},
		appendCardinalityChangeInfo: (el) => {
			return Structr.appendInfoTextToElement({
				text: 'Multiplicity was changed without changing the remote property name - make sure this is correct',
				element: el,
				insertAfter: true,
				customToggleIcon: 'warning-sign-icon-filled',
				customToggleIconClasses: ['ml-2'],
				helpElementCss: {
					fontSize: '9pt'
				}
			});
		},
		validateBasicRelInfo: (relDef, container) => {

			let allow = true;

			// check relationshipType against regex
			let match = relDef.relationshipType.match(/^[a-zA-Z][a-zA-Z0-9_]*$/);
			if (match === null) {
				allow = false;
				blinkRed(container.querySelector('#relationship-type-name'));
			}

			return allow;
		},
		createRelationship: (info) => {

			let sourceId = Structr.getIdFromPrefixIdString(info.sourceId, 'id_');
			let targetId = Structr.getIdFromPrefixIdString(info.targetId, 'id_');

			Structr.dialog("Create Relationship", () => {}, () => {}, ['schema-edit-dialog']);

			dialogCancelButton.hide();

			let container = dialogText;

			container.append(_Schema.templates.relationshipBasicTab());

			_Schema.relationships.appendCascadingDeleteHelpText();

			let sourceTypeName = _Schema.nodeData[sourceId].name;
			let targetTypeName = _Schema.nodeData[targetId].name;

			$('#source-type-name').text(sourceTypeName);
			$('#target-type-name').text(targetTypeName);

			let previousSourceSuggestion = '';
			let previousTargetSuggestion = '';

			let updateSuggestions = () => {

				let currentSourceSuggestion = _Schema.relationships.createRemotePropertyNameSuggestion(sourceTypeName, $('#source-multiplicity-selector').val());
				let currentTargetSuggestion = _Schema.relationships.createRemotePropertyNameSuggestion(targetTypeName, $('#target-multiplicity-selector').val());

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

			if (Structr.isModuleActive(_Schema)) {

				let actionButtons = Structr.createSingleDOMElementFromHTML(_Schema.templates.schemaActionButtons({ saveButtonText: 'Create', discardButtonText: 'Discard and close' }));
				dialogBtn[0].prepend(actionButtons);

				let saveButton    = actionButtons.querySelector('#save-entity-button');
				let discardButton = actionButtons.querySelector('#discard-entity-changes-button');

				saveButton.addEventListener('click', async (e) => {

					let relData = _Schema.relationships.getRelationshipDefinitionDataFromForm();
					relData.sourceId = sourceId;
					relData.targetId = targetId;

					if (relData.relationshipType.trim() === '') {

						blinkRed($('#relationship-type-name'));

					} else {

						await _Schema.relationships.createRelationshipDefinition(relData);
					}
				});

				discardButton.addEventListener('click', function(e) {

					_Schema.currentNodeDialogId = null;

					_Schema.ui.jsPlumbInstance.repaintEverything();
					_Schema.ui.jsPlumbInstance.detach(info.connection);

					Structr.dialogCancelBaseAction();

					dialogCancelButton.show();
				});
			}

			Structr.resize();
		},
		getRelationshipDefinitionDataFromForm: () => {

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
		getRelationshipDefinitionChanges: (entity) => {

			let newData = _Schema.relationships.getRelationshipDefinitionDataFromForm();

			for (let key in newData) {

				let shouldDelete = (entity[key] === newData[key]) || (key === 'cascadingDeleteFlag' && !(entity[key]) && newData[key] === 0) || (key === 'autocreationFlag' && !(entity[key]) && newData[key] === 0) || (key === 'propertyMask' && !(entity[key]) && newData[key].trim() === '');

				if (shouldDelete) {
					delete newData[key];
				}
			}

			return newData;
		},
		createRemotePropertyNameSuggestion: (typeName, cardinality) => {

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
		appendCascadingDeleteHelpText: () => {

			Structr.appendInfoTextToElement({
				text: '<dl class="help-definitions"><dt>NONE</dt><dd>No cascading delete</dd><dt>SOURCE_TO_TARGET</dt><dd>Delete target node when source node is deleted</dd><dt>TARGET_TO_SOURCE</dt><dd>Delete source node when target node is deleted</dd><dt>ALWAYS</dt><dd>Delete source node if target node is deleted AND delete target node if source node is deleted</dd><dt>CONSTRAINT_BASED</dt><dd>Delete source or target node if deletion of the other side would result in a constraint violation on the node (e.g. notNull constraint)</dd></dl>',
				element: $('#cascading-delete-selector'),
				customToggleIconClasses: ['ml-2', 'icon-blue'],
				insertAfter: true,
				noSpan: true
			});
		},
		createRelationshipDefinition: async (data) => {

			_Schema.showSchemaRecompileMessage();

			let response = await fetch(Structr.rootUrl + 'schema_relationship_nodes', {
				method: 'POST',
				body: JSON.stringify(data)
			});

			let responseData = await response.json();

			_Schema.hideSchemaRecompileMessage();

			if (response.ok) {

				_Schema.openEditDialog(responseData.result[0]);
				_Schema.reload();

			} else {

				_Schema.relationships.reportRelationshipError(data, data, responseData);
			}
		},
		removeRelationshipDefinition: async (id) => {

			_Schema.showSchemaRecompileMessage();

			let response = await fetch(Structr.rootUrl + 'schema_relationship_nodes/' + id, {
				method: 'DELETE'
			});

			if (response.ok) {

				_Schema.reload();
				_Schema.hideSchemaRecompileMessage();

			} else {

				let data = await response.json();

				_Schema.hideSchemaRecompileMessage();
				Structr.errorFromResponse(data);
			}

		},
		updateRelationship: async (entity, newData) => {

			_Schema.showSchemaRecompileMessage();

			let getResponse = await fetch(Structr.rootUrl + 'schema_relationship_nodes/' + entity.id);

			if (getResponse.ok) {

				let existingData = await getResponse.json();

				let hasChanges = Object.keys(newData).some((key) => {
					return (existingData.result[key] !== newData[key]);
				});

				if (hasChanges) {

					let putResponse = await fetch(Structr.rootUrl + 'schema_relationship_nodes/' + entity.id, {
						method: 'PUT',
						body: JSON.stringify(newData)
					});

					_Schema.hideSchemaRecompileMessage();
					let responseData = await putResponse.json();

					if (putResponse.ok) {

						for (let attribute in newData) {
							blinkGreen($('#relationship-options [data-attr-name=' + attribute + ']'));
							entity[attribute] = newData[attribute];
						}

						return true;

					} else {

						_Schema.relationships.reportRelationshipError(existingData.result, newData, responseData);

						for (let attribute in newData) {
							blinkRed($('#relationship-options [data-attr-name=' + attribute + ']'));
						}

						return false;
					}

				} else {

					// force a schema-reload so that we dont break the relationships
					_Schema.reload();
					_Schema.hideSchemaRecompileMessage();
				}
			}
		},
		reportRelationshipError: (relData, newData, responseData) => {

			for (let error in responseData.errors) {
				blinkRed($('#relationship-options [data-attr-name=' + error.property + ']'));
			}

			let additionalInformation  = {};
			let hasDuplicateClassError = responseData.errors.some((e) => { return (e.token === 'duplicate_relationship_definition'); });

			if (hasDuplicateClassError) {

				let relType = newData?.relationshipType ?? relData.relationshipType;

				additionalInformation.requiresConfirmation = true;
				additionalInformation.title                = 'Error';
				additionalInformation.furtherText          = 'You are trying to create a second relationship named <strong>' + relType + '</strong> between these types.<br>Relationship names between types have to be unique.';
				additionalInformation.requiresConfirmation = true;
			}

			Structr.errorFromResponse(responseData, null, additionalInformation);
		},
		askDeleteRelationship: (resId, name) => {
			Structr.confirmation(`<h3>Delete schema relationship${(name ? ` '${name}'` : '')}?</h3>`,
				async() => {
					$.unblockUI({
						fadeOut: 25
					});

					await _Schema.relationships.removeRelationshipDefinition(resId);
				});
		},
	},
	properties: {
		appendLocalProperties: (el, entity, overrides, optionalAfterSaveCallback) => {

			let dbNameClass = (UISettings.getValueForSetting(UISettings.schema.settings.showDatabaseNameForDirectProperties) === true) ? '' : 'hidden';

			let tableConfig = {
				class: 'local schema-props',
				cols: [
					{ class: '', title: 'JSON Name' },
					{ class: dbNameClass, title: 'DB Name' },
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

				let row = $(_Schema.templates.propertyLocal({ property: prop, typeOptions: typeOptions, typeHintOptions: typeHintOptions, dbNameClass: dbNameClass }));
				tbody.append(row);

				_Schema.properties.setAttributesInRow(prop, row);
				_Schema.properties.bindRowEvents(prop, row, overrides);

				_Schema.bulkDialogsGeneral.tableChanged(propertiesTable);
			}

			let resetFunction = () => {
				for (let discardIcon of tbody[0].querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
			};
			propertiesTable[0].querySelector('.discard-all').addEventListener('click', resetFunction);

			propertiesTable[0].querySelector('.save-all').addEventListener('click', () => {
				_Schema.properties.bulkSave(el, tbody, entity, overrides, optionalAfterSaveCallback);
			});

			el[0].querySelector('button.add-button').addEventListener('click', () => {

				let newPropertyHtml = _Schema.templates.propertyNew({ typeOptions: typeOptions, dbNameClass: dbNameClass });
				let tr = $(newPropertyHtml);
				tbody.append(tr);

				let propertyTypeSelect = tr[0].querySelector('.property-type');
				propertyTypeSelect.addEventListener('change', () => {

					let selectedOption = propertyTypeSelect.querySelector('option:checked');
					let shouldIndex = selectedOption.dataset['indexed'];
					if (shouldIndex === undefined) {
						shouldIndex = true;
					} else if (shouldIndex === 'false') {
						shouldIndex = false;
					}
					let indexedCb = tr[0].querySelector('.indexed');
					if (indexedCb.checked !== shouldIndex) {

						indexedCb.checked = shouldIndex;

						blink(indexedCb.closest('td'), '#fff', '#bde5f8');
						Structr.showAndHideInfoBoxMessage('Automatically updated indexed flag to default behavior for property type (you can still override this)', 'info', 2000, 200);
					}
				});

				tr[0].querySelector('.discard-changes').addEventListener('click', () => {
					tr.remove();
					_Schema.bulkDialogsGeneral.tableChanged(propertiesTable);
				});

				_Schema.bulkDialogsGeneral.tableChanged(propertiesTable);
			});

			return {
				getBulkInfo: (doValidate) => {
					return _Schema.properties.getDataFromTable(el, tbody, entity, doValidate);
				},
				reset: resetFunction
			};
		},
		getDataFromTable: (el, tbody, entity, doValidate = true) => {

			let name = 'Properties';
			let data = {
				schemaProperties: []
			};
			let allow = true;
			let counts = {
				update: 0,
				delete: 0,
				new: 0
			};

			for (let tr of tbody[0].querySelectorAll('tr')) {

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
						if (doValidate) {
							allow = _Schema.properties.validateProperty(prop, row) && allow;
						}
						data.schemaProperties.push(prop);
					} else {
						// unchanged lines, only transmit id
						prop = { id: propertyId };
						data.schemaProperties.push(prop);
					}

				} else {
					//new lines
					counts.new++;
					prop.type = 'SchemaProperty';
					if (doValidate) {
						allow = _Schema.properties.validateProperty(prop, row) && allow;
					}
					data.schemaProperties.push(prop);
				}
			}

			return { name, data, allow, counts };
		},
		bulkSave: (el, tbody, entity, overrides, optionalAfterSaveCallback) => {

			let { allow, counts, data } = _Schema.properties.getDataFromTable(el, tbody, entity);

			if (allow) {

				_Schema.showSchemaRecompileMessage();

				fetch(Structr.rootUrl + entity.id, {
					method: 'PUT',
					body: JSON.stringify(data)
				}).then((response) => {

					if (response.ok) {

						Command.get(entity.id, null, (reloadedEntity) => {

							el.empty();
							_Schema.properties.appendLocalProperties(el, reloadedEntity, overrides, optionalAfterSaveCallback);
							_Schema.hideSchemaRecompileMessage();

							if (optionalAfterSaveCallback) {
								optionalAfterSaveCallback();
							}
						});

					} else {

						response.json().then((data) => {
							Structr.errorFromResponse(data, undefined, { requiresConfirmation: true });
						});

						_Schema.hideSchemaRecompileMessage();
					}
				});
			}
		},
		bindRowEvents: (property, row, overrides) => {

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

					let unsavedChanges = _Schema.bulkDialogsGeneral.hasUnsavedChangesInTable(row.closest('table'));

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

					let unsavedChanges = _Schema.bulkDialogsGeneral.hasUnsavedChangesInTable(row.closest('table'));

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
		getInfoFromRow: (tr) => {

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
		setAttributesInRow: (property, tr) => {

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
		rowChanged: (property, row) => {

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

			_Schema.bulkDialogsGeneral.tableChanged(row.closest('table'));
		},
		validateProperty: (propertyDefinition, tr) => {

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
		openCodeEditorForFunctionProperty: (id, key, callback) => {

			dialogMeta.show();

			Command.get(id, 'id,name,contentType,' + key, (entity) => {

				Structr.dialog('Edit ' + key + ' of ' + entity.name, () => {}, () => {}, ['popup-dialog-with-editor']);

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

			let editor = _Editors.getMonacoEditor(entity, key, element[0].querySelector('.editor'), functionPropertyMonacoConfig);

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

			_Editors.focusEditor(editor);
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

			let editor = _Editors.getMonacoEditor(entity, key, element[0].querySelector('.editor'), cypherPropertyMonacoConfig);

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

			_Editors.focusEditor(editor);
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
							id: prop.id,
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
						$('tbody', propertiesTable).append(builtinPropertyHtml);
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
		appendRemote: (el, entity, editSchemaObjectLinkHandler, optionalAfterSaveCallback) => {

			let tableConfig = {
				class: 'related-attrs schema-props',
				cols: [
					{ class: '', title: 'JSON Name' },
					{ class: '', title: 'Type, direction and related type' },
					{ class: 'actions-col', title: 'Action' }
				]
			};

			let tbl   = $(_Schema.templates.schemaTable(tableConfig));
			let tbody = tbl.find('tbody');
			el.append(tbl);

			if (entity.relatedTo.length === 0 && entity.relatedFrom.length === 0) {

				tbody.append('<td colspan=3 class="no-rels">Type has no relationships...</td></tr>');
				$('tfoot', tbl).addClass('hidden');

			} else {

				for (let target of entity.relatedTo) {
					_Schema.remoteProperties.appendRemoteProperty(tbody, target, true, editSchemaObjectLinkHandler);
				}

				for (let source of entity.relatedFrom) {
					_Schema.remoteProperties.appendRemoteProperty(tbody, source, false, editSchemaObjectLinkHandler);
				}
			}

			let resetFunction = () => {
				for (let discardIcon of tbl[0].querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
			};
			tbl[0].querySelector('.discard-all').addEventListener('click', resetFunction);

			tbl[0].querySelector('.save-all').addEventListener('click', () => {
				_Schema.remoteProperties.bulkSave(el, tbody, entity, editSchemaObjectLinkHandler, optionalAfterSaveCallback);
			});

			_Schema.bulkDialogsGeneral.tableChanged(tbl);

			return {
				getBulkInfo: (doValidate) => {
					return _Schema.remoteProperties.getDataFromTable(el, tbody, entity, doValidate);
				},
				reset: resetFunction
			};
		},
		getDataFromTable: (el, tbody, entity, doValidate = true) => {

			let name = 'Linked Properties';
			let data = {
				relatedTo: [],
				relatedFrom: []
			};
			let allow = true;
			let counts = {
				update:0,
				reset:0
			};

			for (let tr of tbody[0].querySelectorAll('tr')) {

				let row  = $(tr);
				let info = {
					id: row.data('relationshipId')
				};

				info[row.data('propertyName')] = $('.property-name', row).val();
				if (info[row.data('propertyName')] === '') {
					info[row.data('propertyName')] = null;
				}

				if (row.hasClass('has-changes')) {
					if (doValidate) {
						allow = _Schema.remoteProperties.validate(row) && allow;
					}

					if (info[row.data('propertyName')] === null) {
						counts.reset++;
					} else {
						counts.update++;
					}
				}

				data[row.data('targetCollection')].push(info);
			}

			return { name, data, allow, counts };
		},
		bulkSave: (el, tbody, entity, editSchemaObjectLinkHandler, optionalAfterSaveCallback) => {

			let { allow, counts, data } = _Schema.remoteProperties.getDataFromTable(el, tbody, entity);

			if (allow) {

				_Schema.showSchemaRecompileMessage();

				fetch(Structr.rootUrl + entity.id, {
					method: 'PUT',
					body: JSON.stringify(data)
				}).then((response) => {

					if (response.ok) {

						Command.get(entity.id, null, (reloadedEntity) => {
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
							Structr.errorFromResponse(data, undefined, { requiresConfirmation: true });
						});
					}
				});
			}
		},
		appendRemoteProperty: (el, rel, out, editSchemaObjectLinkHandler) => {

			let relType       = (rel.relationshipType === _Schema.undefinedRelType) ? '' : rel.relationshipType;
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

				if (Structr.isModuleActive(_Schema)) {
					$('.edit-schema-object', row).on('click', async (e) => {
						e.stopPropagation();


						let unsavedChanges = _Schema.bulkDialogsGeneral.hasUnsavedChangesInTable(row.closest('table'));

						if (!unsavedChanges || confirm("Really switch to other type? There are unsaved changes which will be lost!")) {
							editSchemaObjectLinkHandler($(e.target));
						}

						return false;
					});
				} else {
					$('.edit-schema-object', row).removeClass('edit-schema-object');
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

			if (!_Schema.nodeData[relatedNodeId]) {
				Command.get(relatedNodeId, 'name', (data) => {
					tplConfig.relatedNodeType = data.name;
					renderRemoteProperty(tplConfig);
				});
			} else {
				tplConfig.relatedNodeType = _Schema.nodeData[relatedNodeId].name;
				renderRemoteProperty(tplConfig);
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

			_Schema.bulkDialogsGeneral.tableChanged(row.closest('table'));
		},
		validate: (row) => {
			return true;
		}
	},
	views: {
		initialViewConfig: {},
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

			let viewsTable = $(_Schema.templates.schemaTable(tableConfig));
			el.append(viewsTable);

			let tbody = viewsTable.find('tbody');

			_Schema.sort(entity.schemaViews);

			for (let view of entity.schemaViews) {
				_Schema.views.appendView(tbody, view, entity);
			}

			let resetFunction = () => {
				for (let discardIcon of tbody[0].querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
			};
			viewsTable[0].querySelector('.discard-all').addEventListener('click', resetFunction);

			viewsTable[0].querySelector('.save-all').addEventListener('click', () => {
				_Schema.views.bulkSave(el, tbody, entity, optionalAfterSaveCallback);
			});

			el[0].querySelector('button.add-button').addEventListener('click', () => {

				let newViewHtml = _Schema.templates.viewNew();
				let row = $(newViewHtml);
				tbody.append(row);

				_Schema.views.appendViewSelectionElement(row, { name: 'new' }, entity, (chznElement) => {
					chznElement.chosenSortable();

					_Schema.bulkDialogsGeneral.tableChanged(viewsTable);
				});

				row[0].querySelector('.discard-changes').addEventListener('click', () => {
					row.remove();
					_Schema.bulkDialogsGeneral.tableChanged(viewsTable);
				});
			});

			_Schema.bulkDialogsGeneral.tableChanged(viewsTable);

			return {
				getBulkInfo: (doValidate) => {
					return _Schema.views.getDataFromTable(el, tbody, entity, doValidate);
				},
				reset: resetFunction
			};
		},
		getDataFromTable: (el, tbody, entity, doValidate = true) => {

			let name = 'Views';
			let data = {
				schemaViews: []
			};
			let allow = true;
			let counts = {
				update: 0,
				delete: 0,
				new: 0
			};

			for (let tr of tbody[0].querySelectorAll('tr')) {

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
						if (doValidate) {
							allow = _Schema.views.validateViewRow(row) && allow;
						}
						data.schemaViews.push(view);
					} else {
						// unchanged lines, only transmit id
						view = { id: viewId };
						data.schemaViews.push(view);
					}

				} else {

					// new views
					counts.new++;
					view.type = 'SchemaView';
					if (doValidate) {
						allow = _Schema.views.validateViewRow(row) && allow;
					}
					data.schemaViews.push(view);
				}
			}

			return { name, data, allow, counts };
		},
		bulkSave: (el, tbody, entity, optionalAfterSaveCallback) => {

			let { data, allow, counts } = _Schema.views.getDataFromTable(el, tbody, entity);

			if (allow) {

				_Schema.showSchemaRecompileMessage();

				fetch(Structr.rootUrl + entity.id, {
					method: 'PUT',
					body: JSON.stringify(data)
				}).then((response) => {

					if (response.ok) {

						Command.get(entity.id, null, (reloadedEntity) => {
							el.empty();
							_Schema.views.appendViews(el, reloadedEntity, optionalAfterSaveCallback);
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
		},
		appendView: (el, view, entity) => {

			let viewHtml = _Schema.templates.view({view: view, type: entity});
			let row = $(viewHtml);
			el.append(row);

			_Schema.views.appendViewSelectionElement(row, view, entity, (chznElement) => {

				// store initial configuration for later comparison
				let initialViewConfig = _Schema.views.getInfoFromRow(row, entity);

				// store initial configuration for each view to be able to determine excluded properties later
				_Schema.views.initialViewConfig[view.id] = initialViewConfig;

				chznElement.chosenSortable(function() {
					// sort order changed
					_Schema.views.rowChanged(row, entity, initialViewConfig);
				});

				_Schema.views.bindRowEvents(row, entity, view, initialViewConfig);
			});
		},
		bindRowEvents: (row, entity, view, initialViewConfig) => {

			let viewInfoChangeHandler = (event, params) => {
				_Schema.views.rowChanged(row, entity, initialViewConfig);
			};

			$('.view.property-name', row).off('change').on('change', viewInfoChangeHandler);
			$('.view.property-attrs', row).off('change').on('change', viewInfoChangeHandler);

			$('.discard-changes', row).off('click').on('click', function() {

				var select = $('select', row);

				$('.view.property-name', row).val(view.name);

				Command.listSchemaProperties(entity.id, view.name, (data) => {

					for (let prop of data) {
						$('option[value="' + prop.name + '"]', select).prop('selected', prop.isSelected);
					}

					select.trigger('chosen:updated');

					row.removeClass('to-delete');
					row.removeClass('has-changes');

					_Schema.bulkDialogsGeneral.tableChanged(row.closest('table'));
				});
			});

			$('.remove-action', row).off('click').on('click', function() {

				row.addClass('to-delete');
				viewInfoChangeHandler();

			}).prop('disabled', null);
		},
		appendViewSelectionElement: (row, view, schemaEntity, callback) => {

			let propertySelectTd = $('.view-properties-select', row).last();
			propertySelectTd.append('<select class="property-attrs view chosen-sortable" multiple="multiple"></select>');
			let viewSelectElem = $('.property-attrs', propertySelectTd);

			Command.listSchemaProperties(schemaEntity.id, view.name, (properties) => {

				let appendProperty = (prop) => {
					let name       = prop.name;
					let isSelected = prop.isSelected ? ' selected="selected"' : '';
					let isDisabled = (view.name === 'ui' || view.name === 'custom' || prop.isDisabled) ? ' disabled="disabled"' : '';

					viewSelectElem.append('<option value="' + name + '"' + isSelected + isDisabled + '>' + name + '</option>');
				};

				if (view.sortOrder) {
					for (let sortedProp of view.sortOrder.split(',')) {

						let prop = properties.filter(prop => (prop.name === sortedProp));

						if (prop.length > 0) {
							appendProperty(prop[0]);

							properties = properties.filter(prop => (prop.name !== sortedProp));
						}
					}
				}

				for (let prop of properties) {
					appendProperty(prop);
				}

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
		findSchemaPropertiesByNodeAndName: (entity, names) => {

			let result = [];
			let props  = entity['schemaProperties'];

			for (let name of names) {

				for (let prop of props) {

					if (prop.name === name) {
						result.push( { id: prop.id, name: prop.name } );
					}
				}
			}

			return result;
		},
		findNonGraphProperties: (entity, names) => {

			let result = [];
			let props  = entity['schemaProperties'];

			if (names && names.length && props && props.length) {

				for (let name of names) {

					let found = false;

					for (let prop of props) {

						if (prop.name === name) {
							found = true;
						}
					}

					if (!found) {
						result.push(name);
					}
				}

			} else if (names) {

				result = names;
			}

			return result.join(', ');
		},
		findInheritedPropertyByName: (entity, names) => {

			let inheritedProperties = [];

			for (let prop of $('.builtin.schema-props tr')) {
				let name = prop.dataset.propertyName;
				let id   = prop.dataset.propertyId;
				if (names.includes(name)) {
					inheritedProperties.push({id: id});
				}
			}

			return inheritedProperties;
		},
		findExcludedProperties: (entity, names, row) => {

			let excludedProps = [];

			let viewId = row.data('viewId');
			let initialViewConfig = _Schema.views.initialViewConfig[viewId];

			if (initialViewConfig && initialViewConfig.hasOwnProperty('nonGraphProperties')) {
				excludedProps = _Schema.views.findInheritedPropertyByName(entity, initialViewConfig.nonGraphProperties.split(',').map(p => p.trim()).filter(p => !names.includes(p)));
			}

			return excludedProps;
		},
		getInfoFromRow: (row, schemaNodeEntity) => {
			let sortedAttrs = $('.view.property-attrs', row).sortedVals();

			return {
				name: $('.view.property-name', row).val(),
				schemaProperties: _Schema.views.findSchemaPropertiesByNodeAndName(schemaNodeEntity, sortedAttrs),
				nonGraphProperties: _Schema.views.findNonGraphProperties(schemaNodeEntity, sortedAttrs),
				excludedProperties: _Schema.views.findExcludedProperties(schemaNodeEntity, sortedAttrs, row),
				sortOrder: sortedAttrs.join(',')
			};
		},
		rowChanged: (row, entity, initialViewConfig) => {

			let viewInfoInUI = _Schema.views.getInfoFromRow(row, entity);
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

			_Schema.bulkDialogsGeneral.tableChanged(row.closest('table'));
		},
		validateViewRow: (row) => {

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

			methods = _Schema.filterJavaMethods(methods);

			el.append(_Schema.templates.methods({ class: (entity ? 'entity' : 'global') }));

			let tableConfig = {
				class: 'actions schema-props',
				cols: [
					{ class: '', title: 'Name' },
					{ class: 'isstatic-col', title: 'isStatic' },
					{ class: 'actions-col', title: 'Action' }
				],
				entity: entity
			};

			let methodsFakeTable = $(_Schema.templates.fakeTable(tableConfig));
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
					type:            method.type,
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

			if (rowToActivate) {
				rowToActivate[0].querySelector('.edit-action').dispatchEvent(new Event('click'));
			}

			let resetFunction = () => {
				for (let discardIcon of methodsFakeTable[0].querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
			};
			methodsFakeTable[0].querySelector('.discard-all').addEventListener('click', resetFunction);

			methodsFakeTable[0].querySelector('.save-all').addEventListener('click', () => {
				_Schema.methods.bulkSave(el, fakeTbody, entity, optionalAfterSaveCallback);
			});

			let editorInfo = el[0].querySelector('#methods-container-right .editor-info');
			_Editors.appendEditorOptionsElement(editorInfo);

			_Schema.bulkDialogsGeneral.fakeTableChanged(methodsFakeTable);

			return {
				getBulkInfo: (doValidate) => {
					return _Schema.methods.getDataFromTable(el, fakeTbody, entity, doValidate);
				},
				reset: resetFunction
			};
		},
		getDataFromTable: (el, tbody, entity, doValidate = true) => {

			let name = 'Methods';
			let data = {
				schemaMethods: []
			};
			let allow = true;
			let counts = {
				update: 0,
				delete: 0,
				new: 0
			};

			// insert java methods if they are not being displayed
			let javaMethodsOrEmpty = _Schema.getOnlyJavaMethodsIfFilteringIsActive(entity?.schemaMethods ?? []);
			for (let javaMethod of javaMethodsOrEmpty) {
				data.schemaMethods.push({ id: javaMethod.id });
			}

			for (let tr of tbody[0].querySelectorAll('.fake-tr')) {

				let row        = $(tr);
				let methodId   = row.data('methodId');
				let methodData = _Schema.methods.methodsData[methodId];

				if (methodData.isNew === false) {

					if (row.hasClass('to-delete')) {

						counts.delete++;
						data.schemaMethods.push({
							id: methodId,
							deleteMethod: true
						});

					} else if (row.hasClass('has-changes')) {

						counts.update++;
						data.schemaMethods.push({
							id:       methodId,
							name:     methodData.name,
							isStatic: methodData.isStatic,
							source:   methodData.source,
						});
						if (doValidate) {
							allow = _Schema.methods.validateMethodRow(row) && allow;
						}

					} else {

						// unchanged lines, only transmit id (and only for entity-methods)
						if (entity) {
							data.schemaMethods.push({
								id: methodId,
							});
						}
					}

				} else {

					counts.new++;
					if (doValidate) {
						allow = _Schema.methods.validateMethodRow(row) && allow;
					}
					let method = {
						type:     'SchemaMethod',
						name:     methodData.name,
						isStatic: methodData.isStatic,
						source:   methodData.source,
					};

					data.schemaMethods.push(method);
				}
			}

			if (!entity) {
				// global schema methods are saved differently
				data = data.schemaMethods;
			}

			return { name, data, allow, counts };
		},
		bulkSave: (el, fakeTbody, entity, optionalAfterSaveCallback) => {

			let { data, allow, counts } = _Schema.methods.getDataFromTable(el, fakeTbody, entity);

			if (allow) {

				let activeMethod = fakeTbody.find('.fake-tr.editing');
				if (activeMethod) {
					_Schema.methods.setLastEditedMethod(entity, _Schema.methods.methodsData[activeMethod.data('methodId')]);
				} else {
					_Schema.methods.setLastEditedMethod(entity, undefined);
				}

				_Schema.showSchemaRecompileMessage();

				let targetUrl = (entity ? Structr.rootUrl + entity.id : Structr.rootUrl + 'SchemaMethod');

				fetch(targetUrl, {
					method: 'PATCH',
					body: JSON.stringify(data)
				}).then((response) => {

					if (response.ok) {

						if (entity) {

							Command.get(entity.id, null, (reloadedEntity) => {

								el.empty();
								_Schema.methods.appendMethods(el, reloadedEntity, reloadedEntity.schemaMethods, optionalAfterSaveCallback);
								_Schema.hideSchemaRecompileMessage();

								if (optionalAfterSaveCallback) {
									optionalAfterSaveCallback();
								}
							});

						} else {

							Command.rest('SchemaMethod?schemaNode=null&' + Structr.getRequestParameterName('sort') + '=name&' + Structr.getRequestParameterName('order') + '=ascending', (methods) => {
								el.empty();
								_Schema.methods.appendMethods(el, null, methods, optionalAfterSaveCallback);
								_Schema.hideSchemaRecompileMessage();

								if (optionalAfterSaveCallback) {
									optionalAfterSaveCallback();
								}
							});
						}

					} else {

						response.json().then((data) => {
							Structr.errorFromResponse(data, undefined, { requiresConfirmation: true });
						});
						_Schema.hideSchemaRecompileMessage();
					}
				});
			}
		},
		activateUIActions: (el, fakeTbody, entity) => {

			let addedMethodsCounter = 1;

			for (let addMethodButton of el[0].querySelectorAll('.add-method-button')) {

				addMethodButton.addEventListener('click', () => {

					let prefix           = addMethodButton.dataset['prefix'] || '';
					let baseMethodConfig = {
						name: _Schema.methods.getFirstFreeMethodName(prefix),
						id: 'new' + (addedMethodsCounter++)
					};

					_Schema.methods.appendNewMethod(fakeTbody, baseMethodConfig, entity);
				});
			}

			Structr.activateCommentsInElement(el[0], { css: {}, noSpan: true, customToggleIconClasses: ['icon-blue', 'ml-2'] });
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

			_Schema.bulkDialogsGeneral.fakeTableChanged(fakeTbody.closest('.fake-table'));

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

			let sourceEditor = _Editors.getMonacoEditor(methodData, 'source', document.querySelector('#methods-content .editor'), sourceMonacoConfig);
			_Editors.focusEditor(sourceEditor);

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
					_Schema.ui.jsPlumbInstance.repaintEverything();
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
		rowChanged: (row, hasChanges) => {

			if (hasChanges) {
				row.addClass('has-changes');
			} else {
				row.removeClass('has-changes');
			}

			_Schema.bulkDialogsGeneral.fakeTableChanged(row.closest('.fake-table'));
		},
		validateMethodRow: (row) => {

			if ($('.property-name', row).val().length === 0) {

				blinkRed($('.property-name', row).closest('.fake-td'));
				return false;
			}

			return true;
		},
	},
	showGeneratedSource: async (sourceContainer) => {

		if (sourceContainer) {

			let typeName = sourceContainer.dataset.typeName;
			let typeId   = sourceContainer.dataset.typeId;

			if (typeName && typeId) {

				fastRemoveAllChildren(sourceContainer);

				sourceContainer.classList.add('h-full');

				let response = await fetch(Structr.rootUrl + typeName + '/' + typeId + '/getGeneratedSourceCode', { method: 'POST' });

				if (response.ok) {

					// remove id so we do not refresh the editor all the time
					sourceContainer.dataset.typeId = '';

					let result = await response.json();

					let typeSourceConfig = {
						value: result.result,
						language: 'java',
						lint: false,
						autocomplete: false,
						readOnly: true
					};

					_Editors.getMonacoEditor({}, 'source-code', sourceContainer, typeSourceConfig);

					_Editors.resizeVisibleEditors();
				}
			}
		}
	},
	resize: function() {

		Structr.resize();

		if (_Schema.ui.canvas) {

			let zoom           = (_Schema.ui.jsPlumbInstance ? _Schema.ui.jsPlumbInstance.getZoom() : 1);
			let canvasPosition = _Schema.ui.canvas.offset();
			let padding        = 100;

			let canvasSize = {
				w: ($(window).width() - canvasPosition.left) / zoom,
				h: ($(window).height() - canvasPosition.top) / zoom
			};

			for (let elem of document.querySelectorAll('.node')) {
				let $elem = $(elem);
				canvasSize.w = Math.max(canvasSize.w, (($elem.position().left + $elem.outerWidth() - canvasPosition.left) / zoom));
				canvasSize.h = Math.max(canvasSize.h, (($elem.position().top + $elem.outerHeight() - canvasPosition.top)  / zoom + $elem.outerHeight()));
			}

			if (canvasSize.w * zoom < $(window).width() - canvasPosition.left) {
				canvasSize.w = ($(window).width()) / zoom - canvasPosition.left + padding;
			}

			if (canvasSize.h * zoom < $(window).height() - canvasPosition.top) {
				canvasSize.h = ($(window).height()) / zoom  - canvasPosition.top + padding;
			}

			_Schema.ui.canvas.css({
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
	storeSchemaEntity: async (entity, data, onSuccess, onError, onNoop) => {

		let obj = JSON.parse(data);

		if (entity && entity.id) {

			let getResponse  = await fetch(Structr.rootUrl + entity.id);
			let existingData = await getResponse.json();

			let changed = false;
			for (let key in obj) {
				if (existingData.result[key] !== obj[key]) {
					changed |= true;
				}
			}

			if (changed) {

				_Schema.showSchemaRecompileMessage();

				let response = await fetch(Structr.rootUrl + entity.id, {
					method: 'PUT',
					body: JSON.stringify(obj)
				});
				let data = await response.json();

				_Schema.hideSchemaRecompileMessage();

				if (response.ok) {

					_Schema.reload();
					onSuccess?.();

				} else {

					onError?.(data);
				}

			} else {

				onNoop?.();
			}

		} else {

			_Schema.showSchemaRecompileMessage();

			let response = await fetch(Structr.rootUrl + 'schema_nodes', {
				method: 'POST',
				body: JSON.stringify({name: type})
			});
			let data = await response.json();

			_Schema.hideSchemaRecompileMessage();

			if (response.ok) {

				onSuccess?.(data);

			} else {

				onError?.(data);
			}
		}
	},
	createNode: async (type) => {

		_Schema.showSchemaRecompileMessage();

		let response = await fetch(Structr.rootUrl + 'schema_nodes', {
			method: 'POST',
			body: JSON.stringify({name: type})
		});
		let data = await response.json();

		_Schema.hideSchemaRecompileMessage();

		if (response.ok) {

			_Schema.reload();

		} else {

			Structr.errorFromResponse(data, undefined, { requiresConfirmation: true });
		}
	},
	deleteNode: async (id) => {

		_Schema.showSchemaRecompileMessage();

		let response = await fetch(Structr.rootUrl + 'schema_nodes/' + id, {
			method: 'DELETE'
		});
		let data = await response.json();

		_Schema.hideSchemaRecompileMessage();

		if (response.ok) {

			_Schema.reload();

		} else {

			Structr.errorFromResponse(data);
		}
	},
	makeAttrEditable: (element, key) => {

		// cut off three leading underscores and only use 32 characters (the UUID)
		let id = element.prop('id').substring(3, 35);
		if (!id) {
			return false;
		}

		element.children('b').hide();
		let oldVal = $.trim(element.children('b').text());
		let input = $('input.new-' + key, element);

		if (!input.length) {
			element.prepend('<input type="text" size="' + (oldVal.length + 8) + '" class="new-' + key + '" value="' + oldVal + '">');
			input = $('input.new-' + key, element);
		}

		input.show().focus().select();
		let saving = false;
		input.off('blur').on('blur', () => {

			if (!saving) {
				saving = true;
				Command.get(id, 'id', (entity) => {
					_Schema.changeAttr(entity, element, input, key, oldVal);
				});
			}

			return false;
		});

		input.keypress((e) => {
			if (e.keyCode === 13 || e.code === 'Enter' || e.keyCode === 9 || e.code === 'Tab') {
				e.preventDefault();

				if (!saving) {
					saving = true;
					Command.get(id, 'id', (entity) => {
						_Schema.changeAttr(entity, element, input, key, oldVal);
					});
				}
				return false;
			}
		});
		element.off('click');
	},
	changeAttr: (entity, element, input, key, oldVal) => {

		let newVal = input.val();
		input.hide();
		element.children('b').text(newVal).show();

		if (oldVal !== newVal) {
			let obj = {};
			obj[key] = newVal;

			_Schema.storeSchemaEntity(entity, JSON.stringify(obj), null, (data) => {
				Structr.errorFromResponse(data);
				element.children('b').text(oldVal).show();
				element.children('input').val(oldVal);
			});
		}
	},
	activateSnapshotsDialog: () => {

		let table = $('#snapshots');

		let refresh = () => {

			table.empty();

			Command.snapshots('list', '', null, (result) => {

				for (let data of result) {

					data.snapshots.forEach(function(snapshot, i) {
						table.append(`
							<tr>
								<td class="snapshot-link name-${i}">
									<a href="#">${snapshot}</a>
								</td>
								<td style="text-align:right;">
									<button id="restore-${i}" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Restore</button>
									<button id="add-${i}" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Add</button>
									<button id="delete-${i}" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Delete</button>
								</td>
							</tr>
						`);

						$('.name-' + i + ' a').on('click', function() {

							Command.snapshots("get", snapshot, null, (data) => {

								let element = document.createElement('a');
								element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(data.schemaJson));
								element.setAttribute('download', snapshot);

								element.style.display = 'none';
								document.body.appendChild(element);

								element.click();
								document.body.removeChild(element);
							});
						});

						$('#restore-' + i).on('click', () => {
							_Schema.performSnapshotAction('restore', snapshot);
						});
						$('#add-' + i).on('click', () => {
							_Schema.performSnapshotAction('add', snapshot);
						});
						$('#delete-' + i).on('click', () => {
							Command.snapshots('delete', snapshot, null, refresh);
						});
					});
				}
			});
		};

		$('#create-snapshot').off('click').on('click', function() {

			let suffix = $('#snapshot-suffix').val();

			let types = [];

			if (_Schema.ui.selectedNodes && _Schema.ui.selectedNodes.length) {

				for (let selectedNode of _Schema.ui.selectedNodes) {
					types.push(selectedNode.name);
				}

				for (let el of _Schema.ui.canvas[0].querySelectorAll('.label.rel-type')) {
					let $el = $(el);

					let sourceType = $el.children('div').attr('data-source-type');
					let targetType = $el.children('div').attr('data-target-type');

					// include schema relationship if both source and target type are selected
					if (types.indexOf(sourceType) !== -1 && types.indexOf(targetType) !== -1) {
						types.push($el.children('div').attr('data-name'));
					}
				}
			}

			Command.snapshots('export', suffix, types, (data) => {

				let status = data[0].status;
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

	},
	performSnapshotAction: (action, snapshot) => {

		Command.snapshots(action, snapshot, null, (data) => {

			let status = data[0].status;

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

		let registerSchemaToolButtonAction = (btn, target, connectedSelectElement, getPayloadFunction) => {

			btn.on('click', async (e) => {
				e.preventDefault();
				let oldHtml = btn.html();

				let transportAction = async (target, payload) => {

					Structr.updateButtonWithSpinnerAndText(btn, oldHtml);

					let response = await fetch(Structr.rootUrl + 'maintenance/' + target, {
						method: 'POST',
						body: JSON.stringify(payload)
					});

					if (response.ok) {
						Structr.updateButtonWithSuccessIcon(btn, oldHtml);
					}
				};

				if (!connectedSelectElement) {

					await transportAction(target, {});

				} else {

					let type = connectedSelectElement.val();
					if (!type) {
						blinkRed(connectedSelectElement);
					} else {
						await transportAction(target, ((typeof getPayloadFunction === "function") ? getPayloadFunction(type) : {}));
					}
				}
			});
		};

		registerSchemaToolButtonAction($('#rebuild-index'), 'rebuildIndex');
		registerSchemaToolButtonAction($('#flush-caches'), 'flushCaches');

		$('#clear-schema').on('click', (e) => {
			Structr.confirmation('<h3>Delete schema?</h3><p>This will remove all dynamic schema information, but not your other data.</p><p>&nbsp;</p>', () => {
				$.unblockUI({
					fadeOut: 25
				});

				_Schema.showSchemaRecompileMessage();
				Command.snapshots("purge", undefined, undefined, () => {
					_Schema.reload();
					_Schema.hideSchemaRecompileMessage();
				});
			});
		});

		let nodeTypeSelector = $('#node-type-selector');
		Command.list('SchemaNode', true, 1000, 1, 'name', 'asc', 'id,name', (nodes) => {
			nodeTypeSelector.append(nodes.map(node => `<option>${node.name}</option>`).join(''));
		});

		registerSchemaToolButtonAction($('#reindex-nodes'), 'rebuildIndex', nodeTypeSelector, (type) => {
			return (type === 'allNodes') ? {'mode': 'nodesOnly'} : {'mode': 'nodesOnly', 'type': type};
		});

		registerSchemaToolButtonAction($('#add-node-uuids'), 'setUuid', nodeTypeSelector, (type) => {
			return (type === 'allNodes') ? {'allNodes': true} : {'type': type};
		});

		registerSchemaToolButtonAction($('#create-labels'), 'createLabels', nodeTypeSelector, (type) => {
			return (type === 'allNodes') ? {} : {'type': type};
		});

		let relTypeSelector = $('#rel-type-selector');
		Command.list('SchemaRelationshipNode', true, 1000, 1, 'relationshipType', 'asc', 'id,relationshipType', (rels) => {
			relTypeSelector.append(rels.map(rel => `<option>${rel.relationshipType}</option>`).join(''));
		});

		registerSchemaToolButtonAction($('#reindex-rels'), 'rebuildIndex', relTypeSelector, (type) => {
			return (type === 'allRels') ? {'mode': 'relsOnly'} : {'mode': 'relsOnly', 'type': type};
		});

		registerSchemaToolButtonAction($('#add-rel-uuids'), 'setUuid', relTypeSelector, (type) => {
			return (type === 'allRels') ? {'allRels': true} : {'relType': type};
		});

		let showJavaMethodsCheckbox = $('#show-java-methods-in-schema-checkbox');
		if (showJavaMethodsCheckbox) {
			showJavaMethodsCheckbox.prop("checked", _Schema.showJavaMethods);
			showJavaMethodsCheckbox.on('click', () => {
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

		let activateLayoutFunctions = async () => {

			let layoutSelector        = $('#saved-layout-selector');
			let layoutNameInput       = $('#layout-name');
			let createNewLayoutButton = $('#create-new-layout');
			let updateLayoutButton    = $('#update-layout');
			let restoreLayoutButton   = $('#restore-layout');
			let deleteLayoutButton    = $('#delete-layout');

			let layoutSelectorChangeHandler = () => {

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

			updateLayoutButton.click(() => {

				let selectedLayout = layoutSelector.val();

				Command.setProperty(selectedLayout, 'content', JSON.stringify(_Schema.getSchemaLayoutConfiguration()), false, (data) => {

					if (!data.error) {

						new MessageBuilder().success("Layout saved").show();

						blinkGreen(layoutSelector);

					} else {

						new MessageBuilder().error().title(data.error).text(data.message).show();
					}
				});
			});

			restoreLayoutButton.click(() => {
				_Schema.restoreLayout(layoutSelector);
			});

			deleteLayoutButton.click(() => {

				let selectedLayout = layoutSelector.val();

				Command.deleteNode(selectedLayout, false, async () => {
					await _Schema.updateGroupedLayoutSelector(layoutSelector);
					layoutSelectorChangeHandler();
					blinkGreen(layoutSelector);
				});
			});

			createNewLayoutButton.on('click', () => {

				let layoutName = layoutNameInput.val();

				if (layoutName && layoutName.length) {

					Command.createApplicationConfigurationDataNode('layout', layoutName, JSON.stringify(_Schema.getSchemaLayoutConfiguration()), async (data) => {

						if (!data.error) {

							new MessageBuilder().success("Layout saved").show();

							await _Schema.updateGroupedLayoutSelector(layoutSelector);
							layoutSelectorChangeHandler();
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

			await _Schema.updateGroupedLayoutSelector(layoutSelector);
			layoutSelectorChangeHandler();
		};
		activateLayoutFunctions();

		document.getElementById('reset-schema-positions').addEventListener('click', (e) => {
			_Schema.clearPositions();
		});
	},
	updateGroupedLayoutSelector: async (layoutSelector) => {

		return new Promise((resolve, reject) => {

			Command.getApplicationConfigurationDataNodesGroupedByUser('layout', (grouped) => {

				let html = '<option selected value="" disabled>-- Select Layout --</option>';

				if (grouped.length === 0) {

					html += '<option value="" disabled>no layouts available</option>';

				} else {

					html += grouped.map(group => `
						<optgroup data-ownerless="${group.ownerless}" label="${group.label}">
							${group.configs.map(layout => `
								<option value="${layout.id}">${layout.name}</option>
							`).join('')}
						</optgroup>
					`).join('');
				}

				layoutSelector.html(html);

				resolve();
			});
		});
	},
	restoreLayout: (layoutSelector) => {

		let selectedLayout = layoutSelector.val();

		if (selectedLayout) {

			Command.getApplicationConfigurationDataNode(selectedLayout, (data) => {
				_Schema.applySavedLayoutConfiguration(data.content);
			});
		}
	},
	applySavedLayoutConfiguration: (layoutJSON) => {

		try {

			let loadedConfig = JSON.parse(layoutJSON);

			if (loadedConfig._version) {

				switch (loadedConfig._version) {
					case 2: {

						_Schema.ui.zoomLevel = loadedConfig.zoom;
						LSWrapper.setItem(_Schema.schemaZoomLevelKey, _Schema.ui.zoomLevel);
						_Schema.ui.setZoom(_Schema.ui.zoomLevel, _Schema.ui.jsPlumbInstance, [0,0], _Schema.ui.canvas[0]);
						$( "#zoom-slider" ).slider('value', _Schema.ui.zoomLevel);

						_Schema.ui.updateOverlayVisibility(loadedConfig.showRelLabels);

						let hiddenTypes = loadedConfig.hiddenTypes;

						// Filter out types that do not exist in the schema (if types are available already!)
						if (_Schema.availableTypeNames.length > 0) {
							hiddenTypes = hiddenTypes.filter((typeName) => {
								return (_Schema.availableTypeNames.indexOf(typeName) !== -1);
							});
						}
						_Schema.hiddenSchemaNodes = hiddenTypes;
						LSWrapper.setItem(_Schema.hiddenSchemaNodesKey, JSON.stringify(_Schema.hiddenSchemaNodes));

						// update the list in the visibility table
						$('#schema-options-table input.toggle-type').prop('checked', true);
						for (let hiddenType of _Schema.hiddenSchemaNodes) {
							$('#schema-options-table input.toggle-type[data-structr-type="' + hiddenType + '"]').prop('checked', false);
						}

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
					let schemaPositions = {};
					for (let type in loadedConfig) {
						schemaPositions[type] = loadedConfig[type].position;
					}
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
	applyNodePositions: (positions) => {

		for (let n of _Schema.ui.canvas[0].querySelectorAll('.node')) {
			let node = $(n);
			let type = node.text().trim();

			if (positions[type]) {
				node.css('top', positions[type].top);
				node.css('left', positions[type].left);
			}
		}
	},
	getSchemaLayoutConfiguration: () => {
		return {
			_version:       2,
			positions:      _Schema.nodePositions,
			hiddenTypes:    _Schema.hiddenSchemaNodes,
			zoom:           _Schema.ui.zoomLevel,
			connectorStyle: _Schema.ui.connectorStyle,
			showRelLabels:  $('#schema-show-overlays').prop('checked')
		};
	},
	openTypeVisibilityDialog: () => {

		Structr.dialog('', () => {}, () => {});

		let visibilityTables = [
			{
				caption: "Custom Types",
				filterFn: node => (node.isBuiltinType === false),
				exact: true
			},
			{
				caption: "Core Types",
				filterFn: node => (node.isBuiltinType === true && node.category === 'core'),
				exact: false
			},
			{
				caption: "UI Types",
				filterFn: node => (node.isBuiltinType === true && node.category === 'ui'),
				exact: false
			},
			{
				caption: "HTML Types",
				filterFn: node => (node.isBuiltinType === true && node.category === 'html'),
				exact: false
			},
			{
				caption: "Uncategorized Types",
				filterFn: node => (node.isBuiltinType === true && node.category === null),
				exact: true
			}
		];

		let id = "schema-tools-visibility";
		dialogHead.append(`<div class="data-tabs level-two"><ul id="${id}-tabs"></ul></div>`);
		dialogText.append(`<div id="${id}_content"></div>`);

		let ul        = dialogHead[0].querySelector('#' + id + '-tabs', );
		let contentEl = dialogText[0].querySelector('#' + id + '_content');

		let activateTab = (tabName) => {
			[...contentEl.querySelectorAll('.tab')].forEach(tab => tab.style.display = 'none');
			[...ul.querySelectorAll('li')].forEach(li => li.classList.remove('active'));
			contentEl.querySelector('div[data-name="' + tabName + '"]').style.display = 'block';
			ul.querySelector('li[data-name="' + tabName + '"]').classList.add('active');
			LSWrapper.setItem(_Schema.activeSchemaToolsSelectedVisibilityTab, tabName);
		};

		Command.query('SchemaNode', 2000, 1, 'name', 'asc', {}, (schemaNodes) => {

			let tabsHtml = visibilityTables.map(visType => `<li id="tab" data-name="${visType.caption}">${visType.caption}</li>`).join('');
			ul.insertAdjacentHTML('beforeend', tabsHtml);

			let tabContentsHtml = visibilityTables.map(visType => `
				<div class="tab" data-name="${visType.caption}">
					<table class="props schema-visibility-table">
						<tr>
							<th class="toggle-column-header"><input type="checkbox" title="Toggle all" class="toggle-all-types"><i class="invert-all-types invert-icon ${_Icons.getFullSpriteClass(_Icons.toggle_icon)}" title="Invert all"></i> Visible</th>
							<th>Type</th>
						</tr>
						${schemaNodes.filter(schemaNode => visType.filterFn(schemaNode)).map(schemaNode => {
							let isHidden = (_Schema.hiddenSchemaNodes.indexOf(schemaNode.name) > -1);
							return `<tr><td><input class="toggle-type" data-structr-type="${schemaNode.name}" type="checkbox" ${(isHidden ? '' : 'checked')}></td><td>${schemaNode.name}</td></tr>`;
						}).join('')}
					</table>
				</div>
			`).join('');
			contentEl.insertAdjacentHTML('beforeend', tabContentsHtml);


			for (let toggleAllCb of contentEl.querySelectorAll('input.toggle-all-types')) {

				toggleAllCb.addEventListener('change', () => {

					let typeTable = toggleAllCb.closest('table');
					let checked   = toggleAllCb.checked;

					for (let checkbox of typeTable.querySelectorAll('.toggle-type')) {
						checkbox.checked = checked;
					}
					_Schema.updateHiddenSchemaTypes();
					_Schema.reload();
				});
			}

			for (let invertAllCb of contentEl.querySelectorAll('i.invert-all-types')) {

				invertAllCb.addEventListener('click', () => {

					let typeTable = invertAllCb.closest('table');

					for (let checkbox of typeTable.querySelectorAll('.toggle-type')) {
						checkbox.checked = !checkbox.checked;
					}
					_Schema.updateHiddenSchemaTypes();
					_Schema.reload();
				});
			}

			let handleToggleVisibility = (e) => {
				e.stopPropagation();

				let inp = e.target.closest('tr').querySelector('.toggle-type');
				if (inp !== e.target) {
					inp.checked = !inp.checked;
				}

				_Schema.updateHiddenSchemaTypes();
				_Schema.reload();
			};

			for (let el of contentEl.querySelectorAll('td .toggle-type, .schema-visibility-table td')) {
				el.addEventListener('click', handleToggleVisibility);
			}

			for (let tab of ul.querySelectorAll('li')) {
				tab.addEventListener('click', (e) => {
					e.stopPropagation();
					activateTab(tab.dataset['name']);
				});
			}

			let activeTab = LSWrapper.getItem(_Schema.activeSchemaToolsSelectedVisibilityTab) || visibilityTables[0].caption;
			activateTab(activeTab);
		}, false, null, 'id,name,isBuiltinType,category');

	},
	updateHiddenSchemaTypes: () => {

		let hiddenTypes = [];

		for (let checkbox of document.querySelectorAll('.schema-visibility-table input.toggle-type')) {
			let typeName = checkbox.dataset['structrType'];
			let visible  = checkbox.checked;

			if (!visible) {
				hiddenTypes.push(typeName);
			}
		}

		_Schema.hiddenSchemaNodes = hiddenTypes;
		LSWrapper.setItem(_Schema.hiddenSchemaNodesKey, JSON.stringify(_Schema.hiddenSchemaNodes));
	},
	hideSelectedSchemaTypes: () => {

		if (_Schema.ui.selectedNodes.length > 0) {

			for (let n of _Schema.ui.selectedNodes) {
				_Schema.hiddenSchemaNodes.push(n.name);
			}

			LSWrapper.setItem(_Schema.hiddenSchemaNodesKey, JSON.stringify(_Schema.hiddenSchemaNodes));
			_Schema.reload();
		}
	},
	hideSingleSchemaType: (name) => {

		if (name) {

			_Schema.hiddenSchemaNodes.push(name);

			LSWrapper.setItem(_Schema.hiddenSchemaNodesKey, JSON.stringify(_Schema.hiddenSchemaNodes));
			_Schema.reload();
		}
	},
	sort: (collection, sortKey, secondarySortKey) => {

		if (!sortKey) {
			sortKey = "name";
		}

		collection.sort((a, b) => {

			let equal = ((a[sortKey] > b[sortKey]) ? 1 : ((a[sortKey] < b[sortKey]) ? -1 : 0));
			if (equal === 0 && secondarySortKey) {

				equal = ((a[secondarySortKey] > b[secondarySortKey]) ? 1 : ((a[secondarySortKey] < b[secondarySortKey]) ? -1 : 0));
			}

			return equal;
		});
	},
	loadClassTree: (schemaNodes) => {

		let classTree       = {};
		let tmpHierarchy    = {};
		let classnameToId   = {};
		let schemaNodesById = {};

		let searchInheritanceTypesInput = document.querySelector('#search-types');
		let showBuiltinTypesCheckbox    = document.querySelector('#show-builtin-types');
		let showBuiltinTypes            = showBuiltinTypesCheckbox.checked;

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

			let requiredTypes = 0;
			let classes       = Object.keys(classTree).sort();

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

					let $newLi               = $(`<li data-id="${classnameToId[classname]}">${classname}${icons}</li>`).appendTo($newUl);
					let requiredSubTypeCount = printClassTree($newLi, classTree[classname]);
					let iconId               = (Object.keys(classTree[classname]).length > 0) ? 'folder-open-icon' : 'folder-closed-icon';

					let data = {
						opened: true,
						hidden: (showBuiltinTypes === false && isCustomType === false && requiredSubTypeCount === 0),
						icon: _Icons.jstree_fake_icon,
						svgIcon: _Icons.getSvgIcon(iconId, 16, 24),
						requiredIfBuiltinTypesHidden: (isCustomType || requiredSubTypeCount > 0),
						subs: requiredSubTypeCount
					};

					if (isCustomType || requiredSubTypeCount > 0) {
						requiredTypes++;
					}

					$newLi[0].dataset['jstree'] = JSON.stringify(data);
				}
			}

			return requiredTypes;
		};

		let getParentClassName = (str) => {
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

		let eventsInitialized = false;
		let initEvents = (force = false) => {

			if (eventsInitialized === false || force === true) {

				eventsInitialized = true;

				for (let editIcon of document.querySelectorAll('#inheritance-tree .edit-type-icon')) {

					editIcon.addEventListener('click', () => {
						let nodeId = editIcon.closest('li').dataset['id'];
						if (nodeId) {
							_Schema.openEditDialog(nodeId);
						}
					});
				}

				for (let delIcon of document.querySelectorAll('#inheritance-tree .delete-type-icon')) {

					delIcon.addEventListener('click', async () => {

						let nodeId = delIcon.closest('li').dataset['id'];
						if (nodeId) {
							let confirm = await Structr.confirmationPromiseNonBlockUI(`
								<h3>Delete schema node '${delIcon.closest('a').textContent.trim()}'?</h3>
								<p>This will delete all incoming and outgoing schema relationships as well,<br> but no data will be removed.</p>
							`);

							if (confirm === true) {
								$.unblockUI({fadeOut: 25});

								await _Schema.deleteNode(nodeId);
							}
						}
					});
				}
			}
		};

		let initJsTree = () => {

			eventsInitialized = false;

			$.jstree.destroy();
			printClassTree(_Schema.inheritanceTree, classTree);
			_Schema.inheritanceTree.jstree({
				core: {
					animation: 0,
					multiple: false,
					themes: {
						dots: false
					}
				},
				plugins: ["search"]
			}).on('ready.jstree', (e, data) => {

				initEvents();

				// in case we are switching builtin type visibility, react to search input
				if (searchInheritanceTypesInput.value) {
					searchInheritanceTypesInput.dispatchEvent(new Event('keyup'));
				}

			}).on('search.jstree', (e, data) => {

				initEvents(true);

			}).on('clear_search.jstree', (e, data) => {

				initEvents(true);

			}).on('changed.jstree', function(e, data) {

				if (data.node) {
					let $node = $('#id_' + data.node.data.id);
					if ($node.length > 0) {
						$('.selected').removeClass('selected');
						$node.addClass('selected');
						_Schema.ui.selectedNodes = [$node];
					}
				}
			});

			_TreeHelper.addSvgIconReplacementBehaviorToTree(_Schema.inheritanceTree);

			_Schema.inheritanceTree.jstree(true).refresh();
		};
		initJsTree();

		let searchTimeout;
		searchInheritanceTypesInput?.addEventListener('keyup', (e) => {
			if (e.which === 27) {

				searchInheritanceTypesInput.value = '';
				_Schema.inheritanceTree.jstree(true).clear_search();

			} else {
				if (searchTimeout) {
					clearTimeout(searchTimeout);
				}

				searchTimeout = setTimeout(() => {
					let query = searchInheritanceTypesInput.value;
					_Schema.inheritanceTree.jstree(true).search(query, true, true);
				}, 250);
			}
		});

		showBuiltinTypesCheckbox.addEventListener('change', (e) => {
			showBuiltinTypes = showBuiltinTypesCheckbox.checked;

			LSWrapper.setItem(_Schema.showBuiltinTypesInInheritanceTreeKey, showBuiltinTypes);

			initJsTree();
		});
	},
	overlapsExistingNodes: (position) => {
		if (!position) {
			return false;
		}
		let overlaps = false;
		for (let node of _Schema.ui.canvas[0].querySelectorAll('.node.schema.compact')) {
			let offset = $(node).offset();
			overlaps |= (Math.abs(position.left - offset.left) < 20 && Math.abs(position.top - offset.top) < 20);
		}
		return overlaps;
	},
	typeInfoCache: {},
	clearTypeInfoCache: () => {
		_Schema.typeInfoCache = {};
	},
	getTypeInfo: (type, callback) => {

		if (_Schema.typeInfoCache[type] && typeof _Schema.typeInfoCache[type] === 'object') {

			callback(_Schema.typeInfoCache[type]);

		} else {

			Command.getSchemaInfo(type, (schemaInfo) => {

				let typeInfo = {};
				for (let prop of schemaInfo) {
					typeInfo[prop.jsonName] = prop;
				}

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
			let maxDepth  = 5;
			let types     = {};

			let collect = (list, type) => {

				for (let n of list) {

					if (n.extendsClass === type) {

						fileTypes.push('org.structr.dynamic.' + n.name);

						if (!n.isAbstract && !blacklist.includes(n.name)) {
							types[n.name] = 1;
						}
					} else {
						// console.log({ ext: n.extendsClass, type: type });
					}
				}
			};

			collect(result, baseType);

			for (let i = 0; i < maxDepth; i++) {

				for (let type of fileTypes) {
					collect(result, type);
				}
			}

			return Object.keys(types);

		} else {
			return [];
		}
	},
	ui: {
		canvas: undefined,
		jsPlumbInstance: undefined,
		showInheritance: true,
		showSchemaOverlays: true,
		connectorStyle: undefined,
		zoomLevel: undefined,
		selectedRel: undefined,
		relHighlightColor: 'red',
		maxZ: 0,
		selectionInProgress: false,
		mouseDownCoords: { x: 0, y: 0 },
		mouseUpCoords: { x: 0, y: 0 },
		selectBox: undefined,
		selectedNodes: [],
		selectRel: function(rel) {
			_Schema.ui.clearSelection();

			_Schema.ui.selectedRel = rel;
			_Schema.ui.selectedRel.css({zIndex: ++_Schema.ui.maxZ});

			if (!rel.hasClass('dashed-inheritance-relationship')) {
				_Schema.ui.selectedRel.nextAll('._jsPlumb_overlay').slice(0, 3).css({zIndex: ++_Schema.ui.maxZ, border: '1px solid ' + _Schema.ui.relHighlightColor, borderRadius:'2px', background: 'rgba(255, 255, 255, 1)'});
			}

			let pathElements = _Schema.ui.selectedRel.find('path');
			pathElements.css({stroke: _Schema.ui.relHighlightColor});
			$(pathElements[1]).css({fill: _Schema.ui.relHighlightColor});
		},
		clearSelection: () => {
			// deselect selected node
			$('.node', _Schema.ui.canvas).removeClass('selected');
			_Schema.ui.selectionStop();

			// deselect selected Relationship
			if (_Schema.ui.selectedRel) {
				_Schema.ui.selectedRel.nextAll('._jsPlumb_overlay').slice(0, 3).css({border:'', borderRadius:'', background: 'rgba(255, 255, 255, .8)'});

				let pathElements = _Schema.ui.selectedRel.find('path');
				pathElements.css({stroke: '', fill: ''});
				$(pathElements[1]).css('fill', '');

				_Schema.ui.selectedRel = undefined;
			}
		},
		selectionStart: (e) => {
			_Schema.ui.selectionInProgress = true;

			let schemaOffset = _Schema.ui.canvas.offset();
			_Schema.ui.mouseDownCoords.x = e.pageX - schemaOffset.left;
			_Schema.ui.mouseDownCoords.y = e.pageY - schemaOffset.top;
		},
		prevAnimFrameReqId_selectionDrag: undefined,
		selectionDrag: (e) => {
			if (_Schema.ui.selectionInProgress === true) {

				Structr.requestAnimationFrameWrapper(_Schema.ui.prevAnimFrameReqId_selectionDrag, () => {
					let schemaOffset = _Schema.ui.canvas.offset();
					_Schema.ui.mouseUpCoords.x = e.pageX - schemaOffset.left;
					_Schema.ui.mouseUpCoords.y = e.pageY - schemaOffset.top;


					_Schema.ui.drawSelectElem();
				});
			}
		},
		selectionStop: () => {
			_Schema.ui.selectionInProgress = false;
			if (_Schema.ui.selectBox) {
				_Schema.ui.selectBox.remove();
				_Schema.ui.selectBox = undefined;
			}
			_Schema.ui.updateSelectedNodes();
		},
		updateSelectedNodes: () => {
			_Schema.ui.selectedNodes = [];
			let canvasOffset = _Schema.ui.canvas.offset();

			for (let el of _Schema.ui.canvas[0].querySelectorAll('.node.selected')) {
				let $el = $(el);
				let elementOffset = $el.offset();

				_Schema.ui.selectedNodes.push({
					nodeId: $el.attr('id'),
					name: $el.children('b').text(),
					pos: {
						top:  (elementOffset.top  - canvasOffset.top ),
						left: (elementOffset.left - canvasOffset.left)
					}
				});
			}
		},
		drawSelectElem: () => {

			if (!_Schema.ui.selectBox || !_Schema.ui.selectBox.length) {
				_Schema.ui.canvas.append('<svg id="schema-graph-select-box"><path version="1.1" xmlns="http://www.w3.org/1999/xhtml" fill="none" stroke="#aaa" stroke-width="5"></path></svg>');
				_Schema.ui.selectBox = $('#schema-graph-select-box');
			}

			let cssRect = {
				position: 'absolute',
				top:    Math.min(_Schema.ui.mouseDownCoords.y, _Schema.ui.mouseUpCoords.y)  / _Schema.ui.zoomLevel,
				left:   Math.min(_Schema.ui.mouseDownCoords.x, _Schema.ui.mouseUpCoords.x)  / _Schema.ui.zoomLevel,
				width:  Math.abs(_Schema.ui.mouseDownCoords.x - _Schema.ui.mouseUpCoords.x) / _Schema.ui.zoomLevel,
				height: Math.abs(_Schema.ui.mouseDownCoords.y - _Schema.ui.mouseUpCoords.y) / _Schema.ui.zoomLevel
			};

			_Schema.ui.selectBox.css(cssRect);
			_Schema.ui.selectBox.find('path').attr('d', `m 0 0 h ${cssRect.width} v ${cssRect.height} h ${(-cssRect.width)} v ${(-cssRect.height)} z`);
			_Schema.ui.selectNodesInRect(cssRect);
		},
		selectNodesInRect: (selectionRect) => {
			_Schema.ui.selectedNodes = [];

			for (let el of _Schema.ui.canvas[0].querySelectorAll('.node')) {
				let $el = $(el);
				if (_Schema.ui.isElemInSelection($el, selectionRect)) {
					_Schema.ui.selectedNodes.push($el);
					$el.addClass('selected');
				} else {
					$el.removeClass('selected');
				}
			}
		},
		isElemInSelection: ($el, selectionRect) => {
			let elPos = $el.offset();
			elPos.top /= _Schema.ui.zoomLevel;
			elPos.left /= _Schema.ui.zoomLevel;

			let schemaOffset = _Schema.ui.canvas.offset();
			return !(
				(elPos.top) > (selectionRect.top + schemaOffset.top / _Schema.ui.zoomLevel + selectionRect.height) ||
				elPos.left > (selectionRect.left + schemaOffset.left / _Schema.ui.zoomLevel + selectionRect.width) ||
				(elPos.top + $el.innerHeight()) < (selectionRect.top + schemaOffset.top / _Schema.ui.zoomLevel) ||
				(elPos.left + $el.innerWidth()) < (selectionRect.left + schemaOffset.left / _Schema.ui.zoomLevel)
			);
		},
		setZoom: (zoom, instance, transformOrigin, el) => {
			transformOrigin = transformOrigin || [ 0.5, 0.5 ];

			el = el || instance.getContainer();
			let p = [ "webkit", "moz", "ms", "o" ];
			let s = _Schema.ui.getSchemaCSSTransform();
			let oString = (transformOrigin[0] * 100) + "% " + (transformOrigin[1] * 100) + "%";

			for (let vendorPrefix of p) {
				el.style[vendorPrefix + "Transform"] = s;
				el.style[vendorPrefix + "TransformOrigin"] = oString;
			}

			el.style["transform"] = s;
			el.style["transformOrigin"] = oString;

			instance.setZoom(zoom);
			_Schema.resize();
		},
		getSchemaCSSTransform: () => {
			return `scale(${_Schema.ui.zoomLevel})`;
		},
		updateOverlayVisibility: (show) => {
			_Schema.ui.showSchemaOverlays = show;
			LSWrapper.setItem(_Schema.showSchemaOverlaysKey, show);

			$('#schema-show-overlays').prop('checked', show);
			if (show) {
				_Schema.ui.canvas.removeClass('hide-relationship-labels');
			} else {
				_Schema.ui.canvas.addClass('hide-relationship-labels');
			}
		},
		updateInheritanceVisibility: (show) => {
			_Schema.ui.showInheritance = show;
			LSWrapper.setItem(_Schema.showSchemaInheritanceKey, show);

			$('#schema-show-inheritance').prop('checked', show);
			if (show) {
				_Schema.ui.canvas.removeClass('hide-inheritance-arrows');
			} else {
				_Schema.ui.canvas.addClass('hide-inheritance-arrows');
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
	filterJavaMethods: (methods) => {
		if (!_Schema.showJavaMethods) {
			return methods.filter(m => m.codeType !== 'java');
		}
		return methods;
	},
	getOnlyJavaMethodsIfFilteringIsActive: (methods) => {
		if (_Schema.showJavaMethods) {
			return [];
		} else {
			return methods.filter(m => m.codeType === 'java');
		}
	},

	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/schema.css">

			<div class="slideout-activator left" id="inheritanceTab">
				<svg viewBox="0 0 28 28" height="28" width="28" xmlns="http://www.w3.org/2000/svg">
					<g transform="matrix(1.1666666666666667,0,0,1.1666666666666667,0,0)"><path d="M22.5,11.25A5.24,5.24,0,0,0,19.45,6.5,2.954,2.954,0,0,0,16.5,3c-.063,0-.122.015-.185.019a5.237,5.237,0,0,0-8.63,0C7.622,3.015,7.563,3,7.5,3A2.954,2.954,0,0,0,4.55,6.5a5.239,5.239,0,0,0,1.106,9.885A4.082,4.082,0,0,0,12,17.782a4.082,4.082,0,0,0,6.344-1.4A5.248,5.248,0,0,0,22.5,11.25Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M12 8.25L12 23.25" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M12,15q4.5,0,4.5-4.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M12,12A3.543,3.543,0,0,1,8.25,8.25" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path></g>
				</svg>
				Inheri&shy;tance
			</div>

			<div id="inheritance-tree" class="slideOut slideOutLeft">
				<div class="flex items-center justify-between my-2">
					<label class="ml-4">Search: <input type="text" id="search-types" autocomplete="off"></label>
					<label class="mr-4" data-comment="Built-in types will still be shown if they are ancestors of custom types."><input type="checkbox" id="show-builtin-types" ${(LSWrapper.getItem(_Schema.showBuiltinTypesInInheritanceTreeKey, false) ? 'checked' : '')}>Show built-in types</label>
				</div>
				<div id="inheritance-tree-container" class="ver-scrollable hidden"></div>
			</div>

			<div id="schema-container">
				<div class="canvas noselect" id="schema-graph"></div>
			</div>
		`,
		functions: config => `
			<div class="flex-grow">
				<div class="inline-flex">

					<input class="schema-input mr-2" id="type-name" type="text" size="20" placeholder="New type" autocomplete="off">

					<button id="create-type" class="action inline-flex items-center">
						${_Icons.getSvgIcon('circle_plus', 16, 16, ['mr-2'])} Add
					</button>

					<div class="dropdown-menu dropdown-menu-large">
						<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green" id="global-schema-methods">
							${_Icons.getSvgIcon('globe-icon', 16, 16, '')} Global Methods
						</button>
					</div>

					<div class="dropdown-menu dropdown-menu-large">
						<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green">
							${_Icons.getSvgIcon('network-icon', 16, 16, '')} Display
						</button>

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
								<select id="saved-layout-selector" class="hover:bg-gray-100 focus:border-gray-666 active:border-green"></select>
								<button id="restore-layout" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Apply</button>
								<button id="update-layout" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Update</button>
								<button id="delete-layout" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Delete</button>
							</div>

							<div class="row">
								<input id="layout-name" placeholder="Enter name for layout">
								<button id="create-new-layout" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Save</button>
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
						<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green">${_Icons.getSvgIcon('snapshots-icon', 16, 16, '')} Snapshots</button>

						<div class="dropdown-menu-container">
							<div class="heading-row">
								<h3>Create snapshot</h3>
							</div>
							<div class="row">Creates a new snapshot of the current schema configuration that can be restored later.<br>You can enter an (optional) suffix for the snapshot.</div>

							<div class="row">
								<input type="text" name="suffix" id="snapshot-suffix" placeholder="Enter a suffix" length="20">
								<button id="create-snapshot" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Create snapshot</button>
							</div>

							<div class="heading-row">
								<h3>Available Snapshots</h3>
							</div>

							<table class="props" id="snapshots">

							</table>

							<div class="separator"></div>

							<div class="row">
								<a id="refresh-snapshots" class="block">Reload stored snapshots</a>
							</div>
						</div>
					</div>

					<div class="dropdown-menu dropdown-menu-large">
						<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green">
							${_Icons.getSvgIcon('settings-cog', 16, 16, '')} Admin
						</button>

						<div class="dropdown-menu-container">
							<div class="heading-row">
								<h3>Indexing</h3>
							</div>
							<div class="row">
								<select id="node-type-selector" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
									<option selected value="">-- Select Node Type --</option>
									<option disabled>──────────</option>
									<option value="allNodes">All Node Types</option>
									<option disabled>──────────</option>
								</select>
								<button id="reindex-nodes" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">Rebuild node index</button>
								<button id="add-node-uuids" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">Add UUIDs</button>
								<button id="create-labels" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">Create Labels</button>
							</div>
							<div class="row">
								<select id="rel-type-selector" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
									<option selected value="">-- Select Relationship Type --</option>
									<option disabled>──────────</option>
									<option value="allRels">All Relationship Types</option>
									<option disabled>──────────</option>
								</select>
								<button id="reindex-rels" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">Rebuild relationship index</button>
								<button id="add-rel-uuids" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">Add UUIDs</button>
							</div>
							<div class="row flex items-center">
								<button id="rebuild-index" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
									${_Icons.getSvgIcon('refresh-arrows', 16, 16, 'mr-2')} Rebuild all indexes
								</button>
								<label for="rebuild-index">Rebuild indexes for entire database (all node and relationship indexes)</label>
							</div>
							<div class="separator"></div>
							<div class="heading-row">
								<h3>Maintenance</h3>
							</div>
							<div class="row flex items-center">
								<button id="flush-caches" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
									${_Icons.getSvgIcon('refresh-arrows', 16, 16, 'mr-2')} Flush Caches
								</button>
								<label for="flush-caches">Flushes internal caches to refresh schema information</label>
							</div>

							<div class="row flex items-center">
								<button id="clear-schema" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
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
		typeBasicTab: config => `
			<div class="schema-details pl-2">
				<div class="flex items-center gap-x-2 pt-4">

					${(true === config.type.isBuiltinType) ? `<input class="disabled" disabled value="${config.type.name}" class="flex-grow">` : `<input data-property="name" value="${config.type.name}" class="flex-grow">`}

					${(config.type.extendsClass || false === config.type.isBuiltinType) ? `
						<div class="extends-type">
							extends
							${(true === config.type.isBuiltinType) ? `<select disabled><option>${config.type.extendsClass.name}</option></select>` : '<select class="extends-class-select" data-property="extendsClass"></select>' }

							${config.type.extendsClass ? _Icons.getSvgIcon('pencil_edit', 16, 16, _Icons.getSvgIconClassesNonColorIcon(['ml-2', 'edit-parent-type']), 'Edit parent type') : ''}
						</div>
					` : ''}
				</div>

				<h3>Options</h3>
				<div class="property-options-group">
					<div>
						<label data-comment="Only takes effect if the changelog is active">
							<input id="changelog-checkbox" type="checkbox" data-property="changelogDisabled" ${config.type.changelogDisabled ? 'checked' : ''}> Disable changelog
						</label>
						<label class="ml-8" data-comment="Makes all nodes of this type visible to public users if checked">
							<input id="public-checkbox" type="checkbox" data-property="defaultVisibleToPublic" ${config.type.defaultVisibleToPublic ? 'checked' : ''}> Visible for public users
						</label>
						<label class="ml-8" data-comment="Makes all nodes of this type visible to authenticated users if checked">
							<input id="authenticated-checkbox" type="checkbox" data-property="defaultVisibleToAuth" ${config.type.defaultVisibleToAuth ? 'checked' : ''}> Visible for authenticated users
						</label>
					</div>
				</div>

				<h3>OpenAPI</h3>
				<div class="property-options-group">
					<div id="type-openapi">
						${_Code.templates.openAPIBaseConfig({ element: config.type, availableTags: _Code.availableTags })}
					</div>
				</div>
			</div>
		`,
		relationshipBasicTab: config => `
			<div class="schema-details">
				<div id="relationship-options">

					<div id="basic-options" class="grid grid-cols-5 gap-y-2 items-center mb-4">

						<div class="text-right pb-2 truncate">
							<span id="source-type-name" class="edit-schema-object relationship-emphasis"></span>
						</div>

						<div class="flex items-center justify-around">
							<div class="overflow-hidden whitespace-nowrap">&#8212;</div>

							<select id="source-multiplicity-selector" data-attr-name="sourceMultiplicity" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
								<option value="1">1</option>
								<option value="*" selected>*</option>
							</select>

							<div class="overflow-hidden whitespace-nowrap">&#8212;[</div>
						</div>

						<input id="relationship-type-name" data-attr-name="relationshipType" autocomplete="off">

						<div class="flex items-center justify-around">
							<div class="overflow-hidden whitespace-nowrap">]&#8212;</div>

							<select id="target-multiplicity-selector" data-attr-name="targetMultiplicity" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
								<option value="1">1</option>
								<option value="*" selected>*</option>
							</select>

							<div class="overflow-hidden whitespace-nowrap">&#8212;&#9658;</div>
						</div>

						<div class="text-left pb-2 truncate">
							<span id="target-type-name" class="edit-schema-object relationship-emphasis"></span>
						</div>

						<div></div>
						<div class="flex items-center">
							<input id="source-json-name" class="remote-property-name" data-attr-name="sourceJsonName" autocomplete="off">
						</div>
						<div></div>
						<div class="flex items-center">
							<input id="target-json-name" class="remote-property-name" data-attr-name="targetJsonName" autocomplete="off">
						</div>
						<div></div>
					</div>

					<div class="grid grid-cols-2 gap-x-4">

						<div id="cascading-options">
							<h3>Cascading Delete</h3>
							<p>Direction of automatic removal of related nodes when a node is deleted</p>

							<div class="flex items-center">
								<select id="cascading-delete-selector" data-attr-name="cascadingDeleteFlag" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
									<option value="0">NONE</option>
									<option value="1">SOURCE_TO_TARGET</option>
									<option value="2">TARGET_TO_SOURCE</option>
									<option value="3">ALWAYS</option>
									<option value="4">CONSTRAINT_BASED</option>
								</select>
							</div>

							<h3>Automatic Creation of Related Nodes</h3>
							<p>Direction of automatic creation of related nodes when a node is created</p>

							<select id="autocreate-selector" data-attr-name="autocreationFlag" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
								<option value="0">NONE</option>
								<option value="1">SOURCE_TO_TARGET</option>
								<option value="2">TARGET_TO_SOURCE</option>
								<option value="3">ALWAYS</option>
							</select>
						</div>

						<div id="propagation-options">

							<div>
								<h3>Permission Resolution</h3>
								<select id="propagation-selector" data-attr-name="permissionPropagation" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
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
										<select id="read-selector" data-attr-name="readPropagation" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
											<option value="Add">Add</option>
											<option value="Keep">Keep</option>
											<option value="Remove" selected>Remove</option>
										</select>
									</div>

									<div class="selector">
										<p>Write</p>
										<select id="write-selector" data-attr-name="writePropagation" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
											<option value="Add">Add</option>
											<option value="Keep">Keep</option>
											<option value="Remove" selected>Remove</option>
										</select>
									</div>

									<div class="selector">
										<p>Delete</p>
										<select id="delete-selector" data-attr-name="deletePropagation" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
											<option value="Add">Add</option>
											<option value="Keep">Keep</option>
											<option value="Remove" selected>Remove</option>
										</select>
									</div>

									<div class="selector">
										<p>AccessControl</p>
										<select id="access-control-selector" data-attr-name="accessControlPropagation" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
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
		`,
		schemaActionButtons: config => `
			<div>
				<button id="save-entity-button" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
					${_Icons.getSvgIcon('checkmark_bold', 16, 16, ['icon-green', 'mr-2'])} ${(config?.saveButtonText ?? 'Save')}
				</button>
				<button id="discard-entity-changes-button" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
					${_Icons.getSvgIcon('close-dialog-x', 16, 16, ['icon-red', 'mr-2'])} ${(config?.discardButtonText ?? 'Discard')}
				</button>
			</div>
		`,
		methods: config => `
			<div id="methods-container" class="${config.class} h-full flex">
				<div id="methods-container-left">
					<div id="methods-table-container"></div>
				</div>

				<div id="methods-container-right" class="flex flex-col flex-grow">
					<div id="methods-content" class="flex-grow">
						<div class="editor h-full"></div>
					</div>
					<div class="editor-info"></div>
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
			<tr data-property-name="${config.property.name}" data-property-id="${config.property.id}">
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
				<td class="${config.dbNameClass}"><input size="15" type="text" class="property-dbname" value="${escapeForHtmlAttributes(config.property.dbName)}"></td>
				<td>${config.typeOptions}</td>
				<td>
					${
						(() => {
							switch (config.property.propertyType) {
								case 'Function':
									return `
										<div class="flex items-center">
											<button class="edit-read-function mr-1 hover:bg-gray-100 focus:border-gray-666 active:border-green">Read</button>
											<button class="edit-write-function hover:bg-gray-100 focus:border-gray-666 active:border-green">Write</button>
											<input id="checkbox-${config.property.id}" class="caching-enabled" type="checkbox" ${(config.property.isCachingEnabled ? 'checked' : '')}>
											<label for="checkbox-${config.property.id}" class="caching-enabled-label pr-4" title="If caching is enabled, the last value read from this function is written to the database to enable searching/sorting on this field">Cache</label>
											${config.typeHintOptions}
										</div>
									`;
								case 'Cypher':
									return `<button class="edit-cypher-query hover:bg-gray-100 focus:border-gray-666 active:border-green">Query</button>`;
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
				<td class="${config.dbNameClass}"><input size="15" type="text" class="property-dbname" placeholder="Enter DB Name"></td>
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
				<div class="fake-thead">
					<div class="fake-tr">
						${config.cols.map(col=> `<div class="fake-th ${col.class}">${col.title}</div>`).join('')}
					</div>
				</div>
				<div class="fake-tbody"></div>
				<div class="fake-tfoot">
					<div class="fake-tr">
						<div class="fake-td actions-col flex justify-end">
							<button class="discard-all inline-flex items-center disabled hover:bg-gray-100 focus:border-gray-666 active:border-green" disabled>
								${_Icons.getSvgIcon('close-dialog-x', 16, 16, 'icon-red mr-2')} Discard all
							</button>
							<button class="save-all inline-flex items-center disabled hover:bg-gray-100 focus:border-gray-666 active:border-green" disabled>
								${_Icons.getSvgIcon('checkmark_bold', 16, 16, 'icon-green mr-2')} Save all
							</button>
						</div>
					</div>
				</div>
				<div class="fake-tfoot-buttons">

					${(config.entity ? _Schema.templates.addMethodsDropdown(config) : _Schema.templates.addMethodDropdown(config))}

				</div>
			</div>
		`,
		addMethodsDropdown: config => `
			<div class="dropdown-menu darker-shadow-dropdown dropdown-menu-large">
				<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green" data-wants-fixed="true">
					${_Icons.getSvgIcon('circle_plus', 16, 16, 'icon-green mr-2')}
				</button>
				<div class="dropdown-menu-container">
					<div class="flex flex-col divide-x-0 divide-y">
						<a data-prefix="" class="add-method-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer p-4">
							${_Icons.getSvgIcon('circle_plus', 16, 16, 'icon-green mr-2')} Add method
						</a>
						<a data-prefix="onCreate" class="add-method-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer p-4 border-0 border-t border-gray-ddd border-solid">
							${_Icons.getSvgIcon('circle_plus', 16, 16, 'icon-green mr-2')} Add onCreate
						</a>
						<a data-prefix="afterCreate" class="add-method-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer p-4 border-0 border-t border-gray-ddd border-solid" data-comment="The difference between <strong>onCreate</strong> and <strong>afterCreate</strong> is that <strong>afterCreate</strong> is called after all checks have run and the transaction is committed.<br><br>Example: There is a unique constraint and you want to send an email when an object is created.<br>Calling 'send_html_mail()' in onCreate would send the email even if the transaction would be rolled back due to an error. The appropriate place for this would be afterCreate.">
							${_Icons.getSvgIcon('circle_plus', 16, 16, 'icon-green mr-2')} Add afterCreate
						</a>
						<a data-prefix="onSave" class="add-method-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer p-4 border-0 border-t border-gray-ddd border-solid">
							${_Icons.getSvgIcon('circle_plus', 16, 16, 'icon-green mr-2')} Add onSave
						</a>
						<a data-prefix="afterSave" class="add-method-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer p-4 border-0 border-t border-gray-ddd border-solid" data-comment="The difference between <strong>onSave</strong> and <strong>afterSave</strong> is that <strong>afterSave</strong> is called after all checks have run and the transaction is committed.<br><br>Example: There is a unique constraint and you want to send an email when an object is saved successfully.<br>Calling 'send_html_mail()' in onSave would send the email even if the transaction would be rolled back due to an error. The appropriate place for this would be afterSave.">
							${_Icons.getSvgIcon('circle_plus', 16, 16, 'icon-green mr-2')} Add afterSave
						</a>
					</div>
				</div>
			</div>
		`,
		addMethodDropdown: config => `
			<button prefix="" class="inline-flex items-center add-method-button hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer">
				${_Icons.getSvgIcon('circle_plus', 16, 16, 'icon-green mr-2')} Add method
			</button>
		`,
		schemaTable: config => `
			<table class="${config.class}">
				<thead>
					<tr>${config.cols.map(col=> `<th class="${col.class}">${col.title}</th>`).join('')}</tr>
				</thead>
				<tbody></tbody>
				<tfoot>
					<th colspan=${config.cols.length} class="actions-col">
						${(config.addButtonText ? '<button class="add-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">' + _Icons.getSvgIcon('circle_plus', 16, 16, 'icon-green mr-2') + config.addButtonText + '</button>' : '')}
						<button class="discard-all inline-flex items-center disabled hover:bg-gray-100 focus:border-gray-666 active:border-green" disabled>
							${_Icons.getSvgIcon('close-dialog-x', 16, 16, 'icon-red mr-2')} ${(config.discardButtonText ? config.discardButtonText : 'Discard all')}
						</button>
						<button class="save-all inline-flex items-center disabled hover:bg-gray-100 focus:border-gray-666 active:border-green" disabled>
							${_Icons.getSvgIcon('checkmark_bold', 16, 16, 'icon-green mr-2')} ${(config.discardButtonText ? config.saveButtonText : 'Save all')}
						</button>
					</th>
				</tfoot>
			</table>
		`,
		typeHintOptions: config => `
			<select class="type-hint pr-2 hover:bg-gray-100 focus:border-gray-666 active:border-green">
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
			<select class="property-type pr-6 hover:bg-gray-100 focus:border-gray-666 active:border-green">
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
		schemaGrantsTabContent: config => `
			<div>
				<div class="inline-info">
					<div class="inline-info-icon">
						${_Icons.getSvgIcon('info-icon', 24, 24)}
					</div>
					<div class="inline-info-text">
						To grant the corresponding permissions on <strong>all nodes of that type</strong>, simply check the corresponding boxes and save the grants.
					</div>
				</div>

				<div style="width: calc(100% - 4rem);" class="pt-4">
					${config.tableMarkup}
				</div>

			</div>
		`,
		workingSets: config => `
			<div>
				<div class="inline-info">
					<div class="inline-info-icon">
						${_Icons.getSvgIcon('info-icon', 24, 24)}
					</div>
					<div class="inline-info-text">
						Working Sets are identical to layouts. Removing an element from a group removes it from the layout
					</div>
				</div>

				<div style="width: calc(100% - 4rem);" class="pt-4 mb-4">
					<select id="type-groups" multiple="multiple"></select>
					<span id="add-to-new-group"></span>
				</div>

				${(config.addButtonText ? '<button class="add-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">' + _Icons.getSvgIcon('circle_plus', 16, 16, 'icon-green mr-2') + config.addButtonText + '</button>' : '')}

			</div>
		`,
		usageSearch: config => `
			<div class="mb-4">
				<div>
					<label id="usage-label"></label>
				</div>

				<div id="usage-tree-container">
					<ul id="usage-tree"></ul>
				</div>
			</div>
		`,
	}
};