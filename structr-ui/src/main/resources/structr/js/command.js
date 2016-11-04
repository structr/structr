/*
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * Use these commands to send command calls to the websocket server.
 *
 * The websocket listener is in the websocket.js file.
 *
 */
var Command = {
	/**
	 * Send the LOGIN command to the server.
	 */
	login: function(username, password) {
		var obj = {};
		obj.command = 'LOGIN';
		obj.sessionId = Structr.getSessionId();
		var data = {};
		data.username = username;
		data.password = password;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'login()' + username + ' with ' + password);
		return sendObj(obj);
	},
	/**
	 * Send the LOGOUT command to the server.
	 */
	logout: function(username) {
		var obj = {};
		obj.command = 'LOGOUT';
		obj.sessionId = Structr.getSessionId();
		var data = {};
		data.username = username;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'logout() ' + username);
		return sendObj(obj);
	},
	/**
	 * Send the PING command to the server.
	 */
	ping: function(callback) {
		var obj = {};
		obj.command = 'PING';
		obj.sessionId = Structr.getSessionId();
		_Logger.log(_LogType.WS[obj.command], 'ping()');
		return sendObj(obj, callback);
	},
	/**
	 * Send a single GET command to the server.
	 *
	 * The server will return a list containing a single node with all properties
	 * of the node with the given id to the sending client (no broadcast).
	 */
	get: function(id, callback) {
		var obj = {};
		obj.command = 'GET';
		obj.id = id;
		//var data = {};
		//data.id = id;
		//obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'get()', obj, callback);
		return sendObj(obj, callback);
	},
	/**
	 * Send a CONSOLE command with a single line as payload to the server.
	 *
	 * The server will return the result returned from the underlying
	 * console infrastructure to the sending client (no broadcast).
	 */
	console: function(line, callback, completion) {
		var obj = {};
		obj.command = 'CONSOLE';
		var data = {};
		data.line = line;
		if (completion) {
			data.completion = true;
		}
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'console()', obj, callback);
		return sendObj(obj, callback);
	},
	/**
	 * Send a single GET command to the server.
	 *
	 * The server will return a single relationship with all properties
	 * with the given id to the sending client (no broadcast).
	 */
	getRelationship: function(id, callback) {
		var obj = {};
		obj.command = 'GET_RELATIONSHIP';
		obj.id = id;
		//var data = {};
		//data.id = id;
		//obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'getRelationship()', obj, callback);
		return sendObj(obj, callback);
	},
	/**
	 * Send a single GET_BY_TYPE command to the server.
	 *
	 * The server will return a list of nodes of the given type.
	 *
	 * The optional callback function will be executed for each node in the result set.
	 */
	getByType: function(type, pageSize, page, sort, order, properties, includeDeletedAndHidden, callback) {
		var obj = {};
		obj.command = 'GET_BY_TYPE';
		var data = {};
		data.type = type;
		if (pageSize) obj.pageSize = pageSize;
		if (page) obj.page = page;
		if (sort) obj.sort = sort;
		if (order) obj.order = order;
		if (properties) data.properties = properties;
		data.includeDeletedAndHidden = includeDeletedAndHidden;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'getByType()', obj, callback);
		return sendObj(obj, callback);
	},
	/**
	 * Send a single GET_TYPE_INFO command to the server.
	 *
	 * The server will return a single schema node with all relevant properties
	 * of the node with the given type to the sending client (no broadcast).
	 */
	getTypeInfo: function(type, callback) {
		var obj = {};
		obj.command = 'GET_TYPE_INFO';
		var data = {};
		data.type = type;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'getTypeInfo()', obj, callback);
		return sendObj(obj, callback);
	},
	/**
	 * Send a single GET_SCHEMA_INFO command to the server.
	 *
	 * The server will return a schema overview with all relevant properties.
	 */
	getSchemaInfo: function(type, callback) {
		var obj = {};
		var data = {};
		data.type = type;
		obj.data = data;
		obj.command = 'GET_SCHEMA_INFO';
		_Logger.log(_LogType.WS[obj.command], 'getSchemaInfo()', obj, callback);
		return sendObj(obj, callback);
	},
	/**
	 * Send a LIST command to the server.
	 *
	 * The server will return a result set containing all items of the given
	 * type which are not children of another node to the sending client (no broadcast).
	 *
	 * The optional callback function will be executed for each node in the result set.
	 */
	list: function(type, rootOnly, pageSize, page, sort, order, properties, callback) {
		var obj = {};
		obj.command = 'LIST';
		var data = {};
		data.type = type;
		data.rootOnly = rootOnly;
		if (properties) data.properties = properties;
		obj.pageSize = pageSize;
		obj.page = page;
		obj.sort = sort;
		obj.order = order;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'list()', obj, callback);
		return sendObj(obj, callback);
	},
	/**
	 * Send a QUERY command to the server.
	 *
	 * The server will return a result set containing all query results that match the
     * given type and property values.
	 *
	 * The optional callback function will be executed for each node in the result set.
	 */
	query: function(type, pageSize, page, sort, order, properties, callback, exact, view) {
		var obj = {};
		obj.command = 'QUERY';
		var data = {};
		data.type = type;
		if (properties) data.properties = JSON.stringify(properties);
		if (exact != null) data.exact = exact;
		obj.pageSize = pageSize;
		obj.page = page;
		obj.sort = sort;
		obj.order = order;
		if (view) obj.view = view;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'query()', obj, callback);
		return sendObj(obj, callback);
	},
	/**
	 * Send a CHILDREN or DOM_NODE_CHILDREN command to the server.
	 *
	 * The server will return a result set containing all children of the
	 * node with the given id to the sending client (no broadcast).
	 *
	 * The optional callback function will be executed for each node in the result set.
	 */
	children: function(id, callback) {
		var obj = {};
		obj.id = id;
		var data = {};

		var structrObj = StructrModel.obj(id);
		if (structrObj && (structrObj instanceof StructrElement || structrObj.type === 'Template')) {
			obj.command = 'DOM_NODE_CHILDREN';
			_Logger.log(_LogType.WS[obj.command], 'children of DOM node requested', structrObj);
		} else {
			obj.command = 'CHILDREN';
		}

		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'children()', obj, callback);
		return sendObj(obj, callback);
	},
	/**
	 * Send a GET command to the server.
	 *
	 * The server will return the value of the property with the given key
	 * of the node with the given id to the sending client (no broadcast).
	 */
	getProperty: function(id, key, callback) {
		var obj = {};
		obj.command = 'GET_PROPERTY';
		obj.id = id;
		var data = {};
		data.key = key;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'getProperty()', obj, callback);
		return sendObj(obj, callback);
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
	search: function(searchString, type, callback) {
		var obj = {};
		obj.command = 'SEARCH';
		var data = {};
		data.searchString = searchString;
		data.type = type;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'search()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send a REST query by SEARCH command to the server.
	 *
	 */
	rest: function(searchString, callback) {
		var obj = {};
		obj.command = 'SEARCH';
		var data = {};
		data.restQuery = searchString;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'rest()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send a Cypher query by SEARCH command to the server.
	 *
	 */
	cypher: function(query, params, callback, pageSize, page) {
		var obj = {};
		obj.command = 'SEARCH';
		var data = {};
		data.cypherQuery = query;
		data.cypherParams = params;
		if (pageSize && page) {
			obj.pageSize = pageSize;
			obj.page = page;
		}
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'cypher()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send a DELETE command to the server.
	 *
	 * The server will delete the node with the given id and broadcast
	 * a deletion notification.
	 */
	deleteNode: function(id, recursive) {
		var obj = {};
		obj.command = 'DELETE';
		obj.id = id;
		var data = {};
		if (recursive) {
			data.recursive = recursive;
		}
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'deleteNode()', obj);
		return sendObj(obj);
	},
	/**
	 * Send a DELETE_RELATIONSHIP command to the server.
	 *
	 * The server will delete the relationship with the given id and broadcast
	 * a deletion notification.
	 */
	deleteRelationship: function(id, recursive) {
		var obj = {};
		obj.command = 'DELETE_RELATIONSHIP';
		obj.id = id;
		var data = {};
		if (recursive) {
			data.recursive = recursive;
		}
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'deleteRelationship()', obj);
		return sendObj(obj);
	},
	/**
	 * Send a REMOVE command to the server.
	 *
	 * The server will remove the node with the given sourceId from the node
	 * with the given targetId and broadcast a removal notification.
	 */
	removeSourceFromTarget: function(entityId, parentId) {
		var obj = {};
		obj.command = 'REMOVE';
		obj.id = entityId;
		var data = {};
		data.id = parentId;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'Remove ' + entityId + ' from ' + parentId);
		_Logger.log(_LogType.WS[obj.command], 'removeSourceFromTarget()', obj);
		return sendObj(obj);
	},
	/**
	 * Send a REMOVE command to the server.
	 *
	 * The server will remove the node from the
	 * tree and broadcast a removal notification.
	 */
	removeChild: function(id) {
		var obj = {};
		obj.command = 'REMOVE';
		obj.id = id;
		var data = {};
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'Remove ' + id);
		_Logger.log(_LogType.WS[obj.command], 'removeChild()', obj);
		return sendObj(obj);
	},
	/**
	 * Send a REMOVE_FROM_COLLECTION command to the server.
	 *
	 * The server will remove the object with the given idToRemove from the
	 * collection property with the given key of the object with the given id.
	 */
	removeFromCollection: function(id, key, idToRemove, callback) {
		var obj = {};
		obj.command = 'REMOVE_FROM_COLLECTION';
		obj.id = id;
		var data = {};
		data.key = key;
		data.idToRemove = idToRemove;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'removeFromCollection()', obj, callback);
		return sendObj(obj, callback);
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
		var obj = {};
		obj.command = 'UPDATE';
		obj.id = id;
		var data = {};
		data[key] = value;
		if (recursive) {
			data['recursive'] = true;
		}
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'setProperty()', obj, callback);
		return sendObj(obj, callback);
	},
	/**
	 * Send an UPDATE command to the server.
	 *
	 * The server will set the properties contained in the 'data' on the node
	 * with the given id and broadcast an update notification.
	 */
	setProperties: function(id, data, callback) {
		var obj = {};
		obj.command = 'UPDATE';
		obj.id = id;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'setProperties()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send a SET_PERMISSIONS command to the server.
	 *
	 * The server will set the permission contained in the 'data' on the node
	 * with the given id and broadcast an update notification.
	 */
	setPermission: function(id, principalId, action, permission, recursive, callback) {
		var obj = {};
		obj.command = 'SET_PERMISSION';
		obj.id = id;
		obj.data = { 'principalId': principalId, 'action': action, 'permission': permission, 'recursive': recursive };
		_Logger.log(_LogType.WS[obj.command], 'setPermission()', obj, callback);
		return sendObj(obj, callback);
	},
	/**
	 * Send an APPEND_FILE command to the server.
	 *
	 * The server will append the file or folder node with the given id
	 * as child of the parent folder node with the given parent id.
	 *
	 * If the node was child of another folder before, it will be
	 * removed from the former parent before being appended
	 * to the new one.
	 *
	 */
	appendFile: function(id, parentId, callback) {
		var obj = {};
		obj.command = 'APPEND_FILE';
		obj.id = id;
		var data = {};
		data.parentId = parentId;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'appendFile()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send an APPEND_CONTENT_ITEM command to the server.
	 */
	appendContentItem: function(id, parentId, callback) {
		var obj = {};
		obj.command = 'APPEND_CONTENT_ITEM';
		obj.id = id;
		var data = {};
		data.parentId = parentId;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'appendContentItem()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send an UNARCHIVE command to the server.
	 *
	 * The server will unarchive the file with the given id in the folder
	 * with the given parent folder id and create files for each archive entry.
	 *
	 */
	unarchive: function(id, parentFolderId, callback) {
		var obj = {};
		obj.command = 'UNARCHIVE';
		obj.id = id;
		var data = {};
		data.parentFolderId = parentFolderId;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'unarchive()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send an APPEND_USER command to the server.
	 *
	 * The server will append the user node with the given id
	 * as child of the parent group node with the given group id.
	 *
	 */
	appendUser: function(id, groupId) {
		var obj = {};
		obj.command = 'APPEND_USER';
		obj.id = id;
		var data = {};
		data.parentId = groupId;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'appendUser()', obj);
		return sendObj(obj);
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
	appendChild: function(id, parentId, key) {
		var obj = {};
		obj.command = 'APPEND_CHILD';
		obj.id = id;
		var data = {};
		data.parentId = parentId;
		data.key = key;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'appendChild()', obj, key);
		return sendObj(obj);
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
	appendWidget: function(source, parentId, pageId, widgetHostBaseUrl, attributes) {
		var obj = {};
		obj.command = 'APPEND_WIDGET';
		obj.pageId = pageId;
		var data = {};
		data.widgetHostBaseUrl = widgetHostBaseUrl;
		data.parentId = parentId;
		data.source = source;
		if (attributes) {
			$.each(Object.keys(attributes), function(i, key) {
				data[key] = attributes[key];
			});
		}
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'appendWidget()', obj);
		return sendObj(obj);
	},
	/**
	 * Send a SAVE_NODE command to the server.
	 *
	 * The server will modify the existing node based on the differences
	 * to the original node.
	 *
	 */
	saveNode: function(source, id, callback) {
		var obj = {};
		obj.command = 'SAVE_NODE';
		obj.id = id;
		var data = {};
		data.source = source;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'saveNode()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send a SAVE_LOCAL_STORAGE command to the server.
	 *
	 * The server will save the given string as 'localStorage' attribute
	 * of the current user.
	 *
	 */
	saveLocalStorage: function(callback) {
		var obj = {};
		obj.command = 'SAVE_LOCAL_STORAGE';
		var data = {};
		data.localStorageString = LSWrapper.getAsJSON();
		obj.data = data;
		//_Logger.log(_LogType.WS[obj.command], 'saveLocalStorage()', data.localStorageString);
		return sendObj(obj, callback);
	},
	/**
	 * Send a GET_LOCAL_STORAGE command to the server.
	 */
	getLocalStorage: function(callback) {
		var obj = {};
		obj.command = 'GET_LOCAL_STORAGE';
		_Logger.log(_LogType.WS[obj.command], 'getLocalStorage()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send an INSERT_BEFORE command to the server.
	 *
	 * The server will insert the DOM node with the given id
	 * after the node with the given refId as child of the node
	 * with the given parentId.
	 *
	 * If the node was in the tree before, it will be
	 * removed from the former parent before being inserted.
	 *
	 */
	insertBefore: function(parentId, id, refId) {
		var obj = {};
		obj.command = 'INSERT_BEFORE';
		obj.id = id;
		var data = {};
		data.refId = refId;
		data.parentId = parentId;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'insertBefore()', obj);
		return sendObj(obj);
	},
	/**
	 * Send a CREATE_DOM_NODE command to the server.
	 *
	 * The server will create a new DOM node with the given tag name.
	 * If tagName is omitted (undefined, null or empty), the server
	 * will create a content (#text) node.
	 *
	 */
	createDOMNode: function(pageId, tagName) {
		var obj = {};
		obj.command = 'CREATE_DOM_NODE';
		obj.pageId = pageId;
		obj.data.tagName = tagName;
		_Logger.log(_LogType.WS[obj.command], 'createDOMNode()', obj);
		return sendObj(obj);
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
	createAndAppendDOMNode: function(pageId, parentId, tagName, attributes) {
		var obj = {};
		obj.command = 'CREATE_AND_APPEND_DOM_NODE';
		obj.pageId = pageId;
		var data = {};
		data.parentId = parentId;
		data.tagName = tagName;
		$.each(Object.keys(attributes), function(i, key) {
			data[key] = attributes[key];
		});
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'createAndAppendDOMNode()', obj);
		return sendObj(obj);
	},
	wrapContent: function(pageId, parentId, tagName) {
		var obj = {};
		obj.command = 'WRAP_CONTENT';
		obj.pageId = pageId;
		var data = {};
		data.parentId = parentId;
		data.tagName = tagName;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'wrapContentInElement()', obj);
		return sendObj(obj);
	},
	/**
	 * Send a CREATE_COMPONENT command to the server.
	 *
	 * The server will transform the node into a reusable component.
	 *
	 */
	createComponent: function(id, callback) {
		var obj = {};
		obj.command = 'CREATE_COMPONENT';
		obj.id = id;
		_Logger.log(_LogType.WS[obj.command], 'createComponent()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send a CLONE_COMPONENT command to the server.
	 *
	 * The server will clone the component node with the given id
	 * and append it to a the parent with given parentId.
	 *
	 */
	cloneComponent: function(id, parentId) {
		var obj = {};
		obj.command = 'CLONE_COMPONENT';
		obj.id = id;
		var data = {};
		data.parentId = parentId;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'cloneComponent()', obj);
		return sendObj(obj);
	},
	/**
	 * Send a CREATE_LOCAL_WIDGET command to the server.
	 *
	 * The server will create a local widget element with the given
	 * name and source code.
	 */
	createLocalWidget: function(id, name, source, callback) {
		var obj = {};
		obj.command = 'CREATE_LOCAL_WIDGET';
		obj.id = id;
		var data = {};
		data.name = name;
		data.source = source;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'createLocalWidget()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send a CLONE_NODE command to the server.
	 *
	 * The server will clone the DOM node with the given id
	 * and append it to a the parent with given parentId.
	 *
	 */
	cloneNode: function(id, parentId, deep) {
		var obj = {};
		obj.command = 'CLONE_NODE';
		obj.id = id;
		var data = {};
		data.parentId = parentId;
		data.deep = deep;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'cloneNode()', obj);
		return sendObj(obj);
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
		var obj = {};
		obj.command = 'SYNC_MODE';
		obj.id = id;
		var data = {};
		data.targetId = targetId;
		data.syncMode = mode;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'setSyncMode()', obj);
		return sendObj(obj);
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
		var obj = {};
		obj.command = 'ADD';
		if (!nodeData.name) {
			nodeData.name = 'New ' + nodeData.type + ' ' + Math.floor(Math.random() * (999999 - 1));
		}
		if (nodeData.isContent && !nodeData.content) {
			nodeData.content = nodeData.name;
		}
		obj.id = id;
		obj.data = nodeData;
		obj.relData = relData;
		_Logger.log(_LogType.WS[obj.command], 'createAndAdd()', obj);
		return sendObj(obj);
	},
	/**
	 * Send a CREATE command to the server.
	 *
	 * The server will create a new node with the given properties contained
	 * in the 'nodeData' hash and broadcast a CREATE notification.
	 */
	create: function(nodeData, callback) {
		var obj = {};
		obj.command = 'CREATE';
		if (!nodeData.name) {
			nodeData.name = 'New ' + nodeData.type + ' ' + Math.floor(Math.random() * (999999 - 1));
		}
		if (nodeData.isContent && !nodeData.content) {
			nodeData.content = nodeData.name;
		}
		obj.data = nodeData;
		_Logger.log(_LogType.WS[obj.command], 'create()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send a CREATE_RELATIONSHIP command to the server.
	 *
	 * The server will create a new relationship with the given properties contained
	 * in the 'relData' hash and broadcast a CREATE_RELATIONSHIP notification.
	 */
	createRelationship: function(relData, callback) {
		var obj = {};
		obj.command = 'CREATE_RELATIONSHIP';
		obj.relData = relData;
		_Logger.log(_LogType.WS[obj.command], 'createRelationship()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send a CREATE_SIMPLE_PAGE command to the server.
	 *
	 * The server will create a simple HTML page with head, body
	 * and title element and broadcast a CREATE notification.
	 */
	createSimplePage: function() {
		var obj = {};
		obj.command = 'CREATE_SIMPLE_PAGE';
		var nodeData = {};
		if (!nodeData.name) {
			nodeData.name = 'New Page ' + Math.floor(Math.random() * (999999 - 1));
		}
		obj.data = nodeData;
		_Logger.log(_LogType.WS[obj.command], 'createSimplePage()', obj);
		return sendObj(obj);
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
	importPage: function(code, address, name, publicVisible, authVisible) {
		var obj = {};
		var data = {};
		obj.command = 'IMPORT';
		data.code = code;
		data.address = address;
		data.name = name;
		data.publicVisible = publicVisible;
		data.authVisible = authVisible;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'importPage()', obj);
		return sendObj(obj);
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

		var p = dmp.patch_make(text1, text2);
		var strp = dmp.patch_toText(p);

		var obj = {};
		obj.command = 'PATCH';
		obj.id = id;
		var data = {};
		data.patch = strp;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], strp, $.quoteString(strp));
		_Logger.log(_LogType.WS[obj.command], 'patch()', obj, callback);
		return sendObj(obj, callback);
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
		var nodeData = {};
		if (!nodeData.name) {
			nodeData.name = 'New Page ' + Math.floor(Math.random() * (999999 - 1));
		}
		var obj = {};
		obj.data = nodeData;
		obj.command = 'CLONE_PAGE';
		obj.id = id;
		_Logger.log(_LogType.WS[obj.command], 'clonePage()', obj);
		return sendObj(obj);
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
	chunk: function(id, chunkId, chunkSize, chunk, chunks) {
		var obj = {};
		obj.command = 'CHUNK';
		obj.id = id;
		var data = {};
		data.chunkId = chunkId;
		data.chunkSize = chunkSize;
		data.chunk = chunk;
		data.chunks = chunks;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'chunk()', obj);
		return sendObj(obj);
	},
	/**
	 * Send a CREATE command to the server.
	 *
	 * This will create a file with the given properties.
	 */
	createFile: function(file, callback) {
		var obj = {};
		obj.command = 'CREATE';
		var data = {};
		data.contentType = file.type;
		data.name = file.name;
		data.size = file.size;
		data.parent = file.parent;
		data.hasParent = file.hasParent;
		data.parentId = file.parentId;
		data.type = isImage(file.type) ? 'Image' : isVideo(file.type) ? 'VideoFile' : 'File';
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'createFile()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send an UPLOAD command to the server.
	 *
	 *
	 */
	upload: function(name, fileData) {
		var obj = {};
		obj.command = 'UPLOAD';
		var data = {};
		data.name = name;
		data.fileData = fileData;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'upload()', obj);
		return sendObj(obj);
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
		var obj = {};
		obj.command = 'LINK';
		obj.id = id;
		var data = {};
		data.targetId = targetId;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'link()', obj);
		return sendObj(obj);
	},
	/**
	 * Send a PUSH command to the server.
	 *
	 * The server will establish a socket connection to the post on the given
	 * port, authenticate with given username and password, and push the node
	 * with the given id to the server.
	 *
	 * The server gives no feedback on a LINK command.
	 */
	push: function(id, host, port, username, password, key, recursive) {
		var obj = {};
		obj.command = 'PUSH';
		obj.id = id;
		var data = {};
		data.host = host;
		data.port = port;
		data.username = username;
		data.password = password;
		data.key = key;
		data.recursive = recursive;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'push()', obj);
		return sendObj(obj);
	},
	/**
	 * Send a PUSH_SCHEMA command to the server.
	 *
	 * The server will establish a socket connection to the post on the given
	 * port, authenticate with given username and password, and push the node
	 * with the given id to the server.
	 *
	 * The server gives no feedback on a LINK command.
	 */
	pushSchema: function(host, port, username, password, key, callback) {
		var obj = {};
		obj.command = 'PUSH_SCHEMA';
		var data = {};
		data.host = host;
		data.port = port;
		data.username = username;
		data.password = password;
		data.key = key;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'push_schema()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send a PULL command to the server.
	 *
	 * The server will establish a socket connection to the post on the given
	 * port, authenticate with given username and password, and pull the node
	 * with the given id from the server.
	 *
	 * The server gives no feedback on a LINK command.
	 */
	pull: function(id, host, port, username, password, key, recursive, callback) {
		var obj = {};
		obj.command = 'PULL';
		obj.id = id;
		var data = {};
		data.host = host;
		data.port = port;
		data.username = username;
		data.password = password;
		data.key = key;
		data.recursive = recursive;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'pull()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send a LIST_SYNCABLES command to the server.
	 *
	 * The server will establish a socket connection to the post on the given
	 * port, authenticate with given username and password, and pull the node
	 * with the given id from the server.
	 *
	 * The server gives no feedback on a LINK command.
	 */
	listSyncables: function(host, port, username, password, key, type, callback) {
		var obj = {};
		obj.command = 'LIST_SYNCABLES';
		var data = {};
		data.host = host;
		data.port = port;
		data.username = username;
		data.password = password;
		data.key = key;
		data.type = type;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'list_syncables()', obj);
		return sendObj(obj, callback);
	},
	/**
	 * Send a LIST_ACTIVE_ELEMENTS command to the server.
	 *
	 * The server will return a result set containing all active elements
	 * in the given page.
	 *
	 * The optional callback function will be executed for each node in the result set.
	 */
	listActiveElements: function(pageId, callback) {
		var obj = {};
		obj.command = 'LIST_ACTIVE_ELEMENTS';
		obj.id = pageId;
		_Logger.log(_LogType.WS[obj.command], 'list_active_elements()', obj);
		return sendObj(obj, callback);
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
		var obj = {};
		obj.command = 'LIST_COMPONENTS';
		var data = {};
		obj.pageSize = pageSize;
		obj.page = page;
		obj.sort = sort;
		obj.order = order;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'listComponents()', obj, callback);
		return sendObj(obj, callback);
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
		var obj = {};
		obj.command = 'LIST_UNATTACHED_NODES';
		var data = {};
		obj.pageSize = pageSize;
		obj.page = page;
		obj.sort = sort;
		obj.order = order;
		obj.data = data;
		_Logger.log(_LogType.WS[obj.command], 'listUnattachedNodes()', obj, callback);
		return sendObj(obj, callback);
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
		var obj = {};
		obj.command = 'DELETE_UNATTACHED_NODES';
		_Logger.log(_LogType.WS[obj.command], 'deleteUnattachedNodes()', obj);
		return sendObj(obj);
	},
	/**
	 * Send a LIST_SCHEMA_PROPERTIES command to the server.
	 *
	 * No broadcast.
	 */
	listSchemaProperties: function(id, view, callback) {
		var obj  = {};
		obj.command = 'LIST_SCHEMA_PROPERTIES';
		obj.id      = id;
		obj.data    = { view: view };
		_Logger.log(_LogType.WS[obj.command], 'listSchemaProperties()', obj, callback);
		return sendObj(obj, callback);
	},
	/**
	 * Send a SNAPSHOTS command to the server.
	 *
	 * The server will return a status object.
	 */
	snapshots: function(mode, name, types, callback) {
		var obj  = {};
		obj.data = { mode: mode, name: name };
		if (types && types.length) {
			obj.data.types = types.join(',');
		}
		obj.command = 'SNAPSHOTS';
		//_Logger.log(_LogType.WS[obj.command], 'snapshots()', obj, callback);
		return sendObj(obj, callback);
	},
	/**
	 * Send an AUTOCOMPLETE command to the server.
	 *
	 * No broadcast.
	 */
	autocomplete: function(id, type, currentToken, previousToken, thirdToken, line, cursorPosition, callback) {
		var obj  = {};
		obj.command = 'AUTOCOMPLETE';
        obj.id      = id;

		obj.data = {
            type: type,
            currentToken: currentToken,
            previousToken: previousToken,
            thirdToken: thirdToken,
            contentType: contentType,
            line: line,
            cursorPosition: cursorPosition
        };

		_Logger.log(_LogType.WS[obj.command], 'autocomplete()', obj, callback);
		return sendObj(obj, callback);
	},
	/**
	 * Send a LAYOUTS command to the server.
	 *
	 * The server will return the stored layout filenames
	 */
	layouts: function(mode, name, schemaLayout, callback) {
		var obj  = {};
		obj.data = { mode: mode, name: name };
		if (schemaLayout && schemaLayout.length) {
			obj.data.schemaLayout = schemaLayout;
		}
		obj.command = 'LAYOUTS';
		return sendObj(obj, callback);
	},
	/**
	 * Send a FIND_DUPLICATES command to the server.
	 *
	 * The server will return a list of all files with identical paths
	 */
	findDuplicates: function(callback) {
		var obj  = {
			command: 'FIND_DUPLICATES'
		};
		return sendObj(obj, callback);
	},
	/**
	 * Send a FAVORITES command to the server.
	 *
	 * Depending on the mode the server will either add/remove the file to/from
	 * the users favorite files.
	 */
	favorites: function(mode, fileId, callback) {
		var obj  = {
			command: 'FAVORITES',
			data: {
				mode: mode,
				id: fileId
			}
		};
		return sendObj(obj, callback);
	}
};
