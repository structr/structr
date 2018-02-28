/*
 * Copyright (C) 2010-2018 Structr GmbH
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

		var mode = $(this).data('action');
		var jobId = $(this).data('jobId');

		Command.fileImport(mode, jobId, Importer.updateJobTable);

		return false;
	});

});

var Importer = {
	_moduleName: 'importer',
	appDataXMLKey: 'xml-import-config',
	appDataCSVKey: 'csv-import-config',
	timeout: undefined,

	init: function() {
		main = $('#main');
	},
	resize: function() {
	},
	onload: function() {
		Importer.init();

		Structr.fetchHtmlTemplate('importer/main', {}, function(html) {

			main.append(html);

			$('#importer-main .refresh').click(function () {
				Importer.updateJobTable();
			});

			Importer.updateJobTable();

			Structr.unblockMenu(100);

		});

	},
	unload: function() {

	},
	updateJobTable: function () {

		window.clearTimeout(Importer.timeout);

		window.setTimeout(function () {

			Command.fileImport("list", null, function (jobs) {

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
			dialogBtn.prepend('<button id="start-import">Start import</button>');
		}

		if (!close) {

			dialogCancelButton.addClass('hidden');

		} else {

			// bind restore buttons event to existing cancel button
			dialogCancelButton.on('click', Importer.restoreButtons);
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
	importCSVDialog: function(file) {

		Structr.dialog('Import CSV from ' + file.name, function() {}, function() {});

		Importer.initializeButtons(true, false, false, true);

		Structr.fetchHtmlTemplate('importer/dialog.configurations', {type: 'csv'}, function(html) {

			dialogBox.append(html);

			Command.appData('list', Importer.appDataCSVKey, null, null, function(result) {

				result[0].names.forEach(function(v) {
					$('#load-csv-config-name').append('<option>' + v + '</option>');
				});
			});

			$('#load-csv-config-button').on('click', function() {

				var name = $('#load-csv-config-name').val();
				if (name && name.length) {

					Command.appData('get', Importer.appDataCSVKey, name, null, function(result) {

						if (result && result.value) {

							var config = JSON.parse(result.value);

							$('#delimiter').val(config.delimiter);
							$('#quote-char').val(config.quoteChar);
							$('#record-separator').val(config.recordSeparator);
							$('#target-type-select').val(config.targetType).trigger('change', [config]);
							$('#commit-interval').val(config.commitInterval);
							$('#ignore-invalid').prop('checked', config.ignoreInvalid),
							$('#range').val(config.range);
						}
					});
				}
			});

			$('#save-csv-config').on('click', function() {

				var name = $('#csv-config-name').val();
				if (name && name.length) {

					// collect mappings and transforms
					var mappings   = {};
					var transforms = {};

					$('select.attr-mapping').each(function(i, elem) {

						var e     = $(elem);
						var name  = e.prop('name');
						var value = e.val();

						if (value && value.length) {
							mappings[name] = value;
						}

						var transform = $('input#transform' + i).val();
						if (transform && transform.length) {
							transforms[name] = transform;
						}
					});

					// mode, category, name, value, callback
					Command.appData('add', Importer.appDataCSVKey, name, JSON.stringify({
						delimiter: $('#delimiter').val(),
						quoteChar: $('#quote-char').val(),
						recordSeparator: $('#record-separator').val(),
						targetType: $('#target-type-select').val(),
						commitInterval: $('#commit-interval').val() || $('#commit-interval').attr('placeholder'),
						ignoreInvalid: $('#ignore-invalid').prop('checked'),
						range: $('#range').val(),
						mappings: mappings,
						transforms: transforms
					}));
				}
			});

			$('#delete-csv-config-button').on('click', function() {

				var name = $('#load-csv-config-name').val();
				if (name && name.length) {

					Command.appData('delete', Importer.appDataCSVKey, name, null, function(result) {
						$('#load-csv-config-name option:selected').remove();
					});
				}
			});



			// load first lines to display a sample of the data
			$.post(rootUrl + 'File/' + file.id + '/getFirstLines', {}, function(data) {

				if (data && data.result) {

					var results = Papa.parse(data.result.lines);
					var delim = results.meta.delimiter;
					var qc    = data.result.lines.substring(0,1);

					Structr.fetchHtmlTemplate('importer/dialog.csv', { data: data, delim: delim, qc: qc }, function(html) {

						var container = $(html);
						dialog.append(container);

						var targetTypeSelector = $('#target-type-select', container);

						$.get(rootUrl + 'SchemaNode?sort=name', function(data) {

							if (data && data.result) {

								data.result.forEach(function(r) {

									targetTypeSelector.append('<option value="' + r.name + '">' + r.name + '</option>');
								});
							}
						});

						targetTypeSelector.on('change', function(e, data) { Importer.updateMapping(file, data); });
						$(".import-option", container).on('change', function(e, data) { Importer.updateMapping(file, data); });
					});
				}
			});

		});

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

		$.post(rootUrl + 'File/' + file.id + '/getCSVHeaders', JSON.stringify({

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

					$('#start-import').on('click', function() {

						var mappings   = {};
						var transforms = {};

						$('select.attr-mapping').each(function(i, elem) {

							var e     = $(elem);
							var name  = names[i];
							var value = e.val();

							// property mappings need to be from source type to target name
							if (value && value.length) {
								mappings[value] = name;
							}

							var transform = $('input#transform' + i).val();
							if (transform && transform.length) {
								transforms[value] = transform;
							}
						});

						$.post(rootUrl + 'File/' + file.id + '/doCSVImport', JSON.stringify({
							targetType: type,
							delimiter: $('#delimiter').val(),
							quoteChar: $('#quote-char').val(),
							commitInterval: $('#commit-interval').val() || $('#commit-interval').attr('placeholder'),
							ignoreInvalid: $('#ignore-invalid').prop('checked'),
							range: $('#range').val(),
							mappings: mappings,
							transforms: transforms
						}));
					});
				});
			}
		});
	},
	displayImportPropertyMapping: function(type, inputProperties, rowContainer, names, displayTransformInput, typeConfig, onLoadComplete, onSelect) {

		var blacklist = [
			'id', 'owner', 'ownerId', 'base', 'type', 'createdBy', 'deleted', 'hidden', 'createdDate', 'lastModifiedDate',
			'visibleToPublicUsers', 'visibleToAuthenticatedUsers', 'visibilityStartDate', 'visibilityEndDate',
			'lastModifiedBy', 'createdBy', 'grantees', 'structrChangeLog'
		];

		$.get(rootUrl + '_schema/' + type + '/all', function(typeInfo) {

			if (typeInfo && typeInfo.result) {

				// sort by name
				typeInfo.result.sort(function(a, b) {
					return a.jsonName > b.jsonName ? 1 : a.jsonName < b.jsonName ? -1 : 0;
				});

				var mapping = {};

				inputProperties.forEach(function(inputPropertyName, i) {

					rowContainer.append(
						'<tr>' +
							'<td class="key">' + inputPropertyName + '</td>' +
							(displayTransformInput ?
								'<td class="transform"><input type="text" name="' +
								inputPropertyName +
								'" id="transform' +
								i +
								'" value="' +
								(typeConfig && typeConfig.mappings && typeConfig.mappings[inputPropertyName] ? typeConfig.mappings[inputPropertyName] : '') +
								'" /></td>' : ''
							) +
							'<td>' +
								'<select class="attr-mapping" name="' + inputPropertyName +'" id="key' + i + '">' +
									'<option value="">-- skip --</option>' +
								'</select>' +
							'</td>' +
						'</tr>'
					);

					var selectedString = '';
					var select         = $('select#key' + i);
					var longestMatch   = 0;
					names[i]           = inputPropertyName;

					// create drop-down list with pre-selected options
					typeInfo.result.forEach(function(info) {

						if (blacklist.indexOf(info.jsonName) === -1) {

							// match with longest target property wins
							if (Importer.checkSelection(typeConfig, inputPropertyName, info.jsonName) && info.jsonName.length > longestMatch) {

								selectedString             = ' selected="selected"';
								longestMatch               = info.jsonName.length;
								mapping[inputPropertyName] = info.jsonName;

							} else {
								selectedString = '';
							}

							select.append('<option' + selectedString + '>' + info.jsonName + '</option>');
						}
					});

					if (onSelect && typeof onSelect === "function") {
						select.on('change', onSelect);
					}
				});

				if (onLoadComplete && typeof onLoadComplete === "function") {
					onLoadComplete(mapping);
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

		Structr.fetchHtmlTemplate('importer/dialog.configurations', {type: 'xml'}, function(html) {

			dialogBox.append(html);

			Command.appData('list', Importer.appDataXMLKey, null, null, function(result) {

				result[0].names.forEach(function(v) {
					$('#load-xml-config-name').append('<option>' + v + '</option>');
				});
			});

			$('#load-xml-config-button').on('click', function() {

				var name = $('#load-xml-config-name').val();
				if (name && name.length) {

					Command.appData('get', Importer.appDataXMLKey, name, null, function(result) {
						if (result && result.value) {
							var config = JSON.parse(result.value);
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
				}
			});

			dialog.append('<div id="xml-import"></div>');

			$('#cancel-button').on('click', function() {
				// close dialog
				$.unblockUI({
					fadeOut: 25
				});
				Importer.restoreButtons();
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

			$('#start-import').on('click', function() {

				$.post(rootUrl + 'File/' + file.id + '/doXMLImport', JSON.stringify(configuration), function(data) {});
			});

			$('#save-xml-config').on('click', function() {

				var name = $('#xml-config-name').val();
				if (name && name.length) {

					// mode, category, name, value, callback
					Command.appData('add', Importer.appDataXMLKey, name, JSON.stringify(configuration));
				}
			});

			$('#delete-xml-config-button').on('click', function() {

				var name = $('#load-xml-config-name').val();
				if (name && name.length) {

					Command.appData('delete', Importer.appDataXMLKey, name, null, function(result) {
						$('#load-xml-config-name option:selected').remove();
					});
				}
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

			$.post(rootUrl + 'File/' + file.id + '/getXMLStructure', {}, function(data) {

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

		$.get(rootUrl + 'SchemaNode?sort=name', function(data) {

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

		$.get(rootUrl + '_schema/' + type + '/all', function(typeInfo) {

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
	}
};