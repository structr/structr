/*
 * Copyright (C) 2010-2023 Structr GmbH
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
document.addEventListener("DOMContentLoaded", () => {
	Structr.registerModule(_Localization);
});

let _Localization = {
	_moduleName: 'localization',

	keysAndDomainsList: undefined,
	keyAndDomainPager: undefined,
	localizationDetailContainer: undefined,
	localizationDetailKey: undefined,
	localizationDetailDomain: undefined,
	localizationDetailSaveButton: undefined,
	localizationDetailList: undefined,

	localizationSelectedElementKey: 'structrLocalizationSelectedElementKey_' + location.port,
	localizationPreselectNameKey  : 'structrLocalizationPreselectNameKey_' + location.port,
	localizationPreselectDomainKey: 'structrLocalizationPreselectDomainKey_' + location.port,
	localizationPreselectLocaleKey: 'structrLocalizationPreselectLocaleKey_' + location.port,
	localizationResizerLeftKey    : 'structrLocalizationResizerLeftKey_' + location.port,

	init: () => {},
	resize: () => {
		_Localization.moveResizer();
	},
	onload: () => {

		_Localization.init();

		Structr.updateMainHelpLink(_Helpers.getDocumentationURLForTopic('localization'));

		Structr.setMainContainerHTML(_Localization.templates.main());
		Structr.setFunctionBarHTML(_Localization.templates.functions());

		document.getElementById('add-new-translation').addEventListener('click', (event) => {
			event.preventDefault();
			_Localization.createNewLocalizationEntry();
		});

		// UISettings.showSettingsForCurrentModule();

		let keyPreselect    = document.getElementById('localization-key-preselect');
		let domainPreselect = document.getElementById('localization-domain-preselect');
		let localePreselect = document.getElementById('localization-locale-preselect');

		keyPreselect.addEventListener('keyup', (e) => {

			if (e.code === 'Escape' || e.keyCode === 27) {
				keyPreselect.value = '';
				LSWrapper.setItem(_Localization.localizationPreselectNameKey, '');
			}
			return false;
		});

		domainPreselect.addEventListener('keyup', (e) => {

			if (e.code === 'Escape' || e.keyCode === 27) {
				domainPreselect.value = '';
				LSWrapper.setItem(_Localization.localizationPreselectDomainKey, '');
			}
			return false;
		});

		//keyPreselect.value    = LSWrapper.getItem(_Localization.localizationPreselectNameKey) || '';
		domainPreselect.value = LSWrapper.getItem(_Localization.localizationPreselectDomainKey) || '';
		localePreselect.value = LSWrapper.getItem(_Localization.localizationPreselectLocaleKey) || 'en';

		document.getElementById('create-localization-form').addEventListener('submit', (e) => {
			e.preventDefault();

			_Localization.showMain();

			let preselectData = {
				name: keyPreselect.value,
			};

			if (domainPreselect.value.length > 0) {
				preselectData.domain = domainPreselect.value;
			}

			let preselectLocalesString = localePreselect.value.trim();

			LSWrapper.setItem(_Localization.localizationPreselectNameKey,   preselectData.name);
			LSWrapper.setItem(_Localization.localizationPreselectDomainKey, preselectData.domain);
			LSWrapper.setItem(_Localization.localizationPreselectLocaleKey, preselectLocalesString);

			let preselectLocales = preselectLocalesString.split(',').map((l) => l.trim());

			_Localization.createNewLocalizationKey(preselectData, preselectLocales);
		});

		_Localization.keysAndDomainsList = $('#localization-table tbody');
		_Localization.listKeysAndDomains();

		_Localization.localizationDetailContainer  = document.getElementById('localization-detail-container');
		_Localization.localizationDetailKey        = $('#localization-key');
		_Localization.localizationDetailDomain     = $('#localization-domain');
		_Localization.localizationDetailSaveButton = $('#localization-fields-save').on('click', _Localization.saveButtonAction);
		_Localization.localizationsDetailList      = $('#localization-detail-table tbody');

		Structr.mainMenu.unblock(100);

		_Localization.moveResizer();
		Structr.initVerticalSlider(Structr.mainContainer.querySelector('.column-resizer'), _Localization.localizationResizerLeftKey, 340, _Localization.moveResizer);

		Structr.resize();
	},
	unload: () => { },
	getContextMenuElements: (div, keyAndDomainObject) => {

		let elements = [];

		elements.push({
			name: 'Edit',
			clickHandler: () => {
				_Localization.showLocalizationsForKeyAndDomainObject(keyAndDomainObject);
			}
		});

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		elements.push({
			icon: _Icons.getMenuSvgIcon(_Icons.iconTrashcan),
			classes: ['menu-bolder', 'danger'],
			name: 'Delete Localization',
			clickHandler: async () => {

				let confirm = await _Dialogs.confirmation.showPromise(`<p>Do you really want to delete the complete localizations for "<b>${keyAndDomainObject.name}</b>" ${(keyAndDomainObject.domain ? ` in domain "<b>${keyAndDomainObject.domain}</b>"` : ' with empty domain')} ?</p>`);
				if (confirm === true) {
					await _Localization.deleteCompleteLocalization((keyAndDomainObject.name ? keyAndDomainObject.name : null), (keyAndDomainObject.domain ? keyAndDomainObject.domain : null));

					_Localization.localizationDetailContainer.style.display = 'none';
				}
			}
		});

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		return elements;
	},
	prevAnimFrameReqId_moveResizer: undefined,
	moveResizer: (left) => {

		_Helpers.requestAnimationFrameWrapper(_Localization.prevAnimFrameReqId_moveResizer, () => {

			left = left || LSWrapper.getItem(_Localization.localizationResizerLeftKey) || 340;
			left = Math.max(300, Math.min(left, window.innerWidth - 300));

			document.getElementById('localization-list-container').style.width = 'calc(' + left + 'px - 1rem)';
			document.querySelector('.column-resizer').style.left                = left + 'px';
			_Localization.localizationDetailContainer.style.width                       = 'calc(100% - ' + left + 'px - 3rem)';

			return true;
		});
	},
	listKeysAndDomains: () => {

		let pagerEl = document.querySelector('#localization-pager');

		_Pager.initPager('localizations', 'Localization', 1, 25, 'name', 'asc');

		_Localization.keyAndDomainPager = _Pager.addPager('localizations', pagerEl, false, 'Localization', 'ui', _Localization.processPagerData, _Localization.customPagerTransportFunction, undefined, true);

		_Localization.keyAndDomainPager.cleanupFunction = _Localization.clearLocalizationsList;
		_Localization.keyAndDomainPager.appendFilterElements(`
			<span class="mr-1">Filters:</span>
			<input type="text" class="filter w75 localization-key" data-attribute="name" placeholder="Key">
			<input type="text" class="filter w75 localization-domain" data-attribute="domain" placeholder="Domain">
			<input type="text" class="filter w75 localization-text" data-attribute="localizedName" placeholder="Content">
		`);
		_Localization.keyAndDomainPager.activateFilterElements();
		_Localization.keyAndDomainPager.setIsPaused(false);
		_Localization.keyAndDomainPager.refresh();

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
	customPagerTransportFunction: (type, pageSize, page, filterAttrs, callback) => {

		let filterString = "";
		let presentFilters = Object.keys(filterAttrs);
		if (presentFilters.length > 0) {
			filterString = 'WHERE ' + presentFilters.map((key) => { return `n.${key} =~ "(?i).*${filterAttrs[key]}.*"`; }).join(' AND ');
		}

		Command.cypher(`MATCH (n:Localization) ${filterString} RETURN DISTINCT { name: n.name, domain: n.domain } as res ORDER BY res.${_Pager.sortKey[type]} ${_Pager.sortOrder[type]}`, undefined, callback, pageSize, page);
	},
	processPagerData: (pagerData) => {
		if (pagerData && pagerData.length) {
			for (let entry of pagerData) {
				_Localization.appendKeyAndDomainListRow(entry);
			}
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
		for (let row of rows) {
			selectedRowExists |= row.classList.contains('selected');
		}
		if (!rows || rows.length === 0) {
			_Localization.hideMain();
		} else if (!selectedRowExists) {
			_Localization.localizationDetailContainer.style.display = 'none';
		}
	},
	selectRow: (row) => {

		for (let row of document.querySelectorAll('.localization-row.selected')) {
			row.classList.remove('selected');
		}

		row.classList.add('selected');
	},
	appendKeyAndDomainListRow: (keyAndDomainObject) => {

		_Localization.showMain();

		let combinedTypeForId     = _Localization.getCombinedTypeForId(keyAndDomainObject);
		keyAndDomainObject.htmlId = combinedTypeForId;

		let html = _Localization.templates.typeRow({ localization: keyAndDomainObject });
		let row  = $(html);
		_Localization.keysAndDomainsList.append(row);

		row[0].addEventListener('click', () => {
			_Localization.selectRow(row[0]);
			_Localization.showLocalizationsForKeyAndDomainObject(keyAndDomainObject);
		});

		_Elements.contextMenu.enableContextMenuOnElement(row[0], keyAndDomainObject);
		_Entities.appendContextMenuIcon($('.icons-container', row), keyAndDomainObject, true);

		let previouslySelectedElement = LSWrapper.getItem(_Localization.localizationSelectedElementKey);
		if (previouslySelectedElement && previouslySelectedElement.htmlId === row[0].id) {
			row.click();
		}
	},
	getCombinedTypeForId: (keyAndDomainObject) => {
		let key    = (keyAndDomainObject.name ? keyAndDomainObject.name : null);
		let domain = (keyAndDomainObject.domain ? keyAndDomainObject.domain : null);

		let combinedTypeForId = 'localization-' + (key ? key : 'nullKey') + '___' + (domain ? domain : 'nullDomain');
		return combinedTypeForId;
	},
	getLocalizationsForNameAndDomain: async (name, domain) => {

		let response = await fetch(Structr.graphQLRootUrl + '?query=' + `{ Localization(_sort: "locale") { id, locale, name${name ? `(_equals: "${name}")` : ''}, domain${domain ? `(_equals: "${domain}")` : ''}, localizedName, visibleToPublicUsers, visibleToAuthenticatedUsers }}`);

		if (response.ok) {

			let data = await response.json();

			return { responseOk: true, result: data['Localization'] };
		}

		return { responseOk: false, result: [] };
	},
	showLocalizationsForKeyAndDomainObject: async (keyAndDomainObject, isCreate) => {

		let key    = (keyAndDomainObject.name === undefined ? null : keyAndDomainObject.name);
		let domain = (keyAndDomainObject.domain === undefined ? null: keyAndDomainObject.domain);

		let combinedTypeForId     = _Localization.getCombinedTypeForId(keyAndDomainObject);
		keyAndDomainObject.htmlId = combinedTypeForId;

		LSWrapper.setItem(_Localization.localizationSelectedElementKey, keyAndDomainObject);

		let { responseOk, result } = await _Localization.getLocalizationsForNameAndDomain(key, domain);

		if (responseOk && result.length > 0) {

			_Localization.localizationDetailContainer.style.display = null;

			_Localization.localizationDetailKey.val(key).removeData('oldValue').data('oldValue', key);
			_Localization.localizationDetailDomain.val(domain).removeData('oldValue').data('oldValue', domain);

			_Localization.clearLocalizationDetailsList();

			for (let loc of result) {

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

		} else {
			LSWrapper.removeItem(_Localization.localizationSelectedElementKey);
		}
	},
	clearLocalizationsList: () => {
		_Helpers.fastRemoveAllChildren(_Localization.keysAndDomainsList[0]);
	},
	appendLocalizationDetailListRow: (localization) => {

		let html = _Localization.templates.emptyRow();
		_Localization.localizationsDetailList.append(html);

		let $tr = $('tr:last', _Localization.localizationsDetailList);

		_Localization.fillLocalizationRow($tr, localization);
	},
	clearLocalizationDetailsList: () => {
		_Helpers.fastRemoveAllChildren(_Localization.localizationsDetailList[0]);
	},
	saveButtonAction: async (e) => {

		let oldKey    = _Localization.localizationDetailKey.data('oldValue');
		let curKey    = _Localization.localizationDetailKey.val().trim();
		let oldDomain = _Localization.localizationDetailDomain.data('oldValue');
		let curDomain = _Localization.localizationDetailDomain.val().trim();
		if (curDomain === '') {
			curDomain = null;
		}

		if ((oldKey !== curKey) || (oldDomain !== curDomain)) {

			if (_Localization.isFieldNonEmpty(_Localization.localizationDetailKey)) {

				let newData = {
					name: curKey,
					domain: curDomain
				};

				let { responseOk, result } = await _Localization.getLocalizationsForNameAndDomain(oldKey, oldDomain);

				if (responseOk) {

					LSWrapper.setItem(_Localization.localizationSelectedElementKey, newData);

					let patchData = [];

					for (let loc of result) {

						if (oldKey === loc.name && oldDomain === loc.domain) {
							patchData.push(Object.assign({ id: loc.id }, newData));
						}
					}

					let putResponse = await fetch(Structr.rootUrl + 'Localization/', {
						method: 'PATCH',
						body: JSON.stringify(patchData)
					});

					if (putResponse.ok) {

						_Helpers.blinkGreen($(e.target));

						_Localization.keyAndDomainPager.refresh();
						await _Localization.showLocalizationsForKeyAndDomainObject(newData);

					} else {

						_Helpers.blinkRed($(e.target));
					}
				}

			} else {
				_Localization.keyFieldErrorAction();
			}
		}
	},
	keyFieldErrorAction: () => {
		_Localization.localizationDetailKey.focus();
		_Helpers.blinkRed(_Localization.localizationDetailKey);
	},
	isFieldNonEmpty: ($field) => {
		return ($field.val().trim() !== '');
	},
	fillLocalizationRow: ($row, localization) => {

		$row.attr('id', 'loc_' + localization.id);

		let $localeField = $('.___locale', $row);
		$localeField.val(localization.locale)
			.data('oldValue', localization.locale)
			.on('blur', (event) => {
				_Localization.textfieldChangeAction($(event.target), localization, 'locale');
			});

		$('.___localizedName', $row)
			.val(localization.localizedName)
			.data('oldValue', localization.localizedName)
			.on('blur', (event) => {
				_Localization.textfieldChangeAction($(event.target), localization, 'localizedName');
			});

		$('.___visibleToPublicUsers', $row)
			.prop('checked', (localization.visibleToPublicUsers === true))
			.attr('disabled', null)
			.data('oldValue', localization.visibleToPublicUsers)
			.on('change', (event) => {
				_Localization.checkboxChangeAction($(event.target), localization, 'visibleToPublicUsers');
			});

		$('.___visibleToAuthenticatedUsers', $row)
			.prop('checked', (localization.visibleToAuthenticatedUsers === true))
			.attr('disabled', null)
			.data('oldValue', localization.visibleToAuthenticatedUsers)
			.on('change', (event) => {
				_Localization.checkboxChangeAction($(event.target), localization, 'visibleToAuthenticatedUsers');
			});

		$('.id', $row).text(localization.id);

		$('td.actions .save-localization', $row).remove();

		$row[0].querySelector('.remove-localization').addEventListener('click', async (e) => {
			e.preventDefault();

			let key    = _Localization.localizationDetailKey.val();
			let domain = _Localization.localizationDetailDomain.val();
			let confirm = await _Dialogs.confirmation.showPromise(`<p>Really delete localization "${(localization.localizedName || '')}" for key "${key}"${(domain ? ` in domain "${domain}"` : ' with empty domain')}?</p>`);

			if (confirm === true) {

				let success = await _Localization.deleteSingleLocalization(localization.id);
				if (success === true) {

					$row.remove();

					if ($('tr', _Localization.localizationsDetailList).length === 0) {
						_Localization.keyAndDomainPager.refresh();
						_Localization.localizationDetailContainer.style.display = 'none';
					}

				} else {
					_Helpers.blinkRed($row);
				}
			}
		});
	},
	textfieldChangeAction: ($el, localization, attr) => {

		let oldValue = $el.data('oldValue');
		let curValue = $el.val();

		if (oldValue !== curValue) {
			_Localization.updateLocalization(localization, attr, curValue, oldValue, $el);
		}
	},
	checkboxChangeAction: ($el, localization, attr) => {

		let oldValue = $el.data('oldValue');
		let curValue = $el.prop('checked');

		if (oldValue !== curValue) {
			_Localization.updateLocalization(localization, attr, curValue, oldValue, $el, $el.parent());
		}
	},
	updateLocalization: async (localization, attr, curValue, oldValue, $el, $blinkTarget) => {

		let newData = {};
		newData[attr] = curValue;

		let response = await fetch(Structr.rootUrl + 'Localization/' + localization.id, {
			method: 'PUT',
			body: JSON.stringify(newData),
		});

		if (response.ok) {

			$el.data('oldValue', curValue);
			localization[attr] = curValue;
			_Helpers.blinkGreen(($blinkTarget ? $blinkTarget : $el));

		} else {

			if ($el.attr('type') === 'checkbox') {
				$el.prop('checked', oldValue);
			} else {
				$el.val(oldValue);
			}
			_Helpers.blinkRed(($blinkTarget ? $blinkTarget : $el));
		}
	},
	createNewLocalizationKey: (newData, preselectLocales) => {

		Promise.all(
			preselectLocales.map((locale) => {
				newData.locale = locale;

				return fetch(Structr.rootUrl + 'Localization', {
					method: 'POST',
					body: JSON.stringify(newData)
				});
			})
		).then((done) => {
			_Localization.showLocalizationsForKeyAndDomainObject(newData, true);
		});
	},
	createNewLocalizationEntry: () => {

		let html = _Localization.templates.emptyRow();
		_Localization.localizationsDetailList.append(html);
		let $tr       = $('tr:last', _Localization.localizationsDetailList);
		let trElement = $tr[0];

		let removeRow = (event) => {
			event.preventDefault();
			trElement.remove();
		};

		let removeButton = trElement.querySelector('td.actions .remove-localization');
		removeButton.addEventListener('click', removeRow);

		trElement.querySelector('td.actions .save-localization').addEventListener('click', async (event) => {

			event.preventDefault();

			if (_Localization.isFieldNonEmpty(_Localization.localizationDetailKey)) {

				let newData = {
					name:                        _Localization.localizationDetailKey.data('oldValue') || _Localization.localizationDetailKey.val().trim(),
					locale:                      $('.___locale', $tr).val().trim(),
					localizedName:               $('.___localizedName', $tr).val(),
					visibleToPublicUsers:        $('.___visibleToPublicUsers', $tr).prop('checked'),
					visibleToAuthenticatedUsers: $('.___visibleToAuthenticatedUsers', $tr).prop('checked')
				};

				newData.domain = _Localization.localizationDetailDomain.data('oldValue') || _Localization.localizationDetailDomain.val().trim();
				if (newData.domain.trim() === "") {
					newData.domain = null;
				}

				let response = await fetch(Structr.rootUrl + 'Localization', {
					method: 'POST',
					body: JSON.stringify(newData),
				});

				if (response.ok) {

					let data = await response.json();

					newData.id = data.result[0];

					removeButton.removeEventListener('click', removeRow);

					_Localization.fillLocalizationRow($tr, newData);

				} else {
					_Helpers.blinkRed($('td', $tr));
				}

			} else {
				_Localization.keyFieldErrorAction();
			}
		});
	},
	deleteSingleLocalization: async (id) => {

		let response = await fetch(Structr.rootUrl + id, {
			method: 'DELETE',
		});

		return response.ok;
	},
	deleteCompleteLocalization: async (key, domain) => {

		let { responseOk, result } = await _Localization.getLocalizationsForNameAndDomain(key, domain);

		if (responseOk) {

			let promises = result.filter((loc) => {

				return (key === loc.name && domain === loc.domain);

			}).map((loc) => {

				return _Localization.deleteSingleLocalization(loc.id);
			});

			Promise.all(promises).then(() => {

				_Localization.keyAndDomainPager.refresh();
				_Localization.checkMainVisibility();
			});
		}
	},

	templates: {
		main: config => `
			<div id="localization-main">

				<div class="column-resizer column-resizer-left"></div>

				<div id="localization-list-container">
					<div id="localization-list" class="resourceBox">
						<table id="localization-table">
			<!--				<thead><tr>-->
			<!--					<th><a class="sort" data-sort="key">Key</a></th>-->
			<!--					<th><a class="sort" data-sort="domain">Domain</a></th>-->
			<!--					<th id="localization-list-th-actions" class="narrow">Actions</th>-->
			<!--				</tr></thead>-->
							<tbody></tbody>
						</table>
					</div>
				</div>

				<div id="localization-detail-container" style="display: none;">

					<form id="localization-detail-form">

						<div class="form-row">
							<div class="form-col">
								<label for="localization-key">Key</label>
								<input id="localization-key" type="text">
							</div>
							<div class="form-col">
								<label for="localization-domain">Domain</label>
								<input id="localization-domain" type="text">
							</div>
						</div>
					</form>
					<div>
						<button id="localization-fields-save" title="Save" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
							${_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 14, 14, 'icon-green mr-2')} Save
						</button>
					</div>

					<div class="mt-8">
						<button id="add-new-translation" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'icon-green mr-2')} Add translation
						</button>
					</div>

					<table id="localization-detail-table" class="props">
						<thead>
							<tr>
								<th class="w-24"></th>
								<th class="w-40">Locale</th>
								<th>Translation</th>
								<th class="w-16">ID</th>
								<th class="w-24">${Structr.abbreviations['visibleToPublicUsers']}</th>
								<th class="w-24">${Structr.abbreviations['visibleToAuthenticatedUsers']}</th>
							</tr>
						</thead>
						<tbody></tbody>
					</table>
				</div>
			</div>
		`,
		functions: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/crud.css">
			<link rel="stylesheet" type="text/css" media="screen" href="css/localization.css">

			<form id="create-localization-form" autocomplete="off">
				<div class="inline-flex gap-x-2">
					<input title="Enter a key to create translations for" id="localization-key-preselect" type="text" size="30" placeholder="Key" name="key" required data-lpignore="true">
					<input title="Enter a domain to create translations for" id="localization-domain-preselect" type="text" size="10" placeholder="Domain" name="domain">
					<input title="Enter a comma-separated list of locales (f.e. en,de,fr) to create translations for" id="localization-locale-preselect" type="text" size="10" placeholder="Locale(s)" name="locale">

					<button type="submit" form="create-localization-form" class="action inline-flex items-center">
						${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'icon-white mr-2')} Create new localization key
					</button>
				</div>
			</form>

				<div id="localization-pager"></div>
		`,
		typeRow: config => `
			<tr class="localization-row key-domain-pair" id="${config.localization.htmlId}">
				<td class="property allow-break" data-property="key">${config.localization.name}</td>
				<td class="property" data-property="domain">${config.localization.domain || ''}</td>
				<td class="actions">
					<div class="icons-container flex items-center justify-end"></div>
				</td>
			</tr>
		`,
		emptyRow: config => `
			<tr class="localization">
				<td class="actions text-center">
					<div class="flex items-center justify-center">
						${_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-green', 'mr-2', 'save-localization']))}
						${_Icons.getSvgIcon(_Icons.iconTrashcan,      20, 20, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-localization']), 'Remove')}
					</div>
				</td>
				<td>
					<input class="___locale">
				</td>
				<td>
					<textarea class="___localizedName" cols="40"></textarea>
				</td>
				<td>
					<span class="abbr-ellipsis abbr-80 id"></span>
				</td>
				<td>
					<input class="___visibleToPublicUsers" type="checkbox" checked>
				</td>
				<td>
					<input class="___visibleToAuthenticatedUsers" type="checkbox" checked>
				</td>
			</tr>
		`,
	}
};
