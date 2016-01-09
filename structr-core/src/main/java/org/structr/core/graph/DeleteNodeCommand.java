/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.graph;

import java.util.HashSet;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Relation;

//~--- JDK imports ------------------------------------------------------------
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.NotFoundException;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;

//~--- classes ----------------------------------------------------------------
/**
 * Deletes a node.
 *
 *
 */
public class DeleteNodeCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(DeleteNodeCommand.class.getName());

	private final Set<NodeInterface> deletedNodes = new HashSet<>();

	//~--- methods --------------------------------------------------------
	public void execute(NodeInterface node) {

		doDeleteNode(node);
		deletedNodes.clear();

	}

	private AbstractNode doDeleteNode(final NodeInterface node) {

		try {
			if (!deletedNodes.contains(node) && node.getUuid() == null) {

				logger.log(Level.WARNING, "Will not delete node which has no UUID, dumping stack.");
				Thread.dumpStack();

				return null;
			}

		} catch (java.lang.IllegalStateException ise) {
			logger.log(Level.WARNING, "Trying to delete a node which is already deleted", ise.getMessage());
			return null;
		}

		deletedNodes.add(node);

		App app = StructrApp.getInstance(securityContext);

		try {

			List<NodeInterface> nodesToCheckAfterDeletion = new LinkedList<>();

			// Delete all end nodes of outgoing relationships which are connected
			// by relationships which are marked with DELETE_OUTGOING
			for (AbstractRelationship rel : node.getOutgoingRelationships()) {

				// deleted rels can be null..
				if (rel != null) {

					int cascadeDelete = rel.cascadeDelete();
					NodeInterface endNode = rel.getTargetNode();

					if ((cascadeDelete & Relation.CONSTRAINT_BASED) == Relation.CONSTRAINT_BASED) {

						nodesToCheckAfterDeletion.add(endNode);
					}

					if (!deletedNodes.contains(endNode) && ((cascadeDelete & Relation.SOURCE_TO_TARGET) == Relation.SOURCE_TO_TARGET)) {

						// remove end node from index
						endNode.removeFromIndex();
						doDeleteNode(endNode);
					}
				}
			}

			// Delete all start nodes of incoming relationships which are connected
			// by relationships which are marked with DELETE_INCOMING
			for (AbstractRelationship rel : node.getIncomingRelationships()) {

				// deleted rels can be null
				if (rel != null) {

					int cascadeDelete = rel.cascadeDelete();
					NodeInterface startNode = rel.getSourceNode();

					if ((cascadeDelete & Relation.CONSTRAINT_BASED) == Relation.CONSTRAINT_BASED) {

						nodesToCheckAfterDeletion.add(startNode);
					}

					if (!deletedNodes.contains(startNode) && ((cascadeDelete & Relation.TARGET_TO_SOURCE) == Relation.TARGET_TO_SOURCE)) {

						// remove start node from index
						startNode.removeFromIndex();
						doDeleteNode(startNode);
					}
				}
			}

			// deletion callback, must not prevent node deletion!
			node.onNodeDeletion();

			try {
				// Delete any relationship (this is PASSIVE DELETION)
				for (AbstractRelationship r : node.getRelationships()) {

					if (r != null) {

						app.delete(r);
					}
				}

			} catch (NotFoundException nfex) {
				// ignore, we cannot do anything about it..
			}

			// remove node from index
			node.removeFromIndex();

			// mark node as deleted in transaction
			TransactionCommand.nodeDeleted(securityContext.getCachedUser(), node);

			// delete node in database
			node.getNode().delete();

			// now check again the deletion cascade for violated constraints
			// Check all end nodes of outgoing relationships which are connected if they are
			// still valid after node deletion
			for (NodeInterface nodeToCheck : nodesToCheckAfterDeletion) {

				ErrorBuffer errorBuffer = new ErrorBuffer();

				if (!deletedNodes.contains(nodeToCheck) && !nodeToCheck.isValid(errorBuffer)) {

					// remove end node from index
					nodeToCheck.removeFromIndex();
					doDeleteNode(nodeToCheck);
				}
			}

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Exception while deleting node: {0}", t);

		}

		return null;
	}
}
