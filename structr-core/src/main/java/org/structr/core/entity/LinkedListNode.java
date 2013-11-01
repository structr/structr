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
import org.structr.core.entity.relationship.AbstractListSiblings;
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
public abstract class LinkedListNode<R extends AbstractListSiblings<T, T>, T extends LinkedListNode> extends ValidatedNode {
	
	// this is not used for the node itself but for the relationship(s) this node maintains
	public static final PropertyKey<String>      keyProperty     = new StringProperty("key");

	public abstract Class<R> getSiblingLinkType();
	
	/**
	 * Returns the predecessor of the given element in the list structure
	 * defined by this LinkedListManager.
	 *
	 * @param currentElement
	 * @return
	 */
	public  T listGetPrevious(final T currentElement) {

		R prevRel = currentElement.getIncomingRelationship(getSiblingLinkType());
		if (prevRel != null) {
			
			return prevRel.getSourceNode();
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
	public T listGetNext(final T currentElement) {

		R nextRel = currentElement.getOutgoingRelationship(getSiblingLinkType());
		if (nextRel != null) {
			
			return nextRel.getTargetNode();
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
	public void listInsertBefore(final T currentElement, final T newElement) throws FrameworkException {

		if (currentElement.getId() == newElement.getId()) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}

		final T previousElement = listGetPrevious(currentElement);
		if (previousElement == null) {

			// trivial: new node will become new head of existing list
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					linkNodes(getSiblingLinkType(), newElement, currentElement);

					return null;
				}
			});

		} else {

			// not-so-trivial: insert new element between predecessor and current node
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					// delete old relationship
					unlinkNodes(previousElement, currentElement);
					
					linkNodes(getSiblingLinkType(), previousElement, newElement);
					linkNodes(getSiblingLinkType(), newElement, currentElement);

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
	public void listInsertAfter(final T currentElement, final T newElement) throws FrameworkException {

		if (currentElement.getId() == newElement.getId()) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}

		final T next = listGetNext(currentElement);
		if (next == null) {

			// trivial: new node will become new tail of existing list
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					linkNodes(getSiblingLinkType(), currentElement, newElement);

					return null;
				}
			});

		} else {

			// not-so-trivial: insert new element between current node and successor
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					unlinkNodes(currentElement, next);

					linkNodes(getSiblingLinkType(), currentElement, newElement);
					linkNodes(getSiblingLinkType(), newElement, next);

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
	public void listRemove(final T currentElement) throws FrameworkException {
		
		final T previousElement = listGetPrevious(currentElement);
		final T nextElement     = listGetNext(currentElement);

		if (currentElement != null) {
			
			if (previousElement != null) {

				unlinkNodes(previousElement, currentElement);
			}

			if (nextElement != null) {

				unlinkNodes(currentElement, nextElement);
			}
		}

		if (previousElement != null && nextElement != null) {

			Node previousNode = previousElement.getNode();
			Node nextNode = nextElement.getNode();

			if (previousNode != null && nextNode != null) {

				linkNodes(getSiblingLinkType(), previousElement, nextElement);
			}

		}
	}
	
	public <R extends Relation<T, T, ?, ?>> void linkNodes(final Class<R> linkType, final T startNode, final T endNode) throws FrameworkException {
		linkNodes(linkType, startNode, endNode, null);
	}
	
	public <R extends Relation<T, T, ?, ?>> void linkNodes(final Class<R> linkType, final T startNode, final T endNode, final PropertyMap properties) throws FrameworkException {
		
		CreateRelationshipCommand cmd = Services.command(securityContext, CreateRelationshipCommand.class);
		
		// do not check for duplicates here
		cmd.execute(startNode, endNode, linkType, properties);
	}
	
	public void unlinkNodes(final NodeInterface startNode, final NodeInterface endNode) throws FrameworkException {
		
		final DeleteRelationshipCommand cmd = Services.command(securityContext, DeleteRelationshipCommand.class);
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				// FIXME: this is not complete!
				
				for (AbstractRelationship rel : startNode.getRelationships()) {
					
					if (rel != null && rel.getTargetNode().equals(endNode)) {
						cmd.execute(rel);
					}
				}
				
				return null;
			}
			
		});
	}
}
