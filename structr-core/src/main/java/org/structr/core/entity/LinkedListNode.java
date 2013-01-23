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
package org.structr.core.entity;

import java.util.List;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.CollectionProperty;

/**
 * Abstract base class for a linked list datastructure.
 * 
 * @author Christian Morgner
 */
public abstract class LinkedListNode extends AbstractNode {

	/**
	 * Returns the predecessor of the given element in the list structure
	 * defined by this LinkedListManager.
	 *
	 * @param currentElement
	 * @return
	 */
	protected LinkedListNode listGetPrevious(final CollectionProperty<? extends LinkedListNode> listProperty, final LinkedListNode currentElement) {

		final RelationshipType relType                         = listProperty.getRelType();
		final List<AbstractRelationship> incomingRelationships = currentElement.getIncomingRelationships(relType);
		
		if (incomingRelationships != null && !incomingRelationships.isEmpty()) {

			int size = incomingRelationships.size();
			if (size == 1) {

				AbstractRelationship incomingRel = incomingRelationships.get(0);
				if (incomingRel != null) {

					return (LinkedListNode)incomingRel.getStartNode();
				}
			}

			throw new IllegalStateException("Given node is not a valid list node for the given relationship type.");
		}

		return null;
	}

	/**
	 * Returns the successor of the given element in the list structure
	 * defined by this LinkedListManager.
	 *
	 * @param currentElement
	 * @return
	 */
	protected LinkedListNode listGetNext(final CollectionProperty<? extends LinkedListNode> listProperty, final LinkedListNode currentElement) {
		
		final RelationshipType relType                   = listProperty.getRelType();
		List<AbstractRelationship> outgoingRelationships = currentElement.getOutgoingRelationships(relType);
		
		if (outgoingRelationships != null && !outgoingRelationships.isEmpty()) {

			int size = outgoingRelationships.size();
			if (size == 1) {

				AbstractRelationship outgoingRel = outgoingRelationships.get(0);
				if (outgoingRel != null) {

					return (LinkedListNode)outgoingRel.getEndNode();
				}
			}

			throw new IllegalStateException("Given node is not a valid list node for the given relationship type: found " + size + " outgoing relationships of type " + relType);
		}

		return null;
	}

	/**
	 * Inserts newElement before currentElement in the list defined by this
	 * LinkedListManager.
	 *
	 * @param currentElement the reference element
	 * @param newElement the new element
	 */
	protected void listInsertBefore(final CollectionProperty<? extends LinkedListNode> listProperty, final LinkedListNode currentElement, final LinkedListNode newElement) throws FrameworkException {

		if (currentElement.getId() == newElement.getId()) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}

		final RelationshipType relType       = listProperty.getRelType();
		final LinkedListNode previousElement = listGetPrevious(listProperty, currentElement);

		if (previousElement == null) {

			// trivial: new node will become new head of existing list
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					Node startNode = newElement.getNode();
					Node endNode = currentElement.getNode();

					if (startNode != null && endNode != null) {
						startNode.createRelationshipTo(endNode, relType);
					}

					return null;
				}
			});

		} else {

			// not-so-trivial: insert new element between predecessor and current node
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					Node previousNode = previousElement.getNode();
					Node currentNode = currentElement.getNode();
					Node newNode = newElement.getNode();

					if (previousNode != null && newNode != null && currentNode != null) {

						// delete old relationship
						removeRelationshipBetween(listProperty, previousNode, currentNode);

						// create two new ones
						previousNode.createRelationshipTo(newNode, relType);
						newNode.createRelationshipTo(currentNode, relType);
					}

					return null;
				}
			});
		}
	}

	/**
	 * Inserts newElement after currentElement in the list defined by this
	 * LinkedListManager.
	 *
	 * @param currentElement the reference element
	 * @param newElement the new element
	 */
	protected void listInsertAfter(final CollectionProperty<? extends LinkedListNode> listProperty, final LinkedListNode currentElement, final LinkedListNode newElement) throws FrameworkException {

		if (currentElement.getId() == newElement.getId()) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}

		final RelationshipType relType = listProperty.getRelType();
		final LinkedListNode next      = listGetNext(listProperty, currentElement);
		
		if (next == null) {

			// trivial: new node will become new tail of existing list
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					Node startNode = currentElement.getNode();
					Node endNode = newElement.getNode();

					if (startNode != null && endNode != null) {
						startNode.createRelationshipTo(endNode, relType);
					}

					return null;
				}
			});

		} else {

			// not-so-trivial: insert new element between current node and successor
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					Node nextNode = next.getNode();
					Node currentNode = currentElement.getNode();
					Node newNode = newElement.getNode();

					if (nextNode != null && newNode != null && currentNode != null) {

						// delete old relationship
						removeRelationshipBetween(listProperty, currentNode, nextNode);

						// create two new ones
						currentNode.createRelationshipTo(newNode, relType);
						newNode.createRelationshipTo(nextNode, relType);
					}

					return null;
				}
			});
		}
	}

	/**
	 * Removes the current element from the list defined by this
	 * LinkedListManager.
	 *
	 * @param currentElement the element to be removed
	 */
	protected void listRemove(CollectionProperty<? extends LinkedListNode> listProperty, final LinkedListNode currentElement) {
		
		final RelationshipType relType                                  = listProperty.getRelType();
		final LinkedListNode previousElement                            = listGetPrevious(listProperty, currentElement);
		final LinkedListNode nextElement                                = listGetNext(listProperty, currentElement);
		final Node currentNode                                          = currentElement.getNode();

		if (previousElement != null) {

			Node previousNode = previousElement.getNode();
			if (previousNode != null && currentNode != null) {

				removeRelationshipBetween(listProperty, previousNode, currentNode);
			}
		}

		if (nextElement != null) {

			Node nextNode = nextElement.getNode();
			if (nextNode != null && currentNode != null) {

				removeRelationshipBetween(listProperty, currentNode, nextNode);
			}
		}

		if (previousElement != null && nextElement != null) {

			Node previousNode = previousElement.getNode();
			Node nextNode = nextElement.getNode();

			if (previousNode != null && nextNode != null) {

				previousNode.createRelationshipTo(nextNode, relType);
			}

		}
	}

	/**
	 * Removes the relationship of the type defined in this
	 * LinkedListManager between the two nodes. Please note that this method
	 * does not create its own transaction, so it needs to be wrapped into a
	 * transaction.
	 *
	 * @param startNode
	 * @param endNode
	 */
	protected void removeRelationshipBetween(CollectionProperty<? extends LinkedListNode> listProperty, final Node startNode, final Node endNode) {

		final RelationshipType relType = listProperty.getRelType();

		if (startNode != null && endNode != null) {

			if (startNode.hasRelationship(relType, Direction.OUTGOING)) {

				Relationship rel = startNode.getSingleRelationship(relType, Direction.OUTGOING);
				if (rel.getEndNode().getId() == endNode.getId()) {

					rel.delete();
				}
			}
		}
	}
}
