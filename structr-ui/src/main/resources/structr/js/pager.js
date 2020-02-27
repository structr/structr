/*
 * Copyright (C) 2010-2020 Structr GmbH
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

	initPager: function(id, type, p, ps, sort, order, filters) {
		_Pager.restorePagerData(id);

		if (pagerType[id] === undefined) {
			pagerType[id] = type;
		}
		if (page[id] === undefined) {
			page[id] = parseInt(p);
		}
		if (pageSize[id] === undefined) {
			pageSize[id] = parseInt(ps);
		}
		if (sortKey[id] === undefined) {
			sortKey[id] = sort;
		}
		if (sortOrder[id] === undefined) {
			sortOrder[id] = order;
		}
		if (pagerFilters[id] === undefined) {
			pagerFilters[id] = filters || {};
		}

		_Pager.storePagerData(id, type, page[id], pageSize[id], sortKey[id], sortOrder[id], pagerFilters[id]);
	},

	initFilters: function(id, type, filters) {
		pagerFilters[id] = filters;
		_Pager.storePagerData(id, type, page[id], pageSize[id], sortKey[id], sortOrder[id], pagerFilters[id]);
	},

	forceAddFilters: function(id, type, filters) {
		_Pager.initFilters(id, type, $.extend(pagerFilters[id], filters));
	},

	storePagerData: function(id, type, page, pageSize, sort, order, filters) {
		var data = {
			id: id,
			type: type,
			page: parseInt(page),
			pageSize: parseInt(pageSize),
			sort: sort,
			order: order,
			filters: filters
		};
		LSWrapper.setItem(pagerDataKey + id, JSON.stringify(data));
	},

	restorePagerData: function(id) {
		var pagerData = LSWrapper.getItem(pagerDataKey + id);
		if (pagerData) {

			if (!pagerData.startsWith('{')) {
				LSWrapper.removeItem(pagerDataKey + id);
				return false;
			}

			pagerData = JSON.parse(pagerData);
			pagerType[id] = pagerData.id;
			page[id]      = pagerData.page;
			pageSize[id]  = pagerData.pageSize;
			sortKey[id]   = pagerData.sort;
			sortOrder[id] = pagerData.order;
			if (pagerData.filters) {
				//console.log(pagerData);
				pagerFilters[id] = pagerFilters[id] || {};
				$.extend(pagerFilters[id], pagerData.filters);
				//console.log(pagerFilters[id]);
			}

			return true;
		}

		return false;
	},
	addPager: function (id, el, rootOnly, type, view, callback, optionalTransportFunction) {
		_Logger.log(_LogType.PAGER, 'add Pager', type, pageSize[id], page[id], sortKey[id], sortOrder[id]);
		var pager = new Pager(id, el, rootOnly, type, view, callback);
		pager.transportFunction = function() {
			var filterAttrs = pager.getNonEmptyFilterAttributes();
			if (typeof optionalTransportFunction === "function") {
				optionalTransportFunction(id, pageSize[id], page[id], filterAttrs, pager.internalCallback);
			} else {
				Command.query(pager.type, pageSize[id], page[id], sortKey[id], sortOrder[id], filterAttrs, pager.internalCallback, false, view);
			}
		};
		pager.init();
		return pager;
	}
};

var Pager = function (id, el, rootOnly, type, view, callback) {

	var pagerObj = this;

	// Parameters
	this.el = el;
	this.filterEl = undefined; // if set, use this as container for filters
	this.rootOnly = rootOnly;
	this.id = id;
	this.type = type;
	this.view = view;
	if (!callback) {
		this.callback = function(entities) {
			entities.forEach(function(entity) {
				StructrModel.create(entity);
			});
		};
	} else {
		this.callback = callback;
	}

	if (typeof pagerFilters[this.id] !== 'object') {
		pagerFilters[this.id] = {};
	}

	this.internalCallback = function (result, count) {

		rawResultCount[pagerObj.id] = count;
		pageCount[pagerObj.id] = Math.max(1, Math.ceil(rawResultCount[pagerObj.id] / pageSize[pagerObj.id]));
		pagerObj.updatePager(pagerObj.id, dialog.is(':visible') ? dialog : undefined);

		pagerObj.pageCount.val(pageCount[pagerObj.id]);

		pagerObj.callback(result);
	};

	this.init = function () {

		_Pager.restorePagerData(this.id);

		this.el.append('<div class="pager pager' + this.id + '" style="clear: both"><i class="pageLeft fa fa-angle-left"></i>'
				+ ' <input class="pageNo" type="text" size="4" value="' + page[this.id] + '"><i class="pageRight fa fa-angle-right"></i>'
				+ ' of <input readonly="readonly" class="readonly pageCount" type="text" size="4">'
				+ ' Items: <select class="pageSize">'
				+ '<option' + (pageSize[this.id] === 5 ? ' selected' : '') + '>5</option>'
				+ '<option' + (pageSize[this.id] === 10 ? ' selected' : '') + '>10</option>'
				+ '<option' + (pageSize[this.id] === 25 ? ' selected' : '') + '>25</option>'
				+ '<option' + (pageSize[this.id] === 50 ? ' selected' : '') + '>50</option>'
				+ '<option' + (pageSize[this.id] === 100 ? ' selected' : '') + '>100</option>'
				+ '</select></div>');

		this.pager = $('.pager' + this.id, this.el);

		this.pageLeft  = $('.pageLeft', this.pager);
		this.pageRight = $('.pageRight', this.pager);
		this.pageNo    = $('.pageNo', this.pager);
		this.pageSize  = $('.pageSize', this.pager);
		this.pageCount = $('.pageCount', this.pager);

		this.pageSize.on('change', function(e) {
                    pageSize[pagerObj.id] = $(this).val();
                    page[pagerObj.id] = 1;
                    pagerObj.updatePagerElements();
                    pagerObj.transportFunction();
		});


                let limitPager = function(inputEl) {
                    let val = $(inputEl).val();
                    if (val < 1 || val > pageCount[pagerObj.id]) {
                            $(inputEl).val(page[pagerObj.id]);
                    } else {
                        page[pagerObj.id] = val;
                    }
                    pagerObj.updatePagerElements();
                    pagerObj.transportFunction();
                };

		this.pageNo.on('keypress', function(e) {
                    if (e.keyCode === 13) {
                        limitPager(this);
                    }
		});

		this.pageNo.on('blur', function(e) {
                    if (e.target.classList.contains('disabled')) return;
                    limitPager(this);
		});

                this.pageNo.on('click', function(e) {
                    if (e.target.classList.contains('disabled')) return;
                    e.target.select();
		});

		this.pageLeft.on('click', function(e) {
                    if (e.target.classList.contains('disabled')) return;
                    page[pagerObj.id]--;
                    pagerObj.updatePagerElements();
                    pagerObj.transportFunction();
		});

		this.pageRight.on('click', function(e) {
                    if (e.target.classList.contains('disabled')) return;
                    page[pagerObj.id]++;
                    pagerObj.updatePagerElements();
                    pagerObj.transportFunction();
		});

		pagerObj.transportFunction();
	};

	/**
	 * Gets called after a new slice of data has been received
	 */
	this.updatePager = function() {

		//console.log(pageCount[this.id]);

		if (page[this.id] === 1) {
			this.pageLeft.attr('disabled', 'disabled').addClass('disabled');
		} else {
			this.pageLeft.removeAttr('disabled', 'disabled').removeClass('disabled');
		}

		if (pageCount[this.id] === 1 || (page[this.id] === pageCount[this.id])) {
			this.pageRight.attr('disabled', 'disabled').addClass('disabled');
		} else {
			this.pageRight.removeAttr('disabled', 'disabled').removeClass('disabled');
		}

		if (pageCount[this.id] === 1) {
			this.pageNo.attr('disabled', 'disabled').addClass('disabled');
		} else {
			this.pageNo.removeAttr('disabled', 'disabled').removeClass('disabled');
		}

		_Pager.storePagerData(this.id, pagerType[this.id], page[this.id], pageSize[this.id], sortKey[this.id], sortOrder[this.id], pagerFilters[this.id]);
	};

	/**
	 * Gets called whenever a change has been made (i.e. button has been pressed)
	 */
	this.updatePagerElements = function () {
		$('.pageNo', this.pager).val(page[this.id]);
		$('.pageSize', this.pager).val(pageSize[this.id]);

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
	 * filters don't necessarily exist at the time the pager is created
	 */
	this.activateFilterElements = function (filterContainer) {

		// If filterContainer is given, set as filter element. Default is the pager container itself.
		this.filterEl = filterContainer || pagerObj.pager;

		$('input.filter[type=text]', this.filterEl).each(function (idx, elem) {
			var $elem = $(elem);
			var filterAttribute = $elem.data('attribute');
			if (pagerFilters[pagerObj.id][filterAttribute]) {
				$elem.val(pagerFilters[pagerObj.id][filterAttribute]);
			}
		});

		$('input.filter[type=text]', this.filterEl).on('keyup', function(e) {
			var $filterEl = $(this);
			var filterAttribute = $filterEl.data('attribute');

			let filterVal = $filterEl.val();

			if (filterVal === '') {
				pagerFilters[pagerObj.id][filterAttribute] = null;
			} else {
				pagerFilters[pagerObj.id][filterAttribute] = filterVal;
			}

			if (e.keyCode === 13) {

				if (filterAttribute && filterAttribute.length) {
					page[pagerObj.id] = 1;
					pagerObj.updatePagerElements();
					pagerObj.transportFunction();
				}

			} else if (e.keyCode === 27) {

				pagerFilters[pagerObj.id][filterAttribute] = null;
				$filterEl.val('');

				page[pagerObj.id] = 1;
				pagerObj.updatePagerElements();
				pagerObj.transportFunction();
			}
		});

		$('input.filter[type=checkbox]', this.filterEl).each(function (idx, elem) {
			var $elem = $(elem);
			var filterAttribute = $elem.data('attribute');
			if (pagerFilters[pagerObj.id][filterAttribute]) {
				$elem.prop('checked', pagerFilters[pagerObj.id][filterAttribute]);
			}
		});

		$('input.filter[type=checkbox]', this.filterEl).on('change', function(e) {
			var $filterEl = $(this);
			var filterAttribute = $filterEl.data('attribute');

			if (filterAttribute && filterAttribute.length) {
				pagerFilters[pagerObj.id][filterAttribute] = $filterEl.prop('checked');

				page[pagerObj.id] = 1;
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

		Object.keys(pagerFilters[pagerObj.id]).forEach(function(fa) {
			if (pagerFilters[pagerObj.id][fa] !== null && pagerFilters[pagerObj.id][fa] !== "") {
				nonEmptyFilters[fa] = pagerFilters[pagerObj.id][fa];
			}
		});

		return nonEmptyFilters;
	};

	/**
	 *
	 * @param {type} key The key to sort by
	 */
	this.setSortKey = function (key) {
		if (sortKey[pagerObj.id] === key) {

			// invert sort order
			if (sortOrder[pagerObj.id] === "asc") {
				sortOrder[pagerObj.id] = "desc";
			} else {
				sortOrder[pagerObj.id] = "asc";
			}

		} else {
			sortKey[pagerObj.id] = key;
			sortOrder[pagerObj.id] = "asc";
		}

		page[pagerObj.id] = 1;
		pagerObj.updatePagerElements();
		pagerObj.transportFunction();
	};

	/**
	 * Refresh the currently displayed page
	 */
	this.refresh = function () {
		pagerObj.updatePagerElements();
		pagerObj.transportFunction();
	};
};
