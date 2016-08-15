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

import java.util.HashMap;
import java.util.Map;
import org.structr.api.search.QueryPredicate;
import org.structr.bolt.index.CypherQuery;

public class KeywordQueryFactory extends AbstractQueryFactory {

	protected static final Map<Character, String> SPECIAL_CHARS = new HashMap<>();

	static {

		SPECIAL_CHARS.put('+', "\\");
		SPECIAL_CHARS.put('-', "\\");
		SPECIAL_CHARS.put('*', ".");
		SPECIAL_CHARS.put('?', ".");
		SPECIAL_CHARS.put('~', "\\");
		SPECIAL_CHARS.put('.', "\\");
		SPECIAL_CHARS.put('(', "\\");
		SPECIAL_CHARS.put(')', "\\");
		SPECIAL_CHARS.put('{', "\\");
		SPECIAL_CHARS.put('}', "\\");
		SPECIAL_CHARS.put('[', "\\");
		SPECIAL_CHARS.put(']', "\\");
		SPECIAL_CHARS.put(':', "\\");
		SPECIAL_CHARS.put('^', "\\");
		SPECIAL_CHARS.put('&', "\\");
		SPECIAL_CHARS.put('|', "\\");
	}

	@Override
	public void createQuery(final QueryFactory parent, final QueryPredicate predicate, final CypherQuery query) {

		final Object value = getReadValue(predicate.getValue());
		final String name  = predicate.getName();

		if (predicate.isExactMatch()) {

			query.addSimpleParameter(name, value != null ? "=" : "is", value);

		} else {

			if (value != null) {

				query.addSimpleParameter(name, "=~", "(?i).*" + escape(value) + ".*");

			} else {

				query.addSimpleParameter(name, "is", null);
			}
		}
	}

	// ----- private methods -----
	protected String escape(final Object src) {

		final StringBuilder output = new StringBuilder();
		final String input         = src.toString();

		for (int i = 0; i < input.length(); i++) {

			final char c        = input.charAt(i);
			final String prefix = SPECIAL_CHARS.get(c);

			if (prefix != null) {
				output.append(prefix);
			}

			output.append(c);
		}

		return output.toString();
	}
}
