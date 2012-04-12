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
	_Entities.getEntities(type);
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

    getEntities : function(type) {
	var obj = {};
	obj.command = 'LIST';
	var data = {};
	data.type = type;
	obj.data = data;
	if (debug) console.log('showEntities()', obj);
	return sendObj(obj);
    },

    getEntity : function(id) {
	var obj = {};
	obj.command = 'GET';
	var data = {};
	data.id = id;
	obj.data = data;
	if (debug) console.log('getEntity()', obj);
	return sendObj(obj);
    },

    changeBooleanAttribute : function(attrElement, value) {

	if (debug) console.log('Change boolean attribute ', attrElement, ' to ', value);

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
	console.log(obj);
	return sendObj(obj);
    },

    setProperties : function(id, data) {
	var obj = {};
	obj.command = 'UPDATE';
	obj.id = id;
	obj.data = data;
	console.log(obj);
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
	disable(button);
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
	    debug = true;
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

	    tabs.append('<div id="tabView-' + view + '"></div>');

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
		url: rootUrl + entity.id + (view ? '/' + view : ''),
		async: false,
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
				
			tabView.append('<table class="props ' + view + '"></table>');

			var props = $('.props.' + view, tabView);
				
			$(keys).each(function(i, key) {

			    if (view == '_html_') {
				props.append('<tr><td class="key">' + key.replace(view, '') + '</td><td class="value ' + key + '_">' + formatValue(key, res[key]) + '</td></tr>');
			    } else if (view == 'in' || view == 'out') {
				props.append('<tr><td class="key">' + key + '</td><td class="value ' + key + '_">' + formatValue(key, res[key]) + '</td></tr>');
			    } else if (view == 'all') {
				props.append('<tr><td class="key">' + formatKey(key) + '</td><td class="value ' + key + '_">' + formatValue(key, res[key]) + '</td></tr>');
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
	    console.log(publicSwitch);

	    _Entities.getProperty(entity.id, 'visibleToPublicUsers', '#dialogBox');
	    _Entities.getProperty(entity.id, 'visibleToAuthenticatedUsers', '#dialogBox');

	    publicSwitch.on('click', function() {
		console.log('Toggle switch', publicSwitch.hasClass('disabled'))
		_Entities.setProperty(entity.id, 'visibleToPublicUsers', publicSwitch.hasClass('disabled'));
	    });

	    authSwitch.on('click', function() {
		console.log('Toggle switch', authSwitch.hasClass('disabled'))
		_Entities.setProperty(entity.id, 'visibleToAuthenticatedUsers', authSwitch.hasClass('disabled'));
	    });

	});
        
	keyIcon.on('mouseover', function(e) {
	    var self = $(this);
	    self.show();

	});
    }

};

function plural(type) {
    var plural = type + 's';
    if (type.substring(type.length-1, type.length) == 'y') {
	plural = type.substring(0, type.length-1) + 'ies';
    }
    return plural;
}
