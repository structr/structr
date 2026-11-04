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
 * Drawing the application map on its canvas, back to front: the sector borders, the lines, the strands, the grouping
 * outlines, the elements, the labels, the open page panels and finally the area names.
 *
 * Every element has the colour of its kind, always: every page the same, every method the same, every type the same; what
 * belongs together is told by the outlines and the sectors, not by colour.
 *
 * A frame costs the browser mostly rasterisation, not script: what lies off screen or would be invisible is not drawn at
 * all, and what only changes with the layout (the outlines of the groupings, the rim of the map) is computed
 * once per step of the layout, not once per frame.
 */
class AppMapRenderer {

	// every area has its own hue for its name: the interface violet, the data model green and so on
	static AREA_HUE = { ui: 265, components: 295, logic: 190, security: 28, data: 135, builtin: 215, scheduled: 330, integration: 50, api: 90 };

	// what an edge does to data shows in its colour: create, change, delete; reading stays neutral; [light, dark]
	static OP_COLOR   = { creates: ['#2b8a3e', '#69db7c'], updates: ['#c77700', '#ffc94d'], deletes: ['#c92a5a', '#ff8fab'] };

	// the rights of the access lens around a type, the schema relationships that delete along or pass rights on, and a gap
	// (light and dark look, see AppMapAccess)
	static RIGHT_COLOR  = { r: ['#1c7ed6', '#74c0fc'], w: ['#c77700', '#ffc94d'], d: ['#c92a5a', '#ff8fab'] };
	static SCHEMA_COLOR = { cascade: ['#d1253b', '#ff6b6b'], passes: ['#e67700', '#ffa94d'], gap: ['#d1253b', '#ff6b6b'] };


	// the access icons of the pages and shared components, as in the Pages area (visibility-lock-* in icon/sprites.svg), on a
	// grid of 24: open when visible to public and authenticated users, locked when visible to neither, a key in between
	static ACCESS_ICON = {
		open:   'M.75,9.75V6a5.25,5.25,0,0,1,10.5,0V9.75 M8.25 9.75h13.5a1.5 1.5 0 0 1 1.5 1.5v10.5a1.5 1.5 0 0 1-1.5 1.5H8.25a1.5 1.5 0 0 1-1.5-1.5V11.25a1.5 1.5 0 0 1 1.5-1.5Z M15 15V18',
		locked: 'M5.25 9.75h13.5a1.5 1.5 0 0 1 1.5 1.5v10.5a1.5 1.5 0 0 1-1.5 1.5H5.25a1.5 1.5 0 0 1-1.5-1.5V11.25a1.5 1.5 0 0 1 1.5-1.5Z M6.75,9.75V6a5.25,5.25,0,0,1,10.5,0V9.75 M12 15V18',
		key:    'M0.75 20.25A3 3 0 1 0 6.75 20.25A3 3 0 1 0 0.75 20.25Z M6.75 20.25L14.25 20.25 14.25 22.5 M11.25 20.25L11.25 21.75 M6.75,15V11.25a1.5,1.5,0,0,1,1.5-1.5h13.5a1.5,1.5,0,0,1,1.5,1.5v10.5a1.5,1.5,0,0,1-1.5,1.5h-4.5 M9.75,9.75V6a5.25,5.25,0,0,1,10.5,0V9.75 M15 14.25L15 17.25'
	};

	// the access badge on screen: a share of the width of its page, within these pixels, and how large a page must be on
	// screen before every page shows its badge; a page public users see shows it at every zoom, as what is open to everyone
	// matters most
	static ACCESS_SHARE = 0.35;
	static ACCESS_SIZE  = 14;
	static ACCESS_SMALL = 8;
	static ACCESS_FROM  = 8;

	// the colour of the badge: public users see the page, only authenticated users, only admins and those with grants;
	// [light, dark]
	static ACCESS_COLOR = { pub: ['#e03131', '#ff6b6b'], auth: ['#1c7ed6', '#4dabf7'], none: ['#495057', '#adb5bd'] };

	// the glow of the activity lens reaches this many pixels beyond an element, plus this many more for the hottest one, in
	// an amber that reads as warmth and that no kind of element has; the strength is in size and opacity; [light, dark]
	static HEAT_GLOW    = [4, 20];
	static HEAT_COLOUR  = ['#f08c00', '#ffa94d'];

	// the hotspots of the activity: the map in grey, veiled with this much of the background, and the names of this many of
	// the most used elements
	static SPOTLIGHT_VEIL  = 0.55;
	static SPOTLIGHT_NAMES = 12;

	// the tags of a page below it: the start page green, the pages shown for errors red; [light, dark]
	static ROUTING_COLOR = { start: ['#2f9e44', '#69db7c'], error: ['#e03131', '#ff6b6b'] };

	// a method is a rectangle lying on its side, as wide and as high as these shares of its radius on each side, so it can
	// show its code when it comes close (see AppMapPanels.codePanel); a page or shared component is a sheet standing upright
	// in the proportion of A4, with about the area of the square it was
	static METHOD_BOX = [1.25, 0.85];
	static PAGE_BOX   = [Math.pow(2, -0.25), Math.pow(2, 0.25)];

	// the box of an element as shares of its radius across and down, or null for a round one
	static boxOf(n) {

		return n.k === 'page' || n.k === 'component' ? AppMapRenderer.PAGE_BOX : (n.k === 'method' ? AppMapRenderer.METHOD_BOX : null);
	}

	// relations between methods, components and endpoints only come forward with the zoom
	static MINOR = new Set(['method', 'component', 'api']);

	// the relations that describe how the application is built step back behind the functional ones by this share
	static QUIET = 0.4;

	// exploring shows at most this many jump markers, this many pixels away from the border of the screen
	static MARKERS     = 12;
	static MARKER_EDGE = 12;

	// the height the line of hints takes at the bottom of the map, kept free of markers
	static MARKER_HUD = 22;

	// markers at the border of the screen closer than this, across and down in pixels, collapse into one
	static MARKER_MERGE = [170, 30];

	// the distance of the outermost element when there is none, in map units
	static EMPTY_REACH = 300;

	// how far the borders of the sectors and the names of the areas lie from the centre, as a share of the distance of the
	// outermost element
	static SECTOR_REACH = 1.5;
	static NAME_RING    = 1.3;

	// a line fainter than this is not worth its rasterisation
	static MIN_ALPHA = 0.012;

	// the relations of the whole map are its most expensive layer, thousands of lines: while the view moves they come from the
	// picture of the last frame that drew them, moved and scaled along, and are drawn anew this many milliseconds after it rests
	static RELATIONS_REST = 150;

	// labels: the size on screen, the cell size of the grid that keeps them apart, and how many a frame may try and place
	static LABEL_SIZE   = 10.5;
	static LABEL_CELL   = 48;
	static LABEL_TRIES  = 900;
	static LABEL_PLACED = 400;

	// from afar only the most connected elements are named: this many where the whole map fits the screen, more as the map
	// comes closer, growing with the zoom to the power of LABEL_GROWTH, so the overview stays an overview
	static LABEL_OVERVIEW = 30;

	// the size of the name of a grouping, in pixels
	static GROUPING_NAME_SIZE = 10.5;
	static LABEL_GROWTH   = 1.5;

	// the kinds whose names matter most come first when labels compete for room
	static LABEL_RANK = { page: 0, type: 0, group: 1, external: 1, component: 2, api: 2, method: 3, view: 4, action: 4 };

	constructor(view) {

		this.view       = view;
		this.ctx        = view.ctx;
		this.colorCache = new Map();
		this.textWidths = new Map();
		this.cache      = {};

		// labels are measured on a context of their own, at their size on screen, so measuring never touches the map's state
		this.measure = document.createElement('canvas').getContext('2d');
	}

	recolor() {

		this.colorCache = new Map(this.view.nodes.map(n => [n, this.view.look.kinds[n.k] ?? '#888888']));
	}

	colorOf(n) {

		return this.colorCache.get(n) ?? this.view.look.kinds[n.k] ?? '#888888';
	}

	opColor(l) {

		const c = AppMapRenderer.OP_COLOR[l.kind];

		return c ? c[this.view.look.dark ? 1 : 0] : null;
	}

	areaColor(key, opacity) {

		return d3.hsl(AppMapRenderer.AREA_HUE[key] ?? 0, 0.75, this.view.look.dark ? 0.62 : 0.42).copy({ opacity }).formatRgb();
	}

	font(weight, size, mono = false) {

		return `${weight} ${size}px ${mono ? this.view.fonts.mono : this.view.fonts.main}`;
	}

	onScreen(n, margin = 50) {

		const t = this.view.transform;
		const x = t.applyX(n.x);
		const y = t.applyY(n.y);

		return x > -margin && x < this.view.width + margin && y > -margin && y < this.view.height + margin;
	}

	// the visible part of the map in map coordinates, with a margin, as [x0, y0, x1, y1]
	visibleRect(margin = 40) {

		const [x0, y0] = this.view.transform.invert([-margin, -margin]);
		const [x1, y1] = this.view.transform.invert([this.view.width + margin, this.view.height + margin]);

		return [x0, y0, x1, y1];
	}

	static intersects(box, rect) {

		return box[0] <= rect[2] && box[2] >= rect[0] && box[1] <= rect[3] && box[3] >= rect[1];
	}

