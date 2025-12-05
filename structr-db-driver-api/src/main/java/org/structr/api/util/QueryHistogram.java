/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import java.util.*;

public class QueryHistogram {

	private static final List<QueryTimer> timers = new LinkedList<>();
	private static final int HISTOGRAM_SIZE      = 100_000;

	private static final String COUNT      = "Count";
	private static final String TOTAL_TIME = "Overall time (s)";
	private static final String MAX_TIME   = "Maximum time (s)";
	private static final String MIN_TIME   = "Minimum time (s)";
	private static final String AVG_TIME   = "Average time (s)";

	public static synchronized QueryTimer newTimer() {

		final QueryTimer timer = new QueryTimer();

		// remove first element
		if (timers.size() > HISTOGRAM_SIZE) {
			timers.remove(0);
		}

		timers.add(timer);

		return timer;
	}

	public static synchronized void clear() {
		timers.clear();
	}

	public static synchronized List<Map<String, Object>> analyze(final String sortKey, final int topCount) {

		final Map<String, Map<String, Object>> data = new LinkedHashMap<>();
		final List<Map<String, Object>> sorted      = new LinkedList<>();
		String actualSortKey                        = TOTAL_TIME;

		if (sortKey != null) {

			switch (sortKey) {

				case "min":
					actualSortKey = MIN_TIME;
					break;

				case "max":
					actualSortKey = MAX_TIME;
					break;

				case "avg":
					actualSortKey = AVG_TIME;
					break;

				case "count":
					actualSortKey = COUNT;
					break;
			}
		}

		for (final QueryTimer timer : timers) {

			if (!timer.isEmpty()) {

				final String statement    = timer.getStatement();
				final double duration     = timer.getOverallDuration();
				Map<String, Object> value = data.get(statement);

				if (value == null) {

					value = new LinkedHashMap<>();
					data.put(statement, value);

					// store statement in map as well
					value.put("Query", statement);
				}

				value.put(COUNT,      get(value, COUNT, 0.0) + 1.0);
				value.put(TOTAL_TIME, get(value, TOTAL_TIME, 0.0) + duration);
				value.put(MAX_TIME,   Math.max(get(value, MAX_TIME, Double.MIN_VALUE), duration));
				value.put(MIN_TIME,   Math.min(get(value, MIN_TIME, Double.MAX_VALUE), duration));
			}
		}

		for (final Map<String, Object> value : data.values()) {

			// calculate average
			final double count = get(value, COUNT,      1.0);
			final double time  = get(value, TOTAL_TIME, 0.0);

			if (count > 0) {

				value.put(AVG_TIME, (time / count));

			} else {

				value.put(AVG_TIME, 0L);
			}

			// round values to 4 decimal places
			round(value, COUNT,      0);
			round(value, TOTAL_TIME, 8);
			round(value, MAX_TIME,   8);
			round(value, MIN_TIME,   8);
			round(value, AVG_TIME,   8);

			sorted.add(value);
		}

		final String finalActualSortKey = actualSortKey;

		Collections.sort(sorted, (v1, v2) -> {

			final BigDecimal count1 = (BigDecimal)v1.get(finalActualSortKey);
			final BigDecimal count2 = (BigDecimal)v2.get(finalActualSortKey);

			return count2.compareTo(count1);
		});

		return sorted.subList(0, Math.max(0, Math.min(sorted.size(), topCount)));
	}

	private static double get(final Map<String, Object> value, final String key, final double defaultValue) {

		final Double data = (Double)value.get(key);
		if (data != null) {

			return data;
		}

		return defaultValue;
	}

	private static void round(final Map<String, Object> value, final String key, final int scale) {

		final BigDecimal decimal = BigDecimal.valueOf(get(value, key, 0.0));

		value.put(key, decimal.setScale(scale, RoundingMode.CEILING));
	}
}
