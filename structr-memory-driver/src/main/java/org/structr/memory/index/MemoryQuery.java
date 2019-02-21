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
package org.structr.memory.index;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.search.QueryContext;
import org.structr.api.search.SortType;
import org.structr.api.index.DatabaseQuery;
import org.structr.api.util.Iterables;
import org.structr.memory.index.predicate.Conjunction;
import org.structr.memory.index.predicate.GroupPredicate;

/**
 *
 * @author Christian Morgner
 */
public class MemoryQuery<T extends PropertyContainer> implements DatabaseQuery, Predicate<T> {

	private QueryContext queryContext          = new QueryContext();
	private GroupPredicate<T> rootPredicate    = new GroupPredicate<>(null);
	private GroupPredicate<T> currentPredicate = rootPredicate;
	private String sortKey                     = null;
	private String mainType                    = null;
	private boolean sortDescending             = false;

	public MemoryQuery() {
	}

	public void setMainType(final String mainType) {
		this.mainType = mainType;
	}

	public String getMainType() {
		return mainType;
	}

	public void addPredicate(final Predicate<T> predicate) {
		currentPredicate.add(predicate);
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
		currentPredicate.setConjunction(Conjunction.Not);
	}

	@Override
	public void andNot() {
		throw new UnsupportedOperationException("AND NOT is not supported yet.");
	}

	@Override
	public void sort(SortType sortType, String sortKey, boolean sortDescending) {

		this.sortDescending = sortDescending;
		this.sortKey        = sortKey;
	}

	public void beginGroup() {

		final GroupPredicate<T> group = new GroupPredicate<>(currentPredicate);

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
