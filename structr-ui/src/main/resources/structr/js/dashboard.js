/*
 * Copyright (C) 2010-2022 Structr GmbH
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

let _Dashboard = {
	_moduleName: 'dashboard',
	dashboard: undefined,
	activeTabPrefixKey: 'activeDashboardTabPrefix' + location.port,

	showScriptingErrorPopupsKey:         'showScriptinErrorPopups' + location.port,
	showVisibilityFlagsInGrantsTableKey: 'showVisibilityFlagsInResourceAccessGrantsTable' + location.port,
	favorEditorForContentElementsKey:    'favorEditorForContentElements' + location.port,
	favorHTMLForDOMNodesKey:             'favorHTMLForDOMNodes' + location.port,

	init: function() {

	},
	unload: function() {
		window.clearInterval(_Dashboard.serverlog.interval);
	},
	removeActiveClass: function(nodelist) {
		nodelist.forEach(function(el) {
			el.classList.remove('active');
		});
	},
	onload: async function(retryCount = 0) {

		try {

			_Dashboard.init();

			Structr.updateMainHelpLink(Structr.getDocumentationURLForTopic('dashboard'));

			let templateConfig = {};
			let releasesIndexUrl = '';
			let snapshotsIndexUrl = '';

			let envResponse = await fetch(rootUrl + '_env');

			if (!envResponse.ok) {
				throw Error("Unable to read env resource data");
			}

			let envData = await envResponse.json();

			templateConfig.envInfo = envData.result;
			if (Array.isArray(templateConfig.envInfo)) {
				templateConfig.envInfo = templateConfig.envInfo[0];
			}

			templateConfig.envInfo.version = (templateConfig.envInfo.components['structr'] || templateConfig.envInfo.components['structr-ui']).version || '';
			templateConfig.envInfo.build   = (templateConfig.envInfo.components['structr'] || templateConfig.envInfo.components['structr-ui']).build   || '';
			templateConfig.envInfo.date    = (templateConfig.envInfo.components['structr'] || templateConfig.envInfo.components['structr-ui']).date    || '';

			releasesIndexUrl  = templateConfig.envInfo.availableReleasesUrl;
			snapshotsIndexUrl = templateConfig.envInfo.availableSnapshotsUrl;

			if (templateConfig.envInfo.startDate) {
				templateConfig.envInfo.startDate = templateConfig.envInfo.startDate.slice(0, 10);
			}

			if (templateConfig.envInfo.endDate) {
				templateConfig.envInfo.endDate = templateConfig.envInfo.endDate.slice(0, 10);
			}

			templateConfig.databaseDriver = Structr.getDatabaseDriverNameForDatabaseServiceName(templateConfig.envInfo.databaseService);

			let meResponse       = await fetch(rootUrl + 'me/ui');
			let meData           = await meResponse.json();

			if (Array.isArray(meData.result)) {
				meData.result = meData.result[0];
			}
			templateConfig.meObj = meData.result;
			let deployResponse   = await fetch('/structr/deploy?mode=test');

			templateConfig.deployServletAvailable       = (deployResponse.status == 200);
			templateConfig.zipExportPrefix              = LSWrapper.getItem(_Dashboard.deployment.zipExportPrefixKey);
			templateConfig.zipExportAppendTimestamp     = LSWrapper.getItem(_Dashboard.deployment.zipExportAppendTimestampKey, true);
			templateConfig.zipDataExportAppendTimestamp = LSWrapper.getItem(_Dashboard.deployment.zipDataExportAppendTimestamp, true);
			templateConfig.dataImportRebuildIndexes     = LSWrapper.getItem(_Dashboard.deployment.dataImportRebuildIndexes, true);
			templateConfig.zipDataImportRebuildIndexes  = LSWrapper.getItem(_Dashboard.deployment.zipDataImportRebuildIndexes, true);

			Structr.fetchHtmlTemplate('dashboard/dashboard', templateConfig, function(html) {

				main[0].innerHTML = html;

				Structr.fetchHtmlTemplate('dashboard/functions', templateConfig, function(html) {

					Structr.functionBar.innerHTML = html;

					UISettings.showSettingsForCurrentModule();

					if (templateConfig.envInfo.databaseService === 'MemoryDatabaseService') {
						Structr.appendInMemoryInfoToElement($('#dashboard-about-structr .db-driver'));
					}

					_Dashboard.eventlog.initializeRuntimeEventLog();
					_Dashboard.serverlog.init();

					_Dashboard.gatherVersionUpdateInfo(templateConfig.envInfo.version, releasesIndexUrl, snapshotsIndexUrl);

					document.querySelectorAll('#function-bar .tabs-menu li a').forEach(function(tabLink) {

						tabLink.addEventListener('click', function(e) {
							e.preventDefault();

							let urlHash   = e.target.closest('a').getAttribute('href');
							let subModule = urlHash.split(':')[1];
							let targetId  = '#dashboard-' + subModule;
							window.location.hash = urlHash;

							_Dashboard.removeActiveClass(document.querySelectorAll('#dashboard .tabs-contents .tab-content'));
							_Dashboard.removeActiveClass(document.querySelectorAll('#function-bar .tabs-menu li'));

							e.target.closest('li').classList.add('active');
							document.querySelector(targetId).classList.add('active');
							LSWrapper.setItem(_Dashboard.activeTabPrefixKey, subModule);

							$(targetId).trigger('show');
						});
					});

					// activate tab - either defined by URL or by last accessed
					let tabSelected = false;
					if (location.hash.split(':').length > 1) {
						let locationHashLink = Structr.functionBar.querySelector('[href="' + location.hash + '"]');
						if (locationHashLink) {
							tabSelected = true;
							locationHashLink.click();
						}
					}

					if (!tabSelected) {
						let storedSubModule = LSWrapper.getItem(_Dashboard.activeTabPrefixKey);
						let activeTabLink   = document.querySelector('[href="#' + _Dashboard._moduleName + ':' + storedSubModule + '"]');
						if (activeTabLink) {
							tabSelected = true;
							activeTabLink.click();
						}
					}

					if (!tabSelected) {
						let firstTab = Structr.functionBar.querySelector('a');
						if (firstTab) {
							firstTab.click();
						}
					}

					_Dashboard.checkLicenseEnd(templateConfig.envInfo, $('#dashboard-about-structr .end-date'));

					_Dashboard.deployment.init();

					_Dashboard.appendGlobalSchemaMethods($('#dashboard-global-schema-methods'));

					$(window).off('resize');
					$(window).on('resize', function() {
						Structr.resize();
					});

					_Dashboard.uisettings.showMainMenuConfiguration(templateConfig.envInfo.mainMenu);

					_Dashboard.uisettings.showConfigurableSettings();

					_Dashboard.uisettings.handleResetConfiguration(templateConfig.meObj.id);

					Structr.unblockMenu(100);
				});
			});

		} catch(e) {

			if (retryCount < 3) {
				setTimeout(() => {
					_Dashboard.onload(++retryCount);
				}, 250);
			} else {
				console.log(e);
			}
		}

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
	checkLicenseEnd: function(envInfo, element, cfg) {

		if (envInfo && envInfo.endDate && element) {

			let showMessage = true;
			let daysLeft    = Math.ceil((new Date(envInfo.endDate.slice(0, 10)) - new Date()) / 86400000) + 1;

			let config = {
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

		let maintenanceList = $('<table class="props"></table>').appendTo(container);

		$.get(rootUrl + 'SchemaMethod?schemaNode=&' + Structr.getRequestParameterName('sort') + '=name', function(data) {

			if (data.result.length === 0) {
				maintenanceList.append('No global schema methods.')
			} else {

				for (let method of data.result) {

					let methodRow = $('<tr class="global-method"></tr>');
					let methodName = $('<td><span class="method-name">' + method.name + '</span></td>');

					methodRow.append(methodName).append('<td><button id="run-' + method.id + '" class="action button">Run now</button></td>');
					maintenanceList.append(methodRow);

					$('button#run-' + method.id).on('click', function() {
						_Code.runGlobalSchemaMethod(method);
					});
				}
			}
		});
	},


	deployment: {
		zipExportPrefixKey:              'zipExportPrefix' + location.port,
		zipDataExportPrefixKey:          'zipDataExportPrefix' + location.port,
		zipExportAppendTimestampKey:     'zipExportAppendTimestamp' + location.port,
		zipDataExportAppendTimestampKey: 'zipDataExportAppendTimestamp' + location.port,
		dataImportRebuildIndexes:        'dataImportRebuildIndexes' + location.port,
		zipDataImportRebuildIndexes:     'zipDataImportRebuildIndexes' + location.port,

		init: () => {

			// App Import
			let deploymentFileInput = document.getElementById('deployment-file-input');
			let deploymentUrlInput  = document.getElementById('deployment-url-input');

			deploymentFileInput.addEventListener('input', () => {
				deploymentUrlInput.value = '';
			});

			deploymentUrlInput.addEventListener('input', () => {
				deploymentFileInput.value = '';
			});

			document.getElementById('do-app-import').addEventListener('click', () => {
				_Dashboard.deployment.deploy('import', document.getElementById('deployment-source-input').value);
			});

			document.getElementById('do-app-import-from-zip').addEventListener('click', () => {
				if (deploymentFileInput && deploymentFileInput.files.length > 0) {
					_Dashboard.deployment.deployFromZIPFileUpload(window.location.href, deploymentFileInput);
				} else {

					let downloadUrl = deploymentUrlInput.value;

					if (!(downloadUrl && downloadUrl.length)) {
						new MessageBuilder().title('Unable to start application import from URL').warning('Please enter a URL or upload a ZIP file containing the application export.').requiresConfirmation().allowConfirmAll().show();
					} else {
						_Dashboard.deployment.deployFromZIPURL(window.location.href, downloadUrl);
					}
				}
			});

			// App Export
			document.getElementById('do-app-export').addEventListener('click', () => {
				_Dashboard.deployment.deploy('export', document.getElementById('app-export-target-input').value);
			});

			document.getElementById('do-app-export-to-zip').addEventListener('click', () => {
				_Dashboard.deployment.exportAsZip();
			});

			// Data Import
			let dataDeploymentFileInput = document.getElementById('data-deployment-file-input');
			let dataDeploymentUrlInput  = document.getElementById('data-deployment-url-input');

			dataDeploymentFileInput.addEventListener('input', () => {
				dataDeploymentUrlInput.value = '';
			});

			dataDeploymentUrlInput.addEventListener('input', () => {
				dataDeploymentFileInput.value = '';
			});

			document.getElementById('do-data-import').addEventListener('click', () => {
				_Dashboard.deployment.deployData('import', document.getElementById('data-import-source-input').value);
			});

			document.getElementById('do-data-import-from-zip').addEventListener('click', () => {

				if (dataDeploymentFileInput && dataDeploymentFileInput.files.length > 0) {

					_Dashboard.deployment.deployDataFromZIPFileUpload(window.location.href, dataDeploymentFileInput);

				} else {

					let downloadUrl = dataDeploymentUrlInput.value;
					if (!(downloadUrl && downloadUrl.length)) {

						new MessageBuilder().title('Unable to start data import from URL').warning('Please enter a URL or upload a ZIP file containing the data export.').requiresConfirmation().allowConfirmAll().show();

					} else {

						_Dashboard.deployment.deployDataFromZIPURL(window.location.href, downloadUrl);
					}
				}
			});

			// Data Export
			document.getElementById('do-data-export').addEventListener('click', () => {
				_Dashboard.deployment.deployData('export', $('#data-export-target-input').val(), $('#data-export-types-input').val());
			});

			document.getElementById('do-data-export-to-zip').addEventListener('click', () => {
				_Dashboard.deployment.exportDataAsZip();
			});


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

				for (let typesSelectElemSelector of ['#data-export-types-input', '#zip-data-export-types-input']) {

					let typesSelectElem = $(typesSelectElemSelector);

					$('.custom-types', typesSelectElem).append(customTypes.map((type) => '<option>' + type.name + '</option>').join(''));
					$('.builtin-types', typesSelectElem).append(builtinTypes.map((type) => '<option>' + type.name + '</option>').join(''));

					typesSelectElem.chosen({
						search_contains: true,
						width: 'calc(100% - 2rem)',
						display_selected_options: false,
						hide_results_on_select: false,
						display_disabled_options: false
					}).chosenSortable();
				}
			});
		},
		deploy: async (mode, location) => {

			if (!(location && location.length)) {
				new MessageBuilder().title('Unable to start application ' + mode).warning('Please enter a local directory path for application ' + mode + '.').requiresConfirmation().allowConfirmAll().show();
				return;
			}

			let data = {
				mode: mode
			};

			if (mode === 'import') {
				data['source'] = location;
			} else if (mode === 'export') {
				data['target'] = location;
			}

			await fetch(rootUrl + 'maintenance/deploy', {
				method: 'POST',
				body: JSON.stringify(data)
			});

			// do not listen for errors - they are sent by the backend via WS
		},
		cleanFileNamePrefix: (prefix) => {
			let cleaned = prefix.replaceAll(/[^a-zA-Z0-9 _-]/g, '').trim();
			if (cleaned !== prefix) {
				new MessageBuilder().title('Cleaned prefix').info('The given filename prefix was changed to "' + cleaned + '".').requiresConfirmation().allowConfirmAll().show();
			}
			return cleaned;
		},
		appendTimeStampToPrefix: (prefix) => {

			let zeroPad = (v) => (((v < 10) ? '0' : '') + v);
			let date    = new Date();

			return prefix + '_' + date.getFullYear() + zeroPad(date.getMonth()+1) + zeroPad(date.getDate()) + '_' + zeroPad(date.getHours()) + zeroPad(date.getMinutes()) + zeroPad(date.getSeconds());
		},
		exportAsZip: () => {

			let prefix = _Dashboard.deployment.cleanFileNamePrefix(document.getElementById('zip-export-prefix').value);
			LSWrapper.setItem(_Dashboard.deployment.zipExportPrefixKey, prefix);

			let appendTimestamp = document.getElementById('zip-export-append-timestamp').checked;
			LSWrapper.setItem(_Dashboard.deployment.zipExportAppendTimestampKey, appendTimestamp);

			if (appendTimestamp) {
				prefix = _Dashboard.deployment.appendTimeStampToPrefix(prefix);
			}

			if (prefix === '') {
				new MessageBuilder().title('Unable to export application').warning('Please enter a prefix or select "Append timestamp"').requiresConfirmation().allowConfirmAll().show();
			} else {
				window.location = '/structr/deploy?name=' + prefix;
			}
		},
		exportDataAsZip: () => {

			let prefix          = _Dashboard.deployment.cleanFileNamePrefix(document.getElementById('zip-data-export-prefix').value);
			let appendTimestamp = document.getElementById('zip-data-export-append-timestamp').checked;

			LSWrapper.setItem(_Dashboard.deployment.zipDataExportPrefixKey, prefix);
			LSWrapper.setItem(_Dashboard.deployment.zipDataExportAppendTimestampKey, appendTimestamp);

			if (appendTimestamp) {
				prefix = _Dashboard.deployment.appendTimeStampToPrefix(prefix);
			}

			let zipDataExportTypesSelect = document.getElementById('zip-data-export-types-input');
			let types                    = Array.from(zipDataExportTypesSelect.selectedOptions).map(o => o.value).join(',');

			if (types === '') {
				new MessageBuilder().title('Unable to start data export').warning('Please select at least one data type.').requiresConfirmation().allowConfirmAll().show();
			} else if (prefix === '') {
				new MessageBuilder().title('Unable to start data export').warning('Please enter a prefix or select "Append timestamp"').requiresConfirmation().allowConfirmAll().show();
			} else {
				window.location = '/structr/deploy?mode=data&name=' + prefix + '&types=' + types;
			}
		},
		deployFromZIPURL: async (redirectUrl, downloadUrl) => {

			let formData = new FormData();
			formData.append('redirectUrl', redirectUrl);
			formData.append('downloadUrl', downloadUrl);
			formData.append('mode', 'app');

			let response = await fetch('/structr/deploy', {
				method: 'POST',
				body: formData
			});

			if (!response.ok && response.status === 400) {
				let responseText = await response.text();
				new MessageBuilder().title('Unable to import app from URL').warning(responseText).requiresConfirmation().allowConfirmAll().show();
			}
		},
		deployDataFromZIPURL: async (redirectUrl, downloadUrl) => {

			let formData = new FormData();
			formData.append('redirectUrl', redirectUrl);
			formData.append('downloadUrl', downloadUrl);
			formData.append('mode', 'data');

			let rebuildAllIndexes = document.getElementById('zip-data-import-rebuild-indexes').checked;
			LSWrapper.setItem(_Dashboard.deployment.zipDataImportRebuildIndexes, rebuildAllIndexes);

			formData.append('rebuildAllIndexes', rebuildAllIndexes);

			let response = await fetch('/structr/deploy', {
				method: 'POST',
				body: formData
			});

			if (!response.ok && response.status === 400) {
				let responseText = await response.text();
				new MessageBuilder().title('Unable to import app from ZIP URL').warning(responseText).requiresConfirmation().allowConfirmAll().show();
			}
		},
		deployFromZIPFileUpload: async (redirectUrl, filesSelectField) => {

			let formData = new FormData();
			formData.append('redirectUrl', redirectUrl);
			formData.append('mode', 'app');
			formData.append('file', filesSelectField.files[0]);

			let response = await fetch('/structr/deploy', {
				method: 'POST',
				body: formData
			});

			if (!response.ok && response.status === 400) {
				let responseText = await response.text();
				new MessageBuilder().title('Unable to import app from uploaded ZIP').warning(responseText).requiresConfirmation().allowConfirmAll().show();
			}
		},
		deployDataFromZIPFileUpload: async (redirectUrl, filesSelectField) => {

			let formData = new FormData();
			formData.append('file', filesSelectField.files[0]);
			formData.append('redirectUrl', redirectUrl);
			formData.append('mode', 'data');

			let rebuildAllIndexes = document.getElementById('zip-data-import-rebuild-indexes').checked;
			LSWrapper.setItem(_Dashboard.deployment.zipDataImportRebuildIndexes, rebuildAllIndexes);

			formData.append('rebuildAllIndexes', rebuildAllIndexes);

			let response = await fetch('/structr/deploy', {
				method: 'POST',
				body: formData
			});

			if (!response.ok && response.status === 400) {
				let responseText = await response.text();
				new MessageBuilder().title('Unable to import app from uploaded ZIP').warning(responseText).requiresConfirmation().allowConfirmAll().show();
			}
		},
		deployData: async (mode, location, types) => {

			if (!(location && location.length)) {
				new MessageBuilder().title('Unable to start data ' + mode + '').warning('Please enter a local directory path for data ' + mode + '.').requiresConfirmation().allowConfirmAll().show();
				return;
			}

			let data = {
				mode: mode
			};

			if (mode === 'import') {
				data['source'] = location;

				let rebuildAllIndexes = document.getElementById('data-import-rebuild-indexes').checked;
				LSWrapper.setItem(_Dashboard.deployment.dataImportRebuildIndexes, rebuildAllIndexes);

				data['rebuildAllIndexes'] = rebuildAllIndexes;

			} else if (mode === 'export') {

				data['target'] = location;

				if (types && types.length) {
					data['types'] = types.join(',');
				} else {
					new MessageBuilder().title('Unable to ' + mode + ' data').warning('Please select at least one data type.').requiresConfirmation().allowConfirmAll().show();
					return;
				}
			}

			await fetch(rootUrl + 'maintenance/deployData', {
				method: 'POST',
				body: JSON.stringify(data)
			});

			// do not listen for errors - they are sent by the backend via WS
		},
	},

	serverlog: {
		interval: undefined,
		refreshTimeIntervalKey: 'dashboardLogRefreshTimeInterval' + location.port,
		numberOfLinesKey: 'dashboardNumberOfLines' + location.port,

		init: function() {

			let feedbackElement      = document.querySelector('#dashboard-server-log-feedback');
			let numberOfLines        = LSWrapper.getItem(_Dashboard.serverlog.numberOfLinesKey, 300);
			let numberOfLinesInput   = document.querySelector('#dashboard-server-log-lines');
			numberOfLinesInput.value = numberOfLines;

			numberOfLinesInput.addEventListener('change', () => {
				numberOfLines = numberOfLinesInput.value;
				LSWrapper.setItem(_Dashboard.serverlog.numberOfLinesKey, numberOfLines);

				blinkGreen($(numberOfLinesInput));
			});

			let manualRefreshButton       = document.querySelector('#dashboard-server-log-manual-refresh');
			manualRefreshButton.addEventListener('click', () => updateLog());

			let registerRefreshInterval = (timeInMs) => {

				window.clearInterval(_Dashboard.serverlog.interval);

				if (timeInMs > 0) {
					manualRefreshButton.classList.add('hidden');
					_Dashboard.serverlog.interval = window.setInterval(() => updateLog(), timeInMs);
				} else {
					manualRefreshButton.classList.remove('hidden');
				}
			};

			let logRefreshTimeInterval    = LSWrapper.getItem(_Dashboard.serverlog.refreshTimeIntervalKey, 1000);
			let refreshTimeIntervalSelect = document.querySelector('#dashboard-server-log-refresh-interval');

			refreshTimeIntervalSelect.value = logRefreshTimeInterval;

			refreshTimeIntervalSelect.addEventListener('change', () => {
				logRefreshTimeInterval = refreshTimeIntervalSelect.value;
				LSWrapper.setItem(_Dashboard.serverlog.refreshTimeIntervalKey, logRefreshTimeInterval);

				registerRefreshInterval(logRefreshTimeInterval);
				blinkGreen($(refreshTimeIntervalSelect));
			});

			let logBoxContentBox = $('#dashboard-server-log textarea');

			let scrollEnabled    = true;
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
				let textarea      = event.target;
				let maxScroll     = textarea.scrollHeight - 4;
				let currentScroll = (textarea.scrollTop + $(textarea).height());

				scrollEnabled     = (currentScroll >= maxScroll);
			});

			document.getElementById('dashboard-server-log-copy').addEventListener('click', async () => {
				let text = logBoxContentBox[0].textContent;
				await navigator.clipboard.writeText(text);
			});

			let container = $('#dashboard-server-log');
			if (container) {

				container.on('show', function() {
					updateLog();
					registerRefreshInterval(logRefreshTimeInterval);
				});

				container.on('hide', function() {
					window.clearInterval(_Dashboard.serverlog.interval);
				});
			}
		},

	},

	eventlog: {
		initializeRuntimeEventLog: function() {

			let container = $('#dashboard-event-log');
			if (container) {

				container.on('show', function() {
					_Dashboard.eventlog.loadRuntimeEventLog();
				});

				$('#refresh-event-log').off('click').on('click', _Dashboard.eventlog.loadRuntimeEventLog);
				$('#event-type-filter').off('change').on('change', _Dashboard.eventlog.loadRuntimeEventLog);
			}
		},

		elementWithContent: function(parent, tag, content) {

			let element       = document.createElement(tag);
			element.innerHTML = content;

			parent.appendChild(element);

			return element;
		},

		loadRuntimeEventLog: function() {

			let row    = document.querySelector('#event-log-container');
			let num    = document.querySelector('#event-type-page-size');
			let filter = document.querySelector('#event-type-filter');
			let url    = rootUrl + '_runtimeEventLog?' + Structr.getRequestParameterName('order') + '=absoluteTimestamp&' + Structr.getRequestParameterName('sort') + '=desc&' + Structr.getRequestParameterName('pageSize') + '=' + num.value;
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

						_Dashboard.eventlog.elementWithContent(tr, 'td', timestamp);
						_Dashboard.eventlog.elementWithContent(tr, 'td', event.type);
						_Dashboard.eventlog.elementWithContent(tr, 'td', event.description);

						if (data) {

							switch (event.type) {

								case 'Authentication':
									_Dashboard.eventlog.elementWithContent(tr, 'td', JSON.stringify(data));
									break;

								case 'Scripting':
									_Dashboard.eventlog.elementWithContent(tr, 'td', JSON.stringify(data));
									break;

								default:
									_Dashboard.eventlog.elementWithContent(tr, 'td', JSON.stringify(data));
									break;
							}

						} else {

							_Dashboard.eventlog.elementWithContent(tr, 'td', '');
						}

						let buttonContainer = _Dashboard.eventlog.elementWithContent(tr, 'td', '');
						if (data.id && data.type) {

							if (data.type === 'SchemaMethod' || data.type === 'SchemaProperty') {

								let button = _Dashboard.eventlog.elementWithContent(buttonContainer, 'button', 'Go to code');
								button.addEventListener('click', function() {

									Command.get(data.id, null, function (obj) {

										let pathToOpen = '';

										if (obj.schemaNode) {

											pathToOpen = 'custom--' + obj.schemaNode.id + '-methods-' + obj.id;

										} else {

											pathToOpen = 'globals--' + obj.id;
										}

										window.location.href = '#code';
										window.setTimeout(function() {
											_Code.findAndOpenNode(pathToOpen, false);
										}, 1000);
									});
								});

							} else {

								let button = _Dashboard.eventlog.elementWithContent(buttonContainer, 'button', 'Open content in editor');
								button.addEventListener('click', () => {

									Command.get(data.id, null, function (obj) {
										_Elements.openEditContentDialog(obj);
									});
								});
							}
						}

						row.appendChild(tr);
					}
				}
			);
		},
	},

	uisettings: {

		showMainMenuConfiguration: (defaultMainMenu) => {

			let userConfigMenu = LSWrapper.getItem(Structr.keyMenuConfig);
			if (!userConfigMenu) {
				userConfigMenu = {
					main: defaultMainMenu,
					sub: []
				};
			}

			let mainMenuConfigContainer = document.querySelector('#main-menu-entries-config');
			let subMenuConfigContainer  = document.querySelector('#sub-menu-entries-config');

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
				connectWith: ".connectedSortable",
				stop: _Dashboard.uisettings.updateMenu
			}).disableSelection();

			_Dashboard.uisettings.updateMenu();

		},

		updateMenu: () => {

			let mainMenuConfigContainer = document.querySelector('#main-menu-entries-config');
			let subMenuConfigContainer  = document.querySelector('#sub-menu-entries-config');

			let newMenuConfig = {
				main: [].map.call(mainMenuConfigContainer.querySelectorAll('div.menu-item'), (el) => { return el.dataset.name; }),
				sub: [].map.call(subMenuConfigContainer.querySelectorAll('div.menu-item'), (el) => { return el.dataset.name; })
			};

			Structr.updateMainMenu(newMenuConfig);
		},

		showConfigurableSettings: () => {

			let settingsContainer = document.querySelector('#settings-container');
			let allSettings       = UISettings.getSettings();
			let offCanvasDummy    = Structr.createSingleDOMElementFromHTML('<div></div>');	// prepare off-canvas to reduce number of renders

			for (let section of allSettings) {
				UISettings.appendSettingsSectionToContainer(section, offCanvasDummy);
			}

			settingsContainer.appendChild(offCanvasDummy);
		},

		handleResetConfiguration: (userId) => {

			document.querySelector('#clear-local-storage-on-server').addEventListener('click', function() {

				// save before we remove to possibly force an update. (if no update is done, the callback is not fired)
				LSWrapper.save(() => {

					Command.setProperty(userId, 'localStorage', null, false, function() {
						blinkGreen($('#clear-local-storage-on-server'));
						LSWrapper.clear();
						_Dashboard.onload();
					});
				});
			});
		}
	}
};
