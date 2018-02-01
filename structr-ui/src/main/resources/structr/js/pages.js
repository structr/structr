/*
 * Copyright (C) 2010-2018 Structr GmbH
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
var rsw;
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
	activeTabRightKey: 'structrActiveTabRight_' + port,
	activeTabLeftKey: 'structrActiveTabLeft_' + port,
	selectedTypeKey: 'structrSelectedType_' + port,
	init: function() {

		_Pager.initPager('pages',   'Page', 1, 25, 'name', 'asc');
		_Pager.forceAddFilters('pages', 'Page', { hidden: false });
		_Pager.initPager('files',   'File', 1, 25, 'name', 'asc');
		_Pager.initPager('folders', 'Folder', 1, 25, 'name', 'asc');
		_Pager.initPager('images',  'Image', 1, 25, 'name', 'asc');

		$(window.document).on('mouseup', function() {
			_Elements.removeContextMenu();
		});

		Command.getByType('ShadowDocument', 1, 1, null, null, null, true, function(entities) {
			shadowPage = entities[0];
		});

	},
	resize: function(offsetLeft, offsetRight) {

		Structr.resize();

		$('body').css({
			position: 'fixed'
		});

		var windowWidth = $(window).width();
		var windowHeight = $(window).height();
		var headerOffsetHeight = 84, previewOffset = 30;

		if (previews) {

			if (offsetLeft) {
				previews.css({
					marginLeft: '+=' + offsetLeft + 'px'
				});
			}

			if (offsetRight) {
				previews.css({
					marginRight: '+=' + offsetRight + 'px'
				});
			}

			var w = windowWidth - parseInt(previews.css('marginLeft')) - parseInt(previews.css('marginRight')) - 15 + 'px';

			previews.css({
				width: w,
				height: windowHeight - headerOffsetHeight - 2 + 'px'
			});

			$('.previewBox', previews).css({
				width: w,
				height: windowHeight - (headerOffsetHeight + previewOffset) + 'px'
			});

			var iframes = $('.previewBox', previews).find('iframe');
			iframes.css({
				width: w,
				height: windowHeight - (headerOffsetHeight + previewOffset) + 'px'
			});
		}

		var leftSlideout = $('#' + activeTabLeft).closest('.slideOut');
		leftSlideout.css({
			height: windowHeight - headerOffsetHeight - 42 + 'px'
		});

		var rightSlideout = $('#' + activeTabRight).closest('.slideOut');
		rightSlideout.css({
			height: windowHeight - headerOffsetHeight - 42 + 'px'
		});

		$('.ver-scrollable').css({
			height: windowHeight - headerOffsetHeight - 42 + 'px'
		});
	},
	onload: function() {

		_Pages.init();

		Structr.updateMainHelpLink('https://support.structr.com/article/204');

		activeTab = LSWrapper.getItem(_Pages.activeTabKey);
		activeTabLeft = LSWrapper.getItem(_Pages.activeTabLeftKey);
		activeTabRight = LSWrapper.getItem(_Pages.activeTabRightKey);
		_Logger.log(_LogType.PAGES, 'value read from local storage', activeTab);

		_Logger.log(_LogType.PAGES, 'onload');

		main.prepend(
				'<div id="pages" class="slideOut slideOutLeft"><div class="compTab" id="pagesTab">Pages Tree View</div></div>'
				+ '<div id="activeElements" class="slideOut slideOutLeft"><div class="compTab" id="activeElementsTab">Active Elements</div><div class="page inner"></div></div>'
				+ '<div id="dataBinding" class="slideOut slideOutLeft"><div class="compTab" id="dataBindingTab">Data Binding</div></div>'
				+ '<div id="templates" class="slideOut slideOutLeft"><div class="compTab" id="templatesTab">Templates</div></div>'
				+ '<div id="previews"></div>'
				+ '<div id="widgetsSlideout" class="slideOut slideOutRight"><div class="compTab" id="widgetsTab">Widgets</div></div>'
				+ '<div id="palette" class="slideOut slideOutRight"><div class="compTab" id="paletteTab">HTML Palette</div></div>'
				+ '<div id="components" class="slideOut slideOutRight"><div class="compTab" id="componentsTab">Shared Components</div></div>'
				+ '<div id="elements" class="slideOut slideOutRight"><div class="compTab" id="elementsTab">Unused Elements</div></div>');

		pagesSlideout = $('#pages');
		activeElementsSlideout = $('#activeElements');
		dataBindingSlideout = $('#dataBinding');
		templatesSlideout = $('#templates');

		previews = $('#previews');

		widgetsSlideout = $('#widgetsSlideout');
		paletteSlideout = $('#palette');
		componentsSlideout = $('#components');
		elementsSlideout = $('#elements');
		elementsSlideout.data('closeCallback', _Elements.clearUnattachedNodes);

		rsw = widgetsSlideout.width() + 12;

		var pagesTabSlideoutAction = function() {
			_Pages.leftSlideoutTrigger(this, pagesSlideout, [activeElementsSlideout, dataBindingSlideout, templatesSlideout], _Pages.activeTabLeftKey, function (params) {
				_Pages.resize(params.sw, 0);
				_Pages.pagesTabResizeContent();
			}, _Pages.leftSlideoutClosedCallback);
		};
		$('#pagesTab').on('click', pagesTabSlideoutAction).droppable({
			tolerance: 'touch',
			over: pagesTabSlideoutAction
		});

		$('#activeElementsTab').on('click', function() {
			_Pages.leftSlideoutTrigger(this, activeElementsSlideout, [pagesSlideout, dataBindingSlideout, templatesSlideout], _Pages.activeTabLeftKey, function(params) {
				_Pages.refreshActiveElements();
				_Pages.resize(params.sw, 0);
			}, _Pages.leftSlideoutClosedCallback);
		});

		$('#dataBindingTab').on('click', function() {
			_Pages.leftSlideoutTrigger(this, dataBindingSlideout, [pagesSlideout, activeElementsSlideout, templatesSlideout], _Pages.activeTabLeftKey, function(params) {
				_Pages.reloadDataBindingWizard();
				_Pages.resize(params.sw, 0);
			}, _Pages.leftSlideoutClosedCallback);
		});

		$('#templatesTab').on('click', function() {
			_Pages.leftSlideoutTrigger(this, templatesSlideout, [pagesSlideout, activeElementsSlideout, dataBindingSlideout], _Pages.activeTabLeftKey, function(params) {
				_Pages.resize(params.sw, 0);
			}, _Pages.leftSlideoutClosedCallback);
		});

		$('#widgetsTab').on('click', function() {
			_Pages.rightSlideoutClickTrigger(this, widgetsSlideout, [paletteSlideout, componentsSlideout, elementsSlideout], false, _Widgets.reloadWidgets);
		});

		$('#paletteTab').on('click', function() {
			_Pages.rightSlideoutClickTrigger(this, paletteSlideout, [widgetsSlideout, componentsSlideout, elementsSlideout], false, _Elements.reloadPalette);
		});

		$('#componentsTab').on('click', function() {
			_Pages.rightSlideoutClickTrigger(this, componentsSlideout, [widgetsSlideout, paletteSlideout, elementsSlideout], false, _Elements.reloadComponents);
		}).droppable({
			tolerance: 'touch',
			over: function(e, ui) {
				_Pages.rightSlideoutClickTrigger(this, componentsSlideout, [widgetsSlideout, paletteSlideout, elementsSlideout], true, _Elements.reloadComponents);
			}
		});

		$('#elementsTab').on('click', function() {
			_Pages.rightSlideoutClickTrigger(this, elementsSlideout, [widgetsSlideout, paletteSlideout, componentsSlideout], false, _Elements.reloadUnattachedNodes);
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

		pagesSlideout.append('<div class="ver-scrollable" id="pagesTree"></div>');
		pages = $('#pagesTree', pagesSlideout);

		var pPager = _Pager.addPager('pages', pages, true, 'Page', null, function(pages) {
			pages.forEach(function(page) {
				StructrModel.create(page);
			});
			_Pages.hideAllPreviews();
		});
		pPager.cleanupFunction = function () {
			_Pages.clearPreviews();
			$('.node', pPager.el).remove();
		};
		pPager.pager.append('Filters: <input type="text" class="filter" data-attribute="name" placeholder="Name" title="Here you can filter the pages list by page name"/>');
		var categoryFilter = $('<input type="text" class="filter page-label" data-attribute="category" placeholder="Category" />');
		pPager.pager.append(categoryFilter);
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

		previewTabs.append('<li id="import_page" title="Import Template" class="button"><i class="add_button icon ' + _Icons.getFullSpriteClass(_Icons.pull_file_icon) + '" /></li>');
		previewTabs.append('<li id="pull_page" title="Sync page from remote instance" class="button module-dependend" data-structr-module="cloud"><i class="pull_page_button icon ' + _Icons.getFullSpriteClass(_Icons.pull_page_icon) + '" /></li>');
		previewTabs.append('<li id="add_page" title="Add page" class="button"><i class="add_button icon ' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" /></li>');

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
					+ '<tr><td><label for="processDeploymentInfo">Process deployment annotations</label></td><td><input type="checkbox" id="_processDeploymentInfo" name="processDeploymentInfo"></td></tr>'
					+ '</table>');

			$('#_address', dialog).on('blur', function() {
				var addr = $(this).val().replace(/\/+$/, "");
				_Logger.log(_LogType.PAGES, addr);
				$('#_name', dialog).val(addr.substring(addr.lastIndexOf("/") + 1));
			});

			dialog.append('<button id="startImport">Start Import</button>');

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
				var processDeploymentInfo = $('#_processDeploymentInfo', dialog).prop('checked');

				_Logger.log(_LogType.PAGES, 'start');
				return Command.importPage(code, address, name, publicVisible, authVisible, processDeploymentInfo);
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

		Structr.adaptUiToAvailableFeatures();

	},
	addTab: function(entity) {
		previewTabs.children().last().before('<li id="show_' + entity.id + '" class="page ' + entity.id + '_"></li>');

		var tab = $('#show_' + entity.id, previews);

		tab.append('<b title="' + entity.name + '" class="name_">' + fitStringToWidth(entity.name, 200) + '</b>');
		tab.append('<i title="Edit page settings of ' + entity.name + '" class="edit_ui_properties_icon button ' + _Icons.getFullSpriteClass(_Icons.wrench_icon) + '" />');
		tab.append('<i title="View ' + entity.name + ' in new window" class="view_icon button ' + _Icons.getFullSpriteClass(_Icons.eye_icon) + '" />');

		$('.view_icon', tab).on('click', function(e) {
			e.stopPropagation();
			var self = $(this);
			var link = $.trim(self.parent().children('b.name_').attr('title'));
			var url = viewRootUrl + link + (LSWrapper.getItem(detailsObjectId + entity.id) ? '/' + LSWrapper.getItem(detailsObjectId + entity.id) : '');
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
					+ '<tr><td><label for="details-object-id">UUID of details object to append to preview URL</label></td><td><input id="_details-object-id" name="details-object-id" size="30" value="' + (LSWrapper.getItem(detailsObjectId + entity.id) ?  LSWrapper.getItem(detailsObjectId + entity.id) : '') + '"> <i id="clear-details-object-id" class="' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></td></tr>'
					+ '<tr><td><label for="auto-refresh">Automatic refresh</label></td><td><input title="Auto-refresh page on changes" alt="Auto-refresh page on changes" class="auto-refresh" type="checkbox"' + (LSWrapper.getItem(autoRefreshDisabledKey + entity.id) ? '' : ' checked="checked"') + '></td></tr>'
					+ '<tr><td><label for="page-category">Category</label></td><td><input name="page-category" id="_page-category" type="text" value="' + (entity.category || '') + '"> <i id="clear-page-category" class="' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></td></tr>'
					+ '</table>');

			var detailsObjectIdInput = $('#_details-object-id');

			window.setTimeout(function() {
				detailsObjectIdInput.select().focus();
			}, 200);

			$('#clear-details-object-id').on('click', function() {
				detailsObjectIdInput.val('');
				var oldVal = LSWrapper.getItem(detailsObjectId + entity.id) || null;
				if (oldVal) {
					blinkGreen(detailsObjectIdInput);
					LSWrapper.removeItem(detailsObjectId + entity.id);
					detailsObjectIdInput.focus();
				}
			});

			detailsObjectIdInput.on('blur', function() {
				var inp = $(this);
				var oldVal = LSWrapper.getItem(detailsObjectId + entity.id) || null;
				var newVal = inp.val() || null;
				if (newVal !== oldVal) {
					LSWrapper.setItem(detailsObjectId + entity.id, newVal);
					blinkGreen(detailsObjectIdInput);
				}
			});

			$('.auto-refresh', dialog).on('click', function(e) {
				e.stopPropagation();
				var key = autoRefreshDisabledKey + entity.id;
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

		_Logger.log(_LogType.PAGES, 'resetTab', element);

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
				_Logger.log(_LogType.PAGES, 'click', self, self.css('z-index'));
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

		var name = $.trim(element.children('b.name_').attr('title'));
		_Logger.log(_LogType.PAGES, 'activateTab', element, name);

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

		_Logger.log(_LogType.PAGES, 'store active tab', activeTab);
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
					activeElementsContainer.append("<br><center>Page does not contain active elements</center>");
				}
			});

		} else {
			activeElementsContainer.append('<br><center>Cannot show active elements - no preview loaded<br><br></center>');
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
		var iframe = $('#preview_' + id);
		Command.get(id, "id,name", function(obj) {
			var url = viewRootUrl + obj.name + (LSWrapper.getItem(detailsObjectId + id) ? '/' + LSWrapper.getItem(detailsObjectId + id) : '') + '?edit=2';
			iframe.prop('src', url);
			_Logger.log(_LogType.PAGES, 'iframe', id, 'activated');
			_Pages.hideAllPreviews();
			iframe.parent().show();
			_Pages.resize();
		});
	},
	/**
	 * Reload preview iframe with given id
	 */
	reloadIframe: function(id) {
		if (!id || id !== activeTab || !_Pages.isPageTabPresent(id)) {
			return false;
		}
		var autoRefreshDisabled = LSWrapper.getItem(autoRefreshDisabledKey + id);

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
		_Logger.log(_LogType.PAGES, 'unloading all preview iframes');
		_Pages.clearIframeDroppables();
		$('iframe', $('#previews')).each(function() {
			var pageId = $(this).prop('id').substring('preview_'.length);
			var iframe = $('#preview_' + pageId);
			try {
				iframe.contents().empty();
			} catch (e) {}
			_Logger.log(_LogType.PAGES, 'iframe', pageId, 'deactivated');
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
		var id = element.prop('id').substring(5);

		element.off('hover');
		//var oldName = $.trim(element.children('.name_').text());
		var oldName = $.trim(element.children('b.name_').attr('title'));
		element.children('b').hide();
		element.find('.button').hide();
		var input = $('input.new-name', element);

		if (!input.length) {
			element.append('<input type="text" size="' + (oldName.length + 4) + '" class="new-name" value="' + oldName + '">');
			input = $('input', element);
		}

		input.show().focus().select();

		input.on('blur', function() {
			input.off('blur');
			_Logger.log(_LogType.PAGES, 'blur');
			var self = $(this);
			var newName = self.val();
			Command.setProperty(id, "name", newName);
			_Pages.resetTab(element, newName);
		});

		input.keypress(function(e) {
			if (e.keyCode === 13 || e.keyCode === 9) {
				e.preventDefault();
				_Logger.log(_LogType.PAGES, 'keypress');
				var self = $(this);
				var newName = self.val();
				Command.setProperty(id, "name", newName);
				_Pages.resetTab(element, newName);
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

		div.append('<i class="typeIcon ' + _Icons.getFullSpriteClass(_Icons.page_icon) + '" />'
				+ '<b title="' + entity.name + '" class="name_">' + fitStringToWidth(entity.name, 200) + '</b> <span class="id">' + entity.id + '</span>' + (entity.position ? ' <span class="position">' + entity.position + '</span>' : ''));

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

		div.append('<i title="Sync page \'' + entity.name + '\' to remote instance" class="push_icon button ' + _Icons.getFullSpriteClass(_Icons.push_file_icon) + '" />');
		div.children('.push_icon').on('click', function() {
			Structr.pushDialog(entity.id, true);
			return false;
		});

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

		$('#preview_' + entity.id).load(function() {
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
							+ '.nodeHover { -moz-box-shadow: 0 0 5px #888; -webkit-box-shadow: 0 0 5px #888; box-shadow: 0 0 5px #888; }\n'
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

					_Dragndrop.makeDroppable(el, entity.id);

					var structrId = el.attr('data-structr-id');
					if (structrId) {

						$('.move_icon', el).on('mousedown', function(e) {
							e.stopPropagation();
							var self = $(this);
							var element = self.closest('[data-structr-id]');
							_Logger.log(_LogType.PAGES, element);
							var entity = Structr.entity(structrId, element.prop('data-structr-id'));
							entity.type = element.prop('data-structr_type');
							entity.name = element.prop('data-structr_name');
							_Logger.log(_LogType.PAGES, 'move', entity);
							self.parent().children('.structr-node').show();
						});

						$('.delete_icon', el).on('click', function(e) {
							e.stopPropagation();
							var self = $(this);
							var element = self.closest('[data-structr-id]');
							var entity = Structr.entity(structrId, element.prop('data-structr-id'));
							entity.type = element.prop('data-structr_type');
							entity.name = element.prop('data-structr_name');
							_Logger.log(_LogType.PAGES, 'delete', entity);
							var parentId = element.prop('data-structr-id');

							Command.removeSourceFromTarget(entity.id, parentId);
							_Entities.deleteNode(this, entity);
						});
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
								_Logger.log(_LogType.PAGES, header);
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

		_Pages.pagesTabResizeContent();

		return div;

	},
	activateComments: function(doc, callback) {

		doc.find('*').each(function(i, element) {

			getComments(element).forEach(function(c) {

				var inner = $(getNonCommentSiblings(c.node));
				$(c.node).replaceWith('<div data-structr-id="' + c.id + '" data-structr-raw-content="' + escapeForHtmlAttributes(c.rawContent, false) + '"></div>');
				var el = $(element).children('[data-structr-id="' + c.id + '"]');
				el.append(inner);

				$(el).on({
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
		_Logger.log(_LogType.PAGES, '_Pages.appendElementElement(', entity, refNode, refNodeIsParent, ');');
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
	zoomPreviews: function(value) {
		$('.previewBox', previews).each(function() {
			var val = value / 100;
			var box = $(this);

			box.css('-moz-transform', 'scale(' + val + ')');
			box.css('-o-transform', 'scale(' + val + ')');
			box.css('-webkit-transform', 'scale(' + val + ')');

			var w = origWidth * val;
			var h = origHeight * val;

			box.width(w);
			box.height(h);

			$('iframe', box).width(w);
			$('iframe', box).height(h);

			_Logger.log(_LogType.PAGES, "box,w,h", box, w, h);

		});

	},
	displayDataBinding: function(id) {
		dataBindingSlideout.children('#data-binding-inputs').remove();
		dataBindingSlideout.append('<div class="inner" id="data-binding-inputs"></div>');

		var el = $('#data-binding-inputs');

		var entity = StructrModel.obj(id);

		el.append('<div id="data-binding-tabs" class="data-tabs"><ul><li class="active" id="tab-binding-rest">REST Query</li><li id="tab-binding-cypher">Cypher Query</li><li id="tab-binding-xpath">XPath Query</li><li id="tab-binding-function">Function Query</li></ul>'
				+ '<div id="content-tab-binding-rest"></div><div id="content-tab-binding-cypher"></div><div id="content-tab-binding-xpath"></div><div id="content-tab-binding-function"></div></div>');

		_Entities.appendTextarea($('#content-tab-binding-rest'), entity, 'restQuery', 'REST Query', '');
		_Entities.appendTextarea($('#content-tab-binding-cypher'), entity, 'cypherQuery', 'Cypher Query', '');
		_Entities.appendTextarea($('#content-tab-binding-xpath'), entity, 'xpathQuery', 'XPath Query', '');
		_Entities.appendTextarea($('#content-tab-binding-function'), entity, 'functionQuery', 'Function Query', '');

		_Entities.activateTabs(id, '#data-binding-tabs', '#content-tab-binding-rest');

		_Entities.appendInput(el, entity, 'dataKey', 'Data Key', 'Query results are mapped to this key and can be accessed by ${<i>&lt;dataKey&gt;.&lt;propertyKey&gt;</i>}');

	},
	reloadDataBindingWizard: function() {
		dataBindingSlideout.children('#wizard').remove();
		dataBindingSlideout.prepend('<div class="inner" id="wizard"><select id="type-selector"><option>--- Select type ---</option></select><div id="data-wizard-attributes"></div></div>');
		// Command.list(type, rootOnly, pageSize, page, sort, order, callback) {
		var selectedType = LSWrapper.getItem(_Pages.selectedTypeKey);
		Command.list('SchemaNode', false, 1000, 1, 'name', 'asc', 'id,name', function(typeNodes) {
			typeNodes.forEach(function(typeNode) {
				$('#type-selector').append('<option ' + (typeNode.id === selectedType ? 'selected' : '') + ' value="' + typeNode.id + '">' + typeNode.name + '</option>');
			});
		});

		$('#data-wizard-attributes').empty();
		if (selectedType) {
			_Pages.showTypeData(selectedType);
		}

		$('#type-selector').on('change', function() {
			$('#data-wizard-attributes').empty();
			var id = $(this).children(':selected').attr('value');
			_Pages.showTypeData(id);
		});

	},
	showTypeData: function(id) {
		if (!id) {
			return;
		}
		Command.get(id, "id,name", function(sourceSchemaNode) {

			var typeKey = sourceSchemaNode.name.toLowerCase();
			LSWrapper.setItem(_Pages.selectedTypeKey, id);

			$('#data-wizard-attributes')
					.append('<div class="clear">&nbsp;</div><p>You can drag and drop the type box onto a block in a page. The type will be bound to the block which will loop over the result set.</p>')
					.append('<div class="data-binding-type draggable">:' + sourceSchemaNode.name + '</div>')
					.append('<h3>Properties</h3><div class="properties"></div>')
					.append('<div class="clear">&nbsp;</div><p>Drag and drop these elements onto the page for data binding.</p>');

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
		Command.get(id, "id,parent", function(obj) {
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
	pagesTabResizeContent: function () {
		var storedLeftSlideoutWidth = LSWrapper.getItem(_Pages.leftSlideoutWidthKey);
		var psw = storedLeftSlideoutWidth ? parseInt(storedLeftSlideoutWidth) : (pagesSlideout.width() + 12);
		$('.node.page', pagesSlideout).width(psw - 35);
	},
	leftSlideoutTrigger: function (triggerEl, slideoutElement, otherSlideouts, activeTabKey, openCallback, closeCallback) {
		if ($(triggerEl).hasClass('noclick')) {
			$(triggerEl).removeClass('noclick');
		} else {
			if (Math.abs(slideoutElement.position().left + slideoutElement.width() + 12) <= 3) {
				Structr.closeLeftSlideOuts(otherSlideouts, activeTabKey, closeCallback);
				Structr.openLeftSlideOut(triggerEl, slideoutElement, activeTabKey, openCallback);
			} else {
				Structr.closeLeftSlideOuts([slideoutElement], activeTabKey, closeCallback);
			}
		}
	},
	rightSlideoutClickTrigger: function (triggerEl, slideoutElement, otherSlideouts, isDrag, callback) {
		if (Math.abs(slideoutElement.position().left - $(window).width()) <= 3) {
			Structr.closeSlideOuts(otherSlideouts, _Pages.activeTabRightKey);
			Structr.openSlideOut(slideoutElement, triggerEl, _Pages.activeTabRightKey, callback);
		} else if (!isDrag) {
			Structr.closeSlideOuts([slideoutElement], _Pages.activeTabRightKey);
		}
	},
	leftSlideoutClosedCallback: function(wasOpen, offsetLeft, offsetRight) {
		if (wasOpen) {
			_Pages.resize(offsetLeft, offsetRight);
		}
	}
};