	// what changes only with the layout is kept until the layout takes its next step
	layoutCache() {

		const layout = this.view.layout;
		const key    = `${layout.version}|${this.view.nodes.length}`;

		if (this.cache.key !== key) {
			// an application without elements still shows its areas, on a ring of a fixed size
			const reach = d3.max(this.view.nodes, n => AppMapLayout.polar(n.x, n.y)[1]) || AppMapRenderer.EMPTY_REACH;

			// the borders of the sectors reach well beyond the outermost element, the names of the areas stand between
			this.cache = { key, outer: reach * AppMapRenderer.SECTOR_REACH, names: reach * AppMapRenderer.NAME_RING };
		}

		return this.cache;
	}

	// text in a panel is drawn on the screen grid at its real pixel size: a font of a fraction of a map unit, scaled up by
	// the zoom, is placed by the browser with rounded metrics and slides off the middle of its box
	screenText(text, x, y, weight, size, mono, align) {

		const ctx = this.ctx;
		const m   = ctx.getTransform();

		ctx.save();
		ctx.setTransform(1, 0, 0, 1, 0, 0);
		ctx.font         = this.font(weight, size * m.a, mono);
		ctx.textAlign    = align;
		ctx.textBaseline = 'middle';
		ctx.fillText(text, m.a * x + m.c * y + m.e, m.b * x + m.d * y + m.f);
		ctx.restore();
	}

	draw() {

		const view    = this.view;
		const ctx     = this.ctx;
		const look    = view.look;
		const k       = view.transform.k;
		const focus   = view.spotlight ? null : view.focus;
		const lifted  = focus ? view.liftedTargets(focus) : [];
		const shown   = focus ? this.focusLinks(focus) : [];
		const cascade = focus?.k === 'type' ? AppMapAccess.cascadeOf(view.links, focus) : null;
		const near    = focus ? new Set([focus, ...view.neighbours.get(focus), ...lifted.map(([anchor]) => anchor), ...shown.flatMap(l => [l.source, l.target]), ...(cascade?.keys() ?? [])]) : null;
		const around  = focus ? view.contextAround(focus) : null;
		const rect    = this.visibleRect();

		// the labels name what deleting the type in focus takes along, too
		this.cascade = cascade;

		ctx.save();
		ctx.fillStyle = look.bg;
		ctx.fillRect(0, 0, view.width, view.height);
		ctx.translate(view.transform.x, view.transform.y);
		ctx.scale(k, k);

		this.drawAreaSectors(k);

		ctx.globalCompositeOperation = look.blend;

		if (focus) {
			this.drawTrace(k);
		} else {
			this.drawRelationsLayer(k, rect);
		}

		ctx.globalCompositeOperation = 'source-over';

		if (focus) {

			// a relation in focus is not drawn a second time as context
			this.drawContext(k, around, new Set(shown));
			this.drawFocusRelations(k, focus, shown);
		}

		if (view.strands.bundled()) {
			this.drawStrands(k, focus);
		}

		if (view.spotlight) {

			// an open page stays, greyed out with everything else
			this.drawGroupings(k, null, rect);
			this.drawElements(k, null, null, null, rect);
			view.panels.draw(this, k);

			if (view.layout.areaPlaces.size) {
				this.drawAreaNames(k);
			}

			this.greyOut();
			this.drawHeat(k, rect);
			this.drawSpotlightNames(k);

			ctx.restore();

			if (!view.nodes.length) {
				this.drawEmpty();
			}

			return;
		}

		this.drawGroupings(k, focus, rect);
		this.drawHeat(k, rect);
		this.drawFound(k, rect);
		this.drawElements(k, focus, near, around, rect);

		if (view.access && !focus) {
			this.drawLens(k, rect);
		}

		if (cascade?.size) {
			this.drawCascade(k, cascade);
		}

		if (focus) {
			this.drawNear(k, focus, near, lifted);
		}

		this.drawHotspots(k, focus, near, around);
		this.drawLabels(k, focus, lifted, around);

		// an open page lies above every hover decoration: rings, arcs and labels must not cover its structure
		view.panels.draw(this, k);
		this.drawAccess(k, rect, focus, near, around);
		this.drawRouting(k, rect, focus, near);

		if (!focus && view.layout.areaPlaces.size) {
			this.drawAreaNames(k);
		}

		ctx.restore();

		if (!view.nodes.length) {
			this.drawEmpty();
		}

		// the jump markers belong to the element held, also when it shows its inner structure and is no focus therefore, or to
		// the HTML element held in an open page
		this.drawMarkers(view.pinnedDom ? view.panels.boxTargets(view.pinnedDom) : (view.pinned ? view.liftedTargets(view.pinned) : []));
	}

	// ----- exploring: the groupings the element in focus reaches, as names that lead there -----
	// a marker stands above an element on screen, and for one beyond it where its relation leaves the screen, with an arrow
	// the way the relation runs; markers at the border that would cover each other collapse into one, named after the strongest with the number
	// of the others. The strongest relations first, as many as find room
	drawMarkers(lifted) {

		const view    = this.view;
		const ctx     = this.ctx;
		const t       = view.transform;
		const edge    = AppMapRenderer.MARKER_EDGE;
		const pad     = 9;
		const h       = 24;
		const inside  = [];
		const outside = [];

		// the row of the trail lies over the top of the map, the line of hints along its bottom; the markers keep between
		const top    = edge + (view.trail.length ? (view.trailBar.offsetHeight || 24) + 8 : 0);
		const bottom = view.height - edge - AppMapRenderer.MARKER_HUD;

		// the relations of an HTML element run as calm arcs from its box (see AppMapPanels.drawRelations); so do those of an open
		// page held as a whole, each from the box it starts at, as its card covers the lines from its middle
		const origin = view.pinnedDom ? view.panels.boxOrigin(view.pinnedDom) : null;
		const tree   = view.pinnedDom ? null : view.panels.trees.get(view.pinned);
		const open   = tree?.open ? tree : null;

		this.markers = [];

		if (!view.pinned) {
			return;
		}

		ctx.save();
		ctx.textBaseline = 'middle';
		ctx.textAlign    = 'left';

		// the name in bold, then the kind of the element (or how many more lie that way), quieter
		const nameFont = this.font(600, 12.5);
		const kindFont = this.font(400, 11.5);
		const measure  = (name, rest) => {

			ctx.font = nameFont;

			const nameWidth = ctx.measureText(name).width;

			ctx.font = kindFont;

			return [nameWidth, nameWidth + ctx.measureText(rest).width + 2 * pad];
		};

		for (const [anchor] of lifted.slice(0, AppMapRenderer.MARKERS)) {

			const [sx, sy] = t.apply([anchor.x, anchor.y]);

			if (sx >= edge && sx <= view.width - edge && sy >= top && sy <= bottom) {

				inside.push({ anchors: [anchor], x: sx, y: sy - view.shownRadius(anchor) * (AppMapRenderer.boxOf(anchor)?.[1] ?? 1) * t.k - h / 2 - 6 });

			} else {

				// where the drawn relation leaves the screen on its way to the element, else the line from the middle
				const points         = origin ? AppMapRenderer.arcPoints(origin, anchor, 0.12) : (this.boxPoints(open, anchor) ?? this.markerPoints(view.pinned, anchor));
				const [x, y, dx, dy] = this.exitOf(points, [edge, top, view.width - edge, bottom]) ?? AppMapRenderer.edgeToward(sx, sy, view, edge, top, bottom);
				const near           = outside.find(m => Math.abs(m.x - x) < AppMapRenderer.MARKER_MERGE[0] && Math.abs(m.y - y) < AppMapRenderer.MARKER_MERGE[1]);

				if (near) {
					near.anchors.push(anchor);
				} else {
					outside.push({ anchors: [anchor], x, y, arrow: AppMapRenderer.arrowToward(dx, dy) });
				}
			}
		}

		const boxes    = [];
		const expanded = [];
		const pill     = (x, y, w, nameWidth, name, rest, colour, lit) => {

			// filled in the colour of the kind, lifted off the map by a shadow; under the pointer it turns light
			ctx.beginPath();
			ctx.roundRect(x, y, w, h, h / 2);
			ctx.shadowColor   = 'rgba(0, 0, 0, 0.3)';
			ctx.shadowBlur    = 6;
			ctx.shadowOffsetY = 1;
			ctx.fillStyle     = lit ? view.look.bg : colour;
			ctx.fill();
			ctx.shadowColor   = 'transparent';
			ctx.lineWidth     = 2;
			ctx.strokeStyle   = colour;
			ctx.stroke();
			ctx.fillStyle     = lit ? colour : '#ffffff';
			ctx.font          = nameFont;
			ctx.fillText(name, x + pad, y + h / 2 + 0.5);
			ctx.globalAlpha   = 0.8;
			ctx.font          = kindFont;
			ctx.fillText(rest, x + pad + nameWidth, y + h / 2 + 0.5);
			ctx.globalAlpha   = 1;
		};

		for (const m of [...inside, ...outside]) {

			const first          = m.anchors[0];
			const name           = (m.arrow ? m.arrow + ' ' : '') + String(first.n).slice(0, 40);
			const rest           = m.anchors.length > 1 ? ` +${m.anchors.length - 1} more` : ' ' + (AppMapModel.KIND_LABEL[first.k] ?? first.k);
			const [nameWidth, w] = measure(name, rest);
			let x                = Math.min(view.width - edge - w, Math.max(edge, m.x - w / 2));
			let y                = Math.min(bottom - h, Math.max(top, m.y - h / 2));

			// a marker that would cover another moves aside within the screen, or stays away
			for (let i = 0; i < 4 && boxes.some(b => b[0] < x + w && b[2] > x && b[1] < y + h && b[3] > y); i++) {
				y = y + h + 4 > bottom - h ? y - (h + 4) * (i + 1) : y + h + 4;
			}

			if (boxes.some(b => b[0] < x + w && b[2] > x && b[1] < y + h && b[3] > y)) {
				continue;
			}

			boxes.push([x, y, x + w, y + h]);

			// a collapsed marker under the pointer opens into a column of its elements, each of which leads there
			if (m.anchors.length > 1 && view.expandedMarker === first) {

				expanded.push({ m, x, y });
				continue;
			}

			this.markers.push({ anchors: m.anchors, box: [x, y, x + w, y + h], group: first });
			pill(x, y, w, nameWidth, name, rest, view.look.kinds[first.k] ?? view.look.ring, first === view.hoveredMarker);
		}

		// the open column lies above everything else, downward where there is room, else upward
		for (const { m, x, y } of expanded) {

			const step = h + 4;
			const down = y + m.anchors.length * step <= bottom;

			m.anchors.forEach((anchor, i) => {

				const name           = (i === 0 && m.arrow ? m.arrow + ' ' : '') + String(anchor.n).slice(0, 40);
				const rest           = ' ' + (AppMapModel.KIND_LABEL[anchor.k] ?? anchor.k);
				const [nameWidth, w] = measure(name, rest);
				const px             = Math.min(view.width - edge - w, x);
				const py             = Math.max(top, down ? y + i * step : y - i * step);

				this.markers.push({ anchors: [anchor], box: [px, py, px + w, py + h], group: m.anchors[0] });
				pill(px, py, w, nameWidth, name, rest, view.look.kinds[anchor.k] ?? view.look.ring, anchor === view.hoveredMarker);
			});
		}

		ctx.restore();
	}

