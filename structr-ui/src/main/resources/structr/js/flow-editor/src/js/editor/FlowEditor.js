'use strict';

import {FlowCall} from "./entities/FlowCall.js";
import {FlowDataSource} from "./entities/FlowDataSource.js";
import {FlowReturn} from "./entities/FlowReturn.js";
import {FlowNode} from "./entities/FlowNode.js";
import {FlowAction} from "./entities/FlowAction.js";
import {FlowParameterInput} from "./entities/FlowParameterInput.js";
import {FlowConnectionTypes} from "./FlowConnectionTypes.js";
import {Persistence} from "../persistence/Persistence.js";
import {FlowParameterDataSource} from "./entities/FlowParameterDataSource.js";
import {FlowNotNull} from "./entities/FlowNotNull.js";
import {FlowDecision} from "./entities/FlowDecision.js";
import {FlowKeyValue} from "./entities/FlowKeyValue.js";
import {FlowObjectDataSource} from "./entities/FlowObjectDataSource.js";
import {FlowStore} from "./entities/FlowStore.js";
import {FlowScriptCondition} from "./entities/FlowScriptCondition.js";
import {FlowNot} from "./entities/FlowNot.js";
import {FlowOr} from "./entities/FlowOr.js";
import {FlowAnd} from "./entities/FlowAnd.js";
import {FlowForEach} from "./entities/FlowForEach.js";
import {Rest} from "../rest/Rest.js";
import {CodeModal} from "./utility/CodeModal.js";
import {DependencyLoader} from "./utility/DependencyLoader.js";
import {FlowAggregate} from "./entities/FlowAggregate.js";
import {FlowConstant} from "./entities/FlowConstant.js";
import {FlowGetProperty} from "./entities/FlowGetProperty.js";
import {FlowCollectionDataSource} from "./entities/FlowCollectionDataSource.js";
import {LayoutManager} from "./utility/LayoutManager.js";
import {LayoutModal} from "./utility/LayoutModal.js";
import {FlowExceptionHandler} from "./entities/FlowExceptionHandler.js";



export class FlowEditor {

    constructor(rootElement, flowContainer) {

        this._initializationPromise = new Promise(resolve => {

            this._injectDependencies().then(() => {


                this._editorId = 'structr-flow-editor@0.1.0';

                this._flowContainer = flowContainer;
                this._rootElement = rootElement;
                this.flowNodes = [];

                window._rootElement = rootElement;

                this._setupEditor();

                document.addEventListener('openeditor', e => {
                    new CodeModal(e.detail.element);
                });

                resolve();

            });

        });

    }


    waitForInitialization() {
        return this._initializationPromise;
    }

    _injectDependencies() {
        let dep = new DependencyLoader();
        let depObject = {
            scripts: [
                "lib/d3-node-editor/d3.min.js",
                "lib/d3-node-editor/alight.min.js"
            ],
            stylesheets: [
                "lib/d3-node-editor/d3-node-editor.css",
                "css/FlowEditor.css"
            ]
        };

        return dep.injectDependencies(depObject).then( () => {return dep.injectScript("lib/d3-node-editor/d3-node-editor.js");})
    }

    _setupEditor() {
        this._editor = new D3NE.NodeEditor(this._editorId, this._rootElement, this._getComponents(), this._getMenu());

        // Extend the maximum viewport to feature a much larger workspace
        this._editor.view.setTranslateExtent(-65536, -65536, 65536, 65536);
        this._editor.view.setScaleExtent(0.01, 1);

        // Override default D3NE event bindings
        d3.select(this._rootElement).on('click', () => {
            if (d3.event.target === this._rootElement) {
                this._editor.view.pickedOutput = null;
                this._editor.view.update();
            }
        });

        // Bind context menu to right click instead of left click
        this._rootElement.oncontextmenu = event => {
            if (event.target === this._rootElement) {
                this._editor.view.assignContextMenuHandler();
                this._editor.view.contextMenu.show(event.clientX, event.clientY);
            }
            return false;
        };

        this._editor.eventListener.on('connectioncreate', (data) =>{
            try {
                this._connectionCreationHandler(data.input, data.output);
            } catch (e) {
                this._editor.eventListener.trigger('error',e);
                return false;
            }
        });

        this._editor.eventListener.on('connectionremove', (data) =>{
            this._connectionDeletionHandler(data);
        });

        this._editor.eventListener.on('noderemove', (data) =>{
            this._nodeDeletionHandler(data);
        });

    }

