/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.memory.index.predicate;

import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;

/**
 */
public class RangePredicate<T extends PropertyContainer, V extends Comparable> implements Predicate<T> {

	private String key             = null;
	private Comparable rangeStart  = null;
	private Comparable rangeEnd    = null;
	private boolean startInclusive = true;
	private boolean endInclusive   = true;

	public RangePredicate(final String key, final V rangeStart, final V rangeEnd) {

		this.key        = key;
		this.rangeStart = rangeStart;
		this.rangeEnd   = rangeEnd;
	}

	public RangePredicate<T, V> setStartInclusive(final boolean startInclusive) {
		this.startInclusive = startInclusive;
		return this;
	}

	public RangePredicate<T, V> setEndInclusive(final boolean endInclusive) {
		this.endInclusive = endInclusive;
		return this;
	}

	@Override
	public boolean accept(final T entity) {

		final Object value = entity.getProperty(key);

		if (value != null && value instanceof Comparable) {

			final Comparable actual = (Comparable)value;

			if (value.getClass().isArray()) {

				// how to implement range queries on array values?!

			} else {

				if (rangeStart == null && rangeEnd != null) {

					// all values <= rangeEnd
					return lessThan(actual, rangeEnd);
				}

				if (rangeStart != null && rangeEnd == null) {

					// all values >= rangeStart
					return greaterThan(actual, rangeStart);
				}

				return greaterThan(actual, rangeStart) && lessThan(actual, rangeEnd);
			}
		}

		return false;
	}

	// ----- private methods -----
	private boolean greaterThan(final Comparable actual, final Comparable expected) {

		if (startInclusive) {

			return actual.compareTo(expected) >= 0;
		}

		return actual.compareTo(expected) > 0;
	}

	private boolean lessThan(final Comparable actual, final Comparable expected) {

		if (endInclusive) {

			return actual.compareTo(expected) <= 0;
		}

		return actual.compareTo(expected) < 0;
	}
}
