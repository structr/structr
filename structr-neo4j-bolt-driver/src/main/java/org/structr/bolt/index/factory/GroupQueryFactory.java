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

import org.structr.api.search.GroupQuery;
import org.structr.api.search.Occurrence;
import org.structr.api.search.QueryPredicate;
import org.structr.bolt.index.CypherQuery;

/**
 *
 */
public class GroupQueryFactory extends AbstractQueryFactory {

	@Override
	public boolean createQuery(final QueryFactory parent, final QueryPredicate predicate, final CypherQuery query, final boolean isFirst) {

		if (predicate instanceof GroupQuery) {

			if (predicate.getOccurrence().equals(Occurrence.FORBIDDEN)) {
				query.not();
			}

			//query.beginGroup();

			final GroupQuery group   = (GroupQuery)predicate;
			boolean first            = isFirst;

			for (final QueryPredicate attr : group.getQueryPredicates()) {

				if (parent.createQuery(parent, attr, query, first)) {

					first = false;
				}
			}

			//query.endGroup();

			return !first;
		}

		return false;
	}
}
