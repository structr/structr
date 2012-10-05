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



package org.structr.core.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;

import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;
import org.structr.core.Services;
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

	//~--- fields ---------------------------------------------------------

	private GraphDatabaseService graphDb;
	private NodeFactory nodeFactory;

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		graphDb     = (GraphDatabaseService) arguments.get("graphDb");
		nodeFactory = new NodeFactory(securityContext);

		if (graphDb != null) {

			switch (parameters.length) {

				case 0 :
					throw new UnsupportedArgumentError("No arguments supplied");

				case 1 :
					return (handleSingleArgument(nodeFactory, parameters[0]));

				default :
					throw new UnsupportedArgumentError("Too many arguments supplied");

			}

		}

		return null;

	}

	// <editor-fold defaultstate="collapsed" desc="private methods">
	private Object handleSingleArgument(final NodeFactory nodeFactory, final Object argument) throws FrameworkException {

		Object result;

		if (argument instanceof Node) {

			/*
			 * If argument is a Neo4j node, just let the node factory create
			 * an instance of AbstractNode out of it.
			 */
			result = nodeFactory.createNode((Node) argument);

			if (result != null) {

				return result;
			}
		} else if (argument instanceof Long) {

			/*
			 * In case of a numerical id, let Neo4j find the node
			 */
			long id = ((Long) argument).longValue();

			try {

				result = findByDbId(id);

				if (result != null) {

					return result;
				}

			} catch (NotFoundException nfe) {

				logger.log(Level.WARNING, "Node with long id {0} not found in database!", id);

			}
		} else if (argument instanceof String) {

			/*
			 * If given id is a string, try to find in the UUID index
			 */
			result = (AbstractNode) Services.command(securityContext, GetNodeByIdCommand.class).execute((String) argument);

			if (result != null) {

				return result;
			}
		} else if (argument instanceof NodeAttribute) {

			/*
			 * Single node attribute: find node by attribute is not supported by this command
			 */
			throw new UnsupportedOperationException("Not supported, use SearchNodeCommand instead!");
		} else if (argument instanceof AbstractNode) {

			/*
			 * If argument is already an AbstractNode, return it directly
			 */
			result = (AbstractNode) argument;

			if (result != null) {

				return result;
			}
		}

		throw new FrameworkException("FindNodeCommand", new IdNotFoundToken(argument));

	}

	/**
	 * Lookup a db node by numerical id and create a framework node from it
	 *
	 * @param id
	 * @return
	 * @throws FrameworkException
	 */
	private AbstractNode findByDbId(final long id) throws FrameworkException {

		Node node = graphDb.getNodeById(id);

		return nodeFactory.createNode(node);

	}

}
