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
 * Who may do what, as far as the application's schema and pages say it, for one role: public users, authenticated users
 * or the members of a group. A page is seen by a role when it is visible to public or authenticated users or grants read
 * to the group (or a group it is a member of). A type gives a role read, write or delete on all its objects through its
 * default visibility and the schema grants of the group, and through the schema relationships that pass rights on: a
 * right Added on the way reaches the other end, Kept it passes on what an earlier step added, Removed it stops (as
 * AccessControllableTraitDefinition resolves permissions). A type is governed by the schema when any of this applies to
 * it for any role; access to the objects of any other type rests on their owners and the grants on single objects, which
 * are no part of the map. A gap is a page the role sees that does with a governed type what the role may not: shows it
 * without read, changes it without write, deletes from it without delete.
 *
 * Also what deleting an object of a type takes with it (cascading delete), and what a type passes on to others.
 */
class AppMapAccess {

	static RIGHTS      = ['r', 'w', 'd'];
	static RIGHT_LABEL = { r: 'read', w: 'write', d: 'delete' };

	// what a page needs for what it does with a type
	static NEEDS = { reads: 'r', current: 'r', updates: 'w', deletes: 'd' };

	// cascading delete (Relation.CASCADING_DESCRIPTIONS): whether deleting the source deletes the target, and the other way
	static CASCADE = { 1: [true, false], 2: [false, true], 3: [true, true], 4: [true, true] };

	// how many gaps of a page its tooltip names
	static GAPS_SHOWN = 3;

	/**
	 * The access of a role, see the class comment; role is { kind: 'public' }, { kind: 'auth' } or { kind: 'group', id }.
	 * Returns { role, pages: page → reason or null, rights: type → Map right → reason, governed: types, rest: element →
	 * reason, gaps }.
	 */
	static of(model, links, role) {

		const groups   = new Map(model.groups.map(g => [g.id, g]));
		const member   = new Set();
		const signedIn = role.kind !== 'public';
		const walk     = id => {

			if (groups.has(id) && !member.has(id)) {

				member.add(id);
				(groups.get(id).in ?? []).forEach(walk);
			}
		};

		if (role.kind === 'group') {
			walk(role.id);
		}

		const names  = new Set([...member].map(id => groups.get(id).n));
		const pages  = new Map();
		const rights = new Map();
		const rest   = new Map();
		const via    = new Map();
		const give   = (type, right, why) => {

			const own = rights.get(type) ?? new Map();

			if (own.has(right)) {
				return false;
			}

			own.set(right, why);
			rights.set(type, own);

			return true;
		};

		for (const n of model.nodes) {

			if (n.k === 'page' || n.k === 'component') {

				const acc   = n.acc ?? {};
				const grant = (acc.g ?? []).find(([who, permissions]) => names.has(who) && String(permissions).split(',').includes('read'));

				pages.set(n, acc.pub ? 'visible to public users' : signedIn && acc.auth ? 'visible to authenticated users' : grant ? `read granted to ${grant[0]}` : null);
			}

			if (n.k === 'type') {

				if (n.dv?.pub) {
					give(n, 'r', 'every object is visible to public users');
				} else if (signedIn && n.dv?.auth) {
					give(n, 'r', 'every object is visible to authenticated users');
				}
			}

			// what resource access grants open to REST for the role
			if (n.rest && (n.rest.pub || (signedIn && n.rest.auth))) {
				rest.set(n, n.rest.pub ? 'open to public users' : 'open to authenticated users');
			}
		}

		for (const id of member) {

			for (const [typeId, letters] of Object.entries(groups.get(id).p ?? {})) {

				const type = model.byId.get(typeId);

				if (type) {
					[...letters].forEach(right => give(type, right, `schema grant for ${groups.get(id).n}`));
				}
			}
		}

		// the rights that schema relationships pass on, until nothing changes any more
		const hops     = AppMapAccess.propagation(links);
		const governed = new Set([
			...model.nodes.filter(n => n.k === 'type' && n.dv),
			...model.groups.flatMap(g => Object.keys(g.p ?? {})).map(id => model.byId.get(id)).filter(Boolean),
			...hops.map(hop => hop.to)
		]);

		for (let changed = true; changed;) {

			changed = false;

			for (const { from, to, l } of hops) {

				for (const right of AppMapAccess.RIGHTS) {

					const mode   = l.x[right];
					const passes = (mode === 'Add' && rights.get(from)?.has(right)) || (mode === 'Keep' && via.get(from)?.has(right));

					if (passes && give(to, right, `passed on from ${from.n}`)) {

						via.set(to, (via.get(to) ?? new Set()).add(right));
						changed = true;
					}
				}
			}
		}

		const gaps = [];
		const seen = new Set();

		for (const l of links) {

			const need = AppMapAccess.NEEDS[l.kind];
			const key  = `${l.source.id}|${l.target.id}|${need}`;

			if (need && pages.get(l.source) && governed.has(l.target) && !rights.get(l.target)?.has(need) && !seen.has(key)) {

				seen.add(key);
				gaps.push({ page: l.source, type: l.target, need, link: l });
			}
		}

		return { role, pages, rights, governed, rest, gaps };
	}

