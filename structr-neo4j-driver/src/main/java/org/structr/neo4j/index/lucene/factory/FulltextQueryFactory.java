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

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.structr.api.search.QueryPredicate;

/**
 *
 */
public class FulltextQueryFactory extends AbstractQueryFactory<Query> {

	@Override
	public Query getQuery(final QueryFactory<Query> parent, final QueryPredicate predicate) {

		final boolean isExactMatch = predicate.isExactMatch();
		if (isExactMatch) {

			String value = getReadValue(predicate).toString();

			if (StringUtils.isEmpty(value)) {

				value = getReadValue(null).toString();
			}

			if (value.matches("[\\s]+")) {

				value = "\"" + value + "\"";
			}

			return new TermQuery(new Term(predicate.getName(), value));

		} else {

			String value = getInexactValue(predicate).toString();

			// If there are double quotes around the search value treat as phrase
			if (value.startsWith("\"") && value.endsWith("\"")) {

				value = StringUtils.stripStart(StringUtils.stripEnd(value, "\""), "\"");

				// Split string into words
				String[] words = StringUtils.split(value, " ");


				PhraseQuery query = new PhraseQuery();

				for (String word : words) {

					query.add(new Term(predicate.getName(), word));

				}

				return query;

			}

			BooleanQuery query = new BooleanQuery();

			// Split string into words
			String[] words = StringUtils.split(value, " ");

			for (String word : words) {

				query.add(new WildcardQuery(new Term(predicate.getName(), word)), Occur.SHOULD);
				query.add(new WildcardQuery(new Term(predicate.getName(), "*" + escape(word) + "*")), Occur.SHOULD);
			}

			return query;
		}
	}

}
