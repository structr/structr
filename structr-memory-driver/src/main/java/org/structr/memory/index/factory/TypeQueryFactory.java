/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.structr.api.index.AbstractIndex;
import org.structr.api.index.AbstractQueryFactory;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.TypeQuery;
import org.structr.memory.index.MemoryQuery;
import org.structr.memory.index.predicate.LabelPredicate;

public class TypeQueryFactory extends AbstractQueryFactory<MemoryQuery> {

	public TypeQueryFactory(final AbstractIndex index) {
		super(index);
	}

	@Override
	public boolean createQuery(final QueryPredicate predicate, final MemoryQuery query, final boolean isFirst) {

		final TypeQuery typeQuery = (TypeQuery)predicate;
		final String sourceType   = typeQuery.getSourceType();
		final String targetType   = typeQuery.getTargetType();
		final Object mainType     = typeQuery.getValue();
		final String label        = mainType.toString();

		if (sourceType != null && targetType != null) {

			// relationship type, include source and target type labels
			query.addPredicate(new LabelPredicate(label, sourceType, targetType));

		} else {

			query.addPredicate(new LabelPredicate(label));
		}

		// allow caching
		query.addTypeLabel(mainType.toString());

		return true;
	}
}
