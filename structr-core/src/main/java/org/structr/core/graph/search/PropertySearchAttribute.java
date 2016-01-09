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
package org.structr.core.graph.search;

import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.structr.core.GraphObject;
import org.structr.core.property.AbstractPrimitiveProperty;
import org.structr.core.property.PropertyKey;

/**
 * Represents an attribute for textual search, used in {@link SearchNodeCommand}.
 *
 *
 */
public class PropertySearchAttribute<T> extends SearchAttribute<T> {

	private static final Logger logger = Logger.getLogger(PropertySearchAttribute.class.getName());

	private boolean isExactMatch = false;

	public PropertySearchAttribute(final PropertyKey<T> key, final T value, final Occur occur, final boolean isExactMatch) {

		super(occur, key, value);

		this.isExactMatch = isExactMatch;
	}

	@Override
	public String toString() {
		return "PropertySearchAttribute(" + super.toString() + ")";
	}

	@Override
	public Query getQuery() {

		if (isExactMatch) {

			String value = getStringValue();

			if (StringUtils.isEmpty(value)) {

				value = getValueForEmptyField();
			}

			if (value.matches("[\\s]+")) {

				value = "\"" + value + "\"";
			}

			return new TermQuery(new Term(getKey().dbName(), value));

		} else {

			String value = getInexactValue();

			// If there are double quotes around the search value treat as phrase
			if (value.startsWith("\"") && value.endsWith("\"")) {

				value = StringUtils.stripStart(StringUtils.stripEnd(value, "\""), "\"");

				// Split string into words
				String[] words = StringUtils.split(value, " ");


				PhraseQuery query = new PhraseQuery();

				for (String word : words) {

					query.add(new Term(getKey().dbName(), word));

				}

				return query;

			}

			BooleanQuery query = new BooleanQuery();

			// Split string into words
			String[] words = StringUtils.split(value, " ");

			for (String word : words) {

				query.add(new WildcardQuery(new Term(getKey().dbName(), word)), Occur.SHOULD);

				word = "*" + SearchCommand.escapeForLucene(word) + "*";

				query.add(new WildcardQuery(new Term(getKey().dbName(), word)), Occur.SHOULD);
			}

			return query;
		}
	}

	@Override
	public boolean isExactMatch() {
		return isExactMatch;
	}

	@Override
	public void setExactMatch(final boolean exact) {
		this.isExactMatch = exact;
	}

	@Override
	public String getStringValue() {

		Object value = getValue();
		if (value != null) {
			return value.toString();
		}

		return null;
	}

	@Override
	public String getInexactValue() {

		String stringValue = getStringValue();
		if (stringValue != null) {

			return stringValue.toLowerCase();
		}

		return null;
	}

	@Override
	public boolean includeInResult(final GraphObject entity) {

		T nodeValue          = entity.getProperty(getKey());
		Occur occur          = getOccur();
		T searchValue        = getValue();

		if (occur.equals(Occur.MUST_NOT)) {

			if ((nodeValue != null) && compare(nodeValue, searchValue) == 0) {

				// don't add, do not check other results
				return false;
			}

		} else {

			if (nodeValue != null) {

				if (!isExactMatch) {

					if (nodeValue instanceof String && searchValue instanceof String) {

						String n = (String) nodeValue;
						String s = (String) searchValue;

						return StringUtils.containsIgnoreCase(n, s);

					}

				}

				if (compare(nodeValue, searchValue) != 0) {
					return false;
				}

			} else {

				if (searchValue != null && StringUtils.isNotBlank(searchValue.toString())) {
					return false;
				}
			}
		}

		return true;
	}

	private int compare(T nodeValue, T searchValue) {

		if (nodeValue instanceof Comparable && searchValue instanceof Comparable) {

			Comparable n = (Comparable)nodeValue;
			Comparable s = (Comparable)searchValue;

			return n.compareTo(s);
		}

		return 0;
	}

	@Override
	public String getValueForEmptyField() {
		return AbstractPrimitiveProperty.STRING_EMPTY_FIELD_VALUE;
	}
}
