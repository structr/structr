/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.bolt.index.factory;

import org.structr.api.search.QueryPredicate;
import org.structr.api.search.RangeQuery;
import org.structr.bolt.index.AdvancedCypherQuery;

/**
 *
 */
public class RangeQueryFactory extends AbstractQueryFactory {

	@Override
	public boolean createQuery(final QueryFactory parent, final QueryPredicate predicate, final AdvancedCypherQuery query, final boolean isFirst) {

		if (predicate instanceof RangeQuery) {

			checkOccur(query, predicate.getOccurrence(), isFirst);

			final RangeQuery rangeQuery = (RangeQuery)predicate;
			final Object rangeStart     = getReadValue(rangeQuery.getRangeStart());
			final Object rangeEnd       = getReadValue(rangeQuery.getRangeEnd());
			final String name           = predicate.getName();

			if (rangeStart == null && rangeEnd == null) {
				return false;
			}

			query.addParameters(name, ">=", rangeStart, "<=", rangeEnd);

			return true;
		}

		return false;
	}
}
