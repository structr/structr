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

/**
 * The application map on a canvas: every type, method, page, shared component, endpoint, external service and group is
 * an element, placed by a force simulation in the sector of its Structr area; repeaters and event action mappings live
 * inside their page. The relations between the elements run bundled into strands. The map is looked at: it pans, zooms
 * and answers the pointer; a click holds the focus on an element to explore from there, a double click glides closer,
 * nothing is moved.
 *
 * The view holds the state that all parts share (the elements, the current relation set and its index, the transform, the
 * look, the settings and what is hovered) and composes the parts that work on it: AppMapLayout places the elements,
 * AppMapStrands bundles the relations, AppMapPanels shows what is inside the elements (the DOM of the pages, the code of
 * the methods) and AppMapRenderer draws. The settings and
 * the developer tools are the view's public interface; the module in appmap.js only talks to it.
 */
class AppMapView {

	// two looks, the light and the dark theme of the design system of the new UI ("Simple"): warm paper neutrals and the
	// five colours of its aspects, Types green, Screens purple, Permissions blue, Processes orange and Data amber, which the
	// kinds of the map share (external services stay neutral, as the app does there); kinds of one aspect differ in their
	// shape. The canvas is soft grey with a faint grid of dots, the cards white; colour lives in strokes and rings, not in
	// large fills. On black overlaps glow, on paper they are simply painted over each other: darkening them by multiplying
	// took more than half of every frame and was hardly to be seen.
	// bg is the canvas, card a card on it, node the inside of the ring of an element, tint the faint fill of a quiet surface, border and hair the two strengths of a
	// line, text the strongest text, label the one of most text, faint the quietest, grid the dots of the canvas
	static LOOKS = {
		light: {
			bg: '#f6f5f0', card: '#ffffff', node: '#ffffff', tint: '#eceae1', border: '#d8d5ca', hair: '#e9e7de', grid: '#e1dfd5', text: '#1d1c18', faint: '#8a8678',
			veil: '#eceae1', trace: '#8a8678', ring: '#1d1c18', outline: '#ffffff', halo: '#f6f5f0', label: '#56534b', labelNear: '#1d1c18', labelFocus: '#1d1c18', column: '#8a8678', found: '#ffd84d', warning: '#8a6500', error: '#c22a2a', blend: 'source-over', dark: false,
			kinds: { page: '#8b83e6', component: '#8b83e6', type: '#7ab02a', method: '#e06a40', api: '#4d97e8', external: '#8a8678', group: '#4d97e8', dom: '#8b83e6', view: '#ef9f27', action: '#e06a40' }
		},
		dark: {
			bg: '#1b1a16', card: '#262420', node: '#2f2c27', tint: '#322f29', border: '#3a3832', hair: '#2c2a25', grid: '#2c2a25', text: '#f3f1ea', faint: '#8b887d',
			veil: '#141310', trace: '#8b887d', ring: '#f3f1ea', outline: '#1b1a16', halo: '#1b1a16', label: '#bdb9ad', labelNear: '#f3f1ea', labelFocus: '#ffffff', column: '#8b887d', found: '#d4a900', warning: '#e6c13f', error: '#f4746e', blend: 'lighter', dark: true,
			kinds: { page: '#8b83e6', component: '#8b83e6', type: '#7ab02a', method: '#e06a40', api: '#4d97e8', external: '#8b887d', group: '#4d97e8', dom: '#8b83e6', view: '#ef9f27', action: '#e06a40' }
		}
	};

	// the size of an element grows with its measure (see measureOf), against the largest element of its own sector: each
	// sector has its own scale, so a sector of small elements shows its structure as clearly as one of large ones (see
	// radiusOf for the curve); SIZE_BY is 'size' for how large an element is, 'links' for how connected, 'objects' for
	// the clusters of the data map, which are compared over all sectors by the area of their objects; a view may have
	// its own (see the options sizeBy and radius)
	static SIZE_BY = 'links';
	static R_MIN   = 2.6;
	static R_MAX   = 26;

	// a page is at least this large, so its shape stays readable
	static R_MIN_PAGE = 5;

	// from afar the elements are drawn larger than the layout spaces them, by up to this factor, fading out as the map comes
	// closer, where they have the size the layout gives them and keep clear of each other; the layout keeps its distances
	static DRAWN = 1;

	// from the zoom at which the whole map fits the screen up to SIZE_ZOOM times closer the elements grow slower than the map,
	// by the square root of the zoom, so from afar they are large enough to be seen and close up they leave room between them;
	// beyond SIZE_ZOOM they have the size of the layout
	static SIZE_ZOOM  = 5;
	static MAX_GROWTH = Math.sqrt(AppMapView.SIZE_ZOOM);

	// the pointer finds an element within its shape; while it is small on screen, within at least MIN_TARGET pixels of its
	// centre, and below SMALL pixels a type also this share of its radius beyond its edge and the outline of its grouping
	// with it, so the centre of a grouping is easy to hit from afar; close up the shape alone answers
	static MIN_TARGET  = 10;
	static SMALL       = 12;
	static TYPE_MARGIN = 0.6;

	// how far out the map can be zoomed, as a share of the zoom at which it fits the screen; how far in at least, and further
	// for the pages with the largest trees, until the boxes of their HTML elements are ZOOM_IN_BOX pixels high, so that
	// every HTML element can open with its controls (see AppMapPanels.elementPanel)
	static ZOOM_OUT    = 0.25;
	static ZOOM_IN     = 150;
	static ZOOM_IN_BOX = 480;

	// a double click on an HTML element glides so close that its controls stand at this share of their size
	static ELEMENT_FILL = 1.15;

	// the wheel does not jump from notch to notch: each notch moves a target, and the map approaches it continuously, by about
	// two thirds of the remaining way in this many milliseconds, independent of how long a frame takes to draw
	static WHEEL_EASE_MS = 80;

	// panning coasts on after the pointer lets go, as on a map: with the speed of the last DRAG_WINDOW milliseconds of the
	// drag, slowing down to about a third in COAST_MS; a drag that came to rest before letting go does not coast
	static DRAG_WINDOW = 80;
	static COAST_MS    = 325;
	static COAST_FROM  = 0.15;

	// a double click glides to the inner structure of an element in this many milliseconds; a page or shared component
	// then fills this share of the screen, with its tree open, a grouping its outline
	static GLIDE_MS   = 900;
	// the shorter way into place when an editor opens, see settleOn
	static SETTLE_MS  = 350;

	// the most important pages are named in bold, as many as there are hotspots (see AppMapModel.importance)
	static KEY_PAGES = AppMapModel.HOTSPOTS;
	static GLIDE_FILL = 0.86;

	// the editor of code, of a method or a template, stands only in an element that fills the screen, as after a click or a
	// double-click on it: it opens when the element takes this share of the screen, and closes again below the second
	static MAXIMIZED = [0.7, 0.55];

	// the editor of a method stays open down to this share of the zoom at which it opened, as MAXIMIZED keeps that of a
	// template
	static OPEN_KEEP = 0.75;

	// the size of the text in every editor that stands in an element filling the screen, in pixels
	static EDITOR_FONT = 13;

	// exploring an element without inner structure ends at most so close that it takes this share of the screen, so its
	// relations and neighbours stay in view
	static EXPLORE_SIZE = 0.12;

	// the room around the map when it fits the screen, in pixels, across and down: the names of the areas are wide
	static FIT_MARGIN = [110, 40];

	// in focus the map shows the relations one step beyond the neighbours, at most this many
	static CONTEXT_LIMIT = 240;

	// a long computation takes at most this many milliseconds of a frame, so the browser stays responsive
	static SLICE_MS = 12;

	constructor(container, model, options = {}) {

		const settings = Object.assign({ look: 'light' }, options.settings);

		this.container   = container;
		this.model       = model;
		this.nodes       = model.nodes;
		this.byId        = model.byId;
		this.storage     = options.storage ?? { get: () => null, set: () => {}, remove: () => {} };
		this.onStatus    = options.onStatus ?? (() => {});
		this.synchronous = !!options.synchronous;

		this.look  = AppMapView.LOOKS[settings.look] ?? AppMapView.LOOKS.light;

		// the sectors, their hues and the sizes of the elements: those of the application, or those of another map in its
		// form, as the data map (see _AppMap.loadData)
		this.areas   = options.areas ?? AppMapModel.AREAS;
		this.areaHue = options.areaHue ?? AppMapRenderer.AREA_HUE;
		this.sizeBy  = options.sizeBy ?? AppMapView.SIZE_BY;
		this.rMin    = options.radius?.[0] ?? AppMapView.R_MIN;
		this.rMax    = options.radius?.[1] ?? AppMapView.R_MAX;
		this.links = [...model.links];

		// the hovered things: an element, an HTML element of a page, a strand, a jump marker, and the collapsed jump marker
		// that is open
		this.hovered        = null;
		this.hoveredDom     = null;
		this.hoveredStrand  = null;
		this.hoveredMarker  = null;
		this.expandedMarker = null;

		// exploring: a click or a double click holds the focus on an element, or on an HTML element of an open page; the trail
		// is the way it took there
		this.pinned    = null;
		this.pinnedDom = null;
		this.trail     = [];
		this.onHint = options.onHint ?? null;
		this.hint   = null;

		// the global search: what it found on the map, the element each of its results leads to, and where the server said
		// the objects lie, asked once per object
		this.found      = null;
		this.foundFor   = new Map();
		this.located    = new Map();
		this.locate     = options.locate ?? null;
		this.searchRun  = 0;

		// the access lens: what one role reaches, see AppMapAccess
		this.access = null;

		// the activity lens: how much the entry points are used, see setActivity
		this.activity      = null;
		this.spotlight     = false;
		this.spotlightLens = false;
		this.loadActivity  = options.loadActivity ?? null;
		this.activityTimer = null;

		this.transform  = d3.zoomIdentity;
		this.baseK      = 1;
		this.growth     = 1;
		this.width      = 0;
		this.height     = 0;
		this.pending    = false;
		this.destroyed  = false;
		this.userZoomed = false;
		this.wheelK     = null;
		this.wheelAt    = null;
		this.wheelLast  = 0;
		this.wheelFrame = null;
		this.drag       = [];
		this.coastFrame = null;
		this.grabbing   = false;

		// reads the source of a method, or the text of the template a page consists of, for its box close up, as a promise of
		// the text
		this.loadSource = options.loadSource ?? null;

		// reads the texts of an HTML element, for its box once it opens, as a promise of a list of { content, code }
		this.loadContent = options.loadContent ?? null;

		// reads the schema of a type, for its card close up, as a promise of the type as the _schema endpoint gives it, and
		// the code of its function properties, as a promise of a list of { name, readFunction, writeFunction }
		this.loadType      = options.loadType ?? null;
		this.loadFunctions = options.loadFunctions ?? null;

		// reads the schema relationships of types, for the boxes of their relationship properties, as a promise of a list
		this.loadRelationships = options.loadRelationships ?? null;

		// reads the resource access permissions, for the cards of the REST endpoints, and the members of a group, for its card
		this.loadGrants  = options.loadGrants ?? null;

		// reads how often the grants decided a request and the pages were viewed in the last days, see usageOf
		this.loadUsage = options.loadUsage ?? null;
		this.usage     = {};

		// the views of the pages are read at once, so that the first tooltip on a page has them
		this.usageOf('pages');
		this.loadMembers = options.loadMembers ?? null;

		// leads out of the map to what an open card stands for, in the Code area or the Security area, see placeOpenButton
		this.openIn = options.openIn ?? null;

		// switches a setting in an open box, see flip; a promise of whether it was changed
		this.toggle = options.toggle ?? null;

		// edits the code of a method or the text of a template above its field, see edit; the editor itself is the Admin
		// UI's, the map only says where it stands
		this.editor  = options.editor ?? null;
		this.editing = null;
		this.ending  = false;

		// the element the last click set off a glide into, for a double click it begins (see gliding)
		this.glidingFrom = null;

		// the scripting errors with their call traces (see setTraces), by the element each failed in, and the one shown
		this.traceData = [];
		this.traces    = [];
		this.tracesOf  = new Map();
		this.callTrace = null;

		// the controls of an HTML element opened inside its box, which follow the open element nearest the middle of the
		// screen (see followElement)
		this.domEditor = options.domEditor ?? null;

		// the fonts of the design system of the new UI: the sans serif font of the system, and JetBrains Mono for names
		// of fields and code; the map is drawn again once the latter has loaded, it is not there before it is first used
		this.fonts = {
			main: 'ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif',
			mono: '"JetBrains Mono", ui-monospace, SFMono-Regular, Menlo, monospace'
		};

		document.fonts?.load(`400 12px ${this.fonts.mono}`).then(() => {

			// widths measured with the fallback are wrong now
			this.renderer?.textWidths?.clear();
			this.requestDraw();

		}).catch(() => {});

		this.canvas             = document.createElement('canvas');
		this.canvas.className   = 'appmap-canvas';
		this.ctx                = this.canvas.getContext('2d');
		this.tip                = document.createElement('div');
		this.tip.className      = 'appmap-tip';
		this.tip.hidden         = true;
		this.trailBar           = document.createElement('div');
		this.trailBar.className = 'appmap-trail';
		this.trailBar.hidden    = true;

		container.appendChild(this.canvas);
		container.appendChild(this.tip);
		container.appendChild(this.trailBar);

		// the way out of the map, as an HTML element has its way into the Pages area; the wheel over it zooms the map
		if (this.openIn) {

			this.openButton             = document.createElement('button');
			this.openButton.type        = 'button';
			this.openButton.className   = 'appmap-open';
			this.openButton.textContent = 'Open';
			this.openButton.hidden      = true;
			this.openButton.addEventListener('click', () => {

				const card = this.openCard;

				// a method lies in the folder of its type in the Code area
				if (card) {
					this.openIn({ ...card.open, owner: card.open.kind === 'method' ? this.links.find(l => l.kind === 'member' && l.source === card.open.node)?.target ?? null : null });
				}
			});
			this.openButton.addEventListener('wheel', e => {

				this.canvas.dispatchEvent(new WheelEvent('wheel', e));
				e.preventDefault();

			}, { passive: false });

			container.appendChild(this.openButton);
		}

		this.rebuildIndex();

		this.layout   = new AppMapLayout(this);
		this.strands  = new AppMapStrands(this);
		this.panels   = new AppMapPanels(this);
		this.renderer = new AppMapRenderer(this);

		this.renderer.recolor();
		this.bindEvents();
		this.resize();

		// the map shows from the first frame; a fresh layout settles visibly, a remembered one is there at once
		this.layout.start(moved => this.onSettled(moved));
		this.fitAll();
	}

