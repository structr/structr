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

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.structr.api.search.QueryPredicate;

/**
 *
 */
public class KeywordQueryFactory extends AbstractQueryFactory<Query> {

	@Override
	public Query getQuery(final QueryFactory<Query> parent, final QueryPredicate predicate) {

		final String value = getReadValue(predicate).toString();
		if (predicate.isExactMatch()) {

			return new TermQuery(new Term(predicate.getName(), value));

		} else {

			return new TermQuery(new Term(predicate.getName(), value.toLowerCase()));
		}
	}
}
