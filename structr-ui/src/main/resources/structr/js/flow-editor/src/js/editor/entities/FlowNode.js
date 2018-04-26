'use strict';

export class FlowNode {

    constructor(dbNode) {
        this.dbNode = dbNode;
        this.ports = [];
    }


    render(graph, customContent) {
        let gNode = graph.createNode();

        let labelText = `<div class="label ${this.dbNode.isStartNodeOfContainer !== null && this.dbNode.isStartNodeOfContainer !== undefined ? "startNode" : ""}">`;

        if (customContent === undefined || customContent === null) {
            var keys = Object.keys(this.dbNode);
            keys.forEach(k => {
                if (this.dbNode[k] !== undefined) {
                    labelText += `<p><b>${this.dbNode[k]}</b></p>`;
                }
            });
        } else {
            labelText += customContent;
        }

        labelText += `<p>${this.dbNode.type}</p>`

        labelText += '</div>';
        graph.addLabel(gNode, labelText);

        return gNode;
    }
}