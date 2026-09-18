/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.diff.compare;

import org.structr.diff.model.Entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Pairs the entities of two exports and says what differs.
 *
 * Two passes, and the order carries the whole argument.
 *
 * <p><b>Pass one</b> matches on the primary key, which is the uuid wherever the export carries
 * one. On real exports this settles almost everything.
 *
 * <p><b>Pass two</b> takes only what pass one left unmatched and retries on the fallback key, and
 * it applies one further rule: the fallback must be UNAMBIGUOUS within the residue on both sides.
 * That rule is not defensive programming, it is the reason the design works. Three ResourceAccess
 * grants share the signature {@code DOMElement/_id/event}; they pair up perfectly by uuid in pass
 * one and never reach pass two, so the ambiguity never arises. Were one of them genuinely
 * unmatched, pairing it with another by signature alone would be a guess, and this refuses to
 * guess: it reports an addition and a removal instead, which is honest about what is known.
 *
 * <p>Nothing here is aware of noise. Every difference found is reported; deciding which ones a
 * reader cares about is the classifier's job, and keeping that out of here is what guarantees the
 * matcher can never silently drop a real change.
 */
public class Matcher {

	/** Entities of one export, indexed by identity, plus whatever collided. */
	private static class Index {

		final Map<String, Entity> byIdentity = new LinkedHashMap<>();
		final List<Entity> collisions        = new LinkedList<>();

		Index(final List<Entity> entities) {

			for (final Entity e : entities) {

				if (byIdentity.putIfAbsent(e.getIdentity(), e) != null) {

					collisions.add(e);
				}
			}
		}
	}

	private final List<Delta> deltas        = new LinkedList<>();
	private final List<Entity> leftCollisions;
	private final List<Entity> rightCollisions;

	public Matcher(final List<Entity> left, final List<Entity> right) {

		final Index l = new Index(left);
		final Index r = new Index(right);

		this.leftCollisions  = l.collisions;
		this.rightCollisions = r.collisions;

		final Set<String> shared = new TreeSet<>(l.byIdentity.keySet());
		shared.retainAll(r.byIdentity.keySet());

		for (final String identity : shared) {

			describe(l.byIdentity.get(identity), r.byIdentity.get(identity), Delta.MatchedBy.KEY);
		}

		final List<Entity> leftRest  = residue(l, shared);
		final List<Entity> rightRest = residue(r, shared);

		matchByAlternateKey(leftRest, rightRest);

		// Signal is assigned here rather than left to the caller so that nothing can consume deltas
		// that were never classified and silently treat everything as normal.
		Classifier.classify(deltas);
	}

	public List<Delta> getDeltas() {

		return deltas;
	}

	/**
	 * Entities of one export that pass one did not pair, in a stable order.
	 */
	private static List<Entity> residue(final Index index, final Set<String> matched) {

		final List<Entity> rest = new ArrayList<>();

		for (final Map.Entry<String, Entity> entry : index.byIdentity.entrySet()) {

			if (!matched.contains(entry.getKey())) {

				rest.add(entry.getValue());
			}
		}

		return rest;
	}

	/**
	 * Pass two. Anything still unpaired afterwards is reported as a plain addition or removal.
	 */
	private void matchByAlternateKey(final List<Entity> leftRest, final List<Entity> rightRest) {

		final Map<String, Entity> leftAlt  = unambiguousByAlternateKey(leftRest);
		final Map<String, Entity> rightAlt = unambiguousByAlternateKey(rightRest);
		final Set<String> paired = new TreeSet<>(leftAlt.keySet());

		paired.retainAll(rightAlt.keySet());

		final Set<String> pairedIdentities = new TreeSet<>();

		for (final String alt : paired) {

			final Entity a = leftAlt.get(alt);
			final Entity b = rightAlt.get(alt);

			pairedIdentities.add(a.getIdentity());
			pairedIdentities.add(b.getIdentity());

			deltas.add(new Delta(a.getKind(), a.getKey(), Delta.Operation.REIDENTIFIED, a.getName() != null ? a.getName() : b.getName(),
				Delta.Detail.IDENTITY, a.getKey() + " -> " + b.getKey() + ", paired on " + a.getAlternateKey(),
				a.getOrigin(), Delta.MatchedBy.ALTERNATE_KEY));

			describe(a, b, Delta.MatchedBy.ALTERNATE_KEY);
		}

		for (final Entity e : leftRest) {

			if (!pairedIdentities.contains(e.getIdentity())) {

				deltas.add(Delta.removed(e));
			}
		}

		for (final Entity e : rightRest) {

			if (!pairedIdentities.contains(e.getIdentity())) {

				deltas.add(Delta.added(e));
			}
		}
	}

