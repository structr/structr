/*
 * Copyright (C) 2010-2026 Structr GmbH
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

	Structr.registerModule(Importer);

	$(document).on('click', '.import-job-action', function (e) {
		e.preventDefault();

		let mode  = $(this).data('action');
		let jobId = $(this).data('jobId');

		Command.fileImport(mode, jobId, Importer.updateJobTable);

		return false;
	});

});

let Importer = {
	_moduleName: 'importer',
	appDataXMLKey: 'xml-import-config',
	appDataCSVKey: 'csv-import-config',
	timeout: undefined,
	customTypesOnly: true,
	schemaTypeCachePopulated: false,
	schemaTypeCache: {
		nodeTypes: [],
		relTypes: [],
		graphTypes: []
	},
	init: () => {},
	resize: () => {},
	onload: () => {

		Importer.init();

		Structr.setMainContainerHTML(Importer.templates.main());
		Structr.setFunctionBarHTML(Importer.templates.functions());

		UISettings.showSettingsForCurrentModule();

		$('#importer-main .refresh').click(function () {
			Importer.updateJobTable();
		});

		$('#cancel-all-queued-after').click(function () {

			let jobId = parseInt($('#cancel-all-queued-after-job-id').val());

			if (isNaN(jobId)) {
				new WarningMessage().text("Unable to parse job id").show();
			} else {
				Command.fileImport('cancelAllAfter', jobId, () => {

					$('#cancel-all-queued-after-job-id').val('');
					Importer.updateJobTable();
				});
			}
		});

		Importer.updateJobTable();

		Structr.mainMenu.unblock(100);
	},
	unload: () => {
		Importer.schemaTypeCachePopulated = false;
	},
	isShowNotifications: () => {
		return UISettings.getValueForSetting(UISettings.settingGroups.importer.settings.showNotificationsKey);
	},
	updateJobTable: () => {

		window.clearTimeout(Importer.timeout);

		window.setTimeout(function () {

			Command.fileImport('list', null, function (jobs) {

				let table = $('#importer-jobs-table');
				let tbody = $('tbody', table);
				tbody.empty();

				let imports = jobs[0].imports;

				if (imports.length) {

					imports.forEach(function (job) {
						tbody.append(Importer.createRowForJob(job));
					});

				} else {
					tbody.append('<td colspan=9>No import jobs</td>');
				}

			});

			window.clearTimeout(Importer.timeout);

		}, 250);

	},
	createRowForJob: (job) => {
		return $(`<tr><td>${job.jobId}</td><td>${job.jobtype}</td><td>${job.username}</td>${Importer.createJobInfoHTML(job)}<td>${job.status}</td><td>${Importer.createActionButtons(job)}</td></tr>`);
	},
	createJobInfoHTML: (job) => {
		switch (job.jobtype) {
			case 'XML':
			case 'CSV':
				return `<td>${job.fileUuid}</td><td>${job.filepath}</td><td>${job.filesize}</td><td>${job.processedChunks}</td>`;

			case 'SCRIPT':
				return `<td colspan=4 class="${job.jobName.length === 0 ? 'placeholderText' : ''}">${job.jobName}</td>`;

			default:
				return '<td colspan=4 class="placeholderText"> - not applicable - </td>';
		}
	},
	createActionButtons: (job) => {
		let actionHtml = '';

		switch (job.jobtype) {
			case 'XML':
			case 'CSV':
				switch (job.status) {
					case 'QUEUED':
						actionHtml += Importer.createActionButton('start', job.jobId, 'Start');
						actionHtml += Importer.createActionButton('cancel', job.jobId, 'Cancel');
						break;

					case 'PAUSED':
						actionHtml += Importer.createActionButton('resume', job.jobId, 'Resume');
						actionHtml += Importer.createActionButton('abort', job.jobId, 'Abort');
						break;

					case 'RUNNING':
						actionHtml += Importer.createActionButton('pause', job.jobId, 'Pause');
						actionHtml += Importer.createActionButton('abort', job.jobId, 'Abort');
						break;
				}
				break;

			default:
				if (job.status === 'QUEUED') {
					actionHtml += Importer.createActionButton('cancel', job.jobId, 'Cancel');
				}
		}

		return actionHtml;
	},
	createActionButton: function (action, jobId, content) {
		return `<button class="import-job-action" data-action="${action}" data-job-id="${jobId}">${content}</button>`;
	},
	updateConfigSelector: (elem, importType) => {

		Command.getApplicationConfigurationDataNodesGroupedByUser(importType + '-import', (grouped) => {

			elem.empty();
			elem.append(`
				<option selected value="" disabled>--- Select configuration to load ---</option>
				${grouped.map(group => `<optgroup label="${group.label}">${group.configs.map(cfg => `<option value="${cfg.id}">${cfg.name}</option>`).join('')}</optgroup>`).join('')}
			`);

			Importer.configSelectorChangeHandler(elem, importType);
		});

	},
	saveImportConfiguration: (elem, importType, configuration) => {

		let inputElem = $('#' + importType + '-config-name-input');
		let name = inputElem.val();

		if (name && name.length) {

			Command.createApplicationConfigurationDataNode(`${importType}-import`, name, JSON.stringify(configuration), (data) => {

				if (!data.error) {

					new SuccessMessage().text('Import Configuration saved').show();

					Importer.updateConfigSelector(elem, importType);
					inputElem.val('');

					_Helpers.blinkGreen(elem);

				} else {
					new ErrorMessage().title(data.error).text(data.message).show();
				}
			});

		} else {
			Structr.error('Import configuration layout name is required.');
		}
	},
	updateImportConfiguration: (elem, configuration) => {

		let selectedConfig = elem.val();

		Command.setProperty(selectedConfig, 'content', JSON.stringify(configuration), false, (data) => {

			if (!data.error) {

				new SuccessMessage().text("Import Configuration saved").show();

				_Helpers.blinkGreen(elem);

			} else {

				new ErrorMessage().title(data.error).text(data.message).show();
			}
		});
	},
	loadImportConfiguration: (elem, callback) => {

		try {
			Command.getApplicationConfigurationDataNode(elem.val(), callback);
		} catch (e) {
			Structr.error('Error parsing configuration, please see browser log');
			console.log(e);
		}
	},
	configSelectorChangeHandler: (selectElement, importType) => {

		let updateImportConfigButton = document.querySelector(`#update-${importType}-config-button`);
		let loadImportConfigButton   = document.querySelector(`#load-${importType}-config-button`);
		let deleteImportConfigButton = document.querySelector(`#delete-${importType}-config-button`);
		let selectedOption           = selectElement[0].querySelector(':checked:not(:disabled)');

		if (!selectedOption) {

			_Helpers.disableElements(true, updateImportConfigButton, loadImportConfigButton, deleteImportConfigButton);

		} else {

			_Helpers.enableElement(loadImportConfigButton);

			let username = selectedOption.closest('optgroup').label;
			_Helpers.disableElements((username !== 'null' && username !== StructrWS.me.username), updateImportConfigButton, deleteImportConfigButton);
		}
	},
	deleteImportConfiguration: (elem, importType) => {

		Command.deleteNode(elem.val(), false, () => {
			Importer.updateConfigSelector(elem, importType);
			_Helpers.blinkGreen(elem);
		});
	},
	importCSVDialog: (file) => {

		// define data structure here to be able to use it in callbacks etc. below
		let mixedMappingConfig = {
			availableProperties: {},
			mappedTypes: {}
		};

		Importer.clearSchemaTypeCache();

		let { dialogText, dialogMeta } = _Dialogs.custom.openDialog(`Import CSV from ${file.name}`, Importer.unload);

		let startButton = _Dialogs.custom.prependCustomDialogButton('<button class="action disabled" disabled id="start-import">Start import</button>');

		dialogMeta.insertAdjacentHTML('beforeend', Importer.templates.dialogConfigurations({type: 'csv'}));

		let importConfigSelector = $('#load-csv-config-selector');
		importConfigSelector.on('change', function () {
			Importer.configSelectorChangeHandler(importConfigSelector, 'csv');
		});
		Importer.configSelectorChangeHandler(importConfigSelector, 'csv');

		Importer.updateConfigSelector(importConfigSelector, 'csv');

		$('#load-csv-config-button').on('click', function() {

			Importer.loadImportConfiguration(importConfigSelector, function(data) {

				if (data && data.content) {

					let config = JSON.parse(data.content).config;

					if (!config.version) {
						// initial version, no reversal needed

					} else if (config.version === 2) {

						let reversedTransforms = {};
						for (let k in config.transforms) {
							reversedTransforms[config.mappings[k]] = config.transforms[k];
						}

						config.transforms = reversedTransforms;

						// New version - reverse mappings/transforms so we can display them
						let reversedMappings = {};

						for (let k in config.mappings) {
							reversedMappings[config.mappings[k]] = k;
						}

						config.mappings = reversedMappings;
					}

					$('#delimiter').val(config.delimiter);
					$('#quote-char').val(config.quoteChar);
					$('#record-separator').val(config.recordSeparator);
					$('#commit-interval').val(config.commitInterval);
					$('#rfc4180-mode').prop('checked', config.rfc4180Mode === true);
					$('#strict-quotes').prop('checked', config.strictQuotes === true);
					$('#ignore-invalid').prop('checked', config.ignoreInvalid === true);
					$('#distinct').prop('checked', config.distinct === true);
					$('#range').val(config.range);

					let importType = config.importType || "node";

					let typeInfo = Importer.schemaTypeCache[importType + 'Types'].filter((t) => {
						return t.name === config.targetType;
					});
					if (importType !== 'graph') {

						if (typeInfo.length > 0) {

							Importer.customTypesOnly = !typeInfo[0].isBuiltinType;

						} else {

							new WarningMessage().text(`Type ${config.targetType} from loaded configuration does not exist. This may be due to an outdated configuration.`).show();
							Importer.customTypesOnly = true;
						}
					}

					// update storage data structure before triggering events..
					if (importType === 'graph') {
						mixedMappingConfig.mappedTypes = config.mixedMappingConfig;
					}

					$('input[name=import-type][value=' + importType + ']').prop('checked', 'checked').trigger('change');

					$('#target-type-select').val(config.targetType).trigger('change', [config]);
				}
			});
		});

		$('#update-csv-config-button').on('click', function() {

			let configInfo = Importer.collectCSVImportConfigurationInfo(mixedMappingConfig.mappedTypes);

			if (configInfo.errors.length > 0) {

				for (let e of configInfo.errors) {
					new ErrorMessage().title(e.title).text(e.message).show();
				}

			} else {

				Importer.updateImportConfiguration(importConfigSelector, configInfo);
			}
		});

		$('#save-csv-config-button').on('click', function() {

			let configInfo = Importer.collectCSVImportConfigurationInfo(mixedMappingConfig.mappedTypes);

			if (configInfo.errors.length > 0) {

				for (let e of configInfo.errors){
					new ErrorMessage().title(e.title).text(e.message).show();
				}

			} else {

				Importer.saveImportConfiguration(importConfigSelector, 'csv', configInfo);
			}
		});

		$('#delete-csv-config-button').on('click', function() {
			Importer.deleteImportConfiguration(importConfigSelector, 'csv');
		});

		// load first lines to display a sample of the data
		fetch(`${Structr.rootUrl}File/${file.id}/getFirstLines`, { method: 'POST' }).then(response => response.json()).then(data => {

			if (data && data.result) {

				let results   = Papa.parse(data.result.lines);
				let delim     = results.meta.delimiter;
				let qc        = data.result.lines.substring(0,1);
				dialogText.insertAdjacentHTML('beforeend', Importer.templates.dialogCSV({ data: data, delim: delim, qc: qc, importType: "node" }));

				Importer.formatImportTypeSelectorDialog(file, mixedMappingConfig);

				$('input[name=import-type]').off('change').on('change', function() {

					// call self on change
					Importer.formatImportTypeSelectorDialog(file, mixedMappingConfig);
				});
			}
		});
	},
	formatImportTypeSelectorDialog: (file, mixedMappingConfig) => {

		let importType = $('input[name=import-type]:checked').val();

		$('#import-dialog-type-container').empty();
		$('#import-dialog-type-container').append(Importer.templates['dialogTargetTypeSelect_' + importType]());

		switch (importType) {

			case 'node':
			case 'rel':
				Importer.formatNodeOrRelImportDialog(file);
				break;

			case 'graph':
				Importer.formatMixedImportDialog(file, mixedMappingConfig);
				break;
		}
	},
	formatNodeOrRelImportDialog: (file) => {

		let targetTypeSelector = $('#target-type-select');

		Importer.updateSchemaTypeCache().then(ignore => {
			Importer.updateSchemaTypeSelector(targetTypeSelector);
		});

		targetTypeSelector.off('change').on('change', function(e, data) { Importer.updateMapping(file, data); });
		$(".import-option").off('change').on('change', function(e, data) { Importer.updateMapping(file, data); });

		let customOnlyCheckbox = $('input#target-type-custom-only');

		if (Importer.customTypesOnly) {
			customOnlyCheckbox.prop('checked', true);
		}

		$('#property-select').empty();
		$('#start-import').off('click');

		customOnlyCheckbox.off('change').on('change', function() {
			Importer.customTypesOnly = $(this).prop('checked');
			Importer.updateSchemaTypeSelector(targetTypeSelector);
			$('#property-select').empty();
			$('#start-import').off('click');
		});

		Importer.updateSchemaTypeSelector(targetTypeSelector);

	},
	updateSchemaTypeCache: async () => {

		return new Promise((resolve) => {

			if (!Importer.schemaTypeCachePopulated) {

				_Helpers.getSchemaInformationPromise().then(schemaData => {

					Importer.clearSchemaTypeCache();

					for (let res of schemaData) {

						if (res.isServiceClass === false) {

							if (res.isRel) {

								Importer.schemaTypeCache['relTypes'].push(res);

							} else {

								Importer.schemaTypeCache['graphTypes'].push(res);
								Importer.schemaTypeCache['nodeTypes'].push(res);
							}
						}
					}

					Importer.schemaTypeCachePopulated = true;

					resolve('success');
				});

			} else {

				resolve('success_from_cache');
			}
		});
	},
	updateSchemaTypeSelector: (typeSelect) => {

		let importType = $('input[name=import-type]:checked').val();

		$('option[disabled!=disabled]', typeSelect).remove();
		typeSelect.val("");

		let data = Importer.getSchemaTypeSelectorData(importType);

		typeSelect.append(data.map(name => `<option value="${name}">${name}</option>`).join(''));
	},
	getSchemaTypeSelectorData: (importType = '') => {

		let allTypeData = Importer.schemaTypeCache[importType + 'Types'];

		if (Importer.customTypesOnly === true) {
			allTypeData = allTypeData.filter(t => t.isBuiltin === false);
		}

		return allTypeData.map(t => t.name).sort();
	},
	clearSchemaTypeCache: () => {

		Importer.schemaTypeCache = {
			nodeTypes: [],
			relTypes: [],
			graphTypes: []
		};
	},
	updateMapping: (file, data) => {

		let targetTypeSelector = $('#target-type-select');
		let propertySelector   = $('#property-select');
		let type               = targetTypeSelector.val();

		// dont do anything if there is no type set
		if (!type) {
			return;
		}

		// clear current mapping list
		propertySelector.empty();

		fetch(`${Structr.rootUrl}File/${file.id}/getCSVHeaders`, {
			method: 'POST',
			body: JSON.stringify({
				delimiter: $('#delimiter').val(),
				quoteChar: $('#quote-char').val(),
				recordSeparator: $('#record-separator').val()
			})
		}).then(response => response.json()).then(csvHeaders => {

			propertySelector.append('<h3>Select Mapping</h3>');
			propertySelector.append('<div class="attr-mapping"><table><thead><tr><th>Column name</th><th class="transform-head">Transformation (optional)</th><th></th></tr></thead><tbody id="row-container"></tbody></table></div>');

			let helpText = 'Specify optional StructrScript expression here to transform the input value.<br>The data key is &quot;input&quot; and the return value of the expression will be imported.<br><br><b>Example</b>: capitalize(input)';
			_Helpers.appendInfoTextToElement({
				text: helpText,
				element: $('th.transform-head', propertySelector),
				css: {
					marginLeft: "2px"
				}
			});

			if (csvHeaders && csvHeaders.result && csvHeaders.result.headers) {

				let names      = [];
				let typeConfig = {};

				if (data) {
					typeConfig['properties'] = data.mappings;
					typeConfig['mappings']   = data.transforms;
				};

				Importer.displayImportPropertyMapping(type, csvHeaders.result.headers, $('#row-container'), names, true, typeConfig, () => {

					_Helpers.enableElement($('#start-import')[0]);
					$('#start-import').off('click').on('click', function() {

						let configInfo = Importer.collectCSVImportConfigurationInfo();
						let allowImport = (configInfo.errors.length === 0);

						if (!allowImport) {

							configInfo.errors.forEach(function(e) {
								new ErrorMessage().title(e.title).text(e.message).show();
							});

						} else {

							fetch(`${Structr.rootUrl}File/${file.id}/doCSVImport`, {
								method: 'POST',
								body: JSON.stringify(configInfo.config)
							});
						}
					});
				});
			}
		});
	},
	collectCSVImportConfigurationInfo: function (mixedMappings) {

		let info = {
			errors: []
		};

		// collect mappings and transforms
		let mappings   = {};
		let transforms = {};

		$('select.attr-mapping').each(function(i, elem) {

			let e     = $(elem);
			let name  = e.prop('name');
			let value = e.val();

			if (value && value.length) {
				if (!mappings[value]) {
					mappings[value] = name;
				} else {
					info.errors.push({
						title: "Import Configuration Error",
						message: "Duplicate mapping found: <strong>" + value + "</strong>"
					});
				}

				let transform = $('input#transform' + i).val();
				if (transform && transform.length) {
					transforms[value] = transform;
				}
			}
		});

		let importType = $('input[name=import-type]:checked').val();
		if (importType === 'rel' && (!mappings.sourceId || !mappings.targetId) ) {
			info.errors.push({
				title: "Relationship Import Error",
				message: "sourceId and targetId are required fields for relationship imports!"
			});
		}

		info.config = {
			targetType: $('#target-type-select').val(),
			delimiter: $('#delimiter').val(),
			quoteChar: $('#quote-char').val(),
			recordSeparator: $('#record-separator').val(),
			commitInterval: $('#commit-interval').val() || $('#commit-interval').attr('placeholder'),
			rfc4180Mode: $('#rfc4180-mode').prop('checked'),
			strictQuotes: $('#strict-quotes').prop('checked'),
			ignoreInvalid: $('#ignore-invalid').prop('checked'),
			distinct: $('#distinct').prop('checked'),
			range: $('#range').val(),
			importType: importType,
			mixedMappingConfig: mixedMappings,
			mappings: mappings,
			transforms: transforms,
			version: 2
		};

		return info;
	},
	displayImportPropertyMapping: (type, inputProperties, rowContainer, names, displayTransformInput, typeConfig, onLoadComplete, onSelect) => {

		let config = {
			type: type,
			inputProperties: inputProperties,
			rowContainer: rowContainer,
			names: names,
			displayTransformInput: displayTransformInput,
			typeConfig: typeConfig,
			onLoadComplete: onLoadComplete,
			onSelect: onSelect,
			displayMatchingPropertiesOnly: true
		};

		Importer.displayImportPropertyMappingWithConfig(config);
	},
	displayImportPropertyMappingWithConfig: (config) => {

		let blacklist = [
			'owner', 'ownerId', 'base', 'type', 'relType', 'createdBy', 'deleted', 'hidden', 'createdDate', 'lastModifiedDate',
			'visibleToPublicUsers', 'visibleToAuthenticatedUsers', 'lastModifiedBy', 'createdBy', 'grantees', 'structrChangeLog'
		];

		if ($('input[name=import-type]:checked').val() === 'rel') {
			blacklist.push('id');
		};

		fetch(`${Structr.rootUrl}_schema/${config.type}/all`).then(response => response.json()).then(typeInfo => {

			if (typeInfo && typeInfo.result) {

				_Helpers.sort(typeInfo.result, 'jsonName');

				let mapping = {};

				for (let [i, inputPropertyName] of Object.entries(config.inputProperties)) {

					let options        = '';
					let selectedString = '';
					let select         = $('select#key' + i);
					let longestMatch   = 0;
					config.names[i]    = inputPropertyName;

					// create drop-down list with pre-selected options
					for (let info of typeInfo.result) {

						if (blacklist.indexOf(info.jsonName) === -1) {

							// match with longest target property wins
							if (Importer.checkSelection(config.typeConfig, inputPropertyName, info.jsonName) && info.jsonName.length > longestMatch) {

								selectedString             = ' selected="selected"';
								longestMatch               = info.jsonName.length;
								mapping[inputPropertyName] = info.jsonName;

							} else {
								selectedString = '';
							}

							options += `<option${selectedString}>${info.jsonName}</option>`;
						}
					}

					// display selection
					config.rowContainer.append(`
						<tr>
							<td class="key">${inputPropertyName}</td>
							${config.displayTransformInput ? `<td class="transform"><input type="text" name="${inputPropertyName}" id="transform${i}" value="${config.typeConfig && config.typeConfig.mappings && config.typeConfig.mappings[inputPropertyName] ? config.typeConfig.mappings[inputPropertyName] : ''}" /></td>` : ''}
							<td>
								<select class="attr-mapping" name="${inputPropertyName}" id="key${i}">
									<option value="">-- skip --</option>
									${options}
								</select>
							</td>
						</tr>
					`);

					if (config.onSelect && typeof config.onSelect === "function") {
						select.on('change', config.onSelect);
					}
				}

				if (config.onLoadComplete && typeof config.onLoadComplete === "function") {
					config.onLoadComplete(mapping);
				}
			}
		});
	},
	checkSelection: (typeConfig, sourceName, targetName) => {

		if (typeConfig && typeConfig.properties) {

			return typeConfig.properties[sourceName] === targetName;

		} else if (sourceName && sourceName.length && targetName && targetName.length) {

			let src      = sourceName.toLowerCase().replace(/\W/g, '');
			let tgt      = targetName.toLowerCase().replace(/\W/g, '');
			return src === tgt;// || src.indexOf(tgt) >= 0 || tgt.indexOf(src) >= 0;
		}

		return false;
	},
	importXMLDialog: (file) => {

		let configuration = {};

		let { dialogText, dialogMeta } = _Dialogs.custom.openDialog(`Import XML from ${file.name}`, Importer.unload);

		let prevButton = _Dialogs.custom.prependCustomDialogButton('<button id="prev-element">Previous</button>');
		let nextButton = _Dialogs.custom.prependCustomDialogButton('<button id="next-element">Next</button>');
		let startButton = _Dialogs.custom.prependCustomDialogButton('<button class="action" id="start-import">Start import</button>');

		let html = Importer.templates.dialogConfigurations({type: 'xml'});
		dialogMeta.insertAdjacentHTML('beforeend', html);

		let importConfigSelector = $('#load-xml-config-selector');
		importConfigSelector.on('change', () => {
			Importer.configSelectorChangeHandler(importConfigSelector, 'xml');
		});
		Importer.configSelectorChangeHandler(importConfigSelector, 'xml');

		Importer.updateConfigSelector(importConfigSelector, 'xml');

		$('#load-xml-config-button').on('click', function() {

			Importer.loadImportConfiguration(importConfigSelector, (data) => {

				if (data && data.content) {

					let config = JSON.parse(data.content);

					Object.keys(config).forEach(function(k) {
						configuration[k] = config[k];

						switch (configuration[k]?.action) {
							case 'createNode':
								Importer.updateStructureSelector('', k, configuration[k].type);
								break;

							case 'setProperty':
								Importer.updateStructureSelectorForSetProperty('', k, configuration[k].propertyName);
								break;

							default:
								console.log("Unknown action: ", configuration[k]?.action ?? k);
						}
					});
				}
			});
		});

		dialogText.insertAdjacentHTML('beforeend', `
			<div id="xml-import">
				<div id="left">
					<h2>Document Structure</h2>
					<div class="xml-mapping">
						<table>
							<thead id="structure"></thead>
						</table>
					</div>
				</div>
				<div id="right">
					<div id="xml-config">
						<p class="hint">
							Please click one of the XML elements on the left to configure the XML import for that element.<br /><br />
							Use the &laquo;Next&raquo; and &laquo;Prev&raquo; buttons below to step through the XML elements.
						</p>
					</div>
				</div>
				<div style="clear: both;"></div>
			</div>
		`);

		nextButton.addEventListener('click', () => {

			let elem = $('td.xml-mapping.selected').parent('tr').next().children('td.xml-mapping');
			if (elem && elem.get(0)) {
				elem.get(0).scrollIntoView(false);
				elem.click();
			}
		});

		prevButton.addEventListener('click', () => {

			let elem = $('td.xml-mapping.selected').parent('tr').prev().children('td.xml-mapping');
			if (elem && elem.get(0)) {
				elem.get(0).scrollIntoView(false);
				elem.click();
			}
		});

		startButton.addEventListener('click', async () => {

			await fetch(`${Structr.rootUrl}File/${file.id}/doXMLImport`, {
				method: 'POST',
				body: JSON.stringify(configuration)
			});
		});

		$('#save-xml-config-button').on('click', () => {
			Importer.saveImportConfiguration(importConfigSelector, 'xml', configuration);
		});

		$('#update-xml-config-button').on('click', () => {
			Importer.updateImportConfiguration(importConfigSelector, configuration);
		});

		$('#delete-xml-config-button').on('click', () => {
			Importer.deleteImportConfiguration(importConfigSelector, 'xml');
		});

		let xmlConfig = $('#xml-config');
		fetch(`${Structr.rootUrl}File/${file.id}/getXMLStructure`, { method: 'POST' }).then(response => response.json()).then(data => {

			if (data && data.result) {

				let structure  = JSON.parse(data.result);
				let attributes = {};

				function buildTree(htmlElement, parentKey, treeElement, path, level) {

					for (let key in treeElement) {

						// store attributes
						if (key === '::attributes') {

							if (!attributes[parentKey]) {
								attributes[parentKey] = {};
							}
							let map = attributes[parentKey];

							for (let attr of treeElement[key]) {
								map[attr] = 1;
							}

						} else {

							let hasChildren = Object.keys(treeElement[key]).length > 1;
							let localPath   = path + '/' + key;

							htmlElement.append(
								`<tr>
									<td data-name="${localPath}" data-level="${level}" class="xml-mapping"  style="padding-left: ${level * 2}rem;">
										${_Icons.getSvgIcon(_Icons.iconChevronRightFilled, 8, 8, ['mr-2'])}${key}
									</td>
								</tr>`
							);

							$('td[data-name="' + localPath + '"]').on('click', function() {

								$('td.xml-mapping').removeClass('selected');
								$(this).addClass('selected');

								xmlConfig.empty();
								xmlConfig.append('<h2>&nbsp;</h2>');
								xmlConfig.append('<div id="config"></div>');

								let config = $('#config');
								config.append('<label>Select action:</label>');
								config.append('<select id="action-select" class="xml-config-select"></select>');

								let action = $('#action-select');
								action.append('<option value="">Skip</option>');
								action.append('<option value="ignore">Ignore branch</option>');
								action.append('<option value="createNode">Create node</option>');
								action.append('<option value="setProperty">Set property</option>');

								config.append('<div id="options"></div>');
								let options = $('#options');

								action.on('change', function() {

									// remove dialog options
									$('#options').empty();

									switch ($(this).val()) {
										case "createNode":
											Importer.showCreateNodeOptions(options, key, localPath, structure, configuration, attributes, hasChildren);
											break;
										case "setProperty":
											Importer.showSetPropertyOptions(options, key, localPath, structure, configuration, attributes);
											break;
										case "ignore":
											// reset configuration
											configuration[localPath] = { action: 'ignore' };
											Importer.updateStructureSelector(localPath);
											break;

										default:
											configuration[localPath] = {};
											Importer.updateStructureSelector(localPath);
											break;
									}
								});

								// activate elements for existing configuration
								let typeConfig = configuration[localPath];
								if (typeConfig && typeConfig.action) {

									$('#action-select').val(typeConfig.action).trigger('change');
								}
							});

							let value = treeElement[key];
							if (value) {

								buildTree(htmlElement, key, value, localPath, level + 1);
							}
						}
					}
				}

				buildTree($('#structure'), '', structure, '', 0);
			}
		});

	},
	hasRoot: (configuration) => {
		let hasRoot = false;
		for (let k in configuration) {
			let typeConfig = configuration[k];
			hasRoot |= typeConfig && typeConfig.isRoot;
		}
		return hasRoot;
	},
	isRoot: (configuration, path) => {
		let typeConfig = configuration[path];
		return typeConfig && typeConfig.isRoot;
	},
	setRoot: function(configuration, path) {
		Object.keys(configuration).forEach(function(k) {
			var typeConfig = configuration[k];
			if (typeConfig) {
				typeConfig.isRoot = (k === path);
			}
		});
	},
	getParentType: (path, configuration) => {

		let parts = path.split('/');
		let index = parts.length;

		while (--index >= 0) {

			let fullPath = parts.slice(0, index).join('/');
			if (configuration[fullPath] && configuration[fullPath].type) {
				return configuration[fullPath].type;
			}
		}

		return undefined;
	},
	showCreateNodeOptions: (el, key, path, structure, configuration, attributes, hasChildren) => {

		if (!configuration[path]) {
			configuration[path] = {};
		}

		configuration[path].action = 'createNode';

		let isRoot  = Importer.isRoot(configuration, path);
		let hasRoot = Importer.hasRoot(configuration);

		if (!hasRoot) {
			Importer.setRoot(configuration, path);
			isRoot = true;
		}

		let defaultSelectEntry = '<option>-- select --</option>';

		el.append(`
			<label>Select type:</label>
			<select id="type-select" class="xml-config-select"></select>
			<span>
				<input type="checkbox" id="target-type-custom-only">
				<label for="target-type-custom-only">Only show custom types</label>
			</span>
			${(!isRoot) ? '<div id="non-root-options"></div>' : ''}
			<div id="property-select"></div>
		`);

		let typeSelector     = $('#type-select');
		let propertySelector = $('#property-select');
		let typeConfig       = configuration[path];

		typeSelector.on('change', function(e) {

			let type  = $(this).val();
			let names = [];

			Importer.updateStructureSelector(key, path, type);

			if (!isRoot) {

				let parentType = Importer.getParentType(path, configuration);
				if (parentType) {

					let nonRoot    = $('#non-root-options');

					nonRoot.empty();
					nonRoot.append('<div><label>Select property name:</label><select id="name-select" class="xml-config-select"></select></div>');

					let nameSelect    = $('#name-select');

					//fetchPropertyList(type, typeConfig, key, select, loadCallback, changeCallback, appendCallback) {
					Importer.fetchPropertyList(parentType, typeConfig, '', nameSelect, function() {

						// trigger select event when an element is already configured
						if (typeConfig && typeConfig.propertyName) {
							nameSelect.val(typeConfig.propertyName);
						}
						nameSelect.trigger('change');

					}, function() {

						let option       = nameSelect.children(':selected');
						let isCollection = $(option).data('isCollection');
						let value        = $(option).val();

						if (value && value.length) {

							configuration[path].propertyName = value;
							if (!isCollection) {

								configuration[path].multiplicity = "1";
							}
						}

					}, function(info, selectedString) {
						if (info.type === type) {
							nameSelect.append(`<option ${selectedString} data-is-collection="${info.isCollection}">${info.jsonName}</option>`);
						}
					});

					// allow text content of a node to be stored
					if (!hasChildren) {

						nonRoot.append('<div><label>Select property for text content:</label><select id="content-select" class="xml-config-select"></select></div>');

						let contentSelect = $('#content-select');

						//fetchPropertyList(type, typeConfig, key, select, loadCallback, changeCallback, appendCallback) {
						Importer.fetchPropertyList(type, typeConfig, '', contentSelect, function() {

							// trigger select event when an element is already configured
							if (typeConfig && typeConfig.content) {
								nameSelect.val(typeConfig.content);
							}

							contentSelect.trigger('change');

						}, function() {

							let value = contentSelect.val();
							if (value && value.length) {

								configuration[path].content = value;
							}

						}, function(info, selectedString) {

							if (info.type === 'String') {
								contentSelect.append('<option ' + selectedString + '">' + info.jsonName + '</option>');
							}
						});
					}

					nonRoot.append('<div><label>Use batching for this type</label><input type="checkbox" id="batching-checkbox"></div>');

					if (configuration['batchType'] === type) {
						$('#batching-checkbox').prop('checked', true);
					}

					$('#batching-checkbox').on('change', function() {
						if ($(this).is(':checked')) {
							configuration['batchType'] = typeSelector.val();
						} else {
							configuration['batchType'] = null;
						}
					});
				}
			}

			propertySelector.empty();
			propertySelector.append(`
				<h3>Attribute mapping</h3>
				<div class="attr-mapping">
					<table>
						<tbody id="row-container"></tbody>
					</table>
				</div>
			`);

			let rowContainer    = $('#row-container');
			let inputProperties = [];
			if (attributes[key]) {
				inputProperties = Object.keys(attributes[key]);
			}

			Importer.displayImportPropertyMapping(type, inputProperties, rowContainer, names, false, configuration[path], (mapping) => {

				let typeConfig = configuration[path];
				if (!typeConfig) {

					typeConfig = {};
					configuration[path] = typeConfig;
				}

				typeConfig.action     = 'createNode';
				typeConfig.type       = type;
				typeConfig.properties = {};

				for (let mappedKey of Object.keys(mapping)) {
					typeConfig.properties[mappedKey] = mapping[mappedKey];
				}

			}, function() {

				// on select
				let select = $(this);
				let name   = select.attr('name');
				let value  = select.val();

				if (name && name.length) {

					let typeConfig = configuration[path];
					typeConfig.properties[name] = value;
				}
			});

		});

		let showOnlyCustomTypesCheckbox = el[0].querySelector('#target-type-custom-only');

		Importer.updateSchemaTypeCache().then(ignore => {

			showOnlyCustomTypesCheckbox.addEventListener('change', () => {

				let showOnlyCustomTypes                 = showOnlyCustomTypesCheckbox.checked;
				configuration[path].showOnlyCustomTypes = showOnlyCustomTypes;
				let list                                = Importer.schemaTypeCache.nodeTypes;
				let prevSelected                        = typeSelector.val();

				if (showOnlyCustomTypes) {
					list = list.filter(type => !type.isBuiltin);
				}

				typeSelector.html(defaultSelectEntry + list.map(n => `<option value="${n.name}">${n.name}</option>`).sort().join(''));

				typeSelector.val(prevSelected);

				if (typeSelector.val() !== prevSelected) {
					typeSelector[0].selectedIndex = 0;

					typeSelector.trigger('change');
				}
			});

			typeSelector.html(defaultSelectEntry + Importer.schemaTypeCache.nodeTypes.map(n => `<option value="${n.name}">${n.name}</option>`).sort().join(''));

			if (typeConfig) {

				if (typeConfig.showOnlyCustomTypes === true) {
					showOnlyCustomTypesCheckbox.checked = true;
					showOnlyCustomTypesCheckbox.dispatchEvent(new Event('change'));
				}

				// trigger select event when an element is already configured
				if (typeConfig && typeConfig.type) {
					typeSelector.val(typeConfig.type).trigger('change');
				}
			}
		});
	},
	showSetPropertyOptions: (el, key, path, structure, configuration, attributes) => {

		if (!configuration[path]) {
			configuration[path] = {};
		}

		configuration[path].action = 'setProperty';

		let parentType = Importer.getParentType(path, configuration);
		if (!parentType) {

			el.append('<p class="hint">Action &laquo;setProperty&raquo; cannot be used without enclosing &laquo;createNode&raquo; action.</p>');

		} else {

			el.append('<label>Select property for text content:</label>');
			el.append('<select id="text-select" class="xml-config-select"><option value="">--- ignore ---</option></select>');

			let textSelect = $('#text-select');
			let typeConfig = configuration[path];

			Importer.fetchPropertyList(parentType, typeConfig, '', textSelect, () => {

				// trigger select event when an element is already configured
				if (typeConfig && typeConfig.propertyName) {
					textSelect.val(typeConfig.propertyName).trigger('change');
				}

			}, () => {

				let value = textSelect.val();

				if (value && value.length) {
					configuration[path].propertyName = value;
					Importer.updateStructureSelector(key, path, value);
				}
			});
		}
	},
	fetchPropertyList: (type, typeConfig, key, select, loadCallback, changeCallback, appendCallback) => {

		let blacklist = [
			'id', 'owner', 'ownerId', 'base', 'type', 'createdBy', 'deleted', 'hidden', 'createdDate', 'lastModifiedDate',
			'visibleToPublicUsers', 'visibleToAuthenticatedUsers', 'lastModifiedBy', 'createdBy', 'grantees', 'structrChangeLog'
		];

		fetch(Structr.rootUrl + '_schema/' + type + '/all').then(response => response.json()).then(typeInfo => {

			if (typeInfo && typeInfo.result) {

				_Helpers.sort(typeInfo.result, 'jsonName');

				let selectedString = '';
				let longestMatch   = 0;

				// create drop-down list with pre-selected options
				for (let info of typeInfo.result) {

					// skip names that are blacklisted
					if (blacklist.indexOf(info.jsonName) === -1) {

						// match with longest target property wins
						if (Importer.checkSelection(typeConfig, key, info.jsonName) && info.jsonName.length > longestMatch) {

							selectedString = ' selected="selected"';
							longestMatch   = info.jsonName.length;

						} else {
							selectedString = '';
						}

						if (appendCallback && typeof appendCallback === 'function') {
							appendCallback(info, selectedString);
						} else {
							select.append(`<option${selectedString}>${info.jsonName}</option>`);
						}
					}
				}

				// onChange callback
				if (changeCallback && typeof changeCallback === 'function') {
					select.on('change', changeCallback);
				}

				// callback when loading is finished
				if (loadCallback && typeof loadCallback === 'function') {
					loadCallback();
				}
			}
		});
	},
	updateStructureSelector: (key, path, value) => {

		let elem = $('td.xml-mapping[data-name="' + path + '"]');
		elem.empty();

		if (value && value.length) {
			elem.append(`<b>${_Icons.getSvgIcon(_Icons.iconChevronRightFilled, 8, 8, ['mr-2'])}${value}</b>`);
		} else if (key && key.length) {
			elem.append(`${_Icons.getSvgIcon(_Icons.iconChevronRightFilled, 8, 8, ['mr-2'])}${key}`);
		}
	},
	updateStructureSelectorForSetProperty: (key, path, propertyName) => {

		let elem = $('td.xml-mapping[data-name="' + path + '"]');
		elem.empty();
		elem.append(`<b>${_Icons.getSvgIcon(_Icons.iconChevronRightFilled, 8, 8, ['mr-2'])}${propertyName}</b>`);
	},




	/***************************************************************************************************************************************************************************************/
	/* BEGIN NEW MIXED DATA IMPORT DIALOG */


	formatMixedImportDialog: (file, mixedMappingConfig) => {

		_Helpers.appendInfoTextToElement({
			text: "Use the select box labelled &quot;Add target type..&quot; to add type mappings.",
			element: $('#type-mapping-title'),
			css: {
				marginLeft: "2px"
			}
		});

		let targetTypeSelector = $('#target-type-select');
		let customOnlyCheckbox = $('input#target-type-custom-only');

		if (Importer.customTypesOnly) {
			customOnlyCheckbox.prop('checked', true);
		}

		Importer.updateSchemaTypeCache().then(ignore => {
			Importer.updateSchemaTypeSelector(targetTypeSelector);
		});

		$('#types-container').empty();
		$('#start-import').off('click');
		_Helpers.disableElement($('#start-import')[0]);

		customOnlyCheckbox.off('change').on('change', function() {
			Importer.customTypesOnly = $(this).prop('checked');
			Importer.updateSchemaTypeSelector(targetTypeSelector);
			$('#types-container').empty();
			$('#start-import').off('click');
			_Helpers.disableElement($('#start-import')[0]);
		});

		Importer.updateSchemaTypeSelector(targetTypeSelector);

		// collect CSV headers to use
		fetch(`${Structr.rootUrl}File/${file.id}/getCSVHeaders`, {
			method: 'POST',
			body: JSON.stringify({
				delimiter: $('#delimiter').val(),
				quoteChar: $('#quote-char').val(),
				recordSeparator: $('#record-separator').val()
			})
		}).then(response => response.json()).then(csvHeaders => {

			if (csvHeaders && csvHeaders.result && csvHeaders.result.headers) {

				for (let p of csvHeaders.result.headers) {
					mixedMappingConfig.availableProperties[p] = {
						name: p,
						matched: false
					}
				}

				Importer.displayMixedMappingConfiguration(mixedMappingConfig);

				// what happens when the user selects a type
				targetTypeSelector.off('change').on('change', function(e, data) {

					let selectedType = $('select[name=targetType]').val();

					Importer.updateMixedMapping(selectedType, file, data, mixedMappingConfig);

					targetTypeSelector.val('');
				});
			}
		});
	},
	removeMixedTypeMapping: (id, mixedMapping) => {

		// remove "matched" flag
		for (let key of Object.keys(mixedMapping.mappedTypes[id].properties)) {
			mixedMapping.availableProperties[key].matched = false;
		}

		delete mixedMapping.mappedTypes[id];

		Importer.displayMixedMappingConfiguration(mixedMapping);
	},
	updateMixedMapping: (id, file, data, mixedMappingConfig) => {

		let targetTypeSelector = $('#target-type-select');
		let matchingContainer  = $('#matching-properties-' + id);
		let availableContainer = $('#available-properties-' + id);
		let type               = targetTypeSelector.val();
		let typeConfig         = {};

		// dont do anything if there is no type set
		if (!type) {
			return;
		}

		// add type to mixed mapping config
		mixedMappingConfig.mappedTypes[id] = {
			name: id,
			properties: {},
			relationships: []
		};

		// clear current mapping list
		availableContainer.empty();
		matchingContainer.empty();

		if (data) {
			typeConfig['properties'] = data.mappings;
			typeConfig['mappings']   = data.transforms;
		}

		let config = {
			type: type,
			availablePropertyContainer: availableContainer,
			matchingPropertyContainer: matchingContainer,
			names: [],
			displayTransformInput: false,
			typeConfig: typeConfig,
			mixedMappingConfig: mixedMappingConfig,
			displayMatchingPropertiesOnly: true,
			onLoadComplete: () => {

				_Helpers.enableElement($('#start-import')[0]);
				$('#start-import').off('click').on('click', function() {

					let configInfo = Importer.collectCSVImportConfigurationInfo();
					let allowImport = (configInfo.errors.length === 0);

					if (!allowImport) {

						for (let e of configInfo.errors) {
							new ErrorMessage().title(e.title).text(e.message).show();
						}

					} else {

						configInfo.config.mixedMappings = mixedMappingConfig.mappedTypes;

						fetch(`${Structr.rootUrl}File/${file.id}/doCSVImport`, {
							method: 'POST',
							body: JSON.stringify(configInfo.config)
						});
					}
				})
			}
		};

		Importer.updateAdvancedImportPropertyMapping(config);
	},
	updateAdvancedImportPropertyMapping: (config) => {

		let blacklist = [
			'owner', 'ownerId', 'base', 'type', 'relType', 'createdBy', 'deleted', 'hidden', 'createdDate', 'lastModifiedDate',
			'visibleToPublicUsers', 'visibleToAuthenticatedUsers', 'lastModifiedBy', 'createdBy', 'grantees', 'structrChangeLog'
		];

		if ($('input[name=import-type]:checked').val() === 'rel') {
			blacklist.push('id');
		}

		fetch(`${Structr.rootUrl}_schema/${config.type}/all`).then(response => response.json()).then(typeInfo => {

			if (typeInfo && typeInfo.result) {

				_Helpers.sort(typeInfo.result, 'jsonName');

				let mapping = config.mixedMappingConfig;

				Object.keys(mapping.availableProperties).forEach((key, i) => {

					let inputProperty     = mapping.availableProperties[key];
					let inputPropertyName = inputProperty.name;
					let longestMatch      = 0;
					let matchFound        = false;
					config.names[i]       = inputPropertyName;

					// create drop-down list with pre-selected options
					typeInfo.result.some(info => {

						if (blacklist.indexOf(info.jsonName) === -1) {

							// match with longest target property wins
							if (Importer.checkSelection(config.typeConfig, inputPropertyName, info.jsonName) && info.jsonName.length > longestMatch) {

								let mappedType = mapping.mappedTypes[config.type];

								mappedType.properties[inputPropertyName] = info.jsonName;
								longestMatch                             = info.jsonName.length;
								matchFound                               = true;

								return true;
							}
						}
					});

					// display selection
					if (matchFound) {
						inputProperty.matched = true;
					}
				});

				// update display
				Importer.displayMixedMappingConfiguration(mapping);

				if (config.onLoadComplete && typeof config.onLoadComplete === "function") {
					config.onLoadComplete(mapping);
				}
			}
		});
	},
	displayMixedMappingConfiguration: (mixedMapping) => {

		let availableProperties = $('#available-properties');
		let mappedTypes         = $('#types-container');

		availableProperties.empty();
		mappedTypes.empty();

		for (let key of Object.keys(mixedMapping.availableProperties)) {

			let property = mixedMapping.availableProperties[key];

			if (property.matched === false) {
				availableProperties.append(`<span>${property.name}</span>`);
			}
		}

		let options = `
			<option>Add relationship</option>
			${Object.keys(mixedMapping.mappedTypes).map(key => `<option>${key}</option>`).join('')}
		`;

		for (let key in mixedMapping.mappedTypes) {

			let mappedType = mixedMapping.mappedTypes[key];

			mappedTypes.append(Importer.templates.snippetTypeContainer({ type: mappedType.name, id: mappedType.name }));

			let propertyContainer     = $('#matching-properties-' + mappedType.name);
			let relationshipContainer = $('#relationships-' + mappedType.name);

			propertyContainer.append(Object.keys(mappedType.properties).map(propertyName => Importer.templates.snippetMappingRow({ name: propertyName })).join(''));
			relationshipContainer.append(mappedType.relationships.map(propertyName => Importer.templates.snippetMappingRow({ name: propertyName })).join(''));

			$('#remove-button-' + mappedType.name).off('click').on('click', () => {
				Importer.removeMixedTypeMapping(mappedType.name, mixedMapping);
			});

			let addRelationshipContainer = $('#add-relationship-' + mappedType.name);

			addRelationshipContainer.append(`<select class="add-selector" id="${mappedType.name}-related-to">${options}</select>`);

			$(`#${mappedType.name}-related-to`).off('change').on('change', function(e) {

				mappedType.relationships.push($(this).val());
				Importer.displayMixedMappingConfiguration(mixedMapping);
			});
		}
	},

	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/crud.css">
			<link rel="stylesheet" type="text/css" media="screen" href="css/importer.css">
			
			<div id="importer-main" class="resourceBox">
				<table id="importer-jobs-table">
					<thead><tr>
						<th>Job ID</th>
						<th>Job Type</th>
						<th>User</th>
						<th>File UUID</th>
						<th>File path</th>
						<th>File size</th>
						<th>Processed Chunks</th>
						<th>Status</th>
						<th>Action</th>
					</tr></thead>
					<tbody></tbody>
				</table>
			</div>
		`,
		functions: config => `
			<div class="flex flex-grow">

				<button class="refresh flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
					${_Icons.getSvgIcon(_Icons.iconRefreshArrows, 16, 16, 'mr-2')} Refresh
				</button>

				<div class="button-and-input inline-flex items-center">

					<label for="cancel-all-queued-after-job-id" class="mr-2">
						Cancel ALL <b>queued</b> jobs after this ID:
					</label>

					<input size="6" type="text" id="cancel-all-queued-after-job-id" placeholder="Job ID">

					<button id="cancel-all-queued-after" class="inline-flex items-center ml-2 hover:bg-gray-100 focus:border-gray-666 active:border-green">
						${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['mr-2', 'icon-red']))} Cancel Job
					</button>

				</div>

			</div>
		`,
		dialogConfigurations: config => `
			<div id="${config.type}-configurations">
				<select id="load-${config.type}-config-selector"></select>
				<button id="update-${config.type}-config-button">Update</button>
				<button id="load-${config.type}-config-button">Load</button>
				<button id="delete-${config.type}-config-button">Delete</button>
				<input id="${config.type}-config-name-input" type="text" placeholder="Enter name for configuration" />
				<button id="save-${config.type}-config-button">Save</button>
			</div>
		`,
		dialogCSV: config => `
			<div>
				<div id="sample">
					<h3>Data Sample</h3>
					<pre class="csv-preview">${config.data.result.lines}</pre>
				</div>
				<h3>Import Options</h3>
				<table id="csv-import">
					<tbody>
						<tr id="options-row1">
							<td valign="top">
								<label class="inline-block min-w-36">Delimiter:</label>
								<select id="delimiter" class="import-option">
									<option ${(config.delim === ',' ? 'selected' : '')}>,</option>
									<option ${(config.delim === ';' ? 'selected' : '')}>;</option>
									<option ${(config.delim === '|' ? 'selected' : '')}>|</option>
								</select>
							</td>
							<td valign="top">
								<label class="inline-block min-w-36">Quote character:</label>
								<select id="quote-char" class="import-option">
									<option ${(config.qc === '' ? 'selected' : '')}></option>
									<option ${(config.qc === '"' ? 'selected' : '')}>&quot;</option>
									<option ${(config.qc === '\'' ? 'selected' : '')}>\'</option>
								</select>
							</td>
							<td valign="top">
								<label class="inline-block min-w-36">Record separator:</label>
								<select id="record-separator" class="import-option">
									<option ${(config.data.result.separator ===    'LF' ? 'selected' : '')}>LF</option>
									<option ${(config.data.result.separator ===    'CR' ? 'selected' : '')}>CR</option>
									<option ${(config.data.result.separator === 'CR+LF' ? 'selected' : '')}>CR+LF</option>
								</select>
							</td>
							<td valign="top">
								<label class="inline-block min-w-36">Commit interval:</label>
								<input type="number" id="commit-interval" value="1000" placeholder="1000" title="Enter 0 to disable periodic commit.">
							</td>
						</tr>
						<tr id="options-row2">
							<td valign="top">
								<div class="flex">
									<div class="min-w-36">Import type:</div>
									<div class="flex flex-col">
										<label class="inline-block"><input type="radio" name="import-type" value="node" ${(config.importType === 'node' ? 'checked' : '')}> Node</label>
										<label class="pl-36"><input type="radio" name="import-type" value="rel" ${(config.importType === 'rel' ? 'checked' : '')}> Relationship</label>
										<label class="pl-36"><input type="radio" name="import-type" value="graph" ${(config.importType === 'graph' ? 'checked' : '')}> Mixed</label>
									</div>
								</div>
							</td>
							<td valign="top">
								<div class="flex flex-col">
									<label><input type="checkbox" id="rfc4180-mode">RFC4180 mode</label>
									<label><input type="checkbox" id="strict-quotes">Strict quotes</label>
									<label><input type="checkbox" id="ignore-invalid">Ignore invalid lines</label>
									<label><input type="checkbox" id="distinct">Skip duplicates</label>
								</div>
							</td>
							<td colspan="2" valign="top">
								<label class="inline-block min-w-36" for="range">Line range:</label>
								<input type="text" id="range" title="Enter range (0-100)." placeholder="e.g. 1-100 or 1,2,3-10">
							</td>
						</tr>
					</tbody>
				</table>
				<div id="import-dialog-type-container"></div>
			</div>
		`,
		dialogTargetTypeSelect_graph: config => `
			<h3>Add target types and properties</h3>
			<select id="target-type-select" name="targetType">
				<option value="" disabled="disabled" selected="selected">Add target type..</option>
			</select>
			<span><input type="checkbox" id="target-type-custom-only"><label for="target-type-custom-only">Only show custom types</label></span>
			<h3>Available properties</h3>
			<div id="available-properties"></div>
			<h3 id="type-mapping-title">Type Mapping</h3>
			<div id="types-container"></div>
		`,
		dialogTargetTypeSelect_node: config => `
			<h3>Select target type</h3>
			<select id="target-type-select" name="targetType">
				<option value="" disabled="disabled" selected="selected">Select target type..</option>
			</select>
			<span><input type="checkbox" id="target-type-custom-only"><label for="target-type-custom-only">Only show custom types</label></span>
			<div id="property-select"></div>
		`,
		dialogTargetTypeSelect_rel: config => `
			<h3>Select target type</h3>
			<select id="target-type-select" name="targetType">
				<option value="" disabled="disabled" selected="selected">Select target type..</option>
			</select>
			<span><input type="checkbox" id="target-type-custom-only"><label for="target-type-custom-only">Only show custom types</label></span>
			<div id="property-select"></div>
		`,
		snippetMappingRow: config => `
			<p>${config.name}</p>
		`,
		snippetTypeContainer: config => `
			<div class="type-mapping" id="type-mapping-${config.id}">
				<span class="pull-right"><i class="fa fa-remove" title="Remove this mapping" id="remove-button-${config.id}"></i></span>
				<h3>${config.type}</h3>
				<p class="divider">Mapped properties</p>
				<div id="matching-properties-${config.type}"></div>
				<p class="divider">Relationships</p>
				<div id="relationships-${config.type}">
				</div>
				<div id="add-relationship-${config.type}">
				</div>
			</div>
		`
	}
};