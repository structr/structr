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


import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import org.structr.common.SecurityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.core.node.NodeServiceCommand;
import org.structr.core.node.NodeFactory;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.search.Query;
import org.structr.common.error.FrameworkException;

//~--- classes ----------------------------------------------------------------
/**
 * Executes a query on the graph database without respecting access restrictions.
 * 
 * @author Christian Morgner
 */
public class NodeQueryCommand extends NodeServiceCommand {

	private static final Logger logger            = Logger.getLogger(NodeQueryCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		if ((parameters == null) || (parameters.length < 2)) {

			logger.log(Level.WARNING, "Two parameters are required for query search.");
			return Collections.emptyList();
		}

		NodeIndex whichIndex = null;
		Query query = null;

		if(!(parameters[0] instanceof NodeIndex)) {
			logger.log(Level.WARNING, "NodeQueryCommand needs an index identifier as its first parameter.");
		} else {
			whichIndex = (NodeIndex)parameters[0];
		}

		if(!(parameters[1] instanceof Query)) {
			logger.log(Level.WARNING, "NodeQueryCommand needs a lucene query as its second parameter.");
		} else {
			query = (Query)parameters[1];
		}

		if(whichIndex == null || query == null) {
			return Collections.emptyList();
		}

		return search(securityContext, whichIndex, query);
	}

	/**
	 * Return a list of nodes which fit to all search criteria.
	 *
	 * @param securityContext       Search in this security context
	 * @param topNode               If set, return only search results below this top node (follows the HAS_CHILD relationship)
	 * @param includeDeleted        If true, include nodes marked as deleted or contained in a Trash node as well
	 * @param publicOnly            If true, don't include nodes which are not public
	 * @param searchAttrs           List with search attributes
	 * @return
	 */
	private List<AbstractNode> search(final SecurityContext securityContext, final NodeIndex whichIndex, final Query query) throws FrameworkException {

		GraphDatabaseService graphDb   = (GraphDatabaseService) arguments.get("graphDb");
		NodeFactory nodeFactory        = (NodeFactory) arguments.get("nodeFactory");

		if (graphDb != null) {

			Index<Node> index         = (Index<Node>)arguments.get(whichIndex.name());
			IndexHits hits            = index.query(query.toString());
			List<AbstractNode> nodes  = nodeFactory.createNodes(securityContext, hits);

			// close hits
			hits.close();

			Collections.sort(nodes);

			return nodes;
		}

		return Collections.emptyList();
	}

}