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
package org.structr.memory.index.predicate;

import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;

import java.util.Date;

/**
 */
public class RangePredicate<T extends PropertyContainer, V extends Comparable> implements Predicate<T> {

	private Class typeHint         = null;
	private String key             = null;
	private Comparable rangeStart  = null;
	private Comparable rangeEnd    = null;
	private boolean startInclusive = true;
	private boolean endInclusive   = true;

	public RangePredicate(final String key, final V rangeStart, final V rangeEnd, final Class typeHint) {

		this.key        = key;
		this.rangeStart = rangeStart;
		this.rangeEnd   = rangeEnd;
		this.typeHint   = typeHint;
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

		final Comparable value = convertWithTypeHint(entity.getProperty(key));
		final Comparable start = convertWithTypeHint(rangeStart);
		final Comparable end   = convertWithTypeHint(rangeEnd);

		if (value != null && value instanceof Comparable) {

			final Comparable actual = (Comparable)value;

			if (value.getClass().isArray()) {

				// how to implement range queries on array values?!

			} else {

				if (start == null && end != null) {

					// all values <= rangeEnd
					return lessThan(actual, end);
				}

				if (start != null && end == null) {

					// all values >= rangeStart
					return greaterThan(actual, start);
				}

				return greaterThan(actual, start) && lessThan(actual, end);
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

	private Comparable convertWithTypeHint(final Object value) {

		if (value != null && typeHint != null) {

			if (value.getClass().isAssignableFrom(typeHint)) {
				return (Comparable)value;
			}

			if (value instanceof Number) {

				final Number number = (Number)value;

				if (Long.class.equals(typeHint)) {
					return number.longValue();
				}

				if (Integer.class.equals(typeHint)) {
					return number.intValue();
				}

				if (Double.class.equals(typeHint)) {
					return number.doubleValue();
				}

				if (Date.class.equals(typeHint)) {
					return number.longValue();
				}
			}
		}

		return (Comparable)value;
	}
}
