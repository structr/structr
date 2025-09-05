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
package org.structr.api.index;

import org.structr.api.search.QueryPredicate;

/**
 *
 */
public interface QueryFactory<T extends DatabaseQuery> {

	/**
	 * Modifies the query according to the given predicate, returns a boolean that
	 * indicates whether the query was modified or not.
	 *
	 * @param predicate the predicate
	 * @param query the query
	 * @param isFirst the isFirst
	 *
	 * @return a boolean that indicates whether the query was modified or not
	 */
	boolean createQuery(final QueryPredicate predicate, final T query, final boolean isFirst);
}
