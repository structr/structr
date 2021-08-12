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
	activeTabPrefixKey: 'activeDashboardTabPrefix' + port,

	showScriptingErrorPopupsKey:         'showScriptinErrorPopups' + port,
	showVisibilityFlagsInGrantsTableKey: 'showVisibilityFlagsInResourceAccessGrantsTable' + port,
	favorEditorForContentElementsKey:    'favorEditorForContentElements' + port,
	favorHTMLForDOMNodesKey:             'favorHTMLForDOMNodes' + port,

	init: function() {
		if (!subModule) subModule = LSWrapper.getItem(_Dashboard.activeTabPrefixKey);
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

			let envResponse = await fetch(rootUrl + '/_env');

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

			let meResponse       = await fetch(rootUrl + '/me/ui');
			let meData           = await meResponse.json();

			if (Array.isArray(meData.result)) {
			    meData.result = meData.result[0];
			}
			templateConfig.meObj = meData.result;
			let deployResponse = await fetch('/structr/deploy?mode=test');

			templateConfig.deployServletAvailable = (deployResponse.status == 200);

			templateConfig.zipExportPrefix              = LSWrapper.getItem(_Dashboard.deployment.zipExportPrefixKey);
			templateConfig.zipExportAppendTimestamp     = LSWrapper.getItem(_Dashboard.deployment.zipExportAppendTimestampKey, true);
			templateConfig.zipDataExportAppendTimestamp = LSWrapper.getItem(_Dashboard.deployment.zipDataExportAppendTimestamp, true);

			Structr.fetchHtmlTemplate('dashboard/dashboard', templateConfig, function(html) {

				main.append(html);

				document.getElementById('deployment-file-input').addEventListener('input', () => {
					document.getElementById('deployment-url-input').value = '';
				});

				document.getElementById('deployment-url-input').addEventListener('input', () => {
					document.getElementById('deployment-file-input').value = '';
				});

				Structr.fetchHtmlTemplate('dashboard/dashboard.menu', templateConfig, function(html) {
					functionBar.append(html);

					if (templateConfig.envInfo.databaseService === 'MemoryDatabaseService') {
						Structr.appendInMemoryInfoToElement($('#dashboard-about-structr .db-driver'));
					}

					_Dashboard.eventlog.initializeRuntimeEventLog();
					_Dashboard.serverlog.init();

					_Dashboard.gatherVersionUpdateInfo(templateConfig.envInfo.version, releasesIndexUrl, snapshotsIndexUrl);

					document.querySelectorAll('#function-bar .tabs-menu li a').forEach(function(tabLink) {

						tabLink.addEventListener('click', function(e) {
							e.preventDefault();

							let urlHash = e.target.closest('a').getAttribute('href');

							subModule = urlHash.split(':')[1];
							let targetId = '#dashboard-' + subModule;
							window.location.hash = urlHash;

							_Dashboard.removeActiveClass(document.querySelectorAll('#dashboard .tabs-contents .tab-content'));
							_Dashboard.removeActiveClass(document.querySelectorAll('#function-bar .tabs-menu li'));

							e.target.closest('li').classList.add('active');
							document.querySelector(targetId).classList.add('active');
							LSWrapper.setItem(_Dashboard.activeTabPrefixKey, subModule);

							$(targetId).trigger('show');
						});

						if (tabLink.closest('a').getAttribute('href') === '#' + mainModule + ':' + subModule) {
							// tabLink.closest('li').classList.add('active');
							tabLink.click();
						}
					});

					document.querySelector('#clear-local-storage-on-server').addEventListener('click', function() {
						_Dashboard.clearLocalStorageOnServer(templateConfig.meObj.id);
					});

					_Dashboard.checkLicenseEnd(templateConfig.envInfo, $('#dashboard-about-structr .end-date'));

                    _Dashboard.deployment.init();

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

					let showScriptingErrorPopups = _Dashboard.isShowScriptingErrorPopups();

					let showScriptingErrorPopupsCheckbox = document.querySelector('#dashboard-show-scripting-error-popups');
					if (showScriptingErrorPopupsCheckbox) {
						showScriptingErrorPopupsCheckbox.checked = showScriptingErrorPopups;

						showScriptingErrorPopupsCheckbox.addEventListener('change', () => {
							LSWrapper.setItem(_Dashboard.showScriptingErrorPopupsKey, showScriptingErrorPopupsCheckbox.checked);
						});
					}

					let showVisibilityFlagsInGrantsTable = _Dashboard.isShowVisibilityFlagsInGrantsTable();

					let showVisibilityFlagsInGrantsTableCheckbox = document.querySelector('#dashboard-show-visibility-flags-grants');
					if (showVisibilityFlagsInGrantsTableCheckbox) {
						showVisibilityFlagsInGrantsTableCheckbox.checked = showVisibilityFlagsInGrantsTable;

						showVisibilityFlagsInGrantsTableCheckbox.addEventListener('change', () => {
							LSWrapper.setItem(_Dashboard.showVisibilityFlagsInGrantsTableKey, showVisibilityFlagsInGrantsTableCheckbox.checked);
						});
					}

					let favorEditorForContentElements = _Dashboard.isFavorEditorForContentElements();

					let favorEditorForContentElementsCheckbox = document.querySelector('#dashboard-favor-editors-for-content-elements');
					if (favorEditorForContentElementsCheckbox) {
						favorEditorForContentElementsCheckbox.checked = favorEditorForContentElements;

						favorEditorForContentElementsCheckbox.addEventListener('change', () => {
							LSWrapper.setItem(_Dashboard.favorEditorForContentElementsKey, favorEditorForContentElementsCheckbox.checked);
						});
					}

					let favorHTMLForDOMNodes = _Dashboard.isFavorHTMLForDOMNodes();

					let favorHTMLForDOMNodesCheckbox = document.querySelector('#dashboard-favor-html-tab-for-dom-nodes');
					if (favorHTMLForDOMNodesCheckbox) {
						favorHTMLForDOMNodesCheckbox.checked = favorHTMLForDOMNodes;

						favorHTMLForDOMNodesCheckbox.addEventListener('change', () => {
							LSWrapper.setItem(_Dashboard.favorHTMLForDOMNodesKey, favorHTMLForDOMNodesCheckbox.checked);
						});
					}

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
	isShowScriptingErrorPopups: function() {
		return LSWrapper.getItem(_Dashboard.showScriptingErrorPopupsKey, true);
	},
	isShowVisibilityFlagsInGrantsTable: function() {
		return LSWrapper.getItem(_Dashboard.showVisibilityFlagsInGrantsTableKey, false);
	},
	isFavorEditorForContentElements: () => {
		return LSWrapper.getItem(_Dashboard.favorEditorForContentElementsKey, true);
	},
	isFavorHTMLForDOMNodes: () => {
		return LSWrapper.getItem(_Dashboard.favorHTMLForDOMNodesKey, true);
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

		let maintenanceList = $('<table class="props"></table>').appendTo(container);

		$.get(rootUrl + '/SchemaMethod?schemaNode=&' + Structr.getRequestParameterName('sort') + '=name', function(data) {

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


	elementWithContent: function(parent, tag, content) {

		let element = document.createElement(tag);
		element.innerHTML = content;

		parent.appendChild(element);

		return element;
	},

	deployment: {
    	zipExportPrefixKey:              'zipExportPrefix' + port,
		zipDataExportPrefixKey:          'zipDataExportPrefix' + port,
        zipExportAppendTimestampKey:     'zipExportAppendTimestamp' + port,
		zipDataExportAppendTimestampKey: 'zipDataExportAppendTimestamp' + port,

        init: () => {

    		// App Import

            $('button#do-app-import').on('click', function() {
                _Dashboard.deployment.deploy('import', $('#deployment-source-input').val());
            });

			$('button#do-app-import-from-zip').on('click', function() {
				let filesSelectField = document.getElementById('deployment-file-input');
				if (filesSelectField && filesSelectField.files.length > 0) {
					_Dashboard.deployment.deployFromZIPUpload($('#redirect-url').val(), filesSelectField);
				} else {
					_Dashboard.deployment.deployFromZIPURL($('#redirect-url').val(), $('#deployment-url-input').val());
				}
			});

			// App Export

            $('button#do-app-export').on('click', function() {
                _Dashboard.deployment.deploy('export', $('#app-export-target-input').val());
            });

			$('button#do-app-export-to-zip').on('click', function() {
				_Dashboard.deployment.exportAsZip();
			});


            // Data Import

            $('button#do-data-import').on('click', function() {
                _Dashboard.deployment.deployData('import', $('#data-import-source-input').val());
            });

			$('button#do-data-import-from-zip').on('click', function() {
				let filesSelectField = document.getElementById('data-deployment-file-input');
				if (filesSelectField && filesSelectField.files.length > 0) {
					_Dashboard.deployment.deployDataFromZIPUpload($('#redirect-url').val(), filesSelectField);
				} else {
					_Dashboard.deployment.deployDataFromZIPURL($('#redirect-url').val(), $('#data-deployment-url-input').val());
				}
			});

			// Data Export

            $('button#do-data-export').on('click', function() {
                _Dashboard.deployment.deployData('export', $('#data-export-target-input').val(), $('#data-export-types-input').val());
            });

			$('button#do-data-export-to-zip').on('click', function() {
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

                if (customTypes.length > 0) {

                    for (let typesSelectElemSelector of ['#data-export-types-input', '#zip-data-export-types-input']) {

						let typesSelectElem = $(typesSelectElemSelector);

						typesSelectElem.append(customTypes.reduce(function (html, node) {
							return html + '<option>' + node.name + '</option>';
						}, '<optgroup label="Custom Types">') + '</optgroup>');

						typesSelectElem.append(builtinTypes.reduce(function(html, node) {
							return html + '<option>' + node.name + '</option>';
						}, '<optgroup label="Builtin Types">') + '</optgroup>');

						typesSelectElem.chosen({
							search_contains: true,
							width: 'calc(100% - 2rem)',
							display_selected_options: false,
							hide_results_on_select: false,
							display_disabled_options: false
						}).chosenSortable();

					}
                }
            });

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
            LSWrapper.setItem(_Dashboard.deployment.zipExportPrefixKey, prefix);

            let appendTimestamp = document.getElementById('zip-export-append-timestamp').checked;
            LSWrapper.setItem(_Dashboard.deployment.zipExportAppendTimestampKey, appendTimestamp);

            if (appendTimestamp) {

                let zeroPad = (v) => {
                    return ((v < 10) ? '0' : '') + v;
                };

                let date = new Date();

                prefix += '_' + date.getFullYear() + zeroPad(date.getMonth()+1) + zeroPad(date.getDate()) + '_' + zeroPad(date.getHours()) + zeroPad(date.getMinutes()) + zeroPad(date.getSeconds());
            }

            window.location = '/structr/deploy?name=' + prefix;
        },
		exportDataAsZip: function() {

			let prefix = document.getElementById('zip-data-export-prefix').value;

			let cleaned = prefix.replaceAll(/[^a-zA-Z0-9 _-]/g, '').trim();
			if (cleaned !== prefix) {
				new MessageBuilder().title('Cleaned prefix').info('The given filename prefix was changed to "' + cleaned + '".').requiresConfirmation().show();
				prefix = cleaned;
			}
			LSWrapper.setItem(_Dashboard.deployment.zipDataExportPrefixKey, prefix);

			let appendTimestamp = document.getElementById('zip-data-export-append-timestamp').checked;
			LSWrapper.setItem(_Dashboard.deployment.zipDataExportAppendTimestampKey, appendTimestamp);

			if (appendTimestamp) {

				let zeroPad = (v) => {
					return ((v < 10) ? '0' : '') + v;
				};

				let date = new Date();

				prefix += '_' + date.getFullYear() + zeroPad(date.getMonth()+1) + zeroPad(date.getDate()) + '_' + zeroPad(date.getHours()) + zeroPad(date.getMinutes()) + zeroPad(date.getSeconds());
			}

			window.location = '/structr/deploy?mode=data&name=' + prefix + '&types=' + $('#zip-data-export-types-input').val().join(',');
		},
        deployFromZIPURL: function(redirectUrl, downloadUrl) {

            if (!(downloadUrl && downloadUrl.length)) {
                new MessageBuilder().title('Unable to start app import from URL').warning('Please enter a URL or upload a ZIP file containing the app data.').requiresConfirmation().show();
                return;
            }

            let data = new FormData();
            data.append('redirectUrl', redirectUrl);
            data.append('downloadUrl', downloadUrl);
			data.append('mode', 'app');

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
		deployDataFromZIPURL: function(redirectUrl, downloadUrl) {

			if (!(downloadUrl && downloadUrl.length)) {
				new MessageBuilder().title('Unable to start data import from URL').warning('Please enter a URL or upload a ZIP file containing the ddata.').requiresConfirmation().show();
				return;
			}

			let data = new FormData();
			data.append('redirectUrl', redirectUrl);
			data.append('downloadUrl', downloadUrl);
			data.append('mode', 'data');

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
		deployFromZIPUpload: function(redirectUrl, filesSelectField) {

			if (!(filesSelectField && filesSelectField.files.length)) {
				new MessageBuilder().title('Unable to start app import from ZIP file').warning('Please select a ZIP file containing the app data for upload.').requiresConfirmation().show();
				return;
			}

			let data = new FormData();
			data.append('redirectUrl', redirectUrl);
			data.append('mode', 'app');
			data.append('file', filesSelectField.files[0]);

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
		deployDataFromZIPUpload: function(redirectUrl, filesSelectField) {

			if (!(filesSelectField && filesSelectField.files.length)) {
				new MessageBuilder().title('Unable to start data import from ZIP file').warning('Please select a ZIP file containing the data for upload.').requiresConfirmation().show();
				return;
			}

			let data = new FormData();
			data.append('file', filesSelectField.files[0]);
			data.append('redirectUrl', redirectUrl);
			data.append('mode', 'data');

			$.ajax({
				url: '/structr/deploy',
				method: 'POST',
				processData: false,
				contentType: false,
				data: data,
				statusCode: {
					400: function(data) {
						new MessageBuilder().title('Unable to import data from URL').warning(data.responseText).requiresConfirmation().show();
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
	},

	serverlog: {
        interval: undefined,
        refreshTimeIntervalKey: 'dashboardLogRefreshTimeInterval' + port,
        numberOfLinesKey: 'dashboardNumberOfLines' + port,

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
                let textarea      = event.target;
                let maxScroll     = textarea.scrollHeight - 4;
                let currentScroll = (textarea.scrollTop + $(textarea).height());

                scrollEnabled = currentScroll >= maxScroll;
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

        loadRuntimeEventLog: function() {

            let row    = document.querySelector('#event-log-container');
            let num    = document.querySelector('#event-type-page-size');
            let filter = document.querySelector('#event-type-filter');
            let url    = rootUrl + '/_runtimeEventLog?' + Structr.getRequestParameterName('order') + '=absoluteTimestamp&' + Structr.getRequestParameterName('sort') + '=desc&' + Structr.getRequestParameterName('pageSize') + '=' + num.value;
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
                                    _Dashboard.elementWithContent(tr, 'td', JSON.stringify(data));
                                    break;

                                case 'Scripting':
                                    _Dashboard.elementWithContent(tr, 'td', JSON.stringify(data));
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

                        	if (data.type === 'SchemaMethod' || data.type === 'SchemaProperty') {

								let button = _Dashboard.elementWithContent(buttonContainer, 'button', 'Go to code');
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

                        }

                        row.appendChild(tr);
                    }
                }
            );
        },
	}
};