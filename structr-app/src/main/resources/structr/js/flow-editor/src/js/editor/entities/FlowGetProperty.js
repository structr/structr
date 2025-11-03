'use strict';

import {FlowNode} from "./FlowNode.js";
import {FlowSockets} from "../FlowSockets.js";

export class FlowGetProperty extends FlowNode {

    constructor(node) {
        super(node);
    }

    getComponent() {
        let scopedDbNode = this.dbNode;
        return new D3NE.Component('GetProperty', {
            template: FlowGetProperty._nodeTemplate(),
            builder(node) {
                let socket = FlowSockets.getInst();
                let dataTarget = new D3NE.Output('DataTarget', socket.getSocket('dataTarget'), true);
                let nodeSource = new D3NE.Input('NodeSource', socket.getSocket('nodeSource'));
                let propertyNameSource = new D3NE.Input('PropertyNameSource', socket.getSocket('propertyNameSource'));

                node.data.dbNode = scopedDbNode;

                let propertyName = new D3NE.Control('<input type="text" value="" class="control-text">', (element, control) =>{

                    if(scopedDbNode !== undefined && scopedDbNode.propertyName !== undefined && scopedDbNode.propertyName !== null) {
                        element.setAttribute("value",scopedDbNode.propertyName);
                        control.putData('propertyName',element.value);
                    }

                    control.id = "propertyName";
                    control.name = "PropertyName";

                    element.addEventListener('mousedown', event => {
                        event.stopPropagation();
                    });

                    element.addEventListener('change', ()=>{
                        control.putData('propertyName',element.value);
                        node.data['dbNode'].propertyName = element.value;
                    });
                });

                return node
                    .addInput(nodeSource)
                    .addInput(propertyNameSource)
                    .addOutput(dataTarget)
                    .addControl(propertyName);
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
                    <div al-if="node.inputs[1].connections.length==0">
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
            </div>
        `;
    }


}