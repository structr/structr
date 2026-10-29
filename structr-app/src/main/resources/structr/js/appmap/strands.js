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
 * The relations of the application map as lines: one line per pair of elements, the functional ones bundled along a
 * routing tree over the places of the groupings (hierarchical edge bundling, Holten 2006), so they branch like trees;
 * bundled edges between the same two groupings form a named strand.
 *
 * The courses are computed once the layout rests and kept as Path2D objects in map coordinates, so a frame only strokes
 * them; while the layout moves, every relation is a straight line between its ends.
 */
class AppMapStrands {

	// the strongest effect of a pair leads and gives the line its colour: creating before deleting before changing before reading
	static EFFECT_RANK = ['creates', 'deletes', 'updates', 'reads', 'invokes', 'calls', 'requests', 'mails', 'endpoint', 'member', 'contains', 'inherits', 'relates', 'navigates'];

	// the relations that change data
	static WRITES = new Set(['creates', 'updates', 'deletes']);

	// how closely a course follows the hubs of the routing tree; 0 would draw it straight
	static BUNDLE_BETA = 0.92;

	// the number of points of a bundled course; the line is drawn through them with quadratic curves
	static SAMPLES = 24;

	// a hub of the routing tree has at most this many children; the hubs between the groupings and the root of their area
	// lean toward the root by this share, so the branches run toward the trunk and no bundle turns back on its way
	static BRANCHING = 3;
	static ROOT_PULL = 0.4;

	// a course bends at a hub by at most this angle, in degrees; a sharper turn would draw a hook
	static MAX_TURN = 100;

	constructor(view) {

		this.view           = view;
		this.list           = [];
		this.bundles        = null;
		this.trunkOf        = new Map();
		this.paths          = new Map();
		this.bandCache      = new Map();
		this.groupingsDirty = true;
		this.groupings      = null;
		this.pairCache      = null;
	}

	// ----- geometry -----
	// spreads a polyline evenly over a given number of points, keeping its ends
	static resample(points, count) {

		const lengths = [0];
		const result  = [];
		let j         = 1;

		for (let i = 1; i < points.length; i++) {
			lengths.push(lengths[i - 1] + Math.hypot(points[i][0] - points[i - 1][0], points[i][1] - points[i - 1][1]));
		}

		const total = lengths[lengths.length - 1] || 1;

		for (let i = 0; i < count; i++) {

			const target = total * i / (count - 1);

			while (j < points.length - 1 && lengths[j] < target) {
				j++;
			}

			const t = (target - lengths[j - 1]) / ((lengths[j] - lengths[j - 1]) || 1);

			result.push([points[j - 1][0] + (points[j][0] - points[j - 1][0]) * t, points[j - 1][1] + (points[j][1] - points[j - 1][1]) * t]);
		}

		return result;
	}

	// a uniform cubic B-spline, clamped to its first and last control point
	static bspline(control) {

		const p      = [control[0], control[0], ...control, control[control.length - 1], control[control.length - 1]];
		const points = [];

		for (let j = 0; j + 3 < p.length; j++) {

			for (let s = 0; s < 8; s++) {

				const t  = s / 8;
				const b0 = (1 - t) ** 3 / 6;
				const b1 = (3 * t ** 3 - 6 * t ** 2 + 4) / 6;
				const b2 = (-3 * t ** 3 + 3 * t ** 2 + 3 * t + 1) / 6;
				const b3 = t ** 3 / 6;

				points.push([b0 * p[j][0] + b1 * p[j + 1][0] + b2 * p[j + 2][0] + b3 * p[j + 3][0], b0 * p[j][1] + b1 * p[j + 1][1] + b2 * p[j + 2][1] + b3 * p[j + 3][1]]);
			}
		}

		points.push(control[control.length - 1]);

		return points;
	}

	static distanceToPath(x, y, path) {

		let best = Infinity;

		for (let i = 1; i < path.length; i++) {

			const [ax, ay] = path[i - 1];
			const [bx, by] = path[i];
			const dx       = bx - ax;
			const dy       = by - ay;
			const t        = Math.max(0, Math.min(1, ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy || 1)));

			best = Math.min(best, Math.hypot(x - ax - t * dx, y - ay - t * dy));
		}

