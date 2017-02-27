/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.neo4j.index.lucene.factory;

import org.structr.api.search.QueryPredicate;
import org.structr.api.search.TypeConverter;
import org.structr.neo4j.index.lucene.LuceneIndexWrapper;

/**
 *
 */
public abstract class AbstractQueryFactory<T> implements QueryFactory<T> {

	protected Object getReadValue(final QueryPredicate predicate) {

		final TypeConverter converter = getTypeConverter(predicate.getType());
		return converter.getReadValue(predicate.getValue());
	}

	protected Object getWriteValue(final QueryPredicate predicate) {

		final TypeConverter converter = getTypeConverter(predicate.getType());
		return converter.getWriteValue(predicate.getValue());
	}

	protected Object getInexactValue(final QueryPredicate predicate) {

		final TypeConverter converter = getTypeConverter(predicate.getType());
		return converter.getInexactValue(predicate.getValue());
	}

	protected String escape(String input) {

		StringBuilder output = new StringBuilder();

		for (int i = 0; i < input.length(); i++) {

			char c = input.charAt(i);

			if (Character.isWhitespace(c)) {

				output.append('\\');
			}

			output.append(c);
		}

		return output.toString();
	}

	// ----- private methods -----
	private TypeConverter getTypeConverter(final Class type) {

		final TypeConverter conv = LuceneIndexWrapper.CONVERTERS.get(type);
		if (conv != null) {

			return conv;
		}

		// default type: String
		return LuceneIndexWrapper.DEFAULT_CONVERTER;
	}
}
