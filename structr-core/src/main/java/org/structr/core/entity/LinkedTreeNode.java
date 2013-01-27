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
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
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

public abstract class LinkedTreeNode extends LinkedListNode {

	private static final String LIST_KEY_SUFFIX = "_NEXT_SIBLING";
	
	// this is not used for the node itself but for the relationship(s) this node maintains
	public static final PropertyKey<Integer> positionProperty = new IntProperty("position");
	
	protected LinkedTreeNode treeGetParent(final RelationshipType relType) {
		
		for (AbstractRelationship rel : getIncomingRelationships(relType)) {
			
			return (LinkedTreeNode)rel.getStartNode();
		}

		
		return null;
	}

	protected void treeAppendChild(final RelationshipType relType, final LinkedTreeNode childElement) throws FrameworkException {
		
		final LinkedTreeNode lastChild = treeGetLastChild(relType);
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				PropertyMap properties = new PropertyMap();
				properties.put(positionProperty, treeGetChildCount(relType));

				// create child relationship
				linkNodes(relType, LinkedTreeNode.this, childElement, properties);
				
				// add new node to linked list
				if (lastChild != null) {
					LinkedTreeNode.super.listInsertAfter(getListKey(relType), lastChild, childElement);
				}

				return null;
			}

		});
	}
	
	protected void treeInsertBefore(final RelationshipType relType, final LinkedTreeNode newChild, final LinkedTreeNode refChild) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<AbstractRelationship> rels = treeGetChildRelationships(relType);
				int position                    = 0;

				for (AbstractRelationship rel : rels) {

					AbstractNode node = rel.getEndNode();

					if (node.equals(refChild)) {

						// will be used only once here..
						PropertyMap properties = new PropertyMap();
						properties.put(positionProperty, position);
						
						linkNodes(relType, LinkedTreeNode.this, newChild, properties);
						
						position++;
					}

					rel.setProperty(positionProperty, position);

					position++;
				}

				// insert new node in linked list
				LinkedTreeNode.super.listInsertBefore(getListKey(relType), refChild, newChild);

				
				return null;
			}

		});
	}
	
	protected void treeInsertAfter(final RelationshipType relType, final LinkedTreeNode newChild, final LinkedTreeNode refChild) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<AbstractRelationship> rels = treeGetChildRelationships(relType);
				int position                    = 0;

				for (AbstractRelationship rel : rels) {

					AbstractNode node = rel.getEndNode();

					rel.setProperty(positionProperty, position);
					position++;

					if (node.equals(refChild)) {

						// will be used only once here..
						PropertyMap properties = new PropertyMap();
						properties.put(positionProperty, position);
						
						linkNodes(relType, LinkedTreeNode.this, newChild, properties);
						
						position++;
					}
				}

				// insert new node in linked list
				LinkedTreeNode.super.listInsertAfter(getListKey(relType), refChild, newChild);
				
				return null;
			}

		});
	}

	protected void treeRemoveChild(final RelationshipType relType, final LinkedTreeNode childToRemove) throws FrameworkException {

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {
				
				// remove element from linked list
				LinkedTreeNode.super.listRemove(getListKey(relType), childToRemove);

				unlinkNodes(relType, LinkedTreeNode.this, childToRemove);
				
				ensureCorrectChildPositions(relType);

				return null;
			}

		});
	}
	
	protected void treeReplaceChild(final RelationshipType relType, final LinkedTreeNode newChild, final LinkedTreeNode oldChild) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				// save old position
				int oldPosition = treeGetChildPosition(relType, oldChild);

				// remove old node
				unlinkNodes(relType, LinkedTreeNode.this, oldChild);

				// insert new node with position from old node
				PropertyMap properties = new PropertyMap();
				properties.put(positionProperty, oldPosition);
				
				linkNodes(relType, LinkedTreeNode.this, newChild, properties);

				// replace element in linked list as well
				LinkedTreeNode.super.listInsertBefore(getListKey(relType), oldChild, newChild);
				LinkedTreeNode.super.listRemove(getListKey(relType), oldChild);
				
				return null;
			}

		});
	}
	
	protected LinkedTreeNode treeGetFirstChild(final RelationshipType relType) {
		return treeGetChild(relType, 0);
	}
	
	protected LinkedTreeNode treeGetLastChild(final RelationshipType relType) {
		
		int last = treeGetChildCount(relType) - 1;
		if (last >= 0) {
			
			return treeGetChild(relType, last);
		}
		
		return null;
	}
	
	protected LinkedTreeNode treeGetChild(final RelationshipType relType, final int position) {
		
		for (AbstractRelationship rel : getOutgoingRelationships(relType)) {
			
			Integer pos = rel.getProperty(positionProperty);
			
			if (pos != null && pos.intValue() == position) {
				
				return (LinkedTreeNode)rel.getEndNode();
			}
		}

		
		return null;
	}
	
	protected int treeGetChildPosition(final RelationshipType relType, final LinkedTreeNode child) {
		
		List<AbstractRelationship> rels = child.getIncomingRelationships(relType);
		if (rels != null && rels.size() == 1) {
			
			// node should have only one parent
			AbstractRelationship rel = rels.get(0);
			
			Integer pos = rel.getProperty(positionProperty);
			if (pos != null) {
				
				return pos.intValue();
			}
		}
		
		return -1;
	}
	
	protected List<LinkedTreeNode> treeGetChildren(final RelationshipType relType) {
		
		List<LinkedTreeNode> children = new ArrayList<LinkedTreeNode>();
		
		for (AbstractRelationship rel : treeGetChildRelationships(relType)) {
			
			children.add((LinkedTreeNode)rel.getEndNode());
		}
		
		return children;
	}
	
	protected int treeGetChildCount(final RelationshipType relType) {
		return getOutgoingRelationships(relType).size();
	}
	
	protected List<AbstractRelationship> treeGetChildRelationships(final RelationshipType relType) {
		
		// fetch all relationships
		List<AbstractRelationship> childRels = getOutgoingRelationships(relType);
		
		// sort relationships by position
		Collections.sort(childRels, new Comparator<AbstractRelationship>() {

			@Override
			public int compare(AbstractRelationship o1, AbstractRelationship o2) {

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
	private void ensureCorrectChildPositions(final RelationshipType relType) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<AbstractRelationship> childRels = treeGetChildRelationships(relType);
				int position                         = 0;

				for (AbstractRelationship childRel : childRels) {

					childRel.removeProperty(positionProperty);
					childRel.setProperty(positionProperty, position++);
				}

				return null;
			}

		});
	}
	
	/**
	 * Creates a list key from the give tree key by appending a fixed string
	 */
	protected RelationshipType getListKey(RelationshipType treeRel) {
		return DynamicRelationshipType.withName(treeRel.name().concat(LIST_KEY_SUFFIX));
	}
}
