/*
 * Copyright (C) 2010-2021 Structr GmbH
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
	$(document).on('contextmenu', '#menu-area', function(e) {
		e.stopPropagation();
		e.preventDefault();
	});

	$(document).on('click', '#add-child-dialog #inherit-visibility-flags' , function (e) {
		e.preventDefault();
		e.stopPropagation();
	});

	$(document).on('mouseup', function() {
		_Elements.removeContextMenu();
	});

});

var _Elements = {
	inheritVisibilityFlagsKey: 'inheritVisibilityFlags_' + port,
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
			elements: ['a', 'em', 'strong', 'small', 's', 'cite', 'g', 'dfn', 'abbr', 'time', 'code', 'var', 'samp', 'kbd', 'sub', 'sup', 'i', 'b', 'u', 'mark', 'ruby', 'rt', 'rp', 'bdi', 'bdo', 'span', 'br', 'wbr', 'q']
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
			attrs: ['name', 'type', 'checked', 'selected', 'value', 'size', 'multiple', 'disabled', 'autofocus', 'placeholder', 'style', 'rows', 'cols', 'required'],
			focus: 'type'
		},
		{
			elements: ['button'],
			attrs: ['name', 'type', 'checked', 'selected', 'value', 'size', 'multiple', 'disabled', 'autofocus', 'placeholder', 'onclick', 'style', 'title', 'form', 'formaction', 'formmethod']
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
			elements: ['script', 'object'],
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
			elements: ['p', 'param', 'pre', 'progress', '|',  'q', '|', 'rp', 'rt', 'ruby']
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

		var html = '<div id="id_' + id + '" class="' + elementClasses.join(' ') + '"><div class="node-selector"></div></div>';

		if (refNode && !refNodeIsParent) {
			refNode.before(html);
		} else {
			parent.append(html);
		}

		var div = Structr.node(id);

		if (!div) {
			return false;
		}

		var displayName = getElementDisplayName(entity);

		var icon = _Elements.getElementIcon(entity);

		div.append('<i class="typeIcon ' + _Icons.getFullSpriteClass(icon) + '" />'
			+ '<b title="' + escapeForHtmlAttributes(displayName) + '" class="tag_ name_ abbr-ellipsis abbr-75pc">' + displayName + '</b><span class="id">' + entity.id + '</span>'
			+ _Elements.classIdString(entity._html_id, entity._html_class));

		_Elements.enableContextMenuOnElement(div, entity);

		_Entities.appendExpandIcon(div, entity, hasChildren);

		//_Entities.appendAccessControlIcon(div, entity);

		_Entities.setMouseOver(div, undefined, ((entity.syncedNodesIds&&entity.syncedNodesIds.length)?entity.syncedNodesIds:[entity.sharedComponentId]));

		if (!hasChildren && !entity.sharedComponentId) {
			_Entities.appendEditSourceIcon(div, entity);
		}

		_Entities.appendEditPropertiesIcon(div, entity);

		if (entity.tag === 'a' || entity.tag === 'link' || entity.tag === 'script' || entity.tag === 'img' || entity.tag === 'video' || entity.tag === 'object') {

			div.append('<i title="Edit Link" class="link_icon button ' + _Icons.getFullSpriteClass(_Icons.link_icon) + '" />');
			if (entity.linkableId) {

				Command.get(entity.linkableId, 'id,type,name,isFile,isImage,isPage,isTemplate', function(linkedEntity) {

					div.append('<span class="linkable' + (linkedEntity.isImage ? ' default-cursor' : '') + '">' + linkedEntity.name + '</span>');

					if (linkedEntity.isFile && !linkedEntity.isImage) {

						$('.linkable', div).on('click', function(e) {
							e.stopPropagation();

							Structr.dialog('Edit ' + linkedEntity.name, function() {
							}, function() {
							});
							_Files.editContent(this, linkedEntity, $('#dialogBox .dialogText'));
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
					let pagesPager = _Pager.addPager('pages-to-link', pagesToLink, true, 'Page', null, function(pages) {

						pages.forEach(function(page){

							if (page.type !== 'ShadowDocument') {

								pagesToLink.append('<div class="node page ' + page.id + '_"><i class="' + _Icons.getFullSpriteClass(_Icons.page_icon) + '" /><b title="' + escapeForHtmlAttributes(page.name) + '" class="name_ abbr-ellipsis abbr-120">' + page.name + '</b></div>');

								var div = $('.' + page.id + '_', pagesToLink);

								_Elements.handleLinkableElement(div, entity, page);
							}
						});
					});

					let pagesPagerFilters = $('<span style="white-space: nowrap;">Filter: <input type="text" class="filter" data-attribute="name" placeholder="Name"/></span>');
					pagesPager.pager.append(pagesPagerFilters);
					pagesPager.activateFilterElements();

					dialog.append('<h3>Files</h3><div class="linkBox" id="foldersToLink"></div><div class="linkBox" id="filesToLink"></div>');

					let filesToLink = $('#filesToLink');
					let foldersToLink = $('#foldersToLink');

					_Pager.initPager('folders-to-link', 'Folder', 1, 25);
					_Pager.forceAddFilters('folders-to-link', 'Folder', { hasParent: false });
					let linkFolderPager = _Pager.addPager('folders-to-link', foldersToLink, true, 'Folder', 'ui', function(folders) {

						for (let folder of folders) {
							_Elements.appendFolder(entity, foldersToLink, folder);
						};
					}, null, 'id,name,hasParent');

					let folderPagerFilters = $('<span style="white-space: nowrap;">Filter: <input type="text" class="filter" data-attribute="name" placeholder="Name" /></span>');
					linkFolderPager.pager.append(folderPagerFilters);
					linkFolderPager.activateFilterElements();

					_Pager.initPager('files-to-link', 'File', 1, 25);
					let linkFilesPager = _Pager.addPager('files-to-link', filesToLink, true, 'File', 'ui', function(files) {

						files.forEach(function(file) {

							filesToLink.append('<div class="node file ' + file.id + '_"><i class="fa ' + _Icons.getFileIconClass(file) + '"></i> '
									+ '<b title="' + escapeForHtmlAttributes(file.path) + '" class="name_ abbr-ellipsis abbr-120">' + file.name + '</b></div>');

							var div = $('.' + file.id + '_', filesToLink);

							_Elements.handleLinkableElement(div, entity, file);
						});
					}, null, 'id,name,contentType,linkingElementsIds,path');

					let filesPagerFilters = $('<span style="white-space: nowrap;">Filters: <input type="text" class="filter" data-attribute="name" placeholder="Name" /><label><input type="checkbox"  class="filter" data-attribute="hasParent" /> Include subdirectories</label></span>');
					linkFilesPager.pager.append(filesPagerFilters);
					linkFilesPager.activateFilterElements();
				}

				if (entity.tag === 'img' || entity.tag === 'link' || entity.tag === 'a') {

					dialog.append('<h3>Images</h3><div class="linkBox" id="imagesToLink"></div>');

					var imagesToLink = $('#imagesToLink');

					_Pager.initPager('images-to-link', 'Image', 1, 25);
					let imagesPager = _Pager.addPager('images-to-link', imagesToLink, false, 'Image', 'ui', function(images) {

						images.forEach(function(image) {

							imagesToLink.append('<div class="node file ' + image.id + '_" title="' + escapeForHtmlAttributes(image.path) + '">' + _Icons.getImageOrIcon(image) + '<b class="name_ abbr-ellipsis abbr-120">' + image.name + '</b></div>');

							var div = $('.' + image.id + '_', imagesToLink);

							_Elements.handleLinkableElement(div, entity, image);
						});
					}, null, 'id,name,contentType,linkingElementsIds,path,tnSmall');

					let imagesPagerFilters = $('<span style="white-space: nowrap;">Filter: <input type="text" class="filter" data-attribute="name" placeholder="Name"/></span>');
					imagesPager.pager.append(imagesPagerFilters);
					imagesPager.activateFilterElements();
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
		var classIdString = '<span class="class-id-attrs abbr-ellipsis abbr-75pc">' + (idString ? '<span class="_html_id_">#' + idString.replace(/\${.*}/g, '${…}') + '</span>' : '')
				+ (classString ? '<span class="_html_class_">.' + classString.replace(/\${.*}/g, '${…}').replace(/ /g, '.') + '</span>' : '') + '</span>';
		return classIdString;
	},
	appendFolder: function(entityToLinkTo, folderEl, subFolder) {

		folderEl.append((subFolder.hasParent ? '<div class="clear"></div>' : '') + '<div class="node folder ' + (subFolder.hasParent ? 'sub ' : '') + subFolder.id + '_"><i class="fa fa-folder"></i> '
				+ '<b title="' + escapeForHtmlAttributes(subFolder.name) + '" class="name_ abbr-ellipsis abbr-200">' + subFolder.name + '</b></div>');

		let subFolderEl = $('.' + subFolder.id + '_', folderEl);

		subFolderEl.on('click', function(e) {
			e.stopPropagation();
			e.preventDefault();

			if (!subFolderEl.children('.fa-folder-open').length) {

				_Elements.expandFolder(entityToLinkTo, subFolder);

			} else {
				subFolderEl.children('.node').remove();
				subFolderEl.children('.clear').remove();
				subFolderEl.children('.fa-folder-open').removeClass('fa-folder-open').addClass('fa-folder');
			}

			return false;

		}).hover(function() {
			$(this).addClass('nodeHover');
		}, function() {
			$(this).removeClass('nodeHover');
		});

	},
	expandFolder: function(entityToLinkTo, folder) {

		Command.get(folder.id, 'id,name,hasParent,files,folders', function(node) {

			let folderEl = $('.' + node.id + '_');
			folderEl.children('.fa-folder').removeClass('fa-folder').addClass('fa-folder-open');

			for (let subFolder of node.folders) {
				_Elements.appendFolder(entityToLinkTo, folderEl, subFolder);
			}

			for (let f of node.files) {

				Command.get(f.id, 'id,name,contentType,linkingElementsIds,path', function(file) {

					$('.' + node.id + '_').append('<div class="clear"></div><div class="node file sub ' + file.id + '_"><i class="fa ' + _Icons.getFileIconClass(file) + '"></i> '
							+ '<b title="' + escapeForHtmlAttributes(file.path) + '" class="name_ abbr-ellipsis abbr-200">' + file.name + '</b></div>');

					let div = $('.' + file.id + '_');

					_Elements.handleLinkableElement(div, entityToLinkTo, file);
				});
			}
		});
	},
	handleLinkableElement: function(div, entityToLinkTo, linkableObject) {

		if (isIn(entityToLinkTo.id, linkableObject.linkingElementsIds)) {
			div.addClass('nodeActive');
		}

		div.on('click', function(event) {

			event.stopPropagation();

			if (div.hasClass('nodeActive')) {

				Command.setProperty(entityToLinkTo.id, 'linkableId', null);

			} else {

				let attrName = (entityToLinkTo.type === 'Link') ? '_html_href' : '_html_src';

				Command.getProperty(entityToLinkTo.id, attrName, function(val) {
					if (!val || val === '') {
						Command.setProperty(entityToLinkTo.id, attrName, '${link.path}', null);
					}
				});

				Command.link(entityToLinkTo.id, linkableObject.id);
			}

			_Entities.reloadChildren(entityToLinkTo.parent.id);

			$('#dialogBox .dialogText').empty();

			$.unblockUI({
				fadeOut: 25
			});
		}).hover(function() {
			$(this).addClass('nodeHover');
		}, function() {
			$(this).removeClass('nodeHover');
		});
	},
	enableContextMenuOnElement: function(div, entity) {

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

		let menuElements = _Elements.getContextMenuElements(div, entity);

		let menuHeight = 24 * menuElements.length;

		let leftOrRight = 'left';
		let topOrBottom = 'top';
		let x = (e.clientX - 8);
		let y = div.offset().top;
		let windowWidth = $(window).width();

		if (e.pageX > (windowWidth / 2)) {
			leftOrRight = 'right';
		}

		if (e.pageY > ($(window).height() - menuHeight)) {
			y -= 20 + menuHeight - ($(window).height() - e.pageY);
		}

		let cssPositionClasses = leftOrRight + ' ' + topOrBottom;

		_Elements.removeContextMenu();

		div.addClass('contextMenuActive');
		$('#menu-area').append('<div id="add-child-dialog"></div>');

		let menu = $('#add-child-dialog');
		menu.css({
			left: x + 'px',
			top: y + 'px'
		});

		let registerContextMenuItemClickHandler = function (el, contextMenuItem) {

			el.on('mouseup', function(e) {
				e.stopPropagation();

				let preventClose = true;

				if (contextMenuItem.clickHandler && (typeof contextMenuItem.clickHandler === 'function')) {
					preventClose = contextMenuItem.clickHandler($(this), contextMenuItem);
				}

				if (!preventClose) {
					_Elements.removeContextMenu();
				}
			});
		};

		let registerPlaintextContextMenuItemHandler = function (el, itemText, forcedClickHandler) {

			el.on('mouseup', function (e) {
				e.stopPropagation();

				if (forcedClickHandler && (typeof forcedClickHandler === 'function')) {
					forcedClickHandler(itemText);
				} else {
					let pageId = (entity.type === 'Page') ? entity.id : entity.pageId;
					let tagName = (itemText === 'content') ? null : itemText;

					Command.createAndAppendDOMNode(pageId, entity.id, tagName, _Dragndrop.getAdditionalDataForElementCreation(tagName), _Elements.isInheritVisibililtyFlagsChecked());
				}

				_Elements.removeContextMenu();
			});
		};

		let addContextMenuElements = function (ul, element, hidden, forcedClickHandler, prepend) {

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

				let menuEntry        = $('<li class="element-group-switch"></li>');
				let menuEntryContent = $('<span class="menu-entry-container">' + (element.icon || '') + '</span>');
				let menuEntryText    = $('<span class="menu-entry-text">' + element.name + '</span>');

				for (let cls of (element.classes || [])) {
					menuEntry.addClass(cls);
				}

				menuEntryContent.append(menuEntryText);
				menuEntry.append(menuEntryContent);

				registerContextMenuItemClickHandler(menuEntry, element);
				if (prepend) {
					ul.prepend(menuEntry);
				} else {
					ul.append(menuEntry);
				}

				if (element.elements) {
					menuEntryContent.append('<i class="fa fa-caret-right pull-right"></i>');

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

		let updateMenuGroupVisibility = function() {

			$('.element-group-switch').hover(function() {
				$(this).children('.element-group').removeClass('hidden');
			}, function() {
				$(this).children('.element-group').addClass('hidden');
			});
		};

		let mainMenuList = $('<ul class="element-group ' + cssPositionClasses + '"></ul>');
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

		let repositionMenu = function() {
			let menuWidth = menu.width();

			if (windowWidth < (x + menuWidth)) {
				menu.css({
					left: (x - menuWidth) + 'px'
				});
            }
		};

		repositionMenu();
	},
	updateVisibilityInheritanceCheckbox: function() {
		let checked = LSWrapper.getItem(_Elements.inheritVisibilityFlagsKey) || false;

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

		const isPage             = (entity.type === 'Page');
		const isContent          = (entity.type === 'Content');
		const isFile             = entity.isFile;
		const isFolder           = entity.isFolder;
		const isUser             = entity.isUser;
		const isGroup            = entity.isGroup;
		const isContentContainer = entity.isContentContainer;
		const isContentItem      = entity.isContentItem;
		const isMailTemplate     = (entity.type === 'MailTemplate');
		const isLocalization     = (entity.type === 'Localization');
		const hasChildren        = (entity.children && entity.children.length > 0);

		var handleInsertHTMLAction = function (itemText) {
			var pageId = isPage ? entity.id : entity.pageId;
			var tagName = (itemText === 'content') ? null : itemText;

			Command.createAndAppendDOMNode(pageId, entity.id, tagName, _Dragndrop.getAdditionalDataForElementCreation(tagName, entity.tag), _Elements.isInheritVisibililtyFlagsChecked());
		};

		var handleInsertBeforeAction = function (itemText) {

			Command.createAndInsertRelativeToDOMNode(entity.pageId, entity.id, itemText, 'Before', _Elements.isInheritVisibililtyFlagsChecked());
		};

		var handleInsertAfterAction = function (itemText) {

			Command.createAndInsertRelativeToDOMNode(entity.pageId, entity.id, itemText, 'After', _Elements.isInheritVisibililtyFlagsChecked());
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

		if (!isFile && !isFolder && !isUser && !isGroup && !isMailTemplate && !isLocalization && !isContentContainer && !isContentItem) {

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
					clickHandler: function () {
						Command.createAndAppendDOMNode(entity.pageId, entity.id, 'div', _Dragndrop.getAdditionalDataForElementCreation('div'), _Elements.isInheritVisibililtyFlagsChecked());
						return false;
					}
				});
			}

			appendSeparator();

			if (!isPage && entity.parent !== null && (entity.parent && entity.parent.type !== 'Page')) {

				elements.push({
					name: 'Insert before...',
					elements: [
						{
							name: '... HTML element',
							elements: _Elements.sortedElementGroups,
							forcedClickHandler: handleInsertBeforeAction
						},
						{
							name: '... Content element',
							elements: ['content', 'template'],
							forcedClickHandler: handleInsertBeforeAction
						},
						{
							name: '... div element',
							clickHandler: function () {
								handleInsertBeforeAction('div');
							}
						}
					]
				});

				elements.push({
					name: 'Insert after...',
					elements: [
						{
							name: '... HTML element',
							elements: _Elements.sortedElementGroups,
							forcedClickHandler: handleInsertAfterAction
						},
						{
							name: '... Content element',
							elements: ['content', 'template'],
							forcedClickHandler: handleInsertAfterAction
						},
						{
							name: '... div element',
							clickHandler: function () {
								handleInsertAfterAction('div');
							}
						}
					]
				});

				appendSeparator();
			}

			if (!isPage && entity.parent !== null && (entity.parent && entity.parent.type !== 'Page')) {

				elements.push({
					name: 'Clone Node',
					clickHandler: function () {
						Command.cloneNode(entity.id, (entity.parent ? entity.parent.id : null), true);
						return false;
					}
				});

				appendSeparator();

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

			if (isPage) {
				elements.push({
					name: 'Clone Page',
					clickHandler: function () {
						Command.clonePage(entity.id);
						return false;
					}
				});
			}

			if (!isPage) {

				appendSeparator();

				if (_Elements.selectedEntity && _Elements.selectedEntity.id === entity.id) {
					elements.push({
						name: 'Deselect element',
						clickHandler: function () {
							_Elements.unselectEntity();
							return false;
						}
					});
				} else {
					elements.push({
						name: 'Select element',
						clickHandler: function () {
							_Elements.selectEntity(entity);
							return false;
						}
					});
				}

				let isEntitySharedComponent = entity.sharedComponent || (entity.isPage && entity.pageId === shadowPage.id);
				if (!isEntitySharedComponent) {
					appendSeparator();

					elements.push({
						name: 'Convert to Shared Component',
						clickHandler: function () {
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
						clickHandler: function () {
							Command.cloneComponent(_Elements.selectedEntity.id, entity.id);
							_Elements.unselectEntity();
							return false;
						}
					});

				}

				if (!isPage || (isPage && !hasChildren && (_Elements.selectedEntity.tag === 'html' || _Elements.selectedEntity.type === 'Template'))) {
					elements.push({
						name: 'Clone selected element here',
						clickHandler: function () {
							Command.cloneNode(_Elements.selectedEntity.id, entity.id, true);
							_Elements.unselectEntity();
							return false;
						}
					});
				}

				if (isSamePage && !isThisEntityDirectParentOfSelectedEntity && !isSelectedEntityInShadowPage && !isDescendantOfSelectedEntity(entity)) {
					elements.push({
						name: 'Move selected element here',
						clickHandler: function () {
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
					name: 'Repeater',
					clickHandler: function () {
						_Entities.showProperties(entity, 'query');
						return false;
					}
				});
				elements.push({
					name: 'Events',
					clickHandler: function () {
						_Entities.showProperties(entity, 'editBinding');
						return false;
					}
				});
				elements.push({
					name: 'HTML Attributes',
					clickHandler: function () {
						_Entities.showProperties(entity, '_html_');
						return false;
					}
				});

			}
		}

		if (isUser || isGroup) {

			let userOrGroupEl = div.closest('.node');
			let parentGroupEl = userOrGroupEl.parent().closest('.group');
			if (parentGroupEl.length) {
				let parentGroupId = Structr.getGroupId(parentGroupEl);
				elements.push({
					name: 'Remove ' + entity.name + ' from ' + $('.name_', parentGroupEl).attr('title'),
					clickHandler: function () {
						Command.removeFromCollection(parentGroupId, 'members', entity.id, function () {
							_UsersAndGroups.refreshGroups();
						});
						return false;
					}
				});
			}

			appendSeparator();
		}

		if (isFile) {

			if (displayingFavorites) {
				elements.push({
					name: 'Remove from Favorites',
					clickHandler: function () {
						Command.favorites('remove', entity.id, () => {
							Structr.node(entity.id).remove();
						});
						return false;
					}
				});

			} else if (entity.isFavoritable) {
				elements.push({
					name: 'Add to Favorites',
					clickHandler: function () {
						Command.favorites('add', entity.id, () => {});
						return false;
					}
				});
			}

			if (_Files.isArchive(entity)) {
				elements.push({
					name: 'Unpack archive',
					clickHandler: function () {
						_Files.unpackArchive(entity);
						return false;
					}
				});
			}

			appendSeparator();

			if (isFile && entity.isImage && entity.contentType !== 'text/svg' && !entity.contentType.startsWith('image/svg')) {
				elements.push({
					name: 'Edit Image',
					clickHandler: function () {
						_Files.editImage(entity);
						return false;
					}
				});
			} else if (isFile) {
				elements.push({
					name: 'Edit File',
					clickHandler: function () {
						_Files.editFile(entity);
						return false;
					}
				});
			}

			Structr.performModuleDependendAction(function () {
				if (Structr.isModulePresent('csv') && Structr.isModulePresent('api-builder') && entity.contentType === 'text/csv') {
					elements.push({
						name: 'Import CSV',
						clickHandler: function () {
							Importer.importCSVDialog(entity, false);
							return false;
						}
					});
				}
			});

			Structr.performModuleDependendAction(function () {
				if (Structr.isModulePresent('xml') && (entity.contentType === 'text/xml' || entity.contentType === 'application/xml')) {
					elements.push({
						name: 'Import XML',
						clickHandler: function () {
							Importer.importXMLDialog(entity, false);
							return false;
						}
					});
				}
			});
		}

		elements.push({
			name: 'Properties',
			clickHandler: function() {
				_Entities.showProperties(entity, 'ui');
				return false;
			}
		});

		appendSeparator();

		elements.push({
			icon: _Icons.svg.security,
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
							name: 'Make element invisible',
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

		if (!isFile && !isFolder && !isContent && !isUser && !isGroup && !isMailTemplate && !isLocalization && !isContentContainer && !isContentItem) {

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

		if (isPage) {
			elements.push({
				icon: _Icons.svg.page_settings,
				name: 'Configure Page Preview',
				clickHandler: function () {
					_Pages.previews.configurePreview(entity);
					return false;
				}
			});

			elements.push({
				icon: _Icons.svg.page_open,
				name: 'Open Page in new tab',
				clickHandler: function () {
					let url = _Pages.previews.getUrlForPage(entity);
					window.open(url);
					return false;
				}
			});
		}

		// DELETE AREA - ALWAYS AT THE BOTTOM
		// allow "Remove Node" on first level children of page
		if (Structr.isModuleActive(_Pages) && !isPage && entity.parent !== null) {

			elements.push({
				icon: _Icons.svg.trashcan,
				classes: ['menu-bolder', 'danger'],
				name: 'Remove Node',
				clickHandler: function () {
					Command.removeChild(entity.id, () => {
						_Pages.unattachedNodes.blinkUI();
					});
					return false;
				}
			});
		}

		// "Delete <type>" should be visible everywhere... except in Pages: There it should only be visible for type===Page
		if (!Structr.isModuleActive(_Pages) || (Structr.isModuleActive(_Pages) && (isPage || !entity.parent))) {

			elements.push({
				icon: _Icons.svg.trashcan,
				classes: ['menu-bolder', 'danger'],
				name: 'Delete ' + entity.type,
				clickHandler: () => {

					if (isContent || isFile || isUser || isGroup) {
						_Entities.deleteNode(this, entity);
					} else if (isFile) {
						_Entities.deleteNode(this, entity);
					} else if (isFolder) {

						let selectedElements = document.querySelectorAll('.node.selected');
						let selectedCount = selectedElements.length;
						let el = Structr.node(entity.id)[0].closest('.folder');
						if (selectedCount > 1 && el.classList.contains('selected')) {

							let files = [];

							selectedElements.forEach((el) => {
								files.push(Structr.entityFromElement(el));
							});

							_Entities.deleteNodes(this, files, true, () => {
								_Files.refreshTree();
							});

						} else {
							_Entities.deleteNode(this, entity, true, () => {
								_Files.refreshTree();
							});
						}
					} else if (isMailTemplate) {

						_Entities.deleteNode(this, entity, false, () => {
							if (LSWrapper.getItem(_MailTemplates.mailTemplateSelectedElementKey) && LSWrapper.getItem(_MailTemplates.mailTemplateSelectedElementKey) === entity.id) {
								LSWrapper.removeItem(_MailTemplates.mailTemplateSelectedElementKey);
							}
							let row = Structr.node(entity.id, '#mail-template-');
							if (row) {
								row.remove();
								// _MailTemplates.clearMailTemplateDetails();
								_MailTemplates.checkMainVisibility();
							}

						});

					} else if (isLocalization) {
						let keyAndDomainObject = entity;
						if (true === confirm('Do you really want to delete the complete localizations for "' + keyAndDomainObject.name + '"' + (keyAndDomainObject.domain ? ' in domain "' + keyAndDomainObject.domain + '"' : ' with empty domain') + ' ?')) {
							_Localization.deleteCompleteLocalization((keyAndDomainObject.name ? keyAndDomainObject.name : null), (keyAndDomainObject.domain ? keyAndDomainObject.domain : null), this);
						}
					} else if (isContentContainer) {
						_Entities.deleteNode(this, entity, true, function() {
							_Contents.refreshTree();
						});
					} else if (isContentItem) {
						_Entities.deleteNode(this, entity);
					} else {
						_Entities.deleteNode(this, entity, true, () => {
							var synced = entity.syncedNodesIds;
							if (synced && synced.length) {
								synced.forEach(function (id) {
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
					}
					return false;
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
		var html = '<div id="id_' + entity.id + '" class="node content ' + (isActiveNode ? ' activeNode' : 'staticNode') + (_Elements.isEntitySelected(entity) ? ' nodeSelectedFromContextMenu' : '') + '"><div class="node-selector"></div>'
				+ '<i class="typeIcon ' + _Icons.getFullSpriteClass(icon) + ' typeIcon-nochildren" />'
				+ (name ? ('<b title="' + escapeForHtmlAttributes(displayName) + '" class="tag_ name_ abbr-ellipsis abbr-75pc">' + displayName + '</b>') : ('<div class="content_ abbr-ellipsis abbr-75pc">' + escapeTags(entity.content) + '</div>'))
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

		if (entity.hidden === true) {
			div.addClass('is-hidden');
		}

		//_Entities.appendAccessControlIcon(div, entity);

		_Elements.enableContextMenuOnElement(div, entity);

		_Pages.registerDetailClickHandler(div, entity);

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
			_Elements.openEditContentDialog(this, entity, {
				extraKeys: { "Ctrl-Space": "autocomplete" },
				gutters: ["CodeMirror-lint-markers"],
				lint: {
					getAnnotations: function(text, callback) {
						_Code.showScriptErrors(entity, text, callback, 'content');
					},
					async: true
				}
			});
			return false;
		});

	},
	openEditContentDialog: function(btn, entity, configOverride) {
		Structr.dialog('Edit content of ' + (entity.name ? entity.name : entity.id), function() {
		}, function() {
		});
		Command.get(entity.id, 'content,contentType', function(data) {
			currentEntity = entity;
			entity.contentType = data.contentType;
			_Elements.editContent(this, entity, data.content, dialogText, configOverride);
		});
	},
	displayCentralEditor: function(entity, configOverride) {

		let previewsContainer = document.querySelector('#center-pane');
		let contentEditorContainer = document.querySelector('#center-pane .content-editor-container');

		if (contentEditorContainer) {
			previewsContainer.removeChild(contentEditorContainer);
		}

		Structr.fetchHtmlTemplate('pages/content-editor', {}, (html) => {

			previewsContainer.insertAdjacentHTML('afterbegin', html);

			contentEditorContainer = document.querySelector('#center-pane .content-editor-container');

			Command.get(entity.id, 'content,contentType', function(data) {
				currentEntity = entity;
				entity.contentType = data.contentType;
				_Elements.editContent(this, entity, data.content, contentEditorContainer, configOverride);
			});
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
	editContent: function(button, entity, text, element, configOverride = {}) {

		let buttonArea = element.querySelector('.editor-button-container') || dialogBtn;
		let infoArea   = element.querySelector('.editor-info-container')   || dialogMeta;

		if (Structr.isButtonDisabled(button)) {
			return;
		}

		element.insertAdjacentHTML('afterbegin', '<div class="editor"></div>');
		let contentBox = element.querySelector('.editor');
		contentType = entity.contentType || 'text/plain';

		_Elements.activateEditorMode(contentType);

		var text1, text2;

		let cmConfig = Structr.getCodeMirrorSettings({
			value: text || '',
			mode: mode || contentType,
			lineNumbers: true,
			lineWrapping: false,
			extraKeys: {
				'Ctrl-Space': 'autocomplete'
			},
			indentUnit: 4,
			tabSize: 4,
			indentWithTabs: true
		});

		cmConfig = Object.assign(cmConfig, configOverride);

		// Intitialize editor
		CodeMirror.defineMIME('text/html', 'htmlmixed-structr');
		editor = CodeMirror(contentBox, cmConfig);

		_Code.setupAutocompletion(editor, entity.id);

		Structr.resize();

		buttonArea.insertAdjacentHTML('beforeend', '<button id="editorSave" disabled="disabled" class="disabled">Save</button>');

		// Append "Save and close" button only when dialog window is open
		if (dialogBox.is(':visible')) {
			buttonArea.insertAdjacentHTML('beforeend', '<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');
		}

		if (entity.isFavoritable) {
			buttonArea.insertAdjacentHTML('beforeend', '<i title="Add to favorites" class="add-to-favorites ' + _Icons.getFullSpriteClass(_Icons.star_icon) + '" >');
			let addToFavs = buttonArea.querySelector('.add-to-favorites');
			addToFavs.addEventListener('click', () => {
				Command.favorites('add', entity.id, () => {
					blinkGreen(addToFavs);
				});
			});
		}

		// Experimental speech recognition, works only in Chrome 25+
		if (typeof(webkitSpeechRecognition) === 'function') {

			buttonArea.insertAdjacentHTML('beforeend', '<button class="speechToText"><i class="' + _Icons.getFullSpriteClass(_Icons.microphone_icon) + '" /></button>');
			let speechToTextButton = buttonArea.querySelector('.speechToText');
			let speechBtn = $(speechToTextButton);
			_Speech.init(speechBtn, function(interim, finalResult) {

				if (_Speech.isCommand('save', interim)) {
					dialogSaveButton.click();
				} else if (_Speech.isCommand('saveAndClose', interim)) {
					_Speech.toggleStartStop(speechBtn, function() {
						buttonArea.querySelector('#saveAndClose').click();
					});
				} else if (_Speech.isCommand('close', interim)) {
					_Speech.toggleStartStop(speechBtn, function() {
						buttonArea.querySelector('.closeButton').click();
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

		dialogSaveButton = $('#editorSave');
		saveAndClose = $('#saveAndClose');

		let saveButton = buttonArea.querySelector('.save-button') || document.querySelector('#editorSave');

		let saveAndCloseButton;
		if (dialogBox.is(':visible')) {

			saveAndCloseButton = buttonArea.querySelector('.save-and-close-button') || document.querySelector('#saveAndClose');
			saveAndCloseButton.addEventListener('click', (e) => {
				e.stopPropagation();
				saveButton.click();
				// setTimeout(function() {
				// 	dialogSaveButton = $('#editorSave');
				// 	saveAndClose = $('#saveAndClose');
				// 	dialogSaveButton.remove();
				// 	saveAndClose.remove();
				// 	dialogCancelButton.click();
				// }, 500);
			});
		}

		editor.on('change', function(cm, change) {

			let editorText = editor.getValue();

			if (text === editorText) {
				saveButton.setAttribute('disabled', 'disabled');
				saveButton.classList.add('disabled');
				if (saveAndCloseButton) {
					saveAndCloseButton.setAttribute('disabled', 'disabled');
					saveAndCloseButton.classList.add('disabled');
				}
			} else {
				saveButton.removeAttribute('disabled');
				saveButton.classList.remove('disabled');
				if (saveAndCloseButton) {
					saveAndCloseButton.removeAttribute('disabled');
					saveAndCloseButton.classList.remove('disabled');
				}
			}

			$('#chars').text(editorText.length);
			$('#words').text((editorText.match(/\S+/g) || []).length);
		});

		let scrollInfo = JSON.parse(LSWrapper.getItem(scrollInfoKey + '_' + entity.id));
		if (scrollInfo) {
			editor.scrollTo(scrollInfo.left, scrollInfo.top);
		}

		editor.on('scroll', function() {
			let scrollInfo = editor.getScrollInfo();
			LSWrapper.setItem(scrollInfoKey + '_' + entity.id, JSON.stringify(scrollInfo));
		});

		if (saveButton) {

			saveButton.addEventListener('click', (e) => {
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

				Command.patch(entity.id, text1, text2, function () {
					Structr.showAndHideInfoBoxMessage('Content saved.', 'success', 2000, 200);
					dialogSaveButton.prop('disabled', true).addClass('disabled');
					saveAndClose.prop('disabled', true).addClass('disabled');
					Command.getProperty(entity.id, 'content', function (newText) {
						text = newText;
					});

					window.setTimeout(function () {
						editor.performLint();
					}, 300);
				});

			});
		}

		const values = ['text/plain', 'text/html', 'text/xml', 'text/css', 'text/javascript', 'text/markdown', 'text/textile', 'text/mediawiki', 'text/tracwiki', 'text/confluence', 'text/asciidoc'];

		infoArea.insertAdjacentHTML('beforeend', '<label for="contentTypeSelect">Content-Type:</label> <select class="contentType_" id="contentTypeSelect"></select>');
		let select = infoArea.querySelector('#contentTypeSelect');
		values.forEach((type) => {
			select.insertAdjacentHTML('beforeend', '<option ' + (type === entity.contentType ? 'selected' : '') + ' value="' + type + '">' + type + '</option>');
		});
		select.addEventListener('change', () => {
			contentType = select.value;
			_Elements.activateEditorMode(contentType);
			editor.setOption('mode', contentType);

			entity.setProperty('contentType', contentType, false, function() {
				blinkGreen(select);
			});
		});

		infoArea.insertAdjacentHTML('beforeend', '<span class="editor-info"><label for="lineWrapping">Line Wrapping:</label> <input id="lineWrapping" type="checkbox"' + (Structr.getCodeMirrorSettings().lineWrapping ? ' checked="checked" ' : '') + '></span>');
		let lineWrappingHandler = (e) => {
			Structr.updateCodeMirrorOptionGlobally('lineWrapping', e.target.checked);
			blinkGreen(this.parentNode);
			editor.refresh();
		};

		let lineWrapping = document.querySelector('#lineWrapping');
		if (lineWrapping) {
			lineWrapping.removeEventListener('change', lineWrappingHandler);
			lineWrapping.addEventListener('change', lineWrappingHandler);
		}

		infoArea.insertAdjacentHTML('beforeend', '<span class="editor-info">Characters: <span id="chars">' + editor.getValue().length + '</span></span>');
		infoArea.insertAdjacentHTML('beforeend', '<span class="editor-info">Words: <span id="words">' + (editor.getValue().match(/\S+/g) !== null ? editor.getValue().match(/\S+/g).length : 0) + '</span></span>');

		editor.id = entity.id;
		editor.focus();

		window.setTimeout(function() { editor.performLint(); }, 300);

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
				let data = [];
				if (result && result.length) {
					result.forEach(function (r) {
						data.push({
							id: r.id,
							name: r.name,
							source: r.source,
							clickHandler: clickHandler
						});
					});
				}
				callback(data);
			});
		}
	}
};