	// the steps along which schema relationships pass rights on, as { from, to, l }
	static propagation(links) {

		return links.filter(l => l.kind === 'relates' && l.x?.pp).flatMap(l => [
			...(l.x.pp === 'Out' || l.x.pp === 'Both' ? [{ from: l.source, to: l.target, l }] : []),
			...(l.x.pp === 'In' || l.x.pp === 'Both' ? [{ from: l.target, to: l.source, l }] : [])
		]);
	}

	/**
	 * What deleting an object of the type deletes with it, step by step along the schema relationships with cascading
	 * delete: type → { from, l, maybe } (maybe for a relationship that deletes only what would become invalid).
	 */
	static cascadeOf(links, type) {

		const reached = new Map();
		const open    = [type];

		while (open.length) {

			const current = open.shift();

			for (const l of links) {

				const [forward, backward] = l.kind === 'relates' ? (AppMapAccess.CASCADE[l.x?.cd] ?? [false, false]) : [false, false];
				const other               = forward && l.source === current ? l.target : backward && l.target === current ? l.source : null;

				if (other && other !== type && !reached.has(other)) {

					reached.set(other, { from: current, l, maybe: l.x.cd === 4 });
					open.push(other);
				}
			}
		}

		return reached;
	}

	// the access of the role to an element in words, for the tooltip
	static describe(access, n) {

		const who = access.role.name;

		if (access.pages.has(n)) {

			const reason = access.pages.get(n);
			const gaps   = access.gaps.filter(g => g.page === n).map(g => `${AppMapAccess.verb(g.need)} ${g.type.n} without ${AppMapAccess.RIGHT_LABEL[g.need]}`);
			const shown  = gaps.slice(0, AppMapAccess.GAPS_SHOWN).join(', ') + (gaps.length > AppMapAccess.GAPS_SHOWN ? ` and ${gaps.length - AppMapAccess.GAPS_SHOWN} more` : '');

			return (reason ? `${who} see it: ${reason}` : `${who} do not see it`) + (gaps.length ? ` · for ${who} it ${shown}` : '');
		}

		if (n.k === 'type') {

			const own  = access.rights.get(n);
			const list = own ? [...own].map(([right, why]) => `${AppMapAccess.RIGHT_LABEL[right]} (${why})`) : [];

			return !access.governed.has(n) ? `${who}: no schema rule, access rests on the owners of its objects and their grants`
				: list.length ? `${who} may ${list.join(', ')} on all objects`
				: `${who} may not use its objects unless they own them or were granted`;
		}

		return '';
	}

	static verb(need) {

		return { r: 'shows', w: 'changes', d: 'deletes from' }[need];
	}

	// whether the role reaches an element at all: a page it sees, a type it may do something with, reach over REST or that
	// the schema does not govern
	static reaches(access, n) {

		return access.pages.has(n) ? !!access.pages.get(n) : (access.rights.has(n) || access.rest.has(n) || (n.k === 'type' && !access.governed.has(n)));
	}
}
