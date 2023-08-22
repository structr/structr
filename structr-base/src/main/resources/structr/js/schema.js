/*
 * Copyright (C) 2010-2023 Structr GmbH
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
	schemaMethodsHeightsKey: 'structrSchemaMethodsHeights_' + location.port,
	schemaActiveTabLeftKey: 'structrSchemaActiveTabLeft_' + location.port,
	activeSchemaToolsSelectedTabLevel1Key: 'structrSchemaToolsSelectedTabLevel1_' + location.port,
	activeSchemaToolsSelectedVisibilityTab: 'activeSchemaToolsSelectedVisibilityTab_' + location.port,
	schemaZoomLevelKey: '_schema_' + location.port + 'zoomLevel',
	schemaConnectorStyleKey: '_schema_' + location.port + 'connectorStyle',
	schemaNodePositionKeySuffix: '_schema_' + location.port + 'node-position',
	currentNodeDialogId: null,
	inheritanceTree: undefined,
	inheritanceSlideout: undefined,
	onload: () => {

		_Code.preloadAvailableTagsForEntities().then(() => {

			Structr.setMainContainerHTML(_Schema.templates.main());
			_Helpers.activateCommentsInElement(Structr.mainContainer);

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

			let inheritanceTab = document.querySelector('#inheritanceTab');
			inheritanceTab.addEventListener('click', () => {
				Structr.slideouts.leftSlideoutTrigger(inheritanceTab, _Schema.inheritanceSlideout, [], () => {
					LSWrapper.setItem(_Schema.schemaActiveTabLeftKey, inheritanceTab.id);
					_Schema.inheritanceTree.show();
				}, () => {
					LSWrapper.removeItem(_Schema.schemaActiveTabLeftKey);
					_Schema.inheritanceTree.hide();
				});
			});

			if (LSWrapper.getItem(_Schema.schemaActiveTabLeftKey)) {
				$('#' + LSWrapper.getItem(_Schema.schemaActiveTabLeftKey)).click();
			}

			_Schema.init(null,() => {
				Structr.resize();
			});

			Structr.updateMainHelpLink(_Helpers.getDocumentationURLForTopic('schema'));
		});
	},
	getLeftResizerKey: () => {
		return null;
	},
	reload: (callback) => {

		_Schema.clearTypeInfoCache();

		if (_Schema.isReloading) {
			return;
		}

		_Schema.isReloading = true;
		//_Schema.storePositions();	/* CHM: don't store positions on every reload, let automatic positioning do its job.. */

		_Helpers.fastRemoveAllChildren(_Schema.ui.canvas[0]);
		_Schema.init({ x: window.scrollX, y: window.scrollY }, callback);
		Structr.resize();
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

		_Schema.schemaLoading      = false;
		_Schema.ui.connectorStyle  = LSWrapper.getItem(_Schema.schemaConnectorStyleKey) || 'Flowchart';
		_Schema.ui.zoomLevel       = parseFloat(LSWrapper.getItem(_Schema.schemaZoomLevelKey)) || 1.0;
		_Schema.ui.showInheritance = LSWrapper.getItem(_Schema.showSchemaInheritanceKey, true) || true;

		Structr.setFunctionBarHTML(_Schema.templates.functions());

		UISettings.showSettingsForCurrentModule(UISettings.settingGroups.schema_code);

		_Schema.activateDisplayDropdownTools();
		_Schema.activateSnapshotsDialog();
		_Schema.activateAdminTools();

		document.getElementById('create-type').addEventListener('click', _Schema.nodes.showCreateTypeDialog);
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
				Structr.resize();
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

						_Schema.relationships.showCreateRelationshipDialog(info);
					}

				} else {
					new WarningMessage().text('Moving existing relationships is not permitted!').title('Not allowed').requiresConfirmation().show();
					_Schema.reload();
				}
			});

			_Schema.ui.jsPlumbInstance.bind('connectionDetached', (info) => {

				if (info.connection.scope !== 'jsPlumb_DefaultScope') {
					new WarningMessage().text('Deleting relationships is only possible via the delete button!').title('Not allowed').requiresConfirmation().show();
					_Schema.reload();
				}
			});
			_Schema.isReloading = false;

			_Schema.ui.setZoom(_Schema.ui.zoomLevel, _Schema.ui.jsPlumbInstance, [0,0], _Schema.ui.canvas[0]);

			$('._jsPlumb_connector').click(function(e) {
				e.stopPropagation();
				_Schema.ui.selectRel($(this));
			});

			Structr.resize();

			Structr.mainMenu.unblock(500);

			let showSchemaOverlays = LSWrapper.getItem(_Schema.showSchemaOverlaysKey, true);
			_Schema.ui.updateOverlayVisibility(showSchemaOverlays);

			let showSchemaInheritance = LSWrapper.getItem(_Schema.showSchemaInheritanceKey, true);
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
		_Dialogs.loadingMessage.show('Schema is compiling', 'Please wait...', 'schema-compilation-message');
	},
	hideSchemaRecompileMessage:  () => {
		_Dialogs.loadingMessage.hide('schema-compilation-message');
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

			new InfoMessage()
				.title("Schema recompiled")
				.text("Another user made changes to the schema. Do you want to reload to see the changes?")
				.specialInteractionButton("Reload", _Schema.reloadSchemaAfterRecompileNotification, "Ignore")
				.uniqueClass('schema')
				.incrementsUniqueCount()
				.show();
		}
	},
	reloadSchemaAfterRecompileNotification: () => {

		if (_Schema.currentNodeDialogId !== null) {

			// we break the current dialog the hard way (because if we 'click' the close button we might re-open the previous dialog
			_Dialogs.custom.dialogCancelBaseAction();

			let currentView = LSWrapper.getItem(`${_Entities.activeEditTabPrefix}_${_Schema.currentNodeDialogId}`);

			_Schema.reload(() => {
				_Schema.openEditDialog(_Schema.currentNodeDialogId, currentView);
			});

		} else {

			_Schema.reload();
		}
	},
	getFirstSchemaLayoutOrFalse: async () => {

		let response = await fetch(`${Structr.rootUrl}ApplicationConfigurationDataNode/ui?configType=layout&${Structr.getRequestParameterName('pageSize')}=1&${Structr.getRequestParameterName('order')}=desc&${Structr.getRequestParameterName('sort')}=createdDate`);
		let data     = await response.json();

		if (data.result?.length > 0) {

			return data.result[0];
		}

		return false;
	},
	openEditDialog: (id, targetView, callback) => {

		targetView = targetView || LSWrapper.getItem(`${_Entities.activeEditTabPrefix}_${id}`) || 'basic';

		_Schema.currentNodeDialogId = id;

		Command.get(id, null, (entity) => {

			let isRelationship = (entity.type === "SchemaRelationshipNode");
			let title          = isRelationship ? `(:${_Schema.nodeData[entity.sourceId].name})—[:${entity.relationshipType}]—►(:${_Schema.nodeData[entity.targetId].name})` : entity.name;

			let callbackCancel = () => {

				_Schema.currentNodeDialogId = null;

				callback?.();

				_Schema.ui.jsPlumbInstance.repaintEverything();
			};

			let { dialogText } = _Dialogs.custom.openDialog(title, callbackCancel, ['schema-edit-dialog']);

			dialogText.insertAdjacentHTML('beforeend', `
				<div class="schema-details flex flex-col h-full overflow-hidden">
					<div id="tabs">
						<ul class="flex-shrink-0"></ul>
					</div>
				</div>
			`);

			let mainTabs  = dialogText.querySelector('#tabs');
			let contentEl = dialogText.querySelector('.schema-details');

			let tabControls;

			if (isRelationship) {
				tabControls = _Schema.relationships.loadRelationship(entity, mainTabs, contentEl, _Schema.nodeData[entity.sourceId], _Schema.nodeData[entity.targetId], targetView, callbackCancel);
			} else {
				tabControls = _Schema.nodes.loadNode(entity, mainTabs, contentEl, targetView, callbackCancel);
			}

			// remove bulk edit save/discard buttons
			for (let button of dialogText.querySelectorAll('.discard-all, .save-all')) {
				_Helpers.fastRemoveElement(button);
			}

			let cancelButton = _Dialogs.custom.prependCustomDialogButton(_Schema.templates.discardActionButton({ text: 'Discard All' }));
			let saveButton   = _Dialogs.custom.prependCustomDialogButton(_Schema.templates.saveActionButton({ text: 'Save All' }));

			_Dialogs.custom.setDialogSaveButton(saveButton);

			saveButton.addEventListener('click', async (e) => {

				let ok = await _Schema.bulkDialogsGeneral.saveEntityFromTabControls(entity, tabControls);

				if (ok) {
					_Schema.openEditDialog(id);
				}
			});

			cancelButton.addEventListener('click', (e) => {
				_Schema.bulkDialogsGeneral.resetInputsViaTabControls(tabControls);
			});

			_Helpers.disableElements(true, saveButton, cancelButton);

			contentEl.addEventListener('bulk-data-change', (e) => {

				e.stopPropagation();

				let changeCount = _Schema.bulkDialogsGeneral.getChangeCountFromBulkInfo(_Schema.bulkDialogsGeneral.getBulkInfoFromTabControls(tabControls, false));
				let isDirty     = (changeCount > 0);
				_Helpers.disableElements(!isDirty, saveButton, cancelButton);
			});

			_Schema.ui.clearSelection();

		}, 'schema');

	},
	bulkDialogsGeneral: {
		closeWithoutSavingChangesQuestionOpen: false,
		overrideDialogCancel: (mainTabs, additionalCallback) => {

			if (Structr.isModuleActive(_Schema)) {

				let newCancelButton = _Dialogs.custom.updateOrCreateDialogCloseButton();

				newCancelButton.addEventListener('click', async (e) => {

					if (_Schema.bulkDialogsGeneral.closeWithoutSavingChangesQuestionOpen === false) {

						let allowNavigation = true;
						let hasChanges      = _Schema.bulkDialogsGeneral.hasUnsavedChangesInTabs(mainTabs);

						if (hasChanges) {

							_Schema.bulkDialogsGeneral.closeWithoutSavingChangesQuestionOpen = true;
							allowNavigation = await _Dialogs.confirmation.showPromise("Really close with unsaved changes?")
						}

						_Schema.bulkDialogsGeneral.closeWithoutSavingChangesQuestionOpen = false;

						if (allowNavigation === true) {

							_Dialogs.custom.dialogCancelBaseAction();

							if (additionalCallback) {
								additionalCallback();
							}
						}
					}
				});
			}
		},
		hasUnsavedChangesInTabs: (mainTabs) => {
			return (mainTabs.querySelectorAll('.has-changes').length > 0);
		},
		hasUnsavedChangesInTable: (table) => {
			return (table.querySelectorAll('tbody tr.to-delete, tbody tr.has-changes').length > 0);
		},
		tableChanged: (table) => {
			let unsavedChanges = _Schema.bulkDialogsGeneral.hasUnsavedChangesInTable(table);
			_Schema.bulkDialogsGeneral.dataChanged(unsavedChanges, table, table.querySelector('tfoot'));
		},
		fakeTableChanged: (fakeTable) => {
			let unsavedChanges = (fakeTable.querySelectorAll('.fake-tbody .fake-tr.to-delete, .fake-tbody .fake-tr.has-changes').length > 0);
			_Schema.bulkDialogsGeneral.dataChanged(unsavedChanges, fakeTable, fakeTable.querySelector('.fake-tfoot'));
		},
		dataChanged: (unsavedChanges, table, tfoot) => {

			let tabContainer = table.closest('.propTabContent');
			if (tabContainer) {
				_Schema.bulkDialogsGeneral.dataChangedInTab(tabContainer, unsavedChanges);
			}

			let discardAll = tfoot.querySelector('.discard-all');
			let saveAll    = tfoot.querySelector('.save-all');

			_Helpers.disableElements(!unsavedChanges, discardAll, saveAll);
		},
		dataChangedInTab: (tabContainer, unsavedChanges) => {

			let tab = document.querySelector(`#${tabContainer.dataset['tabId']}`);

			if (tab) {
				_Schema.markElementAsChanged(tab, unsavedChanges);
			}

			tabContainer.dispatchEvent(new CustomEvent('bulk-data-change', {
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

			return { allow, reasons };
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
		saveEntityFromTabControls: async (schemaNode, tabControls) => {

			let bulkInfo           = _Schema.bulkDialogsGeneral.getBulkInfoFromTabControls(tabControls, true);
			let counts             = _Schema.bulkDialogsGeneral.getChangeCountFromBulkInfo(bulkInfo);
			let { allow, reasons } = _Schema.bulkDialogsGeneral.isSaveAllowedFromBulkInfo(bulkInfo);

			if (counts > 0) {

				if (!allow) {

					new WarningMessage().text(`Unable to save. ${reasons.join()} are preventing saving.`).show();

				} else {

					// save data
					_Schema.showSchemaRecompileMessage();
					let data = _Schema.bulkDialogsGeneral.getPayloadFromBulkInfo(bulkInfo);

					let response = await fetch(Structr.rootUrl + schemaNode.id, {
						method: 'PATCH',
						body: JSON.stringify(data)
					});

					if (Structr.isModuleActive(_Schema)) {

						// Reload is only necessary for changes in the "basic" tab for types (name and extendsClass) and relationships (relType and cardinality)
						let typeChangeRequiresReload = (bulkInfo?.basic?.changes?.name || bulkInfo?.basic?.changes?.extendsClass)
						let relChangeRequiresReload  = (bulkInfo?.basic?.changes?.relationshipType || bulkInfo?.basic?.changes?.sourceMultiplicity || bulkInfo?.basic?.changes?.targetMultiplicity)

						if (typeChangeRequiresReload || relChangeRequiresReload) {
							_Schema.reload();
						}
					}

					_Schema.hideSchemaRecompileMessage();

					if (response.ok) {

						_Code.addAvailableTagsForEntities([data]);

						_Schema.bulkDialogsGeneral.resetInputsViaTabControls(tabControls);

						_Schema.invalidateTypeInfoCache(schemaNode.name);

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

			let schemaLayout = await _Schema.getFirstSchemaLayoutOrFalse();

			if (schemaLayout && schemaLayout.content && (_Schema.hiddenSchemaNodes === null)) {

				_Schema.applySavedLayoutConfiguration(schemaLayout, true);
			}

			let response = await fetch(`${Structr.rootUrl}SchemaNode/ui?${Structr.getRequestParameterName('sort')}=hierarchyLevel&${Structr.getRequestParameterName('order')}=asc`);
			if (response.ok) {

				let data             = await response.json();
				let entities         = {};
				let inheritancePairs = {};
				let hierarchy        = {};
				let x = 0, y = 0;

				if (_Schema.hiddenSchemaNodes === null) {
					_Schema.hiddenSchemaNodes = data.result.filter((entity) => entity.isBuiltinType).map((entity) => entity.name);
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
										${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, _Icons.getSvgIconClassesNonColorIcon(['node-action-icon', 'mr-1', 'edit-type-icon']))}
										${(entity.isBuiltinType ? '' : _Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'node-action-icon', 'delete-type-icon'])))}
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

								node[0].querySelector('.delete-type-icon').addEventListener('click', async () => {
									let confirm = await _Dialogs.confirmation.showPromise(`
										<h3>Delete schema node '${entity.name}'?</h3>
										<p>This will delete all incoming and outgoing schema relationships as well,<br> but no data will be removed.</p>
									`);

									if (confirm === true) {

										await _Schema.deleteNode(entity.id);
									}
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

										_Helpers.requestAnimationFrameWrapper(_Schema.prevAnimFrameReqId_dragNode, () => {
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
									Structr.resize();
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
		},
		loadNode: (entity, mainTabs, contentDiv, targetView = 'local', callbackCancel) => {

			if (!Structr.isModuleActive(_Code) && (targetView === 'source-code' || targetView === 'working-sets')) {
				targetView = 'basic';
			}
			let basicTabContent        = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'basic', 'Basic', targetView === 'basic');
			let localPropsTabContent   = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'local', 'Direct properties', targetView === 'local');
			let remotePropsTabContent  = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'remote', 'Linked properties', targetView === 'remote');
			let builtinPropsTabContent = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'builtin', 'Inherited properties', targetView === 'builtin');
			let viewsTabContent        = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'views', 'Views', targetView === 'views');
			let methodsTabContent      = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'methods', 'Methods', targetView === 'methods', _Editors.resizeVisibleEditors);
			let schemaGrantsTabContent = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'schema-grants', 'Schema Grants', targetView === 'schema-grants');

			_Schema.properties.appendBuiltinProperties(builtinPropsTabContent, entity);

			let tabControls = {
				basic            : _Schema.nodes.appendBasicNodeInfo(basicTabContent, entity, mainTabs),
				schemaProperties : _Schema.properties.appendLocalProperties(localPropsTabContent, entity),
				remoteProperties : _Schema.remoteProperties.appendRemote(remotePropsTabContent, entity, async (el) => { await _Schema.remoteProperties.asyncEditSchemaObjectLinkHandler(el, mainTabs, entity.id); }),
				schemaViews      : _Schema.views.appendViews(viewsTabContent, entity),
				schemaMethods    : _Schema.methods.appendMethods(methodsTabContent, entity, entity.schemaMethods),
				schemaGrants     : _Schema.nodes.schemaGrants.appendSchemaGrants(schemaGrantsTabContent, entity)
			};

			if (Structr.isModuleActive(_Code)) {

				// only show the following tabs in the Code area where it is not opened in a popup

				let workingSetsTabContent = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'working-sets', 'Working Sets', targetView === 'working-sets');
				workingSetsTabContent.classList.add('relative');
				_Schema.nodes.appendWorkingSets(workingSetsTabContent, entity);

				_Schema.nodes.appendGeneratedSourceCodeTab(entity, mainTabs, contentDiv, targetView);
			}

			_Schema.bulkDialogsGeneral.overrideDialogCancel(mainTabs, callbackCancel);

			Structr.resize();

			return tabControls;
		},
		showCreateTypeDialog: async () => {

			let { dialogText } = _Dialogs.custom.openDialog("Create Type", null, ['schema-edit-dialog']);

			_Schema.nodes.showCreateNewTypeDialog(dialogText);

			let discardButton = _Dialogs.custom.prependCustomDialogButton(_Schema.templates.discardActionButton({ text: 'Discard and close' }));
			let saveButton    = _Dialogs.custom.prependCustomDialogButton(_Schema.templates.saveActionButton({ text: 'Create' }));

			_Dialogs.custom.setDialogSaveButton(saveButton);

			saveButton.addEventListener('click', async (e) => {

				let typeData = _Schema.nodes.getTypeDefinitionDataFromForm(dialogText, {});

				if (!typeData.name || typeData.name.trim() === '') {

					_Helpers.blinkRed(dialogText.querySelector('[data-property="name"]'));

				} else {

					_Schema.nodes.createTypeDefinition(typeData).then(responseData => {
						_Schema.openEditDialog(responseData.result[0]);
						_Schema.reload();
					}, rejectData => {
						Structr.errorFromResponse(rejectData, undefined, { requiresConfirmation: true });
					});

				}
			});

			// replace old cancel button with "discard button" to enable global ESC handler
			_Dialogs.custom.replaceDialogCloseButton(discardButton, false);
			discardButton.addEventListener('click', (e) => {
				_Dialogs.custom.dialogCancelBaseAction();
			});
		},
		populateBasicTypeInfo: (container, entity) => {

			let nameInput = container.querySelector('[data-property="name"]');
			nameInput.value = entity.name;
			if (entity.isBuiltinType === true) {
				delete nameInput.dataset['property'];
				nameInput.disabled = true;
				nameInput.classList.add('disabled');
			} else {
			}

			if (entity.extendsClass || entity.isBuiltinType === false) {

				let select = container.querySelector('[data-property="extendsClass"]');
				if (entity.isBuiltinType === true) {
					select.insertAdjacentHTML('beforeend', `<option>${entity.extendsClass.name}</option>`);
					delete select.dataset['property'];
					select.disabled = true;
					select.classList.add('disabled');
				}

				if (!entity.extendsClass) {
					_Helpers.fastRemoveElement(container.querySelector('.edit-parent-type'));
				}

			} else {
				_Helpers.fastRemoveElement(container.querySelector('.extends-type'));
			}

			container.querySelector('[data-property="changelogDisabled"]').checked      = (true === entity.changelogDisabled);
			container.querySelector('[data-property="defaultVisibleToPublic"]').checked = (true === entity.defaultVisibleToPublic);
			container.querySelector('[data-property="defaultVisibleToAuth"]').checked   = (true === entity.defaultVisibleToAuth);

			_Code.populateOpenAPIBaseConfig(container, entity, _Code.availableTags);
		},
		appendTypeHierarchy: (container, entity = {}, changeFn) => {

			fetch(`${Structr.rootUrl}SchemaNode/ui?${Structr.getRequestParameterName('sort')}=name`).then(async (response) => {

				let data         = await response.json();
				let customTypes  = data.result.filter(cls => ((!cls.category || cls.category !== 'html') && !cls.isAbstract && !cls.isInterface && !cls.isBuiltinType) && (cls.id !== entity.id));
				let builtinTypes = data.result.filter(cls => ((!cls.category || cls.category !== 'html') && !cls.isAbstract && !cls.isInterface && cls.isBuiltinType) && (cls.id !== entity.id));

				let getOptionsForList = (list) => {
					return list.map(cls => `<option ${((entity.extendsClass && entity.extendsClass.name && entity.extendsClass.id === cls.id) ? 'selected' : '')} value="${cls.id}">${cls.name}</option>`).join('');
				};

				let classSelect = container.querySelector('.extends-class-select');
				classSelect.insertAdjacentHTML('beforeend', `
					<optgroup label="Default Type">
						<option value="">AbstractNode - Structr default base type</option>
					</optgroup>
					${(customTypes.length > 0) ? `<optgroup id="for-custom-types" label="Custom Types">${getOptionsForList(customTypes)}</optgroup>` : ''}
					${(builtinTypes.length > 0) ? `<optgroup id="for-builtin-types" label="System Types">${getOptionsForList(builtinTypes)}</optgroup>` : ''}
				`);

				$(classSelect).select2({
					search_contains: true,
					width: '500px',
					dropdownParent: $(container)
				}).on('change', () => {
					changeFn?.();
				});
			});
		},
		activateTagsSelect: (container, changeFn) => {

			let dropdownParent = _Dialogs.custom.isDialogOpen() ? $(_Dialogs.custom.getDialogBoxElement()) : $('body');

			$('#tags-select', $(container)).select2({
				tags: true,
				width: '100%',
				dropdownParent: dropdownParent
			}).on('change', () => {
				changeFn?.();
			});
		},
		appendBasicNodeInfo: (tabContent, entity, mainTabs) => {

			tabContent.appendChild(_Helpers.createSingleDOMElementFromHTML(_Schema.templates.typeBasicTab()));

			_Helpers.activateCommentsInElement(tabContent);

			let updateChangeStatus = () => {

				let typeInfo    = _Schema.nodes.getTypeDefinitionDataFromForm(tabContent, entity);
				let changedData = _Schema.nodes.getTypeDefinitionChanges(entity, typeInfo);
				let hasChanges  = Object.keys(changedData).length > 0;

				_Schema.bulkDialogsGeneral.dataChangedInTab(tabContent, hasChanges);
			};

			_Schema.nodes.populateBasicTypeInfo(tabContent, entity);

			for (let property of tabContent.querySelectorAll('[data-property]')) {
				property.addEventListener('input', updateChangeStatus);
			}

			if (false === entity.isBuiltinType) {

				_Schema.nodes.appendTypeHierarchy(tabContent, entity, updateChangeStatus);
			}

			if (entity?.extendsClass?.id) {

				tabContent.querySelector('.edit-parent-type')?.addEventListener('click', async () => {

					let okToNavigate = !_Schema.bulkDialogsGeneral.hasUnsavedChangesInTabs(mainTabs) || (await _Dialogs.confirmation.showPromise("There are unsaved changes - really navigate to parent type?"));
					if (okToNavigate) {
						_Schema.openEditDialog(entity.extendsClass.id, undefined, () => {

							window.setTimeout(() => {
								_Schema.openEditDialog(entity.id);
							}, 250);
						});
					}
				});
			}

			_Schema.nodes.activateTagsSelect(tabContent.querySelector('#openapi-options'), updateChangeStatus);

			return {
				getBulkInfo: (doValidate) => {

					let typeInfo    = _Schema.nodes.getTypeDefinitionDataFromForm(tabContent, entity);
					let allow       = true;
					if (doValidate) {
						allow = _Schema.nodes.validateBasicTypeInfo(typeInfo, tabContent, entity);
					}
					let changes     = _Schema.nodes.getTypeDefinitionChanges(entity, typeInfo);
					let changeCount = Object.keys(changes).length;

					return {
						name: 'Basic type attributes',
						data: typeInfo,
						counts: {
							updated: changeCount
						},
						changes: changes,
						allow: allow
					}
				},
				reset: () => {
					_Schema.nodes.resetTypeDefinition(tabContent, entity);
				}
			};
		},
		appendWorkingSets: (container, entity) => {

			container.insertAdjacentHTML('beforeend', _Schema.templates.workingSets({ type: entity, addButtonText: 'Add Working Set' }));

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

				container.querySelector('.add-button')?.addEventListener('click', () => {

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
				_Schema.showGeneratedSource(tabContent.querySelector('.generated-source')).then(() => {
					_Editors.resizeVisibleEditors();
				});
			};
			let tabContent = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'source-code', 'Source Code', targetView === 'source-code', generatedSourceTabShowCallback, false, true);

			tabContent.insertAdjacentHTML('beforeend', `<div class="generated-source" data-type-name="${entity.type}" data-type-id="${entity.id}"></div>`);

			if (targetView === 'source-code') {
				generatedSourceTabShowCallback(tabContent);
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
				if (typeInfo.name === null || typeInfo.name.match(/^[A-Z][a-zA-Z0-9_]*$/) === null) {
					allow = false;
					_Helpers.blinkRed(container.querySelector('[data-property="name"]'));
				}
			}

			return allow;
		},
		getTypeDefinitionChanges: (entity, newData) => {

			for (let key in newData) {

				let shouldDelete = (entity[key] === newData[key]);

				if (key === 'tags') {
					let prevTags = entity[key] ?? [];
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
		showCreateNewTypeDialog: (container) => {

			container.insertAdjacentHTML('beforeend', _Schema.templates.typeBasicTab());

			_Helpers.fastRemoveElement(container.querySelector('.edit-parent-type'));

			_Schema.nodes.appendTypeHierarchy(container);
			_Schema.nodes.activateTagsSelect(container);
			_Code.populateOpenAPIBaseConfig(container, {}, _Code.availableTags);

			_Helpers.activateCommentsInElement(container);
		},
		createTypeDefinition: (data) => {

			return new Promise(((resolve, reject) => {

				_Schema.showSchemaRecompileMessage();

				fetch(`${Structr.rootUrl}schema_nodes`, {
					method: 'POST',
					body: JSON.stringify(data)
				}).then(response => {

					response.json().then(responseData => {

						_Schema.hideSchemaRecompileMessage();

						if (response.ok) {

							resolve(responseData);

						} else {

							reject(responseData);
						}
					});
				})
			}));
		},

		schemaGrants: {
			schemaGrantsTableConfig: {
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
			},
			appendSchemaGrants: (container, entity) => {

				let schemaGrantsContainer = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.schemaGrantsTabContent({ tableMarkup: _Schema.templates.schemaTable(_Schema.nodes.schemaGrants.schemaGrantsTableConfig) }));
				let tbody                 = schemaGrantsContainer.querySelector('tbody');
				container.appendChild(schemaGrantsContainer);

				let updateStatus = () => {
					let hasChanges = ((schemaGrantsContainer.querySelectorAll('.changed')).length > 0);

					_Schema.bulkDialogsGeneral.dataChangedInTab(container, hasChanges);
				};

				let schemaGrantsTableChange = (cb, rowConfig) => {

					let property = cb.dataset['property'];

					cb.classList[(rowConfig[property] !== cb.checked) ? 'add' : 'remove']('changed');

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

						let row = _Helpers.createSingleDOMElementFromHTML(_Code.templates.schemaGrantsRow(tplConfig));
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
			let data     = await response.json();

			let existingRels = {};
			let relCnt       = {};

			for (let res of data.result) {

				if (!_Schema.nodeData[res.sourceId] || !_Schema.nodeData[res.targetId] || _Schema.hiddenSchemaNodes.indexOf(_Schema.nodeData[res.sourceId].name) > -1 || _Schema.hiddenSchemaNodes.indexOf(_Schema.nodeData[res.targetId].name) > -1) {

					// relationship is not displayed

				} else {

					let relIndex = `${res.sourceId}-${res.targetId}`;
					if (relCnt[relIndex] === undefined) {
						relCnt[relIndex] = 0;
					} else {
						relCnt[relIndex]++;
					}

					existingRels[relIndex] = true;
					if (res.targetId !== res.sourceId && existingRels[`${res.targetId}-${res.sourceId}`]) {
						relCnt[relIndex] += existingRels[`${res.targetId}-${res.sourceId}`];
					}

					let stub   = 30 + 80 * relCnt[relIndex];
					let offset =     0.2 * relCnt[relIndex];

					_Schema.ui.jsPlumbInstance.connect({
						source: _Schema.nodeData[`${res.sourceId}_bottom`],
						target: _Schema.nodeData[`${res.targetId}_top`],
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
								label: `<div id="rel_${res.id}" class="flex items-center" data-name="${res.name}" data-source-type="${_Schema.nodeData[res.sourceId].name}" data-target-type="${_Schema.nodeData[res.targetId].name}">
											${(res.relationshipType === _Schema.initialRelType ? '<span>&nbsp;</span>' : res.relationshipType)}
											${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, _Icons.getSvgIconClassesNonColorIcon(['mr-1', 'ml-2', 'edit-relationship-icon']))}
											${(res.isPartOfBuiltInSchema ? '' : _Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'mr-1', 'delete-relationship-icon'])))}
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

						_Helpers.appendInfoTextToElement({
							text: "It is highly advisable to set a relationship type on the relationship! To do this, click the pencil icon to open the edit dialog.<br><br><strong>Note: </strong>Any existing relationships of this type have to be migrated manually.",
							element: $('span', relTypeOverlay),
							customToggleIcon: _Icons.iconWarningYellowFilled,
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
		loadRelationship: (entity, tabsContainer, contentDiv, sourceNode, targetNode, targetView, callbackCancel) => {

			let basicTabContent      = _Entities.appendPropTab(entity, tabsContainer, contentDiv, 'basic', 'Basic', targetView === 'basic');
			let localPropsTabContent = _Entities.appendPropTab(entity, tabsContainer, contentDiv, 'local', 'Direct properties', targetView === 'local');
			let viewsTabContent      = _Entities.appendPropTab(entity, tabsContainer, contentDiv, 'views', 'Views', targetView === 'views');
			let methodsTabContent    = _Entities.appendPropTab(entity, tabsContainer, contentDiv, 'methods', 'Methods', targetView === 'methods', _Editors.resizeVisibleEditors);

			let tabControls = {
				basic            : _Schema.relationships.appendBasicRelInfo(basicTabContent, entity, sourceNode, targetNode, tabsContainer),
				schemaProperties : _Schema.properties.appendLocalProperties(localPropsTabContent, entity),
				schemaViews      : _Schema.views.appendViews(viewsTabContent, entity),
				schemaMethods    : _Schema.methods.appendMethods(methodsTabContent, entity, entity.schemaMethods)
			};

			_Schema.bulkDialogsGeneral.overrideDialogCancel(tabsContainer, callbackCancel);

			Structr.resize();

			return tabControls;
		},
		appendBasicRelInfo: (container, entity, sourceNode, targetNode, mainTabs) => {

			container.insertAdjacentHTML('beforeend', _Schema.templates.relationshipBasicTab());

			_Schema.relationships.appendCascadingDeleteHelpText(container);

			let updateChangeStatus = () => {

				let changedData = _Schema.relationships.getRelationshipDefinitionChanges(container, entity);
				let hasChanges  = Object.keys(changedData).length > 0;

				_Schema.bulkDialogsGeneral.dataChangedInTab(container, hasChanges);
			};

			_Schema.relationships.populateBasicRelInfo(container, entity, sourceNode, targetNode);

			let sourceHelpElements = [];
			let targetHelpElements = [];

			let reactToCardinalityChange = () => {
				_Schema.relationships.reactToCardinalityChange(container, entity, sourceHelpElements, targetHelpElements);
			};

			updateChangeStatus();

			if (Structr.isModuleActive(_Schema)) {

				for (let editLink of container.querySelectorAll('.edit-schema-object')) {

					editLink.addEventListener('click', async (e) => {
						e.stopPropagation();

						let okToNavigate = !_Schema.bulkDialogsGeneral.hasUnsavedChangesInTabs(mainTabs) || (await _Dialogs.confirmation.showPromise("There are unsaved changes - really navigate to related type?"));
						if (okToNavigate) {
							_Schema.openEditDialog(editLink.dataset['objectId']);
						}

						return false;
					});
				}

			} else {

				container.querySelector('.edit-schema-object').classList.remove('edit-schema-object');
			}

			let initActions = () => {

				container.querySelector('#source-json-name').addEventListener('keyup', updateChangeStatus);
				container.querySelector('#target-json-name').addEventListener('keyup', updateChangeStatus);

				container.querySelector('#source-multiplicity-selector').addEventListener('change', updateChangeStatus);
				container.querySelector('#target-multiplicity-selector').addEventListener('change', updateChangeStatus);

				container.querySelector('#relationship-type-name').addEventListener('keyup', updateChangeStatus);

				container.querySelector('#cascading-delete-selector').addEventListener('change', updateChangeStatus);
				container.querySelector('#autocreate-selector').addEventListener('change', updateChangeStatus);
				container.querySelector('#propagation-selector').addEventListener('change', updateChangeStatus);
				container.querySelector('#read-selector').addEventListener('change', updateChangeStatus);
				container.querySelector('#write-selector').addEventListener('change', updateChangeStatus);
				container.querySelector('#delete-selector').addEventListener('change', updateChangeStatus);
				container.querySelector('#access-control-selector').addEventListener('change', updateChangeStatus);
				container.querySelector('#masked-properties').addEventListener('keyup', updateChangeStatus);

				container.querySelector('#source-multiplicity-selector').addEventListener('change', reactToCardinalityChange);
				container.querySelector('#target-multiplicity-selector').addEventListener('change', reactToCardinalityChange);
				container.querySelector('#source-json-name').addEventListener('keyup', reactToCardinalityChange);
				container.querySelector('#target-json-name').addEventListener('keyup', reactToCardinalityChange);
			};
			initActions();

			return {
				getBulkInfo: (doValidate) => {

					let relInfo     = _Schema.relationships.getRelationshipDefinitionDataFromForm(container);
					let allow       = true;
					if (doValidate) {
						allow = _Schema.relationships.validateBasicRelInfo(container, relInfo);
					}
					let changes     = _Schema.relationships.getRelationshipDefinitionChanges(container, entity);
					let changeCount = Object.keys(changes).length;

					return {
						name: 'Basic relationship attributes',
						data: relInfo,
						counts: {
							updated: changeCount
						},
						changes: changes,
						allow: allow
					}
				},
				reset: () => {
					_Schema.relationships.populateBasicRelInfo(container, entity, sourceNode, targetNode);
					reactToCardinalityChange();
					updateChangeStatus();
				},
			};
		},
		populateBasicRelInfo: (container, rel, sourceNode, targetNode) => {

			container.querySelector('#source-type-name').textContent         = sourceNode.name;
			container.querySelector('#target-type-name').textContent         = targetNode.name;
			container.querySelector('#source-type-name').dataset['objectId'] = rel.sourceId;
			container.querySelector('#target-type-name').dataset['objectId'] = rel.targetId;
			container.querySelector('#source-json-name').value               = (rel.sourceJsonName || rel.oldSourceJsonName);
			container.querySelector('#target-json-name').value               = (rel.targetJsonName || rel.oldTargetJsonName);
			container.querySelector('#source-multiplicity-selector').value   = (rel.sourceMultiplicity || '*');
			container.querySelector('#target-multiplicity-selector').value   = (rel.targetMultiplicity || '*');
			container.querySelector('#relationship-type-name').value         = (rel.relationshipType === _Schema.initialRelType ? '' : rel.relationshipType);
			container.querySelector('#cascading-delete-selector').value      = (rel.cascadingDeleteFlag || 0);
			container.querySelector('#autocreate-selector').value            = (rel.autocreationFlag || 0);
			container.querySelector('#propagation-selector').value           = (rel.permissionPropagation || 'None');
			container.querySelector('#read-selector').value                  = (rel.readPropagation || 'Remove');
			container.querySelector('#write-selector').value                 = (rel.writePropagation || 'Remove');
			container.querySelector('#delete-selector').value                = (rel.deletePropagation || 'Remove');
			container.querySelector('#access-control-selector').value        = (rel.accessControlPropagation || 'Remove');
			container.querySelector('#masked-properties').value              = (rel.propertyMask);

			if (rel.isPartOfBuiltInSchema) {

				container.querySelector('#source-type-name').disabled             = true;
				container.querySelector('#target-type-name').disabled             = true;
				container.querySelector('#source-json-name').disabled             = true;
				container.querySelector('#target-json-name').disabled             = true;
				container.querySelector('#source-multiplicity-selector').disabled = true;
				container.querySelector('#target-multiplicity-selector').disabled = true;
				container.querySelector('#relationship-type-name').disabled       = true;
				container.querySelector('#cascading-delete-selector').disabled    = true;
				container.querySelector('#autocreate-selector').disabled          = true;
				container.querySelector('#propagation-selector').disabled         = true;
				container.querySelector('#read-selector').disabled                = true;
				container.querySelector('#write-selector').disabled               = true;
				container.querySelector('#delete-selector').disabled              = true;
				container.querySelector('#access-control-selector').disabled      = true;
				container.querySelector('#masked-properties').disabled            = true;
			}
		},
		reactToCardinalityChange: (container, entity, sourceHelpElements, targetHelpElements) => {

			sourceHelpElements.map(e => e.remove());
			while (sourceHelpElements.length > 0) sourceHelpElements.pop();

			let sourceMultiplicitySelect = container.querySelector('#source-multiplicity-selector');
			let sourceJsonNameInput      = container.querySelector('#source-json-name');

			if (sourceMultiplicitySelect.value !== (entity.sourceMultiplicity || '*') && sourceJsonNameInput.value === (entity.sourceJsonName || entity.oldSourceJsonName)) {
				for (let el of _Schema.relationships.appendCardinalityChangeInfo(sourceJsonNameInput)) {
					sourceHelpElements.push(el);
				}
			}

			targetHelpElements.map(e => e.remove());
			while (targetHelpElements.length > 0) targetHelpElements.pop();

			let targetMultiplicitySelect = container.querySelector('#target-multiplicity-selector');
			let targetJsonNameInput      = container.querySelector('#target-json-name');

			if (targetMultiplicitySelect.value !== (entity.targetMultiplicity || '*') && targetJsonNameInput.value === (entity.targetJsonName || entity.oldTargetJsonName)) {
				for (let el of _Schema.relationships.appendCardinalityChangeInfo(targetJsonNameInput)) {
					targetHelpElements.push(el);
				}
			}
		},
		appendCardinalityChangeInfo: (el) => {

			return _Helpers.appendInfoTextToElement({
				text: 'Multiplicity was changed without changing the remote property name - make sure this is correct',
				element: el,
				insertAfter: true,
				customToggleIcon: _Icons.iconWarningYellowFilled,
				customToggleIconClasses: ['ml-2'],
				helpElementCss: {
					fontSize: '9pt'
				}
			});
		},
		validateBasicRelInfo: (container, relInfo) => {

			let allow = true;

			// check relationshipType against regex
			let match = relInfo.relationshipType.match(/^[a-zA-Z][a-zA-Z0-9_]*$/);
			if (match === null) {
				allow = false;
				_Helpers.blinkRed(container.querySelector('#relationship-type-name'));
			}

			return allow;
		},
		showCreateRelationshipDialog: (info) => {

			let sourceId = Structr.getIdFromPrefixIdString(info.sourceId, 'id_');
			let targetId = Structr.getIdFromPrefixIdString(info.targetId, 'id_');

			let { dialogText } = _Dialogs.custom.openDialog("Create Relationship", ['schema-edit-dialog']);

			dialogText.insertAdjacentHTML('beforeend', _Schema.templates.relationshipBasicTab());

			_Schema.relationships.appendCascadingDeleteHelpText(dialogText);

			let sourceTypeName = _Schema.nodeData[sourceId].name;
			let targetTypeName = _Schema.nodeData[targetId].name;

			dialogText.querySelector('#source-type-name').textContent = sourceTypeName;
			dialogText.querySelector('#target-type-name').textContent = targetTypeName;

			let previousSourceSuggestion = '';
			let previousTargetSuggestion = '';

			let updateSuggestions = () => {

				let currentSourceSuggestion = _Schema.relationships.createRemotePropertyNameSuggestion(sourceTypeName, dialogText.querySelector('#source-multiplicity-selector').value);
				let currentTargetSuggestion = _Schema.relationships.createRemotePropertyNameSuggestion(targetTypeName, dialogText.querySelector('#target-multiplicity-selector').value);

				if (currentSourceSuggestion === currentTargetSuggestion) {
					currentSourceSuggestion += '_source';
					currentTargetSuggestion += '_target';
				}

				let sourceJsonNameInput = dialogText.querySelector('#source-json-name');
				if (previousSourceSuggestion === sourceJsonNameInput.value) {

					sourceJsonNameInput.value = currentSourceSuggestion;
					previousSourceSuggestion  = currentSourceSuggestion;
				}

				let targetJsonNameInput = dialogText.querySelector('#target-json-name');
				if (previousTargetSuggestion === targetJsonNameInput.value) {

					targetJsonNameInput.value = currentTargetSuggestion;
					previousTargetSuggestion  = currentTargetSuggestion;
				}
			};
			updateSuggestions();

			dialogText.querySelector('#source-multiplicity-selector').addEventListener('change', updateSuggestions);
			dialogText.querySelector('#target-multiplicity-selector').addEventListener('change', updateSuggestions);

			if (Structr.isModuleActive(_Schema)) {

				let discardButton = _Dialogs.custom.prependCustomDialogButton(_Schema.templates.discardActionButton({ text: 'Discard and close' }));
				let saveButton    = _Dialogs.custom.prependCustomDialogButton(_Schema.templates.saveActionButton({ text: 'Create' }));

				_Dialogs.custom.setDialogSaveButton(saveButton);

				saveButton.addEventListener('click', async () => {

					let relData = _Schema.relationships.getRelationshipDefinitionDataFromForm(dialogText);
					relData.sourceId = sourceId;
					relData.targetId = targetId;

					let allow = _Schema.relationships.validateBasicRelInfo(dialogText, relData);

					if (allow) {

						await _Schema.relationships.createRelationshipDefinition(relData);
					}
				});

				// replace old cancel button with "discard button" to enable global ESC handler
				_Dialogs.custom.replaceDialogCloseButton(discardButton, false);
				discardButton.addEventListener('click', () => {

					_Schema.currentNodeDialogId = null;

					_Schema.ui.jsPlumbInstance.repaintEverything();
					_Schema.ui.jsPlumbInstance.detach(info.connection);

					_Dialogs.custom.dialogCancelBaseAction();
				});
			}

			Structr.resize();
		},
		getRelationshipDefinitionDataFromForm: (container) => {

			return {
				relationshipType:         container.querySelector('#relationship-type-name').value,
				sourceMultiplicity:       container.querySelector('#source-multiplicity-selector').value,
				targetMultiplicity:       container.querySelector('#target-multiplicity-selector').value,
				sourceJsonName:           container.querySelector('#source-json-name').value,
				targetJsonName:           container.querySelector('#target-json-name').value,
				cascadingDeleteFlag:      parseInt(container.querySelector('#cascading-delete-selector').value),
				autocreationFlag:         parseInt(container.querySelector('#autocreate-selector').value),
				permissionPropagation:    container.querySelector('#propagation-selector').value,
				readPropagation:          container.querySelector('#read-selector').value,
				writePropagation:         container.querySelector('#write-selector').value,
				deletePropagation:        container.querySelector('#delete-selector').value,
				accessControlPropagation: container.querySelector('#access-control-selector').value,
				propertyMask:             container.querySelector('#masked-properties').value
			};

		},
		getRelationshipDefinitionChanges: (container, entity) => {

			let newData = _Schema.relationships.getRelationshipDefinitionDataFromForm(container);

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

				new WarningMessage().title(`Unsupported cardinality: ${cardinality}`).text('Unable to generate suggestion for linked property name. Unsupported cardinality encountered!').requiresConfirmation().show();
				return typeName;
			}
		},
		appendCascadingDeleteHelpText: (container) => {

			_Helpers.appendInfoTextToElement({
				text: '<dl class="help-definitions"><dt>NONE</dt><dd>No cascading delete</dd><dt>SOURCE_TO_TARGET</dt><dd>Delete target node when source node is deleted</dd><dt>TARGET_TO_SOURCE</dt><dd>Delete source node when target node is deleted</dd><dt>ALWAYS</dt><dd>Delete source node if target node is deleted AND delete target node if source node is deleted</dd><dt>CONSTRAINT_BASED</dt><dd>Delete source or target node if deletion of the other side would result in a constraint violation on the node (e.g. notNull constraint)</dd></dl>',
				element: container.querySelector('#cascading-delete-selector'),
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

			let getResponse = await fetch(`${Structr.rootUrl}schema_relationship_nodes/${entity.id}`);

			if (getResponse.ok) {

				let existingData = await getResponse.json();

				let hasChanges = Object.keys(newData).some((key) => {
					return (existingData.result[key] !== newData[key]);
				});

				if (hasChanges) {

					let putResponse = await fetch(`${Structr.rootUrl}schema_relationship_nodes/${entity.id}`, {
						method: 'PUT',
						body: JSON.stringify(newData)
					});

					_Schema.hideSchemaRecompileMessage();
					let responseData = await putResponse.json();

					if (putResponse.ok) {

						for (let attribute in newData) {
							_Helpers.blinkGreen($(`#relationship-options [data-attr-name=${attribute}]`));
							entity[attribute] = newData[attribute];
						}

						return true;

					} else {

						_Schema.relationships.reportRelationshipError(existingData.result, newData, responseData);

						for (let attribute in newData) {
							_Helpers.blinkRed($(`#relationship-options [data-attr-name=${attribute}]`));
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

			for (let error of responseData.errors) {
				_Helpers.blinkRed($(`#relationship-options [data-attr-name=${error.property}]`));
			}

			let additionalInformation  = {};
			let hasDuplicateClassError = responseData.errors.some(e => (e.token === 'duplicate_relationship_definition'));

			if (hasDuplicateClassError) {

				let relType = newData?.relationshipType ?? relData.relationshipType;

				additionalInformation.requiresConfirmation = true;
				additionalInformation.title                = 'Error';
				additionalInformation.errorExplanation     = `You are trying to create a second relationship named <strong>${relType}</strong> between these types.<br>Relationship names between types have to be unique.`;
				additionalInformation.requiresConfirmation = true;
			}

			Structr.errorFromResponse(responseData, null, additionalInformation);
		},
		askDeleteRelationship: (resId, name) => {

			_Dialogs.confirmation.showPromise(`<h3>Delete schema relationship${(name ? ` '${name}'` : '')}?</h3>`).then(async confirm => {
				if (confirm === true) {
					await _Schema.relationships.removeRelationshipDefinition(resId);
				}
			});
		},
	},
	properties: {
		appendLocalProperties: (container, entity, overrides, optionalAfterSaveCallback) => {

			let dbNameClass = (UISettings.getValueForSetting(UISettings.settingGroups.schema_code.settings.showDatabaseNameForDirectProperties) === true) ? '' : 'hidden';

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

			let propertiesTable = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.schemaTable(tableConfig));
			container.appendChild(propertiesTable);
			let tbody = propertiesTable.querySelector('tbody');

			_Helpers.sort(entity.schemaProperties);

			let typeOptions       = _Schema.templates.typeOptions();
			let typeHintOptions   = _Schema.templates.typeHintOptions();

			for (let prop of entity.schemaProperties) {

				let row = $(_Schema.templates.propertyLocal({ property: prop, typeOptions: typeOptions, typeHintOptions: typeHintOptions, dbNameClass: dbNameClass }));
				tbody.appendChild(row[0]);

				_Schema.properties.setAttributesInRow(prop, row);
				_Schema.properties.bindRowEvents(prop, row, overrides);

				_Schema.properties.checkProperty(row);

				_Schema.bulkDialogsGeneral.tableChanged(propertiesTable);
			}

			let resetFunction = () => {
				for (let discardIcon of tbody.querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
			};

			container.querySelector('.discard-all').addEventListener('click', resetFunction);

			propertiesTable.querySelector('.save-all').addEventListener('click', () => {
				_Schema.properties.bulkSave(container, tbody, entity, overrides, optionalAfterSaveCallback);
			});

			container.querySelector('button.add-button').addEventListener('click', () => {

				let tr = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.propertyNew({ typeOptions: typeOptions, dbNameClass: dbNameClass }));
				tbody.appendChild(tr);

				let propertyTypeSelect = tr.querySelector('.property-type');

				propertyTypeSelect.addEventListener('change', () => {

					let selectedOption = propertyTypeSelect.querySelector('option:checked');
					let indexedCb      = tr.querySelector('.indexed');
					let shouldIndex    = selectedOption.dataset['indexed'];
					shouldIndex = (shouldIndex === undefined) || (shouldIndex !== 'false');

					if (indexedCb.checked !== shouldIndex) {

						indexedCb.checked = shouldIndex;

						_Helpers.blink(indexedCb.closest('td'), '#fff', '#bde5f8');
						_Dialogs.custom.showAndHideInfoBoxMessage('Automatically updated indexed flag to default behavior for property type (you can still override this)', 'info', 2000, 200);
					}
				});

				tr.querySelector('.discard-changes').addEventListener('click', () => {
					_Helpers.fastRemoveElement(tr);
					_Schema.bulkDialogsGeneral.tableChanged(propertiesTable);
				});

				_Schema.bulkDialogsGeneral.tableChanged(propertiesTable);
			});

			return {
				getBulkInfo: (doValidate) => {
					return _Schema.properties.getDataFromTable(tbody, entity, doValidate);
				},
				reset: resetFunction
			};
		},
		getDataFromTable: (tbody, entity, doValidate = true) => {

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

			for (let tr of tbody.querySelectorAll('tr')) {

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
		bulkSave: (container, tbody, entity, overrides, optionalAfterSaveCallback) => {

			let { allow, counts, data } = _Schema.properties.getDataFromTable(tbody, entity);

			if (allow) {

				_Schema.showSchemaRecompileMessage();

				fetch(Structr.rootUrl + entity.id, {
					method: 'PUT',
					body: JSON.stringify(data)
				}).then((response) => {

					if (response.ok) {

						Command.get(entity.id, null, (reloadedEntity) => {

							_Helpers.fastRemoveAllChildren(container);

							_Schema.properties.appendLocalProperties(container, reloadedEntity, overrides, optionalAfterSaveCallback);
							_Schema.hideSchemaRecompileMessage();

							_Schema.invalidateTypeInfoCache(entity.name);

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

			let propertyTypeOption = $(`.property-type option[value="${property.propertyType}"]`, row);
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

					let unsavedChanges = _Schema.bulkDialogsGeneral.hasUnsavedChangesInTable(row[0].closest('table'));

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

					let unsavedChanges = _Schema.bulkDialogsGeneral.hasUnsavedChangesInTable(row[0].closest('table'));

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
		checkProperty: (row) => {

			let propertyInfoUI = _Schema.properties.getInfoFromRow(row);

			if (propertyInfoUI.propertyType === 'Function') {

				let container = row[0].querySelector('.indexed').closest('td');

				_Schema.properties.checkFunctionProperty(propertyInfoUI, container);
			}
		},
		checkFunctionProperty: (propertyInfoUI, containerForWarning) => {

			let warningClassName = 'indexed-function-property-warning';
			let existingWarning  = containerForWarning.querySelector(`.${warningClassName}`);

			if (propertyInfoUI.indexed === true && (!propertyInfoUI.typeHint || propertyInfoUI.typeHint === '')) {

				if (!existingWarning) {

					let warningEl = _Helpers.createSingleDOMElementFromHTML(`<span class="${warningClassName}"></span>`);
					containerForWarning.append(warningEl);

					_Helpers.appendInfoTextToElement({
						element: warningEl,
						text: `Searching on this attribute only works if a type hint is set along with the indexed flag. A re-indexing may be necessary if applied after data has already been created.`,
						customToggleIcon: _Icons.iconWarningYellowFilled,
					});
				}

			} else {

				_Helpers.fastRemoveElement(existingWarning);
			}
		},
		rowChanged: (property, row) => {

			let propertyInfoUI = _Schema.properties.getInfoFromRow(row);
			let hasChanges     = false;

			_Schema.properties.checkProperty(row);

			for (let key in propertyInfoUI) {

				if ((propertyInfoUI[key] === '' || propertyInfoUI[key] === null || propertyInfoUI[key] === undefined) && (property[key] === '' || property[key] === null || property[key] === undefined)) {
					// account for different attribute-sets and fuzzy equality
				} else if (propertyInfoUI[key] !== property[key]) {

					if (property.propertyType === 'Cypher' && key === 'format') {
						// ignore "changes" in format... it is not display and collected
					} else {
						hasChanges = true;
					}
				}
			}

			_Schema.markElementAsChanged(row[0], hasChanges);

			_Schema.bulkDialogsGeneral.tableChanged(row[0].closest('table'));
		},
		validateProperty: (propertyDefinition, tr) => {

			if (propertyDefinition.name.length === 0) {

				_Helpers.blinkRed($('.property-name', tr).closest('td'));
				return false;

			} else if (propertyDefinition.propertyType.length === 0) {

				_Helpers.blinkRed($('.property-type', tr).closest('td'));
				return false;

			} else if (propertyDefinition.propertyType === 'Enum' && propertyDefinition.format.trim().length === 0) {

				_Helpers.blinkRed($('.property-format', tr).closest('td'));
				return false;

			} else if (propertyDefinition.propertyType === 'Enum') {

				var containsSpace = propertyDefinition.format.split(',').some(function (enumVal) {
					return enumVal.trim().indexOf(' ') !== -1;
				});

				if (containsSpace) {
					_Helpers.blinkRed($('.property-format', tr).closest('td'));
					new WarningMessage().text(`Enum values must be separated by commas and can not contain spaces<br>See the <a href="${_Helpers.getDocumentationURLForTopic('schema-enum')}" target="_blank">support article on enum properties</a> for more information.`).requiresConfirmation().show();
					return false;
				}
			}

			return true;
		},
		openCodeEditorForFunctionProperty: (id, key, closeCallback) => {

			Command.get(id, `id,name,contentType,${key}`, (entity) => {

				let { dialogText, dialogMeta } = _Dialogs.custom.openDialog(`Edit ${key} of ${entity.name}`, closeCallback, ['popup-dialog-with-editor']);
				_Dialogs.custom.showMeta();

				let initialText = entity[key] || '';

				dialogText.insertAdjacentHTML('beforeend', '<div class="editor h-full"></div>');
				dialogMeta.insertAdjacentHTML('beforeend', '<span class="editor-info"></span>');

				let dialogSaveButton = _Dialogs.custom.updateOrCreateDialogSaveButton();
				let saveAndClose     = _Dialogs.custom.updateOrCreateDialogSaveAndCloseButton();
				let editorInfo       = dialogMeta.querySelector('.editor-info');
				_Editors.appendEditorOptionsElement(editorInfo);

				let functionPropertyMonacoConfig = {
					language: 'auto',
					lint: true,
					autocomplete: true,
					changeFn: (editor, entity) => {

						let disabled = (initialText === editor.getValue());
						_Helpers.disableElements(disabled, dialogSaveButton, saveAndClose);
					},
					saveFn: (editor, entity, close = false) => {

						let text1 = initialText;
						let text2 = editor.getValue();

						if (text1 === text2) {
							return;
						}

						Command.setProperty(entity.id, key, text2, false, () => {

							_Dialogs.custom.showAndHideInfoBoxMessage('Code saved.', 'success', 2000, 200);

							_Helpers.disableElements(true, dialogSaveButton, saveAndClose);

							Command.getProperty(entity.id, key, (newText) => {
								initialText = newText;
							});

							if (close === true) {
								_Dialogs.custom.clickDialogCancelButton();
							}
						});
					},
					saveFnText: `Save ${key} Function`,
					preventRestoreModel: true,
					isAutoscriptEnv: true
				};

				let editor = _Editors.getMonacoEditor(entity, key, dialogText.querySelector('.editor'), functionPropertyMonacoConfig);

				_Editors.resizeVisibleEditors();
				Structr.resize();

				dialogSaveButton.addEventListener('click', (e) => {
					e.stopPropagation();

					functionPropertyMonacoConfig.saveFn(editor, entity);
				});

				saveAndClose.addEventListener('click', (e) => {
					e.stopPropagation();

					functionPropertyMonacoConfig.saveFn(editor, entity, true);
				});

				_Editors.focusEditor(editor);
			});
		},
		openCodeEditorForCypherProperty: (id, closeCallback) => {

			Command.get(id, 'id,name,format', (entity) => {

				let { dialogText, dialogMeta } = _Dialogs.custom.openDialog(`Edit cypher query of ${entity.name}`, closeCallback, ['popup-dialog-with-editor']);
				_Dialogs.custom.showMeta();

				let key         = 'format';
				let initialText = entity[key] || '';

				dialogText.insertAdjacentHTML('beforeend', '<div class="editor h-full"></div>');
				dialogMeta.insertAdjacentHTML('beforeend', '<span class="editor-info"></span>');

				let dialogSaveButton = _Dialogs.custom.updateOrCreateDialogSaveButton();
				let saveAndClose     = _Dialogs.custom.updateOrCreateDialogSaveAndCloseButton();
				let editorInfo       = dialogMeta.querySelector('.editor-info');
				_Editors.appendEditorOptionsElement(editorInfo);

				let cypherPropertyMonacoConfig = {
					language: 'cypher',
					lint: false,
					autocomplete: false,
					changeFn: (editor, entity) => {

						let disabled = (initialText === editor.getValue());
						_Helpers.disableElements(disabled, dialogSaveButton, saveAndClose);
					},
					saveFn: (editor, entity, close = false) => {

						let text1 = initialText;
						let text2 = editor.getValue();

						if (text1 === text2) {
							return;
						}

						_Schema.showSchemaRecompileMessage();

						Command.setProperty(entity.id, key, text2, false, () => {

							_Schema.hideSchemaRecompileMessage();

							_Dialogs.custom.showAndHideInfoBoxMessage('Code saved.', 'success', 2000, 200);
							_Helpers.disableElements(true, dialogSaveButton, saveAndClose);

							Command.getProperty(entity.id, key, (newText) => {
								initialText = newText;
							});

							if (close === true) {
								_Dialogs.custom.clickDialogCancelButton();
							}
						});
					},
					saveFnText: `Save Cypher Property Query`,
					preventRestoreModel: true,
					isAutoscriptEnv: false
				};

				let editor = _Editors.getMonacoEditor(entity, key, dialogText.querySelector('.editor'), cypherPropertyMonacoConfig);

				_Editors.resizeVisibleEditors();
				Structr.resize();

				dialogSaveButton.addEventListener('click', (e) => {
					e.stopPropagation();

					cypherPropertyMonacoConfig.saveFn(editor, entity);
				});

				saveAndClose.addEventListener('click', (e) => {
					e.stopPropagation();

					cypherPropertyMonacoConfig.saveFn(editor, entity, true);
				});

				_Editors.focusEditor(editor);
			});
		},
		appendBuiltinProperties: (container, entity) => {

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

			let propertiesTable = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.schemaTable(tableConfig));
			let tbody           = propertiesTable.querySelector('tbody')
			propertiesTable.querySelector('tfoot').classList.add('hidden');

			container.appendChild(propertiesTable);

			_Helpers.sort(entity.schemaProperties);

			Command.listSchemaProperties(entity.id, 'ui', (data) => {

				// sort by name
				_Helpers.sort(data, "declaringClass", "name");

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

						tbody.insertAdjacentHTML('beforeend', _Schema.templates.propertyBuiltin({ property: property }));
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
		asyncEditSchemaObjectLinkHandler: async (el, mainTabs, previousEntityId) => {
			let okToNavigate = !_Schema.bulkDialogsGeneral.hasUnsavedChangesInTabs(mainTabs) || (await _Dialogs.confirmation.showPromise("There are unsaved changes - really navigate to related type?"));
			if (okToNavigate) {
				_Schema.openEditDialog(el.dataset['objectId'], undefined, () => {
					window.setTimeout(() => {
						_Schema.openEditDialog(previousEntityId);
					}, 250);
				});
			}
		},
		appendRemote: (container, entity, editSchemaObjectLinkHandler, optionalAfterSaveCallback) => {

			let tableConfig = {
				class: 'related-attrs schema-props',
				cols: [
					{ class: '', title: 'JSON Name' },
					{ class: '', title: 'Type, direction and related type' },
					{ class: 'actions-col', title: 'Action' }
				]
			};

			let table = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.schemaTable(tableConfig));
			let tbody = table.querySelector('tbody');
			container.appendChild(table);

			if (entity.relatedTo.length === 0 && entity.relatedFrom.length === 0) {

				tbody.insertAdjacentHTML('beforeend', '<tr><td colspan=3 class="no-rels">Type has no relationships...</td></tr>');
				table.querySelector('tfoot').classList.add('hidden');

			} else {

				for (let target of entity.relatedTo) {
					_Schema.remoteProperties.appendRemoteProperty(tbody, target, true, editSchemaObjectLinkHandler);
				}

				for (let source of entity.relatedFrom) {
					_Schema.remoteProperties.appendRemoteProperty(tbody, source, false, editSchemaObjectLinkHandler);
				}
			}

			let resetFunction = () => {
				for (let discardIcon of table.querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
			};

			table.querySelector('.discard-all').addEventListener('click', resetFunction);

			table.querySelector('.save-all').addEventListener('click', () => {
				_Schema.remoteProperties.bulkSave(container, tbody, entity, editSchemaObjectLinkHandler, optionalAfterSaveCallback);
			});

			_Schema.bulkDialogsGeneral.tableChanged(table);

			return {
				getBulkInfo: (doValidate) => {
					return _Schema.remoteProperties.getDataFromTable(tbody, entity, doValidate);
				},
				reset: resetFunction
			};
		},
		getDataFromTable: (tbody, entity, doValidate = true) => {

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

			for (let tr of tbody.querySelectorAll('tr')) {

				let relId            = tr.dataset['relationshipId'];
				let propertyName     = tr.dataset['propertyName'];
				let targetCollection = tr.dataset['targetCollection'];

				if (relId) {

					let info = {
						id: relId
					};

					info[propertyName] = tr.querySelector('.property-name').value;
					if (info[propertyName] === '') {
						info[propertyName] = null;
					}

					if (tr.classList.contains('has-changes')) {

						if (doValidate) {
							allow = _Schema.remoteProperties.validate(tr) && allow;
						}

						if (info[propertyName] === null) {
							counts.reset++;
						} else {
							counts.update++;
						}
					}

					data[targetCollection].push(info);
				}
			}

			return { name, data, allow, counts };
		},
		bulkSave: (el, tbody, entity, editSchemaObjectLinkHandler, optionalAfterSaveCallback) => {

			let { allow, counts, data } = _Schema.remoteProperties.getDataFromTable(tbody, entity);

			if (allow) {

				_Schema.showSchemaRecompileMessage();

				fetch(Structr.rootUrl + entity.id, {
					method: 'PUT',
					body: JSON.stringify(data)
				}).then((response) => {

					if (response.ok) {

						Command.get(entity.id, null, (reloadedEntity) => {

							_Helpers.fastRemoveElement(el);

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

				let row = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.remoteProperty(tplConfig));
				el.appendChild(row);

				row.querySelector('.property-name').addEventListener('keyup', () => {
					_Schema.remoteProperties.rowChanged(row, attributeName);
				});

				row.querySelector('.reset-action').addEventListener('click', () => {
					row.querySelector('.property-name').value = '';
					_Schema.remoteProperties.rowChanged(row, attributeName);
				});

				row.querySelector('.discard-changes').addEventListener('click', () => {
					$('.property-name', row).val(attributeName);
					_Schema.remoteProperties.rowChanged(row, attributeName);
				});

				if (Structr.isModuleActive(_Schema)) {

					for (let otherSchemaTypeLink of row.querySelectorAll('.edit-schema-object')) {

						otherSchemaTypeLink.addEventListener('click', async (e) => {
							e.stopPropagation();

							editSchemaObjectLinkHandler(e.target);

							return false;
						});
					}

				} else {

					row.querySelector('.edit-schema-object').classList.remove('edit-schema-object');
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

			let nameInUI   = row.querySelector('.property-name').value;
			let hasChanges = (nameInUI !== originalName);

			_Schema.markElementAsChanged(row, hasChanges);

			_Schema.bulkDialogsGeneral.tableChanged(row.closest('table'));
		},
		validate: (row) => {
			return true;
		}
	},
	views: {
		initialViewConfig: {},
		viewsTableConfig: {
			class: 'related-attrs schema-props',
			cols: [
				{ class: '', title: 'Name' },
				{ class: '', title: 'Properties' },
				{ class: 'actions-col', title: 'Action' }
			],
			addButtonText: 'Add view'
		},
		protectedViewsList: ['all', 'ui', 'custom'],	// not allowed to add/remove properties, but allowed to sort
		isViewEditable: (view) => {
			return (_Schema.views.isViewNameChangeForbidden(view) === false || _Schema.views.isViewPropertiesChangeForbidden(view) === false);
		},
		isDeleteViewAllowed: (view) => {
			return (view.isBuiltinView === false && _Schema.views.protectedViewsList.includes(view.name) === false);
		},
		isViewNameChangeForbidden: (view) => {
			return (view.isBuiltinView === true || _Schema.views.protectedViewsList.includes(view.name) === true);
		},
		isViewPropertiesChangeForbidden: (view) => {
			return _Schema.views.protectedViewsList.includes(view.name);
		},
		appendViews: (container, entity, optionalAfterSaveCallback) => {

			let viewsTable = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.schemaTable(_Schema.views.viewsTableConfig));
			let tbody      = viewsTable.querySelector('tbody');
			container.appendChild(viewsTable);

			_Helpers.sort(entity.schemaViews);

			for (let view of entity.schemaViews) {
				_Schema.views.appendView(tbody, view, entity);
			}

			let resetFunction = () => {
				for (let discardIcon of tbody.querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
			};
			viewsTable.querySelector('.discard-all').addEventListener('click', resetFunction);

			viewsTable.querySelector('.save-all').addEventListener('click', () => {
				_Schema.views.bulkSave(container, tbody, entity, optionalAfterSaveCallback);
			});

			container.querySelector('button.add-button').addEventListener('click', () => {

				let tr = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.viewNew());
				tbody.appendChild(tr);

				_Schema.views.appendViewSelectionElement(tr, { name: 'new' }, entity, (selectElement) => {

					selectElement.select2Sortable();

					_Schema.bulkDialogsGeneral.tableChanged(viewsTable);
				});

				tr.querySelector('.discard-changes').addEventListener('click', () => {
					_Helpers.fastRemoveElement(tr);
					_Schema.bulkDialogsGeneral.tableChanged(viewsTable);
				});
			});

			_Schema.bulkDialogsGeneral.tableChanged(viewsTable);

			return {
				getBulkInfo: (doValidate) => {
					return _Schema.views.getDataFromTable(tbody, entity, doValidate);
				},
				reset: resetFunction
			};
		},
		getDataFromTable: (tbody, entity, doValidate = true) => {

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

			for (let tr of tbody.querySelectorAll('tr')) {

				let viewId = tr.dataset['viewId'];
				let view   = _Schema.views.getInfoFromRow(tr, entity);

				if (viewId) {

					if (tr.classList.contains('to-delete')) {

						// do not add this property to the list
						counts.delete++;

					} else if (tr.classList.contains('has-changes')) {

						// changed lines
						counts.update++;
						view.id = viewId;
						if (doValidate) {
							allow = _Schema.views.validateViewRow(tr) && allow;
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
						allow = _Schema.views.validateViewRow(tr) && allow;
					}
					data.schemaViews.push(view);
				}
			}

			return { name, data, allow, counts };
		},
		bulkSave: (el, tbody, entity, optionalAfterSaveCallback) => {

			let { data, allow, counts } = _Schema.views.getDataFromTable(tbody, entity);

			if (allow) {

				_Schema.showSchemaRecompileMessage();

				fetch(Structr.rootUrl + entity.id, {
					method: 'PUT',
					body: JSON.stringify(data)
				}).then((response) => {

					if (response.ok) {

						Command.get(entity.id, null, (reloadedEntity) => {
							_Helpers.fastRemoveAllChildren(el);
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
		appendView: (tbody, view, entity) => {

			let tr = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.view({ view: view, type: entity }));
			tbody.appendChild(tr);

			_Schema.views.appendViewSelectionElement(tr, view, entity, (selectElement) => {

				// store initial configuration for later comparison
				let initialViewConfig = _Schema.views.getInfoFromRow(tr, entity);

				// store initial configuration for each view to be able to determine excluded properties later
				_Schema.views.initialViewConfig[view.id] = initialViewConfig;

				selectElement.select2Sortable(() => {
					_Schema.views.rowChanged(tr, entity, initialViewConfig);
				});

				_Schema.views.bindRowEvents(tr, entity, view, initialViewConfig);
			});
		},
		bindRowEvents: (tr, entity, view, initialViewConfig) => {

			let viewInfoChangeHandler = () => { _Schema.views.rowChanged(tr, entity, initialViewConfig); };

			let nameInput    = tr.querySelector('.view.property-name');
			let nameDisabled = _Schema.views.isViewNameChangeForbidden(view);

			if (nameDisabled) {
				nameInput.disabled = true;
				nameInput.classList.add('cursor-not-allowed');
			} else {
				nameInput.addEventListener('input', viewInfoChangeHandler);
			}

			// jquery is required for change handler of select2 plugin
			$(tr.querySelector('.view.property-attrs')).on('change', viewInfoChangeHandler);

			tr.querySelector('.discard-changes')?.addEventListener('click', () => {

				tr.querySelector('.view.property-name').value = view.name;

				let select = tr.querySelector('select');
				Command.listSchemaProperties(entity.id, view.name, (data) => {

					for (let prop of data) {
						let option = select.querySelector(`option[value="${prop.name}"]`);
						if (option) {
							option.selected = prop.isSelected;
						}
					}

					select.dispatchEvent(new CustomEvent('change'));

					tr.classList.remove('to-delete');
					tr.classList.remove('has-changes');

					_Schema.bulkDialogsGeneral.tableChanged(tr.closest('table'));
				});
			});

			let removeAction = tr.querySelector('.remove-action');
			if (removeAction) {
				removeAction.addEventListener('click', () => {

					tr.classList.add('to-delete');
					viewInfoChangeHandler();

				})
				removeAction.disabled = false;
			}
		},
		appendPropertyForViewSelect: (viewSelectElem, view, prop) => {

			let viewIsEditable = _Schema.views.isViewEditable(view);

			// never show internalEntityContextPath
			if (prop.name !== 'internalEntityContextPath') {

				let isSelected = prop.isSelected ? ' selected="selected"' : '';
				let isDisabled = prop.isDisabled ? ' disabled="disabled"' : '';

				if (viewIsEditable || prop.isSelected) {
					viewSelectElem.insertAdjacentHTML('beforeend', `<option value="${prop.name}"${isSelected}${isDisabled}>${prop.name}</option>`);
				}
			}
		},
		appendViewSelectionElement: (row, view, schemaEntity, callback) => {

			let viewIsEditable = _Schema.views.isViewEditable(view);
			let viewSelectElem = row.querySelector('.property-attrs');

			Command.listSchemaProperties(schemaEntity.id, view.name, (properties) => {

				if (view.sortOrder) {

					for (let sortedProp of view.sortOrder.split(',')) {

						let prop = properties.filter(prop => (prop.name === sortedProp));

						if (prop.length > 0) {

							_Schema.views.appendPropertyForViewSelect(viewSelectElem, view, prop[0]);

							properties = properties.filter(prop => (prop.name !== sortedProp));
						}
					}
				}

				for (let prop of properties) {
					_Schema.views.appendPropertyForViewSelect(viewSelectElem, view, prop);
				}

				let dropdownParent = _Dialogs.custom.isDialogOpen() ? $(_Dialogs.custom.getDialogBoxElement()) : $('body');

				let selectElement = $(viewSelectElem).select2({
					search_contains: true,
					width: '100%',
					dropdownCssClass: 'select2-sortable hide-selected-options hide-disabled-options',
					containerCssClass: 'select2-sortable hide-selected-options hide-disabled-options' + (viewIsEditable ? '' : ' not-editable'),
					closeOnSelect: false,
					scrollAfterSelect: false,
					dropdownParent: dropdownParent
				});

				if (!viewIsEditable) {
					// prevent removal of elements for views that we consider "not editable"...
					selectElement.on("select2:unselecting", function (e) {
						e.preventDefault();
					});
				}

				callback(selectElement);
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

			for (let prop of document.querySelectorAll('.builtin.schema-props tr')) {

				let name = prop.dataset.propertyName;
				let id   = prop.dataset.propertyId;

				if (names.includes(name)) {
					const data = { name: name };
					if (id && id !== 'null') {
						data.id = id;
					}
					inheritedProperties.push(data);
				}
			}

			return inheritedProperties;
		},
		findExcludedProperties: (entity, names, tr) => {

			let excludedProps = [];

			let viewId            = tr.dataset['viewId'];
			let initialViewConfig = _Schema.views.initialViewConfig[viewId];

			if (initialViewConfig && initialViewConfig.hasOwnProperty('nonGraphProperties')) {
				excludedProps = _Schema.views.findInheritedPropertyByName(entity, initialViewConfig.nonGraphProperties.split(',').map(p => p.trim()).filter(p => !names.includes(p)));
			}

			return excludedProps;
		},
		getInfoFromRow: (tr, schemaNodeEntity) => {

			const sortedAttrs = $(tr.querySelector('.view.property-attrs')).sortedValues();

			const data = {
				name: tr.querySelector('.view.property-name').value,
				schemaProperties: _Schema.views.findSchemaPropertiesByNodeAndName(schemaNodeEntity, sortedAttrs),
				nonGraphProperties: _Schema.views.findNonGraphProperties(schemaNodeEntity, sortedAttrs),
				//excludedProperties: _Schema.views.findExcludedProperties(schemaNodeEntity, sortedAttrs, tr),
				sortOrder: sortedAttrs.join(',')
			};
			return data;
		},
		rowChanged: (tr, entity, initialViewConfig) => {

			let viewInfoInUI = _Schema.views.getInfoFromRow(tr, entity);
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

			_Schema.markElementAsChanged(tr, hasChanges);

			_Schema.bulkDialogsGeneral.tableChanged(tr.closest('table'));
		},
		validateViewRow: (row) => {

			let nameField = row.querySelector('.property-name');
			if (nameField.value.length === 0) {

				_Helpers.blinkRed(nameField.closest('td'));
				return false;
			}

			let viewPropertiesSelect = row.querySelector('.view.property-attrs');
			if ($(viewPropertiesSelect).sortedValues().length === 0) {

				_Helpers.blinkRed(viewPropertiesSelect.closest('td'));
				return false;
			}

			return true;
		},
	},
	methods: {
		methodsData: {},
		lastEditedMethod: {},
		getLastEditedMethod: (entity) => _Schema.methods.lastEditedMethod[(entity ? entity.id : 'global')],
		setLastEditedMethod: (entity, method) => {
			_Schema.methods.lastEditedMethod[(entity ? entity.id : 'global')] = method;
		},
		methodsTableConfig: {
			class: 'actions schema-props',
			cols: [
				{ class: '', title: 'Name' },
				{ class: 'isstatic-col', title: 'isStatic' },
				{ class: 'actions-col', title: 'Action' }
			]
		},
		appendMethods: (container, entity, methods, optionalAfterSaveCallback) => {

			_Schema.methods.methodsData = {};

			methods = _Schema.filterJavaMethods(methods, entity);

			container.insertAdjacentHTML('beforeend', _Schema.templates.methods({ class: (entity ? 'entity' : 'global') }));

			let methodsFakeTable = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.fakeTable(_Schema.methods.methodsTableConfig));
			container.querySelector('#methods-table-container').appendChild(methodsFakeTable);

			let fakeTbody = container.querySelector('.fake-tbody');

			let fakeTfootButtonsContainer = methodsFakeTable.querySelector('.fake-tfoot-buttons');
			fakeTfootButtonsContainer.insertAdjacentHTML('beforeend', (entity) ? _Schema.templates.addMethodsDropdown() : _Schema.templates.addMethodDropdown());

			_Schema.methods.activateUIActions(container, fakeTbody, entity);

			_Helpers.sort(methods);

			let lastEditedMethod = _Schema.methods.getLastEditedMethod(entity);

			let rowToActivate = undefined;

			for (let method of methods) {

				let fakeRow = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.method({ method: method }));
				fakeTbody.appendChild(fakeRow);

				fakeRow.dataset['typeName']   = (entity ? entity.name : 'global_schema_method');
				fakeRow.dataset['methodName'] = method.name;

				fakeRow.querySelector('.property-name').value       = method.name;
				fakeRow.querySelector('.property-isStatic').checked = method.isStatic;

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
					codeType:        method.codeType || ''
				};

				_Schema.methods.bindRowEvents(fakeRow, entity);

				// auto-edit first method (or last used)
				if ((rowToActivate === undefined) || (lastEditedMethod && ((lastEditedMethod.isNew === false && lastEditedMethod.id === method.id) || (lastEditedMethod.isNew === true && lastEditedMethod.name === method.name)))) {
					rowToActivate = fakeRow;
				}
			}

			if (rowToActivate) {
				rowToActivate.querySelector('.edit-action').dispatchEvent(new Event('click'));
			}

			let resetFunction = () => {
				for (let discardIcon of methodsFakeTable.querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
			};
			methodsFakeTable.querySelector('.discard-all').addEventListener('click', resetFunction);

			methodsFakeTable.querySelector('.save-all').addEventListener('click', () => {
				_Schema.methods.bulkSave(container, fakeTbody, entity, optionalAfterSaveCallback);
			});

			let editorInfo = container.querySelector('#methods-container-right .editor-info');
			_Editors.appendEditorOptionsElement(editorInfo);

			_Schema.bulkDialogsGeneral.fakeTableChanged(methodsFakeTable);

			return {
				getBulkInfo: (doValidate) => {
					return _Schema.methods.getDataFromTable(fakeTbody, entity, doValidate);
				},
				reset: resetFunction
			};
		},
		getDataFromTable: (tbody, entity, doValidate = true) => {

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

			for (let tr of tbody.querySelectorAll('.fake-tr')) {

				let methodId   = tr.dataset['methodId'];
				let methodData = _Schema.methods.methodsData[methodId];

				if (methodData.isNew === false) {

					if (tr.classList.contains('to-delete')) {

						counts.delete++;
						data.schemaMethods.push({
							id: methodId,
							deleteMethod: true
						});

					} else if (tr.classList.contains('has-changes')) {

						counts.update++;
						data.schemaMethods.push({
							id:       methodId,
							name:     methodData.name,
							isStatic: methodData.isStatic,
							source:   methodData.source,
						});
						if (doValidate) {
							allow = _Schema.methods.validateMethodRow(tr) && allow;
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
						allow = _Schema.methods.validateMethodRow(tr) && allow;
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
		bulkSave: (container, fakeTbody, entity, optionalAfterSaveCallback) => {

			let { data, allow, counts } = _Schema.methods.getDataFromTable(fakeTbody, entity);

			if (allow) {

				let activeMethod = fakeTbody.querySelector('.fake-tr.editing');
				if (activeMethod) {
					_Schema.methods.setLastEditedMethod(entity, _Schema.methods.methodsData[activeMethod.dataset['methodId']]);
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

								_Helpers.fastRemoveAllChildren(container);

								_Schema.methods.appendMethods(container, reloadedEntity, reloadedEntity.schemaMethods, optionalAfterSaveCallback);
								_Schema.hideSchemaRecompileMessage();

								optionalAfterSaveCallback?.();
							});

						} else {

							Command.rest(`SchemaMethod?schemaNode=null&${Structr.getRequestParameterName('sort')}=name&${Structr.getRequestParameterName('order')}=ascending`, (methods) => {

								_Helpers.fastRemoveAllChildren(container);

								_Schema.methods.appendMethods(container, null, methods, optionalAfterSaveCallback);
								_Schema.hideSchemaRecompileMessage();

								optionalAfterSaveCallback?.();
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
		activateUIActions: (container, fakeTbody, entity) => {

			let addedMethodsCounter = 1;

			for (let addMethodButton of container.querySelectorAll('.add-method-button')) {

				addMethodButton.addEventListener('click', () => {

					let prefix           = addMethodButton.dataset['prefix'] || '';
					let baseMethodConfig = {
						name: _Schema.methods.getFirstFreeMethodName(prefix),
						id: 'new' + (addedMethodsCounter++)
					};

					_Schema.methods.appendNewMethod(fakeTbody, baseMethodConfig, entity);
				});
			}

			_Helpers.activateCommentsInElement(container, { css: {}, noSpan: true, customToggleIconClasses: ['icon-blue', 'ml-2'] });
		},
		appendNewMethod: (fakeTbody, method, entity) => {

			let fakeTr = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.method({ method: method, isNew: true }));
			fakeTbody.appendChild(fakeTr);

			fakeTbody.scrollTop = fakeTr.offsetTop;

			_Schema.methods.methodsData[method.id] = {
				id: method.id,
				isNew: true,
				name: method.name,
				isStatic: method.isStatic || false,
				source: method.source || '',
			};

			let propertyNameInput = fakeTr.querySelector('.property-name');
			propertyNameInput.addEventListener('input', () => {
				_Schema.methods.methodsData[method.id].name = propertyNameInput.value;
			});

			let isStaticCheckbox = fakeTr.querySelector('.property-isStatic');
			isStaticCheckbox.addEventListener('change', () => {
				_Schema.methods.methodsData[method.id].isStatic = isStaticCheckbox.checked;
			});

			fakeTr.querySelector('.edit-action').addEventListener('click', () => {
				_Schema.methods.editMethod(fakeTr, entity);
			});

			fakeTr.querySelector('.clone-action').addEventListener('click', () => {
				_Schema.methods.appendNewMethod(fakeTr.closest('.fake-tbody'), {
					id:       method.id + '_clone_' + (new Date().getTime()),
					name:     _Schema.methods.getFirstFreeMethodName(_Schema.methods.methodsData[method.id].name + '_copy'),
					isStatic: _Schema.methods.methodsData[method.id].isStatic,
					source:   _Schema.methods.methodsData[method.id].source
				}, entity);
			});

			fakeTr.querySelector('.discard-changes').addEventListener('click', () => {

				if (fakeTr.classList.contains('editing')) {
					document.querySelector('#methods-container-right').style.display = 'none';
				}
				_Helpers.fastRemoveElement(fakeTr);

				_Schema.methods.rowChanged(fakeTbody.closest('.fake-table'));

				_Editors.nukeEditorsById(method.id);
			});

			_Schema.bulkDialogsGeneral.fakeTableChanged(fakeTbody.closest('.fake-table'));

			_Schema.methods.editMethod(fakeTr, entity);
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
		bindRowEvents: (tr, entity) => {

			let methodId   = tr.dataset['methodId'];
			let methodData = _Schema.methods.methodsData[methodId];

			let propertyNameInput = tr.querySelector('.property-name');
			propertyNameInput.addEventListener('input', () => {
				methodData.name = propertyNameInput.value;
				_Schema.methods.rowChanged(tr, (methodData.name !== methodData.initialName));
			});

			let isStaticCheckbox = tr.querySelector('.property-isStatic');
			isStaticCheckbox.addEventListener('change', () => {
				methodData.isStatic = isStaticCheckbox.checked;
				_Schema.methods.rowChanged(tr, (methodData.isStatic !== methodData.initialisStatic));
			});

			tr.querySelector('.edit-action').addEventListener('click', () => {
				_Schema.methods.editMethod(tr, entity);
			});

			tr.querySelector('.clone-action').addEventListener('click', () => {
				_Schema.methods.appendNewMethod(tr.closest('.fake-tbody'), {
					id:       methodId + '_clone_' + (new Date().getTime()),
					name:     _Schema.methods.getFirstFreeMethodName(methodData.name + '_copy'),
					isStatic: methodData.isStatic,
					source:   methodData.source
				}, entity);
			});

			tr.querySelector('.remove-action').addEventListener('click', () => {
				tr.classList.add('to-delete');
				_Schema.methods.rowChanged(tr, true);
			});

			tr.querySelector('.discard-changes').addEventListener('click', () => {

				if (tr.classList.contains('to-delete') || tr.classList.contains('has-changes')) {

					tr.classList.remove('to-delete');
					tr.classList.remove('has-changes');

					methodData.name     = methodData.initialName;
					methodData.isStatic = methodData.initialisStatic;
					methodData.source   = methodData.initialSource;

					tr.querySelector('.property-name').value       = methodData.name;
					tr.querySelector('.property-isStatic').checked = methodData.isStatic;

					if (tr.classList.contains('editing')) {
						_Editors.disposeEditorModel(methodData.id, 'source');
						_Schema.methods.editMethod(tr);
					}

					_Schema.methods.rowChanged(tr, false);
				}
			});
		},
		saveAndDisposePreviousEditor: (tr) => {

			let previouslyActiveRow = tr.closest('.fake-tbody').querySelector('.fake-tr.editing');
			if (previouslyActiveRow) {

				let previousMethodId = previouslyActiveRow.dataset['methodId'];

				if (previousMethodId) {
					_Editors.saveViewState(previousMethodId, 'source');
					_Editors.disposeEditor(previousMethodId, 'source');
				}
			}
		},
		editMethod: (tr, entity) => {

			_Schema.methods.saveAndDisposePreviousEditor(tr);

			document.querySelector('#methods-container-right').style.display = '';

			tr.closest('.fake-tbody').querySelector('.fake-tr.editing')?.classList.remove('editing');
			tr.classList.add('editing');

			let methodId   = tr.dataset['methodId'];
			let methodData = _Schema.methods.methodsData[methodId];

			_Schema.methods.setLastEditedMethod(entity, methodData);

			let sourceMonacoConfig = {
				language: 'auto',
				lint: true,
				autocomplete: true,
				changeFn: (editor, entity) => {
					methodData.source = editor.getValue();
					let hasChanges = (methodData.source !== methodData.initialSource) || (methodData.name !== methodData.initialName) || (methodData.isStatic !== methodData.initialisStatic);
					_Schema.methods.rowChanged(tr, hasChanges);
				}
			};

			let sourceEditor = _Editors.getMonacoEditor(methodData, 'source', document.querySelector('#methods-content .editor'), sourceMonacoConfig);
			_Editors.focusEditor(sourceEditor);

			sourceMonacoConfig.changeFn(sourceEditor);

			_Editors.resizeVisibleEditors();
		},
		showGlobalSchemaMethods: () => {

			Command.rest(`SchemaMethod?schemaNode=null&${Structr.getRequestParameterName('sort')}=name&${Structr.getRequestParameterName('order')}=ascending`, (methods) => {

				let { dialogText } = _Dialogs.custom.openDialog('Global Schema Methods', () => {

					_Schema.currentNodeDialogId = null;

					_Schema.ui.jsPlumbInstance.repaintEverything();

				}, ['schema-edit-dialog', 'global-methods-dialog']);

				dialogText.insertAdjacentHTML('beforeend', '<div class="schema-details"><div id="tabView-methods" class="schema-details"></div></div>');

				_Schema.methods.appendMethods(dialogText.querySelector('#tabView-methods'), null, methods);
			});
		},
		rowChanged: (tr, hasChanges) => {

			_Schema.markElementAsChanged(tr, hasChanges);

			_Schema.bulkDialogsGeneral.fakeTableChanged(tr.closest('.fake-table'));
		},
		validateMethodRow: (tr) => {

			let propertyNameInput = tr.querySelector('.property-name');
			if (propertyNameInput.value.length === 0) {

				_Helpers.blinkRed(propertyNameInput.closest('.fake-td'));
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

				_Helpers.fastRemoveAllChildren(sourceContainer);

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
	resize: () => {

		if (_Schema.ui.canvas) {

			let zoom           = (_Schema.ui.jsPlumbInstance ? _Schema.ui.jsPlumbInstance.getZoom() : 1);
			let canvasPosition = _Schema.ui.canvas.offset();
			let padding        = 100;
			let windowHeight   = window.innerHeight;
			let windowWidth    = window.innerWidth;

			let maxElementPosition = {
				right:  0,
				bottom: 0
			};

			// do not include ._jsPlumb_connector because that has huge containers for bezier
			for (let elem of _Schema.ui.canvas[0].querySelectorAll('.node, .label, ._jsPlumb_endpoint_anchor')) {
				let rect = elem.getBoundingClientRect();
				maxElementPosition.right  = Math.max(maxElementPosition.right,  Math.ceil((rect.right  + window.scrollX - canvasPosition.left) / zoom));
				maxElementPosition.bottom = Math.max(maxElementPosition.bottom, Math.ceil((rect.bottom + window.scrollY - canvasPosition.top)  / zoom));
			}

			let canvasSize = {
				width:  (windowWidth  - canvasPosition.left) / zoom,
				height: (windowHeight - canvasPosition.top)  / zoom
			};

			if (maxElementPosition.right >= canvasSize.width) {
				canvasSize.width = maxElementPosition.right + padding;
			}

			if (maxElementPosition.bottom >= canvasSize.height) {
				canvasSize.height = maxElementPosition.bottom + padding;
			}

			_Schema.ui.canvas.css({
				width:  canvasSize.width + 'px',
				height: canvasSize.height + 'px'
			});
		}
	},
	dialogSizeChanged: () => {
		_Editors.resizeVisibleEditors();
	},
	deleteNode: async (id) => {

		_Schema.showSchemaRecompileMessage();

		let response = await fetch(`${Structr.rootUrl}schema_nodes/${id}`, {
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
	activateSnapshotsDialog: () => {

		let snapshotsContainer = document.querySelector('#snapshots');

		let refresh = () => {

			_Helpers.fastRemoveAllChildren(snapshotsContainer);

			Command.snapshots('list', '', null, (result) => {

				for (let data of result) {

					for (let snapshotName of data.snapshots) {

						let tr = _Helpers.createSingleDOMElementFromHTML(`
							<div class="flex items-center justify-between p-2">
								<div class="snapshot-link name"><a href="#">${snapshotName}</a></div>
								<div>
									<button class="restore-snapshot hover:bg-gray-100 focus:border-gray-666 active:border-green">Restore</button>
									<button class="add-snapshot     hover:bg-gray-100 focus:border-gray-666 active:border-green">Add</button>
									<button class="delete-snapshot  hover:bg-gray-100 focus:border-gray-666 active:border-green">Delete</button>
								</div>
							</div>
						`);

						snapshotsContainer.appendChild(tr);

						tr.querySelector(`.snapshot-link.name a`).addEventListener('click', (e) => {

							e.preventDefault();

							Command.snapshots("get", snapshotName, null, (snapshotData) => {

								let element = document.createElement('a');
								element.setAttribute('href', `data:text/plain;charset=utf-8,${encodeURIComponent(snapshotData.schemaJson)}`);
								element.setAttribute('download', snapshotName);

								element.style.display = 'none';
								document.body.appendChild(element);

								element.click();
								document.body.removeChild(element);
							});
						});

						tr.querySelector('.restore-snapshot').addEventListener('click', () => {
							_Schema.performSnapshotAction('restore', snapshotName);
						});
						tr.querySelector('.add-snapshot').addEventListener('click', () => {
							_Schema.performSnapshotAction('add', snapshotName);
						});
						tr.querySelector('.delete-snapshot').addEventListener('click', () => {
							Command.snapshots('delete', snapshotName, null, refresh);
						});
					}
				}
			});
		};

		document.querySelector('#create-snapshot').addEventListener('click', () => {

			let suffix = document.querySelector('#snapshot-suffix').value;
			let types  = [];

			if (_Schema.ui.selectedNodes && _Schema.ui.selectedNodes.length) {

				for (let selectedNode of _Schema.ui.selectedNodes) {
					types.push(selectedNode.name);
				}

				for (let el of _Schema.ui.canvas[0].querySelectorAll('.label.rel-type')) {

					let sourceType = el.children[0].dataset['sourceType'];
					let targetType = el.children[0].dataset['targetType'];

					// include schema relationship if both source and target type are selected
					if (types.indexOf(sourceType) !== -1 && types.indexOf(targetType) !== -1) {
						types.push(el.children[0].dataset['name']);
					}
				}
			}

			Command.snapshots('export', suffix, types, (data) => {

				let status = data[0].status;
				if (status !== 'success') {
					new ErrorMessage().text('Snapshot creation failed').show();
				}

				refresh();
			});
		});

		document.querySelector('#refresh-snapshots').addEventListener('click', refresh);
		refresh();
	},
	performSnapshotAction: (action, snapshot) => {

		Command.snapshots(action, snapshot, null, (data) => {

			let status = data[0].status;

			if (status === 'success') {
				_Schema.reload();
			} else {
				if (_Dialogs.custom.isDialogOpen()) {
					_Dialogs.custom.showAndHideInfoBoxMessage(status, 'error', 2000, 200);
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

					_Helpers.updateButtonWithSpinnerAndText(btn[0], oldHtml);

					let response = await fetch(`${Structr.rootUrl}maintenance/${target}`, {
						method: 'POST',
						body: JSON.stringify(payload)
					});

					if (response.ok) {
						_Helpers.updateButtonWithSuccessIcon(btn[0], oldHtml);
					}
				};

				if (!connectedSelectElement) {

					await transportAction(target, {});

				} else {

					let type = connectedSelectElement.val();
					if (!type) {
						_Helpers.blinkRed(connectedSelectElement);
					} else {
						await transportAction(target, ((typeof getPayloadFunction === "function") ? getPayloadFunction(type) : {}));
					}
				}
			});
		};

		registerSchemaToolButtonAction($('#rebuild-index'), 'rebuildIndex');
		registerSchemaToolButtonAction($('#flush-caches'), 'flushCaches');

		$('#clear-schema').on('click', async (e) => {

			let confirm = await _Dialogs.confirmation.showPromise('<h3>Delete schema?</h3><p>This will remove all dynamic schema information, but not your other data.</p><p>&nbsp;</p>');

			if (confirm === true) {

				_Schema.showSchemaRecompileMessage();
				Command.snapshots("purge", undefined, undefined, () => {
					_Schema.reload();
					_Schema.hideSchemaRecompileMessage();
				});
			}
		});

		let nodeTypeSelector = $('#node-type-selector');
		Command.list('SchemaNode', true, 1000, 1, 'name', 'asc', 'id,name', (nodes) => {
			nodeTypeSelector.append(nodes.map(node => `<option>${node.name}</option>`).join(''));
		});

		registerSchemaToolButtonAction($('#reindex-nodes'), 'rebuildIndex', nodeTypeSelector, (type) => {
			return (type === 'allNodes') ? { mode: 'nodesOnly' } : { mode: 'nodesOnly', type: type };
		});

		registerSchemaToolButtonAction($('#add-node-uuids'), 'setUuid', nodeTypeSelector, (type) => {
			return (type === 'allNodes') ? { allNodes: true } : { type: type };
		});

		registerSchemaToolButtonAction($('#create-labels'), 'createLabels', nodeTypeSelector, (type) => {
			return (type === 'allNodes') ? {} : { type: type };
		});

		let relTypeSelector = $('#rel-type-selector');
		Command.list('SchemaRelationshipNode', true, 1000, 1, 'relationshipType', 'asc', 'id,relationshipType', (rels) => {
			relTypeSelector.append(rels.map(rel => `<option>${rel.relationshipType}</option>`).join(''));
		});

		registerSchemaToolButtonAction($('#reindex-rels'), 'rebuildIndex', relTypeSelector, (type) => {
			return (type === 'allRels') ? { mode: 'relsOnly' } : { mode: 'relsOnly', type: type };
		});

		registerSchemaToolButtonAction($('#add-rel-uuids'), 'setUuid', relTypeSelector, (type) => {
			return (type === 'allRels') ? { allRels: true } : { relType: type };
		});
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

					_Helpers.disableElements(true, updateLayoutButton[0], restoreLayoutButton[0], deleteLayoutButton[0]);

				} else {

					_Helpers.enableElement(restoreLayoutButton[0]);

					let optGroup    = selectedOption.closest('optgroup');
					let username    = optGroup.prop('label');
					let isOwnerless = optGroup.data('ownerless') === true;

					_Helpers.disableElements(!(isOwnerless || username === StructrWS.me.username), updateLayoutButton[0], deleteLayoutButton[0]);
				}
			};
			layoutSelectorChangeHandler();

			layoutSelector[0].addEventListener('change', layoutSelectorChangeHandler);

			updateLayoutButton.click(() => {

				let selectedLayout = layoutSelector.val();

				Command.setProperty(selectedLayout, 'content', JSON.stringify(_Schema.getSchemaLayoutConfiguration()), false, (data) => {

					if (!data.error) {

						new SuccessMessage().text("Layout saved").show();

						_Helpers.blinkGreen(layoutSelector);

					} else {

						new ErrorMessage().title(data.error).text(data.message).show();
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
					_Helpers.blinkGreen(layoutSelector);
				});
			});

			createNewLayoutButton.on('click', () => {

				let layoutName = layoutNameInput.val();

				if (layoutName && layoutName.length) {

					Command.createApplicationConfigurationDataNode('layout', layoutName, JSON.stringify(_Schema.getSchemaLayoutConfiguration()), async (data) => {

						if (!data.error) {

							new SuccessMessage().text("Layout saved").show();

							await _Schema.updateGroupedLayoutSelector(layoutSelector);
							layoutSelectorChangeHandler();
							layoutNameInput.val('');

							_Helpers.blinkGreen(layoutSelector);

						} else {

							new ErrorMessage().title(data.error).text(data.message).show();
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
				_Schema.applySavedLayoutConfiguration(data);
			});
		}
	},
	applySavedLayoutConfiguration: (data, initialRestore = false) => {

		try {

			let loadedConfig = JSON.parse(data.content);

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
							$(`#schema-options-table input.toggle-type[data-structr-type="${hiddenType}"]`).prop('checked', false);
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

				new InfoMessage().text("This layout was created using an older version of Structr. To make use of newer features you should delete and re-create it with the current version.").show();
			}

			LSWrapper.save();

			if (initialRestore === true) {
				new SuccessMessage().text(`No saved schema layout detected, loaded "${data.name}"`).show();
			} else {
				_Schema.reload();
			}

		} catch (e) {

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

		let { dialogText } = _Dialogs.custom.openDialog('Schema Type Visibility', null, ['full-height-dialog-text']);

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
		dialogText.insertAdjacentHTML('beforeend', `
			<div id="${id}_content" class="code-tabs flex flex-col h-full overflow-hidden">
				<ul id="${id}-tabs" class="flex-shrink-0"></ul>
			</div>
		`);

		let ul        = dialogText.querySelector(`#${id}-tabs`);
		let contentEl = dialogText.querySelector(`#${id}_content`);

		let activateTab = (tabName) => {
			[...contentEl.querySelectorAll('.tab')].forEach(tab => tab.style.display = 'none');
			[...ul.querySelectorAll('li')].forEach(li => li.classList.remove('active'));
			contentEl.querySelector(`div[data-name="${tabName}"]`).style.display = 'block';
			ul.querySelector(`li[data-name="${tabName}"]`).classList.add('active');
			LSWrapper.setItem(_Schema.activeSchemaToolsSelectedVisibilityTab, tabName);
		};

		Command.query('SchemaNode', 2000, 1, 'name', 'asc', {}, (schemaNodes) => {

			let tabsHtml = visibilityTables.map(visType => `<li id="tab" data-name="${visType.caption}">${visType.caption}</li>`).join('');
			ul.insertAdjacentHTML('beforeend', tabsHtml);

			let tabContentsHtml = visibilityTables.map(visType => `
				<div class="tab overflow-y-auto" data-name="${visType.caption}">
					<table class="props schema-visibility-table">
						<tr>
							<th class="toggle-column-header">
								<div class="flex items-center gap-3">
									<span>Visible</span>
									<div class="flex items-center gap-1">
										${_Icons.getSvgIcon(_Icons.iconInvertSelection, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-green', 'invert-all-types', 'invert-icon']), 'Invert all')}
										<input type="checkbox" title="Toggle all" class="toggle-all-types">
									</div>
								</div>
							</th>
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

			for (let invertAllCb of contentEl.querySelectorAll('.invert-all-types')) {

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
							${(idForClassname ? _Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, _Icons.getSvgIconClassesNonColorIcon(['mr-1', 'node-action-icon', 'edit-type-icon'])) : '')}
							${(idForClassname && isCustomType ? _Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'mr-1', 'node-action-icon', 'delete-type-icon'])) : '')}
						</div>
					`;

					let $newLi               = $(`<li data-id="${classnameToId[classname]}">${classname}${icons}</li>`).appendTo($newUl);
					let requiredSubTypeCount = printClassTree($newLi, classTree[classname]);
					let iconId               = (Object.keys(classTree[classname]).length > 0) ? _Icons.iconFolderOpen : _Icons.iconFolderClosed;

					let data = {
						opened: true,
						hidden: (showBuiltinTypes === false && isCustomType === false && requiredSubTypeCount === 0),
						icon: _Icons.nonExistentEmptyIcon,
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

					delIcon.addEventListener('click', async (e) => {

						// otherwise the node on the canvas is focused and ESC does not work
						e.stopPropagation();

						let nodeId = delIcon.closest('li').dataset['id'];
						if (nodeId) {

							let confirm = await _Dialogs.confirmation.showPromise(`
								<h3>Delete schema node '${delIcon.closest('a').textContent.trim()}'?</h3>
								<p>This will delete all incoming and outgoing schema relationships as well,<br> but no data will be removed.</p>
							`);

							if (confirm === true) {

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

				searchTimeout = window.setTimeout(() => {
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
	invalidateTypeInfoCache: (type) => {

		delete _Schema.typeInfoCache[type];

		// clear type cache for this type and derived types
		_Schema.getDerivedTypes(type, [], false).then(derivedTypes => {

			for (let derivedType of derivedTypes) {
				delete _Schema.typeInfoCache[derivedType];
			}
		});
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
	getDerivedTypes: async (baseType, blacklist = [], baseTypeIsFQCN = true) => {

		let response = await fetch(`${Structr.rootUrl}_schema`);

		if (response.ok) {

			let data      = await response.json();
			let result    = data.result;
			let fileTypes = [];
			let maxDepth  = 5;
			let types     = {};

			if (baseTypeIsFQCN === false) {

				// first get FQCN for type name (if exists)
				let exactTypeNameMatches = result.filter(typeInfo => typeInfo.name === baseType);
				baseType = exactTypeNameMatches[0]?.className ?? baseType;
			}

			let collect = (list, type) => {

				for (let n of list) {

					if (n.extendsClass === type) {

						fileTypes.push(`org.structr.dynamic.${n.name}`);

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
				_Schema.ui.selectedRel.nextAll('._jsPlumb_overlay').slice(0, 3).css({ border: '', borderRadius: '', background: 'rgba(255, 255, 255, .8)' });

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

				_Helpers.requestAnimationFrameWrapper(_Schema.ui.prevAnimFrameReqId_selectionDrag, () => {

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
			Structr.resize();
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
	shouldShowJavaMethodsForBuiltInTypes: () => UISettings.getValueForSetting(UISettings.settingGroups.schema_code.settings.showJavaMethodsForBuiltInTypes),
	filterJavaMethods: (methods, entity) => {

		// java methods should always be shown for custom types and for global schema methods
		// otherwise (for built-in types) they should only be shown if the setting is active

		let isGlobalSchemaMethods = !entity;
		let isCustomType          = !(entity?.isBuiltinType ?? true);

		if (isGlobalSchemaMethods || isCustomType || _Schema.shouldShowJavaMethodsForBuiltInTypes()) {
			return methods;
		}

		return methods.filter(m => m.codeType !== 'java');
	},
	getOnlyJavaMethodsIfFilteringIsActive: (methods) => {

		// only relevant when bulk saving methods to not lose java methods (if they are not shown)

		if (_Schema.shouldShowJavaMethodsForBuiltInTypes() === true) {

			return [];

		} else {

			return methods.filter(m => m.codeType === 'java');
		}
	},
	markElementAsChanged: (element, hasClass) => {

		if (hasClass === true) {

			element.classList.add('has-changes');

		} else {

			element.classList.remove('has-changes');
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
					<label class="mr-4 flex" data-comment="Built-in types will still be shown if they are ancestors of custom types.">
						<input type="checkbox" id="show-builtin-types" ${(LSWrapper.getItem(_Schema.showBuiltinTypesInInheritanceTreeKey, false) ? 'checked' : '')}>
						<span class="whitespace-nowrap">Show built-in types</span>
					</label>
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

					<button id="create-type" class="action inline-flex items-center">
						${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['mr-2'])} New Type
					</button>

					<div class="dropdown-menu dropdown-menu-large">
						<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green" id="global-schema-methods">
							${_Icons.getSvgIcon(_Icons.iconGlobe, 16, 16, '')} Global Methods
						</button>
					</div>

					<div class="dropdown-menu dropdown-menu-large">
						<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green">
							${_Icons.getSvgIcon(_Icons.iconNetwork, 16, 16, '')} Display
						</button>

						<div class="dropdown-menu-container">
							<div class="row">
								<a title="Open dialog to show/hide the data types" id="schema-tools" class="flex items-center">
									${_Icons.getSvgIcon(_Icons.iconTypeVisibility, 16, 16, 'mr-2')} Type Visibility
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
									${_Icons.getSvgIcon(_Icons.iconResetArrow, 16, 16, 'mr-2')} Reset Layout (apply Auto-Layouting)
								</a>
							</div>
						</div>
					</div>

					<div class="dropdown-menu dropdown-menu-large">
						<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green">${_Icons.getSvgIcon(_Icons.iconSnapshots, 16, 16, '')} Snapshots</button>

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

							<div class="props" id="snapshots"></div>

							<div class="separator"></div>

							<div class="row">
								<a id="refresh-snapshots" class="block">Reload stored snapshots</a>
							</div>
						</div>
					</div>

					<div class="dropdown-menu dropdown-menu-large">
						<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green">
							${_Icons.getSvgIcon(_Icons.iconSettingsCog, 16, 16, '')} Admin
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
									${_Icons.getSvgIcon(_Icons.iconRefreshArrows, 16, 16, 'mr-2')} Rebuild all indexes
								</button>
								<label for="rebuild-index">Rebuild indexes for entire database (all node and relationship indexes)</label>
							</div>
							<div class="separator"></div>
							<div class="heading-row">
								<h3>Maintenance</h3>
							</div>
							<div class="row flex items-center">
								<button id="flush-caches" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
									${_Icons.getSvgIcon(_Icons.iconRefreshArrows, 16, 16, 'mr-2')} Flush Caches
								</button>
								<label for="flush-caches">Flushes internal caches to refresh schema information</label>
							</div>

							<div class="row flex items-center">
								<button id="clear-schema" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
									${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, 'mr-2 icon-red')} Clear Schema
								</button>
								<label for="clear-schema">Delete all schema nodes and relationships in custom schema</label>
							</div>
						</div>
					</div>
				</div>
			</div>

			<div id="zoom-slider" class="mr-8"></div>
		`,
		typeBasicTab: config => `
			<div class="schema-details pl-2">
				<div class="flex items-center gap-x-2 pt-4">

					<input data-property="name" class="flex-grow" placeholder="Type Name...">

					<div class="extends-type flex items-center gap-2">
						extends
						<select class="extends-class-select" data-property="extendsClass"></select>
						${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, _Icons.getSvgIconClassesNonColorIcon(['edit-parent-type']), 'Edit parent type')}
					</div>
				</div>

				<h3>Options</h3>
				<div class="property-options-group">
					<div>
						<label data-comment="Only takes effect if the changelog is active">
							<input id="changelog-checkbox" type="checkbox" data-property="changelogDisabled"> Disable changelog
						</label>
						<label class="ml-8" data-comment="Makes all nodes of this type visible to public users if checked">
							<input id="public-checkbox" type="checkbox" data-property="defaultVisibleToPublic"> Visible for public users
						</label>
						<label class="ml-8" data-comment="Makes all nodes of this type visible to authenticated users if checked">
							<input id="authenticated-checkbox" type="checkbox" data-property="defaultVisibleToAuth"> Visible for authenticated users
						</label>
					</div>
				</div>

				<h3>OpenAPI</h3>
				<div class="property-options-group">
					<div id="type-openapi">
						${_Code.templates.openAPIBaseConfig({ type: 'SchemaNode' })}
					</div>
				</div>
			</div>
		`,
		relationshipBasicTab: config => `
			<div class="schema-details">
				<div id="relationship-options">

					<div id="basic-options" class="grid grid-cols-5 gap-y-2 items-baseline mb-4">

						<div class="text-right pb-2 truncate">
							<span id="source-type-name" class="edit-schema-object font-medium cursor-pointer"></span>
						</div>

						<div class="flex items-center justify-around">
							<div class="overflow-hidden whitespace-nowrap">&#8212;</div>

							<select id="source-multiplicity-selector" data-attr-name="sourceMultiplicity" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
								<option value="1">1</option>
								<option value="*" selected>*</option>
							</select>

							<div class="overflow-hidden whitespace-nowrap">&#8212;[</div>
						</div>

						<input id="relationship-type-name" data-attr-name="relationshipType" autocomplete="off" placeholder="Relationship Name...">

						<div class="flex items-center justify-around">
							<div class="overflow-hidden whitespace-nowrap">]&#8212;</div>

							<select id="target-multiplicity-selector" data-attr-name="targetMultiplicity" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
								<option value="1">1</option>
								<option value="*" selected>*</option>
							</select>

							<div class="overflow-hidden whitespace-nowrap">&#8212;&#9658;</div>
						</div>

						<div class="text-left pb-2 truncate">
							<span id="target-type-name" class="edit-schema-object font-medium cursor-pointer"></span>
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
		saveActionButton: config => `
			<button id="save-entity-button" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
				${_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 14, 14, ['icon-green', 'mr-2'])} ${(config?.text ?? 'Save')}
			</button>
		`,
		discardActionButton: config => `
			<button id="discard-entity-changes-button" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
				${_Icons.getSvgIcon(_Icons.iconCrossIcon, 14, 14, ['icon-red', 'mr-2'])} ${(config?.text ?? 'Discard')}
			</button>
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
					${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, _Icons.getSvgIconClassesNonColorIcon(['edit-action']))}
					${_Icons.getSvgIcon(_Icons.iconClone, 16, 16, _Icons.getSvgIconClassesNonColorIcon(['clone-action']))}
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16,  _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']))}
					${config.isNew ? '' : _Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16,    _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-action']))}
				</div>
			</div>
		`,
		propertyBuiltin: config => `
			<tr data-property-name="${config.property.name}" data-property-id="${config.property.id}">
				<td>${_Helpers.escapeForHtmlAttributes(config.property.declaringClass)}</td>
				<td>${_Helpers.escapeForHtmlAttributes(config.property.name)}</td>
				<td>${config.property.propertyType}</td>
				<td class="centered"><input class="not-null" type="checkbox" disabled="disabled" ${(config.property.notNull ? 'checked' : '')}></td>
				<td class="centered"><input class="compound" type="checkbox" disabled="disabled" ${(config.property.compound ? 'checked' : '')}></td>
				<td class="centered"><input class="unique" type="checkbox" disabled="disabled" ${(config.property.unique ? 'checked' : '')}></td>
				<td class="centered"><input class="indexed" type="checkbox" disabled="disabled" ${(config.property.indexed ? 'checked' : '')}></td>
			</tr>
		`,
		propertyLocal: config => `
			<tr data-property-id="${config.property.id}" >
				<td><input size="15" type="text" class="property-name" value="${_Helpers.escapeForHtmlAttributes(config.property.name)}"></td>
				<td class="${config.dbNameClass}"><input size="15" type="text" class="property-dbname" value="${_Helpers.escapeForHtmlAttributes(config.property.dbName)}"></td>
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
									return `<input size="15" type="text" class="property-format" value="${(config.property.format ? _Helpers.escapeForHtmlAttributes(config.property.format) : '')}">`;
							};
						})()
					}
				</td>
				<td class="centered"><input class="not-null" type="checkbox" ${(config.property.notNull ? 'checked' : '')}></td>
				<td class="centered"><input class="compound" type="checkbox" ${(config.property.compound ? 'checked' : '')}></td>
				<td class="centered"><input class="unique" type="checkbox" ${(config.property.unique ? 'checked' : '')}></td>
				<td class="centered"><input class="indexed" type="checkbox" ${(config.property.indexed ? 'checked' : '')}></td>
				<td><input type="text" size="10" class="property-default" value="${_Helpers.escapeForHtmlAttributes(config.property.defaultValue)}"></td>
				<td class="centered actions-col">
					${config.property.isBuiltinProperty ? '' : _Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']))}
					${config.property.isBuiltinProperty ? '' : _Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16,   _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-action']))}
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
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16,  _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']), 'Remove')}
				</td>
			</tr>
		`,
		remoteProperty: config => `
			<tr data-relationship-id="${config.rel.id}" data-property-name="${config.propertyName}" data-target-collection="${config.targetCollection}">
				<td><input size="15" type="text" class="property-name related" value="${config.attributeName}" /></td>
				<td>
					${config.arrowLeft}&mdash;<i class="cardinality ${config.cardinalityClassLeft}"></i>&mdash;[:<span class="edit-schema-object font-medium cursor-pointer" data-object-id="${config.rel.id}">${config.relType}</span>]&mdash;<i class="cardinality ${config.cardinalityClassRight}"></i>&mdash;${config.arrowRight}
					<span class="edit-schema-object font-medium cursor-pointer" data-object-id="${config.relatedNodeId}">${config.relatedNodeType}</span>
				</td>
				<td class="centered">
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16,  _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']))}
					${_Icons.getSvgIcon(_Icons.iconResetArrow, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-green', 'reset-action']))}
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
						<div class="fake-td actions-col flex">
							<div class="flex-grow"></div>
							<button class="discard-all inline-flex items-center disabled hover:bg-gray-100 focus:border-gray-666 active:border-green" disabled>
								${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16, 'icon-red mr-2')} Discard all
							</button>
							<button class="save-all inline-flex items-center disabled hover:bg-gray-100 focus:border-gray-666 active:border-green" disabled>
								${_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, 'icon-green mr-2')} Save all
							</button>
						</div>
					</div>
				</div>
				<div class="fake-tfoot-buttons"></div>
			</div>
		`,
		addMethodsDropdown: config => `
			<div class="dropdown-menu darker-shadow-dropdown dropdown-menu-large">
				<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green" data-wants-fixed="true">
					${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'icon-green mr-2')}
				</button>
				<div class="dropdown-menu-container">
					<div class="flex flex-col divide-x-0 divide-y">
						<a data-prefix="" class="add-method-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer p-4">
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'icon-green mr-2')} Add method
						</a>
						<a data-prefix="onCreate" class="add-method-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer p-4 border-0 border-t border-gray-ddd border-solid">
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'icon-green mr-2')} Add onCreate
						</a>
						<a data-prefix="afterCreate" class="add-method-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer p-4 border-0 border-t border-gray-ddd border-solid" data-comment="The difference between <strong>onCreate</strong> and <strong>afterCreate</strong> is that <strong>afterCreate</strong> is called after all checks have run and the transaction is committed.<br><br>Example: There is a unique constraint and you want to send an email when an object is created.<br>Calling 'send_html_mail()' in onCreate would send the email even if the transaction would be rolled back due to an error. The appropriate place for this would be afterCreate.">
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'icon-green mr-2')} Add afterCreate
						</a>
						<a data-prefix="onSave" class="add-method-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer p-4 border-0 border-t border-gray-ddd border-solid">
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'icon-green mr-2')} Add onSave
						</a>
						<a data-prefix="afterSave" class="add-method-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer p-4 border-0 border-t border-gray-ddd border-solid" data-comment="The difference between <strong>onSave</strong> and <strong>afterSave</strong> is that <strong>afterSave</strong> is called after all checks have run and the transaction is committed.<br><br>Example: There is a unique constraint and you want to send an email when an object is saved successfully.<br>Calling 'send_html_mail()' in onSave would send the email even if the transaction would be rolled back due to an error. The appropriate place for this would be afterSave.">
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'icon-green mr-2')} Add afterSave
						</a>
					</div>
				</div>
			</div>
		`,
		addMethodDropdown: config => `
			<button prefix="" class="inline-flex items-center add-method-button hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer">
				${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'icon-green mr-2')} Add method
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
						${(config.addButtonText ? '<button class="add-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">' + _Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'icon-green mr-2') + config.addButtonText + '</button>' : '')}
						<button class="discard-all inline-flex items-center disabled hover:bg-gray-100 focus:border-gray-666 active:border-green" disabled>
							${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16, 'icon-red mr-2')} ${(config.discardButtonText ? config.discardButtonText : 'Discard all')}
						</button>
						<button class="save-all inline-flex items-center disabled hover:bg-gray-100 focus:border-gray-666 active:border-green" disabled>
							${_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, 'icon-green mr-2')} ${(config.discardButtonText ? config.saveButtonText : 'Save all')}
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
					<input size="15" type="text" class="view property-name" placeholder="Enter view name" value="${(config.view ? _Helpers.escapeForHtmlAttributes(config.view.name) : '')}">
				</td>
				<td class="view-properties-select">
					<select class="property-attrs view" multiple="multiple" ${config?.propertiesDisabled === true ? 'disabled' : ''}></select>
				</td>
				<td class="centered actions-col">
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']))}
					${(_Schema.views.isDeleteViewAllowed(config.view) === true) ? _Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16,   _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-action'])) : ''}

					<a href="${Structr.rootUrl}${config.type.name}/${config.view.name}?${Structr.getRequestParameterName('pageSize')}=1" target="_blank">
						${_Icons.getSvgIcon(_Icons.iconOpenInNewPage, 16, 16, _Icons.getSvgIconClassesNonColorIcon(), 'Preview in new tab (with pageSize=1)')}
					</a>
				</td>
			</tr>
		`,
		viewNew: config => `
			<tr class="has-changes">
				<td style="width:20%;">
					<input size="15" type="text" class="view property-name" placeholder="Enter view name">
				</td>
				<td class="view-properties-select">
					<select class="property-attrs view" multiple="multiple"></select>
				</td>
				<td class="centered actions">
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16,  _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']), 'Discard changes')}
				</td>
			</tr>
		`,
		schemaGrantsTabContent: config => `
			<div>
				<div class="inline-info">
					<div class="inline-info-icon">
						${_Icons.getSvgIcon(_Icons.iconInfo, 24, 24)}
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
						${_Icons.getSvgIcon(_Icons.iconInfo, 24, 24)}
					</div>
					<div class="inline-info-text">
						Working Sets are identical to layouts. Removing an element from a group removes it from the layout
					</div>
				</div>

				<div style="width: calc(100% - 4rem);" class="pt-4 mb-4">
					<select id="type-groups" multiple="multiple"></select>
					<span id="add-to-new-group"></span>
				</div>

				${(config.addButtonText ? '<button class="add-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">' + _Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'icon-green mr-2') + config.addButtonText + '</button>' : '')}

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