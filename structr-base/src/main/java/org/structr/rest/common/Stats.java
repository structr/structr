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

public class Stats {

	private long count   = 0L;
	private long sum     = 0L;
	private long min     = Long.MAX_VALUE;
	private long max     = Long.MIN_VALUE;

	public void value(final long value) {

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
}