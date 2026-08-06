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
import org.structr.memory.index.MemoryQuery;
import org.structr.memory.index.predicate.Conjunction;
import org.structr.memory.index.predicate.StringContainsPredicate;
import org.structr.memory.index.predicate.ValuePredicate;

import java.util.Collection;

public class AnyQueryFactory extends AbstractQueryFactory<MemoryQuery> {

	public AnyQueryFactory(final AbstractIndex index) {

		super(index);
	}

	@Override
	public boolean createQuery(final QueryPredicate predicate, final MemoryQuery query, final boolean isFirst) {

		final Object rawValue  = predicate.getValue();
		final String name      = predicate.getName();
		final boolean exact    = predicate.isExactMatch();
		final boolean isString = String.class.equals(predicate.getType());

		if (rawValue instanceof Collection<?> collection && !collection.isEmpty()) {

			query.beginGroup(Conjunction.Or);

			for (final Object item : collection) {

				final Object readValue = getReadValue(item);
				if (!exact && isString && readValue instanceof String s) {

					query.addPredicate(new StringContainsPredicate(name, s, true));

				} else {

					query.addPredicate(new ValuePredicate(name, readValue));
				}
			}

			query.endGroup();
		}

		return true;
	}
}
