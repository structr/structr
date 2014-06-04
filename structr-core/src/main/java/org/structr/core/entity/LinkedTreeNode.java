/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.relationship.AbstractChildren;
import org.structr.core.entity.relationship.AbstractListSiblings;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.IntProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 * Abstract base class for a multi-dimensional ordered tree datastructure.
 *
 * @author Christian Morgner
 */
public abstract class LinkedTreeNode<R extends AbstractChildren<T, T>, S extends AbstractListSiblings<T, T>, T extends LinkedTreeNode> extends LinkedListNode<S, T> {

	// this is not used for the node itself but for the relationship(s) this node maintains
	public static final PropertyKey<Integer> positionProperty = new IntProperty("position");

	public abstract Class<R> getChildLinkType();

	public T treeGetParent() {

		AbstractChildren prevRel = getIncomingRelationship(getChildLinkType());
		if (prevRel != null) {

			return (T) prevRel.getSourceNode();
		}

		return null;
	}

	public void treeAppendChild(final T childElement) throws FrameworkException {

		final T lastChild = treeGetLastChild();

		PropertyMap properties = new PropertyMap();
		properties.put(positionProperty, treeGetChildCount());

		// create child relationship
		linkNodes(getChildLinkType(), (T) LinkedTreeNode.this, childElement, properties);

		// add new node to linked list
		if (lastChild != null) {
			LinkedTreeNode.super.listInsertAfter(lastChild, childElement);
		}
	}

	public void treeInsertBefore(final T newChild, final T refChild) throws FrameworkException {

		List<R> rels = treeGetChildRelationships();
		boolean found = false;
		int position = 0;

		// when there are no child rels, this is an append operation
		if (rels.isEmpty()) {

			// we have no children, but the ref child is non-null => can't be ours.. :)
			if (refChild != null) {
				throw new FrameworkException(404, "Referenced child is not a child of parent node.");
			}

			treeAppendChild(newChild);
			return;
		}

		for (R rel : rels) {

			T node = rel.getTargetNode();
			if (node.equals(refChild)) {

				// will be used only once here..
				PropertyMap properties = new PropertyMap();
				properties.put(positionProperty, position);

				linkNodes(getChildLinkType(), (T) LinkedTreeNode.this, newChild, properties);

				found = true;

				position++;
			}

			rel.setProperty(positionProperty, position);

			position++;
		}

		// if child is not found, raise an exception
		if (!found) {
			throw new FrameworkException(404, "Referenced child is not a child of parent node.");
		}

		// insert new node in linked list
		LinkedTreeNode.super.listInsertBefore(refChild, newChild);
	}

	public void treeInsertAfter(final T newChild, final T refChild) throws FrameworkException {

		List<R> rels = treeGetChildRelationships();
		int position = 0;

		// when there are no child rels, this is an append operation
		if (rels.isEmpty()) {

			treeAppendChild(newChild);
			return;
		}

		for (R rel : rels) {

			T node = rel.getTargetNode();

			rel.setProperty(positionProperty, position);
			position++;

			if (node.equals(refChild)) {

				// will be used only once here..
				PropertyMap properties = new PropertyMap();
				properties.put(positionProperty, position);

				linkNodes(getChildLinkType(), (T) LinkedTreeNode.this, newChild, properties);

				position++;
			}
		}

		// insert new node in linked list
		LinkedTreeNode.super.listInsertAfter(refChild, newChild);

	}

	public void treeRemoveChild(final T childToRemove) throws FrameworkException {

		// remove element from linked list
		LinkedTreeNode.super.listRemove(childToRemove);

		unlinkNodes(getChildLinkType(), (T) LinkedTreeNode.this, childToRemove);

		ensureCorrectChildPositions();

	}

	public void treeReplaceChild(final T newChild, final T oldChild) throws FrameworkException {

		// save old position
		int oldPosition = treeGetChildPosition(oldChild);

		// remove old node
		unlinkNodes(getChildLinkType(), (T) LinkedTreeNode.this, oldChild);

		// insert new node with position from old node
		PropertyMap properties = new PropertyMap();
		properties.put(positionProperty, oldPosition);

		linkNodes(getChildLinkType(), (T) LinkedTreeNode.this, newChild, properties);

		// replace element in linked list as well
		LinkedTreeNode.super.listInsertBefore(oldChild, newChild);
		LinkedTreeNode.super.listRemove(oldChild);
	}

	public T treeGetFirstChild() {
		return treeGetChild(0);
	}

	public T treeGetLastChild() {

		int last = treeGetChildCount() - 1;
		if (last >= 0) {

			return treeGetChild(last);
		}

		return null;
	}

	public T treeGetChild(final int position) {

		for (R rel : getOutgoingRelationships(getChildLinkType())) {

			Integer pos = rel.getProperty(positionProperty);

			if (pos != null && pos == position) {

				return (T) rel.getTargetNode();
			}
		}

		return null;
	}

	public int treeGetChildPosition(final T child) {

		final R rel = child.getIncomingRelationship(getChildLinkType());
		if (rel != null) {

			Integer pos = rel.getProperty(positionProperty);
			if (pos != null) {

				return pos;
			}
		}

		return 0;
	}

	public List<T> treeGetChildren() {

		List<T> abstractChildren = new ArrayList<>();

		for (R rel : treeGetChildRelationships()) {

			abstractChildren.add(rel.getTargetNode());
		}

		return abstractChildren;
	}

	public int treeGetChildCount() {
		return (int) Iterables.count(getOutgoingRelationships(getChildLinkType()));
	}

	public List<R> treeGetChildRelationships() {

		// fetch all relationships
		List<R> childRels = Iterables.toList(getOutgoingRelationships(getChildLinkType()));

		// sort relationships by position
		Collections.sort(childRels, new Comparator<R>() {

			@Override
			public int compare(R o1, R o2) {

				Integer pos1 = o1.getProperty(positionProperty);
				Integer pos2 = o2.getProperty(positionProperty);

				if (pos1 != null && pos2 != null) {

					return pos1.compareTo(pos2);
				}

				return 0;
			}

		});

		return childRels;
	}

	/**
	 * Ensures that the position attributes of the AbstractChildren of this
	 * node are correct. Please note that this method needs to run in the
	 * same transaction as any modifiying operation that changes the order
	 * of child nodes, and therefore this method does _not_ create its own
	 * transaction. However, it will not raise a NotInTransactionException
	 * when called outside of modifying operations, because each setProperty
	 * call creates its own transaction.
	 */
	private void ensureCorrectChildPositions() throws FrameworkException {

		List<R> childRels = treeGetChildRelationships();
		int position = 0;

		for (R childRel : childRels) {
			childRel.setProperty(positionProperty, position++);
		}
	}

	private void unlinkNodes(final Class<R> linkType, final T startNode, final T endNode) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);

		for (RelationshipInterface rel : startNode.getRelationships(linkType)) {

			if (rel != null && rel.getTargetNode().equals(endNode)) {
				app.delete(rel);
			}
		}
	}
}
