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
 * The placement of the application map, in two levels: first every grouping is placed as a disc in the sector of its
 * Structr area, then a force simulation places the members inside their disc.
 *
 * The map must not reshuffle when the application grows a little:
 * 1. deterministic: start positions come from a hash of the id, not from the order of the data
 * 2. remembered: once the map rests, positions are saved; when the remembered layout knows every element, nothing is
 *    simulated at all, otherwise the known elements are held at their place
 * 3. incremental: new elements start next to their neighbours, only they and their direct neighbours move
 *
 * Both simulations run in slices of a few milliseconds per animation frame (AppMapView.slice), never in one piece: a
 * fresh layout of a large application takes seconds, and the browser must stay responsive meanwhile.
 */
class AppMapLayout {

	// the forces of the layout, as a remembered layout keeps them; spread scales the distances that the discs of the groupings
	// are measured in
	static FORCE_DEFAULTS = { charge: 4, length: 5, width: 3, step: 1.6, gap: 1, cohesion: 0.3 };
	static VERSION        = 15;

	// how strongly a held element is pulled back to its remembered place while its new neighbours settle
	static HOLD = 0.35;

	// what an edge means decides how it pulls: [length, strength]; belonging is short and firm, a reference long and loose;
	// the scaffolding (embedded components, rights, helper calls) does not pull at all
	static SPRING = {
		member: [0.6, 1], contains: [0.6, 1], endpoint: [0.7, 0.9], inherits: [0.8, 0.8],
		creates: [0.8, 0.9], updates: [0.8, 0.9], deletes: [0.8, 0.9],
		relates: [1, 0.6], invokes: [1, 0.7], reads: [1.2, 0.5], calls: [1.2, 0.5],
		navigates: [2, 0.15]
	};

	// inside a grouping's disc every kind has its layer: the type in the middle, its methods around it, then actions and
	// repeaters, pages and components at the rim; the layer is a distance from the centre, not a fixed place, so the disc stays organic
	static LAYER = { type: 0, method: 0.42, api: 0.55, view: 0.68, action: 0.72, page: 0.9, component: 0.9, external: 0.95, group: 0.6 };

	// how far from the ray of its area a grouping's place and disc may lie, and an element, as a share of half the sector, so
	// the elements fill nearly all of it and keep a narrow gap at the borders
	static DISC_SPAN    = 0.91;
	static ELEMENT_SPAN = 0.95;

	// nothing comes closer to the centre than this share of the field, where the sectors are so narrow that a direction
	// would mean little; the innermost places of the groupings begin there too (see areaSlots)
	static INNER = 0.1;

	// every sector is at least this wide, in degrees, an empty one too; the rest of the circle goes to the areas after the room
	// their groupings need
	static MIN_SECTOR = 16;

	// the map is an ellipse, wider than high by this factor, as the screen is: the areas on the left and right, the pages
	// first, get the room the width of the screen offers. Angles and distances from the centre are taken on the circle the
	// ellipse is stretched from, see polar and point
	static ASPECT = 1.6;

	// the discs settle in a few hundred steps of a small simulation; the members settle until the simulation cools down
	static DISC_TICKS = 400;

	// how much room the landscape has beyond what the fullest sector needs: the groupings lie further apart, with open
	// country between them
	static STRETCH = 1.2;

	constructor(view) {

		this.view         = view;
		this.forces       = { ...AppMapLayout.FORCE_DEFAULTS };
		this.held         = new Map();
		this.groupDiscs   = new Map();
		this.areaPlaces   = new Map();
		this.sectors      = new Map();
		this.areaField    = 1;
		this.settling     = false;
		this.version      = 0;
		this.storedLayout = null;
		this.layoutSaved  = null;
		this.run          = 0;
	}

	static hash(id) {

		let h = 2166136261;

		for (const ch of String(id)) {

			h ^= ch.charCodeAt(0);
			h = Math.imul(h, 16777619);
		}

		return (h >>> 0) / 4294967296;
	}

