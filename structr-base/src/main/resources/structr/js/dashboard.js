/*
 * Copyright (C) 2010-2023 Structr GmbH
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

			dashboardUiConfig.envInfo.version = (dashboardUiConfig.envInfo.components['structr'] || dashboardUiConfig.envInfo.components['structr-ui']).version || '';
			dashboardUiConfig.envInfo.build   = (dashboardUiConfig.envInfo.components['structr'] || dashboardUiConfig.envInfo.components['structr-ui']).build   || '';
			dashboardUiConfig.envInfo.date    = (dashboardUiConfig.envInfo.components['structr'] || dashboardUiConfig.envInfo.components['structr-ui']).date    || '';

			dashboardUiConfig.releasesIndexUrl  = dashboardUiConfig.envInfo.availableReleasesUrl;
			dashboardUiConfig.snapshotsIndexUrl = dashboardUiConfig.envInfo.availableSnapshotsUrl;

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
			dashboardUiConfig.zipExportPrefix              = LSWrapper.getItem(_Dashboard.tabs['deployment'].zipExportPrefixKey);
			dashboardUiConfig.zipExportAppendTimestamp     = LSWrapper.getItem(_Dashboard.tabs['deployment'].zipExportAppendTimestampKey, true);
			dashboardUiConfig.zipDataExportAppendTimestamp = LSWrapper.getItem(_Dashboard.tabs['deployment'].zipDataExportAppendTimestamp, true);

			Structr.setMainContainerHTML(_Dashboard.templates.main(dashboardUiConfig));
			Structr.setFunctionBarHTML(_Dashboard.templates.functions());

			_Helpers.activateCommentsInElement(Structr.mainContainer);

			// UISettings.showSettingsForCurrentModule();

			if (dashboardUiConfig.envInfo.databaseService === 'MemoryDatabaseService') {
				Structr.appendInMemoryInfoToElement($('#dashboard-about-structr .db-driver'));
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
				_Dashboard.tabs['about-structr'].gatherVersionUpdateInfo(dashboardUiConfig.envInfo.version, dashboardUiConfig.releasesIndexUrl, dashboardUiConfig.snapshotsIndexUrl);
				_Dashboard.tabs['about-structr'].checkLicenseEnd(dashboardUiConfig.envInfo, $('#dashboard-about-structr .end-date'), { noSpan: true });
			},
			gatherVersionUpdateInfo: async (currentVersion, releasesIndexUrl, snapshotsIndexUrl) => {

				let releaseInfo  = '';
				let snapshotInfo = '';

				if (releasesIndexUrl !== '') {

					// Search for newer releases and store latest version
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

				if (snapshotsIndexUrl !== '') {

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
		'deployment': {
			zipExportPrefixKey:              'zipExportPrefix' + location.port,
			zipDataExportPrefixKey:          'zipDataExportPrefix' + location.port,
			zipExportAppendTimestampKey:     'zipExportAppendTimestamp' + location.port,
			zipDataExportAppendTimestampKey: 'zipDataExportAppendTimestamp' + location.port,

			onShow: async () => {},
			onHide: async () => {},
			init: () => {

				// App Import
				let deploymentFileInput                = document.getElementById('deployment-file-input');
				let deploymentUrlInput                 = document.getElementById('deployment-url-input');
				let deploymentZipConentPathInput       = document.getElementById('deployment-zip-content');

				deploymentFileInput.addEventListener('input', () => {
					deploymentUrlInput.value = '';
				});

				deploymentUrlInput.addEventListener('input', () => {
					deploymentFileInput.value = '';
				});

				document.getElementById('do-app-import').addEventListener('click', () => {
					_Dashboard.tabs['deployment'].deploy('import', document.getElementById('deployment-source-input').value);
				});

				document.getElementById('do-app-import-from-zip').addEventListener('click', () => {

				    let zipContentPath = deploymentZipConentPathInput?.value ?? null;

					if (deploymentFileInput && deploymentFileInput.files.length > 0) {

						_Dashboard.tabs['deployment'].deployFromZIPFileUpload(deploymentFileInput, zipContentPath);

					} else {

						let downloadUrl = deploymentUrlInput.value;

						if (!(downloadUrl && downloadUrl.length)) {
							new WarningMessage().title('Unable to start application import from URL').text('Please enter a URL or upload a ZIP file containing the application export.').requiresConfirmation().allowConfirmAll().show();
						} else {
							_Dashboard.tabs['deployment'].deployFromZIPURL(downloadUrl, zipContentPath);
						}
					}
				});

				// App Export
				document.getElementById('do-app-export').addEventListener('click', () => {
					_Dashboard.tabs['deployment'].deploy('export', document.getElementById('app-export-target-input').value);
				});

				document.getElementById('do-app-export-to-zip').addEventListener('click', () => {
					_Dashboard.tabs['deployment'].exportAsZip();
				});

				// Data Import
				let dataDeploymentFileInput            = document.getElementById('data-deployment-file-input');
				let dataDeploymentUrlInput             = document.getElementById('data-deployment-url-input');


				dataDeploymentFileInput.addEventListener('input', () => {
					dataDeploymentUrlInput.value = '';
				});

				dataDeploymentUrlInput.addEventListener('input', () => {
					dataDeploymentFileInput.value = '';
				});

				document.getElementById('do-data-import').addEventListener('click', () => {
					_Dashboard.tabs['deployment'].deployData('import', document.getElementById('data-import-source-input').value);
				});

				document.getElementById('do-data-import-from-zip').addEventListener('click', () => {

					if (dataDeploymentFileInput && dataDeploymentFileInput.files.length > 0) {

						_Dashboard.tabs['deployment'].deployDataFromZIPFileUpload(dataDeploymentFileInput);

					} else {

						let downloadUrl = dataDeploymentUrlInput.value;

						if (!(downloadUrl && downloadUrl.length)) {

							new WarningMessage().title('Unable to start data import from URL').text('Please enter a URL or upload a ZIP file containing the data export.').requiresConfirmation().allowConfirmAll().show();

						} else {

							_Dashboard.tabs['deployment'].deployDataFromZIPURL(downloadUrl);
						}
					}
				});

				// Data Export
				document.getElementById('do-data-export').addEventListener('click', () => {
					_Dashboard.tabs['deployment'].deployData('export', $('#data-export-target-input').val(), $('#data-export-types-input').val());
				});

				document.getElementById('do-data-export-to-zip').addEventListener('click', () => {
					_Dashboard.tabs['deployment'].exportDataAsZip();
				});


				Command.list('SchemaNode', true, 1000, 1, 'name', 'asc', 'id,name,isBuiltinType', (nodes) => {

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
							width: 'calc(100% - 2rem)',
							dropdownParent: typesSelectElem.parent(),
							dropdownCssClass: ':all:',
							closeOnSelect: false,
							scrollAfterSelect: false
						});
					}
				});
			},
			getDeploymentServletMessage: (message) => {
				return `<span class="deployment-warning" data-comment="The DeplyomentServlet can be activated via the config servlet or via structr.conf." data-comment-config='{"insertAfter":true}'>${message}</span>`;
			},
			deploy: async (mode, location) => {

				if (!(location && location.length)) {
					new WarningMessage().title('Unable to start application ' + mode).text(`Please enter a local directory path for application ${mode}.`).requiresConfirmation().allowConfirmAll().show();
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
					new InfoMessage().title('Cleaned prefix').text(`The given filename prefix was changed to "${cleaned}".`).requiresConfirmation().allowConfirmAll().show();
				}
				return cleaned;
			},
			appendTimeStampToPrefix: (prefix) => {

				let zeroPad = (v) => (((v < 10) ? '0' : '') + v);
				let date    = new Date();

				return `${prefix}_${date.getFullYear()}${zeroPad(date.getMonth() + 1)}${zeroPad(date.getDate())}_${zeroPad(date.getHours())}${zeroPad(date.getMinutes())}${zeroPad(date.getSeconds())}`;
			},
			exportAsZip: () => {

				let prefix = _Dashboard.tabs['deployment'].cleanFileNamePrefix(document.getElementById('zip-export-prefix').value);
				LSWrapper.setItem(_Dashboard.tabs['deployment'].zipExportPrefixKey, prefix);

				let appendTimestamp = document.getElementById('zip-export-append-timestamp').checked;
				LSWrapper.setItem(_Dashboard.tabs['deployment'].zipExportAppendTimestampKey, appendTimestamp);

				if (appendTimestamp) {
					prefix = _Dashboard.tabs['deployment'].appendTimeStampToPrefix(prefix);
				}

				if (prefix === '') {
					new WarningMessage().title('Unable to export application').text('Please enter a prefix or select "Append timestamp"').requiresConfirmation().allowConfirmAll().show();
				} else {
					window.location = Structr.deployRoot + '?name=' + prefix;
				}
			},
			exportDataAsZip: () => {

				let prefix          = _Dashboard.tabs['deployment'].cleanFileNamePrefix(document.getElementById('zip-data-export-prefix').value);
				let appendTimestamp = document.getElementById('zip-data-export-append-timestamp').checked;

				LSWrapper.setItem(_Dashboard.tabs['deployment'].zipDataExportPrefixKey, prefix);
				LSWrapper.setItem(_Dashboard.tabs['deployment'].zipDataExportAppendTimestampKey, appendTimestamp);

				if (appendTimestamp) {
					prefix = _Dashboard.tabs['deployment'].appendTimeStampToPrefix(prefix);
				}

				let zipDataExportTypesSelect = document.getElementById('zip-data-export-types-input');
				let types                    = Array.from(zipDataExportTypesSelect.selectedOptions).map(o => o.value).join(',');

				if (types === '') {
					new WarningMessage().title('Unable to start data export').text('Please select at least one data type.').requiresConfirmation().allowConfirmAll().show();
				} else if (prefix === '') {
					new WarningMessage().title('Unable to start data export').text('Please enter a prefix or select "Append timestamp"').requiresConfirmation().allowConfirmAll().show();
				} else {
					window.location = `${Structr.deployRoot}?mode=data&name=${prefix}&types=${types}`;
				}
			},
			deployFromZIPURL: async (downloadUrl, zipContentPath) => {

				let formData = new FormData();
				formData.append('redirectUrl', window.location.pathname);
				formData.append('zipContentPath', zipContentPath);
				formData.append('downloadUrl', downloadUrl);
				formData.append('mode', 'app');

				let response = await fetch(Structr.deployRoot, {
					method: 'POST',
					body: formData
				});

				if (!response.ok && response.status === 400) {
					let responseText = await response.text();
					new WarningMessage().title('Unable to import app from URL').text(responseText).requiresConfirmation().allowConfirmAll().show();
				}
			},
			deployDataFromZIPURL: async (downloadUrl, zipContentPath) => {

				let formData = new FormData();
				formData.append('redirectUrl', window.location.pathname);
				formData.append('downloadUrl', downloadUrl);
				formData.append('zipContentPath', zipContentPath);
				formData.append('mode', 'data');

				let response = await fetch(Structr.deployRoot, {
					method: 'POST',
					body: formData
				});

				if (!response.ok && response.status === 400) {
					let responseText = await response.text();
					new WarningMessage().title('Unable to import app from ZIP URL').text(responseText).requiresConfirmation().allowConfirmAll().show();
				}
			},
			deployFromZIPFileUpload: async (filesSelectField, zipContentPath) => {

				let formData = new FormData();
				formData.append('redirectUrl', window.location.pathname);
				formData.append('zipContentPath', zipContentPath);
				formData.append('mode', 'app');
				formData.append('file', filesSelectField.files[0]);

				let response = await fetch(Structr.deployRoot, {
					method: 'POST',
					body: formData
				});

				if (!response.ok && response.status === 400) {
					let responseText = await response.text();
					new WarningMessage().title('Unable to import app from uploaded ZIP').text(responseText).requiresConfirmation().allowConfirmAll().show();
				}
			},
			deployDataFromZIPFileUpload: async (filesSelectField, zipContentPath) => {

				let formData = new FormData();
				formData.append('file', filesSelectField.files[0]);
				formData.append('zipContentPath', zipContentPath);
				formData.append('redirectUrl', window.location.pathname);
				formData.append('mode', 'data');

				let response = await fetch(Structr.deployRoot, {
					method: 'POST',
					body: formData
				});

				if (!response.ok && response.status === 400) {
					let responseText = await response.text();
					new WarningMessage().title('Unable to import app from uploaded ZIP').text(responseText).requiresConfirmation().allowConfirmAll().show();
				}
			},
			deployData: async (mode, location, types) => {

				if (!(location && location.length)) {
					new WarningMessage().title(`Unable to start data ${mode}`).text(`Please enter a local directory path for data ${mode}.`).requiresConfirmation().allowConfirmAll().show();
					return;
				}

				let data = {
					mode: mode
				};

				if (mode === 'import') {

					data['source'] = location;

				} else if (mode === 'export') {

					data['target'] = location;

					if (types && types.length) {
						data['types'] = types.join(',');
					} else {
						new WarningMessage().title(`Unable to ${mode} data`).text('Please select at least one data type.').requiresConfirmation().allowConfirmAll().show();
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
		'global-schema-methods': {
			init: () => {
				_Dashboard.tabs['global-schema-methods'].appendGlobalSchemaMethods();
			},
			appendGlobalSchemaMethods: async () => {

				let container = document.querySelector('#dashboard-global-schema-methods');
				_Helpers.fastRemoveAllChildren(container);
				let response  = await fetch(`${Structr.rootUrl}SchemaMethod?schemaNode=&${Structr.getRequestParameterName('sort')}=name`);

				if (response.ok) {

					let data = await response.json();

					if (data.result.length === 0) {

						container.textContent = 'No global schema methods.';

					} else {

						let maintenanceList = _Helpers.createSingleDOMElementFromHTML(`
							<table class="props">
								${data.result.map(method => `
									<tr class="global-method">
										<td><span class="method-name">${method.name}</span></td>
										<td><button id="run-${method.id}" class="action button">Run now</button></td>
									</tr>
								`).join('')}
							</table>
						`);
						container.appendChild(maintenanceList);

						for (let method of data.result) {

							maintenanceList.querySelector('button#run-' + method.id).addEventListener('click', () => {
								_Code.runSchemaMethod(method);
							});
						}
					}
				}
			},
			onShow: async () => {},
			onHide: async () => {}
		},
		'server-log': {
			refreshTimeIntervalKey: 'dashboardLogRefreshTimeInterval' + location.port,
			numberOfLinesKey: 'dashboardNumberOfLines' + location.port,
			truncateLinesAfterKey: 'dashboardTruncateLinesAfter' + location.port,
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
			getTruncateLinesAfterInput: () => document.querySelector('#dashboard-server-truncate-lines'),
			getNumberOfLinesInput: () => document.querySelector('#dashboard-server-log-lines'),
			getServerLogTextarea: () => document.querySelector('#dashboard-server-log textarea'),
			getManualRefreshButton: () => document.querySelector('#dashboard-server-log-manual-refresh'),
			setFeedback: (message) => {
				let el = document.querySelector('#dashboard-server-log-feedback');
				if (el) {
					// textContent creates a new node, not 100% efficient if there already is a node... but gc should sort that out
					el.textContent = message;
				}
			},
			init: () => {

				let textarea = _Dashboard.tabs['server-log'].getServerLogTextarea();

				let initServerLogInput = (element, lsKey, defaultValue, successFn) => {

					element.value = LSWrapper.getItem(lsKey, defaultValue);

					element.addEventListener('change', (e) => {

						LSWrapper.setItem(lsKey, e.target.value);

						successFn?.();

						_Helpers.blinkGreen(e.target);
					});
				};

				initServerLogInput(_Dashboard.tabs['server-log'].getTimeIntervalSelect(), _Dashboard.tabs['server-log'].refreshTimeIntervalKey, 1000, _Dashboard.tabs['server-log'].updateRefreshInterval);
				initServerLogInput(_Dashboard.tabs['server-log'].getNumberOfLinesInput(), _Dashboard.tabs['server-log'].numberOfLinesKey, 1000);
				initServerLogInput(_Dashboard.tabs['server-log'].getTruncateLinesAfterInput(), _Dashboard.tabs['server-log'].truncateLinesAfterKey, -1);

				document.querySelector('#dashboard-server-log-copy').addEventListener('click', async () => {
					await navigator.clipboard.writeText(textarea.textContent);
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
					if (true === _Dashboard.tabs['server-log'].scrollEnabled) {
						_Dashboard.tabs['server-log'].getServerLogTextarea()?.parentNode?.classList.remove('textarea-is-scrolled');
					} else {
						_Dashboard.tabs['server-log'].getServerLogTextarea()?.parentNode?.classList.add('textarea-is-scrolled');
					}
				});
			},
			updateLog: () => {

				if (false === _Dashboard.tabs['server-log'].textAreaHasFocus) {

					_Dashboard.tabs['server-log'].setFeedback('Refreshing server log...');

					Command.getServerLogSnapshot(_Dashboard.tabs['server-log'].getNumberOfLinesInput().value, _Dashboard.tabs['server-log'].getTruncateLinesAfterInput().value).then(log => {

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
			updateRefreshInterval: () => {

				_Dashboard.tabs['server-log'].stop();

				let timeInMs            = _Dashboard.tabs['server-log'].getTimeIntervalSelect().value;
				let manualRefreshButton = _Dashboard.tabs['server-log'].getManualRefreshButton();

				if (timeInMs > 0) {

					manualRefreshButton.classList.add('hidden');
					_Dashboard.tabs['server-log'].intervalID = window.setInterval(_Dashboard.tabs['server-log'].updateLog, timeInMs);

				} else {

					manualRefreshButton.classList.remove('hidden');
				}
			},
			start: () => {
				_Dashboard.tabs['server-log'].updateLog();
				_Dashboard.tabs['server-log'].updateRefreshInterval();
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

									Command.get(data.id, null, (obj) => {

										let pathToOpen = (obj.schemaNode) ? `/root/custom/${obj.schemaNode.id}/methods/${obj.id}` : `/globals/${obj.id}`;

										window.location.href = '#code';
										window.setTimeout(() => {
											_Code.findAndOpenNode(pathToOpen, false);
										}, 1000);
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
				_Dashboard.tabs['ui-config'].showMainMenuConfiguration(templateConfig.envInfo.mainMenu);
				_Dashboard.tabs['ui-config'].showConfigurableSettings();
				_Dashboard.tabs['ui-config'].handleResetConfiguration(templateConfig.meObj.id);
			},

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

					// only show menu items that are allowed in the current configuration
					if (Structr.availableMenuItems.includes(menuitem.dataset.name)) {

						// account for missing modules because of license
						if (menuitem.style.display !== 'none') {
							let n = document.createElement('div');
							n.classList.add('menu-item');
							n.textContent = menuitem.dataset.name;
							n.dataset.name = menuitem.dataset.name;
							subMenuConfigContainer.appendChild(n);
						}
					}
				}

				for (let mainMenuItem of userConfigMenu.main) {
					if (Structr.availableMenuItems.includes(mainMenuItem)) {
						let child = subMenuConfigContainer.querySelector('div[data-name="' + mainMenuItem + '"]');
						if (child) {
							mainMenuConfigContainer.appendChild(child);
						}
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

					<div class="tab-content active" id="dashboard-about-me">
						<table class="props">
							<tr><td class="key">User Name</td><td>${config.meObj.name}</td></tr>
							<tr><td class="key">ID</td><td>${config.meObj.id}</td></tr>
							<tr><td class="key">E-Mail</td><td>${config.meObj.eMail || ''}</td></tr>
							<tr><td class="key">Working Directory</td><td>${config.meObj.workingDirectory ? config.meObj.workingDirectory.name : ''}</td></tr>
							<tr><td class="key">Session ID(s)</td><td>${config.meObj.sessionIds.join('<br>')}</td></tr>
							<tr><td class="key">Groups</td><td>${config.meObj.groups.map(g => g.name).join(', ')}</td></tr>
						</table>

					</div>

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
								${(config.deploymentServletAvailable ? '' : _Dashboard.tabs.deployment.getDeploymentServletMessage('Deployment via URL is not possible because <code>DeploymentServlet</code> is not active.'))}
								<div>
									<input type="text" id="deployment-url-input" placeholder="Download URL of ZIP file for app import" name="downloadUrl" ${(config.deploymentServletAvailable ? '' : 'disabled')}>
									<input type="text" id="deployment-zip-content" placeholder="Path to the webapp folder inside the ZIP file, leave blank for default" name="downloadUrl" ${(config.deploymentServletAvailable ? '' : 'disabled')}>
									<input type="file" id="deployment-file-input" placeholder="Upload ZIP file" ${(config.deploymentServletAvailable ? '' : 'disabled')}>
									<button class="action ${(config.deploymentServletAvailable ? '' : 'disabled')}" ${(config.deploymentServletAvailable ? '' : 'disabled')} id="do-app-import-from-zip">Import app from ZIP file</button>
								</div>
							</div>

							<div>
								<h3>Export application and download as ZIP file</h3>
								${(config.deploymentServletAvailable ? '' : _Dashboard.tabs.deployment.getDeploymentServletMessage('Export and download as ZIP file is not possible because <code>DeploymentServlet</code> is not active.'))}
								<div>
									<input type="text" id="zip-export-prefix" placeholder="ZIP File prefix" ${(config.deploymentServletAvailable ? '' : 'disabled')} value="${(config.zipExportPrefix || 'webapp')}">
									<label class="checkbox-label"><input type="checkbox" id="zip-export-append-timestamp" ${(config.deploymentServletAvailable ? '' : 'disabled')} ${(config.zipExportAppendTimestamp ? 'checked' : '')}> Append timestamp</label>
									<button class="action ${(config.deploymentServletAvailable ? '' : 'disabled')}" ${(config.deploymentServletAvailable ? '' : 'disabled')} id="do-app-export-to-zip">Export and download app as ZIP file</button>
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
									<button class="action" id="do-data-import">Import data from server directory</button>
								</div>
							</div>

							<div>
								<h3>Data export to server directory</h3>
								<div>
									<input type="text" id="data-export-target-input" placeholder="Server directory path for data export">
									<div>
										<select id="data-export-types-input" class="hide-selected-options" data-placeholder="Please select data type(s) to export" multiple="multiple">
											<optgroup label="Custom Types" class="custom-types"></optgroup>
											<optgroup label="Builtin Types" class="builtin-types"></optgroup>
										</select>
									</div>
									<button class="action" id="do-data-export">Export data to server directory</button>
								</div>
							</div>

							<div>
								<h3>Import data from URL or upload a ZIP file</h3>
								${(config.deploymentServletAvailable ? '' : _Dashboard.tabs.deployment.getDeploymentServletMessage('Deployment via URL is not possible because <code>DeploymentServlet</code> is not active.'))}
								<div>
									<input type="text" id="data-deployment-url-input" placeholder="Download URL of ZIP file for data import" name="downloadUrl" ${(config.deploymentServletAvailable ? '' : 'disabled')}>
									<input type="file" id="data-deployment-file-input" placeholder="Upload ZIP file" ${(config.deploymentServletAvailable ? '' : 'disabled')}>
									<button id="do-data-import-from-zip" class="action ${(config.deploymentServletAvailable ? '' : 'disabled')}" ${(config.deploymentServletAvailable ? '' : 'disabled')}>Import data from ZIP file</button>
								</div>
							</div>

							<div>
								<h3>Export data and download as ZIP file</h3>
								${(config.deploymentServletAvailable ? '' : _Dashboard.tabs.deployment.getDeploymentServletMessage('Export and download data as ZIP file is not possible because <code>DeploymentServlet</code> is not active.'))}
								<div>
									<input type="text" id="zip-data-export-prefix" placeholder="ZIP file prefix" ${(config.deploymentServletAvailable ? '' : 'disabled')} value="${(config.zipDataExportPrefix || 'data')}">
									<select id="zip-data-export-types-input" class="hide-selected-options" data-placeholder="Please select data type(s) to export" multiple="multiple">
										<optgroup label="Custom Types" class="custom-types"></optgroup>
										<optgroup label="Builtin Types" class="builtin-types"></optgroup>
									</select>
									<label class="checkbox-label"><input type="checkbox" id="zip-data-export-append-timestamp" ${(config.deploymentServletAvailable ? '' : 'disabled')} ${(config.zipDataExportAppendTimestamp ? 'checked' : '')}> Append timestamp</label>
									<button id="do-data-export-to-zip" class="action ${(config.deploymentServletAvailable ? '' : 'disabled')}" ${(config.deploymentServletAvailable ? '' : 'disabled')}>Export and download data as ZIP file</button>
								</div>
							</div>
						</div>
					</div>

					<div class="tab-content" id="dashboard-global-schema-methods">

					</div>

					<div class="tab-content" id="dashboard-server-log">

						<div class="flex flex-col h-full">

							<div id="dashboard-server-log-controls" class="pb-4">

								<div class="editor-settings-popup dropdown-menu darker-shadow-dropdown dropdown-menu-large">
									<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green" data-preferred-position-y="bottom" data-wants-fixed="true">
										${_Icons.getSvgIcon(_Icons.iconSettingsCog)}
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
											<input id="dashboard-server-truncate-lines" type="number" class="w-16">
										</div>
									</div>
								</div>

								<button id="dashboard-server-log-manual-refresh" class="action">Refresh</button>
								<button id="dashboard-server-log-copy" class="action">Copy</button>

								<span id="dashboard-server-log-feedback"></span>
							</div>

							<div class="flex-grow">
								<textarea readonly="readonly" class="h-full w-full"></textarea>
							</div>
						</div>
					</div>

					<div class="tab-content" id="dashboard-sysinfo">

					</div>

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
					<a href="#dashboard:running-threads">Threads</a>
				</li>
				<li>
					<a href="#dashboard:ui-config">UI Settings</a>
				</li>
			</ul>
		`
	}
};