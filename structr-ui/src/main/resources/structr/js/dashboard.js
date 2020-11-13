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
	Structr.registerModule(_Dashboard);
});

var _Dashboard = {
	_moduleName: 'dashboard',
	dashboard: undefined,
	logInterval: undefined,
	activeTabPrefixKey: 'activeDashboardTabPrefix' + port,
	logRefreshTimeIntervalKey: 'dashboardLogRefreshTimeInterval' + port,
	logLinesKey: 'dashboardNumberOfLines' + port,
	zipExportPrefixKey: 'zipExportPrefix' + port,
	zipExportAppendTimestampKey: 'zipExportAppendTimestamp' + port,

	init: function() {},
	unload: function() {
		window.clearInterval(_Dashboard.logInterval);
	},
	removeActiveClass: function(nodelist) {
		nodelist.forEach(function(el) {
			el.classList.remove('active');
		});
	},
	activateLastActiveTab: function() {
		let tabId = LSWrapper.getItem(_Dashboard.activeTabPrefixKey);
		if (tabId) {
			let tab = document.querySelector('#dashboard .tabs-menu li a[href="' + tabId + '"]');
			if (tab) {
				tab.click();
			}
		}
	},
	onload: function(retryCount = 0) {

		_Dashboard.init();
		Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('dashboard'));

		let templateConfig = {};
		let releasesIndexUrl = '';
		let snapshotsIndexUrl = '';

		fetch(rootUrl + '/_env').then(function(response) {

			if (response.ok) {
				return response.json();
			} else {
				throw Error("Unable to read env resource data");
			}

		}).then(function(data) {

			templateConfig.envInfo = data.result;

			templateConfig.envInfo.version = (data.result.components['structr'] || data.result.components['structr-ui']).version || '';
			templateConfig.envInfo.build   = (data.result.components['structr'] || data.result.components['structr-ui']).build   || '';
			templateConfig.envInfo.date    = (data.result.components['structr'] || data.result.components['structr-ui']).date    || '';

			releasesIndexUrl  = data.result.availableReleasesUrl;
			snapshotsIndexUrl = data.result.availableSnapshotsUrl;

			if (templateConfig.envInfo.startDate) {
				templateConfig.envInfo.startDate = templateConfig.envInfo.startDate.slice(0, 10);
			}

			if (templateConfig.envInfo.endDate) {
				templateConfig.envInfo.endDate = templateConfig.envInfo.endDate.slice(0, 10);
			}

			templateConfig.databaseDriver = Structr.getDatabaseDriverNameForDatabaseServiceName(templateConfig.envInfo.databaseService);

			return fetch(rootUrl + '/me/ui');

		}).then(function(response) {

			return response.json();

		}).then(function(data) {

			templateConfig.meObj = data.result;

			return fetch('/structr/deploy');

		}).then((result) => {

			templateConfig.deployServletAvailable = (result.status !== 404);

		}).then(function() {

			templateConfig.zipExportPrefix = LSWrapper.getItem(_Dashboard.zipExportPrefixKey);
			templateConfig.zipExportAppendTimestamp = LSWrapper.getItem(_Dashboard.zipExportAppendTimestampKey, true);

			Structr.fetchHtmlTemplate('dashboard/dashboard', templateConfig, function(html) {

				main.append(html);

				if (templateConfig.envInfo.databaseService === 'MemoryDatabaseService') {
					Structr.appendInMemoryInfoToElement($('#dashboard-about-structr .db-driver'));
				}

				_Dashboard.gatherVersionUpdateInfo(templateConfig.envInfo.version, releasesIndexUrl, snapshotsIndexUrl);

				document.querySelectorAll('#dashboard .tabs-menu li a').forEach(function(tabLink) {
					tabLink.addEventListener('click', function(e) {
						e.preventDefault();

						let activeLink = document.querySelector('#dashboard .tabs-menu li.active a');
						if (activeLink) {
							$(activeLink.getAttribute('href')).trigger('hide');
						}

						let targetId = e.target.getAttribute('href');

						_Dashboard.removeActiveClass(document.querySelectorAll('#dashboard .tabs-contents .tab-content'));
						_Dashboard.removeActiveClass(document.querySelectorAll('#dashboard .tabs-menu li'));

						e.target.closest('li').classList.add('active');
						document.querySelector(targetId).classList.add('active');
						LSWrapper.setItem(_Dashboard.activeTabPrefixKey, targetId);

						$(targetId).trigger('show');
					});
				});

				document.querySelector('#clear-local-storage-on-server').addEventListener('click', function() {
					_Dashboard.clearLocalStorageOnServer(templateConfig.meObj.id);
				});

				_Dashboard.checkLicenseEnd(templateConfig.envInfo, $('#dashboard-about-structr .end-date'));

				$('button#do-app-import').on('click', function() {
					_Dashboard.deploy('import', $('#deployment-source-input').val());
				});

				$('button#do-app-export').on('click', function() {
					_Dashboard.deploy('export', $('#app-export-target-input').val());
				});

				$('button#do-app-import-from-url').on('click', function() {
					_Dashboard.deployFromURL($('#redirect-url').val(), $('#deployment-url-input').val());
				});

				$('button#do-data-import').on('click', function() {
					_Dashboard.deployData('import', $('#data-import-source-input').val());
				});

				$('button#do-data-export').on('click', function() {
					_Dashboard.deployData('export', $('#data-export-target-input').val(), $('#data-export-types-input').val());
				});

				$('button#do-app-export-to-zip').on('click', function() {
					_Dashboard.exportAsZip();
				});

				_Dashboard.initializeRuntimeEventLog();

				let typesSelectElem = $('#data-export-types-input');

				Command.list('SchemaNode', true, 1000, 1, 'name', 'asc', 'id,name,isBuiltinType', function(nodes) {

					let builtinTypes = [];
					let customTypes = [];

					for (let n of nodes) {
						if (n.isBuiltinType) {
							builtinTypes.push(n);
						} else {
							customTypes.push(n);
						}
					}

					if (customTypes.length > 0) {

						typesSelectElem.append(customTypes.reduce(function(html, node) {
							return html + '<option>' + node.name + '</option>';
						}, '<optgroup label="Custom Types">') + '</optgroup>');
					}

					typesSelectElem.append(builtinTypes.reduce(function(html, node) {
							return html + '<option>' + node.name + '</option>';
						}, '<optgroup label="Builtin Types">') + '</optgroup>');


					typesSelectElem.chosen({
						search_contains: true,
						width: 'calc(100% - 14px)',
						display_selected_options: false,
						hide_results_on_select: false,
						display_disabled_options: false
					}).chosenSortable();
				});

				_Dashboard.activateLogBox();
				_Dashboard.activateLastActiveTab();
				_Dashboard.appendGlobalSchemaMethods($('#dashboard-global-schema-methods'));

				$(window).off('resize');
				$(window).on('resize', function() {
					Structr.resize();
				});


				let userConfigMenu = LSWrapper.getItem(Structr.keyMenuConfig);
				if (!userConfigMenu) {
					userConfigMenu = {
						main: templateConfig.envInfo.mainMenu,
						sub: []
					};
				}

				let mainMenuConfigContainer = document.querySelector('#main-menu-entries-config');
				let subMenuConfigContainer = document.querySelector('#sub-menu-entries-config');

				for (let menuitem of document.querySelectorAll('#menu li[data-name]')) {

					// account for missing modules because of license
					if (menuitem.style.display !== 'none') {
						let n = document.createElement('div');
						n.classList.add('menu-item');
						n.textContent = menuitem.dataset.name;
						n.dataset.name = menuitem.dataset.name;
						subMenuConfigContainer.appendChild(n);
					}
				}

				for (let mainMenuItem of userConfigMenu.main) {
					mainMenuConfigContainer.appendChild(subMenuConfigContainer.querySelector('div[data-name="' + mainMenuItem + '"]'));
				}

				$('#main-menu-entries-config, #sub-menu-entries-config').sortable({
					connectWith: ".connectedSortable"
				}).disableSelection();

				document.querySelector('#save-menu-config').addEventListener('click', () => {
					let newMenuConfig = {
						main: [].map.call(mainMenuConfigContainer.querySelectorAll('div.menu-item'), (el) => { return el.dataset.name; }),
						sub: [].map.call(subMenuConfigContainer.querySelectorAll('div.menu-item'), (el) => { return el.dataset.name; })
					};

					Structr.updateMainMenu(newMenuConfig);
				});

				Structr.unblockMenu(100);
			});
		}).catch((e) => {
			if (retryCount < 3) {
				setTimeout(() => {
					_Dashboard.onload(++retryCount);
				}, 250);
			} else {
				console.log(e);
			}
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
	displayVersion: function(obj) {
		return (obj.version ? ' (v' + obj.version + ')': '');
	},
	clearLocalStorageOnServer: function(userId) {

		Command.setProperty(userId, 'localStorage', null, false, function() {
			blinkGreen($('#clear-local-storage-on-server'));
			LSWrapper.clear();
		});
	},
	checkNewVersions: function() {

	},
	checkLicenseEnd: function(envInfo, element, cfg) {

		if (envInfo && envInfo.endDate && element) {

			var showMessage = true;
			var daysLeft = Math.ceil((new Date(envInfo.endDate.slice(0, 10)) - new Date()) / 86400000) + 1;

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

		$.get(rootUrl + '/SchemaMethod?schemaNode=&sort=name', function(data) {

			data.result.forEach(function(result) {

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

				$('button#run-' + result.id).on('click', function() {
					_Code.runGlobalSchemaMethod(result);
				});
			});
		});
	},
    activateLogBox: function() {

		let feedbackElement = document.querySelector('#dashboard-server-log-feedback');

		let numberOfLines      = LSWrapper.getItem(_Dashboard.logLinesKey, 300);
		let numberOfLinesInput = document.querySelector('#dashboard-server-log-lines');

		numberOfLinesInput.value = numberOfLines;

		numberOfLinesInput.addEventListener('change', () => {
			numberOfLines = numberOfLinesInput.value;
			LSWrapper.setItem(_Dashboard.logLinesKey, numberOfLines);

			blinkGreen($(numberOfLinesInput));
		});

		let registerRefreshInterval = (timeInMs) => {

			window.clearInterval(_Dashboard.logInterval);

			if (timeInMs > 0) {
				_Dashboard.logInterval = window.setInterval(() => updateLog(), timeInMs);
			}
		};

		let logRefreshTimeInterval    = LSWrapper.getItem(_Dashboard.logRefreshTimeIntervalKey, 1000);
		let refreshTimeIntervalSelect = document.querySelector('#dashboard-server-log-refresh-interval');

		refreshTimeIntervalSelect.value = logRefreshTimeInterval;

		refreshTimeIntervalSelect.addEventListener('change', () => {
			logRefreshTimeInterval = refreshTimeIntervalSelect.value;
			LSWrapper.setItem(_Dashboard.logRefreshTimeIntervalKey, logRefreshTimeInterval);

			registerRefreshInterval(logRefreshTimeInterval);
			blinkGreen($(refreshTimeIntervalSelect));
		});

		let logBoxContentBox = $('#dashboard-server-log textarea');

		let scrollEnabled = true;
		let textAreaHasFocus = false;

		logBoxContentBox.on('focus', () => {
			textAreaHasFocus = true;
			feedbackElement.textContent = 'Text area has focus, refresh disabled until focus lost.';
		});

		logBoxContentBox.on('blur', () => {
			textAreaHasFocus = false;
			feedbackElement.textContent = '';
		});

        let updateLog = function() {

			if (!textAreaHasFocus) {

				feedbackElement.textContent = 'Refreshing server log...';

				Command.getServerLogSnapshot(numberOfLines, (a) => {
					logBoxContentBox.text(a[0].result);
					if (scrollEnabled) {
						logBoxContentBox.scrollTop(logBoxContentBox[0].scrollHeight);
					}

					window.setTimeout(() => {
						feedbackElement.textContent = '';
					}, 250);
				});
			}
		};

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

		new Clipboard('#dashboard-server-log-copy', {
			target: function () {
				return logBoxContentBox[0];
			}
		});

		let container = $('#dashboard-server-log');
		if (container) {

			container.on('show', function() {
				updateLog();
				registerRefreshInterval(logRefreshTimeInterval);
			});

			container.on('hide', function() {
				window.clearInterval(_Dashboard.logInterval);
			});
		}
    },
	deploy: function(mode, location) {

		if (!(location && location.length)) {
			new MessageBuilder().title('Unable to start data ' + mode + '').warning('Please enter a local directory path for data export.').requiresConfirmation().show();
			return;
		}

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
			method: 'POST',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				422: function(data) {
					//new MessageBuilder().title('Unable to start app ' + mode + '').warning(data.responseJSON.message).requiresConfirmation().show();
				}
			}
		});
	},
	exportAsZip: function() {

		let prefix = document.getElementById('zip-export-prefix').value;

		let cleaned = prefix.replaceAll(/[^a-zA-Z0-9 _-]/g, '').trim();
		if (cleaned !== prefix) {
			new MessageBuilder().title('Cleaned prefix').info('The given filename prefix was changed to "' + cleaned + '".').requiresConfirmation().show();
			prefix = cleaned;
		}
		LSWrapper.setItem(_Dashboard.zipExportPrefixKey, prefix);

		let appendTimestamp = document.getElementById('zip-export-append-timestamp').checked;
		LSWrapper.setItem(_Dashboard.zipExportAppendTimestampKey, appendTimestamp);

		if (appendTimestamp) {

			let zeroPad = (v) => {
				return ((v < 10) ? '0' : '') + v;
			};

			let date = new Date();

			prefix += '_' + date.getFullYear() + zeroPad(date.getMonth()+1) + zeroPad(date.getDate()) + '_' + zeroPad(date.getHours()) + zeroPad(date.getMinutes()) + zeroPad(date.getSeconds());
		}

		window.location = '/structr/deploy?name=' + prefix;
	},
	deployFromURL: function(redirectUrl, downloadUrl) {

		if (!(downloadUrl && downloadUrl.length)) {
			new MessageBuilder().title('Unable to start app import from URL').warning('Please enter the URL of the ZIP file containing the app data.').requiresConfirmation().show();
			return;
		}

		let data = new FormData();
		data.append('redirectUrl', redirectUrl);
		data.append('downloadUrl', downloadUrl);

		$.ajax({
			url: '/structr/deploy',
			method: 'POST',
			processData: false,
			contentType: false,
			data: data,
			statusCode: {
				400: function(data) {
					new MessageBuilder().title('Unable to import app from URL').warning(data.responseText).requiresConfirmation().show();
				}
			}
		});
	},
	deployData: function(mode, location, types) {

		if (!(location && location.length)) {
			new MessageBuilder().title('Unable to ' + mode + ' data').warning('Please enter a directory path for data ' + mode + '.').requiresConfirmation().show();
			return;
		}

		var data = {
			mode: mode
		};

		if (mode === 'import') {
			data['source'] = location;
		} else if (mode === 'export') {
			data['target'] = location;
			if (types && types.length) {
				data['types'] = types.join(',');
			} else {
				new MessageBuilder().title('Unable to ' + mode + ' data').warning('Please select at least one data type.').requiresConfirmation().show();
				return;
			}

		}

		let url = rootUrl + '/maintenance/deployData';

		$.ajax({
			url: url,
			data: JSON.stringify(data),
			method: 'POST',
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				422: function(data) {
					new MessageBuilder().warning(data.responseJSON.message).requiresConfirmation().show();
				}
			}
		});

	},
	initializeRuntimeEventLog: function() {

		let container = $('#dashboard-event-log');
		if (container) {

			container.on('show', function() {
				_Dashboard.loadRuntimeEventLog();
			});

			$('#refresh-event-log').off('click').on('click', _Dashboard.loadRuntimeEventLog);
			$('#event-type-filter').off('change').on('change', _Dashboard.loadRuntimeEventLog);
		}
	},

	loadRuntimeEventLog: function() {

		let row    = document.querySelector('#event-log-container');
		let num    = document.querySelector('#event-type-page-size');
		let filter = document.querySelector('#event-type-filter');
		let url    = rootUrl + '/_runtimeEventLog?order=absoluteTimestamp&sort=desc&pageSize=' + num.value;
		let type   = filter.value;

		row.innerHTML = '';

		if ( type &&type.length) {
			url += '&type=' + type;
		}

		fetch(url)
			.then(response => response.json())
			.then(result => {

				for (let event of result.result) {

					let timestamp = new Date(event.absoluteTimestamp).toISOString();
					let tr        = document.createElement('tr');
					let data      = event.data;

					_Dashboard.elementWithContent(tr, 'td', timestamp);
					_Dashboard.elementWithContent(tr, 'td', event.type);
					_Dashboard.elementWithContent(tr, 'td', event.description);

					if (data) {

						switch (event.type) {

							case 'Authentication':
								_Dashboard.elementWithContent(tr, 'td', '<code style="white-space: pre; text-decoration: underline; text-underline-position: under;">' + JSON.stringify(data) + '</code>');
								break;

							case 'Javascript':
								_Dashboard.elementWithContent(tr, 'td', data.message);
								break;

							default:
								_Dashboard.elementWithContent(tr, 'td', JSON.stringify(data));
								break;
						}

					} else {

						_Dashboard.elementWithContent(tr, 'td', '');
					}

					let buttonContainer = _Dashboard.elementWithContent(tr, 'td', '');
					if (data.id && data.type) {

						let button = _Dashboard.elementWithContent(buttonContainer, 'button', 'Open content in editor');
						button.addEventListener('click', function() {

							Command.get(data.id, null, function (obj) {
								_Elements.openEditContentDialog(button, obj, {
									extraKeys: { "Ctrl-Space": "autocomplete" },
									gutters: ["CodeMirror-lint-markers"],
									lint: {
										getAnnotations: function(text, callback) {
											_Code.showScriptErrors(obj, text, callback);
										},
										async: true
									}
								});
							});
						});
					}

					row.appendChild(tr);
				}
			}
		);
	},
	elementWithContent: function(parent, tag, content) {

		let element = document.createElement(tag);
		element.innerHTML = content;

		parent.appendChild(element);

		return element;
	}
};

/*
 */