	static normalizeAngle(a) {

		return Math.atan2(Math.sin(a), Math.cos(a));
	}

	// a place on the map as [angle, distance] on the circle the map is stretched from
	static polar(x, y) {

		return [Math.atan2(y, x / AppMapLayout.ASPECT), Math.hypot(x / AppMapLayout.ASPECT, y)];
	}

	// the place on the map at an angle and a distance on that circle
	static point(angle, distance) {

		return [Math.cos(angle) * distance * AppMapLayout.ASPECT, Math.sin(angle) * distance];
	}

	get nodes() {

		return this.view.nodes;
	}

	get links() {

		return this.view.links;
	}

	spread() {

		return Math.sqrt(this.forces.length * this.forces.width);
	}

	// ----- memory: the remembered layout -----
	// the remembered layout; the map shows both kinds of relations, the key still names them, as the layouts of earlier
	// versions were kept per kind
	key() {

		return `structrAppMapLayout_v${AppMapLayout.VERSION}_both_${location.port}`;
	}

	saveLayout() {

		const groups    = Object.fromEntries([...this.groupDiscs.values()].map(d => [d.key, [Math.round(d.x), Math.round(d.y)]]));
		const positions = Object.fromEntries(this.nodes.map(n => [n.id, [Math.round(n.x * 10) / 10, Math.round(n.y * 10) / 10]]));

		const sectors   = Object.fromEntries(this.sectors);

		this.view.storage.set(this.key(), { saved: Date.now(), forces: this.forces, groups, positions, sectors, field: this.areaField });
		this.layoutSaved = new Date();
		this.updateStatus();
	}

	updateStatus() {

		this.view.onStatus(this.settling ? 'Laying out…' : (this.layoutSaved ? `Layout saved ${this.layoutSaved.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}` : 'Layout not saved yet'));
	}

	/**
	 * Opens the map: the remembered layout where it knows the elements, a deterministic new
	 * placement where it does not. Calls back when the layout rests.
	 */
	start(onSettled) {

		this.onSettled    = onSettled;
		this.storedLayout = this.view.storage.get(this.key());
		this.held         = new Map();

		for (const key of Object.keys(this.forces)) {

			if (typeof this.storedLayout?.forces?.[key] === 'number') {
				this.forces[key] = this.storedLayout.forces[key];
			}
		}

		this.createSimulation();
		this.restore();
	}

	// the remembered layout; what it does not know is placed and settled anew
	restore() {

		const positions = this.storedLayout?.positions ?? {};

		this.held.clear();

		for (const n of this.nodes) {

			const p = positions[n.id];

			if (p) {

				n.x = p[0];
				n.y = p[1];
				this.held.set(n, p);
			}
		}

		const fresh = this.nodes.filter(n => !this.held.has(n));

		this.layoutGroups(true, () => {

			// the remembered layout knows every element: nothing moves, nothing is simulated
			if (!fresh.length) {

				this.pinAll();
				this.settled(false);

				return;
			}

			for (const n of fresh) {
				this.placeNew(n, m => this.held.has(m));
			}

			this.pinKnown(new Set(fresh));
			this.settle(this.held.size ? 0.3 : 1);
		});
	}

