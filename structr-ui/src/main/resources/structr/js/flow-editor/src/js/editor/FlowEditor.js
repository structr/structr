'use strict';

import { GraphComponent, InteriorStretchLabelModel } from "../../lib/yfiles/view-component.js";
import { OrganicLayout } from "../../lib/yfiles/layout-organic.js";
import * as viewLayoutBridge from "../../lib/yfiles/view-layout-bridge.js";
import { GraphEditorInputMode } from "../../lib/yfiles/view-editor.js";
import {Font} from "../../lib/yfiles/view-component.js";
import {FlowLabelStyle} from "../../js/editor/FlowLabelStyle.js";
import {FlowCall} from "./entities/FlowCall.js";
import {FlowDataSource} from "./entities/FlowDataSource.js";
import {FlowReturn} from "./entities/FlowReturn.js";
import {FlowNode} from "./entities/FlowNode.js";
import {FlowAction} from "./entities/FlowAction.js";
import {FlowParameterInput} from "./entities/FlowParameterInput.js";

export class FlowEditor {

    constructor(rootDOMNode) {
        this.rootDOMNode = rootDOMNode;
        this._graphComponent = new GraphComponent("#graphComponent");
        this._graph = this._graphComponent.graph;
        this.setup();
    }

    setup() {
        this._graph.nodeDefaults.size = new yfiles.geometry.Size(200, 100);
        this._graph.nodeDefaults.labels.layoutParameter = InteriorStretchLabelModel.CENTER;
        const font = new Font('"Seogoe UI", Arial', 13);
        this._graph.nodeDefaults.labels.style = new FlowLabelStyle(font);
        this._graph.edgeDefaults.labels.style = new FlowLabelStyle(font);

        this._graphComponent.fitGraphBounds();
        this._graphComponent.inputMode = new GraphEditorInputMode();
        this._graphComponent.inputMode.showHandleItems = yfiles.graph.GraphItemTypes.ALL & ~yfiles.graph.GraphItemTypes.NODE;
        this._graphComponent.inputMode.moveViewportInputMode.enabled = false;
        this._graphComponent.inputMode.moveUnselectedInputMode.enabled = false;
        this._graphComponent.inputMode.allowCreateNode = false;
        this._graphComponent.inputMode.allowCreateBend = false;
        this._graphComponent.inputMode.allowEditLabel = false;

        this._layout = new OrganicLayout();
        this._layout.considerNodeSizes = true;
        this._layout.minimumNodeDistance = 100;

    }

    doLayout(button) {
        if (button === undefined) {
            this._graphComponent.morphLayout(this._layout).then(() => console.log("morphed layout")).catch(e => {
                alert("Error during layout morph:" + e);
            });
        } else {
            button.setAttribute("disabled","disabled");
            this._graphComponent.morphLayout(this._layout).then(() => button.removeAttribute("disabled")).catch(e => {button.removeAttribute("disabled");alert("Error during layout morph:" + e);});
        }
    }

    getGraph() {
        return this._graph;
    }

    renderNode(node) {
        let fNode = undefined;
        switch (node.type) {
            case 'FlowAction':
                fNode = new FlowAction(node);
                break;
            case 'FlowCall':
                fNode = new FlowCall(node);
                break;
            case 'FlowDataSource':
                fNode = new FlowDataSource(node);
                break;
            case 'FlowReturn':
                fNode = new FlowReturn(node);
                break;
            case 'FlowParameterInput':
                fNode = new FlowParameterInput(node);
                break;
            default:
                console.log('FlowEditor: renderNode() -> Used default FlowNode class. Implement custom class for proper handling! Given node type: ' + node.type);
                fNode = new FlowNode(node);
                break;
        }

        fNode.render(this.getGraph());

        this.doLayout();
    }




}