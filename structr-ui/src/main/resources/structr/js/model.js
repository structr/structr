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


var StructrModel = {
    
    objects : {},
    
    obj : function(id) {
        return StructrModel.objects[id];
    },
    
    /**
     * Create a new object in the model and append a UI element
     * If refId is set, insert before this node
     */
    create : function(data, refId) {
        
        log("StructrModel.create", data);
        
        var type = data.type;
        var obj;
        
        if (type == 'Page') {
            
            obj = new StructrPage(data);
            _Entities.appendObj(obj);
            
        } else if (type == 'Group') {
            
            obj = new StructrGroup(data);
            _Entities.appendObj(obj);
            
        } else if (type == 'User') {
            
            obj = new StructrUser(data);
            _Entities.appendObj(obj);
            
        } else if (type == 'File') {
            
            obj = new StructrFile(data);
            _Entities.appendObj(obj);
            
        } else if (type == 'File' || type == 'Image') {
            
            obj = new StructrFile(data);
            _Entities.appendObj(obj);
            
        } else if (type == 'Folder') {
            
            obj = new StructrFolder(data);
            _Entities.appendObj(obj);
            
        } else {
            
            obj = new StructrElement(data);
            _Entities.appendObj(obj, refId);
            
        }
        
        // Store a reference of this object
        StructrModel.objects[data.id] = obj;
        
        return obj;
    
    },
    
    /**
     * Removes an object from the model.
     */
    remove : function(id) {
        log('StructrModel.remove', id);
        delete StructrModel.objects[id];
    },
    
    /**
     * Deletes an object
     */
    del : function(id) {
        log('StructrModel.remove', id);
        Command.deleteNode(id);
        StructrModel.remove(id);
    },

/**
     * Update the model with the given data.
     * 
     * This function is usually triggered by a websocket message
     * and will trigger a UI refresh.
     **/
    update : function(data) {
        log('StructrModel.update', data);
        var obj = StructrModel.obj(data.id);
        
        if (obj) {
            $.each(Object.keys(data.data), function(i, key) {
                log(key, data.data[key]);
                obj.setAttribute(key, data.data[key]);
            });
        }
        
        StructrModel.refresh(data.id);
    },
    
    /**
     * Refresh the object's UI representation
     * with the current object data from the model.
     */
    refresh : function(id) {
        log('StructrModel.refresh', id);
        
        var obj = StructrModel.obj(id);
        
        if (obj) {
            var element = $('.' + id + '_');
        
            log(obj, id, element);
        
            // update values with given key
            $.each(Object.keys(obj.attributes), function(i, key) {
                //for (var key in data.data) {
                var inputElement = $('td.' + key + '_ input', element);
                log(inputElement);
                var newValue = obj.attributes[key];
                //console.log(key, newValue, typeof newValue);
            
                var attrElement = element.children('.' + key + '_');
            
                if (attrElement && $(attrElement).length) {
                
                    var tag = $(attrElement).get(0).tagName.toLowerCase();
                
                //                attrElement.val(newValue);
                //                attrElement.show();
                //                log(attrElement, inputElement);
            
                }
            
            
                if (typeof newValue  == 'boolean') {
                
                    _Entities.changeBooleanAttribute(attrElement, newValue);
            
                } else {
                
                    attrElement.animate({
                        color: '#81ce25'
                    }, 100, function() {
                        $(this).animate({
                            color: '#333333'
                        }, 200);
                    });
                
                    if (attrElement && tag == 'select') {
                        attrElement.val(newValue);
                    } else {
                        log(key, newValue);
                        if (key == 'name') {
                            attrElement.html(fitStringToSize(newValue, 200));
                            attrElement.attr('title', newValue);
                        }
                    }
                
                    if (inputElement) {
                        inputElement.val(newValue);
                    }
                
                    if (key == 'content') {
                    
                        log(attrElement.text(), newValue);
                    
                        attrElement.text(newValue);
                    
                        // hook for CodeMirror edit areas
                        if (editor && editor.id == id) {
                            log(editor.id);
                            editor.setValue(newValue);
                        //editor.setCursor(editorCursor);
                        }
                    }
                }
            
                log(key, Structr.getClass(element));
            
                if (key == 'name' && Structr.getClass(element) == 'page') {
                    log('Reload iframe', data.id, newValue);
                    window.setTimeout(function() {
                        _Pages.reloadIframe(data.id, newValue)
                    }, 100);
                }
        
            });
        
        }
    
    },
    
    /**
     * Fetch data from server. This will trigger a refresh of the model.
     */
    fetch : function(id) {
        Command.get(id);
    },
    
    /**
     * Save model data to server. This will trigger a refresh of the model.
     */
    save : function(id) {
        var obj = StructrModel.obj(id);
        log('StructrModel.save', obj.attributes);
        
        // Filter out object type data
        var data = {};
        $.each(Object.keys(obj.attributes), function(i, key) {
            
            var value = obj.attributes[key];
            
            if (typeof value != 'object') {
                data[key] = value;
            }
            
        });
        
        Command.setProperties(id, data);
    }

}


