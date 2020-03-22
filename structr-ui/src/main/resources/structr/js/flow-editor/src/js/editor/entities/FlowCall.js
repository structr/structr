'use strict';

import {FlowNode} from "./FlowNode.js";
import {FlowSockets} from "../FlowSockets.js";
import {Persistence} from "../../../../../lib/structr/persistence/Persistence.js";
import {FlowContainer} from "./FlowContainer.js";

export class FlowCall extends FlowNode {

    constructor(node, flowEditor) {
        super(node, flowEditor);
    }

    getComponent() {
        let scopedDbNode = this.dbNode;
        let flowEditor = this.flowEditor;
        return new D3NE.Component('Call', {
            template: FlowCall._nodeTemplate(),
            builder(node) {
                let socket = FlowSockets.getInst();
                let prev = new D3NE.Input('Prev', socket.getSocket('prev'), true);
                let next = new D3NE.Output('Next', socket.getSocket('next'));
                let parameters = new D3NE.Input('Parameters', socket.getSocket('parameters'), true);
                let dataTarget = new D3NE.Output('DataTarget', socket.getSocket('dataTarget'), true);

                if (scopedDbNode !== undefined && scopedDbNode.isStartNodeOfContainer !== undefined && scopedDbNode.isStartNodeOfContainer !== null) {
                    node.isStartNode = true;
                } else {
                    node.isStartNode = false;
                }

                // Add select box and render all FlowContainer as Call options
                let call = new D3NE.Control('<select class="control-select"><option disabled selected>None</option></select>', (element, control) =>{

                    let persistence = new Persistence();

                    persistence.getNodesByClass(new FlowContainer(), 'effectiveNameView').then(result => {
                        
                        result = result.sort((a,b) => {
                            return a.effectiveName.toLowerCase() > b.effectiveName.toLowerCase() ? 1 : a.effectiveName.toLowerCase() < b.effectiveName.toLowerCase() ? -1 : 0;
                        });

                        for (let container of result) {
                            if (container.id !== flowEditor._flowContainer.id) {
                                let option = document.createElement("option");
                                option.text = container.effectiveName;
                                option.value = container.id;

                                if (scopedDbNode.flow !== undefined && scopedDbNode.flow !== null && scopedDbNode.flow.id === container.id) {
                                    option.selected = true;
                                }

                                element.add(option);
                            }
                        }

                    });

                    control.putData('flow',element.value);
                    control.putData('dbNode', scopedDbNode);

                    control.id = "flow";
                    control.name = "Flow";

                    element.addEventListener('change', ()=>{
                        control.putData('flow',element.value);
                        node.data['dbNode'].flow = element.value;
                    });
                });

                return node
                    .addInput(prev)
                    .addOutput(next)
                    .addInput(parameters)
                    .addOutput(dataTarget)
                    .addControl(call);
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