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

import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.RelationClass;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.ErrorBuffer;

//~--- classes ----------------------------------------------------------------

/**
 * Delete a node
 *
 * @param node the node, a Long nodeId or a String nodeId
 * @return null
 *
 * @author Axel Morgner
 */
public class DeleteNodeCommand extends NodeServiceCommand {

	private static final Logger logger            = Logger.getLogger(DeleteNodeCommand.class.getName());
	private static Set<AbstractNode> deletedNodes = new LinkedHashSet<AbstractNode>();

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		AbstractNode node = null;
		Boolean cascade   = false;
		Command findNode  = Services.command(securityContext, FindNodeCommand.class);

		switch (parameters.length) {

			case 1 :    // single parameter: node
				node = (AbstractNode) findNode.execute(parameters[0]);

				break;

			case 2 :    // first parameter: node, second parameter: cascade
				node = (AbstractNode) findNode.execute(parameters[0]);

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

			logger.log(Level.WARNING, "Could not delete node null");

			return null;

		}

		if (node.getId() == 0) {

			logger.log(Level.WARNING, "Deleting the root node is not allowed.");

			return null;

		}

//              EntityContext.getGlobalModificationListener().graphObjectDeleted(securityContext, node);
		doDeleteNode(node, cascade);
		deletedNodes.clear();

		return null;

	}

	private AbstractNode doDeleteNode(final AbstractNode node, final Boolean cascade) throws FrameworkException {

		deletedNodes.add(node);

		if (node.getStringProperty(AbstractNode.uuid) == null) {

			logger.log(Level.WARNING, "Will not delete node which has no UUID");

			return null;

		}

		// final Node node                  = graphDb.getNodeById(structrNode.getId());
		final Command transactionCommand  = Services.command(securityContext, TransactionCommand.class);
		final Command removeNodeFromIndex = Services.command(securityContext, RemoveNodeFromIndex.class);
		final Command deleteRel           = Services.command(securityContext, DeleteRelationshipCommand.class);

		transactionCommand.execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				try {

					List<AbstractNode> nodesToCheckAfterDeletion = new LinkedList<AbstractNode>();

					if (cascade) {

						// Delete all end nodes of outgoing relationships which are connected
						// by relationships which are marked with DELETE_OUTGOING
						List<AbstractRelationship> outgoingRels = node.getOutgoingRelationships();

						for (AbstractRelationship rel : outgoingRels) {

							int cascadeDelete    = rel.cascadeDelete();
							AbstractNode endNode = rel.getEndNode();

							if ((cascadeDelete & RelationClass.DELETE_IF_CONSTRAINT_WOULD_BE_VIOLATED) == RelationClass.DELETE_IF_CONSTRAINT_WOULD_BE_VIOLATED) {

								nodesToCheckAfterDeletion.add(endNode);
							}

							if (!deletedNodes.contains(endNode) && ((cascadeDelete & RelationClass.DELETE_OUTGOING) == RelationClass.DELETE_OUTGOING)) {

								// remove end node from index
								removeNodeFromIndex.execute(endNode);
								doDeleteNode(endNode, cascade);
							}

						}

						// Delete all start nodes of incoming relationships which are connected
						// by relationships which are marked with DELETE_INCOMING
						List<AbstractRelationship> incomingRels = node.getIncomingRelationships();

						for (AbstractRelationship rel : incomingRels) {

							int cascadeDelete      = rel.cascadeDelete();
							AbstractNode startNode = rel.getStartNode();

							if ((cascadeDelete & RelationClass.DELETE_IF_CONSTRAINT_WOULD_BE_VIOLATED) == RelationClass.DELETE_IF_CONSTRAINT_WOULD_BE_VIOLATED) {

								nodesToCheckAfterDeletion.add(startNode);
							}

							if (!deletedNodes.contains(startNode) && ((cascadeDelete & RelationClass.DELETE_INCOMING) == RelationClass.DELETE_INCOMING)) {

								// remove start node from index
								removeNodeFromIndex.execute(startNode);
								doDeleteNode(startNode, cascade);
							}

						}
					}

					// deletion callback, must not prevent node deletion!
					node.onNodeDeletion();

					// Delete any relationship
					List<AbstractRelationship> rels = node.getRelationships();

					for (AbstractRelationship r : rels) {

						deleteRel.execute(r);
					}

					// remove node from index
					removeNodeFromIndex.execute(node);

					// delete node in database
					node.getNode().delete();

					// now check again the deletion cascade for violated constraints
					if (cascade) {

						// Check all end nodes of outgoing relationships which are connected if they are
						// still valid after node deletion
						for (AbstractNode nodeToCheck : nodesToCheckAfterDeletion) {

							ErrorBuffer errorBuffer = new ErrorBuffer();
							
							if (!nodeToCheck.isValid(errorBuffer)) {

								// remove end node from index
								removeNodeFromIndex.execute(nodeToCheck);
								doDeleteNode(nodeToCheck, cascade);
							}

						}
					}

				} catch (Throwable t) {

					logger.log(Level.WARNING, "Exception while deleting node: {0}", t);

				}

				return null;

			}

		});

		return null;

	}

}
