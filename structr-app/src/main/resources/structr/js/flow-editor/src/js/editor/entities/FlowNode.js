'use strict';

export class FlowNode {

    constructor(dbNode, flowEditor) {
        this.dbNode = dbNode;
        this.flowEditor = flowEditor;
    }

    getName() {
        return this.constructor.name;
    }

    getComponent() {
        let scopedDbNode = this.dbNode;
        return new D3NE.Component('Node', {
            template: FlowNode._nodeTemplate(),
            builder(node) {
                node.data.dbNode = scopedDbNode;
                return node;
            },
            worker(node, inputs, outputs) {
            }
        });
    }

    static _nodeTemplate() {
        return `
            <div class="title">{{node.data.dbNode.type || node.title}}</div>
                <content>
                    <column>
                        <div class="control">
                            <p class="control-title">
                                Missing node implementation.
                            </p>
                        </div>
                    </column>
                </content>
            </div>
        `;
    }

}