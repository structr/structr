export class QueryOperation {

    constructor() {

        this.handles = {};

        this.model = new Proxy(
            {
              key:"",
              op:"eq",
              value:""
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

    loadConfiguration(config) {
        if(config !== undefined) {
            this.model.key = config.key;
            this.model.op = config.op;
            this.model.value = config.value;
        }
    }

    interpret() {
        return {
            type: "operation",
            key: this.handles.key.value,
            op: this.handles.op.querySelector("option:checked").value,
            value: this.handles.value.value
        }
    }

    // ---------- Internal ----------

    _constructDOMElements() {
        const html = new DOMParser().parseFromString(this._getTemplate(), "text/html");
        const rootElement = html.body.firstChild;

        //Select configured operation
        this.handles.key = rootElement.querySelector(".query-operation .query-key");
        this.handles.op = rootElement.querySelector(".query-operation .query-operation-select");
        this.handles.value = rootElement.querySelector(".query-operation .query-value");
        this.handles.buttonDelete = rootElement.querySelector(".query-operation-delete");

        return rootElement;
    }

    _dispatchChangeEvent() {
        this.getDOMNodes().dispatchEvent(new CustomEvent("query.operation.change", {detail: this}));
    }

    _bindEvents() {
        // Events
        this.handles.key.addEventListener("change", () => {
            this.model.key = this.handles.key.value;
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
                <input class="query-key" type="text" placeholder="Key">
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
                        entity.handles.key.value = value;
                        break;
                    case 'op':
                        entity.handles.op.querySelector("option[value=\"" + value + "\"]").setAttribute("selected","selected");
                        break;
                    case 'value':
                        entity.handles.value.value = value;
                        break;
                }

                obj[prop] = value;
                return true;
            }

        }
    }

}