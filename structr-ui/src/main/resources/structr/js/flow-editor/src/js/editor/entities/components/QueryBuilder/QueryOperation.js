import {Persistence} from "../../../../persistence/Persistence.js";
import {Rest} from "../../../../rest/Rest.js";
import {StructrRest} from "../../../../rest/StructrRest.js";

export class QueryOperation {

    constructor() {

        this.handles = {};

        this.model = new Proxy(
            {
                key:"",
                op:"eq",
                value:"",
                queryType: ""
            },
            QueryOperation._getProxyHandler(this)
        );

        this.domNodes = this._constructDOMElements();
        this._bindEvents();
    }

    // ---------- Interface ----------

    getDOMNodes() {
        return this.domNodes;
    }

    getModel() {
        return this.model;
    }

    setQueryType(type) {
        this.model.queryType = type;
        this._loadKeyOptions();
    }

    loadConfiguration(config) {
        if(config !== undefined && config !== undefined) {
            this.model.key = config.key;
            this.model.op = config.op;
            this.model.value = config.value;
            this.model.queryType = config.queryType;

            console.log(config.queryType);

            this._loadKeyOptions();
        }
    }

    interpret() {
        return {
            type: "operation",
            key: this.handles.key.value,
            op: this.handles.op.querySelector("option:checked").value,
            value: this.handles.value.value,
            queryType: this.model.queryType
        }
    }

    // ---------- Internal ----------

    _constructDOMElements() {
        const html = new DOMParser().parseFromString(this._getTemplate(), "text/html");
        const rootElement = html.body.firstChild;

        //Select configured operation
        this.handles.key = rootElement.querySelector(".query-operation .query-key-select");
        this.handles.op = rootElement.querySelector(".query-operation .query-operation-select");
        this.handles.value = rootElement.querySelector(".query-operation .query-value");
        this.handles.buttonDelete = rootElement.querySelector(".query-operation-delete");

        return rootElement;
    }

    _loadKeyOptions(type) {
        let queryType = undefined;
        if (type !== undefined && type !== null && type.length > 0) {
            queryType = type;
        }

        if (this.model.queryType !== undefined && this.model.queryType.length > 0) {
            queryType = this.model.queryType;
        }

        if (queryType !== undefined) {
            this.handles.key.remove(this.handles.key.childNodes);

            const structrRest = new StructrRest();
            structrRest.get("_schema/" + queryType).then((res) => {
                const properties = res.result[0].views.ui;
                for (let [key,prop] of Object.entries(properties)) {
                    const option = document.createElement("option");
                    option.value = prop.jsonName;
                    option.text = prop.jsonName;

                    if (prop.jsonName === this.model.key) {
                        option.selected = true;
                    }

                    this.handles.key.appendChild(option);
                }
            });

        }
    }

    _dispatchChangeEvent() {
        this.getDOMNodes().dispatchEvent(new CustomEvent("query.operation.change", {detail: this}));
    }

    _bindEvents() {
        // Events
        this.handles.key.addEventListener("change", () => {
            this.model.key = this.handles.key.querySelector("option:checked").value;
            this._dispatchChangeEvent();
        });

        this.handles.op.addEventListener("change", () => {
            this.model.op = this.handles.op.querySelector("option:checked").value;
            this._dispatchChangeEvent();
        });

        this.handles.value.addEventListener("change", () => {
            this.model.value = this.handles.value.value;
            this._dispatchChangeEvent();
        });

        this.handles.buttonDelete.addEventListener("click", () => {
            this.getDOMNodes().dispatchEvent(new CustomEvent("query.operation.delete", {detail: this}));
        });

    }

    _getTemplate() {
        return `
            <div class="query-operation">
                <select class="query-key-select"><option>N/A</option></select>
                <select class="query-operation-select">
                    <option value="eq">Equal</option>
                    <option value="neq"> Not Equal</option>
                </select>
                <input class="query-value" type="text" placeholder="Value">
                <button class="query-operation-delete">X</button>
            </div>
        `;
    }

    // ---------- Static ----------

    static _getProxyHandler(entity) {
        return {
            set: function(obj, prop, value){

                switch (prop) {
                    case 'key':
                        const option = entity.handles.key.querySelector("option[value=\"" + value + "\"]");
                        if (option !== undefined && option !== null) {
                            option.setAttribute("selected", "selected");
                        }
                        break;
                    case 'op':
                        entity.handles.op.querySelector("option[value=\"" + value + "\"]").setAttribute("selected","selected");
                        break;
                    case 'value':
                        entity.handles.value.value = value;
                        break;
                    case 'queryType':
                        entity._loadKeyOptions(value);
                        break;
                }

                obj[prop] = value;
                return true;
            }

        }
    }

}