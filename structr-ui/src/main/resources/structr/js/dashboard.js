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
$(document).ready(function () {
	Structr.registerModule(_Dashboard);
});

var _Dashboard = {
	_moduleName: 'dashboard',
	dashboard: undefined,
	logInterval: undefined,
	activeTabPrefixKey: 'activeDashboardTabPrefix' + port,
	logRefreshTimeIntervalKey: 'dashboardLogRefreshTimeInterval' + port,
	logLinesKey: 'dashboardNumberOfLines' + port,

	init: function () {},
	unload: function () {
		window.clearInterval(_Dashboard.logInterval);
	},
	removeActiveClass: function (nodelist) {
		nodelist.forEach(function (el) {
			el.classList.remove('active');
		});
	},
	activateLastActiveTab: function() {
		let tabId = LSWrapper.getItem(_Dashboard.activeTabPrefixKey);
		if (tabId) {
			let tab = document.querySelector('#dashboard .tabs-menu li a[href="' + tabId + '"]');
			tab.click();
		}
	},
	onload: function() {

		_Dashboard.init();
		Structr.updateMainHelpLink('https://support.structr.com/article/202');

		let templateConfig = {};
		let releasesIndexUrl = '';
		let snapshotsIndexUrl = '';

		fetch(rootUrl + '/_env').then(function(response) {

			return response.json();

		}).then(function (data) {

			templateConfig.envInfo = data.result;

			templateConfig.envInfo.version = (data.result.components['structr'] || data.result.components['structr-ui']).version || '';
			templateConfig.envInfo.build   = (data.result.components['structr'] || data.result.components['structr-ui']).build   || '';
			templateConfig.envInfo.date    = (data.result.components['structr'] || data.result.components['structr-ui']).date    || '';

			releasesIndexUrl  = data.result.availableReleasesUrl;
			snapshotsIndexUrl = data.result.availableSnapshotsUrl;

			if (templateConfig.envInfo.startDate) {
				templateConfig.envInfo.startDate = _Dashboard.dateToIsoString(templateConfig.envInfo.startDate);
			}

			if (templateConfig.envInfo.endDate) {
				templateConfig.envInfo.endDate = _Dashboard.dateToIsoString(templateConfig.envInfo.endDate);
			}

			return fetch(rootUrl + '/me/ui');

		}).then(function (response) {

			return response.json();

		}).then(function (data) {

			templateConfig.meObj = data.result;

			return fetch('/structr/deploy');

		}).then((result) => {

			templateConfig.deployServletAvailable = (result.status !== 404);

		}).then(function() {

			Structr.fetchHtmlTemplate('dashboard/dashboard', templateConfig, function (html) {

				main.empty();
				main.append(html);

				_Dashboard.gatherVersionUpdateInfo(templateConfig.envInfo.version, releasesIndexUrl, snapshotsIndexUrl);

				document.querySelectorAll('#dashboard .tabs-menu li a').forEach(function(tabLink) {
					tabLink.addEventListener('click', function(e) {
						e.preventDefault();
						let targetId = e.target.getAttribute('href');

						_Dashboard.removeActiveClass(document.querySelectorAll('#dashboard .tabs-contents .tab-content'));
						_Dashboard.removeActiveClass(document.querySelectorAll('#dashboard .tabs-menu li'));

						e.target.closest('li').classList.add('active');
						document.querySelector(targetId).classList.add('active');
						LSWrapper.setItem(_Dashboard.activeTabPrefixKey, targetId);
					});
				});

				document.querySelector('#clear-local-storage-on-server').addEventListener('click', function () {
					_Dashboard.clearLocalStorageOnServer(templateConfig.meObj.id);
				});

				_Dashboard.checkLicenseEnd(templateConfig.envInfo, $('#dash-about-structr .end-date'));

				$('button#do-import').on('click', function () {
					var location = $('#deployment-source-input').val();
					if (location && location.length) {
						_Dashboard.deploy('import', location);
					} else {
						// show error message
					}
				});

				$('button#do-export').on('click', function () {
					var location = $('#deployment-target-input').val();
					if (location && location.length) {
						_Dashboard.deploy('export', location);
					} else {
						// show error message
					}
				});

				_Dashboard.activateLogBox();
				_Dashboard.activateLastActiveTab();
				_Dashboard.appendGlobalSchemaMethods($('#dash-global-schema-methods'));
				_Dashboard.appendDatabaseSelectionBox();

				$(window).off('resize');
				$(window).on('resize', function () {
					Structr.resize();
				});

				Structr.unblockMenu(100);
			});
		});
	},
	gatherVersionUpdateInfo(currentVersion, releasesIndexUrl, snapshotsIndexUrl) {

		let releaseInfo = '';
		let snapshotInfo = '';

		let gatherVersionUpdateInfoFinished = () => {

			let versionUpdateInfoElement = document.querySelector('#version-update-info');
			if (versionUpdateInfoElement) {

				let versionInfo = [];
				if (releaseInfo !== '') {
					versionInfo.push(releaseInfo);
				}
				if (snapshotInfo !== '') {
					versionInfo.push(snapshotInfo);
				}

				if (versionInfo.length > 0) {
					versionUpdateInfoElement.textContent = '(' + versionInfo.join(' | ') + ')';
				}
			}
		};

		let requiredFetchCount = 0;
		if (releasesIndexUrl !== '') {
			requiredFetchCount++;

			// Search for newer releases and store latest version
			fetch(releasesIndexUrl).then((response) => {
				return response.text();

			}).then((releaseVersionsList) => {

				let newReleaseAvailable = undefined;

				releaseVersionsList.split(/[\n\r]/).forEach(function(version) {
					if (version > currentVersion) {
						newReleaseAvailable = version;
					}
				});

				releaseInfo = (newReleaseAvailable ? 'newer release available: ' +  newReleaseAvailable : 'no new release available');

				requiredFetchCount--;
				if (requiredFetchCount === 0) {
					gatherVersionUpdateInfoFinished();
				}
			});
		}

		if (snapshotsIndexUrl !== '') {
			requiredFetchCount++;

			fetch(snapshotsIndexUrl).then((response) => {
				return response.text();

			}).then((snapshotVersionsList) => {

				let newSnapshotAvailable = undefined;
				snapshotVersionsList.split(/[\n\r]/).forEach(function(version) {
					if (version > currentVersion) {
						newSnapshotAvailable = version;
					}
				});

				snapshotInfo = (newSnapshotAvailable ? 'newer snapshot available: ' +  newSnapshotAvailable : 'no new snapshot available');

				requiredFetchCount--;
				if (requiredFetchCount === 0) {
					gatherVersionUpdateInfoFinished();
				}
			});
		}
	},
	dateToIsoString: function(dateString) {
		let date = new Date(dateString);
		return date.getFullYear() + '-' + ('' + (date.getMonth() + 1)).padStart(2, '0') + '-' + ('' + date.getDate()).padStart(2, '0');
	},
	displayVersion: function (obj) {
		return (obj.version ? ' (v' + obj.version + ')' : '');
	},
	displayName: function (obj) {
		return fitStringToWidth(obj.name, 160);
	},
	clearLocalStorageOnServer: function (userId) {

		Command.setProperty(userId, 'localStorage', null, false, function () {
			blinkGreen($('#clear-local-storage-on-server'));
			LSWrapper.clear();
		});
	},
	appendDatabaseSelectionBox: function () {

		Structr.fetchHtmlTemplate('dashboard/database.connections', {}, function (html) {

			var parent = $('#dash-connections');

			parent.append(html);

			_Dashboard.loadDatabaseSelectionBox();
		});
	},
	loadDatabaseSelectionBox: function () {

		$.post(
			rootUrl + '/maintenance/manageDatabases',
			JSON.stringify({command: "list"}),
			function (data) {

				var body = $('#database-connection-table-body');
				body.empty();

				data.result.forEach(function (result) {

					Structr.fetchHtmlTemplate('dashboard/connection.row', _Dashboard.mapConnectionResult(result), function (html) {

						body.append(html);

						$('button#connect-button_' + result.name).on('click', function (btn) {

							Structr.showLoadingMessage(
								'Changing database connection to ' + result.name,
								'Please wait until the change has been applied. If you don\'t have a valid session ID in the other database, you will need to re-login after the change.',
								200
								);

							$.ajax({
								url: rootUrl + '/maintenance/manageDatabases',
								type: 'post',
								data: JSON.stringify({
									command: 'activate',
									name: result.name
								}),
								statusCode: {
									200: function(response) {

										Structr.hideLoadingMessage();
										_Dashboard.onload();
									},
									503: function(response) {

										var message = new MessageBuilder().title("Service Unavailable").error(response.responseJSON.message);

										message.delayDuration(5000).fadeDuration(1000);
										message.show();

										Structr.hideLoadingMessage();
										_Dashboard.onload();
									}
								}
							});
						});

						$('button#delete-button_' + result.name).on('click', function (btn) {

							$.post(
								rootUrl + '/maintenance/manageDatabases',
								JSON.stringify({
									command: 'remove',
									name: result.name
								}),
								function () {
									Structr.hideLoadingMessage();
									_Dashboard.onload();
								}
							);
						});
					});
				});

				Structr.fetchHtmlTemplate('dashboard/new-connection.row', {}, function (html) {

					body.append(html);

					$('button#new-database-connection-button').on('click', function (btn) {

						$.post(
							rootUrl + '/maintenance/manageDatabases',
							JSON.stringify({
								command: 'add',
								driver: 'org.structr.bolt.BoltDatabaseService',
								mode: 'remote',
								name: $('#connection-name').val(),
								url: $('#connection-url').val(),
								username: $('#connection-username').val(),
								password: $('#connection-password').val()
							}),
							function () {
								Structr.hideLoadingMessage();
								_Dashboard.onload();
							}
						);
					});
				});
			}
		);
	},
	checkLicenseEnd: function (envInfo, element, cfg) {

		if (envInfo && envInfo.endDate && element) {

			var showMessage = true;
			var daysLeft = Math.ceil((new Date(envInfo.endDate) - new Date()) / 86400000) + 1;

			var config = {
				element: element,
				appendToElement: element
			};
			config = $.extend(config, cfg);

			if (daysLeft <= 0) {

				config.customToggleIcon = _Icons.exclamation_icon;
				config.text = "Your Structr <b>license has expired</b>. Upon restart the Community edition will be loaded.";

			} else if (daysLeft <= 7) {

				config.customToggleIcon = _Icons.error_icon;
				config.text = "Your Structr <b>license will expire in less than a week</b>. After that the Community edition will be loaded.";

			} else {
				showMessage = false;
			}

			if (showMessage) {

				config.text += " Please get in touch via <b>licensing@structr.com</b> to renew your license.";
				Structr.appendInfoTextToElement(config);
			}

		}
	},
	appendGlobalSchemaMethods: function(container) {

		var maintenanceList = $('<div></div>').appendTo(container);

		$.get(rootUrl + '/SchemaMethod?schemaNode=&sort=name', function (data) {

			data.result.forEach(function (result) {

				var methodRow = $('<div class="global-method" style=""></div>');
				var methodName = $('<span>' + result.name + '</span>');

				methodRow.append('<button id="run-' + result.id + '" class="action button">Run now</button>').append(methodName);
				maintenanceList.append(methodRow);

				var cleanedComment = (result.comment && result.comment.trim() !== '') ? result.comment.replaceAll("\n", "<br>") : '';

				if (cleanedComment.trim() !== '') {
					Structr.appendInfoTextToElement({
						element: methodName,
						text: cleanedComment,
						helpElementCss: {
							"line-height": "initial"
						}
					});
				}

				$('button#run-' + result.id).on('click', function () {

					Structr.dialog('Run global schema method ' + result.name, function () {}, function () {
						$('#run-method').remove();
						$('#clear-log').remove();
					});

					dialogBtn.prepend('<button id="run-method">Run</button>');
					dialogBtn.append('<button id="clear-log">Clear output</button>');

					var paramsOuterBox = $('<div id="params"><h3 class="heading-narrow">Parameters</h3></div>');
					var paramsBox = $('<div></div>');
					paramsOuterBox.append(paramsBox);
					var addParamBtn = $('<i title="Add parameter" class="button ' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" />');
					paramsBox.append(addParamBtn);
					dialog.append(paramsOuterBox);

					if (cleanedComment.trim() !== '') {
						dialog.append('<div id="global-method-comment"><h3 class="heading-narrow">Comment</h3>' + cleanedComment + '</div>');
					}

					Structr.appendInfoTextToElement({
						element: $('#params h3'),
						text: "Parameters can be accessed by using the <code>retrieve()</code> function.",
						css: {marginLeft: "5px"},
						helpElementCss: {fontSize: "12px"}
					});

					addParamBtn.on('click', function () {
						var newParam = $('<div class="param"><input class="param-name" placeholder="Parameter name"> : <input class="param-value" placeholder="Parameter value"></div>');
						var removeParam = $('<i class="button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" alt="Remove parameter" title="Remove parameter"/>');

						newParam.append(removeParam);
						removeParam.on('click', function () {
							newParam.remove();
						});
						paramsBox.append(newParam);
					});

					dialog.append('<h3>Method output</h3>');
					dialog.append('<pre id="log-output"></pre>');

					$('#run-method').on('click', function () {

						$('#log-output').empty();
						$('#log-output').append('Running method..\n');

						var params = {};
						$('#params .param').each(function (index, el) {
							var name = $('.param-name', el).val();
							var val = $('.param-value', el).val();
							if (name) {
								params[name] = val;
							}
						});

						$.ajax({
							url: rootUrl + '/maintenance/globalSchemaMethods/' + result.name,
							data: JSON.stringify(params),
							method: 'POST',
							complete: function (data) {
								$('#log-output').append(data.responseText);
								$('#log-output').append('Done.');
							}
						});
					});

					$('#clear-log').on('click', function () {
						$('#log-output').empty();
					});
				});
			});
		});
	},
	activateLogBox: function () {

		let logBoxContentBox = $('#dash-server-log textarea');

		let scrollEnabled = true;

		let updateLog = function () {
			Command.getServerLogSnapshot(300, (a) => {
				logBoxContentBox.html(a[0].result);
				if (scrollEnabled) {
					logBoxContentBox.scrollTop(logBoxContentBox[0].scrollHeight);
				}
			});
		};

		let registerEventHandlers = function () {
			_Dashboard.logInterval = window.setInterval(() => updateLog(), 1000);

			logBoxContentBox.bind('scroll', (event) => {
				let textarea = event.target;

				let maxScroll = textarea.scrollHeight - 4;
				let currentScroll = (textarea.scrollTop + $(textarea).height());

				if (currentScroll >= maxScroll) {
					scrollEnabled = true;
				} else {
					scrollEnabled = false;
				}
			});
		};

		registerEventHandlers();
		updateLog();

	},
	deploy: function (mode, location) {

		var data = {
			mode: mode
		};

		if (mode === 'import') {
			data['source'] = location;
		} else if (mode === 'export') {
			data['target'] = location;
		}

		$.ajax({
			url: rootUrl + '/maintenance/deploy',
			data: JSON.stringify(data),
			method: 'POST'
		});

	},
	mapConnectionResult: function (result) {

		var activeString = result.active ? '<b>active</b>' : '-';
		var button = '';

		if (!result.active) {

			button += '<button class="action" id="connect-button_' + result.name + '">Connect</button>';
			button += '<button class="" id="delete-button_' + result.name + '">Delete</button>';
		}

		if (result.driver === 'org.structr.memory.MemoryDatabaseService') {

			return {

				name: result.name,
				type: 'in-memory',
				url: '-',
				username: '-',
				active: activeString,
				button: button

			};

		} else {

			return {

				name: result.name,
				type: 'neo4j',
				url: result.url,
				username: result.username,
				active: activeString,
				button: button
			};
		}
	}
};
