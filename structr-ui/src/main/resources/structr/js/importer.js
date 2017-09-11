/*
 * Copyright (C) 2010-2017 Structr GmbH
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

		Command.fileImport(mode, jobId);

		return false;
	});

});

var Importer = {
	_moduleName: 'importer',
	appDataXMLKey: 'xml-import-config',
	timeout: undefined,

	init: function() {
		main = $('#main');
	},
	resize: function() {
	},
	onload: function() {
		Importer.init();

		main.append(
			'<div id="importer-main" class="resourceBox full-height-box">' +
					'<button class="refresh">Refresh</button>' +
					'<table id="importer-jobs-table">' +
						'<thead><tr>' +
							'<th>Job ID</th>' +
							'<th>File UUID</th>' +
							'<th>File path</th>' +
							'<th>File size</th>' +
							'<th>User</th>' +
							'<th>Processed Chunks</th>' +
							'<th>Status</th>' +
							'<th>Action</th>' +
						'</tr></thead>' +
						'<tbody></tbody>' +
					'</table>' +
			'</div>'
		);

		$('#importer-main .refresh').click(function () {
			Importer.updateJobTable();
		});

		Importer.updateJobTable();

		Structr.unblockMenu(100);
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
					tbody.append('<td colspan=8>No import jobs</td>');
				}

			});

			window.clearTimeout(Importer.timeout);

		}, 250);


	},
	createRowForJob: function (job) {
		return $('<tr><td>' + job.jobId + '</td><td>' + job.fileUuid + '</td><td>' + job.filepath + '</td><td>' + job.filesize + '</td><td>' + job.username + '</td><td>' + job.processedChunks + '</td><td>' + job.status + '</td><td>' + Importer.createActionButtons(job) + '</td></tr>');
	},
	createActionButtons: function (job) {
		var actionHtml = '';

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

		return actionHtml;
	},
	createActionButton: function (action, jobId, content) {
		return '<button class="import-job-action" data-action="' + action + '" data-job-id="' + jobId + '">' + content + '</button>';
	},

	initializeButtons: function(start, next, prev, cancel, configurations, configuration) {

		dialogCancelButton.addClass('hidden');

		if (start) {
			dialogBtn.append('<button id="start-import">Start import</button>');
		}

		if (next) {
			dialogBtn.append('<button id="next-element">Next</button>');
		}

		if (prev) {
			dialogBtn.append('<button id="prev-element">Previous</button>');
		}

		if (cancel) {
			dialogBtn.append('<button id="cancel-button">Cancel</button>');
		}

		if (configurations) {

			dialogBox.append(
				'<div id="xml-configurations">' +
				'<select id="load-xml-config-name">' +
				'<option value="">--- Select configuration to load ---</option>' +
				'</select>' +
				'<button id="load-xml-config-button">Load</button>' +
				'<input id="xml-config-name" type="text" placeholder="Enter name for configuration" />' +
				'<button id="save-xml-config">Save</button>' +
				'</div>');

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
		}

	},
	restoreButtons: function() {
		dialogCancelButton.removeClass('hidden');
		$('#start-import').remove();
		$('#next-element').remove();
		$('#prev-element').remove();
		$('#cancel-button').remove();
		$('#xml-configurations').remove();
	},
	importCSVDialog: function(file) {

		Structr.dialog('Import CSV from ' + file.name, function() {}, function() {});

		dialog.append('<div id="csv-import"><div id="sample"></div></div>');

		Importer.initializeButtons(true, false, false, true);

		$('#cancel-button').on('click', function() {
			// close dialog
			$.unblockUI({
				fadeOut: 25
			});
			Importer.restoreButtons();
		});

		var container       = $('#csv-import');
		var sample          = $('#sample');

		// load first lines to display a sample of the data
		$.post(rootUrl + 'File/' + file.id + '/getFirstLines', {}, function(data) {

			if (data && data.result) {

				sample.append('<h3>Data Sample</h3>');
				sample.append('<pre class="csv-preview">' + data.result.lines + '</pre>');

				var results = Papa.parse(data.result.lines);
				var delim = results.meta.delimiter;
				var qc    = data.result.lines.substring(0,1);

				// import options
				container.append('<h3>Import Options</h3>');
				container.append('<label>Delimiter: <select id="delimiter" class="import-option"><option' + (delim === ',' ? ' selected' : '') + '>,</option><option' + (delim === ';' ? ' selected' : '') + '>;</option><option' + (delim === '|' ? ' selected' : '') + '>|</option></select></label>');
				container.append('<label>Quote character: <select id="quote-char" class="import-option"><option' + (qc === '"' ? ' selected' : '') + '>&quot;</option><option' + (qc === '\'' ? ' selected' : '') + '>\'</option></select></label>');
				container.append('<label>Record separator: <select id="record-separator" class="import-option"></select></label>');
				container.append('<label>Commit Interval: <input type="number" id="commit-interval" value="1000" placeholder="1000"></label>');

				// target selection
				container.append('<h3>Select target type</h3>');
				container.append('<select id="target-type-select" name="targetType"><option value="" disabled="disabled" selected="selected">Select target type..</option></select>');
				container.append('<div id="property-select"></div>');

				$('#record-separator').append(
					'<option ' + (data.result.separator ===    'LF' ? 'selected="selected"' : '') + '>LF</option>' +
					'<option ' + (data.result.separator ===    'CR' ? 'selected="selected"' : '') + '>CR</option>' +
					'<option ' + (data.result.separator === 'CR+LF' ? 'selected="selected"' : '') + '>CR+LF</option>'
				);

				var targetTypeSelector = $('#target-type-select');
				var propertySelector   = $('#property-select');

				$.get(rootUrl + 'SchemaNode?sort=name', function(data) {

					if (data && data.result) {

						data.result.forEach(function(r) {

							targetTypeSelector.append('<option value="' + r.name + '">' + r.name + '</option>');
						});
					}
				});

				var updateMapping = function() {

					var type = targetTypeSelector.val();
					if (!type) {
						return;
					};

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

							var names = [];

							Importer.displayImportPropertyMapping(type, csvHeaders.result.headers, $('#row-container'), names, true, {}, function() {

								$('#start-import').on('click', function() {

									var mappings = {};
									var transforms = {};

									$('select.attr-mapping').each(function(i, elem) {

										var e     = $(elem);
										var name  = names[i];
										var value = e.val();

										// property mappings need to be from source type to target type
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
										mappings: mappings,
										transforms: transforms
									}), function(data) {
										$.unblockUI({
											fadeOut: 25
										});
										Importer.restoreButtons();
									});
								});
							});
						}
					});
				};

				targetTypeSelector.on('change', updateMapping);
				$(".import-option", container).on('change', updateMapping);


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
							(displayTransformInput ? '<td class="transform"><input type="text" id="transform' + i + '" /></td>' : '') +
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

								selectedString = ' selected="selected"';
								longestMatch   = info.jsonName.length;
								mapping[inputPropertyName]     = info.jsonName;

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
		Importer.initializeButtons(true, true, true, true, true, configuration);
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

			$.post(rootUrl + 'File/' + file.id + '/doXMLImport', JSON.stringify(configuration), function(data) {
				$.unblockUI({
					fadeOut: 25
				});
				Importer.restoreButtons();
			});
		});

		/*
		'<input id="xml-config-name" type="text" placeholder="Enter name for configuration" />' +
		'<button id="save-xml-config">Save</button>' +
		*/
		$('#save-xml-config').on('click', function() {

			var name = $('#xml-config-name').val();
			if (name && name.length) {

				// mode, category, name, value, callback
				Command.appData('add', Importer.appDataXMLKey, name, JSON.stringify(configuration));
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

							console.log("HERE", localPath);

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