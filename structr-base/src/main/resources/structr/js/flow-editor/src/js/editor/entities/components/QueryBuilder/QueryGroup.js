import {QueryOperation} from "./QueryOperation.js";
import {QuerySortOperation} from "./QuerySortOperation.js";

export class QueryGroup {

	constructor(isRootGroup, isFirstGroup) {
		this.isRootGroup = isRootGroup || false;
		this.isFirstGroup = isRootGroup || isFirstGroup || false;

		this.handles = {};

		this.model = new Proxy(
			{
				op:"and",
				operations:[],
				queryType: ""
			},
			QueryGroup._getProxyHandler(this)
		);

		this.sortOp = undefined;

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
		for(let op of this.model.operations) {
			op.setQueryType(type);
		}
		this.sortOp.setQueryType(type);
	}

	loadConfiguration(config) {
		if(config !== undefined && config !== undefined) {
			this.model.op = config.op;
			this.model.queryType = config.queryType;

			for (const element of config.operations) {
				switch (element.type) {
					case "group":
						let group = this._addGroup();
						group.loadConfiguration(element);
						break;
					case "operation":
						let operation = this._addOperation();
						operation.loadConfiguration(element);
						break;
					case "sort":
						let sortoperation = this._addSortOperation();
						sortoperation.loadConfiguration(element);
						break;
				}
			}
		}
	}

	interpret() {
		return {
			type: "group",
			op: this.model.op,
			operations: this.model.operations.length > 0 ? this.model.operations.map( op => op.interpret()) : [],
			queryType: this.model.queryType
		}
	}

	// ---------- Internal ----------

	_constructDOMElements() {
		const html = new DOMParser().parseFromString(this._getTemplate(), "text/html");
		const rootElement = html.body.firstChild;

		//Select configured operation
		this.handles.operations = rootElement.querySelector(".query-group .query-group-operations");
		this.handles.andButton = rootElement.querySelector(".query-group .query-group-button-and");
		this.handles.orButton = rootElement.querySelector(".query-group .query-group-button-or");
		this.handles.notButton = rootElement.querySelector(".query-group .query-group-button-not");
		this.handles.addOperation = rootElement.querySelector(".query-group .query-group-button-add-operation");
		this.handles.addGroup = rootElement.querySelector(".query-group .query-group-button-add-group");
		this.handles.addSort = rootElement.querySelector(".query-group .query-group-button-add-sort");
		this.handles.deleteButton = rootElement.querySelector(".query-group .query-group-button-delete");

		if (this.isRootGroup) {
			this.handles.deleteButton.classList.add("hidden");
		} else {
			this.handles.addSort.classList.add("hidden");
		}

		if (this.isFirstGroup) {
			this.handles.andButton.classList.add("hidden");
			this.handles.orButton.classList.add("hidden");
			this.handles.notButton.classList.add("hidden");
		}

		return rootElement;
	}

	_dispatchChangeEvent() {
		this.getDOMNodes().dispatchEvent(new CustomEvent("query.group.change", {detail: this}));
	}

	_bindEvents() {
		// Events
		this.handles.andButton.addEventListener("click", () => {
			this.model.op = "and";
			this._dispatchChangeEvent();
		});

		this.handles.orButton.addEventListener("click", () => {
			this.model.op = "or";
			this._dispatchChangeEvent();
		});

		this.handles.notButton.addEventListener("click", () => {
			this.model.op = "not";
			this._dispatchChangeEvent();
		});

		this.handles.addOperation.addEventListener("click", () => {
			this._addOperation();
			this._dispatchChangeEvent();
		});

		this.handles.addGroup.addEventListener("click", () => {
			this._addGroup();
			this._dispatchChangeEvent();
		});

		this.handles.addSort.addEventListener("click", () => {
			this._addSortOperation();
			this._dispatchChangeEvent();
		});

		this.handles.deleteButton.addEventListener("click", () => {
			this.getDOMNodes().dispatchEvent(new CustomEvent("query.group.delete", {detail: this}));
		});

	}

	_addGroup() {
		let group = undefined;

		if (this.model.operations.length > 0) {
			group = new QueryGroup(false, false);
		} else {
			group = new QueryGroup(false, true);
		}

		//Add listener for deletion event fired by QueryGroup delete button
		group.getDOMNodes().addEventListener("query.group.delete", (event) => {
			const toBeDeleted = event.detail;
			this.model.operations.splice(this.model.operations.indexOf(toBeDeleted), 1);
			this.handles.operations.removeChild(toBeDeleted.getDOMNodes());
			this._dispatchChangeEvent();
		});

		group.getDOMNodes().addEventListener("query.group.change", (event) => {
			this._dispatchChangeEvent();
		});

		group.getModel().queryType = this.model.queryType;
		this.model.operations.push(group);
		this._sortOperations();

		const sortOp = this.handles.operations.querySelector(".query-sort");

		if (sortOp !== null) {
			this.handles.operations.insertBefore(group.getDOMNodes(), sortOp);
		} else {
			this.handles.operations.appendChild(group.getDOMNodes());
		}

		return group;
	}

