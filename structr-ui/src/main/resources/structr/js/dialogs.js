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

/*
		el.append('<table class="props"></table>');
		var t = $('.props', el);

		t.append('<tr><td class="key">Query auto-limit</td><td class="value" id="queryAutoLimit"></td></tr>');
		t.append('<tr><td class="key">Hide in index mode</td><td  class="value" id="hideIndexMode"></td></tr>');
		t.append('<tr><td class="key">Hide in details mode</td><td  class="value" id="hideDetailsMode"></td></tr>');

		_Entities.appendBooleanSwitch($('#queryAutoLimit', t), entity, 'renderDetails', ['Query is limited', 'Query is not limited'], 'Limit result to the object with the ID the URL ends with.');
		_Entities.appendBooleanSwitch($('#hideIndexMode', t), entity, 'hideOnIndex', ['Hidden in index mode', 'Visible in index mode'], 'if URL does not end with an ID');
		_Entities.appendBooleanSwitch($('#hideDetailsMode', t), entity, 'hideOnDetail', ['Hidden in details mode', 'Visible in details mode'], 'if URL ends with an ID.');

		el.append('<div id="data-tabs" class="data-tabs"><ul><li class="active" id="tab-rest">REST Query</li><li id="tab-cypher">Cypher Query</li><li id="tab-xpath">XPath Query</li></ul>'
				+ '<div id="content-tab-rest"></div><div id="content-tab-cypher"></div><div id="content-tab-xpath"></div></div>');

		_Entities.appendTextarea($('#content-tab-rest'), entity, 'restQuery', 'REST Query', '');
		_Entities.appendTextarea($('#content-tab-cypher'), entity, 'cypherQuery', 'Cypher Query', '');
		_Entities.appendTextarea($('#content-tab-xpath'), entity, 'xpathQuery', 'XPath Query', '');

		_Entities.appendInput(el, entity, 'dataKey', 'Data Key', 'The data key is either a word to reference result objects, or it can be the name of a collection property of the result object.<br>' +
				'You can access result objects or the objects of the collection using ${<i>&lt;dataKey&gt;.&lt;propertyKey&gt;</i>}');

 */