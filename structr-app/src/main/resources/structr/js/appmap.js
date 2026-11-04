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
			[['Click'],        'Holds an element to explore it, or edits its code.'],
			[['Double-click'], 'Opens a page or method.'],
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

		await _AppMap.load();
	},
	unload: () => {

		// unsaved changes in the editor keep the map open, unless they are discarded
		if (_AppMap.view?.editor?.isDirty() && !confirm('Discard unsaved changes?')) {
			return false;
		}

		_AppMap.view?.destroy();
		_AppMap.view = undefined;
	},
	beforeunloadHandler: () => {

		if (_AppMap.view?.editor?.isDirty()) {
			return 'There are unsaved changes - discard changes?';
		}
	},

	// the global search finds on the map too: its results are marked there, and a click on one goes there (see
	// Structr.globalSearch.notifyModule)
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
			settings: { look: LSWrapper.getItem(_AppMap.settingsKey, {}).look },
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
			loadSource: async n => {

				let response = await fetch(Structr.rootUrl + (n.tpl ? 'Template/' + n.tpl : 'SchemaMethod/' + n.id));
				let result   = response.ok ? (await response.json()).result : null;

				return result ? ((n.tpl ? result.content : result.source) ?? '') : null;
			},
			onStatus: text => {

				let status = document.querySelector('#appmap-status');

				if (status) {
					status.textContent = text;
				}
			}
		});

		_AppMap.initControls();

		// a search made before the map opened
		if (Structr.globalSearch.current) {
			_AppMap.view.search(Structr.globalSearch.current);
		}
	},
	// the data of the map loaded anew in place, after a change to the application, see AppMapView.replaceModel
	reload: async () => {

		let response = await fetch(Structr.rootUrl + '_appmap');

		if (!response.ok || !_AppMap.view) {
			return;
		}

		let json = await response.json();

		_AppMap.view?.replaceModel(new AppMapModel(json.result));
		_AppMap.showCounts();

		if (Structr.globalSearch.current) {
			_AppMap.view?.search(Structr.globalSearch.current);
		}
	},

	// the code of a method or the text of a template, edited above its field on the map (see AppMapView.edit) in the editor
	// of the Code area; Ctrl+S saves, and the map loads its data anew, as a change of code may change relations
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
				element.textContent = text ?? (isDirty() ? 'Unsaved changes · Ctrl+S saves' : 'Ctrl+S saves · Esc closes');
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

				state();
				_AppMap.view?.requestDraw();
				editor.focus();
			},

			// on the field of the code, in the size of the code on the map, but never too small to read or too large to see
			// enough of it
			place: (rect, font) => {

				if (!box) {
					return;
				}

				box.hidden = false;

				Object.assign(box.style, { left: `${rect.left}px`, top: `${rect.top}px`, width: `${rect.width}px`, height: `${rect.height}px` });

				let fontSize = Math.round(Math.min(18, Math.max(10, font)));
				let key      = `${Math.round(rect.width)}x${Math.round(rect.height)}@${fontSize}`;

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
	showCounts: () => {

		let counts = _AppMap.view.counts;

		document.querySelector('#appmap-counts').textContent = `${counts.elements.toLocaleString()} elements · ${counts.dom.toLocaleString()} HTML elements · ${counts.relations.toLocaleString()} relations`;
	},
	saveSettings: () => {
		LSWrapper.setItem(_AppMap.settingsKey, _AppMap.view.settings);
	},
	initControls: () => {

		let view = _AppMap.view;
		let bar  = Structr.functionBar;

		_AppMap.showCounts();

		let lookButtons = bar.querySelectorAll('[data-look]');
		let press       = active => lookButtons.forEach(b => b.setAttribute('aria-pressed', String(b === active)));

		press([...lookButtons].find(b => b.dataset.look === view.settings.look));

		for (let button of lookButtons) {

			button.addEventListener('click', () => {

				view.setLook(button.dataset.look);
				press(button);
				_AppMap.saveSettings();
			});
		}

		// the access lens: nobody, public users, authenticated users or one of the groups
		let lens  = bar.querySelector('#appmap-lens');
		let roles = [
			{ key: '',       name: 'nobody' },
			{ key: 'public', name: 'Public users', kind: 'public' },
			{ key: 'auth',   name: 'Authenticated users', kind: 'auth' },
			...view.model.groups.slice().sort((a, b) => a.n.localeCompare(b.n)).map(g => ({ key: 'group:' + g.id, name: g.n, kind: 'group', id: g.id }))
		];

		for (let role of roles) {
			lens.add(new Option(role.name, role.key));
		}

		lens.addEventListener('change', () => {

			let role = roles.find(r => r.key === lens.value);

			view.setLens(role?.kind ? role : null);
		});

		// the activity lens, remembered: how much the entry points are used; Slow, which an earlier version offered, shows the
		// use as well
		let saved    = LSWrapper.getItem(_AppMap.settingsKey, {});
		let activity = bar.querySelector('#appmap-activity');
		let showUse  = on => {

			activity.setAttribute('aria-pressed', String(on));
			view.setActivity(on ? 'usage' : null);
		};

		showUse(!!saved.activity);

		// the hotspots show while the pointer or the keyboard focus rests on their button: the most used elements
		let spotlight = bar.querySelector('#appmap-spotlight');

		// pointer and focus each hold them on their own: one that leaves does not end what the other still holds
		let holders = new Set();

		for (let [event, holder, on] of [['pointerenter', 'pointer', true], ['pointerleave', 'pointer', false], ['focus', 'focus', true], ['blur', 'focus', false]]) {

			spotlight.addEventListener(event, () => {

				let was = holders.size > 0;

				if (on) {
					holders.add(holder);
				} else {
					holders.delete(holder);
				}

				if (was !== holders.size > 0) {
					view.setSpotlight(!was);
				}
			});
		}

		activity.addEventListener('click', () => {

			showUse(activity.getAttribute('aria-pressed') !== 'true');
			_AppMap.saveSettings();
		});

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
					<span id="appmap-status"></span>
				</div>
			</div>
		`,
		functions: () => `
			<div class="flex items-center gap-4 flex-grow">
				<span class="appmap-segment" role="group" aria-label="Look">
					<button data-look="light">Light</button>
					<button data-look="dark">Dark</button>
				</span>
				<label class="appmap-lens">Access of
					<select id="appmap-lens" title="Shows what a role sees and may do, and where a page it sees does what it may not"></select>
				</label>
				<span class="appmap-segment" role="group" aria-label="Activity">
					<button id="appmap-activity" aria-pressed="false" title="Shows how often pages, endpoints and types were requested and scheduled methods ran since the start">Usage</button>
				</span>
				<span class="appmap-segment" role="group" aria-label="Hotspots">
					<button id="appmap-spotlight" title="Hover to grey out the map and show only the most used pages, endpoints, types and scheduled methods, with their number of requests or runs">Hotspots</button>
				</span>
				<span class="appmap-segment" role="group" aria-label="Help">
					<button id="appmap-intro-toggle" aria-pressed="false" title="How to read and use the map">?</button>
				</span>
				<span id="appmap-counts" class="appmap-counts"></span>
			</div>
		`
	}
};
