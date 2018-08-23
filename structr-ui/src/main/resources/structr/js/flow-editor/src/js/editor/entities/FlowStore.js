'use strict';

import {FlowNode} from "./FlowNode.js";
import {FlowSockets} from "../FlowSockets.js";

export class FlowStore extends FlowNode {

    constructor(node) {
        super(node);
    }



    getComponent() {
        let scopedDbNode = this.dbNode;
        return new D3NE.Component('Store', {
            template: FlowStore._nodeTemplate(),
            builder(node) {
                let socket = FlowSockets.getInst();
                let prev = new D3NE.Input('Prev', socket.getSocket('prev'), true);
                let next = new D3NE.Output('Next', socket.getSocket('next'));
                let dataSource = new D3NE.Input('DataSource', socket.getSocket('dataSource'));
                let dataTarget = new D3NE.Output('DataTarget', socket.getSocket('dataTarget'), true);

                if (scopedDbNode !== undefined && scopedDbNode.isStartNodeOfContainer !== undefined && scopedDbNode.isStartNodeOfContainer !== null) {
                    node.isStartNode = true;
                } else {
                    node.isStartNode = false;
                }

                node.data.dbNode = scopedDbNode;


                let operation = new D3NE.Control('<select class="control-select"><option value="store">Store</option><option value="retrieve">Retrieve</option></select>', (element, control) =>{

                    if(scopedDbNode !== undefined && scopedDbNode.operation !== undefined && scopedDbNode.operation !== null) {

                        for (let option of element.getElementsByTagName("option")) {
                            if (option.getAttribute("value") === scopedDbNode.operation) {
                                option.setAttribute("selected", "selected");
                            }
                        }

                        if (scopedDbNode.operation !== null && scopedDbNode.operation !== undefined) {
                            element.value = scopedDbNode.operation;
                            control.putData('operation',element.value);
                        }

                    } else {
                        // Otherwise set 'store' as default operation
                        scopedDbNode.operation = 'store';
                        control.putData('operation','store');
                    }

                    control.id = "operation";
                    control.name = "Operation";

                    element.addEventListener('change', ()=>{
                        control.putData('operation',element.value);
                        node.data.dbNode.operation = element.value;
                    });
                });

                let key = new D3NE.Control('<input type="text" value="" class="control-text">', (element, control) =>{

                    if(scopedDbNode !== undefined && scopedDbNode.key !== undefined && scopedDbNode.key !== null) {
                        element.setAttribute("value",scopedDbNode.key);
                        control.putData('key',element.value);
                    }

                    control.id = "key";
                    control.name = "Key";

                    element.addEventListener('mousedown', event => {
                        event.stopPropagation();
                    });

                    element.addEventListener('change', ()=>{
                        control.putData('key',element.value);
                        node.data['dbNode'].key = element.value;
                    });
                });

                return node
                    .addInput(prev)
                    .addOutput(next)
                    .addInput(dataSource)
                    .addOutput(dataTarget)
                    .addControl(operation)
                    .addControl(key);
            },
            worker(node, inputs, outputs) {
            }
        });
    }

    static _nodeTemplate() {
        return `
            <div class="title {{node.isStartNode ? 'startNode' : ''}}">{{node.title}}</div>
                <content>
                    <column al-if="node.controls.length&gt;0 || node.inputs.length&gt;0">
                        <!-- Inputs-->
                        <div al-repeat="input in node.inputs" style="text-align: left">
                            <div class="socket input {{input.socket.id}} {{input.multipleConnections?'multiple':''}} {{input.connections.length&gt;0?'used':''}}" al-pick-input="al-pick-input" title="{{input.socket.name}}
                {{input.socket.hint}}"></div>
                            <div class="input-title" al-if="!input.showControl()">{{input.title}}</div>
                            <div class="input-control" al-if="input.showControl()" al-control="input.control"></div>
                        </div>
                    </column>
                    <column>
                        <!-- Outputs-->
                        <div al-repeat="output in node.outputs" style="text-align: right">
                            <div class="output-title">{{output.title}}</div>
                            <div class="socket output {{output.socket.id}} {{output.connections.length>0?'used':''}}" al-pick-output="al-pick-output" title="{{output.socket.name}}
                {{output.socket.hint}}"></div>
                        </div>
                    </column>
                </content>
                <!-- Controls-->
                <content al-repeat="control in node.controls">
                    <column>
                        <label class="control-title" for="{{control.id}}">{{control.name}}</label>
                    </column>
                    <column>
                        <div class="control" id="{{control.id}}" style="text-align: center" :width="control.parent.width - 2 * control.margin" :height="control.height" al-control="control"></div>
                    </column>
                </content>
            </div>
        `;
    }

}