	destroy() {

		this.destroyed = true;
		clearInterval(this.activityTimer);
		this.layout.stop();
		this.strands.cancel();
		this.resizeObserver?.disconnect();

		if (this.wheelFrame) {
			cancelAnimationFrame(this.wheelFrame);
		}

		this.stopCoast();
		document.removeEventListener('keydown', this.onKey);
		document.removeEventListener('keyup', this.onKey);
		this.editor?.dispose();
		this.domEditor?.dispose();

		d3.select(this.canvas).on('.zoom', null).on('mousemove', null).on('mouseleave', null).on('click', null).on('dblclick', null);
		this.canvas.remove();
		this.tip.remove();
		this.openButton?.remove();
		this.trailBar.remove();
	}

	get settings() {

		return { look: this.look.dark ? 'dark' : 'light' };
	}

	get counts() {

		return { elements: this.nodes.length, dom: this.panels.domCount, relations: this.links.length };
	}

	/**
	 * Runs a long computation in slices of at most SLICE_MS per animation frame: step() is called until it returns false,
	 * frame() after every slice, done() at the end. With the synchronous option (the tests) it runs to the end at once.
	 */
	slice(step, done, frame) {

		const next = () => {

			if (this.destroyed) {
				return;
			}

			const end = performance.now() + AppMapView.SLICE_MS;
			let more  = true;

			while (more && (this.synchronous || performance.now() < end)) {
				more = step();
			}

			frame?.();

			if (more) {
				requestAnimationFrame(next);
			} else {
				done();
			}
		};

		next();
	}

	// ----- the index of the current relation set: degree, neighbours, hotspots and modules -----
	rebuildIndex() {

		this.degree      = new Map();
		this.neighbours  = new Map(this.nodes.map(n => [n, []]));
		this.sectorScale = null;

		for (const l of this.links) {

			this.degree.set(l.source, (this.degree.get(l.source) ?? 0) + 1);
			this.degree.set(l.target, (this.degree.get(l.target) ?? 0) + 1);
			this.neighbours.get(l.source).push(l.target);
			this.neighbours.get(l.target).push(l.source);
		}

		// the caches that hang on the relation set start anew: the set is replaced in place, so its identity says nothing
		this.generation = (this.generation ?? 0) + 1;
		this.analysis   = AppMapModel.analyse(this.links, n => this.anchorOf(n));
		this.importance = AppMapModel.importance(this.links);
		this.keyPages   = new Set([...this.importance].filter(([n, i]) => n.kind === 'page' && i.value > 0).sort((a, b) => b[1].value - a[1].value).slice(0, AppMapView.KEY_PAGES).map(([n]) => n));
		this.context    = null;
		this.liftedFor  = null;

		// an element that left the map leaves the trail
		if (this.trail?.some(n => !this.neighbours.has(n))) {

			this.trail  = this.trail.filter(n => this.neighbours.has(n));
			this.pinned = this.trail[this.trail.length - 1] ?? null;
			this.showTrail();
		}
	}

	// the hotspot an element is, as AppMapModel.analyse describes it, or null; only the anchor of a grouping can be one
	hotspotOf(n) {

		return this.analysis.hotspots.get(n) ?? null;
	}

	groupKeyOf(n) {

		return n.grouping ?? '-';
	}

	// the element a grouping is named after: the type, page, component, group or endpoint
	anchorOf(n) {

		return this.byId.get(this.groupKeyOf(n)) ?? n;
	}

	radiusOf(n) {

		// full size needs at least ten, so a sector of small or loosely connected elements does not swell
		if (!this.sectorScale) {
			this.sectorScale = d3.rollup(this.nodes, v => Math.max(10, d3.max(v, m => this.measureOf(m))), m => this.scaleKeyOf(m));
		}

		const most = this.sectorScale.get(this.scaleKeyOf(n)) ?? 1;
		const min  = AppMapView.isSheet(n) ? AppMapView.R_MIN_PAGE : this.rMin;

		// sizes spread widely (a method of ten lines beside one of nine hundred), so they count by their logarithm; relations
		// and objects by the square root, so the area is proportional
		const share = this.sizeBy === 'size' ? Math.log1p(this.measureOf(n)) / Math.log1p(most) : Math.sqrt(this.measureOf(n) / most);

		return min + (this.rMax - min) * Math.min(1, share);
	}

	// the keys that save in an editor, as the Admin UI takes them: Cmd+S on a Mac, Ctrl+S elsewhere
	static get SAVE_KEY() {

		return typeof _Helpers !== 'undefined' && _Helpers.isMac() ? 'Cmd+S' : 'Ctrl+S';
	}

	// pages and shared components, the sheets of the map
	static isSheet(n) {

		return n.kind === 'page' || n.kind === 'component';
	}

	/**
	 * What the size of an element follows (see SIZE_BY): how large it is in its own terms, as the server measures it (the
	 * properties of a type, the lines of code of a method, the HTML elements of a page, the members of a group), or how much
	 * it matters: the number of its relations, for a page or shared component its importance (see AppMapModel.importance).
	 * Where the server sends no size, a page is measured by its outline and anything else by its relations.
	 */
	measureOf(n) {

		const degree = this.degree.get(n) ?? 0;

		// a REST endpoint and a group are as large as what their cards hold, the grants of the one, the members and grants of
		// the other (see AppMapPanels.insidePanel, whose card is as large as the element)
		if (AppMapView.byContent(n)) {
			return n.kind === 'api' ? (n.rest?.rules ?? 1) : n.size;
		}

		if (this.sizeBy === 'objects') {
			return n.size ?? 0;
		}

		if (this.sizeBy !== 'size') {
			return AppMapView.isSheet(n) ? (this.importance.get(n)?.value ?? 0) : degree;
		}

		return n.size ?? (n.outline ? AppMapView.outlineSize(n.outline) : null) ?? (n.kind === 'group' ? n.members : null) ?? degree;
	}

	// sizes are compared within a sector, and by size within one kind, as the lines of a method say nothing about a type
	scaleKeyOf(n) {

		const area = n.area ?? '-';

		if (this.sizeBy === 'objects') {
			return '*';
		}

		return this.sizeBy === 'size' || AppMapView.byContent(n) ? `${area}|${n.kind}` : area;
	}

	// the elements whose size follows what they hold, compared only among their kind, see measureOf
	static byContent(n) {

		return n.kind === 'api' || n.kind === 'group';
	}

	// the number of HTML elements in an outline, a flat list of its items, see AppMapModel
	static outlineSize(items) {

		return items.length;
	}

	// the radius an element is drawn with at the current zoom, see SIZE_ZOOM and DRAWN
	shownRadius(n) {

		return this.radiusOf(n) * AppMapView.enlarged(this.growth);
	}

	// the radius an element would be drawn with at another zoom, for a glide that heads there
	drawnRadiusAt(n, k) {

		return this.radiusOf(n) * AppMapView.enlarged(this.growthAt(k));
	}

	// how much larger than in the layout an element is drawn, from the growth at a zoom: DRAWN times as large where the
	// growth is greatest, not at all where it has ended
	static enlarged(growth) {

		return growth * (1 + (AppMapView.DRAWN - 1) * (growth - 1) / (AppMapView.MAX_GROWTH - 1));
	}

	/**
	 * The groupings an element reaches, strongest first, as [anchor, number of relations]: for the anchor of a grouping
	 * those of the whole grouping, for any other element its own, each lifted to the grouping on the other side. On the
	 * map a repeater matters through its page and a method through its type, so hovering shows the grouping it reaches;
	 * the endpoint keeps delivering the relations between the elements themselves for other consumers.
	 */
	liftedTargets(n) {

		return [...this.liftedLinks(n)].map(([anchor, links]) => [anchor, links.length]).sort((a, b) => b[1] - a[1]);
	}

	// the relations behind each lifted arc, per grouping on the other side; a relation inside the element's own grouping
	// lifts to nothing, and the scaffolding (see AppMapModel.STRUCTURE) not at all
	liftedLinks(n) {

		if (n !== this.liftedFor?.focus || this.liftedFor.generation !== this.generation) {

			const home   = this.anchorOf(n);
			const own    = home === n ? (x => this.anchorOf(x) === n) : (x => x === n);
			const result = new Map();

			for (const l of this.links) {

				if (AppMapModel.STRUCTURE.has(l.kind)) {
					continue;
				}

				const other  = own(l.source) ? l.target : (own(l.target) ? l.source : null);
				const anchor = other ? this.anchorOf(other) : null;

				if (anchor && anchor !== home) {
					(result.get(anchor) ?? result.set(anchor, []).get(anchor)).push(l);
				}
			}

			this.liftedFor = { focus: n, generation: this.generation, result };
		}

		return this.liftedFor.result;
	}

	// the context of the element in focus one step beyond its neighbours: the relations of its neighbours to further elements,
	// and those elements; the strongest first, at most CONTEXT_LIMIT relations, so a type everybody uses stays readable; for
	// a hotspot also the elements of its module
	contextAround(n) {

		if (n !== this.context?.focus || this.context.generation !== this.generation) {

			const first  = new Set(this.neighbours.get(n));
			const shown  = l => !AppMapModel.STRUCTURE.has(l.kind);
			const beyond = this.links
				.filter(l => l.source !== n && l.target !== n && (first.has(l.source) || first.has(l.target)) && shown(l))
				.sort((a, b) => b.weight - a.weight)
				.slice(0, AppMapView.CONTEXT_LIMIT);
			const nodes  = new Set(beyond.flatMap(l => [l.source, l.target]).filter(x => x !== n && !first.has(x)));
			const module = this.hotspotOf(n) ? this.analysis.moduleOf.get(n) : null;

			// a hotspot brings its whole module forward: everything that works together with it
			if (module) {

				const members = new Set(module.members);

				for (const m of this.nodes) {

					if (m !== n && !first.has(m) && members.has(this.anchorOf(m))) {
						nodes.add(m);
					}
				}
			}

			this.context = { focus: n, generation: this.generation, beyond, nodes };
		}

		return this.context;
	}

	// ----- what the layout reports -----
	// the elements move: the courses and strands of the old places are void
	onLayoutChanged() {

		this.renderer.recolor();
		this.strands.cancel();
		this.strands.reset();
		this.hoveredStrand = null;
		this.requestDraw();
	}

	// the layout rests: now the relations are bundled; a fresh layout is fitted to the screen unless the user moved it
	onSettled(moved) {

		if (moved && !this.userZoomed) {
			this.fitAll();
		}

		if (this.synchronous) {
			this.strands.rebuild();
		} else {
			this.strands.schedule();
		}

		this.requestDraw();
	}

	// ----- settings -----
	setLook(name) {

		this.look = AppMapView.LOOKS[name] ?? AppMapView.LOOKS.light;
		this.renderer.recolor();
		this.requestDraw();
	}

	// ----- for the console: forgets the remembered layout and lays everything out anew -----
	relayout() {

		this.userZoomed = false;
		this.layout.relayout();
	}

	// ----- drawing -----
	requestDraw() {

		if (this.pending || this.destroyed) {
			return;
		}

		this.pending = true;

		requestAnimationFrame(() => {

			this.pending = false;

			if (!this.destroyed) {
				this.draw();
			}
		});
	}

	draw() {

		this.renderer.draw();
		this.followElement();
		this.followCode();
		this.placeEditor();
		this.placeOpenButton();
		this.updateHint();
	}

	// the element in focus: the one exploring holds, else the one under the pointer; an element whose inner structure is
	// open shows that instead
	get focus() {

		const n = this.pinned ?? this.hovered;

		return n && !this.panels.isOpen(n) ? n : null;
	}

	// ----- exploring: a click holds the focus, the names at the ends of its relations lead on -----
	// a click on an element holds its focus, on a jump marker leads to the element it names, on the empty map ends exploring;
	// inside an open page or a readable method the click belongs to what is shown there
	async onClick(e) {

		const [px, py] = d3.pointer(e, this.canvas);
		const [wx, wy] = this.transform.invert([px, py]);

		// the second click of a double click whose first one set off a glide lands on the moving map; it belongs to the
		// double click, which leads where the first one did (see onDoubleClick)
		if (e.detail > 1 && this.gliding()) {
			return;
		}

		this.glidingFrom = null;

		// a setting in an open box is switched where it is
		const toggle = this.toggle && this.panels.toggleAt(wx, wy);

		if (toggle) {

			toggle.item.tile.grant ? this.flipAccess(toggle) : this.flip(toggle);
			return;
		}

		const marker   = this.renderer.findMarker(px, py);
		const dom      = this.panels.findDom(wx, wy);
		const code     = this.editor && this.panels.codeAt(wx, wy, this.transform.k);

		// a click on readable code or on a template fills the screen with it, and there its editor opens (see followCode and
		// followElement); a click beside the editor of code ends editing first. The controls of other HTML elements come and
		// go with the zoom
		if (code && !marker) {

			// unsaved changes in another editor are kept or discarded first
			if (this.editing && this.editing !== code && !(await this.endEdit())) {
				return;
			}

			this.glidingFrom = { at: performance.now(), code };
			this.glideInto(code);
			return;
		}

		if (dom && AppMapPanels.isSheet(dom) && this.domEditor && !marker) {

			this.glidingFrom = { at: performance.now(), dom };
			this.pinDom(dom);
			this.glideIntoElement(dom);
			return;
		}

		if (this.editing && !this.editing.page) {

			this.endEdit();
			return;
		}

		// a marker leads to the element it names; a collapsed one opens under the pointer, so each of its elements can be chosen
		if (marker) {

			this.explore(marker.anchors[0]);
			return;
		}

		// an HTML element with relations of its own is explored from like an element
		if (dom) {

			this.pinDom(dom);
			return;
		}

		// the sign of an element with scripting errors shows the call trace of one of them, the next one with every click
		const troubled = this.renderer.findProblems(wx, wy);

		if (troubled && this.tracesOf.get(troubled)?.length) {

			this.showCallTrace(troubled);
			return;
		}

		const access = troubled ?? this.renderer.findAccess(wx, wy);
		const n      = access ?? this.find(px, py);

		if (n) {

			this.pin(n);

		} else if (!this.panels.covers(wx, wy) && !this.panels.findDom(wx, wy)) {

			this.unpin();
		}
	}

