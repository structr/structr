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

var _VirtualTypes = {
	_moduleName: 'virtual-types',

	virtualTypesPager: undefined,
	virtualTypesList: undefined,
	virtualTypeDetail: undefined,
	virtualTypeDetailTableRow: undefined,
	virtualPropertiesTableBody: undefined,
	resourceLink: undefined,

	virtualTypesResizerLeftKey: 'structrVirtualTypesResizerLeftKey_' + port,

	init: function() {},
	unload: function() {},
	onload: async function() {

		Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('virtual-types'));

		let html = await Structr.fetchHtmlTemplate('virtual-types/main', {});

		main.append(html);

		$('#create-virtual-type').on('click', function() {
			_VirtualTypes.clearVirtualTypeDetails();

			_VirtualTypes.enableCreateMode();
			_VirtualTypes.virtualTypeDetail.show();
		});

		_VirtualTypes.virtualTypesList = $('#virtual-types-table tbody');
		_VirtualTypes.listVirtualTypes();

		_VirtualTypes.virtualTypeDetail = $('#virtual-type-detail').hide();
		_VirtualTypes.resourceLink = $('.resource-link a', _VirtualTypes.virtualTypeDetail);
		_VirtualTypes.virtualTypeDetailTableRow = $('#virtual-type-detail-table tbody tr');
		_VirtualTypes.virtualPropertiesTableBody = $('#virtual-properties-table tbody');

		$('<button class="create"><i class="' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" /> New Virtual Property</button>').on('click', function() {
			_VirtualTypes.appendVirtualProperty();
		}).appendTo($('#virtual-properties', _VirtualTypes.virtualTypeDetail));

		_VirtualTypes.registerChangeListeners();

		var actionsCol = $('.actions', _VirtualTypes.virtualTypeDetail);

		$('<i class="button ' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" />').on('click', function() {

			var data = _VirtualTypes.getVirtualObjectDataFromRow(_VirtualTypes.virtualTypeDetailTableRow);

			_VirtualTypes.saveVirtualObject('VirtualType', data, _VirtualTypes.virtualTypeDetailTableRow, function (data) {
				var createdId = data.result[0];
				_VirtualTypes.virtualTypeDetailTableRow.data('virtual-type-id', createdId);
				_VirtualTypes.updateResourceLink(_VirtualTypes.getVirtualObjectDataFromRow(_VirtualTypes.virtualTypeDetailTableRow));

				_VirtualTypes.disableCreateMode();
				_VirtualTypes.virtualTypesPager.refresh();
			});

		}).appendTo(actionsCol);

		$('<i class="button ' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" />').on('click', function () {
			_VirtualTypes.clearVirtualTypeDetails();

			_VirtualTypes.virtualTypeDetail.hide();
		}).appendTo(actionsCol);

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
	},
	resize: function() {
		_VirtualTypes.moveResizer();
		Structr.resize();
	},
	moveResizer: function(left) {
		left = left || LSWrapper.getItem(_VirtualTypes.virtualTypesResizerLeftKey) || 300;
		$('.column-resizer', main).css({ left: left });

		$('#virtual-types-list').css({width: left - 25 + 'px'});
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
	enableCreateMode: function() {
		$('.show-in-create-mode', _VirtualTypes.virtualTypeDetail).show();
		$('.hide-in-create-mode', _VirtualTypes.virtualTypeDetail).hide();
	},
	disableCreateMode: function() {
		$('.show-in-create-mode', _VirtualTypes.virtualTypeDetail).hide();
		$('.hide-in-create-mode', _VirtualTypes.virtualTypeDetail).show();
	},
	registerChangeListeners: function() {

		$(_VirtualTypes.virtualTypeDetailTableRow).on('change', '.property', function() {
			var el = $(this);
			var row = el.closest('tr');
			var typeId = row.data('virtual-type-id');
			if (typeId) {
				var data = _VirtualTypes.getVirtualObjectDataFromRow(row);
				_VirtualTypes.updateVirtualObject('VirtualType', typeId, data, el, el.closest('td'), function() {
					var rowInList = $('#virtual-type-' + typeId, _VirtualTypes.virtualTypesList);
					_VirtualTypes.updateResourceLink(data);
					_VirtualTypes.populateVirtualTypeRow(rowInList, data);
				});
			}
		});

		$(_VirtualTypes.virtualPropertiesTableBody).on('change', '.property', function() {
			var el = $(this);
			var row = el.closest('tr');
			var propertyId = row.data('virtual-property-id');
			if (propertyId) {
				var data = _VirtualTypes.getVirtualObjectDataFromRow(row);
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
		_VirtualTypes.virtualTypesPager.pager.append('<br>Filters: <input type="text" class="filter w100 virtual-type-name" data-attribute="name" placeholder="Name" />');
		_VirtualTypes.virtualTypesPager.pager.append('<input type="text" class="filter w100 virtual-type-sourceType" data-attribute="sourceType" placeholder="Source Type" />');
		_VirtualTypes.virtualTypesPager.activateFilterElements();

		pagerEl.append('<div style="clear:both;"></div>');

		$('#virtual-types-table .sort').on('click', function () {
			_VirtualTypes.virtualTypesPager.setSortKey($(this).data('sort'));
		});
	},
	processPagerData: function (pagerData) {
		if (pagerData && pagerData.length) {
			pagerData.forEach(_VirtualTypes.appendVirtualType);
		}
	},
	appendVirtualType: async function (virtualType) {

		let html = await Structr.fetchHtmlTemplate('virtual-types/row.type', {virtualType: virtualType});

		var row = $(html);

		_VirtualTypes.populateVirtualTypeRow(row, virtualType);
		_VirtualTypes.virtualTypesList.append(row);

		var actionsCol = $('.actions', row);

		$('<a title="Edit Properties" class="properties"><i class=" button ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" /></a>').on('click', function () {
			_VirtualTypes.showVirtualTypeDetails(virtualType.id);
		}).appendTo(actionsCol);

		_VirtualTypes.appendDeleteIcon(actionsCol, 'Do you really want to delete the virtual type "' + virtualType.name + '" with all its virtual properties?', function() {

			Command.get(virtualType.id, 'id,properties', function(vt) {

				vt.properties.forEach(function(vp) {
					Command.deleteNode(vp.id);
				});

				Command.deleteNode(vt.id);

				if (virtualType.id === _VirtualTypes.virtualTypeDetailTableRow.data('virtual-type-id')) {
					_VirtualTypes.clearVirtualTypeDetails();
					_VirtualTypes.virtualTypeDetail.hide();
				}

				row.remove();
			});
		});
	},
	populateVirtualTypeRow: function(row, virtualTypeData) {
		$('.position', row).text(virtualTypeData.position !== null ? virtualTypeData.position : "");
		$('.name', row).text(virtualTypeData.name !== null ? virtualTypeData.name : "");
		$('.sourceType', row).text(virtualTypeData.sourceType !== null ? virtualTypeData.sourceType : "");
	},
	getVirtualObjectDataFromRow: function(row) {
		var data = {};

		$('.property', row).each(function(idx, el) {
			var el = $(el);

			if (el.attr('type') === 'checkbox') {
				data[el.data('property')] = el.prop('checked');
			} else {
				var val = el.val();
				if (val === "") {
					val = null;
				}
				data[el.data('property')] = val;
			}
		});

		return data;
	},
	showVirtualTypeDetails:function(virtualTypeId) {

		Command.get(virtualTypeId, '', function(vt) {

			_VirtualTypes.virtualTypeDetailTableRow.data('virtual-type-id', virtualTypeId);

			$('.property', _VirtualTypes.virtualTypeDetailTableRow).each(function(idx, el) {
				var el = $(el);
				var val = vt[el.data('property')];

				if (el.attr('type') === 'checkbox') {
					el.prop('checked', (val === true));
				} else {
					el.val(val);
				}
			});

			_VirtualTypes.updateResourceLink(vt);
			_VirtualTypes.listVirtualProperties(vt.properties);
			_VirtualTypes.disableCreateMode();
			_VirtualTypes.virtualTypeDetail.show();
		});
	},
	updateResourceLink: function (virtualType) {
		_VirtualTypes.resourceLink.attr('href' , '/structr/rest/' + virtualType.name + '?pageSize=1');
		_VirtualTypes.resourceLink.text('/' + virtualType.name);
	},
	clearVirtualTypeDetails: function() {

		_VirtualTypes.virtualTypeDetailTableRow.removeData('virtual-type-id');

		$('.property', _VirtualTypes.virtualTypeDetailTableRow).each(function(idx, el) {
			var el = $(el);

			if (el.attr('type') === 'checkbox') {
				el.prop('checked', true);
			} else {
				el.val("");
			}
		});

		_VirtualTypes.clearVirtualPropertyTable();
	},
	clearVirtualPropertyTable: function() {
		fastRemoveAllChildren(_VirtualTypes.virtualPropertiesTableBody[0]);
	},
	listVirtualProperties:function(properties) {

		properties.sort(function(a, b) {
			return (a.position === b.position) ? 0 : (a.position > b.position) ? 1 : -1;
		});

		_VirtualTypes.clearVirtualPropertyTable();

		properties.forEach(function(p) {
			_VirtualTypes.appendVirtualProperty(p);
		});
	},
	appendVirtualProperty: async function(optionalProperty) {

		let html = await Structr.fetchHtmlTemplate('virtual-types/row.property', {});

		var row = $(html);

		var actionsCol = $('.actions', row);

		if (optionalProperty) {
			row.data('virtual-property-id', optionalProperty.id);

			_VirtualTypes.appendDeleteIcon(actionsCol, 'Do you really want to delete the virtual property?', function() {
				_VirtualTypes.deleteVirtualProperty(row.data('virtual-property-id'), row);
			});

			$('.property', row).each(function(idx, el) {
				var el = $(el);
				var val = optionalProperty[el.data('property')];

				if (el.attr('type') === 'checkbox') {
					el.prop('checked', (val === true));
				} else {
					el.val(val);
				}
			});

		} else {

			$('<i class="button ' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" />').on('click', function() {

				var data = _VirtualTypes.getVirtualObjectDataFromRow(row);
				data.virtualType = _VirtualTypes.virtualTypeDetailTableRow.data('virtual-type-id');

				_VirtualTypes.saveVirtualObject('VirtualProperty', data, row, function (data) {
					row.data('virtual-property-id', data.result[0]);
					fastRemoveAllChildren(actionsCol[0]);

					_VirtualTypes.appendDeleteIcon(actionsCol, 'Do you really want to delete the virtual property?', function() {
						_VirtualTypes.deleteVirtualProperty(row.data('virtual-property-id'), row);
					});
				});

			}).appendTo(actionsCol);

			$('<i class=" button ' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" />').on('click', function () {
				row.remove();
			}).appendTo(actionsCol);
		}

		_VirtualTypes.virtualPropertiesTableBody.append(row);
	},
	appendDeleteIcon: function(insertPoint, confirmText, deletionActionCallback) {

		$('<a title="Delete" class="delete"><i class=" button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" /></a>').on('click', function() {
			if (true === confirm(confirmText)) {
				deletionActionCallback();
			}
		}).appendTo(insertPoint);
	},
	saveVirtualObject:function (type, data, row, successCallback) {

		$.ajax({
			url: rootUrl + type,
			type: 'POST',
			dataType: 'json',
			data: JSON.stringify(data),
			contentType: 'application/json; charset=utf-8',
			success: function(data) {
				blinkGreen($('td', row));

				if (typeof(successCallback) === "function") {
					successCallback(data);
				}
			},
			error: function () {
				blinkRed($('td', row));
			}
		});
	},
	updateVirtualObject:function (type, id, newData, $el, $blinkTarget, successCallback) {

		$.ajax({
			url: rootUrl + type + '/' + id,
			type: 'PUT',
			dataType: 'json',
			data: JSON.stringify(newData),
			contentType: 'application/json; charset=utf-8',
			success: function() {
				blinkGreen(($blinkTarget ? $blinkTarget : $el));
				if (typeof(successCallback) === "function") {
					successCallback();
				}
			},
			error: function () {
				blinkRed(($blinkTarget ? $blinkTarget : $el));
			}
		});
	},
	deleteVirtualProperty: function(id, row) {

		$.ajax({
			url: rootUrl + 'VirtualProperty/' + id,
			type: 'DELETE',
			contentType: 'application/json; charset=utf-8',
			success: function() {
				row.remove();
			},
			error: function () {
				blinkRed($('td', row));
			}
		});
	}
};