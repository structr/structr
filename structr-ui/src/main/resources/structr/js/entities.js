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
let _Entities = {
	selectedObject: {},
	activeElements: {},
	activeQueryTabPrefix: 'structrActiveQueryTab_' + location.port,
	activeEditTabPrefix: 'structrActiveEditTab_' + location.port,
	selectedObjectIdKey: 'structrSelectedObjectId_' + location.port,
	numberAttrs: ['position', 'size'],
	readOnlyAttrs: ['lastModifiedDate', 'createdDate', 'createdBy', 'id', 'checksum', 'size', 'version', 'relativeFilePath'],
	pencilEditBlacklist: ['html', 'body', 'head', 'title', 'script',  'input', 'label', 'button', 'textarea', 'link', 'meta', 'noscript', 'tbody', 'thead', 'tr', 'td', 'caption', 'colgroup', 'tfoot', 'col', 'style'],
	null_prefix: 'null_attr_',
	collectionPropertiesResultCount: {},
	changeBooleanAttribute: function(attrElement, value, activeLabel, inactiveLabel) {

		if (value === true) {
			attrElement.removeClass('inactive').addClass('active').prop('checked', true).html(_Icons.getSvgIcon('checkmark_bold', 12, 12, 'icon-green mr-2') + (activeLabel ? ' ' + activeLabel : ''));
		} else {
			attrElement.removeClass('active').addClass('inactive').prop('checked', false).text((inactiveLabel ? inactiveLabel : '-'));
		}

	},
	reloadChildren: function(id) {
		let el = Structr.node(id);

		$(el).children('.node').remove();
		_Entities.resetMouseOverState(el);

		Command.children(id);
	},
	deleteNodes: function(button, entities, recursive, callback) {

		if ( !Structr.isButtonDisabled(button) ) {

			let confirmationHtml = '<p>Delete the following objects' + (recursive ? ' (all folders recursively) ' : '') + '?</p>';

			let nodeIds = [];

			for (let entity of entities) {

				confirmationHtml += '<strong>' + entity.name + '</strong> [' + entity.id + ']<br>';
				nodeIds.push(entity.id);
			}

			confirmationHtml += '<br>';

			Structr.confirmation(confirmationHtml,() => {

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
	deleteNode: (button, entity, recursive, callback) => {

		if ( !Structr.isButtonDisabled(button) ) {
			Structr.confirmation('<p>Delete ' + entity.type + ' <strong>' + (entity.name || '') + '</strong> [' + entity.id + ']' + (recursive ? ' recursively' : '') + '?</p>',
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
	deleteEdge: (button, entity, recursive, callback) => {

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
	// showSyncDialog: function(source, target) {
	// 	Structr.dialog('Sync between ' + source.id + ' and ' + target.id, function() {
	// 		return true;
	// 	}, function() {
	// 		return true;
	// 	});
	//
	// 	dialog.append('<div><input type="radio" name="syncMode" value="none"><label for="unidir">None</label></div>');
	// 	dialog.append('<div><input type="radio" name="syncMode" value="unidir"><label for="unidir">Uni-directional (primary/secondary)</label></div>');
	// 	dialog.append('<div><input type="radio" name="syncMode" value="bidir"><label for="unidir">Bi-directional</label></div>');
	//
	// 	$('input[name=syncMode]:radio', dialog).on('change', function() {
	// 		Command.setSyncMode(source.id, target.id, $(this).val());
	// 	});
	//
	// },
	dataBindingDialog: function(entity, el, typeInfo) {

		Structr.fetchHtmlTemplate('pages/event-action-mapping', { entity: entity }, function (html) {

			el.empty();
			el.append(html);

			let width = '100%';
			let style = 'text-align: left;';
			let parent = $('.blockPage');

			if (parent.length === 0) {
			    parent = $(document.body);
			}

			let eventMappingSelect  =  $('select#event-mapping-select', el);
			let targetTypeSelect    =  $('select#target-type-select', el);
			let deleteTargetInput   =  $('#delete-target-input', el);
			let methodNameInput     =  $('#method-name-input', el);
			let methodTargetInput   =  $('#method-target-input', el);
			let updateTargetInput   =  $('#update-target-input', el);
			let updatePropertyInput =  $('#update-property-input', el);
			let reloadOptionSelect  =  $('select#reload-option-select', el);
			let reloadSelectorInput =  $('#reload-selector-input', el);
			let reloadUrlInput      =  $('#reload-url-input', el);
			let reloadEventInput    =  $('#reload-event-input', el);
			let customEventInput    =  $('#custom-event-input', el);
			let customActionInput   =  $('#custom-action-input', el);
			let customTargetInput   =  $('#custom-target-input', el);
			let paginationNameInput =  $('#pagination-name-input', el);

			// event mapping selector
			eventMappingSelect.select2({
				placeholder: 'Event',
				style: style,
				width: width,
				dropdownParent: parent,
			}).on('select2:select', function(e) {
				let data = e.params.data;
				$('div.event-options', el).addClass('hidden');
				if (this.value && this.value.length > 0) {
					$('.' + this.value).removeClass('hidden');
				}
				if (data.id !== 'options-none') {
					$('.options-reload-target').removeClass('hidden');
				}

				// name comes from _html_name, always fill this field
				Command.getProperty(entity.id, '_html_name', function(result) {
					updatePropertyInput.val(result);
				});
			});

			// target type selector
			targetTypeSelect.select2({
				allowClear: true,
				placeholder: 'Select type..',
				style: style,
				width: width,
				dropdownParent: parent,
				ajax: {
					url: '/structr/rest/SchemaNode',
					processResults: function (data) {
						return {
							results: data.result.map(n => ({ id: n.name, text: n.name }))
						};
					},
					data: function (params) {

						let config = {
							name: params.term
						};

						config[Structr.getRequestParameterName('sort')] = 'name';
						config[Structr.getRequestParameterName('loose')] = 1;

						return config;
					}
				}
			}).on('select2:select', function(e) {
				$('.select2-selection', targetTypeSelect.parent()).removeClass('required');
			});

			// reload target selector
			reloadOptionSelect.select2({
				placeholder: 'Reload target',
				style: style,
				width: width,
				dropdownParent: parent,
			}).on('select2:select', function(e) {
				$('div.reload-options', el).addClass('hidden');
				if (this.value && this.value.length > 0) {
					$('#' + this.value).removeClass('hidden');
				}
			});

			deleteTargetInput.on('change', function(e) { deleteTargetInput.removeClass('required'); });
			methodTargetInput.on('change', function(e) { methodTargetInput.removeClass('required'); });
			methodNameInput.on('change', function(e) { methodNameInput.removeClass('required'); });
			updateTargetInput.on('change', function(e) { updateTargetInput.removeClass('required'); });
			updatePropertyInput.on('change', function(e) { updatePropertyInput.removeClass('required'); });
			reloadSelectorInput.on('change', function(e) { reloadSelectorInput.removeClass('required'); });
			reloadUrlInput.on('change', function(e) { reloadUrlInput.removeClass('required'); });
			reloadEventInput.on('change', function(e) { reloadEventInput.removeClass('required'); });
			customEventInput.on('change', function(e) { customEventInput.removeClass('required'); });
			customActionInput.on('change', function(e) { customActionInput.removeClass('required'); });
			customTargetInput.on('change', function(e) { customTargetInput.removeClass('required'); });
			paginationNameInput.on('change', function(e) { paginationNameInput.removeClass('required'); });

			Structr.activateCommentsInElement(el);

			// initialize fields from values in the object
			let eventMapping = JSON.parse(entity.eventMapping);
			if (eventMapping) {

				let click        = eventMapping.click;
				let change       = eventMapping.change;
				let id           = 'options-custom';

				if (click) {

					switch (click) {

						case 'create':
							id = 'options-create-click';
							break;

						case 'update':
							id = 'options-update-click';
							break;

						case 'delete':
							id = 'options-delete-click';
							break;

						case 'previous-page':
							id = 'options-prev-page-click';
							break;

						case 'next-page':
							id = 'options-next-page-click';
							break;

						default:
							id = 'options-method-click';
							methodNameInput.val(click);
							break;
					}

				} else if (change) {

					switch (change) {

						case 'update':
							id = 'options-update-change';
							break;

						case 'create':
							id = 'options-create-change';
							break;

						default:
							id = 'options-method-change';
							methodNameInput.val(change);
							break;
					}

				} else {

					for (let key of Object.keys(eventMapping)) {

						customEventInput.val(key);
						customActionInput.val(eventMapping[key]);
					}
				}

				eventMappingSelect.val(id);
				eventMappingSelect.trigger('change');
				eventMappingSelect.trigger({ type: 'select2:select', params: { data: { id: id }}});

				// set selected option in targetTypeSelect
				let selectedType = entity['data-structr-target'];
				if (selectedType) {

					let option = new Option(selectedType, selectedType, true, true);
					targetTypeSelect.append(option).trigger('change');

					targetTypeSelect.trigger({
						type: 'select2:select',
						params: { data: { id: selectedType } }
					});
				}

				deleteTargetInput.val(entity['data-structr-target']);
				methodTargetInput.val(entity['data-structr-target']);
				updateTargetInput.val(entity['data-structr-target']);
				customTargetInput.val(entity['data-structr-target']);
				paginationNameInput.val(entity['data-structr-target']);

				let reloadTargetValue = entity['data-structr-reload-target'];
				if (reloadTargetValue) {

					let reloadOption = 'reload-manual';

					if (reloadTargetValue === 'none') {

						reloadOption = 'reload-none';

					} else if (reloadTargetValue.indexOf('url:') === 0) {

						reloadOption = 'reload-url';
						reloadUrlInput.val(reloadTargetValue.substring(4));

					} else if (reloadTargetValue.indexOf('event:') === 0) {

						reloadOption = 'reload-event';
						reloadEventInput.val(reloadTargetValue.substring(6));

					} else {

						reloadOption = 'reload-selector';
						reloadSelectorInput.val(reloadTargetValue);
					}

					reloadOptionSelect.val(reloadOption);
					reloadOptionSelect.trigger('change');
					reloadOptionSelect.trigger({ type: 'select2:select', params: { data: { id: reloadOption }}});

				} else {

					// reload option default is "page" for empty values
					reloadOptionSelect.val('reload-page');
					reloadOptionSelect.trigger('change');
					reloadOptionSelect.trigger({ type: 'select2:select', params: { data: { id: 'reload-page' }}});
				}
			}

			// name comes from _html_name, always fill this field
			Command.getProperty(entity.id, '_html_name', function(result) {
				updatePropertyInput.val(result);
			});

			let saveButton = $('#save-event-mapping-button');
			if (saveButton) {

				saveButton.on('click', function() {

					// collect values
					let eventType      = eventMappingSelect.val();
					let targetType     = targetTypeSelect.val();
					let methodName     = methodNameInput.val();
					let methodTarget   = methodTargetInput.val();
					let updateTarget   = updateTargetInput.val();
					let updateProperty = updatePropertyInput.val();
					let deleteTarget   = deleteTargetInput.val();
					let reloadOption   = reloadOptionSelect.val();
					let customEvent    = customEventInput.val();
					let customAction   = customActionInput.val();
					let customTarget   = customTargetInput.val();
					let paginationName = paginationNameInput.val();
					let reloadTarget   = null;
					let inputEl        = $(eventType);

					// build reload target according to reloadOption
					switch (reloadOption) {
						case 'reload-none':
							reloadTarget = 'none';
							break;
						case 'reload-page':
							// this is the default, so nothing to do
							break;
						case 'reload-selector':
							reloadTarget = reloadSelectorInput.val();
							break;
						case 'reload-url':
							reloadTarget = 'url:' + reloadUrlInput.val();
							break;
						case 'reload-event':
							reloadTarget = 'event:' + reloadEventInput.val();
							break;
					}

					switch (eventType) {

						case 'options-none':
							_Entities.setPropertyWithFeedback(entity, 'data-structr-reload-target', null, $(inputEl), null);
							_Entities.setPropertyWithFeedback(entity, 'data-structr-target',        null, $(inputEl), null);
							_Entities.setPropertyWithFeedback(entity, 'eventMapping',               null, $(inputEl), null);
							break;

						case 'options-custom':
							if (customEvent && customAction && customTarget) {
								let customMapping = {};
								customMapping[customEvent] = customAction;
								_Entities.setPropertyWithFeedback(entity, 'eventMapping', JSON.stringify(customMapping), $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-target',  customTarget, $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-reload-target',  reloadTarget, $(inputEl), null);
							} else {
								Structr.showAndHideInfoBoxMessage('Please enter event and action.', 'warning', 2000, 200);
								customEventInput.addClass('required');
								customActionInput.addClass('required');
								customTargetInput.addClass('required');
							}
							break;

						case 'options-create-click':
							if (targetType) {
								_Entities.setPropertyWithFeedback(entity, 'eventMapping', '{ "click": "create" }', $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-target',  targetType, $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-reload-target',  reloadTarget, $(inputEl), null);
							} else {
								Structr.showAndHideInfoBoxMessage('Please select the type of object to create.', 'warning', 2000, 200);
								$('.select2-selection', targetTypeSelect.parent()).addClass('required');
							}
							break;

						case 'options-create-change':
							if (targetType) {
								_Entities.setPropertyWithFeedback(entity, 'eventMapping', '{ "change": "create" }', $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-target',  targetType, $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-reload-target',  reloadTarget, $(inputEl), null);
							} else {
								Structr.showAndHideInfoBoxMessage('Please select the type of object to create.', 'warning', 2000, 200);
								$('.select2-selection', targetTypeSelect.parent()).addClass('required');
							}
							break;

						case 'options-delete-click':
							if (deleteTarget) {
								_Entities.setPropertyWithFeedback(entity, 'eventMapping', '{ "click": "delete" }', $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-target',  deleteTarget, $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-reload-target',  reloadTarget, $(inputEl), null);
							} else {
								Structr.showAndHideInfoBoxMessage('Please provide the UUID of the object to delete.', 'warning', 2000, 200);
								deleteTargetInput.addClass('required');
							}
							break;

						case 'options-next-page-click':
						case 'options-prev-page-click':
							if (paginationName) {
								let paginationAction = 'next-page';
								if (eventType === 'options-prev-page-click') { paginationAction = 'previous-page'; }
								_Entities.setPropertyWithFeedback(entity, 'eventMapping', '{ "click": "' + paginationAction + '" }', $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-target',  paginationName, $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-reload-target',  reloadTarget, $(inputEl), null);
							} else {
								Structr.showAndHideInfoBoxMessage('Please provide the name of the pagination request parameter.', 'warning', 2000, 200);
								paginationNameInput.addClass('required');
							}
							break;

						case 'options-method-click':
							if (methodTarget && methodName) {
								_Entities.setPropertyWithFeedback(entity, 'eventMapping', '{ "click": "' + methodName + '" }', $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-target',  methodTarget, $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-reload-target',  reloadTarget, $(inputEl), null);
							} else {
								if (!methodTarget) {
									Structr.showAndHideInfoBoxMessage('Please provide the UUID of the object to call.', 'warning', 2000, 200);
									methodTargetInput.addClass('required');
								}
								if (!methodName) {
									Structr.showAndHideInfoBoxMessage('Please provide the name of the method to execute.', 'warning', 2000, 200);
									methodNameInput.addClass('required');
								}
							}
							break;

						case 'options-method-change':
							if (methodTarget && methodName) {
								_Entities.setPropertyWithFeedback(entity, 'eventMapping', '{ "change": "' + methodName + '" }', $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-target',  methodTarget, $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-reload-target',  reloadTarget, $(inputEl), null);
							} else {
								if (!methodTarget) {
									Structr.showAndHideInfoBoxMessage('Please provide the UUID of the object to call.', 'warning', 2000, 200);
									methodTargetInput.addClass('required');
								}
								if (!methodName) {
									Structr.showAndHideInfoBoxMessage('Please provide the name of the method to execute.', 'warning', 2000, 200);
									methodNameInput.addClass('required');
								}
							}
							break;

						case 'options-update-change':
							if (updateTarget && updateProperty) {
								_Entities.setPropertyWithFeedback(entity, 'eventMapping', '{ "change": "update" }', $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-target',  updateTarget, $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, '_html_name',  updateProperty, $(inputEl), null);
								_Entities.setPropertyWithFeedback(entity, 'data-structr-reload-target',  reloadTarget, $(inputEl), null);
							} else {
								if (!updateTarget) {

									updateTargetInput.addClass('required');
									Structr.showAndHideInfoBoxMessage('Please provide the UUID of the object to update.', 'warning', 2000, 200);
								}
								if (!updateProperty) {
									updatePropertyInput.addClass('required');
									Structr.showAndHideInfoBoxMessage('Please provide the name of the property to update.', 'warning', 2000, 200);
								}
							}
							break;
					}
				});
			}
		});
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
	// appendRowWithBooleanSwitch: function (entity, el, key, label, text, typeInfo) {
	// 	el.append('<tr><td class="key">' + label + '</td><td class="value"></td><td></td></tr>');
	//
	// 	_Entities.appendBooleanSwitch($('tr:last .value', el), entity, key, '', text);
	//
	// 	_Entities.appendSchemaHint($('.key:last', el), key, typeInfo);
	// },
	appendSchemaHint: function (el, key, typeInfo) {

		if (typeInfo[key] && typeInfo[key].hint) {
			Structr.appendInfoTextToElement({
				element: el,
				text: typeInfo[key].hint,
				class: 'hint'
			});
		}

	},
	// queryDialog: function(entity, el) {
	// 	return _Entities.repeaterConfig(entity, el);
	// },
	repeaterConfig: function(entity, el) {

		let queryTypes = [
			{ title: 'REST Query',     propertyName: 'restQuery' },
			{ title: 'Cypher Query',   propertyName: 'cypherQuery' },
//			{ title: 'XPath Query',    propertyName: 'xpathQuery' },
			{ title: 'Function Query', propertyName: 'functionQuery' }
		];

		if (Structr.isModulePresent('flows')) {
			queryTypes.unshift({ title: 'Flow', propertyName: 'flow' });
		}

		let queryTypeButtonsContainer = $('.query-type-buttons', el);

		let queryTextElement = $('.query-text', el);
		let flowSelector     = $('#flow-selector');

		let repeaterConfigEditor;
		let saveBtn;

		let saveFunction = () => {

			if ($('button.active', queryTypeButtonsContainer).length > 1) {
				return new MessageBuilder().error('Please select only one query type.').show();
			}

			let data = {};

			for (let queryType of queryTypes) {

				let val = null;

				if ($('.' + queryType.propertyName, queryTypeButtonsContainer).hasClass('active')) {

					if (queryType.propertyName === 'flow') {

						val = flowSelector.val();

					} else {

						val = repeaterConfigEditor.getValue();
						data.flow = null;
						flowSelector.val('--- Select Flow ---');
					}
				}

				data[queryType.propertyName] = val;
			};

			Command.setProperties(entity.id, data, function(obj) {

				Object.assign(entity, data);

				if (flowSelector.is(':visible')) {
					blinkGreen(flowSelector);
				} else {
					blinkGreen(saveBtn);
				}
			});
		};

		let activateEditor = (queryType) => {

			let queryText = entity[queryType] || '';

			if (repeaterConfigEditor) {

				repeaterConfigEditor.setValue(queryText);

			} else {

				let customConfig = {
					value: queryText,
					language: 'plain',
					lint: false,
					autocomplete: false,
					// changeFn: (editor, entity) => { },
					isAutoscriptEnv: true,
					saveFn: saveFunction,
					saveFnText: 'Save Repeater Config'
				};

				repeaterConfigEditor = _Editors.getMonacoEditor(entity, 'repeater-config-fake-key', queryTextElement, customConfig);
			}

			let model = repeaterConfigEditor.getModel();

			if (queryType === 'functionQuery') {
				// enable auto completion
				repeaterConfigEditor.customConfig.language = 'auto';
				repeaterConfigEditor.customConfig.autocomplete = true;
				repeaterConfigEditor.customConfig.lint = true;
				model.uri.isAutoscriptEnv = true;
			} else {
				// disable auto completion
				repeaterConfigEditor.customConfig.language = 'text';
				repeaterConfigEditor.customConfig.autocomplete = false;
				repeaterConfigEditor.customConfig.lint = false;
				model.uri.isAutoscriptEnv = false;
			}

			_Editors.updateMonacoEditorLanguage(repeaterConfigEditor, repeaterConfigEditor.customConfig.language);
		};

		let initRepeaterInputs = function() {

			saveBtn = $('button.save', el);

			for (let queryType of queryTypes) {
				$('<button data-query-type="' + queryType.propertyName + '" class="' + queryType.propertyName + '">' + queryType.title + '</button>').appendTo(queryTypeButtonsContainer);
			}

			let allButtons = $('.query-type-buttons button', el);

			allButtons.on('click', function() {

				allButtons.removeClass('active');

				let btn = $(this);
				btn.addClass('active');

				let queryType = btn.data('query-type');

				if (queryType === 'flow') {

					saveBtn.hide();
					queryTextElement.hide();
					flowSelector.show();

				} else {

					saveBtn.show();
					queryTextElement.show();
					flowSelector.hide();
					activateEditor(queryType);
				}
			});

			for (let queryType of queryTypes) {

				if (queryType.propertyName === 'flow' && entity[queryType.propertyName]) {
					$('button.' + queryType.propertyName).click();
					flowSelector.val(entity[queryType.propertyName]);
				} else if (entity[queryType.propertyName] && entity[queryType.propertyName].trim() !== "") {
					$('button.' + queryType.propertyName).click();
				}
			}

			if ($('button.active', queryTypeButtonsContainer).length === 0) {
				$('.query-type-buttons button:first', el).click();
			}

			flowSelector.on('change', saveFunction);
			saveBtn.on('click', saveFunction);

			_Entities.appendInput(el, entity, 'dataKey', 'Repeater Keyword',
				'The repeater keyword or data key is either a word to reference result objects, or it can be the name of a collection property of the result object.<br><br>' +
				'You can access result objects or the objects of the collection using template expressions, e.g. <i>${project.name}</i>.');
		};

		if (Structr.isModulePresent('flows')) {

			flowSelector.append('<option>--- Select Flow ---</option>');

			Command.getByType('FlowContainer', 1000, 1, 'effectiveName', 'asc', null, false, function(flows) {

				for (let flow of flows) {
					flowSelector.append('<option value="' + flow.id + '">' + flow.effectiveName + '</option>');
				}

				initRepeaterInputs();

				_Editors.resizeVisibleEditors();
			});

		} else {

			if (flowSelector && flowSelector.length) {
				flowSelector.remove();
			}

			initRepeaterInputs();
			_Editors.resizeVisibleEditors();
		}
	},
	editEmptyDiv: function(entity) {

		Structr.dialog('Edit source of "' + (entity.name ? entity.name : entity.id) + '"', function () {}, function () {}, ['popup-dialog-with-editor']);
		dialog.append('<div class="editor h-full"></div>');
		dialogBtn.append('<button id="saveFile" disabled="disabled" class="disabled"> Save </button>');
		dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');

		dialogMeta.html('<span class="editor-info"></span>');

		let editorInfo       = dialogMeta[0].querySelector('.editor-info');
		_Editors.appendEditorOptionsElement(editorInfo);

		let dialogSaveButton = dialogBtn[0].querySelector('#saveFile');
		let saveAndClose     = dialogBtn[0].querySelector('#saveAndClose');

		let initialText = '';

		let emptyDivMonacoConfig = {
			value: initialText,
			language: 'html',
			lint: true,
			autocomplete: true,
			preventRestoreModel: true,
			forceAllowAutoComplete: true,
			changeFn: (editor, entity) => {

				let editorText = editor.getValue();

				if (initialText === editorText) {
					dialogSaveButton.disabled = true;
					dialogSaveButton.classList.add('disabled');
					saveAndClose.disabled = true;
					saveAndClose.classList.add('disabled');
				} else {
					dialogSaveButton.disabled = null;
					dialogSaveButton.classList.remove('disabled');
					saveAndClose.disabled = null;
					saveAndClose.classList.remove('disabled');
				}
			},
			saveFn: (editor, entity) => {

				let text1 = initialText || '';
				let text2 = editor.getValue();

				if (text1 === text2) {
					return;
				}

				Command.patch(entity.id, text1, text2, function () {

					Structr.showAndHideInfoBoxMessage('Content saved.', 'success', 2000, 200);
					saveButton.disabled = true;
					saveButton.classList.add('disabled')

					Command.getProperty(entity.id, 'content', function (newText) {
						initialText = newText;
					});
				});
				Command.saveNode(`<div data-structr-hash="${entity.id}">${editor.getValue()}</div>`, entity.id, function() {
					Structr.showAndHideInfoBoxMessage('Node source saved and DOM tree rebuilt.', 'success', 2000, 200);

					if (_Entities.isExpanded(Structr.node(entity.id))) {
						$('.expand_icon_svg', Structr.node(entity.id)).click().click();
					}

					dialogCancelButton.click();
				});
			}
		};

		let editor = _Editors.getMonacoEditor(entity, 'source', $('.editor', dialog), emptyDivMonacoConfig);

		Structr.resize();

		saveAndClose.addEventListener('click', (e) => {
			e.stopPropagation();

			emptyDivMonacoConfig.saveFn(editor, entity);

			setTimeout(function() {
				dialogSaveButton.remove();
				saveAndClose.remove();
				dialogCancelButton.click();
			}, 500);
		});

		Structr.resize();

		_Editors.resizeVisibleEditors();
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
	showProperties: function(obj, activeViewOverride, parent) {

		let handleGraphObject;

		_Entities.getSchemaProperties(obj.type, 'custom', function(properties) {

			handleGraphObject = function(entity) {

				let views      = ['ui'];
				let activeView = 'ui';
				let tabTexts   = [];

				if (Object.keys(properties).length) {
					views.push('custom');
				}

				if (activeViewOverride) {
					activeView = activeViewOverride;
				}

				_Schema.getTypeInfo(entity.type, function(typeInfo) {

					var dialogTitle;

					if (entity.hasOwnProperty('relType')) {

						views = ['all'];

						tabTexts.all = 'Relationship Properties';
						tabTexts.sourceNode = 'Source Node Properties';
						tabTexts.targetNode = 'Target Node Properties';

						dialogTitle = 'Edit properties of ' + (entity.type ? entity.type : '') + ' relationship ' + (entity.name ? entity.name : entity.id);

					} else {

						if (entity.isDOMNode && !entity.isContent) {
							views.unshift('_html_');
							if (Structr.isModuleActive(_Pages)) {
								activeView = 'general';
							}
						}

						tabTexts._html_ = 'HTML Attributes';
						tabTexts.ui = 'System Properties';
						tabTexts.custom = 'Custom Properties';

						dialogTitle = 'Edit properties of ' + (entity.type ? entity.type : '') + ' node ' + (entity.name ? entity.name : entity.id);
					}


					let tabsdiv, mainTabs, contentEl;
					if (!parent) {
						Structr.dialog(dialogTitle, function() { return true; }, function() { return true; });

						tabsdiv   = dialogHead.append('<div id="tabs"></div>');
						mainTabs  = tabsdiv.append('<ul></ul>');
						contentEl = dialog.append('<div></div>');

					} else {
						contentEl = parent;
					}

					// custom dialog tab?
					let hasCustomDialog = _Dialogs.findAndAppendCustomTypeDialog(entity, mainTabs, contentEl);

					_Entities.appendViews(entity, views, tabTexts, mainTabs, contentEl, typeInfo);

					if (!entity.hasOwnProperty('relType')) {
						_Entities.appendPropTab(entity, mainTabs, contentEl, 'permissions', 'Security', false, function(c) {
							_Entities.accessControlDialog(entity, c, typeInfo);
						});
					}

					activeView = activeViewOverride || LSWrapper.getItem(_Entities.activeEditTabPrefix  + '_' + entity.id) || activeView;
					$('#tab-' + activeView).click();

					Structr.resize();
				});
			};

			if (obj.relType) {
				Command.getRelationship(obj.id, obj.target, null, function(entity) { handleGraphObject(entity); }, 'ui');
			} else {
				Command.get(obj.id, null, function(entity) { handleGraphObject(entity); }, 'ui');
			}
		});
	},
	appendPropTab: function(entity, tabsEl, contentEl, name, label, active, callback, initCallback, showCallback) {

		let ul = tabsEl.children('ul');
		ul.append(`<li id="tab-${name}">${label}</li>`);

		let tab = $('#tab-' + name + '');
		if (active) {
			tab.addClass('active');
		}

		tab.on('click', function(e) {
			e.stopPropagation();
			$('.propTabContent').hide();
			$('li', ul).removeClass('active');
			$('#tabView-' + name).show();
			tab.addClass('active');
			LSWrapper.setItem(_Entities.activeEditTabPrefix  + '_' + entity.id, name);

			if (typeof initCallback === "function") {
				initCallback();
			}

			if (typeof showCallback === "function") {

				// update entity for show callback
				if (entity.relType) {
					Command.getRelationship(entity.id, entity.target, null, function(e) {
						showCallback($('#tabView-' + name), e);
					});
				} else {
					Command.get(entity.id, null, function(e) {
						showCallback($('#tabView-' + name), e);
					});
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
				url: rootUrl + entity.type + '/' + entity.id + '/all?' + Structr.getRequestParameterName('edit') + '=2',
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
			url: rootUrl + entity.type + '/' + entity.id + '/' + key + '?' + Structr.getRequestParameterName('pageSize') + '=' + pageSize + '&' + Structr.getRequestParameterName('page') + '=' + page,
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
	createPropertyTable: function(heading, keys, res, entity, view, container, typeInfo, tempNodeCache) {

		if (heading) {
			container.append('<h2>' + heading + '</h2>');
		}
		container.append('<table class="props ' + view + ' ' + res['id'] + '_"></table>');
		var propsTable = $('table:last', container);
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
							if ((!isReadOnly || StructrWS.isAdmin) && !isSystem) {
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
									//_Entities.showProperties(entity);
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

			var nullIcon = $('#' + _Entities.null_prefix + key, container);
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

		$('.props tr td.value input',    container).each(function(i, inputEl)    { _Entities.activateInput(inputEl,    id, entity.pageId, typeInfo); });
		$('.props tr td.value textarea', container).each(function(i, textareaEl) { _Entities.activateInput(textareaEl, id, entity.pageId, typeInfo); });


		if (view === '_html_') {
			$('input[name="_html_' + focusAttr + '"]', propsTable).focus();

			container.append('<button class="show-all">Show all attributes</button>');
			$('.show-all', container).on('click', function() {

				propsTable.addClass('show-all');

				$('tr:visible:odd').css({'background-color': '#f6f6f6'});
				$('tr:visible:even').css({'background-color': '#fff'});
				$(this).attr('disabled', 'disabled').addClass('disabled');
			});

			let addCustomAttributeButton = $('<button class="add-custom-attribute">Add custom property</button>');
			container.append(addCustomAttributeButton);

			Structr.appendInfoTextToElement({
				element: addCustomAttributeButton,
				text: "Any property name is allowed but the 'data-' prefix is recommended. Please note that 'data-structr-' is reserved for internal use.",
				insertAfter: true,
				css: {
					marginLeft: ".25rem",
					top: "-.5rem",
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

					if (key.indexOf('data-structr-') === 0) {

						blinkRed(keyInput);
						new MessageBuilder().error('Key can not begin with "data-structr-" as it is reserved for internal use.').show();

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

		$('tr:visible:odd', container).css({'background-color': '#f6f6f6'});
		$('tr:visible:even', container).css({'background-color': '#fff'});
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
						valueMsg = (newVal !== undefined || newValue !== null) ? 'value [' + newVal.join(',\n') + ']': 'empty value';
					} else {
						input.val(newVal);
						valueMsg = (newVal !== undefined || newValue !== null) ? 'value "' + newVal + '"': 'empty value';
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
				valueMsg = (newVal !== undefined || newValue !== null) ? 'value [' + newVal.join(',\n') + ']': 'empty value';
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

		let isProtected = !entity.visibleToPublicUsers || !entity.visibleToAuthenticatedUsers;

		let keyIcon = $('.key_icon', parent);
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

		let id = entity.id;

		let handleGraphObject = function(entity) {

			let owner_select_id = 'owner_select_' + id;
			el.append('<h3>Owner</h3><div><select id="' + owner_select_id + '"></select></div>');
			el.append('<h3>Visibility</h3>');

			let allowRecursive = (entity.type === 'Template' || entity.isFolder || (Structr.isModuleActive(_Pages) && !(entity.isContent)));

			if (allowRecursive) {
				el.append('<div><input id="recursive" type="checkbox" name="recursive"><label for="recursive">Apply visibility switches recursively</label></div><br>');
			}

			let securityContainer = el.append('<div class="security-container"></div>');

			_Entities.appendBooleanSwitch(securityContainer, entity, 'visibleToPublicUsers', ['Visible to public users', 'Not visible to public users'], 'Click to toggle visibility for users not logged-in', '#recursive');
			_Entities.appendBooleanSwitch(securityContainer, entity, 'visibleToAuthenticatedUsers', ['Visible to auth. users', 'Not visible to auth. users'], 'Click to toggle visibility to logged-in users', '#recursive');

			el.append('<h3>Access Rights</h3>');
			el.append('<table class="props" id="principals"><thead><tr><th>Name</th><th>Read</th><th>Write</th><th>Delete</th><th>Access Control</th>' + (allowRecursive ? '<th></th>' : '') + '</tr></thead><tbody></tbody></table');

			var tb = $('#principals tbody', el);
			tb.append('<tr id="new"><td><select style="z-index: 999" id="newPrincipal"><option></option></select></td><td></td><td></td><td></td><td></td>' + (allowRecursive ? '<td></td>' : '') + '</tr>');

			$.ajax({
				url: rootUrl + entity.id + '/in',
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
								_Entities.addPrincipal(entity, p, permissions, allowRecursive, el);
							});
						}
					}
				}
			});

			let ownerSelect   = $('#' + owner_select_id, el);
			let granteeSelect = $('#newPrincipal', el);
			let spinnerIcon   = Structr.loaderIcon(granteeSelect.parent(), {float: 'right'});

			Command.getByType('Principal', null, null, 'name', 'asc', 'id,name,isGroup', false, function(principals) {

				let ownerOptions        = '';
				let granteeGroupOptions = '';
				let granteeUserOptions  = '';

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
					dropdownParent: el,
					templateResult: (state) => {
						return templateOption(state, false);
					},
					templateSelection: (state) => {
						return templateOption(state, true);
					}
				}).on('select2:unselecting', function(e) {
					e.preventDefault();

					Command.setProperty(id, 'owner', null, false, function() {
						blinkGreen(ownerSelect.parent());
						ownerSelect.val(null).trigger('change');
					});

				}).on('select2:select', function(e) {

					let data = e.params.data;
					Command.setProperty(id, 'owner', data.id, false, function() {
						blinkGreen(ownerSelect.parent());
					});
				});

				granteeSelect.select2({
					placeholder: 'Select Group/User',
					width: '100%',
					dropdownParent: el,
					templateResult: (state) => {
						return templateOption(state, false);
					}
				}).on('select2:select', function(e) {

					let data = e.params.data;
					let pId = data.id;
					let rec = $('#recursive', el).is(':checked');
					Command.setPermission(entity.id, pId, 'grant', 'read', rec);

					Command.get(pId, 'id,name,isGroup', function(p) {
						_Entities.addPrincipal(entity, p, {read: true}, allowRecursive, el);
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

		let id = entity.id;

		let initialObj = {
			ownerId: entity.owner ? entity.owner.id : null,
			visibleToPublicUsers: entity.visibleToPublicUsers,
			visibleToAuthenticatedUsers: entity.visibleToAuthenticatedUsers
		};

		Structr.dialog('Access Control and Visibility', function() {
		}, function() {
			if (Structr.isModuleActive(_Crud)) {

				let handleGraphObject = function(entity) {
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
	addPrincipal: function (entity, principal, permissions, allowRecursive, container) {

		$('#newPrincipal option[value="' + principal.id + '"]', container).remove();
		$('#newPrincipal', container).trigger('chosen:updated');

		if ($('#principals ._' + principal.id, container).length > 0) {
			return;
		}

		let row = $('<tr class="_' + principal.id + '"><td><i class="typeIcon ' + _Icons.getFullSpriteClass((principal.isGroup ? _Icons.group_icon : _Icons.user_icon)) + '"></i> <span class="name">' + principal.name + '</span></td></tr>');
		$('#new', container).after(row);

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

					$('#newPrincipal', container).append('<option value="' + principal.id + '">' + principal.name + '</option>');
					$('#newPrincipal', container).trigger('chosen:updated');

					row.remove();
				}
				let recursive = $('#recursive', container).is(':checked');

				Command.setPermission(entity.id, principal.id, permissions[perm] ? 'revoke' : 'grant', perm, recursive, function() {

					permissions[perm] = !permissions[perm];
					checkbox.prop('checked', permissions[perm]);

					checkbox.prop('disabled', false);

					blinkGreen(checkbox.parent());
				});
			});
		});

		if (allowRecursive) {

			row.append('<td><button class="apply-to-child-nodes">Apply to child nodes</button></td>');

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
		el.append('<div class="input-section"><h3>' + label + '</h3><p>' + desc + '</p><div class="input-and-button"><input type="text" class="' + key + '_" value="' + (entity[key] ? entity[key] : '') + '"><button class="save_' + key + '">Save</button></div></div>');

		let btn = $('.save_' + key, el);
		let inp = $('.' + key + '_', el);

		btn.on('click', function() {
			let value = $('.' + key + '_', el).val();
			Command.setProperty(entity.id, key, value, false, function(obj) {
				blinkGreen(inp);
				entity[key] = value;
			});
		});
	},
	appendBooleanSwitch: function(el, entity, key, label, desc, recElementId) {
		if (!el || !entity) {
			return false;
		}
		el.append('<div class="' + entity.id + '_"><button class="switch inactive ' + key + '_ inline-flex items-center"></button>' + desc + '</div>');

		let sw = $('.' + key + '_', el);
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
	appendNewAccessControlIcon: (parent, entity, onlyShowWhenProtected = true) => {

		let keyIcon = $('.svg_key_icon', parent);
		if (!(keyIcon && keyIcon.length)) {

			let iconClasses = ['svg_key_icon', 'icon-grey', 'cursor-pointer'];

			if (onlyShowWhenProtected) {
				if (entity.visibleToPublicUsers && entity.visibleToAuthenticatedUsers) {
					iconClasses.push('node-action-icon');
				}
			}

			keyIcon = $(_Icons.getSvgIcon(_Entities.getVisibilityIconId(entity), 16, 16, iconClasses));
			parent.append(keyIcon);

			_Entities.bindAccessControl(keyIcon, entity);
		}

		keyIcon[0].dataset['onlyShowWhenProtected'] = onlyShowWhenProtected;

		return keyIcon;
	},
	getVisibilityIconId: (entity) => {

		let iconId = 'visibility-lock-key';

		if (true === entity.visibleToPublicUsers &&  true === entity.visibleToAuthenticatedUsers) {

			iconId = 'visibility-lock-open';

		} else if (false === entity.visibleToPublicUsers &&  false === entity.visibleToAuthenticatedUsers) {

			iconId = 'visibility-lock-locked';
		}

		return iconId;
	},
	appendContextMenuIcon: (parent, entity, visible) => {

		let contextMenuIcon = $('.node-action-icon', parent);

		if (!(contextMenuIcon && contextMenuIcon.length)) {
			contextMenuIcon = $(_Icons.getSvgIcon('kebab_icon', 16, 16, 'node-action-icon'));
			parent.append(contextMenuIcon);
		}

		contextMenuIcon.on('click', function(e) {
			e.stopPropagation();
			_Elements.activateContextMenu(e, parent, entity);
		});

		if (visible) {
			contextMenuIcon.css({
				visibility: 'visible',
				display: 'inline-block'
			});
		}

		return contextMenuIcon;
	},
	appendExpandIcon: function(el, entity, hasChildren, expanded, callback) {

		let button = $(el.children('.expand_icon_svg').first());
		if (button && button.length) {
			return;
		}

		if (hasChildren) {

			let typeIcon    = $(el.children('.typeIcon').first());
			let icon        = expanded ? _Icons.expandedClass : _Icons.collapsedClass;
			let displayName = getElementDisplayName(entity);

			typeIcon.removeClass('typeIcon-nochildren').before(`<i title="Expand ${displayName}" class="expand_icon_svg ${icon}"></i>`);

			button = $(el.children('.expand_icon_svg').first());

			if (button) {

				button.on('click', function(e) {
					e.stopPropagation();
					_Entities.toggleElement($(this).parent('.node'), undefined, callback);
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

		_Pages.registerDetailClickHandler($(el), entity);
	},
	removeExpandIcon: function(el) {
		if (!el)
			return;

		let button = $(el.children('.expand_icon_svg').first());

		// unregister click handlers
		$(button).off('click');

		button.remove();
		el.children('.typeIcon').addClass('typeIcon-nochildren');
	},
	makeSelectable: function(el) {
		let node = $(el).closest('.node');
		if (!node || !node.children) {
			return;
		}
		node.on('click', function(e) {
			if (e.originalEvent.detail === 1) {
				$(this).toggleClass('selected');
			}
		});
	},
	setMouseOver: function(el, allowClick, syncedNodesIds) {

		let node = $(el).closest('.node');
		if (!node || !node.children) {
			return;
		}

		if (!allowClick) {
			node.on('click', function(e) {
				e.stopPropagation();
				return false;
			});
		}

		// node.children('b.name_').off('click').on('click', function(e) {
		// 	e.stopPropagation();
		// 	_Entities.makeNameEditable(node);
		// });

		let nodeId = Structr.getId(el);
		let isComponent = false;

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

				let page = $(el).closest('.page');
				if (page.length) {
					try {
						$(`.previewBox[data-id="${Structr.getId(page)}"] iframe`).contents().find('[data-structr-id=' + nodeId + ']').addClass('nodeHover');
					} catch (e) {}
				}
				self.addClass('nodeHover');
				self.children('i.button').css('display', 'inline-block');
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
	resetMouseOverState: (element) => {
		let el = $(element);
		let node = el.closest('.node');
		if (node) {
			node.removeClass('nodeHover');
			node.find('i.button').not('.donthide').hide().css('display', 'none');
		}
		let page = node.closest('.page');
		if (page.length) {
			try {
				$(`.previewBox[data-id="${Structr.getId(page)}"] iframe`).contents().find('[data-structr-id]').removeClass('nodeHover');
			} catch (e) {}
		}
	},
	isExpanded: function(element) {
		let b = $(element).children('.expand_icon_svg').first();
		if (!b) {
			return false;
		}
		return b.hasClass(_Icons.expandedClass);
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
			let displayName = getElementDisplayName(StructrModel.obj(id));

			el.children('.expand_icon_svg').first()
				.removeClass(_Icons.collapsedClass)
				.addClass(_Icons.expandedClass)
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
	deselectAllElements: function() {
		$('.nodeSelected').removeClass('nodeSelected');
	},
	scrollTimer: undefined,
	highlightElement:function(el) {

		if (el) {
			el.addClass('nodeSelected');

			// inner debounced function
			let scrollFn = () => {
				let elOffsetTop = el.offset().top;
				let elHeight    = el.height();
				let pagesScrollTop = _Pages.pagesTree.scrollTop();
				let pagesOffsetTop = _Pages.pagesTree.offset().top

				let topPositionOfElementInTree    = elOffsetTop - pagesOffsetTop;
				let bottomPositionOfElementInTree = elOffsetTop + elHeight - pagesOffsetTop;

				if (topPositionOfElementInTree < 0) {
					// element is *above* the currently visible portion of the pages tree

					_Pages.pagesTree.animate({
						scrollTop: elOffsetTop - pagesOffsetTop + pagesScrollTop
					});

				} else if (bottomPositionOfElementInTree > _Pages.pagesTree.height()) {
					// element is *below* the currently visible portion of the pages tree

					_Pages.pagesTree.animate({
						scrollTop: elOffsetTop + elHeight + pagesScrollTop - _Pages.pagesTree.prop('clientHeight')
					});
				}
			};

			if (_Entities.scrollTimer) {
				window.clearTimeout(_Entities.scrollTimer);
			}

			_Entities.scrollTimer = window.setTimeout(scrollFn, 100);
		}
	},
	highlightSelectedElementOnSlideoutOpen: () => {

		// on slideout open the elements are not (always) refreshed --> highlight the currently active element (if there is one)
		let selectedElementId = LSWrapper.getItem(_Entities.selectedObjectIdKey);
		let element = document.querySelector('#id_' + selectedElementId);

		element?.querySelector('.node-selector')?.classList.add('active');
	},
	selectElement: function(element, entity) {

		for (let activeSelector of document.querySelectorAll('.node-selector.active')) {
			activeSelector.classList.remove('active');
		}
		element.querySelector('.node-selector').classList.add('active');

		_Entities.selectedObject = entity;
		LSWrapper.setItem(_Entities.selectedObjectIdKey, entity.id);
	},
	toggleElement: function(element, expanded, callback) {

		let el          = $(element);
		let id          = Structr.getId(el) || Structr.getComponentId(el) || Structr.getGroupId(el);
		let b           = el.children('.expand_icon_svg').first();
		let displayName = getElementDisplayName(StructrModel.obj(id));

		if (_Entities.isExpanded(element)) {

			el.children('.node').remove();

			b.removeClass(_Icons.expandedClass)
				.addClass(_Icons.collapsedClass)
				.prop('title', 'Expand ' + displayName);

			Structr.removeExpandedNode(id);

		} else {

			if (!expanded) {
				Command.children(id, callback);
			}

			b.removeClass(_Icons.collapsedClass)
				.addClass(_Icons.expandedClass)
				.prop('title', 'Collapse ' + displayName);

			Structr.addExpandedNode(id);
		}
	},
	makeAttributeEditable: function(parentElement, id, attributeSelector, attributeName, callback) {

		let attributeElement        = parentElement.find(attributeSelector).first();
		let additionalInputClass    = attributeElement.data('inputClass') || '';
		let attributeElementTagName = attributeElement.prop('tagName').toLowerCase();
		let oldValue                = $.trim(attributeElement.attr('title'));

		attributeElement.replaceWith('<input type="text" size="' + (oldValue.length + 4) + '" class="new-' + attributeName + ' ' + additionalInputClass + '" value="' + oldValue + '">');

		let input = $('input', parentElement);
		input.focus().select();

		let restoreNonEditableTag = function(el, text) {

			let dataInputClass = (additionalInputClass !== '') ? ' data-input-class="' + additionalInputClass + '"' : '';

			let newEl = $('<' + attributeElementTagName + ' title="' + escapeForHtmlAttributes(text) + '" class="' + attributeName + '_ abbr-ellipsis abbr-75pc"' + dataInputClass + '>' + text + '</' + attributeElementTagName + '>');
			el.replaceWith(newEl);

			parentElement.find(attributeSelector).first().off('click').on('click', function(e) {
				e.stopPropagation();
				_Entities.makeAttributeEditable(parentElement, id, attributeSelector, attributeName, callback);
			});

			return newEl;
		};

		let saveAndUpdate = function (el) {

			let newVal = el.val();

			// first restore old value. only update with new value if save succeeds
			let newEl = restoreNonEditableTag(el, oldValue);

			let successFunction = () => {

				let finalEl = restoreNonEditableTag(newEl, newVal);

				if (callback) {
					callback(finalEl);
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

//					_Pages.reloadPreviews();
					console.log('reload preview?');

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
	isContentElement: function (entity) {
		return (entity.type === 'Template' || entity.type === 'Content');
	},
	setPropertyWithFeedback: function(entity, key, newVal, input, blinkEl) {
		const oldVal = entity[key];
		Command.setProperty(entity.id, key, newVal, false, function(result) {
			let newVal = result[key];

			// update entity so this works multiple times
			entity[key] = newVal;

			if (key === 'password' || newVal !== oldVal) {
				blinkGreen(input);
				if (blinkEl) {
					blinkGreen(blinkEl);
				}
				if (newVal && newVal.constructor === Array) {
					newVal = newVal.join(',');
				}
				input.val(newVal);
				let valueMsg = (newVal !== undefined || newVal !== null) ? 'value "' + newVal : 'empty value';
				Structr.showAndHideInfoBoxMessage('Updated property "' + key + '" with ' + valueMsg, 'success', 2000, 200);
			} else {
				input.val(oldVal);
			}
		});
	}
}

function formatValueInputField(key, obj, isPassword, isReadOnly, isMultiline) {

	if (obj === undefined || obj === null) {

		return formatRegularValueField(key, '', isMultiline, isReadOnly, isPassword);

	} else if (obj.constructor === Object) {

		let displayName = _Crud.displayName(obj);
		return '<div title="' + escapeForHtmlAttributes(displayName) + '" id="_' + obj.id + '" class="node ' + (obj.type ? obj.type.toLowerCase() : (obj.tag ? obj.tag : 'element')) + ' ' + obj.id + '_"><span class="abbr-ellipsis abbr-80">' + displayName + '</span><i class="remove ' + _Icons.getFullSpriteClass(_Icons.grey_cross_icon) + '"></i></div>';

	} else if (obj.constructor === Array) {

		return formatArrayValueField(key, obj, isMultiline, isReadOnly, isPassword);

	} else {

		return formatRegularValueField(key, escapeForHtmlAttributes(obj), isMultiline, isReadOnly, isPassword);
	}
}

function formatArrayValueField(key, values, isMultiline, isReadOnly, isPassword) {

	let html            = '';
	let readonlyHTML    = (isReadOnly ? ' readonly class="readonly"' : '');
	let inputTypeHTML   = (isPassword ? 'password' : 'text');
	let removeIconClass = _Icons.getFullSpriteClass(_Icons.grey_cross_icon);

	for (let value of values) {

		if (isMultiline) {

			html += '<div class="array-attr"><textarea rows="4" name="' + key + '"' + readonlyHTML + ' autocomplete="new-password">' + value + '</textarea><i title="Remove single value" class="remove ' + removeIconClass + '"></i></div>';

		} else {

			html += '<div class="array-attr"><input name="' + key + '" type="' + inputTypeHTML + '" value="' + value + '"' + readonlyHTML + ' autocomplete="new-password"><i title="Remove single value" class="remove ' + removeIconClass + '"></i></div>';
		}
	}

	if (isMultiline) {

		html += '<div class="array-attr"><textarea rows="4" name="' + key + '"' + readonlyHTML + ' autocomplete="new-password"></textarea></div>';

	} else {

		html += '<div class="array-attr"><input name="' + key + '" type="' + inputTypeHTML + '" value=""' + readonlyHTML + ' autocomplete="new-password"></div>';
	}

	return html;
}

function formatRegularValueField(key, value, isMultiline, isReadOnly, isPassword) {

	if (isMultiline) {

		return '<textarea rows="4" name="' + key + '"' + (isReadOnly ? ' readonly class="readonly"' : '') + ' autocomplete="new-password">' + value + '</textarea>';

	} else {

		return '<input name="' + key + '" type="' + (isPassword ? 'password' : 'text') + '" value="' + value + '"' + (isReadOnly ? 'readonly class="readonly"' : '') + ' autocomplete="new-password">';
	}
}
