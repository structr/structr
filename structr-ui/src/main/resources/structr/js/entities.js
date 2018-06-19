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
var buttonClicked;

var _Entities = {
	activeElements: {},
	activeQueryTabPrefix: 'structrActiveQueryTab_' + port,
	activeEditTabPrefix: 'structrActiveEditTab_' + port,
	numberAttrs: ['position', 'size'],
	readOnlyAttrs: ['lastModifiedDate', 'createdDate', 'createdBy', 'id', 'checksum', 'size', 'version', 'relativeFilePath'],
	pencilEditBlacklist: ['html', 'body', 'head', 'title', 'script',  'input', 'label', 'button', 'textarea', 'link', 'meta', 'noscript', 'tbody', 'thead', 'tr', 'td', 'caption', 'colgroup', 'tfoot', 'col', 'style'],
	null_prefix: 'null_attr_',
	changeBooleanAttribute: function(attrElement, value, activeLabel, inactiveLabel) {

		_Logger.log(_LogType.ENTITIES, 'Change boolean attribute ', attrElement, ' to ', value);

		if (value === true) {
			attrElement.removeClass('inactive').addClass('active').prop('checked', true).html('<i class="' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" />' + (activeLabel ? ' ' + activeLabel : ''));
		} else {
			attrElement.removeClass('active').addClass('inactive').prop('checked', false).text((inactiveLabel ? inactiveLabel : '-'));
		}

	},
	reloadChildren: function(id) {
		var el = Structr.node(id);

		_Logger.log(_LogType.ENTITIES, 'reloadChildren', el);

		$(el).children('.node').remove();
		_Entities.resetMouseOverState(el);

		Command.children(id);

	},
	deleteNodes: function(button, entities, recursive, callback) {
		buttonClicked = button;
		if ( !Structr.isButtonDisabled(button) ) {

			var confirmationText = '<p>Delete the following objects' + (recursive ? ' (all folders recursively) ' : '') + '?</p>\n';

			var nodeIds = [];

			entities.forEach(function(entity) {

				confirmationText += '' + entity.name + ' [' + entity.id + ']<br>';
				nodeIds.push(entity.id);
			});

			confirmationText += '<br>';

			Structr.confirmation(confirmationText,
				function() {

					Command.deleteNodes(nodeIds, recursive);
					$.unblockUI({
						fadeOut: 25
					});
					if (callback) {
						callback();
					}
				});
		}
	},
	deleteNode: function(button, entity, recursive, callback) {
		buttonClicked = button;
		if ( !Structr.isButtonDisabled(button) ) {
			Structr.confirmation('<p>Delete ' + entity.type + ' \'' + entity.name + '\' [' + entity.id + ']' + (recursive ? ' recursively' : '') + '?</p>',
				function() {
					Command.deleteNode(entity.id, recursive);
					$.unblockUI({
						fadeOut: 25
					});
					if (callback) {
						callback(entity);
					}
				});
		}

	},
	deleteEdge: function(button, entity, recursive, callback) {
		buttonClicked = button;

		if ( !Structr.isButtonDisabled(button) ) {

			Structr.confirmation('<p>Delete Relationship</p><p>(' + entity.sourceId + ')-[' + entity.type + ']->(' + entity.targetId + ')' + (recursive ? ' recursively' : '') + '?</p>',
				function() {
					Command.deleteRelationship(entity.id, recursive);
					$.unblockUI({
						fadeOut: 25
					});
					if (callback) {
						callback(entity);
					}
				});

		}

	},
	showSyncDialog: function(source, target) {
		Structr.dialog('Sync between ' + source.id + ' and ' + target.id, function() {
			return true;
		}, function() {
			return true;
		});

		dialog.append('<div><input type="radio" name="syncMode" value="none"><label for="unidir">None</label></div>');
		dialog.append('<div><input type="radio" name="syncMode" value="unidir"><label for="unidir">Uni-directional (primary/secondary)</label></div>');
		dialog.append('<div><input type="radio" name="syncMode" value="bidir"><label for="unidir">Bi-directional</label></div>');

		$('input[name=syncMode]:radio', dialog).on('change', function() {
			Command.setSyncMode(source.id, target.id, $(this).val());
		});

	},
	dataBindingDialog: function(entity, el, typeInfo) {

		el.append('<table class="props"></table>');
		var t = $('.props', el);

		// General
		_Entities.appendRowWithInputField(entity, t, 'data-structr-id',                   'Element ID', typeInfo);
		_Entities.appendRowWithInputField(entity, t, 'data-structr-attr',                 'Attribute Key', typeInfo);
		_Entities.appendRowWithInputField(entity, t, 'data-structr-type',                 'Data type', typeInfo);
		_Entities.appendRowWithInputField(entity, t, 'data-structr-placeholder',          'Placeholder text', typeInfo);
		_Entities.appendRowWithInputField(entity, t, 'data-structr-custom-options-query', 'Custom REST query', typeInfo);
		_Entities.appendRowWithInputField(entity, t, 'data-structr-options-key',          'Options attribute key', typeInfo);
		_Entities.appendRowWithInputField(entity, t, 'data-structr-raw-value',            'Raw value', typeInfo);
		_Entities.appendRowWithInputField(entity, t, 'data-structr-hide',                 'Hide mode(s)', typeInfo);
		_Entities.appendRowWithInputField(entity, t, 'data-structr-edit-class',           'Edit mode CSS class', typeInfo);

		if (entity.type === 'Button' || entity.type === 'A') {

			_Entities.appendRowWithInputField(entity, t, 'data-structr-action',           'Action', typeInfo);
			_Entities.appendRowWithInputField(entity, t, 'data-structr-attributes',       'Attributes', typeInfo);
			_Entities.appendRowWithBooleanSwitch(entity, t, 'data-structr-reload',        'Reload', '', typeInfo);
			_Entities.appendRowWithBooleanSwitch(entity, t, 'data-structr-confirm',       'Confirm action?', '', typeInfo);
			_Entities.appendRowWithInputField(entity, t, 'data-structr-return',           'Return URI', typeInfo);
			_Entities.appendRowWithBooleanSwitch(entity, t, 'data-structr-append-id',     'Append ID on create', '', typeInfo);

		} else if (entity.type === 'Input' || entity.type === 'Select' || entity.type === 'Textarea') {
			_Entities.appendRowWithInputField(entity, t, 'data-structr-name',             'Field name', typeInfo);

		}
	},
	appendRowWithInputField: function(entity, el, key, label, typeInfo) {
		el.append('<tr><td class="key">' + label + '</td><td class="value"><input class="' + key + '_" name="' + key + '" value="' + (entity[key] ? escapeForHtmlAttributes(entity[key]) : '') + '"></td><td><i id="null_' + key + '" class="nullIcon ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></td></tr>');
		var inp = $('[name="' + key + '"]', el);
		_Entities.activateInput(inp, entity.id, entity.pageId);
		var nullIcon = $('#null_' + key, el);
		nullIcon.on('click', function() {
			Command.setProperty(entity.id, key, null, false, function() {
				inp.val(null);
				blinkGreen(inp);
				Structr.showAndHideInfoBoxMessage('Property "' + key + '" was set to null.', 'success', 2000, 1000);
			});
		});

		_Entities.appendSchemaHint($('.key:last', el), key, typeInfo);
	},
	appendRowWithBooleanSwitch: function (entity, el, key, label, text, typeInfo) {
		el.append('<tr><td class="key">' + label + '</td><td class="value"></td><td></td></tr>');

		_Entities.appendBooleanSwitch($('tr:last .value', el), entity, key, '', text);

		_Entities.appendSchemaHint($('.key:last', el), key, typeInfo);
	},
	appendSchemaHint: function (el, key, typeInfo) {

		if (typeInfo[key] && typeInfo[key].hint) {
			Structr.appendInfoTextToElement({
				element: el,
				text: typeInfo[key].hint,
				class: 'hint'
			});
		}

	},
	queryDialog: function(entity, el) {

		el.append('<table class="props"></table>');
		var t = $('.props', el);

		t.append('<tr><td class="key">Query auto-limit</td><td class="value" id="queryAutoLimit"></td></tr>');
		t.append('<tr><td class="key">Hide in index mode</td><td  class="value" id="hideIndexMode"></td></tr>');
		t.append('<tr><td class="key">Hide in details mode</td><td  class="value" id="hideDetailsMode"></td></tr>');

		_Entities.appendBooleanSwitch($('#queryAutoLimit', t), entity, 'renderDetails', ['Query is limited', 'Query is not limited'], 'Limit result to the object with the ID the URL ends with.');
		_Entities.appendBooleanSwitch($('#hideIndexMode', t), entity, 'hideOnIndex', ['Hidden in index mode', 'Visible in index mode'], 'if URL does not end with an ID');
		_Entities.appendBooleanSwitch($('#hideDetailsMode', t), entity, 'hideOnDetail', ['Hidden in details mode', 'Visible in details mode'], 'if URL ends with an ID.');

		el.append('<div id="data-tabs" class="data-tabs"><ul><li id="tab-rest">REST Query</li><li id="tab-cypher">Cypher Query</li><li id="tab-xpath">XPath Query</li><li id="tab-function">Function Query</li></ul>'
			+ '<div id="content-tab-rest"></div><div id="content-tab-cypher"></div><div id="content-tab-xpath"></div><div id="content-tab-function"></div></div>');

		_Entities.appendTextarea($('#content-tab-rest'), entity, 'restQuery', 'REST Query', '');
		_Entities.appendTextarea($('#content-tab-cypher'), entity, 'cypherQuery', 'Cypher Query', '');
		_Entities.appendTextarea($('#content-tab-xpath'), entity, 'xpathQuery', 'XPath Query', '');
		_Entities.appendTextarea($('#content-tab-function'), entity, 'functionQuery', 'Function Query', '');

		_Entities.appendInput(el, entity, 'dataKey', 'Data Key', 'The data key is either a word to reference result objects, or it can be the name of a collection property of the result object.<br>' +
			'You can access result objects or the objects of the collection using ${<i>&lt;dataKey&gt;.&lt;propertyKey&gt;</i>}');

		_Entities.activateTabs(entity.id, '#data-tabs', '#content-tab-rest');

	},
	activateTabs: function(nodeId, elId, activeId, activeTabPrefix) {
		activeTabPrefix = activeTabPrefix || _Entities.activeQueryTabPrefix;
		var el = $(elId);
		var tabs = $('li', el);
		$.each(tabs, function(i, tab) {
			$(tab).on('click', function() {
				var tab = $(this);
				tabs.removeClass('active');
				tab.addClass('active');
				el.children('div').hide();
				var id = tab.prop('id').substring(4);
				LSWrapper.setItem(activeTabPrefix  + '_' + nodeId, id);
				var content = $('#content-tab-' + id);
				content.show();
			});
		});
		var id = LSWrapper.getItem(activeTabPrefix  + '_' + nodeId) || activeId.substring(13);
		var tab = $('#tab-' + id);
		if (!tab.hasClass('active')) {
			tab.click();
		}
	},
	editSource: function(entity) {

		Structr.dialog('Edit source of "' + (entity.name ? entity.name : entity.id) + '"', function () {
			_Logger.log(_LogType.ENTITIES, 'Element source saved');
		}, function () {
			_Logger.log(_LogType.ENTITIES, 'cancelled');
		});

		// Get content in widget mode
		var url = viewRootUrl + entity.id + '?edit=3', contentType = 'text/html';

		$.ajax({
			url: url,
			//async: false,
			contentType: contentType,
			success: function(data) {
				text = data;
				text = text.replace(/<!DOCTYPE[^>]*>/, '');
				var startTag = text.replace(/(<[^>]*>)([^]*)(<\/[^>]*>)/, '$1').replace(/^\s+|\s+$/g, '');
				var innerText = text.replace(/(<[^>]*>)([^]*)(<\/[^>]*>)/, '$2').replace(/^\s+|\s+$/g, '');
				var endTag = text.replace(/(<[^>]*>)([^]*)(<\/[^>]*>)/, '$3').replace(/^\s+|\s+$/g, '');
				text = innerText;

				dialog.append('<div class="editor"></div>');

				var contentBox = $('.editor', dialog);
				var lineWrapping = LSWrapper.getItem(lineWrappingKey);

				// Intitialize editor
				editor = CodeMirror(contentBox.get(0), {
					value: unescapeTags(innerText),
					mode: contentType,
					lineNumbers: true,
					lineWrapping: lineWrapping,
					indentUnit: 4,
					tabSize:4,
					indentWithTabs: true
				});

				$('.CodeMirror-scroll').prepend('<div class="starttag"></div>');
				$('.CodeMirror-scroll').append('<div class="endtag"></div>');
				$('.starttag', dialog).append(escapeTags(startTag.replace(/\sdata-structr-hash=".{32}"/, "")));
				$('.endtag', dialog).append(escapeTags(endTag));

				editor.id = entity.id;

				dialogBtn.append('<button id="saveFile" disabled="disabled" class="disabled"> Save </button>');
				dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');

				dialogSaveButton = $('#saveFile', dialogBtn);
				saveAndClose = $('#saveAndClose', dialogBtn);

				editor.on('scroll', function() {
					_Entities.hideDataHashAttribute(editor);
				});

				editor.on('change', function(cm, change) {

					//text1 = $(contentNode).children('.content_').text();
					text2 = editor.getValue();
					//console.log(text, text2, text === text2);
					if (text === text2) {
						dialogSaveButton.prop("disabled", true).addClass('disabled');
						saveAndClose.prop("disabled", true).addClass('disabled');
					} else {
						dialogSaveButton.prop("disabled", false).removeClass('disabled');
						saveAndClose.prop("disabled", false).removeClass('disabled');
					}

					_Entities.hideDataHashAttribute(editor);
				});

				dialogSaveButton.on('click', function(e) {
					e.stopPropagation();
					text2 = editor.getValue();
					//console.log(text, text2, text === text2);
					if (text === text2) {
						dialogSaveButton.prop("disabled", true).addClass('disabled');
						saveAndClose.prop("disabled", true).addClass('disabled');
					} else {
						dialogSaveButton.prop("disabled", false).removeClass('disabled');
						saveAndClose.prop("disabled", false).removeClass('disabled');
					}

					Command.saveNode(startTag + editor.getValue() + endTag, entity.id, function() {
						$.ajax({
							url: url,
							contentType: contentType,
							success: function(data) {
								text = unescapeTags(data).replace(/<!DOCTYPE[^>]*>/, '');
								text = text.replace(/(<[^>]*>)([^]*)(<\/[^>]*>)/, '$2').replace(/^\s+|\s+$/g, '');
								editor.setValue(text);

								dialogSaveButton.prop("disabled", true).addClass('disabled');
								saveAndClose.prop("disabled", true).addClass('disabled');
								Structr.showAndHideInfoBoxMessage('Node source saved and DOM tree rebuilt.', 'success', 2000, 200);

								if (_Entities.isExpanded(Structr.node(entity.id))) {
									$('.expand_icon', Structr.node(entity.id)).click().click();
								}
							}
						});


					});

				});

				saveAndClose.on('click', function(e) {
					e.stopPropagation();
					dialogSaveButton.click();
					setTimeout(function() {
						dialogSaveButton.remove();
						saveAndClose.remove();
						dialogCancelButton.click();
					}, 500);
				});

				dialogMeta.append('<span class="editor-info"><label for"lineWrapping">Line Wrapping:</label> <input id="lineWrapping" type="checkbox"' + (lineWrapping ? ' checked="checked" ' : '') + '></span>');
				$('#lineWrapping').on('change', function() {
					var inp = $(this);
					if (inp.is(':checked')) {
						LSWrapper.setItem(lineWrappingKey, "1");
						editor.setOption('lineWrapping', true);
					} else {
						LSWrapper.removeItem(lineWrappingKey);
						editor.setOption('lineWrapping', false);
					}
					editor.refresh();
				});

				Structr.resize();

				_Entities.hideDataHashAttribute(editor);

			},
			error: function(xhr, statusText, error) {
				console.log(xhr, statusText, error);
			}
		});

	},
	hideDataHashAttribute: function(editor) {
		var sc = editor.getSearchCursor(/\sdata-structr-hash=".{32}"/);
		while (sc.findNext()) {
			editor.markText(sc.from(), sc.to(), {className: 'data-structr-hash', collapsed: true, inclusiveLeft: true});
		}
	},
	showProperties: function(obj, activeViewOverride) {

		var handleGraphObject = function(entity) {

			var views = ['ui'];
			var activeView = 'ui';
			var tabTexts = [];

			if (activeViewOverride) {
				activeView = activeViewOverride;
			}

			_Schema.getTypeInfo(entity.type, function(typeInfo) {
				var dialogTitle;

				if (entity.hasOwnProperty('relType')) {

					tabTexts.ui = 'Relationship Properties';
					tabTexts.sourceNode = 'Source Node Properties';
					tabTexts.targetNode = 'Target Node Properties';

					dialogTitle = 'Edit properties of ' + (entity.type ? entity.type : '') + ' relationship ' + (entity.name ? entity.name : entity.id);

				} else {

					views = views.concat(['in', 'out']);

					if (entity.isDOMNode && !entity.isContent) {
						views.unshift('_html_');
						if (Structr.isModuleActive(_Pages)) {
							activeView = '_html_';
						}
					}

					tabTexts._html_ = 'HTML Attributes';
					tabTexts.ui = 'Node Properties';
					tabTexts['in'] = 'Incoming Relationships';
					tabTexts.out = 'Outgoing Relationships';

					dialogTitle = 'Edit properties of ' + (entity.type ? entity.type : '') + ' node ' + (entity.name ? entity.name : entity.id);

				}

				Structr.dialog(dialogTitle, function() { return true; }, function() { return true; });

				var tabsdiv = dialogHead.append('<div id="tabs"></div>');
				var mainTabs = tabsdiv.append('<ul></ul>');
				var contentEl = dialog.append('<div></div>');

				if (entity.isDOMNode) {

					_Entities.appendPropTab(entity, mainTabs, contentEl, 'query', 'Query and Data Binding', true, function(c) {
						_Entities.queryDialog(entity, c, typeInfo);
					});

					_Entities.appendPropTab(entity, mainTabs, contentEl, 'editBinding', 'Edit Mode Binding', false, function(c) {
						_Entities.dataBindingDialog(entity, c, typeInfo);
					});

				}

				_Entities.appendViews(entity, views, tabTexts, mainTabs, contentEl, activeView, activeViewOverride, typeInfo);

				Structr.resize();

			});

		};

		if (obj.relType) {
			Command.getRelationship(obj.id, obj.target, null, function(entity) { handleGraphObject(entity); });
		} else {
			Command.get(obj.id, null, function(entity) { handleGraphObject(entity); });
		}
	},
	appendPropTab: function(entity, tabsEl, contentEl, name, label, active, callback, initCallback) {

		var ul = tabsEl.children('ul');
		ul.append('<li id="tab-' + name + '">' + label + '</li>');

		var tab = $('#tab-' + name + '');
		if (active) {
			tab.addClass('active');
		}
		tab.on('click', function(e) {
			e.stopPropagation();
			var self = $(this);
			$('.propTabContent').hide();
			$('li', ul).removeClass('active');
			$('#tabView-' + name).show();
			self.addClass('active');
			LSWrapper.setItem(_Entities.activeEditTabPrefix  + '_' + entity.id, name);

			if (typeof initCallback === "function") {
				initCallback();
			}
		});
		contentEl.append('<div class="propTabContent" id="tabView-' + name + '"></div>');
		var content = $('#tabView-' + name);
		if (active) {
			content.show();
		}
		if (callback) {
			callback(content);
		}
		if (active && typeof initCallback === "function") {
			initCallback();
		}
		return content;
	},
	appendViews: function(entity, views, texts, tabsEl, contentEl, activeView, activeViewOverride, typeInfo) {

		var ul = tabsEl.children('ul');

		$(views).each(function(i, view) {

			var tabText = texts[view];

			ul.append('<li id="tab-' + view + '">' + tabText + '</li>');

			contentEl.append('<div class="propTabContent" id="tabView-' + view + '"></div>');

			var tab = $('#tab-' + view);

			tab.on('click', function(e) {
				e.stopPropagation();
				var self = $(this);
				contentEl.children('div').hide();
				$('li', ul).removeClass('active');
				self.addClass('active');
				var tabView = $('#tabView-' + view);
				fastRemoveAllChildren(tabView[0]);
				tabView.show();
				LSWrapper.setItem(_Entities.activeEditTabPrefix  + '_' + entity.id, view);

				_Entities.listProperties(entity, view, tabView, typeInfo);
			});
		});
		activeView = activeViewOverride || LSWrapper.getItem(_Entities.activeEditTabPrefix  + '_' + entity.id) || activeView;
		$('#tab-' + activeView).click();

	},
	getNullIconForKey: function (key) {
		return '<i id="' + _Entities.null_prefix + key + '" class="nullIcon ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" />';
	},
	listProperties: function (entity, view, tabView, typeInfo) {
		$.ajax({
			url: rootUrl + entity.id + (view ? '/' + view : '') + '?pageSize=100', // TODO: Implement paging or scroll-into-view here
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			success: function(data) {
				// Default: Edit node id
				var id = entity.id;

				var tempNodeCache = new AsyncObjectCache(function(id) {
					Command.getProperties(id, 'id,name,type,tag,isContent,content', function (node) {
						tempNodeCache.addObject(node, node.id);
					});
				});

				// ID of graph object to edit
				$(data.result).each(function(i, res) {

					// reset id for each object group
					var keys = Object.keys(res);

					var noCategoryKeys = [];
					var groupedKeys = {};

					if (typeInfo) {
						keys.forEach(function(key) {
							if (typeInfo[key] && typeInfo[key].category && typeInfo[key].category !== "System") {

								var category = typeInfo[key].category;
								if (!groupedKeys[category]) {
									groupedKeys[category] = [];
								}
								groupedKeys[category].push(key);
							} else {
								noCategoryKeys.push(key);
							}
						});
					}

					_Entities.createPropertyTable(null, noCategoryKeys, res, entity, view, tabView, typeInfo, tempNodeCache);
					Object.keys(groupedKeys).sort().forEach(function(categoryName) {
						_Entities.createPropertyTable(categoryName, groupedKeys[categoryName], res, entity, view, tabView, typeInfo, tempNodeCache);
					});
				});
			}
		});

	},
	createPropertyTable: function (heading, keys, res, entity, view, tabView, typeInfo, tempNodeCache) {

		if (heading) {
			tabView.append('<h2>' + heading + '</h2>');
		}
		tabView.append('<table class="props ' + view + ' ' + res['id'] + '_"></table>');
		var propsTable = $('table:last', tabView);
		var focusAttr = 'class';
		var id = entity.id;

		if (view === '_html_') {
			keys.sort();
		}

		$(keys).each(function(i, key) {

			if (view === '_html_') {

				var display = false;
				_Elements.mostUsedAttrs.forEach(function(mostUsed) {
					if (isIn(entity.tag, mostUsed.elements) && isIn(key.substring(6), mostUsed.attrs)) {
						display = true;
						focusAttr = mostUsed.focus ? mostUsed.focus : focusAttr;
					}
				});

				// Always show non-empty, non 'data-structr-' attributes
				if (res[key] !== null && key.indexOf('data-structr-') !== 0) {
					display = true;
				}

				var displayKey = key;
				if (key.indexOf('data-') !== 0) {
					if (key.indexOf('_html_') === 0) {
						displayKey = displayKey.substring(6);
					} else if (key.indexOf('_custom_html_') === 0) {
						displayKey = displayKey.substring(13);
					}
				}

				if (display || key === '_html_class' || key === '_html_id') {
					propsTable.append('<tr><td class="key">' + displayKey + '</td><td class="value ' + key + '_">' + formatValueInputField(key, res[key]) + '</td><td>' + _Entities.getNullIconForKey(key) + '</td></tr>');
				} else if (key !== 'id') {
					propsTable.append('<tr class="hidden"><td class="key">' + displayKey + '</td><td class="value ' + key + '_">' + formatValueInputField(key, res[key]) + '</td><td>' + _Entities.getNullIconForKey(key) + '</td></tr>');
				}

			} else if (view === 'in' || view === 'out') {
				if (key === 'id') {
					// set ID to rel ID
					id = res[key];
				}
				propsTable.append('<tr><td class="key">' + key + '</td><td rel_id="' + id + '" class="value ' + key + '_">' + formatValueInputField(key, res[key]) + '</td></tr>');

			} else {

				var isReadOnly   = false;
				var isSystem     = false;
				var isBoolean    = false;
				var isDate       = false;
				var isPassword   = false;
				var isRelated    = false;
				var isCollection = false;
				var isMultiline  = false;

				var row = $('<tr><td class="key">' + formatKey(key) + '</td><td class="value ' + key + '_"></td><td>' + _Entities.getNullIconForKey(key) + '</td></tr>');
				propsTable.append(row);
				var cell = $('.value.' + key + '_', propsTable);

				if (!typeInfo[key]) {
					cell.append(formatValueInputField(key, res[key], isPassword, isReadOnly, isMultiline));

				} else {

					var type = typeInfo[key].type;

					isReadOnly = isIn(key, _Entities.readOnlyAttrs) || (typeInfo[key].readOnly);
					isSystem = typeInfo[key].system;
					isPassword = (typeInfo[key].className === 'org.structr.core.property.PasswordProperty');
					isMultiline = (typeInfo[key].format === 'multi-line');
					isRelated = typeInfo[key].relatedType;
					if (isRelated) {
						isCollection = typeInfo[key].isCollection;
					}

					if (type) {
						isBoolean = (type === 'Boolean');
						isDate = (type === 'Date');
					}

					if (!key.startsWith('_html_')) {
						if (isBoolean) {
							cell.removeClass('value').append('<input type="checkbox" class="' + key + '_">');
							var checkbox = $(propsTable.find('input[type="checkbox"].' + key + '_'));

							var val = res[key];
							if (val) {
								checkbox.prop('checked', true);
							}
							if ((!isReadOnly || isAdmin) && !isSystem) {
								checkbox.on('change', function() {
									var checked = checkbox.prop('checked');
									_Entities.setProperty(id, key, checked, false, function(newVal) {
										if (val !== newVal) {
											blinkGreen(cell);
										}
										checkbox.prop('checked', newVal);
										val = newVal;
									});
								});
							} else {
								checkbox.prop('disabled', 'disabled').addClass('readOnly').addClass('disabled');
							}

						} else if (isDate && !isReadOnly) {

							_Entities.appendDatePicker(cell, res, key, typeInfo[key].format);

						} else if (isRelated) {

							if (res[key]) {

								if (!isCollection) {

									var nodeId = res[key].id || res[key];

									tempNodeCache.registerCallback(nodeId, nodeId, function(node) {

										_Entities.appendRelatedNode(cell, node, function(nodeEl) {
											$('.remove', nodeEl).on('click', function(e) {
												e.preventDefault();
												_Entities.setProperty(id, key, null, false, function(newVal) {
													if (!newVal) {
														nodeEl.remove();
														blinkGreen(cell);
														Structr.showAndHideInfoBoxMessage('Related node "' + (node.name || node.id) + '" was removed from property "' + key + '".', 'success', 2000, 1000);
													} else {
														blinkRed(cell);
													}
												});
												return false;
											});
										});
									});

								} else {

									res[key].forEach(function(obj) {

										var nodeId = obj.id || obj;

										tempNodeCache.registerCallback(nodeId, nodeId, function(node) {

											_Entities.appendRelatedNode(cell, node, function(nodeEl) {
												$('.remove', nodeEl).on('click', function(e) {
													e.preventDefault();
													Command.removeFromCollection(id, key, node.id, function() {
														nodeEl.remove();
														blinkGreen(cell);
														Structr.showAndHideInfoBoxMessage('Related node "' + (node.name || node.id) + '" was removed from property "' + key + '".', 'success', 2000, 1000);
													});
													return false;
												});
											});
										});

									});
								}
							}

							cell.append('<i class="add ' + _Icons.getFullSpriteClass(_Icons.add_grey_icon) + '" />');
							$('.add', cell).on('click', function() {
								Structr.dialog('Add ' + typeInfo[key].type, function() {
								}, function() {
									_Entities.showProperties(entity);
								});
								_Entities.displaySearch(id, key, typeInfo[key].type, dialogText, isCollection);
							});

						} else {
							cell.append(formatValueInputField(key, res[key], isPassword, isReadOnly, isMultiline));
						}

					}
				}

				if (isSystem || isReadOnly || isBoolean) {
					$('i.nullIcon', row).remove();
				}
			}

			_Entities.appendSchemaHint($('.key:last', propsTable), key, typeInfo);

			var nullIcon = $('#' + _Entities.null_prefix + key);
			nullIcon.on('click', function() {
				var key = $(this).prop('id').substring(_Entities.null_prefix.length);
				var input    = $('.' + key + '_').find('input');
				var textarea = $('.' + key + '_').find('textarea');
				_Entities.setProperty(id, key, null, false, function(newVal) {
					if (!newVal) {
						blinkGreen(cell);
						Structr.showAndHideInfoBoxMessage('Property "' + key + '" was set to null.', 'success', 2000, 1000);

						if (key === 'name') {
							var entity = StructrModel.objects[id];
							if (entity.type !== 'Template' && entity.type !== 'Content') {
								entity.name = entity.tag ? entity.tag : '[' + entity.type + ']';
							}
							StructrModel.refresh(id);
						}
						if (isRelated) {
							cell.empty();
						}
						if (isBoolean) {
							input.prop('checked', false);
						}
					} else {
						blinkRed(input);
					}
					if (!isRelated) {
						input.val(newVal);
						textarea.val(newVal);
					}
				});
			});
		});


		propsTable.append('<tr class="hidden"><td class="key"><input type="text" class="newKey" name="key"></td><td class="value"><input type="text" value=""></td><td></td></tr>');
		$('.props tr td.value input',    dialog).each(function(i, inputEl)    { _Entities.activateInput(inputEl,    id, entity.pageId); });
		$('.props tr td.value textarea', dialog).each(function(i, textareaEl) { _Entities.activateInput(textareaEl, id, entity.pageId); });

		Structr.appendInfoTextToElement({
			element: $('.newKey', propsTable),
			text: "Any attribute name is allowed but 'data-' attributes are recommended.",
			insertAfter: true,
			css: {
				marginLeft: "3px",
				top: "-5px",
				position: "relative"
			}
		});

		if (view === '_html_') {
			$('input[name="_html_' + focusAttr + '"]', propsTable).focus();

			tabView.append('<button class="show-all">Show all attributes</button>');
			$('.show-all', tabView).on('click', function() {
				$('tr.hidden').toggle();
				$(this).remove();
			});
		}

	},
	displaySearch: function(id, key, type, el, isCollection) {

		el.append('<div class="searchBox searchBoxDialog"><input class="search" name="search" size="20" placeholder="Search"><i class="clearSearchIcon ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></div>');
		var searchBox = $('.searchBoxDialog', el);
		var search = $('.search', searchBox);
		window.setTimeout(function() {
			search.focus();
		}, 250);

		search.keyup(function(e) {
			e.preventDefault();

			var searchString = $(this).val();
			if (searchString && searchString.length && e.keyCode === 13) {

				$('.clearSearchIcon', searchBox).show().on('click', function() {
					if (_Entities.clearSearchResults(el)) {
						$('.clearSearchIcon').hide().off('click');
						search.val('');
						search.focus();
					}
				});

				$('.result-box', el).remove();
				var box = $('<div class="result-box"></div>');
				el.append(box);

				var resultHandler = function(nodes) {

					nodes.forEach(function(node) {
						var displayName = node.title || node.name || node.id;
						box.append('<div title="' + displayName + '" " class="_' + node.id + ' node element">' + fitStringToWidth(displayName, 120) + '</div>');
						$('._' + node.id, box).on('click', function() {

							var nodeEl = $(this);

							if (isCollection) {

								_Entities.addToCollection(id, node.id, key, function() {

									blinkGreen(nodeEl);

									if (Structr.isModuleActive(_Contents)) {
										_Contents.refreshTree();
									}
									if (Structr.isModuleActive(_Files)) {
										_Files.refreshTree();
									}
								});

							} else {
								Command.setProperty(id, key, node.id, false, function() {
									dialogCancelButton.click();
								});
							}

						});
					});

				};

				if (searchString.trim() === '*') {
					Command.getByType(type, 1000, 1, 'name', 'asc', null, false, resultHandler);
				} else {
					Command.search(searchString, type, false, resultHandler);
				}


			} else if (e.keyCode === 27) {

				if (!searchString || searchString === '') {
					dialogCancelButton.click();
				}

				if (_Entities.clearSearchResults(el)) {
					$('.clearSearchIcon').hide().off('click');
					search.val('');
					search.focus();
				} else {
					search.val('');
				}

			}

			return false;

		});
	},
	clearSearch: function(el) {
		if (_Entities.clearSearchResults(el)) {
			$('.clearSearchIcon').hide().off('click');
			$('.search').val('');
			$('#resourceTabs', main).show();
			$('#resourceBox', main).show();
		}
	},
	clearSearchResults: function(el) {
		var searchResults = $('.searchResults', el);
		if (searchResults.length) {
			searchResults.remove();
			$('.searchResultsTitle').remove();
			return true;
		}
		return false;
	},
	addToCollection: function(itemId, newItemId, key, callback) {
		_Entities.extendCollection(itemId, newItemId, key, function(collectionIds) {
			Command.setProperty(itemId, key, collectionIds, false, function() {
				if (callback) {
					callback();
				}
			});
		});
	},
	extendCollection: function(itemId, newItemId, key, callback) {
		var collectionIds = [];
		Command.get(itemId, key, function(obj) {
				//var keyInfo = typeInfo.filter(function(item) { return item.jsonName === key; })[0];
			var collection = obj[key];
			if (collection && collection.length) {
				collection.forEach(function(collectionItem) {

					if (collectionItem.id) {
						// object or ObjectNotion/UiNotion
						collectionIds.push(collectionItem.id);
					} else {
						// in case of PropertyNotion or the like
						collectionIds.push(collectionItem);
					}
				});
			}
			collectionIds.push(newItemId);
			callback(collectionIds);
		});
	},
	appendDatePicker: function(el, entity, key, format) {
		if (!entity[key] || entity[key] === 'null') {
			entity[key] = '';
		}
		el.append('<input class="dateField" name="' + key + '" type="text" value="' + entity[key] + '">');
		var dateField = $(el.find('.dateField'));
		var dateTimePickerFormat = getDateTimePickerFormat(format);
		dateField.datetimepicker({
			dateFormat: dateTimePickerFormat.dateFormat,
			timeFormat: dateTimePickerFormat.timeFormat,
			separator: dateTimePickerFormat.separator
		});
	},
	appendRelatedNode: function(cell, node, onDelete) {
		var displayName = _Crud.displayName(node);
		cell.append('<div title="' + displayName + '" class="_' + node.id + ' node ' + (node.type ? node.type.toLowerCase() : (node.tag ? node.tag : 'element')) + ' ' + node.id + '_">' + fitStringToWidth(displayName, 80) + '<i class="remove ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></div>');
		var nodeEl = $('._' + node.id, cell);

		nodeEl.on('click', function(e) {
			e.preventDefault();
			_Entities.showProperties(node);
			return false;
		});

		if (onDelete) {
			return onDelete(nodeEl);
		}
	},
	activateInput: function(el, id, pageId) {

		var input = $(el);
		var oldVal = input.val();
		var relId = input.parent().attr('rel_id');

		if (!input.hasClass('readonly') && !input.hasClass('newKey')) {

			input.on('focus', function() {
				input.addClass('active');
			});

			input.on('change', function() {
				input.data('changed', true);

				if (pageId && pageId === activeTab) {
					//console.log('reloading previews')
					_Pages.reloadPreviews();
				}
			});

			input.on('focusout', function() {
				_Logger.log(_LogType.ENTITIES, 'relId', relId);
				var objId = relId ? relId : id;
				_Logger.log(_LogType.ENTITIES, 'set properties of obj', objId);

				var keyInput = input.parent().parent().children('td').first().children('input');
				_Logger.log(_LogType.ENTITIES, keyInput);
				if (keyInput && keyInput.length) {

					var newKey = '_custom_html_' + keyInput.val();
					var val = input.val();

					// new key
					_Logger.log(_LogType.ENTITIES, 'new key: Command.setProperty(', objId, newKey, val);
					Command.setProperty(objId, newKey, val, false, function() {
						blinkGreen(input);
						Structr.showAndHideInfoBoxMessage('New property "' + newKey + '" was added and saved with value "' + val + '".', 'success', 2000, 1000);
					});


				} else {
					var key = input.prop('name');
					var val = input.val();
					var isPassword = input.prop('type') === 'password';
					if (input.data('changed')) {
						input.data('changed', false);
						_Logger.log(_LogType.ENTITIES, 'existing key: Command.setProperty(', objId, key, val);
						_Entities.setProperty(objId, key, val, false, function(newVal) {
							if (isPassword || (newVal && newVal !== oldVal)) {
								blinkGreen(input);
								input.val(newVal);
								Structr.showAndHideInfoBoxMessage('Updated property "' + key + '"' + (!isPassword ? ' with value "' + newVal + '".' : '.'), 'success', 2000, 200);

							} else {
								input.val(oldVal);
							}
							oldVal = newVal;
						});
					}
				}
				input.removeClass('active');
				input.parent().children('.icon').each(function(i, icon) {
					$(icon).remove();
				});
			});
		}
	},
	setProperty: function(id, key, val, recursive, callback) {
		Command.setProperty(id, key, val, recursive, function() {
			Command.getProperty(id, key, callback);
		});
	},
	appendAccessControlIcon: function(parent, entity) {

		var isProtected = !entity.visibleToPublicUsers || !entity.visibleToAuthenticatedUsers;

		var keyIcon = $('.key_icon', parent);
		var newKeyIcon = '<i title="Access Control and Visibility" class="key_icon button ' + _Icons.getFullSpriteClass(_Icons.key_icon) + '" />';
		if (!(keyIcon && keyIcon.length)) {
			parent.append(newKeyIcon);
			keyIcon = $('.key_icon', parent);
			if (isProtected) {
				keyIcon.show();
				keyIcon.addClass('donthide');
			}

			_Entities.bindAccessControl(keyIcon, entity);
		}
	},
	bindAccessControl: function(btn, entity) {

		btn.on('click', function(e) {
			e.stopPropagation();
			_Entities.showAccessControlDialog(entity);
		});
	},
	showAccessControlDialog: function(entity) {

		var id = entity.id;

		var initialObj;

		Structr.dialog('Access Control and Visibility', function() {
		}, function() {
			if (Structr.isModuleActive(_Crud)) {

				var handleGraphObject = function(entity) {
					if (!entity.owner || initialObj.ownerId !== entity.owner.id) {
						_Crud.refreshCell(id, "owner", entity.owner, entity.type, initialObj.ownerId);
					}

					_Crud.refreshCell(id, "visibleToPublicUsers",        entity.visibleToPublicUsers,        entity.type, initialObj.visibleToPublicUsers);
					_Crud.refreshCell(id, "visibleToAuthenticatedUsers", entity.visibleToAuthenticatedUsers, entity.type, initialObj.visibleToAuthenticatedUsers);
				};

				if (entity.targetId) {
					Command.getRelationship(id, entity.targetId, "id,type,owner,visibleToPublicUsers,visibleToAuthenticatedUsers", handleGraphObject);
				} else {
					Command.get(id, "id,type,owner,visibleToPublicUsers,visibleToAuthenticatedUsers", handleGraphObject);
				}
			}
		});

		var handleGraphObject = function(entity) {

			initialObj = {
				ownerId: entity.owner ? entity.owner.id : null,
				visibleToPublicUsers: entity.visibleToPublicUsers,
				visibleToAuthenticatedUsers: entity.visibleToAuthenticatedUsers
			};

			var owner_select_id = 'owner_select_' + id;
			dialogText.append('<span id="' + owner_select_id + '"></span>');
			var owner_select = $('#' + owner_select_id, dialogText);

			dialogText.append('<h3>Visibility</h3>');

			if (entity.type === 'Template' || entity.isFolder || (Structr.isModuleActive(_Pages) && !(entity.isContent))) {
				dialogText.append('<div>Apply visibility switches recursively? <input id="recursive" type="checkbox" name="recursive"></div><br>');
			}

			_Entities.appendBooleanSwitch(dialogText, entity, 'visibleToPublicUsers', ['Visible to public users', 'Not visible to public users'], 'Click to toggle visibility for users not logged-in', '#recursive');
			_Entities.appendBooleanSwitch(dialogText, entity, 'visibleToAuthenticatedUsers', ['Visible to auth. users', 'Not visible to auth. users'], 'Click to toggle visibility to logged-in users', '#recursive');

			dialogText.append('<h3>Access Rights</h3>');
			dialogText.append('<table class="props" id="principals"><thead><tr><th>Name</th><th>Read</th><th>Write</th><th>Delete</th><th>Access Control</th></tr></thead><tbody></tbody></table');

			var tb = $('#principals tbody', dialogText);
			tb.append('<tr id="new"><td><select style="width: 300px;z-index: 999" id="newPrincipal"><option>Select Group/User</option></select></td><td><input id="newRead" type="checkbox" disabled="disabled"></td><td><input id="newWrite" type="checkbox" disabled="disabled"></td><td><input id="newDelete" type="checkbox" disabled="disabled"></td><td><input id="newAccessControl" type="checkbox" disabled="disabled"></td></tr>');

			$.ajax({
				url: rootUrl + '/' + entity.id + '/in',
				dataType: 'json',
				contentType: 'application/json; charset=utf-8',
				success: function(data) {

					$(data.result).each(function(i, result) {

						var permissions = {
							'read': isIn('read', result.allowed),
							'write': isIn('write', result.allowed),
							'delete': isIn('delete', result.allowed),
							'accessControl': isIn('accessControl', result.allowed)
						};

						var principalId = result.principalId;
						if (principalId) {
							Command.get(principalId, "id,name,isGroup", function(p) {
								_Entities.addPrincipal(entity, p, permissions);
							});
						}
					});
				}
			});
			var select = $('#newPrincipal');
			select.chosen({width: '90%'});
			var n = 10000;
			Command.getByType('Group', n, 1, 'name', 'asc', 'id,name', false, function(groups) {
				groups.forEach(function(group) {
					select.append('<option value="' + group.id + '">' + group.name + '</option>');
				});
				select.trigger("chosen:updated");
			});
			var al2 = Structr.loaderIcon(select.parent(), {'float':'right'});
			Command.getByType('User', n, 1, 'name', 'asc', 'id,name', false, function(users) {

				_Entities.appendSimpleSelection(owner_select, entity, 'users', 'Owner', 'owner.id', users);

				users.forEach(function(user) {
					select.append('<option value="' + user.id + '">' + user.name + '</option>');
				});
				select.trigger("chosen:updated");
				if (al2.length) {
					al2.remove();
				}
			});
			select.on('change', function() {
				var sel = $(this);
				var pId = sel[0].value;
				var rec = $('#recursive', dialogText).is(':checked');
				Command.setPermission(entity.id, pId, 'grant', 'read', rec);
				$('#new', tb).selectedIndex = 0;

				Command.get(pId, "id,name,isGroup", function(p) {
					_Entities.addPrincipal(entity, p, {'read': true});
				});
			});
		};

		if (entity.targetId) {
			Command.getRelationship(id, entity.targetId, "id,type,isFolder,isContent,owner,visibleToPublicUsers,visibleToAuthenticatedUsers", function(entity) { handleGraphObject(entity); });
		} else {
			Command.get(id, "id,type,isFolder,isContent,owner,visibleToPublicUsers,visibleToAuthenticatedUsers", function(entity) { handleGraphObject(entity); });
		}
	},
	addPrincipal: function (entity, principal, permissions) {
		$('#newPrincipal option[value="' + principal.id + '"]').remove();
		$('#newPrincipal').trigger('chosen:updated');
		$('#new').after('<tr class="_' + principal.id + '"><td><i class="typeIcon ' + _Icons.getFullSpriteClass((principal.isGroup ? _Icons.group_icon : _Icons.user_icon)) + '" /> <span class="name">' + principal.name + '</span></td><tr>');

		var row = $('._' + principal.id, dialogText);

		['read', 'write', 'delete', 'accessControl'].forEach(function(perm) {

			row.append('<td><input class="' + perm + '" type="checkbox"' + (permissions[perm] ? ' checked="checked"' : '') + '"></td>');

			$('.' + perm, row).on('dblclick', function() {
				return false;
			});

			$('.' + perm, row).on('click', function(e) {
				e.preventDefault();

				var checkbox = $(this);
				checkbox.prop('disabled', true);

				if (!$('input:checked', row).length) {
					$('#newPrincipal').append('<option value="' + row.attr('class').substring(1) + '">' + $('.name', row).text() + '</option>').trigger('chosen:updated');
					row.remove();
				}
				var recursive = $('#recursive', dialogText).is(':checked');

				Command.setPermission(entity.id, principal.id, permissions[perm] ? 'revoke' : 'grant', perm, recursive, function() {
					permissions[perm] = !permissions[perm];
					checkbox.prop('checked', permissions[perm]);
					_Logger.log(_LogType.ENTITIES, 'Permission successfully updated!');

					disabled = false;
					checkbox.prop('disabled', false);

					blinkGreen(checkbox.parent());
				});
			});
		});
	},
	appendTextarea: function(el, entity, key, label, desc) {
		if (!el || !entity) {
			return false;
		}
		el.append('<div><h3>' + label + '</h3><p>' + desc + '</p><textarea class="query-text ' + key + '_">' + (entity[key] ? entity[key] : '') + '</textarea></div>');
		el.append('<div><button class="apply_' + key + '">Save</button></div>');
		var btn = $('.apply_' + key, el);
		btn.on('click', function() {
			Command.setProperty(entity.id, key, $('.' + key + '_', el).val(), false, function(obj) {
				_Logger.log(_LogType.ENTITIES, key + ' successfully updated!', obj[key]);
				blinkGreen(btn);
				_Pages.reloadPreviews();
			});
		});
	},
	appendInput: function(el, entity, key, label, desc) {
		if (!el || !entity) {
			return false;
		}
		el.append('<div><h3>' + label + '</h3><p>' + desc + '</p><div class="input-and-button"><input type="text" class="' + key + '_" value="' + (entity[key] ? entity[key] : '') + '"><button class="save_' + key + '">Save</button></div></div>');
		var btn = $('.save_' + key, el);
		btn.on('click', function() {
			Command.setProperty(entity.id, key, $('.' + key + '_', el).val(), false, function(obj) {
				_Logger.log(_LogType.ENTITIES, key + ' successfully updated!', obj[key]);
				blinkGreen(btn);
				_Pages.reloadPreviews();
			});
		});
	},
	appendBooleanSwitch: function(el, entity, key, label, desc, recElementId) {
		if (!el || !entity) {
			return false;
		}
		el.append('<div class="' + entity.id + '_"><button class="switch inactive ' + key + '_"></button>' + desc + '</div>');
		var sw = $('.' + key + '_', el);
		_Entities.changeBooleanAttribute(sw, entity[key], label[0], label[1]);
		sw.on('click', function(e) {
			e.stopPropagation();
			Command.setProperty(entity.id, key, sw.hasClass('inactive'), $(recElementId, el).is(':checked'), function(obj) {
				if (obj.id !== entity.id) {
					return false;
				}
				_Entities.changeBooleanAttribute(sw, obj[key], label[0], label[1]);
				blinkGreen(sw);
				return true;
			});
		});
	},
	appendSimpleSelection: function(el, entity, type, title, key, prefetchedData) {
		var subKey;
		if (key.contains('.')) {
			subKey = key.substring(key.indexOf('.') + 1, key.length);
			key = key.substring(0, key.indexOf('.'));
		}

		el.append('<h3>' + title + '</h3><p class="' + key + 'Box"></p>');
		var element = $('.' + key + 'Box', el);
		element.append('<span class="' + entity.id + '_"><select class="' + key + '_ ' + key + 'Select"></select></span>');
		var selectElement = $('.' + key + 'Select');
		selectElement.append('<option></option>');
		selectElement.css({'width': '400px'}).chosen();

		var id = (subKey && entity[key] ? entity[key][subKey] : entity[key]);
		var al = Structr.loaderIcon(el, {position: 'absolute', left: '416px', top: '32px'});

		var populateSelect = function (results) {
			results.forEach(function(result) {
				var selected = (id === result.id ? 'selected' : '');
				selectElement.append('<option ' + selected + ' value="' + result.id + '">' + result.name + '</option>');
			});
			selectElement.trigger("chosen:updated");
			if (al.length) {
				al.remove();
			}
		};

		if (prefetchedData === undefined || prefetchedData.length === 0) {
			var n = 10000;
			Command.getByType(type, n, 1, 'name', 'asc', 'id,name', false, populateSelect);
		} else {
			populateSelect(prefetchedData);
		}

		selectElement.on('change', function() {

			var value = selectElement.val();
			if (subKey) {
				if (!entity[key]) {
					entity[key] = {};
				}
				entity[key][subKey] = value;
			}

			Command.setProperty(entity.id, key, value, false, function() {
				blinkGreen(el);
			});
		});
	},
	appendEditSourceIcon: function(parent, entity) {

		if (_Entities.pencilEditBlacklist.indexOf(entity.tag) === -1) {

			var editIcon = $('.edit_icon', parent);

			if (!(editIcon && editIcon.length)) {
				parent.append('<i title="Edit source code" class="edit_icon button ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" />');
				editIcon = $('.edit_icon', parent);
			}
			editIcon.on('click', function(e) {
				e.stopPropagation();
				_Logger.log(_LogType.ENTITIES, 'editSource', entity);
				_Entities.editSource(entity);
			});

		}

	},
	appendEditPropertiesIcon: function(parent, entity, visible) {

		var editIcon = $('.edit_props_icon', parent);

		if (!(editIcon && editIcon.length)) {
			parent.append('<i title="Edit Properties" class="edit_props_icon button ' + _Icons.getFullSpriteClass(_Icons.view_detail_icon) + '" />');
			editIcon = $('.edit_props_icon', parent);
		}
		editIcon.on('click', function(e) {
			e.stopPropagation();
			_Logger.log(_LogType.ENTITIES, 'showProperties', entity);
			_Entities.showProperties(entity);
		});
		if (visible) {
			editIcon.css({
				visibility: 'visible',
				display: 'inline-block'
			});
		}
		return editIcon;
	},
	appendExpandIcon: function(el, entity, hasChildren, expanded) {

		_Logger.log(_LogType.ENTITIES, '_Entities.appendExpandIcon', el, entity, hasChildren, expanded);

		var button = $(el.children('.expand_icon').first());
		if (button && button.length) {
			_Logger.log(_LogType.ENTITIES, 'Expand icon already existing', el, button);
			return;
		}

		if (hasChildren) {

			_Logger.log(_LogType.ENTITIES, 'appendExpandIcon hasChildren?', hasChildren, 'expand?', expanded);

			var typeIcon = $(el.children('.typeIcon').first());
			var icon = expanded ? _Icons.expanded_icon : _Icons.collapsed_icon;

			var displayName = getElementDisplayName(entity);

			typeIcon.removeClass('typeIcon-nochildren').after('<i title="Expand ' + displayName + '" class="expand_icon ' + _Icons.getFullSpriteClass(icon) + '" />');

			$(el).on('click', function(e) {
				e.stopPropagation();
				_Entities.toggleElement(this);
			});

			button = $(el.children('.expand_icon').first());

			if (button) {

				button.on('click', function(e) {
					e.stopPropagation();
					_Entities.toggleElement($(this).parent('.node'));
				});

				// Prevent expand icon from being draggable
				button.on('mousedown', function(e) {
					e.stopPropagation();
				});

				if (expanded) {
					_Entities.ensureExpanded(el);
				}
			}

		} else {
			el.children('.typeIcon').addClass('typeIcon-nochildren');
		}

	},
	removeExpandIcon: function(el) {
		if (!el)
			return;
		_Logger.log(_LogType.ENTITIES, 'removeExpandIcon', el);
		var button = $(el.children('.expand_icon').first());

		// unregister click handlers
		$(el).off('click');
		$(button).off('click');

		button.remove();
		el.children('.typeIcon').addClass('typeIcon-nochildren');
	},
	makeSelectable: function(el) {
		var node = $(el).closest('.node');
		if (!node || !node.children) {
			return;
		}
		node.on('click', function() {
			$(this).toggleClass('selected');
		});
	},
	setMouseOver: function(el, allowClick, syncedNodesIds) {
		var node = $(el).closest('.node');
		if (!node || !node.children) {
			return;
		}

		if (!allowClick) {
			node.on('click', function(e) {
				e.stopPropagation();
				return false;
			});
		}

		node.children('b.name_').off('click').on('click', function(e) {
			e.stopPropagation();
			_Entities.makeNameEditable(node);
		});

		var nodeId = Structr.getId(el), isComponent;
		if (nodeId === undefined) {
			nodeId = Structr.getComponentId(el);
			if (nodeId) {
				isComponent = true;
			} else {
				nodeId = Structr.getActiveElementId(el);
			}
		}

		node.on({
			mouseover: function(e) {
				e.stopPropagation();
				var self = $(this);
				$('#componentId_' + nodeId).addClass('nodeHover');
				if (isComponent) {
					$('#id_' + nodeId).addClass('nodeHover');
				}

				if (syncedNodesIds && syncedNodesIds.length) {
					syncedNodesIds.forEach(function(s) {
						$('#id_' + s).addClass('nodeHover');
						$('#componentId_' + s).addClass('nodeHover');
					});
				}

				var page = $(el).closest('.page');
				if (page.length) {
					try {
						$('#preview_' + Structr.getId(page)).contents().find('[data-structr-id=' + nodeId + ']').addClass('nodeHover');
					} catch (e) {}
				}
				self.addClass('nodeHover');
				self.children('i.button').showInlineBlock();
//				self.children('.icons').children('img.button').show();
			},
			mouseout: function(e) {
				e.stopPropagation();
				$('#componentId_' + nodeId).removeClass('nodeHover');
				if (isComponent) {
					$('#id_' + nodeId).removeClass('nodeHover');
				}
				if (syncedNodesIds && syncedNodesIds.length) {
					syncedNodesIds.forEach(function(s) {
						$('#id_' + s).removeClass('nodeHover');
						$('#componentId_' + s).removeClass('nodeHover');
					});
				}
				_Entities.resetMouseOverState(this);
			}
		});
	},
	resetMouseOverState: function(element) {
		var el = $(element);
		var node = el.closest('.node');
		if (node) {
			node.removeClass('nodeHover');
//			node.find('img.button').not('.donthide').hide().css('display', 'none');
			node.find('i.button').not('.donthide').hide().css('display', 'none');
		}
		var page = node.closest('.page');
		if (page.length) {
			try {
				$('#preview_' + Structr.getId(page)).contents().find('[data-structr-id]').removeClass('nodeHover');
			} catch (e) {}
		}
	},
	isExpanded: function(element) {
		var b = $(element).children('.expand_icon').first();
		if (!b) {
			return false;
		}
		return b.hasClass(_Icons.getSpriteClassOnly(_Icons.expanded_icon));
	},
	ensureExpanded: function(element, callback) {
		if (!element) {
			return;
		}
		var el = $(element);
		var id = Structr.getId(el);

		if (!id) {
			return;
		}

		Structr.addExpandedNode(id);

		if (_Entities.isExpanded(element)) {
			return;
		} else {
			_Logger.log(_LogType.ENTITIES, 'ensureExpanded: fetch children', el);

			Command.children(id, callback);
			var displayName = getElementDisplayName(Structr.entity(id));

			el.children('.expand_icon').first()
				.removeClass(_Icons.getSpriteClassOnly(_Icons.collapsed_icon))
				.addClass(_Icons.getSpriteClassOnly(_Icons.expanded_icon))
				.prop('title', 'Collapse ' + displayName);
		}
	},
	expandAll: function(ids, lastId) {
		if (!ids || ids.length === 0) {
			return;
		}

		ids.forEach(function(id) {
			var el = Structr.node(id);

			if (el && id === lastId) {
				_Entities.deselectAllElements();
				_Entities.highlightElement(el);
			} else if (!el && id === lastId) {
				// if node is not present, delay and retry
				window.setTimeout(function() {
					var el = Structr.node(id);
					if (el) {
						_Entities.deselectAllElements();
						_Entities.highlightElement(el);
					}
				}, 500);
			}

			_Entities.ensureExpanded(el, function(childNodes) {
				if (childNodes && childNodes.length) {
					var childNode = childNodes[0];
					var i = ids.indexOf(childNode.id);
					if (i > 1) {
						ids.slice(i - 1, i);
					}
					_Entities.expandAll(ids, lastId);
				}
			});
		});
	},
	deselectAllElements: function () {
		$('.nodeSelected').removeClass('nodeSelected');
	},
	highlightElement:function(el) {
		el.addClass('nodeSelected');

		if (el.offset().top + el.height() + pages.scrollTop() > pages.prop('clientHeight')) {
			// element is *below* the currently visible portion of the pages tree

			// scroll to lower boundary of the element
			pages.animate({
				scrollTop: el.offset().top + el.height() + pages.scrollTop() - pages.prop('clientHeight')
			});

		} else if (el.offset().top - pages.offset().top < pages.scrollTop()) {
			// element is *above* the currently visible portion of the pages tree

			// scroll to upper boundary of element
			pages.animate({
				scrollTop: el.offset().top - pages.offset().top + pages.scrollTop()
			});

		}

	},
	toggleElement: function(element, expanded) {

		var el = $(element);
		var id = Structr.getId(el) || Structr.getComponentId(el) || Structr.getGroupId(el);

		_Logger.log(_LogType.ENTITIES, 'toggleElement: ', el, id);

		var b = el.children('.expand_icon').first();
		var displayName = getElementDisplayName(Structr.entity(id));

		if (_Entities.isExpanded(element)) {

			$.each(el.children('.node'), function(i, child) {
				$(child).remove();
			});

			b.removeClass(_Icons.getSpriteClassOnly(_Icons.expanded_icon))
				.addClass(_Icons.getSpriteClassOnly(_Icons.collapsed_icon))
				.prop('title', 'Expand ' + displayName);

			Structr.removeExpandedNode(id);
		} else {

			if (!expanded) {
				_Logger.log(_LogType.ENTITIES, 'toggleElement: fetch children', id);
				Command.children(id);
			}

			b.removeClass(_Icons.getSpriteClassOnly(_Icons.collapsed_icon))
				.addClass(_Icons.getSpriteClassOnly(_Icons.expanded_icon))
				.prop('title', 'Collapse ' + displayName);

			Structr.addExpandedNode(id);
		}

	},
	makeAttributeEditable: function(parentElement, id, attributeSelector, attributeName, width, callback) {
		var w = width || 200;
		var attributeElement = parentElement.find(attributeSelector).first();
		var attributeElementTagName = attributeElement.prop('tagName').toLowerCase();
		var oldValue = $.trim(attributeElement.attr('title'));

		attributeElement.replaceWith('<input type="text" size="' + (oldValue.length + 4) + '" class="new-' + attributeName + '" value="' + oldValue + '">');

		var input = $('input', parentElement);
		input.focus().select();

		var makeNonEditable = function (el, commitChanges) {
			var displayValue = (commitChanges === true) ? el.val() : oldValue;
			el.replaceWith('<' + attributeElementTagName + ' title="' + displayValue + '" class="' + attributeName + '_">' + fitStringToWidth(displayValue, w) + '</' + attributeElementTagName + '>');
			parentElement.find(attributeSelector).first().off('click').on('click', function(e) {
				e.stopPropagation();
				_Entities.makeAttributeEditable(parentElement, id, attributeSelector, attributeName, w);
			});
			if (commitChanges === true) {
				_Entities.setNewAttributeValue(parentElement, id, attributeName, displayValue, callback, function () {
					var attributeElement = parentElement.find(attributeSelector).first();
					attributeElement.attr('title', oldValue).text(oldValue);
					blinkRed(parentElement);
				});
			}
		};

		input.on('blur', function() {
			makeNonEditable($(this), true);
		});

		input.keydown(function(e) {
			if (e.keyCode === 13) {
				makeNonEditable($(this), true);

			} else if (e.keyCode === 27) {
				e.stopPropagation();
				makeNonEditable($(this), false);
			}
		});
	},
	makeNameEditable: function(element, width, callback) {
		var id = Structr.getId(element);
		_Entities.makeAttributeEditable(element, id, 'b.name_', 'name', width, callback);
	},
	setNewName: function(element, newName, callback) {
		var id = Structr.getId(element);
		_Entities.setNewAttributeValue(element, id, 'name', newName, callback);
	},
	setNewAttributeValue: function(element, id, attributeName, newValue, callback, failCallback) {
		Command.setProperty(id, attributeName, newValue, false, function(entity, resultSize, errorOccurred) {
			if (!errorOccurred || errorOccurred === false) {
				blinkGreen(element.find('.' + attributeName + '_').first());
				if (Structr.isModuleActive(_Pages)) {
					_Pages.reloadPreviews();
				} else if (Structr.isModuleActive(_Files) && attributeName === 'name') {
					var a = element.closest('td').prev().children('a').first();
					Command.getProperty(id, 'path', function(newPath) {
						a.attr('href', newPath);
					});
				}
				if (callback) {
					callback();
				}
			} else if (failCallback) {
				failCallback();
			}
		});
	},
	handleActiveElement: function(entity) {

		if (entity) {

			var idString = 'id_' + entity.id;

			if (!_Entities.activeElements.hasOwnProperty(idString)) {

				_Entities.activeElements[idString] = entity;

				var parent = $('#activeElements div.inner');
				var id = entity.id;

				if (entity.parentId) {
					parent = $('#active_' + entity.parentId);
				}

				parent.append('<div id="active_' + id + '" class="node active-element' + (entity.tag === 'html' ? ' html_element' : '') + ' "></div>');

				var div = $('#active_' + id);
				var query = entity.query;
				var expand = entity.state === 'Query';
				var icon = _Icons.brick_icon;
				var name = '', content = '', action = '';

				switch (entity.state) {
					case 'Query':
						icon = _Icons.database_table_icon;
						name = query || entity.dataKey.replace(',', '.');
						break;
					case 'Content':
						icon = _Icons.page_white_icon;
						content = entity.content ? entity.content : entity.type;
						break;
					case 'Button':
						icon = _Icons.button_icon;
						action = entity.action;
						break;
					case 'Link':
						icon = _Icons.link_icon;
						content = entity.action;
						break;
					default:
						content = entity.type;
				}

				div.append('<i class="typeIcon ' + _Icons.getFullSpriteClass(icon) + '" />'
					+ '<b title="' + name + '">' + fitStringToWidth(name, 180, 'slideOut') + '</b>'
					+ '<b class="action">' + (action ? action : '&nbsp;') + '</b>'
					+ '<span class="content_">' + (content ? fitStringToWidth(content, 180, 'slideOut') : '&nbsp;') + '</span>'
					+ '<span class="id">' + entity.id + '</span>'
				);

				_Entities.setMouseOver(div);

				var editIcon = $('.edit_icon', div);

				if (!(editIcon && editIcon.length)) {
					if (entity.state === 'Content') {
						div.append('<i title="Edit" class="edit_icon button ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" />');
					} else {
						div.append('<i title="Edit Properties" class="edit_icon button ' + _Icons.getFullSpriteClass(_Icons.view_detail_icon) + '" />');
					}
					editIcon = $('.edit_icon', div);
				}
				editIcon.on('click', function(e) {
					e.stopPropagation();

					switch (entity.state) {
						case 'Query':
							_Entities.showProperties(entity, 'query');
							break;
						case 'Content':
							_Elements.openEditContentDialog(this, entity);
							break;
						case 'Button':
							_Entities.showProperties(entity, 'editBinding');
							break;
						case 'Link':
							_Entities.showProperties(entity);
							break;
						default:
							_Entities.showProperties(entity);
					}

				});

				$('b[title]', div).on('click', function() {
					_Entities.showProperties(entity, 'query');
				});

				$('.content_', div).on('click', function() {
					_Elements.openEditContentDialog(this, entity);
				});

				$('.action', div).on('click', function() {
					_Entities.showProperties(entity, 'editBinding');
				});

				var typeIcon = $(div.children('.typeIcon').first());
				var displayName = getElementDisplayName(entity);

				if (!expand) {
					typeIcon.addClass('typeIcon-nochildren');
				} else {
					typeIcon.removeClass('typeIcon-nochildren').after('<i title="Expand ' + displayName + '" class="expand_icon ' + _Icons.getFullSpriteClass(_Icons.expanded_icon) + '" />');
				}
			}
		}
	}
};

function formatValueInputField(key, obj, isPassword, isReadOnly, isMultiline) {

	if (obj === null) {

		return formatRegularValueField(key, '', isMultiline, isReadOnly, isPassword);

	} else if (obj.constructor === Object) {

		var displayName = _Crud.displayName(obj);
		return '<div title="' + displayName + '" id="_' + obj.id + '" class="node ' + (obj.type ? obj.type.toLowerCase() : (obj.tag ? obj.tag : 'element')) + ' ' + obj.id + '_">' + fitStringToWidth(displayName, 80) + '<i class="remove ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></div>';

	} else if (obj.constructor === Array) {

		return obj.reduce(function (acc, v) {
			return acc + formatValueInputField(key, v, isPassword, isReadOnly, isMultiline) + '<br>';
		}, '');

	} else {

		return formatRegularValueField(key, escapeForHtmlAttributes(obj), isMultiline, isReadOnly, isPassword);
	}
};

function formatRegularValueField(key, value, isMultiline, isReadOnly, isPassword) {
	if (isMultiline) {
		return '<textarea name="' + key + '"' + (isReadOnly ? ' readonly class="readonly"' : '') + '>' + value + '</textarea>';
	} else {
		return '<input name="' + key + '" type="' + (isPassword ? 'password" autocomplete="new-password' : 'text') + '" value="' + value + '"' + (isReadOnly ? 'readonly class="readonly"' : '') + '>';
	}
};