	// the course that leads from the element held to a grouping it reaches: the strongest relation between them as the map
	// draws it, from the side of the element held, in map units
	// the arc of the first relation an open page has from one of its boxes toward an element, as drawn by
	// AppMapPanels.drawRelations; null when the page is not open or no box leads there
	boxPoints(tree, anchor) {

		if (!tree) {
			return null;
		}

		const view  = this.view;
		const page  = view.pinned;
		const found = view.panels.relationsOf(page, tree).find(([, other]) => view.anchorOf(other) === anchor);

		if (!found) {
			return null;
		}

		const [d, other] = found;

		return AppMapRenderer.arcPoints(AppMapPanels.origin(d, view.panels.pagePanel(page, tree, view.transform.k)), other, 0.12);
	}

	markerPoints(from, anchor) {

		const view  = this.view;
		const links = view.liftedLinks(from).get(anchor) ?? [];
		const rank  = l => AppMapStrands.rankOf(l.kind);
		const lead  = [...links].sort((a, b) => rank(a) - rank(b))[0];

		if (!lead) {
			return AppMapRenderer.arcPoints(from, anchor, 0.14);
		}

		const points = this.linkPoints(lead);

		return view.anchorOf(lead.source) === anchor ? [...points].reverse() : points;
	}

	// where a course, given by its points in map units, last leaves a rectangle of the screen on its way, as [x, y] with the
	// direction it runs there; null where it never crosses the rectangle
	exitOf(course, [x0, y0, x1, y1]) {

		const t       = this.view.transform;
		const points  = course.map(p => t.apply(p));
		const inside  = ([x, y]) => x >= x0 && x <= x1 && y >= y0 && y <= y1;
		let exit      = null;

		for (let i = 1; i < points.length; i++) {

			const p = points[i - 1];
			const q = points[i];

			if (inside(p) && !inside(q)) {

				// the crossing on the segment, by halving
				let a = 0;
				let b = 1;

				for (let j = 0; j < 16; j++) {

					const m = (a + b) / 2;

					if (inside([p[0] + (q[0] - p[0]) * m, p[1] + (q[1] - p[1]) * m])) {
						a = m;
					} else {
						b = m;
					}
				}

				exit = [p[0] + (q[0] - p[0]) * a, p[1] + (q[1] - p[1]) * a, q[0] - p[0], q[1] - p[1]];
			}
		}

		return exit;
	}

	// the points of the calm arc from one element to another, as [x, y] in map units
	static arcPoints(from, to, bend) {

		const [cx, cy] = AppMapRenderer.control(from, to, bend);

		return d3.range(AppMapStrands.SAMPLES + 1).map(i => {

			const u = i / AppMapStrands.SAMPLES;

			return [(1 - u) * (1 - u) * from.x + 2 * (1 - u) * u * cx + u * u * to.x, (1 - u) * (1 - u) * from.y + 2 * (1 - u) * u * cy + u * u * to.y];
		});
	}

	// where the line from the middle of the screen toward a point beyond it leaves the screen, with its direction
	static edgeToward(sx, sy, view, edge, top, bottom) {

		const dx    = sx - view.width / 2;
		const dy    = sy - view.height / 2;
		const scale = Math.min((view.width / 2 - edge) / Math.abs(dx || 1e-9), (dy < 0 ? view.height / 2 - top : bottom - view.height / 2) / Math.abs(dy || 1e-9));

		return [view.width / 2 + dx * scale, view.height / 2 + dy * scale, dx, dy];
	}

	// the jump marker under a point of the screen, as { anchors, box, group }: one element, or several that lie the same
	// way; group names the collapsed marker it belongs to
	findMarker(px, py) {

		return (this.markers ?? []).findLast(({ box }) => px >= box[0] && px <= box[2] && py >= box[1] && py <= box[3]) ?? null;
	}

	// an arrow pointing the way of a direction on screen, in eight steps
	static arrowToward(dx, dy) {

		return ['→', '↘', '↓', '↙', '←', '↖', '↑', '↗'][(Math.round(Math.atan2(dy, dx) / (Math.PI / 4)) + 8) % 8];
	}

	// an application without types, pages or methods: the empty areas, and in their middle a word why and what to do
	static EMPTY_TEXT = [
		[600, 13,   'This application has no types, pages or methods yet'],
		[400, 11.5, 'The map shows what an application consists of. Create types in the Schema area,'],
		[400, 11.5, 'build pages in the Pages area, or import an application in Dashboard › Deployment.']
	];

	drawEmpty() {

		const ctx    = this.ctx;
		const view   = this.view;
		const x      = view.transform.applyX(0);
		const y      = view.transform.applyY(0);
		const lines  = AppMapRenderer.EMPTY_TEXT;
		const height = 20;
		const top    = y - (lines.length - 1) * height / 2;
		const width  = d3.max(lines, ([weight, size, text]) => {

			ctx.font = this.font(weight, size);

			return ctx.measureText(text).width;
		});

		// on a tile of the background, so the borders of the sectors do not run through the words
		ctx.save();
		ctx.fillStyle = view.look.bg;
		ctx.fillRect(x - width / 2 - 16, top - height, width + 32, lines.length * height + height);
		ctx.textAlign    = 'center';
		ctx.textBaseline = 'middle';

		lines.forEach(([weight, size, text], i) => {

			ctx.font      = this.font(weight, size);
			ctx.fillStyle = i ? view.look.label : view.look.labelNear;
			ctx.fillText(text, x, top + i * height + (i ? 4 : 0));
		});

		ctx.restore();
	}

	// the relations from their picture, which is drawn anew when they change, when the elements move while the layout
	// settles, when the canvas changes its size, or when the view has moved and come to rest
	drawRelationsLayer(k, rect) {

		const view  = this.view;
		const ctx   = this.ctx;
		const t     = view.transform;
		const now   = performance.now();
		const open  = [...view.panels.trees].filter(([, tree]) => tree.open).map(([page]) => page.id).join();
		const key   = [view.generation, view.layout.version, view.look.bg, view.strands.bundled(), ctx.canvas.width, ctx.canvas.height, open].join('|');
		const shot  = this.relationsShot;
		const last  = this.lastView;

		if (!last || last.x !== t.x || last.y !== t.y || last.k !== t.k) {

			this.lastView = { x: t.x, y: t.y, k: t.k };
			this.lastMove = now;
		}

		const moving  = now - this.lastMove < AppMapRenderer.RELATIONS_REST;
		const current = shot && shot.key === key && shot.paths === view.strands.paths;
		const exact   = current && shot.x === t.x && shot.y === t.y && shot.k === t.k;

		if (!exact && !(current && moving)) {
			this.shootRelations(k, rect, key);
		}

		// a picture of the map device pixel for device pixel, placed by the change of the view since it was taken
		const matrix = ctx.getTransform();
		const taken  = this.relationsShot.matrix;
		const scale  = matrix.a / taken.a;

		ctx.save();
		ctx.setTransform(1, 0, 0, 1, 0, 0);
		ctx.drawImage(this.relationsShot.canvas, matrix.e - taken.e * scale, matrix.f - taken.f * scale, this.relationsShot.canvas.width * scale, this.relationsShot.canvas.height * scale);
		ctx.restore();

		// once the view rests, the relations are drawn sharp at their new place
		if (moving) {

			clearTimeout(this.restTimer);
			this.restTimer = setTimeout(() => view.requestDraw(), AppMapRenderer.RELATIONS_REST + 10);
		}
	}

