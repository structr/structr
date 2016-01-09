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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.NodeServiceCommand;

/**
 * A special command that uses a Lucene index lookup to count the number of
 * entities between a lower and an upper search term. This is used for range
 * queries.
 *
 *
 */
public class CountEntitiesCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(CountEntitiesCommand.class.getName());
	
	public int execute(Class entityType) throws FrameworkException {
		return execute(entityType, null, null);
	}
	
	public int execute(Class entityType, String lowerTerm, String upperTerm) throws FrameworkException {
		
		Index<Node> index = (Index<Node>)arguments.get(NodeService.NodeIndex.keyword.name());
		String type       = entityType.getSimpleName();
		int count = -1;

		if(type != null) {

			// create type query first
			Query typeQuery = new TermQuery(new Term("type", type));
			Query actualQuery = null;

			// if date terms are set, create date query
			if(lowerTerm != null && upperTerm != null) {

				// do range query, including start value, excluding end value!
				Query dateQuery = new TermRangeQuery("createdDate", lowerTerm, upperTerm, true, false);
				
				BooleanQuery booleanQuery = new BooleanQuery();
				booleanQuery.add(dateQuery, BooleanClause.Occur.MUST);
				booleanQuery.add(typeQuery, BooleanClause.Occur.MUST);
				
				actualQuery = booleanQuery;
				
			} else {
				
				actualQuery  = typeQuery;
				
			}

			long start = System.currentTimeMillis();

			IndexHits hits = index.query(actualQuery);
			for (Object hit : hits) {
				count++;
			}
			
			// this is not accurate
			// count = hits.size();

			long end = System.currentTimeMillis();

			logger.log(Level.FINE, "Counted {0} entities in {1} ms.", new Object[] { count, (end-start) } );

			hits.close();
		}
		
		return count;
	}
}
