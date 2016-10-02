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
$(document).ready(function() {

	Structr.registerModule('localization', _Localization);
	_Localization.resize();

});

var _Localization = {

	keysAndDomainsList: undefined,
	keyAndDomainPager: undefined,
	localizationDetails: undefined,
	localizationDetailKey: undefined,
	localizationDetailDomain: undefined,
	localizationDetailEditButton: undefined,
	localizationDetailSaveButton: undefined,
	localizationDetailDiscardButton: undefined,
	localizationDetailList: undefined,

	init: function() {
		main = $('#main');
	},
	resize: function() {
		Structr.resize();
	},
	onload: function() {
		_Localization.init();

		$('#main-help a').attr('href', 'https://support.structr.com/article/135');

		main.append(
			'<div id="localization-main">' +
				'<div id="localizations-list" class="resourceBox">' +
					'<div id="localizations-pager"></div>' +
					'<p><button class="create"><img src="' + _Icons.add_icon + '"> Prepare new Localization</button></p>' +
					'<table id="localizations-table">' +
						'<thead><tr>' +
							'<th><a class="sortByKey">Key</a></th>' +
							'<th><a class="sortByDomain">Domain</a></th>' +
							'<th>Actions</th>' +
						'</tr></thead>' +
						'<tbody></tbody>' +
					'</table>' +
				'</div>' +
				'<div id="localization-detail" class="resourceBox">' +
					'<strong>Key:</strong> <input id="localization-key" class="disabled" disabled /> <strong>Domain:</strong> <input id="localization-domain" class="disabled" disabled />' +
					' <a title="Edit" class="edit" id="localization-fields-edit"><img alt="Edit Icon" src="' + _Icons.edit_icon + '"></a>' +
					' <a title="Save" class="save" id="localization-fields-save"><img alt="Save" src="' + _Icons.tick_icon + '"></a>' +
					' <a title="Discard" class="discard" id="localization-fields-discard"><img alt="Discard" src="' + _Icons.cross_icon + '"></a>' +
					'<p><button class="create"><img src="' + _Icons.add_icon + '"> New Localization</button></p>' +
					'<table id="localization-detail-table">' +
						'<thead><tr>' +
							'<th>ID</th>' +
							'<th>Locale</th>' +
							'<th>Localized Value</th>' +
							'<th>visibleToPublicUsers</th>' +
							'<th>visibleToAuthenticatedUsers</th>' +
							'<th>Imported</th>' +
							'<th>Actions</th>' +
						'</tr></thead>' +
						'<tbody></tbody>' +
					'</table>' +
				'</div>' +
			'</div>'
		);

		$('#localizations-list .create').on('click', function () {
			_Localization.showEmptyCreateLocalizationDialog();
		});

		$('#localization-detail .create').on('click', function (event) {
			event.preventDefault();
			_Localization.createNewLocalizationEntry();
		});

		_Localization.keysAndDomainsList = $('#localizations-table tbody');
		_Localization.listKeysAndDomains();

		_Localization.localizationDetails = $('#localization-detail');
		_Localization.localizationDetails.hide();
		_Localization.localizationDetailKey = $('#localization-key');
		_Localization.localizationDetailKey.on('keyup', _Localization.determineKeyFieldValidity);
		_Localization.localizationDetailDomain = $('#localization-domain');
		_Localization.localizationDetailEditButton = $('#localization-fields-edit').on('click', _Localization.editButtonAction);
		_Localization.localizationDetailSaveButton = $('#localization-fields-save').hide().on('click', _Localization.saveButtonAction);
		_Localization.localizationDetailDiscardButton = $('#localization-fields-discard').hide().on('click', _Localization.discardButtonAction);
		_Localization.localizationsDetailList = $('#localization-detail-table tbody');

		Structr.unblockMenu(100);

		_Localization.resize();
	},
	unload: function() {

	},
	listKeysAndDomains: function () {
		_Pager.initPager('localizations', 'Localization', 1, 25, 'name', 'asc');

		_Localization.keyAndDomainPager = _Pager.addPager('localizations', $('#localizations-pager'), false, 'Localization', 'ui', _Localization.processPagerData, _Localization.customPagerTransportFunction);

		_Localization.keyAndDomainPager.cleanupFunction = _Localization.clearLocalizationsList;
		_Localization.keyAndDomainPager.pager.append('Filters: <input type="text" class="filter localization-key" data-attribute="name" placeholder="Key" />');
		_Localization.keyAndDomainPager.pager.append('<input type="text" class="filter localization-domain" data-attribute="domain" placeholder="Domain" />');
		_Localization.keyAndDomainPager.activateFilterElements();

		$('#localizations-table .sortByKey').on('click', function () {
			_Localization.keyAndDomainPager.setSortKey('name');
		});

		$('#localizations-table .sortByDomain').on('click', function () {
			_Localization.keyAndDomainPager.setSortKey('domain');
		});
	},
	customPagerTransportFunction: function(type, pageSize, page, filterAttrs, callback) {
		var filterString = "";
		var presentFilters = Object.keys(filterAttrs);
		if (presentFilters.length > 0) {
			filterString = 'WHERE ' + presentFilters.map(function(key) { return 'n.' + key + ' =~ ".*' + filterAttrs[key] + '.*"'; }).join(' AND ');
		}
		Command.cypher('MATCH (n:Localization) ' + filterString + ' RETURN DISTINCT {name: n.name, domain: n.domain} as res ORDER BY res.' + sortKey[type] + ' ' + sortOrder[type], undefined, callback, pageSize, page);
	},
	processPagerData: function (pagerData) {
		if (pagerData && pagerData.length) {
			pagerData.forEach(_Localization.appendKeyAndDomainListRow);
		}
	},
	appendKeyAndDomainListRow: function (keyAndDomainObject) {
		_Localization.keysAndDomainsList.append(
			'<tr class="node key-domain-pair">' +
				'<td>' + keyAndDomainObject.name + '</td>' +
				'<td>' + (keyAndDomainObject.domain || '') + '</td>' +
				'<td class="actions">' +
					'<a title="Edit Properties" class="properties"><img alt="Edit" src="' + _Icons.edit_icon + '"></a>' +
					'<a title="Delete" class="delete"><img alt="Delete Icon" src="' + _Icons.delete_icon + '"></a>' +
				'</td>' +
			'</tr>'
		);

		var $el = $('tr.key-domain-pair:last');

		$('td.actions .properties', $el).on('click', function(event) {
			event.preventDefault();
			_Localization.showLocalizationsForKeyAndDomain((keyAndDomainObject.name ? keyAndDomainObject.name : null), (keyAndDomainObject.domain ? keyAndDomainObject.domain : null));
		});
		$('td.actions .delete', $el).on('click', function(event) {
			event.preventDefault();
			if (true === confirm('Do you really want to delete the complete localizations for "' + keyAndDomainObject.name + '"' + (keyAndDomainObject.domain ? ' in domain "' + keyAndDomainObject.domain + '"' : ' with empty domain') + ' ?')) {
				_Localization.deleteCompleteLocalization((keyAndDomainObject.name ? keyAndDomainObject.name : null), (keyAndDomainObject.domain ? keyAndDomainObject.domain : null), $el);
			}
		});
	},
	showLocalizationsForKeyAndDomain: function (key, domain) {
		_Localization.clearLocalizationDetailsList();

		_Localization.lockKeyAndDomain();
		_Localization.localizationDetailKey.val(key).removeData('oldValue').data('oldValue', key);
		_Localization.determineKeyFieldValidity();
		_Localization.localizationDetailDomain.val(domain).removeData('oldValue').data('oldValue', domain);

		_Localization.setLocalizationKeyAndDomainEditMode(false);

		_Localization.localizationDetails.show();

		$.ajax({
			url: rootUrl + 'Localizations/ui?' + (key ? 'name=' + key : '') + (domain ? '&domain=' + domain : '') + '&sort=locale',
			success: function(data) {
				data.result.forEach(function (loc) {
					if (key === loc.name && domain === loc.domain) {
						_Localization.appendLocalizationDetailListRow(loc);
					}
				});
			}
		});
	},
	clearLocalizationsList: function () {
		fastRemoveAllChildren(_Localization.keysAndDomainsList[0]);
	},
	appendLocalizationDetailListRow: function (loc) {
		var $tr = _Localization.appendEmptyLocalizationRow();
		_Localization.fillLocalizationRow($tr, loc);
	},
	clearLocalizationDetailsList: function () {
		_Localization.localizationDetailKey.val("");
		_Localization.localizationDetailDomain.val("");
		fastRemoveAllChildren(_Localization.localizationsDetailList[0]);
	},
	editButtonAction: function () {
		_Localization.unlockKeyAndDomain();
		_Localization.setLocalizationKeyAndDomainEditMode(true);
	},
	saveButtonAction: function () {
		var oldKey = _Localization.localizationDetailKey.data('oldValue');
		var curKey = _Localization.localizationDetailKey.val().trim();
		var oldDomain = _Localization.localizationDetailDomain.data('oldValue');
		var curDomain = _Localization.localizationDetailDomain.val().trim();
		if (curDomain === "") {
			curDomain = null;
		}
		if ((oldKey !== curKey) || (oldDomain !== curDomain)) {
			if (_Localization.isFieldNonEmpty(_Localization.localizationDetailKey)) {

				var newData = {
					name: curKey,
					domain: curDomain
				};

				$.ajax({
					url: rootUrl + 'Localizations/ui?' + (oldKey ? 'name=' + oldKey : '') + (oldDomain ? '&domain=' + oldDomain : '') + '&sort=locale',
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
											_Localization.solidifyKeyAndDomain(curKey, curDomain);
											_Localization.keyAndDomainPager.refresh();
										}
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
		} else {
			_Localization.lockKeyAndDomain();
			_Localization.setLocalizationKeyAndDomainEditMode(false);
		}
	},
	solidifyKeyAndDomain: function (key, domain) {
		_Localization.localizationDetailKey.val(key).data('oldValue', key);
		_Localization.localizationDetailDomain.val(domain).data('oldValue', domain);

		_Localization.lockKeyAndDomain();
		_Localization.setLocalizationKeyAndDomainEditMode(false);

	},
	discardButtonAction: function () {
		_Localization.lockKeyAndDomain();
		_Localization.setLocalizationKeyAndDomainEditMode(false);

		_Localization.localizationDetailKey.val(_Localization.localizationDetailKey.data('oldValue'));
		_Localization.determineKeyFieldValidity();
		_Localization.localizationDetailDomain.val(_Localization.localizationDetailDomain.data('oldValue'));
	},
	keyFieldErrorAction: function () {
		_Localization.localizationDetailKey.focus();
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
	lockKeyAndDomain: function () {
		_Localization.localizationDetailKey.addClass('disabled').attr('disabled', 'disabled');
		_Localization.localizationDetailDomain.addClass('disabled').attr('disabled', 'disabled');
	},
	unlockKeyAndDomain: function () {
		_Localization.localizationDetailKey.removeClass('disabled').attr('disabled', null);
		_Localization.localizationDetailDomain.removeClass('disabled').attr('disabled', null);
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
	appendEmptyLocalizationRow: function () {
		_Localization.localizationsDetailList.append(
			'<tr class="node localization">' +
				'<td><span class="placeholderText"> - not saved yet - </span></td>' +
				'<td><input class="___locale"></td>' +
				'<td><textarea class="___localizedName" cols="40"></textarea></td>' +
				'<td><input class="___visibleToPublicUsers" type="checkbox" checked></td>' +
				'<td><input class="___visibleToAuthenticatedUsers" type="checkbox" checked></td>' +
				'<td><input class="___imported" type="checkbox"></td>' +
				'<td class="actions"></td>' +
			'</tr>'
		);
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

		$('td:eq(0)', $row).text(localization.id);

		$localeField = $('.___locale', $row);
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

		$('.___imported', $row)
			.prop('checked', (localization.imported === true))
			.attr('disabled', null)
			.data('oldValue', localization.imported)
			.on('change', function (event) {
				_Localization.checkboxChangeAction($(event.target), localization.id, 'imported');
			});

		$('td.actions', $row).html('<a title="Delete" class="delete"><img alt="Delete Icon" src="' + _Icons.delete_icon + '"></a>');

		$('#loc_' + localization.id + ' td.actions .delete').on('click', function(event) {
			event.preventDefault();
			if (true === confirm('Really delete localizations "' + localization.localizedName + '" ?')) {
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
		var curValue = $el.val().trim();
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
	createNewLocalizationEntry: function () {
		var $tr = _Localization.appendEmptyLocalizationRow();

		$('input[type=checkbox]', $tr).attr('disabled', 'disabled');

		$('td.actions', $tr).html('<a title="Save" class="save"><img alt="Save" src="' + _Icons.tick_icon + '"></a><a title="Discard" class="discard"><img alt="Discard" src="' + _Icons.cross_icon + '"></a>');

		$('td.actions .discard', $tr).on('click', function(event) {
			event.preventDefault();
			$tr.remove();
		});
		$('td.actions .save', $tr).on('click', function(event) {
			event.preventDefault();

			if (_Localization.isFieldNonEmpty(_Localization.localizationDetailKey)) {

				var newData = {
					name: _Localization.localizationDetailKey.data('oldValue') || _Localization.localizationDetailKey.val().trim(),
					locale: $('.___locale', $tr).val().trim(),
					localizedName: $('.___localizedName', $tr).val(),
					visibleToPublicUsers: $('.___visibleToPublicUsers', $tr).prop('checked'),
					visibleToAuthenticatedUsers: $('.___visibleToAuthenticatedUsers', $tr).prop('checked'),
					imported: $('.___imported', $tr).prop('checked')
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
						_Localization.solidifyKeyAndDomain(newData.name, newData.domain);

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
			_Localization.localizationDetails.hide();
		}

		$.ajax({
			url: rootUrl + 'Localizations/ui?' + (key ? 'name=' + key : '') + (domain ? '&domain=' + domain : '') + '&sort=locale',
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
							}
						});
					}
				});
			}
		});
	}
};
