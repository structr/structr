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
/**
 * Library for all drag & drop actions in Structr
 */

var sorting = false;
var sortParent;

var _Dragndrop = {
	makeDroppable: function(element, previewId) {
		var el = $(element);
		var tag, iframe = previewId ? $('#preview_' + previewId) : undefined;

		el.droppable({
			iframeFix: true,
			iframe: iframe,
			accept: '.node, .element, .content, .image, .file, .widget, .data-binding-attribute, .data-binding-type',
			greedy: true,
			hoverClass: 'nodeHover',
			drop: function(e, ui) {

				_Logger.log(_LogType.DND, 'Drop event', e, ui);

				if (dropBlocked) {
					_Logger.log(_LogType.DND, 'Drop in iframe was blocked');
					dropBlocked = false;
					return false;
				}

				e.preventDefault();
				e.stopPropagation();

				var self = $(this), related;

				var sourceId = Structr.getId(ui.draggable) || Structr.getComponentId(ui.draggable);

				if (!sourceId) {
					// palette element dragged

					var d = $(ui.draggable);
					tag = d.text();
					if (d.attr('subkey')) {
						related = {};
						related.subKey = d.attr('subkey');
						related.isCollection = (d.attr('collection') === 'true');
					}
				}

				var targetId = Structr.getId(self);

				if (self.hasClass('jstree-wholerow')) {
					targetId = self.parent().prop('id');

					if (targetId === 'root') {
						Command.setProperty(sourceId, 'parent', null, false, function() {
							$(ui.draggable).remove();
							_Files.refreshTree();
							return true;
						});
						return;
					}
				}

				if (!targetId) {
					targetId = self.attr('data-structr-id');
				}

				_Logger.log(_LogType.DND, 'dropped onto', self, targetId, Structr.getId(sortParent));
				if (targetId === Structr.getId(sortParent)) {
					_Logger.log(_LogType.DND, 'target id == sortParent id', targetId, Structr.getId(sortParent));
					return false;
				}

				_Entities.ensureExpanded(self);
				sorting = false;
				sortParent = undefined;

				var source = StructrModel.obj(sourceId);
				var target = StructrModel.obj(targetId);

				var page = self.closest('.page')[0];

				if (!page) {
					page = self.closest('[data-structr-page]')[0];
				}
				var pageId = (page ? Structr.getId(page) : target.pageId);

				if (!pageId) {
					pageId = $(page).attr('data-structr-page');
				}

				if (!target) {
					// synthetize target with id only
					target = {id: targetId};
				}

				_Logger.log(_LogType.DND, source, target, pageId, tag, related);

				if (target.isContent && target.type !== 'Template' && source) {
					return false;
				}

				if (target.isContent && target.type !== 'Template' && isIn(tag, _Elements.voidAttrs)) {
					return false;
				}

				if (_Dragndrop.dropAction(source, target, pageId, tag, related)) {
					$(ui.draggable).remove();
					sortParent = undefined;
				}

			}
		});

	},
	makeSortable: function(element) {
		var el = $(element);

		var sortableOptions = {
			iframeFix: true,
			//sortable: '.node',
			appendTo: '#main',
			forceHelperSize: true,
			forcePlaceholderSize: true,
			distance: 5,
			helper: function (event, helperEl) {
				pages.append('<div id="collapse-offset"></div>');
				$('#collapse-offset', pages).css('height', helperEl.height() - 17);
				helperEl.css({height: '17px'});
				var hlp = helperEl.clone();
				hlp.find('.node').remove();
				hlp.find('.expand_icon').attr('src', _Icons.collapsed_icon);
				return hlp;
			},
			zIndex: 99,
			containment: 'body',
			start: function(event, ui) {
				sorting = true;
				sortParent = $(ui.item).parent();
				_Logger.log(_LogType.DND, '### sortable start: sorting?', sorting, Structr.getId(el), Structr.getId(self), Structr.getId(sortParent));
			},
			update: function(event, ui) {
				var el = $(ui.item);
				if (!sorting)
					return false;

				var id = Structr.getId(el);
				if (!id)
					id = Structr.getComponentId(el);

				var nextNode = el.next('.node');
				var refId = Structr.getId(nextNode);
				if (!refId) {
					refId = Structr.getComponentId(nextNode);
				}

				_Logger.log(_LogType.DND, '### sortable update: sorting?', sorting, Structr.getId(el), Structr.getId(self), Structr.getId(sortParent), nextNode, refId);

				var parentId = Structr.getId(sortParent);
				el.remove();
				Command.insertBefore(parentId, id, refId);
				sorting = false;
				sortParent = undefined;
				$('#collapse-offset', pages).remove();
			},
			stop: function(event, ui) {
				sorting = false;
				_Entities.resetMouseOverState(ui.item);
				$(event.toElement).one('click', function(e) {
					e.stopImmediatePropagation();
				});
				$(ui.item).css({height: ''});
				$('#collapse-offset', pages).remove();
			}
		};

		el.sortable(sortableOptions).disableSelection();

	},
	/**
	 * Define what happens when a source object is dropped onto
	 * a target object in the given page.
	 *
	 * The optional tag is used to create new elements if the source object
	 * is undefined. This is the case if an element was dragged from the
	 * HTML elements palette.
	 */
	dropAction: function(source, target, pageId, tag, related) {

		_Logger.log(_LogType.DND, 'dropAction', source, target, pageId, tag, related);

		if (source && pageId && source.pageId && pageId !== source.pageId) {

			if (shadowPage && source.pageId === shadowPage.id) {
				Command.cloneComponent(source.id, target.id);
				_Logger.log(_LogType.DND, 'dropped', source.id, 'onto', target.id, 'in page', pageId, ', cloneComponent');

			} else if (source.pageId !== target.pageId) {
				Command.cloneNode(source.id, target.id, true);
				_Logger.log(_LogType.DND, 'dropped', source.id, 'onto', target.id, 'in page', pageId, ', cloneNode');

			} else {
				Command.appendChild(source.id, target.id, pageId);
				_Logger.log(_LogType.DND, 'dropped', source.id, 'onto', target.id, 'in page', pageId, ', appendChild');

			}

			return false;
		}

		// element dropped on itself?
		if (source && target && (source.id === target.id)) {
			_Logger.log(_LogType.DND, 'drop on self not allowed');
			return false;
		}

		if (source && source.isWidget) {
			return _Dragndrop.widgetDropped(source, target, pageId);
		}

		if (source && source.isImage) {
			return _Dragndrop.imageDropped(source, target, pageId);
		}

		if (source && (source.isFile || source.isFolder)) {
			return _Dragndrop.fileDropped(source, target, pageId);
		}

		if (!source && tag) {

			if (tag.indexOf('.') !== -1) {

				_Logger.log(_LogType.DND, tag, source, target, related);

				Command.get(target.id, function(target) {
					var firstContentId = target.children[0].id;
					if (related) {
						var key = tag.substring(tag.indexOf('.') + 1);
						_Logger.log(_LogType.DND, 'tag, key, subkey', tag, key, related.subKey);
						if (related.isCollection) {
							Command.setProperty(firstContentId, 'content', '${' + key + '.' + related.subKey + '}');
							Command.setProperty(firstContentId, 'dataKey', key);
							$('#dataKey_').val(key);
						} else {
							Command.setProperty(firstContentId, 'content', '${' + tag + '.' + related.subKey + '}');
							Command.setProperty(firstContentId, 'dataKey', null);
							$('#dataKey_').val('');
						}
					} else {
						Command.setProperty(firstContentId, 'content', '${' + tag + '}');
						Command.setProperty(firstContentId, 'dataKey', null);
						$('#dataKey_').val('');
					}
				});

			} else if (tag.indexOf(':') !== -1) {
				var type = tag.substring(1);
				Command.setProperty(target.id, 'restQuery', type);
				Command.setProperty(target.id, 'dataKey', type.toLowerCase(), false, function() {
					_Pages.reloadPreviews();
				});
			} else {
				return _Dragndrop.htmlElementFromPaletteDropped(tag, target, pageId);
			}

		} else {

			tag = target.tag;

			if (source && target && source.id && target.id) {

				sorting = false;
				_Logger.log(_LogType.DND, 'appendChild', source, target);
				Command.appendChild(source.id, target.id);

				return true;

			} else {
				_Logger.log(_LogType.DND, 'unknown drag\'n drop  situation', source, target);
			}
		}

		_Logger.log(_LogType.DND, 'drop event in appendElementElement', pageId, Structr.getId(self), (tag !== 'content' ? tag : ''));

	},
	htmlElementFromPaletteDropped: function(tag, target, pageId) {
		var nodeData = {};
		if (tag === 'a' || tag === 'p'
				|| tag === 'h1' || tag === 'h2' || tag === 'h3' || tag === 'h4' || tag === 'h5' || tag === 'h5' || tag === 'pre' || tag === 'label' || tag === 'option'
				|| tag === 'li' || tag === 'em' || tag === 'title' || tag === 'b' || tag === 'span' || tag === 'th' || tag === 'td' || tag === 'button' || tag === 'figcaption') {
			if (tag === 'a') {
				nodeData._html_href = '${link.name}';
				nodeData.childContent = '${parent.link.name}';
			} else if (tag === 'title') {
				nodeData.childContent = '${page.name}';
			} else {
				nodeData.childContent = 'Initial text for ' + tag;
			}
		}
		if (target.type !== 'Template' && (target.isContent || target.type === 'Comment')) {
			if (tag === 'content' || tag === 'comment') {
				_Logger.log(_LogType.DND, 'content element dropped on content or comment, doing nothing');
			} else {
				_Logger.log(_LogType.DND, 'wrap content', pageId, target.id, tag);
				Command.wrapContent(pageId, target.id, tag);
			}
		} else {
			Command.createAndAppendDOMNode(pageId, target.id, tag !== 'content' ? tag : '', nodeData);
		}
		return false;
	},
	widgetDropped: function(source, target, pageId) {

		Structr.makePagesMenuDroppable();

		var pattern = /\[[a-zA-Z:,]+\]/g;
		var widgetSource = source.source;
		if (widgetSource) {

			var rawMatches = widgetSource.match(pattern);

			if (rawMatches) {

				var matches = $.unique(rawMatches);

				if (matches && matches.length) {

					Structr.dialog('Configure Widget', function() { }, function() { });

					dialogText.append('<p>Fill out the following parameters to correctly configure the widget.</p><table class="props"></table>');
					var table = $('table', dialogText);

					$.each(matches, function(i, match) {

						var propertyKey = match.replace(/\[/, '').replace(/\]/, '');
						var options = '';
						var hasOptions = false;

						if (propertyKey.contains(":")) {

							var parts = propertyKey.split(":");

							if (parts.length === 2) {
								propertyKey = parts[0];
								options = parts[1];
								hasOptions = true;
							}
						}

						var label = _Crud.formatKey(propertyKey);

						if (hasOptions) {

							var buffer = '';

							buffer += '<tr><td><label for="' + label + '">' + label + '</label></td><td>';
							buffer += '<select id="' + propertyKey + '" class="input-field" >';

							$.each(options.split(","), function(i, option) {
								buffer += '<option>' + option + '</option>';
							});

							buffer += '</select>';
							buffer += '</td></tr>';

							table.append(buffer);

						} else {

							table.append('<tr><td><label for="' + label + '">' + label + '</label></td><td><input class="input-field" type="text" id="' + propertyKey + '" placeholder="' + label + '"></td></tr>');
						}

					});

					dialog.append('<button id="appendWidget">Append Widget</button>');
					var attrs = {};
					$('#appendWidget').on('click', function(e) {

						$.each(matches, function(i, match) {

							$.each($('.input-field', table), function(i, m) {
								var key = $(m).prop('id').replace(/\[/, '').replace(/\]/, '');
								attrs[key] = $(this).val();
							});

						});

						e.stopPropagation();
						Command.appendWidget(widgetSource, target.id, pageId, widgetsUrl, attrs);

						dialogCancelButton.click();
						return false;
					});

				}

			} else {

				// If no matches, directly append widget
				Command.appendWidget(widgetSource, target.id, pageId, widgetsUrl);

			}

		}

	},
	fileDropped: function(source, target, pageId) {
		var refreshTimeout;
		if (!pageId) {

			if (source.id === target.id) {
				return false;
			}

			if (selectedElements.length > 1) {

				$.each(selectedElements, function(i, fileEl) {
					var fileId = Structr.getId(fileEl);

					if (fileId === target.id) {
						return false;
					}

					Command.appendFile(fileId, target.id, function() {
						if (refreshTimeout) {
							window.clearTimeout(refreshTimeout);
						}
						refreshTimeout = window.setTimeout(function() {
							_Files.refreshTree();
							refreshTimeout = 0;
						}, 100);

					});
				});
				selectedElements.length = 0;

			} else {
				Command.appendFile(source.id, target.id, function() {
					_Files.refreshTree();
				});
			}

			return true;
		}

		var nodeData = {}, tag;

		//name = $(ui.draggable).children('.name_').attr('title');
		name = source.name;

		var parentTag = target.tag;

		//var parentTag = self.children('.tag_').text();
		_Logger.log(_LogType.DND, source, target, parentTag);
		nodeData.linkableId = source.id;

		if (parentTag === 'head') {

			_Logger.log(_LogType.DND, 'File dropped in <head>');

			if (name.endsWith('.css')) {

				//console.log('CSS file dropped in <head>, creating <link>');

				tag = 'link';
				nodeData._html_href = '${link.path}?${link.version}';
				nodeData._html_type = 'text/css';
				nodeData._html_rel = 'stylesheet';
				nodeData._html_media = 'screen';

			} else if (name.endsWith('.js')) {

				_Logger.log(_LogType.DND, 'JS file dropped in <head>, creating <script>');

				tag = 'script';
				nodeData._html_src = '${link.path}?${link.version}';
			}

		} else {

			_Logger.log(_LogType.DND, 'File dropped, creating <a> node', name);
			nodeData._html_href = '${link.path}';
			nodeData._html_title = '${link.name}';
			nodeData.childContent = '${parent.link.name}';
			tag = 'a';
		}
		source.id = undefined;

		Structr.modules['files'].unload();
		Command.createAndAppendDOMNode(pageId, target.id, tag, nodeData);

		return true;
	},
	imageDropped: function(source, target, pageId) {

		var nodeData = {}, name = source.name, tag;
		_Logger.log(_LogType.DND, 'Image dropped, creating <img> node', name);
		nodeData._html_src = '${link.path}?${link.version}';
		nodeData.linkableId = source.id;
		//nodeData.name = '${link.name}';
		tag = 'img';

		Structr.modules['images'].unload();
		Command.createAndAppendDOMNode(pageId, target.id, tag, nodeData);

		return true;
	}
};