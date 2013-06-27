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
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.Version;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.PropertyKey;

/**
 * Represents an attribute for textual search, used in {@link SearchNodeCommand}.
 *
 * @author Axel Morgner
 */
public class StringSearchAttribute extends SearchAttribute {

	private boolean isExactMatch = false;
	
	public StringSearchAttribute(final PropertyKey key, final String value, final Occur occur, final boolean isExactMatch) {
		
		super(occur, new NodeAttribute(key, value));
		
		this.isExactMatch = isExactMatch;
	}
	
	@Override
	public Query getQuery() {

		if (isExactMatch) {
			
			String value = getStringValue();

			if (StringUtils.isEmpty(value)) {

				value = SearchNodeCommand.IMPROBABLE_SEARCH_VALUE;
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

				word = "*" + Search.escapeForLucene(word.toLowerCase()) + "*";

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
	public boolean forcesExclusivelyOptionalQueries() {
		
		// wildcard queries seem to require all-optional
		// boolean queries, i.e. no Occur.MUST..
		return !isExactMatch;
	}
	
	public static final void main(String[] args) {
		
		QueryParser p = new QueryParser(Version.LUCENE_36,WILDCARD, new KeywordAnalyzer());
		
		try {
			
			p.setAllowLeadingWildcard(true);
			
			Query q = p.parse("name:*B*");
			
			System.out.println(q.getClass().getSimpleName());
			
		} catch (Throwable t) {
			
			t.printStackTrace();
		}
		
	}
}
