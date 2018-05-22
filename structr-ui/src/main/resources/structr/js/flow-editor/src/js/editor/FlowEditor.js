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

export class FlowEditor {

    constructor(rootElement, flowContainer) {
        this._editorId = 'structr-flow-editor@0.1.0';

        this._flowContainer = flowContainer;
        this._rootElement = rootElement;
        this.flowNodes = [];

        this._setupEditor();
        this._setupEngine();

    }

    _setupEditor() {
        this._editor = new D3NE.NodeEditor(this._editorId, this._rootElement, this._getComponents(), this._getMenu());

        this._overrideContextMenu();

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
                        persistence.createNode({type: relType, sourceId: sourceId, targetId: targetId});

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


    _setupEngine() {
        this._engine = new D3NE.Engine(this._editorId, this._getComponents());

        this._editor.eventListener.on('change', async () => {
            await this._engine.abort();
            let status = await this._engine.process(this._editor.toJSON());
        });
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
            new FlowObjectDataSource()
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
                'FlowDecision' : self._getNodeCreationFunction("FlowDecision"),
                'FlowKeyValue' : self._getNodeCreationFunction("FlowKeyValue"),
                'FlowObjectDataSource' : self._getNodeCreationFunction("FlowObjectDataSource"),
                'FlowReturn' : self._getNodeCreationFunction("FlowReturn")
            },
            'Actions': {
                'Save Layout' : self.saveLayout(),
                'Apply Layout' : self.applySavedLayout()
            }
        }, false);

        return menu;
    }


    _overrideContextMenu() {
        let view = this._editor.view;
        let al = alight.makeInstance();
        let self = this;

        al.directives.al.node = function(scope, element, expression, env) {
            let items = {
                'Remove node': () => {
                    this.editor.removeNode(node);
                },
                'Set as start node': () => {
                    node.data.dbNode.isStartNodeOfContainer = self._flowContainer;
                }
            };

            var onClick = (subitem) => {
                subitem.call(this);
                this.contextMenu.hide();
            }

            d3.select(element).on('contextmenu', null);

            d3.select(element).on('contextmenu', () => {
                if (this.editor.readOnly) return;

                var x = d3.event.clientX;
                var y = d3.event.clientY;

                self._editor.selectNode(node);
                view.contextMenu.show(x, y, items, false, onClick);
                d3.event.preventDefault();
            });
        }
    }

    saveLayout() {
        let self = this;
        return function() {

            var layout = {};
            var editorConfig = self.getEditorJson();

            for (let [key,node] of Object.entries(editorConfig.nodes)) {

                if (node.data.dbNode !== null && node.data.dbNode !== undefined) {
                    layout[node.data.dbNode.id] = node.position;
                }

            }

            window.localStorage.setItem('flow-' + self._flowContainer.id, JSON.stringify(layout));
        };
    }

    applySavedLayout() {
        let self = this;
        return function() {

            var layout = JSON.parse(window.localStorage.getItem('flow-' + self._flowContainer.id));

            var editorConfig = self._editor;

            if (layout !== null && layout !== undefined) {

                for (let [key, node] of Object.entries(editorConfig.nodes)) {
                    if (node.data.dbNode !== undefined && layout[node.data.dbNode.id] !== undefined) {
                        node.position = layout[node.data.dbNode.id];
                    }

                }

                self._editor.view.update();
            }

        };
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
            default:
                console.log('FlowEditor: renderNode() -> Used default FlowNode class. Implement custom class for proper handling! Given node type: ' + node.type);
                fNode = new FlowNode(node, this);
                break;
        }

        let component = fNode.getComponent();
        let editorNode = component.builder(component.newNode());
        fNode.editorNode = editorNode;

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