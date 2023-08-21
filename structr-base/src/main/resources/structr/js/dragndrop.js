/*
 * Copyright (C) 2010-2023 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * Library for all drag & drop actions in Structr
 */
let _Dragndrop = {
	sorting: false,
	sortParent: undefined,

	makeDroppable: (element, previewId) => {
		let el = $(element);
		let tag, iframe = previewId ? $('#preview_' + previewId) : undefined;

		el.droppable({
			iframeFix: true,
			iframe: iframe,
			accept: '.node, .element, .content, .image, .file, .widget, .data-binding-attribute, .data-binding-type',
			greedy: true,
			// hoverClass: 'nodeHover',
			tolerance: 'pointer',
			drop: function(e, ui) {

				$('.node-container').removeClass('nodeHover');

				if (_Elements.dropBlocked === true) {
					_Elements.dropBlocked = false;
					return false;
				}

				e.preventDefault();
				e.stopPropagation();

				let self = $(this), related;

				let sourceId = Structr.getId(ui.draggable) || Structr.getComponentId(ui.draggable);
				let targetId = Structr.getId(self.hasClass('node-container') ? self.parent() : self);

				//console.log(self, self.closest('.nodeHover'));

				if (self.hasClass('jstree-wholerow')) {
					targetId = self.parent().prop('id');

					if (targetId === 'root') {

						Command.setProperty(sourceId, 'parent', null, false, () => {
							$(ui.draggable).remove();

							let activeModule = Structr.getActiveModule();
							if (typeof activeModule.refreshTree === 'function') {
								activeModule.refreshTree();
							}

							return true;
						});

						return;

					} else if (targetId === 'favorites') {

						let obj = StructrModel.obj(sourceId);
						if (obj.isFile) {

							Command.favorites('add', sourceId, function() {
								_Helpers.blinkGreen(Structr.node(sourceId));
							});

						} else {

							_Helpers.blinkRed(Structr.node(sourceId));
						}

						return;
					}
				}

				if (!targetId) {
					targetId = self.attr('data-structr-id');
				}

				if (targetId === Structr.getId(_Dragndrop.sortParent)) {
					return false;
				}

				_Entities.ensureExpanded(self);
				_Dragndrop.sorting    = false;
				_Dragndrop.sortParent = undefined;

				let source = StructrModel.obj(sourceId);
				let target = StructrModel.obj(targetId);

				let page = self.closest('.page')[0];

				if (!page) {
					page = self.closest('[data-structr-page]')[0];
				}
				let pageId = (page ? Structr.getId(page) : target.pageId);

				if (!pageId) {
					pageId = $(page).attr('data-structr-page');
				}

				if (!target) {
					// synthesize target with id only
					target = {id: targetId};
				}

				if (target.type === 'Page') {
					pageId = target.id;
				}

				if (target.isContent && target.type !== 'Template' && source) {
					return false;
				}

				if (target.isContent && target.type !== 'Template' && _Helpers.isIn(tag, _Elements.voidAttrs)) {
					return false;
				}

				if (_Dragndrop.dropAction(source, target, pageId, tag, related)) {
					$(ui.draggable).remove();
					_Dragndrop.sortParent = undefined;
				}
			},
			over: function (e, ui) {

				$('.node-container').removeClass('nodeHover');
				$(this).children('.node-container').addClass('nodeHover');
			},
			out: function (e, ui) {
				$('.node-container').removeClass('nodeHover');
			}
		});
	},
	makeSortable: (element) => {
		let el = $(element);

		let sortableOptions = {
			iframeFix: true,
			appendTo: '#main',
			forceHelperSize: true,
			forcePlaceholderSize: true,
			placeholder: 'pages-sortable-placeholder',
			distance: 5,
			items: '> .node',
			helper: function (event, helperEl) {

				_Pages.pagesTree.append('<div id="collapse-offset"></div>');

				$('#collapse-offset', _Pages.pagesTree).css('height', helperEl.height() - 17);
				helperEl.css({height: '2rem'});

				let hlp = helperEl.clone();
				hlp.find('.node').remove();

				// toggle expand icon in helper (only from expanded to collapsed)
				hlp.find('.expand_icon_svg').removeClass(_Icons.expandedClass).addClass(_Icons.collapsedClass);
				return hlp;
			},
			zIndex: 99,
			containment: 'body',
			start: function(event, ui) {

				_Dragndrop.sorting    = true;
				_Dragndrop.sortParent = $(ui.item).parent();

				Structr.currentlyActiveSortable = el;

				_Pages.sharedComponents.highlightNewSharedComponentDropZone();
			},
			update: function(event, ui) {

				let el = $(ui.item);
				if (!_Dragndrop.sorting) {
					return false;
				}

				let id = Structr.getId(el);
				if (!id) {
					id = Structr.getComponentId(el);
				}

				let nextNode = el.next('.node');
				let refId = Structr.getId(nextNode);
				if (!refId) {
					refId = Structr.getComponentId(nextNode);
				}

				let parentId = Structr.getId(_Dragndrop.sortParent);
				el.remove();

				_Dragndrop.storeTemporarilyRemovedElementUUID(id);

				Command.insertBefore(parentId, id, refId);

				_Dragndrop.clearTemporarilyRemovedElementUUID();

				_Dragndrop.sorting    = false;
				_Dragndrop.sortParent = undefined;
				$('#collapse-offset', _Pages.pagesTree).remove();

				Structr.currentlyActiveSortable = undefined;
			},
			stop: function(event, ui) {
				$('.element-dropzone').removeClass("allow-drop");
				_Dragndrop.sorting = false;
				_Entities.resetMouseOverState(ui.item);
				$(event.toElement).one('click', function(e) {
					e.stopImmediatePropagation();
				});
				$(ui.item).css({height: ''});
				$('#collapse-offset', _Pages.pagesTree).remove();

				Structr.currentlyActiveSortable = undefined;
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
	dropAction: (source, target, pageId, tag, related) => {

		if (source && pageId && source.pageId && pageId !== source.pageId) {

			if (_Pages.shadowPage && source.pageId === _Pages.shadowPage.id) {

				Command.cloneComponent(source.id, target.id);

			} else if (source.pageId !== target.pageId) {

				Command.cloneNode(source.id, target.id, true);

			} else {

				Command.appendChild(source.id, target.id, pageId);
			}

			return false;
		}

		// element dropped on itself?
		if (source && target && (source.id === target.id)) {
			return false;
		}

		if (source && source.isWidget) {
			return _Dragndrop.widgetDropped(source, target, pageId);
		}

		if (Structr.isModuleActive(_Pages) && source && source.isImage) {
			return _Dragndrop.imageDropped(source, target, pageId);
		}

		if (source && (source.isFile || source.isFolder)) {
			return _Dragndrop.fileDropped(source, target, pageId);
		}

		if (source && (source.isContentItem || source.isContentContainer)) {
			return _Dragndrop.contentItemDropped(source, target);
		}

		if (!source && tag) {

			if (tag.indexOf('.') !== -1) {

				Command.get(target.id, 'id,children', function(target) {
					let firstContentId = target.children[0].id;
					if (related) {
						let key = tag.substring(tag.indexOf('.') + 1);

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
				let type = tag.substring(1);
				Command.setProperty(target.id, 'restQuery', type);
				Command.setProperty(target.id, 'dataKey', type.toLowerCase(), false, function() {
//					_Pages.reloadPreviews();
// 					console.log('reload preview?');
				});
			}

		} else {

			tag = target.tag;

			if (source && target && source.id && target.id) {

				_Dragndrop.sorting = false;

				_Dragndrop.storeTemporarilyRemovedElementUUID(source.id);

				Command.appendChild(source.id, target.id, undefined, () => {
					_Dragndrop.temporarilyRemovedElementUUID = undefined;
				});

				return true;
			}
		}
	},
	getAdditionalDataForElementCreation: (tag, parentTag) => {
		let nodeData = {};

		let tagsWithAutoContent = ['a', 'p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h5', 'pre', 'label', 'option', 'li', 'em', 'title', 'b', 'span', 'th', 'td', 'button', 'figcaption'];

		if (tagsWithAutoContent.includes(tag)) {
			if (tag === 'a') {
				nodeData._html_href = '${link.name}';
				nodeData.childContent = '${parent.link.name}';
			} else if (tag === 'title') {
				nodeData.childContent = '${capitalize(page.name)}';
			} else {
				nodeData.childContent = 'Initial text for ' + tag;
			}
		}

		if (parentTag) {

			if (tag === null || tag === 'content') {
				if (parentTag === 'script') {
					nodeData.contentType = 'text/javascript';
					nodeData.content = '// text';
				} else if (parentTag === 'style') {
					nodeData.contentType = 'text/css';
					nodeData.content = '/* text */';
				}
			}

			if (tag === 'template') {
				if (parentTag === 'script') {
					nodeData.contentType = 'text/javascript';
					nodeData.content = '// template';
				} else if (parentTag === 'style') {
					nodeData.contentType = 'text/css';
					nodeData.content = '/* template */';
				}
			}
		}
		return nodeData;
	},
	widgetDropped: function(source, target, pageId) {

		Structr.makePagesMenuDroppable();

		_Widgets.insertWidgetIntoPage(source, target, pageId);
	},
	fileDropped: (source, target, pageId) => {

		if (!pageId) {

			_Files.handleMoveObjectsAction(target.id, source.id);

			return true;

		} else {

			let nodeData  = {}, tag;
			let name      = source.name;
			let parentTag = target.tag;

			nodeData.linkableId = source.id;

			if (parentTag === 'head') {

				if (name.endsWith('.css')) {

					tag = 'link';
					nodeData._html_href = '${link.path}?${link.version}';
					nodeData._html_type = 'text/css';
					nodeData._html_rel = 'stylesheet';
					nodeData._html_media = 'screen';

				} else if (name.endsWith('.js')) {

					tag = 'script';
					nodeData._html_src = '${link.path}?${link.version}';
				}

			} else {

				nodeData._html_href = '${link.path}';
				nodeData._html_title = '${link.name}';
				nodeData.childContent = '${parent.link.name}';
				tag = 'a';
			}
			source.id = undefined;

			Structr.modules['files'].unload();
			Command.createAndAppendDOMNode(pageId, target.id, tag, nodeData);

			return true;
		}
	},
	imageDropped: function(source, target, pageId) {

		let nodeData = {}, tag;
		nodeData._html_src = '${link.path}?${link.version}';
		nodeData.linkableId = source.id;
		tag = 'img';

		Structr.modules['files'].unload();
		Command.createAndAppendDOMNode(pageId, target.id, tag, nodeData);

		return true;
	},
	contentItemDropped: (source, target) => {

		_Contents.handleMoveObjectsAction(target.id, source.id);

		return true;
	},


	/**
	 * save and element uuid which will be removed from the UI temporarily so we do not close the center editing pane for this element
	 * this happens if the selected element is moved/wrapped or changes its parent
	 * the cleanup function is used if the websocket function which causes the change does not have a callback
	 */
	temporarilyRemovedElementUUID: undefined,
	storeTemporarilyRemovedElementUUID: (id) => {
		if (id === _Entities?.selectedObject?.id) {
			_Dragndrop.temporarilyRemovedElementUUID = id;
		}
	},
	clearTemporarilyRemovedElementUUID: () => {
		window.setTimeout(() => {
			_Dragndrop.temporarilyRemovedElementUUID = undefined;
		}, 1000);
	}
};