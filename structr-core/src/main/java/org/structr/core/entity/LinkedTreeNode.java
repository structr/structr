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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.relationship.AbstractChildren;
import org.structr.core.entity.relationship.AbstractListSiblings;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
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
	public static final PropertyKey<Integer> positionProperty    = new IntProperty("position");

	public abstract Class<R> getChildLinkType();
	
	public T treeGetParent(final Class<? extends AbstractChildren> type) {

		AbstractChildren prevRel = getIncomingRelationship(type);
		if (prevRel != null) {
			
			return (T)prevRel.getSourceNode();
		}

		return null;
	}

	public <R extends AbstractChildren<T, T>> void treeAppendChild(final Class<R> type, final T childElement) throws FrameworkException {
		
		final T lastChild = treeGetLastChild(type);
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				PropertyMap properties = new PropertyMap();
				properties.put(positionProperty, treeGetChildCount(type));

				// create child relationship
				linkNodes(getChildLinkType(), (T)LinkedTreeNode.this, childElement, properties);
				
				// add new node to linked list
				if (lastChild != null) {
					LinkedTreeNode.super.listInsertAfter(lastChild, childElement);
				}

				return null;
			}

		});
	}
	
	public <R extends AbstractChildren<T, T>>  void treeInsertBefore(final Class<R> type, final T newChild, final T refChild) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<R> rels = treeGetChildRelationships(type);
				int position = 0;

				for (R rel : rels) {

					T node = rel.getTargetNode();
					if (node.equals(refChild)) {

						// will be used only once here..
						PropertyMap properties = new PropertyMap();
						properties.put(positionProperty, position);
						
						linkNodes(getChildLinkType(), (T)LinkedTreeNode.this, newChild, properties);
						
						position++;
					}

					rel.setProperty(positionProperty, position);

					position++;
				}

				// insert new node in linked list
				LinkedTreeNode.super.listInsertBefore(refChild, newChild);

				
				return null;
			}

		});
	}
	
	public <R extends AbstractChildren<T, T>> void treeInsertAfter(final Class<R> type, final T newChild, final T refChild) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<R> rels = treeGetChildRelationships(type);
				int position = 0;

				for (R rel : rels) {

					T node = rel.getTargetNode();

					rel.setProperty(positionProperty, position);
					position++;

					if (node.equals(refChild)) {

						// will be used only once here..
						PropertyMap properties = new PropertyMap();
						properties.put(positionProperty, position);
						
						linkNodes(getChildLinkType(), (T)LinkedTreeNode.this, newChild, properties);
						
						position++;
					}
				}

				// insert new node in linked list
				LinkedTreeNode.super.listInsertAfter(refChild, newChild);
				
				return null;
			}

		});
	}

	public <R extends AbstractChildren<T, T>> void treeRemoveChild(final Class<R> type, final T childToRemove) throws FrameworkException {

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {
				
				// remove element from linked list
				LinkedTreeNode.super.listRemove(childToRemove);

				unlinkNodes(LinkedTreeNode.this, childToRemove);
				
				ensureCorrectChildPositions(type);

				return null;
			}

		});
	}
	
	public <R extends AbstractChildren<T, T>> void treeReplaceChild(final Class<R> type, final T newChild, final T oldChild) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				// save old position
				int oldPosition = treeGetChildPosition(type, oldChild);

				// remove old node
				unlinkNodes(LinkedTreeNode.this, oldChild);

				// insert new node with position from old node
				PropertyMap properties = new PropertyMap();
				properties.put(positionProperty, oldPosition);
				
				linkNodes(getChildLinkType(), (T)LinkedTreeNode.this, newChild, properties);

				// replace element in linked list as well
				LinkedTreeNode.super.listInsertBefore(oldChild, newChild);
				LinkedTreeNode.super.listRemove(oldChild);
				
				return null;
			}

		});
	}
	
	public <R extends AbstractChildren<T, T>> T treeGetFirstChild(final Class<R> type) {
		return treeGetChild(type, 0);
	}
	
	public <R extends AbstractChildren<T, T>> T treeGetLastChild(final Class<R> type) {
		
		int last = treeGetChildCount(type) - 1;
		if (last >= 0) {
			
			return treeGetChild(type, last);
		}
		
		return null;
	}
	
	public <R extends AbstractChildren<T, T>> T treeGetChild(final Class<R> type, final int position) {
		
		for (R rel : getOutgoingRelationships(type)) {
			
			Integer pos = rel.getProperty(positionProperty);
			
			if (pos != null && pos.intValue() == position) {
				
				return (T)rel.getTargetNode();
			}
		}

		
		return null;
	}
	
	public <R extends AbstractChildren<T, T>> int treeGetChildPosition(final Class<R> type, final T child) {
		
		final R rel = child.getIncomingRelationship(type);
		if (rel != null) {

			Integer pos = rel.getProperty(positionProperty);
			if (pos != null) {

				return pos.intValue();
			}
		}
		
		return 0;
	}
	
	public <R extends AbstractChildren<T, T>> List<T> treeGetChildren(final Class<R> type) {
		
		List<T> abstractChildren = new ArrayList<>();
		
		for (R rel : treeGetChildRelationships(type)) {
			
			abstractChildren.add(rel.getTargetNode());
		}
		
		return abstractChildren;
	}
	
	public <R extends AbstractChildren<T, T>> int treeGetChildCount(final Class<R> type) {
		return (int)Iterables.count(getOutgoingRelationships(type));
	}
	
	public <R extends AbstractChildren<T, T>> List<R> treeGetChildRelationships(final Class<R> type) {
		
		// fetch all relationships
		List<R> childRels = Iterables.toList(getOutgoingRelationships(type));
		
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
	 * Ensures that the position attributes of the AbstractChildren of this node
	 * are correct. Please note that this method needs to run in the same
	 * transaction as any modifiying operation that changes the order of
	 * child nodes, and therefore this method does _not_ create its own
	 * transaction. However, it will not raise a NotInTransactionException
	 * when called outside of modifying operations, because each setProperty
	 * call creates its own transaction.
	 */
	private <R extends AbstractChildren<T, T>> void ensureCorrectChildPositions(final Class<R> type) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<R> childRels = treeGetChildRelationships(type);
				int position      = 0;

				for (R childRel : childRels) {

					childRel.removeProperty(positionProperty);
					childRel.setProperty(positionProperty, position++);
				}

				return null;
			}

		});
	}
}
