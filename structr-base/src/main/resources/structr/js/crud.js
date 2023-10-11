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
document.addEventListener("DOMContentLoaded", () => {
	Structr.registerModule(_Crud);
});

let _Crud = {
	_moduleName: 'crud',
	displayTypeConfigKey: 'structrCrudDisplayTypes_' + location.port,
	defaultCollectionPageSize: 10,
	resultCountSoftLimit: 10000,
	defaultType: 'Page',
	defaultView: 'all',
	defaultSort: 'createdDate',
	defaultOrder: 'desc',
	defaultPage: 1,
	defaultPageSize: 10,
	crudPagerDataKey: 'structrCrudPagerData_' + location.port + '_',
	crudTypeKey: 'structrCrudType_' + location.port,
	crudHiddenColumnsKey: 'structrCrudHiddenColumns_' + location.port,
	crudSortedColumnsKey: 'structrCrudSortedColumns_' + location.port,
	crudRecentTypesKey: 'structrCrudRecentTypes_' + location.port,
	crudResizerLeftKey: 'structrCrudResizerLeft_' + location.port,
	crudExactTypeKey: 'structrCrudExactType_' + location.port,
	searchField: undefined,
	searchFieldClearIcon: undefined,
	types: {},
	typeColumnSort: {},
	abstractSchemaNodes: {},
	availableViews: {},
	relInfo: {},
	keys: {},
	hiddenKeysForAllTypes: [
		'base',
		'createdBy',
		'lastModifiedBy',
		'ownerId',
		'hidden',
		'internalEntityContextPath',
		'grantees'
	],
	hiddenKeysForFileTypes: [
		'base64Data',
		'favoriteContent',
		'favoriteContext',
		'favoriteUsers',
		'relationshipId',
		'resultDocumentForExporter',
		'documentTemplateForExporter',
		'isFile',
		'position',
		'extractedContent',
		'indexedWords',
		'fileModificationDate',
		'nextSiblingId'
	],
	hiddenKeysForImageTypes: [
		'base64Data',
		'imageData',
		'favoriteContent',
		'favoriteContext',
		'favoriteUsers',
		'resultDocumentForExporter',
		'documentTemplateForExporter',
		'isFile',
		'position',
		'extractedContent',
		'indexedWords',
		'fileModificationDate'
	],
	hiddenKeysForPrincipalTypes: [
		'isUser',
		'isAdmin',
		'createdBy',
		'sessionIds',
		'publicKeys',
		'sessionData',
		'password',
		'passwordChangeDate',
		'salt',
		'twoFactorSecret',
		'twoFactorToken',
		'isTwoFactorUser',
		'twoFactorConfirmed',
		'ownedNodes',
		'localStorage',
		'customPermissionQueryAccessControl',
		'customPermissionQueryDelete',
		'customPermissionQueryRead',
		'customPermissionQueryWrite'
	],
	crudCache: new AsyncObjectCache(async (obj) => {

		let response = await fetch(Structr.rootUrl + (obj.type ? obj.type + '/' : '') + obj.id + '/' + _Crud.defaultView, {
			headers: {
				Accept: 'application/json; charset=utf-8; properties=id,name,type,contentType,isThumbnail,isImage,tnSmall,tnMid'
			}
		})

		if (response.ok) {

			let data = await response.json();

			if (data && data.result) {
				let node = data.result;
				_Crud.crudCache.addObject(node, node.id);
			}
		}

	}),
	getTypeInfo: (type, callback) => {

		let url = `${Structr.rootUrl}_schema/${type}`;

		fetch(url).then(async response => {

			let data = await response.json();

			if (response.ok) {

				if (data && data.result && data.result[0]) {

					_Crud.availableViews[type] = data.result[0].views;

					let properties = {};

					let processViewInfo = (view) => {
						if (view) {
							for (let key in view) {
								let prop = view[key];
								properties[prop.jsonName] = prop;
							}
						}
					};

					processViewInfo(data.result[0].views.public);
					processViewInfo(data.result[0].views.custom);
					processViewInfo(data.result[0].views.all);

					_Crud.keys[type] = properties;

					if (typeof callback === 'function') {
						callback();
					}

				} else {

					new ErrorMessage().text(`No type information found for type: ${type}`).delayDuration(5000).show();
					_Crud.showMessageAfterDelay(`<span class="mr-1">No type information found for type: </span><b>${type}</b>`, 500);
				}

			} else {
				Structr.errorFromResponse(data, url);
			}
		})
	},
	getProperties: (type, callback) => {

		if (type === null) {
			return;
		}

		if (_Crud.keys[type]) {

			callback?.();

		} else {

			_Crud.getTypeInfo(type, () => {

				let properties = _Crud.keys[type];

				if (Object.keys(properties).length === 0) {

					new WarningMessage().text(`Unable to find schema information for type '${type}'. There might be database nodes with no type information or a type unknown to Structr in the database.`).show();
					_Crud.showMessageAfterDelay(`Unable to find schema information for type '${type}'.<br>There might be database nodes with no type information or a type unknown to Structr in the database.`, 500);

				} else {

					callback?.();
				}
			});
		}
	},
	type: null,
	pageCount: null,
	view: {},
	sort: {},
	order: {},
	page: {},
	exact: {},
	pageSize: {},
	prevAnimFrameReqId_moveResizer: undefined,
	moveResizer: (left) => {

		_Helpers.requestAnimationFrameWrapper(_Crud.prevAnimFrameReqId_moveResizer, () => {
			left = left || LSWrapper.getItem(_Crud.crudResizerLeftKey) || 210;
			Structr.mainContainer.querySelector('.column-resizer').style.left = left + 'px';

			document.getElementById('crud-types').style.width        = left - 12 + 'px';
			document.getElementById('crud-recent-types').style.width = left - 12 + 'px';
		});
	},
	reloadList: () => {
		_Crud.typeSelected(_Crud.type);
	},
	init: () => {

		Structr.setMainContainerHTML(_Crud.templates.main());
		Structr.setFunctionBarHTML(_Crud.templates.functions());

		// UISettings.showSettingsForCurrentModule();

		_Crud.moveResizer();

		Structr.initVerticalSlider(Structr.mainContainer.querySelector('.column-resizer'), _Crud.crudResizerLeftKey, 204, _Crud.moveResizer);

		document.querySelector('#crud-left').addEventListener('click', (e) => {
			let type = e.target.closest('.crud-type');
			if (type) {
				_Crud.typeSelected(type.dataset['type']);
			}
		});

		document.querySelector('#crud-recent-types').addEventListener('click', (e) => {
			let removeRecentType = e.target.closest('.remove-recent-type');
			if (removeRecentType) {
				e.stopPropagation();
				let type = removeRecentType.closest('div[data-type]');
				_Crud.removeRecentType(type);
			}
		});

		for (let typeFilterCheckbox of document.querySelectorAll('#crudTypeFilterSettings input')) {
			typeFilterCheckbox.addEventListener('change', () => {
				LSWrapper.setItem(_Crud.displayTypeConfigKey, _Crud.getTypeVisibilityConfig());
				_Crud.updateTypeList();
			})
		}

		document.querySelector('#crudTypesSearch').addEventListener('keyup', (e) => {

			if (e.keyCode === 27) {

				e.target.value = '';

			} else if (e.keyCode === 13) {

				let visibleTypes = document.querySelectorAll('#crud-types-list .crud-type:not(.hidden)');

				if (visibleTypes.length === 1) {

					_Crud.typeSelected(visibleTypes[0].dataset['type']);

				} else {

					let filterVal     = e.target.value.toLowerCase();
					let matchingTypes = Object.keys(_Crud.types).filter(type => type.toLowerCase() === filterVal);

					if (matchingTypes.length === 1) {
						_Crud.typeSelected(matchingTypes[0]);
					}
				}
			}

			_Crud.filterTypes(e.target.value.toLowerCase());
		});

		_Crud.exact         = LSWrapper.getItem(_Crud.crudExactTypeKey) || {};
		_Crud.schemaLoading = false;
		_Crud.schemaLoaded  = false;
		_Crud.keys          = {};

		_Crud.loadSchema().then(() => {

			Command.query('AbstractSchemaNode', 2000, 1, 'name', 'asc', {}, (abstractSchemaNodes) => {

				for (let asn of abstractSchemaNodes) {
					_Crud.abstractSchemaNodes[asn.name] = asn;
				}

				_Crud.updateTypeList();
				_Crud.typeSelected(_Crud.type);
				_Crud.updateRecentTypeList(_Crud.type);

				Structr.resize();
				Structr.mainMenu.unblock();
			});
		});

		_Crud.searchField          = document.getElementById('crud-search-box');
		_Crud.searchFieldClearIcon = document.querySelector('.clearSearchIcon');
		_Crud.searchField.focus();

		_Helpers.appendInfoTextToElement({
			element: _Crud.searchField,
			text: 'By default a fuzzy search is performed on the <code>name</code> attribute of <b>every</b> node type. Optionally, you can specify a type and an attribute to search as follows:<br><br>User.name:admin<br><br>If a UUID-string is supplied, the search is performed on the base type AbstractNode to yield the fastest results.',
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

		let crudMain = $('#crud-main');

		_Crud.searchFieldClearIcon.addEventListener('click', (e) => {
			_Crud.clearMainSearch(crudMain);
			_Crud.searchField.focus();
		});

		_Crud.searchField.addEventListener('keyup', (e) => {

			let searchString = _Crud.searchField.value;

			if (searchString && searchString.length) {
				_Crud.searchFieldClearIcon.style.display = 'block';
			}

			if (searchString && searchString.length && e.keyCode === 13) {

				_Crud.search(searchString, crudMain, null, function(e, node) {
					e.preventDefault();
					_Entities.showProperties(node, 'ui');
					return false;
				});

				$('#crud-type-detail').hide();

			} else if (e.keyCode === 27 || searchString === '') {

				_Crud.clearMainSearch(crudMain);
			}
		});
	},
	onload: () => {

		Structr.updateMainHelpLink(_Helpers.getDocumentationURLForTopic('crud'));

		if (!_Crud.type) {
			_Crud.restoreType();
		}

		if (!_Crud.type) {
			_Crud.type = _Helpers.urlParam('type');
		}

		if (!_Crud.type) {
			_Crud.type = _Crud.defaultType;
		}

		_Crud.init();
	},
	messageTimeout: undefined,
	showLoadingMessageAfterDelay: (message, delay) => {

		_Crud.showMessageAfterDelay(`${_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 24, 24, 'mr-2')}<span>${message} - please stand by</span>`, delay);
	},
	showMessageAfterDelay: (message, delay) => {

		clearTimeout(_Crud.messageTimeout);

		_Crud.messageTimeout = window.setTimeout(() => {

			_Crud.removeMessage();

			let crudRight = $('#crud-type-detail');
			crudRight.append(`
				<div class="crud-message">
					<div class="crud-centered flex items-center justify-center">${message}</div>
				</div>
			`);

		}, delay);
	},
	removeMessage: () => {

		clearTimeout(_Crud.messageTimeout);
		_Helpers.fastRemoveElement(document.querySelector('#crud-type-detail .crud-message'));
	},
	typeSelected: (type) => {

		_Crud.updateRecentTypeList(type);
		_Crud.highlightCurrentType(type);

		let crudRight = document.querySelector('#crud-type-detail');

		_Crud.showLoadingMessageAfterDelay(`Loading schema information for type <b>${type}</b>`, 500);

		_Crud.getProperties(type, () => {

			_Crud.removeMessage();

			_Helpers.fastRemoveAllChildren(crudRight);

			_Helpers.setContainerHTML(Structr.functionBar.querySelector('#crud-buttons'), _Crud.templates.typeButtons({ type: type }));

			_Crud.determinePagerData(type);

			let pagerNode = _Crud.addPager(type, crudRight);

			crudRight.insertAdjacentHTML('beforeend', '<table class="crud-table"><thead><tr></tr></thead><tbody></tbody></table><div id="query-info">Query: <span class="queryTime"></span> s &nbsp; Serialization: <span class="serTime"></span> s</div>');

			_Crud.updateCrudTableHeader(type);

			document.querySelector('#create' + type).addEventListener('click', () => {
				_Crud.crudCreate(type);
			});

			document.querySelector('#export' + type).addEventListener('click', () => {
				_Crud.crudExport(type);
			});

			document.querySelector('#import' + type).addEventListener('click', () => {
				_Crud.crudImport(type);
			});

			let exactTypeCheckbox = document.querySelector('#exact_type_' + type);
			exactTypeCheckbox.checked = _Crud.exact[type];

			exactTypeCheckbox.addEventListener('change', () => {
				_Crud.exact[type] = exactTypeCheckbox.checked;
				LSWrapper.setItem(_Crud.crudExactTypeKey, _Crud.exact);
				_Crud.refreshList(type);
			});

			document.querySelector('#delete' + type).addEventListener('click', async () => {

				let confirm = await _Dialogs.confirmation.showPromise(`
					<h3>WARNING: Really delete all objects of type '${type}'${((exactTypeCheckbox.checked === true) ? '' : ' and of inheriting types')}?</h3>
					<p>This will delete all objects of the type (<b>${((exactTypeCheckbox.checked === true) ? 'excluding' : 'including')}</b> all objects of inheriting types).</p>
					<p>Depending on the amount of objects this can take a while.</p>
				`);

				if (confirm === true) {
					await _Crud.deleteAllNodesOfType(type, exactTypeCheckbox.checked);
				}
			});

			_Crud.deActivatePagerElements(pagerNode);
			_Crud.activateList(type);
			_Crud.activatePagerElements(type, pagerNode);
		});
	},
	getCurrentProperties: (type) => {

		let properties = _Crud.availableViews[type].all;

		if (_Crud.view[type] !== 'all') {
			let viewDef = _Crud.availableViews[type][_Crud.view[type]];

			if (viewDef) {
				properties = viewDef;
			}
		}

		return properties;
	},
	updateCrudTableHeader: (type) => {

		let properties     = _Crud.getCurrentProperties(type);
		let tableHeaderRow = document.querySelector('#crud-type-detail table thead tr');

		_Helpers.fastRemoveAllChildren(tableHeaderRow);

		let newHeaderHTML = `
			<th class="___action_header" data-key="action_header">Actions</th>
			${_Crud.filterKeys(type, Object.keys(properties)).map(key => `<th class="${_Crud.cssClassForKey(key)}" data-key="${key}">${key}</th>`).join('')}
		`;

		tableHeaderRow.insertAdjacentHTML('beforeend', newHeaderHTML);
	},
	updateTypeList: () => {

		let typesList      = document.querySelector('#crud-types-list');
		let typeVisibility = _Crud.getStoredTypeVisibilityConfig();

		let typesToShow    = Object.keys(_Crud.types).sort().filter(typeName => {

			let schemaNode    = _Crud.abstractSchemaNodes[typeName];
			let type          = _Crud.types[typeName];

			let isRelType       = type.isRel === true;
			let isBuiltInRel    = isRelType && (schemaNode === undefined || ((schemaNode?.isPartOfBuiltInSchema ?? false) || (schemaNode?.isBuiltinType ?? false)));
			let isCustomRelType = isRelType && !(schemaNode === undefined || ((schemaNode?.isPartOfBuiltInSchema ?? false) || (schemaNode?.isBuiltinType ?? false)));
			let isDynamicType   = !isRelType && (schemaNode && schemaNode.isBuiltinType === false);
			let isCoreType      = !isRelType && (schemaNode && schemaNode.isBuiltinType === true && schemaNode.category === 'core');
			let isHtmlType      = !isRelType && (schemaNode && schemaNode.isBuiltinType === true && schemaNode.category === 'html');
			let isUiType        = !isRelType && (schemaNode && schemaNode.isBuiltinType === true && schemaNode.category === 'ui');
			let isLogType       = !isRelType && type.className.startsWith('org.structr.rest.logging.entity');
			let isOtherType     = !(isRelType || isDynamicType || isCoreType || isHtmlType || isUiType || isLogType);

			let hide =  (!typeVisibility.rels && isBuiltInRel) || (!typeVisibility.customRels && isCustomRelType) || (!typeVisibility.custom && isDynamicType) || (!typeVisibility.core && isCoreType) ||
				(!typeVisibility.html && isHtmlType) || (!typeVisibility.ui && isUiType) || (!typeVisibility.log && isLogType) || (!typeVisibility.other && isOtherType);

			return !hide;
		});

		typesList.innerHTML = (typesToShow.length > 0) ? typesToShow.map(typeName => `<div class="crud-type truncate" data-type="${typeName}">${typeName}</div>`).join('') : '<div class="px-3">No types available. Use the above configuration dropdown to adjust the filter settings.</div>';

		_Crud.highlightCurrentType(_Crud.type);
		_Crud.filterTypes($('#crudTypesSearch').val().toLowerCase());
		Structr.resize();
	},
	getStoredTypeVisibilityConfig: (singleKey) => {

		let config = LSWrapper.getItem(_Crud.displayTypeConfigKey, {
			custom:     true,
			customRels: true,
			rels:       false,
			core:       false,
			html:       false,
			ui:         false,
			log:        false,
			other:      false
		});

		if (singleKey) {

			return config[singleKey];
		}

		return config;
	},
	getTypeVisibilityConfig: () => {

		return {
			custom:       $('#crudTypeToggleCustom').prop('checked'),
			customRels:   $('#crudTypeToggleCustomRels').prop('checked'),
			rels:         $('#crudTypeToggleRels').prop('checked'),
			core:         $('#crudTypeToggleCore').prop('checked'),
			html:         $('#crudTypeToggleHtml').prop('checked'),
			ui:           $('#crudTypeToggleUi').prop('checked'),
			log:          $('#crudTypeToggleLog').prop('checked'),
			other:        $('#crudTypeToggleOther').prop('checked')
		};
	},
	highlightCurrentType: (selectedType) => {

		$('#crud-left .crud-type').removeClass('active');
		$('#crud-left .crud-type[data-type="' + selectedType + '"]').addClass('active');

		let $crudTypesList             = $('#crud-types-list');
		let $selectedElementInTypeList = $('.crud-type[data-type="' + selectedType + '"]', $crudTypesList);

		if ($selectedElementInTypeList && $selectedElementInTypeList.length > 0) {
			let positionOfList    = $crudTypesList.position().top;
			let scrollTopOfList   = $crudTypesList.scrollTop();
			let positionOfElement = $selectedElementInTypeList.position().top;
			$crudTypesList.animate({scrollTop: positionOfElement + scrollTopOfList - positionOfList });
		} else {
			$crudTypesList.animate({scrollTop: 0});
		}

	},
	filterTypes: (filterVal) => {

		for (let el of document.querySelectorAll('#crud-types-list .crud-type')) {

			if (el.dataset['type'].toLowerCase().indexOf(filterVal) === -1) {
				el.classList.add('hidden');
			} else {
				el.classList.remove('hidden');
			}
		}
	},
	updateRecentTypeList: (selectedType) => {

		let recentTypes = LSWrapper.getItem(_Crud.crudRecentTypesKey);

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

		LSWrapper.setItem(_Crud.crudRecentTypesKey, recentTypes);
	},
	removeRecentType: (recentTypeElement) => {

		let typeToRemove = recentTypeElement.dataset['type'];
		let recentTypes  = LSWrapper.getItem(_Crud.crudRecentTypesKey);

		if (recentTypes) {
			recentTypes = recentTypes.filter((type) => (type !== typeToRemove));
		}

		LSWrapper.setItem(_Crud.crudRecentTypesKey, recentTypes);

		_Helpers.fastRemoveElement(recentTypeElement);
	},
	updateUrl: (type) => {

		if (type) {
			_Crud.type = type;
			_Crud.storeType();
			_Crud.storePagerData();
			_Crud.updateResourceLink(type);
		}

		_Crud.searchField.focus();
	},
	loadSchema: async () => {

		_Crud.showLoadingMessageAfterDelay('Loading data', 100);

		let processRelInfo = (relInfo) => {
			if (relInfo) {
				for (let r of relInfo) {
					_Crud.relInfo[r.type] = {
						source: r.possibleSourceTypes,
						target: r.possibleTargetTypes
					};
				}
			}
		};

		let response = await fetch(Structr.rootUrl + '_schema');

		if (response.ok) {

			let data = await response.json();

			for (let typeObj of data.result) {
				_Crud.types[typeObj.type] = typeObj;
				processRelInfo(typeObj.relatedTo);
				processRelInfo(typeObj.relatedFrom);
			}
		}
	},
	determinePagerData: (type) => {

		// Priority: JS vars -> Local Storage -> URL -> Default

		if (!_Crud.view[type]) {
			_Crud.view[type]     = _Helpers.urlParam('view');
			_Crud.sort[type]     = _Helpers.urlParam('sort');
			_Crud.order[type]    = _Helpers.urlParam('order');
			_Crud.pageSize[type] = _Helpers.urlParam('pageSize');
			_Crud.page[type]     = _Helpers.urlParam('page');
		}

		if (!_Crud.view[type]) {
			_Crud.restorePagerData();
		}

		if (!_Crud.view[type]) {
			_Crud.view[type]     = _Crud.defaultView;
			_Crud.sort[type]     = _Crud.defaultSort;
			_Crud.order[type]    = _Crud.defaultOrder;
			_Crud.pageSize[type] = _Crud.defaultPageSize;
			_Crud.page[type]     = _Crud.defaultPage;
		}
	},
	/**
	 * Return true if the combination of the given property key
	 * and the given type is a collection
	 */
	isCollection: (key, type) => {
		return (key && type && _Crud.keys[type] && _Crud.keys[type][key] && _Crud.keys[type][key].isCollection);
	},
	isFunctionProperty: (key, type) => {
		return ('org.structr.core.property.FunctionProperty' === _Crud.keys[type][key].className);
	},
	isCypherProperty: (key, type) => {
		return ('org.structr.core.property.CypherQueryProperty' === _Crud.keys[type][key].className);
	},
	/**
	 * Return true if the combination of the given property key
	 * and the given type is an Enum
	 */
	isEnum: (key, type) => {
		return (key && type && _Crud.keys[type] && _Crud.keys[type][key] && _Crud.keys[type][key].className === 'org.structr.core.property.EnumProperty');
	},
	/**
	 * Return true if the combination of the given property key
	 * and the given type is a read-only property
	 */
	readOnly: (key, type) => {
		return (key && type && _Crud.keys[type] && _Crud.keys[type][key] && (_Crud.keys[type][key].readOnly === true || _Crud.isCypherProperty(key, type)));
	},
	/**
	 * Return the related type of the given property key
	 * of the given type
	 */
	relatedType: (key, type) => {

		if (key && type && _Crud.keys[type] && _Crud.keys[type][key]) {

			let storedInfo = _Crud.keys[type][key].relatedType;

			if (!storedInfo) {

				let declaringClass = _Crud.keys[type][key].declaringClass;
				if (declaringClass && _Crud.relInfo[declaringClass]) {
					if (key === 'sourceId') {

						storedInfo = _Crud.relInfo[declaringClass].source;

					} else if (key === 'targetId') {

						storedInfo = _Crud.relInfo[declaringClass].target;
					}
				}

				// sourceNode and targetNode are defined on type AbstractRelationship, thus we use the info from sourceId and targetId
				if (key === 'sourceNode') {

					declaringClass     = _Crud.keys[type]?.sourceId?.declaringClass;
					storedInfo         = _Crud.relInfo[declaringClass]?.source;

				} else if (key === 'targetNode' && _Crud.keys[type].targetId) {

					declaringClass     = _Crud.keys[type]?.targetId?.declaringClass;
					storedInfo         = _Crud.relInfo[declaringClass]?.target;
				}
			}

			return storedInfo;
		}

		console.log(`Unknown relatedType for ${type}.${key}`);
	},
	getFormat: (key, type) => {
		return _Crud.keys[type][key].format;
	},
	addPager: (type, el) => {

		if (!_Crud.page[type]) {
			_Crud.page[type] = _Helpers.urlParam('page') ? _Helpers.urlParam('page') : (_Crud.defaultPage ? _Crud.defaultPage : 1);
		}

		if (!_Crud.pageSize[type]) {
			_Crud.pageSize[type] = _Helpers.urlParam('pageSize') ? _Helpers.urlParam('pageSize') : (_Crud.defaultPageSize ? _Crud.defaultPageSize : 10);
		}

		el.insertAdjacentHTML('beforeend', `
			<div class="flex items-center justify-between">

				<div class="pager whitespace-nowrap flex items-center">
					<button class="pageLeft flex" style="margin: 0! important; padding: 0.5rem 0.25rem; background: transparent;">
						${_Icons.getSvgIcon(_Icons.iconChevronLeft)}
					</button>
					<span class="pageWrapper">
						<input class="pageNo" type="text" size="3" value="${_Crud.page[type]}">
						<span class="of">of</span>
						<input readonly="readonly" class="readonly pageCount" type="text" size="3" value="${_Helpers.nvl(_Crud.pageCount, 0)}">
					</span>
					<button class="pageRight flex" style="margin: 0! important; padding: 0.5rem 0.25rem; background: transparent;">
						${_Icons.getSvgIcon(_Icons.iconChevronRight)}
					</button>
					<span class="ml-2 mr-1">Page Size:</span>
					<input class="pageSize" type="text" value="${_Crud.pageSize[type]}">
					<span class="ml-2 mr-1">View:</span>
					<select class="view hover:bg-gray-100 focus:border-gray-666 active:border-green">
						${Object.keys(_Crud.availableViews[type]).map(view => `<option${(_Crud.view[type] === view) ? ' selected' : ''}>${view}</option>`).join('')}
					</select>
				</div>

				<div class="resource-link mr-4 flex items-center">
					<a target="_blank" href=""></a>
				</div>
			</div>
		`);

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
	updateResourceLink: (type) => {

		let resourceLink = document.querySelector('#crud-type-detail .resource-link a');
		let endpointURL  = `${Structr.rootUrl}${type}/${_Crud.view[type]}?${Structr.getRequestParameterName('pageSize')}=${_Crud.pageSize[type]}&${Structr.getRequestParameterName('page')}=${_Crud.page[type]}`;

		resourceLink.setAttribute('href', endpointURL);
		resourceLink.textContent = endpointURL;
	},
	storeType: () => {
		LSWrapper.setItem(_Crud.crudTypeKey, _Crud.type);
	},
	restoreType: () => {
		let val = LSWrapper.getItem(_Crud.crudTypeKey);
		if (val) {
			_Crud.type = val;
		}
	},
	storePagerData: () => {
		let type      = _Crud.type;
		let pagerData = `${_Crud.view[type]},${_Crud.sort[type]},${_Crud.order[type]},${_Crud.page[type]},${_Crud.pageSize[type]}`;
		LSWrapper.setItem(_Crud.crudPagerDataKey + type, pagerData);
	},
	restorePagerData: () => {
		let type = _Crud.type;
		let val  = LSWrapper.getItem(_Crud.crudPagerDataKey + type);

		if (val) {
			let pagerData        = val.split(',');
			_Crud.view[type]     = pagerData[0];
			_Crud.sort[type]     = pagerData[1];
			_Crud.order[type]    = pagerData[2];
			_Crud.page[type]     = pagerData[3];
			_Crud.pageSize[type] = pagerData[4];
		}
	},
	setCollectionPageSize: (type, key, value) => {
		LSWrapper.setItem(`${_Crud.crudPagerDataKey}_collectionPageSize_${type}.${_Crud.cssClassForKey(key)}`, value);
	},
	getCollectionPageSize: (type, key) => {
		return LSWrapper.getItem(`${_Crud.crudPagerDataKey}_collectionPageSize_${type}.${_Crud.cssClassForKey(key)}`);
	},
	setCollectionPage: (type, key, value) => {
		LSWrapper.setItem(`${_Crud.crudPagerDataKey}_collectionPage_${type}.${_Crud.cssClassForKey(key)}`, value);
	},
	getCollectionPage: (type, key) => {
		return LSWrapper.getItem(`${_Crud.crudPagerDataKey}_collectionPage_${type}.${_Crud.cssClassForKey(key)}`);
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

					fetch(`${Structr.rootUrl}_schema/${type}/${_Crud.defaultView}`).then(async response => {

						if (response.ok) {

							let data = await response.json();

							// no schema entry found?
							if (!data || !data.result || data.result_count === 0) {

								new WarningMessage().text(`Unable to find schema information for type '${type}'. There might be database nodes with no type information or a type unknown to Structr in the database.`).show();

							} else {

								let sortOrder    = _Crud.getSortOrderOfColumns(type);
								let currentOrder = _Crud.filterKeys(type, Object.keys(_Crud.getCurrentProperties(type)));

								if (sortOrder.length > 0) {
									currentOrder = sortOrder;
								}

								let properties   = Object.fromEntries(data.result.map(prop => [prop.jsonName, prop]));
								let hiddenKeys   = _Crud.getHiddenKeys(type).filter(attr => currentOrder.indexOf(attr) === -1);

								let orderedColumnsSet = new Set(currentOrder);
								for (let key of Object.keys(properties)) {
									orderedColumnsSet.add(key);
								}

								let options = Array.from(orderedColumnsSet).map(key => {
									let isHidden   = hiddenKeys.includes(key);
									let isIdOrType = (key === 'id' || key === 'type');
									let isSelected = ((!isHidden || isIdOrType) ? 'selected' : '');
									let isDisabled = (isIdOrType ? 'disabled' : '');

									return `<option value="${key}" ${isSelected} ${isDisabled}>${key}</option>`;
								}).join('');
								columnSelect.insertAdjacentHTML('beforeend', options);

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

									_Crud.saveSortOrderOfColumns(type, jqSelect.sortedValues());
									_Crud.reloadList();

									_Dialogs.custom.clickDialogCancelButton();
								});

								console.log(jqSelect);
							}
						}
					});
				});

			} else if (key !== 'Actions') {

				let sortKey = key;
				th.innerHTML = `
					<a class="${((_Crud.sort[type] === key) ? 'column-sorted-active' : '')}" href="${_Crud.sortAndPagingParameters(type, sortKey, newOrder, _Crud.pageSize[type], _Crud.page[type])}#${type}">${key}</a>
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['icon-lightgrey', 'cursor-pointer']), 'Hide column ' + key)}
				`;

				if (_Crud.isCollection(key, type)) {
					_Crud.appendPerCollectionPager($(th), type, key);
				}

				$('a', th).on('click', function(event) {
					event.preventDefault();
					_Crud.sort[type] = key;
					_Crud.order[type] = (_Crud.order[type] && _Crud.order[type] === 'desc' ? 'asc' : 'desc');
					_Crud.refreshList(type);
					return false;
				});

				$('svg', th).on('click', function(event) {
					event.preventDefault();
					_Crud.toggleColumn(type, key);
					return false;
				});
			}
		}
	},
	appendPerCollectionPager: (el, type, key, callback) => {
		el.append(`<input type="text" class="collection-page-size" size="1" value="${_Crud.getCollectionPageSize(type, key) || _Crud.defaultCollectionPageSize}">`);

		$('.collection-page-size', el).on('blur', function() {
			var newPageSize = $(this).val();
			if (newPageSize !== _Crud.getCollectionPageSize(type, key)) {
				_Crud.setCollectionPageSize(type, key, newPageSize);
				if (callback) {
					callback();
				} else {
					_Crud.refreshList(type);
				}
			}
		});

		$('.collection-page-size', el).on('keypress', function(e) {
			if (e.keyCode === 13) {
				var newPageSize = $(this).val();
				if (newPageSize !== _Crud.getCollectionPageSize(type, key)) {
					_Crud.setCollectionPageSize(type, key, newPageSize);
					if (callback) {
						callback();
					} else {
						_Crud.refreshList(type);
					}
				}
			}
		});
	},
	updateCellPager: (el, id, type, key, page, pageSize) => {

		fetch(`${Structr.rootUrl}${type}/${id}/${key}/public?${Structr.getRequestParameterName('page')}=${page}&${Structr.getRequestParameterName('pageSize')}=${pageSize}`).then(async response => {

			if (response.ok) {

				let data = await response.json();

				let softLimited = false;
				let pageCount   = data.page_count;

				// handle new soft-limited REST result without counts
				if (data.result_count === undefined && data.page_count === undefined) {
					pageCount = _Crud.getSoftLimitedPageCount(pageSize);
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
					_Crud.getAndAppendNode(type, id, key, preloadedNode.id, el, preloadedNode);
				}

				if (softLimited) {
					_Crud.showSoftLimitAlert($('input.page-count'));
				}
			}
		});

	},
	appendCellPager: (el, id, type, key) => {

		let pageSize = _Crud.getCollectionPageSize(type, key) || _Crud.defaultCollectionPageSize;

		// use public view for cell pager - we should not need more information than this!
		fetch(`${Structr.rootUrl}${type}/${id}/${key}/public${_Crud.sortAndPagingParameters(null, null, null, pageSize, null)}`).then(async response => {

			let data = await response.json();

			if (response.ok) {

				let softLimited = false;
				let resultCount = data.result_count;
				let pageCount   = data.page_count;

				if (data.result && data.result.length > 0) {
					for (let preloadedNode of data.result) {
						_Crud.getAndAppendNode(type, id, key, preloadedNode.id, el, preloadedNode);
					}
				}

				let page = 1;

				// handle new soft-limited REST result without counts
				if (data.result_count === undefined && data.page_count === undefined) {
					resultCount = _Crud.getSoftLimitedResultCount();
					pageCount   = _Crud.getSoftLimitedPageCount(pageSize);
					softLimited = true;
				}

				if (!resultCount || !pageCount || pageCount === 1) {
					return;
				}

				el.prepend('<div class="cell-pager"></div>');

				el[0].insertAdjacentHTML('afterbegin', `
					<div class="cell-pager whitespace-nowrap flex items-center">
						<button class="prev disabled flex" style="margin: 0! important; padding: 0.5rem 0.25rem; background: transparent;">
							${_Icons.getSvgIcon(_Icons.iconChevronLeft)}
						</button>
						<span class="pageWrapper">
							<input class="collection-page" type="text" size="1" value="${page}">
							<span class="of">of</span>
							<input readonly="readonly" class="readonly page-count" type="text" size="1" value="${pageCount}">
						</span>
						<button class="next disabled flex" style="margin: 0! important; padding: 0.5rem 0.25rem; background: transparent;">
							${_Icons.getSvgIcon(_Icons.iconChevronRight)}
						</button>
					</div>
				`);


				if (page > 1) {
					$('.cell-pager .prev', el).removeClass('disabled').prop('disabled', '');
				}

				$('.collection-page', $('.cell-pager', el)).on('blur', function() {
					var newPage = $(this).val();
					_Crud.updateCellPager(el, id, type, key, newPage, pageSize);
				});

				$('.collection-page', $('.cell-pager', el)).on('keypress', function(e) {
					if (e.keyCode === 13) {
						var newPage = $(this).val();
						_Crud.updateCellPager(el, id, type, key, newPage, pageSize);
					}
				});

				if (page < pageCount) {
					$('.cell-pager .next', el).removeClass('disabled').prop('disabled', '');
				}

				$('.prev', el).on('click', function() {
					let page    = $('.cell-pager .collection-page', el).val();
					let newPage = Math.max(1, page - 1);
					_Crud.updateCellPager(el, id, type, key, newPage, pageSize);
				});

				$('.next', el).on('click', function() {
					let page    = $('.cell-pager .collection-page', el).val();
					let newPage = parseInt(page) + 1;
					_Crud.updateCellPager(el, id, type, key, newPage, pageSize);
				});

				if (softLimited) {
					_Crud.showSoftLimitAlert($('input.page-count'));
				}

			} else {

//				console.log('appendCellPager', id, el, type, key);
//				console.log('Property: ', _Crud.keys[type][key]);
//				console.log('Error: ', textStatus, errorThrown, jqXHR.responseJSON);
			}
		});

	},
	sortAndPagingParameters: function(t, s, o, ps, p, exact = false) {

		let paramsArray = [];

		if (s) {
			paramsArray.push(Structr.getRequestParameterName('sort') + '=' + s);
		}
		if (o) {
			paramsArray.push(Structr.getRequestParameterName('order') + '=' + o);
		}
		if (ps) {
			paramsArray.push(Structr.getRequestParameterName('pageSize') + '=' + ps);
		}
		if (p) {
			paramsArray.push(Structr.getRequestParameterName('page') + '=' + p);
		}
		if (exact === true) {
			paramsArray.push('type=' + t);
		}

		return '?' + paramsArray.join('&');
	},
	refreshList: (type) => {
		_Crud.clearList(type);
		_Crud.activateList(type);
	},
	activateList: (type) => {
		let url = Structr.rootUrl + type + '/all' + _Crud.sortAndPagingParameters(type, _Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type], _Crud.exact[type]);
		_Crud.list(type, url);
	},
	clearList: () => {
		let div = $('#crud-type-detail table tbody');
		_Helpers.fastRemoveAllChildren(div[0]);
	},
	list: (type, url, isRetry) => {

		let properties = _Crud.getCurrentProperties(type);

		_Crud.showLoadingMessageAfterDelay(`Loading data for type <b>${type}</b>`, 100);

		let acceptHeaderProperties = (isRetry ? '' : ' properties=' + _Crud.filterKeys(type, Object.keys(properties)).join(','));

		fetch (url, {
			headers: {
				Range: _Crud.ranges(type),
				Accept: 'application/json; charset=utf-8;' + acceptHeaderProperties
			}
		}).then(async response => {

			let data = await response.json();

			if (response.ok) {

				_Crud.removeMessage();

				if (!data || !Structr.isModuleActive(_Crud)) {
					return;
				}

				_Crud.crudCache.clear();

				for (let item of data.result) {
					StructrModel.create(item);
					_Crud.appendRow(type, properties, item);
				}
				_Crud.updatePager(type, data);
				_Crud.replaceSortHeader(type);

				if (_Crud.types[type].isRel && !_Crud.relInfo[type]) {

					let button = $('#crud-buttons #create' + type);
					button.attr('disabled', 'disabled').addClass('disabled');
					_Helpers.appendInfoTextToElement({
						text: 'Action not supported for built-in relationship types',
						element: $('#crud-buttons #create' + type),
						css: {
							marginRight: '10px'
						},
						offsetY: -50,
						insertAfter: true
					});
				}

			} else {

				if (response.status === 431) {
					// Happens if headers grow too large (property list too long)

					if (!isRetry) {
						_Crud.list(type, url, true);
					} else {
						_Crud.showMessageAfterDelay(_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 24, 24, 'mr-2') + ' View is too large - please select different view', 1);
					}

				} else {
					console.log(type, url);
				}

				_Crud.removeMessage();
			}
		});

	},
	ranges: function(type) {
		var ranges = '';
		var keys = type && _Crud.keys[type] && Object.keys(_Crud.keys[type]);
		if (!keys) {
			var typeDef = _Crud.type[type];
			if (typeDef) {
				keys = Object.keys(typeDef.views[_Crud.view[type]]);
			}
		}
		if (keys) {
			keys.forEach(function(key) {
				if ( _Crud.isCollection(key, type)) {
					var page = _Crud.getCollectionPage(type, key) || 1;
					var pageSize = _Crud.getCollectionPageSize(type, key) || _Crud.defaultCollectionPageSize;
					var start = (page-1)*pageSize;
					var end = page*pageSize;
					ranges += `${key}=${start}-${end};`;
				}
			});
			return ranges;
		}
	},
	crudExport: (type) => {

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

		let hiddenKeys             = _Crud.getHiddenKeys(type);
		let acceptHeaderProperties = Object.keys(_Crud.keys[type]).filter(key => !hiddenKeys.includes(key)).join(',');

		fetch(`${Structr.csvRootUrl}${type}/all${_Crud.sortAndPagingParameters(type, _Crud.sort[type], _Crud.order[type], _Crud.pageSize[type], _Crud.page[type])}`, {
			headers: {
				Range: _Crud.ranges(type),
				Accept: 'properties=' + acceptHeaderProperties
			}
		}).then(async response => {

			let data = await response.text();
			exportArea.value = data;
		})
	},
	crudImport: (type) => {

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

				_Crud.refreshList(type);

			} else {

				let data = await response.data;
				if (data) {
					Structr.errorFromResponse(data, url);
				}
			}
		});
	},
	deleteAllNodesOfType: async (type, exact) => {

		let url      = `${Structr.rootUrl}${type}${((exact === true) ? `?type=${type}` : '')}`;
		let response = await fetch(url, { method: 'DELETE' });

		if (response.ok) {

			new SuccessMessage().text(`Deletion of all nodes of type '${type}' finished.`).show();
			_Crud.typeSelected(type);

		} else {

			let data = await response.json();
			Structr.errorFromResponse(data, url, { statusCode: 400, requiresConfirmation: true });
		}
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
			pageCount = _Crud.getSoftLimitedPageCount(data.page_size);
			softLimited = true;
		}

		_Crud.pageCount = pageCount;
		$('.pageCount', typeNode).val(_Crud.pageCount);

		if (softLimited) {
			_Crud.showSoftLimitAlert($('input.pageCount'));
		} else {
			_Crud.showActualResultCount($('input.pageCount'), data.result_count);
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

		_Crud.updateUrl(type);
	},
	activatePagerElements: (type, pagerNode) => {

		$('.pageNo', pagerNode).on('keypress', function(e) {
			if (e.keyCode === 13) {
				_Crud.page[type] = $(this).val();
				_Crud.refreshList(type);
			}
		});

		$('.pageSize', pagerNode).on('keypress', function(e) {
			if (e.keyCode === 13) {
				// calculate which page we should be on after the pagesize changed
				var oldFirstObject = ((_Crud.page[type] -1 ) * _Crud.pageSize[type]) + 1;
				var newPage = Math.ceil(oldFirstObject / $(this).val());
				_Crud.pageSize[type] = $(this).val();
				_Crud.page[type] = newPage;
				_Crud.refreshList(type);
			}
		});

		$('select.view', pagerNode).on('change', function(e) {
			_Crud.view[type] = $(this).val();
			_Crud.updateCrudTableHeader(type);
			_Crud.refreshList(type);
		});

		let pageLeft  = $('.pageLeft', pagerNode);
		let pageRight = $('.pageRight', pagerNode);

		pageLeft.on('click', function() {
			pageRight.removeAttr('disabled').removeClass('disabled');
			if (_Crud.page[type] > 1) {
				_Crud.page[type]--;
				_Crud.refreshList(type);
			}
		});

		pageRight.on('click', function() {
			pageLeft.removeAttr('disabled').removeClass('disabled');
			if (_Crud.page[type] < _Crud.pageCount) {
				_Crud.page[type]++;
				_Crud.refreshList(type);
			}
		});

	},
	deActivatePagerElements: (pagerNode) => {

		$('.pageNo', pagerNode).off('keypress');
		$('.pageSize', pagerNode).off('keypress');
		$('.pageLeft', pagerNode).off('click');
		$('.pageRight', pagerNode).off('click');
	},
	crudCreate: (type, json, onError) => {

		fetch(Structr.rootUrl + type, {
			method: 'POST',
			body: json
		}).then(async response => {

			let data = await response.json();

			if (response.ok) {

				let properties = _Crud.getCurrentProperties(type);

				let newNodeResponse = await fetch(`${Structr.rootUrl}${data.result[0]}/all`, {
					headers: {
						Accept: 'application/json; charset=utf-8; properties=' + _Crud.filterKeys(type, Object.keys(properties)).join(',')
					}
				});

				if (newNodeResponse.ok) {

					let newNodeResult = await newNodeResponse.json();
					let newNode       = newNodeResult.result;
					_Crud.appendRow(type, properties, newNode);

					_Helpers.blinkGreen(_Crud.row(newNode.id));

				} else {

					_Crud.refreshList(type);
				}

			} else {

				if (response.status !== 422 || _Dialogs.custom.isDialogOpen()) {
					Structr.errorFromResponse(data, url, { statusCode: response.status, requiresConfirmation: true });
				}
				_Crud.showCreateError(type, data, onError);
			}
		});
	},
	showCreateError: (type, data, onError) => {

		if (onError) {

			onError();

		} else {

			let dialogText = _Dialogs.custom.getDialogTextElement();

			if (!_Dialogs.custom.isDialogOpen()) {
				console.log('show create dialog!');
				let elements = _Crud.showCreate(type);
				dialogText = elements.dialogText;
			}

			$('.props input', $(dialogText)).css({
				backgroundColor: '#fff',
				borderColor: '#a5a5a5'
			});

			$('.props textarea', $(dialogText)).css({
				backgroundColor: '#fff',
				borderColor: '#a5a5a5'
			});

			$('.props td.value', $(dialogText)).css({
				backgroundColor: '#fff',
				borderColor: '#b5b5b5'
			});

			window.setTimeout(() => {

				for (let error of data.errors) {

					let key      = error.property;
					let errorMsg = error.token;

					let input = $(`td [name="${key}"]`, $(dialogText));
					if (input.length) {

						let errorText = `"${key}" ${errorMsg.replace(/_/gi, ' ')}`;

						if (error.detail) {
							errorText += ` ${error.detail}`;
						}

						_Dialogs.custom.showAndHideInfoBoxMessage(errorText, 'error', 2000, 1000);

						input.css({
							backgroundColor: '#fee',
							borderColor: '#933'
						});

						input.focus();
					}
				}
			}, 100);
		}
	},
	crudRefresh: (id, key, oldValue) => {

		fetch(`${Structr.rootUrl}${id}/all`, {
			headers: {
				Accept: `application/json; charset=utf-8; properties=id,type,${key}`
			}
		}).then(async response => {

			if (response.ok) {

				let data = await response.json();

				if (data) {
					if (key) {
						_Crud.refreshCell(id, key, data.result[key], data.result.type, oldValue);
					} else {
						_Crud.refreshRow(id, data.result, data.result.type);
					}
				}
			}
		});
	},
	crudReset: (id, key) => {

		fetch(`${Structr.rootUrl}${id}/all`, {
			headers: {
				Accept: `application/json; charset=utf-8; properties=id,type,${key}`
			}
		}).then(async response => {

			if (response.ok) {

				let data = await response.json();

				if (data) {
					_Crud.resetCell(id, key, data.result[key]);
				}
			}
		});
	},
	crudUpdateObj: (id, json, onSuccess, onError) => {

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
					_Crud.crudRefresh(id);
				}

			} else {

				Structr.errorFromResponse(data, url, { statusCode: response.status, requiresConfirmation: true });

				if (typeof onError === "function") {
					onError();
				} else {
					_Crud.crudReset(id);
				}
			}
		});
	},
	crudUpdate: (id, key, newValue, oldValue, onSuccess, onError) => {

		let url = `${Structr.rootUrl}${id}`;

		let obj = {};
		obj[key] = (newValue && newValue !== '') ? newValue : null;

		fetch(url, {
			method: 'PUT',
			body: JSON.stringify(obj)
		}).then(async response => {

			let data = await response.json();

			if (response.ok) {
				if (typeof onSuccess === "function") {
					onSuccess();
				} else {
					_Crud.crudRefresh(id, key, oldValue);
				}
			} else {
				Structr.errorFromResponse(data, url, { statusCode: response.status, requiresConfirmation: true });

				if (typeof onError === "function") {
					onError();
				} else {
					_Crud.crudReset(id, key);
				}
			}
		});

	},
	crudRemoveProperty: (id, key, onSuccess, onError) => {

		let url = `${Structr.rootUrl}${id}`;
		let obj = {};
		obj[key] = null;

		fetch(url, {
			method: 'PUT',
			body: JSON.stringify(obj)
		}).then(async response => {

			let data = await response.json();

			if (response.ok) {

				if (typeof onSuccess === "function") {
					onSuccess();
				} else {
					_Crud.crudRefresh(id, key);
				}

			} else {

				Structr.errorFromResponse(data, url, { statusCode: response.status, requiresConfirmation: true });

				if (typeof onError === "function") {
					onError();
				} else {
					_Crud.crudReset(id, key);
				}
			}
		});

	},
	crudDelete: (type, id) => {

		let url = `${Structr.rootUrl}${type}/${id}`;

		fetch(url, {
			method: 'DELETE'
		}).then(async response => {

			let data = await response.json();

			if (response.ok) {

				let row = _Crud.row(id);
				_Helpers.fastRemoveElement(row[0]);

			} else {
				Structr.errorFromResponse(data, url, { statusCode: response.status, requiresConfirmation: true });
			}
		});

	},
	cells: (id, key) => {

		let row = _Crud.row(id);

		let cellInMainTable    = $('.' + _Crud.cssClassForKey(key), row);
		let cellInDetailsTable = $('.' + _Crud.cssClassForKey(key), $('#details_' + id));

		let result = [];

		if (cellInMainTable && cellInMainTable.length > 0) {
			result.push(cellInMainTable);
		}

		if (cellInDetailsTable && cellInDetailsTable.length > 0) {
			result.push(cellInDetailsTable);
		}

		return result;
	},
	resetCell: (id, key, oldValue) => {

		let cells = _Crud.cells(id, key);

		for (let cell of cells) {

			_Helpers.fastRemoveAllChildren(cell[0]);
			_Crud.populateCell(id, key, _Crud.type, oldValue, cell);
		}

	},
	refreshCell: (id, key, newValue, type, oldValue) => {

		let cells = _Crud.cells(id, key);

		for (let cell of cells) {

			_Helpers.fastRemoveAllChildren(cell[0]);
			_Crud.populateCell(id, key, type, newValue, cell);

			if (newValue !== oldValue && !(!newValue && oldValue === '')) {
				_Helpers.blinkGreen(cell);
			}
		}

	},
	refreshRow: (id, item, type) => {

		let row = _Crud.row(id);
		_Helpers.fastRemoveAllChildren(row[0]);
		_Crud.populateRow(id, item, type, _Crud.keys[type]);

	},
	activateTextInputField: (el, id, key, propertyType) => {

		var oldValue = el.text();
		el.off('click');
//		var w = el.width(), h = el.height();
		var input;
		if (propertyType === 'String') {
			el.html(`<textarea name="${key}" class="__value"></textarea>`);
			input = $('textarea', el);
//			input.width(w);
//			input.height(h);
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
				_Crud.crudUpdate(id, key, newValue, oldValue);
			}
		});

	},
	row: (id) => {
		return $('tr#id_' + id);
	},
	appendRow: (type, properties, item) => {

		_Crud.getProperties(item.type, () => {

			let id = item['id'];
			let tbody = $('#crud-type-detail table tbody');
			let row = _Crud.row(id);

			if ( !(row && row.length) ) {
				tbody.append(`<tr id="id_${id}"></tr>`);
			}
			_Crud.populateRow(id, item, type, properties);
		});

	},
	populateRow: (id, item, type, properties) => {

		let row = _Crud.row(id);
		_Helpers.fastRemoveAllChildren(row[0]);

		if (properties) {

			let actions = $(`
				<td class="actions">
					${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, _Icons.getSvgIconClassesNonColorIcon(['mr-1', 'edit']))}
					${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['mr-1', 'icon-red', 'delete']), 'Remove')}
				</td>
			`);

			if (!(_Crud.types[type] && _Crud.types[type].isRel === true)) {
				_Entities.appendNewAccessControlIcon(actions, item, false);
			}

			row.append(actions);

			let filterKeys = _Crud.filterKeys(type, Object.keys(properties));

			for (let key of filterKeys) {

				row.append(`<td class="value ${_Crud.cssClassForKey(key)}"></td>`);
				let cells = _Crud.cells(id, key);

				for (let cell of cells) {
					_Crud.populateCell(id, key, type, item[key], cell);
				}
			}

			Structr.resize();

			row[0].querySelector('.actions .edit').addEventListener('click', (e) => {
				_Crud.showDetails(id, type);
			});

			row[0].querySelector('.actions .delete').addEventListener('click', async (e) => {
				let confirm = await _Dialogs.confirmation.showPromise(`<p>Are you sure you want to delete <b>${type}</b> ${id}?</p>`);
				if (confirm === true) {
					_Crud.crudDelete(type, id);
				}
			});
		}
	},
	populateCell: (id, key, type, value, cell) => {

		let isRel            = _Crud.types[type].isRel;
		let isCollection     = _Crud.isCollection(key, type);
		let isEnum           = _Crud.isEnum(key, type);
		let isCypher         = _Crud.isCypherProperty(key, type);
		let relatedType      = _Crud.relatedType(key, type);
		let readOnly         = _Crud.readOnly(key, type);
		let isSourceOrTarget = _Crud.types[type].isRel && (key === 'sourceId' || key === 'targetId' || key === 'sourceNode' || key === 'targetNode');
		let propertyType     = _Crud.keys[type][key].type;
		let simpleType;

		if (readOnly) {
			cell.addClass('readonly');
		}

		if (!isSourceOrTarget && !relatedType) {

			if (!_Crud.keys[type]) {
				return;
			}

			if (propertyType === 'Boolean') {

				cell.append(`<input name="${key}" ${readOnly ? 'class="readonly" readonly disabled ' : ''}type="checkbox" ${value ? 'checked="checked"' : ''}>`);

				if (!readOnly) {
					$('input', cell).on('change', function() {
						if (id) {
							let checked = $(this).prop('checked');
							_Crud.crudUpdate(id, key, checked, undefined, () => {

								_Crud.crudRefresh(id, key, !checked);

								if (key === 'visibleToPublicUsers' || key === 'visibleToAuthenticatedUsers') {
									StructrModel.updateKey(id, key, checked);
									_Entities.updateNewAccessControlIconInElement(StructrModel.obj(id), Structr.node(id));
								}
							});
						}
					});
				}

			} else if (propertyType === 'Date') {

				cell.html(_Helpers.nvl(value, _Icons.getSvgIcon(_Icons.iconDatetime, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-lightgray']))));

				if (!readOnly) {

					let format = _Crud.isFunctionProperty(key, type) ? "yyyy-MM-dd'T'HH:mm:ssZ" : _Crud.getFormat(key, type);
					let dateTimePickerFormat = _Helpers.getDateTimePickerFormat(format);

					cell.on('click', function(event) {
						event.preventDefault();
						var self = $(this);
						var oldValue = self.text().trim();
						self.html(`<input name="${key}" class="__value" type="text" size="40">`);
						var input = $('input', self);
						input.val(oldValue);

						if (dateTimePickerFormat.timeFormat) {
							input.datetimepicker({
								parse: 'loose',
								dateFormat: dateTimePickerFormat.dateFormat,
								timeFormat: dateTimePickerFormat.timeFormat,
								separator: dateTimePickerFormat.separator,
								onClose: function() {
									var newValue = input.val();
									if (id && newValue !== oldValue) {
										_Crud.crudUpdate(id, key, newValue);
									} else {
										_Crud.resetCell(id, key, oldValue);
									}
								}
							});
							input.datetimepicker('show');
						} else {
							input.datepicker({
								parse: 'loose',
								dateFormat: dateTimePickerFormat.dateFormat,
								onClose: function() {
									var newValue = input.val();
									if (id && newValue !== oldValue) {
										_Crud.crudUpdate(id, key, newValue);
									} else {
										_Crud.resetCell(id, key, oldValue);
									}
								}
							});
							input.datepicker('show');
						}
						self.off('click');
					});
				}

			} else if (isEnum) {

				let format = _Crud.getFormat(key, type);
				cell.text(_Helpers.nvl(value, ''));
				if (!readOnly) {
					cell.on('click', function (event) {
						event.preventDefault();
						_Crud.appendEnumSelect(cell, id, key, format);
					});
					if (!id) { // create
						_Crud.appendEnumSelect(cell, id, key, format);
					}
				}

			} else if (isCypher) {

				cell.text((value === undefined || value === null) ? '' : JSON.stringify(value));

			} else if (isCollection) { // Array types

				let values = value || [];

				if (!id) {

					let focusAndActivateField = function (el) {
						$(el).focus().on('keydown', function (e) {
							if (e.which === 9) { // tab key
								e.stopPropagation();
								cell.append(`<input name="${key}" size="4">`);
								focusAndActivateField(cell.find(`[name="${key}"]`).last());
								return false;
							}
						});
						return false;
					};

					// create dialog
					_Schema.getTypeInfo(type, function (typeInfo) {
						cell.append(`<input name="${key}" size="4">`);
						focusAndActivateField(cell.find(`[name="${key}"]`).last());
					});

				} else {

					// update existing object
					_Schema.getTypeInfo(type, function (typeInfo) {
						cell.append(_Helpers.formatArrayValueField(key, values, typeInfo.format === 'multi-line', typeInfo.readOnly, false));
						cell.find(`[name="${key}"]`).each(function (i, el) {
							_Entities.activateInput(el, id, null, typeInfo, function () {
								_Crud.crudRefresh(id, key);
							});
						});
					});
				}

			} else {

				cell.text(_Helpers.nvl(value, ''));

				if (!readOnly) {
					cell.on('click', function(event) {
						event.preventDefault();
						var self = $(this);
						_Crud.activateTextInputField(self, id, key, propertyType);
					});
					if (!id) { // create
						_Crud.activateTextInputField(cell, id, key, propertyType);
					}
				}
			}

		} else if (isCollection) {

			simpleType = relatedType.substring(relatedType.lastIndexOf('.') + 1);

			cell.append(_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['add', 'icon-lightgrey', 'cursor-pointer'])));

			$('.add', cell).on('click', function() {

				let { dialogText } = _Dialogs.custom.openDialog('Add ' + simpleType);
				_Crud.displaySearch(type, id, key, simpleType, $(dialogText));
			});

			if (_Crud.keys[type][key] && _Crud.keys[type][key].className.indexOf('CollectionIdProperty') === -1 && _Crud.keys[type][key].className.indexOf("CollectionNotionProperty") === -1) {

				_Crud.appendCellPager(cell, id, type, key);
			}

		} else {

			simpleType = relatedType.substring(relatedType.lastIndexOf('.') + 1);

			if (isRel && _Crud.relInfo[type]) {

				if (key === 'sourceId') {
					simpleType = _Crud.relInfo[type].source;
				} else if (key === 'targetId') {
					simpleType = _Crud.relInfo[type].target;
				}
			}

			if (value) {

				_Crud.getAndAppendNode(type, id, key, value, cell);

			} else {

				if (simpleType) {

					cell.append(_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['add', 'icon-lightgrey', 'cursor-pointer'])));
					$('.add', cell).on('click', function() {

						if (!_Dialogs.custom.isDialogOpen() || isRel == false) {

							let { dialogText } = _Dialogs.custom.openDialog(`Add ${simpleType} to ${key}`);
							_Crud.displaySearch(type, id, key, simpleType, $(dialogText));

						} else {

							//TODO
							let dialogText = $(_Dialogs.custom.getDialogTextElement());

							var btn = $(this);
							$('#entityForm').hide();
							_Crud.displaySearch(type, id, key, simpleType, dialogText, function (n) {

								$('.searchBox', dialogText).remove();
								btn.remove();

								_Crud.getAndAppendNode(type, id, key, n, cell, n, true);
								_Crud.clearSearchResults(dialogText);
								$('#entityForm').show();
							});
						}
					});
				}
			}
		}

		if (id && !isSourceOrTarget && !readOnly && !relatedType && propertyType !== 'Boolean') {

			cell.prepend(_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['crud-clear-value', 'icon-lightgrey', 'cursor-pointer'])));

			$('.crud-clear-value', cell).on('click', function(e) {
				e.preventDefault();
				_Crud.crudRemoveProperty(id, key);
				return false;
			});
		}

	},
	appendEnumSelect: function(cell, id, key, format) {
		cell.off('click');
		var input;
		var oldValue = cell.text();
		cell.empty().append(`<select name="${key}">`);
		input = $('select', cell);
		input.focus();
		var values = format.split(',');
		input.append('<option></option>');
		values.forEach(function(value) {
			value = value.trim();
			if (value.length > 0) {
				input.append(`<option ${value === oldValue ? 'selected="selected"' : ''}value="${value}">${value}</option>`);
			}
		});
		input.on('change', function() {
			let newValue = input.val();
			if (id) {
				_Crud.crudUpdate(id, key, newValue, oldValue);
			}
		});
		input.on('blur', function() {
			_Crud.resetCell(id, key, oldValue);
		});
	},
	getAndAppendNode: (parentType, parentId, key, obj, cell, preloadedNode, insertFakeInput) => {

		if (!obj) {
			return;
		}
		var id, type;
		if ((typeof obj) === 'object') {
			id = obj.id;
			type = obj.type;
		} else if (_Helpers.isUUID(obj)) {
			id = obj;
		} else {
			// search object by name
			type = _Crud.keys[parentType][key].relatedType.split('.').pop();

			fetch(Structr.rootUrl + type + '?name=' + obj).then(async response => {

				let data = await response.json();

				if (response.ok) {
					if (data.result.length > 0) {
						_Crud.getAndAppendNode(parentType, parentId, key, data.result[0], cell);
					}
				}
			});

			return;
		}

		let nodeHandler = (node) => {

			var displayName = _Crud.displayName(node);

			cell.append(`<div title="${_Helpers.escapeForHtmlAttributes(displayName)}" id="_${node.id}" class="node ${node.isImage ? 'image ' : ''} ${node.id}_ relative"><span class="name_ abbr-ellipsis abbr-75pc">${displayName}</span></div>`);
			var nodeEl = $('#_' + node.id, cell);

			var isSourceOrTarget = _Crud.types[parentType].isRel && (key === 'sourceId' || key === 'targetId');
			if (!isSourceOrTarget) {
				nodeEl.prepend(_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['remove', 'icon-lightgrey', 'cursor-pointer'])));
			} else if (insertFakeInput) {
				nodeEl.append(`<input type="hidden" name="${key}" value="${node.id}"></div>`);
			}

			if (node.isImage) {

				if (node.isThumbnail) {
					nodeEl.prepend(`<div class="wrap"><img class="thumbnail" src="/${node.id}"></div>`);
				} else if (node.tnSmall) {
					nodeEl.prepend(`<div class="wrap"><img class="thumbnail" src="/${node.tnSmall.id}"></div>`);
				} else if (node.contentType === 'image/svg+xml') {
					nodeEl.prepend(`<div class="wrap"><img class="thumbnail" src="/${node.id}"></div>`);
				}

				if (node.tnMid || node.contentType === 'image/svg+xml') {
					$('.thumbnail', nodeEl).on('mouseenter', function(e) {
						e.stopPropagation();
						$('.thumbnailZoom').remove();
						nodeEl.parent().append(`<img class="thumbnailZoom" src="/${node.tnMid ? node.tnMid.id : node.id}">`);
						var tnZoom = $($('.thumbnailZoom', nodeEl.parent())[0]);
						tnZoom.css({
							top: (nodeEl.position().top) + 'px',
							left: (nodeEl.position().left - 42) + 'px'
						});
						tnZoom.on('mouseleave', function(e) {
							e.stopPropagation();
							$('.thumbnailZoom').remove();
						});
					});
				}
			}

			$('.remove', nodeEl).on('click', function(e) {
				e.preventDefault();
				Command.get(parentId, 'id,type', (parentObj) => {
					_Crud.removeRelatedObject(parentObj, key, obj);
				});
				return false;
			});

			nodeEl.on('click', function(e) {
				e.preventDefault();
				_Crud.showDetails(node.id, node.type);
				return false;
			});
		};

		if (preloadedNode) {
			nodeHandler(preloadedNode);
		} else {
			_Crud.crudCache.registerCallback({ id: id, type: type }, id, nodeHandler);
		}

	},
	clearMainSearch: (el) => {

		_Crud.clearSearchResults(el);
		_Crud.searchFieldClearIcon.style.display = 'none';
		_Crud.searchField.value = '';
		$('#crud-type-detail').show();

	},
	clearSearchResults: (el) => {

		let searchResults = $('.searchResults', el);
		if (searchResults.length) {
			_Helpers.fastRemoveElement(searchResults[0]);
			return true;
		}
		return false;

	},
	/**
	 * Conduct a search and append search results to 'el'.
	 *
	 * If an optional type is given, restrict search to this type.
	 */
	search: (searchString, el, type, onClickCallback, optionalPageSize, blacklistedIds = []) => {

		_Crud.clearSearchResults(el);

		el.append(`<div class="searchResults"><h2>Search Results${(searchString !== '*' && searchString !== '') ? ` for "${searchString}"` : ''}</h2></div>`);
		let searchResults = $('.searchResults', el);

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
				types = ['AbstractNode'];
			}
		}

		for (let type of types) {

			let url, searchPart;
			if (attr === 'uuid') {
				url = `${Structr.rootUrl}${type}/${searchString}`;
			} else {
				searchPart = (searchString === '*' || searchString === '') ? '' : `&${attr}=${encodeURIComponent(searchString)}&${Structr.getRequestParameterName('loose')}=1`;
				url = `${Structr.rootUrl}${type}/public${_Crud.sortAndPagingParameters(type, 'name', 'asc', optionalPageSize || 1000, 1)}${searchPart}`;
			}

			searchResults.append(`
				<div id="placeholderFor${type}" class="searchResultGroup resourceBox flex items-center">
					${_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 24, 24, 'mr-2')} Searching for "${searchString}" in ${type}
				</div>
			`);

			fetch(url).then(async response => {

				if (response.ok) {

					let data = await response.json();

					if (!data || !data.result) {
						return;
					}

					let result = data.result;
					_Helpers.fastRemoveElement(document.querySelector(`#placeholderFor${type}`));

					if (result) {
						if (Array.isArray(result)) {
							if (result.length) {
								for (let node of result) {
									if (!blacklistedIds.includes(node.id)) {
										_Crud.searchResult(searchResults, type, node, onClickCallback);
									}
								}
							} else {
								_Crud.noResults(searchResults, type);
							}
						} else if (result.id) {
							_Crud.searchResult(searchResults, type, result, onClickCallback);
						}
					} else {
						_Crud.noResults(searchResults, type);
					}

				} else {
					_Helpers.fastRemoveElement(document.querySelector(`#placeholderFor${type}`));
				}
			});
		}

	},
	noResults: (searchResults, type) => {

		searchResults.append(`<div id="resultsFor${type}" class="searchResultGroup resourceBox">No results for ${type}</div>`);
		window.setTimeout(() => {
			$(`#resultsFor${type}`).fadeOut('fast');
		}, 1000);

	},
	searchResult: (searchResults, type, node, onClickCallback) => {
		if (!$(`#resultsFor${type}`, searchResults).length) {
			searchResults.append(`<div id="resultsFor${type}" class="searchResultGroup resourceBox"><h3>${type}</h3></div>`);
		}
		let displayName = _Crud.displayName(node);
		let title = `name: ${node.name}
id: ${node.id} 
type: ${node.type}`;
		$('#resultsFor' + type, searchResults).append(`<div title="${_Helpers.escapeForHtmlAttributes(title)}" class="_${node.id} node abbr-ellipsis abbr-120">${displayName}</div>`);

		let nodeEl = $(`#resultsFor${type} ._${node.id}`, searchResults);
		if (node.isImage) {
			nodeEl.append(`<div class="wrap"><img class="thumbnail" src="/${node.id}" alt=""></div>`);
		}

		nodeEl.on('click', function(e) {
			onClickCallback(e, node);
		});
	},
	displayName: (node) => {
		var displayName;
		if (node.isContent && node.content && !node.name) {
			displayName = _Helpers.escapeTags(node.content.substring(0, 100));
		} else {
			displayName = node.name || node.id || node;
		}
		return displayName;
	},
	displaySearch: (parentType, id, key, type, el, callbackOverride) => {

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
					_Crud.clearSearchResults(el);
					$('.clearSearchIcon').hide().off('click');
					search.focus();
					search.val('');
				});

				_Crud.search(searchString, el, type, function(e, node) {
					e.preventDefault();
					if (typeof callbackOverride === "function") {
						callbackOverride(node);
					} else {
						_Crud.addRelatedObject(parentType, id, key, node);
					}
					return false;
				});

			} else if (e.keyCode === 27) {

				if (searchString.trim() === '') {
					_Dialogs.custom.clickDialogCancelButton();
				}

				_Crud.clearSearchResults(el);
				$('.clearSearchIcon').hide().off('click');
				search.focus();
				search.val('');
			}

			return false;
		});

		_Crud.populateSearchDialogWithInitialResult(parentType, id, key, type, el, callbackOverride, "*");
	},
	populateSearchDialogWithInitialResult: (parentType, id, key, type, el, callbackOverride, initialSearchText) => {

		// display initial result list
		_Crud.search(initialSearchText, el, type, (e, node) => {
			e.preventDefault();
			if (typeof callbackOverride === "function") {
				callbackOverride(node);
			} else {
				_Crud.addRelatedObject(parentType, id, key, node, () => {});
			}
			return false;
		}, 100);
	},
	removeRelatedObject: (obj, key, relatedObj, callback) => {

		let type = obj.type;

		if (_Crud.isCollection(key, type)) {

			fetch(Structr.rootUrl + type + '/' + obj.id + '/all').then(async response => {

				if (response.ok) {

					let data      = await response.json();
					let relatedId = (typeof relatedObj === 'object' ? relatedObj.id : relatedObj);
					let objects   = _Crud.extractIds(data.result[key]).filter(obj => (obj.id !== relatedId));

					_Crud.updateRelatedCollection(obj.id, key, objects, callback);
				}
			});

		} else {
			_Crud.crudRemoveProperty(obj.id, key);
		}
	},
	addRelatedObject: (type, id, key, relatedObj, callback) => {

		if (_Crud.isCollection(key, type)) {

			fetch(`${Structr.rootUrl}${type}/${id}/all`).then(async response => {

				if (response.ok) {

					let data    = await response.json();
					let objects = _Crud.extractIds(data.result[key]);
					if (!_Helpers.isIn(relatedObj.id, objects)) {
						objects.push({'id': relatedObj.id});
					}

					_Crud.updateRelatedCollection(id, key, objects, callback);
				}
			});

		} else {
			let updateObj = {};
			updateObj[key] = {
				id: relatedObj.id
			};

			_Crud.crudUpdateObj(id, JSON.stringify(updateObj), () => {
				_Crud.crudRefresh(id, key);
				_Dialogs.custom.clickDialogCancelButton();
			});
		}

	},
	updateRelatedCollection: (id, key, objects, callback) => {

		let updateObj = {};
		updateObj[key] = objects;

		_Crud.crudUpdateObj(id, JSON.stringify(updateObj), () => {
			_Crud.crudRefresh(id, key);
			callback?.();
		});

	},
	removeStringFromArray: (type, id, key, obj, pos, callback) => {

		fetch(`${Structr.rootUrl}${type}/${id}/public`).then(async response => {

			if (response.ok) {

				let data = await response.json();

				obj = unescape(obj);
				let newData = {};
				let curVal = data.result[key];

				if (curVal[pos] === obj) {

					// string is at expected position => just remove that position/element
					curVal.splice(pos, 1);
					newData[key] = curVal;

				} else {

					// obj is not at position. it seems the crud is not up to date => remove the first occurence of the string (if any)
					let newVal = [];
					let found = false;
					for (let v of curVal) {
						if (v === obj && !found) {
							found = true;
						} else {
							newVal.push(v);
						}
					}
					newData[key] = newVal;
				}

				_Crud.crudUpdateObj(id, JSON.stringify(newData), () => {
					_Crud.crudRefresh(id, key);
					callback?.();
				});
			}
		});

	},
	addStringToArray: (type, id, key, obj, callback) => {

		fetch(`${Structr.rootUrl}${type}/${id}/all`).then(async response => {

			if (response.ok) {

				let data   = await response.json();
				let curVal = data.result[key];
				if (curVal === null) {
					curVal = [obj];
				} else {
					curVal.push(obj);
				}

				let newData = {};
				newData[key] = curVal;
				_Crud.crudUpdateObj(id, JSON.stringify(newData), () => {
					_Crud.crudRefresh(id, key);
					callback?.();
				});
			}
		});

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
	resize: () => {},
	showDetails: (id, type) => {

		if (!type) {
			new ErrorMessage().text('Missing type').requiresConfirmation().show();
			return;
		}

		let typeDef = _Crud.keys[type];

		if (!typeDef) {
			_Crud.getProperties(type, () => {
				_Crud.showDetails(id, type);
			});
			return;
		}

		let availableKeys = Object.keys(typeDef);
		let visibleKeys   = _Crud.filterKeys(type, availableKeys);

		if (_Dialogs.custom.isDialogOpen()) {
			_Dialogs.custom.clickDialogCancelButton();
		}

		let view = _Crud.view[type] || 'ui';

		fetch(`${Structr.rootUrl}${type}/${id}/${view}`, {
			headers: {
				Range: _Crud.ranges(type),
				Accept: `application/json; charset=utf-8;properties=${visibleKeys.join(',')}`
			}
		}).then(async response => {


			let data = await response.json();
			if (!data)
				return;

			let node = data.result;

			let { dialogText } = _Dialogs.custom.openDialog(`Details of ${type} ${node?.name ?? node.id}`);

			dialogText.insertAdjacentHTML('beforeend', `<table class="props" id="details_${node.id}"><tr><th>Name</th><th>Value</th>`);

			let table = dialogText.querySelector('table');

			for (let key of visibleKeys) {

				let cssClassForKey = _Crud.cssClassForKey(key);

				let row = _Helpers.createSingleDOMElementFromHTML(`
					<tr>
						<td class="key"><label for="${key}">${key}</label></td>
						<td class="__value ${cssClassForKey}"></td>
					</tr>
				`);
				table.appendChild(row);

				let cell = $(`.${cssClassForKey}`, $(row));

				if (_Crud.isCollection(key, type)) {
					_Crud.appendPerCollectionPager(cell.prev('td'), type, key, () => {
						_Crud.showDetails(node.id, type);
					});
				}

				_Crud.populateCell(node.id, key, node.type, node[key], cell);
			}

			if (node && node.isImage) {
				dialogText.insertAdjacentHTML('beforeend', `<div class="img"><div class="wrap"><img class="thumbnailZoom" src="/${node.id}"></div></div>`);
			}
		});
	},
	showCreate: (type) => {

		if (!type) {
			Structr.error('Missing type');
			return;
		}
		let typeDef = _Crud.types[type];

		let { dialogText } = _Dialogs.custom.openDialog(`Create new ${type}`);

		dialogText.insertAdjacentHTML('beforeend', '<form id="entityForm"><table class="props"><tr><th>Property Name</th><th>Value</th></tr>');

		let table = dialogText.querySelector('table');

		for (let key in _Crud.keys[type]) {

			let readOnly     = _Crud.readOnly(key, type);
			let isCollection = _Crud.isCollection(key, type);
			let relatedType  = _Crud.relatedType(key, type);

			if (!readOnly && !isCollection && (!relatedType || _Crud.relInfo[type])) {

				let cssClassForKey = _Crud.cssClassForKey(key);

				let row = _Helpers.createSingleDOMElementFromHTML(`
					<tr>
						<td class="key"><label for="${key}">${key}</label></td>
						<td class="__value ${cssClassForKey}"></td>
					</tr>
				`);
				table.appendChild(row);

				let cell = $(`.${cssClassForKey}`, $(row));

				_Crud.populateCell(null, key, type, null, cell);
			}
		}

		let dialogSaveButton = _Dialogs.custom.updateOrCreateDialogSaveButton();
		_Helpers.enableElement(dialogSaveButton);

		dialogSaveButton.addEventListener('click', () => {
			_Helpers.disableElement(dialogSaveButton);
			let json = JSON.stringify(_Crud.serializeObject($('#entityForm')));
			_Crud.crudCreate(type, json, undefined, () => { _Helpers.enableElement(dialogSaveButton); });
		});
	},
	getHiddenKeys: (type) => {

		let hiddenKeysSource = LSWrapper.getItem(_Crud.crudHiddenColumnsKey + type);
		let hiddenKeys = [];
		if (hiddenKeysSource) {

			hiddenKeys = JSON.parse(hiddenKeysSource);

			if (!Array.isArray(hiddenKeys)) {
				// migrate old format
				let newKeys = [];

				for (let key in hiddenKeys) {
					newKeys.push(key);
				}

				hiddenKeys = newKeys;
			}

		} else {

			// hide some keys depending on the type

			if (_Crud.isPrincipalType(_Crud.types[type])) {

				for (let key of _Crud.hiddenKeysForPrincipalTypes) {
					if (hiddenKeys.indexOf(key) === -1) {
						hiddenKeys.push(key);
					}
				}
			}

			if (_Crud.isImageType(_Crud.types[type])) {

				for (let key of _Crud.hiddenKeysForImageTypes) {
					if (hiddenKeys.indexOf(key) === -1) {
						hiddenKeys.push(key);
					}
				}
			}

			if (_Crud.isFileType(_Crud.types[type])) {

				for (let key of _Crud.hiddenKeysForFileTypes) {
					if (hiddenKeys.indexOf(key) === -1) {
						hiddenKeys.push(key);
					}
				}
			}
		}

		// hidden keys for all types
		for (let key of _Crud.hiddenKeysForAllTypes) {
			if (hiddenKeys.indexOf(key) === -1) {
				hiddenKeys.push(key);
			}
		}

		return hiddenKeys;
	},
	getSortOrderOfColumns: (type) => {
		let sortOrder = LSWrapper.getItem(_Crud.crudSortedColumnsKey + type, '[]');
		return JSON.parse(sortOrder);
	},
	saveSortOrderOfColumns: (type, order) => {

		_Crud.typeColumnSort[type] = order;

		// this also updates hidden keys (inverted!)
		let allPropertiesOfType = Object.keys(_Crud.getCurrentProperties(type));
		let hiddenKeys          = allPropertiesOfType.filter(prop => !order.includes(prop));

		LSWrapper.setItem(_Crud.crudHiddenColumnsKey + type, JSON.stringify(hiddenKeys));
		LSWrapper.setItem(_Crud.crudSortedColumnsKey + type, JSON.stringify(order));
	},
	isPrincipalType: (typeDef) => {
		let cls = 'org.structr.dynamic.Principal';
		return typeDef.className === cls || _Crud.inheritsFromAncestorType(typeDef, cls);
	},
	isFileType: (typeDef) => {
		let cls = 'org.structr.dynamic.AbstractFile';
		return typeDef.className === cls || _Crud.inheritsFromAncestorType(typeDef, cls);
	},
	isImageType: (typeDef) => {
		let cls = 'org.structr.dynamic.Image';
		return typeDef.className === cls || _Crud.inheritsFromAncestorType(typeDef, cls);
	},
	inheritsFromAncestorType: (typeDef, ancestorFQCN) => {

		if (typeDef.extendsClass === ancestorFQCN) {

			return true;

		} else {

			// search parent type
			let parentType = Object.values(_Crud.types).filter(t => (t.className === typeDef.extendsClass));

			if (parentType.length === 1) {
				return _Crud.inheritsFromAncestorType(parentType[0], ancestorFQCN);
			}
		}

		return false;
	},
	filterKeys: (type, sourceArray) => {

		if (!sourceArray) {
			return;
		}

		let sortOrder    = _Crud.getSortOrderOfColumns(type);
		let hiddenKeys   = _Crud.getHiddenKeys(type);
		let filteredKeys = sourceArray.filter(key => !(hiddenKeys.includes(key)));

		if (sortOrder.length > 0) {
			return sortOrder.filter(prop => sourceArray.includes(prop));
		}

		let typePos = filteredKeys.indexOf('type');
		if (typePos !== -1) {
			filteredKeys.splice(typePos, 1);
		}
		filteredKeys.unshift('type');

		let idPos = filteredKeys.indexOf('id');
		if (idPos !== -1) {
			filteredKeys.splice(idPos, 1);
		}
		filteredKeys.unshift('id');

		return filteredKeys;
	},
	toggleColumn: (type, key) => {

		let hiddenKeys = _Crud.getHiddenKeys(type);

		if (hiddenKeys.includes(key)) {

			hiddenKeys.splice(hiddenKeys.indexOf(key), 1);

		} else {

			hiddenKeys.push(key);

			let table = $('#crud-type-detail table');

			// remove column(s) from table
			$(`th.${_Crud.cssClassForKey(key)}`, table).remove();
			$(`td.${_Crud.cssClassForKey(key)}`, table).each(function(i, t) {
				t.remove();
			});
		}

		LSWrapper.setItem(_Crud.crudHiddenColumnsKey + type, JSON.stringify(hiddenKeys));
	},
	serializeObject: function(obj) {
		let o = {};
		var a = obj.serializeArray();
		$.each(a, function() {
			if (this.value && this.value !== '') {
				if (o[this.name]) {
					if (!o[this.name].push) {
						o[this.name] = [o[this.name]];
					}
					o[this.name].push(this.value || '');
				} else {
					o[this.name] = this.value || '';
				}
			}
		});
		return o;
	},
	getSoftLimitedPageCount: (pageSize) => Math.ceil(_Crud.getSoftLimitedResultCount() / pageSize),
	getSoftLimitedResultCount: () => _Crud.resultCountSoftLimit,
	getSoftLimitMessage: () => 'Result count exceeds soft limit (' + _Crud.resultCountSoftLimit + '). Page count may be higher than displayed.',
	showSoftLimitAlert: (el) => {
		el.attr('style', 'background-color: #fc0 !important;');
		el.attr('title', _Crud.getSoftLimitMessage());
	},
	showActualResultCount: (el, pageSize) => {
		el.attr('title', 'Result count = ' + pageSize);
	},
	cssClassForKey: (key) => {
		return '___' + key.replace(/\s/g,  '_whitespace_');
	},

	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/crud.css">

			<div id="crud-main">

				<div class="column-resizer"></div>

				<div id="crud-left" class="resourceBox">

					<div id="crud-types">

						<div class="flex">
							<h2 class="flex-grow">All Types</h2>

							<div id="crudTypeFilterSettings" class="dropdown-menu dropdown-menu-large">

								<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green" id="crudTypesFilterToggle">
									${_Icons.getSvgIcon(_Icons.iconSettingsWrench)}
								</button>

								<div class="dropdown-menu-container" style="width: 17rem;">

									<div class="heading-row">
										<h3>Type Filters</h3>
									</div>

									<div class="row">
										<label class="block"><input ${_Crud.getStoredTypeVisibilityConfig('custom') ? 'checked' : ''} type="checkbox" id="crudTypeToggleCustom"> Custom Types</label>
									</div>
									<div class="row">
										<label class="block"><input ${_Crud.getStoredTypeVisibilityConfig('customRels')   ? 'checked' : ''} type="checkbox" id="crudTypeToggleCustomRels"> Custom Relationship Types</label>
									</div>
									<div class="row">
										<label class="block"><input ${_Crud.getStoredTypeVisibilityConfig('rels')   ? 'checked' : ''} type="checkbox" id="crudTypeToggleRels"> Built-In Relationship Types</label>
									</div>
									<div class="row">
										<label class="block"><input ${_Crud.getStoredTypeVisibilityConfig('core')   ? 'checked' : ''} type="checkbox" id="crudTypeToggleCore"> Core Types</label>
									</div>
									<div class="row">
										<label class="block"><input ${_Crud.getStoredTypeVisibilityConfig('html')   ? 'checked' : ''} type="checkbox" id="crudTypeToggleHtml"> HTML Types</label>
									</div>
									<div class="row">
										<label class="block"><input ${_Crud.getStoredTypeVisibilityConfig('ui')     ? 'checked' : ''} type="checkbox" id="crudTypeToggleUi"> UI Types</label>
									</div>
									<div class="row">
										<label class="block"><input ${_Crud.getStoredTypeVisibilityConfig('log')    ? 'checked' : ''} type="checkbox" id="crudTypeToggleLog"> Log Types</label>
									</div>
									<div class="row mb-2">
										<label class="block"><input ${_Crud.getStoredTypeVisibilityConfig('other')  ? 'checked' : ''} type="checkbox" id="crudTypeToggleOther"> Other Types</label>
									</div>

								</div>
							</div>
						</div>

						<input placeholder="Filter types..." id="crudTypesSearch" autocomplete="off">

						<div id="crud-types-list"></div>
					</div>

					<div id="crud-recent-types">
						<h2>Recent</h2>

						<div id="crud-recent-types-list"></div>
					</div>

				</div>

				<div id="crud-type-detail" class="resourceBox"></div>

			</div>
		`,
		functions: config => `
			<div id="crud-buttons" class="flex-grow"></div>

			<div class="searchBox">
				<input id="crud-search-box" class="search" name="crud-search" placeholder="Search">
				${_Icons.getSvgIcon(_Icons.iconCrossIcon, 12, 12, _Icons.getSvgIconClassesForColoredIcon(['clearSearchIcon', 'icon-lightgrey', 'cursor-pointer']), 'Clear Search')}
			</div>
		`,
		typeButtons: config => `
			<div id="crud-buttons" class="flex items-center">
				<button class="action inline-flex items-center" id="create${config.type}">
					${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['mr-2'])} Create new ${config.type}
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
				<label for="exact_type_${config.type}" class="exact-type-checkbox-label"><input id="exact_type_${config.type}" class="exact-type-checkbox" type="checkbox"> Exclude subtypes</label>
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