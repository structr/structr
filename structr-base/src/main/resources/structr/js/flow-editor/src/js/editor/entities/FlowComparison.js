'use strict';

import {FlowNode} from "./FlowNode.js";
import {FlowAction} from "./FlowAction.js";
import {FlowSockets} from "../FlowSockets.js";

export class FlowComparison extends FlowNode {

    constructor(node) {
        super(node);
    }

    getComponent() {
        let scopedDbNode = this.dbNode;
        return new D3NE.Component('Comparison', {
            template: FlowComparison._nodeTemplate(),
            builder(node) {
                let socket = FlowSockets.getInst();
                let dataSources = new D3NE.Input('DataSources', socket.getSocket('condition_dataSource'), true);
                let valueSource = new D3NE.Input('ValueSource', socket.getSocket('valueSource'), true);
                let result = new D3NE.Output('Result', socket.getSocket('condition_Result'), true);

                node.data.dbNode = scopedDbNode;

                let operation = new D3NE.Control('<select class="control-select">' +
                    '<option value="equal">Equal</option>' +
                    '<option value="notEqual">Not Equal</option>' +
                    '<option value="less">Less</option>' +
                    '<option value="lessOrEqual">Less or Equal</option>' +
                    '<option value="greater">Greater</option>' +
                    '<option value="greaterOrEqual">Greater or Equal</option>' +
                    '</select>', (element, control) =>{

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
                        // Otherwise set 'equal' as default operation
                        scopedDbNode.operation = 'equal';
                        control.putData('operation','equal');
                    }

                    control.id = "operation";
                    control.name = "Operation";

                    element.addEventListener('change', ()=>{
                        control.putData('operation',element.value);
                        node.data.dbNode.operation = element.value;
                    });
                });

                return node
                    .addInput(dataSources)
                    .addInput(valueSource)
                    .addOutput(result)
                    .addControl(operation);
            },
            worker(node, inputs, outputs) {
            }
        });
    }

    static _nodeTemplate() {
        return `
            <div class="title">{{node.title}}</div>
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
                <content al-repeat="control in node.controls" style="display:inline">
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