	// ----- first level: every grouping is a disc with a place of its own in the sector of its area -----
	layoutGroups(remembered, done) {

		const spread = this.spread();
		const areas  = AppMapModel.AREAS;
		// a disc is as large as its members need, each with its own size and some room around it: a grouping of a few large,
		// well connected elements takes as much room as one of many small ones
		const discs  = d3.groups(this.nodes, n => this.view.groupKeyOf(n))
			.map(([key, members]) => ({ key, size: members.length, area: members[0].c?.[0], r: (14 + 1.25 * Math.sqrt(d3.sum(members, m => (this.view.radiusOf(m) + 9) ** 2))) * spread }));

		// the field is as large as the fullest sector needs; a sector of angle θ and radius R has the area θR²/2
		const needed = d3.rollup(discs.filter(d => areas[d.area]), ds => d3.sum(ds, x => Math.PI * (x.r + 18 * spread) ** 2), d => d.area);

		// a remembered layout keeps its sectors and field: the room the groupings need changes with the sizes of the elements,
		// and borders taken from it anew would no longer match the remembered places
		const kept    = remembered && this.storedLayout?.sectors && Object.keys(AppMapModel.AREAS).every(key => this.storedLayout.sectors[key]) ? this.storedLayout : null;

		this.sectors = kept ? new Map(Object.entries(kept.sectors)) : AppMapLayout.sectorsFor(needed);

		const fullest = d3.max([...this.sectors], ([key, s]) => Math.sqrt(2 * (needed.get(key) ?? 0) / (2 * s.half * AppMapLayout.DISC_SPAN))) ?? 0;
		const field   = kept?.field ?? Math.max(Math.sqrt(d3.sum(discs, x => (x.r + 30 * spread) ** 2)) * 0.9, fullest * 1.05) * AppMapLayout.STRETCH;
		const slotOf  = this.slots(discs, field);
		const stored  = remembered ? (this.storedLayout?.groups ?? {}) : {};

		this.areaField = field;

		// remembered discs stay where they were; new ones start at their place, a little off by their name
		for (const d of discs) {

			const p = stored[d.key];

			if (p) {

				d.x = d.fx = p[0];
				d.y = d.fy = p[1];

			} else {

				const centre = slotOf.get(d) ?? { x: 0, y: 0 };
				const a      = AppMapLayout.hash(d.key) * Math.PI * 2;
				const r      = Math.sqrt(AppMapLayout.hash(d.key + '/r')) * field * 0.25;

				d.x = centre.x + Math.cos(a) * r;
				d.y = centre.y + Math.sin(a) * r;
			}
		}

		this.groupDiscs = new Map(discs.map(d => [d.key, d]));

		// while the discs settle, the elements without a place ride along with their disc, so the map can be shown from the
		// first frame; they get their real place when the discs rest
		const followers = this.nodes.filter(n => !this.held.has(n)).map(n => {

			const disc = this.groupDiscs.get(this.view.groupKeyOf(n));

			this.placeNew(n, m => this.held.has(m));

			return [n, disc, n.x - disc.x, n.y - disc.y];
		});

		const follow = () => {

			for (const [n, disc, dx, dy] of followers) {

				n.x = disc.x + dx;
				n.y = disc.y + dy;
			}

			this.version++;
			this.view.requestDraw();
		};

		const finish = () => {

			discs.forEach(d => {

				d.fx = null;
				d.fy = null;
			});

			done();
		};

		// when every disc is remembered there is nothing to settle
		if (discs.every(d => stored[d.key])) {

			finish();

			return;
		}

		const meta  = this.discSimulation(discs, slotOf, field, spread);
		const run   = ++this.run;
		let ticks   = 0;

		this.settling = true;
		this.updateStatus();

		this.view.slice(() => {

			meta.tick();

			return run === this.run && ++ticks < AppMapLayout.DISC_TICKS;

		}, () => {

			if (run === this.run) {
				finish();
			}

		}, follow);
	}

	// the sectors of the areas in their fixed order around the circle, each as wide as the room its groupings need, so every
	// sector is filled alike and the map is no larger than it must be; Pages keeps its place on the left. Kept per area as
	// { angle, half }: the ray in the middle of the sector and half its width, in radians
	static sectorsFor(needed) {

		const keys  = Object.keys(AppMapModel.AREAS);
		const total = d3.sum(needed.values());
		const min   = AppMapLayout.MIN_SECTOR * Math.PI / 180;
		const free  = 2 * Math.PI - keys.length * min;
		const width = key => min + free * (total ? (needed.get(key) ?? 0) / total : 1 / keys.length);
		const list  = new Map();
		let start   = Math.PI - width(keys[0]) / 2;

		for (const key of keys) {

			list.set(key, { angle: start + width(key) / 2, half: width(key) / 2 });
			start += width(key);
		}

		return list;
	}

