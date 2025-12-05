/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.RangeQuery;
import org.structr.bolt.AdvancedCypherQuery;

/**
 *
 */
public class RangeQueryFactory extends AbstractQueryFactory<AdvancedCypherQuery> {

	public RangeQueryFactory(final AbstractIndex index) {
		super(index);
	}

	@Override
	public boolean createQuery(final QueryPredicate predicate, final AdvancedCypherQuery query, final boolean isFirst) {

		if (predicate instanceof RangeQuery) {

			//checkOperation(query, predicate.getOperation(), isFirst);

			// add label of declaring class for the given property name
			// to select the correct index
			final String label = predicate.getLabel();
			if (label != null) {
				query.indexLabel(label);
			}

			final RangeQuery rangeQuery = (RangeQuery)predicate;
			final Object rangeStart     = getReadValue(rangeQuery.getRangeStart());
			final Object rangeEnd       = getReadValue(rangeQuery.getRangeEnd());
			final String name           = predicate.getName();

			if (rangeStart == null && rangeEnd == null) {
				return false;
			}

			// range start is not set => less than
			if (rangeStart == null && rangeEnd != null) {

				query.addSimpleParameter(name, getLessThanOperator(rangeQuery.getIncludeEnd()), rangeEnd);
				return true;
			}

			// range end is not set => greater than
			if (rangeStart != null && rangeEnd == null) {

				query.addSimpleParameter(name, getGreaterThanOperator(rangeQuery.getIncludeStart()), rangeStart);
				return true;
			}

			// both are set => range
			query.addParameters(name, getGreaterThanOperator(rangeQuery.getIncludeStart()), rangeStart, getLessThanOperator(rangeQuery.getIncludeEnd()), rangeEnd);

			return true;
		}

		return false;
	}

	private String getLessThanOperator(final boolean includeBounds) {

		if (includeBounds) {

			return "<=";
		}

		return "<";
	}

	private String getGreaterThanOperator(final boolean includeBounds) {

		if (includeBounds) {

			return ">=";
		}

		return ">";
	}
}
