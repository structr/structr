/*
 * Copyright (C) 2010-2024 Structr GmbH
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
 * Create a pager for the given type to the given DOM element.
 *
 * This pager either calls Command#list (WebSocket call) after being loaded
 * and binds Command#list to all actions or does the same for REST calls.
 *
 * If the optional callback function is given, it will be executed
 * instead of the default action.
 */
let _Pager = {
	pagerType: {},
	pageCount: {},
	page: {},
	pageSize: {},
	sortKey: {},
	sortOrder: {},
	pagerFilters: {},
	pagerForcedFilters: {},
	pagerExactFilterKeys: {},
	rawResultCount: [],
	pagerDataKey: `structrPagerData_${location.port}_`,

	initPager: (id, type, p, ps, sort, order, filters) => {
		_Pager.restorePagerData(id);

		if (_Pager.pagerType[id] === undefined) {
			_Pager.pagerType[id] = type;
		}
		if (_Pager.page[id] === undefined) {
			_Pager.page[id] = parseInt(p);
		}
		if (_Pager.pageSize[id] === undefined) {
			_Pager.pageSize[id] = parseInt(ps);
		}
		if (_Pager.sortKey[id] === undefined) {
			_Pager.sortKey[id] = sort;
		}
		if (_Pager.sortOrder[id] === undefined) {
			_Pager.sortOrder[id] = order;
		}
		if (_Pager.pagerFilters[id] === undefined) {
			_Pager.pagerFilters[id] = filters || {};
		}

		_Pager.storePagerData(id, type, _Pager.page[id], _Pager.pageSize[id], _Pager.sortKey[id], _Pager.sortOrder[id], _Pager.pagerFilters[id]);
	},

	initFilters: (id, type, filters, exactFilterKeys) => {

		_Pager.pagerFilters[id] = filters;
		_Pager.pagerExactFilterKeys[id] = exactFilterKeys;
		_Pager.storePagerData(id, type, _Pager.page[id], _Pager.pageSize[id], _Pager.sortKey[id], _Pager.sortOrder[id], _Pager.pagerFilters[id]);
	},

	forceAddFilters: (id, type, filters) => {

		_Pager.pagerForcedFilters[id] = Object.assign({}, _Pager.pagerForcedFilters[id], filters);
		_Pager.initFilters(id, type, Object.assign({}, _Pager.pagerFilters[id], filters));
	},

	storePagerData: (id, type, page, pageSize, sort, order, filters) => {

		// do not include forced filters so they can be removed in later versions without persisting in localstorage
		let data = {
			id: id,
			type: type,
			page: parseInt(page),
			pageSize: parseInt(pageSize),
			sort: sort,
			order: order,
			filters: filters
		};
		LSWrapper.setItem(_Pager.pagerDataKey + id, JSON.stringify(data));
	},

	restorePagerData: (id) => {

		let pagerData = LSWrapper.getItem(_Pager.pagerDataKey + id);
		if (pagerData) {

			if (!pagerData.startsWith('{')) {
				LSWrapper.removeItem(_Pager.pagerDataKey + id);
				return false;
			}

			pagerData = JSON.parse(pagerData);
			_Pager.pagerType[id] = pagerData.id;
			_Pager.page[id]      = pagerData.page;
			_Pager.pageSize[id]  = pagerData.pageSize;
			_Pager.sortKey[id]   = pagerData.sort;
			_Pager.sortOrder[id] = pagerData.order;
			if (pagerData.filters) {
				_Pager.pagerFilters[id] = _Pager.pagerFilters[id] || {};
				_Pager.pagerFilters[id] = Object.assign(_Pager.pagerFilters[id], pagerData.filters);
			}

			return true;
		}

		return false;
	},
	addPager: (id, el, rootOnly, type, view, callback, optionalTransportFunction, customView, startPaused, useHTTP = false) => {

		let pager = new Pager(id, el, rootOnly, type, view, callback, startPaused);

		pager.transportFunction = () => {

			let filterAttrs = pager.getNonEmptyFilterAttributes();

			let isExactPager = false;

			if (_Pager.pagerExactFilterKeys[id]) {

				let keysToFilter = Object.keys(filterAttrs);
				let inExactKeys  = keysToFilter.filter((k) => !_Pager.pagerExactFilterKeys[id].includes(k));

				isExactPager = (inExactKeys.length === 0);
			}

			if (pager.isPaused() === false) {

				if (typeof optionalTransportFunction === "function") {

					optionalTransportFunction(id, _Pager.pageSize[id], _Pager.page[id], filterAttrs, pager.internalCallback);

				} else {

					// Filter out the special page named __ShadowDocument__ to allow displaying hidden pages in the admin UI
					if (pager.type === 'Page') {
						filterAttrs['!type'] = 'ShadowDocument';
					}

					if (useHTTP === false) {

						Command.query(pager.type, _Pager.pageSize[id], _Pager.page[id], _Pager.sortKey[id], _Pager.sortOrder[id], filterAttrs, pager.internalCallback, isExactPager, view, customView);

					} else {

						let prefix = (Structr.legacyRequestParameters === true) ? '' : '_';
						let params = Object.assign({
							[prefix + 'sort']:     _Pager.sortKey[id],
							[prefix + 'order']:    _Pager.sortOrder[id],
							[prefix + 'pageSize']: _Pager.pageSize[id],
							[prefix + 'page']:     _Pager.page[id],
							[prefix + 'loose']:    1
						}, filterAttrs);

						let url = Structr.rootUrl + pager.getType() + '/' + view + '?' + new URLSearchParams(params).toString();
						let fetchConfig = {};

						if (customView) {
							fetchConfig['headers'] = {
								'Accept': 'application/json; properties=' + customView
							};
						}

						fetch(url, fetchConfig).then(response => response.json()).then(data => {

							let resultCount = data.result_count;

							// handle new soft-limited REST result without counts
							if (data.result_count === undefined && data.page_count === undefined) {
								resultCount = _Helpers.softlimit.getSoftLimitedResultCount();
								_Helpers.softlimit.showSoftLimitAlert($('input.pageCount'));
							}

							pager.internalCallback(data.result, resultCount);
						});
					}
				}
			}
		};

		pager.init();

		return pager;
	}
};