	// where the areas stand, and the place of every grouping in the sector of its area
	slots(discs, field) {

		const areas    = AppMapModel.AREAS;
		const incoming = new Map();
		const outgoing = new Map();
		const groupIn  = new Map();
		const groupOut = new Map();

		for (const l of this.links) {

			const [from, to] = AppMapModel.dependency(l);
			const a          = from?.c?.[0];
			const b          = to?.c?.[0];

			if (a && b && a !== b) {

				outgoing.set(a, (outgoing.get(a) ?? 0) + 1);
				incoming.set(b, (incoming.get(b) ?? 0) + 1);
			}

			if (from && to && this.view.groupKeyOf(from) !== this.view.groupKeyOf(to)) {

				groupOut.set(this.view.groupKeyOf(from), (groupOut.get(this.view.groupKeyOf(from)) ?? 0) + 1);
				groupIn.set(this.view.groupKeyOf(to), (groupIn.get(this.view.groupKeyOf(to)) ?? 0) + 1);
			}
		}

		// how central each area is, after Robert Martin's instability I = Ce / (Ca + Ce): what the other areas depend on
		// (incoming, Ca) and what it depends on itself (outgoing, Ce); I = 0 is the stable core everything relies on, I = 1 the
		// rim that only uses others. Only the ratio counts, not the number of relations; an area without relations stands at
		// the rim. Kept as the point [x, y].
		const instability = (into, out) => into + out ? out / (into + out) : 1;

		this.areaPlaces = new Map(Object.keys(areas).map(key => {

			const ca       = incoming.get(key) ?? 0;
			const ce       = outgoing.get(key) ?? 0;
			const distance = (0.12 + 0.88 * instability(ca, ce)) * field;
			const rad      = this.sectors.get(key).angle;

			return [key, AppMapLayout.point(rad, distance)];
		}));

		// the places spread evenly over the whole sector, each grouping gets a share of the sector's area after the area of
		// its disc, so a page with many actions does not crowd its neighbours; inward lies what others depend on, outward what
		// only uses others (the grouping's own instability), the angle scatters by the golden ratio
		const slotOf = new Map();
		const inner  = AppMapLayout.INNER * field;
		const outer  = field;
		const own    = d => instability(groupIn.get(d.key) ?? 0, groupOut.get(d.key) ?? 0);

		for (const [area, ds] of d3.group(discs.filter(d => areas[d.area]), d => d.area)) {

			const centre  = this.sectors.get(area).angle;
			const half    = this.sectors.get(area).half * AppMapLayout.DISC_SPAN;
			const ordered = ds.slice().sort((a, b) => own(a) - own(b) || AppMapLayout.hash(a.key) - AppMapLayout.hash(b.key));
			const total   = d3.sum(ordered, d => d.r * d.r);
			let before    = 0;

			ordered.forEach((d, i) => {

				const u = (before + d.r * d.r / 2) / total;

				before += d.r * d.r;

				const radius = Math.sqrt(inner * inner + u * (outer * outer - inner * inner));
				const angle  = centre + (((i * 0.6180339887) % 1) * 2 - 1) * half;

				const [x, y] = AppMapLayout.point(angle, radius);

				slotOf.set(d, { x, y });
			});
		}

		return slotOf;
	}

