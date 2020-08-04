/*
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class QueryHistogram {

	private static final List<QueryTimer> timers = new LinkedList<>();
	private static final int HISTOGRAM_SIZE      = 100_000;

	private static final String COUNT      = "Count";
	private static final String TOTAL_TIME = "Total time (ms)";
	private static final String MAX_TIME   = "Max time (ms)";
	private static final String MIN_TIME   = "Min time (ms)";

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

	public static synchronized List<Map<String, Object>> analyze() {

		final Map<String, Map<String, Object>> data = new LinkedHashMap<>();
		final List<Map<String, Object>> sorted      = new LinkedList<>();

		for (final QueryTimer timer : timers) {

			if (!timer.isEmpty()) {

				final String statement    = timer.getStatement();
				final long duration       = timer.getOverallDuration();
				Map<String, Object> value = data.get(statement);

				if (value == null) {

					value = new LinkedHashMap<>();
					data.put(statement, value);

					// store statement in map as well
					value.put("Query", statement);
				}

				value.put(COUNT,      get(value, COUNT, 0L) + 1);
				value.put(TOTAL_TIME, get(value, TOTAL_TIME, 0L) + duration);
				value.put(MAX_TIME,   Math.max(get(value, MAX_TIME, Long.MIN_VALUE), duration));
				value.put(MIN_TIME,   Math.min(get(value, MIN_TIME, Long.MAX_VALUE), duration));
			}
		}

		sorted.addAll(data.values());

		Collections.sort(sorted, (v1, v2) -> {

			final long count1 = get(v1, COUNT, 0L);
			final long count2 = get(v2, COUNT, 0L);

			return Long.compare(count2, count1);
		});

		return sorted;
	}

	private static long get(final Map<String, Object> value, final String key, final long defaultValue) {

		final Long data = (Long)value.get(key);
		if (data != null) {

			return data;
		}

		return defaultValue;
	}
}
