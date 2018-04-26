'use strict';

import {FlowNode} from "./FlowNode.js";
import {FreeNodePortLocationModel} from "../../../lib/yfiles/view-component.js";

export class FlowCall extends FlowNode {

    constructor(node) {
        super(node);
    }

    render(graph) {
        let labelText = "";
        if (this.dbNode.flow !== undefined) {
            labelText += `<p><b>${this.dbNode.flow.name}</b></p>`;
        }

        let node = super.render(graph,labelText);

        this.ports['parameters'] = graph.addPort(node, FreeNodePortLocationModel.NODE_TOP_ANCHORED);
        this.ports['prev'] = graph.addPort(node, FreeNodePortLocationModel.NODE_LEFT_ANCHORED);
        this.ports['next'] = graph.addPort(node, FreeNodePortLocationModel.NODE_RIGHT_ANCHORED);
        this.ports['dataTarget'] = graph.addPort(node, FreeNodePortLocationModel.NODE_BOTTOM_ANCHORED);
    }

}