	// no relation pulls groupings together: each holds its place in its sector, and all push each other apart
	discSimulation(discs, slotOf, field, spread) {

		// radial consistency: a disc stays within DISC_SPAN of the ray of its area, so its direction from the centre is the same
		// in every layout
		const keepInSector = alpha => {

			for (const d of discs) {

				const target = slotOf.has(d) && this.areaPlaces.get(d.area) ? this.inSector(d.x, d.y, this.sectors.get(d.area), AppMapLayout.DISC_SPAN) : null;

				if (target) {

					// turn the disc back toward the edge of its sector, along the ellipse it is on
					const pull = Math.min(1, alpha * 3);

					d.vx += (target[0] - d.x) * pull;
					d.vy += (target[1] - d.y) * pull;
				}
			}
		};

		return d3.forceSimulation(discs)
			.force('sector', keepInSector)
			.force('areaX', d3.forceX(d => slotOf.get(d)?.x ?? 0).strength(d => slotOf.has(d) ? 0.3 : 0))
			.force('areaY', d3.forceY(d => slotOf.get(d)?.y ?? 0).strength(d => slotOf.has(d) ? 0.3 : 0))
			.force('charge', d3.forceManyBody().strength(d => -2 * d.r).distanceMax(8 * d3.max(discs, d => d.r)))
			.force('collide', d3.forceCollide(d => d.r + 30 * spread * this.forces.gap).iterations(4))
			.stop();
	}

	// a deterministic place for an element: inside its grouping's disc, next to the placed neighbours of the same grouping
	placeNew(n, placed) {

		const key   = this.view.groupKeyOf(n);
		const disc  = this.groupDiscs.get(key);
		const known = this.view.neighbours.get(n).filter(m => placed(m) && this.view.groupKeyOf(m) === key);
		const a     = AppMapLayout.hash(n.id) * Math.PI * 2;
		const h     = Math.sqrt(AppMapLayout.hash(n.id + '/r'));
		const r     = known.length ? 18 + 20 * h * h : (disc ? disc.r * 0.8 * h : 40 + 260 * h);
		const cx    = known.length ? d3.mean(known, m => m.x) : (disc?.x ?? 0);
		const cy    = known.length ? d3.mean(known, m => m.y) : (disc?.y ?? 0);

		n.x = cx + Math.cos(a) * r;
		n.y = cy + Math.sin(a) * r;
	}

	// at rest every element is anchored
	pinAll() {

		for (const n of this.nodes) {

			n.fx = n.x;
			n.fy = n.y;
		}
	}

	unpinAll() {

		for (const n of this.nodes) {

			n.fx = null;
			n.fy = null;
		}
	}

	// only new elements and their direct neighbours may move, every other known element stays exactly where it was
	pinKnown(fresh) {

		const near = new Set([...fresh].flatMap(n => this.view.neighbours.get(n)));

		for (const n of this.nodes) {

			if (fresh.has(n) || near.has(n) || !this.held.has(n)) {

				n.fx = null;
				n.fy = null;

			} else {

				n.fx = this.held.get(n)[0];
				n.fy = this.held.get(n)[1];
			}
		}
	}

	// ----- second level: the members inside their discs -----
	// the type a method belongs to: by a member edge, by its name, or else the type it works with most
	computeOwners() {

		const usage = new Map();

		this.ownerOf = new Map();

		for (const l of this.links) {

			for (const [m, t] of [[l.source, l.target], [l.target, l.source]]) {

				if (l.kind === 'contains' && m === l.target) {
					this.ownerOf.set(m, t);
				}

				if (m.k === 'method' && t.k === 'type') {

					const counts = usage.get(m) ?? new Map();

					if (l.kind === 'member') {
						this.ownerOf.set(m, t);
					}

					counts.set(t, (counts.get(t) ?? 0) + l.w);
					usage.set(m, counts);
				}
			}
		}

		for (const n of this.nodes) {

			if (n.k === 'type') {

				this.ownerOf.set(n, n);

			} else if (n.k === 'method' && !this.ownerOf.has(n)) {

				const named = n.n.includes('.') ? this.view.model.typeByName.get(n.n.slice(0, n.n.lastIndexOf('.'))) : null;
				const used  = usage.has(n) ? d3.greatest(usage.get(n), ([, w]) => w)[0] : null;

				if (named ?? used) {
					this.ownerOf.set(n, named ?? used);
				}
			}
		}
	}

