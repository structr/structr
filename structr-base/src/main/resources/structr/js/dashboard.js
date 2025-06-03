/*
 * Copyright (C) 2010-2025 Structr GmbH
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
	Structr.registerModule(_Dashboard);
});

let _Dashboard = {
	_moduleName: 'dashboard',
	activeTabPrefixKey: 'activeDashboardTabPrefix' + location.port,

	init: () => {},
	unload: () => {
		_Dashboard.tabs['server-log'].stop();
	},
	onload: async (retryCount = 0) => {

		try {

			_Dashboard.init();

			Structr.updateMainHelpLink(_Helpers.getDocumentationURLForTopic('dashboard'));

			let dashboardUiConfig = {};
			let envResponse       = await fetch(Structr.rootUrl + '_env');

			if (!envResponse.ok) {
				throw Error("Unable to read env resource data");
			}

			let envData = await envResponse.json();

			dashboardUiConfig.envInfo = envData.result;
			if (Array.isArray(dashboardUiConfig.envInfo)) {
				dashboardUiConfig.envInfo = dashboardUiConfig.envInfo[0];
			}

			dashboardUiConfig.envInfo.version = (dashboardUiConfig.envInfo.components['structr'] || dashboardUiConfig.envInfo.components['structr-base']).version || '';
			dashboardUiConfig.envInfo.build   = (dashboardUiConfig.envInfo.components['structr'] || dashboardUiConfig.envInfo.components['structr-base']).build   || '';
			dashboardUiConfig.envInfo.date    = (dashboardUiConfig.envInfo.components['structr'] || dashboardUiConfig.envInfo.components['structr-base']).date    || '';

			if (dashboardUiConfig.envInfo.startDate) {
				dashboardUiConfig.envInfo.startDate = dashboardUiConfig.envInfo.startDate.slice(0, 10);
			}

			if (dashboardUiConfig.envInfo.endDate) {
				dashboardUiConfig.envInfo.endDate = dashboardUiConfig.envInfo.endDate.slice(0, 10);
			}

			dashboardUiConfig.databaseDriver = Structr.getDatabaseDriverNameForDatabaseServiceName(dashboardUiConfig.envInfo.databaseService);

			let meResponse       = await fetch(Structr.rootUrl + 'me/ui');
			let meData           = await meResponse.json();

			if (Array.isArray(meData.result)) {
				meData.result = meData.result[0];
			}
			dashboardUiConfig.meObj = meData.result;
			let deployResponse   = await fetch(Structr.deployRoot + '?mode=test');

			dashboardUiConfig.deploymentServletAvailable   = (deployResponse.status == 200);

			Structr.setMainContainerHTML(_Dashboard.templates.main(dashboardUiConfig));
			Structr.setFunctionBarHTML(_Dashboard.templates.functions());

			UISettings.showSettingsForCurrentModule();

			_Helpers.activateCommentsInElement(Structr.mainContainer);

			// Update GraalVM Scripting Debugger Info
			let graalVMScriptingDebuggerCell = document.querySelector('#graal-vm-chrome-scripting-debugger');
			if (dashboardUiConfig.envInfo.debuggerEnabled === true) {

				graalVMScriptingDebuggerCell.insertAdjacentHTML('beforeend', _Dashboard.templates.graalVMChromeScriptingDebugger({ path: dashboardUiConfig.envInfo.debuggerPath }));

			} else {

				let inactive = _Helpers.createSingleDOMElementFromHTML('<div>inactive</div>');
				graalVMScriptingDebuggerCell.appendChild(inactive);

				_Helpers.appendInfoTextToElement({
					element: inactive,
					text: 'The GraalVM Chrome Scripting Debugger can be used after enabling the setting <b>application.scripting.debugger</b> in structr.conf'
				});
			}

			// Display security warnings if there are any
			let securityWarningsCell = document.querySelector('#security-warnings');
			if (dashboardUiConfig.envInfo.dashboardInfo?.configFileInfo?.permissionsOk === false) {

				let warningEl = _Helpers.createSingleDOMElementFromHTML(_Dashboard.templates.tabContentAboutStructrSecurity( dashboardUiConfig.envInfo.dashboardInfo.configFileInfo ));

				securityWarningsCell.appendChild(warningEl);

			} else {

				securityWarningsCell.textContent = 'No warnings';
			}

			// display HTTP stats
			let httpStatisticsCell = document.querySelector('#http-access-statistics');
			if (httpStatisticsCell) {

				let drawStatistics = (interval) => {

					fetch(`${Structr.rootUrl}stats?interval=${interval}`).then(response => response.json()).then(json => {

						httpStatisticsCell.innerHTML = `
							<label>Resolution: </label>
							<select id="statistics-resolution-selector">
								<option value="60000">Minutes</option>
								<option value="3600000">Hours</option>
								<option value="86400000">Days</option>
								</option>
							</select>
							<button id="statistics-refresh-button">Refresh</button>
							<div id="statistics-diagram-container"></div>
						`;

						let data = json.result;
						let items = [];
						let groups = {};
						let id = 0;
						let min = 999999999999999;
						let max = 0;

						for (let key of Object.keys(data)) {

							let local = data[key];
							let group = id++;

							if (!groups[key]) {
								groups[key] = {
									id: group,
									content: key,
									className: 'http-statistics-' + key
								};
							}

							for (let date of Object.keys(local)) {

								let dateNumber = new Number(date);
								min = Math.min(dateNumber, min);
								max = Math.max(dateNumber, max);
								items.push({
									x: new Date(dateNumber).toISOString(),
									//x2: new Date(dateNumber + interval).toISOString(),
									y: local[date],
									group: group
								});
							}
						}

						// console.log(items);

						let groupList = Object.values(groups);

						let timeAxisScale = {
							//'1000': 'second',
							'60000': 'minute',
							'3600000': 'hour'
						};
						let timeAxisStep = {
							//'1000': 600,
							'60000': 15,
							'3600000': 1
						};

						let options = {
							start: new Date(min - interval).toISOString(),
							end: new Date(max + interval).toISOString(),
							legend: true,
							drawPoints: true,
							barChart: {
								align: 'right'
							},
							style: 'bar',
							stack: true,
							dataAxis: {
								left: {
									range: {
										min: 0
									}
								}
							},
							timeAxis: {
								scale: timeAxisScale[interval],
								step: timeAxisStep[interval]
							}
						};

						new vis.Graph2d(httpStatisticsCell.querySelector("#statistics-diagram-container"), new vis.DataSet(items), new vis.DataSet(groupList), options);

						let selector = document.querySelector('#statistics-resolution-selector');

						selector.addEventListener('change', (e) => {
							drawStatistics(new Number(e.target.value));
						});

						selector.value = interval;

						// refresh button
						httpStatisticsCell.querySelector("#statistics-refresh-button").addEventListener('click', () => {
							drawStatistics(interval);
						});
					});
				}

				drawStatistics(60000);
			}

			// UISettings.showSettingsForCurrentModule();

			if (dashboardUiConfig.envInfo.databaseService === 'MemoryDatabaseService') {
				Structr.appendInMemoryInfoToElement($('#dashboard-about-structr .db-driver'));
			}

			let formats = {
				with_dashes: 'UUIDv4 with dashes',
				without_dashes: 'UUIDv4 without dashes',
				both: 'UUIDv4 with or without dashes'
			};

			let allowedUUIDFormatElement = document.querySelector('.allowed-uuid-format');
			allowedUUIDFormatElement.textContent = formats[dashboardUiConfig.envInfo.dashboardInfo.allowedUUIDFormat];

			if (dashboardUiConfig.envInfo.dashboardInfo.allowedUUIDFormat === 'both') {

				let compact = dashboardUiConfig.envInfo.dashboardInfo.createCompactUUIDs === true;
				allowedUUIDFormatElement.textContent += ' (created UUIDs will ' + (compact ? 'not' : '')  + ' contain dashes)';

				_Helpers.appendInfoTextToElement({
					element: allowedUUIDFormatElement,
					customToggleIcon: _Icons.iconWarningYellowFilled,
					text: 'Warning! This mode is not supported and is only meant to be used to consolidate databases where both UUID formats are used. Using this setting is strongly discouraged and should only be temporary!',
					width: 16,
					height: 16,
					noSpan: true,
					css: {
						marginLeft: '6px'
					}
				});
			}

			_Dashboard.initializeTabsAndTabMenu(dashboardUiConfig);

			Structr.mainMenu.unblock(100);

		} catch (e) {

			if (retryCount < 3) {
				window.setTimeout(() => {
					_Dashboard.onload(++retryCount);
				}, 250);
			} else {
				console.log(e);
			}
		}

	},
	initializeTabsAndTabMenu: (dashboardUiConfig) => {

		let allTabLinks = Structr.functionBar.querySelectorAll('.tabs-menu li a');

		let removeActiveClass = (nodelist) => {
			for (let el of nodelist) {
				el.classList.remove('active');
			}
		};

		for (let tabLink of allTabLinks) {

			let urlHash   = tabLink.getAttribute('href');
			let subModule = urlHash.split(':')[1];

			// init target module
			_Dashboard.tabs[subModule].init(dashboardUiConfig);

			tabLink.addEventListener('click', (e) => {
				e.preventDefault();

				// find currently active module and run onHide
				let activeTabLink = Structr.functionBar.querySelector('.tabs-menu li.active a');
				if (activeTabLink) {
					let activeSubModule = activeTabLink.getAttribute('href').split(':')[1];
					_Dashboard.tabs[activeSubModule]?.onHide?.();
				}

				let targetId = '#dashboard-' + subModule;
				window.location.hash = urlHash;

				removeActiveClass(document.querySelectorAll('#dashboard .tabs-contents .tab-content'));
				removeActiveClass(Structr.functionBar.querySelectorAll('.tabs-menu li'));

				e.target.closest('li').classList.add('active');
				document.querySelector(targetId).classList.add('active');
				LSWrapper.setItem(_Dashboard.activeTabPrefixKey, subModule);

				_Dashboard.tabs[subModule].onShow();
			});
		}

		_Dashboard.activateCurrentTab();
	},
	activateCurrentTab: () => {

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
	},

	tabs: {
		'about-me': {
			onShow: async () => {},
			onHide: async () => {},
			init: () => {}
		},
		'about-structr': {
			onShow: async () => {},
			onHide: async () => {},
			init: (dashboardUiConfig) => {
				_Dashboard.tabs['about-structr'].gatherVersionUpdateInfo(dashboardUiConfig.envInfo);
				_Dashboard.tabs['about-structr'].checkLicenseEnd(dashboardUiConfig.envInfo, $('#dashboard-about-structr .end-date'), { noSpan: true });
			},
			gatherVersionUpdateInfo: async (envInfo) => {

				let currentVersion = envInfo.version;
				// let currentBuild = envInfo.build;
				// let currentDate = envInfo.date;

				let releasesIndexUrl  = 'https://download.structr.com/repositories/releases/org/structr/structr/index';
				let snapshotsIndexUrl = 'https://download.structr.com/repositories/snapshots/org/structr/structr/index';

				let releaseInfo  = '';
				let snapshotInfo = '';

				// Search for newer releases and store latest version
				{
					let response = await fetch(releasesIndexUrl);
					if (response.ok) {

						let releaseVersionsList = await response.text();
						let newReleaseAvailable = undefined;

						for (let version of releaseVersionsList.split(/[\n\r]/)) {
							if (version > currentVersion) {
								newReleaseAvailable = version;
							}
						}

						releaseInfo = (newReleaseAvailable ? 'newer release available: ' +  newReleaseAvailable : 'no new release available');
					}
				}

				// Search for newer snapshots and store latest version
				{
					let response = await fetch(snapshotsIndexUrl);
					if (response.ok) {

						let snapshotVersionsList = await response.text();
						let newSnapshotAvailable = undefined;

						for (let version of snapshotVersionsList.split(/[\n\r]/)) {
							if (version > currentVersion) {
								newSnapshotAvailable = version;
							}
						}

						snapshotInfo = (newSnapshotAvailable ? 'newer snapshot available: ' +  newSnapshotAvailable : 'no new snapshot available');
					}
				}

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
			},
			checkLicenseEnd: (envInfo, element, cfg) => {

				if (envInfo && envInfo.endDate && element) {

					let showMessage = true;
					let daysLeft    = Math.ceil((new Date(envInfo.endDate.slice(0, 10)) - new Date()) / 86400000) + 1;

					let config = Object.assign({
						element: element,
						appendToElement: element
					}, cfg);

					if (daysLeft <= 0) {

						config.customToggleIcon        = _Icons.iconErrorRedFilled;
						config.customToggleIconClasses = ['icon-red', 'ml-2'];
						config.text = "Your Structr <b>license has expired</b>. Upon restart the Community edition will be loaded.";

					} else if (daysLeft <= 7) {

						config.customToggleIcon        = _Icons.iconWarningYellowFilled;
						config.customToggleIconClasses = ['ml-2'];
						config.text = "Your Structr <b>license will expire in less than a week</b>. After that the Community edition will be loaded.";

					} else {
						showMessage = false;
					}

					if (showMessage) {

						config.text += " Please get in touch via <b>licensing@structr.com</b> to renew your license.";
						_Helpers.appendInfoTextToElement(config);
					}
				}
			},

		},
		deployment: {
			deploymentHistoryKey: 'deploymentHistory_' + location.port,
			onShow: async () => {},
			onHide: async () => {},
			init: () => {

				_Dashboard.tabs.deployment.wizard.init();

				// App Import
				let deploymentFileInput           = document.getElementById('deployment-file-input');
				let deploymentUrlInput            = document.getElementById('deployment-url-input');
				let deploymentZipContentPathInput = document.getElementById('deployment-zip-content');

				deploymentFileInput.addEventListener('input', () => {
					deploymentUrlInput.value = '';
				});

				deploymentUrlInput.addEventListener('input', () => {
					deploymentFileInput.value = '';
				});

				_Dashboard.tabs.deployment.history.refreshAll();

				document.getElementById('do-app-import').addEventListener('click', () => {
					_Dashboard.tabs.deployment.actions.deploy('import', document.getElementById('deployment-source-input').value);
				});

				document.getElementById('do-app-import-from-zip').addEventListener('click', () => {

				    let zipContentPath = deploymentZipContentPathInput?.value ?? null;

					if (deploymentFileInput && deploymentFileInput.files.length > 0) {

						_Dashboard.tabs.deployment.actions.importFromZIPFileUpload('app', deploymentFileInput, zipContentPath);

					} else {

						let downloadUrl = deploymentUrlInput.value;

						if (!(downloadUrl && downloadUrl.length)) {
							new WarningMessage().title('Unable to start application import from URL').text('Please enter a URL or upload a ZIP file containing the application export.').requiresConfirmation().show();
						} else {
							_Dashboard.tabs.deployment.actions.importFromZIPURL('app', downloadUrl, zipContentPath);
						}
					}
				});

				// App Export
				document.getElementById('do-app-export').addEventListener('click', () => {
					_Dashboard.tabs.deployment.actions.deploy('export', document.getElementById('app-export-target-input').value);
				});

				document.getElementById('do-app-export-to-zip').addEventListener('click', () => {
					_Dashboard.tabs.deployment.actions.exportAsZip();
				});

				// Data Import
				let dataDeploymentFileInput             = document.getElementById('data-deployment-file-input');
				let dataDeploymentUrlInput              = document.getElementById('data-deployment-url-input');
				let dataDeploymentZipContentPathInput   = document.getElementById('data-deployment-zip-content');

				dataDeploymentFileInput.addEventListener('input', () => {
					dataDeploymentUrlInput.value = '';
				});

				dataDeploymentUrlInput.addEventListener('input', () => {
					dataDeploymentFileInput.value = '';
				});

				document.getElementById('do-data-import').addEventListener('click', () => {
					_Dashboard.tabs.deployment.actions.deployData('import', document.getElementById('data-import-source-input').value);
				});

				document.getElementById('do-data-import-from-zip').addEventListener('click', () => {

					let zipContentPath = dataDeploymentZipContentPathInput?.value ?? null;

					if (dataDeploymentFileInput && dataDeploymentFileInput.files.length > 0) {

						_Dashboard.tabs.deployment.actions.importFromZIPFileUpload('data', dataDeploymentFileInput, zipContentPath);

					} else {

						let downloadUrl = dataDeploymentUrlInput.value;

						if (!(downloadUrl && downloadUrl.length)) {

							new WarningMessage().title('Unable to start data import from URL').text('Please enter a URL or upload a ZIP file containing the data export.').requiresConfirmation().show();

						} else {

							_Dashboard.tabs.deployment.actions.importFromZIPURL('data', downloadUrl, zipContentPath);
						}
					}
				});

				// Data Export
				document.getElementById('do-data-export').addEventListener('click', () => {
					_Dashboard.tabs.deployment.actions.deployData('export', document.querySelector('#data-export-target-input').value, $('#data-export-types-input').val());
				});

				document.getElementById('do-data-export-to-zip').addEventListener('click', () => {
					_Dashboard.tabs.deployment.actions.exportDataAsZip();
				});

				// FIXME: This can only load actual custom nodes, for ALL node types we would need to use _schema
				Command.list('SchemaNode', true, 1000, 1, 'name', 'asc', 'id,name,isBuiltinType,isServiceClass', (nodes) => {

					nodes = nodes.filter(n => !n.isServiceClass);

					let builtinTypes = [];
					let customTypes  = [];

					for (let n of nodes) {
						if (n.isBuiltinType) {
							builtinTypes.push(n);
						} else {
							customTypes.push(n);
						}
					}

					for (let typesSelectElemSelector of ['#data-export-types-input', '#zip-data-export-types-input']) {

						let typesSelectElem = $(typesSelectElemSelector);

						$('.custom-types', typesSelectElem).append(customTypes.map((type) => `<option>${type.name}</option>`).join(''));
						$('.builtin-types', typesSelectElem).append(builtinTypes.map((type) => `<option>${type.name}</option>`).join(''));

						typesSelectElem.select2({
							search_contains: true,
							width: '100%',
							dropdownParent: typesSelectElem.parent(),
							dropdownCssClass: ':all:',
							closeOnSelect: false,
							scrollAfterSelect: false
						});
					}
				});
			},
			actions: {
				deploy: async (mode, location) => {

					if (!(location && location.length)) {
						new WarningMessage().title(`Unable to start application ${mode}`).text(`Please enter a local directory path for application ${mode}.`).requiresConfirmation().show();
						return;
					}

					let data = {
						mode: mode,
						[mode === 'import' ? 'source' : 'target']: location
					};

					let confirm = await _Dialogs.confirmation.showPromise(`Are you sure you want to start an application ${mode}?<br>This will overwrite application data ${mode === 'export' ? 'on disk' : 'in the database'}.`, false);
					if (confirm === true) {

						_Dashboard.tabs.deployment.history.addEntry(data);

						// do not listen for errors - they are sent by the backend via WS
						await fetch(`${Structr.rootUrl}maintenance/deploy`, {
							method: 'POST',
							body: JSON.stringify(data)
						});
					}
				},
				deployData: async (mode, location, types) => {

					if (!(location && location.length)) {
						new WarningMessage().title(`Unable to start data ${mode}`).text(`Please enter a local directory path for data ${mode}.`).requiresConfirmation().show();
						return;
					}

					let data = {
						mode: mode,
						[mode === 'import' ? 'source' : 'target']: location
					};

					if (mode === 'export') {

						if (types && types.length) {
							data['types'] = types.join(',');
						} else {
							new WarningMessage().title(`Unable to ${mode} data`).text('Please select at least one data type.').requiresConfirmation().show();
							return;
						}
					}

					let confirm = await _Dialogs.confirmation.showPromise(`Are you sure you want to start a data ${mode}?<br>This will overwrite data ${mode === 'export' ? 'on disk' : 'in the database'}.`, false);
					if (confirm = true) {

						// do not listen for errors - they are sent by the backend via WS
						await fetch(`${Structr.rootUrl}maintenance/deployData`, {
							method: 'POST',
							body: JSON.stringify(data)
						});

						// update data to distinguish it in our history
						data.mode += 'Data';
						data.types = types;
						_Dashboard.tabs.deployment.history.addEntry(data);
					}
				},
				exportAsZip: () => {

					let prefix          = _Dashboard.tabs.deployment.helpers.cleanFileNamePrefix(document.getElementById('zip-export-prefix').value);
					let appendTimestamp = document.getElementById('zip-export-append-timestamp').checked;

					let historyData = {
						mode: 'exportAsZip',
						prefix: prefix,
						ts: appendTimestamp
					};

					if (appendTimestamp) {
						prefix = _Dashboard.tabs.deployment.helpers.appendTimeStampToPrefix(prefix);
					}

					if (prefix === '') {

						new WarningMessage().title('Unable to export application').text('Please enter a prefix or select "Append timestamp"').requiresConfirmation().show();

					} else {

						_Dashboard.tabs.deployment.history.addEntry(historyData);
						window.location = Structr.deployRoot + '?name=' + prefix;
					}
				},
				exportDataAsZip: () => {

					let prefix                   = _Dashboard.tabs.deployment.helpers.cleanFileNamePrefix(document.getElementById('zip-data-export-prefix').value);
					let appendTimestamp          = document.getElementById('zip-data-export-append-timestamp').checked;
					let zipDataExportTypesSelect = document.getElementById('zip-data-export-types-input');
					let types                    = Array.from(zipDataExportTypesSelect.selectedOptions).map(o => o.value);

					let historyData = {
						mode: 'exportDataAsZip',
						prefix: prefix,
						ts: appendTimestamp,
						types: types
					};

					if (appendTimestamp) {
						prefix = _Dashboard.tabs.deployment.helpers.appendTimeStampToPrefix(prefix);
					}

					if (types.length === 0) {

						new WarningMessage().title('Unable to start data export').text('Please select at least one data type.').requiresConfirmation().show();

					} else if (prefix === '') {

						new WarningMessage().title('Unable to start data export').text('Please enter a prefix or select "Append timestamp"').requiresConfirmation().show();

					} else {

						_Dashboard.tabs.deployment.history.addEntry(historyData);
						window.location = `${Structr.deployRoot}?mode=data&name=${prefix}&types=${types.join(',')}`;
					}
				},
				importFromZIPURL: async (deploymentType, downloadUrl, zipContentPath) => {

					let confirm = await _Dialogs.confirmation.showPromise(`Are you sure you want to start ${deploymentType} import from the given ZIP URL?<br>This will overwrite ${deploymentType === 'app' ? 'the application' : 'data'} in the database.`, false);
					if (confirm === true) {

						let formData = new FormData();
						formData.append('redirectUrl', window.location.pathname);
						formData.append('zipContentPath', zipContentPath);
						formData.append('downloadUrl', downloadUrl);
						formData.append('mode', deploymentType);

						let response = await fetch(Structr.deployRoot, {
							method: 'POST',
							body: formData
						});

						if (!response.ok && response.status === 400) {

							let responseText = await response.text();
							new WarningMessage().title(`Unable to import ${deploymentType} from ZIP URL`).text(responseText).requiresConfirmation().show();

						} else {

							_Dashboard.tabs.deployment.history.addEntry({
								mode: (deploymentType === 'app' ? 'deployFromZIPURL' : 'deployDataFromZIPURL'),
								zipContentPath: zipContentPath,
								downloadUrl: downloadUrl
							});
						}
					}
				},
				importFromZIPFileUpload: async (deploymentType, filesSelectField, zipContentPath) => {

					let confirm = await _Dialogs.confirmation.showPromise(`Are you sure you want to start ${deploymentType} import from the given ZIP file?<br>This will overwrite ${deploymentType === 'app' ? 'the application' : 'data'} in the database.`, false);
					if (confirm === true) {

						let formData = new FormData();
						formData.append('redirectUrl', window.location.pathname);
						formData.append('zipContentPath', zipContentPath);
						formData.append('mode', deploymentType);
						formData.append('file', filesSelectField.files[0]);

						let response = await fetch(Structr.deployRoot, {
							method: 'POST',
							body: formData
						});

						if (!response.ok && response.status === 400) {

							let responseText = await response.text();
							new WarningMessage().title(`Unable to import ${deploymentType} from uploaded ZIP`).text(responseText).requiresConfirmation().show();

						} else {

							// this can not be restored via deployment history
						}
					}
				},
			},
			helpers: {
				cleanFileNamePrefix: (prefix) => {
					let cleaned = prefix.replaceAll(/[^a-zA-Z0-9 _-]/g, '').trim();
					if (cleaned !== prefix) {
						new InfoMessage().title('Cleaned prefix').text(`The given filename prefix was changed to "${cleaned}".`).requiresConfirmation().show();
					}
					return cleaned;
				},
				appendTimeStampToPrefix: (prefix) => {

					let date    = new Date();
					let year    = date.getFullYear();
					let month   = String(date.getMonth() + 1).padStart(2, '0');
					let day     = String(date.getDate()).padStart(2, '0');
					let hours   = String(date.getHours()).padStart(2, '0');
					let minutes = String(date.getMinutes()).padStart(2, '0');
					let seconds = String(date.getSeconds()).padStart(2, '0');

					return `${prefix}_${year}${month}${day}_${hours}${minutes}${seconds}`;
				},
				getMessageMarkupIfDeploymentServletInactive: (config, message) => (config.deploymentServletAvailable ? '' : _Dashboard.tabs.deployment.templates.servletMessage({ message }))
			},
			history: {
				get: () => LSWrapper.getItem(_Dashboard.tabs.deployment.deploymentHistoryKey, []),
				set: (entries) => LSWrapper.setItem(_Dashboard.tabs.deployment.deploymentHistoryKey, entries),
				modes: {
					import: {
						containerId: '#dropdown-deployment-import',
						rowTpl: entry => _Dashboard.tabs.deployment.history.templates.rowData({ 'Path': entry.source }),
						apply: entry => {
							document.querySelector('#deployment-source-input').value = entry.source;
						}
					},
					deployFromZIPURL: {
						containerId: '#dropdown-deployment-import-url',
						rowTpl: entry => _Dashboard.tabs.deployment.history.templates.rowData({ 'URL': entry.downloadUrl, 'Path': entry.zipContentPath }),
						apply: entry => {
							document.querySelector('#deployment-url-input').value   = entry.downloadUrl;
							document.querySelector('#deployment-zip-content').value = entry.zipContentPath;
						}
					},
					importData: {
						containerId: '#dropdown-deployment-import-data',
						rowTpl: entry => _Dashboard.tabs.deployment.history.templates.rowData({ 'Path': entry.source }),
						apply: entry => {
							document.querySelector('#data-import-source-input').value = entry.source;
						}
					},
					deployDataFromZIPURL: {
						containerId: '#dropdown-deployment-import-data-url',
						rowTpl: entry => _Dashboard.tabs.deployment.history.templates.rowData({ 'URL': entry.downloadUrl, 'Path': entry.zipContentPath }),
						apply: entry => {
							document.querySelector('#data-deployment-url-input').value   = entry.downloadUrl;
							document.querySelector('#data-deployment-zip-content').value = entry.zipContentPath;
						}
					},
					export: {
						containerId: '#dropdown-deployment-export',
						rowTpl: entry => _Dashboard.tabs.deployment.history.templates.rowData({ 'Path': entry.target }),
						apply: entry => {
							document.querySelector('#app-export-target-input').value = entry.target;
						}
					},
					exportAsZip: {
						containerId: '#dropdown-deployment-export-zip',
						rowTpl: entry => _Dashboard.tabs.deployment.history.templates.rowData({ 'Prefix': entry.prefix, 'Timestamp': (entry.ts ? 'yes' : 'no') }),
						apply: entry => {
							document.querySelector('#zip-export-prefix').value             = entry.prefix;
							document.querySelector('#zip-export-append-timestamp').checked = entry.ts;
						}
					},
					exportData: {
						containerId: '#dropdown-deployment-export-data',
						rowTpl: entry => _Dashboard.tabs.deployment.history.templates.rowData({ 'Path': entry.target, '# of types': `<span title="${entry.types.join(', ')}">${entry.types.length}</span>` }),
						apply: entry => {
							document.querySelector('#data-export-target-input').value = entry.target;

							let jquerySelect = $(document.querySelector('#data-export-types-input'));
							jquerySelect.val(entry.types);
							jquerySelect.trigger('change');
						}
					},
					exportDataAsZip: {
						containerId: '#dropdown-deployment-export-data-zip',
						rowTpl: entry => _Dashboard.tabs.deployment.history.templates.rowData({ 'Prefix': entry.prefix, 'Timestamp': (entry.ts ? 'yes' : 'no'), '# of types': `<span title="${entry.types.join(', ')}">${entry.types.length}</span>` }),
						apply: entry => {
							document.querySelector('#zip-data-export-prefix').value             = entry.prefix;
							document.querySelector('#zip-data-export-append-timestamp').checked = entry.ts;

							let jquerySelect = $(document.querySelector('#zip-data-export-types-input'));
							jquerySelect.val(entry.types);
							jquerySelect.trigger('change');
						}
					}
				},
				refreshAll: () => {

					for (let variantKey in _Dashboard.tabs.deployment.history.modes) {
						_Dashboard.tabs.deployment.history.refresh(variantKey);
					}
				},
				refresh: (deploymentMode) => {

					let modeDetail     = _Dashboard.tabs.deployment.history.modes[deploymentMode];
					let historyEntries = _Dashboard.tabs.deployment.history.get().filter(e => e.mode === deploymentMode).reverse();
					let container      = document.querySelector(modeDetail.containerId);

					_Helpers.fastRemoveAllChildren(container);

					if (historyEntries.length == 0) {

						container.append(_Helpers.createSingleDOMElementFromHTML('<div class="p-2">No history entries</div>'));

					} else {

						for (let entry of historyEntries) {

							let html    = _Dashboard.tabs.deployment.history.templates.row({ rowHTML: modeDetail.rowTpl(entry) });
							let element = _Helpers.createSingleDOMElementFromHTML(html);

							element.querySelector('.remove-action').addEventListener('click', (e) => {
								e.stopPropagation();
								_Dashboard.tabs.deployment.history.removeEntry(entry);
							});

							element.addEventListener('click', () => {
								_Dashboard.tabs.deployment.history.modes[entry.mode].apply(entry);
								Structr.hideDropdownContainer(container);
							});

							container.append(element);
						}
					}
				},
				addEntry: (entry) => {

					let entries = _Dashboard.tabs.deployment.history.removeEntry(entry, false);
					entries.push(entry);
					_Dashboard.tabs.deployment.history.set(entries);
					_Dashboard.tabs.deployment.history.refresh(entry.mode);
				},
				removeEntry: (entry, refresh = true) => {

					let entries = _Dashboard.tabs.deployment.history.get().filter(oldEntry => (false === Object.keys(oldEntry).every(oldKey => (JSON.stringify(oldEntry[oldKey]) === JSON.stringify(entry[oldKey])))));

					_Dashboard.tabs.deployment.history.set(entries);

					if (refresh === true) {
						_Dashboard.tabs.deployment.history.refresh(entry.mode);
					}

					return entries;
				},
				templates: {
					dropdown: config => `
						<div class="dropdown-menu dropdown-menu-large ml-2">
							<button class="mr-0 dropdown-select" data-preferred-position-x="${config.position ?? ''}">
								${_Icons.getSvgIcon(_Icons.iconHistory, 16, 16, '', 'History')}
							</button>

							<div class="dropdown-menu-container" id="${config.id}"></div>
						</div>
					`,
					row: config => `
						<div class="flex items-center hover:bg-gray-100">
							<div class="flex-grow cursor-pointer p-3">
								${config.rowHTML}
							</div>
							${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-action', 'mr-3']), 'Remove from history')}
						</div>
					`,
					rowData: config => `
						<div class="flex flex-col gap-1 max-w-120 min-w-80">
							${Object.entries(config).map(([key, value]) => `
								<div class="flex gap-1">
									<div class="font-bold">${key}:</div>
									<div class="truncate">${value}</div>
								</div>
							`).join('')}
						</div>
					`,
				}
			},
			wizard: {
				stateKey: 'deploymentWizardState_' + location.port,
				init: () => {

					let allRadios = document.querySelectorAll('input[data-is-deployment-radio]');

					let radioButtonChanged = () => {

						let state = Object.fromEntries([...allRadios].filter(r => r.checked).map(r => [r.name, r.value]));

						_Dashboard.tabs.deployment.wizard.saveState(state);

						_Dashboard.tabs.deployment.wizard.updateUi(Object.values(state));
					};

					for (let radio of allRadios) {
						radio.addEventListener('change', radioButtonChanged);
					}

					_Dashboard.tabs.deployment.wizard.updateWizardVisibility();

					_Dashboard.tabs.deployment.wizard.restoreState(allRadios);
				},
				updateUi: (values) => {

					let wizardEnabled = UISettings.getValueForSetting(UISettings.settingGroups.dashboard.settings.useDeploymentWizard);

					if (wizardEnabled && values.length === 3) {

						_Helpers.addClasses(document.querySelectorAll('[data-is-deployment-container]'), ['hidden']);

						let selectedDataAttributes = values.map(value => `[data-is-deployment-${value}]`).join('');

						document.querySelector(selectedDataAttributes)?.classList.remove('hidden');
					}
				},
				saveState: (state) => {
					LSWrapper.setItem(_Dashboard.tabs.deployment.wizard.stateKey, state);
				},
				restoreState: (allRadios) => {

					let state = LSWrapper.getItem(_Dashboard.tabs.deployment.wizard.stateKey, {});

					for (let radio of allRadios) {

						let savedValue = state[radio.name];

						if (radio.value === savedValue) {
							radio.checked = true;
						}
					}

					_Dashboard.tabs.deployment.wizard.updateUi(Object.values(state));
				},
				updateWizardVisibility: () => {

					let wizardEnabled = UISettings.getValueForSetting(UISettings.settingGroups.dashboard.settings.useDeploymentWizard);

					let deploymentWizardNodes = document.querySelectorAll('[data-is-deployment-wizard]');
					let deploymentDesignNodes = document.querySelectorAll('[data-is-deployment-design]');
					let deploymentActionNodes = document.querySelectorAll('[data-is-deployment-container]');

					if (wizardEnabled) {

						_Helpers.removeClasses(deploymentWizardNodes, ['hidden']);
						_Helpers.addClasses(deploymentDesignNodes, ['hidden']);
						_Helpers.addClasses(deploymentActionNodes, ['hidden']);

						// change direction of history dropdowns (some dropdowns are left-oriented because they are close to the right border
						let leftyDropdowns = document.querySelectorAll('#dashboard-deployment [data-preferred-position-x=left]');
						for (let lefty of leftyDropdowns) {
							lefty.dataset['preferredPositionX'] = null;
							lefty.dataset['preferredPositionXBak'] = 'left';
						}

					} else {

						_Helpers.addClasses(deploymentWizardNodes, ['hidden']);
						_Helpers.removeClasses(deploymentDesignNodes, ['hidden']);
						_Helpers.removeClasses(deploymentActionNodes, ['hidden']);

						// change direction of history dropdowns (some dropdowns are left-oriented because they are close to the right border
						let leftyDropdowns = document.querySelectorAll('#dashboard-deployment [data-preferred-position-x-bak=left]');
						for (let lefty of leftyDropdowns) {
							lefty.dataset['preferredPositionXBak'] = null;
							lefty.dataset['preferredPositionX'] = 'left';
						}
					}

					let allRadios = document.querySelectorAll('input[data-is-deployment-radio]');

					_Dashboard.tabs.deployment.wizard.updateHistoryDropdownPositions(allRadios);

					_Dashboard.tabs.deployment.wizard.restoreState(allRadios);
				},
				updateHistoryDropdownPositions: () => {

					let wizardEnabled = UISettings.getValueForSetting(UISettings.settingGroups.dashboard.settings.useDeploymentWizard);

					if (wizardEnabled) {

						// change direction of history dropdowns (some dropdowns are left-oriented because they are close to the right border
						let leftyDropdowns = document.querySelectorAll('#dashboard-deployment [data-preferred-position-x=left]');
						for (let lefty of leftyDropdowns) {
							lefty.dataset['preferredPositionX'] = null;
							lefty.dataset['preferredPositionXBak'] = 'left';
						}

					} else {

						// change direction of history dropdowns (some dropdowns are left-oriented because they are close to the right border
						let leftyDropdowns = document.querySelectorAll('#dashboard-deployment [data-preferred-position-x-bak=left]');
						for (let lefty of leftyDropdowns) {
							lefty.dataset['preferredPositionXBak'] = null;
							lefty.dataset['preferredPositionX'] = 'left';
						}
					}
				},
				templates: {
					ui: (config) => `
						<div class="hidden mb-8" data-is-deployment-wizard>

							<div class="inline-info">
								<div class="inline-info-icon">
									${_Icons.getSvgIcon(_Icons.iconInfo, 24, 24)}
								</div>
								<div class="inline-info-text">
									A Structr application consists of functional components and user or domain data that can be exported and imported separately.
									<br><br>
									The process can be configured using the following three selection options <b>Action/Mode</b>, <b>Type</b> and <b>Target/Source</b>.
									<br><br>
									To avoid misunderstandings, errors and unintentionally overwriting data, please be sure to read the help texts carefully before starting the export or import.
									<br><br>
									If in doubt, contact <a href="https://support.structr.com">Structr Support</a>.
								</div>
							</div>								

							<div class="flex gap-8">

								<fieldset class="border-0 p-0">
									<h3>Action / Mode<span data-comment="If <b>Export</b> is selected, no data is changed in this instance, but an older export data record may be overwritten.<br><br>If <b>Import</b> is selected, data in this Structr instance will be overwritten."></span></h3>
									<div class="w-1/3 mb-4">
										<p>Specify if data is to be exported from or imported into Structr.</p>
										
									</div>
									<div class="options-switch">
										<input type="radio" id="deploy-action-export" name="deploy-action-radio" value="export" data-is-deployment-radio>
										<label for="deploy-action-export">Export</label>
										<input type="radio" id="deploy-action-import" name="deploy-action-radio" value="import" data-is-deployment-radio>
										<label for="deploy-action-import">Import</label>
									</div>
								</fieldset>

								<fieldset class="border-0 p-0">
									<h3>Type<span data-comment="<b>Application</b> is equivalent to the source code of an application in a classic development environment.<br><br>Select <b>Data</b> for handling user-created and domain-specific data of a Structr instance."></span></h3>
									<div class="w-1/3 mb-4">
										<p>Choose the type of data to be exported or imported.</p>
									</div>
									<div class="options-switch">
										<input type="radio" id="deploy-type-application" name="deploy-what-radio" value="app" data-is-deployment-radio>
										<label for="deploy-type-application">Application</label>
										<input type="radio" id="deploy-type-data" name="deploy-what-radio" value="data" data-is-deployment-radio>
										<label for="deploy-type-data">Data</label>
									</div>
								</fieldset>

								<fieldset class="border-0 p-0">
									<h3>Target / Source<span data-comment="<b>Server directory</b> means a local directory on the server on which Structr is running.<br><br><b>ZIP</b> means downloading the export as a ZIP file or upload a ZIP file to import app or user data."></span></h3>
									<div class="w-1/3 mb-4">
										<p>Select where to/from it should be written/read.</p>
									</div>
									<div class="options-switch">
										<input type="radio" id="deploy-target-local" name="deploy-how-radio" value="local" data-is-deployment-radio>
										<label for="deploy-target-local">Server Directory</label>
										<input type="radio" id="deploy-target-zip" name="deploy-how-radio" value="zip" data-is-deployment-radio>
										<label for="deploy-target-zip">ZIP</label>
									</div>
								</fieldset>
							</div>

						</div>
					`
				}
			},
			templates: {
				tabContent: config => `
					<div class="tab-content" id="dashboard-deployment">

						${_Dashboard.tabs.deployment.wizard.templates.ui()}

						<div class="dashboard-grid-wrapper mt-2" data-is-deployment-wrapper>

							<div class="dashboard-grid-row" data-is-deployment-design>
								<h2 class="m-0">Application Import and Export</h2>
							</div>

							<div class="group" data-is-deployment-container data-is-deployment-app data-is-deployment-import data-is-deployment-local>
								<div class="font-bold my-4">Application <span class="group-hover:underline">import</span> from server directory</div>
								<div>
									<div class="flex">
										<input class="mb-4 flex-grow" type="text" id="deployment-source-input" placeholder="Server directory path for app import">
									</div>
									<button class="action" id="do-app-import">Import app from server directory</button>
									${_Dashboard.tabs.deployment.history.templates.dropdown({ id: 'dropdown-deployment-import' })}
								</div>
							</div>

							<div class="group" data-is-deployment-container data-is-deployment-app data-is-deployment-export data-is-deployment-local>
								<div class="font-bold my-4">Application <span class="group-hover:underline">export</span> to server directory</div>
								<div>
									<div class="flex">
										<input class="mb-4 flex-grow" type="text" id="app-export-target-input" placeholder="Server directory path for app export">
									</div>
									<button class="action" id="do-app-export">Export app to server directory</button>
									${_Dashboard.tabs.deployment.history.templates.dropdown({ id: 'dropdown-deployment-export', position: 'left' })}
								</div>
							</div>

							<div class="group" data-is-deployment-container data-is-deployment-app data-is-deployment-import data-is-deployment-zip>
								<div class="font-bold my-4"><span class="group-hover:underline">Import</span> application from URL or upload a ZIP file</div>
								${_Dashboard.tabs.deployment.helpers.getMessageMarkupIfDeploymentServletInactive(config, 'Deployment via URL is not possible because <code>DeploymentServlet</code> is not active.')}
								<div>
									<div class="flex flex-col">
										<input class="mb-4 flex-grow" type="text" id="deployment-url-input" placeholder="Download URL of ZIP file for app import" ${(config.deploymentServletAvailable ? '' : 'disabled')}>
										<input class="mt-1 mb-4 flex-grow" type="text" id="deployment-zip-content" placeholder="Path to the webapp folder inside the ZIP file, leave blank for default" ${(config.deploymentServletAvailable ? '' : 'disabled')}>
										<input class="mt-1 mb-4 flex-grow" type="file" id="deployment-file-input" placeholder="Upload ZIP file" ${(config.deploymentServletAvailable ? '' : 'disabled')}>
									</div>
									<button class="action ${(config.deploymentServletAvailable ? '' : 'disabled')}" ${(config.deploymentServletAvailable ? '' : 'disabled')} id="do-app-import-from-zip">Import app from ZIP file</button>
									${_Dashboard.tabs.deployment.history.templates.dropdown({ id: 'dropdown-deployment-import-url' })}
								</div>
							</div>

							<div class="group" data-is-deployment-container data-is-deployment-app data-is-deployment-export data-is-deployment-zip>
								<div class="font-bold my-4"><span class="group-hover:underline">Export</span> application and download as ZIP file</div>
								${_Dashboard.tabs.deployment.helpers.getMessageMarkupIfDeploymentServletInactive(config, 'Export and download as ZIP file is not possible because <code>DeploymentServlet</code> is not active.')}
								<div>
									<div class="flex flex-col">
										<input class="mb-4 flex-grow" type="text" id="zip-export-prefix" placeholder="ZIP File prefix" ${(config.deploymentServletAvailable ? '' : 'disabled')} value="webapp">
										<label class="checkbox-label">
											<input type="checkbox" id="zip-export-append-timestamp" ${(config.deploymentServletAvailable ? '' : 'disabled')} checked> Append timestamp
										</label>
									</div>
									<button class="action ${(config.deploymentServletAvailable ? '' : 'disabled')}" ${(config.deploymentServletAvailable ? '' : 'disabled')} id="do-app-export-to-zip">Export and download app as ZIP file</button>
									${_Dashboard.tabs.deployment.history.templates.dropdown({ id: 'dropdown-deployment-export-zip', position: 'left' })}
								</div>
							</div>

							<div class="dashboard-grid-row" data-is-deployment-design>
								<hr class="my-4">
							</div>

							<div class="dashboard-grid-row" data-is-deployment-design>
								<h2 class="m-0">Data Import and Export</h2>
							</div>

							<div class="group" data-is-deployment-container data-is-deployment-data data-is-deployment-import data-is-deployment-local>
								<div class="font-bold my-4">Data <span class="group-hover:underline">import</span> from server directory</div>
								<div>
									<div class="flex">
										<input class="mt-1 mb-4 flex-grow" type="text" id="data-import-source-input" placeholder="Local directory path for data import">
									</div>
									<button class="action" id="do-data-import">Import data from server directory</button>
									${_Dashboard.tabs.deployment.history.templates.dropdown({ id: 'dropdown-deployment-import-data' })}
								</div>
							</div>

							<div class="group" data-is-deployment-container data-is-deployment-data data-is-deployment-export data-is-deployment-local>
								<div class="font-bold my-4">Data <span class="group-hover:underline">export</span> to server directory</div>
								<div>
									<div class="flex flex-col">
										<input class="mt-1 mb-4" type="text" id="data-export-target-input" placeholder="Server directory path for data export">
										<select id="data-export-types-input" class="hide-selected-options" data-placeholder="Please select data type(s) to export" multiple="multiple">
											<optgroup label="Custom Types" class="custom-types"></optgroup>
											<optgroup label="Builtin Types" class="builtin-types"></optgroup>
										</select>
									</div>
									<button class="action" id="do-data-export">Export data to server directory</button>
									${_Dashboard.tabs.deployment.history.templates.dropdown({ id: 'dropdown-deployment-export-data', position: 'left' })}
								</div>
							</div>

							<div class="group" data-is-deployment-container data-is-deployment-data data-is-deployment-import data-is-deployment-zip>
								<div class="font-bold my-4"><span class="group-hover:underline">Import</span> data from URL or upload a ZIP file</div>
								${_Dashboard.tabs.deployment.helpers.getMessageMarkupIfDeploymentServletInactive(config, 'Deployment via URL is not possible because <code>DeploymentServlet</code> is not active.')}
								<div>
									<div class="flex flex-col">
										<input class="mt-1 mb-4 flex-grow" type="text" id="data-deployment-url-input" placeholder="Download URL of ZIP file for data import" ${(config.deploymentServletAvailable ? '' : 'disabled')}>
										<input class="mt-1 mb-4 flex-grow" type="text" id="data-deployment-zip-content" placeholder="Path to the data folder inside the ZIP file, leave blank for default" ${(config.deploymentServletAvailable ? '' : 'disabled')}>
										<input class="mt-1 mb-4 flex-grow" type="file" id="data-deployment-file-input" placeholder="Upload ZIP file" ${(config.deploymentServletAvailable ? '' : 'disabled')}>
									</div>
									<button id="do-data-import-from-zip" class="action ${(config.deploymentServletAvailable ? '' : 'disabled')}" ${(config.deploymentServletAvailable ? '' : 'disabled')}>Import data from ZIP file</button>
									${_Dashboard.tabs.deployment.history.templates.dropdown({ id: 'dropdown-deployment-import-data-url' })}
								</div>
							</div>

							<div class="group" data-is-deployment-container data-is-deployment-data data-is-deployment-export data-is-deployment-zip>
								<div class="font-bold my-4"><span class="group-hover:underline">Export</span> data and download as ZIP file</div>
								${_Dashboard.tabs.deployment.helpers.getMessageMarkupIfDeploymentServletInactive(config, 'Export and download data as ZIP file is not possible because <code>DeploymentServlet</code> is not active.')}
								<div>
									<div class="flex flex-col">
										<input class="mt-1 mb-4 flex-grow" type="text" id="zip-data-export-prefix" placeholder="ZIP file prefix" ${(config.deploymentServletAvailable ? '' : 'disabled')} value="data">
										<select id="zip-data-export-types-input" class="hide-selected-options" data-placeholder="Please select data type(s) to export" multiple="multiple">
											<optgroup label="Custom Types" class="custom-types"></optgroup>
											<optgroup label="Builtin Types" class="builtin-types"></optgroup>
										</select>
										<label class="checkbox-label">
											<input type="checkbox" id="zip-data-export-append-timestamp" ${(config.deploymentServletAvailable ? '' : 'disabled')} checked> Append timestamp
										</label>
									</div>
									<button id="do-data-export-to-zip" class="action ${(config.deploymentServletAvailable ? '' : 'disabled')}" ${(config.deploymentServletAvailable ? '' : 'disabled')}>Export and download data as ZIP file</button>
									${_Dashboard.tabs.deployment.history.templates.dropdown({ id: 'dropdown-deployment-export-data-zip', position: 'left' })}
								</div>
							</div>
						</div>
					</div>
				`,
				servletMessage: config => `
					<span class="deployment-warning" data-comment="The DeplyomentServlet can be activated via the config servlet or via structr.conf." data-comment-config='{"insertAfter":true}'>${config.message}</span>
				`
			}
		},
		methods: {
			init: () => {
				_Dashboard.tabs.methods.appendMethods();
			},
			getMarkupForMethod: (method) => `
				<tr>
					<td>
						<span class="method-name">${method.schemaNode ? `<span class="font-semibold">${method.schemaNode.name}</span>.` : ''}${method.name}</span>
					</td>
					<td>
						<button class="run action button flex items-center gap-2">${_Icons.getSvgIcon(_Icons.iconRunButton)} Open run dialog</button>
					</td>
				</tr>
			`,
			appendMethods: () => {

				let container = document.querySelector('#dashboard-methods');
				_Helpers.fastRemoveAllChildren(container);

				fetch(`${Structr.rootUrl}SchemaMethod/schema?isStatic=true&isPrivate=false&${Structr.getRequestParameterName('sort')}=name`, {
					headers: {
						Accept: 'properties=id,type,name,isStatic,httpVerb,schemaNode,summary,description,parameters,index,exampleValue,parameterType'
					}
				}).then(response => {

					if (response.ok) {
						return response.json();
					}

				}).then(data => {

					let userDefinedFunctions = data.result.filter(method => !method.schemaNode);
					let staticFunctions      = data.result.filter(method => method.schemaNode).reduce((acc, curr) => {
						acc[curr.schemaNode.name] ??= {};
						acc[curr.schemaNode.name][curr.name] = curr;
						return acc;
					}, {});

					if (userDefinedFunctions.length === 0 && Object.keys(staticFunctions).length === 0) {

						container.textContent = 'No functions available.';

					} else {

						let callableMethodsListElement = _Helpers.createSingleDOMElementFromHTML(`<table class="props"></table>`);

						for (let method of userDefinedFunctions) {
							_Dashboard.tabs.methods.appendMethod(method, callableMethodsListElement);
						}

						for (let typeName of Object.keys(staticFunctions).sort()) {

							for (let methodName of Object.keys(staticFunctions[typeName]).sort()) {

								let method = staticFunctions[typeName][methodName];

								_Dashboard.tabs.methods.appendMethod(method, callableMethodsListElement);
							}
						}

						container.appendChild(callableMethodsListElement);
					}
				});
			},
			appendMethod: (method, container) => {

				let methodEntry = _Helpers.createSingleDOMElementFromHTML(_Dashboard.tabs.methods.getMarkupForMethod(method));

				methodEntry.querySelector('button.run').addEventListener('click', () => {
					_Schema.methods.runSchemaMethod(method);
				});

				container.appendChild(methodEntry);
			},
			onShow: async () => {},
			onHide: async () => {}
		},
		'server-log': {
			isInitialized: false,
			refreshTimeIntervalKey: 'dashboardLogRefreshTimeInterval' + location.port,
			numberOfLinesKey: 'dashboardNumberOfLines' + location.port,
			truncateLinesAfterKey: 'dashboardTruncateLinesAfter' + location.port,
			selectedLogFileKey: 'dashboardSelectedLogFile' + location.port,
			defaultRefreshTimeIntervalMs: 1000,
			defaultNumberOfLines: 1000,
			defaultTruncateLinesAfter: -1,
			intervalID: undefined,
			textAreaHasFocus: false,
			scrollEnabled: true,
			onShow: async () => {
				_Dashboard.tabs['server-log'].start();
			},
			onHide: async () => {
				_Dashboard.tabs['server-log'].stop();
			},
			getTimeIntervalSelect: () => document.querySelector('#dashboard-server-log-refresh-interval'),
			getTruncateLinesAfterInput: () => document.querySelector('#dashboard-server-log-truncate-lines'),
			getLogFileSelect: () => document.querySelector('#dashboard-server-log-file'),
			getNumberOfLinesInput: () => document.querySelector('#dashboard-server-log-lines'),
			getServerLogTextarea: () => document.querySelector('#dashboard-server-log textarea'),
			getManualRefreshButton: () => document.querySelector('#dashboard-server-log-manual-refresh'),
			getRefreshInterval: () => LSWrapper.getItem(_Dashboard.tabs['server-log'].refreshTimeIntervalKey, _Dashboard.tabs['server-log'].defaultRefreshTimeIntervalMs),
			getNumberOfLines: () => LSWrapper.getItem(_Dashboard.tabs['server-log'].numberOfLinesKey, _Dashboard.tabs['server-log'].defaultNumberOfLines),
			getTruncateLinesAfter: () => LSWrapper.getItem(_Dashboard.tabs['server-log'].truncateLinesAfterKey, _Dashboard.tabs['server-log'].defaultTruncateLinesAfter),
			getSelectedLogFile: () => LSWrapper.getItem(_Dashboard.tabs['server-log'].selectedLogFileKey),
			setFeedback: (message) => {
				let el = document.querySelector('#dashboard-server-log-feedback');
				if (el) {
					el.innerHTML = message;
				}
			},
			init: () => {

				let textarea = _Dashboard.tabs['server-log'].getServerLogTextarea();

				let initServerLogInput = (element, lsKey, defaultValue) => {

					element.value = LSWrapper.getItem(lsKey, defaultValue);

					if (element.tagName === 'SELECT' && element.selectedIndex < 0) {
						element.selectedIndex = 0;
					}

					element.addEventListener('change', (e) => {

						LSWrapper.setItem(lsKey, e.target.value);

						_Dashboard.tabs['server-log'].updateSettings();

						_Helpers.blinkGreen(e.target);
					});
				};

				initServerLogInput(_Dashboard.tabs['server-log'].getTimeIntervalSelect(),      _Dashboard.tabs['server-log'].refreshTimeIntervalKey, _Dashboard.tabs['server-log'].defaultRefreshTimeIntervalMs);
				initServerLogInput(_Dashboard.tabs['server-log'].getNumberOfLinesInput(),      _Dashboard.tabs['server-log'].numberOfLinesKey,       _Dashboard.tabs['server-log'].defaultNumberOfLines);
				initServerLogInput(_Dashboard.tabs['server-log'].getTruncateLinesAfterInput(), _Dashboard.tabs['server-log'].truncateLinesAfterKey,  _Dashboard.tabs['server-log'].defaultTruncateLinesAfter);

				document.querySelector('#dashboard-server-log-copy').addEventListener('click', async () => {
					await navigator.clipboard.writeText(textarea.textContent);
				});

				document.querySelector('#dashboard-server-log-download').addEventListener('click', async () => {

					const file = new File([textarea.textContent], 'structr.log.txt', { type: 'text/plain' });
					const link = document.createElement('a');
					const url  = URL.createObjectURL(file);

					link.href = url;
					link.download = file.name;
					document.body.appendChild(link);
					link.click();

					document.body.removeChild(link);
					window.URL.revokeObjectURL(url);
				});

				_Dashboard.tabs['server-log'].getManualRefreshButton().addEventListener('click', _Dashboard.tabs['server-log'].updateLog);

				textarea.addEventListener('focus', () => {
					_Dashboard.tabs['server-log'].textAreaHasFocus = true;
					_Dashboard.tabs['server-log'].setFeedback('Text area has focus, refresh disabled until focus lost.');
				});

				textarea.addEventListener('blur', () => {
					_Dashboard.tabs['server-log'].textAreaHasFocus = false;
					_Dashboard.tabs['server-log'].setFeedback('');
				});

				textarea.addEventListener('scroll', (event) => {

					let maxScroll     = event.target.scrollHeight - 4;
					let currentScroll = (event.target.scrollTop + event.target.offsetHeight);

					_Dashboard.tabs['server-log'].scrollEnabled = (currentScroll >= maxScroll);

					let isScrolled = (false === _Dashboard.tabs['server-log'].scrollEnabled);

					_Dashboard.tabs['server-log'].getServerLogTextarea()?.parentNode?.classList.toggle('textarea-is-scrolled', isScrolled);
				});

				Command.getAvailableServerLogs().then(data => {

					let logfiles = data.result;

					// sort the rolled logs in descending order
					logfiles.push(...logfiles.splice(1, logfiles.length).reverse());

					let logFileSelect = _Dashboard.tabs['server-log'].getLogFileSelect();
					for (let log of logfiles) {
						logFileSelect.insertAdjacentHTML('beforeend', `<option>${log}</option>`);
					}

					initServerLogInput(_Dashboard.tabs['server-log'].getLogFileSelect(), _Dashboard.tabs['server-log'].selectedLogFileKey);

					_Dashboard.tabs['server-log'].isInitialized = true;
				});
			},
			updateLog: () => {

				if (false === _Dashboard.tabs['server-log'].textAreaHasFocus) {

					let noOfLines     = _Dashboard.tabs['server-log'].getNumberOfLines();
					let truncateAfter = _Dashboard.tabs['server-log'].getTruncateLinesAfter();
					let logFile       = _Dashboard.tabs['server-log'].getSelectedLogFile();

					let logFileForFeedback = logFile ? `(${logFile})` : '';

					_Dashboard.tabs['server-log'].setFeedback(`<span class="flex items-center">${_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 18, 18, 'mr-2')}<span> Refreshing log file ${logFileForFeedback}...</span></span>`);

					Command.getServerLogSnapshot(noOfLines, truncateAfter, logFile).then(log => {

						let textarea = _Dashboard.tabs['server-log'].getServerLogTextarea();
						textarea.textContent = log[0].result;

						if (_Dashboard.tabs['server-log'].scrollEnabled) {
							textarea.scrollTop = textarea.scrollHeight;
						}

						window.setTimeout(() => {
							_Dashboard.tabs['server-log'].setFeedback('');
						}, 250);
					});
				}
			},
			updateSettings: () => {

				_Dashboard.tabs['server-log'].stop();

				let intervalMs    = _Dashboard.tabs['server-log'].getRefreshInterval();
				let hasInterval   = (intervalMs > 0);

				let manualRefreshButton = _Dashboard.tabs['server-log'].getManualRefreshButton();
				manualRefreshButton.classList.toggle('hidden', hasInterval);

				// update once with the new settings
				_Dashboard.tabs['server-log'].updateLog();

				// initialize the interval
				if (hasInterval) {
					_Dashboard.tabs['server-log'].intervalID = window.setInterval(_Dashboard.tabs['server-log'].updateLog, intervalMs);
				}
			},
			waitUntilInitialized: () => {

				return new Promise(resolve => {

					let fn = () => {
						if (_Dashboard.tabs['server-log'].isInitialized) {
							resolve();
						} else {
							window.setTimeout(fn, 100);
						}
					};

					fn();
				})
			},
			start: () => {

				_Dashboard.tabs['server-log'].waitUntilInitialized().then(() => {

					_Dashboard.tabs['server-log'].updateSettings();
				});
			},
			stop: () => {
				window.clearInterval(_Dashboard.tabs['server-log'].intervalID);
			}
		},
		'event-log': {
			onShow: async () => {
				await _Dashboard.tabs['event-log'].loadRuntimeEventLog();
			},
			onHide: async () => {},
			init: () => {

				let container = document.querySelector('#dashboard-event-log');

				container.querySelector('#refresh-event-log').addEventListener('click', _Dashboard.tabs['event-log'].loadRuntimeEventLog);
				container.querySelector('#event-type-filter').addEventListener('change', _Dashboard.tabs['event-log'].loadRuntimeEventLog);
			},
			loadRuntimeEventLog: async () => {

				let tbody  = document.querySelector('#event-log-container');
				let num    = document.querySelector('#event-type-page-size');
				let filter = document.querySelector('#event-type-filter');
				let url    = Structr.rootUrl + '_runtimeEventLog?' + Structr.getRequestParameterName('order') + '=absoluteTimestamp&' + Structr.getRequestParameterName('sort') + '=desc&' + Structr.getRequestParameterName('pageSize') + '=' + num.value;
				let type   = filter.value;

				_Helpers.fastRemoveAllChildren(tbody);

				if (type && type.length) {
					url += '&type=' + type;
				}

				let response = await fetch(url);

				if (response.ok) {

					let result = await response.json();

					for (let event of result.result) {

						let data = event.data;

						let row = _Helpers.createSingleDOMElementFromHTML(`
							<tr>
								<td>${new Date(event.absoluteTimestamp).toISOString()}</td>
								<td>${event.type}</td>
								<td>${event.description}</td>
								<td>${(data ? JSON.stringify(data) : '')}</td>
								<td class="actions-cell"></td>
							</tr>
						`);

						tbody.appendChild(row);

						if (data.id && data.type) {

							let actionsCell = row.querySelector('.actions-cell');

							if (data.type === 'SchemaMethod' || data.type === 'SchemaProperty') {

								let button = _Helpers.createSingleDOMElementFromHTML(`<button>Go to code</button>`);
								actionsCell.appendChild(button);

								button.addEventListener('click', () => {

									Command.get(data.id, _Code.helpers.getAttributesToFetchForErrorObject(), (obj) => {

										_Code.helpers.navigateToSchemaObjectFromAnywhere(obj);
									});
								});

							} else {

								let button = _Helpers.createSingleDOMElementFromHTML(`<button>Open content in editor</button>`);
								actionsCell.appendChild(button);

								button.addEventListener('click', () => {

									Command.get(data.id, null, (obj) => {
										_Elements.openEditContentDialog(obj);
									});
								});
							}
						}
					}
				}
			},
		},
		'running-threads': {
			onShow: async () => {
				await _Dashboard.tabs['running-threads'].loadRunningThreads();
			},
			onHide: async () => {},
			init: () => {

				let container = document.querySelector('#dashboard-running-threads');

				container.querySelector('#refresh-running-threads').addEventListener('click', _Dashboard.tabs['running-threads'].loadRunningThreads);
			},
			loadRunningThreads: async () => {

				let tbody  = document.querySelector('#running-threads-container');

				_Helpers.fastRemoveAllChildren(tbody);

				let response  = await fetch(Structr.rootUrl + 'maintenance/manageThreads', {
					method: 'POST',
					body: JSON.stringify({ command: 'list' })
				});

				if (response.ok) {

					let result = await response.json();

					for (let thread of result.result) {

						let row = _Helpers.createSingleDOMElementFromHTML(`
							<tr>
								<td>${thread.id}</td>
								<td>${thread.name}</td>
								<td>${thread.state}</td>
								<td>${thread.deadlock}</td>
								<td>${thread.cpuTime}</td>
								<td>${thread.stack.join(' > ')}</td>
								<td class="actions-cell"></td>
							</tr>
						`);

						tbody.appendChild(row);

						let actionsCell     = row.querySelector('.actions-cell');
						let interruptButton = _Helpers.createSingleDOMElementFromHTML(`<button>Interrupt</button>`);
						let killButton      = _Helpers.createSingleDOMElementFromHTML(`<button>Kill</button>`);

						actionsCell.appendChild(interruptButton);
						actionsCell.appendChild(killButton);

						interruptButton.addEventListener('click', () => { _Dashboard.tabs['running-threads'].sendThreadCommand(thread.id, 'interrupt'); });
						killButton.addEventListener('click', () => { _Dashboard.tabs['running-threads'].sendThreadCommand(thread.id, 'kill'); });
					}
				}
			},
			sendThreadCommand: async (id, cmd) => {

				await fetch(Structr.rootUrl + 'maintenance/manageThreads', {
					method: 'POST',
					body: JSON.stringify({ command: cmd, id: id })
				});
			}
		},
		'ui-config': {
			onShow: async () => {},
			onHide: async () => {},
			init: (templateConfig) => {
				_Dashboard.tabs['ui-config'].showMainMenuConfiguration();
				_Dashboard.tabs['ui-config'].showConfigurableSettings();
				_Dashboard.tabs['ui-config'].handleResetConfiguration(templateConfig.meObj.id);
			},

			showMainMenuConfiguration: () => {

				let userConfigMenu = Structr.mainMenu.getSavedMenuConfig();

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
					let child = subMenuConfigContainer.querySelector('div[data-name="' + mainMenuItem + '"]');
					if (child) {
						mainMenuConfigContainer.appendChild(child);
					}
				}

				$('#main-menu-entries-config, #sub-menu-entries-config').sortable({
					connectWith: ".connectedSortable",
					stop: _Dashboard.tabs['ui-config'].updateMenu
				}).disableSelection();

				_Dashboard.tabs['ui-config'].updateMenu();
			},

			updateMenu: () => {

				let mainMenuConfigContainer = document.querySelector('#main-menu-entries-config');
				let subMenuConfigContainer  = document.querySelector('#sub-menu-entries-config');

				let newMenuConfig = {
					main: [...mainMenuConfigContainer.querySelectorAll('div.menu-item')].map(el => el.dataset.name),
					sub:  [...subMenuConfigContainer.querySelectorAll('div.menu-item')].map(el => el.dataset.name)
				};

				Structr.mainMenu.update(newMenuConfig);
			},

			showConfigurableSettings: () => {

				let settingsContainer = document.querySelector('#settings-container');
				let allSettings       = UISettings.getSettings();
				let offCanvasDummy    = _Helpers.createSingleDOMElementFromHTML('<div></div>');	// prepare off-canvas to reduce number of renders

				for (let section of allSettings) {
					UISettings.appendSettingsSectionToContainer(section, offCanvasDummy);
				}

				settingsContainer.appendChild(offCanvasDummy);

				_Helpers.activateCommentsInElement(settingsContainer);
			},

			handleResetConfiguration: (userId) => {

				document.querySelector('#clear-local-storage-on-server').addEventListener('click', () => {

					// save before we remove to possibly force an update. (if no update is done, the callback is not fired)
					LSWrapper.save(() => {

						Command.setProperty(userId, 'localStorage', null, false, () => {
							_Helpers.blinkGreen(document.querySelector('#clear-local-storage-on-server'));
							LSWrapper.clear();
							_Dashboard.onload();
						});
					});
				});
			}
		},
	},
	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/dashboard.css">

			<div class="main-app-box" id="dashboard">

				<div class="tabs-contents">

					${_Dashboard.templates.tabContentAboutMe(config)}
					${_Dashboard.templates.tabContentAboutStructr(config)}
					${_Dashboard.tabs.deployment.templates.tabContent(config)}
					${_Dashboard.templates.tabContentUserDefinedMethods(config)}
					${_Dashboard.templates.tabContentServerLog(config)}
					${_Dashboard.templates.tabContentEventLog(config)}
					${_Dashboard.templates.tabContentThreads(config)}
					${_Dashboard.templates.tabContentUIConfig(config)}

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
					<a href="#dashboard:methods">User-defined functions</a>
				</li>
				<li>
					<a href="#dashboard:server-log">Server Log</a>
				</li>
				<li>
					<a href="#dashboard:event-log">Event Log</a>
				</li>
				<li>
					<a href="#dashboard:running-threads">Threads</a>
				</li>
				<li>
					<a href="#dashboard:ui-config">UI Settings</a>
				</li>
			</ul>
		`,
		tabContentAboutMe: config => `
			<div class="tab-content active" id="dashboard-about-me">

				<table class="props">
					<tr>
						<td class="key">User Name</td><td>${config.meObj.name}</td>
					</tr>
					<tr>
						<td class="key">ID</td><td>${config.meObj.id}</td>
					</tr>
					<tr>
						<td class="key">E-Mail</td><td>${config.meObj.eMail || ''}</td>
					</tr>
					<tr>
						<td class="key">Working Directory</td><td>${config.meObj.workingDirectory ? config.meObj.workingDirectory.name : ''}</td>
					</tr>
					<tr>
						<td class="key">Session ID(s)</td><td>${config.meObj.sessionIds.join('<br>')}</td>
					</tr>
					<tr>
						<td class="key">Groups</td><td>${config.meObj.groups.map(g => g.name).join(', ')}</td>
					</tr>
				</table>

			</div>
		`,
		tabContentAboutStructr: config => `
			<div class="tab-content" id="dashboard-about-structr">

				<table class="props">
					<tr>
						<td class="key">Version</td>
						<td>${config.envInfo.version} ${config.envInfo.build} ${config.envInfo.date} <span id="version-update-info"></span></td>
					</tr>
					<tr>
						<td class="key">Edition</td>
						<td>
							<div class="flex items-center">
								${_Icons.getSvgIcon(_Icons.getIconForEdition(config.envInfo.edition), 16,16,['mr-2'], `Structr ${config.envInfo.edition} Edition`)} (Structr ${config.envInfo.edition} Edition)
							</div>
						</td>
					</tr>
					<tr>
						<td class="key">Modules</td>
						<td>${Object.keys(config.envInfo.modules).join(', ')}</td>
					</tr>
					<tr>
						<td class="key">Licensee</td>
						<td>${config.envInfo.licensee || 'Unlicensed'}</td>
					</tr>
					<tr>
						<td class="key">Host ID</td>
						<td>${config.envInfo.hostId || ''}</td>
					</tr>
					<tr>
						<td class="key">License Start Date</td>
						<td>${config.envInfo.startDate || '-'}</td>
					</tr>
					<tr>
						<td class="key">License End Date</td>
						<td><div class="end-date flex items-center">${config.envInfo.endDate || '-'}</div></td>
					</tr>
					<tr>
						<td class="key">Database Driver</td>
						<td><div class="db-driver flex items-center">${config.databaseDriver}</div></td>
					</tr>
					<tr>
						<td class="key">UUID Format</td>
						<td><div class="allowed-uuid-format flex items-center"></div></td>
					</tr>
					<tr>
						<td class="key">Runtime Info</td>
						<td id="runtime-info">
							<div class="grid gap-x-8" style="grid-template-columns: max-content max-content">
								<div>Available Processors</div>
								<div>${config.envInfo.dashboardInfo.runtimeInfo.availableProcessors}</div>
								<div>Free Memory</div>
								<div>${_Helpers.formatBytes(config.envInfo.dashboardInfo.runtimeInfo.freeMemory)}</div>
								<div>Total Memory</div>
								<div>${_Helpers.formatBytes(config.envInfo.dashboardInfo.runtimeInfo.totalMemory)}</div>
								<div>Max Memory</div>
								<div ${(config.envInfo.dashboardInfo.runtimeInfo.maxMemory < (8 * 1024*1024*1024)) ? 'data-comment="Maximum heap size is smaller than recommended, this can lead to problems with large databases! Please configure AT LEAST 8 GBs of heap memory using -Xmx8g." data-comment-config=\'{"customToggleIcon":"' + _Icons.iconWarningYellowFilled + '"}\'' : ''}>${_Helpers.formatBytes(config.envInfo.dashboardInfo.runtimeInfo.maxMemory)}</div>
							</div>
						</td>
					</tr>
					<tr>
						<td class="key">Scripting Debugger</td>
						<td id="graal-vm-chrome-scripting-debugger"></td>
					</tr>
					<tr>
						<td class="key">Security Warnings</td>
						<td id="security-warnings"></td>
					</tr>
					<tr>
						<td class="key">HTTP Access Statistics</td>
						<td id="http-access-statistics"></td>
					</tr>
				</table>
			</div>
		`,
		tabContentUserDefinedMethods: config => `
			<div class="tab-content" id="dashboard-methods">

			</div>
		`,
		tabContentServerLog: config => `
			<div class="tab-content" id="dashboard-server-log">

				<div class="flex flex-col h-full">

					<div id="dashboard-server-log-controls" class="flex items-center pb-4">

						<div class="editor-settings-popup dropdown-menu darker-shadow-dropdown dropdown-menu-large">
							<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green" data-preferred-position-y="bottom" data-wants-fixed="true">
								${_Icons.getSvgIcon(_Icons.iconSettingsCog, 16, 16, ['mr-2'])}
							</button>

							<div class="dropdown-menu-container" style="display: none;">
								<div class="font-bold pt-4 pb-2">Server Log Settings</div>
								<div class="editor-setting flex items-center p-1">

									<label class="flex-grow">Refresh Interval</label>

									<select id="dashboard-server-log-refresh-interval" class="w-28">
										<option value="10000">10s</option>
										<option value="5000">5s</option>
										<option value="2000">2s</option>
										<option value="1000">1s</option>
										<option value="-1">manual</option>
									</select>
								</div>

								<div class="editor-setting flex items-center p-1">
									<label class="flex-grow">Number of lines</label>
									<input id="dashboard-server-log-lines" type="number" class="w-16">
								</div>

								<div class="editor-setting flex items-center p-1">
									<label class="flex-grow">Truncate lines at</label>
									<input id="dashboard-server-log-truncate-lines" type="number" class="w-16">
								</div>

								<div class="editor-setting flex items-center p-1">
									<label class="flex-grow">Log File</label>
									<select id="dashboard-server-log-file">
									</select>
								</div>
							</div>
						</div>

						<button id="dashboard-server-log-manual-refresh" class="action">Refresh</button>
						<button id="dashboard-server-log-copy" class="action mr-1">Copy</button>
						<button id="dashboard-server-log-download" class="action">Download</button>

						<span id="dashboard-server-log-feedback"></span>
					</div>

					<div class="flex-grow">
						<textarea readonly="readonly" class="h-full w-full"></textarea>
					</div>
				</div>
			</div>
		`,
		tabContentUIConfig: config => `
			<div class="tab-content" id="dashboard-ui-config">

				<div class="flex">

					<div class="flex flex-col mr-12">

						<div class="flex menu-order-container">
							<div class="text-center font-bold w-40 p-4">Main Menu</div>
							<div class="text-center font-bold w-40 p-4">Custom Menu (${_Icons.getSvgIcon(_Icons.iconHamburgerMenu, 8, 8)})</div>
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
		`,
		tabContentEventLog: config => `
			<div class="tab-content" id="dashboard-event-log">

				<div id="event-log-options" class="flex items-center mb-4">

					<label class="mr-1">Filter:</label>
					<select id="event-type-filter" class="mr-8 hover:bg-gray-100 focus:border-gray-666 active:border-green">
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

					<button id="refresh-event-log" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
						${_Icons.getSvgIcon(_Icons.iconRefreshArrows, 16, 16, 'mr-2')} Refresh
					</button>
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
		`,
		tabContentThreads: config => `
			<div class="tab-content" id="dashboard-running-threads">

				<div id="running-threads-options" class="flex items-center mb-4">

					<button id="refresh-running-threads" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
						${_Icons.getSvgIcon(_Icons.iconRefreshArrows, 16, 16, 'mr-2')} Refresh
					</button>

				</div>

				<table class="props">
					<thead>
						<tr>
							<th class="text-left">ID</th>
							<th class="text-left">Name</th>
							<th class="text-left">State</th>
							<th class="text-left">Deadlock detected</th>
							<th class="text-left">CPU Time</th>
							<th class="text-left">Stack</th>
							<th class="text-left" style="width:160px;">Actions</th>
						</tr>
					</thead>
					<tbody id="running-threads-container">

					</tbody>
				</table>
			</div>
		`,
		graalVMChromeScriptingDebugger: config => `
			<div class="mb-4">To access the GraalVM Chrome Scripting Debugger, open a new tab with the following URL in Chrome:</div>

			<div class="mb-4">
				<code class="mb-4">devtools://devtools/bundled/js_app.html?ws=127.0.0.1:4242${config.path}</code>
			</div>

			<div class="mb-4">
				Using the <code>debugger;</code> statement in any of your custom scripts, you can then inspect the source code and debug from there.
			</div>

			<div>For more information, please refer to the <a href="https://www.graalvm.org/tools/chrome-debugger">GraalVM documentation regarding the Chrome Debugger</a></div>
		`,
		tabContentAboutStructrSecurity: config => `
			<div>
				<div class="flex items-center mb-4 font-bold">${_Icons.getSvgIcon(_Icons.iconWarningYellowFilled, 16, 16, 'mr-2')} Warning: The permissions for structr.conf configuration file do not match the expected permissions and pose a security risk in multi-user environments.</div>
				<div class="grid grid-cols-2 max-w-120">
					<span>Expected Permissions</span>
					${config?.expectedPermissions}
					<span>Actual Permissions</span>
					${config?.actualPermissions}
				</div>
				<p>It is strongly recommended to change these permissions to the expected permissions.</p>
			</div>
		`
	}
};