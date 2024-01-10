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
package org.structr.memory.index;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.index.DatabaseQuery;
import org.structr.api.search.QueryContext;
import org.structr.api.search.SortOrder;
import org.structr.api.search.SortSpec;
import org.structr.api.util.Iterables;
import org.structr.memory.index.predicate.Conjunction;
import org.structr.memory.index.predicate.GroupPredicate;
import org.structr.memory.index.predicate.NotPredicate;

import java.util.*;

/**
 */
public class MemoryQuery<T extends PropertyContainer> implements DatabaseQuery, Predicate<T> {

	private static final Logger logger = LoggerFactory.getLogger(MemoryQuery.class);

	private final GroupPredicate<T> rootPredicate = new GroupPredicate<>(null, Conjunction.And);
	private final Set<String> labels              = new LinkedHashSet<>();
	private GroupPredicate<T> currentPredicate    = rootPredicate;
	private QueryContext queryContext             = null;
	private SortOrder sortOrder                   = null;
	private boolean negateNextPredicate           = false;

	public MemoryQuery(final QueryContext queryContext) {
		this.queryContext = queryContext;
	}

	@Override
	public String toString() {
		return "MemoryQuery(" + labels.toString() + ")";
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
	public void sort(final SortOrder sortOrder) {
		this.sortOrder = sortOrder;
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

		if (sortOrder != null) {

			try {

				final List<T> list = Iterables.toList(source);

				Collections.sort(list, new Sorter(sortOrder));

				return list;

			} catch (Throwable t) {
				logger.error(ExceptionUtils.getStackTrace(t));
			}
		}

		return source;
	}

	@Override
	public boolean accept(final T value) {
		return rootPredicate.accept(value);
	}

	public QueryContext getQueryContext() {
		return queryContext;
	}

	// ----- nested classes -----
	private class Sorter implements Comparator<T> {

		private SortOrder sortOrder = null;

		public Sorter(final SortOrder order) {
			this.sortOrder = order;
		}

		@Override
		public int compare(final T o1, final T o2) {

			if (o1 == null || o2 == null) {
				throw new NullPointerException("Cannot compare null objects.");
			}

			if (o1 instanceof PropertyContainer && o2 instanceof PropertyContainer) {

				final PropertyContainer g1 = (PropertyContainer)o1;
				final PropertyContainer g2 = (PropertyContainer)o2;

				for (final SortSpec spec : sortOrder.getSortElements()) {

					final String key   = spec.getSortKey();
					final boolean desc = spec.sortDescending();
					Object v1          = g1.getProperty(key);
					Object v2          = g2.getProperty(key);

					if (v1 == null || v2 == null) {

						if (v1 == null && v2 == null) {

							return 0;

						} else if (v1 == null) {

							// sort order is "nulls last"
							return desc ? -1 : 1;

						} else {

							return desc ? 1 : -1;

						}
					}

					if (v1 instanceof Comparable && v2 instanceof Comparable) {

						Comparable c1 = (Comparable)v1;
						Comparable c2 = (Comparable)v2;

						final int result = desc ? c2.compareTo(c1) : c1.compareTo(c2);
						if (result != 0) {

							// return result if values are different, stay in loop if values are equal
							return result;
						}

					} else {

						throw new ClassCastException("Cannot sort values of types " + v1.getClass().getName() + ", " + v2.getClass().getName());
					}
				}

				// if we arrive here, the values for all the keys are equal
				return 0;
			}

			throw new ClassCastException("Cannot sort values of types " + o1.getClass().getName() + ", " + o2.getClass().getName());
		}
	}
}
