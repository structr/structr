/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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
        var data = {};
        data.id = id;
        obj.data = data;
        if (debug) console.log('get()', obj);
        return sendObj(obj);
    },

    /**
     * Send a LIST command to the server.
     * 
     * The server will return a result set containing all items of the given
     * type to the sending client (no broadcast).
     * The server will return an array with all
     * node ids which have child nodes in this page.
     * 
     * TODO: Add paging and sorting
     */
    list : function(type) {
        var obj = {};
        obj.command = 'LIST';
        var data = {};
        data.type = type;
        obj.data = data;
        if (debug) console.log('list()', obj);
        return sendObj(obj);
    },

    /**
     * Send a CHILDREN command to the server.
     * 
     * The server will return a result set containing all children of the
     * node with the given id which are rendered within the page with
     * the given pageId and within the component with the given
     * componentId to the sending client (no broadcast).
     * 
     * If the tree address is set, the client will add the children
     * at the given address.
     * 
     * The server will return an array with all node ids which have child
     * nodes in this page.
     */
    children : function(id, componentId, pageId, treeAddress) {
        var obj = {};
        obj.command = 'CHILDREN';
        obj.id = id;
        var data = {};
        if (componentId) data.componentId = componentId;
        if (treeAddress) data.treeAddress = treeAddress;
        if (pageId) data.pageId = pageId;
        obj.data = data;
        if (debug) console.log('children()', obj);
        return sendObj(obj);
    },

    /**
     * Send a TREE command to the server.
     * 
     * The server will return a the complete tree below the root node with
     * the given id to the sending client (no broadcast).
     */
    getTree : function(id) {
        var obj = {};
        obj.command = 'TREE';
        obj.id = id;
        if (debug) console.log('getTree()', obj);
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
        if (debug) console.log('getProperty()', obj);
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
    //        if (debug) console.log('insert()', obj);
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
        if (debug) console.log('deleteNode()', obj);
        return sendObj(obj);
    },

    /**
     * Send a REMOVE command to the server.
     * 
     * The server will remove the node with the given sourceId from the node
     * with the given targetId and broadcast a removal notification.
     */
    removeSourceFromTarget : function(entityId, parentId, componentId, pageId, position) {
        if (debug) console.log('Remove ' + entityId + ' from ' + parentId);
        var obj = {};
        obj.command = 'REMOVE';
        obj.id = entityId;
        var data = {};
        data.id = parentId;
        data.componentId = componentId;
        data.pageId = pageId;
        data.position = position;
        obj.data = data;
        if (debug) console.log('removeSourceFromTarget()', obj);
        return sendObj(obj);
    },

    /**
     * Send a REMOVE command to the server.
     * 
     * The server will remove the node at the address in the
     * tree and broadcast a removal notification.
     */
    remove : function(entityId, treeAddress) {
        if (debug) console.log('Remove ' + treeAddress);
        var obj = {};
        obj.command = 'REMOVE';
        obj.id = entityId;
        var data = {};
        data.treeAddress = treeAddress;
        obj.data = data;
        if (debug) console.log('remove()', obj);
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
        if (debug) console.log('setProperty()', obj);
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
        if (debug) console.log('setProperties()', obj);
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
        if (debug) console.log('createAndAdd()', obj);
        return sendObj(obj);
    },

    /**
     * Send a MOVE command to the server.
     * 
     * Add the node with the given id to the children of the node with the
     * id given as 'id' property of the 'nodeData' hash. If the 'relData'
     * contains a page id in both property fields 'sourcePageId' and 
     * 'targetPageId', the node will be copied from the source to the
     * target page,
     * 
     * When finished, the server will broadcast a MOVE notification.
     */
    move : function(id, nodeData) {
        var obj = {};
        obj.command = 'MOVE';
        obj.id = id;
        obj.data = nodeData;
        //obj.relData = relData;
        if (debug) console.log('move()', obj);
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
        if (debug) console.log('create()', obj);
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
        if (debug) console.log('createSimplePage()', obj);
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
        if (debug) console.log('importPage()', obj);
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
        if (debug) console.log(text1, text2);

        // no null values allowed
        if (!text1) text1 = '';
        if (!text2) text2 = '';

        var p = dmp.patch_make(text1, text2);
        var strp = dmp.patch_toText(p);
        if (debug) console.log(strp, $.quoteString(strp));

        var obj = {};
        obj.command = 'PATCH';
        obj.id = id;
        var data = {};
        data.patch = strp;
        obj.data = data;
        if (debug) console.log('patch()', obj);
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
        if (debug) console.log('clonePage()', obj);
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
        if (debug) console.log('chunk()', obj);
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
        if (debug) console.log('link()', obj);
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
        if (debug) console.log('wrap()', obj);
        return sendObj(obj);
        
    }

}