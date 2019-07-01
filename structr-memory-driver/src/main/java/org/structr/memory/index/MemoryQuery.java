/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.memory.index;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.search.QueryContext;
import org.structr.api.search.SortType;
import org.structr.api.index.DatabaseQuery;
import org.structr.api.util.Iterables;
import org.structr.memory.index.predicate.Conjunction;
import org.structr.memory.index.predicate.GroupPredicate;
import org.structr.memory.index.predicate.NotPredicate;

/**
 */
public class MemoryQuery<T extends PropertyContainer> implements DatabaseQuery, Predicate<T> {

	private final GroupPredicate<T> rootPredicate = new GroupPredicate<>(null, Conjunction.And);
	private final Set<String> labels              = new LinkedHashSet<>();
	private GroupPredicate<T> currentPredicate    = rootPredicate;
	private QueryContext queryContext             = null;
	private String sortKey                        = null;
	private boolean sortDescending                = false;
	private boolean negateNextPredicate           = false;

	public MemoryQuery(final QueryContext queryContext) {
		this.queryContext = queryContext;
	}

	public void addTypeLabel(final String typeLabel) {

		labels.add(typeLabel);
	}

	public Set<String> getTypeLabels() {
		return labels;
	}

	public void addPredicate(final Predicate<T> predicate) {

		if (negateNextPredicate) {

			negateNextPredicate = false;
			currentPredicate.add(new NotPredicate<>(predicate));

		} else {

			currentPredicate.add(predicate);
		}
	}

	@Override
	public void and() {
		currentPredicate.setConjunction(Conjunction.And);
	}

	@Override
	public void or() {
		currentPredicate.setConjunction(Conjunction.Or);
	}

	@Override
	public void not() {
		negateNextPredicate = true;
	}

	@Override
	public void andNot() {
		currentPredicate.setConjunction(Conjunction.And);
		not();
	}

	@Override
	public void sort(SortType sortType, String sortKey, boolean sortDescending) {

		this.sortDescending = sortDescending;
		this.sortKey        = sortKey;
	}

	public void beginGroup(final Conjunction conj) {

		final GroupPredicate<T> group = new GroupPredicate<>(currentPredicate, conj);

		currentPredicate.add(group);

		// enter group
		currentPredicate = group;
	}

	public void endGroup() {

		currentPredicate = currentPredicate.getParent();
	}

	public Iterable<T> sort(final Iterable<T> source) {

		if (sortKey != null) {

			try {

				final List<T> list = Iterables.toList(source);

				Collections.sort(list, new Sorter());

				return list;

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		return source;
	}

	@Override
	public boolean accept(final T value) {
		return rootPredicate.accept(value);
	}

	@Override
	public QueryContext getQueryContext() {
		return queryContext;
	}

	// ----- nested classes -----
	private class Sorter implements Comparator<T> {

		@Override
		public int compare(T o1, T o2) {

			final Object v1 = o1.getProperty(sortKey);
			final Object v2 = o2.getProperty(sortKey);

			if (v1 == null && v2 == null) {
				return 0;
			}

			if (v1 == null && v2 != null) {
				return sortDescending ? 1 : -1;
			}

			if (v1 != null && v2 == null) {
				return sortDescending ? -1 : 1;
			}

			if (v1 instanceof Comparable && v2 instanceof Comparable) {

				final Comparable c1 = (Comparable)v1;
				final Comparable c2 = (Comparable)v2;

				if (sortDescending) {

					return c2.compareTo(c1);

				} else {

					return c1.compareTo(c2);
				}
			}

			throw new ClassCastException("Cannot sort values of types " + v1.getClass().getName() + ", " + v2.getClass().getName());
		}
	}
}
