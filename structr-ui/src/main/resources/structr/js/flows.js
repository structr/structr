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
import { Persistence }         from "./flow-editor/src/js/persistence/Persistence.js";
import { FlowContainer }       from "./flow-editor/src/js/editor/entities/FlowContainer.js";
import { FlowEditor }          from "./flow-editor/src/js/editor/FlowEditor.js";
import { FlowConnectionTypes } from "./flow-editor/src/js/editor/FlowConnectionTypes.js";
import { LayoutModal }         from "./flow-editor/src/js/editor/utility/LayoutModal.js";

import { Component }           from "./lib/structr/Component.js";

let main, flowsMain, flowsTree, flowsCanvas;
let editor, flowId;
var drop;
var selectedElements = [];
var activeMethodId, methodContents = {};
var currentWorkingDir;
var methodPageSize = 10000, methodPage = 1;
var timeout, attempts = 0, maxRetry = 10;
var displayingFavorites = false;
var flowsLastOpenMethodKey = 'structrFlowsLastOpenMethodKey_' + port;
var flowsResizerLeftKey = 'structrFlowsResizerLeftKey_' + port;
var activeFlowsTabPrefix = 'activeFlowsTabPrefix' + port;

document.addEventListener("DOMContentLoaded", function() {
    Structr.registerModule(_Flows);
});

