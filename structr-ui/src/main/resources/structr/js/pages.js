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
var previews, previewTabs, controls, activeTab, activeTabLeft, activeTabRight, paletteSlideout, elementsSlideout, componentsSlideout, widgetsSlideout, pagesSlideout, activeElementsSlideout, dataBindingSlideout;
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
	leftSlideoutWidthKey: 'structrLeftSlideoutWidthKey_' + port,
	rightSlideoutWidthKey: 'structrRightSlideoutWidthKey_' + port,
	activeTabRightKey: 'structrActiveTabRight_' + port,
	activeTabLeftKey: 'structrActiveTabLeft_' + port,
	selectedTypeKey: 'structrSelectedType_' + port,
	autoRefreshDisabledKey: 'structrAutoRefreshDisabled_' + port,
	detailsObjectIdKey: 'structrDetailsObjectId_' + port,
	requestParametersKey: 'structrRequestParameters_' + port,
	init: function() {

		_Pager.initPager('pages',   'Page', 1, 25, 'name', 'asc');
		_Pager.forceAddFilters('pages', 'Page', { hidden: false });
		_Pager.initPager('files',   'File', 1, 25, 'name', 'asc');
		_Pager.initPager('folders', 'Folder', 1, 25, 'name', 'asc');
		_Pager.initPager('images',  'Image', 1, 25, 'name', 'asc');

		$(window.document).on('mouseup', function() {
			_Elements.removeContextMenu();
		});

		Structr.getShadowPage();

	},
	resize: function() {

		Structr.resize();

		$('body').css({
			position: 'fixed'
		});

		let windowWidth = $(window).width();

		if (previews) {

			let leftSlideout  = $('.slideOutLeft.open');
			let leftTab       = (leftSlideout.length === 0)  ? $('.slideOutRight .compTab') : $('.compTab', leftSlideout);
			let marginLeft    = (leftSlideout.length > 0 && leftTab.length > 0)   ? leftSlideout[0].getBoundingClientRect().right : 0;
			marginLeft       += leftTab[0].getBoundingClientRect().width;

			let rightSlideout = $('.slideOutRight.open');
			let rightTab      = (rightSlideout.length === 0) ? $('.slideOutRight .compTab') : $('.compTab', rightSlideout);
			let marginRight   = (rightSlideout.length > 0) ? (rightSlideout[0].getBoundingClientRect().right - rightSlideout[0].getBoundingClientRect().left) : 0;
			marginRight      += rightTab[0].getBoundingClientRect().width;

			previews.css('marginLeft', marginLeft);
			previews.css('marginRight', marginRight);

			let w = windowWidth - marginLeft - marginRight - 15 + 'px';

			previews.css('width', w);

			$('.previewBox', previews).css('width', w);

			var iframes = $('.previewBox', previews).find('iframe');
			iframes.css('width', w);
		}
	},
	onload: async function() {

		let html = await Structr.fetchHtmlTemplate('pages/pages', {});

		main[0].innerHTML = html;

		_Pages.init();

		Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('pages'));

		activeTab = LSWrapper.getItem(_Pages.activeTabKey);
		activeTabLeft = LSWrapper.getItem(_Pages.activeTabLeftKey);
		activeTabRight = LSWrapper.getItem(_Pages.activeTabRightKey);

		pagesSlideout          = $('#pages');
		activeElementsSlideout = $('#activeElements');
		dataBindingSlideout    = $('#dataBinding');
		localizationsSlideout  = $('#localizations');

		previews = $('#previews');

		widgetsSlideout    = $('#widgetsSlideout');
		paletteSlideout    = $('#palette');
		componentsSlideout = $('#components');
		elementsSlideout   = $('#elements');
		elementsSlideout.data('closeCallback', _Elements.clearUnattachedNodes);

		var pagesTabSlideoutAction = function() {
			_Pages.leftSlideoutTrigger(this, pagesSlideout, [activeElementsSlideout, dataBindingSlideout, localizationsSlideout], _Pages.activeTabLeftKey, function (params) {
				_Pages.resize();
			}, _Pages.slideoutClosedCallback);
		};
		$('#pagesTab').on('click', pagesTabSlideoutAction).droppable({
			tolerance: 'touch',
			over: pagesTabSlideoutAction
		});

		$('#activeElementsTab').on('click', function() {
			_Pages.leftSlideoutTrigger(this, activeElementsSlideout, [pagesSlideout, dataBindingSlideout, localizationsSlideout], _Pages.activeTabLeftKey, function(params) {
				if (params.isOpenAction) {
					_Pages.refreshActiveElements();
				}
				_Pages.resize();
			}, _Pages.slideoutClosedCallback);
		});

		$('#dataBindingTab').on('click', function() {
			_Pages.leftSlideoutTrigger(this, dataBindingSlideout, [pagesSlideout, activeElementsSlideout, localizationsSlideout], _Pages.activeTabLeftKey, function(params) {
				if (params.isOpenAction) {
					_Pages.reloadDataBindingWizard();
				}
				_Pages.resize();
			}, _Pages.slideoutClosedCallback);
		});

		$('#localizationsTab').on('click', function() {
			_Pages.leftSlideoutTrigger(this, localizationsSlideout, [pagesSlideout, activeElementsSlideout, dataBindingSlideout], _Pages.activeTabLeftKey, function(params) {
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

		$('#widgetsTab').on('click', function() {
			_Pages.rightSlideoutClickTrigger(this, widgetsSlideout, [paletteSlideout, componentsSlideout, elementsSlideout], _Pages.activeTabRightKey, function(params) {
				if (params.isOpenAction) {
					_Widgets.reloadWidgets();
				}
				_Pages.resize();
			}, _Pages.slideoutClosedCallback);
		});

		$('#paletteTab').on('click', function() {
			_Pages.rightSlideoutClickTrigger(this, paletteSlideout, [widgetsSlideout, componentsSlideout, elementsSlideout], _Pages.activeTabRightKey, function(params) {
				if (params.isOpenAction) {
					_Elements.reloadPalette();
				}
				_Pages.resize();
			}, _Pages.slideoutClosedCallback);
		});

		var componentsTabSlideoutAction = function() {
			_Pages.rightSlideoutClickTrigger(this, componentsSlideout, [widgetsSlideout, paletteSlideout, elementsSlideout], _Pages.activeTabRightKey, function(params) {
				if (params.isOpenAction) {
					_Elements.reloadComponents();
				}
				_Pages.resize();
			}, _Pages.slideoutClosedCallback);
		};
		$('#componentsTab').on('click', componentsTabSlideoutAction).droppable({
			tolerance: 'touch',
			over: function() {
				if (!componentsSlideout.hasClass('open')) {
					componentsTabSlideoutAction();
				}
			}
		});

		$('#elementsTab').on('click', function() {
			_Pages.rightSlideoutClickTrigger(this, elementsSlideout, [widgetsSlideout, paletteSlideout, componentsSlideout], _Pages.activeTabRightKey, function(params) {
				if (params.isOpenAction) {
					_Elements.reloadUnattachedNodes();
				}
				_Pages.resize();
			}, _Pages.slideoutClosedCallback);
		});

		previewTabs = $('<ul id="previewTabs"></ul>');
		previews.append(previewTabs);

		_Pages.refresh();

		if (activeTabLeft) {
			$('#' + activeTabLeft).addClass('active').click();
		}

		if (activeTabRight) {
			$('#' + activeTabRight).addClass('active').click();
		}

		// activate first page if local storage is empty
		if (!LSWrapper.getItem(_Pages.activeTabKey)) {
			window.setTimeout(function(e) {
				_Pages.activateTab($('#previewTabs .page').first());
			}, 1000);
		}

		_Pages.resize();

		$(window).off('resize').resize(function() {
			_Pages.resize();
		});

		Structr.unblockMenu(500);

	},
	clearPreviews: function() {

		if (previewTabs && previewTabs.length) {
			previewTabs.children('.page').remove();
		}
	},
	refresh: function() {

		pagesSlideout.find(':not(.compTab)').remove();
		previewTabs.empty();

		pagesSlideout.append('<div id="pagesPager"></div>');
		pagesSlideout.append('<div id="pagesTree"></div>');
		let pagesPager = $('#pagesPager', pagesSlideout);
		pages = $('#pagesTree', pagesSlideout);

		var pPager = _Pager.addPager('pages', pagesPager, true, 'Page', null, function(pages) {
			pages.forEach(function(page) {
				StructrModel.create(page);
			});
			_Pages.hideAllPreviews();
		});
		pPager.cleanupFunction = function () {
			_Pages.clearPreviews();
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

				var helpText = 'Here you can filter the pages list by page category.';
				if (categories.length > 0) {
					helpText += 'Available categories are: \n\n' + categories.join('\n');
				}
				helpText += '\n\nPro Tip: If category names have identical substrings you can filter for multiple categories at once.';

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

		previewTabs.append('<li id="import_page" title="Import Template" class="button"><i class="add_button icon ' + _Icons.getFullSpriteClass(_Icons.pull_file_icon) + '" /></li>');
		previewTabs.append('<li id="add_page" title="Add page" class="button"><i class="add_button icon ' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" /></li>');
		previewTabs.append('<li id="add_template" title="Add Template" class="button"><i class="add_button icon ' + _Icons.getFullSpriteClass(_Icons.wand_icon) + '" /></li>');

		$('#import_page', previewTabs).on('click', function(e) {
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

		$('#pull_page', previewTabs).on('click', function(e) {
			e.stopPropagation();
			Structr.pullDialog('Page');
		});

		$('#add_page', previewTabs).on('click', function(e) {
			e.stopPropagation();
			Command.createSimplePage();
		});

		// page template widgets present? Display special create page dialog
		Command.query('Widget', 10, 1, 'name', 'asc', { isPageTemplate: true }, function(result) {

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
		}, true);

		Structr.adaptUiToAvailableFeatures();

	},
	addTab: function(entity) {
		previewTabs.append('<li id="show_' + entity.id + '" class="page ' + entity.id + '_"></li>');

		var tab = $('#show_' + entity.id, previews);

		tab.append('<div class="fill-pixel"></div><b title="' + escapeForHtmlAttributes(entity.name) + '" class="name_ abbr-ellipsis abbr-200">' + entity.name + '</b>');
		tab.append('<i title="Edit page settings of ' + entity.name + '" class="edit_ui_properties_icon button ' + _Icons.getFullSpriteClass(_Icons.wrench_icon) + '" />');
		tab.append('<i title="View ' + entity.name + ' in new window" class="view_icon button ' + _Icons.getFullSpriteClass(_Icons.eye_icon) + '" />');

		$('.view_icon', tab).on('click', function(e) {
			e.stopPropagation();
			var self = $(this);
			var link = $.trim(self.parent().children('b.name_').attr('title'));
			let pagePath = entity.path ? entity.path.replace(/^\//, '') : link;

			let detailsObject     = (LSWrapper.getItem(_Pages.detailsObjectIdKey + entity.id) ? '/' + LSWrapper.getItem(_Pages.detailsObjectIdKey + entity.id) : '');
			let requestParameters = (LSWrapper.getItem(_Pages.requestParametersKey + entity.id) ? '?' + LSWrapper.getItem(_Pages.requestParametersKey + entity.id) : '');

			var url = (entity.site && entity.site.hostname ? '//' + entity.site.hostname + (entity.site.port ? ':' + entity.site.port : '') + '/' : viewRootUrl) + pagePath + detailsObject + requestParameters;
			window.open(url);
		});

		var editUiPropertiesIcon = $('.edit_ui_properties_icon', tab);
		editUiPropertiesIcon.hide();
		editUiPropertiesIcon.on('click', function(e) {
			e.stopPropagation();

			Structr.dialog('Edit Preview Settings of ' + entity.name, function() {
				return true;
			}, function() {
				return true;
			});

			dialog.empty();
			dialogMsg.empty();

			dialog.append('<p>With these settings you can influence the behaviour of the page previews only. They are not persisted on the Page object but only stored in the UI settings.</p>');

			dialog.append('<table class="props">'
					+ '<tr><td><label for="_details-object-id">UUID of details object to append to preview URL</label></td><td><input id="_details-object-id" value="' + (LSWrapper.getItem(_Pages.detailsObjectIdKey + entity.id) ? LSWrapper.getItem(_Pages.detailsObjectIdKey + entity.id) : '') + '" style="width:90%;"> <i id="clear-details-object-id" class="' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></td></tr>'
					+ '<tr><td><label for="_request-parameters">Request parameters to append to preview URL</label></td><td><code style="font-size: 10pt;">?</code><input id="_request-parameters" value="' + (LSWrapper.getItem(_Pages.requestParametersKey + entity.id) ? LSWrapper.getItem(_Pages.requestParametersKey + entity.id) : '') + '" style="width:90%;"> <i id="clear-request-parameters" class="' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></td></tr>'
					+ '<tr><td><label for="_auto-refresh">Automatic refresh</label></td><td><input id="_auto-refresh" title="Auto-refresh page on changes" alt="Auto-refresh page on changes" class="auto-refresh" type="checkbox"' + (LSWrapper.getItem(_Pages.autoRefreshDisabledKey + entity.id) ? '' : ' checked="checked"') + '></td></tr>'
					+ '<tr><td><label for="_page-category">Category</label></td><td><input id="_page-category" type="text" value="' + (entity.category || '') + '" style="width:90%;"> <i id="clear-page-category" class="' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></td></tr>'
					+ '</table>');

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

					_Pages.reloadIframe(entity.id);
				}
			});

			detailsObjectIdInput.on('blur', function() {
				var inp = $(this);
				var oldVal = LSWrapper.getItem(_Pages.detailsObjectIdKey + entity.id) || null;
				var newVal = inp.val() || null;
				if (newVal !== oldVal) {
					LSWrapper.setItem(_Pages.detailsObjectIdKey + entity.id, newVal);
					blinkGreen(detailsObjectIdInput);

					_Pages.reloadIframe(entity.id);
				}
			});

			$('#clear-request-parameters').on('click', function() {
				requestParametersInput.val('');
				var oldVal = LSWrapper.getItem(_Pages.requestParametersKey + entity.id) || null;
				if (oldVal) {
					blinkGreen(requestParametersInput);
					LSWrapper.removeItem(_Pages.requestParametersKey + entity.id);
					requestParametersInput.focus();

					_Pages.reloadIframe(entity.id);
				}
			});

			requestParametersInput.on('blur', function() {
				var inp = $(this);
				var oldVal = LSWrapper.getItem(_Pages.requestParametersKey + entity.id) || null;
				var newVal = inp.val() || null;
				if (newVal !== oldVal) {
					LSWrapper.setItem(_Pages.requestParametersKey + entity.id, newVal);
					blinkGreen(requestParametersInput);

					_Pages.reloadIframe(entity.id);
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

		return tab;
	},
	removePage:function(page) {

		var id = page.id;

		Structr.removeExpandedNode(id);
		var iframe = $('#preview_' + id);
		var tab = $('#show_' + id);

		if (id === activeTab) {
			_Pages.activateTab(tab.prev());
		}

		tab.remove();
		iframe.remove();

		_Pages.reloadPreviews();

	},
	resetTab: function(element) {

		element.children('input').hide();
		element.children('.name_').show();

		var icons = $('.button', element);
		var autoRefreshSelector = $('.auto-refresh', element);

		element.hover(function(e) {
			icons.showInlineBlock();
			autoRefreshSelector.showInlineBlock();
		}, function(e) {
			icons.hide();
			autoRefreshSelector.hide();
		});

		element.on('click', function(e) {
			e.stopPropagation();
			var self = $(this);
			var clicks = e.originalEvent.detail;
			if (clicks === 1) {
				if (self.hasClass('active')) {
					_Pages.makeTabEditable(self);
				} else {
					_Pages.activateTab(self);
				}
			}
		});

		if (element.prop('id').substring(5) === activeTab) {
			_Pages.activateTab(element);
		}
	},
	activateTab: function(element) {

		previewTabs.children('li').each(function() {
			$(this).removeClass('active');
		});

		if (!element.hasClass('page')) {
			return false;
		}

		var id = element.prop('id').substring(5);
		activeTab = id;

		_Pages.loadIframe(id);

		element.addClass('active');
		previews.removeClass('no-preview');

		LSWrapper.setItem(_Pages.activeTabKey, activeTab);

		if (LSWrapper.getItem(_Pages.activeTabLeftKey) === $('#activeElementsTab').prop('id')) {
			_Pages.refreshActiveElements();
		}

	},
	hideAllPreviews: function () {

		$('.previewBox', previews).each(function() {
			$(this).hide();
		});

	},
	refreshActiveElements: function() {
		var id = activeTab;

		_Entities.activeElements = {};

		var activeElementsContainer = $('#activeElements div.inner');
		activeElementsContainer.empty().attr('id', 'id_' + id);

		if (_Pages.isPageTabPresent(id)) {

			Command.listActiveElements(id, function(result) {
				if (result.length > 0) {
					result.forEach(function(activeElement) {
						_Entities.handleActiveElement(activeElement);
					});
				} else {
					activeElementsContainer.append('<br><center>Page does not contain any active elements.</center>');
				}
			});

		} else {
			activeElementsContainer.append('<br><center>Unable to show active elements - no preview loaded.<br><br></center>');
		}
	},
	/**
	 * Load and display the preview iframe with the given id.
	 */
	loadIframe: function(id) {
		if (!id || !_Pages.isPageTabPresent(id)) {
			return false;
		}
		_Pages.unloadIframes();

		window.clearTimeout(_Pages.loadIframeTimer);

		_Pages.loadIframeTimer = window.setTimeout(function() {

			var iframe = $('#preview_' + id);
			Command.get(id, 'id,name', function(obj) {
				let detailsObject     = (LSWrapper.getItem(_Pages.detailsObjectIdKey + id) ? '/' + LSWrapper.getItem(_Pages.detailsObjectIdKey + id) : '');
				let requestParameters = (LSWrapper.getItem(_Pages.requestParametersKey + id) ? '&' + LSWrapper.getItem(_Pages.requestParametersKey + id) : '');
				var url = viewRootUrl + obj.name + detailsObject + '?edit=2' + requestParameters;
				iframe.prop('src', url);

				_Pages.hideAllPreviews();
				iframe.parent().show();
				_Pages.resize();
			});
		}, 100);
	},
	/**
	 * Reload preview iframe with given id
	 */
	reloadIframe: function(id) {
		if (lastMenuEntry === _Pages._moduleName && (!id || id !== activeTab || !_Pages.isPageTabPresent(id))) {

			if ($('.previewBox iframe').length === 0) {
				previews.addClass('no-preview');
				_Pages.hideAllPreviews();
			}

			return false;
		}
		var autoRefreshDisabled = LSWrapper.getItem(_Pages.autoRefreshDisabledKey + id);

		if (!autoRefreshDisabled && id) {
			_Pages.loadIframe(id);
		}
	},
	/**
	 * simply checks if the preview tab for that id is visible. if it is not, the preview can not be shown
	 */
	isPageTabPresent: function(id) {
		return ($('#show_' + id, previewTabs).length > 0);
	},
	unloadIframes: function() {
		_Pages.clearIframeDroppables();
		$('iframe', previews).each(function() {
			var pageId = $(this).prop('id').substring('preview_'.length);
			var iframe = $('#preview_' + pageId);
			try {
				iframe.contents().empty();
			} catch (e) {}
		});
	},
	/**
	 * Reload "all" previews. This means, reload only the active preview iframe.
	 */
	reloadPreviews: function() {
		_Pages.reloadIframe(activeTab);
	},
	clearIframeDroppables: function() {
		var droppablesArray = [];
		var d = $.ui.ddmanager.droppables['default'];
		if (!d) {
			return;
		}
		d.forEach(function(d) {
			if (!d.options.iframe) {
				droppablesArray.push(d);
			}
		});
		$.ui.ddmanager.droppables['default'] = droppablesArray;
	},
	makeTabEditable: function(element) {

		let id = element.prop('id').substring(5);

		element.off('hover');
		let oldName = $.trim(element.children('b.name_').attr('title'));
		element.children('b').hide();
		element.find('.button').hide();
		let input = $('input.new-name', element);

		if (!input.length) {
			element.append('<input type="text" size="' + (oldName.length + 4) + '" class="new-name" value="' + oldName + '">');
			input = $('input', element);
		}

		input.show().focus().select();

		let saveFn = (self) => {
			let newName = self.val();
			Command.setProperty(id, "name", newName);
			_Pages.resetTab(element, newName);
		};

		input.off('blur').on('blur', function() {
			input.off('blur');
			saveFn($(this));
		});

		input.off('keypress').on('keypress', function(e) {
			if (e.keyCode === 13 || e.keyCode === 9) {
				e.stopPropagation();
				input.off('blur');
				saveFn($(this));
			}
		});

		element.off('click');
	},
	appendPageElement: function(entity) {

		entity = StructrModel.ensureObject(entity);

		var hasChildren = entity.children && entity.children.length;

		if (!pages) return;

		if ($('#id_' + entity.id, pages).length > 0) {
			return;
		}

		pages.append('<div id="id_' + entity.id + '" class="node page"></div>');
		var div = Structr.node(entity.id);

		_Dragndrop.makeSortable(div);

		$('.button', div).on('mousedown', function(e) {
			e.stopPropagation();
		});

		let pageName = (entity.name ? entity.name : '[' + entity.type + ']');

		div.append('<i class="typeIcon ' + _Icons.getFullSpriteClass(_Icons.page_icon) + '" />'
				+ '<b title="' + escapeForHtmlAttributes(entity.name) + '" class="name_ abbr-ellipsis abbr-75pc">' + pageName + '</b> <span class="id">' + entity.id + '</span>' + (entity.position ? ' <span class="position">' + entity.position + '</span>' : ''));

		_Entities.appendExpandIcon(div, entity, hasChildren);
		_Entities.appendAccessControlIcon(div, entity);

		div.append('<i title="Delete page \'' + entity.name + '\'" class="delete_icon button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" />');
		$('.delete_icon', div).on('click', function(e) {
			e.stopPropagation();
			_Entities.deleteNode(this, entity);
		});

		_Entities.appendEditPropertiesIcon(div, entity);

		div.append('<i title="Clone page \'' + entity.name + '\'" class="clone_icon button ' + _Icons.getFullSpriteClass(_Icons.clone_icon) + '" />');
		$('.clone_icon', div).on('click', function(e) {
			e.stopPropagation();
			Command.clonePage(entity.id);
		});

		_Elements.enableContextMenuOnElement(div, entity);
		_Entities.setMouseOver(div);

		var tab = _Pages.addTab(entity);

		var existingIframe = $('#preview_' + entity.id);
		if (existingIframe && existingIframe.length) {
			existingIframe.replaceWith('<iframe id="preview_' + entity.id + '"></iframe>');
		} else {
			previews.append('<div class="previewBox"><iframe id="preview_' + entity.id + '"></iframe></div><div style="clear: both"></div>');
		}

		_Pages.resetTab(tab, entity.name);

		$('#preview_' + entity.id).hover(function() {
			try {
				var self = $(this);
				var elementContainer = self.contents().find('.structr-element-container');
				elementContainer.addClass('structr-element-container-active');
				elementContainer.removeClass('structr-element-container');
			} catch (e) {}
		}, function() {
			try {
				var self = $(this);
				var elementContainer = self.contents().find('.structr-element-container-active');
				elementContainer.addClass('structr-element-container');
				elementContainer.removeClass('structr-element-container-active');
			} catch (e) {}
		});

		$('#preview_' + entity.id).on('load', function() {
			try {
				var doc = $(this).contents();
				var head = $(doc).find('head');
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
				_Pages.findDroppablesInIframe(doc, entity.id).each(function(i, element) {
					var el = $(element);

//					_Dragndrop.makeDroppable(el, entity.id);

					var structrId = el.attr('data-structr-id');
					if (structrId) {
//
//						$('.move_icon', el).on('mousedown', function(e) {
//							e.stopPropagation();
//							var self = $(this);
//							var element = self.closest('[data-structr-id]');
//							var entity = Structr.entity(structrId, element.prop('data-structr-id'));
//							entity.type = element.prop('data-structr_type');
//							entity.name = element.prop('data-structr_name');
//							self.parent().children('.structr-node').show();
//						});
//
//						$('.delete_icon', el).on('click', function(e) {
//							e.stopPropagation();
//							var self = $(this);
//							var element = self.closest('[data-structr-id]');
//							var entity = Structr.entity(structrId, element.prop('data-structr-id'));
//							entity.type = element.prop('data-structr_type');
//							entity.name = element.prop('data-structr_name');
//							var parentId = element.prop('data-structr-id');
//
//							Command.removeSourceFromTarget(entity.id, parentId);
//							_Entities.deleteNode(this, entity);
//						});
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
								_Pages.displayDataBinding(structrId);
								if (!Structr.node(structrId)) {
									_Pages.expandTreeNode(structrId);
								} else {
									var treeEl = Structr.node(structrId);
									if (treeEl && !selected) {
										_Entities.highlightElement(treeEl);
									}
								}
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

			_Pages.activateComments(doc);
		});

		_Dragndrop.makeDroppable(div);

		return div;
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
		_Pages.loadIframe(activeTab);
	},
	findDroppablesInIframe: function(iframeDocument, id) {
		var droppables = iframeDocument.find('[data-structr-id]');
		if (droppables.length === 0) {
			var html = iframeDocument.find('html');
			html.attr('data-structr-id', id);
			html.addClass('structr-element-container');
		}
		droppables = iframeDocument.find('[data-structr-id]');
		return droppables;
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
		if (!$(triggerEl).hasClass('noclick')) {
			if (Math.abs(slideoutElement.position().left + slideoutElement.width() + 12) <= 3) {
				Structr.closeLeftSlideOuts(otherSlideouts, activeTabKey, closeCallback);
				Structr.openLeftSlideOut(triggerEl, slideoutElement, activeTabKey, openCallback);
			} else {
				Structr.closeLeftSlideOuts([slideoutElement], activeTabKey, closeCallback);
			}
		}
	},
	rightSlideoutClickTrigger: function (triggerEl, slideoutElement, otherSlideouts, activeTabKey, openCallback, closeCallback) {
		if (!$(triggerEl).hasClass('noclick')) {
			if (Math.abs(slideoutElement.position().left - $(window).width()) <= 3) {
				Structr.closeSlideOuts(otherSlideouts, activeTabKey, closeCallback);
				Structr.openSlideOut(triggerEl, slideoutElement, activeTabKey, openCallback);
			} else {
				Structr.closeSlideOuts([slideoutElement], activeTabKey, closeCallback);
			}
		}
	},
	slideoutClosedCallback: function(wasOpen) {
		if (wasOpen) {
			_Pages.resize();
		}
	},
	refreshLocalizations: function() {

		let id = activeTab;

		if (_Pages.isPageTabPresent(id)) {

			let localeInput = $('#localizations input.locale');
			let locale      = localeInput.val();

			if (!locale) {
				blinkRed(localeInput);
				return;
			}

			let detailObjectId = LSWrapper.getItem(_Pages.detailsObjectIdKey + id);
			let queryString    = LSWrapper.getItem(_Pages.requestParametersKey + id);

			Command.listLocalizations(id, locale, detailObjectId, queryString, function(result) {

				$('#localizations .page').prop('id', 'id_' + id);

				let localizationsContainer = $('#localizations div.inner div.results');
				localizationsContainer.empty().attr('id', 'id_' + id);

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

					localizationsContainer.append("<br><center>No localizations found in page</center>");
				}
			});

		} else {
			localizationsContainer.append('<br><center>Cannot show localizations - no preview loaded<br><br></center>');
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
	}
};