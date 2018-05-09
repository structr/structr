'use strict';

export class FlowConnectionTypes {

    constructor() {
        this._connectionTypes = {
            FlowDataInput: {
                name: "DATA",
                sourceAttribute: "dataSource",
                targetAttribute: "dataTarget",
                type: "FlowDataInput"
            },
            FlowNodes: {
                name: "NEXT_FLOW_NODE",
                sourceAttribute: "prev",
                targetAttribute: "next",
                type: "FlowNodes"
            },
            FlowCallParameter: {
                name: "INPUT_FOR",
                sourceAttribute: "parameters",
                targetAttribute: "call",
                type: "FlowCallParameter"
            }

        };


    }

    static getInst() {
        if (window._flowConnectionTypesInst === undefined) {
            window._flowConnectionTypesInst = new FlowConnectionTypes();
        }
        return _flowConnectionTypesInst;
    }

    getAllConnectionTypes() {
        return this._connectionTypes;
    }

    getConnectionType(key) {
        return this._connectionTypes[key];
    }

}