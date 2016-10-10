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
var defaultType, defaultView, defaultSort, defaultOrder, defaultPage, defaultPageSize;
var searchField;
var defaultCollectionPageSize = 10;
var hiddenTabs = [];
var browser = (typeof document === 'object');
var crudPagerDataKey = 'structrCrudPagerData_' + port + '_';
var crudTypeKey = 'structrCrudType_' + port;
var crudHiddenColumnsKey = 'structrCrudHiddenColumns_' + port;
var crudHiddenTabsKey = 'structrCrudHiddenTabs_' + port;

if (browser) {

	$(document).ready(function() {

		defaultType = 'Page';
		defaultView = 'ui';
		defaultSort = '';
		defaultOrder = '';

		defaultPage = 1;
		defaultPageSize = 10;

		main = $('#main');

		Structr.registerModule('crud', _Crud);

		_Crud.resize();

	});

} else {
	defaultView = 'public';
}

var _Crud = {
	types: {},
	keys: {},
	getProperties: function(type, callback) {

		var url = rootUrl + '_schema/' + type + '/ui';
		$.ajax({
			url: url,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function(data) {

					// no schema entry found?
					if (!data || !data.result || data.result_count === 0) {

						// console.log("ERROR: loading Schema " + type);
						new MessageBuilder().warning("Failed loading Schema for '" + type + "' - check your resource access grants.").show();

					} else {

						var properties = {};
						data.result.forEach(function(prop) {
							properties[prop.jsonName] = prop;
						});

						_Crud.keys[type] = properties;

						if (callback) {
							callback(type, properties);
						}
					}
				},
				400: function(data) {
					console.log(data);
					Structr.errorFromResponse(data.responseJSON, url);
				},
				401: function(data) {
					console.log(data);
					Structr.errorFromResponse(data.responseJSON, url);
				},
				403: function(data) {
					console.log(data);
					Structr.errorFromResponse(data.responseJSON, url);
				},
				404: function(data) {
					console.log(data);
					Structr.errorFromResponse(data.responseJSON, url);
				},
				422: function(data) {
					Structr.errorFromResponse(data.responseJSON, url);
				}
			},
			error:function () {
				console.log("ERROR: loading Schema " + type);
			}

		});

	},
	type: null,
	pageCount: null,
	view: {},
	sort: {},
	order: {},
	page: {},
	pageSize: {},
	filteredNodeTypes: [],
	displayCustomTypes: true,
	displayCoreTypes: false,
	displayHtmlTypes: false,
	displayUiTypes: false,
	displayLogTypes: false,
	displayOtherTypes: false,
	init: function() {

		main.append('<div class="searchBox"><input class="search" name="search" placeholder="Search"><img class="clearSearchIcon" src="' + _Icons.grey_cross_icon + '"></div>');
		main.append('<div id="resourceTabs">'
			+ '<div id="resourceTabsSettings"></div>'
			+ '<ul id="resourceTabsMenu"><li class="last hidden">'
			+ '<input type="checkbox" id="resourceTabsAutoHideCheckbox"><label for="resourceTabsAutoHideCheckbox"> Show selected tabs only</label>'
			+ ' <span id="resourceTabsSelectAllWrapper"><input type="checkbox" id="resourceTabsSelectAll"><label for="resourceTabsSelectAll"> Select all</label></span>'
	        + ' <span id="resourceTabsToggleCustomWrapper"><input type="checkbox" id="resourceTabsToggleCustom"><label for="resourceTabsToggleCustom"> Custom types</label></span>'
	        + ' <span id="resourceTabsToggleCoreWrapper"><input type="checkbox" id="resourceTabsToggleCore"><label for="resourceTabsToggleCore"> Core types</label></span>'
	        + ' <span id="resourceTabsToggleHtmlWrapper"><input type="checkbox" id="resourceTabsToggleHtml"><label for="resourceTabsToggleHtml"> HTML types</label></span>'
	        + ' <span id="resourceTabsToggleUiWrapper"><input type="checkbox" id="resourceTabsToggleUi"><label for="resourceTabsToggleUi"> UI types</label></span>'
	        + ' <span id="resourceTabsToggleLogWrapper"><input type="checkbox" id="resourceTabsToggleLog"><label for="resourceTabsToggleLog"> Log types</label></span>'
	        + ' <span id="resourceTabsToggleOtherWrapper"><input type="checkbox" id="resourceTabsToggleOther"><label for="resourceTabsToggleOther"> Other types</label></span>'
			+ '</li></ul>'
	        + '</div>');

		if (Structr.getAutoHideInactiveTabs()) {
			$('#resourceTabsAutoHideCheckbox').prop('checked', true);
			Structr.doHideSelectAllCheckbox();
			$('#resourceTabsMenu li.last span').hide();
		}
		$('#resourceTabsAutoHideCheckbox').change(function () {
			var checked = $(this).prop('checked');
			Structr.setAutoHideInactiveTabs(checked);
			Structr.setHideInactiveTabs(checked);
			if (checked) {
				$('#resourceTabsMenu li.last span').hide();
			} else {
				$('#resourceTabsMenu li.last span').show();
			}
		});

		$('#resourceTabsSelectAll').change(function () {
			($(this).prop('checked') ? Structr.doSelectAllTabs() : Structr.doDeselectAllTabs());
			$('#resourceTabsToggleCustom').prop('checked', true);
			$('#resourceTabsToggleCore').prop('checked', true);
			$('#resourceTabsToggleHtml').prop('checked', true);
			$('#resourceTabsToggleUi').prop('checked', true);
			$('#resourceTabsToggleLog').prop('checked', true);
			$('#resourceTabsToggleOther').prop('checked', true);
		});

		$('#resourceTabsToggleCustom').change(function () {
			_Crud.displayCustomTypes = $(this).prop('checked');
			_Crud.filterTabs();
		});

		$('#resourceTabsToggleCore').change(function () {
			_Crud.displayCoreTypes = $(this).prop('checked');
			_Crud.filterTabs();
		});

		$('#resourceTabsToggleHtml').change(function () {
			_Crud.displayHtmlTypes = $(this).prop('checked');
			_Crud.filterTabs();
		});

		$('#resourceTabsToggleUi').change(function () {
			_Crud.displayUiTypes = $(this).prop('checked');
			_Crud.filterTabs();
		});

		$('#resourceTabsToggleLog').change(function () {
			_Crud.displayLogTypes = $(this).prop('checked');
			_Crud.filterTabs();
		});

		$('#resourceTabsToggleOther').change(function () {
			_Crud.displayOtherTypes = $(this).prop('checked');
			_Crud.filterTabs();
		});

		Structr.ensureIsAdmin($('#resourceTabs'), function() {

			_Crud.schemaLoading = false;
			_Crud.schemaLoaded = false;
			_Crud.keys = {};

			_Crud.loadSchema(function() {
				if (browser) {
					_Crud.initTabs();
				}
				Structr.determineSelectAllCheckboxState();
				_Crud.resize();
				Structr.unblockMenu();
				$('a[href="#' + _Crud.type + '"]').click();
			});

			searchField = $('.search', main);
			searchField.focus();

			searchField.keyup(function(e) {
				var searchString = $(this).val();
				if (searchString && searchString.length && e.keyCode === 13) {

					$('.clearSearchIcon').show().on('click', function() {
						_Crud.clearSearch(main);
					});

					_Crud.search(searchString, main, null, function(e, node) {
						e.preventDefault();
						_Crud.showDetails(node, false, node.type);
						return false;
					});

					$('#resourceTabs', main).hide();
					$('#resourceBox', main).hide();

				} else if (e.keyCode === 27 || searchString === '') {

					_Crud.clearSearch(main);

				}

			});

		});

	},
	onload: function() {

		$('#main-help a').attr('href', 'http://docs.structr.org/frontend-user-guide#Data');

		if (!_Crud.type) {
			_Crud.restoreType();
			//console.log(_Crud.type);
		}

		if (!_Crud.type) {
			_Crud.type = urlParam('type');
			//console.log(_Crud.type);
		}

		if (!_Crud.type) {
			_Crud.type = defaultType;
			//console.log(_Crud.type);
		}

		// check for single edit mode
		var id = urlParam('id');
		if (id) {
			//console.log('edit mode, editing ', id);
			_Crud.loadSchema(function() {
				_Crud.crudRead(null, id, function(node) {
					//console.log(node, _Crud.view[node.type]);
					_Crud.showDetails(node, node.type);
				});
			});

		} else {
			_Crud.init();
		}

		$(window).off('resize');
		$(window).on('resize', function() {
			_Crud.resize();
		});
		hiddenTabs = JSON.parse(LSWrapper.getItem(crudHiddenTabsKey)) || hiddenTabs;
		_Logger.log(_LogType.CRUD, '########## Hidden tabs ##############', hiddenTabs);

	},
	initTabs: function() {

		Object.keys(_Crud.types).sort().forEach(function(type) {
			//console.log('Init tab for type', type);
			_Crud.addTab(_Crud.types[type]);
		});

		$('#resourceTabsMenu li:not(.last) input[type="checkbox"]').on('click', function(e) {
			e.stopPropagation();
			//e.preventDefault();

			var inp = $(this);

			var key = inp.parent().find('span').text();

			if (!inp.is(':checked')) {
				hiddenTabs.push(key);
			} else {
				if (hiddenTabs.indexOf(key) > -1) {
					hiddenTabs.splice(hiddenTabs.indexOf(key), 1);
				}
			}
			 LSWrapper.setItem(crudHiddenTabsKey, JSON.stringify(hiddenTabs));

			 Structr.determineSelectAllCheckboxState();
		});

		$('#resourceTabs').tabs({
			active: true,
			activate: function(event, ui) {

				var newType = ui.newPanel[0].id;
				_Crud.getProperties(newType, function(type, properties) {

					fastRemoveAllChildren($('#' + type)[0]);
					//console.log('deactivated', _Crud.type, 'activated', newType);

					_Crud.determinePagerData(type);

					var typeNode = $('#' + type);
					var pagerNode = _Crud.addPager(type, typeNode);
					typeNode.append('<table><thead></thead><tbody></tbody></table>');
					var table = $('table', typeNode);
					$('thead', table).append('<tr></tr>');
					var tableHeaderRow = $('tr:first-child', table);

					_Crud.filterKeys(type, Object.keys(properties)).forEach(function(key) {
						var prop = properties[key];
						tableHeaderRow.append('<th class="___' + prop.jsonName + '">' + _Crud.formatKey(prop.jsonName) + '</th>');
					});
					tableHeaderRow.append('<th class="___action_header">Actions</th>');

					typeNode.append('<div class="infoFooter">Query: <span class="queryTime"></span> s &nbsp; Serialization: <span class="serTime"></span> s</div>');
					typeNode.append('<button id="create' + type + '"><img src="' + _Icons.add_icon + '"> Create new ' + type + '</button>');
					typeNode.append('<button id="export' + type + '"><img src="' + _Icons.database_table_icon + '"> Export as CSV</button>');
					typeNode.append('<button id="import' + type + '"><img src="' + _Icons.database_add_icon + '"> Import CSV</button>');

					$('#create' + type, typeNode).on('click', function() {
						_Crud.crudCreate(type);
					});
					$('#export' + type, typeNode).on('click', function() {
						_Crud.crudExport(type);
					});

					$('#import' + type, typeNode).on('click', function() {
						_Crud.crudImport(type);
					});

					var pagerNode = $('.pager', typeNode);
					_Crud.deActivatePagerElements(pagerNode);
					_Crud.activateList(type, properties);
					typeNode = $('#' + type);
					pagerNode = $('.pager', typeNode);
					_Crud.activatePagerElements(type, pagerNode);
					_Crud.updateUrl(type);
				});

			}
		});
	},
	filterTabs: function() {

		Command.getSchemaInfo(null, function(nodes) {

			nodes.sort(function(a, b) {
				var aName = a.name.toLowerCase();
				var bName = b.name.toLowerCase();
				return aName < bName ? -1 : aName > bName ? 1 : 0;
			});

			nodes.forEach(function(node) {

				var hide = false;

				if (!_Crud.displayCustomTypes && node.className.startsWith('org.structr.dynamic')) hide = true;
				if (!hide && !_Crud.displayCoreTypes   && node.className.startsWith('org.structr.core.entity')) hide = true;
				if (!hide && !_Crud.displayHtmlTypes   && node.className.startsWith('org.structr.web.entity.html')) hide = true;
				if (!hide && !_Crud.displayUiTypes     && node.className.startsWith('org.structr.web.entity') && !(_Crud.displayHtmlTypes && node.className.startsWith('org.structr.web.entity.html'))) hide = true;
				if (!hide && !_Crud.displayLogTypes    && node.className.startsWith('org.structr.rest.logging.entity')) hide = true;
				if (!hide && !_Crud.displayOtherTypes  && node.className.startsWith('org.structr.xmpp')) hide = true;

				//console.log(hide, node.type);
				if (hide) {
					//_Crud.filteredNodeTypes.push(node.type);
					$('#resourceTabsMenu li:not(.last) a[href="#' + node.type + '"] input[type="checkbox"]:checked').click();
					return;
				} else {
					//_Crud.filteredNodeTypes.splice(_Crud.filteredNodeTypes.indexOf(node.type), 1);
					$('#resourceTabsMenu li:not(.last) a[href="#' + node.type + '"] input[type="checkbox"]:not(:checked)').click();
				}

			});

		});

	},
	updateUrl: function(type) {
		//console.log('updateUrl', type, _Crud.pageSize[type], _Crud.page[type]);

		if (type) {
			_Crud.type = type;
			_Crud.storeType();
			_Crud.storePagerData();
			//window.history.pushState('', '', _Crud.sortAndPagingParameters(_Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type]) + '&view=' + _Crud.view[type] + '&type=' + type + '#crud');
			window.location.hash = 'crud';

			if (Structr.getAutoHideInactiveTabs() || Structr.getHideInactiveTabs()) {
				Structr.doHideInactiveTabs();
			}
		}
		searchField.focus();

	},
	/**
	 * Read the schema from the _schema REST resource and call 'callback'
	 * after the schema is loaded.
	 */
	loadSchema: function(callback) {

		$.ajax({
			url: rootUrl + '_schema',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function(data) {
					data.result.forEach(function(typeObj) {
						_Crud.types[typeObj.type] = typeObj;
					});

					if (callback) {
						callback();
					}
				}
			}
		});
	},
	determinePagerData: function(type) {

		// Priority: JS vars -> Local Storage -> URL -> Default

		if (!_Crud.view[type]) {
			_Crud.view[type] = urlParam('view');
			_Crud.sort[type] = urlParam('sort');
			_Crud.order[type] = urlParam('order');
			_Crud.pageSize[type] = urlParam('pageSize');
			_Crud.page[type] = urlParam('page');
		}

		if (!_Crud.view[type]) {
			_Crud.restorePagerData();
		}

		if (!_Crud.view[type]) {
			_Crud.view[type] = defaultView;
			_Crud.sort[type] = defaultSort;
			_Crud.order[type] = defaultOrder;
			_Crud.pageSize[type] = defaultPageSize;
			_Crud.page[type] = defaultPage;
		}
	},
	addTab: function(typeObj) {
		var type = typeObj.type;
		var hidden = hiddenTabs.indexOf(type) > -1;
		$('#resourceTabsMenu li.last').before('<li' + (hidden ? ' class="hidden"' : '') + '><a href="#' + type + '"><span>' + _Crud.formatKey(type) + '</span><input type="checkbox"' + (!hidden ? ' checked="checked"' : '') + '></a></li>');
		$('#resourceTabs').append('<div class="resourceBox" id="' + type + '" data-url="' + typeObj.url + '"></div>');
		$('#resourceTabsMenu li.last').removeClass('hidden');
		_Crud.resize();
	},
	/**
	 * Return true if the combination of the given property key
	 * and the given type is a collection
	 */
	isCollection: function(key, type) {
		return (key && type && _Crud.keys[type] && _Crud.keys[type][key] && _Crud.keys[type][key].isCollection);
	},
	/**
	 * Return true if the combination of the given property key
	 * and the given type is an Enum
	 */
	isEnum: function(key, type) {
		return (key && type && _Crud.keys[type] && _Crud.keys[type][key] && _Crud.keys[type][key].className === 'org.structr.core.property.EnumProperty');
	},
	/**
	 * Return true if the combination of the given property key
	 * and the given type is a read-only property
	 */
	readOnly: function(key, type) {
		return (key && type && _Crud.keys[type] && _Crud.keys[type][key] && _Crud.keys[type][key].readOnly);
	},
	/**
	 * Return the related type of the given property key
	 * of the given type
	 */
	relatedType: function(key, type) {
		return (key && type && _Crud.keys[type] && _Crud.keys[type][key] && _Crud.keys[type][key].relatedType);
	},
	/**
	 * Return the format information stored about the given property key
	 */
	getFormat: function(key, type) {
		var typeDef = _Crud.keys[type][key];
		return typeDef.format;
	},
	/**
	 * Append a pager for the given type to the given DOM element.
	 */
	addPager: function(type, el) {

		if (!_Crud.page[type]) {
			_Crud.page[type] = urlParam('page') ? urlParam('page') : (defaultPage ? defaultPage : 1);
		}

		if (!_Crud.pageSize[type]) {
			_Crud.pageSize[type] = urlParam('pageSize') ? urlParam('pageSize') : (defaultPageSize ? defaultPageSize : 10);
		}

		el.append('<div class="pager" style="clear: both"><button class="pageLeft">&lt; Prev</button>'
				+ ' Page <input class="page" type="text" size="3" value="' + _Crud.page[type] + '"><button class="pageRight">Next &gt;</button> of <input class="readonly pageCount" readonly="readonly" size="3" value="' + nvl(_Crud.pageCount, 0) + '">'
				+ ' Page Size: <input class="pageSize" type="text" size="3" value="' + _Crud.pageSize[type] + '"></div>');

		el.append('<div class="resource-link"><a target="_blank" href="' + rootUrl + type + '">/' + type + '</a></div>');

		return $('.pager', el);

	},
	storeType: function() {
		LSWrapper.setItem(crudTypeKey, _Crud.type);
	},
	restoreType: function() {
		var val = LSWrapper.getItem(crudTypeKey);
		if (val) {
			_Crud.type = val;
		}
	},
	storePagerData: function() {
		var type = _Crud.type;
		var pagerData = _Crud.view[type] + ',' + _Crud.sort[type] + ',' + _Crud.order[type] + ',' + _Crud.page[type] + ',' + _Crud.pageSize[type];
		LSWrapper.setItem(crudPagerDataKey + type, pagerData);
	},
	restorePagerData: function() {
		var type = _Crud.type;
		var val = LSWrapper.getItem(crudPagerDataKey + type);

		if (val) {
			var pagerData = val.split(',');
			_Crud.view[type] = pagerData[0];
			_Crud.sort[type] = pagerData[1];
			_Crud.order[type] = pagerData[2];
			_Crud.page[type] = pagerData[3];
			_Crud.pageSize[type] = pagerData[4];
		}
	},
	setCollectionPageSize: function(type, key, value) {
		LSWrapper.setItem(crudPagerDataKey + '_collectionPageSize_' + type + '.___' + key, value);
	},
	getCollectionPageSize: function(type, key) {
		return LSWrapper.getItem(crudPagerDataKey + '_collectionPageSize_' + type + '.___' + key);
	},
	setCollectionPage: function(type, key, value) {
		LSWrapper.setItem(crudPagerDataKey + '_collectionPage_' + type + '.___' + key, value);
	},
	getCollectionPage: function(type, key) {
		return LSWrapper.getItem(crudPagerDataKey + '_collectionPage_' + type + '.___' + key);
	},
	replaceSortHeader: function(type) {
		var table = $('#' + type + ' table');
		var newOrder = (_Crud.order[type] && _Crud.order[type] === 'desc' ? 'asc' : 'desc');
		$('th', table).each(function(i, t) {
			var th = $(t);
			var key = th.attr('class').substring(3);
			if (key === "action_header") {

				th.empty();
				th.append('Actions <img style="margin-left: 4px" src="' + _Icons.view_detail_icon + '" alt="Configure columns" title="Configure columns">');
				$('img', th).on('click', function(event) {

					_Crud.dialog('<h3>Configure columns for type ' + type + '</h3>', function() {
					}, function() {
					});

					$('div.dialogText').append('<table class="props" id="configure-' + type + '-columns"></table>');

					var table = $('#configure-' + type + '-columns');

					// append header row
					table.append('<tr><th>Column Key</th><th>Visible</th></tr>');
					var filterSource = LSWrapper.getItem(crudHiddenColumnsKey + type);
					var filteredKeys = {};
					if (filterSource) {

						filteredKeys = JSON.parse(filterSource);
					}

					Object.keys(_Crud.keys[type]).forEach(function(key) {

						var checkboxKey = 'column-' + type + '-' + key + '-visible';
						var hidden = filteredKeys.hasOwnProperty(key) && filteredKeys[key] === 0;

						table.append(
								'<tr>'
								+ '<td><b>' + key + '</b></td>'
								+ '<td><input type="checkbox" id="' + checkboxKey + '" ' + (hidden ? '' : 'checked="checked"') + '></td>'
								+ '</tr>'
								);

						$('#' + checkboxKey).on('click', function() {
							_Crud.toggleColumn(type, key);
						});
					});

					dialogCloseButton = $('.closeButton', $('#dialogBox'));
					dialogCloseButton.on('click', function() {
						location.reload();
					});

				});

			} else if (key !== 'Actions') {
				$('a', th).off('click');
				th.empty();
				var sortKey = key;
				th.append(
					'<img src="' + _Icons.grey_cross_icon + '" alt="Hide this column" title="Hide this column">'
					+ '<a href="' + _Crud.sortAndPagingParameters(type, sortKey, newOrder, _Crud.pageSize[type], _Crud.page[type]) + '#' + type + '">' + _Crud.formatKey(key) + '</a>');

				if (_Crud.isCollection(key, type)) {
					_Crud.appendPerCollectionPager(th, type, key);
				}

				$('a', th).on('click', function(event) {
					event.preventDefault();
					_Crud.sort[type] = key;
					_Crud.order[type] = (_Crud.order[type] && _Crud.order[type] === 'desc' ? 'asc' : 'desc');
					_Crud.refreshList(type);
					//_Crud.updateUrl(type);
					return false;
				});
				$('img', th).on('click', function(event) {

					event.preventDefault();
					_Crud.toggleColumn(type, key);
					return false;
				});
			}
		});
	},
	appendPerCollectionPager: function(el, type, key, callback) {
		el.append('<input type="text" class="collection-page-size" size="1" value="' + (_Crud.getCollectionPageSize(type, key) || defaultCollectionPageSize) + '">');

		$('.collection-page-size', el).on('blur', function() {
			var newPageSize = $(this).val();
			if (newPageSize !== _Crud.getCollectionPageSize(type, key)) {
				_Crud.setCollectionPageSize(type, key, newPageSize);
				if (callback) {
					callback();
				} else {
					_Crud.refreshList(type);
				}
			}
		});

		$('.collection-page-size', el).on('keypress', function(e) {
			if (e.keyCode === 13) {
				var newPageSize = $(this).val();
				if (newPageSize !== _Crud.getCollectionPageSize(type, key)) {
					_Crud.setCollectionPageSize(type, key, newPageSize);
					if (callback) {
						callback();
					} else {
						_Crud.refreshList(type);
					}
				}
			}
		});
	},
	updateCellPager: function(el, id, type, key, page, pageSize) {
		//console.log('updateCellPager', id, el, type, key, page, pageSize);
		$.ajax({
			url: rootUrl + type + '/' + id + '/' + key + '?page=' + page + '&pageSize=' + pageSize,
			contentType: 'application/json; charset=UTF-8',
			dataType: 'json',
			statusCode: {
				200: function(data) {

					//var resultCount = data.result_count;
					var pageCount   = data.page_count;

					//console.log('page count', pageCount, 'page', page, 'pageSize', pageSize);

					$('.cell-pager .collection-page', el).val(page);
					$('.cell-pager .page-count', el).val(pageCount);

					if (page > 1) {
						$('.cell-pager .prev', el).removeClass('disabled').prop('disabled', '');
					} else {
						$('.cell-pager .prev', el).addClass('disabled').prop('disabled', 'disabled');
					}
					if (page < pageCount) {
						$('.cell-pager .next', el).removeClass('disabled').prop('disabled', '');
					} else {
						$('.cell-pager .next', el).addClass('disabled').prop('disabled', 'disabled');
					}

					el.children('.node').remove();

					data.result.forEach(function(obj) {
						_Crud.getAndAppendNode(type, id, key, obj, el);
					});

				}
			}

		});
	},
	appendCellPager: function(el, id, type, key) {

		// console.log('appendCellPager', id, el, type, key);

		var pageSize = _Crud.getCollectionPageSize(type, key) || defaultCollectionPageSize;

		$.ajax({
			url: rootUrl + type + '/' + id + '/' + key + '?pageSize=' + pageSize,
			contentType: 'application/json; charset=UTF-8',
			dataType: 'json',
			statusCode: {
				200: function(data) {

					var resultCount = data.result_count;
					var pageCount   = data.page_count;

					//console.log('result count', resultCount, 'page count', pageCount, 'page', page, 'pageSize', pageSize);

					//var oldPage = parseInt(_Crud.getCollectionPage(type, key) || 1);
					var page = 1;

					if (!resultCount || !pageCount || pageCount === 1) return;

					//el.append('<div class="collection-cell"></div>');
					el.prepend('<div class="cell-pager"></div>');
					$('.cell-pager', el).append('<button class="prev disabled" disabled>&lt;</button>');
					if (page > 1) {
						$('.cell-pager .prev', el).removeClass('disabled').prop('disabled', '');
					}
					$('.cell-pager', el).append('<input type="text" size="1" class="collection-page" value="' + page + '">');

					$('.collection-page', $('.cell-pager', el)).on('blur', function() {
						var newPage = $(this).val();
						_Crud.updateCellPager(el, id, type, key, newPage, pageSize);
					});

					$('.collection-page', $('.cell-pager', el)).on('keypress', function(e) {
						if (e.keyCode === 13) {
							var newPage = $(this).val();
							_Crud.updateCellPager(el, id, type, key, newPage, pageSize);
						}
					});

					$('.cell-pager', el).append('<button class="next disabled" disabled>&gt;</button>');
					if (page < pageCount) {
						$('.cell-pager .next', el).removeClass('disabled').prop('disabled', '');
					}

					$('.prev', el).on('click', function() {
						var page = $('.cell-pager .collection-page', el).val();
						var newPage = Math.max(1, page - 1);
						_Crud.updateCellPager(el, id, type, key, newPage, pageSize);
					});

					$('.next', el).on('click', function() {
						var page = $('.cell-pager .collection-page', el).val();
						var newPage = parseInt(page) + 1;
						_Crud.updateCellPager(el, id, type, key, newPage, pageSize);
					});

					$('.cell-pager', el).append(' of <input type="text" size="1" readonly class="readonly page-count" value="' + pageCount + '">');

				}

			},
			error:function(jqXHR, textStatus, errorThrown) {
				_Logger.log(_LogType.CRUD, 'appendCellPager', id, el, type, key);
				_Logger.log(_LogType.CRUD, 'Property: ', _Crud.keys[type][key]);
				_Logger.log(_LogType.CRUD, 'Error: ', textStatus, errorThrown, jqXHR.responseJSON);
			}

		});

	},
	sortAndPagingParameters: function(t, s, o, ps, p) {
		//var typeParam = (t ? 'type=' + t : '');
		var sortParam = (s ? 'sort=' + s : '');
		var orderParam = (o ? 'order=' + o : '');
		var pageSizeParam = (ps ? 'pageSize=' + ps : '');
		var pageParam = (p ? 'page=' + p : '');

		//var params = (typeParam ? '?' + typeParam : '');
		var params = '';
		params = params + (sortParam ? (params.length ? '&' : '?') + sortParam : '');
		params = params + (orderParam ? (params.length ? '&' : '?') + orderParam : '');
		params = params + (pageSizeParam ? (params.length ? '&' : '?') + pageSizeParam : '');
		params = params + (pageParam ? (params.length ? '&' : '?') + pageParam : '');

		return params;
	},
	refreshList: function(type, properties) {
		_Crud.clearList(type);
		_Crud.activateList(type, properties);
	},
	activateList: function(type, properties) {
		properties = properties || _Crud.keys[type];
		var url = rootUrl + type + '/ui' + _Crud.sortAndPagingParameters(type, _Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type]);
		_Crud.list(type, properties, url);
		//document.location.hash = type;
	},
	clearList: function(type) {
		var  div = $('#' + type + ' table tbody');
		fastRemoveAllChildren(div[0]);
	},
	list: function(type, properties, url) {
		//_Crud.clearList(type);

		$.ajax({
			headers: {
				Range: _Crud.ranges(type)
			},
			url: url,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			//async: false,
			success: function(data) {
				if (!data) {
					return;
				}
				data.result.forEach(function(item) {
					//console.log('calling appendRow', type, item);
					_Crud.appendRow(type, properties, item);
				});
				_Crud.updatePager(type, data.query_time, data.serialization_time, data.page_size, data.page, data.page_count);
				_Crud.replaceSortHeader(type);
			},
			error: function(a, b, c) {
				console.log(a, b, c, type, url);
				// something went really wrong, probably a network issue, so reload the whole page
//				window.setTimeout(function() {
//					location.reload();
//				}, 1000);
			}
		});
	},
	ranges: function(type) {
		var ranges = '';
		var keys = type && _Crud.keys[type] && Object.keys(_Crud.keys[type]);
		if (!keys) {
			var typeDef = _Crud.type[type];
			if (typeDef) {
				keys = Object.keys(typeDef.views[_Crud.view[type]]);
			}
		}
		if (keys) {
			keys.forEach(function(key) {
				if ( _Crud.isCollection(key, type)) {
					var page = _Crud.getCollectionPage(type, key) || 1;
					var pageSize = _Crud.getCollectionPageSize(type, key) || defaultCollectionPageSize;
					var start = (page-1)*pageSize;
					var end = page*pageSize;
					ranges += key + '=' + start + '-' + end + ';';
				}
			});
			return ranges;
		}
	},
	crudExport: function(type) {
		var url = rootUrl + '/' + $('#' + type).attr('data-url') + '/ui' + _Crud.sortAndPagingParameters(type, _Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type]);

		_Crud.dialog('Export ' + type + ' list as CSV', function() {
		}, function() {
		});
		dialogText.append('<textarea class="exportArea"></textarea>');
		var exportArea = $('.exportArea', dialogText);

		dialogBtn.append('<button id="copyToClipboard">Copy to Clipboard</button>');

		var clipboard = new Clipboard('#copyToClipboard', {
			target: function () {
				new MessageBuilder().success('Copied to clipboard').show();
				return $('.exportArea', dialogText)[0];
			}
		});

		$('.closeButton', $('#dialogBox')).on('click', function() {
			clipboard.destroy();
			$('#copyToClipboard', dialogBtn).remove();
		});

		var keys = Object.keys(_Crud.keys[type]);

		if (keys) {
			$.each(keys, function(k, key) {
				exportArea.append('"' + key + '"');
				if (k < keys.length - 1) {
					exportArea.append(',');
				}
			});
			exportArea.append('\n');
		}
		$.ajax({
			url: url,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			//async: false,
			success: function(data) {
				if (!data)
					return;
				data.result.forEach(function(item) {
					_Crud.appendRowAsCSV(type, item, exportArea);
				});
			}
		});
	},
	crudImport: function(type) {

		var url = csvRootUrl + $('#' + type).attr('data-url');

		_Crud.dialog('Import CSV data for type ' + type + '', function() {
		}, function() {
		});
		dialogText.append('<textarea class="importArea"></textarea>');
		var importArea = $('.importArea', dialogText);

		window.setTimeout(function() {
			importArea.focus();
		}, 200);

		dialogBtn.append('<button id="startImport">Start Import</button>');

		$('#startImport', dialogBtn).on('click', function() {

			$.ajax({
				url: url,
				dataType: 'json',
				contentType: 'text/csv; charset=utf-8',
				method: 'POST',
				data: importArea.val().split('\n').map($.trim).filter(function(line) { return line !== '' }).join('\n'),
				success: function(data) {
					//console.log(data);
					_Crud.refreshList(type);
				}
			});

		});

		$('.closeButton', $('#dialogBox')).on('click', function() {
			$('#startImport', dialogBtn).remove();
		});

	},
	updatePager: function(type, qt, st, ps, p, pc) {
		var typeNode = $('#' + type);
		$('.queryTime', typeNode).text(qt);
		$('.serTime', typeNode).text(st);
		$('.pageSize', typeNode).val(ps);

		_Crud.page[type] = p;
		$('.page', typeNode).val(_Crud.page[type]);

		_Crud.pageCount = pc;
		$('.pageCount', typeNode).val(_Crud.pageCount);

		var pageLeft = $('.pageLeft', typeNode);
		var pageRight = $('.pageRight', typeNode);

		if (_Crud.page[type] < 2) {
			pageLeft.attr('disabled', 'disabled').addClass('disabled');
		} else {
			pageLeft.removeAttr('disabled').removeClass('disabled');
		}

		if (!_Crud.pageCount || _Crud.pageCount === 0 || (_Crud.page[type] === _Crud.pageCount)) {
			pageRight.attr('disabled', 'disabled').addClass('disabled');
		} else {
			pageRight.removeAttr('disabled').removeClass('disabled');
		}

		_Crud.updateUrl(type);
	},
	activatePagerElements: function(type, pagerNode) {

		$('.page', pagerNode).on('keypress', function(e) {
			if (e.keyCode === 13) {
				_Crud.page[type] = $(this).val();
				_Crud.refreshList(type);
			}
		});

		$('.pageSize', pagerNode).on('keypress', function(e) {
			if (e.keyCode === 13) {
				// calculate which page we should be on after the pagesize changed
				var oldFirstObject = ((_Crud.page[type] -1 ) * _Crud.pageSize[type]) + 1;
				var newPage = Math.ceil(oldFirstObject / $(this).val());
				_Crud.pageSize[type] = $(this).val();
				_Crud.page[type] = newPage;
				_Crud.refreshList(type);
			}
		});

		var pageLeft = $('.pageLeft', pagerNode);
		var pageRight = $('.pageRight', pagerNode);

		pageLeft.on('click', function() {
			pageRight.removeAttr('disabled').removeClass('disabled');
			if (_Crud.page[type] > 1) {
				_Crud.page[type]--;
				_Crud.refreshList(type);
			}
		});

		pageRight.on('click', function() {
			pageLeft.removeAttr('disabled').removeClass('disabled');
			if (_Crud.page[type] < _Crud.pageCount) {
				_Crud.page[type]++;
				_Crud.refreshList(type);
			}
		});

	},
	deActivatePagerElements: function(pagerNode) {

		$('.page', pagerNode).off('keypress');
		$('.pageSize', pagerNode).off('keypress');
		$('.pageLeft', pagerNode).off('click');
		$('.pageRight', pagerNode).off('click');
	},
	crudRead: function(type, id, callback, errorCallback) {
		// use 'ui' view as default to make the 'edit by id' feature work
		var view = (type && _Crud.view[type] ? _Crud.view[type] : 'ui');
		var url = rootUrl + id + '/' + view;
		$.ajax({
			url: url,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			//async: false,
			success: function(data) {
				if (!data)
					return;
				if (callback) {
					callback(data.result);
				} else {
					_Crud.appendRow(type, data.result);
				}
			},
			error: function(data) {
				if (errorCallback) {
					errorCallback(data);
				}
			}
		});
	},
	crudEdit: function(id, type) {
		var t = type || _Crud.type;

		$.ajax({
			url: rootUrl + id + (_Crud.view[t] ? '/' + _Crud.view[t] : ''),
			type: 'GET',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			//async: false,
			success: function(data) {
				if (!data)
					return;
				//console.log('type', type);
				_Crud.dialog('Edit ' + t + ' ' + id, function() {
					//console.log('ok')
				}, function() {
					//console.log('cancel')
				});
				_Crud.showDetails(data.result, t);
				//_Crud.populateForm($('#entityForm'), data.result);
			}
		});
	},
	crudCreate: function(type, url, json, onSuccess, onError) {

		url = url || type;
		$.ajax({
			url: rootUrl + url,
			type: 'POST',
			dataType: 'json',
			data: json,
			contentType: 'application/json; charset=utf-8',
			//async: false,
			statusCode: {
				201: function(xhr) {
					if (onSuccess) {
						onSuccess();
					} else {
						$.unblockUI({
							fadeOut: 25
						});
						_Crud.refreshList(type);
					}
					if (dialogCloseButton) {
						dialogCloseButton.remove();
					}
					$('#saveProperties').remove();
				},
				400: function(data, status, xhr) {
					_Crud.error('Bad request: ' + data.responseText, true);
					_Crud.showCreateError(type, data, onError);
				},
				401: function(data, status, xhr) {
					_Crud.error('Authentication required: ' + data.responseText, true);
					_Crud.showCreateError(type, data, onError);
				},
				403: function(data, status, xhr) {
					_Crud.error('Forbidden: ' + data.responseText, true);
					_Crud.showCreateError(type, data, onError);
				},
				404: function(data, status, xhr) {
					_Crud.error('Not found: ' + data.responseText, true);
					_Crud.showCreateError(type, data, onError);
				},
				422: function(data, status, xhr) {
					_Crud.showCreateError(type, data, onError);
				},
				500: function(data, status, xhr) {
					_Crud.error('Internal Error: ' + data.responseText, true);
					_Crud.showCreateError(type, data, onError);
				}
			}
		});
	},
	showCreateError: function(type, data, onError) {
		if (onError) {
			onError();
		} else {

			if (!dialogBox.is(':visible')) {
				_Crud.showCreate(null, type);
			}
			var resp = JSON.parse(data.responseText);

			$('.props input', dialogBox).css({
				backgroundColor: '#fff',
				borderColor: '#a5a5a5'
			});

			$('.props textarea', dialogBox).css({
				backgroundColor: '#fff',
				borderColor: '#a5a5a5'
			});

			$('.props td.value', dialogBox).css({
				backgroundColor: '#fff',
				borderColor: '#b5b5b5'
			});

			$.each(resp.errors, function(i, error) {

                var key = error.property;
                var errorMsg = error.token;

				var input = $('td [name="' + key + '"]', dialogText);
				if (input.length) {

					var errorText = '';
                    errorText += '"' + key + '" ' + errorMsg.replace(/_/gi, ' ');

                    if (error.detail) {
                        errorText += ' ' + error.detail;
                    }

					dialogMsg.html('<div class="infoBox error">' + errorText + '</div>');
					$('.infoBox', dialogMsg).delay(2000).fadeOut(1000);

					input.css({
						backgroundColor: '#fee',
						borderColor: '#933'
					});
				}
			});
		}
	},
	crudRefresh: function(id, key, oldValue) {
		var url = rootUrl + id + '/' + _Crud.view[_Crud.type];

		$.ajax({
			url: url,
			type: 'GET',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			//async: false,
			success: function(data) {
				if (!data)
					return;

				if (key) {
					_Crud.refreshCell(id, key, data.result[key], data.result.type, oldValue);
				} else {
					_Crud.refreshRow(id, data.result, data.result.type);
				}
			}
		});
	},
	crudReset: function(id, key) {
		$.ajax({
			url: rootUrl + id + '/' + _Crud.view[_Crud.type],
			type: 'GET',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			//async: false,
			success: function(data) {
				if (!data)
					return;

				_Crud.resetCell(id, key, data.result[key]);
			}
		});
	},
	crudUpdateObj: function(id, json, onSuccess, onError) {
		$.ajax({
			url: rootUrl + id,
			data: json,
			type: 'PUT',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			//async: false,
			statusCode: {
				200: function() {
					if (onSuccess) {
						onSuccess();
					} else {
						_Crud.crudRefresh(id);
					}
				},
				400: function(data, status, xhr) {
					_Crud.error('Bad request: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id);
					}
				},
				401: function(data, status, xhr) {
					_Crud.error('Authentication required: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id);
					}
				},
				403: function(data, status, xhr) {
					console.log(data, status, xhr);
					_Crud.error('Forbidden: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id);
					}
				},
				404: function(data, status, xhr) {
					_Crud.error('Not found: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id);
					}
				},
				422: function(data, status, xhr) {
					_Crud.error('Error: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id);
					}
				},
				500: function(data, status, xhr) {
					_Crud.error('Internal Error: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id);
					}
				}
			}
		});
	},
	crudUpdate: function(id, key, newValue, oldValue, onSuccess, onError) {
		var url = rootUrl + id;

		var obj = {};
		if (newValue && newValue !== '') {
			obj[key] = newValue;
		} else {
			obj[key] = null;
		}

		$.ajax({
			url: url,
			data: JSON.stringify(obj),
			type: 'PUT',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function() {
					if (onSuccess) {
						onSuccess();
					} else {
						_Crud.crudRefresh(id, key, oldValue);
					}
				},
				400: function(data, status, xhr) {
					_Crud.error('Bad request: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id, key);
					}
				},
				401: function(data, status, xhr) {
					_Crud.error('Authentication required: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id, key);
					}
				},
				403: function(data, status, xhr) {
					console.log(data, status, xhr);
					_Crud.error('Forbidden: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id, key);
					}
				},
				404: function(data, status, xhr) {
					_Crud.error('Not found: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id, key);
					}
				},
				422: function(data, status, xhr) {
					_Crud.error('Error: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id, key);
					}
				},
				500: function(data, status, xhr) {
					_Crud.error('Internal Error: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id, key);
					}
				}
			}
		});
	},
	crudRemoveProperty: function(id, key, onSuccess, onError) {
		var url = rootUrl + id;
		var obj = {};
		obj[key] = null;
		$.ajax({
			url: url,
			data: JSON.stringify(obj),
			type: 'PUT',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			//async: false,
			statusCode: {
				200: function() {
					if (onSuccess) {
						onSuccess();
					} else {
						_Crud.crudRefresh(id, key);
					}
				},
				204: function() {
					if (onSuccess) {
						onSuccess();
					} else {
						_Crud.crudRefresh(id, key);
					}
				},
				400: function(data, status, xhr) {
					_Crud.error('Bad request: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id, key);
					}
				},
				401: function(data, status, xhr) {
					_Crud.error('Authentication required: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id, key);
					}
				},
				403: function(data, status, xhr) {
					_Crud.error('Forbidden: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id, key);
					}
				},
				404: function(data, status, xhr) {
					_Crud.error('Not found: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id, key);
					}
				},
				422: function(data, status, xhr) {
					_Crud.error('Error: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id, key);
					}
				},
				500: function(data, status, xhr) {
					_Crud.error('Internal Error: ' + data.responseText, true);
					if (onError) {
						onError();
					} else {
						_Crud.crudReset(id, key);
					}
				}
			}
		});
	},
	crudDelete: function(id) {
		$.ajax({
			url: rootUrl + '/' + id,
			type: 'DELETE',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			//async: false,
			statusCode: {
				200: function() {
					var row = _Crud.row(id);
					row.remove();
				},
				204: function() {
					var row = _Crud.row(id);
					row.remove();
				},
				400: function(data, status, xhr) {
					_Crud.error('Bad request: ' + data.responseText, true);
				},
				401: function(data, status, xhr) {
					_Crud.error('Authentication required: ' + data.responseText, true);
				},
				403: function(data, status, xhr) {
					console.log(data, status, xhr);
					_Crud.error('Forbidden: ' + data.responseText, true);
				},
				404: function(data, status, xhr) {
					_Crud.error('Not found: ' + data.responseText, true);
				},
				422: function(data, status, xhr) {
				},
				500: function(data, status, xhr) {
					_Crud.error('Internal Error: ' + data.responseText, true);
				}
			}
		});
	},
	populateForm: function(form, node) {
		//console.log(form, node);
		var fields = $('input', form);
		form.attr('data-id', node.id);
		$.each(fields, function(f, field) {
			var value = formatValue(node[field.name], field.name, node.type, node.id);
			//console.log(field, field.name, value);
			$('input[name="' + field.name + '"]').val(value);
		});
	},
	cells: function(id, key) {
		var row = _Crud.row(id);
		var cellInMainTable = $('.___' + key, row);

		var cellInDetailsTable;
		var table = $('#details_' + id);
		if (table) {
			cellInDetailsTable = $('.___' + key, table);
		}

		if (cellInMainTable && cellInMainTable.length && !(cellInDetailsTable && cellInDetailsTable.length)) {
			return [cellInMainTable];
		}

		if (cellInDetailsTable && cellInDetailsTable.length && !(cellInMainTable && cellInMainTable.length)) {
			return [cellInDetailsTable];
		}

		return [cellInMainTable, cellInDetailsTable];
	},
	resetCell: function(id, key, oldValue) {
		var cells = _Crud.cells(id, key);

		$.each(cells, function(i, cell) {
			cell.empty();
			_Crud.populateCell(id, key, _Crud.type, oldValue, cell);
		});
	},
	refreshCell: function(id, key, newValue, type, oldValue) {
		var cells = _Crud.cells(id, key);
		$.each(cells, function(i, cell) {
			cell.empty();
			_Crud.populateCell(id, key, type, newValue, cell);
			if (newValue !== oldValue && !(!newValue && oldValue === '')) {
				blinkGreen(cell);
			}
		});
	},
	refreshRow: function(id, item, type) {
		var row = _Crud.row(id);
		row.empty();
		_Crud.populateRow(id, item, type, _Crud.keys[type]);
	},
	activateTextInputField: function(el, id, key, propertyType) {
		var oldValue = el.text();
		el.off('click');
		var w = el.width(), h = el.height();
		var input;
		if (propertyType === 'String') {
			el.html('<textarea name="' + key + '" class="__value"></textarea>');
			input = $('textarea', el);
			input.width(w);
			input.height(h);
		} else {
			el.html('<input name="' + key + '" class="__value" type="text" size="10">');
			input = $('input', el);
		}
		input.val(oldValue);
		input.off('click');
		input.focus();
		input.on('blur', function() {
			var newValue = input.val();
			if (id) {
				_Crud.crudUpdate(id, key, newValue, oldValue);
			}
		});
	},
	row: function(id) {
		return $('tr._' + id);
	},
	appendRow: function(type, properties, item) {
		if (!_Crud.keys[item.type]) {
			_Crud.getProperties(item.type);
		}
		var id = item['id'];
		var tbody = $('#' + type + ' table tbody');
		tbody.append('<tr class="_' + id + '"></tr>');
		_Crud.populateRow(id, item, type, properties);
	},
	populateRow: function(id, item, type, properties) {
		var row = _Crud.row(id);
		if (properties) {
			_Crud.filterKeys(type, Object.keys(properties)).forEach(function(key) {
				//console.log('populateRow for key', key, row);
				row.append('<td class="___' + key + '"></td>');
				var cells = _Crud.cells(id, key);
				$.each(cells, function(i, cell) {
					_Crud.populateCell(id, key, type, item[key], cell);
				});
			});
			row.append('<td class="actions"><a title="Edit" class="edit"><img alt="Edit Icon" src="' + _Icons.edit_icon + '"></a><a title="Delete" class="delete"><img alt="Delete Icon" src="' + _Icons.cross_icon + '"></a><a title="Access Control" class="security"><img alt="Access Control Icon" src="' + _Icons.key_icon + '"></a></td>');
			_Crud.resize();

			$('.actions .edit', row).on('click', function(event) {
				event.preventDefault();
				_Crud.crudEdit(id);
				return false;
			});
			$('.actions .delete', row).on('click', function(event) {
				event.preventDefault();
				var c = confirm('Are you sure to delete ' + type + ' ' + id + ' ?');
				if (c === true) {
					_Crud.crudDelete(id);
				}
			});
			_Entities.bindAccessControl($('.actions .security', row), id);
		}
	},
	populateCell: function(id, key, type, value, cell) {
		var isCollection = _Crud.isCollection(key, type);
		var isEnum = _Crud.isEnum(key, type);
		var relatedType = _Crud.relatedType(key, type);
		var readOnly = _Crud.readOnly(key, type);
		var simpleType;
		var isSourceOrTarget = _Crud.types[type].isRel && (key === 'sourceId' || key === 'targetId');

		if (readOnly) {
			cell.addClass('readonly');
		}

		if (!isSourceOrTarget && !relatedType) {

			if (!_Crud.keys[type]) {
				return;
			}

			var propertyType = _Crud.keys[type][key].type;

			if (propertyType === 'Boolean') {
				cell.append('<input name="' + key + '" ' + (readOnly ? 'class="readonly" readonly disabled ' : '') + 'type="checkbox" ' + (value ? 'checked="checked"' : '') + '>');
				if (!readOnly) {
					$('input', cell).on('change', function() {
						if (id) {
							_Crud.crudUpdate(id, key, $(this).prop('checked').toString());
						}
					});
				}
			} else if (propertyType === 'Date') {
				cell.html(nvl(formatValue(value), '<img alt="Show calendar" title="Show calendar" src="' + _Icons.calendar_icon + '">'));
				if (!readOnly) {
					var dateTimePickerFormat = getDateTimePickerFormat(_Crud.getFormat(key, type));
					cell.on('click', function(event) {
						event.preventDefault();
						var self = $(this);
						var oldValue = self.text();
						self.html('<input name="' + key + '" class="__value" type="text" size="40">');
						var input = $('input', self);
						input.val(oldValue);

						if (dateTimePickerFormat.timeFormat) {
							input.datetimepicker({
								parse: 'loose',
								dateFormat: dateTimePickerFormat.dateFormat,
								timeFormat: dateTimePickerFormat.timeFormat,
								separator: dateTimePickerFormat.separator,
								onClose: function() {
									var newValue = input.val();
									if (id && newValue !== oldValue) {
										_Crud.crudUpdate(id, key, newValue);
									} else {
										_Crud.resetCell(id, key, oldValue);
									}
								}
							});
							input.datetimepicker('show');
						} else {
							input.datepicker({
								parse: 'loose',
								dateFormat: dateTimePickerFormat.dateFormat,
								onClose: function() {
									var newValue = input.val();
									if (id && newValue !== oldValue) {
										_Crud.crudUpdate(id, key, newValue);
									} else {
										_Crud.resetCell(id, key, oldValue);
									}
								}
							});
							input.datepicker('show');
						}
						self.off('click');
					});
				}
			} else if (isEnum) {
				var format = _Crud.getFormat(key, type);
				cell.text(nvl(formatValue(value), ''));
				if (!readOnly) {
					cell.on('click', function(event) {
						event.preventDefault();
						_Crud.appendEnumSelect(cell, id, key, format);
					});
					if (!id) { // create
						_Crud.appendEnumSelect(cell, id, key, format);
					}
				}

			} else if (propertyType === 'String[]') {

				if (value && value.length) {
					value.forEach(function (v, i) {
						cell.append('<div title="' + v + '" class="node stringarray">' + fitStringToWidth(v, 80) + '<img class="remove" src="' + _Icons.grey_cross_icon + '" data-string-element="' + escape(v) + '" data-string-position="' + i + '"></div>');
					});

					$('.stringarray .remove', cell).on('click', function(e) {
						e.preventDefault();
						_Crud.removeStringFromArray(type, id, key, $(this).data('string-element'), $(this).data('string-position'));
						return false;
					});

				}

				cell.append('<img class="add" src="' + _Icons.add_grey_icon + '">');
				$('.add', cell).on('click', function() {
					var newStringElement = window.prompt("Enter new string");

					if (newStringElement !== null) {
						_Crud.addStringToArray(type, id, key, newStringElement);
					}
				});

			} else {
				cell.text(nvl(formatValue(value), ''));
				if (!readOnly) {
					cell.on('click', function(event) {
						event.preventDefault();
						var self = $(this);
						_Crud.activateTextInputField(self, id, key, propertyType);
					});
					if (!id) { // create
						_Crud.activateTextInputField(cell, id, key, propertyType);
					}
				}
			}

		} else if (isCollection) {

			simpleType = lastPart(relatedType, '.');

			if (value && value.length) {
				value.forEach(function(relatedId) {
					_Crud.getAndAppendNode(type, id, key, relatedId, cell);
				});
			}

			cell.append('<img class="add" src="' + _Icons.add_grey_icon + '">');
			$('.add', cell).on('click', function() {
				_Crud.dialog('Add ' + simpleType, function() {
				}, function() {
				});
				_Crud.displaySearch(type, id, key, simpleType, dialogText);
			});

			if (_Crud.keys[type][key] && _Crud.keys[type][key].className.indexOf('CollectionIdProperty') === -1 && _Crud.keys[type][key].className.indexOf("CollectionNotionProperty") === -1) {

				_Crud.appendCellPager(cell, id, type, key);

			}

		} else {

			simpleType = lastPart(relatedType, '.');

			if (value) {

				_Crud.getAndAppendNode(type, id, key, value, cell);

			} else {

				cell.append('<img class="add" src="' + _Icons.add_grey_icon + '">');
				$('.add', cell).on('click', function() {
					_Crud.dialog('Add ' + simpleType + ' to ' + key, function() {
					}, function() {
					});
					_Crud.displaySearch(type, id, key, simpleType, dialogText);
				});
			}

		}

		if (!isSourceOrTarget && !readOnly && !relatedType && propertyType !== 'Boolean' && propertyType !== 'String[]') {
			cell.append('<img class="crud-clear-value" alt="Clear value" title="Clear value" src="' + _Icons.grey_cross_icon + '">');
			$('.crud-clear-value', cell).on('click', function(e) {
				e.preventDefault();
				_Crud.crudRemoveProperty(id, key);
				return false;
			});
		}


		//searchField.focus();
	},
	appendEnumSelect: function(cell, id, key, format) {
		cell.off('click');
		var input;
		var oldValue = cell.text();
		cell.empty().append('<select name="' + key + '">');
		input = $('select', cell);
		input.focus();
		var values = format.split(',');
		input.append('<option></option>');
		values.forEach(function(value) {
			value = value.trim();
			if (value.length > 0) {
				input.append('<option ' + (value === oldValue ? 'selected="selected"' : '') + 'value="' + value + '">' + value + '</option>');
			}
		});
		input.on('change', function() {
			var newValue = input.val();
			if (id) {
				_Crud.crudUpdate(id, key, newValue, oldValue);
			}
		});
		input.on('blur', function() {
			_Crud.resetCell(id, key, oldValue);
		});
	},
	getAndAppendNode: function(parentType, parentId, key, obj, cell) {
		//console.log(parentType, parentId, key, obj, cell);
		if (!obj) {
			return;
		}
		var id;
		if ((typeof obj) === 'object') {
			id = obj.id;
		} else if (isUUID(obj)) {
			id = obj;
		} else {
			// search object by name
			var type = _Crud.keys[parentType][key].relatedType.split('.').pop();

			$.ajax({
				url: rootUrl + type + '?name=' + obj,
				type: 'GET',
				dataType: 'json',
				contentType: 'application/json; charset=utf-8;',
				//async: false,
				success: function(data) {
					if (data.result.length > 0) {
						_Crud.getAndAppendNode(parentType, parentId, key, data.result[0], cell);
					}
				}
			});

			return;
		}

		$.ajax({
			url: rootUrl + id + '/ui',
			type: 'GET',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8;',
			headers: {
				Accept: 'application/json; charset=utf-8; properties=id,name,type,contentType,isThumbnail,isImage,tnSmall,tnMid'
			},
			//async: false,
			success: function(data) {
				if (!data)
					return;
				var node = data.result;
				//console.log('node', node);

				var displayName = _Crud.displayName(node);

				cell.append('<div title="' + displayName + '" id="_' + node.id + '" class="node ' + (node.isImage? 'image ' : '') + (node.type ? node.type.toLowerCase() : (node.tag ? node.tag : 'element')) + ' ' + node.id + '_">' + fitStringToWidth(displayName, 80));
				var nodeEl = $('#_' + node.id, cell);

				//console.log('Schema types', _Crud.types);

				var isSourceOrTarget = _Crud.types[parentType].isRel && (key === 'sourceId' || key === 'targetId');
				if (!isSourceOrTarget) {
					nodeEl.append('<img class="remove" src="' + _Icons.grey_cross_icon + '"></div>');
				}

				//console.log(node);
				if (node.isImage) {

					if (node.isThumbnail) {
						nodeEl.prepend('<div class="wrap"><img class="thumbnail" src="/' + node.id + '"></div>');
					} else if (node.tnSmall) {
						nodeEl.prepend('<div class="wrap"><img class="thumbnail" src="/' + node.tnSmall.id + '"></div>');
					} else if (node.contentType === 'image/svg+xml') {
						nodeEl.prepend('<div class="wrap"><img class="thumbnail" src="/' + node.id + '"></div>');
					}

					$('.thumbnail', nodeEl).on('click', function(e) {
						e.stopPropagation();
						e.preventDefault();
						_Crud.showDetails(node, node.type);
						return false;
					});

					if (node.tnMid || node.contentType === 'image/svg+xml') {
						$('.thumbnail', nodeEl).on('mouseenter', function(e) {
							e.stopPropagation();
							$('.thumbnailZoom').remove();
							nodeEl.parent().append('<img class="thumbnailZoom" src="/' + (node.tnMid ? node.tnMid.id : node.id) + '">');
							var tnZoom = $($('.thumbnailZoom', nodeEl.parent())[0]);
							tnZoom.css({
								top: (nodeEl.position().top) + 'px',
								left: (nodeEl.position().left - 42) + 'px'
							});
							tnZoom.on('mouseleave', function(e) {
								e.stopPropagation();
								$('.thumbnailZoom').remove();
							});
						});
					}
				}

				$('.remove', nodeEl).on('click', function(e) {
					e.preventDefault();
					Command.get(parentId, function(parentObj) {
						_Crud.removeRelatedObject(parentObj, key, obj);
					});
					return false;
				});
				nodeEl.on('click', function(e) {
					e.preventDefault();
					_Crud.showDetails(node, node.type);
					return false;
				});
			}
		});

	},
	clearSearch: function(el) {
		if (_Crud.clearSearchResults(el)) {
			$('.clearSearchIcon').hide().off('click');
			$('.search').val('');
			$('#resourceTabs', main).show();
			$('#resourceBox', main).show();
		}
	},
	clearSearchResults: function(el) {
		var searchResults = $('.searchResults', el);
		if (searchResults.length) {
			searchResults.remove();
			$('.searchResultsTitle').remove();
			return true;
		}
		return false;
	},
	/**
	 * Conduct a search and append search results to 'el'.
	 *
	 * If an optional type is given, restrict search to this type.
	 */
	search: function(searchString, el, type, onClickCallback) {

		_Crud.clearSearchResults(el);

		el.append('<h2 class="searchResultsTitle">Search Results</h2>');
		el.append('<div class="searchResults"></div>');
		var searchResults = $('.searchResults', el);

		_Crud.resize();
		//$('.search').select();

		//var types = type ? [ type ] : _Crud.types;
		var types;
		var attr = 'name';
		var posOfColon = searchString.indexOf(':');
		if (posOfColon > -1) {
			var typeAndValue = searchString.split(':');
			var type = typeAndValue[0];
			var posOfDot = type.indexOf('.');
			if (posOfDot > -1) {
				var typeAndAttr = type.split('.');
				type = typeAndAttr[0];
				attr = typeAndAttr[1];
			}
			types = [type.capitalize()];
			searchString = typeAndValue[1];
			//console.log('filter search type', types, attr, searchString);
		} else {
			types = type ? [type] : Object.keys(_Crud.types);
			if (searchString.match(/[0-9a-f]{32}/)) {
				attr = 'uuid'; // UUID
				//types = ['']; // will be ignored anyway
				//console.log('UUID detected', searchString);
			}
		}

		types.forEach(function(type) {
			var url, searchPart;
			if (attr === 'uuid') {
				url = rootUrl + type + '/' + searchString;
			} else {
				searchPart = searchString === '*' || searchString === '' ? '' : '&' + attr + '=' + encodeURIComponent(searchString) + '&loose=1';
				url = rootUrl + type + '/ui' + _Crud.sortAndPagingParameters(type, 'name', 'asc', 1000, 1) + searchPart;
			}

			searchResults.append('<div id="placeholderFor' + type + '" class="searchResultGroup resourceBox"><img class="loader" src="' + _Icons.ajax_loader_1 + '">Searching for "' + searchString + '" in ' + type + '</div>');

			//console.log('Search URL', url)

			$.ajax({
				url: url,
				type: 'GET',
				dataType: 'json',
				contentType: 'application/json; charset=utf-8',
				statusCode: {
					200: function(data) {
						if (!data || !data.result) {
							return;
						}
						var result = data.result;
						//console.log(result);
						$('#placeholderFor' + type + '').remove();

						if (result) {
							if (Array.isArray(result)) {
								if (result.length) {
									$.each(result, function(i, node) {
										_Crud.searchResult(searchResults, type, node, onClickCallback);
									});
								} else {
									_Crud.noResults(searchResults, type);
								}
							} else if (result.id) {
								_Crud.searchResult(searchResults, type, result, onClickCallback);
							}
						} else {
							_Crud.noResults(searchResults, type);
						}

					},
					400: function() {
						$('#placeholderFor' + type + '').remove();
					},
					401: function() {
						$('#placeholderFor' + type + '').remove();
					},
					403: function() {
						$('#placeholderFor' + type + '').remove();
					},
					404: function() {
						$('#placeholderFor' + type + '').remove();
					},
					422: function() {
						$('#placeholderFor' + type + '').remove();
					},
					500: function() {
						$('#placeholderFor' + type + '').remove();
					},
					503: function() {
						$('#placeholderFor' + type + '').remove();
					}
				}
			});

		});
	},
	noResults: function(searchResults, type) {
		searchResults.append('<div id="resultsFor' + type + '" class="searchResultGroup resourceBox">No results for ' + type.capitalize() + '</div>');
		//console.log('noResults', 'resultsFor' + type, searchResults, $('#resultsFor' + type));
		window.setTimeout(function() {
			$('#resultsFor' + type).fadeOut('fast');
		}, 1000);
	},
	searchResult: function(searchResults, type, node, onClickCallback) {
		if (!$('#resultsFor' + type).length) {
			searchResults.append('<div id="resultsFor' + type + '" class="searchResultGroup resourceBox"><h3>' + type.capitalize() + '</h3></div>');
		}
		var displayName = _Crud.displayName(node);
		$('#resultsFor' + type, searchResults).append('<div title="' + displayName + '" " class="_' + node.id + ' node">' + fitStringToWidth(displayName, 120) + '</div>');

		var nodeEl = $('#resultsFor' + type + ' ._' + node.id, searchResults);
		//console.log(node);
		if (node.isImage) {
			nodeEl.append('<div class="wrap"><img class="thumbnail" src="/' + node.id + '"></div>');
		}

		nodeEl.on('click', function(e) {
			onClickCallback(e, node);
		});
	},
	displayName: function(node) {
		var displayName;
		if (node.isContent && node.content) {
			//displayName = $(node.content).text().substring(0, 100);
			displayName = escapeTags(node.content.substring(0, 100));
		} else {
			displayName = node.name || node.id || node;
		}
		return displayName;
	},
	displaySearch: function(parentType, id, key, type, el) {
		el.append('<div class="searchBox searchBoxDialog"><input class="search" name="search" size="20" placeholder="Search"><img class="clearSearchIcon" src="' + _Icons.grey_cross_icon + '"></div>');
		var searchBox = $('.searchBoxDialog', el);
		var search = $('.search', searchBox);
		window.setTimeout(function() {
			search.focus();
		}, 250);
		search.keyup(function(e) {
			e.preventDefault();

			var searchString = $(this).val();
			if (searchString && searchString.length && e.keyCode === 13) {

				$('.clearSearchIcon', searchBox).show().on('click', function() {
					if (_Crud.clearSearchResults(el)) {
						$('.clearSearchIcon').hide().off('click');
						search.val('');
						search.focus();
					}
				});

				_Crud.search(searchString, el, type, function(e, node) {
					e.preventDefault();
					_Crud.addRelatedObject(parentType, id, key, node, function() {
						//document.location.reload();
					});
					return false;
				});

			} else if (e.keyCode === 27) {

				if (!searchString || searchString === '') {
					dialogCancelButton.click();
				}

				if (_Crud.clearSearchResults(el)) {
					$('.clearSearchIcon').hide().off('click');
					search.val('');
					search.focus();
				} else {
					search.val('');
				}

			}

			return false;

		});
	},
	removeRelatedObject: function(obj, key, relatedObj, callback) {
		var type = obj.type;
		var url = rootUrl + type + '/' + obj.id + '/ui';
		if (_Crud.isCollection(key, type)) {
			$.ajax({
				url: url,
				type: 'GET',
				dataType: 'json',
				contentType: 'application/json; charset=utf-8',
				success: function(data) {

					var objects = _Crud.extractIds(data.result[key]);
					var relatedId = (typeof relatedObj === 'object' ? relatedObj.id : relatedObj);
					objects = objects.filter(function(obj) {
						return obj.id !== relatedId;
					});
					var json = '{"' + key + '":' + JSON.stringify(objects) + '}';
					_Crud.crudUpdateObj(obj.id, json, function() {
						_Crud.crudRefresh(obj.id, key);
						if (callback) {
							callback();
						}
					});
				}
			});
		} else {
			Command.get(obj.id, function(data) {
				_Crud.crudRemoveProperty(data.id, key);
			});
		}
	},
	addRelatedObject: function(type, id, key, relatedObj, callback) {
		var url = rootUrl + type + '/' + id + '/ui';
		if (_Crud.isCollection(key, type)) {
			$.ajax({
				url: url,
				type: 'GET',
				dataType: 'json',
				contentType: 'application/json; charset=utf-8',
				success: function(data) {
					var objects = _Crud.extractIds(data.result[key]);
					if (!isIn(relatedObj.id, objects)) {
						objects.push({'id': relatedObj.id});
					}
					var json = '{"' + key + '":' + JSON.stringify(objects) + '}';
					_Crud.crudUpdateObj(id, json, function() {
						_Crud.crudRefresh(id, key);
						if (callback) {
							callback();
						}
					});
				}
			});
		} else {
			_Crud.crudUpdateObj(id, '{"' + key + '":' + JSON.stringify({'id': relatedObj.id}) + '}', function() {
				_Crud.crudRefresh(id, key);
				dialogCancelButton.click();
			});
		}
	},
	removeStringFromArray: function(type, id, key, obj, pos, callback) {
		var url = rootUrl + '/' + id + '/ui';
		$.ajax({
			url: url,
			type: 'GET',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			success: function(data) {
				obj = unescape(obj);
				var newData = {};
				var curVal = data.result[key];

				if (curVal[pos] === obj) {
					// string is at expected position => just remove that position/element
					curVal.splice(pos, 1);
					newData[key] = curVal;
				} else {
					// obj is not at position. it seems the crud is not up to date => remove the first occurence of the string (if any)
					var newVal = [];
					var found = false;
					curVal.forEach(function (v) {
						if (v === obj && !found) {
							found = true;
						} else {
							newVal.push(v);
						}
					});
					newData[key] = newVal;
				}

				_Crud.crudUpdateObj(id, JSON.stringify(newData), function() {
					_Crud.crudRefresh(id, key);
					if (callback) {
						callback();
					}
				});
			}
		});
	},
	addStringToArray: function(type, id, key, obj, callback) {
		var url = rootUrl + '/' + id + '/ui';
		$.ajax({
			url: url,
			type: 'GET',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			success: function(data) {
				var curVal = data.result[key];
				if (curVal === null) {
					curVal = [obj];
				} else {
					curVal.push(obj);
				}

				var data = {};
				data[key] = curVal;
				_Crud.crudUpdateObj(id, JSON.stringify(data), function() {
					_Crud.crudRefresh(id, key);
					if (callback) {
						callback();
					}
				});
			}
		});
	},
	extractIds: function(result) {
		var objects = [];
		result.forEach(function(obj) {
			// value can be an ID string or an object
			if (typeof obj === 'object') {
				objects.push({'id': obj.id});
			} else {
				objects.push(obj);
			}
		});
		return objects;
	},
	appendRowAsCSV: function(type, item, textArea) {
		var keys = Object.keys(_Crud.keys[type]);
		if (keys) {
			//console.log(type);
			$.each(keys, function(k, key) {
				textArea.append('"' + nvl(item[key], '') + '"');
				if (k < keys.length - 1) {
					textArea.append(',');
				}
			});
			textArea.append('\n');
		}
	},
	dialog: function(text, callbackOk, callbackCancel) {

		if (browser) {

			dialogHead.empty();
			dialogText.empty();
			dialogMsg.empty();
			dialogMeta.empty();
			//dialogBtn.empty();

			if (text) {
				dialogTitle.html(text);
			}

			if (callbackCancel) {
				dialogCancelButton.off('click').on('click', function(e) {
					e.stopPropagation();
					callbackCancel();
					dialogText.empty();
					$.unblockUI({
						fadeOut: 25
					});
					if (dialogCloseButton) {
						dialogCloseButton.remove();
					}
					$('#saveProperties').remove();
					searchField.focus();
				});
			}
			$.blockUI.defaults.overlayCSS.opacity = .6;
			$.blockUI.defaults.applyPlatformOpacityRules = false;
			$.blockUI.defaults.css.cursor = 'default';


			var w = $(window).width();
			var h = $(window).height();

			var ml = 0;
			var mt = 24;

			// Calculate dimensions of dialog
			var dw = Math.min(900, w - ml);
			var dh = Math.min(600, h - mt);
			//            var dw = (w-24) + 'px';
			//            var dh = (h-24) + 'px';

			var l = parseInt((w - dw) / 2);
			var t = parseInt((h - dh) / 2);

			//console.log(w, h, dw, dh, l, t);

			$.blockUI({
				fadeIn: 25,
				fadeOut: 25,
				message: dialogBox,
				css: {
					border: 'none',
					backgroundColor: 'transparent',
					width: dw + 'px',
					height: dh + 'px',
					top: t + 'px',
					left: l + 'px'
				},
				themedCSS: {
					width: dw + 'px',
					height: dh + 'px',
					top: t + 'px',
					left: l + 'px'
				},
				width: dw + 'px',
				height: dh + 'px',
				top: t + 'px',
				left: l + 'px'
			});

		}
	},
	resize: function() {

		Structr.resize();

		var w = $(window).width();
		var h = $(window).height();

		var ml = 0;
		var mt = 24;

		// Calculate dimensions of dialog
		var dw = Math.min(900, w - ml);
		var dh = Math.min(600, h - mt);
		//            var dw = (w-24) + 'px';
		//            var dh = (h-24) + 'px';

		var l = parseInt((w - dw) / 2);
		var t = parseInt((h - dh) / 2);

		if (dialogBox && dialogBox.is(':visible')) {

			$('.blockPage').css({
				width: dw + 'px',
				height: dh + 'px',
				top: t + 'px',
				left: l + 'px'
			});

		}

		var bw = (dw - 28) + 'px';
		var bh = (dh - 106) + 'px';

		$('#dialogBox .dialogTextWrapper').css({
			width: bw,
			height: bh
		});

		$('#resourceTabs .resourceBox table').css({
			height: h - ($('#resourceTabsMenu').height() + 225) + 'px',
			width:  w - 59 + 'px'
		});

		$('.searchResults').css({
			height: h - 103 + 'px'
		});

	},
	error: function(text, confirmationRequired) {
		var message = new MessageBuilder().error(text);
		if (confirmationRequired) {
			message.requiresConfirmation();
		} else {
			message.delayDuration(2000).fadeDuration(1000);
		}
		message.show();
	},
	showDetails: function(n, typeParam) {

		var type = typeParam || n.type;
		if (!type) {
			new MessageBuilder().error('Missing type').requiresConfirmation().show();
			return;
		}

		if (!dialogBox.is(':visible')) {
			if (n) {
				//console.log('Edit node', node);
				_Crud.dialog('Details of ' + type + ' ' + (n.name ? n.name : n.id) + '<span class="id"> [' + n.id + ']</span>', function() {
				}, function() {
				});
			} else {
				//console.log('Create new node of type', typeOnCreate);
				_Crud.dialog('Create new ' + type, function() {
				}, function() {
				});
			}
		}
		var view = _Crud.view[type] || 'ui';

		// load details
		$.ajax({
			url: rootUrl + n.id + '/' + view,
			type: 'GET',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8;',
			headers: {
				Range: _Crud.ranges(type)
			},
			success: function(data) {
				if (!data)
					return;
				var node = data.result;

				var typeDef = _Crud.keys[type];

				if (!typeDef) {
					_Crud.getProperties(type, function() {
						_Crud.showDetails(n, type);
						return;
					});
				}

				dialogText.html('<table class="props" id="details_' + node.id + '"><tr><th>Name</th><th>Value</th>');//<th>Type</th><th>Read Only</th></table>');

				var table = $('table', dialogText);

				var keys;
				if (_Crud.keys[type]) {
					keys = Object.keys(_Crud.keys[type]);
				}

//				if (!keys) {
//					keys = Object.keys(typeDef.views[_Crud.view[type]]);
//				}

				if (!keys) {
					keys = Object.keys(node);
				}

				$.each(keys, function(i, key) {
					table.append('<tr><td class="key"><label for="' + key + '">' + _Crud.formatKey(key) + '</label></td><td class="__value ___' + key + '"></td>');//<td>' + type + '</td><td>' + property.readOnly + '</td></tr>');
					var cell = $('.___' + key, table);
					if (_Crud.isCollection(key, type)) {
						_Crud.appendPerCollectionPager(cell.prev('td'), type, key, function() {
							_Crud.showDetails(n, typeParam);
						});
					}
					if (node && node.id) {
						_Crud.populateCell(node.id, key, node.type, node[key], cell);

					} else {
						_Crud.populateCell(null, key, type, null, cell);
					}
				});

				if (node && node.isImage) {
					dialogText.prepend('<div class="img"><div class="wrap"><img class="thumbnailZoom" src="/' + node.id + '"></div></div>');
				}
			}
		});

	},
	showCreate: function(node, typeParam) {

		var type = typeParam || node.type;
		if (!type) {
			Structr.error('Missing type', function() {
			}, function() {
			});
			return;
		}
		var typeDef = _Crud.types[type]; console.log(typeDef);

		if (!dialogBox.is(':visible')) {
			if (node) {
				//console.log('Edit node', node);
				_Crud.dialog('Details of ' + type + ' ' + (node.name ? node.name : node.id) + '<span class="id"> [' + node.id + ']</span>', function() {
				}, function() {
				});
			} else {
				//console.log('Create new node of type', typeOnCreate);
				_Crud.dialog('Create new ' + type, function() {
				}, function() {
				});
			}
		}
		//console.log('readonly', readonly);

		dialogText.append('<form id="entityForm"><table class="props"><tr><th>Property Name</th><th>Value</th>');//<th>Type</th><th>Read Only</th></table></form>');

		var table = $('table', dialogText);

		var keys = Object.keys(_Crud.keys[type]);

		keys.forEach(function(key) {
			var readOnly = _Crud.readOnly(key, type);
			var isCollection = _Crud.isCollection(key, type);
			var relatedType = _Crud.relatedType(key, type);
			if (readOnly || (isCollection || relatedType)) {
				return;
			}

			table.append('<tr><td class="key"><label for="' + key + '">' + _Crud.formatKey(key) + '</label></td><td class="__value ___' + key + '"></td>');//<td>' + type + '</td><td>' + property.readOnly + '</td></tr>');
			var cell = $('.___' + key, table);
			if (node && node.id) {
				//console.log(node.id, key, type, node[key], cell);
				_Crud.populateCell(node.id, key, node.type, node[key], cell);
			} else {
				_Crud.populateCell(null, key, type, null, cell);
			}
		});

		var dialogSaveButton = $('.save', $('#dialogBox'));
		if (!(dialogSaveButton.length)) {
			dialogBox.append('<button class="save" id="saveProperties">Save</button>');
			dialogSaveButton = $('.save', $('#dialogBox'));
		}

		dialogSaveButton.off('click');
		dialogSaveButton.on('click', function() {
			var form = $('#entityForm');
			var json = JSON.stringify(_Crud.serializeObject(form));
			_Crud.crudCreate(type, typeDef.url.substring(1), json);
		});

		if (node && node.isImage) {
			dialogText.prepend('<div class="img"><div class="wrap"><img class="thumbnailZoom" src="/' + node.id + '"></div></div>');
		}

	},
	formatKey: function(text) {
		return text;
//		if (!text)
//			return '';
//		var result = '';
//		for (var i = 0; i < text.length; i++) {
//			var c = text.charAt(i);
//			if (c === c.toUpperCase()) {
//				result += ' ' + c;
//			} else {
//				result += (i === 0 ? c.toUpperCase() : c);
//			}
//		}
//		return result;
	},
	filterKeys: function(type, sourceArray) {

		if (!sourceArray) {
			return;
		}

		var filterSource = LSWrapper.getItem(crudHiddenColumnsKey + type);
		var filteredKeys = {};
		if (filterSource) {
			filteredKeys = JSON.parse(filterSource);
		} else {
			// default
			filteredKeys = { createdBy: 0, deleted: 0, hidden: 0, visibilityStartDate: 0, visibilityEndDate: 0};
			if (type === 'User') {
				filteredKeys.password = 0;
				filteredKeys.sessionIds = 0;
			}
			LSWrapper.setItem(crudHiddenColumnsKey + type, JSON.stringify(filteredKeys));
		}
		var result = sourceArray.filter(function(key) {
			return !(filteredKeys.hasOwnProperty(key) && filteredKeys[key] === 0);
		});
		//console.log(type, sourceArray, result);
		return result;
	},
	toggleColumn: function(type, key) {

		var table = $('#' + type + ' table');
		var filterSource = LSWrapper.getItem(crudHiddenColumnsKey + type);
		var filteredKeys = {};
		if (filterSource) {

			filteredKeys = JSON.parse(filterSource);
		}

		var hidden = filteredKeys.hasOwnProperty(key) && filteredKeys[key] === 0;

		if (hidden) {

			filteredKeys[key] = 1;

		} else {

			filteredKeys[key] = 0;

			// remove column(s) from table
			$('th.___' + key, table).remove();
			$('td.___' + key, table).each(function(i, t) {
				t.remove();
			});
		}

		LSWrapper.setItem(crudHiddenColumnsKey + type, JSON.stringify(filteredKeys));

	},
	serializeObject: function(obj) {
		var o = {};
		var a = obj.serializeArray();
		$.each(a, function() {
			if (this.value && this.value !== '') {
				if (o[this.name]) {
					if (!o[this.name].push) {
						o[this.name] = [o[this.name]];
					}
					o[this.name].push(this.value || '');
				} else {
					o[this.name] = this.value || '';
				}
			}
		});
		return o;
	},
	idArray: function(ids) {
		var arr = [];
		$.each(ids, function(i, id) {
			var obj = {};
			obj.id = id;
			arr.push(obj);
		});
		return arr;
	},
	/**
	 * Return the id of the given object.
	 */
	id: function(objectOrId) {
		if (typeof objectOrId === 'object') {
			return objectOrId.id;
		} else {
			return objectOrId;
		}
	},
	/**
	 * Compare to values and return true if they can be regarded equal.
	 *
	 * If both values are of type 'object', compare their id property,
	 * in any other case, compare values directly.
	 */
	equal: function(obj1, obj2) {
		if (typeof obj1 === 'object' && typeof obj2 === 'object') {
			var id1 = obj1.id;
			var id2 = obj2.id;
			if (id1 && id2) {
				return id1 === id2;
			}
		} else {
			return obj1 === obj2;
		}

		// default
		return false;
	},

	loadDetails : function(id, parentType) {

		var node;

		$.ajax({
			url: rootUrl + id + '/' + _Crud.view[parentType],
			type: 'GET',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8;',
			success: function(data) {
				if (!data)
					return;
				node = data.result;
				//console.log(node);
				//console.log(data.result);
			}
		});

		//console.log(node);

		return node;
	}

};