	shootRelations(k, rect, key) {

		const view   = this.view;
		const main   = this.ctx;
		const shot   = this.relationsShot ?? { canvas: document.createElement('canvas') };
		const canvas = shot.canvas;

		if (canvas.width !== main.canvas.width || canvas.height !== main.canvas.height) {

			canvas.width  = main.canvas.width;
			canvas.height = main.canvas.height;
		}

		const ctx = canvas.getContext('2d');

		ctx.setTransform(1, 0, 0, 1, 0, 0);
		ctx.clearRect(0, 0, canvas.width, canvas.height);
		ctx.setTransform(main.getTransform());
		ctx.globalCompositeOperation = view.look.blend;

		// the relations draw on the picture as they would on the map
		this.ctx = ctx;

		try {
			this.drawRelations(k, rect);
		} finally {
			this.ctx = main;
		}

		Object.assign(shot, { key, paths: view.strands.paths, matrix: main.getTransform(), x: view.transform.x, y: view.transform.y, k: view.transform.k });

		this.relationsShot = shot;
	}

	// one line per pair of elements: all relations between two elements are summed into one stroke, thicker with their
	// number and in the colour of their strongest effect; it never draws a second line beside the first
	drawRelations(k, rect) {

		const ctx     = this.ctx;
		const strands = this.view.strands;
		const bundled = strands.bundled();

		for (const p of strands.linkPairs()) {

			const l          = p.lead;
			const functional = AppMapModel.FUNCTIONAL.has(l.kind);
			const length     = Math.hypot(l.target.x - l.source.x, l.target.y - l.source.y);
			const fade       = bundled && functional ? Math.max(0.35, 1 - length / 2400) : Math.pow(Math.max(0, 1 - length / 700), 1.6);
			const lod        = AppMapRenderer.MINOR.has(l.source.k) && AppMapRenderer.MINOR.has(l.target.k) ? Math.max(0.25, Math.min(1, (k - 0.35) / 0.9)) : 1;
			const alpha      = Math.min(0.45, 0.13 + p.w / 16) * fade * lod * (l.kind === 'reads' ? 0.75 : 1) * (functional ? 1 : AppMapRenderer.QUIET);

			if (alpha < AppMapRenderer.MIN_ALPHA) {
				continue;
			}

			// a page that is open draws the relations of its repeaters and action mappings from their boxes itself
			if (p.links.every(x => this.view.panels.leavesFromBox(x))) {
				continue;
			}

			const course = strands.pathOf(l);

			if (!AppMapRenderer.intersects(course.box, rect)) {
				continue;
			}

			// the functional relations carry the colour of their effect; how the application is built stays a fine grey trace
			ctx.globalAlpha = alpha;
			ctx.strokeStyle = functional ? (this.opColor(l) ?? this.colorOf(l.source)) : this.view.look.trace;
			ctx.lineWidth   = (functional ? 0.8 * AppMapStrands.pairWidth(p) : 0.5) / k;
			ctx.stroke(course.path);
		}
	}

	// in focus the rest of the web stays as a faint trace, all in one path
	drawTrace(k) {

		const ctx = this.ctx;

		const trace = new Path2D();

		// along the same courses as without focus, so the web does not change its shape under the pointer
		for (const p of this.view.strands.linkPairs()) {
			trace.addPath(this.view.strands.pathOf(p.lead).path);
		}

		ctx.globalAlpha = 0.05;
		ctx.strokeStyle = this.view.look.trace;
		ctx.lineWidth   = 0.6 / k;
		ctx.stroke(trace);
	}

	// one step beyond the neighbours: how they hang together with the rest of the map, thin and quiet behind the relations of
	// the element in focus
	drawContext(k, around, covered) {

		const ctx = this.ctx;

		ctx.save();
		ctx.lineCap = 'round';

		for (const p of AppMapStrands.pairsOf(around.beyond.filter(l => !covered.has(l)))) {

			const l = p.lead;

			ctx.globalAlpha = 0.28;
			ctx.strokeStyle = this.opColor(l) ?? this.colorOf(l.source);
			ctx.lineWidth   = 0.5 * AppMapStrands.pairWidth(p) / k;
			ctx.stroke(this.linkCourse(l).path);
		}

		ctx.restore();
	}

	// the relations of the element in focus: its own, and for the anchor of a grouping those of its whole grouping, except
	// the scaffolding
	focusLinks(focus) {

		const own = this.view.links.filter(l => (l.source === focus || l.target === focus) && !AppMapModel.STRUCTURE.has(l.kind));

		return [...new Set([...own, ...[...this.view.liftedLinks(focus).values()].flat()])];
	}

	// the relations in focus, one line per pair of elements, each on exactly the course it has on the map (bundled on its way
	// where it is functional, straight otherwise), so what lights up is the line that was there; with a casing, in the
	// colour of its strongest effect or of the element on the other side, and an arrowhead where it arrives
	drawFocusRelations(k, focus, links) {

		const ctx  = this.ctx;
		const view = this.view;
		const look = view.look;
		const home = view.anchorOf(focus);
		const near = n => n === focus || (home === focus && view.anchorOf(n) === focus);

		ctx.save();
		ctx.lineCap = 'round';

		for (const p of AppMapStrands.pairsOf(links)) {

			const l        = p.lead;
			const to       = near(l.source) ? l.target : l.source;
			const inferred = p.links.every(x => x.inferred);
			const course   = this.linkCourse(l);
			const width    = (inferred ? 0.7 : 0.8) * 1.6 * AppMapStrands.pairWidth(p) / k;
			const colour   = this.gapColor(l) ?? this.schemaColor(l) ?? this.opColor(l) ?? look.kinds[to.k] ?? look.trace;
			const radius   = Math.max(view.shownRadius(l.target), 2 / k);

			// a casing in the colour of the background lifts the relation off everything behind it
			ctx.globalAlpha = 0.85;
			ctx.strokeStyle = look.halo;
			ctx.lineWidth   = width + 2.4 / k;
			ctx.setLineDash([]);
			ctx.stroke(course.path);

			ctx.globalAlpha = 0.95;
			ctx.strokeStyle = colour;
			ctx.lineWidth   = width;
			ctx.setLineDash(inferred ? [2.2 / k, 2.6 / k] : []);
			ctx.stroke(course.path);

			// the direction: an arrowhead where the relation arrives, just outside the element it points to; the data model
			// between types has none, it says its multiplicity instead
			if (l.kind !== 'relates') {

				ctx.setLineDash([]);
				ctx.fillStyle = colour;
				this.arrowFrom(course.toward(l.target, radius * 2), l.target, radius, width, k);

			} else if (l.x?.m) {

				this.drawMultiplicity(l, k, colour);
			}
		}

		ctx.restore();
	}

	// with the access lens on, a relation of a page to a type that the role may not use as the page does is red
	gapColor(l) {

		const gap = this.view.access?.gaps.some(g => g.page === l.source && g.type === l.target && AppMapAccess.NEEDS[l.kind] === g.need);

		return gap ? AppMapRenderer.SCHEMA_COLOR.gap[this.view.look.dark ? 1 : 0] : null;
	}

	// a schema relationship that deletes along is red, one that passes rights on orange, as in the schema editor
	schemaColor(l) {

		const key = l.kind !== 'relates' || !l.x ? null : l.x.cd ? 'cascade' : l.x.pp ? 'passes' : null;

		return key ? AppMapRenderer.SCHEMA_COLOR[key][this.view.look.dark ? 1 : 0] : null;
	}

	// the multiplicity of a schema relationship at both its ends, e.g. 1 at the source and * at the target
	drawMultiplicity(l, k, colour) {

		const ctx    = this.ctx;
		const ends   = l.x.m.split(':');
		const points = this.linkPoints(l);
		const last   = points.length - 1;

		ctx.save();
		ctx.font         = this.font(600, 10 / k);
		ctx.textAlign    = 'center';
		ctx.textBaseline = 'middle';
		ctx.lineWidth    = 3 / k;
		ctx.globalAlpha  = 1;

		for (const [end, text, [x, y], [nx, ny]] of [[l.source, ends[0], points[0], points[Math.min(1, last)]], [l.target, ends[1], points[last], points[Math.max(0, last - 1)]]]) {

			// beside the end, a little way along the relation
			const gap = Math.max(this.view.shownRadius(end), 2 / k) + 9 / k;
			const d   = Math.hypot(nx - x, ny - y) || 1;
			const tx  = x + (nx - x) / d * gap;
			const ty  = y + (ny - y) / d * gap;

			ctx.strokeStyle = this.view.look.halo;
			ctx.strokeText(text, tx, ty);
			ctx.fillStyle   = colour;
			ctx.fillText(text, tx, ty);
		}

		ctx.restore();
	}

	// what deleting an object of the type in focus deletes with it: a red dashed ring around each of those types, wherever
	// they are, and their relationships in red
	drawCascade(k, reached) {

		const ctx    = this.ctx;
		const colour = AppMapRenderer.SCHEMA_COLOR.cascade[this.view.look.dark ? 1 : 0];

		ctx.save();
		ctx.strokeStyle = colour;

		for (const [type, step] of reached) {

			ctx.globalAlpha = 0.9;
			ctx.lineWidth   = 1.6 / k;
			ctx.setLineDash(step.maybe ? [2 / k, 3 / k] : [5 / k, 3 / k]);
			ctx.beginPath();
			ctx.arc(type.x, type.y, Math.max(this.view.shownRadius(type), 2 / k) + 6 / k, 0, Math.PI * 2);
			ctx.stroke();

			ctx.globalAlpha = 0.7;
			ctx.lineWidth   = 1.4 / k;
			ctx.setLineDash([]);
			ctx.stroke(this.linkCourse(step.l).path);
		}

		ctx.restore();
	}

