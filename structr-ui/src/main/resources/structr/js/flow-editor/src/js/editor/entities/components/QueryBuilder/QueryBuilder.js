import {QueryGroup} from "./QueryGroup.js";

export class QueryBuilder {

    constructor() {
        this.domNodes = this._constructDOMElements();
    }

    // ---------- Interface ----------

    getDOMNodes() {
        return this.domNodes;
    }

    interpret() {
        return this.rootGroup.interpret();
    }

    loadConfiguration(queryString) {
        if (queryString !== undefined && queryString !== null) {
            const queryObject = JSON.parse(queryString);
            this.rootGroup.loadConfiguration(queryObject);
        }
    }

    setQueryType(type) {
        this.rootGroup.setQueryType(type);
        this.getDOMNodes().dispatchEvent(new CustomEvent("query.builder.change", {detail: this}));
    }

    // ---------- Internal ----------

    _constructDOMElements() {
        const html = new DOMParser().parseFromString(this._getTemplate(), "text/html");
        const rootElement = html.body.firstChild;

        this.rootGroup = new QueryGroup(true);

        this.rootGroup.getDOMNodes().addEventListener("query.group.change", (event) => {
            this.getDOMNodes().dispatchEvent(new CustomEvent("query.builder.change", {detail: this}));
        });

        rootElement.appendChild(this.rootGroup.getDOMNodes());

        return rootElement;
    }

    _getTemplate() {
        return `<div class="query-builder"></div>`;
    }

}