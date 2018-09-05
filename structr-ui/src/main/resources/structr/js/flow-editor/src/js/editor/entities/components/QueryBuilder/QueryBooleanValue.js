import {QueryValue} from "./QueryValue.js";

export class QueryBooleanValue extends QueryValue {

    constructor() {
        super();

        this.handles = {};

        this.domNodes = this._constructDOMElements();

        this._bindEvents();

        this.setValue(true);
    }

    // ---------- Interface ----------

    getValue() {
        const stringValue = this.handles.value.querySelector(".query-value option:checked").value;
        return stringValue === "true" ? true : false;
    }

    setValue(value) {
        let currentlySelectedOption = this.handles.value.querySelector(".query-value option:checked");
        if (currentlySelectedOption !== undefined && currentlySelectedOption !== null) {
            currentlySelectedOption.removeAttribute("selected");
        }

        if (value === true) {
            value = "true";
        } else if (value === false) {
            value = "false";
        }

        if (value === "true" || value === "false") {
            this.handles.value.querySelector(".query-value option[value=\"" + value + "\"]").setAttribute("selected", "selected");
        }
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

    }

    _getTemplate() {
        return `
            <select class="query-value">
                <option value=true>True</option>
                <option value=false>False</option>
            </select>
        `;
    }

    _constructDOMElements() {
        const html = new DOMParser().parseFromString(this._getTemplate(), "text/html");
        const rootElement = html.body.firstChild;

        //Select configured operation
        this.handles.value = rootElement;

        return rootElement;
    }

}