    _getContextMenuItemsForElement(editor, element) {
        let items = {};

        const viableStartNodeTypes = [
            'FlowAction',
            'FlowCall',
            'FlowDecision',
            'FlowForEach',
            'FlowReturn',
            'FlowStore',
            'FlowAggregate'
        ];

        if ( viableStartNodeTypes.filter( t => (t===element.dbNode.type) ).length > 0 ) {
            items['Set as start node'] = function setAsStartNode() {
                element.dbNode.isStartNodeOfContainer = editor._flowContainer.id;
                let oldStartNode = document.querySelector("div.title.startNode");
                if (oldStartNode !== undefined && oldStartNode !== null) {
                    oldStartNode.classList.remove("startNode");
                }
                element.editorNode.el.querySelector("div.title").classList.add("startNode");
            };
        }

        if (element instanceof FlowCall && element.dbNode.flow !== null) {
            items['Go to flow'] = function goToFlow() {
                let flow = element.dbNode.flow;
                if (flow !== undefined && flow !== null) {

                    let id = undefined;

                    if (flow instanceof Object) {
                        id = flow.id;
                    } else {
                        id = flow;
                    }

                    let searchParams = new URLSearchParams(window.location.search);
                    searchParams.set("id", id);
                    window.location.search = searchParams.toString();

                }
            }
        }

        items['Remove node'] = function RemoveNode() {
            this._editor.removeNode(element.editorNode);
        };

        return items;
    }

    // HACK: replace the default D3NE context menu for nodes
    _overrideContextMenu(element) {
        let self = this;

        d3.select(element.editorNode.el).on('contextmenu', null);

        let onClick = function onClick(subitem) {
            subitem.call(self);
            self._editor.view.contextMenu.hide();
        };

        d3.select(element.editorNode.el).on('contextmenu', () => {
            if (self._editor.readOnly) return;

            let x = d3.event.clientX;
            let y = d3.event.clientY;

            self._editor.selectNode(element.editorNode);
            self._editor.view.contextMenu.show(x, y, this._getContextMenuItemsForElement(self, element), false, onClick);
            d3.event.preventDefault();
        });

    }

    _connectionCreationHandler(input, output) {

        if (input.node.id === output.node.id) {
            this._editor.view.pickedOutput = null;
            throw new TypeError("Cannot connect a node to itself. Cancelling connection creation.");
        }

        if(input.node.data.dbNode !== undefined && output.node.data.dbNode !== undefined) {
            try {
                for (let [key, con] of Object.entries(FlowConnectionTypes.getInst().getAllConnectionTypes())) {

                    if (con.sourceAttribute === output.socket.id && con.targetAttribute === input.socket.id) {
                        let sourceId = output.node.data.dbNode.id;
                        let targetId = input.node.data.dbNode.id;
                        let relType = con.type;

                        let persistence = new Persistence();

                        persistence.getNodesByClass({type:relType}).then( result => {

                            let shouldCreate = result.filter( el => el.sourceId == sourceId && el.targetId == targetId).length == 0;

                            if (shouldCreate) {
                                persistence.createNode({type: relType, sourceId: sourceId, targetId: targetId});
                            }
                        });

                        break;
                    }

                }
            } catch (e) {
                console.log("Exception during rel creation:");
                console.log(e);
            }

        }

    }

    _connectionDeletionHandler(connection) {
        let persistence = new Persistence();
        persistence.deleteNode(connection);
    }

    _nodeDeletionHandler(node) {
        let persistence = new Persistence();
        persistence.deleteNode(node.data.dbNode);
    }

