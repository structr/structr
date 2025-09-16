/*
 * Copyright (C) 2010-2025 Structr GmbH
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
	activeQueryTabPrefix: 'structrActiveQueryTab_' + location.port,
	activeEditTabPrefix: 'structrActiveEditTab_' + location.port,
	selectedObjectIdKey: 'structrSelectedObjectId_' + location.port,
	readOnlyAttrs: ['lastModifiedDate', 'createdDate', 'createdBy', 'id', 'checksum', 'size', 'version', 'relativeFilePath'],
	pencilEditBlacklist: ['html', 'body', 'head', 'title', ' script',  'input', 'label', 'button', 'textarea', 'link', 'meta', 'noscript', 'tbody', 'thead', 'tr', 'td', 'caption', 'colgroup', 'tfoot', 'col', 'style'],
	null_prefix: 'null_attr_',
	collectionPropertiesResultCount: {},
	exampleShowHideConditions: [
		{ value: '', text: '(none)' },
		{ value: 'true' },
		{ value: 'false' },
		{ value: 'me.isAdmin' },
		{ value: 'empty(current)' },
		{ value: 'not(empty(current))' }
	],
	changeBooleanAttribute: (attrElement, value, activeLabel, inactiveLabel) => {

		if (value === true) {
			attrElement.removeClass('inactive').addClass('active').prop('checked', true).html(_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 14, 14, ['icon-green', 'mr-2']) + ' ' + (activeLabel ?? ''));
		} else {
			attrElement.removeClass('active').addClass('inactive').prop('checked', false).text((inactiveLabel ?? '-'));
		}

	},
	reloadChildren: (id) => {

		let el = Structr.node(id);

		$(el).children('.node').remove();
		_Entities.resetMouseOverState(el);

		Command.children(id);
	},
	deleteNodes: (entities, recursive, callback) => {

		let confirmationHtml = `
			<p>Delete the following objects ${recursive ? '(all folders recursively) ' : ''}?</p>
			<div>
				${entities.map(entity => `<div><strong>${_Helpers.escapeTags(entity.name)}</strong> [${entity.id}]</div>`).join('')}
			</div>
			<br>
		`;

		_Dialogs.confirmation.showPromise(confirmationHtml).then(confirm => {

			if (confirm === true) {

				let nodeIds = entities.map(e => e.id);
				Command.deleteNodes(nodeIds, recursive);

				callback?.();
			}
		});
	},
	deleteNode: (entity, recursive, callback) => {

		_Dialogs.confirmation.showPromise(`Delete ${entity.type} <strong>${_Helpers.escapeTags(entity?.name ?? '')}</strong> [${entity.id}] ${recursive ? 'recursively ' : ''}?`).then(confirm => {

			if (confirm === true) {

				Command.deleteNode(entity.id, recursive);

				callback?.(entity);
			}
		});

	},
	deleteEdge: (entity, recursive, callback) => {

		_Dialogs.confirmation.showPromise(`Delete Relationship (${entity.sourceId})-[${entity.type}]->(${entity.targetId})${recursive ? ' recursively' : ''}?`).then(confirm => {

			if (confirm === true) {

				Command.deleteRelationship(entity.id, recursive);

				callback?.(entity);
			}
		});
	},
	repeaterConfig: (entity, el) => {

		let queryTypes = [
			{ title: 'Flow', propertyName: 'flow' },
			{ title: 'REST Query',     propertyName: 'restQuery' },
			{ title: 'Cypher Query',   propertyName: 'cypherQuery' },
			{ title: 'Function Query', propertyName: 'functionQuery' }
		];

		let queryTypeButtonsContainer = el.querySelector('.query-type-buttons');

		let queryTextElement = el.querySelector('.query-text');
		let flowSelector     = el.querySelector('#flow-selector');

		let repeaterConfigEditor;
		let saveQueryButton;

		let saveFunction = () => {

			if (queryTypeButtonsContainer.querySelectorAll('button.active').length > 1) {
				return new ErrorMessage().text('Please select only one query type.').show();
			}

			let data    = {};
			let mainKey = null;

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

					mainKey = queryType.propertyName;
				}

				data[queryType.propertyName] = val;
			}

			Command.setProperties(entity.id, data, (obj) => {

				Object.assign(entity, data);

				// vanilla replacement for $.is(':visible')
				if (flowSelector.offsetParent !== null) {
					_Helpers.blinkGreen(flowSelector);
				} else {
					_Helpers.blinkGreen(saveQueryButton);
				}
			}, null, mainKey);
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

			_Editors.updateMonacoEditorLanguage(repeaterConfigEditor, repeaterConfigEditor.customConfig.language, entity);
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

				let queryType   = e.target.dataset['queryType'];
				let isFlowQuery = (queryType === 'flow');

				saveQueryButton.classList.toggle('hidden', isFlowQuery);
				queryTextElement.classList.toggle('hidden', isFlowQuery);
				flowSelector.classList.toggle('hidden', !isFlowQuery);

				if (!isFlowQuery) {
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

			let saveFn = () => {
				Command.setProperty(entity.id, 'dataKey', datakeyInput.value, false, () => {
					_Helpers.blinkGreen(datakeyInput);
					entity.dataKey = datakeyInput.value;
				});
			};

			datakeyInput.addEventListener('keydown', (e) => {

				let keyCode = e.keyCode;
				let code    = e.code;

				// ctrl-s / cmd-s
				if ((code === 'KeyS' || keyCode === 83) && ((!_Helpers.isMac() && e.ctrlKey) || (_Helpers.isMac() && e.metaKey))) {
					e.preventDefault();
					e.stopPropagation();
					saveFn();
				}
			});

			saveDatakeyButton.addEventListener('click', () => {
				saveFn();
			});
		};

		flowSelector.insertAdjacentHTML('beforeend', '<option>--- Select Flow ---</option>');

		Command.getByType('FlowContainer', 1000, 1, 'effectiveName', 'asc', null, false, (flows) => {

			flowSelector.insertAdjacentHTML('beforeend', flows.map(flow => '<option value="' + flow.id + '">' + flow.effectiveName + '</option>').join());

			initRepeaterInputs();

			_Editors.resizeVisibleEditors();
		});
	},
	editEmptyDiv: (entity) => {

		let { dialogText, dialogMeta } = _Dialogs.custom.openDialog(`Edit source of "${entity?.name ?? entity.id}"`, null, ['popup-dialog-with-editor']);
		_Dialogs.custom.showMeta();

		dialogText.insertAdjacentHTML('beforeend', '<div class="editor h-full"></div>');
		dialogMeta.insertAdjacentHTML('beforeend', '<span class="editor-info"></span>');

		let dialogSaveButton   = _Dialogs.custom.updateOrCreateDialogSaveButton();
		let saveAndCloseButton = _Dialogs.custom.updateOrCreateDialogSaveAndCloseButton();
		let editorInfo         = dialogMeta.querySelector('.editor-info');
		_Editors.appendEditorOptionsElement(editorInfo);

		let initialText = '';

		let emptyDivMonacoConfig = {
			value: initialText,
			language: 'html',
			lint: true,
			autocomplete: true,
			preventRestoreModel: true,
			forceAllowAutoComplete: true,
			changeFn: (editor, entity) => {

				let disabled = (initialText === editor.getValue());
				_Helpers.disableElements(disabled, dialogSaveButton, saveAndCloseButton);
			},
			saveFn: (editor, entity, close = false) => {

				let text1 = initialText || '';
				let text2 = editor.getValue();

				if (text1 === text2) {
					return;
				}

				Command.patch(entity.id, text1, text2, () => {

					_Dialogs.custom.showAndHideInfoBoxMessage('Content saved.', 'success', 2000, 200);
					_Helpers.disableElements(true, dialogSaveButton, saveAndCloseButton);

					Command.getProperty(entity.id, 'content', (newText) => {
						initialText = newText;
					});
				});

				Command.saveNode(`<div data-structr-hash="${entity.id}">${editor.getValue()}</div>`, entity.id, () => {

					_Dialogs.custom.showAndHideInfoBoxMessage('Node source saved and DOM tree rebuilt.', 'success', 2000, 200);

					if (_Entities.isExpanded(Structr.node(entity.id))) {
						$('.expand_icon_svg', Structr.node(entity.id)).click().click();
					}

					if (close === true) {
						_Dialogs.custom.clickDialogCancelButton();
					}
				});
			}
		};

		let editor = _Editors.getMonacoEditor(entity, 'source', dialogText.querySelector('.editor'), emptyDivMonacoConfig);

		_Editors.addEscapeKeyHandlersToPreventPopupClose(entity.id, 'source', editor);

		dialogSaveButton.addEventListener('click', (e) => {
			e.stopPropagation();

			emptyDivMonacoConfig.saveFn(editor, entity);
		});

		saveAndCloseButton.addEventListener('click', (e) => {
			e.stopPropagation();

			emptyDivMonacoConfig.saveFn(editor, entity, true);
		});

		Structr.resize();

		_Editors.resizeVisibleEditors();
	},
	getSchemaProperties: (type, view, callback, allowFallback = true) => {

		let url = `${Structr.rootUrl}_schema/${type}/${view}`;

		fetch(url).then(async response => {

			let data = await response.json();

			if (response.ok) {

				let properties = {};

				if (data.result) {

					for (let prop of data.result) {
						properties[prop.jsonName] = prop;
					}
				}

				callback?.(properties);

			} else if (allowFallback === false) {

				Structr.errorFromResponse(data, url);
				console.log(`ERROR: loading Schema ${type} with view ${view}`);

			} else {

				throw new Error(response.status + ': ' + response.statusText);
			}

		}).catch(e => {

			if (allowFallback === true) {
				_Entities.getSchemaProperties(type, 'ui', callback, false);
			}
		});
	},
	showProperties: (obj, activeViewOverride, showDeleteBtn = Structr.isModuleActive(_Crud)) => {

		_Entities.getSchemaProperties(obj.type, 'custom', (properties) => {

			let handleGraphObject = (entity) => {

				let views      = ['ui'];
				let activeView = 'ui';
				let tabTexts   = [];

				// filter out id,name,type from properties to only show tab "Custom attributes" if there really are custom attributes
				let customPropertiesWithoutBasicProps = Object.keys(properties).filter(key => !['id', 'type', 'name'].includes(key));
				if (customPropertiesWithoutBasicProps.length > 0) {
					views.push('custom');
				}

				if (activeViewOverride) {
					activeView = activeViewOverride;
				}

				_Schema.caches.getTypeInfo(entity.type, (typeInfo) => {

					let dialogTitle = 'Edit properties';

					if (entity.hasOwnProperty('relType')) {

						views = ['all'];

						tabTexts.all = 'Relationship Properties';
						tabTexts.sourceNode = 'Source Node Properties';
						tabTexts.targetNode = 'Target Node Properties';

						dialogTitle = `Edit properties of ${entity?.type ?? ''} relationship ${entity?.name ?? entity.id}`;

					} else {

						if (entity.isDOMNode && !entity.isContent) {
							views.unshift('_html_');
							if (Structr.isModuleActive(_Pages)) {
								activeView = 'general';
							}
						}

						tabTexts._html_ = 'HTML';
						tabTexts.ui     = 'Advanced';
						tabTexts.custom = 'Custom Properties';

						dialogTitle = `Edit properties of ${entity?.type ?? ''} node ${entity?.name ?? entity.id}`;
					}

					let { dialogText } = _Dialogs.custom.openDialog(dialogTitle);

					if (showDeleteBtn) {

						let deleteBtn = _Dialogs.custom.appendCustomDialogButton(_Dialogs.custom.templates.deleteButton());

						deleteBtn.addEventListener('click', async (e) => {
							let deleted = await _Crud.helpers.crudAskDelete(obj.type, obj.id);

							if (deleted) {
								_Dialogs.custom.getCloseDialogButton().click();
							}
						});
					}

					dialogText.insertAdjacentHTML('beforeend', `
						<div id="tabs" class="flex flex-col h-full overflow-hidden">
							<ul class="flex-shrink-0"></ul>
						</div>
					`);

					let mainTabs  = dialogText.querySelector('#tabs');
					let contentEl = dialogText.querySelector('#tabs');

					_Entities.generalTab.appendGeneralTypeTab(entity, mainTabs, contentEl, typeInfo);

					_Entities.appendViews(entity, views, tabTexts, mainTabs, contentEl, typeInfo);

					if (!entity.hasOwnProperty('relType')) {
						let tabContent = _Entities.appendPropTab(entity, mainTabs, contentEl, 'permissions', 'Security', false);
						_Entities.accessControlDialog(entity, $(tabContent), typeInfo);
					}

					activeView = activeViewOverride || LSWrapper.getItem(`${_Entities.activeEditTabPrefix}_${entity.id}`) || activeView;

					let requestedTabLi = mainTabs.querySelector(`#tab-${activeView}`);
					if (requestedTabLi) {
						requestedTabLi.click();
					} else {
						mainTabs.querySelector('li').click();
					}

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
	appendPropTab: (entity, tabsEl, contentEl, name, label, isActive, showCallback = null, refreshOnShow = false, tabHidden = false) => {

		let tabId      = `tab-${name}`;
		let ul         = tabsEl.querySelector('ul');
		let tabContent = _Helpers.createSingleDOMElementFromHTML(`<div class="propTabContent h-full overflow-y-auto mt-6" id="tabView-${name}" data-tab-id="${tabId}"></div>`);
		let tab        = _Helpers.createSingleDOMElementFromHTML(`<li id="${tabId}">${label}</li>`);

		contentEl.appendChild(tabContent);
		ul.appendChild(tab);

		if (isActive) {
			tab.classList.add('active');
		}
		if (tabHidden && !isActive) {
			tab.style.display = 'none';
		}

		tab.addEventListener('click', (e) => {

			e.stopPropagation();

			for (let everyTab of contentEl.querySelectorAll('.propTabContent')) {
				everyTab.style.display = 'none';
			}
			for (let everyLi of ul.querySelectorAll('.active')) {
				everyLi.classList.remove('active');
			}
			tabContent.style.display = 'block';
			tab.classList.add('active');

			LSWrapper.setItem(`${_Entities.activeEditTabPrefix}_${entity.id}`, name);

			if (typeof showCallback === 'function') {

				// this does not really work for the initially active tab because its html is not yet output... showCallback must also be called from 'outside'

				if (refreshOnShow === true) {

					// update entity for show callback
					if (entity.relType) {
						Command.getRelationship(entity.id, entity.target, null, (reloadedEntity) => {
							showCallback(tabContent, reloadedEntity);
						});
					} else {
						Command.get(entity.id, null, (reloadedEntity) => {
							showCallback(tabContent, reloadedEntity);
						});
					}
				} else {
					showCallback(tabContent, entity);
				}
			}
		});

		if (isActive) {
			tabContent.style.display = 'block';
		}

		return tabContent;
	},
	appendViews: (entity, views, texts, tabsEl, contentEl, typeInfo) => {

		let ul = tabsEl.querySelector('ul');

		for (let view of views) {

			let tab     = _Helpers.createSingleDOMElementFromHTML(`<li id="tab-${view}">${texts[view]}</li>`);
			let tabView = _Helpers.createSingleDOMElementFromHTML(`<div class="propTabContent overflow-y-auto mt-6" id="tabView-${view}"></div>`);

			ul.appendChild(tab);
			contentEl.appendChild(tabView);

			tab.addEventListener('click', (e) => {
				e.stopPropagation();

				for (let everyTab of contentEl.querySelectorAll('.propTabContent')) {
					everyTab.style.display = 'none';
				}

				for (let li of ul.querySelectorAll('.active')) {
					li.classList.remove('active');
				}
				tab.classList.add('active');

				_Helpers.fastRemoveAllChildren(tabView);
				tabView.style.display = 'block';

				LSWrapper.setItem(`${_Entities.activeEditTabPrefix}_${entity.id}`, view);

				_Entities.listProperties(entity, view, $(tabView), typeInfo, () => {
					for (let input of tabView.querySelectorAll('input.dateField')) {
						_Entities.activateDatePicker($(input));
					}
				});
			});
		}
	},
	getNullIconForKey: (key) => _Icons.getSvgIconWithID(`${_Entities.null_prefix}${key}`, _Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['nullIcon', 'icon-grey'])),
	classNameForEmptyStringWarningContainer: 'warn-empty-string',
	showOrHideWarningForEmptyStringInHTMLAttribute: (row, value) => {

		let warnContainer = row.querySelector(`.${_Entities.classNameForEmptyStringWarningContainer}`);

		if (value === '') {

			let key        = row.querySelector('input').name;
			let displayKey = _Entities.getDisplayKeyForHTMLKey(key);

			_Helpers.appendInfoTextToElement({
				text: `The <b>${displayKey}</b> attribute contains an empty string and will be present in the HTML output. To clear this attribute completely, use the ${_Entities.getNullIconForKey()} icon to the right of the input field.`,
				element: warnContainer,
				customToggleIcon: _Icons.iconWarningYellowFilled,
				width: 16,
				height: 16,
				noSpan: true
			});

		} else {

			_Helpers.fastRemoveAllChildren(warnContainer);
		}
	},
	listProperties: (entity, view, tabView, typeInfo, callback) => {

		_Entities.getSchemaProperties(entity.type, view, (schemaProperties) => {

			let serializableKeys     = Object.keys(schemaProperties).filter(key => schemaProperties[key].serializationDisabled !== true);
			let filteredProperties   = serializableKeys.filter(key => !(typeInfo[key].isCollection && typeInfo[key].relatedType) );
			let collectionProperties = serializableKeys.filter(key => typeInfo[key].isCollection && typeInfo[key].relatedType );

			fetch(`${Structr.rootUrl}${entity.type}/${entity.id}/all?${Structr.getRequestParameterName('edit')}=2`, {
				headers: _Helpers.getHeadersForCustomView(filteredProperties)
			}).then(async response => {

				if (response.ok) {

					let data          = await response.json();
					let fetchedEntity = data.result;

					StructrModel.updateModelWithData(fetchedEntity.id, fetchedEntity);

					let tempNodeCache = new AsyncObjectCache((id) => {
						Command.get(id, 'id,name,type,tag,isContent,content', (node) => {
							tempNodeCache.addObject(node, node.id);
						});
					});

					let keys           = Object.keys(schemaProperties);
					let noCategoryKeys = [];
					let groupedKeys    = {};

					for (let key of keys) {

						// completely hide attributes with the "serializationDisabled" flag
						let serializationDisabled = typeInfo?.[key]?.serializationDisabled;
						if (serializationDisabled !== true) {

							let category = typeInfo?.[key]?.category ?? 'System';

							if (category !== 'System') {

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

					if (view === 'ui') {
						noCategoryKeys = noCategoryKeys.filter(key => !['_html_id', '_html_class'].includes(key));
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

				callback?.(schemaProperties);
			});
		});
	},
	displayCollectionPager: (tempNodeCache, entity, key, page, container) => {

		let pageSize = 10, resultCount;

		let cell = $(`.value.${key}_`, container);
		cell.css('height', '60px');

		let fetchKey = key;
		if (key === 'syncedNodesIds') { fetchKey = 'syncedNodes'; }
		if (key === 'childrenIds') { fetchKey = 'children'; }

		fetch(`${Structr.rootUrl + entity.type}/${entity.id}/${fetchKey}?${Structr.getRequestParameterName('pageSize')}=${pageSize}&${Structr.getRequestParameterName('page')}=${page}`, {
			headers: _Helpers.getHeadersForCustomView(['id', 'name'])
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

					let collection = (data.result[0][key] ?? data.result ?? []);

					for (let obj of collection) {

						let nodeId = (typeof obj === 'string') ? obj : obj.id;

						tempNodeCache.registerCallback(nodeId, nodeId, (node) => {

							_Entities.appendRelatedNode(cell, node, (nodeEl) => {

								nodeEl[0].querySelector('.remove')?.addEventListener('click', e => {

									e.stopPropagation();

									Command.removeFromCollection(entity.id, fetchKey, node.id, () => {
										nodeEl.remove();
										_Helpers.blinkGreen(cell);
										_Dialogs.custom.showAndHideInfoBoxMessage(`Related node "${node.name || node.id}" has been removed from property "${key}".`, 'success', 2000, 1000);
									});
								});
							});
						});
					}
				}
			}
		})
	},
	getDisplayKeyForHTMLKey: (key) => {
		let displayKey = key;
		if (key.indexOf('data-') !== 0) {
			if (key.indexOf('_html_') === 0) {
				displayKey = displayKey.substring(6);
			} else if (key.indexOf('_custom_html_') === 0) {
				displayKey = displayKey.substring(13);
			}
		}
		return displayKey;
	},
	createPropertyTable: (heading, keys, res, entity, view, container, typeInfo, tempNodeCache) => {

		if (heading) {
			container.append(`<h2>${heading}</h2>`);
		}
		container.append(`<table class="props ${view} ${res['id']}_"></table>`);
		let propsTable = $('table:last', container);
		let focusAttr  = 'class';
		let id         = entity.id;

		let onUpdateCallback = (row, newValue) => {

			if (view === '_html_') {
				_Entities.showOrHideWarningForEmptyStringInHTMLAttribute(row, newValue);
			}
		};

		if (view === '_html_') {
			keys.sort();
		}

		for (let key of keys) {

			let valueCell    = undefined;
			let isReadOnly   = _Entities.readOnlyAttrs.includes(key) || (typeInfo[key]?.readOnly ?? false);
			let isSystem     = (typeInfo[key]?.system ?? false);
			let isBoolean    = (typeInfo[key]?.type === 'Boolean');
			let isDate       = (typeInfo[key]?.type === 'Date');
			let isRelated    = (typeInfo[key]?.relatedType !== undefined);
			let isCollection = (typeInfo[key]?.isCollection ?? false);

			if (view === '_html_') {

				let showKeyInitially = (key === '_html_class' || key === '_html_id');
				for (let mostUsed of _Elements.mostUsedAttrs) {
					if (mostUsed.elements.includes(entity.tag) && mostUsed.attrs.includes(key.substring(6))) {
						showKeyInitially = true;
						focusAttr = mostUsed.focus ? mostUsed.focus : focusAttr;
					}
				}

				// Always show non-empty, non 'data-structr-' attributes
				if (res[key] !== null && key.indexOf('data-structr-') !== 0) {
					showKeyInitially = true;
				}

				let value = res[key];
				let row   = _Helpers.createSingleDOMElementFromHTML(_Entities.templates.propertyRow({
					rowClass: (showKeyInitially === false) ? 'hidden' : '',
					displayKey: _Entities.getDisplayKeyForHTMLKey(key),
					key,
					value,
					typeInfo: typeInfo[key]
				}));
				propsTable[0].appendChild(row);
				valueCell = $(`.value.${key}_`, propsTable);

				_Entities.showOrHideWarningForEmptyStringInHTMLAttribute(row, value);

			} else {

				let row = $(`<tr><td class="key">${_Helpers.formatKey(key)}</td><td class="value ${key}_"></td><td>${_Entities.getNullIconForKey(key)}</td></tr>`);
				propsTable.append(row);
				valueCell = $(`.value.${key}_`, propsTable);

				if (!typeInfo[key]) {

					valueCell.append(_Helpers.formatValueInputField(key, res[key], typeInfo[key]));

				} else {

					if (key.startsWith('_html_') === false) {

						if (isBoolean) {

							valueCell.removeClass('value').append(`<input type="checkbox" class="${key}_">`);
							let checkbox = $(propsTable.find(`input[type="checkbox"].${key}_`));

							let val = res[key] ?? false;
							checkbox.prop('checked', val);

							let allowChange = ((!isReadOnly || StructrWS.isAdmin) && !isSystem);
							if (typeInfo[key].className === 'org.structr.core.property.ConstantBooleanProperty') {
								allowChange = false;
							}

							if (allowChange) {

								checkbox.on('change', function() {

									let checked = checkbox.prop('checked');
									_Entities.setProperty(id, key, checked, false, (newVal) => {
										if (val !== newVal) {
											_Helpers.blinkGreen(valueCell);
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

									tempNodeCache.registerCallback(nodeId, nodeId, (node) => {

										_Entities.appendRelatedNode(valueCell, node, (nodeEl) => {

											$('.remove', nodeEl).on('click', function(e) {
												e.preventDefault();

												_Entities.setProperty(id, key, null, false, (newVal) => {

													if (!newVal) {

														nodeEl.remove();
														_Helpers.blinkGreen(valueCell);
														_Dialogs.custom.showAndHideInfoBoxMessage(`Related node "${node.name || node.id}" has been removed from property "${key}".`, 'success', 2000, 1000);

													} else {

														_Helpers.blinkRed(valueCell);
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

							valueCell.append(_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['add', 'icon-green'])));

							$('.add', valueCell).on('click', function() {
								let { dialogText } = _Dialogs.custom.openDialog(`Add ${typeInfo[key].type}`);
								_Entities.displaySearch(id, key, typeInfo[key].type, $(dialogText), isCollection);
							});

						} else {

							valueCell.append(_Helpers.formatValueInputField(key, res[key], typeInfo[key]));
						}
					}
				}
			}

			let hintText = typeInfo[key]?.hint;

			if (hintText) {
				_Helpers.appendInfoTextToElement({
					element: $('.key:last', propsTable),
					text: hintText,
					class: 'hint'
				});
			}

			let nullIcon = container[0].querySelector(`#${_Entities.null_prefix}${key}`);

			if (isSystem || isReadOnly || isBoolean) {

				nullIcon?.remove();

			} else {

				nullIcon?.addEventListener('click', (e) => {

					let row      = e.target.closest('tr');
					let input    = $(`.${key}_`).find('input');
					let textarea = $(`.${key}_`).find('textarea');

					_Entities.setProperty(id, key, null, false, (newVal = null) => {

						if (!newVal) {

							if (key.indexOf('_custom_html_') === -1) {

								if (isCollection) {

									let cell = valueCell[0];
									_Helpers.fastRemoveAllChildren(cell);
									cell.innerHTML = _Helpers.formatArrayValueField(key, [], typeInfo[key]);

									for (let el of cell.querySelectorAll(`[name="${key}"]`)) {
										_Entities.activateInput(el, id, key, entity.type, typeInfo, onUpdateCallback);
									}
								}

								_Helpers.blinkGreen(valueCell);

								_Dialogs.custom.showAndHideInfoBoxMessage(`Property "${key}" has been set to null.`, 'success', 2000, 1000);

							} else {

								row.remove();
								_Dialogs.custom.showAndHideInfoBoxMessage(`Custom HTML property "${key}" has been removed`, 'success', 2000, 1000);
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

							onUpdateCallback?.(row, newVal);

						} else {

							_Helpers.blinkRed(input);
						}

						if (!isRelated) {
							input.val(newVal);
							textarea.val(newVal);
						}
					});
				});
			}
		}

		for (let el of propsTable[0].querySelectorAll('tr td.value textarea, tr td.value input')) {

			let key = el.name;
			_Entities.activateInput(el, id, key, entity.type, typeInfo, onUpdateCallback);
		}

		if (view === '_html_') {

			let focusedInput = propsTable[0].querySelector(`input[name="_html_${focusAttr}"]`);
			focusedInput?.focus();
			focusedInput?.setSelectionRange(focusedInput.value?.length, focusedInput.value?.length);

			container.append(`
				<div class="flex items-center mt-4 mb-4">
					<button class="show-all hover:bg-gray-100 focus:border-gray-666 active:border-green ml-4 mr-4">Show all attributes</button>
					<button class="add-custom-attribute hover:bg-gray-100 focus:border-gray-666 active:border-green mr-2">Add custom property</button>
				</div>
			`);

			$('.show-all', container).on('click', function() {

				propsTable.addClass('show-all');

				$(this).attr('disabled', 'disabled').addClass('disabled');
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

						_Helpers.blinkRed(keyInput);
						new ErrorMessage().text('Key can not begin with "data-structr-" as it is reserved for internal use.').show();

					} else if (!regexAllowed.test(key)) {

						_Helpers.blinkRed(keyInput);
						new ErrorMessage().text('Key contains forbidden characters. Allowed: "a-z", "A-Z", "-" and "_".').show();

					} else {

						let newKey = '_custom_html_' + key;

						Command.setProperty(id, newKey, val, false, (newVal) => {

							// replace input so that the old event (this function) is removed and we can attach new elements via "activateInput"
							let newRow = _Helpers.createSingleDOMElementFromHTML(_Entities.templates.propertyRow({
								rowClass: '',
								displayKey: _Entities.getDisplayKeyForHTMLKey(key),
								key: newKey,
								value: val,
								typeInfo: typeInfo[key]
							}));

							_Helpers.fastRemoveAllChildren(row);

							row.replaceWith(newRow);

							let newValueInput = newRow.querySelector('input');
							_Helpers.blinkGreen(newValueInput);

							_Dialogs.custom.showAndHideInfoBoxMessage(`New property "${key}" has been added and saved with value "${val}".`, 'success', 2000, 1000);

							_Entities.activateInput(newValueInput, id, newKey, entity.type, typeInfo, onUpdateCallback);

							newRow.querySelector(`#${_Entities.null_prefix}${newKey}`).addEventListener('click', () => {

								_Entities.setProperty(id, newKey, null, false, (newVal) => {
									newRow.remove();
									_Dialogs.custom.showAndHideInfoBoxMessage(`Custom HTML property "${key}" has been removed`, 'success', 2000, 1000);
								});
							});
						});
					}
				}
			};

			let addCustomAttributeButton = $('.add-custom-attribute', container);

			_Helpers.appendInfoTextToElement({
				element: addCustomAttributeButton,
				text: "Any property name is allowed but the 'data-' prefix is recommended. Please note that 'data-structr-' is reserved for internal use.",
				insertAfter: true,
				customToggleIconClasses: ['icon-blue'],
				noSpan: true
			});

			addCustomAttributeButton.on('click', () => {
				let newAttributeRow = _Helpers.createSingleDOMElementFromHTML('<tr><td class="key"><input type="text" class="newKey" name="key" placeholder="data-..."></td><td class="value"><input type="text" value=""></td><td></td></tr>');
				propsTable[0].appendChild(newAttributeRow);

				for (let input of newAttributeRow.querySelectorAll('input')) {
					input.addEventListener('focusout', () => {
						saveCustomHTMLAttribute(newAttributeRow, input);
					});
				}
			});
		}
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
			if (searchString.length && e.keyCode === 13) {

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
							box.append(`<div title="${_Helpers.escapeForHtmlAttributes(displayName)}" " class="_${node.id} node element abbr-ellipsis abbr-120">${displayName}</div>`);
							$('._' + node.id, box).on('click', function() {

								let nodeEl = $(this);

								if (isCollection) {

									_Entities.addToCollection(id, node.id, key, () => {

										_Helpers.blinkGreen(nodeEl);

										if (Structr.isModuleActive(_Pages)) {
											_Pages.refreshCenterPane(StructrModel.obj(id), location.hash);
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

										_Dialogs.custom.clickDialogCancelButton();
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

				if (searchString.trim() === '') {
					_Dialogs.custom.clickDialogCancelButton();
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
			Command.setProperty(itemId, key, collectionIds, false, () => {
				callback?.();
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
	// appendDatePicker: function(el, entity, key, format) {
	//
	// 	if (!entity[key] || entity[key] === 'null') {
	// 		entity[key] = '';
	// 	}
	//
	// 	el.append(`<input class="dateField" name="${key}" type="text" value="${entity[key]}" autocomplete="off">`);
	//
	// 	let dateField = $(el.find('.dateField'));
	// 	_Entities.activateDatePicker(dateField, format);
	//
	// 	return dateField;
	// },
	activateDatePicker: (input, format) => {

		if (!format) {
			format = input.data('dateFormat');
		}

		let dateTimePickerFormat = _Helpers.getDateTimePickerFormat(format);
		input.datetimepicker({
			dateFormat: dateTimePickerFormat.dateFormat,
			timeFormat: dateTimePickerFormat.timeFormat,
			separator: dateTimePickerFormat.separator
		});
	},
	getRelatedNodeHTML: (node, displayName = null, includeRemoveIcon = true) => {

		if (!displayName) {
			displayName = _Crud.helpers.getDisplayName(node);
		}

		return `
			<div title="${_Helpers.escapeForHtmlAttributes(displayName)}" class="_${node.id} node related-node ${node.type ? node.type.toLowerCase() : (node?.tag ?? 'element')} ${node.id}_ relative">
				<span class="abbr-ellipsis abbr-80">${displayName}</span>
				${includeRemoveIcon ? _Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['remove', 'icon-lightgrey', 'cursor-pointer'])) : ''}
			</div>
		`;
	},
	updateRelatedNodeName: (relatedNodeEl, newName) => {

		relatedNodeEl.title = _Helpers.escapeForHtmlAttributes(newName);
		relatedNodeEl.querySelector('span').textContent = newName;
	},
	insertRelatedNode: (cell, node, onDelete, position, displayName) => {
		/** Alternative function to appendRelatedNode
		    - no jQuery
		    - uses insertAdjacentHTML
		    - default position: beforeend
		*/
		cell = (cell instanceof jQuery ? cell[0] : cell);
		cell.insertAdjacentHTML(position ?? 'beforeend', _Entities.getRelatedNodeHTML(node, displayName));

		let nodeEl = cell.querySelector('._' + node.id);

		nodeEl.addEventListener('click', (e) => {
			e.preventDefault();
			_Entities.showProperties(node);
			return false;
		});

		if (onDelete) {
			return onDelete(nodeEl);
		}
	},
	appendRelatedNode: (cell, node, onDelete, displayName) => {

		cell.append(_Entities.getRelatedNodeHTML(node, displayName));
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
	activateInput: (input, id, key, type, typeInfo, onUpdateCallback) => {

		let relId  = $(input).parent().attr('rel_id');
		let objId  = relId ? relId : id;

		let cell = input.closest('.value');
		if (!cell) {
			cell = input.closest('.__value');
		}

		if (!input.classList.contains('readonly') && !input.classList.contains('newKey')) {

			if (input.dataset['activated'] !== 'true') {

				input.dataset['activated'] = 'true';

				input.closest('.array-attr')?.querySelector('svg.remove')?.addEventListener('click', () => {

					let oldValue = _Entities.getArrayValue(key, cell);
					input.parentNode.remove();
					input.dataset['changed'] = 'true';

					_Entities.saveValue(cell, input, objId, key, oldValue, id, type, typeInfo, onUpdateCallback);
				});

				input.addEventListener('focus', function() {
					input.classList.add('active');
				});

				input.addEventListener('change', function() {
					input.dataset['changed'] = 'true';
					if (input.type === 'checkbox') {
						input.blur();
					}
				});

				if (!input.classList.contains('input-datetime')) {

					input.addEventListener('focusout', function() {
						input.dispatchEvent(new Event('input-finished'));
					});
				}

				input.addEventListener('input-finished', (e) => {

					let oldValue = StructrModel.obj(id)?.[key] ?? null;
					_Entities.saveValue(cell, input, objId, key, oldValue, id, type, typeInfo, onUpdateCallback);

					input.classList.remove('active');

					for (let icon of input.parentNode.querySelectorAll('.icon')) {
						icon.remove();
					}
				});

				if (_Crud.types?.[type]?.views?.all?.[key]?.type === 'Date[]') {

					_Entities.addDatePicker(input, key, type, () => {
						input.dataset['changed'] = 'true';
						input.dispatchEvent(new Event('input-finished'));
					});
				}
			}
		}
	},
	addDatePicker: (input, key, type, onCloseCallback) => {

		let defaultFormat = "yyyy-MM-dd'T'HH:mm:ssZ";
		let format = _Crud.helpers.isFunctionProperty(key, type) ? defaultFormat : _Crud.helpers.getFormat(type, key);
		let dateTimePickerFormat = _Helpers.getDateTimePickerFormat(format);

		let pickerConfig = {
			parse: 'loose',
			dateFormat: dateTimePickerFormat.dateFormat,
			onClose: onCloseCallback
		};

		if (dateTimePickerFormat.timeFormat) {

			pickerConfig.timeFormat = dateTimePickerFormat.timeFormat;
			pickerConfig.separator  = dateTimePickerFormat.separator;

			$(input).datetimepicker(pickerConfig);
			input.addEventListener('focus', (e) => {
				$(input).datetimepicker('show');
			});

		} else {

			$(input).datepicker(pickerConfig);
			input.addEventListener('focus', (e) => {
				$(input).datepicker('show');
			});
		}
	},
	getArrayValue: (key, cell) => {

		let values      = [];
		let valueInputs = cell.querySelectorAll(`[name="${key}"]`);

		for (let el of valueInputs) {

			let isNew     = (el.dataset['isNew'] === 'true');
			let isChanged = (el.dataset['changed'] === 'true');

			if (!isNew || (isNew && isChanged)) {

				let value = (el.type === 'checkbox') ? (el.checked ? 'true' : 'false') : $(el).val();
				values.push(value);
			}
		}

		return values;
	},
	saveValue: (cell, el, objId, key, oldVal, id, type, typeInfo, onUpdateCallback) => {

		let input = $(el);
		let isCreateDialog = !objId;
		let val;

		let isArrayType = (typeInfo?.[key]?.isCollection == true && (typeInfo?.[key]?.relatedType === undefined));
		if (isArrayType) {
			val = _Entities.getArrayValue(key, cell);
		} else {
			val = input.val();
		}

		if (input[0].dataset['changed'] === 'true') {

			input[0].dataset['changed'] = 'false';

			let updateInput = (newVal) => {

				let isPassword = (input.prop('type') === 'password');

				if (isPassword || (newVal !== oldVal)) {

					if (!isCreateDialog) {
						let blinkTarget = (isArrayType) ? cell : input;
						_Helpers.blinkGreen(blinkTarget);
					}

					let valueMsg;
					if (newVal.constructor === Array) {

						_Helpers.fastRemoveAllChildren(cell);
						cell.innerHTML = _Helpers.formatArrayValueField(key, newVal, typeInfo[key]);
						valueMsg = (newVal !== undefined || newValue !== null) ? `value [${newVal.join(',\n')}]`: 'empty value';

						for (let el of cell.querySelectorAll(`[name="${key}"]`)) {
							_Entities.activateInput(el, id, key, type, typeInfo, onUpdateCallback);
						}

					} else {

						input.val(newVal);
						valueMsg = (newVal !== undefined || newValue !== null) ? `value "${newVal}"`: 'empty value';
					}

					if (!isCreateDialog) {
						_Dialogs.custom.showAndHideInfoBoxMessage(`Updated property "${key}"${!isPassword ? ' with ' + valueMsg : ''}`, 'success', 2000, 200);
					}

					onUpdateCallback(input[0].closest('tr'), newVal);

				} else {

					input.val(oldVal);
				}

				oldVal = newVal;
			}

			if (!isCreateDialog) {

				_Entities.setProperty(objId, key, val, false, newVal => {
					updateInput(newVal ?? (isArrayType ? [] : undefined));
				});

			} else {

				updateInput(val);
			}
		}
	},
	setProperty: (id, key, val, recursive, callback) => {

		let isCreateDialog = !id;

		if (isCreateDialog) {

			/* special handling for create-dialogs - simply allow the change and directly call the callback */
			callback(val);

		} else {

			Command.setProperty(id, key, val, recursive, () => {
				Command.getProperty(id, key, callback);
			});
		}
	},
	bindAccessControl: function(btn, entity) {

		btn.on('click', function(e) {
			e.stopPropagation();
			_Entities.showAccessControlDialog(entity);
		});
	},
	accessControlDialog: (entity, container, typeInfo) => {

		let id = entity.id;
		let requiredAttributesForPrincipals = 'id,name,type,isGroup,isAdmin,blocked';

		let handleGraphObject = (entity) => {

			let allowRecursive = (entity.type === 'Template' || entity.isFolder || (Structr.isModuleActive(_Pages) && !(entity.isContent)));
			let owner_select_id = 'owner_select_' + id;
			container.append(`
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
							<td class="relative">
								<select style="z-index: 999" id="newPrincipal">
									<option></option>
								</select>
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

			let securityContainer = container.find('.security-container');
			_Entities.appendBooleanSwitch(securityContainer, entity, 'visibleToPublicUsers', ['Visible to public users', 'Not visible to public users'], 'Click to toggle visibility for users not logged-in', '#recursive');
			_Entities.appendBooleanSwitch(securityContainer, entity, 'visibleToAuthenticatedUsers', ['Visible to auth. users', 'Not visible to auth. users'], 'Click to toggle visibility to logged-in users', '#recursive');

			fetch(`${Structr.rootUrl}${entity.type}/${entity.id}/in`).then(async response => {

				let data = await response.json();

				if (response.ok) {

					for (let result of data.result) {

						let allowed = result.allowed ?? [];

						let permissions = {
							read: allowed.includes('read'),
							write: allowed.includes('write'),
							delete: allowed.includes('delete'),
							accessControl: allowed.includes('accessControl')
						};

						let principalId = result.principalId;
						if (principalId) {
							Command.get(principalId, requiredAttributesForPrincipals, (p) => {
								_Entities.addPrincipal(entity, p, permissions, allowRecursive, container);
							});
						}
					}
				}
			})

			let ownerSelect      = $('#' + owner_select_id, container);
			let granteeSelect    = $('#newPrincipal', container);
			let spinnerIcon      = _Helpers.createSingleDOMElementFromHTML(_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 24, 24, ['absolute', 'right-0']));
			granteeSelect.parent()[0].appendChild(spinnerIcon);

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

				let dropdownParent = _Dialogs.custom.isDialogOpen() ? $(_Dialogs.custom.getDialogBoxElement()) : $('body');

				ownerSelect.select2({
					allowClear: true,
					placeholder: 'Owner',
					width: '300px',
					dropdownParent: dropdownParent,
					templateResult: (state) => templateOption(state, false),
					templateSelection: (state) => templateOption(state, true)
				}).on('select2:unselecting', function(e) {
					e.preventDefault();

					Command.setProperty(id, 'owner', null, false, () => {
						_Helpers.blinkGreen(ownerSelect.parent());
						ownerSelect.val(null).trigger('change');
					});

				}).on('select2:select', function(e) {

					let data = e.params.data;
					Command.setProperty(id, 'owner', data.id, false, () => {
						_Helpers.blinkGreen(ownerSelect.parent());
					});
				});

				granteeSelect.select2({
					placeholder: 'Select Group/User',
					width: '100%',
					dropdownParent: dropdownParent,
					templateResult: (state) => templateOption(state, false)
				}).on('select2:select', function(e) {

					let principalId = e.params.data.id;
					let recursive   = container[0].querySelector('#recursive')?.checked ?? false;

					Command.setPermission(entity.id, principalId, 'grant', 'read', recursive);

					Command.get(principalId, requiredAttributesForPrincipals, (p) => {
						_Entities.addPrincipal(entity, p, { read: true }, allowRecursive, container);
					});
				});

				_Helpers.fastRemoveElement(spinnerIcon);
			});
		};

		if (entity.targetId) {
			Command.getRelationship(id, entity.targetId, 'id,type,name,isFolder,isContent,owner,visibleToPublicUsers,visibleToAuthenticatedUsers', handleGraphObject);
		} else {
			Command.get(id, 'id,type,name,isFolder,isContent,owner,visibleToPublicUsers,visibleToAuthenticatedUsers', handleGraphObject);
		}
	},
	templateForPrincipalOption: (p, selected = false) => `<option value="${p.id}" data-principal="${_Helpers.escapeForHtmlAttributes(JSON.stringify(p))}" ${(selected ? 'selected' : '')}>${p.name}</option>`,
	showAccessControlDialog: (entity) => {

		let id = entity.id;

		let initialObj = {
			ownerId: entity.owner ? entity.owner.id : null,
			visibleToPublicUsers: entity.visibleToPublicUsers,
			visibleToAuthenticatedUsers: entity.visibleToAuthenticatedUsers
		};

		let { dialogText } = _Dialogs.custom.openDialog('Access Control and Visibility', () => {

			if (Structr.isModuleActive(_Crud)) {

				let handleGraphObject = (entity) => {

					if ((!entity.owner && initialObj.owner !== null) || initialObj.ownerId !== entity.owner.id) {
						_Crud.objectList.refreshCellWithNewValue(id, "owner", entity.owner, entity.type, initialObj.ownerId);
					}

					_Crud.objectList.refreshCellWithNewValue(id, 'visibleToPublicUsers',        entity.visibleToPublicUsers,        entity.type, initialObj.visibleToPublicUsers);
					_Crud.objectList.refreshCellWithNewValue(id, 'visibleToAuthenticatedUsers', entity.visibleToAuthenticatedUsers, entity.type, initialObj.visibleToAuthenticatedUsers);
				};

				if (entity.targetId) {
					Command.getRelationship(id, entity.targetId, 'id,type,owner,visibleToPublicUsers,visibleToAuthenticatedUsers', handleGraphObject);
				} else {
					Command.get(id, 'id,type,owner,visibleToPublicUsers,visibleToAuthenticatedUsers', handleGraphObject);
				}

			} else if (Structr.isModuleActive(_Security)) {
				_ResourceAccessPermissions.updateResourcesAccessRow(id, false);
			}
		});

		_Entities.accessControlDialog(entity, $(dialogText));
	},
	addPrincipal: (entity, principal, permissions, allowRecursive, container) => {

		$(`#newPrincipal option[value="${principal.id}"]`, container).remove();

		if ($(`#principals ._${principal.id}`, container).length > 0) {
			return;
		}

		let row = $(`
			<tr class="_${principal.id}">
				<td>
					<div class="flex items-center gap-2">${_Icons.getIconForPrincipal(principal)}<span class="name">${principal.name}</span></div>
				</td>
			</tr>
		`);
		$('#new', container).after(row);

		for (let perm of ['read', 'write', 'delete', 'accessControl']) {

			row.append(`<td><input class="${perm}" type="checkbox" data-permission="${perm}"${permissions[perm] ? ' checked="checked"' : ''}"></td>`);

			$(`.${perm}`, row).on('dblclick', () => {
				return false;
			});

			$(`.${perm}`, row).on('click', function(e) {
				e.preventDefault();

				let checkbox = $(this);
				checkbox.prop('disabled', true);

				if (!$('input:checked', row).length) {

					$('#newPrincipal', container).append(_Entities.templateForPrincipalOption(principal));

					row.remove();
				}

				let recursive = $('#recursive', container).is(':checked');

				Command.setPermission(entity.id, principal.id, permissions[perm] ? 'revoke' : 'grant', perm, recursive, () => {

					permissions[perm] = !permissions[perm];
					checkbox.prop('checked', permissions[perm]);

					checkbox.prop('disabled', false);

					_Helpers.blinkGreen(checkbox.parent());
				});
			});
		}

		if (allowRecursive) {

			row.append('<td><button class="apply-to-child-nodes hover:bg-gray-100 focus:border-gray-666 active:border-green">Apply to child nodes</button></td>');

			let button = row[0].querySelector('button.apply-to-child-nodes');

			button.addEventListener('click', (e) => {

				button.setAttribute('disabled', 'disabled');

				let permissions = [...row[0].querySelectorAll('input:checked')].map(i => i.dataset.permission).join(',');

				// this does not apply to shared component sync ==> syncMode = NONE
				Command.setPermission(entity.id, principal.id, 'setAllowed', permissions, true, () => {

					button.removeAttribute('disabled');
					_Helpers.blinkGreen(row);
				});
			});
		}
	},
	appendBooleanSwitch: function(el, entity, key, labels, desc, recElementId) {

		if (!el || !entity) {
			return false;
		}
		el.append(`<div class="${entity.id}_ flex items-center mt-2"><button class="switch inactive ${key}_ inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green min-w-56"></button>${desc}</div>`);

		let sw = $(`.${key}_`, el);
		_Entities.changeBooleanAttribute(sw, entity[key], labels[0], labels[1]);

		sw.on('click', function(e) {
			e.stopPropagation();
			Command.setProperty(entity.id, key, sw.hasClass('inactive'), $(recElementId, el).is(':checked'), (obj) => {
				if (obj.id !== entity.id) {
					return false;
				}
				_Entities.changeBooleanAttribute(sw, obj[key], labels[0], labels[1]);
				_Helpers.blinkGreen(sw);
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

			keyIcon = $(_Icons.getSvgIcon(_Icons.getAccessControlIconId(entity), 16, 16, iconClasses, 'Access Control'));
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

				svgKeyIcon.classList.toggle('node-action-icon', !isProtected);
			}
		}
	},
	appendContextMenuIcon: (parent, entity, visible) => {

		let contextMenuIconClass = 'context_menu_icon';
		let icon                 = parent.querySelector('.' + contextMenuIconClass);

		if (!icon) {
			icon = _Helpers.createSingleDOMElementFromHTML(_Icons.getSvgIcon(_Icons.iconKebabMenu, 16, 16, _Icons.getSvgIconClassesNonColorIcon([contextMenuIconClass, 'node-action-icon']), 'Context-Menu'));
			parent.appendChild(icon);
		}

		icon.addEventListener('click', (e) => {
			e.stopPropagation();
			_Elements.contextMenu.activateContextMenu(e, parent, entity);
		});

		if (visible) {
			icon.style.visibility = 'visible';
			icon.style.display    = 'inline-block';
		}
	},
	appendExpandIcon: (nodeContainer, entity, hasChildren, expanded, callback) => {

		let button = $(nodeContainer.children('.expand_icon_svg').first());
		if (button && button.length) {
			return;
		}

		if (hasChildren) {

			let typeIcon    = $(nodeContainer.children('.typeIcon').first());
			let icon        = expanded ? _Icons.expandedClass : _Icons.collapsedClass;

			typeIcon.removeClass(_Icons.typeIconClassNoChildren).before(`<i class="expand_icon_svg ${icon}" draggable="false"></i>`);

			button = $(nodeContainer.children('.expand_icon_svg').first());

			if (button) {

				button[0].addEventListener('click', (e) => {
					e.stopPropagation();
					_Entities.toggleElement(entity.id, nodeContainer, undefined, callback);
				});

				if (expanded) {
					_Entities.ensureExpanded(nodeContainer);
				}
			}

		} else {
			nodeContainer.children('.typeIcon').addClass(_Icons.typeIconClassNoChildren);
		}

		_Pages.registerDetailClickHandler($(nodeContainer), entity);
	},
	removeExpandIcon: (el) => {
		if (!el)
			return;

		if (el.hasClass('node')) {
			el = el.children('.node-container');
		}

		let expandIcon = $(el.children('.expand_icon_svg').first());
		expandIcon.remove();

		el.children('.typeIcon').addClass(_Icons.typeIconClassNoChildren);
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

		_Entities.setMouseOverHandlers(nodeId, el, node, isComponent, syncedNodesIds);
	},
	setMouseOverHandlers: (nodeId, el, node, isComponent = false, syncedNodesIds = []) => {

		node.on({
			mouseover: function(e) {

				if (_Dragndrop.dragActive === true) {
					return;
				}

				e.stopPropagation();
				var self = $(this);
				$('#componentId_' + nodeId).addClass('nodeHover');
				if (isComponent) {
					$('#id_' + nodeId).addClass('nodeHover');
				}

				syncedNodesIds.forEach(function(s) {
					$('#id_' + s).addClass('nodeHover');
					$('#componentId_' + s).addClass('nodeHover');
				});

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

				if (_Dragndrop.dragActive === true) {
					return;
				}

				e.stopPropagation();
				$('#componentId_' + nodeId).removeClass('nodeHover');
				if (isComponent) {
					$('#id_' + nodeId).removeClass('nodeHover');
				}

				syncedNodesIds.forEach(function(s) {
					$('#id_' + s).removeClass('nodeHover');
					$('#componentId_' + s).removeClass('nodeHover');
				});

				_Entities.resetMouseOverState(this);
			}
		});
	},
	resetMouseOverState: (element) => {
		let el   = $(element);
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
		let icon = _Entities.getIconFromElement($(el));
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
		let node = document.querySelector('#id_' + selectedElementId);

		node?.querySelector('.node-container')?.classList.add('node-selected');
	},
	selectElement: (element, entity) => {

		_Entities.removeSelectedNodeClassFromAllNodes();

		element.querySelector('.node-container').classList.add('node-selected');

		_Entities.selectedObject = entity;
		LSWrapper.setItem(_Entities.selectedObjectIdKey, entity.id);
	},
	removeSelectedNodeClassFromAllNodes: () => {

		for (let activeSelector of document.querySelectorAll('.node-selected')) {
			activeSelector.classList.remove('node-selected');
		}
	},
	toggleElement: (id, el, isExpanded, callback) => {

		let nodeContainer = $(el);

		if (!nodeContainer.hasClass('node-container')) {
			nodeContainer = nodeContainer.children('.node-container').first();
		}

		let toggleIcon = nodeContainer.children('.expand_icon_svg').first();

		if (_Entities.isExpanded(nodeContainer)) {

			_Entities.removeChildNodesFromUI(nodeContainer.closest('.node'));

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
	removeChildNodesFromUI: (node) => {

		for (let childNode of node[0].querySelectorAll(':scope > .node')) {
			_Helpers.fastRemoveElement(childNode);
		}
	},
	makeAttributeEditable: (parentElement, id, attributeSelector, attributeName, callback) => {

		let attributeElement        = parentElement.find(attributeSelector).first();
		let additionalInputClass    = attributeElement.data('inputClass') || '';
		let oldValue                = $.trim(attributeElement.attr('title'));
		let attributeElementRawHTML = attributeElement[0].outerHTML;

		attributeElement.replaceWith(`<input type="text" size="${oldValue.length + 4}" class="new-${attributeName} ${additionalInputClass}" value="${oldValue}">`);

		let input = $('input', parentElement);
		input.focus().select();

		let restoreNonEditableTag = (el, text) => {

			let newEl = $(attributeElementRawHTML);
			newEl.text(text);
			newEl.attr('title', text);
			el.replaceWith(newEl);

			parentElement.find(attributeSelector).first().off('click').on('click', (e) => {
				e.stopPropagation();
				_Entities.makeAttributeEditable(parentElement, id, attributeSelector, attributeName, callback);
			});

			return newEl;
		};

		let saveAndUpdate = (el) => {

			let newVal = el.val();

			// first restore old value. only update with new value if save succeeds
			let newEl = restoreNonEditableTag(el, oldValue);

			let successFunction = () => {

				let finalEl = restoreNonEditableTag(newEl, newVal);

				callback?.(finalEl);
			};

			_Entities.setNewAttributeValue(parentElement, id, attributeName, newVal, successFunction, () => {

				let attributeElement = parentElement.find(attributeSelector).first();
				attributeElement.attr('title', oldValue).text(oldValue);
				_Helpers.blinkRed(parentElement);
			});
		};

		input.on('blur', () => {
			saveAndUpdate(input);
		});

		input.keydown(function(e) {

			if (e.keyCode === 13) {

				saveAndUpdate(input);

			} else if (e.keyCode === 27) {
				e.stopPropagation();
				restoreNonEditableTag(input, oldValue);
			}
		});
	},
	makeNameEditable: (element, callback) => {
		let id = Structr.getId(element);
		_Entities.makeAttributeEditable(element, id, 'b.name_', 'name', callback);
	},
	setNewAttributeValue: (element, id, attributeName, newValue, callback, failCallback) => {

		Command.setProperty(id, attributeName, newValue, false, (entity, resultSize, errorOccurred) => {

			if (!errorOccurred || errorOccurred === false) {

				_Helpers.blinkGreen(element.find(`.${attributeName}_`).first());

				if (Structr.isModuleActive(_Pages)) {

//					_Pages.reloadPreviews();
// 					console.log('reload preview?');

				} else if (Structr.isModuleActive(_Files) && attributeName === 'name') {

					let a = element.closest('td').prev().children('a').first();

					Command.getProperty(id, 'path', (newPath) => {
						a.attr('href', newPath);
					});
				}

				callback?.();

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
	setPropertyWithFeedback: (entity, key, newVal, input, blinkEl) => {

		const oldVal = entity[key];
		input.val(oldVal);

		Command.setProperty(entity.id, key, newVal, false, (result) => {

			let newVal = result[key];

			// update entity so this works multiple times
			entity[key] = newVal;

			if (key === 'password' || newVal !== oldVal) {
				_Helpers.blinkGreen(input);
				if (blinkEl) {
					_Helpers.blinkGreen(blinkEl);
				}
				if (newVal && newVal.constructor === Array) {
					newVal = newVal.join(',');
				}
				input.val(newVal);
				let valueMsg = (newVal !== undefined || newVal !== null) ? `value "${newVal}` : 'empty value';
				_Dialogs.custom.showAndHideInfoBoxMessage(`Updated property "${key}" with ${valueMsg}`, 'success', 2000, 200);

			} else {

				input.val(oldVal);
			}
		});
	},
	templates: {
		propertyRow: config => `
			<tr class="${config.rowClass}">
				<td class="key">
					<span class="flex justify-between items-center">
						<span>${config.displayKey}</span>
						<span class="${_Entities.classNameForEmptyStringWarningContainer} flex"></span>
					</span>
				</td>
				<td class="value ${config.key}_">${_Helpers.formatValueInputField(config.key, config.value, config.typeInfo)}</td>
				<td>${_Entities.getNullIconForKey(config.key)}</td>
			</tr>
		`
	},

	generalTab: {
		dialogs: {
			defaultDom: async (el, entity, typeInfo) => {

				let enrichedEntity = await _Entities.generalTab.addHtmlPropertiesToEntity(entity);

				el.html(_Entities.generalTab.templates.defaultDOMOptions({ entity: enrichedEntity, typeInfo }));

				_Entities.generalTab.populateInputFields(el, enrichedEntity);
				_Entities.generalTab.registerSimpleInputChangeHandlers(el, enrichedEntity);

				_Entities.generalTab.focusInput(el);

				await _Entities.generalTab.showCustomProperties(el, entity);
				_Entities.generalTab.activateShowHideConditionOptions(el, entity);
				_Entities.generalTab.showRenderingOptions(el, entity);

				_Entities.generalTab.showChildContentEditor(el, entity);

				_Entities.generalTab.showSharedComponentConfigurationEditor(el, entity);
			},
			a: async (el, entity, typeInfo) => {

				let enrichedEntity = await _Entities.generalTab.addHtmlPropertiesToEntity(entity);

				el.html(_Entities.generalTab.templates.aOptions({ entity: enrichedEntity, typeInfo }));

				_Entities.generalTab.populateInputFields(el, enrichedEntity);
				_Entities.generalTab.registerSimpleInputChangeHandlers(el, enrichedEntity);

				_Entities.generalTab.focusInput(el);

				await _Entities.generalTab.showCustomProperties(el, entity);
				_Entities.generalTab.activateShowHideConditionOptions(el, entity);
				_Entities.generalTab.showRenderingOptions(el, entity);

				_Entities.generalTab.showChildContentEditor(el, entity);

				_Entities.generalTab.showSharedComponentConfigurationEditor(el, entity);
			},
			button: async (el, entity, typeInfo) => {

				let enrichedEntity = await _Entities.generalTab.addHtmlPropertiesToEntity(entity);

				el.html(_Entities.generalTab.templates.buttonOptions({ entity: enrichedEntity, typeInfo }));

				_Entities.generalTab.populateInputFields(el, enrichedEntity);
				_Entities.generalTab.registerSimpleInputChangeHandlers(el, enrichedEntity);

				_Entities.generalTab.focusInput(el);

				await _Entities.generalTab.showCustomProperties(el, entity);
				_Entities.generalTab.activateShowHideConditionOptions(el, entity);
				_Entities.generalTab.showRenderingOptions(el, entity);

				_Entities.generalTab.showChildContentEditor(el, entity);

				_Entities.generalTab.showSharedComponentConfigurationEditor(el, entity);
			},
			content: async (el, entity, typeInfo) => {

				el.html(_Entities.generalTab.templates.contentOptions({ entity: entity, typeInfo }));

				_Entities.generalTab.populateInputFields(el, entity);
				_Entities.generalTab.registerSimpleInputChangeHandlers(el, entity);

				_Entities.generalTab.focusInput(el);

				await _Entities.generalTab.showCustomProperties(el, entity);
				_Entities.generalTab.activateShowHideConditionOptions(el, entity);
			},
			div: async (el, entity, typeInfo) => {

				let enrichedEntity = await _Entities.generalTab.addHtmlPropertiesToEntity(entity);

				el.html(_Entities.generalTab.templates.divOptions({ entity: enrichedEntity, typeInfo }));

				_Entities.generalTab.populateInputFields(el, enrichedEntity);
				_Entities.generalTab.registerSimpleInputChangeHandlers(el, enrichedEntity);

				_Entities.generalTab.focusInput(el);

				await _Entities.generalTab.showCustomProperties(el, entity);
				_Entities.generalTab.activateShowHideConditionOptions(el, entity);
				_Entities.generalTab.showRenderingOptions(el, entity);

				_Entities.generalTab.showChildContentEditor(el, entity);

				_Entities.generalTab.showSharedComponentConfigurationEditor(el, entity);
			},
			option: async (el, entity, typeInfo) => {

				let enrichedEntity = await _Entities.generalTab.addHtmlPropertiesToEntity(entity);

				el.html(_Entities.generalTab.templates.optionOptions({ entity: enrichedEntity, typeInfo }));

				_Entities.generalTab.populateInputFields(el, enrichedEntity);
				_Entities.generalTab.registerSimpleInputChangeHandlers(el, enrichedEntity);

				_Entities.generalTab.focusInput(el);

				await _Entities.generalTab.showCustomProperties(el, entity);
				_Entities.generalTab.activateShowHideConditionOptions(el, entity);
				_Entities.generalTab.showRenderingOptions(el, entity);

				_Entities.generalTab.showChildContentEditor(el, entity);

				_Entities.generalTab.showSharedComponentConfigurationEditor(el, entity);
			},
			file: async (el, entity) => {

				el.html(_Entities.generalTab.templates.fileOptions({ file: entity }));

				_Entities.generalTab.populateInputFields(el, entity);
				_Entities.generalTab.registerSimpleInputChangeHandlers(el, entity);

				_Entities.generalTab.focusInput(el);
			},
			folder: async (el, entity) => {

				el.html(_Entities.generalTab.templates.folderOptions({ file: entity }));

				_Entities.generalTab.populateInputFields(el, entity);
				_Entities.generalTab.registerSimpleInputChangeHandlers(el, entity);
			},
			input: async (el, entity) => {

				let enrichedEntity = await _Entities.generalTab.addHtmlPropertiesToEntity(entity);

				el.html(_Entities.generalTab.templates.inputOptions({ entity: enrichedEntity }));

				_Entities.generalTab.populateInputFields(el, enrichedEntity);
				_Entities.generalTab.registerSimpleInputChangeHandlers(el, enrichedEntity);

				_Entities.generalTab.focusInput(el);

				await _Entities.generalTab.showCustomProperties(el, entity);
				_Entities.generalTab.activateShowHideConditionOptions(el, entity);
				_Entities.generalTab.showRenderingOptions(el, entity);
			},
			ldapGroup: async (el, entity) => {

				el.html(_Entities.generalTab.templates.ldapGroup({ group: entity }));

				let dnInput     = $('input#ldap-group-dn');
				let pathInput   = $('input#ldap-group-path');
				let filterInput = $('input#ldap-group-filter');
				let scopeInput  = $('input#ldap-group-scope');

				_Entities.generalTab.registerSimpleInputChangeHandlers(el, entity);

				// dialog logic here..
				$('.clear-ldap-group-dn', el).on('click', () => { _Entities.generalTab.setNull(entity.id, 'distinguishedName', dnInput); });
				$('.clear-ldap-group-path', el).on('click', () => { _Entities.generalTab.setNull(entity.id, 'path', pathInput); });
				$('.clear-ldap-group-filter', el).on('click', () => { _Entities.generalTab.setNull(entity.id, 'filter', filterInput); });
				$('.clear-ldap-group-scope', el).on('click', () => { _Entities.generalTab.setNull(entity.id, 'scope', scopeInput); });

				$('button#ldap-sync-button').on('click', async () => {

					let response = await fetch(`${Structr.rootUrl}${entity.type}/${entity.id}/update`, {
						method: 'POST'
					});

					if (response.ok) {
						_Dialogs.custom.showAndHideInfoBoxMessage('Updated LDAP group successfully', 'success', 2000, 200);
					} else {
						_Dialogs.custom.showAndHideInfoBoxMessage('LDAP group could not be updated', 'warning', 5000, 200);
					}
				});

				_Entities.generalTab.focusInput(el);
			},
			user: async (el, entity) => {

				el.html(_Entities.generalTab.templates.userOptions({ entity: entity, user: entity }));

				_Entities.generalTab.populateInputFields(el, entity);
				_Entities.generalTab.registerSimpleInputChangeHandlers(el, entity);

				$('button#set-password-button').on('click', (e) => {
					let input = $('input#password-input');
					_Entities.setPropertyWithFeedback(entity, 'password', input.val(), input);
				});

				_Entities.generalTab.focusInput(el);

				_Entities.generalTab.showCustomProperties(el, entity);
			},
			page: async (el, entity) => {

				el.html(_Entities.generalTab.templates.pageOptions({ entity: entity, page: entity }));

				_Entities.generalTab.populateInputFields(el, entity);
				_Entities.generalTab.registerSimpleInputChangeHandlers(el, entity);

				_Pages.previews.configurePreview(entity, el[0]);

				_Entities.generalTab.focusInput(el);

				await _Entities.generalTab.showCustomProperties(el, entity);
			},
		},
		getGeneralTabConfig: (entity) => {

			let registeredDialogs = {
				'DEFAULT_DOM_NODE': { id: 'general', title: 'General',     appendDialogForEntityToContainer: _Entities.generalTab.dialogs.defaultDom },
				'A':                { id: 'general', title: 'General',     appendDialogForEntityToContainer: _Entities.generalTab.dialogs.a },
				'Button':           { id: 'general', title: 'General',     appendDialogForEntityToContainer: _Entities.generalTab.dialogs.button },
				'Content':          { id: 'general', title: 'General',     appendDialogForEntityToContainer: _Entities.generalTab.dialogs.content },
				'Div':              { id: 'general', title: 'General',     appendDialogForEntityToContainer: _Entities.generalTab.dialogs.div },
				'File':             { id: 'general', title: 'General',     appendDialogForEntityToContainer: _Entities.generalTab.dialogs.file },
				'Image':            { id: 'general', title: 'General',     appendDialogForEntityToContainer: _Entities.generalTab.dialogs.file },
				'Folder':           { id: 'general', title: 'General',     appendDialogForEntityToContainer: _Entities.generalTab.dialogs.folder },
				'Input':            { id: 'general', title: 'General',     appendDialogForEntityToContainer: _Entities.generalTab.dialogs.input },
				'LDAPGroup':        { id: 'general', title: 'LDAP Config', appendDialogForEntityToContainer: _Entities.generalTab.dialogs.ldapGroup },
				'Option':           { id: 'general', title: 'General',     appendDialogForEntityToContainer: _Entities.generalTab.dialogs.option },
				'Page':             { id: 'general', title: 'General',     appendDialogForEntityToContainer: _Entities.generalTab.dialogs.page },
				'Template':         { id: 'general', title: 'General',     appendDialogForEntityToContainer: _Entities.generalTab.dialogs.content },
				'User':             { id: 'general', title: 'General',     appendDialogForEntityToContainer: _Entities.generalTab.dialogs.user }
			};

			let dialogConfig = registeredDialogs[entity.type];

			if (!dialogConfig && entity.isDOMNode) {
				dialogConfig = registeredDialogs['DEFAULT_DOM_NODE'];
			}

			if (!dialogConfig && entity.isUser) {
				dialogConfig = registeredDialogs['User'];
			}

			if (!dialogConfig && entity.isFile) {
				dialogConfig = registeredDialogs['File'];
			}

			if (!dialogConfig && entity.isFolder) {
				dialogConfig = registeredDialogs['Folder'];
			}

			return dialogConfig;
		},
		appendGeneralTypeTab: (entity, mainTabs, contentEl, typeInfo) => {

			let dialogConfig = _Entities.generalTab.getGeneralTabConfig(entity);

			if (dialogConfig) {

				if (dialogConfig.condition === undefined || (typeof dialogConfig.condition === 'function' && dialogConfig.condition())) {

					let wrapperFn = (contentElement) => {

						dialogConfig.appendDialogForEntityToContainer($(contentElement), entity, typeInfo).then(() => {
							_Helpers.activateCommentsInElement(contentElement);
						});
					}

					// call method with the same callback object for initial callback and show callback
					let tabContent = _Entities.appendPropTab(entity, mainTabs, contentEl, dialogConfig.id, dialogConfig.title, true, wrapperFn, true);

					wrapperFn(tabContent);
				}
			}
		},
		showCustomProperties: async (el, entity) => {

			return new Promise((resolve, reject) => {

				let customContainer = $('div#custom-properties-container', el);

				_Schema.caches.getTypeInfo(entity.type, (typeInfo) => {

					_Entities.listProperties(entity, 'custom', customContainer, typeInfo, (propertiesInfo) => {

						// filter out id,name,type from properties
						let customProperties = Object.keys(propertiesInfo).filter(key => !['id', 'type', 'name'].includes(key));

						// make container visible when custom properties exist
						if (customProperties.length > 0) {
							$('div#custom-properties-parent').removeClass("hidden");
						}

						$('input.dateField', customContainer).each(function(i, input) {
							_Entities.activateDatePicker($(input));
						});

						resolve();
					});
				});
			});
		},
		activateShowHideConditionOptions: (el) => {

			for (let showConditionExample of el[0].querySelectorAll('.example-condition')) {
				showConditionExample.onclick = function() {
					let input   = this.closest('.conditions-container').querySelector('input');
					input.value = this.dataset['value'];
					input.dispatchEvent(new Event('change'));
				}
			}
		},
		showChildContentEditor:(el, entity) => {

			let textContentContainer = el.find('#child-content-editor')[0];

			if (entity && entity.children && entity.children.length === 1 && entity.children[0].type === 'Content') {

				if (textContentContainer) {

					textContentContainer.classList.remove('hidden');

					let child    = entity.children[0];
					let modelObj = StructrModel.obj(child.id) ?? child;

					let populateDialog = (child) => {
						_Entities.generalTab.populateInputFields($(textContentContainer), child);
						_Entities.generalTab.registerDeferredSimpleInputChangeHandlers($(textContentContainer), child, true);
					};

					if (!modelObj.content) {
						Command.get(modelObj.id, 'id,type,content', populateDialog);
					} else {
						populateDialog(modelObj);
					}
				}
			}
		},
		showSharedComponentConfigurationEditor: (el, entity) => {

			let textContentContainer = el.find('#shared-component-configuration-editor')[0];

			if (entity && (entity.sharedComponent?.id || entity.sharedComponentId)) {

				if (textContentContainer) {

					textContentContainer.classList.remove('hidden');
				}
			}
		},
		showRenderingOptions: (el, entity) => {

			// Disable render options on certain elements
			if (['html', 'body', 'head', 'title', 'style', 'meta', 'link', 'script', 'base'].includes(entity.tag)) {
				return;
			}

			let renderingOptionsContainer = $('#rendering-options-container', el);

			if (renderingOptionsContainer.length) {

				renderingOptionsContainer.removeClass('hidden');

				let renderingModeSelect = $('select#rendering-mode-select', renderingOptionsContainer);
				renderingModeSelect.select2({
					width: '100%'
				});

				Command.getProperty(entity.id, 'data-structr-rendering-mode', (result) => {
					renderingModeSelect.val(result);
					renderingModeSelect.trigger('change');
				});

				renderingModeSelect.on('change', () => {
					let renderingMode = renderingModeSelect.val() === '' ? null : renderingModeSelect.val();
					_Entities.setPropertyWithFeedback(entity, 'data-structr-rendering-mode', renderingMode, renderingModeSelect, null);
				});
			}
		},
		getValueFromFormElement: (el) => {

			if (el.tagName === 'SELECT' && el.multiple === true) {
				return [].map.call(el.selectedOptions, (o) => o.value);
			} else if (el.tagName === 'INPUT' && el.type === 'date') {
				return new Date(el.value);
			} else if (el.tagName === 'INPUT' && el.type === 'checkbox') {
				return el.checked;
			} else if (el.tagName === 'INPUT' && el.type === 'radio') {
				if (el.checked === true) {
					return el.value;
				} else {
					return null;
				}
			}

			return el.value;
		},
		registerDeferredSimpleInputChangeHandlers: (el, entity, emptyStringInsteadOfNull) => {

			_Entities.generalTab.registerSimpleInputChangeHandlers(el, entity, emptyStringInsteadOfNull, true);
		},
		registerSimpleInputChangeHandlers: (el, entity, emptyStringInsteadOfNull, isDeferredChangeHandler = false) => {

			for (let inputEl of el[0].querySelectorAll('textarea[name], input[name]')) {

				let shouldDeferChangeHandler = inputEl.dataset['deferChangeHandler'];

				if (shouldDeferChangeHandler !== 'true' || (shouldDeferChangeHandler === 'true' && isDeferredChangeHandler === true) ) {

					inputEl.addEventListener('change', () => {

						let key      = inputEl.name;
						let oldVal   = entity[key];
						let newVal   = _Entities.generalTab.getValueFromFormElement(inputEl);
						let isChange = (oldVal !== newVal) && !((oldVal === null || oldVal === undefined) && newVal === '');

						if (isChange) {

							let blinkElement = (inputEl.type === 'checkbox') ? $(inputEl).parent() : null;

							_Entities.setPropertyWithFeedback(entity, key, newVal || (emptyStringInsteadOfNull ? '' : null), $(inputEl), blinkElement);
						}
					});
				}
			}
		},
		populateInputFields: (el, entity) => {

			for (let inputEl of el[0].querySelectorAll('textarea[name], input[name]')) {

				let val = entity[inputEl.name];
				if (val != undefined && val != null) {
					if (inputEl.type === 'checkbox') {
						inputEl.checked = val;
					} else {
						inputEl.value = val;
					}
				}
			}
		},
		focusInput: (el, selector) => {

			if (selector) {
				$(selector, el).focus().select();
			} else {
				$('input:first', el).focus().select();
			}
		},
		setNull: (id, key, input) => {

			Command.setProperty(id, key, null, false, () => {
				input.val(null);
				_Helpers.blinkGreen(input);
				_Dialogs.custom.showAndHideInfoBoxMessage(`Property "${key}" has been set to null.`, 'success', 2000, 1000);
			});
		},
		addHtmlPropertiesToEntity: async (entity, callback) => {

			let htmlProperties = await Command.getPromise(entity.id, null, '_html_');

			StructrModel.update(Object.assign(htmlProperties, entity));

			return entity;
		},

		templates: {
			nameTile: config => `
				<div class="${(config.doubleWide === true ? 'col-span-2' : '')}">
					<label class="block mb-2" for="name-input">Name</label>
					<input type="text" id="name-input" autocomplete="off" name="name">
				</div>
			`,
			htmlClassTile: config => `
				<div>
					<label class="block mb-2" for="class-input">CSS Class</label>
					<input type="text" id="class-input" name="_html_class">
				</div>
			`,
			htmlIdTile: config => `
				<div>
					<label class="block mb-2" for="id-input">HTML ID</label>
					<input type="text" id="id-input" name="_html_id">
				</div>
			`,
			htmlStyleTile: config => `
				<div class="col-span-2">
					<label class="block mb-2" for="style-input">Style</label>
					<input type="text" id="style-input" name="_html_style">
				</div>
			`,
			htmlTitleTile: config => `
				<div>
					<label class="block mb-2" for="title-input">Title</label>
					<input type="text" id="title-input" name="_html_title">
				</div>
			`,
			htmlTypeTile: config => `
				<div>
					<label class="block mb-2" for="type-input">Type</label>
					<input type="text" id="type-input" name="_html_type">
				</div>
			`,
			aOptions: config => `
				<div id="div-options" class="quick-access-options">

					<div class="grid grid-cols-2 gap-8">

						${_Entities.generalTab.templates.nameTile(Object.assign({ doubleWide: true }, config))}

						${_Entities.generalTab.templates.htmlClassTile(config)}

						${_Entities.generalTab.templates.htmlIdTile(config)}

						<div class="col-span-2">
							<label class="block mb-2" for="href-input">HREF attribute</label>
							<input type="text" id="href-input" name="_html_href">
						</div>

						${_Entities.generalTab.templates.htmlStyleTile(config)}

						${_Entities.generalTab.templates.repeaterPartial(config)}

						${_Entities.generalTab.templates.visibilityPartial(config)}

						${_Entities.generalTab.templates.textContentPartial()}

					</div>

					${_Entities.generalTab.templates.renderingOptions(config)}

					${_Entities.generalTab.templates.customPropertiesPartial(config)}
				</div>
			`,
			buttonOptions: config => `
				<div id="div-options" class="quick-access-options">

					<div class="grid grid-cols-2 gap-8">

						${_Entities.generalTab.templates.nameTile(Object.assign({ doubleWide: true }, config))}

						${_Entities.generalTab.templates.htmlClassTile(config)}

						${_Entities.generalTab.templates.htmlIdTile(config)}

						${_Entities.generalTab.templates.htmlTitleTile(config)}

						${_Entities.generalTab.templates.htmlTypeTile(config)}

						${_Entities.generalTab.templates.htmlStyleTile(config)}

						${_Entities.generalTab.templates.repeaterPartial(config)}

						${_Entities.generalTab.templates.visibilityPartial(config)}

						${_Entities.generalTab.templates.textContentPartial()}

						${_Entities.generalTab.templates.sharedComponentConfigurationPartial(config)}
					</div>

					${_Entities.generalTab.templates.customPropertiesPartial(config)}
				</div>
			`,
			contentOptions: config => `
				<div id="default-dom-options" class="quick-access-options">

					<div class="grid grid-cols-2 gap-8">

						${_Entities.generalTab.templates.nameTile(Object.assign({ doubleWide: true }, config))}

						${_Entities.generalTab.templates.repeaterPartial(config)}

						${_Entities.generalTab.templates.visibilityPartial(config)}

					</div>

					${_Entities.generalTab.templates.customPropertiesPartial(config)}
				</div>
			`,
			textContentPartial: config => `
				<div id="child-content-editor" class="col-span-2 hidden">
					<label class="block mb-2" for="content-input">Text Content</label>
					<textarea id="content-input" name="content" data-defer-change-handler="true"></textarea>
				</div>
			`,
			sharedComponentConfigurationPartial: config => `
				<div id="shared-component-configuration-editor" class="col-span-2 hidden">
					<label class="block mb-2" for="shared-component-configuration-input" ${_Helpers.getDataCommentAttributeForPropertyFromSchemaInfoHint('sharedComponentConfiguration', config.typeInfo)}>Shared Component Configuration</label>
					${Structr.templates.autoScriptTextArea({ wrapperId: 'shared-component-configuration-editor', wrapperClassString: 'col-span-2', textareaId: 'shared-component-configuration-input', textareaAttributeString: 'name="sharedComponentConfiguration"' })}
				</div>
			`,
			customPropertiesPartial: config => `
				<div id="custom-properties-parent" class="hidden">
					<h3>Custom Attributes</h3>
					<div id="custom-properties-container"></div>
				</div>
			`,
			defaultDOMOptions: config => `
				<div id="default-dom-options" class="quick-access-options">

					<div class="grid grid-cols-2 gap-8">

						${_Entities.generalTab.templates.nameTile(Object.assign({ doubleWide: true }, config))}

						${_Entities.generalTab.templates.htmlClassTile(config)}

						${_Entities.generalTab.templates.htmlIdTile(config)}

						${_Entities.generalTab.templates.htmlStyleTile(config)}

						${_Entities.generalTab.templates.repeaterPartial(config)}

						${_Entities.generalTab.templates.visibilityPartial(config)}

						${_Entities.generalTab.templates.textContentPartial()}

						${_Entities.generalTab.templates.sharedComponentConfigurationPartial(config)}
					</div>

					${_Entities.generalTab.templates.renderingOptions(config)}

					${_Entities.generalTab.templates.customPropertiesPartial(config)}
				</div>
			`,
			divOptions: config => `
				<div id="div-options" class="quick-access-options">

					<div class="grid grid-cols-2 gap-8">

						${_Entities.generalTab.templates.nameTile(Object.assign({ doubleWide: true }, config))}

						${_Entities.generalTab.templates.htmlClassTile(config)}

						${_Entities.generalTab.templates.htmlIdTile(config)}

						${_Entities.generalTab.templates.htmlStyleTile(config)}

						${_Entities.generalTab.templates.repeaterPartial(config)}

						${_Entities.generalTab.templates.visibilityPartial(config)}

						${_Entities.generalTab.templates.textContentPartial()}

						${_Entities.generalTab.templates.sharedComponentConfigurationPartial(config)}
					</div>

					${_Entities.generalTab.templates.renderingOptions(config)}

					${_Entities.generalTab.templates.customPropertiesPartial(config)}

				</div>
			`,
			includeInFrontendExport: config => `
				<div class="mb-2 flex items-center">
					<input type="checkbox" name="includeInFrontendExport" id="includeInFrontendExport">
					<label for="includeInFrontendExport" data-comment-config='{"insertAfter":true}' data-comment="If checked this file/folder is exported in the deployment process. If a parent folder has this flag enabled, it will automatically be exported and the flag does not need to be set.">Include in frontend export</label>
				</div>
			`,
			fileOptions: config => `
				<div id="file-options" class="quick-access-options">

					<div class="grid grid-cols-2 gap-8">

						${_Entities.generalTab.templates.nameTile(config)}

						<div>
							<label class="block mb-2" for="content-type-input">Content Type</label>
							<input type="text" id="content-type-input" autocomplete="off" name="contentType">
						</div>

						<div>
							<label class="block mb-2" for="cache-for-seconds-input" class="block">Cache for n seconds</label>
							<input type="text" id="cache-for-seconds-input" value="" name="cacheForSeconds">
						</div>

						<div>

							<label class="block mb-2">Options</label>

							<div class="mb-2 flex items-center">
								<input type="checkbox" name="isTemplate" id="isTemplate">
								<label for="isTemplate">Is template (dynamic file)</label>
							</div>

							<div class="mb-2 flex items-center">
								<input type="checkbox" name="dontCache" id="dontCache">
								<label for="dontCache">Caching disabled</label>
							</div>

							${_Entities.generalTab.templates.includeInFrontendExport(config)}
						</div>
					</div>
				</div>
			`,
			folderOptions: config => `
				<div id="file-options" class="quick-access-options">

					<div class="grid grid-cols-2 gap-8">

						${_Entities.generalTab.templates.nameTile(config)}

						<div>

							<label class="block mb-2">Options</label>

							${_Entities.generalTab.templates.includeInFrontendExport(config)}

						</div>
					</div>
				</div>
			`,
			inputOptions: config => `
				<div id="div-options" class="quick-access-options">

					<div class="grid grid-cols-2 gap-8">

						${_Entities.generalTab.templates.nameTile(Object.assign({ doubleWide: true }, config))}

						${_Entities.generalTab.templates.htmlClassTile(config)}

						${_Entities.generalTab.templates.htmlIdTile(config)}

						${_Entities.generalTab.templates.htmlTypeTile(config)}

						<div>
							<label class="block mb-2" for="placeholder-input">Placeholder</label>
							<input type="text" id="placeholder-input" name="_html_placeholder">
						</div>

						${_Entities.generalTab.templates.htmlStyleTile(config)}

						${_Entities.generalTab.templates.htmlTitleTile(config)}

						${_Entities.generalTab.templates.visibilityPartial(config)}

					</div>

					${_Entities.generalTab.templates.customPropertiesPartial(config)}
				</div>
			`,
			ldapGroup: config => `
				<div id="ldap-group-config">
					<h3>Synchronize this group using distinguished name (prioritized if set)</h3>

					<div class="mb-3">
						<input type="text" size="80" id="ldap-group-dn" placeholder="Distinguished Name" name="distinguishedName">
						${_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['clear-ldap-group-dn', 'icon-lightgrey', 'cursor-pointer']), 'Clear value')}
					</div>

					<h3>Synchronize this group using path, filter and scope (if distinguished name not set above)</h3>

					<div class="mb-3">
						<input type="text" size="80" id="ldap-group-path" placeholder="Path" name="path">
						${_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['clear-ldap-group-path', 'icon-lightgrey', 'cursor-pointer']), 'Clear value')}
					</div>

					<div class="mb-3">
						<input type="text" size="80" id="ldap-group-filter" placeholder="Filter" name="filter">
						${_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['clear-ldap-group-filter', 'icon-lightgrey', 'cursor-pointer']), 'Clear value')}
					</div>

					<div class="mb-3">
						<input type="text" size="80" id="ldap-group-scope" placeholder="Scope" name="scope">
						${_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['clear-ldap-group-scope', 'icon-lightgrey', 'cursor-pointer']), 'Clear value')}
					</div>

					<div class="mb-3">
						<button type="button" class="action" id="ldap-sync-button">Synchronize now</button>
					</div>

					<div>
						<a href="/structr/config" target="_blank">Open Structr configuration</a>
					</div>
				</div>
			`,
			optionOptions: config => `
				<div id="div-options" class="quick-access-options">

					<div class="grid grid-cols-2 gap-8">

						${_Entities.generalTab.templates.nameTile(Object.assign({ doubleWide: true }, config))}

						${_Entities.generalTab.templates.htmlClassTile(config)}

						${_Entities.generalTab.templates.htmlIdTile(config)}

						${_Entities.generalTab.templates.htmlStyleTile(config)}

						${_Entities.generalTab.templates.repeaterPartial(config)}

						<div>
							<label class="block mb-2" for="selected-input">Selected</label>
							<input type="text" id="selected-input" name="_html_selected">
						</div>

						<div>
							<label class="block mb-2" for="selected-values-input" data-comment="This is a shortcut to automatically select options based on their appearance in the collection returned by this script expression and is mutually exclusive with the <code>selected</code> attribute. It is mainly intended for database objects. Should the repeater use custom objects, use a scripting expression in the <code>selected</code> attribute like so: <code>\${is(logicStatement, 'selected')}</code>">Selected Values Expression</label>
							${Structr.templates.autoScriptInput({ inputAttributeString: 'id="selected-values-input" name="selectedValues"', wrapperClassString: 'w-full'})}
						</div>

						<div>
							<label class="block mb-2" for="value-input">Value</label>
							<input type="text" id="value-input" name="_html_value">
						</div>

						<div><!-- occupy space in grid UI --></div>

						${_Entities.generalTab.templates.visibilityPartial(config)}

						${_Entities.generalTab.templates.textContentPartial()}

						${_Entities.generalTab.templates.sharedComponentConfigurationPartial(config)}
					</div>

					${_Entities.generalTab.templates.renderingOptions(config)}

					${_Entities.generalTab.templates.customPropertiesPartial(config)}

				</div>
			`,
			pageOptions: config => `
				<div id="div-options" class="quick-access-options">

					<div class="grid grid-cols-2 gap-8">

						${_Entities.generalTab.templates.nameTile(config)}

						<div>
							<label class="block mb-2" for="content-type-input">Content Type</label>
							<input type="text" id="content-type-input" name="contentType">
						</div>

						<div>
							<label class="block mb-2" for="category-input">Category</label>
							<input type="text" id="category-input" name="category">
						</div>

						<div>
							<label class="block mb-2" for="show-on-error-codes-input">Show on Error Codes</label>
							<input type="text" id="show-on-error-codes-input" name="showOnErrorCodes">
						</div>

						<div>
							<label class="block mb-2" for="position-input" data-comment="The position is important to identify the page, the client will get served for the '/' path. The page with the lowest position (which is visible for the requesting client) will be shown.">Position</label>
							<input type="text" id="position-input" name="position">
						</div>

						<div>
							<label class="block mb-2" for="path-input" data-comment="If set, the page will be available under this path and not under its name.">Custom Path</label>
							<input type="text" id="path-input" name="path">
						</div>

						<div><!-- occupy space in grid UI --></div>

						<div>

							<label class="block mb-2">Options</label>

							<div class="mb-2 flex items-center">
								<label for="dont-cache-checkbox" data-comment="Especially important for dynamic pages which are visible to public users.">
									<input type="checkbox" name="dontCache" id="dont-cache-checkbox"> Caching disabled
								</label>
							</div>

							<div class="mb-2 flex items-center">
								<label for="page-creates-raw-data-checkbox">
									<input type="checkbox" name="pageCreatesRawData" id="page-creates-raw-data-checkbox"> Use binary encoding for output
								</label>
							</div>

							<div class="mb-2 flex items-center">
								<label for="_auto-refresh" data-comment="Auto-refresh page preview on changes (if page preview is active)">
									<input id="_auto-refresh" type="checkbox" ${(LSWrapper.getItem(_Pages.autoRefreshDisabledKey + config.entity.id) ? '' : ' checked="checked"')}> Auto-refresh
								</label>
							</div>

						</div>

						<div>
							<label class="block mb-2" for="_details-object-id" data-comment="UUID of detail object to append to preview URL">Preview Detail Object</label>
							<input id="_details-object-id" type="text" value="${(LSWrapper.getItem(_Pages.detailsObjectIdKey + config.entity.id) ? LSWrapper.getItem(_Pages.detailsObjectIdKey + config.entity.id) : '')}">
						</div>

						<div>
							<label class="block mb-2" for="_request-parameters" data-comment="Request parameters to append to preview URL">Preview Request Parameters</label>
							<div class="flex items-center">
								<code>?</code>
								<input id="_request-parameters" type="text" value="${(LSWrapper.getItem(_Pages.requestParametersKey + config.entity.id) ? LSWrapper.getItem(_Pages.requestParametersKey + config.entity.id) : '')}">
							</div>
						</div>
					</div>

					${_Entities.generalTab.templates.customPropertiesPartial(config)}

				</div>
			`,
			renderingOptions: config => `
				<div id="rendering-options-container" class="hidden">
					<h3>Rendering Options</h3>

					<div class="grid grid-cols-2 gap-8">

						<div>
							<label class="block mb-2" for="rendering-mode-select" data-comment="Select update mode for this element to activate lazy or periodic loading.">Load/Update Mode</label>
							<select class="select2" id="rendering-mode-select" name="data-structr-rendering-mode">
								<option value="">Eager (default)</option>
								<option value="load">When page has finished loading</option>
								<option value="delayed">With a delay after page has finished loading</option>
								<option value="visible">When element becomes visible</option>
								<option value="periodic">With periodic updates</option>
							</select>
						</div>

						<div>
							<label class="block mb-2" for="rendering-delay-or-interval" data-comment="Works in conjunction with Load/Update Mode and is required for delayed and periodic modes.">Delay or interval (ms)</label>
							<input type="number" id="rendering-delay-or-interval" name="data-structr-delay-or-interval">
						</div>
					</div>
				</div>
			`,
			repeaterPartial: config => `
				<div>
					<label class="block mb-2" for="function-query-input">Function Query</label>
					${Structr.templates.autoScriptInput({ inputAttributeString: 'id="function-query-input" name="functionQuery"', wrapperClassString: 'w-full'})}
				</div>

				<div>
					<label class="block mb-2" for="data-key-input">Data Key</label>
					<input type="text" id="data-key-input" name="dataKey">
				</div>
			`,
			userOptions: config => `
				<div id="div-options" class="quick-access-options">

					<div class="grid grid-cols-2 gap-8">

						${_Entities.generalTab.templates.nameTile(config)}

						<div>
							<label class="block mb-2" for="e-mail-input">eMail</label>
							<input type="text" id="e-mail-input" autocomplete="off" name="eMail">
						</div>

						<div>
							<label class="block mb-2" for="password-input">Password</label>
							<input class="mb-2" type="password" id="password-input" autocomplete="new-password" value="****** HIDDEN ******">
							<button class="action" type="button" id="set-password-button">Set Password</button>
						</div>

						<div>

							<label class="block mb-2">Options</label>

							<div class="mb-2 flex items-center">
								<input type="checkbox" name="isAdmin" id="isAdmin">
								<label for="isAdmin">Is Admin User</label>
							</div>

							<div class="mb-2 flex items-center">
								<input type="checkbox" name="skipSecurityRelationships" id="skipSecurityRelationships">
								<label for="skipSecurityRelationships">Skip Security Relationships</label>
							</div>

							<div class="mb-2 flex items-center">
								<input type="checkbox" name="isTwoFactorUser" id="isTwoFactorUser">
								<label for="isTwoFactorUser">Enable Two-Factor Authentication for this User</label>
							</div>
						</div>

						<div>
							<label class="block mb-2" for="password-attempts-input" data-comment="The number of failed login attempts for this user. Depending on the configuration a user is blocked after a certain number of failed login attempts. The user must then reset their password (if allowed via the configuration) or this counter must be reset by an admin.<br><br>Before that threshold is reached, the counter is reset on each successful login.">Failed Login Attempts</label>
							<input type="text" id="password-attempts-input" name="passwordAttempts">
						</div>

						<div>
							<label class="block mb-2" for="confirmation-key" data-comment="Used for self-registration and password reset. If a confirmation key is set, log in via password is prevented, unless <code>registration.allowloginbeforeconfirmation</code> is enabled via structr.conf">Confirmation Key</label>
							<input type="text" id="confirmation-key" name="confirmationKey">
						</div>
					</div>

					${_Entities.generalTab.templates.customPropertiesPartial(config)}

				</div>
			`,
			visibilityPartial: config => `
				<div>
					<label class="block mb-2" for="show-conditions">Show Conditions</label>
					<div class="conditions-container flex">

						${Structr.templates.autoScriptInput({ inputAttributeString: 'id="show-conditions" name="showConditions"', wrapperClassString: 'w-full'})}

						<div class="dropdown-menu dropdown-menu-large">
							<button class="mr-0 dropdown-select rounded border ml-2">
								${_Icons.getSvgIcon(_Icons.iconLightBulb, 16, 16, '', 'Examples')}
							</button>

							<div class="dropdown-menu-container">
								<div class="heading-row">
									<h3>Example show conditions</h3>
								</div>

								${_Entities.exampleShowHideConditions.map(c => _Entities.generalTab.templates.showConditionEntry(c)).join('')}
							</div>
						</div>
					</div>

				</div>

				<div>
					<label class="block mb-2" for="hide-conditions">Hide Conditions</label>
					<div class="conditions-container flex">

						${Structr.templates.autoScriptInput({ inputAttributeString: 'id="hide-conditions" name="hideConditions"', wrapperClassString: 'w-full'})}

						<div class="dropdown-menu dropdown-menu-large">
							<button class="mr-0 dropdown-select rounded ml-2" data-preferred-position-x="left">
								${_Icons.getSvgIcon(_Icons.iconLightBulb, 16, 16, '', 'Examples')}
							</button>

							<div class="dropdown-menu-container">
								<div class="heading-row">
									<h3>Example hide conditions</h3>
								</div>

								${_Entities.exampleShowHideConditions.map(c => _Entities.generalTab.templates.showConditionEntry(c)).join('')}
							</div>
						</div>
					</div>
				</div>
			`,
			showConditionEntry: config => `
				<div class="row">
					<a class="block example-condition" data-value="${config.value}">${config.text ?? config.value}</a>
				</div>
			`
		}
	}
};