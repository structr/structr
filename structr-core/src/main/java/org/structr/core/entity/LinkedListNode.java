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
import org.neo4j.graphdb.Node;
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

		final LinkedListNode previousElement = listGetPrevious(listProperty, currentElement);
		if (previousElement == null) {

			// trivial: new node will become new head of existing list
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					listProperty.createRelationship(securityContext, newElement, currentElement);

					return null;
				}
			});

		} else {

			// not-so-trivial: insert new element between predecessor and current node
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					// delete old relationship
					listProperty.removeRelationship(securityContext, previousElement, currentElement);
					
					listProperty.createRelationship(securityContext, previousElement, newElement);
					listProperty.createRelationship(securityContext, newElement, currentElement);

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

					listProperty.createRelationship(securityContext, currentElement, newElement);

					return null;
				}
			});

		} else {

			// not-so-trivial: insert new element between current node and successor
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					listProperty.removeRelationship(securityContext, currentElement, next);

					listProperty.createRelationship(securityContext, currentElement, newElement);
					listProperty.createRelationship(securityContext, newElement, next);

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
	protected void listRemove(CollectionProperty<? extends LinkedListNode> listProperty, final LinkedListNode currentElement) throws FrameworkException {
		
		final RelationshipType relType                                  = listProperty.getRelType();
		final LinkedListNode previousElement                            = listGetPrevious(listProperty, currentElement);
		final LinkedListNode nextElement                                = listGetNext(listProperty, currentElement);

		if (currentElement != null) {
			
			if (previousElement != null) {

				listProperty.removeRelationship(securityContext, previousElement, currentElement);
			}

			if (nextElement != null) {

				listProperty.removeRelationship(securityContext, currentElement, nextElement);
			}
		}

		if (previousElement != null && nextElement != null) {

			Node previousNode = previousElement.getNode();
			Node nextNode = nextElement.getNode();

			if (previousNode != null && nextNode != null) {

				listProperty.createRelationship(securityContext, previousElement, nextElement);
			}

		}
	}
}
