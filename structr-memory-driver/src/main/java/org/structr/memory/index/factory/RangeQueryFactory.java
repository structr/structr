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
package org.structr.memory.index.factory;

import org.structr.api.index.AbstractIndex;
import org.structr.api.index.AbstractQueryFactory;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.RangeQuery;
import org.structr.memory.index.MemoryQuery;
import org.structr.memory.index.predicate.RangePredicate;

/**
 *
 */
public class RangeQueryFactory extends AbstractQueryFactory<MemoryQuery> {

	public RangeQueryFactory(final AbstractIndex index) {
		super(index);
	}

	@Override
	public boolean createQuery(final QueryPredicate predicate, final MemoryQuery query, final boolean isFirst) {

		if (predicate instanceof RangeQuery) {

			checkOccur(query, predicate.getOccurrence(), isFirst);

			final RangeQuery rangeQuery = (RangeQuery)predicate;
			final Comparable rangeStart = (Comparable)getReadValue(rangeQuery.getRangeStart());
			final Comparable rangeEnd   = (Comparable)getReadValue(rangeQuery.getRangeEnd());
			final String name           = predicate.getName();

			if (rangeStart == null && rangeEnd == null) {
				return false;
			}

			final RangePredicate rangePredicate = new RangePredicate<>(name, rangeStart, rangeEnd, predicate.getType());

			rangePredicate.setStartInclusive(rangeQuery.getIncludeStart());
			rangePredicate.setEndInclusive(rangeQuery.getIncludeEnd());

			query.addPredicate(rangePredicate);

			return true;
		}

		return false;
	}
}