let Pager = function(id, el, rootOnly, type, view, callback, startPaused = false) {

	// Parameters
	this.el       = el;
	this.rootOnly = rootOnly;
	this.id       = id;
	this.type     = type;
	this.view     = view;
	this.paused   = startPaused;

	if (!callback) {
		this.callback = (entities) => {
			for (let entity of entities) {
				StructrModel.create(entity);
			}
		};
	} else {
		this.callback = callback;
	}

	if (typeof _Pager.pagerFilters[this.id] !== 'object') {
		_Pager.pagerFilters[this.id] = {};
	}

	this.getType = () => {
		return this.type;
	}

	this.isPaused = () => {
		return this.paused;
	};

	this.setIsPaused = (paused) => {
		this.paused = paused;
	}

	this.internalCallback = (result, count) => {

		_Pager.rawResultCount[this.id] = count;
		_Pager.pageCount[this.id] = Math.max(1, Math.ceil(_Pager.rawResultCount[this.id] / _Pager.pageSize[this.id]));
		this.pageCount.value = _Pager.pageCount[this.id];

		if (_Pager.page[this.id] < 1) {
			_Pager.page[this.id] = 1;
			this.pageNo.value = _Pager.page[this.id];
		}

		if (_Pager.page[this.id] > _Pager.pageCount[this.id]) {
			_Pager.page[this.id] = _Pager.pageCount[this.id];
			this.pageNo.value = _Pager.page[this.id];
		}

		this.updatePager();

		this.callback(result);
	};

	this.init = () => {

		_Pager.restorePagerData(this.id);

		let pagerHtml = `
			<div class="pager pager${this.id} flex items-center">
				<button class="pageLeft flex" style="margin: 0! important; padding: 0.5rem 0.25rem; background: transparent;">
					${_Icons.getSvgIcon(_Icons.iconChevronLeft)}
				</button>
				<span class="pageWrapper">
					<input class="pageNo" value="${_Pager.page[this.id]}">
					<span class="of">of</span>
					<input readonly="readonly" class="readonly pageCount" type="text" size="2">
				</span>
				<button class="pageRight flex" style="margin: 0! important; padding: 0.5rem 0.25rem; background: transparent;">
					${_Icons.getSvgIcon(_Icons.iconChevronRight)}
				</button>
				<span class="ml-2 mr-1">Items:</span>
				<select class="pageSize mr-4 hover:bg-gray-100 focus:border-gray-666 active:border-green">
					${[5, 10, 25, 50, 100].map((pageSize) => `<option${(_Pager.pageSize[this.id] === pageSize ? ' selected' : '')}>${pageSize}</option>`).join('')}
				</select>
			</div>
		`;

		this.el.insertAdjacentHTML('beforeend', pagerHtml);

		this.pager     = this.el.querySelector(`.pager${this.id}`);
		this.pageLeft  = this.pager.querySelector('.pageLeft');
		this.pageRight = this.pager.querySelector('.pageRight');
		this.pageNo    = this.pager.querySelector('.pageNo');
		this.pageSize  = this.pager.querySelector('.pageSize');
		this.pageCount = this.pager.querySelector('.pageCount');

		this.pageSize.addEventListener('change', (e) => {
			_Pager.pageSize[this.id] = this.pageSize.value;
			_Pager.page[this.id]     = 1;

			this.updatePagerElements();
			this.transportFunction();
		});

		let limitPager = (inputEl) => {
			let val = parseInt(inputEl.value);
			if (val < 1 || val > _Pager.pageCount[this.id]) {
				inputEl.value = _Pager.page[this.id];
			} else {
				_Pager.page[this.id] = val;
			}
			this.updatePagerElements();
			this.transportFunction();
		};

		this.pageNo.addEventListener('keypress', (e) => {
			if (e.keyCode === 13) {
				// prevent change event from reloading same content
				this.pageNo.classList.add('disabled');
				limitPager(this.pageNo);
			}
		});

		this.pageNo.addEventListener('change', (e) => {
			if (this.pageNo.classList.contains('disabled')) return;
			limitPager(this.pageNo);
		});

		this.pageNo.addEventListener('click', (e) => {
			if (this.pageNo.classList.contains('disabled')) return;
			this.pageNo.select();
		});

		this.pageLeft.addEventListener('click', (e) => {
			if (this.pageLeft.classList.contains('disabled') || _Pager.page[this.id] === 1) return;
			_Pager.page[this.id]--;
			this.updatePagerElements();
			this.transportFunction();
		});

		this.pageRight.addEventListener('click', (e) => {
			if (this.pageRight.classList.contains('disabled')) return;
			_Pager.page[this.id]++;
			this.updatePagerElements();
			this.transportFunction();
		});

		this.transportFunction();
	};

	/**
	 * Gets called after a new slice of data has been received
	 */
	this.updatePager = () => {

		_Helpers.disableElements((_Pager.page[this.id] === 1), this.pageLeft);
		_Helpers.disableElements((_Pager.pageCount[this.id] === 1 || (_Pager.page[this.id] === _Pager.pageCount[this.id])), this.pageRight);
		_Helpers.disableElements((_Pager.pageCount[this.id] === 1), this.pageNo);

		_Pager.storePagerData(this.id, _Pager.pagerType[this.id], _Pager.page[this.id], _Pager.pageSize[this.id], _Pager.sortKey[this.id], _Pager.sortOrder[this.id], _Pager.pagerFilters[this.id]);
	};

	/**
	 * Gets called whenever a change has been made (e.g. a button has been pressed)
	 */
	this.updatePagerElements = () => {

		this.pageNo.value   = _Pager.page[this.id];
		this.pageSize.value = _Pager.pageSize[this.id];

		this.cleanupFunction();
	};

	/**
	 * the default Pager
	 * @returns {undefined}
	 */
	this.transportFunction = () => {
		// default implementation does nothing!
	};

	/**
	 * by default all node elements are removed
	 * specialized implementations can just override this method
	 */
	this.cleanupFunction = () => {
		for (let node of this.el.querySelectorAll('.node')) {
			_Helpers.fastRemoveElement(node);
		}
	};

	this.appendFilterElements = (markup) => {
		this.pager.insertAdjacentHTML('beforeend', markup);
	};

	/**
	 * activate the filter elements for the pager
	 *
	 * must be called after the pager has been initialized because the
	 * filters don't necessarily exist at the time the pager is created
	 */
	this.activateFilterElements = (filterContainer = this.pager) => {

		let foundFilters = [];

		let textFilters = filterContainer.querySelectorAll('input.filter[type=text]');

		for (let textFilter of textFilters) {

			let filterAttribute = textFilter.dataset['attribute'];

			foundFilters.push(filterAttribute);

			if (_Pager.pagerFilters[this.id][filterAttribute]) {
				textFilter.value = _Pager.pagerFilters[this.id][filterAttribute];
			}

			textFilter.addEventListener('keyup', (e) => {

				if (e.keyCode === 13) {

					if (filterAttribute && filterAttribute.length) {

						let filterVal = textFilter.value;

						if (filterVal === '') {
							_Pager.pagerFilters[this.id][filterAttribute] = null;
						} else {
							_Pager.pagerFilters[this.id][filterAttribute] = filterVal;
						}

						_Pager.page[this.id] = 1;
						_Pager.pagerFilters[this.id][filterAttribute] = filterVal;
						this.updatePagerElements();
						this.transportFunction();
					}

				} else if (e.keyCode === 27) {

					// allow ESC in pagers in dialogs (do not close dialog on ESC while focus in input)
					e.preventDefault();
					e.stopPropagation();

					_Pager.pagerFilters[this.id][filterAttribute] = null;
					textFilter.value = '';

					_Pager.page[this.id] = 1;
					this.updatePagerElements();
					this.transportFunction();
				}
			});

			textFilter.addEventListener('blur', (e) => {

				let filterVal       = textFilter.value;
				let lastFilterValue = _Pager.pagerFilters[this.id][filterAttribute];

				if (filterVal === '') {
					_Pager.pagerFilters[this.id][filterAttribute] = null;
				} else {
					_Pager.pagerFilters[this.id][filterAttribute] = filterVal;
				}

				if (filterAttribute && filterAttribute.length) {

					if (lastFilterValue !== filterVal && !(filterVal === '' && lastFilterValue === null)) {
						_Pager.page[this.id] = 1;
						this.updatePagerElements();
						this.transportFunction();
					}

				} else {

					_Pager.pagerFilters[this.id][filterAttribute] = null;
					textFilter.value = '';

					_Pager.page[this.id] = 1;
					this.updatePagerElements();
					this.transportFunction();
				}
			})
		}

		let boolFilters = filterContainer.querySelectorAll('input.filter[type=checkbox]');

		for (let boolFilter of boolFilters) {

			let filterAttribute = boolFilter.dataset['attribute'];

			foundFilters.push(filterAttribute);

			if (_Pager.pagerFilters[this.id][filterAttribute]) {
				boolFilter.checked = _Pager.pagerFilters[this.id][filterAttribute];
			}

			boolFilter.addEventListener('change', (e) => {

				if (filterAttribute && filterAttribute.length) {

					_Pager.pagerFilters[this.id][filterAttribute] = boolFilter.checked;

					_Pager.page[this.id] = 1;
					this.updatePagerElements();
					this.transportFunction();
				}
			});
		}

		// remove filters which are either removed or temporarily disabled due to database limitations
		// for example resource access grants filtering for "active" grants
		let storedKeys = Object.keys(_Pager.pagerFilters[this.id]);
		let forcedKeys = Object.keys(Object.assign({}, _Pager.pagerForcedFilters[this.id]));
		let update = false;

		for (let key of storedKeys) {

			let isUiFilter     = (foundFilters.indexOf(key) > -1);
			let isForcedFilter = (forcedKeys.indexOf(key) > -1);

			if (!isUiFilter && !isForcedFilter) {
				update = true;
				delete _Pager.pagerFilters[this.id][key];
			}
		}

		if (update) {
			this.transportFunction();
		}
	};

	/**
	 * @returns the non-empty filter attributes
	 */
	this.getNonEmptyFilterAttributes = () => {

		let nonEmptyFilters = {};

		for (let filterAttribute in _Pager.pagerFilters[this.id]) {
			if (_Pager.pagerFilters[this.id][filterAttribute] !== null && _Pager.pagerFilters[this.id][filterAttribute] !== "") {
				nonEmptyFilters[filterAttribute] = _Pager.pagerFilters[this.id][filterAttribute];
			}
		}

		return nonEmptyFilters;
	};

	/**
	 *
	 * @param {type} key The key to sort by
	 */
	this.setSortKey = (key) => {

		if (_Pager.sortKey[this.id] === key) {

			// invert sort order
			if (_Pager.sortOrder[this.id] === "asc") {
				_Pager.sortOrder[this.id] = "desc";
			} else {
				_Pager.sortOrder[this.id] = "asc";
			}

		} else {
			_Pager.sortKey[this.id] = key;
			_Pager.sortOrder[this.id] = "asc";
		}

		_Pager.page[this.id] = 1;
		this.updatePagerElements();
		this.transportFunction();
	};

	/**
	 * Refresh the currently displayed page
	 */
	this.refresh = () => {
		this.updatePagerElements();
		this.transportFunction();
	};
};