	// ----- editing: the code of a method or the text of a template, in the editor of the Admin UI above its field, and the
	// settings of an HTML element in it when it is open -----
	// an HTML element of a page has its own editor
	editorOf(n) {

		return n.page ? this.domEditor : this.editor;
	}

	// the controls of an HTML element stand in the open element nearest the middle of the screen: they follow it as the map
	// moves, stay with an element whose changes are not saved, and give way to the editor of code
	followElement() {

		const editing = this.editing;

		if (!this.domEditor || this.spotlight || (editing && !editing.page)) {
			return;
		}

		// a template has its controls, with the editor of its content, only while it fills the screen
		const open = this.panels.openElements().filter(d => !AppMapPanels.isSheet(d) || this.fills(d, d === editing));
		const next = open[0] ?? null;

		// the controls stay with their element while it is open and about as near the middle as the nearest one, so that
		// they do not jump between two elements at the same distance; unsaved changes keep them where they are
		if (editing && this.domEditor.isDirty()) {
			return;
		}

		if (editing && open.includes(editing) && (editing === next || this.panels.fromCentre(editing) < this.panels.fromCentre(next) * 1.3 + 40)) {
			return;
		}

		if (editing) {

			this.domEditor.dispose();
			this.editing = null;
		}

		if (next) {

			this.editing = next;
			this.domEditor.open(next);

			// the editor of a template stands in the middle of the screen
			if (AppMapPanels.isSheet(next)) {
				this.settleOn(next);
			}
		}
	}

	// the editor of code opens on the readable code that fills the screen, and closes when it no longer does; code whose
	// editor was closed by hand gets it again only after it has left the screen. Unsaved changes keep it where it is
	followCode() {

		const editing = this.editing;

		if (!this.editor || this.spotlight || editing?.page) {
			return;
		}

		if (this.dismissed && !(this.panels.reading.has(this.dismissed) && this.fills(this.dismissed, true))) {
			this.dismissed = null;
		}

		if (editing && (this.editor.isDirty() || (this.panels.reading.has(editing) && this.fills(editing, true)))) {
			return;
		}

		const next = [...this.panels.reading].find(n => n !== this.dismissed && this.fills(n, false)) ?? null;

		if (next === editing) {
			return;
		}

		if (editing) {

			this.editor.dispose();
			this.editing = null;
		}

		if (next) {

			this.tip.hidden = true;
			this.editing    = next;
			this.editor.open(next);
			this.settleOn(next);
		}
	}

	// when an editor opens, its element glides to the middle of the screen and to the zoom at which it fills it, unless a
	// glide is already under way, which leads there itself, or the user moves the map, by hand or as it coasts on after
	settleOn(n) {

		if (d3.active(this.canvas) || this.grabbing || this.coastFrame) {
			return;
		}

		if (n.page) {
			this.glideIntoElement(n, AppMapView.SETTLE_MS);
		} else {
			this.glideInto(n, AppMapView.SETTLE_MS);
		}
	}

	// the zoom at which a method, a type, a page of a single template or a template in a page fills GLIDE_FILL of the screen, with
	// its longer side
	fillZoom(n) {

		// the card of a type, a REST endpoint, a group or an external service, and the code of a method, open at the size of
		// their text, whatever their size: the names of the rows at CARD_TEXT, the code at CODE_TEXT; a card taller than the
		// screen stands with its top on it (see glideInto)
		if (!n.page && AppMapPanels.INSIDE_KINDS.has(n.kind)) {
			return this.zoomWhere(k => this.panels.insidePanel(n, k, this.drawnRadiusAt(n, k)).em * AppMapPanels.ROW_NAME * k, AppMapView.CARD_TEXT);
		}

		if (!n.page && AppMapPanels.hasCode(n) && n.kind === 'method') {
			return this.zoomWhere(k => this.panels.codePanel(n, k, this.drawnRadiusAt(n, k)).font * k, AppMapView.CODE_TEXT);
		}

		const size   = n.page ? k => (p => [p.width, p.height])(this.panels.elementPanel(n, k))
			: AppMapPanels.INSIDE_KINDS.has(n.kind) ? k => (p => [p.width, p.height])(this.panels.insidePanel(n, k, this.drawnRadiusAt(n, k)))
			: k => AppMapRenderer.boxOf(n).map(v => 2 * v * this.drawnRadiusAt(n, k));
		const across = this.zoomWhere(k => size(k)[0] * k, AppMapView.GLIDE_FILL * this.width);
		const down   = this.zoomWhere(k => size(k)[1] * k, AppMapView.GLIDE_FILL * this.height);

		return Math.min(across, down);
	}

	// the leaves of the map near the middle of the screen, the elements with nothing further inside, each with the zoom at
	// which it stands open at its full size, the one a double click on it glides to: a method and a template fill the screen
	// (see fillZoom), an HTML element has its controls at ELEMENT_FILL (see glideIntoElement), the box of a property its
	// full size (see tileZoom)
	leafZooms() {

		const quarter = Math.min(this.width, this.height) / 4;
		const near    = n => Math.abs(this.transform.applyX(n.x ?? 0) - this.width / 2) < this.width / 4 && Math.abs(this.transform.applyY(n.y ?? 0) - this.height / 2) < this.height / 4;
		const opened  = this.panels.openElements().filter(d => this.panels.fromCentre(d) < quarter);
		const methods = [...this.panels.reading].filter(near);
		const tiles   = this.tilesNearMiddle();

		return [
			...methods.map(n => this.fillZoom(n)),
			...opened.map(d => AppMapPanels.isSheet(d) ? this.fillZoom(d) : this.zoomWhere(k => this.panels.elementPanel(d, k).fit, AppMapView.ELEMENT_FILL)),
			...tiles.map(t => this.tileZoom(t.item))
		];
	}

	// zooming in stops where a leaf near the middle of the screen stands open at its full size (see leafZooms), and goes on
	// past it only with the next turn, where onWheel holds it
	fillStop(target) {

		const k     = this.transform.k;
		const stops = this.leafZooms().filter(f => f > k * 1.001 && f < target);

		return stops.length ? Math.min(...stops) : target;
	}

	// whether a leaf near the middle of the screen stands open at its full size or closer: then the map only zooms out
	atLeaf() {

		const k = this.transform.k;

		return this.leafZooms().some(f => k >= f * 0.999);
	}

	// whether a method, a page of a single template or a template in a page fills the screen, see MAXIMIZED: to open its
	// editor, or, when it has one already, to keep it. A method opens at the size of its code (see fillZoom), not filling
	// the screen, so it counts as filling it from that zoom on, and down to OPEN_KEEP of it once its editor is open
	fills(n, keep) {

		const t       = this.transform;

		if (!n.page && n.kind === 'method') {

			const centred = Math.abs(t.applyX(n.x) - this.width / 2) < this.width / 4 && Math.abs(t.applyY(n.y) - this.height / 2) < this.height / 4;

			return centred && t.k >= this.fillZoom(n) * (keep ? AppMapView.OPEN_KEEP : 0.999);
		}

		const [w, h]  = n.page ? (p => [p.width, p.height])(this.panels.elementPanel(n, t.k)) : (([bw, bh]) => [2 * bw * this.shownRadius(n), 2 * bh * this.shownRadius(n)])(AppMapRenderer.boxOf(n));
		const [x, y]  = n.page ? (p => [p.x + p.width / 2, p.y + p.height / 2])(this.panels.elementPanel(n, t.k)) : [n.x, n.y];
		const share   = Math.max(w * t.k / this.width, h * t.k / this.height);
		const centred = Math.abs(t.applyX(x) - this.width / 2) < this.width / 4 && Math.abs(t.applyY(y) - this.height / 2) < this.height / 4;

		return centred && share >= AppMapView.MAXIMIZED[keep ? 1 : 0];
	}

	// whether the controls of an HTML element stand in it, so its sketch makes way
	showsControls(d) {

		return this.editing === d && !!this.domEditor?.ready();
	}

	// ends editing, unless there are unsaved changes the user keeps; says whether it ended. While the question about them is
	// open, a second Escape or click asks nothing more
	async endEdit() {

		const editing = this.editing;

		if (!editing) {
			return true;
		}

		if (this.ending) {
			return false;
		}

		this.ending = true;

		try {

			if (!(await this.editorOf(editing).close())) {
				return false;
			}

		} finally {

			this.ending = false;
		}

		if (this.editing === editing) {
			this.editing = null;
		}

		// code closed by hand stays closed until it leaves the screen, see followCode
		if (!editing.page) {
			this.dismissed = editing;
		}

		this.requestDraw();

		return true;
	}

	// the editor stands on the field of the code it edits, in the size the map shows it; while the code is not readable, or
	// the hotspots show, it waits hidden
	placeEditor() {

		const n = this.editing;

		if (!n || !this.editorOf(n)) {
			return;
		}

		// the settings of an HTML element stand where it shows them, while its page is open
		if (n.page) {

			if (this.spotlight || !n.opened) {

				this.domEditor.hide();
				return;
			}

			const t    = this.transform;
			const area = n.opened.area;

			const tone = this.panels.toneOf(n);

			this.domEditor.place({ left: t.applyX(area.x), top: t.applyY(area.y), width: area.width * t.k, height: area.height * t.k }, n.opened.fit, this.look.dark, tone === this.look.trace ? this.look.kinds.dom : tone, Math.min(1, (n.opened.change - 0.5) * 2));

			return;
		}

		if (this.spotlight || !this.panels.reading.has(n)) {

			this.editor.hide();
			return;
		}

		const t                = this.transform;
		const panel            = this.panels.codePanel(n, t.k);
		const [fx, fy, fw, fh] = AppMapPanels.codeField(panel);

		this.editor.place({ left: t.applyX(fx), top: t.applyY(fy), width: fw * t.k, height: fh * t.k });
	}

	// the way into the Code area stands at the foot of the open card at the middle of the screen, on the right as the way
	// into the Pages area of an HTML element: the smallest one the middle lies in, a property before its type, else the one
	// nearest the middle; it comes and goes with the card
	placeOpenButton() {

		const button = this.openButton;

		if (!button) {
			return;
		}

		const t      = this.transform;
		const middle = [this.width / 2, this.height / 2];
		const cards  = this.spotlight ? [] : this.panels.cards
			.map(c => ({ ...c, screen: [t.applyX(c.rect[0]), t.applyY(c.rect[1]), c.rect[2] * t.k, c.rect[3] * t.k] }))
			.filter(({ screen: [x, y, w, h] }) => x < this.width && x + w > 0 && y < this.height && y + h > 0);
		const within = cards.filter(({ screen: [x, y, w, h] }) => middle[0] >= x && middle[0] <= x + w && middle[1] >= y && middle[1] <= y + h);
		const away   = c => Math.hypot(c.screen[0] + c.screen[2] / 2 - middle[0], c.screen[1] + c.screen[3] / 2 - middle[1]);
		const card   = within.sort((a, b) => a.screen[2] * a.screen[3] - b.screen[2] * b.screen[3])[0] ?? cards.sort((a, b) => away(a) - away(b))[0] ?? null;

		this.openCard = card;
		button.hidden = !card;

		if (!card) {
			return;
		}

		const [x, y, w, h] = card.screen;
		const foot         = card.foot * t.k;
		const tile         = card.item?.tile;
		const label        = card.open.area === 'security' ? 'Open in Security Area' : 'Open in Code Area';

		if (button.textContent !== label) {
			button.textContent = label;
		}

		button.style.opacity = String(card.opacity);
		button.style.fontSize = `${Math.min(12.5, foot * 0.28)}px`;
		button.style.left     = `${x + w - foot * 0.4}px`;
		button.style.top      = `${y + h - foot / 2}px`;
		// in the colour of the box it stands in; the box of a request opens in that of its endpoint, see AppMapPanels.drawBox
		button.style.setProperty('--page', (!tile?.access && this.panels.tintOf(this.renderer, tile?.tint)) || this.renderer.colorOf(card.n));
		button.classList.toggle('appmap-open-dark', this.look.dark);
	}

