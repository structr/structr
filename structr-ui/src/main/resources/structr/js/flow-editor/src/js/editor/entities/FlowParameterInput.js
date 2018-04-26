'use strict';

import {FlowNode} from "./FlowNode.js";
import {FreeNodePortLocationModel} from "../../../lib/yfiles/view-component.js";

export class FlowParameterInput extends FlowNode {

    constructor(node) {
        super(node);
    }

    render(graph) {
        let labelText = "";
        labelText += `<p><b>${this.dbNode.key}</b></p>`;

        let node = super.render(graph, labelText);

        this.ports['dataSource'] = graph.addPort(node, FreeNodePortLocationModel.NODE_TOP_ANCHORED);
        this.ports['call'] = graph.addPort(node, FreeNodePortLocationModel.NODE_BOTTOM_ANCHORED);
    }

}