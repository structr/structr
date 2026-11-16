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
	Structr.registerModule(_AppMap);
});

/**
 * The application map: the whole application as one zoomable map, built from the /structr/rest/_appmap endpoint
 * (ApplicationMap on the server, see AppMapModel for what it delivers) and shown by AppMapView, which lays it out without
 * blocking the browser: a remembered layout opens at once, a new one settles visibly.
 */
let _AppMap = {
	_moduleName: 'appmap',
	view: undefined,

	settingsKey: 'structrAppMapSettings_' + location.port,

	// what the map shows: the application, or its data in the form of the map (see loadData)
	mode: 'app',

	// whether the user has seen the introduction; until then it opens with the map
	introKey: 'structrAppMapIntroSeen_' + location.port,

	// the introduction for a first visit: what the map shows and how it is used
	intro: {

		// the shapes of the elements, each in the colour the light look gives its kind (see AppMapView.LOOKS); as SVG paths
		// in a box of 20 by 20
		shapes: [
			['type',      'Type',             'M10 3a7 7 0 1 1 0 14a7 7 0 1 1 0-14z'],
			['page',      'Page',             'M5 2h7l3 3v13h-10z'],
			['component', 'Shared Component', 'M5 2h7l3 3v13h-10z'],
			['method',    'Method',           'M2.5 5.5h15a1.5 1.5 0 0 1 1.5 1.5v6a1.5 1.5 0 0 1-1.5 1.5h-15a1.5 1.5 0 0 1-1.5-1.5v-6a1.5 1.5 0 0 1 1.5-1.5z'],
			['api',       'REST Endpoint',    'M10 2.5l8 13.5h-16z'],
			['group',     'Group',            'M10 2l7 4v8l-7 4l-7-4v-8z'],
			['external',  'External Service', 'M10 1.5l2 5.6l5.9 0.2l-4.7 3.6l1.7 5.7l-4.9-3.4l-4.9 3.4l1.7-5.7l-4.7-3.6l5.9-0.2z']
		],

		read: [
			['Sectors',  'Each sector is an area of Structr.'],
			['Bold',     'Hotspots and key pages.'],
			['Tags',     'Mark the <span class="appmap-intro-tag appmap-intro-start">start</span> page and <span class="appmap-intro-tag appmap-intro-error">404</span> error pages.'],
			['Badges',   'Who sees a page: <span class="appmap-intro-dot" style="--dot: var(--access-pub)"></span>public, <span class="appmap-intro-dot" style="--dot: var(--access-auth)"></span>signed in, <span class="appmap-intro-dot" style="--dot: var(--access-none)"></span>admins.']
		],

		use: [
			[['Hover'],        'Shows the connections.'],
			[['Click'],        'Holds an element to explore it, or edits its code and settings.'],
			[['Double-click'], 'Opens a page, method, type or property.'],
			[['Esc'],          'Ends editing or exploring.']
		]
	},

	// the colours the map gives start and error tags and access badges, in the light look, for the introduction to show
	introColours: () => {

		const routing = AppMapRenderer.ROUTING_COLOR;
		const access  = AppMapRenderer.ACCESS_COLOR;

		return `--start: ${routing.start[0]}; --error: ${routing.error[0]}; --access-pub: ${access.pub[0]}; --access-auth: ${access.auth[0]}; --access-none: ${access.none[0]}`;
	},

	onload: async () => {

		Structr.setMainContainerHTML(_AppMap.templates.main());
		Structr.setFunctionBarHTML(_AppMap.templates.functions());

		// the menu is blocked while a module opens; the map can take a while to load or fail to, and leaving it in the
		// meantime is safe (see load)
		Structr.mainMenu.unblock(100);

		document.addEventListener('keydown', _AppMap.typeToSearch);

		await _AppMap.show(_AppMap.mode);
	},
	// shows the application or its data: a map of its own each, the controls that belong to it in the bar
	show: async mode => {

		_AppMap.view?.destroy();
		_AppMap.view = undefined;
		_AppMap.mode = mode;

		document.querySelector('#appmap-canvas')?.replaceChildren();
		Structr.setFunctionBarHTML(_AppMap.templates.functions());

		let bar = Structr.functionBar;

		bar.classList.toggle('appmap-data-mode', mode === 'data');

		for (let button of bar.querySelectorAll('[data-mode]')) {

			button.setAttribute('aria-pressed', String(button.dataset.mode === mode));
			button.addEventListener('click', () => {

				if (button.dataset.mode !== _AppMap.mode && !(_AppMap.isDirty() && !confirm('Discard unsaved changes?'))) {
					_AppMap.show(button.dataset.mode);
				}
			});
		}

		if (mode === 'data') {
			await _AppMap.loadData();
		} else {
			await _AppMap.load();
		}
	},
	/**
	 * The data of the application in the form of the map (see DataMap on the server): the types in families, each family
	 * a sector; the objects of a type in clusters, each an element as large as it has objects; the relationships between
	 * the objects of two clusters as one relation. Its layout is not kept, it is laid out anew every time.
	 */
	loadData: async () => {

		let status = document.querySelector('#appmap-status');

		if (status) {
			status.textContent = 'Reading and grouping all objects…';
		}

		let response = await fetch(Structr.rootUrl + '_appmap/data');

		if (!response.ok) {

			new ErrorMessage().text(`Unable to load the data map (${response.status})`).show();

			return;
		}

		let result    = (await response.json()).result;
		let data      = Array.isArray(result) ? result[0] : result;
		let container = document.querySelector('#appmap-canvas');

		// the module or the mode may have been left while the data was loading
		if (!container || _AppMap.mode !== 'data') {
			return;
		}

		// the families in turn take the hues of the aspects of the design system, the other types grey
		let hues    = _AppMap.dataHues;
		let areas   = {};
		let areaHue = {};

		(data.areas ?? []).forEach((area, i) => {

			areas[area.key]   = { label: area.label };
			areaHue[area.key] = area.key === 'other' ? 40 : hues[i % hues.length];
		});

		for (let n of data.nodes ?? []) {
			n.colour = n.area === 'other' ? '#8a8678' : d3.hsl(areaHue[n.area], 0.62, 0.5).formatHex();
		}

		_AppMap.view = new AppMapView(container, new AppMapModel({ nodes: data.nodes ?? [], edges: data.edges ?? [], groups: [], sections: [], sites: [] }), {
			settings: { look: _AppMap.schemeLook() ?? LSWrapper.getItem(_AppMap.settingsKey, {}).look },
			areas,
			areaHue,
			sizeBy: 'objects',
			radius: _AppMap.dataRadius,
			onHint: text => {

				let hint = document.querySelector('#appmap-hint');

				if (hint) {
					hint.textContent = text;
				}
			},
			onStatus: text => {

				if (status) {
					status.textContent = text;
				}
			}
		});

		let relations = d3.sum(data.edges ?? [], e => e[2]);

		document.querySelector('#appmap-counts').textContent = `${(data.objects ?? 0).toLocaleString()} objects · ${(data.nodes ?? []).length.toLocaleString()} clusters · ${relations.toLocaleString()} relationships`;

		_AppMap.initLook();
	},
	// the hues of the families on the data map, and the radius of its smallest and largest cluster
	dataHues:   [30, 85, 244, 15, 211, 160, 300],
	dataRadius: [5, 70],
	// typing on the map searches: the first character opens the global search with it in its field, which marks what it
	// finds on the map (see onGlobalSearch). Not with a space, not while a field, an editor or a dialog has the keys, and
	// not with a modifier, which belongs to a shortcut
	typeToSearch: e => {

		if (e.key.length !== 1 || e.key.trim() === '' || e.ctrlKey || e.metaKey || e.altKey || e.defaultPrevented || e.isComposing) {
			return;
		}

		// Monaco takes its keys in Chromium through an EditContext on a plain div, neither a textarea nor contenteditable
		let target = e.target;

		if (target.closest?.('input, textarea, select, [contenteditable=""], [contenteditable="true"], dialog, [popover], .monaco-editor')) {
			return;
		}

		if (Structr.globalSearch.open?.(e.key)) {
			e.preventDefault();
		}
	},
	unload: () => {

		// unsaved changes in the editors keep the map open, unless they are discarded
		if (_AppMap.isDirty() && !confirm('Discard unsaved changes?')) {
			return false;
		}

		document.removeEventListener('keydown', _AppMap.typeToSearch);

		_AppMap.view?.destroy();
		_AppMap.view = undefined;

		Structr.functionBar?.classList.remove('appmap-data-mode');
	},
	beforeunloadHandler: () => {

		if (_AppMap.isDirty()) {
			return 'There are unsaved changes - discard changes?';
		}
	},

	// the global search finds on the map too: its results are marked there, and a click on one goes there (see
	// Structr.globalSearch.notifyModule)
	isDirty: () => {
		return !!(_AppMap.view?.editor?.isDirty() || _AppMap.view?.domEditor?.isDirty());
	},
	onGlobalSearch: search => {
		_AppMap.view?.search(search);
	},
	goToGlobalSearchResult: result => {
		return _AppMap.view?.goToResult(result.id) ?? false;
	},
	load: async () => {

		let response = await fetch(Structr.rootUrl + '_appmap');

		if (!response.ok) {

			new ErrorMessage().text(`Unable to load the application map (${response.status})`).show();

			return;
		}

		let json      = await response.json();
		let container = document.querySelector('#appmap-canvas');

		// the module may have been left while the map was loading
		if (!container) {
			return;
		}

		_AppMap.view = new AppMapView(container, new AppMapModel(json.result), {
			// only the look can be chosen; what else an earlier version remembered no longer applies
			settings: { look: _AppMap.schemeLook() ?? LSWrapper.getItem(_AppMap.settingsKey, {}).look },
			storage: {
				get:    key => LSWrapper.getItem(key),
				set:    (key, value) => LSWrapper.setItem(key, value),
				remove: keys => LSWrapper.removeItems([].concat(keys)),
				keys:   () => LSWrapper.keys()
			},
			onHint: text => {

				let hint = document.querySelector('#appmap-hint');

				if (hint) {
					hint.textContent = text;
				}
			},
			// where objects that are no elements of the map lie on it, see ApplicationMap.locate
			locate: async ids => {

				let response = await fetch(Structr.rootUrl + '_appmap/locate', { method: 'POST', body: JSON.stringify({ ids }) });

				if (!response.ok) {
					return {};
				}

				// a single result may come wrapped in a list
				let result = (await response.json()).result;

				return (Array.isArray(result) ? result[0] : result) ?? {};
			},
			// the current use per element, see ApplicationMap.toActivityObject
			loadActivity: async () => {

				let response = await fetch(Structr.rootUrl + '_appmap/activity');

				if (!response.ok) {
					return null;
				}

				// a single result may come wrapped in a list
				let result = (await response.json()).result;

				return (Array.isArray(result) ? result[0] : result)?.nodes ?? null;
			},
			editor: _AppMap.codeEditor(container),
			domEditor: _AppMap.elementEditor(container),
			loadContent: id => _AppMap.loadContent(id),
			// the text of a template, or the source of a method with what its card shows of it: whether it is static or private,
			// the HTTP verb it answers, whether it returns its raw result, whether it is in the OpenAPI description, its summary
			// and whether it belongs to a type
			loadSource: async n => {

				let response = await fetch(Structr.rootUrl + (n.tpl ? 'Template/' + n.tpl : 'SchemaMethod/' + n.id));
				let result   = response.ok ? (await response.json()).result : null;

				if (!result) {
					return null;
				}

				if (n.tpl) {
					return result.content ?? '';
				}

				return {
					source: result.source ?? '',
					meta:   {
						isStatic:         !!result.isStatic,
						isPrivate:        !!result.isPrivate,
						httpVerb:         result.httpVerb ?? null,
						returnRawResult:  !!result.returnRawResult,
						includeInOpenAPI: !!result.includeInOpenAPI,
						summary:          result.summary ?? null,
						onType:           !!(result.schemaNode || result.staticSchemaNodeName)
					}
				};
			},
			openIn: open => _AppMap.openIn(open),
			toggle: change => _AppMap.toggle(change),
			// how often the grants decided a request, or the pages were viewed, in the last days, as { days, usage } (see
			// UsageStatistics); null where it cannot be read
			loadUsage: async kind => {

				let response = await fetch(`${Structr.rootUrl}stats?usage=${kind}`);

				if (!response.ok) {
					return null;
				}

				// a single result may come wrapped in a list
				let result = (await response.json()).result;

				return (Array.isArray(result) ? result[0] : result) ?? null;
			},
			// the schema relationships that start or end at the given types, for the boxes of their relationship properties, with
			// what the relationship dialog in the Code area shows of them
			loadRelationships: async ids => {

				let size  = Structr.getRequestParameterName('pageSize') + '=1000';
				let props = ['id', 'relationshipType', 'sourceNode', 'targetNode', 'sourceMultiplicity', 'targetMultiplicity', 'sourceJsonName', 'targetJsonName', 'cascadingDeleteFlag', 'autocreationFlag', 'permissionPropagation', 'readPropagation', 'writePropagation', 'deletePropagation', 'accessControlPropagation', 'propertyMask'];
				let query = async side => {

					let lists = await Promise.all(ids.map(async id => {

						let response = await fetch(`${Structr.rootUrl}SchemaRelationshipNode?${side}=${id}&${size}`, {
							headers: { Accept: `application/json; charset=utf-8; properties=${props.join(',')}` }
						});

						return response.ok ? ((await response.json()).result ?? []) : [];
					}));

					return lists.flat();
				};

				let all = [...await query('sourceNode'), ...await query('targetNode')];

				return [...new Map(all.map(rel => [rel.id, rel])).values()];
			},
			// the code of the function properties of a type, for the boxes of its fields, as a list of { name, readFunction,
			// writeFunction }
			loadFunctions: async n => {

				let size     = Structr.getRequestParameterName('pageSize') + '=1000';
				let response = await fetch(`${Structr.rootUrl}SchemaProperty?schemaNode=${n.id}&propertyType=Function&${size}`, {
					headers: { Accept: 'application/json; charset=utf-8; properties=name,readFunction,writeFunction' }
				});

				return response.ok ? ((await response.json()).result ?? []) : [];
			},
			// the resource access permissions, for the cards of the REST endpoints
			loadGrants: async () => {

				let size = Structr.getRequestParameterName('pageSize') + '=10000';

				// with whom the permissions are visible to, the users and groups that have rights on them included, as a
				// permission only applies to those who can see it (see AppMapPanels.accessOf)
				let response = await fetch(Structr.rootUrl + `ResourceAccess?${size}`, {
					headers: { Accept: 'application/json; charset=utf-8; properties=id,flags,signature,visibleToPublicUsers,visibleToAuthenticatedUsers,grantees' }
				});

				if (response.ok) {
					return (await response.json()).result;
				}

				console.warn(`Application map: ResourceAccess answered ${response.status}`);

				return null;
			},
			// the members of a group, users and groups, for its card; a group that cannot be read has none after a while
			loadMembers: n => new Promise(resolve => {

				Command.get(n.id, 'id,name,members', group => resolve(group?.members ?? []));
				window.setTimeout(() => resolve([]), 5000);
			}),
			loadType: async n => {

				let response = await fetch(Structr.rootUrl + '_schema/' + encodeURIComponent(n.n));
				let result   = response.ok ? (await response.json()).result : null;

				// a single result may come wrapped in a list
				return (Array.isArray(result) ? result[0] : result) ?? null;
			},
			onStatus: text => {

				let status = document.querySelector('#appmap-status');

				if (status) {
					status.textContent = text;
				}
			}
		});

		_AppMap.initControls();

		// the scripting errors of HTML elements, which the map marks on them (see AppMapPanels.problemsOf); the Pages area
		// keeps them, and is missing in the preview
		_AppMap.loadProblems();

		// a search made before the map opened
		if (Structr.globalSearch.current) {
			_AppMap.view.search(Structr.globalSearch.current);
		}
	},
	// leaves the map for what an open card stands for, see AppMapView.placeOpenButton: in the Code area a type, a method (of
	// a type, or a user-defined function) or a property of a type, a relationship as the remote property it is there, what
	// cannot be found as its type; in the Security area the resource access permissions or the users and groups, filtered
	// by the name of what the card stands for
	// switches a setting of an open box on the map (see AppMapView.flip and flipAccess) in the database: a setting of a field
	// on its schema property, whether a user is an admin or blocked on the user, after asking, and the flags and the visibility of a resource access permission on it, after asking where the
	// change opens more than was clicked; says whether it was changed, and why not where it was not
	toggle: async ({ kind, key, label, value, values, question, owner, name, id, name: principal }) => {

		let first = async url => {

			let response = await fetch(Structr.rootUrl + url);
			let result   = response.ok ? (await response.json()).result : null;

			return Array.isArray(result) ? result[0] : result;
		};

		let target = null;

		if (kind === 'property') {

			target = await first(`SchemaProperty?schemaNode=${owner.id}&name=${encodeURIComponent(name)}`);

		} else if (kind === 'principal') {

			let question = `${value ? 'Make' : 'No longer make'} ${principal} ${key === 'isAdmin' ? 'an admin' : 'blocked'}?`;

			if (!(await _Dialogs.confirmation.showPromise(question, false))) {
				return false;
			}

			target = { id };

		} else if (kind === 'access') {

			if (question && !(await _Dialogs.confirmation.showPromise(question, false))) {
				return false;
			}

			target = { id };
		}

		if (!target?.id) {

			new ErrorMessage().text(`${label} could not be changed: the property was not found`).show();

			return false;
		}

		let response = await fetch(Structr.rootUrl + target.id, { method: 'PUT', body: JSON.stringify(values ?? { [key]: value }) });

		if (!response.ok) {

			let message = (await response.json().catch(() => null))?.message ?? response.status;

			new ErrorMessage().text(`${label} could not be changed: ${message}`).show();

			return false;
		}

		return true;
	},

	openIn: async open => {

		if (open.area === 'security') {

			// the filter goes into the stored state of the list before the area opens: the area reads it when it builds the
			// list, shows it in the filter field and loads the list with it, however long that takes (see _Pager.initPager)
			let id        = open.tab === 'resource-access' ? _Security.resourceAccessesPagerId : (open.group ? _Security.groupsPagerId : _Security.usersPagerId);
			let attribute = open.tab === 'resource-access' ? 'signature' : 'name';
			let type      = open.tab === 'resource-access' ? 'ResourceAccess' : (open.group ? 'Group' : 'User');

			_Pager.restorePagerData(id);

			_Pager.pagerFilters[id] = Object.assign({}, _Pager.pagerFilters[id], { [attribute]: open.filter || null });
			_Pager.page[id]         = 1;

			_Pager.storePagerData(id, type, 1, _Pager.pageSize[id] ?? 25, _Pager.sortKey[id] ?? attribute, _Pager.sortOrder[id] ?? 'asc', _Pager.pagerFilters[id]);

			// the area opens the tab it showed last (see _Security.onload); an address with a tab in it opens no area at all,
			// the handler of the address leaves those to the area that is open (see the hashchange listener in init.js)
			LSWrapper.setItem(_Security.securityTabKey, open.tab);

			window.location.href = '#security';

			// a group opens with its properties, at the tab the card stands for (see _UsersAndGroups.typePermissions)
			if (open.dialog && open.id) {

				await _Helpers.waitForElement('#groups-list');

				let response = await fetch(Structr.rootUrl + open.id);
				let group    = response.ok ? (await response.json()).result : null;

				if (group) {
					_Entities.showProperties(group, open.dialog);
				}
			}

			return;
		}

		if (typeof _Code === 'undefined') {
			return;
		}

		let list = async url => {

			let response = await fetch(Structr.rootUrl + url);
			let result   = response.ok ? (await response.json()).result : null;

			return Array.isArray(result) ? result : (result ? [result] : []);
		};

		// a service class has its own folder in the tree; only the schema of the type says whether it is one
		let base = async type => {

			let schema = (await list(`_schema/${encodeURIComponent(type.n)}`))[0];

			return `/root/${schema?.isServiceClass ? 'services' : 'custom'}/${type.id}`;
		};

		let path = null;

		if (open.kind === 'type') {

			path = await base(open.node);

		} else if (open.kind === 'method') {

			path = open.owner ? `${await base(open.owner)}/methods/${open.node.id}` : `/globals/${open.node.id}`;

		} else {

			let owner    = open.node;
			let name     = open.name;
			let type     = await base(owner);
			let property = (await list(`SchemaProperty?schemaNode=${owner.id}&name=${encodeURIComponent(name)}`))[0];

			if (property) {

				path = `${type}/properties/${property.id}`;

			} else {

				// a relationship is named on the type it leaves from by its target name, on the other by its source name
				let relationship = (await list(`SchemaRelationshipNode?targetJsonName=${encodeURIComponent(name)}`)).find(r => r.sourceId === owner.id)
					?? (await list(`SchemaRelationshipNode?sourceJsonName=${encodeURIComponent(name)}`)).find(r => r.targetId === owner.id);

				path = relationship ? `${type}/remoteproperties/${relationship.id}` : type;
			}
		}

		// as _Code.helpers.navigateToSchemaObjectFromAnywhere: the tree needs a moment once the Code area has opened
		let timeout = window.location.hash === '#code' ? 100 : 1000;

		window.location.href = '#code';
		window.setTimeout(() => _Code.tree.findAndOpenNode(path, false), timeout);
	},
	// the scripting errors: the Pages area keeps those of HTML elements and has the call traces loaded with them (see
	// _Pages.problems.reloadRuntimeErrors); without it the call traces are loaded here
	loadProblems: () => {

		if (typeof _Pages !== 'undefined') {
			_Pages.problems.reloadRuntimeErrors();
		} else {
			_AppMap.loadTraces();
		}
	},
	// the scripting errors with the call traces that led to them, which the map marks on the elements they failed in, see
	// CallTraces and AppMapView.setTraces; the data map has none
	loadTraces: async () => {

		if (!_AppMap.view || _AppMap.mode !== 'app') {
			return;
		}

		let response = await fetch(Structr.rootUrl + '_appmap/traces');

		if (!response.ok) {
			return;
		}

		// a single result may come wrapped in a list
		let result = (await response.json()).result;

		_AppMap.view?.setTraces((Array.isArray(result) ? result[0] : result)?.traces ?? []);
	},
	// the data of the map loaded anew in place, after a change to the application, see AppMapView.replaceModel
	reload: async () => {

		// the data map is not reloaded after a change to the application
		if (_AppMap.mode !== 'app') {
			return;
		}

		let response = await fetch(Structr.rootUrl + '_appmap');

		if (!response.ok || !_AppMap.view) {
			return;
		}

		let json = await response.json();

		_AppMap.view?.replaceModel(new AppMapModel(json.result));
		_AppMap.showCounts();

		// saving an element acknowledges its scripting errors
		_AppMap.loadProblems();

		if (Structr.globalSearch.current) {
			_AppMap.view?.search(Structr.globalSearch.current);
		}
	},

	// the code of a method or the text of a template, edited above its field on the map (see AppMapView.edit) in the editor
	// of the Code area; Ctrl+S (Cmd+S on a Mac) saves, and the map loads its data anew, as a change of code may change relations
	codeEditor: container => {

		let box      = null;
		let editor   = null;
		let entity   = null;
		let property = null;
		let saved    = '';
		let size     = null;
		let run      = 0;

		let isDirty = () => !!editor && editor.getValue() !== saved;
		let state   = text => {

			let element = box?.querySelector('.appmap-editor-state');

			if (element) {
				element.textContent = text ?? (isDirty() ? `Unsaved changes · ${AppMapView.SAVE_KEY} saves` : `${AppMapView.SAVE_KEY} saves · Esc closes`);
				element.classList.toggle('appmap-editor-dirty', !text && isDirty());
			}
		};

		let dispose = () => {

			run++;

			if (editor) {
				_Editors.disposeEditor(entity.id, property);
			}

			box?.remove();

			box    = null;
			editor = null;
			entity = null;
			size   = null;
		};

		let save = async () => {

			if (!isDirty()) {
				return;
			}

			let current  = run;
			let target   = entity;
			let value    = editor.getValue();
			let response = await fetch(Structr.rootUrl + target.id, { method: 'PUT', body: JSON.stringify({ [property]: value }) });

			if (!response.ok) {

				let data = await response.json().catch(() => ({ message: `${response.status} ${response.statusText}` }));

				Structr.errorFromResponse(data, undefined, { requiresConfirmation: true });

				return;
			}

			target[property] = value;

			// the editor may have been closed or opened elsewhere meanwhile; the map loads anew all the same
			if (current === run) {
				saved = value;
			}

			state();
			_AppMap.reload();
		};

		return {
			isDirty,
			dispose,

			open: async n => {

				dispose();

				let current = run;

				box           = document.createElement('div');
				box.className = 'appmap-editor';
				box.hidden    = true;
				box.innerHTML = '<div class="appmap-editor-code"></div><div class="appmap-editor-bar"><span class="appmap-editor-state"></span></div>';

				container.appendChild(box);
				state('Loading …');

				// a method with its type, as the Code area reads it, a template with its content type
				let response = await fetch(Structr.rootUrl + (n.tpl ? n.tpl + '/ui' : n.id + '/schema'));
				let result   = response.ok ? (await response.json()).result : null;

				// another element was opened meanwhile, or editing ended
				if (current !== run) {
					return;
				}

				entity   = Array.isArray(result) ? result[0] : result;
				property = n.tpl ? 'content' : 'source';

				if (!entity) {

					state('The code could not be read.');

					return;
				}

				saved  = entity[property] ?? '';
				editor = _Editors.getMonacoEditor(entity, property, box.querySelector('.appmap-editor-code'), {
					value:                  saved,
					language:               n.tpl ? (entity.contentType || 'text/html') : 'auto',
					lint:                   !n.tpl,
					autocomplete:           true,
					isAutoscriptEnv:        !n.tpl,
					forceAllowAutoComplete: !!n.tpl,
					preventRestoreModel:    true,
					changeFn:               () => state(),
					saveFn:                 () => save()
				});

				// the overview at the right edge and the folding marks would only take from the room of the field
				editor.updateOptions({ minimap: { enabled: false }, folding: false, glyphMargin: false, lineNumbersMinChars: 3, lineDecorationsWidth: 4 });

				state();
				_AppMap.view?.requestDraw();

				editor.focus();
			},

			// on the field of the code, which fills the screen while the editor is open (see AppMapView.followCode), with the
			// text in the same size in every element
			place: rect => {

				if (!box) {
					return;
				}

				box.hidden = false;

				Object.assign(box.style, { left: `${rect.left}px`, top: `${rect.top}px`, width: `${rect.width}px`, height: `${rect.height}px` });

				let fontSize = Math.round(AppMapView.EDITOR_FONT * 0.92);
				let key      = `${Math.round(rect.width)}x${Math.round(rect.height)}`;

				if (editor && key !== size) {

					size = key;

					editor.updateOptions({ fontSize, lineHeight: Math.round(fontSize * 1.4) });
					editor.layout();
				}
			},

			hide: () => {

				if (box) {
					box.hidden = true;
				}
			},

			// says whether editing ended; unsaved changes end only when they are discarded
			close: async () => {

				if (isDirty() && !(await _Dialogs.confirmation.showPromise('There are unsaved changes - discard?', false))) {
					return false;
				}

				dispose();

				return true;
			}
		};
	},
	// everything the controls of an open HTML element show (see AppMapView.followElement): where a repeater takes its data
	// from, its query and its data key, the show and hide conditions, the texts the element holds (its text nodes, or
	// itself for a template) and for each action mapping its event and action; with the element itself
	// an element of a page as the view ui gives it, or null where it cannot be read
	readElement: async id => {

		let response = await fetch(Structr.rootUrl + id + '/ui');
		let result   = response.ok ? (await response.json()).result : null;

		return Array.isArray(result) ? result[0] : result;
	},

	// the texts of an element: the content of a template, else those of its text nodes, the text of a script or a style
	// marked as code; nested objects may come with their id only
	textsOf: async element => {

		let texts = [];

		if (typeof element.content === 'string') {

			texts.push({ id: element.id, content: element.content, code: true, entity: element, contentType: element.contentType || 'text/html' });

		} else {

			for (let nested of element.children ?? []) {

				let child = 'content' in nested || 'tag' in nested ? nested : await _AppMap.readElement(nested.id);

				// the text of a script or a style is code, edited as the content of a template is
				if (child && typeof child.content === 'string' && child.type !== 'Comment') {

					let code = AppMapPanels.SCRIPTS[element.tag] && !texts.some(t => t.code);

					texts.push({ id: child.id, content: child.content, code, entity: child, contentType: child.contentType || AppMapPanels.SCRIPTS[element.tag] });
				}
			}
		}

		return texts;
	},

	// the texts of an element, for its box on the map, read when it opens there
	loadContent: async id => {

		let element = await _AppMap.readElement(id);

		return element ? (await _AppMap.textsOf(element)).map(({ content, code }) => ({ content, code: !!code })) : null;
	},

	loadElement: async id => {

		let read    = _AppMap.readElement;
		let element = await read(id);

		if (!element) {
			return null;
		}

		// nested objects may come with their id only
		let actions = [];

		for (let nested of element.triggeredActions ?? []) {

			let action = 'event' in nested ? nested : await read(nested.id);

			if (action) {
				actions.push({ id: action.id, event: action.event ?? '', action: action.action ?? '', method: action.method ?? '' });
			}
		}

		let texts = await _AppMap.textsOf(element);

		let flow   = element.flow?.name ?? element.flow ?? null;
		let source = flow ? 'flow' : (element.cypherQuery && !element.functionQuery ? 'cypherQuery' : 'functionQuery');

		return {
			element,
			repeater: { source, query: source === 'flow' ? '' : (element[source] ?? ''), flow, key: element.dataKey ?? '' },
			actions,
			texts,
			show: element.showConditions ?? '',
			hide: element.hideConditions ?? ''
		};
	},

	// the controls of an HTML element opened on the map, laid out for AppMapPanels.ELEMENT_SIZE and scaled with the map:
	// the main field on the left (the query of a repeater, the text of the element, the content of a template in the
	// editor of the Code area), the settings on the right (data key, conditions, action mappings). Ctrl+S (Cmd+S on a Mac)
	// or Save saves, and the map loads its data anew, as a change may change relations; Esc discards unsaved changes
	elementEditor: container => {

		let box    = null;
		let data   = null;
		let saved  = null;
		let monaco = null;
		let ready  = false;
		let size   = null;
		let run    = 0;

		let actions = [
			['none', 'No action'], ['create', 'Create object'], ['update', 'Update object'], ['delete', 'Delete object'],
			['method', 'Execute method'], ['flow', 'Execute flow'], ['control-process', 'Control process'],
			['next-page', 'Next page'], ['prev-page', 'Previous page'],
			['sign-in', 'Sign in'], ['sign-out', 'Sign out'], ['sign-up', 'Sign up'], ['reset-password', 'Reset password']
		];

		// the events the Pages area offers, if it is there
		let events = () => (typeof _Pages !== 'undefined' && _Pages.getEventActionMappingAvailableEvents?.()) || ['none', 'click', 'change', 'input', 'focusout', 'keydown', 'keyup', 'submit', 'load'];
		let escape = value => _Helpers.escapeForHtmlAttributes(value ?? '');

		// the values of the form, as they would be saved
		let values = () => {

			if (!box || !data) {
				return null;
			}

			return {
				repeater: data.isRepeater ? {
					source: box.querySelector('.appmap-el-tabs [aria-pressed="true"]')?.dataset.source ?? null,
					query:  box.querySelector('.appmap-el-query')?.value ?? null,
					key:    box.querySelector('.appmap-el-key').value.trim()
				} : null,
				texts: data.texts.map((t, i) => ({ id: t.id, content: t.code && monaco ? monaco.getValue() : box.querySelector(`.appmap-el-text[data-index="${i}"]`)?.value ?? t.content })),
				show: box.querySelector('.appmap-el-show').value,
				hide: box.querySelector('.appmap-el-hide').value,
				actions: [...box.querySelectorAll('.appmap-el-action')].map(row => ({
					id:     row.dataset.id,
					event:  row.querySelector('.appmap-el-event').value,
					action: row.querySelector('.appmap-el-verb').value,
					method: row.querySelector('.appmap-el-method').value.trim()
				}))
			};
		};

		let isDirty = () => !!saved && JSON.stringify(values()) !== saved;

		let state = text => {

			let element = box?.querySelector('.appmap-el-state');

			if (element) {
				element.textContent = text ?? (isDirty() ? `Unsaved changes · ${AppMapView.SAVE_KEY} saves · Esc discards` : `${AppMapView.SAVE_KEY} saves`);
				element.classList.toggle('appmap-editor-dirty', !text && isDirty());
			}

			box?.querySelector('.appmap-el-save')?.toggleAttribute('disabled', !isDirty());
		};

		let dispose = () => {

			run++;

			if (monaco) {
				_Editors.disposeEditor(data.texts.find(t => t.code).entity.id, 'content');
			}

			box?.remove();

			box    = null;
			data   = null;
			saved  = null;
			monaco = null;
			ready  = false;
			size   = null;
		};

		let options = (list, value) => {

			// a value the lists do not know, written by hand in the Pages area, stays choosable
			let all = list.some(([key]) => key === value) || !value ? list : [[value, value], ...list];

			return all.map(([key, label]) => `<option value="${escape(key)}"${key === value ? ' selected' : ''}>${escape(label)}</option>`).join('');
		};

		let form = () => {

			let heading = text => `<div class="appmap-el-heading">${text}</div>`;

			let main = [];
			let side = [];
			let r    = data.repeater;

			// what is wrong with the element comes first, as of the map's data; saving loads the map anew
			let { errors, warnings } = data.problems;

			if (errors.length || warnings.length) {

				let item = (text, kind) => `<li class="appmap-el-${kind}">${_Helpers.escapeTags(text)}</li>`;

				side.push(`<ul class="appmap-el-problems">${errors.map(e => item(e, 'error')).join('')}${warnings.map(w => item(w, 'warning')).join('')}</ul>`);
			}

			if (data.isRepeater) {

				let tabs = AppMapPanels.SOURCES.filter(([key]) => key !== 'flow' || r.source === 'flow');

				main.push(`
					${heading('Data source')}
					<div class="appmap-el-tabs" role="group" aria-label="Source">
						${tabs.map(([key, label]) => `<button type="button" data-source="${key}" aria-pressed="${key === r.source}"${key === 'flow' || r.source === 'flow' ? ' disabled' : ''}>${label}</button>`).join('')}
					</div>
					${r.source === 'flow'
						? `<div class="appmap-el-note">Flow ${escape(r.flow)}, changed in the Pages area</div>`
						: `<textarea class="appmap-el-query appmap-el-field" spellcheck="false" placeholder="No query">${escape(r.query)}</textarea>`}
				`);

				side.push(`<label class="appmap-el-row"><span>Key</span><input class="appmap-el-key appmap-el-mono" spellcheck="false" value="${escape(r.key)}"></label>`);
			}

			// a sheet holds nothing but its code; elsewhere a heading says which field holds the content of the element
			if (data.texts.length && !data.isSheet) {
				main.push(heading(data.texts.length > 1 ? 'Contents' : 'Content'));
			}

			data.texts.forEach((t, i) => {
				main.push(t.code ? '<div class="appmap-el-code appmap-el-field"></div>' : `<textarea class="appmap-el-text appmap-el-field" data-index="${i}" spellcheck="false" placeholder="No text">${escape(t.content)}</textarea>`);
			});

			side.push(`
				<label class="appmap-el-row"><span>Show if</span><input class="appmap-el-show appmap-el-mono" spellcheck="false" placeholder="always" value="${escape(data.show)}"></label>
				<label class="appmap-el-row"><span>Hide if</span><input class="appmap-el-hide appmap-el-mono" spellcheck="false" placeholder="never" value="${escape(data.hide)}"></label>
			`);

			for (let a of data.actions) {

				side.push(`
					<div class="appmap-el-action" data-id="${a.id}">
						<select class="appmap-el-event" aria-label="Event">${options(events().map(e => [e, e]), a.event)}</select>
						<span class="appmap-el-arrow">→</span>
						<select class="appmap-el-verb" aria-label="Action">${options(actions, a.action)}</select>
						<input class="appmap-el-method appmap-el-mono" spellcheck="false" placeholder="Method name" aria-label="Method" value="${escape(a.method)}"${a.action === 'method' ? '' : ' hidden'}>
					</div>
				`);
			}

			box.classList.toggle('appmap-el-single', !main.length);
			// a template stands upright: its settings go below its content
			box.classList.toggle('appmap-el-sheet', !!data.isSheet && main.length > 0);
			box.innerHTML = `
				${main.length ? `<div class="appmap-el-main">${main.join('')}</div>` : ''}
				<div class="appmap-el-side">${side.join('')}</div>
				<div class="appmap-el-bar"><span class="appmap-el-state"></span><span class="appmap-el-buttons"><button type="button" class="appmap-el-pages">Open in Pages Area</button><button type="button" class="appmap-el-save" disabled>Save</button></span></div>
			`;

			for (let button of box.querySelectorAll('.appmap-el-tabs button')) {

				button.addEventListener('click', () => {

					box.querySelectorAll('.appmap-el-tabs button').forEach(b => b.setAttribute('aria-pressed', String(b === button)));
					state();
				});
			}

			for (let row of box.querySelectorAll('.appmap-el-action')) {

				row.querySelector('.appmap-el-verb').addEventListener('change', e => {
					row.querySelector('.appmap-el-method').hidden = e.target.value !== 'method';
				});
			}

			// the content of a template, a script or a style in the editor of the Code area, where there is one
			let template = data.texts.find(t => t.code);
			let field    = box.querySelector('.appmap-el-code');

			if (template && field) {

				if (typeof _Editors !== 'undefined') {

					monaco = _Editors.getMonacoEditor(template.entity, 'content', field, {
						value:                  template.content,
						language:               template.contentType,
						lint:                   false,
						autocomplete:           true,
						forceAllowAutoComplete: true,
						preventRestoreModel:    true,
						changeFn:               () => state(),
						saveFn:                 () => save()
					});

					// the overview of the code at the right edge, the folding marks and the wide gutter would only take from the
					// little room the element has
					monaco.updateOptions({ minimap: { enabled: false }, folding: false, glyphMargin: false, lineNumbersMinChars: 3, lineDecorationsWidth: 4 });

				} else {

					field.outerHTML = `<textarea class="appmap-el-text appmap-el-field" data-index="${data.texts.indexOf(template)}" spellcheck="false">${escape(template.content)}</textarea>`;
				}
			}

			box.addEventListener('input', () => state());
			box.addEventListener('change', () => state());
			box.querySelector('.appmap-el-save').addEventListener('click', () => save());

			// the element in the tree of its page, selected, with the tab of what it does in front, as the search of the Pages
			// area opens it (see _Pages.search.goTo); leaving the map asks first if there are unsaved changes
			box.querySelector('.appmap-el-pages').addEventListener('click', () => {

				let tab = data.isRepeater ? 'repeater' : (data.actions.length > 0 ? 'events' : null);

				if (tab) {
					_Pages.saveActiveCenterTab(data.element.id, '#pages:' + tab);
				}

				_Pages.selectAndShowArbitraryDOMElement(data.element.id);
			});
			box.querySelector('.appmap-el-pages').hidden = typeof _Pages === 'undefined';

			saved = JSON.stringify(values());
		};

		let put = async (id, body) => {

			let response = await fetch(Structr.rootUrl + id, { method: 'PUT', body: JSON.stringify(body) });

			if (!response.ok) {

				let error = await response.json().catch(() => ({ message: `${response.status} ${response.statusText}` }));

				Structr.errorFromResponse(error, undefined, { requiresConfirmation: true });
			}

			return response.ok;
		};

		let save = async () => {

			if (!isDirty()) {
				return;
			}

			let current = run;
			let id      = data.element.id;
			let now     = values();
			let before  = JSON.parse(saved);
			let body    = {};
			let ok      = true;

			state('Saving …');

			// the query goes to the property of its source, the other one is emptied, as in the Pages area; a flow is kept
			if (now.repeater && JSON.stringify(now.repeater) !== JSON.stringify(before.repeater)) {

				body.dataKey = now.repeater.key || null;

				if (now.repeater.source !== 'flow') {

					body.functionQuery = now.repeater.source === 'functionQuery' ? now.repeater.query : null;
					body.cypherQuery   = now.repeater.source === 'cypherQuery' ? now.repeater.query : null;
				}
			}

			if (now.show !== before.show || now.hide !== before.hide) {

				body.showConditions = now.show || null;
				body.hideConditions = now.hide || null;
			}

			if (Object.keys(body).length) {
				ok = await put(id, body);
			}

			for (let text of now.texts) {

				if (ok && text.content !== before.texts.find(t => t.id === text.id)?.content) {
					ok = await put(text.id, { content: text.content });
				}
			}

			for (let action of now.actions) {

				let was = before.actions.find(a => a.id === action.id);

				if (ok && JSON.stringify(action) !== JSON.stringify(was)) {
					ok = await put(action.id, action.action === 'method' ? { event: action.event, action: action.action, method: action.method || null } : { event: action.event, action: action.action });
				}
			}

			if (current !== run) {
				return;
			}

			// what failed stays in the form, to be corrected; what was saved, the map shows after loading anew
			if (ok) {

				saved = JSON.stringify(now);
				state();
				_AppMap.reload();

			} else {

				state();
			}
		};

		return {
			isDirty,
			dispose,
			ready: () => ready,

			open: async d => {

				dispose();

				let current = run;

				box           = document.createElement('div');
				box.className = 'appmap-el';
				box.hidden    = true;

				container.appendChild(box);

				box.addEventListener('keydown', e => {

					if ((e.ctrlKey || e.metaKey) && e.key === 's') {

						e.preventDefault();
						save();
					}
				});

				// the wheel over the controls zooms the map, as everywhere on it, unless a field scrolls
				box.addEventListener('wheel', e => {

					let field = e.target.closest('textarea, .appmap-el-side, .appmap-el-code');

					if (!field || field.scrollHeight <= field.clientHeight) {
						_AppMap.view?.canvas.dispatchEvent(new WheelEvent('wheel', e));
						e.preventDefault();
					}

				}, { passive: false });

				let loaded = await _AppMap.loadElement(d.data.id);

				// another element opened meanwhile, or the element closed
				if (current !== run) {
					return;
				}

				if (!loaded) {

					box.innerHTML = '<div class="appmap-el-note">The element could not be read.</div>';
					ready = true;
					_AppMap.view?.requestDraw();

					return;
				}

				data = { ...loaded, isRepeater: d.data.repeater, isSheet: AppMapPanels.isSheet(d), problems: AppMapPanels.problemsOf(d) };

				form();
				state();

				ready = true;
				_AppMap.view?.requestDraw();
			},

			// in the room of the element on the map, its text in the size the map shows it in, in the colour of the element
			place: (rect, fit, dark, colour, opacity = 1) => {

				if (!box) {
					return;
				}

				box.hidden        = !ready;
				box.style.opacity = String(opacity);
				box.classList.toggle('appmap-el-dark', !!dark);
				box.style.setProperty('--page', colour);

				// on a sheet, which has its controls only while it fills the screen, the text has the same size in every one;
				// the controls of other elements grow with the zoom
				let font = data?.isSheet ? AppMapView.EDITOR_FONT : 13 * fit;

				Object.assign(box.style, { left: `${rect.left}px`, top: `${rect.top}px`, width: `${rect.width}px`, height: `${rect.height}px`, fontSize: `${font}px` });

				let key = `${Math.round(rect.width)}x${Math.round(rect.height)}`;

				if (monaco && key !== size) {

					size = key;

					// the code in the size of the text of the field it stands in, see .appmap-el-field; Monaco takes a line height
					// below 8 as a multiple of the font size, so it never goes below that; the scroll bars shrink with the text
					let code = Math.max(4, Math.round(font * 0.92 * 10) / 10);
					let bar  = Math.max(4, Math.round(code * 0.8));

					monaco.updateOptions({ fontSize: code, lineHeight: Math.max(8, Math.round(code * 1.4)), scrollbar: { verticalScrollbarSize: bar, horizontalScrollbarSize: bar } });
					monaco.layout();
				}
			},

			hide: () => {

				if (box) {
					box.hidden = true;
				}
			},

			// says whether editing ended; unsaved changes end only when they are discarded
			close: async () => {

				if (isDirty() && !(await _Dialogs.confirmation.showPromise('There are unsaved changes - discard?', false))) {
					return false;
				}

				dispose();

				return true;
			}
		};
	},
	showCounts: () => {

		let counts = _AppMap.view.counts;

		document.querySelector('#appmap-counts').textContent = `${counts.elements.toLocaleString()} elements · ${counts.dom.toLocaleString()} HTML elements · ${counts.relations.toLocaleString()} relations`;
	},
	saveSettings: () => {
		LSWrapper.setItem(_AppMap.settingsKey, _AppMap.view.settings);
	},
	// in the new look the map follows the colour scheme of the UI; the classic look keeps the map's own choice
	schemeLook: () => _Theme.isNext() ? (_Theme.isDark() ? 'dark' : 'light') : undefined,
	followColorScheme: () => {

		let look = _AppMap.schemeLook();

		if (look && _AppMap.view) {
			_AppMap.view.setLook(look);
		}
	},
	// the look, light or dark, the same on both maps
	initLook: () => {

		let view        = _AppMap.view;
		let lookButtons = Structr.functionBar.querySelectorAll('[data-look]');
		let press       = active => lookButtons.forEach(b => b.setAttribute('aria-pressed', String(b === active)));

		press([...lookButtons].find(b => b.dataset.look === view.settings.look));

		for (let button of lookButtons) {

			button.addEventListener('click', () => {

				view.setLook(button.dataset.look);
				press(button);
				_AppMap.saveSettings();
			});
		}
	},
	initControls: () => {

		let view = _AppMap.view;
		let bar  = Structr.functionBar;

		_AppMap.showCounts();
		_AppMap.initLook();

		// the access lens: the perspective of an admin (everything), of public users, of authenticated users or of one of the groups
		let lens  = bar.querySelector('#appmap-lens');
		let roles = [
			{ key: '',       name: 'Admin (everything)' },
			{ key: 'public', name: 'Public users', kind: 'public' },
			{ key: 'auth',   name: 'Authenticated users', kind: 'auth' },
			...view.model.groups.slice().sort((a, b) => a.n.localeCompare(b.n)).map(g => ({ key: 'group:' + g.id, name: g.n, kind: 'group', id: g.id }))
		];

		let groups = document.createElement('optgroup');

		groups.label = 'Groups';

		for (let role of roles) {
			(role.kind === 'group' ? groups : lens).appendChild(new Option(role.name, role.key));
		}

		if (groups.children.length) {
			lens.appendChild(groups);
		}

		lens.addEventListener('change', () => {

			let role = roles.find(r => r.key === lens.value);

			view.setLens(role?.kind ? role : null);
		});

		// usage and hotspots: a click keeps the overlay on, and the button lit, until the next click; while the pointer rests on
		// a button its overlay shows for a glance. Usage is the glow of how much the entry points are used, hotspots the map in
		// grey around the most used elements. One at a time: switching one on switches the other off
		let overlays = { usage: bar.querySelector('#appmap-activity'), spotlight: bar.querySelector('#appmap-spotlight') };
		let pinned   = null;
		let peek     = null;
		let shown    = null;

		let showOverlay = () => {

			let overlay = peek ?? pinned;

			if (overlay !== shown) {

				shown = overlay;

				// the hotspots end the usage they borrowed, so they go first and come last
				if (overlay !== 'spotlight') {
					view.setSpotlight(false);
				}

				view.setActivity(overlay === 'usage' ? 'usage' : null);

				if (overlay === 'spotlight') {
					view.setSpotlight(true);
				}
			}

			for (let [name, button] of Object.entries(overlays)) {
				button.setAttribute('aria-pressed', String(pinned === name));
			}
		};

		for (let [name, button] of Object.entries(overlays)) {

			button.addEventListener('pointerenter', () => { peek = name; showOverlay(); });
			button.addEventListener('pointerleave', () => { peek = null; showOverlay(); });

			// switching off ends the glance too, so the click shows at once
			button.addEventListener('click', () => {

				pinned = (pinned === name ? null : name);
				peek   = (pinned ? peek : null);

				showOverlay();
			});
		}

		showOverlay();

		// the introduction: open on a first visit, the question mark brings it back
		let intro     = document.querySelector('#appmap-intro');
		let introOpen = bar.querySelector('#appmap-intro-toggle');
		let showIntro = show => {

			intro.classList.toggle('hidden', !show);
			introOpen.setAttribute('aria-pressed', String(show));
		};

		introOpen.addEventListener('click', () => showIntro(intro.classList.contains('hidden')));
		for (const close of intro.querySelectorAll('[data-intro-close]')) {

			close.addEventListener('click', () => {

				showIntro(false);
				LSWrapper.setItem(_AppMap.introKey, true);
			});
		}

		showIntro(!LSWrapper.getItem(_AppMap.introKey, false));
	},

	templates: {
		main: () => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/appmap.css">

			<div class="main-app-box" id="appmap">
				<div id="appmap-canvas"></div>
				<div id="appmap-intro" class="appmap-panel appmap-intro hidden" role="dialog" aria-labelledby="appmap-intro-title" style="${_AppMap.introColours()}">
					<header>
						<h3 id="appmap-intro-title">How to read the map</h3>
						<button class="appmap-intro-x" data-intro-close aria-label="Close">×</button>
					</header>
					<ul class="appmap-intro-shapes">
						${_AppMap.intro.shapes.map(([kind, label, path]) => `<li><svg viewBox="0 0 20 20" aria-hidden="true"><path d="${path}" style="--kind: ${AppMapView.LOOKS.light.kinds[kind]}"/></svg>${label}</li>`).join('')}
					</ul>
					<div class="appmap-intro-columns">
						<section>
							<h4>Reading</h4>
							<dl>
								${_AppMap.intro.read.map(([term, text]) => `<dt>${term}</dt><dd>${text}</dd>`).join('')}
							</dl>
						</section>
						<section>
							<h4>Using</h4>
							<dl>
								${_AppMap.intro.use.map(([keys, text]) => `<dt>${keys.map(key => `<kbd>${key}</kbd>`).join('')}</dt><dd>${text}</dd>`).join('')}
							</dl>
						</section>
					</div>
					<footer>
						<span><kbd>?</kbd> brings this back.</span>
						<button id="appmap-intro-close" data-intro-close class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Got it</button>
					</footer>
				</div>
				<div id="appmap-hud">
					<span id="appmap-hint"></span>
				</div>
				<div id="appmap-info">
					<span id="appmap-counts"></span>
					<span id="appmap-status"></span>
				</div>
			</div>
		`,
		functions: () => `
			<div class="flex items-center gap-4 flex-grow">
				<span class="appmap-segment" role="group" aria-label="Map">
					<button data-mode="app" title="The application: its pages, functions, data model and security">Application</button>
					<button data-mode="data" title="Its data: the types in families, the objects in clusters, the relationships between them">Data</button>
				</span>
				<span class="appmap-segment" role="group" aria-label="Look">
					<button data-look="light">Light</button>
					<button data-look="dark">Dark</button>
				</span>
				<label class="appmap-lens appmap-app-only">Perspective of
					<select id="appmap-lens" title="Shows the map from the perspective of public users, authenticated users or a group: which pages they see and what they may read, write or delete. Marks in red where a page does something they may not"></select>
				</label>
				<span class="appmap-segment appmap-app-only" role="group" aria-label="Activity">
					<button id="appmap-activity" aria-pressed="false" title="Shows how often pages, endpoints and types were requested and scheduled methods ran since the start. Hover for a glance, click to keep it on">Usage</button>
				</span>
				<span class="appmap-segment appmap-app-only" role="group" aria-label="Hotspots">
					<button id="appmap-spotlight" aria-pressed="false" title="Greys out the map and shows only the most used pages, endpoints, types and scheduled methods, with their number of requests or runs. Hover for a glance, click to keep it on">Hotspots</button>
				</span>
				<span class="appmap-segment appmap-app-only" role="group" aria-label="Help">
					<button id="appmap-intro-toggle" aria-pressed="false" title="How to read and use the map">?</button>
				</span>
			</div>
		`
	}
};

_Theme.onChange(() => _AppMap.followColorScheme());