	// the access lens: around every type the schema governs the rights the role has on all its objects, a third of a ring
	// each (read, write, delete), and a red ring at every page with a gap; its relations to the types turn red in focus
	drawLens(k, rect) {

		const view   = this.view;
		const ctx    = this.ctx;
		const access = view.access;
		const dark   = view.look.dark ? 1 : 0;
		const third  = Math.PI * 2 / 3;

		ctx.save();
		ctx.lineCap = 'butt';

		for (const type of access.governed) {

			const own = access.rights.get(type) ?? new Map();
			const r   = Math.max(view.shownRadius(type), 2 / k) + 3.5 / k;

			if (type.x + r < rect[0] || type.x - r > rect[2] || type.y + r < rect[1] || type.y - r > rect[3]) {
				continue;
			}

			AppMapAccess.RIGHTS.forEach((right, i) => {

				const start = -Math.PI / 2 + i * third + 0.12;

				ctx.globalAlpha = own.has(right) ? 1 : 0.18;
				ctx.strokeStyle = AppMapRenderer.RIGHT_COLOR[right][dark];
				ctx.lineWidth   = 2.4 / k;
				ctx.beginPath();
				ctx.arc(type.x, type.y, r, start, start + third - 0.24);
				ctx.stroke();
			});
		}

		const gap = AppMapRenderer.SCHEMA_COLOR.gap[dark];

		ctx.strokeStyle = gap;
		ctx.lineCap     = 'round';

		for (const page of new Set(access.gaps.map(gap => gap.page))) {

			ctx.globalAlpha = 0.85;
			ctx.lineWidth   = 2 / k;
			ctx.beginPath();
			ctx.arc(page.x, page.y, Math.max(view.shownRadius(page), 2 / k) * Math.SQRT2 + 4 / k, 0, Math.PI * 2);
			ctx.stroke();
		}

		ctx.restore();
	}

	// the course of a relation as the map draws it (see AppMapStrands.pathOf), with its points from source to target;
	// toward(end, gap) is a point on it before that end, for the arrowhead
	linkCourse(l) {

		return AppMapRenderer.along({ path: this.view.strands.pathOf(l).path, points: this.linkPoints(l) });
	}

	// the points of the course of a relation, from its source to its target, in map units
	linkPoints(l) {

		const bundled = this.view.strands.bundled() ? this.view.strands.bundles.get(l) : null;

		return bundled ?? [[l.source.x, l.source.y], [l.target.x, l.target.y]];
	}

	static along({ path, points }) {

		return {
			path,
			toward: (end, gap) => {

				const near = (p) => Math.hypot(p[0] - end.x, p[1] - end.y);
				const seq  = near(points[0]) < near(points[points.length - 1]) ? points : [...points].reverse();

				return seq.find(p => near(p) > gap) ?? seq[seq.length - 1];
			}
		};
	}

	// a calm arc from one element to another, bent to the side by the given share of its length
	static arc(from, to, bend) {

		const path     = new Path2D();
		const [cx, cy] = AppMapRenderer.control(from, to, bend);

		path.moveTo(from.x, from.y);
		path.quadraticCurveTo(cx, cy, to.x, to.y);

		return path;
	}

	// the control point of the arc from one element to another
	static control(from, to, bend) {

		return [(from.x + to.x) / 2 - (to.y - from.y) * bend, (from.y + to.y) / 2 + (to.x - from.x) * bend];
	}

	// an arrowhead at an element, pointing the way from the given point, set back by the radius of the element
	arrowFrom([cx, cy], to, radius, width, k) {

		const ctx      = this.ctx;
		const angle    = Math.atan2(to.y - cy, to.x - cx);
		const length   = 4.5 / k + width * 2;
		const tipX     = to.x - Math.cos(angle) * (radius + 1.5 / k);
		const tipY     = to.y - Math.sin(angle) * (radius + 1.5 / k);

		ctx.beginPath();
		ctx.moveTo(tipX, tipY);
		ctx.lineTo(tipX - Math.cos(angle - 0.42) * length, tipY - Math.sin(angle - 0.42) * length);
		ctx.lineTo(tipX - Math.cos(angle + 0.42) * length, tipY - Math.sin(angle + 0.42) * length);
		ctx.closePath();
		ctx.fill();
	}

	drawElements(k, focus, near, around, rect) {

		const ctx  = this.ctx;
		const look = this.view.look;

		for (const n of this.view.nodes) {

			// never smaller than two pixels on screen, and nothing fades from afar: every element stays visible
			const r = Math.max(this.view.shownRadius(n), 2 / k);

			if ((focus && near.has(n)) || n.x + r < rect[0] || n.x - r > rect[2] || n.y + r < rect[1] || n.y - r > rect[3]) {
				continue;
			}

			ctx.globalAlpha = this.presence(n, focus, around);
			ctx.fillStyle   = this.colorOf(n);
			this.shape(n, r);
			ctx.fill();

			// a fine outline sets every element off against the lines behind it
			ctx.lineWidth   = 0.6 / k;
			ctx.strokeStyle = look.outline;
			ctx.stroke();

			// a type that resource access grants open to REST wears a ring in the colour of the REST API: solid when
			// anonymous users may use it, dashed when only authenticated users may
			if (n.rest) {

				ctx.beginPath();
				ctx.arc(n.x, n.y, r + 2.2 / k, 0, Math.PI * 2);
				ctx.setLineDash(n.rest.pub ? [] : [2 / k, 1.6 / k]);
				ctx.lineWidth   = 1 / k;
				ctx.strokeStyle = this.areaColor('api', 0.9);
				ctx.stroke();
				ctx.setLineDash([]);
			}
		}

		ctx.globalAlpha = 1;
	}

	// who may see a page or shared component, as a badge on the upper right corner of its square: a filled tile in the
	// colour of who sees it (red for public users, blue for authenticated users, grey for nobody but admins and grants)
	// with the icon of the Pages area in white; the pointer on the badge shows the rights (see AppMapView.describeAccess)
	drawAccess(k, rect, focus, near, around) {

		const view = this.view;
		const ctx  = this.ctx;
		const dark = view.look.dark ? 1 : 0;

		this.accessBoxes = [];
		this.accessIcons ??= Object.fromEntries(Object.entries(AppMapRenderer.ACCESS_ICON).map(([key, d]) => [key, new Path2D(d)]));

		ctx.save();
		ctx.lineCap  = 'round';
		ctx.lineJoin = 'round';

		for (const n of view.nodes) {

			const badge = this.accessBadge(n, k);
			const half  = Math.max(view.shownRadius(n), 2 / k) * AppMapRenderer.PAGE_BOX[1];

			if (!badge || n.x + half < rect[0] || n.x - half > rect[2] || n.y + half < rect[1] || n.y - half > rect[3]) {
				continue;
			}

			const shape  = n.acc.pub && n.acc.auth ? 'open' : (!n.acc.pub && !n.acc.auth ? 'locked' : 'key');
			const colour = AppMapRenderer.ACCESS_COLOR[n.acc.pub ? 'pub' : (n.acc.auth ? 'auth' : 'none')][dark];
			const { x, y, size } = badge;

			// in focus the badges step back with their pages
			ctx.globalAlpha = focus && !near.has(n) ? (around.nodes.has(n) ? 0.55 : 0.15) : 1;

			this.accessBoxes.push([n, x, y, x + size, y + size]);

			// the tile, with a rim in the colour of the background so it stands off the page and the lines behind it
			ctx.beginPath();
			ctx.roundRect(x, y, size, size, size * 0.22);
			ctx.lineWidth   = 2 / k;
			ctx.strokeStyle = view.look.halo;
			ctx.stroke();
			ctx.fillStyle   = colour;
			ctx.fill();

			ctx.save();
			ctx.translate(x + size * 0.17, y + size * 0.15);
			ctx.scale(size * 0.66 / 24, size * 0.66 / 24);
			ctx.lineWidth   = 2.6;
			ctx.strokeStyle = '#ffffff';
			ctx.stroke(this.accessIcons[shape]);
			ctx.restore();
		}

		ctx.restore();
	}

	// where the access badge of an element sits at a zoom, or null if it carries none there
	accessBadge(n, k) {

		const r      = Math.max(this.view.shownRadius(n), 2 / k);
		const [w, h] = AppMapRenderer.boxOf(n) ?? [1, 1];

		if (!n.acc || !(2 * r * w * k >= AppMapRenderer.ACCESS_FROM || n.acc.pub)) {
			return null;
		}

		const size = Math.min(AppMapRenderer.ACCESS_SIZE, Math.max(AppMapRenderer.ACCESS_SMALL, 2 * r * w * k * AppMapRenderer.ACCESS_SHARE)) / k;

		return { x: n.x + r * w - size * 0.35, y: n.y - r * h - size * 0.65, size };
	}

