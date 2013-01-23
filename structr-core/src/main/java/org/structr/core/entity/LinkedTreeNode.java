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
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 * Abstract base class for an ordered tree datastructure.
 * 
 * @author Christian Morgner
 */

public abstract class LinkedTreeNode extends LinkedListNode {

	// this is not used for the node itself but for the relationship(s) this node maintains
	public static final PropertyKey<Integer> positionProperty = new IntProperty("position");

	protected void treeAppendChild(final CollectionProperty<? extends LinkedTreeNode> treeProperty, final CollectionProperty<? extends LinkedListNode> listProperty, final LinkedTreeNode childElement) throws FrameworkException {
		
		final LinkedTreeNode lastChild = treeGetLastChild(treeProperty);
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				PropertyMap properties = new PropertyMap();
				properties.put(positionProperty, treeGetChildCount(treeProperty));

				// create child relationship
				treeProperty.createRelationship(securityContext, LinkedTreeNode.this, childElement, properties);
				
				// add new node to linked list
				if (lastChild != null) {
					LinkedTreeNode.super.listInsertAfter(listProperty, lastChild, childElement);
				}

				return null;
			}

		});
	}
	
	protected void treeInsertBefore(final CollectionProperty<? extends LinkedTreeNode> treeProperty, final CollectionProperty<? extends LinkedListNode> listProperty, final LinkedTreeNode newChild, final LinkedTreeNode refChild) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<AbstractRelationship> rels = treeGetChildRelationships(treeProperty);
				int position                    = 0;

				for (AbstractRelationship rel : rels) {

					AbstractNode node = rel.getEndNode();

					if (node.equals(refChild)) {

						// will be used only once here..
						PropertyMap properties = new PropertyMap();
						properties.put(positionProperty, position);
						treeProperty.createRelationship(securityContext, LinkedTreeNode.this, newChild, properties);
						
						position++;
					}

					rel.setProperty(positionProperty, position);

					position++;
				}

				// insert new node in linked list
				LinkedTreeNode.super.listInsertBefore(listProperty, refChild, newChild);

				
				return null;
			}

		});
	}
	
	protected void treeInsertAfter(final CollectionProperty<? extends LinkedTreeNode> treeProperty, final CollectionProperty<? extends LinkedListNode> listProperty, final LinkedTreeNode newChild, final LinkedTreeNode refChild) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<AbstractRelationship> rels = treeGetChildRelationships(treeProperty);
				int position                    = 0;

				for (AbstractRelationship rel : rels) {

					AbstractNode node = rel.getEndNode();

					rel.setProperty(positionProperty, position);
					position++;

					if (node.equals(refChild)) {

						// will be used only once here..
						PropertyMap properties = new PropertyMap();
						properties.put(positionProperty, position);
						treeProperty.createRelationship(securityContext, LinkedTreeNode.this, newChild, properties);
						
						position++;
					}
				}

				// insert new node in linked list
				LinkedTreeNode.super.listInsertAfter(listProperty, refChild, newChild);
				
				return null;
			}

		});
	}

	protected void treeRemoveChild(final CollectionProperty<? extends LinkedTreeNode> treeProperty, final CollectionProperty<? extends LinkedListNode> listProperty, final LinkedTreeNode childToRemove) throws FrameworkException {

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {
				
				// remove element from linked list
				LinkedTreeNode.super.listRemove(listProperty, childToRemove);

				treeProperty.removeRelationship(securityContext, LinkedTreeNode.this, childToRemove);
				
				ensureCorrectChildPositions(treeProperty);

				return null;
			}

		});
	}
	
	protected void treeReplaceChild(final CollectionProperty<? extends LinkedTreeNode> treeProperty, final CollectionProperty<? extends LinkedListNode> listProperty, final LinkedTreeNode newChild, final LinkedTreeNode oldChild) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				// save old position
				int oldPosition = treeGetChildPosition(treeProperty, oldChild);

				// remove old node
				treeProperty.removeRelationship(securityContext, LinkedTreeNode.this, oldChild);

				// insert new node with position from old node
				PropertyMap properties = new PropertyMap();
				properties.put(positionProperty, oldPosition);
				treeProperty.createRelationship(securityContext, LinkedTreeNode.this, newChild, properties);

				// replace element in linked list as well
				LinkedTreeNode.super.listInsertBefore(listProperty, oldChild, newChild);
				LinkedTreeNode.super.listRemove(listProperty, oldChild);
				
				return null;
			}

		});
	}
	
	protected LinkedTreeNode treeGetFirstChild(final CollectionProperty<? extends LinkedTreeNode> treeProperty) {
		return treeGetChild(treeProperty, 0);
	}
	
	protected LinkedTreeNode treeGetLastChild(final CollectionProperty<? extends LinkedTreeNode> treeProperty) {
		
		int last = treeGetChildCount(treeProperty) - 1;
		if (last >= 0) {
			
			return treeGetChild(treeProperty, last);
		}
		
		return null;
	}
	
	protected LinkedTreeNode treeGetChild(final CollectionProperty<? extends LinkedTreeNode> treeProperty, final int position) {
		
		final RelationshipType treeRelationship = treeProperty.getRelType();
		
		for (AbstractRelationship rel : getOutgoingRelationships(treeRelationship)) {
			
			Integer pos = rel.getProperty(positionProperty);
			
			if (pos != null && pos.intValue() == position) {
				
				return (LinkedTreeNode)rel.getEndNode();
			}
		}

		
		return null;
	}
	
	protected int treeGetChildPosition(final CollectionProperty<? extends LinkedTreeNode> treeProperty, final LinkedTreeNode child) {
		
		final RelationshipType treeRelationship = treeProperty.getRelType();
		
		List<AbstractRelationship> rels = child.getIncomingRelationships(treeRelationship);
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
	
	protected List<LinkedTreeNode> treeGetChildren(final CollectionProperty<? extends LinkedTreeNode> treeProperty) {
		
		List<LinkedTreeNode> children = new ArrayList<LinkedTreeNode>();
		
		for (AbstractRelationship rel : treeGetChildRelationships(treeProperty)) {
			
			children.add((LinkedTreeNode)rel.getEndNode());
		}
		
		return children;
	}
	
	protected int treeGetChildCount(final CollectionProperty<? extends LinkedTreeNode> treeProperty) {
		
		final RelationshipType treeRelationship = treeProperty.getRelType();

		return getOutgoingRelationships(treeRelationship).size();
	}
	
	protected List<AbstractRelationship> treeGetChildRelationships(final CollectionProperty<? extends LinkedTreeNode> treeProperty) {
		
		final RelationshipType treeRelationship = treeProperty.getRelType();

		// fetch all relationships
		List<AbstractRelationship> childRels = getOutgoingRelationships(treeRelationship);
		
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
	private void ensureCorrectChildPositions(final CollectionProperty<? extends LinkedTreeNode> treeProperty) throws FrameworkException {
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<AbstractRelationship> childRels = treeGetChildRelationships(treeProperty);
				int position                         = 0;

				for (AbstractRelationship childRel : childRels) {

					childRel.removeProperty(positionProperty);
					childRel.setProperty(positionProperty, position++);
				}

				return null;
			}

		});
	}

}
