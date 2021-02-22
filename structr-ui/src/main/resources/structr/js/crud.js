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
var defaultType, defaultView, defaultSort, defaultOrder, defaultPage, defaultPageSize;
var browser = (typeof document === 'object');

if (browser) {

	$(document).ready(function() {

		defaultType = 'Page';
		defaultView = 'all';
		defaultSort = 'createdDate';
		defaultOrder = 'desc';

		defaultPage = 1;
		defaultPageSize = 10;

		main = $('#main');

		Structr.registerModule(_Crud);
	});

} else {
	defaultView = 'public';
}

var _Crud = {
	_moduleName: 'crud',
	defaultCollectionPageSize: 10,
	resultCountSoftLimit: 10000,
	crudPagerDataKey: 'structrCrudPagerData_' + port + '_',
	crudTypeKey: 'structrCrudType_' + port,
	crudHiddenColumnsKey: 'structrCrudHiddenColumns_' + port,
	crudRecentTypesKey: 'structrCrudRecentTypes_' + port,
	crudExactTypeKey: 'structrCrudExactType_' + port,
	searchField: undefined,
	types: {},
	availableViews: {},
	relInfo: {},
	keys: {},
	crudCache: new AsyncObjectCache(function(obj) {

		$.ajax({
			url: rootUrl + (obj.type ? obj.type + '/' : '') + obj.id + '/' + defaultView,
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
				_Crud.crudCache.addObject(node, node.id);
			}
		});
	}),
	ensureTypeInfoExists: async (type) => {

		if (!type) {
			console.log('Empty type?');
			return;
		}

		if (_Crud.keys[type]) {
			return;
		}

		let url = rootUrl + '_schema/' + type;

		let response = await fetch (url);
		let data     = await response.json();

		if (!response.ok) {

			Structr.errorFromResponse(data, url);

		} else {

			if (data && data.result && data.result[0]) {

				_Crud.availableViews[type] = data.result[0].views;

				let properties = {};

				let processViewInfo = function (view) {
					if (view) {
						for (let key of Object.keys(view)) {
							let prop = view[key];
							properties[prop.jsonName] = prop;
						}
					}
				};

				processViewInfo(data.result[0].views.public);
				processViewInfo(data.result[0].views.custom);
				processViewInfo(data.result[0].views.all);

				_Crud.keys[type] = properties;

			} else {

				new MessageBuilder().error('No type information found for type: ' + type).delayDuration(5000).fadeDuration(1000).show();
				_Crud.showMessageAfterDelay('No type information found for type: <b>' + type + '</b>', 500);
			}
		}
	},
	type: null,
	pageCount: null,
	view: {},
	sort: {},
	order: {},
	page: {},
	exact: {},
	pageSize: {},
	init: async function() {

		let html = await Structr.fetchHtmlTemplate('crud/main', {});

		main.append(html);

		_Crud.exact = LSWrapper.getItem(_Crud.crudExactTypeKey) || {};

		_Crud.schemaLoading = false;
		_Crud.schemaLoaded = false;
		_Crud.keys = {};

		await _Crud.loadSchema();

		if (browser) {

			let typeSelect     = document.getElementById('crud-types-select');
			let optGroupAll    = document.getElementById('crud-types-select-all');
			let optGroupRecent = document.getElementById('crud-types-select-recent');

			let allTypes    = Object.keys(_Crud.types).sort();
			let recentTypes = LSWrapper.getItem(_Crud.crudRecentTypesKey, []);

			for (let recentTypeName of recentTypes) {

				// only add to list if it exists!
				if (allTypes.includes(recentTypeName)) {

					let option = document.createElement('option');
					option.id = recentTypeName;
					option.textContent = recentTypeName;
					option.selected = (recentTypeName === _Crud.type);

					optGroupRecent.appendChild(option);
				}
			}

			allTypes.map((typeName) => {
				let option = document.createElement('option');
				option.id = typeName;
				option.textContent = typeName;

				if (typeName !== _Crud.type && !recentTypes.includes(typeName)) {
					optGroupAll.appendChild(option);
				}
			});

			let select2 = $(typeSelect).select2({
				dropdownParent: $('#crud-top')
			});
			select2.on('select2:select', (e) => {

				let typeName = e.params.data.text;
				let element  = e.params.data.element;

				optGroupRecent.insertBefore(element, optGroupRecent.firstChild);

				select2 = $(typeSelect).select2({
					dropdownParent: $('#crud-top')
				});

				_Crud.typeSelected(typeName);
			});

			let refreshButton = document.querySelector('#crud-refresh-list');
			refreshButton.addEventListener('click', () => {
				_Crud.typeSelected(_Crud.type);
			});

			_Crud.typeSelected(_Crud.type);
		}
		_Crud.resize();
		Structr.unblockMenu();

		_Crud.searchField = $('.search', main);
		_Crud.searchField.focus();

		Structr.appendInfoTextToElement({
			element: _Crud.searchField,
			text: 'By default a fuzzy search is performed on the <code>name</code> attribute of <b>every</b> node type. Optionally, you can specify a type and an attribute to search like so:<br><br>User.name:admin<br><br>If a UUID-string is supplied, the search is performed on the base type AbstractNode to yield the fastest results.',
			insertAfter: true,
			css: {
				left: '-18px',
				position: 'absolute'
			},
			helpElementCss: {
				fontSize: '12px',
				lineHeight: '1.1em'
			}
		});

		let crudMain = $('#crud-main');

		_Crud.searchField.keyup(function(e) {
			var searchString = $(this).val();
			if (searchString && searchString.length && e.keyCode === 13) {

				$('.clearSearchIcon').show().on('click', function() {
					_Crud.clearSearch(crudMain);
				});

				_Crud.search(searchString, crudMain, null, function(e, node) {
					e.preventDefault();
					_Crud.showDetails(node, false, node.type);
					return false;
				});

				$('#crud-top').hide();
				$('#crud-type-detail').hide();

			} else if (e.keyCode === 27 || searchString === '') {

				_Crud.clearSearch(crudMain);
			}
		});
	},
	onload: function() {

		Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('crud'));

		if (!_Crud.type) {
			_Crud.restoreType();
		}

		if (!_Crud.type) {
			_Crud.type = urlParam('type');
		}

		if (!_Crud.type) {
			_Crud.type = defaultType;
		}

		_Crud.init();

		$(window).off('resize');
		$(window).on('resize', function() {
			_Crud.resize();
		});
	},
	messageTimeout: undefined,
	showLoadingMessageAfterDelay: function (message, delay) {

		_Crud.showMessageAfterDelay('<img src="' + _Icons.getSpinnerImageAsData() + '"> ' + message + ' - please stand by', delay);

	},
	showMessageAfterDelay: function (message, delay) {

		clearTimeout(_Crud.messageTimeout);

		_Crud.messageTimeout = setTimeout(function() {

			_Crud.removeMessage();

			let crudRight = $('#crud-type-detail');
			crudRight.append('<div class="crud-message"><div class="crud-centered">' + message + '</div></div>');

		}, delay);

	},
	removeMessage: function() {
		$('#crud-type-detail .crud-message').remove();
	},
	typeSelected: async function (type) {

		_Crud.updateRecentTypeList(type);

		var crudRight = $('#crud-type-detail');
		fastRemoveAllChildren(crudRight[0]);
		_Crud.showLoadingMessageAfterDelay('Loading schema information for type <b>' + type + '</b>', 500);


		await _Crud.ensureTypeInfoExists(type);

		clearTimeout(_Crud.messageTimeout);

		fastRemoveAllChildren(crudRight[0]);

		crudRight.data('url', '/' + type);

		crudRight.append('<div id="crud-buttons">'
				+ '<button class="action" id="create' + type + '"><i class="' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" /> Create new ' + type + '</button>'
				+ '<button id="export' + type + '"><i class="' + _Icons.getFullSpriteClass(_Icons.database_table_icon) + '" /> Export as CSV</button>'
				+ '<button id="import' + type + '"><i class="' + _Icons.getFullSpriteClass(_Icons.database_add_icon) + '" /> Import CSV</button>'
				+ '<button id="delete' + type + '"><i class="' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" /> Delete <b>all</b> objects of this type</button>'
				+ '<input id="exact_type_' + type + '" class="exact-type-checkbox" type="checkbox"><label for="exact_type_' + type + '" class="exact-type-checkbox-label"> Exclude subtypes</label>'
				+ '</div>');

		_Crud.determinePagerData(type);
		var pagerNode = _Crud.addPager(type, crudRight);

		crudRight.append('<table class="crud-table"><thead><tr></tr></thead><tbody></tbody></table>');
		_Crud.updateCrudTableHeader(type);

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

		$('#delete' + type, crudRight).on('click', function() {

			Structr.confirmation('<h3>WARNING: Really delete all objects of type \'' + type + '\'?</h3><p>This will delete all objects of the type (and of all inheriting types!).</p><p>Depending on the amount of objects this can take a while.</p>', function() {
				$.unblockUI({
					fadeOut: 25
				});

				_Crud.deleteAllNodesOfType(type);
			});
		});

		let exactTypeCheckbox = $('#exact_type_' + type, crudRight);
		if (_Crud.exact[type] === true) {
			exactTypeCheckbox.prop('checked', true);
		}
		exactTypeCheckbox.on('change', function() {
			_Crud.exact[type] = exactTypeCheckbox.prop('checked');
			LSWrapper.setItem(_Crud.crudExactTypeKey, _Crud.exact);
			_Crud.refreshList(type);
		});

		_Crud.deActivatePagerElements(pagerNode);
		_Crud.activateList(type);
		_Crud.activatePagerElements(type, pagerNode);
		_Crud.updateUrl(type);
	},
	updateRecentTypeList: function (selectedType) {

		let recentTypes = LSWrapper.getItem(_Crud.crudRecentTypesKey);

		if (recentTypes && selectedType) {

			recentTypes = recentTypes.filter(function(type) {
				return (type !== selectedType);
			});
			recentTypes.unshift(selectedType);

		} else if (selectedType) {

			recentTypes = [selectedType];

		}

		LSWrapper.setItem(_Crud.crudRecentTypesKey, recentTypes);

	},
	getCurrentProperties: function(type) {

		let properties = _Crud.availableViews[type].all;

		if (_Crud.view[type] !== 'all') {
			let viewDef = _Crud.availableViews[type][_Crud.view[type]];

			if (viewDef) {
				properties = viewDef;
			}
		}

		return properties;
	},
	updateCrudTableHeader: function(type) {

		let properties = _Crud.getCurrentProperties(type);

		var tableHeaderRow = $('#crud-type-detail table thead tr');
		fastRemoveAllChildren(tableHeaderRow[0]);

		_Crud.filterKeys(type, Object.keys(properties)).forEach(function(key) {
			tableHeaderRow.append('<th class="' + _Crud.cssClassForKey(key) + '">' + key + '</th>');
		});
		tableHeaderRow.append('<th class="___action_header">Actions</th>');
	},
	updateUrl: function(type) {

		if (type) {
			_Crud.type = type;
			_Crud.storeType();
			_Crud.storePagerData();
			window.location.hash = 'crud';

		}
		_Crud.searchField.focus();

	},
	/**
	 * Read the schema from the _schema REST resource and call 'callback'
	 * after the schema is loaded.
	 */
	loadSchema: async function() {

		var processRelInfo = function (relInfo) {
			if (relInfo) {
				relInfo.forEach(function(r) {
					_Crud.relInfo[r.type] = {
						source: r.possibleSourceTypes,
						target: r.possibleTargetTypes
					};

				});
			}
		};

		let response = await fetch (rootUrl + '_schema');
		let data     = await response.json();

		for (let typeObj of data.result) {
			_Crud.types[typeObj.type] = typeObj;
			processRelInfo(typeObj.relatedTo);
			processRelInfo(typeObj.relatedFrom);
		}
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
	isFunctionProperty: function(key, type) {
		return ("org.structr.core.property.FunctionProperty" === _Crud.keys[type][key].className);
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
				+ ' Page Size: <input class="pageSize" type="text" size="3" value="' + _Crud.pageSize[type] + '">'
				+ ' View: <select class="view"></select></div>');

		let select = $('select.view', el);
		for (let view in _Crud.availableViews[type]) {
			let selected = '';
			if (_Crud.view[type] === view) {
				selected = ' selected';
			}
			select.append('<option' + selected + '>' + view + '</option>');
		}

		Structr.appendInfoTextToElement({
			element: select,
			text: 'The attributes of the given view are fetched. Attributes can still be hidden using the "Configure columns" dialog. id and type are always shown first.',
			insertAfter: true
		});

		el.append('<div class="resource-link"><a target="_blank" href="' + rootUrl + type + '">/' + type + '</a></div>');

		return $('.pager', el);
	},
	storeType: function() {
		LSWrapper.setItem(_Crud.crudTypeKey, _Crud.type);
	},
	restoreType: function() {
		var val = LSWrapper.getItem(_Crud.crudTypeKey);
		if (val) {
			_Crud.type = val;
		}
	},
	storePagerData: function() {
		var type = _Crud.type;
		var pagerData = _Crud.view[type] + ',' + _Crud.sort[type] + ',' + _Crud.order[type] + ',' + _Crud.page[type] + ',' + _Crud.pageSize[type];
		LSWrapper.setItem(_Crud.crudPagerDataKey + type, pagerData);
	},
	restorePagerData: function() {
		var type = _Crud.type;
		var val = LSWrapper.getItem(_Crud.crudPagerDataKey + type);

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
		LSWrapper.setItem(_Crud.crudPagerDataKey + '_collectionPageSize_' + type + '.' + _Crud.cssClassForKey(key), value);
	},
	getCollectionPageSize: function(type, key) {
		return LSWrapper.getItem(_Crud.crudPagerDataKey + '_collectionPageSize_' + type + '.' + _Crud.cssClassForKey(key));
	},
	setCollectionPage: function(type, key, value) {
		LSWrapper.setItem(_Crud.crudPagerDataKey + '_collectionPage_' + type + '.' + _Crud.cssClassForKey(key), value);
	},
	getCollectionPage: function(type, key) {
		return LSWrapper.getItem(_Crud.crudPagerDataKey + '_collectionPage_' + type + '.' + _Crud.cssClassForKey(key));
	},
	replaceSortHeader: function(type) {
		var table = $('#crud-type-detail table');
		var newOrder = (_Crud.order[type] && _Crud.order[type] === 'desc' ? 'asc' : 'desc');
		$('th', table).each(function(i, t) {
			var th = $(t);
			var key = th.attr('class').substring(3);
			if (key === "action_header") {

				th.empty();
				th.append('Actions <i title="Configure columns" style="margin-left: 4px" class="' + _Icons.getFullSpriteClass(_Icons.view_detail_icon) + '" />');
				$('i', th).on('click', async function(event) {

					_Crud.dialog('<h3>Configure columns for type ' + type + '</h3>', function() {
					}, function() {
					});

					$('div.dialogText').append('<table class="props" id="configure-' + type + '-columns"></table>');

					let table = $('#configure-' + type + '-columns');
					table.append('<tr><th>Column Key</th><th>Visible</th></tr>');

					let url = rootUrl + '_schema/' + type + '/' + defaultView;

					let response = await fetch (url);
					let data     = await response.json();

					// no schema entry found?
					if (!data || !data.result || data.result_count === 0) {

						new MessageBuilder().warning('Unable to find schema information for type \'' + type + '\'. There might be database nodes with no type information or a type unknown to Structr in the database.').show();

					} else {

						var properties = {};
						data.result.forEach(function(prop) {
							properties[prop.jsonName] = prop;
						});

						var hiddenKeys = _Crud.getHiddenKeys(type);

						Object.keys(properties).forEach(function(key) {

							var checkboxKey = 'column-' + type + '-' + key + '-visible';
							var hidden = hiddenKeys.includes(key);

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
					}
				});

			} else if (key !== 'Actions') {
				$('a', th).off('click');
				th.empty();
				var sortKey = key;
				th.append(
					'<i title="Hide this column" class="' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /><a href="' + _Crud.sortAndPagingParameters(type, sortKey, newOrder, _Crud.pageSize[type], _Crud.page[type]) + '#' + type + '">' + key + '</a>');

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

				$('i', th).on('click', function(event) {
					event.preventDefault();
					_Crud.toggleColumn(type, key);
					return false;
				});
			}
		});
	},
	appendPerCollectionPager: function(el, type, key, callback) {
		el.append('<input type="text" class="collection-page-size" size="1" value="' + (_Crud.getCollectionPageSize(type, key) || _Crud.defaultCollectionPageSize) + '">');

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
	updateCellPager: async function(el, id, type, key, page, pageSize) {

		let response = await fetch(rootUrl + type + '/' + id + '/' + key + '/public?page=' + page + '&pageSize=' + pageSize);
		let data     = await response.json();

		var softLimited = false;
		var pageCount = data.page_count;

		// handle new soft-limited REST result without counts
		if (data.result_count === undefined && data.page_count === undefined) {
			pageCount = _Crud.getSoftLimitedPageCount(pageSize);
			softLimited = true;
		}

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

		if (softLimited) {
			_Crud.showSoftLimitAlert($('input.page-count'));
		}
	},
	appendCellPager: async function(el, id, type, key) {

		var pageSize = _Crud.getCollectionPageSize(type, key) || _Crud.defaultCollectionPageSize;

		// use public view for cell pager - we should not need more information than this!
		let response = await fetch(rootUrl + type + '/' + id + '/' + key + '/public?pageSize=' + pageSize);
		let data     = await response.json();

		var softLimited = false;
		var resultCount = data.result_count;
		var pageCount   = data.page_count;

		if (data.result && data.result.length > 0) {
			for (let preloadedNode of data.result) {
				_Crud.getAndAppendNode(type, id, key, preloadedNode.id, el, preloadedNode);
			}
		}

		var page = 1;

		// handle new soft-limited REST result without counts
		if (data.result_count === undefined && data.page_count === undefined) {
			resultCount = _Crud.getSoftLimitedResultCount();
			pageCount   = _Crud.getSoftLimitedPageCount(pageSize);
			softLimited = true;
		}

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

		if (softLimited) {
			_Crud.showSoftLimitAlert($('input.page-count'));
		}

	},
	sortAndPagingParameters: function(t, s, o, ps, p, exact = false) {

		let paramsArray = [];

		if (s) {
			paramsArray.push('sort=' + s);
		}
		if (o) {
			paramsArray.push('order=' + o);
		}
		if (ps) {
			paramsArray.push('pageSize=' + ps);
		}
		if (p) {
			paramsArray.push('page=' + p);
		}
		if (exact === true) {
			paramsArray.push('type=' + t);
		}

		return '?' + paramsArray.join('&');
	},
	refreshList: function(type) {
		_Crud.clearList(type);
		_Crud.activateList(type);
	},
	activateList: function(type) {
		var url = rootUrl + type + '/all' + _Crud.sortAndPagingParameters(type, _Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type], _Crud.exact[type]);
		_Crud.list(type, url);
	},
	clearList: function(type) {
		var div = $('#crud-type-detail table tbody');
		fastRemoveAllChildren(div[0]);
	},
	list: async function(type, url, isRetry) {

		let properties = _Crud.getCurrentProperties(type);

		_Crud.showLoadingMessageAfterDelay('Loading data for type <b>' + type + '</b>', 100);

		let acceptHeaderProperties = (isRetry ? '' : ' properties=' + _Crud.filterKeys(type, Object.keys(properties)).join(','));

		let response = await fetch (url, {
			headers: {
				Range: _Crud.ranges(type),
				Accept: 'application/json; charset=utf-8;' + acceptHeaderProperties
			},
		});
		let data    = await response.json();

		clearTimeout(_Crud.messageTimeout);
		_Crud.removeMessage();

		if (!response.ok) {
			if (response.status === 431) {
				// Happens if headers grow too large (property list too long)

				if (!isRetry) {
					_Crud.list(type, url, true);
				} else {
					_Crud.showMessageAfterDelay('<img src="' + _Icons.getSpinnerImageAsData() + '"> View is too large - please select different view', 1);
				}
			}

		} else {

			if (!data) {
				return;
			}

			_Crud.crudCache.clear();

			for (let item of data.result) {
				_Crud.appendRow(type, properties, item);
			}

			_Crud.updatePager(type, data.query_time, data.serialization_time, data.page_size, data.page, data.page_count);
			_Crud.replaceSortHeader(type);

			if (_Crud.types[type].isRel && !_Crud.relInfo[type]) {

				var button = $('#crud-buttons #create' + type);
				button.attr('disabled', 'disabled').addClass('disabled');
				Structr.appendInfoTextToElement({
					text: 'Action not supported for built-in relationship types',
					element: $('#crud-buttons #create' + type),
					css: {
						marginRight: '10px'
					},
					offsetY: -50,
					insertAfter: true
				});
			}
		}

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
					var pageSize = _Crud.getCollectionPageSize(type, key) || _Crud.defaultCollectionPageSize;
					var start = (page-1)*pageSize;
					var end = page*pageSize;
					ranges += key + '=' + start + '-' + end + ';';
				}
			});
			return ranges;
		}
	},
	crudExport: async function(type) {

		var url = csvRootUrl + '/' + $('#crud-type-detail').data('url') + '/all' + _Crud.sortAndPagingParameters(type, _Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type]);

		_Crud.dialog('Export ' + type + ' list as CSV', function() {}, function() {});

		if (!Structr.activeModules.csv) {
			dialogText.append('CSV Module is not included in the current license. See <a href="https://structr.com/editions">Structr Edition Info</a> for more information.');
			return;
		}

		var exportArea = $('<textarea class="exportArea"></textarea>');
		dialogText.append(exportArea);

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

		var hiddenKeys             = _Crud.getHiddenKeys(type);
		var acceptHeaderProperties = Object.keys(_Crud.keys[type]).filter(function(key) { return !hiddenKeys.includes(key); }).join(',');

		let response = await fetch(url, {
			headers: {
				Range: _Crud.ranges(type),
				Accept: 'properties=' + acceptHeaderProperties
			}
		});

		exportArea.text(await response.text());
	},
	crudImport: function(type) {

		var url = csvRootUrl + $('#crud-type-detail').data('url');

		_Crud.dialog('Import CSV data for type ' + type + '', function() {}, function() {});

		if (!Structr.activeModules.csv) {
			dialogText.append('CSV Module is not included in the current license. See <a href="https://structr.com/editions">Structr Edition Info</a> for more information.');
			return;
		}

		var importArea = $('<textarea class="importArea"></textarea>');
		dialogText.append(importArea);

		var separatorSelect = $('<select><option selected>;</option><option>,</option></select>');
		var separatorContainer = $('<span><label>Field Separator: </label></span>');
		separatorContainer.append(separatorSelect);
		dialogMeta.append(separatorContainer);

		var quoteCharacterSelect = $('<select><option selected>"</option><option>\'</option></select>');
		var quoteCharacterContainer = $('<span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<label>Quote Character: </label></span>');
		quoteCharacterContainer.append(quoteCharacterSelect);
		dialogMeta.append(quoteCharacterContainer);

		var periodicCommitCheckbox = $('<input type="checkbox" />');
		var periodicCommitContainer = $('<span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<label>Periodic Commit? </label></span>');
		periodicCommitContainer.append(periodicCommitCheckbox);
		dialogMeta.append(periodicCommitContainer);

		var periodicCommitInput = $('<input type="text" value="1000" size=5 />');
		var periodicCommitIntervalSpan = $('<span> (Interval: </span>');
		periodicCommitIntervalSpan.append(periodicCommitInput).append(' lines)');
		periodicCommitIntervalSpan.hide();
		periodicCommitContainer.append(periodicCommitIntervalSpan);

		periodicCommitCheckbox.change(function () {
			if (periodicCommitCheckbox.prop('checked')) {
				periodicCommitIntervalSpan.show();
			} else {
				periodicCommitIntervalSpan.hide();
			}
		});

		window.setTimeout(function() {
			importArea.focus();
		}, 200);

		$('#startImport', dialogBtn).remove();
		dialogBtn.append('<button class="action" id="startImport">Start Import</button>');

		$('#startImport', dialogBtn).on('click', async function() {

			var maxImportCharacters = 100000;
			var importLength        = importArea.val().length;

			if (importLength > maxImportCharacters) {
				var importTooBig = 'Not starting import because it contains too many characters (' + importLength + '). The limit is ' + maxImportCharacters + '.<br> Consider uploading the CSV file to the Structr filesystem and using the file-based CSV import which is more powerful than this import.<br><br>';

				new MessageBuilder().error(importTooBig).title('Too much import data').requiresConfirmation().show();
				return;
			}

			let response = await fetch(url, {
				method: 'POST',
				body: importArea.val().split('\n').map($.trim).filter(function(line) { return line !== ''; }).join('\n'),
				headers: {
					'X-CSV-Field-Separator': separatorSelect.val(),
					'X-CSV-Quote-Character': quoteCharacterSelect.val(),
					'X-CSV-Periodic-Commit': periodicCommitCheckbox.prop('checked'),
					'X-CSV-Periodic-Commit-Interval': periodicCommitInput.val()
				}
			});

			if (response.ok) {
				_Crud.refreshList(type);
			} else {
				Structr.errorFromResponse(await response.json(), url);
			}
		});

		$('.closeButton', $('#dialogBox')).on('click', function() {
			$('#startImport', dialogBtn).remove();
		});

	},
	deleteAllNodesOfType: async function(type) {

		var url = rootUrl + '/' + type;

		let response = await fetch(url, {
			method: 'DELETE'
		});

		let data = await response.json();

		if (response.ok) {
			new MessageBuilder().success('Deletion of all nodes of type \'' + type + '\' finished.').delayDuration(2000).fadeDuration(1000).show();
			_Crud.typeSelected(type);
		} else {
			Structr.errorFromResponse(data, url, {statusCode: 400, requiresConfirmation: true});
		}

	},
	updatePager: function(type, qt, st, ps, p, pc) {

		let softLimited = false;
		var typeNode = $('#crud-type-detail');
		$('.queryTime', typeNode).text(qt);
		$('.serTime', typeNode).text(st);
		$('.pageSize', typeNode).val(ps);

		_Crud.page[type] = p;
		$('.page', typeNode).val(_Crud.page[type]);

		if (pc === undefined) {
			pc = _Crud.getSoftLimitedPageCount(ps);
			softLimited = true;
		}

		_Crud.pageCount = pc;
		$('.pageCount', typeNode).val(_Crud.pageCount);

		if (softLimited && !$('.soft-limit-warning')[0]) {
			_Crud.showSoftLimitAlert($('input.pageCount'));
		}

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

		$('select.view', pagerNode).on('change', function(e) {
			_Crud.view[type] = $(this).val();
			_Crud.updateCrudTableHeader(type);
			_Crud.refreshList(type);
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
	crudEdit: async function(id, type) {
		var t = type || _Crud.type;

		let response = await fetch (rootUrl + t + '/' + id + '/public');
		let data     = await response.json();

		if (data) {
			_Crud.dialog('Edit ' + t + ' ' + id, function() {}, function() {});
			_Crud.showDetails(data.result, t);
		}
	},
	crudCreate: async function(type, url, json, onSuccess, onError) {

		url = url || type;

		let response = await fetch(rootUrl + url, {
			method: 'POST',
			body: json
		});
		let data = await response.json();

		if (response.ok) {

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

		} else {
			if (response.status !== 422 || !dialogBox.is(':visible')) {
				Structr.errorFromResponse(data, url, {statusCode: response.status, requiresConfirmation: true});
			}

			_Crud.showCreateError(type, data, onError);
		}

	},
	showCreateError: async function(type, data, onError) {

		if (onError) {
			onError();
		} else {

			if (!dialogBox.is(':visible')) {
				await _Crud.showCreate(null, type);
			}

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

			window.setTimeout(function() {

				for (let error of data.errors) {
					var key = error.property;
					var errorMsg = error.token;

					var input = $('td [name="' + key + '"]', dialogText);
					if (input.length) {

						var errorText = '';
						errorText += '"' + key + '" ' + errorMsg.replace(/_/gi, ' ');

						if (error.detail) {
							errorText += ' ' + error.detail;
						}

						Structr.showAndHideInfoBoxMessage(errorText, 'error', 2000, 1000);

						input.css({
							backgroundColor: '#fee',
							borderColor: '#933'
						});
					}
				}

			}, 100);
		}
	},
	crudRefresh: async function(id, key, oldValue) {

		let response = await fetch(rootUrl + id + '/all', {
			headers: {
				Accept: 'application/json; charset=utf-8; properties=id,type,' + key
			}
		});
		let data     = await response.json();

		if (!data)
			return;

		if (key) {
			_Crud.refreshCell(id, key, data.result[key], data.result.type, oldValue);
		} else {
			_Crud.refreshRow(id, data.result, data.result.type);
		}

	},
	crudReset: async function(id, key) {

		let response = await fetch(rootUrl + id + '/all', {
			headers: {
				Accept: 'application/json; charset=utf-8; properties=id,type,' + key
			}
		});
		let data     = await response.json();

		if (data) {
			_Crud.resetCell(id, key, data.result[key]);
		}

	},
	crudUpdateObj: async function(id, json, onSuccess, onError) {

		let url = rootUrl + id;

		let handleError = function (data, code) {
			Structr.errorFromResponse(data, url, {statusCode: code, requiresConfirmation: true});

			if (typeof onError === "function") {
				onError();
			} else {
				_Crud.crudReset(id);
			}
		};

		let response = await fetch(url, {
			method: 'PUT',
			body: json
		});
		let data     = await response.json();

		if (response.ok) {

			if (typeof onSuccess === "function") {
				onSuccess();
			} else {
				_Crud.crudRefresh(id);
			}

		} else {
			handleError(data, response.status);
		}
	},
	crudUpdate: async function(id, key, newValue, oldValue, onSuccess, onError) {

		let url = rootUrl + id;

		let obj = {};
		if (newValue && newValue !== '') {
			obj[key] = newValue;
		} else {
			obj[key] = null;
		}

		let handleError = function (data, code) {
			Structr.errorFromResponse(data, url, {statusCode: code, requiresConfirmation: true});

			if (typeof onError === "function") {
				onError();
			} else {
				_Crud.crudReset(id, key);
			}
		};

		let response = await fetch(url, {
			method: 'PUT',
			body: JSON.stringify(obj)
		});
		let data     = await response.json();

		if (response.ok) {
			if (typeof onSuccess === "function") {
				onSuccess();
			} else {
				_Crud.crudRefresh(id, key, oldValue);
			}
		} else {
			handleError(data, response.status);
		}
	},
	crudRemoveProperty: async function(id, key, onSuccess, onError) {

		let url = rootUrl + id;
		let obj = {};
		obj[key] = null;

		let handleSuccess = function () {
			if (typeof onSuccess === "function") {
				onSuccess();
			} else {
				_Crud.crudRefresh(id, key);
			}
		};

		let handleError = function (data, code) {
			Structr.errorFromResponse(data, url, {statusCode: code, requiresConfirmation: true});

			if (typeof onError === "function") {
				onError();
			} else {
				_Crud.crudReset(id, key);
			}
		};

		let response = await fetch(url, {
			method: 'PUT',
			body: JSON.stringify(obj)
		});
		let data     = await response.json();

		if (response.ok) {
			handleSuccess();
		} else {
			handleError(data, response.status);
		}
	},
	crudDelete: async function(type, id) {
		var url = rootUrl + '/' + type + '/' + id;

		let response = await fetch(url, { method: 'DELETE' });
		let data     = await response.json();

		if (response.ok) {
			let row = _Crud.row(id);
			row.remove();
		} else {
			Structr.errorFromResponse(data, url, {statusCode: response.status, requiresConfirmation: true});
		}
	},
	populateForm: function(form, node) {
		var fields = $('input', form);
		form.attr('data-id', node.id);
		for (let field of fields) {
			var value = formatValue(node[field.name]);
			$('input[name="' + field.name + '"]').val(value);
		}
	},
	cells: function(id, key) {
		var row = _Crud.row(id);
		var cellInMainTable = $('.' + _Crud.cssClassForKey(key), row);

		var cellInDetailsTable;
		var table = $('#details_' + id);
		if (table) {
			cellInDetailsTable = $('.' + _Crud.cssClassForKey(key), table);
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

		for (let cell of cells) {
			cell.empty();
			_Crud.populateCell(id, key, _Crud.type, oldValue, cell);
		}
	},
	refreshCell: function(id, key, newValue, type, oldValue) {
		var cells = _Crud.cells(id, key);

		for (let cell of cells) {
			cell.empty();
			_Crud.populateCell(id, key, type, newValue, cell);
			if (newValue !== oldValue && !(!newValue && oldValue === '')) {
				blinkGreen(cell);
			}
		}
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
	appendRow: async function(type, properties, item) {

		await _Crud.ensureTypeInfoExists(item.type);

		let id = item['id'];
		let tbody = $('#crud-type-detail table tbody');
		let row = _Crud.row(id);

		if ( !(row && row.length) ) {
			tbody.append('<tr class="_' + id + '"></tr>');
		}
		_Crud.populateRow(id, item, type, properties);
	},
	populateRow: async function(id, item, type, properties) {

		var row = _Crud.row(id);
		row.empty();

		if (properties) {

			let keys =_Crud.filterKeys(type, Object.keys(properties));
			for (let key of keys) {
				row.append('<td class="value ' + _Crud.cssClassForKey(key) + '"></td>');
				var cells = _Crud.cells(id, key);
				for (let cell of cells) {
					_Crud.populateCell(id, key, type, item[key], cell);
				}
			}
			row.append('<td class="actions"><a title="Edit" class="edit"><i class="' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" /></a><a title="Delete" class="delete"><i class="' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" /></a><a title="Access Control" class="security"><i class="' + _Icons.getFullSpriteClass(_Icons.key_icon) + '" /></a></td>');
			_Crud.resize();

			$('.actions .edit', row).on('click', function(event) {
				event.preventDefault();
				_Crud.crudEdit(id);
				return false;
			});
			$('.actions .delete', row).on('click', function(event) {
				event.preventDefault();
				var c = confirm('Are you sure you want to delete ' + type + ' ' + id + ' ?');
				if (c === true) {
					_Crud.crudDelete(type, id);
				}
			});

			if (_Crud.types[type] && _Crud.types[type].isRel === true) {
				$('.actions .security', row).hide();
			} else {
				_Entities.bindAccessControl($('.actions .security', row), item);
			}
		}
	},
	populateCell: async function(id, key, type, value, cell) {

		var isRel = _Crud.types[type].isRel;
		var isCollection = _Crud.isCollection(key, type);
		var isEnum = _Crud.isEnum(key, type);
		var relatedType = _Crud.relatedType(key, type);
		var readOnly = _Crud.readOnly(key, type);
		var simpleType;
		var isSourceOrTarget = _Crud.types[type].isRel && (key === 'sourceId' || key === 'targetId');

		if (readOnly) {
			cell.addClass('readonly');
		}

		await _Crud.ensureTypeInfoExists(type);

		if (!isSourceOrTarget && !relatedType) {

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

				cell.html(nvl(formatValue(value), '<i title="Show calendar" class="' + _Icons.getFullSpriteClass(_Icons.calendar_icon) + '" />'));
				if (!readOnly) {
					var format = _Crud.isFunctionProperty(key, type) ? "yyyy-MM-dd'T'HH:mm:ssZ" : _Crud.getFormat(key, type);
					var dateTimePickerFormat = getDateTimePickerFormat(format);

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

					if (!id) {
						_Crud.appendEnumSelect(cell, id, key, format);
					}
				}

			} else if (isCollection) { // Array types

				let values = value || [];

				if (!id) {

					let focusAndActivateField = function(el) {
						$(el).focus().on('keydown', function(e) {
							if (e.which === 9) { // tab key
								e.stopPropagation();
								cell.append('<input name="' + key + '" size="4">');
								focusAndActivateField(cell.find('[name="' + key + '"]').last());
								return false;
							}
						});
						return false;
					};

					// create dialog
					cell.append('<input name="' + key + '" size="4">');
					focusAndActivateField(cell.find('[name="' + key + '"]').last());

				} else {

					// update existing object

					let typeInfo = _Crud.keys[type];
					cell.append(formatArrayValueField(key, values, typeInfo.format === 'multi-line', typeInfo.readOnly, false));
					cell.find('[name="' + key + '"]').each(function(i, el) {
						_Entities.activateInput(el, id, null, typeInfo, function() {
							_Crud.crudRefresh(id, key);
						});
					});
				}

			} else {

				cell.text(nvl(formatValue(value), ''));
				if (!readOnly) {

					cell.on('click', function(event) {
						event.preventDefault();
						var self = $(this);
						_Crud.activateTextInputField(self, id, key, propertyType);
					});

					if (!id) {
						_Crud.activateTextInputField(cell, id, key, propertyType);
					}
				}
			}

		} else if (isCollection) {

			simpleType = lastPart(relatedType, '.');

			cell.append('<i class="add ' + _Icons.getFullSpriteClass(_Icons.add_grey_icon) + '" />');

			$('.add', cell).on('click', function() {

				_Crud.dialog('Add ' + simpleType, function() { }, function() { });
				_Crud.displaySearch(type, id, key, simpleType, dialogText);

			});

			if (_Crud.keys[type][key] && _Crud.keys[type][key].className.indexOf('CollectionIdProperty') === -1 && _Crud.keys[type][key].className.indexOf("CollectionNotionProperty") === -1) {

				_Crud.appendCellPager(cell, id, type, key);
			}

		} else {

			simpleType = lastPart(relatedType, '.');

			if (isRel && _Crud.relInfo[type]) {

				if (key === 'sourceId') {
					simpleType = _Crud.relInfo[type].source;
				} else if (key === 'targetId') {
					simpleType = _Crud.relInfo[type].target;
				}
			}

			if (value) {

				_Crud.getAndAppendNode(type, id, key, value, cell);

			} else {

				if (simpleType) {

					cell.append('<i class="add ' + _Icons.getFullSpriteClass(_Icons.add_grey_icon) + '" />');

					$('.add', cell).on('click', function() {

						if (!dialogBox.is(':visible') || !isRel) {

							_Crud.dialog('Add ' + simpleType + ' to ' + key, function() { }, function() { });
							_Crud.displaySearch(type, id, key, simpleType, dialogText);

						} else {

							var btn = $(this);
							$('#entityForm').hide();
							_Crud.displaySearch(type, id, key, simpleType, dialogText, function (n) {
								$('.searchBox', dialogText).remove();
								btn.remove();
								_Crud.getAndAppendNode(type, id, key, n, cell, n, true);
								_Crud.clearSearchResults(dialogText);
								$('#entityForm').show();
							});
						}
					});
				}
			}
		}

		if (!isSourceOrTarget && !readOnly && !relatedType && propertyType !== 'Boolean') {
			cell.prepend('<i title="Clear value" class="crud-clear-value ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /><br>');
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
	tryAppendNodeByName: async function(parentType, parentId, key, obj, cell, node, insertFakeInput) {

		// search object by name
		type = _Crud.keys[parentType][key].relatedType.split('.').pop();

		let response = await fetch(rootUrl + type + '?name=' + obj);
		let data     = await response.json();

		if (response.ok) {

			if (data.result.length > 0) {
				_Crud.getAndAppendNode(parentType, parentId, key, data.result[0], cell);
			}
		}

	},
	getAndAppendNode: function(parentType, parentId, key, obj, cell, preloadedNode, insertFakeInput) {

		if (!obj) {
			return;
		}
		var id, type;
		if ((typeof obj) === 'object') {
			id = obj.id;
			type = obj.type;
		} else if (isUUID(obj)) {
			id = obj;
		} else {
			_Crud.tryAppendNodeByName(parentType, parentId, key, obj, cell, preloadedNode, insertFakeInput);
			return;
		}

		let nodeHandler = (node) => {

			var displayName = _Crud.displayName(node);

			cell.append('<div title="' + escapeForHtmlAttributes(displayName) + '" id="_' + node.id + '" class="node ' + (node.isImage ? 'image ' : '') + ' ' + node.id + '_"><span class="name_ abbr-ellipsis abbr-75pc">' + displayName + '</span></div>');
			var nodeEl = $('#_' + node.id, cell);

			var isSourceOrTarget = _Crud.types[parentType].isRel && (key === 'sourceId' || key === 'targetId');
			if (!isSourceOrTarget) {
				nodeEl.append('<i class="remove ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></div>');
			} else if (insertFakeInput) {
				nodeEl.append('<input type="hidden" name="' + key + '" value="' + node.id + '" /></div>');
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
				Command.get(parentId, 'id,type', function(parentObj) {
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
			_Crud.crudCache.registerCallback({id: id, type: type}, id, nodeHandler);
		}
	},
	clearSearch: function(el) {
		if (_Crud.clearSearchResults(el)) {
			$('.clearSearchIcon').hide().off('click');
			$('.search').val('');
			$('#crud-top').show();
			$('#crud-type-detail').show();
		}
	},
	clearSearchResults: function(el) {
		var searchResults = $('.searchResults', el);
		if (searchResults.length) {
			searchResults.remove();
			return true;
		}
		return false;
	},
	/**
	 * Conduct a search and append search results to 'el'.
	 *
	 * If an optional type is given, restrict search to this type.
	 */
	search: async function(searchString, el, type, onClickCallback, optionalPageSize, blacklistedIds = []) {

		_Crud.clearSearchResults(el);

		el.append('<div class="searchResults"><h2>Search Results</h2></div>');
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
			if (type) {
				types = type.split(',').filter(function(t) { return t.trim() !== ''; });
			} else {
				// only search in node types
				types = Object.keys(_Crud.types).filter((t) => { return !_Crud.types[t].isRel; });
			}
			if (searchString.match(/[0-9a-f]{32}/)) {
				attr = 'uuid'; // UUID
				types = ['AbstractNode'];
			}
		}

		for (let type of types) {

			var url, searchPart;
			if (attr === 'uuid') {
				url = rootUrl + type + '/' + searchString;
			} else {
				searchPart = searchString === '*' || searchString === '' ? '' : '&' + attr + '=' + encodeURIComponent(searchString) + '&loose=1';
				url = rootUrl + type + '/public' + _Crud.sortAndPagingParameters(type, 'name', 'asc', optionalPageSize || 1000, 1) + searchPart;
			}

			searchResults.append('<div id="placeholderFor' + type + '" class="searchResultGroup resourceBox"><img class="loader" src="' + _Icons.getSpinnerImageAsData() + '">Searching for "' + searchString + '" in ' + type + '</div>');

			let response = await fetch(url);
			let data     = await response.json();

			if (response.ok) {
				if (!data || !data.result) {
					return;
				}
				var result = data.result;
				$('#placeholderFor' + type + '').remove();

				if (result) {
					if (Array.isArray(result)) {
						if (result.length) {

							for (let node of result) {
								if (!blacklistedIds.includes(node.id)) {
									_Crud.searchResult(searchResults, type, node, onClickCallback);
								}
							}

						} else {

							_Crud.noResults(searchResults, type);

						}

					} else if (result.id) {

						_Crud.searchResult(searchResults, type, result, onClickCallback);

					}

				} else {

					_Crud.noResults(searchResults, type);
				}

			} else {

				$('#placeholderFor' + type + '').remove();
			}
		};
	},
	noResults: function(searchResults, type) {
		searchResults.append('<div id="resultsFor' + type + '" class="searchResultGroup resourceBox">No results for ' + type.capitalize() + '</div>');
		window.setTimeout(function() {
			$('#resultsFor' + type).fadeOut('fast');
		}, 1000);
	},
	searchResult: function(searchResults, type, node, onClickCallback) {
		if (!$('#resultsFor' + type, searchResults).length) {
			searchResults.append('<div id="resultsFor' + type + '" class="searchResultGroup resourceBox"><h3>' + type.capitalize() + '</h3></div>');
		}
		let displayName = _Crud.displayName(node);
		let title = 'name: ' + node.name + '\nid: ' + node.id  + '' + ' \ntype: ' + node.type;
		$('#resultsFor' + type, searchResults).append('<div title="' + escapeForHtmlAttributes(title) + '" class="_' + node.id + ' node abbr-ellipsis abbr-120">' + displayName + '</div>');

		let nodeEl = $('#resultsFor' + type + ' ._' + node.id, searchResults);
		if (node.isImage) {
			nodeEl.append('<div class="wrap"><img class="thumbnail" src="/' + node.id + '"></div>');
		}

		nodeEl.on('click', function(e) {
			onClickCallback(e, node);
		});
	},
	displayName: function(node) {
		var displayName;
		if (node.isContent && node.content && !node.name) {
			displayName = escapeTags(node.content.substring(0, 100));
		} else {
			displayName = node.name || node.id || node;
		}
		return displayName;
	},
	displaySearch: function(parentType, id, key, type, el, callbackOverride) {

		el.append('<div class="searchBox searchBoxDialog"><input class="search" name="search" size="20" placeholder="Search"><i class="clearSearchIcon ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></div>');
		var searchBox = $('.searchBoxDialog', el);
		var search = $('.search', searchBox);

		window.setTimeout(function() {
			search.focus();
		}, 250);

		search.keyup(function(e) {
			e.preventDefault();

			var searchString = $(this).val();
			if (e.keyCode === 13) {

				$('.clearSearchIcon', searchBox).show().on('click', function() {
					if (_Crud.clearSearchResults(el)) {
						$('.clearSearchIcon').hide().off('click');
						search.val('');
						search.focus();
					}
				});

				_Crud.search(searchString, el, type, function(e, node) {
					e.preventDefault();
					if (typeof callbackOverride === "function") {
						callbackOverride(node);
					} else {
						_Crud.addRelatedObject(parentType, id, key, node, function() {});
					}
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

		_Crud.populateSearchDialogWithInitialResult(parentType, id, key, type, el, callbackOverride, "*");
	},
	populateSearchDialogWithInitialResult: function(parentType, id, key, type, el, callbackOverride, initialSearchText) {

		// display initial result list
		_Crud.search(initialSearchText, el, type, function(e, node) {
			e.preventDefault();
			if (typeof callbackOverride === "function") {
				callbackOverride(node);
			} else {
				_Crud.addRelatedObject(parentType, id, key, node, function() {});
			}
			return false;
		}, 100);
	},
	removeRelatedObject: async function(obj, key, relatedObj, callback) {

		let type = obj.type;

		if (_Crud.isCollection(key, type)) {

			let response = await fetch(rootUrl + type + '/' + obj.id + '/all');
			let data     = await response.json();

			if (response.ok) {
				var objects = _Crud.extractIds(data.result[key]);
				var relatedId = (typeof relatedObj === 'object' ? relatedObj.id : relatedObj);
				objects = objects.filter(function(obj) {
					return obj.id !== relatedId;
				});

				_Crud.updateRelatedCollection(obj.id, key, objects, callback);
			}

		} else {
			_Crud.crudRemoveProperty(obj.id, key);
		}
	},
	addRelatedObject: async function(type, id, key, relatedObj, callback) {

		if (_Crud.isCollection(key, type)) {

			let response = await fetch(rootUrl + type + '/' + id + '/all');
			let data     = await response.json();

			if (response.ok) {
				var objects = _Crud.extractIds(data.result[key]);
				if (!isIn(relatedObj.id, objects)) {
					objects.push({id: relatedObj.id});
				}

				_Crud.updateRelatedCollection(id, key, objects, callback);
			}

		} else {

			var updateObj = {};
			updateObj[key] = {
				id: relatedObj.id
			};

			_Crud.crudUpdateObj(id, JSON.stringify(updateObj), function() {
				_Crud.crudRefresh(id, key);
				dialogCancelButton.click();
			});
		}
	},
	updateRelatedCollection: function (id, key, objects, callback) {
		var updateObj = {};
		updateObj[key] = objects;

		_Crud.crudUpdateObj(id, JSON.stringify(updateObj), function() {
			_Crud.crudRefresh(id, key);
			if (callback) {
				callback();
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
					_Crud.searchField.focus();
				});
			}

			var dimensions = Structr.getDialogDimensions(0, 24);
			Structr.blockUI(dimensions);

			_Crud.resize();
		}
	},
	resize: function() {

		var dimensions = Structr.getDialogDimensions(0, 24);

		if (dialogBox && dialogBox.is(':visible')) {

			$('.blockPage').css({
				width: dimensions.width + 'px',
				height: dimensions.height + 'px',
				top: dimensions.top + 'px',
				left: dimensions.left + 'px'
			});
		}

		$('#dialogBox .dialogTextWrapper').css({
			width: (dimensions.width - 28) + 'px',
			height: (dimensions.height - 106) + 'px'
		});

		Structr.resize();

	},
	showDetails: async function(n, typeParam) {

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

		let response = await fetch(rootUrl + type + '/' + n.id + '/' + view, {
			headers: {
				Range: _Crud.ranges(type),
				Accept: 'application/json; charset=utf-8;' + ((_Crud.keys[type]) ? 'properties=' + _Crud.filterKeys(type, Object.keys(_Crud.keys[type])).join(',') : '')
			}
		});
		let data     = await response.json();

		if (response.ok) {

			if (!data)
				return;
			var node = data.result;

			await _Crud.ensureTypeInfoExists(type);

			dialogText.html('<table class="props" id="details_' + node.id + '"><tr><th>Name</th><th>Value</th>');

			var table = $('table', dialogText);

			var keys;
			if (_Crud.keys[type]) {
				keys = Object.keys(_Crud.keys[type]);
			}

			if (!keys) {
				keys = Object.keys(node);
			}

			for (let key of keys) {
				table.append('<tr><td class="key"><label for="' + key + '">' + key + '</label></td><td class="__value ' + _Crud.cssClassForKey(key) + '"></td>');
				var cell = $('.' + _Crud.cssClassForKey(key), table);
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
			}

			if (node && node.isImage) {
				dialogText.prepend('<div class="img"><div class="wrap"><img class="thumbnailZoom" src="/' + node.id + '"></div></div>');
			}
		}
	},
	showCreate: async function(node, typeParam) {

		var type = typeParam || node.type;
		if (!type) {
			Structr.error('Missing type');
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

		dialogText.append('<form id="entityForm"><table class="props"><tr><th>Property Name</th><th>Value</th>');

		var table = $('table', dialogText);

		var keys = Object.keys(_Crud.keys[type]);

		for (let key of keys) {
			var readOnly = _Crud.readOnly(key, type);
			var isCollection = _Crud.isCollection(key, type);
			var relatedType = _Crud.relatedType(key, type);
			if (!(readOnly || (isCollection && relatedType))) {
				table.append('<tr><td class="key"><label for="' + key + '">' + key + '</label></td><td class="__value ' + _Crud.cssClassForKey(key) + '"></td>');
				var cell = $('.' + _Crud.cssClassForKey(key), table);
				if (node && node.id) {
					await _Crud.populateCell(node.id, key, node.type, node[key], cell);
				} else {
					await _Crud.populateCell(null, key, type, null, cell);
				}
			}
		}

		var dialogSaveButton = $('.save', $('#dialogBox'));
		if (!(dialogSaveButton.length)) {
			dialogBtn.append('<button class="save" id="saveProperties">Save</button>');
			dialogSaveButton = $('.save', $('#dialogBox'));
		}

		dialogSaveButton.off('click');
		dialogSaveButton.on('click', function() {
			dialogSaveButton.attr('disabled', true);
			var form = $('#entityForm');
			var json = JSON.stringify(_Crud.serializeObject(form));
			_Crud.crudCreate(type, typeDef.url.substring(1), json, undefined, function () { dialogSaveButton.attr('disabled', false); });
		});

		if (node && node.isImage) {
			dialogText.prepend('<div class="img"><div class="wrap"><img class="thumbnailZoom" src="/' + node.id + '"></div></div>');
		}

	},
	getHiddenKeys: function(type) {
		var hiddenKeysSource = LSWrapper.getItem(_Crud.crudHiddenColumnsKey + type);
		var hiddenKeys = [];
		if (hiddenKeysSource) {

			hiddenKeys = JSON.parse(hiddenKeysSource);

			if (!Array.isArray(hiddenKeys)) {
				// migrate old format
				var newKeys = [];
				Object.keys(hiddenKeys).forEach(function(key) {
					newKeys.push(key);
				});
				hiddenKeys = newKeys;
			}

		} else {

			// Default: Hide some large fields
			if (_Crud.isPrincipalType(_Crud.types[type])) {

				// hidden keys for Principal types
				[
					'isUser',
					'isAdmin',
					'createdBy',
					'sessionIds',
					'publicKeys',
					'sessionData',
					'password',
					'passwordChangeDate',
					'salt',
					'twoFactorSecret',
					'twoFactorToken',
					'isTwoFactorUser',
					'twoFactorConfirmed',
					'ownedNodes',
					'localStorage',
					'customPermissionQueryAccessControl',
					'customPermissionQueryDelete',
					'customPermissionQueryRead',
					'customPermissionQueryWrite'].forEach(function(key) {
					if (hiddenKeys.indexOf(key) === -1) {
						hiddenKeys.push(key);
					}
				});

			}

			if (_Crud.isImageType(_Crud.types[type])) {

				// hidden keys for Image types
				[
					'base64Data',
					'imageData',
					'favoriteContent',
					'favoriteContext',
					'favoriteUsers',
					'resultDocumentForExporter',
					'documentTemplateForExporter',
					'isFile',
					'position',
					'extractedContent',
					'indexedWords',
					'minificationTargets',
					'fileModificationDate'].forEach(function(key) {
					if (hiddenKeys.indexOf(key) === -1) {
						hiddenKeys.push(key);
					}
				});

			}

			if (_Crud.isFileType(_Crud.types[type])) {

				// hidden keys for File types
				[
					'base64Data',
					'favoriteContent',
					'favoriteContext',
					'favoriteUsers',
					'relationshipId',
					'resultDocumentForExporter',
					'documentTemplateForExporter',
					'isFile',
					'position',
					'extractedContent',
					'indexedWords',
					'minificationTargets',
					'fileModificationDate',
					'nextSiblingId'
				].forEach(function(key) {
					if (hiddenKeys.indexOf(key) === -1) {
						hiddenKeys.push(key);
					}
				});
			}
		}

		// hidden keys for all types
		[
			'base',
			'createdBy',
			'lastModifiedBy',
			'ownerId',
			'hidden',
			'internalEntityContextPath',
			'grantees'
		].forEach(function(key) {
			if (hiddenKeys.indexOf(key) === -1) {
				hiddenKeys.push(key);
			}
		});

		return hiddenKeys;
	},
	isPrincipalType: function (typeDef) {
		let cls = 'org.structr.dynamic.Principal';
		return typeDef.className === cls || _Crud.inheritsFromAncestorType(typeDef, cls);
	},
	isFileType: function (typeDef) {
		let cls = 'org.structr.dynamic.AbstractFile';
		return typeDef.className === cls || _Crud.inheritsFromAncestorType(typeDef, cls);
	},
	isImageType: function (typeDef) {
		let cls = 'org.structr.dynamic.Image';
		return typeDef.className === cls || _Crud.inheritsFromAncestorType(typeDef, cls);
	},
	inheritsFromAncestorType: function (typeDef, ancestorFQCN) {

		if (typeDef.extendsClass === ancestorFQCN) {

			return true;

		} else {

			// search parent type
			var parentType = Object.values(_Crud.types).filter(function(t) {
				return (t.className === typeDef.extendsClass);
			});

			if (parentType.length === 1) {
				return _Crud.inheritsFromAncestorType(parentType[0], ancestorFQCN);
			}
		}

		return false;
	},
	filterKeys: function(type, sourceArray) {

		if (!sourceArray) {
			return;
		}

		let hiddenKeys   = _Crud.getHiddenKeys(type);
		let filteredKeys = sourceArray.filter(function(key) {
			return !(hiddenKeys.includes(key));
		});

		let typePos = filteredKeys.indexOf('type');
		if (typePos !== -1) {
			filteredKeys.splice(typePos, 1);
		}
		filteredKeys.unshift('type');

		let idPos = filteredKeys.indexOf('id');
		if (idPos !== -1) {
			filteredKeys.splice(idPos, 1);
		}
		filteredKeys.unshift('id');

		return filteredKeys;
	},
	toggleColumn: function(type, key) {

		var hiddenKeys = _Crud.getHiddenKeys(type);

		if (hiddenKeys.includes(key)) {

			hiddenKeys.splice(hiddenKeys.indexOf(key), 1);

		} else {

			hiddenKeys.push(key);

			var table = $('#crud-type-detail table');

			// remove column(s) from table
			$('th.' + _Crud.cssClassForKey(key), table).remove();
			$('td.' + _Crud.cssClassForKey(key), table).each(function(i, t) {
				t.remove();
			});
		}

		LSWrapper.setItem(_Crud.crudHiddenColumnsKey + type, JSON.stringify(hiddenKeys));
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
	getSoftLimitedPageCount: function(pageSize) {
		return Math.ceil(_Crud.getSoftLimitedResultCount() / pageSize);
	},
	getSoftLimitedResultCount: function() {
		return _Crud.resultCountSoftLimit;
	},
	getSoftLimitMessage: function() {
		return 'Result count exceeds soft limit (' + _Crud.resultCountSoftLimit + '). Page count may be higher than displayed.';
	},
	showSoftLimitAlert: function(el) {
		el.attr('style', 'background-color: #fc0 !important;');
		el.attr('title', _Crud.getSoftLimitMessage());
	},
	cssClassForKey: function (key) {
		return '___' + key.replace(/\s/g,  '_whitespace_');
	}
};
