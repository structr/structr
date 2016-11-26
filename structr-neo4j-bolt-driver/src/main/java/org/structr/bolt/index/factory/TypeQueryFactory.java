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

import org.structr.api.search.QueryPredicate;
import org.structr.api.search.TypeQuery;
import org.structr.bolt.index.CypherQuery;

public class TypeQueryFactory extends AbstractQueryFactory {

	@Override
	public boolean createQuery(final QueryFactory parent, final QueryPredicate predicate, final CypherQuery query, final boolean isFirst) {

		final TypeQuery typeQuery = (TypeQuery)predicate;
		final Class sourceType    = typeQuery.getSourceType();
		final Class targetType    = typeQuery.getTargetType();
		final Object mainType     = typeQuery.getValue();

		if (mainType != null && mainType instanceof String) {

			query.typeLabel((String)mainType);
		}

		if (sourceType != null && targetType != null) {

			// relationship type, include source
			// and target type labels
			query.setSourceType(sourceType.getSimpleName());
			query.setTargetType(targetType.getSimpleName());
		}

		// setting the label does not result in a modified WHERE clause
		return false;
	}
}
