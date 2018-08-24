/**
 * Copyright (C) 2010-2018 Structr GmbH
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
import org.structr.api.search.TypeQuery;
import org.structr.bolt.index.AdvancedCypherQuery;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 *
 */
public class GroupQueryFactory extends AbstractQueryFactory {

	@Override
	public boolean createQuery(final QueryFactory parent, final QueryPredicate predicate, final AdvancedCypherQuery query, final boolean isFirst) {

		if (predicate instanceof GroupQuery) {

			final GroupQuery group   = (GroupQuery)predicate;
			boolean first            = isFirst;

			// Filter type predicates since they require special handling
			List<QueryPredicate> predicateList = group.getQueryPredicates();

			List<QueryPredicate> typePredicates = predicateList.stream().filter((p) -> {
				return p instanceof TypeQuery;
			}).collect(Collectors.toList());

			List<QueryPredicate> attributeAndGroupPredicates = predicateList.stream().filter((p) -> {
				return !(p instanceof TypeQuery);
			}).collect(Collectors.toList());

			// Apply all type queries first as they affect as different part of the query expression
			for (final QueryPredicate p : typePredicates) {
				parent.createQuery(parent, p, query, first);
			}

			// Apply any group and attribute predicates, if existent
			if (attributeAndGroupPredicates.size() > 0) {

				checkOccur(query, predicate.getOccurrence(), first);

				if (attributeAndGroupPredicates.size() > 1) {
					query.beginGroup();
				}

				boolean firstWithinGroup = true;

				Iterator<QueryPredicate> it = attributeAndGroupPredicates.iterator();

				while (it.hasNext()) {

					if (parent.createQuery(parent, it.next(), query, firstWithinGroup)) {

						firstWithinGroup = false;
					}
				}

				if (attributeAndGroupPredicates.size() > 1) {
					query.endGroup();
				}
			}

			return first;
		}

		return false;
	}
}
