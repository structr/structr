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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.index.AbstractIndex;
import org.structr.api.index.AbstractQueryFactory;
import org.structr.api.search.GraphQuery;
import org.structr.api.search.QueryPredicate;
import org.structr.memory.index.MemoryQuery;
import org.structr.memory.index.predicate.GraphPredicate;
import org.structr.memory.index.predicate.NoRelationshipPredicate;

import java.util.Set;

public class GraphQueryFactory extends AbstractQueryFactory<MemoryQuery> {

	public GraphQueryFactory(final AbstractIndex index) {
		super(index);
	}

	@Override
	public boolean createQuery(final QueryPredicate predicate, final MemoryQuery query, final boolean isFirst) {

		final GraphQuery graphQuery = (GraphQuery)predicate;
		final Set<Object> values    = graphQuery.getValues();

		if (values.isEmpty() || onlyEmptyValues(values)) {

			checkOccur(query, predicate.getOccurrence(), isFirst);

			query.addPredicate(new NoRelationshipPredicate<>(predicate.getName(), graphQuery));

		} else {

			checkOccur(query, predicate.getOccurrence(), isFirst);

			query.addPredicate(new GraphPredicate(graphQuery));
		}

		return true;
	}

	private boolean onlyEmptyValues(final Set<Object> values) {

		boolean onlyEmpty = true;

		for (final Object o : values) {

			onlyEmpty &= (o == null || StringUtils.isBlank(o.toString()));
		}

		return onlyEmpty;
	}
}