		return best;
	}

	// a smooth line through the points, from start to end: quadratic curves through the midpoints, as d3's curveBasis does
	static smooth(path, points, start, end) {

		path.moveTo(start[0], start[1]);

		for (let i = 1; i < points.length - 2; i++) {
			path.quadraticCurveTo(points[i][0], points[i][1], (points[i][0] + points[i + 1][0]) / 2, (points[i][1] + points[i + 1][1]) / 2);
		}

		const last = points[points.length - 2];

		path.quadraticCurveTo(last[0], last[1], end[0], end[1]);

		return path;
	}

	// ----- one line per pair: the relations between two elements, summed -----
	static rankOf(kind) {

		return AppMapModel.STRUCTURE.has(kind) ? 99 : (AppMapStrands.EFFECT_RANK.indexOf(kind) + 1 || 50);
	}

	static pairsOf(list) {

		const byKey = new Map();

		for (const l of list) {

			const key = l.source.id < l.target.id ? `${l.source.id}|${l.target.id}` : `${l.target.id}|${l.source.id}`;
			const p   = byKey.get(key) ?? { links: [], lead: l, w: 0 };

			p.links.push(l);
			p.w += l.w;

			if (AppMapStrands.rankOf(l.kind) < AppMapStrands.rankOf(p.lead.kind)) {
				p.lead = l;
			}

			byKey.set(key, p);
		}

		return [...byKey.values()];
	}

	// a pair with several relations is drawn thicker, growing slowly with their number
	static pairWidth(p) {

		return 1 + 0.6 * Math.log2(p.links.length);
	}

	linkPairs() {

		const view = this.view;
		const key  = view.generation;

		if (this.pairCache?.key !== key) {
			this.pairCache = { key, pairs: AppMapStrands.pairsOf(view.links.filter(l => !AppMapModel.STRUCTURE.has(l.kind))) };
		}

		return this.pairCache.pairs;
	}

	// only the functional relations are bundled: they are what the trees show; the others stay straight and quiet
	drawnLinks() {

		return this.view.links.filter(l => AppMapModel.FUNCTIONAL.has(l.kind));
	}

	// ----- the groupings, each with its members and the kind most of them have -----
	ensureGroupings() {

		if (!this.groupingsDirty) {
			return;
		}

		this.groupings = { list: [], groupingOf: new Map() };

		for (const [key, members] of d3.group(this.view.nodes, n => n.c?.[1] ?? '-')) {

			const lead   = d3.rollups(members, v => v.length, m => m.k).sort((x, y) => y[1] - x[1])[0][0];
			const grouping = { anchor: this.view.byId.get(key) ?? null, members, lead, name: this.view.model.sections.get(key)?.n ?? key };

			this.groupings.list.push(grouping);
			members.forEach(m => this.groupings.groupingOf.set(m, grouping));
		}

		this.groupingsDirty = false;
	}

	// a grouping is named after its anchor, a section of the user interface by its own name
	static groupingLabel(grouping) {

		return grouping.anchor ? `${AppMapModel.KIND_LABEL[grouping.anchor.k]} ${grouping.anchor.n}` : grouping.name;
	}

	// ----- bundling along a routing tree, so the relations branch like a tree or a bundle of fibres -----
	// the groupings of every area are split again and again in two along the direction in which they spread most; each part
	// is a hub at the centre of its members, the whole area the root. A relation climbs from the grouping of its element
	// through the hubs to the hub both ends share, or to the root of its area and over to the root of the other area, and
	// down again: relations whose ends lie near each other share their way, and the closer to the root, the more of them
	routingTree() {

		this.ensureGroupings();

		const hubOf = new Map();

		for (const inArea of d3.group(this.groupings.list, grouping => grouping.members[0].c?.[0] ?? '-').values()) {

			const leaves = inArea.map(grouping => ({ point: [d3.mean(grouping.members, m => m.x), d3.mean(grouping.members, m => m.y)], weight: grouping.members.length, parent: null }));

			const inner = [];

			inArea.forEach((grouping, i) => hubOf.set(grouping, leaves[i]));

			const root = AppMapStrands.branch(leaves, inner);

			for (const h of inner) {

				if (h !== root) {
					h.point = [h.point[0] + (root.point[0] - h.point[0]) * AppMapStrands.ROOT_PULL, h.point[1] + (root.point[1] - h.point[1]) * AppMapStrands.ROOT_PULL];
				}
			}
		}

		return hubOf;
	}

	// the hub above the given ones, at their weighted centre; more than BRANCHING are first split in two at the weighted
	// median along their principal axis. Every hub it creates is added to the given list
	static branch(hubs, created) {

		const weight = d3.sum(hubs, h => h.weight);
		const x      = d3.sum(hubs, h => h.point[0] * h.weight) / weight;
		const y      = d3.sum(hubs, h => h.point[1] * h.weight) / weight;
		const node   = { point: [x, y], weight, parent: null };

		created.push(node);

		if (hubs.length <= AppMapStrands.BRANCHING) {

			hubs.forEach(h => h.parent = node);

			return node;
		}

		const xx     = d3.sum(hubs, h => (h.point[0] - x) ** 2 * h.weight);
		const yy     = d3.sum(hubs, h => (h.point[1] - y) ** 2 * h.weight);
		const xy     = d3.sum(hubs, h => (h.point[0] - x) * (h.point[1] - y) * h.weight);
		const axis   = 0.5 * Math.atan2(2 * xy, xx - yy);
		const sorted = hubs.slice().sort((a, b) => (a.point[0] * Math.cos(axis) + a.point[1] * Math.sin(axis)) - (b.point[0] * Math.cos(axis) + b.point[1] * Math.sin(axis)));
		let half     = 0;
		let split    = 1;

		for (; split < sorted.length - 1; split++) {

			half += sorted[split - 1].weight;

			if (half >= weight / 2) {
				break;
			}
		}

		for (const part of [sorted.slice(0, split), sorted.slice(split)]) {

			const child = part.length === 1 ? part[0] : AppMapStrands.branch(part, created);

			child.parent = node;
		}

		return node;
	}

	// removes the hubs where a course would turn back sharper than MAX_TURN, the sharpest first, until none is left: a hook,
	// mostly where a relation would run on to the root of the other area and come back to an end that lies before it; the
	// gentle bends at the hubs, where the bundles run together, stay
	static removeHooks(control) {

		const limit = AppMapStrands.MAX_TURN * Math.PI / 180;
		const turn  = i => {

			const [ax, ay] = control[i - 1];
			const [bx, by] = control[i];
			const [cx, cy] = control[i + 1];

			return Math.abs(AppMapLayout.normalizeAngle(Math.atan2(cy - by, cx - bx) - Math.atan2(by - ay, bx - ax)));
		};

		while (control.length > 2) {

			const sharpest = d3.maxIndex(d3.range(1, control.length - 1), i => turn(i)) + 1;

			if (turn(sharpest) <= limit) {
				break;
			}

			control.splice(sharpest, 1);
		}
	}

	bundleEdges(list) {

		const result = new Map();

		this.hubOf   = this.routingTree();
		this.trunkOf = new Map();

		for (const l of list) {

			// a bundled edge belongs to the strand between the groupings of its two ends
			this.trunkOf.set(l, [this.groupings.groupingOf.get(l.source), this.groupings.groupingOf.get(l.target)]);

			result.set(l, this.course(l.source, l.target));
		}

		return result;
	}

	// the bundled course from one element to another through the hubs of the routing tree, as resampled points
	course(source, target) {

		const hubOf  = this.hubOf;
		const beta   = AppMapStrands.BUNDLE_BETA;
		const chain  = n => {

			const hubs = [];

			for (let h = hubOf.get(this.groupings.groupingOf.get(n)); h; h = h.parent) {
				hubs.push(h);
			}

			return hubs;
		};

		const up      = chain(source);
		const down    = chain(target);
		const shared  = up.findIndex(h => down.includes(h));
		const hubs    = shared < 0 ? [...up, ...down.reverse()] : [...up.slice(0, shared + 1), ...down.slice(0, down.indexOf(up[shared])).reverse()];
		const control = [[source.x, source.y]];
		const dx      = target.x - source.x;
		const dy      = target.y - source.y;
		const length  = Math.hypot(dx, dy) || 1;
		let progress  = 0;

		// a hub counts only when it lies ahead on the way, so no bundle turns back in a loop; a hub right on top of the
		// element or the hub before it adds nothing but a kink
		for (const h of hubs) {

			const last = control[control.length - 1];
			const t    = ((h.point[0] - source.x) * dx + (h.point[1] - source.y) * dy) / (length * length);

			if (t > progress - 0.1 && t < 1.1 && Math.hypot(h.point[0] - last[0], h.point[1] - last[1]) > 4) {

				control.push(h.point);
				progress = Math.max(progress, t);
			}
		}

		control.push([target.x, target.y]);
		AppMapStrands.removeHooks(control);

		// beta pulls the course toward the straight line, as in d3.curveBundle
		const [x0, y0]     = control[0];
		const [x1, y1]     = control[control.length - 1];
		const last         = control.length - 1;
		const straightened = control.map(([x, y], i) => [beta * x + (1 - beta) * (x0 + (x1 - x0) * i / last), beta * y + (1 - beta) * (y0 + (y1 - y0) * i / last)]);

		return AppMapStrands.resample(AppMapStrands.bspline(straightened), AppMapStrands.SAMPLES);
	}

	// recomputes the courses and strands soon, once: several changes in a row cost one computation
	schedule() {

		clearTimeout(this.timer);

		this.timer = setTimeout(() => {

			if (!this.view.destroyed) {
				this.rebuild();
			}

		}, 60);
	}

	cancel() {

		clearTimeout(this.timer);
	}

	rebuild() {

		this.groupingsDirty = true;
		this.bundles        = this.bundleEdges(this.drawnLinks());
		this.paths          = new Map();
		this.bandCache      = new Map();

		this.computeStrands();
		this.view.requestDraw();
	}

	reset() {

		this.bundles        = null;
		this.list           = [];
		this.paths          = new Map();
		this.bandCache      = new Map();
		this.groupingsDirty = true;
		this.pairCache      = null;
	}

	bundled() {

		return this.bundles && !this.view.layout.settling;
	}

	/**
	 * The course of a relation as a Path2D in map coordinates, with its bounding box for culling. A bundled course is built
	 * once per bundling, a straight line once per step of the layout.
	 */
	pathOf(l) {

		const bundled = this.bundled();
		const version = bundled ? -1 : this.view.layout.version;
		const cached  = this.paths.get(l);

		if (cached && cached.version === version) {
			return cached;
		}

		const points = bundled ? this.bundles.get(l) : null;
		const path   = new Path2D();
		const box    = [Math.min(l.source.x, l.target.x), Math.min(l.source.y, l.target.y), Math.max(l.source.x, l.target.x), Math.max(l.source.y, l.target.y)];

		if (points) {

			AppMapStrands.smooth(path, points, [l.source.x, l.source.y], [l.target.x, l.target.y]);

			for (const [x, y] of points) {

				box[0] = Math.min(box[0], x);
				box[1] = Math.min(box[1], y);
				box[2] = Math.max(box[2], x);
				box[3] = Math.max(box[3], y);
			}

		} else {

			path.moveTo(l.source.x, l.source.y);
			path.lineTo(l.target.x, l.target.y);
		}

		const entry = { path, box, version };

		this.paths.set(l, entry);

		return entry;
	}

	// ----- strands: bundled edges between the same two groupings become one named strand -----
	computeStrands() {

		this.list = [];

		if (!this.bundles) {
			return;
		}

		this.ensureGroupings();

		const ids     = new Map(this.groupings.list.map((grouping, i) => [grouping, `r${i}`]));
		const side    = grouping => ({ kind: grouping.lead, name: AppMapStrands.groupingLabel(grouping) });
		const trunked = [...this.bundles.keys()].filter(l => this.trunkOf.get(l)[0] !== this.trunkOf.get(l)[1]);
		const groups  = d3.group(trunked, l => this.trunkOf.get(l).map(grouping => ids.get(grouping)).sort().join('>'));

		for (const [key, list] of groups) {

			if (list.length < 3) {
				continue;
			}

			// every member is read from side a to side b, then the strand runs along their mean
			const [a, b]    = key.split('>');
			const forward   = l => ids.get(this.trunkOf.get(l)[0]) === a;
			const lines     = list.map(l => forward(l) ? this.bundles.get(l) : this.bundles.get(l).slice().reverse());
			const path      = d3.range(lines[0].length).map(i => [d3.mean(lines, p => p[i][0]), d3.mean(lines, p => p[i][1])]);
			const fromNodes = list.map(l => forward(l) ? l.source : l.target);
			const toNodes   = list.map(l => forward(l) ? l.target : l.source);
			const ends      = forward(list[0]) ? this.trunkOf.get(list[0]) : this.trunkOf.get(list[0]).slice().reverse();

			this.list.push({
				links:     list,
				path,
				from:      side(ends[0]),
				to:        side(ends[1]),
				ends:      new Set([...fromNodes, ...toNodes]),
				fromNodes: [...new Set(fromNodes)],
				toNodes:   [...new Set(toNodes)],
				kinds:     d3.rollups(list, v => v.length, l => l.kind).sort((x, y) => y[1] - x[1])
			});
		}

		this.list.sort((x, y) => y.links.length - x.links.length);
	}

	static strandLabel(strand) {

		return `${strand.from.name} ↔ ${strand.to.name}`;
	}

	// only strong strands between different groupings are drawn as bands, and only until the single elements are visible:
	// close up a band would end between the elements it summarises; the others appear on hover
	bandVisible(strand) {

		return strand.links.length >= 6 && this.view.zoomTier() !== 2;
	}

	// the band runs between the centres of the elements on its two sides, through the inner waypoints; the layout rests while
	// strands exist, so the band is computed once
	bandPath(strand) {

		let band = this.bandCache.get(strand);

		if (!band) {

			const p0 = [d3.mean(strand.fromNodes, n => n.x), d3.mean(strand.fromNodes, n => n.y)];
			const p1 = [d3.mean(strand.toNodes, n => n.x), d3.mean(strand.toNodes, n => n.y)];

			band = [p0, ...strand.path.slice(1, -1), p1];

			this.bandCache.set(strand, band);
		}

		return band;
	}

	// a strand answers only where its band is drawn
	findStrand(x, y) {

		let best         = null;
		let bestDistance = 7 / this.view.transform.k;

		for (const strand of this.list) {

			if (!this.bandVisible(strand)) {
				continue;
			}

			const d = AppMapStrands.distanceToPath(x, y, this.bandPath(strand));

			if (d < bestDistance) {

				best         = strand;
				bestDistance = d;
			}
		}

		return best;
	}
}
