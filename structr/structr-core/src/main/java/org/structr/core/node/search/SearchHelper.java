/*
 *  Copyright (C) 2011 Axel Morgner
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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.module.GetEntitiesCommand;

/**
 *
 * @author Christian Morgner
 */
public class SearchHelper {

	private static final Logger logger = Logger.getLogger(SearchHelper.class.getName());

	public static Query propertyValue(String key, String value, boolean strict) {
		if(strict) {
			return new TermQuery(new Term(key, value));
		} else {
			BooleanQuery query = new BooleanQuery();
			query.add(new TermQuery(new Term(key, value)), Occur.SHOULD);
			query.add(new WildcardQuery(new Term(key, "\"" + value + "\"")), Occur.SHOULD);
			query.add(new WildcardQuery(new Term(key, value + "*")), Occur.SHOULD);

			return(query);
		}
	}

	public static Query type(String type) {
		return new TermQuery(new Term(AbstractNode.Key.type.name(), StringUtils.capitalize(type)));
	}

	public static Query typeAndSubtypes(String type) {

		try {
			Map<String, Class> entities = (Map) Services.command(SecurityContext.getSuperUserInstance(), GetEntitiesCommand.class).execute();
			Class parentClass           = entities.get(StringUtils.capitalize(type));
			BooleanQuery query          = new BooleanQuery();

			// no parent class found, return unmodified type query
			if (parentClass == null) {
				return type(type);
			}

			for (Map.Entry<String, Class> entity : entities.entrySet()) {

				Class entityClass = entity.getValue();
				if (parentClass.isAssignableFrom(entityClass)) {
					query.add(type(entity.getKey()), Occur.SHOULD);
				}

			}

			return query;

		} catch(FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to create type and subtype query", fex);
		}

		return type(type);
	}

	public static Query and(Query query1, Query query2) {

		BooleanQuery query = new BooleanQuery();
		query.add(query1, Occur.MUST);
		query.add(query2, Occur.MUST);

		return query;
	}

	public static Query or(Query query1, Query query2) {

		BooleanQuery query = new BooleanQuery();
		query.add(query1, Occur.SHOULD);
		query.add(query2, Occur.SHOULD);

		return query;
	}

}