	// the data of the map loaded anew, after a change to the application: the elements keep their places, the view, the
	// trail, the lenses and what is edited stay; new elements are placed and settle, relations come and go
	replaceModel(model) {

		const trail   = this.trail.map(n => n.id);
		const pinned  = this.pinned?.id;
		const editing = this.editing?.page ? null : this.editing?.id;

		// the controls of an HTML element belong to the old trees; they open anew with the new data, see followElement
		if (this.editing?.page) {
			this.domEditor.dispose();
		}

		this.layout.stop();
		this.strands.cancel();

		this.model = model;
		this.nodes = model.nodes;
		this.byId  = model.byId;
		this.links = [...model.links];

		this.hovered        = null;
		this.hoveredDom     = null;
		this.hoveredStrand  = null;
		this.hoveredMarker  = null;
		this.expandedMarker = null;
		this.pinnedDom      = null;
		this.trail          = trail.map(id => this.byId.get(id)).filter(n => n);
		this.pinned         = this.byId.get(pinned) ?? null;
		this.editing        = this.byId.get(editing) ?? null;
		this.dismissed      = this.dismissed ? this.byId.get(this.dismissed.id) ?? null : null;
		this.found          = null;
		this.foundFor       = new Map();

		// what was edited is gone from the map
		if (editing && !this.editing) {
			this.editor.dispose();
		}

		this.rebuildIndex();

		this.layout  = new AppMapLayout(this);
		this.strands = new AppMapStrands(this);
		this.panels  = new AppMapPanels(this);

		this.renderer.recolor();
		this.setLens(this.accessRole ?? null);
		this.measure();

		// the view stays where it is, also when the layout moves something
		this.userZoomed = true;
		this.layout.start(moved => this.onSettled(moved));

		// the call traces name the elements by id, which are new objects now
		this.resolveTraces();

		// the closest zoom needs the elements in their places, which the layout restores as it starts; before that it would
		// fall back to ZOOM_IN and pull the view out of an open page
		this.zoom.scaleExtent([this.zoom.scaleExtent()[0], this.deepestZoom()]);
		this.showTrail();
		this.requestDraw();
	}

	pin(n) {

		if (this.trail[this.trail.length - 1] !== n) {
			this.trail.push(n);
		}

		// a call trace belongs to the element it failed in
		if (this.callTrace && this.callTrace.origin !== n) {
			this.callTrace = null;
		}

		this.pinned    = n;
		this.pinnedDom = null;
		this.showTrail();
		this.requestDraw();
	}

	// holds an HTML element of an open page; its page joins the trail, the element follows it there
	pinDom(d) {

		if (!this.panels.boxTargets(d).length) {
			return;
		}

		this.pin(d.page);
		this.pinnedDom = d;
		this.showTrail();
		this.requestDraw();
	}

	unpin() {

		this.pinned    = null;
		this.pinnedDom = null;
		this.callTrace = null;
		this.trail     = [];
		this.showTrail();
		this.requestDraw();
	}

	// ----- the global search: what it finds is marked on the map -----
	/**
	 * Shows a search of the global search on the map, or none for null: the elements whose name holds the text, and the
	 * elements its results lie in. A result that is no element of the map (an HTML element, a schema property) lies in the
	 * first of the elements the server names for it (see locate) that the map knows; a repeater or an action mapping marks
	 * its box in the page and the page.
	 */
	async search(search) {

		const run = ++this.searchRun;

		if (!search) {

			this.found = null;
			this.foundFor.clear();
			this.updateHint();
			this.requestDraw();

			return;
		}

		const { searchString, searchIgnoreCase } = search.data;
		const fold     = text => searchIgnoreCase ? String(text).toLowerCase() : String(text);
		const wanted   = fold(searchString);
		const known    = id => this.byId.get(id) ?? this.byId.get('view:' + id);
		const ids      = [...new Set(search.results.map(r => r.id))];
		const unknown  = ids.filter(id => !known(id) && !this.located.has(id));

		if (unknown.length && this.locate) {

			const located = await this.locate(unknown).catch(() => ({}));

			unknown.forEach(id => this.located.set(id, located[id] ?? []));
		}

		// a newer search, or none, has come in the meantime
		if (run !== this.searchRun || this.destroyed) {
			return;
		}

		const targetOf = id => known(id) ?? (this.located.get(id) ?? []).map(known).find(Boolean);
		const hits     = [...this.byId.values()].filter(n => (n.name != null && fold(n.name).includes(wanted)) || (n.routes ?? []).some(route => fold(route.path).includes(wanted)));
		const found    = { text: searchString, elements: new Set(), inner: new Set() };

		this.foundFor = new Map(ids.map(id => [id, targetOf(id)]).filter(([, n]) => n));

		for (const n of [...hits, ...this.foundFor.values()]) {

			const home = this.model.inner.get(n);

			if (home) {

				found.inner.add(n);
				found.elements.add(home);

			} else if (this.neighbours.has(n)) {

				found.elements.add(n);
			}
		}

		this.found = found;
		this.updateHint();
		this.requestDraw();
	}

	// goes to the element a result of the global search lies in and holds it; false when the result lies nowhere on the map
	goToResult(id) {

		const n    = this.foundFor.get(id);
		const home = n && this.model.inner.get(n);

		if (!n || (!home && !this.neighbours.has(n))) {
			return false;
		}

		this.pin(home ?? n);

		// inside a page: its box is held, and the page opens
		if (home) {

			const d = this.panels.trees.get(home)?.boxOf.get(n);

			if (d) {
				this.pinDom(d);
			}
		}

		this.glideInto(home ?? n);

		return true;
	}

	// ----- the access lens: what one role reaches -----
	// shows what a role reaches (see AppMapAccess), or nothing for null; role is { kind, id?, name }
	setLens(role) {

		this.accessRole = role;
		this.access     = role ? AppMapAccess.of(this.model, this.links, role) : null;
		this.updateHint();
		this.requestDraw();
	}

	// ----- the activity lens: how much the entry points are used, see Usage on the server -----
	// 'usage' for the number of requests or runs, or null
	setActivity(mode) {

		// the hotspots still showing keep the usage they need, and end it themselves (see setSpotlight)
		if (!mode && this.spotlight && this.activity) {

			this.spotlightLens = true;
			return;
		}

		// usage asked for itself outlasts the hotspots that had turned it on
		if (mode) {
			this.spotlightLens = false;
		}

		this.activity = AppMapView.ACTIVITY[mode] ? { mode } : null;
		this.measure();
		this.watchActivity();
	}

	// the hotspots, while the pointer rests on their button: the map steps back into grey, the glow of the activity and the
	// names of the most used elements stay; without the usage showing, it shows meanwhile
	setSpotlight(on) {

		this.spotlight = on;

		if (on && !this.activity) {

			this.spotlightLens = true;
			this.setActivity('usage');
		}

		if (!on && this.spotlightLens) {

			this.spotlightLens = false;
			this.setActivity(null);
		}

		this.requestDraw();
	}

	// the heat of the activity from the numbers the elements carry now
	measure() {

		if (this.activity) {
			this.activity = { mode: this.activity.mode, ...this.heatOf(n => n.requests != null, AppMapView.ACTIVITY[this.activity.mode]) };
		}

		this.updateHint();
		this.requestDraw();
	}

	// while the activity shows, its numbers are fetched fresh: at once when it comes on, then every ACTIVITY_REFRESH
	// milliseconds; the map itself is not loaded again
	watchActivity() {

		const on = !!this.activity && !!this.loadActivity;

		if (on && this.activityTimer == null) {

			this.refreshActivity();
			this.activityTimer = setInterval(() => this.refreshActivity(), AppMapView.ACTIVITY_REFRESH);

		} else if (!on && this.activityTimer != null) {

			clearInterval(this.activityTimer);
			this.activityTimer = null;
		}
	}

	static ACTIVITY_REFRESH = 30000;

	// the size of the text on a card opened with a double click, in pixels: the names of its rows, and code; a card taller
	// than the screen stands this many pixels below its top edge
	static CARD_TEXT = 16;
	static CODE_TEXT = 13;
	static CARD_TOP  = 40;

	// the rows of the tree of a page opened with a double click are this many pixels high, which gives the names of its
	// elements about the size of code
	static PAGE_ROW  = 36;

	async refreshActivity() {

		const fresh = await this.loadActivity();

		if (!fresh || this.destroyed) {
			return;
		}

		for (const n of this.nodes) {

			n.requests = fresh[n.id]?.requests;
		}

		this.measure();
	}

	// how often the grants decided a request ('grants') or the pages were viewed ('pages') in the last days, as a promise of
	// { days, usage } or null; read at most every ACTIVITY_REFRESH milliseconds, and kept in usage[kind] for the tooltip,
	// which cannot wait for it
	usageOf(kind) {

		const known = this.usage[kind];

		if (!this.loadUsage) {
			return Promise.resolve(null);
		}

		if (!known || Date.now() - known.time > AppMapView.ACTIVITY_REFRESH) {

			const loaded = this.loadUsage(kind).catch(() => null);

			this.usage[kind] = { time: Date.now(), loaded, data: known?.data ?? null };

			loaded.then(data => {

				if (this.usage[kind]?.loaded === loaded && data) {

					this.usage[kind].data = data;
					this.requestDraw();
				}
			});

			return loaded;
		}

		return known.loaded;
	}

	// the views of a page in words, from what usageOf last read: how many in how many days, and when the last was
	describePageViews(n) {

		// read again for the next time where it is old
		this.usageOf('pages');

		const data = this.usage.pages?.data;

		if (!data) {
			return null;
		}

		const use  = data.usage?.[n.id];
		const days = `in the last ${data.days} ${data.days === 1 ? 'day' : 'days'}`;

		return use ? `${use.total.toLocaleString()} ${use.total === 1 ? 'view' : 'views'} ${days}, the last on ${new Date(use.lastUsed).toLocaleString()}` : `no views ${days}`;
	}

	// what the activity lens shows of an element: its number of requests or runs
	static ACTIVITY = { usage: n => n.requests };

	// the use of an element in words: a scheduled method runs, anything else is requested
	static describeUse(n) {

		const count = n.requests;

		return `${count.toLocaleString()} ` + (n.cron ? (count === 1 ? 'run' : 'runs') : (count === 1 ? 'request' : 'requests'));
	}

	/**
	 * Every measured element with a heat between 0 and 1 by its rank, not by its value: the square of its percentile, so in
	 * any application, quiet or under heavy load, only the top stands out and the lower half stays faint; the numbers
	 * themselves are in the tooltip. As { heat, top }, top the element of the highest value.
	 */
	heatOf(measured, value) {

		const nodes  = this.nodes.filter(measured);
		const sorted = nodes.map(value).sort((a, b) => a - b);
		const rank   = v => {

			// equal values share the middle of their ranks
			const first = d3.bisectLeft(sorted, v);
			const last  = d3.bisectRight(sorted, v) - 1;

			return sorted.length > 1 ? (first + last) / 2 / (sorted.length - 1) : 1;
		};

		return { heat: new Map(nodes.map(n => [n, rank(value(n)) ** 2])), top: nodes.reduce((a, b) => value(b) > value(a) ? b : a, nodes[0]) };
	}

	// the activity in one line, for the line below the map
	describeActivity() {

		const { heat, top } = this.activity;

		return !top ? 'Nothing used since the start' : `${heat.size.toLocaleString()} elements used since the start · most used: ${top.name} with ${AppMapView.describeUse(top)}`;
	}

	// the lens in one line, for the line below the map
	describeLens() {

		const access  = this.access;
		const pages   = this.nodes.filter(n => n.kind === 'page');
		const seen    = pages.filter(n => access.pages.get(n)).length;
		const types   = [...access.rights.values()];
		const count   = right => types.filter(own => own.has(right)).length;
		const gaps    = access.gaps.length;

		return `${access.role.name} see ${seen} of ${pages.length} pages · of ${access.governed.size} types governed by the schema may read ${count('r')}, write ${count('w')}, delete ${count('d')}` + (gaps ? ` · ${gaps} ${gaps === 1 ? 'gap' : 'gaps'} in red: a page they see does what they may not` : '');
	}

	// the start page: the page of the lowest position, among those the role of the lens sees
	startPage() {

		const pages = this.nodes.filter(n => n.kind === 'page' && n.position != null && (!this.access || this.access.pages.get(n)));

		return pages.length ? pages.reduce((a, b) => b.position < a.position ? b : a) : null;
	}

	// glides to an element and holds its focus: at the current zoom, unless that is too close for it, as it is after the card
	// of another element filled the screen; an element with inner structure, a method or a page, then shows as a double
	// click shows it (see glideInto), any other only as close as EXPLORE_SIZE allows, so that its relations stay in sight
	explore(n) {

		const inner  = AppMapPanels.hasCode(n) || this.panels.trees.has(n);
		const [w, h] = AppMapRenderer.boxOf(n) ?? [1, 1];
		const close  = inner ? this.innerZoom(n) : this.zoomWhere(k => 2 * Math.max(w, h) * this.drawnRadiusAt(n, k) * k, AppMapView.EXPLORE_SIZE * Math.min(this.width, this.height));

		this.pin(n);
		this.glide(n.x, n.y, Math.min(this.transform.k, close));
	}

	// the zoom at which glideInto shows a method or a page: its box filling the screen, a page at least open
	innerZoom(n) {

		const tree = this.panels.trees.get(n);

		if (!tree || AppMapPanels.hasCode(n)) {
			return this.fillZoom(n);
		}

		// the rows of its tree at PAGE_ROW, as the text of every other card has one size (see fillZoom), whatever the size of
		// the tree; a page with a large tree is then larger than the screen (see glideInto)
		return this.zoomWhere(k => this.panels.pagePanel(n, tree, k, this.drawnRadiusAt(n, k)).row, AppMapView.PAGE_ROW);
	}

	// the way exploring took, as a row of names above the map: a name leads back there, the cross ends exploring
	showTrail() {

		const bar = this.trailBar;

		bar.replaceChildren();
		bar.hidden = !this.trail.length;

		this.trail.forEach((n, i) => {

			const button = document.createElement('button');

			button.textContent = n.name;
			button.title       = AppMapModel.KIND_LABEL[n.kind];
			button.style.setProperty('--kind', this.look.kinds[n.kind] ?? '#888888');
			button.setAttribute('aria-current', String(n === this.pinned && !this.pinnedDom));
			button.addEventListener('click', () => {

				this.trail = this.trail.slice(0, i);
				this.explore(n);
			});

			bar.append(button);
		});

		// the HTML element held, after its page; it leads nowhere itself
		if (this.pinnedDom) {

			const box = document.createElement('button');

			box.textContent = AppMapPanels.nameOf(this.pinnedDom);
			box.title       = (this.pinnedDom.data.inner ?? []).map(m => m.name).join(', ');
			box.disabled    = true;
			box.style.setProperty('--kind', this.look.kinds.dom ?? '#888888');
			box.setAttribute('aria-current', 'true');

			bar.append(box);
		}

		if (this.trail.length) {

			const close = document.createElement('button');

			close.textContent = '×';
			close.title       = 'End exploring (Esc)';
			close.className   = 'appmap-trail-close';
			close.addEventListener('click', () => this.unpin());

			bar.append(close);
		}
	}

