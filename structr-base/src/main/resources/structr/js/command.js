/*
 * Copyright (C) 2010-2023 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * Use these commands to send command calls to the websocket server.
 *
 * The websocket listener is in the websocket.js file.
 *
 */
let Command = {
	/**
	 * Send the LOGIN command to the server.
	 */
	login: function(data) {
		let obj = {
			command: 'LOGIN',
			sessionId: Structr.getSessionId(),
			data: data
		};
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send the LOGOUT command to the server.
	 */
	logout: function(username) {
		let obj = {
			command: 'LOGOUT',
			sessionId: Structr.getSessionId(),
			data: {
				username: username
			}
		};
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send the PING command to the server.
	 */
	ping: function(callback) {
		let obj = {
			command: 'PING',
			sessionId: Structr.getSessionId()
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a single GET command to the server.
	 *
	 * The server will return a list containing a single node with the given id to the sending client (no broadcast).
	 * The returned properties can be customized via the `properties` parameter. A comma-separated list of property names can be supplied.
	 * If null is provided, all properties are returned.
	 */
	get: function(id, properties, callback, view) {
		let obj = {
			command: 'GET',
			id: id
		};
		if (properties !== null) {
			obj.data = {
				properties: properties
			};
		}
		if (view) {
			obj.view = view;
		}
		return StructrWS.sendObj(obj, callback);
	},
	getPromise: (id, properties, view) => {
		return new Promise((resolve, reject) => {
			Command.get(id, properties, resolve, view);
		});
	},
	/**
	 * Send a CONSOLE command with a single line as payload to the server.
	 *
	 * The server will return the result returned from the underlying
	 * console infrastructure to the sending client (no broadcast).
	 */
	console: function(line, mode, callback, completion) {
		let obj = {
			command: 'CONSOLE',
			data: {
				line: line,
				mode: mode,
				completion: (completion === true ? true : false)
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a single GET command to the server.
	 *
	 * The server will return a single relationship with all properties
	 * with the given id to the sending client (no broadcast).
	 *
	 * Providing a nodeId is strongly recommended.
	 */
	getRelationship: function(id, nodeId, properties, callback, view) {

		if (!nodeId) {
			console.warn('getRelationship called without nodeId');
		}

		let obj = {
			command: 'GET_RELATIONSHIP',
			id: id,
			data: {
				nodeId: nodeId
			}
		};
		if (properties !== null) {
			obj.relData = { 'properties': properties };
		}
		if (view) {
			obj.view = view;
		}
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a single GET_BY_TYPE command to the server.
	 *
	 * The server will return a list of nodes of the given type.
	 *
	 * The optional callback function will be executed for each node in the result set.
	 */
	getByType: function(type, pageSize, page, sort, order, properties, includeHidden, callback) {
		let obj = {
			command: 'GET_BY_TYPE',
			data: {
				type: type,
				includeHidden: includeHidden
			}
		};
		if (pageSize) obj.pageSize = pageSize;
		if (page) obj.page = page;
		if (sort) obj.sort = sort;
		if (order) obj.order = order;
		if (properties) obj.data.properties = properties;
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a single GET_TYPE_INFO command to the server.
	 *
	 * The server will return a collection containing a single schema node
	 * with all relevant properties of the node with the given type to the
	 * sending client (no broadcast).
	 */
	getTypeInfo: function(type, callback) {
		let obj = {
			command: 'GET_TYPE_INFO',
			data: {
				type: type
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a single GET_SCHEMA_INFO command to the server.
	 *
	 * The server will return a schema overview with all relevant properties.
	 */
	getSchemaInfo: function(type, callback) {
		let obj = {
			command: 'GET_SCHEMA_INFO',
			data: {
				type: type
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a LIST command to the server.
	 *
	 * The server will return a result set containing all items of the given
	 * type which are not children of another node to the sending client (no broadcast).
	 *
	 * The optional callback function will be executed with the result set as parameter.
	 */
	list: function(type, rootOnly, pageSize, page, sort, order, properties, callback) {
		let obj = {
			command: 'LIST',
			pageSize: pageSize,
			page: page,
			sort: sort,
			order: order,
			data: {
				type: type,
				rootOnly: rootOnly
			}
		};
		if (properties) obj.data.properties = properties;
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a QUERY command to the server.
	 *
	 * The server will return a result set containing all query results that match the
	 * given type and property values.
	 *
	 * The optional callback function will be executed with the result set as parameter.
	 */
	query: function(type, pageSize, page, sort, order, properties, callback, exact, view, customView) {
		let obj = {
			command: 'QUERY',
			pageSize: pageSize,
			page: page,
			sort: sort,
			order: order,
			data: {
				type: type
			}
		};
		if (properties) {
			const andProperties = {};
			const notProperties = {};
			for (const key of Object.keys(properties)) {
				if (key.startsWith('!')) {
					notProperties[key.substring(1)] = properties[key];
				} else {
					andProperties[key] = properties[key];
				}
			}
			obj.data.properties    = JSON.stringify(andProperties);
			obj.data.notProperties = JSON.stringify(notProperties);
		}
		if (exact !== null) obj.data.exact = exact;
		if (view) obj.view = view;
		if (customView) obj.data.customView = customView;
		return StructrWS.sendObj(obj, callback);
	},
	queryPromise: (type, pageSize, page, sort, order, properties, exact, view, customView) => {

		return new Promise((resolve, reject) => {
			Command.query(type, pageSize, page, sort, order, properties, resolve, exact, view, customView);
		});
	},
	/**
	 * Send a CHILDREN or DOM_NODE_CHILDREN command to the server.
	 *
	 * The server will return a result set containing all children of the
	 * node with the given id to the sending client (no broadcast).
	 *
	 * The optional callback function will be executed with the result set as parameter.
	 */
	children: function(id, callback) {
		let obj = {
			id: id
		};

		let structrObj = StructrModel.obj(id);
		if (structrObj && (structrObj instanceof StructrElement || structrObj.type === 'Template')) {
			obj.command = 'DOM_NODE_CHILDREN';
		} else {
			obj.command = 'CHILDREN';
		}

		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a GET command to the server.
	 *
	 * The server will return the value of the property with the given key
	 * of the node with the given id to the sending client (no broadcast).
	 */
	getProperty: function(id, key, callback) {
		let obj = {
			command: 'GET_PROPERTY',
			id: id,
			data: {
				key: key
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send an SEARCH command to the server.
	 *
	 * The server will search for nodes containing the
	 * search string in their name, or being their id.
	 *
	 * If type is given, the search will be filtered to nodes
	 * of that type.
	 *
	 */
	search: function(searchString, type, exact, callback) {
		let obj = {
			command: 'SEARCH',
			data: {
				searchString: searchString,
				type: type,
				exact: exact
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a REST query by SEARCH command to the server.
	 *
	 */
	rest: function(searchString, callback) {
		let obj = {
			command: 'SEARCH',
			data: {
				restQuery: searchString
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a Cypher query by SEARCH command to the server.
	 *
	 */
	cypher: function(query, params, callback, pageSize, page) {
		let obj = {
			command: 'SEARCH',
			data: {
				cypherQuery: query,
				cypherParams: params
			}
		};
		if (pageSize && page) {
			obj.pageSize = pageSize;
			obj.page = page;
		}
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a DELETE command to the server.
	 *
	 * The server will delete the node with the given id and broadcast
	 * a deletion notification.
	 */
	deleteNode: function(id, recursive, callback) {
		let obj = {
			command: 'DELETE',
			id: id,
			data: {}
		};
		if (recursive) obj.data.recursive = recursive;
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a DELETE_NODES command to the server.
	 *
	 * The server will delete the nodes with the given ids and broadcast
	 * a deletion notification.
	 */
	deleteNodes: function(ids, recursive) {
		let obj = {
			command: 'DELETE_NODES',
			data: {
				nodeIds: ids
			}
		};
		if (recursive) obj.data.recursive = recursive;
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send a DELETE_RELATIONSHIP command to the server.
	 *
	 * The server will delete the relationship with the given id and broadcast
	 * a deletion notification.
	 */
	deleteRelationship: function(id, recursive) {
		let obj = {
			command: 'DELETE_RELATIONSHIP',
			id: id,
			data: {}
		};
		if (recursive) obj.data.recursive = recursive;
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send a REMOVE command to the server.
	 *
	 * The server will remove the node with the given sourceId from the node
	 * with the given targetId and broadcast a removal notification.
	 */
	removeSourceFromTarget: function(entityId, parentId) {
		let obj = {
			command: 'REMOVE',
			id: entityId,
			data: {
				id: parentId
			}
		};
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send a REMOVE command to the server.
	 *
	 * The server will remove the node from the
	 * tree and broadcast a removal notification.
	 */
	removeChild: function(id, callback) {
		let obj = {
			command: 'REMOVE',
			id: id,
			data: {}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a REMOVE_FROM_COLLECTION command to the server.
	 *
	 * The server will remove the object with the given idToRemove from the
	 * collection property with the given key of the object with the given id.
	 */
	removeFromCollection: function(id, key, idToRemove, callback) {
		let obj = {
			command: 'REMOVE_FROM_COLLECTION',
			id: id,
			data: {
				key: key,
				idToRemove: idToRemove
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send an UPDATE command to the server.
	 *
	 * The server will set the given value as new value of the property with
	 * the given key for the node with the given id and broadcast an update
	 * notification.
	 *
	 * If recursive is set to true, the property will be set on all subnodes, too.
	 */
	setProperty: function(id, key, value, recursive, callback) {
		let obj = {
			command: 'UPDATE',
			id: id,
			data: {}
		};
		obj.data[key] = value;
		if (recursive) obj.data.recursive = true;
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send an UPDATE command to the server.
	 *
	 * The server will set the properties contained in the 'data' on the node
	 * with the given id and broadcast an update notification.
	 */
	setProperties: function(id, data, callback) {
		let obj = {
			command: 'UPDATE',
			id: id,
			data: data
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a SET_PERMISSIONS command to the server.
	 *
	 * The server will set the permission contained in the 'data' on the node
	 * with the given id and broadcast an update notification.
	 */
	setPermission: function(id, principalId, action, permissions, recursive, callback) {
		let obj = {
			command: 'SET_PERMISSION',
			id: id,
			data: {
				principalId: principalId,
				action: action,
				permissions: permissions,
				recursive: recursive
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send an APPEND_CONTENT_ITEM command to the server.
	 */
	appendContentItem: function(id, parentId, callback) {
		let obj = {
			command: 'APPEND_CONTENT_ITEM',
			id: id,
			data: {
				parentId: parentId
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send an UNARCHIVE command to the server.
	 *
	 * The server will unarchive the file with the given id in the folder
	 * with the given parent folder id and create files for each archive entry.
	 *
	 */
	unarchive: function(id, parentFolderId, callback) {
		let obj = {
			command: 'UNARCHIVE',
			id: id,
			data: {
				parentFolderId: parentFolderId
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send an APPEND_MEMBER command to the server.
	 *
	 * The server will append the user or group node with the given id
	 * as child of the parent group node with the given group id.
	 *
	 */
	appendMember: function(id, groupId, callback) {
		let obj = {
			command: 'APPEND_MEMBER',
			id: id,
			data: {
				parentId: groupId
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send an APPEND_CHILD command to the server.
	 *
	 * The server will append the node with the given id
	 * as child of the node with the given parent id.
	 *
	 * If the node was child of a parent before, it will be
	 * removed from the former parent before being appended
	 * to the new one.
	 *
	 */
	appendChild: async (id, parentId, key, callback) => {

		return new Promise((resolve => {
			let obj = {
				command: 'APPEND_CHILD',
				id: id,
				data: {
					parentId: parentId,
					key: key
				}
			};

			return StructrWS.sendObj(obj, resolve);
		}));
	},
	/**
	 * Send an APPEND_WIDGET command to the server.
	 *
	 * The server will create nodes from the given source and
	 * append them as children of the node with the given parent id.
	 *
	 * If the node was child of a parent before, it will be
	 * removed from the former parent before being appended
	 * to the new one.
	 *
	 */
	appendWidget: function(source, parentId, pageId, widgetHostBaseUrl, attributes, processDeploymentInfo, callback) {
		let obj = {
			command: 'APPEND_WIDGET',
			pageId: pageId,
			data: {
				widgetHostBaseUrl: widgetHostBaseUrl,
				parentId: parentId,
				source: source,
				processDeploymentInfo: (processDeploymentInfo || false)
			}
		};
		if (attributes) {
			$.extend(obj.data, attributes);
		}
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a SAVE_NODE command to the server.
	 *
	 * The server will modify the existing node based on the differences
	 * to the original node.
	 *
	 */
	saveNode: function(source, id, callback) {
		let obj = {
			command: 'SAVE_NODE',
			id: id,
			data: {
				source: source
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a SAVE_LOCAL_STORAGE command to the server.
	 *
	 * The server will save the given string as 'localStorage' attribute
	 * of the current user.
	 *
	 */
	saveLocalStorage: function(callback) {
		let obj = {
			command: 'SAVE_LOCAL_STORAGE',
			data: {
				localStorageString: LSWrapper.getAsJSON()
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a GET_LOCAL_STORAGE command to the server.
	 */
	getLocalStorage: function(callback) {
		let obj = {
			command: 'GET_LOCAL_STORAGE'
		};
		return StructrWS.sendObj(obj, callback);
	},
	// /**
	//  * Send an INSERT_BEFORE command to the server.
	//  *
	//  * The server will insert the DOM node with the given id
	//  * after the node with the given refId as child of the node
	//  * with the given parentId.
	//  *
	//  * If the node was in the tree before, it will be
	//  * removed from the former parent before being inserted.
	//  *
	//  */
	// insertBefore: function(parentId, id, refId) {
	// 	let obj = {
	// 		command: 'INSERT_BEFORE',
	// 		id: id,
	// 		data: {
	// 			refId: refId,
	// 			parentId: parentId
	// 		}
	// 	};
	// 	return StructrWS.sendObj(obj);
	// },
	/**
	 * Send an INSERT_RELATIVE_TO_DOM_NODE command to the server.
	 *
	 * The server will insert the DOM node with the given id
	 * relative to the node with the given refId.
	 *
	 * If the node was in the tree before, it will be
	 * removed from the former parent before being inserted.
	 */
	insertRelative: async (id, parentId, refId, relativePosition) => {

		return new Promise(((resolve, reject) => {

			let obj = {
				command: 'INSERT_RELATIVE_TO_DOM_NODE',
				id: id,
				data: {
					refId: refId,
					parentId: parentId,
					relativePosition: relativePosition
				}
			};

			StructrWS.sendObj(obj, resolve);
		}));
	},
	/**
	 * Send a CREATE_AND_ADD_DOM_ELEMENT command to the server.
	 *
	 * The server will create a new DOM node with the given tag name and
	 * append it as child of the node with the given parent id.
	 *
	 * If tagName is omitted (undefined, null or empty), the server
	 * will create a content (#text) node.
	 *
	 */
	createAndAppendDOMNode: (pageId, parentId, tagName, attributes, inheritVisibilityFlags, inheritGrantees) => {
		let obj = {
			command: 'CREATE_AND_APPEND_DOM_NODE',
			pageId: pageId,
			data: {
				parentId: parentId,
				tagName: tagName,
				inheritVisibilityFlags: (inheritVisibilityFlags || false),
				inheritGrantees: (inheritGrantees || false)
			}
		};
		$.extend(obj.data, attributes);
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send a CREATE_AND_INSERT_RELATIVE_TO_DOM_NODE command to the server.
	 *
	 * The server will create a new DOM node with the given tag name and
	 * insert it relative to the node with the given id.
	 *
	 */
	createAndInsertRelativeToDOMNode: (pageId, nodeId, tagName, attributes, relativePosition, inheritVisibilityFlags, inheritGrantees) => {
		let obj = {
			command: 'CREATE_AND_INSERT_RELATIVE_TO_DOM_NODE',
			pageId: pageId,
			data: {
				nodeId: nodeId,
				tagName: tagName,
				inheritVisibilityFlags: (inheritVisibilityFlags || false),
				inheritGrantees: (inheritGrantees || false),
				relativePosition: relativePosition
			}
		};

		$.extend(obj.data, attributes);
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send a REPLACE_WITH command to the server.
	 *
	 * The server will create a new DOM node with the given tag name and
	 * replace the current node with the newly created node, copying all
	 * the attributes etc.
	 *
	 */
	replaceWith: (pageId, nodeId, tagName, attributes, inheritVisibilityFlags, inheritGrantees, callback) => {
		let obj = {
			command: 'REPLACE_WITH',
			pageId: pageId,
			data: {
				nodeId: nodeId,
				tagName: tagName,
				inheritVisibilityFlags: (inheritVisibilityFlags || false),
				inheritGrantees: (inheritGrantees || false)
			}
		};

		$.extend(obj.data, attributes);
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a WRAP_DOM_NODE command to the server.
	 *
	 * The server will create a new DOM node with the given tag name and
	 * wrap the node with the given nodeId in it.
	 *
	 */
	wrapDOMNodeInNewDOMNode: (pageId, nodeId, tagName, attributes, inheritVisibilityFlags, inheritGrantees) => {
		let obj = {
			command: 'WRAP_DOM_NODE',
			pageId: pageId,
			data: {
				nodeId: nodeId,
				tagName: tagName,
				inheritVisibilityFlags: (inheritVisibilityFlags || false),
				inheritGrantees: (inheritGrantees || false)
			}
		};
		$.extend(obj.data, attributes);
		return StructrWS.sendObj(obj);
	},
	wrapContent: function(pageId, parentId, tagName) {
		let obj = {
			command: 'WRAP_CONTENT',
			pageId: pageId,
			data: {
				parentId: parentId,
				tagName: tagName
			}
		};
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send a CREATE_COMPONENT command to the server.
	 *
	 * The server will transform the node into a reusable component.
	 *
	 */
	createComponent: function(id, callback) {
		let obj = {
			command: 'CREATE_COMPONENT',
			id: id
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a CLONE_COMPONENT command to the server.
	 *
	 * The server will clone the component node with the given id
	 * and append it to a the parent with given parentId.
	 *
	 */
	cloneComponent: async (id, parentId, refId, relativePosition) => {

		return new Promise(((resolve) => {

			let obj = {
				command: 'CLONE_COMPONENT',
				id: id,
				data: {
					parentId: parentId,
					refId: refId,
					relativePosition: relativePosition
				}
			};

			return StructrWS.sendObj(obj, resolve);
		}));
	},
	/**
	 * Send a REPLACE_TEMPLATE command to the server.
	 *
	 * The server will replace the template node with the given id
	 * with the template node with given newTemplateId.
	 *
	 */
	replaceTemplate: function(id, newTemplateId, callback) {
		let obj = {
			command: 'REPLACE_TEMPLATE',
			id: id,
			data: {
				newTemplateId: newTemplateId
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a CREATE_LOCAL_WIDGET command to the server.
	 *
	 * The server will create a local widget element with the given
	 * name and source code.
	 */
	createLocalWidget: function(id, name, source, callback) {
		let obj = {
			command: 'CREATE_LOCAL_WIDGET',
			id: id,
			data: {
				name: name,
				source: source
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a CLONE_NODE command to the server.
	 *
	 * The server will clone the DOM node with the given id
	 * and append it to a the parent with given parentId.
	 *
	 * NEW: The server will clone the DOM node with the given id
	 * after the node with the given refId as child of the node
	 * with the given parentId.
	 */
	cloneNode: async (id, parentId, deep, refId, relativePosition) => {

		return new Promise((resolve => {

			let obj = {
				command: 'CLONE_NODE',
				id: id,
				data: {
					parentId: parentId,
					deep: deep,
					refId: refId,
					relativePosition: relativePosition
				}
			};

			return StructrWS.sendObj(obj, resolve);
		}));
	},
	/**
	 * Send a SYNC_MODE command to the server.
	 *
	 * The server set the mode for synchronization
	 * between source and target node to the
	 * given value.
	 *
	 * Internally, SYNC relationships will be created.
	 *
	 */
	setSyncMode: function(id, targetId, mode) {
		let obj = {
			command: 'SYNC_MODE',
			id: id,
			data: {
				targetId: targetId,
				syncMode: mode
			}
		};
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send an ADD command to the server.
	 *
	 * The server will do one of the following:
	 *
	 * Add the node with the given id to the children of the node with the
	 * id given as 'id' property of the 'nodeData' hash. If the 'relData'
	 * contains a page id in both property fields 'sourcePageId' and
	 * 'targetPageId', the node will be copied from the source to the
	 * target page,
	 *
	 * - or -
	 *
	 * If 'id' is null, create a new node with the properties contained
	 * in the 'nodeData' has and use the relationship parameters from the
	 * 'relData' hash.
	 *
	 * When finished, the server will broadcast an ADD and eventually
	 * a CREATE notification.
	 */
	createAndAdd: function(id, nodeData, relData) {
		let obj = {
			command: 'ADD',
			id: id,
			data: nodeData,
			relData: relData
		};
		if (!obj.data.name) {
			obj.data.name = `New ${obj.data.type} ${Math.floor(Math.random() * (999999 - 1))}`;
		}
		if (obj.data.isContent && !obj.data.content) {
			obj.data.content = obj.data.name;
		}
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send a CREATE command to the server.
	 *
	 * The server will create a new node with the given properties contained
	 * in the 'nodeData' hash and broadcast a CREATE notification.
	 */
	create: function(nodeData, callback) {
		let obj = {
			command: 'CREATE',
			data: nodeData
		};
		if (!obj.data.name) {
			obj.data.name = `New ${obj.data.type} ${Math.floor(Math.random() * (999999 - 1))}`;
		}
		if (obj.data.isContent && !obj.data.content) {
			obj.data.content = obj.data.name;
		}
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a CREATE_RELATIONSHIP command to the server.
	 *
	 * The server will create a new relationship with the given properties contained
	 * in the 'relData' hash and broadcast a CREATE_RELATIONSHIP notification.
	 */
	createRelationship: function(relData, callback) {
		let obj = {
			command: 'CREATE_RELATIONSHIP',
			relData: relData
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a CREATE_SIMPLE_PAGE command to the server.
	 *
	 * The server will create a simple HTML page with head, body
	 * and title element and broadcast a CREATE notification.
	 */
	createSimplePage: function() {
		let obj = {
			command: 'CREATE_SIMPLE_PAGE',
			data: {
				name: 'New Page ' + Math.floor(Math.random() * (999999 - 1))
			}
		};
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send an IMPORT command to the server.
	 *
	 * This command will trigger the server-side importer to start with
	 * parsing the given source code or importing the page data from the given address.
	 * If successful, the server will add a new page with the given name
	 * and make it visible for public or authenticated users.
	 *
	 * The server will broadcast CREATE and ADD notifications for each
	 * node respective relationship created.
	 */
	importPage: function(code, address, name, publicVisible, authVisible, includeInExport, processDeploymentInfo) {
		let obj = {
			command: 'IMPORT',
			data: {
				code: code,
				address: address,
				name: name,
				publicVisible: publicVisible,
				authVisible: authVisible,
				includeInExport: includeInExport,
				processDeploymentInfo: processDeploymentInfo
			}
		};
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send an IMPORT command to the server.
	 *
	 * This command will trigger the server-side importer to start with
	 * parsing the given source code or importing the page data from the given address.
	 * If successful, the server will add a new page with the given name which contains a single Template node
	 * and make it visible for public or authenticated users.
	 */
	importPageAsTemplate: function(address, name, templateName) {
		let obj = {
			command: 'IMPORT',
			data: {
				code: null,
				address: address,
				name: name,
				publicVisible: false,
				authVisible: false,
				includeInExport: false,
				processDeploymentInfo: false,
				withTemplate: true,
				templateName: templateName
			}
		};
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send a PATCH command to the server.
	 *
	 * This will change the text contained in the 'content' property of the
	 * content node with the given id by a patch calculated from the
	 * difference between the given texts.
	 *
	 * The server will broadcast an UPDATE notification.
	 */
	patch: function(id, text1, text2, callback) {
		// no null values allowed
		if (!text1) {
			text1 = '';
		}
		if (!text2) {
			text2 = '';
		}

		let p    = Structr.getDiffMatchPatch().patch_make(text1, text2);
		let strp = Structr.getDiffMatchPatch().patch_toText(p);

		let obj = {
			command: 'PATCH',
			id: id,
			data: {
				patch: strp
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a CLONE command to the server.
	 *
	 * This will create a new page and copy all children of the page
	 * with the given id so that they become children of the newly created
	 * page, too.
	 *
	 * The server will broadcast a CREATE and an ADD notification.
	 */
	clonePage: function(id) {
		let obj = {
			command: 'CLONE_PAGE',
			id: id,
			data: {
				name: 'New Page ' + Math.floor(Math.random() * (999999 - 1))
			}
		};
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send a CHUNK command to the server.
	 *
	 * For each chunk, a chunk command is sent with the given chunk id, chunk
	 * size (in bytes), and the chunk content.
	 *
	 * The server collects and concatenates all data chunks to the binary content
	 * of the file node with the given id.
	 *
	 * The server gives no feedback on a CHUNK command.
	 */
	chunk: function(id, chunkId, chunkSize, chunk, chunks, callback) {
		let obj = {
			command: 'CHUNK',
			id: id,
			data: {
				chunkId: chunkId,
				chunkSize: chunkSize,
				chunk: chunk,
				chunks: chunks
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a CREATE command to the server.
	 *
	 * This will create a file with the given properties.
	 */
	createFile: function(file, callback) {
		let obj = {
			command: 'CREATE',
			data: {
				contentType: file.type,
				name: file.name,
				size: file.size,
				parent: file.parent,
				hasParent: file.hasParent,
				parentId: file.parentId,
				type: (_Helpers.isImage(file.type) ? 'Image' : (_Helpers.isVideo(file.type) && Structr.isModulePresent('media')) ? 'VideoFile' : 'File')
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send an UPLOAD command to the server.
	 *
	 *
	 */
	upload: function(name, fileData) {
		let obj = {
			command: 'UPLOAD',
			data: {
				name: name,
				fileData: fileData
			}
		};
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send a LINK command to the server.
	 *
	 * The server will establish a relationship from the node with the given
	 * id to the page with the given page id.
	 *
	 * The server gives no feedback on a LINK command.
	 */
	link: function(id, targetId) {
		let obj = {
			command: 'LINK',
			id: id,
			data: {
				targetId: targetId
			}
		};
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send a LIST_ACTIVE_ELEMENTS command to the server.
	 *
	 * The server will return a result set containing all active elements
	 * in the given page.
	 *
	 * The optional callback function will be executed for each node in the result set.
	 */
	// listActiveElements: function(pageId, callback) {
	// 	let obj = {
	// 		command: 'LIST_ACTIVE_ELEMENTS',
	// 		id: pageId
	// 	};
	// 	return StructrWS.sendObj(obj, callback);
	// },
	listLocalizations: (pageId, locale, detailObjectId, queryString) => {
		return new Promise((resolve, reject) => {
			let obj = {
				command: 'LIST_LOCALIZATIONS',
				id: pageId,
				data: {
					locale: locale,
					detailObjectId: detailObjectId,
					queryString: queryString
				}
			};

			StructrWS.sendObj(obj, resolve);
		});
	},
	/**
	 * Send a LIST_COMPONENTS command to the server.
	 *
	 * The server will return a result set containing all element nodes
	 * which are used in more than one page to the sending client (no broadcast).
	 *
	 * The optional callback function will be executed for each node in the result set.
	 */
	listComponents: function(pageSize, page, sort, order, callback) {
		let obj = {
			command: 'LIST_COMPONENTS',
			pageSize: pageSize,
			page: page,
			sort: sort,
			order: order
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a LIST_UNATTACHED_NODES command to the server.
	 *
	 * The server will return a result set containing all DOM nodes
	 * which are not connected to a parent node to the sending client (no broadcast).
	 *
	 * The optional callback function will be executed for each node in the result set.
	 */
	listUnattachedNodes: function(pageSize, page, sort, order, callback) {
		let obj = {
			command: 'LIST_UNATTACHED_NODES',
			pageSize: pageSize,
			page: page,
			sort: sort,
			order: order
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a DELETE_UNATTACHED_NODES command to the server.
	 *
	 * The server will delete all DOM nodes
	 * which are not connected to a parent node.
	 *
	 * No broadcast.
	 */
	deleteUnattachedNodes: function() {
		let obj = {
			command: 'DELETE_UNATTACHED_NODES'
		};
		return StructrWS.sendObj(obj);
	},
	/**
	 * Send a LIST_SCHEMA_PROPERTIES command to the server.
	 *
	 * No broadcast.
	 */
	listSchemaProperties: function(id, view, callback) {
		let obj  = {
			command: 'LIST_SCHEMA_PROPERTIES',
			id: id,
			data: {
				view: view
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a SNAPSHOTS command to the server.
	 *
	 * The server will return a status object.
	 */
	snapshots: function(mode, name, types, callback) {
		let obj  = {
			command: 'SNAPSHOTS',
			data: {
				mode: mode,
				name: name
			}
		};
		if (types && types.length) {
			obj.data.types = types.join(',');
		}
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send an AUTOCOMPLETE command to the server.
	 *
	 * No broadcast.
	 */
	autocomplete: function(id, isAutoscriptEnv, before, after, line, cursorPosition, contentType, callback) {
		let obj  = {
			command: 'AUTOCOMPLETE',
			id: id,
			data: {
				before: before,
				after: after,
				contentType: contentType,
				line: line,
				cursorPosition: cursorPosition,
				isAutoscriptEnv: isAutoscriptEnv
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a FIND_DUPLICATES command to the server.
	 *
	 * The server will return a list of all files with identical paths
	 */
	findDuplicates: function(callback) {
		let obj  = {
			command: 'FIND_DUPLICATES'
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a FAVORITES command to the server.
	 *
	 * Depending on the mode the server will either add/remove the file to/from
	 * the users favorite files.
	 */
	favorites: function(mode, fileId, callback) {
		let obj  = {
			command: 'FAVORITES',
			data: {
				mode: mode,
				id: fileId
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a CONVERT_IMAGE command to the server.
	 *
	 * Depending on the mode the server will either add/remove the file to/from
	 * the users favorite files.
	 */
	createConvertedImage: function(originalImageId, width, height, format, offsetX, offsetY, callback) {
		let obj  = {
			command: 'CONVERT_IMAGE',
			id: originalImageId,
			data: {
				format: format,
				width: width,
				height: height,
				offsetX: offsetX,
				offsetY: offsetY
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Send a FILE_IMPORT command to the server.
	 *
	 * Depending on the mode the server will either list/start/pause/resume/cancel
	 * file imports
	 */
	fileImport: function(mode, jobId, callback) {
		let obj  = {
			command: 'FILE_IMPORT',
			data: {
				mode: mode,
				jobId: jobId
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
	 * Shortcut to create an ApplicationConfigurationDataNode
	 */
	createApplicationConfigurationDataNode: function(configType, name, content, callback) {
		Command.create({
			type: 'ApplicationConfigurationDataNode',
			name: name,
			configType: configType,
			content: content
		}, callback);
	},
	/**
	 * Shortcut to get all ApplicationConfigurationDataNodes for a specific configType
	 */
	getApplicationConfigurationDataNodes: function(configType, customView, callback) {
		return Command.query('ApplicationConfigurationDataNode', 1000, 1, 'name', true, { configType: configType }, callback, true, null, customView);
	},
	/**
	 * Shortcut to get all ApplicationConfigurationDataNodes for a specific configType grouped by user
	 */
	getApplicationConfigurationDataNodesGroupedByUser: function(configType, callback) {
		return Command.query('ApplicationConfigurationDataNode', 1000, 1, 'name', true, { configType: configType }, function(data) {

			let grouped          = {};
			let ownerlessConfigs = [];

			for (let n of data) {
				if (n.owner) {
					if (!grouped[n.owner.name]) {
						grouped[n.owner.name] = [];
					}
					grouped[n.owner.name].push(n);
				} else {
					ownerlessConfigs.push(n);
				}
			}

			let ownerNames = Object.keys(grouped);
			ownerNames.sort();

			let sortedAndGrouped = [];

			// add current users config first
			let myIndex = ownerNames.indexOf(StructrWS.me.username);
			if (myIndex !== -1) {
				ownerNames.splice(myIndex,1);

				sortedAndGrouped.push({
					label: StructrWS.me.username,
					ownerless: false,
					configs: grouped[StructrWS.me.username]
				});
			}

			// add ownerless configs
			if (ownerlessConfigs.length > 0) {
				sortedAndGrouped.push({
					label: 'Layouts without owner',
					ownerless: true,
					configs: ownerlessConfigs
				});
			}

			// add the other configs grouped by owner and sorted by owner name
			for (let on of ownerNames) {
				sortedAndGrouped.push({
					label: on,
					ownerless: false,
					configs: grouped[on]
				});
			}

			callback(sortedAndGrouped);

		}, true, null, 'id,name,owner');
	},
	/**
	 * Shortcut to get a single ApplicationConfigurationDataNode
	 */
	getApplicationConfigurationDataNode: function(id, callback) {
		return Command.get(id, 'name,content', callback);
	},
	/**
	 * Send a GET_SUGGESTIONS command to the server.
	 *
	 * This command send id, name, tag and a list of
	 * CSS classes to the server to obtain a list of widget-like
	 * templates that the user can choose from.
	 *
	 */
	getSuggestions: function(id, name, tag, classes, callback) {
		let obj  = {
			command: 'GET_SUGGESTIONS',
			data: {
				htmlId: id,
				name: name,
				tag: tag,
				classes: classes
			}
		};
		return StructrWS.sendObj(obj, callback);
	},
	/**
     * Requests log snapshot from the server.
     */
    getServerLogSnapshot: (numberOfLines, truncateLinesAfter) => {

		return new Promise((resolve, reject) => {

			let obj  = {
				command: 'SERVER_LOG',
				data: {
					numberOfLines:      numberOfLines,
					truncateLinesAfter: truncateLinesAfter
				}
			};

			StructrWS.sendObj(obj, resolve);
		});
    }
};
