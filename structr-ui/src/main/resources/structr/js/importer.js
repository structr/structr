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

	init: function() {
	},
	resize: function() {
	},
	onload: function() {

		Importer.init();

		Structr.mainContainer.innerHTML = Importer.templates.main();
		Structr.functionBar.innerHTML   = Importer.templates.functions();

		UISettings.showSettingsForCurrentModule();

		$('#importer-main .refresh').click(function () {
			Importer.updateJobTable();
		});

		$('#cancel-all-queued-after').click(function () {

			let jobId = parseInt($('#cancel-all-queued-after-job-id').val());

			if (isNaN(jobId)) {
				new MessageBuilder().warning("Unable to parse job id").show();
			} else {
				Command.fileImport('cancelAllAfter', jobId, () => {

					$('#cancel-all-queued-after-job-id').val('');
					Importer.updateJobTable();
				});
			}
		});

		Importer.updateJobTable();

		Structr.unblockMenu(100);
	},
	unload: function() {
		Importer.schemaTypeCachePopulated = false;
		Importer.restoreButtons();
	},
	isShowNotifications: function() {
		return UISettings.getValueForSetting(UISettings.importer.settings.showNotificationsKey);
	},
	updateJobTable: function () {

		window.clearTimeout(Importer.timeout);

		window.setTimeout(function () {

			Command.fileImport('list', null, function (jobs) {

				var table = $('#importer-jobs-table');
				var tbody = $('tbody', table);
				tbody.empty();

				var imports = jobs[0].imports;

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
	createRowForJob: function (job) {
		return $('<tr><td>' + job.jobId + '</td><td>' + job.jobtype + '</td><td>' + job.username + '</td>' + Importer.createJobInfoHTML(job) + '<td>' + job.status + '</td><td>' + Importer.createActionButtons(job) + '</td></tr>');
	},
	createJobInfoHTML:function(job) {
		switch (job.jobtype) {
			case 'XML':
			case 'CSV':
				return '<td>' + job.fileUuid + '</td><td>' + job.filepath + '</td><td>' + job.filesize + '</td><td>' + job.processedChunks + '</td>';

			case 'SCRIPT':
				return '<td colspan=4 class="' + (job.jobName.length === 0 ? 'placeholderText' : '') + '">' + job.jobName + '</td>';

			default:
				return '<td colspan=4 class="placeholderText"> - not applicable - </td>';
		}
	},
	createActionButtons: function (job) {
		var actionHtml = '';

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
		return '<button class="import-job-action" data-action="' + action + '" data-job-id="' + jobId + '">' + content + '</button>';
	},

	initializeButtons: function(start, next, prev, close) {

		if (prev) {
			dialogBtn.prepend('<button id="prev-element">Previous</button>');
		}

		if (next) {
			dialogBtn.prepend('<button id="next-element">Next</button>');
		}

		if (start) {
			dialogBtn.prepend('<button class="action" id="start-import">Start import</button>');
		}

		if (!close) {

			dialogCancelButton.addClass('hidden');

		} else {

			dialogCancelButton.on('click', Importer.unload);
		}

	},
	restoreButtons: function() {
		dialogCancelButton.removeClass('hidden');
		$('#start-import').remove();
		$('#next-element').remove();
		$('#prev-element').remove();
		$('#csv-configurations').remove();
		$('#xml-configurations').remove();
	},
	updateConfigSelector: function(elem, importType) {

		Command.getApplicationConfigurationDataNodesGroupedByUser(importType + '-import', function(grouped) {

			elem.empty();
			elem.append('<option selected value="" disabled>--- Select configuration to load ---</option>');

			grouped.forEach(function(group) {

				var optGroup = $('<optgroup label="' + group.ownerName + '"></optgroup>');
				elem.append(optGroup);

				group.configs.forEach(function(cfg) {

					optGroup.append('<option value="' + cfg.id + '">' + cfg.name + '</option>');
				});
			});

			Importer.configSelectorChangeHandler(elem, importType);
		});

	},
	saveImportConfiguration: function (elem, importType, configuration) {

		var inputElem = $('#' + importType + '-config-name-input');
		var name = inputElem.val();

		if (name && name.length) {

			Command.createApplicationConfigurationDataNode(importType + '-import', name, JSON.stringify(configuration), function(data) {

				if (!data.error) {

					new MessageBuilder().success("Import Configuration saved").show();

					Importer.updateConfigSelector(elem, importType);
					inputElem.val('');

					blinkGreen(elem);

				} else {
					new MessageBuilder().error().title(data.error).text(data.message).show();
				}
			});

		} else {
			Structr.error('Import configuration layout name is required.');
		}
	},
	updateImportConfiguration: function (elem, configuration) {

		var selectedConfig = elem.val();

		Command.setProperty(selectedConfig, 'content', JSON.stringify(configuration), false, function(data) {

			if (!data.error) {

				new MessageBuilder().success("Import Configuration saved").show();

				blinkGreen(elem);

			} else {

				new MessageBuilder().error().title(data.error).text(data.message).show();
			}
		});
	},
	loadImportConfiguration: function(elem, callback) {

		try {
			Command.getApplicationConfigurationDataNode(elem.val(), callback);
		} catch (e) {
			Structr.error('Error parsing configuration, please see browser log');
			console.log(e);
		}
	},
	configSelectorChangeHandler: function(elem, importType) {

		var updateImportConfigButton = $('#update-' + importType + '-config-button');
		var loadImportConfigButton   = $('#load-' + importType + '-config-button');
		var deleteImportConfigButton = $('#delete-' + importType + '-config-button');

		var selectedOption = $(':selected:not(:disabled)', elem);

		if (selectedOption.length === 0) {

			Structr.disableButton(updateImportConfigButton);
			Structr.disableButton(loadImportConfigButton);
			Structr.disableButton(deleteImportConfigButton);

		} else {

			Structr.enableButton(loadImportConfigButton);

			var username = selectedOption.closest('optgroup').prop('label');

			if (username !== 'null' && username !== StructrWS.me.username) {
				Structr.disableButton(updateImportConfigButton);
				Structr.disableButton(deleteImportConfigButton);
			} else {
				Structr.enableButton(updateImportConfigButton);
				Structr.enableButton(deleteImportConfigButton);
			}
		}
	},
	deleteImportConfiguration: function(elem, importType) {

		Command.deleteNode(elem.val(), false, function() {
			Importer.updateConfigSelector(elem, importType);
			blinkGreen(elem);
		});
	},
	importCSVDialog: function(file) {

		// define datastructure here to be able to use it in callbacks etc. below
		var mixedMappingConfig = {
			availableProperties: {},
			mappedTypes: {}
		};

		Importer.clearSchemaTypeCache();

		Structr.dialog('Import CSV from ' + file.name, function() {}, function() {});

		Importer.initializeButtons(true, false, false, true);

		let html = Importer.templates.dialogConfigurations({type: 'csv'});
		dialogMeta.append(html);

		var importConfigSelector = $('#load-csv-config-selector');
		importConfigSelector.on('change', function () {
			Importer.configSelectorChangeHandler(importConfigSelector, 'csv');
		});
		Importer.configSelectorChangeHandler(importConfigSelector, 'csv');

		Importer.updateConfigSelector(importConfigSelector, 'csv');

		$('#load-csv-config-button').on('click', function() {

			Importer.loadImportConfiguration(importConfigSelector, function(data) {

				if (data && data.content) {

					var config = JSON.parse(data.content).config;

					if (!config.version) {
						// initial version, no reversal needed

					} else if (config.version === 2) {

						var reversedTransforms = {};
						Object.keys(config.transforms).forEach(function(k) {
							reversedTransforms[config.mappings[k]] = config.transforms[k];
						});

						config.transforms = reversedTransforms;

						// New version - reverse mappings/transforms so we can display them
						var reversedMappings = {};

						Object.keys(config.mappings).forEach(function(k) {
							reversedMappings[config.mappings[k]] = k;
						});

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

							new MessageBuilder().warning('Type ' + config.targetType + ' from loaded configuration does not exist. This may be due to an outdated configuration.').show();
							Importer.customTypesOnly = true;

						}
					}

					// update storage datastructure before triggering events..
					if (importType === 'graph') {
						mixedMappingConfig.mappedTypes = config.mixedMappingConfig;
					}

					$('input[name=import-type][value=' + importType + ']').prop('checked', 'checked').trigger('change');

					$('#target-type-select').val(config.targetType).trigger('change', [config]);
				}
			});
		});

		$('#update-csv-config-button').on('click', function() {

			var configInfo = Importer.collectCSVImportConfigurationInfo(mixedMappingConfig.mappedTypes);

			if (configInfo.errors.length > 0) {

				configInfo.errors.forEach(function(e) {
					new MessageBuilder().title(e.title).error(e.message).show();
				});

			} else {

				Importer.updateImportConfiguration(importConfigSelector, configInfo);
			}
		});

		$('#save-csv-config-button').on('click', function() {

			var configInfo = Importer.collectCSVImportConfigurationInfo(mixedMappingConfig.mappedTypes);

			if (configInfo.errors.length > 0) {

				configInfo.errors.forEach(function(e) {
					new MessageBuilder().title(e.title).error(e.message).show();
				});

			} else {

				Importer.saveImportConfiguration(importConfigSelector, 'csv', configInfo);
			}
		});

		$('#delete-csv-config-button').on('click', function() {
			Importer.deleteImportConfiguration(importConfigSelector, 'csv');
		});

		// load first lines to display a sample of the data
		$.post(Structr.rootUrl + 'File/' + file.id + '/getFirstLines', {}, function(data) {

			if (data && data.result) {

				let results   = Papa.parse(data.result.lines);
				let delim     = results.meta.delimiter;
				let qc        = data.result.lines.substring(0,1);
				let html      = Importer.templates.dialogCSV({ data: data, delim: delim, qc: qc, importType: "node" });
				let container = $(html);
				dialog.append(container);

				Importer.formatImportTypeSelectorDialog(file, mixedMappingConfig);

				$('input[name=import-type]').off('change').on('change', function() {

					// call self on change
					Importer.formatImportTypeSelectorDialog(file, mixedMappingConfig);
				});
			}
		});
	},
	formatImportTypeSelectorDialog: function(file, mixedMappingConfig) {

		let importType = $('input[name=import-type]:checked').val();
		let html       = Importer.templates['dialogTargetTypeSelect_' + importType]();

		$('#import-dialog-type-container').empty();
		$('#import-dialog-type-container').append(html);

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
	formatNodeOrRelImportDialog: function(file) {

		var targetTypeSelector = $('#target-type-select');

		Importer.updateSchemaTypeCache(targetTypeSelector);

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
	updateSchemaTypeCache: function(targetTypeSelector) {

		if (!Importer.schemaTypeCachePopulated) {

			$.get(Structr.rootUrl + 'AbstractSchemaNode?' + Structr.getRequestParameterName('sort') + '=name', function(data) {

				if (data && data.result) {

					Importer.clearSchemaTypeCache();

					data.result.forEach(function(res) {

						if (res.type === 'SchemaRelationshipNode') {

							Importer.schemaTypeCache['relTypes'].push(res);

						} else {

							Importer.schemaTypeCache['graphTypes'].push(res);
							Importer.schemaTypeCache['nodeTypes'].push(res);
						}
					});

					Importer.updateSchemaTypeSelector(targetTypeSelector);

					Importer.schemaTypeCachePopulated = true;
				}
			});
		}

	},
	updateSchemaTypeSelector: function(typeSelect) {

		var importType = $('input[name=import-type]:checked').val();

		$('option[disabled!=disabled]', typeSelect).remove();
		typeSelect.val("");

		var data = Importer.getSchemaTypeSelectorData(importType);

		data.forEach(function(name) {

			typeSelect.append('<option value="' + name + '">' + name + '</option>');
		});

	},
	getSchemaTypeSelectorData: function(importType = "") {

		let allTypeData = Importer.schemaTypeCache[importType + "Types"];

		if ((importType === 'node' || importType === 'graph') && Importer.customTypesOnly === true) {
			allTypeData = allTypeData.filter((t) => {
				return t.isBuiltinType === false;
			});
		}

		return allTypeData.map((t) => { return t.name; });

	},
	clearSchemaTypeCache: function() {

		Importer.schemaTypeCache = {
			nodeTypes: [],
			relTypes: [],
			graphTypes: []
		};

	},
	updateMapping: function(file, data) {

		var targetTypeSelector = $('#target-type-select');
		var propertySelector   = $('#property-select');
		var type               = targetTypeSelector.val();

		// dont do anything if there is no type set
		if (!type) {
			return;
		};

		// clear current mapping list
		propertySelector.empty();

		$.post(Structr.rootUrl + 'File/' + file.id + '/getCSVHeaders', JSON.stringify({

			delimiter: $('#delimiter').val(),
			quoteChar: $('#quote-char').val(),
			recordSeparator: $('#record-separator').val()

		}), function(csvHeaders) {

			propertySelector.append('<h3>Select Mapping</h3>');
			propertySelector.append('<div class="attr-mapping"><table><thead><tr><th>Column name</th><th class="transform-head">Transformation (optional)</th><th></th></tr></thead><tbody id="row-container"></tbody></table></div>');

			var helpText = 'Specify optional StructrScript expression here to transform the input value.<br>The data key is &quot;input&quot; and the return value of the expression will be imported.<br><br><b>Example</b>: capitalize(input)';
			Structr.appendInfoTextToElement({
				text: helpText,
				element: $('th.transform-head', propertySelector),
				css: {
					marginLeft: "2px"
				}
			});

			if (csvHeaders && csvHeaders.result && csvHeaders.result.headers) {

				var names      = [];
				var typeConfig = {};

				if (data) {
					typeConfig['properties'] = data.mappings;
					typeConfig['mappings']   = data.transforms;
				};

				Importer.displayImportPropertyMapping(type, csvHeaders.result.headers, $('#row-container'), names, true, typeConfig, function() {

					$('#start-import').off('click').on('click', function() {

						var configInfo = Importer.collectCSVImportConfigurationInfo();
						var allowImport = (configInfo.errors.length === 0);

						if (!allowImport) {

							configInfo.errors.forEach(function(e) {
								new MessageBuilder().title(e.title).error(e.message).show();
							});

						} else {

							$.post(Structr.rootUrl + 'File/' + file.id + '/doCSVImport', JSON.stringify(configInfo.config));
						}
					});
				});
			}
		});
	},
	collectCSVImportConfigurationInfo: function (mixedMappings) {

		var info = {
			errors: []
		};

		// collect mappings and transforms
		var mappings   = {};
		var transforms = {};

		$('select.attr-mapping').each(function(i, elem) {

			var e     = $(elem);
			var name  = e.prop('name');
			var value = e.val();

			if (value && value.length) {
				if (!mappings[value]) {
					mappings[value] = name;
				} else {
					info.errors.push({
						title: "Import Configuration Error",
						message: "Duplicate mapping found: <strong>" + value + "</strong>"
					});
				}

				var transform = $('input#transform' + i).val();
				if (transform && transform.length) {
					transforms[value] = transform;
				}
			}
		});

		var importType = $('input[name=import-type]:checked').val();
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
	displayImportPropertyMapping: function(type, inputProperties, rowContainer, names, displayTransformInput, typeConfig, onLoadComplete, onSelect) {

		var config = {
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
	displayImportPropertyMappingWithConfig: function(config) {

		var blacklist = [
			'owner', 'ownerId', 'base', 'type', 'relType', 'createdBy', 'deleted', 'hidden', 'createdDate', 'lastModifiedDate',
			'visibleToPublicUsers', 'visibleToAuthenticatedUsers', 'visibilityStartDate', 'visibilityEndDate',
			'lastModifiedBy', 'createdBy', 'grantees', 'structrChangeLog'
		];

		if ($('input[name=import-type]:checked').val() === 'rel') {
			blacklist.push('id');
		};

		$.get(Structr.rootUrl + '_schema/' + config.type + '/all', function(typeInfo) {

			if (typeInfo && typeInfo.result) {

				// sort by name
				typeInfo.result.sort(function(a, b) {
					return a.jsonName > b.jsonName ? 1 : a.jsonName < b.jsonName ? -1 : 0;
				});

				var mapping = {};

				config.inputProperties.forEach(function(inputPropertyName, i) {

					var options        = '';
					var selectedString = '';
					var select         = $('select#key' + i);
					var longestMatch   = 0;
					config.names[i]    = inputPropertyName;

					// create drop-down list with pre-selected options
					typeInfo.result.forEach(function(info) {

						if (blacklist.indexOf(info.jsonName) === -1) {

							// match with longest target property wins
							if (Importer.checkSelection(config.typeConfig, inputPropertyName, info.jsonName) && info.jsonName.length > longestMatch) {

								selectedString             = ' selected="selected"';
								longestMatch               = info.jsonName.length;
								mapping[inputPropertyName] = info.jsonName;

							} else {
								selectedString = '';
							}

							options += '<option' + selectedString + '>' + info.jsonName + '</option>';
						}
					});

					// display selection
					config.rowContainer.append(
						'<tr>' +
							'<td class="key">' + inputPropertyName + '</td>' +
							(config.displayTransformInput ?
								'<td class="transform"><input type="text" name="' +
								inputPropertyName +
								'" id="transform' +
								i +
								'" value="' +
								(config.typeConfig && config.typeConfig.mappings && config.typeConfig.mappings[inputPropertyName] ? config.typeConfig.mappings[inputPropertyName] : '') +
								'" /></td>' : ''
							) +
							'<td>' +
								'<select class="attr-mapping" name="' + inputPropertyName +'" id="key' + i + '">' +
									'<option value="">-- skip --</option>' +
									options +
								'</select>' +
							'</td>' +
						'</tr>'
					);

					if (config.onSelect && typeof config.onSelect === "function") {
						select.on('change', config.onSelect);
					}
				});

				if (config.onLoadComplete && typeof config.onLoadComplete === "function") {
					config.onLoadComplete(mapping);
				}
			}
		});
	},
	checkSelection: function(typeConfig, sourceName, targetName) {

		if (typeConfig && typeConfig.properties) {

			return typeConfig.properties[sourceName] === targetName;

		} else if (sourceName && sourceName.length && targetName && targetName.length) {

			var src      = sourceName.toLowerCase().replace(/\W/g, '');
			var tgt      = targetName.toLowerCase().replace(/\W/g, '');
			return src === tgt;// || src.indexOf(tgt) >= 0 || tgt.indexOf(src) >= 0;
		}

		return false;
	},
	importXMLDialog: function(file) {

		var configuration = {};

		Structr.dialog('Import XML from ' + file.name, function() {}, function() {});

		Importer.initializeButtons(true, true, true, true, true);

		let html = Importer.templates.dialogConfigurations({type: 'xml'});
		dialogMeta.append(html);

		var importConfigSelector = $('#load-xml-config-selector');
		importConfigSelector.on('change', function () {
			Importer.configSelectorChangeHandler(importConfigSelector, 'xml');
		});
		Importer.configSelectorChangeHandler(importConfigSelector, 'xml');

		Importer.updateConfigSelector(importConfigSelector, 'xml');

		$('#load-xml-config-button').on('click', function() {

			Importer.loadImportConfiguration(importConfigSelector, function(data) {

				if (data && data.content) {

					var config = JSON.parse(data.content);

					Object.keys(config).forEach(function(k) {
						configuration[k] = config[k];

						switch (configuration[k].action) {
							case 'createNode':
								Importer.updateStructureSelector('', k, configuration[k].type);
								break;

							case 'setProperty':
								Importer.updateStructureSelectorForSetProperty('', k, configuration[k].propertyName);
								break;

							default:
								console.log("Unknown action: ", configuration[k].action);
						}
					});
				}
			});
		});

		dialog.append('<div id="xml-import"></div>');

		$('#cancel-button').on('click', function() {
			// close dialog
			$.unblockUI({
				fadeOut: 25
			});
			Importer.unload();
		});

		$('#next-element').on('click', function() {
			var elem = $('td.xml-mapping.selected').parent('tr').next().children('td.xml-mapping');
			if (elem && elem.get(0)) {
				elem.get(0).scrollIntoView(false);
				elem.click();
			}
		});

		$('#prev-element').on('click', function() {
			var elem = $('td.xml-mapping.selected').parent('tr').prev().children('td.xml-mapping');
			if (elem && elem.get(0)) {
				elem.get(0).scrollIntoView(false);
				elem.click();
			}
		});

		$('#start-import').off('click').on('click', function() {

			$.post(Structr.rootUrl + 'File/' + file.id + '/doXMLImport', JSON.stringify(configuration), function(data) {});
		});

		$('#save-xml-config-button').on('click', function() {
			Importer.saveImportConfiguration(importConfigSelector, 'xml', configuration);
		});

		$('#update-xml-config-button').on('click', function() {
			Importer.updateImportConfiguration(importConfigSelector, configuration);
		});

		$('#delete-xml-config-button').on('click', function() {
			Importer.deleteImportConfiguration(importConfigSelector, 'xml');
		});

		var container = $('#xml-import');

		container.append('<div id="left"><h2>Document Structure</h2><div class="xml-mapping"><table><thead id="structure"></thead></table></div></div><div id="right"><div id="xml-config"></div></div>');
		container.append('<div style="clear: both;"></div>');

		var xmlConfig = $('#xml-config');

		xmlConfig.append(
			'<p class="hint">' +
			'Please click one of the XML elements on the left to configure the XML import for that element.<br /><br />' +
			'Use the &laquo;Next&raquo; and &laquo;Prev&raquo; buttons below to step through the XML elements.' +
			'</p>');

		$.post(Structr.rootUrl + 'File/' + file.id + '/getXMLStructure', {}, function(data) {

			if (data && data.result) {

				var structure  = JSON.parse(data.result);
				var attributes = {};

				function buildTree(htmlElement, parentKey, treeElement, path, level) {

					Object.keys(treeElement).forEach(function(key) {

						// store attributes
						if (key === '::attributes') {
							if (!attributes[parentKey]) {
								attributes[parentKey] = {};
							}
							var map = attributes[parentKey];
							treeElement[key].forEach(function(attr) {
								map[attr] = 1;
							});
							return;
						}


						var hasChildren = Object.keys(treeElement[key]).length > 1;
						var localPath   = path + '/' + key;

						htmlElement.append(
							'<tr><td data-name="' + localPath + '" data-level="' + level + '"' +
							' class="xml-mapping" ' +
							' style="padding-left: ' + (level * 30) + 'px;">' +
							_Icons.getHtmlForIcon(_Icons.collapsed_icon) +
							'&nbsp;&nbsp;' + key + '</td></tr>'
						);

						$('td[data-name="' + localPath + '"]').on('click', function() {

							$('td.xml-mapping').removeClass('selected');
							$(this).addClass('selected');

							xmlConfig.empty();
							xmlConfig.append('<h2>&nbsp;</h2>');
							xmlConfig.append('<div id="config"></div>');

							var config = $('#config');
							config.append('<label>Select action:</label>');
							config.append('<select id="action-select" class="xml-config-select"></select>');

							var action = $('#action-select');
							action.append('<option value="">Skip</option>');
							action.append('<option value="ignore">Ignore branch</option>');
							action.append('<option value="createNode">Create node</option>');
							action.append('<option value="setProperty">Set property</option>');

							config.append('<div id="options"></div>');
							var options = $('#options');

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
							var typeConfig = configuration[localPath];
							if (typeConfig && typeConfig.action) {

								$('#action-select').val(typeConfig.action).trigger('change');
							}
						});

						var value = treeElement[key];
						if (value) {

							buildTree(htmlElement, key, value, localPath, level + 1);
						}
					});
				}

				buildTree($('#structure'), '', structure, '', 0);
			}
		});
	},
	hasRoot: function(configuration) {
		var hasRoot = false;
		Object.keys(configuration).forEach(function(k) {
			var typeConfig = configuration[k];
			hasRoot |= typeConfig && typeConfig.isRoot;
		});
		return hasRoot;
	},
	isRoot: function(configuration, path) {
		var typeConfig = configuration[path];
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
	getParentType: function(path, configuration) {

		var parts = path.split('/');
		var index = parts.length;

		while (--index >= 0) {

			var fullPath = parts.slice(0, index).join('/');
			if (configuration[fullPath] && configuration[fullPath].type) {
				return configuration[fullPath].type;
			}
		}

		return undefined;

	},
	showCreateNodeOptions: function(el, key, path, structure, configuration, attributes, hasChildren) {

		if (!configuration[path]) {
			configuration[path] = {};
		}

		configuration[path].action = 'createNode';

		var isRoot  = Importer.isRoot(configuration, path);
		var hasRoot = Importer.hasRoot(configuration);

		if (!hasRoot) {
			Importer.setRoot(configuration, path);
			isRoot = true;
		}

		el.append('<label>Select type:</label>');
		el.append('<select id="type-select" class="xml-config-select"><option>-- select --</option></select>');

		if (!isRoot) {
			el.append('<div id="non-root-options"></div>');
		}

		el.append('<div id="property-select"></div>');

		var typeSelector     = $('#type-select');
		var propertySelector = $('#property-select');
		var typeConfig       = configuration[path];

		$.get(Structr.rootUrl + 'SchemaNode?' + Structr.getRequestParameterName('sort') + '=name', function(data) {

			if (data && data.result) {

				data.result.forEach(function(r) {

					typeSelector.append('<option value="' + r.name + '">' + r.name + '</option>');
				});

				// trigger select event when an element is already configured
				if (typeConfig && typeConfig.type) {
					typeSelector.val(typeConfig.type).trigger('change');
				}
			}
		});

		typeSelector.on('change', function(e) {

			var type  = $(this).val();
			var names = [];

			Importer.updateStructureSelector(key, path, type);

			if (!isRoot) {

				var parentType = Importer.getParentType(path, configuration);
				if (parentType) {

					var nonRoot    = $('#non-root-options');

					nonRoot.empty();
					nonRoot.append('<label>Select property name:</label>');
					nonRoot.append('<select id="name-select" class="xml-config-select"></select>');

					var nameSelect    = $('#name-select');

					//fetchPropertyList(type, typeConfig, key, select, loadCallback, changeCallback, appendCallback) {
					Importer.fetchPropertyList(parentType, typeConfig, '', nameSelect, function() {

						// trigger select event when an element is already configured
						if (typeConfig && typeConfig.propertyName) {
							nameSelect.val(typeConfig.propertyName);
						}
						nameSelect.trigger('change');

					}, function() {

						var option       = nameSelect.children(':selected');
						var isCollection = $(option).data('isCollection');
						var value        = $(option).val();

						if (value && value.length) {

							configuration[path].propertyName = value;
							if (!isCollection) {

								configuration[path].multiplicity = "1";
							}
						}

					}, function(info, selectedString) {
						if (info.type === type) {
							nameSelect.append('<option ' + selectedString + ' data-is-collection="' + info.isCollection + '">' + info.jsonName + '</option>');
						}
					});

					// allow text content of a node to be stored
					if (!hasChildren) {

						nonRoot.append('<label>Select property for text content:</label>');
						nonRoot.append('<select id="content-select" class="xml-config-select"></select>');

						var contentSelect = $('#content-select');

						//fetchPropertyList(type, typeConfig, key, select, loadCallback, changeCallback, appendCallback) {
						Importer.fetchPropertyList(type, typeConfig, '', contentSelect, function() {

							// trigger select event when an element is already configured
							if (typeConfig && typeConfig.content) {
								nameSelect.val(typeConfig.content);
							}

							contentSelect.trigger('change');

						}, function() {

							var value = contentSelect.val();
							if (value && value.length) {

								configuration[path].content = value;
							}

						}, function(info, selectedString) {
							if (info.type === 'String') {
								contentSelect.append('<option ' + selectedString + '">' + info.jsonName + '</option>');
							}
						});
					}

					nonRoot.append('<label>Use batching for this type</label>');
					nonRoot.append('<input type="checkbox" id="batching-checkbox" />');

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
			propertySelector.append('<h3>Attribute mapping</h3>');
			propertySelector.append('<div class="attr-mapping"><table><tbody id="row-container"></tbody></table></div>');

			var rowContainer    = $('#row-container');
			var inputProperties = [];
			if (attributes[key]) {
				inputProperties = Object.keys(attributes[key]);
			}

			//displayImportPropertyMapping: function(type, inputProperties, rowContainer, names, callback) {
			Importer.displayImportPropertyMapping(type, inputProperties, rowContainer, names, false, configuration[path], function(mapping) {

				var typeConfig = configuration[path];
				if (!typeConfig) {

					typeConfig = {};
					configuration[path] = typeConfig;
				}

				typeConfig.action     = 'createNode';
				typeConfig.type       = type;
				typeConfig.properties = {};

				Object.keys(mapping).forEach(function(mappedKey) {
					typeConfig.properties[mappedKey] = mapping[mappedKey];
				});

			}, function() {

				// on select
				var select = $(this);
				var name   = select.attr('name');
				var value  = select.val();

				if (name && name.length) {

					var typeConfig = configuration[path];
					typeConfig.properties[name] = value;
				}
			});

		});
	},
	showSetPropertyOptions: function(el, key, path, structure, configuration, attributes) {

		if (!configuration[path]) {
			configuration[path] = {};
		}

		configuration[path].action = 'setProperty';

		var parentType = Importer.getParentType(path, configuration);
		if (!parentType) {

			el.append('<p class="hint">Action &laquo;setProperty&raquo; cannot be used without enclosing &laquo;createNode&raquo; action.</p>');

		} else {

			el.append('<label>Select property for text content:</label>');
			el.append('<select id="text-select" class="xml-config-select"><option value="">--- ignore ---</option></select>');

			var textSelect = $('#text-select');
			var typeConfig = configuration[path];

			Importer.fetchPropertyList(parentType, typeConfig, '', textSelect, function() {

				// trigger select event when an element is already configured
				if (typeConfig && typeConfig.propertyName) {
					textSelect.val(typeConfig.propertyName).trigger('change');
				}

			}, function() {

				var value = textSelect.val();

				if (value && value.length) {
					configuration[path].propertyName = value;
					Importer.updateStructureSelector(key, path, value);
				}
			});
		}
	},
	fetchPropertyList: function(type, typeConfig, key, select, loadCallback, changeCallback, appendCallback) {

		var blacklist = [
			'id', 'owner', 'ownerId', 'base', 'type', 'createdBy', 'deleted', 'hidden', 'createdDate', 'lastModifiedDate',
			'visibleToPublicUsers', 'visibleToAuthenticatedUsers', 'visibilityStartDate', 'visibilityEndDate',
			'lastModifiedBy', 'createdBy', 'grantees', 'structrChangeLog'
		];

		$.get(Structr.rootUrl + '_schema/' + type + '/all', function(typeInfo) {

			if (typeInfo && typeInfo.result) {

				// sort by name
				typeInfo.result.sort(function(a, b) {
					return a.jsonName > b.jsonName ? 1 : a.jsonName < b.jsonName ? -1 : 0;
				});

				var selectedString = '';
				var longestMatch   = 0;

				// create drop-down list with pre-selected options
				typeInfo.result.forEach(function(info) {

					// skip names that are blacklisted
					if (blacklist.indexOf(info.jsonName) >= 0) {
						return;
					}

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
						select.append('<option' + selectedString + '>' + info.jsonName + '</option>');
					}
				});

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
	updateStructureSelector: function(key, path, value) {
		var elem = $('td.xml-mapping[data-name="' + path + '"]');
		elem.empty();
		if (value && value.length) {
			elem.append('<b>' + _Icons.getHtmlForIcon(_Icons.collapsed_icon) + '&nbsp;&nbsp;' + value + '</b>');
		} else if (key && key.length) {
			elem.append(_Icons.getHtmlForIcon(_Icons.collapsed_icon) + '&nbsp;&nbsp;' + key);
		}
	},
	updateStructureSelectorForSetProperty: function(key, path, propertyName) {
		var elem = $('td.xml-mapping[data-name="' + path + '"]');
		elem.empty();
		elem.append('<b>' + _Icons.getHtmlForIcon(_Icons.collapsed_icon) + '&nbsp;&nbsp;' + propertyName + '</b>');
	},




	/***************************************************************************************************************************************************************************************/
	/* BEGIN NEW MIXED DATA IMPORT DIALOG */
















	formatMixedImportDialog: function(file, mixedMappingConfig) {

		Structr.appendInfoTextToElement({
			text: "Use the select box labelled &quot;Add target type..&quot; to add type mappings.",
			element: $('#type-mapping-title'),
			css: {
				marginLeft: "2px"
			}
		});

		var targetTypeSelector = $('#target-type-select');
		let customOnlyCheckbox = $('input#target-type-custom-only');

		if (Importer.customTypesOnly) {
			customOnlyCheckbox.prop('checked', true);
		}

		Importer.updateSchemaTypeCache(targetTypeSelector);

		$('#types-container').empty();
		$('#start-import').off('click');

		customOnlyCheckbox.off('change').on('change', function() {
			Importer.customTypesOnly = $(this).prop('checked');
			Importer.updateSchemaTypeSelector(targetTypeSelector);
			$('#types-container').empty();
			$('#start-import').off('click');
		});

		Importer.updateSchemaTypeSelector(targetTypeSelector);

		// collect CSV headers to use
		$.post(Structr.rootUrl + 'File/' + file.id + '/getCSVHeaders', JSON.stringify({

			delimiter: $('#delimiter').val(),
			quoteChar: $('#quote-char').val(),
			recordSeparator: $('#record-separator').val()

		}), function(csvHeaders) {

			if (csvHeaders && csvHeaders.result && csvHeaders.result.headers) {

				csvHeaders.result.headers.forEach(p => mixedMappingConfig.availableProperties[p] = {
					name: p,
					matched: false
				});

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
	removeMixedTypeMapping: function(id, mixedMapping) {

		// remove "matched" flag
		Object.keys(mixedMapping.mappedTypes[id].properties).forEach(key => {
			mixedMapping.availableProperties[key].matched = false;
		});

		delete mixedMapping.mappedTypes[id];

		Importer.displayMixedMappingConfiguration(mixedMapping);
	},
	updateMixedMapping: function(id, file, data, mixedMappingConfig) {

		var targetTypeSelector = $('#target-type-select');
		var matchingContainer  = $('#matching-properties-' + id);
		var availableContainer = $('#available-properties-' + id);
		var type               = targetTypeSelector.val();

		// dont do anything if there is no type set
		if (!type) {
			return;
		};

		// add type to mixed mapping config
		mixedMappingConfig.mappedTypes[id] = {
			name: id,
			properties: {},
			relationships: []
		};

		// clear current mapping list
		availableContainer.empty();
		matchingContainer.empty();

		var typeConfig = {};

		if (data) {
			typeConfig['properties'] = data.mappings;
			typeConfig['mappings']   = data.transforms;
		};

		var config = {
			type: type,
			availablePropertyContainer: availableContainer,
			matchingPropertyContainer: matchingContainer,
			names: [],
			displayTransformInput: false,
			typeConfig: typeConfig,
			mixedMappingConfig: mixedMappingConfig,
			displayMatchingPropertiesOnly: true,
			onLoadComplete: function() {

				$('#start-import').off('click').on('click', function() {

					var configInfo = Importer.collectCSVImportConfigurationInfo();
					var allowImport = (configInfo.errors.length === 0);

					if (!allowImport) {

						configInfo.errors.forEach(function(e) {
							new MessageBuilder().title(e.title).error(e.message).show();
						});

					} else {

						configInfo.config.mixedMappings = mixedMappingConfig.mappedTypes;

						$.post(Structr.rootUrl + 'File/' + file.id + '/doCSVImport', JSON.stringify(configInfo.config));
					}
				})
			}
		};

		Importer.updateAdvancedImportPropertyMapping(config);
	},
	updateAdvancedImportPropertyMapping: function(config) {

		var blacklist = [
			'owner', 'ownerId', 'base', 'type', 'relType', 'createdBy', 'deleted', 'hidden', 'createdDate', 'lastModifiedDate',
			'visibleToPublicUsers', 'visibleToAuthenticatedUsers', 'visibilityStartDate', 'visibilityEndDate',
			'lastModifiedBy', 'createdBy', 'grantees', 'structrChangeLog'
		];

		if ($('input[name=import-type]:checked').val() === 'rel') {
			blacklist.push('id');
		};

		$.get(Structr.rootUrl + '_schema/' + config.type + '/all', function(typeInfo) {

			if (typeInfo && typeInfo.result) {

				// sort by name
				typeInfo.result.sort(function(a, b) {
					return a.jsonName > b.jsonName ? 1 : a.jsonName < b.jsonName ? -1 : 0;
				});

				var mapping = config.mixedMappingConfig;

				Object.keys(mapping.availableProperties).forEach(function(key, i) {

					let inputProperty = mapping.availableProperties[key];

					var inputPropertyName = inputProperty.name;
					var longestMatch      = 0;
					var matchFound        = false;
					config.names[i]       = inputPropertyName;

					// create drop-down list with pre-selected options
					typeInfo.result.some(info => {

						if (blacklist.indexOf(info.jsonName) === -1) {

							// match with longest target property wins
							if (Importer.checkSelection(config.typeConfig, inputPropertyName, info.jsonName) && info.jsonName.length > longestMatch) {

								var mappedType = mapping.mappedTypes[config.type];

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
	displayMixedMappingConfiguration: function(mixedMapping) {

		let availableProperties = $('#available-properties');
		let mappedTypes         = $('#types-container');
		let options             = '<option>Add relationship</option>';

		availableProperties.empty();
		mappedTypes.empty();

		for (let key of Object.keys(mixedMapping.availableProperties)) {

			let property = mixedMapping.availableProperties[key];

			if (property.matched === false) {
				availableProperties.append('<span>' + property.name + '</span>');
			}
		}

		for (let key of Object.keys(mixedMapping.mappedTypes)) {

			options += '<option>' + key + '</option>';
		}

		for (let key of Object.keys(mixedMapping.mappedTypes)) {

			let mappedType = mixedMapping.mappedTypes[key];

			let html = Importer.templates.snippetTypeContainer({ type: mappedType.name, id: mappedType.name });
			mappedTypes.append(html);

			let propertyContainer     = $('#matching-properties-' + mappedType.name);
			let relationshipContainer = $('#relationships-' + mappedType.name);

			for (let propertyName of Object.keys(mappedType.properties)) {

				let html = Importer.templates.snippetMappingRow({ name: propertyName });
				propertyContainer.append(html);
			}

			for (let propertyName of mappedType.relationships) {

				let html = Importer.templates.snippetMappingRow({ name: propertyName });
				relationshipContainer.append(html);
			}

			$('#remove-button-' + mappedType.name).off('click').on('click', function(e) {
				Importer.removeMixedTypeMapping(mappedType.name, mixedMapping);
			});

			let addRelationshipContainer = $('#add-relationship-' + mappedType.name);

			addRelationshipContainer.append('<select class="add-selector" id="' + mappedType.name + '-related-to">' + options + '</select>');

			$('#' + mappedType.name + '-related-to').off('change').on('change', function(e) {

				mappedType.relationships.push($(this).val());
				Importer.displayMixedMappingConfiguration(mixedMapping);
			});
		}
	},

	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/crud.css">
			<link rel="stylesheet" type="text/css" media="screen" href="css/importer.css">
			
			<div id="importer-main" class="resourceBox full-height-box">
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
					${_Icons.getSvgIcon('refresh-arrows', 16, 16, 'mr-2')} Refresh
				</button>
			
				<div class="button-and-input inline-flex items-center">

					<label for="cancel-all-queued-after-job-id" class="mr-2">
						Cancel ALL <b>queued</b> jobs after this ID:
					</label>

					<input size="6" type="text" id="cancel-all-queued-after-job-id" placeholder="Job ID">

					<button id="cancel-all-queued-after" class="inline-flex items-center ml-2 hover:bg-gray-100 focus:border-gray-666 active:border-green">
						${_Icons.getSvgIcon('close-dialog-x', 16, 16, _Icons.getSvgIconClassesForColoredIcon(['mr-2', 'icon-red']))} Cancel
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
								<label>Delimiter:</label>
								<select id="delimiter" class="import-option">
									<option ${(config.delim === ',' ? 'selected' : '')}>,</option>
									<option ${(config.delim === ';' ? 'selected' : '')}>;</option>
									<option ${(config.delim === '|' ? 'selected' : '')}>|</option>
								</select>
							</td>
							<td valign="top">
								<label>Quote character:</label>
								<select id="quote-char" class="import-option">
									<option ${(config.qc === '' ? 'selected' : '')}></option>
									<option ${(config.qc === '"' ? 'selected' : '')}>&quot;</option>
									<option ${(config.qc === '\'' ? 'selected' : '')}>\'</option>
								</select>
							</td>
							<td valign="top">
								<label>Record separator:</label>
								<select id="record-separator" class="import-option">
									<option ${(config.data.result.separator ===    'LF' ? 'selected' : '')}>LF</option>
									<option ${(config.data.result.separator ===    'CR' ? 'selected' : '')}>CR</option>
									<option ${(config.data.result.separator === 'CR+LF' ? 'selected' : '')}>CR+LF</option>
								</select>
							</td>
							<td valign="top">
								<label>Commit interval:</label>
								<input type="number" id="commit-interval" value="1000" placeholder="1000" title="Enter 0 to disable periodic commit.">
							</td>
						</tr>
						<tr id="options-row2">
							<td valign="top">
								<label>Import type:</label><input type="radio" name="import-type" value="node" ${(config.importType === 'node' ? 'checked' : '')}> Node<br>
								<label></label><input type="radio" name="import-type" value="rel" ${(config.importType === 'rel' ? 'checked' : '')}> Relationship<br>
								<label></label><input type="radio" name="import-type" value="graph" ${(config.importType === 'graph' ? 'checked' : '')}> Mixed
							</td>
							<td valign="top">
								<input type="checkbox" id="rfc4180-mode" /><label for="rfc4180-mode">RFC4180 mode</label><br>
								<input type="checkbox" id="strict-quotes" /><label for="strict-quotes">Strict quotes</label><br>
								<input type="checkbox" id="ignore-invalid" /><label for="ignore-invalid">Ignore invalid lines</label><br>
								<input type="checkbox" id="distinct" /><label for="distinct">Skip duplicates</label>
							</td>
							<td colspan="2" valign="top">
								<label for="range">Line range:</label>
								<input type="text" id="range" title="Enter range (0-100)." placeholder="e.g. 1-100 or 1,2,3-10" />
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
		`,
		wizardCSV: config => `
			<div>
				<div id="sample">
					<h3>Data Sample</h3>
					<pre class="csv-preview">${config.data.result.lines}</pre>
				</div>
				<h3>Import Options</h3>
				<table id="csv-import">
					<tbody>
						<tr id="options-row1">
							<td>
								<label for="delimiter">Delimiter:</label>
								<select id="delimiter" class="import-option">
									<option ${(config.delim === ',' ? 'selected' : '')}>,</option>
									<option ${(config.delim === ';' ? 'selected' : '')}>;</option>
									<option ${(config.delim === '|' ? 'selected' : '')}>|</option>
								</select>
							</td>
							<td>
								<label for="quote-char">Quote character:</label>
								<select id="quote-char" class="import-option">
									<option ${(config.qc === '' ? 'selected' : '')}></option>
									<option ${(config.qc === '"' ? 'selected' : '')}>&quot;</option>
									<option ${(config.qc === '\'' ? 'selected' : '')}>\'</option>
								</select>
							</td>
							<td>
								<label for="record-separator">Record separator:</label>
								<select id="record-separator" class="import-option">
									<option ${(config.data.result.separator ===    'LF' ? 'selected' : '')}>LF</option>
									<option ${(config.data.result.separator ===    'CR' ? 'selected' : '')}>CR</option>
									<option ${(config.data.result.separator === 'CR+LF' ? 'selected' : '')}>CR+LF</option>
								</select>
							</td>
						</tr>
						<tr id="options-row2">
							<td>
								<label for="commit-interval">Commit interval:</label>
								<input type="number" id="commit-interval" value="1000" placeholder="1000" title="Enter 0 to disable periodic commit.">
							</td>
							<td>
								<input type="checkbox" id="strict-quotes" /><label for="strict-quotes">Strict Quotes</label><br>
								<input type="checkbox" id="ignore-invalid" /><label for="ignore-invalid">Ignore invalid lines</label>
							</td>
							<td>
								<label for="range">Line range:</label>
								<input type="text" id="range" title="Enter range (0-100)." placeholder="e.g. 1-100 or 1,2,3-10" />
							</td>
						</tr>
						<tr id="options-row3">
							<td>
								<label for="import-type">Import type:</label><input type="radio" id="import-type" name="import-type" value="node" ${(config.importType === 'node' ? 'checked' : '')}> Node<br>
								<label></label><input type="radio" name="import-type" value="rel" ${(config.importType === 'rel' ? 'checked' : '')}> Relationship
							</td>
							<td>
								<label for="distinct">Skip duplicates:</label>
								<input type="checkbox" id="distinct" /><br>
							</td>
							<td>
							</td>
						</tr>
					</tbody>
				</table>
				<h3>Select target type</h3>
				<select id="target-type-select" name="targetType">
					<option value="" disabled="disabled" selected="selected">Select target type..</option>
				</select>
				<span><input type="checkbox" id="target-type-custom-only" checked><label for="target-type-custom-only">Only show custom types</label></span>
				<div id="property-select"></div>
			</div>
		`,
	}
};