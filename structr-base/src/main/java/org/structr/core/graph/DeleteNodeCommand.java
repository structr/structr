/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.traits.NodeTrait;
import org.structr.core.traits.RelationshipTrait;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Deletes a node. Caution, this command cannot be used multiple times, please instantiate
 * a new command for every delete action.
 *
 */
public class DeleteNodeCommand extends NodeServiceCommand {

	public void execute(final NodeTrait node) throws FrameworkException {

		if (securityContext.doCascadingDelete()) {

			cascadeDelete(node);

		} else {

			node.onNodeDeletion(securityContext);
			node.getNode().delete(true);
		}
	}

	private void cascadeDelete(final NodeTrait node) throws FrameworkException {

		final DeleteRelationshipCommand drc       = StructrApp.getInstance().command(DeleteRelationshipCommand.class);
		final Principal user                      = securityContext.getCachedUser();
		final Set<RelationshipTrait> relsToDelete = new HashSet<>();
		final Set<NodeTrait> nodesToDelete        = new HashSet<>();
		final Set<NodeTrait> nodesToCheck         = new HashSet<>();

		// collect nodes that are to be deleted
		collectNodesForCascadingDelete(nodesToDelete, relsToDelete, nodesToCheck, node);

		// remove all deleted nodes from nodes to check
		nodesToCheck.removeAll(nodesToDelete);

		// notify nodes of pending deletion
		for (final NodeTrait deleteMe : nodesToDelete) {
			deleteMe.onNodeDeletion(securityContext);
		}

		// delete all rels
		for (final RelationshipTrait deleteMe : relsToDelete) {
			drc.execute(deleteMe);
		}

		// delete all nodes
		for (final NodeTrait deleteMe : nodesToDelete) {

			// node can already be deleted by a previous run
			if (!deleteMe.isDeleted()) {

				// mark node as deleted in transaction
				TransactionCommand.nodeDeleted(user, deleteMe);

				// delete node in database
				deleteMe.getNode().delete(true);
			}
		}

		final Set<NodeTrait> invalidNodes = new HashSet<>();

		// check remaining nodes and delete as well
		for (final NodeTrait checkMe : nodesToCheck) {

			ErrorBuffer errorBuffer = new ErrorBuffer();

			if (!checkMe.isValid(errorBuffer)) {

				invalidNodes.add(checkMe);
			}
		}

		// recurse for constraint-based cascading delete
		for (final NodeTrait invalidNode : invalidNodes) {

			cascadeDelete(invalidNode);
		}
	}

	private void collectNodesForCascadingDelete(final Set<NodeTrait> nodesToDelete, final Set<RelationshipTrait> relsToDelete, final Set<NodeTrait> nodesToCheck, final NodeTrait node) {

		if (node == null) {
			return;
		}

		final Queue<NodeTrait> queue = new ArrayDeque<>();

		// initial object
		queue.add(node);

		while (!queue.isEmpty()) {

			final NodeTrait current = queue.remove();

			if (current != null && !nodesToDelete.contains(current) && !current.isDeleted()) {

				nodesToDelete.add(current);

				for (RelationshipTrait rel : current.getRelationships()) {

					// deleted rels can be null..
					if (rel != null) {

						final int cascadeDelete   = rel.getCascadingDeleteFlag();
						final NodeTrait startNode = rel.getSourceNode();
						final NodeTrait endNode   = rel.getTargetNode();

						if (startNode != null && endNode != null) {

							// outgoing?
							if (current.getNode().getId().equals(startNode.getNode().getId())) {

								if ((cascadeDelete & Relation.CONSTRAINT_BASED) == Relation.CONSTRAINT_BASED) {

									nodesToCheck.add(endNode);
								}

								if ((cascadeDelete & Relation.SOURCE_TO_TARGET) == Relation.SOURCE_TO_TARGET) {

									if (!nodesToDelete.contains(endNode)) {

										queue.add(endNode);
									}
								}

							} else {

								if ((cascadeDelete & Relation.CONSTRAINT_BASED) == Relation.CONSTRAINT_BASED) {

									nodesToCheck.add(startNode);
								}

								if ((cascadeDelete & Relation.TARGET_TO_SOURCE) == Relation.TARGET_TO_SOURCE) {

									if (!nodesToDelete.contains(startNode)) {

										queue.add(startNode);
									}
								}
							}
						}

						relsToDelete.add(rel);
					}
				}
			}
		}
	}











