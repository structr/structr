import {StructrRest} from "../../../../../../../lib/structr/rest/StructrRest.js";
import {QueryStringValue} from "./QueryStringValue.js";
import {QueryBooleanValue} from "./QueryBooleanValue.js";

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
			this.model.op = config.op;
			this.model.value = config.value;
			this.model.queryType = config.queryType;

			await this._loadKeyOptions();

			let selectedKeyOption = this.getDOMNodes().querySelector(".query-operation .query-key-select option:checked");
			if (selectedKeyOption !== null) {
				this._swapValueComponent(selectedKeyOption.dataset.dataType, true);
				this.valueComponent.setValue(this.model.value);
			}

		}
	}

	interpret() {
		return {
			type: "operation",
			key: this.model.key,
			op: this.model.op,
			value: this.model.value,
			queryType: this.model.queryType
		}
	}

	// ---------- Internal ----------

	_constructDOMElements() {
		const html = new DOMParser().parseFromString(this._getTemplate(), "text/html");
		const rootElement = html.body.firstChild;

		this.handles.key = rootElement.querySelector(".query-operation .query-key-select");
		this.handles.op = rootElement.querySelector(".query-operation .query-operation-select");
		this.handles.buttonDelete = rootElement.querySelector(".query-operation-delete");

		rootElement.insertBefore(this.valueComponent.getDOMNodes(),this.handles.buttonDelete);
		this.handles.value = rootElement.querySelector(".query-operation .query-value");

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

	_swapValueComponent(type, retainValue) {

		if (retainValue !== true) {
			this.model.value = "";
		}

		// Remove value component
		if (this.handles.value !== undefined) {
			this.getDOMNodes().removeChild(this.handles.value);
			this.handles.value = undefined;
		}

		if (this.model.op !== "null" && this.model.op !== "notNull") {

			let valueComponentType = undefined;

			switch (type) {
				case "String":
					valueComponentType = QueryStringValue;
					break;
				case "Boolean":
					valueComponentType = QueryBooleanValue;
					break;
				default:
					valueComponentType = QueryStringValue;
					break;
			}

			this.valueComponent = new valueComponentType;

			this.getDOMNodes().insertBefore(this.valueComponent.getDOMNodes(), this.handles.buttonDelete);
			this.handles.value = this.getDOMNodes().querySelector(".query-operation .query-value");

			if (this.model.value !== undefined && this.model.value !== null) {
				this.valueComponent.setValue(this.model.value);
			}

			this.handles.value.addEventListener("query.operation.value.change", () => {
				this.model.value = this.valueComponent.getValue();
				this._dispatchChangeEvent();
			});

			this.model.value = this.valueComponent.getValue();
			this._dispatchChangeEvent();

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

			// If Null/NotNull is selected, remove value component
			if ((this.model.op === "null"  || this.model.op === "notNull") && this.handles.value !== undefined) {
				this.getDOMNodes().removeChild(this.handles.value);
				this.handles.value = undefined;
			} else if (this.handles.value === undefined) {
				if (this.model.queryType !== undefined && this.model.queryType !== null && this.model.queryType.length > 0) {
					this._swapValueComponent(this.model.queryType);
				}
			}

			this._dispatchChangeEvent();
		});


		this.handles.value.addEventListener("query.operation.value.change", () => {
			this.model.value = this.valueComponent.getValue();
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
					<optgroup label="Exact">
						<option value="eq">Equal</option>
						<option value="neq">Not Equal</option>
						<option value="gt">Greater</option>
						<option value="gteq">Greater/Equal</option>
						<option value="ls">Less</option>
						<option value="lseq">Less/Equal</option>
						<option value="null">Null</option>
						<option value="notNull">NotNull</option>
						<option value="startsWith">StartsWith</option>
						<option value="endsWith">EndsWith</option>
						<option value="contains">Contains</option>
					</optgroup>
					<optgroup label="Case Insensitive">
						<option value="caseInsensitiveStartsWith">StartsWith</option>
						<option value="caseInsensitiveEndsWith">EndsWith</option>
						<option value="caseInsensitiveContains">Contains</option>
					</optgroup>
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
						if (option !== undefined && option !== null && value !== obj[prop]) {
							obj[prop] = value;
							option.setAttribute("selected", "selected");
							entity._swapValueComponent(option.dataset.dataType);
						}
						break;
					case 'op':
						entity.handles.op.querySelector("option[value=\"" + value + "\"]").setAttribute("selected","selected");
						break;
					case 'value':
						entity.valueComponent.setValue(value);
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