	// what a page is for beyond its content, as small tags in a row below it: the start page, the errors it is shown for, a
	// content type other than HTML; the start page always, the others once the page is large enough on screen
	drawRouting(k, rect, focus, near) {

		const view  = this.view;
		const ctx   = this.ctx;
		const start = view.startPage();
		const dark  = view.look.dark ? 1 : 0;
		const gap   = 2 / k;

		ctx.save();
		ctx.font         = this.font(600, 9 / k);
		ctx.textAlign    = 'center';
		ctx.textBaseline = 'middle';

		for (const n of view.nodes) {

			if (n.k !== 'page' || (focus && !near.has(n)) || n.x < rect[0] || n.x > rect[2] || n.y < rect[1] || n.y > rect[3]) {
				continue;
			}

			const r    = Math.max(view.shownRadius(n), 2 / k);
			const page = this.colorOf(n);
			const tags = [
				n === start ? ['start', AppMapRenderer.ROUTING_COLOR.start[dark]] : null,
				...(r * k >= 4 ? [
					n.err ? [String(n.err), AppMapRenderer.ROUTING_COLOR.error[dark]] : null,
					n.ct ? [AppMapRenderer.shortType(n.ct), page] : null
				] : [])
			].filter(Boolean).map(([text, colour]) => ({ text, colour, width: this.textWidth(text, 600) * 9 / AppMapRenderer.LABEL_SIZE / k + 6 / k }));

			if (!tags.length) {
				continue;
			}

			const y = n.y + r * (AppMapRenderer.boxOf(n)?.[1] ?? 1) + 7 / k;
			let x   = n.x - (d3.sum(tags, t => t.width) + gap * (tags.length - 1)) / 2;

			ctx.globalAlpha = 0.95;

			for (const tag of tags) {

				ctx.fillStyle = tag.colour;
				ctx.beginPath();
				ctx.roundRect(x, y - 5.5 / k, tag.width, 11 / k, 5.5 / k);
				ctx.fill();
				ctx.fillStyle = view.look.bg;
				ctx.fillText(tag.text, x + tag.width / 2, y);

				x += tag.width + gap;
			}
		}

		ctx.restore();
	}

	// a content type in short, as "json" for application/json
	static shortType(contentType) {

		return contentType.split(';')[0].split('/').pop().replace(/^x-/, '');
	}

	// the page or shared component whose access icon lies under a point of the map
	findAccess(x, y) {

		const pad = 2 / this.view.transform.k;

		return (this.accessBoxes ?? []).find(([, x0, y0, x1, y1]) => x >= x0 - pad && x <= x1 + pad && y >= y0 - pad && y <= y1 + pad)?.[0] ?? null;
	}

	// the plain shape of an element: pages and components a sheet, methods a rectangle on its side, endpoints a triangle,
	// everything else round
	shape(n, r) {

		const ctx = this.ctx;

		ctx.beginPath();

		if (n.k === 'page' || n.k === 'component') {

			const [w, h] = AppMapRenderer.PAGE_BOX;

			// rounded as the card it opens to (see AppMapPanels.drawCard), so the card covers it to the corners
			ctx.roundRect(n.x - r * w, n.y - r * h, 2 * r * w, 2 * r * h, 2 * r * Math.min(w, h) * 0.035);

		} else if (n.k === 'method') {

			const [w, h] = AppMapRenderer.METHOD_BOX;

			ctx.roundRect(n.x - r * w, n.y - r * h, 2 * r * w, 2 * r * h, r * 0.12);

		} else if (n.k === 'api') {

			ctx.moveTo(n.x, n.y - r * 1.2);
			ctx.lineTo(n.x + r * 1.1, n.y + r * 0.8);
			ctx.lineTo(n.x - r * 1.1, n.y + r * 0.8);
			ctx.closePath();

		} else {

			ctx.arc(n.x, n.y, r, 0, Math.PI * 2);
		}
	}

	// in focus an element keeps its size: a ring marks it, the groupings it reaches carry a ring too, the rest steps back
	drawNear(k, focus, near, lifted) {

		const ctx     = this.ctx;
		const look    = this.view.look;
		const targets = new Set(lifted.map(([anchor]) => anchor));

		for (const n of near) {

			const ringed = n === focus || targets.has(n);

			this.focusShape(n, Math.max(this.view.shownRadius(n), 2 / k));
			ctx.fillStyle   = look.kinds[n.k];
			ctx.fill();
			ctx.lineWidth   = (n === focus ? 2.2 : (ringed ? 1.6 : 1)) / k;
			ctx.strokeStyle = ringed ? look.ring : look.outline;
			ctx.stroke();
		}
	}

	focusShape(n, r) {

		const ctx = this.ctx;

		ctx.beginPath();

		if (AppMapRenderer.boxOf(n)) {

			// the shape of the element itself, a little larger
			const [w, h] = AppMapRenderer.boxOf(n);

			ctx.roundRect(n.x - r * (w + 0.1), n.y - r * (h + 0.1), 2 * r * (w + 0.1), 2 * r * (h + 0.1), n.k === 'method' ? r * 0.15 : 0);

		} else if (n.k === 'api') {

			ctx.moveTo(n.x, n.y - r * 1.25);
			ctx.lineTo(n.x + r * 1.15, n.y + r * 0.85);
			ctx.lineTo(n.x - r * 1.15, n.y + r * 0.85);
			ctx.closePath();

		} else if (n.k === 'group') {

			this.polygon(n, 6, i => Math.PI / 3 * i + Math.PI / 6, () => r * 1.15);

		} else if (n.k === 'external') {

			this.polygon(n, 10, i => Math.PI / 5 * i - Math.PI / 2, i => i % 2 ? r * 0.55 : r * 1.25);

		} else {

			ctx.arc(n.x, n.y, r, 0, Math.PI * 2);
		}
	}

	polygon(n, corners, angleOf, radiusOf) {

		const ctx = this.ctx;

		for (let i = 0; i < corners; i++) {

			const a  = angleOf(i);
			const rr = radiusOf(i);

			ctx[i ? 'lineTo' : 'moveTo'](n.x + Math.cos(a) * rr, n.y + Math.sin(a) * rr);
		}

		ctx.closePath();
	}

	// ----- labels: as many names as fit without covering each other, the most important first -----
	// in focus the element, the groupings it reaches and its neighbours are named first, then the elements one step further
	labelCandidates(focus, lifted, around) {

		const view  = this.view;
		const first = [];

		if (focus) {

			return [...new Set([focus, ...lifted.map(([anchor]) => anchor), ...view.neighbours.get(focus), ...(this.cascade?.keys() ?? []), ...around.nodes])];
		}

		if (view.hoveredStrand) {

			first.push(...view.hoveredStrand.ends);
		}

		// then what the global search found, the hotspots and the most important pages, whatever else competes for room
		first.push(...view.found?.elements ?? [], ...view.analysis.hotspots.keys(), ...view.keyPages);

		return [...new Set([...first, ...this.labelOrder()])];
	}

	// the most connected elements first, the larger the more relations they have; of the same size pages, types and groups
	// before methods and repeaters; changes only with the relations, so it is sorted once per relation set
	labelOrder() {

		const view = this.view;
		const key  = view.generation;

		if (this.order?.key !== key) {

			const rank = n => AppMapRenderer.LABEL_RANK[n.k] ?? 5;

			this.order = { key, nodes: view.nodes.slice().sort((a, b) => view.radiusOf(b) - view.radiusOf(a) || rank(a) - rank(b)) };
		}

		return this.order.nodes;
	}

	// the width of a label on screen, measured once per text and weight
	textWidth(text, weight) {

		const key = `${weight}|${text}`;
		let width = this.textWidths.get(key);

		if (width === undefined) {

			this.measure.font = this.font(weight, AppMapRenderer.LABEL_SIZE);
			width             = this.measure.measureText(text).width;

			this.textWidths.set(key, width);
		}

		return width;
	}

	drawLabels(k, focus, lifted, around) {

		const ctx       = this.ctx;
		const view      = this.view;
		const look      = view.look;
		const t         = view.transform;
		const weight    = focus ? 500 : 400;
		const size      = AppMapRenderer.LABEL_SIZE;
		const cell      = AppMapRenderer.LABEL_CELL;
		const grid      = new Map();
		const near      = focus ? new Set([focus, ...lifted.map(([anchor]) => anchor), ...view.neighbours.get(focus)]) : null;
		const covered   = (x0, y0, x1, y1) => {

			for (let cx = Math.floor(x0 / cell); cx <= Math.floor(x1 / cell); cx++) {

				for (let cy = Math.floor(y0 / cell); cy <= Math.floor(y1 / cell); cy++) {

					for (const b of grid.get(cx * 65536 + cy) ?? []) {

						if (b[0] < x1 && b[2] > x0 && b[1] < y1 && b[3] > y0) {
							return true;
						}
					}
				}
			}

			return false;
		};
		const occupy    = box => {

			for (let cx = Math.floor(box[0] / cell); cx <= Math.floor(box[2] / cell); cx++) {

				for (let cy = Math.floor(box[1] / cell); cy <= Math.floor(box[3] / cell); cy++) {

					const key   = cx * 65536 + cy;
					const boxes = grid.get(key);

					if (boxes) {
						boxes.push(box);
					} else {
						grid.set(key, [box]);
					}
				}
			}
		};
		let tries       = 0;
		let placed      = 0;

		// the names of the areas are there first: the names of the elements give way to them
		if (!focus && view.layout.areaPlaces.size) {

			for (const { box } of this.areaNamePlaces(k)) {
				occupy([t.applyX(box[0]), t.applyY(box[1]), t.applyX(box[2]), t.applyY(box[3])]);
			}

			// and so do the names of the groupings, a section of pages among them
			this.groupingNamePlaces(k).forEach(({ box }) => occupy(box));
		}

		ctx.font         = this.font(weight, size / k);
		ctx.textBaseline = 'middle';
		ctx.textAlign    = 'left';
		ctx.lineWidth    = 3 / k;

		const budget = focus ? AppMapRenderer.LABEL_PLACED : Math.min(AppMapRenderer.LABEL_PLACED, Math.round(AppMapRenderer.LABEL_OVERVIEW * Math.max(1, k / view.baseK) ** AppMapRenderer.LABEL_GROWTH));

		for (const n of this.labelCandidates(focus, lifted, around)) {

			if (tries >= AppMapRenderer.LABEL_TRIES || placed >= budget) {
				break;
			}

			// an element with a jump marker is named by the marker
			if (!this.onScreen(n, 0) || view.panels.covers(n.x, n.y) || this.markers?.some(m => m.anchors.includes(n))) {
				continue;
			}

			tries++;

			const text    = String(n.n).length > 32 ? String(n.n).slice(0, 31) + '…' : String(n.n);
			const inFocus = near?.has(n);
			const badge   = this.accessBadge(n, k);
			// right of the element's own shape, in focus right of the slightly larger focus shape (see focusShape)
			const beside  = Math.max(view.shownRadius(n), 2 / k) * ((AppMapRenderer.boxOf(n)?.[0] ?? 1) + (inFocus ? 0.1 : 0)) + 4 / k;
			const offset  = badge ? Math.max(beside, badge.x + badge.size - n.x + 3 / k) : beside;
			const sx      = t.applyX(n.x + offset);
			const sy      = t.applyY(n.y);
			const bold    = view.hotspotOf(n) || view.keyPages.has(n) ? 650 : weight;
			const box     = [sx, sy - size * 0.6, sx + this.textWidth(text, bold), sy + size * 0.6];

			if (n !== focus && covered(...box)) {
				continue;
			}

			occupy(box);
			placed++;

			// a hotspot and a key page are named in bold and dark, so the overview reads as what matters in the application
			ctx.font        = this.font(bold, size / k);
			ctx.strokeStyle = look.halo;
			ctx.strokeText(text, n.x + offset, n.y);
			ctx.fillStyle   = n === focus || bold !== weight ? look.labelFocus : (inFocus ? look.labelNear : look.label);
			ctx.fillText(text, n.x + offset, n.y);
		}
	}

