/*
 * Copyright (C) 2010-2019 Structr GmbH
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
	Structr.registerModule(_Apps);
});

var _Apps = {
	_moduleName: 'apps',

	init: function() {},
	unload: function() {},
	onload: function() {

		_Apps.init();
		main.append('<div id="apps"></div>');

		_Apps.loadData();

		$('.apps-info a.internal-link').on('click', function() {
			$('#' + $(this).text() + '_').click();
		});

		$(window).off('resize');
		$(window).on('resize', function() {
			Structr.resize();
		});

		Structr.unblockMenu(100);

	},
	loadData: function() {
		
		$.ajax({
			url: 'https://structr.com/structr/rest/StructrApplicationCategory?sort=position',
			statusCode: {
				200: function(result) {
					result.result.forEach(function(category) {
						_Apps.appendCategory(category);
					});
				}
			}
		});
	},
	appendCategory: function(category) {

		Structr.fetchHtmlTemplate('apps/category', category, function(html) {

			main.append(html);
			var container = $('#' + category.id);

			category.apps.sort(function(a, b) {
				if (a.position > b.position) return 1;
				if (a.position < b.position) return -1;
				return 0;
			});

			category.apps.forEach(function(app) {
				
				_Apps.appendTile(container, app);

			});
		});
	},
	appendTile: function(container, app) {
		
		Structr.fetchHtmlTemplate('apps/tile', app, function(tile) {
			container.append(tile);
			$('#_' + app.id).on('click', function() {
				_Apps.install(app);
			});
		});
	},
	install: function(app) {

		var data = {
			mode: 'import',
			extendExistingSchema: app.extendExistingSchema,
			downloadUrl: app.url
		};

		$.ajax({
			url: '/structr/deploy',
			data: JSON.stringify(data),
			contentType: 'multipart/form-data',
			method: 'POST',
			cache: false,
			contentType: false,
			processData: false
		});

	}
};