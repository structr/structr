/*
 *  Copyright (C) 2011 Axel Morgner
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

var buttonClicked;

var _Entities = {
	
    refreshEntities : function(type) {
        if (debug) console.log('refreshEntities(' + type + ')');
        var types = plural(type);
        var parentElement = $('#' + types);
        parentElement.empty();
        _Entities.showEntities(type);
        parentElement.append('<div style="clear: both"></div>');
        parentElement.append('<img title="Add ' + type + '" alt="Add ' + type + '" class="add_icon button" src="' + Structr.add_icon + '">');
        $('.add_icon', main).on('click', function() {
            _Entities.addEntity(this, type);
        });
        parentElement.append('<img title="Delete all ' + types + '" alt="Delete all ' + types + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', main).on('click', function() {
            deleteAll(this, type);
        });
    },

    showEntities : function(type) {
        if (debug) console.log('showEntities(' + type + ')');
        var data = '{ "command" : "LIST", "data" : { "type" : "' + type + '" } }';
        return send(data);
    },

    getTree : function(id) {
        if (debug) console.log('renderTree(' + id + ')');
        var data = '{ "command" : "TREE", "id" : "' + id + '" }';
        return send(data);
    },

    renderTree : function(parent, rootId) {
        if (debug) console.log('Entities.renderTree');
        var children = parent.children;
					
        if (children && children.length > 0) {
            $(children).each(function(i,child) {
                if (debug) console.log(child);
                if (child.type == "Resource") {
                    _Resources.appendResourceElement(child, parent.id, rootId);
                } else if (child.type == "Component") {
                    _Resources.appendComponentElement(child, parent.id, rootId);
                } else if (child.type == "Content") {
                    _Resources.appendContentElement(child, parent.id, rootId);
                } else if (child.type == "Folder") {
					var entity = child;
					console.log('Render Tree: ' , entity);
					var folderElement = _Files.appendFolderElement(child, parent.id);
//					var folders = entity.folders;
//					if (folders && folders.length > 0) {
//						disable($('.delete_icon', folderElement)[0]);
//						$(folders).each(function(i, folder) {
//							_Files.appendFolderElement(file, entity.id);
//						});
//					}
					var files = entity.files;
					if (files && files.length > 0) {
						disable($('.delete_icon', folderElement)[0]);
						$(files).each(function(i, file) {
							_Files.appendFileElement(file, entity.id);
						});
					}					
                } else {
                    _Resources.appendElementElement(child, parent.id, rootId);
                }
				
                _Entities.renderTree(child, rootId);
            });
        }
    },
	
    appendEntityElement : function(entity, parentElement) {
        if (debug) console.log(entity);
        var element;
        if (parentElement) {
            element = parentElement;
        } else {
            element = elements;
        //element = $('#' + plural(entity.type.toLowerCase()));
        }

        var type = entity.type ? entity.type : 'unknown';

        //    console.log(element);
        element.append('<div class="node ' + type.toLowerCase() + ' ' + entity.id + '_">'
            + (entity.iconUrl ? '<img class="typeIcon" src="' + entity.iconUrl + '">' : '')
            + '<b class="name_">' + entity.name + '</b> '
            + '<span class="id">' + entity.id + '</span>'
            + '</div>');
        div = $('.' + entity.id + '_', element);
        div.append('<img title="Delete ' + entity.name + ' [' + entity.id + ']" '
            + 'alt="Delete ' + entity.type + '\'' + entity.name + '\' [' + entity.id + ']" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function() {
            deleteNode(this, entity)
        });
        div.append('<img title="Edit ' + entity.name + ' [' + entity.id + ']" alt="Edit ' + entity.name + ' [' + entity.id + ']" class="edit_icon button" src="icon/pencil.png">');
        $('.edit_icon', div).on('click', function() {
            _Entities.showProperties(this, entity, 'all', $('.' + entity.id + '_', element));
        });
    },

    addSourceToTarget : function(sourceId, targetId, props) {
        if (debug) console.log('Add ' + sourceId + ' to ' + targetId + ' with additional properties: ' + props);
        var data = (sourceId ? '"id" : "' + sourceId + '" ' : '') + (props ? (sourceId ? ',' : '') + props : '');
        var command = '{ "command" : "ADD" , "id" : "' + targetId + '", "data" : {' + data + '} }';
        if (debug) console.log(command);
        return send(command);
    },

    removeSourceFromTarget : function(sourceId, targetId) {
        if (debug) console.log('Remove ' + sourceId + ' from ' + targetId);
        var data = '"id" : "' + sourceId + '"';
        var command = '{ "command" : "REMOVE" , "id" : "' + targetId + '", "data" : {' + data + '} }';
        if (debug) console.log(command);
        return send(command);
    },

    setProperty : function(id, key, value) {
        var command = '{ "command" : "UPDATE" , "id" : "' + id + '", "data" : { "' + key + '" : "' + value + '" } }';
        if (debug) console.log(command);
        return send(command);
    },

    setProperties : function(id, data) {
        var command = '{ "command" : "UPDATE" , "id" : "' + id + '", "data" : {' + data + '} }';
        if (debug) console.log(command);
        return send(command);
    },

    create : function(entity) {
        var toSend = {};
        toSend.data = entity;
        toSend.command = 'CREATE';
        if (debug) console.log($.toJSON(toSend));
        return send($.toJSON(toSend));
    },

    add : function(button, type, props) {
        if (debug) console.log('add new ' + type);
        if (isDisabled(button)) return false;
        disable(button);
        buttonClicked = button;
        disable(button);
        return _Entities.create($.parseJSON('{ "type" : "' + type + '", "name" : "New ' + type + ' ' + Math.floor(Math.random() * (999999 - 1)) + '" ' + (props ? ',' + props : '') + '}'));
    },

    hideProperties : function(button, entity, view, element) {
        element.find('.sep').remove();
        element.find('.props').remove();
        enable(button, function() {
            _Entities.showProperties(button, entity, view, element);
        });
    },

    showProperties : function(button, entity, view, dialog) {

        var dialog = $('#dialogText');
        dialog.empty();
        Structr.dialog('Edit Properties of ' + entity.id,
            function() {
                return true;
            },
            function() {
                return true;
            }
        );


        var debug = true
        if (isDisabled(button)) return;
        disable(button, function() {
            _Entities.hideProperties(button, entity, view, dialog);
        });

        if (debug) console.log('showProperties URL: ' + rootUrl + entity.id + (view ? '/' + view : ''));
        $.ajax({
            url: rootUrl + entity.id + (view ? '/' + view : ''),
            async: false,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            headers: headers,
            success: function(data) {
                //element.append('<div class="sep"></div>');
                //element.append('<table class="props"></table>');
                var keys = Object.keys(data.result);
				
                if (keys.length > 1) {
                    if (debug) console.log('element', dialog);
                    dialog.append('<table class="props"></table>');
                }
				
                $(keys).each(function(i, key) {

                    if (view == '_html_') {
                        $('.props', dialog).append('<tr><td class="key">' + key.replace(view, '') + '</td><td class="value ' + key + '_">' + formatValue(key, data.result[key]) + '</td></tr>');
                    } else {
                        $('.props', dialog).append('<tr><td class="key">' + formatKey(key) + '</td><td class="value ' + key + '_">' + formatValue(key, data.result[key]) + '</td></tr>');
                    }
                });

                $('.props tr td.value input', dialog).each(function(i,v) {
                    var input = $(v);
                    var oldVal = input.val();

                    input.on('focus', function() {
                        input.addClass('active');
                        input.parent().append('<img class="button icon cancel" src="icon/cross.png">');
                        input.parent().append('<img class="button icon save" src="icon/tick.png">');

                        $('.cancel', input.parent()).on('click', function() {
                            input.val(oldVal);
                            input.removeClass('active');
                        });

                        $('.save', input.parent()).on('click', function() {
                            _Entities.setProperty(entity.id, input.attr('name'), input.val());
                        });
                    });

                    input.on('change', function() {
                        input.data('changed', true);
                    });

                    input.on('focusout', function() {
                        _Entities.setProperty(entity.id, input.attr('name'), input.val());
                        input.removeClass('active');
                        input.parent().children('.icon').each(function(i, img) {
                            $(img).remove();
                        });
                    });

                });
            }
        });
    },

    showNonEmptyProperties : function(button, entity, view, element) {
        if (isDisabled(button)) return;
        disable(button, function() {
            _Entities.hideProperties(button, entity, view, element);

            element.on('mouseover', function(e) {
                e.stopPropagation();
                _Entities.showNonEmptyProperties(this, entity, '_html_', $(this));
            });

            element.on('mouseout', function(e) {
                e.stopPropagation();
                _Entities.hideNonEmptyProperties(this, entity, '_html_', $(this));
            });
			
        });

        //console.log(element);
        $.ajax({
            url: rootUrl + entity.id + (view ? '/' + view : ''),
            async: false,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            headers: headers,
            success: function(data) {
                //element.append('<div class="sep"></div>');
                //element.append('<table class="props"></table>');
                var keys = Object.keys(data.result);
				
                if (keys.length > 1) {
                    element.children('.delete_icon').after('<table class="props"></table>');
                }
				
                $(keys).each(function(i, key) {
					
                    if (data.result[key] && key.startsWith('_html_')) {

                        if (view == '_html_') {
                            $('.props', element).append('<tr><td class="key">' + key.replace(view, '') + '</td><td class="value ' + key + '_">' + formatValue(key, data.result[key]) + '</td></tr>');
                        } else {
                            $('.props', element).append('<tr><td class="key">' + formatKey(key) + '</td><td class="value ' + key + '_">' + formatValue(key, data.result[key]) + '</td></tr>');
                        }
                    }
                });

            }
        });
    },
	
    hideNonEmptyProperties : function(button, entity, view, element) {
        _Entities.hideProperties(button, entity, view, element);
    }

};

function plural(type) {
    var plural = type + 's';
    if (type.substring(type.length-1, type.length) == 'y') {
        plural = type.substring(0, type.length-1) + 'ies';
    }
    return plural;
}
