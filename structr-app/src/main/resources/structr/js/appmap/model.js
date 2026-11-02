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
 * The graph of the application map, built from what the _appmap endpoint delivers:
 *
 *     { result: { nodes: [...], edges: [...], groups: [...], sections: [...], sites: [...] } }
 *
 * - node:  { id, k, n, p, g, area, d?, crud?, svc?, cron?, rest?, acc?, dv?, r?, pos?, err?, ct? }
 *          k is the kind (type, method, page, component, action, view, api, external), n the name, p 1 for a public
 *          element; g is the grouping it belongs to (the type, page, component, group or endpoint), area the Structr area
 *          of that grouping (ui, components, integration, security, builtin, logic, scheduled, data), both worked out on the server;
 *          d is the DOM tree of a page or shared component as nested [tag, flags, children?, ids?, expressions?]
 *          with the flags 1 active, 2 has an event action mapping, 4 repeater, the ids of the repeaters and event
 *          action mappings that live on the element and the template expressions its text shows; crud lists what a method, action, repeater or endpoint does to which
 *          type ('C:Task', 'R:User'); svc marks a service class; cron is the schedule of a method that CronService runs;
 *          rest summarises the resource access grants on a type or endpoint as { n: rules, pub, auth, pubw, authw }, each
 *          0 or 1, for anonymous and authenticated users and whether they may write (the map reads n, pub and auth);
 *          acc says who sees a page or shared component as { pub, auth, own?: owner, g?: [[principal, permissions]] };
 *          dv whom every object of a type is visible to as { pub, auth }; r the URL routes of a page as [{ p: path,
 *          cur?: type of the current object, w?: warnings }], pos its position (the start page has the lowest), err the
 *          error codes it is shown for, ct a content type other than HTML
 * - edge:  [source id, target id, weight, kind, inferred (0 or 1), evidence, details?], kind one of include, reads,
 *          contains, member, creates, updates, deletes, uses, helper, calls, relates, invokes, navigates, endpoint,
 *          inherits, requests (to a web host or a geocoding or translation service), queries (a foreign database), executes
 *          (a script configured for exec), mails, current (a page to the type of the current object of its
 *          route); details of a schema relationship as { m: multiplicity, cd?: cascading delete, pp?: direction in which
 *          it passes on rights, r, w, d, a: Add, Keep or Remove per right }, see AppMapAccess
 * - group: { id, n, m: number of members, t: ids of the types the group has grants on, p: the rights per type as the
 *          letters r, w and d, in: ids of the groups it is a member of, g, area }
 * - site:  { id, n: host name and port, pages: ids }
 * - section: { id, n, how, members }, a grouping of pages and shared components that belong together and that is no
 *          element itself: by the category of its pages, the type they work with most, or the pages that hold or lead to
 *          them (how is category or type); its members carry its id as g
 *
 * The model has no geometry. It adds the Structr groups as nodes and derives the business relations from the technical
 * ones; the map shows both at once (links). Grouping and area come with the data, as n.c = [area, grouping].
 *
 * Repeaters and event action mappings are no elements of the map of their own: they live inside their page or shared
 * component, and a page close enough shows them in its DOM tree. Their relations start at the page, each remembering in
 * l.via the repeater or action mapping it comes from, so that an open page can draw it from the right HTML element.
 */
class AppMapModel {

	// component use, includes, rights and helper calls connect almost everything with everything; they stay hidden unless asked for
	static STRUCTURE = new Set(['include', 'uses', 'grant', 'helper']);

	// ----- the areas every Structr application has, each a sector around the centre, in the same order in every application -----
	// they follow the mechanisms Structr offers (see the class comment of ApplicationMap on the server); the pages always lie
	// on the left, clockwise follow the others; the order lets the relations between the areas take the shortest way: of all
	// circular orders it is the one where a relation crosses the fewest sectors on average, grants of groups on types included,
	// worked out on two real applications and kept fixed, so that every application is read the same way; the shared
	// components close the circle between the data model and the pages they are part of. How wide a sector is follows
	// from what it holds, see AppMapLayout.sectorsFor
	static AREAS = {
		ui:          { label: 'Pages' },
		builtin:     { label: 'System Types' },
		scheduled:   { label: 'Scheduled Tasks' },
		integration: { label: 'Integrations' },
		logic:       { label: 'Services & Functions' },
		security:    { label: 'Security' },
		data:        { label: 'Data Model' },
		components:  { label: 'Shared Components' }
	};

