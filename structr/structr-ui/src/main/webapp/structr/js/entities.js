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

var buttonClicked, timer = [];

var _Entities = {
	
    refreshEntities : function(type) {
	if (debug) console.log('refreshEntities(' + type + ')');
	var types = plural(type);
	var parentElement = $('#' + types);
	parentElement.empty();
	_Entities.list(type);
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

    list : function(type) {
	var obj = {};
	obj.command = 'LIST';
	var data = {};
	data.type = type;
	obj.data = data;
	if (debug) console.log('list()', obj);
	return sendObj(obj);
    },

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

    get : function(id) {
	var obj = {};
	obj.command = 'GET';
	var data = {};
	data.id = id;
	obj.data = data;
	if (debug) console.log('get()', obj);
	return sendObj(obj);
    },

    changeBooleanAttribute : function(attrElement, value) {

	console.log('Change boolean attribute ', attrElement, ' to ', value);

	if (value == true) {
	    attrElement.removeClass('disabled');
	    attrElement.addClass('enabled')
	} else {
	    attrElement.removeClass('enabled');
	    attrElement.addClass('disabled')
	}

    },

    getTree : function(id) {
	var obj = {};
	obj.command = 'TREE';
	obj.id = id;
	if (debug) console.log('renderTree()', obj);
	return sendObj(obj);
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

    appendObj : function(entity, parentId, resourceId) {

	// Check if object is not already contained
	var node = Structr.node(entity.id, parentId, resourceId);
	if (debug) console.log('appendObj', node);
	if (node && node.length > 0) return;

	if (entity.type == 'User') {
	    var groups = entity.groups;
	    if (!groups || groups.length == 0) {
		_UsersAndGroups.appendUserElement(entity, parentId, resourceId);
	    }

	} else if (entity.type == 'Group') {
	    var groupElement = _UsersAndGroups.appendGroupElement(entity);
	    var users = entity.users;
	    if (users && users.length > 0) {
		disable($('.delete_icon', groupElement)[0]);
		$(users).each(function(i, user) {
		    _UsersAndGroups.appendUserElement(user, entity.id);
		});
	    }

	} else if (entity.type == 'Resource') {

	    //_Entities.getTree(entity.id);

	    var resourceElement = _Resources.appendResourceElement(entity);
	//			var elements = entity.elements;
	//			if (elements && elements.length > 0) {
	//			    disable($('.delete_icon', respourceElement)[0]);
	//			    $(elements).each(function(i, element) {
	//				if (element.type == 'Element') {
	//				    _Resources.appendElementElement(element, entity.id);
	//				}
	//			    });
	//			}

	} else if (entity.type == 'Component') {

	    var componentElement = _Resources.appendComponentElement(entity, parentId, resourceId);
	    var elements = entity.elements;
	    if (elements && elements.length > 0) {
		disable($('.delete_icon', componentElement)[0]);
		$(elements).each(function(i, element) {
		    if (element.type == 'Element') {
			_Resources.appendElementElement(element, entity.id);
		    }
		});
	    }

	} else if (entity.type == 'Content') {
	    _Resources.appendContentElement(entity, parentId, resourceId);

	} else if (entity.type == 'Folder') {

	    var folderElement = _Files.appendFolderElement(entity);
	    var folders = entity.folders;
	    if (folders && folders.length > 0) {
		disable($('.delete_icon', folderElement)[0]);
		$(folders).each(function(i, folder) {
		    _Files.appendFolderElement(folder, entity.id);
		});
	    }
	    var images = entity.images;
	    if (images && images.length > 0) {
		disable($('.delete_icon', folderElement)[0]);
		$(images).each(function(i, image) {
		    _Files.appendImageElement(image, entity.id);
		});
	    }
	    var files = entity.files;
	    if (files && files.length > 0) {
		disable($('.delete_icon', folderElement)[0]);
		$(files).each(function(i, file) {

		    if (file.type == 'File') { // files comprise images
			_Files.appendFileElement(file, entity.id);
		    }

		});
	    }

	} else if (entity.type == 'Image') {
	    if (debug) console.log('Image:', entity);
	    var imageFolder = entity.folder;
	    if (!imageFolder || imageFolder.length == 0) {
		_Files.appendImageElement(entity);
	    }

	} else if (entity.type == 'File') {
	    if (debug) console.log('File: ', entity);
	    var parentFolder = entity.folder;
	    if (!parentFolder || parentFolder.length == 0) {
		_Files.appendFileElement(entity);
	    }

	} else {
	    if (debug) console.log('Entity: ', entity);
	    var elementElement = _Resources.appendElementElement(entity, parentId, resourceId);
	    var elem = entity.elements;
	    if (elem && elem.length > 0) {
		if (debug) console.log(elem);
		disable($('.delete_icon', elementElement)[0]);
		$(elem).each(function(i, element) {
		    if (elem.type == 'Element') {
			_Resources.appendElementElement(element, entity.id);
		    } else if (elem.type == 'Content') {
			_Resources.appendContentElement(element, entity.id);
		    }
		});
	    }
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
	    _Entities.showProperties(this, entity, $('.' + entity.id + '_', element));
	});
    },

    removeSourceFromTarget : function(sourceId, targetId) {
	if (debug) console.log('Remove ' + sourceId + ' from ' + targetId);
	var obj = {};
	obj.command = 'REMOVE';
	obj.id = targetId;
	var data = {};
	data.id = sourceId;
	obj.data = data;
	if (debug) console.log(obj);
	return sendObj(obj);
    },

    getProperty : function(id, key, elementId) {
	var obj = {};
	obj.command = 'GET';
	obj.id = id;
	var data = {};
	data.key = key;

	if (key.startsWith('_html_')) {
	    obj.view = '_html_';
	}

	data.displayElementId = elementId;
	obj.data = data;
	if (debug) console.log(obj);
	return sendObj(obj);
    },

    setProperty : function(id, key, value) {
	var obj = {};
	obj.command = 'UPDATE';
	obj.id = id;
	var data = {};
	data[key] = value;
	obj.data = data;
	if (debug) console.log(obj);
	return sendObj(obj);
    },

    setProperties : function(id, data) {
	var obj = {};
	obj.command = 'UPDATE';
	obj.id = id;
	obj.data = data;
	if (debug) console.log(obj);
	return sendObj(obj);
    },

    createAndAdd : function(id, nodeData, relData) {
	if (!nodeData.name) {
	    nodeData.name = 'New ' + nodeData.type + ' ' + Math.floor(Math.random() * (999999 - 1));
	}
	var obj = {};
	obj.command = 'ADD';
	obj.id = id;
	obj.data = nodeData;
	obj.relData = relData;
	if (debug) console.log('_Entities.createAndAdd', obj);
	return sendObj(obj);
    },

    create : function(button, entity) {
	if (isDisabled(button)) return false;
	disable(button);
	buttonClicked = button;
	var obj = {};
	obj.command = 'CREATE';
	if (!entity.name) {
	    entity.name = 'New ' + entity.type + ' ' + Math.floor(Math.random() * (999999 - 1));
	}
	obj.data = entity;
	if (debug) console.log('create new entity', obj);
	return sendObj(obj);
    },

    hideProperties : function(button, entity, element) {
	enable(button, function() {
	    _Entities.showProperties(button, entity, element);
	});
	element.find('.sep').remove();
	element.find('.props').remove();
    },

    showProperties : function(button, entity, dialog) {

	var views;
	    
	if (entity.type == 'Content') {
	    views = ['all', 'in', 'out' ];
	} else {
	    views = ['all', 'in', 'out', '_html_'];
	}

	dialog.empty();
	Structr.dialog('Edit Properties of ' + entity.id,
	    function() {
		return true;
	    },
	    function() {
		return true;
	    }
	    );

	//        if (isDisabled(button)) return;
	//        disable(button, function() {
	//            _Entities.hideProperties(button, entity, view, dialog);
	//        });

	dialog.append('<div id="tabs"><ul></ul>');

	$(views).each(function(i, view) {
	    var tabs = $('#tabs', dialog);

	    var tabText;
	    if (view == 'all') {
		tabText = 'Node';
	    } else if (view == '_html_') {
		tabText = 'HTML';
	    } else if (view == 'in') {
		tabText = 'Incoming';
	    } else if (view == 'out') {
		tabText = 'Outgoing';
	    } else {
		tabText = 'Other';
	    }

	    $('ul', tabs).append('<li class="' + (view == 'all' ? 'active' : '') + '" id="tab-' + view + '">' + tabText + '</li>');

	    tabs.append('<div id="tabView-' + view + '"><br></div>');

	    var tab = $('#tab-' + view);

	    tab.on('click', function() {
		var self = $(this);
		tabs.children('div').hide();
		$('li', tabs).removeClass('active');
		$('#tabView-' + view).show();
		self.addClass('active');
	    });

	    var tabView = $('#tabView-' + view);
	    if (view != 'all') tabView.hide();


	    var headers = {
		'X-StructrSessionToken' : token
	    };
	    if (debug) console.log('showProperties URL: ' + rootUrl + entity.id + (view ? '/' + view : ''), headers);
	    $.ajax({
		url: rootUrl + entity.id + (view ? '/' + view : '') + '?pageSize=10',
		async: true,
		dataType: 'json',
		contentType: 'application/json; charset=utf-8',
		headers: headers,
		success: function(data) {
		    //element.append('<div class="sep"></div>');
		    //element.append('<table class="props"></table>');
		    if (debug) console.log(data.result);
		    $(data.result).each(function(i, res) {
			
			var keys = Object.keys(res);

			if (debug ) console.log('keys', keys);

			//			if (view == 'in' || view == 'out') {
			//			    tabView.append('<br><h3>Relationship ' + res['id']+ '</h3>')
			//			}
				
			tabView.append('<table class="props ' + view + '_' + res['id'] +'"></table>');

			var props = $('.props.' + view + '_' + res['id'], tabView);
				
			$(keys).each(function(i, key) {

			    if (view == '_html_') {
				props.append('<tr><td class="key">' + key.replace(view, '') + '</td><td class="value ' + key + '_">' + formatValue(key, res[key]) + '</td></tr>');
			    } else if (view == 'in' || view == 'out') {
				props.append('<tr><td class="key">' + key + '</td><td class="value ' + key + '_">' + formatValue(key, res[key]) + '</td></tr>');
			    } else {
				props.append('<tr><td class="key">' + formatKey(key) + '</td><td class="value ' + key + '_">' + formatValue(key, res[key]) + '</td></tr>');
			    }

			});

			$('.props tr td.value input', dialog).each(function(i,v) {
			    var input = $(v);
			    var oldVal = input.val();

			    input.on('focus', function() {
				input.addClass('active');
			    //								input.parent().append('<img class="button icon cancel" src="icon/cross.png">');
			    //								input.parent().append('<img class="button icon save" src="icon/tick.png">');

			    //				$('.cancel', input.parent()).on('click', function() {
			    //				    input.val(oldVal);
			    //				    input.removeClass('active');
			    //				});
			    //
			    //				$('.save', input.parent()).on('click', function() {
			    //				    _Entities.setProperty(entity.id, input.attr('name'), input.val());
			    //				});
			    });

			    input.on('change', function() {
				input.data('changed', true);
				_Resources.reloadPreviews();
			    });

			    input.on('focusout', function() {
				_Entities.setProperty(entity.id, input.attr('name'), input.val());
				input.removeClass('active');
				input.parent().children('.icon').each(function(i, img) {
				    $(img).remove();
				});
			    });

			});
		    });

		}
	    });
	    debug = false;
	});

    //$( "#tabs" ).tabs();

    },

    appendAccessControlIcon: function(parent, entity, hide) {

	parent.append('<img title="Access Control and Visibility" alt="Access Control and Visibility" class="key_icon button" src="' + Structr.key_icon + '">');

	var keyIcon = $('.key_icon', parent);
	if (hide) keyIcon.hide();
	keyIcon.on('click', function(e) {
	    e.stopPropagation();
	    Structr.dialog('Access Control and Visibility', function() {}, function() {});
	    var dt = $('#dialogBox .dialogText');

	    dt.append('<h3>Owner</h3><p class="nodeSelectBox" id="ownersBox"></p>');
	    _Entities.getProperty(entity.id, 'ownerId', '#ownersBox');

	    dt.append('<h3>Visibility</h3><div class="' + entity.id + '_"><button class="switch disabled visibleToPublicUsers_">Public (visible to anyone)</button><button class="switch disabled visibleToAuthenticatedUsers_">Authenticated Users</button></div>');
	    var publicSwitch = $('.visibleToPublicUsers_');
	    var authSwitch = $('.visibleToAuthenticatedUsers_');

	    _Entities.getProperty(entity.id, 'visibleToPublicUsers', '#dialogBox');
	    _Entities.getProperty(entity.id, 'visibleToAuthenticatedUsers', '#dialogBox');

	    if (debug) console.log(publicSwitch);
	    if (debug) console.log(authSwitch);

	    publicSwitch.on('click', function() {
		if (debug) console.log('Toggle switch', publicSwitch.hasClass('disabled'))
		_Entities.setProperty(entity.id, 'visibleToPublicUsers', publicSwitch.hasClass('disabled'));
	    });

	    authSwitch.on('click', function() {
		if (debug) console.log('Toggle switch', authSwitch.hasClass('disabled'))
		_Entities.setProperty(entity.id, 'visibleToAuthenticatedUsers', authSwitch.hasClass('disabled'));
	    });

	});
        
	keyIcon.on('mouseover', function(e) {
	    var self = $(this);
	    self.show();

	});
    },

    appendEditPropertiesIcon : function(el, entity) {

	if (entity.type != 'Content') {
	    el.append('<span class="_html_id_">#</span> <span class="_html_class_">.</span>');
	}

	//_Entities.getProperty(entity.id, '_html_id');
	_Entities.getProperty(entity.id, '_html_class');

	el.append('<img title="Edit Properties" alt="Edit Properties" class="edit_props_icon button" src="' + '/structr/icon/application_view_detail.png' + '">');
	$('.edit_props_icon', el).on('click', function(e) {
	    e.stopPropagation();
	    if (debug) console.log('showProperties', entity);
	    _Entities.showProperties(this, entity, $('#dialogBox .dialogText'));
	});

    //	$('b', el).on('click', function(e) {
    //	    e.stopPropagation();
    //	    _Entities.showProperties(this, entity, $('#dialogBox .dialogText'));
    //	});

    },

    //    setClick : function(el) {
    //	el.on('click', function(e) {
    //	    e.stopPropagation();
    //	    var resourceId = getId($(this).closest('.resource'));
    //	    console.log('Clicked on element', el, resourceId);
    //	    var id = getId(el);
    //	    _Entities.children(id, resourceId);
    //	});
    //    },
    appendExpandIcon : function(el, entity) {
	el.append('<img title="Expand \'' + entity.name + '\'" alt="Expand \'' + entity.name + '\'" class="expand_icon" src="' + Structr.expand_icon + '">');
	var button = $('.expand_icon', el).first();

	if (button) {

	    button.on('click', function() {
		_Entities.toggleElement(this);
	    });
	    if (debug) console.log(isExpanded(entity.id, null, entity.resourceId), entity);
	    if (isExpanded(entity.id, null, entity.resourceId)) {
		if (debug) console.log('toggle', entity.id, entity.resourceId);
		_Entities.toggleElement(button)
	    }
	}

    },

    setMouseOver : function(el) {
	el.on({
	    mouseover: function(e) {
		e.stopPropagation();
		var self = $(this);
		var nodeId = getId(el);
		//window.clearTimeout(timer[nodeId]);
		//		console.log('setMouseOver', nodeId);
		var nodes = $('.' + nodeId + '_');
		var resource = $(el).closest('.resource');
		if (resource.length) {
		    var resId = getId(resource);
		    //console.log('setMouseOver resourceId', resId);
		    var previewNodes = $('#preview_' + resId).contents().find('[structr_element_id]');
		    previewNodes.each(function(i,v) {
			var self = $(v);
			var sid = self.attr('structr_element_id');
			if (sid == nodeId) {
			    self.addClass('nodeHover');
			}
		    });

		//		    _Entities.children(nodeId, resId);

		}
		nodes.addClass('nodeHover');
		self.children('img.button').show();
	    },
	    mouseout: function(e) {
		e.stopPropagation();
		_Entities.resetMouseOverState(this);
	    }
	});
    },

    resetMouseOverState : function(element) {
	var el = $(element);
	el.removeClass('nodeHover');
	el.children('img.button').hide();
	var nodeId = getId(el);
	var nodes = $('.' + nodeId + '_');
	//	timer[nodeId] = window.setTimeout(function() {
	//	    el.children('.node').remove();
	//	}, 1000);
	nodes.removeClass('nodeHover');
	var resource = $(el).closest('.resource');
	if (resource.length) {
	    var resId = getId(resource);
	    //		    console.log('setMouseOver resourceId', resId);
	    var previewNodes = $('#preview_' + resId).contents().find('[structr_element_id]');
	    previewNodes.each(function(i,v) {
		var self = $(v);
		var sid = self.attr('structr_element_id');
		if (sid == nodeId) {
		    if (debug) console.log(sid);
		    self.removeClass('nodeHover');
		    if (debug) console.log(self);
		}
	    });
	}
    },

    toggleElement : function(button) {

	var b = $(button);
	var src = b.attr('src');

	var nodeElement = $(button).parent();
	var id = getId(nodeElement);
	var resId = getId(nodeElement.closest('.resource'));

	if (src.endsWith('icon/tree_arrow_down.png')) {
	    nodeElement.children('.node').remove();
	    b.attr('src', 'icon/tree_arrow_right.png');

	    removeExpandedNode(id, null, resId);
	} else {
	    _Entities.children(id, resId);
	    b.attr('src', 'icon/tree_arrow_down.png');

	    addExpandedNode(id, null, resId);
	}

    //console.log(Structr.expanded[resId]);
    }
};

function plural(type) {
    var plural = type + 's';
    if (type.substring(type.length-1, type.length) == 'y') {
	plural = type.substring(0, type.length-1) + 'ies';
    }
    return plural;
}

function addExpandedNode(id, parentId, resourceId) {

    var expandedIds = getExpanded()[resourceId];
    if (!expandedIds) {
	expandedIds = {};
	Structr.expanded[resourceId] = expandedIds;
    }

    expandedIds[id] = true;

}

function removeExpandedNode(id, parentId, resourceId) {
    var expandedIds = getExpanded()[resourceId];
    if (!expandedIds) {
	expandedIds = {};
	Structr.expanded[resourceId] = expandedIds;
    }

    expandedIds[id] = false;

}

function isExpanded(id, parentId, resourceId) {

    var expandedIds = getExpanded()[resourceId];
    if (!expandedIds) {
	expandedIds = {};
	Structr.expanded[resourceId] = expandedIds;
    }
    var isExpanded = expandedIds[id] ? expandedIds[id] : false;

    if (debug) console.log('node ' + id + ' in resource ' + resourceId + ' expanded?', isExpanded);

    return isExpanded;
}

function getExpanded() {

    if (!Structr.expanded) {
	Structr.expanded = {};
    }

    return Structr.expanded;
}