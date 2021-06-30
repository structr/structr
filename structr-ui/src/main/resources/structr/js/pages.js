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
var pages, shadowPage;
var controls, activeTab, activeTabLeft, activeTabRight, paletteSlideout, elementsSlideout, componentsSlideout, widgetsSlideout, pagesSlideout, activeElementsSlideout, dataBindingSlideout;
var components, elements;
var selStart, selEnd;
var sel;
var contentSourceId, elementSourceId, rootId;
var textBeforeEditing;

$(document).ready(function() {
	Structr.registerModule(_Pages);
	Structr.classes.push('page');
});

var _Pages = {
	_moduleName: 'pages',
	autoRefresh: [],
	activeTabKey: 'structrActiveTab_' + port,
	urlHashKey: 'structrUrlHashKey_' + port,
	leftSlideoutWidthKey: 'structrLeftSlideoutWidthKey_' + port,
	rightSlideoutWidthKey: 'structrRightSlideoutWidthKey_' + port,
	activeTabRightKey: 'structrActiveTabRight_' + port,
	activeTabLeftKey: 'structrActiveTabLeft_' + port,
	selectedTypeKey: 'structrSelectedType_' + port,
	autoRefreshDisabledKey: 'structrAutoRefreshDisabled_' + port,
	detailsObjectIdKey: 'structrDetailsObjectId_' + port,
	requestParametersKey: 'structrRequestParameters_' + port,
	pagesResizerLeftKey: 'structrPagesResizerLeftKey_' + port,
	pagesResizerRightKey: 'structrPagesResizerRightKey_' + port,
	functionBarSwitchKey: 'structrFunctionBarSwitchKey_' + port,

	centerPane: undefined,

	init: function() {

		_Pager.initPager('pages',   'Page', 1, 25, 'name', 'asc');
		_Pager.forceAddFilters('pages', 'Page', { hidden: false });
		_Pager.initPager('files',   'File', 1, 25, 'name', 'asc');
		_Pager.initPager('folders', 'Folder', 1, 25, 'name', 'asc');
		_Pager.initPager('images',  'Image', 1, 25, 'name', 'asc');

		Structr.getShadowPage();

	},
	resize: function(left, right) {

		Structr.resize();

		$('body').css({
			position: 'fixed'
		});

		_Pages.resizeColumns();
	},
	onload: function() {

		let urlHash = LSWrapper.getItem(_Pages.urlHashKey);
		if (urlHash) {
			menuBlocked = false;
			window.location.hash = urlHash;
		}

		Structr.fetchHtmlTemplate('pages/pages', {}, function(html) {

			fastRemoveAllChildren(main[0]);

			main[0].insertAdjacentHTML('afterbegin', html);

			_Pages.init();

			Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('pages'));

			activeTab = LSWrapper.getItem(_Pages.activeTabKey);
			activeTabLeft = LSWrapper.getItem(_Pages.activeTabLeftKey);
			activeTabRight = LSWrapper.getItem(_Pages.activeTabRightKey);

			pagesSlideout = $('#pages');
//			activeElementsSlideout = $('#activeElements');
//			dataBindingSlideout = $('#dataBinding');
			localizationsSlideout = $('#localizations');

			_Pages.centerPane = document.querySelector('#center-pane');
			_Pages.previews.previewElement = document.querySelector('#previews');


			widgetsSlideout = $('#widgetsSlideout');
			paletteSlideout = $('#palette');
			componentsSlideout = $('#components');
			elementsSlideout = $('#elements');
			elementsSlideout.data('closeCallback', _Elements.clearUnattachedNodes);

			var pagesTabSlideoutAction = function () {
				//console.log('pagesTabSlideoutAction');
				_Pages.leftSlideoutTrigger(this, pagesSlideout, [/*activeElementsSlideout, dataBindingSlideout*/, localizationsSlideout], _Pages.activeTabLeftKey, function (params) {
					_Pages.resize();
					_Pages.showPagesPager();
					_Pages.showTabsMenu();
				}, _Pages.slideoutClosedCallback);
			};
			$('#pagesTab').on('click', pagesTabSlideoutAction).droppable({
				tolerance: 'touch',
				//over: pagesTabSlideoutAction
			});

//			$('#activeElementsTab').on('click', function () {
//				_Pages.leftSlideoutTrigger(this, activeElementsSlideout, [pagesSlideout, dataBindingSlideout, localizationsSlideout], _Pages.activeTabLeftKey, function (params) {
//					if (params.isOpenAction) {
//						_Pages.activeelements.refreshActiveElements();
//					}
//					_Pages.resize();
//				}, _Pages.slideoutClosedCallback);
//			});
//
//			$('#dataBindingTab').on('click', function () {
//				_Pages.leftSlideoutTrigger(this, dataBindingSlideout, [pagesSlideout, activeElementsSlideout, localizationsSlideout], _Pages.activeTabLeftKey, function (params) {
//					if (params.isOpenAction) {
//						_Pages.databinding.reloadDataBindingWizard();
//					}
//					_Pages.resize();
//				}, _Pages.slideoutClosedCallback);
//			});

			$('#localizationsTab').on('click', function () {
				_Pages.leftSlideoutTrigger(this, localizationsSlideout, [pagesSlideout, /*activeElementsSlideout, dataBindingSlideout*/], _Pages.activeTabLeftKey, function (params) {
					_Pages.resize();
				}, _Pages.slideoutClosedCallback);
			});

			$('#localizations input.locale').on('keydown', function (e) {
				if (e.which === 13) {
					_Pages.refreshLocalizations();
				}
			});
			$('#localizations button.refresh').on('click', function () {
				_Pages.refreshLocalizations();
			});

			Structr.appendInfoTextToElement({
				element: $('#localizations button.refresh'),
				text: "On this tab you can load the localizations requested for the given locale on the currently previewed page (including the UUID of the details object and the query parameters which are also used for the preview).<br><br>The retrieval process works just as rendering the page. If you request the locale \"en_US\" you might get Localizations for \"en\" as a fallback if no exact match is found.<br><br>If no Localization could be found, an empty input field is rendered where you can quickly create the missing Localization.",
				insertAfter: true,
				css: {
					right: "2px",
					top: "2px"
				},
				helpElementCss: {
					width: "200px"
				},
				offsetX: -50
			});

			$('#widgetsTab').on('click', function () {
				_Pages.rightSlideoutClickTrigger(this, widgetsSlideout, [paletteSlideout, componentsSlideout, elementsSlideout], _Pages.activeTabRightKey, function (params) {
					if (params.isOpenAction) {
						_Widgets.reloadWidgets();
					}
					_Pages.resize();
				}, _Pages.slideoutClosedCallback);
			});

			$('#paletteTab').on('click', function () {
				_Pages.rightSlideoutClickTrigger(this, paletteSlideout, [widgetsSlideout, componentsSlideout, elementsSlideout], _Pages.activeTabRightKey, function (params) {
					if (params.isOpenAction) {
						_Elements.reloadPalette();
					}
					_Pages.resize();
				}, _Pages.slideoutClosedCallback);
			});

			var componentsTabSlideoutAction = function () {
				_Pages.rightSlideoutClickTrigger(this, componentsSlideout, [widgetsSlideout, paletteSlideout, elementsSlideout], _Pages.activeTabRightKey, function (params) {
					if (params.isOpenAction) {
						_Elements.reloadComponents();
					}
					_Pages.resize();
				}, _Pages.slideoutClosedCallback);
			};
			$('#componentsTab').on('click', componentsTabSlideoutAction).droppable({
				tolerance: 'touch',
				over: function () {
					if (!componentsSlideout.hasClass('open')) {
						componentsTabSlideoutAction();
					}
				}
			});

			$('#elementsTab').on('click', function () {
				_Pages.rightSlideoutClickTrigger(this, elementsSlideout, [widgetsSlideout, paletteSlideout, componentsSlideout], _Pages.activeTabRightKey, function (params) {
					if (params.isOpenAction) {
						_Elements.reloadUnattachedNodes();
					}
					_Pages.resize();
				}, _Pages.slideoutClosedCallback);
			});

			live('#function-bar-switch', 'click', (e) => {
				let icon = e.target.closest('.icon');
				let pagesPager = document.getElementById('pagesPager');
				let subMenu = document.querySelector('#function-bar .tabs-menu');

				if (pagesPager.classList.contains('hidden')) {
					if (subMenu) subMenu.style.display = 'none';
					pagesPager.classList.remove('hidden');
					icon.innerHTML = '<svg viewBox="0 0 24 24" height="24" width="24" xmlns="http://www.w3.org/2000/svg"><g transform="matrix(1,0,0,1,0,0)"><path d="M.748,12.25a6,6,0,0,0,6,6h10.5a6,6,0,0,0,0-12H6.748A6,6,0,0,0,.748,12.25Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M17.248 9.25L17.248 15.25" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M14.248 9.25L14.248 15.25" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path></g></svg>';
					LSWrapper.setItem(_Pages.functionBarSwitchKey, 'visible');
				} else {
					pagesPager.classList.add('hidden');
					icon.innerHTML = '<svg viewBox="0 0 24 24" height="24" width="24" xmlns="http://www.w3.org/2000/svg"><g transform="matrix(1,0,0,1,0,0)"><path d="M23.248,12a6,6,0,0,1-6,6H6.748a6,6,0,0,1,0-12h10.5A6,6,0,0,1,23.248,12Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M6.748 9L6.748 15" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M9.748 9L9.748 15" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path></g></svg>';
					LSWrapper.setItem(_Pages.functionBarSwitchKey, 'hidden');
					if (subMenu) subMenu.style.display = 'inline-block';
				}
			});

			_Pages.refresh();

			if (activeTabLeft) {
				$('#' + activeTabLeft).addClass('active').click();
			}

			if (activeTabRight) {
				$('#' + activeTabRight).addClass('active').click();
			}

			// activate first page if local storage is empty
			if (!LSWrapper.getItem(_Pages.activeTabKey)) {
				window.setTimeout(function (e) {
					$('#pagesTree .node.page').first().click();
				}, 1000);
			}

			_Pages.resize();

			$(window).off('resize').resize(function () {
				_Pages.resize();
			});

			Structr.initVerticalSlider($('.column-resizer-left', main), _Pages.pagesResizerLeftKey, 300, _Pages.moveLeftResizer);
			Structr.initVerticalSlider($('.column-resizer-right', main), _Pages.pagesResizerRightKey, 400, _Pages.moveRightResizer, true);

			Structr.unblockMenu(500);

			_Pages.resizeColumns(LSWrapper.getItem(_Pages.pagesResizerLeftKey) || 200, LSWrapper.getItem(_Pages.pagesResizerRightKey) || 200);
		});
	},
	moveLeftResizer: function(left) {
		// throttle
		requestAnimationFrame(() => {
			//left = left || LSWrapper.getItem(_Pages.pagesResizerLeftKey) || 300;
			_Pages.resizeColumns(left, null);
		});
	},
	moveRightResizer: function(right) {
		// throttle
		requestAnimationFrame(() => {
			//left = left || LSWrapper.getItem(_Pages.pagesResizerLeftKey) || 300;
			_Pages.resizeColumns(null, right);
		});
	},
	resizeColumns: function(left, right) {

		let openLeftSlideout = document.querySelector('.slideOutLeft.open');
		let openRightSlideout = document.querySelector('.slideOutRight.open');

		let leftPos  = left  ? left  : (openLeftSlideout  ? openLeftSlideout.getBoundingClientRect().right  : 0);
		let rightPos = right ? right : (openRightSlideout ? openRightSlideout.getBoundingClientRect().width : 0);

		let tabsMenu = document.querySelector('#function-bar .tabs-menu');

		if (left) {
			let columnResizerLeft = 'calc(' + leftPos + 'px + 0rem)';
			document.querySelector('.column-resizer-left').style.left = columnResizerLeft;
			if (openLeftSlideout) openLeftSlideout.style.width = 'calc(' + leftPos + 'px - 3rem)';
			_Pages.centerPane.style.marginLeft = 'calc(' + leftPos + 'px + 3rem)';
			if (tabsMenu) tabsMenu.style.marginLeft = 'calc(' + leftPos + 'px + 2rem)';
		} else {
			if (leftPos === 0) {
				if (tabsMenu) tabsMenu.style.marginLeft = 'calc(' + leftPos + 'px + 2rem)';
				let columnResizerLeft = '4rem';
				document.querySelector('.column-resizer-left').style.left = columnResizerLeft;
				_Pages.centerPane.style.marginLeft = columnResizerLeft;
			} else {
				if (tabsMenu) tabsMenu.style.marginLeft = 'calc(' + leftPos + 'px + 0rem)';
				document.querySelector('.column-resizer-left').style.left = 'calc(' + leftPos + 'px - 1rem)';
				_Pages.centerPane.style.marginLeft = 'calc(' + leftPos + 'px + 2rem)';
			}
		}

		if (right) {
			let columnResizerRight = 'calc(' + (window.innerWidth - rightPos) + 'px + 0rem)';
			document.querySelector('.column-resizer-right').style.left = columnResizerRight;
			if (openRightSlideout) openRightSlideout.style.width = 'calc(' + rightPos + 'px - 7rem)';
			_Pages.centerPane.style.marginRight = 'calc(' + rightPos + 'px - 1rem)';
		} else {
			if (rightPos === 0) {
				let columnResizerRight = '4rem';
				document.querySelector('.column-resizer-right').style.left = columnResizerRight;
				_Pages.centerPane.style.marginRight = columnResizerRight;
			} else {
				document.querySelector('.column-resizer-right').style.left = 'calc(' + (window.innerWidth - rightPos) + 'px - 3rem)';
				_Pages.centerPane.style.marginRight = 'calc(' + (rightPos) + 'px + 2rem)';
			}
		}

	},
	refresh: function() {

		pagesSlideout.find(':not(.slideout-activator)').remove();

		pagesSlideout.append('<div id="pagesTree"></div>');
		pages = $('#pagesTree', pagesSlideout);

		let functionBarSwitchActive = (LSWrapper.getItem(_Pages.functionBarSwitchKey) === 'active');
		if (functionBarSwitchActive) {
			functionBar.append('<div id="function-bar-switch" class="icon"><svg viewBox="0 0 24 24" height="24" width="24" xmlns="http://www.w3.org/2000/svg"><g transform="matrix(1,0,0,1,0,0)"><path d="M.748,12.25a6,6,0,0,0,6,6h10.5a6,6,0,0,0,0-12H6.748A6,6,0,0,0,.748,12.25Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M17.248 9.25L17.248 15.25" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M14.248 9.25L14.248 15.25" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path></g></svg></div>');
		} else {
			functionBar.append('<div id="function-bar-switch" class="icon hidden"><svg viewBox="0 0 24 24" height="24" width="24" xmlns="http://www.w3.org/2000/svg"><g transform="matrix(1,0,0,1,0,0)"><path d="M23.248,12a6,6,0,0,1-6,6H6.748a6,6,0,0,1,0-12h10.5A6,6,0,0,1,23.248,12Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M6.748 9L6.748 15" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M9.748 9L9.748 15" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path></g></svg></div>');
		}

		functionBar.append('<div class="hidden" id="pagesPager"></div>');
		let pagesPager = $('#pagesPager', functionBar);

		Structr.fetchHtmlTemplate('pages/submenu', {}, (html) => {

			functionBar.append(html);

			for (const menuLink of document.querySelectorAll('#function-bar .tabs-menu li a')) {
				menuLink.onclick = (event) => _Pages.activateCenterPane(event);
			}

			var pPager = _Pager.addPager('pages', pagesPager, true, 'Page', null, function(pages) {
				pages.forEach(function(page) {
					StructrModel.create(page);
				});
			});

			pPager.cleanupFunction = function () {
				$('.node', pages).remove();
			};
			let pagerFilters = $('<span style="white-space: nowrap;">Filters: <input type="text" class="filter" data-attribute="name" placeholder="Name" title="Here you can filter the pages list by page name"/></span>');
			pPager.pager.append(pagerFilters);
			var categoryFilter = $('<input type="text" class="filter page-label" data-attribute="category" placeholder="Category" />');
			pagerFilters.append(categoryFilter);
			pPager.activateFilterElements();

			$.ajax({
				url: '/structr/rest/Page/category',
				success: function(data) {
					var categories = [];
					data.result.forEach(function(page) {
						if (page.category !== null && categories.indexOf(page.category) === -1) {
							categories.push(page.category);
						}
					});
					categories.sort();

					let helpText = 'Filter pages by page category.';
					if (categories.length > 0) {
						helpText += 'Available categories: \n\n' + categories.join('\n');
					}

					categoryFilter.attr('title', helpText);
				}
			});

			/*
			var bulkEditingHelper = $(
				'<button type="button" title="Open Bulk Editing Helper (Ctrl-Alt-E)" class="icon-button">'
				+ '<i class="icon ' + _Icons.getFullSpriteClass(_Icons.wand_icon) + '" />'
				+ '</button>');
			pPager.pager.append(bulkEditingHelper);
			bulkEditingHelper.on('click', e => {
				Structr.dialog('Bulk Editing Helper (Ctrl-Alt-E)');
				new RefactoringHelper(dialog).show();
			});
			*/

			functionBar.append('<a id="import_page" title="Import Template" class="icon"><svg xmlns="http://www.w3.org/2000/svg" version="1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:svgjs="http://svgjs.com/svgjs" viewBox="0 0 24 24" width="24" height="24"><g transform="matrix(1,0,0,1,0,0)"><path d="M11.250 17.250 A6.000 6.000 0 1 0 23.250 17.250 A6.000 6.000 0 1 0 11.250 17.250 Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M17.25 14.25L17.25 20.25" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M14.25 17.25L20.25 17.25" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M8.25,20.25h-6a1.5,1.5,0,0,1-1.5-1.5V2.25A1.5,1.5,0,0,1,2.25.75H12.879a1.5,1.5,0,0,1,1.06.439l2.872,2.872a1.5,1.5,0,0,1,.439,1.06V8.25" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path></g></svg></a>');
			functionBar.append('<a id="add_page" title="Add page" class="icon"><svg xmlns="http://www.w3.org/2000/svg" version="1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:svgjs="http://svgjs.com/svgjs" viewBox="0 0 24 24" width="24" height="24"><g transform="matrix(1,0,0,1,0,0)"><path d="M12 7.5L12 16.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M7.5 12L16.5 12" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M0.750 12.000 A11.250 11.250 0 1 0 23.250 12.000 A11.250 11.250 0 1 0 0.750 12.000 Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path></g></svg></a>');
			functionBar.append('<a id="add_template" title="Add Template" class="icon"><svg xmlns="http://www.w3.org/2000/svg" version="1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:svgjs="http://svgjs.com/svgjs" viewBox="0 0 24 24" width="24" height="24"><g transform="matrix(1,0,0,1,0,0)"><path d="M22.151,2.85,20.892,6.289l2.121,2.122a.735.735,0,0,1-.541,1.273l-3.653-.029L17.5,13.018a.785.785,0,0,1-1.485-.1L14.932,9.07,11.08,7.991a.786.786,0,0,1-.1-1.486l3.363-1.323-.029-3.653A.734.734,0,0,1,15.588.986L17.71,3.107,21.151,1.85A.8.8,0,0,1,22.151,2.85Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M14.932 9.07L0.75 23.25" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path></g></svg></a>');

			$('#import_page', functionBar).on('click', function(e) {
				e.stopPropagation();

				Structr.dialog('Import Template', function() {}, function() {});

				dialog.empty();
				dialogMsg.empty();

				dialog.append('<h3>Create page from source code ...</h3>'
						+ '<textarea id="_code" name="code" cols="40" rows="10" placeholder="Paste HTML code here"></textarea>');

				dialog.append('<h3>... or fetch page from URL: <input id="_address" name="address" size="40" value="http://"></h3><table class="props">'
						+ '<tr><td><label for="name">Name of new page:</label></td><td><input id="_name" name="name" size="20"></td></tr>'
						+ '<tr><td><label for="publicVisibilty">Visible to public</label></td><td><input type="checkbox" id="_publicVisible" name="publicVisibility"></td></tr>'
						+ '<tr><td><label for="authVisibilty">Visible to authenticated users</label></td><td><input type="checkbox" id="_authVisible" name="authVisibilty"></td></tr>'
						+ '<tr><td><label for="includeInExport">Include imported files in deployment export</label></td><td><input type="checkbox" id="_includeInExport" name="includeInExport" checked="checked"></td></tr>'
						+ '<tr><td><label for="processDeploymentInfo">Process deployment annotations</label></td><td><input type="checkbox" id="_processDeploymentInfo" name="processDeploymentInfo"></td></tr>'
						+ '</table>');

				$('#_address', dialog).on('blur', function() {
					var addr = $(this).val().replace(/\/+$/, "");
					$('#_name', dialog).val(addr.substring(addr.lastIndexOf("/") + 1));
				});

				dialog.append('<button class="action" id="startImport">Start Import</button>');

				$('#startImport').on('click', function(e) {
					e.stopPropagation();

					var code = $('#_code', dialog).val();
					var address = $('#_address', dialog).val();

					if (code.length > 0) {
						address = null;
					}

					var name = $('#_name', dialog).val();
					var publicVisible = $('#_publicVisible', dialog).prop('checked');
					var authVisible = $('#_authVisible', dialog).prop('checked');
					var includeInExport = $('#_includeInExport', dialog).prop('checked');
					var processDeploymentInfo = $('#_processDeploymentInfo', dialog).prop('checked');

					return Command.importPage(code, address, name, publicVisible, authVisible, includeInExport, processDeploymentInfo);
				});

			});

			$('#pull_page', functionBar).on('click', function(e) {
				e.stopPropagation();
				Structr.pullDialog('Page');
			});

			$('#add_page', functionBar).on('click', function(e) {
				e.stopPropagation();
				Command.createSimplePage();
			});

			// page template widgets present? Display special create page dialog
			_Widgets.fetchAllPageTemplateWidgets(function(result) {

				if (result && result.length) {

					$('#add_template').on('click', function(e) {

						e.stopPropagation();

						Structr.dialog('Select Template to Create New Page', function() {}, function() {});

						dialog.empty();
						dialogMsg.empty();
						dialog.append('<div id="template-tiles"></div>');

						var container = $('#template-tiles');

						result.forEach(function(widget) {

							var id = 'create-from-' + widget.id;
							container.append('<div class="app-tile"><h4>' + widget.name + '</h4><p>' + widget.description + '</p><button class="action" id="' + id + '">Create Page</button></div>');
							$('#' + id).on('click', function() {
								Command.create({ type: 'Page' }, function(page) {
									Structr.removeExpandedNode(page.id);
									Command.appendWidget(widget.source, page.id, page.id, null, {}, true);
								});
							});

						});
					});

				} else {

					// remove wizard button if no page templates exist (can be changed later when the dialog includes some hints etc.)
					$('#add_template').remove();
				}
			});

			Structr.adaptUiToAvailableFeatures();

			let selectedObjectId = LSWrapper.getItem(_Entities.selectedObjectIdKey);
			if (selectedObjectId) {
				Command.get(selectedObjectId, null, (obj) => {
					// Wait for the tree element to become visible
					const observer = new MutationObserver((mutations, obs) => {
						let el = Structr.node(selectedObjectId);
						if (el) {
							el.click();
							obs.disconnect();
							return;
						}
					});
					observer.observe(document, { childList: true, subtree: true });
				});
			}
		});

	},
	removePage:function(page) {

		Structr.removeExpandedNode(page.id);
	},
	deactivateAllSubmenuLinks: () => {
		for (const otherTab of document.querySelectorAll('#function-bar .tabs-menu li')) {
			otherTab.classList.remove('active');
		}
	},
	adaptSubmenu: (obj) => {
		switch (obj.type) {
			case 'Page':
				document.querySelector('a[href="#pages:html"]').closest('li').classList.add('hidden');
				document.querySelector('a[href="#pages:editor"]').closest('li').classList.add('hidden');
				document.querySelector('a[href="#pages:repeater"]').closest('li').classList.add('hidden');
				document.querySelector('a[href="#pages:events"]').closest('li').classList.add('hidden');
				break;

			default:

				if (obj.isContent) {
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
	},
	activateSubmenuTabElement: (tab) => {
		_Pages.deactivateAllSubmenuLinks();

		tab.classList.add('active');
	},
	activateSubmenuLink: (selector) => {
		let submenuLink = document.querySelector('#function-bar .tabs-menu li a[href="' + selector + '"]');
		if (submenuLink) {
			let tab = submenuLink.closest('li');
			_Pages.activateSubmenuTabElement(tab);
		}
	},
	activateCenterPane: (e) => {
		let el  = e.target;
		let tab = el.closest('li');
		let active = tab.classList.contains('active');

		// return if clicked tab already is active
		if (active) return;

		_Pages.activateSubmenuTabElement(tab);

		let obj = _Entities.selectedObject;
		if (!obj || !obj.type) return;

		active = tab.classList.contains('active');

		_Pages.refreshCenterPane(active);
	},
	refreshCenterPane: (active) => {

		 _Entities.deselectAllElements();

		let contentContainers = _Pages.centerPane.querySelectorAll('.content-container');

		for (const contentContainer of contentContainers) {
			_Pages.centerPane.removeChild(contentContainer);
		}

		let obj = _Entities.selectedObject; // || _Entities.selectedObjects.element || _Entities.selectedObjects.contentNode;

		let activeLink = document.querySelector('#function-bar .tabs-menu li.active a');
		let urlHash;

		_Pages.adaptSubmenu(obj);

//		let targetLink = document.querySelector('#function-bar .tabs-menu li a[href="' + location.hash + '"]');
//		if (!activeLink.closest('li').classList.contains('hidden')) {
//			urlHash = '#pages:basic';
//		}


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
			}

			urlHash = new URL(activeLink.href).hash;

			_Pages.activateSubmenuTabElement(activeLink.closest('li'));
		}

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

				let callbackObject = registeredDialogs[obj.type];

				if (!callbackObject && obj.isDOMNode) {
					callbackObject = registeredDialogs['DEFAULT_DOM_NODE'];
				}

				Structr.fetchHtmlTemplate('pages/basic', {}, (html) => {

					_Pages.centerPane.insertAdjacentHTML('beforeend', html);
					propertiesContainer = document.querySelector('#center-pane .basic-container');

					if (callbackObject) {
						callbackObject.callback($(propertiesContainer), obj);
					}

				});

				break;

			case '#pages:html':

				if (active) {

					Structr.fetchHtmlTemplate('pages/properties', {}, (html) => {

						_Pages.centerPane.insertAdjacentHTML('beforeend', html);
						propertiesContainer = document.querySelector('#center-pane .properties-container');

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

					});
				}

				break;

			case '#pages:advanced':

				if (active) {

					Structr.fetchHtmlTemplate('pages/properties', {}, (html) => {

						_Pages.centerPane.insertAdjacentHTML('beforeend', html);
						propertiesContainer = document.querySelector('#center-pane .properties-container');

						_Schema.getTypeInfo(obj.type, function(typeInfo) {

							_Entities.listProperties(obj, 'ui', $(propertiesContainer), typeInfo, function(properties) {

								// make container visible when custom properties exist
								if (Object.keys(properties).length > 0) {
									$('div#custom-properties-parent').removeClass("hidden");
								}

								$('input.dateField', $(propertiesContainer)).each(function(i, input) {
									_Entities.activateDatePicker($(input));
								});
							});
						});

					});
				}

				break;

			case '#pages:preview':

				let pageId = null;

				if (obj.type === 'Page') {

					if (active) {
						pageId = obj.id;
					}

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

				Structr.fetchHtmlTemplate('pages/repeater', {}, (html) => {
					_Pages.centerPane.insertAdjacentHTML('beforeend', html);
					repeaterContainer = document.querySelector('#center-pane .repeater-container');
					_Entities.repeaterConfig(obj, $(repeaterContainer));
				});
				break;

			case '#pages:events':
			case '#pages:event-binding':

				Structr.fetchHtmlTemplate('pages/events', {}, (html) => {
					_Pages.centerPane.insertAdjacentHTML('beforeend', html);
					repeaterContainer = document.querySelector('#center-pane .events-container');

					_Schema.getTypeInfo(obj.type, function(typeInfo) {
						_Entities.dataBindingDialog(obj, $(repeaterContainer), typeInfo);
					});
				});

				break;

			case '#pages:security':

				Structr.fetchHtmlTemplate('pages/security', {}, (html) => {

					_Pages.centerPane.insertAdjacentHTML('beforeend', html);
					securityContainer = document.querySelector('#center-pane .security-container');
					_Schema.getTypeInfo(obj.type, function(typeInfo) {
						_Entities.accessControlDialog(obj, $(securityContainer), typeInfo);
					});

				});
				break;

			default:
				console.log('do something else, urlHash:', urlHash);
		}

	},
	appendPageElement: function(entity) {

		entity = StructrModel.ensureObject(entity);

		let hasChildren = entity.children && entity.children.length;

		if (!pages) return;

		if ($('#id_' + entity.id, pages).length > 0) {
			return;
		}

		pages.append('<div id="id_' + entity.id + '" class="node page"><div class="node-selector"></div></div>');
		let div = Structr.node(entity.id);

		_Dragndrop.makeSortable(div);

		$('.button', div).on('mousedown', function(e) {
			e.stopPropagation();
		});

		let pageName = (entity.name ? entity.name : '[' + entity.type + ']');

		div.append('<i class="typeIcon ' + _Icons.getFullSpriteClass(_Icons.page_icon) + '" />'
				+ '<b title="' + escapeForHtmlAttributes(entity.name) + '" class="name_ abbr-ellipsis abbr-75pc">' + pageName + '</b> <span class="id">' + entity.id + '</span>' + (entity.position ? ' <span class="position">' + entity.position + '</span>' : ''));

		// div.append('<div class="node-selector"></div>')

		_Entities.appendExpandIcon(div, entity, hasChildren);
		//_Entities.appendAccessControlIcon(div, entity);

		_Entities.appendEditPropertiesIcon(div, entity);

		_Elements.enableContextMenuOnElement(div, entity);
		_Entities.setMouseOver(div);

		let dblclickHandler = (e) => {
			e.stopPropagation();
			let self = e.target;

			// only on page nodes and if not clicked on expand/collapse icon
			if (!self.classList.contains('expand_icon') && self.closest('.node').classList.contains('page')) {
				let link = self.closest('.page').querySelector('b.name_').getAttribute('title');

				let pagePath = entity.path ? entity.path.replace(/^\//, '') : link;

				let detailsObject = (LSWrapper.getItem(_Pages.detailsObjectIdKey + entity.id) ? '/' + LSWrapper.getItem(_Pages.detailsObjectIdKey + entity.id) : '');
				let requestParameters = (LSWrapper.getItem(_Pages.requestParametersKey + entity.id) ? '?' + LSWrapper.getItem(_Pages.requestParametersKey + entity.id) : '');

				let url = (entity.site && entity.site.hostname ? '//' + entity.site.hostname + (entity.site.port ? ':' + entity.site.port : '') + '/' : viewRootUrl) + pagePath + detailsObject + requestParameters;
				window.open(url);
			}
		};

		let pageNode = Structr.node(entity.id)[0];
		if (pageNode) {
			pageNode.removeEventListener('dblclick', dblclickHandler);
			pageNode.addEventListener('dblclick', dblclickHandler);
		}

		_Dragndrop.makeDroppable(div);

		return div;
	},

	saveInlineElement: function(el, callback) {
		var self = $(el);
		contentSourceId = self.attr('data-structr-id');
		var text = unescapeTags(cleanText(self.html()));
		self.attr('contenteditable', false);
		self.removeClass('structr-editable-area-active').removeClass('structr-editable-area');
		Command.setProperty(contentSourceId, 'content', text, false, function(obj) {
			if (contentSourceId === obj.id) {
				if (callback) {
					callback();
				}
				contentSourceId = null;
			}
		});
	},
	appendElementElement: function(entity, refNode, refNodeIsParent) {
		entity = StructrModel.ensureObject(entity);
		var div = _Elements.appendElementElement(entity, refNode, refNodeIsParent);

		if (!div) {
			return false;
		}

		var parentId = entity.parent && entity.parent.id;
		if (parentId) {
			$('.delete_icon', div).replaceWith('<i title="Remove" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.delete_brick_icon) + '" />');
			$('.button', div).on('mousedown', function(e) {
				e.stopPropagation();
			});
			$('.delete_icon', div).on('click', function(e) {
				e.stopPropagation();
				Command.removeChild(entity.id);
			});
		}

		_Dragndrop.makeDroppable(div);
		_Dragndrop.makeSortable(div);

		return div;
	},
	showTypeData: function(id) {
		if (!id) {
			return;
		}
		Command.get(id, 'id,name', function(sourceSchemaNode) {

			var typeKey = sourceSchemaNode.name.toLowerCase();
			LSWrapper.setItem(_Pages.selectedTypeKey, id);

			$('#data-wizard-attributes')
					.append('<div class="clear">&nbsp;</div><p>You can drag and drop the type box onto a block in a page. The type will be bound to the block which will loop over the result set.</p>')
					.append('<div class="data-binding-type draggable">:' + sourceSchemaNode.name + '</div>')
					.append('<h3>Properties</h3><div class="properties"></div>')
					.append('<p>Drag and drop these elements onto the page for data binding.</p>');

			var draggableSettings = {
				iframeFix: true,
				revert: 'invalid',
				containment: 'body',
				helper: 'clone',
				appendTo: '#main',
				stack: '.node',
				zIndex: 99
			};

			$('.data-binding-type').draggable(draggableSettings);

			Command.getSchemaInfo(sourceSchemaNode.name, function(properties) {

				var el = $('#data-wizard-attributes .properties');

				properties.reverse().forEach(function(property) {

					var subkey = property.relatedType ? 'name' : '';

					el.append('<div class="draggable data-binding-attribute ' + property.jsonName + '" collection="' + property.isCollection + '" subkey="' + subkey + '">' + typeKey + '.' + property.jsonName  + '</div>');
					el.children('.' + property.jsonName).draggable(draggableSettings);
				});
			});
		});
	},
	expandTreeNode: function(id, stack, lastId) {
		if (!id) {
			return;
		}
		lastId = lastId || id;
		stack = stack || [];
		stack.push(id);
		Command.get(id, 'id,parent', function(obj) {
			if (obj.parent) {
				_Pages.expandTreeNode(obj.parent.id, stack, lastId);
			} else {
				_Entities.expandAll(stack.reverse(), lastId);
			}
		});
	},
	highlight: function(id) {
		var node = Structr.node(id);
		if (node) {
			node.parent().removeClass('nodeHover');
			node.addClass('nodeHover');
		}
		var activeNode = Structr.node(id, '#active_');
		if (activeNode) {
			activeNode.parent().removeClass('nodeHover');
			activeNode.addClass('nodeHover');
		}
	},
	unhighlight: function(id) {
		var node = Structr.node(id);
		if (node) {
			node.removeClass('nodeHover');
		}
		var activeNode = Structr.node(id, '#active_');
		if (activeNode) {
			activeNode.removeClass('nodeHover');
		}
	},
	leftSlideoutTrigger: function (triggerEl, slideoutElement, otherSlideouts, activeTabKey, openCallback, closeCallback) {
		let leftResizer = document.querySelector('.column-resizer-left');
		if (!$(triggerEl).hasClass('noclick')) {
			//if (Math.abs(slideoutElement.position().left + slideoutElement.width() + 12) <= 3) {
			if (slideoutElement.position().left < -1) {
				Structr.closeLeftSlideOuts(otherSlideouts, activeTabKey, closeCallback);
				Structr.openLeftSlideOut(triggerEl, slideoutElement, activeTabKey, openCallback);
				if (leftResizer) {
					leftResizer.classList.remove('hidden');
				}
			} else {
				Structr.closeLeftSlideOuts([slideoutElement], activeTabKey, closeCallback);
				if (leftResizer) {
					leftResizer.classList.add('hidden');
				}
			}
		}
	},
	rightSlideoutClickTrigger: function (triggerEl, slideoutElement, otherSlideouts, activeTabKey, openCallback, closeCallback) {
		if (!$(triggerEl).hasClass('noclick')) {
			if (Math.abs(slideoutElement.position().left - $(window).width()) <= 3) {
			//if (slideoutElement.position().left >= $(window).width()) {
				Structr.closeSlideOuts(otherSlideouts, activeTabKey, closeCallback);
				Structr.openSlideOut(triggerEl, slideoutElement, activeTabKey, openCallback);
				document.querySelector('.column-resizer-right').classList.remove('hidden');
			} else {
				Structr.closeSlideOuts([slideoutElement], activeTabKey, closeCallback);
				document.querySelector('.column-resizer-right').classList.add('hidden');
			}
		}
	},
	slideoutClosedCallback: function(wasOpen) {
		if (wasOpen) {
			_Pages.resize();
		}
	},
	hidePagesPager: () => {
		// document.getElementById('pagesPager').classList.add('hidden');
		document.getElementById('function-bar-switch').classList.add('hidden');
	},
	showPagesPager: () => {
		// document.getElementById('pagesPager').classList.remove('hidden');
		document.getElementById('function-bar-switch').classList.remove('hidden');
	},
	showTabsMenu: () => {
		let tabsMenu = document.querySelector('#function-bar .tabs-menu');
		if (tabsMenu) tabsMenu.classList.remove('hidden');
	},
	refreshLocalizations: function() {

		let id = activeTab;

		let localizationsContainer = $('#localizations div.inner div.results');
		localizationsContainer.empty().attr('id', 'id_' + id);

		if (_Pages.previews.isPreviewForActiveForPage(id)) {

			let localeInput = $('#localizations input.locale');
			let locale      = localeInput.val();

			if (!locale) {
				blinkRed(localeInput);
				return;
			}

			let detailObjectId = LSWrapper.getItem(_Pages.detailsObjectIdKey + id);
			let queryString    = LSWrapper.getItem(_Pages.requestParametersKey + id);

			Command.listLocalizations(id, locale, detailObjectId, queryString, function (result) {

				$('#localizations .page').prop('id', 'id_' + id);

				let localizationIdKey = 'localizationId';
				let previousValueKey  = 'previousValue';

				if (result.length > 0) {

					for (let res of result) {

						let div   = _Pages.getNodeForLocalization(localizationsContainer, res.node);
						let tbody = $('tbody', div);
						let row   = $('<tr><td><div class="key-column allow-break">' + res.key + '</div></td><td class="domain-column">' + res.domain + '</td><td class="locale-column">' + ((res.localization !== null) ? res.localization.locale : res.locale) + '</td><td class="input"><input class="localized-value" placeholder="..."><a title="Delete" class="delete"><i class="' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" /></a></td></tr>');
						let key   = $('div.key-column', row).attr('title', res.key);
						let input = $('input.localized-value', row);

						if (res.localization) {
							let domainIdentical = (res.localization.domain === res.domain) || (!res.localization.domain && !res.domain);
							if (!domainIdentical) {
								res.localization = null;
								// we are getting the fallback localization for this entry - do not show this as we would update the wrong localization otherwise
							}
						}

						if (res.localization !== null) {
							row.addClass('has-value');
							input.val(res.localization.localizedName).data(localizationIdKey, res.localization.id).data(previousValueKey, res.localization.localizedName);
						}

						$('.delete', row).on('click', function(event) {
							event.preventDefault();

							let id = input.data(localizationIdKey);

							if (id) {
								var c = confirm('Are you sure you want to delete this localization ' + id + ' ?');
								if (c === true) {
									Command.deleteNode(id, false, () => {
										row.removeClass('has-value');
										input.data(localizationIdKey, null).data(previousValueKey, null).val('');
									});
								}
							}
						});

						input.on('blur', function() {

							let el             = $(this);
							let newValue       = el.val();
							let localizationId = el.data(localizationIdKey);
							let previousValue  = el.data(previousValueKey);
							let isChange       = (!previousValue && newValue !== '') || (previousValue && previousValue !== newValue);

							if (isChange) {

								if (localizationId) {

									Command.setProperties(localizationId, {
										localizedName: newValue
									}, function() {
										blinkGreen(el);
										el.data(previousValueKey, newValue);
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
										el.data(localizationIdKey, createdLocalization.id);
										el.data(previousValueKey, newValue);
										row.addClass('has-value');
										blinkGreen(el);
									});
								}
							}
						});

						tbody.append(row);
					};

				} else {

					localizationsContainer.append("<br>No localizations found in page.");
				}
			});

		} else {
			localizationsContainer.append('<br>Cannot show localizations - no preview loaded.<br><br>');
		}
	},
	getNodeForLocalization: function (container, entity) {

		let idString = 'locNode_' + entity.id;
		let existing = $('#' + idString, container);

		if (existing.length) {
			return existing;
		}

		let div = $('<div id="' + idString + '" class="node localization-element ' + (entity.tag === 'html' ? ' html_element' : '') + ' "></div>');

		div.data('nodeId', (_Entities.isContentElement(entity) ? entity.parent.id : entity.id ));

		let displayName = getElementDisplayName(entity);
		let iconClass   = _Icons.getFullSpriteClass(_Elements.getElementIcon(entity));
		let detailHtml  = '';

		if (entity.type === 'Content') {
			detailHtml = '<div class="abbr-ellipsis abbr-75pc">' + entity.content + '</div>';
		} else if (entity.type === 'Template') {
			if (entity.name) {
				detailHtml = '<div class="abbr-ellipsis abbr-75pc">' + displayName + '</div>';
			} else {
				detailHtml = '<div class="abbr-ellipsis abbr-75pc">' + escapeTags(entity.content) + '</div>';
			}
		} else {
			detailHtml = '<b title="' + escapeForHtmlAttributes(displayName) + '" class="tag_ name_">' + displayName + '</b>';
		}

		div.append('<i class="typeIcon ' + iconClass + '" />' + detailHtml + _Elements.classIdString(entity._html_id, entity._html_class));

		if (_Entities.isContentElement(entity)) {

			_Elements.appendEditContentIcon(div, entity);
		}

		_Entities.appendEditPropertiesIcon(div, entity, false);

		div.append('<table><thead><tr><th>Key</th><th>Domain</th><th>Locale</th><th>Localization</th></tr></thead><tbody></tbody></table>');

		container.append(div);

		_Entities.setMouseOver(div, undefined, ((entity.syncedNodesIds && entity.syncedNodesIds.length) ? entity.syncedNodesIds : [entity.sharedComponentId] ));

		return div;
	},

	previews: {
		previewElement: undefined,
		activePreviewPage: null,
		activePreviewHighlightElement: null,

		findDroppablesInIframe: function (iframeDocument, id) {
			var droppables = iframeDocument.find('[data-structr-id]');
			if (droppables.length === 0) {
				var html = iframeDocument.find('html');
				html.attr('data-structr-id', id);
				html.addClass('structr-element-container');
			}
			droppables = iframeDocument.find('[data-structr-id]');
			return droppables;
		},

		previewIframeLoaded: function (iframe, highlightElementId) {

			try {
				var doc = $(iframe.contentDocument || iframe.contentWindow.document);
				var head = doc.find('head');
				if (head) {
					head.append('<style media="screen" type="text/css">'
							+ '* { z-index: 0}\n'
							+ '.nodeHover { -moz-box-shadow: 0 0 5px #888; -webkit-box-shadow: 0 0 5px #888; box-shadow: 0 0 5px #888; }\n'
							+ '.structr-content-container { min-height: .25em; min-width: .25em; }\n'
							+ '.structr-element-container-active:hover { -moz-box-shadow: 0 0 5px #888; -webkit-box-shadow: 0 0 5px #888; box-shadow: 0 0 5px #888; }\n'
							+ '.structr-element-container-selected { -moz-box-shadow: 0 0 8px #860; -webkit-box-shadow: 0 0 8px #860; box-shadow: 0 0 8px #860; }\n'
							+ '.structr-element-container-selected:hover { -moz-box-shadow: 0 0 10px #750; -webkit-box-shadow: 0 0 10px #750; box-shadow: 0 0 10px #750; }\n'
							+ '.structr-editable-area { background-color: #ffe; -moz-box-shadow: 0 0 5px #888; -webkit-box-shadow: 0 0 5px yellow; box-shadow: 0 0 5px #888; }\n'
							+ '.structr-editable-area-active { background-color: #ffe; border: 1px solid orange ! important; color: #333; }\n'
							+ '.link-hover { border: 1px solid #00c; }\n'
							+ '.edit_icon, .add_icon, .delete_icon, .close_icon, .key_icon {  cursor: pointer; heigth: 16px; width: 16px; vertical-align: top; float: right;  position: relative;}\n'
							/**
							 * Fix for bug in Chrome preventing the modal dialog background
							 * from being displayed if a page is shown in the preview which has the
							 * transform3d rule activated.
							 */
							+ '.navbar-fixed-top { -webkit-transform: none ! important; }'
							+ '</style>');
				}

				_Pages.previews.findDroppablesInIframe(doc, highlightElementId).each(function(i, element) {

					var el = $(element);

	//				_Dragndrop.makeDroppable(el, highlightElementId);

					var structrId = el.attr('data-structr-id');
					if (structrId) {

						var offsetTop = -30;
						var offsetLeft = 0;
						el.on({
							click: function(e) {
								e.stopPropagation();
								var self = $(this);
								var selected = self.hasClass('structr-element-container-selected');
								self.closest('body').find('.structr-element-container-selected').removeClass('structr-element-container-selected');
								if (!selected) {
									self.toggleClass('structr-element-container-selected');
								}
								_Entities.deselectAllElements();
//								_Pages.databinding.displayDataBinding(structrId);
								if (!Structr.node(structrId)) {
									_Pages.expandTreeNode(structrId);
								} else {
									var treeEl = Structr.node(structrId);
									if (treeEl && !selected) {
										_Entities.highlightElement(treeEl);
									}
								}
								LSWrapper.setItem(_Entities.selectedObjectIdKey, structrId);
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

			} catch (e) {}

			_Pages.previews.activateComments(doc);
		},

		activateComments: function(doc, callback) {

			doc.find('*').each(function(i, element) {

				getComments(element).forEach(function(c) {

					var inner = $(getNonCommentSiblings(c.node));
					let newDiv = $('<div data-structr-id="' + c.id + '" data-structr-raw-content="' + escapeForHtmlAttributes(c.rawContent, false) + '"></div>');

					newDiv.append(inner);
					$(c.node).replaceWith(newDiv);

					$(newDiv).on({
						mouseover: function(e) {
							e.stopPropagation();
							var self = $(this);
							self.addClass('structr-editable-area');
							_Pages.highlight(self.attr('data-structr-id'));
						},
						mouseout: function(e) {
							e.stopPropagation();
							var self = $(this);
							self.removeClass('structr-editable-area');
							_Pages.unhighlight(self.attr('data-structr-id'));
						},
						click: function(e) {
							e.stopPropagation();
							e.preventDefault();
							var self = $(this);

							if (contentSourceId) {
								// click on same element again?
								if (self.attr('data-structr-id') === contentSourceId) {
									return;
								}
							}
							contentSourceId = self.attr('data-structr-id');

							if (self.hasClass('structr-editable-area-active')) {
								return false;
							}
							self.removeClass('structr-editable-area').addClass('structr-editable-area-active').prop('contenteditable', true).focus();

							// Store old text in global var and attribute
							textBeforeEditing = self.text();

							var srcText = expandNewline(self.attr('data-structr-raw-content'));

							// Replace only if it differs (e.g. for variables)
							if (srcText !== textBeforeEditing) {
								self.html(srcText);
								textBeforeEditing = srcText;
							}
							_Pages.expandTreeNode(contentSourceId);
							return false;
						},
						blur: function(e) {
							e.stopPropagation();
							_Pages.saveInlineElement(this, callback);
						}
					});
				});
			});
		},

		showPreviewInIframe: (pageId, highlightElementId) => {

			if (pageId) {

				_Pages.previews.activePreviewPage = pageId;
				if (highlightElementId) {
					_Pages.previews.activePreviewHighlightElement = highlightElementId;
				}

				Command.get(pageId, 'id,name', function (pageObj) {

					let detailsObject     = (LSWrapper.getItem(_Pages.detailsObjectIdKey + pageId) ? '/' + LSWrapper.getItem(_Pages.detailsObjectIdKey + pageId) : '');
					let requestParameters = (LSWrapper.getItem(_Pages.requestParametersKey + pageId) ? '&' + LSWrapper.getItem(_Pages.requestParametersKey + pageId) : '');
					let previewUrl        = viewRootUrl + pageObj.name + detailsObject + '?edit=2' + requestParameters;

					Structr.fetchHtmlTemplate('pages/preview', {}, (html) => {

						_Pages.centerPane.insertAdjacentHTML('beforeend', html);

						let iframe = _Pages.centerPane.querySelector('iframe');

						_Entities.highlightElement(Structr.node(highlightElementId));

						iframe.onload = () => {

							_Pages.previews.previewIframeLoaded(iframe, highlightElementId);

							let activatePreviewElementContainer = () => {

								let doc       = iframe.contentDocument || iframe.contentWindow.document;
								let previewEl = doc.querySelector('[data-structr-id="' + highlightElementId + '"]');

								doc.querySelectorAll('.structr-element-container-selected').forEach((el) => {
									el.classList.remove('structr-element-container-selected');
								});

								previewEl?.classList.add('structr-element-container-selected');
							};

							activatePreviewElementContainer();
						}

						iframe.src = previewUrl;
					});

					_Pages.resize();
				});
			}
		},

		updatePreviewInIframe: () => {

		},

		reloadPreviewInIframe: function() {
			_Pages.previews.showPreviewInIframe(_Pages.previews.activePreviewPage, _Pages.previews.activePreviewHighlightElement);
		},

		isPreviewForActiveForPage: (pageId) => {

			return (_Pages.previews.activePreviewPage === pageId);
		},

		modelForPageUpdated: (pageId) => {

			if (_Pages.previews.isPreviewForActiveForPage(pageId)) {
				_Pages.previews.reloadPreviewInIframe();
			}
		},

//		clearIframeDroppables: function() {
//
//			let droppables = $.ui.ddmanager.droppables['default'];
//
//			if (!droppables) {
//				return;
//			}
//
//			$.ui.ddmanager.droppables['default'] = droppables.filter((d) => {
//				return (!d.options.iframe);
//			});
//		},

		configurePreview: (entity) => {

			Structr.dialog('Edit Preview Settings of ' + entity.name, function() { return true; }, function() { return true; });

			dialog.empty();
			dialogMsg.empty();

			Structr.fetchHtmlTemplate('pages/previewconfig', { entity: entity }, (html) => {

				dialog.append(html);

				var detailsObjectIdInput   = $('#_details-object-id');
				var requestParametersInput = $('#_request-parameters');

				window.setTimeout(function() {
					detailsObjectIdInput.select().focus();
				}, 200);

				$('#clear-details-object-id').on('click', function() {
					detailsObjectIdInput.val('');
					var oldVal = LSWrapper.getItem(_Pages.detailsObjectIdKey + entity.id) || null;
					if (oldVal) {
						blinkGreen(detailsObjectIdInput);
						LSWrapper.removeItem(_Pages.detailsObjectIdKey + entity.id);
						detailsObjectIdInput.focus();

						_Pages.previews.modelForPageUpdated(entity.id);
					}
				});

				detailsObjectIdInput.on('blur', function() {
					var inp = $(this);
					var oldVal = LSWrapper.getItem(_Pages.detailsObjectIdKey + entity.id) || null;
					var newVal = inp.val() || null;
					if (newVal !== oldVal) {
						LSWrapper.setItem(_Pages.detailsObjectIdKey + entity.id, newVal);
						blinkGreen(detailsObjectIdInput);

						_Pages.previews.modelForPageUpdated(entity.id);
					}
				});

				$('#clear-request-parameters').on('click', function() {
					requestParametersInput.val('');
					var oldVal = LSWrapper.getItem(_Pages.requestParametersKey + entity.id) || null;
					if (oldVal) {
						blinkGreen(requestParametersInput);
						LSWrapper.removeItem(_Pages.requestParametersKey + entity.id);
						requestParametersInput.focus();

						_Pages.previews.modelForPageUpdated(entity.id);
					}
				});

				requestParametersInput.on('blur', function() {
					var inp = $(this);
					var oldVal = LSWrapper.getItem(_Pages.requestParametersKey + entity.id) || null;
					var newVal = inp.val() || null;
					if (newVal !== oldVal) {
						LSWrapper.setItem(_Pages.requestParametersKey + entity.id, newVal);
						blinkGreen(requestParametersInput);

						_Pages.previews.modelForPageUpdated(entity.id);
					}
				});

				$('.auto-refresh', dialog).on('click', function(e) {
					e.stopPropagation();
					var key = _Pages.autoRefreshDisabledKey + entity.id;
					var autoRefreshDisabled = (LSWrapper.getItem(key) === '1');
					if (autoRefreshDisabled) {
						LSWrapper.removeItem(key);
					} else {
						LSWrapper.setItem(key, '1');
					}
					blinkGreen($('.auto-refresh', dialog).parent());
				});

				var pageCategoryInput = $('#_page-category');
				pageCategoryInput.on('blur', function() {
					var oldVal = entity.category;
					var newVal = pageCategoryInput.val() || null;
					if (newVal !== oldVal) {
						Command.setProperty(entity.id, "category", newVal, false, function () {
							blinkGreen(pageCategoryInput);
							entity.category = newVal;
						});
					}
				});

				$('#clear-page-category').on('click', function () {
					Command.setProperty(entity.id, "category", null, false, function () {
						blinkGreen(pageCategoryInput);
						entity.category = null;
						pageCategoryInput.val("");
					});
				});
			});
		},

	},
	databinding: {
		displayDataBinding: function(id) {
			dataBindingSlideout.children('#data-binding-inputs').remove();
			dataBindingSlideout.append('<div class="inner" id="data-binding-inputs"></div>');

			var el = $('#data-binding-inputs');
			var entity = StructrModel.obj(id);

			if (entity) {
				_Entities.repeaterConfig(entity, el);
			}

			},
		reloadDataBindingWizard: function() {
			dataBindingSlideout.children('#wizard').remove();
			dataBindingSlideout.prepend('<div class="inner" id="wizard"><select id="type-selector"><option>--- Select type ---</option></select><div id="data-wizard-attributes"></div></div>');

			let lastSelectedType = LSWrapper.getItem(_Pages.selectedTypeKey);

			Command.list('SchemaNode', false, 1000, 1, 'name', 'asc', 'id,name', function(typeNodes) {

				let lastSelectedTypeExists = false;

				typeNodes.forEach(function(typeNode) {

					let selected = '';
					if (typeNode.id === lastSelectedType) {
						lastSelectedTypeExists = true;
						selected = 'selected';
					}

					$('#type-selector').append('<option ' + selected + ' value="' + typeNode.id + '">' + typeNode.name + '</option>');
				});

				$('#data-wizard-attributes').empty();
				if (lastSelectedType && lastSelectedTypeExists) {
					_Pages.showTypeData(lastSelectedType);
				}
			});

			$('#type-selector').on('change', function() {
				$('#data-wizard-attributes').empty();
				let id = $(this).children(':selected').attr('value');
				_Pages.showTypeData(id);
			});
		},
	},
	activeelements: {
		refreshActiveElements: function() {
//			var id = activeTab;
//
//			_Entities.activeElements = {};
//
//			var activeElementsContainer = $('#activeElements div.inner');
//			activeElementsContainer.empty().attr('id', 'id_' + id);
//
//			if (_Pages.previews.isPreviewForActiveForPage(id)) {
//
//				Command.listActiveElements(id, function(result) {
//					if (result.length > 0) {
//						result.forEach(function(activeElement) {
//							_Entities.handleActiveElement(activeElement);
//						});
//					} else {
//						activeElementsContainer.append('<br>Page does not contain any active elements.');
//					}
//				});
//
//			} else {
//				activeElementsContainer.append('<br>Unable to show active elements - no preview loaded.<br><br');
//			}
		},
	}
};