    _getFlowClasses() {
        return [
            new FlowAction(),
            new FlowCall(),
            new FlowDataSource(),
            new FlowParameterInput(),
            new FlowParameterDataSource(),
            new FlowReturn(),
            new FlowNode(),
            new FlowNotNull(),
            new FlowDecision(),
            new FlowKeyValue(),
            new FlowObjectDataSource(),
            new FlowStore(),
            new FlowScriptCondition(),
            new FlowNot(),
            new FlowOr(),
            new FlowAnd(),
            new FlowForEach(),
            new FlowAggregate(),
            new FlowConstant(),
            new FlowGetProperty(),
            new FlowCollectionDataSource(),
            new FlowExceptionHandler()
        ];
    }

    _getComponents() {

        if(this._components === undefined) {
            this._components = [];

            for (let comp of this._getFlowClasses()) {
               this._components.push(comp.getComponent());
            }
        }
        return this._components;
    }


    _getNodeCreationFunction(type) {
        let self = this;
        let entType = type;
        return function() {
            let persistence = new Persistence();
            let dbNode = persistence.createNode({type: entType}).then(node => {
                let fNode = self.renderNode(node);
                node.flowContainer = self._flowContainer.id;
                fNode.editorNode.position = self._editor.view.mouse;
                self._editor.view.update();
            });
        }
    }

    _getMenu() {
        let self = this;

        let menu = new D3NE.ContextMenu({
            'Action Nodes': {
                'FlowAction' : self._getNodeCreationFunction("FlowAction"),
                'FlowCall' : self._getNodeCreationFunction("FlowCall"),
                'FlowForEach' : self._getNodeCreationFunction("FlowForEach"),
                'FlowAggregate' : self._getNodeCreationFunction("FlowAggregate"),
                'FlowExceptionHandler': self._getNodeCreationFunction("FlowExceptionHandler"),
                'FlowReturn' : self._getNodeCreationFunction("FlowReturn")
            },
            'Data Nodes': {
                'FlowDataSource' : self._getNodeCreationFunction("FlowDataSource"),
                'FlowConstant' : self._getNodeCreationFunction("FlowConstant"),
                'FlowCollectionDataSource' : self._getNodeCreationFunction("FlowCollectionDataSource"),
                'FlowObjectDataSource' : self._getNodeCreationFunction("FlowObjectDataSource"),
                'FlowKeyValue' : self._getNodeCreationFunction("FlowKeyValue"),
                'FlowParameterInput' : self._getNodeCreationFunction("FlowParameterInput"),
                'FlowParameterDataSource' : self._getNodeCreationFunction("FlowParameterDataSource"),
                'FlowStore' : self._getNodeCreationFunction("FlowStore"),
                'FlowGetProperty': self._getNodeCreationFunction("FlowGetProperty")
            },
            'Logic Nodes': {
                'FlowDecision' : self._getNodeCreationFunction("FlowDecision"),
                'FlowNotNull' : self._getNodeCreationFunction("FlowNotNull"),
                'FlowNot' : self._getNodeCreationFunction("FlowNot"),
                'FlowOr' : self._getNodeCreationFunction("FlowOr"),
                'FlowAnd' : self._getNodeCreationFunction("FlowAnd"),
                'FlowScriptCondition' : self._getNodeCreationFunction("FlowScriptCondition")
            },
            'Actions': {
                'Execute Flow': function() { self.executeFlow() },
                'Reset View': function() { self.resetView() },
                'Select & Apply Layout' : function() { new LayoutModal(self) }
            }
        }, false);

        return menu;
    }


    resetView() {
        this._editor.view.zoomAt(this.flowNodes.map( n => n.editorNode ));
    }

    executeFlow() {

        let rest = new Rest();

        rest.post('/structr/rest/FlowContainer/' + this._flowContainer.id + '/evaluate', {}).then((res) => {
            console.log(res);
        });

    }

    async saveLayout() {

        let layoutManager = new LayoutManager(this);
        await layoutManager.saveLayout();

    }

    async applySavedLayout() {

        let layoutManager = new LayoutManager(this);
        let layout = await layoutManager.getOwnSavedLayout();

        if (layout === null) {
            let layouts = await layoutManager.getSavedLayouts();

            if (layouts !== null && layouts.length > 0) {

                layoutManager.applySavedLayout(layouts[0]);
            }
        }

        layoutManager.applySavedLayout(layout);

    }


