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

import org.structr.api.index.AbstractIndex;
import org.structr.api.index.AbstractQueryFactory;
import org.structr.api.search.ComparisonQuery;
import org.structr.api.search.QueryPredicate;
import org.structr.memory.index.MemoryQuery;
import org.structr.memory.index.predicate.*;

public class ComparisonQueryFactory extends AbstractQueryFactory<MemoryQuery> {

	public ComparisonQueryFactory(final AbstractIndex index) {
		super(index);
	}

	@Override
	public boolean createQuery(final QueryPredicate predicate, final MemoryQuery query, final boolean isFirst) {

		if (predicate instanceof ComparisonQuery) {

			checkOccur(query, predicate.getOccurrence(), isFirst);

			final ComparisonQuery comparisonQuery     = (ComparisonQuery)predicate;
			final Comparable value                    = (Comparable)getReadValue(comparisonQuery.getSearchValue());
			final ComparisonQuery.Operation operation = comparisonQuery.getOperation();
			final String name                         = predicate.getName();
			final Class type                          = null;

			if (value == null && operation == null) {
				return false;
			}

			switch (operation) {
				case equal:
					query.addPredicate(new ValuePredicate(name, value));
					return true;

				case notEqual:
					query.addPredicate(new NotPredicate(new ValuePredicate(name, value)));
					break;

				case greater:
					query.addPredicate(new RangePredicate<>(name, value, null, type).setStartInclusive(false));
					break;

				case greaterOrEqual:
					query.addPredicate(new RangePredicate<>(name, value, null, type));
					break;

				case less:
					query.addPredicate(new RangePredicate<>(name, null, value, type).setEndInclusive(false));
					break;

				case lessOrEqual:
					query.addPredicate(new RangePredicate<>(name, null, value, type));
					break;

				case isNull:
					query.addPredicate(new NullPredicate<>(name));
					return true;

				case isNotNull:
					query.addPredicate(new NotPredicate(new NullPredicate<>(name)));
					return true;

				case startsWith:
					query.addPredicate(new StartsOrEndsWithPredicate(name, value.toString(), true, false));
					break;

				case endsWith:
					query.addPredicate(new StartsOrEndsWithPredicate(name, value.toString(), false, false));
					break;

				case contains:
					query.addPredicate(new StringContainsPredicate(name, value.toString(), false));
					break;

				case caseInsensitiveStartsWith:
					query.addPredicate(new StartsOrEndsWithPredicate(name, value.toString().toLowerCase(), true, true));
					break;

				case caseInsensitiveEndsWith:
					query.addPredicate(new StartsOrEndsWithPredicate(name, value.toString().toLowerCase(), false, true));
					break;

				case caseInsensitiveContains:
					query.addPredicate(new StringContainsPredicate(name, value.toString().toLowerCase(), true));
					break;

				case matches:
					query.addPredicate(new RegexPredicate(name, value.toString()));
					break;
			}
		}

		return true;
	}
}
