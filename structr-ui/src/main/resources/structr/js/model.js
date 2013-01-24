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
    callbacks : [],
    
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
            
        } else if (type == 'Group') {
            
            obj = new StructrGroup(data);
            
        } else if (type == 'User') {
            
            obj = new StructrUser(data);
            
        } else if (type == 'File') {
            
            obj = new StructrFile(data);
            
        } else if (type == 'Image') {
            
            obj = new StructrImage(data);
            
        } else if (type == 'Folder') {
            
            obj = new StructrFolder(data);
            
        } else if (type == 'Content') {
            
            obj = new StructrContent(data);
            
        } else {
            
            obj = new StructrElement(data);
            
        }
        
        // Store a reference of this object
        StructrModel.objects[data.id] = obj;
        
        var refNode = refId ? Structr.node(refId) : undefined;
        
        // Display in page (before refNode, if given)
        var element = obj.append(refNode);
        
        if (element) {
        
            if (isExpanded(obj.id)) {
                _Entities.ensureExpanded(element);
            }
        
            var parent = element.parent();
        
            if (parent && parent.hasClass('node') && parent.children('.node') && parent.children('.node').length==1) {
                
                log('parent of last appended object has children');

                var ent = Structr.entityFromElement(parent);
                _Entities.ensureExpanded(parent);
                log('entity', ent);
                _Entities.appendExpandIcon(parent, ent, true, true);

            }
        }
        
        return obj;
    
    },
    
    /**
     * Deletes an object
     */
    del : function(id) {
        
        Structr.node(id).remove();
        $('#show_' + id, previews).remove();
        _Pages.reloadPreviews();

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
                log('update model', key, data.data[key]);
                obj[key] = data.data[key];
                StructrModel.refreshKey(obj.id, key);
            });
        }
        
        if (data.callback) {
            log('executing callback with id', data.callback);
            StructrModel.callbacks[data.callback]();
        }
        
    },

    updateKey : function(id, key, value) {
        log('StructrModel.updateKey', id, key, value);
        var obj = StructrModel.obj(id);
        
        if (obj) {
            obj[key] = value;
        }
        
        StructrModel.refreshKey(id, key);

    },

    /**
     * Refresh the object's UI representation with
     * the current model value for the given key
     */
    refreshKey : function(id, key) {
        
        var obj = StructrModel.obj(id);
        if (!obj) return;
        
        var element = Structr.node(id);
        
        if (!element) return;

        //for (var key in data.data) {
        var inputElement = $('td.' + key + '_ input', element);
        log(inputElement);
        var newValue = obj[key];
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
            //                        if (editor && editor.id == id) {
            //                            log(editor.id);
            //                            editor.setValue(newValue);
            //                            editor.setCursor(editorCursor);
            //                        }
            }
        }
            
        log(key, Structr.getClass(element));
            
        if (key == 'name' && Structr.getClass(element) == 'page') {

            // update tab and reload iframe
            var tabNameElement = $('#show_' + id).children('.name_');
                                        
            tabNameElement.animate({
                color: '#81ce25'
            }, 100, function() {
                $(this).animate({
                    color: '#333333'
                }, 200);
            });
                    
            tabNameElement.html(fitStringToSize(newValue, 200));
            tabNameElement.attr('title', newValue);
                    
            log('Reload iframe', id, newValue);
            window.setTimeout(function() {
                _Pages.reloadIframe(id, newValue)
            }, 100);
        }
                
    },

    /**
     * Refresh the object's UI representation
     * with the current object data from the model.
     */
    refresh : function(id) {
        
        var obj = StructrModel.obj(id);
        
        console.log('StructrModel.refresh', id, obj);
        
        if (obj) {
            var element = Structr.node(id);
            
            if (!element) return;
        
            log(obj, id, element);
        
            // update values with given key
            $.each(Object.keys(obj), function(i, key) {
                
                StructrModel.refreshKey(id, key);
                
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
        log('StructrModel.save', obj);
        
        // Filter out object type data
        var data = {};
        $.each(Object.keys(obj), function(i, key) {
            
            var value = obj[key];
            
            if (typeof value != 'object') {
                data[key] = value;
            }
            
        });
        //console.log('save', id, data);
        Command.setProperties(id, data);
    }

}


/**************************************
 * Structr Object
 **************************************/
//
//function StructrObj(data) {
//    this.id = data.id;
//    this.attributes = data;
//}
//
//StructrObj.prototype.getId = function() {
//    return this.id;
//}
//
//StructrObj.prototype.save = function() {
//    StructrModel.save(this.id);
//}
//
//StructrObj.prototype.setProperty = function(key, value) {
//    this.attributes[key] = value;
//}


/**************************************
 * Structr Folder
 **************************************/

function StructrFolder(data) {
    var self = this;
    $.each(Object.keys(data), function(i, key) {
        self[key] = data[key];
    });
}

StructrFolder.prototype.save = function() {
    StructrModel.save(this.id);
}

StructrFolder.prototype.setProperty = function(key, value, recursive, callback) {
    Command.setProperty(this.id, key, value, recursive, callback);
}

StructrFolder.prototype.remove = function() {
    var folder = this;
    var folderEl = Structr.node(folder.id);
    var parentFolderEl = Structr.node(folder.parent.id);
    log('removeFolderFromFolder', folderEl);
        
    _Entities.resetMouseOverState(folderEl);

    folderEl.children('.delete_icon').replaceWith('<img title="Delete folder ' + folder.id + '" '
        + 'alt="Delete folder ' + folder.id + '" class="delete_icon button" src="' + Structr.delete_icon + '">');

    folderEl.children('.delete_icon').on('click', function(e) {
        e.stopPropagation();
        _Entities.deleteNode(this, folder);
    });
        
    folders.append(folderEl);
        
    if (!Structr.containsNodes(parentFolderEl)) {
        _Entities.removeExpandIcon(parentFolderEl);
        enable(parentFolderEl.children('.delete_icon')[0]);
    }

    folderEl.draggable({
        revert: 'invalid',
        containment: '#main',
        stack: 'div'
    });
}

StructrFolder.prototype.append = function(refNode) {
    return _Files.appendFolderElement(this, refNode);    
}


/**************************************
 * Structr File
 **************************************/

function StructrFile(data) {
    var self = this;
    $.each(Object.keys(data), function(i, key) {
        self[key] = data[key];
    });
}

StructrFile.prototype.save = function() {
    StructrModel.save(this.id);
}

StructrFile.prototype.setProperty = function(key, value, callback) {
    Command.setProperty(this.id, key, value, false, callback);
}

StructrFile.prototype.remove = function() {
    _Files.removeFileFromFolder(this.id);
}

StructrFile.prototype.append = function(refNode) {
    _Files.uploadFile(this);
    return _Files.appendFileElement(this, refNode);    
}


/**************************************
 * Structr Image
 **************************************/

function StructrImage(data) {
    var self = this;
    $.each(Object.keys(data), function(i, key) {
        self[key] = data[key];
    });
}

StructrImage.prototype.save = function() {
    StructrModel.save(this.id);
}

StructrImage.prototype.setProperty = function(key, value, callback) {
    Command.setProperty(this.id, key, value, false, callback);
}

StructrImage.prototype.remove = function() {
    _Images.removeImageFromFolder(this.id);
}

StructrImage.prototype.append = function(refNode) {
    _Files.uploadFile(this);
    return _Images.appendImageElement(this, refNode);    
}


/**************************************
 * Structr User
 **************************************/

function StructrUser(data) {
    var self = this;
    $.each(Object.keys(data), function(i, key) {
        self[key] = data[key];
    });
}

StructrUser.prototype.save = function() {
    StructrModel.save(this.id);
}

StructrUser.prototype.setProperty = function(key, value, recursive, callback) {
    Command.setProperty(this.id, key, value, false, callback);
}

StructrUser.prototype.remove = function() {
    _UsersAndGroups.removeUserFromGroup(this.id);
}

StructrUser.prototype.append = function(refNode) {
    _UsersAndGroups.appendUserElement(this, refNode);
}


/**************************************
 * Structr Group
 **************************************/

function StructrGroup(data) {
    var self = this;
    $.each(Object.keys(data), function(i, key) {
        self[key] = data[key];
    });
}

StructrGroup.prototype.save = function() {
    StructrModel.save(this.id);
}

StructrGroup.prototype.setProperty = function(key, value, recursive, callback) {
    Command.setProperty(this.id, key, value, recursive, callback);
}

StructrGroup.prototype.append = function(refNode) {
    _UsersAndGroups.appendGroupElement(this, refNode);
}

/**************************************
 * Structr Page
 **************************************/

function StructrPage(data) {
    var self = this;
    $.each(Object.keys(data), function(i, key) {
        self[key] = data[key];
    });
}

//StructrPage.prototype.createElement = function(name) {
//    return new Element(name);
//}

StructrPage.prototype.setProperty = function(key, value, rec, callback) {
    Command.setProperty(this.id, key, value, rec, callback);
}

StructrPage.prototype.append = function() {
    return _Pages.appendPageElement(this);
}


/**************************************
 * Structr Element
 **************************************/

function StructrElement(data) {
    var self = this;
    $.each(Object.keys(data), function(i, key) {
        self[key] = data[key];
    });
}

StructrElement.prototype.appendChild = function(el) {
    var self = this;
    self.children.push(el);
}

StructrElement.prototype.setProperty = function(key, value, recursive, callback) {
    Command.setProperty(this.id, key, value, recursive, callback);
}

StructrElement.prototype.removeAttribute = function(key) {
    var self = this;
    delete self[key];
}

StructrElement.prototype.save = function() {
    StructrModel.save(this.id);
}

StructrElement.prototype.remove = function() {
    var element = Structr.node(this.id);
    if (this.parent) {
        var parent = Structr.node(this.parent.id);
    }
        
    if (element) element.remove();

    if (parent && !Structr.containsNodes(parent)) {
        _Entities.removeExpandIcon(parent);
    }
    _Pages.reloadPreviews();   
}

StructrElement.prototype.append = function(refNode) {
    return _Pages.appendElementElement(this, refNode);
}


/**************************************
 * Structr Element
 **************************************/

function StructrContent(data) {
    var self = this;
    $.each(Object.keys(data), function(i, key) {
        self[key] = data[key];
    });
}

StructrContent.prototype.appendChild = function(el) {
    var self = this;
    self.children.push(el);
}

StructrContent.prototype.setProperty = function(key, value, recursive, callback) {
    Command.setProperty(this.id, key, value, recursive, callback);
}

//StructrContent.prototype.setProperties = function(attributes) {
//    this.attributes = attributes;
//}

StructrContent.prototype.removeAttribute = function(key) {
    var self = this;
    delete self[key];
}

StructrContent.prototype.save = function() {
    StructrModel.save(this.id);
}

StructrContent.prototype.remove = function() {
    var element = Structr.node(this.id);
    if (this.parent) {
        var parent = Structr.node(this.parent.id);
    }
        
    if (element) element.remove();

    if (parent && !Structr.containsNodes(parent)) {
        _Entities.removeExpandIcon(parent);
    }
    _Pages.reloadPreviews();   
}

StructrContent.prototype.append = function(refNode) {
    
    var id = this.id;
    var parentId;
    
    var parent;
    if (this.parent) {
        parentId = this.parent.id;
        parent = Structr.node(parentId);
    }
		
    var div = _Contents.appendContentElement(this, refNode);
    if (!div) return false;

    log('appendContentElement div', div);

    if (parent) {
            
        $('.button', div).on('mousedown', function(e) {
            e.stopPropagation();
        });
            
        $('.delete_icon', div).replaceWith('<img title="Remove content element from parent ' + parentId + '" '
            + 'alt="Remove content element from parent ' + parentId + '" class="delete_icon button" src="' + _Contents.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            Command.removeChild(id);
        });
    }

    _Entities.setMouseOver(div);
        
    return div;
}