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

var Importer = {

	importCSVDialog: function(file) {

		Structr.dialog('Import CSV from ' + file.name, function() {}, function() {});

		dialog.append('<div id="csv-import"><div id="sample"></div></div>');

		var container       = $('#csv-import');
		var sample          = $('#sample');

		// load first lines to display a sample of the data
		$.post(rootUrl + 'File/' + file.id + '/getFirstLines', {}, function(data) {

			if (data && data.result) {

				sample.append('<h3>Data Sample</h3>');
				sample.append('<pre class="csv-preview">' + data.result.lines + '</pre>');

				$('#record-separator').append(
					'<option ' + (data.result.separator ===    'LF' ? 'selected="selected"' : '') + '>LF</option>' +
					'<option ' + (data.result.separator ===    'CR' ? 'selected="selected"' : '') + '>CR</option>' +
					'<option ' + (data.result.separator === 'CR+LF' ? 'selected="selected"' : '') + '>CR+LF</option>'
				);
			}
		});

		// import options
		container.append('<h3>Import Options</h3>');
		container.append('<label>Delimiter: <select id="delimiter"><option>,</option><option>;</option><option>|</option></select></label>');
		container.append('<label>Quote character: <select id="quote-char"><option>&quot;</option></select></label>');
		container.append('<label>Record separator: <select id="record-separator"></select></label>');

		// target selection
		container.append('<h3>Select target type</h3>');
		container.append('<select id="target-type-select" name="targetType"><option value="">Select target type..</option></select>');
		container.append('<div id="property-select"></div>');

		var targetTypeSelector = $('#target-type-select');
		var propertySelector   = $('#property-select');

		$.get(rootUrl + 'SchemaNode?sort=name', function(data) {

			if (data && data.result) {

				data.result.forEach(function(r) {

					targetTypeSelector.append('<option value="' + r.name + '">' + r.name + '</option>');
				});
			}
		});

		targetTypeSelector.on('change', function(e) {

			var type      = $(this).val();

			propertySelector.empty();

			$.post(rootUrl + 'File/' + file.id + '/getCSVHeaders', JSON.stringify({
				delimiter: $('#delimiter').val(),
				quoteChar: $('#quote-char').val(),
				recordSeparator: $('#record-separator').val()
			}), function(csvHeaders) {

				propertySelector.append('<h3>Select Mapping</h3>');
				propertySelector.append('<div class="attr-mapping"><table><thead><tr><th>Column name</th><th>Transformation (optional)</th><th></th></tr></thead><tbody id="row-container"></tbody></table></div>');

				if (csvHeaders && csvHeaders.result && csvHeaders.result.headers) {

					var names = [];

					Structr.displayImportPropertyMapping(type, csvHeaders.result.headers, $('#row-container'), names, true, {}, function() {

						propertySelector.append('<div style="text-align: center"><button id="start-import">Start import</button></div>');
						$('#start-import').on('click', function() {

							var mappings = {};
							var transforms = {};

							$('select.csv').each(function(i, elem) {

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
								mappings: mappings,
								transforms: transforms
							}), function(data) {
								$.unblockUI({
									fadeOut: 25
								});
							});
						});
					});
				}
			});
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
							'<td class="transform"><input type="text" id="transform' + i + '" title="' +
							'Specify optional StructrScript expression here to\n' +
							'transform the input value. The data key is &quot;input&quot;\n' +
							'and the return value of the expression will be\nimported.' +
							'" /></td>' : '') +
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
							if (checkSelection(inputPropertyName, info.jsonName) && info.jsonName.length > longestMatch) {

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

		function checkSelection(sourceName, targetName) {

			if (typeConfig && typeConfig.properties) {

				return typeConfig.properties[sourceName] === targetName;

			} else {

				var src     = sourceName.toLowerCase().replace(/\W/g, '');
				var tgt     = targetName.toLowerCase().replace(/\W/g, '');
				return src == tgt || src.indexOf(tgt) >= 0 || tgt.indexOf(src) >= 0;
			}
		}
	},
	importXMLDialog: function(file) {

		var configuration = {};

		Structr.dialog('Import XML from ' + file.name, function() {}, function() {});
		dialog.append('<div id="xml-import"></div>');

		dialogBtn.empty();
		dialogBtn.append('<button id="start-import">Start import</button>');
		dialogBtn.append('<button id="next-element">Next</button>');
		dialogBtn.append('<button id="prev-element">Previous</button>');
		dialogBtn.append('<button id="cancel-button">Cancel</button>');

		$('#cancel-button').on('click', function() {
			// close dialog
			$.unblockUI({
				fadeOut: 25
			});
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

			console.log(configuration);
			/*
			$.post(rootUrl + 'File/' + file.id + '/doXMLImport', JSON.stringify(configuration), function(data) {
				$.unblockUI({
					fadeOut: 25
				});
			});
			*/
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

						var cleanedKey = key.replace(/:/g, '');
						var localPath  = path + cleanedKey + level;

						htmlElement.append('<tr><td data-name="' + key + '" class="xml-mapping" id="' + localPath + '" style="padding-left: ' + (level * 30) + 'px;">&#11208;&nbsp;&nbsp;' + key + '</td></tr>');
						$('#' + localPath).on('click', function() {

							$('td.xml-mapping').removeClass('selected');
							$(this).addClass('selected');

							xmlConfig.empty();
							xmlConfig.append('<h2>&nbsp;</h2>');
							xmlConfig.append('<div id="config"></div>');

							var config = $('#config');
							config.append('<label>Select action:</label>');
							config.append('<select id="action-select"></select>');

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
										Structr.showCreateNodeOptions(options, key, configuration, attributes);
										break;
									case "setProperty":
										Structr.showSetPropertyOptions(options, key, configuration, attributes);
										break;
									case "ignore":
										// reset configuration
										configuration[key] = { action: 'ignore' };
										break;
								}
							});

							// activate elements for existing configuration
							var typeConfig = configuration[key];
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

				buildTree($('#structure'), '', JSON.parse(data.result), 'xmlRootStructure', 0);
			}
		});
	},
	xmlImportHasRoot: function(configuration) {
		var hasRoot = false;
		Object.keys(configuration).forEach(function(k) {
			var typeConfig = configuration[k];
			hasRoot |= typeConfig && typeConfig.isRoot;
		});
		return hasRoot;
	},
	xmlImportIsRoot: function(configuration, key) {
		var typeConfig = configuration[key];
		return typeConfig && typeConfig.isRoot;
	},
	xmlImportSetRoot: function(configuration, key) {
		Object.keys(configuration).forEach(function(k) {
			var typeConfig = configuration[k];
			if (typeConfig) {
				typeConfig.isRoot = (k === key);
			}
		});
	},
	xmlImportGetParentType: function(configuration) {

		var elem = $('td.xml-mapping.selected').parent('tr').prev().children('td.xml-mapping');
		var name = elem.data('name');
		var parentType;

		while (elem && name && !parentType) {

			name = elem.data('name');
			if (name) {

				if (configuration && configuration[name] && configuration[name].action === 'createNode') {
					parentType = configuration[name].type;
				}
			}

			elem = elem.parent('tr').prev().children('td.xml-mapping');
		}

		return parentType;
	},
	showCreateNodeOptions: function(el, key, configuration, attributes) {

		if (!configuration[key]) {
			configuration[key] = {};
		}

		configuration[key].action = 'createNode';

		var isRoot = Structr.xmlImportIsRoot(configuration, key);
		var hasRoot = Structr.xmlImportHasRoot(configuration);

		if (!hasRoot) {
			Structr.xmlImportSetRoot(configuration, key);
			isRoot = true;
		}

		el.append('<label>Select type:</label>');
		el.append('<select id="type-select"><option>-- select --</option></select>');

		if (!isRoot) {
			el.append('<div id="non-root-options"></div>');
		}

		el.append('<div id="property-select"></div>');

		var typeSelector     = $('#type-select');
		var propertySelector = $('#property-select');
		var typeConfig       = configuration[key];

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

			if (!isRoot) {

				var parentType = Structr.xmlImportGetParentType(configuration);
				if (parentType) {

					var nonRoot    = $('#non-root-options');

					nonRoot.append('<label>Select property name:</label>');
					nonRoot.append('<select id="name-select"></select>');
					nonRoot.append('<label>Select property for text content:</label>');
					nonRoot.append('<select id="text-select"><option value="">--- ignore ---</option></select>');

					var nameSelect = $('#name-select');

					$.get(rootUrl + '_schema/' + parentType + '/all', function(typeInfo) {

						if (typeInfo && typeInfo.result) {

							// sort by name
							typeInfo.result.sort(function(a, b) {
								return a.jsonName > b.jsonName ? 1 : a.jsonName < b.jsonName ? -1 : 0;
							});

							typeInfo.result.forEach(function(info) {

								if (info.type === type) {

									nameSelect.append('<option data-is-collection="' + info.isCollection + '">' + info.jsonName + '</option>');
								}
							});

							nameSelect.on('change', function() {

								var option       = nameSelect.children(':selected');
								var isCollection = $(option).data('isCollection');
								var value        = $(option).val();

								if (value && value.length) {

									configuration[key].propertyName = value;
									if (!isCollection) {

										configuration[key].multiplicity = "1";
									}
								}
							});
						}
					});
				}
			}

			propertySelector.empty();
			propertySelector.append('<h3>Attribute mapping</h3>');
			propertySelector.append('<div class="attr-mapping"><table><tbody id="row-container"></tbody></table></div>');

			var rowContainer    = $('#row-container');
			var inputProperties = Object.keys(attributes[key]);

			//displayImportPropertyMapping: function(type, inputProperties, rowContainer, names, callback) {
			Structr.displayImportPropertyMapping(type, inputProperties, rowContainer, names, false, configuration[key], function(mapping) {

				var typeConfig = configuration[key];
				if (!typeConfig) {

					typeConfig = {};
					configuration[key] = typeConfig;
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

					var typeConfig = configuration[key];
					typeConfig.properties[name] = value;
				}
			});

		});
	},
	showSetPropertyOptions: function(el, key, configuration, attributes) {

		if (!configuration[key]) {
			configuration[key] = {};
		}

		configuration[key].action = 'setProperty';

	},
}