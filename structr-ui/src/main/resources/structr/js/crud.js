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
var crudRecentTypesKey = 'structrCrudRecentTypes_' + port;

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

		$(document).on('click', '#crud-left li', function() {
			_Crud.typeSelected($(this).data('type'));
		});

		$(document).on('click', '#crud-recent-types .remove-recent-type', function (e) {
			e.stopPropagation();
			_Crud.removeRecentType($(this).closest('li').data('type'));
		});

		$(document).on('click', '#crudTypesFilterToggle', function (e) {
			$('#crudTypeFilterSettings').toggleClass('hidden');
		});

		$(document).on('change', '#crudTypeFilterSettings input', function(e) {
			_Crud.updateTypeList();
			LSWrapper.setItem(_Crud.displayTypeConfigKey, _Crud.getTypeVisibilityConfig());
		});

		$(document).on('click', '#crudTypeFilterSettings .close-button', function (e) {
			_Crud.hideTypeVisibilityConfig();
		});

	});

} else {
	defaultView = 'public';
}

var _Crud = {
	displayTypeConfigKey: 'structrCrudDisplayTypes_' + port,
	types: {},
	keys: {},
	crudCache: new CacheWithCallbacks(),
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
					Structr.errorFromResponse(data.responseJSON, url);
				},
				401: function(data) {
					Structr.errorFromResponse(data.responseJSON, url);
				},
				403: function(data) {
					Structr.errorFromResponse(data.responseJSON, url);
				},
				404: function(data) {
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
	init: function() {

		main.append('<div class="searchBox"><input class="search" name="search" placeholder="Search"><img class="clearSearchIcon" src="' + _Icons.grey_cross_icon + '"></div>');
		main.append('<div id="crud-main"><div id="crud-left">'
				+ '<div id="crud-types" class="resourceBox"><h2>All Types</h2><img src="' + _Icons.wrench_icon + '" id="crudTypesFilterToggle" title="Auto-filter types"><div id="crudTypeFilterSettings" class="hidden"></div><input placeholder="Filter types..." id="crudTypesSearch"><ul id="crud-types-list"></ul></div>'
				+ '<div id="crud-recent-types" class="resourceBox"><h2>Recent</h2><ul id="crud-recent-types-list"></ul></div></div>'
				+ '<div id="crud-right" class="resourceBox full-height-box"></div></div>');

		$('#crudTypeFilterSettings').append(
			'<div><input type="checkbox" id="crudTypeToggleCustom"><label for="crudTypeToggleCustom"> Show Custom Types</label></div>' +
			'<div><input type="checkbox" id="crudTypeToggleCore"><label for="crudTypeToggleCore"> Show Core Types</label></div>' +
			'<div><input type="checkbox" id="crudTypeToggleHtml"><label for="crudTypeToggleHtml"> Show HTML Types</label></div>' +
			'<div><input type="checkbox" id="crudTypeToggleUi"><label for="crudTypeToggleUi"> Show UI Types</label></div>' +
			'<div><input type="checkbox" id="crudTypeToggleLog"><label for="crudTypeToggleLog"> Show Log Types</label></div>' +
			'<div><input type="checkbox" id="crudTypeToggleOther"><label for="crudTypeToggleOther"> Show Other Types</label></div>' +
			'<div><button class="close-button">Close</button></div>'
		);

		var savedTypeVisibility = LSWrapper.getItem(_Crud.displayTypeConfigKey) || {};
		$('#crudTypeToggleCustom').prop('checked', (savedTypeVisibility.custom === undefined ? true : savedTypeVisibility.custom));
		$('#crudTypeToggleCore').prop('checked', (savedTypeVisibility.core === undefined ? true : savedTypeVisibility.core));
		$('#crudTypeToggleHtml').prop('checked', (savedTypeVisibility.html === undefined ? true : savedTypeVisibility.html));
		$('#crudTypeToggleUi').prop('checked', (savedTypeVisibility.ui === undefined ? true : savedTypeVisibility.ui));
		$('#crudTypeToggleLog').prop('checked', (savedTypeVisibility.log === undefined ? true : savedTypeVisibility.log));
		$('#crudTypeToggleOther').prop('checked', (savedTypeVisibility.other === undefined ? true : savedTypeVisibility.other));

		$('#crudTypesSearch').keyup(function (e) {
			if (e.keyCode === 27) {

				$(this).val('');

			} else if (e.keyCode === 13) {

				var filterVal = $(this).val().toLowerCase();
				var matchingTypes = Object.keys(_Crud.types).filter(function(type) {
					return type.toLowerCase() === filterVal;
				});

				if (matchingTypes.length === 1) {
					_Crud.typeSelected(matchingTypes[0]);
				}

			}

			_Crud.filterTypes($(this).val().toLowerCase());
		});

		Structr.ensureIsAdmin($('#crud-main'), function() {

			_Crud.schemaLoading = false;
			_Crud.schemaLoaded = false;
			_Crud.keys = {};

			_Crud.loadSchema(function() {

				if (browser) {
					_Crud.updateTypeList();
					_Crud.typeSelected(_Crud.type);
					_Crud.updateRecentTypeList(_Crud.type);
				}
				_Crud.resize();
				Structr.unblockMenu();

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

					$('#crud-main', main).hide();
					$('#resourceBox', main).hide();

				} else if (e.keyCode === 27 || searchString === '') {

					_Crud.clearSearch(main);

				}

			});

		});

	},
	onload: function() {

		Structr.updateMainHelpLink('http://docs.structr.org/frontend-user-guide#Data');

		if (!_Crud.type) {
			_Crud.restoreType();
		}

		if (!_Crud.type) {
			_Crud.type = urlParam('type');
		}

		if (!_Crud.type) {
			_Crud.type = defaultType;
		}

		// check for single edit mode
		var id = urlParam('id');
		if (id) {
			_Crud.loadSchema(function() {
				_Crud.crudRead(null, id, function(node) {
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

	},
	updateTypeList: function () {

		var $typesList = $('#crud-types-list');
		$typesList.empty();

		var typeVisibility = _Crud.getTypeVisibilityConfig();

		Object.keys(_Crud.types).sort().forEach(function(typeName) {

			var type = _Crud.types[typeName];

			var isDynamicType = type.className.startsWith('org.structr.dynamic');
			var isCoreType    = type.className.startsWith('org.structr.core.entity');
			var isHtmlType    = type.className.startsWith('org.structr.web.entity.html');
			var isUiType      = type.className.startsWith('org.structr.web.entity') && !type.className.startsWith('org.structr.web.entity.html');
			var isLogType     = type.className.startsWith('org.structr.rest.logging.entity');
			var isOtherType   = !(isDynamicType || isCoreType || isHtmlType || isUiType || isLogType);

			var hide =	(!typeVisibility.custom && isDynamicType) || (!typeVisibility.core && isCoreType) || (!typeVisibility.html && isHtmlType) ||
						(!typeVisibility.ui && isUiType) || (!typeVisibility.log && isLogType) || (!typeVisibility.other && isOtherType);

			if (!hide) {
				$typesList.append('<li class="crud-type" data-type="' + typeName + '">' + typeName + '</li>');
			}
		});

		_Crud.highlightCurrentType(_Crud.type);
		_Crud.resize();
	},
	typeSelected: function (selectedType) {

		_Crud.hideTypeVisibilityConfig();
		_Crud.updateRecentTypeList(selectedType);
		_Crud.highlightCurrentType(selectedType);

		_Crud.getProperties(selectedType, function(type, properties) {

			var crudRight = $('#crud-right');
			fastRemoveAllChildren(crudRight[0]);

			crudRight.data('url', '/' + type);

			_Crud.determinePagerData(type);
			var pagerNode = _Crud.addPager(type, crudRight);

			crudRight.append('<table class="crud-table"><thead></thead><tbody></tbody></table>');
			var table = $('table', crudRight);
			$('thead', table).append('<tr></tr>');
			var tableHeaderRow = $('tr:first-child', table);

			_Crud.filterKeys(type, Object.keys(properties)).forEach(function(key) {
				var prop = properties[key];
				tableHeaderRow.append('<th class="___' + prop.jsonName + '">' + prop.jsonName + '</th>');
			});
			tableHeaderRow.append('<th class="___action_header">Actions</th>');

			crudRight.append('<div id="crud-buttons">'
					+ '<button id="create' + type + '"><img src="' + _Icons.add_icon + '"> Create new ' + type + '</button>'
					+ '<button id="export' + type + '"><img src="' + _Icons.database_table_icon + '"> Export as CSV</button>'
					+ '<button id="import' + type + '"><img src="' + _Icons.database_add_icon + '"> Import CSV</button>'
					+ '</div>');

			crudRight.append('<div id="query-info">Query: <span class="queryTime"></span> s &nbsp; Serialization: <span class="serTime"></span> s</div>');

			$('#create' + type, crudRight).on('click', function() {
				_Crud.crudCreate(type);
			});
			$('#export' + type, crudRight).on('click', function() {
				_Crud.crudExport(type);
			});

			$('#import' + type, crudRight).on('click', function() {
				_Crud.crudImport(type);
			});

			_Crud.deActivatePagerElements(pagerNode);
			_Crud.activateList(type, properties);
			_Crud.activatePagerElements(type, pagerNode);
			_Crud.updateUrl(type);
		});
	},
	getTypeVisibilityConfig: function () {

		return {
			custom: $('#crudTypeToggleCustom').prop('checked'),
			core:   $('#crudTypeToggleCore').prop('checked'),
			html:   $('#crudTypeToggleHtml').prop('checked'),
			ui:     $('#crudTypeToggleUi').prop('checked'),
			log:    $('#crudTypeToggleLog').prop('checked'),
			other:  $('#crudTypeToggleOther').prop('checked')
		};

	},
	hideTypeVisibilityConfig: function () {
		$('#crudTypeFilterSettings').addClass('hidden');
	},
	highlightCurrentType: function (selectedType) {

		$('#crud-left li').removeClass('active');
		$('#crud-left li[data-type="' + selectedType + '"]').addClass('active');

		var $crudTypesList = $('#crud-types-list');
		var $selectedElementInTypeList = $('li[data-type="' + selectedType + '"]', $crudTypesList);

		if ($selectedElementInTypeList && $selectedElementInTypeList.length > 0) {
			var positionOfList = $crudTypesList.position().top;
			var scrollTopOfList = $crudTypesList.scrollTop();
			var positionOfElement = $selectedElementInTypeList.position().top;
			$crudTypesList.animate({scrollTop: positionOfElement + scrollTopOfList - positionOfList });
		} else {
			$crudTypesList.animate({scrollTop: 0});
		}

	},
	filterTypes: function (filterVal) {
		$('#crud-types-list li').each(function (i, el) {
			var $el = $(el);
			($el.data('type').toLowerCase().indexOf(filterVal) === -1) ? $el.hide() : $el.show();
		});
	},
	updateRecentTypeList: function (selectedType) {

		var recentTypes = LSWrapper.getItem(crudRecentTypesKey);

		if (recentTypes && selectedType) {

			var recentTypes = recentTypes.filter(function(type) {
				return (type !== selectedType);
			});
			recentTypes.unshift(selectedType);

		} else if (selectedType) {

			recentTypes = [selectedType];

		}

		recentTypes = recentTypes.slice(0, 12);

		if (recentTypes) {
			var $recentTypesList = $('#crud-recent-types-list');

			$('li', $recentTypesList).remove();

			recentTypes.forEach(function (type) {
				$recentTypesList.append('<li class="crud-type' + (selectedType === type ? ' active' : '') + '" data-type="' + type + '">' + type + '<img src="' + _Icons.grey_cross_icon + '" class="remove-recent-type"></li>');
			});

		}

		LSWrapper.setItem(crudRecentTypesKey, recentTypes);

	},
	removeRecentType: function (typeToRemove) {

		var recentTypes = LSWrapper.getItem(crudRecentTypesKey);

		if (recentTypes) {
			recentTypes = recentTypes.filter(function(type) {
				return (type !== typeToRemove);
			});
		}

		LSWrapper.setItem(crudRecentTypesKey, recentTypes);

		_Crud.updateRecentTypeList();
	},
	updateUrl: function(type) {

		if (type) {
			_Crud.type = type;
			_Crud.storeType();
			_Crud.storePagerData();
			window.location.hash = 'crud';

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
			_Crud.view[type]     = urlParam('view');
			_Crud.sort[type]     = urlParam('sort');
			_Crud.order[type]    = urlParam('order');
			_Crud.pageSize[type] = urlParam('pageSize');
			_Crud.page[type]     = urlParam('page');
		}

		if (!_Crud.view[type]) {
			_Crud.restorePagerData();
		}

		if (!_Crud.view[type]) {
			_Crud.view[type]     = defaultView;
			_Crud.sort[type]     = defaultSort;
			_Crud.order[type]    = defaultOrder;
			_Crud.pageSize[type] = defaultPageSize;
			_Crud.page[type]     = defaultPage;
		}
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
		var table = $('#crud-right table');
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
					+ '<a href="' + _Crud.sortAndPagingParameters(type, sortKey, newOrder, _Crud.pageSize[type], _Crud.page[type]) + '#' + type + '">' + key + '</a>');

				if (_Crud.isCollection(key, type)) {
					_Crud.appendPerCollectionPager(th, type, key);
				}

				$('a', th).on('click', function(event) {
					event.preventDefault();
					_Crud.sort[type] = key;
					_Crud.order[type] = (_Crud.order[type] && _Crud.order[type] === 'desc' ? 'asc' : 'desc');
					_Crud.refreshList(type);
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
		$.ajax({
			url: rootUrl + type + '/' + id + '/' + key + '/ui?page=' + page + '&pageSize=' + pageSize,
			contentType: 'application/json; charset=UTF-8',
			dataType: 'json',
			statusCode: {
				200: function(data) {

					var pageCount   = data.page_count;

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

					data.result.forEach(function(preloadedNode) {
						_Crud.getAndAppendNode(type, id, key, preloadedNode.id, el, preloadedNode);
					});
				}
			}

		});
	},
	appendCellPager: function(el, id, type, key) {

		var pageSize = _Crud.getCollectionPageSize(type, key) || defaultCollectionPageSize;

		$.ajax({
			url: rootUrl + type + '/' + id + '/' + key + '/ui?pageSize=' + pageSize,
			contentType: 'application/json; charset=UTF-8',
			dataType: 'json',
			statusCode: {
				200: function(data) {

					var resultCount = data.result_count;
					var pageCount   = data.page_count;

					data.result.forEach(function(preloadedNode) {
						_Crud.getAndAppendNode(type, id, key, preloadedNode.id, el, preloadedNode);
					});

					var page = 1;

					if (!resultCount || !pageCount || pageCount === 1) {
						return;
					}

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
		var sortParam = (s ? 'sort=' + s : '');
		var orderParam = (o ? 'order=' + o : '');
		var pageSizeParam = (ps ? 'pageSize=' + ps : '');
		var pageParam = (p ? 'page=' + p : '');

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
	},
	clearList: function(type) {
		var  div = $('#crud-right table tbody');
		fastRemoveAllChildren(div[0]);
	},
	list: function(type, properties, url) {

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

				_Crud.crudCache.clear();

				data.result.forEach(function(item) {
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
		var url = rootUrl + '/' + $('#crud-right').data('url') + '/ui' + _Crud.sortAndPagingParameters(type, _Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type]);

		_Crud.dialog('Export ' + type + ' list as CSV', function() {}, function() {});
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

		var url = csvRootUrl + $('#crud-right').data('url');

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
				data: importArea.val().split('\n').map($.trim).filter(function(line) { return line !== ''; }).join('\n'),
				success: function(data) {
					_Crud.refreshList(type);
				}
			});

		});

		$('.closeButton', $('#dialogBox')).on('click', function() {
			$('#startImport', dialogBtn).remove();
		});

	},
	updatePager: function(type, qt, st, ps, p, pc) {
		var typeNode = $('#crud-right');
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
				_Crud.dialog('Edit ' + t + ' ' + id, function() {}, function() {});
				_Crud.showDetails(data.result, t);
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
			success: function(data) {
				if (!data)
					return;

				_Crud.resetCell(id, key, data.result[key]);
			}
		});
	},
	crudUpdateObj: function(id, json, onSuccess, onError) {
		var handleError = function () {
			if (typeof onError === "function") {
				onError();
			} else {
				_Crud.crudReset(id);
			}
		};

		$.ajax({
			url: rootUrl + id,
			data: json,
			type: 'PUT',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function() {
					if (typeof onSuccess === "function") {
						onSuccess();
					} else {
						_Crud.crudRefresh(id);
					}
				},
				400: function(data, status, xhr) {
					_Crud.error('Bad request: ' + data.responseText, true);
					handleError();
				},
				401: function(data, status, xhr) {
					_Crud.error('Authentication required: ' + data.responseText, true);
					handleError();
				},
				403: function(data, status, xhr) {
					console.log(data, status, xhr);
					_Crud.error('Forbidden: ' + data.responseText, true);
					handleError();
				},
				404: function(data, status, xhr) {
					_Crud.error('Not found: ' + data.responseText, true);
					handleError();
				},
				422: function(data, status, xhr) {
					_Crud.error('Error: ' + data.responseText, true);
					handleError();
				},
				500: function(data, status, xhr) {
					_Crud.error('Internal Error: ' + data.responseText, true);
					handleError();
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

		var handleError = function () {
			if (typeof onError === "function") {
				onError();
			} else {
				_Crud.crudReset(id, key);
			}
		};

		$.ajax({
			url: url,
			data: JSON.stringify(obj),
			type: 'PUT',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function() {
					if (typeof onSuccess === "function") {
						onSuccess();
					} else {
						_Crud.crudRefresh(id, key, oldValue);
					}
				},
				400: function(data, status, xhr) {
					_Crud.error('Bad request: ' + data.responseText, true);
					handleError();
				},
				401: function(data, status, xhr) {
					_Crud.error('Authentication required: ' + data.responseText, true);
					handleError();
				},
				403: function(data, status, xhr) {
					_Crud.error('Forbidden: ' + data.responseText, true);
					handleError();
				},
				404: function(data, status, xhr) {
					_Crud.error('Not found: ' + data.responseText, true);
					handleError();
				},
				422: function(data, status, xhr) {
					_Crud.error('Error: ' + data.responseText, true);
					handleError();
				},
				500: function(data, status, xhr) {
					_Crud.error('Internal Error: ' + data.responseText, true);
					handleError();
				}
			}
		});
	},
	crudRemoveProperty: function(id, key, onSuccess, onError) {
		var url = rootUrl + id;
		var obj = {};
		obj[key] = null;

		var handleSuccess = function () {
			if (typeof onSuccess === "function") {
				onSuccess();
			} else {
				_Crud.crudRefresh(id, key);
			}
		};

		var handleError = function () {
			if (typeof onError === "function") {
				onError();
			} else {
				_Crud.crudReset(id, key);
			}
		};

		$.ajax({
			url: url,
			data: JSON.stringify(obj),
			type: 'PUT',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function() {
					handleSuccess();
				},
				204: function() {
					handleSuccess();
				},
				400: function(data, status, xhr) {
					_Crud.error('Bad request: ' + data.responseText, true);
					handleError();
				},
				401: function(data, status, xhr) {
					_Crud.error('Authentication required: ' + data.responseText, true);
					handleError();
				},
				403: function(data, status, xhr) {
					_Crud.error('Forbidden: ' + data.responseText, true);
					handleError();
				},
				404: function(data, status, xhr) {
					_Crud.error('Not found: ' + data.responseText, true);
					handleError();
				},
				422: function(data, status, xhr) {
					_Crud.error('Error: ' + data.responseText, true);
					handleError();
				},
				500: function(data, status, xhr) {
					_Crud.error('Internal Error: ' + data.responseText, true);
					handleError();
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
		var fields = $('input', form);
		form.attr('data-id', node.id);
		$.each(fields, function(f, field) {
			var value = formatValue(node[field.name], field.name, node.type, node.id);
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
		var tbody = $('#crud-right table tbody');
		tbody.append('<tr class="_' + id + '"></tr>');
		_Crud.populateRow(id, item, type, properties);
	},
	populateRow: function(id, item, type, properties) {
		var row = _Crud.row(id);
		if (properties) {
			_Crud.filterKeys(type, Object.keys(properties)).forEach(function(key) {
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

			cell.append('<img class="add" src="' + _Icons.add_grey_icon + '">');

			$('.add', cell).on('click', function() {

				_Crud.dialog('Add ' + simpleType, function() { }, function() { });
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
	getAndAppendNode: function(parentType, parentId, key, obj, cell, preloadedNode) {
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
				success: function(data) {
					if (data.result.length > 0) {
						_Crud.getAndAppendNode(parentType, parentId, key, data.result[0], cell);
					}
				}
			});

			return;
		}

		var nodeHandler = function (node) {

			_Crud.crudCache.addObject(node);

			var displayName = _Crud.displayName(node);

			cell.append('<div title="' + displayName + '" id="_' + node.id + '" class="node ' + (node.isImage? 'image ' : '') + ' ' + node.id + '_">' + fitStringToWidth(displayName, 80));
			var nodeEl = $('#_' + node.id, cell);

			var isSourceOrTarget = _Crud.types[parentType].isRel && (key === 'sourceId' || key === 'targetId');
			if (!isSourceOrTarget) {
				nodeEl.append('<img class="remove" src="' + _Icons.grey_cross_icon + '"></div>');
			}

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
		};

		if (preloadedNode) {

			nodeHandler(preloadedNode);

		} else {

			if (_Crud.crudCache.registerCallbackForId(id, nodeHandler)) {

				$.ajax({
					url: rootUrl + id + '/ui',
					type: 'GET',
					dataType: 'json',
					contentType: 'application/json; charset=utf-8;',
					headers: {
						Accept: 'application/json; charset=utf-8; properties=id,name,type,contentType,isThumbnail,isImage,tnSmall,tnMid'
					},
					success: function(data) {
						if (!data)
							return;

						var node = data.result;
						_Crud.crudCache.addObject(node);
					}
				});

			};

		}

	},
	clearSearch: function(el) {
		if (_Crud.clearSearchResults(el)) {
			$('.clearSearchIcon').hide().off('click');
			$('.search').val('');
			$('#crud-main', main).show();
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
		} else {
			types = type ? [type] : Object.keys(_Crud.types);
			if (searchString.match(/[0-9a-f]{32}/)) {
				attr = 'uuid'; // UUID
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
				_Crud.dialog('Details of ' + type + ' ' + (n.name ? n.name : n.id) + '<span class="id"> [' + n.id + ']</span>', function() {}, function() {});
			} else {
				_Crud.dialog('Create new ' + type, function() {}, function() {});
			}
		}
		var view = _Crud.view[type] || 'ui';

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

				dialogText.html('<table class="props" id="details_' + node.id + '"><tr><th>Name</th><th>Value</th>');

				var table = $('table', dialogText);

				var keys;
				if (_Crud.keys[type]) {
					keys = Object.keys(_Crud.keys[type]);
				}

				if (!keys) {
					keys = Object.keys(node);
				}

				$.each(keys, function(i, key) {
					table.append('<tr><td class="key"><label for="' + key + '">' + key + '</label></td><td class="__value ___' + key + '"></td>');
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
			Structr.error('Missing type', function() {}, function() {});
			return;
		}
		var typeDef = _Crud.types[type];

		if (!dialogBox.is(':visible')) {
			if (node) {
				_Crud.dialog('Details of ' + type + ' ' + (node.name ? node.name : node.id) + '<span class="id"> [' + node.id + ']</span>', function() {}, function() {});
			} else {
				_Crud.dialog('Create new ' + type, function() {}, function() {});
			}
		}

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

			table.append('<tr><td class="key"><label for="' + key + '">' + key + '</label></td><td class="__value ___' + key + '"></td>');//<td>' + type + '</td><td>' + property.readOnly + '</td></tr>');
			var cell = $('.___' + key, table);
			if (node && node.id) {
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
		return result;
	},
	toggleColumn: function(type, key) {

		var table = $('#crud-right table');
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
	}

};
