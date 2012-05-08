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



package org.structr.core.node;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.kernel.Traversal;

import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

//import org.structr.common.xpath.JXPathFinder;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Look up a node in database and returns the result.
 *
 * @author amorgner
 * @author cmorgner
 */
public class FindNodeCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(FindNodeCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		NodeFactory nodeFactory      = (NodeFactory) arguments.get("nodeFactory");

		if (graphDb != null) {

			switch (parameters.length) {

				case 0 :
					throw new UnsupportedArgumentError("No arguments supplied");

				case 1 :
					return (handleSingleArgument(graphDb, nodeFactory, parameters[0]));

				default :
					throw new UnsupportedArgumentError("Too many arguments supplied");

			}

		}

		return (null);
	}

	// <editor-fold defaultstate="collapsed" desc="private methods">
	private Object handleSingleArgument(GraphDatabaseService graphDb, NodeFactory nodeFactory, Object argument) throws FrameworkException {

		Object result = null;

		if (argument instanceof Node) {

			result = nodeFactory.createNode(securityContext, (Node) argument);

		} else if (argument instanceof Long) {

			// single long value: find node by id
			long id   = ((Long) argument).longValue();
			Node node = null;

			try {

				node   = graphDb.getNodeById(id);
				result = nodeFactory.createNode(securityContext, node);

			} catch (NotFoundException nfe) {

				logger.log(Level.WARNING, "Node with id {0} not found in database!", id);

				throw new FrameworkException("FindNodeCommand", new IdNotFoundToken(id));
			}
		} else if (argument instanceof String) {

			// single string value, try to parse to long
			long id   = Long.parseLong((String) argument);
			Node node = graphDb.getNodeById(id);

			result = nodeFactory.createNode(securityContext, node);
		} else if (argument instanceof ReferenceNode) {

			// return reference node
			Node node = graphDb.getReferenceNode();

			result = nodeFactory.createNode(securityContext, node);
		} else if (argument instanceof NodeAttribute) {

			// single node attribute: find node by attribute..
			throw new UnsupportedOperationException("Not supported yet, use SearchNodeCommand instead!");
		} else if (argument instanceof AbstractNode) {

			result = (AbstractNode) argument;

		}

		return result;
	}
}
