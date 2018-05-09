'use strict';

import {FlowCall} from "./entities/FlowCall.js";
import {FlowDataSource} from "./entities/FlowDataSource.js";
import {FlowReturn} from "./entities/FlowReturn.js";
import {FlowNode} from "./entities/FlowNode.js";
import {FlowAction} from "./entities/FlowAction.js";
import {FlowParameterInput} from "./entities/FlowParameterInput.js";

export class FlowEditor {

    constructor(rootElement) {
        this._rootElement = rootElement;
        this.flowNodes = [];

        this._setupEditor();
        this._setupEngine();

    }

    _setupEditor() {
        this._editor = new D3NE.NodeEditor('demo@0.1.0', this._rootElement, this._getComponents(), this._getMenu());
    }

    _setupEngine() {
        this._engine = new D3NE.Engine('demo@0.1.0', this._getComponents());

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
            new FlowReturn()
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

    _getMenu() {
        let nodeTypes = {};

        for (let comp of this._getFlowClasses()) {
            nodeTypes[comp.getName()] = comp.getComponent();
        }

        let menu = new D3NE.ContextMenu({
            'Nodes':nodeTypes
        });

        return menu;
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
            default:
                console.log('FlowEditor: renderNode() -> Used default FlowNode class. Implement custom class for proper handling! Given node type: ' + node.type);
                fNode = new FlowNode(node, this);
                break;
        }

        fNode.getComponent().builder(fNode.getComponent());

        this.flowNodes.push(fNode);

    }

    getFlowNodeForDbId(id) {

        for (let node of this.flowNodes) {
            if (node.dbNode.id == id) {
                return node;
            }
        }
        return undefined;
    }

}