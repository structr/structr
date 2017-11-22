/*
 * Copyright (C) 2010-2017 Structr GmbH
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

	init: function() {},
	unload: function() {},
	onload: function() {
		Structr.updateMainHelpLink('https://support.structr.com/article/233');

		main.append(
			'<div id="virtual-types-main">' +
				'<div id="virtual-types-list" class="resourceBox full-height-box">' +
					'<div id="virtual-types-pager"></div>' +
					'<button id="create-virtual-type"><i class="' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" /> New Virtual Type</button>' +
					'<table id="virtual-types-table">' +
						'<thead><tr>' +
							'<th class="narrow"><a class="sort" data-sort="position">Pos.</a></th>' +
							'<th><a class="sort" data-sort="name">Name</a></th>' +
							'<th><a class="sort" data-sort="sourceType">Source Type</a></th>' +
							'<th class="narrow">Actions</th>' +
						'</tr></thead>' +
						'<tbody></tbody>' +
					'</table>' +
				'</div>' +
				'<div id="virtual-type-detail" class="resourceBox full-height-box">' +
					'<div class="resource-link hide-in-create-mode"><a target="_blank" href="">/</a></div>' +
					'<h2><span class="show-in-create-mode">New </span>Virtual Type</h2>' +
					'<table id="virtual-type-detail-table">' +
						'<thead><tr>' +
							'<th class="position">Position</th>' +
							'<th class="name">Name</th>' +
							'<th class="sourceType">Source Type</th>' +
							'<th class="filterExpression">Filter Expression</th>' +
							'<th>Visible To Public Users</th>' +
							'<th>Visible To Authenticated Users</th>' +
							'<th class="show-in-create-mode"></th>' +
						'</tr></thead>' +
						'<tbody><tr>' +
							'<td><input class="property" data-property="position" size="3" /></td>' +
							'<td><input class="property" data-property="name" /></td>' +
							'<td><input class="property" data-property="sourceType" /></td>' +
							'<td><textarea class="property" data-property="filterExpression"></textarea></td>' +
							'<td><input class="property" type="checkbox" data-property="visibleToPublicUsers" /></td>' +
							'<td><input class="property" type="checkbox" data-property="visibleToAuthenticatedUsers" /></td>' +
							'<td class="show-in-create-mode actions"></td>' +
						'</tr></tbody>' +
					'</table>' +
					'<div id="virtual-properties" class="hide-in-create-mode">' +
						'<h3>Virtual Properties</h3>' +
						'<table id="virtual-properties-table">' +
							'<thead><tr>' +
								'<th class="position">position</th>' +
								'<th class="sourceName">sourceName</th>' +
								'<th class="targetName">targetName</th>' +
								'<th class="inputFunction">inputFunction</th>' +
								'<th class="outputFunction">outputFunction</th>' +
								'<th>Visible To Public Users</th>' +
								'<th>Visible To Authenticated Users</th>' +
								'<th>Actions</th>' +
							'</tr></thead>' +
							'<tbody></tbody>' +
						'</table>' +
					'</div>' +
				'</div>' +
			'</div>'
		);

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

		$('<i class=" button ' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" />').on('click', function () {
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

		_VirtualTypes.appendInfoTextToColumnHeader($('#virtual-type-detail-table th.position'),         "The position attribute for virtual types is mostly for sorting them in the left column");
		_VirtualTypes.appendInfoTextToColumnHeader($('#virtual-type-detail-table th.name'),             "The name under which the virtual type can be queried via REST");
		_VirtualTypes.appendInfoTextToColumnHeader($('#virtual-type-detail-table th.sourceType'),       "Specifies the source type of the type mapping");
		_VirtualTypes.appendInfoTextToColumnHeader($('#virtual-type-detail-table th.filterExpression'), "Can be used to remove entities from the target collection, e.g. filter entities with invalid names etc. The filter expression is a StructrScript (JavaScript may severely impact performance) expression that will be called for every entity in the source collection, with the current entity being available under the keyword <code>this</code>.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}");

		_VirtualTypes.appendInfoTextToColumnHeader($('#virtual-properties-table th.position'),       "Specifies the position of the property in the output JSON document");
		_VirtualTypes.appendInfoTextToColumnHeader($('#virtual-properties-table th.sourceName'),     "Specifies the property key of the source entity");
		_VirtualTypes.appendInfoTextToColumnHeader($('#virtual-properties-table th.targetName'),     "The desired target property name. If left empty, the source property name is used");
		_VirtualTypes.appendInfoTextToColumnHeader($('#virtual-properties-table th.inputFunction'),  "An optional input transformation when writing to this virtual property. The input data is made available via the keyword <code>input</code>.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}");
		_VirtualTypes.appendInfoTextToColumnHeader($('#virtual-properties-table th.outputFunction'), "An optional scripting expression that creates or transforms the output value for the current property. It can be either a constant value, or a function that transforms the input value, which is provided using the keyword <code>input</code>.<br><br>This is an 'auto-script' environment, meaning that the text is automatically surrounded with ${}");

		$(window).off('resize');
		$(window).on('resize', function() {
			Structr.resize();
		});

		Structr.unblockMenu(100);
	},
	appendInfoTextToColumnHeader: function(el, text) {
		Structr.appendInfoTextToElement({
			element: el,
			text: text,
			css: { marginLeft: "5px" }
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
		_Pager.initPager('virtual-types', 'VirtualType', 1, 25, 'name', 'asc');

		_VirtualTypes.virtualTypesPager = _Pager.addPager('virtual-types', $('#virtual-types-pager'), false, 'VirtualType', 'ui', _VirtualTypes.processPagerData);

		_VirtualTypes.virtualTypesPager.cleanupFunction = function () {
			fastRemoveAllChildren(_VirtualTypes.virtualTypesList[0]);
		};
		_VirtualTypes.virtualTypesPager.pager.append('<br>Filters: <input type="text" class="filter w100 virtual-type-name" data-attribute="name" placeholder="Name" />');
		_VirtualTypes.virtualTypesPager.pager.append('<input type="text" class="filter w100 virtual-type-sourceType" data-attribute="sourceType" placeholder="Source Type" />');
		_VirtualTypes.virtualTypesPager.activateFilterElements();

		$('#virtual-types-table .sort').on('click', function () {
			_VirtualTypes.virtualTypesPager.setSortKey($(this).data('sort'));
		});

	},
	processPagerData: function (pagerData) {
		if (pagerData && pagerData.length) {
			pagerData.forEach(_VirtualTypes.appendVirtualType);
		}
	},
	appendVirtualType: function (virtualType) {

		var row = $(
			'<tr id="virtual-type-' + virtualType.id + '">' +
				'<td class="position"></td>' +
				'<td class="name"></td>' +
				'<td class="sourceType"></td>' +
				'<td class="actions"></td>' +
			'</tr>'
		);

		_VirtualTypes.populateVirtualTypeRow(row, virtualType);
		_VirtualTypes.virtualTypesList.append(row);

		var actionsCol = $('.actions', row);

		$('<a title="Edit Properties" class="properties"><i class=" button ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" /></a>').on('click', function () {
			_VirtualTypes.showVirtualTypeDetails(virtualType.id);
		}).appendTo(actionsCol);

		_VirtualTypes.appendDeleteIcon(virtualType.id, row, actionsCol, 'Do you really want to delete the virtual type "' + virtualType.name + '" with all its virtual properties?', function() {

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
	deleteVirtualType: function(id) {

		console.log('DELETE id: ' + id + ' + all properties!');
		console.log('If this ID is currently active, remove it!');

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
	appendVirtualProperty: function(optionalProperty) {

		var row = $(
			'<tr class="virtual-property">' +
				'<td><input class="property" data-property="position" size="3"></td>' +
				'<td><input class="property" data-property="sourceName"></td>' +
				'<td><input class="property" data-property="targetName"></td>' +
				'<td><textarea class="property" data-property="inputFunction" cols="40"></textarea></td>' +
				'<td><textarea class="property" data-property="outputFunction" cols="40"></textarea></td>' +
				'<td><input class="property" data-property="visibleToPublicUsers" type="checkbox" checked></td>' +
				'<td><input class="property" data-property="visibleToAuthenticatedUsers" type="checkbox" checked></td>' +
				'<td class="actions"></td>' +
			'</tr>'
		);

		var actionsCol = $('.actions', row);

		if (optionalProperty) {
			row.data('virtual-property-id', optionalProperty.id);

			_VirtualTypes.appendDeleteIcon(optionalProperty.id, row, actionsCol, 'Do you really want to delete the virtual property?', function() {
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

					_VirtualTypes.appendDeleteIcon(row.data('virtual-property-id'), row, actionsCol, 'Do you really want to delete the virtual property?', function() {
						_VirtualTypes.deleteVirtualProperty(row.data('virtual-property-id'), row);
					});
				});

			}).appendTo(actionsCol);

			$('<i class=" button ' + _Icons.getFullSpriteClass(_Icons.cross_icon) + '" />').on('click', function () {
				row.remove();
			}).appendTo(actionsCol);
		}

		_VirtualTypes.virtualPropertiesTableBody.append(row);

		return row;
	},
	appendDeleteIcon: function(id, row, insertPoint, confirmText, deletionActionCallback) {

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