	// every method is held near its type, so the types become cores with their methods around them
	ownerLinks() {

		return [...this.ownerOf].filter(([m, t]) => m !== t).map(([m, t]) => ({ source: m, target: t }));
	}

	keepInDiscs(alpha) {

		const cohesion = alpha * this.forces.cohesion * 0.6;
		const pull     = Math.min(0.5, alpha * 3);

		for (const n of this.nodes) {

			const disc = n.fx == null ? this.groupDiscs.get(this.view.groupKeyOf(n)) : null;

			if (!disc) {
				continue;
			}

			const dx       = disc.x - n.x;
			const dy       = disc.y - n.y;
			const distance = Math.hypot(dx, dy) || 1;
			const layer    = (AppMapLayout.LAYER[n.k] ?? 0.6) * disc.r * 0.85;
			const toward   = (distance - layer) / distance;

			n.vx += dx * toward * cohesion;
			n.vy += dy * toward * cohesion;

			if (distance > disc.r) {

				const push = (distance - disc.r) / distance * 0.5;

				n.vx += dx * push;
				n.vy += dy * push;
			}

			// and no element leaves the sector of its area: past the border it is turned back along its circle, more gently as
			// the layout cools down, so the map comes to rest; when it rests, confine puts it inside for good
			const target = this.inSector(n.x, n.y, this.sectors.get(n.c?.[0]), AppMapLayout.ELEMENT_SPAN);

			if (target) {

				n.vx += (target[0] - n.x) * pull;
				n.vy += (target[1] - n.y) * pull;
			}
		}
	}

	// the nearest place to a point inside a sector, within the given share of its half width and no closer to the centre
	// than INNER; null when the point lies there already or there is no sector
	inSector(x, y, sector, span) {

		if (!sector) {
			return null;
		}

		const [angle, distance] = AppMapLayout.polar(x, y);
		const limit             = sector.half * span;
		const off               = AppMapLayout.normalizeAngle(angle - sector.angle);
		const inner             = AppMapLayout.INNER * this.areaField;

		if (Math.abs(off) <= limit && distance >= inner) {
			return null;
		}

		return AppMapLayout.point(sector.angle + Math.max(-limit, Math.min(limit, off)), Math.max(inner, distance));
	}

	// every element inside the sector of its area, whatever the forces left it at or a remembered layout says; true when
	// one had to move
	confine() {

		let moved = false;

		for (const n of this.nodes) {

			const target = this.inSector(n.x, n.y, this.sectors.get(n.c?.[0]), AppMapLayout.ELEMENT_SPAN);

			if (target) {

				[n.x, n.y] = target;
				moved      = true;
			}
		}

		return moved;
	}

	// the own type is the innermost level, then the grouping and the area; the levels are 0, 1, 3 and 4, as a level between
	// grouping and area is gone and its step stays: the tuned forces and the lengths of the layout rest on these numbers
	hierarchyDistance(l) {

		const a     = l.source.c ?? [];
		const b     = l.target.c ?? [];
		const owner = this.ownerOf.get(l.source);

		if (owner && owner === this.ownerOf.get(l.target)) {
			return 0;
		}

		if (a[1] != null && a[1] === b[1]) {
			return 1;
		}

		if (a[0] != null && a[0] === b[0]) {
			return 3;
		}

		return 4;
	}

	springOf(l) {

		return AppMapModel.STRUCTURE.has(l.kind) ? [1, 0] : (AppMapLayout.SPRING[l.kind] ?? [1, 0.3]);
	}

	// the degree a spring is divided by counts only the edges that pull
	countSprings() {

		this.springDegree = new Map();

		for (const l of this.links) {

			if (!AppMapModel.STRUCTURE.has(l.kind)) {

				this.springDegree.set(l.source, (this.springDegree.get(l.source) ?? 0) + 1);
				this.springDegree.set(l.target, (this.springDegree.get(l.target) ?? 0) + 1);
			}
		}
	}

