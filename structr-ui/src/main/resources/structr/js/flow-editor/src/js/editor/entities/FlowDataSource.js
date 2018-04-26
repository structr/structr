'use strict';

import {FlowNode} from "./FlowNode.js";

export class FlowDataSource extends FlowNode {

    constructor(node) {
        super(node);
    }

    render(graph) {
        let labelText = "";
        labelText += `<p><b>${this.dbNode.query}</b></p>`;
        super.render(graph, labelText);
    }

}