/*
 * Copyright (C) 2010-2020 Structr GmbH
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
var main, codeMain, codeTree, codeContents, codeContext;
var drop;
var selectedElements = [];
var activeMethodId, methodContents = {};
var currentWorkingDir;
var methodPageSize = 10000, methodPage = 1;
var timeout, attempts = 0, maxRetry = 10;
var displayingFavorites = false;
var codeLastOpenMethodKey = 'structrCodeLastOpenMethod_' + port;
var codeResizerLeftKey = 'structrCodeResizerLeftKey_' + port;
var activeCodeTabPrefix = 'activeCodeTabPrefix' + port;

$(document).ready(function() {
	Structr.registerModule(_Code);
});

var _Code = {
	_moduleName: 'code',
	pathLocationStack: [],
	pathLocationIndex: 0,
	searchThreshold: 3,
	searchTextLength: 0,
	lastClickedPath: '',
	layouter: null,
	seed: 42,
	codeRecentElementsKey: 'structrCodeRecentElements_' + port,
	init: function() {

		_Logger.log(_LogType.CODE, '_Code.init');

		Structr.makePagesMenuDroppable();
		Structr.adaptUiToAvailableFeatures();

		$(window).on('keydown', _Code.handleKeyDownEvent);

	},
	beforeunloadHandler: function() {
		if (_Code.isDirty()) {
			return 'There are unsaved changes - discard changes?';
		}
	},
	resize: function() {

		var windowHeight = $(window).height();
		var headerOffsetHeight = 100;

		if (codeTree) {
			codeTree.css({
				height: windowHeight - headerOffsetHeight - 27 + 'px'
			});
		}

		if (codeContents) {
			codeContents.css({
				height: windowHeight - headerOffsetHeight - 11 + 'px'
			});
		}

		if (codeContext) {
			codeContext.css({
				height: windowHeight - headerOffsetHeight - 11 + 'px'
			});
		}

		_Code.moveResizer();
		Structr.resize();

		var nameColumnWidth = $('#code-table th:nth-child(2)').width();

		if (nameColumnWidth < 300) {
			$('#code-table th:nth-child(4)').css({ display: 'none' });
			$('#code-table td:nth-child(4)').css({ display: 'none' });
			$('#code-table th:nth-child(5)').css({ display: 'none' });
			$('#code-table td:nth-child(5)').css({ display: 'none' });
		}

		if (nameColumnWidth > 550) {
			$('#code-table th:nth-child(4)').css({ display: 'table-cell' });
			$('#code-table td:nth-child(4)').css({ display: 'table-cell' });
			$('#code-table th:nth-child(5)').css({ display: 'table-cell' });
			$('#code-table td:nth-child(5)').css({ display: 'table-cell' });
		}

		nameColumnWidth = $('#code-table th:nth-child(2)').width() - 96;

		$('.node.method .name_').each(function(i, el) {
			var title = $(el).attr('title');
			$(el).replaceWith('<b title="' +  title + '" class="name_">' + fitStringToWidth(title ? title : '[unnamed]', nameColumnWidth) + '</b>');
		});

		if (codeContents) {
			codeContents.find('.node').each(function() {
				_Entities.setMouseOver($(this), true);
			});
		}

		var contentBox = $('.CodeMirror');
		contentBox.height('100%');
	},
	moveResizer: function(left) {
		left = left || LSWrapper.getItem(codeResizerLeftKey) || 300;
		$('.column-resizer', codeMain).css({ left: left });

		var contextWidth = 240;
		var width        = $(window).width() - left - contextWidth - 80;

		$('#code-tree').css({width: left - 14 + 'px'});
		$('#code-contents').css({left: left + 8 + 'px', width: width + 'px'});
		$('#code-context').css({left: left + width + 41 + 'px', width: contextWidth + 'px'});
	},
	onload: function() {

		Structr.fetchHtmlTemplate('code/main', {}, function(html) {

			main.append(html);

			// preload action button
			Structr.fetchHtmlTemplate('code/action-button', { }, function(html) {

				_Code.init();

				codeMain     = $('#code-main');
				codeTree     = $('#code-tree');
				codeContents = $('#code-contents');
				codeContext  = $('#code-context');

				_Code.moveResizer();
				Structr.initVerticalSlider($('.column-resizer', codeMain), codeResizerLeftKey, 204, _Code.moveResizer);

				$.jstree.defaults.core.themes.dots      = false;
				$.jstree.defaults.dnd.inside_pos        = 'last';
				$.jstree.defaults.dnd.large_drop_target = true;

				codeTree.on('select_node.jstree', _Code.handleTreeClick);
				codeTree.on('refresh.jstree', _Code.activateLastClicked);

				_Code.loadRecentlyUsedElements(function() {
					_TreeHelper.initTree(codeTree, _Code.treeInitFunction, 'structr-ui-code');
				});

				$(window).off('resize').resize(function() {
					_Code.resize();
				});

				Structr.unblockMenu(100);

				_Code.resize();
				Structr.adaptUiToAvailableFeatures();

				$('#tree-search-input').on('input', _Code.doSearch);
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
			});
		});

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
	dirty: false,
	isDirty: function() {
		return _Code.dirty;
	},
	updateDirtyFlag: function(entity) {

		let formContent = _Code.collectChangedPropertyData(entity);
		let dirty       = Object.keys(formContent).length > 0;

		if (dirty) {
			$('#action-button-save').removeClass('disabled').attr('disabled', null);
			$('#action-button-cancel').removeClass('disabled').attr('disabled', null);
		} else {
			$('#action-button-save').addClass('disabled').attr('disabled', 'disabled');
			$('#action-button-cancel').addClass('disabled').attr('disabled', 'disabled');
		}

		_Code.dirty = (dirty === true);
	},
	testAllowNavigation: function() {
		if (_Code.isDirty()) {
			return confirm('Discard unsaved changes?');
		}

		return true;
	},
	collectPropertyData: function() {

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

		for (let p of document.querySelectorAll('#code-contents div.editor[data-property]')) {
			let cm = p.querySelector('.CodeMirror');
			if (cm) {
				propertyData[p.dataset.property] = p.querySelector('.CodeMirror').CodeMirror.getValue();
			}
		}

		return propertyData;
	},
	collectChangedPropertyData: function(entity) {

		let formContent = _Code.collectPropertyData();
		let keys = Object.keys(formContent);

		for (let key of keys) {
			if (!(formContent[key] !== entity[key] && !((formContent[key] === null && entity[key] === "") || (formContent[key] === "" && entity[key] === null)))) {
				delete formContent[key];
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
					p.value = entity[p.dataset.property];
					break;
			}
		}

		for (let p of document.querySelectorAll('#code-contents div.editor[data-property]')) {
			p.querySelector('.CodeMirror').CodeMirror.setValue(entity[p.dataset.property] || '');
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
		alert('Save action triggered before set up!');
	},
	saveEntityAction:function(entity, callback) {

		if (_Code.isDirty()) {

			let formData        = _Code.collectChangedPropertyData(entity);
			let compileRequired = _Code.isCompileRequiredForSave(formData);

			if (compileRequired) {
				_Code.showSchemaRecompileMessage();
			}

			Command.setProperties(entity.id, formData, function() {
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

		var recentElements = LSWrapper.getItem(_Code.codeRecentElementsKey) || [];

		var promises = recentElements.map(function(recentElement) {
			return new Promise(function(resolve, reject) {
				Command.query('AbstractNode', 1, 1, 'name', true, { id: recentElement.id }, function(res) {
					resolve(res[0]);
				});
			});
		});

		Promise.all(promises).then(foundElements => {

			var updatedRecentElements = [];

			foundElements.forEach(function(entity) {
				if (entity) {
					_Code.addRecentlyUsedElement(entity, true);

					updatedRecentElements.push({id: entity.id});
				}
			});

			LSWrapper.setItem(_Code.codeRecentElementsKey, updatedRecentElements);

		}).then(doneCallback);

	},
	addRecentlyUsedElement: function(entity, fromStorage) {

		var id   = entity.id;
		var name = _Code.getDisplayNameInRecentsForType(entity);
		var icon = _Code.getIconForNodeType(entity);
		var path = _Code.getPathForEntity(entity);

		if (!fromStorage) {

			var recentElements = LSWrapper.getItem(_Code.codeRecentElementsKey) || [];

			var updatedList = recentElements.filter(function(recentElement) {
				return (recentElement.id !== entity.id);
			});
			updatedList.unshift({id: entity.id});

			$('#recently-used-' + entity.id).remove();

			LSWrapper.setItem(_Code.codeRecentElementsKey, updatedList);
		}

		Structr.fetchHtmlTemplate('code/recently-used-button', { id: id, name: name, icon: icon }, function(html) {
			var ctx  = $('#code-context');

			if (fromStorage) {
				ctx.append(html);
			} else {
				ctx.prepend(html);
			}

			$('#recently-used-' + id).on('click.recently-used', function() {
				_Code.findAndOpenNode(path, true);
			});
			$('#remove-recently-used-' + id).on('click.recently-used', function(e) {
				e.stopPropagation();
				_Code.deleteRecentlyUsedElement(id);
			});

		});
	},
	deleteRecentlyUsedElement: function(recentlyUsedElementId) {

		var recentElements = LSWrapper.getItem(_Code.codeRecentElementsKey) || [];

		var filteredRecentElements = recentElements.filter(function(recentElement) {
			return (recentElement.id !== recentlyUsedElementId);
		});
		$('#recently-used-' + recentlyUsedElementId).remove();

		LSWrapper.setItem(_Code.codeRecentElementsKey, filteredRecentElements);

	},
	refreshTree: function() {
		_TreeHelper.refreshTree(codeTree);
		if (_Code.layouter !== null) {
			_Code.layouter.on('click', _Code.handleGraphClick);
		}
	},
	treeInitFunction: function(obj, callback) {

		switch (obj.id) {

			case '#':

				var defaultEntries = [
					{
						id: 'globals',
						text: 'Global Methods',
						children: true,
						icon: _Icons.star_icon
					},
					{
						id: 'root',
						text: 'Types',
						children: [
							{ id: 'custom', text: 'Custom', children: true, icon: _Icons.folder_icon },
							{ id: 'builtin', text: 'Built-In', children: true, icon: _Icons.folder_icon }
						],
						icon: _Icons.structr_logo_small,
						path: '/',
						state: {
							opened: true
						}
					}
				];

				if (_Code.searchIsActive()) {

					callback({
						id: 'search-results',
						text: 'Search Results',
						children: true,
						icon: 'fa fa-search',
						state: {
							opened: true
						}
					});

				} else {

					callback(defaultEntries);
				}
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
			fastRemoveAllChildren($('.searchBox', main));
			fastRemoveAllChildren($('#code-main', main));
		}

		return allow;
	},
	load: function(obj, callback) {

		var id = obj.id;

		var displayFunction = function (result, count, isSearch) {

			var list = [];

			result.forEach(function(entity) {

				// skip HTML entities
				if (entity.category && entity.category === 'html') {
					return;
				}

				var icon     = _Code.getIconForNodeType(entity);
				var treeName = entity.name || '[unnamed]';
				var treeId   = entity.id;

				if (isSearch) {
					treeName = (entity.schemaNode ? entity.schemaNode.name + '.' : '') + entity.name;
					treeId = entity.id + '-' + entity.id + '-search';
				}

				if (entity.type === 'SchemaNode') {

					var data     = {
						id: entity.id,
						type: entity.name,
						name: entity.name
					};
					var children = [];

					// build list of children for this type
					{
						children.push({
							id: 'properties-' + entity.id + '-' + entity.name,
							text: 'Local Attributes',
							children: entity.schemaProperties.length > 0,
							icon: 'fa fa-sliders gray',
							data: data
						});

						children.push({
							id: 'remoteproperties-' + entity.id + '-' + entity.name,
							text: 'Remote Attributes',
							children: ((entity.relatedTo.length + entity.relatedFrom.length) > 0),
							icon: 'fa fa-sliders gray',
							data: data
						});

						children.push({
							id: 'views-' + entity.id + '-' + entity.name,
							text: 'Views',
							children: entity.schemaViews.length > 0,
							icon: 'fa fa-television gray',
							data: data
						});

						children.push({
							id: 'methods-' + entity.id + '-' + entity.name,
							text: 'Methods',
							children: entity.schemaMethods.length > 0,
							icon: 'fa fa-code gray',
							data: data
						});

						children.push({
							id: 'inherited-' + entity.id,
							text: 'Inherited Attributes',
							children: true,
							icon: 'fa fa-sliders gray',
							data: data
						});
					}

					list.push({
						id: treeId,
						text:  treeName,
						children: children,
						icon: 'fa fa-' + icon,
						data: {
							type: entity.type,
							name: entity.name
						}
					});

				} else {

					if (entity.inherited) {

						list.push({
							id: treeId,
							text:  treeName + (' (' + (entity.propertyType || '') + ')'),
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

						var hasVisibleChildren = _Code.hasVisibleChildren(id, entity);
						var name               = treeName;

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
							data: {
								type: entity.type,
								name: entity.name
							}
						});

					}
				}
			});

			list.sort(function(a, b) {
				if (a.text > b.text) { return 1; }
				if (a.text < b.text) { return -1; }
				return 0;
			});

			callback(list);
		};

		switch (id) {

			case 'search-results':
				{
					var text          = $('#tree-search-input').val();
					var searchResults = {};
					var count         = 0;
					var collectFunction = function(result) {
						result.forEach(function(r) {
							searchResults[r.id] = r;
						});
						if (++count === 6) {
							displayFunction(Object.values(searchResults), 0, true);
						}
					};
					Command.query('SchemaNode',     methodPageSize, methodPage, 'name', 'asc', { name: text}, collectFunction, false);
					Command.query('SchemaProperty', methodPageSize, methodPage, 'name', 'asc', { name: text}, collectFunction, false);
					Command.query('SchemaMethod',   methodPageSize, methodPage, 'name', 'asc', { name: text}, collectFunction, false);
					Command.query('SchemaMethod',   methodPageSize, methodPage, 'name', 'asc', { source: text}, collectFunction, false);
					Command.query('SchemaProperty', methodPageSize, methodPage, 'name', 'asc', { writeFunction: text}, collectFunction, false);
					Command.query('SchemaProperty', methodPageSize, methodPage, 'name', 'asc', { readFunction: text}, collectFunction, false);
				}
				break;
			case 'custom':
				Command.query('SchemaNode', methodPageSize, methodPage, 'name', 'asc', { isBuiltinType: false}, displayFunction, true);
				break;
			case 'builtin':
				Command.query('SchemaNode', methodPageSize, methodPage, 'name', 'asc', { isBuiltinType: true }, displayFunction, false);
				break;
			case 'globals':
				Command.query('SchemaMethod', methodPageSize, methodPage, 'name', 'asc', {schemaNode: null}, displayFunction, true, 'ui');
				break;
			default:
				var identifier = _Code.splitIdentifier(id);
				switch (identifier.type) {

					case 'inherited':
						Command.listSchemaProperties(identifier.id, 'custom', function(result) {
							var filtered = result.filter(function(p) {
								return p.declaringClass !== obj.data.type;
							});
							displayFunction(filtered.map(function(s) {

								var builtIn = s.isPartOfBuiltInSchema;

								return {
									id: 'inherited-' + s.declaringUuid + '-' + s.declaringClass + '-' + obj.data.type + '-' + s.name + (builtIn ? '-builtin' : ''),
									type: 'SchemaProperty',
									name: s.declaringClass + '.' + s.name,
									propertyType: s.declaringPropertyType ? s.declaringPropertyType : s.propertyType,
									inherited: true
								};
							}));
						});
						break;
					case 'properties':
						Command.query('SchemaProperty', methodPageSize, methodPage, 'name', 'asc', {schemaNode: identifier.id }, displayFunction, true, 'ui');
						break;
					case 'remoteproperties':
						Command.get(identifier.id, null, (entity) => {

							let mapFn = (rel, out) => {
								let attrName = (out ? (rel.targetJsonName || rel.oldTargetJsonName) : (rel.sourceJsonName || rel.oldSourceJsonName));

								return {
									id: 'remoteproperties-' + entity.id + '-' + entity.name + '-' + attrName,
									type: rel.type,
									name: attrName,
									propertyType: '',
									inherited: false
								};
							};

							let processedRemoteAttributes = [].concat(entity.relatedTo.map((r) => mapFn(r, true))).concat(entity.relatedFrom.map((r) => mapFn(r, false)));

							displayFunction(processedRemoteAttributes);
						});
						break;
					case 'views':
						Command.query('SchemaView', methodPageSize, methodPage, 'name', 'asc', {schemaNode: identifier.id }, displayFunction, true, 'ui');
						break;
					case 'methods':
						Command.query('SchemaMethod', methodPageSize, methodPage, 'name', 'asc', {schemaNode: identifier.id }, displayFunction, true, 'ui');
						break;
					default:
						Command.query('SchemaMethod', methodPageSize, methodPage, 'name', 'asc', {schemaNode: identifier.id}, displayFunction, true, 'ui');
						break;
				}
		}

	},
	clearMainArea: function() {
		fastRemoveAllChildren(codeContents[0]);
		fastRemoveAllChildren($('#code-button-container')[0]);
	},
	fileOrFolderCreationNotification: function (newFileOrFolder) {
		if ((currentWorkingDir === undefined || currentWorkingDir === null) && newFileOrFolder.parent === null) {
			_Code.appendFileOrFolder(newFileOrFolder);
		} else if ((currentWorkingDir !== undefined && currentWorkingDir !== null) && newFileOrFolder.parent && currentWorkingDir.id === newFileOrFolder.parent.id) {
			_Code.appendFileOrFolder(newFileOrFolder);
		}
	},
	registerParentLink: function(d, triggerEl) {

		// Change working dir by click on folder icon
		triggerEl.on('click', function(e) {

			e.preventDefault();
			e.stopPropagation();

			if (d.parentId) {

				codeTree.jstree('open_node', $('#' + d.parentId), function() {
					if (d.name === '..') {
						$('#' + d.parentId + '_anchor').click();
					} else {
						$('#' + d.id + '_anchor').click();
					}
				});

			} else {
				$('#' + d.id + '_anchor').click();
			}

			return false;
		});
	},
	editPropertyContent: function(entity, key, element) {

		var text = entity[key] || '';

		var contentBox = $('.editor', element);
		var editor = CodeMirror(contentBox.get(0), Structr.getCodeMirrorSettings({
			value: text,
			mode: _Code.getEditorModeForContent(text),
			lineNumbers: true,
			lineWrapping: false,
			indentUnit: 4,
			tabSize: 4,
			indentWithTabs: true,
			extraKeys: {
				"Ctrl-Space": "autocomplete"
			}
		}));

		_Code.setupAutocompletion(editor, entity.id, true);

		var scrollInfo = JSON.parse(LSWrapper.getItem(scrollInfoKey + '_' + entity.id));
		if (scrollInfo) {
			editor.scrollTo(scrollInfo.left, scrollInfo.top);
		}

		editor.on('scroll', function() {
			var scrollInfo = editor.getScrollInfo();
			LSWrapper.setItem(scrollInfoKey + '_' + entity.id, JSON.stringify(scrollInfo));
		});

		if (entity.codeType === 'java') {

			editor.setOption('mode', 'text/x-java');

		} else {

			editor.on('change', function() {
				var type = _Code.getEditorModeForContent(editor.getValue());
				var prev = editor.getOption('mode');
				if (prev !== type) {
					editor.setOption('mode', type);
				}
			});
		}

		editor.id = entity.id;

		editor.on('change', function(cm, change) {
			_Code.updateDirtyFlag(entity);
		});

		_Code.resize();

		editor.refresh();

		_Code.displayDefaultMethodOptions(entity);
	},
	displayCreateButtons: function(showCreateMethodsButton, showCreateGlobalButton, showCreateTypeButton, schemaNodeId) {

		if (showCreateMethodsButton) {

			Structr.fetchHtmlTemplate('code/method-button', {}, function(html) {

				$('#code-button-container').append(html);

				$('#add-method-button').on('click', function() {
					_Code.createMethodAndRefreshTree('SchemaMethod', $('#schema-method-name').val() || 'unnamed', schemaNodeId);
				});

				$('#add-onCreate-button').on('click', function() {
					_Code.createMethodAndRefreshTree('SchemaMethod', 'onCreate', schemaNodeId);
				});

				$('#add-onSave-button').on('click', function() {
					_Code.createMethodAndRefreshTree('SchemaMethod', 'onSave', schemaNodeId);
				});
			});

		}

		if (showCreateGlobalButton) {

			Structr.fetchHtmlTemplate('code/global-button', {}, function(html) {

				$('#code-button-container').append(html);

				$('#create-global-method-button').on('click', function() {
					_Code.createMethodAndRefreshTree('SchemaMethod', $('#schema-method-name').val() || 'unnamed');
				});
			});

		}

		if (showCreateTypeButton) {

			Structr.fetchHtmlTemplate('code/type-button', {}, function(html) {

				$('#code-button-container').append(html);

				$('#add-type-button').on('click', function() {
					_Code.createMethodAndRefreshTree('SchemaNode', $('#schema-type-name').val());
				});
			});

		}
	},
	createMethodAndRefreshTree: function(type, name, schemaNode) {

		_Code.showSchemaRecompileMessage();
		Command.create({
			type: type,
			name: name,
			schemaNode: schemaNode,
			source: ''
		}, function() {
			_TreeHelper.refreshTree('#code-tree');
			_Code.hideSchemaRecompileMessage();
		});
	},
	getDisplayNameInRecentsForType: function (entity) {

		var name = entity.name;

		switch (entity.type) {
			case 'SchemaNode':
				name = 'Class ' + entity.name;
				break;
			case 'SchemaMethod':
				if (entity.schemaNode && entity.schemaNode.name) {
					name = entity.schemaNode.name + '.' + entity.name + '()';
				} else {
					name = entity.name + '()';
				}
				break;
			case 'SchemaProperty':
				if (entity.schemaNode && entity.schemaNode.name) {
					name = entity.schemaNode.name + '.' + entity.name;
				}
				break;
		}

		return name;
	},
	getIconForNodeType: function(entity) {

		// set global default
		var icon = 'file-code-o gray';

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

		var icon = 'exclamation-triangle';

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
		}

		return icon;
	},
	splitIdentifier: function(id) {

		var parts = id.split('-');
		if (parts.length >= 2) {

			switch (parts.length) {
				//inherited-8358fbdd264a42c79771c64f12b8878a-BaseEntity-Wette-labels

				case 6:
					var builtin  = "builtin" === parts[5].trim();
				case 5:
					var property = parts[4].trim();
				case 4:
					var extended = parts[3].trim();
				case 3:
					var base     = parts[2].trim();
				case 2:
					var entity   = parts[1].trim();
					var subtype  = parts[0].trim();
			}

			return { id: entity, type: subtype, base: base, extended: extended, property: property, isBuiltinType: builtin };
		}

		return { id : id, type: id };
	},
	convertOutgoingRelationshipNodesForTree: function(related) {

		var list = [];

		related.forEach(function(rel) {

			list.push({
				id: 'out-' + rel.id,
				text: rel.targetJsonName + ': ' + rel.targetMultiplicity,
				children: false,
				icon: 'fa fa-arrow-right gray'
			});
		});

		return list;
	},
	convertIncomingRelationshipNodesForTree: function(related) {

		var list = [];

		related.forEach(function(rel) {

			list.push({
				id: 'in-' + rel.id,
				text: rel.sourceJsonName + ': ' + rel.sourceMultiplicity,
				children: false,
				icon: 'fa fa-arrow-left gray'
			});
		});

		return list;
	},
	hasVisibleChildren: function(id, entity) {

		var hasVisibleChildren = false;

		if (entity.schemaMethods) {

			entity.schemaMethods.forEach(function(m) {

				if (id === 'custom' || !m.isPartOfBuiltInSchema) {

					hasVisibleChildren = true;
				}
			});
		}

		return hasVisibleChildren;
	},
	handleTreeClick: function(evt, data) {

		if (data && data.node && data.node.id) {

			var selection = {
				id: data.node.id,
				updateLocationStack: true
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
	handleSelection: function(data) {

		if (_Code.testAllowNavigation()) {

			_Code.dirty = false;

			// clear page
			_Code.clearMainArea();

			var identifier = _Code.splitIdentifier(data.id);
			switch (identifier.type) {

				// global types that are not associated with an actual entity
				case 'core':
				case 'html':
				case 'ui':
				case 'web':
					_Code.displayContent(identifier.type);
					break;

				case 'root':
					_Code.displayRootContent();
					break;

				case 'globals':
					_Code.displayGlobalMethodsContent();
					break;

				case 'custom':
					_Code.displayCustomTypesContent(data);
					break;

				case 'builtin':
					_Code.displayBuiltInTypesContent(identifier.type);
					break;

				// properties (with uuid)
				case 'properties':
					_Code.displayPropertiesContent(identifier, data.updateLocationStack);
					break;

				// remoteproperties (with uuid)
				case 'remoteproperties':
					_Code.displayRemotePropertiesContent(identifier, data.updateLocationStack);
					break;

				// views (with uuid)
				case 'views':
					_Code.displayViewsContent(identifier, data.updateLocationStack);
					break;

				// methods (with uuid)
				case 'methods':
					_Code.displayMethodsContent(identifier, data.updateLocationStack);
					break;

				// outgoing relationships (with uuid)
				case 'outgoing':
					_Code.displayOutgoingRelationshipsContent(identifier);
					break;

				// incoming relationships (with uuid)
				case 'incoming':
					_Code.displayIncomingRelationshipsContent(identifier);
					break;

				// outgoing relationship (with uuid)
				case 'out':
					_Code.displayOutRelationshipContent(identifier);
					break;

				// incoming relationship (with uuid)
				case 'in':
					_Code.displayInRelationshipContent(identifier);
					break;

				case 'inherited':
					if (identifier.isBuiltinType) {
						_Code.findAndOpenNode('Types/Built-In/' + identifier.base + '/Properties/' + identifier.property, true);
					} else {
						_Code.findAndOpenNode('Types/Custom/' + identifier.base + '/Properties/' + identifier.property, true);
					}
					break;

				// other (click on an actual object)
				default:
					_Code.handleNodeObjectClick(data);
					break;
			}
		}
	},
	handleNodeObjectClick: function(data) {

		var identifier = _Code.splitIdentifier(data.id);

		if (data.type) {

			switch (data.type) {

				case 'SchemaView':
					_Code.displayViewDetails(data);
					break;

				case 'SchemaProperty':
					_Code.displayPropertyDetails(data);
					break;

				case 'SchemaMethod':
					Command.get(identifier.id, null, function(result) {
						_Code.updateRecentlyUsed(result, data.updateLocationStack);
						Structr.fetchHtmlTemplate('code/method', { method: result }, function(html) {
							fastRemoveAllChildren(codeContents[0]);
							codeContents.append(html);

							LSWrapper.setItem(codeLastOpenMethodKey, result.id);
							_Code.editPropertyContent(result, 'source', codeContents);
						});
					});
					break;

				case 'SchemaNode':
					_Code.displaySchemaNodeContent(data);
					break;
			}

		} else {

			switch (identifier.id) {

				case 'globals':
					_Code.displayCreateButtons(false, true, false, '');
					break;

				case 'custom':
					_Code.displayCreateButtons(false, false, true, '');
					break;
			}
		}
	},
	displaySchemaNodeContent: function(data) {

		var identifier = _Code.splitIdentifier(data.id);

		if (_Code.layouter) {
			_Code.layouter.focus(identifier.id);
			return;
		}

		Command.get(identifier.id, null, function(result) {

			_Code.updateRecentlyUsed(result, data.updateLocationStack);
			Structr.fetchHtmlTemplate('code/type', { type: result }, function(html) {

				fastRemoveAllChildren(codeContents[0]);
				codeContents.append(html);

				var propertyData   = { type: 'SchemaProperty', schemaNode: identifier.id };
				var methodData     = { type: 'SchemaMethod', schemaNode: identifier.id };
				var methodParent   = '#method-actions';

				// delete button
				if (!result.isBuiltinType) {
					_Code.displayActionButton('#type-actions', _Icons.getFullSpriteClass(_Icons.delete_icon), 'delete', 'Delete type', function() {
						_Code.deleteSchemaEntity(result, 'Delete type ' + result.name + '?', 'This will delete all schema relationships as well, but no data will be removed.');
					});
				}

				_Code.displayCreateButton('#view-actions', 'fa fa-tv', 'new-view', 'Add view', '', { type: 'SchemaView', schemaNode: result.id });

				_Code.displayCreateMethodButton(methodParent, 'onCreate', methodData, 'onCreate');
				_Code.displayCreateMethodButton(methodParent, 'onSave',   methodData, 'onSave');
				_Code.displayCreateMethodButton(methodParent, 'schema',   methodData, '');
			});
		});
	},
	showGeneratedSource: function() {

		let sourceContainer = document.getElementById('generated-source-code');
		if (sourceContainer) {

			let typeId = sourceContainer.dataset.typeId;

			if (typeId) {

				$.ajax({
					url: '/structr/rest/SchemaNode/' + typeId + '/getGeneratedSourceCode',
					method: 'post',
					statusCode: {
						200: function(result) {

							var container = $(sourceContainer);

							var editor    = CodeMirror(container[0], Structr.getCodeMirrorSettings({
								value: result.result,
								mode: 'text/x-java',
								lineNumbers: true,
								readOnly: true
							}));

							$('.CodeMirror').height('100%');
							editor.refresh();
						}
					}
				});
			}
		}
	},
	displayContent: function(templateName) {
		Structr.fetchHtmlTemplate('code/' + templateName, { }, function(html) {
			codeContents.append(html);
		});
	},
	random: function() {
	    var x = Math.sin(_Code.seed++) * 10000;
	    return x - Math.floor(x);
	},
	displayRootContent: function() {

		var hiddenSchemaNodes = JSON.parse(LSWrapper.getItem(_Schema.hiddenSchemaNodesKey));
		if (hiddenSchemaNodes === null) {

			_Schema.updateHiddenSchemaNodes();

			hiddenSchemaNodes = JSON.parse(LSWrapper.getItem(_Schema.hiddenSchemaNodesKey));
		}

		Structr.fetchHtmlTemplate('code/root', { }, function(html) {

			fastRemoveAllChildren(codeContents[0]);
			codeContents.append(html);

			var layouter          = new SigmaLayouter('all-types');

			Command.query('SchemaNode', 10000, 1, 'name', 'asc', { }, function(result1) {

				result1.forEach(function(node) {

					if (hiddenSchemaNodes === null || hiddenSchemaNodes.indexOf(node.name) === -1) {
						layouter.addNode(node);
					}
				});

				Command.query('SchemaRelationshipNode', 10000, 1, 'name', 'asc', { }, function(result2) {

					result2.forEach(function(r) {
						layouter.addEdge(r.id, r.relationshipType, r.sourceId, r.targetId, true);
					});

					layouter.refresh();
					layouter.layout();
					layouter.on('click', _Code.handleGraphClick);

					_Code.layouter = layouter;

				}, true, 'ui');

			}, true, 'ui');
		});
	},
	handleGraphClick: function(data) {

		if (data.nodes.length === 1) {

			Command.get(data.nodes[0], null, function(result) {
				_Code.findAndOpenNode(_Code.getPathForEntity(result), false, false);
				_Code.displaySchemaNodeContext(result);
			});
		}

	},
	displaySchemaNodeContext:function(entity) {

		Structr.fetchHtmlTemplate('code/type-context', { entity: entity }, function(html) {

			fastRemoveAllChildren(codeContext[0]);
			codeContext.append(html);

			$('#schema-node-name').off('blur').on('blur', function() {

				var name = $(this).val();

				_Code.showSchemaRecompileMessage();

				Command.setProperty(entity.id, 'name', name, false, function() {

					_Code.layouter.getNodes().update({ id: entity.id, label: '<b>' + name + '</b>' });
					_Code.hideSchemaRecompileMessage();
					_Code.refreshTree();
				});
			});
		});

	},
	displayCustomTypesContent: function(data) {
		Structr.fetchHtmlTemplate('code/custom', { }, function(html) {
			fastRemoveAllChildren(codeContents[0]);
			codeContents.append(html);

			// create button
			_Code.displayCreateTypeButton("#type-actions");

			// list of existing custom types
			Command.query('SchemaNode', 10000, 1, 'name', 'asc', { isBuiltinType: false }, function(result) {
				result.forEach(function(t) {
					_Code.displayActionButton('#existing-types', 'fa fa-file-code-o', t.id, t.name, function() {
						_Code.findAndOpenNode('Types/Custom/' + t.name);
					});
				});
			}, true);
		});
	},
	displayBuiltInTypesContent: function() {
		Structr.fetchHtmlTemplate('code/builtin', { }, function(html) {
			fastRemoveAllChildren(codeContents[0]);
			codeContents.append(html);
			var container = $('#builtin-types');
			Command.query('SchemaNode', 10000, 1, 'name', 'asc', { isBuiltinType: true}, function(result) {
				result.forEach(function(t) {
					if (t.category && t.category === 'html') {
						return;
					}
					_Code.displayTypeContent(container, t.id, t.name, 'file-code-o', 'Types/Built-In/' + t.name);
				});
			}, true);
		});
	},
	displayGlobalMethodsContent: function() {
		Structr.fetchHtmlTemplate('code/globals', { }, function(html) {
			fastRemoveAllChildren(codeContents[0]);
			codeContents.append(html);
			_Code.displayCreateButton('#method-actions', 'fa fa-magic', 'new', 'Add global schema method', '', { type: 'SchemaMethod' });
		});
	},
	displayPropertiesContent: function(selection, updateLocationStack) {

		var path = 'Types/' + _Code.getPathComponent(selection) + '/' + selection.base + '/Properties';

		if (updateLocationStack === true) {
			_Code.updatePathLocationStack(path);
			_Code.lastClickedPath = path;
		}

		Command.get(selection.id, null, (entity) => {
			_Schema.properties.appendLocalProperties(codeContents, entity);
		});
	},
	displayRemotePropertiesContent: function (selection, updateLocationStack) {

		var path = 'Types/' + _Code.getPathComponent(selection) + '/' + selection.base + '/RemoteProperties';

		if (updateLocationStack === true) {
			_Code.updatePathLocationStack(path);
			_Code.lastClickedPath = path;
		}

		Command.get(selection.id, null, (entity) => {
			_Schema.remoteProperties.appendRemote(codeContents, entity, _Code.schemaNodes);
		});

	},
	displayViewsContent: function(selection, updateLocationStack) {

		var path = 'Types/' + _Code.getPathComponent(selection) + '/' + selection.base + '/Views';

		if (updateLocationStack === true) {
			_Code.updatePathLocationStack(path);
			_Code.lastClickedPath = path;
		}

		Structr.fetchHtmlTemplate('code/views', { identifier: selection }, function(html) {
			fastRemoveAllChildren(codeContents[0]);
			codeContents.append(html);

			var callback = function() { _Code.displayViewsContent(selection); };
			var data     = { type: 'SchemaViews', schemaNode: selection.id };

			_Code.displayCreateButton('#view-actions', 'fa fa-tv', 'new-view', 'Add view', '', data, callback);

			// list of existing properties
			Command.query('SchemaView', 10000, 1, 'name', 'asc', { schemaNode: selection.id }, function(result) {
				result.forEach(function(t) {
					_Code.displayActionButton('#existing-views', 'fa fa-' + _Code.getIconForNodeType(t), t.id, t.name, function() {
						_Code.findAndOpenNode(path + '/' + t.name);
					});
				});
			}, true);
		});
	},
	displayMethodsContent: function(identifier, updateLocationStack) {

		var path = 'Types/' + _Code.getPathComponent(identifier) + '/' + identifier.base + '/Methods';

		if (updateLocationStack === true) {
			_Code.updatePathLocationStack(path);
			_Code.lastClickedPath = path;
		}

		Structr.fetchHtmlTemplate('code/methods', { identifier: identifier }, function(html) {
			fastRemoveAllChildren(codeContents[0]);
			codeContents.append(html);
			var data     = { type: 'SchemaMethod', schemaNode: identifier.id };
			var containerId = '#method-actions';

			_Code.displayCreateButton(containerId, 'fa fa-magic', 'on-create',    'Add onCreate method',    'onCreate',    data);
			_Code.displayCreateButton(containerId, 'fa fa-magic', 'after-create', 'Add afterCreate method', 'afterCreate', data);
			_Code.displayCreateButton(containerId, 'fa fa-magic', 'on-save',      'Add onSave method',      'onSave',      data);
			_Code.displayCreateButton(containerId, 'fa fa-magic', 'new',          'Add schema method',      '',            data);

			// list of existing properties
			Command.query('SchemaMethod', 10000, 1, 'name', 'asc', { schemaNode: identifier.id }, function(result) {
				result.forEach(function(t) {
					_Code.displayActionButton('#existing-methods', 'fa fa-' + _Code.getIconForNodeType(t), t.id, t.name, function() {
						_Code.findAndOpenNode(path + '/' + t.name);
					});
				});
			}, true);
		});
	},
	displayOutgoingRelationshipsContent: function(identifier) {
		Structr.fetchHtmlTemplate('code/outgoing-relationships', { identifier: identifier }, function(html) {
			codeContents.append(html);
		});
	},
	displayIncomingRelationshipsContent: function(identifier) {
		Structr.fetchHtmlTemplate('code/incoming-relationships', { identifier: identifier }, function(html) {
			codeContents.append(html);
		});
	},
	displayOutRelationshipContent: function(identifier) {
		Structr.fetchHtmlTemplate('code/outgoing-relationship', { identifier: identifier }, function(html) {
			codeContents.append(html);
		});
	},
	displayInRelationshipContent: function(identifier) {
		Structr.fetchHtmlTemplate('code/incoming-relationship', { identifier: identifier }, function(html) {
			codeContents.append(html);
		});
	},
	displayPropertyDetails: function(selection) {

		var id = _Code.splitIdentifier(selection.id);

		Command.get(id.id, null, function(result) {

			_Code.updateRecentlyUsed(result, selection.updateLocationStack);

			if (result.propertyType) {

				switch (result.propertyType) {
					case 'Cypher':
						_Code.displayCypherPropertyDetails(result);
						break;

					case 'Function':
						_Code.displayFunctionPropertyDetails(result);
						break;

					case 'String':
						_Code.displayStringPropertyDetails(result);
						break;

					case 'Boolean':
						_Code.displayBooleanPropertyDetails(result);
						break;

					default:
						_Code.displayDefaultPropertyDetails(result);
						break;
				}

			} else {

				if (result.type === 'SchemaRelationshipNode') {
					// this is a remote property/adjacent type
					// _Code.displayRelationshipPropertyDetails(result);
				}
			}
		});
	},
	displayViewDetails: function(selection) {

		var id = _Code.splitIdentifier(selection.id);

		Command.get(id.id, null, function(result) {

			_Code.updateRecentlyUsed(result, selection.updateLocationStack);

			Structr.fetchHtmlTemplate('code/default-view', { view: result }, function(html) {
				fastRemoveAllChildren(codeContents[0]);
				codeContents.append(html);
				_Code.displayDefaultViewOptions(result);
			});
		});
	},
	displayFunctionPropertyDetails: function(property) {
		Structr.fetchHtmlTemplate('code/function-property', { property: property }, function(html) {

			fastRemoveAllChildren(codeContents[0]);
			codeContents.append(html);

			Structr.activateCommentsInElement(codeContents, {insertAfter: true});

			_Code.editPropertyContent(property, 'readFunction',  $('#read-code-container'));
			_Code.editPropertyContent(property, 'writeFunction', $('#write-code-container'));
			_Code.displayDefaultPropertyOptions(property);
		});
	},
	displayCypherPropertyDetails: function(property) {

		Structr.fetchHtmlTemplate('code/cypher-property', { property: property }, function(html) {
			fastRemoveAllChildren(codeContents[0]);
			codeContents.append(html);

			_Code.editPropertyContent(property, 'format', $('#cypher-code-container'));
			_Code.displayDefaultPropertyOptions(property);
		});
	},
	displayStringPropertyDetails: function(property) {

		Structr.fetchHtmlTemplate('code/string-property', { property: property }, function(html) {
			fastRemoveAllChildren(codeContents[0]);
			codeContents.append(html);
			_Code.displayDefaultPropertyOptions(property);
		});
	},
	displayBooleanPropertyDetails: function(property) {

		Structr.fetchHtmlTemplate('code/boolean-property', { property: property }, function(html) {
			fastRemoveAllChildren(codeContents[0]);
			codeContents.append(html);
			_Code.displayDefaultPropertyOptions(property);
		});
	},
	displayDefaultPropertyDetails: function(property) {

		Structr.fetchHtmlTemplate('code/default-property', { property: property }, function(html) {
			fastRemoveAllChildren(codeContents[0]);
			codeContents.append(html);
			_Code.displayDefaultPropertyOptions(property);
		});
	},
	displayDefaultPropertyOptions: function(property, callback) {

		// default buttons
		Structr.fetchHtmlTemplate('code/property-options', { property: property }, function(html) {

			_Code.runCurrentEntitySaveAction = function() {
				_Code.saveEntityAction(property);
			};

			var buttons = $('#property-buttons');
			buttons.prepend(html);

			_Code.displayActionButton('#property-actions', _Icons.getFullSpriteClass(_Icons.floppy_icon), 'save', 'Save property', _Code.runCurrentEntitySaveAction);

			_Code.displayActionButton('#property-actions', _Icons.getFullSpriteClass(_Icons.cross_icon), 'cancel', 'Revert changes', function() {
				_Code.revertFormData(property);
			});

			// delete button
			if (!property.schemaNode.isBuiltinType) {

				_Code.displayActionButton('#property-actions', _Icons.getFullSpriteClass(_Icons.delete_icon), 'delete', 'Delete property', function() {
					_Code.deleteSchemaEntity(property, 'Delete property ' + property.name + '?', 'No data will be removed.');
				});
			}

			_Code.updateDirtyFlag(property);

			if (property.propertyType !== 'Function') {
				$('#property-type-hint-input').parent().remove();
			}

			if (property.propertyType === 'Cypher') {
				$('#property-format-input').parent().remove();
			}

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
					_Code.deleteSchemaEntity(property, 'Delete property ' + property.name + '?', 'Property values will not be removed from data nodes.');
				});
			}

			if (typeof callback === 'function') {
				callback();
			}
		});
	},
	displayDefaultViewOptions: function(view, callback) {

		// default buttons
		Structr.fetchHtmlTemplate('code/view-options', { view: view }, function(html) {

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

			var buttons = $('#view-buttons');
			buttons.prepend(html);

			_Code.displayActionButton('#view-actions', _Icons.getFullSpriteClass(_Icons.floppy_icon), 'save', 'Save view', _Code.runCurrentEntitySaveAction);

			_Code.displayActionButton('#view-actions', _Icons.getFullSpriteClass(_Icons.cross_icon), 'cancel', 'Revert changes', function() {
				_Code.revertFormData(view);
				_Code.displayViewSelect(view);
			});

			// delete button
			let removeIcon = (view.isBuiltinView ? _Icons.arrow_undo_icon : _Icons.delete_icon);
			let removeText = (view.isBuiltinView ? 'Reset view' : 'Delete view');
			_Code.displayActionButton('#view-actions', _Icons.getFullSpriteClass(removeIcon), 'delete', removeText, function() {
				_Code.deleteSchemaEntity(view, removeText + ' ' + view.name + '?', 'No data will be removed.');
			});

			_Code.updateDirtyFlag(view);

			$('input[type=text]', buttons).on('keyup', function() {
				_Code.updateDirtyFlag(view);
			});

			_Code.displayViewSelect(view);

			if (typeof callback === 'function') {
				callback();
			}
		});
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
				// schedule because otherwise the sortable does not know something has been removed
				window.setTimeout(()=> {
					changeFn();
				}, 0);
			}).chosenSortable(function() {
				changeFn();
			});
		});
	},
	displayDefaultMethodOptions: function(method, callback) {

		// default buttons
		Structr.fetchHtmlTemplate('code/method-options', { method: method }, function(html) {

			_Code.runCurrentEntitySaveAction = function() {
				_Code.saveEntityAction(method);
			};

			var buttons = $('#method-buttons');
			buttons.prepend(html);

			_Code.displayActionButton('#method-actions', _Icons.getFullSpriteClass(_Icons.floppy_icon), 'save', 'Save method', _Code.runCurrentEntitySaveAction);

			_Code.displayActionButton('#method-actions', _Icons.getFullSpriteClass(_Icons.cross_icon), 'cancel', 'Revert changes', function() {
				_Code.revertFormData(method);
			});

			// delete button
			if (!method.schemaNode || !method.schemaNode.isBuiltinType) {
				_Code.displayActionButton('#method-actions', _Icons.getFullSpriteClass(_Icons.delete_icon), 'delete', 'Delete method', function() {
					_Code.deleteSchemaEntity(method, 'Delete method ' + method.name + '?');
				});
			}

			// run button
			if (!method.schemaNode && !method.isPartOfBuiltInSchema) {
				_Code.displayActionButton('#method-actions', _Icons.getFullSpriteClass(_Icons.exec_blue_icon), 'run', 'Run method', function() {
					_Code.runGlobalSchemaMethod(method);
				});
			}

			_Code.updateDirtyFlag(method);

			$('input[type=checkbox]', buttons).on('change', function() {
				_Code.updateDirtyFlag(method);
			});

			$('input[type=text]', buttons).on('keyup', function() {
				_Code.updateDirtyFlag(method);
			});

			if (typeof callback === 'function') {
				callback();
			}
		});
	},
	lockPropertyOptions: function() {
		$('#property-options').find('input').attr('disabled', true);
	},
	unlockPropertyOptions: function() {
		$('#property-options').find('input').attr('disabled', false);
	},
	activatePropertyValueInput: function(inputId, id, name) {
		$('input#' + inputId).on('blur', function() {

			if (_Code.testAllowNavigation()) {

				var elem     = $(this);
				var previous = elem.attr('value');
				if (previous !== elem.val()) {
					var data   = {};
					data[name] = elem.val();
					_Code.lockPropertyOptions();
					_Code.showSchemaRecompileMessage();
					Command.setProperties(id, data, function() {
						_Code.unlockPropertyOptions();
						blinkGreen(elem);
						_Code.refreshTree();
						_Code.hideSchemaRecompileMessage();
					});
				}
			}
		});
	},
	setupAutocompletion: function(editor, id, isAutoscriptEnv) {

		CodeMirror.registerHelper('hint', 'ajax', (editor, callback) => _Code.getAutocompleteHint(editor, id, isAutoscriptEnv, callback));
		CodeMirror.hint.ajax.async = true;
		CodeMirror.commands.autocomplete = function(mirror) { mirror.showHint({ hint: CodeMirror.hint.ajax }); };
		editor.on('keyup', (instance, event) => {
			switch (event.key) {

				case "'":
				case '"':
				case '.':
				case '(':
					CodeMirror.commands.autocomplete(instance, null, {completeSingle: false});
			}
		});
	},
	getAutocompleteHint: function(editor, id, isAutoscriptEnv, callback) {

		var cursor = editor.getCursor();
		var before = editor.getRange({ line: 0, ch: 0 }, cursor);
		var after  = editor.getRange(cursor, { line: cursor.line + 1, ch: 0 });
		var type   = _Code.getEditorModeForContent(editor.getValue());
		Command.autocomplete(id, isAutoscriptEnv, before, after, cursor.line, cursor.ch, type, function(result) {
			var inner  = { from: cursor, to: cursor, list: result };
			callback(inner);
		});
	},
	activateCreateDialog: function(suffix, presetValue, nodeData, elHtml) {

		var button = $('button#action-button-' + suffix);

		var revertFunction = function () {
			button.replaceWith(elHtml);
			_Code.activateCreateDialog(suffix, presetValue, nodeData, elHtml);
		};

		button.on('click.create-object-' + suffix, function() {

			button.off('click');
			button.addClass('action-button-open');

			Structr.fetchHtmlTemplate('code/create-object-form', { value: presetValue, suffix: suffix }, function(html) {
				button.append(html);
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
						_Code.displayCustomTypesContent();
						_Code.hideSchemaRecompileMessage();
					});
				});
			});
		});
	},
	displayCreateButton: function(targetId, iconClass, suffix, name, presetValue, createData) {
		Structr.fetchHtmlTemplate('code/action-button', { iconClass: iconClass, suffix: suffix, name: name }, function(html) {
			$(targetId).append(html);
			_Code.activateCreateDialog(suffix, presetValue, createData, html);
		});
	},
	displayActionButton: function(targetId, iconClass, suffix, name, callback) {
		let buttonId = '#action-button-' + suffix;
		Structr.fetchHtmlTemplate('code/action-button', { iconClass: iconClass, suffix: suffix, name: name }, function(html) {
			$(targetId).append(html);
			$(buttonId).off('click.action').on('click.action', callback);
		});
		return buttonId;
	},
	displayCreatePropertyButton: function(targetId, type, nodeData) {
		var data = Object.assign({}, nodeData);
		data['propertyType'] = type;
		if (type === 'Enum') {
			data.format = 'value1, value2, value3';
		}
		_Code.displayCreateButton(targetId, 'fa fa-' + _Code.getIconForPropertyType(type), type.toLowerCase(), 'Add ' + type + ' property', '', data);
	},
	displayCreateMethodButton: function(targetId, type, data, presetValue) {
		_Code.displayCreateButton(targetId, 'fa fa-' + _Code.getIconForNodeType(data), type.toLowerCase(), 'Add ' + type + ' method', presetValue, data);
	},
	displayCreateTypeButton: function(targetId) {
		_Code.displayCreateButton(targetId, 'fa fa-magic', 'create-type', 'Create new type', '', { type: 'SchemaNode'});
	},
	getEditorModeForContent: function(content) {
		if (content && (content.indexOf('{') === 0 || content.indexOf("${{") === 0)) {
			return 'text/javascript';
		}
		return 'text';
	},
	updateRecentlyUsed: function(entity, updateLocationStack) {

		_Code.addRecentlyUsedElement(entity);

		var path = _Code.getPathForEntity(entity);

		if (updateLocationStack) {
			_Code.updatePathLocationStack(path);
			_Code.lastClickedPath = path;
		}
	},
	findAndOpenNode: function(path, updateLocationStack) {
		var tree = $('#code-tree').jstree(true);
		_Code.findAndOpenNodeRecursive(tree, path, 0, undefined, updateLocationStack);
	},
	findAndOpenNodeRecursive: function(tree, path, depth, node, updateLocationStack) {
		var parts = path.split('/');
		if (path.length === 0) { return; }
		if (parts.length < 1) {	return; }
		if (depth > 15) { return; }

		var pos  = path.indexOf('/', 1);
		var tail = pos >= 0 ? path.substring(pos + 1) : '';
		var id   = parts[0];

		if (id.length === 0) {
			id  = parts[1];
		}

		var data     = tree.get_json(node);
		var searchId = _Code.findNodeIdByText(data, id);

		if (tail.length === 0) {

			// node found, activate
			if (tree.get_selected().indexOf(searchId) === -1) {
				tree.activate_node(searchId, { updateLocationStack: updateLocationStack });
			}

			// also scroll into view if node is in tree
			var domNode = document.getElementById( tree.get_parent(tree.get_parent(searchId)) );
			if (domNode) {

				var rect = domNode.getBoundingClientRect();
				if (rect.bottom > window.innerHeight) {

					domNode.scrollIntoView(false);
				}

				if (rect.top < 0) {
					domNode.scrollIntoView();
				}

			}

		} else {

			tree.open_node(searchId, function(n) {
				_Code.findAndOpenNodeRecursive(tree, tail, depth + 1, n, updateLocationStack);
			});
		}
	},
	findNodeIdByText: function(data, text, path) {

		if (data && data.length) {

			for (var i=0; i<data.length; i++) {

				var result = _Code.findNodeIdByTextRecursive(data[i], text, path);
				if (result) {

					return result;
				}
			}

		} else {

			return _Code.findNodeIdByTextRecursive(data, text, path);
		}
	},
	findNodeIdByTextRecursive: function(data, text, path) {

		if (data.data && data.data.name && data.data.name === text) {
			return data.id;
		}

		if (data.id && data.id === text) {
			return data.id;
		}

		//if (data.text && data.text.indexOf(text) >= 0) {
		if (data.text && data.text === text) {

			return data.id;

		} else if (data.children) {

			var children = data.children;
			for (var i=0; i<children.length; i++) {

				var result = _Code.findNodeIdByTextRecursive(children[i], text, path);
				if (result) {

					if (path) {
						path.unshift(data.text);
					}

					return result;
				}
			}
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

		var stackSize       = _Code.pathLocationStack.length;
		var forwardDisabled = stackSize <= 1 || _Code.pathLocationIndex >= stackSize - 1;
		var backDisabled    = stackSize <= 1 || _Code.pathLocationIndex <= 0;

		$('#tree-forward-button').prop('disabled', forwardDisabled);
		$('#tree-back-button').prop('disabled', backDisabled);
	},
	doSearch: function(e) {
		var tree      = $('#code-tree').jstree(true);
		var input     = $('#tree-search-input');
		var text      = input.val();
		var threshold = _Code.searchThreshold;

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
	searchIsActive: function() {
		var text = $('#tree-search-input').val();
		return text && text.length >= _Code.searchThreshold;
	},
	cancelSearch: function() {
		$('#tree-search-input').val('');
		$('#tree-search-input').trigger('input');
	},
	activateLastClicked: function() {
		_Code.findAndOpenNode(_Code.lastClickedPath);
	},
	getPathForEntity: function(entity) {

		var getPathComponent = function(e) {

			if (e && e.isBuiltinType) {
				return 'Built-In';
			};

			return 'Custom';
		};

		var path = [];

		switch (entity.type) {

			case 'SchemaNode':
				path.push('Types');
				path.push(getPathComponent(entity));
				path.push(entity.name);
				break;


			case 'SchemaProperty':
				path.push('Types');
				path.push(getPathComponent(entity.schemaNode));
				path.push(entity.schemaNode.name);
				path.push('Properties');
				path.push(entity.name);
				break;

			case 'SchemaMethod':
				if (entity.schemaNode) {
					path.push('Types');
					path.push(getPathComponent(entity.schemaNode));
					path.push(entity.schemaNode.name);
					path.push('Methods');
					path.push(entity.name);
				} else {
					path.push('Global Methods');
					path.push(entity.name);
				}
				break;

			case 'SchemaView':
				path.push('Types');
				path.push(getPathComponent(entity));
				path.push(entity.schemaNode.name);
				path.push('Views');
				path.push(entity.name);
				break;
		}

		return path.join('/');
	},
	getPathComponent: function(selection) {
		if (selection.isBuiltinType) {
			return 'Built-In';
		}
		return 'Custom';
	},
	deleteSchemaEntity: function(entity, title, text) {

		var parentPath = _Code.getPathForEntity(entity);
		var components = parentPath.split('/');

		// remove last component from path
		components.pop();
		parentPath = components.join('/');

		Structr.confirmation('<h3>' + title + '</h3><p>' + (text || '') + '</p>',
			function() {
				$.unblockUI({
					fadeOut: 25
				});
				_Code.showSchemaRecompileMessage();
				_Code.dirty = false;

				Command.deleteNode(entity.id, false, function() {
					_Code.hideSchemaRecompileMessage();
					_Code.findAndOpenNode(parentPath, false);
					_Code.refreshTree();
				});
			}
		);
	},
	runGlobalSchemaMethod: function(schemaMethod) {

		let cleanedComment = (schemaMethod.comment && schemaMethod.comment.trim() !== '') ? schemaMethod.comment.replaceAll("\n", "<br>") : '';

		Structr.dialog('Run global schema method ' + schemaMethod.name, function() {}, function() {
			$('#run-method').remove();
			$('#clear-log').remove();
		});

		dialogBtn.prepend('<button id="run-method">Run</button>');
		dialogBtn.append('<button id="clear-log">Clear output</button>');

		var paramsOuterBox = $('<div id="params"><h3 class="heading-narrow">Parameters</h3></div>');
		var paramsBox = $('<div></div>');
		paramsOuterBox.append(paramsBox);
		var addParamBtn = $('<i title="Add parameter" class="button ' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" />');
		paramsBox.append(addParamBtn);
		dialog.append(paramsOuterBox);

		if (cleanedComment.trim() !== '') {
			dialog.append('<div id="global-method-comment"><h3 class="heading-narrow">Comment</h3>' + cleanedComment + '</div>');
		}

		Structr.appendInfoTextToElement({
			element: $('#params h3'),
			text: "Parameters can be accessed by using the <code>retrieve()</code> function.",
			css: { marginLeft: "5px" },
			helpElementCss: { fontSize: "12px" }
		});

		addParamBtn.on('click', function() {
			var newParam = $('<div class="param"><input class="param-name" placeholder="Parameter name"> : <input class="param-value" placeholder="Parameter value"></div>');
			var removeParam = $('<i class="button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" alt="Remove parameter" title="Remove parameter"/>');

			newParam.append(removeParam);
			removeParam.on('click', function() {
				newParam.remove();
			});
			paramsBox.append(newParam);
		});

		dialog.append('<h3>Method output</h3>');
		dialog.append('<pre id="log-output"></pre>');

		$('#run-method').on('click', function() {

			$('#log-output').empty();
			$('#log-output').append('Running method..\n');

			var params = {};
			$('#params .param').each(function (index, el) {
				var name = $('.param-name', el).val();
				var val = $('.param-value', el).val();
				if (name) {
					params[name] = val;
				}
			});

			$.ajax({
				url: rootUrl + '/maintenance/globalSchemaMethods/' + schemaMethod.name,
				data: JSON.stringify(params),
				method: 'POST',
				complete: function(data) {
					$('#log-output').append(data.responseText);
					$('#log-output').append('Done.');
				}
			});
		});

		$('#clear-log').on('click', function() {
			$('#log-output').empty();
		});
	}
};
