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
	intro: [
		['The map', 'Every sector is an area of Structr, every point a type, page, method, endpoint or group of the application; bold names are the hotspots many others rely on and the most important pages. Pages that belong together form a section, by their category or by the type they work with most.'],
		['Move', 'The wheel zooms, dragging pans the map.'],
		['Look', 'Hover an element to see what it connects to.'],
		['Explore', 'Click an element to hold it, then click the names at the ends of its relations to go there; the row above the map leads back, Esc ends exploring.'],
		['Open', 'Double-click a page to see its HTML tree, a method to read its code; double-click the empty map for the whole map again.'],
		['Search', 'The global search at the top right marks what it finds on the map; a click on one of its results goes there.'],
		['Access', 'The badge at a page shows who may see it: red for public users, blue for signed-in users, grey for admins and grants only. "Access of" above the map shows what a role sees and may do: around every type a ring of read, write and delete, and in red the pages it sees that do what it may not.'],
		['Data model', 'Hover a type to see its relationships with their multiplicities: red ones delete along, orange ones pass rights on; dashed red rings mark everything deleting one of its objects takes with it.'],
		['Pages', 'A tag below a page marks the start page, the errors it is shown for and content other than HTML; its tooltip names its URLs and the type of object they open.']
	],

	onload: async () => {

		Structr.setMainContainerHTML(_AppMap.templates.main());
		Structr.setFunctionBarHTML(_AppMap.templates.functions());

		await _AppMap.load();
	},
	unload: () => {

		_AppMap.view?.destroy();
		_AppMap.view = undefined;
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
				remove: key => LSWrapper.removeItem(key)
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
			loadSource: async id => {

				let response = await fetch(Structr.rootUrl + 'SchemaMethod/' + id);

				return response.ok ? ((await response.json()).result?.source ?? '') : null;
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
	showCounts: () => {

		let counts = _AppMap.view.counts;

		document.querySelector('#appmap-counts').textContent = `${counts.elements.toLocaleString()} elements · ${counts.dom.toLocaleString()} HTML elements · ${counts.relations.toLocaleString()} relations`;
	},
	saveSettings: () => {
		LSWrapper.setItem(_AppMap.settingsKey, { look: _AppMap.view.settings.look });
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

		// the introduction: open on a first visit, the question mark brings it back
		let intro     = document.querySelector('#appmap-intro');
		let introOpen = bar.querySelector('#appmap-intro-toggle');
		let showIntro = show => {

			intro.classList.toggle('hidden', !show);
			introOpen.setAttribute('aria-pressed', String(show));
		};

		introOpen.addEventListener('click', () => showIntro(intro.classList.contains('hidden')));
		intro.querySelector('#appmap-intro-close').addEventListener('click', () => {

			showIntro(false);
			LSWrapper.setItem(_AppMap.introKey, true);
		});

		showIntro(!LSWrapper.getItem(_AppMap.introKey, false));
	},

	templates: {
		main: () => `
			<link rel="stylesheet" type="text/css" media="screen" href="css/appmap.css">

			<div class="main-app-box" id="appmap">
				<div id="appmap-canvas"></div>
				<div id="appmap-intro" class="appmap-panel appmap-intro hidden">
					<b>How to read the map</b>
					<dl>
						${_AppMap.intro.map(([term, text]) => `<dt>${term}</dt><dd>${text}</dd>`).join('')}
					</dl>
					<button id="appmap-intro-close" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">Got it</button>
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
				<span class="appmap-segment" role="group" aria-label="Help">
					<button id="appmap-intro-toggle" aria-pressed="false" title="How to read and use the map">?</button>
				</span>
				<span id="appmap-counts" class="appmap-counts"></span>
			</div>
		`
	}
};
