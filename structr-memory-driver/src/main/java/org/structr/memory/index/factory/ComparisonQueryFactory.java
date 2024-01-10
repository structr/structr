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
import org.structr.memory.index.predicate.NotPredicate;
import org.structr.memory.index.predicate.NullPredicate;
import org.structr.memory.index.predicate.RangePredicate;
import org.structr.memory.index.predicate.ValuePredicate;

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
					query.addPredicate(new RangePredicate<>(name, value, null, predicate.getType()).setStartInclusive(false));
					break;

				case greaterOrEqual:
					query.addPredicate(new RangePredicate<>(name, value, null, predicate.getType()));
					break;

				case less:
					query.addPredicate(new RangePredicate<>(name, null, value, predicate.getType()).setEndInclusive(false));
					break;

				case lessOrEqual:
					query.addPredicate(new RangePredicate<>(name, null, value, predicate.getType()));
					break;

				case isNull:
					query.addPredicate(new NullPredicate<>(name));
					return true;

				case isNotNull:
					query.addPredicate(new NotPredicate(new NullPredicate<>(name)));
					return true;

				case startsWith:
					//operationString = "STARTS WITH";
					break;

				case endsWith:
					//operationString = "ENDS WITH";
					break;

				case contains:
					//operationString = "CONTAINS";
					break;

				case caseInsensitiveStartsWith:
					//operationString = "STARTS WITH";
					//query.addSimpleParameter(name, operationString, value.toString().toLowerCase(), true, true);
					return true;

				case caseInsensitiveEndsWith:
					//operationString = "ENDS WITH";
					//query.addSimpleParameter(name, operationString, value.toString().toLowerCase(), true, true);
					return true;

				case caseInsensitiveContains:
					//operationString = "CONTAINS";
					//query.addSimpleParameter(name, operationString, value.toString().toLowerCase(), true, true);
					return true;
			}
		}

		return true;
	}
}
