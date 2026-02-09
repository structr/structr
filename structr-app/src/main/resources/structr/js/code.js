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
document.addEventListener("DOMContentLoaded", () => {
	Structr.registerModule(_Code);
});

let _Code = {
	_moduleName: 'code',
	codeMain: undefined,
	codeTree: undefined,
	codeContents: undefined,
	lastClickedPath: '',
	availableTags: [],
	tagBlacklist: ['core', 'ui', 'html'],       // don't show internal tags (core, ui, html)
	codeLastOpenMethodKey: 'structrCodeLastOpenMethod_' + location.port,
	codeResizerLeftKey: 'structrCodeResizerLeftKey_' + location.port,
	codeResizerRightKey: 'structrCodeResizerRightKey_' + location.port,
	methodsFetchExampleStateKey: 'methodsFetchExampleStateKey_' + location.port,
	methodsCurlExampleStateKey: 'methodsCurlExampleStateKey_' + location.port,
	methodsScriptingExampleStateKey: 'methodsScriptingExampleStateKey_' + location.port,
	additionalDirtyChecks: [],
	defaultPageSize: 10000,
	defaultPage: 1,
	classIndicatorEverythingReady: 'code-area-ready',

	init: () => {
	},
	beforeunloadHandler: () => {
		if (_Code.persistence.isDirty()) {
			return 'There are unsaved changes - discard changes?';
		}
	},
	resize: () => {
		_Code.updatedResizers();
	},
	leftTabMinWidth: 360,
	rightTabMinWidth: 300,
	prevAnimFrameReqId_moveLeftResizer: undefined,
	moveLeftResizer: (left) => {

		_Helpers.requestAnimationFrameWrapper(_Code.prevAnimFrameReqId_moveLeftResizer, () => {
			_Code.updatedResizers(left, null);
		});
	},
	prevAnimFrameReqId_moveRightResizer: undefined,
	moveRightResizer: (right) => {

		_Helpers.requestAnimationFrameWrapper(_Code.prevAnimFrameReqId_moveRightResizer, () => {
			_Code.updatedResizers(null, right);
		});
	},
	updatedResizers: (left, right) => {

		left  = left || LSWrapper.getItem(_Code.codeResizerLeftKey) || _Code.leftTabMinWidth;
		right = right || LSWrapper.getItem(_Code.codeResizerRightKey) || _Code.rightTabMinWidth;

		if (_Code.recentElements.isVisible() === false) {
			right = '3rem';
		} else {
			right = right + 'px';
		}

		let availableWidth = _Code.codeMain.innerWidth();
		let outerPadding   = (window.innerWidth - availableWidth) / 2;

		_Code.codeMain[0].querySelector('.column-resizer-left').style.left     = `${left}px`;
		_Code.codeMain[0].querySelector('.column-resizer-right').style.left    = `calc(${window.innerWidth - outerPadding}px - ${right})`;

		document.getElementById('code-tree').style.width              = `calc(${left}px - 1rem)`;
		document.getElementById('code-context-container').style.width = `calc(${right} - 3rem)`;
		document.getElementById('code-contents').style.width          = `calc(${availableWidth}px - ${left}px - ${right} - 4rem)`;

		_Editors.resizeVisibleEditors();
	},
	unload: () => {

		let allow = _Code.persistence.testAllowNavigation();
		if (allow) {

			document.removeEventListener('keydown', _Code.helpers.handleKeyDownEvent, { capture: true });

			_Code.persistence.runCurrentEntitySaveAction = null;
			_Editors.disposeAllEditors();

			_Helpers.fastRemoveAllChildren(document.querySelector('#code-main'));
		}

		return allow;
	},
	onload: () => {

		Structr.setFunctionBarHTML(_Code.templates.functions());
		Structr.setMainContainerHTML(_Code.templates.main());

		_Code.helpers.preloadAvailableTagsForEntities().then(() => {

			UISettings.showSettingsForCurrentModule(UISettings.settingGroups.schema_code);

			document.addEventListener('keydown', _Code.helpers.handleKeyDownEvent, { capture: true });

			_Code.recentElements.init();
			_Code.pathLocations.init();
			_Code.search.init();
			_Code.init();

			_Code.codeMain     = $('#code-main');
			_Code.codeTree     = $('#code-tree');
			_Code.codeContents = $('#code-contents');

			Structr.initVerticalSlider($('.column-resizer-left', _Code.codeMain), _Code.codeResizerLeftKey, _Code.leftTabMinWidth, _Code.moveLeftResizer);
			Structr.initVerticalSlider($('.column-resizer-right', _Code.codeMain), _Code.codeResizerRightKey, _Code.rightTabMinWidth, _Code.moveRightResizer, true);

			$.jstree.defaults.core.themes.dots      = false;
			$.jstree.defaults.dnd.inside_pos        = 'last';
			$.jstree.defaults.dnd.large_drop_target = true;

			_Code.codeTree.on('select_node.jstree', _Code.tree.handleTreeClick);
			_Code.codeTree.on('refresh.jstree', _Code.tree.activateLastClicked);
			_Code.codeTree.on('ready.jstree', () => {
				_Code.codeMain[0].classList.add(_Code.classIndicatorEverythingReady);
			});

			_Code.recentElements.loadRecentlyUsedElements(() => {
				_TreeHelper.initTree(_Code.codeTree, _Code.tree.treeInitFunction, 'structr-ui-code');
			});

			Structr.mainMenu.unblock(100);

			Structr.resize();
		});
	},
	tree: {
		treeInitFunction: (obj, callback) => {

			let id   = obj?.data?.key || '#';
			let path = obj?.data?.path || '';

			/* The tree construction is now based on obj.data.key, together with
			 * additional information like type, parent etc. That means all nodes
			 * must specify a data object (see below for example).
			 */

			if (id === '#') {

				let defaultEntries = [
					{
						id:       path + '/globals',
						text:     'User-defined functions',
						children: true,
						icon:     _Icons.nonExistentEmptyIcon,
						li_attr:  { 'data-id': 'globals' },
						data: {
							svgIcon: _Icons.getSvgIcon(_Icons.iconGlobe, 16, 24),
							key:     'SchemaMethod',
							query:   { schemaNode: null },
							content: 'globals',
							path:    path + '/globals'
						},
					},
					{
						id:       path + '/openapi',
						text:     'OpenAPI - Swagger UI',
						children: (_Code.availableTags.length > 0),
						icon:     _Icons.nonExistentEmptyIcon,
						li_attr:  { 'data-id': 'openapi' },
						data: {
							svgIcon: _Icons.getSvgIcon(_Icons.iconSwagger, 18, 24),
							key:     'openapi',
							content: 'openapi',
							path:    path + '/openapi'
						},
					},
					{
						id:      '/root',
						text:    'Types',
						icon:    _Icons.nonExistentEmptyIcon,
						li_attr: { 'data-id': 'root' },
						data: {
							svgIcon: _Icons.getSvgIcon(_Icons.iconStructrSSmall, 18, 24),
							key:     'root',
							path:    '/root',
						},
						children: [
							{
								id:       '/root/custom',
								text:     'Custom',
								children: true,
								icon:     _Icons.nonExistentEmptyIcon,
								li_attr:  { 'data-id': 'custom' },
								data: {
									svgIcon: _Icons.getSvgIcon(_Icons.iconFolderClosed, 16, 24),
									key:     'SchemaNode',
									query:   { isServiceClass: false },
									content: 'custom',
									path:    '/root/custom',
								},
							},
							{
								id:       '/root/services',
								text:     'Services',
								children: true,
								icon:     _Icons.nonExistentEmptyIcon,
								li_attr:  { 'data-id': 'services' },
								data: {
									svgIcon: _Icons.getSvgIcon(_Icons.iconFolderClosed, 16, 24),
									key:     'SchemaNode',
									query:   { isServiceClass: true },
									content: 'services',
									path:    '/root/services',
								},
							},
							// maybe show something like "overrides"
							// {
							// 	id:       '/root/builtin',
							// 	text:     'Built-In',
							// 	children: true,
							// 	icon:     _Icons.nonExistentEmptyIcon,
							// 	li_attr:  { 'data-id': 'builtin' },
							// 	data: {
							// 		svgIcon: _Icons.getSvgIcon(_Icons.iconFolderClosed, 16, 24),
							// 		key:     'SchemaNode',
							// 		query:   { isBuiltinType: true },
							// 		content: 'builtin',
							// 		path:    '/root/builtin'
							// 	},
							// },
						],
					}
				];

				if (_Code.search.searchIsActive()) {

					defaultEntries.unshift({
						id:       path + '/searchresults',
						text:     'Search Results',
						children: true,
						icon:     _Icons.nonExistentEmptyIcon,
						data: {
							key: 'searchresults',
							path: path + '/searchresults',
							svgIcon: _Icons.getSvgIcon(_Icons.iconSearch, 20, 24)
						},
						state: {
							opened: true
						}
					});
				}

				callback(defaultEntries);

			} else {

				let data = obj.data;

				data.callback = callback;

				switch (data.key) {

					case 'searchresults':
						_Code.tree.loadSearchResults(data);
						break;

					case 'openapi':
						_Code.tree.displayFunction(_Code.availableTags.map(t => { return { id: t, name: t, type: "OpenAPITag" } }), data);
						break;

					case 'remoteproperties':
						_Code.tree.loadRemoteProperties(data);
						break;

					case 'inheritedproperties':
						_Code.tree.loadInheritedProperties(data);
						break;

					default: {

						if (data.key === 'SchemaNode' && data.path.indexOf(data.id) >= 0) {

							// reload function for single type
							Command.get(data.id, null, (entity) => {

								// in case the type was renamed, update the text
								obj.text = entity.name;

								let children = _Code.tree.getChildElementsForSchemaNode(entity, data.path);

								// directly call callback method to insert child nodes of type
								callback(children);

							}, 'ui');

						} else {

							// generic query function, controlled by data object
							Command.query(data.key, _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', data.query, result => {

								_Code.tree.displayFunction(result, data);

							}, true, 'ui');
						}
					}
				}
			}
		},
		activateLastClicked: () => {
			_Code.tree.findAndOpenNode(_Code.lastClickedPath);
		},
		refreshTree: () => {
			_TreeHelper.refreshTree(_Code.codeTree);
		},
		displayFunction: (result, data, dontSort, isSearch) => {

			let path = data.path;
			let list = [];

			for (let entity of result) {

				// skip HTML entities
				if (entity?.category !== 'html') {

					let icon = _Icons.getIconForSchemaNodeType(entity);

					switch (entity.type) {

						case 'OpenAPITag': {

							list.push({
								id:       path + '/' + entity.id,
								text:     entity.name,
								children: false,
								icon:     _Icons.nonExistentEmptyIcon,
								li_attr:  { 'data-id': entity.id },
								data: {
									svgIcon: _Icons.getSvgIcon(_Icons.iconSwagger, 16, 24),
									name:    entity.name,
									key:     entity.type,
									id:      entity.id,
									path:    path + '/' + entity.id
								},
							});

							break;
						}

						case 'SchemaNode': {

							let doHighlight = false;  //entity?.schemaMethods?.length > 0;

							list.push({
								id:       path + '/' + entity.id,
								text:     entity.name,
								children: _Code.tree.getChildElementsForSchemaNode(entity, path + '/' + entity.id),
								icon:     _Icons.nonExistentEmptyIcon,
								li_attr:  { 'data-id': entity.id, class: doHighlight ? 'highlight-name' : '' },
								data: {
									svgIcon: icon,
									key:     entity.type,
									id:      entity.id,
									path:    path + '/' + entity.id
								},
							});

							break;
						}

						case 'SchemaRelationshipNode': {

							let name = entity.name || '[unnamed]';

							list.push({
								id:       path + '/' + entity.name,
								text:     name,
								children: false,
								icon:     _Icons.nonExistentEmptyIcon,
								li_attr: { 'data-id': entity.id },
								data: {
									svgIcon: icon,
									key:     entity.type,
									id:      entity.id,
									path:    path + '/' + entity.id
								},
							});

							break;
						}

						default: {

							let name = entity.name || '[unnamed]';

							if (isSearch && entity.schemaNode) {
								name = entity.schemaNode.name + '.' + name;
							}

							if (entity.inherited) {

								list.push({
									id:       path + '/' + entity.id,
									text:     name + (' (' + (entity.propertyType || '') + ')'),
									children: false,
									icon:     _Icons.nonExistentEmptyIcon,
									li_attr:  { 'data-id': entity.id, style: 'color: #aaa;' },
									data: {
										svgIcon: icon,
										key:     entity.type,
										id:      entity.id,
										path:    path + '/' + entity.id
									}
								});

							} else {

								let hasVisibleChildren = _Code.tree.hasVisibleChildren(data.root, entity);

								if (entity.type === 'SchemaMethod') {
									name = name + '()';
								}

								if (entity.type === 'SchemaProperty') {
									name = name + ' (' + entity.propertyType + ')';
								}

								list.push({
									id:       path + '/' + entity.id,
									text:     name,
									children: hasVisibleChildren,
									icon:     _Icons.nonExistentEmptyIcon,
									li_attr:  { 'data-id': entity.id },
									data: {
										svgIcon: icon,
										key:     entity.type,
										id:      entity.id,
										path:    path + '/' + entity.id
									},
								});
							}

							break;
						}
					}
				}
			}

			if (!dontSort) {
				_Helpers.sort(list, 'text');
			}

			data.callback(list);
		},
		getChildElementsForSchemaNode: (entity, path) => {

			let children = [
				{
					id:       path + '/properties',
					text:     'Direct Properties',
					children: (entity.schemaProperties.length > 0),
					icon:     _Icons.nonExistentEmptyIcon,
					li_attr:  { 'data-id': 'properties' },
					data:     {
						svgIcon: _Icons.getSvgIcon(_Icons.iconSliders, 16, 24),
						key:     'SchemaProperty',
						id:      entity.id,
						type:    entity.name,
						query:   { schemaNode: entity.id },
						content: 'properties',
						path:    path + '/properties'
					},
				},
				{
					id:       path + '/remoteproperties',
					text:     'Linked Properties',
					children: ((entity.relatedTo.length + entity.relatedFrom.length) > 0),
					icon:     _Icons.nonExistentEmptyIcon,
					li_attr:  { 'data-id': 'remoteproperties' },
					data:     {
						svgIcon: _Icons.getSvgIcon(_Icons.iconSliders, 16, 24),
						key:     'remoteproperties',
						id:      entity.id,
						type:    entity.name,
						content: 'remoteproperties',
						path:    path + '/remoteproperties'
					},
				},
				{
					id:       path + '/views',
					text:     'Views',
					children: (entity.schemaViews.length > 0),
					icon:     _Icons.nonExistentEmptyIcon,
					li_attr:  { 'data-id': 'views' },
					data:     {
						svgIcon: _Icons.getSvgIcon(_Icons.iconSchemaViews, 16, 24),
						key:     'SchemaView',
						id:      entity.id,
						type:    entity.name,
						query:   { schemaNode: entity.id },
						content: 'views',
						path:    path + '/views'
					},
				},
				{
					id:       path + '/methods',
					text:     'Methods',
					children: entity.schemaMethods.length > 0,
					icon:     _Icons.nonExistentEmptyIcon,
					li_attr:  { 'data-id': 'methods' },
					data:     {
						svgIcon: _Icons.getSvgIcon(_Icons.iconSchemaMethods, 16, 24),
						key:     'SchemaMethod',
						id:      entity.id,
						type:    entity.name,
						query:   { schemaNode: entity.id },
						content: 'methods',
						path:    path + '/methods'
					},
				},
				{
					id:       path + '/inheritedproperties',
					text:     'Inherited Properties',
					children: false,
					icon:     _Icons.nonExistentEmptyIcon,
					li_attr:  { 'data-id': 'inheritedproperties' },
					data:     {
						svgIcon: _Icons.getSvgIcon(_Icons.iconSliders, 16, 24),
						key:     'inheritedproperties',
						id:      entity.id,
						type:    entity.name,
						content: 'inheritedproperties',
						path:    path + '/inheritedproperties'
					},
				}
			];

			if (entity.isServiceClass === true) {
				children = [children[3]];
			}

			return children;
		},
		loadSearchResults: (data) => {

			let text          = $('#tree-search-input').val();
			let searchResults = {};
			let count         = 0;
			let collectFunction = (result) => {

				// remove duplicates
				for (let r of result) {
					searchResults[r.id] = r;
				}

				// only show results after all 6 searches are finished (to prevent duplicates)
				if (++count === 6) {

					_Code.tree.displayFunction(Object.values(searchResults), data, false, true);
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

						for (let method of schemaNode.schemaMethods) {

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
		hasVisibleChildren: (id, entity) => {

			return (entity?.schemaMethods ?? []).some(method => {

				return (id === 'custom' || !method.isPartOfBuiltInSchema);
			});
		},
		handleTreeClick: (evt, data) => {

			let selection = data?.node?.data || {};

			selection.updateLocationStack = true;

			// copy key => source for backwards compatibility
			selection.source = selection.key;

			if (data && data.event && data.event.updateLocationStack === false) {
				selection.updateLocationStack = false;
			}

			_Code.tree.handleSelection(selection);
		},
		handleSelection: (data) => {

			if (_Code.persistence.testAllowNavigation()) {

				_Code.dirty = false;
				_Code.additionalDirtyChecks = [];

				// clear page
				_Code.mainArea.clearMainArea();

				switch (data.content) {

					case 'searchresults':
					case 'undefined':
					case 'null':
					case 'root':
						break;

					case 'globals':
						_Code.mainArea.displayGlobalMethodsContent(data, true);
						break;

					case 'openapi':
						_Code.mainArea.showSwaggerUI(data);
						break;

					case 'custom':
						_Code.mainArea.displayCustomTypesContent(data);
						break;

					case 'services':
						_Code.mainArea.displayServiceClassesContent(data);
						break;

					case 'builtin':
						_Code.mainArea.displayBuiltInTypesContent(data.type);
						break;

					case 'properties':
						_Code.mainArea.displayPropertiesContent(data, data.updateLocationStack);
						break;

					case 'remoteproperties':
						_Code.mainArea.displayRemotePropertiesContent(data, data.updateLocationStack);
						break;

					case 'views':
						_Code.mainArea.displayViewsContent(data, data.updateLocationStack);
						break;

					case 'methods':
						_Code.mainArea.displayMethodsContent(data, data.updateLocationStack);
						break;

					case 'inheritedproperties':
						_Code.mainArea.displayInheritedPropertiesContent(data, data.updateLocationStack);
						break;

					case 'inherited':
						_Code.tree.findAndOpenNode(data.path, true);
						break;

					default:
						_Code.tree.handleNodeObjectClick(data);
						break;
				}
			}
		},
		loadRemoteProperties: (data) => {

			Command.get(data.id, null, entity => {

				let mapFn = (rel, out) => {

					let attrName = (out ? (rel.targetJsonName || rel.oldTargetJsonName) : (rel.sourceJsonName || rel.oldSourceJsonName));

					return {
						id:           rel.id,
						type:         rel.type,
						name:         attrName,
						propertyType: '',
						inherited:    false
					};
				};

				let processedRemoteAttributes = [].concat(entity.relatedTo.map(r => mapFn(r, true))).concat(entity.relatedFrom.map((r) => mapFn(r, false)));

				_Code.tree.displayFunction(processedRemoteAttributes, data);
			});
		},
		loadInheritedProperties: (data) => {

			Command.listSchemaProperties(data.id, 'custom', (result) => {

				let filtered = result.filter(p => (p.declaringClass !== data.type));

				_Code.tree.displayFunction(filtered.map(s => {
					return {
						id: s.declaringUuid + '-' + s.name,
						type: 'SchemaProperty',
						name: s.declaringClass + '.' + s.name,
						propertyType: s.declaringPropertyType ? s.declaringPropertyType : s.propertyType,
						inherited: true
					};
				}), data);
			});
		},
		handleNodeObjectClick: (data) => {

			let nodeType = data.key || data.type;
			if (nodeType) {

				switch (nodeType) {

					case 'SchemaView':
						_Code.mainArea.displayViewDetails(data);
						break;

					case 'SchemaProperty':
						_Code.mainArea.displayPropertyDetails(data);
						break;

					case 'SchemaMethod':
						_Code.mainArea.displaySchemaMethodContent(data);
						break;

					case 'SchemaNode':
						_Code.mainArea.displaySchemaNodeContent(data);
						break;

					case 'SchemaRelationshipNode':
						_Code.mainArea.displaySchemaRelationshipNodeContent(data);
						break;

					case 'OpenAPITag':
						_Code.mainArea.showSwaggerUI(data);
						break;
				}
			}
		},
		refreshNode: (id) => {

			if (_Code.codeTree) {

				let escapedId = id.replaceAll('/', '\\/');
				_Code.codeTree.jstree().refresh_node(document.querySelector(`li#${escapedId}`));
			}
		},
		findAndOpenNode: (path, updateLocationStack) => {
			let tree = $('#code-tree').jstree(true);
			_Code.tree.findAndOpenNodeRecursive(tree, document, path, 0, updateLocationStack);
		},
		findAndOpenNodeRecursive: (tree, parent, path, depth, updateLocationStack) => {

			let parts = path.split('/').filter(p => p.length);
			if (path.length === 0) { return; }
			if (parts.length < 1) { return; }
			if (depth > 15) { return; }

			let id   = parts[depth];
			let node = parent.querySelector(`li[data-id="${id}"]`);

			if (depth === parts.length - 1) {

				if (node != null) {

					// node found, activate
					if (tree.get_selected().indexOf(node.id) === -1) {
						tree.activate_node(node, { updateLocationStack: updateLocationStack });
						tree.open_node(node);
					}

					let selectedNode = tree.get_node(node, true);
					if (selectedNode) {

						// depending on the depth we select a different parent level
						let parentToScrollTo = selectedNode;
						switch (selectedNode.parents.length) {
							case 4:
								parentToScrollTo = tree.get_node(tree.get_parent(node), true);
								break;
							case 5:
								parentToScrollTo = tree.get_node(tree.get_parent(tree.get_parent(node)), true)
								break;
						}

						if (parentToScrollTo.length) {
							parentToScrollTo[0].scrollIntoView();
						} else {
							parentToScrollTo.scrollIntoView();
						}
					}

					if (_Code.search.searchIsActive()) {
						tree.element[0].scrollTo(0,0);
					}
				}

			} else {

				tree.open_node(node, function(n) {
					let newParent = parent.querySelector(`li[data-id="${id}"]`);
					_Code.tree.findAndOpenNodeRecursive(tree, newParent, path, depth + 1, updateLocationStack);
				});
			}
		}
	},
	mainArea: {
		clearMainArea: () => {
			_Helpers.fastRemoveAllChildren(_Code.codeContents[0]);

			_Code.persistence.runCurrentEntitySaveAction = null;
		},
		displayGlobalMethodsContent: (data, updateLocationStack) => {

			if (updateLocationStack === true) {
				_Code.pathLocations.updatePathLocationStack(data.path);
				_Code.lastClickedPath = data.path;
			}

			_Code.recentElements.addRecentlyUsedElement(data.content, "User-defined functions", data.svgIcon, data.path, false);

			_Code.codeContents.append(_Code.templates.globals());

			_Schema.methods.fetchUserDefinedMethods((methods) => {

				_Schema.methods.appendMethods(_Code.codeContents[0].querySelector('.content-container'), null, methods, () => {
					_Code.tree.refreshNode(data.path);
				});

				_Code.persistence.runCurrentEntitySaveAction = () => {
					$('.save-all', _Code.codeContents).click();
				};
			});
		},
		displayCustomTypesContent: (data) => {

			_Code.codeContents[0].insertAdjacentHTML('beforeend', _Code.templates.createNewType());

			let container = _Code.codeContents[0].querySelector('#create-type-container');

			_Schema.nodes.showCreateNewTypeDialog(container, null, { isServiceClass: false });

			_Code.persistence.runCurrentEntitySaveAction = () => {

				let typeData = _Schema.nodes.getTypeDefinitionDataFromForm(container, {});

				if (!typeData.name || typeData.name.trim() === '') {

					_Helpers.blinkRed(container.querySelector('[data-property="name"]'));

				} else {

					_Schema.nodes.createTypeDefinition(typeData).then(responseData => {

						_Code.tree.refreshNode(data.path);

						window.setTimeout(() => {
							// tree needs to be refreshed - implement better solution using refresh_node.jstree event
							_Code.tree.findAndOpenNode(data.path + '/' + responseData.result[0], true);
						}, 250);

					}, rejectData => {

						Structr.errorFromResponse(rejectData, undefined, { requiresConfirmation: true });
					});
				}
			};

			_Code.mainArea.helpers.displaySvgActionButton('#create-type-actions', _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 14, 14, 'icon-green'), 'create', 'Create', _Code.persistence.runCurrentEntitySaveAction);
		},
		displayBuiltInTypesContent: () => {
			_Code.codeContents.append(_Code.templates.builtin());
		},
		displaySchemaNodeContent: (data) => {

			fetch(`${Structr.rootUrl}${data.id}/schema`).then(response => {

				if (response.ok) {
					return response.json();
				} else {
					throw Error("Unable to fetch schema node content");
				}

			}).then(json => {

				let entity = json.result;

				_Code.recentElements.updateRecentlyUsed(entity, data.path, data.updateLocationStack);

				_Helpers.fastRemoveAllChildren(_Code.codeContents[0]);
				_Code.codeContents.append(_Code.templates.type({ data, type: entity }));

				let targetView  = LSWrapper.getItem(`${_Entities.activeEditTabPrefix}_${entity.id}`, 'general');
				let tabControls = _Schema.nodes.loadNode(entity, $('.tabs-container', )[0], $('.tabs-content-container', _Code.codeContents)[0], targetView);

				// remove bulk edit save/discard buttons
				for (let button of _Code.codeContents[0].querySelectorAll('.discard-all, .save-all')) {
					_Helpers.fastRemoveElement(button);
				}

				_Code.persistence.runCurrentEntitySaveAction = () => {

					_Schema.bulkDialogsGeneral.saveEntityFromTabControls(entity, tabControls).then((success) => {

						if (success) {

							// reload tree node
							_Code.tree.refreshNode(data.path);

							// and refresh center pane
							_Code.tree.handleNodeObjectClick(data);
						}
					});
				};

				let saveButton = _Code.mainArea.helpers.displaySvgActionButton('#type-actions', _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 14, 14, 'icon-green'), 'save', 'Save', _Code.persistence.runCurrentEntitySaveAction);

				let cancelButton = _Code.mainArea.helpers.displaySvgActionButton('#type-actions', _Icons.getSvgIcon(_Icons.iconCrossIcon, 14, 14, 'icon-red'), 'cancel', 'Revert changes', () => {
					_Schema.bulkDialogsGeneral.resetInputsViaTabControls(tabControls);
				});

				// delete button
				if (!entity.isPartOfBuiltInSchema) {
					_Code.mainArea.helpers.displaySvgActionButton('#type-actions', _Icons.getSvgIcon(_Icons.iconTrashcan, 14, 14, 'icon-red'), 'delete', 'Delete type ' + entity.name, () => {
						_Code.persistence.deleteSchemaEntity(entity, `Delete type ${entity.name}?`, 'This will delete all schema relationships as well, but no data will be removed.', data);
					});
				}

				_Helpers.disableElements(true, saveButton, cancelButton);

				document.querySelector('#code-contents .tabs-content-container')?.addEventListener('bulk-data-change', (e) => {

					e.stopPropagation();

					let changeCount = _Schema.bulkDialogsGeneral.getChangeCountFromBulkInfo(_Schema.bulkDialogsGeneral.getBulkInfoFromTabControls(tabControls, false));
					let isDirty     = (changeCount > 0);
					_Helpers.disableElements(!isDirty, saveButton, cancelButton);

					_Code.persistence.tellFirstElementToShowDirtyState(isDirty);
				});
			});
		},
		displayPropertiesContent: (data, updateLocationStack) => {

			if (updateLocationStack === true) {
				_Code.pathLocations.updatePathLocationStack(data.path);
				_Code.lastClickedPath = data.path;
			}

			_Code.codeContents.append(_Code.templates.propertiesLocal({ data: data }));

			Command.get(data.id, null, (entity) => {

				_Schema.properties.appendLocalProperties(document.querySelector('#code-contents .content-container'), entity, {

					editReadWriteFunction: (property) => {
						_Code.tree.findAndOpenNode(data.path + '/' + property.id, true);
					},
					editCypherProperty: (property) => {
						_Code.tree.findAndOpenNode(data.path + '/' + property.id, true);
					}
				}, () => {
					_Code.tree.refreshNode(data.path);
				});

				_Code.persistence.runCurrentEntitySaveAction = () => {
					$('.save-all', _Code.codeContents).click();
				};
			});
		},
		displayRemotePropertiesContent: (data, updateLocationStack) => {

			if (updateLocationStack === true) {
				_Code.pathLocations.updatePathLocationStack(data.path);
				_Code.lastClickedPath = data.path;
			}

			_Code.codeContents.append(_Code.templates.propertiesRemote({ data: data }));

			Command.get(data.id, null, (entity) => {

				_Schema.remoteProperties.appendRemote(_Code.codeContents[0].querySelector('.content-container'), entity, () => {

					// TODO: navigation should/could be possible in the code area as well - currently this is deactivated in schema.js

				}, _Code.tree.refreshTree);

				_Code.persistence.runCurrentEntitySaveAction = () => {
					$('.save-all', _Code.codeContents).click();
				};
			});
		},
		displayViewsContent: (data, updateLocationStack) => {

			if (updateLocationStack === true) {
				_Code.pathLocations.updatePathLocationStack(data.path);
				_Code.lastClickedPath = data.path;
			}

			_Code.codeContents.append(_Code.templates.views({ data: data }));

			Command.get(data.id, null, (entity) => {
				_Schema.views.appendViews(_Code.codeContents[0].querySelector('.content-container'), entity, () => {
					_Code.tree.refreshNode(data.path);
				});

				_Code.persistence.runCurrentEntitySaveAction = () => {
					$('.save-all', _Code.codeContents).click();
				};
			});
		},
		displayMethodsContent: (data, updateLocationStack) => {

			if (updateLocationStack === true) {
				_Code.pathLocations.updatePathLocationStack(data.path);
				_Code.lastClickedPath = data.path;
			}

			_Code.codeContents.append(_Code.templates.methods({ data: data }));

			fetch(Structr.rootUrl + data.id + '/schema').then(res => res.json()).then(r => r.result).then((entity) => {

				_Schema.methods.appendMethods(_Code.codeContents[0].querySelector('.content-container'), entity, entity.schemaMethods, () => {
					_Code.tree.refreshNode(data.path);
				});

				_Code.persistence.runCurrentEntitySaveAction = () => {
					$('.save-all', _Code.codeContents).click();
				};
			});
		},
		displayInheritedPropertiesContent: (data, updateLocationStack) => {

			if (updateLocationStack === true) {
				_Code.pathLocations.updatePathLocationStack(data.path);
				_Code.lastClickedPath = data.path;
			}

			_Code.codeContents.append(_Code.templates.propertiesInherited({ data: data }));

			Command.get(data.id, null, (entity) => {
				_Schema.properties.appendBuiltinProperties(_Code.codeContents[0].querySelector('.content-container'), entity);
			});
		},
		displayPropertyDetails: (data) => {

			Command.get(data.id, null, (result) => {

				_Code.recentElements.updateRecentlyUsed(result, data.path, data.updateLocationStack);

				if (result.propertyType) {

					switch (result.propertyType) {
						case 'Cypher':
							_Code.mainArea.displayCypherPropertyDetails(result);
							break;

						case 'Function':
							_Code.mainArea.displayFunctionPropertyDetails(result, data);
							break;

						case 'String':
							_Code.mainArea.displayStringPropertyDetails(result, data);
							break;

						case 'Boolean':
							_Code.mainArea.displayBooleanPropertyDetails(result, data);
							break;

						default:
							_Code.mainArea.displayDefaultPropertyDetails(result, data);
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
		displayFunctionPropertyDetails: (property, data, lastOpenTab) => {

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
			if (_Code.codeContents[0].querySelector(`li[data-name="${lastOpenTab}"]`)) {
				activateTab(lastOpenTab || 'source');
			} else {
				activateTab('source');
			}

			_Helpers.activateCommentsInElement(_Code.codeContents[0], { insertAfter: true });

			let updateWrapFlagVisibility = (key, text) => {

				let isJs = (_Editors.getMonacoEditorModeForContent(text, property) === 'javascript');
				let el = document.querySelector(`[data-property="${key}"]`);
				el?.parentNode.classList.toggle('hidden', !isJs);
			};

			let functionPropertyMonacoConfig = {
				language: 'auto',
				lint: true,
				autocomplete: true,
				changeFn: (editor, entity) => {
					_Code.persistence.updateDirtyFlag(entity);

					let key = `${editor.getModel().uri.structr_property}WrapJS`;
					updateWrapFlagVisibility(key, editor.getValue());
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
					_Code.persistence.updateDirtyFlag(entity);
				}
			};

			_Editors.getMonacoEditor(property, 'openAPIReturnType', _Code.codeContents[0].querySelector('#tabView-api .editor'), openAPIReturnTypeMonacoConfig);

			_Code.mainArea.displayDefaultPropertyOptions(property, _Editors.resizeVisibleEditors, data);

			updateWrapFlagVisibility('readFunctionWrapJS', property.readFunction);
			updateWrapFlagVisibility('writeFunctionWrapJS', property.writeFunction);
		},
		displayCypherPropertyDetails: (property, data) => {

			_Code.codeContents.append(_Code.templates.cypherProperty({ property: property }));

			let cypherMonacoConfig = {
				language: 'cypher',
				lint: false,
				autocomplete: false,
				changeFn: (editor, entity) => {
					_Code.persistence.updateDirtyFlag(entity);
				},
				isAutoscriptEnv: false
			};

			_Editors.getMonacoEditor(property, 'format', _Code.codeContents[0].querySelector('#cypher-code-container .editor'), cypherMonacoConfig);
			_Editors.appendEditorOptionsElement(_Code.codeContents[0].querySelector('.editor-info'));

			_Code.mainArea.displayDefaultPropertyOptions(property, _Editors.resizeVisibleEditors, data);
		},
		displayStringPropertyDetails: (property, data) => {

			_Code.codeContents.append(_Code.templates.stringProperty({ property: property }));
			_Code.mainArea.displayDefaultPropertyOptions(property, undefined, data);
		},
		displayBooleanPropertyDetails: (property, data) => {

			_Code.codeContents.append(_Code.templates.booleanProperty({ property: property }));
			_Code.mainArea.displayDefaultPropertyOptions(property, undefined, data);
		},
		displayDefaultPropertyDetails: (property, data) => {

			_Code.codeContents.append(_Code.templates.defaultProperty({ property: property }));
			_Code.mainArea.displayDefaultPropertyOptions(property, undefined, data);
		},
		displayDefaultPropertyOptions: (property, callback, data) => {

			_Code.persistence.runCurrentEntitySaveAction = () => {
				_Code.persistence.saveEntityAction(property);
			};

			let propertyUIContainer = $('#property-buttons');
			let showDBName          = UISettings.getValueForSetting(UISettings.settingGroups.schema_code.settings.showDatabaseNameForDirectProperties);
			let dbNameClass         = ((showDBName === true) ? '' : 'hidden');

			propertyUIContainer.prepend(_Code.templates.propertyOptions({ property: property, dbNameClass: dbNameClass }));

			_Code.mainArea.helpers.displaySvgActionButton('#property-actions', _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 14, 14, 'icon-green'), 'save', 'Save property', _Code.persistence.runCurrentEntitySaveAction);

			_Code.mainArea.helpers.displaySvgActionButton('#property-actions', _Icons.getSvgIcon(_Icons.iconCrossIcon, 14, 14, 'icon-red'), 'cancel', 'Revert changes', () => {
				_Code.persistence.revertFormData(property);
			});

			if (!property.schemaNode.isBuiltinType) {

				_Code.mainArea.helpers.displaySvgActionButton('#property-actions', _Icons.getSvgIcon(_Icons.iconTrashcan, 14, 14, 'icon-red'), 'delete', 'Delete property', () => {
					_Code.persistence.deleteSchemaEntity(property, `Delete property ${property.name}?`, 'No data will be removed.', data);
				});
			}

			let changeHandler = () => {

				_Code.persistence.updateDirtyFlag(property);

				let propertyInfoUI = Object.assign({ propertyType: property.propertyType }, _Code.persistence.collectPropertyData(property));
				let container      = propertyUIContainer[0].querySelector('#property-indexed').closest('div');

				_Schema.properties.checkFunctionProperty(propertyInfoUI, container);
			};

			if (property.propertyType !== 'Function') {
				_Helpers.fastRemoveElement(propertyUIContainer[0].querySelector('#property-type-hint-input').closest('[data-is-property-attribute-container]'));
				_Helpers.fastRemoveElement(propertyUIContainer[0].querySelector('#property-cached').closest('[data-is-property-attribute-container]'));
				_Helpers.fastRemoveElement(propertyUIContainer[0].querySelector('#property-readfunction-wrap').closest('[data-is-property-attribute-container]'));
				_Helpers.fastRemoveElement(propertyUIContainer[0].querySelector('#property-writefunction-wrap').closest('[data-is-property-attribute-container]'));
			} else {
				$('#property-type-hint-input').val(property.typeHint || 'null');
			}

			if (property.propertyType !== 'String' && property.propertyType !== 'StringArray') {
				_Helpers.fastRemoveElement(propertyUIContainer[0].querySelector('#property-fulltext-indexed').closest('[data-is-property-attribute-container]'));
			}

			if (property.propertyType === 'Cypher') {
				_Helpers.fastRemoveElement($('#property-format-input').parent()[0]);
			}

			$('select', propertyUIContainer).on('change', changeHandler);
			$('input[type=checkbox]', propertyUIContainer).on('change', changeHandler);
			$('input[type=text]', propertyUIContainer).on('keyup', changeHandler);

			if (property.schemaNode.isBuiltinType) {

				_Helpers.fastRemoveElement($('button#delete-property-button').parent()[0]);

			} else {

				$('button#delete-property-button').on('click', function() {
					_Code.persistence.deleteSchemaEntity(property, `Delete property ${property.name}?`, 'Property values will not be removed from data nodes.', data);
				});
			}

			changeHandler();

			_Helpers.activateCommentsInElement(propertyUIContainer[0]);

			if (typeof callback === 'function') {
				callback();
			}
		},
		displayViewDetails: (data) => {

			Command.get(data.id, null, (result) => {

				_Code.recentElements.updateRecentlyUsed(result, data.path, data.updateLocationStack);

				_Code.codeContents.append(_Code.templates.defaultView({ view: result }));
				_Code.mainArea.displayDefaultViewOptions(result, undefined, data);
			});
		},
		displayDefaultViewOptions: (view, callback, data) => {

			_Code.persistence.runCurrentEntitySaveAction = () => {

				if (_Code.persistence.isDirty()) {

					// update entity before storing the view to make sure that nonGraphProperties are correctly identified..
					Command.get(view.schemaNode.id, null, (reloadedEntity) => {

						let formData                = _Code.persistence.collectChangedPropertyData(view);
						let sortedAttrs             = $('.property-attrs.view').sortedValues();
						formData.schemaProperties   = _Schema.views.findSchemaPropertiesByNodeAndName(reloadedEntity, sortedAttrs);
						formData.nonGraphProperties = _Schema.views.findNonGraphProperties(reloadedEntity, sortedAttrs);

						Command.setProperties(view.id, formData, () => {
							Object.assign(view, formData);
							_Code.persistence.updateDirtyFlag(view);

							_Code.persistence.showSaveAction(formData);

							if (formData.name) {
								_Code.tree.refreshTree();
							}
						});
					});
				}
			};

			let buttons = $('#view-buttons');
			buttons.prepend(_Code.templates.viewOptions({ view: view }));

			if (_Schema.views.isViewEditable(view)) {

				_Code.mainArea.helpers.displaySvgActionButton('#view-actions', _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 14, 14, 'icon-green'), 'save', 'Save view', _Code.persistence.runCurrentEntitySaveAction);

				_Code.mainArea.helpers.displaySvgActionButton('#view-actions', _Icons.getSvgIcon(_Icons.iconCrossIcon, 14, 14, 'icon-red'), 'cancel', 'Revert changes', () => {
					_Code.persistence.revertFormData(view);
					Command.listSchemaProperties(view.schemaNode.id, view.name, (data) => {

						let select = document.querySelector('#view-properties .property-attrs');
						for (let prop of data) {
							let option = select.querySelector(`option[value="${prop.name}"]`);
							if (option) {
								option.selected = prop.isSelected;
							}
						}

						select.dispatchEvent(new CustomEvent('change'));
					});
				});
			}

			if (_Schema.views.isDeleteViewAllowed(view)) {

				_Code.mainArea.helpers.displaySvgActionButton('#view-actions', _Icons.getSvgIcon(_Icons.iconTrashcan, 14, 14, 'icon-red'), 'delete', 'Delete view', () => {
					_Code.persistence.deleteSchemaEntity(view, `Delete view ${view.name}?`, 'Note: Builtin views will be restored in their initial configuration', data);
				});
			}

			_Code.persistence.updateDirtyFlag(view);

			$('input[type=text]', buttons).on('keyup', () => {
				_Code.persistence.updateDirtyFlag(view);
			});

			_Code.mainArea.displayViewSelect(view);

			if (typeof callback === 'function') {
				callback();
			}
		},
		displayViewSelect: (view) => {

			Command.listSchemaProperties(view.schemaNode.id, view.name, (properties) => {

				let viewIsEditable = _Schema.views.isViewEditable(view);
				let viewSelectElem = document.querySelector('#view-properties .property-attrs');
				viewSelectElem.disabled = !viewIsEditable;

				if (view.sortOrder) {

					for (let sortedProp of view.sortOrder.split(',')) {

						let prop = properties.filter(prop => (prop.name === sortedProp));

						if (prop.length) {

							_Schema.views.appendPropertyForViewSelect(viewSelectElem, view, prop[0]);

							properties = properties.filter(prop  => (prop.name !== sortedProp));
						}
					}
				}

				for (let prop of properties) {
					_Schema.views.appendPropertyForViewSelect(viewSelectElem, view, prop);
				}

				let viewSelect2 = $(viewSelectElem).select2({
					search_contains: true,
					width: '100%',
					dropdownCssClass: 'select2-sortable hide-selected-options hide-disabled-options',
					containerCssClass: 'select2-sortable hide-selected-options hide-disabled-options',
					closeOnSelect: false,
					scrollAfterSelect: false
				});

				if (viewIsEditable) {

					let changeFn = () => {
						let sortedAttrs = $(viewSelectElem).sortedValues();
						$('input#view-sort-order').val(sortedAttrs.join(','));
						_Code.persistence.updateDirtyFlag(view);
					};

					viewSelect2.on('change', changeFn).select2Sortable(changeFn);
				}
			});
		},
		displaySchemaMethodContent: (data) => {

			fetch(Structr.rootUrl + data.id + '/schema').then(res => res.json()).then(r => r.result).then((result) => {

				let isCallableViaHTTP   = (result.isPrivate !== true);
				let isUserDefinedMethod = (!result.schemaNode && !result.isPartOfBuiltInSchema);
				let isStaticMethod      = result.isStatic;

				let lastOpenTab = LSWrapper.getItem(`${_Entities.activeEditTabPrefix}_${data.id}`, 'source');

				_Code.recentElements.updateRecentlyUsed(result, data.path, data.updateLocationStack);

				_Helpers.fastRemoveAllChildren(_Code.codeContents[0]);
				_Code.codeContents.append(_Code.templates.method({ method: result }));

				LSWrapper.setItem(_Code.codeLastOpenMethodKey, result.id);

				let buttons = $('#method-buttons');

				_Schema.methods.updateUIForAllAttributes(buttons[0], result);

				// method name input,etc
				{
					let methodNameOutputElement    = document.getElementById('method-name-output');
					let methodNameInputElement     = document.getElementById('method-name-input');
					let methodNameContainerElement = document.getElementById('method-name-container');
					let currentMethodName          = result.name;

					let setMethodNameInUI = (name) => {

						currentMethodName                 = name;
						methodNameOutputElement.innerText = currentMethodName;

						methodNameInputElement.classList.add('hidden');
						methodNameContainerElement.classList.remove('hidden');

						_Editors.resizeVisibleEditors();
						_Code.persistence.updateDirtyFlag(result);

						let updatedObj = Object.assign({}, result, { name: currentMethodName });
						_Schema.methods.updateUIForAllAttributes(buttons[0], updatedObj);
					};

					methodNameInputElement.addEventListener('keyup', (e) => {

						if (e.key === 'Escape') {
							methodNameInputElement.value = currentMethodName;
							setMethodNameInUI(currentMethodName);
						} else if (e.key === 'Enter') {
							setMethodNameInUI(methodNameInputElement.value);
						}
					});

					methodNameInputElement.addEventListener('blur', (e) => {
						setMethodNameInUI(methodNameInputElement.value);
					});

					methodNameContainerElement.addEventListener('click', (e) => {

						e.stopPropagation();
						methodNameContainerElement.classList.add('hidden');

						const methodNameInputField = methodNameInputElement;
						methodNameInputField?.classList.remove('hidden');
						methodNameInputField?.focus();

						_Editors.resizeVisibleEditors();
					});
				}

				// Source Editor
				let sourceMonacoConfig = {
					language: 'auto',
					lint: true,
					autocomplete: true,
					isAutoscriptEnv: true,
					changeFn: (editor, entity) => {
						_Code.persistence.updateDirtyFlag(entity);

						let updatedObj = Object.assign({}, result, { source: editor.getValue() });
						_Schema.methods.updateUIForAllAttributes(buttons[0], updatedObj);
					}
				};

				let sourceEditor = _Editors.getMonacoEditor(result, 'source', _Code.codeContents[0].querySelector('#tabView-source .editor'), sourceMonacoConfig);
				_Editors.appendEditorOptionsElement(_Code.codeContents[0].querySelector('.editor-info'));

				if (_Code.mainArea.helpers.shouldHideOpenAPITabForMethod(result)) {

					$('li[data-name=api]').hide();

					if (lastOpenTab === 'api') {
						lastOpenTab = 'source';
					}

				} else {

					let apiTab = $('#tabView-api', _Code.codeContents);
					apiTab.append(_Code.templates.openAPIBaseConfig({ type: result.type }));

					_Code.mainArea.populateOpenAPIBaseConfig(apiTab[0], result, _Code.availableTags);

					let parameterTplRow = $('.template', apiTab);
					let parameterContainer = parameterTplRow.parent();

					// do not use "fastRemove" because it also removes all children and we want to use it as a template afterwards
					parameterTplRow[0].remove();

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

						$('input[data-parameter-property]', clone).on('input', () => {
							_Code.persistence.updateDirtyFlag(result);
						});

						parameterContainer.append(clone);

						_Editors.resizeVisibleEditors();

						$('.method-parameter-delete .remove-action', clone).on('click', () => {
							clone.remove();

							_Editors.resizeVisibleEditors();

							_Code.persistence.updateDirtyFlag(result);
						});

						_Code.persistence.updateDirtyFlag(result);
					};

					result.parameters.sort((p1, p2) => (p1.index ?? 0) - (p2.index ?? 0));

					for (let p of result.parameters) {
						addParameterRow(p);
					}

					_Code.additionalDirtyChecks.push(() => {
						return _Code.mainArea.schemaMethodParametersChanged(result.parameters);
					});

					$('#add-parameter-button').on('click', () => { addParameterRow(); });

					$('#tags-select', apiTab).select2({
						tags: true,
						width: '100%'
					}).on('change', () => {
						_Code.persistence.updateDirtyFlag(result);
					});

					$('input[type=checkbox]', apiTab).on('change', function() {
						_Code.persistence.updateDirtyFlag(result);
					});

					$('input[type=text]', apiTab).on('keyup', function() {
						_Code.persistence.updateDirtyFlag(result);
					});

					let openAPIReturnTypeMonacoConfig = {
						language: 'json',
						lint: false,
						autocomplete: true,
						changeFn: (editor, entity) => {
							_Code.persistence.updateDirtyFlag(entity);
						}
					};

					_Editors.getMonacoEditor(result, 'openAPIReturnType', apiTab[0].querySelector('.editor'), openAPIReturnTypeMonacoConfig);

					_Helpers.activateCommentsInElement(apiTab[0]);
				}

				if (_Code.mainArea.helpers.shouldHideAPIExampleTabForMethod(result)) {

					$('li[data-name=api-usage]').hide();

					if (lastOpenTab === 'api-usage') {
						lastOpenTab = 'source';
					}

				} else {

					let apiExamplesTab = _Code.codeContents[0].querySelector('#tabView-api-usage');

					let exampleData      = Object.fromEntries((result.parameters ?? []).map(p => [p.name, (p.exampleValue ?? '')]));
					let isGet            = (result.httpVerb.toLowerCase() === 'get');
					let isStatic         = (result.isStatic === true);
					let url              = _Schema.methods.getURLForSchemaMethod(result, true);
					let queryString      = new URLSearchParams(exampleData).toString();
					let isUserDefinedFn  = (!result.schemaNode);
					let isInstanceMethod = (!isStatic && !isUserDefinedMethod);

					let uuidHelpText = 'Note that {uuid} must be replaced with a UUID of a database object of a matching type';

					let getFetchParts = () => {

						let parts = [];

						if (isGet) {

							parts.push(`let parameters = ${JSON.stringify(exampleData, undefined, '\t')};`);
							parts.push('let queryString = new URLSearchParams(parameters).toString();');
							parts.push('');

							if (isInstanceMethod) {
								parts.push(`// ${uuidHelpText}`);
							}

							parts.push(`fetch('${url}?' + queryString).then(response => {`);
							parts.push('	// handle response');
							parts.push('	console.log(response);');
							parts.push('});');

						} else {

							parts.push(`let data = ${JSON.stringify(exampleData, undefined, '\t')};`);
							parts.push('');

							if (isInstanceMethod) {
								parts.push(`// ${uuidHelpText}`);
							}

							parts.push(`fetch('${_Schema.methods.getURLForSchemaMethod(result, true)}', {`);
							parts.push(`	method: '${result.httpVerb.toUpperCase()}',`);
							parts.push('	body: JSON.stringify(data)');
							parts.push('}).then(response => {');
							parts.push('	// handle response');
							parts.push('	console.log(response);');
							parts.push('});');
						}

						return parts;
					};

					let getCurlParts = () => {

						let parts = [
							`${isInstanceMethod ? `# ${uuidHelpText}\n` : ''}curl -HX-User:admin -HX-Password:admin -X${result.httpVerb.toUpperCase()} "${url}${(isGet && queryString.length > 0) ? '?' + queryString : ''}"`
						];

						if (!isGet) {
							parts.push(`-d '${JSON.stringify(exampleData)}'`);
						}

						return parts;
					};

					let getScriptingParts = () => {

						let parts = [];

						if (isUserDefinedFn) {

							parts.push('${{');
							parts.push(`	let parameters = ${JSON.stringify(exampleData, undefined, '\t').split('\n').join('\n\t')};`);
							parts.push(`	let result     = $.${result.name}(parameters);`);
							parts.push('}}');

						} else if (isStatic) {

							parts.push('${{');
							parts.push(`	let parameters = ${JSON.stringify(exampleData, undefined, '\t').split('\n').join('\n\t')};`);
							parts.push(`	let result     = $.${result.schemaNode.name}.${result.name}(parameters);`);
							parts.push('}}');

						} else {

							parts.push('${{');
							parts.push(`	let parameters = ${JSON.stringify(exampleData, undefined, '\t').split('\n').join('\n\t')};`);
							parts.push('');
							parts.push(`	// option a) this instance method can be called from anywhere by looking up a specific object of type "${result.schemaNode.name}" by UUID (or any other means)`);
							parts.push(`	// ${uuidHelpText}`);
							parts.push(`	let objectUUID   = '<b>{uuid}</b>';`);
							parts.push(`	let nodeInstance = $.find('${result.schemaNode.name}', objectUUID);`);
							parts.push(`	let resultOne    = nodeInstance.${result.name}(parameters);`);
							parts.push('');
							parts.push('');
							parts.push(`	// option b) this instance method can be called if the current scripting context is already a object of type "${result.schemaNode.name}". Then we can use $.this`);
							parts.push(`	let resultTwo = $.this.${result.name}(parameters);`);
							parts.push('}}');
						}

						return parts;
					}

					let templateConfig = {
						type: result.type,
						method: result,
						exampleData: exampleData,
						fetchExample: getFetchParts().join('\n'),
						curlExample: getCurlParts().join(' \\\n\t'),
						scriptingExample: getScriptingParts().join('\n')
					};
					apiExamplesTab.insertAdjacentHTML('beforeend', _Code.templates.schemaMethodAPIExamples(templateConfig));

					for (let copyBtn of apiExamplesTab.querySelectorAll('button[copy-button]')) {
						copyBtn.addEventListener('click', async (e) => {
							let text  =copyBtn.closest('details').querySelector('div[usage-text]').textContent;
							await navigator.clipboard.writeText(text);
						});
					}

					apiExamplesTab.querySelector('#fetch-example')?.addEventListener('toggle', (e) => {
						LSWrapper.setItem(_Code.methodsFetchExampleStateKey, e.newState);
					});

					apiExamplesTab.querySelector('#curl-example')?.addEventListener('toggle', (e) => {
						LSWrapper.setItem(_Code.methodsCurlExampleStateKey, e.newState);
					});

					apiExamplesTab.querySelector('#scripting-example')?.addEventListener('toggle', (e) => {
						LSWrapper.setItem(_Code.methodsScriptingExampleStateKey, e.newState);
					});
				}

				// default buttons
				_Code.persistence.runCurrentEntitySaveAction = () => {

					let storeParametersInFormDataFunction = (formData) => {

						let parametersData = _Code.mainArea.helpers.collectSchemaMethodParameters();

						if (parametersData !== null) {

							formData['parameters'] = parametersData;
						}
					};

					let afterSaveCallback = () => {

						_Code.additionalDirtyChecks = [];
						_Code.mainArea.displaySchemaMethodContent(data);

						// refresh parent in case icon changed
						_Code.tree.refreshNode(data.path.slice(0, data.path.lastIndexOf('/')));
					};

					_Code.persistence.saveEntityAction(result, afterSaveCallback, [storeParametersInFormDataFunction]);
				};

				_Code.mainArea.helpers.displaySvgActionButton('#method-actions', _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 14, 14, 'icon-green'), 'save', 'Save method', _Code.persistence.runCurrentEntitySaveAction);

				_Code.mainArea.helpers.displaySvgActionButton('#method-actions', _Icons.getSvgIcon(_Icons.iconCrossIcon, 14, 14, 'icon-red'), 'cancel', 'Revert changes', () => {
					_Code.additionalDirtyChecks = [];
					_Editors.disposeEditorModel(result.id, 'source');
					_Editors.disposeEditorModel(result.id, 'openAPIReturnType');
					_Code.mainArea.displaySchemaMethodContent(data);
				});

				// delete button
				_Code.mainArea.helpers.displaySvgActionButton('#method-actions', _Icons.getSvgIcon(_Icons.iconTrashcan, 14, 14, 'icon-red'), 'delete', 'Delete method', () => {
					_Code.persistence.deleteSchemaEntity(result, 'Delete method ' + result.name + '?', 'Note: Builtin methods will be restored in their initial configuration', data);
				});

				// run button (for user-defined functions and static methods which are callable via HTTP)
				if ((isUserDefinedMethod || isStaticMethod) && isCallableViaHTTP) {

					_Code.mainArea.helpers.displaySvgActionButton('#method-actions', _Icons.getSvgIcon(_Icons.iconRunButton, 14, 14), 'run', 'Open run dialog', () => {
						_Schema.methods.runSchemaMethod(result);
					});
				}

				_Helpers.activateCommentsInElement(buttons[0]);

				_Code.persistence.updateDirtyFlag(result);

				$('input, select', buttons).on('input', () => {
					let changes = _Code.persistence.updateDirtyFlag(result);

					let updatedObj = Object.assign({}, result, changes);
					_Schema.methods.updateUIForAllAttributes(buttons[0], updatedObj);
				});

				if (typeof callback === 'function') {
					callback();
				}

				let activateTab = (tabName) => {

					_Code.mainArea.saveActiveTabForMethod(data.id, tabName);

					$('.method-tab-content', _Code.codeContents).hide();
					$('li[data-name]', _Code.codeContents).removeClass('active');

					let activeTab = $(`#tabView-${tabName}`, _Code.codeContents);
					activeTab.show();
					$(`li[data-name="${tabName}"]`, _Code.codeContents).addClass('active');

					window.setTimeout(() => { _Editors.resizeVisibleEditors(); }, 250);
				};

				$('li[data-name]', _Code.codeContents).off('click').on('click', function(e) {
					e.stopPropagation();
					activateTab($(this).data('name'));
				});

				if (_Code.codeContents[0].querySelector(`li[data-name="${lastOpenTab}"]`)) {
					activateTab(lastOpenTab || 'source');
				} else {
					activateTab('source');
				}

				_Editors.focusEditor(sourceEditor);
			});
		},
		saveActiveTabForMethod: (methodId, tabName) => {
			LSWrapper.setItem(`${_Entities.activeEditTabPrefix}_${methodId}`, tabName);
		},
		schemaMethodParametersChanged: (parameters) => {

			let parametersData = _Code.mainArea.helpers.collectSchemaMethodParameters();

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
		populateOpenAPIBaseConfig: (container, entity = {}, availableTags) => {

			container.querySelector('[data-property="includeInOpenAPI"]').checked = (entity.includeInOpenAPI === true);

			let tagsSelect = container.querySelector('#tags-select');

			tagsSelect.insertAdjacentHTML('beforeend', ((entity.tags) ? entity.tags.map(tag => `<option selected>${tag}</option>`).join() : ''));
			tagsSelect.insertAdjacentHTML('beforeend', availableTags.filter(tag => (!entity.tags || !entity.tags.includes(tag))).map(tag => `<option>${tag}</option>`).join());

			container.querySelector('[data-property="summary"]').value = entity.summary || '';

			if (entity.type && entity.type !== 'SchemaNode') {

				container.querySelector('[data-property="description"]').value = entity.description || '';
			}
		},
		displaySchemaRelationshipNodeContent: (data) => {

			Command.get(data.id, null, (entity) => {

				Command.get(entity.sourceId, null, (sourceNode) => {

					Command.get(entity.targetId, null, (targetNode) => {

						_Helpers.fastRemoveAllChildren(_Code.codeContents[0]);
						_Code.codeContents.append(_Code.templates.propertyRemote({ data, entity, sourceNode, targetNode }));

						let targetView  = LSWrapper.getItem(_Entities.activeEditTabPrefix  + '_' + entity.id, 'general');
						let tabControls = _Schema.relationships.loadRelationship(entity, $('.tabs-container', _Code.codeContents)[0], $('.tabs-content-container', _Code.codeContents)[0], sourceNode, targetNode, targetView);

						// remove bulk edit save/discard buttons
						for (let button of _Code.codeContents[0].querySelectorAll('.discard-all, .save-all')) {
							_Helpers.fastRemoveElement(button);
						}

						_Code.persistence.runCurrentEntitySaveAction = () => {

							_Schema.bulkDialogsGeneral.saveEntityFromTabControls(entity, tabControls).then((success) => {

								if (success) {

									// refresh parent node "Related properties"
									let parentPath = data.path.substring(0, data.path.lastIndexOf('/'));
									_Code.tree.refreshNode(parentPath);
								}
							});
						};

						let saveButton = _Code.mainArea.helpers.displaySvgActionButton('#type-actions', _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 14, 14, 'icon-green'), 'save', 'Save', _Code.persistence.runCurrentEntitySaveAction);

						let cancelButton = _Code.mainArea.helpers.displaySvgActionButton('#type-actions', _Icons.getSvgIcon(_Icons.iconCrossIcon, 14, 14, 'icon-red'), 'cancel', 'Revert changes', () => {
							_Schema.bulkDialogsGeneral.resetInputsViaTabControls(tabControls);
						});

						// delete button
						if (!entity.isPartOfBuiltInSchema) {
							_Code.mainArea.helpers.displaySvgActionButton('#type-actions', _Icons.getSvgIcon(_Icons.iconTrashcan, 14, 14, 'icon-red'), 'delete', 'Delete relationship ' + entity.relationshipType, () => {
								_Code.persistence.deleteSchemaEntity(entity, 'Delete relationship ' + entity.relationshipType + '?', 'This will delete all schema relationships as well, but no data will be removed.', data);
							});
						}

						_Helpers.disableElements(true, saveButton, cancelButton);

						document.querySelector('#code-contents .tabs-content-container')?.childNodes[0]?.addEventListener('bulk-data-change', (e) => {

							e.stopPropagation();

							let changeCount = _Schema.bulkDialogsGeneral.getChangeCountFromBulkInfo(_Schema.bulkDialogsGeneral.getBulkInfoFromTabControls(tabControls, false));
							let isDirty     = (changeCount > 0);
							_Helpers.disableElements(!isDirty, saveButton, cancelButton);

							_Code.persistence.tellFirstElementToShowDirtyState(isDirty);
						});
					});
				});
			}, 'schema');
		},
		showSwaggerUI: (data) => {

			_Code.pathLocations.updatePathLocationStack(data.path);
			_Code.lastClickedPath = data.path;

			_Helpers.fastRemoveAllChildren(_Code.codeContents[0]);

			let tagName       = data?.name;
			let baseUrl       = location.origin + location.pathname;
			let swaggerUrl    = baseUrl + 'swagger/';
			let openApiTagUrl = baseUrl + 'openapi/' + (tagName ? tagName + '.json' : '');
			let iframeSrc     = `${swaggerUrl}?url=${openApiTagUrl}`;

			_Code.codeContents.append(_Code.templates.swaggerui({ iframeSrc: iframeSrc }));
		},
		displayServiceClassesContent: (data) => {

			_Code.codeContents[0].insertAdjacentHTML('beforeend', _Code.templates.createNewType({ text: 'Service Class'}));

			let container = _Code.codeContents[0].querySelector('#create-type-container');

			_Schema.nodes.showCreateNewTypeDialog(container, null, { isServiceClass: true });

			_Code.persistence.runCurrentEntitySaveAction = () => {

				let typeData = _Schema.nodes.getTypeDefinitionDataFromForm(container, {});

				if (!typeData.name || typeData.name.trim() === '') {

					_Helpers.blinkRed(container.querySelector('[data-property="name"]'));

				} else {

					_Schema.nodes.createTypeDefinition(typeData).then(responseData => {

						_Code.tree.refreshNode(data.path);

						window.setTimeout(() => {
							// tree needs to be refreshed - implement better solution using refresh_node.jstree event
							_Code.tree.findAndOpenNode(data.path + '/' + responseData.result[0], true);
						}, 250);

					}, rejectData => {

						Structr.errorFromResponse(rejectData, undefined, { requiresConfirmation: true });
					});
				}
			};

			_Code.mainArea.helpers.displaySvgActionButton('#create-type-actions', _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 14, 14, 'icon-green'), 'create', 'Create', _Code.persistence.runCurrentEntitySaveAction);
		},
		helpers: {
			shouldHideOpenAPITabForMethod: (entity) => {

				let isJavaMethod         = (entity.codeType === 'java');
				let isLifecycleMethod    = LifecycleMethods.isLifecycleMethod(entity);
				let isNotCallableViaHTTP = (entity.isPrivate === true);

				return isJavaMethod || isLifecycleMethod || isNotCallableViaHTTP;
			},
			shouldHideAPIExampleTabForMethod: (entity) => {

				let isJavaMethod         = (entity.codeType === 'java');
				let isLifecycleMethod    = LifecycleMethods.isLifecycleMethod(entity);

				return isJavaMethod || isLifecycleMethod;
			},
			collectSchemaMethodParameters: () => {

				let container = _Code.codeContents[0].querySelector('#openapi-options');
				if (container === null) {

					// we are not showing API tab, do not return anything
					return null;
				}

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
			displaySvgActionButton: (targetId, iconSvg, suffix, name, callback) => {

				let button     = _Helpers.createSingleDOMElementFromHTML(_Code.templates.actionButton({ iconSvg: iconSvg, suffix: suffix, name: name }));
				button.addEventListener('click', callback);

				let targetNode = document.querySelector(targetId);
				targetNode.appendChild(button);

				return button;
			},
		}
	},
	helpers: {
		getAttributesToFetchForErrorObject: () => 'id,type,name,content,isStatic,ownerDocument,schemaNode',
		getPathToOpenForSchemaObject: (obj) => {

			if (obj.type === 'SchemaNode') {

				return `/root/${obj.isServiceClass === true ? 'services' : 'custom'}/${obj.id}`;

			} else {

				let firstSubFolder = (obj.schemaNode?.isServiceClass === true) ? 'services' : 'custom';
				let typeFolder     = _Code.helpers.getTypeFolderForNode(obj);

				if (obj.schemaNode) {
					return `/root/${firstSubFolder}/${obj.schemaNode.id}/${typeFolder}/${obj.id}`
				} else if (obj.type === 'SchemaRelationshipNode') {
					return `/root/${firstSubFolder}/${obj.targetId}/${typeFolder}/${obj.id}`;
				} else {
					return `/globals/${obj.id}`;
				}
			}
		},
		getTypeFolderForNode:(node) => {

			if (node.type === 'SchemaProperty') return 'properties';
			if (node.type === 'SchemaView') return 'views';
			if (node.type === 'SchemaRelationshipNode') return 'remoteproperties';
			return 'methods';
		},
		navigateToSchemaObjectFromAnywhere: (obj, updateLocationStack = false) => {

			let pathToOpen = _Code.helpers.getPathToOpenForSchemaObject(obj);
			let timeout    = (window.location.hash === '#code') ? 100 : 1000;

			window.location.href = '#code';

			window.setTimeout(() => {
				_Code.tree.findAndOpenNode(pathToOpen, updateLocationStack);
			}, timeout);
		},
		handleKeyDownEvent: (e) => {

			if (Structr.isModuleActive(_Code)) {

				let event   = e?.originalEvent ?? e;
				let keyCode = event.keyCode;
				let code    = event.code;

				// ctrl-s / cmd-s
				if ((code === 'KeyS' || keyCode === 83) && ((!_Helpers.isMac() && event.ctrlKey) || (_Helpers.isMac() && event.metaKey))) {
					event.preventDefault();
					_Code.persistence.runCurrentEntitySaveAction();
				}

				// ctrl-r / cmd-r
				if ((code === 'KeyR' || keyCode === 82) && ((!_Helpers.isMac() && event.ctrlKey) || (_Helpers.isMac() && event.metaKey))) {

					// allow browser hard-reload with CMD/CTRL+SHIFT+R
					if (!event.shiftKey) {

						let runButton = document.getElementById('action-button-run');

						if (runButton) {

							event.preventDefault();
							event.stopPropagation();

							if (!_Dialogs.custom.isDialogOpen()) {
								runButton.click();
							}
						}
					}
				}

				if (_Code.search.searchIsActive()) {
					if (e.key === 'Escape') {
						_Code.search.cancelSearch();
					}
				}
			}
		},
		preloadAvailableTagsForEntities: async () => {

			let schemaNodeTags   = await Command.queryPromise('SchemaNode', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', null, false, null, 'tags');
			let schemaMethodTags = await Command.queryPromise('SchemaMethod', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', null, false, null, 'tags');

			_Code.helpers.addAvailableTagsForEntities(schemaNodeTags);
			_Code.helpers.addAvailableTagsForEntities(schemaMethodTags);
		},
		addAvailableTagsForEntities: (entities) => {

			let change = false;

			for (let entity of entities) {

				if (entity.tags) {

					for (let tag of entity.tags) {

						if (!_Code.availableTags.includes(tag) && !_Code.tagBlacklist.includes(tag)) {
							change = true;
							_Code.availableTags.push(tag);
						}
					}
				}
			}

			if (change) {
				_Code.tree.refreshNode('/openapi');
			}

			_Code.availableTags.sort();
		},
		getElementForKey: (key) => {
			return document.querySelector(`#code-contents [data-property=${key}]`);
		},
		ensureCodeAreaIsActive: async () => {

			if (!location.hash.startsWith('#code')) {

				location.hash = 'code';
			}

			await _Helpers.waitForElement('.' + _Code.classIndicatorEverythingReady, { childList: true, subtree: true, attributes: true });

			return true;
		},
	},
	persistence: {
		saveEntityAction: (entity, callback, optionalFormDataModificationFunctions = []) => {

			if (_Code.persistence.isDirty()) {

				let formData = _Code.persistence.collectChangedPropertyData(entity);

				for (let modFn of optionalFormDataModificationFunctions) {
					modFn(formData);
				}

				fetch(Structr.rootUrl + entity.id, {
					method: 'PUT',
					body: JSON.stringify(formData)
				}).then(async response => {

					if (response.ok) {

						_Code.helpers.addAvailableTagsForEntities([formData]);

						Object.assign(entity, formData);
						_Code.persistence.updateDirtyFlag(entity);

						if (formData.name) {
							_Code.tree.refreshTree();
						}

						_Code.persistence.showSaveAction(formData);

						if (typeof callback === 'function') {
							callback();
						}

					} else {

						let data = await response.json();

						Structr.errorFromResponse(data);
					}
				});
			}
		},
		deleteSchemaEntity: (entity, title, text, data) => {

			let path  = data.path;
			let parts = path.split('/');

			parts.pop();

			let parent = parts.join('/');

			_Dialogs.confirmation.showPromise(`<h3>${title}</h3><p>${(text || '')}</p>`).then((confirm) => {

				if (confirm === true) {

					_Code.dirty = false;

					Command.deleteNode(entity.id, false, () => {
						_Code.persistence.forceNotDirty();
						_Code.tree.findAndOpenNode(parent, false);
						_Code.tree.refreshTree();
					});
				}
			});
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

			let formContent = _Code.persistence.collectChangedPropertyData(entity);
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

			_Code.persistence.tellFirstElementToShowDirtyState(dirty);

			return formContent;
		},
		tellFirstElementToShowDirtyState: (dirty) => {
			if (dirty === true) {
				_Code.codeContents.children().first().addClass('has-changes');
			} else {
				_Code.codeContents.children().first().removeClass('has-changes');
			}
		},
		testAllowNavigation: () => {
			if (_Code.persistence.isDirty()) {
				return confirm('Discard unsaved changes?');
			}

			return true;
		},
		collectPropertyData: (entity) => {

			return _Code.persistence.collectDataFromContainer(document.querySelector('#code-contents'), entity);
		},
		collectDataFromContainer: (container, entity) => {

			let data = {};

			for (let p of container.querySelectorAll('input[data-property]')) {
				switch (p.type) {
					case "checkbox":
						data[p.dataset.property] = p.checked;
						break;
					case "number":
						if (p.value) {
							data[p.dataset.property] = parseInt(p.value);
						} else {
							data[p.dataset.property] = null;
						}
						break;
					case "text":
					default:
						if (entity[p.dataset.property] === null && p.value === '') {
							data[p.dataset.property] = entity[p.dataset.property];
						} else if (p.value) {
							data[p.dataset.property] = p.value;
						} else {
							data[p.dataset.property] = null;
						}
						break;
				}
			}

			for (let p of container.querySelectorAll('select[data-property]')) {
				if (p.multiple === true) {
					data[p.dataset.property] = Array.prototype.map.call(p.selectedOptions, (o) => o.value);
				} else {

					data[p.dataset.property] = p.value;

					// add exception for typeHint
					if (p.dataset.property === 'typeHint' && p.value === 'null') {
						data.typeHint = null;
					}
				}
			}

			for (let editorWrapper of container.querySelectorAll('.editor[data-property]')) {
				let propertyName = editorWrapper.dataset.property;
				let entityId = entity?.id;
				if (!entityId) {
					entityId = editorWrapper.dataset.id;
				}
				if (!entityId) {
					console.log('Editor should be saved but ID is missing from dataset - getting data will probably fail!');
				}

				data[propertyName] = _Editors.getTextForExistingEditor(entityId, propertyName);
			}

			return data;
		},
		collectChangedPropertyData: (entity) => {

			let formContent = _Code.persistence.collectPropertyData(entity);
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
		revertFormData: (entity) => {

			_Code.persistence.revertFormDataInContainer(document.querySelector('#code-contents'), entity);

			_Code.persistence.updateDirtyFlag(entity);
		},
		revertFormDataInContainer: (container, entity) => {

			for (let p of container.querySelectorAll('input[data-property]')) {
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

			for (let p of container.querySelectorAll('select[data-property]')) {

				let isSet   = !!entity[p.dataset.property];
				let isArray = Array.isArray(entity[p.dataset.property]);

				for (let option of p.options) {
					if (!isSet) {
						option.selected = false;
					} else {
						if (isArray) {
							option.selected = entity[p.dataset.property].includes(option.value);
						} else {
							// only use id for nodes - otherwise use value directly
							let valueInEntity = (entity[p.dataset.property]?.id ?? entity[p.dataset.property]);
							option.selected = (valueInEntity === option.value);
						}
					}
				}

				p.dispatchEvent(new Event('change'));
			}

			for (let editorWrapper of container.querySelectorAll('.editor[data-property]')) {

				let propertyName = editorWrapper.dataset.property;
				let entityId     = editorWrapper.dataset.id;
				let value        = '';

				if (entity) {
					entityId = entity.id;
					value = (entity[propertyName] || '');
				} else {
					console.log('Editor should be saved but ID is missing from dataset - getting data will probably fail!');
				}

				_Editors.setTextForExistingEditor(entityId, propertyName, value);
			}
		},
		showSaveAction: (changes) => {

			for (let key of Object.keys(changes)) {
				let element = _Code.helpers.getElementForKey(key);
				if (element) {

					if (element.tagName === 'INPUT' && element.type === 'text' && !element.classList.contains('hidden')) {
						_Helpers.blinkGreen($(element));
					} else if (element.tagName === 'INPUT' && element.type === 'checkbox' && !element.classList.contains('hidden')) {
						_Helpers.blinkGreen($(element.closest('.checkbox')));
					} else {
						_Helpers.blinkGreen($(element.closest('.property-box')));
					}
				}
			}
		},
		runCurrentEntitySaveAction: () => {
			// this is the default action - it should always be overwritten by specific save actions and is only here to prevent errors
			if (_Code.persistence.isDirty()) {
				new WarningMessage().text('No save action is defined - but the editor has unsaved changes!').requiresConfirmation().show();
			}
		},
	},
	search: {
		searchThreshold: 3,
		searchTextLength: 0,
		getSearchInputElement: () => {
			return document.querySelector('#tree-search-input');
		},
		init: () => {

			let codeSearchInput = _Code.search.getSearchInputElement();

			codeSearchInput.addEventListener('input', _Helpers.debounce(_Code.search.doSearch, 300));
			$('.clearSearchIcon').on('click', _Code.search.cancelSearch);
		},
		doSearch: () => {

			let tree = $('#code-tree').jstree(true);
			let text = _Code.search.getSearchInputElement().value;

			let clearSearchIcon = Structr.functionBar.querySelector('.clearSearchIcon');

			let shouldBeBlock = (text.length >= _Code.search.searchThreshold);
			clearSearchIcon.classList.toggle('block', shouldBeBlock);

			if (text.length >= _Code.search.searchThreshold || (_Code.search.searchTextLength >= _Code.search.searchThreshold && text.length <= _Code.search.searchTextLength)) {
				tree.refresh();
			}

			_Code.search.searchTextLength = text.length;
		},
		inSearchBox: () => {
			return document.activeElement === _Code.search.getSearchInputElement();
		},
		searchIsActive: () => {

			let text = _Code.search.getSearchInputElement()?.value;
			return (text && text.length >= _Code.search.searchThreshold);
		},
		cancelSearch: () => {

			_Code.search.getSearchInputElement().value = '';
			_Code.search.doSearch();
		},

		// search from global search widget
		goToResult: (result, key, searchData) => {

			_Code.helpers.ensureCodeAreaIsActive().then(() => Command.getPromise(result.id)).then(node => {

				// console.log(node);
				let path = _Code.search.getPathForNode(node);
				// console.log(path);

				if (result.type === 'SchemaMethod' && key === 'source') {

					_Editors.highlightTextInNextEditor(searchData.queryString);
				}

				_Code.search.prepareTabForNodeAndKey(node, key);
				_Code.tree.findAndOpenNode(path);
			});
		},
		getPathForNode: (node) => {

			if (node.type === 'SchemaMethodParameter') {
				return _Code.helpers.getPathToOpenForSchemaObject(node.schemaMethod);
			}

			return _Code.helpers.getPathToOpenForSchemaObject(node);
		},
		prepareTabForNodeAndKey: (node, key) => {

			if (node.type === 'SchemaMethod') {
				if (key === 'source') {
					_Code.mainArea.saveActiveTabForMethod(node.id, 'code');
				} else {
					_Code.mainArea.saveActiveTabForMethod(node.id, 'api');
				}

			} else if (node.type === 'SchemaMethodParameter') {
				_Code.mainArea.saveActiveTabForMethod(node.schemaMethod.id, 'api');
			}
		}
	},
	recentElements: {
		codeRecentElementsKey: 'structrCodeRecentElements_' + location.port,
		init: () => {
			_Code.recentElements.updateVisibility();
		},
		isVisible: () => UISettings.getValueForSetting(UISettings.settingGroups.code.settings.showRecentsInCodeArea),
		loadRecentlyUsedElements: (doneCallback) => {

			let recentElements = LSWrapper.getItem(_Code.recentElements.codeRecentElementsKey) || [];

			for (let element of recentElements) {

				let nameIsUndefined = !element.name;
				let notSvgIcon      = !element.iconSvg;

				if (!nameIsUndefined && !notSvgIcon) {
					_Code.recentElements.addRecentlyUsedElement(element.id, element.name, element.iconSvg, element.path, true);
				}
			}

			doneCallback();
		},
		addRecentlyUsedEntity: (entity, path, fromStorage) => {

			let name      = _Code.recentElements.getDisplayNameInRecentsForType(entity);
			let iconSvg   = _Icons.getIconForSchemaNodeType(entity);
			let localPath = path;

			// don't add search results to recently used elements (we cannot construct the path)
			if (localPath.indexOf('root/searchresults/') !== 0) {
				_Code.recentElements.addRecentlyUsedElement(entity.id, name, iconSvg, localPath, fromStorage);
			}
		},
		addRecentlyUsedElement: (id, name, iconSvg, path, fromStorage) => {

			if (!fromStorage) {

				let recentElements = LSWrapper.getItem(_Code.recentElements.codeRecentElementsKey) || [];
				let updatedList    = recentElements.filter((recentElement) => (recentElement.id !== id));
				updatedList.push({ id: id, name: name, iconSvg: iconSvg, path: path });

				// keep list at length 20
				while (updatedList.length > 20) {

					let toRemove = updatedList.pop();
					_Helpers.fastRemoveElement(document.querySelector('#recently-used-' + toRemove.id));
				}

				// sort by name (keep order!)
				updatedList.sort((a, b) => a.name > b.name ? 1 : a.name < b.name ? -1 : 0);

				LSWrapper.setItem(_Code.recentElements.codeRecentElementsKey, updatedList);

				// element with id from parameter was clicked => highlight
				for (let e of document.querySelectorAll('.code-favorite')) {
					e.classList.remove('active');
				}
				document.querySelector('#recently-used-' + id)?.classList.add('active');
			}

			let recentlyUsedButton = _Helpers.createSingleDOMElementFromHTML(_Code.templates.recentlyUsedButton({ id: id, name: name, iconSvg: iconSvg }));

			let codeContext  = document.querySelector('#code-context');
			if (fromStorage) {

				codeContext.append(recentlyUsedButton);

			} else if (!document.querySelector('#recently-used-' + id)) {

				// new element => reload all so we have a sorted list
				for (let e of document.querySelectorAll('.code-favorite')) {
					_Helpers.fastRemoveElement(e);
				}
				_Code.recentElements.loadRecentlyUsedElements(()=>{});
			}

			recentlyUsedButton.addEventListener('click', () => {
				_Code.tree.findAndOpenNode(path, true);
			});

			recentlyUsedButton.querySelector('.remove-recently-used').addEventListener('click', (e) => {
				e.stopPropagation();
				_Code.recentElements.deleteRecentlyUsedElement(id);
			});
		},
		deleteRecentlyUsedElement: (recentlyUsedElementId) => {

			let recentElements         = LSWrapper.getItem(_Code.recentElements.codeRecentElementsKey) || [];
			let filteredRecentElements = recentElements.filter((recentElement) => (recentElement.id !== recentlyUsedElementId));

			_Helpers.fastRemoveElement(document.querySelector('#recently-used-' + recentlyUsedElementId));

			LSWrapper.setItem(_Code.recentElements.codeRecentElementsKey, filteredRecentElements);
		},
		updateRecentlyUsed: (entity, path, updateLocationStack) => {

			_Code.recentElements.addRecentlyUsedEntity(entity, path);

			if (updateLocationStack) {
				_Code.pathLocations.updatePathLocationStack(path);
				_Code.lastClickedPath = path;
			}
		},
		getDisplayNameInRecentsForType: (entity) => {

			let displayName = entity.name;

			switch (entity.type) {
				case 'SchemaNode':
					displayName = `Type ${entity.name}`;
					break;

				case 'SchemaMethod':
					if (entity.schemaNode && entity.schemaNode.name) {
						displayName = `${entity.schemaNode.name}.${entity.name}()`;
					} else {
						displayName = `${entity.name}()`;
					}
					break;

				case 'SchemaProperty':
					if (entity.schemaNode && entity.schemaNode.name) {
						displayName = `${entity.schemaNode.name}.${entity.name}`;
					}
					break;
			}

			return displayName;
		},
		updateVisibility: () => {

			let codeContext  = document.querySelector('#code-context');
			let isHidden     = !_Code.recentElements.isVisible();

			codeContext.classList.toggle('hidden', isHidden);
			document.querySelector('.column-resizer-right')?.classList.toggle('hidden', isHidden);

			Structr.resize();
		}
	},
	pathLocations: {
		stack: [],
		currentIndex: 0,
		init: () => {
			_Code.pathLocations.getForwardButton().addEventListener('click', _Code.pathLocations.goForward);
			_Code.pathLocations.getBackwardButton().addEventListener('click', _Code.pathLocations.goBackward);
		},
		getForwardButton: () => document.querySelector('#tree-forward-button'),
		getBackwardButton: () => document.querySelector('#tree-back-button'),
		updatePathLocationStack: (path) => {

			let pos = _Code.pathLocations.stack.indexOf(path);
			if (pos >= 0) {

				_Code.pathLocations.stack.splice(pos, 1);
			}

			// remove tail of stack when click history branches
			if (_Code.pathLocations.currentIndex !== _Code.pathLocations.stack.length - 1) {

				_Code.pathLocations.stack.splice(_Code.pathLocations.currentIndex + 1, _Code.pathLocations.stack.length - _Code.pathLocations.currentIndex);
			}

			// add element to the end of the stack
			_Code.pathLocations.stack.push(path);
			_Code.pathLocations.currentIndex = _Code.pathLocations.stack.length - 1;

			_Code.pathLocations.updateButtons();
		},
		goForward: () => {

			_Code.pathLocations.currentIndex += 1;
			let pos = _Code.pathLocations.currentIndex;

			_Code.pathLocations.updateButtons();

			if (pos >= 0 && pos < _Code.pathLocations.stack.length) {

				let path = _Code.pathLocations.stack[pos];
				_Code.tree.findAndOpenNode(path, false);

			} else {
				_Code.pathLocations.currentIndex -= 1;
			}

			_Code.pathLocations.updateButtons();
		},
		goBackward: () => {

			_Code.pathLocations.currentIndex -= 1;
			let pos = _Code.pathLocations.currentIndex;

			if (pos >= 0 && pos < _Code.pathLocations.stack.length) {

				let path = _Code.pathLocations.stack[pos];
				_Code.tree.findAndOpenNode(path, false);

			} else {

				_Code.pathLocations.currentIndex += 1;
			}

			_Code.pathLocations.updateButtons();
		},
		updateButtons: () => {

			let stackSize       = _Code.pathLocations.stack.length;
			let forwardDisabled = stackSize <= 1 || _Code.pathLocations.currentIndex >= stackSize - 1;
			let backDisabled    = stackSize <= 1 || _Code.pathLocations.currentIndex <= 0;

			let forwardButton = _Code.pathLocations.getForwardButton();
			let backButton    = _Code.pathLocations.getBackwardButton();

			forwardButton.disabled = forwardDisabled;
			backButton.disabled    = backDisabled;

			forwardButton.classList.toggle('icon-lightgrey', forwardDisabled);
			forwardButton.classList.toggle('cursor-not-allowed', forwardDisabled);
			backButton.classList.toggle('icon-lightgrey', backDisabled);
			backButton.classList.toggle('cursor-not-allowed', backDisabled);
		},
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
			<div class="flex-grow">

				<div class="tree-search-container flex" id="tree-search-container">

					<button type="button" class="tree-back-button hover:bg-gray-100 focus:border-gray-666 active:border-green flex items-center" id="tree-back-button" title="Back" disabled>
						${_Icons.getSvgIcon(_Icons.iconChevronLeftFilled, 12, 12)}
					</button>

					<div class="relative">
						<input type="text" class="tree-search-input" id="tree-search-input" placeholder="Search..." autocomplete="off">
						${_Icons.getSvgIcon(_Icons.iconCrossIcon, 12, 12, _Icons.getSvgIconClassesForColoredIcon(['clearSearchIcon', 'icon-lightgrey', 'cursor-pointer']), 'Clear Search')}
					</div>

					<button type="button" class="tree-forward-button hover:bg-gray-100 focus:border-gray-666 active:border-green flex items-center" id="tree-forward-button" title="Forward" disabled>
						${_Icons.getSvgIcon(_Icons.iconChevronRightFilled, 12, 12)}
					</button>

				</div>

			</div>
		`,
		actionButton: config => `
			<button id="action-button-${config.suffix}" class="action-button hover:bg-gray-100 focus:border-gray-666 active:border-green">
				<div class="action-button-icon">
					${config.iconSvg ?? ''}
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
		`,
		createNewType: config => `
			<div class="flex flex-wrap justify-between gap-2 mb-2">
				<h2>Create New ${config?.text ?? 'Type'}</h2>
				<div id="method-buttons">
					<div class="flex flex-wrap gap-x-4">
						<div class="mb-2">
							<div id="create-type-actions"></div>
						</div>
					</div>
				</div>
			</div>
				
			<div id="create-type-container"></div>
		`,
		cypherProperty: config => `
			<h2>CypherProperty ${config.property.schemaNode.name}.${config.property.name}</h2>
			<div id="property-buttons"></div>
			<div class="flex flex-col flex-grow">
				<div id="cypher-code-container" class="mb-4 flex flex-col h-full">
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

			<div id="function-property-container" class="code-tabs level-two flex flex-col flex-grow">
				<ul>
					<li data-name="source">Code</li>
					<li data-name="api">API</li>
				</ul>
				<div id="function-property-content" class="flex flex-col flex-grow">

					<div class="tab function-property-tab-content flex flex-col flex-grow" id="tabView-source">

						<div id="read-code-container" class="mb-4 flex flex-col h-1/2">
							<h4 class="py-2 font-semibold">Read Function</h4>
							<div class="editor flex-grow" data-property="readFunction"></div>
						</div>
						<div id="write-code-container" class="mb-4 flex flex-col h-1/2">
							<div>
								<h4 class="py-2 font-semibold" data-comment="To retrieve the parameter passed to the write function, use &lt;code&gt;Structr.get('value');&lt;/code&gt; in a JavaScript context or the keyword &lt;code&gt;value&lt;/code&gt; in a StructrScript context.">
									Write Function
								</h4>
							</div>
							<div class="editor flex-grow" data-property="writeFunction"></div>
						</div>

					</div>

					<div class="tab function-property-tab-content flex flex-col flex-grow" id="tabView-api">
						<div>
							<h4 class="py-2 font-semibold" data-comment="Write an OpenAPI schema for your return type here.">Return Type</h4>
						</div>
						<div class="editor flex-grow" data-property="openAPIReturnType"></div>
					</div>

				</div>
				<div class="editor-info"></div>
			</div>
		`,
		globals: config => `
			<h2>User-defined functions</h2>
			<div id="code-methods-container" class="content-container"></div>
		`,
		method: config => `
			<h2 class="cursor-text">
				<span id="method-name-container">
					<span>${(config.method.schemaNode ? config.method.schemaNode.name + '.' : '')}</span><span id="method-name-output">${config.method.name}</span><span>()</span>
				</span>
				<input class="hidden font-bold text-lg " type="text" id="method-name-input" data-property="name" size="60" value="${config.method.name}" autocomplete="off">
			</h2>
			<div id="method-buttons">
				<div id="method-options" class="flex flex-wrap gap-x-4">
					<div id="method-actions"></div>
					${_Schema.methods.templates.methodFlags(config)}
				</div>
			</div>
			<div id="method-code-container" class="code-tabs level-two flex flex-col flex-grow">
				<ul>
					<li data-name="source">Code</li>
					<li data-name="api">API</li>
					<li data-name="api-usage">Usage</li>
				</ul>
				<div id="methods-content" class="flex flex-col flex-grow">

					<div class="tab method-tab-content flex flex-col flex-grow" id="tabView-source">
						<div class="editor property-code flex-grow" data-property="source"></div>
					</div>

					<div class="tab method-tab-content flex flex-col flex-grow" id="tabView-api">
					</div>
					
					<div class="tab method-tab-content flex flex-col flex-grow" id="tabView-api-usage">
					</div>

				</div>
				<div class="editor-info"></div>
				<div class="logging-output"></div>
			</div>
		`,
		methods: config => `
			<h2>Methods of type ${config.data.type}</h2>
			<div id="code-methods-container" class="content-container"></div>
		`,
		openAPIBaseConfig: config => `
			<div id="openapi-options" class="flex flex-col flex-grow">

				<div class="flex flex-wrap gap-8">

					<div class="min-w-48">
						<label class="block mb-5">&nbsp;</label>
						<label class="flex"><input type="checkbox" data-property="includeInOpenAPI"> Include in OpenAPI output</label>
					</div>

					<div class="min-w-48">
						<label class="block mb-2" data-comment="Use tags to combine types and methods into an API. Each tag is available under its own OpenAPI endpoint (/structr/openapi/tag.json).">Tags</label>
						<select id="tags-select" data-property="tags" multiple="multiple">
						</select>
					</div>

					<div class="min-w-48">
						<label class="block mb-2">Summary</label>
						<input data-property="summary" type="text">
					</div>

					${(config.type === 'SchemaNode') ? '' : `
						<div class="min-w-48">
							<label class="block mb-2">Description</label>
							<input data-property="description" type="text">
						</div>
					`}
				</div>

				${(config.type === 'SchemaMethod' ? _Code.templates.openAPIMethodConfig() : '')}
			</div>
		`,
		openAPIMethodConfig: config => `
			<div class="mt-4">

				<label class="font-semibold">Parameters</label>
				<button id="add-parameter-button">
					${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-green']), 'Add parameter')}
				</button>

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
							${_Icons.getSvgIcon(_Icons.iconTrashcan, 16, 16, _Icons.getSvgIconClassesForColoredIcon(['icon-red', 'remove-action', 'ml-2']))}
						</div>
					</div>
				</div>
			</div>

			<div class="mt-4 flex flex-col flex-grow">
				<label class="font-semibold" data-comment="Write an OpenAPI schema for your return type here.">Return Type</label>
				<div class="editor flex-grow" data-property="openAPIReturnType"></div>
			</div>
		`,
		schemaMethodAPIExamples: config => `
			
			<div class="p-2">

				${(config.method.isPrivate !== true) ? _Code.templates.schemaMethodAPIExampleDetail({ id: 'fetch-example', stateKey: _Code.methodsFetchExampleStateKey, summary: 'Browser fetch()', text: config.fetchExample }) : ''}

				${(config.method.isPrivate !== true) ? _Code.templates.schemaMethodAPIExampleDetail({ id: 'curl-example', stateKey: _Code.methodsCurlExampleStateKey, summary: 'curl', text: config.curlExample }) : ''}

				${_Code.templates.schemaMethodAPIExampleDetail({ id: 'scripting-example', stateKey: _Code.methodsScriptingExampleStateKey, summary: 'Serverside JavaScript Scripting', text: config.scriptingExample })}

			</div>
		`,
		schemaMethodAPIExampleDetail: config => `

			<details id="${config.id}" ${LSWrapper.getItem(config.stateKey, 'closed')} class="py-1">

				<summary class="cursor-pointer">${config.summary}</summary>

				<div class="flex justify-between ml-4 my-2 p-4 rounded-md bg-black text-gray-ddd">
					<div usage-text class="whitespace-pre-wrap font-mono">${config.text}</div>
					
					<div>
						<button copy-button class="flex items-center bg-gray hover:bg-gray-200 focus:border-gray-666 active:border-green" style="margin: 0;">${_Icons.getSvgIcon(_Icons.iconClipboardPencil, 16, 16, 'mr-2')} Copy</button>
					</div>
				</div>

			</details>
		`,
		propertiesInherited: config => `
			<h2>Inherited Attributes of type ${config.data.type}</h2>
			<div class="content-container"></div>
		`,
		propertiesLocal: config => `
			<h2>Direct Properties of type ${config.data.type}</h2>
			<div class="content-container"></div>
		`,
		propertiesRemote: config => `
			<h2>Linked properties of type ${config.data.type}</h2>
			<div class="content-container"></div>
		`,
		property: config => `
			<h2>${config.property.schemaNode.name}.${config.property.name}</h2>
		`,
		propertyRemote: config => `
			<h2>Relationship (:${config.sourceNode.name})-[:${config.entity.relationshipType}]-&gt;(:${config.targetNode.name})</h2>
			<div id="type-actions"></div>

			<div class="tabs-container code-tabs">
				<ul></ul>
			</div>
			<div class="tabs-content-container flex-grow"></div>
		`,
		propertyOptions: config => `
			<div id="property-options">

				<div id="default-buttons" class="mb-4">
					<div id="property-actions"></div>
				</div>
				<div class="mb-4 grid grid-cols-4 gap-4">
					<div class="col-span-3" data-is-property-attribute-container>
						<label class="block mb-1 font-semibold">Name</label>
						<input type="text" id="property-name-input" data-property="name" value="${config.property.name}">
					</div>
					<div class="col-span-1" data-is-property-attribute-container>
						<label class="block mb-1 font-semibold">Default value</label>
						<input type="text" id="property-default-input" data-property="defaultValue" value="${config.property.defaultValue || ''}">
					</div>
					<div class="col-span-2" data-is-property-attribute-container>
						<label class="block mb-1 font-semibold">Format</label>
						<input type="text" id="property-format-input" data-property="format" value="${config.property.format || ''}">
					</div>
					<div class="col-span-1" data-is-property-attribute-container>
						<label class="block mb-1 font-semibold">Type hint</label>
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
					<div class="col-span-2 ${config.dbNameClass}" data-is-property-attribute-container>
						<label class="block mb-1 font-semibold">Database name</label>
						<input type="text" id="property-dbname-input" data-property="dbName" value="${config.property.dbName ?? ''}">
					</div>
				</div>

				<div class="mb-4">
					<div>
						<label class="font-semibold">Options</label>
					</div>
					<div class="mt-2 grid grid-cols-4 gap-4">
						<div data-is-property-attribute-container>
							<label><input type="checkbox" id="property-unique" data-property="unique" ${config.property.unique ? 'checked' : ''}>Property value must be unique</label>
						</div>
						<div data-is-property-attribute-container>
							<label><input type="checkbox" id="property-composite" data-property="compound" ${config.property.compound ? 'checked' : ''}>Include in composite uniqueness</label>
						</div>
						<div data-is-property-attribute-container>
							<label><input type="checkbox" id="property-notnull" data-property="notNull" ${config.property.notNull ? 'checked' : ''}>Property value must not be null</label>
						</div>
						<div data-is-property-attribute-container>
							${_Schema.properties.templates.functionProperty.readFunctionWrapJS({ entity: config.property })}
						</div>
						<div data-is-property-attribute-container>
							<label><input type="checkbox" id="property-indexed" data-property="indexed" ${config.property.indexed ? 'checked' : ''}>Property value is indexed</label>
						</div>
						<div data-is-property-attribute-container>
							<label><input type="checkbox" id="property-fulltext-indexed" data-property="fulltext" ${config.property.fulltext ? 'checked' : ''}>Property value is fulltext indexed</label>
						</div>
						<div data-is-property-attribute-container>
							<label><input type="checkbox" id="property-cached" data-property="isCachingEnabled" ${config.property.isCachingEnabled ? 'checked' : ''}>Property value can be cached</label>
						</div>
						<div data-is-property-attribute-container>
							<label data-comment="If active, the property will not be serialized (REST/CSV etc) but can be used otherwise (e.g. scripting). It will not be shown in the backend UI in the data area and dialogs. This setting overrides view configurations. Such attributes will still not be serialized even if they are in a view.">
								<input type="checkbox" id="property-is-serialization-disabled" data-property="isSerializationDisabled" ${config.property.isSerializationDisabled ? 'checked' : ''}>Disable Property serialization (via REST)
							</label>
						</div>
						<div data-is-property-attribute-container>
							${_Schema.properties.templates.functionProperty.writeFunctionWrapJS({ entity: config.property })}
						</div>

					</div>
				</div>
			</div>
		`,
		recentlyUsedButton: config => `
			<div class="code-favorite items-center px-2 py-1" id="recently-used-${config.id}">
				${config.iconSvg ? config.iconSvg : ''}
				${config.iconClass ? `<i class="${config.iconClass} flex-none"></i>` : ''}
				<div class="truncate flex-grow">${config.name}</div>
				${_Icons.getSvgIcon(_Icons.iconCrossIcon, 14, 14, _Icons.getSvgIconClassesForColoredIcon(['flex-none', 'icon-grey', 'remove-recently-used']))}
			</div>
		`,
		root: config => `
			<div id="all-types" style="position: relative; width: 100%; height: 98%;">
			</div>
		`,
		stringProperty: config => `
			<h2>StringProperty ${config.property.schemaNode.name}.${config.property.name}</h2>
			<div id="property-buttons"></div>
		`,
		type: config => `
			<div class="flex flex-wrap justify-between gap-2 mb-2">
				<h2>Type ${config.type.name}</h2>
				<div id="type-actions"></div>
			</div>
			
			<div class="tabs-container code-tabs">
				<ul></ul>
			</div>
			<div class="tabs-content-container flex-grow"></div>
		`,
		viewOptions: config => `
			<div id="view-options">

				<div class="mb-4">
					<div id="view-actions"></div>
				</div>

				<div class="mb-4">
					<div>
						<label class="font-semibold">Name</label>
						<input type="text" id="view-name-input" data-property="name" value="${config.view.name}" ${_Schema.views.isViewNameChangeForbidden(config.view) === true ? 'disabled' : ''}>
					</div>
				</div>

				<div class="mb-4">
					<div>
						<label class="font-semibold">Properties</label>
					</div>
					<input type="text" id="view-sort-order" class="hidden" data-property="sortOrder" value="${config.view.sortOrder || ''}">
					<div id="view-properties">
						<div class="view-properties-select">
							<select class="property-attrs view" multiple="multiple"></select>
						</div>
					</div>
				</div>
			</div>
		`,
		views: config => `
			<h2>Views of type ${config.data.type}</h2>
			<div class="content-container"></div>
		`,
		swaggerui: config => `
			<div id="swagger-ui-container" class="flex-grow">
				<iframe class="border-0" src="${config.iframeSrc}" width="100%" height="100%"></iframe>
			</div>
		`
	}
};