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
var lineWrappingKey = 'structrEditorLineWrapping_' + port;
var contents, editor, contentType, currentEntity;

var _Elements = {
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
			name: 'Root',
			elements: ['html', 'content', 'comment', 'template']
		},
		{
			name: 'Metadata',
			elements: ['head', 'title', 'base', 'link', 'meta', 'style']
		},
		{
			name: 'Sections',
			elements: ['body', 'section', 'nav', 'article', 'aside', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hgroup', 'header', 'footer', 'address']
		},
		{
			name: 'Grouping',
			elements: ['div', 'p', 'hr', 'ol', 'ul', 'li', 'dl', 'dt', 'dd', 'pre', 'blockquote', 'figure', 'figcaption']
		},
		{
			name: 'Scripting',
			elements: ['script', 'noscript']
		},
		{
			name: 'Tabular',
			elements: ['table', 'tr', 'td', 'th', 'caption', 'colgroup', 'col', 'tbody', 'thead', 'tfoot']
		},
		{
			name: 'Text',
			elements: ['a', 'em', 'strong', 'small', 's', 'cite', 'g', 'dfn', 'abbr', 'time', 'code', 'var', 'samp', 'kbd', 'sub', 'sup', 'i', 'b', 'u', 'mark', 'ruby', 'rt', 'rp', 'bdi', 'bdo', 'span', 'br', 'wbr']
		},
		{
			name: 'Edits',
			elements: ['ins', 'del']
		},
		{
			name: 'Embedded',
			elements: ['img', 'video', 'audio', 'source', 'track', 'canvas', 'map', 'area', 'iframe', 'embed', 'object', 'param']
		},
		{
			name: 'Forms',
			elements: ['form', 'input', 'button', 'select', 'datalist', 'optgroup', 'option', 'textarea', 'fieldset', 'legend', 'label', 'keygen', 'output', 'progress', 'meter']
		},
		{
			name: 'Interactive',
			elements: ['details', 'summary', 'command', 'menu']
		}
	],
	mostUsedAttrs: [
		{
			elements: ['input'],
			attrs: ['name', 'type', 'checked', 'selected', 'value', 'size', 'multiple', 'disabled', 'autofocus', 'placeholder'],
			focus: 'type'
		},
		{
			elements: ['button'],
			attrs: ['name', 'type', 'checked', 'selected', 'value', 'size', 'multiple', 'disabled', 'autofocus', 'placeholder', 'onclick']
		},
		{
			elements: ['select', 'option'],
			attrs: ['name', 'type', 'checked', 'selected', 'value', 'size', 'multiple', 'disabled', 'autofocus', 'placeholder']
		},
		{
			elements: ['optgroup'],
			attrs: ['label', 'disabled'],
			focus: 'label'
		},
		{
			elements: ['form'],
			attrs: ['action', 'method']
		},
		{
			elements: ['img'],
			attrs: ['alt', 'title', 'src'],
			focus: 'src'
		},
		{
			elements: ['script', 'img', 'object'],
			attrs: ['type', 'rel', 'href', 'media', 'src'],
			focus: 'src'
		},
		{
			elements: ['link'],
			attrs: ['type', 'rel', 'href'],
			focus: 'href'
		},
		{
			elements: ['a'],
			attrs: ['type', 'rel', 'href', 'target'],
			focus: 'href'
		},
		{
			elements: ['td', 'th'],
			attrs: ['colspan', 'rowspan']
		},
		{
			elements: ['label'],
			attrs: ['for', 'form'],
			focus: 'for'
		},
		{
			elements: ['style'],
			attrs: ['type', 'media', 'scoped'],
			focus: 'type'
		},
		{
			elements: ['iframe'],
			attrs: ['src', 'width', 'height'],
			focus: 'src'
		},
		{
			elements: ['source'],
			attrs: ['src', 'type', 'media'],
			focus: 'src'
		},
		{
			elements: ['video'],
			attrs: ['autoplay', 'controls', 'height', 'loop', 'muted', 'poster', 'preload', 'src', 'width'],
			focus: 'controls'
		},
		{
			elements: ['audio'],
			attrs: ['autoplay', 'controls', 'loop', 'muted', 'preload', 'src'],
			focus: 'controls'
		}
	],
	voidAttrs: ['br', 'hr', 'img', 'input', 'link', 'meta', 'area', 'base', 'col', 'embed', 'keygen', 'menuitem', 'param', 'track', 'wbr'],
	sortedElementGroups: [
		{
			name: 'a',
			elements: ['a', 'abbr', 'address', 'area', 'aside', 'article', 'audio']
		},
		{
			name: 'b',
			elements: ['b', 'base', 'bdi', 'bdo', 'blockquote', 'body', 'br', 'button']
		},
		{
			name: 'c',
			elements: ['canvas', 'caption', 'cite', 'code', 'colgroup', 'col', 'command', 'comment']
		},
		{
			name: 'd',
			elements: ['datalist', 'dd', 'del', 'details', 'div', 'dfn', 'dl', 'dt']
		},
		{
			name: 'e-f',
			elements: ['em', 'embed', '|', 'fieldset', 'figcaption', 'figure', 'form', 'footer']
		},
		{
			name: 'g-h',
			elements: ['g', '|', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'header', 'hgroup', 'hr']
		},
		{
			name: 'i-k',
			elements: ['i', 'iframe', 'img', 'input', 'ins', '|', 'kbd', 'keygen']
		},
		{
			name: 'l-m',
			elements: ['label', 'legend', 'li', 'link', '|', 'map', 'mark', 'menu', 'meta', 'meter']
		},
		{
			name: 'n-o',
			elements: ['nav', 'noscript', '|', 'object', 'ol', 'optgroup', 'option', 'output']
		},
		{
			name: 'p-r',
			elements: ['p', 'param', 'pre', 'progress', '|', 'rp', 'rt', 'ruby']
		},
		{
			name: 's',
			elements: ['s', 'samp', 'script', 'section', 'select', 'small', 'source', 'span', 'strong', 'style', 'sub', 'summary', 'sup']
		},
		{
			name: 't',
			elements: ['table', 'tbody', 'td', 'textarea', 'th', 'thead', 'tfoot', 'time', 'title', 'tr', 'track']
		},
		{
			name: 'u-w',
			elements: ['u', 'ul', '|', 'var', 'video', '|', 'wbr']
		}
	],
	reloadPalette: function() {

		paletteSlideout.find(':not(.compTab)').remove();
		paletteSlideout.append('<div class="ver-scrollable" id="paletteArea"></div>');
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
	reloadComponents: function() {

		if (!componentsSlideout) return;
		componentsSlideout.find(':not(.compTab)').remove();
		componentsSlideout.append('<div class="ver-scrollable" id="componentsArea"></div>');
		components = $('#componentsArea', componentsSlideout);

		components.droppable({
			drop: function(e, ui) {
				e.preventDefault();
				e.stopPropagation();

				if (!shadowPage) {
					// Create shadow page if not existing
					Command.getByType('ShadowDocument', 1, 1, null, null, null, true, function(entities) {
						shadowPage = entities[0];
						_Elements.createComponend(ui);
					});
				} else {
					_Elements.createComponend(ui);
				}
				
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
					el = _Elements.appendContentElement(obj, components, true);
				} else {
					el = _Pages.appendElementElement(obj, components, true);
				}

				if (isExpanded(entity.id)) {
					_Entities.ensureExpanded(el);
				}

			});

		});

	},
	createComponend: function(el) {
		
		dropBlocked = true;
		var sourceEl = $(el.draggable);
		var sourceId = Structr.getId(sourceEl);
		if (!sourceId) return false;
		var obj = StructrModel.obj(sourceId);
		if (obj && obj.syncedNodes && obj.syncedNodes.length || sourceEl.parent().attr('id') === 'componentsArea') {
			_Logger.log(_LogType.ELEMENTS, 'component dropped on components area, aborting');
			return false;
		}
		Command.createComponent(sourceId);
		dropBlocked = false;
	},
	reloadUnattachedNodes: function() {

		elementsSlideout.find(':not(.compTab)').remove();
		elementsSlideout.append('<div class="ver-scrollable" id="elementsArea"></div>');
		elements = $('#elementsArea', elementsSlideout);

		elements.append('<button class="btn" id="delete-all-unattached-nodes">Delete all</button>');

		var btn = $('#delete-all-unattached-nodes');
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
					el = _Elements.appendContentElement(obj, elements, true);
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
			div.append('<img title="Clone ' + displayName + ' element ' + entity.id + '\" alt="Clone ' + entity.tag + ' element ' + entity.id + '" class="clone_icon button" src="' + _Icons.clone_icon + '">');
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
		div.append('<img title="Delete ' + displayName + ' element ' + entity.id + '" alt="Delete ' + entity.tag + ' element ' + entity.id + '" class="delete_icon button" src="' + _Icons.delete_icon + '">');
		$('.delete_icon', div).on('click', function(e) {
			e.stopPropagation();
			_Entities.deleteNode(this, entity, function() {
				var synced = entity.syncedNodes;
				if (synced && synced.length) {
					synced.forEach(function(id) {
						var el = Structr.node(id);
						if (el && el.children && el.children.length) {
							el.children('img.typeIcon').attr('src', _Icons.brick_icon);
						}

					});
				}
			});
		});

		_Entities.setMouseOver(div, undefined, ((entity.syncedNodes&&entity.syncedNodes.length)?entity.syncedNodes:[entity.sharedComponent]));

		if (!hasChildren && !entity.sharedComponent) {
			_Entities.appendEditSourceIcon(div, entity);
		}

		_Entities.appendEditPropertiesIcon(div, entity);
		//_Entities.appendDataIcon(div, entity);

		if (entity.tag === 'a' || entity.tag === 'link' || entity.tag === 'script' || entity.tag === 'img' || entity.tag === 'video' || entity.tag === 'object') {

			div.append('<img title="Edit Link" alt="Edit Link" class="link_icon button" src="' + _Icons.link_icon + '">');
			if (entity.linkable) {
				div.append('<span class="linkable">' + entity.linkable + '</span>');
			}

			$('.linkable', div).on('click', function(e) {
				e.stopPropagation();

				var file = {'name': entity.linkable, 'id': entity.linkableId};

				Structr.dialog('Edit ' + file.name, function() {
					_Logger.log(_LogType.ELEMENTS, 'content saved');
				}, function() {
					_Logger.log(_LogType.ELEMENTS, 'cancelled');
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

					_Pager.initPager('pages-to-link', 'Page', 1, 25);
					_Pager.addPager('pages-to-link', pagesToLink, true, 'Page', null, function(pages) {

						pages.forEach(function(page){

							if (page.type === 'ShadowDocument') {
								return;
							}

							pagesToLink.append('<div class="node page ' + page.id + '_"><img class="typeIcon" src="' + _Icons.page_icon + '">'
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

					_Pager.initPager('folders-to-link', 'Folder', 1, 25);
					_Pager.initFilters('folders-to-link', 'Folder', {
						hasParent: false
					});
					var linkFolderPager = _Pager.addPager('folders-to-link', foldersToLink, true, 'Folder', 'public', function(folders) {

						folders.forEach(function(folder) {

							if (folder.files.length + folder.folders.length === 0) {
								return;
							}

							foldersToLink.append('<div class="node folder ' + folder.id + '_"><i class="fa fa-folder"></i> '
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

					_Pager.initPager('files-to-link', 'FileBase', 1, 25);
					var linkFilesPager = _Pager.addPager('files-to-link', filesToLink, true, 'FileBase', 'public', function(files) {

						files.forEach(function(file) {

							filesToLink.append('<div class="node file ' + file.id + '_"><i class="fa ' + _Files.getIcon(file) + '"></i> '
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

					_Pager.initPager('images-to-link', 'Image', 1, 25);
					_Pager.addPager('images-to-link', imagesToLink, false, 'Image', 'public', function(images) {

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
									//console.log('removing')
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

		return (isActiveNode ? _Icons.repeater_icon : (isComponent ? _Icons.comp_icon : _Icons.brick_icon));
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
		//div.css({'border': '1px solid #ccc', 'backgroundColor': '#f5f5f5'});

		div.children('b').on('click', function() {
			$(this).siblings('.node.sub').remove();
		});

		$.each(folder.folders, function(i, subFolder) {
			e.stopPropagation();
			$('.' + folder.id + '_').append('<div class="clear"></div><div class="node folder sub ' + subFolder.id + '_"><i class="fa fa-folder"></i> '
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

				$('.' + folder.id + '_').append('<div class="clear"></div><div class="node file sub ' + file.id + '_"><i class="fa ' + _Files.getIcon(file) + '"></i> '
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

		$(div).on('mouseup', function(e) {

			if (e.button !== 2 || $(e.target).hasClass('content')) {
				return;
			}

			e.stopPropagation();

			$('#add-child-dialog').remove();
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
				y -= 175;

				if (entity.mostUsedTags.length) {
					y -= 24;
				}
			}

			var cssPositionClasses = leftOrRight + ' ' + topOrBottom;

			$('#add-child-dialog').css('left', x + 'px');
			$('#add-child-dialog').css('top', y + 'px');

			// FIXME: its either this or accept that the div will not be highlighted any more when the menu appears. This is
			// due to the fact that the menu has to be outside of the actual div to be visible even with overflow: hidden,
			// which is needed to hide the vertical scroll bar in the pages tree view and others.
			var setHover = function() {
				$(div).addClass('nodeHover');
				if ($('#add-child-dialog').length) {
					window.setTimeout(setHover, 200);
				}
			};

			window.setTimeout(setHover, 10);

			var menu = [
				{
					name: 'Insert HTML element',
					elements: _Elements.sortedElementGroups
				},
				{
					name: 'Insert content element',
					elements: ['content', 'template'],
					separator: true
				},
				{
					name: 'Query and Data Binding...',
					func: function() {
						_Entities.showProperties(entity, 'query');
					}
				},
				{
					name: 'Edit Mode Binding...',
					func: function() {
						_Entities.showProperties(entity, 'editBinding');
					}
				},
				{
					name: 'HTML Attributes...',
					func: function() {
						_Entities.showProperties(entity, '_html_');
					}
				},
				{
					name: 'Node Properties...',
					func: function() {
						_Entities.showProperties(entity, 'ui');
					},
					separator: true
				},
				{
					name: 'Security...',
					elements: [
						{
							name: 'Access Control and Visibility...',
							func: function() {
								_Entities.showAccessControlDialog(entity.id);
							},
							separator: true
						},
						{
							name: 'Authenticated Users...',
							elements: [
								{
									name: 'Make element visible',
									func: function() {
										Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', true, false);
									}
								},
								{
									name: 'Make Element invisible',
									func: function() {
										Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', false, false);
									}
								},
								{
									name: 'Make subtree visible',
									func: function() {
										Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', true, true);
									},
									separator: true
								},
								{
									name: 'Make subtree invisible',
									func: function() {
										Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', false, true);
									}
								}
							],
							separator: true
						},
						{
							name: 'Public Users...',
							elements: [
								{
									name: 'Make element visible',
									func: function() {
										Command.setProperty(entity.id, 'visibleToPublicUsers', true, false);
									}
								},
								{
									name: 'Make element invisible',
									func: function() {
										Command.setProperty(entity.id, 'visibleToPublicUsers', false, false);
									}
								},
								{
									name: 'Make subtree visible',
									func: function() {
										Command.setProperty(entity.id, 'visibleToPublicUsers', true, true);
									},
									separator: true
								},
								{
									name: 'Make subtree invisible',
									func: function() {
										Command.setProperty(entity.id, 'visibleToPublicUsers', false, true);
									}
								}
							]
						}
					],
					separator: true
				},
				{
					name: 'Expand / Collapse',
					elements: [
						{
							name: 'Expand subtree',
							func: function() {
								$(div).find('.node').each(function(i, el) {
									if (!_Entities.isExpanded(el)) {
										_Entities.toggleElement(el);
									}
								});
								if (!_Entities.isExpanded(div)) {
									_Entities.toggleElement(div);
								}
							}
						},
						{
							name: 'Collapse subtree',
							func: function() {
								$(div).find('.node').each(function(i, el) {
									if (_Entities.isExpanded(el)) {
										_Entities.toggleElement(el);
									}
								});
								if (_Entities.isExpanded(div)) {
									_Entities.toggleElement(div);
								}
							}
						}
					],
					separator: true
				}
			];

			// information about most used elements in this page from backend
			if (entity.mostUsedTags && entity.mostUsedTags.length) {
				menu.push({
					name: 'Most used elements',
					elements: entity.mostUsedTags,
					separator: true
				});
			}

			menu.forEach(function(item, i) {

				var isSubmenu = item.elements && item.elements.length;

				$('#add-child-dialog').append(
					'<ul class="' + cssPositionClasses + '" id="element-menu-' + i + '"><li id="element-group-switch-' + i + '">' + item.name +
					(isSubmenu ?
						'<i class="fa fa-caret-right pull-right"></i>' +
						'<ul class="element-group hidden ' +
						cssPositionClasses +
						'" id="element-group-' + i + '"></ul>'
						: ''
					) +
					'</li></ul>'
				);

				if (item.separator) {
					$('#element-menu-' + i ).append('<hr />');
				}

				if (isSubmenu) {

					item.elements.forEach(function(subitem, j) {

						if (subitem.elements && subitem.elements.length) {

							if (subitem.separator) {
								$('#element-group-' + i).append('<hr />');
							}

							$('#element-group-' + i).append(
								'<li id="element-subgroup-switch-' + i + '-' + j + '">' + subitem.name +
								'<i class="fa fa-caret-right pull-right"></i>' +
								'<ul class="element-subgroup hidden ' + cssPositionClasses + '" id="element-subgroup-' + i + '-' + j + '"></ul></li>'
							);

							subitem.elements.forEach(function(subsubitem, k) {

								if (subsubitem.separator) {
									$('#element-subgroup-' + i + '-' + j).append('<hr />');
								}

								if (subsubitem.func && (typeof subsubitem.func === 'function')) {

									$('#element-subgroup-' + i + '-' + j).append(
										'<li id="element-subsubgroup-switch-' + i + '-' + j + '-' + k + '">' + subsubitem.name + '</li>'
									);

									$('#element-subsubgroup-switch-' + i + '-' + j + '-' + k).on('mouseup', subsubitem.func);

								} else {

									if (subsubitem === '|') {

										$('#element-subgroup-' + i + '-' + j).append('<hr />');

									} else {

										$('#element-subgroup-' + i + '-' + j).append('<li id="add-' + subsubitem + '-' + i + '-' + j + '-' + k + '">' + subsubitem + '</li>');
										$('#add-' + subsubitem + '-' + i + '-' + j + '-' + k).on('mouseup', function(e) {

											e.stopPropagation();
											if (subsubitem === 'content') {
												Command.createAndAppendDOMNode(entity.pageId, entity.id, null, {});
											} else {
												Command.createAndAppendDOMNode(entity.pageId, entity.id, subsubitem, {});
											}
											$('#add-child-dialog').remove();
											$(div).removeClass('nodeHover');
										});
									}
								}
							});

						} else {

							$('#element-group-' + i ).append('<li id="add-' + i + '-' + j + '">' + (subitem.name ? subitem.name : subitem) + '</li>');
							$('#add-' + i + '-' + j).on('mouseup', function(e) {

								e.stopPropagation();
								if (subitem.func && (typeof subitem.func === 'function')) {
									subitem.func();
								} else {
									if (subitem === 'content') {
										Command.createAndAppendDOMNode(entity.pageId, entity.id, null, {});
									} else {
										Command.createAndAppendDOMNode(entity.pageId, entity.id, subitem, {});
									}
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

				} else {

					$('#element-menu-' + i).on('mouseup', function(e) {

						e.stopPropagation();

						if (item.func && (typeof item.func === 'function')) {
							item.func();
						}

						$('#add-child-dialog').remove();
						$(div).removeClass('nodeHover');
					});
				}

				$('#element-group-switch-' + i).hover(function() {
					$('.element-group').addClass('hidden');
					$('#element-group-' + i).removeClass('hidden');
				}, function() {});
			});
		});
	},
	appendContentElement: function(entity, refNode, refNodeIsParent) {
		_Logger.log(_LogType.CONTENTS, 'Contents.appendContentElement', entity, refNode);

		var parent;

		if (entity.parent && entity.parent.id) {
			parent = Structr.node(entity.parent.id);
			_Entities.ensureExpanded(parent);
		} else {
			parent = refNode;
		}

		if (!parent)
			return false;

		var isActiveNode = entity.isActiveNode();
		var isTemplate = (entity.type === 'Template');

		var name = entity.name;
		var displayName = getElementDisplayName(entity);

		var icon = _Elements.getContentIcon(entity);
		var html = '<div id="id_' + entity.id + '" class="node content ' + (isActiveNode ? ' activeNode' : 'staticNode') + '">'
				+ '<img class="typeIcon" src="' + icon + '">'
				+ (name ? ('<b title="' + displayName + '" class="tag_ name_">' + fitStringToWidth(displayName, 200) + '</b>') : ('<div class="content_">' + escapeTags(entity.content) + '</div>'))
				+ '<span class="id">' + entity.id + '</span>'
				+ '</div>';

		if (refNode && !refNodeIsParent) {
			refNode.before(html);
		} else {
			parent.append(html);
		}

		var div = Structr.node(entity.id);

		_Dragndrop.makeSortable(div);
		_Dragndrop.makeDroppable(div);

		if (isTemplate) {
			var hasChildren = entity.childrenIds && entity.childrenIds.length;
			_Entities.appendExpandIcon(div, entity, hasChildren);
		}

		_Entities.appendAccessControlIcon(div, entity);

		div.append('<img title="Clone content node ' + entity.id + '" alt="Clone content node ' + entity.id + '" class="clone_icon button" src="' + _Icons.clone_icon + '">');
		$('.clone_icon', div).on('click', function(e) {
			e.stopPropagation();
			Command.cloneNode(entity.id, entity.parent.id, true);
		});

		div.append('<img title="Delete content \'' + (entity.name ? entity.name : entity.id) + '\'" alt="Delete content \'' + (entity.name ? entity.name : entity.id) + '\'" class="delete_icon button" src="' + _Icons.delete_content_icon + '">');
		$('.delete_icon', div).on('click', function(e) {
			e.stopPropagation();
			_Entities.deleteNode(this, entity);
		});

		div.append('<img title="Edit Content" alt="Edit Content of ' + (entity.name ? entity.name : entity.id) + '" class="edit_icon button" src="' + _Icons.edit_icon + '">');
		$('.edit_icon', div).on('click', function(e) {
			e.stopPropagation();
			_Elements.openEditContentDialog(this, entity);
			return false;
		});

		$('.content_', div).on('click', function(e) {
			e.stopPropagation();
			_Elements.openEditContentDialog(this, entity);
			return false;
		});

		_Entities.setMouseOver(div, undefined, ((entity.syncedNodes&&entity.syncedNodes.length)?entity.syncedNodes:[entity.sharedComponent]));

		_Entities.appendEditPropertiesIcon(div, entity);

		return div;
	},
	getContentIcon:function(content) {
		var isComment = (content.type === 'Comment');
		var isTemplate = (content.type === 'Template');
		var isComponent = content.sharedComponent || (content.syncedNodes && content.syncedNodes.length);
		var isActiveNode = content.isActiveNode();

		return isComment ? _Icons.comment_icon : ((isTemplate && isComponent) ? _Icons.comp_templ_icon : (isTemplate ? (isActiveNode ? _Icons.active_template_icon : _Icons.template_icon) : (isComponent ? _Icons.active_content_icon : (isActiveNode ? _Icons.active_content_icon : _Icons.content_icon))));
	},
	openEditContentDialog: function(btn, entity) {
		Structr.dialog('Edit content of ' + (entity.name ? entity.name : entity.id), function() {
			_Logger.log(_LogType.CONTENTS, 'content saved');
		}, function() {
			_Logger.log(_LogType.CONTENTS, 'cancelled');
		});
		Command.getProperty(entity.id, 'content', function(text) {
            currentEntity = entity;
			_Elements.editContent(this, entity, text, dialogText);
		});
	},
    autoComplete: function(cm, pred) {
      if (!pred || pred()) setTimeout(function() {
        if (!cm.state.completionActive)
			CodeMirror.showHint(cm, _Elements.hint, {
				async: true,
				extraKeys: {
				   "Esc": function(cm, e) {
					   if (cm.state.completionActive) {
						   cm.state.completionActive.close();
						   ignoreKeyUp = true;
					   }
				   }
				}

			});
      }, 100);
      return CodeMirror.Pass;
    },
    hint: function(cm, callback) {

        var cursor        = cm.getCursor();
        var currentToken  = cm.getTokenAt(cursor);
        var previousToken = cm.getTokenAt( { line: cursor.line, ch: currentToken.start - 1 } );
        var thirdToken    = cm.getTokenAt( { line: cursor.line, ch: previousToken.start - 1 } );
        var id            = "";

        if (currentEntity && currentEntity.id) {
            id = currentEntity.id;
        }

		Command.autocomplete(id, currentToken.type, currentToken.string, previousToken.string, thirdToken.string, cursor.line, cursor.ch, function(data) {
            callback( { from: { line: cursor.line, ch: currentToken.end } , to: { line: cursor.line, ch: currentToken.end } , list: data } );
        });

    },
	editContent: function(button, entity, text, element) {
		if (isDisabled(button)) {
			return;
		}
		var div = element.append('<div class="editor"></div>');
		_Logger.log(_LogType.CONTENTS, div);
		var contentBox = $('.editor', element);
		contentType = contentType ? contentType : entity.contentType;
		var text1, text2;

		var lineWrapping = LSWrapper.getItem(lineWrappingKey);

		// Intitialize editor
		editor = CodeMirror(contentBox.get(0), {
			value: text,
			mode: contentType,
			lineNumbers: true,
			lineWrapping: lineWrapping,
			extraKeys: {
				"'.'":        _Elements.autoComplete,
				"Ctrl-Space": _Elements.autoComplete
			},
			indentUnit: 4,
			tabSize:4,
			indentWithTabs: true
        });

		Structr.resize();

		dialogBtn.append('<button id="editorSave" disabled="disabled" class="disabled">Save</button>');
		dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');

		// Experimental speech recognition, works only in Chrome 25+
		if (typeof(webkitSpeechRecognition) === 'function') {

			dialogBox.append('<button class="speechToText"><img src="' + _Icons.microphone_icon + '"></button>');
			var speechBtn = $('.speechToText', dialogBox);

			_Speech.init(speechBtn, function(interim, finalResult) {
				//console.log('Interim:', interim);
				//console.log('Final:', finalResult);

				if (_Speech.isCommand('save', interim)) {
					//console.log('Save command detected');
					dialogSaveButton.click();
				} else if (_Speech.isCommand('saveAndClose', interim)) {
					//console.log('Save and close command detected');
					_Speech.toggleStartStop(speechBtn, function() {
						$('#saveAndClose', dialogBtn).click();
					});
				} else if (_Speech.isCommand('close', interim)) {
					//console.log('Close command detected');
					_Speech.toggleStartStop(speechBtn, function() {
						dialogCancelButton.click();
					});
				} else if (_Speech.isCommand('stop', interim)) {
					_Speech.toggleStartStop(speechBtn, function() {
						//
					});
				} else if (_Speech.isCommand('clearAll', interim)) {
					editor.setValue('');
					editor.focus();
					editor.execCommand('goDocEnd');
				} else if (_Speech.isCommand('deleteLastParagraph', interim)) {
					var text = editor.getValue();
					editor.setValue(text.substring(0, text.lastIndexOf('\n')));
					editor.focus();
					editor.execCommand('goDocEnd');
				} else if (_Speech.isCommand('deleteLastSentence', interim)) {
					var text = editor.getValue();
					editor.setValue(text.substring(0, text.lastIndexOf('.')+1));
					editor.focus();
					editor.execCommand('goDocEnd');
				} else if (_Speech.isCommand('deleteLastWord', interim)) {
					var text = editor.getValue();
					editor.setValue(text.substring(0, text.lastIndexOf(' ')));
					editor.focus();
					editor.execCommand('goDocEnd');
				} else if (_Speech.isCommand('deleteLine', interim)) {
					editor.execCommand('deleteLine');
				} else if (_Speech.isCommand('deleteLineLeft', interim)) {
					editor.execCommand('deleteLineLeft');
				} else if (_Speech.isCommand('deleteLineRight', interim)) {
					editor.execCommand('killLine');

				} else if (_Speech.isCommand('lineUp', interim)) {
					editor.execCommand('goLineUp');
				} else if (_Speech.isCommand('lineDown', interim)) {
					editor.execCommand('goLineDown');
				} else if (_Speech.isCommand('wordLeft', interim)) {
					editor.execCommand('goWordLeft');
				} else if (_Speech.isCommand('wordRight', interim)) {
					editor.execCommand('goWordRight');
				} else if (_Speech.isCommand('left', interim)) {
					editor.execCommand('goCharLeft');
				} else if (_Speech.isCommand('right', interim)) {
					editor.execCommand('goCharRight');


				} else {
					//editor.setValue(editor.getValue() + interim);

					editor.replaceSelection(interim);

					//editor.focus();
					//editor.execCommand('goDocEnd');
				}

			});
		}

		dialogSaveButton = $('#editorSave', dialogBtn);
		var saveAndClose = $('#saveAndClose', dialogBtn);

		saveAndClose.on('click', function(e) {
			e.stopPropagation();
			dialogSaveButton.click();
			setTimeout(function() {
				dialogSaveButton.remove();
				saveAndClose.remove();
				dialogCancelButton.click();
			}, 500);
		});

		editor.on('change', function(cm, change) {

			if (text === editor.getValue()) {
				dialogSaveButton.prop("disabled", true).addClass('disabled');
				saveAndClose.prop("disabled", true).addClass('disabled');
			} else {
				dialogSaveButton.prop("disabled", false).removeClass('disabled');
				saveAndClose.prop("disabled", false).removeClass('disabled');
			}

			$('#chars').text(editor.getValue().length);
			$('#words').text(editor.getValue().match(/\S+/g) !== null ? editor.getValue().match(/\S+/g).length : 0);
		});

		var scrollInfo = JSON.parse(LSWrapper.getItem(scrollInfoKey + '_' + entity.id));
		if (scrollInfo) {
			editor.scrollTo(scrollInfo.left, scrollInfo.top);
		}

		editor.on('scroll', function() {
			var scrollInfo = editor.getScrollInfo();
			LSWrapper.setItem(scrollInfoKey + '_' + entity.id, JSON.stringify(scrollInfo));
		});

		dialogSaveButton.on('click', function(e) {
			e.stopPropagation();

			text1 = text;
			text2 = editor.getValue();

			if (!text1)
				text1 = '';
			if (!text2)
				text2 = '';

//			var contentNode = Structr.node(entity.id)[0];
//			_Logger.consoleLog('Element', contentNode);
//			_Logger.consoleLog('text1', text1);
//			_Logger.consoleLog('text2', text2);

			if (text1 === text2) {
				return;
			}

			Command.patch(entity.id, text1, text2, function() {
				dialogMsg.html('<div class="infoBox success">Content saved.</div>');
				$('.infoBox', dialogMsg).delay(2000).fadeOut(200);
				_Pages.reloadPreviews();
				dialogSaveButton.prop("disabled", true).addClass('disabled');
				saveAndClose.prop("disabled", true).addClass('disabled');
				Command.getProperty(entity.id, 'content', function(newText) {
					text = newText;
				});
			});

		});

		//_Entities.appendBooleanSwitch(dialogMeta, entity, 'editable', 'Editable', 'If enabled, data fields in this content element are editable in edit mode.');

		var values = ['text/plain', 'text/html', 'text/xml', 'text/css', 'text/javascript', 'text/markdown', 'text/textile', 'text/mediawiki', 'text/tracwiki', 'text/confluence', 'text/asciidoc'];

		dialogMeta.append('<label for="contentTypeSelect">Content-Type:</label> <select class="contentType_" id="contentTypeSelect"></select>');
		var select = $('#contentTypeSelect', dialogMeta);
		$.each(values, function(i, type) {
			select.append('<option ' + (type === entity.contentType ? 'selected' : '') + ' value="' + type + '">' + type + '</option>');
		});
		select.on('change', function() {
			contentType = select.val();
			entity.setProperty('contentType', contentType, false, function() {
				blinkGreen(select);
				_Pages.reloadPreviews();
			});
		});

		dialogMeta.append('<span class="editor-info"><label for="lineWrapping">Line Wrapping:</label> <input id="lineWrapping" type="checkbox"' + (lineWrapping ? ' checked="checked" ' : '') + '></span>');
		$('#lineWrapping').on('change', function() {
			var inp = $(this);
			if  (inp.is(':checked')) {
				LSWrapper.setItem(lineWrappingKey, "1");
				editor.setOption('lineWrapping', true);
			} else {
				LSWrapper.removeItem(lineWrappingKey);
				editor.setOption('lineWrapping', false);
			}
			blinkGreen(inp.parent());
			editor.refresh();
		});

		dialogMeta.append('<span class="editor-info">Characters: <span id="chars">' + editor.getValue().length + '</span></span>');
		dialogMeta.append('<span class="editor-info">Words: <span id="words">' + (editor.getValue().match(/\S+/g) !== null ? editor.getValue().match(/\S+/g).length : 0) + '</span></span>');

		editor.id = entity.id;

		editor.focus();
		//editor.execCommand('goDocEnd');

	}};