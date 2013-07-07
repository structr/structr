/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.graph.search;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.PropertyKey;

/**
 * Represents an attribute for textual search, used in {@link SearchNodeCommand}.
 *
 * @author Axel Morgner
 */
public class PropertySearchAttribute<T> extends SearchAttribute<T> {

	private boolean isExactMatch = false;
	
	public PropertySearchAttribute(final PropertyKey<T> key, final T value, final Occur occur, final boolean isExactMatch) {
		
		super(occur, new NodeAttribute<T>(key, value));
		
		this.isExactMatch = isExactMatch;
	}
	
	@Override
	public Query getQuery() {

		if (isExactMatch) {
			
			String value = getStringValue();

			if (StringUtils.isEmpty(value)) {

				value = SearchNodeCommand.EMPTY_FIELD_VALUE;
			}

			if (value.matches("[\\s]+")) {

				value = "\"" + value + "\"";
			}
			
			return new TermQuery(new Term(getKey().dbName(), value));
			
		} else {
		
			BooleanQuery query = new BooleanQuery();
			
			// Split string into words
			String[] words = StringUtils.split(getStringValue(), " ");
			for (String word : words) {

				query.add(new WildcardQuery(new Term(getKey().dbName(), word)), Occur.SHOULD);

				word = "*" + Search.escapeForLucene(word) + "*";

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
	public String getStringValue() {
		
		Object value = getValue();
		if (value != null) {
			return value.toString();
		}
		
		return null;
	}

	@Override
	public boolean includeInResult(GraphObject entity) {
		
		Occur occur   = getOccur();
		T searchValue = getValue();
		T nodeValue   = entity.getProperty(getKey());
		
		if (occur.equals(Occur.MUST_NOT)) {

			if ((nodeValue != null) && !(nodeValue.equals(searchValue))) {

				// don't add, do not check other results
				return false;
			}

		} else {

			if (nodeValue != null) {

				if (!nodeValue.equals(searchValue)) {
					return false;
				}

			} else {

				if (searchValue != null) {
					return false;
				}
			}
		}
		
		return true;
	}
}