	// a short line on what can be done next, for the line below the map
	updateHint() {

		const open = this.panels.reading.size || this.panels.inspecting.size || [...this.panels.trees.values()].some(t => t.open);
		const hint = !this.nodes.length ? ''
			: this.access && !this.pinned ? this.describeLens()
			: this.activity && !this.pinned ? this.describeActivity()
			: this.found ? `${this.found.elements.size ? this.found.elements.size : 'No'} ${this.found.elements.size === 1 ? 'element matches' : 'elements match'} “${this.found.text}” · click a result of the search to go there`
			: this.callTrace ? this.describeCallTrace()
			: this.pinned ? 'Click a name at the end of a relation to go there · Esc or a click on the empty map ends exploring'
			: this.editing?.page ? `${AppMapView.SAVE_KEY} saves the element · Esc discards its changes · zoom out to leave it`
			: open && this.domEditor ? 'Zoom into an element or double-click it to change it, click code or a template to edit it · double-click the empty map to see the whole map again'
			: open ? 'Double-click the empty map to see the whole map again'
			: this.zoomTier() === 0 ? 'Hover an element to see what it connects to · click it to explore from there'
			: 'Double-click a page, method or type to open it · drag to pan, the wheel zooms';

		if (hint !== this.hint) {

			this.hint = hint;
			this.onHint?.(hint);
		}
	}

	// ----- interaction: the map reacts to panning, zooming and hovering -----
	bindEvents() {

		this.zoom = d3.zoom().scaleExtent([0.1, this.deepestZoom()]).on('zoom', e => {

			// only a gesture of the user counts, not fitting the map to the screen
			if (e.sourceEvent) {

				this.userZoomed = true;
				this.drag.push([performance.now(), e.transform.x, e.transform.y]);
			}

			this.transform = e.transform;
			this.growth    = this.growthAt(e.transform.k);
			this.requestDraw();

		}).on('start', e => {

			// grabbing the map stops it where it is, and while it is held no glide moves it, see glide
			if (e.sourceEvent) {

				this.stopCoast();
				this.drag     = [];
				this.grabbing = true;
			}

		}).on('end', e => {

			if (e.sourceEvent) {

				this.grabbing = false;
				this.coast();
			}
		});

		d3.select(this.canvas)
			.call(this.zoom)
			.on('dblclick.zoom', null)
			.on('wheel.zoom', e => this.onWheel(e), { passive: false })
			.on('dblclick', e => this.onDoubleClick(e))
			.on('click', e => this.onClick(e))
			.on('mousemove', e => this.onPointerMove(e))
			.on('mouseleave', () => this.onPointerLeave());

		// Escape ends editing code, then exploring; the controls of an open HTML element stay as long as it is open, and
		// Escape discards their unsaved changes: they end, and open anew with what is saved (see followElement). An Escape
		// the editor used itself, to close its suggestions, ends nothing, and the editor stops it on its way, so only one
		// that arrives here unused counts. Editing ends when the key is released: the question about unsaved changes takes
		// the focus, and a release in it would answer it at once
		this.onKey = e => {

			if (e.key !== 'Escape') {
				return;
			}

			if (e.type === 'keydown') {

				this.escapeFree = !e.defaultPrevented;

				if ((!this.editing || (this.editing.page && !this.domEditor.isDirty())) && this.escapeFree && this.pinned) {
					this.unpin();
				}

			} else {

				// every release ends what its key began, so an earlier Escape says nothing about the next
				const free      = this.escapeFree;
				this.escapeFree = false;

				if (this.editing && (!this.editing.page || this.domEditor.isDirty()) && free) {
					this.endEdit();
				}
			}
		};

		this.escapeFree = false;

		document.addEventListener('keydown', this.onKey);
		document.addEventListener('keyup', this.onKey);

		this.resizeObserver = new ResizeObserver(() => {

			this.resize();
			this.fitAll();
		});

		this.resizeObserver.observe(this.container);
	}

	// the same amount per notch as d3-zoom, but eased towards: lines in Firefox, pixels elsewhere, a pinch on a touchpad sends ctrl
	onWheel(e) {

		e.preventDefault();

		// the wheel takes over from a glide or a coast under way
		d3.select(this.canvas).interrupt();
		this.stopCoast();

		const delta    = -e.deltaY * (e.deltaMode === 1 ? 0.05 : e.deltaMode ? 1 : 0.002) * (e.ctrlKey ? 10 : 1);
		const [lo, hi] = this.zoom.scaleExtent();

		// while the editor of code or of a template stands in its element on the screen, or a leaf of the map stands open at
		// its full size in the middle (see atLeaf), the map only zooms out, to leave it: there is nothing further in. An
		// editor kept for its unsaved changes after zooming out (see followElement and followCode) does not hold the map, so
		// that its element can be zoomed into again
		const filled = this.editing && (!this.editing.page || AppMapPanels.isSheet(this.editing)) && this.fills(this.editing, true);
		const editor = filled || (delta > 0 && this.atLeaf());
		const top    = editor ? Math.min(hi, this.transform.k) : hi;

		if (editor && delta > 0) {
			return;
		}

		// zooming in stops where an element with an editor fills the screen, and goes on past it only with the next turn
		const target = Math.min(top, Math.max(lo, (this.wheelK ?? this.transform.k) * Math.pow(2, delta)));

		this.wheelK     = delta > 0 ? this.fillStop(target) : target;
		this.wheelAt    = d3.pointer(e, this.canvas);
		this.userZoomed = true;

		if (!this.wheelFrame) {

			this.wheelLast  = performance.now();
			this.wheelFrame = requestAnimationFrame(now => this.wheelStep(now));
		}
	}

	wheelStep(now) {

		this.wheelFrame = null;

		if (this.destroyed || this.wheelK === null) {
			return;
		}

		// eased in log space, so zooming in and out feel alike
		const k     = this.transform.k;
		const share = 1 - Math.exp(-Math.max(0, now - this.wheelLast) / AppMapView.WHEEL_EASE_MS);
		const done  = Math.abs(Math.log(this.wheelK / k)) * (1 - share) < 0.002;
		const next  = done ? this.wheelK : k * Math.pow(this.wheelK / k, share);

		this.wheelLast = now;

		d3.select(this.canvas).call(this.zoom.scaleTo, next, this.wheelAt);

		if (done) {
			this.wheelK = null;
		} else {
			this.wheelFrame = requestAnimationFrame(t => this.wheelStep(t));
		}
	}

	// the closest zoom: ZOOM_IN, or where the boxes of the HTML elements of the page with the smallest ones are ZOOM_IN_BOX
	// pixels high; close up the elements have the size of the layout, so the scale of a tree no longer changes there
	deepestZoom() {

		const scales = [...this.panels.trees].map(([page, tree]) => this.panels.pagePanel(page, tree, 1, this.radiusOf(page)).scale);
		const finest = d3.min(scales.filter(s => s > 0)) ?? 0;

		return Math.max(AppMapView.ZOOM_IN, finest ? AppMapView.ZOOM_IN_BOX / (AppMapPanels.BOX * finest) : 0);
	}

	// how much larger than in the layout the elements are drawn at a zoom, see SIZE_ZOOM
	growthAt(k) {

		return this.baseK ? Math.min(AppMapView.MAX_GROWTH, Math.max(1, Math.sqrt(AppMapView.SIZE_ZOOM * this.baseK / k))) : 1;
	}

	// whether the last click set off a glide that a double click may still belong to: within DOUBLE_CLICK_MS
	gliding() {

		return !!this.glidingFrom && performance.now() - this.glidingFrom.at < AppMapView.DOUBLE_CLICK_MS;
	}

	static DOUBLE_CLICK_MS = 600;

	/**
	 * A double click glides to the inner structure of the element under the pointer: a page or shared component opens
	 * with its tree filling the screen, a method with its code, a type with its properties, any other element shows its
	 * grouping; on the empty map the whole map comes back. The element is held in focus as by a click, so its jump markers
	 * show. Only the view moves: the places of the elements stay exactly where they are.
	 */
	onDoubleClick(e) {

		e.preventDefault();

		// a first click that set off a glide moved the map under the pointer: the double click leads where that click did,
		// into its template or code, not into whatever has meanwhile come under the pointer
		if (this.gliding()) {

			const { dom, code } = this.glidingFrom;

			if (dom) {

				this.pinDom(dom);
				this.glideIntoElement(dom);

			} else {

				this.glideInto(code);
			}

			return;
		}

		const [px, py] = d3.pointer(e, this.canvas);
		const [wx, wy] = this.transform.invert([px, py]);
		const dom      = this.panels.findDom(wx, wy);
		const n        = dom ? dom.page : (this.panels.openAt(wx, wy) ?? this.find(px, py));

		// readable code glides to its element, as a click on it does
		const code = this.editor && this.panels.codeAt(wx, wy, this.transform.k);

		if (code) {

			this.glideInto(code);
			return;
		}

		// a property of an open type opens as its tile comes closer, as an HTML element does in its page
		const tile = this.panels.tileAt(wx, wy);

		if (tile) {

			this.glideIntoTile(tile);
			return;
		}

		// the element opened is the one explored from: its jump markers lead on; inside an open page the HTML element stays held
		if (dom) {
			this.pinDom(dom);
		} else if (n) {
			this.pin(n);
		}

		// an HTML element opens with its controls, as a page opens with its tree
		if (dom && this.domEditor) {
			this.glideIntoElement(dom);
		} else if (n) {
			this.glideInto(n);
		} else {
			this.glide(...this.fitTransform());
		}
	}

	// switches a setting in an open box: at once on the map, then in the database (see the option toggle); where that fails,
	// it switches back. A required field also changes its dot
	async flip({ item, index, key }) {

		const tile    = item.tile;
		const setting = tile.settings[index];
		const value   = !setting[1];
		const apply   = v => {

			setting[1] = v;

			if (key === 'notNull') {
				tile.dot = v;
			}

			this.requestDraw();
		};

		if (this.flipping) {
			return;
		}

		this.flipping = true;
		apply(value);

		try {

			if (!(await this.toggle({ ...tile.edit, key, label: setting[0], value }))) {
				apply(!value);
			}

		} catch (error) {

			console.warn('Application map: a setting could not be changed', error);
			apply(!value);

		} finally {

			this.flipping = false;
		}
	}

	// switches a cell of the matrix of the permission of a request in its open box (see AppMapPanels.switchAccess): at once
	// on the map, in every request of the card the permission allows, then in the database (see the option toggle), after
	// asking where the change opens more than the cell; back where that fails
	async flipAccess({ item, n, key, column, state, label }) {

		const grant  = item.tile.grant;
		const before = { flags: grant.flags ?? 0, visibleToPublicUsers: !!grant.visibleToPublicUsers, visibleToAuthenticatedUsers: !!grant.visibleToAuthenticatedUsers };
		const { question, ...after } = AppMapPanels.switchAccess(grant, key, column, state !== 'yes');
		const rows   = (this.panels.insides.get(n)?.sections ?? []).flatMap(s => s.items).filter(row => row.grant === grant);
		const apply  = values => {

			Object.assign(grant, values);
			rows.forEach(AppMapPanels.updateRequestRow);
			this.requestDraw();
		};

		if (this.flipping) {
			return;
		}

		this.flipping = true;
		apply(after);

		try {

			if (!(await this.toggle({ kind: 'access', id: grant.id, label, values: after, question }))) {
				apply(before);
			}

		} catch (error) {

			console.warn('Application map: a resource access permission could not be changed', error);
			apply(before);

		} finally {

			this.flipping = false;
		}
	}

	// glides to the box of a row, the square before its name, so that it stands open in the middle of the screen at its
	// full size, see tileZoom
	glideIntoTile(tile, ms = AppMapView.GLIDE_MS) {

		const [x, y, w, h] = tile.item.box;
		const width        = AppMapPanels.openWidth(tile.item, h);

		// the middle of the box as it stands open, which is wider than its square (see AppMapPanels.openWidth and openLeft)
		this.glide(AppMapPanels.openLeft(tile.item, x, w, width) + width / 2, y + h / 2, this.tileZoom(tile.item), ms);
	}

	// the zoom at which the box of a row stands open at its full size: as high as what it shows needs, its header, its
	// settings at the largest size of their text (see TILE_TEXT and AppMapPanels.drawBox) and its foot, and at least a little
	// more than TILE_OPEN; the box keeps its size on the map whatever the zoom
	tileZoom(item) {

		return AppMapPanels.openHeight(item.tile) / item.box[3];
	}

	// the boxes of properties drawn in the last frame whose middle lies near the middle of the screen
	tilesNearMiddle() {

		const t = this.transform;

		return this.panels.tiles.filter(({ x, y, width, height }) => Math.hypot(t.applyX(x + width / 2) - this.width / 2, t.applyY(y + height / 2) - this.height / 2) < Math.min(this.width, this.height) / 4);
	}

