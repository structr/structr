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
import org.structr.diff.model.Kind;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Answers whether two exports are the same application, and if so whether they share an identity lineage.
 */
public class Congruence {

	// above this share of common uuids the two exports are the same app carrying the same identities
	private static final double SAME_LINEAGE = 0.5;

	// name and structure agreement above this means the same application even when no uuid is shared
	private static final double SAME_APP = 0.5;

	// a page is structurally the same when its element count differs by no more than this share
	private static final double STRUCTURE_TOLERANCE = 0.1;

	// Entities every instance carries whatever application is installed, so they agree before an app is
	// even considered: config files are keyed by their path, and the bundled widgets ship with the platform.
	private static final Set<String> PLATFORM_KINDS = Set.of(Kind.CONFIG_FILE, Kind.WIDGET);

	/** When an export was taken, read from the File node rather than from the export, which carries no dates. */
	public record Taken(java.util.Date created, java.util.Date modified) {}

	public static Map<String, Object> of(final List<Entity> left, final List<Entity> right, final String leftVersion, final String rightVersion, final Taken leftTaken, final Taken rightTaken) {

		final double identity      = overlap(applicationKeys(left), applicationKeys(right));
		final double rawIdentity   = overlap(keys(left), keys(right));
		final double pages         = jaccard(names(left, Kind.PAGE), names(right, Kind.PAGE));
		final double schema        = jaccard(names(left, Kind.SCHEMA_TYPE), names(right, Kind.SCHEMA_TYPE));
		final double localizations = jaccard(names(left, Kind.LOCALIZATION), names(right, Kind.LOCALIZATION));
		final double structure     = structuralAgreement(left, right);

		// the four say the same thing from different angles, so a plain mean is honest enough; structure
		// only speaks for pages both sides have, which is why it does not carry more weight than the rest
		final double similarity = mean(pages, schema, localizations, structure);
		final String verdict     = verdict(identity, similarity);
		final Map<String, Object> result = new LinkedHashMap<>();

		result.put("verdict", verdict);
		result.put("explanation", explain(verdict, identity, similarity));
		result.put("identityOverlap", round(identity));
		result.put("identityOverlapAllEntities", round(rawIdentity));
		result.put("similarity", round(similarity));

		final Map<String, Object> signals = new LinkedHashMap<>();

		signals.put("pageNames", round(pages));
		signals.put("schemaTypes", round(schema));
		signals.put("localizations", round(localizations));
		signals.put("pageStructure", round(structure));

		result.put("signals", signals);
		result.put("provenance", provenance(leftVersion, rightVersion, leftTaken, rightTaken));

		return result;
	}

	private static String verdict(final double identity, final double similarity) {

		if (identity >= SAME_LINEAGE) {

			return "SAME_LINEAGE";
		}

		return similarity >= SAME_APP ? "SAME_APP_DIFFERENT_LINEAGE" : "DIFFERENT_APPS";
	}

	private static String explain(final String verdict, final double identity, final double similarity) {

		return switch (verdict) {

			case "SAME_LINEAGE" -> String.format(
				"Versions of one application: %.0f%% of application entities carry the same uuid, so a change between them can be expressed as an update.", identity * 100);

			case "SAME_APP_DIFFERENT_LINEAGE" -> String.format(
				"The same application, but installed independently: names and structure agree to %.0f%% while only %.0f%% of application uuids are shared. An update cannot be computed from identity alone.", similarity * 100, identity * 100);

			default -> String.format(
				"Different applications: names and structure agree to only %.0f%%, uuid overlap %.0f%%. A diff between them describes two unrelated apps rather than a change.", similarity * 100, identity * 100);
		};
	}

	/** Pages both sides name, whose element counts agree. Entities of one page share an origin file. */
	private static double structuralAgreement(final List<Entity> left, final List<Entity> right) {

		final Map<String, Integer> leftSizes  = sizesByOrigin(left);
		final Map<String, Integer> rightSizes = sizesByOrigin(right);
		final Set<String> shared              = new HashSet<>(leftSizes.keySet());
		final Set<String> either              = new HashSet<>(leftSizes.keySet());

		shared.retainAll(rightSizes.keySet());
		either.addAll(rightSizes.keySet());

		if (either.isEmpty()) {

			return 0.0;
		}

		int agreeing = 0;

		for (final String origin : shared) {

			final double a = leftSizes.get(origin);
			final double b = rightSizes.get(origin);
			final double max = Math.max(a, b);

			if (max == 0 || Math.abs(a - b) / max <= STRUCTURE_TOLERANCE) {

				agreeing++;
			}
		}

		// over every origin either side has, not just the shared ones: measured against the shared subset,
		// a handful of coincidental matches between unrelated apps scored 52% and dominated the mean

		return (double) agreeing / either.size();
	}

