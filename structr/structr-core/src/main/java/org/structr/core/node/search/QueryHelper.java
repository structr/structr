/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.core.node.search;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.structr.common.PropertyKey;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class QueryHelper {

	public static Query exactType(Class type) {
		return new TermQuery(new Term(AbstractNode.Key.type.name(), Search.exactMatch(type.getSimpleName())));
	}

	public static Query exactPropertyValue(PropertyKey propertyKey, String value) {
		return new TermQuery(new Term(propertyKey.name(), Search.exactMatch(value)));
	}

	public static Query and(Query query1, Query query2) {

		BooleanQuery query = new BooleanQuery();

		query.add(new BooleanClause(query1, BooleanClause.Occur.MUST));
		query.add(new BooleanClause(query2, BooleanClause.Occur.MUST));

		return query;
	}

	public static Query or(Query query1, Query query2) {

		BooleanQuery query = new BooleanQuery();

		query.add(new BooleanClause(query1, BooleanClause.Occur.SHOULD));
		query.add(new BooleanClause(query2, BooleanClause.Occur.SHOULD));

		return query;
	}
}