	// glides so close to an HTML element that it opens with its controls at ELEMENT_FILL of their size, with the controls in
	// the middle of the screen; a long box may then reach beyond it
	glideIntoElement(d, ms = AppMapView.GLIDE_MS) {

		// a template fills the screen with its sheet, as a method does with its box
		if (AppMapPanels.isSheet(d)) {

			const k     = this.fillZoom(d);
			const sheet = this.panels.elementPanel(d, k);

			this.glide(sheet.x + sheet.width / 2, sheet.y + sheet.height / 2, k, ms);

			return;
		}

		const k    = this.zoomWhere(k => this.panels.elementPanel(d, k).fit, AppMapView.ELEMENT_FILL);
		const area = this.panels.elementPanel(d, k).area;

		this.glide(area.x + area.width / 2, area.y + area.height / 2, k, ms);
	}

	// glides so close to an element that it shows what it holds: the code of a method, the tree of a page, the elements of
	// a grouping
	glideInto(n, ms = AppMapView.GLIDE_MS) {

		// a method, or a page of a single template: its box fills the screen and shows its code
		if (AppMapPanels.hasCode(n)) {

			this.panels.requestCode(n);
			this.glide(n.x, n.y, this.fillZoom(n), ms);

			return;
		}

		// a type, a REST endpoint, a group or an external service: its card at the size of its text (see fillZoom), in the
		// middle, or with its top on the screen where it is taller; the card is only as high as what it holds, so the glide
		// waits until that is there
		if (AppMapPanels.INSIDE_KINDS.has(n.kind)) {

			this.panels.requestInside(n).then(() => {

				const k     = this.fillZoom(n);
				const panel = this.panels.insidePanel(n, k, this.drawnRadiusAt(n, k));
				const tall  = panel.height * k > AppMapView.GLIDE_FILL * this.height;

				this.glide(n.x, tall ? panel.y0 + (this.height / 2 - AppMapView.CARD_TOP) / k : n.y, k, ms);
			});

			return;
		}

		// a page as large as the screen allows, and at least so close that its tree opens
		// a page at the size of the rows of its tree (see innerZoom), in the middle, or, where it is larger than the screen,
		// as a document is read: from its top left corner, with its name and its strip on the screen
		if (this.panels.trees.has(n)) {

			const k     = this.innerZoom(n);
			const panel = this.panels.pagePanel(n, this.panels.trees.get(n), k, this.drawnRadiusAt(n, k));
			const wide  = 2 * panel.hw * k > AppMapView.GLIDE_FILL * this.width;
			const tall  = 2 * panel.hh * k > AppMapView.GLIDE_FILL * this.height;

			this.glide(wide ? n.x - panel.hw + (this.width / 2 - AppMapView.CARD_TOP) / k : n.x, tall ? n.y - panel.hh + (this.height / 2 - AppMapView.CARD_TOP) / k : n.y, k, ms);

			return;
		}

		const members = this.nodes.filter(m => this.groupKeyOf(m) === this.groupKeyOf(n));

		if (members.length > 1) {

			// the outline of the grouping with the elements at their size close up
			const { x, y, r } = AppMapRenderer.circleOf(members, m => this.radiusOf(m));

			this.glide(x, y, AppMapView.GLIDE_FILL * Math.min(this.width, this.height) / (2 * r));

		} else {

			this.glide(n.x, n.y, this.transform.k);
		}
	}

	// after a drag the map runs on with the speed the pointer had when it let go, and slows down
	coast() {

		const now    = performance.now();
		const recent = this.drag.filter(([t]) => now - t <= AppMapView.DRAG_WINDOW);

		this.drag = [];

		if (recent.length < 2) {
			return;
		}

		const [t0, x0, y0] = recent[0];
		const [t1, x1, y1] = recent[recent.length - 1];
		const dt           = Math.max(1, t1 - t0);
		let vx             = (x1 - x0) / dt;
		let vy             = (y1 - y0) / dt;
		let last           = now;

		if (Math.hypot(vx, vy) < AppMapView.COAST_FROM) {
			return;
		}

		const step = time => {

			const elapsed = Math.max(0, time - last);
			const decay   = Math.exp(-elapsed / AppMapView.COAST_MS);

			last = time;

			// the distance the speed covers while it decays over this frame
			const dx = vx * AppMapView.COAST_MS * (1 - decay);
			const dy = vy * AppMapView.COAST_MS * (1 - decay);

			vx *= decay;
			vy *= decay;

			d3.select(this.canvas).call(this.zoom.translateBy, dx / this.transform.k, dy / this.transform.k);

			this.coastFrame = !this.destroyed && Math.hypot(vx, vy) > 0.01 ? requestAnimationFrame(step) : null;
		};

		this.coastFrame = requestAnimationFrame(step);
	}

	stopCoast() {

		if (this.coastFrame) {

			cancelAnimationFrame(this.coastFrame);
			this.coastFrame = null;
		}
	}

	// the zoom at which a size on screen that grows with the zoom reaches the given number of pixels, by bisection
	zoomWhere(size, pixels) {

		let [lo, hi] = this.zoom.scaleExtent();

		for (let i = 0; i < 50; i++) {

			const mid = Math.sqrt(lo * hi);

			if (size(mid) < pixels) {
				lo = mid;
			} else {
				hi = mid;
			}
		}

		return hi;
	}

	// a smooth flight to the given point of the map at the given zoom (van Wijk and Nuij, as d3 interpolates zoom transitions)
	glide(x, y, k, ms = AppMapView.GLIDE_MS) {

		// the hand of the user leads: a glide would move the map under the pointer, and the speed the map coasts on with
		// would include it
		if (this.grabbing) {
			return;
		}

		const [lo, hi] = this.zoom.scaleExtent();
		const scale    = Math.min(hi, Math.max(lo, k));

		this.wheelK     = null;
		this.userZoomed = true;

		this.stopCoast();

		d3.select(this.canvas)
			.interrupt()
			.transition()
			.duration(ms)
			.call(this.zoom.transform, d3.zoomIdentity.translate(this.width / 2, this.height / 2).scale(scale).translate(-x, -y));
	}

	resize() {

		const dpr = window.devicePixelRatio || 1;

		this.width  = this.container.clientWidth;
		this.height = this.container.clientHeight;

		this.canvas.width        = this.width * dpr;
		this.canvas.height       = this.height * dpr;
		this.canvas.style.width  = `${this.width}px`;
		this.canvas.style.height = `${this.height}px`;

		this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
		this.requestDraw();
	}

	fitAll() {

		if (!this.width || !this.height) {
			return;
		}

		const [x, y, k] = this.fitTransform();

		this.baseK  = k;
		this.wheelK = null;
		this.zoom.scaleExtent([k * AppMapView.ZOOM_OUT, this.deepestZoom()]);

		d3.select(this.canvas).interrupt().call(this.zoom.transform, d3.zoomIdentity.translate(this.width / 2, this.height / 2).scale(k).translate(-x, -y));
	}

	// the centre and the zoom at which the whole map fits the screen
	fitTransform() {

		// the elements and the names of the areas around them, which keep their size on screen: a margin of pixels, and
		// room for a name that moves out because its neighbour stands in the way
		const names  = this.renderer.layoutCache().names * 1.1;
		const points = [...this.nodes.map(n => [n.x, n.y]), ...[...this.layout.sectors.values()].map(s => AppMapLayout.point(s.angle, names))];
		const x0     = d3.min(points, p => p[0]);
		const x1     = d3.max(points, p => p[0]);
		const y0     = d3.min(points, p => p[1]);
		const y1     = d3.max(points, p => p[1]);
		const k      = Math.min((this.width - 2 * AppMapView.FIT_MARGIN[0]) / ((x1 - x0) || 1), (this.height - 2 * AppMapView.FIT_MARGIN[1]) / ((y1 - y0) || 1));

		return [(x0 + x1) / 2, (y0 + y1) / 2, k];
	}

	// how close the map is, in three steps: from afar (0) the strands are bands that do not answer the pointer, in the
	// middle (1) a band answers it, close up (2) the bands give way to the single relations
	zoomTier() {

		const ratio = this.transform.k / this.baseK;

		return ratio < 1.8 ? 0 : (ratio < 4 ? 1 : 2);
	}

	// an element answers wherever the pointer is over it, and while it is small around it (see MIN_TARGET); where elements
	// overlap, the one whose edge is closest wins, so a small element in front of a large one stays reachable. Within the
	// outline of a grouping whose anchor is small, the anchor answers, the type of its methods; on the empty map nothing does
	find(px, py) {

		const k      = this.transform.k;
		const [x, y] = this.transform.invert([px, py]);
		const small  = n => this.shownRadius(n) * k < AppMapView.SMALL;
		let best     = null;
		let bestGap  = Infinity;

		if (this.panels.covers(x, y)) {
			return null;
		}

		for (const n of this.nodes) {

			const r      = Math.max(this.shownRadius(n), 2 / k);
			const box    = AppMapRenderer.boxOf(n);
			const gap    = box ? AppMapView.boxGap(x - n.x, y - n.y, r * box[0], r * box[1]) : Math.hypot(n.x - x, n.y - y) - r;
			const margin = Math.max(0, AppMapView.MIN_TARGET / k - r, n.kind === 'type' && small(n) ? r * AppMapView.TYPE_MARGIN : 0);

			if (gap <= margin && gap < bestGap) {

				best    = n;
				bestGap = gap;
			}
		}

		if (!best) {

			const around = this.renderer.groupingCircles().filter(c => c.anchor && this.neighbours.has(c.anchor) && small(c.anchor) && Math.hypot(c.x - x, c.y - y) <= c.r);

			best = around.sort((a, b) => a.r - b.r)[0]?.anchor ?? null;
		}

		return best;
	}

	// how far a point lies outside a box of the given half width and height around the origin; inside, how far from its edge,
	// negative like the gap of a circle
	static boxGap(dx, dy, w, h) {

		const outX = Math.abs(dx) - w;
		const outY = Math.abs(dy) - h;

		return outX > 0 || outY > 0 ? Math.hypot(Math.max(outX, 0), Math.max(outY, 0)) : Math.max(outX, outY);
	}

	// what the tooltip says about an element: its kind, and what Structr does with it
	describe(n) {

		const parts = [AppMapModel.KIND_LABEL[n.kind]];

		if (n.public) {
			parts.push('public');
		}

		if (n.cron) {
			parts.push(`scheduled: ${n.cron}`);
		}

		// how large it is in its own terms, as the server measures it
		if (n.size) {
			parts.push(`${n.size} ${{ type: 'properties', method: 'lines of code', page: 'HTML elements', component: 'HTML elements' }[n.kind] ?? 'parts'}`);
		}

		// how a page is reached and what it is for
		parts.push(...this.describeRouting(n));

		// how often it was requested or ran since the start
		if (n.requests != null) {
			parts.push(AppMapView.describeUse(n) + ' since the start');
		}

		// how often a page was viewed in the last days, which outlives a restart
		if (n.kind === 'page') {

			const views = this.describePageViews(n);

			if (views) {
				parts.push(views);
			}
		}

		// what deleting it takes along, and what it passes on
		if (n.kind === 'type') {
			parts.push(...this.describeSchema(n));
		}

		// what the role of the lens may do with it
		if (this.access) {

			const text = AppMapAccess.describe(this.access, n);

			if (text) {
				parts.push(text);
			}
		}

		// the section of pages it belongs to
		const section = this.model.sections.get(this.groupKeyOf(n));

		if (section) {
			parts.push(`in the section ${section.name}`);
		}

		// why a page or component has its size, see AppMapModel.importance
		const importance = this.importance.get(n);

		if (importance) {
			parts.push(AppMapView.describeImportance(n, importance));
		}

		const hotspot = this.hotspotOf(n);

		if (hotspot) {

			const module = this.analysis.moduleOf.get(n);

			parts.push(`hotspot ${hotspot.place} of ${this.analysis.hotspots.size}, used by ${hotspot.users} groupings`);

			if (hotspot.changedBy.length) {
				parts.push(`changed by ${hotspot.changedBy.map(x => x.name).join(', ')}`);
			}

			if (hotspot.effects.length) {
				parts.push(`then ${hotspot.effects.join(', ')}`);
			}

			parts.push(`module ${module.name}: ${module.members.length} groupings`);
		}

		if (n.rest) {

			const whom = [n.rest.public ? 'anonymous' : null, n.rest.authenticated ? 'authenticated' : null].filter(Boolean).join(' and ');

			parts.push(`REST: ${n.rest.rules} resource access ${n.rest.rules === 1 ? 'grant' : 'grants'}${whom ? ` for ${whom} users` : ''}`);
		}

		return parts.join(' · ');
	}

	// the URL routes of a page, whether it is the start page or an error page, what it delivers and the sites it is on
	describeRouting(n) {

		const parts = [];

		for (const route of n.routes ?? []) {
			parts.push(`URL ${route.path}${route.currentType ? ` opens a ${route.currentType}` : ''}${route.warnings ? ` (${route.warnings} ${route.warnings === 1 ? 'warning' : 'warnings'})` : ''}`);
		}

		if (n === this.startPage()) {
			parts.push('the start page');
		}

		if (n.errorCodes) {
			parts.push(`shown for the errors ${n.errorCodes}`);
		}

		if (n.contentType) {
			parts.push(`delivers ${n.contentType}`);
		}

		const sites = this.model.sites.filter(site => site.pages.includes(n.id)).map(site => site.name);

		if (sites.length) {
			parts.push(`on ${sites.length === 1 ? 'the site' : 'the sites'} ${sites.join(', ')}`);
		}

		return parts;
	}

