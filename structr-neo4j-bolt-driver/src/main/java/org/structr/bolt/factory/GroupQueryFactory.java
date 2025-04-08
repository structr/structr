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
package org.structr.bolt.factory;

import org.structr.api.index.AbstractIndex;
import org.structr.api.index.AbstractQueryFactory;
import org.structr.api.search.GroupQuery;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.TypeQuery;
import org.structr.bolt.AdvancedCypherQuery;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class GroupQueryFactory extends AbstractQueryFactory<AdvancedCypherQuery> {

	public GroupQueryFactory(final AbstractIndex index) {
		super(index);
	}

	@Override
	public boolean createQuery(final QueryPredicate predicate, final AdvancedCypherQuery query, final boolean isFirst) {

		if (predicate instanceof GroupQuery) {

			final GroupQuery group   = (GroupQuery)predicate;

			// Filter type predicates since they require special handling
			final List<QueryPredicate> predicateList               = group.getQueryPredicates();
			final List<QueryPredicate> typePredicates              = predicateList.stream().filter((p) -> p instanceof TypeQuery).collect(Collectors.toList());
			final List<QueryPredicate> attributeAndGroupPredicates = predicateList.stream().filter((p) -> !(p instanceof TypeQuery)).collect(Collectors.toList());

			// Apply all type queries first as they affect as different part of the query expression
			for (final QueryPredicate p : typePredicates) {

				index.createQuery(p, query, isFirst);
			}

			// Apply any group and attribute predicates, if existent
			if (!attributeAndGroupPredicates.isEmpty()) {

				// Check if any child group contains elements
				boolean allChildrenAreGroups = true;
				boolean nonEmptyGroup        = false;

				for (QueryPredicate p : attributeAndGroupPredicates) {

					if (p instanceof GroupQuery) {

						final List<QueryPredicate> containedPredicates = ((GroupQuery)p).getQueryPredicates();
						if (!containedPredicates.isEmpty()) {

							nonEmptyGroup = true;
						}
					} else {
						allChildrenAreGroups = false;
					}
				}

				if (!(allChildrenAreGroups && !nonEmptyGroup)) {
					checkOperation(query, predicate.getOperation(), isFirst);
				}

				if (attributeAndGroupPredicates.size() > 1 && !(allChildrenAreGroups && !nonEmptyGroup)) {
					query.beginGroup();
				}

				boolean firstWithinGroup = true;

				Iterator<QueryPredicate> it = attributeAndGroupPredicates.iterator();

				while (it.hasNext()) {

					if (index.createQuery(it.next(), query, firstWithinGroup)) {

						firstWithinGroup = false;
					}
				}

				if (attributeAndGroupPredicates.size() > 1 && !(allChildrenAreGroups && !nonEmptyGroup)) {
					query.endGroup();
				}

				if (allChildrenAreGroups && !nonEmptyGroup) {
					return false;
				} else {
					return true;
				}

			}

			return false;
		}

		return false;
	}
}
