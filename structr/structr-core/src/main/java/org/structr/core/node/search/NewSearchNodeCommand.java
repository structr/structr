/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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


import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import org.structr.core.entity.AbstractNode;
import org.structr.core.node.NodeServiceCommand;
import org.structr.core.node.NodeFactory;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.search.Query;
import org.neo4j.index.lucene.QueryContext;
import org.structr.common.error.FrameworkException;

//~--- classes ----------------------------------------------------------------

/**
 * <b>Search for nodes by attributes</b>
 * <p>
 * The execute method takes four parameters:
 * <p>
 * <ol>
 * <li>{@see AbstractNode} top node: search only below this node
 *     <p>if null, search everywhere (top node = root node)
 * <li>boolean include deleted: if true, return deleted nodes as well
 * <li>boolean public only: if true, return only public nodes
 * <li>List<{@see TextualSearchAttribute}> search attributes: key/value pairs with search operator
 *    <p>if no TextualSearchAttribute is given, return any node matching the other
 *       search criteria
 * </ol>
 *
 * @author amorgner
 */
public class NewSearchNodeCommand extends NodeServiceCommand {

	private static final Logger logger            = Logger.getLogger(NewSearchNodeCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		if(parameters != null && parameters.length > 0) {

			Query query            = (Query)parameters[0];
			boolean strict         = false;
			int desiredResultCount = 0;

			if(parameters.length > 1 && parameters[1] instanceof Integer) {
				desiredResultCount = (Integer)parameters[1];
			}

			if(parameters.length > 1 && parameters[1] instanceof Boolean) {
				strict = (Boolean)parameters[1];
			}

			if(parameters.length > 2 && parameters[2] instanceof Boolean) {
				strict = (Boolean)parameters[2];
			}

			return search(query, desiredResultCount, strict);

		}

		return Collections.emptyList();
	}

	private List<AbstractNode> search(final Query query, final int desiredResultCount, final boolean strict) throws FrameworkException {

		GraphDatabaseService graphDb   = (GraphDatabaseService) arguments.get("graphDb");
		if (graphDb != null) {

			Index<Node> fulltextIndex      = (Index<Node>) arguments.get("index");
			Index<Node> exactIndex         = (Index<Node>) arguments.get("exactIndex");
			NodeFactory nodeFactory = (NodeFactory) arguments.get("nodeFactory");
			List<AbstractNode> finalResult = new LinkedList<AbstractNode>();

			// configure query context
			QueryContext queryContext = new QueryContext(query);
			queryContext.sort(AbstractNode.Key.name.name());

			// fixed result count
			if(desiredResultCount > 0) {
				queryContext.top(desiredResultCount);
			}

			logger.log(Level.INFO, "Querying {0} index with {1}", new Object[] { strict ? "exact" : "fulltext", query.toString() } );

			// query index
			IndexHits<Node> hits = strict ? exactIndex.query(queryContext) : fulltextIndex.query(queryContext);
			for(Node node : hits) {
				finalResult.add(nodeFactory.createNode(securityContext, node));
			}

			logger.log(Level.INFO, "{0} results", finalResult.size());

			return finalResult;
		}

		return Collections.emptyList();
	}
}
