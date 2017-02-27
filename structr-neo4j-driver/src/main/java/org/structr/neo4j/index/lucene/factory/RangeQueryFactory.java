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
package org.structr.neo4j.index.lucene.factory;

import java.util.Date;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.neo4j.index.impl.lucene.LuceneUtil;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.RangeQuery;

/**
 *
 */
public class RangeQueryFactory implements QueryFactory<Query> {

	@Override
	public Query getQuery(final QueryFactory<Query> parent, final QueryPredicate predicate) {

		if (predicate instanceof RangeQuery) {

			final RangeQuery rangeQuery = (RangeQuery)predicate;
			final Object rangeStart     = rangeQuery.getRangeStart();
			final Object rangeEnd       = rangeQuery.getRangeEnd();

			if (rangeStart == null && rangeEnd == null) {
				return null;
			}

			if ((rangeStart != null && rangeStart instanceof Date) || (rangeEnd != null && rangeEnd instanceof Date)) {

				return LuceneUtil.rangeQuery(predicate.getName(), rangeStart == null ? null : ((Date) rangeStart).getTime(), rangeEnd == null ? null : ((Date) rangeEnd).getTime(), true, true);

			} else if ((rangeStart != null && rangeStart instanceof Number) || (rangeEnd != null && rangeEnd instanceof Number)) {

				return LuceneUtil.rangeQuery(predicate.getName(), rangeStart == null ? null : (Number) rangeStart, rangeEnd == null ? null : (Number) rangeEnd, true, true);

			} else {

				return new TermRangeQuery(predicate.getName(), rangeStart == null ? null : rangeStart.toString(), rangeEnd == null ? null : rangeEnd.toString(), true, true);

			}

		}

		return null;
	}
}
