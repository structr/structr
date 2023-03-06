import {StructrRest} from "../../../../../../../lib/structr/rest/StructrRest.js";
import {QueryStringValue} from "./QueryStringValue.js";

export class QuerySortOperation {

	constructor() {

		this.handles = {};

		this.model = new Proxy(
			{
				key:"",
				order:"asc",
				queryType: ""
			},
			QuerySortOperation._getProxyHandler(this)
		);

		this.valueComponent = new QueryStringValue();

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

	async loadConfiguration(config) {
		if(config !== undefined && config !== undefined) {
			this.model.key = config.key;
			this.model.order = config.order;
			this.model.queryType = config.queryType;

			await this._loadKeyOptions();
		}
	}

	interpret() {
		return {
			type: "sort",
			key: this.model.key,
			order: this.model.order,
			queryType: this.model.queryType
		}
	}

	// ---------- Internal ----------

	_constructDOMElements() {
		const html = new DOMParser().parseFromString(this._getTemplate(), "text/html");
		const rootElement = html.body.firstChild;

		this.handles.key = rootElement.querySelector(".query-operation .query-key-select");
		this.handles.order = rootElement.querySelector(".query-operation .query-order-select");
		this.handles.buttonDelete = rootElement.querySelector(".query-operation-delete");

		return rootElement;
	}

	async _loadKeyOptions(type) {
		let queryType = undefined;
		if (type !== undefined && type !== null && type.length > 0) {
			queryType = type;
		}

		if (this.model.queryType !== undefined && this.model.queryType.length > 0) {
			queryType = this.model.queryType;
		}

		if (queryType !== undefined) {
			while(this.handles.key.hasChildNodes()) {
				this.handles.key.removeChild(this.handles.key.firstChild);
			}

			const structrRest = new StructrRest();
			await structrRest.get("_schema/" + queryType).then((res) => {
				const properties = res.result[0].views.all;

				let entries = Object.entries(properties).sort();

				for (let [key,prop] of entries) {
					const option = document.createElement("option");
					option.value = prop.jsonName;
					option.text = prop.jsonName;
					option.dataset.dataType = prop.type;

					if (prop.jsonName === this.model.key) {
						option.setAttribute("selected","selected");
					}

					this.handles.key.appendChild(option);
				}

				if (this.model.key === undefined || this.model.key === null || this.model.key.length <= 0) {
					this.model.key = this.handles.key.querySelector("option").value;
					this._dispatchChangeEvent();
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

		this.handles.order.addEventListener("change", () => {
			this.model.order = this.handles.order.querySelector("option:checked").value;
			this._dispatchChangeEvent();
		});

		this.handles.buttonDelete.addEventListener("click", () => {
			this.getDOMNodes().dispatchEvent(new CustomEvent("query.operation.delete", {detail: this}));
		});

	}

	_getTemplate() {
		return `
			<div class="query-operation query-sort">
				<select class="query-key-select"><option>N/A</option></select>
				<select class="query-order-select">
					<option value="asc">ASC</option>
					<option value="desc">DESC</option>
				</select>
				<button class="query-operation-delete">
					${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red']))}
				</button>
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
					case 'order':
						entity.handles.order.querySelector("option[value=\"" + value + "\"]").setAttribute("selected","selected");
						break;
					case 'queryType':
						//entity._loadKeyOptions(value);
						break;
				}

				obj[prop] = value;
				return true;
			}

		}
	}

}