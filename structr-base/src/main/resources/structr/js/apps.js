/*
 * Copyright (C) 2010-2024 Structr GmbH
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
	Structr.registerModule(_Apps);
});

let _Apps = {
	_moduleName: 'apps',
	appsContainer: undefined,
	deployServletAvailable: true,

	init: function() {},
	unload: function() {},
	onload: function() {

		_Apps.init();

		fetch(Structr.deployRoot).then((result) => {
			_Apps.deployServletAvailable = (result.status !== 404);
		}).then(() => {

			Structr.setMainContainerHTML(_Apps.templates.apps({ hideInfo: _Apps.deployServletAvailable }));

			_Apps.appsContainer = Structr.mainContainer.querySelector('#apps');

			_Apps.loadData();

			Structr.mainMenu.unblock(100);
		});
	},
	loadData: async () => {

		let response = await fetch('https://structr.com/structr/rest/StructrApplicationCategory?' + Structr.getRequestParameterName('sort') + '=position');

		if (response.ok) {
			let data = await response.json();

			for (let category of data.result) {
				_Apps.appendCategory(category);
			}
		}
	},
	appendCategory: (category) => {

		_Apps.appsContainer.insertAdjacentHTML('beforeend', _Apps.templates.category(category));

		let container = $('#' + category.id);

		_Helpers.sort(category.apps, 'position');

		for (let app of category.apps) {
			_Apps.appendTile(container, app);
		}
	},
	appendTile: (container, app) => {

		let tile = _Apps.templates.tile(app);

		let $tile = $(tile);
		container.append($tile);

		let form = $tile.find('form');
		if (form.length > 0) {
			form[0].addEventListener('submit', async (e) => {
				e.preventDefault();

				if (_Apps.deployServletAvailable) {

					let confirm = await _Dialogs.confirmation.showPromise(`
						<h3>Install "${app.name}"?</h3>
						<p>The current application will be <b>REMOVED</b>!</p>
						<p>Make sure you have a backup or nothing important in this installation!</p>
					`);

					if (confirm === true) {

						LSWrapper.removeItem(_Schema.hiddenSchemaNodesKey);
						LSWrapper.save(() => {
							form.submit();
						});
					}

				} else {
					_Apps.showDeploymentServletUnavailableMessage(app);
				}

				return false;
			});
		}
	},
	showDeploymentServletUnavailableMessage: (app) => {

		new WarningMessage()
			.title(`Unable to install "${app.name}"`)
			.text('The <code>DeploymentServlet</code> needs to be activated in <code>structr.conf</code> for the installation process to work.')
			.requiresConfirmation()
			.show();
	},

	templates: {
		apps: config => `
			<div class="main-app-box" id="apps"></div>
		`,
		category: config => `
			<div class="app-category">
				<h2>${config.name}</h2>
				<p>${config.description}</p>
				<div id="${config.id}"></div>
			</div>
		`,
		tile: config => `
			<div class="app-tile">
				<h4>${config.name}</h4>
				<p>${config.description}</p>
				<form action="/structr/deploy" method="POST" enctype="multipart/form-data">
					<input type="hidden" name="downloadUrl" value="${config.url}">
					<input type="hidden" name="redirectUrl" value="${window.location.pathname}">
					<input type="hidden" name="mode" value="import">
					<button class="action" type="submit">Install</button>
				</form>
			</div>
		`,
	}
};