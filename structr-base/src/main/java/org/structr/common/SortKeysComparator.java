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
package org.structr.common;

import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Compares objects by a list of sort keys, each with its own order, natural order and position of
 * missing values: the first key decides, the next one only for objects that are equal in all keys
 * before it. A key is either a property or a path through related objects, like owner.name.
 *
 * In natural order, strings are compared with a {@link NaturalStringComparator}. A missing value
 * comes last in ascending and first in descending order, unless nulls are explicitly placed first
 * or last, which then applies regardless of the order, like NULLS FIRST and NULLS LAST in SQL.
 */
public class SortKeysComparator implements Comparator<Object> {

	public enum Nulls {

		First, Last
	}

	private final List<SortKey> keys = new ArrayList<>();
	private final ActionContext actionContext;

	public SortKeysComparator(final ActionContext actionContext) {

		this.actionContext = actionContext;
	}

	/**
	 * @param natural whether strings are compared in natural order, in the locale of the action context
	 * @param nulls   the position of missing values, or null for last in ascending and first in descending order
	 */
	public void addKey(final PropertyKey key, final boolean descending, final boolean natural, final Nulls nulls) {

		keys.add(new SortKey(cached(obj -> obj.getComparableProperty(key)), descending, valueComparator(natural), nulls));
	}

	public void addPath(final String path, final boolean descending, final boolean natural, final Nulls nulls) {

		final Comparator<Comparable> values    = valueComparator(natural);
		final PathResolvingComparator resolver = new PathResolvingComparator(actionContext, path, descending, values);

		// the resolver keeps the resolved values itself
		keys.add(new SortKey(resolver::getValue, descending, values, nulls));
	}

	/**
	 * A comparator for collections of plain values like strings, which have no sort keys.
	 */
	public Comparator<Object> forValues(final boolean descending, final boolean natural, final Nulls nulls) {

		final Comparator<Comparable> values = valueComparator(natural);

		return (o1, o2) -> compareValues((Comparable)o1, (Comparable)o2, descending, values, nulls);
	}

	@Override
	public int compare(final Object o1, final Object o2) {

		for (final SortKey key : keys) {

			final int result = compareValues(key.valueOf(o1), key.valueOf(o2), key.descending, key.values, key.nulls);
			if (result != 0) {

				return result;
			}
		}

		return 0;
	}

	// ----- private methods -----
	private int compareValues(final Comparable value1, final Comparable value2, final boolean descending, final Comparator<Comparable> values, final Nulls nulls) {

		if (value1 == null || value2 == null) {

			if (value1 == null && value2 == null) {

				return 0;
			}

			// a missing value counts as larger than any value, unless nulls have an explicit position
			final int missingIsLarger = value1 == null ? 1 : -1;

			if (nulls == Nulls.First) {

				return -missingIsLarger;
			}

			if (nulls == Nulls.Last) {

				return missingIsLarger;
			}

			return descending ? -missingIsLarger : missingIsLarger;
		}

		return descending ? values.compare(value2, value1) : values.compare(value1, value2);
	}

	private Comparator<Comparable> valueComparator(final boolean natural) {

		if (!natural) {

			return Comparable::compareTo;
		}

		final NaturalStringComparator strings = new NaturalStringComparator(actionContext.getLocale());

		return (value1, value2) -> {

			if (value1 instanceof String s1 && value2 instanceof String s2) {

				return strings.compare(s1, s2);
			}

			return value1.compareTo(value2);
		};
	}

	/**
	 * A sort compares each object many times, and a property can be computed, so each value is resolved once.
	 */
	private Function<GraphObject, Comparable> cached(final Function<GraphObject, Comparable> resolver) {

		final Map<GraphObject, Comparable> values = new IdentityHashMap<>();

		return obj -> {

			// not computeIfAbsent, which does not store a missing value
			if (!values.containsKey(obj)) {

				values.put(obj, resolver.apply(obj));
			}

			return values.get(obj);
		};
	}

	// ----- nested classes -----
	private record SortKey(Function<GraphObject, Comparable> resolver, boolean descending, Comparator<Comparable> values, Nulls nulls) {

		// an element of the collection that is null has no value for any key
		Comparable valueOf(final Object obj) {

			return obj instanceof GraphObject graphObject ? resolver.apply(graphObject) : null;
		}
	}
}
