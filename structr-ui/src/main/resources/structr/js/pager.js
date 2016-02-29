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
/**
 * Create a pager for the given type to the given DOM element.
 *
 * This pager either calls Command#list (WebSocket call) after being loaded
 * and binds Command#list to all actions or does the same for REST calls.
 *
 * If the optional callback function is given, it will be executed
 * instead of the default action.
 */
var _Pager = {

	initPager: function(type, p, ps, sort, order, filters) {
		if (!_Pager.restorePagerData(type)) {
			page[type] = parseInt(p);
			pageSize[type] = parseInt(ps);
			sortKey[type] = sort;
			sortOrder[type] = order;
			pagerFilters[type] = filters || {};

			_Pager.storePagerData(type, page[type], pageSize[type], sortKey[type], sortOrder[type], pagerFilters[type], pagerFilters[type]);
		}
	},

	initFilters: function(type, filters) {
		pagerFilters[type] = filters;
		_Pager.storePagerData(type, page[type], pageSize[type], sortKey[type], sortOrder[type], pagerFilters[type], pagerFilters[type]);
	},

	storePagerData: function(type, page, pageSize, sort, order, filters) {
		var data = {
			page: parseInt(page),
			pageSize: parseInt(pageSize),
			sort: sort,
			order: order,
			filters: filters
		};
		LSWrapper.setItem(pagerDataKey + type, JSON.stringify(data));
	},

	restorePagerData: function(type) {
		var pagerData = LSWrapper.getItem(pagerDataKey + type);
		if (pagerData) {

			if (!pagerData.startsWith('{')) {
				LSWrapper.removeItem(pagerDataKey + type);
				return false;
			}

			pagerData = JSON.parse(pagerData);

			page[type] = pagerData.page;
			pageSize[type] = pagerData.pageSize;
			sortKey[type] = pagerData.sort;
			sortOrder[type] = pagerData.order;
			if (pagerData.filters) {
				$.extend(pagerFilters[type], pagerData.filters);
			}

			return true;
		}

		return false;
	},
/*
	addRestPager: function (el, rootOnly, type, callback) {
		_Logger.log(_LogType.PAGER, 'add Rest Pager', type, pageSize[type], page[type], sortKey[type], sortOrder[type]);
		var pgr = new Pager(el, rootOnly, type, callback);
		pgr.transportFunction = function() {

			var url = rootUrl + type + '?page=' + page[pgr.type] + '&pageSize=' + pageSize[pgr.type] + '&sort=' + sortKey[pgr.type] + '&order=' + sortOrder[pgr.type];

			var filterAttributes = Object.keys(pgr.getNonEmptyFilterAttributes());

			if (filterAttributes.length > 0) {
				Object.keys(pgr.getNonEmptyFilterAttributes()).forEach(function(fa) {
					url += '&' + fa + '=' + pagerFilters[pgr.type][fa];
					filterAdded = true;
				});

				url += '&loose=1';
			}

			$.ajax({
				url: url,
				contentType: 'application/json; charset=UTF-8',
				dataType: 'json',
				statusCode: {
					200: function(data) {
						pgr.internalCallback(data.result, data.result_count);
					}
				}
			});
		};

		pgr.init();
		return pgr;
	},
*/
	addPager: function (el, rootOnly, type, callback) {
		_Logger.log(_LogType.PAGER, 'add WS Pager', type, pageSize[type], page[type], sortKey[type], sortOrder[type]);
		var pgr = new Pager(el, rootOnly, type, callback);
		pgr.transportFunction = function() {
			var filterAttrs = pgr.getNonEmptyFilterAttributes();
			Command.query(pgr.type,  pageSize[type], page[pgr.type], sortKey[pgr.type], sortOrder[pgr.type], filterAttrs, pgr.internalCallback, false);
		}
		pgr.init();
		return pgr;
	}
};