var _Flows = {
	_moduleName: 'flows',
	init: function() {

		_Logger.log(_LogType.FLOWS, '_Flows.init');

		Structr.makePagesMenuDroppable();
		Structr.adaptUiToAvailableFeatures();
		
	},
	resize: function() {

		var windowHeight = window.innerHeight;
		var headerOffsetHeight = 100;

		if (flowsTree) {
			flowsTree.style.height = windowHeight - headerOffsetHeight + 5 + 'px';
		}

		if (flowsCanvas) {
			flowsCanvas.style.height = windowHeight - headerOffsetHeight - 24 + 'px';
		}

		_Flows.moveResizer();
		Structr.resize();

	},
	moveResizer: function(left) {
		left = left || LSWrapper.getItem(flowsResizerLeftKey) || 300;
		document.querySelector('#flows-main .column-resizer').style.left = left + 'px';

		document.querySelector('#flows-tree').style.width   = left - 14 + 'px';
		document.querySelector('#flows-canvas').style.left  = left +  8 + 'px';
		document.querySelector('#flows-canvas').style.width = window.innerWidth - left - 47 + 'px';
	},
	onload: function() {

		_Flows.init();
		
		main = document.querySelector('#main');

		main.innerHTML = '<div id="flows-main"><div class="column-resizer"></div><div class="fit-to-height" id="flows-tree-container"><div id="flows-tree"></div></div><div class="fit-to-height" id="flows-canvas-container"><div id="flows-canvas"></div></div>';
		flowsMain = document.querySelector('#flows-main');
		
		let markup = `
			<div class="input-and-button"><input id="name-input" type="text" size="12" placeholder="Enter flow name"><button id="create-new-flow" class="action btn"><i class="${_Icons.getFullSpriteClass(_Icons.add_icon)}"></i> Add</button></div>
			<!--button class="add-flow-node"><i class="${_Icons.getFullSpriteClass(_Icons.add_brick_icon)}"></i> Add node</button-->
			<button class="delete_flow_icon button disabled"><i title="Delete" class="${_Icons.getFullSpriteClass(_Icons.delete_icon)}"></i> Delete flow</button>
			<button class="run_flow_icon button disabled"><i title="Run" class="${_Icons.getFullSpriteClass(_Icons.exec_icon)}"></i> Run</button>
			<button class="reset_view_icon button"><i title="Reset view" class="${_Icons.getFullSpriteClass(_Icons.refresh_icon)}"></i> Reset view</button>
			<button class="layout_icon button disabled"><i title="Layout" class="${_Icons.getFullSpriteClass(_Icons.wand_icon)}"></i> Layout</button>
		`;
		
		document.querySelector('#flows-canvas-container').insertAdjacentHTML('afterbegin', markup);

		let persistence = new Persistence();
		
		function createFlow(inputElement) {
            let name = inputElement.value;
            inputElement.value = "";
            persistence.createNode({type:"FlowContainer", name:name}).then( (r) => {
               _Flows.refreshTree();
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
			
		document.querySelector('#create-new-flow').onclick = () => createFlow(document.getElementById('name-input'));
		document.querySelector('.reset_view_icon').onclick = () => editor.resetView();
		
		document.querySelector('.delete_flow_icon').onclick = () => deleteFlow(flowId);
		document.querySelector('.layout_icon').onclick = function() {
			if (!this.getAttribute('class').includes('disabled')) {
				new LayoutModal(editor);
			}
		};

		//document.querySelector('.add-flow-node').onclick = function() {};

		document.querySelector('.run_flow_icon').addEventListener('click', function() {
			if (!this.getAttribute('class').includes('disabled')) {
				editor.executeFlow();
			}
		});

		flowsTree = document.querySelector('#flows-tree');
		flowsCanvas = document.querySelector('#flows-canvas');

		_Flows.moveResizer();
		Structr.initVerticalSlider(document.querySelector('#flows-main .column-resizer'), flowsResizerLeftKey, 204, _Flows.moveResizer);

//		$.jstree.defaults.core.themes.dots      = false;
//		$.jstree.defaults.dnd.inside_pos        = 'last';
//		$.jstree.defaults.dnd.large_drop_target = true;

		let displayFlow = function(e) {

			let id = e.target.closest('.jstree-node') ? e.target.closest('.jstree-node').getAttribute('id') : e.target.closest('[data-id]').getAttribute('data-id');
			
			_Flows.initFlow(id);
		}

//		_Flows.components = {
//			'flowsTree':  new Component('folderTree',  '#flows-tree'),
//			'flowsCanvas': new Component('flowsCanvas', '#flows-canvas')
//		};
//		
//		_Flows.components.flowsTree
//				.on('click', ['.jstree-node'], displayFlow)
//		;


        $(flowsTree).jstree({
            plugins: ["themes", "dnd", "search", "state", "types", "wholerow"],
            contextmenu: {items: _Flows.createFlowsTreeContextMenuItems},
            core: {
                animation: 0,
                state: {
                    key: 'structr-ui-flows'
                },
                async: true,
                data: _Flows.treeInitFunction
            }
        });

		window.removeEventListener('resize', resizeFunction);
		window.addEventListener('resize', function() {
			_Flows.resize();
		});

		Structr.unblockMenu(100);

		_Flows.resize();
		Structr.adaptUiToAvailableFeatures();
		
		$(flowsTree).on('select_node.jstree', function(a, b) {
			if (b.event && b.event.type === "contextmenu") {
				return;
			}

			let id = $(flowsTree).jstree('get_selected')[0];
			if (id && b.node.data.type === "FlowContainer") {
				_Flows.initFlow(id);
			}
		});

		document.addEventListener("floweditor.nodescriptclick", event => {
            _Flows.openCodemirror(event.detail.element, event.detail.nodeType);
		});

	},
	refreshTree: function() {
		_TreeHelper.refreshTree(flowsTree);

	},
	treeInitFunction: function(obj, callback) {
		
		switch (obj.id) {

			case '#':

				var defaultEntries = [
					{
						id: 'globals',
						text: 'Favorites',
						children: true,
						icon: _Icons.star_icon,
						data: {type: "favorite"}
					},
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

		var displayFunction = function(result) {

			var list = [];
                        
			result.forEach(function(d) {

				var icon = 'fa-file-code-o gray';
				switch (d.type) {
					case "FlowContainer":
						switch (d.codeType) {
							case 'java':
								icon = 'fa-dot-circle-o red';
								break;
							default:
								icon = 'fa-circle-o blue';
								break;
						}
				}

				list.push({
					id: d.id,
					text:  d.name ? d.name : '[unnamed]',
					children: false,
					icon: 'fa ' + icon,
					data: {
						type: d.type
					}
				});
			});

			callback(list);
		};

		if (!id) {

			Command.list('FlowContainer', false, methodPageSize, methodPage, 'name', 'asc', 'id,type,name,flowNodes', displayFunction);

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
		var contentBox = $('.editor', dialogText);
		var lineWrapping = LSWrapper.getItem(lineWrappingKey);
		editor = CodeMirror(contentBox.get(0), {
			value: element.value,
			mode: "application/javascript",
			lineNumbers: true,
			lineWrapping: lineWrapping,
			indentUnit: 4,
			tabSize:4,
			indentWithTabs: true
		});

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
            element.value = editor.getValue();
            element.dispatchEvent(new Event('change'));
            dialogSaveButton.prop("disabled", true).addClass('disabled');
        });

        dialogCancelButton.on('click', function(e) {
            dialogSaveButton.remove();
            saveAndClose.remove();
            return false;
        });

        editor.on('change', function(cm, change) {
            if (element.value === editor.getValue()) {
                dialogSaveButton.prop("disabled", true).addClass('disabled');
                saveAndClose.prop("disabled", true).addClass('disabled');
            } else {
                dialogSaveButton.prop("disabled", false).removeClass('disabled');
                saveAndClose.prop("disabled", false).removeClass('disabled');
            }
        });

	},
    /*createFlowsTreeContextMenuItems: function(node, test) {
        var items = {
            addFlowCallToCurrentOpenFlow: { // The "rename" menu item
                label: "Add Call to current Flow",
                action: function () {
					editor._getNodeCreationFunction("FlowCall")().then(newCallNode => {

						if (node.id === newCallNode.id) {
							return;
						}

                        console.log(newCallNode);
                    	console.log(node);

                    	let select = document.querySelector('#flow-call-node-select-' + newCallNode.id);
                    	select.value = node.id;
                    	select.dispatchEvent(new Event("change"));

					});


				}
            }
        };

        if (!$(node).hasClass("FlowContainer")) {
            // Delete the "delete" menu item
            delete items.deleteItem;
        }

        return items;
	},*/
	initFlow: function(id) {

		// display flow canvas
		flowsCanvas.innerHTML = '<div id="nodeEditor" class="node-editor"></div>';
		
		flowId = id;
        let persistence = new Persistence();

        persistence.getNodesById(id, new FlowContainer()).then( r => {
            document.title = "Flow - " + r[0].name;

            let rootElement = document.querySelector("#nodeEditor");
            editor = new FlowEditor(rootElement, r[0], {deactivateInternalEvents: true});

            editor.waitForInitialization().then( () => {

                let promises = [];

                r[0].flowNodes.forEach(node => {
                    promises.push(persistence.getNodesById(node.id).then(n => editor.renderNode(n[0])));
                });

                Promise.all(promises).then(() => {
                    for (let [name, con] of Object.entries(FlowConnectionTypes.getInst().getAllConnectionTypes())) {

                        persistence.getNodesByClass(con).then(relNodes => {

                            relNodes.forEach(rel => {

                                if (Array.isArray(rel)) {
                                    rel = rel[0];
                                }

                                if (r[0].flowNodes.filter(el => el.id === rel.sourceId).length > 0 && r[0].flowNodes.filter(el => el.id === rel.targetId).length > 0) {
                                    editor.connectNodes(rel);
                                }

                            });

                        });

                    }

                    editor.applySavedLayout();
                    editor._editor.view.update();
					editor.resetView();
					
					// activate buttons
					document.querySelector('.run_flow_icon').classList.remove('disabled');
					document.querySelector('.delete_flow_icon').classList.remove('disabled');
					document.querySelector('.layout_icon').classList.remove('disabled');

                });

            });

        });		
	}
};