	static KIND_LABEL  = { page: 'Page', component: 'Shared Component', type: 'Type', method: 'Method', api: 'REST Endpoint', external: 'External Service', group: 'Group', view: 'Repeater', action: 'Event Action Mapping' };
	static EDGE_LABEL  = { creates: 'creates', updates: 'updates', deletes: 'deletes', inherits: 'inherits from', helper: 'uses helper', contains: 'contains', reads: 'reads', current: 'opens with the current', invokes: 'invokes', calls: 'calls', member: 'belongs to', relates: 'relationship', navigates: 'navigates to', endpoint: 'endpoint', include: 'includes', uses: 'uses', grant: 'grants', requests: 'sends requests to', queries: 'queries', executes: 'executes', mails: 'sends mail with' };

	// business relations: what a page or method does to which type (C/R/U/D), the data model between types, navigation
	// between pages; the wiring in between (page contains action, action invokes method, method calls method) is folded into them
	static OP_KIND = { C: 'creates', D: 'deletes', U: 'updates', R: 'reads' };
	static OPERATION_KINDS = new Set(Object.values(AppMapModel.OP_KIND));

	// the functional relations: what the application does, with its data, its methods and the world outside; the others
	// (the data model between types, inheritance, navigation between pages) describe how it is built
	static FUNCTIONAL = new Set(['creates', 'updates', 'deletes', 'reads', 'current', 'invokes', 'calls', 'endpoint', 'requests', 'queries', 'executes', 'mails']);

	// how much a relation says about what matters, for the rank of a grouping: changing data weighs most, reading least
	static RANK_WEIGHT = { creates: 3, updates: 2.5, deletes: 2.5, reads: 1, current: 1, invokes: 1.5, calls: 1.5, endpoint: 1, requests: 1.5, queries: 1.5, executes: 1.5, mails: 1.5 };

	// the hotspots: the highest ranked groupings that at least this many others use
	static HOTSPOTS      = 8;
	static HOTSPOT_USERS = 3;

	// the importance of a page or shared component, see importance: how much one more page that leads to it counts (by its
	// logarithm), a type it changes, a type it only shows, and being open to public users
	static IMPORTANCE = { reach: 2, changes: 2, shows: 1, public: 4 };
	static OP_RANK = ['C', 'D', 'U', 'R'];

	// the kinds that live inside a page or shared component instead of being elements of the map
	static INNER_KINDS = new Set(['view', 'action']);

	constructor(data) {

		this.nodes    = data.nodes.map(n => ({ ...n }));
		this.byId     = new Map(this.nodes.map(n => [n.id, n]));
		this.sections = new Map((data.sections ?? []).map(s => [s.id, s]));
		this.groups   = data.groups ?? [];
		this.sites    = data.sites ?? [];

		// x holds what a schema relationship does beyond connecting, see AppMapAccess
		const links = data.edges
			.filter(([a, b]) => this.byId.has(a) && this.byId.has(b))
			.map(([a, b, w, kind, inferred, evidence, x]) => ({ source: this.byId.get(a), target: this.byId.get(b), w, kind, inferred, evidence, ...(x ? { x } : {}) }));

		// a Structr group is an element of its own, connected to the types it has grants on
		for (const g of data.groups ?? []) {

			const targets = g.t.map(id => this.byId.get(id)).filter(Boolean);

			if (!targets.length) {
				continue;
			}

			const node = { id: g.id, k: 'group', n: g.n, m: g.m, g: g.g ?? g.id, area: g.area ?? 'security' };

			this.nodes.push(node);
			this.byId.set(node.id, node);
			targets.forEach(t => links.push({ source: node, target: t, w: 0.4, kind: 'grant', inferred: 0 }));
		}

		this.techLinks  = links;
		this.typeByName = new Map(this.nodes.filter(n => n.k === 'type').map(n => [n.n, n]));
		this.homeOf     = new Map(links.filter(l => l.kind === 'contains').map(l => [l.target, l.source]));
		this.bizLinks   = this.businessLinks();
		this.inner      = new Map();

		for (const n of this.nodes) {

			const home = AppMapModel.INNER_KINDS.has(n.k) ? (this.byId.get(n.g) ?? this.homeOf.get(n)) : null;

			if (home && home !== n && !AppMapModel.INNER_KINDS.has(home.k)) {
				this.inner.set(n, home);
			}
		}

		this.nodes     = this.nodes.filter(n => !this.inner.has(n));
		this.techLinks = this.moveInside(this.techLinks);
		this.bizLinks  = this.moveInside(this.bizLinks);

		this.links = this.bothLinks();

		this.assignAreas();
	}

