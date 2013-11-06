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
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.relationship.AbstractListSiblings;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.RelationshipInterface;
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

		final App app = StructrApp.getInstance(securityContext);
		
		if (currentElement.getId() == newElement.getId()) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}

		final T previousElement = listGetPrevious(currentElement);
		if (previousElement == null) {

			try {
				app.beginTx();

				linkNodes(getSiblingLinkType(), newElement, currentElement);

				app.commitTx();

			} finally {

				app.finishTx();
			}

		} else {

			try {
				app.beginTx();

				// delete old relationship
				unlinkNodes(getSiblingLinkType(), previousElement, currentElement);

				linkNodes(getSiblingLinkType(), previousElement, newElement);
				linkNodes(getSiblingLinkType(), newElement, currentElement);

				app.commitTx();

			} finally {

				app.finishTx();
			}
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

		final App app = StructrApp.getInstance(securityContext);

		if (currentElement.getId() == newElement.getId()) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}

		final T next = listGetNext(currentElement);
		if (next == null) {

			try {
				app.beginTx();

				linkNodes(getSiblingLinkType(), currentElement, newElement);

				app.commitTx();

			} finally {

				app.finishTx();
			}

		} else {

			try {
				app.beginTx();

				unlinkNodes(getSiblingLinkType(), currentElement, next);

				linkNodes(getSiblingLinkType(), currentElement, newElement);
				linkNodes(getSiblingLinkType(), newElement, next);

				app.commitTx();

			} finally {

				app.finishTx();
			}
		}
	}

	/**
	 * Removes the current element from the list defined by this
	 * LinkedListManager.
	 *
	 * @param currentElement the element to be removed
	 */
	public void listRemove(final T currentElement) throws FrameworkException {
		
		final App app           = StructrApp.getInstance(securityContext);
		final T previousElement = listGetPrevious(currentElement);
		final T nextElement     = listGetNext(currentElement);

		if (currentElement != null) {
			
			try {
				app.beginTx();

				if (previousElement != null) {
					unlinkNodes(getSiblingLinkType(), previousElement, currentElement);
				}

				if (nextElement != null) {
					unlinkNodes(getSiblingLinkType(), currentElement, nextElement);
				}

				app.commitTx();

			} finally {

				app.finishTx();
			}
		}

		if (previousElement != null && nextElement != null) {

			Node previousNode = previousElement.getNode();
			Node nextNode = nextElement.getNode();

			if (previousNode != null && nextNode != null) {

				try {
					app.beginTx();
					linkNodes(getSiblingLinkType(), previousElement, nextElement);
					app.commitTx();

				} finally {

					app.finishTx();
				}
			}

		}
	}
	
	public <R extends Relation<T, T, ?, ?>> void linkNodes(final Class<R> linkType, final T startNode, final T endNode) throws FrameworkException {
		linkNodes(linkType, startNode, endNode, null);
	}
	
	public <R extends Relation<T, T, ?, ?>> void linkNodes(final Class<R> linkType, final T startNode, final T endNode, final PropertyMap properties) throws FrameworkException {
		
		final App app = StructrApp.getInstance(securityContext);
		
		try {
			app.beginTx();
			app.create(startNode, endNode, linkType, properties);
			app.commitTx();

		} finally {

			app.finishTx();
		}
	}
	
	private void unlinkNodes(final Class<R> linkType, final T startNode, final T endNode) throws FrameworkException {
		
		final App app = StructrApp.getInstance(securityContext);
		
		try {
			app.beginTx();

			for (RelationshipInterface rel : startNode.getRelationships(linkType)) {

				if (rel != null && rel.getTargetNode().equals(endNode)) {
					app.delete(rel);
				}
			}

			app.commitTx();

		} finally {

			app.finishTx();
		}
	}
}