	// an edge is as long as the hierarchy between its ends demands
	linkDistance(l) {

		return this.springOf(l)[0] * 22 * this.forces.length * Math.pow(this.forces.step, this.hierarchyDistance(l));
	}

	// a spring is divided by the degree of its smaller end, as d3 does, so hubs with a hundred edges do not overshoot;
	// between groupings an edge does not pull at all: the groupings have their places, inside a grouping it holds it together
	linkStrength(l) {

		if (this.view.groupKeyOf(l.source) !== this.view.groupKeyOf(l.target)) {
			return 0;
		}

		return this.springOf(l)[1] * 0.9 / Math.min(this.springDegree.get(l.source) ?? 1, this.springDegree.get(l.target) ?? 1);
	}

	// the simulation never runs on d3's own timer: settle() advances it in slices
	createSimulation() {

		this.computeOwners();
		this.countSprings();

		this.sim = d3.forceSimulation(this.nodes)
			.force('link', d3.forceLink(this.links))
			.force('charge', d3.forceManyBody().theta(0.9).distanceMax(900))
			.force('owner', d3.forceLink(this.ownerLinks()).strength(0.25))
			.force('groups', alpha => this.keepInDiscs(alpha))
			// from afar the elements are drawn larger than their size (AppMapView.SIZE_ZOOM), and even then they must not touch
			.force('collide', d3.forceCollide(n => this.view.radiusOf(n) * AppMapView.MAX_GROWTH + 4))
			.force('gx', d3.forceX())
			.force('gy', d3.forceY())
			.alphaDecay(0.02)
			.velocityDecay(0.42)
			.stop();

		this.restartForces();
	}

	// the forces read the current settings, the held elements and the relations again
	restartForces() {

		this.sim.force('link').distance(l => this.linkDistance(l)).strength(l => this.linkStrength(l));
		this.sim.force('charge').strength(-14 * this.forces.charge);
		this.sim.force('owner').distance(22 * this.forces.length);
		this.sim.force('gx').x(n => this.held.get(n)?.[0] ?? 0).strength(n => this.held.has(n) ? AppMapLayout.HOLD : 0);
		this.sim.force('gy').y(n => this.held.get(n)?.[1] ?? 0).strength(n => this.held.has(n) ? AppMapLayout.HOLD : 0);
	}

	// lets the members settle from the given heat, a few steps per frame, and draws every frame
	settle(alpha) {

		const run = ++this.run;

		this.settling = true;
		this.view.onLayoutChanged();
		this.updateStatus();
		this.sim.alpha(alpha);

		this.view.slice(() => {

			if (run !== this.run) {
				return false;
			}

			this.sim.tick();

			return this.sim.alpha() >= this.sim.alphaMin();

		}, () => {

			if (run === this.run) {
				this.settled(true);
			}

		}, () => {

			this.version++;
			this.view.requestDraw();
		});
	}

	// the layout rests: every element is anchored at its place, the place is remembered
	settled(moved) {

		moved = this.confine() || moved;

		for (const n of this.nodes) {
			this.held.set(n, [n.x, n.y]);
		}

		this.pinAll();
		this.settling = false;
		this.version++;

		if (moved || !this.layoutSaved) {
			this.saveLayout();
		}

		this.updateStatus();
		this.onSettled?.(moved);
	}

	// whatever runs in slices stops at its next slice
	stop() {

		this.run++;
	}

	// ----- for the console -----
	// forgets the remembered layout and lays everything out anew
	relayout() {

		this.view.storage.remove(this.key());
		this.held.clear();
		this.unpinAll();
		this.layoutGroups(false, () => {

			for (const n of this.nodes) {
				this.placeNew(n, () => false);
			}

			this.restartForces();
			this.settle(1);
		});
	}
}
