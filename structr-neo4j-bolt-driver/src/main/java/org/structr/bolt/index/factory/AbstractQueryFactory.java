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

import org.structr.api.search.Occurrence;
import static org.structr.api.search.Occurrence.FORBIDDEN;
import static org.structr.api.search.Occurrence.OPTIONAL;
import org.structr.api.search.TypeConverter;
import org.structr.bolt.index.AbstractCypherIndex;
import org.structr.bolt.index.AdvancedCypherQuery;

/**
 *
 */
public abstract class AbstractQueryFactory implements QueryFactory {

	protected Object getReadValue(final Object value) {

		if (value != null) {

			final TypeConverter converter = AbstractCypherIndex.CONVERTERS.get(value.getClass());
			if (converter != null) {

				return converter.getReadValue(value);
			}

			// use string value of enum types
			if (value.getClass().isEnum()) {
				return value.toString();
			}
		}

		return value;
	}

	protected Object getWriteValue(final Object value) {

		if (value != null) {

			final TypeConverter converter = AbstractCypherIndex.CONVERTERS.get(value.getClass());
			if (converter != null) {

				return converter.getWriteValue(value);
			}
		}

		return value;
	}

	// ----- protected methods -----
	protected void checkOccur(final AdvancedCypherQuery query, final Occurrence occ, final boolean first) {

		if (!first || occ.equals(Occurrence.FORBIDDEN)) {
			addOccur(query, occ, first);
		}
	}


	protected void addOccur(final AdvancedCypherQuery query, final Occurrence occ, final boolean first) {

		switch (occ) {

			case FORBIDDEN:
				if (first) {
					query.not();
				} else {
					query.andNot();
				}
				break;
			case OPTIONAL:
				query.or();
				break;

			default:
				query.and();
				break;
		}
	}
}