	_addOperation() {
		const operation = new QueryOperation();

		operation.setQueryType(this.model.queryType);

		//Add listener for deletion event fired by QueryOperation delete button
		operation.getDOMNodes().addEventListener("query.operation.delete", (event) => {
			const toBeDeleted = event.detail;
			this.model.operations.splice(this.model.operations.indexOf(toBeDeleted), 1);
			this.handles.operations.removeChild(toBeDeleted.getDOMNodes());
			this._dispatchChangeEvent();
		});

		operation.getDOMNodes().addEventListener("query.operation.change", (event) => {
			this._dispatchChangeEvent();
		});

		operation.getModel().queryType = this.model.queryType;
		this.model.operations.push(operation);
		this._sortOperations();

		const firstGroup = this.handles.operations.querySelector(".query-group");

		if (firstGroup !== null) {
			this.handles.operations.insertBefore(operation.getDOMNodes(), firstGroup);
		} else {
			const sortOp = this.handles.operations.querySelector(".query-sort");
			if (sortOp !== null) {
				this.handles.operations.insertBefore(operation.getDOMNodes(), sortOp);
			} else {
				this.handles.operations.appendChild(operation.getDOMNodes());
			}
		}

		return operation;
	}

	_addSortOperation() {
		// Only allow root group to add sort op and limit to one instance
		if (this.isRootGroup && this.handles.operations.querySelector(".query-sort") === null) {

			const operation = new QuerySortOperation();

			this.sortOp = operation;

			//Add listener for deletion event fired by QueryOperation delete button
			operation.getDOMNodes().addEventListener("query.operation.delete", (event) => {
				const toBeDeleted = event.detail;
				this.model.operations.splice(this.model.operations.indexOf(toBeDeleted), 1);
				this.handles.operations.removeChild(toBeDeleted.getDOMNodes());
				this._dispatchChangeEvent();
			});

			operation.getDOMNodes().addEventListener("query.operation.change", (event) => {
				this._dispatchChangeEvent();
			});

			operation.setQueryType(this.model.queryType);

			this.model.operations.push(operation);
			this._sortOperations();

			this.handles.operations.appendChild(operation.getDOMNodes());

			return operation;

		} else {
			return null;
		}
	}

	_getTemplate() {
		return `
			<div class="query-group">
				<div class="query-controls">
					<div class="query-button-group query-button-group-operation">
						<button class="query-group-button-and ${this.model.op === 'and' ? "active" : ''}">AND</button>
						<button class="query-group-button-or ${this.model.op === 'or' ? "active" : ''}">OR</button>
						<button class="query-group-button-not ${this.model.op === 'not' ? "active" : ''}">NOT</button>                      
					</div>
					<div class="query-button-group query-button-group-control">
						<button class="query-group-button-delete">
							${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red']))}
						</button>
					</div>
				</div>
				<div class="query-group-operations"></div>
				<div class="query-controls">
					<div class="query-button-group query-button-group-control flex">
						<button class="query-group-button-add-operation flex items-center gap-2">
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-green']))} Attribute
						   </button>
						<button class="query-group-button-add-group flex items-center gap-2">
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-green']))} Group
						</button>
						<button class="query-group-button-add-sort flex items-center gap-2">
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-green']))} Sort
						</button>
					</div>
				</div>
			</div>
		`;
	}

	_sortOperations() {
		this.model.operations = this.model.operations.sort( (a,b) => {
			if (a instanceof QueryOperation && b instanceof QueryGroup) {
				return -1;
			} else if (a instanceof QueryGroup && b instanceof QueryOperation) {
				return 1;
			}
			return 0;
		});
	}

	// ---------- Static ----------

	static _getProxyHandler(entity) {
		return {
			set: function(obj, prop, value){

				switch (prop) {
					case "op":
						switch (value) {
							case "and":
								entity.handles.orButton.classList.remove('active');
								entity.handles.notButton.classList.remove('active');
								entity.handles.andButton.classList.add('active');
								break;
							case "or":
								entity.handles.orButton.classList.add('active');
								entity.handles.notButton.classList.remove('active');
								entity.handles.andButton.classList.remove('active');
								break;
							case "not":
								entity.handles.notButton.classList.add('active');
								entity.handles.andButton.classList.remove('active');
								entity.handles.orButton.classList.remove('active');
								break;
						}
						break;
					case "operations":
						entity._dispatchChangeEvent();
						break;
				}

				obj[prop] = value;
				return true;
			}
		}
	}
}