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
package org.structr.api.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects the duration of every query per statement since startup: count, total, minimum and maximum.
 * Memory depends on the number of distinct statements, which is capped, and recording a query only
 * locks the entry of its own statement.
 */
public class QueryHistogram {

	private static final Map<String, Entry> entries = new ConcurrentHashMap<>();
	private static final int MAX_STATEMENTS         = 10_000;
	private static final String OTHER_STATEMENTS    = "(statements beyond the first " + MAX_STATEMENTS + " distinct ones)";

	private static final String QUERY      = "Query";
	private static final String COUNT      = "Count";
	private static final String TOTAL_TIME = "Overall time (s)";
	private static final String MAX_TIME   = "Maximum time (s)";
	private static final String MIN_TIME   = "Minimum time (s)";
	private static final String AVG_TIME   = "Average time (s)";

	public static QueryTimer newTimer() {
		return new QueryTimer();
	}

	public static void clear() {
		entries.clear();
	}

	static void record(final String statement, final long nanos) {

		Entry entry = entries.get(statement);
		if (entry == null) {

			// statements with inline values would otherwise grow the map without end
			final String key = entries.size() < MAX_STATEMENTS ? statement : OTHER_STATEMENTS;

			entry = entries.computeIfAbsent(key, k -> new Entry());
		}

		entry.add(nanos);
	}

	public static List<Map<String, Object>> analyze(final String sortKey, final int topCount) {

		final List<Map<String, Object>> sorted = new ArrayList<>();
		final String actualSortKey             = switch (sortKey != null ? sortKey : "") {

			case "min"   -> MIN_TIME;
			case "max"   -> MAX_TIME;
			case "avg"   -> AVG_TIME;
			case "count" -> COUNT;
			default      -> TOTAL_TIME;
		};

		for (final Map.Entry<String, Entry> entry : entries.entrySet()) {

			final long[] values = entry.getValue().snapshot();
			final long count    = values[0];

			if (count == 0) {

				continue;
			}

			final Map<String, Object> value = new LinkedHashMap<>();

			value.put(QUERY,      entry.getKey());
			value.put(COUNT,      BigDecimal.valueOf(count));
			value.put(TOTAL_TIME, seconds(values[1]));
			value.put(MAX_TIME,   seconds(values[3]));
			value.put(MIN_TIME,   seconds(values[2]));
			value.put(AVG_TIME,   seconds(values[1] / (double) count));

			sorted.add(value);
		}

		sorted.sort((v1, v2) -> ((BigDecimal) v2.get(actualSortKey)).compareTo((BigDecimal) v1.get(actualSortKey)));

		return sorted.subList(0, Math.max(0, Math.min(sorted.size(), topCount)));
	}

	// ----- private methods -----
	private static BigDecimal seconds(final double nanos) {

		return BigDecimal.valueOf(nanos / 1_000_000_000.0).setScale(8, RoundingMode.CEILING);
	}

	private static final class Entry {

		private long count = 0L;
		private long total = 0L;
		private long min   = Long.MAX_VALUE;
		private long max   = 0L;

		synchronized void add(final long nanos) {

			count++;
			total += nanos;
			min    = Math.min(min, nanos);
			max    = Math.max(max, nanos);
		}

		synchronized long[] snapshot() {

			return new long[] { count, total, min, max };
		}
	}
}
