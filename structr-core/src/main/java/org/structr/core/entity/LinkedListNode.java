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

import org.neo4j.graphdb.Node;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.relationship.ListSibling;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.DeleteRelationshipCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;

/**
 * Abstract base class for a multi-dimensional linked list datastructure.
 * 
 * @author Christian Morgner
 */
public abstract class LinkedListNode extends ValidatedNode {
	
	// this is not used for the node itself but for the relationship(s) this node maintains
	public static final PropertyKey<String> keyProperty = new StringProperty("key");

	/**
	 * Returns the predecessor of the given element in the list structure
	 * defined by this LinkedListManager.
	 *
	 * @param currentElement
	 * @return
	 */
	public LinkedListNode listGetPrevious(final Class<? extends ListSibling> type, final LinkedListNode currentElement) {

		ListSibling prevRel = currentElement.getIncomingRelationship(type);
		if (prevRel != null) {
			
			return prevRel.getStartNode();
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
	public LinkedListNode listGetNext(final Class<? extends ListSibling> type, final LinkedListNode currentElement) {

		ListSibling nextRel = currentElement.getOutgoingRelationship(type);
		if (nextRel != null) {
			
			return nextRel.getStartNode();
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
	public void listInsertBefore(final Class<? extends ListSibling> type, final LinkedListNode currentElement, final LinkedListNode newElement) throws FrameworkException {

		if (currentElement.getId() == newElement.getId()) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}

		final LinkedListNode previousElement = listGetPrevious(type, currentElement);
		if (previousElement == null) {

			// trivial: new node will become new head of existing list
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					linkNodes(type, newElement, currentElement);

					return null;
				}
			});

		} else {

			// not-so-trivial: insert new element between predecessor and current node
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					// delete old relationship
					unlinkNodes(type, previousElement, currentElement);
					
					linkNodes(type, previousElement, newElement);
					linkNodes(type, newElement, currentElement);

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
	public void listInsertAfter(final Class<? extends ListSibling> type, final LinkedListNode currentElement, final LinkedListNode newElement) throws FrameworkException {

		if (currentElement.getId() == newElement.getId()) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}

		final LinkedListNode next = listGetNext(type, currentElement);
		if (next == null) {

			// trivial: new node will become new tail of existing list
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					linkNodes(type, currentElement, newElement);

					return null;
				}
			});

		} else {

			// not-so-trivial: insert new element between current node and successor
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					unlinkNodes(type, currentElement, next);

					linkNodes(type, currentElement, newElement);
					linkNodes(type, newElement, next);

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
	public void listRemove(final Class<? extends ListSibling> type, final LinkedListNode currentElement) throws FrameworkException {
		
		final LinkedListNode previousElement = listGetPrevious(type, currentElement);
		final LinkedListNode nextElement     = listGetNext(type, currentElement);

		if (currentElement != null) {
			
			if (previousElement != null) {

				unlinkNodes(type, previousElement, currentElement);
			}

			if (nextElement != null) {

				unlinkNodes(type, currentElement, nextElement);
			}
		}

		if (previousElement != null && nextElement != null) {

			Node previousNode = previousElement.getNode();
			Node nextNode = nextElement.getNode();

			if (previousNode != null && nextNode != null) {

				linkNodes(type, previousElement, nextElement);
			}

		}
	}
	
	public void linkNodes(final Class<? extends Relation> type, final LinkedListNode startNode, final LinkedListNode endNode) throws FrameworkException {
		linkNodes(type, startNode, endNode, null);
	}
	
	public void linkNodes(final Class<? extends Relation> type, final LinkedListNode startNode, final LinkedListNode endNode, final PropertyMap properties) throws FrameworkException {
		
		CreateRelationshipCommand cmd = Services.command(securityContext, CreateRelationshipCommand.class);
		Relation rel                  = getRelationshipForType(type);
		
		// do not check for duplicates here
		cmd.execute(startNode, endNode, rel.getRelationshipType(), properties, false);
	}
	
	public void unlinkNodes(final Class<? extends Relation> type, final NodeInterface startNode, final NodeInterface endNode) throws FrameworkException {
		
		final DeleteRelationshipCommand cmd = Services.command(securityContext, DeleteRelationshipCommand.class);
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				// FIXME: this is not complete!
				
				for (AbstractRelationship rel : startNode.getRelationships()) {
					
					if (rel != null && rel.getEndNode().equals(endNode)) {
						cmd.execute(rel);
					}
				}
				
				return null;
			}
			
		});
	}
}
