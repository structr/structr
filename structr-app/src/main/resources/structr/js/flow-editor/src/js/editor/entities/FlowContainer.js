'use strict';

import {Node} from "../../../../../lib/structr/persistence/model/Node.js";

export class FlowContainer extends Node {

    constructor(id, name, startNode, flowNodes) {

        super(id,name, FlowContainer.getType());

        if (startNode !== undefined && startNode !== null) {
            this.startNode = startNode;
        }
        if (flowNodes !== undefined && flowNodes !== null) {
            this.flowNodes = flowNodes;
        }
    }

    static getType() {
        return "FlowContainer";
    }
}