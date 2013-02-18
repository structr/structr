/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.core.graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;

import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

//import org.structr.common.xpath.JXPathFinder;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Looks up a node in database and returns the result.
 *
 * @author Axel Morgner
 * @author Christian Morgner
 */
public class FindNodeCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(FindNodeCommand.class.getName());

	//~--- fields ---------------------------------------------------------

	private GraphDatabaseService graphDb;
	private NodeFactory nodeFactory;

	//~--- methods --------------------------------------------------------
	public AbstractNode execute(final Object argument) throws FrameworkException {
		return handleSingleArgument(nodeFactory, argument);
	}

	// <editor-fold defaultstate="collapsed" desc="private methods">
	private AbstractNode handleSingleArgument(final NodeFactory nodeFactory, final Object argument) throws FrameworkException {

		AbstractNode result;

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
