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

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyMap;

import java.util.*;

/**
 * Abstract base class for a multi-dimensional ordered tree datastructure.
 */
public abstract class LinkedTreeNodeImpl<T extends NodeInterface> extends LinkedListNodeImpl<T> implements LinkedTreeNode<T> {

	@Override
	public T treeGetParent() {

		Relation prevRel = getIncomingRelationship(getChildLinkType());
		if (prevRel != null) {

			return (T) prevRel.getSourceNode();
		}

		return null;
	}

	@Override
	public void treeAppendChild(final T childElement) throws FrameworkException {

		final T lastChild = treeGetLastChild();

		PropertyMap properties = new PropertyMap();
		properties.put(getPositionProperty(), treeGetChildCount());

		// create child relationship
		linkNodes(getChildLinkType(), (T)this, childElement, properties);

		// add new node to linked list
		if (lastChild != null) {
			listInsertAfter(lastChild, childElement);
		}

		ensureCorrectChildPositions();
	}

	@Override
	public void treeInsertBefore(final T newChild, final T refChild) throws FrameworkException {

		final List<Relation<T, T, OneStartpoint<T>, ManyEndpoint<T>>> rels = treeGetChildRelationships();
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

		for (Relation<T, T, OneStartpoint<T>, ManyEndpoint<T>> rel : rels) {

			T node = (T)rel.getTargetNode();
			if (node.equals(refChild)) {

				// will be used only once here..
				PropertyMap properties = new PropertyMap();
				properties.put(getPositionProperty(), position);

				linkNodes(getChildLinkType(), (T)this, newChild, properties);

				found = true;

				position++;
			}

			rel.setProperty(getPositionProperty(), position);

			position++;
		}

		// if child is not found, raise an exception
		if (!found) {
			throw new FrameworkException(404, "Referenced child is not a child of parent node.");
		}

		// insert new node in linked list
		listInsertBefore(refChild, newChild);

		ensureCorrectChildPositions();
	}

	@Override
	public void treeInsertAfter(final T newChild, final T refChild) throws FrameworkException {

		final List<Relation<T, T, OneStartpoint<T>, ManyEndpoint<T>>> rels = treeGetChildRelationships();
		int position = 0;

		// when there are no child rels, this is an append operation
		if (rels.isEmpty()) {

			treeAppendChild(newChild);
			return;
		}

		for (Relation<T, T, OneStartpoint<T>, ManyEndpoint<T>> rel : rels) {

			T node = (T)rel.getTargetNode();

			rel.setProperty(getPositionProperty(), position);
			position++;

			if (node.equals(refChild)) {

				// will be used only once here..
				PropertyMap properties = new PropertyMap();
				properties.put(getPositionProperty(), position);

				linkNodes(getChildLinkType(), (T)this, newChild, properties);

				position++;
			}
		}

		// insert new node in linked list
		listInsertAfter(refChild, newChild);

		ensureCorrectChildPositions();

	}

	@Override
	public void treeRemoveChild(final T childToRemove) throws FrameworkException {

		// remove element from linked list
		listRemove(childToRemove);

		unlinkNodes(getChildLinkType(), (T)this, childToRemove);

		ensureCorrectChildPositions();

	}

	@Override
	public void treeReplaceChild(final T newChild, final T oldChild) throws FrameworkException {

		// save old position
		int oldPosition = treeGetChildPosition(oldChild);

		// remove old node
		unlinkNodes(getChildLinkType(), (T)this, oldChild);

		// insert new node with position from old node
		PropertyMap properties = new PropertyMap();

		properties.put(getPositionProperty(), oldPosition);

		linkNodes(getChildLinkType(), (T)this, newChild, properties);

		// replace element in linked list as well
		listInsertBefore(oldChild, newChild);
		listRemove(oldChild);

		ensureCorrectChildPositions();
	}

	@Override
	public T treeGetFirstChild() {
		return treeGetChild(0);
	}

	@Override
	public T treeGetLastChild() {

		int last = treeGetChildCount() - 1;
		if (last >= 0) {

			return treeGetChild(last);
		}

		return null;
	}

	@Override
	public T treeGetChild(final int position) {

		for (Relation<T, T, OneStartpoint<T>, ManyEndpoint<T>> rel : getOutgoingRelationships(getChildLinkType())) {

			Integer pos = rel.getProperty(getPositionProperty());

			if (pos != null && pos == position) {

				return (T) rel.getTargetNode();
			}
		}

		return null;
	}

	@Override
	public int treeGetChildPosition(final T child) {

		final Relation<T, T, OneStartpoint<T>, ManyEndpoint<T>> rel = child.getIncomingRelationship(getChildLinkType());
		if (rel != null) {

			Integer pos = rel.getProperty(getPositionProperty());
			if (pos != null) {

				return pos;
			}
		}

		return 0;
	}

	@Override
	public List<T> treeGetChildren() {

		List<T> abstractChildren = new ArrayList<>();

		for (Relation<T, T, OneStartpoint<T>, ManyEndpoint<T>> rel : treeGetChildRelationships()) {

			abstractChildren.add((T)rel.getTargetNode());
		}

		return abstractChildren;
	}

	@Override
	public int treeGetChildCount() {
		return (int)Iterables.count(getOutgoingRelationships(getChildLinkType()));
	}

	@Override
	public <R extends Relation<T, T, OneStartpoint<T>, ManyEndpoint<T>>> List<R> treeGetChildRelationships() {

		// fetch all relationships
		List<R> childRels = Iterables.toList(getOutgoingRelationships(getChildLinkType()));

		// sort relationships by position
		Collections.sort(childRels, new Comparator<R>() {

			@Override
			public int compare(R o1, R o2) {

				Integer pos1 = o1.getProperty(getPositionProperty());
				Integer pos2 = o2.getProperty(getPositionProperty());

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

		final List<Relation<T, T, OneStartpoint<T>, ManyEndpoint<T>>> childRels = treeGetChildRelationships();
		int position = 0;

		for (Relation<T, T, OneStartpoint<T>, ManyEndpoint<T>> childRel : childRels) {
			childRel.setProperty(getPositionProperty(), position++);
		}
	}

	private <R extends Relation<T, T, OneStartpoint<T>, ManyEndpoint<T>>> void unlinkNodes(final Class<R> linkType, final T startNode, final T endNode) throws FrameworkException {

		final App app      = StructrApp.getInstance(securityContext);
		final List<R> list = Iterables.toList(startNode.getRelationships(linkType));

		for (RelationshipInterface rel : list) {

			if (rel != null && rel.getTargetNode().equals(endNode)) {
				app.delete(rel);
			}
		}
	}

	/**
	 * Return a set containing all child nodes of this node.
	 *
	 * This node is not included.
	 * There are no duplicates.
	 *
	 * @return child nodes
	 */
	@Override
	public Set<T> getAllChildNodes() {

		Set<T> allChildNodes = new HashSet();

		List<T> childNodes = treeGetChildren();

		for (final T child : childNodes) {

			allChildNodes.add(child);

			if (child instanceof LinkedTreeNode) {

				final LinkedTreeNode treeNode = (LinkedTreeNode)child;
				allChildNodes.addAll(treeNode.getAllChildNodes());
			}
		}

		return allChildNodes;
	}
}
