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
package org.structr.api.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class QueryTimer {

	private final Map<String, Double> timestamps = new LinkedHashMap<>();
	private String statement                     = null;

	// prevent access outside of package
	QueryTimer() {}

	public String getStatement() {
		return statement;
	}

	public double getOverallDuration() {

		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;

		for (final String key : timestamps.keySet()) {

			double value = value(key);

			min = Math.min(min, value);
			max = Math.max(max, value);
		}

		// return milliseconds
		return (max - min) / 1000000000.0;
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

	// ----- private methods -----
	private void recordTime(final String key) {
		timestamps.put(key, Double.valueOf(System.nanoTime()));
	}

	private double value(final String key) {

		Double value = timestamps.get(key);
		if (value != null) {

			return value;
		}

		return 0;
	}
}
