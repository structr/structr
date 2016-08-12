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
	public void createQuery(final QueryFactory parent, final QueryPredicate predicate, final CypherQuery query) {

		if (predicate instanceof GroupQuery) {

			query.beginGroup();

			final GroupQuery group   = (GroupQuery)predicate;
			boolean first            = true;

			for (final QueryPredicate attr : group.getQueryPredicates()) {

				if (!first) {
					addOccur(query, attr.getOccurrence());
				}

				parent.createQuery(parent, attr, query);

				first = false;
			}

			query.endGroup();
		}
	}

	// ----- private methods -----
	private void addOccur(final CypherQuery query, final Occurrence occ) {

		switch (occ) {

			case FORBIDDEN:
				query.andNot();
				break;

			case OPTIONAL:
				query.or();
				break;

			default:
				query.and();
				break;
		}
	}
}