	// what deleting an object of a type deletes with it, and to whom the type passes on rights
	describeSchema(n) {

		const parts   = [];
		const cascade = [...AppMapAccess.cascadeOf(this.links, n)];
		const certain = cascade.filter(([, step]) => !step.maybe).map(([type]) => type.name);
		const maybe   = cascade.filter(([, step]) => step.maybe).map(([type]) => type.name);

		if (certain.length) {
			parts.push(`deleting one deletes its ${certain.join(', ')}`);
		}

		if (maybe.length) {
			parts.push(`deleting one deletes the ${maybe.join(', ')} that would become invalid`);
		}

		// the types it passes rights on to, by the rights they get
		const passes = d3.groups(AppMapAccess.propagation(this.links).filter(hop => hop.from === n), ({ l }) => AppMapAccess.RIGHTS.filter(right => l.details[AppMapAccess.RIGHT_LABEL[right]] === 'Add').map(right => AppMapAccess.RIGHT_LABEL[right]).join(', '));

		for (const [rights, hops] of passes.filter(([rights]) => rights)) {
			parts.push(`passes ${rights} on to ${hops.map(({ to }) => to.name).join(', ')}`);
		}

		return parts;
	}

	// what holds a grouping together, for the tooltip on its name: a section of pages by its category or the type its pages
	// work with, any other grouping by its anchor and what belongs to it
	describeGrouping(key) {

		const members = this.nodes.filter(m => this.groupKeyOf(m) === key);
		const section = this.model.sections.get(key);
		const count   = (n, one) => `${n} ${one}${n === 1 ? '' : 's'}`;

		if (section) {

			const pages = `${members.length} pages and shared components`;
			const why   = section.how === 'category'
				? `that the developer gave the category ${section.name}`
				: `that work with ${this.byId.get(key.slice('section:type:'.length))?.name ?? 'one type'} more than with any other type`;

			return `A section of the user interface: ${pages} ${why}, and what only they hold or lead to.`;
		}

		const anchor = this.byId.get(key);
		const kinds  = d3.rollups(members.filter(m => m !== anchor), v => v.length, m => AppMapModel.KIND_LABEL[m.kind].toLowerCase());

		return `${AppMapModel.KIND_LABEL[anchor?.kind] ?? 'Grouping'} ${anchor?.name ?? key} with ${kinds.map(([kind, n]) => count(n, kind)).join(', ') || 'nothing else'} that belong to it.`;
	}

	// what makes a page or shared component important: "opened from 13 pages, changes 5 types, shows 4"
	static describeImportance(n, importance) {

		const count = (n, one) => `${n} ${one}${n === 1 ? '' : 's'}`;
		const parts = [];

		// a component is reached through the pages that hold it, too
		parts.push(importance.from.size ? `${n.kind === 'component' ? 'on' : 'opened from'} ${count(importance.from.size, 'page')}` : 'no page leads here');

		if (importance.changes.size) {
			parts.push(`changes ${count(importance.changes.size, 'type')}`);
		}

		if (importance.shows.size) {
			parts.push(`shows ${importance.changes.size ? importance.shows.size : count(importance.shows.size, 'type')}`);
		}

		if (!importance.changes.size && !importance.shows.size) {
			parts.push('works with no type');
		}

		return parts.join(', ');
	}

	// who may see a page or shared component: its visibility, its owner and the grants of users and groups
	describeAccess(access) {

		const parts = [];

		if (access.public || access.authenticated) {
			parts.push(`visible to ${[access.public ? 'public' : null, access.authenticated ? 'authenticated' : null].filter(Boolean).join(' and ')} users`);
		} else {
			parts.push('not visible to public or authenticated users');
		}

		if (access.owner) {
			parts.push(`owner ${access.owner}`);
		}

		for (const { principal, permissions } of access.grants ?? []) {
			parts.push(`${principal}: ${permissions.split(',').join(', ')}`);
		}

		if (!access.owner && !access.grants?.length && !access.public && !access.authenticated) {
			parts.push('only admins see it');
		}

		return parts.join(' · ');
	}

	// how many HTML elements of a page or shared component have problems, see AppMapPanels.problemsOfPage
	// ----- call traces: the scripting errors with the calls that led to them (see CallTraces), on the elements of the map -----
	/**
	 * Takes the scripting errors as the server gives them: { message, count, last, frames }, the frames the
	 * failing one first, then its callers, each with the line it failed in or made its call from.
	 */
	setTraces(list) {

		this.traceData = list ?? [];
		this.resolveTraces();
		this.updateHint();
		this.requestDraw();
	}

	/**
	 * Each trace as the steps over the map it took: the frames as the elements of the map they are shown as, the failing
	 * one first, frames in the same element as one step; a frame the map has no element for stays a step of its own, to be
	 * named but not drawn. A trace belongs to the element it failed in.
	 */
	resolveTraces() {

		const key      = t => JSON.stringify([t.message, t.frames]);
		const shown    = this.callTrace ? key(this.callTrace) : null;
		this.traces    = [];
		this.tracesOf  = new Map();

		for (const t of this.traceData) {

			const steps = [];

			for (const frame of t.frames ?? []) {

				const n    = this.traceElement(frame);
				const last = steps[steps.length - 1];

				if (n && last?.node === n) {

					last.frames.push(frame);

				} else {

					steps.push({ node: n, frames: [frame] });
				}
			}

			const trace = { ...t, steps, origin: steps[0]?.node ?? null };

			this.traces.push(trace);

			if (trace.origin) {
				this.tracesOf.set(trace.origin, [...(this.tracesOf.get(trace.origin) ?? []), trace]);
			}
		}

		// a trace shown stays, with its fresh numbers, while it is still there
		this.callTrace = shown ? this.traces.find(t => key(t) === shown && t.origin) ?? null : null;
	}

	/**
	 * The element of the map a frame of a call trace is shown as: the first element the server located its code in (a
	 * method is itself, a template is its page), a request its REST endpoint, the type it addresses or the page at its
	 * path, a scheduled run the method it runs; null for what the map has no element for.
	 */
	traceElement(frame) {

		for (const id of frame.elements ?? []) {

			const n = this.byId.get(id);

			if (n) {
				return n;
			}
		}

		const name = String(frame.name ?? '');

		if (frame.kind === 'CRON') {
			return this.nodes.find(n => n.kind === 'method' && n.name === name) ?? null;
		}

		if (frame.kind !== 'HTTP') {
			return null;
		}

		const path = name.split('?')[0];
		const rest = /\/rest\/(.*)$/.exec(path);

		if (rest) {

			const target = '/' + rest[1].replace(/\/$/, '');
			const api    = this.nodes.filter(n => n.kind === 'api' && (target === n.name || target.startsWith(n.name + '/'))).sort((a, b) => b.name.length - a.name.length)[0];

			return api ?? this.nodes.find(n => n.kind === 'type' && n.name === target.split('/')[1]) ?? null;
		}

		const first = path.split('/').filter(Boolean)[0] ?? '';

		return this.nodes.find(n => n.kind === 'page' && (n.routes ?? []).some(route => route.path === path))
			?? this.nodes.find(n => n.kind === 'page' && n.name === first)
			?? null;
	}

	// what is wrong with an element: the scripting errors that failed in it, and for a page or shared component the
	// problems of its HTML elements (see AppMapPanels.problemsOfPage); { errors, warnings, traces }
	problemsOfElement(n) {

		const traces = this.tracesOf.get(n) ?? [];
		const page   = AppMapView.isSheet(n) ? this.panels.problemsOfPage(n) : { errors: 0, warnings: 0 };

		return { errors: page.errors + traces.length, warnings: page.warnings, traces };
	}

	// shows the call trace of a scripting error that failed in an element, the most recent first and the next one with
	// every further call; the element is held, so the rest of the map steps back behind the elements of the trace
	showCallTrace(n) {

		const traces = this.tracesOf.get(n) ?? [];

		if (!traces.length) {
			return;
		}

		const index = this.callTrace?.origin === n ? (traces.indexOf(this.callTrace) + 1) % traces.length : 0;

		this.pin(n);
		this.callTrace = traces[index];
		this.showWhole(this.callTrace.steps.map(step => step.node).filter(Boolean));
		this.updateHint();
		this.requestDraw();
	}

	// glides so that all the given elements are on the screen with their shapes, unless they are already; it only zooms out
	// for that. The size of an element is taken at the current zoom, a little generous for a smaller one
	showWhole(list) {

		const t      = this.transform;
		const margin = AppMapView.TRACE_MARGIN;
		const half   = n => (AppMapRenderer.boxOf(n) ?? [1, 1]).map(v => v * this.shownRadius(n));
		const x0     = d3.min(list, n => n.x - half(n)[0]);
		const x1     = d3.max(list, n => n.x + half(n)[0]);
		const y0     = d3.min(list, n => n.y - half(n)[1]);
		const y1     = d3.max(list, n => n.y + half(n)[1]);

		if (!list.length || (t.applyX(x0) > margin && t.applyX(x1) < this.width - margin && t.applyY(y0) > margin && t.applyY(y1) < this.height - margin)) {
			return;
		}

		const fit = Math.min((this.width - 2 * margin) / Math.max(x1 - x0, 1e-6), (this.height - 2 * margin) / Math.max(y1 - y0, 1e-6));

		this.glide((x0 + x1) / 2, (y0 + y1) / 2, Math.min(t.k, fit));
	}

	// the room a call trace keeps from the edges of the screen when it is shown whole, in pixels
	static TRACE_MARGIN = 120;

	// a step of a call trace as it is written: the element, or the frame where the map has no element for it, with the line
	// (the line that failed in the failing step, the line of the call in a caller); a request with its HTTP method
	static stepLabel(step) {

		const frame = step.frames[0];
		const name  = String(step.node?.name ?? frame.description ?? frame.name ?? frame.kind);
		const verb  = frame.kind === 'HTTP' && frame.method && !name.startsWith(frame.method) ? `${frame.method} ` : '';

		return `${verb}${name}${frame.row ? `:${frame.row}` : ''}`;
	}

	// a call trace in one line, from the failing step to where the work entered
	static traceLine(trace) {

		return trace.steps.map(step => AppMapView.stepLabel(step)).join(' ← ');
	}

	// what the problem sign of an element says
	describeProblems(n) {

		const { traces } = this.problemsOfElement(n);
		const lines      = [];
		const shorten    = text => text.length > 160 ? text.slice(0, 157) + '…' : text;

		for (const t of traces.slice(0, AppMapView.TRACES_SHOWN)) {

			lines.push(`${shorten(t.message ?? 'Scripting error')}${t.count > 1 ? ` (${t.count}×)` : ''}`);
			lines.push(`   ${AppMapView.traceLine(t)}`);
		}

		if (traces.length > AppMapView.TRACES_SHOWN) {
			lines.push(`and ${traces.length - AppMapView.TRACES_SHOWN} more`);
		}

		if (AppMapView.isSheet(n)) {

			const page = this.panels.problemsOfPage(n);

			if (page.errors || page.warnings) {
				lines.push(...this.describePageProblems(n));
			}
		}

		if (traces.length) {
			lines.push(traces.length > 1 ? 'Click the sign to show a call trace on the map, again for the next one' : 'Click the sign to show the call trace on the map');
		}

		return lines;
	}

	static TRACES_SHOWN = 3;

	// the line below the map while a call trace shows
	describeCallTrace() {

		const t      = this.callTrace;
		const others = (this.tracesOf.get(t.origin)?.length ?? 1) - 1;

		return `${t.message ?? 'Scripting error'} · ${AppMapView.traceLine(t)}${others ? ` · click the sign for the next of ${others + 1}` : ''} · Esc ends`;
	}

	describePageProblems(n) {

		const { errors, warnings } = this.panels.problemsOfPage(n);
		const count = (number, what) => `${number} ${what}${number === 1 ? '' : 's'}`;
		const lines = [];

		if (errors) {
			lines.push(`${count(errors, 'element')} caused scripting errors`);
		}

		if (warnings) {
			lines.push(`${count(warnings, 'repeater')} cannot work as configured`);
		}

		lines.push('Zoom in to see them at their elements');

		return lines;
	}

	// what is wrong with an HTML element (see AppMapPanels.problemsOf), then what it does
	describeDom(d) {

		const { errors, warnings } = AppMapPanels.problemsOf(d);

		return [...errors, ...warnings, ...this.describeDomUse(d)];
	}

	// what an HTML element does, a line for each of its repeaters and event action mappings, in words: what it repeats,
	// on which event it does what to which type, where that type comes from, and where it leads afterwards
	describeDomUse(d) {

		// [when, what]: what happens on which event, or what the element repeats
		const lines = [];

		for (const n of d.data.inner ?? []) {

			if (n.kind === 'view') {

				lines.push(['Repeats', `${n.type ?? 'objects of a type not found'}${AppMapView.typeSource(n.how)}`]);
				continue;
			}

			// a method the server did not find in the schema keeps the name the action mapping gives it
			const named  = String(n.name).split('→').pop().trim();
			const method = this.byId.get(n.method)?.name;
			const verb   = { create: 'creates', update: 'updates', delete: 'deletes' }[n.action];
			const what   = n.action === 'method' || (!n.action && method) ? (method ? `runs ${method}()` : `runs ${named}, which is no method of the schema`)
				: verb ? `${verb} ${n.type ?? 'an object of a type not found'}${AppMapView.typeSource(n.how)}`
				: (AppMapView.ACTIONS[n.action] ?? (n.action ? `does ${n.action}` : named));
			const leads  = this.links.filter(l => l.via === n && l.kind === 'navigates').map(l => l.target.name);

			lines.push([`On ${n.event ?? 'an event'}`, `${what}${leads.length ? `, then opens ${leads.join(', ')}` : ''}`]);
		}

		const shows = d.data.shows ? [`Shows ${d.data.shows}`] : [];

		if (!lines.length) {
			return shows.length ? shows : [`HTML element at level ${d.depth}${d.data.action ? ', with an event action mapping whose target was not found' : ''}`];
		}

		// the elements below the box that the outline no longer shows one by one name their actions at the box: the same
		// line once, with the number of elements it stands for
		return shows.concat(d3.rollups(lines, v => v.length, ([when]) => when, ([, what]) => what).flatMap(([when, whats]) => whats.map(([what, count]) =>
			when === 'Repeats' ? `Repeats ${what}${count > 1 ? `, ${count} times` : ''}` : `${when}${count > 1 ? ` at ${count} elements` : ''}: ${what}`
		)));
	}

