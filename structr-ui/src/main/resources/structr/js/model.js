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
 * Structr page model
 * 
 */

var StructrModel = {
    
    objects : {},
    
    register : function(obj) {
        StructrModel.objects[obj.id] = obj;
    },
    
    fetch : function(id) {
        Command.get(id);
    },
    
    save : function(id) {
        Command.setProperties(id, StructrModel.objects[id].attributes);
    }
}


/**************************************
 * Structr User
 **************************************/

function StructrUser(id) {
    this.id = id;
    StructrModel.register(this);
}

StructrUser.prototype.createElement = function(name) {
    return new Element(name);
}

StructrUser.prototype.getDocumentElement = function() {
    return this.documentElement;
}


/**************************************
 * Structr Document (Page)
 **************************************/

function StructrDocument(id) {
    this.id = id;
    this.documentElement = new StructrElement();
    StructrModel.register(this);
}

StructrDocument.prototype.createElement = function(name) {
    return new Element(name);
}

StructrDocument.prototype.getId = function() {
    return this.id;
}

StructrDocument.prototype.getDocumentElement = function() {
    return this.documentElement;
}


/**************************************
 * Structr Element
 **************************************/

function StructrElement(tagName, id) {
    this.id = id;
    this.tagName = tagName;
    this.children = [];
    this.attributes = {};
    console.log('StructrElement', id, tagName);
    StructrModel.register(this);
    StructrModel.fetch(id);
}

StructrElement.prototype.appendChild = function(el) {
    this.children.push(el);
}

StructrElement.prototype.setAttribute = function(key, value) {
    this.attributes[key] = value;
}

StructrElement.prototype.getId = function() {
    return this.id;
}

StructrElement.prototype.getTagName = function() {
    return this.tagName;
}

StructrElement.prototype.getAttribute = function(key) {
    return this.attributes[key];
}

StructrElement.prototype.removeAttribute = function(key) {
    delete this.attributes[key];
}

StructrElement.prototype.save = function() {
    StructrModel.save(this.id);
}