	// what the global search found lies on a mark as from a highlighter, large enough to be seen from afar
	// the activity lens: a glow behind every measured element, as wide and as strong as it is used or failing; its size is in
	// pixels, so the heat shows in the overview as close up
	drawHeat(k, rect) {

		const activity = this.view.activity;
		const ctx      = this.ctx;

		if (!activity) {
			return;
		}

		const colour = d3.color(AppMapRenderer.HEAT_COLOUR[this.view.look.dark ? 1 : 0]);

		ctx.save();

		for (const [n, heat] of activity.heat) {

			const inner = Math.max(this.view.shownRadius(n) * (AppMapRenderer.boxOf(n)?.[0] ?? 1), 2 / k);
			const r     = inner + (AppMapRenderer.HEAT_GLOW[0] + AppMapRenderer.HEAT_GLOW[1] * heat) / k;

			if (n.x + r < rect[0] || n.x - r > rect[2] || n.y + r < rect[1] || n.y - r > rect[3]) {
				continue;
			}

			const gradient = ctx.createRadialGradient(n.x, n.y, inner, n.x, n.y, r);

			gradient.addColorStop(0, colour.copy({ opacity: 0.15 + 0.45 * heat }).formatRgb());
			gradient.addColorStop(1, colour.copy({ opacity: 0 }).formatRgb());

			ctx.fillStyle = gradient;
			ctx.beginPath();
			ctx.arc(n.x, n.y, r, 0, Math.PI * 2);
			ctx.fill();
		}

		ctx.restore();
	}

	// everything drawn so far loses its colour and steps back behind a veil of the background, in two passes over the canvas
	greyOut() {

		const ctx    = this.ctx;
		const canvas = this.view.canvas;

		ctx.save();
		ctx.setTransform(1, 0, 0, 1, 0, 0);
		ctx.globalCompositeOperation = 'saturation';
		ctx.fillStyle                = '#808080';
		ctx.fillRect(0, 0, canvas.width, canvas.height);
		ctx.globalCompositeOperation = 'source-over';
		ctx.globalAlpha              = AppMapRenderer.SPOTLIGHT_VEIL;
		ctx.fillStyle                = this.view.look.bg;
		ctx.fillRect(0, 0, canvas.width, canvas.height);
		ctx.restore();
	}

	// the names of the most used elements on screen, each with its number of requests or runs; a name that would cover one already
	// placed gives way
	drawSpotlightNames(k) {

		const view   = this.view;
		const ctx    = this.ctx;
		const look   = view.look;
		const t      = view.transform;
		const size   = AppMapRenderer.LABEL_SIZE;
		const placed = [];
		const names  = view.activity ? [...view.activity.heat].filter(([n]) => this.onScreen(n, 0)).sort((a, b) => b[1] - a[1]).slice(0, AppMapRenderer.SPOTLIGHT_NAMES)
			.map(([n]) => [n, AppMapView.describeUse(n)]) : [];

		ctx.save();
		ctx.textBaseline = 'middle';
		ctx.textAlign    = 'left';
		ctx.lineWidth    = 3 / k;

		for (const [n, value] of names) {

			const name   = String(n.n).length > 32 ? String(n.n).slice(0, 31) + '…' : String(n.n);
			const offset = Math.max(view.shownRadius(n), 2 / k) * (AppMapRenderer.boxOf(n)?.[0] ?? 1) + 6 / k;
			const sx     = t.applyX(n.x + offset);
			const sy     = t.applyY(n.y);
			const width  = this.textWidth(name, 650) + 6 + this.textWidth(value, 400);
			const box    = [sx, sy - size * 0.7, sx + width, sy + size * 0.7];

			if (placed.some(b => b[0] < box[2] && b[2] > box[0] && b[1] < box[3] && b[3] > box[1])) {
				continue;
			}

			placed.push(box);

			ctx.font        = this.font(650, size / k);
			ctx.strokeStyle = look.halo;
			ctx.strokeText(name, n.x + offset, n.y);
			ctx.fillStyle   = look.labelFocus;
			ctx.fillText(name, n.x + offset, n.y);

			const after = n.x + offset + (this.textWidth(name, 650) + 6) / k;

			ctx.font = this.font(400, size / k);
			ctx.strokeText(value, after, n.y);
			ctx.fillStyle = look.label;
			ctx.fillText(value, after, n.y);
		}

		ctx.restore();
	}

	// how strongly an element shows: in focus the elements one step beyond the neighbours stay half visible and the rest
	// steps back; so does what the global search did not find, what the role of the access lens does not reach, and what
	// the activity lens did not measure
	presence(n, focus, around) {

		const view = this.view;

		if (focus) {
			return around.nodes.has(n) ? 0.55 : 0.08;
		}

		if (view.found && !view.found.elements.has(n)) {
			return 0.3;
		}

		if (view.access && !AppMapAccess.reaches(view.access, n)) {
			return 0.15;
		}

		if (view.activity && !view.activity.heat.has(n)) {
			return 0.55;
		}

		return 1;
	}

	drawFound(k, rect) {

		const found = this.view.found;
		const ctx   = this.ctx;

		if (!found) {
			return;
		}

		ctx.save();
		ctx.fillStyle   = this.view.look.found;
		ctx.globalAlpha = 0.75;

		for (const n of found.elements) {

			const r = Math.max(this.view.shownRadius(n) * (AppMapRenderer.boxOf(n)?.[0] ?? 1), 2 / k) + 7 / k;

			if (n.x + r < rect[0] || n.x - r > rect[2] || n.y + r < rect[1] || n.y - r > rect[3]) {
				continue;
			}

			ctx.beginPath();
			ctx.arc(n.x, n.y, r, 0, Math.PI * 2);
			ctx.fill();
		}

		ctx.restore();
	}

	// a hotspot wears a quiet ring in the colour of its area, at every zoom; in focus only where it belongs to what is shown
	drawHotspots(k, focus, near, around) {

		const ctx = this.ctx;

		ctx.save();
		ctx.lineWidth = 1.4 / k;

		for (const n of this.view.analysis.hotspots.keys()) {

			if (!this.onScreen(n, 40) || (focus && !near.has(n) && !around.nodes.has(n))) {
				continue;
			}

			ctx.globalAlpha = 0.7;
			ctx.strokeStyle = this.areaColor(n.c?.[0], 1);
			ctx.beginPath();
			ctx.arc(n.x, n.y, Math.max(this.view.shownRadius(n), 2 / k) + 5 / k, 0, Math.PI * 2);
			ctx.stroke();
		}

		ctx.restore();
	}

	// ----- the Structr areas -----
	// the sectors of the areas: only their borders, from the centre to beyond the outermost element, halfway between two rays
	drawAreaSectors(k) {

		if (!this.view.layout.areaPlaces.size) {
			return;
		}

		const ctx   = this.ctx;
		const outer = this.layoutCache().outer;

		ctx.save();
		ctx.strokeStyle = this.view.look.column;
		ctx.globalAlpha = 0.32;
		ctx.lineWidth   = 1.2 / k;
		ctx.beginPath();

		for (const s of this.view.layout.sectors.values()) {

			const b = s.angle + s.half;

			ctx.moveTo(0, 0);
			ctx.lineTo(...AppMapLayout.point(b, outer));
		}

		ctx.stroke();
		ctx.restore();
	}

	// the names of the areas on a common ring near the rim, and the names of the groupings
	drawAreaNames(k) {

		const ctx    = this.ctx;
		const view   = this.view;
		const counts = d3.rollup(view.nodes, v => v.length, n => n.c?.[0]);

		ctx.save();
		ctx.globalAlpha = 0.6;
		ctx.fillStyle   = view.look.column;
		ctx.beginPath();
		ctx.arc(0, 0, 4 / k, 0, Math.PI * 2);
		ctx.fill();

		this.drawGroupingNames(k);

		ctx.textAlign    = 'center';
		ctx.textBaseline = 'middle';

		for (const { key, label, x, y } of this.areaNamePlaces(k)) {

			const count = counts.get(key) ?? 0;

			ctx.globalAlpha = count ? 0.85 : 0.4;
			ctx.fillStyle   = this.areaColor(key, 1);
			ctx.font        = this.font(600, 18 / k);
			ctx.fillText(label, x, y);
			ctx.globalAlpha = count ? 0.6 : 0.4;
			ctx.font        = this.font(400, 11 / k, true);
			ctx.fillText(count ? `${count} elements` : 'none', x, y + 18 / k);
		}

		ctx.restore();
	}

