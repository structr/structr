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

var Entities = {

	refreshEntities : function(type) {
		if (debug) console.log('refreshEntities(' + type + ')');
		var types = plural(type);
		var parentElement = $('#' + types);
		parentElement.empty();
		Entities.showEntities(type);
		parentElement.append('<div style="clear: both"></div>');
		parentElement.append('<img title="Add ' + type + '" alt="Add ' + type + '" class="add_icon button" src="icon/add.png">');
		$('.add_icon', main).on('click', function() {
			Entities.addEntity(type, this);
		});
		parentElement.append('<img title="Delete all ' + types + '" alt="Delete all ' + types + '" class="delete_icon button" src="icon/delete.png">');
		$('.delete_icon', main).on('click', function() {
			deleteAll(this, type);
		});
	},

	showEntities : function(type) {
		if (debug) console.log('showEntities(' + type + ')');
		var data = '{ "command" : "LIST", "data" : { "type" : "' + type + '" } }';
		return send(data);
	},

	appendEntityElement : function(entity, parentElement) {
		var element;
		if (parentElement) {
			element = parentElement;
		} else {
			element = $('#' + plural(entity.type.toLowerCase()));
		}
		//    console.log(element);
		element.append('<div class="nested top ' + entity.type.toLowerCase() + ' ' + entity.id + '_">'
			+ (entity.iconUrl ? '<img class="typeIcon" src="' + entity.iconUrl + '">' : '')
			+ '<b class="name">' + entity.name + '</b> '
			+ '[' + entity.id + ']'
			+ '</div>');
		div = $('.' + entity.id + '_', element);
		div.append('<img title="Delete ' + entity.name + ' [<span class="id">' + entity.id + '</span>]" '
			+ 'alt="Delete ' + entity.name + ' [' + entity.id + ']" class="delete_icon button" src="icon/delete.png">');
		$('.delete_icon', div).on('click', function() {
			deleteNode(this, entity)
		});
		div.append('<img title="Edit ' + entity.name + ' [' + entity.id + ']" alt="Edit ' + entity.name + ' [' + entity.id + ']" class="edit_icon button" src="icon/pencil.png">');
		$('.edit_icon', div).on('click', function() {
			showProperties(this, entity, 'all', $('.' + entity.id + '_', element));
		});
	},

	addSourceToTarget : function(sourceId, targetId) {
		if (debug) console.log('Add ' + sourceId + ' to ' + targetId);
		var data = '{ "command" : "ADD" , "id" : "' + targetId + '" , "data" : { "id" : "' + sourceId + '" } }';
		return send(data);
	},

	removeSourceFromTarget : function(sourceId, targetId) {
		if (debug) console.log('Remove ' + sourceId + ' from ' + targetId);
		var data = '{ "command" : "REMOVE" , "id" : "' + targetId + '" , "data" : { "id" : "' + sourceId + '" } }';
		return send(data);
	},

	createEntity : function(entity, parentElement) {
		var toSend = {};
		toSend.data = entity;
		toSend.command = 'CREATE';
		if (debug) console.log($.toJSON(toSend));
		return send($.toJSON(toSend));
	},

	addEntity : function(type, button) {
		buttonClicked = button;
		if (isDisabled(button)) return;
		disable(button);
		Entities.createEntity($.parseJSON('{ "type" : "' + type + '", "name" : "New ' + type + ' ' + Math.floor(Math.random() * (999999 - 1)) + '" }'));
	},

	hideProperties : function(button, entity, view, element) {
		element.children('.sep').remove();
		element.children('.props').remove();
		enable(button, function() {
			Entities.showProperties(button, entity, view, element);
		});
	},

	showProperties : function(button, entity, view, element) {
		if (isDisabled(button)) return;
		disable(button, function() {
			Entities.hideProperties(button, entity, view, element);
		});
		//console.log(element);
		$.ajax({
			url: rootUrl + entity.id + (view ? '/' + view : ''),
			async: false,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			headers: headers,
			success: function(data) {
				element.append('<div class="sep"></div>');
				element.append('<table class="props"></table>');
				var keys = Object.keys(data.result);
				$(keys).each(function(i, key) {
					$('.props', element).append('<tr><td class="key">' + formatKey(key) + '</td><td class="value ' + key + '">' + formatValue(key, data.result[key]) + '</td></tr>');
				});
        
				$('.props tr td.value input', element).each(function(i,v) {
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
							var key = input.attr('name');
							var value = input.val();
							send('{ "command" : "UPDATE" , "id" : "' + entity.id + '", "' + key + '" : "' + value + '" }');
						});
					});
          
					input.on('change', function() {
						input.data('changed', true);
					});
                   
					input.on('focusout', function() {
            
						if (input.data('changed') && confirm('Save changes?')) {
              
							var key = input.attr('name');
							var value = input.val();
							var data = '{ "command" : "UPDATE" , "id" : "' + entity.id + '", "data" : { "' + key + '" : "' + value + '" } }';
							if (debug) console.log(data);
							send(data);
						}
						input.removeClass('active');
						input.parent().children('.icon').each(function(i, img) {
							$(img).remove();
						});
					});

				});
			}
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
