import {QueryValue} from "./QueryValue.js";

export class QueryStringValue extends QueryValue {

    constructor() {
        super();

        this.handles = {};

        this.domNodes = this._constructDOMElements();

        this._bindEvents();
    }

    // ---------- Interface ----------

    getValue() {
        return this.handles.value.value;
    }

    setValue(value) {
        this.handles.value.value = value;
    }

    getDOMNodes() {
        return this.domNodes;
    }

    // ---------- Internal ----------
    _dispatchChangeEvent() {
        this.getDOMNodes().dispatchEvent(new CustomEvent("query.operation.value.change", {detail: this}));
    }

    _bindEvents() {
        // Events
        this.handles.value.addEventListener("change", () => {
            this._dispatchChangeEvent();
        });

        this.handles.value.addEventListener("keydown", (event) => {
            event.stopPropagation();
        });

    }

    _getTemplate() {
        return `<input class="query-value" type="text" placeholder="Value">`;
    }

    _constructDOMElements() {
        const html = new DOMParser().parseFromString(this._getTemplate(), "text/html");
        const rootElement = html.body.firstChild;

        //Select configured operation
        this.handles.value = rootElement;

        return rootElement;
    }

}