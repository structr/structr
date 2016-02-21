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
var elements, dropBlocked;

var _Elements = {
	icon: 'icon/brick.png',
	icon_comp: 'icon/brick_yellow.png',
	icon_shared_template: 'icon/layout_yellow.png',
	icon_repeater: 'icon/bricks.png',
	add_icon: 'icon/brick_add.png',
	delete_icon: 'icon/brick_delete.png',
	elementNames: [
		// The root element
		'html',
		// Document metadata
		'head', 'title', 'base', 'link', 'meta', 'style',
		// Scripting
		'script', 'noscript',
		// Sections
		'body', 'section', 'nav', 'article', 'aside', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hgroup', 'header', 'footer', 'address',
		// Grouping content
		'p', 'hr', 'pre', 'blockquote', 'ol', 'ul', 'li', 'dl', 'dt', 'dd', 'figure', 'figcaption', 'div',
		// Text-level semantics
		'a', 'em', 'strong', 'small', 's', 'cite', 'g', 'dfn', 'abbr', 'time', 'code', 'var', 'samp', 'kbd', 'sub', 'sup',
		'i', 'b', 'u', 'mark', 'ruby', 'rt', 'rp', 'bdi', 'bdo', 'span', 'br', 'wbr',
		// Edits
		'ins', 'del',
		// Embedded content
		'img', 'iframe', 'embed', 'object', 'param', 'video', 'audio', 'source', 'track', 'canvas', 'map', 'area',
		// Tabular data
		'table', 'caption', 'colgroup', 'col', 'tbody', 'thead', 'tfoot', 'tr', 'td', 'th',
		// Forms
		'form', 'fieldset', 'legend', 'label', 'input', 'button', 'select', 'datalist', 'optgroup', 'option', 'textarea', 'keygen', 'output',
		'progress', 'meter',
		// Interactive elements
		'details', 'summary', 'command', 'menu'
	],
	elementGroups: [
		{
			'name': 'Root',
			'elements': ['html', 'content', 'comment', 'template']
		},
		{
			'name': 'Metadata',
			'elements': ['head', 'title', 'base', 'link', 'meta', 'style']
		},
		{
			'name': 'Sections',
			'elements': ['body', 'section', 'nav', 'article', 'aside', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hgroup', 'header', 'footer', 'address']
		},
		{
			'name': 'Grouping',
			'elements': ['div', 'p', 'hr', 'ol', 'ul', 'li', 'dl', 'dt', 'dd', 'pre', 'blockquote', 'figure', 'figcaption']
		},
		{
			'name': 'Scripting',
			'elements': ['script', 'noscript']
		},
		{
			'name': 'Tabular',
			'elements': ['table', 'tr', 'td', 'th', 'caption', 'colgroup', 'col', 'tbody', 'thead', 'tfoot']
		},
		{
			'name': 'Text',
			'elements': ['a', 'em', 'strong', 'small', 's', 'cite', 'g', 'dfn', 'abbr', 'time', 'code', 'var', 'samp', 'kbd', 'sub', 'sup', 'i', 'b', 'u', 'mark', 'ruby', 'rt', 'rp', 'bdi', 'bdo', 'span', 'br', 'wbr']
		},
		{
			'name': 'Edits',
			'elements': ['ins', 'del']
		},
		{
			'name': 'Embedded',
			'elements': ['img', 'video', 'audio', 'source', 'track', 'canvas', 'map', 'area', 'iframe', 'embed', 'object', 'param']
		},
		{
			'name': 'Forms',
			'elements': ['form', 'input', 'button', 'select', 'datalist', 'optgroup', 'option', 'textarea', 'fieldset', 'legend', 'label', 'keygen', 'output', 'progress', 'meter']
		},
		{
			'name': 'Interactive',
			'elements': ['details', 'summary', 'command', 'menu']
		}
	],
	mostUsedAttrs: [
		{
			'elements': ['input'],
			'attrs': ['name', 'type', 'checked', 'selected', 'value', 'size', 'multiple', 'disabled', 'autofocus', 'placeholder'],
			'focus': 'type'
		},
		{
			'elements': ['button'],
			'attrs': ['name', 'type', 'checked', 'selected', 'value', 'size', 'multiple', 'disabled', 'autofocus', 'placeholder', 'onclick']
		},
		{
			'elements': ['select', 'option'],
			'attrs': ['name', 'type', 'checked', 'selected', 'value', 'size', 'multiple', 'disabled', 'autofocus', 'placeholder']
		},
		{
			'elements': ['optgroup'],
			'attrs': ['label', 'disabled'],
			'focus': 'label'
		},
		{
			'elements': ['form'],
			'attrs': ['action', 'method']
		},
		{
			'elements': ['img'],
			'attrs': ['alt', 'title', 'src'],
			'focus': 'src'
		},
		{
			'elements': ['script', 'img', 'object'],
			'attrs': ['type', 'rel', 'href', 'media', 'src'],
			'focus': 'src'
		},
		{
			'elements': ['link'],
			'attrs': ['type', 'rel', 'href'],
			'focus': 'href'
		},
		{
			'elements': ['a'],
			'attrs': ['type', 'rel', 'href', 'target'],
			'focus': 'href'
		},
		{
			'elements': ['td', 'th'],
			'attrs': ['colspan', 'rowspan']
		},
		{
			'elements': ['label'],
			'attrs': ['for', 'form'],
			'focus' : 'for'
		},
		{
			'elements': ['style'],
			'attrs': ['type', 'media', 'scoped'],
			'focus' : 'type'
		},
		{
			'elements': ['iframe'],
			'attrs': ['src', 'width', 'height'],
			'focus' : 'src'
		},
		{
			'elements': ['source'],
			'attrs': ['src', 'type', 'media'],
			'focus' : 'src'
		},
		{
			'elements': ['video'],
			'attrs': ['autoplay', 'controls', 'height', 'loop', 'muted', 'poster', 'preload', 'src', 'width'],
			'focus' : 'controls'
		},
		{
			'elements': ['audio'],
			'attrs': ['autoplay', 'controls', 'loop', 'muted', 'preload', 'src'],
			'focus' : 'controls'
		}
	],
	voidAttrs: ['br', 'hr', 'img', 'input', 'link', 'meta', 'area', 'base', 'col', 'embed', 'keygen', 'menuitem', 'param', 'track', 'wbr'],
	favoriteChildElements: {
		'script': ['content'],
		'button': ['content', 'span', 'img', 'i'],
		'table': ['thead', 'tbody', 'tfoot'],
		'title': ['content'],
		'thead': ['tr'],
		'tbody': ['tr'],
		'tfoot': ['tr'],
		'html': ['head', 'body'],
		'head': ['link', 'meta', 'script', 'title'],
		'body': ['div', 'script'],
		'span': ['content', 'img', 'span', 'i'],
		'div': ['div', 'content', 'span', 'button', 'img', 'table', 'form'],
		'tr': ['th', 'td'],
		'th': ['div', 'p', 'span', 'a', 'content'],
		'td': ['div', 'p', 'span', 'a', 'content'],
		'ul': ['li'],
		'ol': ['li'],
		'li': ['content', 'a', 'div', 'span', 'button'],
		'h1': ['content', 'span'],
		'h2': ['content', 'span'],
		'h3': ['content', 'span'],
		'h4': ['content', 'span'],
		'h5': ['content', 'span'],
		'h6': ['content', 'span'],
		'p': ['content', 'a', 'img', 'button', 'span'],
		'a': ['content', 'img', 'span'],
		'i': ['content']
	},
	/**
	 * Reload widgets
	 */
	reloadWidgets: function() {

		widgetsSlideout.find(':not(.compTab)').remove();

		widgetsSlideout.append('<div class="ver-scrollable"><h2>Local Widgets</h2><div id="widgets"></div><h2>Remote Widgets</h2><input placeholder="Filter..." id="remoteWidgetsFilter"><div id="remoteWidgets"></div></div>');
		widgets = $('#widgets', widgetsSlideout);


		widgets.droppable({
			drop: function(e, ui) {
				e.preventDefault();
				e.stopPropagation();
				dropBlocked = true;
				var sourceEl = $(ui.draggable);
				if (sourceEl.parent().attr('id') === 'widgets') {
					_Logger.log(_LogType.ELEMENTS, 'widget dropped on widget area, aborting');
					return false;
				}
				var sourceId = Structr.getId(sourceEl);

				$.ajax({
					url: viewRootUrl + sourceId + '?edit=1',
					contentType: 'text/html',
					statusCode: {
						200: function(data) {
							Command.createLocalWidget(sourceId, 'New Widget (' + sourceId + ')', data, function(entity) {

								//_Widgets.appendWidgetElement(entity, false, localWidgetsArea);

							});
						}
					}
				});
			}

		});

		_Pager.initPager('Widget', 1, 25);
		_Pager.addPager(widgets, true, 'Widget', function(entities) {
			entities.forEach(function (entity) {
				StructrModel.create(entity, null, false);
				_Widgets.appendWidgetElement(entity, false, widgets);
			});
		});

		remoteWidgets = $('#remoteWidgets', widgetsSlideout);

		$('#remoteWidgetsFilter').keyup(function (event) {

			var e = event || window.event();

			if (e.keyCode === 27) {
				$(this).val("");
			}

			_Widgets.repaintRemoteWidgets($(this).val());

		});

		_Widgets.refreshRemoteWidgets();

	},
	/**
	 * Reload HTML palette
	 *
	 * @returns {undefined}
	 */
	reloadPalette: function() {

		paletteSlideout.find(':not(.compTab)').remove();
		paletteSlideout.append('<div class="ver-scrollable" id="paletteArea"></div>')
		palette = $('#paletteArea', paletteSlideout);

		palette.droppable({
			drop: function(e, ui) {
				e.preventDefault();
				e.stopPropagation();
				return false;
			}
		});

		if (!$('.draggable', palette).length) {

			$(_Elements.elementGroups).each(function(i, group) {
				_Logger.log(_LogType.ELEMENTS, group);
				palette.append('<div class="elementGroup" id="group_' + group.name + '"><h3>' + group.name + '</h3></div>');
				$(group.elements).each(function(j, elem) {
					var div = $('#group_' + group.name);
					div.append('<div class="draggable element" id="add_' + elem + '">' + elem + '</div>');
					$('#add_' + elem, div).draggable({
						iframeFix: true,
						revert: 'invalid',
						containment: 'body',
						helper: 'clone',
						appendTo: '#main',
						stack: '.node',
						zIndex: 99
					});
				});
			});

		}
	},
	/**
	 * Reload components in component area
	 */
	reloadComponents: function() {

		if (!componentsSlideout) return;
		componentsSlideout.find(':not(.compTab)').remove();
		componentsSlideout.append('<div class="ver-scrollable" id="componentsArea"></div>')
		components = $('#componentsArea', componentsSlideout);

		Command.getByType('ShadowDocument', 1, 1, null, null, null, true, function(entities) {
			shadowPage = entities[0];
		});
		components.droppable({
			drop: function(e, ui) {
				e.preventDefault();
				e.stopPropagation();
				dropBlocked = true;
				var sourceEl = $(ui.draggable);
				var sourceId = Structr.getId(sourceEl);
				if (!sourceId) return false;
				var obj = StructrModel.obj(sourceId);
				if (obj && obj.syncedNodes && obj.syncedNodes.length || sourceEl.parent().attr('id') === 'componentsArea') {
					_Logger.log(_LogType.ELEMENTS, 'component dropped on components area, aborting');
					return false;
				}
				Command.createComponent(sourceId);
				dropBlocked = false;
			}

		});
		_Dragndrop.makeSortable(components);

		Command.listComponents(1000, 1, 'name', 'asc', function(result) {

			result.forEach(function(entity) {

				if (!entity) {
					return false;
				}

				var obj = StructrModel.create(entity, null, false);
				var el;
				if (obj.isContent || obj.type === 'Template') {
					el = _Contents.appendContentElement(obj, components, true);
				} else {
					el = _Pages.appendElementElement(obj, components, true);
				}

				if (isExpanded(entity.id)) {
					_Entities.ensureExpanded(el);
				}

			});

		});

	},
	/**
	 * Reload unattached nodes in elements area
	 */
	reloadUnattachedNodes: function() {

		elementsSlideout.find(':not(.compTab)').remove();
		elementsSlideout.append('<div class="ver-scrollable" id="elementsArea"></div>')
		elements = $('#elementsArea', elementsSlideout);

		elements.append('<button class="btn" id="delete-all-unattached-nodes">Delete all</button>');

		var btn = $('#delete-all-unattached-nodes')
		btn.on('click', function() {
			Structr.confirmation('<p>Delete all DOM elements without parent?</p>',
					function() {
						Command.deleteUnattachedNodes();
						$.unblockUI({
							fadeOut: 25
						});
						Structr.closeSlideOuts([elementsSlideout]);
					});
		});

		_Dragndrop.makeSortable(elements);
		Command.listUnattachedNodes(1000, 1, 'name', 'asc', function(result) {

			result.forEach(function(entity) {

				if (!entity) {
					return;
				}

				var obj = StructrModel.create(entity, null, false);
				var el;
				if (obj.isContent) {
					el = _Contents.appendContentElement(obj, elements, true);
				} else {
					el = _Pages.appendElementElement(obj, elements, true);
				}

				if (isExpanded(entity.id)) {
					_Entities.ensureExpanded(el);
				}
			});

		});

	},
	componentNode: function(id) {
		return $($('#componentId_' + id)[0]);
	},
	/**
	 * Create a DOM node and append to the appropriate parent
	 */
	appendElementElement: function(entity, refNode, refNodeIsParent) {
		_Logger.log(_LogType.ELEMENTS, '_Elements.appendElementElement', entity);

		if (!entity) {
			return false;
		}

		entity = StructrModel.ensureObject(entity);

		var hasChildren = entity.childrenIds && entity.childrenIds.length;

		// store active nodes in special place..
		var isActiveNode = entity.isActiveNode();

		var parent;
		if (refNodeIsParent) {
			parent = refNode;
		} else {
			parent = entity.parent && entity.parent.id ? Structr.node(entity.parent.id) : elements;
		}

		_Logger.log(_LogType.ELEMENTS, 'appendElementElement parent, refNode, refNodeIsParent', parent, refNode, refNodeIsParent);
		if (!parent)
			return false;

		_Entities.ensureExpanded(parent);

		var id = entity.id;

		var html = '<div id="id_' + id + '" class="node element' + (entity.tag === 'html' ? ' html_element' : '') + ' ' + (isActiveNode ? ' activeNode' : 'staticNode') + '"></div>';

		if (refNode && !refNodeIsParent) {
			refNode.before(html);
		} else {
			parent.append(html);
		}

		var div = Structr.node(id);

		_Logger.log(_LogType.ELEMENTS, 'Element appended (div, parent)', div, parent);

		if (!div)
			return false;

		var displayName = getElementDisplayName(entity);

		var icon = _Elements.getElementIcon(entity);

		div.append('<img class="typeIcon" src="' + icon + '">'
			+ '<b title="' + displayName + '" class="tag_ name_">' + fitStringToWidth(displayName, 200) + '</b><span class="id">' + entity.id + '</span>'
			+ _Elements.classIdString(entity._html_id, entity._html_class)
			+ '</div>');

		if (entity.parent) {
			div.append('<img title="Clone ' + displayName + ' element ' + entity.id + '\" alt="Clone ' + entity.tag + ' element ' + entity.id + '" class="clone_icon button" src="icon/page_copy.png">');
			$('.clone_icon', div).on('click', function(e) {
				e.stopPropagation();
				_Logger.log(_LogType.ELEMENTS, 'Cloning node (div, parent)', entity, entity.parent);
				Command.cloneNode(entity.id, entity.parent.id, true);
			});
		}

		_Elements.appendContextMenu(div, entity);

		_Entities.appendExpandIcon(div, entity, hasChildren);

		// Prevent type icon from being draggable
		$('.typeIcon', div).on('mousedown', function(e) {
			e.stopPropagation();
		});

		// Prevent display name
		$('b', div).on('mousedown', function(e) {
			e.stopPropagation();
		});

		// Prevent id from being draggable
		$('#id', div).on('mousedown', function(e) {
			e.stopPropagation();
		});

		// Prevent icons from being draggable
		$('img', div).on('mousedown', function(e) {
			e.stopPropagation();
		});

		_Entities.appendAccessControlIcon(div, entity);
		div.append('<img title="Delete ' + displayName + ' element ' + entity.id + '" alt="Delete ' + entity.tag + ' element ' + entity.id + '" class="delete_icon button" src="' + Structr.delete_icon + '">');
		$('.delete_icon', div).on('click', function(e) {
			e.stopPropagation();
			_Entities.deleteNode(this, entity, function() {
				var synced = entity.syncedNodes;
				if (synced && synced.length) {
					synced.forEach(function(id) {
						var el = Structr.node(id);
						if (el && el.children && el.children.length) {
							el.children('img.typeIcon').attr('src', _Elements.icon);
						}

					});
				}
			});
		});

		_Entities.setMouseOver(div, undefined, ((entity.syncedNodes&&entity.syncedNodes.length)?entity.syncedNodes:[entity.sharedComponent]));

		if (!hasChildren) {

			if (entity.sharedComponent) {
				Command.get(entity.sharedComponent, function(obj) {
					_Entities.appendEditSourceIcon(div, obj);
				});
			} else {
				_Entities.appendEditSourceIcon(div, entity);
			}
		}

		_Entities.appendEditPropertiesIcon(div, entity);
		//_Entities.appendDataIcon(div, entity);

		if (entity.tag === 'a' || entity.tag === 'link' || entity.tag === 'script' || entity.tag === 'img' || entity.tag === 'video' || entity.tag === 'object') {

			div.append('<img title="Edit Link" alt="Edit Link" class="link_icon button" src="' + Structr.link_icon + '">');
			if (entity.linkable) {
				div.append('<span class="linkable">' + entity.linkable + '</span>');
			}

			$('.linkable', div).on('click', function(e) {
				e.stopPropagation();

				var file = {'name': entity.linkable, 'id': entity.linkableId};

				Structr.dialog('Edit ' + file.name, function() {
					_Logger.log(_LogType.ELEMENTS, 'content saved')
				}, function() {
					_Logger.log(_LogType.ELEMENTS, 'cancelled')
				});
				_Files.editContent(this, file, $('#dialogBox .dialogText'));

			});

			$('.link_icon', div).on('click', function(e) {
				e.stopPropagation();

				Structr.dialog('Link to Resource (Page, File or Image)', function() {
					return true;
				}, function() {
					return true;
				});

				dialog.empty();
				dialogMsg.empty();

				if (entity.tag !== 'img') {

					dialog.append('<p>Click on a Page, File or Image to establish a hyperlink to this &lt;' + entity.tag + '&gt; element.</p>');

					dialog.append('<h3>Pages</h3><div class="linkBox" id="pagesToLink"></div>');

					var pagesToLink = $('#pagesToLink');

					_Pager.addPager(pagesToLink, true, 'Page', function(pages) {

						pages.forEach(function(page){

							if (page.type === 'ShadowDocument') {
								return;
							}

							pagesToLink.append('<div class="node page ' + page.id + '_"><img class="typeIcon" src="icon/page.png">'
									+ '<b title="' + page.name + '" class="name_">' + page.name + '</b></div>');

							var div = $('.' + page.id + '_', pagesToLink);

							if (isIn(entity.id, page.linkingElements)) {
								div.addClass('nodeActive');
							}

							div.on('click', function(e) {
								e.stopPropagation();
								if (div.hasClass('nodeActive')) {
									Command.setProperty(entity.id, 'linkableId', null);
								} else {
									Command.link(entity.id, page.id);
								}
								_Entities.reloadChildren(entity.parent.id);
								$('#dialogBox .dialogText').empty();
								_Pages.reloadPreviews();
								$.unblockUI({
									fadeOut: 25
								});
							}).css({
								cursor: 'pointer'
							}).hover(function() {
								$(this).addClass('nodeHover');
							}, function() {
								$(this).removeClass('nodeHover');
							});
						});

					});

					dialog.append('<h3>Files</h3><div class="linkBox" id="foldersToLink"></div><div class="linkBox" id="filesToLink"></div>');

					var filesToLink = $('#filesToLink');
					var foldersToLink = $('#foldersToLink');

					var linkFolderPager = _Pager.addPager(foldersToLink, true, 'Folder', function(folders) {

						folders.forEach(function(folder) {

							if (folder.files.length + folder.folders.length === 0) {
								return;
							}

							foldersToLink.append('<div class="node folder ' + folder.id + '_"><img class="typeIcon" src="icon/folder.png">'
									+ '<b title="' + folder.name + '" class="name_">' + folder.name + '</b></div>');

							var div = $('.' + folder.id + '_', foldersToLink);
							div.on('click', function(e) {
								if (!div.children('.node').length) {
									_Elements.expandFolder(e, entity, folder);
								} else {
									div.children('.node').remove();
								}
							}).css({
								cursor: 'pointer'
							}).hover(function() {
								$(this).addClass('nodeHover');
							}, function() {
								$(this).removeClass('nodeHover');
							});

						});

					});

					linkFolderPager.pager.append('<input type="checkbox" class="filter" data-attribute="hasParent" hidden>');
					linkFolderPager.activateFilterElements();


					var linkFilesPager = _Pager.addPager(filesToLink, true, 'FileBase', function(files) {

						files.forEach(function(file) {

							filesToLink.append('<div class="node file ' + file.id + '_"><img class="typeIcon" src="' + _Files.getIcon(file) + '">'
									+ '<b title="' + file.name + '" class="name_">' + file.name + '</b></div>');

							var div = $('.' + file.id + '_', filesToLink);

							if (isIn(entity.id, file.linkingElements)) {
								div.addClass('nodeActive');
							}

							div.on('click', function(e) {
								e.stopPropagation();
								if (div.hasClass('nodeActive')) {
									Command.setProperty(entity.id, 'linkableId', null);
								} else {
									Command.link(entity.id, file.id);
								}
								_Entities.reloadChildren(entity.parent.id);
								$('#dialogBox .dialogText').empty();
								_Pages.reloadPreviews();
								$.unblockUI({
									fadeOut: 25
								});
							}).css({
								cursor: 'pointer'
							}).hover(function() {
								$(this).addClass('nodeHover');
							}, function() {
								$(this).removeClass('nodeHover');
							});

						});

					});

					linkFilesPager.pager.append('<input type="checkbox" class="filter" data-attribute="hasParent" hidden>');
					linkFilesPager.activateFilterElements();

				}

				if (entity.tag === 'img' || entity.tag === 'link' || entity.tag === 'a') {

					dialog.append('<h3>Images</h3><div class="linkBox" id="imagesToLink"></div>');

					var imagesToLink = $('#imagesToLink');

					_Pager.addPager(imagesToLink, false, 'Image', function(images) {

						images.forEach(function(image) {

							imagesToLink.append('<div class="node file ' + image.id + '_"><img class="typeIcon" src="' + _Images.getIcon(image) + '">'
									+ '<b title="' + image.name + '" class="name_">' + image.name + '</b></div>');

							var div = $('.' + image.id + '_', imagesToLink);

							if (isIn(entity.id, image.linkingElements)) {
								div.addClass('nodeActive');
							}

							div.on('click', function(e) {
								e.stopPropagation();
								if (div.hasClass('nodeActive')) {
									console.log('removing')
									Command.setProperty(entity.id, 'linkableId', null);
								} else {
									Command.link(entity.id, image.id);
								}
								_Entities.reloadChildren(entity.parent.id);
								$('#dialogBox .dialogText').empty();
								_Pages.reloadPreviews();
								$.unblockUI({
									fadeOut: 25
								});
							}).css({
								cursor: 'pointer'
							}).hover(function() {
								$(this).addClass('nodeHover');
							}, function() {
								$(this).removeClass('nodeHover');
							});

						});

					});

				}

			});
		}
		return div;
	},
	getElementIcon:function(element) {
		var isComponent = element.sharedComponent || (element.syncedNodes && element.syncedNodes.length);
		var isActiveNode = element.isActiveNode();

		return isActiveNode ? _Elements.icon_repeater : isComponent ? _Elements.icon_comp : _Elements.icon;
	},
	classIdString: function(idString, classString) {
		var classIdString = '<span class="class-id-attrs">' + (idString ? '<span class="_html_id_">#' + idString.replace(/\${.*}/g, '${…}') + '</span>' : '')
				+ (classString ? '<span class="_html_class_">.' + classString.replace(/\${.*}/g, '${…}').replace(/ /g, '.') + '</span>' : '') + '</span>';
		return classIdString;
	},
	expandFolder: function(e, entity, folder, callback) {
		if (folder.files.length + folder.folders.length === 0) {
			return;
		}

		var div = $('.' + folder.id + '_');
		div.css({'border': '1px solid #ccc', 'backgroundColor': '#f5f5f5'});

		div.children('b').on('click', function() {
			$(this).siblings('.node.sub').remove();
		});

		$.each(folder.folders, function(i, subFolder) {
			e.stopPropagation();
			$('.' + folder.id + '_').append('<div class="clear"></div><div class="node folder sub ' + subFolder.id + '_"><img class="typeIcon" src="icon/folder.png">'
					+ '<b title="' + subFolder.name + '" class="name_">' + subFolder.name + '</b></div>');
			var subDiv = $('.' + subFolder.id + '_');

			subDiv.on('click', function(e) {
				if (!subDiv.children('.node').length) {
					e.stopPropagation();
					Command.get(subFolder.id, function(node) {
						_Elements.expandFolder(e, entity, node, callback);
					});
				} else {
					subDiv.children('.node').remove();
				}
				return false;
			}).css({
				cursor: 'pointer'
			}).hover(function() {
				$(this).addClass('nodeHover');
			}, function() {
				$(this).removeClass('nodeHover');
			});

		});

		$.each(folder.files, function(i, f) {

			Command.get(f.id, function(file) {

				$('.' + folder.id + '_').append('<div class="clear"></div><div class="node file sub ' + file.id + '_"><img class="typeIcon" src="' + _Files.getIcon(file) + '">'
						+ '<b title="' + file.name + '" class="name_">' + file.name + '</b></div>');
				var div = $('.' + file.id + '_');

				if (isIn(entity.id, file.linkingElements)) {
					div.addClass('nodeActive');
				}

				div.on('click', function(e) {
					e.stopPropagation();
					if (div.hasClass('nodeActive')) {
						Command.setProperty(entity.id, 'linkableId', null);
					} else {
						Command.link(entity.id, file.id);
					}
					_Entities.reloadChildren(entity.parent.id);
					$('#dialogBox .dialogText').empty();
					_Pages.reloadPreviews();
					$.unblockUI({
						fadeOut: 25
					});
				}).css({
					cursor: 'pointer'
				}).hover(function() {
					$(this).addClass('nodeHover');
				}, function() {
					$(this).removeClass('nodeHover');
				});
			});

		});
	},
	appendContextMenu: function(div, entity) {

		$('#menu-area').on("contextmenu",function(e){
			e.stopPropagation();
			e.preventDefault();
		});

		$(div).on("contextmenu",function(e){
			e.stopPropagation();
			e.preventDefault();
		});

		$(div).on('mousedown', function(e) {

			if (e.button !== 2 || $(e.target).hasClass('content')) {
				return;
			}

			e.stopPropagation();

			$('#menu-area').append('<div id="add-child-dialog"></div>');

			var leftOrRight = 'left';
			var topOrBottom = 'top';
			var x = (e.pageX - 8);
			var y = (div.offset().top - 58);

			if (e.pageX > ($(window).width() / 2)) {
				leftOrRight = 'right';
			}

			if (e.pageY > ($(window).height() / 2)) {
				topOrBottom = 'bottom';
				y -= 36;
			}

			var cssPositionClasses = leftOrRight + ' ' + topOrBottom;

			$('#add-child-dialog').css('left', x + 'px');
			$('#add-child-dialog').css('top', y + 'px');

			// FIXME: its either this or accept that the div will not be highlighted any more when the menu appears. This is
			// due to the fact that the menu has to be outside of the actual div to be visible even with overflow: hidden,
			// which is needed to hide the vertical scroll bar in the pages tree view and others.
			window.setTimeout(function() { $(div).addClass('nodeHover') }, 10);

			var menu = [
				{ name: 'Insert element',    elements: _Elements.elementGroups },
				{ name: 'Expand / Collapse', elements: [
						{ name: 'Expand subtree', func: function() {

							$(div).find('.node').each(function(i, el) {
								if (!_Entities.isExpanded(el)) {
									_Entities.toggleElement(el);
								}
							});
							if (!_Entities.isExpanded(div)) {
								_Entities.toggleElement(div);
							}
						}},
						{ name: 'Collapse subtree', func: function() {

							$(div).find('.node').each(function(i, el) {
								if (_Entities.isExpanded(el)) {
									_Entities.toggleElement(el);
								}
							});
							if (_Entities.isExpanded(div)) {
								_Entities.toggleElement(div);
							}
						}}
					], separator: true
				},
				/*
				{ name: 'Movement', elements: [

						{ name: 'Move to top',    func: function() {} },
						{ name: 'Move to bottom', func: function() {} }
					]
				},
				{ name: 'Other actions', elements: [
						{ name: 'Remove element', func: function() {} },
						{ name: 'Delete element', func: function() {} }
					]
				}
				*/
			];

			// information about most used elements in this page from backend
			if (entity.mostUsedTags && entity.mostUsedTags.length) {
				menu.push({
					name: 'Most used elements', elements: entity.mostUsedTags
				});
			}

			menu.forEach(function(item, i) {

				$('#add-child-dialog').append(
					'<ul class="' + cssPositionClasses + '" id="element-menu-' + i + '"><li id="element-group-switch-' + i + '">' + item.name +
					'<i class="fa fa-caret-right pull-right"></i>' +
					'<ul class="element-group hidden ' + cssPositionClasses + '" id="element-group-' + i + '"></ul></li></ul>'
				);

				$('#element-menu-' + i ).append(item.separator ? '<hr />' : '');

				item.elements.forEach(function(subitem, j) {

					if (subitem.elements && subitem.elements.length) {

						$('#element-group-' + i).append(
							'<li id="element-subgroup-switch-' + i + '-' + j + '">' + subitem.name +
							'<i class="fa fa-caret-right pull-right"></i>' +
							'<ul class="element-subgroup hidden ' + cssPositionClasses + '" id="element-subgroup-' + i + '-' + j + '"></ul></li>'
						);

						subitem.elements.forEach(function(tag, k) {

							$('#element-subgroup-' + i + '-' + j).append('<li id="add-' + tag + '-' + i + '-' + j + '-' + k + '">' + tag + '</li>');
							$('#add-' + tag + '-' + i + '-' + j + '-' + k).on('mouseup', function(e) {
								e.stopPropagation();
								if (tag === 'content') {
									Command.createAndAppendDOMNode(entity.pageId, entity.id, null, {});
								} else {
									Command.createAndAppendDOMNode(entity.pageId, entity.id, tag, {});
								}
								$('#add-child-dialog').remove();
								$(div).removeClass('nodeHover');
							});
						});

					} else {

						$('#element-group-' + i ).append('<li id="add-' + i + '-' + j + '">' + (subitem.name ? subitem.name : subitem) + '</li>');
						$('#add-' + i + '-' + j).on('mouseup', function(e) {
							e.stopPropagation();
							if (subitem.func && (typeof subitem.func === 'function')) {
								subitem.func();
							} else {
								Command.createAndAppendDOMNode(entity.pageId, entity.id, subitem, {});
							}
							$('#add-child-dialog').remove();
							$(div).removeClass('nodeHover');
						});
					}

					$('#element-subgroup-switch-' + i + '-' + j).hover(function() {
						$('.element-subgroup').addClass('hidden');
						$('#element-subgroup-' + i + '-' + j).removeClass('hidden');
					}, function() {});
				});

				$('#element-group-switch-' + i).hover(function() {
					$('.element-group').addClass('hidden');
					$('#element-group-' + i).removeClass('hidden');
				}, function() {});
			});

			var offHandler = function() {

				$('#add-child-dialog').remove();
				$(div).removeClass('nodeHover');

				// remove offHandler when menu closes
				$(window.document).off('mouseup', offHandler);
			};

			$(window.document).on('mouseup', offHandler);
		});
	}
};