	// the relations of repeaters and action mappings start at their page; the page containing them is no relation any more
	moveInside(links) {

		const result = [];

		for (const l of links) {

			const source = this.inner.get(l.source) ?? l.source;
			const target = this.inner.get(l.target) ?? l.target;

			if (source === target) {
				continue;
			}

			if (source === l.source && target === l.target) {

				result.push(l);

			} else {

				result.push({ ...l, source, target, via: this.inner.has(l.source) ? l.source : l.target });
			}
		}

		return result;
	}

	// which end of a relation depends on the other: the source, except for an endpoint, which depends on its method or type;
	// the undirected data model, the scaffolding and belonging do not count
	static dependency(l) {

		if (AppMapModel.STRUCTURE.has(l.kind) || l.kind === 'relates' || l.kind === 'contains' || l.kind === 'member') {
			return [null, null];
		}

		if (l.kind === 'endpoint') {
			return l.source.k === 'api' ? [l.source, l.target] : [l.target, l.source];
		}

		return [l.source, l.target];
	}

	crudOf(n) {

		return (n.crud ?? []).map(e => e.split(':')).map(([op, t]) => [op, this.typeByName.get(t)]).filter(([, t]) => t);
	}

	businessLinks() {

		const result = [];
		const ops    = new Map();
		const add    = (source, type, op) => {

			const key   = `${source.id}|${type.id}`;
			const entry = ops.get(key) ?? { source, target: type, ops: new Set() };

			entry.ops.add(op);
			ops.set(key, entry);
		};

		for (const n of this.nodes) {

			// a page or component does what its actions and repeaters do; a method what its script and the methods it calls do
			if (n.k === 'action' || n.k === 'view') {

				const home = this.homeOf.get(n);

				if (home) {
					this.crudOf(n).forEach(([op, t]) => add(home, t, op));
				}

			} else if (n.k === 'method' || n.k === 'api') {

				this.crudOf(n).forEach(([op, t]) => add(n, t, op));
			}
		}

		for (const e of ops.values()) {

			const strongest = AppMapModel.OP_RANK.find(op => e.ops.has(op));
			const ordered   = AppMapModel.OP_RANK.filter(op => e.ops.has(op)).join('');

			result.push({
				source:   e.source,
				target:   e.target,
				w:        1 + (ordered.length - 1) * 0.5 + (strongest === 'R' ? 0 : 1),
				kind:     AppMapModel.OP_KIND[strongest],
				ops:      ordered,
				inferred: 0,
				evidence: `${e.source.n} ${ordered} ${e.target.n}`
			});
		}

		for (const l of this.techLinks) {

			// the data model, where methods and actions belong, what goes out to external services and the scaffolding stay as they are
			if (['relates', 'inherits', 'member', 'endpoint', 'contains', 'requests', 'queries', 'executes', 'mails'].includes(l.kind) || AppMapModel.STRUCTURE.has(l.kind)) {

				result.push(l);

			} else if (l.kind === 'navigates') {

				const from = this.homeOf.get(l.source) ?? l.source;

				if (from !== l.target) {
					result.push({ ...l, source: from });
				}
			}
		}

		return result;
	}

	// both at once: the whole wiring, and from the business relations what the wiring does not say itself, the sum of what a
	// page does through its actions and what a method does through the methods it calls; one relation per pair of elements
	bothLinks() {

		const pair  = l => [l.source.id, l.target.id].sort().join('|');
		const wired = new Set(this.techLinks.map(pair));

		return [...this.techLinks, ...this.bizLinks.filter(l => !wired.has(pair(l)))];
	}

