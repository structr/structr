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
	Structr.registerModule(_VirtualTypes);
});

let _VirtualTypes = {
	_moduleName: 'virtual-types',

	virtualTypesPager: undefined,
	virtualTypesList: undefined,
	virtualTypeDetail: undefined,
	virtualTypeDetailTableRow: undefined,
	virtualPropertiesTableBody: undefined,

	virtualTypesResizerLeftKey: 'structrVirtualTypesResizerLeftKey_' + location.port,
	virtualTypeSelectedElementKey: 'structrVirtualTypeSelectedElementKey_' + location.port,

	init: function() {},
	unload: function() {},
	onload: function() {

		Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('virtual-types'));

		Structr.fetchHtmlTemplate('virtual-types/main', {}, function (html) {

			main.append(html);

			Structr.fetchHtmlTemplate('virtual-types/functions', {}, function (html) {

				Structr.functionBar.innerHTML = html;

				UISettings.showSettingsForCurrentModule();

				document.querySelector('form#create-virtual-type-form').addEventListener('submit', async (e) => {

					e.preventDefault();

					let name       = document.getElementById('virtual-type-name-preselect');
					let sourceType = document.getElementById('virtual-type-source-type-preselect');

					let response = await fetch(rootUrl + 'VirtualType', {
						method: 'POST',
						body: JSON.stringify({
							name: name.value,
							sourceType: sourceType.value
						})
					});

					if (response.ok) {
						let data = await response.json();

						LSWrapper.setItem(_VirtualTypes.virtualTypeSelectedElementKey, data.result[0]);
						_VirtualTypes.virtualTypesPager.refresh();

					} else {
						blinkRed(Structr.functionBar);
					}
				});

				_VirtualTypes.virtualTypesList = $('#virtual-types-table tbody');
				_VirtualTypes.listVirtualTypes();

				_VirtualTypes.virtualTypeDetail          = document.getElementById('virtual-type-detail');
				_VirtualTypes.virtualTypeDetailTableRow  = $('#virtual-type-detail-table tbody tr');
				_VirtualTypes.virtualPropertiesTableBody = $('#virtual-properties-table tbody');

				_VirtualTypes.virtualTypeDetail.querySelector('button.create').addEventListener('click', () => { _VirtualTypes.appendVirtualProperty(); });

				_VirtualTypes.registerChangeListeners();

				Structr.appendInfoTextToElement({
					element: $('.resource-link'),
					text: "Preview the virtual type in a new window/tab.<br>The request parameter pageSize=1 is automatically appended to reduce the number of results in the preview.",
					css: { marginLeft: "5px" },
					offsetX: -300,
					offsetY: 10
				});

				_VirtualTypes.activateInfoTextsInColumnHeaders();

				Structr.unblockMenu(100);

				_VirtualTypes.moveResizer();
				Structr.initVerticalSlider($('.column-resizer', main), _VirtualTypes.virtualTypesResizerLeftKey, 300, _VirtualTypes.moveResizer);

				_VirtualTypes.resize();
			});
		});
	},
	showMain: () => {
		document.getElementById('virtual-types-main').style.display = 'flex';
		_VirtualTypes.moveResizer();
	},
	hideMain: () => {
		document.getElementById('virtual-types-main').style.display = 'none';
	},
	checkMainVisibility: () => {
		let rows = document.querySelectorAll('.virtual-type-row');
		let selectedRowExists = false;
		rows.forEach((row) => {
			selectedRowExists |= row.classList.contains('selected');
		});
		if (!rows || rows.length === 0) {
			_VirtualTypes.hideMain();
		} else if (!selectedRowExists) {
			_VirtualTypes.virtualTypeDetail.style.display = 'none';
		}
	},
	resize: function() {
		_VirtualTypes.moveResizer();
		Structr.resize();
	},
	moveResizer: function(left) {

		requestAnimationFrame(() => {

			left = left || LSWrapper.getItem(_VirtualTypes.virtualTypesResizerLeftKey) || 340;
			left = Math.max(340, Math.min(left, window.innerWidth - 340));

			document.querySelector('.column-resizer').style.left = left + 'px';

			let listContainer = document.getElementById('virtual-types-list-container');
			listContainer.style.width = 'calc(' + left + 'px - 1rem)';

			let detailContainer = document.getElementById('virtual-type-detail');
			detailContainer.style.width = 'calc(100% - ' + left + 'px - 3rem)';

			return true;
		});
	},
	activateInfoTextsInColumnHeaders: function() {
		$('th[data-info-text]').each(function(i, el) {
			Structr.appendInfoTextToElement({
				element: $(el),
				text: $(el).data('info-text'),
				css: { marginLeft: "5px" }
			});
		});
	},
	registerChangeListeners: function() {

		$(_VirtualTypes.virtualTypeDetailTableRow).on('change', '.property', function() {
			let el     = $(this);
			let row    = el.closest('tr');
			let typeId = row.data('virtual-type-id');

			if (typeId) {
				let data = _VirtualTypes.getVirtualObjectDataFromRow(row);

				_VirtualTypes.updateVirtualObject('VirtualType', typeId, data, el, el.closest('td'), function() {
					let rowInList = $('#virtual-type-' + typeId, _VirtualTypes.virtualTypesList);
					_VirtualTypes.updateResourceLink(data);
					_VirtualTypes.populateVirtualTypeRow(rowInList, data);
				});
			}
		});

		$(_VirtualTypes.virtualPropertiesTableBody).on('change', '.property', function() {
			let el         = $(this);
			let row        = el.closest('tr');
			let propertyId = row.data('virtual-property-id');

			if (propertyId) {
				let data = _VirtualTypes.getVirtualObjectDataFromRow(row);

				_VirtualTypes.updateVirtualObject('VirtualProperty', propertyId, data, el, el.closest('td'));
			}
		});
	},
	listVirtualTypes: function () {

		let pagerEl = $('#virtual-types-pager');

		_Pager.initPager('virtual-types', 'VirtualType', 1, 25, 'name', 'asc');

		_VirtualTypes.virtualTypesPager = _Pager.addPager('virtual-types', pagerEl, false, 'VirtualType', 'ui', _VirtualTypes.processPagerData);

		_VirtualTypes.virtualTypesPager.cleanupFunction = function () {
			fastRemoveAllChildren(_VirtualTypes.virtualTypesList[0]);
		};
		_VirtualTypes.virtualTypesPager.pager.append('Filters: <input type="text" class="filter w100 virtual-type-name" data-attribute="name" placeholder="Name" />');
		_VirtualTypes.virtualTypesPager.pager.append('<input type="text" class="filter w100 virtual-type-sourceType" data-attribute="sourceType" placeholder="Source Type" />');
		_VirtualTypes.virtualTypesPager.activateFilterElements();

		pagerEl.append('<div style="clear:both;"></div>');

		// $('#virtual-types-table .sort').on('click', function () {
		// 	_VirtualTypes.virtualTypesPager.setSortKey($(this).data('sort'));
		// });
	},
	processPagerData: function (pagerData) {
		if (pagerData && pagerData.length) {
			pagerData.forEach(_VirtualTypes.appendVirtualType);
		}
	},
	appendVirtualType: function (virtualType) {

		_VirtualTypes.showMain();

		Structr.fetchHtmlTemplate('virtual-types/row.type', {virtualType: virtualType}, function(html) {

			let row = $(html);
			_VirtualTypes.populateVirtualTypeRow(row, virtualType);
			_VirtualTypes.virtualTypesList.append(row);

			row[0].addEventListener('click', () => {
				_VirtualTypes.selectRow(row[0]);
				_VirtualTypes.showVirtualTypeDetails(virtualType.id);
			});

			_Elements.enableContextMenuOnElement(row, virtualType);
			_Entities.appendEditPropertiesIcon($('.actions', row), virtualType, true);

			let previouslySelectedElement = LSWrapper.getItem(_VirtualTypes.virtualTypeSelectedElementKey);
			if (previouslySelectedElement && previouslySelectedElement === virtualType.id) {
				row.click();
			}
		});
	},
	populateVirtualTypeRow: function(row, virtualTypeData) {
		$('.position', row).text(virtualTypeData.position !== null ? virtualTypeData.position : "");
		$('.name', row).text(virtualTypeData.name !== null ? virtualTypeData.name : "");
		$('.sourceType', row).text(virtualTypeData.sourceType !== null ? virtualTypeData.sourceType : "");
	},
	getContextMenuElements: function (div, virtualType) {

		let elements = [];

		elements.push({
			name: 'Edit',
			clickHandler: function() {
				_VirtualTypes.showVirtualTypeDetails(virtualType.id);
				return false;
			}
		});

		_Elements.appendContextMenuSeparator(elements);

		elements.push({
			icon: _Icons.getSvgIcon('trashcan'),
			classes: ['menu-bolder', 'danger'],
			name: 'Delete Virtual Type',
			clickHandler: () => {

				Structr.confirmation('<p>Do you really want to delete the virtual type "' + virtualType.name + '" with all its virtual properties?</p>', () => {

					Command.get(virtualType.id, 'id,properties', function(vt) {

						vt.properties.forEach(function(vp) {
							Command.deleteNode(vp.id);
						});

						Command.deleteNode(vt.id, null, () => {

							_VirtualTypes.virtualTypesPager.refresh();
							window.setTimeout(_VirtualTypes.checkMainVisibility, 200);

							div.remove();

							$.unblockUI({
								fadeOut: 25
							});
						});
					});
				});

				return false;
			}
		});

		_Elements.appendContextMenuSeparator(elements);

		return elements;
	},

	getVirtualObjectDataFromRow: function(row) {
		let data = {};

		$('.property', row).each(function(idx, el) {
			let $el = $(el);

			if ($el.attr('type') === 'checkbox') {
				data[$el.data('property')] = $el.prop('checked');
			} else {
				let val = $el.val();
				if (val === "") {
					val = null;
				}
				data[$el.data('property')] = val;
			}
		});

		return data;
	},
	showVirtualTypeDetails:function(virtualTypeId) {

		Command.get(virtualTypeId, '', function(vt) {

			LSWrapper.setItem(_VirtualTypes.virtualTypeSelectedElementKey, virtualTypeId);

			_VirtualTypes.virtualTypeDetailTableRow.data('virtual-type-id', virtualTypeId);

			$('.property', _VirtualTypes.virtualTypeDetailTableRow).each(function(idx, el) {
				let $el = $(el);
				let val = vt[$el.data('property')];

				if ($el.attr('type') === 'checkbox') {
					$el.prop('checked', (val === true));
				} else {
					$el.val(val);
				}
			});

			_VirtualTypes.updateResourceLink(vt);
			_VirtualTypes.listVirtualProperties(vt.properties);
			_VirtualTypes.virtualTypeDetail.style.display = null;
		});
	},
	updateResourceLink: function (virtualType) {
		let resourceLink = _VirtualTypes.virtualTypeDetail.querySelector('.resource-link a');
		resourceLink.setAttribute('href' , '/structr/rest/' + virtualType.name + '?' + Structr.getRequestParameterName('pageSize') + '=1');
		resourceLink.textContent = '/' + virtualType.name;
	},
	listVirtualProperties: (properties) => {

		properties.sort(function(a, b) {
			return (a.position === b.position) ? 0 : (a.position > b.position) ? 1 : -1;
		});

		fastRemoveAllChildren(_VirtualTypes.virtualPropertiesTableBody[0]);

		for (let p of properties) {
			_VirtualTypes.appendVirtualProperty(p);
		}
	},
	appendVirtualProperty: function(optionalProperty) {

		Structr.fetchHtmlTemplate('virtual-types/row.property', {}, function(html) {

			let row          = $(html);
			let removeButton = $('a.remove', row);
			let saveButton   = $('button.save', row);

			if (optionalProperty) {

				saveButton.remove();

				row.data('virtual-property-id', optionalProperty.id);

				removeButton.attr('title', 'Delete').on('click', function () {
					_VirtualTypes.deleteVirtualProperty(row.data('virtual-property-id'), row);
				});

				$('.property', row).each(function(idx, el) {
					let $el = $(el);
					let val = optionalProperty[$el.data('property')];

					if ($el.attr('type') === 'checkbox') {
						$el.prop('checked', (val === true));
					} else {
						$el.val(val);
					}
				});

			} else {

				saveButton.on('click', async () => {

					let data         = _VirtualTypes.getVirtualObjectDataFromRow(row);
					data.virtualType = _VirtualTypes.virtualTypeDetailTableRow.data('virtual-type-id');

					let response = await fetch(rootUrl + 'VirtualProperty', {
						method: 'POST',
						body: JSON.stringify(data),
					});

					if (response.ok) {

						blinkGreen($('td', row));

						let data = await response.json();

						row.data('virtual-property-id', data.result[0]);

						saveButton.remove();

						removeButton.attr('title', 'Delete').off('click').on('click', function () {
							_VirtualTypes.deleteVirtualProperty(row.data('virtual-property-id'), row);
						});

					} else {
						blinkRed($('td', row));
					}
				});

				removeButton.attr('title', 'Discard').on('click', function () {
					row.remove();
				});
			}

			_VirtualTypes.virtualPropertiesTableBody.append(row);
		});
	},
	updateVirtualObject: async (type, id, newData, $el, $blinkTarget, successCallback) => {

		let response = await fetch(rootUrl + type + '/' + id, {
			method: 'PUT',
			body: JSON.stringify(newData)
		});

		if (response.ok) {

			blinkGreen(($blinkTarget ? $blinkTarget : $el));

			if (typeof(successCallback) === "function") {
				successCallback();
			}

		} else {

			blinkRed(($blinkTarget ? $blinkTarget : $el));
		}
	},
	deleteVirtualProperty: (id, row) => {

		Structr.confirmation('<p>Do you really want to delete the virtual property?</p>', async () => {

			let response = await fetch(rootUrl + 'VirtualProperty/' + id, {
				method: 'DELETE'
			});

			if (response.ok) {
				row.remove();
			} else {
				blinkRed($('td', row));
			}

			$.unblockUI({
				fadeOut: 25
			});
		});
	},
	selectRow: (row) => {

		for (let row of document.querySelectorAll('.virtual-type-row')) {
			row.classList.remove('selected');
		}

		row.classList.add('selected');
	}
};