	/*


	private final Set<NodeInterface> deletedNodes = new LinkedHashSet<>();


	public void execute(final NodeInterface node) throws FrameworkException {

		if (securityContext.doCascadingDelete()) {

			doDeleteNode(node);

			for (final NodeInterface deleteMe : deletedNodes) {

				// mark node as deleted in transaction
				TransactionCommand.nodeDeleted(securityContext.getCachedUser(), deleteMe);

				// delete node in database
				deleteMe.getNode().delete(false);
			}

		} else {

			node.onNodeDeletion(securityContext);
			node.getNode().delete(true);
		}

		// allow re-used of this command
		deletedNodes.clear();
	}

	private void doDeleteNode(final NodeInterface node) throws FrameworkException {

		if (node == null || TransactionCommand.isDeleted(node.getNode())) {
			return;
		}

		try {
			if (!deletedNodes.contains(node) && node.getUuid() == null) {

				logger.debug("Will not delete node which has no UUID: {}", node);

				return;
			}

		} catch (java.lang.IllegalStateException ise) {

			logger.warn("Trying to delete a node which is already deleted", ise.getMessage());
			return;

		} catch (org.structr.api.NotFoundException nfex) {
			nfex.printStackTrace();
		}

		deletedNodes.add(node);

		App app = StructrApp.getInstance(securityContext);

		node.getNode().invalidate();

		final List<NodeInterface> nodesToCheckAfterDeletion = new LinkedList<>();
		final List<AbstractRelationship> allRelationships   = Iterables.toList(node.getRelationships());
		final List<AbstractRelationship> outgoingOnly       = Iterables.toList(node.getOutgoingRelationships());
		final List<AbstractRelationship> incomingOnly       = Iterables.toList(node.getIncomingRelationships());


		// Delete all end nodes of outgoing relationships which are connected
		// by relationships which are marked with DELETE_OUTGOING
		for (AbstractRelationship rel : outgoingOnly) {

			// deleted rels can be null..
			if (rel != null) {

				int cascadeDelete = rel.getCascadingDeleteFlag();
				NodeInterface endNode = rel.getTargetNode();

				if ((cascadeDelete & Relation.CONSTRAINT_BASED) == Relation.CONSTRAINT_BASED) {

					nodesToCheckAfterDeletion.add(endNode);
				}

				if (!deletedNodes.contains(endNode) && ((cascadeDelete & Relation.SOURCE_TO_TARGET) == Relation.SOURCE_TO_TARGET)) {

					// remove end node from index
					doDeleteNode(endNode);
				}
			}
		}

		// Delete all start nodes of incoming relationships which are connected
		// by relationships which are marked with DELETE_INCOMING
		for (AbstractRelationship rel : incomingOnly) {

			final int cascadeDelete       = rel.getCascadingDeleteFlag();
			final NodeInterface startNode = rel.getSourceNode();

			if ((cascadeDelete & Relation.CONSTRAINT_BASED) == Relation.CONSTRAINT_BASED) {

				nodesToCheckAfterDeletion.add(startNode);
			}

			if (!deletedNodes.contains(startNode) && ((cascadeDelete & Relation.TARGET_TO_SOURCE) == Relation.TARGET_TO_SOURCE)) {

				doDeleteNode(startNode);
			}
		}

		// deletion callback, must not prevent node deletion!
		node.onNodeDeletion(securityContext);

		// Delete any relationship (this is PASSIVE DELETION)
		if (!allRelationships.isEmpty()) {

			final DeleteRelationshipCommand cmd = app.command(DeleteRelationshipCommand.class);
			for (AbstractRelationship r : allRelationships) {

				cmd.execute(r);
			}
		}

		// now check again the deletion cascade for violated constraints
		// Check all end nodes of outgoing relationships which are connected if they are
		// still valid after node deletion
		for (NodeInterface nodeToCheck : nodesToCheckAfterDeletion) {

			ErrorBuffer errorBuffer = new ErrorBuffer();

			if (!deletedNodes.contains(nodeToCheck) && !nodeToCheck.isValid(errorBuffer)) {

				doDeleteNode(nodeToCheck);
			}
		}
	}
	*/
}
