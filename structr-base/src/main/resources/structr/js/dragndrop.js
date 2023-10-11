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
	},



	// drag temp data
	dragActive: false,
	dragEntity: undefined,
	dragElementDropCallback: undefined,
	showRelativeDropTargets: true,

	cssClasses: {
		// functionaliy classes
		dragOverClass:                 'drag-over',
		dragNodeHoverClass:            'drag-hover',
		dragNodeDropNotPossibleClass:  'drop-not-possible',
		dragShowDropTargetBeforeClass: 'show-drop-target-before',
		dragShowDropTargetAfterClass:  'show-drop-target-after',

		// node background classes
		backgroundNodeHighlight: 'drag-background-node-highlight',
		backgroundDropForbidden: 'drag-background-drop-forbidden'
	},
	dragNDropEnded: () => {

		_Dragndrop.clearDragNDropClasses();

		_Dragndrop.dragActive              = false;
		_Dragndrop.dragEntity              = undefined;
		_Dragndrop.dragElementDropCallback = undefined;
		_Dragndrop.showRelativeDropTargets = true;
	},
	clearDragNDropClasses: () => {

		for (let overNode of document.querySelectorAll(`.${_Dragndrop.cssClasses.dragOverClass}`)) {
			overNode.classList.remove(_Dragndrop.cssClasses.dragOverClass);
		}

		for (let showRelativeDropTargetNode of document.querySelectorAll(`.${_Dragndrop.cssClasses.dragShowDropTargetBeforeClass}`)) {
			showRelativeDropTargetNode.classList.remove(_Dragndrop.cssClasses.dragShowDropTargetBeforeClass);
		}

		for (let showRelativeDropTargetNode of document.querySelectorAll(`.${_Dragndrop.cssClasses.dragShowDropTargetAfterClass}`)) {
			showRelativeDropTargetNode.classList.remove(_Dragndrop.cssClasses.dragShowDropTargetAfterClass);
		}

		for (let dropNotPossibleNode of document.querySelectorAll(`.${_Dragndrop.cssClasses.dragNodeDropNotPossibleClass}`)) {
			dropNotPossibleNode.classList.remove(_Dragndrop.cssClasses.dragNodeDropNotPossibleClass);
		}

		_Dragndrop.clearDragHover();

		_Dragndrop.removeBackgroundClasses();
	},
	removeBackgroundClasses: () => {

		for (let element of document.querySelectorAll(`.${_Dragndrop.cssClasses.backgroundNodeHighlight}, .${_Dragndrop.cssClasses.backgroundDropForbidden}`)) {
			_Dragndrop.removeBackgroundFromNode(element);
		}
	},
	removeBackgroundFromNode: (node) => {

		node.classList.remove(_Dragndrop.cssClasses.backgroundNodeHighlight);
		node.classList.remove(_Dragndrop.cssClasses.backgroundDropForbidden);
	},
	clearDragHover: () => {

		for (let hoverNode of document.querySelectorAll(`.${_Dragndrop.cssClasses.dragNodeHoverClass}`)) {
			hoverNode.classList.remove(_Dragndrop.cssClasses.dragNodeHoverClass);
		}
	},
	setDragHover: (element) => {

		_Dragndrop.clearDragHover();

		element.classList.add(_Dragndrop.cssClasses.dragNodeHoverClass);
	},

	enableDraggable: (entity, node, dropHandler, showRelativeDropTargets = true, dragStartHandler, dragEndHandler) => {

		node.addEventListener('dragstart', (e) => {
			// only run for innermost element
			e.stopPropagation();

			e.dataTransfer.effectAllowed = "all";

			// TODO: necessary?
			node.classList.remove('nodeHover');

			_Dragndrop.dragActive              = true;
			_Dragndrop.dragEntity              = entity;
			_Dragndrop.dragElementDropCallback = dropHandler;
			_Dragndrop.showRelativeDropTargets = showRelativeDropTargets;

			dragStartHandler?.(e, entity, node);
		});

		node.addEventListener('dragend', (e) => {
			// only run for innermost element
			e.stopPropagation();

			_Dragndrop.dragNDropEnded();

			dragEndHandler?.(e, entity, node);
		});
	},
	pages: {
		isDraggedNodeParentOfTargetNode: (targetId) => {

			let node          = Structr.node(targetId)[0];
			let draggedNode   = Structr.node(_Dragndrop.dragEntity.id)[0];

			return draggedNode.contains(node);
		},
		draggedNodeStillInSamePage: (targetId) => {

			let draggedNodePageId = StructrModel.obj(_Dragndrop.dragEntity.id).pageId;
			let targetModelObject = StructrModel.obj(targetId);

			let targetPageId = (targetModelObject.type === 'Page') ? targetId : targetModelObject.pageId;

			return (draggedNodePageId === targetPageId);
		},
		getDropEffect: (overNode, nodeId, isOverNode) => {

			if (_Dragndrop.pages.isDraggedNodeParentOfTargetNode(nodeId) || (isOverNode === true && StructrModel.obj(nodeId).type === 'Content')) {

				return 'none';

			} else {

				let inSamePage = _Dragndrop.pages.draggedNodeStillInSamePage(nodeId);
				if (inSamePage === true) {

					return 'move';

				} else {

					return 'copy';
				}
			}
		},
		dragStart: (e, entity, node) => {

			let isInSharedComponents             = _Pages.sharedComponents.isNodeInSharedComponents(node);
			let isAlreadySyncedToSharedComponent = entity.sharedComponentId !== null;

			if (isInSharedComponents === false && isAlreadySyncedToSharedComponent === false) {

				// highlight dropzone to create new shared components
				_Pages.sharedComponents.highlightNewSharedComponentDropZone();

				// highlight dropzones related to event mapping
				for (let eventMappingDropzone of _Pages.centerPane?.querySelectorAll('.element-dropzone') ?? []) {
					_Pages.highlightDropZone(eventMappingDropzone);
				}
			}
		},
		dragEnd: (e, entity, node) => {

			_Pages.sharedComponents.unhighlightNewSharedComponentDropZone();

			// highlight dropzones related to event mapping
			for (let eventMappingDropzone of _Pages.centerPane?.querySelectorAll('.element-dropzone') ?? []) {
				_Pages.unhighlightDropZone(eventMappingDropzone);
			}
		},
		enableDroppable: (entity, node, nodeContainer) => {

			let entityIsContentNode = (entity.type === 'Content');
			let beforeNodeElement   = node.querySelector(':scope > .before-node');
			let afterNodeElement    = node.querySelector(':scope > .after-node');

			nodeContainer.addEventListener('dragenter', (e) => {

				if (!_Dragndrop.dragEntity) {
					return;
				}

				e.stopPropagation();

				let draggedNode = Structr.node(_Dragndrop.dragEntity.id)[0];

				_Dragndrop.clearDragNDropClasses();

				let nodeHasVisibleChildren = node.querySelectorAll(':scope > .node').length > 0;
				let nodeHasDirectSibling   = node.nextElementSibling?.classList.contains('node') ?? false;

				// show parent elements 'after' (if this node has no visible child elements)
				// walk up parents until we see a node where nextSibling is also a node
				// TODO: if dragged node is PARENT of target node, do not show the 'after' elements that are children of the dragged node
				if (_Dragndrop.showRelativeDropTargets) {

					let canStop = nodeHasVisibleChildren || nodeHasDirectSibling;
					let curNode = node;
					while (canStop === false) {

						curNode = curNode.parentNode.closest('.node');

						if (!curNode) {
							canStop = true;
						} else {

							if (false === draggedNode.contains(curNode)) {
								curNode.classList.add(_Dragndrop.cssClasses.dragShowDropTargetAfterClass);
							}
							canStop = curNode.nextElementSibling?.classList.contains('node') ?? false;
						}
					}
				}

				e.dataTransfer.dropEffect = _Dragndrop.pages.getDropEffect(nodeContainer, entity.id, true);

				if (e.dataTransfer.dropEffect === 'none') {
					nodeContainer.classList.add(_Dragndrop.cssClasses.backgroundDropForbidden);
				} else {
					node.classList.add(_Dragndrop.cssClasses.backgroundNodeHighlight);
				}

				if (_Dragndrop.pages.isDraggedNodeParentOfTargetNode(entity.id)) {
					return false;
				}

				// If the target node has children (and is expanded), find the first child of the target node and highlight only the "before" element!
				// UNLESS that child is the currently dragged node
				if (_Dragndrop.showRelativeDropTargets) {

					let firstChild = node.querySelector(':scope .node');
					if (firstChild && (_Dragndrop.dragEntity.id !== Structr.getId(firstChild))) {
						firstChild.classList.add(_Dragndrop.cssClasses.dragShowDropTargetBeforeClass);
					}
				}

				// preventDefault indicates that this is a valid drop target
				e.preventDefault();

				node.classList.add(_Dragndrop.cssClasses.dragOverClass);
				if (_Dragndrop.showRelativeDropTargets) {

					node.classList.add(_Dragndrop.cssClasses.dragShowDropTargetBeforeClass);

					if (nodeHasVisibleChildren === false) {
						node.classList.add(_Dragndrop.cssClasses.dragShowDropTargetAfterClass);
					}
				}

				if (entityIsContentNode) {
					nodeContainer.classList.add(_Dragndrop.cssClasses.dragNodeDropNotPossibleClass);
				} else {
					_Dragndrop.setDragHover(nodeContainer);
				}
			});

			nodeContainer.addEventListener('dragover', (e) => {

				if (!_Dragndrop.dragEntity) {
					return;
				}

				e.preventDefault();
				e.stopPropagation();
			});

			if (!entityIsContentNode) {

				nodeContainer.addEventListener('drop', async (e) => {
					e.preventDefault();
					e.stopPropagation();

					if (_Dragndrop.pages.isDraggedNodeParentOfTargetNode(entity.id)) {
						return false;
					}

					let newParentNodeId = entity.id;
					let inSamePage      = _Dragndrop.pages.draggedNodeStillInSamePage(newParentNodeId);

					await _Dragndrop.dropActions.baseDropAction({ newParentNodeId, inSamePage });

					return false;
				});
			}

			if (beforeNodeElement) {

				beforeNodeElement.addEventListener('dragenter', (e) => {
					e.preventDefault();
					e.stopPropagation();

					_Dragndrop.removeBackgroundClasses();

					node.classList.add(_Dragndrop.cssClasses.backgroundNodeHighlight);

					e.dataTransfer.dropEffect = _Dragndrop.pages.getDropEffect(nodeContainer, entity.id, false);

					beforeNodeElement.classList.add(_Dragndrop.cssClasses.dragOverClass);
					_Dragndrop.clearDragHover();
				});

				beforeNodeElement.addEventListener('dragover', (e) => {
					e.preventDefault();
					e.stopPropagation();

					e.dataTransfer.dropEffect = _Dragndrop.pages.getDropEffect(nodeContainer, entity.id, false);
				});

				beforeNodeElement.addEventListener('dragleave', (e) => {
					e.preventDefault();
					e.stopPropagation();

					beforeNodeElement.classList.remove(_Dragndrop.cssClasses.dragOverClass);
				});

				beforeNodeElement.addEventListener('drop', (e) => {
					e.preventDefault();
					e.stopPropagation();

					let newParentNodeId = Structr.getId(node.parentNode);
					let inSamePage      = _Dragndrop.pages.draggedNodeStillInSamePage(entity.id);

					_Dragndrop.dropActions.baseDropAction({ newParentNodeId, inSamePage, relativePosition: 'Before', refId: entity.id });

					return false;
				});
			}

			if (afterNodeElement) {

				afterNodeElement.addEventListener('dragenter', (e) => {
					e.preventDefault();
					e.stopPropagation();

					_Dragndrop.removeBackgroundClasses();

					node.classList.add(_Dragndrop.cssClasses.backgroundNodeHighlight);

					e.dataTransfer.dropEffect = _Dragndrop.pages.getDropEffect(nodeContainer, entity.id, false);

					afterNodeElement.classList.add(_Dragndrop.cssClasses.dragOverClass);
					_Dragndrop.clearDragHover();
				});

				afterNodeElement.addEventListener('dragover', (e) => {
					e.preventDefault();
					e.stopPropagation();

					e.dataTransfer.dropEffect = _Dragndrop.pages.getDropEffect(nodeContainer, entity.id, false);
				});

				afterNodeElement.addEventListener('dragleave', (e) => {
					e.preventDefault();
					e.stopPropagation();

					afterNodeElement.classList.remove(_Dragndrop.cssClasses.dragOverClass);
				});

				afterNodeElement.addEventListener('drop', async (e) => {
					e.preventDefault();
					e.stopPropagation();

					let newParentNodeId = Structr.getId(node.parentNode);
					let inSamePage      = _Dragndrop.pages.draggedNodeStillInSamePage(entity.id);

					await _Dragndrop.dropActions.baseDropAction({ newParentNodeId, inSamePage, relativePosition: 'After', refId: entity.id });

					return false;
				});
			}
		},
		enableEventMappingDroppable: (entity, domElement, customDropAction) => {

			let cnt = 0;

			domElement.addEventListener('dragenter', (e) => {
				cnt++;
				if (!_Dragndrop.dragEntity || !_Dragndrop.dragActive) {
					return;
				}

				e.stopPropagation();
				e.preventDefault();

				_Dragndrop.clearDragNDropClasses();

				let dropNotAllowed = _Pages.sharedComponents.isNodeInSharedComponents(Structr.node(_Dragndrop.dragEntity.id)[0]);
				if (dropNotAllowed) {

					e.dataTransfer.dropEffect = 'none';

				} else {

					e.dataTransfer.dropEffect = 'move';
				}

				domElement.classList.add(_Dragndrop.cssClasses.dragOverClass);

				if (dropNotAllowed) {
					domElement.classList.add(_Dragndrop.cssClasses.dragNodeDropNotPossibleClass);
				} else {
					_Dragndrop.setDragHover(domElement);
				}
			});

			domElement.addEventListener('dragover', (e) => {

				if (!_Dragndrop.dragEntity || !_Dragndrop.dragActive) {
					return;
				}

				let dropNotAllowed = _Pages.sharedComponents.isNodeInSharedComponents(Structr.node(_Dragndrop.dragEntity.id)[0]);
				if (!dropNotAllowed) {
					// preventDefault indicates that this is a valid drop target
					e.preventDefault();
				}

				e.stopPropagation();
			});

			domElement.addEventListener('dragleave', (e) => {
				cnt--;
				if (!_Dragndrop.dragEntity || !_Dragndrop.dragActive) {
					return;
				}

				if (cnt === 0) {
					domElement.classList.remove(_Dragndrop.cssClasses.dragOverClass);
					_Dragndrop.clearDragHover();
				}

				e.preventDefault();
				e.stopPropagation();
			});

			domElement.addEventListener('drop', async (e) => {
				e.preventDefault();
				e.stopPropagation();

				await customDropAction({ draggedEntity: _Dragndrop.dragEntity, targetEntity: entity });

				return false;
			});
		},
		enableNewSharedComponentDropzone: () => {

			let newComponentDropzone = _Pages.sharedComponents.getNewSharedComponentDropzone();
			let cnt = 0;

			newComponentDropzone?.addEventListener('dragenter', (e) => {
				cnt++;
				e.preventDefault();
				e.stopPropagation();

				_Dragndrop.clearDragNDropClasses();

				newComponentDropzone.classList.add(_Dragndrop.cssClasses.dragOverClass);
				_Dragndrop.setDragHover(newComponentDropzone);
			});

			newComponentDropzone?.addEventListener('dragover', (e) => {
				e.stopPropagation();

				if (_Pages.sharedComponents.isDropAllowed()) {
					// preventDefault indicates that this is a valid drop target
					e.preventDefault();
				}

				return true;
			});

			newComponentDropzone?.addEventListener('dragleave', (e) => {
				cnt--;
				e.stopPropagation();
				e.preventDefault();

				if (cnt === 0) {
					newComponentDropzone.classList.remove(_Dragndrop.cssClasses.dragOverClass);
					_Dragndrop.clearDragHover();
				}

				return true;
			});

			newComponentDropzone?.addEventListener('drop', (e) => {
				e.stopPropagation();

				// Create shadow page if not existing
				Structr.ensureShadowPageExists().then(() => {
					_Pages.sharedComponents.createNew(_Dragndrop.dragEntity.id);
				});

				return false;
			});
		}
	},
	files: {
		draggedEntityIds: [],
		enableDroppable: (entity, node) => {

			let entityIsFileNode = (entity.isFile === true);
			let cnt = 0;

			node.addEventListener('dragenter', (e) => {
				cnt++;
				if (!_Dragndrop.files.draggedEntityIds || !_Dragndrop.dragActive) {
					return;
				}

				e.stopPropagation();

				_Dragndrop.clearDragNDropClasses();

				e.dataTransfer.dropEffect = (entityIsFileNode ? 'none' : 'copy');

				if (e.dataTransfer.dropEffect === 'none') {
					node.classList.add(_Dragndrop.cssClasses.backgroundDropForbidden);
				} else {
					node.classList.add(_Dragndrop.cssClasses.backgroundNodeHighlight);
				}

				if (!entityIsFileNode) {
					// preventDefault indicates that this is a valid drop target
					e.preventDefault();
				}

				node.classList.add(_Dragndrop.cssClasses.dragOverClass);

				if (entityIsFileNode) {
					node.classList.add(_Dragndrop.cssClasses.dragNodeDropNotPossibleClass);
				} else {
					_Dragndrop.setDragHover(node);
				}
			});

			node.addEventListener('dragover', (e) => {

				if (!_Dragndrop.files.draggedEntityIds || !_Dragndrop.dragActive) {
					return;
				}

				e.preventDefault();
				e.stopPropagation();
			});

			node.addEventListener('dragleave', (e) => {
				cnt--;
				if (!_Dragndrop.files.draggedEntityIds || !_Dragndrop.dragActive) {
					return;
				}

				e.preventDefault();
				e.stopPropagation();

				if (cnt === 0) {
					_Dragndrop.removeBackgroundFromNode(node);
				}
			});

			if (!entityIsFileNode) {

				node.addEventListener('drop', async (e) => {

					e.preventDefault();
					e.stopPropagation();

					await _Dragndrop.dropActions.baseDropAction({ targetEntity: entity });

					return false;
				});
			}
		},
		isAnyDraggedEntityAParentOfTarget: (target) => {

			let anyDraggedElementInTargetHierarchy = false;
			let draggedFolderIds                  = _Dragndrop.files.draggedEntityIds.filter(id => StructrModel.obj(id).isFolder);

			// can only happen for folders
			if (draggedFolderIds.length > 0) {

				let curTargetEntity = target;
				while (curTargetEntity && anyDraggedElementInTargetHierarchy === false) {

					anyDraggedElementInTargetHierarchy = draggedFolderIds.includes(curTargetEntity.id);

					// walk up hierarchy
					curTargetEntity = StructrModel.obj(curTargetEntity.parentId);
				}
			}

			return anyDraggedElementInTargetHierarchy;
		},
		enableTreeElementDroppable: (entity, element) => {

			let highlightElement = element.querySelector('.jstree-wholerow');

			element.addEventListener('dragenter', (e) => {

				if (!_Dragndrop.files.draggedEntityIds || !_Dragndrop.dragActive) {
					return;
				}

				e.stopPropagation();
				e.preventDefault();

				// Determine if the target entity is a child of any of the dragged elements ==> do not allow
				let anyDraggedElementInTargetHierarchy = _Dragndrop.files.isAnyDraggedEntityAParentOfTarget(entity);

				_Dragndrop.clearDragNDropClasses();

				if (anyDraggedElementInTargetHierarchy) {

					e.dataTransfer.dropEffect = 'none';
					highlightElement.classList.add(_Dragndrop.cssClasses.backgroundDropForbidden);

				} else {

					e.dataTransfer.dropEffect = 'move';
					highlightElement.classList.add(_Dragndrop.cssClasses.backgroundNodeHighlight);
				}

				element.classList.add(_Dragndrop.cssClasses.dragOverClass);

				if (anyDraggedElementInTargetHierarchy) {
					element.classList.add(_Dragndrop.cssClasses.dragNodeDropNotPossibleClass);
				} else {
					_Dragndrop.setDragHover(element);
				}
			});

			element.addEventListener('dragover', (e) => {

				if (!_Dragndrop.files.draggedEntityIds || !_Dragndrop.dragActive) {
					return;
				}

				// Determine if the target entity is a child of any of the dragged elements ==> do not allow
				let anyDraggedElementInTargetHierarchy = _Dragndrop.files.isAnyDraggedEntityAParentOfTarget(entity);

				if (!anyDraggedElementInTargetHierarchy) {
					// preventDefault indicates that this is a valid drop target
					e.preventDefault();
				}

				e.stopPropagation();
			});

			element.addEventListener('dragleave', (e) => {

				if (!_Dragndrop.files.draggedEntityIds || !_Dragndrop.dragActive) {
					return;
				}

				e.preventDefault();
				e.stopPropagation();

				// cannot use simple method because this runs after dragenter for next node
				_Dragndrop.removeBackgroundFromNode(element);
			});

			element.addEventListener('drop', async (e) => {
				e.preventDefault();
				e.stopPropagation();

				await _Dragndrop.dropActions.baseDropAction({ targetEntity: entity });

				return false;
			});
		},
	},
	contents: {
		enableDroppable: (entity, node) => {

			let entityIsContentContainer = (entity.isContentContainer === true || entity.id === 'root');
			let cnt = 0;

			node.addEventListener('dragenter', (e) => {

				cnt++;
				if (!_Dragndrop.contents.draggedEntityIds || !_Dragndrop.dragActive) {
					return;
				}

				e.stopPropagation();
				e.preventDefault();

				_Dragndrop.clearDragNDropClasses();

				node.classList.add(_Dragndrop.cssClasses.dragOverClass);

				if (entityIsContentContainer) {

					e.dataTransfer.dropEffect = 'copy';
					node.classList.add(_Dragndrop.cssClasses.backgroundNodeHighlight);
					_Dragndrop.setDragHover(node);

				} else {

					e.dataTransfer.dropEffect = 'none';
					node.classList.add(_Dragndrop.cssClasses.backgroundDropForbidden);
					node.classList.add(_Dragndrop.cssClasses.dragNodeDropNotPossibleClass);
				}
			});

			node.addEventListener('dragover', (e) => {

				if (!_Dragndrop.contents.draggedEntityIds || !_Dragndrop.dragActive) {
					return;
				}

				if (entityIsContentContainer) {
					// preventDefault indicates that this is a valid drop target
					e.preventDefault();
				}

				e.stopPropagation();
			});

			node.addEventListener('dragleave', (e) => {
				cnt--;
				if (!_Dragndrop.contents.draggedEntityIds || !_Dragndrop.dragActive) {
					return;
				}

				e.preventDefault();
				e.stopPropagation();

				if (cnt === 0) {
					_Dragndrop.removeBackgroundFromNode(node);
				}
			});

			if (entityIsContentContainer) {

				node.addEventListener('drop', async (e) => {

					e.preventDefault();
					e.stopPropagation();

					await _Dragndrop.dropActions.baseDropAction({ targetEntity: entity });

					return false;
				});
			}
		},
		isAnyDraggedEntityAParentOfTarget: (targetEntity) => {

			let anyDraggedElementInTargetHierarchy = false;
			let draggedContainerIds                = _Dragndrop.contents.draggedEntityIds.filter(id => StructrModel.obj(id).isContentContainer);

			// can only happen for containers
			if (draggedContainerIds.length > 0) {

				let curTargetEntity = targetEntity;
				while (curTargetEntity && anyDraggedElementInTargetHierarchy === false) {

					anyDraggedElementInTargetHierarchy = draggedContainerIds.includes(curTargetEntity.id);

					// walk up hierarchy
					curTargetEntity = StructrModel.obj(curTargetEntity.parent?.id);
				}
			}

			return anyDraggedElementInTargetHierarchy;
		},
		enableTreeElementDroppable: (entity, element) => {

			let highlightElement = element.querySelector('.jstree-wholerow');

			element.addEventListener('dragenter', (e) => {

				if (!_Dragndrop.contents.draggedEntityIds || !_Dragndrop.dragActive) {
					return;
				}

				e.stopPropagation();
				e.preventDefault();

				_Dragndrop.clearDragNDropClasses();

				element.classList.add(_Dragndrop.cssClasses.dragOverClass);

				let isDraggedElementInTargetHierarchy = _Dragndrop.contents.isAnyDraggedEntityAParentOfTarget(entity);

				if (isDraggedElementInTargetHierarchy) {

					e.dataTransfer.dropEffect = 'none';
					highlightElement.classList.add(_Dragndrop.cssClasses.backgroundDropForbidden);
					element.classList.add(_Dragndrop.cssClasses.dragNodeDropNotPossibleClass);

				} else {

					e.dataTransfer.dropEffect = 'move';
					highlightElement.classList.add(_Dragndrop.cssClasses.backgroundNodeHighlight);
					_Dragndrop.setDragHover(element);
				}
			});

			element.addEventListener('dragover', (e) => {

				if (!_Dragndrop.contents.draggedEntityIds || !_Dragndrop.dragActive) {
					return;
				}

				let isDraggedElementInTargetHierarchy = _Dragndrop.contents.isAnyDraggedEntityAParentOfTarget(entity);

				if (!isDraggedElementInTargetHierarchy) {
					// preventDefault indicates that this is a valid drop target
					e.preventDefault();
				}

				e.stopPropagation();
			});

			element.addEventListener('dragleave', (e) => {

				if (!_Dragndrop.contents.draggedEntityIds || !_Dragndrop.dragActive) {
					return;
				}

				e.preventDefault();
				e.stopPropagation();

				// cannot use simple method because this runs after dragenter for next node
				_Dragndrop.removeBackgroundFromNode(element);
			});

			element.addEventListener('drop', async (e) => {
				e.preventDefault();
				e.stopPropagation();

				await _Dragndrop.dropActions.baseDropAction({ targetEntity: entity });

				return false;
			});
		},
	},
	dropActions: {
		baseDropAction: async (data) => {

			if (_Dragndrop.dragElementDropCallback) {

				await _Dragndrop.dragElementDropCallback(data);

			} else {

				console.log('No drop handler set for data: ', data);
			}
		},
		domElement: async ({ newParentNodeId, inSamePage, relativePosition, refId }) => {

			let isInTrash         = (_Dragndrop.dragEntity.pageId === null);
			let isSharedComponent = (_Pages.shadowPage && _Dragndrop.dragEntity.pageId === _Pages.shadowPage.id);

			if (relativePosition) {

				if (inSamePage === true || isInTrash === true) {

					await Command.insertRelative(_Dragndrop.dragEntity.id, newParentNodeId, refId, relativePosition);

				} else if (isSharedComponent) {

					await Command.cloneComponent(_Dragndrop.dragEntity.id, newParentNodeId, refId, relativePosition);

				} else {

					await Command.cloneNode(_Dragndrop.dragEntity.id, newParentNodeId, true, refId, relativePosition);
				}

			} else {

				if (inSamePage === true || isInTrash === true) {

					await Command.appendChild(_Dragndrop.dragEntity.id, newParentNodeId, undefined);

				} else if (isSharedComponent) {

					await Command.cloneComponent(_Dragndrop.dragEntity.id, newParentNodeId);

				} else {

					await Command.cloneNode(_Dragndrop.dragEntity.id, newParentNodeId, true);
				}
			}

			if (isInTrash) {

				_Pages.unattachedNodes.reload();
			}
		},
		widget: ({ newParentNodeId, relativePosition }) => {

			if (relativePosition) {

				console.log('not yet implemented');

			} else {

				let targetObj = StructrModel.obj(newParentNodeId);
				let pageId    = targetObj.isPage ? targetObj.id : targetObj.pageId;

				_Widgets.insertWidgetIntoPage(_Dragndrop.dragEntity, targetObj, pageId);
			}
		},
		file: ({ targetEntity }) => {

			if (targetEntity.type === 'fake' && targetEntity.id === 'favorites') {

				for (let dragId of _Dragndrop.files.draggedEntityIds) {

					let obj = StructrModel.obj(dragId);
					if (obj.isFile) {

						Command.favorites('add', obj.id, () => {
							_Helpers.blinkGreen(Structr.node(obj.id));
						});

					} else {

						_Helpers.blinkRed(Structr.node(obj.id));
					}
				}

			} else {

				let targetId = (targetEntity.type === 'fake' && targetEntity.id === 'root') ? null : targetEntity.id;

				_Files.handleMoveObjectsAction(targetId, _Dragndrop.files.draggedEntityIds);
			}
		},
		contents: ({ targetEntity }) => {

			let targetId = (targetEntity.type === 'fake' && targetEntity.id === 'root') ? null : targetEntity.id;

			_Contents.handleMoveObjectsAction(targetId, _Dragndrop.contents.draggedEntityIds);
		}
	}
};