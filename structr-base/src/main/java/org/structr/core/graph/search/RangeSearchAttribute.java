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
package org.structr.core.graph.search;

import org.structr.api.search.RangeQuery;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;

/**
 * Search attribute for range queries
 *
 *
 *
 */
public class RangeSearchAttribute<T> extends SearchAttribute<T> implements RangeQuery {

	private PropertyKey<T> searchKey = null;
	private boolean includeStart     = true;
	private boolean includeEnd       = true;
	private T rangeStart             = null;
	private T rangeEnd               = null;

	public RangeSearchAttribute(final PropertyKey<T> searchKey, final T rangeStart, final T rangeEnd, final boolean includeStart, final boolean includeEnd) {

		super(null, null);

		this.includeStart = includeStart;
		this.includeEnd   = includeEnd;
		this.searchKey    = searchKey;
		this.rangeStart   = rangeStart;
		this.rangeEnd     = rangeEnd;
	}

	@Override
	public String toString() {
		return "RangeSearchAttribute()";
	}

	public PropertyKey getKey() {
		return searchKey;
	}

	@Override
	public T getValue() {
		return null;
	}

	@Override
	public boolean isExactMatch() {
		return true;
	}

	@Override
	public boolean includeInResult(GraphObject entity) {

		final T value = entity.getProperty(searchKey);
		if (value != null) {

			final Comparable cv = (Comparable)value;
			final Comparable cs = (Comparable)rangeStart;
			final Comparable ce = (Comparable)rangeEnd;

			if (includeStart && includeEnd) {
				return (cs == null || cs.compareTo(cv) <= 0) && (ce == null || ce.compareTo(cv) >= 0);
			}

			if (includeStart && !includeEnd) {
				return (cs == null || cs.compareTo(cv) <= 0) && (ce == null || ce.compareTo(cv) > 0);
			}

			if (!includeStart && includeEnd) {
				return (cs == null || cs.compareTo(cv) < 0) && (ce == null || ce.compareTo(cv) >= 0);
			}

			if (!includeStart && !includeEnd) {
				return (cs == null || cs.compareTo(cv) < 0) && (ce == null || ce.compareTo(cv) > 0);
			}
		}

		return false;
	}

	@Override
	public T getRangeStart() {
		return rangeStart;
	}

	@Override
	public T getRangeEnd() {
		return rangeEnd;
	}

	@Override
	public boolean getIncludeStart() {
		return includeStart;
	}

	@Override
	public boolean getIncludeEnd() {
		return includeEnd;
	}

	@Override
	public Class getQueryType() {
		return RangeQuery.class;
	}
}