    connectNodes(rel) {
        let source = this.getFlowNodeForDbId(rel.sourceId);
        let target = this.getFlowNodeForDbId(rel.targetId);

        if (source !== undefined && target !== undefined) {
            let connectionType = FlowConnectionTypes.getInst().getConnectionType(rel.type);

            let output = source.editorNode.outputs.filter(o => o.socket.id === connectionType.sourceAttribute)[0];
            let input = target.editorNode.inputs.filter(i => i.socket.id === connectionType.targetAttribute)[0];

            try {
                if (output !== undefined && output !== null && input !== undefined && input !== null) {
                    this._editor.connect(output, input);

                    let connection = output.connections.filter(c => c.input === input)[0];
                    connection.label = connectionType.name;
                    connection.type = connectionType.type;
                    connection.id = rel.id;
                }
            } catch (e) {
                console.log("Could not connect nodes: " + rel.sourceId + " and " + rel.targetId + " RelType: " + rel.type);
            }

        }

    }

    renderNode(node) {
        let fNode = undefined;
        switch (node.type) {
            case 'FlowAction':
                fNode = new FlowAction(node, this);
                break;
            case 'FlowCall':
                fNode = new FlowCall(node, this);
                break;
            case 'FlowDataSource':
                fNode = new FlowDataSource(node, this);
                break;
            case 'FlowReturn':
                fNode = new FlowReturn(node, this);
                break;
            case 'FlowParameterInput':
                fNode = new FlowParameterInput(node, this);
                break;
            case 'FlowParameterDataSource':
                fNode = new FlowParameterDataSource(node, this);
                break;
            case 'FlowNotNull':
                fNode = new FlowNotNull(node, this);
                break;
            case 'FlowDecision':
                fNode = new FlowDecision(node, this);
                break;
            case 'FlowKeyValue':
                fNode = new FlowKeyValue(node, this);
                break;
            case 'FlowObjectDataSource':
                fNode = new FlowObjectDataSource(node, this);
                break;
            case 'FlowStore':
                fNode = new FlowStore(node, this);
                break;
            case 'FlowScriptCondition':
                fNode = new FlowScriptCondition(node, this);
                break;
            case 'FlowNot':
                fNode = new FlowNot(node, this);
                break;
            case 'FlowOr':
                fNode = new FlowOr(node, this);
                break;
            case 'FlowAnd':
                fNode = new FlowAnd(node, this);
                break;
            case 'FlowForEach':
                fNode = new FlowForEach(node, this);
                break;
            case 'FlowAggregate':
                fNode = new FlowAggregate(node, this);
                break;
            case 'FlowConstant':
                fNode = new FlowConstant(node, this);
                break;
            case 'FlowGetProperty':
                fNode = new FlowGetProperty(node, this);
                break;
            case 'FlowCollectionDataSource':
                fNode = new FlowCollectionDataSource(node, this);
                break;
            case 'FlowExceptionHandler':
                fNode = new FlowExceptionHandler(node, this);
                break;
            default:
                console.log('FlowEditor: renderNode() -> Used default FlowNode class. Implement custom class for proper handling! Given node type: ' + node.type);
                fNode = new FlowNode(node, this);
                break;
        }

        let component = fNode.getComponent();
        let editorNode = component.builder(component.newNode());
        fNode.editorNode = editorNode;

        this.flowNodes.push(fNode);

        this._editor.addNode(editorNode);

        this._editor.view.update();

        this._overrideContextMenu(fNode);

        return fNode;
    }

    getFlowNodeForDbId(id) {

        for (let node of this.flowNodes) {
            if (node.dbNode.id === id) {
                return node;
            }
        }
        return undefined;
    }

    getFlowNodeForEditorNode(editorNode) {

        for (let node of this.flowNodes) {
            if (node.editorNode.id === editorNode.id) {
                return node;
            }
        }
        return undefined;
    }

    getEditorJson() {
        return this._editor.toJSON();
    }

}