/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Use these commands to send command calls to the websocket server.
 * 
 * The websocket listener is in the websocket.js file.
 * 
 */
var Command = {

    /**
     * Send a single GET command to the server.
     * 
     * The server will return a single item with all properties
     * of the node with the given id to the sending client (no broadcast).
     */
    get : function(id) {
        var obj = {};
        obj.command = 'GET';
        obj.id = id;
        //var data = {};
        //data.id = id;
        //obj.data = data;
        log('get()', obj);
        return sendObj(obj);
    },

    /**
     * Send a LIST command to the server.
     * 
     * The server will return a result set containing all items of the given
     * type to the sending client (no broadcast).
     */
    list : function(type, pageSize, page, sort, order) {
        var obj = {};
        obj.command = 'LIST';
        var data = {};
        data.type = type;
        obj.pageSize = pageSize;
        obj.page = page;
        obj.sort = sort;
        obj.order = order;
        obj.data = data;
        log('list()', obj);
        return sendObj(obj);
    },

    /**
     * Send a CHILDREN command to the server.
     * 
     * The server will return a result set containing all children of the
     * node with the given id to the sending client (no broadcast).
     */
    children : function(id) {
        var obj = {};
        obj.command = 'CHILDREN';
        obj.id = id;
        var data = {};
        obj.data = data;
        log('children()', obj);
        return sendObj(obj);
    },

    /**
     * Send a GET command with key and and element selector to the server.
     * 
     * The server will return the value of the property with the given key
     * of the node with the given id to the sending client (no broadcast).
     * 
     * Use the elementSelector to transmit a jQuery selector which addresses
     * the target element for the websocket listener to display the value in.
     */
    getProperty : function(id, key, elementSelector) {
        var obj = {};
        obj.command = 'GET';
        obj.id = id;
        var data = {};
        data.key = key;

        if (key.startsWith('_html_')) {
            obj.view = '_html_';
        }

        data.displayElementId = elementSelector;
        obj.data = data;
        log('getProperty()', obj);
        return sendObj(obj);
    },
    
    /**
     * Send an INSERT command to the server.
     * 
     * The server will parse the 'html' text and create nodes and relationships
     * according to the HTML structure contained. 
     * 
     * The server will broadcast CREATE and ADD notifications.
     */
    //    insert : function(id, html) {
    //        var obj = {};
    //        obj.command = 'INSERT';
    //        obj.id = id;
    //        var data = {};
    //        data.html = html;
    //        obj.data = data;
    //        log('insert()', obj);
    //        return sendObj(obj);
    //    },

    /**
     * Send a DELETE command to the server.
     * 
     * The server will delete the node with the given id and broadcast
     * a deletion notification.
     */
    deleteNode : function(id) {
        var obj = {};
        obj.command = 'DELETE';
        obj.id = id;
        log('deleteNode()', obj);
        return sendObj(obj);
    },

    /**
     * Send a REMOVE command to the server.
     * 
     * The server will remove the node with the given sourceId from the node
     * with the given targetId and broadcast a removal notification.
     */
    removeSourceFromTarget : function(entityId, parentId) {
        log('Remove ' + entityId + ' from ' + parentId);
        var obj = {};
        obj.command = 'REMOVE';
        obj.id = entityId;
        var data = {};
        data.id = parentId;
        obj.data = data;
        log('removeSourceFromTarget()', obj);
        return sendObj(obj);
    },

    /**
     * Send a REMOVE command to the server.
     * 
     * The server will remove the node from the
     * tree and broadcast a removal notification.
     */
    remove : function(id) {
        log('Remove ' + id);
        var obj = {};
        obj.command = 'REMOVE';
        obj.id = id;
        var data = {};
        obj.data = data;
        log('remove()', obj);
        return sendObj(obj);
    },

    /**
     * Send an UPDATE command to the server.
     * 
     * The server will set the given value as new value of the property with
     * the given key for the node with the given id and broadcast an update
     * notification.
     * 
     * If recursive is set and true, the property will be set on all subnodes, too.
     */
    setProperty : function(id, key, value, recursive) {
        var obj = {};
        obj.command = 'UPDATE';
        obj.id = id;
        var data = {};
        data[key] = value;
        if (recursive) data['recursive'] = true;
        obj.data = data;
        log('setProperty()', obj);
        return sendObj(obj);
    },

    /**
     * Send an UPDATE command to the server.
     * 
     * The server will set the properties contained in the 'data' on the node
     * with the given id and broadcast an update notification.
     */
    setProperties : function(id, data) {
        var obj = {};
        obj.command = 'UPDATE';
        obj.id = id;
        obj.data = data;
        log('setProperties()', obj);
        return sendObj(obj);
    },

    /**
     * Send an APPEND_CHLID command to the server.
     * 
     * The server will append the DOM node with the given id
     * as child of the node with the given parent id.
     * 
     * If the node was child of a parent before, it will be
     * removed from the former parent before being appended
     * to the new one.
     * 
     */
    appendChild : function(id, parentId) {
        var obj = {};
        obj.command = 'APPEND_CHILD';
        obj.id = id;
        var data = {};
        data.parentId = parentId;
        obj.data = data;
        console.log('appendChild()', obj);
        return sendObj(obj);
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
    insertBefore : function(parentId, id, refId) {
        var obj = {};
        obj.command = 'INSERT_BEFORE';
        obj.id = id;
        var data = {};
        data.refId   = refId;
        data.parentId = parentId;
        obj.data = data;
        console.log('insertBefore()', obj);
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
    createDOMNode : function(pageId, tagName) {
        var obj = {};
        obj.command = 'CREATE_DOM_NODE';
        obj.pageId = pageId;
        obj.data.tagName = tagName;
        console.log('createDOMNode()', obj);
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
    createAndAppendDOMNode : function(pageId, parentId, tagName) {
        var obj = {};
        obj.command = 'CREATE_AND_APPEND_DOM_NODE';
        obj.pageId = pageId;
        var data = {};
        data.parentId = parentId;
        data.tagName = tagName;
        obj.data = data;
        console.log('createAndAppendDOMNode()', obj);
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
    createAndAdd : function(id, nodeData, relData) {
        var obj = {};
        obj.command = 'ADD';
        if (!nodeData.name) {
            nodeData.name = 'New ' + nodeData.type + ' ' + Math.floor(Math.random() * (999999 - 1));
        }
        if (nodeData.type == 'Content' && !nodeData.content) {
            nodeData.content = nodeData.name;
        }
        obj.id = id;
        obj.data = nodeData;
        obj.relData = relData;
        log('createAndAdd()', obj);
        return sendObj(obj);
    },

    /**
     * Send a CREATE command to the server.
     * 
     * The server will create a new node with the given properties contained
     * in the 'nodeData' hash and broadcast a CREATE notification.
     */
    create : function(nodeData) {
        var obj = {};
        obj.command = 'CREATE';
        if (!nodeData.name) {
            nodeData.name = 'New ' + nodeData.type + ' ' + Math.floor(Math.random() * (999999 - 1));
        }
        if (nodeData.type == 'Content' && !nodeData.content) {
            nodeData.content = nodeData.name;
        }
        obj.data = nodeData;
        log('create()', obj);
        return sendObj(obj);
    },

    /**
     * Send a CREATE_SIMPLE_PAGE command to the server.
     * 
     * The server will create a simple HTML page with head, body
     * and title element and broadcast a CREATE notification.
     */
    createSimplePage : function() {
        var obj = {};
        obj.command = 'CREATE_SIMPLE_PAGE';
        var nodeData = {};
        if (!nodeData.name) {
            nodeData.name = 'New Page ' + Math.floor(Math.random() * (999999 - 1));
        }
        obj.data = nodeData;
        log('createSimplePage()', obj);
        return sendObj(obj);
    },
    
    /**
     * Send an IMPORT command to the server.
     * 
     * This command will trigger the server-side importer to start with
     * parsing and importing the page data from the given address.
     * If successful, the server will add a new page with the given name
     * and make it visible for public or authenticated users.
     * 
     * The server will broadcast CREATE and ADD notifications for each
     * node respective relationship created.
     */
    importPage : function(address, name, timeout, publicVisible, authVisible) {
        var obj = {};
        var data = {};
        obj.command = 'IMPORT';
        data.address = address;
        data.name = name;
        data.timeout = timeout;
        data.publicVisible = publicVisible;
        data.authVisible = authVisible;
        obj.data = data;
        log('importPage()', obj);
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
    patch : function(id, text1, text2) {
        log(text1, text2);

        // no null values allowed
        if (!text1) text1 = '';
        if (!text2) text2 = '';

        var p = dmp.patch_make(text1, text2);
        var strp = dmp.patch_toText(p);
        log(strp, $.quoteString(strp));

        var obj = {};
        obj.command = 'PATCH';
        obj.id = id;
        var data = {};
        data.patch = strp;
        obj.data = data;
        log('patch()', obj);
        return sendObj(obj);
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
    clonePage : function(id) {
        var nodeData = {};
        if (!nodeData.name) {
            nodeData.name = 'New Page ' + Math.floor(Math.random() * (999999 - 1));
        }
        var obj = {};
        obj.data = nodeData;
        obj.command = 'CLONE';
        obj.id = id;
        log('clonePage()', obj);
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
    chunk : function(id, chunkId, chunkSize, chunk) {
        var obj = {};
        obj.command = 'CHUNK';
        obj.id = id;
        var data = {};
        data.chunkId = chunkId;
        data.chunkSize = chunkSize;
        data.chunk = chunk;
        obj.data = data;
        log('chunk()', obj);
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
    link : function(id, targetId) {
        var obj = {};
        obj.command = 'LINK';
        obj.id = id;
        var data = {};
        data.targetId = targetId;
        obj.data = data;
        log('link()', obj);
        return sendObj(obj);
    },
    
    /**
     * Send a WRAP command to the server.
     * 
     * The server will wrap the node with the given id into an
     * additional component node.
     * 
     * The server will broadcast CREATE and ADD notifications.
     */
    wrap : function(id, nodeData, relData) {
        var obj = {};
        obj.command = 'WRAP';
        obj.id = id;
        obj.data = nodeData;
        obj.relData = relData;
        log('wrap()', obj);
        return sendObj(obj);
        
    }

}