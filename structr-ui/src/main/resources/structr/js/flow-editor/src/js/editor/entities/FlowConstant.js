'use strict';

import {FlowNode} from "./FlowNode.js";
import {FlowSockets} from "../FlowSockets.js";

export class FlowConstant extends FlowNode {

    constructor(node) {
        super(node);
    }

    getComponent() {
        let scopedDbNode = this.dbNode;
        return new D3NE.Component('Constant', {
            template: FlowConstant._nodeTemplate(),
            builder(node) {
                let socket = FlowSockets.getInst();
                let dataTarget = new D3NE.Output('DataTarget', socket.getSocket('dataTarget'), true);

                let value = new D3NE.Control('<textarea class="control-textarea">', (element, control) =>{

                    if(scopedDbNode !== undefined && scopedDbNode.value !== undefined) {
                        element.value = scopedDbNode.value;
                    }

                    control.putData('value',element.value);
                    control.putData('dbNode', scopedDbNode);

                    control.id = "value";
                    control.name = "Value";

                    element.addEventListener('focus', ()=> {
                        document.dispatchEvent(new CustomEvent('floweditor.internal.openeditor', {detail: {element: element}}));
                        document.dispatchEvent(new CustomEvent('floweditor.nodescriptclick', {detail: {element: element, nodeType: "Constant"}}));
                    });

                    element.addEventListener('change', ()=>{
                        control.putData('value',element.value);
                        node.data['dbNode'].value = element.value;
                    });
                });

                return node
                    .addOutput(dataTarget)
                    .addControl(value);
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