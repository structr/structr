'use strict';

import {FlowNode} from "./FlowNode.js";
import {FlowSockets} from "../FlowSockets.js";

export class FlowSwitchCase extends FlowNode {

    constructor(node) {
        super(node);
    }

    getComponent() {
        let scopedDbNode = this.dbNode;
        return new D3NE.Component('SwitchCase', {
            template: FlowSwitchCase._nodeTemplate(),
            builder(node) {
                let socket = FlowSockets.getInst();
                let next = new D3NE.Output('Next', socket.getSocket('next'));
                let switch_switch = new D3NE.Input('Switch', socket.getSocket('switch'));

                node.data.dbNode = scopedDbNode;

                let caseValue = new D3NE.Control('<input type="text" value="" class="control-text">', (element, control) =>{

                    if(scopedDbNode !== undefined && scopedDbNode.case !== undefined && scopedDbNode.case !== null) {
                        element.setAttribute("value",scopedDbNode.case);
                        control.putData('case',element.value);
                    }

                    control.id = "case";
                    control.name = "Case";

                    element.addEventListener('mousedown', event => {
                        event.stopPropagation();
                    });

                    element.addEventListener('change', ()=>{
                        control.putData('case',element.value);
                        node.data['dbNode'].case = element.value;
                    });
                });

                return node
                    .addInput(switch_switch)
                    .addOutput(next)
                    .addControl(caseValue);
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