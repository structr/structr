/*
 * Copyright (C) 2010-2025 Structr GmbH
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
	showSchemaInheritanceArrowsKey: 'structrShowSchemaInheritance_' + location.port,
	schemaMethodsHeightsKey: 'structrSchemaMethodsHeights_' + location.port,
	schemaActiveTabLeftKey: 'structrSchemaActiveTabLeft_' + location.port,
	activeSchemaToolsSelectedTabLevel1Key: 'structrSchemaToolsSelectedTabLevel1_' + location.port,
	schemaZoomLevelKey: '_schema_' + location.port + 'zoomLevel',
	schemaConnectorStyleKey: '_schema_' + location.port + 'connectorStyle',
	schemaNodePositionKeySuffix: '_schema_' + location.port + 'node-position',
	currentNodeDialogId: null,
	onload: () => {

		_Code.helpers.preloadAvailableTagsForEntities().then(() => {

			Structr.setMainContainerHTML(_Schema.templates.main());
			_Helpers.activateCommentsInElement(Structr.mainContainer);

			_Schema.ui.canvas = $('#schema-graph');

			_Schema.ui.nodeDrag.init();
			_Schema.ui.selection.init();

			_Schema.init(() => {
				Structr.resize();
				_Schema.ui.initPanZoom();
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
		_Schema.init(callback);
		Structr.resize();
	},
	storePositions: () => {

		let min = {
			left: null,
			top: null
		};

		let rawPositions = [..._Schema.ui.canvas[0].querySelectorAll('.node')].map(n => {

			let type = n.dataset.type;
			let left = parseFloat(n.style.left);
			let top  = parseFloat(n.style.top)

			min.left = Math.min((min.left ?? left), left);
			min.top  = Math.min((min.top ?? top), top);

			return [type, { top, left }];
		});

		// because of panzoom, we can move nodes anywhere... find the minimum top/left and subtract it everywhere
		_Schema.nodePositions = Object.fromEntries(rawPositions.map(([k, v]) => [k, { top: v.top - min.top, left: v.left - min.left}]));

		LSWrapper.setItem(_Schema.schemaPositionsKey, _Schema.nodePositions);
	},
	storePositionsNewLayout: () => {
		for (let n of document.querySelectorAll('rect.node')) {
			let type = n.dataset.type;
			let obj = {
				left: n.x.baseVal.value,
				top: n.y.baseVal.value
			};
			//obj.left = (obj.left) / _Schema.ui.zoomLevel;
			//obj.top  = (obj.top)  / _Schema.ui.zoomLevel;
			_Schema.nodePositions[type] = obj;
		}
		LSWrapper.setItem(_Schema.schemaPositionsKey, _Schema.nodePositions);
	},
	clearPositions: () => {
		LSWrapper.removeItem(_Schema.schemaPositionsKey);
		_Schema.reload();
	},
	init: (callback) => {

		_Schema.schemaLoading            = false;
		_Schema.ui.connectorStyle        = LSWrapper.getItem(_Schema.schemaConnectorStyleKey) || 'Flowchart';
		_Schema.ui.zoomLevel             = parseFloat(LSWrapper.getItem(_Schema.schemaZoomLevelKey)) || 1.0;
		_Schema.ui.showInheritanceArrows = LSWrapper.getItem(_Schema.showSchemaInheritanceArrowsKey, true) ?? true;

		Structr.setFunctionBarHTML(_Schema.templates.functions());

		UISettings.showSettingsForCurrentModule(UISettings.settingGroups.schema_code);

		_Schema.activateDisplayDropdownTools();
		_Schema.activateAdminTools();

		document.getElementById('create-type').addEventListener('click', _Schema.nodes.showCreateTypeDialog);
		document.getElementById('user-defined-functions').addEventListener('click', _Schema.methods.showUserDefinedMethods);

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
			_Schema.ui.jsPlumbInstance.setZoom(_Schema.ui.zoomLevel);

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

			$('._jsPlumb_connector').click(function(e) {
				e.stopPropagation();
				_Schema.ui.selection.selectRel($(this));
			});

			Structr.resize();

			Structr.mainMenu.unblock(500);

			let showSchemaOverlays = LSWrapper.getItem(_Schema.showSchemaOverlaysKey, true);
			_Schema.ui.updateOverlayVisibility(showSchemaOverlays);

			let showSchemaInheritance = LSWrapper.getItem(_Schema.showSchemaInheritanceArrowsKey, true);
			_Schema.ui.updateInheritanceArrowsVisibility(showSchemaInheritance);

			if (typeof callback === "function") {
				callback();
			}
		});
	},
	showUpdatingSchemaMessage: () => {
		_Dialogs.loadingMessage.show('Updating Schema', 'Please wait...', 'updating-schema-message');
	},
	hideUpdatingSchemaMessage:  () => {
		_Dialogs.loadingMessage.hide('updating-schema-message');
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
				.title('Schema recompiled')
				.text('Another user made changes to the schema. Do you want to reload to see the changes?')
				.specialInteractionButton('Reload', _Schema.reloadSchemaAfterRecompileNotification)
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

		targetView = targetView || LSWrapper.getItem(`${_Entities.activeEditTabPrefix}_${id}`) || 'general';

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

			_Schema.ui.selection.clearSelection();

		}, 'schema');

	},
	bulkDialogsGeneral: {
		closeWithoutSavingChangesQuestionOpen: false,
		overrideDialogCancel: (mainTabs, additionalCallback) => {

			if (Structr.isModuleActive(_Schema)) {

				let newCancelButton = _Dialogs.custom.updateOrCreateDialogCloseButton();
				_Dialogs.custom.setHasCustomCloseHandler();

				newCancelButton.addEventListener('click', async (e) => {

					if (_Schema.bulkDialogsGeneral.closeWithoutSavingChangesQuestionOpen === false) {

						let allowNavigation = true;
						let hasChanges      = _Schema.bulkDialogsGeneral.hasUnsavedChangesInTabs(mainTabs);

						if (hasChanges) {

							_Schema.bulkDialogsGeneral.closeWithoutSavingChangesQuestionOpen = true;
							allowNavigation = await _Dialogs.confirmation.showPromise("Really close with unsaved changes?");
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
		hasUnsavedChangesInGrid: (grid) => {
			return (grid.querySelectorAll('.schema-grid-body .schema-grid-row.to-delete, .schema-grid-body .schema-grid-row.has-changes').length > 0);
		},
		gridChanged: (grid) => {
			let unsavedChanges = _Schema.bulkDialogsGeneral.hasUnsavedChangesInGrid(grid);
			_Schema.bulkDialogsGeneral.dataChanged(unsavedChanges, grid, grid.querySelector('.schema-grid-footer'));
		},
		dataChanged: (hasUnsavedChanges, grid, footerElement) => {

			let tabContainer = grid.closest('.propTabContent');
			if (tabContainer) {
				_Schema.bulkDialogsGeneral.dataChangedInTab(tabContainer, hasUnsavedChanges);
			}

			let discardAll = footerElement.querySelector('.discard-all');
			let saveAll    = footerElement.querySelector('.save-all');

			_Helpers.disableElements(!hasUnsavedChanges, discardAll, saveAll);
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
					let data = _Schema.bulkDialogsGeneral.getPayloadFromBulkInfo(bulkInfo);

					let response = await fetch(Structr.rootUrl + schemaNode.id, {
						method: 'PATCH',
						body: JSON.stringify(data)
					});

					if (Structr.isModuleActive(_Schema)) {

						// Schema reload is only necessary for changes in the "basic" tab for types (name and inheritedTraits) and relationships (relType and cardinality)
						let typeChangeRequiresReload = (bulkInfo?.basic?.changes?.name || bulkInfo?.basic?.changes?.inheritedTraits)
						let relChangeRequiresReload  = (bulkInfo?.basic?.changes?.relationshipType || bulkInfo?.basic?.changes?.sourceMultiplicity || bulkInfo?.basic?.changes?.targetMultiplicity)

						if (typeChangeRequiresReload || relChangeRequiresReload) {
							_Schema.reload();
						}
					}

					if (response.ok) {

						_Code.helpers.addAvailableTagsForEntities([data]);

						_Schema.bulkDialogsGeneral.resetInputsViaTabControls(tabControls);

						_Schema.invalidateTypeInfoCache(schemaNode.name);

					} else {

						let data = await response.json();

						let errors = new Set(data.errors.map(e => e.detail.replaceAll('\n', '<br>').replaceAll(Structr.dynamicClassPrefix, '')));

						if (errors.size > 0) {

							for (let error of errors) {
								new ErrorMessage().title("Problem encountered compiling schema").text(error).requiresConfirmation().show();
							}

						} else {

							Structr.errorFromResponse(data);
						}
					}

					return response.ok;
				}
			}

			return false;
		}
	},
	nodes: {
		populateInheritancePairMap: (list) => {

			let nameKeyedList = Object.fromEntries(list.map(entity => {
				return [entity.name, entity];
			}));

			return Object.fromEntries(list.map(entity => {
				return [entity.id, (entity.inheritedTraits ?? []).map(traitName => nameKeyedList[traitName]?.id).filter(parent => !!parent)]
			}));
		},
		loadNodes: async () => {

			_Schema.hiddenSchemaNodes = JSON.parse(LSWrapper.getItem(_Schema.hiddenSchemaNodesKey));

			let schemaLayout = await _Schema.getFirstSchemaLayoutOrFalse();

			if (schemaLayout && schemaLayout.content && (_Schema.hiddenSchemaNodes === null)) {

				_Schema.applySavedLayoutConfiguration(schemaLayout, true);
			}

			let response = await fetch(`${Structr.rootUrl}SchemaNode/ui?${Structr.getRequestParameterName('sort')}=hierarchyLevel&${Structr.getRequestParameterName('order')}=asc&isServiceClass=false`);
			if (response.ok) {

				let data             = await response.json();
				let entities         = {};
				let inheritanceObject = _Schema.nodes.populateInheritancePairMap(data.result);
				let hierarchy        = {};
				let x = 0, y = 0;

				if (_Schema.hiddenSchemaNodes === null) {
					_Schema.hiddenSchemaNodes = data.result.filter(entity => entity.isBuiltinType).map(entity => entity.name);
					LSWrapper.setItem(_Schema.hiddenSchemaNodesKey, JSON.stringify(_Schema.hiddenSchemaNodes));
				}

				_Schema.nodePositions = LSWrapper.getItem(_Schema.schemaPositionsKey, {});

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

					hierarchy[level] ??= [];
					hierarchy[level].push(entity);

					entities[entity.id] = entity.id;
				}

				for (let entitiesAtHierarchyLevel of Object.values(hierarchy)) {

					for (let entity of entitiesAtHierarchyLevel) {

						_Schema.nodeData[entity.id] = entity;

						if (!(_Schema.hiddenSchemaNodes.length > 0 && _Schema.hiddenSchemaNodes.indexOf(entity.name) > -1)) {

							let id = 'id_' + entity.id;
							_Schema.ui.canvas.append(`
								<div class="schema node compact${(entity.isBuiltinType ? ' light' : '')}" id="${id}" data-type="${entity.name}">
									<b>${entity.name}</b>
									<div class="icons-container flex items-center">
										${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, _Icons.getSvgIconClassesNonColorIcon(['node-action-icon', 'mr-1', 'edit-type-icon']), 'Edit type')}
										${(entity.isBuiltinType ? '' : _Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'node-action-icon', 'delete-type-icon']), 'Delete type'))}
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

							node[0].style.top  = nodePosition.top + 'px';
							node[0].style.left = nodePosition.left + 'px';

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

							x++;
						}
					}

					y++;
					x = 0;
				}

				for (let [sourceId, targetIds] of Object.entries(inheritanceObject)) {

					let sourceEntity = _Schema.nodeData[sourceId];
					let sourceHidden = _Schema.hiddenSchemaNodes.includes(sourceEntity?.name);

					if (sourceEntity && !sourceHidden) {

						for (let targetId of targetIds) {

							let targetEntity = _Schema.nodeData[targetId];
							let targetHidden = _Schema.hiddenSchemaNodes.includes(targetEntity?.name);

							if (targetEntity && !targetHidden) {

								_Schema.ui.jsPlumbInstance.connect({
									source: 'id_' + sourceId,
									target: 'id_' + targetId,
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
						}
					}
				}

			} else {

				throw new Error("Loading of Schema nodes failed");
			}
		},
		loadNode: (entity, mainTabs, contentDiv, targetView = 'general', callbackCancel) => {

			let tabControls       = {};
			let generalTabContent = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'general', 'General', targetView === 'general');
			tabControls.basic     = _Schema.nodes.appendBasicNodeInfo(generalTabContent, entity);

			if (entity.isServiceClass === false) {

				let localPropsTabContent   = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'local', 'Direct properties', targetView === 'local');
				let remotePropsTabContent  = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'remote', 'Linked properties', targetView === 'remote');
				let builtinPropsTabContent = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'builtin', 'Inherited properties', targetView === 'builtin');
				let viewsTabContent        = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'views', 'Views', targetView === 'views');

				tabControls.schemaProperties = _Schema.properties.appendLocalProperties(localPropsTabContent, entity);
				tabControls.remoteProperties = _Schema.remoteProperties.appendRemote(remotePropsTabContent, entity, async (el) => { await _Schema.remoteProperties.asyncEditSchemaObjectLinkHandler(el, mainTabs, entity.id); });
				tabControls.schemaViews      = _Schema.views.appendViews(viewsTabContent, entity);

				let basicTabContentContainer = generalTabContent.querySelector('.schema-details');
				tabControls.schemaGrants     = _Schema.schemaGrants.appendSchemaGrants(basicTabContentContainer, entity);

				_Schema.properties.appendBuiltinProperties(builtinPropsTabContent, entity);
			}

			let methodsTabContent     = _Entities.appendPropTab(entity, mainTabs, contentDiv, 'methods', 'Methods', targetView === 'methods', _Editors.resizeVisibleEditors);
			tabControls.schemaMethods = _Schema.methods.appendMethods(methodsTabContent, entity, entity.schemaMethods);

			_Schema.bulkDialogsGeneral.overrideDialogCancel(mainTabs, callbackCancel);

			// fallback: if no tab is active because the given targetView is not available, use the first tab
			if (!mainTabs.querySelector('.active')) {
				mainTabs.querySelector('li').click();
			}

			Structr.resize();

			return tabControls;
		},
		showCreateTypeDialog: async () => {

			let { dialogText } = _Dialogs.custom.openDialog("Create Type", null, ['schema-edit-dialog']);

			let discardButton = _Dialogs.custom.prependCustomDialogButton(_Schema.templates.discardActionButton({ text: 'Discard and close' }));
			let saveButton    = _Dialogs.custom.prependCustomDialogButton(_Schema.templates.saveActionButton({ text: 'Create' }));

			let initialData;

			let updateChangeStatus = () => {

				let typeInfo    = _Schema.nodes.getTypeDefinitionDataFromForm(dialogText, initialData);
				let changedData = _Schema.nodes.getTypeDefinitionChanges(initialData, typeInfo);
				let hasChanges  = Object.keys(changedData).length > 0;

				_Helpers.disableElements(!hasChanges, saveButton);
			};

			updateChangeStatus();

			_Schema.nodes.showCreateNewTypeDialog(dialogText, updateChangeStatus, { isServiceClass: false });

			initialData = _Schema.nodes.getTypeDefinitionDataFromForm(dialogText, {});

			_Dialogs.custom.setDialogSaveButton(saveButton);

			for (let property of dialogText.querySelectorAll('[data-property]')) {
				property.addEventListener('input', updateChangeStatus);
			}

			_Schema.nodes.activateTagsSelect(dialogText.querySelector('#openapi-options'), updateChangeStatus);

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
			}

			let select = container.querySelector('[data-property="inheritedTraits"]');
			select?.insertAdjacentHTML('beforeend', (entity.inheritedTraits ?? []).map(trait => `<option>${trait}</option>`).join(''));

			let schemaNodeFlags = ['isServiceClass', 'changelogDisabled', 'defaultVisibleToPublic', 'defaultVisibleToAuth'];

			for (let flag of schemaNodeFlags) {

				let checkbox = container.querySelector(`[data-property="${flag}"]`);
				if (checkbox) checkbox.checked = (true === entity[flag]);
			}

			_Code.mainArea.populateOpenAPIBaseConfig(container, entity, _Code.availableTags);
		},
		appendTypeHierarchy: (container, entity = {}, changeFn) => {

			fetch(`${Structr.rootUrl}_schema`).then(response => response.json()).then(schemaData => {

				let customTypes  = schemaData.result.filter(type => !type.isAbstract && !type.isRel && !type.isInterface && !type.isServiceClass && !type.isBuiltin && type.name !== entity.name);
				let builtinTypes = schemaData.result.filter(type => !type.isAbstract && !type.isRel && !type.isInterface && !type.isServiceClass && type.isBuiltin && type.name !== entity.name);

				let getOptionsForListOfTypes = (typeList) => {
					return typeList.map(type => type.name).sort().map(name => `<option ${(entity.inheritedTraits ?? []).includes(name) ? 'selected' : ''} value="${name}">${name}</option>`).join('');
				};

				let classSelect = container.querySelector('[data-property="inheritedTraits"]');
				classSelect.insertAdjacentHTML('beforeend', `
					${(customTypes.length > 0) ? `<optgroup label="Custom Traits">${getOptionsForListOfTypes(customTypes)}</optgroup>` : ''}
					${(builtinTypes.length > 0) ? `<optgroup label="System Traits">${getOptionsForListOfTypes(builtinTypes)}</optgroup>` : ''}
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
		appendBasicNodeInfo: (tabContent, entity) => {

			tabContent.appendChild(_Helpers.createSingleDOMElementFromHTML(_Schema.templates.typeBasicTab({ isServiceClass: entity?.isServiceClass, isCreate: !entity })));

			let basicTypeContainer = tabContent.querySelector('.basic-schema-details');

			_Helpers.activateCommentsInElement(tabContent);

			let updateChangeStatus = () => {

				let typeInfo    = _Schema.nodes.getTypeDefinitionDataFromForm(basicTypeContainer, entity);
				let changedData = _Schema.nodes.getTypeDefinitionChanges(entity, typeInfo);
				let hasChanges  = Object.keys(changedData).length > 0;

				_Schema.bulkDialogsGeneral.dataChangedInTab(tabContent, hasChanges);
			};

			_Schema.nodes.populateBasicTypeInfo(tabContent, entity);

			for (let property of tabContent.querySelectorAll('[data-property]')) {
				property.addEventListener('input', updateChangeStatus);
			}

			if (entity.isServiceClass === false) {

				_Schema.nodes.appendTypeHierarchy(tabContent, entity, updateChangeStatus);
			}

			_Schema.nodes.activateTagsSelect(tabContent.querySelector('#openapi-options'), updateChangeStatus);

			return {
				getBulkInfo: (doValidate) => {

					let typeInfo    = _Schema.nodes.getTypeDefinitionDataFromForm(basicTypeContainer, entity);
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
		getTypeDefinitionDataFromForm: (tabContent, entity) => {
			return _Code.persistence.collectDataFromContainer(tabContent, entity);
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

			let hasArrayChanged = (key) => {

				let prevValue = entity[key] ?? [];
				let newValue  = newData[key];

				if (prevValue.length === newValue.length) {

					let difference = prevValue.filter(t => !newValue.includes(t));
					return (difference.length > 0);
				}

				return true;
			};

			for (let key in newData) {

				let shouldDelete = (entity[key] === newData[key]);

				if (key === 'tags' || key === 'inheritedTraits') {

					shouldDelete = !hasArrayChanged(key);
				}

				if (shouldDelete) {
					delete newData[key];
				}
			}

			return newData;
		},
		resetTypeDefinition: (container, entity) => {
			_Code.persistence.revertFormDataInContainer(container, entity);
		},
		showCreateNewTypeDialog: (container, updateFunction, config = {}) => {

			container.insertAdjacentHTML('beforeend', _Schema.templates.typeBasicTab({ isCreate: true, ...config }));

			if (config.isServiceClass === false) {
				_Schema.nodes.appendTypeHierarchy(container, {}, updateFunction);
			}

			_Schema.nodes.activateTagsSelect(container);
			_Code.mainArea.populateOpenAPIBaseConfig(container, {}, _Code.availableTags);

			_Helpers.activateCommentsInElement(container);
		},
		createTypeDefinition: (data) => {

			return new Promise(((resolve, reject) => {

				fetch(`${Structr.rootUrl}SchemaNode`, {
					method: 'POST',
					body: JSON.stringify(data)
				}).then(response => {

					response.json().then(responseData => {

						if (response.ok) {

							resolve(responseData);

						} else {

							reject(responseData);
						}
					});
				})
			}));
		},
	},
	relationships: {
		loadRels: async () => {

			let response = await fetch(`${Structr.rootUrl}SchemaRelationshipNode`);
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
								cssClass: "label multiplicity",
								label: res.targetMultiplicity ? res.targetMultiplicity : '*',
								location: Math.max(.8 - offset, .6),
								id: "targetMultiplicity"
							}],
							["Label", {
								cssClass: "label rel-type",
								label: `<div id="rel_${res.id}" class="flex items-center" data-name="${res.name}" data-source-type="${_Schema.nodeData[res.sourceId].name}" data-target-type="${_Schema.nodeData[res.targetId].name}">
											${(res.relationshipType === _Schema.initialRelType ? '<span>&nbsp;</span>' : res.relationshipType)}
											${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, _Icons.getSvgIconClassesNonColorIcon(['mr-1', 'ml-2', 'edit-relationship-icon']), 'Edit relationship')}
											${(res.isPartOfBuiltInSchema ? '' : _Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'mr-1', 'delete-relationship-icon']), 'Delete relationship'))}
										</div>`,
								location: .5,
								id: "label"
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

			let basicTabContent      = _Entities.appendPropTab(entity, tabsContainer, contentDiv, 'general', 'General', targetView === 'general');
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

			// fallback: if no tab is active because the given targetView is not available, use the first tab
			if (!tabsContainer.querySelector('.active')) {
				tabsContainer.querySelector('li').click();
			}

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

				for (let underlinedSchemaLink of container.querySelectorAll('.edit-schema-object')) {
					underlinedSchemaLink.classList.remove('edit-schema-object');
					underlinedSchemaLink.classList.remove('cursor-pointer');
				}
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

			container.querySelector('#source-type-name span').textContent    = sourceNode.name;
			container.querySelector('#target-type-name span').textContent    = targetNode.name;
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

				let initialData;

				let updateChangeStatus = () => {

					let changedData = _Schema.relationships.getRelationshipDefinitionChanges(dialogText, initialData);
					let hasChanges  = Object.keys(changedData).length > 0;

					_Helpers.disableElements(!hasChanges, saveButton);
				};

				initialData = _Schema.relationships.getRelationshipDefinitionChanges(dialogText, {});
				updateChangeStatus();

				for (let property of dialogText.querySelectorAll('[data-attr-name]')) {
					property.addEventListener('input', updateChangeStatus);
				}

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

			let response = await fetch(Structr.rootUrl + 'SchemaRelationshipNode', {
				method: 'POST',
				body: JSON.stringify(data)
			});

			let responseData = await response.json();

			if (response.ok) {

				_Schema.openEditDialog(responseData.result[0]);
				_Schema.reload();

			} else {

				_Schema.relationships.reportRelationshipError(data, data, responseData);
			}
		},
		removeRelationshipDefinition: async (id) => {

			let response = await fetch(Structr.rootUrl + 'SchemaRelationshipNode/' + id, {
				method: 'DELETE'
			});

			if (response.ok) {

				_Schema.reload();

			} else {

				let data = await response.json();

				Structr.errorFromResponse(data);
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

			let showDatabaseName = (UISettings.getValueForSetting(UISettings.settingGroups.schema_code.settings.showDatabaseNameForDirectProperties) === true);
			let dbNameClass      = (showDatabaseName ? 'flex' : 'hidden');

			let gridConfig = {
				class: 'local schema-props grid',
				style: 'grid-template-columns: [ name ] minmax(0, 1fr) ' +  ((showDatabaseName) ? '[ dbName ] minmax(0, 1fr) ' : '') + '[ type ] minmax(10%, max-content) [ format ] minmax(10%, max-content) [ notNull ] minmax(5%, max-content) [ compositeUnique ] minmax(5%, max-content) [ unique ] minmax(5%, max-content) [ indexed ] minmax(5%, max-content) [ fulltext ] minmax(5%, max-content) [ defaultValue ] minmax(0, 1fr) [ actions ] 4rem',
				cols: [
					{ class: 'py-2 px-1 font-bold flex items-center justify-center', title: 'JSON Name' },
					{ class: 'py-2 px-1 font-bold items-center justify-center ' + dbNameClass, title: 'DB Name' },
					{ class: 'py-2 px-1 font-bold flex items-center justify-center', title: 'Type' },
					{ class: 'py-2 px-1 font-bold flex items-center justify-center', title: 'Format' },
					{ class: 'py-2 px-1 font-bold flex items-center justify-center', title: 'Notnull' },
					{ class: 'py-2 px-1 font-bold flex items-center justify-center', title: 'Comp.' },
					{ class: 'py-2 px-1 font-bold flex items-center justify-center', title: 'Uniq.' },
					{ class: 'py-2 px-1 font-bold flex items-center justify-center', title: 'Idx' },
					{ class: 'py-2 px-1 font-bold flex items-center justify-center', title: 'Fulltext' },
					{ class: 'py-2 px-1 font-bold flex items-center justify-center', title: 'Default' },
					{ class: 'actions-col pb-1 px-1 font-bold flex items-center justify-center', title: 'Action' }
				],
				buttons: _Schema.templates.basicAddButton({ addButtonText: 'Add direct property' })
			};

			let grid = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.schemaGrid(gridConfig));
			container.appendChild(grid);
			let gridBody = grid.querySelector('.schema-grid-body');

			_Helpers.sort(entity.schemaProperties);

			let typeOptions       = _Schema.properties.templates.typeOptions();
			let typeHintOptions   = _Schema.properties.templates.typeHintOptions();

			for (let prop of entity.schemaProperties) {

				let gridRow = $(_Schema.properties.templates.propertyLocal({ property: prop, typeOptions: typeOptions, typeHintOptions: typeHintOptions, dbNameClass: dbNameClass }));
				gridBody.appendChild(gridRow[0]);

				_Schema.properties.setAttributesInRow(prop, gridRow);
				_Schema.properties.bindRowEvents(prop, gridRow, overrides);

				_Schema.properties.removeUnwantedPropertyTypes(prop, gridRow[0]);

				_Schema.properties.checkProperty(gridRow[0]);

				_Schema.bulkDialogsGeneral.gridChanged(grid);
			}

			let resetFunction = () => {
				for (let discardIcon of gridBody.querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
			};

			container.querySelector('.discard-all').addEventListener('click', resetFunction);

			grid.querySelector('.save-all').addEventListener('click', () => {
				_Schema.properties.bulkSave(container, gridBody, entity, overrides, optionalAfterSaveCallback);
			});

			container.querySelector('button.add-button').addEventListener('click', () => {

				let gridRow = _Helpers.createSingleDOMElementFromHTML(_Schema.properties.templates.propertyNew({ typeOptions: typeOptions, dbNameClass: dbNameClass }));
				gridBody.appendChild(gridRow);

				_Schema.properties.removeUnwantedPropertyTypes({}, gridRow);

				let propertyTypeSelect = gridRow.querySelector('.property-type');

				propertyTypeSelect.addEventListener('change', () => {

					let selectedOption = propertyTypeSelect.querySelector('option:checked');
					let indexedCb      = gridRow.querySelector('.indexed');
					let shouldIndex    = selectedOption.dataset['indexed'];
					shouldIndex = (shouldIndex === undefined) || (shouldIndex !== 'false');

					if (indexedCb.checked !== shouldIndex) {

						indexedCb.checked = shouldIndex;

						_Helpers.blink(indexedCb.parentNode, '#fff', '#bde5f8');
						_Dialogs.custom.showAndHideInfoBoxMessage('Automatically updated indexed flag to default behavior for property type (you can still override this)', 'info', 2000, 500);
					}

					let allowFulltext   = (selectedOption.value === 'String' || selectedOption.value === 'StringArray');
					let fulltextCb      = gridRow.querySelector('.fulltext-indexed');
					let checked         = (allowFulltext && !fulltextCb.disabled && fulltextCb.checked);
					fulltextCb.disabled = !allowFulltext;
					fulltextCb.checked  = checked;
				});

				gridRow.querySelector('.discard-changes').addEventListener('click', () => {
					_Helpers.fastRemoveElement(gridRow);
					_Schema.bulkDialogsGeneral.gridChanged(grid);
				});

				_Schema.bulkDialogsGeneral.gridChanged(grid);
			});

			return {
				getBulkInfo: (doValidate) => {
					return _Schema.properties.getDataFromGrid(gridBody, entity, doValidate);
				},
				reset: resetFunction
			};
		},
		getDataFromGrid: (gridBody, entity, doValidate = true) => {

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

			for (let gridRow of gridBody.querySelectorAll('.schema-grid-row')) {

				let propertyId = gridRow.dataset['propertyId'];
				let prop       = _Schema.properties.getDataFromRow(gridRow);

				if (propertyId) {
					if (gridRow.classList.contains('to-delete')) {
						// do not add this property to the list
						counts.delete++;
					} else if (gridRow.classList.contains('has-changes')) {
						// changed lines
						counts.update++;
						prop.id = propertyId;
						if (doValidate) {
							allow = _Schema.properties.validateProperty(prop, gridRow) && allow;
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
						allow = _Schema.properties.validateProperty(prop, gridRow) && allow;
					}
					data.schemaProperties.push(prop);
				}
			}

			return { name, data, allow, counts };
		},
		bulkSave: (container, tbody, entity, overrides, optionalAfterSaveCallback) => {

			let { allow, counts, data } = _Schema.properties.getDataFromGrid(tbody, entity);

			if (allow) {

				fetch(Structr.rootUrl + entity.id, {
					method: 'PUT',
					body: JSON.stringify(data)
				}).then((response) => {

					if (response.ok) {

						Command.get(entity.id, null, (reloadedEntity) => {

							_Helpers.fastRemoveAllChildren(container);

							_Schema.properties.appendLocalProperties(container, reloadedEntity, overrides, optionalAfterSaveCallback);

							_Schema.invalidateTypeInfoCache(entity.name);

							if (optionalAfterSaveCallback) {
								optionalAfterSaveCallback();
							}
						});

					} else {

						response.json().then((data) => {
							Structr.errorFromResponse(data, undefined, { requiresConfirmation: true });
						});
					}
				});
			}
		},
		bindRowEvents: (property, gridRow, overrides) => {

			let propertyInfoChangeHandler = () => {
				_Schema.properties.rowChanged(property, gridRow[0]);
			};

			let isProtected = false;

			let propertyTypeOption = $(`.property-type option[value="${property.propertyType}"]`, gridRow);
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

			$('.property-type option[value=""]', gridRow).remove();

			if (property.propertyType === 'String' && !property.isBuiltinProperty) {
				$('.content-type', gridRow).val(property.contentType).on('keyup', propertyInfoChangeHandler).prop('disabled', null);
			}

			$('.property-name',    gridRow).on('keyup', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.property-dbname',  gridRow).on('keyup', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.caching-enabled',  gridRow).on('change', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.type-hint',        gridRow).on('change', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.property-type',    gridRow).on('change', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.property-format',  gridRow).on('keyup', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.not-null',         gridRow).on('change', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.compound',         gridRow).on('change', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.unique',           gridRow).on('change', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.indexed',          gridRow).on('change', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.fulltext-indexed', gridRow).on('change', propertyInfoChangeHandler).prop('disabled', isProtected);
			$('.property-default', gridRow).on('keyup', propertyInfoChangeHandler).prop('disabled', isProtected);

			let propertyTypeSelect = gridRow[0].querySelector('.property-type');

			let handlePropertyTypeChange = () => {

				let selectedOption = propertyTypeSelect.querySelector('option:checked');

				let allowFulltext = (selectedOption.value === 'String' || selectedOption.value === 'StringArray');

				let fulltextCb = gridRow[0].querySelector('.fulltext-indexed');
				fulltextCb.disabled = (!allowFulltext || isProtected);
				fulltextCb.checked = allowFulltext && property.fulltext === true;

				propertyInfoChangeHandler();
			};

			propertyTypeSelect.addEventListener('change', handlePropertyTypeChange);
			handlePropertyTypeChange();

			let readWriteButtonClickHandler = async (targetProperty) => {

				if (overrides && overrides.editReadWriteFunction) {

					overrides.editReadWriteFunction(property);

				} else {

					let unsavedChanges = _Schema.bulkDialogsGeneral.hasUnsavedChangesInGrid(gridRow[0].closest('.schema-grid'));

					if (!unsavedChanges || (true === await _Dialogs.confirmation.showPromise("Really switch to code editing? There are unsaved changes which will be lost!"))) {
						_Schema.properties.openCodeEditorForFunctionProperty(property.id, targetProperty, () => {
							if (Structr.isModuleActive(_Schema)) {
								_Schema.openEditDialog(property.schemaNode.id, 'local');
							}
						});
					}
				}
			};

			$('.edit-read-function', gridRow).on('click', () => {
				readWriteButtonClickHandler('read');
			}).prop('disabled', isProtected);

			$('.edit-write-function', gridRow).on('click', () => {
				readWriteButtonClickHandler('write');
			}).prop('disabled', isProtected);

			$('.edit-cypher-query', gridRow).on('click', () => {

				if (overrides && overrides.editCypherProperty) {

					overrides.editCypherProperty(property);

				} else {

					let unsavedChanges = _Schema.bulkDialogsGeneral.hasUnsavedChangesInGrid(gridRow[0].closest('.schema-grid'));

					if (!unsavedChanges || confirm("Really switch to code editing? There are unsaved changes which will be lost!")) {
						_Schema.properties.openCodeEditorForCypherProperty(property.id, () => {
							_Schema.openEditDialog(property.schemaNode.id, 'local');
						});
					}
				}

			}).prop('disabled', isProtected);

			if (!isProtected) {

				$('.remove-action', gridRow).on('click', function() {

					gridRow.addClass('to-delete');
					propertyInfoChangeHandler();

				}).prop('disabled', null);

				$('.discard-changes', gridRow).on('click', function() {

					_Schema.properties.setAttributesInRow(property, gridRow);

					gridRow.removeClass('to-delete');
					gridRow.removeClass('has-changes');

					propertyInfoChangeHandler();

				}).prop('disabled', null);

			} else {
				$('.remove-action', gridRow).hide();
			}
		},
		getDataFromRow: (gridRow) => {

			let obj = {
				name:             gridRow.querySelector('.property-name').value,
				dbName:           gridRow.querySelector('.property-dbname').value,
				propertyType:     gridRow.querySelector('.property-type',).value,
				contentType:      gridRow.querySelector('.content-type')?.value ?? null,
				format:           gridRow.querySelector('.property-format')?.value ?? null,
				notNull:          gridRow.querySelector('.not-null').checked,
				compound:         gridRow.querySelector('.compound').checked,
				unique:           gridRow.querySelector('.unique').checked,
				indexed:          gridRow.querySelector('.indexed').checked,
				fulltext:         gridRow.querySelector('.fulltext-indexed').checked,
				defaultValue:     gridRow.querySelector('.property-default').value,
				isCachingEnabled: gridRow.querySelector('.caching-enabled')?.checked ?? false,
				typeHint:         gridRow.querySelector('.type-hint')?.value ?? null
			};

			if (obj.typeHint === "null") {
				obj.typeHint = null;
			}

			return obj;
		},
		setAttributesInRow: (property, gridRow) => {

			$('.property-name', gridRow).val(property.name);
			$('.property-dbname', gridRow).val(property.dbName);
			$('.property-type', gridRow).val(property.propertyType);
			$('.content-type', gridRow).val(property.contentType);
			$('.property-format', gridRow).val(property.format);
			$('.not-null', gridRow).prop('checked', property.notNull);
			$('.compound', gridRow).prop('checked', property.compound);
			$('.unique', gridRow).prop('checked', property.unique);
			$('.indexed', gridRow).prop('checked', property.indexed);
			$('.fulltext-indexed', gridRow).prop('checked', property.fulltext);
			$('.property-default', gridRow).val(property.defaultValue);
			$('.caching-enabled', gridRow).prop('checked', property.isCachingEnabled);
			$('.type-hint', gridRow).val(property.typeHint ?? "null");
		},
		removeUnwantedPropertyTypes: (property, gridRow) => {

			// remove unused/unwanted property types
			let unwantedPropertyTypes = ['Count', 'Notion', 'Join', 'IdNotion', 'Custom', 'Password'];
			if (false === unwantedPropertyTypes.includes(property.propertyType)) {
				for (let unwantedPropertyType of unwantedPropertyTypes) {
					gridRow.querySelector(`[value=${unwantedPropertyType}]`).remove();
				}
			}
		},
		checkProperty: (gridRow) => {

			let propertyInfoUI = _Schema.properties.getDataFromRow(gridRow);
			let container      = gridRow.querySelector('.indexed').closest('div');

			_Schema.properties.checkFunctionProperty(propertyInfoUI, container);
		},
		checkFunctionProperty: (propertyInfoUI, containerForWarning) => {

			let warningClassName = 'indexed-function-property-warning';
			let existingWarning  = containerForWarning.querySelector(`.${warningClassName}`);

			if (propertyInfoUI.propertyType === 'Function' && propertyInfoUI.indexed === true && (!propertyInfoUI.typeHint || propertyInfoUI.typeHint === '')) {

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
		rowChanged: (property, gridRow) => {

			let propertyInfoUI = _Schema.properties.getDataFromRow(gridRow);
			let hasChanges     = false;

			_Schema.properties.checkProperty(gridRow);

			for (let key in propertyInfoUI) {

				if ((propertyInfoUI[key] === '' || propertyInfoUI[key] === null || propertyInfoUI[key] === undefined) && (property[key] === '' || property[key] === null || property[key] === undefined)) {
					// account for different attribute-sets and fuzzy equality
				} else if (propertyInfoUI[key] !== property[key]) {

					if (property.propertyType === 'Cypher' && key === 'format') {
						// ignore changes in format (if the property is Cypher)... it is not displayed and collected
					} else if (propertyInfoUI.propertyType !== 'Function' && (key === 'isCachingEnabled' || key === 'typeHint')) {
						// ignore changes in isCachingEnabled and typeHint if the property is currently not configured as a Function
					} else {
						hasChanges = true;
					}
				}
			}

			_Schema.markElementAsChanged(gridRow, hasChanges);

			_Schema.bulkDialogsGeneral.gridChanged(gridRow.closest('.schema-grid'));
		},
		validateProperty: (propertyDefinition, gridRow) => {

			if (propertyDefinition.name.length === 0) {

				_Helpers.blinkRed(gridRow.querySelector('.property-name').closest('div'));
				return false;

			} else if (propertyDefinition.propertyType.length === 0) {

				_Helpers.blinkRed(gridRow.querySelector('.property-type').closest('div'));
				return false;

			} else if (propertyDefinition.propertyType === 'Enum' && propertyDefinition.format.trim().length === 0) {

				_Helpers.blinkRed(gridRow.querySelector('.property-format').closest('div'));
				return false;

			} else if (propertyDefinition.propertyType === 'Enum') {

				let containsSpace = propertyDefinition.format.split(',').some(function (enumVal) {
					return enumVal.trim().indexOf(' ') !== -1;
				});

				if (containsSpace) {
					_Helpers.blinkRed(gridRow.querySelector('.property-format').closest('div'));
					new WarningMessage().text(`Enum values must be separated by commas and can not contain spaces<br>See the <a href="${_Helpers.getDocumentationURLForTopic('schema-enum')}" target="_blank">support article on enum properties</a> for more information.`).requiresConfirmation().show();
					return false;
				}
			}

			return true;
		},
		openCodeEditorForFunctionProperty: (id, key, closeCallback) => {

			let codeKey = `${key}Function`;
			let wrapKey = `${key}FunctionWrapJS`;

			Command.get(id, `id,name,contentType,${codeKey},${wrapKey}`, (entity) => {

				let { dialogText, dialogMeta } = _Dialogs.custom.openDialog(`Edit ${key} function of ${entity.name}`, closeCallback, ['popup-dialog-with-editor']);
				dialogText.insertAdjacentHTML('beforeend', '<div class="editor h-full"></div>');
				dialogMeta.insertAdjacentHTML('beforeend', '<span class="editor-info"></span>');
				_Dialogs.custom.showMeta();

				let dialogSaveButton = _Dialogs.custom.updateOrCreateDialogSaveButton();
				let saveAndClose     = _Dialogs.custom.updateOrCreateDialogSaveAndCloseButton();
				let editorInfo       = dialogMeta.querySelector('.editor-info');
				_Editors.appendEditorOptionsElement(editorInfo);

				let wrapChoice = _Helpers.createSingleDOMElementFromHTML(`<span>${ _Schema.properties.templates.functionProperty[wrapKey]({ entity }) }</span>`);
				editorInfo.appendChild(wrapChoice);
				_Helpers.activateCommentsInElement(wrapChoice);

				let initialText     = entity[codeKey] ?? '';
				let initialWrapFlag = entity[wrapKey];
				let editor, flagCheckbox;

				let isChanged = () => {
					return !(editor.getValue() === initialText && flagCheckbox.checked === initialWrapFlag);
				};

				flagCheckbox = wrapChoice.querySelector('input[type="checkbox"]');
				flagCheckbox.addEventListener('change', (e) => {
					let disabled = !isChanged();
					_Helpers.disableElements(disabled, dialogSaveButton, saveAndClose);
				});

				let updateCodeWrappingCheckboxVisibility = (source) => {
					let isJs = (_Editors.getMonacoEditorModeForContent(source, entity) === 'javascript');
					wrapChoice.classList.toggle('hidden', !isJs);
				};
				updateCodeWrappingCheckboxVisibility(initialText);

				let functionPropertyMonacoConfig = {
					language: 'auto',
					lint: true,
					autocomplete: true,
					changeFn: (editor, entity) => {

						updateCodeWrappingCheckboxVisibility(editor.getValue());

						let disabled = !isChanged();
						_Helpers.disableElements(disabled, dialogSaveButton, saveAndClose);
					},
					saveFn: (editor, entity, close = false) => {

						if (!isChanged()) {
							return;
						}

						Command.setProperties(entity.id, { [codeKey]: editor.getValue(), [wrapKey]: flagCheckbox.checked }, () => {

							_Dialogs.custom.showAndHideInfoBoxMessage('Saved successfully', 'success', 2000, 200);

							_Helpers.disableElements(true, dialogSaveButton, saveAndClose);

							Command.get(id, `id,${codeKey},${wrapKey}`, (entity) => {
								initialText     = entity[codeKey];
								initialWrapFlag = entity[wrapKey];
							});

							if (close === true) {
								_Dialogs.custom.clickDialogCancelButton();
							}
						});
					},
					saveFnText: `Save ${key} function`,
					preventRestoreModel: true,
					isAutoscriptEnv: true
				};

				editor = _Editors.getMonacoEditor(entity, codeKey, dialogText.querySelector('.editor'), functionPropertyMonacoConfig);
				_Editors.addEscapeKeyHandlersToPreventPopupClose(entity.id, codeKey, editor);

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
				class: 'builtin schema-props grid',
				style: 'grid-template-columns: [ declaringClass ] minmax(0, 1fr) [ jsonName ] minmax(0, 1fr) [ type ] minmax(0, 1fr) [ notNull ] minmax(5%, max-content) [ compositeUnique ] minmax(5%, max-content) [ unique ] minmax(5%, max-content) [ indexed ] minmax(5%, max-content) [ fulltext ] minmax(5%, max-content);',
				cols: [
					{ class: 'pb-2 px-1 font-bold flex justify-center', title: 'Declaring Class' },
					{ class: 'pb-2 px-1 font-bold flex justify-center', title: 'JSON Name' },
					{ class: 'pb-2 px-1 font-bold flex justify-center', title: 'Type' },
					{ class: 'pb-2 px-1 font-bold flex justify-center', title: 'Notnull' },
					{ class: 'pb-2 px-1 font-bold flex justify-center', title: 'Comp.' },
					{ class: 'pb-2 px-1 font-bold flex justify-center', title: 'Uniq.' },
					{ class: 'pb-2 px-1 font-bold flex justify-center', title: 'Idx' },
					{ class: 'pb-2 px-1 font-bold flex justify-center', title: 'Fulltext' }
				],
				noButtons: true
			};

			let propertiesTable = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.schemaGrid(tableConfig));
			let gridBody           = propertiesTable.querySelector('.schema-grid-body');
			propertiesTable.querySelector('.schema-grid-footer').classList.add('hidden');

			container.appendChild(propertiesTable);

			Command.listSchemaProperties(entity.id, 'ui', (data) => {

				_Helpers.sort(data, 'declaringClass', 'name');

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
							fulltext: prop.fulltext,
							declaringClass: prop.declaringClass
						};

						gridBody.insertAdjacentHTML('beforeend', _Schema.properties.templates.propertyBuiltin({ property: property }));
					}
				}
			});
		},
		templates: {
			propertyBuiltin: config => `
				<div class="schema-grid-row contents" data-property-name="${config.property.name}" data-property-id="${config.property.id}">
					<div class="py-3 px-2 flex items-center">${_Helpers.escapeForHtmlAttributes(config.property.declaringClass)}</div>
					<div class="py-3 px-2 flex items-center">${_Helpers.escapeForHtmlAttributes(config.property.name)}</div>
					<div class="py-3 px-2 flex items-center">${config.property.propertyType}</div>
					<div class="flex items-center justify-center">
						<input class="not-null" type="checkbox" disabled="disabled" ${(config.property.notNull ? 'checked' : '')} style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						<input class="compound" type="checkbox" disabled="disabled" ${(config.property.compound ? 'checked' : '')} style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						<input class="unique" type="checkbox" disabled="disabled" ${(config.property.unique ? 'checked' : '')} style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						<input class="indexed" type="checkbox" disabled="disabled" ${(config.property.indexed ? 'checked' : '')} style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						<input class="fulltext-indexed" type="checkbox" disabled="disabled" ${(config.property.fulltext ? 'checked' : '')} style="margin-right: 0;">
					</div>
				</div>
			`,
			propertyLocal: config => `
				<div class="schema-grid-row contents" data-property-id="${config.property.id}" >
					<div class="p-2 flex items-center">
						<input size="15" type="text" class="property-name" value="${_Helpers.escapeForHtmlAttributes(config.property.name)}">
					</div>
					<div class="p-2 ${config.dbNameClass}">
						<input size="15" type="text" class="property-dbname" value="${_Helpers.escapeForHtmlAttributes(config.property.dbName)}">
					</div>
					<div class="flex items-center">
						${config.typeOptions}
						${(config.property.propertyType === 'String' && !config.property.isBuiltinProperty) ? '<input type="text" class="content-type w-12" title="Content-Type">' : ''}
					</div>
					<div class="p-2 flex items-center">
						${(() => {
							switch (config.property.propertyType) {
								case 'Function':
									return `
										<div class="flex items-center">
											<button class="flex items-center edit-read-function mr-1 hover:bg-gray-100 focus:border-gray-666 active:border-green">Read${(config.property.readFunctionWrapJS === true) ? _Icons.getSvgIcon(_Icons.iconScriptWrapped, 20, 20, ['ml-1'], 'This script (if JavaScript) is being wrapped in a main() function, thus enabling the usage or the return statement and not allowing import statements.') : ''}</button>
											<button class="flex items-center edit-write-function hover:bg-gray-100 focus:border-gray-666 active:border-green">Write${(config.property.writeFunctionWrapJS === true) ? _Icons.getSvgIcon(_Icons.iconScriptWrapped, 20, 20, ['ml-1'], 'This script (if JavaScript) is being wrapped in a main() function, thus enabling the usage or the return statement and not allowing import statements.') : ''}</button>
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
						})()}
					</div>
					<div class="flex items-center justify-center">
						<input class="not-null" type="checkbox" ${(config.property.notNull ? 'checked' : '')} style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						<input class="compound" type="checkbox" ${(config.property.compound ? 'checked' : '')} style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						<input class="unique" type="checkbox" ${(config.property.unique ? 'checked' : '')} style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						<input class="indexed" type="checkbox" ${(config.property.indexed ? 'checked' : '')} style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						<input class="fulltext-indexed" type="checkbox" ${(config.property.fulltext ? 'checked' : '')} style="margin-right: 0;">
					</div>
					<div class="py-3 px-2"><input type="text" size="10" class="property-default" value="${_Helpers.escapeForHtmlAttributes(config.property.defaultValue)}"></div>
					<div class="actions-col flex items-center justify-center">
						${config.property.isBuiltinProperty ? '' : _Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']), 'Discard changes')}
						${config.property.isBuiltinProperty ? '' : _Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16,   _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-action']), 'Delete')}
					</div>
				</div>
			`,
			propertyNew: config => `
				<div class="schema-grid-row has-changes contents">
					<div class="p-2">
						<input size="15" type="text" class="property-name" placeholder="JSON name" autofocus>
					</div>
					<div class="p-1 ${config.dbNameClass}">
						<input size="15" type="text" class="property-dbname" placeholder="DB Name">
					</div>
					<div class="flex items-center">${config.typeOptions}</div>
					<div class="p-2 flex items-center">
						<input size="15" type="text" class="property-format" placeholder="Format">
					</div>
					<div class="flex items-center justify-center">
						<input class="not-null" type="checkbox" style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						<input class="compound" type="checkbox" style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						<input class="unique" type="checkbox" style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						<input class="indexed" type="checkbox" style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						<input class="fulltext-indexed" type="checkbox" style="margin-right: 0;" disabled>
					</div>
					<div class="p-2 flex items-center">
						<input class="property-default" size="10" type="text" placeholder="Default Value">
					</div>
					<div class="actions-col flex items-center justify-center">
						${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16,  _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']), 'Discard')}
					</div>
				</div>
			`,
			typeHintOptions: config => `
				<select class="type-hint pr-2 hover:bg-gray-100 focus:border-gray-666 active:border-green">
					<optgroup label="Type Hint">
						<option value="null">-</option>
						<option value="boolean">Boolean</option>
						<option value="date">Date</option>
						<option value="double">Double</option>
						<option value="int">Int</option>
						<option value="long">Long</option>
						<option value="string">String</option>
					</optgroup>
				</select>
			`,
			typeOptions: config => `
				<select class="property-type pr-6 hover:bg-gray-100 focus:border-gray-666 active:border-green">
					<option value="">--Select--</option>
					<option value="Boolean">Boolean</option>
					<option value="BooleanArray">Boolean[]</option>
					<option value="ByteArray">Byte[]</option>
					<option value="Cypher" data-indexed="false">Cypher</option>
					<option value="Date">Date</option>
					<option value="DateArray">Date[]</option>
					<option value="Double">Double</option>
					<option value="DoubleArray">Double[]</option>
					<option value="Encrypted">Encrypted</option>
					<option value="Enum">Enum</option>
					<option value="EnumArray">Enum[]</option>
					<option value="Function" data-indexed="false">Function</option>
					<option value="Integer">Integer</option>
					<option value="IntegerArray">Integer[]</option>
					<option value="Long">Long</option>
					<option value="LongArray">Long[]</option>
					<option value="String">String</option>
					<option value="StringArray">String[]</option>
					<option value="Thumbnail">Thumbnail</option>
					<option value="ZonedDateTime">ZonedDateTime</option>
					
					<option value="Count">Count</option>
					<option value="Notion">Notion</option>
					<option value="Join">Join</option>
					<option value="IdNotion" data-protected="true" disabled>IdNotion</option>
					<option value="Custom" data-protected="true" disabled>Custom</option>
					<option value="Password" data-protected="true" disabled>Password</option>
				</select>
			`,
			functionProperty: {
				readFunctionWrapJS: (config) => `
					<label data-comment="This configures how the read function script (if it is JavaScript) is interpreted. If set to false, the script itself is not wrapped in a main() function and thus import statements can be used. The return value of the script is the last evaluated instruction, just like in a REPL.">
						<input type="checkbox" id="property-readfunction-wrap" data-property="readFunctionWrapJS" ${config.entity.readFunctionWrapJS ? 'checked' : ''}> Wrap JS read function in main()
					</label>
				`,
				writeFunctionWrapJS: (config) => `
					<label data-comment="This configures how the write function script (if it is JavaScript) is interpreted. If set to false, the script itself is not wrapped in a main() function and thus import statements can be used.">
						<input type="checkbox" id="property-writefunction-wrap" data-property="writeFunctionWrapJS" ${config.entity.writeFunctionWrapJS ? 'checked' : ''}> Wrap JS write function in main()
					</label>
				`
			}
		}
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

			let gridConfig = {
				class: 'related-attrs schema-props grid',
				style: 'grid-template-columns: [ name ] minmax(0, 1fr) [ arrow ] minmax(0, 1fr) [ actions ] fit-content(10%);',
				cols: [
					{ class: 'text-center font-bold pb-2', title: 'JSON Name' },
					{ class: 'text-center font-bold', title: 'Type, direction and related type' },
					{ class: 'actions-col text-center font-bold', title: 'Action' }
				]
			};

			let grid = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.schemaGrid(gridConfig));
			let gridBody = grid.querySelector('.schema-grid-body');

			if (entity.relatedTo.length === 0 && entity.relatedFrom.length === 0) {

				gridBody.insertAdjacentHTML('beforeend', '<div style="grid-column: span 3;" class="no-rels">Type has no relationships...</div>');
				grid.querySelector('.schema-grid-footer').classList.add('hidden');

			} else {

				for (let target of entity.relatedTo) {
					_Schema.remoteProperties.appendRemoteProperty(gridBody, target, true, editSchemaObjectLinkHandler);
				}

				for (let source of entity.relatedFrom) {
					_Schema.remoteProperties.appendRemoteProperty(gridBody, source, false, editSchemaObjectLinkHandler);
				}
			}

			let resetFunction = () => {
				for (let discardIcon of grid.querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
			};

			grid.querySelector('.discard-all').addEventListener('click', resetFunction);

			grid.querySelector('.save-all').addEventListener('click', () => {
				_Schema.remoteProperties.bulkSave(container, gridBody, entity, editSchemaObjectLinkHandler, optionalAfterSaveCallback);
			});

			container.appendChild(grid);

			_Schema.bulkDialogsGeneral.gridChanged(grid);

			return {
				getBulkInfo: (doValidate) => {
					return _Schema.remoteProperties.getDataFromGrid(gridBody, entity, doValidate);
				},
				reset: resetFunction
			};
		},
		getDataFromGrid: (gridBody, entity, doValidate = true) => {

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

			for (let gridRow of gridBody.querySelectorAll('div.schema-grid-row')) {

				let relId            = gridRow.dataset['relationshipId'];
				let propertyName     = gridRow.dataset['propertyName'];
				let targetCollection = gridRow.dataset['targetCollection'];

				if (relId) {

					let info = {
						id: relId
					};

					info[propertyName] = gridRow.querySelector('.property-name').value;
					if (info[propertyName] === '') {
						info[propertyName] = null;
					}

					if (gridRow.classList.contains('has-changes')) {

						if (doValidate) {
							allow = _Schema.remoteProperties.validate(gridRow) && allow;
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
		bulkSave: (el, gridBody, entity, editSchemaObjectLinkHandler, optionalAfterSaveCallback) => {

			let { allow, counts, data } = _Schema.remoteProperties.getDataFromGrid(gridBody, entity);

			if (allow) {

				fetch(Structr.rootUrl + entity.id, {
					method: 'PUT',
					body: JSON.stringify(data)
				}).then((response) => {

					if (response.ok) {

						Command.get(entity.id, null, (reloadedEntity) => {

							_Helpers.fastRemoveElement(el);

							_Schema.remoteProperties.appendRemote(el, reloadedEntity, editSchemaObjectLinkHandler);

							if (optionalAfterSaveCallback) {
								optionalAfterSaveCallback();
							}
						});

					} else {

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

			// related node ID can be null for relationships between dynamic and static classes
			if (!relatedNodeId) {
				return;
			}

			let renderRemoteProperty = (tplConfig) => {

				let gridRow = _Helpers.createSingleDOMElementFromHTML(_Schema.remoteProperties.templates.remoteProperty(tplConfig));
				el.appendChild(gridRow);

				gridRow.querySelector('.property-name').addEventListener('keyup', () => {
					_Schema.remoteProperties.rowChanged(gridRow, attributeName);
				});

				gridRow.querySelector('.reset-action').addEventListener('click', () => {
					gridRow.querySelector('.property-name').value = '';
					_Schema.remoteProperties.rowChanged(gridRow, attributeName);
				});

				gridRow.querySelector('.discard-changes').addEventListener('click', () => {
					$('.property-name', gridRow).val(attributeName);
					_Schema.remoteProperties.rowChanged(gridRow, attributeName);
				});

				if (Structr.isModuleActive(_Schema)) {

					for (let otherSchemaTypeLink of gridRow.querySelectorAll('.edit-schema-object')) {

						otherSchemaTypeLink.addEventListener('click', async (e) => {
							e.stopPropagation();

							editSchemaObjectLinkHandler(e.target);

							return false;
						});
					}

				} else {

					gridRow.querySelector('.edit-schema-object').classList.remove('edit-schema-object');
				}
			};

			let tplConfig = {
				rel: rel,
				relType: relType,
				propertyName: (out ? 'targetJsonName' : 'sourceJsonName'),
				targetCollection: (out ? 'relatedTo' : 'relatedFrom'),
				attributeName: attributeName,
				leftArrow: (out ? '' : '<span style="margin-right: -1px;">&#9668;</span>'),
				rightArrow: (out ? '<span style="margin-left: -1px;">&#9658;</span>' : ''),
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
		rowChanged: (gridRow, originalName) => {

			let nameInUI   = gridRow.querySelector('.property-name').value;
			let hasChanges = (nameInUI !== originalName);

			_Schema.markElementAsChanged(gridRow, hasChanges);

			_Schema.bulkDialogsGeneral.gridChanged(gridRow.closest('.schema-grid'));
		},
		validate: (gridRow) => {
			return true;
		},
		templates: {
			remoteProperty: config => `
				<div class="schema-grid-row contents" data-relationship-id="${config.rel.id}" data-property-name="${config.propertyName}" data-target-collection="${config.targetCollection}">
					<div class="px-1 py-2">
						<input type="text" class="property-name related" value="${config.attributeName}">
					</div>
					<div class="flex items-center whitespace-nowrap">
						${config.leftArrow}
						<i class="cardinality ${config.cardinalityClassLeft}"></i>&mdash;&ndash;
						<span class="edit-schema-object font-medium cursor-pointer truncate break-on-hover squarebrackets relative px-2 py-1" data-object-id="${config.rel.id}">
							<span class="pointer-events-none">${config.relType}</span>
						</span>
						<i class="cardinality ${config.cardinalityClassRight}"></i>&mdash;&ndash;
						${config.rightArrow}
						<span class="edit-schema-object font-medium cursor-pointer truncate break-on-hover ml-2 py-1" data-object-id="${config.relatedNodeId}">
							<span class="pointer-events-none">${config.relatedNodeType}</span>
						</span>
					</div>
					<div class="flex items-center justify-center gap-1 px-2">
						${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16,  _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']), 'Discard changes')}
						${_Icons.getSvgIcon(_Icons.iconResetArrow, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-green', 'reset-action']), 'Reset to default')}
					</div>
				</div>
			`,
		}
	},
	views: {
		initialViewConfig: {},
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

			let gridConfig = {
				class: 'views schema-props grid',
				style: 'grid-template-columns: [ name ] 20% [ attributes ] minmax(0, 2fr) [ actions ] 8%',
				cols: [
					{ class: 'text-center font-bold pb-2', title: 'JSON Name' },
					{ class: 'text-center font-bold', title: 'Properties' },
					{ class: 'actions-col text-center font-bold', title: 'Action' }
				],
				buttons: _Schema.templates.basicAddButton({ addButtonText: 'Add view' })
			};

			let grid = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.schemaGrid(gridConfig));
			let gridBody   = grid.querySelector('.schema-grid-body');
			container.appendChild(grid);

			_Helpers.sort(entity.schemaViews);

			for (let view of entity.schemaViews) {
				_Schema.views.appendView(gridBody, view, entity);
			}

			let resetFunction = () => {
				for (let discardIcon of gridBody.querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
			};
			grid.querySelector('.discard-all').addEventListener('click', resetFunction);

			grid.querySelector('.save-all').addEventListener('click', () => {
				_Schema.views.bulkSave(container, gridBody, entity, optionalAfterSaveCallback);
			});

			container.querySelector('button.add-button').addEventListener('click', () => {

				let gridRow = _Helpers.createSingleDOMElementFromHTML(_Schema.views.templates.viewNew());
				gridBody.appendChild(gridRow);

				_Schema.views.appendViewSelectionElement(gridRow, { name: 'new' }, entity, (selectElement) => {

					selectElement.select2Sortable();

					_Schema.bulkDialogsGeneral.gridChanged(grid);
				});

				gridRow.querySelector('.discard-changes').addEventListener('click', () => {
					_Helpers.fastRemoveElement(gridRow);
					_Schema.bulkDialogsGeneral.gridChanged(grid);
				});
			});

			_Schema.bulkDialogsGeneral.gridChanged(grid);

			return {
				getBulkInfo: (doValidate) => {
					return _Schema.views.getDataFromGrid(gridBody, entity, doValidate);
				},
				reset: resetFunction
			};
		},
		getDataFromGrid: (gridBody, entity, doValidate = true) => {

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

			for (let gridRow of gridBody.querySelectorAll('.schema-grid-row')) {

				let viewId = gridRow.dataset['viewId'];
				let view   = _Schema.views.getDataFromRow(gridRow, entity);

				if (viewId) {

					if (gridRow.classList.contains('to-delete')) {

						// do not add this property to the list
						counts.delete++;

					} else if (gridRow.classList.contains('has-changes')) {

						// changed lines
						counts.update++;
						view.id = viewId;
						if (doValidate) {
							allow = _Schema.views.validateViewRow(gridRow) && allow;
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
						allow = _Schema.views.validateViewRow(gridRow) && allow;
					}
					data.schemaViews.push(view);
				}
			}

			return { name, data, allow, counts };
		},
		bulkSave: (el, tbody, entity, optionalAfterSaveCallback) => {

			let { data, allow, counts } = _Schema.views.getDataFromGrid(tbody, entity);

			if (allow) {

				fetch(Structr.rootUrl + entity.id, {
					method: 'PUT',
					body: JSON.stringify(data)
				}).then((response) => {

					if (response.ok) {

						Command.get(entity.id, null, (reloadedEntity) => {
							_Helpers.fastRemoveAllChildren(el);
							_Schema.views.appendViews(el, reloadedEntity, optionalAfterSaveCallback);

							if (optionalAfterSaveCallback) {
								optionalAfterSaveCallback();
							}
						});

					} else {

						response.json().then((data) => {
							Structr.errorFromResponse(data, undefined, {requiresConfirmation: true});
						});
					}
				});
			}
		},
		appendView: (gridBody, view, entity) => {

			let gridRow = _Helpers.createSingleDOMElementFromHTML(_Schema.views.templates.view({ view: view, type: entity }));
			gridBody.appendChild(gridRow);

			_Schema.views.appendViewSelectionElement(gridRow, view, entity, (selectElement) => {

				// store initial configuration for later comparison
				let initialViewConfig = _Schema.views.getDataFromRow(gridRow, entity);

				// store initial configuration for each view to be able to determine excluded properties later
				_Schema.views.initialViewConfig[view.id] = initialViewConfig;

				selectElement.select2Sortable(() => {
					_Schema.views.rowChanged(gridRow, entity, initialViewConfig);
				});

				_Schema.views.bindRowEvents(gridRow, entity, view, initialViewConfig);
			});
		},
		bindRowEvents: (gridRow, entity, view, initialViewConfig) => {

			let viewInfoChangeHandler = () => { _Schema.views.rowChanged(gridRow, entity, initialViewConfig); };

			let nameInput    = gridRow.querySelector('.view.property-name');
			let nameDisabled = _Schema.views.isViewNameChangeForbidden(view);

			if (nameDisabled) {
				nameInput.disabled = true;
				nameInput.classList.add('cursor-not-allowed');
			} else {
				nameInput.addEventListener('input', viewInfoChangeHandler);
			}

			// jquery is required for change handler of select2 plugin
			$(gridRow.querySelector('.view.property-attrs')).on('change', viewInfoChangeHandler);

			gridRow.querySelector('.discard-changes')?.addEventListener('click', () => {

				gridRow.querySelector('.view.property-name').value = view.name;

				let select = gridRow.querySelector('select');
				Command.listSchemaProperties(entity.id, view.name, (data) => {

					for (let prop of data) {
						let option = select.querySelector(`option[value="${prop.name}"]`);
						if (option) {
							option.selected = prop.isSelected;
						}
					}

					select.dispatchEvent(new CustomEvent('change'));

					gridRow.classList.remove('to-delete');
					gridRow.classList.remove('has-changes');

					_Schema.bulkDialogsGeneral.gridChanged(gridRow.closest('.schema-grid'));
				});
			});

			let removeAction = gridRow.querySelector('.remove-action');
			if (removeAction) {
				removeAction.addEventListener('click', () => {

					gridRow.classList.add('to-delete');
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
		appendViewSelectionElement: (gridRow, view, schemaEntity, callback) => {

			let viewIsEditable = _Schema.views.isViewEditable(view);
			let viewSelectElem = gridRow.querySelector('.property-attrs');

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
					closeOnSelect: true,
					scrollAfterSelect: false,
					dropdownParent: dropdownParent,
					closeOnSelect: false
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
		getDataFromRow: (gridRow, schemaNodeEntity) => {

			const sortedAttrs = $(gridRow.querySelector('.view.property-attrs')).sortedValues();

			return {
				name:               gridRow.querySelector('.view.property-name').value,
				schemaProperties:   _Schema.views.findSchemaPropertiesByNodeAndName(schemaNodeEntity, sortedAttrs),
				nonGraphProperties: _Schema.views.findNonGraphProperties(schemaNodeEntity, sortedAttrs),
				sortOrder:          sortedAttrs.join(',')
			};
		},
		rowChanged: (gridRow, entity, initialViewConfig) => {

			let viewInfoInUI = _Schema.views.getDataFromRow(gridRow, entity);
			let hasChanges   = false;

			for (let key in viewInfoInUI) {

				if (key === 'schemaProperties') {

					let origSchemaProps = initialViewConfig[key].map(p => p.id).sort().join(',');
					let uiSchemaProps   = viewInfoInUI[key].map(p => p.id).sort().join(',');

					if (origSchemaProps !== uiSchemaProps) {
						hasChanges = true;
					}

				} else if (viewInfoInUI[key] !== initialViewConfig[key]) {

					hasChanges = true;
				}
			}

			_Schema.markElementAsChanged(gridRow, hasChanges);

			_Schema.bulkDialogsGeneral.gridChanged(gridRow.closest('.schema-grid'));
		},
		validateViewRow: (gridRow) => {

			let valid = true;

			let nameField = gridRow.querySelector('.property-name');
			if (nameField.value.length === 0) {
				_Helpers.blinkRed(nameField.closest('.name-col'));
				valid = false;
			}

			let viewPropertiesSelect = gridRow.querySelector('.view.property-attrs');
			if ($(viewPropertiesSelect).sortedValues().length === 0) {
				_Helpers.blinkRed(viewPropertiesSelect.closest('.view-properties-select'));
				valid = false;
			}

			return valid;
		},
		templates: {
			view: config => `
				<div data-view-id="${config.view.id}" class="schema-grid-row contents">
					<div class="p-1 flex items-center name-col">
						<input size="15" type="text" class="view property-name" placeholder="Enter view name" value="${(config.view ? _Helpers.escapeForHtmlAttributes(config.view.name) : '')}">
					</div>
					<div class="view-properties-select">
						<select class="property-attrs view" multiple="multiple" ${config?.propertiesDisabled === true ? 'disabled' : ''}></select>
					</div>
					<div class="actions-col gap-1 flex items-center justify-center">
						${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']), 'Discard changes')}
						${(_Schema.views.isDeleteViewAllowed(config.view) === true) ? _Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16,   _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-action']), 'Delete') : ''}
	
						<a href="${Structr.rootUrl}${config.type.name}/${config.view.name}?${Structr.getRequestParameterName('pageSize')}=1" target="_blank">
							${_Icons.getSvgIcon(_Icons.iconOpenInNewPage, 16, 16, _Icons.getSvgIconClassesNonColorIcon(), 'Preview in new tab (with pageSize=1)')}
						</a>
					</div>
				</div>
			`,
			viewNew: config => `
				<div class="schema-grid-row contents has-changes">
					<div class="p-1 flex items-center name-col">
						<input size="15" type="text" class="view property-name" placeholder="Enter view name">
					</div>
					<div class="view-properties-select">
						<select class="property-attrs view" multiple="multiple"></select>
					</div>
					<div class="actions-col flex items-center justify-center">
						${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16,  _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']), 'Discard changes')}
					</div>
				</div>
			`
		}
	},
	methods: {
		methodsData: {},
		lastEditedMethod: {},
		getLastEditedMethod: (entity) => _Schema.methods.lastEditedMethod[(entity ? entity.id : 'global')],
		setLastEditedMethod: (entity, method) => {
			_Schema.methods.lastEditedMethod[(entity ? entity.id : 'global')] = method;
		},
		appendMethods: (container, entity, methods, optionalAfterSaveCallback) => {

			_Schema.methods.methodsData = {};

			let availableLifecycleMethods = LifecycleMethods.getAvailableLifecycleMethods(entity);

			let methodsGridConfig = {
				class: 'actions schema-props grid',
				style: 'grid-template-columns: [ name ] minmax(0, 1fr) [ more ] 2rem [ actions ] 7.5rem',
				cols: [
					{ class: 'text-center font-bold pb-2', title: 'Name' },
					{ class: 'more-settings-col flex justify-center font-bold', title: 'More' },
					{ class: 'actions-col text-center font-bold', title: 'Action' }
				],
				buttons: _Schema.methods.templates.addMethodsDropdown({ entity, availableLifecycleMethods }) + '<div class="flex-grow flex"></div>'
			};

			_Helpers.sort(methods);

			container.insertAdjacentHTML('beforeend', _Schema.methods.templates.methodsContainer({ class: (entity ? 'entity' : 'global') }));

			let methodsGrid = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.schemaGrid(methodsGridConfig));
			container.querySelector('#methods-grid-container').appendChild(methodsGrid);

			let scrollContainer = container.querySelector('#methods-container-left');

			methodsGrid.addEventListener(Structr.dropdowns.openEventName, () => {

				// scroll container to end to show all options
				scrollContainer.scrollTop = scrollContainer.scrollHeight;
			});

			let gridBody = container.querySelector('.schema-grid-body');

			_Schema.methods.activateUIActions(container, gridBody, entity);

			let lastEditedMethod = _Schema.methods.getLastEditedMethod(entity);

			let rowToActivate = undefined;

			for (let method of methods) {

				let gridRow = _Helpers.createSingleDOMElementFromHTML(_Schema.methods.templates.methodRow({ method }));
				gridBody.appendChild(gridRow);

				_Helpers.activateCommentsInElement(gridRow);

				gridRow.dataset['typeName']   = (entity ? entity.name : 'global_schema_method');
				gridRow.dataset['methodName'] = method.name;

				_Schema.methods.methodsData[method.id] = {
					isNew:           false,
					id:              method.id,
					type:            method.type,
					name:            method.name,
					isStatic:        method.isStatic,
					source:          method.source ?? '',
					codeType:        method.codeType ?? '',
					isPrivate:       method.isPrivate,
					returnRawResult: method.returnRawResult,
					httpVerb:        method.httpVerb,
					wrapJsInMain:    method.wrapJsInMain,
					schemaNode:      entity,
					parameters:      method.parameters,
					initialData: {
						name:            method.name,
						isStatic:        method.isStatic,
						source:          method.source ?? '',
						isPrivate:       method.isPrivate,
						returnRawResult: method.returnRawResult,
						httpVerb:        method.httpVerb,
						wrapJsInMain:    method.wrapJsInMain
					}
				};

				_Schema.methods.bindRowEvents(gridBody, gridRow, entity);

				// auto-edit first method (or last used)
				if ((rowToActivate === undefined) || (lastEditedMethod && ((lastEditedMethod.isNew === false && lastEditedMethod.id === method.id) || (lastEditedMethod.isNew === true && lastEditedMethod.name === method.name)))) {
					rowToActivate = gridRow;
				}
			}

			rowToActivate?.querySelector('.edit-action').dispatchEvent(new Event('click'));

			let resetFunction = () => {
				for (let discardIcon of methodsGrid.querySelectorAll('.discard-changes')) {
					discardIcon.dispatchEvent(new Event('click'));
				}
			};
			methodsGrid.querySelector('.discard-all').addEventListener('click', resetFunction);

			methodsGrid.querySelector('.save-all').addEventListener('click', () => {
				_Schema.methods.bulkSave(container, gridBody, entity, optionalAfterSaveCallback);
			});

			let editorInfo = container.querySelector('#methods-container-right .editor-info');
			_Editors.appendEditorOptionsElement(editorInfo);

			_Schema.bulkDialogsGeneral.gridChanged(methodsGrid);

			return {
				getBulkInfo: (doValidate) => {
					return _Schema.methods.getDataFromGrid(gridBody, entity, doValidate);
				},
				reset: resetFunction
			};
		},
		getDataFromGrid: (gridBody, entity, doValidate = true) => {

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

			for (let gridRow of gridBody.querySelectorAll('.schema-grid-row')) {

				let methodId   = gridRow.dataset['methodId'];
				let methodData = _Schema.methods.methodsData[methodId];

				let baseMethodData = {
					name:            methodData.name,
					isStatic:        methodData.isStatic,
					isPrivate:       methodData.isPrivate,
					returnRawResult: methodData.returnRawResult,
					httpVerb:        methodData.httpVerb,
					source:          methodData.source,
				};

				if (methodData.isNew === false) {

					if (gridRow.classList.contains('to-delete')) {

						counts.delete++;
						data.schemaMethods.push({
							id: methodId,
							deleteMethod: true
						});

					} else if (gridRow.classList.contains('has-changes')) {

						counts.update++;
						if (doValidate) {
							allow = _Schema.methods.validateMethodRow(gridRow) && allow;
						}

						data.schemaMethods.push(Object.assign({ id: methodId }, baseMethodData));

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
						allow = _Schema.methods.validateMethodRow(gridRow) && allow;
					}

					data.schemaMethods.push(Object.assign({ type: 'SchemaMethod' }, baseMethodData));
				}
			}

			if (!entity) {
				// global schema methods are saved differently
				data = data.schemaMethods;
			}

			return { name, data, allow, counts };
		},
		bulkSave: (container, gridBody, entity, optionalAfterSaveCallback) => {

			let { data, allow, counts } = _Schema.methods.getDataFromGrid(gridBody, entity);

			if (allow) {

				let activeMethod = gridBody.querySelector('.schema-grid-row.editing');
				if (activeMethod) {
					_Schema.methods.setLastEditedMethod(entity, _Schema.methods.methodsData[activeMethod.dataset['methodId']]);
				} else {
					_Schema.methods.setLastEditedMethod(entity, undefined);
				}

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

								optionalAfterSaveCallback?.();

							}, 'schema');

						} else {

							_Schema.methods.fetchUserDefinedMethods((methods) => {

								_Helpers.fastRemoveAllChildren(container);

								_Schema.methods.appendMethods(container, null, methods, optionalAfterSaveCallback);

								optionalAfterSaveCallback?.();
							});
						}

					} else {

						response.json().then((data) => {
							Structr.errorFromResponse(data, undefined, { requiresConfirmation: true });
						});
					}
				});
			}
		},
		activateUIActions: (container, gridBody, entity) => {

			let addedMethodsCounter = 1;

			for (let addMethodButton of container.querySelectorAll('.add-method-button')) {

				addMethodButton.addEventListener('click', () => {

					let name             = addMethodButton.dataset['name'] ?? ''
					let isPrefix         = addMethodButton.dataset['isPrefix'] === 'true';
					let baseMethodConfig = {
						name:       (isPrefix ? _Schema.methods.getFirstFreeMethodName(name) : name),
						id:         'new' + (addedMethodsCounter++),
						schemaNode: entity,
						httpVerb:   'POST'
					};

					_Schema.methods.appendNewMethod(gridBody, baseMethodConfig, entity);
				});
			}

			_Helpers.activateCommentsInElement(container, { css: {}, noSpan: true, customToggleIconClasses: ['icon-blue', 'ml-2'] });
		},
		appendNewMethod: (gridBody, method, entity) => {

			let gridRow = _Helpers.createSingleDOMElementFromHTML(_Schema.methods.templates.methodRow({ method: method, isNew: true }));
			gridBody.appendChild(gridRow);

			gridBody.scrollTop = gridRow.offsetTop;

			_Schema.methods.methodsData[method.id] = {
				isNew:           true,
				id:              method.id,
				name:            method.name,
				codeType:        method.codeType ?? '',
				isStatic:        method.isStatic ?? false,
				source:          method.source ?? '',
				isPrivate:       method.isPrivate ?? false,
				returnRawResult: method.returnRawResult ?? false,
				httpVerb:        method.httpVerb ?? 'POST',
				schemaNode:      entity
			};

			_Schema.methods.bindRowEvents(gridBody, gridRow, entity);
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
		removeAllMoreMethodSettingsContainers: (e) => {

			let isInSettingsContainer = (e.target.closest('.more-method-settings-container') !== null);

			if (!isInSettingsContainer) {

				document.removeEventListener('mouseup', _Schema.methods.removeAllMoreMethodSettingsContainers);

				for (let container of document.querySelectorAll('.more-method-settings-container')) {
					container.classList.add('hidden');
				}
			}
		},
		methodChangeFromInitialData: (methodData) => {

			return Object.keys(methodData.initialData).some(key => methodData[key] !== methodData.initialData[key])
		},
		bindRowEvents: (gridBody, gridRow, entity) => {

			let methodId       = gridRow.dataset['methodId'];
			let methodData     = _Schema.methods.methodsData[methodId];
			let isDatabaseNode = (methodData.isNew === false);

			_Schema.methods.updateUIForAllAttributes(gridRow, methodData);

			let propertyNameInput = gridRow.querySelector('.property-name');
			propertyNameInput.addEventListener('input', () => {
				methodData.name = propertyNameInput.value;

				if (isDatabaseNode) {
					let rowChanged = _Schema.methods.methodChangeFromInitialData(methodData);
					_Schema.methods.rowChanged(gridRow, rowChanged);
				}

				_Schema.methods.updateUIForAllAttributes(gridRow, methodData);
			});

			for (let checkbox of gridRow.querySelectorAll('input[type="checkbox"][data-property]')) {

				checkbox.addEventListener('change', (e) => {
					let key = checkbox.dataset['property'];
					methodData[key] = checkbox.checked;

					if (isDatabaseNode) {
						let rowChanged = _Schema.methods.methodChangeFromInitialData(methodData);
						_Schema.methods.rowChanged(gridRow, rowChanged);
					}

					_Schema.methods.updateUIForAllAttributes(gridRow, methodData);
				});
			}

			gridRow.querySelector('select[data-property="httpVerb"]').addEventListener('change', (e) => {
				methodData.httpVerb = e.target.value;

				if (isDatabaseNode) {
					_Schema.methods.rowChanged(gridRow, (methodData.httpVerb !== methodData.initialData.httpVerb));
				}
			});

			gridRow.querySelector('.toggle-more-method-settings').addEventListener('click', (e) => {

				e.stopPropagation();

				let settingsContainer = gridRow.querySelector('.more-method-settings-container');
				let wasHidden         = settingsContainer.classList.contains('hidden');

				if (wasHidden) {
					_Schema.methods.removeAllMoreMethodSettingsContainers(e);
					document.addEventListener('click', _Schema.methods.removeAllMoreMethodSettingsContainers);
				}

				settingsContainer.classList.toggle('hidden');
			});

			gridRow.querySelector('.edit-action').addEventListener('click', () => {
				_Schema.methods.editMethod(gridRow, entity);
			});

			gridRow.querySelector('.run-method-action')?.addEventListener('click', () => {

				if (_Code.persistence.isDirty()) {

					_Dialogs.confirmation.showPromise('There are unsaved changes. Running the method without saving may lead to unexpected behaviour. Save changes?', true).then(answer => {

						if (answer === true) {

							_Code.persistence.runCurrentEntitySaveAction();
						}

						_Schema.methods.runSchemaMethod(methodData);
					});

				} else {

					_Schema.methods.runSchemaMethod(methodData);
				}
			});

			gridRow.querySelector('.clone-action').addEventListener('click', () => {

				let clonedData = Object.assign({}, methodData, {
					id:   methodId + '_clone_' + (new Date().getTime()),
					name: _Schema.methods.getFirstFreeMethodName(methodData.name + '_copy')
				});

				_Schema.methods.appendNewMethod(gridBody, clonedData, entity);
			});

			gridRow.querySelector('.remove-action')?.addEventListener('click', () => {
				gridRow.classList.add('to-delete');
				_Schema.methods.rowChanged(gridRow, true);
			});

			gridRow.querySelector('.discard-changes').addEventListener('click', () => {

				if (isDatabaseNode) {

					if (gridRow.classList.contains('to-delete') || gridRow.classList.contains('has-changes')) {

						gridRow.classList.remove('to-delete');
						gridRow.classList.remove('has-changes');

						methodData = Object.assign(methodData, methodData.initialData);

						gridRow.querySelector('.property-name').value                      = methodData.name;
						gridRow.querySelector('[data-property="isStatic"]').checked        = methodData.isStatic;
						gridRow.querySelector('[data-property="isPrivate"]').checked       = methodData.isPrivate;
						gridRow.querySelector('[data-property="returnRawResult"]').checked = methodData.returnRawResult;
						gridRow.querySelector('[data-property="httpVerb"]').checked        = methodData.httpVerb;

						if (gridRow.classList.contains('editing')) {
							_Editors.disposeEditorModel(methodData.id, 'source');
							_Schema.methods.editMethod(gridRow);
						}

						_Schema.methods.rowChanged(gridRow, false);

						_Schema.methods.updateUIForAllAttributes(gridRow, methodData);
					}

				} else {

					if (gridRow.classList.contains('editing')) {
						document.querySelector('#methods-container-right').style.display = 'none';
					}
					_Helpers.fastRemoveElement(gridRow);

					_Schema.methods.rowChanged(gridBody.closest('.schema-grid'), false);

					_Editors.nukeEditorsById(methodData.id);
				}
			});

			if (!isDatabaseNode) {

				_Schema.bulkDialogsGeneral.gridChanged(gridBody.closest('.schema-grid'));

				_Schema.methods.editMethod(gridRow, entity);
			}
		},
		updateUIForAllAttributes: (container, methodData) => {

			let updateVisibilityForAttribute = (attributeName, canSeeAttr) => {

				let element = container.querySelector(`[data-property="${attributeName}"]`);
				element?.closest('.method-config-element')?.classList.toggle('hidden', (canSeeAttr === false));
			};

			let isTypeMethod         = (!!methodData.schemaNode);
			let isServiceClassMethod = (isTypeMethod && methodData.schemaNode.isServiceClass === true);
			let isLifecycleMethod    = LifecycleMethods.isLifecycleMethod(methodData);
			let isCallableViaREST    = (methodData.isPrivate !== true);
			let isJavaScript         = ('javascript' === _Editors.getMonacoEditorModeForContent(methodData.source, methodData));

			updateVisibilityForAttribute('isStatic',        (!isLifecycleMethod && isTypeMethod && !isServiceClassMethod));
			updateVisibilityForAttribute('isPrivate',       (!isLifecycleMethod));
			updateVisibilityForAttribute('returnRawResult', (!isLifecycleMethod && isCallableViaREST));
			updateVisibilityForAttribute('httpVerb',        (!isLifecycleMethod && isCallableViaREST));
			updateVisibilityForAttribute('wrapJsInMain',    (!isLifecycleMethod && isJavaScript));

			// completely hide 'more' button for lifecycle methods
			container.querySelector('.toggle-more-method-settings')?.classList.toggle('hidden', isLifecycleMethod);

			let isStatic = (methodData.isStatic === true);
			container.querySelector('.run-method-action')?.classList.toggle('hidden', !(isStatic && isCallableViaREST));
		},
		saveAndDisposePreviousEditor: (tr) => {

			let previouslyActiveRow = tr.closest('.schema-grid-body').querySelector('.schema-grid-row.editing');
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

			tr.closest('.schema-grid-body').querySelector('.schema-grid-row.editing')?.classList.remove('editing');
			tr.classList.add('editing');

			let methodId   = tr.dataset['methodId'];
			let methodData = _Schema.methods.methodsData[methodId];

			_Schema.methods.setLastEditedMethod(entity, methodData);

			let sourceMonacoConfig = {
				language: 'auto',
				lint: true,
				autocomplete: true,
				isAutoscriptEnv: true,
				changeFn: (editor, entity) => {

					methodData.source = editor.getValue();

					if (methodData.isNew === false) {

						let changesInInitialData = Object.keys(methodData.initialData).reduce((acc, key) => {
							return acc || (methodData[key] !== methodData.initialData[key]);
						}, false);

						_Schema.methods.rowChanged(tr, changesInInitialData);
					}

					_Schema.methods.updateUIForAllAttributes(tr, methodData);
				}
			};

			let sourceEditor = _Editors.getMonacoEditor(methodData, 'source', document.querySelector('#methods-content .editor'), sourceMonacoConfig);
			_Editors.addEscapeKeyHandlersToPreventPopupClose(entity.id, 'source', sourceEditor);
			_Editors.focusEditor(sourceEditor);

			sourceMonacoConfig.changeFn(sourceEditor);

			_Editors.resizeVisibleEditors();
		},
		showUserDefinedMethods: () => {

			_Schema.methods.fetchUserDefinedMethods((methods) => {

				let { dialogText } = _Dialogs.custom.openDialog('User-defined functions', () => {

					_Schema.currentNodeDialogId = null;

					_Schema.ui.jsPlumbInstance.repaintEverything();

				}, ['schema-edit-dialog', 'global-methods-dialog']);

				dialogText.insertAdjacentHTML('beforeend', '<div class="schema-details"><div id="tabView-methods" class="schema-details"></div></div>');

				_Schema.methods.appendMethods(dialogText.querySelector('#tabView-methods'), null, methods);
			});
		},
		fetchUserDefinedMethods: (callback) => {

			Command.rest(`SchemaMethod/schema?schemaNode=null&${Structr.getRequestParameterName('sort')}=name&${Structr.getRequestParameterName('order')}=ascending`, (methods) => {
				callback(methods);
			});
		},
		rowChanged: (gridRow, hasChanges) => {

			_Schema.markElementAsChanged(gridRow, hasChanges);

			_Schema.bulkDialogsGeneral.gridChanged(gridRow.closest('.schema-grid'));
		},
		validateMethodRow: (gridRow) => {

			let propertyNameInput = gridRow.querySelector('.property-name');
			if (propertyNameInput.value.length === 0) {

				_Helpers.blinkRed(propertyNameInput.closest('.name-col'));
				return false;
			}

			return true;
		},
		getURLForSchemaMethod: (schemaMethod, absolute = true) => {

			let isStatic              = (schemaMethod.isStatic === true);
			let isUserDefinedFunction = (schemaMethod.schemaNode === null);

			let parts = [];

			if (!isUserDefinedFunction) {
				parts.push(schemaMethod.schemaNode.name);

				if (!isStatic) {
					parts.push('<b>{uuid}</b>');
				}
			}

			parts.push(schemaMethod.name);
			let url  = (absolute ? location.origin : '') + Structr.rootUrl + parts.join('/');

			return url;
		},
		runSchemaMethod: (schemaMethod) => {

			let storagePrefix = 'schemaMethodParameters_';
			let name          = (schemaMethod.schemaNode === null) ? schemaMethod.name : schemaMethod.schemaNode.name + '/' + schemaMethod.name;
			let url           = _Schema.methods.getURLForSchemaMethod(schemaMethod);

			let { dialogText } = _Dialogs.custom.openDialog(`Run user-defined function ${name}`);

			let runButton = _Dialogs.custom.prependCustomDialogButton(`
					<button id="run-method" class="flex items-center action focus:border-gray-666 active:border-green">
						${_Icons.getSvgIcon(_Icons.iconRunButton, 16, 18, 'mr-2')}
						<span>Run</span>
					</button>
				`);

			let clearButton = _Dialogs.custom.appendCustomDialogButton('<button id="clear-log" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Clear output</button>');

			window.setTimeout(() => {
				runButton.focus();
			}, 50);

			let paramsOuterBox = _Helpers.createSingleDOMElementFromHTML(`
					<div>
						<div id="params">
							<h3 class="heading-narrow">Parameters</h3>
							<div class="method-parameters">
								${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-green', 'add-param-action']), 'Add parameter')}
							</div>
						</div>
						<h3 class="mt-4">Result</h3>
						<pre id="log-output"></pre>
					</div>
				`);
			dialogText.appendChild(paramsOuterBox);

			_Helpers.appendInfoTextToElement({
				element: paramsOuterBox.querySelector('h3'),
				text: 'Parameters can be accessed in the called method by using the <code>$.methodParameters[name]</code> object (JavaScript-only) or the <code>retrieve(name)</code> function.<br>For methods called via GET, the parameters are sent using the request URL and thus, they can be accessed via the <code>request</code> object',
				css: { marginLeft: "5px" },
				helpElementCss: { fontSize: "12px" }
			});

			let appendParameter = (name = '', value = '', paramDefinition = {}) => {

				let infoSpan = '';

				if (paramDefinition.parameterType || paramDefinition.description || paramDefinition.exampleValue) {

					let infoText = `
						Type: ${_Helpers.escapeForHtmlAttributes(paramDefinition.parameterType ?? '')}<br>
						Description: ${_Helpers.escapeForHtmlAttributes(paramDefinition.description ?? '')}<br>
						Example Value: ${_Helpers.escapeForHtmlAttributes(paramDefinition.exampleValue ?? '')}<br>
					`;

					infoSpan = `<span data-comment="${_Helpers.escapeForHtmlAttributes(infoText)}"></span>`;
				}

				let newParam = _Helpers.createSingleDOMElementFromHTML(`
					<div class="param flex items-center mb-1">
						<input class="param-name" placeholder="Key">
						${infoSpan}
						<span class="px-2">=</span>
						<input class="param-value" placeholder="Value" data-input-type="${(paramDefinition.parameterType ?? 'string').toLowerCase()}">
						${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-action', 'ml-2']), 'Remove parameter')}
					</div>
				`);

				newParam.querySelector('.param-name').value  = name;
				newParam.querySelector('.param-value').value = (typeof value === "string") ? value : JSON.stringify(value);

				newParam.querySelector('.remove-action').addEventListener('click', () => {
					_Helpers.fastRemoveElement(newParam);
				});

				paramsOuterBox.querySelector('.method-parameters').appendChild(newParam);
			};

			let lastParams = LSWrapper.getItem(storagePrefix + url, {});

			if (Object.keys(lastParams).length > 0) {

				let paramDefinitions = Object.fromEntries((schemaMethod.parameters ?? []).map(p => [p.name, p]));

				for (let [k,v] of Object.entries(lastParams)) {
					appendParameter(k, v, paramDefinitions[k]);
				}

			} else {

				for (let paramDefinition of (schemaMethod.parameters ?? [])) {
					appendParameter(paramDefinition.name, '', paramDefinition);
				}
			}

			_Helpers.activateCommentsInElement(paramsOuterBox);

			paramsOuterBox.querySelector('.add-param-action').addEventListener('click', () => {
				appendParameter();
			});

			let logOutput = paramsOuterBox.querySelector('#log-output');

			runButton.addEventListener('click', async () => {

				logOutput.textContent = 'Running method...';

				let params = {};
				for (let paramRow of paramsOuterBox.querySelectorAll('#params .param')) {

					let name = paramRow.querySelector('.param-name').value;
					if (name) {

						let valueInput = paramRow.querySelector('.param-value');
						let value = valueInput.value;

						// if the value type is not a basic string, try to parse it as JSON (but fail gracefully)
						// if this ever creates problems, we should rather add a dropdown "Parameter Type" and
						// populate it with "String" by default and also take the OpenAPI parameter definition into account
						if (valueInput.dataset['inputType'] !== 'string') {
							try {
								value = JSON.parse(value);
							} catch(e) {}
						}

						params[name] = value;
					}
				}

				LSWrapper.setItem(storagePrefix + url, params);

				let methodCallUrl = url;
				let fetchConfig = {
					method: schemaMethod.httpVerb
				};

				if (schemaMethod.httpVerb === 'GET') {

					methodCallUrl += '?' + new URLSearchParams(params).toString();

				} else {

					fetchConfig.body = JSON.stringify(params);
				}

				let response = await fetch(methodCallUrl, fetchConfig);
				logOutput.textContent = await response.text();
			});

			clearButton.addEventListener('click', () => {
				logOutput.textContent = '';
			});
		},
		templates: {
			methodsContainer: config => `
				<div id="methods-container" class="${config.class} h-full flex">
					<div id="methods-container-left">
						<div id="methods-grid-container" class="h-full">
						</div>
					</div>
	
					<div id="methods-container-right" class="flex flex-col flex-grow">
						<div id="methods-content" class="flex-grow">
							<div class="editor h-full">
							</div>
						</div>
						<div class="editor-info"></div>
					</div>
				</div>
			`,
			methodRow: config => `
				<div class="schema-grid-row contents ${(config.isNew ? ' has-changes' : '')}" data-method-id="${config.method.id}">
					<div class="name-col px-1 py-2">
						<input size="15" type="text" class="action property-name" placeholder="Enter method name" value="${config.method.name}">
					</div>
					<div class="more-settings-col flex items-center justify-center relative">

						${_Icons.getSvgIcon(_Icons.iconKebabMenu, 16, 16, _Icons.getSvgIconClassesNonColorIcon(['toggle-more-method-settings']), 'More settings')}

						<div class="more-method-settings-container hidden absolute" style="z-index: 1; top: calc(100% - 1rem); right: 0; ">
							<div class="bg-white border border-gray-ddd flex flex-shrink flex-wrap px-4 py-2 rounded-sm">
								${_Schema.methods.templates.methodFlags(Object.assign({ cols: 1 }, config))}
							</div>
						</div>

					</div>
					<div class="flex items-center justify-center gap-1">
						${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, _Icons.getSvgIconClassesNonColorIcon(['edit-action']), 'Edit')}
						${Structr.isModuleActive(_Code) ? _Icons.getSvgIcon(_Icons.iconRunButton, 16, 16, _Icons.getSvgIconClassesNonColorIcon(['run-method-action']), 'Open run dialog') : ''}
						${_Icons.getSvgIcon(_Icons.iconClone, 16, 16, _Icons.getSvgIconClassesNonColorIcon(['clone-action']), 'Clone')}
						${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16,  _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']), 'Discard changes')}
						${config.isNew ? '' : _Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-action']), 'Discard')}
					</div>
				</div>
			`,
			methodFlags: config => `
				<div class="grid grid grid-cols-${config.cols ?? 2} gap-x-2">

					<div>
						<div class="method-config-element entity-method py-1">
							<label class="block whitespace-nowrap" data-comment="Only needs to be set if the method should be callable statically (without an object context). Only applies to non-lifecycle type methods.">
								<input type="checkbox" data-property="isStatic" ${config.method.isStatic ? 'checked' : ''}> Method is static
							</label>
						</div>

						<div class="method-config-element entity-method py-1">
							<label class="block whitespace-nowrap" data-comment="If this flag is set, this method can <strong>not be called via HTTP</strong>.<br>Lifecycle methods can never be called via HTTP.">
								<input type="checkbox" data-property="isPrivate" ${config.method.isPrivate ? 'checked' : ''}> Not callable via HTTP
							</label>
						</div>

						<div class="method-config-element entity-method py-1">
							<label class="block whitespace-nowrap" data-comment="This configures how the script (if it is JavaScript) is interpreted. If set to true, the script itself is wrapped in a main() function to enable the user to use the 'return' keyword at the end to return a value. However, that prevents the user from making use of imports. If this switch is set to false, the script is not wrapped in a main() function and thus import statements can be used. The return value of the script is the last evaluated instruction, just like in a REPL.">
								<input type="checkbox" data-property="wrapJsInMain" ${config.method.wrapJsInMain ? 'checked' : ''}> Wrap JavaScript in main()
							</label>
						</div>
					</div>

					<div>
						<div class="method-config-element entity-method py-1">
							<label class="block whitespace-nowrap" data-comment="If this flag is set, the request response value returned by this method will NOT be wrapped in a result object. Only applies to HTTP calls to this method.">
								<input type="checkbox" data-property="returnRawResult" ${config.method.returnRawResult ? 'checked' : ''}> Return result object only
							</label>
						</div>

						<div class="method-config-element entity-method py-1">
							<select data-property="httpVerb">
								<option value="GET" ${config.method.httpVerb === 'GET' ? 'selected' : ''}>Call method via GET</option>
								<option value="PUT" ${config.method.httpVerb === 'PUT' ? 'selected' : ''}>Call method via PUT</option>
								<option value="POST" ${config.method.httpVerb === 'POST' ? 'selected' : ''}>Call method via POST</option>
								<option value="PATCH" ${config.method.httpVerb === 'PATCH' ? 'selected' : ''}>Call method via PATCH</option>
								<option value="DELETE" ${config.method.httpVerb === 'DELETE' ? 'selected' : ''}>Call method via DELETE</option>
							</select>
						</div>
					</div>
				</div>
			`,
			addMethodsDropdown: config => `
				<div class="dropdown-menu darker-shadow-dropdown dropdown-menu-large relative">
					<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green" data-wants-fixed="false">
						${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['icon-green', 'mr-2'])}
					</button>
					<div class="dropdown-menu-container ml-px w-64">
						<div class="flex flex-col divide-x-0 divide-y">
							<a data-prefix="" class="add-method-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer p-4">
								${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'icon-green mr-2')} Add method
							</a>
							${config.availableLifecycleMethods.map(m => _Schema.methods.templates.methodsDropdownButton(m)).join('')}
						</div>
					</div>
				</div>
			`,
			methodsDropdownButton: (config) => `
				<a data-name="${config.name}" data-is-prefix="${config.isPrefix}" class="add-method-button inline-flex items-center gap-x-2 justify-between hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer p-4 border-0 border-t border-gray-ddd border-solid" data-comment="${_Helpers.escapeForHtmlAttributes(config.comment)}" data-comment-config='{ "customToggleIconClasses": ["flex-shrink-0"] }'>
					<span class="inline-flex items-center">${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'icon-green mr-2 flex-shrink-0')} Add ${config.name}</span>
				</a>
			`
		}
	},
	schemaGrants: {
		appendSchemaGrants: (container, entity) => {

			let gridConfig = {
				class: 'schema-props grid',
				style: 'grid-template-columns: [ group ] minmax(0, 1fr) [ read ] minmax(15%, max-content) [ write ] minmax(15%, max-content) [ delete ] minmax(15%, max-content) [ accessControl ] minmax(15%, max-content) [ actions ] fit-content(10%);',
				cols: [
					{ class: 'pb-2 px-1 font-bold flex justify-center', title: 'Group' },
					{ class: 'pb-2 px-1 font-bold flex justify-center', title: 'Read' },
					{ class: 'pb-2 px-1 font-bold flex justify-center', title: 'Write' },
					{ class: 'pb-2 px-1 font-bold flex justify-center', title: 'Delete' },
					{ class: 'pb-2 px-1 font-bold flex justify-center', title: 'Access-Control' },
					{ class: 'pb-2 px-1 font-bold flex justify-center', title: 'Action' }
				]
			};

			let gridMarkup            = _Schema.templates.schemaGrid(gridConfig);
			let schemaGrantsContainer = _Helpers.createSingleDOMElementFromHTML(_Schema.schemaGrants.templates.schemaGrantsTabContent({ gridMarkup }));
			let gridBody              = schemaGrantsContainer.querySelector('.schema-grid-body');
			container.appendChild(schemaGrantsContainer);

			let schemaGrantsChange = (checkbox, rowConfig) => {

				let property        = checkbox.dataset['property'];
				let gridRow         = checkbox.closest('.schema-grid-row');
				let checkboxChanged = (rowConfig[property] !== checkbox.checked);

				checkbox.classList[checkboxChanged ? 'add' : 'remove']('changed');

				let anyChangeInRow  = (gridRow.querySelectorAll('.changed').length > 0);
				_Schema.markElementAsChanged(gridRow, anyChangeInRow);

				_Schema.bulkDialogsGeneral.gridChanged(gridBody.closest('.schema-grid'));
			};

			let getGrantData = () => {

				let grantData = [];

				for (let gridRow of gridBody.querySelectorAll('.schema-grid-row')) {

					let rowConfig = {
						principal:          gridRow.dataset['groupId'],
						schemaNode:         entity.id,
						allowRead:          gridRow.querySelector('input[data-property=allowRead]').checked,
						allowWrite:         gridRow.querySelector('input[data-property=allowWrite]').checked,
						allowDelete:        gridRow.querySelector('input[data-property=allowDelete]').checked,
						allowAccessControl: gridRow.querySelector('input[data-property=allowAccessControl]').checked
					};

					let grantId = gridRow.dataset['grantId'];
					if (grantId) {
						rowConfig.id = grantId;
					}

					let isChanged         = (gridRow.querySelectorAll('.changed').length > 0);
					let atLeastOneChecked = (gridRow.querySelectorAll('input:checked').length > 0);

					if (isChanged || atLeastOneChecked) {
						grantData.push(rowConfig);
					}
				}

				return grantData;
			};

			let grants = Object.fromEntries(entity.schemaGrants.map(grant => [ grant.principal.id, grant ]));

			let getGrantConfigForGroup = (group) => {
				return {
					groupId            : group.id,
					name               : group.name,
					grantId            : (!grants[group.id]) ? '' : grants[group.id].id,
					allowRead          : (!grants[group.id]) ? false : grants[group.id].allowRead,
					allowWrite         : (!grants[group.id]) ? false : grants[group.id].allowWrite,
					allowDelete        : (!grants[group.id]) ? false : grants[group.id].allowDelete,
					allowAccessControl : (!grants[group.id]) ? false : grants[group.id].allowAccessControl
				};
			};

			let resetGridRow = (gridRow) => {

				let tplConfig = getGrantConfigForGroup({
					id: gridRow.dataset['groupId']
				});

				for (let checkbox of gridRow.querySelectorAll('input[data-property]')) {
					checkbox.checked = tplConfig[checkbox.dataset.property];

					checkbox.classList.remove('changed');
				}

				gridRow.classList.remove('has-changes');

				_Schema.bulkDialogsGeneral.gridChanged(gridBody.closest('.schema-grid'));
			};

			Command.query('Group', 1000, 1, 'name', 'asc', {}, groupResult => {

				for (let group of groupResult) {

					let tplConfig = getGrantConfigForGroup(group);

					let gridRow = _Helpers.createSingleDOMElementFromHTML(_Schema.schemaGrants.templates.schemaGrantRow(tplConfig));
					gridBody.appendChild(gridRow);

					for (let checkbox of gridRow.querySelectorAll('input')) {

						checkbox.addEventListener('change', (e) => {
							schemaGrantsChange(checkbox, tplConfig);
						});
					}

					gridRow.querySelector('.discard-changes').addEventListener('click', () => {
						resetGridRow(gridRow);
					});
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

					for (let gridRow of schemaGrantsContainer.querySelectorAll('.schema-grid-body .schema-grid-row')) {
						resetGridRow(gridRow);
					}
				}
			};
		},
		templates: {
			schemaGrantsTabContent: config => `
				<div>

					<h3 class="mt-8">Permissions</h3>

					<div class="relative">
						<div class="inline-info">
							<div class="inline-info-icon">
								${_Icons.getSvgIcon(_Icons.iconInfo, 24, 24)}
							</div>
							<div class="inline-info-text">
								To define group permissions for access to <strong>all nodes of this type</strong>, simply check the corresponding boxes and save the changes.
							</div>
						</div>
		
						<div style="width: calc(100% - 4rem);" class="pt-4">
							${config.gridMarkup}
						</div>
		
					</div>
				</div>
			`,
			schemaGrantRow: config => `
				<div class="schema-grid-row contents" data-group-id="${config.groupId}" data-grant-id="${config.grantId}">
					<div class="py-3 px-2">${config.name}</div>
					<div class="flex items-center justify-center">
						<input type="checkbox" data-property="allowRead" ${(config.allowRead ? 'checked="checked"' : '')} style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						<input type="checkbox" data-property="allowWrite" ${(config.allowWrite ? 'checked="checked"' : '')} style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						<input type="checkbox" data-property="allowDelete" ${(config.allowDelete ? 'checked="checked"' : '')} style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						<input type="checkbox" data-property="allowAccessControl" ${(config.allowAccessControl ? 'checked="checked"' : '')} style="margin-right: 0;">
					</div>
					<div class="flex items-center justify-center">
						${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16,  _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'discard-changes']), 'Discard changes')}
					</div>
				</div>
			`,
		}
	},
	resize: () => {},
	dialogSizeChanged: () => {
		_Editors.resizeVisibleEditors();
	},
	deleteNode: async (id) => {

		let response = await fetch(`${Structr.rootUrl}SchemaNode/${id}`, {
			method: 'DELETE'
		});
		let data = await response.json();

		if (response.ok) {

			_Schema.reload();

		} else {

			Structr.errorFromResponse(data);
		}
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

		document.querySelector('#clear-schema').addEventListener('click', async (e) => {

			let confirm = await _Dialogs.confirmation.showPromise('<h3>Delete dynamic schema?</h3><p>This will remove all dynamic schema information, but leave the corresponding data intact.</p><p>&nbsp;</p>');

			if (confirm === true) {

				_Schema.showUpdatingSchemaMessage();

				Command.clearSchema((data) => {

					if (data.success !== true) {
						new WarningMessage().text("Clearing schema failed - see log for details.").show();
					}

					let timeoutBecauseClearCommandReturnsTooEarly = 1000;

					setTimeout(() => {
						_Schema.hideUpdatingSchemaMessage();
						_Schema.reload();
					}, timeoutBecauseClearCommandReturnsTooEarly);
				});
			}
		});

		let nodeTypeSelector = $('#node-type-selector');
		Command.list('SchemaNode', true, 1000, 1, 'name', 'asc', 'id,name,isServiceClass', (nodes) => {
			nodeTypeSelector.append(nodes.filter(n => !n.isServiceClass).map(node => `<option>${node.name}</option>`).join(''));
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
			_Schema.ui.updateInheritanceArrowsVisibility(e.target.checked);
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

			let layoutSelector        = document.querySelector('#saved-layout-selector');
			let layoutNameInput       = document.querySelector('#layout-name');
			let createNewLayoutButton = document.querySelector('#create-new-layout');
			let saveLayoutButton      = document.querySelector('#save-layout');
			let loadLayoutButton      = document.querySelector('#load-layout');
			let deleteLayoutButton    = document.querySelector('#delete-layout');

			let layoutSelectorChangeHandler = () => {

				let selectedOption = layoutSelector.querySelector(':checked:not(:disabled)');

				if (!selectedOption) {

					_Helpers.disableElements(true, saveLayoutButton, loadLayoutButton, deleteLayoutButton);

				} else {

					_Helpers.enableElement(loadLayoutButton);

					let optGroup    = selectedOption.closest('optgroup');
					let username    = optGroup.label;
					let isOwnerless = (optGroup.dataset['ownerless'] === 'true');

					_Helpers.disableElements(!(isOwnerless || username === StructrWS.me.username), saveLayoutButton, deleteLayoutButton);
				}
			};
			layoutSelectorChangeHandler();

			layoutSelector.addEventListener('change', layoutSelectorChangeHandler);

			saveLayoutButton.addEventListener('click', async () => {

				let selectedLayout = layoutSelector.value;
				let selectedOption = layoutSelector.querySelector(':checked:not(:disabled)');

				let confirm = await _Dialogs.confirmation.showPromise(`
					<h3>Overwrite stored schema layout "${selectedOption.text}"?</h3>
				`);

				if (confirm === true) {

					Command.setProperty(selectedLayout, 'content', JSON.stringify(_Schema.getSchemaLayoutConfiguration()), false, (data) => {

						if (!data.error) {

							new SuccessMessage().text("Layout saved").show();

							_Helpers.blinkGreen(layoutSelector);

						} else {

							new ErrorMessage().title(data.error).text(data.message).show();
						}
					});
				}
			});

			loadLayoutButton.addEventListener('click', () => {
				_Schema.restoreLayout(layoutSelector.value);
			});

			deleteLayoutButton.addEventListener('click', async () => {

				let selectedLayout = layoutSelector.value;
				let selectedOption = layoutSelector.querySelector(':checked:not(:disabled)');

				let confirm = await _Dialogs.confirmation.showPromise(`
					<h3>Delete stored schema layout "${selectedOption.text}"?</h3>
				`);

				if (confirm === true) {

					Command.deleteNode(selectedLayout, false, async () => {
						await _Schema.updateGroupedLayoutSelector(layoutSelector);
						layoutSelectorChangeHandler();
						_Helpers.blinkGreen(layoutSelector);
					});
				}
			});

			createNewLayoutButton.addEventListener('click', () => {

				let layoutName = layoutNameInput.value;

				if (layoutName && layoutName.length) {

					Command.createApplicationConfigurationDataNode('layout', layoutName, JSON.stringify(_Schema.getSchemaLayoutConfiguration()), async (data) => {

						if (!data.error) {

							new SuccessMessage().text("Layout saved").show();

							await _Schema.updateGroupedLayoutSelector(layoutSelector);
							layoutSelectorChangeHandler();
							layoutNameInput.value = '';

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

		document.getElementById('new-auto-layout').addEventListener('click', (e) => {
			_Schema.newAutoLayout();
			Structr.dropdowns.hideOpenDropdownsExcept();
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

				layoutSelector.innerHTML = html;

				resolve();
			});
		});
	},
	restoreLayout: (selectedLayout) => {

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
						_Schema.ui.panzoomInstance.zoom(_Schema.ui.zoomLevel);
						_Schema.ui.jsPlumbInstance.setZoom(_Schema.ui.zoomLevel);

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

			let type = n.dataset.type;

			if (positions[type]) {
				n.style.top = positions[type].top + 'px';
				n.style.left = positions[type].left + 'px';
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

		let { dialogText } = _Dialogs.custom.openDialog('Schema Type Visibility');

		let contentEl = _Helpers.createSingleDOMElementFromHTML('<div class="code-tabs flex flex-col h-full overflow-hidden"></div>');
		dialogText.appendChild(contentEl);

		let customView = 'id,name,isBuiltinType,category';
		// TODO: Use helper function to fetch all types rather than only schema nodes
		Command.query('SchemaNode', 2000, 1, 'name', 'asc', { isServiceClass: false }, (schemaNodes) => {

			contentEl.insertAdjacentHTML('beforeend', `
				<div class="tab overflow-y-auto">
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
						${schemaNodes.map(schemaNode => {
							let isHidden = (_Schema.hiddenSchemaNodes.indexOf(schemaNode.name) > -1);
							return `<tr><td><input class="toggle-type" data-structr-type="${schemaNode.name}" type="checkbox" ${(isHidden ? '' : 'checked')}></td><td>${schemaNode.name}</td></tr>`;
						}).join('')}
					</table>
				</div>
			`);

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

		}, false, null, customView);

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

		if (_Schema.ui.selection.selectedNodes.length > 0) {

			for (let n of _Schema.ui.selection.selectedNodes) {
				_Schema.hiddenSchemaNodes.push(n.name);
			}

			LSWrapper.setItem(_Schema.hiddenSchemaNodesKey, JSON.stringify(_Schema.hiddenSchemaNodes));
			_Schema.reload();
		}
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
	invalidateTypeInfoCache: (typeName) => {

		delete _Schema.typeInfoCache[typeName];

		// clear type cache for this type and derived types
		_Schema.getDerivedTypeNames(typeName, []).then(derivedTypes => {

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
	getDerivedTypeNames: async (searchTrait, blacklist = []) => {

		let response = await fetch(`${Structr.rootUrl}_schema`);

		if (response.ok) {

			let data                 = await response.json();
			let result               = data.result;
			let typesWithSearchTrait = new Set();

			let repeat = false;
			do {

				let countBefore = typesWithSearchTrait.size;

				for (let type of result) {

					let isSameType         = (type.name === searchTrait);
					let isBlacklisted      = blacklist.includes(type.name);
					let typeHasSearchTrait = (type.traits ?? []).includes(searchTrait);

					if (!isSameType && !isBlacklisted && typeHasSearchTrait) {
						typesWithSearchTrait.add(type.name);
					}
				}

				repeat = (countBefore !== typesWithSearchTrait.size);

			} while (repeat);

			return [...typesWithSearchTrait];

		} else {
			return [];
		}
	},
	ui: {
		canvas: undefined,
		jsPlumbInstance: undefined,
		panzoomInstance: undefined,
		showInheritanceArrows: true,
		showSchemaOverlays: true,
		connectorStyle: undefined,
		zoomLevel: undefined,
		relHighlightColor: 'red',
		maxZ: 0,
		initPanZoom: () => {

			const schemaContainer     = document.getElementById('schema-container');
			let schemaContainerParent = schemaContainer.parentNode;
			const nodeElements        = [...document.querySelectorAll('.jsplumb-draggable, ._jsPlumb_connector')];

			const panzoom = Panzoom(schemaContainer, {
				cursor: 'default',
				canvas: true,
				exclude: nodeElements,
				startScale: _Schema.ui.zoomLevel,
				handleStartEvent: (event) => {

					let shiftPressed       = event.shiftKey;
					let middleMousePressed = (event.button === 1);

					if (shiftPressed || middleMousePressed) {

						panzoom.setOptions({ disablePan: false, cursor: 'move' });
						event.preventDefault();
						event.stopPropagation();

					} else {

						panzoom.setOptions({ disablePan: true, cursor: 'default' });
					}
				}
			});
			_Schema.ui.panzoomInstance = panzoom;

			// TODO: these two listeners are never removed and thus duplicated when re-opening the schema
			document.addEventListener('keydown', (event) => {
				if (event.shiftKey) {
					schemaContainerParent.style.cursor = 'move';
				}
			});
			document.addEventListener('keyup', (event) => {
				if (!event.shiftKey) {
					schemaContainerParent.style.cursor = 'default';
				}
			});

			schemaContainer.addEventListener('panzoomend', (event) => {
				if (!event.detail.originalEvent.shiftKey) {
					schemaContainerParent.style.cursor = 'default';
				}
			});
			schemaContainerParent.addEventListener('wheel', (event) => {
				panzoom.zoomWithWheel(event);
				_Schema.ui.zoomLevel = panzoom.getScale();
				_Schema.ui.jsPlumbInstance.setZoom(_Schema.ui.zoomLevel);

				LSWrapper.setItem(_Schema.schemaZoomLevelKey, _Schema.ui.zoomLevel);
			});
		},
		nodeDrag: {
			init: () => {

				let eventTarget        = _Schema.ui.canvas.parent().parent()[0];
				let prevAnimFrameReqId = undefined;
				let eventStartPoint    = {};
				let nodeStartPositions = {};
				let domElementsToDrag  = [];
				let dragElement        = undefined;

				eventTarget.addEventListener('mousedown', (e) => {

					dragElement = e.target.closest('.node');

					if (dragElement && e.which === 1) {

						e.stopImmediatePropagation();
						e.preventDefault();
						e.stopPropagation();

						domElementsToDrag = _Schema.ui.selection.selectedNodes.map(selectedNode => document.getElementById(selectedNode.nodeId));
						if (!dragElement.classList.contains('selected')) {
							domElementsToDrag.push(dragElement);
						}

						eventStartPoint.x = e.clientX;
						eventStartPoint.y = e.clientY;

						for (let selectedEl of domElementsToDrag) {

							nodeStartPositions[selectedEl.id] = {
								top  : parseFloat(selectedEl.style.top),
								left : parseFloat(selectedEl.style.left)
							}
						}
					}
				});

				eventTarget.addEventListener('mousemove', (e) => {

					if (dragElement && e.which === 1) {

						_Helpers.requestAnimationFrameWrapper(prevAnimFrameReqId, () => {

							let distance = {
								left: (e.clientX - eventStartPoint.x) / _Schema.ui.zoomLevel,
								top: (e.clientY - eventStartPoint.y) / _Schema.ui.zoomLevel,
							};

							for (let selectedEl of domElementsToDrag) {
								selectedEl.style.top  = (nodeStartPositions[selectedEl.id].top + distance.top) + 'px';
								selectedEl.style.left = (nodeStartPositions[selectedEl.id].left + distance.left) + 'px';
							}

							_Schema.ui.jsPlumbInstance.repaintEverything();
						});
					}
				});

				eventTarget.addEventListener('mouseup', (e) => {

					if (dragElement && e.which === 1) {

						dragElement = undefined;

						_Schema.storePositions();
						_Schema.ui.selection.updateSelectedNodes();
						Structr.resize();
					}
				});
			}
		},
		selection: {
			selectionInProgress: false,
			selectedRel: undefined,
			selectedNodes: [],
			selectBox: undefined,
			mouseDownCoords: { x: 0, y: 0 },
			mouseUpCoords: { x: 0, y: 0 },
			init: () => {

				let selectionElement = _Schema.ui.canvas.parent().parent()[0];

				selectionElement.addEventListener('mousedown', (e) => {
					if (e.which === 1) {
						_Schema.ui.selection.clearSelection();
						_Schema.ui.selection.selectionStart(e);
					}
				});

				selectionElement.addEventListener('mousemove', (e) => {
					if (e.which === 1) {
						_Schema.ui.selection.selectionDrag(e);
					}
				});

				selectionElement.addEventListener('mouseup', (e) => {
					_Schema.ui.selection.selectionStop();
				});
			},
			clearSelection: () => {

				// deselect selected node
				$('.node', _Schema.ui.canvas).removeClass('selected');
				_Schema.ui.selection.selectionStop();

				_Schema.ui.selection.deselectRel();
			},
			selectionStart: (e) => {

				_Schema.ui.selection.selectionInProgress = true;

				let schemaOffset = _Schema.ui.canvas.offset();
				_Schema.ui.selection.mouseDownCoords.x = e.pageX - schemaOffset.left;
				_Schema.ui.selection.mouseDownCoords.y = e.pageY - schemaOffset.top;
			},
			prevAnimFrameReqId_selectionDrag: undefined,
			selectionDrag: (e) => {

				if (_Schema.ui.selection.selectionInProgress === true) {

					_Helpers.requestAnimationFrameWrapper(_Schema.ui.selection.prevAnimFrameReqId_selectionDrag, () => {

						let schemaOffset = _Schema.ui.canvas.offset();
						_Schema.ui.selection.mouseUpCoords.x = e.pageX - schemaOffset.left;
						_Schema.ui.selection.mouseUpCoords.y = e.pageY - schemaOffset.top;

						_Schema.ui.selection.drawSelectElem();
					});
				}
			},
			selectionStop: () => {
				_Schema.ui.selection.selectionInProgress = false;
				if (_Schema.ui.selection.selectBox) {
					_Schema.ui.selection.selectBox.remove();
					_Schema.ui.selection.selectBox = undefined;
				}
				_Schema.ui.selection.updateSelectedNodes();
			},
			drawSelectElem: () => {

				if (!_Schema.ui.selection.selectBox || !_Schema.ui.selection.selectBox.length) {
					_Schema.ui.canvas.append('<svg id="schema-graph-select-box"><path d="" fill="none" stroke="#aaa" stroke-width="5"/></svg>');
					_Schema.ui.selection.selectBox = $('#schema-graph-select-box');
				}

				let cssRect = {
					position: 'absolute',
					top:    Math.min(_Schema.ui.selection.mouseDownCoords.y, _Schema.ui.selection.mouseUpCoords.y)  / _Schema.ui.zoomLevel,
					left:   Math.min(_Schema.ui.selection.mouseDownCoords.x, _Schema.ui.selection.mouseUpCoords.x)  / _Schema.ui.zoomLevel,
					width:  Math.abs(_Schema.ui.selection.mouseDownCoords.x - _Schema.ui.selection.mouseUpCoords.x) / _Schema.ui.zoomLevel,
					height: Math.abs(_Schema.ui.selection.mouseDownCoords.y - _Schema.ui.selection.mouseUpCoords.y) / _Schema.ui.zoomLevel
				};

				_Schema.ui.selection.selectBox.css(cssRect);
				_Schema.ui.selection.selectBox.find('path').attr('d', `m 0 0 h ${cssRect.width} v ${cssRect.height} h ${(-cssRect.width)} v ${(-cssRect.height)} z`);
				_Schema.ui.selection.selectNodesInRect(cssRect);
			},
			selectNodesInRect: (selectionRect) => {

				_Schema.ui.selection.selectedNodes = [];

				for (let el of _Schema.ui.canvas[0].querySelectorAll('.node')) {
					let $el = $(el);
					if (_Schema.ui.selection.isNodeInSelection($el, selectionRect)) {
						_Schema.ui.selection.selectedNodes.push($el);
						$el.addClass('selected');
					} else {
						$el.removeClass('selected');
					}
				}
			},
			isNodeInSelection: ($el, selectionRect) => {

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
			updateSelectedNodes: () => {

				_Schema.ui.selection.selectedNodes = [];
				let canvasOffset = _Schema.ui.canvas.offset();

				for (let el of _Schema.ui.canvas[0].querySelectorAll('.node.selected')) {
					let $el = $(el);
					let elementOffset = $el.offset();

					_Schema.ui.selection.selectedNodes.push({
						nodeId: $el.attr('id'),
						name: $el.children('b').text(),
						pos: {
							top:  (elementOffset.top  - canvasOffset.top ),
							left: (elementOffset.left - canvasOffset.left)
						}
					});
				}
			},
			selectRel: (rel) => {

				_Schema.ui.selection.clearSelection();

				_Schema.ui.selection.selectedRel = rel;
				_Schema.ui.selection.selectedRel.css({zIndex: ++_Schema.ui.maxZ});

				if (!rel.hasClass('dashed-inheritance-relationship')) {
					_Schema.ui.selection.selectedRel.nextAll('._jsPlumb_overlay').slice(0, 3).css({zIndex: ++_Schema.ui.maxZ, border: '1px solid ' + _Schema.ui.relHighlightColor, borderRadius:'2px', background: 'rgba(255, 255, 255, 1)'});
				}

				let pathElements = _Schema.ui.selection.selectedRel.find('path');
				pathElements.css({stroke: _Schema.ui.relHighlightColor});
				$(pathElements[1]).css({fill: _Schema.ui.relHighlightColor});
			},
			deselectRel: () => {

				if (_Schema.ui.selection.selectedRel) {

					_Schema.ui.selection.selectedRel.nextAll('._jsPlumb_overlay').slice(0, 3).css({ border: '', borderRadius: '', background: 'rgba(255, 255, 255, .8)' });

					let pathElements = _Schema.ui.selection.selectedRel.find('path');
					pathElements.css({stroke: '', fill: ''});
					$(pathElements[1]).css('fill', '');

					_Schema.ui.selection.selectedRel = undefined;
				}
			},
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
		updateInheritanceArrowsVisibility: (showArrows) => {

			_Schema.ui.showInheritanceArrows = showArrows;
			LSWrapper.setItem(_Schema.showSchemaInheritanceArrowsKey, showArrows);

			$('#schema-show-inheritance').prop('checked', showArrows);
			_Schema.ui.canvas[0]?.classList.toggle('hide-inheritance-arrows', !showArrows)
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
	markElementAsChanged: (element, hasClass) => {

		element.classList.toggle('has-changes', hasClass);
	},
	newAutoLayout: async () => {

		let index    = {};
		let relCount = {};

		let input = {
			id: 'root',
			children: [],
			edges: [],
			layoutOptions: {
				'elk.algorithm':                                       'layered',
				'elk.direction':                                       'DOWN',
				'elk.edgeWidth':                                       4,
				'elk.edgeLabels.inline':                               true,
				'elk.edgeLabels.placement':                            'CENTER',
				'elk.layered.edgeLabels.centerLabelPlacementStrategy': 'SPACE_EFFICIENT_LAYER',
				'elk.layered.edgeLabels.sideSelection':                'ALWAYS_UP',
				'elk.layered.spacing.edgeNodeBetweenLayers':           40,
			}
		};

		let nodeResponse = await fetch(`${Structr.rootUrl}SchemaNode/ui?${Structr.getRequestParameterName('sort')}=hierarchyLevel&${Structr.getRequestParameterName('order')}=asc&isServiceClass=false`);
		let nodes = await nodeResponse.json();

		for (let n of nodes.result) {

			_Schema.nodeData['id_' + n.id] = n;

			let width  = (n.name.length * 12) + 20; // n.clientWidth;
			let height = 20; // n.clientHeight;

			index[n.id] = true;

			let item = {
				id: 'id_' + n.id,
				width: width,
				height: height + 10,
				labels: [
					{ text: n.name }
				],
				ports: [
					{
						id: 'id_' + n.id + '_NORTH',
						layoutOptions: { 'port.side': 'NORTH' }
					},
					{
						id: 'id_' + n.id + '_SOUTH',
						layoutOptions: { 'port.side': 'SOUTH' }
					},
				],
				layoutOptions: {
					portConstraints: 'FIXED_SIDE'
				}
			};

			input.children.push(item);
		}

		// rels
		let response = await fetch(Structr.rootUrl + 'SchemaRelationshipNode');
		let data     = await response.json();

		// inheritance
		let includeInheritance = false;
		let inheritanceRels    = [];

		if (includeInheritance) {

			for (let node of input.children) {

				let data = _Schema.nodeData[node.id.substring(3)];
				if (data) {

					if (data?.extendsClass?.id) {

						let id = data.extendsClass.id;

						if (index[id]) {

/*
							inheritanceRels.push({
								source: 'id_' + id,
								target: node.id
							});
							 *
 */

							input.edges.push({
								id:      node.id + 'extends' + id,
								sources: [ 'id_' + id + '_SOUTH' ],
								targets: [ node.id + '_NORTH' ],
								//sources: [ 'id_' + id ],
								//targets: [ node.id ]
								labels: [
									{ text: 'EXTENDS' }
								]
							});
						}
					}
				}
			}

		} else {

			for (let res of data.result) {

				if (index[res.sourceId] === true && index[res.targetId] === true) {

					let s = 'id_' + res.sourceId;
					let t = 'id_' + res.targetId;

					if (!relCount[s]) { relCount[s] = 0; }
					if (!relCount[t]) { relCount[t] = 0; }

					relCount[s]++;
					relCount[t]++;

					input.edges.push({
						id:      res.id,
						//sources: [ 'id_' + res.sourceId + '_SOUTH' ],
						//targets: [ 'id_' + res.targetId + '_NORTH' ],
						sources: [ s ],
						targets: [ t ],
						labels: [
							{ text: res.relationshipType, width: (res.relationshipType.length * 12) + 20, height: 23 }
						]
					});
				}
			}

		}

		let container = document.querySelector('#schema-graph');

		container.innerText = '';

		_Pages.layout.createSVGDiagram(container, input, new SchemaNodesFormatter(inheritanceRels), _Schema.storePositionsNewLayout);

		/*
		// todo: implement click handler for nodes
		nodes[0].querySelector('.edit-type-icon').addEventListener('click', (e) => {
			_Schema.openEditDialog(entity.id);
		});
		*/


	},
	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/schema.css">

			<div>
				<div id="schema-container">
					<div class="canvas noselect" id="schema-graph"></div>
				</div>
			</div>
		`,
		functions: config => `
			<div class="flex-grow">
				<div class="inline-flex">

					<button id="create-type" class="action inline-flex items-center">
						${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['mr-2'])} Create Data Type
					</button>

					<div class="dropdown-menu dropdown-menu-large">
						<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green" id="user-defined-functions">
							${_Icons.getSvgIcon(_Icons.iconGlobe, 16, 16, ['mr-2'])} User-defined functions
						</button>
					</div>

					<div class="dropdown-menu dropdown-menu-large">
						<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green">
							${_Icons.getSvgIcon(_Icons.iconNetwork, 16, 16, ['mr-2'])} Display
						</button>

						<div class="dropdown-menu-container">
							<div class="row">
								<a title="Open dialog to show/hide the data types" id="schema-tools" class="flex items-center">
									${_Icons.getSvgIcon(_Icons.iconTypeVisibility, 16, 16, ['mr-2'])} Type Visibility
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
								<label class="block"><input ${_Schema.ui.showInheritanceArrows ? 'checked' : ''} type="checkbox" id="schema-show-inheritance" name="schema-show-inheritance"> Trait Inheritance arrows</label>
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
								<button id="load-layout" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Load</button>
								<button id="save-layout" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Save</button>
								<button id="delete-layout" class="mr-0 hover:bg-gray-100 focus:border-gray-666 active:border-green">Delete</button>
							</div>

							<div class="row">
								<input id="layout-name" placeholder="Enter name for layout">
								<button id="create-new-layout" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Create</button>
							</div>

							<div class="separator"></div>

							<div class="row">
								<a title="Reset the stored node positions." id="reset-schema-positions" class="flex items-center">
									${_Icons.getSvgIcon(_Icons.iconResetArrow, 16, 16, 'mr-2')} Reset Layout
								</a>
							</div>

							<div class="separator"></div>

							<div class="row">
								<a title="Apply an experimental new automatic layouting algorithm." id="new-auto-layout" class="flex items-center">
									${_Icons.getSvgIcon(_Icons.iconMagicWand, 16, 16, 'mr-2')} Experimental: Apply Automatic Layout
								</a>
							</div>
						</div>
					</div>

					<div class="dropdown-menu dropdown-menu-large">
						<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green">
							${_Icons.getSvgIcon(_Icons.iconSettingsCog, 16, 16, ['mr-2'])} Admin
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
								<button id="add-node-uuids" class="mt-1 inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">Add UUIDs</button>
								<button id="create-labels" class="mt-1 inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">Create Labels</button>
							</div>
							<div class="row">
								<select id="rel-type-selector" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
									<option selected value="">-- Select Relationship Type --</option>
									<option disabled>──────────</option>
									<option value="allRels">All Relationship Types</option>
									<option disabled>──────────</option>
								</select>
								<button id="reindex-rels" class="mt-1 inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">Rebuild relationship index</button>
								<button id="add-rel-uuids" class="mt-1 inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">Add UUIDs</button>
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
		`,
		typeBasicTab: config => `
			<div class="schema-details">
				
				<div class="basic-schema-details">
					<div class="flex items-center gap-x-2 pt-4">
	
						<input data-property="name" class="flex-grow" placeholder="Type Name...">
	
						${!config.isServiceClass ? `
							<div class="flex items-center gap-2">
								has traits
								<select multiple data-property="inheritedTraits"></select>
							</div>
						` : ''}
					</div>
	
					<h3 class="mt-8">Options</h3>
					<div class="property-options-group">
						<div class="flex">
							${config.isServiceClass ? `
								<label class="flex items-center mr-8" data-comment="Service-classes are containers for grouped functionality and can not be instantiated">
									<input id="serviceclass-checkbox" type="checkbox" data-property="isServiceClass" disabled checked> Is Service Class
								</label>
							` : ''}
							${!config.isServiceClass ? `
								<label class="flex items-center mr-8" data-comment="Only takes effect if the changelog is active">
									<input id="changelog-checkbox" type="checkbox" data-property="changelogDisabled"> Disable changelog
								</label>
								<label class="flex items-center mr-8" data-comment="Makes all nodes of this type visible to public users if checked">
									<input id="public-checkbox" type="checkbox" data-property="defaultVisibleToPublic"> Visible for public users
								</label>
								<label class="flex items-center" data-comment="Makes all nodes of this type visible to authenticated users if checked">
									<input id="authenticated-checkbox" type="checkbox" data-property="defaultVisibleToAuth"> Visible for authenticated users
								</label>
							` : ''}
						</div>
					</div>
	
					<h3 class="mt-8">OpenAPI</h3>
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
							<span id="source-type-name" class="edit-schema-object font-medium cursor-pointer">
								<span class="pointer-events-none"></span>
							</span>
						</div>

						<div class="flex items-center justify-around">
							<div class="overflow-hidden whitespace-nowrap">&#8212;</div>

							<select id="source-multiplicity-selector" data-attr-name="sourceMultiplicity" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
								<option value="1">1</option>
								<option value="*" selected>*</option>
							</select>

							<div class="overflow-hidden whitespace-nowrap">&#8212;[:</div>
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
							<span id="target-type-name" class="edit-schema-object font-medium cursor-pointer">
								<span class="pointer-events-none"></span>
							</span>
						</div>

						<div></div>
						<div class="flex items-center justify-center">
							<input id="source-json-name" class="remote-property-name" data-attr-name="sourceJsonName" autocomplete="off">
						</div>
						<div></div>
						<div class="flex items-center justify-center">
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
		schemaGrid: config => `
			<div class="schema-grid ${config.class}" style="${config.style}">
				<div class="schema-grid-header contents">
					${config.cols.map(col=> `<div class="${col.class}">${col.title}</div>`).join('')}
				</div>
				<div class="schema-grid-body contents">
				</div>
				<div class="schema-grid-footer contents">
					<div class="actions-col flex flex-wrap gap-y-2 items-center mt-2 mb-1" style="grid-column: 1 / -1;">
						${config.buttons ?? ''}
						${config.noButtons === true ? '' : `
							<div class="flex">
								<button class="discard-all inline-flex items-center disabled hover:bg-gray-100 focus:border-gray-666 active:border-green" disabled>
									${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16, 'icon-red mr-2')} Discard all
								</button>
								<button class="save-all inline-flex items-center disabled hover:bg-gray-100 focus:border-gray-666 active:border-green mr-0" disabled>
									${_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, 'icon-green mr-2')} Save all
								</button>
							</div>
						`}
					</div>
				</div>
			</div>
		`,
		basicAddButton: config => `
			<button class="add-button inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'icon-green mr-2')}${config.addButtonText}</button>
		`,
	}
};