	private static Map<String, Integer> sizesByOrigin(final List<Entity> entities) {

		final Map<String, Integer> sizes = new HashMap<>();

		for (final Entity e : entities) {

			if (e.getOrigin() != null) {

				sizes.merge(e.getOrigin(), 1, Integer::sum);
			}
		}

		return sizes;
	}

	/**
	 * Context rather than evidence: which build each export came from and when it was taken. Deliberately
	 * not folded into the score, because two exports being close in time says nothing about whether they
	 * are the same app, but it does say whether a large difference between them is plausible.
	 */
	private static Map<String, Object> provenance(final String leftVersion, final String rightVersion, final Taken leftTaken, final Taken rightTaken) {

		final Map<String, Object> provenance = new LinkedHashMap<>();

		provenance.put("left", side(leftVersion, leftTaken));
		provenance.put("right", side(rightVersion, rightTaken));

		if (leftTaken != null && rightTaken != null && leftTaken.modified() != null && rightTaken.modified() != null) {

			final long days = Math.abs(rightTaken.modified().getTime() - leftTaken.modified().getTime()) / 86400000L;

			provenance.put("daysApart", days);
			provenance.put("newer", rightTaken.modified().after(leftTaken.modified()) ? "right" : "left");
		}

		return provenance;
	}

	private static Map<String, Object> side(final String version, final Taken taken) {

		final Map<String, Object> side = new LinkedHashMap<>();

		side.put("structrVersion", version);
		side.put("created", taken != null && taken.created() != null ? taken.created().toInstant().toString() : null);
		side.put("exported", taken != null && taken.modified() != null ? taken.modified().toInstant().toString() : null);

		return side;
	}

	private static Set<String> keys(final List<Entity> entities) {

		return entities.stream().map(Entity::getKey).filter(k -> k != null).collect(Collectors.toSet());
	}

	/** Keys that belong to the application rather than to the platform every instance ships with. */
	private static Set<String> applicationKeys(final List<Entity> entities) {

		return entities.stream()
			.filter(e -> !PLATFORM_KINDS.contains(e.getKind()))
			.map(Entity::getKey)
			.filter(k -> k != null)
			.collect(Collectors.toSet());
	}

	private static Set<String> names(final List<Entity> entities, final String kind) {

		return entities.stream()
			.filter(e -> kind.equals(e.getKind()))
			.map(e -> e.getName() != null ? e.getName() : e.getKey())
			.filter(n -> n != null)
			.collect(Collectors.toSet());
	}

	/** Share of the smaller set that the larger one also has: a subset should read as fully contained. */
	private static double overlap(final Set<String> a, final Set<String> b) {

		if (a.isEmpty() || b.isEmpty()) {

			return 0.0;
		}

		final Set<String> both = new HashSet<>(a);

		both.retainAll(b);

		// the smaller side on purpose, not the union: uuids do not collide, so a contained export is one lineage, and Jaccard would read a grown app as a different one
		return (double) both.size() / Math.min(a.size(), b.size());
	}

	private static double jaccard(final Set<String> a, final Set<String> b) {

		if (a.isEmpty() && b.isEmpty()) {

			return 1.0;
		}

		final Set<String> both = new HashSet<>(a);
		final Set<String> either = new HashSet<>(a);

		both.retainAll(b);
		either.addAll(b);

		return either.isEmpty() ? 0.0 : (double) both.size() / either.size();
	}

	private static double mean(final double... values) {

		double sum = 0.0;

		for (final double v : values) {

			sum += v;
		}

		return values.length == 0 ? 0.0 : sum / values.length;
	}

	private static double round(final double value) {

		return Math.round(value * 1000.0) / 1000.0;
	}

	private Congruence() {}
}
