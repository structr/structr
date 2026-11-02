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

	// two looks: a sober one on white and a dark one; on white, overlaps darken like ink, on black they glow
	static LOOKS = {
		light: {
			bg: '#ffffff', card: '#ffffff', veil: '#eef1f4', trace: '#7d8794', ring: '#1b2026', outline: '#ffffff', halo: '#ffffff', label: '#4f5862', labelNear: '#2a3038', labelFocus: '#111418', column: '#9aa3ad', found: '#ffd84d', blend: 'multiply', dark: false,
			kinds: { page: '#5b2fe0', component: '#b0179b', type: '#1f8f12', method: '#0081a7', api: '#0b5ed7', external: '#123a9c', group: '#e04e00', dom: '#9a86f2', view: '#d18b00', action: '#e0123f' }
		},
		dark: {
			bg: '#0b0e12', card: '#161b22', veil: '#000000', trace: '#8a96a3', ring: '#ffffff', outline: '#0b0e12', halo: '#0b0e12', label: '#aab3bd', labelNear: '#dfe4e9', labelFocus: '#ffffff', column: '#c3cbd3', found: '#d4a900', blend: 'lighter', dark: true,
			kinds: { page: '#a78bfa', component: '#f472d0', type: '#6ee05a', method: '#22d3ee', api: '#60a5fa', external: '#a5c8ff', group: '#ff8a3d', dom: '#b9a8ff', view: '#ffd04d', action: '#ff5c7c' }
		}
	};

	// the size of an element grows with its measure (see measureOf), against the largest element of its own sector: each
	// sector has its own scale, so a sector of small elements shows its structure as clearly as one of large ones (see
	// radiusOf for the curve); SIZE_BY is 'size' for how large an element is, 'links' for how connected
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

	// how far out the map can be zoomed, as a share of the zoom at which it fits the screen
	static ZOOM_OUT = 0.25;

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

	// the most important pages are named in bold, as many as there are hotspots (see AppMapModel.importance)
	static KEY_PAGES = AppMapModel.HOTSPOTS;
	static GLIDE_FILL = 0.86;

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
		const style    = getComputedStyle(document.documentElement);

		this.container   = container;
		this.model       = model;
		this.nodes       = model.nodes;
		this.byId        = model.byId;
		this.storage     = options.storage ?? { get: () => null, set: () => {}, remove: () => {} };
		this.onStatus    = options.onStatus ?? (() => {});
		this.synchronous = !!options.synchronous;

		this.look  = AppMapView.LOOKS[settings.look] ?? AppMapView.LOOKS.light;
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

		// the activity lens: how much the entry points are used or how slowly they answer, and where scripting errors
		// happened, see setActivity and setErrors
		this.activity      = null;
		this.errors        = null;
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

		// reads the source of a method, or the text of the template a page consists of, for its box close up, as a promise of
		// the text
		this.loadSource = options.loadSource ?? null;

		this.fonts = {
			main: style.getPropertyValue('--font-main').trim() || 'Inter, sans-serif',
			mono: style.getPropertyValue('--font-monospace').trim() || 'monospace'
		};

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

		d3.select(this.canvas).on('.zoom', null).on('mousemove', null).on('mouseleave', null).on('click', null).on('dblclick', null);
		this.canvas.remove();
		this.tip.remove();
		this.trailBar.remove();
	}

	get settings() {

		return { look: this.look.dark ? 'dark' : 'light', activity: this.activity?.mode ?? null, errors: !!this.errors };
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
		this.keyPages   = new Set([...this.importance].filter(([n, i]) => n.k === 'page' && i.value > 0).sort((a, b) => b[1].value - a[1].value).slice(0, AppMapView.KEY_PAGES).map(([n]) => n));
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

		return n.c?.[1] ?? '-';
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
		const min  = AppMapView.isSheet(n) ? AppMapView.R_MIN_PAGE : AppMapView.R_MIN;

		// sizes spread widely (a method of ten lines beside one of nine hundred), so they count by their logarithm; relations
		// by the square root, so the area is proportional
		const share = AppMapView.SIZE_BY === 'size' ? Math.log1p(this.measureOf(n)) / Math.log1p(most) : Math.sqrt(this.measureOf(n) / most);

		return min + (AppMapView.R_MAX - min) * Math.min(1, share);
	}

	// pages and shared components, the sheets of the map
	static isSheet(n) {

		return n.k === 'page' || n.k === 'component';
	}

	/**
	 * What the size of an element follows (see SIZE_BY): how large it is in its own terms, as the server measures it (the
	 * properties of a type, the lines of code of a method, the HTML elements of a page, the members of a group), or how much
	 * it matters: the number of its relations, for a page or shared component its importance (see AppMapModel.importance).
	 * Where the server sends no size, a page is measured by its outline and anything else by its relations.
	 */
	measureOf(n) {

		const degree = this.degree.get(n) ?? 0;

		if (AppMapView.SIZE_BY !== 'size') {
			return AppMapView.isSheet(n) ? (this.importance.get(n)?.value ?? 0) : degree;
		}

		return n.s ?? (n.d ? AppMapView.outlineSize(n.d) : null) ?? (n.k === 'group' ? n.m : null) ?? degree;
	}

	// sizes are compared within a sector, and by size within one kind, as the lines of a method say nothing about a type
	scaleKeyOf(n) {

		const area = n.c?.[0] ?? '-';

		return AppMapView.SIZE_BY === 'size' ? `${area}|${n.k}` : area;
	}

	// the number of HTML elements in an outline of nested [tag, flags, children, ids, expressions]
	static outlineSize(items) {

		return d3.sum(items, item => 1 + AppMapView.outlineSize(item[2] ?? []));
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
				.sort((a, b) => b.w - a.w)
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
	onClick(e) {

		const [px, py] = d3.pointer(e, this.canvas);
		const [wx, wy] = this.transform.invert([px, py]);
		const marker   = this.renderer.findMarker(px, py);
		const dom      = this.panels.findDom(wx, wy);

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

		const access = this.renderer.findAccess(wx, wy);
		const n      = access ?? this.find(px, py);

		if (n) {

			this.pin(n);

		} else if (!this.panels.covers(wx, wy) && !this.panels.findDom(wx, wy)) {

			this.unpin();
		}
	}

	pin(n) {

		if (this.trail[this.trail.length - 1] !== n) {
			this.trail.push(n);
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
		const hits     = [...this.byId.values()].filter(n => (n.n != null && fold(n.n).includes(wanted)) || (n.r ?? []).some(route => fold(route.p).includes(wanted)));
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

		this.access = role ? AppMapAccess.of(this.model, this.links, role) : null;
		this.updateHint();
		this.requestDraw();
	}

	// ----- the activity lens: what the HTTP service measured at the entry points -----
	// 'usage' for the number of requests, 'slow' for their average time, or null
	setActivity(mode) {

		this.activity = AppMapView.ACTIVITY[mode] ? { mode } : null;
		this.measure();
		this.watchActivity();
	}

	// the scripting errors since the last start, on or off; they show together with the activity
	setErrors(on) {

		this.errors = on ? {} : null;
		this.measure();
		this.watchActivity();
	}

	// the heat of the activity and the errors from the numbers the elements carry now
	measure() {

		if (this.activity) {
			this.activity = { mode: this.activity.mode, ...this.heatOf(n => n.hits, AppMapView.ACTIVITY[this.activity.mode]) };
		}

		if (this.errors) {
			this.errors = this.heatOf(n => n.errs, n => n.errs.n);
		}

		this.updateHint();
		this.requestDraw();
	}

	// while the activity or the errors show, their numbers are fetched fresh: at once when they come on, then every
	// ACTIVITY_REFRESH milliseconds; the map itself is not loaded again
	watchActivity() {

		const on = !!(this.activity || this.errors) && !!this.loadActivity;

		if (on && this.activityTimer == null) {

			this.refreshActivity();
			this.activityTimer = setInterval(() => this.refreshActivity(), AppMapView.ACTIVITY_REFRESH);

		} else if (!on && this.activityTimer != null) {

			clearInterval(this.activityTimer);
			this.activityTimer = null;
		}
	}

	static ACTIVITY_REFRESH = 30000;

	async refreshActivity() {

		const fresh = await this.loadActivity();

		if (!fresh || this.destroyed) {
			return;
		}

		for (const n of this.nodes) {

			n.hits = fresh[n.id]?.hits;
			n.errs = fresh[n.id]?.errs;
		}

		this.measure();
	}

	// what the activity lens shows of an element: its number of requests or their average time
	static ACTIVITY = { usage: n => n.hits.n, slow: n => n.hits.avg };

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

	// the activity and the errors in one line, for the line below the map
	describeActivity() {

		const parts = [];

		if (this.activity) {

			const { mode, heat, top } = this.activity;

			parts.push(!top ? 'No requests recorded yet'
				: `${heat.size} pages, endpoints and types measured · ` + (mode === 'usage' ? `most used: ${top.n} with ${top.hits.n.toLocaleString()} requests` : `slowest: ${top.n} with ${top.hits.avg.toLocaleString()} ms on average`));
		}

		if (this.errors) {

			const { heat, top } = this.errors;

			parts.push(!top ? 'no scripting errors since the last start' : `${heat.size} with scripting errors in red, most: ${top.n} with ${top.errs.n.toLocaleString()}`);
		}

		return parts.join(' · ');
	}

	// the lens in one line, for the line below the map
	describeLens() {

		const access  = this.access;
		const pages   = this.nodes.filter(n => n.k === 'page');
		const seen    = pages.filter(n => access.pages.get(n)).length;
		const types   = [...access.rights.values()];
		const count   = right => types.filter(own => own.has(right)).length;
		const gaps    = access.gaps.length;

		return `${access.role.name} see ${seen} of ${pages.length} pages · of ${access.governed.size} types governed by the schema may read ${count('r')}, write ${count('w')}, delete ${count('d')}` + (gaps ? ` · ${gaps} ${gaps === 1 ? 'gap' : 'gaps'} in red: a page they see does what they may not` : '');
	}

	// the start page: the page of the lowest position, among those the role of the lens sees
	startPage() {

		const pages = this.nodes.filter(n => n.k === 'page' && n.pos != null && (!this.access || this.access.pages.get(n)));

		return pages.length ? pages.reduce((a, b) => b.pos < a.pos ? b : a) : null;
	}

	// glides to an element and holds its focus: at the current zoom, unless that is so close that an element without inner
	// structure, like a type, fills the screen and hides its relations; then only as close as EXPLORE_SIZE allows
	explore(n) {

		const inner  = AppMapPanels.hasCode(n) || this.panels.trees.has(n);
		const [w, h] = AppMapRenderer.boxOf(n) ?? [1, 1];
		const close  = this.zoomWhere(k => 2 * Math.max(w, h) * this.drawnRadiusAt(n, k) * k, AppMapView.EXPLORE_SIZE * Math.min(this.width, this.height));

		this.pin(n);
		this.glide(n.x, n.y, inner ? this.transform.k : Math.min(this.transform.k, close));
	}

	// the way exploring took, as a row of names above the map: a name leads back there, the cross ends exploring
	showTrail() {

		const bar = this.trailBar;

		bar.replaceChildren();
		bar.hidden = !this.trail.length;

		this.trail.forEach((n, i) => {

			const button = document.createElement('button');

			button.textContent = n.n;
			button.title       = AppMapModel.KIND_LABEL[n.k];
			button.style.setProperty('--kind', this.look.kinds[n.k] ?? '#888888');
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

			box.textContent = `<${this.pinnedDom.data.tag}>`;
			box.title       = (this.pinnedDom.data.inner ?? []).map(m => m.n).join(', ');
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

		const open = this.panels.reading.size || [...this.panels.trees.values()].some(t => t.open);
		const hint = !this.nodes.length ? ''
			: this.access && !this.pinned ? this.describeLens()
			: (this.activity || this.errors) && !this.pinned ? this.describeActivity()
			: this.found ? `${this.found.elements.size ? this.found.elements.size : 'No'} ${this.found.elements.size === 1 ? 'element matches' : 'elements match'} “${this.found.text}” · click a result of the search to go there`
			: this.pinned ? 'Click a name at the end of a relation to go there · Esc or a click on the empty map ends exploring'
			: open ? 'Double-click the empty map to see the whole map again'
			: this.zoomTier() === 0 ? 'Hover an element to see what it connects to · click it to explore from there'
			: 'Double-click a page or method to open it · drag to pan, the wheel zooms';

		if (hint !== this.hint) {

			this.hint = hint;
			this.onHint?.(hint);
		}
	}

	// ----- interaction: the map reacts to panning, zooming and hovering -----
	bindEvents() {

		this.zoom = d3.zoom().scaleExtent([0.1, 150]).on('zoom', e => {

			// only a gesture of the user counts, not fitting the map to the screen
			if (e.sourceEvent) {

				this.userZoomed = true;
				this.drag.push([performance.now(), e.transform.x, e.transform.y]);
			}

			this.transform = e.transform;
			this.growth    = this.growthAt(e.transform.k);
			this.requestDraw();

		}).on('start', e => {

			// grabbing the map stops it where it is
			if (e.sourceEvent) {

				this.stopCoast();
				this.drag = [];
			}

		}).on('end', e => {

			if (e.sourceEvent) {
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

		// Escape ends exploring
		this.onKey = e => {

			if (e.key === 'Escape' && this.pinned) {
				this.unpin();
			}
		};

		document.addEventListener('keydown', this.onKey);

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

		this.wheelK     = Math.min(hi, Math.max(lo, (this.wheelK ?? this.transform.k) * Math.pow(2, delta)));
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

	// how much larger than in the layout the elements are drawn at a zoom, see SIZE_ZOOM
	growthAt(k) {

		return this.baseK ? Math.min(AppMapView.MAX_GROWTH, Math.max(1, Math.sqrt(AppMapView.SIZE_ZOOM * this.baseK / k))) : 1;
	}

	/**
	 * A double click glides to the inner structure of the element under the pointer: a page or shared component opens
	 * with its tree filling the screen, a method with its code, any other element shows its grouping, a type with its
	 * methods; on the empty map the whole map comes back. The element is held in focus as by a click, so its jump markers
	 * show. Only the view moves: the places of the elements stay exactly where they are.
	 */
	onDoubleClick(e) {

		const [px, py] = d3.pointer(e, this.canvas);
		const [wx, wy] = this.transform.invert([px, py]);
		const dom      = this.panels.findDom(wx, wy);
		const n        = dom ? dom.page : (this.panels.openAt(wx, wy) ?? this.find(px, py));

		e.preventDefault();

		// the element opened is the one explored from: its jump markers lead on; inside an open page the HTML element stays held
		if (dom) {
			this.pinDom(dom);
		} else if (n) {
			this.pin(n);
		}

		if (n) {
			this.glideInto(n);
		} else {
			this.glide(...this.fitTransform());
		}
	}

	// glides so close to an element that it shows what it holds: the code of a method, the tree of a page, the elements of
	// a grouping
	glideInto(n) {

		// a method, or a page of a single template: its box fills the screen and shows its code
		if (AppMapPanels.hasCode(n)) {

			const [w, h] = AppMapRenderer.boxOf(n);
			const across = this.zoomWhere(k => 2 * w * this.drawnRadiusAt(n, k) * k, AppMapView.GLIDE_FILL * this.width);
			const down   = this.zoomWhere(k => 2 * h * this.drawnRadiusAt(n, k) * k, AppMapView.GLIDE_FILL * this.height);

			this.panels.requestCode(n);
			this.glide(n.x, n.y, Math.min(across, down));

			return;
		}

		const tree = this.panels.trees.get(n);

		if (tree) {

			// as large as the screen allows, and at least so close that the tree opens
			const [w, h] = AppMapRenderer.PAGE_BOX;
			const across = this.zoomWhere(k => 2 * w * this.drawnRadiusAt(n, k) * k, AppMapView.GLIDE_FILL * this.width);
			const down   = this.zoomWhere(k => 2 * h * this.drawnRadiusAt(n, k) * k, AppMapView.GLIDE_FILL * this.height);
			const fill   = Math.min(across, down);
			const open   = this.zoomWhere(k => this.panels.pagePanel(n, tree, k, this.drawnRadiusAt(n, k)).row, AppMapPanels.OPEN_ROW * 1.2);

			this.glide(n.x, n.y, Math.max(fill, open));

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
	glide(x, y, k) {

		const [lo, hi] = this.zoom.scaleExtent();
		const scale    = Math.min(hi, Math.max(lo, k));

		this.wheelK     = null;
		this.userZoomed = true;

		this.stopCoast();

		d3.select(this.canvas)
			.interrupt()
			.transition()
			.duration(AppMapView.GLIDE_MS)
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
		this.zoom.scaleExtent([k * AppMapView.ZOOM_OUT, 150]);

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
			const margin = Math.max(0, AppMapView.MIN_TARGET / k - r, n.k === 'type' && small(n) ? r * AppMapView.TYPE_MARGIN : 0);

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

		const parts = [AppMapModel.KIND_LABEL[n.k]];

		if (n.p) {
			parts.push('public');
		}

		if (n.cron) {
			parts.push(`scheduled: ${n.cron}`);
		}

		// how large it is in its own terms, as the server measures it
		if (n.s) {
			parts.push(`${n.s} ${{ type: 'properties', method: 'lines of code', page: 'HTML elements', component: 'HTML elements' }[n.k] ?? 'parts'}`);
		}

		// how a page is reached and what it is for
		parts.push(...this.describeRouting(n));

		// how often it was requested and how fast it answered, as the HTTP service measured it
		if (n.hits) {
			parts.push(`${n.hits.n.toLocaleString()} ${n.hits.n === 1 ? 'request' : 'requests'}, ${n.hits.avg.toLocaleString()} ms on average, ${n.hits.max.toLocaleString()} ms at most`);
		}

		// the scripting errors since the last start, with the latest
		if (n.errs) {
			parts.push(`${n.errs.n.toLocaleString()} scripting ${n.errs.n === 1 ? 'error' : 'errors'}` + (n.errs.msg ? `, latest${n.errs.row ? ` in line ${n.errs.row}` : ''}: ${n.errs.msg}` : ''));
		}

		// what deleting it takes along, and what it passes on
		if (n.k === 'type') {
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
			parts.push(`in the section ${section.n}`);
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
				parts.push(`changed by ${hotspot.changedBy.map(x => x.n).join(', ')}`);
			}

			if (hotspot.effects.length) {
				parts.push(`then ${hotspot.effects.join(', ')}`);
			}

			parts.push(`module ${module.name}: ${module.members.length} groupings`);
		}

		if (n.rest) {

			const whom = [n.rest.pub ? 'anonymous' : null, n.rest.auth ? 'authenticated' : null].filter(Boolean).join(' and ');

			parts.push(`REST: ${n.rest.n} resource access ${n.rest.n === 1 ? 'grant' : 'grants'}${whom ? ` for ${whom} users` : ''}`);
		}

		return parts.join(' · ');
	}

	// the URL routes of a page, whether it is the start page or an error page, what it delivers and the sites it is on
	describeRouting(n) {

		const parts = [];

		for (const route of n.r ?? []) {
			parts.push(`URL ${route.p}${route.cur ? ` opens a ${route.cur}` : ''}${route.w ? ` (${route.w} ${route.w === 1 ? 'warning' : 'warnings'})` : ''}`);
		}

		if (n === this.startPage()) {
			parts.push('the start page');
		}

		if (n.err) {
			parts.push(`shown for the errors ${n.err}`);
		}

		if (n.ct) {
			parts.push(`delivers ${n.ct}`);
		}

		const sites = this.model.sites.filter(site => site.pages.includes(n.id)).map(site => site.n);

		if (sites.length) {
			parts.push(`on ${sites.length === 1 ? 'the site' : 'the sites'} ${sites.join(', ')}`);
		}

		return parts;
	}

	// what deleting an object of a type deletes with it, and to whom the type passes on rights
	describeSchema(n) {

		const parts   = [];
		const cascade = [...AppMapAccess.cascadeOf(this.links, n)];
		const certain = cascade.filter(([, step]) => !step.maybe).map(([type]) => type.n);
		const maybe   = cascade.filter(([, step]) => step.maybe).map(([type]) => type.n);

		if (certain.length) {
			parts.push(`deleting one deletes its ${certain.join(', ')}`);
		}

		if (maybe.length) {
			parts.push(`deleting one deletes the ${maybe.join(', ')} that would become invalid`);
		}

		// the types it passes rights on to, by the rights they get
		const passes = d3.groups(AppMapAccess.propagation(this.links).filter(hop => hop.from === n), ({ l }) => AppMapAccess.RIGHTS.filter(right => l.x[right] === 'Add').map(right => AppMapAccess.RIGHT_LABEL[right]).join(', '));

		for (const [rights, hops] of passes.filter(([rights]) => rights)) {
			parts.push(`passes ${rights} on to ${hops.map(({ to }) => to.n).join(', ')}`);
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
				? `that the developer gave the category ${section.n}`
				: `that work with ${this.byId.get(key.slice('section:type:'.length))?.n ?? 'one type'} more than with any other type`;

			return `A section of the user interface: ${pages} ${why}, and what only they hold or lead to.`;
		}

		const anchor = this.byId.get(key);
		const kinds  = d3.rollups(members.filter(m => m !== anchor), v => v.length, m => AppMapModel.KIND_LABEL[m.k].toLowerCase());

		return `${AppMapModel.KIND_LABEL[anchor?.k] ?? 'Grouping'} ${anchor?.n ?? key} with ${kinds.map(([kind, n]) => count(n, kind)).join(', ') || 'nothing else'} that belong to it.`;
	}

	// what makes a page or shared component important: "opened from 13 pages, changes 5 types, shows 4"
	static describeImportance(n, importance) {

		const count = (n, one) => `${n} ${one}${n === 1 ? '' : 's'}`;
		const parts = [];

		// a component is reached through the pages that hold it, too
		parts.push(importance.from.size ? `${n.k === 'component' ? 'on' : 'opened from'} ${count(importance.from.size, 'page')}` : 'no page leads here');

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
	describeAccess(acc) {

		const parts = [];

		if (acc.pub || acc.auth) {
			parts.push(`visible to ${[acc.pub ? 'public' : null, acc.auth ? 'authenticated' : null].filter(Boolean).join(' and ')} users`);
		} else {
			parts.push('not visible to public or authenticated users');
		}

		if (acc.own) {
			parts.push(`owner ${acc.own}`);
		}

		for (const [principal, permissions] of acc.g ?? []) {
			parts.push(`${principal}: ${permissions.split(',').join(', ')}`);
		}

		if (!acc.own && !acc.g?.length && !acc.pub && !acc.auth) {
			parts.push('only admins see it');
		}

		return parts.join(' · ');
	}

	// what an HTML element does, a line for each of its repeaters and event action mappings, in words: what it repeats,
	// on which event it does what to which type, where that type comes from, and where it leads afterwards
	describeDom(d) {

		// [when, what]: what happens on which event, or what the element repeats
		const lines = [];

		for (const n of d.data.inner ?? []) {

			if (n.k === 'view') {

				lines.push(['Repeats', `${n.t ?? 'objects of a type not found'}${AppMapView.typeSource(n.how)}`]);
				continue;
			}

			// a method the server did not find in the schema keeps the name the action mapping gives it
			const named  = String(n.n).split('→').pop().trim();
			const method = this.byId.get(n.m)?.n;
			const verb   = { create: 'creates', update: 'updates', delete: 'deletes' }[n.a];
			const what   = n.a === 'method' || (!n.a && method) ? (method ? `runs ${method}()` : `runs ${named}, which is no method of the schema`)
				: verb ? `${verb} ${n.t ?? 'an object of a type not found'}${AppMapView.typeSource(n.how)}`
				: (AppMapView.ACTIONS[n.a] ?? (n.a ? `does ${n.a}` : named));
			const leads  = this.links.filter(l => l.via === n && l.kind === 'navigates').map(l => l.target.n);

			lines.push([`On ${n.ev ?? 'an event'}`, `${what}${leads.length ? `, then opens ${leads.join(', ')}` : ''}`]);
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
		const fields = d3.rollups(edits, v => [...new Set(v.map(l => l.via.use?.el ?? l.via.n))], l => `${{ create: 'creates', update: 'updates', delete: 'deletes' }[l.via.a]} ${l.via.t ?? 'an object'}`)
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

		return via?.k === 'action' && ['create', 'update', 'delete'].includes(via.a) && AppMapView.FIELD_EVENTS.has(via.ev) && !via.use?.in?.length;
	}

	/**
	 * A relation in the terms of the application: what the repeater or the event action mapping behind it does (see the
	 * use the server sends), else the line of code the server found it in. The page it lies in is named unless it is one of
	 * the two elements the reader already looks at.
	 */
	explainLink(l, focus, other) {

		const via   = l.via;
		const home  = via ? (l.source.k === 'page' || l.source.k === 'component' ? l.source : l.target) : null;
		const where = home && home !== focus && home !== other ? `${home.n}: ` : '';

		if (via?.k === 'view') {

			const use  = via.use ?? {};
			const type = via.t ?? 'object';
			const what = use.role === 'choice' ? `a dropdown to choose a ${type}${use.f ? ` for ${use.f}` : ''}`
				: use.role === 'row' ? `a table row for every ${type}`
				: `repeats an element for every ${type}`;

			return `${where}${what}${use.q ? ` · ${use.q}` : ''}`;
		}

		if (via?.k === 'action') {

			const use    = via.use ?? {};
			const method = this.byId.get(via.m)?.n;
			const verb   = { create: 'creates', update: 'updates', delete: 'deletes' }[via.a];
			const what   = via.a === 'method' || (!via.a && method) ? `runs ${method ?? String(via.n).split('→').pop().trim()}`
				: verb ? `${verb} ${via.t ?? 'an object'}${use.in?.length ? ` (${use.in.join(', ')})` : ''}`
				: (AppMapView.ACTIONS[via.a] ?? `does ${via.a ?? 'nothing'}`);

			return `${where}on ${via.ev ?? 'an event'} at “${use.el ?? 'an element'}”: ${what}`;
		}

		const label = AppMapModel.EDGE_LABEL[l.kind] ?? l.kind;

		if (l.kind === 'current') {
			return `${where}opens a ${l.target.n} by its ${l.evidence ?? 'URL'}`;
		}

		// the evidence of what a method does names the method and the line of its code, which says what but not to whom;
		// the evidence of a call is a sentence of its own
		if (l.evidence && AppMapModel.OPERATION_KINDS.has(l.kind)) {
			return `${label} ${l.target.n} · ${l.evidence}`;
		}

		return l.evidence ?? `${l.source.n} ${label} ${l.target.n}`;
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

	// the tooltip: a title and a detail, or a title and lines when the detail is a list
	showTip(e, title, detail, width) {

		const box = this.container.getBoundingClientRect();

		this.tip.innerHTML = '<b></b><span></span>';
		this.tip.querySelector('b').textContent    = title;
		this.tip.querySelector('span').textContent = Array.isArray(detail) ? detail.join('\n') : detail;
		this.tip.querySelector('span').classList.toggle('appmap-tip-lines', Array.isArray(detail));
		this.tip.hidden     = false;
		this.tip.style.left = `${Math.min(this.width - width, e.clientX - box.left + 14)}px`;
		this.tip.style.top  = `${e.clientY - box.top + 14}px`;
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

				this.showTip(e, `Go to ${first.n}`, why.length ? [AppMapModel.KIND_LABEL[first.k], ...why] : AppMapModel.KIND_LABEL[first.k], why.length ? 480 : 300);
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

		// the smallest thing under the pointer answers, at every zoom: an HTML element, an element (or the anchor of the
		// grouping whose outline holds the pointer), a strand
		const access = this.renderer.findAccess(wx, wy);

		this.canvas.style.cursor = access ? 'pointer' : '';

		// the access badge of a page stands for the page, which from afar it may be all there is to see of: the page comes
		// into focus, and the tooltip says who may see it
		if (access) {

			// an open page shows its structure; the badge then only tells who may see it, without putting the page in focus
			const focus = this.panels.isOpen(access) ? null : access;

			this.showTip(e, access.n, `${AppMapModel.KIND_LABEL[access.k]} · ${this.describeAccess(access.acc)}`, 420);

			if (this.hovered !== focus || this.hoveredDom || this.hoveredStrand) {

				this.hovered       = focus;
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

			this.showTip(e, `<${dom.data.tag}> in ${dom.page.n}`, this.describeDom(dom), 420);

			// an HTML element with relations can be clicked to explore from it
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
				this.showTip(e, `${n.n} and ${this.pinned.n}`, why, 480);
			} else {
				this.showTip(e, n.n, this.describe(n), 300);
			}

		} else {

			this.tip.hidden = true;
		}
	}

	onPointerLeave() {

		this.hovered        = null;
		this.hoveredDom     = null;
		this.hoveredStrand  = null;
		this.hoveredMarker  = null;
		this.expandedMarker = null;
		this.tip.hidden     = true;
		this.requestDraw();
	}
}