	/**
	 * What matters in the application and what it consists of, from the functional relations between the groupings: a
	 * preprocessing step of this view, like lift, so the endpoint keeps delivering the plain relations.
	 *
	 * The rank of a grouping follows its dependencies (PageRank): it is high when many rely on it that matter themselves;
	 * changing data weighs more than reading it. The hotspots are the HOTSPOTS groupings of highest rank times the square
	 * root of the number of groupings that use them, at least HOTSPOT_USERS: a type that only one other feeds inherits that
	 * one's rank but is no hotspot; an external service ranks high only because every call ends there, so it is none.
	 *
	 * The modules are the groupings that work together (Louvain, one level, Blondel et al. 2008): pages, types, methods and
	 * endpoints across the areas, each module named after its highest ranked grouping.
	 *
	 * Returns the module per grouping anchor and the hotspots, each with who changes it and what it sets off in turn.
	 */
	static analyse(links, anchorOf) {

		const functional = links.filter(l => AppMapModel.FUNCTIONAL.has(l.kind));
		const anchors    = [...new Set(functional.flatMap(l => [anchorOf(l.source), anchorOf(l.target)]))];
		const index      = new Map(anchors.map((n, i) => [n, i]));
		const outWeight  = new Float64Array(anchors.length);
		const edges      = [];

		for (const l of functional) {

			const [from, to] = AppMapModel.dependency(l);
			const a          = from ? index.get(anchorOf(from)) : null;
			const b          = to ? index.get(anchorOf(to)) : null;

			if (a != null && b != null && a !== b) {

				const w = AppMapModel.RANK_WEIGHT[l.kind] ?? 1;

				edges.push([a, b, w]);
				outWeight[a] += w;
			}
		}

		const rank    = AppMapModel.pageRank(anchors.length, edges, outWeight);
		const module  = AppMapModel.louvain(anchors.length, edges);
		const rankOf  = new Map(anchors.map((n, i) => [n, rank[i]]));
		const users   = new Map();
		const writers = new Map();
		const effects = new Map();
		const tally   = (map, key, value) => {

			const inner = map.get(key) ?? new Map();

			inner.set(value, (inner.get(value) ?? 0) + 1);
			map.set(key, inner);
		};

		for (const l of functional) {

			const [from, to] = AppMapModel.dependency(l);

			if (!from || anchorOf(from) === anchorOf(to)) {
				continue;
			}

			tally(users, anchorOf(to), anchorOf(from));

			if (AppMapStrands.WRITES.has(l.kind)) {
				tally(writers, anchorOf(l.target), anchorOf(l.source));
			}

			if (l.kind !== 'reads') {
				tally(effects, anchorOf(from), `${AppMapModel.EDGE_LABEL[l.kind] ?? l.kind} ${anchorOf(to).n}`);
			}
		}

		const strongest = (map, key, count) => [...(map.get(key) ?? new Map())].sort((a, b) => b[1] - a[1]).slice(0, count).map(([x]) => x);
		const modules   = d3.groups(anchors, n => module[index.get(n)])
			.map(([id, members]) => ({ id, members: members.sort((a, b) => rankOf.get(b) - rankOf.get(a)) }))
			.map(m => ({ ...m, name: m.members[0].n, rank: d3.sum(m.members, n => rankOf.get(n)) }))
			.sort((a, b) => b.rank - a.rank);
		const moduleOf  = new Map(modules.flatMap(m => m.members.map(n => [n, m])));
		const hotspots  = anchors
			.filter(n => n.k !== 'external' && (users.get(n)?.size ?? 0) >= AppMapModel.HOTSPOT_USERS)
			.sort((a, b) => rankOf.get(b) * Math.sqrt(users.get(b).size) - rankOf.get(a) * Math.sqrt(users.get(a).size))
			.slice(0, AppMapModel.HOTSPOTS)
			.map((n, i) => ({ anchor: n, place: i + 1, users: users.get(n).size, changedBy: strongest(writers, n, 3), effects: strongest(effects, n, 3) }));

		return { moduleOf, hotspots: new Map(hotspots.map(h => [h.anchor, h])) };
	}