	/**
	 * Why another element is related to the element in focus, as lines: the relations behind the arc between them (see
	 * liftedLinks), each explained in the terms of the application (see explainLink); those with the element itself when
	 * it has any, else those with its whole grouping. The same line once with its number, the most frequent first, at most
	 * EXPLAIN_LINES.
	 */
	explainRelation(focus, other) {

		const links  = this.liftedLinks(focus).get(this.anchorOf(other)) ?? [];
		const own    = links.filter(l => l.source === other || l.target === other);
		const chosen = own.length ? own : links;

		// fields that save themselves when they change are one line: what they change, and the fields
		const edits  = chosen.filter(l => AppMapView.isFieldEdit(l.via));
		const fields = d3.rollups(edits, v => [...new Set(v.map(l => l.via.use?.element ?? l.via.name))], l => `${{ create: 'creates', update: 'updates', delete: 'deletes' }[l.via.action]} ${l.via.type ?? 'an object'}`)
			.map(([what, names]) => [`${what} from ${names.length === 1 ? 'the field' : `${names.length} fields:`} ${names.slice(0, 6).map(n => `“${n}”`).join(', ')}${names.length > 6 ? ', …' : ''}`, 1]);

		const lines  = d3.rollups(chosen.filter(l => !AppMapView.isFieldEdit(l.via)), v => v.length, l => this.explainLink(l, focus, other)).concat(fields).sort((a, b) => b[1] - a[1]);
		const shown  = lines.slice(0, AppMapView.EXPLAIN_LINES).map(([line, count]) => count > 1 ? `${count} × ${line}` : line);

		if (lines.length > shown.length) {
			shown.push(`… and ${d3.sum(lines.slice(shown.length), ([, count]) => count)} more`);
		}

		return shown;
	}

	static EXPLAIN_LINES = 8;

	// the events of a field that saves itself as it is edited, as opposed to a button that saves a form
	static FIELD_EVENTS = new Set(['change', 'input', 'focusout', 'blur', 'keyup', 'keydown']);

	// an event action mapping that creates, updates or deletes an object when a field changes, without user input of others
	static isFieldEdit(via) {

		return via?.kind === 'action' && ['create', 'update', 'delete'].includes(via.action) && AppMapView.FIELD_EVENTS.has(via.event) && !via.use?.inputs?.length;
	}

	/**
	 * A relation in the terms of the application: what the repeater or the event action mapping behind it does (see the
	 * use the server sends), else the line of code the server found it in. The page it lies in is named unless it is one of
	 * the two elements the reader already looks at.
	 */
	explainLink(l, focus, other) {

		const via   = l.via;
		const home  = via ? (l.source.kind === 'page' || l.source.kind === 'component' ? l.source : l.target) : null;
		const where = home && home !== focus && home !== other ? `${home.name}: ` : '';

		if (via?.kind === 'view') {

			const use  = via.use ?? {};
			const type = via.type ?? 'object';
			const what = use.role === 'choice' ? `a dropdown to choose a ${type}${use.field ? ` for ${use.field}` : ''}`
				: use.role === 'row' ? `a table row for every ${type}`
				: `repeats an element for every ${type}`;

			return `${where}${what}${use.query ? ` · ${use.query}` : ''}`;
		}

		if (via?.kind === 'action') {

			const use    = via.use ?? {};
			const method = this.byId.get(via.method)?.name;
			const verb   = { create: 'creates', update: 'updates', delete: 'deletes' }[via.action];
			const what   = via.action === 'method' || (!via.action && method) ? `runs ${method ?? String(via.name).split('→').pop().trim()}`
				: verb ? `${verb} ${via.type ?? 'an object'}${use.inputs?.length ? ` (${use.inputs.join(', ')})` : ''}`
				: (AppMapView.ACTIONS[via.action] ?? `does ${via.action ?? 'nothing'}`);

			return `${where}on ${via.event ?? 'an event'} at “${use.element ?? 'an element'}”: ${what}`;
		}

		const label = AppMapModel.EDGE_LABEL[l.kind] ?? l.kind;

		if (l.kind === 'current') {
			return `${where}opens a ${l.target.name} by its ${l.evidence ?? 'URL'}`;
		}

		// the evidence of what a method does names the method and the line of its code, which says what but not to whom;
		// the evidence of a call is a sentence of its own
		if (l.evidence && AppMapModel.OPERATION_KINDS.has(l.kind)) {
			return `${label} ${l.target.name} · ${l.evidence}`;
		}

		return l.evidence ?? `${l.source.name} ${label} ${l.target.name}`;
	}

	// what the built-in actions of an event action mapping do, in words
	static ACTIONS = {
		'next-page':      'shows the next page of a list',
		'prev-page':      'shows the previous page of a list',
		'first-page':     'shows the first page of a list',
		'last-page':      'shows the last page of a list',
		'sign-in':        'signs the user in',
		'sign-out':       'signs the user out',
		'sign-up':        'signs a new user up',
		'reset-password': 'resets the password'
	};

	// where the type of a repeater or an event action mapping was found, as the server says in how
	static typeSource(how) {

		const [key, value] = String(how ?? '').split(':');
		const from         = {
			dataType:       'its data type',
			idExpression:   'its id expression',
			scope:          value === 'current' ? 'current, the object the page shows' : (value === 'me' ? 'me, the signed-in user' : `the repeater ${value}`),
			'key name':     `the data key ${value}`,
			'method owner': 'the type of its method',
			parameter:      `its parameter ${value}`,
			'input names':  'the names of its inputs',
			query:          'its query',
			relationship:   'the relationship it follows',
			properties:     'the properties it shows'
		}[key];

		return from ? ` (from ${from})` : '';
	}

	// the tooltip: a title and a detail, or a title and lines when the detail is a list. It keeps the shape its text gives
	// it: it is measured where it has the whole width, and then stands beside the pointer, to its left and above it where
	// it would reach beyond the map, so that the edge never squeezes it. The width is what it takes at most (see
	// .appmap-tip), the least room it keeps from the right edge
	showTip(e, title, detail, width) {

		const box = this.container.getBoundingClientRect();
		const x   = e.clientX - box.left;
		const y   = e.clientY - box.top;
		const gap = 14;

		this.tip.innerHTML = '<b></b><span></span>';
		this.tip.querySelector('b').textContent    = title;
		this.tip.querySelector('span').textContent = Array.isArray(detail) ? detail.join('\n') : detail;
		this.tip.querySelector('span').classList.toggle('appmap-tip-lines', Array.isArray(detail));
		this.tip.hidden     = false;
		this.tip.style.left = '0px';
		this.tip.style.top  = '0px';

		const w = Math.max(this.tip.offsetWidth, Math.min(width, this.width)) || width;
		const h = this.tip.offsetHeight;

		this.tip.style.left = `${Math.max(0, x + gap + w <= this.width ? x + gap : Math.min(x - gap - w, this.width - w))}px`;
		this.tip.style.top  = `${Math.max(0, y + gap + h <= this.height ? y + gap : y - gap - h)}px`;
	}

	onPointerMove(e) {

		const [px, py] = d3.pointer(e, this.canvas);
		const [wx, wy] = this.transform.invert([px, py]);
		const marker   = this.renderer.findMarker(px, py);

		// a jump marker of the element being explored answers first; a collapsed one stays open while the pointer is on it
		const expand = marker && (marker.anchors.length > 1 || marker.group === this.expandedMarker) ? marker.group : null;

		if ((marker?.anchors[0] ?? null) !== this.hoveredMarker || expand !== this.expandedMarker) {

			this.hoveredMarker  = marker?.anchors[0] ?? null;
			this.expandedMarker = expand;
			this.requestDraw();
		}

		if (marker) {

			const first = marker.anchors[0];

			this.canvas.style.cursor = 'pointer';
			this.tip.hidden          = marker.anchors.length > 1;

			// a marker of the element held says why it leads there
			if (marker.anchors.length === 1) {

				const why = this.pinned && !this.pinnedDom ? this.explainRelation(this.pinned, first) : [];

				this.showTip(e, `Go to ${first.name}`, why.length ? [AppMapModel.KIND_LABEL[first.kind], ...why] : AppMapModel.KIND_LABEL[first.kind], why.length ? 480 : 300);
			}

			return;
		}

		// the name of a grouping says what holds it together
		const named = !this.pinned ? this.renderer.groupingNamePlaces(this.transform.k).find(({ box }) => px >= box[0] && px <= box[2] && py >= box[1] && py <= box[3]) : null;

		if (named) {

			this.showTip(e, named.name, this.describeGrouping(named.key), 360);

			if (this.hovered || this.hoveredDom || this.hoveredStrand) {

				this.hovered       = null;
				this.hoveredDom    = null;
				this.hoveredStrand = null;
				this.requestDraw();
			}

			return;
		}

		// the kind on the header of a card says what the element is
		const kind = this.panels.kindAt(wx, wy);

		if (kind) {

			this.canvas.style.cursor = '';
			this.showTip(e, kind.kind, kind.about, 360);

			if (this.hoveredDom || this.hoveredStrand) {

				this.hoveredDom    = null;
				this.hoveredStrand = null;
				this.requestDraw();
			}

			return;
		}

		// the smallest thing under the pointer answers, at every zoom: an HTML element, an element (or the anchor of the
		// grouping whose outline holds the pointer), a strand
		const troubled = this.renderer.findProblems(wx, wy);
		const access   = troubled ? null : this.renderer.findAccess(wx, wy);

		this.canvas.style.cursor = troubled || access ? 'pointer' : '';

		// the problem sign of an element says what is wrong: the scripting errors in its code with the calls that led there,
		// and for a page how many of its elements have problems, which show on the elements themselves
		if (troubled) {

			this.showTip(e, troubled.name, this.describeProblems(troubled), 520);

			if (this.hoveredDom || this.hoveredStrand) {

				this.hoveredDom    = null;
				this.hoveredStrand = null;
				this.requestDraw();
			}

			return;
		}

		// the access badge of a page stands for the page, which from afar it may be all there is to see of: the page comes
		// into focus, and the tooltip says who may see it
		if (access) {

			// an open page shows its structure; the badge then only tells who may see it, without putting the page in focus
			const focus = this.panels.isOpen(access) ? null : access;

			this.showTip(e, access.name, `${AppMapModel.KIND_LABEL[access.kind]} · ${this.describeAccess(access.access)}`, 420);

			if (this.hovered !== focus || this.hoveredDom || this.hoveredStrand) {

				this.hovered       = focus;
				this.hoveredDom    = null;
				this.hoveredStrand = null;
				this.requestDraw();
			}

			return;
		}

		// the box of a row of an open card says that it opens closer up: it stands out, the pointer becomes a magnifier, and
		// the tooltip says how; an open one shows itself
		const box  = this.panels.tileAt(wx, wy);
		const item = box?.item ?? null;

		if (item !== this.hoveredTile) {

			this.hoveredTile = item;
			this.requestDraw();
		}

		if (box) {

			const open = box.height * this.transform.k >= AppMapPanels.TILE_OPEN;

			this.canvas.style.cursor = open ? (this.toggle && this.panels.toggleAt(wx, wy) ? 'pointer' : '') : 'zoom-in';

			if (open) {
				this.tip.hidden = true;
			} else {
				this.showTip(e, item.tile.name, `${item.tile.kind ?? ''} · zoom in or double-click to open`, 360);
			}

			if (this.hovered || this.hoveredDom || this.hoveredStrand) {

				this.hovered       = null;
				this.hoveredDom    = null;
				this.hoveredStrand = null;
				this.requestDraw();
			}

			return;
		}

		const tier = this.zoomTier();
		const dom  = this.panels.findDom(wx, wy);
		const n    = dom ? null : this.find(px, py);

		if (dom !== this.hoveredDom) {

			this.hoveredDom = dom;
			this.requestDraw();
		}

		if (dom) {

			// an open element shows itself; an HTML element with relations can be clicked to explore from it
			if (dom.opened) {
				this.tip.hidden = true;
			} else {
				this.showTip(e, `${AppMapPanels.nameOf(dom)} in ${dom.page.name}`, this.describeDom(dom), 420);
			}

			this.canvas.style.cursor = this.panels.boxTargets(dom).length ? 'pointer' : '';

			if (this.hovered || this.hoveredStrand) {

				this.hovered       = null;
				this.hoveredStrand = null;
				this.requestDraw();
			}

			return;
		}

		if (n !== this.hovered) {

			this.hovered = n;
			this.requestDraw();
		}

		// an element can be clicked to explore from it; while exploring, the strands keep still
		this.canvas.style.cursor = n ? 'pointer' : '';

		const strand = !n && !this.pinned && tier >= 1 && this.strands.bundled() ? this.strands.findStrand(wx, wy) : null;

		if (strand !== this.hoveredStrand) {

			this.hoveredStrand = strand;
			this.requestDraw();
		}

		if (strand) {

			this.showTip(e, AppMapStrands.strandLabel(strand), `${strand.links.length} relations · ${strand.kinds.map(([kind, c]) => `${c} ${AppMapModel.EDGE_LABEL[kind] ?? kind}`).join(', ')}`, 320);

		} else if (n) {

			// while an element is held, another one says why it is related to it
			const why = this.pinned && n !== this.pinned && !this.pinnedDom ? this.explainRelation(this.pinned, n) : [];

			if (why.length) {
				this.showTip(e, `${n.name} and ${this.pinned.name}`, why, 480);
			} else {
				this.showTip(e, n.name, this.describe(n), 300);
			}

		} else {

			this.tip.hidden = true;
		}
	}

	onPointerLeave() {

		this.hovered        = null;
		this.hoveredDom     = null;
		this.hoveredTile    = null;
		this.hoveredStrand  = null;
		this.hoveredMarker  = null;
		this.expandedMarker = null;
		this.tip.hidden     = true;
		this.requestDraw();
	}
}
