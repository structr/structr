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
import org.structr.bolt.AdvancedCypherQuery;

import java.util.HashMap;
import java.util.Map;

public class KeywordQueryFactory extends AbstractQueryFactory<AdvancedCypherQuery> {

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

	public KeywordQueryFactory(final AbstractIndex index) {
		super(index);
	}

	@Override
	public boolean createQuery(final QueryPredicate predicate, final AdvancedCypherQuery query, final boolean isFirst) {

		final boolean isString = String.class.equals(predicate.getType());
		final Object value     = getReadValue(predicate.getValue());
		final String name      = predicate.getName();

		//checkOperation(query, predicate.getOperation(), isFirst);

		// add label of declaring class for the given property name
		// to select the correct index
		final String label = predicate.getLabel();
		if (label != null) {
			query.indexLabel(label);
		}

		// only String properties can be used for inexact search
		if (predicate.isExactMatch() || !isString) {

			if (isString && value == null) {

				// special handling for string attributes
				// (empty string is equal to null)
				query.beginGroup();
				query.addSimpleParameter(name, "is", null);
				query.or();
				query.addSimpleParameter(name, "=", "");
				query.endGroup();

			} else {

				query.addSimpleParameter(name, value != null ? "=" : "is", value);
			}

		} else {

			if (value != null && isString) {

				query.addSimpleParameter(name, "CONTAINS", value, true, true);            // works and takes half the time

			} else if ("".equals(predicate.getValue()) && isString) {

				query.addSimpleParameter(name, "CONTAINS", "", true, true);               // works and takes half the time

			} else {

				query.beginGroup();
				query.addSimpleParameter(name, "is", null);
				query.or();
				query.addSimpleParameter(name, "=", "");
				query.endGroup();
			}
		}

		return true;
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
