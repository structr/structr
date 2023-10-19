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

	init: () => {},
	unload: () => {},
	onload: () => {

		Structr.updateMainHelpLink(_Helpers.getDocumentationURLForTopic('virtual-types'));

		Structr.setMainContainerHTML(_VirtualTypes.templates.main());
		Structr.setFunctionBarHTML(_VirtualTypes.templates.functions());

		// UISettings.showSettingsForCurrentModule();

		document.querySelector('form#create-virtual-type-form').addEventListener('submit', async (e) => {

			e.preventDefault();

			let name       = document.getElementById('virtual-type-name-preselect');
			let sourceType = document.getElementById('virtual-type-source-type-preselect');

			let response = await fetch(Structr.rootUrl + 'VirtualType', {
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
				_Helpers.blinkRed(Structr.functionBar);
			}
		});

		_VirtualTypes.virtualTypesList = $('#virtual-types-table tbody');
		_VirtualTypes.listVirtualTypes();

		_VirtualTypes.virtualTypeDetail          = document.getElementById('virtual-type-detail');
		_VirtualTypes.virtualTypeDetailTableRow  = $('#virtual-type-detail-table tbody tr');
		_VirtualTypes.virtualPropertiesTableBody = $('#virtual-properties-table tbody');

		_VirtualTypes.virtualTypeDetail.querySelector('button.create').addEventListener('click', () => { _VirtualTypes.appendVirtualProperty(); });

		_VirtualTypes.registerChangeListeners();

		_Helpers.appendInfoTextToElement({
			element: $('.resource-link'),
			text: "Preview the virtual type in a new window/tab.<br>The request parameter pageSize=1 is automatically appended to reduce the number of results in the preview.",
			css: { marginLeft: "5px" },
			offsetX: -300,
			offsetY: 10
		});

		_Helpers.activateCommentsInElement(_VirtualTypes.virtualTypeDetail);

		Structr.mainMenu.unblock(100);

		_VirtualTypes.moveResizer();
		Structr.initVerticalSlider(Structr.mainContainer.querySelector('.column-resizer'), _VirtualTypes.virtualTypesResizerLeftKey, 300, _VirtualTypes.moveResizer);

		Structr.resize();
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
	resize: () => {
		_VirtualTypes.moveResizer();
	},
	prevAnimFrameReqId_moveResizer: undefined,
	moveResizer: (left) => {

		_Helpers.requestAnimationFrameWrapper(_VirtualTypes.prevAnimFrameReqId_moveResizer, () => {

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
	registerChangeListeners: () => {

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
	listVirtualTypes: () => {

		_Pager.initPager('virtual-types', 'VirtualType', 1, 25, 'name', 'asc');

		_VirtualTypes.virtualTypesPager = _Pager.addPager('virtual-types', document.querySelector('#virtual-types-pager'), false, 'VirtualType', 'ui', _VirtualTypes.processPagerData, undefined, undefined, true);

		_VirtualTypes.virtualTypesPager.cleanupFunction = () => {
			_Helpers.fastRemoveAllChildren(_VirtualTypes.virtualTypesList[0]);
		};
		_VirtualTypes.virtualTypesPager.appendFilterElements(`
			<span class="mr-1">Filters:</span>
			<input type="text" class="filter w100 virtual-type-name" data-attribute="name" placeholder="Name">
			<input type="text" class="filter w100 virtual-type-sourceType" data-attribute="sourceType" placeholder="Source Type">
		`);
		_VirtualTypes.virtualTypesPager.activateFilterElements();
		_VirtualTypes.virtualTypesPager.setIsPaused(false);
		_VirtualTypes.virtualTypesPager.refresh();

	},
	processPagerData: (pagerData) => {
		if (pagerData && pagerData.length) {
			pagerData.forEach(_VirtualTypes.appendVirtualType);
		}
	},
	appendVirtualType: (virtualType) => {

		_VirtualTypes.showMain();

		let mainHtml = _VirtualTypes.templates.typeRow({ virtualType: virtualType });
		let row      = $(mainHtml);
		_VirtualTypes.populateVirtualTypeRow(row, virtualType);
		_VirtualTypes.virtualTypesList.append(row);

		row[0].addEventListener('click', () => {
			_VirtualTypes.selectRow(row[0]);
			_VirtualTypes.showVirtualTypeDetails(virtualType.id);
		});

		_Elements.contextMenu.enableContextMenuOnElement(row[0], virtualType);
		_Entities.appendContextMenuIcon(row[0].querySelector('.icons-container'), virtualType, true);

		let previouslySelectedElement = LSWrapper.getItem(_VirtualTypes.virtualTypeSelectedElementKey);
		if (previouslySelectedElement && previouslySelectedElement === virtualType.id) {
			row.click();
		}
	},
	populateVirtualTypeRow: (row, virtualTypeData) => {
		$('.position', row).text(virtualTypeData.position !== null ? virtualTypeData.position : "");
		$('.name', row).text(virtualTypeData.name !== null ? virtualTypeData.name : "");
		$('.sourceType', row).text(virtualTypeData.sourceType !== null ? virtualTypeData.sourceType : "");
	},
	getContextMenuElements: (div, virtualType) => {

		let elements = [];

		elements.push({
			name: 'Edit',
			clickHandler: () => {
				_VirtualTypes.showVirtualTypeDetails(virtualType.id);
			}
		});

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		elements.push({
			icon: _Icons.getMenuSvgIcon(_Icons.iconTrashcan),
			classes: ['menu-bolder', 'danger'],
			name: 'Delete Virtual Type',
			clickHandler: async () => {

				let confirm = await _Dialogs.confirmation.showPromise(`<p>Do you really want to delete the virtual type "<b>${virtualType.name}</b>" with all its virtual properties?</p>`);

				if (confirm === true) {

					Command.get(virtualType.id, 'id,properties', (vt) => {

						let promises = vt.properties.map((vp) => {
							return fetch(Structr.rootUrl + vp.id, { method: 'DELETE' });
						});

						promises.push(fetch(Structr.rootUrl + vt.id, { method: 'DELETE' }))

						Promise.all(promises).then(() => {

							_VirtualTypes.virtualTypesPager.refresh();
							window.setTimeout(_VirtualTypes.checkMainVisibility, 200);

							_Helpers.fastRemoveElement(div);
						});
					});
				}
			}
		});

		_Elements.contextMenu.appendContextMenuSeparator(elements);

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
	updateResourceLink: (virtualType) => {

		let resourceLink = _VirtualTypes.virtualTypeDetail.querySelector('.resource-link a');

		let endpointURL  = `${Structr.rootUrl + virtualType.name}?${Structr.getRequestParameterName('pageSize')}=1`;

		resourceLink.setAttribute('href', endpointURL);
		resourceLink.textContent = endpointURL;
	},
	listVirtualProperties: (properties) => {

		_Helpers.sort(properties, 'position');

		_Helpers.fastRemoveAllChildren(_VirtualTypes.virtualPropertiesTableBody[0]);

		for (let p of properties) {
			_VirtualTypes.appendVirtualProperty(p);
		}
	},
	appendVirtualProperty: (optionalProperty) => {

		let propertyRowHtml = _VirtualTypes.templates.propertyRow();
		let row             = $(propertyRowHtml);
		let removeButton    = $('.remove-virtual-property', row);
		let saveButton      = $('.save-virtual-property', row);

		if (optionalProperty) {

			saveButton.remove();

			row.data('virtual-property-id', optionalProperty.id);

			removeButton.on('click', () => {
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

				let response = await fetch(Structr.rootUrl + 'VirtualProperty', {
					method: 'POST',
					body: JSON.stringify(data),
				});

				if (response.ok) {

					_Helpers.blinkGreen($('td', row));

					let data = await response.json();

					row.data('virtual-property-id', data.result[0]);

					saveButton.remove();

					removeButton.off('click').on('click', () => {
						_VirtualTypes.deleteVirtualProperty(row.data('virtual-property-id'), row);
					});

				} else {
					_Helpers.blinkRed($('td', row));
				}
			});

			removeButton.on('click', () => {
				row.remove();
			});
		}

		_VirtualTypes.virtualPropertiesTableBody.append(row);
	},
	updateVirtualObject: async (type, id, newData, $el, $blinkTarget, successCallback) => {

		let response = await fetch(`${Structr.rootUrl}${type}/${id}`, {
			method: 'PUT',
			body: JSON.stringify(newData)
		});

		if (response.ok) {

			_Helpers.blinkGreen(($blinkTarget ? $blinkTarget : $el));

			successCallback?.();

		} else {

			_Helpers.blinkRed(($blinkTarget ? $blinkTarget : $el));
		}
	},
	deleteVirtualProperty: (id, row) => {

		_Dialogs.confirmation.showPromise('<p>Do you really want to delete the virtual property?</p>').then(async confirm => {

			if (confirm === true) {

				let response = await fetch(`${Structr.rootUrl}VirtualProperty/${id}`, {
					method: 'DELETE'
				});

				if (response.ok) {
					row.remove();
				} else {
					_Helpers.blinkRed($('td', row));
				}
			}
		});
	},
	selectRow: (row) => {

		for (let row of document.querySelectorAll('.virtual-type-row.selected')) {
			row.classList.remove('selected');
		}

		row.classList.add('selected');
	},

	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/crud.css">
			<link rel="stylesheet" type="text/css" media="screen" href="css/virtual-types.css">

			<div id="virtual-types-main">

				<div class="column-resizer column-resizer-left"></div>

				<div id="virtual-types-list-container">
					<div id="virtual-types-list" class="resourceBox">
						<table id="virtual-types-table">
							<thead>
								<tr>
									<th class="narrow"><!--a class="sort" data-sort="position"-->Pos.<!--/a--></th>
									<th><!--a class="sort" data-sort="name"-->Name<!--/a--></th>
									<th><!--a class="sort" data-sort="sourceType"-->Source Type<!--/a--></th>
									<th></th>
								</tr>
							</thead>
							<tbody></tbody>
						</table>
					</div>
				</div>

				<div id="virtual-type-detail" class="" style="display: none;">

					<div class="flex justify-between">
						<div class="font-bold mb-4 text-xl px-2">Virtual Type</div>
						<div class="resource-link"><a target="_blank" href="">/</a></div>
					</div>

					<table id="virtual-type-detail-table">
						<thead>
							<tr class="odd:bg-white even:bg-gray-75">
								<th class="p-2 pt-1 w-24" data-comment="The position attribute for virtual types is mostly for sorting them in the left column">Position</th>
								<th class="p-2 pt-1" data-comment="The name under which the virtual type can be queried via REST">Name</th>
								<th class="p-2 pt-1" data-comment="Specifies the source type of the type mapping">Source Type</th>
								<th class="p-2 pt-1" data-comment="Can be used to remove entities from the target collection, e.g. filter entities with invalid names etc. The filter expression is a StructrScript (JavaScript may severely impact performance) expression that will be called for every entity in the source collection, with the current entity being available under the keyword <code>this</code>.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with \${}">Filter Expression</th>
								<th class="p-2 pt-1 w-32 text-center">${Structr.abbreviations['visibleToPublicUsers']}</th>
								<th class="p-2 pt-1 w-32 text-center">${Structr.abbreviations['visibleToAuthenticatedUsers']}</th>
							</tr>
						</thead>
						<tbody>
							<tr class="odd:bg-white even:bg-gray-75">
								<td class="px-4">
									<input class="property w-full box-border" data-property="position">
								</td>
								<td class="px-4">
									<input class="property w-full box-border" data-property="name">
								</td>
								<td class="px-4">
									<input class="property w-full box-border" data-property="sourceType">
								</td>
								<td class="px-4">
									<textarea class="property w-full box-border" data-property="filterExpression"></textarea>
								</td>
								<td class="text-center">
									<input class="property" type="checkbox" data-property="visibleToPublicUsers">
								</td>
								<td class="text-center">
									<input class="property" type="checkbox" data-property="visibleToAuthenticatedUsers">
								</td>
							</tr>
						</tbody>
					</table>

					<div id="virtual-properties">

						<div class="font-bold mb-4 text-lg px-2">Virtual Properties</div>

						<table id="virtual-properties-table">
							<thead>
								<tr class="odd:bg-white even:bg-gray-75">
									<th class="p-2 pt-1 w-24">Actions</th>
									<th class="p-2 pt-1 w-32 position" data-comment="Specifies the position of the attribute in the output JSON document and the order in this interface">position</th>
									<th class="p-2 pt-1 sourceName" data-comment="Specifies the attribute name of the source entity">sourceName</th>
									<th class="p-2 pt-1 targetName" data-comment="The desired target attribute name. If left empty, the source attribute name is used">targetName</th>
									<th class="p-2 pt-1 inputFunction" data-comment="An optional input transformation when writing to this virtual property. The input data is made available via the keyword <code>input</code>.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with \${}">inputFunction</th>
									<th class="p-2 pt-1 outputFunction" data-comment="An optional scripting expression that creates or transforms the output value for the current attribute. It can be either a constant value, or a function that transforms the input value, which is provided using the keyword <code>input</code>.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with \${}">outputFunction</th>
									<th class="p-2 pt-1 w-32 text-center">${Structr.abbreviations['visibleToPublicUsers']}</th>
									<th class="p-2 pt-1 w-32 text-center">${Structr.abbreviations['visibleToAuthenticatedUsers']}</th>
								</tr>
							</thead>
							<tbody></tbody>
						</table>

						<button class="create hover:bg-gray-100 focus:border-gray-666 active:border-green inline-flex items-center">
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['mr-2', 'icon-green'])} New Virtual Property
						</button>
					</div>
				</div>
			</div>
		`,
		functions: config => `
			<form id="create-virtual-type-form" autocomplete="off" class="inline-flex">

				<input title="Enter a name for the virtual type" id="virtual-type-name-preselect" type="text" size="10" placeholder="Name" required class="mr-2">
				<input title="Enter the name of the source type of the virtual type" id="virtual-type-source-type-preselect" type="text" size="12" placeholder="Source Type" required class="mr-2">

				<button type="submit" form="create-virtual-type-form" class="btn action inline-flex items-center" id="create-virtual-type">
					${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['mr-2'])} New Virtual Type
				</button>

			</form>

			<div id="virtual-types-pager"></div>
		`,
		propertyRow: config => `
			<tr class="virtual-property odd:bg-white even:bg-gray-75">
				<td class="actions px-4">
					${_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-green', 'mr-2', 'save-virtual-property']))}
					${_Icons.getSvgIcon(_Icons.iconTrashcan,      20, 20, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-virtual-property']), 'Remove')}
				</td>
				<td class="px-4">
					<input class="property w-full box-border" data-property="position">
				</td>
				<td class="px-4">
					<input class="property w-full box-border" data-property="sourceName">
				</td>
				<td class="px-4">
					<input class="property w-full box-border" data-property="targetName">
				</td>
				<td class="px-4">
					<textarea class="property w-full box-border" data-property="inputFunction" cols="25"></textarea>
				</td>
				<td class="px-4">
					<textarea class="property w-full box-border" data-property="outputFunction" cols="25"></textarea>
				</td>
				<td class="text-center">
					<input class="property" data-property="visibleToPublicUsers" type="checkbox" checked>
				</td>
				<td class="text-center">
					<input class="property" data-property="visibleToAuthenticatedUsers" type="checkbox" checked>
				</td>
			</tr>
		`,
		typeRow: config => `
			<tr id="virtual-type-${config.virtualType.id}" class="virtual-type-row">
				<td class="position"></td>
				<td class="name allow-break"></td>
				<td class="sourceType"></td>
				<td class="actions">
					<div class="icons-container flex items-center justify-end"></div>
				</td>
			</tr>
		`,
	}
};