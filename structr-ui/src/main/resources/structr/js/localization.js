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
$(document).ready(function() {
	Structr.registerModule(_Localization);
});

var _Localization = {
	_moduleName: 'localization',

	keysAndDomainsList: undefined,
	keyAndDomainPager: undefined,
	localizationDetails: undefined,
	localizationDetailKey: undefined,
	localizationDetailDomain: undefined,
	localizationDetailEditButton: undefined,
	localizationDetailSaveButton: undefined,
	localizationDetailDiscardButton: undefined,
	localizationDetailList: undefined,

	localizationSelectedElementKey: 'structrLocalizationSelectedElementKey_' + port,
	localizationPreselectLocaleKey: 'structrLocalizationPreselectLocaleKey_' + port,
	localizationResizerLeftKey: 'structrLocalizationResizerLeftKey_' + port,

	init: function() {
		main = $('#main');
	},
	resize: function() {
		_Localization.moveResizer();
		Structr.resize();
	},
	onload: function() {
		_Localization.init();

		Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('localization'));

		Structr.fetchHtmlTemplate('localization/main', {}, function (html) {

			main.append(html);

			document.getElementById('add-new-translation').addEventListener('click', (event) => {
				event.preventDefault();
				_Localization.createNewLocalizationEntry();
			});

			Structr.fetchHtmlTemplate('localization/functions', {}, function (html) {
				functionBar.append(html);

				let localePreselect = document.getElementById('localization-locale-preselect');
				localePreselect.value = LSWrapper.getItem(_Localization.localizationPreselectLocaleKey) || 'en';

				document.getElementById('create-localization-key').addEventListener('click', () => {
					_Localization.showMain();
					_Localization.createNewLocalizationKey({ name: 'localization-key-' + Math.floor(Math.random() * 999999) + 1 });
				});

				_Localization.keysAndDomainsList = $('#localization-table tbody');
				_Localization.listKeysAndDomains();

				_Localization.localizationDetails = $('#localization-detail');
				// _Localization.localizationDetails.hide();
				_Localization.localizationDetailKey = $('#localization-key');
				_Localization.localizationDetailKey.on('keyup', _Localization.determineKeyFieldValidity);
				_Localization.localizationDetailDomain = $('#localization-domain');
				_Localization.localizationDetailEditButton = $('#localization-fields-edit').on('click', _Localization.editButtonAction);
				_Localization.localizationDetailSaveButton = $('#localization-fields-save').hide().on('click', _Localization.saveButtonAction);
				_Localization.localizationDetailDiscardButton = $('#localization-fields-discard').hide().on('click', _Localization.discardButtonAction);
				_Localization.localizationsDetailList = $('#localization-detail-table tbody');

				Structr.unblockMenu(100);

				_Localization.moveResizer();
				Structr.initVerticalSlider($('.column-resizer', main), _Localization.localizationResizerLeftKey, 340, _Localization.moveResizer);

				_Localization.resize();
			});
		});
	},
	unload: function() {

	},
	moveResizer: function(left) {

		requestAnimationFrame(() => {

			left = left || LSWrapper.getItem(_Localization.localizationResizerLeftKey) || 340;
			left = Math.max(300, Math.min(left, window.innerWidth - 300));

			document.querySelector('.column-resizer').style.left = left + 'px';

			let listContainer = document.getElementById('localization-list-container');
			listContainer.style.width = 'calc(' + left + 'px - 1rem)';

			let detailContainer = document.getElementById('localization-detail-container');
			detailContainer.style.width = 'calc(100% - ' + left + 'px - 3rem)';

			return true;
		});
	},
	listKeysAndDomains: function () {

		let pagerEl = $('#localization-pager');

		_Pager.initPager('localizations', 'Localization', 1, 25, 'name', 'asc');

		_Localization.keyAndDomainPager = _Pager.addPager('localizations', pagerEl, false, 'Localization', 'ui', _Localization.processPagerData, _Localization.customPagerTransportFunction);

		_Localization.keyAndDomainPager.cleanupFunction = _Localization.clearLocalizationsList;
		_Localization.keyAndDomainPager.pager.append('Filters: <input type="text" class="filter w75 localization-key" data-attribute="name" placeholder="Key">');
		_Localization.keyAndDomainPager.pager.append('<input type="text" class="filter w75 localization-domain" data-attribute="domain" placeholder="Domain">');
		_Localization.keyAndDomainPager.pager.append('<input type="text" class="filter w75 localization-text" data-attribute="localizedName" placeholder="Content">');
		_Localization.keyAndDomainPager.activateFilterElements();

		pagerEl.append('<div style="clear:both;"></div>');

		$('#localization-table .sort').on('click', function () {
			_Localization.keyAndDomainPager.setSortKey($(this).data('sort'));
		});

		let lastSelectedLocalizationElement = LSWrapper.getItem(_Localization.localizationSelectedElementKey);
		if (!lastSelectedLocalizationElement) {
			let rows = document.querySelectorAll('.localization-row');
			if (rows && rows.length !== 0) {
				rows[0].click();
			}
		} else {
			_Localization.showLocalizationsForKeyAndDomainObject(lastSelectedLocalizationElement);
		}
	},
