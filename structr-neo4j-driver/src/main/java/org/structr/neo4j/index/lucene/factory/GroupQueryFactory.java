/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.structr.api.search.GroupQuery;
import org.structr.api.search.Occurrence;
import org.structr.api.search.QueryPredicate;

/**
 *
 */
public class GroupQueryFactory extends AbstractQueryFactory<Query> {

	@Override
	public Query getQuery(final QueryFactory<Query> parent, final QueryPredicate predicate) {

		if (predicate instanceof GroupQuery) {

			final BooleanQuery query = new BooleanQuery();
			final GroupQuery group   = (GroupQuery)predicate;

			for (final QueryPredicate attr : group.getQueryPredicates()) {

				final Query subQuery = parent.getQuery(parent, attr);
				if (subQuery != null) {

					final Occurrence occur = attr.getOccurrence();
					query.add(subQuery, getOccur(occur));
				}
			}

			return query;
		}

		return null;
	}

	// ----- private methods -----
	private Occur getOccur(final Occurrence occ) {

		switch (occ) {

			case FORBIDDEN:
				return Occur.MUST_NOT;

			case OPTIONAL:
				return Occur.SHOULD;

			default:
				return Occur.MUST;
		}
	}
}
