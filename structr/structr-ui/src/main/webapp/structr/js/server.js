/*
 *  Copyright (C) 2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Use these commands to send command calls to the websocket server.
 * 
 * The websocket listener is contained in the websocket.js file.
 * 
 */
var Server = {

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
     * node with the given id which are rendered within the resource with
     * the given resourceId to the sending client (no broadcast).
     */
    children : function(id, resourceId) {
        var obj = {};
        obj.command = 'CHILDREN';
        obj.id = id;
        var data = {};
        data.resourceId = resourceId;
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
    removeSourceFromTarget : function(sourceId, targetId) {
        if (debug) console.log('Remove ' + sourceId + ' from ' + targetId);
        var obj = {};
        obj.command = 'REMOVE';
        obj.id = targetId;
        var data = {};
        data.id = sourceId;
        obj.data = data;
        if (debug) console.log('removeSourceFromTarget()', obj);
        return sendObj(obj);
    },

    /**
     * Send an UPDATE command to the server.
     * 
     * The server will set the given value as new value of the property with
     * the given key for the node with the given id and broadcast an update
     * notification.
     */
    setProperty : function(id, key, value) {
        var obj = {};
        obj.command = 'UPDATE';
        obj.id = id;
        var data = {};
        data[key] = value;
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
     * The server will either
     * 
     * add the node with the given id to the children of the node with the
     * id given as 'id' property of the 'nodeData' hash. If the 'relData'
     * contains a resource id in both property fields 'sourceResourceId' and 
     * 'targetResourceId', the node will be copied from the source to the
     * target resource,
     * 
     * - or -
     * 
     * if 'id' is null, create a new node with the properties contained
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
            nodeData.content = '';
        }
        obj.id = id;
        obj.data = nodeData;
        obj.relData = relData;
        if (debug) console.log('createAndAdd()', obj);
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
            nodeData.content = '';
        }
        obj.data = nodeData;
        if (debug) console.log('create()', obj);
        return sendObj(obj);
    },

    /**
     * Send an IMPORT command to the server.
     * 
     * This command will trigger the server-side importer to start with
     * parsing and importing the page data from the given address.
     * If successful, the server will add a new resource with the given name
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
     * This will create a new resource and copy all children of the resource
     * with the given id so that they become children of the newly created
     * resource, too.
     * 
     * The server will broadcast a CREATE and an ADD notification.
     */
    cloneResource : function(id) {
        var nodeData = {};
        if (!nodeData.name) {
            nodeData.name = 'New Resource ' + Math.floor(Math.random() * (999999 - 1));
        }
        var obj = {};
        obj.data = nodeData;
        obj.command = 'CLONE';
        obj.id = id
        return sendObj(obj);
    }

}