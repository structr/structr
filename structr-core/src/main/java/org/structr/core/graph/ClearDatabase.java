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
import org.neo4j.tooling.GlobalGraphOperations;

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.Result;

//~--- classes ----------------------------------------------------------------

/**
 * Clears database.
 *
 * This command takes no parameters.
 *
 * @author Axel Morgner
 */
public class ClearDatabase extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(ClearDatabase.class.getName());

	//~--- methods --------------------------------------------------------

	public void execute() throws FrameworkException {

		final GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		final NodeFactory nodeFactory      = new NodeFactory(securityContext);

		if (graphDb != null) {

			final DeleteNodeCommand delNode   = Services.command(securityContext, DeleteNodeCommand.class);
			final Result<AbstractNode> result = nodeFactory.createAllNodes(GlobalGraphOperations.at(graphDb).getAllNodes());
			
			long deletedNodes = bulkGraphOperation(securityContext, result.getResults(), 1000, "ClearDatabase", new BulkGraphOperation<AbstractNode>() {

				@Override
				public void handleGraphObject(SecurityContext securityContext, AbstractNode node) {

					// Delete only "our" nodes
					if (node.getProperty(AbstractNode.uuid) != null) {

						try {
							delNode.execute(node);
							
						} catch (FrameworkException fex) {
							logger.log(Level.WARNING, "Unable to delete node {0}: {1}", new Object[] { node.getUuid(), fex.getMessage() } );
						}
					}
				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractNode node) {
					logger.log(Level.WARNING, "Unable to delete node {0}: {1}", new Object[] { node.getUuid(), t.getMessage() } );
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.log(Level.WARNING, "Unable to clear database: {0}", t.getMessage() );
				}
			});

			logger.log(Level.INFO, "Finished deleting {0} nodes", deletedNodes);

		}
	}
}
