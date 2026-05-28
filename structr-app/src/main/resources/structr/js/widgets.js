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
let _Widgets = {
	localWidgetsEl: undefined,

	getContextMenuElements: (div, entity) => {

		let elements = [];

		elements.push({
			icon: _Icons.getMenuSvgIcon(_Icons.iconPencilEdit),
			name: 'Edit',
			clickHandler: () => {

				Command.get(entity.id, 'id,type,name,source,configuration,description', (entity) => {
					_Widgets.editWidget(entity);
				});
			}
		});

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		elements.push({
			name: 'Advanced',
			clickHandler: () => {
				_Entities.showProperties(entity, 'ui');
			}
		});

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		elements.push({
			icon: _Icons.getMenuSvgIcon(_Icons.iconTrashcan),
			classes: ['menu-bolder', 'danger'],
			name: 'Delete Widget',
			clickHandler: () => {
				_Entities.deleteNode(entity);
			}
		});

		_Elements.contextMenu.appendContextMenuSeparator(elements);

		return elements;
	},
	reloadWidgets: () => {

		_Helpers.fastRemoveAllChildren(_Pages.widgetsSlideout[0]);

		_Pages.widgetsSlideout.append(_Widgets.templates.slideout());

		_Widgets.localWidgetsEl = $('#widgets', _Pages.widgetsSlideout);

		_Pages.widgetsSlideout[0].querySelector('.add_widgets_icon').addEventListener('click', (e) => {
			e.preventDefault();
			Command.create({ type: 'Widget' }, (widget) => {
				_Widgets.editWidget(widget);
			});
		});

		// _Widgets.localWidgetsEl.droppable({
		// 	drop: function(e, ui) {
		// 		e.preventDefault();
		// 		e.stopPropagation();
		// 		_Elements.dropBlocked = true;
		// 		let sourceId = Structr.getId($(ui.draggable));
		//
		// 		// Drop widget from local DOM element
		//
		// 		fetch(`${Structr.viewRootUrl}${sourceId}?${Structr.getRequestParameterName('edit')}=1`).then(async response => {
		//
		// 			if (response.ok) {
		//
		// 				let text = await response.text();
		//
		// 				Command.createLocalWidget(sourceId, `New Widget (${sourceId})`, text, (entity) => {
		// 					_Elements.dropBlocked = false;
		// 				});
		// 			}
		// 		});
		// 	}
		// });

		_Pager.initPager('local-widgets', 'Widget', 1, 1000, 'treePath,name', 'asc');
		let _wPager = _Pager.addPager('local-widgets', _Widgets.localWidgetsEl[0], true, 'Widget', 'public', (entities) => {
			if (entities.length === 0) {
				let container = document.createElement('div');
				container.classList.add('flex', 'items-center', 'justify-center', 'h-full', 'text-gray-999', 'mt-8');
				_Widgets.localWidgetsEl.append(container);
				_Widgets.createImportWidgetsButton(container, 'Import Widget Set', () => {
					_Widgets.reloadWidgets();
				});
			} else {
				for (let entity of entities) {
					StructrModel.create(entity, null, false);
					_Widgets.appendWidgetElement(entity);
				}
			}
		}, undefined, undefined, true);

		_wPager.appendFilterElements('<span style="white-space: nowrap;">Filter: <input type="text" class="filter" data-attribute="name"></span>');
		_wPager.activateFilterElements();
		_wPager.setIsPaused(false);
		_wPager.refresh();
	},
	createImportWidgetsButton: (container, title, callback) => {
		let importTile = _Helpers.createSingleDOMElementFromHTML(
			`<div id="import-widget-set" class="page-tile bg-gray-f8"><div class="h-full flex flex-column items-center" title="${title}">
							<div class="text-gray-555">
								<div class="text-center mt-4">${_Icons.getSvgIcon(_Icons.iconAdd, 24, 24, '', title)}</div>
								<div class="text-center mt-4 px-4">Click here to import the default widget set from structr.com.</div>
							</div>
						</div></div>`
		);
		container.append(importTile);

		let importWidgetsButton = container.querySelector('#import-widget-set');
		importWidgetsButton.addEventListener('click', () => {
			let formData = new FormData();
			formData.append('downloadUrl', 'https://gitlab.structr.com/structr/widgets/-/archive/0.0.4/widgets-0.0.4.zip');
			formData.append('mode', 'app'); // mode "app" implies "quiet mode", i.e. no notifications
			fetch(`${Structr.deployRoot}`, {
				method: 'POST',
				body: formData
			}).then(response => {
				if (response.ok) {
					if (callback && typeof callback === 'function') {
						callback();
					}
				}
			})
		});
	},
	getTreeParent: (element, treePath, suffix) => {

		let parent     = element;
		let title      = treePath ? treePath.split('/').pop() : 'Uncategorized';
		let lowerTitle = title.replace(/\W/g, '').toLowerCase();
		let idString   = lowerTitle + suffix;
		let newParent  = $('#' + idString + '_folder');

		if (newParent.length === 0) {
			_Widgets.appendFolderElement(parent, idString, title);
			newParent = $(`#${idString}_folder`);
		}

		parent = newParent;

		return parent;
	},
	appendFolderElement: (parent, id, name) => {

		parent.append(`
			<div class="relative mt-1 mb-1">
				<div class="absolute inset-0 flex items-center" aria-hidden="true">
					<div class="w-full" style="border-top: 1px solid #ddd;"></div>
				</div>
				<div class="relative flex justify-center">
					<span class="bg-white px-3 text-lg font-medium text-gray-500">${name}</span>
				</div>
			</div>
			<div id="${id}_folder" class="widget-folder"></div>
		`);

		let div = $(`#${id}_folder`);

		_Widgets.appendVisualExpandIcon(div.children('.node-container'), id, name, true, false);
	},
	appendWidgetElement: (widget) => {

		let parent = _Widgets.getTreeParent(_Widgets.localWidgetsEl, widget.treePath, '_local');
		let div    = Structr.node(widget.id);

		if (!div) {

			let widgetElement = $(`
				<div id="id_${widget.id}" class="widget p-2 hover:icon-active" draggable="true">
					<img style="width: 24px;${!widget.svgIconPath ? 'opacity: 0.5;' : ''}" src="${widget.svgIconPath ?? '/structr/icon/streamlinehq-website-build-programing-apps-websites.svg'}" draggable="false">
					<span class="name_ flex-grow mt-4"></span>
				</div>
			`);

			let nameElement         = widgetElement[0].querySelector('.name_');
			nameElement.textContent = widget.name;
			nameElement.title       = widget.name;

			parent.append(widgetElement);

			div = Structr.node(widget.id);
		}

		_Dragndrop.enableDraggable(widget, div[0], _Dragndrop.dropActions.widget, false);

		div.children('.name_').off('click').on('click', (e) => {
			e.stopPropagation();
			_Entities.makeAttributeEditable(div, widget.id, '.name_', 'name', (el) => {
				_Helpers.blinkGreen(el);
			});
		});

		_Elements.contextMenu.enableContextMenuOnElement(div[0], widget);

		return div;
	},
	editWidget: (entity) => {

		let { dialogText } = _Dialogs.custom.openDialog(`Edit widget "${entity.name}"`, null, ['popup-dialog-with-editor']);

		dialogText.insertAdjacentHTML('beforeend', `
			<div class="widgets-tabs flex flex-col h-full overflow-hidden">
				<ul id="widget-dialog-tabs" class="flex-shrink-0">
					<li data-name="source">Source</li>
					<li data-name="config">Configuration</li>
					<li data-name="description">Description</li>
					<li data-name="selectors">Options</li>
					<li data-name="help">Help</li>
				</ul>
				<div class="widget-tab-content flex-grow" id="tabView-source"><div class="editor h-full overflow-hidden"></div></div>
				<div class="widget-tab-content flex-grow" id="tabView-config"><div class="editor h-full overflow-hidden"></div></div>
				<div class="widget-tab-content flex-grow" id="tabView-description"><div class="editor h-full overflow-hidden"></div></div>
				<div class="widget-tab-content" id="tabView-selectors"></div>
				<div class="widget-tab-content overflow-y-auto" id="tabView-help">${_Widgets.templates.help()}</div>
			</div>
		`);

		let ul = $('ul', $(dialogText));

		let activateTab = (tabName) => {
			$('.widget-tab-content', $(dialogText)).hide();
			$('li', ul).removeClass('active');
			$(`#tabView-${tabName}`, $(dialogText)).show();
			$(`li[data-name="${tabName}"]`, ul).addClass('active');

			Structr.resize();
			_Editors.resizeVisibleEditors();
		};

		$('li', ul).on('click', function(e) {
			activateTab($(this).data('name'));
		});

		let changes = {};
		let widgetChanged = () => {
			let changed = false;
			for (let propertyName in changes) {
				changed = changed || changes[propertyName];
			}
			return changed;
		};

		let dialogSaveButton = _Dialogs.custom.updateOrCreateDialogSaveButton();
		let saveAndClose     = _Dialogs.custom.updateOrCreateDialogSaveAndCloseButton();

		let editorChangeHandler = (editor, origEntity, propertyName) => {

			changes[propertyName] = ((entity[propertyName] || '') !== editor.getValue());

			_Helpers.disableElements(!widgetChanged(), dialogSaveButton, saveAndClose);
		};

		let baseEditorConfig = {
			readOnly: false,
			changeFn: editorChangeHandler
		};

		let sourceEditor      = _Editors.getMonacoEditor(entity, 'source',        dialogText.querySelector('#tabView-source .editor'),      Object.assign({}, baseEditorConfig, { language: 'text/html', forceAllowAutoComplete: true }));
		let configEditor      = _Editors.getMonacoEditor(entity, 'configuration', dialogText.querySelector('#tabView-config .editor'),      Object.assign({}, baseEditorConfig, { language: 'application/json' }));
		let descriptionEditor = _Editors.getMonacoEditor(entity, 'description',   dialogText.querySelector('#tabView-description .editor'), Object.assign({}, baseEditorConfig, { language: 'text/html' }));

		// allow editing of selectors property
		_Schema.caches.getTypeInfo(entity.type, (typeInfo) => {
			_Entities.listProperties(entity, 'editWidget', $('#tabView-selectors'), typeInfo);
		});

		let saveWidgetFunction = (closeAfterSave) => {

			let widgetData = {
				source:        sourceEditor.getValue(),
				configuration: configEditor.getValue(),
				description:   descriptionEditor.getValue()
			};

			try {

				if (widgetData.configuration) {
					JSON.parse(widgetData.configuration);
				}

				Command.setProperties(entity.id, widgetData, () => {

					_Dialogs.custom.showAndHideInfoBoxMessage('Widget saved.', 'success', 2000, 200);

					if (closeAfterSave) {

						_Dialogs.custom.clickDialogCancelButton();

					} else {

						let modelObj = StructrModel.obj(entity.id);
						modelObj.source        = widgetData.source;
						modelObj.configuration = widgetData.configuration;
						modelObj.description   = widgetData.description;
						entity.source          = widgetData.source;
						entity.configuration   = widgetData.configuration;
						entity.description     = widgetData.description;

						changes = {};

						_Helpers.disableElements(!widgetChanged(), dialogSaveButton, saveAndClose);
					}
				});

			} catch (e) {
				activateTab('config');
				alert('Configuration is not valid JSON - please review, otherwise the widget configuration dialog will not function correctly');
			}
		};

		saveAndClose.addEventListener('click', () => {
			saveWidgetFunction(true);
		});

		dialogSaveButton.addEventListener('click', () => {
			saveWidgetFunction(false);
		});

		activateTab('source');
	},
	appendVisualExpandIcon: function(el, id, name, hasChildren, expand) {

		if (hasChildren) {

			let typeIcon            = $(el.children('.typeIcon').first());
			let icon                = $(el).children('.node').hasClass('hidden') ? _Icons.collapsedClass : _Icons.expandedClass;
			let expandIconClassName = 'expand_icon_svg';

			typeIcon.before(`<i class="${expandIconClassName} ${icon}"></i>`);

			let expandIcon = el.children('.' + expandIconClassName).first();

			let expandClickHandler = (e) => {

				e.stopPropagation();

				let childNodes = el.parent().children('.node');

				childNodes.toggleClass('hidden');

				let isCollapsed = childNodes.hasClass('hidden');
				if (isCollapsed) {
					Structr.addExpandedNode(id);
					expandIcon.removeClass(_Icons.expandedClass).addClass(_Icons.collapsedClass);
				} else {
					Structr.removeExpandedNode(id);
					expandIcon.removeClass(_Icons.collapsedClass).addClass(_Icons.expandedClass);
				}
			};

			$(el).on('click', expandClickHandler);

			let button = $(el.children('.' + expandIconClassName).first());

			if (button) {
				button.on('click', expandClickHandler);
			}

		} else {

			el.children('.typeIcon').css({
				paddingRight: '11px'
			});
		}
	},
	insertWidgetIntoPage: (entity, target, pageId, callback) => {

		let widget = StructrModel.obj(entity.id);
		let processDeploymentInfo = false;
		let config = {
			componentType: widget.componentType,
			dimensions: widget.dimensions,
		};

		if (widget.configuration) {

			processDeploymentInfo = widget.configuration.processDeploymentInfo;

			_Widgets.showWidgetConfigurationDialog(widget, (attrs) => {
				Command.appendWidget(widget.source, target.id, pageId, null, attrs, config, processDeploymentInfo, callback);
			});

		} else {

			Command.appendWidget(widget.source, target.id, pageId, null, {}, config, processDeploymentInfo, callback);
		}
	},
	wrapElementInWidget: (entity, target, pageId, callback) => {

		let widget = StructrModel.obj(entity.id);
		let processDeploymentInfo = false;
		let config = {
			componentType: widget.componentType,
			dimensions: widget.dimensions,
		};

		if (widget.configuration) {

			processDeploymentInfo = widget.configuration.processDeploymentInfo;

			_Widgets.showWidgetConfigurationDialog(widget, (attrs) => {
				Command.wrapInWidget(widget.source, target.id, pageId, null, attrs, config, processDeploymentInfo, callback);
			});

		} else {

			Command.wrapInWidget(widget.source, target.id, pageId, null, {}, config, processDeploymentInfo, callback);
		}
	},
	replaceElementWithWidget: (entity, target, pageId, callback) => {

		let widget = StructrModel.obj(entity.id);
		let processDeploymentInfo = false;
		let config = {
			componentType: widget.componentType,
			dimensions: widget.dimensions,
		};

		if (widget.configuration) {

			processDeploymentInfo = widget.configuration.processDeploymentInfo;

			_Widgets.showWidgetConfigurationDialog(widget, (attrs) => {
				Command.replaceWidget(widget.source, target.id, pageId, null, attrs, config, processDeploymentInfo, callback);
			});

		} else {

			Command.replaceWidget(widget.source, target.id, pageId, null, {}, config, processDeploymentInfo, callback);
		}
	},
	showWidgetConfigurationDialog: async (widget, callback) => {

		let widgetDescription = widget.description;
		let widgetConfig      = widget.configuration;

		if (widgetConfig) {
			try {
				widgetConfig = JSON.parse(widgetConfig);
			} catch (e) {
				new ErrorMessage().text("Cannot parse Widget configuration").show();
				return;
			}
		}

		let { dialogText } = _Dialogs.custom.openDialog('Insert Widget', undefined, ['insert-widget-dialog']);
		let appendWidgetButton = _Dialogs.custom.appendCustomDialogButton('<button id="appendWidget" class="action">Append Widget</button>');

		if ((widgetDescription === null || widgetDescription.trim() === "")) {
			widgetDescription = ''
		}

		if (widgetDescription.length) {

			dialogText.insertAdjacentHTML('beforeend', `
						<h3>Description</h3>
						<p>${widgetDescription}</p>
					`);
		}

		dialogText.insertAdjacentHTML('beforeend', `
						<h3>Settings</h3>
						<p>Please select values for the following settings before inserting the widget.</p>
						<form id="widget-form"><div class="widget-props grid grid-cols-3 gap-8"></div></formi>
					`);

		let form = $('div', $(dialogText));
		let formElement = document.querySelector('#widget-form');

		let updateButtonState = () => {
			if (formElement.checkValidity()) {
				appendWidgetButton.classList.remove('disabled');
				appendWidgetButton.disabled = false;
			} else {
				appendWidgetButton.disabled = true;
				appendWidgetButton.classList.add('disabled');
			}
		};

		let sortedWidgetConfig = _Widgets.sortWidgetConfigurationByPosition(widgetConfig);

		for (let configElement of sortedWidgetConfig) {

			let label = configElement[0];
			if (label === 'processDeploymentInfo') {
				return;
			}

			let cleanedLabel = label.replace(/[^\w]/g, '_');
			let fieldConfig  = configElement[1];
			let fieldType    = fieldConfig.type;
			let defaultValue = fieldConfig.default || '';
			let titleLabel   = fieldConfig.title || label;
			let placeholder  = fieldConfig.placeholder || titleLabel;

			switch (fieldType) {

				case 'datasource':
					form.append(await _Widgets.templates.dataSourcesInput({ cleanedLabel, titleLabel, label, defaultValue, titleComment: 'Select the data source for this component.' }));
					form.append(
						`<div id="new-schema-node-name-input" class="hidden">
							<h4>New Type</h4>
							<input required class="form-field validated" type="text" name="${cleanedLabel}Name" data-key="${cleanedLabel}Name" placeholder="Enter a name for the new type.."/>
							<label class="block mx-2 mt-4"><input type="checkbox" class="form-field" name="${cleanedLabel}CreateExampleData" data-key="${cleanedLabel}CreateExampleData">Create example data</label>
						</div>`
					);
					form.append(
						`<div id="new-data-source-name-input" class="hidden">
							<h4>New Data Source</h4>
							<input required class="form-field validated" type="text" name="${cleanedLabel}Name" data-key="${cleanedLabel}Name" placeholder="Enter a name for the new data source.."/>
						</div>`
					);
					form.append(
						`<div id="new-schema-node-attributes" class="hidden">
							<h4>Attributes</h4>
							<div id="schema-node-attribute-container" class="w-full">
								<button type="button" class="w-full bg-gray-f8 flex items-center justify-center" id="add-schema-node-attribute-button">
									<svg class="mr-2" width="12" height="12">
										<use href="#circle_plus"></use>
									</svg>Add attribute
								</button>
							</div>
						</div>`
					);
					let input           = document.querySelector(`#${cleanedLabel}`);
					let sourceNameDiv   = document.querySelector('#new-data-source-name-input');
					let schemaNameDiv   = document.querySelector('#new-schema-node-name-input');
					let attributesDiv   = document.querySelector('#new-schema-node-attributes');
					let sourceNameInput = sourceNameDiv.querySelector('input');
					let schemaNameInput = schemaNameDiv.querySelector('input');
					input.dispatchEvent(new CustomEvent('change', {}));
					input.addEventListener('change', async (e) => {
						sourceNameDiv.classList.add('hidden');
						schemaNameDiv.classList.add('hidden');
						sourceNameInput.classList.remove('form-field')
						schemaNameInput.classList.remove('form-field')
						sourceNameInput.type = 'hidden'; // hidden fields are not validated
						schemaNameInput.type = 'hidden';
						delete widgetConfig.dataSourceName;
						let value = e?.target?.value;
						if (value === 'create:schema') {
							schemaNameInput.classList.add('form-field')
							schemaNameInput.type = 'text';
							schemaNameDiv.classList.remove('hidden');
							attributesDiv.classList.remove('hidden');
							widgetConfig.dataSourceName = 'input';
							widgetConfig.dataSourceCreateExampleData = 'input';
						} else if (value === 'create:datasource') {
							sourceNameInput.classList.add('form-field')
							sourceNameInput.type = 'text';
							sourceNameDiv.classList.remove('hidden');
							attributesDiv.classList.add('hidden');
							widgetConfig.dataSourceName = 'input';
						}
					});
					schemaNameInput.addEventListener('keyup', _Helpers.debounce(async (e) => {
						let result = await fetch(`${Structr.rootUrl}SchemaNode/checkValidity`, {
							method: 'POST',
							body: JSON.stringify({
								name: encodeURI(e.target.value)
							})
						});
						let data = await result.json();
						if (data?.result?.length) {
							e.target.setCustomValidity(data.result);
						} else {
							e.target.setCustomValidity('');
						}
						updateButtonState();

					}, 300));
					document.querySelector('#add-schema-node-attribute-button').addEventListener('click', async () => {
						let attributeContainer = document.querySelector('#schema-node-attribute-container');
						let index = attributeContainer.children.length - 1;
						let nameAttributeName = cleanedLabel + 'Attribute' + index;
						let typeAttributeName = cleanedLabel + 'Type' + index;
						let attributeDiv = document.createElement('div');
						let exampleValues = await _Widgets.templates.getExampleValuesForType('attribute-names', 'name-type-list', schemaNameInput.value);
						attributeDiv.classList.add('mt-1', 'grid', 'grid-cols-7', 'gap-4');
						attributeDiv.innerHTML = `
							<input required class="form-field col-span-3 validated" type="text" name="${nameAttributeName}" data-key="${nameAttributeName}" placeholder="Enter name.." value="${exampleValues?.[index]?.name || ''}"/>
							<select required class="form-field col-span-3" type="text" name="${typeAttributeName}" data-key="${typeAttributeName}">
								<option ${exampleValues?.[index]?.type === 'boolean' ? 'selected' : ''}>Boolean</option>
								<option ${exampleValues?.[index]?.type === 'date' ? 'selected' : ''}>Date</option>
								<option ${exampleValues?.[index]?.type === 'float' ? 'selected' : ''}>Float</option>
								<option ${exampleValues?.[index]?.type === 'integer' ? 'selected' : ''}>Integer</option>
								<option ${(exampleValues?.[index]?.type === 'string' || !exampleValues?.[index]) ? 'selected' : ''}>String</option>
							</select>
							<div class="flex items-center justify-end text-red"><svg width="16" height="16"><title>Remove attribute</title><use href="#trashcan"></use></svg></div>
							`;
						attributeContainer.appendChild(attributeDiv);
						widgetConfig[nameAttributeName] = 'input';
						widgetConfig[typeAttributeName] = 'input';
						updateButtonState();
						let removeButton = attributeDiv.querySelector('svg');
						removeButton.addEventListener('click', () => {
							attributeDiv.remove();
							delete widgetConfig[nameAttributeName];
							delete widgetConfig[typeAttributeName];
							updateButtonState();
						});
						attributeDiv.querySelector('input').addEventListener('keyup', _Helpers.debounce(updateButtonState, 300));
						attributeDiv.querySelector('select').addEventListener('change', updateButtonState);
					});
					break;

				case 'fields':
					form.append(`<div><h4 id="label-${cleanedLabel}">${titleLabel}</h4><select required id="${cleanedLabel}" class="form-field" data-key="${label}"></select></div>`);
					{
						let typeSelect = document.querySelector('select[data-info="select-type"]');
						if (typeSelect) {
							typeSelect.addEventListener('change', async (e) => {
								let id = typeSelect.value;
								Command.get(id, 'id,type,name,mapping', (info) => {
									let s = document.querySelector(`select#${cleanedLabel}`);
									let fields = JSON.parse(info.mapping); // right now it's JSON...
									s.insertAdjacentHTML('beforeend', _Widgets.templates.getOptionsAsText(Object.keys(fields).sort(), 'default'));
									s.dispatchEvent(new CustomEvent('change', {}));
								});
							});
						} else {
							console.log('No typeselect');
						}
					}
					break;

				case 'schema-type':
					let types = await _Schema.caches.getFilteredSchemaTypes(t => !t.isBuiltin);
					types = types.map(t => t.name);
					form.append(`<div><h4 id="label-${cleanedLabel}">${titleLabel}</h4><select required data-info="select-type" id="${cleanedLabel}" class="form-field" data-key="${label}"><option value="">--- Select type ---</option>${_Widgets.templates.getOptionsAsText(types, defaultValue)}</select></div>`);
					break;

				case 'schema-property':
					form.append(`<div><h4 id="label-${cleanedLabel}">${titleLabel}</h4><select required id="${cleanedLabel}" class="form-field" data-key="${label}"></select></div>`);
					{
						let typeSelect = document.querySelector('select[data-info="select-type"]');
						if (typeSelect) {
							typeSelect.addEventListener('change', async (e) => {
								let id = typeSelect.value;
								Command.get(id, 'id,type,name,keys', (info) => {
									let s = document.querySelector(`select#${cleanedLabel}`);
									s.insertAdjacentHTML('beforeend', _Widgets.templates.getOptionsAsText(Object.keys(info.keys).sort(), 'name'));
									s.dispatchEvent(new CustomEvent('change', {}));
								});
							});
						} else {
							console.log('No typeselect');
						}
					}
					break;

				case 'schema-method':
					form.append(`<div><h4 id="label-${cleanedLabel}">${titleLabel}</h4><select required id="${cleanedLabel}" class="form-field" data-key="${label}"></select></div>`);
					{
						let typeSelect = document.querySelector('select[data-info="select-type"]');
						if (typeSelect) {
							typeSelect.addEventListener('change', async (e) => {
								Command.getTypeInfo(typeSelect.value, (types) => {
									for (let info of types) {
										let s = document.querySelector(`select#${cleanedLabel}`);
										s.insertAdjacentHTML('beforeend', _Widgets.templates.getOptionsAsText(info.schemaMethods.map(v => v.name).sort(), 'public'));
										s.dispatchEvent(new CustomEvent('change', {}));
									}
								});
							});
						}
					}
					break;

				case 'select':
					let options = fieldConfig.options || ["-"];

					let buffer = `<div><h4 id="label-${cleanedLabel}">${titleLabel}</h4><select required id="${cleanedLabel}" class="form-field" data-key="${label}">`;
					let delayedAppendFunction;

					if (fieldConfig.dynamicOptionsFunction) {

						let dynamicOptionsFunction = new Function("callback", fieldConfig.dynamicOptionsFunction);

						let delayedAppendOptions = function (options) {
							delayedAppendFunction = new function() {
								$(`select#${cleanedLabel}`).append(_Widgets.templates.getOptionsAsText(options, defaultValue));
							};
						};

						dynamicOptionsFunction(delayedAppendOptions);

					} else {

						buffer += _Widgets.templates.getOptionsAsText(options, defaultValue);
					}

					buffer += '</select></div>';

					form.append(buffer);
					if (delayedAppendFunction) {
						delayedAppendFunction();
					}
					break;

				case 'textarea':
					let rows = (fieldConfig.rows ? parseInt(fieldConfig.rows) || 5 : 5);
					form.append(`<div><h4 id="label-${cleanedLabel}">${titleLabel}</h4><textarea required rows=${rows} class="form-field" id="${label}" placeholder="${placeholder}" data-key="${label}">${defaultValue}</textarea></div>`);
					break;

				case 'input':
				default:
					form.append(`<div><h4 id="label-${cleanedLabel}">${titleLabel}</h4><input required class="form-field" type="text" id="${label}" placeholder="${placeholder}" data-key="${label}" value="${defaultValue}"></div>`);
			}

			if (fieldConfig.help) {
				_Helpers.appendInfoTextToElement({
					text: fieldConfig.help,
					element: $(`#label-${cleanedLabel}`)
				});
			}
		}

		// disable button until selections are made
		appendWidgetButton.disabled = true;
		appendWidgetButton.classList.add('disabled');

		for (let e of formElement.elements) {
			e.addEventListener('change', updateButtonState);
			e.addEventListener('input', _Helpers.debounce(updateButtonState, 500));
		}

		appendWidgetButton.addEventListener('click', (e) => {

			e.stopPropagation();

			let attrs = {};

			for (let field of form[0].querySelectorAll('.form-field')) {
				let key = field.dataset['key'];
				if (widgetConfig[key]) {
					attrs[key] = field.value;
				}
			}

			// async callback
			if (callback && typeof callback === 'function') {
				callback(attrs);
			}

			_Dialogs.custom.clickDialogCancelButton();
		});

		_Helpers.activateCommentsInElement(dialogText, { helpElementCss: { 'font-size': '13px'} });

		// update button state initially, just in case the form is already valid..
		updateButtonState();
	},
	sortWidgetConfigurationByPosition: (config) => {

		let entries = Object.entries(config);
		entries.sort((a, b) => {
			return a[1].position - b[1].position;
		})

		return entries;
	},
	fetchLocalPageTemplateWidgets: async () => {

		try {
			let response = await fetch(`${Structr.rootUrl}Widget?isPageTemplate=true&${Structr.getRequestParameterName('sort')}=name`);
			if (response && response.ok) {

				let json = await response.json();
				return json.result;
			}

		} catch (e) {}

		return [];
	},
	fetchAllPageTemplateWidgets: async () => {
		return await _Widgets.fetchLocalPageTemplateWidgets();
	},

	sortables: {
		enableDragSortForDetailsSummary: (container, callback) => {
			let dragged = null;
			const indicator = document.createElement('div');
			indicator.style.cssText = 'height:4px; background:var(--structr-light-green);pointer-events:none;';

			for (const child of container.children) {
				const handle = document.createElement('span');
				handle.textContent = '⠿';
				handle.style.cssText = 'cursor: grab; flex-grow: 0; margin-top: 2px; margin-left: 1rem;';
				let summary = child.querySelector('summary');
				summary.appendChild(handle);
				handle.setAttribute('draggable', 'true');
				handle.addEventListener('dragstart', (e) => {
					dragged = child;
					const rect = summary.getBoundingClientRect();
					summary.style.backgroundColor = 'var(--very-light-structr-green-2)';
					summary.style.borderRadius = '.25rem';
					summary.style.border = '1px solid #ddd';
					summary.style.opacity = '0.4';
					e.dataTransfer.setDragImage(summary, rect.width - 10, rect.height / 2);
				});
				handle.addEventListener('dragend', () => {
					summary.style.backgroundColor = 'transparent';
					summary.style.border = 'none';
					summary.style.opacity = '';
					dragged = null;
					indicator.remove();
				});
			}

			function getDropTarget(y) {
				for (const child of [...container.children]) {
					if (child === indicator || child === dragged) continue;
					const rect = child.getBoundingClientRect();
					if (y < rect.top + rect.height / 2) return child;
				}
				return null;
			}

			container.addEventListener('dragover', e => {
				e.preventDefault();
				if (!dragged) return;
				const before = getDropTarget(e.clientY);
				before ? container.insertBefore(indicator, before) : container.appendChild(indicator);
			});

			container.addEventListener('dragleave', e => {
				if (!container.contains(e.relatedTarget)) indicator.remove();
			});

			container.addEventListener('drop', e => {
				e.preventDefault();
				if (!dragged) return;
				indicator.remove();
				const before = getDropTarget(e.clientY);
				before ? container.insertBefore(dragged, before) : container.appendChild(dragged);
				if (callback && typeof callback === 'function') {
					callback();
				}
			});
		}
	},

	templates: {
		slideout: config => `
			${_Icons.getSvgIcon(_Icons.iconAdd, 20, 20, _Icons.getSvgIconClassesNonColorIcon(['add_widgets_icon']), 'Create New Widget')}
			<div class="inner">
				<div id="widgets" class="mt-9"></div>
				</div>
			</div>
		`,
		help: config => `
			<h2>Source</h2>
			<p>The source HTML code of the widget (enriched with structr expressions etc).</p>
			<p>The easiest way to get this source is to build the functionality in a Structr page and then "exporting" the source of the page. This can be done by using the "edit=1" URL parameter. This way the structr-internal expressions and configuration attributes are output without being evaluated.</p>
			<h4>Example</h4>
			<ol>
				<li>Create your widget in the page "myWidgetPage"</li>
				<li>Go to http://localhost:8082/myWidgetPage?edit=1</li>
				<li>View and copy the source code of that page</li>
				<li>Paste it into the "Source" tab of the "Edit Widget" dialog</li>
			</ol>

			<h2>Configuration</h2>
			<p>You can create advanced widgets and make them configurable by inserting template expressions in the widget source and adding the expression into the configuration. Template expressions look like this "[configSwitch]" and can contain any characters (except the closing bracket). If a corresponding entry is found in the configuration, a dialog is displayed when adding the widget to a page.</p>
			<p>Elements that look like template expressions are only treated as such if a corresponding entry is found in the configuration. This allows the use of square brackets in the widget source without it being interpreted as a template expression.</p>
			<p>The configuration must be a valid JSON string (and is validated as such when trying to save the widget).</p>
			<p>Have a look at the widget configuration of "configurable" widgets for more examples.</p>

			<h4>Basic example</h4>
			<pre>
{
	"configSwitch": {
		"position": 2,
		"default": "This is the default text"
	},
	"selectArray": {
		"position": 3,
		"type": "select",
		"options": [
			"choice_one",
			"choice_two",
			"choice_three"
		],
		"default": "choice_two"
	},
	"selectObject": {
		"position": 1,
		"type": "select",
		"options": {
			"choice_one": "First choice",
			"choice_two": "Second choice",
			"choice_three": "Third choice"
		},
		"default": "choice_two"
	},
	"processDeploymentInfo": true,
}
			</pre>

			<h3>Reserved top-level words</h3>
			<dl>
				<dt class="font-bold">processDeploymentInfo</dt>
				<dd>(<i>boolean, default: false</i>)<br>Special configuration flag which allows the widgets to contain deployment annotations.</dd>
			</dl>
			
			<h3>Supported configuration attributes</h3>
			<dl>
				<dt class="font-bold">title</dt>
				<dd>The title which is displayed in the left column of the "Add Widget to Page" dialog. If this value does not exist, the name of the template expression itself is used.</dd>
				
				<dt class="font-bold">placeholder</dt>
				<dd> <i>(only applicable to type=input|textarea)</i><br>The placeholder text which is displayed when the field is empty. If this value does not exist, the <b>title</b> is used.</dd>
				
				<dt class="font-bold">default</dt>
				<dd>The default value for the element. For type=textarea|input this value is the prefilled. For type=select this value is preselected.</dd>
				
				<dt class="font-bold">position</dt>
				<dd>The options will be sorted according to this numeric attribute. If omitted, the object will occur after the objects with a set position in the natural order of the keys.</dd>
				
				<dt class="font-bold">help</dt>
				<dd><i>(optional)</i><br> The help text which will be displayed while hovering over the information icon.</dd>
				
				<dt class="font-bold">type</dt>
				<dd>
					<ul>
						<li><b>input</b>: A standard input field (<i>default if omitted</i>)</li>
						<li><b>textarea</b>: A textarea with a customizable number of rows (default: 5)</li>
						<li><b>select</b>: A select element</li>
					</ul>
				</dd>
				
				<dt class="font-bold">options</dt>
				<dd><i>(only applicable to type=select)</i><br>This field supports two different type of data: Array (of strings) and Object (value=&gt;Label).<br>
					If the data encountered is an Array, the elements are rendered as simple option elements. If it is an Object, the option elements will have the key of the object as their value and the value of the element will be displayed as the text.</dd>
				
				<dt class="font-bold">dynamicOptionsFunction</dt>
				<dd><i>(only applicable to type=select)</i><br>The body of a function which is used to populate the options array. The function receives a 'callback' parameter which has to be called with the resulting options.<br>The dynamic options can be in the same format as the options above. IMPORTANT: If this key is provided, the options key is ignored.</dd>
				
				<dt class="font-bold">rows</dt>
				<dd> <i>(only applicable to type=textarea)</i><br>The number of rows the textarea will have initially. If omitted, or not parseable as an integer, it will default to 5.</dd>
			</dl>

			<h2>Description</h2>
			<p>The description will be displayed when the user adds the widget to a page. It can contain HTML and usually serves the purpose of explaining what the widget is used for and the function of the configuration switches.</p>

			<h2>Options</h2>
			<p>The following options can be configured for a widget:</p>
			<ul>
				<dt class="font-bold">Selectors</dt>
				<dd>The selectors control into which elements a widget may be inserted. If a selector matches, the widget appears in the "Suggested widgets" context menu in the pages tree.</dd>
				
				<dt class="font-bold">Is Page Template</dt>
				<dd>Check this box if the widget is a page template. The widget can the be selected when creating a page.</dd>
			</ul>
		`,
		dataSourcesInput: async config => {
			let cleanedLabel = config.cleanedLabel;
			let titleLabel   = config.titleLabel;
			let titleComment = config.titleComment;
			let label        = config.label;
			return `
				<div class="relative">
					<h4 id="label-${cleanedLabel}" data-comment="${titleComment}">${titleLabel}</h4>
					${await _Widgets.templates.dataSourcesSelector(cleanedLabel, label, '', 'rounded', 'widgets')}
				</div>
			`;
		},
		dataSourcesSelector: async (id, key, selectedValue, css, attributeSet) => {
			let tmp = {};
			let items = []

			items.push({
				name: 'No Data Source',
				value: '',
				reset: true
			});
			items.push({ isSeparator: true });
			items.push(
			{
				name: 'Use Existing Data Source',
				children: [
					{ name: 'User-defined Data Sources', children: await _Widgets.templates.getUserDefinedSourcesForMenu() },
					{ isSeparator: true },
					{ name: 'Custom Types', children: await _Widgets.templates.getCustomTypesForMenu() },
					{ name: 'System Types', children: await _Widgets.templates.getSystemTypesForMenu() },
					{ isSeparator: true },
					{ name: 'Folders', children: await _Widgets.templates.getFoldersForMenu({ parent: null }, { name: 'All root folders', icon: '#database-icon', value: 'root-folders' }) },
					{ isSeparator: true },
					{ name: 'Channels', children: await _Widgets.templates.getChannelsForMenu() },
				]
			});

			// only allow creation of new data sources in widgets dialog
			if (attributeSet === 'widgets') {
				items.push(
					{
						name: 'Create New Data Source',
						icon: '#circle_plus',
						children: [
							{ name: 'Create New Custom Type', value: 'create:schema', icon: '#database-add' },
							{ name: 'Other Data Sources', icon: '#circle_plus', children: [
									{ name: 'Create Script-based Data Source', value: 'create:datasource', icon: '#curly-braces-wrap-js' },
									{ name: 'Create Query-based Data Source', value: 'create:query', icon: '#list-cog' },
							] }
						]
					});
			}

			let menu = _Widgets.templates.createMenu(items, 'w-full', id, selectedValue, tmp);

			// we need to set different data attributes for the Insert widget dialog and the General tab!
			let widgetDialogAttributes = { 'id': id, 'data-info': 'select-source', 'data-key': key };
			let generalTabAttributes = { 'id': id, 'name': key, 'data-which': 'config' };
			let desiredAttributes = attributeSet === 'widgets' ? widgetDialogAttributes : generalTabAttributes;

			return `
				<div class="data-source-menu w-full" tabindex="0">
					<input type="text" class="form-field hidden" required ${Object.keys(desiredAttributes).map(k => `${k}="${desiredAttributes[k]}"`).join(' ')} />
					<div tabindex="0" class="flex items-center justify-between px-3 py-1 border-gray-input border ${css}"><span class="block truncate" id="${id}-output">${tmp.selectedValue ? tmp.selectedValue : 'Select Data Source'}</span><span class="font-bold text-xl">⏷</span></div>
					${menu}
				</div>
			`;
		},
		// this function is used in the onclick handler of the data source selector!
		setDataSourceValue: (inputId, name, value) => {
			let input = document.querySelector(`#${inputId}`);
			if (input) {
				input.value = value;
				input.dispatchEvent(new CustomEvent('change'));
				document.activeElement.blur();
				document.querySelector(`#${inputId}-output`).innerHTML = name;
			}
		},
		createMenu: (items, css = '', inputId, selectedValue, tmp) => `
			<ul class="${css} border-gray-input border bg-white border-box">
				${items.map(item => {
					if (!item?.children?.length && !item.value && !item.reset && !item.isSeparator && !item.isInputField) { return ''; }
					if (item.isInputField) {
						return `
						<li tabindex="0" class="m-0 p-0">
							<input autofocus placeholder="Enter name of new type.."  class="m-0 px-2 py-1" style="width: 200px;" type="text" name=""/>
						</li>
						`;
					} else if (item.isSeparator) {
						return '<hr>';
					} else {
						let onclick = '';
						if (item?.value || item?.reset) {
							onclick = `onclick="_Widgets.templates.setDataSourceValue('${inputId}', '${item.name}', '${item.value}')"`;
						}
						if (item?.value === selectedValue) {
							tmp.selectedValue = item.name;
						}
						return `
						<li tabindex="0">
							<div ${onclick} class="w-full px-2 ${item?.children?.length ? '' : 'py-1'} flex items-center justify-between">
								<div class="flex items-center justify-between">${item.icon ? `<svg class="mr-2" width="16" height="16"><use href="${item.icon}"></use></svg>` : ''}<span>${item.name}</span></div>
								${item?.children?.length ? '<span class="caret">&#x23f5;</span>' : ''}
							</div>
							${item?.children?.length ? _Widgets.templates.createMenu(item.children, '', inputId, selectedValue, tmp) : ''}
						</li>`;
					}
				}
				).join('')}
			</ul>
		`,
		getUserDefinedSourcesForMenu: async () => {
			let values = [];
			let sources = await _Widgets.templates.getFetchResult('DataSource', t => t.type !== 'SchemaNode' && t.type !== 'Folder');
			for (let value of sources) {
				values.push({ name: `${value.name}`, icon: '#database-icon', value: `node:${value.id}` });
			}
			return values;
		},
		getCustomTypesForMenu: async () => {
			let sources = await _Widgets.templates.getFetchResult('_schema', t => !t.isBuiltin && !t.isRel);
			let values = [];
			for (let value of sources) {
				values.push(await _Widgets.templates.getCustomTypeOptions(value.className, value.className, value.className + " nodes"));
			}
			return values;
		},
		getCustomTypeOptions: async (type, id, plural) => {
			return { name: `All ${plural}`, icon: '#database-icon', value: `node:${type}` };
		},
		getSystemTypesForMenu: async () => {
			return [
				await _Widgets.templates.getSystemTypeOptions('File', 'Files'),
				await _Widgets.templates.getSystemTypeOptions('Folder', 'Folders'),
				await _Widgets.templates.getSystemTypeOptions('Group', 'Groups'),
				await _Widgets.templates.getSystemTypeOptions('Image', 'Images'),
				await _Widgets.templates.getSystemTypeOptions('Page', 'Pages'),
				await _Widgets.templates.getSystemTypeOptions('User', 'Users'),
			];
		},
		getSystemTypeOptions: async (type, plural) => {
			return { name: `All ${plural}`, icon: '#database-icon', value: `node:${type}` };
		},
		getFoldersForMenu: async (properties, firstItem) => {
			let sources = await Command.queryPromise('Folder', 1000, 1, 'name', 'asc', properties);
			let values = [];
			if (firstItem) { values.push(firstItem); }
			if (sources.length) {
				if (firstItem) { values.push({ isSeparator: true }); }
				for (let value of sources) {
					values.push({
						name: value.name,
						children: await _Widgets.templates.getFoldersForMenu({parent: value.id}, {
							name: `All files in ${value.name}`,
							icon: '#database-icon',
							value: `node:${value.id}`
						})
					});
				}
			}
			return values;
		},
		getChannelsForMenu: async () => {
			let channels = await Command.queryPromise('ComponentConfiguration', 1000, 1, 'name', 'asc', {});
			let values = [ { name: 'current', value: 'channel:current'}, { name: 'parent', value: 'parent' } ];
			for (let channel of channels) {
				if (channel.role === 'controller' && channel.selectionChannel && channel.selectionChannel !== 'current') {
					values.push({ name: `${channel.selectionChannel}`, value: `channel:${channel.selectionChannel}` });
				}
			}
			values = values.sort((a, b) => a.name.localeCompare(b.name));
			return values;
		},
		getAvailableDataSources: async config => {
			let sources = await Command.queryPromise('DataSource', 1000, 1, 'name', 'asc', {});
			let channels = await Command.queryPromise('ComponentConfiguration', 1000, 1, 'name', 'asc', {});
			// initialize data sources with some default sources that are created on demand (will be overwritten if actual data sources exist)
			let values = {
				'node:File': 'All File nodes',
				'node:Folder': 'All Folder nodes',
				'node:Group': 'All Group nodes',
				'node:Image': 'All Image nodes',
				'node:Page': 'All Page nodes',
				'node:User': 'All User nodes',
			};
			for (let value of sources) {
				if (value.type === 'SchemaNode') {
					values['node:' + value.name] = 'All ' + value.name + ' nodes';
				} else {
					values['node:' + value.name] = value.name;
				}
			}
			for (let channel of channels) {
				if (channel.role === 'controller' && channel.selectionChannel && channel.selectionChannel !== 'current') {
					values['channel:' + channel.selectionChannel] = 'The "' + channel.selectionChannel + '" channel';
				}
			}
			return Object.fromEntries(Object.entries(values).sort(([a], [b]) => a.localeCompare(b)));
		},
		getFetchResult: async (url, filter) => {
			let response = await fetch(`${Structr.rootUrl}${url}`);
			if (response.ok) {
				let result = await response.json();
				if (result && result.result) {
					if (filter && typeof filter === 'function') {
						return result.result.filter(filter);
					} else {
						return result.result;
					}
				}
			}
			return [];
		},
		getOptionsAsText: (options, defaultValue) => {

			if (Array.isArray(options)) {

				return options.map(option => `<option ${((option === defaultValue) ? 'selected' : '')}>${option}</option>`).join('');

			} else if (Object.prototype.toString.call(options) === '[object Object]') {

				return Object.keys(options).map(option => `<option ${((option === defaultValue) ? 'selected' : '')} value="${option}">${options[option]}</option>`).join('');
			}
		},
		getExampleValuesForType: async (typeOfExampleData, resultFormat, inputValue) => {
			let request = await fetch(`${Structr.rootUrl}SchemaNode/getExampleData`, {
				method: 'POST',
				body: JSON.stringify({ typeOfExampleData, resultFormat, inputValue })
			});
			let result = await request.json();
			return result.result;
		}
	}
};