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
var _Dialogs = {

	showFolderDialog: function(entity, el) {
	},

	showFileDialog: function(entity, el) {
	},

	showImageDialog: function(entity, el) {
	},

	showUserDialog: function(entity, el) {
	},

	showGroupDialog: function(entity, el) {
	},

	showElementDialog: function(entity, el) {

		var id = entity.id;

		el.append('<h3>Most used HTML properties</h3>');
		el.append('<table class="props" id="html-properties"></table>');

		el.append('<h3>Properties for type ' + entity.tag + '</h3>');
		el.append('<table class="props" id="type-properties"></table>');

		var typeProperties = $('#type-properties');
		var htmlProperties = $('#html-properties');

		_Dialogs.appendLabeledInput(htmlProperties, 'class', '_html_class', entity);
		_Dialogs.appendLabeledInput(htmlProperties, 'id', '_html_id', entity);
		_Dialogs.appendLabeledInput(htmlProperties, 'style', '_html_style', entity);

		$.ajax({
			url: rootUrl + '_schema/' + entity.type + '/main',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			success: function(data) {
				$(data.result).each(function(i, prop) {
					_Dialogs.appendLabeledInput(typeProperties, prop.jsonName.replace('_html_', ''), prop.jsonName, entity);
				});
			}
		});

		$('.props input', dialog).each(function(i, v) {
			_Entities.activateInput(v, id);
		});

		$('.props .nullIcon', dialog).each(function(i, icon) {

			$(icon).on('click', function() {

				var key = $(this).prop('id').substring(5);
				var input = $('input[name=' + key + ']');

				_Entities.setProperty(id, key, null, false, function(newVal) {
					if (!newVal) {
						blinkGreen(input);
					} else {
						blinkRed(input);
					}
					input.val(newVal);
				});
			});
		});
	},

	appendLabeledInput: function(el, label, name, entity) {

		var value = entity[name];

		el.append(
			'<tr>'
			+ '<td class="key"><label>' + label + '</label></td>'
			+ '<td class="value"><input type="text" name="' + name + '" value="' + (value ? value : '') + '"></td>'
			+ '<td><img class="nullIcon" id="null_' + name + '" src="icon/cross_small_grey.png"></td>'
		);
	}
};