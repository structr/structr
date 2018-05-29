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
import {CodeModal} from "./CodeModal.js";



export class FlowEditor {

    constructor(rootElement, flowContainer) {

        this._editorId = 'structr-flow-editor@0.1.0';

        this._flowContainer = flowContainer;
        this._rootElement = rootElement;
        this.flowNodes = [];

        this._setupEditor();

        document.addEventListener('openeditor', e => {
            new CodeModal(e.detail.element);
        });

    }

    _setupEditor() {
        this._editor = new D3NE.NodeEditor(this._editorId, this._rootElement, this._getComponents(), this._getMenu());

        this._editor.eventListener.on('connectioncreate', (data) =>{
            this._connectionCreationHandler(data.input, data.output);
        });

        this._editor.eventListener.on('connectionremove', (data) =>{
            this._connectionDeletionHandler(data);
        });

        this._editor.eventListener.on('noderemove', (data) =>{
            this._nodeDeletionHandler(data);
        });
    }

    _connectionCreationHandler(input, output) {

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
            new FlowForEach()
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
            });
        }
    }

    _getMenu() {
        let self = this;

        let menu = new D3NE.ContextMenu({
            'Create Node': {
                'FlowAction' : self._getNodeCreationFunction("FlowAction"),
                'FlowCall' : self._getNodeCreationFunction("FlowCall"),
                'FlowDataSource' : self._getNodeCreationFunction("FlowDataSource"),
                'FlowParameterInput' : self._getNodeCreationFunction("FlowParameterInput"),
                'FlowParameterDataSource' : self._getNodeCreationFunction("FlowParameterDataSource"),
                'FlowNotNull' : self._getNodeCreationFunction("FlowNotNull"),
                'FlowNot' : self._getNodeCreationFunction("FlowNot"),
                'FlowOr' : self._getNodeCreationFunction("FlowOr"),
                'FlowAnd' : self._getNodeCreationFunction("FlowAnd"),
                'FlowDecision' : self._getNodeCreationFunction("FlowDecision"),
                'FlowKeyValue' : self._getNodeCreationFunction("FlowKeyValue"),
                'FlowObjectDataSource' : self._getNodeCreationFunction("FlowObjectDataSource"),
                'FlowStore' : self._getNodeCreationFunction("FlowStore"),
                'FlowScriptCondition' : self._getNodeCreationFunction("FlowScriptCondition"),
                'FlowForEach' : self._getNodeCreationFunction("FlowForEach"),
                'FlowReturn' : self._getNodeCreationFunction("FlowReturn")
            },
            'Actions': {
                'Execute Flow': function() { self.executeFlow() },
                'Reset View': function() { self.resetView() },
                'Save Layout' : function() { self.saveLayout() },
                'Apply Layout' : function() { self.applySavedLayout() }
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


    // Hack to replace the default D3NE context menu for nodes
    _overrideContextMenu(element) {
        let self = this;

        window.setTimeout( ()=> {
            d3.select(element.editorNode.el).on('contextmenu', null);

            var items = {};

            const viableStartNodeTypes = [
                'FlowAction',
                'FlowCall',
                'FlowDecision',
                'FlowForEach',
                'FlowReturn',
                'FlowStore'
            ];

            if ( viableStartNodeTypes.filter( t => t === element.type).map( r => r > 0 ? true : false) ) {
                items['Set as start node'] = function SetAsStartNode() {
                    element.dbNode.isStartNodeOfContainer = self._flowContainer.id;

                };
            }

            items['Remove node'] = function RemoveNode() {
                self._editor.removeNode(element.editorNode);
            };


            var onClick = function onClick(subitem) {
                subitem.call(self);
                self._editor.view.contextMenu.hide();
            };

            d3.select(element.editorNode.el).on('contextmenu', () => {
                if (self._editor.readOnly) return;

                var x = d3.event.clientX;
                var y = d3.event.clientY;

                self._editor.selectNode(element.editorNode);
                self._editor.view.contextMenu.show(x, y, items, false, onClick);
                d3.event.preventDefault();
            });

        }, 100);

    }

    saveLayout() {

        var layout = {};
        var editorConfig = this.getEditorJson();

        for (let [key,node] of Object.entries(editorConfig.nodes)) {

            if (node.data.dbNode !== null && node.data.dbNode !== undefined) {
                layout[node.data.dbNode.id] = node.position;
            }

        }

        window.localStorage.setItem('flow-' + this._flowContainer.id, JSON.stringify(layout));
    }

    applySavedLayout() {

        var layout = JSON.parse(window.localStorage.getItem('flow-' + this._flowContainer.id));

        var editorConfig = this._editor;

        if (layout !== null && layout !== undefined) {

            for (let [key, node] of Object.entries(editorConfig.nodes)) {
                if (node.data.dbNode !== undefined && layout[node.data.dbNode.id] !== undefined) {
                    node.position = layout[node.data.dbNode.id];
                }

            }

            this._editor.view.update();
            this.resetView();
        }

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
            default:
                console.log('FlowEditor: renderNode() -> Used default FlowNode class. Implement custom class for proper handling! Given node type: ' + node.type);
                fNode = new FlowNode(node, this);
                break;
        }

        let component = fNode.getComponent();
        let editorNode = component.builder(component.newNode());
        fNode.editorNode = editorNode;

        this._overrideContextMenu(fNode);

        this._editor.addNode(editorNode);

        this.flowNodes.push(fNode);

        return fNode;
    }

    getFlowNodeForDbId(id) {

        for (let node of this.flowNodes) {
            if (node.dbNode.id == id) {
                return node;
            }
        }
        return undefined;
    }

    getEditorJson() {
        return this._editor.toJSON();
    }

}