var Pager = function (el, rootOnly, type, callback) {

	var pagerObj = this;

	// Parameters
	this.el = el;
	this.filterEl; // if set, use this as container for filters
	this.rootOnly = rootOnly;
	this.type = type;
	if (!callback) {
		this.callback = function(entities) {
			entities.forEach(function(entity) {
				StructrModel.create(entity);
			});
		};
	} else {
		this.callback = callback;
	}

	if (typeof pagerFilters[this.type] !== 'object') {
		pagerFilters[this.type] = {};
	}

	this.internalCallback = function (result, count) {

		rawResultCount[pagerObj.type] = count;
		pageCount[pagerObj.type] = Math.max(1, Math.ceil(rawResultCount[pagerObj.type] / pageSize[pagerObj.type]));
		pagerObj.updatePager(pagerObj.type, dialog.is(':visible') ? dialog : undefined);

		pagerObj.pageCount.val(pageCount[pagerObj.type]);

		pagerObj.callback(result);
	};

	this.init = function () {

		_Pager.restorePagerData(this.type);

		this.el.append('<div class="pager pager' + this.type + '" style="clear: both"><button class="pageLeft">&lt; Prev</button>'
				+ ' <input class="page" type="text" size="3" value="' + page[this.type] + '"><button class="pageRight">Next &gt;</button>'
				+ ' of <input class="readonly pageCount" readonly="readonly" size="3">'
				+ ' Items: <input class="pageSize" type="text" size="3" value="' + pageSize[this.type] + '"></div>');

		this.pager = $('.pager' + this.type, this.el);

		this.pageLeft  = $('.pageLeft', this.pager);
		this.pageRight = $('.pageRight', this.pager);
		this.pageNo    = $('.page', this.pager);
		this.pageSize  = $('.pageSize', this.pager);
		this.pageCount = $('.pageCount', this.pager);

		this.pageSize.on('keypress', function(e) {
			if (e.keyCode === 13) {
				pageSize[pagerObj.type] = $(this).val();
				page[pagerObj.type] = 1;
				pagerObj.updatePagerElements();
				pagerObj.transportFunction();
			}
		});

		this.pageNo.on('keypress', function(e) {
			if (e.keyCode === 13) {
				page[pagerObj.type] = $(this).val();
				pagerObj.updatePagerElements();
				pagerObj.transportFunction();
			}
		});

		this.pageLeft.on('click', function(e) {
			page[pagerObj.type]--;
			pagerObj.updatePagerElements();
			pagerObj.transportFunction();
		});

		this.pageRight.on('click', function() {
			page[pagerObj.type]++;
			pagerObj.updatePagerElements();
			pagerObj.transportFunction();
		});

		pagerObj.transportFunction();
	};

	/**
	 * Gets called after a new slice of data has been received
	 */
	this.updatePager = function() {
		if (page[this.type] === 1) {
			this.pageLeft.attr('disabled', 'disabled').addClass('disabled');
		} else {
			this.pageLeft.removeAttr('disabled', 'disabled').removeClass('disabled');
		}

		if (pageCount[this.type] === 1 || (page[this.type] === pageCount[this.type])) {
			this.pageRight.attr('disabled', 'disabled').addClass('disabled');
		} else {
			this.pageRight.removeAttr('disabled', 'disabled').removeClass('disabled');
		}

		if (pageCount[this.type] === 1) {
			this.pageNo.attr('disabled', 'disabled').addClass('disabled');
		} else {
			this.pageNo.removeAttr('disabled', 'disabled').removeClass('disabled');
		}

		_Pager.storePagerData(this.type, page[this.type], pageSize[this.type], sortKey[this.type], sortOrder[this.type], pagerFilters[this.type]);
	};

	/**
	 * Gets called whenever a change has been made (i.e. button has been pressed)
	 */
	this.updatePagerElements = function () {
		$('.page', this.pager).val(page[this.type]);
		$('.pageSize', this.pager).val(pageSize[this.type]);

		this.cleanupFunction();
	};

	/**
	 * the default Pager
	 * @returns {undefined}
	 */
	this.transportFunction = function () {
		console.warning('default implementation does nothing!');
	};

	/**
	 * by default all node elements are removed
	 * specialized implementations can just override this method
	 */
	this.cleanupFunction = function () {
		$('.node', pagerObj.el).remove();
	};


	/**
	 * activate the filter elements for the pager
	 *
	 * must be called after the pager has been initialized because the
	 * filters dont necessarily exist at the time the pager is created
	 */
	this.activateFilterElements = function (filterContainer) {

		// If filterContainer is given, set as filter element. Default is the pager container itself.
		this.filterEl = filterContainer || pagerObj.pager;

		$('input.filter[type=text]', this.filterEl).each(function (idx, elem) {
			var $elem = $(elem);
			var filterAttribute = $elem.data('attribute');
			if (pagerFilters[pagerObj.type][filterAttribute]) {
				$elem.val(pagerFilters[pagerObj.type][filterAttribute]);
			}
		});

		$('input.filter[type=text]', this.filterEl).on('keyup', function(e) {
			var $filterEl = $(this);
			var filterAttribute = $filterEl.data('attribute');
			if (e.keyCode === 13) {

				if (filterAttribute && filterAttribute.length) {
					pagerFilters[pagerObj.type][filterAttribute] = $filterEl.val();

					page[pagerObj.type] = 1;
					pagerObj.updatePagerElements();
					pagerObj.transportFunction();
				}

			} else if (e.keyCode === 27) {

				pagerFilters[pagerObj.type][filterAttribute] = null;
				$filterEl.val('');

				page[pagerObj.type] = 1;
				pagerObj.updatePagerElements();
				pagerObj.transportFunction();
			}
		});

		$('input.filter[type=checkbox]', this.filterEl).each(function (idx, elem) {
			var $elem = $(elem);
			var filterAttribute = $elem.data('attribute');
			if (pagerFilters[pagerObj.type][filterAttribute]) {
				$elem.prop('checked', pagerFilters[pagerObj.type][filterAttribute]);
			}
		});

		$('input.filter[type=checkbox]', this.filterEl).on('change', function(e) {
			var $filterEl = $(this);
			var filterAttribute = $filterEl.data('attribute');

			if(filterAttribute && filterAttribute.length) {
				pagerFilters[pagerObj.type][filterAttribute] = $filterEl.prop('checked');

				page[pagerObj.type] = 1;
				pagerObj.updatePagerElements();
				pagerObj.transportFunction();
			}
		});
	};

	/**
	 * @returns the non-empty filter attributes
	 */
	this.getNonEmptyFilterAttributes = function () {
		var nonEmptyFilters = {};

		Object.keys(pagerFilters[pagerObj.type]).forEach(function(fa) {
			if (pagerFilters[pagerObj.type][fa] !== null && pagerFilters[pagerObj.type][fa] !== "") {
				nonEmptyFilters[fa] = pagerFilters[pagerObj.type][fa];
			}
		});

		return nonEmptyFilters;
	};
};