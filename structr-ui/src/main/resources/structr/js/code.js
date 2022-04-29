/*
 * Copyright (C) 2010-2022 Structr GmbH
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
	Structr.registerModule(_Code);
});

let _Code = {
	_moduleName: 'code',
	codeMain: undefined,
	codeTree: undefined,
	codeContents: undefined,
	codeContext: undefined,
	pathLocationStack: [],
	pathLocationIndex: 0,
	searchThreshold: 3,
	searchTextLength: 0,
	lastClickedPath: '',
	layouter: null,
	seed: 42,
	availableTags: [],
	tagBlacklist: ['core', 'ui', 'html'],       // don't show internal tags (core, ui, html)
	codeRecentElementsKey: 'structrCodeRecentElements_' + location.port,
	codeLastOpenMethodKey: 'structrCodeLastOpenMethod_' + location.port,
	codeResizerLeftKey: 'structrCodeResizerLeftKey_' + location.port,
	codeResizerRightKey: 'structrCodeResizerRightKey_' + location.port,
	additionalDirtyChecks: [],
	methodNamesWithoutOpenAPITab: ['onCreate', 'onSave', 'onDelete', 'afterCreate'],
	defaultPageSize: 10000,
	defaultPage: 1,
	
	init: function() {

		Structr.makePagesMenuDroppable();
		Structr.adaptUiToAvailableFeatures();

		$(window).off('keydown', _Code.handleKeyDownEvent).on('keydown', _Code.handleKeyDownEvent);
	},
	beforeunloadHandler: () => {
		if (_Code.isDirty()) {
			return 'There are unsaved changes - discard changes?';
		}
	},
	resize: function() {

		requestAnimationFrame(() => {
			_Code.updatedResizers();
			Structr.resize();
		});
	},
	moveLeftResizer: (left) => {
		requestAnimationFrame(() => {
			left = left || LSWrapper.getItem(_Code.codeResizerLeftKey) || 300;
			_Code.updatedResizers(left, null);
		});
	},
	moveRightResizer: (left) => {
		requestAnimationFrame(() => {
			left = left || LSWrapper.getItem(_Code.codeResizerRightKey) || 240;
			_Code.updatedResizers(null, left);
		});
	},
	updatedResizers: (left, right) => {
		left  = left || LSWrapper.getItem(_Code.codeResizerLeftKey) || 300;
		right = right || LSWrapper.getItem(_Code.codeResizerRightKey) || 240;

		_Code.codeMain[0].querySelector('.column-resizer-left').style.left     = left + 'px';
		_Code.codeMain[0].querySelector('.column-resizer-right').style.left    = window.innerWidth - right + 'px';

		document.getElementById('code-tree').style.width              = 'calc(' + left + 'px - 1rem)';
		document.getElementById('code-context-container').style.width = 'calc(' + right + 'px - 3rem)';
		document.getElementById('code-contents').style.width          = 'calc(' + (window.innerWidth - left - right) + 'px - 4rem)';

		_Editors.resizeVisibleEditors();
	},
	onload: function() {

		Command.query('SchemaNode', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', null, _Code.addAvailableTagsForEntities, false, null, 'tags');
		Command.query('SchemaMethod', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', null, _Code.addAvailableTagsForEntities, false, null, 'tags');

		Structr.functionBar.innerHTML = _Code.templates.functions();

		UISettings.showSettingsForCurrentModule();

		$('#tree-search-input').on('input', _Code.debounce(_Code.doSearch, 300));
		$('#tree-forward-button').on('click', _Code.pathLocationForward);
		$('#tree-back-button').on('click', _Code.pathLocationBackward);
		$('#cancel-search-button').on('click', _Code.cancelSearch);

		$(window).on('keydown.search', function(e) {
			if (_Code.searchIsActive()) {
				if (e.key === 'Escape') {
					_Code.cancelSearch();
				}
			};
		});

		Structr.mainContainer.innerHTML = _Code.templates.main();

		_Code.init();

		_Code.codeMain     = $('#code-main');
		_Code.codeTree     = $('#code-tree');
		_Code.codeContents = $('#code-contents');
		_Code.codeContext  = $('#code-context');

		Structr.initVerticalSlider($('.column-resizer-left', _Code.codeMain), _Code.codeResizerLeftKey, 204, _Code.moveLeftResizer);
		Structr.initVerticalSlider($('.column-resizer-right', _Code.codeMain), _Code.codeResizerRightKey, 204, _Code.moveRightResizer, true);

		$.jstree.defaults.core.themes.dots      = false;
		$.jstree.defaults.dnd.inside_pos        = 'last';
		$.jstree.defaults.dnd.large_drop_target = true;

		_Code.codeTree.on('select_node.jstree', _Code.handleTreeClick);
		_Code.codeTree.on('refresh.jstree', _Code.activateLastClicked);

		_Code.loadRecentlyUsedElements(() => {
			_TreeHelper.initTree(_Code.codeTree, _Code.treeInitFunction, 'structr-ui-code');
		});

		$(window).off('resize').resize(() => {
			_Code.resize();
		});

		Structr.unblockMenu(100);

		_Code.resize();
		Structr.adaptUiToAvailableFeatures();
	},
	addAvailableTagsForEntities: (entities) => {

		for (let entity of entities) {

			if (entity.tags) {

				for (let tag of entity.tags) {

					if (!_Code.availableTags.includes(tag) && !_Code.tagBlacklist.includes(tag)) {
						_Code.availableTags.push(tag);
					}
				}
			}
		}

		_Code.availableTags.sort();
	},
	handleKeyDownEvent: function(e) {

		if (Structr.isModuleActive(_Code)) {

			// This hack prevents FF from closing WS connections on ESC
			if (e.keyCode === 27) {
				e.preventDefault();
			}
			var k = e.which;
			if (k === 16) {
				shiftKey = true;
			}
			if (k === 18) {
				altKey = true;
			}
			if (k === 17) {
				ctrlKey = true;
			}
			if (k === 69) {
				eKey = true;
			}

			let cmdKey = (navigator.platform === 'MacIntel' && e.metaKey);

			// ctrl-s / cmd-s
			if (k === 83 && ((navigator.platform !== 'MacIntel' && e.ctrlKey) || (navigator.platform === 'MacIntel' && cmdKey))) {
				e.preventDefault();
				_Code.runCurrentEntitySaveAction();
			}
			// ctrl-u / cmd-u
			if (k === 85 && ((navigator.platform !== 'MacIntel' && e.ctrlKey) || (navigator.platform === 'MacIntel' && cmdKey))) {
				e.preventDefault();
				_Code.showGeneratedSource();
			}
		}
	},
	forceNotDirty: () => {
		_Code.codeContents.find('.to-delete').removeClass('to-delete');
		_Code.codeContents.find('.has-changes').removeClass('has-changes');

		_Code.additionalDirtyChecks = [];
	},
	isDirty: () => {
		let isDirty = false;
		if (_Code.codeContents) {
			isDirty = (_Code.codeContents.find('.to-delete').length + _Code.codeContents.find('.has-changes').length) > 0;
		}
		return isDirty;
	},
	updateDirtyFlag: (entity) => {

		let formContent = _Code.collectChangedPropertyData(entity);
		let dirty       = Object.keys(formContent).length > 0;

		for (let additionalCheck of _Code.additionalDirtyChecks) {
			dirty = dirty || additionalCheck();
		}

		if (dirty) {
			$('#action-button-save').removeClass('disabled').attr('disabled', null);
			$('#action-button-cancel').removeClass('disabled').attr('disabled', null);
		} else {
			$('#action-button-save').addClass('disabled').attr('disabled', 'disabled');
			$('#action-button-cancel').addClass('disabled').attr('disabled', 'disabled');
		}

		if (dirty === true) {
			_Code.codeContents.children().first().addClass('has-changes');
		} else {
			_Code.codeContents.children().first().removeClass('has-changes');
		}
	},
	testAllowNavigation: () => {
		if (_Code.isDirty()) {
			return confirm('Discard unsaved changes?');
		}

		return true;
	},
	collectPropertyData: (entity) => {

		let propertyData = {};

		for (let p of document.querySelectorAll('#code-contents input[data-property]')) {
			switch (p.type) {
				case "checkbox":
					propertyData[p.dataset.property] = p.checked;
					break;
				case "number":
					if (p.value) {
						propertyData[p.dataset.property] = parseInt(p.value);
					} else {
						propertyData[p.dataset.property] = null;
					}
					break;
				case "text":
				default:
					if (p.value) {
						propertyData[p.dataset.property] = p.value;
					} else {
						propertyData[p.dataset.property] = null;
					}
					break;
			}
		}

		for (let p of document.querySelectorAll('#code-contents select[data-property]')) {
			if (p.multiple === true) {
				propertyData[p.dataset.property] = Array.prototype.map.call(p.selectedOptions, (o) => o.value);
			} else {
				propertyData[p.dataset.property] = p.value;
			}
		}

		for (let editorWrapper of document.querySelectorAll('#code-contents .editor[data-property]')) {
			let propertyName = editorWrapper.dataset.property;

			propertyData[propertyName] = _Editors.getTextForExistingEditor(entity.id, propertyName);
		}

		return propertyData;
	},
	collectChangedPropertyData: (entity) => {

		let formContent = _Code.collectPropertyData(entity);
		let keys        = Object.keys(formContent);

		// remove unchanged keys
		for (let key of keys) {

			if ( (formContent[key] === entity[key]) || (!formContent[key] && entity[key] === "") || (formContent[key] === "" && !entity[key]) || (!formContent[key] && !entity[key])) {
				delete formContent[key];
			}

			if (Array.isArray(formContent[key])) {

				let compareSource = entity[key];

				if (key === 'tags' && compareSource) {
					// remove blacklisted tags from source for comparison
					compareSource = compareSource.filter((tag) => { return !_Code.tagBlacklist.includes(tag); });
				}

				if (formContent[key].length === 0 && (!compareSource || compareSource.length === 0)) {

					delete formContent[key];

				} else if (compareSource && compareSource.length === formContent[key].length) {

					// check if same
					let diff = formContent[key].filter((v) => {
						return !compareSource.includes(v);
					});
					if (diff.length === 0) {
						delete formContent[key];
					}
				}
			}
		}

		return formContent;
	},
	revertFormData: function(entity) {

		for (let p of document.querySelectorAll('#code-contents input[data-property]')) {
			switch (p.type) {
				case "checkbox":
					p.checked = entity[p.dataset.property];
					break;
				case "number":
					p.value = entity[p.dataset.property];
					break;
				case "text":
				default:
					p.value = entity[p.dataset.property] || '';
					break;
			}
		}

		for (let p of document.querySelectorAll('#code-contents select[data-property]')) {
			for (let option of p.options) {
				option.selected = (entity[p.dataset.property] && entity[p.dataset.property].includes(option.value));
			}

			p.dispatchEvent(new Event('change'));
		}

		for (let editorWrapper of document.querySelectorAll('#code-contents .editor[data-property]')) {
			let propertyName = editorWrapper.dataset.property;

			_Editors.setTextForExistingEditor(entity.id, propertyName, (entity[propertyName] || ''));
		}

		_Code.updateDirtyFlag(entity);
	},
	isCompileRequiredForSave: function (changes) {

		let compileRequired = false;

		for (let key of Object.keys(changes)) {
			compileRequired = compileRequired || _Code.compileRequiredForKey(key);
		}

		return compileRequired;
	},
	compileRequiredForKey: function(key) {

		let element = _Code.getElementForKey(key);
		if (element && element.dataset.recompile === "false") {
			return false;
		}

		return true;
	},
	getElementForKey: function (key) {

		let input = document.querySelector('#code-contents input[data-property=' + key + ']');
		if (input) {

			return input;

		} else {
			let editor = document.querySelector('#code-contents div.editor[data-property=' + key + ']');
			if (editor) {

				return editor;
			}
		}

		return null;
	},
	showSaveAction: function(changes) {

		for (let key of Object.keys(changes)) {
			let element = _Code.getElementForKey(key);
			if (element) {

				if (element.tagName === 'INPUT' && element.type === 'text' && !element.classList.contains('hidden')) {
					blinkGreen($(element));
				} else if (element.tagName === 'INPUT' && element.type === 'checkbox' && !element.classList.contains('hidden')) {
					blinkGreen($(element.closest('.checkbox')));
				} else {
					blinkGreen($(element.closest('.property-options-group')));
				}
			}
		}
	},
	runCurrentEntitySaveAction: function () {
		// this is the default action - it should always be overwritten by specific save actions and is only here to prevent errors
		if (_Code.isDirty()) {
			new MessageBuilder().warning('No save action is defined - but the editor is dirty!').requiresConfirmation().show();
		}
	},
	saveEntityAction: function(entity, callback, optionalFormDataModificationFunctions = []) {

		if (_Code.isDirty()) {

			let formData        = _Code.collectChangedPropertyData(entity);

			for (let modFn of optionalFormDataModificationFunctions) {
				modFn(formData);
			}

			let compileRequired = _Code.isCompileRequiredForSave(formData);

			if (compileRequired) {
				_Code.showSchemaRecompileMessage();
			}

			Command.setProperties(entity.id, formData, function() {

				_Code.addAvailableTagsForEntities([formData]);

				Object.assign(entity, formData);
				_Code.updateDirtyFlag(entity);

				_Code.showSaveAction(formData);

				if (formData.name) {
					_Code.refreshTree();
				}

				if (compileRequired) {
					_Code.hideSchemaRecompileMessage();
				}

				if (typeof callback === 'function') {
					callback();
				}
			});
		}
	},
	loadRecentlyUsedElements: function(doneCallback) {

		let recentElements = LSWrapper.getItem(_Code.codeRecentElementsKey) || [];

		for (let element of recentElements) {
			if (element.name !== undefined) {
				_Code.addRecentlyUsedElement(element.id, element.name, element.iconClass, element.path, true);
			}
		}

		doneCallback();
	},
	addRecentlyUsedEntity: function(entity, path, fromStorage) {

		let name      = _Code.getDisplayNameInRecentsForType(entity);
		let iconClass = 'fa fa-' + _Code.getIconForNodeType(entity);
		let localPath = path;

		if (localPath.indexOf('searchresults-') === 0) {
			localPath = _Code.removeSearchResultsPartFromPath(localPath, entity);
		}

		_Code.addRecentlyUsedElement(entity.id, name, iconClass, localPath, fromStorage);
	},
	addRecentlyUsedElement: function(id, name, iconClass, path, fromStorage) {

		if (!fromStorage) {

			let recentElements = LSWrapper.getItem(_Code.codeRecentElementsKey) || [];
			let updatedList    = recentElements.filter((recentElement) => { return (recentElement.id !== id); });
			updatedList.unshift({ id: id, name: name, iconClass: iconClass, path: path });

			// keep list at length 10
			while (updatedList.length > 10) {

				let toRemove = updatedList.pop();
				$('#recently-used-' + toRemove.id).remove();
			}

			$('#recently-used-' + id).remove();

			LSWrapper.setItem(_Code.codeRecentElementsKey, updatedList);
		}

		let recentlyUsedButton = $(_Code.templates.recentlyUsedButton({ id: id, name: name, iconClass: iconClass }));

		if (fromStorage) {
			_Code.codeContext.append(recentlyUsedButton);
		} else {
			_Code.codeContext.prepend(recentlyUsedButton);
		}

		recentlyUsedButton.on('click.recently-used', () => {
			_Code.findAndOpenNode(path, true);
		});
		$('.remove-recently-used', recentlyUsedButton).on('click.recently-used', (e) => {
			e.stopPropagation();
			_Code.deleteRecentlyUsedElement(id);
		});

	},
	deleteRecentlyUsedElement: (recentlyUsedElementId) => {

		let recentElements = LSWrapper.getItem(_Code.codeRecentElementsKey) || [];

		let filteredRecentElements = recentElements.filter((recentElement) => {
			return (recentElement.id !== recentlyUsedElementId);
		});
		$('#recently-used-' + recentlyUsedElementId).remove();

		LSWrapper.setItem(_Code.codeRecentElementsKey, filteredRecentElements);
	},
	removeSearchResultsPartFromPath: (path, entity) => {

		let parts = path.split('-');

		// we need to change tree paths that start with searchresults-
		switch (entity.type) {

			case 'SchemaNode':
				if (entity.isBuiltinType) {
					parts[0] = 'builtin';
				} else {
					parts[0] = 'custom';
				}
				break;

			case 'SchemaProperty':
			case 'SchemaView':
				if (entity.schemaNode) {
					if (entity.schemaNode.isBuiltinType) {
						parts[0] = 'builtin';
					} else {
						parts[0] = 'custom';
					}
				} else {
					console.log('Missing schemaNode for ' + entity.id);
				}
				break;

			case 'SchemaMethod':
				if (entity.schemaNode) {
					if (entity.schemaNode.isBuiltinType) {
						parts[0] = 'builtin';
					} else {
						parts[0] = 'custom';
					}
				} else {
					parts[0] = 'globals';
				}
				break;

			default:
				console.log('Unhandled entity type ' + entity.type);
				break;
		}

		return parts.join('-');
	},
	createFullPath: function(entity) {

		var parts = [];

		// we need to change tree paths that start with searchresults-
		switch (entity.type) {

			case 'SchemaNode':
				parts.push('');
				parts.push(entity.id);
				break;

			case 'SchemaProperty':
				if (entity.schemaNode) {
					parts.push('');
					parts.push(entity.schemaNode.id);
					parts.push('properties');
					parts.push(entity.id);
				} else {
					console.log('Missing schemaNode for ' + entity.id);
				}
				break;

			case 'SchemaView':
				if (entity.schemaNode) {
					parts.push('');
					parts.push(entity.schemaNode.id);
					parts.push('views');
					parts.push(entity.id);
				} else {
					console.log('Missing schemaNode for ' + entity.id);
				}
				break;

			case 'SchemaMethod':
				if (entity.schemaNode) {
					parts.push('');
					parts.push(entity.schemaNode.id);
					parts.push('methods');
					parts.push(entity.id);
				} else {
					parts.push('globals');
				}
				break;

			default:
				console.log('Unhandled entity type ' + entity.type);
				break;
		}

		return parts.join('-');
	},
	refreshTree: () => {
		_TreeHelper.refreshTree(_Code.codeTree);
	},
	treeInitFunction: (obj, callback) => {

		switch (obj.id) {

			case '#':

				let defaultEntries = [
					{
						id: 'globals-',
						text: 'Global Methods',
						children: true,
						icon: _Icons.world_icon
					},
					{
						id: 'root',
						text: 'Types',
						children: [
							{ id: 'custom-',     text: 'Custom', children: true, icon: _Icons.folder_icon },
							{ id: 'builtin-',    text: 'Built-In', children: true, icon: _Icons.folder_icon },
							{ id: 'workingsets', text: 'Working Sets', children: true, icon: _Icons.folder_star_icon }
						],
						icon: _Icons.structr_logo_small,
						path: '/',
						state: {
							opened: true
						}
					}
				];

				if (_Code.searchIsActive()) {

					defaultEntries.unshift({
						id: 'searchresults-',
						text: 'Search Results',
						children: true,
						icon: 'fa fa-search',
						state: {
							opened: true
						}
					});
				}

				callback(defaultEntries);
				break;

			case 'root':
				_Code.load(null, callback);
				break;

			default:
				_Code.load(obj, callback);
				break;
		}

	},
	unload: function() {

		let allow = _Code.testAllowNavigation();
		if (allow) {

			_Editors.disposeAllEditors();

			fastRemoveAllChildren($('.searchBox'));
			fastRemoveAllChildren($('#code-main'));
		}

		return allow;
	},
	displayFunction: (result, identifier, dontSort, isSearch) => {

		let list = [];

		for (let entity of result) {

			// skip HTML entities
			if (entity?.category !== 'html') {

				let icon     = _Code.getIconForNodeType(entity);
				let treeId = identifier.source + '-' + entity.id;

				switch (entity.type) {

					case 'SchemaGroup': {

						list.push({
							id: treeId,
							text:  entity.name,
							children: entity.children.length > 0,
							icon: entity.name === _WorkingSets.recentlyUsedName ? _Icons.clock_icon : _Icons.folder_icon,
							data: {
								type: entity.type,
								name: entity.name
							}
						});

						break;
					}

					case 'SchemaNode': {

						let data = {
							type: entity.name,
							name: entity.name
						};

						let children = [
							{
								id: treeId + '-properties',
								text: 'Local Properties',
								children: entity.schemaProperties.length > 0,
								icon: 'fa fa-sliders gray',
								data: data
							},
							{
								id: treeId + '-remoteproperties',
								text: 'Related Properties',
								children: ((entity.relatedTo.length + entity.relatedFrom.length) > 0),
								icon: 'fa fa-sliders gray',
								data: data
							},
							{
								id: treeId + '-views',
								text: 'Views',
								children: entity.schemaViews.length > 0,
								icon: 'fa fa-television gray',
								data: data
							},
							{
								id: treeId + '-methods',
								text: 'Methods',
								children: _Schema.filterJavaMethods(entity.schemaMethods).length > 0,
								icon: 'fa fa-code gray',
								data: data
							},
							{
								id: treeId + '-inheritedproperties',
								text: 'Inherited Properties',
								children: false,
								icon: 'fa fa-sliders gray',
								data: data
							}
						];

						list.push({
							id: treeId,
							text:  entity.name,
							children: children,
							icon: 'fa fa-' + icon,
							data: {
								type: entity.type,
								name: entity.name
							}
						});

						break;
					}

					case 'SchemaRelationshipNode': {

						let name = entity.name || '[unnamed]';
						let listItemAttributes = {};

						treeId = identifier.source + '-' + entity.id + '-' + name;

						list.push({
							id: treeId,
							text:  name,
							children: false,
							icon: 'fa fa-' + icon,
							li_attr: listItemAttributes,
							data: {
								type: entity.type,
								name: entity.name,
								entity: entity
							}
						});

						break;
					}

					default: {

						let name = entity.name || '[unnamed]';

						if (isSearch && entity.schemaNode) {
							name = entity.schemaNode.name + '.' + name;
							treeId = 'searchresults-' + _Code.createFullPath(entity);
						}

						if (entity.inherited) {

							list.push({
								id: treeId,
								text:  name + (' (' + (entity.propertyType || '') + ')'),
								children: false,
								icon: 'fa fa-' + icon,
								li_attr: {
									style: 'color: #aaa;'
								},
								data: {
									type: entity.type,
									name: entity.name
								}
							});

						} else {

							let hasVisibleChildren = _Code.hasVisibleChildren(identifier.root, entity);
							let listItemAttributes = {};

							if (entity.type === 'SchemaMethod') {
								name = name + '()';
							}

							if (entity.type === 'SchemaProperty') {
								name = name + ' (' + entity.propertyType + ')';
							}

							list.push({
								id: treeId,
								text:  name,
								children: hasVisibleChildren,
								icon: 'fa fa-' + icon,
								li_attr: listItemAttributes,
								data: {
									type: entity.type,
									name: entity.name
								}
							});
						}

						break;
					}
				}
			}
		}

		if (!dontSort) {

			list.sort((a, b) => {
				if (a.text > b.text) { return 1; }
				if (a.text < b.text) { return -1; }
				return 0;
			});
		}

		identifier.callback(list);
	},
	load: (obj, callback) => {

		let identifier = _Code.splitIdentifier(obj);

		identifier.callback = callback;

		switch (identifier.root) {

			case 'searchresults':
				if (identifier.memberCollection) {
				       _Code.loadTypeMembers(identifier);
			       } else if (identifier.typeId) {
					_Code.loadType(identifier);
				} else {
					_Code.loadSearchResults(identifier);
				}
				break;

			case 'globals':
				_Code.loadGlobalSchemaMethods(identifier);
				break;

			case 'custom':
				_Code.loadCustomTypes(identifier);
				break;

			case 'builtin':
				_Code.loadBuiltInTypes(identifier);
				break;

			case 'workingsets':
				_Code.loadWorkingSets(identifier);
				break;
		}
	},
	loadSearchResults: (identifier) => {

		let text          = $('#tree-search-input').val();
		let searchResults = {};
		let count         = 0;
		let collectFunction = (result) => {
			for (let r of result) {
				searchResults[r.id] = r;
			}

			// only show results after all 6 searches are finished (to prevent duplicates)
			if (++count === 6) {
				_Code.displayFunction(Object.values(searchResults), identifier, false, true);
			}
		};

		let parts = text.split('.');

		if (parts.length === 2 && parts[1].trim() !== '') {

			let handleExactSchemaNodeSearch = (result) => {

				if (result.length === 0) {
					// because we will not find methods/properties if no schema node was found via exact search
					count += 2;
				}

				collectFunction(result);

				for (let schemaNode of result) {
					// should yield at max one hit because we are using exact search

					let matchingMethods = [];

					for (let method of _Schema.filterJavaMethods(schemaNode.schemaMethods)) {
						if (method.name.indexOf(parts[1]) === 0) {

							// populate backRef to schemaNode because it only contains id by default
							method.schemaNode = schemaNode;

							matchingMethods.push(method);
						}
					}
					collectFunction(matchingMethods);

					let matchingProperties = [];
					for (let property of schemaNode.schemaProperties) {
						if (property.name.indexOf(parts[1]) === 0) {

							// populate backRef to schemaNode because it only contains id by default
							property.schemaNode = schemaNode;

							matchingProperties.push(property);
						}
					}
					collectFunction(matchingProperties);
				}
			};

			Command.query('SchemaNode',     _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { name: parts[0] }, handleExactSchemaNodeSearch, true);

		} else {

			Command.query('SchemaNode',     _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { name: text }, collectFunction, false);
			Command.query('SchemaProperty', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { name: text }, collectFunction, false);
			Command.query('SchemaMethod',   _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { name: text }, collectFunction, false);
		}

		// text search always happens
		Command.query('SchemaMethod',   _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { source: text}, collectFunction, false);
		Command.query('SchemaProperty', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { writeFunction: text}, collectFunction, false);
		Command.query('SchemaProperty', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { readFunction: text}, collectFunction, false);

	},
	loadGlobalSchemaMethods: (identifier) => {
		if (identifier.typeId) {
			_Code.loadType(identifier);
		} else {
			Command.query('SchemaMethod', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', {schemaNode: null}, result => _Code.displayFunction(result, identifier), true, 'ui');
		}
	},
	loadCustomTypes: (identifier) => {
		if (identifier.typeId) {
			_Code.loadType(identifier);
		} else {
			Command.query('SchemaNode', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { isBuiltinType: false}, result => _Code.displayFunction(result, identifier), true);
		}
	},
	loadWorkingSets: (identifier) => {
		if (identifier.typeId) {
			_Code.loadType(identifier);
		} else if (identifier.workingSetId) {
			_WorkingSets.getWorkingSetContents(identifier.workingSetId, result => _Code.displayFunction(result, identifier));
		} else {
			_WorkingSets.getWorkingSets(result => _Code.displayFunction(result, identifier, true));
		}
	},
	loadBuiltInTypes: (identifier) => {
		if (identifier.typeId) {
			_Code.loadType(identifier);
		} else {
			Command.query('SchemaNode', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { isBuiltinType: true }, result => _Code.displayFunction(result, identifier), false);
		}
	},
	loadType: function(identifier) {
	       if (identifier.memberCollection) {
		       _Code.loadTypeMembers(identifier);
	       } else {
			Command.query('SchemaMethod', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', {schemaNode: identifier.typeId}, function(result) {
				_Code.displayFunction(result, identifier);
			}, true, 'ui');
	       }
	},
	loadTypeMembers: (identifier) => {

		switch (identifier.memberCollection) {

			case 'properties':
				_Code.loadLocalProperties(identifier);
				break;

			case 'remoteproperties':
				_Code.loadRemoteProperties(identifier);
				break;

			case 'views':
				_Code.loadViews(identifier);
				break;

			case 'methods':
				_Code.loadMethods(identifier);
				break;

			// case 'inheritedproperties':
			// 	_Code.loadInheritedProperties(identifier);
			// 	break;
		}
	},
	loadLocalProperties: (identifier) => {
		if (identifier.memberId) {
		} else {
			Command.query('SchemaProperty', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { schemaNode: identifier.typeId }, function(result) {
				_Code.displayFunction(result, identifier);
			}, true, 'ui');
		}
	},
	loadRemoteProperties: function(identifier) {

		if (identifier.memberId) {
			// hm?
		} else {

			Command.get(identifier.typeId, null, entity => {

				let mapFn = (rel, out) => {
					let attrName = (out ? (rel.targetJsonName || rel.oldTargetJsonName) : (rel.sourceJsonName || rel.oldSourceJsonName));

					return {
						id: rel.id,
						type: rel.type,
						name: attrName,
						propertyType: '',
						inherited: false
					};
				};

				let processedRemoteAttributes = [].concat(entity.relatedTo.map(r => mapFn(r, true))).concat(entity.relatedFrom.map((r) => mapFn(r, false)));

				_Code.displayFunction(processedRemoteAttributes, identifier);
			});
		}
	},
	loadViews: function(identifier) {

		Command.query('SchemaView', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', {schemaNode: identifier.typeId }, function(result) {
			_Code.displayFunction(result, identifier);
		}, true, 'ui');
	},
	loadMethods: function(identifier) {

		Command.query('SchemaMethod', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', {schemaNode: identifier.typeId }, function(result) {
			_Code.displayFunction(_Schema.filterJavaMethods(result), identifier);
		}, true, 'ui');
	},
	// loadInheritedProperties: function(identifier) {
	//
	// 	Command.listSchemaProperties(identifier.typeId, 'custom', function(result) {
	//
	// 		let filtered = result.filter(function(p) {
	// 			return p.declaringClass !== identifier.obj.data.type;
	// 		});
	//
	// 		_Code.displayFunction(filtered.map(function(s) {
	//
	// 			return {
	// 				id: s.declaringUuid + '-' + s.name,
	// 				type: 'SchemaProperty',
	// 				name: s.declaringClass + '.' + s.name,
	// 				propertyType: s.declaringPropertyType ? s.declaringPropertyType : s.propertyType,
	// 				inherited: true
	// 			};
	// 		}), identifier);
	// 	});
	// },
	splitIdentifier: function(obj) {

		let parts = obj.id.split('-');

		let identifier = {
			source: obj.id,
			obj: obj
		};

		if (parts.length) { identifier.root             = parts[0]; parts = parts.slice(1); }
		if (parts.length) { identifier.workingSetId     = parts[0]; parts = parts.slice(1); }
		if (parts.length) { identifier.typeId           = parts[0]; parts = parts.slice(1); }
		if (parts.length) { identifier.memberCollection = parts[0]; parts = parts.slice(1); }
		if (parts.length) { identifier.memberId         = parts[0]; parts = parts.slice(1); }

		return identifier;
	},
	clearMainArea: function() {
		fastRemoveAllChildren(_Code.codeContents[0]);
		fastRemoveAllChildren($('#code-button-container')[0]);
	},
	getDisplayNameInRecentsForType: function (entity) {

		let displayName = entity.name;

		switch (entity.type) {
			case 'SchemaNode':
				displayName = 'Type ' + entity.name;
				break;

			case 'SchemaMethod':
				if (entity.schemaNode && entity.schemaNode.name) {
					displayName = entity.schemaNode.name + '.' + entity.name + '()';
				} else {
					displayName = entity.name + '()';
				}
				break;

			case 'SchemaProperty':
				if (entity.schemaNode && entity.schemaNode.name) {
					displayName = entity.schemaNode.name + '.' + entity.name;
				}
				break;
		}

		return displayName;
	},
	getIconForNodeType: function(entity) {

		let icon = 'file-code-o gray';

		switch (entity.type) {

			case 'SchemaMethod':

				switch (entity.codeType) {
					case 'java':
						icon = 'dot-circle-o red';
						break;
					default:
						icon = 'circle-o blue';
						break;
				}
				break;

			case 'SchemaProperty':
				return _Code.getIconForPropertyType(entity.propertyType);

			case 'SchemaView':
				return 'th-large';

			case 'SchemaRelationshipNode':
				return 'chain';
		}

		return icon;
	},
	getIconForPropertyType: function(propertyType) {

		let icon = 'exclamation-triangle';

		switch (propertyType) {

			case "Custom":
			case "IdNotion":
			case "Notion":
				icon = 'magic';
				break;

			case "IntegerArray":
			case "StringArray":
				icon = 'magic';
				break;

			case 'Boolean':      icon = 'check'; break;
			case "Cypher":       icon = 'database'; break;
			case 'Date':         icon = 'calendar'; break;
			case "Double":       icon = 'superscript'; break;
			case "Enum":         icon = 'list'; break;
			case "Function":     icon = 'coffee'; break;
			case 'Integer':      icon = 'calculator'; break;
			case "Long":         icon = 'calculator'; break;
			case 'String':       icon = 'pencil-square-o'; break;
			case 'Encrypted':    icon = 'lock'; break;
			default:             icon = 'chain';
		}

		return icon;
	},
	hasVisibleChildren: function(id, entity) {

		var hasVisibleChildren = false;

		if (entity.schemaMethods) {

			_Schema.filterJavaMethods(entity.schemaMethods).forEach(function(m) {

				if (id === 'custom' || !m.isPartOfBuiltInSchema) {

					hasVisibleChildren = true;
				}
			});
		}

		return hasVisibleChildren;
	},
	handleTreeClick: function(evt, data) {

		if (data && data.node && data.node.id) {

			let selection = {
				id: data.node.id,
				updateLocationStack: true,
				nodeData: data.node.data
			};

			if (data.node.data) {
				selection.type = data.node.data.type;
			}

			if (data && data.event && data.event.updateLocationStack === false) {
				selection.updateLocationStack = false;
			}

			_Code.handleSelection(selection);
		}
	},
	handleSelection: (data) => {

		if (_Code.testAllowNavigation()) {

			_Code.dirty = false;
			_Code.additionalDirtyChecks = [];

			// clear page
			_Code.clearMainArea();

			let identifier = _Code.splitIdentifier(data);

			// the order of the checks is important: member id first, then member collection, then type, then root
			if (identifier.memberId) {

				switch (identifier.memberId) {

					case 'searchresults':
					case 'undefined':
					case 'null':
						break;

					default:
						_Code.handleNodeObjectClick(data);
						break;
				}


			} else if (identifier.memberCollection) {

				switch (identifier.memberCollection) {

					case 'properties':
						_Code.displayPropertiesContent(identifier, data.updateLocationStack);
						break;

					case 'remoteproperties':
						_Code.displayRemotePropertiesContent(identifier, data.updateLocationStack);
						break;

					case 'views':
						_Code.displayViewsContent(identifier, data.updateLocationStack);
						break;

					case 'methods':
						_Code.displayMethodsContent(identifier, data.updateLocationStack);
						break;

					case 'inheritedproperties':
						_Code.displayInheritedPropertiesContent(identifier, data.updateLocationStack);
						break;

					// case 'inherited':
					// 	_Code.findAndOpenNode(identifier.source, true);
					// 	break;
				}

			} else if (identifier.typeId) {

				_Code.handleNodeObjectClick(data);

			} else if (identifier.workingSetId) {

				_Code.displaySchemaGroupContent(data, identifier);

			} else {

				switch (identifier.root) {

					case 'root':
						_Code.displayRootContent();
						break;

					case 'globals':
						_Code.displayGlobalMethodsContent(identifier, true);
						break;

					case 'custom':
						_Code.displayCustomTypesContent();
						break;

					case 'builtin':
						_Code.displayBuiltInTypesContent(data.type);
						break;

					case 'workingsets':
						_Code.displayWorkingSetsContent();
						break;
				}
			}
		}
	},
	handleNodeObjectClick: function(data) {

		let identifier = _Code.splitIdentifier(data);

		if (data.type) {

			switch (data.type) {

				case 'SchemaView':
					_Code.displayViewDetails(data, identifier);
					break;

				case 'SchemaProperty':
					_Code.displayPropertyDetails(data, identifier);
					break;

				case 'SchemaMethod':
					_Code.displaySchemaMethodContent(data);
					break;

				case 'SchemaNode':
					_Code.displaySchemaNodeContent(data, identifier);
					break;

				case 'SchemaGroup':
					_Code.displaySchemaGroupContent(data, identifier);
					break;

				case 'SchemaRelationshipNode':
					_Code.displaySchemaRelationshipNodeContent(data, identifier);
					break;
			}
		}
	},
	displaySchemaNodeContent: function(data, identifier) {

		fetch(Structr.rootUrl + identifier.typeId + '/schema').then(function(response) {

			if (response.ok) {
				return response.json();
			} else {
				throw Error("Unable to fetch schema node content");
			}

		}).then(function(json) {

			let result = json.result;

			_Code.updateRecentlyUsed(result, identifier.source, data.updateLocationStack);

			let html = _Code.templates.type({ type: result });
			_Code.codeContents.empty();
			_Code.codeContents.append(html);

			_Code.runCurrentEntitySaveAction = function() {
				_Code.saveEntityAction(result);
			};

			let buttons = $('#method-buttons');
			buttons.prepend(html);

			_Code.displaySvgActionButton('#type-actions', _Icons.getSvgIcon('checkmark_bold', 14, 14, 'icon-green'), 'save', 'Save', _Code.runCurrentEntitySaveAction);

			_Code.displaySvgActionButton('#type-actions', _Icons.getSvgIcon('close-dialog-x', 14, 14, 'icon-red'), 'cancel', 'Revert changes', () => {
				_Code.revertFormData(result);
			});

			// delete button
			if (!result.isBuiltinType) {
				_Code.displaySvgActionButton('#type-actions', _Icons.getSvgIcon('trashcan', 14, 14, 'icon-red'), 'delete', 'Delete type ' + result.name, () => {
					_Code.deleteSchemaEntity(result, 'Delete type ' + result.name + '?', 'This will delete all schema relationships as well, but no data will be removed.', identifier);
				});
			}

			let apiTab = $('#type-openapi', _Code.codeContents);
			apiTab.append(_Code.templates.openAPIConfig({ element: result, availableTags: _Code.availableTags }));

			$('#tags-select', apiTab).select2({
				tags: true,
				width: '100%'
			}).on('change', () => {
				_Code.updateDirtyFlag(result);
			});

			$('input[type=checkbox]', apiTab).on('change', () => {
				_Code.updateDirtyFlag(result);
			});

			$('input[type=text]', apiTab).on('keyup', () => {
				_Code.updateDirtyFlag(result);
			});

			Structr.activateCommentsInElement(apiTab);

			// manage working sets
			_WorkingSets.getWorkingSets(function(workingSets) {

				let groupSelect = document.querySelector('select#type-groups');

				let createAndAddWorkingSetOption = function(set, forceAdd) {

					let setOption = document.createElement('option');
					setOption.textContent = set.name;
					setOption.dataset['groupId'] = set.id;

					if (forceAdd === true || set.children && set.children.includes(result.name)) {
						setOption.selected = true;
					}

					groupSelect.appendChild(setOption);
				};

				for (let set of workingSets) {

					if (set.name !== _WorkingSets.recentlyUsedName) {
						createAndAddWorkingSetOption(set);
					}
				}

				let isUnselect = false;
				$(groupSelect).select2({
					search_contains: true,
					width: '100%',
					closeOnSelect: false
				}).on('select2:unselecting', function(e, p) {
					isUnselect = true;

				}).on('select2:opening', function(e, p) {
					if (isUnselect) {
						e.preventDefault();
						isUnselect = false;
					}

				}).on('select2:select', function(e, p) {
					let id = e.params.data.element.dataset['groupId'];

					_WorkingSets.addTypeToSet(id, result.name, function() {
						_TreeHelper.refreshNode('#code-tree', 'workingsets-' + id);
					});

				}).on('select2:unselect', function(e, p) {
					let id = e.params.data.element.dataset['groupId'];

					_WorkingSets.removeTypeFromSet(id, result.name, function() {
						_TreeHelper.refreshNode('#code-tree', 'workingsets-' + id);
					});
				});

				_Code.displaySvgActionButton('#type-actions', _Icons.getSvgIcon('folder_add', 14, 14, ''), 'new', 'Create new Working Set', function() {

					_WorkingSets.createNewSetAndAddType(result.name, function(ws) {
						_TreeHelper.refreshNode('#code-tree', 'workingsets');

						createAndAddWorkingSetOption(ws, true);
						$(groupSelect).trigger('change');
					});
				});
			});

			_Code.updateDirtyFlag(result);

			$('input[type=checkbox]', _Code.codeContents).on('change', function() {
				_Code.updateDirtyFlag(result);
			});

			$('input[type=text]', _Code.codeContents).on('keyup', function() {
				_Code.updateDirtyFlag(result);
			});

			Structr.activateCommentsInElement(_Code.codeContents);

			let schemaGrantsTableConfig = {
				class: 'schema-grants-table schema-props',
				cols: [
					{ class: '', title: 'Name' },
					{ class: '', title: 'read' },
					{ class: '', title: 'write' },
					{ class: '', title: 'delete' },
					{ class: '', title: 'access control' }
				],
				discardButtonText: 'Discard Schema Grant changes',
				saveButtonText: 'Save Schema Grants'
			};

			let schemaGrantsContainer       = document.querySelector('#schema-grants');
			schemaGrantsContainer.innerHTML = _Schema.templates.schemaTable(schemaGrantsTableConfig);

			let tbody      = schemaGrantsContainer.querySelector('tbody');
			let tfoot      = schemaGrantsContainer.querySelector('tfoot');
			let discardBtn = tfoot.querySelector('.discard-all');
			let saveBtn    = tfoot.querySelector('.save-all');

			let setButtonDisabled = (btn, disabled) => {
				btn.disabled = disabled;
				if (disabled) {
					btn.classList.add('disabled');
				} else {
					btn.classList.remove('disabled');
				}
			}

			let schemaGrantsTableChange = (cb, rowConfig) => {

				if (rowConfig[cb.name] !== cb.checked) {
					cb.classList.add('changed');
				} else {
					cb.classList.remove('changed');
				}

				let hasChanges      = (schemaGrantsContainer.querySelector('.changed'));
				discardBtn.disabled = !hasChanges;
				saveBtn.disabled    = !hasChanges;

				setButtonDisabled(saveBtn,    !hasChanges);
				setButtonDisabled(discardBtn, !hasChanges);
			};

			discardBtn.addEventListener('click', (e) => {

				for (let changedCb of tbody.querySelectorAll('.changed')) {

					changedCb.checked = !changedCb.checked;
					changedCb.classList.remove('changed');
				}

				setButtonDisabled(saveBtn,    true);
				setButtonDisabled(discardBtn, true);
			});

			saveBtn.addEventListener('click', async (e) => {

				let grantData = [];

				for (let row of tbody.querySelectorAll('tr')) {

					if (row.querySelector('.changed')) {

						let rowConfig = {
							principal:          row.dataset['groupId'],
							schemaNode:         result.id,
							allowRead:          row.querySelector('input[name=allowRead]').checked,
							allowWrite:         row.querySelector('input[name=allowWrite]').checked,
							allowDelete:        row.querySelector('input[name=allowDelete]').checked,
							allowAccessControl: row.querySelector('input[name=allowAccessControl]').checked
						};

						let grantId = row.dataset['grantId'];
						if (grantId) {
							rowConfig.id = grantId;
						}

						grantData.push(rowConfig);
					}
				}

				_Code.showSchemaRecompileMessage();

				let response = await fetch(Structr.rootUrl + 'SchemaGrant', {
					dataType: 'json',
					contentType: 'application/json; charset=utf-8',
					method: 'PATCH',
					body: JSON.stringify(grantData)
				});

				if (response.ok) {
					_Code.hideSchemaRecompileMessage();
					_Code.displaySchemaNodeContent(data, identifier);
				} else {
					_Code.hideSchemaRecompileMessage();
					new MessageBuilder().warning('Saving schema grants failed - please try again!').requiresConfirmation().show();
				}
			});

			let grants = {};

			for (let grant of result.schemaGrants) {
				grants[grant.principal.id] = grant;
			}

			Command.query('Group', 1000, 1, 'name', 'asc', { }, (groupResult) => {

				for (let group of groupResult) {

					let tplConfig = {
						groupId: group.id,
						name: group.name,
						grantId            : (!grants[group.id]) ? '' : grants[group.id].id,
						allowRead          : (!grants[group.id]) ? false : grants[group.id].allowRead,
						allowWrite         : (!grants[group.id]) ? false : grants[group.id].allowWrite,
						allowDelete        : (!grants[group.id]) ? false : grants[group.id].allowDelete,
						allowAccessControl : (!grants[group.id]) ? false : grants[group.id].allowAccessControl
					};

					let dummyTbody       = document.createElement('tbody');
					dummyTbody.innerHTML = _Code.templates.schemaGrantsRow(tplConfig);
					let row              = dummyTbody.firstElementChild;

					tbody.appendChild(row);

					for (let cb of row.querySelectorAll('input')) {

						cb.addEventListener('change', (e) => {
							schemaGrantsTableChange(cb, tplConfig);
						});
					}
				}
			});

			// usedIn property
			if (result.usedIn && result.usedIn.length > 0) {

				let usageTreeContainer = document.querySelector('#usage-tree');
				let label              = document.querySelector('#usage-label');

				// add help text
				label.innerHTML = 'This type is used in the following pages, HTML elements and attributes. Please note that this table might not be complete since the information here is collected at runtime, when you browse through the pages of your application.';

				let sorted = result.usedIn.sort((a, b) => {

					let p1 = a.path || a.page || a.type || a.id;
					let p2 = b.path || b.page || b.type || b.id;

					return p1 > p2 ? 1 : p1 < p2 ? -1 : 0;
				});

				let tree = { name: 'Usage', children: {} };

				// append rows
				for (let usage of sorted) {

					let path = usage.path;

					// The path is split into its parts to form the hierarchy, so if there is no
					// path, we use the root term "Types" plus the type of the node.
					if (!path) { path = 'Types/' + usage.type; } else { path = 'Pages/' + path; }

					let parts   = path.split('/').filter(p => p.length > 0);
					let current = tree;

					for (let part of parts) {

						if (!current.children[part]) {

							current.children[part] = {
								name: part,
								children: {}
							};
						}

						current = current.children[part];
					}

					current.data = usage;
				}

				let buildTree = function(root, rootElement) {

					let listItem = document.createElement('li');
					listItem.dataset.jstree = JSON.stringify({ icon: Structr.getPrefixedRootUrl('/structr/icon/folder.png') });
					//listItem.classList.add('jstree-open');
					listItem.innerHTML = root.name;
					rootElement.appendChild(listItem);

					let list = document.createElement('ul');
					listItem.appendChild(list);

					if (root.data) {

						for (let key in root.data.mapped) {

							let value = root.data.mapped[key];
							let item  = document.createElement('li');
							item.dataset.jstree = JSON.stringify({ icon: 'fa fa-edit' });
							item.dataset.id = root.data.id;
							item.innerHTML = key + ': '+ value;

							list.append(item);
						}
					}

					for (let key in root.children) {
						let child = root.children[key];
						buildTree(child, list);
					}
				};

				buildTree(tree, usageTreeContainer);

				let usageTree = $('#usage-tree-container').jstree({
					plugins: ["themes"],
					core: {
						animation: 0
					}
				});

				usageTree.on('select_node.jstree', function(node, selected, event) {

					let id = selected.node.data.id;
					if (id) {

						Command.get(id, 'id,type,name,content,ownerDocument,schemaNode', (obj) => {

							switch (obj.type) {

								case 'Content':
								case 'Template':
									_Elements.openEditContentDialog(obj);
									break;
								default:
									_Entities.showProperties(obj);
									break;
							}
						});

					} else {

						// not a leaf, toggle "opened" state
						usageTree.jstree('toggle_node', selected.node);
					}
				});

			} else {

				let label = document.querySelector('#usage-label');
				if (label) {

					label.innerHTML = 'Browse through your application to populate the usage list for this type.';
				}
			}
		});
	},
	displaySchemaRelationshipNodeContent: function (data, identifier) {

		Command.get(identifier.obj.nodeData.entity.id, null, function(entity) {

			Command.get(entity.sourceId, null, function(sourceNode) {

				Command.get(entity.targetId, null, function(targetNode) {

					_Code.codeContents.empty();
					_Code.codeContents.append(_Code.templates.propertyRemote({ identifier: identifier }));

					_Schema.loadRelationship(entity, $('#headEl', _Code.codeContents), $('#contentEl', _Code.codeContents), sourceNode, targetNode, _Code.refreshTree);
				});
			});
		});
	},
	displaySchemaMethodContent: function(data, lastOpenTab) {

		let identifier = _Code.splitIdentifier(data);

		// ID of schema method can either be in typeId (for global schema methods) or in memberId (for type methods)
		Command.get(identifier.memberId || identifier.typeId, 'id,owner,type,createdBy,hidden,createdDate,lastModifiedDate,visibleToPublicUsers,visibleToAuthenticatedUsers,name,isStatic,schemaNode,source,openAPIReturnType,exceptions,callSuper,overridesExisting,doExport,codeType,isPartOfBuiltInSchema,tags,summary,description,parameters,includeInOpenAPI', function(result) {

			_Code.updateRecentlyUsed(result, identifier.source, data.updateLocationStack);

			_Code.codeContents.empty();
			_Code.codeContents.append(_Code.templates.method({ method: result }));

			LSWrapper.setItem(_Code.codeLastOpenMethodKey, result.id);

			// Source Editor
			let sourceMonacoConfig = {
				language: 'auto',
				lint: true,
				autocomplete: true,
				changeFn: (editor, entity) => {
					_Code.updateDirtyFlag(entity);
				}
			};

			let sourceEditor = _Editors.getMonacoEditor(result, 'source', _Code.codeContents[0].querySelector('#tabView-source .editor'), sourceMonacoConfig);
			_Editors.appendEditorOptionsElement(_Code.codeContents[0].querySelector('.editor-info'));

			if (result.codeType === 'java' || _Code.methodNamesWithoutOpenAPITab.includes(result.name)) {

				$('li[data-name=api]').hide();

			} else {

				let apiTab = $('#tabView-api', _Code.codeContents);
				apiTab.append(_Code.templates.openAPIConfig({ element: result, availableTags: _Code.availableTags }));

				let openApiConfig = $('#openapi-options', apiTab);
				openApiConfig.append(_Code.templates.openAPIMethodConfig());

				let parameterTplRow = $('.template', apiTab);
				let parameterContainer = parameterTplRow.parent();
				parameterTplRow.remove();

				let addParameterRow = (parameter) => {

					let clone = parameterTplRow.clone().removeClass('template');

					if (parameter) {
						$('input[data-parameter-property=index]', clone).val(parameter.index);
						$('input[data-parameter-property=name]', clone).val(parameter.name);
						$('input[data-parameter-property=parameterType]', clone).val(parameter.parameterType);
						$('input[data-parameter-property=description]', clone).val(parameter.description);
						$('input[data-parameter-property=exampleValue]', clone).val(parameter.exampleValue);
					} else {
						let maxCnt = 0;
						for (let indexInput of apiTab[0].querySelectorAll('input[data-parameter-property=index]')) {
							maxCnt = Math.max(maxCnt, parseInt(indexInput.value) + 1);
						}

						$('input[data-parameter-property=index]', clone).val(maxCnt);
					}

					$('input[data-parameter-property]', clone).on('keyup', () => {
						_Code.updateDirtyFlag(result);
					});

					parameterContainer.append(clone);

					$('.method-parameter-delete .remove-action', clone).on('click', () => {
						clone.remove();

						_Code.updateDirtyFlag(result);
					});

					_Code.updateDirtyFlag(result);
				};

				Command.query('SchemaMethodParameter', 1000, 1, 'index', 'asc', { schemaMethod: result.id }, (parameters) => {

					_Code.additionalDirtyChecks.push(() => {
						return _Code.schemaMethodParametersChanged(parameters);
					});

					for (let p of parameters) {

						addParameterRow(p);
					}

				}, true, null, 'index,name,parameterType,description,exampleValue');

				$('#add-parameter-button').on('click', () => { addParameterRow(); });

				$('#tags-select', apiTab).select2({
					tags: true,
					width: '100%'
				}).on('change', () => {
					_Code.updateDirtyFlag(result);
				});

				$('input[type=checkbox]', apiTab).on('change', function() {
					_Code.updateDirtyFlag(result);
				});

				$('input[type=text]', apiTab).on('keyup', function() {
					_Code.updateDirtyFlag(result);
				});

				let openAPIReturnTypeMonacoConfig = {
					language: 'json',
					lint: true,
					autocomplete: true,
					changeFn: (editor, entity) => {
						_Code.updateDirtyFlag(entity);
					}
				};

				_Editors.getMonacoEditor(result, 'openAPIReturnType', apiTab[0].querySelector('.editor'), openAPIReturnTypeMonacoConfig);

				Structr.activateCommentsInElement(apiTab);
			}

			// default buttons
			_Code.runCurrentEntitySaveAction = function() {

				let storeParametersInFormDataFunction = function(formData) {
					let parametersData = _Code.collectSchemaMethodParameters();

					formData['parameters'] = parametersData;
				};

				let afterSaveCallback = () => {
					_Code.additionalDirtyChecks = [];
					_Code.displaySchemaMethodContent(data, $('li.active', _Code.codeContents).data('name'), true);
				};

				_Code.saveEntityAction(result, afterSaveCallback, [storeParametersInFormDataFunction]);
			};

			let buttons = $('#method-buttons');
			buttons.prepend(_Code.templates.methodOptions({ method: result }));

			_Code.displaySvgActionButton('#method-actions', _Icons.getSvgIcon('checkmark_bold', 14, 14, 'icon-green'), 'save', 'Save method', _Code.runCurrentEntitySaveAction);

			_Code.displaySvgActionButton('#method-actions', _Icons.getSvgIcon('close-dialog-x', 14, 14, 'icon-red'), 'cancel', 'Revert changes', () => {
				_Code.additionalDirtyChecks = [];
				_Editors.disposeEditorModel(result.id, 'source');
				_Editors.disposeEditorModel(result.id, 'openAPIReturnType');
				_Code.displaySchemaMethodContent(data, $('li.active', _Code.codeContents).data('name'));
			});

			// delete button
			_Code.displaySvgActionButton('#method-actions', _Icons.getSvgIcon('trashcan', 14, 14, 'icon-red'), 'delete', 'Delete method', () => {
				_Code.deleteSchemaEntity(result, 'Delete method ' + result.name + '?', 'Note: Builtin methods will be restored in their initial configuration', identifier);
			});

			// run button and global schema method flags
			if (!result.schemaNode && !result.isPartOfBuiltInSchema) {

				_Code.displaySvgActionButton('#method-actions', _Icons.getSvgIcon('run_button', 14, 14, ''), 'run', 'Run method', () => {
					_Code.runGlobalSchemaMethod(result);
				});

			} else if (result.schemaNode) {

				$('.checkbox.entity-method.hidden', buttons).removeClass('hidden');
				Structr.activateCommentsInElement(buttons);
			}

			_Code.updateDirtyFlag(result);

			$('input[type=checkbox]', buttons).on('change', function() {
				_Code.updateDirtyFlag(result);
			});

			$('input[type=text]', buttons).on('keyup', function() {
				_Code.updateDirtyFlag(result);
			});

			if (typeof callback === 'function') {
				callback();
			}

			let activateTab = (tabName) => {
				$('.method-tab-content', _Code.codeContents).hide();
				$('li[data-name]', _Code.codeContents).removeClass('active');

				let activeTab = $('#tabView-' + tabName, _Code.codeContents);
				activeTab.show();
				$('li[data-name="' + tabName + '"]', _Code.codeContents).addClass('active');

				window.setTimeout(() => { _Editors.resizeVisibleEditors(); }, 250);
			};

			$('li[data-name]', _Code.codeContents).off('click').on('click', function(e) {
				e.stopPropagation();
				activateTab($(this).data('name'));
			});
			activateTab(lastOpenTab || 'source');

			sourceEditor.focus();
		});
	},
	collectSchemaMethodParameters: function() {

		let parametersData = [];
		for (let formParam of _Code.codeContents[0].querySelectorAll('.method-parameter')) {
			let pData = {};
			for (let formParamValue of formParam.querySelectorAll('[data-parameter-property]')) {

				if (formParamValue.type === 'number') {
					pData[formParamValue.dataset['parameterProperty']] = parseInt(formParamValue.value);
				} else {
					pData[formParamValue.dataset['parameterProperty']] = formParamValue.value;
				}

			}

			parametersData.push(pData);
		}

		return parametersData;
	},
	schemaMethodParametersChanged: function(parameters) {

		let parametersData = _Code.collectSchemaMethodParameters();

		let easyAccessFormData = {};
		for (let fp of parametersData) {
			easyAccessFormData[fp.index] = fp;
		}

		// if params have same length AND every element from original params is identical to formData ==> ALLOW
		if (parametersData.length !== parameters.length) {

			return true;

		} else {

			for (let originalParameter of parameters) {

				let sameIndexFromForm = easyAccessFormData[originalParameter.index];

				if (sameIndexFromForm) {

					for (let key in sameIndexFromForm) {

						if (sameIndexFromForm[key] !== originalParameter[key] && !(sameIndexFromForm[key] === '' && !originalParameter[key])) {
							return true;
						}
					}

				} else {

					return true;

				}
			}
		}

		return false;
	},
	displaySchemaGroupContent: function(data, identifier) {

		_WorkingSets.getWorkingSet(identifier.workingSetId, function(workingSet) {

			_Code.updateRecentlyUsed(workingSet, identifier.source, data.updateLocationStack);
			_Code.codeContents.empty();
			_Code.codeContents.append(_Code.templates.workingSet({ type: workingSet }));

			if (workingSet.name === _WorkingSets.recentlyUsedName) {

				_Code.displaySvgActionButton('#working-set-content', _Icons.getSvgIcon('trashcan', 14, 14, 'icon-red'), 'clear', 'Clear', function() {
					_WorkingSets.clearRecentlyUsed(function() {
						_Code.refreshTree();
					});
				});

				$('#group-name-input').prop('disabled', true);

			} else {

				_Code.displaySvgActionButton('#working-set-content', _Icons.getSvgIcon('trashcan', 14, 14, 'icon-red'), 'remove', 'Remove', function() {
					_WorkingSets.deleteSet(identifier.workingSetId, function() {
						_TreeHelper.refreshNode('#code-tree', 'workingsets');
						_Code.findAndOpenNode('workingsets');
					});
				});

				_Code.activatePropertyValueInput('group-name-input', workingSet.id, 'name');
			}

			$('#schema-graph').append(_Code.templates.root());

			var layouter = new SigmaLayouter('group-contents');

			Command.query('SchemaNode', 10000, 1, 'name', 'asc', { }, function(result1) {

				for (let node of result1) {
					if (workingSet.children.includes(node.name)) {
						layouter.addNode(node, identifier.source);
					}
				}

				Command.query('SchemaRelationshipNode', 10000, 1, 'name', 'asc', { }, function(result2) {

					for (let r of result2) {
						layouter.addEdge(r.id, r.relationshipType, r.sourceId, r.targetId, true);
					}

					layouter.refresh();
					layouter.layout();
					layouter.on('clickNode', _Code.handleGraphClick);

					_Code.layouter = layouter;

					// experimental: use positions from schema layouter to initialize positions in schema editor
					window.setTimeout(function() {

						let minX = 0;
						let minY = 0;

						layouter.getNodes().forEach(function(node) {
							if (node.x < minX) { minX = node.x; }
							if (node.y < minY) { minY = node.y; }
						});

						let positions = {};

						layouter.getNodes().forEach(function(node) {

							positions[node.label] = {
								top: node.y - minY + 20,
								left: node.x - minX + 20
							};
						});

						_WorkingSets.updatePositions(workingSet.id, positions);

					}, 300);

				}, true, 'ui');

			}, true, 'ui');
		});
	},
	showGeneratedSource: async () => {

		let sourceContainer = document.getElementById('generated-source-code');
		if (sourceContainer) {

			fastRemoveAllChildren(sourceContainer);

			sourceContainer.classList.add('h-full');

			let typeId = sourceContainer.dataset.typeId;

			if (typeId) {

				let response = await fetch(Structr.rootUrl + 'SchemaNode/' + typeId + '/getGeneratedSourceCode', { method: 'POST' });

				if (response.ok) {

					let result = await response.json();

					let typeSourceConfig = {
						value: result.result,
						language: 'java',
						lint: false,
						autocomplete: false,
						readOnly: true
					};

					_Editors.getMonacoEditor({}, 'source-code', sourceContainer, typeSourceConfig);

					_Editors.resizeVisibleEditors();
				}
			}
		}
	},
	displayRootContent: function() {

		_Code.codeContents.append(_Code.templates.root());

		let layouter = new SigmaLayouter('all-types');

		Command.query('SchemaNode', 10000, 1, 'name', 'asc', { }, function(result1) {

				result1.forEach(function(node) {
					layouter.addNode(node, '');
				});

				Command.query('SchemaRelationshipNode', 10000, 1, 'name', 'asc', { }, function(result2) {

					result2.forEach(function(r) {
						layouter.addEdge(r.id, r.relationshipType, r.sourceId, r.targetId, true);
					});

					layouter.refresh();
					layouter.layout();
					layouter.on('clickNode', _Code.handleGraphClick);

					_Code.layouter = layouter;

				}, true, 'ui');

			}, true, 'ui');
	},
	handleGraphClick: function(e) {

		var data = e.data;
		if (data.node) {

			if (data.node.path) {

				_Code.findAndOpenNode(data.node.path + '-' + data.node.id, false);

			} else {

				// we need to found out if this node is a custom type or built-in
				if (data.node.builtIn) {
					_Code.findAndOpenNode('builtin--' + data.node.id, false);
				} else {
					_Code.findAndOpenNode('custom--' + data.node.id, false);
				}
			}
		}

	},
	displayCustomTypesContent: function() {

		_Code.codeContents.append(_Code.templates.custom());

		_Code.displayCreateTypeButton("#type-actions");

		// list of existing custom types
		Command.query('SchemaNode', 10000, 1, 'name', 'asc', { isBuiltinType: false }, function(result) {

			for (let t of result) {
				_Code.displayActionButton('#existing-types', 'fa fa-file-code-o', t.id, t.name, () => {
					_Code.findAndOpenNode('custom--' + t.id);
				});
			}
		}, true);
	},
	displayWorkingSetsContent: function() {
		_Code.codeContents.append(_Code.templates.workingSets());
	},
	displayBuiltInTypesContent: function() {
		_Code.codeContents.append(_Code.templates.builtin());

		// list of existing custom types
		Command.query('SchemaNode', 10000, 1, 'name', 'asc', { isBuiltinType: true }, function(result) {

			for (let t of result) {
				_Code.displayActionButton('#builtin-types', 'fa fa-file-code-o', t.id, t.name, () => {
					_Code.findAndOpenNode('builtin--' + t.id);
				});
			}
		}, true);
	},
	displayPropertiesContent: function(selection, updateLocationStack) {

		if (updateLocationStack === true) {
			_Code.updatePathLocationStack(selection.source);
			_Code.lastClickedPath = selection.source;
		}

		_Code.codeContents.append(_Code.templates.propertiesLocal({ identifier: selection }));

		Command.get(selection.typeId, null, (entity) => {

			_Schema.properties.appendLocalProperties($('.content-container', _Code.codeContents), entity, {

				editReadWriteFunction: (property) => {
					_Code.findAndOpenNode(selection.obj.id + '-' + property.id, true);
				},
				editCypherProperty: (property) => {
					_Code.findAndOpenNode(selection.obj.id + '-' + property.id, true);
				}
			}, _Code.refreshTree);

			_Code.runCurrentEntitySaveAction = () => {
				$('.save-all', _Code.codeContents).click();
			};
		});
	},
	displayRemotePropertiesContent: function (selection, updateLocationStack) {

		if (updateLocationStack === true) {
			_Code.updatePathLocationStack(selection.source);
			_Code.lastClickedPath = selection.source;
		}

		_Code.codeContents.append(_Code.templates.propertiesRemote({ identifier: selection }));

		Command.get(selection.typeId, null, (entity) => {
			_Schema.remoteProperties.appendRemote($('.content-container', _Code.codeContents), entity, _Code.schemaNodes, _Code.refreshTree);

			_Code.runCurrentEntitySaveAction = () => {
				$('.save-all', _Code.codeContents).click();
			};
		});
	},
	displayViewsContent: function(selection, updateLocationStack) {

		if (updateLocationStack === true) {
			_Code.updatePathLocationStack(selection.source);
			_Code.lastClickedPath = selection.source;
		}

		_Code.codeContents.append(_Code.templates.views({ identifier: selection }));

		Command.get(selection.typeId, null, (entity) => {
			_Schema.views.appendViews($('.content-container', _Code.codeContents), entity, _Code.refreshTree);

			_Code.runCurrentEntitySaveAction = () => {
				$('.save-all', _Code.codeContents).click();
			};
		});
	},
	displayGlobalMethodsContent: function(selection, updateLocationStack) {

		if (updateLocationStack === true) {
			_Code.updatePathLocationStack(selection.source);
			_Code.lastClickedPath = selection.source;
		}

		_Code.addRecentlyUsedElement(selection.source, "Global methods", _Icons.getFullSpriteClass(_Icons.world_icon), selection.source, false);

		_Code.codeContents.append(_Code.templates.globals());

		Command.rest('SchemaMethod?schemaNode=null&' + Structr.getRequestParameterName('sort') + '=name&' + Structr.getRequestParameterName('order') + '=ascending', function (methods) {

			_Schema.methods.appendMethods($('.content-container', _Code.codeContents), null, methods, function() {
				if (selection && selection.extended) {
					_TreeHelper.refreshNode('#code-tree', 'workingsets-' + selection.extended);
				} else {
					_Code.refreshTree();
				}
			});

			_Code.runCurrentEntitySaveAction = () => {
				$('.save-all', _Code.codeContents).click();
			};
		});
	},
	displayMethodsContent: function(selection, updateLocationStack) {

		if (updateLocationStack === true) {
			_Code.updatePathLocationStack(selection.source);
			_Code.lastClickedPath = selection.source;
		}

		_Code.addRecentlyUsedElement(selection.source, selection.obj.type + ' Methods' , 'fa fa-code gray', selection.source, false);

		_Code.codeContents.append(_Code.templates.methods({ identifier: selection }));

		Command.get(selection.typeId, null, (entity) => {

			_Schema.methods.appendMethods($('.content-container', _Code.codeContents), entity, _Schema.filterJavaMethods(entity.schemaMethods), function() {
				if (selection && selection.extended) {
					_TreeHelper.refreshNode('#code-tree', 'workingsets-' + selection.extended);
				} else {
					_Code.refreshTree();
				}
			});

			_Code.runCurrentEntitySaveAction = () => {
				$('.save-all', _Code.codeContents).click();
			};
		});
	},
	displayInheritedPropertiesContent: (selection, updateLocationStack) => {

		if (updateLocationStack === true) {
			_Code.updatePathLocationStack(selection.source);
			_Code.lastClickedPath = selection.source;
		}

		_Code.codeContents.append(_Code.templates.propertiesInherited({ identifier: selection }));

		Command.get(selection.typeId, null, (entity) => {
			_Schema.properties.appendBuiltinProperties($('.content-container', _Code.codeContents), entity);
		});
	},
	displayPropertyDetails: function(selection, identifier) {

		Command.get(identifier.memberId, null, function(result) {

			_Code.updateRecentlyUsed(result, identifier.source, selection.updateLocationStack);

			if (result.propertyType) {

				switch (result.propertyType) {
					case 'Cypher':
						_Code.displayCypherPropertyDetails(result);
						break;

					case 'Function':
						_Code.displayFunctionPropertyDetails(result, identifier);
						break;

					case 'String':
						_Code.displayStringPropertyDetails(result, identifier);
						break;

					case 'Boolean':
						_Code.displayBooleanPropertyDetails(result, identifier);
						break;

					default:
						_Code.displayDefaultPropertyDetails(result, identifier);
						break;
				}

			} else {

				if (result.type === 'SchemaRelationshipNode') {
					// this is a linked property/adjacent type
					// _Code.displayRelationshipPropertyDetails(result);
				}
			}
		});
	},
	displayViewDetails: function(selection) {

		let identifier = _Code.splitIdentifier(selection);

		Command.get(identifier.memberId, null, function(result) {

			_Code.updateRecentlyUsed(result, identifier.source, selection.updateLocationStack);

			_Code.codeContents.append(_Code.templates.defaultView({ view: result }));
			_Code.displayDefaultViewOptions(result, undefined, identifier);
		});
	},
	displayFunctionPropertyDetails: function(property, identifier, lastOpenTab) {

		_Code.codeContents.append(_Code.templates.functionProperty({ property: property }));

		let activateTab = (tabName) => {
			$('.function-property-tab-content', _Code.codeContents).hide();
			$('li[data-name]', _Code.codeContents).removeClass('active');

			let activeTab = $('#tabView-' + tabName, _Code.codeContents);
			activeTab.show();
			$('li[data-name="' + tabName + '"]', _Code.codeContents).addClass('active');

			window.setTimeout(() => { _Editors.resizeVisibleEditors(); }, 250);
		};

		for (let tabLink of _Code.codeContents[0].querySelectorAll('li[data-name]')) {
			tabLink.addEventListener('click', (e) => {
				e.stopPropagation();
				activateTab(tabLink.dataset.name);
			})
		}
		activateTab(lastOpenTab || 'source');

		Structr.activateCommentsInElement(_Code.codeContents, { insertAfter: true });

		let functionPropertyMonacoConfig = {
			language: 'auto',
			lint: true,
			autocomplete: true,
			changeFn: (editor, entity) => {
				_Code.updateDirtyFlag(entity);
			},
			isAutoscriptEnv: true
		};

		_Editors.getMonacoEditor(property, 'readFunction', _Code.codeContents[0].querySelector('#read-code-container .editor'), functionPropertyMonacoConfig);
		_Editors.getMonacoEditor(property, 'writeFunction', _Code.codeContents[0].querySelector('#write-code-container .editor'), functionPropertyMonacoConfig);

		_Editors.appendEditorOptionsElement(_Code.codeContents[0].querySelector('.editor-info'));

		let openAPIReturnTypeMonacoConfig = {
			language: 'json',
			lint: true,
			autocomplete: true,
			changeFn: (editor, entity) => {
				_Code.updateDirtyFlag(entity);
			}
		};

		_Editors.getMonacoEditor(property, 'openAPIReturnType', _Code.codeContents[0].querySelector('#tabView-api .editor'), openAPIReturnTypeMonacoConfig);

		_Code.displayDefaultPropertyOptions(property, _Editors.resizeVisibleEditors, identifier);
	},
	displayCypherPropertyDetails: function(property, identifier) {

		_Code.codeContents.append(_Code.templates.cypherProperty({ property: property }));

		let cypherMonacoConfig = {
			language: 'cypher',
			lint: false,
			autocomplete: false,
			changeFn: (editor, entity) => {
				_Code.updateDirtyFlag(entity);
			},
			isAutoscriptEnv: false
		};

		_Editors.getMonacoEditor(property, 'format', _Code.codeContents[0].querySelector('#cypher-code-container .editor'), cypherMonacoConfig);
		_Editors.appendEditorOptionsElement(_Code.codeContents[0].querySelector('.editor-info'));

		_Code.displayDefaultPropertyOptions(property, _Editors.resizeVisibleEditors, identifier);
	},
	displayStringPropertyDetails: function(property, identifier) {

		_Code.codeContents.append(_Code.templates.stringProperty({ property: property }));
		_Code.displayDefaultPropertyOptions(property, undefined, identifier);
	},
	displayBooleanPropertyDetails: function(property, identifier) {

		_Code.codeContents.append(_Code.templates.booleanProperty({ property: property }));
		_Code.displayDefaultPropertyOptions(property, undefined, identifier);
	},
	displayDefaultPropertyDetails: function(property, identifier) {

		_Code.codeContents.append(_Code.templates.defaultProperty({ property: property }));
		_Code.displayDefaultPropertyOptions(property, undefined, identifier);
	},
	displayDefaultPropertyOptions: function(property, callback, identifier) {

		_Code.runCurrentEntitySaveAction = function() {
			_Code.saveEntityAction(property);
		};

		let buttons = $('#property-buttons');
		buttons.prepend(_Code.templates.propertyOptions({ property: property }));

		_Code.displaySvgActionButton('#property-actions', _Icons.getSvgIcon('checkmark_bold', 14, 14, 'icon-green'), 'save', 'Save property', _Code.runCurrentEntitySaveAction);

		_Code.displaySvgActionButton('#property-actions', _Icons.getSvgIcon('close-dialog-x', 14, 14, 'icon-red'), 'cancel', 'Revert changes', () => {
			_Code.revertFormData(property);
		});

		if (!property.schemaNode.isBuiltinType) {

			_Code.displaySvgActionButton('#property-actions', _Icons.getSvgIcon('trashcan', 14, 14, 'icon-red'), 'delete', 'Delete property', () => {
				_Code.deleteSchemaEntity(property, 'Delete property ' + property.name + '?', 'No data will be removed.', identifier);
			});
		}

		if (property.propertyType !== 'Function') {
			$('#property-type-hint-input').parent().remove();
		} else {
			$('#property-type-hint-input').val(property.typeHint);
		}

		if (property.propertyType === 'Cypher') {
			$('#property-format-input').parent().remove();
		}

		$('select', buttons).on('change', function() {
			_Code.updateDirtyFlag(property);
		});

		$('input[type=checkbox]', buttons).on('change', function() {
			_Code.updateDirtyFlag(property);
		});

		$('input[type=text]', buttons).on('keyup', function() {
			_Code.updateDirtyFlag(property);
		});

		if (property.schemaNode.isBuiltinType) {
			$('button#delete-property-button').parent().remove();
		} else {
			$('button#delete-property-button').on('click', function() {
				_Code.deleteSchemaEntity(property, 'Delete property ' + property.name + '?', 'Property values will not be removed from data nodes.', identifier);
			});
		}

		_Code.updateDirtyFlag(property);

		if (typeof callback === 'function') {
			callback();
		}
	},
	displayDefaultViewOptions: function(view, callback, identifier) {

		_Code.runCurrentEntitySaveAction = function() {

			if (_Code.isDirty()) {

				// update entity before storing the view to make sure that nonGraphProperties are correctly identified..
				Command.get(view.schemaNode.id, null, function(reloadedEntity) {

					let formData = _Code.collectChangedPropertyData(view);
					let sortedAttrs = $('.property-attrs.view').sortedVals();
					formData.schemaProperties   = _Schema.views.findSchemaPropertiesByNodeAndName(reloadedEntity, sortedAttrs);
					formData.nonGraphProperties = _Schema.views.findNonGraphProperties(reloadedEntity, sortedAttrs);

					_Code.showSchemaRecompileMessage();

					Command.setProperties(view.id, formData, function() {
						Object.assign(view, formData);
						_Code.updateDirtyFlag(view);

						_Code.showSaveAction(formData);

						if (formData.name) {
							_Code.refreshTree();
						}

						_Code.hideSchemaRecompileMessage();
					});
				});
			}
		};

		let buttons = $('#view-buttons');
		buttons.prepend(_Code.templates.viewOptions({ view: view }));

		_Code.displaySvgActionButton('#view-actions', _Icons.getSvgIcon('checkmark_bold', 14, 14, 'icon-green'), 'save', 'Save view', _Code.runCurrentEntitySaveAction);

		_Code.displaySvgActionButton('#view-actions', _Icons.getSvgIcon('close-dialog-x', 14, 14, 'icon-red'), 'cancel', 'Revert changes', () => {
			_Code.revertFormData(view);
			_Code.displayViewSelect(view);
		});

		// delete button
		_Code.displaySvgActionButton('#view-actions', _Icons.getSvgIcon('trashcan', 14, 14, 'icon-red'), 'delete', 'Delete view', () => {
			_Code.deleteSchemaEntity(view, 'Delete view' + ' ' + view.name + '?', 'Note: Builtin views will be restored in their initial configuration', identifier);
		});

		_Code.updateDirtyFlag(view);

		$('input[type=text]', buttons).on('keyup', () => {
			_Code.updateDirtyFlag(view);
		});

		_Code.displayViewSelect(view);

		if (typeof callback === 'function') {
			callback();
		}
	},
	displayViewSelect: function(view) {

		Command.listSchemaProperties(view.schemaNode.id, view.name, function(properties) {

			let propertySelectContainer = $('#view-properties');
			fastRemoveAllChildren(propertySelectContainer[0]);
			propertySelectContainer.append('<div class="view-properties-select"><select class="property-attrs view chosen-sortable" multiple="multiple"></select></div>');
			let viewSelectElem = $('.property-attrs', propertySelectContainer);

			let appendProperty = function(prop) {
				let	name       = prop.name;
				var isSelected = prop.isSelected ? ' selected="selected"' : '';
				var isDisabled = (view.name === 'ui' || view.name === 'custom' || prop.isDisabled) ? ' disabled="disabled"' : '';

				viewSelectElem.append('<option value="' + name + '"' + isSelected + isDisabled + '>' + name + '</option>');
			};

			if (view.sortOrder) {
				view.sortOrder.split(',').forEach(function(sortedProp) {

					let prop = properties.filter(function(prop) {
						return (prop.name === sortedProp);
					});

					if (prop.length) {
						appendProperty(prop[0]);

						properties = properties.filter(function(prop) {
							return (prop.name !== sortedProp);
						});
					}
				});
			}

			properties.forEach(function (prop) {
				appendProperty(prop);
			});

			let changeFn = function () {
				var sortedAttrs = viewSelectElem.sortedVals();
				$('input#view-sort-order').val(sortedAttrs.join(','));
				_Code.updateDirtyFlag(view);
			};

			viewSelectElem.chosen({
				search_contains: true,
				width: '100%',
				display_selected_options: false,
				hide_results_on_select: false,
				display_disabled_options: false
			}).on('change', function(e,p) {
				changeFn();
			}).chosenSortable(function() {
				changeFn();
			});
		});
	},
	activateCreateDialog: function(suffix, presetValue, nodeData, elHtml) {

		let button = $('button#action-button-' + suffix);

		let revertFunction = function () {
			button.replaceWith(elHtml);
			_Code.activateCreateDialog(suffix, presetValue, nodeData, elHtml);
		};

		button.on('click.create-object-' + suffix, function() {

			button.off('click');
			button.addClass('action-button-open');

			button.append(_Code.templates.createObjectForm({ value: presetValue, suffix: suffix }));
			$('#new-object-name-' + suffix).focus();
			$('#new-object-name-' + suffix).on('keyup', function(e) {
				if (e.keyCode === 27) { revertFunction(); }
				if (e.keyCode === 13) { $('#create-button-' + suffix).click(); }
			});
			button.off('click.create-object-' + suffix);
			$('#cancel-button-' + suffix).on('click', function() {
				revertFunction();
			});
			$('#create-button-' + suffix).on('click', function() {
				var data = Object.assign({}, nodeData);
				data['name'] = $('#new-object-name-' + suffix).val();
				_Code.showSchemaRecompileMessage();
				Command.create(data, function() {
					_Code.refreshTree();
					_Code.clearMainArea();
					_Code.displayCustomTypesContent();
					_Code.hideSchemaRecompileMessage();
				});
			});
		});
	},
	displayCreateButton: function(targetId, iconClass, suffix, name, presetValue, createData) {
		let html =_Code.templates.actionButton({ iconClass: iconClass, suffix: suffix, name: name });
		$(targetId).append(html);
		_Code.activateCreateDialog(suffix, presetValue, createData, html);
	},
	displaySvgCreateButton: function(targetId, iconSvg, suffix, name, presetValue, createData) {
		let html = _Code.templates.actionButton({ iconSvg: iconSvg, suffix: suffix, name: name });
		$(targetId).append(html);
		_Code.activateCreateDialog(suffix, presetValue, createData, html);
	},
	displayActionButton: function(targetId, iconClass, suffix, name, callback) {
		let buttonId = '#action-button-' + suffix;
		$(targetId).append(_Code.templates.actionButton({ iconClass: iconClass, suffix: suffix, name: name }));
		$(buttonId).off('click.action').on('click.action', callback);
		return buttonId;
	},
	displaySvgActionButton: function(targetId, iconSvg, suffix, name, callback) {
		let buttonId = '#action-button-' + suffix;
		$(targetId).append(_Code.templates.actionButton({ iconSvg: iconSvg, suffix: suffix, name: name }));
		$(buttonId).off('click.action').on('click.action', callback);
		return buttonId;
	},
	displayCreatePropertyButton: function(targetId, type, nodeData) {
		let data = Object.assign({}, nodeData);
		data['propertyType'] = type;
		if (type === 'Enum') {
			data.format = 'value1, value2, value3';
		}
		_Code.displayCreateButton(targetId, 'fa fa-' + _Code.getIconForPropertyType(type), type.toLowerCase(), 'Add ' + type + ' property', '', data);
	},
	displayCreateTypeButton: function(targetId) {
		_Code.displaySvgCreateButton(targetId, _Icons.getSvgIcon('magic_wand', 14, 14, ''), 'create-type', 'Create new type', '', { type: 'SchemaNode'});
	},
	getEditorModeForContent: function(content) {
		return (content && content.indexOf('{') === 0) ? 'text/javascript' : 'text';
	},
	updateRecentlyUsed: function(entity, path, updateLocationStack) {

		_Code.addRecentlyUsedEntity(entity, path);

		// add recently used types to corresponding working set
		if (entity.type === 'SchemaNode') {
			_WorkingSets.addRecentlyUsed(entity.name);
		}

		if (updateLocationStack) {
			_Code.updatePathLocationStack(path);
			_Code.lastClickedPath = path;
		}
	},
	findAndOpenNode: function(path, updateLocationStack) {
		let tree = $('#code-tree').jstree(true);
		tree.open_node('root', function() {
			_Code.findAndOpenNodeRecursive(tree, path, 0, updateLocationStack);
		});
	},
	findAndOpenNodeRecursive: function(tree, path, depth, updateLocationStack) {

		let parts = path.split('-');
		if (path.length === 0) { return; }
		if (parts.length < 1) {	return; }
		if (depth > 15) { return; }

		let id = parts.slice(0, depth + 1).join('-');

		// special handling for globals, custom and builtin because these ids include an additional dash that skips the workingSetId
		switch (id) {
			case 'globals':
			case 'custom':
			case 'builtin':
				id = id + '-';
				break;
			case 'searchresults':
				// skip first part..
				id = parts.slice(1, depth + 1).join('-');
				break;
		}

		if (depth === parts.length) {

			// node found, activate
			if (tree.get_selected().indexOf(id) === -1) {
				tree.activate_node(id, { updateLocationStack: updateLocationStack });
			}

			let selectedNode = tree.get_node(id);
			if (selectedNode) {

				// depending on the depth we select a different parent level
				let parentToScrollTo = id;
				switch (selectedNode.parents.length) {
					case 1:
					case 2:
					case 3:
						parentToScrollTo = id;
						break;
					case 4:
						parentToScrollTo = tree.get_parent(id);
						break;
					case 5:
						parentToScrollTo = tree.get_parent(tree.get_parent(id));
						break;
				}

				// also scroll into view if node is in tree
				let domNode = document.getElementById( parentToScrollTo ) ;
				if (domNode) {
					domNode.scrollIntoView();
				}
			}

			if (_Code.searchIsActive()) {
				tree.element[0].scrollTo(0,0);
			}

		} else {

			tree.open_node(id, function(n) {
				_Code.findAndOpenNodeRecursive(tree, path, depth + 1, updateLocationStack);
			});
		}
	},
	showSchemaRecompileMessage: function() {
		Structr.showLoadingMessage('Schema is compiling', 'Please wait', 200);
	},
	hideSchemaRecompileMessage:  function() {
		Structr.hideLoadingMessage();
	},
	updatePathLocationStack: function(path) {

		var pos = _Code.pathLocationStack.indexOf(path);
		if (pos >= 0) {

			_Code.pathLocationStack.splice(pos, 1);
		}

		// remove tail of stack when click history branches
		if (_Code.pathLocationIndex !== _Code.pathLocationStack.length - 1) {

			_Code.pathLocationStack.splice(_Code.pathLocationIndex + 1, _Code.pathLocationStack.length - _Code.pathLocationIndex);
		}

		// add element to the end of the stack
		_Code.pathLocationStack.push(path);
		_Code.pathLocationIndex = _Code.pathLocationStack.length - 1;

		_Code.updatePathLocationButtons();
	},
	pathLocationForward: function() {

		_Code.pathLocationIndex += 1;
		var pos = _Code.pathLocationIndex;

		_Code.updatePathLocationButtons();

		if (pos >= 0 && pos < _Code.pathLocationStack.length) {

			var path = _Code.pathLocationStack[pos];
			_Code.findAndOpenNode(path, false);

		} else {
			_Code.pathLocationIndex -= 1;
		}

		_Code.updatePathLocationButtons();
	},
	pathLocationBackward: function() {

		_Code.pathLocationIndex -= 1;
		var pos = _Code.pathLocationIndex;

		if (pos >= 0 && pos < _Code.pathLocationStack.length) {

			var path = _Code.pathLocationStack[pos];
			_Code.findAndOpenNode(path, false);

		} else {

			_Code.pathLocationIndex += 1;
		}

		_Code.updatePathLocationButtons();
	},
	updatePathLocationButtons: function() {

		let stackSize       = _Code.pathLocationStack.length;
		let forwardDisabled = stackSize <= 1 || _Code.pathLocationIndex >= stackSize - 1;
		let backDisabled    = stackSize <= 1 || _Code.pathLocationIndex <= 0;

		$('#tree-forward-button').prop('disabled', forwardDisabled);
		$('#tree-back-button').prop('disabled', backDisabled);
	},
	doSearch: function(e) {
		let tree      = $('#code-tree').jstree(true);
		let input     = $('#tree-search-input');
		let text      = input.val();
		let threshold = _Code.searchThreshold;

		if (text.length >= threshold) {
			$('#cancel-search-button').show();
		} else {
			$('#cancel-search-button').hide();
		}

		if (text.length >= threshold || (_Code.searchTextLength >= threshold && text.length <= _Code.searchTextLength)) {
			tree.refresh();
		}

		_Code.searchTextLength = text.length;
	},
	searchIsActive: () => {
		let text = $('#tree-search-input').val();
		return (text && text.length >= _Code.searchThreshold);
	},
	cancelSearch: () => {
		$('#tree-search-input').val('');
		$('#tree-search-input').trigger('input');
	},
	activateLastClicked: () => {
		_Code.findAndOpenNode(_Code.lastClickedPath);
	},
	deleteSchemaEntity: (entity, title, text, identifier) => {

		let path  = identifier.source;
		let parts = path.split('-');

		parts.pop();

		let parent = parts.join('-');

		Structr.confirmation('<h3>' + title + '</h3><p>' + (text || '') + '</p>',
			function() {
				$.unblockUI({
					fadeOut: 25
				});
				_Code.showSchemaRecompileMessage();
				_Code.dirty = false;

				Command.deleteNode(entity.id, false, () => {
					_Code.forceNotDirty();
					_Code.hideSchemaRecompileMessage();
					_Code.findAndOpenNode(parent, false);
					_Code.refreshTree();
				});
			}
		);
	},
	runGlobalSchemaMethod: (schemaMethod) => {

		Structr.dialog('Run global schema method ' + schemaMethod.name, () => {}, () => {
			$('#run-method').remove();
			$('#clear-log').remove();
		}, ['run-global-schema-method-dialog']);

		dialogBtn.prepend(`<button id="run-method" class="flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">${_Icons.getSvgIcon('run_button', 16, 18, 'mr-2')}<span>Run</span></button>`);
		dialogBtn.append('<button id="clear-log" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Clear output</button>');

		let paramsOuterBox = $('<div id="params"><h3 class="heading-narrow">Parameters</h3></div>');
		let paramsBox = $('<div></div>');
		paramsOuterBox.append(paramsBox);
		let addParamBtn = $('<i title="Add parameter" class="button ' + _Icons.getFullSpriteClass(_Icons.add_icon) + '"></i>');
		paramsBox.append(addParamBtn);
		dialog.append(paramsOuterBox);

		Structr.appendInfoTextToElement({
			element: $('#params h3'),
			text: "Parameters can be accessed in the called method by using the <code>retrieve()</code> function.",
			css: { marginLeft: "5px" },
			helpElementCss: { fontSize: "12px" }
		});

		addParamBtn.on('click', () => {

			let newParam    = $('<div class="param flex items-center mb-1"><input class="param-name" placeholder="Parameter name"><span class="px-2">=</span><input class="param-value" placeholder="Parameter value"></div>');
			let removeParam = $(_Icons.getSvgIcon('trashcan', 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-action', 'ml-2'])));

			newParam.append(removeParam);
			removeParam.on('click', () => {
				newParam.remove();
			});

			paramsBox.append(newParam);
		});

		dialog.append('<h3>Method output</h3><pre id="log-output"></pre>');

		$('#run-method').on('click', async () => {

			let logOutput = document.getElementById('log-output');

			logOutput.textContent = 'Running method..\n';

			let params = {};
			$('#params .param').each(function (index, el) {
				let name = $('.param-name', el).val();
				let val  = $('.param-value', el).val();
				if (name) {
					params[name] = val;
				}
			});

			let response = await fetch(Structr.rootUrl + 'maintenance/globalSchemaMethods/' + schemaMethod.name, {
				method: 'POST',
				body: JSON.stringify(params)
			});

			let text = await response.text();
			logOutput.textContent += text;
			logOutput.textContent += 'Done.';
		});

		$('#clear-log').on('click', function() {
			$('#log-output').empty();
		});
	},
	activatePropertyValueInput: function(inputId, id, name) {

		$('input#' + inputId).on('blur', function() {
			let elem     = $(this);
			let previous = elem.attr('value');

			if (previous !== elem.val()) {
				let data   = {};
				data[name] = elem.val();

				Command.setProperties(id, data, function() {
					blinkGreen(elem);
					_TreeHelper.refreshTree('#code-tree');
				});
			}
		});
	},
	getErrorPropertyNameForLinting: (entity, propertyName) => {

		let errorPropertyNameForLinting = propertyName;

		if (entity.type === 'SchemaMethod') {
			errorPropertyNameForLinting = entity.name;
		} else if (entity.type === 'SchemaProperty') {
			if (propertyName === 'readFunction') {
				errorPropertyNameForLinting = `getProperty(${entity.name})`;
			} else if (propertyName === 'writeFunction') {
				errorPropertyNameForLinting = `setProperty(${entity.name})`;
			} else {
				// error?
			}
		} else if (entity.type === 'Content' || entity.type === 'Template') {
			errorPropertyNameForLinting = 'content';
		}

		return errorPropertyNameForLinting;
	},
	// setCodeMirorUpdateMode: function (entity, editor) {
	//
	// 	if (entity.codeType === 'java') {
	//
	// 		editor.setOption('mode', 'text/x-java');
	//
	// 	} else {
	//
	// 		editor.on('change', function() {
	// 			let type = _Code.getEditorModeForContent(editor.getValue());
	// 			let prev = editor.getOption('mode');
	// 			if (prev !== type) {
	// 				editor.setOption('mode', type);
	// 			}
	// 		});
	// 	}
	// },
	debounce(func, wait, immediate) {
		var timeout;

		return function() {
			var context = this, args = arguments;
			var later = function() {
				timeout = null;
				if (!immediate) func.apply(context, args);
			};
			var callNow = immediate && !timeout;
			clearTimeout(timeout);
			timeout = setTimeout(later, wait);
			if (callNow) func.apply(context, args);
		};
	},

	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/schema.css">
			<link rel="stylesheet" type="text/css" media="screen" href="css/code.css">
			
			<div class="tree-main" id="code-main">
			
				<div class="column-resizer-blocker"></div>
				<div class="column-resizer column-resizer-left"></div>
				<div class="column-resizer column-resizer-right"></div>
			
				<div class="tree-container" id="code-tree-container">
					<div class="tree" id="code-tree">
			
					</div>
				</div>
			
				<div class="tree-contents-container" id="code-contents-container">
					<div class="flex flex-col tree-contents" id="code-contents"></div>
				</div>
			
				<div class="tree-context-container" id="code-context-container">
					<div class="tree-context" id="code-context"></div>
				</div>
			</div>
		`,
		functions: config => `
			<div class="tree-search-container" id="tree-search-container">
				<i id="cancel-search-button" class="cancel-search-button hidden fa fa-remove"></i>
				<button type="button" class="tree-back-button hover:bg-gray-100 focus:border-gray-666 active:border-green" id="tree-back-button" title="Back" disabled>
					<i class="fa fa-caret-left"></i>
				</button>
				<input type="text" class="tree-search-input" id="tree-search-input" placeholder="Search..">
				<button type="button" class="tree-forward-button hover:bg-gray-100 focus:border-gray-666 active:border-green" id="tree-forward-button" title="Forward" disabled>
					<i class="fa fa-caret-right"></i>
				</button>
			</div>
		`,
		actionButton: config => `
			<button id="action-button-${config.suffix}" class="action-button hover:bg-gray-100 focus:border-gray-666 active:border-green">
				<div class="action-button-icon">
					${config.iconClass ? '<i class="' + config.iconClass +'"></i>' : ''}
					${config.iconSvg ? config.iconSvg : ''}
				</div>
			
				<div>${config.name}</div>
			</button>
		`,
		booleanProperty: config => `
			<h2>BooleanProperty ${config.property.schemaNode.name}.${config.property.name}</h2>
			<div id="property-buttons"></div>
		`,
		builtin: config => `
			<h2>System Types</h2>

			<div class="property-options-group">
				<div><label>Existing builtin types</label></div>
				<div id="builtin-types"></div>
			</div>
		`,
		createObjectForm: config => `
			<div class="mt-4">
				<input style="width: 100%; box-sizing: border-box;" id="new-object-name-${config.suffix}" value="${config.value}" placeholder="Enter name...">
			</div>
			<div class="flex justify-between mt-3">
				<button id="cancel-button-${config.suffix}" type="button" class="create-form-button cancel flex items-center px-3 py-1">
					${_Icons.getSvgIcon('cross_bold', 14, 14, 'icon-red')}
				</button>
				<button id="create-button-${config.suffix}" type="button" class="create-form-button accept flex items-center px-3 py-1">
					${_Icons.getSvgIcon('checkmark_bold', 14, 14, 'icon-green')}
				</button>
			</div>
		`,
		custom: config => `
			<h2>Custom types</h2>
			<div class="property-options-group">
			<!--	<div><label>Add type</label></div>-->
				<div id="type-actions"></div>
				<div style="clear:both;"></div>
			</div>
			<div class="property-options-group">
				<div><label>Existing custom types</label></div>
				<div id="existing-types"></div>
				<div style="clear:both;"></div>
			</div>
		`,
		cypherProperty: config => `
			<h2>CypherProperty ${config.property.schemaNode.name}.${config.property.name}</h2>
			<div id="property-buttons"></div>
			<div class="flex flex-col flex-grow">
				<div id="cypher-code-container" class="property-options-group flex flex-col h-full">
					<h4>Cypher Query</h4>
					<div class="editor flex-grow" data-property="format"></div>
					<div class="editor-info"></div>
				</div>
			</div>
		`,
		defaultProperty: config => `
			<h2>${config.property.propertyType}Property ${config.property.schemaNode.name}.${config.property.name}</h2>
			<div id="property-buttons"></div>
		`,
		defaultView: config => `
			<h2>View ${config.view.schemaNode.name}.${config.view.name}</h2>
			<div id="view-buttons"></div>
		`,
		functionProperty: config => `
			<h2>FunctionProperty ${config.property.schemaNode.name}.${config.property.name}</h2>
			<div id="property-buttons"></div>
			
			<div id="function-property-container" class="data-tabs level-two flex flex-col flex-grow">
				<ul>
					<li data-name="source">Code</li>
					<li data-name="api">API</li>
				</ul>
				<div id="function-property-content" class="flex flex-col flex-grow">
			
					<div class="tab function-property-tab-content flex flex-col flex-grow" id="tabView-source">
			
						<div id="read-code-container" class="property-options-group flex flex-col h-1/2">
							<h4>Read Function</h4>
							<div class="editor flex-grow" data-property="readFunction" data-recompile="false"></div>
						</div>
						<div id="write-code-container" class="property-options-group flex flex-col h-1/2">
							<div>
								<h4 data-comment="To retrieve the parameter passed to the write function, use &lt;code&gt;Structr.get('value');&lt;/code&gt; in a JavaScript context or the keyword &lt;code&gt;value&lt;/code&gt; in a StructrScript context.">Write Function</h4>
							</div>
							<div class="editor flex-grow" data-property="writeFunction" data-recompile="false"></div>
						</div>
			
					</div>
			
					<div class="tab function-property-tab-content flex flex-col flex-grow" id="tabView-api">
						<div>
							<h4 class="font-semibold" data-comment="Write an OpenAPI schema for your return type here.">Return Type</h4>
						</div>
						<div class="editor flex-grow" data-property="openAPIReturnType"></div>
					</div>
			
				</div>
				<div class="editor-info"></div>
			</div>
		`,
		globals: config => `
			<h2>Global schema methods</h2>
			<div id="code-methods-container" class="content-container"></div>
		`,
		method: config => `
			<h2>${(config.method.schemaNode ? config.method.schemaNode.name + '.' : '') + config.method.name}()</h2>
			<div id="method-buttons"></div>
			<div id="method-code-container" class="data-tabs level-two flex flex-col flex-grow">
				<ul>
					<li data-name="source">Code</li>
					<li data-name="api">API</li>
				</ul>
				<div id="methods-content" class="flex flex-col flex-grow">
			
					<div class="tab method-tab-content flex flex-col flex-grow" id="tabView-source">
						<div class="editor property-code flex-grow" data-property="source"></div>
					</div>
			
					<div class="tab method-tab-content flex flex-col flex-grow" id="tabView-api">
					</div>
			
				</div>
				<div class="editor-info"></div>
			</div>
		`,
		methodOptions: config => `
			<div id="method-options">
				<div class="property-options-group">
					<div id="method-actions"></div>
				</div>
				<div class="property-options-group flex flex-wrap gap-x-8">
					<div class="input">
						<label>Name</label>
						<input type="text" id="method-name-input" data-property="name" size="20" value="${config.method.name}" />
					</div>
					<div class="checkbox hidden entity-method">
						<label class="block mt-6" data-comment="Only needs to be set if the method should be callable statically (without an object context)."><input type="checkbox" data-property="isStatic" ${config.method.isStatic ? 'checked' : ''} /> isStatic</label>
					</div>
				</div>
			</div>
		`,
		methods: config => `
			<h2>Methods of type ${config.identifier.obj.type}</h2>
			<div id="code-methods-container" class="content-container"></div>
		`,
		openAPIConfig: config => `
			<div id="openapi-options"  class="flex flex-col flex-grow mt-4">
			
				<div class="flex flex-wrap gap-x-8">
			
					<div>
						<label class="font-semibold">Enabled</label>
						<label class="checkbox block mt-3"><input type="checkbox" data-property="includeInOpenAPI" ${config.element.includeInOpenAPI ? "checked" : ""} /> Include in OpenAPI output</label>
					</div>
			
					<div style="width: 200px">
						<label class="font-semibold" data-comment="Use tags to combine types and methods into an API. Each tag is available under its own OpenAPI endpoint (/structr/openapi/tag.json).">Tags</label>
						<select id="tags-select" data-property="tags" multiple="multiple">
							${ ((config.availableTags) ? config.availableTags.map((t) => { let selected = (config.element.tags && config.element.tags.includes(t)); return '<option' + (selected ? ' selected' : '') + '>' + t + '</option>'}).join() : '')}
						</select>
					</div>
					<div>
						<label class="font-semibold">Summary</label>
						<input class="mt-1" data-property="summary" value="${config.element.summary || ''}" type="text">
					</div>
			
					<div class="${config.element.type === 'SchemaNode' ? 'hidden' : ''}">
						<label class="font-semibold">Description</label>
						<input class="mt-1" data-property="description" value="${config.element.description || ''}" type="text">
					</div>
				</div>
			</div>
		`,
		openAPIMethodConfig: config => `
			<div class="mt-4">
			
				<label class="font-semibold">Parameters</label>
				<button id="add-parameter-button"><i class="${ _Icons.getFullSpriteClass(_Icons.add_icon)}"></i></button>
			
				<div>
					<div class="method-parameter-grid">
						<div class="method-parameter-heading">
							<div>Index</div>
							<div>Name</div>
							<div>Parameter Type</div>
							<div>Description</div>
							<div>Example Value</div>
							<div></div>
						</div>
					</div>
				</div>
			
				<div class="method-parameter-grid template">
					<div class="method-parameter">
						<div>
							<input data-parameter-property="index" type="number">
						</div>
						<div>
							<input data-parameter-property="name">
						</div>
						<div>
							<input data-parameter-property="parameterType">
						</div>
						<div>
							<input data-parameter-property="description">
						</div>
						<div>
							<input data-parameter-property="exampleValue">
						</div>
			
						<div class="method-parameter-property method-parameter-delete flex items-center justify-center">
							${_Icons.getSvgIcon('trashcan', 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-action', 'ml-2']))}
						</div>
					</div>
				</div>
			</div>
			
			<div class="mt-4 flex flex-col flex-grow">
				<label class="font-semibold" data-comment="Write an OpenAPI schema for your return type here.">Return Type</label>
				<div class="editor flex-grow" data-property="openAPIReturnType"></div>
			</div>
		`,
		propertiesInherited: config => `
			<h2>Inherited Attributes of type ${config.identifier.obj.type}</h2>
			<div class="content-container"></div>
		`,
		propertiesLocal: config => `
			<h2>Local Properties of type ${config.identifier.obj.type}</h2>
			<div class="content-container"></div>
		`,
		propertiesRemote: config => `
			<h2>Linked properties of type ${config.identifier.obj.type}</h2>
			<div class="content-container"></div>
		`,
		property: config => `
			<h2>${config.property.schemaNode.name}.${config.property.name}</h2>
		`,
		propertyRemote: config => `
			<div class="content-container">
				<div id="headEl"></div>
				<div id="contentEl"></div>
			</div>
		`,
		propertyOptions: config => `
			<div id="property-options">
			
				<div id="default-buttons" class="property-options-group">
					<div id="property-actions"></div>
				</div>
			
				<div class="property-options-group flex flex-wrap gap-x-8">
					<div><label class="font-semibold">Name</label><input type="text" id="property-name-input" data-property="name" value="${config.property.name}" /></div>
					<div><label class="font-semibold">Content type</label><input type="text" id="property-content-type-input" data-property="contentType" value="${config.property.contentType || ''}" /></div>
					<div><label class="font-semibold">Type hint</label>
						<select id="property-type-hint-input" class="type-hint" data-property="typeHint">
							<optgroup label="Type Hint">
								<option value="null">-</option>
								<option value="boolean">Boolean</option>
								<option value="string">String</option>
								<option value="int">Int</option>
								<option value="long">Long</option>
								<option value="double">Double</option>
								<option value="date">Date</option>
							</optgroup>
						</select>
					</div>
					<div><label class="font-semibold">Database name</label><input type="text" id="property-dbname-input" data-property="dbName" value="${config.property.dbName || ''}" /></div>
					<div><label class="font-semibold">Format</label><input type="text" id="property-format-input" data-property="format" value="${config.property.format || ''}" /></div>
					<div><label class="font-semibold">Default value</label><input type="text" id="property-default-input" data-property="defaultValue" value="${config.property.defaultValue || ''}" /></div>
				</div>
			
				<div class="property-options-group">
					<div>
						<label class="font-semibold">Options</label>
					</div>
					<div class="flex flex-wrap gap-x-8 mt-2">
						<div><label><input type="checkbox" id="property-unique" data-property="unique" ${config.property.unique ? 'checked' : ''} />Property value must be unique</label></div>
						<div><label><input type="checkbox" id="property-composite" data-property="compound" ${config.property.compound ? 'checked' : ''} />Include in composite uniqueness</label></div>
						<div><label><input type="checkbox" id="property-notnull" data-property="notNull" ${config.property.notNull ? 'checked' : ''} />Property value must not be null</label></div>
						<div><label><input type="checkbox" id="property-indexed" data-property="indexed" ${config.property.indexed ? 'checked' : ''} />Property value is indexed</label></div>
						<div><label><input type="checkbox" id="property-cached" data-property="isCachingEnabled" ${config.property.isCachingEnabled ? 'checked' : ''} />Property value can be cached</label></div>
					</div>
				</div>
			</div>
		`,
		recentlyUsedButton: config => `
			<div class="code-favorite items-center" id="recently-used-${config.id}">
				<i class="${config.iconClass} flex-none"></i>
				<div class="truncate flex-grow">${config.name}</div>
				${_Icons.getSvgIcon('close-dialog-x', 14, 14, _Icons.getSvgIconClassesForColoredIcon(['flex-none', 'icon-grey', 'remove-recently-used']))}
			</div>
		`,
		root: config => `
			<div id="all-types" style="position: relative; width: 100%; height: 98%;">
			</div>
		`,
		schemaGrantsRow: config => `
			<tr data-group-id="${config.groupId}" data-grant-id="${config.grantId}">
				<td>${config.name}</td>
				<td class="centered"><input type="checkbox" name="allowRead" ${(config.allowRead ? 'checked="checked"' : '')}/></td>
				<td class="centered"><input type="checkbox" name="allowWrite" ${(config.allowWrite ? 'checked="checked"' : '')}/></td>
				<td class="centered"><input type="checkbox" name="allowDelete" ${(config.allowDelete ? 'checked="checked"' : '')}/></td>
				<td class="centered"><input type="checkbox" name="allowAccessControl" ${(config.allowAccessControl ? 'checked="checked"' : '')}/></td>
			</tr>
		`,
		stringProperty: config => `
			<h2>StringProperty ${config.property.schemaNode.name}.${config.property.name}</h2>
			<div id="property-buttons"></div>
		`,
		type: config => `
			<h2>Class ${config.type.name}</h2>
			<div class="property-options-group">
				<div id="type-actions"></div>
			</div>
			<h3 id="type-working-sets-heading" data-comment="Working Sets are identical to layouts. Removing an element from a group removes it from the layout">Working Sets</h3>
			<div class="property-options-group">
				<div>
					<select id="type-groups" multiple="multiple"></select>
					<span id="add-to-new-group"></span>
				</div>
			</div>
			<h3>Options</h3>
			<div class="property-options-group">
				<div>
					<label data-comment="Only takes effect if the changelog is active"><input id="changelog-checkbox" type="checkbox" data-property="changelogDisabled" ${config.type.changelogDisabled ? 'checked' : ''} /> Disable changelog</label>
					<label class="ml-8" data-comment="Makes all nodes of this type visible to public users if checked"><input id="public-checkbox" type="checkbox" data-property="defaultVisibleToPublic" ${config.type.defaultVisibleToPublic ? 'checked' : ''} /> Visible for public users</label>
					<label class="ml-8" data-comment="Makes all nodes of this type visible to authenticated users if checked"><input id="authenticated-checkbox" type="checkbox" data-property="defaultVisibleToAuth" ${config.type.defaultVisibleToAuth ? 'checked' : ''} /> Visible for authenticated users</label>
				</div>
			</div>
			
			<h3>OpenAPI</h3>
			<div class="property-options-group">
				<div id="type-openapi">
				</div>
			</div>
			
			<h3>Schema Grants</h3>
			<div class="property-options-group">
				<div><label>Check the below boxes to grant the corresponding permissions on <b>all ${config.type.name} nodes</b> to the given group.</label></div>
				<div id="schema-grants"></div>
			</div>
			
			<h3>Usage Search</h3>
			<div class="property-options-group">
				<div><label id="usage-label"></label></div>
				<div id="usage-tree-container"><ul id="usage-tree"></ul></div>
			</div>
			
			<div id="generated-source-code" data-type-id="${config.type.id}"></div>
		`,
		viewOptions: config => `
			<div id="view-options">
			
				<div class="property-options-group">
					<div id="view-actions"></div>
				</div>
			
				<div class="property-options-group">
					<div>
						<label class="font-semibold">Name</label>
						<input type="text" id="view-name-input" data-property="name" value="${config.view.name}" />
					</div>
				</div>
			
				<div class="property-options-group">
					<div>
						<label class="font-semibold">Properties</label>
					</div>
					<input type="text" id="view-sort-order" class="hidden" data-property="sortOrder" value="${config.view.sortOrder || ''}">
					<div id="view-properties"></div>
				</div>
			</div>
		`,
		views: config => `
			<h2>Views of type ${config.identifier.obj.type}</h2>
			<div class="content-container"></div>
		`,
		workingSet: config => `
			<h2>Group ${config.type.name}</h2>
			<div class="property-options-group">
				A customizable group of classes. To add classes, click on the class name and then on the "New group" button.
			</div>
			
			<div id="working-set-content" data-type-id="${config.type.id}"></div>
			<div style="clear: both;"></div>
			
			<div class="property-options-group">
				<p class="input"><label>Name</label><input type="text" id="group-name-input" data-property="name" size="30" value="${config.type.name}" /></p>
				<div style="clear: both;"></div>
			</div>
			
			<div id="group-contents" style="height: calc(100% - 200px); background-color: #fff;" class="fit-to-height"></div>
		`,
		workingSets: config => `
			<h2>Working Sets</h2>
			<div class="property-options-group">
				<p>
					Working Sets are customizable group of classes. To add classes to a working set, click on the class name and then on the "New working set" button.
				</p>
			</div>
		`,
	}
};

let _WorkingSets = {

	recentlyUsedName: 'Recently Used Types',
	deleted: {},

	getWorkingSets: function(callback) {

		Command.query('ApplicationConfigurationDataNode', 1000, 1, 'name', true, { configType: 'layout' }, function(result) {

			let workingSets = [];
			let recent;

			for (var layout of result) {

				if (!layout.owner || layout.owner.name === StructrWS.me.username) {

					let content  = JSON.parse(layout.content);
					let children = Object.keys(content.positions);
					let data     = {
						type: 'SchemaGroup',
						id: layout.id,
						name: layout.name,
						children: children
					};

					if (layout.name === _WorkingSets.recentlyUsedName) {

						data.icon = _Icons.image_icon;
						recent    = data;

					} else {

						workingSets.push(data);
					}
				}
			}

			// insert most recent at the top
			if (recent) {
				workingSets.unshift(recent);
			}

			callback(workingSets);

		}, true, null, 'id,type,name,content,owner');
	},

	getWorkingSet: function(id, callback) {

		Command.get(id, null, function(result) {

			let content = JSON.parse(result.content);

			callback({
				id: result.id,
				type: result.type,
				name: result.name,
				owner: result.owner,
				children: Object.keys(content.positions)
			});
		});
	},

	getWorkingSetContents: function(id, callback) {

		Command.get(id, null, function(result) {

			let content   = JSON.parse(result.content);
			let positions = content.positions;

			Command.query('SchemaNode', 1000, 1, 'name', true, {}, function(result) {

				let schemaNodes = [];

				for (var schemaNode of result) {

					if (positions[schemaNode.name]) {

						schemaNodes.push(schemaNode);
					}
				}

				callback(schemaNodes);
			});
		});
	},

	addTypeToSet: function(id, type, callback) {

		Command.get(id, null, function(result) {

			let content = JSON.parse(result.content);
			if (!content.positions[type]) {

				content.positions[type] = {
					top: 0,
					left: 0
				};

				// adjust hidden types as well
				content.hiddenTypes.splice(content.hiddenTypes.indexOf(type), 1);

				Command.setProperty(id, 'content', JSON.stringify(content), false, callback);
			}
		});
	},

	removeTypeFromSet: function(id, type, callback) {

		Command.get(id, null, function(result) {

			let content = JSON.parse(result.content);
			delete content.positions[type];

			// adjust hidden types as well
			content.hiddenTypes.push(type);

			Command.setProperty(id, 'content', JSON.stringify(content), false, callback);
		});
	},

	createNewSetAndAddType: function(name, callback, setName) {

		Command.query('SchemaNode', 10000, 1, 'name', true, { }, function(result) {

			let positions = {};

			positions[name] = { top: 0, left: 0 };

			// all types are hidden except the one we want to add
			let hiddenTypes = result.filter(t => t.name !== name).map(t => t.name);

			let config = {
				_version: 2,
				positions: positions,
				hiddenTypes: hiddenTypes,
				zoom: 1,
				connectorStyle: 'Flowchart',
				showRelLabels: true
			};

			Command.create({
				type: 'ApplicationConfigurationDataNode',
				name: setName || 'New Working Set',
				content: JSON.stringify(config),
				configType: 'layout'
			}, callback);

		}, true, null, 'id,name');
	},

	deleteSet: function(id, callback) {

		_WorkingSets.deleted[id] = true;

		Command.deleteNode(id, false, callback);
	},

	updatePositions: function(id, positions, callback) {

		if (positions && !_WorkingSets.deleted[id]) {

			Command.get(id, null, function(result) {

				let content = JSON.parse(result.content);

				for (var key of Object.keys(content.positions)) {

					let position = content.positions[key];

					if (position && position.left === 0 && position.top === 0 && positions[key]) {

						position.left = positions[key].left * 2;
						position.top  = positions[key].top * 2;
					}
				}

				Command.setProperty(id, 'content', JSON.stringify(content), false, callback);
			});
		}
	},

	addRecentlyUsed: function(name) {

		Command.query('ApplicationConfigurationDataNode', 1, 1, 'name', true, { name: _WorkingSets.recentlyUsedName }, function(result) {

			if (result && result.length) {

				_WorkingSets.addTypeToSet(result[0].id, name, function() {
					_TreeHelper.refreshNode('#code-tree', 'workingsets-' + result[0].id);
				});

 			} else {

				_WorkingSets.createNewSetAndAddType(name, function() {
					_TreeHelper.refreshNode('#code-tree', 'workingsets');
				}, _WorkingSets.recentlyUsedName);

			}

		}, true, null, 'id,name');
	},

	clearRecentlyUsed: function(callback) {

		Command.query('ApplicationConfigurationDataNode', 1, 1, 'name', true, { name: _WorkingSets.recentlyUsedName }, function(result) {

			if (result && result.length) {

				let set     = result[0];
				let content = JSON.parse(set.content);

				for (var type of Object.keys(content.positions)) {

					// remove type from positions object
					delete content.positions[type];

					// add type to hidden types
					content.hiddenTypes.push(type);
				}

				Command.setProperty(set.id, 'content', JSON.stringify(content), false, callback);
			}

		}, true, null, 'id,name,content');
	}
};