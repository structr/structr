import {Persistence} from "../../persistence/Persistence.js";

export class CloneHandler {

	constructor() {
		this.config = {};
		this.config.remotePropertyKey = "_remote_properties";
	}

	copyElements(elements) {

		let processedNodes = [];
		let pendingRemoteProperties = {};

		for (let element of elements) {

			let nodeData = Object.entries(element.data.dbNode).reduce((acc,[key,value]) => {

				if (this._isRemoteProperty(value)) {
					value = this._extractRemoteIds(value);
					pendingRemoteProperties[element.data.dbNode.id] = pendingRemoteProperties[element.data.dbNode.id] || {};
					pendingRemoteProperties[element.data.dbNode.id][key] = value;
				} else {

					acc[key] = value;
				}

				return acc;
			}, {});

			processedNodes.push(nodeData);
		}

		// Check pendingRemoteProperties to see which ones are included within our copies and add existing ones to the node data
		for (let node of processedNodes) {

			node[this.config.remotePropertyKey] = {};

			for (let [key,value] of Object.entries(pendingRemoteProperties[node.id])) {

				if (Array.isArray(value)) {

					let newValue = value.filter(n => processedNodes.map(m => m.id).indexOf(n) !== -1);
					if (newValue.length > 0) {
						node[this.config.remotePropertyKey][key] = newValue;
					}
				} else {

					if (processedNodes.map(n => n.id).indexOf(value) !== -1) {

						node[key] = value;
					}
				}


			}

		}

		// processedNodes is now cleaned up and only contains relationships that are within the copied nodes context
		let result = {
			nodes: processedNodes.reduce((acc,cur) => {
				acc[cur.id] = cur; return acc
			}, {}),
			positions: elements.map(el => {
				return {
					id: el.data.dbNode.id,
					position: el.position
				};
			})
		};

		console.log(result);

		return result;

	}

	pasteElements(flowContainer, pasteData) {

		let persistence = new Persistence();

		let positions = pasteData.positions;
		let nodes = pasteData.nodes;

		// Iterate given pasteData, instantiate nodes and map existing id references
		for (let [nodeId,node] of Object.entries(nodes)) {

			


		}



		// Reminder: Set elements flowContainer to given flowContainer

	}


	_isRemoteProperty(value) {
		return value !== null && ((Array.isArray(value) && value.length > 0 && (typeof value[0] === "object" && value[0].id !== undefined)) || (typeof value === "object" && value.id !== undefined));
	}

	_extractRemoteIds(value) {

		let result = [];

		if (value !== undefined && value !== null) {

			if (Array.isArray(value) && value.length > 0 && value[0].id !== undefined) {

				result.push(this._extractRemoteId(value[0]));
			} else {

				result.push(this._extractRemoteId(value));
			}

		}

		return result;
	}

	_extractRemoteId(node) {

		if (typeof node === "object" && node.id !== undefined) {

			return node.id;
		} else {

			return null;
		}

	}

}