	// where the names of the areas stand, each as { key, label, x, y, box } in map units, the box around the name and the
	// count below it; every sector keeps its name, an empty one too: its place is fixed, and that an application has
	// nothing there (no scheduled tasks, no integrations) is worth knowing. The name of a narrow sector that would cover the
	// one before moves out along its ray. Kept until the zoom or the layout changes
	areaNamePlaces(k) {

		const view = this.view;
		const key  = `${k}|${view.layout.version}`;

		if (this.namePlaces?.key === key) {
			return this.namePlaces.list;
		}

		const ring = this.layoutCache().names;
		const list = [];

		this.measure.font = this.font(600, 18);

		for (const area of view.layout.areaPlaces.keys()) {

			const angle = view.layout.sectors.get(area).angle;
			const label = AppMapModel.AREAS[area].label.toUpperCase();
			const half  = (this.measure.measureText(label).width / 2 + 8) / k;
			let r       = ring;
			let box     = null;

			for (let tries = 0; tries < 12; tries++, r += 22 / k) {

				const [x, y] = AppMapLayout.point(angle, r);

				box = [x - half, y - 14 / k, x + half, y + 30 / k];

				if (!list.some(({ box: b }) => b[0] < box[2] && b[2] > box[0] && b[1] < box[3] && b[3] > box[1])) {
					break;
				}
			}

			list.push({ key: area, label, x: (box[0] + box[2]) / 2, y: box[1] + 14 / k, box });
		}

		this.namePlaces = { key, list };

		return list;
	}

	// ----- strands -----
	drawStrands(k, focus) {

		const ctx     = this.ctx;
		const view    = this.view;
		const strands = view.strands;

		// a hovered strand comes forward with a dark casing
		const lit = new Set(view.hoveredStrand ? [view.hoveredStrand] : []);

		ctx.lineCap  = 'round';
		ctx.lineJoin = 'round';

		for (const strand of strands.list) {

			const on = lit.has(strand);

			if (!on && !strands.bandVisible(strand)) {
				continue;
			}

			// a strand that mostly creates, changes or deletes takes the colour of that effect, otherwise it runs from kind to kind
			const effect = strand.kinds.find(([kind]) => AppMapRenderer.OP_COLOR[kind]);
			const solid  = effect && effect[1] >= 0.3 * strand.links.length ? this.opColor({ kind: effect[0] }) : null;

			if (on) {

				// a highlighted strand shows its real edges, each on its bundled course from element to element, with a dark
				// casing; an averaged band would end in empty space between the elements
				for (const l of strand.links) {

					const path = strands.pathOf(l).path;

					ctx.globalAlpha = 0.45;
					ctx.strokeStyle = view.look.ring;
					ctx.lineWidth   = 1.8 / k;
					ctx.stroke(path);
					ctx.globalAlpha = 0.95;
					ctx.strokeStyle = solid ?? this.opColor(l) ?? view.look.kinds[l.target.k] ?? '#888888';
					ctx.lineWidth   = 0.9 / k;
					ctx.stroke(path);
				}

				continue;
			}

			const band     = strands.bandPath(strand);
			const p0       = band[0];
			const p1       = band[band.length - 1];
			const gradient = ctx.createLinearGradient(p0[0], p0[1], p1[0], p1[1]);

			gradient.addColorStop(0, solid ?? view.look.kinds[strand.from.kind] ?? '#888888');
			gradient.addColorStop(1, solid ?? view.look.kinds[strand.to.kind] ?? '#888888');

			ctx.globalAlpha = focus ? 0.04 : (lit.size ? 0.08 : 0.28);
			ctx.strokeStyle = gradient;
			ctx.lineWidth   = (0.8 + Math.sqrt(strand.links.length) * 0.75) / Math.sqrt(k);
			ctx.stroke(AppMapStrands.smooth(new Path2D(), strand.path, p0, p1));
		}

		ctx.globalAlpha = 1;
		ctx.lineCap     = 'butt';
	}

	// ----- groupings: a crisp circle around the members of every grouping, computed once per step of the layout -----
	groupingCircles() {

		const cache = this.layoutCache();

		// the members grow and shrink with the zoom, so the outlines follow the size the elements are drawn with
		if (cache.circles?.growth !== this.view.growth) {

			cache.circles = { growth: this.view.growth, list: [] };

			for (const [key, members] of d3.group(this.view.nodes.filter(n => n.c?.[1] != null), n => n.c[1])) {

				const { x, y, r } = AppMapRenderer.circleOf(members, n => this.view.shownRadius(n));

				const anchor = this.view.byId.get(key);

				cache.circles.list.push({ key, x, y, r, box: [x - r, y - r, x + r, y + r], anchor, name: anchor?.n ?? this.view.model.sections.get(key)?.n });
			}
		}

		return cache.circles.list;
	}

	// the outline of a grouping: the smallest circle around its members at the given radius. Not around their mean, which
	// drifts toward the denser side when the members lie unevenly on a ring, and leaves the outline off centre
	static circleOf(members, radiusOf) {

		const { x, y, r } = d3.packEnclose(members.map(n => ({ x: n.x, y: n.y, r: radiusOf(n) })));

		return { x, y, r: r + 6 };
	}

	drawGroupings(k, focus, rect) {

		const ctx = this.ctx;

		ctx.save();
		ctx.lineWidth   = 0.7 / k;
		ctx.strokeStyle = this.view.look.column;
		ctx.globalAlpha = focus ? 0.05 : 0.1;
		ctx.beginPath();

		const sections = [];

		for (const c of this.groupingCircles()) {

			if (!AppMapRenderer.intersects(c.box, rect)) {
				continue;
			}

			if (this.view.model.sections.has(c.key)) {

				sections.push(c);

			} else {

				ctx.moveTo(c.x + c.r, c.y);
				ctx.arc(c.x, c.y, c.r, 0, Math.PI * 2);
			}
		}

		ctx.stroke();

		// a section of pages is an area of the user interface: tinted in the colour of the pages, as long as it lies on the
		// screen as an area; close up, where its circle grows beyond the screen, the tint would only colour the background
		// behind the page, so it fades out and the outline remains
		const screen = Math.min(this.view.width, this.view.height) / 2;

		ctx.fillStyle   = this.view.look.kinds.page;
		ctx.strokeStyle = this.view.look.kinds.page;

		for (const c of sections) {

			const tint = Math.max(0, Math.min(1, 2 - c.r * k / screen));

			ctx.beginPath();
			ctx.arc(c.x, c.y, c.r, 0, Math.PI * 2);

			if (tint > 0) {

				ctx.globalAlpha = (focus ? 0.02 : 0.06) * tint;
				ctx.fill();
			}

			ctx.globalAlpha = focus ? 0.08 : 0.25;
			ctx.stroke();
		}

		ctx.restore();
	}

	// the name of a grouping above its outline, once it is large enough on screen to carry one
	drawGroupingNames(k) {

		const ctx = this.ctx;

		ctx.save();
		ctx.textAlign    = 'center';
		ctx.textBaseline = 'bottom';
		ctx.globalAlpha  = 0.85;
		ctx.font         = this.font(500, AppMapRenderer.GROUPING_NAME_SIZE / k);
		ctx.lineWidth    = 3 / k;

		for (const { name, x, y, section } of this.groupingNamePlaces(k)) {

			// a section of pages is named in the colour of the pages, as it is the structure of that area
			ctx.font        = this.font(section ? 600 : 500, AppMapRenderer.GROUPING_NAME_SIZE / k);
			ctx.strokeStyle = this.view.look.halo;
			ctx.strokeText(name, x, y);
			ctx.fillStyle   = section ? this.view.look.kinds.page : this.view.look.labelNear;
			ctx.fillText(name, x, y);
		}

		ctx.restore();
	}

	// where the names of the groupings stand: above their outline, once it is large enough on screen to carry one, the
	// sections of pages and then the larger groupings first, none over another; with the box each takes on screen, so the
	// names of the elements give way to them and the pointer finds them
	groupingNamePlaces(k) {

		const t       = this.view.transform;
		const size    = AppMapRenderer.GROUPING_NAME_SIZE;
		const places  = [];
		const section = c => this.view.model.sections.has(c.key);
		const circles = this.groupingCircles()
			.filter(c => c.name && c.r * k >= (section(c) ? 10 : 18) && this.onScreen(c, c.r * k + 20))
			.sort((a, b) => section(b) - section(a) || b.r - a.r);

		for (const c of circles) {

			const name  = c.name.length > 32 ? c.name.slice(0, 31) + '…' : c.name;
			const width = this.textWidth(name, section(c) ? 600 : 500) * size / AppMapRenderer.LABEL_SIZE;
			const y     = c.y - c.r - 3 / k;
			const box   = [t.applyX(c.x) - width / 2, t.applyY(y) - size, t.applyX(c.x) + width / 2, t.applyY(y)];

			if (places.some(p => p.box[0] < box[2] && p.box[2] > box[0] && p.box[1] < box[3] && p.box[3] > box[1])) {
				continue;
			}

			places.push({ name, x: c.x, y, box, key: c.key, section: section(c) });
		}

		return places;
	}
}
