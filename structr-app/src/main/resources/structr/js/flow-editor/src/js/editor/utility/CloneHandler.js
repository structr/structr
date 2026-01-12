import {Persistence} from "../../../../../lib/structr/persistence/Persistence.js";
import {Rest} from "../../../../../lib/structr/rest/Rest.js";

export class CloneHandler {

	constructor() {
		this.config = {};
		this.config.remotePropertyKey = "_remote_properties";
	}

	copyElements(elements) {

		let processedNodes = [];
		let pendingRemoteProperties = {};

		for (let element of elements) {

			pendingRemoteProperties[element.data.dbNode.id] = {};

			let nodeData = Object.entries(element.data.dbNode).reduce((acc,[key,value]) => {

				if (this._isRemoteProperty(value)) {

					value = this._extractRemoteIds(value);
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

					let newValue = value.filter(n => (processedNodes.map(m => m.id).indexOf(n)) !== -1);
					if (newValue.length > 0) {
						node[this.config.remotePropertyKey][key] = newValue;
					}
				} else {

					if (processedNodes.map(n => n.id).indexOf(value) !== -1) {

						node[this.config.remotePropertyKey][key] = value;
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
					position: Object.assign({},el.position)
				};
			})
		};

		console.log(result);

		return result;

	}

	async pasteElements(editor, pasteData) {

		let positions = pasteData.positions;
		let nodes = pasteData.nodes;

		let newNodes = {};
		let globalRemoteProperties = {};
		let legacyIdMap = {};

		// Iterate given pasteData, instantiate nodes and map existing id references
		for (let [nodeId,node] of Object.entries(nodes)) {

			let legacyId = node.id;

			if (Object.entries(node[this.config.remotePropertyKey]).length > 0) {
				globalRemoteProperties[legacyId] = node[this.config.remotePropertyKey];
			}

			let newNode = await this._getOrCreateNode(node, nodes, editor._flowContainer);
			newNodes[newNode.id] = newNode;
			legacyIdMap[legacyId] = newNode.id;

		}

		for (let [legacyNodeId,remoteProperties] of Object.entries(globalRemoteProperties)) {

			let currentNode = newNodes[legacyIdMap[legacyNodeId]];

			for (let [key, value] of Object.entries(remoteProperties)) {

				if (Array.isArray(value)) {

					currentNode[key] = value.reduce((acc, cur) => {
						acc.push(legacyIdMap[cur]);
						return acc;
					}, [])
				} else {

					currentNode[key] = legacyIdMap[value];
				}

			}

		}

		for (let [index,data] of Object.entries(positions)) {
			data.id = legacyIdMap[data.id];
		}

		await this._renderNewNodes(editor, newNodes, positions);

	}

	async _renderNewNodes(editor, nodes, positions) {

		let rest = new Rest();
		let persistence = new Persistence();

		let promises = [];

		for (let [nodeId,node] of Object.entries(nodes)) {

			await editor.renderNode(node);
		}

		let nodeList = Object.entries(nodes).map(([key,value]) => value);


		// TODO: Filter already connected rels
		rest.post(`${Structr.rootUrl}FlowContainer/${editor._flowContainer.id}/getFlowRelationships`).then((res) => {
			let result = res.result;

			for (let rel of result) {

				editor.connectNodes(rel);
			}
		}).then(() => {

			editor._editor.view.update();
		});


		for (let [index, data] of Object.entries(positions)) {

			let offset = {
				x: 0,
				y: 0
			};

			let view = editor._editor.view;

			offset.x = view.mouse[0];
			offset.y = view.mouse[1];

			data.position[0] += offset.x;
			data.position[1] += offset.y;

			let fNode = editor.flowNodes.filter(n => n.dbNode.id === data.id)[0];

			fNode.editorNode.position = data.position;
		}


		editor._editor.selected.list = editor.flowNodes.filter(n => nodeList.map(m=>m.id).indexOf(n.dbNode.id) !== -1).map(fn => fn.editorNode);

	}


	async _getOrCreateNode(currentNode, nodeData, flowContainer) {
		let persistence = new Persistence();

		let node = Object.assign({}, currentNode);

		// Remove linked property key and id
		delete node[this.config.remotePropertyKey];
		delete node["id"];

		node.flowContainer = flowContainer.id;

		let newNode = (await persistence._persistObject(node))[0];

		return newNode;
	}


	_isRemoteProperty(value) {
		return value !== null && (((Array.isArray(value) && value.length > 0 && (typeof value[0] === "object" && value[0].id !== undefined)) || (typeof value === "object" && value.id !== undefined)));
	}

	_extractRemoteIds(value) {

		let result = null;

		if (value !== undefined && value !== null) {

			if (Array.isArray(value) && value.length > 0) {

				result = [];
				for (let el of value) {
					result.push(this._extractRemoteId(el));
				}
			} else {

				result = this._extractRemoteId(value);
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