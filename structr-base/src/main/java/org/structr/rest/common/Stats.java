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
package org.structr.rest.common;

import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class Stats {

	// static final => can only be changed by restarting the app (would mess up stats otherwise)
	private static final long globalAggregationIntervalMilliseconds = Settings.HttpStatsAggregationInterval.getValue(60_000);

	private LinkedHashMap<Long, Long> values = new LinkedHashMap<>();
	private long count             = 0L;
	private long sum               = 0L;
	private long min               = Long.MAX_VALUE;
	private long max               = Long.MIN_VALUE;

	public void value(final long value, final boolean aggregateOnly) {

		if (!aggregateOnly) {

			synchronized (values) {

				final Long key = value - (value % globalAggregationIntervalMilliseconds);

				Long sum = values.get(key);
				if (sum == null) {

					values.put(key, 1L);

				} else {

					values.put(key, sum + 1);
				}
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

	public Map<Long, Long> aggregate(final long aggregationIntervalMilliseconds, final long maxCount) throws FrameworkException {

		// we can only return stats if the desired aggregation interval is a multiple of the global aggregation interval (our time resolution)
		if (aggregationIntervalMilliseconds % this.globalAggregationIntervalMilliseconds != 0) {

			throw new FrameworkException(422, "Requested aggregation interval " + aggregationIntervalMilliseconds + " is not a multiple of the global aggregation interval " + this.globalAggregationIntervalMilliseconds);
		}

		final Map<Long, Long> aggregation = new TreeMap<>();

		// aggregation interval matches => return data
		if (aggregationIntervalMilliseconds == this.globalAggregationIntervalMilliseconds) {

			synchronized (values) {

				for (final Map.Entry<Long, Long> entry : values.reversed().entrySet()) {

					aggregation.put(entry.getKey(), entry.getValue());

					// max
					if (aggregation.size() >= maxCount) {
						break;
					}
				}
			}

		} else {

			synchronized (values) {

				for (final Map.Entry<Long, Long> entry : values.reversed().entrySet()) {

					final Long originalKey = entry.getKey();
					final Long aggregationKey = originalKey - (originalKey % aggregationIntervalMilliseconds);
					final Long value = entry.getValue();

					Long sum = aggregation.get(aggregationKey);
					if (sum == null) {

						aggregation.put(aggregationKey, value);

					} else {

						aggregation.put(aggregationKey, sum + value);
					}

					// max
					if (aggregation.size() >= maxCount) {
						break;
					}
				}
			}
		}

		return aggregation;
	}
}