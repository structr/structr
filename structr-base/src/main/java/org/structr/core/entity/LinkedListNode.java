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
package org.structr.core.entity;

import org.structr.api.graph.Node;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyMap;

/**
 * Abstract base class for a multi-dimensional linked list data structure.
 *
 *
 * @param <T>
 */
public interface LinkedListNode<T extends NodeInterface> extends NodeInterface {

	<R extends Relation<T, T, OneStartpoint<T>, OneEndpoint<T>>> Class<R> getSiblingLinkType();

	/**
	 * Returns the predecessor of the given element in the list structure
	 * defined by this LinkedListManager.
	 *
	 * @param currentElement
	 * @return previous element
	 */
	default T listGetPrevious(final T currentElement) {

		Relation<T, T, OneStartpoint<T>, OneEndpoint<T>> prevRel = currentElement.getIncomingRelationship(getSiblingLinkType());
		if (prevRel != null) {

			return (T)prevRel.getSourceNode();
		}

		return null;
	}

	/**
	 * Returns the successor of the given element in the list structure
	 * defined by this LinkedListManager.
	 *
	 * @param currentElement
	 * @return next element
	 */
	default T listGetNext(final T currentElement) {

		Relation<T, T, OneStartpoint<T>, OneEndpoint<T>> nextRel = currentElement.getOutgoingRelationship(getSiblingLinkType());
		if (nextRel != null) {

			return (T)nextRel.getTargetNode();
		}

		return null;
	}

	/**
	 * Inserts newElement before currentElement in the list defined by this
	 * LinkedListManager.
	 *
	 * @param currentElement the reference element
	 * @param newElement the new element
	 * @throws org.structr.common.error.FrameworkException
	 */
	default void listInsertBefore(final T currentElement, final T newElement) throws FrameworkException {

		if (currentElement.getUuid().equals(newElement.getUuid())) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}

		final T previousElement = listGetPrevious(currentElement);
		if (previousElement == null) {

			linkSiblings(newElement, currentElement);

		} else {
			// delete old relationship
			unlinkSiblings(previousElement, currentElement);

			// dont create self link
			if (!previousElement.getUuid().equals(newElement.getUuid())) {
				linkSiblings(previousElement, newElement);
			}

			// dont create self link
			if (!newElement.getUuid().equals(currentElement.getUuid())) {
				linkSiblings(newElement, currentElement);
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
	default void listInsertAfter(final T currentElement, final T newElement) throws FrameworkException {
		if (currentElement.getUuid().equals(newElement.getUuid())) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}

		final T next = listGetNext(currentElement);
		if (next == null) {

			linkSiblings(currentElement, newElement);

		} else {

			// unlink predecessor and successor
			unlinkSiblings(currentElement, next);

			// link predecessor to new element
			linkSiblings(currentElement, newElement);

			// dont create self link
			if (!newElement.getUuid().equals(next.getUuid())) {

				// link new element to successor
				linkSiblings(newElement, next);
			}
		}
	}

	/**
	 * Removes the current element from the list defined by this
	 * LinkedListManager.
	 *
	 * @param currentElement the element to be removed
	 */
	default void listRemove(final T currentElement) throws FrameworkException {

		final T previousElement = listGetPrevious(currentElement);
		final T nextElement     = listGetNext(currentElement);

		if (currentElement != null) {

			if (previousElement != null) {
				unlinkSiblings(previousElement, currentElement);
			}

			if (nextElement != null) {
				unlinkSiblings(currentElement, nextElement);
			}
		}

		if (previousElement != null && nextElement != null) {

			Node previousNode = previousElement.getNode();
			Node nextNode     = nextElement.getNode();

			if (previousNode != null && nextNode != null) {

				linkSiblings(previousElement, nextElement);
			}

		}
	}

	default void linkSiblings(final T startNode, final T endNode) throws FrameworkException {
		linkSiblings(startNode, endNode, null);
	}

	default void linkSiblings(final T startNode, final T endNode, final PropertyMap properties) throws FrameworkException {
		StructrApp.getInstance(getSecurityContext()).create(startNode, endNode, getSiblingLinkType(), properties);
	}

	private void unlinkSiblings(final T startNode, final T endNode) throws FrameworkException {

		final App app = StructrApp.getInstance(getSecurityContext());

		for (RelationshipInterface rel : startNode.getRelationships(getSiblingLinkType())) {

			if (rel != null && rel.getTargetNode().equals(endNode)) {
				app.delete(rel);
			}
		}
	}
}
