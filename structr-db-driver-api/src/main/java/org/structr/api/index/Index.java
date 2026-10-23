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
package org.structr.api.index;

import org.structr.api.search.QueryContext;
import org.structr.api.search.QueryPredicate;

import java.util.Map;

/**
 *
 */
public interface Index<T> {

	Iterable<T> query(final QueryContext context, final QueryPredicate predicate, final int requestedPageSize, final int requestedPage);
	Map<T, Double> fulltextQuery(final String indexName, final String searchString);

	/**
	 * The nearest neighbours of a vector in a vector index, best first, with their similarity score
	 * between 0 and 1. Fewer than k if the database has a tenant label and some of them belong to other
	 * tenants. Empty if the database has no vector indexes.
	 *
	 * @param indexName the name of the vector index
	 * @param k         how many neighbours the index is asked for
	 */
	default Map<T, Double> vectorQuery(final String indexName, final int k, final double[] vector) {

		return Map.of();
	}
	boolean supports(final Class type);

	/**
	 * The number of results of a query, counted by the database without reading them, or -1 if this index
	 * cannot count the query, which the caller then counts by iterating. The count knows nothing about
	 * permissions, so it is only correct for a caller that may read everything.
	 */
	default long count(final QueryContext context, final QueryPredicate predicate) {

		return -1;
	}
}
