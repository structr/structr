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
document.addEventListener("DOMContentLoaded", () => {
	Structr.registerModule(_Pages);
	Structr.classes.push('page');
});

let _Pages = {
	_moduleName: 'pages',
	urlHashKey: 'structrUrlHashKey_' + location.port,
	activeTabRightKey: 'structrActiveTabRight_' + location.port,
	activeTabLeftKey: 'structrActiveTabLeft_' + location.port,
	leftTabMinWidth: 410,
	rightTabMinWidth: 400,
	selectedTypeKey: 'structrSelectedType_' + location.port,
	autoRefreshDisabledKey: 'structrAutoRefreshDisabled_' + location.port,
	detailsObjectIdKey: 'structrDetailsObjectId_' + location.port,
	requestParametersKey: 'structrRequestParameters_' + location.port,
	pagesResizerLeftKey: 'structrPagesResizerLeftKey_' + location.port,
	pagesResizerRightKey: 'structrPagesResizerRightKey_' + location.port,
	functionBarSwitchKey: 'structrFunctionBarSwitchKey_' + location.port,

	shadowPage: undefined,

	pagesSlideout: undefined,
	localizationsSlideout: undefined,

	widgetsSlideout: undefined,
	paletteSlideout: undefined,
	componentsSlideout: undefined,
	unusedElementsSlideout: undefined,
	previewSlideout: undefined,

	components: undefined,
	unusedElementsTree: undefined,

	pagesTree: undefined,
	centerPane: undefined,

	textBeforeEditing: undefined,

	contentSourceId: undefined,

	init: () => {

		_Pager.initPager('pages',   'Page', 1, 25, 'name', 'asc');
		_Pager.initPager('files',   'File', 1, 25, 'name', 'asc');
		_Pager.initPager('folders', 'Folder', 1, 25, 'name', 'asc');
		_Pager.initPager('images',  'Image', 1, 25, 'name', 'asc');

		Structr.ensureShadowPageExists();
	},
	resize: () => {
		_Pages.resizeColumns();
	},
	dialogSizeChanged: () => {
		_Editors.resizeVisibleEditors();
	},
	unload: () => {
		document.querySelector('body').style.position = null;
	},
	onload: () => {

		// if removed, pages has a horizontal scrollbar caused by the right slideouts (is removed in "unload" method)
		document.querySelector('body').style.position = 'fixed';

		let urlHash = LSWrapper.getItem(_Pages.urlHashKey);
		if (urlHash) {
			Structr.mainMenu.isBlocked = false;
			window.location.hash       = urlHash;
		}

		Structr.setMainContainerHTML(_Pages.templates.main());

		_Pages.init();

		Structr.updateMainHelpLink(_Helpers.getDocumentationURLForTopic('pages'));

		_Pages.pagesSlideout           = $('#pages');
		_Pages.localizationsSlideout   = $('#localizations');
		_Pages.pagesTree               = $('#pagesTree', _Pages.pagesSlideout);
		_Pages.centerPane              = document.querySelector('#center-pane');
		_Pages.previews.previewElement = document.querySelector('#previews');
		_Pages.widgetsSlideout         = $('#widgetsSlideout');
		_Pages.paletteSlideout         = $('#palette');
		_Pages.componentsSlideout      = $('#components');
		_Pages.unusedElementsSlideout  = $('#elements');
		_Pages.unusedElementsTree      = $('#elementsArea', _Pages.unusedElementsSlideout);
		_Pages.previewSlideout         = $('#previewSlideout');

		let pagesTab = document.getElementById('pagesTab');
		let pagesTabSlideoutAction = (e) => {
			Structr.slideouts.leftSlideoutTrigger(pagesTab, _Pages.pagesSlideout, [_Pages.localizationsSlideout], () => {
				LSWrapper.setItem(_Pages.activeTabLeftKey, pagesTab.id);
				Structr.resize();
				_Entities.highlightSelectedElementOnSlideoutOpen();
			}, _Pages.leftSlideoutClosedCallback);
		};

		pagesTab.addEventListener('click', pagesTabSlideoutAction);

		$(pagesTab).droppable({
			tolerance: 'touch',
			//over: pagesTabSlideoutAction
		});

		let localizationsTab = document.getElementById('localizationsTab');
		localizationsTab.addEventListener('click', () => {
			Structr.slideouts.leftSlideoutTrigger(localizationsTab, _Pages.localizationsSlideout, [_Pages.pagesSlideout], () => {
				LSWrapper.setItem(_Pages.activeTabLeftKey, localizationsTab.id);
				_Pages.localizations.refreshPagesForLocalizationPreview();
				Structr.resize();
				_Entities.highlightSelectedElementOnSlideoutOpen();
			}, _Pages.leftSlideoutClosedCallback);
		});

		document.querySelector('#localizations input.locale').addEventListener('keydown', (e) => {
			if (e.which === 13) {
				_Pages.localizations.refreshLocalizations();
			}
		});

		let refreshLocalizationsButton = document.querySelector('#localizations button.refresh');
		refreshLocalizationsButton.addEventListener('click', _Pages.localizations.refreshLocalizations);

		_Helpers.appendInfoTextToElement({
			element: refreshLocalizationsButton,
			text: "On this tab you can load the localizations requested for the given locale on the currently previewed page (including the UUID of the details object and the query parameters which are also used for the preview).<br><br>The retrieval process works just as rendering the page. If you request the locale \"en_US\" you might get Localizations for \"en\" as a fallback if no exact match is found.<br><br>If no Localization could be found, an empty input field is rendered where you can quickly create the missing Localization.",
			insertAfter: true,
			helpElementCss: { width: "300px" },
			offsetX: -20,
			offsetY: 10
		});

		let widgetsTab = document.getElementById('widgetsTab');
		widgetsTab.addEventListener('click', () => {
			Structr.slideouts.rightSlideoutClickTrigger(widgetsTab, _Pages.widgetsSlideout, [_Pages.paletteSlideout, _Pages.componentsSlideout, _Pages.unusedElementsSlideout, _Pages.previewSlideout], (params) => {
				LSWrapper.setItem(_Pages.activeTabRightKey, widgetsTab.id);
				_Widgets.reloadWidgets();
				Structr.resize();
			}, _Pages.rightSlideoutClosedCallback);
		});

		let paletteTab = document.getElementById('paletteTab');
		paletteTab.addEventListener('click', () => {
			Structr.slideouts.rightSlideoutClickTrigger(paletteTab, _Pages.paletteSlideout, [_Pages.widgetsSlideout, _Pages.componentsSlideout, _Pages.unusedElementsSlideout, _Pages.previewSlideout], (params) => {
				LSWrapper.setItem(_Pages.activeTabRightKey, paletteTab.id);
				_Pages.designTools.reload();
				Structr.resize();
			}, _Pages.rightSlideoutClosedCallback);
		});

		let componentsTab = document.getElementById('componentsTab');
		let componentsTabSlideoutAction = (isDragOpen = false) => {
			Structr.slideouts.rightSlideoutClickTrigger(componentsTab, _Pages.componentsSlideout, [_Pages.widgetsSlideout, _Pages.paletteSlideout, _Pages.unusedElementsSlideout, _Pages.previewSlideout], (params) => {
				LSWrapper.setItem(_Pages.activeTabRightKey, componentsTab.id);
				_Pages.sharedComponents.reload(isDragOpen);
				Structr.resize();
			}, () => {
				_Helpers.fastRemoveAllChildren(_Pages.componentsSlideout[0]);
				_Pages.rightSlideoutClosedCallback();
			});
		};
		componentsTab.addEventListener('click', componentsTabSlideoutAction);

		$(componentsTab).droppable({
			tolerance: 'touch',
			over: (e, ui) => {

				let isComponentsSlideoutOpen = _Pages.componentsSlideout.hasClass('open');
				let isColumnResizer          = $(ui.draggable).hasClass('column-resizer');

				if (!isComponentsSlideoutOpen && !isColumnResizer) {
					componentsTabSlideoutAction(true);
				}
			}
		});

		let elementsTab = document.getElementById('elementsTab');
		elementsTab.addEventListener('click', () => {
			Structr.slideouts.rightSlideoutClickTrigger(elementsTab, _Pages.unusedElementsSlideout, [_Pages.widgetsSlideout, _Pages.paletteSlideout, _Pages.componentsSlideout, _Pages.previewSlideout], (params) => {
				LSWrapper.setItem(_Pages.activeTabRightKey, elementsTab.id);
				_Pages.unattachedNodes.reload();
				Structr.resize();
			}, () => {
				_Pages.unattachedNodes.removeElementsFromUI();
				_Pages.rightSlideoutClosedCallback();
			});
		});

		let previewTab = document.getElementById('previewTab');
		previewTab.addEventListener('click', () => {
			Structr.slideouts.rightSlideoutClickTrigger(previewTab, _Pages.previewSlideout, [_Pages.widgetsSlideout, _Pages.paletteSlideout, _Pages.componentsSlideout, _Pages.unusedElementsSlideout], (params) => {
				LSWrapper.setItem(_Pages.activeTabRightKey, previewTab.id);
				_Pages.previews.updatePreviewSlideout();
				Structr.resize();
			}, () => {
				_Pages.rightSlideoutClosedCallback();
			});
		});

		_Pages.refresh();
	},
	getContextMenuElements: (div, entity) => {

		let elements      = [];
		const isPage      = (entity.type === 'Page');
		const isContent   = (entity.type === 'Content');
		const hasChildren = (entity.children && entity.children.length > 0);

		let handleInsertHTMLAction = (itemText) => {
			let pageId  = isPage ? entity.id : entity.pageId;
			let tagName = (itemText === 'comment') ? '#comment' : itemText;

			Command.createAndAppendDOMNode(pageId, entity.id, tagName, _Dragndrop.getAdditionalDataForElementCreation(tagName, entity.tag), _Elements.isInheritVisibilityFlagsChecked(), _Elements.isInheritGranteesChecked());
		};

		let handleInsertBeforeAction = (itemText) => {
			let tagName = (itemText === 'comment') ? '#comment' : itemText;
			Command.createAndInsertRelativeToDOMNode(entity.pageId, entity.id, tagName, _Dragndrop.getAdditionalDataForElementCreation(tagName, entity.tag), 'Before', _Elements.isInheritVisibilityFlagsChecked(), _Elements.isInheritGranteesChecked());
		};
		let handleInsertAfterAction  = (itemText) => {
			let tagName = (itemText === 'comment') ? '#comment' : itemText;
			Command.createAndInsertRelativeToDOMNode(entity.pageId, entity.id, tagName, _Dragndrop.getAdditionalDataForElementCreation(tagName, entity.tag), 'After', _Elements.isInheritVisibilityFlagsChecked(), _Elements.isInheritGranteesChecked());
		};
		let handleReplaceWithAction  = (itemText) => { Command.replaceWith(entity.pageId, entity.id, itemText, {}, _Elements.isInheritVisibilityFlagsChecked(), _Elements.isInheritGranteesChecked(), c => _Entities.toggleElement(c.id)); };
		let handleWrapInHTMLAction   = (itemText) => {

			_Dragndrop.storeTemporarilyRemovedElementUUID(entity.id);

			Command.wrapDOMNodeInNewDOMNode(entity.pageId, entity.id, itemText, {}, _Elements.isInheritVisibilityFlagsChecked(), _Elements.isInheritGranteesChecked());

			_Dragndrop.clearTemporarilyRemovedElementUUID();
		};

		if (!isContent) {

			elements.push({
				name: 'Insert HTML element',
				elements: !isPage ? _Elements.sortedElementGroups : ['html'],
				forcedClickHandler: handleInsertHTMLAction
			});

			elements.push({
				name: 'Insert content element',
				elements: !isPage ? ['#content', '#template'] : ['#template'],
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
				clickHandler: () => {
					Command.createAndAppendDOMNode(entity.pageId, entity.id, 'div', _Dragndrop.getAdditionalDataForElementCreation('div'), _Elements.isInheritVisibilityFlagsChecked(), _Elements.isInheritGranteesChecked());
				}
			});
		}

		_Elements.contextMenu.appendContextMenuSeparator(elements);

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
						elements: ['#content', '#template'],
						forcedClickHandler: handleInsertBeforeAction
					},
					{
						name: '... div element',
						clickHandler: () => {
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
						elements: ['#content', '#template'],
						forcedClickHandler: handleInsertAfterAction
					},
					{
						name: '... div element',
						clickHandler: () => {
							handleInsertAfterAction('div');
						}
					}
				]
			});

			_Elements.contextMenu.appendContextMenuSeparator(elements);
		}

		if (entity.type === 'Div' && !hasChildren) {

			elements.push({
				icon: _Icons.getMenuSvgIcon(_Icons.iconPencilEdit),
				name: 'Edit',
				clickHandler: () => {
					_Entities.editEmptyDiv(entity);
				}
			});
		}

		let hasParentAndParentIsNotPage = (entity.parent && entity.parent.type !== 'Page');
		let parentIsShadowPage          = (entity.parent === null && entity.pageId === _Pages.shadowPage.id);

		if (!isPage && hasParentAndParentIsNotPage || parentIsShadowPage) {

			elements.push({
				icon: _Icons.getMenuSvgIcon(_Icons.iconClone),
				name: 'Clone',
				clickHandler: () => {
					Command.cloneNode(entity.id, (entity.parent ? entity.parent.id : null), true);
				}
			});

			_Elements.contextMenu.appendContextMenuSeparator(elements);
		}

		if (!isPage && hasParentAndParentIsNotPage) {

			_Elements.contextMenu.appendContextMenuSeparator(elements);

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
						clickHandler: () => {
							handleWrapInHTMLAction('#template');
						}
					},
					{
						name: '... div element',
						clickHandler: () => {
							handleWrapInHTMLAction('div');
						}
					}
				]
			});

			elements.push({
				name: 'Replace element with...',
				elements: [
					{
						name: '... HTML element',
						elements: _Elements.sortedElementGroups,
						forcedClickHandler: handleReplaceWithAction
					},
					{
						name: '... Template element',
						clickHandler: () => {
							handleReplaceWithAction('#template');
						}
					},
					{
						name: '... div element',
						clickHandler: () => {
							handleReplaceWithAction('div');
						}
					}
				]
			});
		}

		if (isPage) {
			elements.push({
				icon: _Icons.getMenuSvgIcon(_Icons.iconClone),
				name: 'Clone Page',
				clickHandler: () => {
					Command.clonePage(entity.id);
				}
			});
		}

		if (!isPage) {

			_Elements.contextMenu.appendContextMenuSeparator(elements);

			if (_Elements.selectedEntity && _Elements.selectedEntity.id === entity.id) {
				elements.push({
					name: 'Deselect element',
					clickHandler: () => {
						_Elements.unselectEntity();
					}
				});
			} else {
				elements.push({
					name: 'Select element',
					clickHandler: () => {
						_Elements.selectEntity(entity);
					}
				});
			}

			let canConvertToSharedComponent = !entity.sharedComponentId && !entity.isPage && (entity.pageId !== _Pages.shadowPage.id || entity.parent !== null );
			if (canConvertToSharedComponent) {
				_Elements.contextMenu.appendContextMenuSeparator(elements);

				elements.push({
					name: 'Convert to Shared Component',
					clickHandler: () => {
						Command.createComponent(entity.id);
					}
				});
			}
		}

		if (!isContent && _Elements.selectedEntity && _Elements.selectedEntity.id !== entity.id) {

			let isSamePage = _Elements.selectedEntity.pageId === entity.pageId;
			let isThisEntityDirectParentOfSelectedEntity = (_Elements.selectedEntity.parent && _Elements.selectedEntity.parent.id === entity.id);
			let isSelectedEntityInShadowPage = _Elements.selectedEntity.pageId === _Pages.shadowPage.id;
			let isSelectedEntitySharedComponent = isSelectedEntityInShadowPage && !_Elements.selectedEntity.parent;

			let isDescendantOfSelectedEntity = function (possibleDescendant) {
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
					clickHandler: () => {
						Command.cloneComponent(_Elements.selectedEntity.id, entity.id);
						_Elements.unselectEntity();
					}
				});
			}

			if (!isPage || (isPage && !hasChildren && (_Elements.selectedEntity.tag === 'html' || _Elements.selectedEntity.type === 'Template'))) {
				elements.push({
					name: 'Clone selected element here',
					clickHandler: () => {
						Command.cloneNode(_Elements.selectedEntity.id, entity.id, true);
						_Elements.unselectEntity();
					}
				});
			}

			if (isSamePage && !isThisEntityDirectParentOfSelectedEntity && !isSelectedEntityInShadowPage && !isDescendantOfSelectedEntity(entity)) {
				elements.push({
					name: 'Move selected element here',
					clickHandler: () => {
						Command.appendChild(_Elements.selectedEntity.id, entity.id, entity.pageId);
						_Elements.unselectEntity();
					}
				});
			}
		}

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		if (!isContent && hasChildren) {

			elements.push({
				name: 'Expand / Collapse',
				elements: [
					{
						name: 'Expand subtree',
						clickHandler: () => {

							$(div).find('.node').each(function(i, el) {
								if (!_Entities.isExpanded(el)) {
									_Entities.toggleElement(entity.id, el);
								}
							});

							if (!_Entities.isExpanded(div)) {
								_Entities.toggleElement(entity.id, div);
							}
						}
					},
					{
						name: 'Expand subtree recursively',
						clickHandler: () => {
							_Entities.expandRecursively([entity.id]);
						}
					},
					{
						name: 'Collapse subtree',
						clickHandler: () => {

							$(div).find('.node').each(function(i, el) {
								if (_Entities.isExpanded(el)) {
									_Entities.toggleElement(entity.id, el);
								}
							});

							if (_Entities.isExpanded(div)) {
								_Entities.toggleElement(entity.id, div);
							}
						}
					}
				]
			});
		}

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		// DELETE AREA - ALWAYS AT THE BOTTOM
		// allow "Remove Node" on first level children of page
		if (!isPage && entity.parent !== null) {

			elements.push({
				icon: _Icons.getMenuSvgIcon(_Icons.iconTrashcan),
				classes: ['menu-bolder', 'danger'],
				name: 'Remove Node',
				clickHandler: () => {

					Command.removeChild(entity.id, () => {
						_Pages.unattachedNodes.blinkUI();
					});
				}
			});
		}

		// should only be visible for type===Page
		if (isPage || !entity.parent) {

			elements.push({
				icon: _Icons.getMenuSvgIcon(_Icons.iconTrashcan),
				classes: ['menu-bolder', 'danger'],
				name: `Delete ${entity.type}`,
				clickHandler: () => {
					_Entities.deleteNode(entity, (isContent === false));
				}
			});
		}

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		return elements;
	},
	prevAnimFrameReqId_moveLeftResizer: undefined,
	moveLeftResizer: (left) => {
		_Helpers.requestAnimationFrameWrapper(_Pages.prevAnimFrameReqId_moveLeftResizer, () => {
			_Pages.resizeColumns(left, null);
		});
	},
	prevAnimFrameReqId_moveRightResizer: undefined,
	moveRightResizer: (right) => {
		_Helpers.requestAnimationFrameWrapper(_Pages.prevAnimFrameReqId_moveRightResizer, () => {
			_Pages.resizeColumns(null, right);
		});
	},
	resizeColumns: (pxLeft, pxRight) => {

		if (!pxLeft && !pxRight) {
			pxLeft  = LSWrapper.getItem(_Pages.getLeftResizerKey())  || _Pages.leftTabMinWidth;
			pxRight = LSWrapper.getItem(_Pages.getRightResizerKey()) || _Pages.rightTabMinWidth;
		}

		let leftResizer       = document.querySelector('.column-resizer-left');
		let openLeftSlideout  = document.querySelector('.slideOutLeft.open');
		let rightResizer      = document.querySelector('.column-resizer-right');
		let openRightSlideout = document.querySelector('.slideOutRight.open');

		if (!openLeftSlideout) {
			pxLeft = 0;
		}
		if (!openRightSlideout) {
			pxRight = 0;
		}

		let leftPos  = pxLeft  ? pxLeft  : (openLeftSlideout  ? openLeftSlideout.getBoundingClientRect().right  : 0);
		let rightPos = pxRight ? pxRight : (openRightSlideout ? openRightSlideout.getBoundingClientRect().width : 0);

		let tabsMenu = document.querySelector('#function-bar .tabs-menu');

		if (leftResizer) {

			if (pxLeft) {

				leftResizer.style.left = `calc(${leftPos}px + 0rem)`;

				if (openLeftSlideout) openLeftSlideout.style.width = `calc(${leftPos}px - 3rem)`;

				_Pages.centerPane.style.marginLeft = `calc(${leftPos}px + 3rem)`;

				if (tabsMenu) tabsMenu.style.marginLeft = `calc(${leftPos}px + 2rem)`;

			} else {

				if (leftPos === 0) {

					if (tabsMenu) tabsMenu.style.marginLeft = `calc(${leftPos}px + 2rem)`;

					let columnResizerLeft = '4rem';
					leftResizer.style.left = columnResizerLeft;
					_Pages.centerPane.style.marginLeft = columnResizerLeft;

				} else {

					if (tabsMenu) tabsMenu.style.marginLeft = `calc(${leftPos}px + 0rem)`;

					leftResizer.style.left = `calc(${leftPos}px - 1rem)`;
					_Pages.centerPane.style.marginLeft = `calc(${leftPos}px + 2rem)`;
				}
			}
		}

		if (rightResizer) {

			if (pxRight) {

				rightResizer.style.left = `calc(${window.innerWidth - rightPos}px + 0rem)`;

				if (openRightSlideout) openRightSlideout.style.width = `calc(${rightPos}px - 7rem)`;

				_Pages.centerPane.style.marginRight = `calc(${rightPos}px - 1rem)`;

			} else {

				if (rightPos === 0) {

					let columnResizerRight = '4rem';
					rightResizer.style.left = columnResizerRight;
					_Pages.centerPane.style.marginRight = columnResizerRight;

				} else {

					rightResizer.style.left = `calc(${window.innerWidth - rightPos}px - 3rem)`;
					_Pages.centerPane.style.marginRight = `calc(${rightPos}px + 2rem)`;
				}
			}
		}

		_Editors.resizeVisibleEditors();
	},
	refresh: () => {

		_Helpers.fastRemoveAllChildren(_Pages.pagesTree[0]);

		Structr.setFunctionBarHTML(_Pages.templates.functions());

		UISettings.showSettingsForCurrentModule();

		Structr.resize();

		Structr.initVerticalSlider(Structr.mainContainer.querySelector('.column-resizer-left'), _Pages.getLeftResizerKey(), _Pages.leftTabMinWidth, _Pages.moveLeftResizer);
		Structr.initVerticalSlider(Structr.mainContainer.querySelector('.column-resizer-right'), _Pages.getRightResizerKey(), _Pages.rightTabMinWidth, _Pages.moveRightResizer, true);

		Structr.mainMenu.unblock(500);

		document.getElementById(_Pages.getActiveTabLeft())?.click();
		document.getElementById(_Pages.getActiveTabRight())?.click();

		_Pages.adaptFunctionBarTabs();

		for (const menuLink of document.querySelectorAll('#function-bar .tabs-menu li a')) {
			menuLink.onclick = (event) => _Pages.activateCenterPane(menuLink);
		}

		let pagerElement = document.querySelector('#pagesPager');
		let pPager = _Pager.addPager('pages', pagerElement, true, 'Page', null, null, null, null, true);

		pPager.cleanupFunction = () => {
			_Helpers.fastRemoveAllChildren(_Pages.pagesTree[0]);
		};

		pagerElement.insertAdjacentHTML('beforeend', `
			<div id="pagesPagerFilters">
				Filters: <input type="text" class="filter" data-attribute="name" placeholder="Name" title="Here you can filter the pages list by page name" autocomplete="new-password"/>
				<input type="text" class="filter page-label category-filter" data-attribute="category" placeholder="Category">
			</div>
		`);
		let filerEl = pagerElement.querySelector('#pagesPagerFilters');
		pPager.activateFilterElements(filerEl);
		pPager.setIsPaused(false);
		pPager.refresh();

		fetch(`${Structr.rootUrl}Page/category`).then((response) => {
			if (response.ok) {
				return response.json();
			}
		}).then((data) => {

			let categories = [];
			for (let page of data.result) {
				if (page.category !== null && categories.indexOf(page.category) === -1) {
					categories.push(page.category);
				}
			}
			categories.sort();

			let helpText = 'Filter pages by page category.';
			if (categories.length > 0) {
				helpText += 'Available categories: \n\n' + categories.join('\n');
			} else {
				helpText += '\nNo categories available - these can be set in the advanced attributes of a page.';
			}

			filerEl.querySelector('input.category-filter').title = helpText;
		});

		$('#import_page').on('click', (e) => {
			e.stopPropagation();

			let { dialogText } = _Dialogs.custom.openDialog('Import Template');

			dialogText.insertAdjacentHTML('beforeend', `
				<h3>Create page from source code ...</h3>
				<textarea id="_code" name="code" cols="40" rows="5" placeholder="Paste HTML code here"></textarea>

				<h3>... or fetch page from URL: <input id="_address" name="address" size="40" value="http://"></h3>
				<table class="props">
					<tr>
						<td><label for="name">Name of new page:</label></td>
						<td><input id="_name" name="name" size="20"></td></tr>
					<tr>
						<td><label for="publicVisibility">Visible to public</label></td>
						<td><input type="checkbox" id="_publicVisible" name="publicVisibility"></td></tr>
					<tr>
						<td><label for="authVisibilty">Visible to authenticated users</label></td>
						<td><input type="checkbox" id="_authVisible" name="authVisibilty"></td></tr>
					<tr>
						<td><label for="includeInExport">Include imported files in deployment export</label></td>
						<td><input type="checkbox" id="_includeInExport" name="includeInExport" checked="checked"></td></tr>
					<tr>
						<td><label for="processDeploymentInfo">Process deployment annotations</label></td>
						<td><input type="checkbox" id="_processDeploymentInfo" name="processDeploymentInfo"></td></tr>
				</table>
			`);

			$('#_address', $(dialogText)).on('blur', function() {
				let addr = $(this).val().replace(/\/+$/, "");
				$('#_name', $(dialogText)).val(addr.substring(addr.lastIndexOf("/") + 1));
			});

			let startImportButton = _Dialogs.custom.appendCustomDialogButton('<button class="action" id="startImport">Start Import</button>');

			startImportButton.addEventListener('click', (e) => {
				e.stopPropagation();

				let code                  = $('#_code', $(dialogText)).val();
				let address               = $('#_address', $(dialogText)).val();
				let name                  = $('#_name', $(dialogText)).val();
				let publicVisible         = $('#_publicVisible', $(dialogText)).prop('checked');
				let authVisible           = $('#_authVisible', $(dialogText)).prop('checked');
				let includeInExport       = $('#_includeInExport', $(dialogText)).prop('checked');
				let processDeploymentInfo = $('#_processDeploymentInfo', $(dialogText)).prop('checked');

				if (code.length > 0) {
					address = null;
				}

				return Command.importPage(code, address, name, publicVisible, authVisible, includeInExport, processDeploymentInfo);
			});
		});

		// Display 'Create Page' dialog
		let createPageButton = document.querySelector('#create_page');
		createPageButton.addEventListener('click', async (e) => {

			e.stopPropagation();

			let pageTemplates = await _Widgets.fetchAllPageTemplateWidgets();

			if (pageTemplates.length === 0) {

				Command.createSimplePage();

			} else {

				let { dialogText } = _Dialogs.custom.openDialog('Select Template to Create New Page');

				let container = _Helpers.createSingleDOMElementFromHTML('<div id="template-tiles"></div>');
				dialogText.appendChild(container);

				for (let widget of pageTemplates) {

					let id = 'create-from-' + widget.id;
					let tile = _Helpers.createSingleDOMElementFromHTML(`<div id="${id}" class="app-tile"><img src="${widget.thumbnailPath}"/><h4>${widget.name}</h4><p>${(widget.description || '')}</p></div>`);
					container.append(tile);

					tile.addEventListener('click', () => {
						Command.create({ type: 'Page' }, (page) => {
							Structr.removeExpandedNode(page.id);
							Command.appendWidget(widget.source, page.id, page.id, null, {}, true);
							_Dialogs.custom.dialogCancelBaseAction();
						});
					});

				}

				// default page
				let defaultTile = _Helpers.createSingleDOMElementFromHTML('<div id="create-simple-page" class="app-tile"><img src="https://apps.structr.com/assets/images/simple.png"/><h4>Simple Page</h4><p>Creates a simple page with a minimal set of HTML elements.</p></div>');
				container.append(defaultTile);

				let createSimplePageButton = container.querySelector('#create-simple-page');
				createSimplePageButton.addEventListener('click', () => {
					Command.createSimplePage();
					_Dialogs.custom.dialogCancelBaseAction();
				});
			}
		});

		Structr.adaptUiToAvailableFeatures();

		let selectedObjectId = LSWrapper.getItem(_Entities.selectedObjectIdKey);
		if (selectedObjectId) {

			fetch(Structr.rootUrl + selectedObjectId).then(response => {
				if (!response.ok) {
					// looks like element was deleted
					LSWrapper.removeItem(_Entities.selectedObjectIdKey);
				}
			});
		}
	},
	removePage: (page) => {
		Structr.removeExpandedNode(page.id);
	},
	deactivateAllFunctionBarTabs: () => {
		for (const otherTab of document.querySelectorAll('#function-bar .tabs-menu li.active')) {
			otherTab.classList.remove('active');
		}
	},
	hideAllFunctionBarTabs: () => {
		for (const otherTab of document.querySelectorAll('#function-bar .tabs-menu li')) {
			otherTab.classList.add('hidden');
		}
	},
	selectedObjectWasDeleted: () => {
		_Pages.emptyCenterPane();
		_Pages.adaptFunctionBarTabs();
	},
	showTabsMenu: () => {
		let tabsMenu = document.querySelector('#function-bar .tabs-menu');
		if (tabsMenu) {
			tabsMenu.style.display = 'inline-block';
		}
	},
	adaptFunctionBarTabs: (entity) => {

		if (entity) {

			_Pages.showTabsMenu();

			// first show everything - later hide some
			for (let li of document.querySelectorAll('.tabs-menu li.hidden')) {
				li.classList.remove('hidden');
			}

			switch (entity.type) {
				case 'Page':
					document.querySelector('a[href="#pages:html"]').closest('li').classList.add('hidden');
					document.querySelector('a[href="#pages:editor"]').closest('li').classList.add('hidden');
					document.querySelector('a[href="#pages:repeater"]').closest('li').classList.add('hidden');
					document.querySelector('a[href="#pages:events"]').closest('li').classList.add('hidden');
					break;

				default:

					if (entity.isContent) {
						document.querySelector('a[href="#pages:html"]').closest('li').classList.add('hidden');
						document.querySelector('a[href="#pages:editor"]').closest('li').classList.remove('hidden');
						document.querySelector('a[href="#pages:events"]').closest('li').classList.add('hidden');
					} else {
						document.querySelector('a[href="#pages:html"]').closest('li').classList.remove('hidden');
						document.querySelector('a[href="#pages:editor"]').closest('li').classList.add('hidden');
						document.querySelector('a[href="#pages:repeater"]').closest('li').classList.remove('hidden');
						document.querySelector('a[href="#pages:events"]').closest('li').classList.remove('hidden');
					}
			}

			if (!_Entities.isLinkableEntity(entity)) {
				document.querySelector('a[href="#pages:link"]').closest('li').classList.add('hidden');
			}

			// Create shadow page if not existing
			Structr.ensureShadowPageExists().then(() => {
				let isEntityInSharedComponents = (entity.pageId === _Pages.shadowPage.id);
				let isEntityInTrash = (!entity.isPage && !entity.pageId);
				if (isEntityInSharedComponents || isEntityInTrash) {
					document.querySelector('a[href="#pages:preview"]').closest('li').classList.add('hidden');
				}
			});

		} else {
			_Pages.hideAllFunctionBarTabs();
		}
	},
	activateSubmenuTabElement: (tab) => {
		_Pages.deactivateAllFunctionBarTabs();

		tab.classList.add('active');
	},
	activateSubmenuLink: (selector) => {
		let submenuLink = document.querySelector(`#function-bar .tabs-menu li a[href="${selector}"]`);
		if (submenuLink) {
			let tab = submenuLink.closest('li');
			_Pages.activateSubmenuTabElement(tab);
		}
	},
	activateCenterPane: (link) => {
		let tab    = link.closest('li');
		let active = tab.classList.contains('active');

		// return if clicked tab already is active
		if (active) return;

		_Pages.activateSubmenuTabElement(tab);

		let obj = _Entities.selectedObject;
		if (!obj || !obj.type) return;

		active = tab.classList.contains('active');

		_Pages.refreshCenterPane(obj, new URL(link.href).hash);
	},
	emptyCenterPane: () => {
		_Helpers.fastRemoveAllChildren(_Pages.centerPane);
	},
	refreshCenterPane: (obj, urlHash) => {

		_Entities.deselectAllElements();

		_Pages.centerPane.dataset['elementId'] = obj.id;

		if (_Pages.previewSlideout.hasClass('open')) {
			_Pages.previews.updatePreviewSlideout();
		}

		if (UISettings.getValueForSetting(UISettings.settingGroups.pages.settings.favorEditorForContentElementsKey) && (!urlHash && obj.isContent)) {
			/*
				if urlHash is given, user has manually selected a tab. if it is not given, user has selected a node
			*/
			urlHash = '#pages:editor';
		}

		if (UISettings.getValueForSetting(UISettings.settingGroups.pages.settings.favorHTMLForDOMNodesKey) && (!urlHash && obj.isDOMNode)) {
			/*
				if urlHash is given, user has manually selected a tab. if it is not given, user has selected a node
			*/
			urlHash = '#pages:html';
		}

		_Pages.emptyCenterPane();
		_Pages.adaptFunctionBarTabs(obj);

		if (!urlHash) {
			urlHash = new URL(location.href).hash;
		}
		let activeLink = document.querySelector(`#function-bar .tabs-menu li a[href="${urlHash}"]`);

		if (activeLink) {

			if (activeLink.closest('li').classList.contains('hidden')) {
				_Entities.deselectAllElements();
				activeLink = document.querySelector('#function-bar .tabs-menu li a');

				_Pages.activateSubmenuTabElement(activeLink.closest('li'));
			}

			urlHash = new URL(activeLink.href).hash;
			LSWrapper.setItem(_Pages.urlHashKey, urlHash);

		} else {

			urlHash = LSWrapper.getItem(_Pages.urlHashKey);
			if (!urlHash) {
				urlHash = new URL(window.location.href).hash;
			}
			// Activate submenu link
			activeLink = document.querySelector('#function-bar .tabs-menu li a[href="' + urlHash + '"]');

			if (activeLink) {
				if (activeLink.closest('li').classList.contains('hidden')) {
					activeLink = document.querySelector('#function-bar .tabs-menu li a');
				}

			} else {
				// activate first link
				activeLink = document.querySelector('#function-bar .tabs-menu li a');

				// TODO: or maybe better default tab for element? Page--> Preview, Content --> Editor
			}

			urlHash = new URL(activeLink.href).hash;
		}

		_Pages.activateSubmenuTabElement(activeLink.closest('li'));

		// Set default views based on object type
		if (urlHash === '#pages') {
			switch (obj.type) {
				case 'Page':
					urlHash = '#pages:preview';
					break;
				case 'Template':
				case 'Content':
					urlHash = '#pages:editor';
				default:
					urlHash = '#pages:basic';
			}
		}

		switch (urlHash) {

			case '#pages:basic':

				let dialogConfig = _Entities.basicTab.getBasicTabConfig(obj);

				_Pages.centerPane.insertAdjacentHTML('beforeend', _Pages.templates.basic());
				let basicContainer = document.querySelector('#center-pane .basic-container');

				if (dialogConfig) {
					dialogConfig.appendDialogForEntityToContainer($(basicContainer), obj).then(() => {
						_Helpers.activateCommentsInElement(basicContainer);
					});
				}

				break;

			case '#pages:html': {

				_Pages.centerPane.insertAdjacentHTML('beforeend', _Pages.templates.properties());
				let propertiesContainer = document.querySelector('#center-pane .properties-container');

				_Schema.getTypeInfo(obj.type, function(typeInfo) {

					_Entities.listProperties(obj, '_html_', $(propertiesContainer), typeInfo, function(properties) {

						// make container visible when custom properties exist
						if (Object.keys(properties).length > 0) {
							$('div#custom-properties-parent').removeClass("hidden");
						}

						$('input.dateField', $(propertiesContainer)).each(function(i, input) {
							_Entities.activateDatePicker($(input));
						});
					});
				});
				break;
			}

			case '#pages:advanced': {

				_Pages.centerPane.insertAdjacentHTML('beforeend', _Pages.templates.properties());
				let propertiesContainer = document.querySelector('#center-pane .properties-container');

				_Schema.getTypeInfo(obj.type, (typeInfo) => {

					_Entities.listProperties(obj, 'ui', $(propertiesContainer), typeInfo, (properties) => {

						// make container visible when custom properties exist
						if (Object.keys(properties).length > 0) {
							$('div#custom-properties-parent').removeClass("hidden");
						}

						$('input.dateField', $(propertiesContainer)).each(function(i, input) {
							_Entities.activateDatePicker($(input));
						});
					});
				});
				break;
			}

			case '#pages:preview':

				let pageId = null;

				if (obj.type === 'Page') {

					pageId = obj.id;

				} else if (obj.isDOMNode) {

					pageId = obj.pageId;
				}

				_Pages.previews.showPreviewInIframe(pageId, obj.id);

				break;

			case '#pages:editor':

				if (obj.isContent) {
					_Elements.displayCentralEditor(obj);
				} else {
					document.querySelector('[href="#pages:advanced"]').click();
				}
				break;

			case '#pages:repeater':

				_Pages.centerPane.insertAdjacentHTML('beforeend', _Pages.templates.repeater());
				let repeaterContainer = document.querySelector('#center-pane .repeater-container');
				_Entities.repeaterConfig(obj, repeaterContainer);
				break;

			case '#pages:events':

				_Pages.centerPane.insertAdjacentHTML('beforeend', _Pages.templates.events());
				let eventsContainer = document.querySelector('#center-pane .events-container');

				_Schema.getTypeInfo(obj.type, (typeInfo) => {
					_Pages.eventActionMappingDialog(obj, eventsContainer, typeInfo);
				});
				break;

			case '#pages:security':

				_Pages.centerPane.insertAdjacentHTML('beforeend', _Pages.templates.security());
				let securityContainer = document.querySelector('#center-pane .security-container');

				_Schema.getTypeInfo(obj.type, (typeInfo) => {
					_Entities.accessControlDialog(obj, $(securityContainer), typeInfo);
				});
				break;

			case '#pages:link':

				_Pages.centerPane.insertAdjacentHTML('beforeend', _Pages.templates.linkable());
				_Pages.linkableDialog.show(obj, _Pages.centerPane.querySelector('.linkable-container'));
				break;

			default:
				console.log('do something else, urlHash:', urlHash);
		}
	},
	appendPageElement: (entity) => {

		entity = StructrModel.ensureObject(entity);

		let hasChildren = entity.children && entity.children.length;

		if (!_Pages.pagesTree) return;

		if ($('#id_' + entity.id, _Pages.pagesTree).length > 0) {
			return;
		}
		let pageName = (entity.name ? entity.name : `[${entity.type}]`);

		_Pages.pagesTree.append(`
			<div id="id_${entity.id}" class="node page${entity.hidden ? ' is-hidden' : ''}">
				<div class="node-container flex items-center">
					${_Icons.getSvgIcon(_Icons.iconDOMTreePage, 16, 16, ['typeIcon', 'icon-grey'])}
					<span class="abbr-ellipsis abbr-pages-tree-page">
						<b title="${_Helpers.escapeForHtmlAttributes(entity.name)}" class="name_">${pageName}</b>
						<span class="position_">${(entity.position ? entity.position : '')}</span>
					</span>
					<div class="icons-container flex items-center"></div>
				</div>
			</div>
		`);

		let div            = Structr.node(entity.id);
		let nodeContainer  = $('.node-container', div);
		let iconsContainer = $('.icons-container', div);

		_Dragndrop.pages.enableDroppable(entity, div[0], nodeContainer[0]);

		_Entities.appendExpandIcon(nodeContainer, entity, hasChildren);

		_Entities.appendContextMenuIcon(iconsContainer, entity);
		_Pages.appendPagePreviewIcon(iconsContainer, entity);
		_Entities.appendNewAccessControlIcon(iconsContainer, entity);

		_Elements.contextMenu.enableContextMenuOnElement(div[0], entity);
		_Entities.setMouseOver(div);

		_Elements.clickOrSelectElementIfLastSelected(div, entity);

		return div;
	},
	appendPagePreviewIcon: (parent, page) => {

		let pagePreviewIconClass = 'svg_page_preview_icon';
		let icon = $('.' + pagePreviewIconClass, parent);

		if (!(icon && icon.length)) {

			icon = $(_Icons.getSvgIcon(_Icons.iconOpenInNewPage, 16, 16, _Icons.getSvgIconClassesNonColorIcon([pagePreviewIconClass, 'node-action-icon', 'mr-1'])));
			parent.append(icon);

			icon.on('click', (e) => {
				e.stopPropagation();
				_Pages.openPagePreviewInNewWindow(page);
			});
		}

		return icon;
	},
	openPagePreviewInNewWindow: (entity) => {

		let url = _Pages.previews.getUrlForPage(entity);
		window.open(url);
	},
	registerDetailClickHandler: (element, entity) => {

		if (Structr.getActiveModule() === _Pages && element.data('clickhandlerSet') !== true) {

			element.data('clickhandlerSet', true);

			element.on('click', function(e) {

				let clickedObjectIsCurrentlySelected = _Entities.selectedObject && _Entities.selectedObject.id === entity.id;
				let isElementBeingEditedCurrently    = (_Pages.centerPane.dataset['elementId'] === entity.id);

				_Entities.selectElement(element.closest('.node')[0], entity);

				if (!clickedObjectIsCurrentlySelected || !isElementBeingEditedCurrently) {

					LSWrapper.setItem(_Entities.selectedObjectIdKey, entity.id);
					_Pages.refreshCenterPane(entity);
				}
			});
		}
	},
	appendElementElement: (entity, refNode, refNodeIsParent) => {

		entity  = StructrModel.ensureObject(entity);
		let div = _Elements.appendElementElement(entity, refNode, refNodeIsParent);

		if (!div) {
			return false;
		}

		return div;
	},
	expandTreeNode: (id, stack, lastId) => {
		if (!id) {
			return;
		}
		lastId = lastId || id;
		stack = stack || [];
		stack.push(id);
		Command.get(id, 'id,parent', (obj) => {
			if (obj.parent) {
				_Pages.expandTreeNode(obj.parent.id, stack, lastId);
			} else {
				_Entities.expandAll(stack.reverse(), lastId);
			}
		});
	},
	highlight: (id) => {
		let node = Structr.node(id);
		if (node) {
			node.parent().removeClass('nodeHover');
			node.addClass('nodeHover');
		}

		let activeNode = Structr.node(id, '#active_');
		if (activeNode) {
			activeNode.parent().removeClass('nodeHover');
			activeNode.addClass('nodeHover');
		}
	},
	unhighlight: (id) => {
		let node = Structr.node(id);
		if (node) {
			node.removeClass('nodeHover');
		}
		let activeNode = Structr.node(id, '#active_');
		if (activeNode) {
			activeNode.removeClass('nodeHover');
		}
	},
	getActiveTabLeft: () => {
		return LSWrapper.getItem(_Pages.activeTabLeftKey, 'pagesTab');
	},
	getActiveTabRight: () => {
		return LSWrapper.getItem(_Pages.activeTabRightKey);
	},
	getLeftResizerKey: () => {
		return _Pages.pagesResizerLeftKey;
	},
	getRightResizerKey: () => {
		return _Pages.pagesResizerRightKey;
	},
	leftSlideoutClosedCallback: () => {

		LSWrapper.removeItem(_Pages.activeTabLeftKey);
		Structr.resize();
	},
	rightSlideoutClosedCallback: () => {

		LSWrapper.removeItem(_Pages.activeTabRightKey);
		Structr.resize();
	},

	openAndSelectTreeObjectById: (id) => {

		_Entities.deselectAllElements();

		if (!Structr.node(id)) {
			_Pages.expandTreeNode(id);
		} else {
			let treeEl = Structr.node(id);
			if (treeEl) {
				_Entities.highlightElement(treeEl);
			}
		}

		LSWrapper.setItem(_Entities.selectedObjectIdKey, id);
	},
	eventActionMappingDialog: (entity, container, typeInfo) => {

		_Helpers.activateCommentsInElement(container);

		let eventSelectElement               = container.querySelector('#event-select');
		let actionSelectElement              = container.querySelector('#action-select');

		let customEventInput                 = container.querySelector('#custom-event-input');

		let dataTypeSelect                   = container.querySelector('#data-type-select');
		let dataTypeSelectUl                 = dataTypeSelect.parentNode.querySelector('ul');
		let dataTypeInput                    = container.querySelector('#data-type-input');
		let methodNameInput                  = container.querySelector('#method-name-input');

		let idExpressionInput                = container.querySelector('#id-expression-input');

		let addParameterMappingButton        = container.querySelector('.em-add-parameter-mapping-button');
		let addParameterMappingForTypeButton = container.querySelector('.em-add-parameter-mapping-for-type-button');

		let successNotificationsSelect       = container.querySelector('#success-notifications-select');
		let successNotificationsPartialInput = container.querySelector('#success-notifications-custom-dialog-input');

		let failureNotificationsSelect       = container.querySelector('#failure-notifications-select');
		let failureNotificationsPartialInput = container.querySelector('#failure-notifications-custom-dialog-input');

		let successBehaviourSelect           = container.querySelector('#success-behaviour-select');
		let successPartialRefreshInput       = container.querySelector('#success-partial-refresh-input');
		let successNavigateToURLInput        = container.querySelector('#success-navigate-to-url-input');
		let successFireEventInput            = container.querySelector('#success-fire-event-input');

		let failureBehaviourSelect           = container.querySelector('#failure-behaviour-select');
		let failurePartialRefreshInput       = container.querySelector('#failure-partial-refresh-input');
		let failureNavigateToURLInput        = container.querySelector('#failure-navigate-to-url-input');
		let failureFireEventInput            = container.querySelector('#failure-fire-event-input');

		let saveButton                       = container.querySelector('#save-event-mapping-button');

		let actionMapping;

		Command.getByType('SchemaNode', 1000, 1, 'name', 'asc', 'id,name', false, result => {
			dataTypeSelect.insertAdjacentHTML('beforeend', result.map(typeObj => `<option>${typeObj.name}</option>`).join(''));
			dataTypeSelectUl.insertAdjacentHTML('beforeend', result.map(typeObj => `<li data-value="${typeObj.name}">${typeObj.name}</li>`).join(''));
		});

		if (entity.triggeredActions && entity.triggeredActions.length) {

			actionMapping = entity.triggeredActions[0];

			Command.get(actionMapping.id, 'event,action,method,idExpression,dataType,parameterMappings,successNotifications,successNotificationsPartial,successNotificationsEvent,failureNotifications,failureNotificationsPartial,failureNotificationsEvent,successBehaviour,successPartial,successURL,successEvent,failureBehaviour,failurePartial,failureURL,failureEvent', (result) => {
				//console.log('Using first object for event action mapping:', result);
				updateEventMappingInterface(entity, result);
			});
		}

		if (saveButton) {
			saveButton.addEventListener('click', () => {
				saveEventMappingData(entity);
				saveParameterMappings();
			});
		}

		container.querySelectorAll('input').forEach(el => el.addEventListener('focusout', e => saveEventMappingData(entity, el)));

		eventSelectElement.addEventListener('change', e => saveEventMappingData(entity));
		actionSelectElement.addEventListener('change', e => saveEventMappingData(entity));

		// combined type select and input
		{
			let showDataTypeList = () => { dataTypeSelectUl.classList.remove('hidden'); };
			let hideDataTypeList = () => { dataTypeSelectUl.classList.add('hidden'); };

			dataTypeSelect.addEventListener('change', e => {
				dataTypeInput.value = dataTypeSelect.value;
			});

			dataTypeSelect.addEventListener('mousedown', e => {
				e.preventDefault(); // => catches click
				showDataTypeList();
			});

			dataTypeSelectUl.addEventListener('mousedown', e => {
				e.preventDefault(); // => catches click
			});

			dataTypeSelectUl.addEventListener('click', e => {
				if (e.target.dataset.value) {
					dataTypeInput.value  = e.target.dataset.value;
					dataTypeSelect.value = dataTypeInput.value;

					saveEventMappingData(entity, dataTypeSelectUl);

					hideDataTypeList();
				}
			});

			container.addEventListener('mousedown', e => {
				if (e.defaultPrevented === false) {
					hideDataTypeList();
				}
			});

			dataTypeInput.addEventListener('keyup', e => {
				const el  = e.target;
				const key = e.key;

				if (key === 'Escape') {

					dataTypeSelectUl.classList.add('hidden');
					return;

				} else if (key === 'Enter') {

					saveEventMappingData(entity, dataTypeInput);
					dataTypeSelectUl.classList.add('hidden');
					return;
				}

				for (let child of dataTypeSelectUl.children) {

					if (child.dataset.value && child.dataset.value.match(el.value)) {
						child.classList.remove('hidden');
					} else {
						child.classList.add('hidden');
					}
				}

				showDataTypeList();
			});
		}

		addParameterMappingButton.addEventListener('click', e => {

			Command.get(entity.id, 'id,type,triggeredActions', (result) => {
				actionMapping = result.triggeredActions[0];
				Command.create({type: 'ParameterMapping', actionMapping: actionMapping.id}, (parameterMapping) => {
					getAndAppendParameterMapping(parameterMapping.id);
				});
			});
		});

		addParameterMappingForTypeButton.addEventListener('click', e => {

			Command.get(entity.id, 'id,type,triggeredActions', (result) => {

				actionMapping = result.triggeredActions[0];

				Command.getSchemaInfo(dataTypeSelect.value, result => {

					let properties = result.filter(property => !property.system);
					//console.log(properties); return;

					for (const property of properties) {

						Command.create({
							type: 'ParameterMapping',
							parameterName: property.jsonName,
							actionMapping: actionMapping.id
						}, (parameterMapping) => {
							getAndAppendParameterMapping(parameterMapping.id);
						});

					}
				});
			});
		});

		successNotificationsSelect.addEventListener('change', e => {
			let el = e.target;
			el.classList.remove('required');
			saveEventMappingData(entity, el);
		});

		failureNotificationsSelect.addEventListener('change', e => {
			let el = e.target;
			el.classList.remove('required');
			saveEventMappingData(entity, el);
		});

		successBehaviourSelect.addEventListener('change', e => {
			let el = e.target;
			el.classList.remove('required');
			saveEventMappingData(entity, el);
		});

		failureBehaviourSelect.addEventListener('change', e => {
			let el = e.target;
			el.classList.remove('required');
			saveEventMappingData(entity, el);
		});

		const updateEventMappingInterface = (entity, actionMapping) => {

			if (!actionMapping) {
				console.warn('No actionMapping object given', entity);
				return;
			}

			//console.log('updateEventMapping', entity, actionMapping);

			// TODO: Find better solution for the following conversion which is necessary because of 'previous-page' vs. 'prev-page'
			if (actionMapping.action === 'previous-page') {
				actionMapping.action = 'prev-page';
			}

			// if (actionMapping.event === 'custom') {
			// 	customEventInput.value  = actionMapping.event;
			// }

			eventSelectElement.value               = actionMapping.event;
			actionSelectElement.value              = actionMapping.action;

			methodNameInput.value                  = actionMapping.method;
			dataTypeSelect.value                   = actionMapping.dataType;
			dataTypeInput.value                    = actionMapping.dataType;

			idExpressionInput.value                = actionMapping.idExpression;

			successNotificationsSelect.value       = actionMapping.successNotifications;
			successNotificationsPartialInput.value = actionMapping.successNotificationsPartial;

			failureNotificationsSelect.value       = actionMapping.failureNotifications;
			failureNotificationsPartialInput.value = actionMapping.failureNotificationsPartial;

			successBehaviourSelect.value           = actionMapping.successBehaviour;
			successPartialRefreshInput.value       = actionMapping.successPartial;
			successNavigateToURLInput.value        = actionMapping.successURL;
			successFireEventInput.value            = actionMapping.successEvent;

			failureBehaviourSelect.value           = actionMapping.failureBehaviour;
			failurePartialRefreshInput.value       = actionMapping.failurePartial;
			failureNavigateToURLInput.value        = actionMapping.failureURL;
			failureFireEventInput.value            = actionMapping.failureEvent;

			// UI-only block
			{

				// Hide everything that is dynamic and depends on event and action being set
				for (let any of document.querySelectorAll('.em-event-element, .em-action-element')) {
					any.classList.add('hidden');
				}

				if (eventSelectElement.value === 'none') {

					eventSelectElement.classList.add('required');

				} else {

					eventSelectElement.classList.remove('required');

					// show all elements that are shown for non-empty event values
					for (let any of document.querySelectorAll('.em-event-any')) {
						any.classList.remove('hidden');
					}

					if (actionSelectElement.value != 'none') {

						actionSelectElement.classList.remove('required');

						for (let any of document.querySelectorAll('.em-action-any')) {
							any.classList.remove('hidden');
						}

					} else {
						actionSelectElement.classList.add('required');
					}

					// show all relevant elements for event
					for (let eventRelevant of document.querySelectorAll(`.em-event-${eventSelectElement.value}`)) {
						eventRelevant.classList.remove('hidden');
					}

					// show all relevant elements for action
					for (let actionRelevant of document.querySelectorAll(`.em-action-${actionSelectElement.value}`)) {
						actionRelevant.classList.remove('hidden');
					}


					// success notifications
					{
						// hide all
						for (let successOption of document.querySelectorAll('.option-success-notifications')){
							successOption.classList.add('hidden');
						}

						// show selected
						for (let successOption of document.querySelectorAll(`.option-success-notifications-${successNotificationsSelect.value}`)) {
							successOption.classList.remove('hidden');
						}
					}


					// failure notifications
					{
						// hide all
						for (let failOption of document.querySelectorAll('.option-failure-notifications')) {
							failOption.classList.add('hidden');
						}

						// show selected
						for (let failOption of document.querySelectorAll(`.option-failure-notifications-${failureNotificationsSelect.value}`)) {
							failOption.classList.remove('hidden');
						}
					}

					// success behaviour
					{
						// hide all
						for (let successOption of document.querySelectorAll('.option-success')){
							successOption.classList.add('hidden');
						}

						// show selected
						for (let successOption of document.querySelectorAll(`.option-success-${successBehaviourSelect.value}`)) {
							successOption.classList.remove('hidden');
						}
					}


					// failure behaviour
					{
						// hide all
						for (let failOption of document.querySelectorAll('.option-failure')) {
							failOption.classList.add('hidden');
						}

						// show selected
						for (let failOption of document.querySelectorAll(`.option-failure-${failureBehaviourSelect.value}`)) {
							failOption.classList.remove('hidden');
						}
					}

				}
			}


			// remove existing parameter mappings
			for (const parameterMappingElement of document.querySelectorAll('.em-parameter-mapping')) {
				_Helpers.fastRemoveElement(parameterMappingElement);
			}

			// append mapped parameters
			Command.get(actionMapping.id, 'id,parameterMappings', (actionMapping) => {
				for (const parameterMapping of actionMapping.parameterMappings) {
					getAndAppendParameterMapping(parameterMapping.id);
				}
			});

			// if (entity.triggeredActions && entity.triggeredActions.length) {
			//
			// 	// TODO: Support multiple actions per DOM element
			// 	let actionMapping = entity.triggeredActions[0];

			// Activate dropzone if success behaviour is partial-refresh-linked
			if (actionMapping.successBehaviour === 'partial-refresh-linked') {

				const parentElement   = document.querySelector('.option-success-partial-refresh-linked');
				const dropzoneElement = parentElement.querySelector('.success-partial-refresh-linked-dropzone');
				const inputElement    = parentElement.querySelector('#success-partial-refresh-linked-input');

				dropzoneElement.querySelectorAll('.node').forEach(n => n.remove());
				activateElementDropzone(parentElement, dropzoneElement, inputElement, 'reloadingActions');

				Command.get(actionMapping.id, 'id,successTargets', actionMapping => {
					for (const successTarget of actionMapping.successTargets) {
						Command.get(successTarget.id, 'id,name', obj => {
							addLinkedElementToDropzone(parentElement, dropzoneElement, obj, 'successTargets');
						});
					}
				});
			}

			// Activate dropzone if failure behaviour is partial-refresh-linked
			if (actionMapping.failureBehaviour === 'partial-refresh-linked') {

				const parentElement   = document.querySelector('.option-failure-partial-refresh-linked');
				const dropzoneElement = parentElement.querySelector('.failure-partial-refresh-linked-dropzone');
				const inputElement    = parentElement.querySelector('#failure-partial-refresh-linked-input');

				dropzoneElement.querySelectorAll('.node').forEach(n => n.remove());
				activateElementDropzone(parentElement, dropzoneElement, inputElement, 'failureActions');

				Command.get(actionMapping.id, 'id,failureTargets', actionMapping => {
					for (const failureTarget of actionMapping.failureTargets) {
						Command.get(failureTarget.id, 'id,name', obj => {
							addLinkedElementToDropzone(parentElement, dropzoneElement, obj, 'failureTargets');
						});
					}
				});
			}

			// Activate dropzone if success notification is custom-dialog-linked
			if (actionMapping.successNotifications === 'custom-dialog-linked') {

				const parentElement   = document.querySelector('.option-success-notifications-custom-dialog-linked');
				const dropzoneElement = parentElement.querySelector('.success-notifications-custom-dialog-linked-dropzone');
				const inputElement    = parentElement.querySelector('#success-notifications-custom-dialog-linked-input');

				dropzoneElement.querySelectorAll('.node').forEach(n => n.remove());
				activateElementDropzone(parentElement, dropzoneElement, inputElement, 'successNotificationActions');

				Command.get(actionMapping.id, 'id,successNotificationElements', actionMapping => {
					for (const successNotificationElement of actionMapping.successNotificationElements) {
						Command.get(successNotificationElement.id, 'id,name', obj => {
							addLinkedElementToDropzone(parentElement, dropzoneElement, obj, 'successNotificationElements');
						});
					}
				});
			}

			// Activate dropzone if failure notification is custom-dialog-linked
			if (actionMapping.failureNotifications === 'custom-dialog-linked') {

				const parentElement   = document.querySelector('.option-failure-notifications-custom-dialog-linked');
				const dropzoneElement = parentElement.querySelector('.failure-notifications-custom-dialog-linked-dropzone');
				const inputElement    = parentElement.querySelector('#failure-notifications-custom-dialog-linked-input');

				dropzoneElement.querySelectorAll('.node').forEach(n => n.remove());
				activateElementDropzone(parentElement, dropzoneElement, inputElement, 'failureNotificationActions');

				Command.get(actionMapping.id, 'id,failureNotificationElements', actionMapping => {
					for (const failureNotificationElement of actionMapping.failureNotificationElements) {
						Command.get(failureNotificationElement.id, 'id,name', obj => {
							addLinkedElementToDropzone(parentElement, dropzoneElement, obj, 'failureNotificationElements');
						});
					}
				});
			}
		};

		const activateElementDropzone = (parentElement, dropzoneElement, inputElement, propertyKey) => {

			if (dropzoneElement) {

				$(dropzoneElement).droppable({

					drop: (e, el) => {

						e.preventDefault();
						e.stopPropagation();

						let sourceEl = $(el.draggable);
						let sourceId = Structr.getId(sourceEl);

						if (!sourceId) {
							return false;
						}

						let obj = StructrModel.obj(sourceId);

						// Ignore shared components
						if (obj && obj.syncedNodesIds && obj.syncedNodesIds.length || sourceEl.parent().attr('id') === 'componentsArea') {
							return false;
						}

						// Ignore already linked elements
						if (dropzoneElement.querySelector('.node._' + obj.id)) {
							return false;
						}

						inputElement.value = sourceId;
						_Elements.dropBlocked = false;

						const newCollection = [...obj[propertyKey]];
						newCollection.push({ id: actionMapping.id });

						Command.setProperty(obj.id, propertyKey, newCollection);

						addLinkedElementToDropzone(parentElement, dropzoneElement, obj, propertyKey);

					}
				});
			}
		};

		const addLinkedElementToDropzone = (parentElement, dropzoneElement, obj, propertyKey) => {
			_Entities.insertRelatedNode(dropzoneElement, obj, (nodeEl) => {
				$('.remove', nodeEl).on('click', function(e) {
					e.preventDefault();
					e.stopPropagation();
					parentElement.querySelector('._' + obj.id).remove();
					dropzoneElement.classList.remove('hidden');
					Command.get(actionMapping.id, 'id,' + propertyKey, actionMapping => {
						Command.setProperty(actionMapping.id, propertyKey, actionMapping[propertyKey] = actionMapping[propertyKey].filter(t => t.id !== obj.id));
					});

				});
			}, 'afterbegin');
		};

		const getAndAppendParameterMapping = (id) => {

			Command.get(id, 'id,name,parameterName,parameterType,constantValue,scriptExpression,inputElement', parameterMapping => {

				//console.log('Append parameter mapping element for', parameterMapping);

				const container = document.querySelector('.em-parameter-mappings-container');
				container.insertAdjacentHTML('beforeend', _Pages.templates.parameterMappingRow(parameterMapping));
				const row = container.querySelector(`.em-parameter-mapping[data-structr-id="${id}"]`);

				const parameterTypeSelector = row.querySelector('.parameter-type-select');
				parameterTypeSelector.value = parameterMapping.parameterType;
				row.querySelector(`.parameter-${parameterTypeSelector.value}`)?.classList.remove('hidden');
				activateUserInputDropzone(row);

				parameterTypeSelector.addEventListener('change', e => {
					const selectElement = e.target;
					const value = selectElement.value;
					for (let el of row.querySelectorAll('.em-parameter-value')) {
						el.classList.add('hidden');
					}
					row.querySelector(`.parameter-${value}`)?.classList.remove('hidden');
					activateUserInputDropzone(row);
					saveEventMappingData(entity);
					saveParameterMappings(parameterTypeSelector);
				});

				//console.log(parameterMapping.parameterType, parameterMapping.inputElement);
				if (parameterMapping.parameterType === 'user-input' && parameterMapping.inputElement) {
					replaceDropzoneByUserInputElement(row, parameterMapping.inputElement);
				}

				const constantValueInputElement = row.querySelector('.parameter-constant-value-input');
				if (parameterMapping.constantValue) {
					constantValueInputElement.value = parameterMapping.constantValue;
				}

				const scriptExpressionInputElement = row.querySelector('.parameter-script-expression-input');
				if (parameterMapping.scriptExpression) {
					scriptExpressionInputElement.value = parameterMapping.scriptExpression;
				}

				container.querySelectorAll('input').forEach(el => el.addEventListener('focusout', e => saveParameterMappings(el)));

				activateRemoveIcon(parameterMapping);
				_Helpers.activateCommentsInElement(container);

			}, null);
		};

		const replaceDropzoneByUserInputElement = (rowElement, obj) => {
			const userInputArea = rowElement.querySelector('.parameter-user-input');
			_Entities.appendRelatedNode($(userInputArea), obj, (nodeEl) => {
				$('.remove', nodeEl).on('click', function(e) {
					e.preventDefault();
					e.stopPropagation();
					userInputArea.querySelector('.node').remove();
					dropzoneElement.classList.remove('hidden');
					saveParameterMappings(dropzoneElement);
				});
			});
			const dropzoneElement = userInputArea.querySelector('.link-existing-element-dropzone');
			dropzoneElement.classList.add('hidden');
			saveParameterMappings(dropzoneElement);
		};

		const activateRemoveIcon = (parameterMapping) => {
			let parameterMappingElement = document.querySelector(`.em-parameter-mapping[data-structr-id="${parameterMapping.id}"]`);
			let removeIcon = parameterMappingElement.querySelector('.em-parameter-mapping-remove-button');
			removeIcon?.addEventListener('click', e => {
				Command.deleteNode(parameterMapping.id, false, () => {
					_Helpers.fastRemoveElement(parameterMappingElement);
				});
			});
		};

		const activateUserInputDropzone = (parentElement) => {

			const dropzoneElement = parentElement.querySelector('.link-existing-element-dropzone');

			if (dropzoneElement) {

				$(dropzoneElement).droppable({

					drop: (e, el) => {

						e.preventDefault();
						e.stopPropagation();

						let sourceEl = $(el.draggable);
						let sourceId = Structr.getId(sourceEl);

						if (!sourceId) {
							return false;
						}

						let obj = StructrModel.obj(sourceId);

						// Ignore shared components
						if (obj && obj.syncedNodesIds && obj.syncedNodesIds.length || sourceEl.parent().attr('id') === 'componentsArea') {
							return false;
						}

						parentElement.querySelector('.parameter-user-input-input').value = sourceId;
						_Elements.dropBlocked = false;

						let userInputName = obj['_html_name'];
						if (userInputName) {

							let parameterNameInput = parentElement.querySelector('.parameter-name-input');
							if (parameterNameInput.value === '') {
								parameterNameInput.value = userInputName;
							}
						}

						replaceDropzoneByUserInputElement(parentElement, obj);
					}
				});
			}
		};

		const saveEventMappingData = (entity, el) => {

			let actionMappingObject = {
				type:                        'ActionMapping',
				event:                       eventSelectElement?.value,
				action:                      actionSelectElement?.value,
				method:                      methodNameInput?.value,
				dataType:                    dataTypeInput?.value ?? dataTypeSelect?.value,
				idExpression:                idExpressionInput.value,
				successNotifications:        successNotificationsSelect.value,
				successNotificationsPartial: successNotificationsPartialInput.value,
				failureNotifications:        failureNotificationsSelect.value,
				failureNotificationsPartial: failureNotificationsPartialInput.value,
				successBehaviour:            successBehaviourSelect?.value,
				successPartial:              successPartialRefreshInput?.value,
				successURL:                  successNavigateToURLInput?.value,
				successEvent:                successFireEventInput?.value,
				failureBehaviour:            failureBehaviourSelect?.value,
				failurePartial:              failurePartialRefreshInput?.value,
				failureURL:                  failureNavigateToURLInput?.value,
				failureEvent:                failureFireEventInput?.value
			};

			//console.log(actionMappingObject);

			if (entity.triggeredActions && entity.triggeredActions.length) {

				actionMappingObject.id = entity.triggeredActions[0].id;

				if (actionMappingObject.event === 'none') {

					//console.log('ActionMapping event === "none"... deleting...', actionMappingObject);
					// the UI will keep the contents until it is reloaded, a chance to undo until we select another node or tab
					Command.deleteNode(actionMappingObject.id, undefined, () => {
						updateEventMappingInterface(entity, actionMappingObject);
						_Helpers.blinkGreen(el);
					});

				} else {

					//console.log('ActionMapping object already exists, updating...', actionMappingObject);
					Command.setProperties(actionMappingObject.id, actionMappingObject, () => {
						_Helpers.blinkGreen(Structr.nodeContainer(entity.id));
						_Helpers.blinkGreen(el);
						updateEventMappingInterface(entity, actionMappingObject);
					});
				}

			} else {

				actionMappingObject.triggerElements = [ entity.id ];

				// update entity with newly created action mapping object to prevent duplicate creation of elements
				entity.triggeredActions = [ actionMappingObject ];

				//console.log('No ActionMapping object exists, create one and update data...');
				Command.create(actionMappingObject, (newActionMapping) => {
					//console.log('Successfully created new ActionMapping object:', actionMapping);
					_Helpers.blinkGreen(Structr.nodeContainer(entity.id));
					_Helpers.blinkGreen(el);
					actionMappingObject.id = newActionMapping.id;
					updateEventMappingInterface(entity, newActionMapping);
					actionMapping = newActionMapping;
				});
			}

		};

		const saveParameterMappings = (el) => {

			const inputDefinitions = [
				{ key: 'parameterName',    selector: '.em-parameter-mapping .parameter-name-input' },
				{ key: 'parameterType',    selector: '.em-parameter-mapping .parameter-type-select' },
				{ key: 'constantValue',    selector: '.em-parameter-mapping .parameter-constant-value-input' },
				{ key: 'scriptExpression', selector: '.em-parameter-mapping .parameter-script-expression-input' },
				{ key: 'inputElement',     selector: '.em-parameter-mapping .parameter-user-input-input' },
				{ key: 'methodResult',     selector: '.em-parameter-mapping .parameter-method-result-input' },
				{ key: 'flowResult',       selector: '.em-parameter-mapping .parameter-flow-result-input' }
			];

			//console.log('save parameter mappings', inputDefinitions, parameterMappings);

			for (const parameterMappingElement of container.querySelectorAll('.em-parameter-mapping')) {

				const parameterMappingId = parameterMappingElement.dataset['structrId'];
				//console.log(parameterMappingId);
				const parameterMappingData = { id: parameterMappingId };
				for (const inputDefinition of inputDefinitions) {

					for (const inp of parameterMappingElement.querySelectorAll(inputDefinition.selector)) {

						const value = inp.value;
						if (value) {
							//console.log(inputDefinition.key, value);
							parameterMappingData[inputDefinition.key] = value;
						}
					}
				}

				//console.log(parameterMappingData);
				Command.setProperties(parameterMappingId, parameterMappingData, () => {
					_Helpers.blinkGreen(el);
				});
			}
		};

	},

	localizations: {
		lastSelectedPageKey: 'structrLocalizationsLastSelectedPageKey_' + location.port,
		lastUsedLocaleKey: 'structrLocalizationsLastUsedLocale_' + location.port,
		wrapperTypeForContextMenu: 'WrappedLocalizationForPreview',
		getContextMenuElements: (div, wrappedEntity) => {

			let entity         = wrappedEntity.entity;
			const isSchemaNode = (entity.type === 'SchemaNode');

			let elements = [];

			if (isSchemaNode) {

				elements.push({
					name: 'Go to Schema Node',
					clickHandler: () => {

						let pathToOpen = `/root/${entity.isBuiltinType ? 'builtin' : 'custom'}/${entity.id}`;

						window.location.href = '#code';
						window.setTimeout(() => {
							_Code.findAndOpenNode(pathToOpen, false);
						}, 1000);
					}
				});
			}

			return elements;
		},

		refreshPagesForLocalizationPreview: async () => {

			let pageSelect       = document.getElementById('localization-preview-page');
			let localeInput      = document.querySelector('#localizations input.locale');
			_Helpers.fastRemoveAllChildren(pageSelect);

			let lastSelectedPage = LSWrapper.getItem(_Pages.localizations.lastSelectedPageKey);
			let lastUsedLocale   = LSWrapper.getItem(_Pages.localizations.lastUsedLocaleKey);
			if (lastUsedLocale) {
				localeInput.value = lastUsedLocale;
			}

			let pages = await Command.queryPromise('Page', 1000, 1, 'name', 'asc', { hidden: false }, true, null, 'id,name');

			for (let page of pages) {

				let option = document.createElement('option');
				option.value = page.id;
				option.textContent = page.name;

				// first assumption: if we are previewing a page, use that page
				if (page.id === _Pages.previews.activePreviewPageId) {
					option.selected = true;
				}

				// second assumption: use previously selected page if not actively previewing a page
				if (!_Pages.previews.activePreviewPageId && page.id === lastSelectedPage) {
					option.selected = true;
				}

				pageSelect.appendChild(option);
			}
		},
		refreshLocalizations: async () => {

			let localizationsContainer = document.querySelector('#localizations div.inner div.results');
			let localeInput            = document.querySelector('#localizations input.locale');
			let locale                 = localeInput.value;
			let pageSelect             = document.getElementById('localization-preview-page');

			if (!locale) {
				_Helpers.blinkRed(localeInput);
				return;
			}

			LSWrapper.setItem(_Pages.localizations.lastSelectedPageKey, pageSelect.value);
			LSWrapper.setItem(_Pages.localizations.lastUsedLocaleKey, locale);

			let id                = pageSelect.value;
			let detailObjectId    = LSWrapper.getItem(_Pages.detailsObjectIdKey + id);
			let queryString       = LSWrapper.getItem(_Pages.requestParametersKey + id);
			let localizations     = await Command.listLocalizations(id, locale, detailObjectId, queryString);
			let localizationIdKey = 'localizationId';
			let previousValueKey  = 'previousValue';

			_Helpers.fastRemoveAllChildren(localizationsContainer);

			if (localizations.length == 0) {

				localizationsContainer.innerHTML = 'No localizations found in page.';

			} else {

				for (let res of localizations) {

					let modelNode = (res.node) ? StructrModel.createFromData(res.node) : { type: "Untraceable source (probably static method)", isFake: true };
					let tbody     = _Pages.localizations.getNodeForLocalization(localizationsContainer, modelNode);
					let row       = _Helpers.createSingleDOMElementFromHTML(`
						<tr>
							<td><div class="key-column allow-break">${_Helpers.escapeForHtmlAttributes(res.key)}</div></td>
							<td class="domain-column">${res.domain}</td>
							<td class="locale-column">${((res.localization !== null) ? res.localization.locale : res.locale)}</td>
							<td class="input"><input class="localized-value" placeholder="...">${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'delete', 'mr-2', 'ml-2']))}</td>
						</tr>
					`);
					let input     = row.querySelector('input.localized-value');

					if (res.localization) {
						let domainIdentical = (res.localization.domain === res.domain) || (!res.localization.domain && !res.domain);
						if (!domainIdentical) {
							res.localization = null;
							// we are getting the fallback localization for this entry - do not show this as we would update the wrong localization otherwise
						}
					}

					if (res.localization !== null) {
						row.classList.add('has-value');
						input.value = res.localization.localizedName;
						input.dataset[localizationIdKey] = res.localization.id;
						input.dataset[previousValueKey]  = res.localization.localizedName;
					}

					row.querySelector('.delete').addEventListener('click', (event) => {
						event.preventDefault();

						let id = input.dataset[localizationIdKey];

						if (id) {
							_Entities.deleteNodes([{id: id, name: input.value}], false, () => {
								row.classList.remove('has-value');
								input.value = '';
								delete input.dataset[localizationIdKey];
								delete input.dataset[previousValueKey];
							});
						}
					});

					input.addEventListener('click', (e) => {
						e.stopPropagation();
					})

					input.addEventListener('blur', (event) => {

						let newValue       = input.value;
						let localizationId = input.dataset[localizationIdKey];
						let previousValue  = input.dataset[previousValueKey];
						let isChange       = (!previousValue && newValue !== '') || (previousValue && previousValue !== newValue);

						if (isChange) {

							if (localizationId) {

								Command.setProperties(localizationId, { localizedName: newValue }, () => {
									_Helpers.blinkGreen(input);
									input.dataset[previousValueKey] = newValue;
								});

							} else {

								Command.create({
										type: 'Localization',
										name: res.key,
										domain: res.domain || null,
										locale: res.locale,
										localizedName: newValue
									},
									function(createdLocalization) {
										input.dataset[localizationIdKey] = createdLocalization.id;
										input.dataset[previousValueKey]  = newValue;
										row.classList.add('has-value');
										_Helpers.blinkGreen(input);
									});
							}
						}
					});

					tbody.append(row);
				};
			}
		},
		getNodeForLocalization: (container, entity) => {

			let idString = 'locNode_' + entity.id;
			let existing = container.querySelector('#' + idString);

			if (existing) {
				return existing.querySelector('tbody');
			}

			let displayName = _Helpers.getElementDisplayName(entity);
			let iconClasses = ['mr-2', 'flex-shrink-0'];
			let iconHTML    = (entity.isDOMNode) ? (entity.isContent ?_Icons.getSvgIconForContentNode(entity, iconClasses) : _Icons.getSvgIconForElementNode(entity, iconClasses)) : _Icons.getSvgIcon(_Icons.iconSchemaNodeDefault, 16, 16, iconClasses);
			let detailHtml  = '';

			if (entity.type === 'Content') {

				detailHtml = _Helpers.escapeTags(entity.content);

			} else if (entity.type === 'Template') {

				if (entity.name) {
					detailHtml = displayName;
				} else {
					detailHtml = _Helpers.escapeTags(entity.content);
				}

			} else if (!entity.isDOMNode) {
				detailHtml = `<b title="${_Helpers.escapeForHtmlAttributes(entity.type)}" class="tag_ name_">${entity.type}</b>`;
			} else {
				detailHtml = `<b title="${_Helpers.escapeForHtmlAttributes(displayName)}" class="tag_ name_">${displayName}</b>`;
			}

			let div = _Helpers.createSingleDOMElementFromHTML(`
				<div id="${idString}" class="node localization-element ${(entity.tag === 'html' ? ' html_element' : '')}" data-node-id="${(_Entities.isContentElement(entity) ? entity.parent.id : entity.id )}">
					<div class="node-container flex items-center">
						${iconHTML}<span class="abbr-ellipsis abbr-pages-tree">${detailHtml}${_Elements.classIdString(entity)}</span>
						<div class="icons-container flex items-center"></div>
					</div>
					<table>
						<thead>
							<tr>
								<th>Key</th>
								<th>Domain</th>
								<th>Locale</th>
								<th>Localization</th>
							</tr>
						</thead>
						<tbody></tbody>
					</table>
				</div>
			`);

			let $div           = $(div);
			let iconsContainer = $('.icons-container', $div);

			if (!entity.isDOMNode && !entity.isFake) {

				// do not use pointer cursor
				div.classList.add('schema');

				Command.queryPromise('SchemaNode', 1, 1, 'name', 'asc', { name: entity.type }, true).then((schemaNodes) => {

					if (schemaNodes.length === 1) {

						_Entities.appendContextMenuIcon(iconsContainer, {
							type: _Pages.localizations.wrapperTypeForContextMenu,
							entity: schemaNodes[0]
						}, false);
					}
				});
			}

			container.appendChild(div);

			_Entities.setMouseOver($div, undefined, ((entity.syncedNodesIds && entity.syncedNodesIds.length) ? entity.syncedNodesIds : [entity.sharedComponentId] ));

			if (entity.isDOMNode) {
				_Pages.registerDetailClickHandler($div, entity);
				_Elements.clickOrSelectElementIfLastSelected($div, entity);
			}

			return div.querySelector('tbody');
		}
	},

	previews: {
		loadPreviewTimer : undefined,
		previewElement: undefined,
		selectedElementOrigSource: null,
		activePreviewPageId: null,
		activePreviewHighlightElementId: null,
		wrapperTypeForContextMenu: 'PreviewElement',

		getContextMenuElements: (div, entityWrapper) => {

			const entity = entityWrapper.entity;

			let elements = [];

			elements.push({
				classes: ['preview-context-menu', 'w-96'],
				icon: entity.isContent ? _Icons.getSvgIconForContentNode(entity) : _Icons.getSvgIconForElementNode(entity),
				html: entity.tag + (entity._html_id ? ' <span class="class-id-attrs _html_id">#' + entity._html_id + '</span>' : '') + (entity._html_class ? '<span class="class-id-attrs _html_class">.' + entity._html_class.split(' ').join('.') + '</span>' : '')
					+ (entity._html_id    ? '<input placeholder="id"    class="hidden ml-2 inline context-menu-input-field-' + entity.id + '" type="text" name="_html_id" size="'  + entity._html_id.length    + '" value="' + entity._html_id    + '">' : '')
					+ (entity._html_class ? '<textarea style="width:calc(100% + 4rem)" rows="' + Math.ceil(entity._html_class.length/35) + '" placeholder="class" class="hidden mt-1 context-menu-input-field-' + entity.id + '" name="_html_class">' + entity._html_class + '</textarea>' : ''),
				clickHandler: (e) => {

					const classInputField = document.querySelector('.context-menu-input-field-' + entity.id + '[name="_html_class"]');
					classInputField?.addEventListener('keydown', (e) => {
						if (e.key === 'Enter') {
							e.preventDefault();
							Command.setProperty(entity.id, '_html_class', classInputField.value);
						} else if (e.key === 'Escape') {
							restoreDisplay();
						}
					});

					const idInputField    = document.querySelector('.context-menu-input-field-' + entity.id + '[name="_html_id"]');
					idInputField?.addEventListener('keydown', (e) => {
						if (e.key === 'Enter') {
							e.preventDefault();
							Command.setProperty(entity.id, '_html_id', idInputField.value);
						} else if (e.key === 'Escape') {
							restoreDisplay();
						}
					});

					const restoreDisplay = () => {
						classInputField?.classList.add('hidden');
						idInputField?.classList.add('hidden');
						document.querySelector('.class-id-attrs._html_class')?.classList.remove('hidden');
						document.querySelector('.class-id-attrs._html_id')?.classList.remove('hidden');

					};

					const clickedEl = e.target.closest('.class-id-attrs');

					if (clickedEl && clickedEl.classList.contains('_html_class')) {
						clickedEl.classList.add('hidden');
						classInputField?.classList.remove('hidden');
						classInputField?.focus();
						classInputField?.addEventListener('change', () => {
							Command.setProperty(entity.id, '_html_class', classInputField.value);
						});
						return true; // don't close
					} else if (clickedEl && clickedEl.classList.contains('_html_id')) {
						clickedEl.classList.add('hidden');
						idInputField?.classList.remove('hidden');
						idInputField?.focus();
						idInputField?.addEventListener('change', () => {
							Command.setProperty(entity.id, '_html_id', idInputField.value);
						});
						return true; // don't close
					} else {
						if (!e.target.closest('.context-menu-input-field-' + entity.id)) {
							restoreDisplay();
						}
						return true; // don't close
					}
				}

			});

			_Elements.contextMenu.appendContextMenuSeparator(elements);

			return elements;
		},

		findDroppablesInIframe: (iframeDocument, id) => {
			let droppables = iframeDocument.find('[data-structr-id]');
			if (droppables.length === 0) {
				const html = iframeDocument.find('html');
				html.attr('data-structr-id', id);
				html.addClass('structr-element-container');
			}
			droppables = iframeDocument.find('[data-structr-id]');
			return droppables;
		},

		previewIframeLoaded: (iframe, highlightElementId) => {

			let designToolsActive = false;
			if (LSWrapper.getItem(_Pages.activeTabRightKey) === 'paletteTab') {
				// Design tools are open
				designToolsActive = true;
			}

			let doc = $(iframe.contentDocument || iframe.contentWindow.document);

			try {
				let head = doc.find('head');
				if (head) {
					head.append('<style media="screen">\n'
						+ '* { z-index: 0}\n'
						+ '.nodeHover { -moz-box-shadow: 0 0 5px #888; -webkit-box-shadow: 0 0 5px #888; box-shadow: 0 0 5px #888; }\n'
						+ '.structr-content-container { min-height: .25em; min-width: .25em; }\n'
						+ '.structr-element-container-active:hover { -moz-box-shadow: 0 0 5px #888; -webkit-box-shadow: 0 0 5px #888; box-shadow: 0 0 5px #888; }\n'
						+ '.structr-element-container-selected { -moz-box-shadow: 0 0 8px #860; -webkit-box-shadow: 0 0 8px #860; box-shadow: 0 0 8px #860; }\n'
						+ '.structr-element-container-selected:hover { -moz-box-shadow: 0 0 10px #750; -webkit-box-shadow: 0 0 10px #750; box-shadow: 0 0 10px #750; }\n'
						+ '.structr-editable-area { background-color: #ffe; -moz-box-shadow: 0 0 5px #888; -webkit-box-shadow: 0 0 5px yellow; box-shadow: 0 0 5px #888; }\n'
						+ '.structr-editable-area-active { background-color: #ffe; border: 1px solid orange ! important; color: #333; }\n'
						+ '.link-hover { border: 1px solid #00c; }\n'
						/**
						 * Fix for bug in Chrome preventing the modal dialog background
						 * from being displayed if a page is shown in the preview which has the
						 * transform3d rule activated.
						 */
						+ '.navbar-fixed-top { -webkit-transform: none ! important; }\n'
						+ '</style>\n');

					if (designToolsActive) {

						const mouseOverHandler = (e) => {
							let tagName     = e.target.tagName;
							let idString    = e.target.id;
							let classString = e.target.className;
							let el = e.target;
							el.classList.add('design-tools-hover');

							// if (!document.getElementById('design-tools-area').classList.contains('locked')) {
							// 	document.getElementById('design-tools-hover-tag-name-input').value = tagName;
							// 	document.getElementById('design-tools-hover-id-input').value       = idString;
							// 	document.getElementById('design-tools-hover-class-input').value    = classString;
							// }

						};

						const mouseOutHandler = (e) => {
							let tagName     = e.target.tagName;
							let idString    = e.target.id;
							let classString = e.target.className;
							let el = e.target;
							el.classList.remove('design-tools-hover');
							// document.getElementById('design-tools-hover-tag-name-input').value = tagName;
							// document.getElementById('design-tools-hover-id-input').value       = idString;
							// document.getElementById('design-tools-hover-class-input').value    = classString;
						};

						head.append('<style media="screen">\n'
							+ '*.design-tools-hover { -moz-box-shadow: 0 0 8px #72a132; -webkit-box-shadow: 0 0 8px #72a132; box-shadow: 0 0 8px #72a132; }\n'
							+ '*.design-tools-locked { -moz-box-shadow: 0 0 8px #595 ; -webkit-box-shadow: 0 0 8px #595; box-shadow: 0 0 8px #595; }\n'
							+ '</style>\n');

						for (let el of doc[0].querySelectorAll('*')) {
							// el.addEventListener('mouseenter', (e) => {
							// 	console.log('Mouseenter:', e.target);
							// 	document.getElementById('design-tools-hover-selector-input').value = e.target;
							// });
							el.addEventListener('mouseover', mouseOverHandler);
							el.addEventListener('mouseout', mouseOutHandler);

							el.addEventListener('click', (e) => {
								e.preventDefault();
								e.stopPropagation();
								//document.getElementById('design-tools-area').classList.add('locked');
								for (let otherEl of doc[0].querySelectorAll('*')) {
									otherEl.classList.remove('design-tools-hover');
									otherEl.classList.remove('design-tools-locked');
									//otherEl.removeEventListener('mouseover', mouseOverHandler);
									//otherEl.removeEventListener('mouseout', mouseOutHandler);
								}
								el.classList.add('design-tools-locked');

								// remove design-tool specific classes
								let html = el.outerHTML.replaceAll(/ ?design-tools-locked/g, '').replaceAll(' class=""', '');

								_Pages.designTools.selectedElementOrigSource = html;
								_Pages.designTools.sourceEditor.setValue(html);
								//_Pages.designTools.sourceEditor.getAction().run();
								_Pages.designTools.sourceEditor.getAction('editor.action.formatDocument').run();
								_Pages.designTools.selectedElement = el;
								_Pages.designTools.selectedElementSelector = TopLevelObject.DOMPresentationUtils.cssPath(el);

								let childTemplateNameInput = document.getElementById('design-tools-template-name-input');
								let tagString   = el.tagName[0].toUpperCase() + el.tagName.toLowerCase().slice(1) + ' Element';
								let idString    = el.id ? ('#' + el.id) : '';
								let classString = Array.from(el.classList).filter(c => c !== 'design-tools-locked').map(t => t.length > 1 ? '.' + t : null).join(' ');
								childTemplateNameInput.value = tagString + ((el.id || classString) ? ' (' : '')
										+ idString + ((el.id && classString) ? ' ' : '')
										+ classString
									+ ((el.id || classString) ? ')' : '');

							});
						}
					}

				}

				_Pages.previews.findDroppablesInIframe(doc, highlightElementId).each(function(i, element) {

					var el = $(element);

					//element.addEventListener('click', () => { _Elements.contextMenu.remove(); });

					var structrId = el.attr('data-structr-id');
					if (structrId) {

						var offsetTop = -30;
						var offsetLeft = 0;
						el.on({
							contextmenu: (e) => {
								e.stopPropagation();

								//console.log(e.clientX, e.clientY, el[0].getBoundingClientRect());

								Command.get(structrId, null, (data) => {
									const entity = StructrModel.createFromData(data);
									_Elements.contextMenu.activateContextMenu(e, el[0],
										{
											type: 'PreviewElement',
											entity: entity,
											offset: {
												x: iframe.getBoundingClientRect().x + el[0].getBoundingClientRect().x - e.clientX,
												y: iframe.getBoundingClientRect().y + el[0].getBoundingClientRect().y - e.clientY + el[0].getBoundingClientRect().height + 12
											}
										});
								}, 'ui');

								return false;
							},
							click: function(e) {
								e.stopPropagation();
								var self = $(this);
								var selected = self.hasClass('structr-element-container-selected');
								self.closest('body').find('.structr-element-container-selected').removeClass('structr-element-container-selected');
								if (!selected) {
									self.toggleClass('structr-element-container-selected');
								}

								_Pages.openAndSelectTreeObjectById(structrId);

								return false;
							},
							mouseover: function(e) {
								e.stopPropagation();
								var self = $(this);
								self.addClass('structr-element-container-active');
								_Pages.highlight(structrId);
								var pos = self.position();
								var header = self.children('.structr-element-container-header');
								header.css({
									position: "absolute",
									top: pos.top + offsetTop + 'px',
									left: pos.left + offsetLeft + 'px',
									cursor: 'pointer'
								}).show();
							},
							mouseout: function(e) {
								e.stopPropagation();
								var self = $(this);
								self.removeClass('.structr-element-container');
								var header = self.children('.structr-element-container-header');
								header.remove();
								_Pages.unhighlight(structrId);
							}
						});
					}
				});

			} catch (e) { console.log('Error:', e)}

			_Pages.previews.activateComments(doc);
		},

		getComments: (el) => {

			let comments = [];
			let child    = el.firstChild;

			while (child) {

				if (child.nodeType === 8) {
					let id = _Helpers.extractVal(child.nodeValue, 'data-structr-id');

					if (id) {
						let raw = _Helpers.extractVal(child.nodeValue, 'data-structr-raw-value');

						if (raw !== undefined) {
							comments.push({
								id: id,
								node: child,
								rawContent: raw
							});
						}
					}
				}
				child = child ? child.nextSibling : child;
			}
			return comments;
		},

		getNonCommentSiblings: (el) => {

			let siblings = [];
			let sibling  = el.nextSibling;

			while (sibling) {
				if (sibling.nodeType === 8) {
					return siblings;
				}
				siblings.push(sibling);
				sibling = sibling.nextSibling;
			}
		},
		activateComments: function(doc, callback) {

			doc.find('*').each(function(i, element) {

				_Pages.previews.getComments(element).forEach(function(c) {

					let inner  = $(_Pages.previews.getNonCommentSiblings(c.node));
					let newDiv = $(`<div data-structr-id="${c.id}" data-structr-raw-content="${_Helpers.escapeForHtmlAttributes(c.rawContent, false)}"></div>`);

					newDiv.append(inner);
					$(c.node).replaceWith(newDiv);

					$(newDiv).on({
						mouseover: function(e) {
							e.stopPropagation();
							let self = $(this);

							self.addClass('structr-editable-area');
							_Pages.highlight(self.attr('data-structr-id'));
						},
						mouseout: function(e) {
							e.stopPropagation();
							let self = $(this);

							self.removeClass('structr-editable-area');
							_Pages.unhighlight(self.attr('data-structr-id'));
						},
						click: function(e) {
							e.stopPropagation();
							e.preventDefault();
							let self = $(this);

							if (_Pages.contentSourceId) {
								// click on same element again?
								if (self.attr('data-structr-id') === _Pages.contentSourceId) {
									return;
								}
							}
							_Pages.contentSourceId = self.attr('data-structr-id');

							if (self.hasClass('structr-editable-area-active')) {
								return false;
							}
							self.removeClass('structr-editable-area').addClass('structr-editable-area-active').prop('contenteditable', true).focus();

							// Store old text in global var and attribute
							_Pages.textBeforeEditing = self.text();

							let srcText = _Helpers.expandNewline(self.attr('data-structr-raw-content'));

							// Replace only if it differs (e.g. for variables)
							if (srcText !== _Pages.textBeforeEditing) {
								self.html(srcText);
								_Pages.textBeforeEditing = srcText;
							}
							_Pages.expandTreeNode(_Pages.contentSourceId);
							return false;
						},
						blur: function(e) {
							e.stopPropagation();
							_Pages.previews.saveInlineElement(this, callback);
						}
					});
				});
			});
		},
		saveInlineElement: (el, callback) => {
			let self = $(el);
			_Pages.contentSourceId = self.attr('data-structr-id');

			let text = _Helpers.unescapeTags(_Helpers.cleanText(self.html()));

			self.attr('contenteditable', false);
			self.removeClass('structr-editable-area-active').removeClass('structr-editable-area');

			Command.setProperty(_Pages.contentSourceId, 'content', text, false, (obj) => {
				if (_Pages.contentSourceId === obj.id) {
					callback?.();
					_Pages.contentSourceId = null;
				}
			});
		},
		isPreviewActive: () => {

			// only reload if the iframe is already present!
			let iframe = _Pages.centerPane.querySelector('iframe');

			if (iframe) {
				return true;
			}

			return false;
		},
		getBaseUrlForPage: (entity) => {
			let pagePath = entity.path ? entity.path.replace(/^\//, '') : entity.name;
			let detailsObject     = (LSWrapper.getItem(_Pages.detailsObjectIdKey + entity.id) ? '/' + LSWrapper.getItem(_Pages.detailsObjectIdKey + entity.id) : '');
			let previewUrl        = (entity.site && entity.site.hostname ? '//' + entity.site.hostname + (entity.site.port ? ':' + entity.site.port : '') + '/' : Structr.viewRootUrl) + pagePath + detailsObject;

			return previewUrl;
		},
		getUrlForPage: (entity) => {
			let requestParameters = (LSWrapper.getItem(_Pages.requestParametersKey + entity.id) ? '?' + LSWrapper.getItem(_Pages.requestParametersKey + entity.id) : '');
			return _Pages.previews.getBaseUrlForPage(entity) + requestParameters;
		},
		getUrlForPreview: (entity) => {
			let requestParameters = (LSWrapper.getItem(_Pages.requestParametersKey + entity.id) ? '&' + LSWrapper.getItem(_Pages.requestParametersKey + entity.id) : '');
			return _Pages.previews.getBaseUrlForPage(entity) + '?' + Structr.getRequestParameterName('edit') + '=2' + requestParameters;
		},
		showPreviewInIframe: (pageId, highlightElementId, parentElement) => {

			parentElement = (parentElement && parentElement.length) ? parentElement[0] : _Pages.centerPane;

			if (pageId) {

				let innerFn = () => {

					_Pages.previews.activePreviewPageId = pageId;
					if (highlightElementId) {
						_Pages.previews.activePreviewHighlightElementId = highlightElementId;
					}

					Command.get(pageId, 'id,name,path,site', (pageObj) => {

						parentElement.insertAdjacentHTML('beforeend', _Pages.templates.preview({ pageId: pageObj.id }));

						let iframe = parentElement.querySelector('iframe');

						if (highlightElementId) {
							_Entities.highlightElement(Structr.node(highlightElementId));
						}

						iframe.onload = () => {

							_Pages.previews.previewIframeLoaded(iframe, highlightElementId);

							let doc       = iframe.contentDocument || iframe.contentWindow.document;
							let previewEl = doc.querySelector('[data-structr-id="' + highlightElementId + '"]');

							for (let el of doc.querySelectorAll('.structr-element-container-selected')) {
								el.classList.remove('structr-element-container-selected');
							}

							previewEl?.classList.add('structr-element-container-selected');

							doc.addEventListener('click', () => { _Elements.contextMenu.remove(); });
						}

						iframe.src = _Pages.previews.getUrlForPreview(pageObj);
					});

					Structr.resize();
				};

				if (_Pages.previews.loadPreviewTimer) {
					window.clearTimeout(_Pages.previews.loadPreviewTimer);
				}

				if (parentElement) {
					innerFn();
				} else {
					_Pages.previews.loadPreviewTimer = window.setTimeout(innerFn, 100);
				}
			}
		},
		showPreviewInIframeIfVisible: (pageId, highlightElementId) => {

			if (_Pages.previews.isPreviewActive()) {
				_Pages.previews.showPreviewInIframe(pageId, highlightElementId);
			}
		},
		reloadPreviewInIframe: () => {

			if (_Pages.previews.isPreviewActive()) {
				_Pages.previews.showPreviewInIframe(_Pages.previews.activePreviewPageId, _Pages.previews.activePreviewHighlightElementId);
			}
		},
		isPreviewForActiveForPage: (pageId) => {

			return (_Pages.previews.activePreviewPageId === pageId && _Pages.previews.isPreviewActive());
		},
		modelForPageUpdated: (pageId) => {

			if (_Pages.previews.isPreviewForActiveForPage(pageId)) {
				_Pages.previews.reloadPreviewInIframe();
			}

			if (_Pages.previewSlideout.hasClass('open')) {
				_Pages.previews.updatePreviewSlideout();
			}

		},
		updatePreviewSlideout: () => {

			let elementId = _Pages.centerPane.dataset['elementId'] ?? LSWrapper.getItem(_Entities.selectedObjectIdKey);

			if (elementId) {
				Command.get(elementId, 'id,type,name,isPage,pageId', (entity) => {
					_Helpers.fastRemoveAllChildren(_Pages.previewSlideout[0]);
					_Pages.previews.showPreviewInIframe(entity.isPage ? entity.id : entity.pageId, elementId, _Pages.previewSlideout);
				});
			}
		},
		configurePreview: (entity, container) => {

			let detailsObjectIdInput = container.querySelector('#_details-object-id');

			detailsObjectIdInput.addEventListener('change', () => {

				let oldVal = LSWrapper.getItem(_Pages.detailsObjectIdKey + entity.id) || null;
				let newVal = detailsObjectIdInput.value;
				if (newVal !== oldVal) {

					_Helpers.blinkGreen(detailsObjectIdInput);

					if (newVal === '') {
						LSWrapper.removeItem(_Pages.detailsObjectIdKey + entity.id);
					} else {
						LSWrapper.setItem(_Pages.detailsObjectIdKey + entity.id, newVal);
					}

					_Pages.previews.modelForPageUpdated(entity.id);
				}
			});

			let requestParametersInput = container.querySelector('#_request-parameters');

			requestParametersInput.addEventListener('change', () => {
				let oldVal = LSWrapper.getItem(_Pages.requestParametersKey + entity.id) || null;
				let newVal = requestParametersInput.value;
				if (newVal !== oldVal) {

					_Helpers.blinkGreen(requestParametersInput);

					if (newVal === '') {
						LSWrapper.removeItem(_Pages.requestParametersKey + entity.id);
					} else {
						LSWrapper.setItem(_Pages.requestParametersKey + entity.id, newVal);
					}

					_Pages.previews.modelForPageUpdated(entity.id);
				}
			});

			let autoRefreshCheckbox = container.querySelector('#_auto-refresh');
			autoRefreshCheckbox.addEventListener('change', () => {
				let key = _Pages.autoRefreshDisabledKey + entity.id;
				let autoRefreshDisabled = (LSWrapper.getItem(key) === '1');

				if (autoRefreshDisabled) {
					LSWrapper.removeItem(key);
				} else {
					LSWrapper.setItem(key, '1');
				}
				_Helpers.blinkGreen(autoRefreshCheckbox.parentNode);
			});
		},
	},

	designTools: {
		sourceEditor: null,
		selectedElement: null,
		urlHistoryKey: 'design-tools-url-history',
		reload: () => {
			//console.log('Design tools opened');

			let html = `
				<div class="inner">
					<div class="mr-12" id="design-tools-area">

						<h3>Import from page</h3>
						<div class="w-full mb-4">
							<label class="block mb-2" for="design-tools-url-input" data-comment="Must be full URL including scheme">Enter URL of example page to preview</label>
							<input class="w-full rounded-r" style="margin-left: -1px" type="text" id="design-tools-url-input">
						</div>
						<div class="w-full mb-4">
							<label class="block mb-2" for="design-tools-url-history-select">URL history</label>
							<select id="design-tools-url-history-select" class=""><option></option></optin></select>
						</div>
						<div class="w-full mb-4">
							<label class="block mb-2" for="design-tools-page-name-input">Page name</label>
							<input id="design-tools-page-name-input" class="w-full" type="text">
						</div>
						<div class="w-full mb-4">
							<label class="block mb-2" for="design-tools-page-template-name-input">Page template name</label>
							<input id="design-tools-page-template-name-input" class="w-full" type="text">
						</div>
						<div class="w-full mb-8">
							<button class="hover:bg-gray-100 focus:border-gray-666 active:border-green" id="design-tools-create-page-button">Create new page</button>
						</div>

						<h3>Select element</h3>
						<p>Hover over elements in the preview page. Click to select and lock an element.</p>
						<div class="grid grid-cols-6 gap-4">
							<!--
							<div class="col-span-3 mr-4">
								<label class="block mb-2" for="design-tools-url-input">Tag name</label>
								<input id="design-tools-hover-tag-name-input" class="w-full" type="text">
							</div>
							<div class="col-span-3">
								<label class="block mb-2" for="design-tools-url-input">ID</label>
								<input id="design-tools-hover-id-input" class="w-full" type="text">
							</div>
							<div class="col-span-6">
								<label class="block mb-2" for="design-tools-url-input">CSS Classes</label>
								<input id="design-tools-hover-class-input"class="w-full" type="text">
							</div>
							-->
							<div class="col-span-6 h-80 mb-4" id="design-tool-source-editor-area">
								<label class="block mb-2" for="design-tools-source-code">Source code</label>
								<!--textarea id="design-tool-source-textarea" class="w-full h-40"></textarea-->
							</div>
							<div class="col-span-6">
								<label class="block mb-2" for="design-tools-template-name-input">Template name</label>
								<input id="design-tools-template-name-input" class="w-full" type="text">
							</div>
							<div class="col-span-3">
								<label class="" for="design-tools-create-as-dom-tree">Create as DOM tree</label>
							</div>
							<div class="col-span-3">
								<input id="design-tools-create-as-dom-tree" class="" type="checkbox">
							</div>
							<div class="col-span-3">
								<label class="" for="design-tools-remove-siblings">Remove similar siblings (for repeater)</label>
							</div>
							<div class="col-span-3">
								<input id="design-tools-remove-siblings" class="" type="checkbox">
							</div>
							<div class="cols-span-6">
								<button class="hover:bg-gray-100 focus:border-gray-666 active:border-green" id="design-tools-create-child-template">Create sub node</button>
							</div>
						</div>
					</div>
				</div>
			`;

			let designTools = document.getElementById('palette');
			_Helpers.fastRemoveAllChildren(designTools);
			designTools.insertAdjacentHTML('afterbegin', html);

			_Helpers.activateCommentsInElement(designTools);

			let pageTemplateNameInput = document.getElementById('design-tools-page-template-name-input');
			let pageNameInput         = document.getElementById('design-tools-page-name-input');
			let urlInput              = document.getElementById('design-tools-url-input');

			document.getElementById('design-tools-create-page-button').addEventListener('click', (e) => {

				Command.importPageAsTemplate(urlInput.value, pageNameInput.value, pageTemplateNameInput.value, (newPage) => {
					//console.log('Page imported as template', newPage);

					//Command.setProperty(newPage.children[0].id)

				});

				// Command.create({ type: 'Page', name: pageNameInput.value }, (newPage) => {
				//
				// 	//console.log('New page created', newPage);
				// 	let previewIframe = document.querySelector('.previewBox iframe');
				// 	let html = previewIframe.contentDocument.documentElement.outerHTML.replaceAll(/ ?design-tools-locked/g, '').replaceAll(' class=""', '');
				// 	//console.log(previewIframe.contentDocument); return;
				// 	let pageTemplateNameInput = document.getElementById('design-tools-page-template-name-input');
				//
				// 	fetch(urlInput.value)
				// 		.then(response => response.text())
				// 		.then(html => {
				//
				// 			Command.create({
				// 				type: 'Template',
				// 				name: pageTemplateNameInput.value,
				// 				content: html,
				// 				contentType: 'text/html'
				// 			}, (newTemplate) => {
				// 				console.log('New template created', newTemplate);
				// 				Command.appendChild(newTemplate.id, newPage.id);
				// 			});
				//
				// 		});
				// });
			});

			const createChildTemplates = (obj, parentTemplateContent) => {

				//console.log('Root of createChildTemplates with obj', obj);

				for (let child of obj.children) {

					//console.log('Testing child', child);

					if (child.type === 'Template') {

						Command.get(child.id, 'id,type,name,content,children', (template) => {
							//console.log('Template found', childObj.content);

							// Match template content with source code of currently selected element
							// let selectedElementSource = _Pages.designTools.selectedElementOrigSource.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
							// let templateContent = template.content.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

							let selectedElementSource = _Pages.designTools.selectedElementOrigSource.replace(/[.*+?^#%${}()|[\]\\]/g, '\\$&');
							let templateContent = template.content; //.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

							//let regex = new RegExp(RegExp.escape(selectedElementSource));
							let regex = new RegExp(selectedElementSource);
							let matches = regex.test(templateContent);
							let el = _Pages.designTools.selectedElement;

							let childTemplateNameInput = document.getElementById('design-tools-template-name-input');
							let newChildTemplateName   = childTemplateNameInput.value;

							//let tmpDoc      = document.implementation.createHTMLDocument();
							//let tmpEl       = tmpDoc.createElement('html');
							//tmpEl.innerHTML = selectedElementSource;

							let templateTmpDoc      = document.implementation.createHTMLDocument();
							let templateTmpEl       = templateTmpDoc.createElement('html');
							templateTmpEl.innerHTML = templateContent;

							//console.log(parentTemplateContent);
							//console.log(templateContent);

							// TODO: Bei der Suche in Child-Elementen muss der relative Pfad berücksichtigt werden.
							// Das Matching muss von der Stelle aus gemacht werden, wo das ${include_child()} im Parent-Template steht.
							// Oder es muss der Reverse CSS Path immer mit class (und id) aufgelöst werden ...

							//console.log(templateTmpEl);

							let selectedEl = templateTmpEl.querySelector(_Pages.designTools.selectedElementSelector); //.replace('html > body > ', ''));
							//console.log(_Pages.designTools.selectedElementSelector);
							//console.log(selectedEl); return;



							//console.log(templateTmpEl); return;

							if (selectedEl) {

								selectedEl.outerHTML = '${include_child(\'' + newChildTemplateName + '\')}';

								//console.log('Match!');

								// If it matches, replace by ${include_child(...)} expression

								//Command.setProperty(template.id, 'content', templateContent.replace(regex, '${include_child(\'' + newChildTemplateName + '\')}\n'));

								let newHTML = templateContent.startsWith('<!DOCTYPE') ? '<!DOCTYPE html>\n' + templateTmpEl.outerHTML : templateTmpEl.querySelector('html > body').innerHTML;

								Command.setProperty(template.id, 'content', newHTML);

								let newTemplateContent = _Pages.designTools.sourceEditor.getValue();

								Command.create({
									type: 'Template',
									name: newChildTemplateName,
									content: newTemplateContent,
									contentType: 'text/html'
								}, (newTemplate) => {
									//console.log('New template created', newTemplate);
									Command.appendChild(newTemplate.id, template.id);
								});

							} else {
								//console.log('No match, step down to', template);
								createChildTemplates(template, templateContent);
							}
						});

					}
				}
			};

			document.getElementById('design-tools-create-child-template').addEventListener('click', (e) => {
				// To create a new template, we must figure out which is the parent template first
				// Get currently selected page and iterate through the templates to find the corresponding HTML
				let activePageId = _Pages.previews.activePreviewPageId;
				Command.get(activePageId, 'id,type,name,children', (pageObj) => {
					createChildTemplates(pageObj);
				});
			});

			const updateUrlHistorySelect = () => {

				let urlHistorySelectEl = document.getElementById('design-tools-url-history-select');
				_Helpers.fastRemoveAllChildren(urlHistorySelectEl);

				urlHistorySelectEl.insertAdjacentHTML('beforeend', '<option disabled selected></option>');

				for (let urlHistoryEntry of (LSWrapper.getItem(_Pages.designTools.urlHistoryKey) || []).reverse()) {
					urlHistorySelectEl.insertAdjacentHTML('beforeend', '<option>' + urlHistoryEntry + '</option>');
				}
			}

			const validateUrl = (element, url) => {

				try {
					new URL(url);
				} catch(e) {
					_Helpers.blinkRed(element);

					return false;
				}

				return true;
			};

			const loadPreviewPage = async (url) => {

				let response = await fetch('/structr/proxy?url=' + url);

				if (response.ok === false) {

					if (response.status === 503) {

						new WarningMessage().title(response.statusText).text('ProxyServlet not available - this can be configured via the <b><code>application.proxy.mode</code></b> setting in structr.conf').requiresConfirmation().show();

					} else {

						new WarningMessage().title(response.statusText).text('Unknown error in ProxyServlet. Please check the server log and act accordingly.').requiresConfirmation().show();
					}

					return;
				}

				let html = await response.text();

				_Pages.hideAllFunctionBarTabs();

				// clear UI in Pages tree
				_Entities.selectedObject = null;
				_Entities.deselectAllElements();
				_Entities.removeSelectedNodeClassFromAllNodes();

				document.querySelector('a[href="#pages:preview"]').closest('li').classList.remove('hidden');
				document.querySelector('a[href="#pages:preview"]').click();

				// make sure center pane + iframe are created to prevent duplicate handlers
				_Pages.emptyCenterPane();

				_Pages.centerPane.insertAdjacentHTML('beforeend', _Pages.templates.preview({ pageId: null }));
				let previewIframe = document.querySelector('.previewBox iframe');
				_Pages.showTabsMenu();
				previewIframe.onload = () => {
					_Pages.previews.previewIframeLoaded(previewIframe);
				};

				previewIframe.addEventListener('load', (e) => {

					let pageName;
					let pageHref = previewIframe.contentDocument.documentElement.querySelector('base').href;
					if (pageHref) {
						let hrefParts = pageHref.split('/');
						pageName = hrefParts[hrefParts.length-2];
					}
					pageNameInput.value = pageName;

					let pageTitle = previewIframe.contentDocument.documentElement.querySelector('title').innerText;
					pageTemplateNameInput.value = pageTitle;
				});

				previewIframe = document.querySelector('.previewBox iframe');
				previewIframe.srcdoc = html;
			}

			let urlHistorySelectEl = document.getElementById('design-tools-url-history-select');
			urlHistorySelectEl.addEventListener('change', async (e) => {
				let url = e.target.value;

				if (validateUrl(e.target, url)) {
					urlInput.value = url;
					await loadPreviewPage(url);
				}
			});

			updateUrlHistorySelect();

			urlInput.addEventListener('keyup', async (e) => {
				let inputElement = e.target;
				switch (e.key) {
					case 'Enter':

						if (validateUrl(inputElement, inputElement.value)) {

							let history = LSWrapper.getItem(_Pages.designTools.urlHistoryKey);
							if (!history || (history.length && history.indexOf(inputElement.value) === -1)) {
								LSWrapper.setItem(_Pages.designTools.urlHistoryKey, (LSWrapper.getItem(_Pages.designTools.urlHistoryKey) || []).concat(inputElement.value));
							}

							updateUrlHistorySelect();

							await loadPreviewPage(inputElement.value);
						}

						break;

					default:
						return;
				}
			});

			let sourceEditorTextElement = document.getElementById('design-tool-source-textarea');
			let sourceEditorConfig = {
				//value: 'test',
				language: 'text/html',
				lint: true,
				autocomplete: true,
				autoIndent: 'full',
				automaticLayout: true,
				//changeFn: (editor, entity) => { },
				isAutoscriptEnv: true,
				//saveFn: saveFunction,
				//saveFnText: 'SAVE TEXT'
			};

			let sourceEditorArea = document.getElementById('design-tool-source-editor-area');
			//let sourceEditor = _Editors.getMonacoEditor({ id: 'dummy-id'}, 'id', $(sourceEditorArea), sourceEditorConfig);

			_Pages.designTools.sourceEditor = monaco.editor.create(sourceEditorArea, sourceEditorConfig);
		}
	},

	sharedComponents: {
		allowDropClass: 'allow-drop',
		reload: (isReloadFromDragEvent = false) => {

			if (!_Pages.componentsSlideout) return;

			Command.listComponents(1000, 1, 'name', 'asc', (result) => {

				_Helpers.fastRemoveAllChildren(_Pages.componentsSlideout[0]);

				_Pages.componentsSlideout[0].insertAdjacentHTML('beforeend', `
					<div id="newComponentDropzone" class="element-dropzone">
						<div class="info-icon h-16 flex items-center justify-center">
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['m-2', 'active', 'icon-green', 'flex-none']))}
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['m-2', 'inactive', 'flex-none'])}
							Drop element here to create a new shared component
						</div>
					</div>
					<div id="componentsArea"></div>
				`);

				if (isReloadFromDragEvent === true) {
					_Pages.sharedComponents.highlightNewSharedComponentDropZone();
				}

				_Dragndrop.pages.enableNewSharedComponentDropzone();

				_Elements.appendEntitiesToDOMElement(result, $('#componentsArea', _Pages.componentsSlideout));

				Structr.refreshPositionsForCurrentlyActiveSortable();
			});
		},
		createNew: (sourceId) => {

			_Elements.dropBlocked = true;

			if (!sourceId) {
				return false;
			}

			Command.createComponent(sourceId);

			_Elements.dropBlocked = false;
		},
		getNewSharedComponentDropzone: () => {
			return document.querySelector('#newComponentDropzone');
		},
		isNodeInSharedComponents: (node) => {
			return (node.closest('#componentsArea') !== null);
		},
		highlightNewSharedComponentDropZone: () => {
			_Pages.sharedComponents.getNewSharedComponentDropzone()?.classList.add(_Pages.sharedComponents.allowDropClass);
		},
		unhighlightNewSharedComponentDropZone: () => {
			_Pages.sharedComponents.getNewSharedComponentDropzone()?.classList.remove(_Pages.sharedComponents.allowDropClass);
		},
		isDropAllowed: () => {
			return _Pages.sharedComponents.getNewSharedComponentDropzone()?.classList.contains(_Pages.sharedComponents.allowDropClass) ?? false;
		}
	},

	unattachedNodes: {
		reload: () => {

			if (_Pages.unusedElementsSlideout.hasClass('open')) {

				_Pages.unattachedNodes.removeElementsFromUI();

				_Pages.unusedElementsTree.append(`
					<button class="btn disabled flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green" id="delete-all-unattached-nodes" disabled>
						<span>Loading</span>${_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 24, 24, ['ml-2'])}
					</button>
				`);

				let deleteUnattachedNodesButton = _Pages.unusedElementsTree[0].querySelector('#delete-all-unattached-nodes');

				deleteUnattachedNodesButton.addEventListener('click', async () => {

					let confirm = await _Dialogs.confirmation.showPromise('<p>Delete all DOM elements without parent?</p>');
					if (confirm === true) {

						Command.deleteUnattachedNodes();

						Structr.slideouts.closeRightSlideOuts([_Pages.unusedElementsSlideout], _Pages.rightSlideoutClosedCallback);
					}
				});

				Command.listUnattachedNodes(1000, 1, 'name', 'asc', (result) => {

					let count = result.length;
					if (count > 0) {

						deleteUnattachedNodesButton.innerHTML = `${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, ['mr-2'])} Delete all (${count})`;
						deleteUnattachedNodesButton.classList.add('hover:bg-gray-100');

						_Helpers.disableElements(false, deleteUnattachedNodesButton);

					} else {

						deleteUnattachedNodesButton.textContent = 'No unused elements';
						deleteUnattachedNodesButton.classList.remove('hover:bg-gray-100');
					}

					_Elements.appendEntitiesToDOMElement(result, _Pages.unusedElementsTree);
				});
			}
		},
		removeElementsFromUI: () => {
			_Helpers.fastRemoveAllChildren(_Pages.unusedElementsTree[0]);
		},
		blinkUI: () => {
			_Helpers.blinkGreen(document.getElementById('elementsTab'));
		}
	},

	linkableDialog: {
		nodeClasses: 'inline-flex flex-col items-start gap-x-2',
		show: (entity, targetNode) => {

			targetNode = $(targetNode);
			targetNode.empty();

			if (entity.tag !== 'img') {

				targetNode.append(`
					<p>Click on a Page, File or Image to establish a hyperlink to this &lt;${entity.tag}&gt; element.</p>
					<h3>Pages</h3><div class="linkBox" id="pagesToLink"></div>
				`);

				let pagesToLink = document.querySelector('#pagesToLink');

				_Pager.initPager('pages-to-link', 'Page', 1, 25);
				let linkPagePager = _Pager.addPager('pages-to-link', pagesToLink, true, 'Page', null, (pages) => {

					for (let page of pages) {

						if (page.type !== 'ShadowDocument') {

							let div = _Helpers.createSingleDOMElementFromHTML(`
								<div class="node page ${_Pages.linkableDialog.nodeClasses}">
									<div class="node-container flex items-center gap-x-2 p-2">
										${_Icons.getSvgIcon(_Icons.iconDomTreePageIcon, 16, 16, ['icon-grey'])}<b title="${_Helpers.escapeForHtmlAttributes(page.name)}" class="name_ abbr-ellipsis abbr-120">${page.name}</b>
									</div>
								</div>
							`);

							pagesToLink.appendChild(div);

							_Pages.linkableDialog.handleLinkableElement(div, entity, page);
						}
					}
				}, undefined, undefined, true);

				linkPagePager.appendFilterElements('<span style="white-space: nowrap;">Filter: <input type="text" class="filter" data-attribute="name" placeholder="Name"></span>');
				linkPagePager.activateFilterElements();
				linkPagePager.setIsPaused(false);
				linkPagePager.refresh();

				targetNode.append('<h3>Files</h3><div class="linkBox" id="foldersToLink"></div><div class="linkBox" id="filesToLink"></div>');

				let filesToLink   = document.querySelector('#filesToLink');
				let foldersToLink = document.querySelector('#foldersToLink');

				_Pager.initPager('folders-to-link', 'Folder', 1, 25);
				_Pager.forceAddFilters('folders-to-link', 'Folder', { hasParent: false });
				let linkFolderPager = _Pager.addPager('folders-to-link', foldersToLink, true, 'Folder', 'ui', (folders) => {

					for (let folder of folders) {
						_Pages.linkableDialog.appendFolder(entity, foldersToLink, folder);
					}
				}, null, 'id,name,hasParent', true);

				linkFolderPager.appendFilterElements('<span style="white-space: nowrap;">Filter: <input type="text" class="filter" data-attribute="name" placeholder="Name"></span>');
				linkFolderPager.activateFilterElements();
				linkFolderPager.setIsPaused(false);
				linkFolderPager.refresh();

				_Pager.initPager('files-to-link', 'File', 1, 25);
				let linkFilesPager = _Pager.addPager('files-to-link', filesToLink, true, 'File', 'ui', (files) => {

					for (let file of files) {

						let div = _Helpers.createSingleDOMElementFromHTML(`
							<div class="node file ${_Pages.linkableDialog.nodeClasses}">
								<div class="node-container flex items-center gap-x-2 p-2">
									${_Icons.getSvgIcon(_Icons.getFileIconSVG(file))}<b title="${_Helpers.escapeForHtmlAttributes(file.path)}" class="name_ abbr-ellipsis abbr-120">${file.name}</b>
								</div>
							</div>
						`);

						filesToLink.appendChild(div);

						_Pages.linkableDialog.handleLinkableElement(div, entity, file);
					}
				}, null, 'id,name,contentType,linkingElementsIds,path', true);

				linkFilesPager.appendFilterElements('<span style="white-space: nowrap;">Filters: <input type="text" class="filter" data-attribute="name" placeholder="Name"><label><input type="checkbox"  class="filter" data-attribute="hasParent"> Include subdirectories</label></span>');
				linkFilesPager.activateFilterElements();
				linkFilesPager.setIsPaused(false);
				linkFilesPager.refresh();
			}

			if (entity.tag === 'img' || entity.tag === 'link' || entity.tag === 'a') {

				targetNode.append('<h3>Images</h3><div class="linkBox" id="imagesToLink"></div>');

				let imagesToLink = document.querySelector('#imagesToLink');

				_Pager.initPager('images-to-link', 'Image', 1, 25);
				let linkImagePager = _Pager.addPager('images-to-link', imagesToLink, false, 'Image', 'ui', (images) => {

					for (let image of images) {

						let div = _Helpers.createSingleDOMElementFromHTML(`
							<div class="node file ${_Pages.linkableDialog.nodeClasses}" title="${_Helpers.escapeForHtmlAttributes(image.path)}">
								<div class="node-container flex items-center gap-x-2 p-2">
									${_Icons.getImageOrIcon(image)}<b class="name_ abbr-ellipsis abbr-120">${image.name}</b>
								</div>
							</div>
						`);

						imagesToLink.append(div);

						_Pages.linkableDialog.handleLinkableElement(div, entity, image);
					}
				}, null, 'id,name,contentType,linkingElementsIds,path,tnSmall', true);

				linkImagePager.appendFilterElements('<span style="white-space: nowrap;">Filter: <input type="text" class="filter" data-attribute="name" placeholder="Name"></span>');
				linkImagePager.activateFilterElements();
				linkImagePager.setIsPaused(false);
				linkImagePager.refresh();
			}
		},
		appendFolder: (entityToLinkTo, folderEl, subFolder) => {

			let subFolderEl = _Helpers.createSingleDOMElementFromHTML(`
				<div class="node folder ${(subFolder.hasParent ? 'sub ' : '')}${_Pages.linkableDialog.nodeClasses}">
					<div class="node-container flex items-center gap-x-2 p-2">
						${_Icons.getSvgIcon(_Icons.iconFolderClosed, 16, 16)}<b title="${_Helpers.escapeForHtmlAttributes(subFolder.name)}" class="name_ abbr-ellipsis abbr-200">${subFolder.name}</b>
					</div>
				</div>
			`);
			folderEl.appendChild(subFolderEl);

			let nodeContainer = subFolderEl.querySelector('.node-container');

			subFolderEl.addEventListener('click', (e) => {
				e.stopPropagation();
				e.preventDefault();

				let folderIsOpen = _Icons.hasSvgIcon(nodeContainer, _Icons.iconFolderOpen);

				if (!folderIsOpen) {

					_Pages.linkableDialog.expandFolder(entityToLinkTo, subFolder, subFolderEl);

				} else {

					for (let node of subFolderEl.querySelectorAll('.node')) {
						node.remove();
					}

					_Icons.updateSvgIconInElement(nodeContainer, _Icons.iconFolderOpen, _Icons.iconFolderClosed);
				}

				return false;
			});

			nodeContainer.addEventListener('mouseenter', (e) => {
				nodeContainer.classList.add('nodeHover');
			});
			nodeContainer.addEventListener('mouseleave', (e) => {
				nodeContainer.classList.remove('nodeHover');
			});

		},
		expandFolder: (entityToLinkTo, folder, folderEl) => {

			Command.get(folder.id, 'id,name,hasParent,files,folders', (node) => {

				_Icons.updateSvgIconInElement(folderEl, _Icons.iconFolderClosed, _Icons.iconFolderOpen);

				for (let subFolder of node.folders) {
					_Pages.linkableDialog.appendFolder(entityToLinkTo, folderEl, subFolder);
				}

				for (let f of node.files) {

					Command.get(f.id, 'id,name,contentType,linkingElementsIds,path', (file) => {

						let div = _Helpers.createSingleDOMElementFromHTML(`
							<div class="node file sub ${_Pages.linkableDialog.nodeClasses}">
								<div class="node-container flex items-center gap-x-2 p-2">
									${_Icons.getSvgIcon(_Icons.getFileIconSVG(file))}<b title="${_Helpers.escapeForHtmlAttributes(file.path)}" class="name_ abbr-ellipsis abbr-200">${file.name}</b>
								</div>
							</div>
						`);
						folderEl.appendChild(div);

						_Pages.linkableDialog.handleLinkableElement(div, entityToLinkTo, file);
					});
				}
			});
		},
		handleLinkableElement: (div, entityToLinkTo, linkableObject) => {

			if (_Helpers.isIn(entityToLinkTo.id, linkableObject.linkingElementsIds)) {
				div.classList.add('nodeActive');
			}

			div.addEventListener('click', (e) => {

				e.stopPropagation();

				let isActive = div.classList.contains('nodeActive');

				div.closest('.linkable-container')?.querySelector('.node.nodeActive')?.classList.remove('nodeActive');

				if (isActive) {

					Command.setProperty(entityToLinkTo.id, 'linkableId', null);

				} else {

					let attrName = (entityToLinkTo.type === 'Link') ? '_html_href' : '_html_src';

					Command.getProperty(entityToLinkTo.id, attrName, (val) => {
						if (!val || val === '') {
							Command.setProperty(entityToLinkTo.id, attrName, '${link.path}', null);
						}
					});

					Command.link(entityToLinkTo.id, linkableObject.id);

					div.classList.add('nodeActive');
				}

				_Entities.reloadChildren(entityToLinkTo.parent.id);
			});

			div.addEventListener('mouseenter', (e) => {
				div.classList.add('nodeHover');
			});
			div.addEventListener('mouseleave', (e) => {
				div.classList.remove('nodeHover');
			});
		},
	},

	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/pages.css">

			<div class="column-resizer-blocker"></div>
			<div class="column-resizer column-resizer-left hidden"></div>
			<div class="column-resizer column-resizer-right hidden"></div>

			<div class="slideout-activator left" id="pagesTab">
				<svg xmlns="http://www.w3.org/2000/svg" version="1.1" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 28 28" width="24" height="24">
					<g transform="matrix(1,0,0,1,0,0)">
						<path d="M9.750 18.748 L23.250 18.748 L23.250 23.248 L9.750 23.248 Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M9.750 11.248 L23.250 11.248 L23.250 15.748 L9.750 15.748 Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M0.750 0.748 L14.250 0.748 L14.250 5.248 L0.750 5.248 Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M6.75,5.248v1.5a1.5,1.5,0,0,0,1.5,1.5h4.5a1.5,1.5,0,0,1,1.5,1.5v1.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M17.25 15.748L17.25 18.748" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path>
					</g>
				</svg>
				<br>
				Page Tree
			</div>

			<div id="pages" class="slideOut slideOutLeft">
				<div id="pages-controls">
					<div id="pagesPager"></div>

					<div id="pages-actions" class="dropdown-menu darker-shadow-dropdown dropdown-menu-large">
						<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green">
							${_Icons.getSvgIcon(_Icons.iconAdd)}
						</button>
						<div class="dropdown-menu-container">

							<div class="flex flex-col divide-x-0 divide-y">
								<a id="create_page" title="Create Page" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer p-4">
									${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'mr-2')} Create Page
								</a>

								<a id="import_page" title="Import Template" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer p-4">
									${_Icons.getSvgIcon(_Icons.iconCreateFile, 16, 16, 'mr-2')} Import Page
								</a>

								<!--a id="add_template" title="Add Template" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green cursor-pointer p-4">
									${_Icons.getSvgIcon(_Icons.iconMagicWand)} Add Template
								</a-->
							</div>
						</div>
					</div>
				</div>
				<div id="pagesTree"></div>
			</div>

			<div class="slideout-activator left" id="localizationsTab">
				<svg xmlns="http://www.w3.org/2000/svg" version="1.1" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 28 28" width="24" height="24">
					<g transform="matrix(1,0,0,1,0,0)">
						<path d="M19.652 0.748L15.902 2.998 18.152 6.748" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M23.25,8.187A6.749,6.749,0,0,0,16.366,3.77" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M4.348 23.248L8.098 20.998 5.848 17.248" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M.75,15.808a6.749,6.749,0,0,0,6.884,4.417" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M0.750 0.748 L12.750 0.748 L12.750 12.748 L0.750 12.748 Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M15.75,11.248h6a1.5,1.5,0,0,1,1.5,1.5v9a1.5,1.5,0,0,1-1.5,1.5h-9a1.5,1.5,0,0,1-1.5-1.5v-6" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M15.75,20.248v-4.5a1.5,1.5,0,0,1,3,0v4.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M15.75 18.748L18.75 18.748" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M6.75 3.748L6.75 5.248" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M3.75 5.248L9.75 5.248" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M8.25,5.248s-1.5,4.5-4.5,4.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M6.749,8.014a3.933,3.933,0,0,0,3,1.734" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path>
					</g>
				</svg>
				<br>
				Trans-<br>lations
			</div>

			<div id="localizations" class="slideOut slideOutLeft">
				<div class="page inner">
					<div class="localizations-inputs flex">
						<select id="localization-preview-page" class="hover:bg-gray-100 focus:border-gray-666 active:border-green"></select>
						<input class="locale" placeholder="Locale">
						<button class="refresh action button flex items-center">${_Icons.getSvgIcon(_Icons.iconRefreshArrows, 16, 16, 'mr-2')} Refresh</button>
					</div>

					<div class="results"></div>
				</div>
			</div>

			<div id="center-pane"></div>

			<div class="slideout-activator right" id="widgetsTab">
				<svg viewBox="0 0 28 28" height="24" width="24" xmlns="http://www.w3.org/2000/svg">
					<g transform="matrix(1.1666666666666667,0,0,1.1666666666666667,0,0)">
						<path d="M16.5,23.248H21a.75.75,0,0,0,.75-.75V17.559a.75.75,0,0,0-.219-.53l-1.06-1.061a.749.749,0,0,0-.53-.22H16.5a.75.75,0,0,0-.75.75v6A.75.75,0,0,0,16.5,23.248Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M16.5,9.748H21A.75.75,0,0,0,21.75,9V4.059a.75.75,0,0,0-.219-.53l-1.06-1.061a.749.749,0,0,0-.53-.22H16.5a.75.75,0,0,0-.75.75V9A.75.75,0,0,0,16.5,9.748Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M2.25 0.748L2.25 2.248" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M2.25 5.248L2.25 8.248" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M2.25 11.248L2.25 14.248" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M2.25,17.248v1.5a1.5,1.5,0,0,0,1.5,1.5h1.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M8.25 20.248L11.25 20.248" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M14.25 20.248L15.75 20.248" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M2.25 6.748L5.25 6.748" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M8.25 6.748L11.25 6.748" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M14.25 6.748L15.75 6.748" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path>
					</g>
				</svg>
				<br>
				Widgets
			</div>

			<div id="widgetsSlideout" class="slideOut slideOutRight">
			</div>

			<div class="slideout-activator right" id="paletteTab">
				<svg height="24" width="24" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 28 28">
					<g transform="matrix(1.1666666666666667,0,0,1.1666666666666667,0,0)"><g>
						<rect x="0.75" y="0.75" width="22.5" height="22.5" rx="1.5" style="fill: none;stroke: currentColor;stroke-linecap: round;stroke-linejoin: round;stroke-width: 1.5px"></rect>
						<rect x="4.25" y="4.25" width="9.5" height="9.5" rx="0.75" style="fill: none;stroke: currentColor;stroke-linecap: round;stroke-linejoin: round;stroke-width: 1.5px"></rect>
						<rect x="13.25" y="16.25" width="6.5" height="3.5" rx="0.75" style="fill: none;stroke: currentColor;stroke-linecap: round;stroke-linejoin: round;stroke-width: 1.5px"></rect>
						<line x1="4.25" y1="19.75" x2="7.75" y2="19.75" style="fill: none;stroke: currentColor;stroke-linecap: round;stroke-linejoin: round;stroke-width: 1.5px"></line>
						<line x1="4.25" y1="16.75" x2="9.25" y2="16.75" style="fill: none;stroke: currentColor;stroke-linecap: round;stroke-linejoin: round;stroke-width: 1.5px"></line>
						<line x1="4.47" y1="4.47" x2="13.53" y2="13.53" style="fill: none;stroke: currentColor;stroke-linecap: round;stroke-linejoin: round;stroke-width: 1.5px"></line>
						<line x1="13.53" y1="4.47" x2="4.47" y2="13.53" style="fill: none;stroke: currentColor;stroke-linecap: round;stroke-linejoin: round;stroke-width: 1.5px"></line>
						<line x1="19.75" y1="13.25" x2="19.75" y2="4.5" style="fill: none;stroke: currentColor;stroke-linecap: round;stroke-linejoin: round;stroke-width: 1.5px"></line>
					  </g></g></svg>
				<br>Design Tools
			</div>

			<div id="palette" class="slideOut slideOutRight">
			</div>

			<div class="slideout-activator right" id="componentsTab">
				<svg viewBox="0 0 28 28" height="24" width="24" xmlns="http://www.w3.org/2000/svg">
					<g transform="matrix(1.1666666666666667,0,0,1.1666666666666667,0,0)">
						<path d="M6.750 3.001 A5.25 2.25 0 1 0 17.250 3.001 A5.25 2.25 0 1 0 6.750 3.001 Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M6.75,3V6c0,1.242,2.351,2.25,5.25,2.25S17.25,7.243,17.25,6V3" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M6.75,6V9c0,1.242,2.351,2.25,5.25,2.25S17.25,10.243,17.25,9V6" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M0.750 14.251 L9.750 14.251 L9.750 20.251 L0.750 20.251 Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M5.25 20.251L5.25 23.251" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M3 23.251L7.5 23.251" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M14.250 14.251 L23.250 14.251 L23.250 20.251 L14.250 20.251 Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M18.75 20.251L18.75 23.251" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M16.5 23.251L21 23.251" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M2.251,11.251v-1.5a1.5,1.5,0,0,1,1.5-1.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M21.751,11.251v-1.5a1.5,1.5,0,0,0-1.5-1.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path>
					</g>
				</svg>
				<br>
				Shared Comp.
			</div>

			<div id="components" class="slideOut slideOutRight">
			</div>

			<div class="slideout-activator right" id="elementsTab">
				<svg viewBox="0 0 28 28" height="24" width="24" xmlns="http://www.w3.org/2000/svg">
					<g transform="matrix(1.1666666666666667,0,0,1.1666666666666667,0,0)">
						<path d="M22.9,1.3A1.5,1.5,0,0,0,21.75.764H2.25A1.5,1.5,0,0,0,.772,2.521l3.387,19.5a1.5,1.5,0,0,0,1.478,1.243H18.363a1.5,1.5,0,0,0,1.478-1.243l3.387-19.5A1.5,1.5,0,0,0,22.9,1.3Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M10.125 13.514L6.375 13.514 6.375 17.264" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M17.792,14.247a5.572,5.572,0,0,1-10.74-.733" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M14.625 10.514L18.375 10.514 18.375 6.764" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M6.958,9.781a5.572,5.572,0,0,1,10.74.733" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path>
					</g>
				</svg>
				<br>
				Recycle Bin
			</div>

			<div id="elements" class="slideOut slideOutRight">
				<div id="elementsArea"></div>
			</div>

			<div class="slideout-activator right" id="previewTab">
				<svg viewBox="0 0 28 28" height="24" width="24" xmlns="http://www.w3.org/2000/svg" stroke-width="1.5px">
					<g transform="matrix(1.1666666666666667,0,0,1.1666666666666667,0,0)">
						<path stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round" d="M21.25 1.0061H2.75C1.64543 1.0061 0.75 1.90153 0.75 3.0061V16.9041C0.75 18.0087 1.64543 18.9041 2.75 18.9041H21.25C22.3546 18.9041 23.25 18.0087 23.25 16.9041V3.0061C23.25 1.90153 22.3546 1.0061 21.25 1.0061Z"></path>
						<path stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round" d="M9.95501 18.9031L8.93201 22.9941"></path>
						<path stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round" d="M8.93201 22.9939H14.557"></path>
						<path stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round" d="M14.046 18.9031L15.068 22.9941"></path>
						<path stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round" d="M16.602 22.9939H7.39801"></path>
						<path stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round" d="M0.75 15.312H23.25"></path>
						<path stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round" d="M3.935 5.15308H6.201"></path>
						<path stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round" d="M8.46701 5.15308H9.82701"></path>
						<path stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round" d="M3.935 8.20996H5.294"></path>
						<path stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round" d="M7.561 8.20996H9.827"></path>
						<path stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round" d="M3.935 11.2671H9.827"></path>
						<path stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round" d="M13.023 1.0061V15.3121"></path>
						<path stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round" d="M13.023 12.0169L16.075 9.04089C16.5729 8.64889 17.1845 8.42882 17.818 8.41368C18.4516 8.39854 19.0729 8.58913 19.589 8.95689L23.25 12.0169"></path>
						<path stroke="currentColor" d="M16.597 5.65308C16.3208 5.65308 16.097 5.42922 16.097 5.15308C16.097 4.87693 16.3208 4.65308 16.597 4.65308"></path>
						<path stroke="currentColor" d="M16.597 5.65308C16.8731 5.65308 17.097 5.42922 17.097 5.15308C17.097 4.87693 16.8731 4.65308 16.597 4.65308"></path>
					</g>
				</svg>
				<br>
				Preview
			</div>

			<div id="previewSlideout" class="slideOut slideOutRight">
			</div>
		`,
		functions: config => `
			<div class="flex-grow">
				<ul class="tabs-menu hidden">
					<li id="tabs-menu-basic">
						<a href="#pages:basic">Basic</a>
					</li>
					<li id="tabs-menu-html">
						<a href="#pages:html">HTML</a>
					</li>
					<li id="tabs-menu-advanced">
						<a href="#pages:advanced">Advanced</a>
					</li>
					<li id="tabs-menu-preview">
						<a href="#pages:preview">Preview</a>
					</li>
					<li id="tabs-menu-editor">
						<a href="#pages:editor">Editor</a>
					</li>
					<li id="tabs-menu-repeater">
						<a href="#pages:repeater">Repeater</a>
					</li>
					<li id="tabs-menu-events">
						<a href="#pages:events">Events</a>
					</li>
					<li id="tabs-menu-link">
						<a href="#pages:link">Link</a>
					</li>
					<li id="tabs-menu-security">
						<a href="#pages:security">Security</a>
					</li>
				</ul>
			</div>
		`,
		basic: config => `
			<div class="content-container basic-container"></div>
		`,
		contentEditor: config => `
			<div class="content-container content-editor-container flex flex-col">
				<div class="editor flex-grow"></div>
				<div class="editor-info-container"></div>
				<div class="editor-button-container"></div>
			</div>
		`,
		events: config => `
			<div class="content-container">

				<div class="inline-info">
					<div class="inline-info-icon">
						${_Icons.getSvgIcon(_Icons.iconInfo, 24, 24)}
					</div>
					<div class="inline-info-text">
						Here you can define actions to modify data objects in the backend like create, update or delete.<br><br>
						Actions can be triggered by specific events like clicking on an element, changing a value or select option, or when it's loosing the focus.
					</div>
				</div>

				<div class="events-container">

					<h3>Event Action Mapping</h3>

					<div class="grid grid-cols-2 gap-8">

						<div>
							<label class="block mb-2" data-comment="Select the event type that triggers the action.">Event</label>

							<select class="select2" id="event-select">
								<option value="none">None</option>
								<option value="click">Click</option>
								<option value="change">Change</option>
								<option value="focusout">Focus out</option>
								<option value="drop">Drop</option>
								<option value="load">Load</option>
								<option value="custom">Custom frontend event</option>
							</select>
						</div>

						<div class="hidden em-event-element em-event-any">
							<label class="block mb-2" data-comment="Select the action that is triggered by the event.">Action</label>

							<select class="select2" id="action-select">
								<option value="none">No action</option>
								<optgroup label="Data">
									<option value="create">Create new object</option>
									<option value="update">Update object</option>
									<option value="delete">Delete object</option>
								</optgroup>
								<optgroup label="Behaviour/Logic">
									<option value="method">Execute method</option>
									<option value="flow">Execute flow</option>
								</optgroup>
								<optgroup label="Pager">
									<option value="next-page">Next page</option>
									<option value="prev-page">Previous page</option>
								</optgroup>
								<optgroup label="Authentication">
									<option value="sign-in">Sign in</option>
									<option value="sign-out">Sign out</option>
									<option value="sign-up">Sign up</option>
									<option value="reset-password">Reset password</option>
								</optgroup>
							</select>
						</div>

						<div class="hidden em-event-element em-event-custom">
							<label class="block mb-2" for="custom-event-input" data-comment="Define the frontend event that triggers the action.">Frontend event</label>
							<input type="text" id="custom-event-input">
						</div>

						<div class="hidden em-event-element em-action-custom">
							<label class="block mb-2" for="custom-action-input" data-comment="Define the backend action that is triggered by the event.">Backend action</label>
							<input type="text" id="custom-action-input">
						</div>

						<div><!-- exists so it is always displayed -->

							<div class="hidden em-action-element em-action-update em-action-delete em-action-method em-action-custom">

								<div class="hidden em-action-element em-action-update">
									<label class="block mb-2" for="id-expression-input" data-comment="Enter a script expression like &quot;&#36;{obj.id}&quot; that evaluates to the UUID of the data object that shall be updated.">UUID of data object to update</label>
								</div>

								<div class="hidden em-action-element em-action-delete">
									<label class="block mb-2" for="id-expression-input" data-comment="Enter a script expression like &quot;&#36;{obj.id}&quot; that evaluates to the UUID of the data object that shall be deleted on click.">UUID of data object to delete</label>
								</div>

								<div class="hidden em-action-element em-action-method">
									<label class="block mb-2" for="id-expression-input" data-comment="Enter a script expression like &quot;&#36;{obj.id}&quot; that evaluates to the UUID of the data object the method shall be called on, or a type name for static methods, or leave empty for global methods.">UUID or type of data object to call method on</label>
								</div>

								<div class="hidden em-action-element em-action-custom">
									<label class="block mb-2" for="id-expression-input" data-comment="Enter a script expression like &quot;&#36;{obj.id}&quot; that evaluates to the UUID of the target data object.">UUID of action target object</label>
								</div>

								<input type="text" id="id-expression-input">
							</div>

						</div>

						<!--div class="hidden options-prev-page options-next-page				em-action-element em-action-next-page em-action-prev-page">
							<div>
								<label class="block mb-2" for="pagination-name-input" data-comment="Define the name of the pagination request parameter (usually &quot;page&quot;).">Pagination request parameter</label>
								<input type="text" id="pagination-name-input">
							</div>
						</div-->

						<div class="hidden em-action-element em-action-create em-action-update">
							<div class="relative">
								<label class="block mb-2" for="data-type-select" data-comment="Define the type of data object to create or update">Enter or select type of data object</label>
								<input type="text" class="combined-input-select-field" id="data-type-input" placeholder="Custom type or script expression">
								<select class="required combined-input-select-field" id="data-type-select">
									<option value="">Select type from schema</option>
								</select>
								<ul class="combined-input-select-field hidden"></ul>
							</div>
						</div>

						<div class="hidden options-method em-action-element em-action-method">
							<div>
								<label class="block mb-2" for="method-name-input">Name of method to execute</label>
								<input type="text" id="method-name-input">
							</div>
						</div>

						<div class="col-span-2 hidden em-event-element em-event-drop">
							<h3>Drag & Drop</h3>
							<div>
								<label class="block mb-2">The following additional configuration is required to enable drag & drop.</label>
								<ul class="mb-2">
									<li>Make other elements draggable: set the <code>draggable</code> attribute to <code>true</code>.</li>
									<li>Add a custom data attribute with the value <code>data()</code> to the drop target, e.g. <code>data-payload</code> etc.</li>
									<li>The custom data attribute will be sent to the method when a drop event occurs.</li>
								<ul>
							</div>
						</div>

						<div class="col-span-2 hidden em-action-element em-action-any">
							<h3>Parameter Mapping
								<i class="m-2 em-add-parameter-mapping-button cursor-pointer align-middle icon-grey icon-inactive hover:icon-active">${_Icons.getSvgIcon(_Icons.iconAdd,16,16,[], 'Add parameter')}</i>
								<i class="m-2 em-add-parameter-mapping-for-type-button cursor-pointer align-middle icon-grey icon-inactive hover:icon-active">${_Icons.getSvgIcon(_Icons.iconListAdd,16,16,[], 'Add parameters for all properties')}</i>
							</h3>

							<div class="em-parameter-mappings-container"></div>

						</div>

						<div class="col-span-2 hidden em-action-element em-action-any">
							<h3>Notifications</h3>
							<div class="grid grid-cols-2 gap-8">

								<div>
									<label class="block mb-2" for="success-notifications-select" data-comment="Define what kind of notifications should be displayed on success">Success notifications</label>
									<select class="select2" id="success-notifications-select">
										<option value="none">None</option>
										<option value="system-alert">System alert</option>
										<option value="inline-text-message">Inline text message</option>
										<option value="custom-dialog">Custom dialog element(s) defined by CSS ID(s)</option>
										<option value="custom-dialog-linked">Custom dialog element(s) defined by linked element(s)</option>
										<option value="fire-event">Raise a custom event</option>
									</select>
								</div>

								<div class="hidden option-success-notifications option-success-notifications-custom-dialog">
									<label class="block mb-2" for="success-notifications-custom-dialog-input" data-comment="Define the area(s) of the current page that should be displayed as notification dialog(s) with their CSS ID selector (comma-separated list of CSS IDs with leading #).">Partial(s) to refresh on success</label>
									<input type="text" id="success-notifications-custom-dialog-input" placeholder="Enter CSS ID(s)">
								</div>

								<div class="hidden option-success-notifications option-success-notifications-custom-dialog-linked">
									<label class="block mb-2" for="success-notifications-custom-dialog-linked-input" data-comment="Drag an element and drop it here">Element(s) to be displayed as success notification dialogs</label>
									<input type="hidden" id="success-notifications-custom-dialog-linked-input" value="">
									<div class="element-dropzone success-notifications-custom-dialog-linked-dropzone">
										<div class="info-icon h-16 flex items-center justify-center">
											<i class="m-2 active align-middle">${_Icons.getSvgIcon(_Icons.iconAdd)}</i>
											<i class="m-2 inactive align-middle">${_Icons.getSvgIcon(_Icons.iconAdd)}</i> Drag and drop existing element here 
										</div>
									</div>
								</div>

								<div class="hidden option-success-notifications option-success-notifications-fire-event">
									<label class="block mb-2" for="success-notifications-fire-event-input" data-comment="Define event that should be raised.">Event to raise to display success notifications</label>
									<input type="text" id="success-notifications-fire-event-input" placeholder="Enter an event name">
								</div>
							</div>

							<div class="grid grid-cols-2 gap-8 mt-4">
							
								<div>
									<label class="block mb-2" for="failure-notifications-select" data-comment="Define what kind of notifications should be displayed on failure">Failure notifications</label>
									<select class="select2" id="failure-notifications-select">
										<option value="none">None</option>
										<option value="system-alert">System alert</option>
										<option value="inline-text-message">Inline text message</option>
										<option value="custom-dialog">Custom dialog element(s) defined by CSS ID(s)</option>
										<option value="custom-dialog-linked">Custom dialog element(s) defined by linked element(s)</option>
										<option value="fire-event">Raise a custom event</option>
									</select>
								</div>

								<div class="hidden option-failure-notifications option-failure-notifications-custom-dialog">
									<label class="block mb-2" for="failure-notifications-custom-dialog-input" data-comment="Define the area(s) of the current page that should be displayed as notification dialog(s) with their CSS ID selector (comma-separated list of CSS IDs with leading #).">Partial(s) to refresh on failure</label>
									<input type="text" id="failure-notifications-custom-dialog-input" placeholder="Enter CSS ID(s)">
								</div>

								<div class="hidden option-failure-notifications option-failure-notifications-custom-dialog-linked">
									<label class="block mb-2" for="failure-notifications-custom-dialog-linked-input" data-comment="Drag an element and drop it here">Element(s) to be displayed as failure notification dialogs</label>
									<input type="hidden" id="failure-notifications-custom-dialog-linked-input" value="">
									<div class="element-dropzone failure-notifications-custom-dialog-linked-dropzone">
										<div class="info-icon h-16 flex items-center justify-center">
											<i class="m-2 active align-middle">${_Icons.getSvgIcon(_Icons.iconAdd)}</i>
											<i class="m-2 inactive align-middle">${_Icons.getSvgIcon(_Icons.iconAdd)}</i> Drag and drop existing element here 
										</div>
									</div>
								</div>

								<div class="hidden option-failure-notifications option-failure-notifications-fire-event">
									<label class="block mb-2" for="failure-notifications-fire-event-input" data-comment="Define event that should be raised.">Event to raise to display failure notifications</label>
									<input type="text" id="failure-notifications-fire-event-input" placeholder="Enter an event name">
								</div>
							</div>
						</div>
						
						<div class="col-span-2 hidden em-action-element em-action-any">
							<h3>Follow-up Actions</h3>
							<div class="grid grid-cols-2 gap-8">

								<div>
									<label class="block mb-2" for="success-behaviour-select" data-comment="Define what should happen after the triggered action succeeded.">Behaviour on success</label>
									<select class="select2" id="success-behaviour-select">
										<option value="none">None</option>
										<option value="full-page-reload">Reload the current page</option>
										<option value="partial-refresh">Refresh page section(s) defined by CSS ID(s)</option>
										<option value="partial-refresh-linked">Refresh page section defined by linked element(s)</option>
										<option value="navigate-to-url">Navigate to a new page</option>
										<option value="fire-event">Raise a custom event</option>
										<option value="sign-out">Sign out</option>
									</select>
								</div>

								<div class="hidden option-success option-success-partial-refresh">
									<label class="block mb-2" for="success-partial-refresh-input" data-comment="Define the area(s) of the current page that should be refreshed by its CSS ID selector (comma-separated list of CSS IDs with leading #).">Partial(s) to refresh on success</label>
									<input type="text" id="success-partial-refresh-input" placeholder="Enter CSS ID(s)">
								</div>

								<div class="hidden option-success option-success-partial-refresh-linked">
									<label class="block mb-2" for="success-partial-refresh-linked-input" data-comment="Drag an element and drop it here">Element(s) to be refreshed on success</label>
									<input type="hidden" id="success-partial-refresh-linked-input" value="">
									<div class="element-dropzone success-partial-refresh-linked-dropzone">
										<div class="info-icon h-16 flex items-center justify-center">
											<i class="m-2 active align-middle">${_Icons.getSvgIcon(_Icons.iconAdd)}</i>
											<i class="m-2 inactive align-middle">${_Icons.getSvgIcon(_Icons.iconAdd)}</i> Drag and drop existing element here 
										</div>
									</div>
								</div>

								<div class="hidden option-success option-success-navigate-to-url">
									<label class="block mb-2" for="success-navigate-to-url-input" data-comment="Define the relative or absolute URL of the page to load on success">Success URL</label>
									<input type="text" id="success-navigate-to-url-input" placeholder="Enter a relative or absolute URL">
								</div>

								<div class="hidden option-success option-success-fire-event">
									<label class="block mb-2" for="success-fire-event-input" data-comment="Define event that should be raised.">Event to raise on success</label>
									<input type="text" id="success-fire-event-input" placeholder="Enter an event name">
								</div>
							</div>

							<div class="grid grid-cols-2 gap-8 mt-4">
								<div>
									<label class="block mb-2" for="failure-behaviour-select" data-comment="Define what should happen after the triggered action failed.">Behaviour on failure</label>
									<select class="select2" id="failure-behaviour-select">
										<option value="none">None</option>
										<option value="full-page-reload">Reload the current page</option>
										<option value="partial-refresh">Refresh section(s) by ID</option>
										<option value="partial-refresh-linked">Refresh page section defined by linked element(s)</option>
										<option value="navigate-to-url">Navigate to a new page</option>
										<option value="fire-event">Raise a custom event</option>
										<option value="sign-out">Sign out</option>
									</select>
								</div>

								<div class="hidden option-failure option-failure-partial-refresh">
									<label class="block mb-2" for="failure-partial-refresh-input" data-comment="Define the area of the current page that should be refreshed by its CSS ID.">Partial to refresh on failure</label>
									<input type="text" id="failure-partial-refresh-input" placeholder="Enter CSS ID(s)">
								</div>

								<div class="hidden option-failure option-failure-partial-refresh-linked">
									<label class="block mb-2" for="failure-partial-refresh-linked-input" data-comment="Drag an element and drop it here">Element(s) to be refreshed on failure</label>
									<input type="hidden" id="failure-partial-refresh-linked-input" value="">
									<div class="element-dropzone failure-partial-refresh-linked-dropzone">
										<div class="info-icon h-16 flex items-center justify-center">
											<i class="m-2 active align-middle">${_Icons.getSvgIcon(_Icons.iconAdd)}</i>
											<i class="m-2 inactive align-middle">${_Icons.getSvgIcon(_Icons.iconAdd)}</i> Drag and drop existing element here 
										</div>
									</div>
								</div>

								<div class="hidden option-failure option-failure-navigate-to-url">
									<label class="block mb-2" for="failure-navigate-to-url-input" data-comment="Define the relative or absolute URL of the page to load on failure">Failure URL</label>
									<input type="text" id="failure-navigate-to-url-input" placeholder="Enter a relative or absolute URL">
								</div>

								<div class="hidden option-failure option-failure-fire-event">
									<label class="block mb-2" for="failure-fire-event-input" data-comment="Define event that should be raised.">Event to raise on failure</label>
									<input type="text" id="failure-fire-event-input" placeholder="Enter an event name">
								</div>
							</div>
						</div>

						<!--div class="col-span-2">
							<button type="button" class="action" id="save-event-mapping-button">Save</button>
						</div-->

					</div>

				</div>
			</div>
		`,
		parameterMappingRow: config => `
			<div class="em-parameter-mapping" data-structr-id="${config.id}">

				<div class="grid grid-cols-4 gap-8 hidden options-reload-target mb-4">

					<div>
						<label class="block mb-2" data-comment="Choose a name/key for this parameter to define how the value is sent to the backend">Parameter name</label>
						<input type="text" class="parameter-name-input" placeholder="Name" value="${config.parameterName || ''}">
					</div>

					<div>
						<label class="block mb-2" for="parameter-type-select" data-comment="Select the type of this parameter.">Parameter type</label>
						<select class="parameter-type-select">
							<option>-- Select --</option>
							<option value="user-input">User Input</option>
							<option value="page-param">Request parameter for page</option>
							<option value="pagesize-param">Request parameter for page size</option>
							<option value="constant-value">Constant value</option>
							<option value="script-expression">Script expression</option>
							<option value="method-result">Result of method call</option>
							<option value="flow-result">Result of flow</option>
						</select>
					</div>

					<div class="hidden em-parameter-value parameter-constant-value">
						<label class="block mb-2" data-comment="Enter a constant value">Value (constant)</label>
						<input type="text" class="parameter-constant-value-input" placeholder="Constant value" value="${config.value || ''}">
					</div>

					<div class="hidden em-parameter-value parameter-script-expression">
						<label class="block mb-2" data-comment="The script expression will be evaluated and the result passed as parameter value">Value expression</label>
						<input type="text" class="parameter-script-expression-input" placeholder="Script expression" value="${config.value || ''}">
					</div>

					<div class="hidden em-parameter-value parameter-user-input">
						<label class="block mb-2" data-comment="Drag a form input element (&amp;lt;input&amp;gt;, &amp;lt;textarea&amp;gt; or &amp;lt;select&amp;gt;) and drop it here">Form input element</label>
						<input type="hidden" class="parameter-user-input-input" value="${config.value || ''}">
						<div class="element-dropzone link-existing-element-dropzone">
							<div class="info-icon h-16 flex items-center justify-center">
								<i class="m-2 active align-middle">${_Icons.getSvgIcon(_Icons.iconAdd)}</i>
								<i class="m-2 inactive align-middle">${_Icons.getSvgIcon(_Icons.iconAdd)}</i> Drag and drop existing form input element here 
							</div>
						</div>
					</div>

					<div class="hidden em-parameter-value parameter-method-result">
						<label class="block mb-2" data-comment="The method will be evaluated and the result passed as parameter value">Method</label>
						<input type="text" class="parameter-method-result-input" placeholder="Method name" value="${config.value || ''}">
					</div>

					<div class="hidden em-parameter-value parameter-flow-result">
						<label class="block mb-2" data-comment="The selected Flow will be evaluated and the result passed as parameter value">Flow result</label>
						<select class="parameter-flow-result-input">
							<option value="">-- Select flow --</option>
						</select>
					</div>

					<div>
						<label class="hidden block mb-2">Actions</label>
						<i class="block mt-4 cursor-pointer em-parameter-mapping-remove-button" data-structr-id="${config.id}">${_Icons.getSvgIcon(_Icons.iconTrashcan)}</i>
					</div>

				</div>

				<!--div class="hidden em-action-element em-action-create em-action-update">
					<div id="link-existing-element-dropzone" class="element-dropzone">
						<div class="info-icon h-16 flex items-center justify-center">
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['m-2', 'active'])}
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['m-2', 'active'])} Drop existing input or select elements here
						</div>
					</div>
				</div>

				<div class="options-properties">
					<button class="inline-flex items-center add-property-input-button hover:bg-gray-100 focus:border-gray-666 active:border-green">${_Icons.getSvgIcon(_Icons.iconAdd)} Create new input</button>
					<button class="inline-flex items-center add-property-select-button hover:bg-gray-100 focus:border-gray-666 active:border-green">${_Icons.getSvgIcon(_Icons.iconAdd)} Create new select</button>
				</div-->

			</div>
		`,
		preview: config => `
			<div class="content-container preview-container">
				<div class="previewBox" data-id="${config.pageId}">
					<iframe></iframe>
				</div>
			</div>
		`,
		properties: config => `
			<div class="content-container properties-container"></div>
		`,
		repeater: config => `
			<div class="content-container">

				<div class="inline-info">
					<div class="inline-info-icon">
						${_Icons.getSvgIcon(_Icons.iconInfo, 24, 24)}
					</div>
					<div class="inline-info-text">
						A repeater is a node in the tree that is repeatedly displayed for each element of the result set.<br><br>
						The result set is a list or collection of objects that can be generated via a database query, a flow or a function.<br><br>
						The respective result element can be accessed via a keyword configured further below.
					</div>
				</div>

				<div class="flex flex-col h-full repeater-container">
					<h3>Result Collection</h3>

					<div class="query-type-buttons"></div>

					<select class="hidden" id="flow-selector"></select>
					<div class="hidden flex-grow query-text"></div>
					<div>
						<button class="save-repeater-query hover:bg-gray-100 focus:border-gray-666 active:border-green">Save</button>
					</div>

					<div class="my-8">
						<h3>Repeater Keyword</h3>
						<p>
							The repeater keyword or data key is either a word to reference result objects, or it can be the
							name of a collection property of the result object.<br><br>
							You can access result objects or the objects of the collection using template expressions,
							e.g. <i>\${project.name}</i>.
						</p>
						<div>
							<input class="repeater-datakey" type="text" value="">
							<button class="save-repeater-datakey hover:bg-gray-100 focus:border-gray-666 active:border-green">Save</button>
						</div>
					</div>
				</div>
			</div>
		`,
		security: config => `
			<div class="content-container security-container">
				<div class="inline-info">
					<div class="inline-info-icon">
						${_Icons.getSvgIcon(_Icons.iconInfo, 24, 24)}
					</div>
					<div class="inline-info-text">
						The Access Control and Visibility dialog provides access to the security settings of a node. In this dialog, you can set, edit or remove the owner of the node, set visibility flags and configure security relationships.
					</div>
				</div>
			</div>
		`,
		linkable: config => `
			<div class="content-container linkable-container"></div>
		`,
	}
};

function findDiff(str1, str2){
	let diff= "";
	str2.split('').forEach(function(val, i){
		if (val != str1.charAt(i))
			diff += val ;
	});
	return diff;
}

// polyfill for RegExp.escape
if (!RegExp.escape) {
	RegExp.escape = function(s) {
		return String(s).replace(/[\\^$*+?.()|[\]{}]/g, '\\$&');
	};
}

var TopLevelObject = {}
TopLevelObject.DOMNodePathStep = function(value, optimized)
{
	this.value = value;
	this.optimized = optimized || false;
}
TopLevelObject.DOMNodePathStep.prototype = {
	/**
	 * @override
	 * @return {string}
	 */
	toString: function()
	{
		return this.value;
	}
}
TopLevelObject.DOMPresentationUtils = {}

TopLevelObject.DOMPresentationUtils.cssPath = function(node, optimized)
{
	if (node.nodeType !== Node.ELEMENT_NODE)
		return "";
	var steps = [];
	var contextNode = node;
	while (contextNode) {
		var step = TopLevelObject.DOMPresentationUtils._cssPathStep(contextNode, !!optimized, contextNode === node);
		if (!step)
			break; // Error - bail out early.
		steps.push(step);
		if (step.optimized)
			break;
		contextNode = contextNode.parentNode;
	}
	steps.reverse();
	return steps.join(" > ");
}

TopLevelObject.DOMPresentationUtils._cssPathStep = function(node, optimized, isTargetNode)
{
	if (node.nodeType !== Node.ELEMENT_NODE)
		return null;
	var id = node.getAttribute("id");
	if (optimized) {
		if (id)
			return new TopLevelObject.DOMNodePathStep(idSelector(id), true);
		var nodeNameLower = node.nodeName.toLowerCase();
		if (nodeNameLower === "body" || nodeNameLower === "head" || nodeNameLower === "html")
			return new TopLevelObject.DOMNodePathStep(node.tagName.toLowerCase(), true);
	}
	var nodeName = node.tagName.toLowerCase();
	if (id)
		return new TopLevelObject.DOMNodePathStep(nodeName + idSelector(id), true);
	var parent = node.parentNode;
	if (!parent || parent.nodeType === Node.DOCUMENT_NODE)
		return new TopLevelObject.DOMNodePathStep(nodeName, true);
	/**
	 * @param {!TopLevelObject.DOMNode} node
	 * @return {!Array.<string>}
	 */
	function prefixedElementClassNames(node)
	{
		var classAttribute = node.getAttribute("class");
		if (!classAttribute)
			return [];
		return classAttribute.split(/\s+/g).filter(Boolean).map(function(name) {
			// The prefix is required to store "__proto__" in a object-based map.
			return "$" + name;
		});
	}
	/**
	 * @param {string} id
	 * @return {string}
	 */
	function idSelector(id)
	{
		return "#" + escapeIdentifierIfNeeded(id);
	}
	/**
	 * @param {string} ident
	 * @return {string}
	 */
	function escapeIdentifierIfNeeded(ident)
	{
		if (isCSSIdentifier(ident))
			return ident;
		var shouldEscapeFirst = /^(?:[0-9]|-[0-9-]?)/.test(ident);
		var lastIndex = ident.length - 1;
		return ident.replace(/./g, function(c, i) {
			return ((shouldEscapeFirst && i === 0) || !isCSSIdentChar(c)) ? escapeAsciiChar(c, i === lastIndex) : c;
		});
	}
	/**
	 * @param {string} c
	 * @param {boolean} isLast
	 * @return {string}
	 */
	function escapeAsciiChar(c, isLast)
	{
		return "\\" + toHexByte(c) + (isLast ? "" : " ");
	}
	/**
	 * @param {string} c
	 */
	function toHexByte(c)
	{
		var hexByte = c.charCodeAt(0).toString(16);
		if (hexByte.length === 1)
			hexByte = "0" + hexByte;
		return hexByte;
	}
	/**
	 * @param {string} c
	 * @return {boolean}
	 */
	function isCSSIdentChar(c)
	{
		if (/[a-zA-Z0-9_-]/.test(c))
			return true;
		return c.charCodeAt(0) >= 0xA0;
	}
	/**
	 * @param {string} value
	 * @return {boolean}
	 */
	function isCSSIdentifier(value)
	{
		return /^-?[a-zA-Z_][a-zA-Z0-9_-]*$/.test(value);
	}
	var prefixedOwnClassNamesArray = prefixedElementClassNames(node);
	var needsClassNames = false;
	var needsNthChild = false;
	var ownIndex = -1;
	var elementIndex = -1;
	var siblings = parent.children;
	for (var i = 0; (ownIndex === -1 || !needsNthChild) && i < siblings.length; ++i) {
	var sibling = siblings[i];
	if (sibling.nodeType !== Node.ELEMENT_NODE)
		continue;
	elementIndex += 1;
	if (sibling === node) {
		ownIndex = elementIndex;
		continue;
	}
	if (needsNthChild)
		continue;
	if (sibling.tagName.toLowerCase() !== nodeName)
		continue;
	needsClassNames = true;
	var ownClassNames = prefixedOwnClassNamesArray.values();
	var ownClassNameCount = 0;
	for (var name in ownClassNames)
		++ownClassNameCount;
	if (ownClassNameCount === 0) {
		needsNthChild = true;
		continue;
	}
	var siblingClassNamesArray = prefixedElementClassNames(sibling);
	for (var j = 0; j < siblingClassNamesArray.length; ++j) {
		var siblingClass = siblingClassNamesArray[j];
		if (!ownClassNames.hasOwnProperty(siblingClass))
			continue;
		delete ownClassNames[siblingClass];
		if (!-ownClassNameCount) {
			needsNthChild = true;
			break;
		}
	}
}
	var result = nodeName;
	if (isTargetNode && nodeName.toLowerCase() === "input" && node.getAttribute("type") && !node.getAttribute("id") && !node.getAttribute("class"))
		result += "[type=\"" + node.getAttribute("type") + "\"]";
	if (needsNthChild) {
		result += ":nth-child(" + (ownIndex + 1) + ")";
	} else if (needsClassNames) {
		for (var prefixedName in prefixedOwnClassNamesArray.values())
			result += "." + escapeIdentifierIfNeeded(prefixedName.substr(1));
	}
	return new TopLevelObject.DOMNodePathStep(result, false);
}
