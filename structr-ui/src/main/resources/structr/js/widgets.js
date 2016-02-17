/*
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
var widgets, remoteWidgets, widgetsUrl = 'https://widgets.structr.org/structr/rest/widgets';
var win = $(window);
var widgetData = [], remoteWidgetData = [], remoteWidgetFilter;

$(document).ready(function() {
	Structr.registerModule('widgets', _Widgets);
	Structr.classes.push('widget');
});

var _Widgets = {
	icon: 'icon/layout.png',
	group_icon: 'icon/folder.png',
	add_widget_icon: 'icon/layout_add.png',
	delete_widget_icon: 'icon/layout_delete.png',
	init: function() {

		_Pager.initPager('Widget', 1, 25);

	},
	onload: function() {

		_Widgets.init();

		$('#main-help a').attr('href', 'http://docs.structr.org/frontend-user-guide#Widgets');

		_Logger.log(_LogType.WIDGETS, 'onload');

		main.append('<div id="dropArea"><div class="fit-to-height" id="widgets"><h2>Local Widgets</h2><div id="widgets-content"></div></div><div class="fit-to-height" id="remoteWidgets"><h2>Remote Widgets</h2><input placeholder="Filter..." id="remoteWidgetsFilter"><div id="remoteWidgets-content"></div></div></div>');
		widgets = $('#widgets-content');
		remoteWidgets = $('#remoteWidgets-content');

		$('#remoteWidgetsFilter').keyup(function (event) {

			var e = event || window.event();

			if (e.keyCode === 27) {
				$(this).val("");
			}

			_Widgets.repaintRemoteWidgets($(this).val());

		});

		_Widgets.refreshWidgets();
		_Widgets.refreshRemoteWidgets();

		Structr.resize();

		win.off('resize');
		win.resize(function() {
			Structr.resize();
		});

		Structr.unblockMenu(100);

	},
	unload: function() {
		$(main.children('table')).remove();
	},
	refreshWidgets: function() {
		widgets.empty();
		widgets.append('<button class="add_widgets_icon button"><img title="Add Widget" alt="Add Widget" src="' + _Widgets.add_widget_icon + '"> Add Widget</button>');
		$('.add_widgets_icon', main).on('click', function(e) {
			e.stopPropagation();
			Command.create({'type': 'Widget'});
		});
		var wPager = _Pager.addPager(widgets, true, 'Widget');
		wPager.cleanupFunction = function() {
			$('.node', wPager.el).remove();
		};
		Structr.resize();
	},
	refreshRemoteWidgets: function() {
		remoteWidgetFilter = undefined;

		if (widgetsUrl.startsWith(document.location.hostname)) {
			return;
		}

		_Widgets.getRemoteWidgets(widgetsUrl, function(entity) {
			var obj = StructrModel.create(entity, null, false);
			obj.srcUrl = widgetsUrl + '/' + entity.id;
			remoteWidgetData.push(obj);
		}, function () {
			_Widgets.repaintRemoteWidgets('');
		});

//		remoteWidgets.append('<input id="widgetServerUrl" type="text" size="40" placeholder="Remote URL" value="http://server2.morgner.de:8084/structr/rest/widgets"><button id="connect_button">Connect</button>');
//		$('#connect_button', main).on('click', function(e) {
//			e.stopPropagation();
//		});
	},
	repaintRemoteWidgets: function (search) {

		if (search !== remoteWidgetFilter) {

			remoteWidgetFilter = search;

			if (search && search.length > 0) {

				search = search.toLowerCase();

				remoteWidgets.empty();

				remoteWidgetData.forEach(function (obj) {

					if (obj.name.toLowerCase().indexOf(search) !== -1) {
						_Widgets.appendWidgetElement(obj, true, remoteWidgets);
					}

				});

			} else {

				remoteWidgets.empty();

				remoteWidgetData.forEach(function (obj) {
					_Widgets.appendWidgetElement(obj, true, remoteWidgets);
				});

			}

		}

	},
	getRemoteWidgets: function(baseUrl, callback, finishCallback) {
		$.ajax({
			//url: $('#widgetServerUrl').val(),
			url: baseUrl + '?sort=treePath',
			type: 'GET',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			//async: false,
			statusCode: {
				200: function(data) {
					if (callback) {
						$.each(data.result, function(i, entity) {
							callback(entity);
						});
						if (finishCallback) {
							finishCallback();
						}
					}
				},
				400: function(data, status, xhr) {
					console.log(data, status, xhr);
				},
				401: function(data, status, xhr) {
					console.log(data, status, xhr);
				},
				403: function(data, status, xhr) {
					console.log(data, status, xhr);
				},
				404: function(data, status, xhr) {
					console.log(data, status, xhr);
				},
				422: function(data, status, xhr) {
					console.log(data, status, xhr);
				},
				500: function(data, status, xhr) {
					console.log(data, status, xhr);
				}
			}

		});

	},
	getIcon: function() {
		var icon = _Widgets.icon; // default
		return icon;
	},
	getTreeParent: function(element, treePath, suffix) {

		var parent = element;

		if (treePath) {

			var parts = treePath.split('/');
			var num = parts.length;
			var i = 0;

			for (i = 0; i < num; i++) {

				var part = parts[i];
				if (part) {

					var lowerPart = part.toLowerCase().replace(/ /g, '');
					var idString = lowerPart + suffix;
					var newParent = $('#' + idString);
					if (newParent.size() === 0) {

						_Widgets.appendFolderElement(parent, idString, _Widgets.group_icon, part);
						// parent.append('<div id="' + idString + '_node" class="node widget"><b>' + part + '</b><div id="' + idString + '" style="padding-left: 2em;"></div></div>');
						newParent = $('#' + idString);
					}

					parent = newParent;
				}
			}

		} else {

			var idString = 'other' + suffix;
			var newParent = $('#' + idString);
			if (newParent.size() === 0) {

				_Widgets.appendFolderElement(parent, idString, _Widgets.group_icon, 'Uncategorized');
				//parent.append('<div id="' + idString + '_node" class="node widget"><b>Uncategorized</b><div id="' + idString + '" style="padding-left: 2em;"></div></div>');
				newParent = $('#' + idString);
			}

			parent = newParent;
		}

		return parent;
	},
	appendFolderElement: function(parent, id, icon, name) {

		var expanded = isExpanded(id);

		parent.append('<div id="' + id + '_folder" class="widget node">'
				+ '<img class="typeIcon" src="' + icon + '">'
				+ '<b title="' + name + '" class="name">' + fitStringToWidth(name, 200) + '</b> <span class="id">' + id + '</span>'
				+ '<div id="' + id + '" class="node' + (expanded ? ' hidden' : '') + '"></div>'
				+ '</div>');

		var div = $('#' + id + '_folder');

		_Widgets.appendVisualExpandIcon(div, id, name, true, false);
	},
	appendWidgetElement: function(widget, remote, el) {

		_Logger.log(_LogType.WIDGETS, 'Widgets.appendWidgetElement', widget, remote);

		var icon = _Widgets.getIcon(widget);
		var parent = _Widgets.getTreeParent(el ? el : (remote ? remoteWidgets : widgets), widget.treePath, remote ? '_remote' : '_local');
		var delIcon, newDelIcon;
		var div = Structr.node(widget.id);
		if (div && div.length) {

			var formerParent = div.parent();

			if (!Structr.containsNodes(formerParent)) {
				_Entities.removeExpandIcon(formerParent);
				enable($('.delete_icon', formerParent)[0]);
			}

		} else {

			parent.append('<div id="id_' + widget.id + '" class="node widget">'
					+ '<img class="typeIcon" src="' + icon + '">'
					+ '<b title="' + widget.name + '" class="name_">' + fitStringToWidth(widget.name, 200) + '</b> <span class="id">' + widget.id + '</span>'
					+ '</div>');
			div = Structr.node(widget.id);

			var typeIcon = $('.typeIcon', div);
			typeIcon.on('click', function() {
				Structr.dialog('Source code of ' + widget.name, function() {}, function() {});
				var text = widget.source || '';
				var div = dialogText.append('<div class="editor"></div>');
				_Logger.log(_LogType.WIDGETS, div);
				var contentBox = $('.editor', dialogText);
				editor = CodeMirror(contentBox.get(0), {
					value: unescapeTags(text),
					mode: 'text/html',
					lineNumbers: true
				});
				editor.focus();
				Structr.resize();
			});

		}

		if (!div)
			return;

		if (!remote) {
			_Entities.appendAccessControlIcon(div, widget);

			delIcon = div.children('.delete_icon');

			newDelIcon = '<img title="Delete widget ' + widget.name + '\'" alt="Delete widget \'' + widget.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">';
			div.append(newDelIcon);
			delIcon = div.children('.delete_icon');
			div.children('.delete_icon').on('click', function(e) {
				e.stopPropagation();
				_Entities.deleteNode(this, widget);
			});

		}

		div.draggable({
			iframeFix: true,
			revert: 'invalid',
			containment: 'body',
			helper: 'clone',
			appendTo: '#main',
			stack: '.node',
			zIndex: 99,
			stop: function(e, ui) {
				//$('#pages_').droppable('enable').removeClass('nodeHover');
			}
		});

		if (!remote) {
			div.append('<img title="Edit widget" alt="Edit widget ' + widget.id + '" class="edit_icon button" src="icon/pencil.png">');
			$('.edit_icon', div).on('click', function(e) {
				e.stopPropagation();
				Structr.dialog('Edit widget "' + widget.name + '"', function() {
					_Logger.log(_LogType.WIDGETS, 'Widget source saved');
				}, function() {
					_Logger.log(_LogType.WIDGETS, 'cancelled');
				});
				if (!widget.id) {
					return false;
				}
				Command.get(widget.id, function(entity) {
					_Widgets.editWidget(this, entity, dialogText);
				});

			});
			_Entities.appendEditPropertiesIcon(div, widget);
		}

		_Entities.setMouseOver(div, false);
		if (remote) {
			div.children('b.name_').off('click').css({cursor: 'move'});
		}

//        div.append('<div class="preview"></div>');
//        //$('.preview', div).contents().find('body').html('<html><head><title>' +  widget.name + '</title></head><body>' + widget.source + '</body></html>');
//        widget.pictures.forEach(function(pic) {
//            $('.preview', div).append('<img src="/' + pic.id + '">');
//        });

		return div;
	},
	editWidget: function(button, entity, element) {
		if (isDisabled(button))
			return;
		var text = entity.source || '';
		var div = element.append('<div class="editor"></div>');
		_Logger.log(_LogType.WIDGETS, div);
		var contentBox = $('.editor', element);
		editor = CodeMirror(contentBox.get(0), {
			value: unescapeTags(text),
			mode: 'text/html',
			lineNumbers: true
		});
		editor.focus();
		Structr.resize();

		dialogBtn.append('<button id="editorSave" disabled="disabled" class="disabled">Save Widget</button>');
		dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');

		dialogSaveButton = $('#editorSave', dialogBtn);
		var saveAndClose = $('#saveAndClose', dialogBtn);

		text1 = text;

		editor.on('change', function(cm, change) {

			text2 = editor.getValue();

			if (text1 === text2) {
				dialogSaveButton.prop("disabled", true).addClass('disabled');
				saveAndClose.prop("disabled", true).addClass('disabled');
			} else {
				dialogSaveButton.prop("disabled", false).removeClass('disabled');
				saveAndClose.prop("disabled", false).removeClass('disabled');
			}
		});

		saveAndClose.on('click', function(e) {
			e.stopPropagation();
			dialogSaveButton.click();
			setTimeout(function() {
				dialogSaveButton.remove();
				saveAndClose.remove();
				dialogCancelButton.click();
			}, 500);
		});

		dialogSaveButton.on('click', function() {

			var newText = editor.getValue();

			if (text1 === newText) {
				return;
			}

			if (entity.srcUrl) {
				var data = JSON.stringify({'source': newText});
				_Logger.log(_LogType.WIDGETS, 'update remote widget', entity.srcUrl, data);
				$.ajax({
					//url: $('#widgetServerUrl').val(),
					url: entity.srcUrl,
					type: 'PUT',
					dataType: 'json',
					data: data,
					contentType: 'application/json; charset=utf-8',
					//async: false,
					statusCode: {
						200: function(data) {
							dialogMsg.html('<div class="infoBox success">Widget source saved.</div>');
							$('.infoBox', dialogMsg).delay(2000).fadeOut(200);
							text1 = newText;
							dialogSaveButton.prop("disabled", true).addClass('disabled');
							saveAndClose.prop("disabled", true).addClass('disabled');

						},
						400: function(data, status, xhr) {
							console.log(data, status, xhr);
						},
						401: function(data, status, xhr) {
							console.log(data, status, xhr);
						},
						403: function(data, status, xhr) {
							console.log(data, status, xhr);
						},
						404: function(data, status, xhr) {
							console.log(data, status, xhr);
						},
						422: function(data, status, xhr) {
							console.log(data, status, xhr);
						},
						500: function(data, status, xhr) {
							console.log(data, status, xhr);
						}
					}

				});

			} else {

				Command.setProperty(entity.id, 'source', newText, false, function() {
					dialogMsg.html('<div class="infoBox success">Widget saved.</div>');
					$('.infoBox', dialogMsg).delay(2000).fadeOut(200);
					text1 = newText;
					dialogSaveButton.prop("disabled", true).addClass('disabled');
					saveAndClose.prop("disabled", true).addClass('disabled');
				});

			}

		});

		editor.id = entity.id;
	},
	appendVisualExpandIcon: function(el, id, name, hasChildren, expand) {

		if (hasChildren) {

			_Logger.log(_LogType.WIDGETS, 'appendExpandIcon hasChildren?', hasChildren, 'expand?', expand);

			var typeIcon = $(el.children('.typeIcon').first());
			var icon = $(el).children('.node').hasClass('hidden') ? Structr.expand_icon : Structr.expanded_icon;

			typeIcon.css({
				paddingRight: 0 + 'px'
			}).after('<img title="Expand \'' + name + '\'" alt="Expand \'' + name + '\'" class="expand_icon" src="' + icon + '">');

			var expandIcon = el.children('.expand_icon').first();

			$(el).on('click', function(e) {
				e.stopPropagation();

				var body = $('#' + id);
				body.toggleClass('hidden');
				var expanded = body.hasClass('hidden');
				if (expanded) {
					addExpandedNode(id);
					expandIcon.prop('src', 'icon/tree_arrow_right.png');

				} else {
					removeExpandedNode(id);
					expandIcon.prop('src', 'icon/tree_arrow_down.png');
				}
			});

			button = $(el.children('.expand_icon').first());

			if (button) {

				button.on('click', function(e) {
					e.stopPropagation();

					var body = $('#' + id);
					body.toggleClass('hidden');
					var expanded = body.hasClass('hidden');
					if (expanded) {
						addExpandedNode(id);
						expandIcon.prop('src', 'icon/tree_arrow_right.png');
					} else {
						removeExpandedNode(id);
						expandIcon.prop('src', 'icon/tree_arrow_down.png');
					}
				});

				// Prevent expand icon from being draggable
				button.on('mousedown', function(e) {
					e.stopPropagation();

				});

				if (expand) {
				}
			}

		} else {
			el.children('.typeIcon').css({
				paddingRight: 11 + 'px'
			});
		}


	}
};
