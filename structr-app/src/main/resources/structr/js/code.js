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
			_Code.updatedResizers(left);
		});
	},
	updatedResizers: (left) => {

		left  = left || LSWrapper.getItem(_Code.codeResizerLeftKey) || _Code.leftTabMinWidth;
		left  = Structr.leftColumn.fit(left, _Code.leftTabMinWidth, _Code.codeMain[0].querySelector('.column-resizer-left'));

		let availableWidth = _Code.codeMain.innerWidth();

		_Code.codeMain[0].querySelector('.column-resizer-left').style.left     = `${left}px`;

		document.getElementById('code-tree').style.width              = `calc(${left}px - 1rem)`;
		document.getElementById('code-contents').style.width          = `calc(${availableWidth}px - ${left}px - 5rem)`;

		_Code.mainArea.typeTabs.align();

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

			_Code.pathLocations.init();
			_Code.search.init();
			_Code.init();

			_Code.codeMain     = $('#code-main');
			_Code.codeTree     = $('#code-tree');
			_Code.codeContents = $('#code-contents');

			_Code.mainArea.typeTabs.toolbar();

			Structr.initVerticalSlider($('.column-resizer-left', _Code.codeMain), _Code.codeResizerLeftKey, _Code.leftTabMinWidth, _Code.moveLeftResizer);

			$.jstree.defaults.core.themes.dots      = false;
			$.jstree.defaults.dnd.inside_pos        = 'last';
			$.jstree.defaults.dnd.large_drop_target = true;

			_Code.codeTree.on('select_node.jstree', _Code.tree.handleTreeClick);
			_Code.codeTree.on('refresh.jstree', _Code.tree.activateLastClicked);
			_Code.codeTree.on('ready.jstree', () => {
				_Helpers.setModuleReadyIndicator(_Code, _Code.codeMain[0]);
			});

			_TreeHelper.initTree(_Code.codeTree, _Code.tree.treeInitFunction, 'structr-ui-code');

			_Schema.relationships.checkAndWarnAboutNonUniqueRelationshipNames();

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
						id:       path + '/scratchpads',
						text:     'Scratchpads',
						children: true,
						icon:     _Icons.nonExistentEmptyIcon,
						li_attr:  { 'data-id': 'scratchpads' },
						data: {
							probe:   true,
							svgIcon: _Icons.getSvgIcon(_Icons.iconScratchpad, 16, 24),
							key:     'scratchpads',
							content: 'scratchpads',
							path:    path + '/scratchpads'
						},
					},
					{
						id:       path + '/globals',
						text:     'User-defined functions',
						children: true,
						icon:     _Icons.nonExistentEmptyIcon,
						li_attr:  { 'data-id': 'globals' },
						data: {
							probe:   true,
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
						id:       path + '/datasources',
						text:     'Data Sources',
						children: true,
						icon:     _Icons.nonExistentEmptyIcon,
						li_attr:  { 'data-id': 'datasources' },
						data: {
							probe:   true,
							svgIcon: _Icons.getSvgIcon(_Icons.iconSchemaPropertyCypher, 18, 24),
							key:     'datasources',
							content: 'datasources',
							path:    path + '/datasources'
						},
					},
					{
						id:       path + '/processes',
						text:     'Processes',
						children: true,
						icon:     _Icons.nonExistentEmptyIcon,
						li_attr:  { 'data-id': 'processes' },
						data: {
							probe:   true,
							svgIcon: _Icons.getSvgIcon(_Icons.iconProcesses, 18, 24),
							key:     'processes',
							content: 'processes',
							path:    path + '/processes'
						},
					},
					_Code.mainArea.deploymentConfigurationScripts.getTreeEntry(path),
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
									probe:   true,
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
									probe:   true,
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

					case 'scratchpads':
						_Code.mainArea.scratchpad.load().then(pads => {
							callback(pads.map(pad => _Code.mainArea.scratchpad.getTreeEntry(pad, data.path)));
						});
						break;

					case 'openapi':
						_Code.tree.displayFunction(_Code.availableTags.map(t => { return { id: t, name: t, type: "OpenAPITag" } }), data);
						break;

					case 'datasources':
						Command.query('DataSource', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { '!type': 'Folder' }, result => {
							let sources = result.filter(s => s.type !== 'SchemaNode' && s.type !== 'Folder');
							_Code.tree.displayFunction(sources, data);
						}, true, 'ui');
							break;

					case 'processes':
						// Top level: BpmnDefinitions as folders.
						Command.query('BpmnDefinitions', 2000, 1, 'name', 'asc', {}, defs => {
							let list = (defs || []).map(def => {
								let baseName = def.processName || def.name || '[unnamed]';
								let label    = (def.version != null) ? `${baseName} (v${def.version})` : baseName;
								return {
									id:       data.path + '/' + def.id,
									text:     label,
									children: true,
									icon:     _Icons.nonExistentEmptyIcon,
									li_attr:  { 'data-id': def.id },
									data: {
										probe:   true,
										svgIcon: _Icons.getSvgIcon(_Icons.iconProcess, 16, 24),
										key:     'bpmnDefinitionMethods',
										content: 'bpmnDefinitionMethods',
										id:      def.id,
										path:    data.path + '/' + def.id
									},
								};
							});
							_Helpers.sort(list, 'text');
							data.callback(list);
						}, true, 'ui');
						break;

					case 'deploy-configs':
						data.callback(_Code.mainArea.deploymentConfigurationScripts.getTreeChildren(path));
						break;

					case 'bpmnDefinitionMethods':
						// Per-definition: BpmnElements that have attached methods.
						// Filtered client-side because Command.query has no "non-empty
						// related collection" predicate.
						//
						// Multi-process refactor: BpmnElement has no `definition`
						// field; it relates to its BpmnProcess via `process`, and
						// BpmnProcess relates to BpmnDefinitions via `definition`.
						// Fetch processes for this definition, then aggregate
						// their elements. A single definition may host several
						// processes (collaboration), so concatenate across all.
						Command.query('BpmnProcess', 1000, 1, 'name', 'asc', { definition: data.id }, procs => {
							const procIds = (procs || []).map(p => p.id);
							if (procIds.length === 0) { data.callback([]); return; }
							let pending = procIds.length;
							let allElements = [];
							const finish = () => {
								let withMethods = allElements.filter(e => Array.isArray(e.methods) && e.methods.length > 0);
								let list = withMethods.map(e => {
									let label = e.bpmnName || e.bpmnId || e.name || '[unnamed]';
									return {
										id:       data.path + '/' + e.id,
										text:     label,
										children: true,
										icon:     _Icons.nonExistentEmptyIcon,
										li_attr:  { 'data-id': e.id },
										data: {
											svgIcon: _Icons.getSvgIcon(_Icons.iconTask, 16, 24),
											key:     'bpmnElementMethods',
											content: 'bpmnElementMethods',
											id:      e.id,
											path:    data.path + '/' + e.id
										},
									};
								});
								_Helpers.sort(list, 'text');
								data.callback(list);
							};
							for (const pid of procIds) {
								Command.query('BpmnElement', 2000, 1, 'bpmnName', 'asc', { process: pid }, elements => {
									allElements = allElements.concat(elements || []);
									if (--pending === 0) finish();
								}, true, 'ui');
							}
						}, true, 'ui');
						break;

					case 'bpmnElementMethods':
						// Leaves: SchemaMethods attached to a single BpmnElement via
						// HAS_METHOD. Clicking a leaf routes through handleNodeObjectClick
						// to displaySchemaMethodContent (key: 'SchemaMethod').
						Command.get(data.id, null, entity => {
							let methods = (entity?.methods || []);
							let list = methods.map(m => ({
								id:       data.path + '/' + m.id,
								text:     (m.name || '[unnamed]') + '()',
								children: false,
								icon:     _Icons.nonExistentEmptyIcon,
								li_attr:  { 'data-id': m.id },
								data: {
									svgIcon: _Icons.getSvgIcon(_Icons.iconSchemaMethods, 16, 24),
									key:     'SchemaMethod',
									id:      m.id,
									path:    data.path + '/' + m.id
								},
							}));
							_Helpers.sort(list, 'text');
							data.callback(list);
						}, 'ui');
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
		createDataSourceEntry: (entity, path) => {

			// A data source is created without a name, so a nameless entry is normal and has to stay
			// selectable - but it has to say what it is, otherwise the tree shows a row that cannot be
			// attributed to anything.
			let text = entity.name || `[unnamed ${entity.type}]`;
			let icon = (entity.type === 'QueryDataSource') ? _Icons.iconFilterFunnel : _Icons.iconScriptWrapped;

			return {
				id:       path + '/' + entity.id,
				text:     text,
				children: false,
				icon:     icon,
				li_attr:  { 'data-id': entity.id },
				data: {
					svgIcon:      _Icons.getSvgIcon(icon, 16, 24),
					key:          entity.type,
					isDataSource: true,
					id:           entity.id,
					path:         path + '/' + entity.id
				},
			};
		},
		displayFunction: (result, data, dontSort, isSearch) => {

			let path = data.path;
			let list = [];

			for (let entity of result) {

				// skip HTML entities
				if (entity?.category !== 'html') {

					let icon = _Icons.getIconForSchemaNodeType(entity);

					// Everything below "Data Sources" is a data source, whatever its concrete type: the
					// section lists nodes by the DataSource label, not by a fixed set of types. A type
					// without a case of its own below fell through to the generic branch, where it showed
					// up as an unattributable "[unnamed]" row that did nothing when clicked.
					if (data.key === 'datasources') {

						list.push(_Code.tree.createDataSourceEntry(entity, path));

						continue;
					}

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

						// reached from search results; the "Data Sources" section is handled above
						case 'QueryDataSource':
						case 'ScriptDataSource':
						case 'DataSource':
							list.push(_Code.tree.createDataSourceEntry(entity, path));
							break;

						case 'SchemaNode': {

							let doHighlight = false;  //entity?.schemaMethods?.length > 0;

							list.push({
								id:       path + '/' + entity.id,
								text:     entity.name,
								children: _Code.tree.getChildElementsForSchemaNode(entity, path + '/' + entity.id),
								icon:     _Icons.nonExistentEmptyIcon,
								li_attr:  { 'data-id': entity.id, class: `${doHighlight ? 'highlight-name' : ''}${entity.isAbstract ? ' abstract-type' : ''}${entity.isInterface ? ' interface-type' : ''}`.trim() },
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

						case 'ApplicationConfigurationDataNode': {

							list.push(_Code.mainArea.deploymentConfigurationScripts.getTreeChildForKey(path, entity.name));

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

			let text           = $('#tree-search-input').val();
			let searchPromises = [];

			let parts = text.split('.');

			if (parts.length === 2 && parts[1].trim() !== '') {

				let handleExactSchemaNodeSearch = (result) => {

					let transformed = [];

					transformed.concat(result);

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
						transformed.concat(matchingMethods);

						let matchingProperties = [];
						for (let property of schemaNode.schemaProperties) {

							if (property.name.indexOf(parts[1]) === 0) {

								// populate backRef to schemaNode because it only contains id by default
								property.schemaNode = schemaNode;

								matchingProperties.push(property);
							}
						}
						transformed.concat(matchingProperties);
					}
				};

				Command.queryPromise('SchemaNode', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { name: parts[0] }, true).then(data => {
					return handleExactSchemaNodeSearch(data);
				});

			} else {

				searchPromises.push(Command.queryPromise('SchemaNode',     _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { name: text }, false));
				searchPromises.push(Command.queryPromise('SchemaProperty', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { name: text }, false));
				searchPromises.push(Command.queryPromise('SchemaMethod',   _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { name: text }, false));
			}

			// text search always happens
			searchPromises.push(Command.queryPromise('SchemaMethod',   _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { source: text}, false));
			searchPromises.push(Command.queryPromise('SchemaProperty', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { writeFunction: text}, false));
			searchPromises.push(Command.queryPromise('SchemaProperty', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { readFunction: text}, false));

			searchPromises.push(Command.queryPromise('ApplicationConfigurationDataNode', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { content: text }, false).then(deployConfigScripts => {
				return deployConfigScripts.filter(acdn => (acdn.configType === 'pre-deploy-conf' || acdn.configType === 'post-deploy-conf'));
			}));

			Promise.all(searchPromises).then(searchResults => {

				let searchResultMap = {};

				for (let searchResult of searchResults) {

					// remove duplicates
					for (let r of searchResult) {
						searchResultMap[r.id] = r;
					}
				}

				_Code.tree.displayFunction(Object.values(searchResultMap), data, false, true);
			});
		},
		hasVisibleChildren: (id, entity) => {

			return (entity?.schemaMethods ?? []).some(method => {

				return (id === 'custom' || !method.isPartOfBuiltInSchema);
			});
		},
		handleTreeClick: (evt, data) => {

			if (_Code.mainArea.typeTabs.selectingInTree) {
				return;
			}

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

			if (_Code.mainArea.typeTabs.showLoaded(data)) {
				return;
			}

			if (_Code.persistence.testAllowNavigation()) {

				_Code.dirty = false;
				_Code.additionalDirtyChecks = [];

				// clear page
				_Code.mainArea.clearMainArea();
				_Code.mainArea.typeTabs.showCreateButtons(data.content ?? data.key);

				switch ((data.key === 'root' || data.key === 'searchresults') ? data.key : data.content) {

					case 'root':
						_Code.mainArea.displayCreateAreas(data);
						break;

					case 'searchresults':
						_Code.codeContents[0].insertAdjacentHTML('beforeend', '<p class="overview-note">Select a result in the tree to open it.</p>');
						break;

					case 'undefined':
					case 'null':
						break;

					case 'processes':
						_Code.mainArea.displayProcessesContent(data);
						break;

					case 'bpmnDefinitionMethods':
						_Code.mainArea.displayProcessContent(data);
						break;

					case 'scratchpads':
						_Code.mainArea.scratchpad.showOverview(data);
						break;

					case 'scratchpad':
						_Code.mainArea.scratchpad.show(data);
						break;

					case 'globals':
						_Code.mainArea.displayGlobalMethodsContent(data, true);
						break;

					case 'openapi':
						_Code.mainArea.showSwaggerUI(data);
						break;

					case 'datasources':
						_Code.mainArea.displayDataSourcesContent(data);
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
					case 'remoteproperties':
					case 'views':
					case 'methods':
					case 'inheritedproperties':
						_Code.mainArea.displaySchemaNodeContent(data);
						break;

					case 'inherited':
						_Code.tree.findAndOpenNode(data.path, true);
						break;

					case 'deploy-configs':
						_Code.mainArea.deploymentConfigurationScripts.showOverview(data);
						break;

					case 'pre-deploy-conf':
						_Code.mainArea.deploymentConfigurationScripts.editDeploymentConfigurationScript(data);
						break;

					case 'post-deploy-conf':
						_Code.mainArea.deploymentConfigurationScripts.editDeploymentConfigurationScript(data);
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

					let attrName = (out ? rel.targetJsonName : rel.sourceJsonName);

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

			// asked before the type switch below, so that a data source whose type has no case there
			// still opens instead of silently doing nothing
			if (data.isDataSource) {

				_Code.mainArea.displayDataSourceContent(data);

				return;
			}

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

					case 'QueryDataSource':
					case 'ScriptDataSource':
					case 'DataSource':
						_Code.mainArea.displayDataSourceContent(data);
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

					// the tree only moves if the entry is not in view
					tree.get_node(node.id, true)?.[0]?.querySelector(':scope > a')?.scrollIntoView({ block: 'nearest' });

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
			_Code.codeContents[0].classList.remove('create-areas');
			_Code.mainArea.typeTabs.remove();

			_Code.persistence.runCurrentEntitySaveAction = null;
		},
		updateLocationStack: (data, updateLocationStack) => {

			if (updateLocationStack === true) {
				_Code.pathLocations.updatePathLocationStack(data.path);
				_Code.lastClickedPath = data.path;
			}
		},
		displayGlobalMethodsContent: (data, updateLocationStack) => {

			_Code.mainArea.updateLocationStack(data, updateLocationStack);

			_Code.mainArea.globalsPath = data.path;

			let yesNo = (flag) => (flag ? 'yes' : 'no');

			_Code.mainArea.displayOverview(data, ['Name', 'Callable via HTTP', 'Wrap JavaScript in main()', 'Return result object only'], (addRow) => {

				_Schema.methods.fetchUserDefinedMethods((methods) => {

					for (let method of methods) {

						addRow(method.id, [
							`${_Icons.getIconForSchemaNodeType(method)}<b>${_Helpers.escapeTags(method.name)}</b>`,
							(method.isPrivate ? 'no' : (method.httpVerb ?? 'POST')),
							yesNo(method.wrapJsInMain),
							yesNo(method.returnRawResult)
						]);
					}
				});
			});
		},
		// creates a user-defined function, or opens the lifecycle method of that name if it exists already
		createGlobalMethod: (lifecycleName) => {

			_Schema.methods.fetchUserDefinedMethods((methods) => {

				let names    = methods.map(method => method.name);
				let existing = methods.find(method => method.name === lifecycleName);
				let name     = lifecycleName;

				if (existing) {
					_Code.search.goToResult(existing);
					return;
				}

				for (let suffix = 1; !name || names.includes(name); suffix++) {
					name = 'newFunction' + (suffix > 1 ? suffix : '');
				}

				Command.create({ type: 'SchemaMethod', name: name, httpVerb: 'POST' }, (result) => {
					_Code.mainArea.renameOnOpen = (lifecycleName ? null : result.id);
					_Code.tree.refreshNode(_Code.mainArea.globalsPath);
					_Code.search.goToResult(result);
				});
			});
		},
		displayDataSourcesContent: (data) => {

			_Code.mainArea.dataSourcesPath = data.path;

			_Code.mainArea.displayOverview(data, ['Name', 'Kind'], (addRow) => {

				Command.query('DataSource', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { '!type': 'Folder' }, (result) => {

					for (let source of result.filter(s => s.type !== 'SchemaNode' && s.type !== 'Folder')) {

						let name = (source.name ? `<b>${_Helpers.escapeTags(source.name)}</b>` : `<span class="type-kind">[unnamed ${source.type}]</span>`);

						let icon = _Icons.getSvgIcon((source.type === 'QueryDataSource') ? _Icons.iconFilterFunnel : _Icons.iconScriptWrapped, 16, 24, ['flex-shrink-0']);

						addRow(source.id, [icon + name, (source.type === 'QueryDataSource' ? 'Query' : (source.type === 'ScriptDataSource' ? 'Script' : source.type))]);
					}

				}, true, 'ui');
			});
		},
		createDataSource: (type) => {

			Command.create({ type: type }, (result) => {
				_Code.tree.refreshNode(_Code.mainArea.dataSourcesPath);
				_Code.search.goToResult(result);
			});
		},
		displayProcessesContent: (data) => {

			_Code.mainArea.displayOverview(data, ['Name', 'Version'], (addRow) => {

				Command.query('BpmnDefinitions', 2000, 1, 'name', 'asc', {}, (definitions) => {

					for (let definition of (definitions ?? [])) {
						addRow(definition.id, [`${_Icons.getSvgIcon(_Icons.iconProcess, 16, 24, ['flex-shrink-0'])}<b>${_Helpers.escapeTags(definition.processName || definition.name || '[unnamed]')}</b>`, definition.version ?? '']);
					}
				});
			});

			_Code.codeContents[0].insertAdjacentHTML('beforeend', '<p class="overview-note">Processes are created and imported in the <a href="#processes">Processes area</a>.</p>');
		},
		// one process: its elements that carry scripts, and the way to its diagram
		displayProcessContent: (data) => {

			let kindOf     = (element) => (element.type ?? '').replace(/^Bpmn/, '').replace(/([a-z])([A-Z])/g, '$1 $2');
			let isSelected = () => (_Code.codeTree.jstree(true).get_selected()[0] === data.path);

			_Code.mainArea.displayOverview(data, ['Element', 'Kind', 'Methods'], (addRow) => {

				Command.query('BpmnProcess', 1000, 1, 'name', 'asc', { definition: data.id }, async (processes) => {

					let elements = [];

					for (let process of (processes ?? [])) {
						elements.push(...await new Promise(resolve => Command.query('BpmnElement', 2000, 1, 'bpmnName', 'asc', { process: process.id }, result => resolve(result ?? []), true, 'ui')));
					}

					if (!isSelected()) {
						return;
					}

					let scripted = elements.filter(element => Array.isArray(element.methods) && element.methods.length > 0);

					for (let element of scripted) {

						addRow(element.id, [
							`${_Icons.getSvgIcon(_Icons.iconTask, 16, 24, ['flex-shrink-0'])}<b>${_Helpers.escapeTags(element.bpmnName || element.bpmnId || element.name || '[unnamed]')}</b>`,
							_Helpers.escapeTags(kindOf(element)),
							element.methods.length
						]);
					}

					if (scripted.length === 0) {

						_Code.codeContents[0].querySelector('.schema-grid')?.remove();
						_Code.codeContents[0].insertAdjacentHTML('beforeend', '<p class="overview-note">No element of this process carries a script. Scripts are attached to elements in the diagram.</p>');
					}

				}, true, 'ui');
			});

			Command.get(data.id, null, (definition) => {

				if (!isSelected()) {
					return;
				}

				let name  = definition?.processName || definition?.name || '[unnamed]';
				let facts = [(definition?.version != null) ? `Version ${definition.version}` : null, `${(definition?.processes ?? []).length} ${(definition?.processes ?? []).length === 1 ? 'process' : 'processes'}`].filter(Boolean).join(' · ');

				_Code.codeContents[0].insertAdjacentHTML('afterbegin', `
					<h2 class="with-icon">${_Icons.getSvgIcon(_Icons.iconProcess, 16, 24, ['flex-shrink-0'])}${_Helpers.escapeTags(name)}</h2>
					<p class="overview-note process-facts">${_Helpers.escapeTags(facts)}</p>
				`);

				// the diagram of the process is edited in the Processes area
				let openButton = _Code.mainArea.typeTabs.toolbar().querySelector('button[data-level="bpmnDefinitionMethods"]');

				openButton.onclick = () => {
					location.hash = '#processes';

					_Helpers.waitForElement('#processDefinitionsTable').then(() => _ProcessDiagram.show({ id: data.id, name: name }, name));
				};

			}, 'ui');
		},
		// the overview of a level of the tree: one row per entry, a click opens it
		displayOverview: (data, columns, load) => {

			_Code.mainArea.updateLocationStack(data, data.updateLocationStack);

			let table = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.schemaGrid({
				class: 'type-overview schema-props grid',
				style: `grid-template-columns: minmax(0, 3fr) repeat(${columns.length - 1}, minmax(0, 1fr));`,
				cols: columns.map(title => ({ class: 'pb-2 px-2 font-bold flex', title: title })),
				noButtons: true
			}));

			let gridBody = table.querySelector('.schema-grid-body');
			table.querySelector('.schema-grid-footer').classList.add('hidden');

			_Code.codeContents[0].prepend(table);

			load((id, cells) => {

				let row = _Helpers.createSingleDOMElementFromHTML(`
					<div class="schema-grid-row contents cursor-pointer">
						${cells.map(cell => `<div class="p-2 flex items-center gap-2">${cell}</div>`).join('')}
					</div>
				`);

				row.addEventListener('click', () => {
					_Code.tree.findAndOpenNode(`${data.path}/${id}`, true);
				});

				gridBody.appendChild(row);
			});
		},
		displayCustomTypesContent: (data) => {
			_Code.mainArea.displayTypeOverview(data, false);
		},
		displayCreateAreas: (data) => {

			_Code.mainArea.updateLocationStack(data, data.updateLocationStack);

			_Code.codeContents[0].classList.add('create-areas');

			for (let isServiceClass of [false, true]) {

				let label = (isServiceClass ? 'Create Service Class' : 'Create Type');
				let path  = (isServiceClass ? '/root/services' : '/root/custom');
				let area  = _Helpers.createSingleDOMElementFromHTML(`
					<div class="create-area">
						<h2>${label}</h2>
						<div class="create-area-form"></div>
						<div class="create-area-actions flex"></div>
					</div>
				`);

				_Code.codeContents[0].appendChild(area);

				let form = area.querySelector('.create-area-form');

				_Schema.nodes.showCreateNewTypeDialog(form, null, { isServiceClass: isServiceClass });

				// two forms on one page: the tags select needs an id of its own, select2 keeps its state by id
				let tagsSelect = $('#tags-select', form);
				if (tagsSelect.hasClass('select2-hidden-accessible')) {
					tagsSelect.select2('destroy');
				}
				tagsSelect.removeData('select2Id').removeAttr('data-select2-id').attr('id', `tags-select-${isServiceClass ? 'service' : 'type'}`).select2({ tags: true, width: '100%' });

				let button = _Helpers.createSingleDOMElementFromHTML(_Code.templates.actionButton({ iconSvg: _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, 'icon-green'), suffix: 'create', name: label }));

				area.querySelector('.create-area-actions').appendChild(button);

				button.addEventListener('click', () => {

					let typeData = _Schema.nodes.getTypeDefinitionDataFromForm(form, {});

					if (!typeData.name || typeData.name.trim() === '') {

						_Helpers.blinkRed(form.querySelector('[data-property="name"]'));

					} else {

						_Schema.nodes.createTypeDefinition(typeData).then(responseData => {

							_Code.tree.refreshNode(path);

							// tree needs to be refreshed - implement better solution using refresh_node.jstree event
							window.setTimeout(() => _Code.tree.findAndOpenNode(`${path}/${responseData.result[0]}`, true), 250);

						}, rejectData => {

							Structr.errorFromResponse(rejectData, undefined, { requiresConfirmation: true });
						});
					}
				});
			}
		},
		displayTypeOverview: (data, isServiceClass) => {

			_Code.mainArea.updateLocationStack(data, data.updateLocationStack);

			let countColumns = (isServiceClass ? ['Methods'] : ['Direct properties', 'Linked properties', 'Views', 'Methods']);
			let tableConfig  = {
				class: 'type-overview schema-props grid',
				style: `grid-template-columns: [ name ] minmax(0, 2fr) [ traits ] minmax(0, 3fr) repeat(${countColumns.length}, minmax(5%, max-content));`,
				cols: [
					{ class: 'pb-2 px-2 font-bold flex', title: 'Name' },
					{ class: 'pb-2 px-2 font-bold flex', title: 'Traits' },
					...countColumns.map(title => ({ class: 'pb-2 px-2 font-bold flex justify-center', title: title }))
				],
				noButtons: true
			};

			let table    = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.schemaGrid(tableConfig));
			let gridBody = table.querySelector('.schema-grid-body');
			table.querySelector('.schema-grid-footer').classList.add('hidden');

			_Code.codeContents[0].appendChild(table);

			Command.query('SchemaNode', _Code.defaultPageSize, _Code.defaultPage, 'name', 'asc', { isServiceClass: isServiceClass }, (types) => {

				for (let type of types) {

					let counts = (isServiceClass ? [type.schemaMethods] : [type.schemaProperties, [...(type.relatedTo ?? []), ...(type.relatedFrom ?? [])], type.schemaViews, type.schemaMethods]);
					let kind   = (type.isInterface ? 'interface' : (type.isAbstract ? 'abstract' : ''));
					let row    = _Helpers.createSingleDOMElementFromHTML(`
						<div class="schema-grid-row contents cursor-pointer" title="Open type ${_Helpers.escapeForHtmlAttributes(type.name)}">
							<div class="p-2 flex items-center gap-2">${_Icons.getIconForSchemaNodeType(type)}<b>${type.name}</b><span class="type-kind">${kind}</span></div>
							<div class="p-2 flex items-center">${(type.inheritedTraits ?? []).join(', ')}</div>
							${counts.map(list => `<div class="p-2 flex items-center justify-center">${(list ?? []).length}</div>`).join('')}
						</div>
					`);

					row.addEventListener('click', () => {
						_Code.tree.findAndOpenNode(`${data.path}/${type.id}`, true);
					});

					gridBody.appendChild(row);
				}

			}, true, 'ui');
		},
		displayBuiltInTypesContent: () => {
			_Code.codeContents.append(_Code.templates.builtin());
		},
		displayDataSourceContent: (data) => {

			fetch(`${Structr.rootUrl}${data.id}`).then(response => {

				if (response.ok) {
					return response.json();
				} else {
					throw Error("Unable to fetch schema node content");
				}

			}).then(json => {

				let entity = json.result;

				_Helpers.fastRemoveAllChildren(_Code.codeContents[0]);

				if (entity.type === 'QueryDataSource') {

					_Code.codeContents.append(_Code.templates.dataSourceEditor(entity.name, entity));

					let container = _Code.codeContents[0].querySelector('#datasource-container');

					container.insertAdjacentHTML('beforeend', _Code.templates.queryDataSourceEditor(entity));

					let queryContainer   = container.querySelector('#query-builder-container');
					let queryInput       = container.querySelector('#data-source-query-input');
					let dataTypeDropdown = container.querySelector('#data-source-query-type-input');

					queryInput.value = entity.query || '';

					let customTypes      = undefined;
					let systemTypes      = undefined;

					_Schema.caches.getFilteredSchemaTypes(t => true).then(async types => {
						let whitelist = ['File','Folder','Group','Image','Page','User'];
						types.sort((a, b) => a.name.localeCompare(b.name));
						for (let type of types) {
							let option = document.createElement('option');
							option.value = type.name;
							option.text  = type.name;
							if (type.isBuiltin) {
								if (whitelist.includes(type.name)) {
									if (!systemTypes) {
										systemTypes = document.createElement('optgroup');
										systemTypes.label = 'System Types';
										dataTypeDropdown.appendChild(systemTypes);
									}
									systemTypes.appendChild(option);
								}
							} else {
								if (!customTypes) {
									customTypes = document.createElement('optgroup');
									customTypes.label = 'Custom Types';
									dataTypeDropdown.appendChild(customTypes);
								}
								customTypes.appendChild(option);
							}
							if (entity.dataType === type.name) {
								option.selected = true;
							}
						}

						await loadFromQuery(entity.query);
					});

					const loadTypeProperties = (typeName) => new Promise(resolve => {
						if (!typeName || typeName.startsWith('---')) { resolve([]); return; }
						_Schema.caches.getTypeInfo(typeName, props => {
							resolve(Object.values(props).sort((a, b) => a.jsonName.localeCompare(b.jsonName)));
						});
					});

					const populateKeySelect = async (keySelect) => {
						const saved = keySelect.value;
						while (keySelect.options.length > 1) keySelect.remove(1);
						const props = await loadTypeProperties(dataTypeDropdown.value);
						for (const prop of props) {
							const opt = document.createElement('option');
							opt.value = prop.jsonName;
							opt.text  = prop.jsonName;
							if (prop.jsonName === saved) opt.selected = true;
							keySelect.appendChild(opt);
						}
					};

					const triggerUpdate = () => {
						_Code.queryBuilder.updateQuery(queryContainer, dataTypeDropdown, queryInput);
						_Code.persistence.updateDirtyFlag(entity);
					};

					const updateValueVisibility = (row) => {
						const op = row.querySelector('[data-role="op"]')?.value;
						const valueInput = row.querySelector('[data-role="value"]');
						if (valueInput) valueInput.style.display = (op === 'null' || op === 'notNull') ? 'none' : '';
					};

					const wireAttributeRow = async (row) => {
						await populateKeySelect(row.querySelector('[data-role="key"]'));
						updateValueVisibility(row);
						row.querySelector('[data-role="op"]').addEventListener('change', () => { updateValueVisibility(row); triggerUpdate(); });
						row.querySelector('[data-role="key"]').addEventListener('change', triggerUpdate);
						row.querySelector('[data-role="value"]').addEventListener('input', triggerUpdate);
						row.querySelector('[data-role="remove"]').addEventListener('click', () => { row.remove(); triggerUpdate(); });
					};

					const wireSortRow = async (row) => {
						await populateKeySelect(row.querySelector('[data-role="key"]'));
						row.querySelector('[data-role="key"]').addEventListener('change', triggerUpdate);
						row.querySelector('[data-role="order"]').addEventListener('change', triggerUpdate);
						row.querySelector('[data-role="remove"]').addEventListener('click', () => { row.remove(); triggerUpdate(); });
					};

					const wireNestedGroup = (groupEl) => {
						const opButtons = groupEl.querySelectorAll(':scope > div > div > [data-role^="op-"]');
						for (const btn of opButtons) {
							btn.addEventListener('click', () => {
								for (const b of opButtons) b.classList.remove('active');
								btn.classList.add('active');
								groupEl.dataset.op = btn.dataset.role.replace('op-', '');
								triggerUpdate();
							});
						}
						groupEl.querySelector('[data-role="remove"]').addEventListener('click', () => { groupEl.remove(); triggerUpdate(); });
						groupEl.querySelector('[data-type="add-attribute"]').addEventListener('click', async () => {
							const attrsDiv = groupEl.querySelector('[data-type="group-attributes"]');
							attrsDiv.insertAdjacentHTML('beforeend', _Code.queryBuilder.templates.attribute());
							await wireAttributeRow(attrsDiv.lastElementChild);
							triggerUpdate();
						});
						groupEl.querySelector('[data-type="add-group"]').addEventListener('click', () => {
							const attrsDiv = groupEl.querySelector('[data-type="group-attributes"]');
							attrsDiv.insertAdjacentHTML('beforeend', _Code.queryBuilder.templates.nestedGroup());
							wireNestedGroup(attrsDiv.lastElementChild);
							triggerUpdate();
						});
					};

					const restoreOperations = async (attrsDiv, operations) => {
						for (const op of operations) {
							if (op.type === 'operation') {
								attrsDiv.insertAdjacentHTML('beforeend', _Code.queryBuilder.templates.attribute());
								const row = attrsDiv.lastElementChild;
								await wireAttributeRow(row);
								if (op.key) row.querySelector('[data-role="key"]').value = op.key;
								if (op.op)  row.querySelector('[data-role="op"]').value  = op.op;
								updateValueVisibility(row);
								if (op.value !== undefined && op.value !== null) {
									const valueInput = row.querySelector('[data-role="value"]');
									if (valueInput) valueInput.value = op.value;
								}
							} else if (op.type === 'group') {
								attrsDiv.insertAdjacentHTML('beforeend', _Code.queryBuilder.templates.nestedGroup());
								const groupEl = attrsDiv.lastElementChild;
								wireNestedGroup(groupEl);
								if (op.op) {
									for (const b of groupEl.querySelectorAll(':scope > div > div > [data-role^="op-"]')) b.classList.remove('active');
									const activeBtn = groupEl.querySelector(`:scope > div > div > [data-role="op-${op.op}"]`);
									if (activeBtn) { activeBtn.classList.add('active'); groupEl.dataset.op = op.op; }
								}
								if (op.operations?.length) {
									await restoreOperations(groupEl.querySelector('[data-type="group-attributes"]'), op.operations);
								}
							}
						}
					};

					const loadFromQuery = async (queryJson) => {
						if (!queryJson) return;
						let query;
						try { query = JSON.parse(queryJson); } catch (e) { return; }

						if (query.queryType && dataTypeDropdown.value !== query.queryType) {
							dataTypeDropdown.value = query.queryType;
						}

						const rootGroup = queryContainer.querySelector(':scope > div[data-type="group-container"]');
						if (query.op) {
							for (const b of rootGroup.querySelectorAll('legend [data-role^="op-"]')) b.classList.remove('active');
							const activeBtn = rootGroup.querySelector(`legend [data-role="op-${query.op}"]`);
							if (activeBtn) { activeBtn.classList.add('active'); rootGroup.dataset.op = query.op; }
						}

						if (query.operations?.length) {
							await restoreOperations(rootGroup.querySelector('[data-type="group-attributes"]'), query.operations);
						}

						if (query.sort?.length) {
							const sortAttrsDiv = queryContainer.querySelector('[data-type="sort-attributes"]');
							for (const sortOp of query.sort) {
								sortAttrsDiv.insertAdjacentHTML('beforeend', _Code.queryBuilder.templates.sort());
								const row = sortAttrsDiv.lastElementChild;
								await wireSortRow(row);
								if (sortOp.key)   row.querySelector('[data-role="key"]').value   = sortOp.key;
								if (sortOp.order) row.querySelector('[data-role="order"]').value = sortOp.order;
							}
						}
					};

					// sorting
					{
						const sortContainer = queryContainer.querySelector('div[data-type="sort-container"]');
						sortContainer.querySelector('button[data-type="add-sort"]').addEventListener('click', async () => {
							const attrsDiv = sortContainer.querySelector('div[data-type="sort-attributes"]');
							attrsDiv.insertAdjacentHTML('beforeend', _Code.queryBuilder.templates.sort());
							await wireSortRow(attrsDiv.lastElementChild);
							triggerUpdate();
						});
					}

					// root group
					{
						const rootGroup = queryContainer.querySelector(':scope > div[data-type="group-container"]');
						const opButtons = rootGroup.querySelectorAll('legend [data-role^="op-"]');
						for (const btn of opButtons) {
							btn.addEventListener('click', () => {
								for (const b of opButtons) b.classList.remove('active');
								btn.classList.add('active');
								rootGroup.dataset.op = btn.dataset.role.replace('op-', '');
								triggerUpdate();
							});
						}
						rootGroup.querySelector('[data-type="add-attribute"]').addEventListener('click', async () => {
							const attrsDiv = rootGroup.querySelector('[data-type="group-attributes"]');
							attrsDiv.insertAdjacentHTML('beforeend', _Code.queryBuilder.templates.attribute());
							await wireAttributeRow(attrsDiv.lastElementChild);
							triggerUpdate();
						});
						rootGroup.querySelector('[data-type="add-group"]').addEventListener('click', () => {
							const attrsDiv = rootGroup.querySelector('[data-type="group-attributes"]');
							attrsDiv.insertAdjacentHTML('beforeend', _Code.queryBuilder.templates.nestedGroup());
							wireNestedGroup(attrsDiv.lastElementChild);
							triggerUpdate();
						});
					}

					_Helpers.activateCommentsInElement(container);

					dataTypeDropdown.addEventListener('change', async () => {
						for (const keySelect of queryContainer.querySelectorAll('[data-role="key"]')) {
							await populateKeySelect(keySelect);
						}
						triggerUpdate();
					});


				} else if (entity.type === 'ScriptDataSource') {

					_Code.codeContents.append(_Code.templates.dataSourceEditor(entity.name, entity));

					let container = _Code.codeContents[0].querySelector('#datasource-container');

					container.insertAdjacentHTML('beforeend', _Code.templates.scriptDataSourceEditor(entity));

					_Helpers.activateCommentsInElement(container);

					let config = {
						language: 'auto',
						lint: true,
						autocomplete: true,
						changeFn: (editor, entity) => {
							_Code.persistence.updateDirtyFlag(entity);
						},
						isAutoscriptEnv: true
					};

					_Editors.getMonacoEditor(entity, 'valuesScript', _Code.codeContents[0].querySelector('#values-script-editor'), config);
					_Editors.getMonacoEditor(entity, 'fieldsScript', _Code.codeContents[0].querySelector('#fields-script-editor'), config);

				} else {

					// A type that carries the DataSource trait but has no editor of its own: without this
					// branch the panel stayed empty and the name input the code below wires up did not
					// exist, so the click ended in a TypeError.
					_Code.codeContents.append(_Code.templates.dataSourceEditor(entity.name || entity.id, entity));

					let container = _Code.codeContents[0].querySelector('#datasource-container');

					container.insertAdjacentHTML('beforeend', _Code.templates.genericDataSourceEditor(entity));
				}

				let changeHandler = () => {
					_Code.persistence.updateDirtyFlag(entity);
				};

				document.querySelector('#data-source-name-input').addEventListener('keyup', changeHandler);

				_Code.persistence.runCurrentEntitySaveAction = () => {

					_Code.persistence.saveEntityAction(entity, (success) => {

					});
				};

				let saveButton = _Code.mainArea.helpers.displaySvgActionButton('#datasource-actions', _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 14, 14, 'icon-green'), 'save', 'Save', _Code.persistence.runCurrentEntitySaveAction);

				let cancelButton = _Code.mainArea.helpers.displaySvgActionButton('#datasource-actions', _Icons.getSvgIcon(_Icons.iconCrossIcon, 14, 14, 'icon-red'), 'cancel', 'Revert changes', () => {
					document.querySelector('#data-source-form').reset();
					_Code.additionalDirtyChecks = [];
					_Editors.disposeEditorModel(entity.id, 'valuesScript');
					_Editors.disposeEditorModel(entity.id, 'fieldsScript');
					_Code.mainArea.displayDataSourceContent(data);
				});

				// delete button
				_Code.mainArea.helpers.displaySvgActionButton('#datasource-actions', _Icons.getSvgIcon(_Icons.iconTrashcan, 14, 14, 'icon-red'), 'delete', 'Delete', () => {
					_Code.persistence.deleteSchemaEntity(entity, `Delete data source ${entity.name}?`, 'Delete data source?', data);
				});

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
		getOverridesForSchemaActions: (data) => (
			{

				editReadWriteFunction: async (property) => {

					let okToNavigate = !_Code.persistence.isDirty();

					if (!okToNavigate) {

						if ((await _Dialogs.confirmation.showPromise("There are unsaved changes - discard?"))) {

							okToNavigate = true;
							_Code.persistence.forceNotDirty()
						}
					}

					if (okToNavigate) {
						_Code.tree.findAndOpenNode(data.path + '/' + property.id, true);
					}
				},
				editCypherProperty: async (property) => {

					let okToNavigate = !_Code.persistence.isDirty();

					if (!okToNavigate) {

						if ((await _Dialogs.confirmation.showPromise("There are unsaved changes - discard?"))) {

							okToNavigate = true;
							_Code.persistence.forceNotDirty()
						}
					}

					if (okToNavigate) {
						_Code.tree.findAndOpenNode(data.path + '/' + property.id, true);
					}
				},
				editVectorIndex: async (property) => {

					let okToNavigate = !_Code.persistence.isDirty();

					if (!okToNavigate) {

						if ((await _Dialogs.confirmation.showPromise("There are unsaved changes - discard?"))) {

							okToNavigate = true;
							_Code.persistence.forceNotDirty()
						}
					}

					// the property details have the vector index settings
					if (okToNavigate) {
						_Code.tree.findAndOpenNode(data.path + '/' + property.id, true);
					}
				}
			}
		),
		displaySchemaNodeContent: (data, targetView = _Code.mainArea.typeTabs.tabFor(data)) => {

			let typePath = _Code.mainArea.typeTabs.typePathFor(data);

			_Code.mainArea.updateLocationStack(data, data.updateLocationStack);

			fetch(`${Structr.rootUrl}${data.id}/schema`).then(response => {

				if (response.ok) {
					return response.json();
				} else {
					throw Error("Unable to fetch schema node content");
				}

			}).then(json => {

				let entity = json.result;

				_Helpers.fastRemoveAllChildren(_Code.codeContents[0]);
				_Code.codeContents.append(_Code.templates.type({ data, type: entity }));

				let tabs        = _Code.mainArea.typeTabs.create(entity, typePath);
				let tabControls = _Schema.nodes.loadNode(entity, tabs, $('.tabs-content-container', _Code.codeContents)[0], targetView, null, _Code.mainArea.getOverridesForSchemaActions({ ...data, path: `${typePath}/properties` }));

				_Code.mainArea.typeTabs.activate(tabs);

				// remove bulk edit save/discard buttons
				for (let button of _Code.codeContents[0].querySelectorAll('.discard-all, .save-all')) {
					_Helpers.fastRemoveElement(button);
				}

				_Code.persistence.runCurrentEntitySaveAction = () => {

					_Schema.bulkDialogsGeneral.saveEntityFromTabControls(entity, tabControls).then((success) => {

						if (success) {

							// reload tree node
							_Code.tree.refreshNode(typePath);

							// and refresh center pane, on the tab that is open
							_Code.mainArea.displaySchemaNodeContent(data, _Code.mainArea.typeTabs.activeTab());
						}
					});
				};

				let saveButton = _Code.mainArea.helpers.displaySvgActionButton('#type-actions', _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, 'icon-green'), 'save', 'Save', _Code.persistence.runCurrentEntitySaveAction);

				let cancelButton = _Code.mainArea.helpers.displaySvgActionButton('#type-actions', _Icons.getSvgIcon(_Icons.iconCrossIcon, 14, 14, 'icon-red'), 'cancel', 'Revert changes', () => {
					_Schema.bulkDialogsGeneral.resetInputsViaTabControls(tabControls);
				});

				// delete button
				if (!entity.isPartOfBuiltInSchema) {
					_Code.mainArea.helpers.displaySvgActionButton('#type-actions', _Icons.getSvgIcon(_Icons.iconTrashcan, 14, 14, 'icon-red'), 'delete', 'Delete type', () => {
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
		typeTabs: {
			typeId: null,
			typePath: null,
			selectingInTree: false,
			folders: { properties: 'local', remoteproperties: 'remote', views: 'views', methods: 'methods', inheritedproperties: 'builtin' },
			element: () => document.querySelector('#code-type-tabs'),
			toolbar: () => {

				let toolbar = document.querySelector('#code-toolbar');
				if (!toolbar) {

					let button = (label) => `<button title="${label}" class="action button btn flex items-center active:border-green hidden">${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'mr-2')} ${label}</button>`;

					toolbar = _Helpers.createSingleDOMElementFromHTML(`<div id="code-toolbar" class="flex items-center">${button('Create Type')}${button('Create Service Class')}${button('Create Scratchpad')}${button('Create Script Data Source')}${button('Create Query Data Source')}
						<button title="Create Pre-Deployment Script" data-level="create-pre-deploy-conf" class="action button btn flex items-center active:border-green hidden">${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'mr-2')} Create Pre-Deployment Script</button>
						<button title="Create Post-Deployment Script" data-level="create-post-deploy-conf" class="action button btn flex items-center active:border-green hidden">${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'mr-2')} Create Post-Deployment Script</button>
						<button title="Open the diagram of this process in the Processes area" data-level="bpmnDefinitionMethods" class="button btn flex items-center hidden">${_Icons.getSvgIcon(_Icons.iconProcess, 16, 16, 'mr-2')} Open in Processes</button>
						<div id="code-create-function" class="hidden">
							<select class="select-create-type combined-select-create">
								<option value="">Function</option>
								${LifecycleMethods.getAvailableLifecycleMethods(null).map(method => `<option value="${method.name}">${method.name}</option>`).join('')}
							</select>
							<button class="action button inline-flex items-center combined-select-create">${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, 'mr-2')}<span>Create</span></button>
						</div>
					</div>`);

					toolbar.querySelector('#code-create-function button').addEventListener('click', () => {
						_Code.mainArea.createGlobalMethod(toolbar.querySelector('#code-create-function select').value);
					});

					toolbar.querySelector('button:nth-child(3)').addEventListener('click', _Code.mainArea.scratchpad.create);
					toolbar.querySelector('button:nth-child(4)').addEventListener('click', () => _Code.mainArea.createDataSource('ScriptDataSource'));
					toolbar.querySelector('button:nth-child(5)').addEventListener('click', () => _Code.mainArea.createDataSource('QueryDataSource'));

					[...toolbar.querySelectorAll('button')].slice(0, 2).forEach((el, index) => {

						let isServiceClass = (index === 1);
						let path           = (isServiceClass ? '/root/services' : '/root/custom');

						el.addEventListener('click', () => {

							_Schema.nodes.showCreateTypeDialog({ isServiceClass: isServiceClass }, (id) => {

								_Code.tree.refreshNode(path);

								// tree needs to be refreshed - implement better solution using refresh_node.jstree event
								window.setTimeout(() => _Code.tree.findAndOpenNode(`${path}/${id}`, true), 250);
							});
						});
					});

					Structr.functionBar.appendChild(toolbar);
					_Code.mainArea.typeTabs.align();
				}

				return toolbar;
			},
			showCreateButtons: (level) => {

				// each level of the tree offers what can be created there
				let offers = { custom: [0], services: [1], scratchpads: [2], datasources: [3, 4] }[level] ?? [];

				[..._Code.mainArea.typeTabs.toolbar().querySelectorAll(':scope > button')].forEach((button, index) => {
					button.classList.toggle('hidden', button.dataset.level ? (button.dataset.level !== level) : !offers.includes(index));
				});

				_Code.mainArea.typeTabs.toolbar().querySelector('#code-create-function').classList.toggle('hidden', level !== 'globals');
			},
			isFolder: (data) => Object.hasOwn(_Code.mainArea.typeTabs.folders, data.content),
			tabFor: (data) => _Code.mainArea.typeTabs.folders[data.content] ?? 'general',
			typePathFor: (data) => _Code.mainArea.typeTabs.isFolder(data) ? data.path.substring(0, data.path.lastIndexOf('/')) : data.path,
			pathFor: (tab) => {

				let folder = Object.keys(_Code.mainArea.typeTabs.folders).find(key => _Code.mainArea.typeTabs.folders[key] === tab);

				return folder ? `${_Code.mainArea.typeTabs.typePath}/${folder}` : _Code.mainArea.typeTabs.typePath;
			},
			activeTab: () => _Code.mainArea.typeTabs.element()?.querySelector('li.active')?.id.substring('tab-'.length) ?? 'general',
			create: (entity, typePath) => {

				_Code.mainArea.typeTabs.remove();

				_Code.mainArea.typeTabs.typeId   = entity.id;
				_Code.mainArea.typeTabs.typePath = typePath;

				let tabs = _Helpers.createSingleDOMElementFromHTML('<div id="code-type-tabs" class="tabs-container"><ul class="tabs-menu"></ul></div>');

				_Code.mainArea.typeTabs.toolbar().appendChild(tabs);

				return tabs;
			},
			adopt: () => {

				// the tabs of a method or a function property move from the card to the toolbar
				let list = _Code.codeContents[0].querySelector('.code-tabs.level-two > ul');
				if (list) {

					_Code.mainArea.typeTabs.remove();

					let tabs = _Helpers.createSingleDOMElementFromHTML('<div id="code-type-tabs" class="tabs-container"></div>');

					list.classList.add('tabs-menu');
					tabs.appendChild(list);
					_Code.mainArea.typeTabs.toolbar().appendChild(tabs);
				}
			},
			activate: (tabs) => {

				let list = tabs.querySelector('ul');

				// the order of the folders in the tree
				let inherited = list.querySelector('#tab-builtin');
				if (inherited) {
					list.appendChild(inherited);
				}

				for (let tab of list.querySelectorAll('li')) {

					tab.addEventListener('click', (e) => {

						if (e.isTrusted) {
							_Code.mainArea.typeTabs.selectInTree(tab.id.substring('tab-'.length));
						}
					});
				}
			},
			remove: () => {

				_Code.mainArea.typeTabs.element()?.remove();

				_Code.mainArea.typeTabs.typeId   = null;
				_Code.mainArea.typeTabs.typePath = null;
			},
			align: () => {

				let toolbar = document.querySelector('#code-toolbar');
				if (toolbar) {
					toolbar.style.left = `${_Code.codeContents[0].parentNode.getBoundingClientRect().left - Structr.functionBar.getBoundingClientRect().left}px`;
				}
			},
			showLoaded: (data) => {

				let isType = (_Code.mainArea.typeTabs.isFolder(data) || (data.key ?? data.type) === 'SchemaNode');
				let tab    = _Code.mainArea.typeTabs.element()?.querySelector(`#tab-${_Code.mainArea.typeTabs.tabFor(data)}`);

				if (!isType || !tab || data.id !== _Code.mainArea.typeTabs.typeId) {
					return false;
				}

				tab.click();
				_Code.mainArea.updateLocationStack(data, data.updateLocationStack);

				return true;
			},
			selectInTree: (tab) => {

				let path = _Code.mainArea.typeTabs.pathFor(tab);
				let tree = _Code.codeTree.jstree(true);

				if (tree.get_node(path) && !tree.is_selected(path)) {

					_Code.mainArea.typeTabs.selectingInTree = true;
					tree.deselect_all();
					tree.select_node(path);
					_Code.mainArea.typeTabs.selectingInTree = false;
				}

				_Code.pathLocations.updatePathLocationStack(path);
				_Code.lastClickedPath = path;
			}
		},
		displayPropertyDetails: (data) => {

			Command.get(data.id, null, (result) => {

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
			_Code.mainArea.typeTabs.adopt();

			let activateTab = (tabName) => {
				$('.function-property-tab-content', _Code.codeContents).hide();
				$('#code-toolbar li[data-name]').removeClass('active');

				let activeTab = $('#tabView-' + tabName, _Code.codeContents);
				activeTab.show();
				$('#code-toolbar li[data-name="' + tabName + '"]').addClass('active');

				window.setTimeout(() => { _Editors.resizeVisibleEditors(); }, 250);
			};

			for (let tabLink of document.querySelectorAll('#code-toolbar li[data-name]')) {
				tabLink.addEventListener('click', (e) => {
					e.stopPropagation();
					activateTab(tabLink.dataset.name);
				})
			}
			if (document.querySelector(`#code-toolbar li[data-name="${lastOpenTab}"]`)) {
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

			_Helpers.activateCommentsInElement(_Code.codeContents[0]);

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

			_Code.mainArea.helpers.displaySvgActionButton('#property-actions', _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, 'icon-green'), 'save', 'Save property', _Code.persistence.runCurrentEntitySaveAction);

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
				propertyUIContainer[0].querySelector('#property-unique').disabled = true;
				propertyUIContainer[0].querySelector('#property-composite').disabled = true;
				$('#property-type-hint-input').val(property.typeHint || 'null');
			}

			if (property.propertyType !== 'String' && property.propertyType !== 'StringArray') {
				_Helpers.fastRemoveElement(propertyUIContainer[0].querySelector('#property-fulltext-indexed').closest('[data-is-property-attribute-container]'));
			}

			if (property.propertyType === 'Cypher') {
				_Helpers.fastRemoveElement($('#property-format-input').parent()[0]);
			}

			let vectorIndexContainer = null;

			if (property.propertyType === 'DoubleArray') {

				// the server's default, so that an existing property without a stored similarity is not dirty
				property.vectorSimilarity = _Schema.properties.vectorIndex.similarityOf(property);

				propertyUIContainer[0].querySelector('#property-options').insertAdjacentHTML('beforeend', `
					<div id="property-vector-index" class="mb-4">
						<label class="font-semibold">Vector Index</label>
						<div class="mt-2">
							${_Schema.properties.vectorIndex.templates.form({
								vectorDimensions: property.vectorDimensions,
								vectorSimilarity: property.vectorSimilarity,
								indexName:        _Schema.properties.vectorIndex.indexName(property, property.schemaNode?.name)
							})}
						</div>
					</div>
				`);

				vectorIndexContainer = propertyUIContainer[0].querySelector('#property-vector-index');
			}

			$('select', propertyUIContainer).on('change', changeHandler);
			$('input[type=checkbox]', propertyUIContainer).on('change', changeHandler);
			$('input[type=text]', propertyUIContainer).on('keyup', changeHandler);

			if (vectorIndexContainer) {
				_Schema.properties.vectorIndex.bindForm(vectorIndexContainer, changeHandler);
			}

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

				_Code.mainArea.helpers.displaySvgActionButton('#view-actions', _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, 'icon-green'), 'save', 'Save view', _Code.persistence.runCurrentEntitySaveAction);

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

				_Helpers.fastRemoveAllChildren(_Code.codeContents[0]);
				_Code.codeContents.append(_Code.templates.method({ method: result }));
				_Code.mainArea.typeTabs.adopt();

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

					apiTab[0].querySelector('[data-property="allowUnknownParameters"]').checked = (result.allowUnknownParameters === true);

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

				_Code.mainArea.helpers.displaySvgActionButton('#method-actions', _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, 'icon-green'), 'save', 'Save method', _Code.persistence.runCurrentEntitySaveAction);

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
					$('#code-toolbar li[data-name]').removeClass('active');

					let activeTab = $(`#tabView-${tabName}`, _Code.codeContents);
					activeTab.show();
					$(`#code-toolbar li[data-name="${tabName}"]`).addClass('active');

					window.setTimeout(() => { _Editors.resizeVisibleEditors(); }, 250);
				};

				$('#code-toolbar li[data-name]').off('click').on('click', function(e) {
					e.stopPropagation();
					activateTab($(this).data('name'));
				});

				if (document.querySelector(`#code-toolbar li[data-name="${lastOpenTab}"]`)) {
					activateTab(lastOpenTab || 'source');
				} else {
					activateTab('source');
				}

				_Editors.focusEditor(sourceEditor);

				// a function that was just created opens with its name being edited
				if (_Code.mainArea.renameOnOpen === result.id) {

					_Code.mainArea.renameOnOpen = null;
					document.getElementById('method-name-container').click();
					document.getElementById('method-name-input').select();
				}
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

						let saveButton = _Code.mainArea.helpers.displaySvgActionButton('#type-actions', _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 16, 16, 'icon-green'), 'save', 'Save', _Code.persistence.runCurrentEntitySaveAction);

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
			_Code.mainArea.displayTypeOverview(data, true);
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

				// the row of Save, Revert and Delete stands at the foot of the card
				if (targetId.endsWith('-actions')) {

					targetNode.classList.add('code-actions-footer');

					if (targetNode.parentNode !== _Code.codeContents[0]) {
						_Code.codeContents[0].appendChild(targetNode);
					}
				}

				// the settings of the view's own editor stand at the right end of that row; an editor inside a tab keeps its own
				let editorInfo = _Code.codeContents[0].querySelector('.editor-info:not(.propTabContent .editor-info)');
				if (editorInfo && targetNode.classList.contains('code-actions-footer') && editorInfo.parentNode !== targetNode) {
					targetNode.appendChild(editorInfo);
				}

				return button;
			},
		},
		scratchpad: {
			path: '/scratchpads',
			nameOf: (pad) => ((pad.name && pad.name.trim().length > 0) ? pad.name : 'Untitled Scratchpad'),
			load: () => Command.queryPromise('Scratchpad', 1000, 1, 'name', 'asc', null, true, 'ui'),
			getTreeEntry: (pad, path) => {
				return {
					id:       `${path}/${pad.id}`,
					text:     _Code.mainArea.scratchpad.nameOf(pad),
					children: false,
					icon:     _Icons.nonExistentEmptyIcon,
					li_attr:  { 'data-id': pad.id },
					data: {
						svgIcon: _Icons.getSvgIcon(_Icons.iconScratchpad, 16, 24),
						key:     'Scratchpad',
						content: 'scratchpad',
						id:      pad.id,
						path:    `${path}/${pad.id}`
					},
				};
			},
			create: async () => {

				let pad = await Command.createPromise({ type: 'Scratchpad', name: 'Untitled Scratchpad' });

				_Code.tree.refreshNode(_Code.mainArea.scratchpad.path);

				// tree needs to be refreshed - implement better solution using refresh_node.jstree event
				window.setTimeout(() => _Code.tree.findAndOpenNode(`${_Code.mainArea.scratchpad.path}/${pad.id}`, true), 250);
			},
			showOverview: async (data) => {

				_Code.mainArea.updateLocationStack(data, data.updateLocationStack);

				_Code.codeContents.append(_Code.mainArea.scratchpad.templates.overview());

				let tableConfig = {
					class: 'type-overview schema-props grid',
					style: 'grid-template-columns: [ name ] minmax(0, 3fr) [ lastRun ] minmax(0, 1fr);',
					cols: [
						{ class: 'pb-2 px-2 font-bold flex', title: 'Name' },
						{ class: 'pb-2 px-2 font-bold flex', title: 'Last run' }
					],
					noButtons: true
				};

				let table    = _Helpers.createSingleDOMElementFromHTML(_Schema.templates.schemaGrid(tableConfig));
				let gridBody = table.querySelector('.schema-grid-body');
				table.querySelector('.schema-grid-footer').classList.add('hidden');

				_Code.codeContents[0].appendChild(table);

				for (let pad of await _Code.mainArea.scratchpad.load()) {

					let name = _Code.mainArea.scratchpad.nameOf(pad);
					let row  = _Helpers.createSingleDOMElementFromHTML(`
						<div class="schema-grid-row contents cursor-pointer" title="Open scratchpad">
							<div class="p-2 flex items-center gap-2">${_Icons.getSvgIcon(_Icons.iconScratchpad, 16, 24, ['flex-shrink-0'])}<b></b></div>
							<div class="p-2 flex items-center">${pad.lastRunTimestamp ? new Date(pad.lastRunTimestamp).toLocaleString() : ''}</div>
						</div>
					`);

					row.querySelector('b').textContent = name;

					row.addEventListener('click', () => {
						_Code.tree.findAndOpenNode(`${data.path}/${pad.id}`, true);
					});

					gridBody.appendChild(row);
				}
			},
			show: async (data) => {

				_Code.mainArea.updateLocationStack(data, data.updateLocationStack);

				let pad = await Command.getPromise(data.id, null, 'ui');

				_Helpers.fastRemoveAllChildren(_Code.codeContents[0]);
				_Code.codeContents.append(_Code.mainArea.scratchpad.templates.scratchpad());

				let contents    = _Code.codeContents[0];
				let nameOutput  = contents.querySelector('#scratchpad-name-output');
				let nameInput   = contents.querySelector('#scratchpad-name-input');
				let wrapOption  = contents.querySelector('#scratchpad-options');
				let wrapInput   = contents.querySelector('[data-property="wrapJsInMain"]');
				let logTextarea = contents.querySelector('[data-is-log]');
				let outputDiv   = contents.querySelector('[data-is-output]');

				// an instance without the switch does not know the property and would refuse the whole save
				let hasWrapSwitch = Object.hasOwn(pad, 'wrapJsInMain');

				let save = () => {

					let { name, source, result, log } = pad;

					Command.setProperties(pad.id, Object.assign({ name, source, result, log }, hasWrapSwitch ? { wrapJsInMain: pad.wrapJsInMain } : {}));
				};

				// name: click to rename, as for a method
				nameOutput.textContent = _Code.mainArea.scratchpad.nameOf(pad);
				nameInput.value        = pad.name ?? '';

				let commitName = () => {

					if (nameInput.classList.contains('hidden')) {
						return;
					}

					nameInput.classList.add('hidden');
					nameOutput.classList.remove('hidden');

					if (nameInput.value !== (pad.name ?? '')) {

						pad.name               = nameInput.value;
						nameOutput.textContent = _Code.mainArea.scratchpad.nameOf(pad);

						save();
						_Code.codeTree.jstree(true).rename_node(data.path, _Code.mainArea.scratchpad.nameOf(pad));
					}
				};

				nameOutput.addEventListener('click', () => {

					nameOutput.classList.add('hidden');
					nameInput.classList.remove('hidden');
					nameInput.focus();
				});

				contents.querySelector('h2 .rename-hint').addEventListener('click', () => nameOutput.click());

				nameInput.addEventListener('blur', commitName);
				nameInput.addEventListener('keyup', (e) => {

					if (e.key === 'Escape') {
						nameInput.value = pad.name ?? '';
						commitName();
					} else if (e.key === 'Enter') {
						commitName();
					}
				});

				// wrap in main(): makes "return" work
				wrapOption.classList.toggle('hidden', !hasWrapSwitch);
				wrapInput.checked = (pad.wrapJsInMain === true);
				wrapInput.addEventListener('change', () => {

					pad.wrapJsInMain = wrapInput.checked;
					save();
				});

				logTextarea.value = pad.log ?? '';
				_Code.mainArea.scratchpad.populateOutputDiv(outputDiv, pad);

				let scratchMonacoConfig = {
					language: 'auto',
					lint: true,
					autocomplete: true,
					isAutoscriptEnv: true,
					changeFn: (editor, entity) => {

						entity.source = editor.getValue();
						save();
					}
				};

				let scratchEditor = _Editors.getMonacoEditor(pad, 'source', contents.querySelector('.editor'), scratchMonacoConfig);
				_Editors.appendEditorOptionsElement(contents.querySelector('.editor-info'));

				let updateLogButton = contents.querySelector('#scratchpad-update-log');

				let updateLog = async () => {

					let response = await fetch(`${Structr.rootUrl}Scratchpad/${pad.id}/getServerLog`, { method: 'POST' });
					let logData  = await response.json();

					if (response.ok) {

						pad.log           = logData;
						logTextarea.value = logData;

						save();

					} else {

						Structr.errorFromResponse(logData);
					}
				};

				let callFinished = false;
				let updateLogUntilCallFinished = () => {

					setTimeout(async () => {
						if (callFinished === false) {
							await updateLog();
							updateLogUntilCallFinished();
						} else {
							callFinished = false;
						}
					}, 1000);
				};

				let runButton = _Code.mainArea.helpers.displaySvgActionButton('#scratchpad-actions', _Icons.getSvgIcon(_Icons.iconRunButton, 14, 14), 'run', 'Run', () => {

					_Helpers.disableElements(true, runButton, updateLogButton);

					_Helpers.fastRemoveAllChildren(outputDiv);
					logTextarea.value = '';

					fetch(`${Structr.rootUrl}Scratchpad/${pad.id}/prepareNextRun`, { method: 'POST' })
						.then(response => {

							updateLogUntilCallFinished();

							return fetch(`${Structr.rootUrl}Scratchpad/${pad.id}/run`, { method: 'POST' })
						})
						.then(response => response.json())
						.then(runData => {

							// a string result is shown as it is: JSON.stringify would wrap it in quotes and turn its newlines into a literal backslash-n
							pad.result = (runData.code === 422) ? runData.message : (typeof runData.result === 'string' ? runData.result : JSON.stringify(runData.result, null, '\t'));
							_Code.mainArea.scratchpad.populateOutputDiv(outputDiv, pad);

						}).catch(e => {

							_Code.mainArea.scratchpad.populateOutputDiv(outputDiv, pad);
							console.log(e);

						}).finally(() => {

							callFinished = true;

							updateLog();

							_Helpers.disableElements(false, runButton, updateLogButton);
						});
				});

				runButton.title = `Run (${_Helpers.isMac() ? 'Cmd' : 'Ctrl'}+Enter)`;

				_Code.mainArea.helpers.displaySvgActionButton('#scratchpad-actions', _Icons.getSvgIcon(_Icons.iconTrashcan, 14, 14, 'icon-red'), 'delete', 'Delete scratchpad', () => {

					_Dialogs.confirmation.showPromise(`Delete scratchpad <strong>${_Helpers.escapeTags(pad.name ?? '')}</strong>?`).then(async confirm => {

						if (confirm === true) {

							let response = await fetch(`${Structr.rootUrl}Scratchpad/${pad.id}`, { method: 'DELETE' });

							if (!response.ok) {

								Structr.errorFromResponse(await response.json());

							} else {

								_Code.tree.refreshNode(_Code.mainArea.scratchpad.path);
								_Code.tree.findAndOpenNode(_Code.mainArea.scratchpad.path, true);
							}
						}
					});
				});

				updateLogButton.addEventListener('click', updateLog);

				_Editors.resizeVisibleEditors();
				_Editors.focusEditor(scratchEditor);
			},
			populateOutputDiv: (div, entity) => {

				// create a child-div because of the font-mono situation... it automatically sets styles, leading to regular text being mono after changing return values
				_Helpers.fastRemoveAllChildren(div);
				let outputDiv = document.createElement('div');

				let output   = entity.result ?? '';
				let isString = (typeof output === 'string');

				if (!isString) {
					output = JSON.stringify(output, null, "\t");
				}

				outputDiv.classList.add('whitespace-pre-wrap');

				outputDiv.textContent = output;

				div.appendChild(outputDiv);
			},
			templates: {
				overview: config => `
					<div class="inline-info mb-4">
						<div class="inline-info-icon">
							${_Icons.getSvgIcon('info-icon')}
						</div>
						<div class="inline-info-text">
							<p>Scratchpads are simple blocks where custom code can be developed without needing to create a method.</p>
							<p>If log output is generated by the scratchpad code, it is tagged with <code>[scratch_xxxx]</code>. The log view is continuously updated while the script is running and only contains log entries that contain the corresponding tag.</p>
						</div>
					</div>
				`,
				scratchpad: config => `
					<h2 class="cursor-text with-icon">
						${_Icons.getSvgIcon(_Icons.iconScratchpad, 16, 24, ['flex-shrink-0'])}
						<span id="scratchpad-name-output"></span>
						${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, ['rename-hint'], 'Rename')}
						<input class="hidden font-bold text-lg" type="text" id="scratchpad-name-input" placeholder="Untitled Scratchpad" size="60" autocomplete="off">
					</h2>
					<div id="scratchpad-options" class="mb-2">
						<label class="whitespace-nowrap"><input type="checkbox" data-property="wrapJsInMain"> Wrap JavaScript in main()</label>
					</div>
					<div id="scratchpad-editor" class="flex flex-col flex-grow">
						<div class="editor flex-grow"></div>
					</div>
					<div id="scratchpad-results" class="grid grid-cols-2 gap-4">
						<div class="scratchpad-pane flex flex-col">
							<h4>Output</h4>
							<div data-is-output class="flex-grow"></div>
						</div>
						<div class="scratchpad-pane flex flex-col">
							<h4 class="flex items-center gap-2">
								Server log
								<button id="scratchpad-update-log" class="hover:bg-gray-100 focus:border-gray-666 active:border-green" title="Update log">${_Icons.getSvgIcon(_Icons.iconRefreshArrows, 14, 14)}</button>
							</h4>
							<textarea data-is-log readonly="readonly" class="flex-grow whitespace-pre"></textarea>
						</div>
					</div>
					<div class="editor-info"></div>
					<div id="scratchpad-actions" class="flex"></div>
				`,
			}
		},
		deploymentConfigurationScripts: {
			getTreeEntry: (path) => {
				return {
					id:       path + '/' + _Code.mainArea.deploymentConfigurationScripts.templates.mainTreeName,
					text:     'Deployment Configuration Scripts',
					children: true,
					icon:     _Icons.nonExistentEmptyIcon,
					li_attr:  { 'data-id': _Code.mainArea.deploymentConfigurationScripts.templates.mainTreeName },
					data: {
						svgIcon: _Icons.getSvgIcon(_Icons.iconDeploymentConfigScripts, 18, 24),
						key:     _Code.mainArea.deploymentConfigurationScripts.templates.mainTreeName,
						content: _Code.mainArea.deploymentConfigurationScripts.templates.mainTreeName,
						path:    path + '/' + _Code.mainArea.deploymentConfigurationScripts.templates.mainTreeName
					},
				}
			},
			getTreeChildForKey: (path, key) => {
				return {
					id:       path + '/' + key,
					text:     _Code.mainArea.deploymentConfigurationScripts.templates.treeNames[key],
					children: false,
					icon:     _Icons.nonExistentEmptyIcon,
					li_attr:  { 'data-id': key },
					data: {
						svgIcon: _Icons.getSvgIcon(_Icons[_Code.mainArea.deploymentConfigurationScripts.templates.icons[key]], 18, 24),
						key:     key,
						content: key,
						path:    path + '/' + key
					},
				}
			},
			getTreeChildren: (path) => {
				return [
					_Code.mainArea.deploymentConfigurationScripts.getTreeChildForKey(path, 'pre-deploy-conf'),
					_Code.mainArea.deploymentConfigurationScripts.getTreeChildForKey(path, 'post-deploy-conf')
				]
			},
			// the level above the two scripts: the scripts that exist, and a button for each that does not
			showOverview: async (data) => {

				let templates = _Code.mainArea.deploymentConfigurationScripts.templates;
				let existing  = {};

				for (let scriptType of Object.keys(templates.treeNames)) {
					existing[scriptType] = (await Command.queryPromise('ApplicationConfigurationDataNode', 1000, 1, 'name', true, { configType: scriptType }, true, null, null)).length > 0;
				}

				if ($('#code-tree').jstree(true).get_selected()[0] !== data.path) {
					return;
				}

				_Code.codeContents[0].insertAdjacentHTML('beforeend', templates.overview());

				_Code.mainArea.displayOverview(data, ['Name', 'File', 'Runs'], (addRow) => {

					for (let scriptType of Object.keys(templates.treeNames).filter(type => existing[type])) {

						addRow(scriptType, [
							`${_Icons.getSvgIcon(_Icons[templates.icons[scriptType]], 18, 24, ['flex-shrink-0'])}<b>${templates.treeNames[scriptType]}</b>`,
							templates.scriptNames[scriptType],
							(scriptType === 'pre-deploy-conf' ? 'before the import' : 'after the import')
						]);
					}
				});

				for (let scriptType of Object.keys(templates.treeNames).filter(type => !existing[type])) {

					_Code.mainArea.deploymentConfigurationScripts.offerCreation(data, scriptType, () => {
						_Code.tree.findAndOpenNode(`${data.path}/${scriptType}`, true);
					});
				}
			},
			// shows the toolbar button that creates the script, as long as the level it was asked for is still selected
			offerCreation: (data, scriptType, afterCreation) => {

				if ($('#code-tree').jstree(true).get_selected()[0] !== data.path) {
					return;
				}

				let createButton = _Code.mainArea.typeTabs.toolbar().querySelector(`button[data-level="create-${scriptType}"]`);

				createButton.classList.remove('hidden');
				createButton.onclick = () => {

					Command.createApplicationConfigurationDataNode(scriptType, scriptType, _Code.mainArea.deploymentConfigurationScripts.templates.initialScript[scriptType], () => {
						createButton.classList.add('hidden');
						afterCreation();
					});
				};
			},
			editDeploymentConfigurationScript: async (data) => {

				let scriptType = data.content;

				_Helpers.fastRemoveAllChildren(_Code.codeContents[0]);

				let scripts = await Command.queryPromise('ApplicationConfigurationDataNode', 1000, 1, 'name', true, { configType: scriptType }, true, null, null);

				_Code.codeContents.append(_Code.mainArea.deploymentConfigurationScripts.templates.main(_Code.mainArea.deploymentConfigurationScripts.templates.treeNames[scriptType], scriptType));

				if (scripts.length === 0) {

					// a script that does not exist yet is created from the toolbar
					_Code.mainArea.deploymentConfigurationScripts.offerCreation(data, scriptType, () => {
						_Code.mainArea.deploymentConfigurationScripts.editDeploymentConfigurationScript(data);
					});

				} else {

					let entity = scripts[0];
					let container = _Code.codeContents[0].querySelector('#deployment-config-script-container');

					container.insertAdjacentHTML('beforeend', _Code.mainArea.deploymentConfigurationScripts.templates.scriptEditor({ scriptType }));
					_Editors.appendEditorOptionsElement(_Code.codeContents[0].querySelector('.editor-info'));

					_Helpers.activateCommentsInElement(container);

					let config = {
						language: 'auto',
						lint: true,
						autocomplete: true,
						changeFn: (editor, entity) => {
							_Code.persistence.updateDirtyFlag(entity);
						},
						isAutoscriptEnv: true
					};

					_Editors.getMonacoEditor(entity, 'content', _Code.codeContents[0].querySelector('#deployment-config-script-editor'), config);

					_Code.persistence.runCurrentEntitySaveAction = () => {

						_Code.persistence.saveEntityAction(entity, (success) => {});
					};

					let saveButton = _Code.mainArea.helpers.displaySvgActionButton('#deploy-config-script-actions', _Icons.getSvgIcon(_Icons.iconCheckmarkBold, 14, 14, 'icon-green'), 'save', 'Save', _Code.persistence.runCurrentEntitySaveAction);

					let cancelButton = _Code.mainArea.helpers.displaySvgActionButton('#deploy-config-script-actions', _Icons.getSvgIcon(_Icons.iconCrossIcon, 14, 14, 'icon-red'), 'cancel', 'Revert changes', () => {
						_Code.additionalDirtyChecks = [];
						_Editors.disposeEditorModel(entity.id, 'content');
						_Code.mainArea.deploymentConfigurationScripts.editDeploymentConfigurationScript(data);
					});

					_Code.mainArea.helpers.displaySvgActionButton('#deploy-config-script-actions', _Icons.getSvgIcon(_Icons.iconTrashcan, 14, 14, 'icon-red'), 'delete', 'Delete', () => {
						_Code.persistence.deleteSchemaEntityAndLoadPath(entity, `Delete deployment config script?`, 'Delete deployment config script?', data.path);
					});

					_Helpers.disableElements(true, saveButton, cancelButton);

					document.querySelector('#code-contents .tabs-content-container')?.addEventListener('bulk-data-change', (e) => {

						e.stopPropagation();

						let isDirty     = _Code.persistence.isDirty();
						_Helpers.disableElements(!isDirty, saveButton, cancelButton);

						_Code.persistence.tellFirstElementToShowDirtyState(isDirty);
					});

					_Helpers.requestAnimationFrameWrapper(null, () => {
						_Editors.resizeVisibleEditors();
					});
				}
			},
			documentation: undefined,
			templates: {
				main: (title, scriptType) => `
					<h2 class="with-icon">${_Icons.getSvgIcon(_Icons[_Code.mainArea.deploymentConfigurationScripts.templates.icons[scriptType]], 18, 24, ['flex-shrink-0'])}${title}</h2>

					<div id="method-buttons">
						<div class="flex flex-wrap gap-x-4">
							<div>
								<div id="deploy-config-script-actions" class="flex items-start"></div>
							</div>
						</div>
					</div>

					<div id="deployment-config-script-container" class="flex flex-col flex-grow"></div>
					<div class="editor-info"></div>
				`,
				overview: () => `
					<div class="info-panel text-sm mt-4">
						<p class="font-semibold mb-2">Pre- and Post-Deploy Scripts</p>
						<p class="mb-2">You can include scripts that run automatically before or after import.</p>

						<p class="font-semibold mt-4 mb-2">pre-deploy.conf</p>
						<p class="mb-2">If a file named <code>pre-deploy.conf</code> is present in the application folder being imported, Structr executes it as a script before importing the data. Since it is run before the schema from the deployment has been imported, only the then-existing schema types can be used.</p>
						<p class="mb-2">Use this to create users or groups that are referenced in visibility settings of the exported files but may not exist in the target system. All operations in this script should be idempotent, meaning they can be safely run multiple times without causing unintended side effects. For example, prefer using methods like <code>get_or_create</code> rather than <code>create</code> to avoid duplicate entries.</p>
						<pre class="p-3 rounded bg-gray-100 overflow-x-auto text-xs leading-relaxed">{
	let myUserGroup = $.getOrCreate('Group', 'name', 'myUserGroup');
	let myNestedUserGroup = $.getOrCreate('Group', 'name', 'myNestedUserGroup');

	if (!$.isInGroup(myUserGroup, myNestedUserGroup)) {
		$.addToGroup(myUserGroup, myNestedUserGroup);
	}
}</pre>

						<p class="font-semibold mt-4 mb-2">post-deploy.conf</p>
						<p class="mb-2">If a file named <code>post-deploy.conf</code> is present, Structr executes it after the import completes successfully. Since this script is run after the new schema has been imported, all types from the deployed schema are available.</p>
						<p class="mb-2">Use this to ensure that required data exists in every instance of your application.</p>
						<pre class="p-3 rounded bg-gray-100 overflow-x-auto text-xs leading-relaxed">{
	let necessaryData = $.getOrCreate('TypeFromDeployedSchema', 'name', 'important');

	// ... ensure that required data exists
}</pre>
					</div>
				`,
				scriptEditor: config => `
					<div class="block mt-6 mb-2 font-bold" data-comment="${_Code.mainArea.deploymentConfigurationScripts.templates.tooltips[config.scriptType]}">${_Code.mainArea.deploymentConfigurationScripts.templates.scriptNames[config.scriptType]}</div>
					<div class="editor property-code flex-grow" id="deployment-config-script-editor" data-property="content"></div>
				`,
				mainTreeName: 'deploy-configs',
				icons: {
					'pre-deploy-conf': 'iconPreDeployConf',
					'post-deploy-conf': 'iconPostDeployConf'
				},
				treeNames: {
					'pre-deploy-conf': 'Pre-Deployment Script',
					'post-deploy-conf': 'Post-Deployment Script'
				},
				scriptNames: {
					'pre-deploy-conf': 'pre-deploy.conf',
					'post-deploy-conf': 'post-deploy.conf'
				},
				tooltips: {
					'pre-deploy-conf': 'This script is run at the start of a deployment import process',
					'post-deploy-conf': 'This script is run at the end of a deployment import process',
				},
				initialScript: {
					'pre-deploy-conf':
						'{\n' +
						'	// During the application deployment import process, this file is treated as a script and executed *before* any other actions take place.\n' +
						'	//\n' +
						'	// Important: because this script runs before the application schema is imported, it operates on the existing (current) schema.\n' +
						'	//\n' +
						'	// Its purpose is to ensure that all required users and groups are present before the application import occurs.\n' +
						'	// All operations in this script should be **idempotent** — meaning they can be safely run multiple times without causing unintended side effects.\n' +
						'	// For example, prefer using methods like \`get_or_create\` rather than \`create\` to avoid duplicate entries.\n' +
						'	//\n' +
						'	// For more information, please refer to the documentation.\n' +
						'}'
					,
					'post-deploy-conf':
						'{\n' +
						'	// During the application deployment import process, this file is treated as a script and executed *after* all other operations have finished.\n' +
						'	//\n' +
						'	// For more information, please refer to the documentation.\n' +
						'}'
				}

			}
		}
	},
	helpers: {
		getAttributesToFetchForErrorObject: () => 'id,type,name,content,isStatic,ownerDocument,schemaNode',
		getPathToOpenForSchemaObject: (obj) => {

			if (obj.type === 'SchemaNode') {

				return `/root/${obj.isServiceClass === true ? 'services' : 'custom'}/${obj.id}`;

			} else if (obj.type === 'ScriptDataSource' || obj.type === 'QueryDataSource') {

				return `/datasources/${obj.id}`;

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
					_Code.persistence.runCurrentEntitySaveAction?.();
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

				// ctrl-enter / cmd-enter runs the open scratchpad
				if (event.key === 'Enter' && ((!_Helpers.isMac() && event.ctrlKey) || (_Helpers.isMac() && event.metaKey))) {

					let runButton = document.querySelector('#scratchpad-actions #action-button-run');

					if (runButton && !_Dialogs.custom.isDialogOpen()) {

						event.preventDefault();
						event.stopPropagation();
						runButton.click();
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

						// attempt to highlight corresponding inputs
						for (let error of data.errors) {
							_Helpers.blinkRed(document.querySelectorAll(`[data-property="${error.property}"`));
						}

						Structr.errorFromResponse(data, undefined, { requiresConfirmation: true });
					}
				});
			}
		},
		deleteSchemaEntity: (entity, title, text, data) => {

			let path  = data.path;
			let parts = path.split('/');

			parts.pop();

			let parent = parts.join('/');

			_Code.persistence.deleteSchemaEntityAndLoadPath(entity, title, text, parent);
		},
		deleteSchemaEntityAndLoadPath: (entity, title, text, pathToReloadTo) => {

			_Dialogs.confirmation.showPromise(`<h3>${title}</h3><p>${(text || '')}</p>`).then((confirm) => {

				if (confirm === true) {

					_Code.dirty = false;

					Command.deleteNode(entity.id, false, () => {
						_Code.persistence.forceNotDirty();
						_Code.tree.findAndOpenNode(pathToReloadTo, false);
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
		createEntity: (type, data) => {

			return new Promise(((resolve, reject) => {

				fetch(`${Structr.rootUrl}${type}`, {
					method: 'POST',
					body: JSON.stringify(data)
				}).then(response => {

					response.json().then(responseData => {

						if (response.ok) {

							resolve(responseData);

						} else {

							reject(responseData);
						}
					});
				})
			}));
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

			_Helpers.ensureDesiredModuleIsActive(_Code).then(() => Command.getPromise(result.id)).then(node => {

				let path = _Code.search.getPathForNode(node);

				if (result.type === 'SchemaMethod' && key === 'source') {

					_Editors.highlightTextInNextEditor(searchData.searchString);
				}

				// only a search result says which tab holds the match
				if (key) {
					_Code.search.prepareTabForNodeAndKey(node, key);
				}

				_Code.tree.findAndOpenNode(path);
			});
		},
		getPathForNode: (node) => {

			if (node.type === 'SchemaMethodParameter') {

				return _Code.helpers.getPathToOpenForSchemaObject(node.schemaMethod);

			} else if (node.type === 'ApplicationConfigurationDataNode') {

				return _Code.mainArea.deploymentConfigurationScripts.getTreeChildForKey('/' + _Code.mainArea.deploymentConfigurationScripts.templates.mainTreeName, node.name).data.path;
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
	queryBuilder: {
		updateQuery: (container, queryTypeSelector, destination) => {

			const queryType        = queryTypeSelector.value;
			const rootGroupEl      = container.querySelector(':scope > div[data-type="group-container"]');
			const query = {
				type: 'group',
				op: rootGroupEl?.dataset.op ?? 'and',
				queryType: queryType,
				operations: [],
				sort: []
			};

			const groupAttrs = rootGroupEl?.querySelector('div[data-type="group-attributes"]');
			if (groupAttrs) {
				_Code.queryBuilder._collectFromGroupAttrs(groupAttrs, query.operations);
			}

			const sortAttrs = container.querySelector('div[data-type="sort-attributes"]');
			if (sortAttrs) {
				for (const row of sortAttrs.querySelectorAll(':scope > div[data-type="sort-operation"]')) {
					const key   = row.querySelector('[data-role="key"]')?.value;
					const order = row.querySelector('[data-role="order"]')?.value;
					if (key) {
						query.sort.push({ type: 'sort', key, order: order || 'asc', queryType });
					}
				}
			}

			destination.value = JSON.stringify(query, null, 2);
			destination.dispatchEvent(new CustomEvent('change'));
		},
		_collectFromGroupAttrs: (groupAttrsEl, operations) => {

			for (const item of groupAttrsEl.querySelectorAll(':scope > div[data-type]')) {

				const type = item.dataset.type;

				if (type === 'operation') {
					const key   = item.querySelector('[data-role="key"]')?.value ?? '';
					const op    = item.querySelector('[data-role="op"]')?.value ?? 'eq';
					const value = item.querySelector('[data-role="value"]')?.value ?? '';
					operations.push({ type: 'operation', key, op, value });

				} else if (type === 'group-container') {
					const nestedGroup = { type: 'group', op: item.dataset.op ?? 'and', operations: [] };
					operations.push(nestedGroup);
					const nestedAttrs = item.querySelector('div[data-type="group-attributes"]');
					if (nestedAttrs) {
						_Code.queryBuilder._collectFromGroupAttrs(nestedAttrs, nestedGroup.operations);
					}
				}
			}
		},
		templates: {
			attribute: () => `
			<div class="w-full flex items-center justify-between gap-2 mb-2" data-type="operation">
				<div class="flex w-full gap-2 items-center">
					<select class="w-full" data-role="key"><option value="">--- Select attribute ---</option></select>
					<select data-role="op">
						<optgroup label="Exact">
							<option value="eq">Equal</option>
							<option value="neq">Not Equal</option>
							<option value="gt">Greater</option>
							<option value="gteq">Greater/Equal</option>
							<option value="ls">Less</option>
							<option value="lseq">Less/Equal</option>
							<option value="null">Null</option>
							<option value="notNull">Not Null</option>
							<option value="startsWith">Starts with</option>
							<option value="endsWith">Ends with</option>
							<option value="contains">Contains</option>
						</optgroup>
						<optgroup label="Case Insensitive">
							<option value="caseInsensitiveStartsWith">Starts with (ci)</option>
							<option value="caseInsensitiveEndsWith">Ends with (ci)</option>
							<option value="caseInsensitiveContains">Contains (ci)</option>
						</optgroup>
					</select>
					<input class="w-full" type="text" data-role="value" placeholder="Value...">
				</div>
				<svg width="16" height="16" class="text-red cursor-pointer flex-shrink-0" data-role="remove"><title>Remove</title><use href="#trashcan"></use></svg>
			</div>
			`,
			sort: () => `
			<div class="w-full flex items-center justify-between gap-2 mb-2" data-type="sort-operation">
				<div class="flex w-full gap-2 items-center">
					<select class="w-full" data-role="key"><option value="">--- Select attribute ---</option></select>
					<select data-role="order">
						<option value="asc">ASC</option>
						<option value="desc">DESC</option>
					</select>
				</div>
				<svg width="16" height="16" class="text-red cursor-pointer flex-shrink-0" data-role="remove"><title>Remove</title><use href="#trashcan"></use></svg>
			</div>
			`,
			nestedGroup: () => `
			<div data-type="group-container" data-op="and" class="border border-gray-ddd rounded p-2 mb-2">
				<div class="flex items-center justify-between mb-2">
					<div class="flex gap-1">
						<button data-role="op-and" type="button" class="px-2 py-0.5 text-xs border rounded active">AND</button>
						<button data-role="op-or" type="button" class="px-2 py-0.5 text-xs border rounded">OR</button>
						<button data-role="op-not" type="button" class="px-2 py-0.5 text-xs border rounded">NOT</button>
					</div>
					<svg width="16" height="16" class="text-red cursor-pointer" data-role="remove"><title>Remove Group</title><use href="#trashcan"></use></svg>
				</div>
				<div class="w-full mb-2" data-type="group-attributes"></div>
				<div class="w-full flex gap-2">
					<button data-type="add-attribute" type="button" class="flex items-center justify-center"><svg class="mr-2 text-green" width="14" height="14"><use href="#circle_plus"></use></svg>Attribute</button>
					<button data-type="add-group" type="button" class="flex items-center justify-center"><svg class="mr-2 text-green" width="14" height="14"><use href="#circle_plus"></use></svg>Group</button>
				</div>
			</div>
			`,
			groupContainer: () => `
			<div data-type="group-container" data-op="and">
				<div class="w-full mt-4">
					<fieldset>
						<legend class="flex items-center gap-2">Predicates
							<div class="flex gap-1 ml-2">
								<button data-role="op-and" type="button" class="px-2 py-0.5 text-xs border rounded active">AND</button>
								<button data-role="op-or" type="button" class="px-2 py-0.5 text-xs border rounded">OR</button>
								<button data-role="op-not" type="button" class="px-2 py-0.5 text-xs border rounded">NOT</button>
							</div>
						</legend>
						<div class="w-full mb-4" data-type="group-attributes"></div>
						<div class="w-full flex gap-2">
							<button data-type="add-attribute" type="button" class="flex items-center justify-center"><svg class="mr-2 text-green" width="14" height="14"><use href="#circle_plus"></use></svg>Attribute</button>
							<button data-type="add-group" type="button" class="flex items-center justify-center"><svg class="mr-2 text-green" width="14" height="14"><use href="#circle_plus"></use></svg>Group</button>
						</div>
					</fieldset>
				</div>
			</div>
			`
		}
	},
	templates: {
		main: config => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/schema.css">
			<link rel="stylesheet" type="text/css" media="screen" href="css/code.css">

			<div class="tree-main" id="code-main">

				<div class="column-resizer-blocker"></div>
				<div class="column-resizer column-resizer-left"></div>

				<div class="tree-container" id="code-tree-container">
					<div class="tree" id="code-tree">

					</div>
				</div>

				<div class="tree-contents-container" id="code-contents-container">
					<div class="flex flex-col tree-contents" id="code-contents"></div>
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
			<button id="action-button-${config.suffix}" class="action-button flex items-center justify-center hover:bg-gray-100 focus:border-gray-666 active:border-green gap-2">
				<div class="action-button-icon flex items-center">
					${config.iconSvg ?? ''}
				</div>
				<div>${config.name}</div>
			</button>
		`,
		booleanProperty: config => `
			<h2 class="with-icon">${_Icons.getIconForSchemaNodeType(config.property)}${config.property.schemaNode.name}.${config.property.name}</h2>
			<div id="property-buttons"></div>
		`,
		builtin: config => `
			<h2>System Types</h2>
		`,
		dataSourceEditor: (title, entity) => `
			<div class="">
				<h2 class="with-icon">${_Icons.getSvgIcon((entity.type === 'QueryDataSource') ? _Icons.iconFilterFunnel : _Icons.iconScriptWrapped, 16, 24, ['flex-shrink-0'])}${title}</h2>
				<div id="method-buttons">
					<div class="flex flex-wrap gap-x-4">
						<div class="">
							<div id="datasource-actions" class="flex items-start"></div>
						</div>
					</div>
				</div>
			</div>
				
			<div id="datasource-container"></div>
		`,
		cypherProperty: config => `
			<h2 class="with-icon">${_Icons.getIconForSchemaNodeType(config.property)}${config.property.schemaNode.name}.${config.property.name}</h2>
			<div id="property-buttons"></div>
			<div class="flex flex-col flex-grow">
				<div id="cypher-code-container" class="mb-4 flex flex-col h-full">
					<h4 data-comment="The id and type of the current object are passed as parameters into the query to be used as <code>$id</code> and <code>$type</code>">Cypher Query</h4>
					<div class="editor flex-grow" data-property="format"></div>
					<div class="editor-info"></div>
				</div>
			</div>
		`,
		defaultProperty: config => `
			<h2 class="with-icon">${_Icons.getIconForSchemaNodeType(config.property)}${config.property.schemaNode.name}.${config.property.name}</h2>
			<div id="property-buttons"></div>
		`,
		defaultView: config => `
			<h2 class="with-icon">${_Icons.getIconForSchemaNodeType(config.view)}${config.view.schemaNode.name}.${config.view.name}</h2>
			<div id="view-buttons"></div>
		`,
		functionProperty: config => `
			<h2 class="with-icon">${_Icons.getIconForSchemaNodeType(config.property)}${config.property.schemaNode.name}.${config.property.name}</h2>
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
								<h4 class="py-2 font-semibold" data-comment="To retrieve the parameter passed to the write function, use &lt;code&gt;$.value;&lt;/code&gt; in a JavaScript context or the keyword &lt;code&gt;value&lt;/code&gt; in a StructrScript context.">
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
		method: config => `
			<h2 class="cursor-text with-icon">
				${_Icons.getIconForSchemaNodeType(config.method)}
				<span id="method-name-container">
					<span>${(config.method.schemaNode ? config.method.schemaNode.name + '.' : '')}</span><span id="method-name-output">${config.method.name}</span><span>()</span>
					${_Icons.getSvgIcon(_Icons.iconPencilEdit, 16, 16, ['rename-hint'], 'Rename')}
				</span>
				<input class="hidden font-bold text-lg " type="text" id="method-name-input" data-property="name" size="60" value="${config.method.name}" autocomplete="off">
			</h2>
			<div id="method-buttons" class="mb-2">
				<div id="method-options" class="flex flex-wrap gap-x-4">
					<div id="method-actions" class="flex items-start mr-4"></div>
					${_Schema.methods.templates.methodFlags(Object.assign({ cssClasses: 'method-flags-row' }, config))}
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

				<label class="flex mt-2" data-comment="If this flag is set, the method also accepts parameters that are not defined here. They are passed on unconverted and can be read with <code>$.args</code> or <code>retrieve()</code>.">
					<input type="checkbox" data-property="allowUnknownParameters"> Allow unknown parameters
				</label>

				<div class="unknown-parameters-warning warning mt-2 p-2">
					Because parameters are defined, the method only accepts these parameters. A REST or script call with any other parameter is rejected and the method is not executed. Event action mappings drop the other parameters and log a warning.
				</div>

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
		property: config => `
			<h2>${config.property.schemaNode.name}.${config.property.name}</h2>
		`,
		propertyRemote: config => `
			<h2 class="with-icon">${_Icons.getIconForSchemaNodeType(config.entity)}(:${config.sourceNode.name})-[:${config.entity.relationshipType}]-&gt;(:${config.targetNode.name})</h2>
			<div id="type-actions" class="flex"></div>

			<div class="tabs-container code-tabs">
				<ul></ul>
			</div>
			<div class="tabs-content-container flex-grow"></div>
		`,
		propertyOptions: config => `
			<div id="property-options">

				<div id="default-buttons" class="mb-4">
					<div id="property-actions" class="flex items-start"></div>
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
		root: config => `
			<div id="all-types" style="position: relative; width: 100%; height: 98%;">
			</div>
		`,
		stringProperty: config => `
			<h2 class="with-icon">${_Icons.getIconForSchemaNodeType(config.property)}${config.property.schemaNode.name}.${config.property.name}</h2>
			<div id="property-buttons"></div>
		`,
		type: config => `
			<div class="mb-2">
				<div id="type-actions" class="flex"></div>
			</div>
			<div class="tabs-content-container flex-grow"></div>
		`,
		viewOptions: config => `
			<div id="view-options">

				<div class="mb-4">
					<div id="view-actions" class="flex"></div>
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
		swaggerui: config => `
			<div id="swagger-ui-container" class="flex-grow">
				<iframe class="border-0" src="${config.iframeSrc}" width="100%" height="100%"></iframe>
			</div>
		`,
		genericDataSourceEditor: config => `
			<div class="@container">
				<div class="grid grid-cols-2 gap-8">
					<div class="@lg:col-span-1 col-span-2">
						<label class="block mt-8 mb-2 font-bold" data-comment="The name of the data source">Name</label>
						<input id="data-source-name-input" data-property="name" class="w-full box-border" placeholder="Data Source Name..." value="${config.name || ''}">
						<p class="mt-8">This is a data source of type <b>${config.type}</b>, which has no editor of its own. Its values and fields are supplied by the type itself.</p>
					</div>
				</div>
			</div>
		`,
		scriptDataSourceEditor: config => `
			<div class="@container">
				<div class="grid grid-cols-2 gap-8">
					<div class="@lg:col-span-1 col-span-2">
						<label class="block mt-8 mb-2 font-bold" data-comment="The name of the data source">Name</label>
						<input id="data-source-name-input" data-property="name" class="w-full box-border" placeholder="Data Source Name..." value="${config.name || ''}">
					</div>
					<div class="col-span-2 data-source-scripts">
						<div>
							<label class="block mb-2 font-bold" data-comment="This script is evaluated to obtain the values in this data source.">Values Script</label>
							<div class="editor w-full h-80" id="values-script-editor" data-property="valuesScript"></div>
						</div>
						<div>
							<label class="block mb-2 font-bold" data-comment="This script is evaluated to obtain information about the fields in this data source.">Fields Script</label>
							<div class="editor w-full h-80" id="fields-script-editor" data-property="fieldsScript"></div>
						</div>
					</div>
				</div>
			</div>
		`,
		queryDataSourceEditor: config => `
			<div class="@container">
				<div class="grid grid-cols-2 gap-8">
					<div class="@lg:col-span-1 col-span-2">
						<label class="block mt-8 mb-2 font-bold" data-comment="The name of the data source">Name</label>
						<input id="data-source-name-input" data-property="name" class="w-full box-border" placeholder="Data Source Name..." value="${config.name || ''}">
						<label class="block mt-8 mb-2 font-bold" data-comment="Use the buttons below to add predicates to your query">Query</label>
						<div id="query-builder-container" class="bg-gray-f8 border border-gray-ddd rounded p-4">
							<div>
								<fieldset>
									<legend>Type</legend>
									<select id="data-source-query-type-input" data-property="dataType" class="w-full">
										<option>--- Select type ---</option>
									</select>
								</fieldset>
							</div>
							${_Code.queryBuilder.templates.groupContainer()}
							<div data-type="sort-container">
								<div class="w-full mt-4">
									<fieldset>
										<legend>Sorting</legend>
										<div class="w-full mb-4" data-type="sort-attributes"></div>
										<button data-type="add-sort" type="button" class="flex items-center justify-center"><svg class="mr-2 text-green" width="14" height="14"><use href="#circle_plus"></use></svg>Attribute</button>
									</fieldset>
								</div>
							</div>
						</div>
						<input type="hidden" id="data-source-query-input" data-property="query">
					</div>
				</div>
			</div>
		`
	}
};