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
let _Entities = {
	selectedObject: {},
	activeElements: {},
	activeQueryTabPrefix: 'structrActiveQueryTab_' + location.port,
	activeEditTabPrefix: 'structrActiveEditTab_' + location.port,
	selectedObjectIdKey: 'structrSelectedObjectId_' + location.port,
	numberAttrs: ['position', 'size'],
	readOnlyAttrs: ['lastModifiedDate', 'createdDate', 'createdBy', 'id', 'checksum', 'size', 'version', 'relativeFilePath'],
	pencilEditBlacklist: ['html', 'body', 'head', 'title', ' script',  'input', 'label', 'button', 'textarea', 'link', 'meta', 'noscript', 'tbody', 'thead', 'tr', 'td', 'caption', 'colgroup', 'tfoot', 'col', 'style'],
	null_prefix: 'null_attr_',
	collectionPropertiesResultCount: {},
	changeBooleanAttribute: function(attrElement, value, activeLabel, inactiveLabel) {

		if (value === true) {
			attrElement.removeClass('inactive').addClass('active').prop('checked', true).html(_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 12, 12, 'icon-green mr-2') + (activeLabel ? ' ' + activeLabel : ''));
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
	deleteNodes: (button, entities, recursive, callback) => {

		if ( !Structr.isButtonDisabled(button) ) {

			let confirmationHtml = `<p>Delete the following objects ${recursive ? '(all folders recursively) ' : ''}?</p>`;

			let nodeIds = [];

			for (let entity of entities) {

				confirmationHtml += `<strong>${entity.name}</strong> [${entity.id}]<br>`;
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
			Structr.confirmation(`<p>Delete ${entity.type} <strong>${entity.name || ''}</strong> [${entity.id}] ${recursive ? 'recursively ' : ''}?</p>`,
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

			Structr.confirmation(`<p>Delete Relationship</p><p>(${entity.sourceId})-[${entity.type}]->(${entity.targetId})${recursive ? ' recursively' : ''}?</p>`,
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
	dataBindingDialog: function(entity, el, typeInfo) {

		let eventsHtml = _Entities.templates.events({entity: entity});
		el.empty();
		el.append(eventsHtml);
		Structr.activateCommentsInElement(el[0]);

		let width = '100%';
		let style = 'text-align: left; color: red;';
		let parent = $('.blockPage');

		if (parent.length === 0) {
			parent = $(document.body);
		}

		let eventSelectElement               = document.getElementById('event-select');
		let actionSelectElement              = document.getElementById('action-select');

		let customEventInput                 = document.getElementById('custom-event-input');
		let customActionInput                = document.getElementById('custom-action-input');

		let dataTypeSelect                   = document.getElementById('data-type-select');
		let dataTypeInput                    = document.getElementById('data-type-input');
		let methodNameInput                  = document.getElementById('method-name-input');

		let updateTargetInput                = document.getElementById('update-target-input');
		let deleteTargetInput                = document.getElementById('delete-target-input');
		let methodTargetInput                = document.getElementById('method-target-input');
		let customTargetInput                = document.getElementById('custom-target-input');

		let addParameterMappingButton        = document.querySelector('.add-parameter-mapping-button');
		let addParameterMappingForTypeButton = document.querySelector('.add-parameter-mapping-for-type-button');

		let successBehaviourSelect           = document.getElementById('success-behaviour-select');
		let successPartialRefreshInput       = document.getElementById('success-partial-refresh-input');
		let successNavigateToURLInput        = document.getElementById('success-navigate-to-url-input');
		let successFireEventInput            = document.getElementById('success-fire-event-input');

		let failureBehaviourSelect           = document.getElementById('failure-behaviour-select');
		let failurePartialRefreshInput       = document.getElementById('failure-partial-refresh-input');
		let failureNavigateToURLInput        = document.getElementById('failure-navigate-to-url-input');
		let failureFireEventInput            = document.getElementById('failure-fire-event-input');

		let saveButton                       = document.getElementById('save-event-mapping-button');

		let actionMapping;

		if (entity.triggeredActions && entity.triggeredActions.length) {

			actionMapping = entity.triggeredActions[0];

			Command.get(actionMapping.id, 'event,action,method,idExpression,dataType,parameterMappings,successBehaviour,successPartial,successURL,successEvent,failureBehaviour,failurePartial,failureURL,failureEvent', (result) => {
				//console.log('Using first object for event action mapping:', result);
				updateEventMapping(entity, result);
			});
		}

		Command.getByType('SchemaNode', 1000, 1, 'name', 'asc', 'id,name', false, result => {
			for (const typeObj of result) {
				dataTypeSelect.insertAdjacentHTML('beforeend', '<option>' + typeObj.name + '</option>');
			}
		});


		if (saveButton) {
			saveButton.addEventListener('click', () => {
				saveEventMappingData(entity);
				saveParameterMappings();
			});
		}

		eventSelectElement.addEventListener('change', e => {

			let el = e.target;
			el.classList.remove('required');
			let selectedValue = el.value;

			if (selectedValue === 'custom') {
				document.querySelectorAll('.options-custom-event').forEach(el => {
					el.classList.remove('opacity-0', 'hidden')
				});
			} else if (selectedValue === 'drop') {
				document.querySelectorAll('.event-drop').forEach(el => {
					el.classList.remove('opacity-0', 'hidden')
				});
			} else {
				document.querySelectorAll('.options-custom-event').forEach(el => {
					el.classList.add('opacity-0')
				});
				if (actionSelectElement.value !== 'custom') {
					document.querySelectorAll('.options-custom-event').forEach(el => {
						el.classList.add('hidden')
					});
				}
			}

			saveEventMappingData(entity);
		});

		actionSelectElement.addEventListener('change', e => {

			let el = e.target;
			el.classList.remove('required');
			let selectedValue = el.value;

			document.querySelectorAll('.options-properties').forEach(el => {
				el.classList.remove('hidden')
			});

			document.querySelectorAll('.options-properties').forEach(el => {
				el.classList.remove('hidden')
			});

			if (selectedValue === 'custom') {
				document.querySelectorAll('.options-custom-action').forEach(el => {
					el.classList.remove('opacity-0', 'hidden')
				});
				document.querySelectorAll('.options-custom-event').forEach(el => {
					el.classList.remove('hidden')
				});
			} else {
				document.querySelectorAll('.options-' + selectedValue).forEach(el => {
					el.classList.remove('hidden')
				});
				document.querySelectorAll('.options-any').forEach(el => {
					el.classList.remove('hidden')
				});
			}

			if (selectedValue !== 'custom') {
				document.querySelectorAll('.options-custom-action').forEach(el => {
					el.classList.add('opacity-0', 'hidden')
				});
				if (eventSelectElement.value !== 'custom') {
					document.querySelectorAll('.options-custom-event').forEach(el => {
						el.classList.add('hidden')
					});
				}
			}

			if (selectedValue !== 'create') {
				//document.querySelectorAll('.options-create').forEach(el => { el.classList.add('hidden') });
			}

			saveEventMappingData(entity);

		});

		dataTypeSelect.addEventListener('change', e => {
			let el = e.target;
			let selectedValue = el.value;
			dataTypeInput.value = selectedValue;
		});

		dataTypeSelect.addEventListener('mousedown', e => {
			e.preventDefault(); // => catches click
			let listEl = dataTypeSelect.parentNode.querySelector('ul');
			if (!listEl) {
				dataTypeSelect.insertAdjacentHTML('afterend', '<ul class="combined-input-select-field"></ul>');
				listEl = dataTypeSelect.parentNode.querySelector('ul');
				document.addEventListener('click', removeListElementHandler);
			}
			dataTypeSelect.querySelectorAll('option').forEach(option => {
				const dataType = option.value;
				appendListItem(listEl, dataType);
			});
		});

		dataTypeInput.addEventListener('keyup', e => {
			const el = e.target;
			const key = e.key;
			let listEl = dataTypeSelect.parentNode.querySelector('ul');
			if (key === 'Escape') {
				listEl.remove(); return;
			}
			if (!listEl) {
				dataTypeSelect.insertAdjacentHTML('afterend', '<ul class="combined-input-select-field"></ul>');
				listEl = dataTypeSelect.parentNode.querySelector('ul');
			} else {
				listEl.querySelectorAll('li').forEach(el => el.remove());
			}
			dataTypeSelect.querySelectorAll('option').forEach(option => {
				const dataType = option.value;
				if (dataType && dataType.match(el.value)) appendListItem(listEl, dataType);
			});
		});

		addParameterMappingButton.addEventListener('click', e => {

			Command.get(entity.id, 'id,type,triggeredActions', (result) => {
				actionMapping = result.triggeredActions[0];
				Command.create({type: 'ParameterMapping', actionMapping: actionMapping.id}, (parameterMapping) => {
					getAndAppendParameterMapping(parameterMapping.id);
				});
			});

		});

		addParameterMappingForTypeButton.addEventListener('click', e => {

			Command.get(entity.id, 'id,type,triggeredActions', (result) => {
				actionMapping = result.triggeredActions[0];
				Command.getSchemaInfo(dataTypeSelect.value, result => {

					let properties = result.filter(property => !property.system);
					//console.log(properties); return;

					for (const property of properties) {

						Command.create({
							type: 'ParameterMapping',
							parameterName: property.jsonName,
							actionMapping: actionMapping.id
						}, (parameterMapping) => {
							getAndAppendParameterMapping(parameterMapping.id);
						});

					}
				});
			});

		});

		successBehaviourSelect.addEventListener('change', e => {
			let el = e.target;
			el.classList.remove('required');
			let selectedValue = el.value;
			//console.log(successBehaviourSelect, selectedValue);
			document.querySelectorAll('.option-success').forEach(el => {
				el.classList.add('hidden')
			});
			document.querySelectorAll('.option-success-' + selectedValue).forEach(el => {
				el.classList.remove('hidden')
			});
		});

		failureBehaviourSelect.addEventListener('change', e => {
			let el = e.target;
			el.classList.remove('required');
			let selectedValue = el.value;
			//console.log(successBehaviourSelect, selectedValue);
			document.querySelectorAll('.option-failure').forEach(el => {
				el.classList.add('hidden')
			});
			document.querySelectorAll('.option-failure-' + selectedValue).forEach(el => {
				el.classList.remove('hidden')
			});

		});

		const appendListItem = (listEl, dataType) => {
			if (dataType) {
				listEl.insertAdjacentHTML('beforeend', `<li data-value="${dataType}">${dataType}</li>`);
				const liEl = listEl.querySelector(`li[data-value="${dataType}"]`);
				if (liEl) {
					liEl.addEventListener('click', e => {
						const el = e.target;
						dataTypeInput.value = el.innerText;
						listEl.remove();
					});
				}
			}
		};

		const removeListElementHandler = (e) => {
			const el = e.target;
			const listEl = dataTypeSelect.parentNode.querySelector('ul');
			if (listEl && el !== dataTypeSelect) {
				console.log(e.target);
				const val = el.dataset['value'];
				if (val) dataTypeInput.value = val;
				e.preventDefault();
				listEl.remove();
				document.removeEventListener('click', removeListElementHandler);
			}
		};


		const updateEventMapping = (entity, actionMapping) => {

			if (!actionMapping) {
				console.warn('No actionMapping object given', entity);
				return;
			}

			//console.log('updateEventMapping', entity, actionMapping);

			let id = 'options-none';

			let event                = actionMapping.event;
			let action               = actionMapping.action;

			let method               = actionMapping.method;
			let targetType           = actionMapping.dataType;
			let idExpression         = actionMapping.idExpression;

			let successBehaviour       = actionMapping.successBehaviour;
			let successPartial         = actionMapping.successPartial;
			let successURL             = actionMapping.successURL;
			let successEvent           = actionMapping.successEvent;

			let failureBehaviour       = actionMapping.failureBehaviour;
			let failurePartial         = actionMapping.failurePartial;
			let failureURL             = actionMapping.failureURL;
			let failureEvent           = actionMapping.failureEvent;

			// TODO: Find better solution for the following conversion which is necessary because of 'previous-page' vs. 'prev-page'
			if (action === 'previous-page') action = 'prev-page';

			if (event === 'custom') {
				customEventInput.value = event;
				customActionInput.value = action;
			} else {
				id = 'options-' + action + '-' + event;
			}

			eventSelectElement.value        = event;
			actionSelectElement.value       = action;

			methodNameInput.value           = method;
			dataTypeSelect.value            = targetType;
			dataTypeInput.value             = targetType;

			if (action === 'update') {
				updateTargetInput.value         = idExpression;
			} else if (action === 'delete') {
				deleteTargetInput.value         = idExpression;
			} else if (action === 'method') {
				methodTargetInput.value         = idExpression;
			} else if (action === 'custom') {
				customTargetInput.value         = idExpression;
			}

			successBehaviourSelect.value     = successBehaviour;
			successPartialRefreshInput.value = successPartial;
			successNavigateToURLInput.value  = successURL;
			successFireEventInput.value      = successEvent;

			failureBehaviourSelect.value     = failureBehaviour;
			failurePartialRefreshInput.value = failurePartial;
			failureNavigateToURLInput.value  = failureURL;
			failureFireEventInput.value      = failureEvent;

			document.querySelectorAll('.options-' + action).forEach(el => {
				el.classList.remove('hidden')
			});
			document.querySelectorAll('.options-any').forEach(el => {
				el.classList.remove('hidden')
			});
			document.querySelectorAll('.option-success-' + successBehaviour).forEach(el => {
				el.classList.remove('hidden')
			});
			document.querySelectorAll('.option-failure-' + failureBehaviour).forEach(el => {
				el.classList.remove('hidden')
			});

			// remove existing parameter mappings
			for (const parameterMappingElement of document.querySelectorAll('.options-properties .parameter-mapping')) {
				parameterMappingElement.remove();
			}

			// append mapped parameters
			Command.get(actionMapping.id, 'id,parameterMappings', (actionMapping) => {
				for (const parameterMapping of actionMapping.parameterMappings) {
					getAndAppendParameterMapping(parameterMapping.id);
				}
			});

			// if (entity.triggeredActions && entity.triggeredActions.length) {
			//
			// 	// TODO: Support multiple actions per DOM element
			// 	let actionMapping = entity.triggeredActions[0];


		};

		const getAndAppendParameterMapping = (id) => {

			Command.get(id, 'id,name,parameterName,parameterType,constantValue,scriptExpression,inputElement', (parameterMapping) => {

				//console.log('Append parameter mapping element for', parameterMapping);

				const html = _Entities.templates.parameterMappingRow(parameterMapping);
				const container = document.querySelector('.parameter-mappings-container');
				container.insertAdjacentHTML('beforeend', html);
				const row = container.querySelector('.parameter-mapping[data-structr-id="' + id + '"]');

				const parameterTypeSelector = row.querySelector('.parameter-type-select');
				parameterTypeSelector.value = parameterMapping.parameterType;
				row.querySelector('.parameter-' + parameterTypeSelector.value)?.classList.remove('hidden');
				activateExistingElementDropzone(row);

				parameterTypeSelector.addEventListener('change', e => {
					const selectElement = e.target;
					const value = selectElement.value;
					row.querySelectorAll('.parameter-value').forEach(el => el.classList.add('hidden'));
					row.querySelector('.parameter-' + value)?.classList.remove('hidden');
					activateExistingElementDropzone(row);
				});

				//console.log(parameterMapping.parameterType, parameterMapping.inputElement);
				if (parameterMapping.parameterType === 'user-input' && parameterMapping.inputElement) {
					replaceDropzoneByInputElement(row, parameterMapping.inputElement);
				}

				const constantValueInputElement = row.querySelector('.parameter-constant-value-input');
				if (parameterMapping.constantValue) {
					constantValueInputElement.value = parameterMapping.constantValue;
				}

				const scriptExpressionInputElement = row.querySelector('.parameter-script-expression-input');
				if (parameterMapping.scriptExpression) {
					scriptExpressionInputElement.value = parameterMapping.scriptExpression;
				}

				activateRemoveIcon(parameterMapping);
				Structr.activateCommentsInElement(container);

			}, null);
		};

		const replaceDropzoneByInputElement = (parentElement, inputElement) => {
			const userInputElement = parentElement.querySelector('.parameter-user-input');
			_Entities.appendRelatedNode($(userInputElement), inputElement, (nodeEl) => {
				$('.remove', nodeEl).on('click', function(e) {
					e.preventDefault();
					e.stopPropagation();
					userInputElement.querySelector('.node').remove();
					dropzoneElement.classList.remove('hidden');
				});
			});
			const dropzoneElement = userInputElement.querySelector('.link-existing-element-dropzone');
			dropzoneElement.classList.add('hidden');
		};

		const activateRemoveIcon = (parameterMapping) => {
			let parameterMappingElement = document.querySelector('.parameter-mapping[data-structr-id="' + parameterMapping.id + '"]');
			let removeIcon = parameterMappingElement.querySelector('.parameter-mapping-remove-button');
			removeIcon?.addEventListener('click', e => {
				Command.deleteNode(parameterMapping.id, false, () => {
					parameterMappingElement.remove();
				});
			});
		};

		const activateExistingElementDropzone = (parentElement) => {

			const parameterMappingId = parentElement.dataset['structrId'];
			const dropzoneElement = parentElement.querySelector('.link-existing-element-dropzone');

			if (dropzoneElement) {

				$(dropzoneElement).droppable({

					drop: (e, el) => {

						e.preventDefault();
						e.stopPropagation();

						let sourceEl = $(el.draggable);
						let sourceId = Structr.getId(sourceEl);

						if (!sourceId) {
							return false;
						}

						let obj = StructrModel.obj(sourceId);

						// Ignore shared components
						if (obj && obj.syncedNodesIds && obj.syncedNodesIds.length || sourceEl.parent().attr('id') === 'componentsArea') {
							return false;
						}

						parentElement.querySelector('.parameter-user-input-input').value = sourceId;
						_Elements.dropBlocked = false;

						let userInputName = obj['_html_name'];
						if (userInputName) {

							let parameterNameInput = parentElement.querySelector('.parameter-name-input');
							if (parameterNameInput.value === '') {
								parameterNameInput.value = userInputName;
							}
						}

						replaceDropzoneByInputElement(parentElement, obj);
					}
				});
			}
		};

		const saveEventMappingData = (entity) => {

			let eventValue             = eventSelectElement?.value;
			let actionValue            = actionSelectElement?.value;

			let methodValue            = methodNameInput?.value;
			let dataTypeValue          = dataTypeInput?.value || dataTypeSelect?.value;

			let updateTargetValue      = updateTargetInput?.value;
			let deleteTargetValue      = deleteTargetInput?.value;
			let methodTargetValue      = methodTargetInput?.value;
			let customTargetValue      = customTargetInput?.value;

			let successBehaviourValue  = successBehaviourSelect?.value;
			let successPartialValue    = successPartialRefreshInput?.value;
			let successURLValue        = successNavigateToURLInput?.value;
			let successEventValue      = successFireEventInput?.value;

			let failureBehaviourValue  = failureBehaviourSelect?.value;
			let failurePartialValue    = failurePartialRefreshInput?.value;
			let failureURLValue        = failureNavigateToURLInput?.value;
			let failureEventValue      = failureFireEventInput?.value;

			// let customEvent        = customEventInput?.value;
			// let customAction       = customActionInput?.value;
			// let customTarget       = customTargetInput?.value;
			//
			// let methodName     = methodNameInput?.value;
			// let methodTarget   = methodTargetInput?.value;
			// let deleteTarget   = deleteTargetInput?.value;
			// let paginationName = paginationNameInput?.value;
			// let reloadTarget   = null;

			let actionMappingObject = {
				type:             'ActionMapping',
				event:            eventValue,
				action:           actionValue,
				method:           methodValue,
				dataType:         dataTypeValue,
				idExpression:     (actionValue === 'method') ? methodTargetValue : (actionValue === 'update') ? updateTargetValue : deleteTargetValue,
				successBehaviour: successBehaviourValue,
				successPartial:   successPartialValue,
				successURL:       successURLValue,
				successEvent:     successEventValue,
				failureBehaviour: failureBehaviourValue,
				failurePartial:   failurePartialValue,
				failureURL:       failureURLValue,
				failureEvent:     failureEventValue
			};

			//console.log(actionMappingObject);

			if (entity.triggeredActions && entity.triggeredActions.length) {

				actionMappingObject.id = entity.triggeredActions[0].id;

				console.log('ActionMapping object already exists, updating...', actionMappingObject);
				Command.setProperties(actionMappingObject.id, actionMappingObject, () => {
					blinkGreen(Structr.nodeContainer(entity.id));
					updateEventMapping(entity, actionMappingObject);
				});

			} else {

				actionMappingObject.triggerElements = [ entity.id ];

				console.log('No ActionMapping object exists, create one and update data...');
				Command.create(actionMappingObject, (actionMapping) => {
					//console.log('Successfully created new ActionMapping object:', actionMapping);
					blinkGreen(Structr.nodeContainer(entity.id));
					updateEventMapping(entity, actionMapping);
				});
			}
		};

		const saveParameterMappings = () => {

			const inputDefinitions = [
				{ key: 'parameterName',    selector: '.parameter-mapping .parameter-name-input' },
				{ key: 'parameterType',    selector: '.parameter-mapping .parameter-type-select' },
				{ key: 'constantValue',    selector: '.parameter-mapping .parameter-constant-value-input' },
				{ key: 'scriptExpression', selector: '.parameter-mapping .parameter-script-expression-input' },
				{ key: 'inputElement',     selector: '.parameter-mapping .parameter-user-input-input' },
				{ key: 'methodResult',     selector: '.parameter-mapping .parameter-method-result-input' },
				{ key: 'flowResult',       selector: '.parameter-mapping .parameter-flow-result-input' }
			];

			const parameterMappings = document.querySelectorAll('.parameter-mapping');

			//console.log('save parameter mappings', inputDefinitions, parameterMappings);

			for (const parameterMappingElement of parameterMappings) {
				const parameterMappingId = parameterMappingElement.dataset['structrId'];
				//console.log(parameterMappingId);
				const parameterMappingData = { id: parameterMappingId };
				for (const inputDefinition of inputDefinitions) {

					for (const inp of parameterMappingElement.querySelectorAll(inputDefinition.selector)) {
						const value     = inp.value;
						if (value) {
							//console.log(inputDefinition.key, value);
							parameterMappingData[inputDefinition.key] = value;
						}
					}
				}

				//console.log(parameterMappingData);
				Command.setProperties(parameterMappingId, parameterMappingData);

			}
		};

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
	repeaterConfig: (entity, el) => {

		let queryTypes = [
			{ title: 'REST Query',     propertyName: 'restQuery' },
			{ title: 'Cypher Query',   propertyName: 'cypherQuery' },
//			{ title: 'XPath Query',    propertyName: 'xpathQuery' },
			{ title: 'Function Query', propertyName: 'functionQuery' }
		];

		if (Structr.isModulePresent('flows')) {
			queryTypes.unshift({ title: 'Flow', propertyName: 'flow' });
		}

		let queryTypeButtonsContainer = el.querySelector('.query-type-buttons');

		let queryTextElement = el.querySelector('.query-text');
		let flowSelector     = el.querySelector('#flow-selector');

		let repeaterConfigEditor;
		let saveQueryButton;

		let saveFunction = () => {

			if (queryTypeButtonsContainer.querySelectorAll('button.active').length > 1) {
				return new MessageBuilder().error('Please select only one query type.').show();
			}

			let data = {};

			for (let queryType of queryTypes) {

				let val = null;

				if (queryTypeButtonsContainer.querySelector('.' + queryType.propertyName).classList.contains('active')) {

					if (queryType.propertyName === 'flow') {

						val = flowSelector.value;

					} else {

						val = repeaterConfigEditor.getValue();
						data.flow = null;
						flowSelector.value = '--- Select Flow ---';
					}
				}

				data[queryType.propertyName] = val;
			};

			Command.setProperties(entity.id, data, (obj) => {

				Object.assign(entity, data);

				// vanilla replacement for $.is(':visible')
				if (flowSelector.offsetParent !== null) {
					blinkGreen(flowSelector);
				} else {
					blinkGreen(saveQueryButton);
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

		let initRepeaterInputs = () => {

			saveQueryButton = el.querySelector('button.save-repeater-query');

			for (let queryType of queryTypes) {
				queryTypeButtonsContainer.insertAdjacentHTML('beforeend', `<button data-query-type="${queryType.propertyName}" class="${queryType.propertyName} hover:bg-gray-100 focus:border-gray-666 active:border-green">${queryType.title}</button>`);
			}

			let allQueryTypeButtons = el.querySelectorAll('.query-type-buttons button');

			let queryTypeButtonClickAction = (e) => {

				for (let queryTypeButton of allQueryTypeButtons) {
					queryTypeButton.classList.remove('active');
				}
				e.target.classList.add('active');

				let queryType = e.target.dataset['queryType'];

				if (queryType === 'flow') {

					saveQueryButton.classList.add('hidden');
					queryTextElement.classList.add('hidden');
					flowSelector.classList.remove('hidden');

				} else {

					saveQueryButton.classList.remove('hidden');
					queryTextElement.classList.remove('hidden');
					flowSelector.classList.add('hidden');
					activateEditor(queryType);
				}
			};

			for (let queryTypeButton of allQueryTypeButtons) {
				queryTypeButton.addEventListener('click', queryTypeButtonClickAction);
			}

			for (let queryType of queryTypes) {

				if (queryType.propertyName === 'flow' && entity[queryType.propertyName]) {

					el.querySelector('button.' + queryType.propertyName).click();
					flowSelector.value = entity[queryType.propertyName].id;

				} else if (entity[queryType.propertyName] && entity[queryType.propertyName].trim() !== "") {

					el.querySelector('button.' + queryType.propertyName).click();
				}
			}

			if (queryTypeButtonsContainer.querySelectorAll('button.active').length === 0) {
				el.querySelector('.query-type-buttons button').click();
			}

			flowSelector.addEventListener('change', saveFunction);
			saveQueryButton.addEventListener('click', saveFunction);

			let datakeyInput      = el.querySelector('input.repeater-datakey');
			let saveDatakeyButton = el.querySelector('button.save-repeater-datakey');

			datakeyInput.value = entity.dataKey;

			saveDatakeyButton.addEventListener('click', () => {

				Command.setProperty(entity.id, 'dataKey', datakeyInput.value, false, () => {
					blinkGreen(datakeyInput);
					entity.dataKey = datakeyInput.value;
				});
			});
		};

		if (Structr.isModulePresent('flows')) {

			flowSelector.insertAdjacentHTML('beforeend', '<option>--- Select Flow ---</option>');

			Command.getByType('FlowContainer', 1000, 1, 'effectiveName', 'asc', null, false, (flows) => {

				flowSelector.insertAdjacentHTML('beforeend', flows.map(flow => '<option value="' + flow.id + '">' + flow.effectiveName + '</option>').join());

				initRepeaterInputs();

				_Editors.resizeVisibleEditors();
			});

		} else {

			flowSelector?.remove();

			initRepeaterInputs();
			_Editors.resizeVisibleEditors();
		}
	},
	editEmptyDiv: (entity) => {

		Structr.dialog('Edit source of "' + (entity.name ? entity.name : entity.id) + '"', () => {}, () => {}, ['popup-dialog-with-editor']);
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
			saveFn: (editor, entity, close = false) => {

				let text1 = initialText || '';
				let text2 = editor.getValue();

				if (text1 === text2) {
					return;
				}

				Command.patch(entity.id, text1, text2, () => {

					Structr.showAndHideInfoBoxMessage('Content saved.', 'success', 2000, 200);
					dialogSaveButton.disabled = true;
					dialogSaveButton.classList.add('disabled')

					Command.getProperty(entity.id, 'content', (newText) => {
						initialText = newText;
					});
				});
				Command.saveNode(`<div data-structr-hash="${entity.id}">${editor.getValue()}</div>`, entity.id, () => {

					Structr.showAndHideInfoBoxMessage('Node source saved and DOM tree rebuilt.', 'success', 2000, 200);

					if (_Entities.isExpanded(Structr.node(entity.id))) {
						$('.expand_icon_svg', Structr.node(entity.id)).click().click();
					}

					if (close === true) {
						dialogCancelButton.click();
					}
				});
			}
		};

		let editor = _Editors.getMonacoEditor(entity, 'source', dialog[0].querySelector('.editor'), emptyDivMonacoConfig);

		_Editors.addEscapeKeyHandlersToPreventPopupClose(editor);

		dialogSaveButton.addEventListener('click', (e) => {
			e.stopPropagation();

			emptyDivMonacoConfig.saveFn(editor, entity);
		});

		saveAndClose.addEventListener('click', (e) => {
			e.stopPropagation();

			emptyDivMonacoConfig.saveFn(editor, entity, true);
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
	getSchemaProperties: (type, view, callback) => {

		fetch(Structr.rootUrl + '_schema/' + type + '/' + view).then(async response => {

			let data = await response.json();

			if (response.ok) {

				let properties = {};

				if (data.result) {

					for (let prop of data.result) {
						properties[prop.jsonName] = prop;
					}
				}

				if (callback) {
					callback(properties);
				}

			} else {
				Structr.errorFromResponse(data, url);
				console.log("ERROR: loading Schema " + type);
			}
		});

	},
	showProperties: (obj, activeViewOverride, parent) => {

		let handleGraphObject;

		_Entities.getSchemaProperties(obj.type, 'custom', (properties) => {

			handleGraphObject = (entity) => {

				let views      = ['ui'];
				let activeView = 'ui';
				let tabTexts   = [];

				if (Object.keys(properties).length) {
					views.push('custom');
				}

				if (activeViewOverride) {
					activeView = activeViewOverride;
				}

				_Schema.getTypeInfo(entity.type, (typeInfo) => {

					let dialogTitle = 'Edit properties';

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
						tabTexts.ui     = 'Advanced';
						tabTexts.custom = 'Custom Properties';

						dialogTitle = 'Edit properties of ' + (entity.type ? entity.type : '') + ' node ' + (entity.name ? entity.name : entity.id);
					}


					let tabsdiv, mainTabs, contentEl;
					if (!parent) {
						Structr.dialog(dialogTitle, () => { return true; }, () => { return true; });

						tabsdiv   = dialogHead.append('<div id="tabs"></div>');
						mainTabs  = tabsdiv.append('<ul></ul>');
						contentEl = dialog.append('<div></div>');

					} else {
						contentEl = parent;
					}

					// custom dialog tab?
					_Dialogs.findAndAppendCustomTypeDialog(entity, mainTabs, contentEl);

					_Entities.appendViews(entity, views, tabTexts, mainTabs, contentEl, typeInfo);

					if (!entity.hasOwnProperty('relType')) {
						_Entities.appendPropTab(entity, mainTabs, contentEl, 'permissions', 'Security', false, (c) => {
							_Entities.accessControlDialog(entity, c, typeInfo);
						});
					}

					activeView = activeViewOverride || LSWrapper.getItem(_Entities.activeEditTabPrefix  + '_' + entity.id) || activeView;
					$('#tab-' + activeView).click();

					Structr.resize();
				});
			};

			if (obj.relType) {
				Command.getRelationship(obj.id, obj.target, null, entity => handleGraphObject(entity), 'ui');
			} else {
				Command.get(obj.id, null, entity => handleGraphObject(entity), 'ui');
			}
		});
	},
	appendPropTab: (entity, tabsEl, contentEl, name, label, isActive, callback, showCallback, refreshOnShow = false) => {

		let tabId     = `tab-${name}`;
		let tabViewId = `tabView-${name}`;
		let ul        = tabsEl.children('ul');

		ul.append(`<li id="${tabId}">${label}</li>`);
		contentEl.append(`<div class="propTabContent h-full" id="${tabViewId}" data-tab-id="${tabId}"></div>`);

		let tabContent = $('#' + tabViewId);
		let tab        = $('#' + tabId);
		if (isActive) {
			tab.addClass('active');
		}

		tab.on('click', (e) => {

			e.stopPropagation();
			$('.propTabContent', contentEl).hide();
			$('li', ul).removeClass('active');
			tabContent.show();
			tab.addClass('active');
			LSWrapper.setItem(_Entities.activeEditTabPrefix  + '_' + entity.id, name);

			if (typeof showCallback === 'function') {

				// this does not really work for the initially active tab because its html is not yet output... showCallback must also be called from 'outside'

				if (refreshOnShow === true) {
					// update entity for show callback
					if (entity.relType) {
						Command.getRelationship(entity.id, entity.target, null, (e) => {
							showCallback(tabContent, e);
						});
					} else {
						Command.get(entity.id, null, (e) => {
							showCallback(tabContent, e);
						});
					}
				} else {
					showCallback(tabContent, e);
				}
			}
		});

		if (isActive) {
			tabContent.show();
		}
		if (callback) {
			callback(tabContent, entity);
		}

		return { tab: tab, tabContent, tabContent };
	},
	appendViews: function(entity, views, texts, tabsEl, contentEl, typeInfo) {

		let ul = tabsEl.children('ul');

		for (let view of views) {

			ul.append(`<li id="tab-${view}">${texts[view]}</li>`);

			contentEl.append(`<div class="propTabContent" id="tabView-${view}"></div>`);

			let tab = $('#tab-' + view);

			tab.on('click', function(e) {
				e.stopPropagation();

				let self = $(this);
				contentEl.children('div').hide();
				$('li', ul).removeClass('active');
				self.addClass('active');

				let tabView = $('#tabView-' + view);
				fastRemoveAllChildren(tabView[0]);
				tabView.show();
				LSWrapper.setItem(_Entities.activeEditTabPrefix  + '_' + entity.id, view);

				_Entities.listProperties(entity, view, tabView, typeInfo, () => {
					$('input.dateField', tabView).each(function(i, input) {
						_Entities.activateDatePicker($(input));
					});
				});
			});
		}
	},
	getNullIconForKey: (key) => _Icons.getSvgIconWithID(`${_Entities.null_prefix}${key}`, _Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['nullIcon', 'icon-grey'])),
	listProperties: (entity, view, tabView, typeInfo, callback) => {

		_Entities.getSchemaProperties(entity.type, view, (properties) => {

			let filteredProperties   = Object.keys(properties).filter(key => !(typeInfo[key].isCollection && typeInfo[key].relatedType) );
			let collectionProperties = Object.keys(properties).filter(key => typeInfo[key].isCollection && typeInfo[key].relatedType );

			fetch(Structr.rootUrl + entity.type + '/' + entity.id + '/all?' + Structr.getRequestParameterName('edit') + '=2', {
				headers: {
					Accept: 'application/json; charset=utf-8; properties=' + filteredProperties.join(',')
				}
			}).then(async response => {

				let data          = await response.json();
				let fetchedEntity = data.result;

				if (response.ok) {

					let tempNodeCache = new AsyncObjectCache((id) => {
						Command.get(id, 'id,name,type,tag,isContent,content', (node) => {
							tempNodeCache.addObject(node, node.id);
						});
					});

					let keys           = Object.keys(properties);
					let noCategoryKeys = [];
					let groupedKeys    = {};

					if (typeInfo) {

						for (let key of keys) {

							if (typeInfo[key] && typeInfo[key].category && typeInfo[key].category !== 'System') {

								let category = typeInfo[key].category;
								if (!groupedKeys[category]) {
									groupedKeys[category] = [];
								}
								groupedKeys[category].push(key);
							} else {
								noCategoryKeys.push(key);
							}
						}
					}

					if (view === '_html_') {
						// add custom html attributes
						for (let key in fetchedEntity) {

							if (key.startsWith('_custom_html_')) {
								noCategoryKeys.push(key);
							}
						}
					}

					// reset result counts
					_Entities.collectionPropertiesResultCount = {};

					_Entities.createPropertyTable(null, noCategoryKeys, fetchedEntity, entity, view, tabView, typeInfo, tempNodeCache);

					for (let categoryName of Object.keys(groupedKeys).sort()) {
						_Entities.createPropertyTable(categoryName, groupedKeys[categoryName], fetchedEntity, entity, view, tabView, typeInfo, tempNodeCache);
					}

					// populate collection properties with first page
					for (let key of collectionProperties) {
						_Entities.displayCollectionPager(tempNodeCache, entity, key, 1, tabView);
					}
				}

				if (typeof callback === 'function') {
					callback(properties);
				}
			});
		});
	},
	displayCollectionPager: (tempNodeCache, entity, key, page, container) => {

		let pageSize = 10, resultCount;

		let cell = $('.value.' + key + '_', container);
		cell.css('height', '60px');

		fetch(`${Structr.rootUrl + entity.type}/${entity.id}/${key}?${Structr.getRequestParameterName('pageSize')}=${pageSize}&${Structr.getRequestParameterName('page')}=${page}`, {
			headers: {
				Accept: 'application/json; charset=utf-8; properties=id,name'
			}
		}).then(async response => {

			let data = await response.json();

			if (response.ok) {

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
					pageUpButton.removeClass('disabled').on('click', () => {
						_Entities.displayCollectionPager(tempNodeCache, entity, key, page-1, container);
						return false;
					});
				}

				if ((!resultCount && data.result.length > 0) || page < Math.ceil(resultCount/pageSize)) {
					pageDownButton.removeClass('disabled').on('click', () => {
						_Entities.displayCollectionPager(tempNodeCache, entity, key, page+1, container);
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

					for (let obj of (data.result[0][key] || data.result)) {

						let nodeId = (typeof obj === 'string') ? obj : obj.id;

						tempNodeCache.registerCallback(nodeId, nodeId, (node) => {

							_Entities.appendRelatedNode(cell, node, (nodeEl) => {
								$('.remove', nodeEl).on('click', (e) => {

									e.preventDefault();
									Command.removeFromCollection(entity.id, key, node.id, () => {
										nodeEl.remove();
										blinkGreen(cell);
										Structr.showAndHideInfoBoxMessage(`Related node "${node.name || node.id}" has been removed from property "${key}".`, 'success', 2000, 1000);
									});
									return false;
								});
							});
						});
					}
				}
			}
		})
	},
	createPropertyTable: function(heading, keys, res, entity, view, container, typeInfo, tempNodeCache) {

		if (heading) {
			container.append(`<h2>${heading}</h2>`);
		}
		container.append(`<table class="props ${view} ${res['id']}_"></table>`);
		let propsTable = $('table:last', container);
		let focusAttr  = 'class';
		let id         = entity.id;

		if (view === '_html_') {
			keys.sort();
		}

		for (let key of keys) {

			let valueCell    = undefined;
			let isReadOnly   = false;
			let isSystem     = false;
			let isBoolean    = false;
			let isDate       = false;
			let isPassword   = false;
			let isRelated    = false;
			let isCollection = false;
			let isMultiline  = false;

			if (view === '_html_') {

				let showKeyInitially = false;
				for (let mostUsed of _Elements.mostUsedAttrs) {
					if (isIn(entity.tag, mostUsed.elements) && isIn(key.substring(6), mostUsed.attrs)) {
						showKeyInitially = true;
						focusAttr = mostUsed.focus ? mostUsed.focus : focusAttr;
					}
				}

				// Always show non-empty, non 'data-structr-' attributes
				if (res[key] !== null && key.indexOf('data-structr-') !== 0) {
					showKeyInitially = true;
				}

				let displayKey = key;
				if (key.indexOf('data-') !== 0) {
					if (key.indexOf('_html_') === 0) {
						displayKey = displayKey.substring(6);
					} else if (key.indexOf('_custom_html_') === 0) {
						displayKey = displayKey.substring(13);
					}
				}

				if (showKeyInitially || key === '_html_class' || key === '_html_id') {
					propsTable.append(`<tr><td class="key">${displayKey}</td><td class="value ${key}_">${formatValueInputField(key, res[key])}</td><td>${_Entities.getNullIconForKey(key)}</td></tr>`);
				} else if (key !== 'id') {
					propsTable.append(`<tr class="hidden"><td class="key">${displayKey}</td><td class="value ${key}_">${formatValueInputField(key, res[key])}</td><td>${_Entities.getNullIconForKey(key)}</td></tr>`);
				}
				valueCell = $(`.value.${key}_`, propsTable);

			} else {

				let row = $(`<tr><td class="key">${formatKey(key)}</td><td class="value ${key}_"></td><td>${_Entities.getNullIconForKey(key)}</td></tr>`);
				propsTable.append(row);
				valueCell = $(`.value.${key}_`, propsTable);

				if (!typeInfo[key]) {

					valueCell.append(formatValueInputField(key, res[key], isPassword, isReadOnly, isMultiline));

				} else {

					let type = typeInfo[key].type;

					isReadOnly  = isIn(key, _Entities.readOnlyAttrs) || (typeInfo[key].readOnly);
					isSystem    = typeInfo[key].system;
					isPassword  = (typeInfo[key].className === 'org.structr.core.property.PasswordProperty');
					isMultiline = (typeInfo[key].format === 'multi-line');
					isRelated   = typeInfo[key].relatedType;
					if (isRelated) {
						isCollection = typeInfo[key].isCollection;
					}

					if (type) {
						isBoolean = (type === 'Boolean');
						isDate = (type === 'Date');
					}

					if (!key.startsWith('_html_')) {
						if (isBoolean) {
							valueCell.removeClass('value').append(`<input type="checkbox" class="${key}_">`);
							let checkbox = $(propsTable.find(`input[type="checkbox"].${key}_`));

							let val = res[key];
							if (val) {
								checkbox.prop('checked', true);
							}
							if ((!isReadOnly || StructrWS.isAdmin) && !isSystem) {
								checkbox.on('change', function() {
									let checked = checkbox.prop('checked');
									_Entities.setProperty(id, key, checked, false, (newVal) => {
										if (val !== newVal) {
											blinkGreen(valueCell);
										}
										checkbox.prop('checked', newVal);
										val = newVal;
									});
								});
							} else {
								checkbox.prop('disabled', 'disabled').addClass('readOnly').addClass('disabled');
							}

						} else if (isDate && !isReadOnly) {

							valueCell.append(`<input class="dateField" name="${key}" type="text" value="${res[key] || ''}" data-date-format="${typeInfo[key].format}">`);

						} else if (isRelated) {

							if (res[key]) {

								if (!isCollection) {

									let nodeId = res[key].id || res[key];

									tempNodeCache.registerCallback(nodeId, nodeId, function(node) {

										_Entities.appendRelatedNode(valueCell, node, function(nodeEl) {
											$('.remove', nodeEl).on('click', function(e) {
												e.preventDefault();
												_Entities.setProperty(id, key, null, false, (newVal) => {
													if (!newVal) {
														nodeEl.remove();
														blinkGreen(valueCell);
														Structr.showAndHideInfoBoxMessage(`Related node "${node.name || node.id}" has been removed from property "${key}".`, 'success', 2000, 1000);
													} else {
														blinkRed(valueCell);
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

							valueCell.append(_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['add', 'icon-green'])))
							$('.add', valueCell).on('click', function() {
								Structr.dialog(`Add ${typeInfo[key].type}`, () => {}, () => {});
								_Entities.displaySearch(id, key, typeInfo[key].type, dialogText, isCollection);
							});

						} else {
							valueCell.append(formatValueInputField(key, res[key], isPassword, isReadOnly, isMultiline));
						}
					}
				}
			}

			_Entities.appendSchemaHint($('.key:last', propsTable), key, typeInfo);

			let nullIconId = `#${_Entities.null_prefix}${key}`;

			if (isSystem || isReadOnly || isBoolean) {

				container[0].querySelector(nullIconId).remove();

			} else {

				container[0].querySelector(nullIconId).addEventListener('click', (e) => {

					let icon     = e.target.closest(nullIconId);
					let key      = icon.id.substring(_Entities.null_prefix.length);
					let input    = $(`.${key}_`).find('input');
					let textarea = $(`.${key}_`).find('textarea');

					_Entities.setProperty(id, key, null, false, (newVal) => {

						if (!newVal) {
							if (key.indexOf('_custom_html_') === -1) {
								blinkGreen(valueCell);
								Structr.showAndHideInfoBoxMessage(`Property "${key}" has been set to null.`, 'success', 2000, 1000);
							} else {
								icon.closest('tr').remove();
								Structr.showAndHideInfoBoxMessage(`Custom HTML property "${key}" has been removed`, 'success', 2000, 1000);
							}

							if (key === 'name') {
								let entity = StructrModel.objects[id];
								if (!_Entities.isContentElement(entity)) {
									entity.name = entity.tag ?? `[${entity.type}]`;
								}
								StructrModel.refresh(id);
							}
							if (isRelated) {
								valueCell.empty();
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
			}
		}

		$('.props tr td.value input',    container).each(function(i, inputEl)    { _Entities.activateInput(inputEl,    id, entity.pageId, typeInfo); });
		$('.props tr td.value textarea', container).each(function(i, textareaEl) { _Entities.activateInput(textareaEl, id, entity.pageId, typeInfo); });

		if (view === '_html_') {

			$(`input[name="_html_${focusAttr}"]`, propsTable).focus();

			container.append(`
				<div class="flex items-center mt-4 mb-4">
					<button class="show-all hover:bg-gray-100 focus:border-gray-666 active:border-green ml-4 mr-4">Show all attributes</button>
					<button class="add-custom-attribute hover:bg-gray-100 focus:border-gray-666 active:border-green mr-2">Add custom property</button>
				</div>
			`);

			$('.show-all', container).on('click', function() {

				propsTable.addClass('show-all');

				$('tr:visible:odd').css({'background-color': '#f6f6f6'});
				$('tr:visible:even').css({'background-color': '#fff'});
				$(this).attr('disabled', 'disabled').addClass('disabled');
			});

			let addCustomAttributeButton = $('.add-custom-attribute', container);

			Structr.appendInfoTextToElement({
				element: addCustomAttributeButton,
				text: "Any property name is allowed but the 'data-' prefix is recommended. Please note that 'data-structr-' is reserved for internal use.",
				insertAfter: true,
				customToggleIconClasses: ['icon-blue'],
				noSpan: true
			});

			let saveCustomHTMLAttribute = (row, exitedInput) => {

				let keyInput = row.querySelector('td.key input');
				let valInput = row.querySelector('td.value input');

				let key = keyInput.value.trim();
				let val = valInput.value.trim();

				// only run save action if we have a key and we just left the value input
				if (key !== '' && exitedInput === valInput) {

					let regexAllowed = new RegExp("^[a-zA-Z0-9_\-]*$");

					if (key.indexOf('data-structr-') === 0) {

						blinkRed(keyInput);
						new MessageBuilder().error('Key can not begin with "data-structr-" as it is reserved for internal use.').show();

					} else if (!regexAllowed.test(key)) {

						blinkRed(keyInput);
						new MessageBuilder().error('Key contains forbidden characters. Allowed: "a-z", "A-Z", "-" and "_".').show();

					} else {

						let newKey = '_custom_html_' + key;

						Command.setProperty(id, newKey, val, false, () => {
							blinkGreen(exitedInput);
							Structr.showAndHideInfoBoxMessage(`New property "${newKey}" has been added and saved with value "${val}".`, 'success', 2000, 1000);

							keyInput.replaceWith(key);
							valInput.name = newKey;

							let nullIcon = Structr.createSingleDOMElementFromHTML(_Entities.getNullIconForKey(newKey));
							row.querySelector('td:last-of-type').appendChild(nullIcon);
							nullIcon.addEventListener('click', () => {

								let key = nullIcon.getAttribute('id').substring(_Entities.null_prefix.length);

								_Entities.setProperty(id, key, null, false, (newVal) => {
									row.remove();
									Structr.showAndHideInfoBoxMessage(`Custom HTML property "${key}" has been removed`, 'success', 2000, 1000);
								});
							});

							// deactivate this function and resume regular save-actions
							_Entities.activateInput(valInput, id, entity.pageId, typeInfo);
						});
					}
				}
			};

			addCustomAttributeButton.on('click', () => {
				let newAttributeRow = Structr.createSingleDOMElementFromHTML('<tr><td class="key"><input type="text" class="newKey" name="key"></td><td class="value"><input type="text" value=""></td><td></td></tr>')
				propsTable[0].appendChild(newAttributeRow);

				for (let input of newAttributeRow.querySelectorAll('input')) {
					input.addEventListener('focusout', () => {
						saveCustomHTMLAttribute(newAttributeRow, input);
					});
				}
			});
		}

		$('tr:visible:odd', container).css({'background-color': '#f6f6f6'});
		$('tr:visible:even', container).css({'background-color': '#fff'});
	},
	displaySearch: (id, key, type, el, isCollection) => {

		el.append(`
			<div class="searchBox searchBoxDialog flex justify-end">
				<input class="search" name="search" size="20" placeholder="Search">
				${_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['clearSearchIcon', 'icon-lightgrey', 'cursor-pointer']), 'Clear Search')}
			</div>
		`);

		let searchBox = $('.searchBoxDialog', el);
		let search = $('.search', searchBox);
		window.setTimeout(() => {
			search.focus();
		}, 250);

		search.keyup(function(e) {
			e.preventDefault();

			let searchString = $(this).val();
			if (searchString && searchString.length && e.keyCode === 13) {

				$('.clearSearchIcon', searchBox).show().on('click', function() {
					if (_Entities.clearSearchResults(el)) {
						$('.clearSearchIcon').hide().off('click');
						search.val('');
						search.focus();
					}
				});

				$('.searchResults', el).remove();
				el.append('<div class="searchResults"><div class="result-box"></div></div>');
				let box = el.find('.result-box');

				let resultHandler = (nodes) => {

					for (let node of nodes) {

						if (!node.path || node.path.indexOf('/._structr_thumbnails/') !== 0) {

							let displayName = node.title || node.name || node.id;
							box.append(`<div title="${escapeForHtmlAttributes(displayName)}" " class="_${node.id} node element abbr-ellipsis abbr-120">${displayName}</div>`);
							$('._' + node.id, box).on('click', function() {

								let nodeEl = $(this);

								if (isCollection) {

									_Entities.addToCollection(id, node.id, key, () => {

										blinkGreen(nodeEl);

										if (Structr.isModuleActive(_Pages)) {
											_Pages.refreshCenterPane(StructrModel.obj(id), location.hash);
										}
										if (Structr.isModuleActive(_Contents)) {
											_Contents.refreshTree();
										}
										if (Structr.isModuleActive(_Files)) {
											_Files.refreshTree();
										}
									});

								} else {

									Command.setProperty(id, key, node.id, false, () => {

										if (Structr.isModuleActive(_Pages)) {
											_Pages.refreshCenterPane(StructrModel.obj(id), location.hash);
										}

										dialogCancelButton.click();
									});
								}
							});
						}
					}
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
	clearSearch: (el) => {
		if (_Entities.clearSearchResults(el)) {
			$('.clearSearchIcon').hide().off('click');
			$('.search').val('');
			$('#resourceTabs', $(Structr.mainContainer)).show();
			$('#resourceBox', $(Structr.mainContainer)).show();
		}
	},
	clearSearchResults: (el) => {
		let searchResults = $('.searchResults', el);
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
		el.append(`<input class="dateField" name="${key}" type="text" value="${entity[key]}" autocomplete="off">`);
		let dateField = $(el.find('.dateField'));
		_Entities.activateDatePicker(dateField, format);

		return dateField;
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
	appendRelatedNode: (cell, node, onDelete) => {
		let displayName = _Crud.displayName(node);
		cell.append(`
			<div title="${escapeForHtmlAttributes(displayName)}" class="_${node.id} node ${node.type ? node.type.toLowerCase() : (node.tag ? node.tag : 'element')} ${node.id}_">
				<span class="abbr-ellipsis abbr-80">${displayName}</span>
				${_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['remove', 'icon-lightgrey', 'cursor-pointer']))}
			</div>
		`);
		let nodeEl = $('._' + node.id, cell);

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

		let input = $(el);
		let oldVal = input.val();
		let relId = input.parent().attr('rel_id');
		let objId = relId ? relId : id;
		let key = input.prop('name');

		if (!input.hasClass('readonly') && !input.hasClass('newKey')) {

			input.closest('.array-attr').find('svg.remove').off('click').on('click', function(el) {
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
					Structr.showAndHideInfoBoxMessage(`Updated property "${key}"${!isPassword ? ' with ' + valueMsg + '' : ''}`, 'success', 2000, 200);

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
				Structr.showAndHideInfoBoxMessage(`Updated property "${key}" with ${valueMsg}.`, 'success', 2000, 200);

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
	bindAccessControl: function(btn, entity) {

		btn.on('click', function(e) {
			e.stopPropagation();
			_Entities.showAccessControlDialog(entity);
		});
	},
	accessControlDialog: (entity, el, typeInfo) => {

		let id = entity.id;
		let requiredAttributesForPrincipals = 'id,name,type,isGroup,isAdmin';

		let handleGraphObject = (entity) => {

			let allowRecursive = (entity.type === 'Template' || entity.isFolder || (Structr.isModuleActive(_Pages) && !(entity.isContent)));
			let owner_select_id = 'owner_select_' + id;
			el.append(`
				<h3>Owner</h3>
				<div>
					<select id="${owner_select_id}"></select>
				</div>
				<h3>Visibility</h3>
				<div class="security-container">
					${allowRecursive ? '<div><input id="recursive" type="checkbox" name="recursive"><label for="recursive">Apply visibility switches recursively</label></div><br>' : ''}
				</div>
				<h3>Access Rights</h3>
				<table class="props" id="principals">
					<thead>
						<tr>
							<th>Name</th>
							<th>Read</th>
							<th>Write</th>
							<th>Delete</th>
							<th>Access Control</th>
							${allowRecursive ? '<th></th>' : ''}
						</tr>
					</thead>
					<tbody>
						<tr id="new">
							<td>
								<select style="z-index: 999" id="newPrincipal"><option></option></select>
							</td>
							<td></td>
							<td></td>
							<td></td>
							<td></td>
							${allowRecursive ? '<td></td>' : ''}
						</tr>
					</tbody>
				</table>
			`);

			let securityContainer = el.find('.security-container');
			_Entities.appendBooleanSwitch(securityContainer, entity, 'visibleToPublicUsers', ['Visible to public users', 'Not visible to public users'], 'Click to toggle visibility for users not logged-in', '#recursive');
			_Entities.appendBooleanSwitch(securityContainer, entity, 'visibleToAuthenticatedUsers', ['Visible to auth. users', 'Not visible to auth. users'], 'Click to toggle visibility to logged-in users', '#recursive');

			fetch(Structr.rootUrl + entity.id + '/in').then(async response => {

				let data = await response.json();

				if (response.ok) {

					for (let result of data.result) {

						let permissions = {
							read: isIn('read', result.allowed),
							write: isIn('write', result.allowed),
							delete: isIn('delete', result.allowed),
							accessControl: isIn('accessControl', result.allowed)
						};

						let principalId = result.principalId;
						if (principalId) {
							Command.get(principalId, requiredAttributesForPrincipals, (p) => {
								_Entities.addPrincipal(entity, p, permissions, allowRecursive, el);
							});
						}
					}
				}
			})

			let ownerSelect   = $('#' + owner_select_id, el);
			let granteeSelect = $('#newPrincipal', el);
			let spinnerIcon   = Structr.loaderIcon(granteeSelect.parent(), { float: 'right' });

			Command.getByType('Principal', null, null, 'name', 'asc', requiredAttributesForPrincipals, false, (principals) => {

				let ownerOptions        = '';
				let granteeGroupOptions = '';
				let granteeUserOptions  = '';

				if (!entity.owner) {
					ownerOptions += '<option></option>';
				}

				for (let p of principals) {

					if (p.isGroup) {
						granteeGroupOptions += _Entities.templateForPrincipalOption(p);
					} else {
						granteeUserOptions  += _Entities.templateForPrincipalOption(p);
						ownerOptions        += _Entities.templateForPrincipalOption(p, (entity.owner?.id === p.id));
					}
				}

				ownerSelect.append(ownerOptions);
				granteeSelect.append(granteeGroupOptions + granteeUserOptions);

				let templateOption = (state, isSelection) => {
					if (!state.id || state.disabled) {
						return state.text;
					}
					let icon = _Icons.getIconForPrincipal(JSON.parse(state.element.dataset.principal));

					return $(`<span class="flex items-center gap-2 ${isSelection ? 'select-selection-with-icon' : 'select-result-with-icon'}">${icon} ${state.text}</span>`);
				};

				let dropdownParent = (dialogBox && dialogBox.is(':visible')) ? dialogBox : $('body');

				ownerSelect.select2({
					allowClear: true,
					placeholder: 'Owner',
					width: '300px',
					style: 'text-align:left;',
					dropdownParent: dropdownParent,
					templateResult: (state) => templateOption(state, false),
					templateSelection: (state) => templateOption(state, true)
				}).on('select2:unselecting', function(e) {
					e.preventDefault();

					Command.setProperty(id, 'owner', null, false, () => {
						blinkGreen(ownerSelect.parent());
						ownerSelect.val(null).trigger('change');
					});

				}).on('select2:select', function(e) {

					let data = e.params.data;
					Command.setProperty(id, 'owner', data.id, false, () => {
						blinkGreen(ownerSelect.parent());
					});
				});

				granteeSelect.select2({
					placeholder: 'Select Group/User',
					width: '100%',
					dropdownParent: dropdownParent,
					templateResult: (state) => templateOption(state, false)
				}).on('select2:select', function(e) {

					let data = e.params.data;
					let pId  = data.id;
					let rec  = $('#recursive', el).is(':checked');

					Command.setPermission(entity.id, pId, 'grant', 'read', rec);

					Command.get(pId, requiredAttributesForPrincipals, (p) => {
						_Entities.addPrincipal(entity, p, { read: true }, allowRecursive, el);
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
	templateForPrincipalOption: (p, selected = false) => `<option value="${p.id}" data-principal="${escapeForHtmlAttributes(JSON.stringify(p))}" ${(selected ? 'selected' : '')}>${p.name}</option>`,
	showAccessControlDialog: (entity) => {

		let id = entity.id;

		let initialObj = {
			ownerId: entity.owner ? entity.owner.id : null,
			visibleToPublicUsers: entity.visibleToPublicUsers,
			visibleToAuthenticatedUsers: entity.visibleToAuthenticatedUsers
		};

		Structr.dialog('Access Control and Visibility', () => {}, () => {

			if (Structr.isModuleActive(_Crud)) {

				let handleGraphObject = (entity) => {

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

		$(`#newPrincipal option[value="${principal.id}"]`, container).css({
			display: 'none'
		});
		$('#newPrincipal', container).trigger('chosen:updated');

		if ($('#principals ._' + principal.id, container).length > 0) {
			return;
		}

		let row = $(`<tr class="_${principal.id}"><td><div class="flex items-center gap-2">${_Icons.getIconForPrincipal(principal)}<span class="name">${principal.name}</span></div></td></tr>`);
		$('#new', container).after(row);

		for (let perm of ['read', 'write', 'delete', 'accessControl']) {

			row.append(`<td><input class="${perm}" type="checkbox" data-permission="${perm}"${permissions[perm] ? ' checked="checked"' : ''}"></td>`);

			$('.' + perm, row).on('dblclick', function() {
				return false;
			});

			$('.' + perm, row).on('click', function(e) {
				e.preventDefault();

				let checkbox = $(this);
				checkbox.prop('disabled', true);

				if (!$('input:checked', row).length) {

					$('#newPrincipal', container).append(_Entities.templateForPrincipalOption(principal));
					$('#newPrincipal', container).trigger('chosen:updated');

					row.remove();
				}

				let recursive = $('#recursive', container).is(':checked');

				Command.setPermission(entity.id, principal.id, permissions[perm] ? 'revoke' : 'grant', perm, recursive, () => {

					permissions[perm] = !permissions[perm];
					checkbox.prop('checked', permissions[perm]);

					checkbox.prop('disabled', false);

					blinkGreen(checkbox.parent());
				});
			});
		}

		if (allowRecursive) {

			row.append('<td><button class="apply-to-child-nodes hover:bg-gray-100 focus:border-gray-666 active:border-green">Apply to child nodes</button></td>');

			let button = row[0].querySelector('button.apply-to-child-nodes');

			button.addEventListener('click', (e) => {

				button.setAttribute('disabled', 'disabled');

				let permissions = [...row[0].querySelectorAll('input:checked')].map(i => i.dataset.permission).join(',');

				Command.setPermission(entity.id, principal.id, 'setAllowed', permissions, true, () => {

					button.removeAttribute('disabled');
					blinkGreen(row);
				});
			});
		}
	},
	appendBooleanSwitch: function(el, entity, key, label, desc, recElementId) {
		if (!el || !entity) {
			return false;
		}
		el.append(`<div class="${entity.id}_ flex items-center mt-2"><button class="switch inactive ${key}_ inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green min-w-56"></button>${desc}</div>`);

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

			let iconClasses = _Icons.getSvgIconClassesNonColorIcon(['svg_key_icon']);

			if (onlyShowWhenProtected) {
				if (entity.visibleToPublicUsers && entity.visibleToAuthenticatedUsers) {
					iconClasses.push('node-action-icon');
				}
			}

			keyIcon = $(_Icons.getSvgIcon(_Icons.getAccessControlIconId(entity), 16, 16, iconClasses));
			parent.append(keyIcon);

			_Entities.bindAccessControl(keyIcon, entity);
		}

		keyIcon[0].dataset['onlyShowWhenProtected'] = onlyShowWhenProtected;

		return keyIcon;
	},
	updateNewAccessControlIconInElement: (entity, element) => {

		let isProtected = !entity.visibleToPublicUsers || !entity.visibleToAuthenticatedUsers;

		// update svg key icon
		let svgKeyIcon = element[0].querySelector(':scope > .svg_key_icon');
		if (!svgKeyIcon) {
			svgKeyIcon = element[0].querySelector(':scope > .icons-container > .svg_key_icon');
		}
		if (!svgKeyIcon) {
			svgKeyIcon = element[0].querySelector(':scope > .actions > .svg_key_icon');
		}
		if (!svgKeyIcon) {
			svgKeyIcon = element[0].querySelector(':scope > .node-container > .icons-container > .svg_key_icon');
		}
		if (svgKeyIcon) {

			let newIconId = _Icons.getAccessControlIconId(entity);

			// replace only href to keep bindings intact
			let use = svgKeyIcon.querySelector(':scope > use');
			use.setAttribute('href', '#' + newIconId);

			if (svgKeyIcon.dataset['onlyShowWhenProtected'] === 'true') {

				if (isProtected) {
					svgKeyIcon.classList.remove('node-action-icon');
				} else {
					svgKeyIcon.classList.add('node-action-icon');
				}
			}
		}
	},
	appendContextMenuIcon: (parent, entity, visible) => {

		let contextMenuIconClass = 'context_menu_icon';
		let icon = $('.' + contextMenuIconClass, parent);

		if (!(icon && icon.length)) {
			icon = $(_Icons.getSvgIcon(_Icons.iconKebabMenu, 16, 16, _Icons.getSvgIconClassesNonColorIcon([contextMenuIconClass, 'node-action-icon'])));
			parent.append(icon);
		}

		icon.on('click', function(e) {
			e.stopPropagation();
			_Elements.activateContextMenu(e, parent, entity);
		});

		if (visible) {
			icon.css({
				visibility: 'visible',
				display: 'inline-block'
			});
		}

		return icon;
	},
	appendExpandIcon: (nodeContainer, entity, hasChildren, expanded, callback) => {

		let button = $(nodeContainer.children('.expand_icon_svg').first());
		if (button && button.length) {
			return;
		}

		if (hasChildren) {

			let typeIcon    = $(nodeContainer.children('.typeIcon').first());
			let icon        = expanded ? _Icons.expandedClass : _Icons.collapsedClass;

			typeIcon.removeClass('typeIcon-nochildren').before(`<i class="expand_icon_svg ${icon}"></i>`);

			button = $(nodeContainer.children('.expand_icon_svg').first());

			if (button) {

				button.on('click', (e) => {
					e.stopPropagation();
					_Entities.toggleElement(entity.id, nodeContainer, undefined, callback);
				});

				// Prevent expand icon from being draggable
				button.on('mousedown', (e) => {
					e.stopPropagation();
				});

				if (expanded) {
					_Entities.ensureExpanded(nodeContainer);
				}
			}

		} else {
			nodeContainer.children('.typeIcon').addClass('typeIcon-nochildren');
		}

		_Pages.registerDetailClickHandler($(nodeContainer), entity);
	},
	removeExpandIcon: function(el) {
		if (!el)
			return;

		if (el.hasClass('node')) {
			el = el.children('.node-container');
		}

		let expandIcon = $(el.children('.expand_icon_svg').first());
		expandIcon.remove();

		el.children('.typeIcon').addClass('typeIcon-nochildren');
	},
	makeSelectable: (el) => {
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
	setMouseOver: (el, allowClick, syncedNodesIds) => {

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
	isExpanded: (el) => {
		let icon = _Entities.getIconFromElement(el);
		return icon.hasClass(_Icons.expandedClass);
	},
	getIconFromElement: (el) => {

		if (el.hasClass('node-container')) {
			return el.children('.expand_icon_svg').first();
		} else {
			return el.children('.node-container').children('.expand_icon_svg').first();
		}
	},
	ensureExpanded: (element, callback, force = false) => {
		if (!element) {
			return;
		}
		let el = $(element);
		let id = Structr.getId(el);

		if (!id) {
			return;
		}

		Structr.addExpandedNode(id);

		if (force === false && _Entities.isExpanded(element)) {
			return;
		} else {
			Command.children(id, callback);

			let icon = _Entities.getIconFromElement(el);
			icon.removeClass(_Icons.collapsedClass).addClass(_Icons.expandedClass);
		}
	},
	expandAll: (ids, lastId) => {

		if (!ids || ids.length === 0) {
			return;
		}

		// top-level object
		let firstId = ids[0];
		let el      = Structr.node(firstId);

		// if top-level element is not present, we can not do anything
		if (el) {

			if (firstId === lastId) {

				// finally highlight last element
				_Entities.deselectAllElements();
				_Entities.highlightElement(el);
			}

			_Entities.ensureExpanded(el, (childNodes) => {
				_Entities.expandAll(ids.slice(1), lastId);
			}, true);
		}
	},
	expandRecursively: (ids) => {
		if (!ids || ids.length === 0) {
			return;
		}

		for (let id of ids) {

			let el = Structr.node(id);

			_Entities.ensureExpanded(el, (childNodes) => {
				if (childNodes && childNodes.length) {
					_Entities.expandRecursively(childNodes.map(n => n.id));
				}
			}, true);
		}
	},
	deselectAllElements: () => {
		for (let selectedElement of document.querySelectorAll('.nodeSelected')) {
			selectedElement.classList.remove('nodeSelected');
		}
	},
	scrollTimer: undefined,
	highlightElement: (el) => {

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
	toggleElement: function(id, nodeContainer, isExpanded, callback) {

		let el            = $(nodeContainer);
		let toggleIcon    = el.children('.expand_icon_svg').first();

		if (_Entities.isExpanded(el)) {

			$(el.closest('.node')).children('.node').remove();

			toggleIcon.removeClass(_Icons.expandedClass).addClass(_Icons.collapsedClass);

			Structr.removeExpandedNode(id);

		} else {

			if (!isExpanded) {
				Command.children(id, callback);
			}

			toggleIcon.removeClass(_Icons.collapsedClass).addClass(_Icons.expandedClass);

			Structr.addExpandedNode(id);
		}
	},
	makeAttributeEditable: function(parentElement, id, attributeSelector, attributeName, callback) {

		let attributeElement        = parentElement.find(attributeSelector).first();
		let additionalInputClass    = attributeElement.data('inputClass') || '';
		let oldValue                = $.trim(attributeElement.attr('title'));
		let attributeElementRawHTML = attributeElement[0].outerHTML;

		attributeElement.replaceWith('<input type="text" size="' + (oldValue.length + 4) + '" class="new-' + attributeName + ' ' + additionalInputClass + '" value="' + oldValue + '">');

		let input = $('input', parentElement);
		input.focus().select();

		let restoreNonEditableTag = function(el, text) {

			let newEl = $(attributeElementRawHTML);
			newEl.html(text);
			newEl.attr('title', text);
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
	isContentElement: (entity) => {
		return (entity.type === 'Template' || entity.type === 'Content');
	},
	isLinkableEntity: (entity) => {
		return (entity.tag === 'a' || entity.tag === 'link' || entity.tag === 'script' || entity.tag === 'img' || entity.tag === 'video' || entity.tag === 'object');
	},
	setPropertyWithFeedback: function(entity, key, newVal, input, blinkEl) {
		const oldVal = entity[key];
		input.val(oldVal);
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
	},

	templates: {
		events: config => `
			<h3>Event Action Mapping</h3>

			<div class="grid grid-cols-2 gap-8">

				<div class="option-tile">
					<label class="block mb-2" data-comment="Select the event type that triggers the action.">Event</label>
					<select class="select2" id="event-select">
						<option value="none">None</option>
						<option value="click">Click</option>
						<option value="change">Change</option>
						<option value="focusout">Focus out</option>
						<option value="drop">Drop</option>
						<option value="custom">Custom frontend event</option>
					</select>
				</div>

				<div class="option-tile">
					<label class="block mb-2" data-comment="Select the action that is triggered by the event.">Action</label>
					<select class="select2" id="action-select">
						<option value="none">No action</option>
						<option value="create">Create new object</option>
						<option value="update">Update object</option>
						<option value="delete">Delete object</option>
						<option value="method">Execute method</option>
						<option value="flow">Execute flow</option>
						<option value="custom">Custom action</option>
						<option value="next-page">Next page</option>
						<option value="prev-page">Previous page</option>
					</select>
				</div>

				<div class="hidden opacity-0 option-tile options-custom-event">
					<label class="block mb-2" for="custom-event-input" data-comment="Define the frontend event that triggers the action.">Frontend event</label>
					<input type="text" id="custom-event-input">
				</div>

				<div class="hidden opacity-0 option-tile options-custom-action">
					<label class="block mb-2" for="custom-action-input" data-comment="Define the backend action that is triggered by the event.">Backend action</label>
					<input type="text" id="custom-action-input">
				</div>

				<div class="option-tile option-any uuid-container-for-all-events">
					<div class="option-tile hidden event-options options-delete">
						<label class="block mb-2" for="delete-target-input" data-comment="Enter a script expression like &quot;&#36;{obj.id}&quot; that evaluates to the UUID of the data object that shall be deleted on click.">UUID of data object to delete</label>
						<input type="text" id="delete-target-input">
					</div>
					<div class="option-tile hidden event-options options-method">
						<label class="block mb-2" for="method-target-input" data-comment="Enter a script expression like &quot;&#36;{obj.id}&quot; that evaluates to the UUID of the data object the method shall be called on, or a type name for static methods.">UUID or type of data object to call method on</label>
						<input type="text" id="method-target-input">
					</div>
					<div class="option-tile hidden event-options options-custom">
						<label class="block mb-2" for="custom-target-input" data-comment="Enter a script expression like &quot;&#36;{obj.id}&quot; that evaluates to the UUID of the target data object.">UUID of action target object</label>
						<input type="text" id="custom-target-input">
					</div>
					<div class="option-tile hidden event-options options-update">
						<label class="block mb-2" for="update-target-input" data-comment="Enter a script expression like &quot;&#36;{obj.id}&quot; that evaluates to the UUID of the data object that shall be updated.">UUID of data object to update</label>
						<input type="text" id="update-target-input">
					</div>
				</div>

				<!--div class="row hidden event-options options-prev-page options-next-page">
					<div class="option-tile">
						<label class="block mb-2" for="pagination-name-input" data-comment="Define the name of the pagination request parameter (usually &quot;page&quot;).">Pagination request parameter</label>
						<input type="text" id="pagination-name-input">
					</div>
				</div-->

				<div class="row hidden event-options options-create options-update">
					<div class="option-tile">
						<label class="block mb-2" for="data-type-select" data-comment="Define the type of data object to create or update">Enter or select type of data object</label>
						<input type="text" class="combined-input-select-field" id="data-type-input" placeholder="Custom type or script expression">
						<select class="required combined-input-select-field" id="data-type-select">
							<option value="">Select type from schema</option>
						</select>
					</div>
				</div>

				<div class="row hidden event-options options-method">
					<div class="option-tile">
						<label class="block mb-2" for="method-name-input">Name of method to execute</label>
						<input type="text" id="method-name-input">
					</div>
				</div>

<!--				<div class="row hidden event-options options-update">-->
<!--					<div class="option-tile">-->
<!--						<label class="block mb-2" for="update-property-input">Name of property to update</label>-->
<!--						<input type="text" id="update-property-input">-->
<!--					</div>-->
<!--				</div>-->
				
				<div class="col-span-2 hidden event-options event-drop">
					<h3>Drag & Drop</h3>
					<div class="option-tile">
						<label class="block mb-2">The following additional configuration is required to enable drag & drop.</label>
						<ul class="mb-2">
							<li>Make other elements draggable: set the <code>draggable</code> attribute to <code>true</code>.</li>
							<li>Add a custom data attribute with the value <code>data()</code> to the drop target, e.g. <code>data-payload</code> etc.</li>
							<li>The custom data attribute will be sent to the method when a drop event occurs.</li>
						<ul>
					</div>
				</div>

				<div class="col-span-2 hidden event-options options-properties options-any">
					<h3>Parameter Mapping
						<i class="m-2 add-parameter-mapping-button cursor-pointer align-middle icon-grey icon-inactive hover:icon-active">${_Icons.getSvgIcon(_Icons.iconAdd,16,16,[], 'Add parameter')}</i>
						<i class="m-2 add-parameter-mapping-for-type-button cursor-pointer align-middle icon-grey icon-inactive hover:icon-active">${_Icons.getSvgIcon(_Icons.iconListAdd,16,16,[], 'Add parameters for all properties')}</i>
					</h3>

					<div class="event-options options-properties options-create options-update parameter-mappings-container"></div>

				</div>

				<div class="col-span-2 hidden event-options options-any">
					<h3>Follow-up Actions</h3>
					<div class="grid grid-cols-2 gap-8 event-options">

						<div class="option-tile">
							<label class="block mb-2" for="success-behaviour-select" data-comment="Define what should happen after the triggered action succeeded.">Behaviour on success</label>
							<select class="select2" id="success-behaviour-select">
								<option value="nothing">Nothing</option>
								<option value="full-page-reload">Reload the current page</option>
								<option value="partial-refresh">Refresh a section of the current page</option>
								<option value="navigate-to-url">Navigate to a new page</option>
								<option value="fire-event">Fire a custom event</option>
							</select>
						</div>
						<div class="hidden option-tile option-success option-success-partial-refresh">
							<label class="block mb-2" for="success-partial-refresh-input" data-comment="Define the area(s) of the current page that should be refreshed by its CSS ID selector (comma-separated list of CSS IDs with leading #).">Partial(s) to refresh on success</label>
							<input type="text" id="success-partial-refresh-input" placeholder="Enter a CSS ID selector">
						</div>
						<div class="hidden option-tile option-success option-success-navigate-to-url">
							<label class="block mb-2" for="success-navigate-to-url-input" data-comment="Define the relative or absolute URL of the page to load on success">Success URL</label>
							<input type="text" id="success-navigate-to-url-input" placeholder="Enter a relative or absolute URL">
						</div>
						<div class="hidden option-tile option-success option-success-fire-event">
							<label class="block mb-2" for="success-fire-event-input" data-comment="Define event that should be fired.">Event to fire on success</label>
							<input type="text" id="success-fire-event-input" placeholder="Enter an event name">
						</div>
					</div>

					<div class="grid grid-cols-2 gap-8 mt-4 event-options">
						<div class="option-tile">
							<label class="block mb-2" for="failure-behaviour-select" data-comment="Define what should happen after the triggered action failed.">Behaviour on failure</label>
							<select class="select2" id="failure-behaviour-select">
								<option value="nothing">Nothing</option>
								<option value="full-page-reload">Reload the current page</option>
								<option value="partial-refresh">Refresh a section of the current page</option>
								<option value="navigate-to-url">Navigate to a new page</option>
								<option value="fire-event">Fire a custom event</option>
							</select>
						</div>
						<div class="hidden option-tile option-failure option-failure-partial-refresh">
							<label class="block mb-2" for="failure-partial-refresh-input" data-comment="Define the area of the current page that should be refreshed by its CSS ID.">Partial to refresh on failure</label>
							<input type="text" id="failure-partial-refresh-input" placeholder="Enter a CSS ID">
						</div>
						<div class="hidden option-tile option-failure option-failure-navigate-to-url">
							<label class="block mb-2" for="failure-navigate-to-url-input" data-comment="Define the relative or absolute URL of the page to load on failure">Failure URL</label>
							<input type="text" id="failure-navigate-to-url-input" placeholder="Enter a relative or absolute URL">
						</div>
						<div class="hidden option-tile option-failure option-failure-fire-event">
							<label class="block mb-2" for="failure-fire-event-input" data-comment="Define event that should be fired.">Event to fire on failure</label>
							<input type="text" id="failure-fire-event-input" placeholder="Enter an event name">
						</div>


					</div>
				</div>
				
				<div class="option-tile col-span-2">
					<button type="button" class="action" id="save-event-mapping-button">Save</button>
				</div>

			</div>
		`,
		parameterMappingRow: config => `
			<div class="event-options options-properties options-update parameter-mapping" data-structr-id="${config.id}">
				
				<div class="grid grid-cols-5 gap-8 hidden event-options options-reload-target mb-4">
				
					<div class="option-tile">
						<label class="block mb-2" data-comment="Choose a name/key for this parameter to define how the value is sent to the backend">Parameter name</label>
						<input type="text" class="parameter-name-input" placeholder="Name" value="${config.parameterName || ''}">
					</div>

					<div class="option-tile">
						<label class="block mb-2" for="parameter-type-select" data-comment="Select the type of this parameter.">Parameter type</label>
						<select class="parameter-type-select">
							<option>-- Select --</option>						
							<option value="user-input">User Input</option>
							<option value="page-param">Request parameter for page</option>
							<option value="pagesize-param">Request parameter for page size</option>
							<option value="constant-value">Constant value</option>
							<option value="script-expression">Script expression</option>
							<option value="method-result">Result of method call</option>
							<option value="flow-result">Result of flow</option>
						</select>
					</div>
					
					<div class="hidden col-span-2 option-tile parameter-value parameter-constant-value">
						<label class="block mb-2" data-comment="Enter a constant value">Value (constant)</label>
						<input type="text" class="parameter-constant-value-input" placeholder="Constant value" value="${config.value || ''}">
					</div>

					<div class="hidden col-span-2 option-tile parameter-value parameter-script-expression">
						<label class="block mb-2" data-comment="The script expression will be evaluated and the result passed as parameter value">Value expression</label>
						<input type="text" class="parameter-script-expression-input" placeholder="Script expression" value="${config.value || ''}">
					</div>

					<div class="hidden col-span-2 option-tile parameter-value parameter-user-input">
						<label class="block mb-2" data-comment="Drag a form input element (&amp;lt;input&amp;gt;, &amp;lt;textarea&amp;gt; or &amp;lt;select&amp;gt;) and drop it here">Form input element</label>
						<input type="hidden" class="parameter-user-input-input" value="${config.value || ''}">
						<div class="element-dropzone link-existing-element-dropzone">
							<div class="info-icon h-16 flex items-center justify-center">
								<i class="m-2 active align-middle">${_Icons.getSvgIcon(_Icons.iconAdd)}</i>
								<i class="m-2 inactive align-middle">${_Icons.getSvgIcon(_Icons.iconAdd)}</i> Drag and drop existing form input element here 
							</div>
						</div>
					</div>

					<div class="hidden col-span-2 option-tile parameter-value parameter-method-result">
						<label class="block mb-2" data-comment="The method will be evaluated and the result passed as parameter value">Method</label>
						<input type="text" class="parameter-method-result-input" placeholder="Method name" value="${config.value || ''}">
					</div>

					<div class="hidden col-span-2 option-tile parameter-value parameter-flow-result">
						<label class="block mb-2" data-comment="The selected Flow will be evaluated and the result passed as parameter value">Flow result</label>
						<select class="parameter-flow-result-input">
							<option value="">-- Select flow --</option>
						</select>
					</div>
					
					<div class="option-tile">
						<label class="hidden block mb-2">Actions</label>
						<i class="block mt-4 cursor-pointer parameter-mapping-remove-button" data-structr-id="${config.id}">${_Icons.getSvgIcon(_Icons.iconTrashcan)}</i>
					</div>

				</div>
								
				<!--div class="event-options options-properties options-create options-update">
					<div id="link-existing-element-dropzone" class="element-dropzone">
						<div class="info-icon h-16 flex items-center justify-center">
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['m-2', 'active'])}
							${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['m-2', 'active'])} Drop existing input or select elements here
						</div>
					</div>
				</div>

				<div class="event-options options-properties options-update">
					<button class="inline-flex items-center add-property-input-button hover:bg-gray-100 focus:border-gray-666 active:border-green">${_Icons.getSvgIcon(_Icons.iconAdd)} Create new input</button>
					<button class="inline-flex items-center add-property-select-button hover:bg-gray-100 focus:border-gray-666 active:border-green">${_Icons.getSvgIcon(_Icons.iconAdd)} Create new select</button>
				</div-->
				
			</div>
		`,
		multipleInputsRow: config => `
			<div class="event-options options-properties options-update multiple-properties">

				<div class="grid grid-cols-5 gap-8 hidden event-options options-reload-target mb-4">
					<div class="option-tile flat">
						<label class="hidden mb-1">Name</label>
						<input type="text" class="multiple-input-name-input" placeholder="Name of input element" data-structr-id="${config.id}">
					</div>
					<div class="option-tile flat">
						<label class="hidden mb-1">Property key</label>
						<input type="text" class="multiple-input-property-key-input" placeholder="Property key to update" data-structr-id="${config.id}">
					</div>
					<div class="option-tile flat">
						<label class="hidden mb-1">CSS id</label>
						<input type="text" class="multiple-input-css-id-input" placeholder="CSS id attribute" data-structr-id="${config.id}">
					</div>
					<div class="option-tile flat">
						<label class="hidden mb-1">Value</label>
						<input type="text" class="multiple-input-value-input" placeholder="Value expression" data-structr-id="${config.id}">
					</div>
					<div class="option-tile flat">
						<label class="hidden mb-1">Actions</label>
						<i class="block mt-2 cursor-pointer multiple-input-remove-button" data-structr-id="${config.id}">${_Icons.getSvgIcon(_Icons.iconTrashcan)}</i>
					</div>
				</div>

			</div>
		`,
	}
}

function formatValueInputField(key, obj, isPassword, isReadOnly, isMultiline) {

	if (obj === undefined || obj === null) {

		return formatRegularValueField(key, '', isMultiline, isReadOnly, isPassword);

	} else if (obj.constructor === Object) {

		let displayName = _Crud.displayName(obj);
		return `<div title="${escapeForHtmlAttributes(displayName)}" id="_${obj.id}" class="node ${obj.type ? obj.type.toLowerCase() : (obj.tag ? obj.tag : 'element')} ${obj.id}_"><span class="abbr-ellipsis abbr-80">${displayName}</span>${_Icons.getSvgIcon(_Icons.iconCrossIcon, 16, 16, ['remove'])}</div>`;

	} else if (obj.constructor === Array) {

		return formatArrayValueField(key, obj, isMultiline, isReadOnly, isPassword);

	} else {

		return formatRegularValueField(key, escapeForHtmlAttributes(obj), isMultiline, isReadOnly, isPassword);
	}
}

function formatArrayValueField(key, values, isMultiline, isReadOnly, isPassword) {

	let html           = '';
	let readonlyHTML   = (isReadOnly ? ' readonly class="readonly"' : '');
	let inputTypeHTML  = (isPassword ? 'password' : 'text');
	let removeIconHTML = _Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['remove', 'icon-lightgrey']), 'Remove single value');

	for (let value of values) {

		if (isMultiline) {

			html += `<div class="array-attr relative"><textarea rows="4" name="${key}"${readonlyHTML} autocomplete="new-password">${value}</textarea>${removeIconHTML}</div>`;

		} else {

			html += `<div class="array-attr relative"><input name="${key}" type="${inputTypeHTML}" value="${value}"${readonlyHTML} autocomplete="new-password">${removeIconHTML}</div>`;
		}
	}

	if (isMultiline) {

		html += `<div class="array-attr"><textarea rows="4" name="${key}"${readonlyHTML} autocomplete="new-password"></textarea></div>`;

	} else {

		html += `<div class="array-attr"><input name="${key}" type="${inputTypeHTML}" value=""${readonlyHTML} autocomplete="new-password"></div>`;
	}

	return html;
}

function formatRegularValueField(key, value, isMultiline, isReadOnly, isPassword) {

	if (isMultiline) {

		return `<textarea rows="4" name="${key}"${isReadOnly ? ' readonly class="readonly"' : ''} autocomplete="new-password">${value}</textarea>`;

	} else {

		return `<input name="${key}" type="${isPassword ? 'password' : 'text'}" value="${value}"${isReadOnly ? 'readonly class="readonly"' : ''} autocomplete="new-password">`;
	}
}
