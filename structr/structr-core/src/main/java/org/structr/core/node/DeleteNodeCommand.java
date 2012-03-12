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
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.Traversal;

import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.RelationClass;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Deletes a node, or removes a LINK relationship respectively.
 *
 * @param node the node, a Long nodeId or a String nodeId
 * @return null
 *
 * @author Axel Morgner
 */
public class DeleteNodeCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(DeleteNodeCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		AbstractNode node = null;
		Boolean cascade   = false;
		Command findNode  = Services.command(securityContext, FindNodeCommand.class);

		switch (parameters.length) {

			case 1 :    // first parameter: node
				if (parameters[0] instanceof Long) {

					long id = ((Long) parameters[0]).longValue();

					node = (AbstractNode) findNode.execute(id);

				} else if (parameters[0] instanceof AbstractNode) {

					node = ((AbstractNode) parameters[0]);

				} else if (parameters[0] instanceof String) {

					long id = Long.parseLong((String) parameters[0]);

					node = (AbstractNode) findNode.execute(id);

				}

				break;

			case 2 :    // first parameter: node, second parameter: cascade
				if (parameters[0] instanceof Long) {

					long id = ((Long) parameters[0]).longValue();

					node = (AbstractNode) findNode.execute(id);

				} else if (parameters[0] instanceof AbstractNode) {

					node = ((AbstractNode) parameters[0]);

				} else if (parameters[0] instanceof String) {

					long id = Long.parseLong((String) parameters[0]);

					node = (AbstractNode) findNode.execute(id);

				}

				if (parameters[1] instanceof String) {

					cascade = Boolean.parseBoolean((String) parameters[2]);

				} else if (parameters[1] instanceof Boolean) {

					cascade = (Boolean) parameters[1];

				}

				break;

			default :
				break;

		}

		if (node == null) {

			setExitCode(Command.exitCode.FAILURE);
			setErrorMessage("Could not delete node null");
			logger.log(Level.WARNING, getErrorMessage());

			return null;

		}

		if (node.getId() == 0) {

			setExitCode(Command.exitCode.FAILURE);
			setErrorMessage("Deleting the root node is not allowed.");
			logger.log(Level.WARNING, getErrorMessage());

			return null;

		}

//              EntityContext.getGlobalModificationListener().graphObjectDeleted(securityContext, node);
		doDeleteNode(node, cascade);

		return null;
	}

	private AbstractNode doDeleteNode(final AbstractNode node, final Boolean cascade) throws FrameworkException {

		// final Node node                  = graphDb.getNodeById(structrNode.getId());
		final Command transactionCommand = Services.command(securityContext, TransactionCommand.class);

		transactionCommand.execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				if (cascade) {

					// Delete all end nodes of outgoing relationships which are connected
					// by relationships which have a cascadeDelete value of DELETE_OUTGOING or DELETE_INCOMING
					List<AbstractRelationship> outgoingRels = node.getOutgoingRelationships();

					for (AbstractRelationship rel : outgoingRels) {
						
						int cascadeDelete = rel.cascadeDelete();

						if (cascadeDelete == RelationClass.DELETE_OUTGOING || cascadeDelete == RelationClass.DELETE_BOTH) {

							try {
								doDeleteNode(rel.getEndNode(), cascade);
							} catch (Throwable t) {
								logger.log(Level.WARNING, "Exception while deleting connected node: {0}", t);
							}

						}

					}

					// Delete all start nodes of incoming relationships which are connected
					// by relationships which have a cascadeDelete value of DELETE_INCOMING or DELETE_INCOMING
					List<AbstractRelationship> incomingRels = node.getIncomingRelationships();

					for (AbstractRelationship rel : incomingRels) {
						
						int cascadeDelete = rel.cascadeDelete();

						if (cascadeDelete == RelationClass.DELETE_INCOMING || cascadeDelete == RelationClass.DELETE_INCOMING) {

							try {
								doDeleteNode(rel.getStartNode(), cascade);
							} catch (Throwable t) {
								logger.log(Level.WARNING, "Exception while deleting connected node: {0}", t);
							}

						}

					}
				}

				// Delete any relationship
				List<AbstractRelationship> rels = node.getRelationships();

				for (AbstractRelationship r : rels) {

					r.getRelationship().delete();

				}

				// deletion callback, must not prevent node deletion!
				try {
					node.onNodeDeletion();
				} catch (Throwable t) {
					logger.log(Level.WARNING, "Exception while calling onDeletion callback: {0}", t);
				}

				// delete node in database
				node.getNode().delete();
				setExitCode(Command.exitCode.SUCCESS);

				return null;
			}

		});

		return null;
	}
}