	/**
	 * How important a page or shared component is, from two questions: how many pages lead to it, by a link, by the success
	 * or failure URL of an action or by holding it as a component (a link in a shared component leads there from every page
	 * that holds the component), and what it does, the types it changes and the types it only shows, itself, through its
	 * components and through the methods it runs. The scaffolding of a page counts for nothing, so a page of many includes
	 * that does little stays small, and a page nothing leads to that does nothing is a candidate for cleaning up.
	 *
	 * Returns per page and component { from, changes, shows, value }, from and the types as sets.
	 */
	static importance(links) {

		const isSheet  = n => n.k === 'page' || n.k === 'component';
		const add      = (map, key, value) => map.set(key, (map.get(key) ?? new Set()).add(value));
		const holders  = new Map();
		const holds    = new Map();
		const leadsTo  = new Map();
		const changes  = new Map();
		const shows    = new Map();
		const runs     = new Map();

		for (const l of links) {

			if (l.source === l.target) {
				continue;
			}

			if (l.kind === 'include' || l.kind === 'uses') {

				add(holders, l.target, l.source);
				add(holds, l.source, l.target);

			} else if (l.kind === 'navigates') {

				add(leadsTo, l.target, l.source);

			} else if (l.target.k === 'type' && AppMapStrands.WRITES.has(l.kind)) {

				add(changes, l.source, l.target);

			} else if (l.target.k === 'type' && (l.kind === 'reads' || l.kind === 'current')) {

				add(shows, l.source, l.target);

			} else if (l.kind === 'invokes' || l.kind === 'calls') {

				add(runs, l.source, l.target);
			}
		}

		// everything along a relation, transitively: the pages holding a component, the components a page holds
		const closure = (map, start) => {

			const seen = new Set([start]);
			const open = [start];

			while (open.length) {

				for (const m of map.get(open.pop()) ?? []) {

					if (!seen.has(m)) {

						seen.add(m);
						open.push(m);
					}
				}
			}

			seen.delete(start);

			return seen;
		};

		const pagesAt = n => [n, ...closure(holders, n)].filter(m => m.k === 'page');
		const result  = new Map();
		const weight  = AppMapModel.IMPORTANCE;
		const sheets  = new Set(links.flatMap(l => [l.source, l.target]).filter(isSheet));

		for (const n of sheets) {

			const from = new Set([...leadsTo.get(n) ?? []].flatMap(pagesAt));

			closure(holders, n).forEach(m => m.k === 'page' && from.add(m));
			from.delete(n);

			// what it does: itself, its components, and the methods any of them runs
			const doers   = [n, ...closure(holds, n)];
			const changed = new Set();
			const shown   = new Set();

			for (const d of [...doers, ...doers.flatMap(d => [...runs.get(d) ?? []])]) {

				changes.get(d)?.forEach(t => changed.add(t));
				shows.get(d)?.forEach(t => shown.add(t));
			}

			changed.forEach(t => shown.delete(t));

			const value = weight.reach * Math.log2(1 + from.size) + weight.changes * changed.size + weight.shows * shown.size + (n.p ? weight.public : 0);

			result.set(n, { from, changes: changed, shows: shown, value });
		}

		return result;
	}

	// PageRank with a damping of 0.85 over weighted edges [from, to, weight]; what a node without outgoing edges holds is
	// spread over all
	static pageRank(count, edges, outWeight) {

		let rank = new Float64Array(count).fill(1 / count);

		for (let step = 0; step < 60; step++) {

			const next   = new Float64Array(count).fill(0.15 / count);
			let dangling = 0;

			for (let i = 0; i < count; i++) {

				if (!outWeight[i]) {
					dangling += rank[i];
				}
			}

			for (const [a, b, w] of edges) {
				next[b] += 0.85 * rank[a] * w / outWeight[a];
			}

			for (let i = 0; i < count; i++) {
				next[i] += 0.85 * dangling / count;
			}

			rank = next;
		}

		return rank;
	}

	// the first level of Louvain on the undirected weighted graph: every node moves to the community of a neighbour while
	// that raises the modularity; returns the community of every node
	static louvain(count, edges) {

		const adjacent  = d3.range(count).map(() => new Map());
		const community = d3.range(count);

		for (const [a, b, w] of edges) {

			adjacent[a].set(b, (adjacent[a].get(b) ?? 0) + w);
			adjacent[b].set(a, (adjacent[b].get(a) ?? 0) + w);
		}

		const degree = adjacent.map(m => d3.sum(m.values()));
		const total  = new Map(community.map(c => [c, degree[c]]));
		const twice  = d3.sum(degree) || 1;

		for (let pass = 0; pass < 20; pass++) {

			let moved = 0;

			for (let i = 0; i < count; i++) {

				const own    = community[i];
				const toward = new Map();

				for (const [j, w] of adjacent[i]) {
					toward.set(community[j], (toward.get(community[j]) ?? 0) + w);
				}

				total.set(own, total.get(own) - degree[i]);

				let best = own;
				let gain = (toward.get(own) ?? 0) - total.get(own) * degree[i] / twice;

				for (const [c, w] of toward) {

					const g = w - total.get(c) * degree[i] / twice;

					if (g > gain) {

						best = c;
						gain = g;
					}
				}

				community[i] = best;
				total.set(best, total.get(best) + degree[i]);
				moved += best !== own ? 1 : 0;
			}

			if (!moved) {
				break;
			}
		}

		return community;
	}

	assignAreas() {

		for (const n of this.nodes) {

			const grouping = this.byId.has(n.g) || this.sections.has(n.g) ? n.g : n.id;

			n.c = [n.area ?? 'data', grouping];
		}
	}
}
