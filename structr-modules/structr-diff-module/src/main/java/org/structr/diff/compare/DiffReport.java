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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A comparison, as plain maps and lists ready to be serialised.
 *
 * One shape serves two readers, which is the constraint that shaped it. A person opens a diff
 * wanting to know how big it is and where it lands before reading anything, so the summary comes
 * first and the counts are pre-aggregated rather than left to be derived. An agent wants every
 * delta addressable, so nothing is elided and each one carries its kind, key, operation and signal
 * as separate fields rather than folded into a sentence.
 *
 * <p>The human-readable {@code detail} is present too, but it is a courtesy: anything deciding
 * something should read {@code detailCode} and {@code signal}.
 */
public class DiffReport {

	public static Map<String, Object> of(final String leftName, final String rightName,
			final List<Entity> left, final List<Entity> right, final List<Delta> deltas,
			final Map<String, Object> congruence) {

		final Map<String, Object> report = new LinkedHashMap<>();

		report.put("summary", summary(leftName, rightName, left, right, deltas));
		report.put("congruence", congruence);
		report.put("profiles", profiles(left, right));
		report.put("deltas", deltaList(deltas));

		return report;
	}

	/** What each side contains, so the reader can see what the two apps are before reading what differs. */
	private static Map<String, Object> profiles(final List<Entity> left, final List<Entity> right) {

		final Map<String, Object> profiles = new LinkedHashMap<>();

		profiles.put("left", Profile.of(left));
		profiles.put("right", Profile.of(right));

		return profiles;
	}

	private static Map<String, Object> summary(final String leftName, final String rightName,
			final List<Entity> left, final List<Entity> right, final List<Delta> deltas) {

		final Map<String, Object> summary   = new LinkedHashMap<>();
		final Map<String, Integer> byGroup  = new TreeMap<>();
		int low                             = 0;

		for (final Delta delta : deltas) {

			byGroup.merge(delta.getOperation() + " " + delta.getKind(), 1, Integer::sum);

			if (Signal.LOW.equals(delta.getSignal())) {

				low++;
			}
		}

		summary.put("left", leftName);
		summary.put("right", rightName);
		summary.put("leftEntities", left.size());
		summary.put("rightEntities", right.size());
		summary.put("deltas", deltas.size());
		summary.put("normal", deltas.size() - low);
		summary.put("lowSignal", low);
		summary.put("byOperationAndKind", byGroup);

		return summary;
	}

	private static List<Map<String, Object>> deltaList(final List<Delta> deltas) {

		final List<Map<String, Object>> result = new ArrayList<>();

		for (final Delta delta : deltas) {

			final Map<String, Object> entry = new LinkedHashMap<>();

			entry.put("operation", delta.getOperation().name());
			entry.put("kind", delta.getKind());
			entry.put("key", delta.getKey());
			entry.put("name", delta.getName());
			entry.put("signal", delta.getSignal().name());
			entry.put("signalReason", delta.getSignalReason());
			entry.put("detailCode", delta.getDetailCode() != null ? delta.getDetailCode().name() : null);
			entry.put("detail", delta.getDetail());
			entry.put("origin", delta.getOrigin());
			entry.put("matchedBy", delta.getMatchedBy().name());

			result.add(entry);
		}

		return result;
	}

	private DiffReport() {}
}
