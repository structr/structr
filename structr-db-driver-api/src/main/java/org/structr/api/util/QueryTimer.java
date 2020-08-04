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

import java.util.LinkedHashMap;
import java.util.Map;

public class QueryTimer {

	private final Map<String, Long> timestamps = new LinkedHashMap<>();
	private String statement                   = null;

	// prevent access outside of package
	QueryTimer() {}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		buf.append(getTimeDeltas());
		buf.append(": ");
		buf.append(statement);

		return buf.toString();
	}

	public String getStatement() {
		return statement;
	}

	public long getOverallDuration() {

		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;

		for (final String key : timestamps.keySet()) {

			long value = value(key);

			min = Math.min(min, value);
			max = Math.max(max, value);
		}

		return (max - min);
	}

	public boolean isEmpty() {
		return timestamps.isEmpty();
	}

	public void started(final String statement) {

		recordTime("started");

		this.statement = statement;
	}

	public void querySent() {
		recordTime("querySent");
	}

	public void closed() {
		recordTime("closed");
	}

	public void consumed() {
		recordTime("consumed");
	}

	public void finishReceived() {
		recordTime("finishReceived");
	}

	public void nextPage() {
		recordTime("nextPage");
	}

	public void finished() {
		recordTime("finished");
	}

	public Map<String, Long> getTimestamps() {
		return timestamps;
	}

	public Map<String, Long> getTimeDeltas() {

		final Map<String, Long> deltas = new LinkedHashMap<>();
		String previousKey             = null;
		long start                     = -1L;

		for (final String key : timestamps.keySet()) {

			if (start == -1) {

				start = value(key);

			} else {

				if (previousKey != null) {

					final long value = value(key);
					if (value > 0) {

						deltas.put(key, (value - start));
					}
				}
			}

			previousKey = key;

		}

		return deltas;
	}

	public boolean longerThan(final long milliseconds) {
		return value("consumed") - value("started") > milliseconds;
	}

	// ----- private methods -----
	private void recordTime(final String key) {
		timestamps.put(key, System.currentTimeMillis());
	}

	private long value(final String key) {

		Long value = timestamps.get(key);
		if (value != null) {

			return value;
		}

		return 0L;
	}
}