//	customPagerTransportFunction: (id, pageSize, page, filterAttrs, callback) => {
//      // !!! this does not work because after pagination is applied, it reduces the number of objects... thus breaking pagination
//		Command.query('Localization', 100000, 1, sortKey[id], sortOrder[id], filterAttrs, (result) => {
//			const resultCount = result.length;
//			Command.query('Localization', pageSize, page, sortKey[id], sortOrder[id], filterAttrs, (result) => {
//				const uniqueResult = [...new Map(result.map(res => [res['name'], res])).values()];
//				if (callback) callback(uniqueResult, resultCount);
//			}, false, 'ui');
//		});
//	},
	customPagerTransportFunction: function(type, pageSize, page, filterAttrs, callback) {
		var filterString = "";
		var presentFilters = Object.keys(filterAttrs);
		if (presentFilters.length > 0) {
			filterString = 'WHERE ' + presentFilters.map(function(key) { return 'n.' + key + ' =~ "(?i).*' + filterAttrs[key] + '.*"'; }).join(' AND ');
		}
		Command.cypher('MATCH (n:Localization) ' + filterString + ' RETURN DISTINCT {name: n.name, domain: n.domain} as res ORDER BY res.' + sortKey[type] + ' ' + sortOrder[type], undefined, callback, pageSize, page);
	},
	processPagerData: (pagerData) => {
		if (pagerData && pagerData.length) {
			pagerData.forEach(_Localization.appendKeyAndDomainListRow);
		}
	},
	showMain: () => {
		document.getElementById('localization-main').style.display = 'flex';
		_Localization.moveResizer();
	},
	hideMain: () => {
		document.getElementById('localization-main').style.display = 'none';
	},
	checkMainVisibility: () => {
		let rows = document.querySelectorAll('.localization-row');
		let selectedRowExists = false;
		rows.forEach((row) => {
			selectedRowExists |= row.classList.contains('selected');
		});
		if (!rows || rows.length === 0) {
			_Localization.hideMain();
		} else if (!selectedRowExists) {
			rows[0].click();
		}
	},
	getContextMenuElements: function (div, keyAndDomainObject) {

		let elements = [];

		elements.push({
			name: 'Edit',
			clickHandler: function() {
				_Localization.showLocalizationsForKeyAndDomainObject(keyAndDomainObject);
				return false;
			}
		});

		_Elements.appendContextMenuSeparator(elements);

		elements.push({
			icon: _Icons.svg.trashcan,
			classes: ['menu-bolder', 'danger'],
			name: 'Delete Localization',
			clickHandler: () => {

				if (true === confirm('Do you really want to delete the complete localizations for "' + keyAndDomainObject.name + '"' + (keyAndDomainObject.domain ? ' in domain "' + keyAndDomainObject.domain + '"' : ' with empty domain') + ' ?')) {
					_Localization.deleteCompleteLocalization((keyAndDomainObject.name ? keyAndDomainObject.name : null), (keyAndDomainObject.domain ? keyAndDomainObject.domain : null), this);
				}

				return false;
			}
		});

		_Elements.appendContextMenuSeparator(elements);

		return elements;
	},
	selectRow: (row) => {

		for (let row of document.querySelectorAll('.localization-row')) {
			row.classList.remove('selected');
		}

		row.classList.add('selected');
	},
	appendKeyAndDomainListRow: (keyAndDomainObject) => {

		_Localization.showMain();

		let combinedTypeForId = _Localization.getCombinedTypeForId(keyAndDomainObject);
		keyAndDomainObject.htmlId = combinedTypeForId;

		Structr.fetchHtmlTemplate('localization/row.type', { localization: keyAndDomainObject }, function(html) {

			let row = $(html);
			_Localization.keysAndDomainsList.append(row);
			let actionsCol = $('.actions', row);

			row[0].addEventListener('click', () => {
				_Localization.selectRow(row[0]);
				_Localization.showLocalizationsForKeyAndDomainObject(keyAndDomainObject);
			});

			_Elements.enableContextMenuOnElement(row, keyAndDomainObject);
			_Entities.appendEditPropertiesIcon(actionsCol, keyAndDomainObject, true);

			let previouslySelectedElement = LSWrapper.getItem(_Localization.localizationSelectedElementKey);
			if (previouslySelectedElement && previouslySelectedElement.htmlId === row[0].id) {
				row.click();
			}
		});
	},
	getCombinedTypeForId: (keyAndDomainObject) => {
		let key    = (keyAndDomainObject.name ? keyAndDomainObject.name : null);
		let domain = (keyAndDomainObject.domain ? keyAndDomainObject.domain : null);

		let combinedTypeForId = 'localization-' + (key ? key : 'nullKey') + '___' + (domain ? domain : 'nullDomain');
		return combinedTypeForId;
	},
	showLocalizationsForKeyAndDomainObject: (keyAndDomainObject, isCreate) => {

		let key    = (keyAndDomainObject.name ? keyAndDomainObject.name : null);
		let domain = (keyAndDomainObject.domain ? keyAndDomainObject.domain : null);

		let combinedTypeForId = _Localization.getCombinedTypeForId(keyAndDomainObject);
		keyAndDomainObject.htmlId = combinedTypeForId;

		LSWrapper.setItem(_Localization.localizationSelectedElementKey, keyAndDomainObject);

		$.ajax({
			url: rootUrl + 'Localizations/all?' + (key ? 'name=' + key : '') + (domain ? '&domain=' + domain : '') + '&' + Structr.getRequestParameterName('sort') + '=locale',
			success: function(data) {

				_Localization.localizationDetailKey.val(key).removeData('oldValue').data('oldValue', key);
				_Localization.determineKeyFieldValidity();
				_Localization.localizationDetailDomain.val(domain).removeData('oldValue').data('oldValue', domain);
				_Localization.setLocalizationKeyAndDomainEditMode(true);
				_Localization.clearLocalizationDetailsList();
				_Localization.localizationDetails.show();

				for (let loc of data.result) {
					if (key === loc.name && domain === loc.domain) {
						_Localization.appendLocalizationDetailListRow(loc);
					}
				}

				if (isCreate === true) {
					let localizationKey = document.getElementById('localization-key');
					localizationKey.focus();
					localizationKey.select();

					_Localization.keyAndDomainPager.refresh();
				}
			}
		});
	},
	clearLocalizationsList: function () {
		fastRemoveAllChildren(_Localization.keysAndDomainsList[0]);
	},
	appendLocalizationDetailListRow: (locale) => {
		Structr.fetchHtmlTemplate('localization/empty-row', {}, function(html) {
			let $tr = _Localization.appendEmptyLocalizationRow(html);
			_Localization.fillLocalizationRow($tr, locale);
		});
	},
	clearLocalizationDetailsList: function () {
		fastRemoveAllChildren(_Localization.localizationsDetailList[0]);
	},
	editButtonAction: function () {
		_Localization.unlockKeyAndDomain();
		_Localization.setLocalizationKeyAndDomainEditMode(true);
	},
	saveButtonAction: (e) => {

		let saveButton = e.target.closest('.btn');
		var oldKey = _Localization.localizationDetailKey.data('oldValue');
		var curKey = _Localization.localizationDetailKey.val().trim();
		var oldDomain = _Localization.localizationDetailDomain.data('oldValue');
		var curDomain = _Localization.localizationDetailDomain.val().trim();
		if (curDomain === '') {
			curDomain = null;
		}
		if ((oldKey !== curKey) || (oldDomain !== curDomain)) {
			if (_Localization.isFieldNonEmpty(_Localization.localizationDetailKey)) {

				var newData = {
					name: curKey,
					domain: curDomain
				};

				$.ajax({
					url: rootUrl + 'Localizations/ui?' + (oldKey ? 'name=' + oldKey : '') + (oldDomain ? '&domain=' + oldDomain : '') + '&' + Structr.getRequestParameterName('sort') + '=locale',
					success: function(data) {
						var totalCounter = 0;
						var finishedCounter = 0;

						data.result.forEach(function (loc) {
							if (oldKey === loc.name && oldDomain === loc.domain) {
								totalCounter++;

								$.ajax({
									url: rootUrl + 'Localizations/' + loc.id,
									type: 'PUT',
									dataType: 'json',
									data: JSON.stringify(newData),
									contentType: 'application/json; charset=utf-8',
									success: function() {
										blinkGreen($('#loc_' + loc.id + ' td'));
										finishedCounter++;
										if (finishedCounter === totalCounter) {
											// _Localization.solidifyKeyAndDomain(curKey, curDomain);
											_Localization.keyAndDomainPager.refresh();
										}
										blinkGreen(saveButton);
									},
									error: function () {
										blinkRed($('#loc_' + loc.id + ' td'));
									}
								});
							}
						});
					}
				});
			} else {
				_Localization.keyFieldErrorAction();
			}
		}
	},
	discardButtonAction: function () {
		_Localization.localizationDetailKey.val(_Localization.localizationDetailKey.data('oldValue'));
		_Localization.determineKeyFieldValidity();
		_Localization.localizationDetailDomain.val(_Localization.localizationDetailDomain.data('oldValue'));
	},
	keyFieldErrorAction: function () {
		_Localization.localizationDetailKey.focus();
		blinkRed(_Localization.localizationDetailKey);
	},
	setLocalizationKeyAndDomainEditMode: function (isEdit) {
		if (isEdit) {
			_Localization.localizationDetailEditButton.hide();
			_Localization.localizationDetailSaveButton.show();
			_Localization.localizationDetailDiscardButton.show();
		} else {
			_Localization.localizationDetailEditButton.show();
			_Localization.localizationDetailSaveButton.hide();
			_Localization.localizationDetailDiscardButton.hide();
		}
	},
	determineKeyFieldValidity: function () {
		_Localization.reactToFieldValidity(_Localization.localizationDetailKey, _Localization.isFieldNonEmpty(_Localization.localizationDetailKey));
	},
	determineLocaleFieldValidity: function ($el) {
		_Localization.reactToFieldValidity($el, _Localization.isFieldNonEmpty($el));
	},
	isFieldNonEmpty: function ($field) {
		return ($field.val().trim() !== "");
	},
	reactToFieldValidity: function ($field, valid) {
		if (!valid) {
			$field.addClass('invalid');
		} else {
			$field.removeClass('invalid');
		}
	},
	showEmptyCreateLocalizationDialog: function () {
		_Localization.clearLocalizationDetailsList();
		_Localization.localizationDetails.show();

		_Localization.localizationDetailEditButton.hide();

		_Localization.localizationDetailKey.val("").removeData('oldValue');
		_Localization.determineKeyFieldValidity();
		_Localization.localizationDetailDomain.val("").removeData('oldValue');

		_Localization.unlockKeyAndDomain();
	},
	appendEmptyLocalizationRow: function (rowHtml) {
		_Localization.localizationsDetailList.append(rowHtml);
		var $row = $('tr:last', _Localization.localizationsDetailList);
		var $localeField = $('.___locale', $row);
		$localeField.on('keyup', function () {
			_Localization.determineLocaleFieldValidity($localeField);
		});
		_Localization.determineLocaleFieldValidity($localeField);

		return $row;
	},
	fillLocalizationRow: function ($row, localization) {
		$row.attr('id', 'loc_' + localization.id);

		// $('td:eq(0)', $row).text(localization.id);

		var $localeField = $('.___locale', $row);
		$localeField.val(localization.locale)
			.data('oldValue', localization.locale)
			.on('blur', function (event) {
				_Localization.textfieldChangeAction($(event.target), localization.id, 'locale');
			});
		_Localization.determineLocaleFieldValidity($localeField);

		$('.___localizedName', $row)
			.val(localization.localizedName)
			.data('oldValue', localization.localizedName)
			.on('blur', function (event) {
				_Localization.textfieldChangeAction($(event.target), localization.id, 'localizedName');
			});

		$('.___description', $row)
			.val(localization.description)
			.data('oldValue', localization.description)
			.on('blur', function (event) {
				_Localization.textfieldChangeAction($(event.target), localization.id, 'description');
			});

		$('.___visibleToPublicUsers', $row)
			.prop('checked', (localization.visibleToPublicUsers === true))
			.attr('disabled', null)
			.data('oldValue', localization.visibleToPublicUsers)
			.on('change', function (event) {
				_Localization.checkboxChangeAction($(event.target), localization.id, 'visibleToPublicUsers');
			});

		$('.___visibleToAuthenticatedUsers', $row)
			.prop('checked', (localization.visibleToAuthenticatedUsers === true))
			.attr('disabled', null).data('oldValue', localization.visibleToAuthenticatedUsers)
			.on('change', function (event) {
				_Localization.checkboxChangeAction($(event.target), localization.id, 'visibleToAuthenticatedUsers');
			});

		$('td.actions', $row).html('<a title="Delete" class="delete"><svg viewBox="0 0 24 24" height="24" width="24" xmlns="http://www.w3.org/2000/svg"><g transform="matrix(1,0,0,1,0,0)"><path d="M17.25,21H6.75a1.5,1.5,0,0,1-1.5-1.5V6h13.5V19.5A1.5,1.5,0,0,1,17.25,21Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M9.75 16.5L9.75 10.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M14.25 16.5L14.25 10.5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M2.25 6L21.75 6" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path><path d="M14.25,3H9.75a1.5,1.5,0,0,0-1.5,1.5V6h7.5V4.5A1.5,1.5,0,0,0,14.25,3Z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"></path></g></svg></a>');

		$('#loc_' + localization.id + ' td.actions .delete').on('click', function(event) {
			event.preventDefault();

			if (true === confirm('Really delete localization "' + (localization.localizedName || localization.id) + '" ?')) {
				_Localization.deleteSingleLocalization(localization.id, function () {
					$('#loc_' + localization.id, _Localization.localizationsDetailList).remove();

					if ($('tr', _Localization.localizationsDetailList).length === 0) {
						_Localization.keyAndDomainPager.refresh();
					}
				});
			}
		});
	},
	textfieldChangeAction: function ($el, id, attr) {
		var oldValue = $el.data('oldValue');
		var curValue = $el.val();
		if (oldValue !== curValue) {
			_Localization.updateLocalization(id, attr, curValue, oldValue, $el);
		}
	},
	checkboxChangeAction: function ($el, id, attr) {
		var oldValue = $el.data('oldValue');
		var curValue = $el.prop('checked');
		if (oldValue !== curValue) {
			_Localization.updateLocalization(id, attr, curValue, oldValue, $el, $el.parent());
		}
	},
	updateLocalization:function (id, attr, curValue, oldValue, $el, $blinkTarget) {
		var newData = {};
		newData[attr] = curValue;

		$.ajax({
			url: rootUrl + 'Localizations/' + id,
			type: 'PUT',
			dataType: 'json',
			data: JSON.stringify(newData),
			contentType: 'application/json; charset=utf-8',
			success: function() {

				$el.data('oldValue', curValue);
				blinkGreen(($blinkTarget ? $blinkTarget : $el));
			},
			error: function () {
				if ($el.attr('type') === 'checkbox') {
					$el.prop('checked', oldValue);
				} else {
					$el.val(oldValue);
				}
				_Localization.determineLocaleFieldValidity($el);

				blinkRed(($blinkTarget ? $blinkTarget : $el));
			}
		});
	},
	createNewLocalizationKey: (newData) => {

		let presetLocalesString = document.getElementById('localization-locale-preselect').value;
		LSWrapper.setItem(_Localization.localizationPreselectLocaleKey, presetLocalesString);

		let preselectLocales = presetLocalesString.split(',').map((l) => l.trim());

		Promise.all(
			preselectLocales.map((locale) => {
				newData.locale = locale;

				return fetch(rootUrl + 'Localization', {
					method: 'POST',
					body: JSON.stringify(newData)
				});
			})
		).then((done) => {
			_Localization.showLocalizationsForKeyAndDomainObject(newData, true);
		});
	},
	createNewLocalizationEntry: () => {

		Structr.fetchHtmlTemplate('localization/empty-row', {}, function(html) {

			let $tr = _Localization.appendEmptyLocalizationRow(html);
			let trElement = $tr[0];

			$('input[type=checkbox]', $tr).attr('disabled', 'disabled');

			trElement.querySelector('td.actions .discard').addEventListener('click', function(event) {
				event.preventDefault();
				$tr.remove();
			});

			trElement.querySelector('td.actions .save').addEventListener('click', function(event) {
				event.preventDefault();

				if (_Localization.isFieldNonEmpty(_Localization.localizationDetailKey)) {

					var newData = {
						name: _Localization.localizationDetailKey.data('oldValue') || _Localization.localizationDetailKey.val().trim(),
						locale: $('.___locale', $tr).val().trim(),
						localizedName: $('.___localizedName', $tr).val(),
						description: $('.___description', $tr).val(),
						visibleToPublicUsers: $('.___visibleToPublicUsers', $tr).prop('checked'),
						visibleToAuthenticatedUsers: $('.___visibleToAuthenticatedUsers', $tr).prop('checked')
					};

					newData.domain = _Localization.localizationDetailDomain.data('oldValue') || _Localization.localizationDetailDomain.val().trim();
					if (newData.domain.trim() === "") {
						newData.domain = null;
					}

					$.ajax({
						url: rootUrl + 'Localizations',
						type: 'POST',
						dataType: 'json',
						data: JSON.stringify(newData),
						contentType: 'application/json; charset=utf-8',
						success: function(data) {
							// _Localization.solidifyKeyAndDomain(newData.name, newData.domain);

							newData.id = data.result[0];
							_Localization.fillLocalizationRow($tr, newData);

							_Localization.keyAndDomainPager.refresh();
						},
						error: function (data) {
							blinkRed($('td', $tr));
						}
					});

				} else {
					_Localization.keyFieldErrorAction();
				}
			});
		});
	},
	deleteSingleLocalization: function (id, callback) {
		$.ajax({
			url: rootUrl + id,
			type: 'DELETE',
			success: function() {
				if (typeof callback === "function") {
					callback();
				}
			}
		});
	},
	deleteCompleteLocalization: function (key, domain) {
		if (_Localization.localizationDetailKey.val() === key && (_Localization.localizationDetailDomain.val() === domain || (_Localization.localizationDetailDomain.val() === "" && domain === null))) {
			_Localization.clearLocalizationDetailsList();
			// _Localization.localizationDetails.hide();
		}

		$.ajax({
			url: rootUrl + 'Localizations/ui?' + (key ? 'name=' + key : '') + (domain ? '&domain=' + domain : '') + '&' + Structr.getRequestParameterName('sort') + '=locale',
			success: function(data) {
				var totalCounter = 0;
				var finishedCounter = 0;

				data.result.forEach(function (loc) {
					if (key === loc.name && domain === loc.domain) {
						totalCounter++;

						_Localization.deleteSingleLocalization(loc.id, function () {
							finishedCounter++;

							if (finishedCounter === totalCounter) {
								_Localization.keyAndDomainPager.refresh();
								_Localization.checkMainVisibility();
							}
						});
					}
				});
			}
		});
	}
};
