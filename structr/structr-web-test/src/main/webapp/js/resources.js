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

var resources;
var previews;

$(document).ready(function() {
	Structr.registerModule('resources', Resources);
});

var Resources = {
	
	init : function() {
	},

	onload : function() {
		//Structr.activateMenuEntry('resources');
		if (debug) console.log('onload');
		main.append('<table id="resourcesEditor"><tr><td id="resources"></td><td id="elements"></td><td id="contents"></td><td id="previews"></td></tr></table>');

		resources = $('#resources');
		elements = $('#elements');
		contents = $('#contents');
		previews = $('#previews');

		Resources.refresh();
		Elements.refresh();
		Contents.refresh();
	},

	refresh : function() {
		resources.empty();
		if (Resources.show()) {
			resources.append('<button class="add_resource_icon button"><img title="Add Resource" alt="Add Resource" src="icon/page_add.png"> Add Resource</button>');
			$('.add_resource_icon', main).on('click', function() {
				Resources.addResource(this);
			});
		}
	},

	refreshElements : function() {
		elements.empty();
		if (Elements.show()) {
			elements.append('<button class="add_element_icon button"><img title="Add Element" alt="Add Element" src="icon/brick_add.png"> Add Element</button>');
			$('.add_element_icon', main).on('click', function() {
				Resources.addElement(this);
			});
		}
	},

	show : function() {
		return Entities.showEntities('Resource');

	//        $.ajax({
	//            url: rootUrl + 'resources',
	//            dataType: 'json',
	//            contentType: 'application/json; charset=utf-8',
	//            //headers: { 'X-User' : 457 },
	//            success: function(data) {
	//                if (!data || data.length == 0 || !data.result) return;
	//                if ($.isArray(data.result)) {
	//
	//                    for (var i=0; i<data.result.length; i++) {
	//                        var resource = data.result[i];
	//
	//                        Resources.appendResourceElement(resource);
	//
	//                        var resourceId = resource.id;
	//                        //          $('#resources').append('<div class="editor_box"><div class="nested top resource" id="resource_' + id + '">'
	//                        //                              + '<b>' + resource.name + '</b>'
	//                        //                              //+ ' [' + id + ']'
	//                        //                              + '<img class="add_icon button" title="Add Element" alt="Add Element" src="icon/add.png" onclick="addElement(' + id + ', \'#resource_' + id + '\')">'
	//                        //                              + '<img class="delete_icon button" title="Delete '
	//                        //                              + resource.name + '" alt="Delete '
	//                        //                              + resource.name + '" src="icon/delete.png" onclick="deleteNode(' + id + ', \'#resource_' + id + '\')">'
	//                        //                              + '</div></div>');
	//                        Resources.showSubEntities(resourceId, null);
	//
	//                        $('#previews').append('<a target="_blank" href="' + viewRootUrl + resource.name + '">' + viewRootUrl + resource.name + '</a><br><div class="preview_box"><iframe id="preview_'
	//                            + resourceId + '" src="' + viewRootUrl + resource.name + '?edit"></iframe></div><div style="clear: both"></div>');
	//
	//                        $('#preview_' + resourceId).load(function() {
	//                            //console.log(this);
	//                            var doc = $(this).contents();
	//                            var head = $(doc).find('head');
	//                            head.append('<style type="text/css">'
	//                                + '.structr-editable-area {'
	//                                + 'border: 1px dotted #a5a5a5;'
	//                                + 'margin: 2px;'
	//                                + 'padding: 2px;'
	//                                + '}'
	//                                + '.structr-editable-area-active {'
	//                                + 'border: 1px dotted #orange;'
	//                                + 'margin: 2px;'
	//                                + 'padding: 2px;'
	//                                + '}'
	//                                + '</style>');
	//
	//
	//                            $(this).contents().find('.structr-editable-area').each(function(i,element) {
	//                                //console.log(element);
	//                                $(element).addClass('structr-editable-area');
	//                                $(element).on({
	//                                    mouseenter: function() {
	//                                        var self = $(this);
	//                                        self.attr('contenteditable', true);
	//                                        self.addClass('structr-editable-area-active');
	//                                    },
	//                                    mouseleave: function() {
	//                                        var self = $(this);
	//                                        self.attr('contenteditable', true);
	//                                        self.removeClass('structr-editable-area-active');
	//                                        var id = self.attr('id');
	//                                        id = lastPart(id, '-');
	//                                        Resources.updateContent(id, this.innerHTML);
	//                                    }
	//                                });
	//                            });
	//                        });
	//
	//                    }
	//                }
	//            }
	//        });
	//
	//        return true;
	},

	showElements : function() {
		return Resources.showEntities('Element');
	},

	appendResourceElement : function(resource) {
		resources.append('<div class="nested top resource ' + resource.id + '_">'
			+ '<img class="typeIcon" src="icon/page.png">'
			+ '<b class="name">' + resource.name + '</b> <span class="id">' + resource.id + '</span>'
			+ '</div>');
		var div = $('.' + resource.id + '_', resources);
		div.append('<img title="Delete resource \'' + resource.name + '\'" alt="Delete resource \'' + resource.name + '\'" class="delete_icon button" src="icon/page_delete.png">');
		$('.delete_icon', div).on('click', function() {
			Resources.deleteResource(this, resource);
		});
		//        div.append('<img class="add_icon button" title="Add Element" alt="Add Element" src="icon/add.png">');
		//        $('.add_icon', div).on('click', function() {
		//            Resources.addElement(this, resource);
		//        });
		$('b', div).on('click', function() {
			Entities.showProperties(this, resource, 'all', $('.' + resource.id + '_', resources));
		});

		var elements = resource.children;
					
		if (elements && elements.length > 0) {
			disable($('.delete_icon', div));
			$(elements).each(function(i, child) {
							
				console.log("type: " + child.type);
				if (child.type == "Element") {
					Resources.appendElementElement(child, resource.id);
				} else if (child.type == "Content") {
					Contents.appendContentElement(child, resource.id)
				}
			});
		}				


		div.droppable({
			accept: '.element',
			hoverClass: 'resourceHover',
			drop: function(event, ui) {
				var elementId = getIdFromClassString(ui.draggable.attr('class'));
				var resourceId = getIdFromClassString($(this).attr('class'));
				var pos = $('.element', $(this)).length;
				console.log(pos);
				var props = '"' + resourceId + '" : "' + pos + '"';
				console.log(props);
				Entities.addSourceToTarget(elementId, resourceId, props);
			}
		});

		return div;
	},

	appendElementElement : function(element, parentId) {
		var div = Elements.appendElementElement(element, parentId);
		//console.log(div);
		if (parentId) {
			var parent = $('.' + parentId + '_');

			$('.delete_icon', div).remove();
			div.append('<img title="Remove element \'' + element.name + '\' from resource ' + parentId + '" '
				+ 'alt="Remove element ' + element.name + ' from resources ' + parentId + '" class="delete_icon button" src="icon/brick_delete.png">');
			$('.delete_icon', div).on('click', function() {
				Entities.removeSourceFromTarget(element.id, parentId);
			});
		}
		var elements = element.children;
		
		if (elements && elements.length > 0) {
			disable($('.delete_icon', div));
			$(elements).each(function(i, child) {
				if (child.type == "Element") {
					Resources.appendElementElement(child, element.id);
				} else if (child.type == "Content") {
					Contents.appendContentElement(child, element.id)
				}
			});
		}

		div.draggable({
			revert: 'invalid',
			containment: '#main',
			zIndex: 1
		});

		div.droppable({
			accept: '.content',
			hoverClass: 'elementHover',
			drop: function(event, ui) {
				var resource = $(this).closest( '.resource')[0];
				console.log(resource);
				var contentId = getIdFromClassString(ui.draggable.attr('class'));
				var elementId = getIdFromClassString($(this).attr('class'));
				var pos = $('.content', $(this)).length;
				console.log(pos);
				var props;
				if (resource) {
					var resourceId = getIdFromClassString($(resource).attr('class'));
					props = '"' + resourceId + '" : "' + pos + '"';
				}
				console.log(props);
				Entities.addSourceToTarget(contentId, elementId, props);
			}
		});

		return div;
	},

	appendContentElement : function(content, parentId) {
		var div = Contents.appendContentElement(content, parentId);
		//console.log(div);
		if (parentId) {
			$('.delete_icon', div).remove();
			div.append('<img title="Remove element \'' + content.name + '\' from resource ' + parentId + '" '
				+ 'alt="Remove content ' + content.name + ' from element ' + parentId + '" class="delete_icon button" src="icon/brick_delete.png">');
			$('.delete_icon', div).on('click', function() {
				Entities.removeSourceFromTarget(content.id, parentId)
			});
		}

		div.draggable({
			revert: 'invalid',
			containment: '#main',
			zIndex: 1
		});
		return div;
	},

	addElementToResource : function(elementId, resourceId) {

		var resource = $('.' + resourceId + '_');
		var element = $('.' + elementId + '_');

		resource.append(element);

		$('.delete_icon', element).remove();
		element.append('<img title="Remove element ' + elementId + ' from resource ' + resourceId + '" '
			+ 'alt="Remove element ' + elementId + ' from resource ' + resourceId + '" class="delete_icon button" src="icon/brick_delete.png">');
		$('.delete_icon', element).on('click', function() {
			Resources.removeElementFromResource(elementId, resourceId)
		});
		element.draggable('destroy');

		var numberOfElements = $('.element', resource).size();
		if (debug) console.log(numberOfElements);
		if (numberOfElements > 0) {
			disable($('.delete_icon', resource)[0]);
		}

	},

	removeElementFromResource : function(elementId, resourceId) {

		var resource = $('.' + resourceId + '_');
		var element = $('.' + elementId + '_', resource);
		element.remove();

		var numberOfElements = $('.element', resource).size();
		if (debug) console.log(numberOfElements);
		if (numberOfElements == 0) {
			enable($('.delete_icon', resource)[0]);
		}
		Entities.removeSourceFromTarget(elementId, resourceId);

	},

	addContentToElement : function(contentId, elementId) {

		var element = $('.' + elementId + '_');
		var content = $('.' + contentId + '_');

		element.append(content);

		$('.delete_icon', content).remove();
		content.append('<img title="Remove content ' + contentId + ' from element ' + elementId + '" '
			+ 'alt="Remove content ' + contentId + ' from element ' + elementId + '" class="delete_icon button" src="icon/page_white_delete.png">');
		$('.delete_icon', content).on('click', function() {
			Resources.removeElementFromResource(contentId, elementId)
		});
		content.draggable('destroy');

		var numberOfContents = $('.element', element).size();
		if (debug) console.log(numberOfContents);
		if (numberOfContents > 0) {
			disable($('.delete_icon', element)[0]);
		}

	},

	removeContentFromElement : function(contentId, elementId) {

		var element = $('.' + elementId + '_');
		var content = $('.' + contentId + '_', element);
		content.remove();

		var numberOfContents = $('.element', element).size();
		if (debug) console.log(numberOfContents);
		if (numberOfContents == 0) {
			enable($('.delete_icon', element)[0]);
		}
		Entities.removeSourceFromTarget(contentId, elementId);

	},

	updateContent : function(contentId, content) {
		//console.log('update ' + contentId + ' with ' + content);
		var url = rootUrl + 'content' + '/' + contentId;
		var data = '{ "content" : ' + $.quoteString(content) + ' }';
		$.ajax({
			url: url,
			//async: false,
			type: 'PUT',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			data: data,
			success: function(data) {
			//refreshIframes();
			//keyEventBlocked = true;
			//enable(button);
			//console.log('success');
			}
		});
	},

	addResource : function(button) {
		return Entities.add(button, 'Resource');
	},

	deleteResource : function(button, resource) {
		if (debug) console.log('delete resource ' + resource);
		deleteNode(button, resource);
	},

	showSubEntities : function(resourceId, entity) {
		var follow = followIds(resourceId, entity);
		$(follow).each(function(i, nodeId) {
			if (nodeId) {
				//            console.log(rootUrl + nodeId);
				$.ajax({
					url: rootUrl + nodeId,
					dataType: 'json',
					contentType: 'application/json; charset=utf-8',
					async: false,
					//headers: { 'X-User' : 457 },
					success: function(data) {
						if (!data || data.length == 0 || !data.result) return;
						var result = data.result;
						//                    console.log(result);
						Resources.appendElement(result, entity, resourceId);
						Resources.showSubEntities(resourceId, result);
					}
				});
			}
		});
	},

	appendElement : function(entity, parentEntity, resourceId) {
		//    console.log('appendElement: resourceId=' + resourceId);
		//    console.log(entity);
		//    console.log(parentEntity);
		var type = entity.type.toLowerCase();
		var id = entity.id;
		var resourceEntitySelector = $('.' + resourceId + '_');
		var element = (parentEntity ? $('.' + parentEntity.id + '_', resourceEntitySelector) : resourceEntitySelector);
		//    console.log(element);
		Entities.appendEntityElement(entity, element);

		if (type == 'content') {
			div.append('<img title="Edit Content" alt="Edit Content" class="edit_icon button" src="icon/pencil.png">');
			$('.edit_icon', div).on('click', function() {
				editContent(this, resourceId, id)
			});
		} else {
			div.append('<img title="Add" alt="Add" class="add_icon button" src="icon/add.png">');
			$('.add_icon', div).on('click', function() {
				addNode(this, 'content', entity, resourceId)
			});
		}
		//    //div.append('<img class="sort_icon" src="icon/arrow_up_down.png">');
		div.sortable({
			axis: 'y',
			appendTo: '.' + resourceId + '_',
			delay: 100,
			containment: 'parent',
			cursor: 'crosshair',
			//handle: '.sort_icon',
			stop: function() {
				$('div.nested', this).each(function(i,v) {
					var nodeId = getIdFromClassString($(v).attr('class'));
					if (!nodeId) return;
					var url = rootUrl + nodeId + '/' + 'in';
					$.ajax({
						url: url,
						dataType: 'json',
						contentType: 'application/json; charset=utf-8',
						async: false,
						headers: headers,
						success: function(data) {
							if (!data || data.length == 0 || !data.result) return;
							//                        var rel = data.result;
							//var pos = rel[parentId];
							var nodeUrl = rootUrl + nodeId;
							setPosition(resourceId, nodeUrl, i)
						}
					});
					refreshIframes();
				});
			}
		});
	},


	addNode : function(button, type, entity, resourceId) {
		if (isDisabled(button)) return;
		disable(button);
		var pos = $('.' + resourceId + '_ .' + entity.id + '_ > div.nested').length;
		//    console.log('addNode(' + type + ', ' + entity.id + ', ' + entity.id + ', ' + pos + ')');
		var url = rootUrl + type;
		var resp = $.ajax({
			url: url,
			//async: false,
			type: 'POST',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			headers: headers,
			data: '{ "type" : "' + type + '", "name" : "' + type + '_' + Math.floor(Math.random() * (9999 - 1)) + '", "elements" : "' + entity.id + '" }',
			success: function(data) {
				var getUrl = resp.getResponseHeader('Location');
				$.ajax({
					url: getUrl + '/all',
					success: function(data) {
						var node = data.result;
						if (entity) {
							Resources.appendElement(node, entity, resourceId);
							Resources.setPosition(resourceId, getUrl, pos);
						}
						//disable($('.' + groupId + '_ .delete_icon')[0]);
						enable(button);
					}
				});
			}
		});
	}

};