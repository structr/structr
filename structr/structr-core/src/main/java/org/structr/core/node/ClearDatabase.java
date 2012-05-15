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

import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.GenericNode;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		final GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		final NodeFactory nodeFactory      = (NodeFactory) arguments.get("nodeFactory");

		if (graphDb != null) {

			final Command transactionCommand = Services.command(securityContext, TransactionCommand.class);

			// final Command delRel             = Services.command(securityContext, DeleteRelationshipCommand.class);
			final Command delNode = Services.command(securityContext, DeleteNodeCommand.class);

			transactionCommand.execute(new BatchTransaction() {

				@Override
				public Object execute(Transaction tx) throws FrameworkException {

					long nodes                  = 0L;
					List<AbstractNode> allNodes = (List<AbstractNode>) nodeFactory.createNodes(securityContext, GlobalGraphOperations.at(graphDb).getAllNodes());

					for (AbstractNode node : allNodes) {

						// Delete only "our" nodes
						if (node.getStringProperty(AbstractNode.Key.uuid) != null) {

							delNode.execute(node);

							if (nodes % 100 == 0) {

								logger.log(Level.INFO, "Deleted {0} nodes, committing results to database.", nodes);
								tx.success();
								tx.finish();

								tx = graphDb.beginTx();

								logger.log(Level.FINE, "######## committed ########", nodes);

							}

							nodes++;

						}
					}

					return null;
				}

			});

		}

		return null;
	}
}
