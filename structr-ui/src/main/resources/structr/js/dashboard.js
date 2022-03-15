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

			let envResponse = await fetch(Structr.rootUrl + '_env');

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

			let meResponse       = await fetch(Structr.rootUrl + 'me/ui');
			let meData           = await meResponse.json();

			if (Array.isArray(meData.result)) {
				meData.result = meData.result[0];
			}
			templateConfig.meObj = meData.result;
			let deployResponse   = await fetch(Structr.deployRoot + '?mode=test');

			templateConfig.deployServletAvailable       = (deployResponse.status == 200);
			templateConfig.zipExportPrefix              = LSWrapper.getItem(_Dashboard.deployment.zipExportPrefixKey);
			templateConfig.zipExportAppendTimestamp     = LSWrapper.getItem(_Dashboard.deployment.zipExportAppendTimestampKey, true);
			templateConfig.zipDataExportAppendTimestamp = LSWrapper.getItem(_Dashboard.deployment.zipDataExportAppendTimestamp, true);
			templateConfig.dataImportRebuildIndexes     = LSWrapper.getItem(_Dashboard.deployment.dataImportRebuildIndexes, true);
			templateConfig.zipDataImportRebuildIndexes  = LSWrapper.getItem(_Dashboard.deployment.zipDataImportRebuildIndexes, true);

			let mainHtml        = _Dashboard.templates.main(templateConfig);
			let functionBarHtml = _Dashboard.templates.functions();

			main[0].innerHTML = mainHtml;
			Structr.functionBar.innerHTML = functionBarHtml;

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

		$.get(Structr.rootUrl + 'SchemaMethod?schemaNode=&' + Structr.getRequestParameterName('sort') + '=name', function(data) {

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
					_Dashboard.deployment.deployFromZIPFileUpload(deploymentFileInput);
				} else {

					let downloadUrl = deploymentUrlInput.value;

					if (!(downloadUrl && downloadUrl.length)) {
						new MessageBuilder().title('Unable to start application import from URL').warning('Please enter a URL or upload a ZIP file containing the application export.').requiresConfirmation().allowConfirmAll().show();
					} else {
						_Dashboard.deployment.deployFromZIPURL(downloadUrl);
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

			await fetch(Structr.rootUrl + 'maintenance/deploy', {
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
				window.location = Structr.deployRoot + '?name=' + prefix;
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
				window.location = Structr.deployRoot + '?mode=data&name=' + prefix + '&types=' + types;
			}
		},
		deployFromZIPURL: async (downloadUrl) => {

			let formData = new FormData();
			formData.append('redirectUrl', window.location.pathname);
			formData.append('downloadUrl', downloadUrl);
			formData.append('mode', 'app');

			let response = await fetch(Structr.deployRoot, {
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

			let response = await fetch(Structr.deployRoot, {
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

			let response = await fetch(Structr.deployRoot, {
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

			let response = await fetch(Structr.deployRoot, {
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

			await fetch(Structr.rootUrl + 'maintenance/deployData', {
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
			let url    = Structr.rootUrl + '_runtimeEventLog?' + Structr.getRequestParameterName('order') + '=absoluteTimestamp&' + Structr.getRequestParameterName('sort') + '=desc&' + Structr.getRequestParameterName('pageSize') + '=' + num.value;
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
	},

	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/dashboard.css">
			
			<div class="main-app-box" id="dashboard">
			
				<div class="tabs-contents">
			
					<div class="tab-content active" id="dashboard-about-me">
						<table class="props">
							<tr><td class="key">User Name</td><td>${config.meObj.name}</td></tr>
							<tr><td class="key">ID</td><td>${config.meObj.id}</td></tr>
							<tr><td class="key">E-Mail</td><td>${config.meObj.eMail || ''}</td></tr>
							<tr><td class="key">Working Directory</td><td>${config.meObj.workingDirectory ? config.meObj.workingDirectory.name : ''}</td></tr>
							<tr><td class="key">Session ID(s)</td><td>${config.meObj.sessionIds.join('<br>')}</td></tr>
							<tr><td class="key">Groups</td><td>${config.meObj.groups.map(function(g) { return g.name; }).join(', ')}</td></tr>
						</table>
			
					</div>
			
					<div class="tab-content" id="dashboard-about-structr">
			
						<table class="props">
							<tr><td class="key">Version</td><td>${config.envInfo.version} ${config.envInfo.build} ${config.envInfo.date} <span id="version-update-info"></span></td></tr>
							<tr><td class="key">Edition</td><td><i title="Structr ${config.envInfo.edition} Edition" class="${_Icons.getFullSpriteClass(_Icons.getIconForEdition(config.envInfo.edition))}"></i> (Structr ${config.envInfo.edition} Edition)</td></tr>
							<tr><td class="key">Modules</td><td>${Object.keys(config.envInfo.modules).join(', ')}</td></tr>
							<tr><td class="key">Licensee</td><td>${config.envInfo.licensee || 'Unlicensed'}</td></tr>
							<tr><td class="key">Host ID</td><td>${config.envInfo.hostId || ''}</td></tr>
							<tr><td class="key">License Start Date</td><td>${config.envInfo.startDate || '-'}</td></tr>
							<tr><td class="key">License End Date</td><td class="end-date">${config.envInfo.endDate || '-'}</td></tr>
							<tr><td class="key">Database Driver</td><td class="db-driver">${config.databaseDriver}</td></tr>
						</table>
					</div>
			
					<div class="tab-content" id="dashboard-deployment">
			
						<div class="dashboard-grid-wrapper">
			
							<div class="dashboard-grid-row">
								<h2>Application Import and Export</h2>
							</div>
			
							<div>
								<h3>Application import from server directory</h3>
								<div>
									<input type="text" id="deployment-source-input" placeholder="Server directory path for app import">
									<button class="action" id="do-app-import">Import app from server directory</button>
								</div>
							</div>
			
							<div>
								<h3>Application export to server directory</h3>
								<div>
									<input type="text" id="app-export-target-input" placeholder="Server directory path for app export">
									<button class="action" id="do-app-export">Export app to server directory</button>
								</div>
							</div>
			
							<div>
								<h3>Import application from URL or upload a ZIP file</h3>
								${(config.deployServletAvailable ? '' : '<span class="deployment-warning">Deployment via URL is not possible because <code>DeploymentServlet</code> is not running.</span>')}
								<div>
									<input type="text" id="deployment-url-input" placeholder="Download URL of ZIP file for app import" name="downloadUrl" ${(config.deployServletAvailable ? '' : 'disabled')}>
									<input type="file" id="deployment-file-input" placeholder="Upload ZIP file" ${(config.deployServletAvailable ? '' : 'disabled')}>
									<button class="action ${(config.deployServletAvailable ? '' : 'disabled')}" ${(config.deployServletAvailable ? '' : 'disabled')} id="do-app-import-from-zip">Import app from ZIP file</button>
								</div>
							</div>
			
							<div>
								<h3>Export application and download as ZIP file</h3>
								${(config.deployServletAvailable ? '' : '<span class="deployment-warning">Export and download as ZIP file is not possible because <code>DeploymentServlet</code> is not running.</span>')}
								<div>
									<input type="text" id="zip-export-prefix" placeholder="ZIP File prefix" ${(config.deployServletAvailable ? '' : 'disabled')} value="${(config.zipExportPrefix || 'webapp')}">
									<label class="checkbox-label"><input type="checkbox" id="zip-export-append-timestamp" ${(config.deployServletAvailable ? '' : 'disabled')} ${(config.zipExportAppendTimestamp ? 'checked' : '')}> Append timestamp</label>
									<button class="action ${(config.deployServletAvailable ? '' : 'disabled')}" ${(config.deployServletAvailable ? '' : 'disabled')} id="do-app-export-to-zip">Export and download app as ZIP file</button>
								</div>
							</div>
			
							<div class="dashboard-grid-row">
								<hr>
							</div>
			
							<div class="dashboard-grid-row">
								<h2>Data Import and Export</h2>
							</div>
			
							<div>
								<h3>Data import from server directory</h3>
								<div>
									<input type="text" id="data-import-source-input" placeholder="Local directory path for data import">
									<label class="checkbox-label"><input type="checkbox" id="data-import-rebuild-indexes" ${(config.dataImportRebuildIndexes ? 'checked' : '')}> Rebuild all indexes after data import</label>
									<button class="action" id="do-data-import">Import data from server directory</button>
								</div>
							</div>
			
							<div>
								<h3>Data export to server directory</h3>
								<div>
									<input type="text" id="data-export-target-input" placeholder="Server directory path for data export">
									<select id="data-export-types-input" class="chosen-sortable" data-placeholder="Please select data type(s) to export" multiple="multiple">
										<optgroup label="Custom Types" class="custom-types"></optgroup>
										<optgroup label="Builtin Types" class="builtin-types"></optgroup>
									</select>
									<button class="action" id="do-data-export">Export data to server directory</button>
								</div>
							</div>
			
							<div>
								<h3>Import data from URL or upload a ZIP file</h3>
								${(config.deployServletAvailable ? '' : '<span class="deployment-warning">Deployment via URL is not possible because <code>DeployServlet</code> is not running.</span>')}
								<div>
									<input type="text" id="data-deployment-url-input" placeholder="Download URL of ZIP file for data import" name="downloadUrl" ${(config.deployServletAvailable ? '' : 'disabled')}>
									<input type="file" id="data-deployment-file-input" placeholder="Upload ZIP file" ${(config.deployServletAvailable ? '' : 'disabled')}>
									<label class="checkbox-label"><input type="checkbox" id="zip-data-import-rebuild-indexes" ${(config.zipDataImportRebuildIndexes ? 'checked' : '')}> Rebuild all indexes after data import</label>
									<button id="do-data-import-from-zip" class="action ${(config.deployServletAvailable ? '' : 'disabled')}" ${(config.deployServletAvailable ? '' : 'disabled')}>Import data from ZIP file</button>
								</div>
							</div>
			
							<div>
								<h3>Export data and download as ZIP file</h3>
								${(config.deployServletAvailable ? '' : '<span class="deployment-warning">Export and download data as ZIP file is not possible because <code>DeployServlet</code> is not running.</span>')}
								<div>
									<input type="text" id="zip-data-export-prefix" placeholder="ZIP file prefix" ${(config.deployServletAvailable ? '' : 'disabled')} value="${(config.zipDataExportPrefix || 'data')}">
									<select id="zip-data-export-types-input" class="chosen-sortable" data-placeholder="Please select data type(s) to export" multiple="multiple">
										<optgroup label="Custom Types" class="custom-types"></optgroup>
										<optgroup label="Builtin Types" class="builtin-types"></optgroup>
									</select>
									<label class="checkbox-label"><input type="checkbox" id="zip-data-export-append-timestamp" ${(config.deployServletAvailable ? '' : 'disabled')} ${(config.zipDataExportAppendTimestamp ? 'checked' : '')}> Append timestamp</label>
									<button id="do-data-export-to-zip" class="action ${(config.deployServletAvailable ? '' : 'disabled')}" ${(config.deployServletAvailable ? '' : 'disabled')}>Export and download data as ZIP file</button>
								</div>
							</div>
						</div>
					</div>
			
					<div class="tab-content" id="dashboard-global-schema-methods">
			
					</div>
			
					<div class="tab-content" id="dashboard-server-log">
			
						<div id="dashboard-server-log-controls">
			
							<label>Refresh Interval:</label>
							<select id="dashboard-server-log-refresh-interval">
								<option value="10000">10s</option>
								<option value="5000">5s</option>
								<option value="2000">2s</option>
								<option value="1000">1s</option>
								<option value="-1">manual</option>
							</select>
			
							<span class="dashboard-spacer"></span>
			
							<label>Number of lines: </label>
							<input id="dashboard-server-log-lines" type="number">
			
							<span class="dashboard-spacer"></span>
			
							<button id="dashboard-server-log-manual-refresh" class="action">Refresh</button>
							<button id="dashboard-server-log-copy" class="action">Copy</button>
			
							<span class="dashboard-spacer"></span>
			
							<span id="dashboard-server-log-feedback"></span>
						</div>
			
						<textarea readonly="readonly"></textarea>
					</div>
			
					<div class="tab-content" id="dashboard-sysinfo">
			
					</div>
			
					<div class="tab-content" id="dashboard-ui-config">
			
						<div class="flex">
			
							<div class="flex flex-col mr-12">
			
								<div class="flex menu-order-container">
									<div class="text-center font-bold w-40 p-4">Main Menu</div>
									<div class="text-center font-bold w-40 p-4">Custom Menu (☰)</div>
								</div>
			
								<div class="flex menu-order-container">
									<div id="main-menu-entries-config" class="connectedSortable menu-order-config-container"></div>
									<div id="sub-menu-entries-config" class="connectedSortable menu-order-config-container"></div>
								</div>
							</div>
			
							<div class="flex flex-col mr-12">
			
								<div class="text-center font-bold p-4 w-full">Misc UI Settings</div>
			
								<div id="settings-container">
			
								</div>
			
								<div>
									<button class="action" id="clear-local-storage-on-server">Reset <strong>all</strong> stored UI settings</button>
								</div>
							</div>
						</div>
					</div>
			
					<div class="tab-content" id="dashboard-event-log">
			
						<div id="event-log-options" class="flex items-center mb-4">
			
							<label class="mr-1">Filter:</label>
							<select id="event-type-filter" class="mr-8">
								<option value="">All events</option>
								<option value="Authentication">Authentication events</option>
								<option value="Cron">Cron events</option>
								<!--<option value="GraphQL">GraphQL requests</option>-->
								<option value="Http">Http requests</option>
								<option value="Maintenance">Maintenance</option>
								<option value="Scripting">Scripting events</option>
								<option value="Rest">REST requests</option>
								<option value="ResourceAccess">ResourceAccess events</option>
								<option value="Transaction">Transactions</option>
								<option value="SystemInfo">SystemInfo</option>
							</select>
			
							<label class="mr-1">Number of events to show:</label>
							<input id="event-type-page-size" class="mr-8" type="number" size="3" value="100">
			
							<button id="refresh-event-log" class="inline-flex items-center">${_Icons.getSvgIcon('refresh-arrows', 16, 16, 'mr-2')} Refresh</button>
						</div>
			
						<table class="props">
							<thead>
								<tr>
									<th class="text-left">Timestamp</th>
									<th class="text-left">Type</th>
									<th class="text-left">Detail</th>
									<th class="text-left">Data</th>
									<th class="text-left">Actions</th>
								</tr>
							</thead>
							<tbody id="event-log-container">
			
							</tbody>
						</table>
					</div>
			
				</div>
			
			</div>
		`,
		functions: config => `
			<ul class="tabs-menu flex-grow">
				<li>
					<a href="#dashboard:about-me">About Me</a>
				</li>
				<li>
					<a href="#dashboard:about-structr">About Structr</a>
				</li>
				<li>
					<a href="#dashboard:deployment">Deployment</a>
				</li>
				<li>
					<a href="#dashboard:global-schema-methods">Global schema methods</a>
				</li>
				<li>
					<a href="#dashboard:server-log">Server Log</a>
				</li>
				<li>
					<a href="#dashboard:event-log">Event Log</a>
				</li>
				<li>
					<a href="#dashboard:ui-config">UI Settings</a>
				</li>
			</ul>
		`
	}
};