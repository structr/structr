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
var buttonClicked;

var _Entities = {
	activeElements: {},
	activeQueryTabPrefix: 'structrActiveQueryTab_' + port,
	activeEditTabPrefix: 'structrActiveEditTab_' + port,
	numberAttrs: ['position', 'size'],
	readOnlyAttrs: ['lastModifiedDate', 'createdDate', 'createdBy', 'id', 'checksum', 'size', 'version', 'relativeFilePath'],
	pencilEditBlacklist: ['html', 'body', 'head', 'title', 'script',  'input', 'label', 'button', 'textarea', 'link', 'meta', 'noscript', 'tbody', 'thead', 'tr', 'td', 'caption', 'colgroup', 'tfoot', 'col', 'style'],
	null_prefix: 'null_attr_',
	collectionPropertiesResultCount: {},
	changeBooleanAttribute: function(attrElement, value, activeLabel, inactiveLabel) {

		if (value === true) {
			attrElement.removeClass('inactive').addClass('active').prop('checked', true).html('<i class="' + _Icons.getFullSpriteClass(_Icons.tick_icon) + '" />' + (activeLabel ? ' ' + activeLabel : ''));
		} else {
			attrElement.removeClass('active').addClass('inactive').prop('checked', false).text((inactiveLabel ? inactiveLabel : '-'));
		}

	},
	reloadChildren: function(id) {
		var el = Structr.node(id);

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

		el.append('<h3>Simple Interactive Elements</h3>');
		el.append('<table class="props" id="new-data-binding-properties"></table>');
		var tNew = $('#new-data-binding-properties', el);

		_Entities.appendRowWithInputField(entity, tNew, 'eventMapping',                    'Event mapping',     typeInfo);
		_Entities.appendRowWithInputField(entity, tNew, 'data-structr-target',             'Event target',      typeInfo);
		_Entities.appendRowWithInputField(entity, tNew, 'data-structr-reload-target',      'Reload target',     typeInfo);
		_Entities.appendRowWithInputField(entity, tNew, 'data-structr-tree-children',      'Tree children key', typeInfo);

		if (entity.type === 'Button' || entity.type === 'A') {

			el.append('<h4>You can specify the data fields for create and update using data-Attributes like this:</h4>');
			el.append('<pre>data-name        = css(input#name-input)\ndata-description = css(input#description-input)\ndata-parent      = json({ id: "5c6214fde6db45d09df027b16a0d6c0e" })</pre>');
			el.append('<h4>Which will produce the following JSON payload:</h4>');
			el.append('<pre>{\n    name: "&lt;value from input#name-input&gt;",\n    description: "&lt;value from input#description-input&gt;",\n    parent: {\n        id: "5c6214fde6db45d09df027b16a0d6c0e"\n    }\n}\n</p>');
		}

		el.append('<h3>Deprecated Edit Mode Binding</h3>');
		el.append('<table class="props" id="deprecated-data-binding-properties"></table>');
		var tOld = $('#deprecated-data-binding-properties', el);

		// General
		_Entities.appendRowWithInputField(entity, tOld, 'data-structr-id',                   'Element ID', typeInfo);
		_Entities.appendRowWithInputField(entity, tOld, 'data-structr-attr',                 'Attribute Key', typeInfo);
		_Entities.appendRowWithInputField(entity, tOld, 'data-structr-type',                 'Data type', typeInfo);
		_Entities.appendRowWithInputField(entity, tOld, 'data-structr-placeholder',          'Placeholder text', typeInfo);
		_Entities.appendRowWithInputField(entity, tOld, 'data-structr-custom-options-query', 'Custom REST query', typeInfo);
		_Entities.appendRowWithInputField(entity, tOld, 'data-structr-options-key',          'Options attribute key', typeInfo);
		_Entities.appendRowWithInputField(entity, tOld, 'data-structr-raw-value',            'Raw value', typeInfo);
		_Entities.appendRowWithInputField(entity, tOld, 'data-structr-hide',                 'Hide mode(s)', typeInfo);
		_Entities.appendRowWithInputField(entity, tOld, 'data-structr-edit-class',           'Edit mode CSS class', typeInfo);

		if (entity.type === 'Button' || entity.type === 'A') {

			_Entities.appendRowWithInputField(entity, tOld, 'data-structr-action',           'Action', typeInfo);
			_Entities.appendRowWithInputField(entity, tOld, 'data-structr-attributes',       'Attributes', typeInfo);
			_Entities.appendRowWithBooleanSwitch(entity, tOld, 'data-structr-reload',        'Reload', '', typeInfo);
			_Entities.appendRowWithBooleanSwitch(entity, tOld, 'data-structr-confirm',       'Confirm action?', '', typeInfo);
			_Entities.appendRowWithInputField(entity, tOld, 'data-structr-return',           'Return URI', typeInfo);
			_Entities.appendRowWithBooleanSwitch(entity, tOld, 'data-structr-append-id',     'Append ID on create', '', typeInfo);

		} else if (entity.type === 'Input' || entity.type === 'Select' || entity.type === 'Textarea') {
			_Entities.appendRowWithInputField(entity, tOld, 'data-structr-name',             'Field name', typeInfo);
			_Entities.appendRowWithInputField(entity, tOld, 'data-structr-format',           'Custom Format', typeInfo);
		}
	},
	appendRowWithInputField: function(entity, el, key, label, typeInfo) {
		el.append('<tr><td class="key">' + label + '</td><td class="value"><input class="' + key + '_" name="' + key + '" value="' + (entity[key] ? escapeForHtmlAttributes(entity[key]) : '') + '"></td><td><i id="null_' + key + '" class="nullIcon ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></td></tr>');
		var inp = $('[name="' + key + '"]', el);
		_Entities.activateInput(inp, entity.id, entity.pageId, typeInfo);
		var nullIcon = $('#null_' + key, el);
		nullIcon.on('click', function() {
			Command.setProperty(entity.id, key, null, false, function() {
				inp.val(null);
				blinkGreen(inp);
				Structr.showAndHideInfoBoxMessage('Property "' + key + '" has been set to null.', 'success', 2000, 1000);
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
		return _Entities.repeaterConfig(entity, el);

	},
	repeaterConfig: function(entity, el) {

		var queryTypes = [
			{ title: 'REST Query',     propertyName: 'restQuery' },
			{ title: 'Cypher Query',   propertyName: 'cypherQuery' },
//			{ title: 'XPath Query',    propertyName: 'xpathQuery' },
			{ title: 'Function Query', propertyName: 'functionQuery' }
		];

		if (Structr.isModulePresent('flows')) {
			queryTypes.unshift({ title: 'Flow', propertyName: 'flow' });
		}

		var queryTypeButtonsContainer = $('<div class="query-type-buttons"></div>');
		el.append('<h3>Repeater Configuration</h3>').append(queryTypeButtonsContainer);

		var queryHeading = $('<h4 class="query-type-heading"></h4>').appendTo(el);

		var textArea = $('<textarea class="hidden query-text"></textarea>').appendTo(el);
		var flowSelector = $('#flow-selector');

		var initRepeaterInputs = function() {

			var saveBtn = $('<button class="action">Save</button>');
			el.append('<br>').append(saveBtn);

			queryTypes.forEach(function(queryType) {

				var btn = $('<button data-query-type="' + queryType.propertyName + '">' + queryType.title + '</button>');
				btn.addClass(queryType.propertyName);
				queryTypeButtonsContainer.append(btn);

				if (queryType.propertyName === 'flow' && entity[queryType.propertyName]) {
					btn.addClass('active');
					btn.click();
					var flow = entity[queryType.propertyName];
					saveBtn.hide();
					textArea.hide();
					flowSelector.show();
					if (flow) {
						//var flowName = flow.effectiveName;
						//$('option', flowSelector).filter(function () { console.log($(this).text()); return $(this).text() === flowName; }).attr('selected', 'selected');
						flowSelector.val(flow.id);
					}

				} else {

					if (entity[queryType.propertyName] && entity[queryType.propertyName].trim() !== "") {
						btn.addClass('active');
						saveBtn.show();
						textArea.show();
						flowSelector.hide();
						$('button.flow').removeClass('active');
						textArea.text(textArea.text() + entity[queryType.propertyName]);
						queryHeading.text(btn.text());
					}
				}
			});

			var allButtons = $('.query-type-buttons button');
			allButtons.on('click', function () {
				allButtons.removeClass('active');
				var btn = $(this);
				btn.addClass('active');
				var queryType = btn.data('query-type');
				queryHeading.text(btn.text());

				if (queryType === 'flow') {
					saveBtn.hide();
					textArea.hide();
					flowSelector.show();

				} else {
					saveBtn.show();
					textArea.show();
					flowSelector.hide();
				}
			});

			if ($('button.active', queryTypeButtonsContainer).length === 0) {
				$('.query-type-buttons button:first', el).click();
			}


			flowSelector.on('change', function() {
				saveBtn.click();
			});

			saveBtn.on('click', function() {

				if ($('button.active', queryTypeButtonsContainer).length > 1) {
					return new MessageBuilder().error('Please select only one query type!').show();
				}

				var data = {};
				queryTypes.forEach(function(queryType) {
					var val = null;
					if ($('.' + queryType.propertyName, queryTypeButtonsContainer).hasClass('active')) {
						if (queryType.propertyName === 'flow') {

							val = flowSelector.val();

						} else {
							val = textArea.val();
							data.flow = null;
							flowSelector.val('--- Select Flow ---');
						}
					}
					data[queryType.propertyName] = val;
				});

				Command.setProperties(entity.id, data, function(obj) {
					blinkGreen(saveBtn);

					_Pages.reloadPreviews();
				});
			});

			_Entities.appendInput(el, entity, 'dataKey', 'Data Key', 'The data key is either a word to reference result objects, or it can be the name of a collection property of the result object.<br>' +
				'You can access result objects or the objects of the collection using ${<i>&lt;dataKey&gt;.&lt;propertyKey&gt;</i>}');

			_Entities.activateTabs(entity.id, '#data-tabs', '#content-tab-rest');
		};

		if (Structr.isModulePresent('flows')) {

			if (flowSelector && flowSelector.length) {
				flowSelector.remove();
			}

			flowSelector = $('<select class="hidden" id="flow-selector"></select>').insertBefore(textArea);

			flowSelector.append('<option>--- Select Flow ---</option>');
			// (type, pageSize, page, sort, order, properties, includeHidden, callback)
			Command.getByType('FlowContainer', 1000, 1, 'effectiveName', 'asc', null, false, function(flows) {

				flows.forEach(function(flow) {
					flowSelector.append('<option value="' + flow.id + '">' + flow.effectiveName + '</option>');
				});

				initRepeaterInputs();
			});
		} else {
			initRepeaterInputs();
		}
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
		}, function () {
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

				// Intitialize editor
				CodeMirror.defineMIME('text/html', 'htmlmixed-structr');
				editor = CodeMirror(contentBox.get(0), Structr.getCodeMirrorSettings({
					value: unescapeTags(innerText),
					mode: contentType,
					lineNumbers: true,
					lineWrapping: false,
					indentUnit: 4,
					tabSize:4,
					indentWithTabs: true
				}));

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
						dialogSaveButton.prop('disabled', true).addClass('disabled');
						saveAndClose.prop('disabled', true).addClass('disabled');
					} else {
						dialogSaveButton.prop('disabled', false).removeClass('disabled');
						saveAndClose.prop('disabled', false).removeClass('disabled');
					}

					_Entities.hideDataHashAttribute(editor);
				});

				dialogSaveButton.on('click', function(e) {
					e.stopPropagation();
					text2 = editor.getValue();
					//console.log(text, text2, text === text2);
					if (text === text2) {
						dialogSaveButton.prop('disabled', true).addClass('disabled');
						saveAndClose.prop('disabled', true).addClass('disabled');
					} else {
						dialogSaveButton.prop('disabled', false).removeClass('disabled');
						saveAndClose.prop('disabled', false).removeClass('disabled');
					}

					Command.saveNode(startTag + editor.getValue() + endTag, entity.id, function() {
						$.ajax({
							url: url,
							contentType: contentType,
							success: function(data) {
								text = unescapeTags(data).replace(/<!DOCTYPE[^>]*>/, '');
								text = text.replace(/(<[^>]*>)([^]*)(<\/[^>]*>)/, '$2').replace(/^\s+|\s+$/g, '');
								editor.setValue(text);

								dialogSaveButton.prop('disabled', true).addClass('disabled');
								saveAndClose.prop('disabled', true).addClass('disabled');
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

				dialogMeta.append('<span class="editor-info"><label for"lineWrapping">Line Wrapping:</label> <input id="lineWrapping" type="checkbox"' + (Structr.getCodeMirrorSettings().lineWrapping ? ' checked="checked" ' : '') + '></span>');
				$('#lineWrapping').off('change').on('change', function() {
					var inp = $(this);
					Structr.updateCodeMirrorOptionGlobally('lineWrapping', inp.is(':checked'));
					blinkGreen(inp.parent());
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
	getSchemaProperties: function(type, view, callback) {
		let url = rootUrl + '_schema/' + type + '/' + view;
		$.ajax({
			url: url,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function(data) {

					let properties = {};
					// no schema entry found?
					if (!data || !data.result || data.result_count === 0) {

					} else {

						data.result.forEach(function(prop) {
							properties[prop.jsonName] = prop;
						});
					}

					if (callback) {
						callback(properties);
					}
				},
				400: function(data) {
					Structr.errorFromResponse(data.responseJSON, url);
				},
				401: function(data) {
					Structr.errorFromResponse(data.responseJSON, url);
				},
				403: function(data) {
					Structr.errorFromResponse(data.responseJSON, url);
				},
				404: function(data) {
					Structr.errorFromResponse(data.responseJSON, url);
				},
				422: function(data) {
					Structr.errorFromResponse(data.responseJSON, url);
				}
			},
			error:function () {
				console.log("ERROR: loading Schema " + type);
			}
		});
	},
	showProperties: function(obj, activeViewOverride) {

		let handleGraphObject;

		_Entities.getSchemaProperties(obj.type, 'custom', function(properties) {

			handleGraphObject = function(entity) {

				var views      = ['ui'];

				if (Object.keys(properties).length) {
					views.push('custom');
				}

				var activeView = 'ui';
				var tabTexts   = [];

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

						if (entity.isDOMNode && !entity.isContent) {
							views.unshift('_html_');
							if (Structr.isModuleActive(_Pages)) {
								activeView = '_html_';
							}
						}

						tabTexts._html_ = 'HTML Attributes';
						tabTexts.ui = 'Built-in Properties';
						tabTexts.custom = 'Custom Properties';

						dialogTitle = 'Edit properties of ' + (entity.type ? entity.type : '') + ' node ' + (entity.name ? entity.name : entity.id);

					}

					Structr.dialog(dialogTitle, function() { return true; }, function() { return true; });

					var tabsdiv = dialogHead.append('<div id="tabs"></div>');
					var mainTabs = tabsdiv.append('<ul></ul>');
					var contentEl = dialog.append('<div></div>');

					// custom dialog tab?
					var hasCustomDialog = _Dialogs.findAndAppendCustomTypeDialog(entity, mainTabs, contentEl);

					if (entity.isDOMNode) {

						if (entity.isContent !== true || entity.type === 'Template') {

							_Entities.appendPropTab(entity, mainTabs, contentEl, 'query', 'Query and Data Binding', !hasCustomDialog, function(c) {
								_Entities.queryDialog(entity, c, typeInfo);
							}, function() { }, function() { });
						}

						if (entity.isContent !== true) {

							_Entities.appendPropTab(entity, mainTabs, contentEl, 'editBinding', 'Edit Mode Binding', false, function(c) {
								_Entities.dataBindingDialog(entity, c, typeInfo);
							});

						}

					}

					_Entities.appendViews(entity, views, tabTexts, mainTabs, contentEl, typeInfo);

					if (!entity.hasOwnProperty('relType')) {
						_Entities.appendPropTab(entity, mainTabs, contentEl, 'permissions', 'Access Control and Visibility', false, function(c) {
							_Entities.accessControlDialog(entity, c, typeInfo);
						});
					}

					activeView = activeViewOverride || LSWrapper.getItem(_Entities.activeEditTabPrefix  + '_' + entity.id) || activeView;
					$('#tab-' + activeView).click();

					Structr.resize();

				});

			};

			if (obj.relType) {
				Command.getRelationship(obj.id, obj.target, null, function(entity) { handleGraphObject(entity); });
			} else {
				Command.get(obj.id, null, function(entity) { handleGraphObject(entity); }, 'ui');
			}

		});

	},
	appendPropTab: function(entity, tabsEl, contentEl, name, label, active, callback, initCallback, showCallback) {

		var ul = tabsEl.children('ul');
		ul.append('<li id="tab-' + name + '"><div class="fill-pixel"></div>' + label + '</li>');

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

			if (typeof showCallback === "function") {

				// update entity for show callback
				if (entity.relType) {
					Command.getRelationship(entity.id, entity.target, null, function(e) { showCallback($('#tabView-' + name), e); });
				} else {
					Command.get(entity.id, null, function(e) { showCallback($('#tabView-' + name), e); });
				}
			}
		});
		contentEl.append('<div class="propTabContent" id="tabView-' + name + '"></div>');
		var content = $('#tabView-' + name);
		if (active) {
			content.show();
		}
		if (callback) {
			callback(content, entity);
		}
		if (active && typeof initCallback === "function") {
			initCallback();
		}
		return content;
	},
	appendViews: function(entity, views, texts, tabsEl, contentEl, typeInfo) {

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

				_Entities.listProperties(entity, view, tabView, typeInfo, function() {
					$('input.dateField', tabView).each(function(i, input) {
						_Entities.activateDatePicker($(input));
					});
				});
			});
		});
	},
	getNullIconForKey: function(key) {
		return '<i id="' + _Entities.null_prefix + key + '" class="nullIcon ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" />';
	},
	listProperties: function(entity, view, tabView, typeInfo, callback) {

		_Entities.getSchemaProperties(entity.type, view, function(properties) {

			let filteredProperties = Object.keys(properties).filter(function(key) {
				return !(typeInfo[key].isCollection && typeInfo[key].relatedType);
			});

			let collectionProperties = Object.keys(properties).filter(function(key) {
				return typeInfo[key].isCollection && typeInfo[key].relatedType;
			});

			$.ajax({
				url: rootUrl + entity.type + '/' + entity.id + '/all?edit=2',
				dataType: 'json',
				headers: {
					Accept: 'application/json; charset=utf-8; properties=' + filteredProperties.join(',')
				},
				contentType: 'application/json; charset=utf-8',
				success: function(data) {
					// Default: Edit node id
					var id = entity.id;

					var tempNodeCache = new AsyncObjectCache(function(id) {
						Command.get(id, 'id,name,type,tag,isContent,content', function (node) {
							tempNodeCache.addObject(node, node.id);
						});
					});

					// ID of graph object to edit
					$(data.result).each(function(i, res) {

						// reset id for each object group
						var keys = Object.keys(properties);

						var noCategoryKeys = [];
						var groupedKeys = {};

						if (typeInfo) {
							keys.forEach(function(key) {

								if (typeInfo[key] && typeInfo[key].category && typeInfo[key].category !== 'System') {

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

						if (view === '_html_') {
							// add custom html attributes
							Object.keys(res).forEach(function(key) {
								if (key.startsWith('_custom_html_')) {
									noCategoryKeys.push(key);
								}
							});
						}

						// reset result counts
						_Entities.collectionPropertiesResultCount = {};

						_Entities.createPropertyTable(null, noCategoryKeys, res, entity, view, tabView, typeInfo, tempNodeCache);
						Object.keys(groupedKeys).sort().forEach(function(categoryName) {
							_Entities.createPropertyTable(categoryName, groupedKeys[categoryName], res, entity, view, tabView, typeInfo, tempNodeCache);
						});

						// populate collection properties with first page
						collectionProperties.forEach(function(key) {
							_Entities.displayCollectionPager(tempNodeCache, entity, key, 1);
						});
					});

					if (typeof callback === 'function') {
						callback(properties);
					}
				}
			});
		});
	},
	displayCollectionPager: function(tempNodeCache, entity, key, page) {

		let pageSize = 10, resultCount;

		let cell = $('.value.' + key + '_');
		cell.css('height', '60px');

		$.ajax({
			url: rootUrl + entity.type + '/' + entity.id + '/' + key + '?pageSize=' + pageSize + '&page=' + page,
			dataType: 'json',
			headers: {
				Accept: 'application/json; charset=utf-8; properties=id,name'
			},
			contentType: 'application/json; charset=utf-8',
			success: function(data) {

				resultCount = _Entities.collectionPropertiesResultCount[key] || data.result_count;

				if (data.result.length < pageSize) {
					_Entities.collectionPropertiesResultCount[key] = (page-1)*pageSize+data.result.length;
					resultCount = _Entities.collectionPropertiesResultCount[key];
				}

				if (!cell.prev('td.key').find('.pager').length) {

					// display arrow buttons
					cell.prev('td.key').append('<div class="pager up disabled"><i title="Previous Page" class="fa fa-caret-up"></i></div><div class="pager range"></div><div class="pager down"><i title="Next Page" class="fa fa-caret-down"></i></div>');

					// display result count
					cell.prev('td.key').append(' <span></span>');
				}

				// update result count
				cell.prev('td.key').find('span').text('(' + ((resultCount !== undefined) ? resultCount : '?') + ')');

				let pageUpButton   = cell.prev('td.key').find('.pager.up');
				let pageDownButton = cell.prev('td.key').find('.pager.down');

				pageUpButton.off('click').addClass('disabled');
				pageDownButton.off('click').addClass('disabled');

				if (page > 1) {
					pageUpButton.removeClass('disabled').on('click', function() {
						_Entities.displayCollectionPager(tempNodeCache, entity, key, page-1);
						return false;
					});
				}

				if ((!resultCount && data.result.length > 0) || page < Math.ceil(resultCount/pageSize)) {
					pageDownButton.removeClass('disabled').on('click', function() {
						_Entities.displayCollectionPager(tempNodeCache, entity, key, page+1);
						return false;
					});
				}

				// don't update cell and fix page no if we're already on the last page
				if (page > 1 && data.result.length === 0) {
					page--;
				} else {
					cell.children('.node').remove();
				}

				if (resultCount === undefined || resultCount > 0) {
					// display current range
					cell.prev('td.key').find('.pager.range').text((page-1)*pageSize+1 + '..' + (resultCount ? Math.min(resultCount, page*pageSize) : '?'));
				}

				if (data.result.length) {

					(data.result[0][key] || data.result).forEach(function(obj) {

						let nodeId = (typeof obj === 'string') ? obj : obj.id;

						tempNodeCache.registerCallback(nodeId, nodeId, function(node) {
							_Entities.appendRelatedNode(cell, node, function(nodeEl) {
								$('.remove', nodeEl).on('click', function(e) {
									e.preventDefault();
									Command.removeFromCollection(entity.id, key, node.id, function() {
										nodeEl.remove();
										blinkGreen(cell);
										Structr.showAndHideInfoBoxMessage('Related node "' + (node.name || node.id) + '" has been removed from property "' + key + '".', 'success', 2000, 1000);
									});
									return false;
								});
							});
						});
					});
				}
			}
		});
	},
	createPropertyTable: function(heading, keys, res, entity, view, tabView, typeInfo, tempNodeCache) {

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

							cell.append('<input class="dateField" name="' + key + '" type="text" value="' + (res[key] || '') + '" data-date-format="' + typeInfo[key].format + '">');

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
														Structr.showAndHideInfoBoxMessage('Related node "' + (node.name || node.id) + '" has been removed from property "' + key + '".', 'success', 2000, 1000);
													} else {
														blinkRed(cell);
													}
												});
												return false;
											});
										});
									});

								} else {
									// will be appended asynchronously
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
						if (key.indexOf('_custom_html_') === -1) {
							blinkGreen(cell);
							Structr.showAndHideInfoBoxMessage('Property "' + key + '" has been set to null.', 'success', 2000, 1000);
						} else {
							nullIcon.closest('tr').remove();
							Structr.showAndHideInfoBoxMessage('Custom HTML property "' + key + '" has been removed', 'success', 2000, 1000);
						}

						if (key === 'name') {
							var entity = StructrModel.objects[id];
							if (!_Entities.isContentElement(entity)) {
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

		$('.props tr td.value input',    dialog).each(function(i, inputEl)    { _Entities.activateInput(inputEl,    id, entity.pageId, typeInfo); });
		$('.props tr td.value textarea', dialog).each(function(i, textareaEl) { _Entities.activateInput(textareaEl, id, entity.pageId, typeInfo); });


		if (view === '_html_') {
			$('input[name="_html_' + focusAttr + '"]', propsTable).focus();

			tabView.append('<button class="show-all">Show all attributes</button>');
			$('.show-all', tabView).on('click', function() {

				propsTable.addClass('show-all');

				$('tr:visible:odd').css({'background-color': '#f6f6f6'});
				$('tr:visible:even').css({'background-color': '#fff'});
				$(this).attr('disabled', 'disabled').addClass('disabled');
			});

			let addCustomAttributeButton = $('<button class="add-custom-attribute">Add custom attribute</button>');
			tabView.append(addCustomAttributeButton);

			Structr.appendInfoTextToElement({
				element: addCustomAttributeButton,
				text: "Any attribute name is allowed but 'data-' attributes are recommended. (data-structr is reserved for internal use!)",
				insertAfter: true,
				css: {
					marginLeft: "3px",
					top: "-5px",
					position: "relative"
				}
			});

			let saveCustomHTMLAttribute = function(row, exitedInput) {

				let keyInput = $('td.key input', row);
				let valInput = $('td.value input', row);

				let key = keyInput.val().trim();
				let val = valInput.val().trim();

				// only run save action if we have a key and we just left the value input
				if (key !== '' && exitedInput[0] === valInput[0]) {

					var regexAllowed = new RegExp("^[a-zA-Z0-9_\-]*$");

					if (key.indexOf('data-structr') === 0) {

						blinkRed(keyInput);
						new MessageBuilder().error('Key can not start with "data-structr" as it is reserved for internal use.').show();

					} else if (!regexAllowed.test(key)) {

						blinkRed(keyInput);
						new MessageBuilder().error('Key contains forbidden characters. Allowed: "a-z", "A-Z", "-" and "_".').show();

					} else {

						var newKey = '_custom_html_' + key;

						Command.setProperty(id, newKey, val, false, function() {
							blinkGreen(exitedInput);
							Structr.showAndHideInfoBoxMessage('New property "' + newKey + '" has been added and saved with value "' + val + '".', 'success', 2000, 1000);

							keyInput.replaceWith(key);
							valInput.attr('name', newKey);

							let nullIcon = $(_Entities.getNullIconForKey(newKey));
							$('td:last', row).append(nullIcon);
							nullIcon.on('click', function() {
								var key = $(this).prop('id').substring(_Entities.null_prefix.length);
								_Entities.setProperty(id, key, null, false, function(newVal) {
									row.remove();
									Structr.showAndHideInfoBoxMessage('Custom HTML property "' + key + '" has been removed', 'success', 2000, 1000);
								});
							});

							// deactivate this function and resume regular save-actions
							_Entities.activateInput(valInput, id, entity.pageId, typeInfo);
						});
					}
				}
			};

			addCustomAttributeButton.on('click', function(e) {
				let newAttributeRow = $('<tr><td class="key"><input type="text" class="newKey" name="key"></td><td class="value"><input type="text" value=""></td><td></td></tr>');
				propsTable.append(newAttributeRow);

				$('input', newAttributeRow).on('focusout', function(e) {
					saveCustomHTMLAttribute(newAttributeRow, $(this));
				});
			});
		}

		$('tr:visible:odd').css({'background-color': '#f6f6f6'});
		$('tr:visible:even').css({'background-color': '#fff'});

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

						if (node.path && node.path.indexOf('/._structr_thumbnails/') === 0) {
							return;
						}

						var displayName = node.title || node.name || node.id;
						box.append('<div title="' + escapeForHtmlAttributes(displayName) + '" " class="_' + node.id + ' node element abbr-ellipsis abbr-120">' + displayName + '</div>');
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
		_Entities.activateDatePicker(dateField, format);
	},
	activateDatePicker: function(input, format) {
		if (!format) {
			format = input.data('dateFormat');
		}

		var dateTimePickerFormat = getDateTimePickerFormat(format);
		input.datetimepicker({
			dateFormat: dateTimePickerFormat.dateFormat,
			timeFormat: dateTimePickerFormat.timeFormat,
			separator: dateTimePickerFormat.separator
		});
	},
	appendRelatedNode: function(cell, node, onDelete) {
		var displayName = _Crud.displayName(node);
		cell.append('<div title="' + escapeForHtmlAttributes(displayName) + '" class="_' + node.id + ' node ' + (node.type ? node.type.toLowerCase() : (node.tag ? node.tag : 'element')) + ' ' + node.id + '_"><span class="abbr-ellipsis abbr-80">' + displayName + '</span><i class="remove ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></div>');
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
	activateInput: function(el, id, pageId, typeInfo, onUpdateCallback) {

		var input = $(el);
		var oldVal = input.val();
		var relId = input.parent().attr('rel_id');
		var objId = relId ? relId : id;
		var key = input.prop('name');

		if (!input.hasClass('readonly') && !input.hasClass('newKey')) {

			input.closest('.array-attr').find('i.remove').off('click').on('click', function(el) {
				let cell = input.closest('.value');
				if (cell.length === 0) {
					cell = input.closest('.__value');
				}
				input.parent().remove();
				_Entities.saveArrayValue(cell, objId, key, oldVal, id, pageId, typeInfo, onUpdateCallback);
			});

			input.off('focus').on('focus', function() {
				input.addClass('active');
			});

			input.off('change').on('change', function() {
				input.data('changed', true);

				if (pageId && pageId === activeTab) {
					//console.log('reloading previews')
					_Pages.reloadPreviews();
				}
			});

			input.off('focusout').on('focusout', function() {
				_Entities.saveValue(input, objId, key, oldVal, id, pageId, typeInfo, onUpdateCallback);

				input.removeClass('active');
				input.parent().children('.icon').each(function(i, icon) {
					$(icon).remove();
				});
			});
		}
	},
	getArrayValue: function(key, cell) {
		let values = [];
		cell.find('[name="' + key + '"]').each(function(i, el) {
			let value = $(el).val();
			if (value && value.length) {
				values.push(value);
			}
		});
		return values;
	},
	saveValue: function(input, objId, key, oldVal, id, pageId, typeInfo, onUpdateCallback) {

		let val;
		let cell = input.closest('.value');
		if (cell.length === 0) {
			cell = input.closest('.__value');
		}

		// Array?
		if (typeInfo[key] && typeInfo[key].isCollection && !typeInfo[key].relatedType) {
			val = _Entities.getArrayValue(key, cell);
		} else {
			val = input.val();
		}

		var isPassword = input.prop('type') === 'password';
		if (input.data('changed')) {
			input.data('changed', false);
			_Entities.setProperty(objId, key, val, false, function(newVal) {
				if (isPassword || (newVal !== oldVal)) {
					blinkGreen(input);
					let valueMsg;
					if (newVal.constructor === Array) {
						cell.html(formatArrayValueField(key, newVal, typeInfo[key].format === 'multi-line', typeInfo[key].readOnly, isPassword));
						cell.find('[name="' + key + '"]').each(function(i, el) {
							_Entities.activateInput(el, id, pageId, typeInfo);
						});
						valueMsg = newVal ? 'value [' + newVal.join(',\n') + ']': 'empty value';
					} else {
						input.val(newVal);
						valueMsg = newVal ? 'value "' + newVal + '"': 'empty value';
					}
					Structr.showAndHideInfoBoxMessage('Updated property "' + key + '"' + (!isPassword ? ' with ' + valueMsg + '' : ''), 'success', 2000, 200);

					if (onUpdateCallback) {
						onUpdateCallback();
					}

				} else {
					input.val(oldVal);
				}
				oldVal = newVal;
			});
		}

	},
	saveArrayValue: function(cell, objId, key, oldVal, id, pageId, typeInfo, onUpdateCallback) {

		var val = _Entities.getArrayValue(key, cell);

		_Entities.setProperty(objId, key, val, false, function(newVal) {
			if (newVal !== oldVal) {
				blinkGreen(cell);
				let valueMsg;
				cell.html(formatArrayValueField(key, newVal, typeInfo[key].format === 'multi-line', typeInfo[key].readOnly, false));
				cell.find('[name="' + key + '"]').each(function(i, el) {
					_Entities.activateInput(el, id, pageId, typeInfo);
				});
				valueMsg = newVal ? 'value [' + newVal.join(',\n') + ']': 'empty value';
				Structr.showAndHideInfoBoxMessage('Updated property "' + key + '" with ' + valueMsg + '.', 'success', 2000, 200);

				if (onUpdateCallback) {
					onUpdateCallback();
				}
			}
			oldVal = newVal;
		});

	},
	setProperty: function(id, key, val, recursive, callback) {
		Command.setProperty(id, key, val, recursive, function() {
			Command.getProperty(id, key, callback);
		});
	},
	appendAccessControlIcon: function(parent, entity) {

		var isProtected = !entity.visibleToPublicUsers || !entity.visibleToAuthenticatedUsers;

		var keyIcon = $('.key_icon', parent);
		if (!(keyIcon && keyIcon.length)) {

			keyIcon = $('<i title="Access Control and Visibility" class="key_icon button ' + (isProtected ? 'donthide ' : '') + _Icons.getFullSpriteClass(_Icons.key_icon) + '" ' + (isProtected ? 'style="display: inline-block;"' : '') + '/>');
			parent.append(keyIcon);

			_Entities.bindAccessControl(keyIcon, entity);
		}
	},
	bindAccessControl: function(btn, entity) {

		btn.on('click', function(e) {
			e.stopPropagation();
			_Entities.showAccessControlDialog(entity);
		});
	},
	accessControlDialog: function(entity, el, typeInfo) {

		var id = entity.id;

		var handleGraphObject = function(entity) {

			let owner_select_id = 'owner_select_' + id;
			el.append('<h3>Owner</h3><div><select id="' + owner_select_id + '"></select></div>');
			el.append('<h3>Visibility</h3>');

			let allowRecursive = (entity.type === 'Template' || entity.isFolder || (Structr.isModuleActive(_Pages) && !(entity.isContent)));

			if (allowRecursive) {
				el.append('<div>Apply visibility switches recursively? <input id="recursive" type="checkbox" name="recursive"></div><br>');
			}

			_Entities.appendBooleanSwitch(el, entity, 'visibleToPublicUsers', ['Visible to public users', 'Not visible to public users'], 'Click to toggle visibility for users not logged-in', '#recursive');
			_Entities.appendBooleanSwitch(el, entity, 'visibleToAuthenticatedUsers', ['Visible to auth. users', 'Not visible to auth. users'], 'Click to toggle visibility to logged-in users', '#recursive');

			el.append('<h3>Access Rights</h3>');
			el.append('<table class="props" id="principals"><thead><tr><th>Name</th><th>Read</th><th>Write</th><th>Delete</th><th>Access Control</th>' + (allowRecursive ? '<th></th>' : '') + '</tr></thead><tbody></tbody></table');

			var tb = $('#principals tbody', el);
			tb.append('<tr id="new"><td><select style="z-index: 999" id="newPrincipal"><option></option></select></td><td></td><td></td><td></td><td></td>' + (allowRecursive ? '<td></td>' : '') + '</tr>');

			$.ajax({
				url: rootUrl + '/' + entity.id + '/in',
				dataType: 'json',
				contentType: 'application/json; charset=utf-8',
				success: function(data) {

					for (let result of data.result) {

						let permissions = {
							read: isIn('read', result.allowed),
							write: isIn('write', result.allowed),
							delete: isIn('delete', result.allowed),
							accessControl: isIn('accessControl', result.allowed)
						};

						let principalId = result.principalId;
						if (principalId) {
							Command.get(principalId, 'id,name,isGroup', function(p) {
								_Entities.addPrincipal(entity, p, permissions, allowRecursive);
							});
						}
					}
				}
			});

			let ownerSelect = $('#' + owner_select_id, el);
			let granteeSelect = $('#newPrincipal', el);
			let spinnerIcon = Structr.loaderIcon(granteeSelect.parent(), {float: 'right'});

			Command.getByType('Principal', null, null, 'name', 'asc', 'id,name,isGroup', false, function(principals) {

				let ownerOptions = '';
				let granteeGroupOptions = '';
				let granteeUserOptions = '';

				if (entity.owner) {
					// owner is first entry
					ownerOptions += '<option value="' + entity.owner.id + '" data-type="User">' + entity.owner.name + '</option>';
				} else {
					ownerOptions += '<option></option>';
				}

				principals.forEach(function(p) {

					if (p.isGroup) {
						granteeGroupOptions += '<option value="' + p.id + '" data-type="Group">' + p.name + '</option>';
					} else {
						granteeUserOptions += '<option value="' + p.id + '" data-type="User">' + p.name + '</option>';

						if (!entity.owner || entity.owner.id !== p.id) {
							ownerOptions += '<option value="' + p.id + '" data-type="User">' + p.name + '</option>';
						}
					}
				});

				ownerSelect.append(ownerOptions);
				granteeSelect.append(granteeGroupOptions + granteeUserOptions);

				let templateOption = (state, isSelection) => {
					if (!state.id || state.disabled) {
						return state.text;
					}

					let icon = (state.element.dataset['type'] === 'Group') ? _Icons.group_icon : _Icons.user_icon ;

					return $('<span class="' + (isSelection ? 'select-selection-with-icon' : 'select-result-with-icon') + '"><i class="typeIcon ' + _Icons.getFullSpriteClass(icon) + '"></i> ' + state.text + '</span>');
				};

				ownerSelect.select2({
					allowClear: true,
					placeholder: 'Owner',
					width: '300px',
					style: 'text-align:left;',
					dropdownParent: $('.blockPage'),
					templateResult: (state) => {
						return templateOption(state, false);
					},
					templateSelection: (state) => {
						return templateOption(state, true);
					}
				}).on('select2:unselecting', function (e) {
					e.preventDefault();

					Command.setProperty(id, 'owner', null, false, function() {
						blinkGreen(ownerSelect.parent());
						ownerSelect.val(null).trigger('change');
					});

				}).on('select2:select', function (e) {

					let data = e.params.data;
					Command.setProperty(id, 'owner', data.id, false, function() {
						blinkGreen(ownerSelect.parent());
					});
				});

				granteeSelect.select2({
					placeholder: 'Select Group/User',
					width: '100%',
					dropdownParent: $('.blockPage'),
					templateResult: (state) => {
						return templateOption(state, false);
					}
				}).on('select2:select', function (e) {

					let data = e.params.data;
					let pId = data.id;
					let rec = $('#recursive', el).is(':checked');
					Command.setPermission(entity.id, pId, 'grant', 'read', rec);

					Command.get(pId, 'id,name,isGroup', function(p) {
						_Entities.addPrincipal(entity, p, {read: true}, allowRecursive);
					});
				});

				if (spinnerIcon.length) {
					spinnerIcon.remove();
				}
			});
		};

		if (entity.targetId) {
			Command.getRelationship(id, entity.targetId, 'id,type,name,isFolder,isContent,owner,visibleToPublicUsers,visibleToAuthenticatedUsers', handleGraphObject);
		} else {
			Command.get(id, 'id,type,name,isFolder,isContent,owner,visibleToPublicUsers,visibleToAuthenticatedUsers', handleGraphObject);
		}
	},
	showAccessControlDialog: function(entity) {

		var id = entity.id;

		var initialObj = {
			ownerId: entity.owner ? entity.owner.id : null,
			visibleToPublicUsers: entity.visibleToPublicUsers,
			visibleToAuthenticatedUsers: entity.visibleToAuthenticatedUsers
		};

		Structr.dialog('Access Control and Visibility', function() {
		}, function() {
			if (Structr.isModuleActive(_Crud)) {

				var handleGraphObject = function(entity) {
					if ((!entity.owner && initialObj.owner !== null) || initialObj.ownerId !== entity.owner.id) {
						_Crud.refreshCell(id, "owner", entity.owner, entity.type, initialObj.ownerId);
					}

					_Crud.refreshCell(id, 'visibleToPublicUsers',        entity.visibleToPublicUsers,        entity.type, initialObj.visibleToPublicUsers);
					_Crud.refreshCell(id, 'visibleToAuthenticatedUsers', entity.visibleToAuthenticatedUsers, entity.type, initialObj.visibleToAuthenticatedUsers);
				};

				if (entity.targetId) {
					Command.getRelationship(id, entity.targetId, 'id,type,owner,visibleToPublicUsers,visibleToAuthenticatedUsers', handleGraphObject);
				} else {
					Command.get(id, 'id,type,owner,visibleToPublicUsers,visibleToAuthenticatedUsers', handleGraphObject);
				}
			} else if (Structr.isModuleActive(_Security)) {
				_ResourceAccessGrants.updateResourcesAccessRow(id, false);
			}
		});

		_Entities.accessControlDialog(entity, dialogText);

	},
	addPrincipal: function (entity, principal, permissions, allowRecursive) {

		$('#newPrincipal option[value="' + principal.id + '"]').remove();
		$('#newPrincipal').trigger('chosen:updated');

		if ($('#principals ._' + principal.id, dialogText).length > 0) {
			return;
		}

		let row = $('<tr class="_' + principal.id + '"><td><i class="typeIcon ' + _Icons.getFullSpriteClass((principal.isGroup ? _Icons.group_icon : _Icons.user_icon)) + '"></i> <span class="name">' + principal.name + '</span></td></tr>');
		$('#new').after(row);

		['read', 'write', 'delete', 'accessControl'].forEach(function(perm) {

			row.append('<td><input class="' + perm + '" type="checkbox" data-permission="' + perm + '"' + (permissions[perm] ? ' checked="checked"' : '') + '"></td>');

			$('.' + perm, row).on('dblclick', function() {
				return false;
			});

			$('.' + perm, row).on('click', function(e) {
				e.preventDefault();

				let checkbox = $(this);
				checkbox.prop('disabled', true);

				if (!$('input:checked', row).length) {

					$('#newPrincipal').append('<option value="' + principal.id + '">' + principal.name + '</option>');
					$('#newPrincipal').trigger('chosen:updated');

					row.remove();
				}
				let recursive = $('#recursive', dialogText).is(':checked');

				Command.setPermission(entity.id, principal.id, permissions[perm] ? 'revoke' : 'grant', perm, recursive, function() {
					permissions[perm] = !permissions[perm];
					checkbox.prop('checked', permissions[perm]);

					checkbox.prop('disabled', false);

					blinkGreen(checkbox.parent());
				});
			});
		});

		if (allowRecursive) {

			row.append('<td><button class="action apply-to-child-nodes">Apply to child nodes</button></td>');

			let button = row[0].querySelector('button.apply-to-child-nodes');

			button.addEventListener('click', (e) => {

				button.setAttribute('disabled', 'disabled');

				let permissions = [].map.call(row[0].querySelectorAll('input:checked'), (i) => {
					return i.dataset.permission;
				}).join(',');

				Command.setPermission(entity.id, principal.id, 'setAllowed', permissions, true, function() {

					button.removeAttribute('disabled');
					blinkGreen(row);
				});
			});
		}
	},
	appendInput: function(el, entity, key, label, desc) {
		if (!el || !entity) {
			return false;
		}
		el.append('<div><h3>' + label + '</h3><p>' + desc + '</p><div class="input-and-button"><input type="text" class="' + key + '_" value="' + (entity[key] ? entity[key] : '') + '"><button class="action save_' + key + '">Save</button></div></div>');
		var btn = $('.save_' + key, el);
		btn.on('click', function() {
			Command.setProperty(entity.id, key, $('.' + key + '_', el).val(), false, function(obj) {
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
	appendEditSourceIcon: function(parent, entity) {

		if (_Entities.pencilEditBlacklist.indexOf(entity.tag) === -1) {

			var editIcon = $('.edit_icon', parent);

			if (!(editIcon && editIcon.length)) {
				parent.append('<i title="Edit source code" class="edit_icon button ' + _Icons.getFullSpriteClass(_Icons.edit_icon) + '" />');
				editIcon = $('.edit_icon', parent);
			}
			editIcon.on('click', function(e) {
				e.stopPropagation();
				_Entities.editSource(entity);
			});
		}
	},
	appendEditPropertiesIcon: function(parent, entity, visible) {

		var editIcon = $('.edit_props_icon', parent);

		if (!(editIcon && editIcon.length)) {
			editIcon = $('<i title="Edit Properties" class="edit_props_icon button ' + _Icons.getFullSpriteClass(_Icons.view_detail_icon) + '" />');
			parent.append(editIcon);
		}
		editIcon.on('click', function(e) {
			e.stopPropagation();
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

		var button = $(el.children('.expand_icon').first());
		if (button && button.length) {
			return;
		}

		if (hasChildren) {

			var typeIcon = $(el.children('.typeIcon').first());
			var icon = expanded ? _Icons.expanded_icon : _Icons.collapsed_icon;

			var displayName = getElementDisplayName(entity);

			typeIcon.removeClass('typeIcon-nochildren').before('<i title="Expand ' + displayName + '" class="expand_icon ' + _Icons.getFullSpriteClass(icon) + '" />');

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
	ensureExpanded: function(element, callback, force = false) {
		if (!element) {
			return;
		}
		var el = $(element);
		var id = Structr.getId(el);

		if (!id) {
			return;
		}

		Structr.addExpandedNode(id);

		if (force === false && _Entities.isExpanded(element)) {
			return;
		} else {
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
	expandRecursively: function(ids) {
		if (!ids || ids.length === 0) {
			return;
		}

		ids.forEach(function(id) {
			var el = Structr.node(id);

			_Entities.ensureExpanded(el, function(childNodes) {
				if (childNodes && childNodes.length) {
					_Entities.expandRecursively(childNodes.map(n => n.id));
				}
			}, true);
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

		var b = el.children('.expand_icon').first();
		var displayName = getElementDisplayName(Structr.entity(id));

		if (_Entities.isExpanded(element)) {

			el.children('.node').remove();

			b.removeClass(_Icons.getSpriteClassOnly(_Icons.expanded_icon))
				.addClass(_Icons.getSpriteClassOnly(_Icons.collapsed_icon))
				.prop('title', 'Expand ' + displayName);

			Structr.removeExpandedNode(id);

		} else {

			if (!expanded) {
				Command.children(id);
			}

			b.removeClass(_Icons.getSpriteClassOnly(_Icons.collapsed_icon))
				.addClass(_Icons.getSpriteClassOnly(_Icons.expanded_icon))
				.prop('title', 'Collapse ' + displayName);

			Structr.addExpandedNode(id);
		}
	},
	makeAttributeEditable: function(parentElement, id, attributeSelector, attributeName, callback) {

		let attributeElement        = parentElement.find(attributeSelector).first();
		let attributeElementTagName = attributeElement.prop('tagName').toLowerCase();
		let oldValue                = $.trim(attributeElement.attr('title'));

		attributeElement.replaceWith('<input type="text" size="' + (oldValue.length + 4) + '" class="new-' + attributeName + '" value="' + oldValue + '">');

		let input = $('input', parentElement);
		input.focus().select();

		let restoreNonEditableTag = function(el, text) {

			let newEl = $('<' + attributeElementTagName + ' title="' + escapeForHtmlAttributes(text) + '" class="' + attributeName + '_ abbr-ellipsis abbr-75pc">' + text + '</' + attributeElementTagName + '>');
			el.replaceWith(newEl);

			parentElement.find(attributeSelector).first().off('click').on('click', function(e) {
				e.stopPropagation();
				_Entities.makeAttributeEditable(parentElement, id, attributeSelector, attributeName);
			});

			return newEl;
		};

		var saveAndUpdate = function (el) {

			let newVal = el.val();

			// first restore old value. only update with new value if save succeeds
			let newEl = restoreNonEditableTag(el, oldValue);

			let successFunction = () => {

				restoreNonEditableTag(newEl, newVal);

				if (callback) {
					callback();
				}
			};

			_Entities.setNewAttributeValue(parentElement, id, attributeName, newVal, successFunction, function () {

				let attributeElement = parentElement.find(attributeSelector).first();
				attributeElement.attr('title', oldValue).text(oldValue);
				blinkRed(parentElement);
			});
		};

		input.on('blur', function() {
			saveAndUpdate($(this));
		});

		input.keydown(function(e) {

			if (e.keyCode === 13) {
				saveAndUpdate($(this));

			} else if (e.keyCode === 27) {
				e.stopPropagation();
				restoreNonEditableTag($(this), oldValue);
			}
		});
	},
	makeNameEditable: function(element, callback) {
		let id = Structr.getId(element);
		_Entities.makeAttributeEditable(element, id, 'b.name_', 'name', callback);
	},
	setNewName: function(element, newName, callback) {
		let id = Structr.getId(element);
		_Entities.setNewAttributeValue(element, id, 'name', newName, callback);
	},
	setNewAttributeValue: function(element, id, attributeName, newValue, callback, failCallback) {

		Command.setProperty(id, attributeName, newValue, false, function(entity, resultSize, errorOccurred) {

			if (!errorOccurred || errorOccurred === false) {

				blinkGreen(element.find('.' + attributeName + '_').first());

				if (Structr.isModuleActive(_Pages)) {

					_Pages.reloadPreviews();

				} else if (Structr.isModuleActive(_Contents)) {

					_Contents.refreshTree();

				} else if (Structr.isModuleActive(_Files) && attributeName === 'name') {

					let a = element.closest('td').prev().children('a').first();
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
					+ '<b title="' + escapeForHtmlAttributes(name) + '" class="abbr-ellipsis abbr-75pc">' + name + '</b>'
					+ '<b class="action">' + (action ? action : '&nbsp;') + '</b>'
					+ '<span class="content_ abbr-ellipsis abbr-75pc">' + (content ? content : '&nbsp;') + '</span>'
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
	},
	isContentElement: function (entity) {
		return (entity.type === 'Template' || entity.type === 'Content');
	},
	setPropertyWithFeedback: function(entity, key, newVal, input) {
		var oldVal = entity[key];
		Command.setProperty(entity.id, key, newVal, false, function(result) {
			var newVal = result[key];

			// update entity so this works multiple times
			entity[key] = newVal;

			if (newVal !== oldVal) {
				blinkGreen(input);
				if (newVal && newVal.constructor === Array) {
					newVal = newVal.join(',');
				}
				input.val(newVal);
				let valueMsg = newVal ? 'value "' + newVal : 'empty value';
				Structr.showAndHideInfoBoxMessage('Updated property "' + key + '" with ' + valueMsg, 'success', 2000, 200);
			} else {
				input.val(oldVal);
			}
		});
	}
};

function formatValueInputField(key, obj, isPassword, isReadOnly, isMultiline) {

	if (!obj) {

		return formatRegularValueField(key, '', isMultiline, isReadOnly, isPassword);

	} else if (obj.constructor === Object) {

		var displayName = _Crud.displayName(obj);
		return '<div title="' + escapeForHtmlAttributes(displayName) + '" id="_' + obj.id + '" class="node ' + (obj.type ? obj.type.toLowerCase() : (obj.tag ? obj.tag : 'element')) + ' ' + obj.id + '_"><span class="abbr-ellipsis abbr-80">' + displayName + '</span><i class="remove ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '" /></div>';

	} else if (obj.constructor === Array) {

		return formatArrayValueField(key, obj, isMultiline, isReadOnly, isPassword);

	} else {

		return formatRegularValueField(key, escapeForHtmlAttributes(obj), isMultiline, isReadOnly, isPassword);
	}
};

function formatArrayValueField(key, values, isMultiline, isReadOnly, isPassword) {

	let html = '';

	values.forEach(function(value) {

		if (isMultiline) {

			html += '<div class="array-attr"><textarea rows="4" name="' + key + '"' + (isReadOnly ? ' readonly class="readonly"' : '') + '>' + value + '</textarea> <i class="remove ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '"></i></div>';

		} else {

			html += '<div class="array-attr"><input name="' + key + '" type="' + (isPassword ? 'password" autocomplete="new-password' : 'text') + '" value="' + value + '"' + (isReadOnly ? 'readonly class="readonly"' : '') + '> <i class="remove ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '"></i></div>';
		}
	});

	if (isMultiline) {

		html += '<div class="array-attr"><textarea rows="4" name="' + key + '"' + (isReadOnly ? ' readonly class="readonly"' : '') + '></textarea></div>';

	} else {

		html += '<div class="array-attr"><input name="' + key + '" type="' + (isPassword ? 'password" autocomplete="new-password' : 'text') + '" value=""' + (isReadOnly ? 'readonly class="readonly"' : '') + '></div>';
	}

	return html;

};

function formatRegularValueField(key, value, isMultiline, isReadOnly, isPassword) {

	if (isMultiline) {

		return '<textarea rows="4" name="' + key + '"' + (isReadOnly ? ' readonly class="readonly"' : '') + '>' + value + '</textarea>';

	} else {

		return '<input name="' + key + '" type="' + (isPassword ? 'password" autocomplete="new-password' : 'text') + '" value="' + value + '"' + (isReadOnly ? 'readonly class="readonly"' : '') + '>';
	}
};
