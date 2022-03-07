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
                        // document.dispatchEvent(new CustomEvent('floweditor.internal.openeditor', {detail: {element: element}}));
                        document.dispatchEvent(new CustomEvent('floweditor.nodescriptclick', {detail: { entity: scopedDbNode, propertyName: 'value', element: element, nodeType: "Constant"}}));
                    });

                    element.addEventListener('mousedown', event => {
                        event.stopPropagation();
                    });

                    element.addEventListener('change', ()=>{
                        control.putData('value',element.value);
                        node.data['dbNode'].value = element.value;
                    });
                });


                let constantType = new D3NE.Control('<select class="control-select">' +
                    '<option value="String">String</option>' +
                    '<option value="Boolean">Boolean</option>' +
                    '<option value="Integer">Integer</option>' +
                    '<option value="Double">Double</option>' +
                    '</select>', (element, control) =>{

                    if(scopedDbNode !== undefined && scopedDbNode.constantType !== undefined && scopedDbNode.constantType !== null) {

                        for (let option of element.getElementsByTagName("option")) {
                            if (option.getAttribute("value") === scopedDbNode.constantType) {
                                option.setAttribute("selected", "selected");
                            }
                        }

                        if (scopedDbNode.constantType !== null && scopedDbNode.constantType !== undefined) {
                            element.value = scopedDbNode.constantType;
                            control.putData('constantType',element.value);
                        }

                    } else {
                        // Otherwise set 'equal' as default operation
                        scopedDbNode.constantType = 'String';
                        control.putData('constantType','String');
                    }

                    control.id = "constantType";
                    control.name = "Type";

                    element.addEventListener('change', ()=>{
                        control.putData('constantType',element.value);
                        node.data.dbNode.constantType = element.value;
                    });
                });

                return node
                    .addOutput(dataTarget)
                    .addControl(constantType)
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