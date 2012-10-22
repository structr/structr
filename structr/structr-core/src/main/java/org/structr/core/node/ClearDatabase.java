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
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.GraphObject;
import org.structr.core.Result;

//~--- classes ----------------------------------------------------------------

/**
 * Clear database.
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

			final DeleteNodeCommand delNode = Services.command(securityContext, DeleteNodeCommand.class);

			Services.command(securityContext, TransactionCommand.class).execute(new BatchTransaction() {

				@Override
				public Object execute(Transaction tx) throws FrameworkException {

					Result<AbstractNode> result = nodeFactory.createAllNodes(GlobalGraphOperations.at(graphDb).getAllNodes());
					long nodes    = 0L;

					for (AbstractNode node : result.getResults()) {

						// Delete only "our" nodes
						if (node.getStringProperty(AbstractNode.uuid) != null) {

							delNode.execute(node);

							if (nodes > 1000 && nodes % 1000 == 0) {

								logger.log(Level.INFO, "Deleted {0} nodes, committing results to database.", nodes);
								tx.success();
								tx.finish();

								tx = graphDb.beginTx();

								logger.log(Level.FINE, "######## committed ########", nodes);

							}

							nodes++;

						}
					}
                                        
                                        logger.log(Level.INFO, "Finished deleting {0} nodes", nodes);

					return null;
				}

			});

		}
	}
}
