/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.rest.common;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Stats {

	private List<Long> values = new LinkedList<>();
	private long count        = 0L;
	private long sum          = 0L;
	private long min          = Long.MAX_VALUE;
	private long max          = Long.MIN_VALUE;

	public void value(final long value, final boolean aggregateOnly) {

		if (!aggregateOnly) {

			synchronized (values) {
				// list is reversed!
				values.add(0, value);
			}
		}

		sum += value;

		if (value < min) {
			min = value;
		}

		if (value > max) {
			max = value;
		}

		count++;
	}

	public List<Long> getValues() {
		return values;
	}

	public long getCount() {
		return count;
	}

	public long getMinValue() {
		return min;
	}

	public long getMaxValue() {
		return max;
	}

	public long getAverageValue() {
		return sum / count;
	}

	public Map<Long, Long> aggregate(final long aggregationIntervalMilliseconds, final long maxCount) {

		final Map<Long, Long> aggregation = new TreeMap<>();

		synchronized (values) {

			for (final Long value : values) {

				final Long key = value - (value % aggregationIntervalMilliseconds);

				Long sum = aggregation.get(key);
				if (sum == null) {

					aggregation.put(key, 1L);

				} else {

					aggregation.put(key, sum + 1);
				}

				// max
				if (aggregation.size() >= maxCount) {
					break;
				}
			}
		}

		return aggregation;
	}
}