/*
 * Copyright (C) 2010-2020 Structr GmbH
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
	appsContainer: undefined,
	deployServletAvailable: true,

	init: function() {},
	unload: function() {},
	onload: function() {

		_Apps.init();

		fetch('/structr/deploy').then((result) => {
			_Apps.deployServletAvailable = (result.status !== 404);
		}).then(() => {

			Structr.fetchHtmlTemplate('apps/apps', {hideInfo: _Apps.deployServletAvailable}, function(html) {

				main.append(html);

				_Apps.appsContainer = $('#apps', main);

				_Apps.loadData();

				$(window).off('resize');
				$(window).on('resize', function() {
					Structr.resize();
				});

				Structr.unblockMenu(100);
			});
		});
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

			_Apps.appsContainer.append(html);
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

			let $tile = $(tile);
			container.append($tile);

			let form = $tile.find('form');
			if (form.length > 0) {
				form[0].addEventListener('submit', (e) => {
					e.preventDefault();

					if (_Apps.deployServletAvailable) {

						Structr.confirmation(
							'<h3>Install "' + app.name + '"?</h3><p>The current application will be <b>REMOVED</b>!</p><p>Make sure you have a backup or nothing important in this installation!</p>',
							function() {
								$.unblockUI({ fadeOut: 25 });

								LSWrapper.removeItem(_Schema.hiddenSchemaNodesKey);
								LSWrapper.save(function() {
									form.submit();
								});
							}
						);

					} else {
						_Apps.showDeploymentServletUnavailableMessage(app);
					}

					return false;
				});
			}
		});
	},
	showDeploymentServletUnavailableMessage: function(app) {

		new MessageBuilder().title('Unable to install "' + app.name + '"').warning('The <code>DeploymentServlet</code> needs to be activated in <code>structr.conf</code> for the installation process to work.').requiresConfirmation().allowConfirmAll().show();
	}
};