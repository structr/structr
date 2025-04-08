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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.index.AbstractIndex;
import org.structr.api.index.AbstractQueryFactory;
import org.structr.api.search.GraphQuery;
import org.structr.api.search.Operation;
import org.structr.api.search.QueryPredicate;
import org.structr.bolt.AdvancedCypherQuery;
import org.structr.bolt.BoltIdentity;
import org.structr.bolt.GraphQueryPart;

import java.util.Set;

public class GraphQueryFactory extends AbstractQueryFactory<AdvancedCypherQuery> {

	public GraphQueryFactory(final AbstractIndex index) {
		super(index);
	}

	@Override
	public boolean createQuery(final QueryPredicate predicate, final AdvancedCypherQuery query, final boolean isFirst) {

		final GraphQuery graphQuery = (GraphQuery)predicate;
		final GraphQueryPart part   = new GraphQueryPart(graphQuery);
		final Set<Object> values    = graphQuery.getValues();

		if (values.isEmpty() || onlyEmptyValues(values)) {

			checkOperation(query, predicate.getOperation(), isFirst);

			query.addNullObjectParameter(graphQuery.getDirection(), graphQuery.getRelationship());

		} else {

			checkOperation(query, predicate.getOperation(), isFirst);

			query.addGraphQueryPart(part);

			final String name = graphQuery.getNotionPropertyName();
			boolean first     = true;

			for (final Object value : graphQuery.getValues()) {

				checkOperation(query, Operation.OR, first);

				if (predicate.isExactMatch()) {

					final BoltIdentity boltIdentity = (BoltIdentity)graphQuery.getIdentity();
					if (boltIdentity != null) {

						final long id = boltIdentity.getId();

						query.addSimpleParameter("ID(" + part.getIdentifier() + ")", "=", id, false, false);

					} else {

						query.addSimpleParameter(part.getIdentifier(), name, "=", value, true, false);
					}

				} else {

					query.addSimpleParameter(part.getIdentifier(), name, "CONTAINS", value, true, true);
				}

				first = false;
			}
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
