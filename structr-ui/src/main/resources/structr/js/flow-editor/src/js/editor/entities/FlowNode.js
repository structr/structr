'use strict';

export class FlowNode {

    constructor(dbNode) {
        this.dbNode = dbNode;
    }

    getName() {
        return this.constructor.name;
    }

}