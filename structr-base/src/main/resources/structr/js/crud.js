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
document.addEventListener("DOMContentLoaded", () => {
	Structr.registerModule(_Crud);
});

let _Crud = {
	_moduleName: 'crud',
	defaultView: 'custom',
	defaultSort: 'createdDate',
	defaultOrder: 'desc',
	types: {},
	typeColumnSort: {},
	keys: {},
	type: null,
	pageCount: null,
	view: {},
	sort: {},
	order: {},
	page: {},
	pageSize: {},
	prevAnimFrameReqId_moveResizer: undefined,
	crudResizerLeftKey: 'structrCrudResizerLeft_' + location.port,
	moveResizer: (left) => {

		_Helpers.requestAnimationFrameWrapper(_Crud.prevAnimFrameReqId_moveResizer, () => {
			left = left || LSWrapper.getItem(_Crud.crudResizerLeftKey) || 210;
			Structr.mainContainer.querySelector('.column-resizer').style.left = left + 'px';

			document.getElementById('crud-types').style.width        = left - 12 + 'px';
			document.getElementById('crud-recent-types').style.width = left - 12 + 'px';
		});
	},
	resize: () => {},
	onload: () => {

		Structr.updateMainHelpLink(_Helpers.getDocumentationURLForTopic('crud'));

		Structr.setMainContainerHTML(_Crud.templates.main());
		Structr.setFunctionBarHTML(_Crud.templates.functions());

		UISettings.showSettingsForCurrentModule();

		_Crud.moveResizer();

		Structr.initVerticalSlider(Structr.mainContainer.querySelector('.column-resizer'), _Crud.crudResizerLeftKey, 204, _Crud.moveResizer);

		_Crud.objectList.excludeInheritingTypes.restoreConfig();

		_Crud.loadBasicSchemaInformation().then(() => {

			_Crud.typeList.addListeners();
			_Crud.typeList.filtering.activateFilterElements();

			_Crud.typeList.setCurrentTypeIfNotYetSet();

			_Crud.typeList.populateTypeList();

			_Crud.typeList.highlightCurrentType(_Crud.type);
			_Crud.typeList.filtering.filterTypes();

			_Crud.typeList.typeSelected(_Crud.type);
			_Crud.typeList.recents.update(_Crud.type);

			Structr.resize();
			Structr.mainMenu.unblock(100);
		});

		_Crud.search.setupGlobalSearch();
	},
	loadBasicSchemaInformation: async () => {

		_Crud.helpers.delayedMessage.showLoadingMessageAfterDelay('Loading data', 100);

		let response = await fetch(`${Structr.rootUrl}_schema`);

		if (response.ok) {

			let data = await response.json();

			let crudTypes = data.result.filter(typeObj => !typeObj.isServiceClass);

			for (let typeObj of crudTypes) {

				_Crud.types[typeObj.type] = typeObj;
			}
		}
	},
	typeList: {
		lastUsedTypeKey: 'structrCrudType_' + location.port,
		defaultType: 'Page',
		addListeners: () => {

			document.querySelector('#crud-left').addEventListener('click', (e) => {
				let type = e.target.closest('.crud-type');
				if (type) {
					_Crud.typeList.typeSelected(type.dataset['type']);
				}
			});

			document.querySelector('#crud-recent-types').addEventListener('click', (e) => {
				let removeRecentType = e.target.closest('.remove-recent-type');
				if (removeRecentType) {
					e.stopPropagation();
					let type = removeRecentType.closest('div[data-type]');
					_Crud.typeList.recents.remove(type);
				}
			});
		},
		populateTypeList: () => {

			let typeListHtml = Object.keys(_Crud.types).sort().map(typeName => `<div class="crud-type truncate hidden" data-type="${typeName}">${typeName}</div>`).join('');
			let typesListEl  = document.querySelector('#crud-types-list');

			typesListEl.insertAdjacentHTML('beforeend', typeListHtml);
		},
		highlightCurrentType: (selectedType) => {

			[...document.querySelectorAll('#crud-left .crud-type.active')].forEach(a => a.classList.remove('active'));
			document.querySelector(`#crud-left .crud-type[data-type="${selectedType}"]`)?.classList?.add('active');

			let $crudTypesList             = $('#crud-types-list');
			let $selectedElementInTypeList = $('.crud-type[data-type="' + selectedType + '"]', $crudTypesList);

			if ($selectedElementInTypeList && $selectedElementInTypeList.length > 0) {

				let positionOfList    = $crudTypesList.position().top;
				let scrollTopOfList   = $crudTypesList.scrollTop();
				let positionOfElement = $selectedElementInTypeList.position().top;
				$crudTypesList.animate({ scrollTop: positionOfElement + scrollTopOfList - positionOfList });

			} else {

				$crudTypesList.animate({ scrollTop: 0 });
			}
		},
		setCurrentTypeIfNotYetSet: () => {

			let setCurrentTypeIfPossible = (type) => {

				if (_Crud.types[type]) {
					_Crud.type = type;
				}
			};

			if (!_Crud.type) {
				setCurrentTypeIfPossible(_Helpers.getURLParameter('type'));
			}

			if (!_Crud.type) {
				setCurrentTypeIfPossible(LSWrapper.getItem(_Crud.typeList.lastUsedTypeKey));
			}

			if (!_Crud.type) {
				setCurrentTypeIfPossible(_Crud.typeList.defaultType);
			}
		},
		storeCurrentType: (type) => {
			LSWrapper.setItem(_Crud.typeList.lastUsedTypeKey, type);
		},
		typeSelected: (type) => {

			_Crud.helpers.delayedMessage.showLoadingMessageAfterDelay(`Loading schema information for type <b>${type}</b>`, 500);

			_Crud.helpers.ensurePropertiesForTypeAreLoaded(type, () => {

				_Crud.typeList.storeCurrentType(type);

				_Crud.typeList.recents.update(type);
				_Crud.typeList.highlightCurrentType(type);

				_Crud.objectList.initializeForType(type);

			}, () => {

				_Crud.typeList.recents.remove(document.querySelector(`#crud-left .crud-type[data-type="${type}"]`));

				_Crud.helpers.delayedMessage.removeMessage();
			});
		},
		recents: {
			crudRecentTypesKey: 'structrCrudRecentTypes_' + location.port,
			update: (selectedType) => {

				let recentTypes = LSWrapper.getItem(_Crud.typeList.recents.crudRecentTypesKey);

				if (recentTypes && selectedType) {

					recentTypes = recentTypes.filter((type) => (type !== selectedType));
					recentTypes.unshift(selectedType);

				} else if (selectedType) {

					recentTypes = [selectedType];
				}

				recentTypes = recentTypes.slice(0, 12);

				if (recentTypes) {

					let recentTypesList = document.querySelector('#crud-recent-types-list');

					recentTypesList.innerHTML = recentTypes.map(type => `
						<div class="crud-type flex items-center justify-between ${(selectedType === type ? ' active' : '')}" data-type="${type}">
							<div class="truncate">${type}</div>
							${_Icons.getSvgIcon(_Icons.iconCrossIcon, 12, 12, _Icons.getSvgIconClassesForColoredIcon(['flex-none', 'icon-grey', 'remove-recent-type']))}
						</div>
					`).join('');
				}

				LSWrapper.setItem(_Crud.typeList.recents.crudRecentTypesKey, recentTypes);
			},
			remove: (recentTypeElement) => {

				if (recentTypeElement) {

					let typeToRemove = recentTypeElement.dataset['type'];
					let recentTypes  = LSWrapper.getItem(_Crud.typeList.recents.crudRecentTypesKey);

					if (recentTypes) {
						recentTypes = recentTypes.filter((type) => (type !== typeToRemove));
					}

					LSWrapper.setItem(_Crud.typeList.recents.crudRecentTypesKey, recentTypes);

					_Helpers.fastRemoveElement(recentTypeElement);
				}
			},
		},
		filtering: {
			displayTypeConfigKey: 'structrCrudDisplayTypes_' + location.port,
			activateFilterElements: () => {

				for (let typeFilterCheckbox of document.querySelectorAll('#crudTypeFilterSettings input')) {

					typeFilterCheckbox.addEventListener('change', () => {
						LSWrapper.setItem(_Crud.typeList.filtering.displayTypeConfigKey, _Crud.typeList.filtering.getTypeVisibilityConfigFromUI());
						_Crud.typeList.filtering.filterTypes();
					});
				}

				document.querySelector('#crudTypesSearch').addEventListener('keyup', (e) => {

					if (e.keyCode === 27) {

						e.target.value = '';

					} else if (e.keyCode === 13) {

						let visibleTypes = document.querySelectorAll('#crud-types-list .crud-type:not(.hidden)');

						if (visibleTypes.length === 1) {

							_Crud.typeList.typeSelected(visibleTypes[0].dataset['type']);

						} else {

							let filterVal     = e.target.value.toLowerCase();
							let matchingTypes = Object.keys(_Crud.types).filter(type => type.toLowerCase() === filterVal);

							if (matchingTypes.length === 1) {
								_Crud.typeList.typeSelected(matchingTypes[0]);
							}
						}
					}

					_Crud.typeList.filtering.filterTypes();
				});
			},
			getFilteredTypes: () => {

				let typeVisibility = _Crud.typeList.filtering.getStoredTypeVisibilityConfig();

				return Object.keys(_Crud.types).sort().filter(typeName => {

					let type            = _Crud.types[typeName];
					let isRelType       = (type.isRel === true);
					let isBuiltInRel    = isRelType && type.isBuiltin;
					let isCustomRelType = isRelType && !type.isBuiltin;
					let isDynamicType   = !isRelType && !type.isBuiltin;
					let isHtmlType      = type.traits.includes('DOMNode');
					let isFlowType      = type.traits.includes('FlowNode');
					let isOtherType     = !(isRelType || isDynamicType || isHtmlType || isFlowType);

					let hide = (!typeVisibility.rels && isBuiltInRel) || (!typeVisibility.customRels && isCustomRelType) || (!typeVisibility.custom && isDynamicType) ||
						(!typeVisibility.html && isHtmlType) || (!typeVisibility.flow && isFlowType) || (!typeVisibility.other && isOtherType);

					return !hide;
				});
			},
			filterTypes: () => {

				// combined filter function for search input and filter checkboxes
				let typesToShowViaFilterCheckbox = _Crud.typeList.filtering.getFilteredTypes();

				let filterString = document.querySelector('#crudTypesSearch').value.toLowerCase();

				for (let el of document.querySelectorAll('#crud-types-list .crud-type')) {

					let typeName = el.dataset['type'];

					let shouldHideByTypeFilters     = !(typesToShowViaFilterCheckbox.includes(typeName));
					let shouldHideByUserInputFilter = (filterString.length > 0) && (typeName.toLowerCase().indexOf(filterString) === -1);

					let force = shouldHideByTypeFilters || shouldHideByUserInputFilter;

					el.classList.toggle('hidden', force);
				}
			},
			getStoredTypeVisibilityConfig: (singleKey) => {

				let config = LSWrapper.getItem(_Crud.typeList.filtering.displayTypeConfigKey, {
					custom:     true,
					customRels: true,
					rels:       false,
					flow:       false,
					html:       false,
					other:      false
				});

				if (singleKey) {

					return config[singleKey];
				}

				return config;
			},
			getTypeVisibilityConfigFromUI: () => {

				return {
					custom:       document.querySelector('#crudTypeToggleCustom').checked,
					customRels:   document.querySelector('#crudTypeToggleCustomRels').checked,
					rels:         document.querySelector('#crudTypeToggleRels').checked,
					flow:         document.querySelector('#crudTypeToggleFlow').checked,
					html:         document.querySelector('#crudTypeToggleHtml').checked,
					other:        document.querySelector('#crudTypeToggleOther').checked
				};
			},
			templates: {
				filterBox: config => `
					<div id="crudTypeFilterSettings" class="dropdown-menu dropdown-menu-large">
	
						<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green" id="crudTypesFilterToggle">
							${_Icons.getSvgIcon(_Icons.iconFilterFunnel, 16, 16, ['mr-2'])}
						</button>
			
						<div class="dropdown-menu-container" style="width: 17rem;">
			
							<div class="heading-row">
								<h3>Type Filters</h3>
							</div>
			
							<div class="row">
								<label class="block"><input ${_Crud.typeList.filtering.getStoredTypeVisibilityConfig('custom') ? 'checked' : ''} type="checkbox" id="crudTypeToggleCustom"> Custom Types</label>
							</div>
							<div class="row">
								<label class="block"><input ${_Crud.typeList.filtering.getStoredTypeVisibilityConfig('customRels')   ? 'checked' : ''} type="checkbox" id="crudTypeToggleCustomRels"> Custom Relationship Types</label>
							</div>
							<div class="row">
								<label class="block"><input ${_Crud.typeList.filtering.getStoredTypeVisibilityConfig('rels')   ? 'checked' : ''} type="checkbox" id="crudTypeToggleRels"> Built-In Relationship Types</label>
							</div>
							<div class="row">
								<label class="block"><input ${_Crud.typeList.filtering.getStoredTypeVisibilityConfig('html')   ? 'checked' : ''} type="checkbox" id="crudTypeToggleHtml"> HTML Types</label>
							</div>
							<div class="row">
								<label class="block"><input ${_Crud.typeList.filtering.getStoredTypeVisibilityConfig('flow')   ? 'checked' : ''} type="checkbox" id="crudTypeToggleFlow"> Flow Types</label>
							</div>
							<div class="row mb-2">
								<label class="block"><input ${_Crud.typeList.filtering.getStoredTypeVisibilityConfig('other')  ? 'checked' : ''} type="checkbox" id="crudTypeToggleOther"> Other Types</label>
							</div>
			
						</div>
					</div>
				`,
				filterInput: config => `<input placeholder="Filter types..." id="crudTypesSearch" autocomplete="off">`
			}
		},
	},
	objectList: {
		crudHiddenColumnsKey: 'structrCrudHiddenColumns_' + location.port,
		crudSortedColumnsKey: 'structrCrudSortedColumns_' + location.port,
		initializeForType: (type) => {

			let crudRight = document.querySelector('#crud-type-detail');

			_Crud.helpers.delayedMessage.removeMessage();

			_Helpers.fastRemoveAllChildren(crudRight);

			let crudButtons = Structr.functionBar.querySelector('#crud-buttons');
			_Helpers.setContainerHTML(crudButtons, _Crud.templates.typeButtons({ type: type }));

			let exactTypeLabel = crudButtons.querySelector('.exact-type-checkbox-label');

			_Helpers.appendInfoTextToElement({
				element: exactTypeLabel,
				text: 'This flag affects the list shown below and the delete function.<br><br>If active only nodes of the selected type ("' + type + '") are shown in the list and types inheriting from this type are excluded. If it is not active, nodes for the current type and nodes of all its subtypes are shown.<br><br>The same is true for the delete function. If active, only nodes with that exact type are deleted and nodes of inheriting types are not deleted. If it is not active, nodes for the active type and nodes of all its subtypes are deleted.',
				insertAfter: true,
				css: {
					marginLeft: '4px',
				},
				helpElementCss: {
					fontSize: '12px',
					lineHeight: '1.1em'
				}
			});

			_Crud.objectList.pager.determinePagerData(type);

			// fall back to public view if saved view does not exist (anymore)
			if (!Object.keys(_Crud.types[type].views).includes(_Crud.view[type])) {
				_Crud.view[type] = 'public';
			}

			let pagerNode = _Crud.objectList.pager.addPager(type, crudRight);

			crudRight.insertAdjacentHTML('beforeend', `
				<table class="crud-table" data-type="${type}">
					<thead>
						<tr></tr>
					</thead>
					<tbody></tbody>
					</table>
				<div id="query-info">Query: <span class="queryTime"></span> s &nbsp; Serialization: <span class="serTime"></span> s</div>
			`);

			_Crud.objectList.updateCrudTableHeader(type);

			document.querySelector('#create' + type).addEventListener('click', (e) => {

				_Crud.creationDialogWithErrorHandling.initializeForEvent(e, type, {}, _Crud.creationDialogWithErrorHandling.crudCreateSuccess);
			});

			document.querySelector('#export' + type).addEventListener('click', () => {
				_Crud.objectList.showExportDialog(type);
			});

			document.querySelector('#import' + type).addEventListener('click', () => {
				_Crud.objectList.showImportDialog(type);
			});

			let exactTypeCheckbox = document.querySelector('#exact_type_' + type);
			exactTypeCheckbox.checked = _Crud.objectList.excludeInheritingTypes.shouldExclude(type);

			exactTypeCheckbox.addEventListener('change', () => {
				_Crud.objectList.excludeInheritingTypes.update(type, exactTypeCheckbox.checked)
				_Crud.objectList.refreshList(type);
			});

			document.querySelector('#delete' + type).addEventListener('click', async () => {
				await _Crud.objectList.askDeleteAllNodesOfType(type, exactTypeCheckbox.checked);
			});

			_Crud.objectList.pager.deActivatePagerElements(pagerNode);
			_Crud.objectList.activateList(type);
			_Crud.objectList.pager.activatePagerElements(type, pagerNode);
		},
		showExportDialog: (type) => {

			let { dialogText } = _Dialogs.custom.openDialog(`Export ${type} list as CSV`);

			if (!Structr.activeModules.csv) {
				dialogText.insertAdjacentHTML('beforeend', 'CSV Module is not included in the current license. See <a href="https://structr.com/editions">Structr Edition Info</a> for more information.');
				return;
			}

			let exportArea = _Helpers.createSingleDOMElementFromHTML('<textarea class="exportArea"></textarea>');
			dialogText.appendChild(exportArea);

			let copyBtn = _Dialogs.custom.appendCustomDialogButton('<button id="copyToClipboard" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Copy to Clipboard</button>');

			copyBtn.addEventListener('click', async () => {
				await navigator.clipboard.writeText(exportArea.value);

				new SuccessMessage().text('Copied to clipboard').show();
			});

			let downLoad = _Dialogs.custom.appendCustomDialogButton('<button id="download-csv-export" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Download</button>');
			downLoad.addEventListener('click', async () => {
				_Helpers.downloadFile([exportArea.value], `csv-export-${_Helpers.getTimestampWithPrefix(type)}.csv`, 'text/csv');
			});

			let hiddenKeys             = _Crud.objectList.getHiddenKeys(type);
			let acceptHeaderProperties = Object.keys(_Crud.types[type].views.all).filter(key => !hiddenKeys.includes(key)).join(',');

			fetch(`${Structr.csvRootUrl}${type}/all${_Crud.helpers.getSortAndPagingParameters(type, _Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type])}`, {
				headers: {
					Range: _Crud.objectList.getRangeHeaderForType(type),
					Accept: 'properties=' + acceptHeaderProperties
				}
			}).then(async response => {

				let data = await response.text();
				exportArea.value = data;
			})
		},
		showImportDialog: (type) => {

			let { dialogText, dialogMeta } = _Dialogs.custom.openDialog(`Import CSV data for type ${type}`);
			_Dialogs.custom.showMeta();

			if (!Structr.activeModules.csv) {
				dialogText.insertAdjacentHTML('beforeend', 'CSV Module is not included in the current license. See <a href="https://structr.com/editions">Structr Edition Info</a> for more information.');
				return;
			}

			let importArea = _Helpers.createSingleDOMElementFromHTML('<textarea class="importArea"></textarea>');
			dialogText.appendChild(importArea);

			dialogMeta.insertAdjacentHTML('beforeend', `
			<div class="flex gap-2 items-center">
				<label>Field Separator: </label>
				<select id="csv-import-field-separator" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
					<option selected="">;</option>
					<option>,</option>
				</select>
				<label>Quote Character: </label>
				<select id="csv-import-quote-character" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
					<option selected="">"</option>
					<option>'</option>
				</select>
				<label>Periodic Commit?</label>
				<input id="csv-import-periodic-commit" type="checkbox">
				<div id="csv-import-commit-interval-container" style="display: none;">
					(Interval: <input id="csv-import-commit-interval" type="text" value="1000" size="5"> lines)
				</div>
			</div>
		`);

			let separatorSelect                 = dialogMeta.querySelector('#csv-import-field-separator');
			let quoteCharacterSelect            = dialogMeta.querySelector('#csv-import-quote-character');
			let periodicCommitCheckbox          = dialogMeta.querySelector('#csv-import-periodic-commit');
			let periodicCommitIntervalContainer = dialogMeta.querySelector('#csv-import-commit-interval-container');
			let periodicCommitIntervalInput     = dialogMeta.querySelector('#csv-import-commit-interval');

			periodicCommitCheckbox.addEventListener('change', () => {

				if (periodicCommitCheckbox.checked) {
					periodicCommitIntervalContainer.style.display = '';
				} else {
					periodicCommitIntervalContainer.style.display = 'none';
				}
			});

			window.setTimeout(() => {
				importArea.focus();
			}, 200);

			let startImportBtn = _Dialogs.custom.appendCustomDialogButton('<button class="action">Start Import</button>');

			startImportBtn.addEventListener('click', async () => {

				let maxImportCharacters = 100000;
				let cleanedBody         = importArea.value.split('\n').map(l => l.trim()).filter(line => (line !== '')).join('\n');
				let importLength        = cleanedBody.length;

				if (importLength > maxImportCharacters) {

					let importTooBig = `Not starting import because it contains too many characters (${importLength}). The limit is ${maxImportCharacters}.<br> Consider uploading the CSV file to the Structr filesystem and using the file-based CSV import which is more powerful than this import.<br><br>`;

					new ErrorMessage().text(importTooBig).title('Too much import data').requiresConfirmation().show();
					return;
				}

				if (cleanedBody.length === 0) {
					new ErrorMessage().text("Unable to import empty CSV").requiresConfirmation().show();
					return;
				}

				let url = Structr.csvRootUrl + type;

				let response = await fetch(url, {
					method: 'POST',
					headers: {
						'X-CSV-Field-Separator': separatorSelect.value,
						'X-CSV-Quote-Character': quoteCharacterSelect.value,
						'X-CSV-Periodic-Commit': periodicCommitCheckbox.checked,
						'X-CSV-Periodic-Commit-Interval': periodicCommitIntervalInput.value
					},
					body: cleanedBody
				});

				if (response.ok) {

					_Crud.objectList.refreshList(type);

				} else {

					let data = await response.data;
					if (data) {
						Structr.errorFromResponse(data, url);
					}
				}
			});
		},
		askDeleteAllNodesOfType: async (type, exact) => {

			let confirm = await _Dialogs.confirmation.showPromise(`
				<h3>WARNING: Really delete all objects of type '${type}'${((exact === true) ? '' : ' and of inheriting types')}?</h3>
				<p>This will delete all objects of the type (<b>${((exact === true) ? 'excluding' : 'including')}</b> all objects of inheriting types).</p>
				<p>Depending on the amount of objects this can take a while.</p>
			`);

			if (confirm === true) {
				let url      = `${Structr.rootUrl}${type}${((exact === true) ? `?type=${type}` : '')}`;
				let response = await fetch(url, { method: 'DELETE' });

				if (response.ok) {

					new SuccessMessage().text(`Deletion of all nodes of type '${type}' finished.`).show();
					_Crud.typeList.typeSelected(type);

				} else {

					let data = await response.json();
					Structr.errorFromResponse(data, url, { statusCode: 400, requiresConfirmation: true });
				}
			}

			return confirm;
		},
		updateCrudTableHeader: (type) => {

			let properties     = _Crud.helpers.getPropertiesForTypeAndCurrentView(type);
			let tableHeaderRow = document.querySelector('#crud-type-detail table thead tr');

			_Helpers.fastRemoveAllChildren(tableHeaderRow);

			let newHeaderHTML = `
				<th class="___action_header" data-key="action_header">Actions</th>
				${_Crud.objectList.filterKeys(type, Object.keys(properties)).map(key => `<th data-key="${key}">${key}</th>`).join('')}
			`;

			tableHeaderRow.insertAdjacentHTML('beforeend', newHeaderHTML);
		},
		list: (type, url, isRetry) => {

			_Crud.crudListFetchAbortMechanism.abortListFetch(type);

			let properties = _Crud.helpers.getPropertiesForTypeAndCurrentView(type);

			_Crud.helpers.delayedMessage.showLoadingMessageAfterDelay(`Loading data for type <b>${type}</b>`, 100);

			let customViewProperties = (isRetry ? [] : _Crud.objectList.filterKeys(type, Object.keys(properties)));

			let signal = _Crud.crudListFetchAbortMechanism.abortController.signal;

			let headers = {
				Range: _Crud.objectList.getRangeHeaderForType(type),
			};
			Object.assign(headers, _Helpers.getHeadersForCustomView(customViewProperties));

			fetch (url, {
				signal: signal,
				headers: headers
			}).then(async response => {

				let data = await response.json();

				if (response.ok) {

					_Crud.helpers.delayedMessage.removeMessage();

					if (!data || !Structr.isModuleActive(_Crud)) {
						return;
					}

					_Crud.crudCache.clear();

					for (let item of data.result) {
						StructrModel.create(item);
						_Crud.objectList.appendRow(type, properties, item);
					}
					_Crud.objectList.pager.updatePager(type, data);
					_Crud.objectList.replaceSortHeader(type);

				} else {

					if (response.status === 431) {
						// Happens if headers grow too large (property list too long)

						if (!isRetry) {
							_Crud.objectList.list(type, url, true);
						} else {
							_Crud.helpers.delayedMessage.showMessageAfterDelay(_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 24, 24, 'mr-2') + ' View is too large - please select different view', 1);
						}

					} else {
						console.log(type, url);
					}

					_Crud.helpers.delayedMessage.removeMessage();
				}
			}).catch(e => {

				//console.log(signal)
				if (signal.aborted !== true) {
					// is we did not abort the request, we should log the output (or show a notification popup?)
					console.log(e);
				}
			});
		},
		isDisplayedTableForType: (type) => {

			return (type === document.querySelector('#crud-type-detail table')?.dataset['type']);
		},
		refreshList: (type) => {
			_Crud.objectList.clearList(type);
			_Crud.objectList.activateList(type);
		},
		activateList: (type) => {
			let url = Structr.rootUrl + type + '/' + _Crud.view[type] + _Crud.helpers.getSortAndPagingParameters(type, _Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type], _Crud.objectList.excludeInheritingTypes.shouldExclude(type));
			_Crud.objectList.list(type, url);
		},
		reloadCompleteObjectListUI: () => {
			_Crud.objectList.initializeForType(_Crud.type);
		},
		clearList: () => {
			_Helpers.fastRemoveAllChildren(document.querySelector('#crud-type-detail table tbody'));
		},
		replaceSortHeader: (type) => {

			let newOrder = (_Crud.order[type] && _Crud.order[type] === 'desc' ? 'asc' : 'desc');

			for (let th of document.querySelectorAll('#crud-type-detail table th')) {

				let key = th.dataset['key'];

				if (key === "action_header") {

					th.innerHTML = '<div class="flex items-center">Actions</div>';

					let configIcon = _Helpers.createSingleDOMElementFromHTML(_Icons.getSvgIcon(_Icons.iconUIConfigSettings, 16, 16, _Icons.getSvgIconClassesNonColorIcon(['ml-2'])));

					th.firstChild.appendChild(configIcon);

					configIcon.addEventListener('click', (e) => {

						let { dialogText } = _Dialogs.custom.openDialog(`Configure columns for type ${type}`);

						let saveAndCloseButton = _Dialogs.custom.updateOrCreateDialogSaveAndCloseButton();
						_Helpers.enableElement(saveAndCloseButton);

						dialogText.insertAdjacentHTML('beforeend', _Crud.templates.configureColumns());
						let columnSelect = dialogText.querySelector('#columns-select');

						_Crud.helpers.ensurePropertiesForTypeAreLoaded(type, () => {

							let sortOrder = _Crud.objectList.getSavedSortOrderOfColumns(type);

							if (sortOrder.length === 0) {

								// all keys for current view minus the ones that are hidden
								sortOrder = _Crud.objectList.filterKeys(type, Object.keys(_Crud.types[type].views.all));
							}

							// filter out keys that have serializationDisabled flag
							sortOrder = sortOrder.filter(key => !_Crud.helpers.isSerializationDisabled(type, key));

							let orderedColumnsSet = new Set(sortOrder);

							// add the available keys, which are currently hidden, so we can add them back
							for (let key of Object.keys(_Crud.types[type].views.all)) {
								orderedColumnsSet.add(key);
							}

							let hiddenKeys = _Crud.objectList.getHiddenKeys(type);

							let optionsHTML = Array.from(orderedColumnsSet).map(key => {

								let isHidden   = hiddenKeys.includes(key);
								let isIdOrType = (key === 'id' || key === 'type');
								let isSelected = ((!isHidden || isIdOrType) ? 'selected' : '');
								let isDisabled = (isIdOrType ? 'disabled' : '');

								return `<option value="${key}" ${isSelected} ${isDisabled}>${key}</option>`;
							}).join('');

							columnSelect.insertAdjacentHTML('beforeend', optionsHTML);

							let dropdownParent = _Dialogs.custom.isDialogOpen() ? $(_Dialogs.custom.getDialogBoxElement()) : $('body');
							let jqSelect       = $(columnSelect);

							jqSelect.select2({
								search_contains: true,
								width: '100%',
								dropdownParent: dropdownParent,
								dropdownCssClass: 'select2-sortable hide-selected-options hide-disabled-options',
								containerCssClass: 'select2-sortable hide-selected-options hide-disabled-options',
								closeOnSelect: false,
								scrollAfterSelect: false
							}).select2Sortable();

							saveAndCloseButton.addEventListener('click', (e) => {
								e.stopPropagation();

								_Crud.objectList.saveSortOrderOfColumns(type, jqSelect.sortedValues());
								_Crud.objectList.reloadCompleteObjectListUI();

								_Dialogs.custom.clickDialogCancelButton();
							});

						}, () => {
							new WarningMessage().text(`Unable to find schema information for type '${type}'. There might be database nodes with no type information or a type unknown to Structr in the database.`).show();
						});

					});

				} else if (key !== 'Actions') {

					let sortKey = key;
					th.innerHTML = `
						<a class="${((_Crud.sort[type] === key) ? 'column-sorted-active' : '')}" href="${_Crud.helpers.getSortAndPagingParameters(type, sortKey, newOrder, _Crud.pageSize[type], _Crud.page[type])}#${type}">${key}</a>
						${_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['icon-lightgrey', 'cursor-pointer']), 'Hide column ' + key)}
					`;

					let isCollection            = _Crud.helpers.isCollection(key, type);
					let isRelationshipAttribute = _Crud.helpers.getRelatedTypeForAttribute(key, type) !== undefined;
					if (isCollection && isRelationshipAttribute) {
						_Crud.objectList.pager.addPageSizeConfigToColumn($(th), type, key);
					}

					$('a', th).on('click', function(event) {
						event.preventDefault();
						_Crud.sort[type] = key;
						_Crud.order[type] = (_Crud.order[type] && _Crud.order[type] === 'desc' ? 'asc' : 'desc');
						_Crud.objectList.refreshList(type);
						return false;
					});

					$('svg', th).on('click', function(e) {
						e.preventDefault();
						// toggle column
						let hiddenKeys = _Crud.objectList.getHiddenKeys(type);

						if (hiddenKeys.includes(key)) {

							hiddenKeys.splice(hiddenKeys.indexOf(key), 1);

						} else {

							hiddenKeys.push(key);

							let table = $('#crud-type-detail table');

							// remove column(s) from table
							$(`th${_Crud.helpers.getCSSSelectorForKey(key)}`, table).remove();
							$(`td${_Crud.helpers.getCSSSelectorForKey(key)}`, table).each(function(i, t) {
								t.remove();
							});
						}

						LSWrapper.setItem(_Crud.objectList.crudHiddenColumnsKey + type, JSON.stringify(hiddenKeys));
						return false;
					});
				}
			}
		},
		getSavedSortOrderOfColumns: (type) => {

			let sortOrder = LSWrapper.getItem(_Crud.objectList.crudSortedColumnsKey + type, undefined);

			if (sortOrder) {

				try {

					let restoredSortOrder = JSON.parse(sortOrder);
					return restoredSortOrder;

				} catch (e) {

				}
			}

			// if we do not have a sort order, simply use default sorting (use all view to get a global sorting for all views)
			return Object.keys(_Crud.types[type].views.all);
		},
		saveSortOrderOfColumns: (type, order) => {

			_Crud.typeColumnSort[type] = order;

			// this also updates hidden keys (inverted!)
			let allPropertiesOfType = Object.keys(_Crud.types[type].views.all);
			let hiddenKeys          = allPropertiesOfType.filter(prop => !order.includes(prop));

			LSWrapper.setItem(_Crud.objectList.crudHiddenColumnsKey + type, JSON.stringify(hiddenKeys));
			LSWrapper.setItem(_Crud.objectList.crudSortedColumnsKey + type, JSON.stringify(order));
		},		
		getRow: (id) => {
			return $('tr#id_' + id);
		},
		appendRow: (type, properties, item) => {

			if (_Crud.objectList.isDisplayedTableForType(type)) {

				_Crud.helpers.ensurePropertiesForTypeAreLoaded(item.type, () => {

					let id  = item['id'];
					let row = _Crud.objectList.getRow(id);

					if (!row[0]) {
						document.querySelector('#crud-type-detail table tbody').insertAdjacentHTML('beforeend', `<tr id="id_${id}"></tr>`);
					}

					_Crud.objectList.populateRow(id, item, type, properties);
				});
			}
		},
		populateRow: (id, item, type, properties) => {

			let row = _Crud.objectList.getRow(id);
			_Helpers.fastRemoveAllChildren(row[0]);

			if (properties) {

				let actions = $(`
					<td class="actions">
						${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, _Icons.getSvgIconClassesNonColorIcon(['mr-1', 'edit']))}
						${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['mr-1', 'icon-red', 'delete']), 'Remove')}
					</td>
				`);

				if (!(_Crud.types[type] && _Crud.helpers.isRelType(type))) {
					_Entities.appendNewAccessControlIcon(actions, item, false);
				}

				row.append(actions);

				let filterKeys = _Crud.objectList.filterKeys(type, Object.keys(properties));

				for (let key of filterKeys) {

					let cell = $(_Helpers.createSingleDOMElementFromHTML(`<td class="value"></td>`));
					row.append(cell);

					_Crud.objectList.populateCell(id, key, type, item[key], cell);
				}

				Structr.resize();

				row[0].querySelector('.actions .edit').addEventListener('click', (e) => {
					_Crud.objectList.showDetails(id, type);
				});

				row[0].querySelector('.actions .delete').addEventListener('click', async (e) => {
					await _Crud.helpers.crudAskDelete(type, id);
				});
			}
		},
		populateCell: (id, key, type, value, cell) => {

			let isRelType                  = _Crud.helpers.isRelType(type);
			let isCollection               = _Crud.helpers.isCollection(key, type);
			let isEnum                     = _Crud.helpers.isEnum(key, type);
			let isCypher                   = _Crud.helpers.isCypherProperty(key, type);
			let relatedType                = _Crud.helpers.getRelatedTypeForAttribute(key, type);
			let readOnly                   = _Crud.helpers.isReadOnly(key, type);
			let isCollectionIdProperty     = _Crud.helpers.isCollectionIdProperty(key, type);
			let isCollectionNotionProperty = _Crud.helpers.isCollectionNotionProperty(key, type);
			let isSourceOrTarget           = isRelType && (key === Structr.internalKeys.sourceId || key === Structr.internalKeys.targetId || key === Structr.internalKeys.sourceNode || key === Structr.internalKeys.targetNode);
			let propertyType               = _Crud.types[type]?.views.all[key]?.type;

			if (readOnly) {
				cell.addClass('readonly');
			}

			cell[0].dataset['key']          = key;
			cell[0].dataset['isCollection'] = isCollection;

			let isRegularDirectAttribute = !isSourceOrTarget && !relatedType;

			if (isRegularDirectAttribute) {

				if (propertyType === 'Boolean') {

					cell.addClass('boolean-attr');
					cell.append(`<input name="${key}" ${readOnly ? 'class="readonly" readonly disabled ' : ''}type="checkbox" ${value ? 'checked="checked"' : ''}>`);

					if (!readOnly) {
						$('input', cell).on('change', function() {
							if (id) {
								let checked = $(this).prop('checked');
								_Crud.objectList.crudUpdate(id, key, checked, undefined, () => {

									_Crud.objectList.refreshObject(id, key, !checked);

									if (key === 'visibleToPublicUsers' || key === 'visibleToAuthenticatedUsers') {
										StructrModel.updateKey(id, key, checked);
										_Entities.updateNewAccessControlIconInElement(StructrModel.obj(id), Structr.node(id));
									}
								});
							}
						});
					}

				} else if (propertyType === 'Date') {

					let cellContent = _Helpers.createSingleDOMElementFromHTML(`
						<div class="flex items-center relative">
							<input name="${key}" class="__value pl-9 mr-3 ${readOnly ? 'readonly' : ''}" type="text" size="26" autocomplete="one-time-code" ${readOnly ? 'readonly' : ''}>
							${_Crud.helpers.getDateTimeIconHTML()}
						</div>
					`);
					cell[0].appendChild(cellContent);

					let input = cellContent.querySelector('input');
					input.value = value ?? '';

					if (!readOnly) {

						let oldValue = input.value;
						let wasEmpty = (oldValue === '');

						_Entities.addDatePicker(input, key, type, () => {

							let newValue = input.value;
							let isEmpty  = (newValue === '');

							let isFirstValue = (wasEmpty && !isEmpty);
							let valueRemoved = (!wasEmpty && isEmpty);
							let valueChanged = (!wasEmpty && !isEmpty && (new Date(newValue).getTime() !== new Date(oldValue).getTime()));
							let shouldSave   = (isFirstValue || valueRemoved || valueChanged);

							if (id && shouldSave) {
								_Crud.objectList.crudUpdate(id, key, newValue);
							} else {
								_Crud.objectList.resetCellToOldValue(id, key, oldValue);
							}
						});
					}

				} else if (propertyType === 'ZonedDateTime') {

					let cellContent = _Helpers.createSingleDOMElementFromHTML(`
						<div class="flex items-center relative">
							<input name="${key}" class="__value pl-9 mr-3" type="text" size="36" autocomplete="one-time-code">
							${_Crud.helpers.getDateTimeIconHTML()}
						</div>
					`);
					cell[0].appendChild(cellContent);

					let input = cellContent.querySelector('input');
					input.value = value ?? '';

					if (!readOnly) {

						let oldValue = input.value;

						// detect timezone id either from system or from old value
						let timezoneId = Intl.DateTimeFormat().resolvedOptions().timeZone;
						let oldTzId = oldValue.match(/\[(.*)\]/);
						if (oldTzId?.length > 0) {
							timezoneId = oldTzId[1];
						}

						const getOffset = (timeZone = 'UTC', date = new Date()) => {
							const utcDate = new Date(date.toLocaleString('en-US', { timeZone: 'UTC' }));
							const tzDate = new Date(date.toLocaleString('en-US', { timeZone }));
							return (tzDate.getTime() - utcDate.getTime()) / 6e4;
						};

						$(input).datetimepicker({
							parse: (timeFormat, timeString, options) => {

								let fakeOptions = Object.assign({}, options);
								fakeOptions.parse = 'loose';

								// remove timezone identifier from timeString
								let pos    = timeString.indexOf('[');
								if (pos > 0) {
									timeString = timeString.slice(0, pos);
								}

								let innerData = $.datepicker.parseTime(timeFormat, timeString, fakeOptions);

								// fake our timezone
								innerData.timezone = oldTzId[0];

								return innerData;
							},
							dateFormat: 'yy-mm-dd',
							timeFormat: 'HH:mm:ssz',
							separator: 'T',
							timezone: '[' + timezoneId + ']',
							timezoneList: Intl.supportedValuesOf('timeZone').map(lbl => { return { label: lbl, value: '[' + lbl + ']'} }),
							onClose: function() {
								$('#ui-datepicker-div').removeClass('is-zoned');
								let newValue = input.value;

								// add timezone corresponding to timezone identifier
								let offset = 0;
								let newTzId = newValue.match(/\[(.*)\]/);
								if (newTzId?.length > 0) {
									offset = getOffset(newTzId[1]);
								}

								let offsetString = $.timepicker.timezoneOffsetString(offset, true);

								newValue = newValue.replaceAll('[', offsetString + '[');

								if (id && newValue !== oldValue) {

									_Crud.objectList.crudUpdate(id, key, newValue);

								} else {

									input.value = newValue;
								}
							}
						});

						input.addEventListener('focus', (e) => {
							$(input).datetimepicker('show');
						});

						if (oldValue) {
							// set the picker because otherwise it fails (because we are injecting timezone identifiers where the plugin expects numbers
							let dateStringWithoutId = oldValue;
							let pos = oldValue.indexOf('[');
							if (pos > 0) {
								dateStringWithoutId = oldValue.slice(0, pos);
							}

							let baseDate = new Date(dateStringWithoutId);

							// baseDate is shifted by our offset to UTC and its own offset to UTC
							let theirOffset = getOffset(timezoneId);
							let ouroffset   = getOffset(Intl.DateTimeFormat().resolvedOptions().timeZone);
							let totalOffset = ouroffset - theirOffset;

							// correct baseDate
							baseDate.setMinutes(baseDate.getMinutes() - totalOffset);

							$(input).datetimepicker('setDate', baseDate);
						}

						$('#ui-datepicker-div').addClass('is-zoned');
					}

				} else if (isEnum) {

					let format = _Crud.helpers.getFormat(type, key);
					cell.text(value ?? '');
					if (!readOnly) {
						cell.on('click', function (event) {
							event.preventDefault();
							_Crud.objectList.appendEnumSelect(cell, id, key, format);
						});
						if (!id) {
							// create dialog
							_Crud.objectList.appendEnumSelect(cell, id, key, format);
						}
					}

				} else if (isCypher) {

					cell.text((value === undefined || value === null) ? '' : JSON.stringify(value));

				} else if (isCollection) { // Array types

					let values   = value ?? [];
					let typeInfo = _Crud.types[type].views.all;

					let displayArrayField = () => {

						cell.append(_Helpers.formatArrayValueField(key, values, typeInfo[key]));

						cell.find(`[name="${key}"]`).each(function (i, el) {
							_Entities.activateInput(el, id, key, type, typeInfo, () => {
								if (id) {
									_Crud.objectList.refreshObject(id, key);
								}
							});
						});
					};

					let threshold = UISettings.getValueForSetting(UISettings.settingGroups.crud.settings.hideLargeArrayElements);
					let showLargeArrayContentsClass = 'show-large-array';
					let largeArrayInfoElementClass  = 'array-attribute-very-big-info';

					if (values.length > threshold && !cell[0].classList.contains(showLargeArrayContentsClass)) {

						let cellContent = _Helpers.createSingleDOMElementFromHTML(`<span class="${largeArrayInfoElementClass}"></span>`);
						cell[0].appendChild(cellContent);

						_Helpers.appendInfoTextToElement({
							element: cellContent,
							text: `Attribute contains more than ${threshold} elements (${values.length}). As configured, it is not shown to preserve legibility. To show the contents, click this icon. You can adjust this threshold in the UI settings.`,
						});

						cellContent.addEventListener('click', (e) => {

							let warningElement = e.target.closest('.' + largeArrayInfoElementClass);
							warningElement.parentNode.classList.add(showLargeArrayContentsClass);
							warningElement.remove();

							displayArrayField();
						});

					} else {

						displayArrayField();
					}

				} else {
					// default: any other type of direct property

					cell.text(value ?? '');

					if (!readOnly) {
						cell.on('click', function(event) {
							event.preventDefault();
							var self = $(this);
							_Crud.objectList.activateTextInputField(self, id, key, propertyType);
						});
						if (!id) { // create
							_Crud.objectList.activateTextInputField(cell, id, key, propertyType);
						}
					}
				}

			} else {

				// This attribute is a relationship attribute, either a collection or a single object
				let simpleType = relatedType?.substring(relatedType.lastIndexOf('.') + 1);

				/**
				 * temporarily hides the entity form and shows the search interface.
				 */
				let addButtonClickHandler = (btn) => {

					if (id && !_Dialogs.custom.isDialogOpen()) {

						let { dialogText } = _Dialogs.custom.openDialog(`Add ${simpleType} to ${key}`);
						_Crud.search.displaySearchDialog(type, id, key, simpleType, $(dialogText));

					} else {

						let dialogText = $(_Dialogs.custom.getDialogTextElement());
						let scrollPos  = _Dialogs.custom.getDialogScrollPosition();

						$('#entityForm').hide();

						_Dialogs.custom.hideAllButtons();

						let backButton = _Dialogs.custom.appendCustomDialogButton('<button id="clear-log" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Back</button>');

						let searchFinishedFunction = (node = null) => {

							let nodeSelected = (node !== null);

							$('.searchBox', dialogText).remove();

							if (nodeSelected) {

								_Crud.objectList.getAndAppendNode(type, id, key, node, cell, node, true);

								if (!isCollection) {

									_Helpers.fastRemoveElement(btn);
								}
							}

							_Crud.search.clearSearchResults(dialogText);

							_Dialogs.custom.showAllButtons();

							$('#entityForm').show();

							_Dialogs.custom.setDialogScrollPosition(scrollPos);

							_Helpers.fastRemoveElement(backButton);
						};

						backButton.addEventListener('click', () => {
							searchFinishedFunction();
						});

						_Crud.search.displaySearchDialog(type, id, key, simpleType, dialogText, searchFinishedFunction);
					}
				};

				let showAddButton = () => {

					let addBtn = _Helpers.createSingleDOMElementFromHTML(_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['add', 'icon-lightgrey', 'cursor-pointer'])));

					cell.append($(addBtn));

					addBtn.addEventListener('click', () => {
						addButtonClickHandler(addBtn);
					});
				};

				if (isCollection) {

					showAddButton();

					if (id && !isCollectionIdProperty && !isCollectionNotionProperty) {

						_Crud.objectList.pager.cellPager.append(cell, id, type, key);
					}

				} else {

					if (value) {

						_Crud.objectList.getAndAppendNode(type, id, key, value, cell);

					} else if (simpleType) {

						showAddButton();
					}
				}
			}

			if (id && !isSourceOrTarget && !readOnly && !relatedType && propertyType !== 'Boolean') {

				cell.prepend(_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['crud-clear-value', 'icon-lightgrey', 'cursor-pointer'])));

				$('.crud-clear-value', cell).on('click', function(e) {
					e.preventDefault();
					_Crud.objectList.crudEmptyKeyForObject(id, key);
					return false;
				});
			}
		},
		activateTextInputField: (el, id, key, propertyType) => {

			var oldValue = el.text();
			el.off('click');
			var input;
			if (propertyType === 'String') {
				el.html(`<textarea name="${key}" class="__value"></textarea>`);
				input = $('textarea', el);
			} else {
				el.html(`<input name="${key}" class="__value" type="text" size="10">`);
				input = $('input', el);
			}
			input.val(oldValue);
			input.off('click');
			input.focus();
			input.on('blur', function() {
				var newValue = input.val();
				if (id) {
					_Crud.objectList.crudUpdate(id, key, newValue, oldValue);
				}
			});

		},
		appendEnumSelect: (cell, id, key, format) => {

			cell.off('click');

			let oldValue       = cell.text().trim();
			let possibleValues = format.split(',').map(value => value.trim()).filter(value => value.length > 0);
			possibleValues.unshift('');

			_Helpers.fastRemoveAllChildren(cell[0]);

			let select = _Helpers.createSingleDOMElementFromHTML(`
				<select name="${key}">
					${possibleValues.map(value => `<option ${value === oldValue ? 'selected="selected"' : ''}value="${value}">${value}</option>`).join()}
				</select>
			`);

			cell[0].appendChild(select);
			select.focus();

			// only attach handlers if we are editing an object
			if (id) {

				let blurHandler = (e) => {
					_Crud.objectList.resetCellToOldValue(id, key, oldValue);
				};

				select.addEventListener('change', function(e) {
					let newValue = select.value;
					select.removeEventListener('blur', blurHandler);
					_Crud.objectList.crudUpdate(id, key, newValue, oldValue);
				});

				select.addEventListener('blur', blurHandler);
			}
		},
		getCellsForKeyInObject: (id, key) => {

			let row = _Crud.objectList.getRow(id);

			let cellInMainTable    = $(_Crud.helpers.getCSSSelectorForKey(key), row);
			let cellInDetailsTable = $(_Crud.helpers.getCSSSelectorForKey(key), $('#details_' + id));

			let result = [];

			if (cellInMainTable && cellInMainTable.length > 0) {
				result.push(cellInMainTable);
			}

			if (cellInDetailsTable && cellInDetailsTable.length > 0) {
				result.push(cellInDetailsTable);
			}

			return result;
		},
		crudUpdate: (id, key, newValue, oldValue, onSuccess, onError) => {

			let json = JSON.stringify({
				[key]: (newValue && newValue !== '') ? newValue : null
			});

			onSuccess ??= () => {
				_Crud.objectList.refreshObject(id, key, oldValue);
			};

			onError ??= () => {
				_Crud.objectList.crudRefreshSingleKey(id, key);
			};

			_Crud.objectList.crudUpdateObj(id, key, json, onSuccess, onError);
		},
		crudUpdateObj: (id, key, json, onSuccess, onError) => {

			let url = `${Structr.rootUrl}${id}`;

			fetch(url, {
				method: 'PUT',
				body: json
			}).then(async response => {

				let data = await response.json();

				if (response.ok) {

					if (typeof onSuccess === "function") {
						onSuccess();
					} else {
						_Crud.objectList.refreshObject(id);
					}

				} else {

					Structr.errorFromResponse(data, url, { statusCode: response.status, requiresConfirmation: true });

					if (typeof onError === "function") {
						onError();
					} else {
						_Crud.objectList.crudRefreshSingleKey(id, key);
					}
				}
			});
		},
		crudRefreshSingleKey: (id, key) => {

			fetch(`${Structr.rootUrl}${id}/all`, {
				headers: {
					Accept: `application/json; charset=utf-8; properties=id,type,${key}`
				}
			}).then(async response => {

				if (response.ok) {

					let data = await response.json();

					if (data) {
						_Crud.objectList.resetCellToOldValue(id, key, data.result[key]);
					}
				}
			});
		},
		crudEmptyKeyForObject: (id, key, onSuccess, onError) => {

			let url = `${Structr.rootUrl}${id}`;
			let obj = {
				[key]: null
			};

			fetch(url, {
				method: 'PUT',
				body: JSON.stringify(obj)
			}).then(async response => {

				let data = await response.json();

				if (response.ok) {

					if (typeof onSuccess === "function") {
						onSuccess();
					} else {
						_Crud.objectList.refreshObject(id, key);
					}

				} else {

					Structr.errorFromResponse(data, url, { statusCode: response.status, requiresConfirmation: true });

					if (typeof onError === "function") {
						onError();
					} else {
						_Crud.objectList.crudRefreshSingleKey(id, key);
					}
				}
			});
		},
		getAndAppendNode: (parentType, parentId, key, obj, cell, preloadedNode, insertFakeInput) => {

			if (!obj) {
				return;
			}
			let id, type;
			if ((typeof obj) === 'object') {
				id = obj.id;
				type = obj.type;
			} else if (_Helpers.isUUID(obj)) {
				id = obj;
			} else {
				// search object by name
				type = _Crud.types[parentType].views.all[key].relatedType.split('.').pop();

				fetch(Structr.rootUrl + type + '?name=' + obj).then(async response => {

					let data = await response.json();

					if (response.ok) {
						if (data.result.length > 0) {
							_Crud.objectList.getAndAppendNode(parentType, parentId, key, data.result[0], cell);
						}
					}
				});

				return;
			}

			let nodeHandler = (node) => {

				let newElement = _Helpers.createSingleDOMElementFromHTML(_Entities.getRelatedNodeHTML(node, null));
				let nodeEl = $(newElement);
				cell.append(nodeEl);

				if (insertFakeInput) {
					nodeEl.append(`<input type="hidden" name="${key}" value="${node.id}"></div>`);
				}

				if (node.isImage) {

					let nodeIDToShow = (node.tnSmall) ? node.tnSmall.id : node.id;
					let resText      = `${node.width||'?'} x ${node.height||'?'}`;

					nodeEl.append(`<div class="wrap"><img class="thumbnail" src="/${nodeIDToShow}"><div class="image-info-overlay">${resText}</div></div>`);

					$('.thumbnail', nodeEl).on('mouseenter', function(e) {
						e.stopPropagation();
						$('.thumbnailZoom').remove();

						let tnZoom = $(_Helpers.createSingleDOMElementFromHTML(`<img class="thumbnailZoom" src="/${node.tnMid?.id ?? node.id}">`));
						nodeEl.parent().append(tnZoom);

						tnZoom.css({
							top:  (nodeEl.position().top) + 'px',
							left: (nodeEl.position().left - 42) + 'px'
						});
						tnZoom.on('mouseleave', function(e) {
							e.stopPropagation();
							_Helpers.fastRemoveElement(tnZoom[0])
						});
					});
				}

				$('.remove', nodeEl).on('click', function(e) {
					e.preventDefault();

					let parentObjStub = {
						type: parentType,
						id: parentId
					};

					if (parentId) {

						_Crud.objectList.removeRelatedObject(parentObjStub, key, obj);

					} else {

						_Helpers.fastRemoveElement(newElement);

						if (!_Crud.helpers.isCollection(key, parentType)) {

							_Crud.objectList.populateCell(null, key, parentType, null, cell);
						}
					}

					return false;
				});

				if (parentId) {
					nodeEl.on('click', function(e) {
						e.preventDefault();
						_Crud.objectList.showDetails(node.id, node.type);
						return false;
					});
				}
			};

			if (preloadedNode) {
				nodeHandler(preloadedNode);
			} else {
				_Crud.crudCache.registerCallback({ id: id, type: type }, id, nodeHandler);
			}
		},
		removeRelatedObject: (parentObj, key, relatedObj, callback) => {

			let type = parentObj.type;

			if (_Crud.helpers.isCollection(key, type)) {

				fetch(Structr.rootUrl + type + '/' + parentObj.id + '/all').then(async response => {

					if (response.ok) {

						let data      = await response.json();
						let relatedId = (typeof relatedObj === 'object' ? relatedObj.id : relatedObj);
						let objects   = _Crud.helpers.extractIds(data.result[key]).filter(obj => (obj.id !== relatedId));

						_Crud.objectList.updateRelatedCollection(parentObj.id, key, objects, callback);
					}
				});

			} else {

				_Crud.objectList.crudEmptyKeyForObject(parentObj.id, key);
			}
		},
		addRelatedObject: (type, id, key, relatedObj, callback) => {

			if (_Crud.helpers.isCollection(key, type)) {

				fetch(`${Structr.rootUrl}${type}/${id}/all`).then(async response => {

					if (response.ok) {

						let data    = await response.json();
						let objects = _Crud.helpers.extractIds(data.result[key]);
						if (objects.includes(relatedObj.id) === false) {
							objects.push({ id: relatedObj.id });
						}

						_Crud.objectList.updateRelatedCollection(id, key, objects, callback);
					}
				});

			} else {

				let updateObj = {
					[key]: relatedObj.id
				};

				_Crud.objectList.crudUpdateObj(id, key, JSON.stringify(updateObj), () => {
					_Crud.objectList.refreshObject(id, key);
					_Dialogs.custom.clickDialogCancelButton();
				});
			}
		},
		updateRelatedCollection: (id, key, objects, callback) => {

			let updateObj = {};
			updateObj[key] = objects;

			_Crud.objectList.crudUpdateObj(id, key, JSON.stringify(updateObj), () => {
				_Crud.objectList.refreshObject(id, key);
				callback?.();
			});
		},
		refreshObject: (id, key, oldValue) => {

			let fetchConfig = {};

			if (key) {
				fetchConfig.headers = {
					Accept: `application/json; charset=utf-8; properties=id,type,${key}`
				};
			}

			fetch(`${Structr.rootUrl}${id}/all`, fetchConfig).then(async response => {

				if (response.ok) {

					let data = await response.json();

					if (data) {

						if (key) {

							// refresh cell(s) with new value
							let newValue = data.result[key];

							let cells = _Crud.objectList.getCellsForKeyInObject(id, key);

							for (let cell of cells) {

								_Helpers.fastRemoveAllChildren(cell[0]);
								_Crud.objectList.populateCell(id, key, data.result.type, newValue, cell);

								if (newValue !== oldValue && !(!newValue && oldValue === '')) {
									_Helpers.blinkGreen(cell);
								}
							}

							if (key === Structr.internalKeys.name) {

								// if the name changed, update all occurrences as a related node
								let otherDisplayedNodes = document.querySelectorAll('.related-node._' + id);
								for (let other of otherDisplayedNodes) {
									_Entities.updateRelatedNodeName(other, newValue ?? id);
								}
							}

						} else {

							// update complete row
							let row = _Crud.objectList.getRow(id);
							_Helpers.fastRemoveAllChildren(row[0]);

							_Crud.objectList.populateRow(id, data.result, data.result.type, _Crud.types[type].views.all);
						}
					}
				}
			});
		},
		resetCellToOldValue: (id, key, oldValue) => {

			let cells = _Crud.objectList.getCellsForKeyInObject(id, key);

			for (let cell of cells) {

				_Helpers.fastRemoveAllChildren(cell[0]);
				_Crud.objectList.populateCell(id, key, _Crud.type, oldValue, cell);
			}
		},
		updateResourceLink: (type) => {

			let resourceLink = document.querySelector('#crud-type-detail .resource-link a');
			let endpointURL  = `${Structr.rootUrl}${type}/${_Crud.view[type]}?${Structr.getRequestParameterName('pageSize')}=${_Crud.pageSize[type]}&${Structr.getRequestParameterName('page')}=${_Crud.page[type]}`;

			resourceLink.setAttribute('href', endpointURL);
			resourceLink.textContent = endpointURL;
		},
		showDetails: (id, type) => {

			if (!type) {
				new ErrorMessage().text('Missing type').requiresConfirmation().show();
				return;
			}

			let typeDef = _Crud.types[type]?.views?.all;

			if (!typeDef) {
				_Crud.helpers.ensurePropertiesForTypeAreLoaded(type, () => {
					_Crud.objectList.showDetails(id, type);
				});
				return;
			}

			let availableKeys = Object.keys(typeDef);
			let visibleKeys   = _Crud.objectList.filterKeys(type, availableKeys);

			if (_Dialogs.custom.isDialogOpen()) {
				_Dialogs.custom.clickDialogCancelButton();
			}

			let view = _Crud.view[type] || 'ui';

			fetch(`${Structr.rootUrl}${type}/${id}/${view}`, {
				headers: {
					Range: _Crud.objectList.getRangeHeaderForType(type),
					Accept: `application/json; charset=utf-8;properties=${visibleKeys.join(',')}`
				}
			}).then(async response => {

				let data = await response.json();
				if (!data)
					return;

				let node = data.result;

				let { dialogText } = _Dialogs.custom.openDialog(`Details of ${type} ${node?.name ?? node.id}`);

				let deleteBtn = _Dialogs.custom.appendCustomDialogButton(_Dialogs.custom.templates.deleteButton());

				deleteBtn.addEventListener('click', async (e) => {

					let deleted = await _Crud.helpers.crudAskDelete(type, id);

					if (deleted) {
						_Dialogs.custom.getCloseDialogButton().click();
					}
				});

				dialogText.insertAdjacentHTML('beforeend', `<form id="entityForm"><table class="props" id="details_${node.id}"><tr><th>Name</th><th>Value</th></tr></table><form>`);

				document.querySelector('#entityForm').addEventListener('submit', e => e.preventDefault());

				let table = dialogText.querySelector('table');

				for (let key of visibleKeys) {

					let row = _Helpers.createSingleDOMElementFromHTML(`
						<tr>
							<td class="key"><label>${key}</label></td>
							<td class="__value relative"></td>
						</tr>
					`);
					table.appendChild(row);

					let cell = $(row.querySelector(`.__value`));

					let isCollection            = _Crud.helpers.isCollection(key, type);
					let isRelationshipAttribute = _Crud.helpers.getRelatedTypeForAttribute(key, type) !== undefined;
					if (isCollection && isRelationshipAttribute) {
						_Crud.objectList.pager.addPageSizeConfigToColumn(cell.prev('td'), type, key, () => {
							_Crud.objectList.showDetails(node.id, type);
						});
					}

					_Crud.objectList.populateCell(node.id, key, node.type, node[key], cell);
				}

				if (node && node.isImage) {
					dialogText.insertAdjacentHTML('beforeend', `<div class="img"><div class="wrap"><img class="thumbnailZoom" src="/${node.id}"></div></div>`);
				}
			});
		},
		getDefaultHiddenKeys: (type) => {

			let hiddenKeys = [];

			const hiddenKeysForAllTypes       = [ 'base', 'createdBy', 'lastModifiedBy', 'hidden', 'internalEntityContextPath', 'grantees' ];
			const hiddenKeysForFileTypes      = [ 'base64Data', 'favoriteContent', 'favoriteContext', 'favoriteUsers', 'relationshipId', 'resultDocumentForExporter', 'documentTemplateForExporter', 'isFile', 'position', 'extractedContent', 'indexedWords', 'fileModificationDate' ];
			const hiddenKeysForImageTypes     = [ 'base64Data', 'imageData', 'favoriteContent', 'favoriteContext', 'favoriteUsers', 'resultDocumentForExporter', 'documentTemplateForExporter', 'isFile', 'position', 'extractedContent', 'indexedWords', 'fileModificationDate' ];
			const hiddenKeysForPrincipalTypes = [ 'isUser', 'isAdmin', 'createdBy', 'sessionIds', 'publicKeys', 'sessionData', 'password', 'passwordChangeDate', 'salt', 'twoFactorSecret', 'twoFactorToken', 'isTwoFactorUser', 'twoFactorConfirmed', 'ownedNodes', 'localStorage' ];

			let typeDef = _Crud.types[type];

			let hasPrincipalTrait = typeDef.traits.includes['Principal'];
			let hasImageTrait     = typeDef.traits.includes['AbstractFile'];
			let hasFileTrait      = typeDef.traits.includes['Image'];

			if (hasPrincipalTrait) {

				for (let key of hiddenKeysForPrincipalTypes) {
					if (!hiddenKeys.includes(key)) {
						hiddenKeys.push(key);
					}
				}
			}

			if (hasImageTrait) {

				for (let key of hiddenKeysForImageTypes) {
					if (!hiddenKeys.includes(key)) {
						hiddenKeys.push(key);
					}
				}
			}

			if (hasFileTrait) {

				for (let key of hiddenKeysForFileTypes) {
					if (!hiddenKeys.includes(key)) {
						hiddenKeys.push(key);
					}
				}
			}

			// hidden keys for all types
			for (let key of hiddenKeysForAllTypes) {
				if (!hiddenKeys.includes(key)) {
					hiddenKeys.push(key);
				}
			}

			// hidden keys depending on property type
			for (let key in _Crud.types[type]?.views?.all ?? {}) {

				let isEntityIdProperty         = _Crud.helpers.isEntityIdProperty(key, type);
				let isMethodProperty           = _Crud.helpers.isMethodProperty(key, type);
				let isCollectionIdProperty     = _Crud.helpers.isCollectionIdProperty(key, type);
				let isCollectionNotionProperty = _Crud.helpers.isCollectionNotionProperty(key, type);

				if (isEntityIdProperty || isMethodProperty || isCollectionIdProperty || isCollectionNotionProperty) {
					hiddenKeys.push(key);
				}
			}

			return hiddenKeys;
		},
		getHiddenKeys: (type) => {

			let savedHiddenKeys = LSWrapper.getItem(_Crud.objectList.crudHiddenColumnsKey + type);
			let hiddenKeys = [];
			if (savedHiddenKeys) {

				hiddenKeys = JSON.parse(savedHiddenKeys);

			} else {

				// if we have no savestate, hide according to our default hide rules
				hiddenKeys = _Crud.objectList.getDefaultHiddenKeys(type);
			}

			return hiddenKeys;
		},
		filterKeys: (type, sourceArray) => {

			if (!sourceArray) {
				return;
			}

			// contains the SORTING of all attributes
			let sortOrder    = _Crud.objectList.getSavedSortOrderOfColumns(type);
			let hiddenKeys   = _Crud.objectList.getHiddenKeys(type);

			// 1. remove all hidden keys
			sortOrder = sortOrder.filter(key => !(hiddenKeys.includes(key)));

			// 2. remove all keys that are not in sourceArray
			sortOrder = sortOrder.filter(key => sourceArray.includes(key));

			// 3. remove alle keys that have the serializationDisabled flag active
			sortOrder = sortOrder.filter(key => !_Crud.helpers.isSerializationDisabled(type, key));

			// always have id,type,name as the first elements of the array
			let idTypeName = ['id', 'type', 'name'];

			sortOrder = sortOrder.filter(key => {
				return (idTypeName.includes(key) === false);
			});

			sortOrder.unshift(...idTypeName);

			return sortOrder;
		},
		getRangeHeaderForType: (type) => {
			let ranges = '';
			let keys;
			if (type && _Crud.types[type]?.views?.all) {
				keys = Object.keys(_Crud.types[type]?.views?.all);
			}

			if (!keys) {
				let typeDef = _Crud.type[type];
				if (typeDef) {
					keys = Object.keys(typeDef.views[_Crud.view[type]]);
				}
			}

			if (keys) {

				for (let key of keys) {

					if (_Crud.helpers.isCollection(key, type)) {

						let page     = 1;
						let pageSize = _Crud.objectList.pager.getCollectionPageSize(type, key);
						let start    = (page-1)*pageSize;
						let end      = page*pageSize;
						ranges += `${key}=${start}-${end};`;
					}
				}

				return ranges;
			}
		},
		pager: {
			defaultPage: 1,
			defaultPageSize: 10,
			defaultCollectionPageSize: 10,
			crudPagerDataKey: 'structrCrudPagerData_' + location.port + '_',
			addPager: (type, el) => {

				_Crud.page[type]     ??= _Helpers.getURLParameter('page') ?? (_Crud.objectList.pager.defaultPage ?? 1);
				_Crud.pageSize[type] ??= _Helpers.getURLParameter('pageSize') ?? (_Crud.objectList.pager.defaultPageSize ?? 10);

				el.insertAdjacentHTML('beforeend', _Crud.objectList.pager.templates.pagerHTML({ type }));

				_Helpers.appendInfoTextToElement({
					element: el.querySelector('select.view'),
					text: 'The attributes of the given view are fetched. Attributes can still be hidden using the "Configure columns" dialog. id and type are always shown first.',
					insertAfter: true,
					customToggleIconClasses: ['icon-blue', 'ml-1']
				});

				_Helpers.appendInfoTextToElement({
					element: el.querySelector('.resource-link'),
					text: "View the REST output in a new tab.",
					customToggleIconClasses: ['icon-blue', 'ml-1'],
					offsetY: 10
				});

				return $('.pager', el);
			},
			activatePagerElements: (type, pagerNode) => {

				$('.pageNo', pagerNode).on('keypress', function(e) {
					if (e.keyCode === 13) {
						_Crud.page[type] = $(this).val();
						_Crud.objectList.refreshList(type);
					}
				});

				$('.pageSize', pagerNode).on('keypress', function(e) {
					if (e.keyCode === 13) {
						// calculate which page we should be on after the pagesize changed
						var oldFirstObject = ((_Crud.page[type] -1 ) * _Crud.pageSize[type]) + 1;
						var newPage = Math.ceil(oldFirstObject / $(this).val());
						_Crud.pageSize[type] = $(this).val();
						_Crud.page[type] = newPage;
						_Crud.objectList.refreshList(type);
					}
				});

				$('select.view', pagerNode).on('change', function(e) {
					_Crud.view[type] = $(this).val();
					_Crud.objectList.updateCrudTableHeader(type);
					_Crud.objectList.refreshList(type);
				});

				let pageLeft  = $('.pageLeft', pagerNode);
				let pageRight = $('.pageRight', pagerNode);

				pageLeft.on('click', function() {
					pageRight.removeAttr('disabled').removeClass('disabled');
					if (_Crud.page[type] > 1) {
						_Crud.page[type]--;
						_Crud.objectList.refreshList(type);
					}
				});

				pageRight.on('click', function() {
					pageLeft.removeAttr('disabled').removeClass('disabled');
					if (_Crud.page[type] < _Crud.pageCount) {
						_Crud.page[type]++;
						_Crud.objectList.refreshList(type);
					}
				});

			},
			deActivatePagerElements: (pagerNode) => {

				$('.pageNo', pagerNode).off('keypress');
				$('.pageSize', pagerNode).off('keypress');
				$('.pageLeft', pagerNode).off('click');
				$('.pageRight', pagerNode).off('click');
			},
			updatePager: (type, data) => {

				let pageCount   = data.page_count;
				let softLimited = false;
				let typeNode = $('#crud-type-detail');
				if (typeNode.length === 0) {
					return;
				}

				$('.queryTime', typeNode).text(data.query_time);
				$('.serTime', typeNode).text(data.serialization_time);
				$('.pageSize', typeNode).val(data.page_size);

				_Crud.page[type] = data.page;
				$('.pageNo', typeNode).val(_Crud.page[type]);

				if (pageCount === undefined) {
					pageCount = _Helpers.softlimit.getSoftLimitedPageCount(data.page_size);
					softLimited = true;
				}

				_Crud.pageCount = pageCount;
				let pageCountInput = $('.pageCount', typeNode);
				pageCountInput.val(_Crud.pageCount);

				if (softLimited) {
					_Helpers.softlimit.showSoftLimitAlert(pageCountInput);
				} else {
					_Helpers.softlimit.showActualResultCount(pageCountInput, data.result_count);
				}

				let pageLeft = $('.pageLeft', typeNode);
				let pageRight = $('.pageRight', typeNode);

				if (_Crud.page[type] < 2) {
					pageLeft.attr('disabled', 'disabled').addClass('disabled');
				} else {
					pageLeft.removeAttr('disabled').removeClass('disabled');
				}

				if (!_Crud.pageCount || _Crud.pageCount === 0 || (_Crud.page[type] === _Crud.pageCount)) {
					pageRight.attr('disabled', 'disabled').addClass('disabled');
				} else {
					pageRight.removeAttr('disabled').removeClass('disabled');
				}

				_Crud.objectList.pager.updateUrl(type);
			},
			storePagerData: () => {
				let type      = _Crud.type;
				let pagerData = `${_Crud.view[type]},${_Crud.sort[type]},${_Crud.order[type]},${_Crud.page[type]},${_Crud.pageSize[type]}`;
				LSWrapper.setItem(_Crud.objectList.pager.crudPagerDataKey + type, pagerData);
			},
			restorePagerData: (type) => {
				let val  = LSWrapper.getItem(_Crud.objectList.pager.crudPagerDataKey + type);

				if (val) {
					let pagerData        = val.split(',');
					_Crud.view[type]     = pagerData[0];
					_Crud.sort[type]     = pagerData[1];
					_Crud.order[type]    = pagerData[2];
					_Crud.page[type]     = pagerData[3];
					_Crud.pageSize[type] = pagerData[4];
				}
			},
			determinePagerData: (type) => {

				// Priority: JS vars -> Local Storage -> URL -> Default

				if (!_Crud.view[type]) {
					_Crud.view[type]     = _Helpers.getURLParameter('view');
					_Crud.sort[type]     = _Helpers.getURLParameter('sort');
					_Crud.order[type]    = _Helpers.getURLParameter('order');
					_Crud.pageSize[type] = _Helpers.getURLParameter('pageSize');
					_Crud.page[type]     = _Helpers.getURLParameter('page');
				}

				if (!_Crud.view[type]) {
					_Crud.objectList.pager.restorePagerData(type);
				}

				if (!_Crud.view[type]) {
					_Crud.view[type]     = _Crud.defaultView;
					_Crud.sort[type]     = _Crud.defaultSort;
					_Crud.order[type]    = _Crud.defaultOrder;
					_Crud.pageSize[type] = _Crud.objectList.pager.defaultPageSize;
					_Crud.page[type]     = _Crud.objectList.pager.defaultPage;
				}
			},
			updateUrl: (type) => {

				if (type) {
					_Crud.type = type;
					_Crud.objectList.pager.storePagerData();
					_Crud.objectList.updateResourceLink(type);
				}

				_Crud.search.focusSearchField();
			},
			addPageSizeConfigToColumn: (el, type, key, callback) => {

				el.append(`<input type="text" class="collection-page-size" size="1" value="${_Crud.objectList.pager.getCollectionPageSize(type, key)}">`);

				let update = (newPageSize) => {
					if (newPageSize !== _Crud.objectList.pager.getCollectionPageSize(type, key)) {
						_Crud.objectList.pager.setCollectionPageSize(type, key, newPageSize);
						if (callback) {
							callback();
						} else {
							_Crud.objectList.refreshList(type);
						}
					}
				};

				$('.collection-page-size', el).on('blur', function() {
					update($(this).val());
				});

				$('.collection-page-size', el).on('keypress', function(e) {
					if (e.keyCode === 13) {
						update($(this).val());
					}
				});
			},
			getCollectionPageSizeLSKey: (type, key) => `${_Crud.objectList.pager.crudPagerDataKey}_collectionPageSize_${type}.${key}`,
			setCollectionPageSize: (type, key, value) => LSWrapper.setItem(_Crud.objectList.pager.getCollectionPageSizeLSKey(type, key), value),
			getCollectionPageSize: (type, key) =>  LSWrapper.getItem(_Crud.objectList.pager.getCollectionPageSizeLSKey(type, key), _Crud.objectList.pager.defaultCollectionPageSize),
			cellPager: {
				append: (el, id, type, key) => {

					let pageSize = _Crud.objectList.pager.getCollectionPageSize(type, key);

					// use public view for cell pager - we should not need more information than this!
					fetch(`${Structr.rootUrl}${type}/${id}/${key}/public${_Crud.helpers.getSortAndPagingParameters(null, null, null, pageSize, null)}`).then(async response => {

						let data = await response.json();

						if (response.ok) {

							let softLimited = false;
							let resultCount = data.result_count;
							let pageCount   = data.page_count;

							if (data.result && data.result.length > 0) {
								for (let preloadedNode of data.result) {
									_Crud.objectList.getAndAppendNode(type, id, key, preloadedNode.id, el, preloadedNode);
								}
							}

							let page = 1;

							// handle new soft-limited REST result without counts
							if (data.result_count === undefined && data.page_count === undefined) {
								resultCount = _Helpers.softlimit.getSoftLimitedResultCount();
								pageCount   = _Helpers.softlimit.getSoftLimitedPageCount(pageSize);
								softLimited = true;
							}

							if (!resultCount || !pageCount || pageCount === 1) {
								return;
							}

							el.prepend('<div class="cell-pager"></div>');

							el[0].insertAdjacentHTML('afterbegin', _Crud.objectList.pager.templates.cellPagerHTML({ page, pageCount }))

							if (page > 1) {
								$('.cell-pager .prev', el).removeClass('disabled').prop('disabled', '');
							}

							$('.collection-page', $('.cell-pager', el)).on('blur', function() {
								var newPage = $(this).val();
								_Crud.objectList.pager.cellPager.update(el, id, type, key, newPage, pageSize);
							});

							$('.collection-page', $('.cell-pager', el)).on('keypress', function(e) {
								if (e.keyCode === 13) {
									var newPage = $(this).val();
									_Crud.objectList.pager.cellPager.update(el, id, type, key, newPage, pageSize);
								}
							});

							if (page < pageCount) {
								$('.cell-pager .next', el).removeClass('disabled').prop('disabled', '');
							}

							$('.prev', el).on('click', function() {
								let page    = $('.cell-pager .collection-page', el).val();
								let newPage = Math.max(1, page - 1);
								_Crud.objectList.pager.cellPager.update(el, id, type, key, newPage, pageSize);
							});

							$('.next', el).on('click', function() {
								let page    = $('.cell-pager .collection-page', el).val();
								let newPage = parseInt(page) + 1;
								_Crud.objectList.pager.cellPager.update(el, id, type, key, newPage, pageSize);
							});

							if (softLimited) {
								_Helpers.softlimit.showSoftLimitAlert($('input.page-count'));
							}
						}
					});
				},
				update: (el, id, type, key, page, pageSize) => {

					fetch(`${Structr.rootUrl}${type}/${id}/${key}/public?${Structr.getRequestParameterName('page')}=${page}&${Structr.getRequestParameterName('pageSize')}=${pageSize}`).then(async response => {

						if (response.ok) {

							let data = await response.json();

							let softLimited = false;
							let pageCount   = data.page_count;

							// handle new soft-limited REST result without counts
							if (data.result_count === undefined && data.page_count === undefined) {
								pageCount = _Helpers.softlimit.getSoftLimitedPageCount(pageSize);
								softLimited = true;
							}

							$('.cell-pager .collection-page', el).val(page);
							$('.cell-pager .page-count', el).val(pageCount);

							if (page > 1) {
								$('.cell-pager .prev', el).removeClass('disabled').prop('disabled', '');
							} else {
								$('.cell-pager .prev', el).addClass('disabled').prop('disabled', 'disabled');
							}
							if (page < pageCount) {
								$('.cell-pager .next', el).removeClass('disabled').prop('disabled', '');
							} else {
								$('.cell-pager .next', el).addClass('disabled').prop('disabled', 'disabled');
							}

							for (let child of el.children('.node')) {
								_Helpers.fastRemoveElement(child);
							}

							for (let preloadedNode of data.result) {
								_Crud.objectList.getAndAppendNode(type, id, key, preloadedNode.id, el, preloadedNode);
							}

							if (softLimited) {
								_Helpers.softlimit.showSoftLimitAlert($('input.page-count'));
							}
						}
					});
				},
			},
			templates: {
				pagerHTML: config => `
					<div class="flex items-center justify-between">
		
						<div class="pager whitespace-nowrap flex items-center">
							<button class="pageLeft flex" style="margin: 0! important; padding: 0.5rem 0.25rem; background: transparent;">
								${_Icons.getSvgIcon(_Icons.iconChevronLeft)}
							</button>
							<span class="pageWrapper">
								<input class="pageNo" type="text" size="3" value="${_Crud.page[config.type]}">
								<span class="of">of</span>
								<input readonly="readonly" class="readonly pageCount" type="text" size="3" value="${_Crud.pageCount ?? 0}">
							</span>
							<button class="pageRight flex" style="margin: 0! important; padding: 0.5rem 0.25rem; background: transparent;">
								${_Icons.getSvgIcon(_Icons.iconChevronRight)}
							</button>
							<span class="ml-2 mr-1">Page Size:</span>
							<input class="pageSize" type="text" value="${_Crud.pageSize[config.type]}">
							<span class="ml-2 mr-1">View:</span>
							<select class="view hover:bg-gray-100 focus:border-gray-666 active:border-green">
								${Object.keys(_Crud.types[config.type].views).map(view => `<option${(_Crud.view[config.type] === view) ? ' selected' : ''}>${view}</option>`).join('')}
							</select>
						</div>
		
						<div class="resource-link mr-4 flex items-center">
							<a target="_blank" href=""></a>
						</div>
					</div>
				`,
				cellPagerHTML: config => `
					<div class="cell-pager whitespace-nowrap flex items-center">
						<button class="prev disabled flex" style="margin: 0! important; padding: 0.5rem 0.25rem; background: transparent;">
							${_Icons.getSvgIcon(_Icons.iconChevronLeft)}
						</button>
						<span class="pageWrapper">
							<input class="collection-page" type="text" size="1" value="${config.page}">
							<span class="of">of</span>
							<input readonly="readonly" class="readonly page-count" type="text" size="1" value="${config.pageCount}">
						</span>
						<button class="next disabled flex" style="margin: 0! important; padding: 0.5rem 0.25rem; background: transparent;">
							${_Icons.getSvgIcon(_Icons.iconChevronRight)}
						</button>
					</div>
				`
			}
		},
		excludeInheritingTypes: {
			lsKey: 'structrCrudExactType_' + location.port,
			config: {},
			restoreConfig: () => {
				_Crud.objectList.excludeInheritingTypes.config = LSWrapper.getItem(_Crud.objectList.excludeInheritingTypes.lsKey, {});
			},
			update: (type, isExact) => {
				_Crud.objectList.excludeInheritingTypes.config[type] = isExact;
				LSWrapper.setItem(_Crud.objectList.excludeInheritingTypes.lsKey, _Crud.objectList.excludeInheritingTypes.config);
			},
			shouldExclude: (type) => {
				return _Crud.objectList.excludeInheritingTypes.config[type];
			}
		},
	},
	search: {
		searchField: undefined,
		searchFieldClearIcon: undefined,
		setupGlobalSearch: () => {

			let crudMain = $('#crud-main');

			_Crud.search.searchField          = document.getElementById('crud-search-box');
			_Crud.search.searchFieldClearIcon = document.querySelector('.clearSearchIcon');
			_Crud.search.focusSearchField();

			_Helpers.appendInfoTextToElement({
				element: _Crud.search.searchField,
				text: 'By default, a fuzzy search is performed on the <code>name</code> attribute of <b>every</b> node type. Optionally, you can specify a type and an attribute to search as follows:<br><br>User.name:admin<br><br>If a UUID-string is supplied, the search is performed on the base node type to achieve the fastest results.',
				insertAfter: true,
				css: {
					left: '-18px',
					position: 'absolute'
				},
				helpElementCss: {
					fontSize: '12px',
					lineHeight: '1.1em'
				}
			});

			_Crud.search.searchFieldClearIcon.addEventListener('click', (e) => {
				_Crud.search.clearMainSearch(crudMain);
				_Crud.search.focusSearchField();
			});

			_Crud.search.searchField.addEventListener('keyup', (e) => {

				let searchString = _Crud.search.searchField.value;

				if (searchString && searchString.length) {
					_Crud.search.searchFieldClearIcon.style.display = 'block';
				}

				if (searchString && searchString.length && e.keyCode === 13) {

					_Crud.search.doSearch(searchString, crudMain, null, (e, node) => {
						e.preventDefault();
						_Entities.showProperties(node, 'ui');
						return false;
					});

					$('#crud-type-detail').hide();

				} else if (e.keyCode === 27 || searchString === '') {

					_Crud.search.clearMainSearch(crudMain);
				}
			});
		},
		/**
		 * Conduct a search and append search results to 'el'.
		 *
		 * If an optional type is given, restrict search to this type.
		 *
		 * Get only the given properties from the backend, otherwise just id,type,name.
		 */
		doSearch: (searchString, el, type, onClickCallback, optionalPageSize = 1000, blacklistedIds = [], properties = 'id,type,name,path,isImage,width,height,isThumbnail,isFile,isFolder') => {

			_Crud.search.clearSearchResults(el);

			let searchResults = _Helpers.createSingleDOMElementFromHTML(`
				<div class="searchResults">
					<h2>Search Results${(searchString !== '*' && searchString !== '') ? ` for "${searchString}"` : ''}</h2>
					<span class="search-results-info">Showing the first ${optionalPageSize} results. Use the input field to refine your search.</span>
				</div>
			`);

			el[0].insertAdjacentElement('beforeend', searchResults);

			Structr.resize();

			let types;
			let attr = 'name';
			let posOfColon = searchString.indexOf(':');

			if (posOfColon > -1) {

				let typeAndValue = searchString.split(':');
				let type = typeAndValue[0];
				let posOfDot = type.indexOf('.');

				if (posOfDot > -1) {
					let typeAndAttr = type.split('.');
					type = typeAndAttr[0];
					attr = typeAndAttr[1];
				}
				types = [_Helpers.capitalize(type)];
				searchString = typeAndValue[1];

			} else {

				if (type) {
					types = type.split(',').filter(t => (t.trim() !== ''));
				} else {
					// only search in node types
					types = Object.keys(_Crud.types).filter(t => !_Crud.types[t].isRel);
				}
				if (_Helpers.isUUID(searchString)) {
					attr = 'uuid';
					types = ['NodeInterface'];
				}
			}

			for (let type of types) {

				let url, searchPart;
				if (attr === 'uuid') {

					url = `${Structr.rootUrl}${type}/${searchString}`;

				} else {

					searchPart = (searchString === '*' || searchString === '') ? '' : `&${attr}=${encodeURIComponent(searchString)}&${Structr.getRequestParameterName('inexact')}=1`;
					url = `${Structr.rootUrl}${type}${_Crud.helpers.getSortAndPagingParameters(type, 'name', 'asc', optionalPageSize || 1000, 1)}${searchPart}`;
				}

				let resultsContainer = _Helpers.createSingleDOMElementFromHTML(`
					<div id="results-for-${type}" class="searchResultGroup resourceBox">
						<span class="flex items-center">${_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 24, 24, 'mr-2')} Searching for "${searchString}" in ${type}</span>
					</div>
				`);

				searchResults.insertAdjacentElement('beforeend', resultsContainer);

				fetch(url, {
					headers: {
						Accept: 'application/json; properties=' + properties
					}
				}).then(async response => {

					if (response.ok) {

						let data = await response.json();

						let filteredResult = [];
						let result         = data?.result ?? [];

						if (Array.isArray(result)) {

							filteredResult = result.filter(node => !blacklistedIds.includes(node.id));

						} else if (result.id && !blacklistedIds.includes(result.id)) {

							filteredResult.push(result);
						}

						_Helpers.fastRemoveAllChildren(resultsContainer);

						if (filteredResult.length > 0) {

							resultsContainer.insertAdjacentHTML('beforeend', `<h3>${type}</h3>`);

							_Crud.search.listSearchResults(resultsContainer, type, filteredResult, onClickCallback);

						} else {

							_Crud.search.noResults(resultsContainer, type);
						}

					} else {

						_Helpers.fastRemoveElement(resultsContainer);
					}
				});
			}
		},
		clearSearchResults: (el) => {

			let searchResults = $('.searchResults', el);
			if (searchResults.length) {
				_Helpers.fastRemoveElement(searchResults[0]);
				return true;
			}
			return false;
		},
		clearMainSearch: (el) => {

			_Crud.search.clearSearchResults(el);
			_Crud.search.searchFieldClearIcon.style.display = 'none';
			_Crud.search.searchField.value = '';
			$('#crud-type-detail').show();
		},
		focusSearchField: () => {

			// only auto-activate search field if no other input element is active
			if ( !(document.activeElement instanceof HTMLInputElement) ) {
				_Crud.search.searchField.focus();
			}
		},
		noResults: (resultsContainer, type) => {

			resultsContainer.insertAdjacentHTML('beforeend', `No results for ${type}`);
			window.setTimeout(() => {
				resultsContainer.remove();
			}, 1000);

		},
		listSearchResults: (resultsContainer, type, nodes, onClickCallback) => {

			for (let node of nodes) {

				let newNode = _Helpers.createSingleDOMElementFromHTML(_Entities.getRelatedNodeHTML(node, null, false));

				resultsContainer.insertAdjacentElement('beforeend', newNode)


				if (node.isImage) {
					newNode.insertAdjacentHTML('beforeend', `<div class="wrap"><img class="thumbnailZoom" src="/${node.id}" alt=""><div class="image-info-overlay">${node.width || '?'} x ${node.height || '?'}</div></div>`);
				}

				$(newNode).on('click', function (e) {
					onClickCallback(e, node);
				});
			}
		},
		displaySearchDialog: (parentType, id, key, type, el, callbackOverride) => {

			el.append(`
				<div class="searchBox searchBoxDialog flex justify-end">
					<input class="search" name="search" size="20" placeholder="Search">
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['clearSearchIcon', 'icon-lightgrey', 'cursor-pointer']), 'Clear Search')}
				</div>
			`);
			let searchBox = $('.searchBoxDialog', el);
			let search    = $('.search', searchBox);

			window.setTimeout(() => {
				search.focus();
			}, 250);

			search.keyup(function(e) {
				e.preventDefault();

				let searchString = search.val();
				if (e.keyCode === 13) {

					$('.clearSearchIcon', searchBox).show().on('click', function() {
						_Crud.search.clearSearchResults(el);
						$('.clearSearchIcon').hide().off('click');
						search.focus();
						search.val('');
					});

					_Crud.search.doSearch(searchString, el, type, function(e, node) {
						e.preventDefault();
						if (typeof callbackOverride === "function") {
							callbackOverride(node);
						} else {
							_Crud.objectList.addRelatedObject(parentType, id, key, node);
						}
						return false;
					});

				} else if (e.keyCode === 27) {

					if (searchString.trim() === '') {
						_Dialogs.custom.clickDialogCancelButton();
					}

					_Crud.search.clearSearchResults(el);
					$('.clearSearchIcon').hide().off('click');
					search.focus();
					search.val('');
				}

				return false;
			});

			// display initial result list
			_Crud.search.doSearch('*', el, type, (e, node) => {
				e.preventDefault();
				if (typeof callbackOverride === "function") {
					callbackOverride(node);
				} else {
					_Crud.objectList.addRelatedObject(parentType, id, key, node, () => {});
				}
				return false;
			}, 100);
		},
	},
	helpers: {
		getDisplayName: (node) => {
			let displayName;
			if (node.isContent && node.content && !node.name) {
				displayName = _Helpers.escapeTags(node.content.substring(0, 100));
			} else {
				displayName = node.name || node.id || node;
			}
			return displayName;
		},
		getCSSSelectorForKey: (key) => `[data-key="${key}"]`,
		// TODO: _Schema.getTypeInfo is pretty similar... merge and make global so that schema information is always present and loaded at the beginning (and only ever re-requested if the schema changes)
		ensureTypeInfoIsLoaded: (type, successCallback, failureCallback) => {

			let url = `${Structr.rootUrl}_schema/${type}`;

			fetch(url).then(async response => {

				let data = await response.json();

				if (response.ok) {

					let typeInfo = data?.result?.[0];

					if (typeInfo) {

						if (typeInfo.isRel === true || typeInfo.isServiceClass === false) {

							_Crud.types[type] = typeInfo;

							successCallback?.();

						} else {

							new ErrorMessage().text(`Unable to show data for service class '${type}'`).delayDuration(5000).show();
							_Crud.helpers.delayedMessage.showMessageAfterDelay(`<span class="mr-1">Unable to show data for service class </span><b>${type}</b>. Please select any other type.`, 500);
						}

					} else {

						new ErrorMessage().text(`No type information found for type '${type}'`).delayDuration(5000).show();
						_Crud.helpers.delayedMessage.showMessageAfterDelay(`<span class="mr-1">No type information found for type </span><b>${type}</b>. Please select any other type.`, 500);
					}

				} else {

					Structr.errorFromResponse(data, url);

					failureCallback?.();
				}
			})
		},
		ensurePropertiesForTypeAreLoaded: (type, successCallback, failureCallback) => {

			if (type === null) {
				return;
			}

			let properties = _Crud.types[type]?.views?.all;

			if (properties) {

				successCallback?.();

			} else {

				_Crud.helpers.ensureTypeInfoIsLoaded(type, () => {

					properties = _Crud.types[type]?.views?.all ?? [];

					if (Object.keys(properties).length === 0) {

						new WarningMessage().text(`Unable to find schema information for type '${type}'. There might be database nodes with no type information or a type unknown to Structr in the database.`).show();
						_Crud.helpers.delayedMessage.showMessageAfterDelay(`Unable to find schema information for type '${type}'.<br>There might be database nodes with no type information or a type unknown to Structr in the database.`, 500);

					} else {

						successCallback?.();
					}

				}, failureCallback);
			}
		},
		getPropertiesForTypeAndCurrentView: (type) => {

			let properties = _Crud.types[type].views.all;
			let currentView = _Crud.view[type];

			if (currentView !== 'all') {

				properties = _Crud.types[type].views[currentView] ?? properties;
			}

			return properties;
		},
		getFormat: (type, key) => {
			return _Crud.types[type].views.all[key].format;
		},
		isRelType: (type) => {
			return (_Crud.types[type]?.isRel === true);
		},
		/**
		 * Return true if the combination of the given property key
		 * and the given type is a collection
		 */
		isCollection: (key, type) => {
			return (key && type && _Crud.types[type]?.views.all[key]?.isCollection === true);
		},
		isBaseProperty: (key, type) => {
			return ('base' === _Crud.types[type]?.views.all[key]?.jsonName && 'GraphObject' === _Crud.types[type]?.views.all[key]?.declaringClass);
		},
		isHiddenProperty: (key, type) => {
			return ('hidden' === _Crud.types[type]?.views.all[key]?.jsonName && 'NodeInterface' === _Crud.types[type]?.views.all[key]?.declaringClass);
		},
		isFunctionProperty: (key, type) => {
			return ('org.structr.core.property.FunctionProperty' === _Crud.types[type]?.views.all[key]?.className);
		},
		isCypherProperty: (key, type) => {
			return ('org.structr.core.property.CypherQueryProperty' === _Crud.types[type]?.views.all[key]?.className);
		},
		isEntityIdProperty: (key, type) => {
			return ('org.structr.core.property.EntityIdProperty' === _Crud.types[type]?.views.all[key]?.className);
		},
		isMethodProperty: (key, type) => {
			return ('org.structr.web.property.MethodProperty' === _Crud.types[type]?.views.all[key]?.className);
		},
		isCollectionIdProperty: (key, type) => {
			return ('org.structr.core.property.CollectionIdProperty' === _Crud.types[type]?.views.all[key]?.className);
		},
		isCollectionNotionProperty: (key, type) => {
			return ('org.structr.core.property.CollectionNotionProperty' === _Crud.types[type]?.views.all[key]?.className);
		},
		/**
		 * Return true if the combination of the given property key
		 * and the given type is an Enum
		 */
		isEnum: (key, type) => {
			return (key && type && _Crud.types[type]?.views.all[key]?.className === 'org.structr.core.property.EnumProperty');
		},
		/**
		 * Return true if the combination of the given property key
		 * and the given type is a read-only property
		 */
		isReadOnly: (key, type) => {
			return (key && type && (_Crud.types[type]?.views.all[key]?.readOnly === true || _Crud.helpers.isCypherProperty(key, type)));
		},
		isSerializationDisabled: (type, key) => {
			return (key && type && _Crud.types[type]?.views.all[key]?.serializationDisabled === true);
		},
		/**
		 * Return the related type of the given property key of the given type (or a comma-separated list of possible related types)
		 */
		getRelatedTypeForAttribute: (key, type) => {

			if (key && type && _Crud.types[type] && _Crud.types[type].views.all[key]) {

				let info;

				let isRelType = _Crud.helpers.isRelType(type);
				if (!isRelType) {

					info = _Crud.types[type].views.all[key].relatedType;

				} else {

					// special handling for relationship types where we want to display the start and end node
					if (key === 'sourceId' || key === 'sourceNode') {

						info = _Crud.types[type].relInfo.possibleSourceTypes;

					} else if (key === 'targetId' || key === 'targetNode') {

						info = _Crud.types[type].relInfo.possibleTargetTypes;
					}
				}

				return info;
			}
		},
		extractIds: (result) => {

			return result.map(obj => {
				// value can be an ID string or an object
				if (typeof obj === 'object') {
					return { id: obj.id };
				} else {
					return obj;
				}
			});
		},
		getSortAndPagingParameters: (type, sort, order, pageSize, page, exactType = false) => {

			let paramsArray = [];

			if (sort) {
				paramsArray.push(Structr.getRequestParameterName('sort') + '=' + sort);
			}
			if (order) {
				paramsArray.push(Structr.getRequestParameterName('order') + '=' + order);
			}
			if (pageSize) {
				paramsArray.push(Structr.getRequestParameterName('pageSize') + '=' + pageSize);
			}
			if (page) {
				paramsArray.push(Structr.getRequestParameterName('page') + '=' + page);
			}
			if (exactType === true) {
				paramsArray.push('type=' + type);
			}

			return '?' + paramsArray.join('&');
		},
		getDataFromForm: (elem) => {

			let returnObject  = {};

			let allCells = elem.querySelectorAll('[data-key]');

			for (let cell of allCells) {

				let key          = cell.dataset['key'];
				let isCollection = (cell.dataset['isCollection'] === 'true');

				if (isCollection) {
					returnObject[key] = [];
				}

				let namedElements = cell.querySelectorAll(`[name="${key}"]`);

				for (let i of namedElements) {

					let isNew     = (i.dataset['isNew'] === 'true');
					let isChanged = (i.dataset['changed'] === 'true');

					if (!isNew || isChanged) {

						let val = _Entities.generalTab.getValueFromFormElement(i);


						if (isCollection) {

							returnObject[key].push(val);

						} else {

							returnObject[key] = val;
						}
					}
				}
			}

			// filter our empty strings and empty arrays
			let filteredData = Object.fromEntries(Object.entries(returnObject).filter(([key, value]) => (value !== '' && !(Array.isArray(value) && value.length === 0))));

			return filteredData;
		},
		crudAskDelete: async (type, id) => {

			let confirm = await _Dialogs.confirmation.showPromise(`Are you sure you want to delete <b>${type}</b> ${id}?`);
			if (confirm === true) {
				_Crud.helpers.crudDelete(type, id);
			}

			return confirm;
		},
		crudDelete: (type, id) => {

			let url = `${Structr.rootUrl}${type}/${id}`;

			fetch(url, {
				method: 'DELETE'
			}).then(async response => {

				let data = await response.json();

				if (response.ok) {

					let row = _Crud.objectList.getRow(id);
					_Helpers.fastRemoveElement(row[0]);

				} else {
					Structr.errorFromResponse(data, url, { statusCode: response.status, requiresConfirmation: true });
				}
			});
		},
		delayedMessage: {
			messageTimeout: undefined,
			showLoadingMessageAfterDelay: (message, delay) => {

				_Crud.helpers.delayedMessage.showMessageAfterDelay(`${_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 24, 24, 'mr-2')}<span>${message} - please stand by</span>`, delay);
			},
			showMessageAfterDelay: (message, delay) => {

				clearTimeout(_Crud.helpers.delayedMessage.messageTimeout);

				_Crud.helpers.delayedMessage.messageTimeout = window.setTimeout(() => {

					_Crud.helpers.delayedMessage.removeMessage();

					let crudRight = $('#crud-type-detail');
					crudRight.append(`
						<div class="crud-message">
							<div class="crud-centered flex items-center justify-center">${message}</div>
						</div>
					`);

				}, delay);
			},
			removeMessage: () => {

				clearTimeout(_Crud.helpers.delayedMessage.messageTimeout);
				_Helpers.fastRemoveElement(document.querySelector('#crud-type-detail .crud-message'));
			},
		},
		getDateTimeIconHTML: () => {
			return _Icons.getSvgIcon(_Icons.iconDatetime, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-lightgray', 'icon-crud-datetime', 'pointer-events-none']));
		}
	},	
	creationDialogWithErrorHandling: {
		initializeForEvent: (e, type, initialData = {}, onSuccess) => {

			if (e.shiftKey === true) {

				_Crud.creationDialogWithErrorHandling.loadTypeInfoAndShowRequestedCreateDialog(type, initialData, onSuccess);

			} else {

				_Crud.creationDialogWithErrorHandling.tryCreate(type, initialData, onSuccess);
			}
		},
		tryCreate: (type, nodeData = {}, onSuccess) => {

			let url = Structr.rootUrl + type;

			fetch(url, {
				method: 'POST',
				body: JSON.stringify(nodeData)
			}).then(async response => {

				let responseData = await response.json();

				if (response.ok) {

					onSuccess?.(type, responseData.result[0]);

					if (_Dialogs.custom.isDialogOpen()) {
						_Dialogs.custom.getCloseDialogButton().click();
					}

				} else {

					if (response.status !== 422 || _Dialogs.custom.isDialogOpen()) {
						Structr.errorFromResponse(responseData, url, { statusCode: response.status, delayDuration: 5000 });
					}

					_Dialogs.custom.enableSaveButton();

					_Crud.helpers.ensureTypeInfoIsLoaded(type, () => {

						_Crud.creationDialogWithErrorHandling.showCreateError(type, nodeData, responseData, onSuccess);
					});
				}
			});
		},
		showCreateError: (type, nodeData, responseData, onSuccess) => {

			let dialogText = _Dialogs.custom.getDialogTextElement();

			let errors = responseData?.errors ?? [];

			if (!_Dialogs.custom.isDialogOpen()) {
				let elements = _Crud.creationDialogWithErrorHandling.showCreateDialog(type, nodeData, onSuccess, errors);
				dialogText = elements.dialogText;
			}

			// remove "invalid" highlight from elements
			for (let el of dialogText.querySelectorAll('.props input, .props textarea, .props td.value')) {
				el.classList.remove('form-input', 'input-invalid');
			}

			// delay only used to further highlight the input elements (slight blink)
			window.setTimeout(() => {

				for (let error of errors) {

					let key      = error.property;
					let errorMsg = error.token;

					let cellsForKeyWithError = dialogText.querySelectorAll(`td [name="${key}"]`);
					if (cellsForKeyWithError.length > 0) {

						let errorText = `"${key}" ${errorMsg.replace(/_/gi, ' ')}`;

						if (error.detail) {
							errorText += ` ${error.detail}`;
						}

						_Dialogs.custom.showAndHideInfoBoxMessage(errorText, 'error', 4000, 1000);

						// add "invalid" highlight from elements
						for (let input of cellsForKeyWithError) {
							input.classList.add('form-input', 'input-invalid');
						}

						cellsForKeyWithError[0].focus();
					}
				}
			}, 100);
		},
		loadTypeInfoAndShowRequestedCreateDialog: (type, initialData = {}, onSuccess) => {

			_Crud.helpers.ensureTypeInfoIsLoaded(type, () => {

				let dialog = _Crud.creationDialogWithErrorHandling.showCreateDialog(type, initialData, onSuccess);

				// when displaying the on-demand create dialog, focus the first element
				let firstInputOrTextarea = dialog.dialogText.querySelector('input, textarea');
				firstInputOrTextarea.focus();
			});
		},
		showCreateDialog: (type, initialData = {}, onSuccess, errors = []) => {

			if (!type) {
				Structr.error('Missing type');
				return;
			}

			let dialog = _Dialogs.custom.openDialog(`Create ${type}`);
			_Dialogs.custom.noConfirmOnEscape();

			dialog.dialogText.insertAdjacentHTML('beforeend', '<form id="entityForm"><table class="props"><tr><th>Property Name</th><th>Value</th></tr>');

			document.querySelector('#entityForm').addEventListener('submit', e => e.preventDefault());

			let table = dialog.dialogText.querySelector('table');

			let isRelType = _Crud.helpers.isRelType(type);

			let keys = Object.keys(_Crud.types[type].views.all).sort();

			// 1. always display name first
			let bucket1 = keys.filter(k => k === 'name').sort();
			keys = keys.filter(k => !bucket1.includes(k));

			// 2. show pre-filled attributes second
			let bucket2 = keys.filter(k => !!initialData[k]).sort();
			keys = keys.filter(k => !bucket2.includes(k));

			// 3. show attributes with errors third
			let bucket3 = keys.filter(k => errors.some(e => e.property === k)).sort();
			keys = keys.filter(k => !bucket3.includes(k));

			let priorityBuckets = [bucket1, bucket2, bucket3, keys];

			for (let bucket of priorityBuckets) {

				for (let key of bucket) {

					let isBuiltinBaseProperty              = _Crud.helpers.isBaseProperty(key, type);
					let isBuiltinHiddenProperty            = _Crud.helpers.isHiddenProperty(key, type);
					let readOnly                           = _Crud.helpers.isReadOnly(key, type);
					let isEntityIdProperty                 = _Crud.helpers.isEntityIdProperty(key, type);
					let isCollectionIdProperty             = _Crud.helpers.isCollectionIdProperty(key, type);
					let isCollectionNotionProperty         = _Crud.helpers.isCollectionNotionProperty(key, type);
					let isSourceOrTargetNode               = isRelType && (key === Structr.internalKeys.sourceNode || key === Structr.internalKeys.targetNode);
					let isInternalTimestamp                = isRelType && (key === Structr.internalKeys.internalTimestamp);
					let isVisibilityFlagOnRelationship     = isRelType && (key === Structr.internalKeys.visibleToPublicUsers || key === Structr.internalKeys.visibleToAuthenticatedUsers);

					if (!readOnly && !isBuiltinHiddenProperty && !isBuiltinBaseProperty && !isInternalTimestamp && !isVisibilityFlagOnRelationship && !isSourceOrTargetNode && !isEntityIdProperty && !isCollectionIdProperty && !isCollectionNotionProperty) {

						let row = _Helpers.createSingleDOMElementFromHTML(`
							<tr>
								<td class="key"><label for="${key}">${key}</label></td>
								<td class="__value"></td>
							</tr>
						`);
						table.appendChild(row);

						let cell = $(row.querySelector(`.__value`));

						_Crud.objectList.populateCell(null, key, type, initialData[key], cell);
					}
				}
			}

			let dialogSaveButton = _Dialogs.custom.updateOrCreateDialogSaveButton('Create');
			_Helpers.enableElement(dialogSaveButton);

			dialogSaveButton.addEventListener('click', () => {

				_Helpers.disableElement(dialogSaveButton);
				let nodeData = _Crud.helpers.getDataFromForm(document.querySelector('#entityForm'));
				_Crud.creationDialogWithErrorHandling.tryCreate(type, nodeData, onSuccess);
			});

			return dialog;
		},
		crudCreateSuccess: async (type, newNodeId) => {

			let properties = _Crud.helpers.getPropertiesForTypeAndCurrentView(type);

			let newNodeResponse = await fetch(`${Structr.rootUrl}${newNodeId}/all`, {
				headers: _Helpers.getHeadersForCustomView(_Crud.objectList.filterKeys(type, Object.keys(properties)))
			});

			if (newNodeResponse.ok) {

				let newNodeResult = await newNodeResponse.json();
				let newNode       = newNodeResult.result;
				_Crud.objectList.appendRow(type, properties, newNode);

				_Helpers.blinkGreen(_Crud.objectList.getRow(newNode.id));

			} else {

				_Crud.objectList.refreshList(type);
			}
		},
	},
	crudCache: new AsyncObjectCache(async (obj) => {

		let properties = ['id', 'name', 'type', 'contentType', 'isThumbnail', 'isImage', 'tnSmall', 'tnMid'];

		let response = await fetch(Structr.rootUrl + (obj.type ? obj.type + '/' : '') + obj.id + '/' + _Crud.defaultView, {
			headers: _Helpers.getHeadersForCustomView(properties)
		});

		if (response.ok) {

			let data = await response.json();

			if (data && data.result) {
				let node = data.result;
				_Crud.crudCache.addObject(node, node.id);
			}
		}
	}),
	crudListFetchAbortMechanism: {
		abortController: undefined,
		lastType: null,
		init: (type) => {
			_Crud.crudListFetchAbortMechanism.lastType        = type;
			_Crud.crudListFetchAbortMechanism.abortController = new AbortController();
		},
		abortListFetch: (type) => {

			if (_Crud.crudListFetchAbortMechanism.abortController) {

				_Crud.crudListFetchAbortMechanism.abortController.signal.onabort = () => {
					_Crud.crudListFetchAbortMechanism.init(type);
				};
				_Crud.crudListFetchAbortMechanism.abortController.abort(`Loading of "${type}" aborted loading of "${_Crud.crudListFetchAbortMechanism.lastType}"`);

			} else {

				_Crud.crudListFetchAbortMechanism.init(type);
			}
		}
	},
	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/crud.css">

			<div id="crud-main">

				<div class="column-resizer"></div>

				<div id="crud-left" class="resourceBox gap-y-12 pt-4 flex flex-col mr-8">

					<div id="crud-types" class="h-1/2 flex flex-col">
					
						<div class="flex">
							<h2 class="flex-grow">Types</h2>
							${_Crud.typeList.filtering.templates.filterBox(config)}
						</div>
						
						${_Crud.typeList.filtering.templates.filterInput(config)}

						<div id="crud-types-list" class="flex-grow"></div>
					</div>

					<div id="crud-recent-types" class="h-1/2 flex flex-col">
						<h2>Recent</h2>
						<div id="crud-recent-types-list" class="flex-grow"></div>
					</div>

				</div>

				<div id="crud-type-detail" class="resourceBox"></div>

			</div>
		`,
		functions: config => `
			<div id="crud-buttons" class="flex-grow"></div>

			<div class="searchBox mr-6">
				<input id="crud-search-box" class="search" name="crud-search" placeholder="Search">
				${_Icons.getSvgIcon(_Icons.iconCrossIcon, 12, 12, _Icons.getSvgIconClassesForColoredIcon(['clearSearchIcon', 'icon-lightgrey', 'cursor-pointer']), 'Clear Search')}
			</div>
		`,
		typeButtons: config => `
			<div id="crud-buttons" class="flex items-center">
				<button class="action inline-flex items-center" id="create${config.type}" title="Create node of type ${config.type}. Hold the shift key to open the 'Create Node' dialog.">
					${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['mr-2'])} Create ${config.type}
				</button>
				<button id="export${config.type}" class="flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
					${_Icons.getSvgIcon(_Icons.iconExportAsCSV, 16, 16, ['mr-2', 'icon-grey'])} Export as CSV
				</button>
				<button id="import${config.type}" class="flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
					${_Icons.getSvgIcon(_Icons.iconImportFromCSV, 16, 16, ['mr-2', 'icon-grey'])} Import CSV
				</button>
				<button id="delete${config.type}" class="flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
					${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, ['mr-2', 'icon-red'])} <span>Delete <b>all</b> objects of this type</span>
				</button>
				<label for="exact_type_${config.type}" class="exact-type-checkbox-label flex items-center">
					<input id="exact_type_${config.type}" class="exact-type-checkbox" type="checkbox"> Exclude inheriting types
				</label>
			</div>
		`,
		configureColumns: config => `
			<div>
				<h3>Configure and sort columns here</h3>

				<style>
					/* inline style to prevent style from leaking */
					.select2-selection__choice[title=id], .select2-selection__choice[title=type] {
						border: 1px solid var(--input-field-border);
						color: #666;
					}

					.select2-selection__choice[title=id] span.select2-selection__choice__remove,
					.select2-selection__choice[title=type] span.select2-selection__choice__remove {
						display: none;
					}
				</style>

				<p>This sets the global sort order for this type - on all views. Depending on the current view, you may see properties here, which are not contained in the view.</p>

				<div class="mb-4">
					<div>
						<label class="font-semibold">Columns</label>
					</div>
					<div id="view-properties">
						<select id="columns-select" class="view" multiple="multiple"></select>
					</div>
				</div>
			</div>
		`
	}
};
