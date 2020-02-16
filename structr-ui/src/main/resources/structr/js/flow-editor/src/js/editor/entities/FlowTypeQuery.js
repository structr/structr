'use strict';

import {FlowNode} from "./FlowNode.js";
import {FlowSockets} from "../FlowSockets.js";
import {Persistence} from "../../../../../lib/structr/persistence/Persistence.js";
import {QueryBuilder} from "./components/QueryBuilder/QueryBuilder.js";

export class FlowTypeQuery extends FlowNode {

    constructor(node) {
        super(node);
    }

    getComponent() {

        let scopedDbNode = this.dbNode;
        return new D3NE.Component('TypeQuery', {
            template: FlowTypeQuery._nodeTemplate(),
            builder(node) {
                let socket = FlowSockets.getInst();
				let dataSource = new D3NE.Input('DataSource', socket.getSocket('dataSource'));
                let dataTarget = new D3NE.Output('DataTarget', socket.getSocket('dataTarget'), true);

                if (scopedDbNode !== undefined && scopedDbNode.isStartNodeOfContainer !== undefined && scopedDbNode.isStartNodeOfContainer !== null) {
                    node.isStartNode = true;
                } else {
                    node.isStartNode = false;
                }


                const builder = new QueryBuilder();

                // Add select box and render all SchemaTypes as dataType options
                let dataType = new D3NE.Control('<select class="control-select"><option>---- Select type ----</option></select>', (element, control) => {

                    let persistence = new Persistence();
                    persistence.getNodesByClass({type:"SchemaNode"},"ui").then(result => {

                        if (result && result.length > 0) {

                            let customTypes = document.createElement("optgroup");
                            customTypes.setAttribute("label", "Custom types");
                            let builtinTypes = document.createElement("optgroup");
                            builtinTypes.setAttribute("label", "Built-In Types");

                            const filteredResults = result.filter(el => {
                                return el.category !== 'html';
                            });

                            for (let schemaNode of filteredResults) {
                                let option = document.createElement("option");
                                option.text = schemaNode.name;
                                option.value = schemaNode.name;

                                if (scopedDbNode.dataType !== undefined && scopedDbNode.dataType !== null && scopedDbNode.dataType === schemaNode.name) {
                                    option.selected = true;
                                }

                                if (schemaNode.isBuiltinType) {
                                    builtinTypes.append(option);
                                } else {
                                    customTypes.append(option);
                                }

                            }

                            element.add(customTypes);
                            element.add(builtinTypes);

                        } else {

                            let option = document.createElement("option");
                            option.selected = true;
                            option.disabled = true;
                            element.add(option);

                        }

                    });

                    control.putData('dataType',element.value);
                    control.putData('dbNode', scopedDbNode);

                    control.id = "dataType";
                    control.name = "Type";

                    element.addEventListener('change', ()=>{
                        control.putData('dataType',element.value);
                        node.data['dbNode'].dataType = element.value;
                        builder.setQueryType(element.value);
                    });
                });

                let queryBuilder = new D3NE.Control('<div class="query-builder-container"></div>', (element, control) => {

                    const queryString = node.data['dbNode'].query;

                    if (queryString !== undefined && queryString !== "") {
                        builder.loadConfiguration(queryString);
                    }

                    element.appendChild(builder.getDOMNodes());

                    element.addEventListener("mousedown", (event)=>{event.stopPropagation();});
                    builder.getDOMNodes().addEventListener("query.builder.change", (event)=>{
                        const builder = event.detail;
                        const queryString = JSON.stringify(builder.interpret());

                        control.putData('query', queryString);
                        node.data['dbNode'].query = queryString;
                    });
                });



                return node
                    .addInput(dataSource)
                    .addOutput(dataTarget)
                    .addControl(dataType)
                    .addControl(queryBuilder);
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
                        <div class="control" id="{{control.id}}" :width="control.parent.width - 2 * control.margin" :height="control.height" al-control="control"></div>
                    </column>
                </content>
            </div>
        `;
    }


}