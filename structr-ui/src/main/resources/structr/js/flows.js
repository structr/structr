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
import { Persistence }         from "./lib/structr/persistence/Persistence.js";
import { FlowContainer }       from "./flow-editor/src/js/editor/entities/FlowContainer.js";
import { FlowEditor }          from "./flow-editor/src/js/editor/FlowEditor.js";
import { LayoutModal }         from "./flow-editor/src/js/editor/utility/LayoutModal.js";
import {Rest} from "./lib/structr/rest/Rest.js";

let main, flowsMain, flowsTree, flowsCanvas;
let flowEditor, flowId;
const methodPageSize = 10000, methodPage = 1;

document.addEventListener("DOMContentLoaded", function() {
    Structr.registerModule(_Flows);
});

var _Flows = {
	_moduleName: 'flows',
	flowsResizerLeftKey: 'structrFlowsResizerLeftKey_' + port,
	init: function() {

		Structr.makePagesMenuDroppable();
		Structr.adaptUiToAvailableFeatures();

	},
	resize: function() {
		_Flows.moveResizer();
		Structr.resize();
	},
	moveResizer: function(left) {
		left = left || LSWrapper.getItem(_Flows.flowsResizerLeftKey) || 300;
		flowsMain.querySelector('.column-resizer').style.left = left + 'px';

		main.querySelector('#flows-tree').style.width = left - 14 + 'px';
	},
	onload: async function() {

		let html = await Structr.fetchHtmlTemplate('flows/flows', {});

		main = document.querySelector('#main');

		main.innerHTML = html;

		_Flows.init();

		Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('flows'));

		flowsMain = document.querySelector('#flows-main');

		let rest = new Rest();
		let persistence = new Persistence();

		async function getOrCreateFlowPackage(packageArray) {

			if (packageArray !== null && packageArray.length > 0) {

				let currentPackage = packageArray[packageArray.length-1];
				packageArray.pop();

				let result = await persistence.getNodesByName(currentPackage, {type:"FlowContainerPackage"});

				if (result != null && result.length > 0 && result[0].effectiveName === (packageArray.join('.') + '.' + currentPackage)) {

					result = result[0];
				} else {

					result = await persistence.createNode({type: "FlowContainerPackage", name: currentPackage});
				}

				if (packageArray.length > 0) {
					result.parent = await getOrCreateFlowPackage(packageArray);
				}

				return result;
			}

			return null;
		}

		async function createFlow(inputElement) {
			let name = inputElement.value;
			inputElement.value = "";

			let parentPackage = null;

			if (name.indexOf(".") !== -1) {
				let nameElements = name.split(".");
				name = nameElements[nameElements.length -1];
				nameElements.pop();

				parentPackage = await getOrCreateFlowPackage(nameElements);
			}

			let flowObject = {
				type: "FlowContainer",
				name: name
			};

			if (parentPackage !== null) {
				flowObject.flowPackage = parentPackage.id;
			}

			persistence.createNode(flowObject).then( (r) => {
			   if (r !== null && r !== undefined && r.id !== null && r.id !== undefined) {
					_Flows.refreshTree(() => {
						$(flowsTree).jstree("deselect_all");
						$(flowsTree).jstree(true).select_node('li[id=\"' + r.id + '\"]');
					});
			   }
			});
		}

		function deleteFlow(id) {
			if (!document.querySelector(".delete_flow_icon").getAttribute('class').includes('disabled')) {
				if (confirm('Really delete flow ' + id + '?')) {
					persistence.deleteNode({type:"FlowContainer", id: flowId}).then(() => {
						_Flows.refreshTree();
					});
				}
			}
		}

		async function getPackageByEffectiveName(name) {
			let nameComponents = name.split("/");
			nameComponents = nameComponents.slice(1, nameComponents.length);
			let packages = await rest.get('/structr/rest/FlowContainerPackage?effectiveName=' + encodeURIComponent(nameComponents.join(".")));
			return packages.result.length > 0 ? packages.result[0] : null;
		}

		document.querySelector('#name-input').onkeydown = ((event) => {
			if (event.key === "Enter") {
				createFlow(document.getElementById('name-input'));
			}
		});
		document.querySelector('#create-new-flow').onclick = () => createFlow(document.getElementById('name-input'));
		document.querySelector('.reset_view_icon').onclick = () => flowEditor.resetView();

		let focusSelect = document.querySelector('#flow-focus-select');
		focusSelect.onchange = () => {

			let focus = focusSelect.value;

			flowsCanvas.classList.remove('focus');
			flowsCanvas.classList.remove('focus-action');
			flowsCanvas.classList.remove('focus-data');
			flowsCanvas.classList.remove('focus-exception');
			flowsCanvas.classList.remove('focus-logic');

			if (focus !== 'none') {
				flowsCanvas.classList.add('focus');
				flowsCanvas.classList.add('focus-' + focus);
			}
		};

		document.querySelector('.delete_flow_icon').onclick = () => deleteFlow(flowId);
		document.querySelector('.layout_icon').onclick = function() {
			if (!this.getAttribute('class').includes('disabled')) {
				new LayoutModal(flowEditor);
			}
		};

		document.querySelector('.run_flow_icon').addEventListener('click', function() {
			if (!this.getAttribute('class').includes('disabled')) {
				flowEditor.executeFlow();
			}
		});

		flowsTree = document.querySelector('#flows-tree');
		flowsCanvas = document.querySelector('#flows-canvas');

		_Flows.moveResizer();
		Structr.initVerticalSlider(document.querySelector('#flows-main .column-resizer'), _Flows.flowsResizerLeftKey, 204, _Flows.moveResizer);

		$(flowsTree).jstree({
			plugins: ["themes", "dnd", "search", "state", "types", "wholerow","sort", "contextmenu"],
			core: {
				check_callback: true,
				animation: 0,
				state: {
					key: 'structr-ui-flows'
				},
				async: true,
				data: _Flows.treeInitFunction,
			},
			sort: function(a, b) {
				let a1 = this.get_node(a);
				let b1 = this.get_node(b);

				if (a1.id.startsWith('/') && !b1.id.startsWith('/')) {
					return -1;
				} else if (b1.id.startsWith('/') && !a1.id.startsWith('/')) {
					return 1;
				} else {
					return (a1.text > b1.text) ? 1 : -1;
				}
			},
			contextmenu: {
				items: function(node) {
					let menuItems = {};

					if (node.data === null || node.id === "root") {

						menuItems.addFlow = {
							label: "Add Flow",
							action: async function (node) {
								let ref = $.jstree.reference(node.reference);
								let sel = ref.get_selected();
								if(!sel.length) { return false; }

								let p = null;
								if (sel[0] !== "root") {
									p = await getPackageByEffectiveName(sel[0]);
								}

								let newFlow = await persistence.createNode({
									type: "FlowContainer",
									flowPackage: p !== null ? p.id : null
								});
								newFlow.name = 'NewFlow-' + newFlow.id;

								newFlow = (await persistence.getNodesById(newFlow.id, {type: "FlowContainer"}))[0];
								_Flows.refreshTree(() => {
									$(flowsTree).jstree("deselect_all");
									$(flowsTree).jstree(true).select_node('li[id=\"' + newFlow.id + '\"]');
									_Flows.initFlow(newFlow.id);
								});

							}
						};
						menuItems.addPackage = {
							label: "Add Package",
							action: async function (node) {
								let ref = $.jstree.reference(node.reference);
								let sel = ref.get_selected();
								if(!sel.length) { return false; }

								let p = null;
								if (sel[0] !== "root") {
									p = await getPackageByEffectiveName(sel[0]);
								}

								let newFlowPackage = await persistence.createNode({
									type: "FlowContainerPackage",
									parent: p !== null ? p.id : null
								});
								newFlowPackage.name = 'NewFlowPackage-' + newFlowPackage.id;

								newFlowPackage = await persistence.getNodesById(newFlowPackage.id, {type: "FlowContainerPackage"});
								_Flows.refreshTree(() => {
									$(flowsTree).jstree("deselect_all");
									$(flowsTree).jstree(true).select_node('li[id=\"' + newFlowPackage.id + '\"]');
								});
							}
						};

					}

					if (node.id !== 'root' && node.id !== 'globals') {
						menuItems.renameItem = {
							label: "Rename",
							action: function(node) {
								let ref = $.jstree.reference(node.reference);
								let sel = ref.get_selected();
								if(!sel.length) { return false; }
								sel = sel[0];
								if(sel) {
									ref.edit(sel);
								}
							}
						};
						menuItems.deleteItem = {
							label: "Delete",
							action: function(node) {
								let ref = $.jstree.reference(node.reference);
								let sel = ref.get_selected();
								if(!sel.length) { return false; }
								sel = sel[0];
								if(sel) {
									let deleteMsg = null;
									if (ref._model.data[sel].data !== null && ref._model.data[sel].data.type === "FlowContainer") {
										deleteMsg = "Delete flow?";
									}  else {
										deleteMsg = "Delete recurively?";
									}
									if (confirm(deleteMsg)) {
										ref.delete_node(sel);
									}
								}
							}
						};
					}

					return menuItems;
				}
			}
		});

		Structr.unblockMenu(100);

		_Flows.resize();

		$(flowsTree).on('select_node.jstree', function(a, b) {

			if (b.event && b.event.type === "contextmenu") {
				return;
			}

			let id = $(flowsTree).jstree('get_selected')[0];
			if (id && b.node.data !== null && b.node.data.type === 'FlowContainer') {
				_Flows.initFlow(id);
			}
		});

		$(flowsTree).on('delete_node.jstree', function(event, data) {

			let handleDeletion = async function() {

				let type = data.node.data !== null && data.node.data.type !== null ? data.node.data.type : "FlowContainerPackage";
				let id = type === "FlowContainer" ? data.node.id : null;

				if (id === null && type === "FlowContainerPackage") {
					let p = await getPackageByEffectiveName(data.node.id);
					id = p.id;
				}

				if (id !== null) {
					persistence.deleteNode({
						type: type,
						id: id
					});
				}

				if (flowEditor !== undefined && flowEditor !== null && flowEditor.cleanup !== undefined) {
					flowEditor.cleanup();
					flowEditor = undefined;
				}

				// display flow canvas
				flowsCanvas.innerHTML = '<div id="nodeEditor" class="node-editor"></div>';
			};

			handleDeletion();

		});

		$(flowsTree).on('rename_node.jstree', function(event, data) {

			let handleRename = async function() {

				let type = data.node.data !== null && data.node.data.type !== null ? data.node.data.type : "FlowContainerPackage";
				let id = type === "FlowContainer" ? data.node.id : null;
				let name = data.text;

				if (id === null && type === "FlowContainerPackage") {
					let p = await getPackageByEffectiveName(data.node.id);
					if (p !== null) {
						id = p.id;
					}
				}

				let dataObject = {
					type: type,
					id: id,
					scheduledForIndexing: true
				};

				if (name.indexOf(".") !== -1) {
					dataObject.effectiveName = name;
				} else {
					dataObject.name = name;
				}

				if (id !== null) {
					await persistence._persistObject(dataObject);

					_Flows.refreshTree(() => {});
				}

			};

			handleRename();

		});

		$(flowsTree).on('move_node.jstree', function(event, data) {

			let handleParentChange = async function() {

				let type = data.node.data !== null && data.node.data.type !== null ? data.node.data.type : "FlowContainerPackage";
				let parent = data.node.parent;
				let id = type === "FlowContainer" ? data.node.id : null;

				let persistNode = async function(node) {
					persistence._persistObject(node);
				};

				if (id === null && type === "FlowContainerPackage") {
					let p = await getPackageByEffectiveName(data.node.id);
					id = p.id;
				}

				let parentId = null;

				if (parent !== "root") {
					let p = await getPackageByEffectiveName(parent);
					parentId = p.id;
				}

				let objectData = {
					type: type,
					id: id,
					scheduledForIndexing: true
				};

				let parentKey = null;
				switch (type) {
					case "FlowContainer":
						parentKey = "flowPackage";
						break;
					case "FlowContainerPackage":
						parentKey = "parent";
						break;
				}

				if (parentKey != null) {
					objectData[parentKey] = parentId;
					await persistNode(objectData);
				}

			};

			handleParentChange();

		});

		document.addEventListener("floweditor.nodescriptclick", event => {
			_Flows.openCodemirror(event.detail.element, event.detail.nodeType);
		});

		document.addEventListener("floweditor.loadflow", event => {
			if (event.detail.id !== undefined && event.detail.id !== null) {
				$(flowsTree).jstree("deselect_all");
				$(flowsTree).jstree(true).select_node('li[id=\"' + event.detail.id + '\"]');
			}
		});
	},
	refreshTree: function(callback) {
		_TreeHelper.refreshTree(flowsTree, callback);

	},
	treeInitFunction: function(obj, callback) {

		switch (obj.id) {

			case '#':

                let defaultEntries = [
                    {
                        id: 'root',
                        text: 'Flows',
                        children: true,
                        icon: _Icons.structr_logo_small,
                        path: '/',
                        state: {
                            opened: true,
                            selected: true
                        },
                        data: {type: "root"}
                    }
                ];

                callback(defaultEntries);
				break;

			case 'root':
				_Flows.load(null, callback);
				break;

			default:
				_Flows.load(obj.id, callback);
				break;
		}

	},
	unload: function() {
		fastRemoveAllChildren(document.querySelector('#main .searchBox'));
		fastRemoveAllChildren(document.querySelector('#main #flows-main'));
	},
	load: function(id, callback) {

        let list = [];

		let createFlowEntry = function(d) {

            return {
                id: d.id,
                text: d.name ? d.name.split('.').pop() : '[unnamed]',
                children: false,
                icon: 'fa fa-circle-o blue',
                data: {
                    type: d.type
                }
            };
		};

		let createFolder = function(path, list) {
			// Consume path to navigate to logical root in list
			let listRoot = list;
			let traversedPath = [];
			for (let p of path) {
				traversedPath.push(p);
				let currentFolder = listRoot.filter( r => r.id === ('/' + traversedPath.join('/')) );
				if (currentFolder.length >= 1) {
					listRoot = currentFolder[0].children;
				} else {
					let newFolder = {
                        id: ('/' + traversedPath.join('/')),
                        text: p,
                        children: [],
                        state: {
                            opened: true,
                        }
                    };
					listRoot.push(newFolder);
					listRoot = newFolder.children;
				}

			}

			return listRoot;

		};


		let displayFunctionPackage = function(result) {

			for (const d of result) {
				createFolder(d.effectiveName.split('.'), list);
            }

            $(flowsTree).jstree(true).sort(list, true);

			callback(list);
		};

        let displayFunction = function(result) {

            for (const d of result) {

                const nameComponents = d.effectiveName.split('.');

                if (nameComponents.length > 1) {
                    // Multi-component names must be abstracted through folders/packages
                    let folders = nameComponents;
                    // Pop the method name from the end of the folder list
                    folders.pop();

                    let folder = createFolder(folders, list);
                    folder.push(createFlowEntry(d));

                } else {
                    list.push(createFlowEntry(d));
                }
            }

            $(flowsTree).jstree(true).sort(list, true);

            callback(list);
        };

		if (!id) {

            Command.list('FlowContainer', false, methodPageSize, methodPage, 'name', 'asc', 'id,type,name,flowNodes,effectiveName', displayFunction);
			Command.list('FlowContainerPackage', false, methodPageSize, methodPage, 'name', 'asc', 'id,type,name,flowNodes,effectiveName,flows', displayFunctionPackage);

		} else {

			switch (id) {

				default:
					Command.query('FlowContainer', methodPageSize, methodPage, 'name', 'asc', {flowContainer: id}, displayFunction, true, 'ui');
					break;
			}
		}

	},
    openCodemirror: function(element, flowNodeType) {
        Structr.dialog("Edit " + flowNodeType, function() {}, function() {});

        dialogText.append('<div class="editor"></div>');
		let contentBox = $('.editor', dialogText);

		let cmEditor = CodeMirror(contentBox.get(0), Structr.getCodeMirrorSettings({
			value: element.value,
			mode: "application/javascript",
			lineNumbers: true,
			lineWrapping: false,
			indentUnit: 4,
			tabSize: 4,
			indentWithTabs: true,
			autofocus: true
		}));

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

        dialogSaveButton.off('click').on('click', function(e) {
            e.stopPropagation();
            element.value = cmEditor.getValue();
            element.dispatchEvent(new Event('change'));
            dialogSaveButton.prop("disabled", true).addClass('disabled');
        });

        dialogCancelButton.on('click', function(e) {
            dialogSaveButton.remove();
            saveAndClose.remove();
            return false;
        });


        cmEditor.on('change', function(cm, change) {
            if (element.value === cmEditor.getValue()) {
                dialogSaveButton.prop("disabled", true).addClass('disabled');
                saveAndClose.prop("disabled", true).addClass('disabled');
            } else {
                dialogSaveButton.prop("disabled", false).removeClass('disabled');
                saveAndClose.prop("disabled", false).removeClass('disabled');
            }
        });

        cmEditor.focus();
        cmEditor.setCursor(cmEditor.lineCount(), 0);

        window.setTimeout(() => {$('.closeButton', dialogBtn).blur(); cmEditor.focus();}, 10);
	},

	initFlow: function(id) {

		if (flowEditor !== undefined && flowEditor !== null && flowEditor.cleanup !== undefined) {
			flowEditor.cleanup();
			flowEditor = undefined;
		}

		// display flow canvas
		flowsCanvas.innerHTML = '<div id="nodeEditor" class="node-editor"></div>';

		flowId = id;
		let rest = new Rest();
        let persistence = new Persistence();

        persistence.getNodesById(id, new FlowContainer()).then( r => {
            document.title = "Flow - " + r[0].name;

            let rootElement = document.querySelector("#nodeEditor");
            flowEditor = new FlowEditor(rootElement, r[0], {deactivateInternalEvents: true});

            flowEditor.waitForInitialization().then( () => {

				rest.post('/structr/rest/FlowContainer/' + r[0].id + "/getFlowNodes").then((res) => {

					let result = res.result;

					if (Array.isArray(result)) {
						for (let node of result) {

							flowEditor.renderNode(persistence._wrapObject(node, node), true);
						}
					} else {

						flowEditor.renderNode(persistence._wrapObject(result, result), true);
					}

					flowEditor._editor.view.update();

				}).then(() => {

					rest.post('/structr/rest/FlowContainer/' + r[0].id + "/getFlowRelationships").then((res) => {

						let result = res.result;

						if (result !== null && result !== undefined && result.length > 0) {

							for (let rel of result) {

								flowEditor.connectNodes(rel);
							}
						}

					}).then(() => {

						flowEditor.applySavedLayout();
						flowEditor._editor.view.update();
						flowEditor.resetView();

						flowEditor.setupContextMenu();

						flowEditor.disableRelationshipEvents = false;

						// activate buttons
						document.querySelector('.run_flow_icon').classList.remove('disabled');
						document.querySelector('.delete_flow_icon').classList.remove('disabled');
						document.querySelector('.layout_icon').classList.remove('disabled');


					});

				});

            });

        });
	}
};
