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
package org.structr.core.graph.search;

import org.structr.api.search.FulltextQuery;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.RangeQuery;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;

/**
 * Search attribute for range queries
 *
 *
 *
 */
public class FulltextSearchAttribute<T> extends SearchAttribute<T> implements QueryPredicate {

	private String value = null;

	public FulltextSearchAttribute(final PropertyKey<T> searchKey, final String value) {

		super(searchKey, (T) value);

		this.value = value;

	}

	@Override
	public String toString() { return "FulltextSearchAttribute(" + value + "))"; }

	@Override
	public T getValue() {
		return (T) value;
	}

	@Override
	public boolean isExactMatch() {
		return false;
	}

	@Override
	public boolean includeInResult(GraphObject entity) {
		return true;
	}

	@Override
	public Class getQueryType() {
		return FulltextQuery.class;
	}
}
