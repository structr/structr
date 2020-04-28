/*
 * Copyright (C) 2010-2020 Structr GmbH
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
var contents, editor, contentType, currentEntity;

$(function() {

	// disable default contextmenu on our contextmenu *once*, so it doesnt fire/register once per element
	$(document).on("contextmenu", '#menu-area', function(e) {
		e.stopPropagation();
		e.preventDefault();
	});

	$(document).on("click", '#add-child-dialog #inherit-visibility-flags' , function (e) {
		e.preventDefault();
		e.stopPropagation();
	});

});

var _Elements = {
	inheritVisibilityFlagsKey: 'inheritVisibilityFlags_' + port,
	elementNames: [
		// The root element
		'html',
		// Document metadata
		'head', 'title', 'base', 'link', 'meta', 'style',
		// Scripting
		'script', 'noscript',
		// Sections
		'body', 'section', 'nav', 'article', 'aside', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hgroup', 'header', 'footer', 'address', 'main',
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
			elements: ['body', 'section', 'nav', 'article', 'aside', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hgroup', 'header', 'footer', 'address', 'main']
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
			elements: ['div'],
			attrs: ['style']
		},
		{
			elements: ['input', 'textarea'],
			attrs: ['name', 'type', 'checked', 'selected', 'value', 'size', 'multiple', 'disabled', 'autofocus', 'placeholder', 'style', 'rows', 'cols'],
			focus: 'type'
		},
		{
			elements: ['button'],
			attrs: ['name', 'type', 'checked', 'selected', 'value', 'size', 'multiple', 'disabled', 'autofocus', 'placeholder', 'onclick', 'style', 'title']
		},
		{
			elements: ['select', 'option'],
			attrs: ['name', 'type', 'checked', 'selected', 'value', 'size', 'multiple', 'disabled', 'autofocus', 'placeholder', 'style']
		},
		{
			elements: ['optgroup'],
			attrs: ['label', 'disabled', 'style'],
			focus: 'label'
		},
		{
			elements: ['form'],
			attrs: ['action', 'method', 'style']
		},
		{
			elements: ['img'],
			attrs: ['alt', 'title', 'src', 'style'],
			focus: 'src'
		},
		{
			elements: ['script', 'img', 'object'],
			attrs: ['type', 'rel', 'href', 'media', 'src', 'style'],
			focus: 'src'
		},
		{
			elements: ['link'],
			attrs: ['type', 'rel', 'href', 'style'],
			focus: 'href'
		},
		{
			elements: ['a'],
			attrs: ['type', 'rel', 'href', 'target', 'style'],
			focus: 'href'
		},
		{
			elements: ['td', 'th'],
			attrs: ['colspan', 'rowspan', 'style']
		},
		{
			elements: ['label'],
			attrs: ['for', 'form', 'style'],
			focus: 'for'
		},
		{
			elements: ['style'],
			attrs: ['type', 'media', 'scoped'],
			focus: 'type'
		},
		{
			elements: ['iframe'],
			attrs: ['src', 'width', 'height', 'style'],
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
			elements: ['g', '|', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'head', 'header', 'hgroup', 'hr']
		},
		{
			name: 'i-k',
			elements: ['i', 'iframe', 'img', 'input', 'ins', '|', 'kbd', 'keygen']
		},
		{
			name: 'l-m',
			elements: ['label', 'legend', 'li', 'link', '|', 'main', 'map', 'mark', 'menu', 'meta', 'meter']
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
		},
		'|',
		'custom'
	],
	suggestedElements: {
		html     : [ 'head', 'body' ],
		body     : [ 'header', 'main', 'footer' ],
		head     : [ 'title', 'style', 'base', 'link', 'meta', 'script', 'noscript' ],
		table    : [ 'thead', 'tbody', 'tr', 'tfoot', 'caption', 'colgroup' ],
		colgroup : [ 'col' ],
		thead    : [ 'tr' ],
		tbody    : [ 'tr' ],
		tfoot    : [ 'tr' ],
		tr       : [ 'th', 'td' ],
		ul       : [ 'li' ],
		ol       : [ 'li' ],
		dir      : [ 'li' ],
		dl       : [ 'dt', 'dd' ],
		select   : [ 'option', 'optgroup' ],
		optgroup : [ 'option' ],
		form     : [ 'input', 'textarea', 'select', 'button', 'label', 'fieldset' ],
		fieldset : [ 'legend', 'input', 'textarea', 'select', 'button', 'label', 'fieldset' ],
		figure   : [ 'img', 'figcaption' ],
		frameset : [ 'frame' , 'noframes' ],
		map      : [ 'area' ],
		nav      : [ 'a' ],
		object   : [ 'param' ],
		details  : [ 'summary' ],
		video    : [ 'source', 'track' ],
		audio    : [ 'source' ]
	},
	selectedEntity: undefined,
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

		componentsSlideout.append('<div class="" id="newComponentDropzone"><div class="new-component-info"><i class="active ' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" /><i class="inactive ' + _Icons.getFullSpriteClass(_Icons.add_grey_icon) + '" /> Drop element here to create new shared component</div></div>');
		let newComponentDropzone = $('#newComponentDropzone', componentsSlideout);

		componentsSlideout.append('<div class="ver-scrollable" id="componentsArea"></div>');
		components = $('#componentsArea', componentsSlideout);

		newComponentDropzone.droppable({
			drop: function(e, ui) {
				e.preventDefault();
				e.stopPropagation();

				if (ui.draggable.hasClass('widget')) {
					// special treatment for widgets dragged to the shared components area

				} else {
					if (!shadowPage) {
						// Create shadow page if not existing
						Command.getByType('ShadowDocument', 1, 1, null, null, null, true, function(entities) {
							shadowPage = entities[0];
							_Elements.createComponent(ui);
						});
					} else {
						_Elements.createComponent(ui);
					}
				}
			}
		});

		_Dragndrop.makeSortable(components);

		Command.listComponents(1000, 1, 'name', 'asc', function(result) {

			_Elements.appendEntitiesToDOMElement(result, components);
			Structr.refreshPositionsForCurrentlyActiveSortable();
		});
	},
	createComponent: function(el) {

		dropBlocked = true;
		var sourceEl = $(el.draggable);
		var sourceId = Structr.getId(sourceEl);
		if (!sourceId) return false;
		var obj = StructrModel.obj(sourceId);
		if (obj && obj.syncedNodesIds && obj.syncedNodesIds.length || sourceEl.parent().attr('id') === 'componentsArea') {
			_Logger.log(_LogType.ELEMENTS, 'component dropped on components area, aborting');
			return false;
		}
		Command.createComponent(sourceId);
		dropBlocked = false;

	},
	clearUnattachedNodes: function() {
		elementsSlideout.find(':not(.compTab)').remove();
	},
	reloadUnattachedNodes: function() {

		if (elementsSlideout.hasClass('open')) {

			_Elements.clearUnattachedNodes();

			elementsSlideout.append('<div class="ver-scrollable" id="elementsArea"></div>');
			elements = $('#elementsArea', elementsSlideout);

			elements.before('<button class="btn action disabled" id="delete-all-unattached-nodes" disabled>Loading </button>');

			var btn = $('#delete-all-unattached-nodes');
			Structr.loaderIcon(btn, {
				"max-height": "100%",
				"height": "initial",
				"width": "initial"
			});
			btn.on('click', function() {
				Structr.confirmation('<p>Delete all DOM elements without parent?</p>',
						function() {
							Command.deleteUnattachedNodes();
							$.unblockUI({
								fadeOut: 25
							});
							Structr.closeSlideOuts([elementsSlideout], _Pages.activeTabRightKey, _Pages.slideoutClosedCallback);
						});
			});

			_Dragndrop.makeSortable(elements);

			Command.listUnattachedNodes(1000, 1, 'name', 'asc', function(result) {

				var count = result.length;
				if (count > 0) {
					btn.text('Delete all (' + count + ')');
					btn.removeClass('disabled');
					btn.prop('disabled', false);
				} else {
					btn.text('No unused elements');
				}

				_Elements.appendEntitiesToDOMElement(result, elements);
			});
		}

	},
	appendEntitiesToDOMElement: function (entities, domElement) {

		entities.forEach(function(entity) {

			if (entity) {

				var obj = StructrModel.create(entity, null, false);
				var el = (obj.isContent) ? _Elements.appendContentElement(obj, domElement, true) : _Pages.appendElementElement(obj, domElement, true);

				if (Structr.isExpanded(entity.id)) {
					_Entities.ensureExpanded(el);
				}
			}

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

		var parent;
		if (refNodeIsParent) {
			parent = refNode;
		} else {
			parent = entity.parent && entity.parent.id ? Structr.node(entity.parent.id) : elements;
		}

		_Logger.log(_LogType.ELEMENTS, 'appendElementElement parent, refNode, refNodeIsParent', parent, refNode, refNodeIsParent);
		if (!parent) {
			return false;
		}

		var hasChildren = entity.childrenIds && entity.childrenIds.length;

		// store active nodes in special place..
		var isActiveNode = entity.isActiveNode();

		let elementClasses = ['node', 'element'];
		elementClasses.push(isActiveNode ? 'activeNode' : 'staticNode');
		if (_Elements.isEntitySelected(entity)) {
			elementClasses.push('nodeSelectedFromContextMenu');
		}
		if (entity.tag === 'html') {
			elementClasses.push('html_element');
		}
		if (entity.hidden === true) {
			elementClasses.push('is-hidden');
		}

		_Entities.ensureExpanded(parent);

		var id = entity.id;

		var html = '<div id="id_' + id + '" class="' + elementClasses.join(' ') + '"></div>';

		if (refNode && !refNodeIsParent) {
			refNode.before(html);
		} else {
			parent.append(html);
		}

		var div = Structr.node(id);

		_Logger.log(_LogType.ELEMENTS, 'Element appended (div, parent)', div, parent);

		if (!div) {
			return false;
		}

		var displayName = getElementDisplayName(entity);

		var icon = _Elements.getElementIcon(entity);

		div.append('<i class="typeIcon ' + _Icons.getFullSpriteClass(icon) + '" />'
			+ '<b title="' + displayName + '" class="tag_ name_">' + fitStringToWidth(displayName, 200) + '</b><span class="id">' + entity.id + '</span>'
			+ _Elements.classIdString(entity._html_id, entity._html_class));

		div.append('<i title="Clone ' + displayName + ' element ' + entity.id + '\" class="clone_icon button ' + _Icons.getFullSpriteClass(_Icons.clone_icon) + '" />');
		$('.clone_icon', div).on('click', function(e) {
			e.stopPropagation();
			_Logger.log(_LogType.ELEMENTS, 'Cloning node (div, parent)', entity, entity.parent);
			Command.cloneNode(entity.id, (entity.parent ? entity.parent.id : null), true);
		});

		_Elements.enableContextMenuOnElement(div, entity);

		_Entities.appendExpandIcon(div, entity, hasChildren);

		_Entities.appendAccessControlIcon(div, entity);
		div.append('<i title="Delete ' + displayName + ' element ' + entity.id + '" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />');
		$('.delete_icon', div).on('click', function(e) {
			e.stopPropagation();
			_Entities.deleteNode(this, entity, true, function() {
				var synced = entity.syncedNodesIds;
				if (synced && synced.length) {
					synced.forEach(function(id) {
						var el = Structr.node(id);
						if (el && el.children && el.children.length) {
							var newSpriteClass = _Icons.getSpriteClassOnly(_Icons.brick_icon);
							el.children('i.typeIcon').each(function (i, el) {
								_Icons.updateSpriteClassTo(el, newSpriteClass);
							});
						}
					});
				}
			});
		});

		_Entities.setMouseOver(div, undefined, ((entity.syncedNodesIds&&entity.syncedNodesIds.length)?entity.syncedNodesIds:[entity.sharedComponentId]));

		if (!hasChildren && !entity.sharedComponentId) {
			_Entities.appendEditSourceIcon(div, entity);
		}

		_Entities.appendEditPropertiesIcon(div, entity);

		if (entity.tag === 'a' || entity.tag === 'link' || entity.tag === 'script' || entity.tag === 'img' || entity.tag === 'video' || entity.tag === 'object') {

			div.append('<i title="Edit Link" class="link_icon button ' + _Icons.getFullSpriteClass(_Icons.link_icon) + '" />');
			if (entity.linkableId) {

				Command.get(entity.linkableId, 'id,type,name,isFile,isPage', function(linkedEntity) {

					div.append('<span class="linkable">' + linkedEntity.name + '</span>');

					if (linkedEntity.isFile) {

						$('.linkable', div).on('click', function(e) {
							e.stopPropagation();

							var file = {'name': linkedEntity.name, 'id': linkedEntity.id};

							Structr.dialog('Edit ' + file.name, function() {
								_Logger.log(_LogType.ELEMENTS, 'content saved');
							}, function() {
								_Logger.log(_LogType.ELEMENTS, 'cancelled');
							});
							_Files.editContent(this, file, $('#dialogBox .dialogText'));
						});
					}
				});
			}

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

							pagesToLink.append('<div class="node page ' + page.id + '_"><i class="' + _Icons.getFullSpriteClass(_Icons.page_icon) + '" /><b title="' + page.name + '" class="name_">' + page.name + '</b></div>');

							var div = $('.' + page.id + '_', pagesToLink);

							_Elements.handleLinkableElement(div, entity, page);

						});

					});

					dialog.append('<h3>Files</h3><div class="linkBox" id="foldersToLink"></div><div class="linkBox" id="filesToLink"></div>');

					var filesToLink = $('#filesToLink');
					var foldersToLink = $('#foldersToLink');

					_Pager.initPager('folders-to-link', 'Folder', 1, 25);
					_Pager.initFilters('folders-to-link', 'Folder', { hasParent: false });
					var linkFolderPager = _Pager.addPager('folders-to-link', foldersToLink, true, 'Folder', 'ui', function(folders) {

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

					_Pager.initPager('files-to-link', 'File', 1, 25);
					var linkFilesPager = _Pager.addPager('files-to-link', filesToLink, true, 'File', 'ui', function(files) {

						files.forEach(function(file) {

							filesToLink.append('<div class="node file ' + file.id + '_"><i class="fa ' + _Icons.getFileIconClass(file) + '"></i> '
									+ '<b title="' + file.name + '" class="name_">' + file.name + '</b></div>');

							var div = $('.' + file.id + '_', filesToLink);

							_Elements.handleLinkableElement(div, entity, file);

						});

					});

					linkFilesPager.pager.append('<input type="checkbox" class="filter" data-attribute="hasParent" hidden>');
					linkFilesPager.activateFilterElements();

				}

				if (entity.tag === 'img' || entity.tag === 'link' || entity.tag === 'a') {

					dialog.append('<h3>Images</h3><div class="linkBox" id="imagesToLink"></div>');

					var imagesToLink = $('#imagesToLink');

					_Pager.initPager('images-to-link', 'Image', 1, 25);
					_Pager.addPager('images-to-link', imagesToLink, false, 'Image', 'ui', function(images) {

						images.forEach(function(image) {

							imagesToLink.append('<div class="node file ' + image.id + '_">' + _Icons.getImageOrIcon(image) + '<b title="' + image.name + '" class="name_">' + image.name + '</b></div>');

							var div = $('.' + image.id + '_', imagesToLink);

							_Elements.handleLinkableElement(div, entity, image);

						});

					});

				}

			});
		}
		return div;
	},
	getElementIcon:function(element) {
		var isComponent = element.sharedComponentId || (element.syncedNodesIds && element.syncedNodesIds.length);
		var isActiveNode = (typeof element.isActiveNode === "function") ? element.isActiveNode() : false;

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
					Command.get(subFolder.id, 'id,name,files,folders', function(node) {
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

			Command.get(f.id, 'id,name,contentType,linkingElements', function(file) {

				$('.' + folder.id + '_').append('<div class="clear"></div><div class="node file sub ' + file.id + '_"><i class="fa ' + _Icons.getFileIconClass(file) + '"></i> '
						+ '<b title="' + file.name + '" class="name_">' + file.name + '</b></div>');

				var div = $('.' + file.id + '_');

				_Elements.handleLinkableElement(div, entity, file);

			});

		});
	},
	handleLinkableElement: function (div, entityToLinkTo, linkableObject) {

		if (isIn(entityToLinkTo.id, linkableObject.linkingElementsIds)) {
			div.addClass('nodeActive');
		}

		div.on('click', function(event) {

			event.stopPropagation();

			if (div.hasClass('nodeActive')) {
				Command.setProperty(entityToLinkTo.id, 'linkableId', null);
			} else {

				var attrName = (entityToLinkTo.type === 'Link') ? '_html_href' : '_html_src';

				Command.getProperty(entityToLinkTo.id, attrName, function(val) {
					if (!val || val === '') {
						Command.setProperty(entityToLinkTo.id, attrName, '${link.path}', null);
					}
				});

				Command.link(entityToLinkTo.id, linkableObject.id);
			}

			_Entities.reloadChildren(entityToLinkTo.parent.id);

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

	},
	enableContextMenuOnElement: function (div, entity) {

		_Elements.disableBrowserContextMenuOnElement(div);

		$(div).on('mouseup', function(e) {
			if (e.button !== 2) {
				return;
			}
			e.stopPropagation();

			_Elements.activateContextMenu(e, div, entity);
		});

	},
	disableBrowserContextMenuOnElement: function (div) {

		$(div).on("contextmenu", function(e) {
			e.stopPropagation();
			e.preventDefault();
		});

	},
	activateContextMenu:function(e, div, entity) {

		var menuElements = _Elements.getContextMenuElements(div, entity);

		var menuHeight = 24 * menuElements.length;

		var leftOrRight = 'left';
		var topOrBottom = 'top';
		var x = (e.clientX - 8);
		var y = div.offset().top;

		if (e.pageX > ($(window).width() / 2)) {
			leftOrRight = 'right';
		}

		if (e.pageY > ($(window).height() - menuHeight)) {
			y -= 20 + menuHeight - ($(window).height() - e.pageY);
		}

		var cssPositionClasses = leftOrRight + ' ' + topOrBottom;

		_Elements.removeContextMenu();
		div.addClass('contextMenuActive');
		$('#menu-area').append('<div id="add-child-dialog"></div>');

		$('#add-child-dialog').css({
			left: x + 'px',
			top: y + 'px'
		});

		var registerContextMenuItemClickHandler = function (el, contextMenuItem) {

			el.on('mouseup', function(e) {
				e.stopPropagation();

				var preventClose = true;

				if (contextMenuItem.clickHandler && (typeof contextMenuItem.clickHandler === 'function')) {
					preventClose = contextMenuItem.clickHandler($(this), contextMenuItem);
				}

				if (!preventClose) {
					_Elements.removeContextMenu();
				}
			});

		};

		var registerPlaintextContextMenuItemHandler = function (el, itemText, forcedClickHandler) {

			el.on('mouseup', function (e) {
				e.stopPropagation();

				if (forcedClickHandler && (typeof forcedClickHandler === 'function')) {
					forcedClickHandler(itemText);
				} else {
					var pageId = (entity.type === 'Page') ? entity.id : entity.pageId;
					var tagName = (itemText === 'content') ? null : itemText;

					Command.createAndAppendDOMNode(pageId, entity.id, tagName, _Dragndrop.getAdditionalDataForElementCreation(tagName), _Elements.isInheritVisibililtyFlagsChecked());
				}

				_Elements.removeContextMenu();
			});

		};

		var addContextMenuElements = function (ul, element, hidden, forcedClickHandler, prepend) {

			if (hidden) {
				ul.addClass('hidden');
			}

			if (Object.prototype.toString.call(element) === '[object Array]') {

				element.forEach(function (el) {
					addContextMenuElements(ul, el, hidden, forcedClickHandler, prepend);
				});

			} else if (Object.prototype.toString.call(element) === '[object Object]') {

				if (element.visible !== undefined && element.visible === false) {
					return;
				}

				var menuEntry = $('<li class="element-group-switch">' + element.name + '</li>');
				registerContextMenuItemClickHandler(menuEntry, element);
				if (prepend) {
					ul.prepend(menuEntry);
				} else {
					ul.append(menuEntry);
				}

				if (element.elements) {
					menuEntry.append('<i class="fa fa-caret-right pull-right"></i>');

					var subListElement = $('<ul class="element-group ' + cssPositionClasses + '"></ul>');
					menuEntry.append(subListElement);
					addContextMenuElements(subListElement, element.elements, true, (forcedClickHandler ? forcedClickHandler : element.forcedClickHandler), prepend);
				}

			} else if (Object.prototype.toString.call(element) === '[object String]') {

				if (element === '|') {

					if (prepend) {
						ul.prepend('<hr />');
					} else {
						ul.append('<hr />');
					}

				} else {

					var listElement = $('<li>' + element + '</li>');
					registerPlaintextContextMenuItemHandler(listElement, element, forcedClickHandler, prepend);

					if (prepend) {
						ul.prepend(listElement);
					} else {
						ul.append(listElement);
					}

				}

			}
		};

		var updateMenuGroupVisibility = function() {

			$('.element-group-switch').hover(function() {
				$(this).children('.element-group').removeClass('hidden');
			}, function() {
				$(this).children('.element-group').addClass('hidden');
			});
		};

		var mainMenuList = $('<ul class="element-group ' + cssPositionClasses + '"></ul>');
		$('#add-child-dialog').append(mainMenuList);
		menuElements.forEach(function (mainEl) {
			addContextMenuElements(mainMenuList, mainEl, false);
		});

		// prepend suggested elements if present
		_Elements.getSuggestedWidgets(entity, function(data) {

			if (data && data.length) {

				addContextMenuElements(mainMenuList, [ '|', { name: 'Suggested Widgets', elements: data } ], false, undefined, true);
				updateMenuGroupVisibility();
			}
		});

		_Elements.updateVisibilityInheritanceCheckbox();
		updateMenuGroupVisibility();
	},
	updateVisibilityInheritanceCheckbox: function() {
		var checked = LSWrapper.getItem(_Elements.inheritVisibilityFlagsKey) || false;

		if (checked === true) {
			$('#add-child-dialog #inherit-visibility-flags').prop('checked', checked);
		}
	},
	isInheritVisibililtyFlagsChecked: function () {
		return $('#add-child-dialog #inherit-visibility-flags').prop('checked');
	},
	removeContextMenu: function() {
		$('#add-child-dialog').remove();
		$('.contextMenuActive').removeClass('contextMenuActive');
	},
	getContextMenuElements: function (div, entity) {

		var isPage      = (entity.type === 'Page');
		var isContent   = (entity.type === 'Content');
		var hasChildren = (entity.children && entity.children.length > 0);

		var handleInsertHTMLAction = function (itemText) {
			var pageId = isPage ? entity.id : entity.pageId;
			var tagName = (itemText === 'content') ? null : itemText;

			Command.createAndAppendDOMNode(pageId, entity.id, tagName, _Dragndrop.getAdditionalDataForElementCreation(tagName, entity.tag), _Elements.isInheritVisibililtyFlagsChecked());
		};

		var handleWrapInHTMLAction = function (itemText) {

			Command.wrapDOMNodeInNewDOMNode(entity.pageId, entity.id, itemText, {}, _Elements.isInheritVisibililtyFlagsChecked());
		};

		var elements = [];

		var appendSeparator = function () {
			if (elements[elements.length - 1] !== '|') {
				elements.push('|');
			}
		};

		if (!isContent) {
			elements.push({
				name: 'Insert HTML element',
				elements: !isPage ? _Elements.sortedElementGroups : ['html'],
				forcedClickHandler: handleInsertHTMLAction
			});
			elements.push({
				name: 'Insert content element',
				elements: !isPage ? ['content', 'template'] : ['template'],
				forcedClickHandler: handleInsertHTMLAction
			});

			if (_Elements.suggestedElements[entity.tag]) {
				elements.push({
					name: 'Suggested HTML element',
					elements: _Elements.suggestedElements[entity.tag],
					forcedClickHandler: handleInsertHTMLAction
				});
			}
		}

		if (!isPage && !isContent) {
			elements.push({
				name: 'Insert div element',
				clickHandler: function() {
					Command.createAndAppendDOMNode(entity.pageId, entity.id, 'div', _Dragndrop.getAdditionalDataForElementCreation('div'), _Elements.isInheritVisibililtyFlagsChecked());
					return false;
				}
			});
		}

		appendSeparator();

		if (!isPage && entity.parent !== null && entity.parent.type !== 'Page') {

			elements.push({
				name: 'Wrap element in...',
				elements: [
					{
						name: '... HTML element',
						elements: _Elements.sortedElementGroups,
						forcedClickHandler: handleWrapInHTMLAction
					},
					{
						name: '... Template element',
						clickHandler: function () {
							handleWrapInHTMLAction('template');
						}
					},
					{
						name: '... div element',
						clickHandler: function () {
							handleWrapInHTMLAction('div');
						}
					}
				]
			});
		}


		if (!isPage) {

			appendSeparator();

			if (_Elements.selectedEntity && _Elements.selectedEntity.id === entity.id) {
				elements.push({
					name: 'Deselect element',
					clickHandler: function() {
						_Elements.unselectEntity();
						return false;
					}
				});
			} else {
				elements.push({
					name: 'Select element',
					clickHandler: function() {
						_Elements.selectEntity(entity);
						return false;
					}
				});
			}

			var isEntitySharedComponent = entity.sharedComponent || entity.pageId === shadowPage.id;
			if (!isEntitySharedComponent) {
				appendSeparator();

				elements.push({
					name: 'Convert to Shared Component',
					clickHandler: function() {
						Command.createComponent(entity.id);
						return false;
					}
				});
			}
		}

		if (!isContent && _Elements.selectedEntity && _Elements.selectedEntity.id !== entity.id) {

			var isSamePage = _Elements.selectedEntity.pageId === entity.pageId;
			var isThisEntityDirectParentOfSelectedEntity = (_Elements.selectedEntity.parent && _Elements.selectedEntity.parent.id === entity.id);
			var isSelectedEntityInShadowPage = _Elements.selectedEntity.pageId === shadowPage.id;
			var isSelectedEntitySharedComponent = isSelectedEntityInShadowPage && !_Elements.selectedEntity.parent;

			var isDescendantOfSelectedEntity = function (possibleDescendant) {
				if (possibleDescendant.parent) {
					if (possibleDescendant.parent.id === _Elements.selectedEntity.id) {
						return true;
					}
					return isDescendantOfSelectedEntity(StructrModel.obj(possibleDescendant.parent.id));
				}
				return false;
			};

			if (isSelectedEntitySharedComponent) {
				elements.push({
					name: 'Link shared component here',
					clickHandler: function() {
						Command.cloneComponent(_Elements.selectedEntity.id, entity.id);
						_Elements.unselectEntity();
						return false;
					}
				});

			}

			if ( !isPage || (isPage && !hasChildren && (_Elements.selectedEntity.tag === 'html' || _Elements.selectedEntity.type === 'Template')) ) {
				elements.push({
					name: 'Clone selected element here',
					clickHandler: function() {
						Command.cloneNode(_Elements.selectedEntity.id, entity.id, true);
						_Elements.unselectEntity();
						return false;
					}
				});
			}

			if (isSamePage && !isThisEntityDirectParentOfSelectedEntity && !isSelectedEntityInShadowPage && !isDescendantOfSelectedEntity(entity)) {
				elements.push({
					name: 'Move selected element here',
					clickHandler: function() {
						Command.appendChild(_Elements.selectedEntity.id, entity.id, entity.pageId);
						_Elements.unselectEntity();
						return false;
					}
				});
			}
		}

		appendSeparator();

		if (!isPage) {

			elements.push({
				name: 'Query and Data Binding',
				clickHandler: function() {
					_Entities.showProperties(entity, 'query');
					return false;
				}
			});
			elements.push({
				name: 'Edit Mode Binding',
				clickHandler: function() {
					_Entities.showProperties(entity, 'editBinding');
					return false;
				}
			});
			elements.push({
				name: 'HTML Attributes',
				clickHandler: function() {
					_Entities.showProperties(entity, '_html_');
					return false;
				}
			});

		}

		elements.push({
			name: 'Node Properties',
			clickHandler: function() {
				_Entities.showProperties(entity, 'ui');
				return false;
			}
		});

		appendSeparator();

		elements.push({
			name: 'Security',
			elements: [
				{
					name: 'Access Control and Visibility',
					clickHandler: function() {
						_Entities.showAccessControlDialog(entity);
						return false;
					}
				},
				'|',
				{
					name: 'Authenticated Users',
					elements: [
						{
							name: 'Make element visible',
							clickHandler: function() {
								Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', true, false);
								return false;
							}
						},
						{
							name: 'Make Element invisible',
							clickHandler: function() {
								Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', false, false);
								return false;
							}
						},
						'|',
						{
							name: 'Make subtree visible',
							clickHandler: function() {
								Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', true, true);
								return false;
							}
						},
						{
							name: 'Make subtree invisible',
							clickHandler: function() {
								Command.setProperty(entity.id, 'visibleToAuthenticatedUsers', false, true);
								return false;
							}
						}
					]
				},
				{
					name: 'Public Users',
					elements: [
						{
							name: 'Make element visible',
							clickHandler: function() {
								Command.setProperty(entity.id, 'visibleToPublicUsers', true, false);
								return false;
							}
						},
						{
							name: 'Make element invisible',
							clickHandler: function() {
								Command.setProperty(entity.id, 'visibleToPublicUsers', false, false);
								return false;
							}
						},
						'|',
						{
							name: 'Make subtree visible',
							clickHandler: function() {
								Command.setProperty(entity.id, 'visibleToPublicUsers', true, true);
								return false;
							}
						},
						{
							name: 'Make subtree invisible',
							clickHandler: function() {
								Command.setProperty(entity.id, 'visibleToPublicUsers', false, true);
								return false;
							}
						}
					]
				}
			]
		});

		appendSeparator();

		if (!isContent && hasChildren) {

			elements.push({
				name: 'Expand / Collapse',
				elements: [
					{
						name: 'Expand subtree',
						clickHandler: function() {
							$(div).find('.node').each(function(i, el) {
								if (!_Entities.isExpanded(el)) {
									_Entities.toggleElement(el);
								}
							});
							if (!_Entities.isExpanded(div)) {
								_Entities.toggleElement(div);
							}
							return false;
						}
					},
					{
						name: 'Expand subtree recursively',
						clickHandler: function() {
							_Entities.expandRecursively([entity.id]);
							return false;
						}
					},
					{
						name: 'Collapse subtree',
						clickHandler: function() {
							$(div).find('.node').each(function(i, el) {
								if (_Entities.isExpanded(el)) {
									_Entities.toggleElement(el);
								}
							});
							if (_Entities.isExpanded(div)) {
								_Entities.toggleElement(div);
							}
							return false;
						}
					}
				]
			});
		}

		appendSeparator();

		if (!isContent) {

			elements.push({
				name: '<input type="checkbox" id="inherit-visibility-flags">Inherit Visibility Flags',
				stayOpen: true,
				clickHandler: function(el) {
					var checkbox = el.find('input');
					var wasChecked = checkbox.prop('checked');
					checkbox.prop('checked', !wasChecked);
					LSWrapper.setItem(_Elements.inheritVisibilityFlagsKey, !wasChecked);
					return true;
				}
			});
		}

		appendSeparator();

		return elements;
	},
	selectEntity: function (entity) {

		_Elements.unselectEntity();

		_Elements.selectedEntity = entity;

		var node = Structr.node(_Elements.selectedEntity.id);
		if (node) {
			node.addClass('nodeSelectedFromContextMenu');
		}
	},
	unselectEntity: function () {
		if (_Elements.selectedEntity) {

			var node = Structr.node(_Elements.selectedEntity.id);
			if (node) {
				node.removeClass('nodeSelectedFromContextMenu');
			}

			_Elements.selectedEntity = undefined;
		}
	},
	isEntitySelected: function (entity) {
		return _Elements.selectedEntity && _Elements.selectedEntity.id === entity.id;
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

		if (!parent) {
			return false;
		}

		var isActiveNode = entity.isActiveNode();
		var isTemplate = (entity.type === 'Template');

		var name = entity.name;
		var displayName = getElementDisplayName(entity);

		var icon = _Elements.getContentIcon(entity);
		var html = '<div id="id_' + entity.id + '" class="node content ' + (isActiveNode ? ' activeNode' : 'staticNode') + (_Elements.isEntitySelected(entity) ? ' nodeSelectedFromContextMenu' : '') + '">'
				+ '<i class="typeIcon ' + _Icons.getFullSpriteClass(icon) + ' typeIcon-nochildren" />'
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

		div.append('<i title="Clone content node ' + entity.id + '" class="clone_icon button ' + _Icons.getFullSpriteClass(_Icons.clone_icon) + '" />');
		$('.clone_icon', div).on('click', function(e) {
			e.stopPropagation();
			_Logger.log(_LogType.ELEMENTS, 'Cloning node (div, parent)', entity, entity.parent);
			Command.cloneNode(entity.id, (entity.parent ? entity.parent.id : null), true);
		});

		_Elements.enableContextMenuOnElement(div, entity);

		div.append('<i title="Delete content \'' + (entity.name ? entity.name : entity.id) + '\'" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.delete_content_icon) + '" />');
		$('.delete_icon', div).on('click', function(e) {
			e.stopPropagation();
			_Entities.deleteNode(this, entity);
		});

		_Elements.appendEditContentIcon(div, entity);

		$('.content_', div).on('click', function(e) {
			e.stopPropagation();
			_Elements.openEditContentDialog(this, entity);
			return false;
		});

		_Entities.setMouseOver(div, undefined, ((entity.syncedNodesIds && entity.syncedNodesIds.length) ? entity.syncedNodesIds : [entity.sharedComponentId]));

		_Entities.appendEditPropertiesIcon(div, entity);

		return div;
	},
	getContentIcon:function(content) {
		var isComment = (content.type === 'Comment');
		var isTemplate = (content.type === 'Template');
		var isComponent = content.sharedComponentId || (content.syncedNodesIds && content.syncedNodesIds.length);
		var isActiveNode = content.isActiveNode();

		return isComment ? _Icons.comment_icon : ((isTemplate && isComponent) ? _Icons.comp_templ_icon : (isTemplate ? (isActiveNode ? _Icons.active_template_icon : _Icons.template_icon) : (isComponent ? _Icons.active_content_icon : (isActiveNode ? _Icons.active_content_icon : _Icons.content_icon))));
	},
	appendEditContentIcon: function(div, entity) {

		div.append('<i title="Edit Content of ' + (entity.name ? entity.name : entity.id) + '" class="edit_icon button ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" />');
		$('.edit_icon', div).on('click', function(e) {
			e.stopPropagation();
			_Elements.openEditContentDialog(this, entity);
			return false;
		});

	},
	openEditContentDialog: function(btn, entity) {
		Structr.dialog('Edit content of ' + (entity.name ? entity.name : entity.id), function() {
			_Logger.log(_LogType.CONTENTS, 'content saved');
		}, function() {
			_Logger.log(_LogType.CONTENTS, 'cancelled');
		});
		Command.get(entity.id, 'content,contentType', function(data) {
            currentEntity = entity;
			entity.contentType = data.contentType;
			_Elements.editContent(this, entity, data.content, dialogText);
		});
	},
	activateEditorMode: function(contentType) {
		let modeObj = CodeMirror.findModeByMIME(contentType);
		let mode = contentType; // default

		if (modeObj) {
			mode = modeObj.mode;
			if (mode && mode !== "null") { // findModeMIME function above returns "null" string :(
				let existingScript = $('head script[data-codemirror-mode="' + mode + '"]');
				if (!existingScript.length) {
					$('head').append('<script data-codemirror-mode="' + mode + '" src="codemirror/mode/' + mode + '/' + mode + '.js"></script>');
				}
			}
		}
	},
	editContent: function(button, entity, text, element) {

		if (Structr.isButtonDisabled(button)) {
			return;
		}

		var div = element.append('<div class="editor"></div>');
		_Logger.log(_LogType.CONTENTS, div);
		var contentBox = $('.editor', element);
		contentType = entity.contentType || 'text/plain';

		_Elements.activateEditorMode(contentType);

		var text1, text2;


		// Intitialize editor
		CodeMirror.defineMIME("text/html", "htmlmixed-structr");
		editor = CodeMirror(contentBox.get(0), Structr.getCodeMirrorSettings({
			value: text,
			mode: mode || contentType,
			lineNumbers: true,
			lineWrapping: false,
			extraKeys: {
				"Ctrl-Space": "autocomplete"
			},
			indentUnit: 4,
			tabSize: 4,
			indentWithTabs: true
		}));

		_Code.setupAutocompletion(editor, entity.id);

		Structr.resize();

		dialogBtn.append('<button id="editorSave" disabled="disabled" class="disabled">Save</button>');
		dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');

		if (entity.isFavoritable) {
			dialogMeta.append('<i title="Add to favorites" class="add-to-favorites ' + _Icons.getFullSpriteClass(_Icons.star_icon) + '" >');
			$('.add-to-favorites', dialogMeta).on('click', function() {
				Command.favorites('add', entity.id, function() {
					blinkGreen($('.add-to-favorites', dialogMeta));
				});
			});
		}


		// Experimental speech recognition, works only in Chrome 25+
		if (typeof(webkitSpeechRecognition) === 'function') {

			dialogBox.append('<button class="speechToText"><i class="' + _Icons.getFullSpriteClass(_Icons.microphone_icon) + '" /></button>');
			var speechBtn = $('.speechToText', dialogBox);

			_Speech.init(speechBtn, function(interim, finalResult) {

				if (_Speech.isCommand('save', interim)) {
					dialogSaveButton.click();
				} else if (_Speech.isCommand('saveAndClose', interim)) {
					_Speech.toggleStartStop(speechBtn, function() {
						$('#saveAndClose', dialogBtn).click();
					});
				} else if (_Speech.isCommand('close', interim)) {
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
					editor.replaceSelection(interim);
				}

			});
		}

		dialogSaveButton = $('#editorSave', dialogBtn);
		saveAndClose = $('#saveAndClose', dialogBtn);

		saveAndClose.on('click', function(e) {
			e.stopPropagation();
			dialogSaveButton.click();
			setTimeout(function() {
				dialogSaveButton = $('#editorSave', dialogBtn);
				saveAndClose = $('#saveAndClose', dialogBtn);
				dialogSaveButton.remove();
				saveAndClose.remove();
				dialogCancelButton.click();
			}, 500);
		});

		editor.on('change', function(cm, change) {

			let editorText = editor.getValue();

			if (text === editorText) {
				dialogSaveButton.prop("disabled", true).addClass('disabled');
				saveAndClose.prop("disabled", true).addClass('disabled');
			} else {
				dialogSaveButton.prop("disabled", false).removeClass('disabled');
				saveAndClose.prop("disabled", false).removeClass('disabled');
			}

			$('#chars').text(editorText.length);
			$('#words').text((editorText.match(/\S+/g) || []).length);
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

			if (text1 === text2) {
				return;
			}

			Command.patch(entity.id, text1, text2, function() {
				Structr.showAndHideInfoBoxMessage('Content saved.', 'success', 2000, 200);
				_Pages.reloadPreviews();
				dialogSaveButton.prop('disabled', true).addClass('disabled');
				saveAndClose.prop('disabled', true).addClass('disabled');
				Command.getProperty(entity.id, 'content', function(newText) {
					text = newText;
				});
			});

		});

		var values = ['text/plain', 'text/html', 'text/xml', 'text/css', 'text/javascript', 'text/markdown', 'text/textile', 'text/mediawiki', 'text/tracwiki', 'text/confluence', 'text/asciidoc'];

		dialogMeta.append('<label for="contentTypeSelect">Content-Type:</label> <select class="contentType_" id="contentTypeSelect"></select>');
		var select = $('#contentTypeSelect', dialogMeta);
		$.each(values, function(i, type) {
			select.append('<option ' + (type === entity.contentType ? 'selected' : '') + ' value="' + type + '">' + type + '</option>');
		});
		select.on('change', function() {
			contentType = select.val();
			_Elements.activateEditorMode(contentType);
			editor.setOption('mode', contentType);

			entity.setProperty('contentType', contentType, false, function() {
				blinkGreen(select);
				_Pages.reloadPreviews();
			});
		});

		dialogMeta.append('<span class="editor-info"><label for="lineWrapping">Line Wrapping:</label> <input id="lineWrapping" type="checkbox"' + (Structr.getCodeMirrorSettings().lineWrapping ? ' checked="checked" ' : '') + '></span>');
		$('#lineWrapping').off('change').on('change', function() {
			var inp = $(this);
			Structr.updateCodeMirrorOptionGlobally('lineWrapping', inp.is(':checked'));
			blinkGreen(inp.parent());
			editor.refresh();
		});

		dialogMeta.append('<span class="editor-info">Characters: <span id="chars">' + editor.getValue().length + '</span></span>');
		dialogMeta.append('<span class="editor-info">Words: <span id="words">' + (editor.getValue().match(/\S+/g) !== null ? editor.getValue().match(/\S+/g).length : 0) + '</span></span>');

		editor.id = entity.id;

		editor.focus();

	},
	getSuggestedWidgets: function(entity, callback) {

		if (entity.isPage || entity.isContent) {

			// no-op

		} else {

			var clickHandler = function(element, item) {
				Command.get(item.id, undefined, function(result) {
					_Widgets.insertWidgetIntoPage(result, entity, entity.pageId);
				});
			};

			var classes = entity._html_class && entity._html_class.length ? entity._html_class.split(' ') : [];
			var htmlId  = entity._html_id;
			var tag     = entity.tag;

			Command.getSuggestions(htmlId, entity.name, tag, classes, function(result) {
				var data = [];
				result.forEach(function(r) {
					data.push({
						id: r.id,
						name: r.name,
						source: r.source,
						clickHandler: clickHandler
					});
				});
				callback(data);
			});
		}
	}
};