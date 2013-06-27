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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Services;

//~--- classes ----------------------------------------------------------------

/**
 * Delete all nodes which have the 'deleted' property set to true
 *
 * @author Axel Morgner
 */
public class BulkDeleteSoftDeletedNodesCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkDeleteSoftDeletedNodesCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void execute(final Map<String, Object> properties) throws FrameworkException {

		final GraphDatabaseService graphDb     = (GraphDatabaseService) arguments.get("graphDb");
		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
		final NodeFactory nodeFactory          = new NodeFactory(superUserContext, true, false);
		
		final DeleteNodeCommand deleteNode     = Services.command(SecurityContext.getSuperUserInstance(), DeleteNodeCommand.class);
		
		if (graphDb != null) {

			Result<AbstractNode> nodes = nodeFactory.instantiateAll(GlobalGraphOperations.at(graphDb).getAllNodes());

			final boolean erase;
			
			if (properties.containsKey("erase") && properties.get("erase").equals("true")) {
				erase = true;
			} else {
				erase = false;
			}
			
			bulkGraphOperation(securityContext, nodes.getResults(), 1000, "DeleteSoftDeletedNodes", new BulkGraphOperation<AbstractNode>() {

				@Override
				public void handleGraphObject(SecurityContext securityContext, AbstractNode node) {

					if (node.isDeleted()) {
						logger.log(Level.INFO, "Found deleted node: {0}", node);
						
						if (erase) {
						
							try {
								deleteNode.execute(node, true);
							} catch (FrameworkException ex) {
								logger.log(Level.WARNING, "Could not delete node " + node, ex);
							}
						}
					}
					
				}

				@Override
				public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractNode node) {
					logger.log(Level.WARNING, "Unable to set properties of node {0}: {1}", new Object[] { node.getUuid(), t.getMessage() } );
				}

				@Override
				public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
					logger.log(Level.WARNING, "Unable to set node properties: {0}", t.getMessage() );
				}
			});

		}

		logger.log(Level.INFO, "Done");
	}

}