	/**
	 * Fallback index, keeping only the keys that identify exactly one entity.
	 *
	 * A fallback key that names two entities is no identity at all, and pairing on it would be a
	 * coin toss dressed up as a match.
	 */
	private static Map<String, Entity> unambiguousByAlternateKey(final List<Entity> entities) {

		final Map<String, Entity> single = new LinkedHashMap<>();
		final Set<String> ambiguous      = new TreeSet<>();

		for (final Entity e : entities) {

			final String alt = e.getAlternateIdentity();
			if (alt == null) {

				continue;
			}

			if (single.putIfAbsent(alt, e) != null) {

				ambiguous.add(alt);
			}
		}

		for (final String alt : ambiguous) {

			single.remove(alt);
		}

		return single;
	}

	/**
	 * What differs between two entities already established to be the same entity.
	 *
	 * One pair can produce several deltas, and that is deliberate: an element that was both moved
	 * and edited says more as two facts than as one merged verdict.
	 */
	private void describe(final Entity a, final Entity b, final Delta.MatchedBy matchedBy) {

		if (!Objects.equals(a.getName(), b.getName())) {

			deltas.add(new Delta(a.getKind(), a.getKey(), Delta.Operation.RENAMED, a.getName(), Delta.Detail.NAME, a.getName() + " -> " + b.getName(), a.getOrigin(), matchedBy));
		}

		if (!Objects.equals(a.getParent(), b.getParent()) || !Objects.equals(a.getOrdinal(), b.getOrdinal())) {

			deltas.add(new Delta(a.getKind(), a.getKey(), Delta.Operation.MOVED, a.getName(),
				Delta.Detail.POSITION, a.getParent() + "#" + a.getOrdinal() + " -> " + b.getParent() + "#" + b.getOrdinal(),
				a.getOrigin(), matchedBy));
		}

		final List<Delta.Change> changes = changedAttributes(a, b);
		if (!changes.isEmpty()) {

			final List<String> changedAttributes = changes.stream().map(Delta.Change::attribute).toList();

			deltas.add(new Delta(a.getKind(), a.getKey(), Delta.Operation.CHANGED, a.getName(),
				Delta.Detail.ATTRIBUTES, "attributes: " + String.join(", ", changedAttributes), a.getOrigin(), matchedBy, changes));
		}

		if (!Objects.equals(a.getContent(), b.getContent())) {

			// Whitespace-only is called out rather than suppressed. It LOOKED like export
			// formatting until it was measured: zero such deltas between two exports of one
			// instance, six between two instances. The serializer is deterministic, so these are
			// real differences in the database, and a filter here would have hidden real content.
			final boolean whitespaceOnly = trimmed(a.getContent()).equals(trimmed(b.getContent()));

			deltas.add(new Delta(a.getKind(), a.getKey(), Delta.Operation.CHANGED, a.getName(),
				whitespaceOnly ? Delta.Detail.CONTENT_WHITESPACE_ONLY : Delta.Detail.CONTENT,
				whitespaceOnly ? "content, whitespace only" : "content", a.getOrigin(), matchedBy,
				List.of(change("content", a.getContent(), b.getContent()))));
		}
	}

	private static String trimmed(final String s) {

		return s != null ? s.strip() : "";
	}

	/** How much of a value travels with a delta before it is cut; a cut always says that it was one. */
	private static final int MAX_VALUE_LENGTH = 400;

	private static List<Delta.Change> changedAttributes(final Entity a, final Entity b) {

		final Set<String> names         = new TreeSet<>(a.getAttributes().keySet());
		final List<Delta.Change> result = new ArrayList<>();

		names.addAll(b.getAttributes().keySet());

		for (final String name : names) {

			final Object from = a.getAttributes().get(name);
			final Object to   = b.getAttributes().get(name);

			if (!Objects.equals(from, to)) {

				result.add(change(name, from != null ? from.toString() : null, to != null ? to.toString() : null));
			}
		}

		return result;
	}

	/**
	 * One attribute with both of its values, cut to a readable length.
	 *
	 * Naming the attribute answers how much changed and never what, and both values are in hand right
	 * here. A cut value is marked as cut and reports its real length, so a reader is never left thinking
	 * a truncation is the content.
	 */
	private static Delta.Change change(final String attribute, final String from, final String to) {

		final int fromLength    = from != null ? from.length() : 0;
		final int toLength      = to   != null ? to.length()   : 0;
		final boolean truncated = fromLength > MAX_VALUE_LENGTH || toLength > MAX_VALUE_LENGTH;

		return new Delta.Change(attribute, cut(from), cut(to), truncated, fromLength, toLength);
	}

	private static String cut(final String value) {

		if (value == null || value.length() <= MAX_VALUE_LENGTH) {

			return value;
		}

		return value.substring(0, MAX_VALUE_LENGTH);
	}

	/**
	 * Entities whose identity was not unique within their own export.
	 *
	 * Nothing in the matcher fails over this, but it is worth surfacing: a collision means the
	 * identity policy for that kind is not actually identifying, and the diff for those entities
	 * is arbitrary rather than wrong-but-explainable.
	 */
	public List<Entity> getLeftCollisions() {

		return leftCollisions;
	}

	public List<Entity> getRightCollisions() {

		return rightCollisions;
	}
}
