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
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.relationship.ListSibling;
import org.structr.core.entity.relationship.TreeChild;
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

public abstract class LinkedTreeNode<T extends LinkedTreeNode> extends LinkedListNode {

	public static final String LIST_KEY_SUFFIX = "_NEXT_SIBLING";
	
	// this is not used for the node itself but for the relationship(s) this node maintains
	public static final PropertyKey<Integer> positionProperty = new IntProperty("position");
	
	public T treeGetParent(final Class<? extends TreeChild> type) {

		TreeChild prevRel = getIncomingRelationship(type);
		if (prevRel != null) {
			
			return (T)prevRel.getStartNode();
		}

		return null;
	}

	public void treeAppendChild(final Class<? extends TreeChild> type, final T childElement) throws FrameworkException {
		
		final T lastChild = treeGetLastChild(type);
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				PropertyMap properties = new PropertyMap();
				properties.put(positionProperty, treeGetChildCount(type));

				// create child relationship
				linkNodes(ListSibling.class, LinkedTreeNode.this, childElement, properties);
				
				// add new node to linked list
				if (lastChild != null) {
					LinkedTreeNode.super.listInsertAfter(ListSibling.class, lastChild, childElement);
				}

				return null;
			}

		});
	}
	
	public void treeInsertBefore(final Class<? extends TreeChild> type, final T newChild, final T refChild) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<? extends TreeChild> rels = treeGetChildRelationships(type);
				int position                    = 0;

				for (TreeChild rel : rels) {

					AbstractNode node = rel.getEndNode();

					if (node.equals(refChild)) {

						// will be used only once here..
						PropertyMap properties = new PropertyMap();
						properties.put(positionProperty, position);
						
						linkNodes(type, LinkedTreeNode.this, newChild, properties);
						
						position++;
					}

					rel.setProperty(positionProperty, position);

					position++;
				}

				// insert new node in linked list
				LinkedTreeNode.super.listInsertBefore(ListSibling.class, refChild, newChild);

				
				return null;
			}

		});
	}
	
	public void treeInsertAfter(final Class<? extends TreeChild> type, final T newChild, final T refChild) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<? extends TreeChild> rels = treeGetChildRelationships(type);
				int position                    = 0;

				for (TreeChild rel : rels) {

					AbstractNode node = rel.getEndNode();

					rel.setProperty(positionProperty, position);
					position++;

					if (node.equals(refChild)) {

						// will be used only once here..
						PropertyMap properties = new PropertyMap();
						properties.put(positionProperty, position);
						
						linkNodes(type, LinkedTreeNode.this, newChild, properties);
						
						position++;
					}
				}

				// insert new node in linked list
				LinkedTreeNode.super.listInsertAfter(ListSibling.class, refChild, newChild);
				
				return null;
			}

		});
	}

	public void treeRemoveChild(final Class<? extends TreeChild> type, final T childToRemove) throws FrameworkException {

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {
				
				// remove element from linked list
				LinkedTreeNode.super.listRemove(ListSibling.class, childToRemove);

				unlinkNodes(type, LinkedTreeNode.this, childToRemove);
				
				ensureCorrectChildPositions(type);

				return null;
			}

		});
	}
	
	public void treeReplaceChild(final Class<? extends TreeChild> type, final T newChild, final T oldChild) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				// save old position
				int oldPosition = treeGetChildPosition(type, oldChild);

				// remove old node
				unlinkNodes(type, LinkedTreeNode.this, oldChild);

				// insert new node with position from old node
				PropertyMap properties = new PropertyMap();
				properties.put(positionProperty, oldPosition);
				
				linkNodes(type, LinkedTreeNode.this, newChild, properties);

				// replace element in linked list as well
				LinkedTreeNode.super.listInsertBefore(ListSibling.class, oldChild, newChild);
				LinkedTreeNode.super.listRemove(ListSibling.class, oldChild);
				
				return null;
			}

		});
	}
	
	public T treeGetFirstChild(final Class<TreeChild> type) {
		return treeGetChild(type, 0);
	}
	
	public T treeGetLastChild(final Class<? extends TreeChild> type) {
		
		int last = treeGetChildCount(type) - 1;
		if (last >= 0) {
			
			return treeGetChild(type, last);
		}
		
		return null;
	}
	
	public T treeGetChild(final Class<? extends TreeChild> type, final int position) {
		
		for (TreeChild rel : getOutgoingRelationships(type)) {
			
			Integer pos = rel.getProperty(positionProperty);
			
			if (pos != null && pos.intValue() == position) {
				
				return (T)rel.getEndNode();
			}
		}

		
		return null;
	}
	
	public int treeGetChildPosition(final Class<? extends TreeChild> type, final T child) {
		
		TreeChild rel = child.getIncomingRelationship(type);

		Integer pos = rel.getProperty(positionProperty);
		if (pos != null) {

			return pos.intValue();
		}
		
		return -1;
	}
	
	public List<T> treeGetChildren(final Class<? extends TreeChild> type) {
		
		List<T> children = new ArrayList<>();
		
		for (TreeChild rel : treeGetChildRelationships(type)) {
			
			children.add((T)rel.getEndNode());
		}
		
		return children;
	}
	
	public int treeGetChildCount(final Class<? extends TreeChild> type) {
		return (int)Iterables.count(getOutgoingRelationships(type));
	}
	
	public List<? extends TreeChild> treeGetChildRelationships(final Class<? extends TreeChild> type) {
		
		// fetch all relationships
		List<? extends TreeChild> childRels = Iterables.toList(getOutgoingRelationships(type));
		
		// sort relationships by position
		Collections.sort(childRels, new Comparator<TreeChild>() {

			@Override
			public int compare(TreeChild o1, TreeChild o2) {

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
	 * Ensures that the position attributes of the children of this node
	 * are correct. Please note that this method needs to run in the same
	 * transaction as any modifiying operation that changes the order of
	 * child nodes, and therefore this method does _not_ create its own
	 * transaction. However, it will not raise a NotInTransactionException
	 * when called outside of modifying operations, because each setProperty
	 * call creates its own transaction.
	 */
	private void ensureCorrectChildPositions(final Class<? extends TreeChild> type) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<? extends TreeChild> childRels = treeGetChildRelationships(type);
				int position                         = 0;

				for (TreeChild childRel : childRels) {

					childRel.removeProperty(positionProperty);
					childRel.setProperty(positionProperty, position++);
				}

				return null;
			}

		});
	}
}