/**************************************
 * Structr Object
 **************************************/

function StructrObj(data) {
    this.id = data.id;
    this.attributes = data;
}

StructrObj.prototype.getId = function() {
    return this.id;
}

StructrObj.prototype.save = function() {
    StructrModel.save(this.id);
}

StructrObj.prototype.setAttribute = function(key, value) {
    this.attributes[key] = value;
}


/**************************************
 * Structr Folder
 **************************************/

function StructrFolder(data) {
    this.id = data.id;
    this.attributes = data;
    this.parent = data.parent;
    this.folders = data.folders;
    this.files = data.files;
}

StructrFolder.prototype.getId = function() {
    return this.id;
}

StructrFolder.prototype.save = function() {
    StructrModel.save(this.id);
}

StructrFolder.prototype.setAttribute = function(key, value) {
    this.attributes[key] = value;
}

StructrFolder.prototype.getFolders = function() {
    return this.folders;
}

StructrFolder.prototype.getFiles = function() {
    return this.files;
}

StructrFolder.prototype.getParent = function() {
    return this.parent;
}

/**************************************
 * Structr File
 **************************************/

function StructrFile(data) {
    this.id = data.id;
    this.attributes = data;
    this.parent = data.parent;
}

StructrFile.prototype.getId = function() {
    return this.id;
}

StructrFile.prototype.save = function() {
    StructrModel.save(this.id);
}

StructrFile.prototype.setAttribute = function(key, value) {
    this.attributes[key] = value;
}

StructrFile.prototype.getParent = function() {
    return this.parent;
}


/**************************************
 * Structr User
 **************************************/

function StructrUser(data) {
    this.id = data.id;
    this.attributes = data;
    this.groups = data.groups;
}

StructrUser.prototype.getId = function() {
    return this.id;
}

StructrUser.prototype.save = function() {
    StructrModel.save(this.id);
}

StructrUser.prototype.setAttribute = function(key, value) {
    this.attributes[key] = value;
}

StructrUser.prototype.getGroups = function() {
    return this.groups;
}


/**************************************
 * Structr Group
 **************************************/

function StructrGroup(data) {
    this.id = data.id;
    this.attributes = data;
    this.users = data.users;
}

StructrGroup.prototype.getId = function() {
    return this.id;
}

StructrGroup.prototype.save = function() {
    StructrModel.save(this.id);
}

StructrGroup.prototype.setAttribute = function(key, value) {
    this.attributes[key] = value;
}

StructrGroup.prototype.getUsers = function() {
    return this.users;
}


/**************************************
 * Structr Page
 **************************************/

function StructrPage(data) {
    this.id = data.id;
    this.attributes = data;
}

StructrPage.prototype.createElement = function(name) {
    return new Element(name);
}

StructrPage.prototype.getId = function() {
    return this.id;
}

StructrPage.prototype.setAttribute = function(key, value) {
    this.attributes[key] = value;
}

StructrPage.prototype.getAttribute = function(key) {
    return this.attributes[key];
}


/**************************************
 * Structr Element
 **************************************/

function StructrElement(data) {
    this.id = data.id;
    this.tagName = data.tag;
    this.children = data.children;
    this.parent = data.parent;
    this.attributes = data;
}

StructrElement.prototype.getChildren = function() {
    return this.children;
}

StructrElement.prototype.getParent = function() {
    return this.parent;
}

StructrElement.prototype.appendChild = function(el) {
    this.children.push(el);
}

StructrElement.prototype.setAttribute = function(key, value) {
    this.attributes[key] = value;
}

StructrElement.prototype.setAttributes = function(attributes) {
    this.attributes = attributes;
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
