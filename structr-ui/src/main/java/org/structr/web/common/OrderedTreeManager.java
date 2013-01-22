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
package org.structr.web.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 * Utility class that provides methods to create, access and manage ordered
 * tree structures on top of existing nodes.
 *
 * @author Christian Morgner
 */
public class OrderedTreeManager<T extends AbstractNode> {
	
	private CollectionProperty<T> collectionKey = null;
	private PropertyKey<Integer> positionKey    = null;
	private LinkedListManager linkedListManager = null;
	private RelationshipType treeRelationship   = null;
	
	public OrderedTreeManager(CollectionProperty<T> collectionKey, PropertyKey<Integer> positionKey, RelationshipType treeRelationship, RelationshipType listRelationship) {
		
		this.linkedListManager = new LinkedListManager(listRelationship);
		this.treeRelationship  = treeRelationship;
		this.collectionKey     = collectionKey;
		this.positionKey       = positionKey;
	}
	
	public void appendChild(final T treeElement, final T childElement) throws FrameworkException {
		
		final SecurityContext securityContext = treeElement.getSecurityContext();
		final T lastChild                     = getLastChild(treeElement);
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				PropertyMap properties = new PropertyMap();
				properties.put(positionKey, getChildCount(treeElement));

				// create child relationship
				collectionKey.createRelationship(securityContext, treeElement, childElement, properties);
				
				// add new node to linked list
				if (lastChild != null) {
					linkedListManager.insertAfter(lastChild, childElement);
				}

				return null;
			}

		});
	}
	
	public void insertBefore(final T treeElement, final T newChild, final T refChild) throws FrameworkException {

		final SecurityContext securityContext = treeElement.getSecurityContext();
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<AbstractRelationship> rels = getChildRelationships(treeElement);
				int position                    = 0;

				for (AbstractRelationship rel : rels) {

					AbstractNode node = rel.getEndNode();

					if (node.equals(refChild)) {

						// will be used only once here..
						PropertyMap properties = new PropertyMap();
						properties.put(positionKey, position);
						collectionKey.createRelationship(securityContext, treeElement, newChild, properties);
						
						position++;
					}

					rel.setProperty(positionKey, position);

					position++;
				}

				// insert new node in linked list
				linkedListManager.insertBefore(refChild, newChild);

				
				return null;
			}

		});
	}
	
	public void insertAfter(final T treeElement, final T newChild, final T refChild) throws FrameworkException {

		final SecurityContext securityContext = treeElement.getSecurityContext();
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<AbstractRelationship> rels = getChildRelationships(treeElement);
				int position                    = 0;

				for (AbstractRelationship rel : rels) {

					AbstractNode node = rel.getEndNode();

					rel.setProperty(positionKey, position);
					position++;

					if (node.equals(refChild)) {

						// will be used only once here..
						PropertyMap properties = new PropertyMap();
						properties.put(positionKey, position);
						collectionKey.createRelationship(securityContext, treeElement, newChild, properties);
						
						position++;
					}
				}

				// insert new node in linked list
				linkedListManager.insertAfter(refChild, newChild);
				
				return null;
			}

		});
	}

	public void removeChild(final T treeElement, final T childToRemove) throws FrameworkException {

		final SecurityContext securityContext = treeElement.getSecurityContext();
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {
				
				// remove element from linked list
				linkedListManager.remove(childToRemove);

				collectionKey.removeRelationship(securityContext, treeElement, childToRemove);
				
				ensureCorrectChildPositions(treeElement);

				return null;
			}

		});
	}
	
	public void replaceChild(final T treeElement, final T newChild, final T oldChild) throws FrameworkException {

		final SecurityContext securityContext = treeElement.getSecurityContext();
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				// save old position
				int oldPosition = getChildPosition(oldChild);

				// remove old node
				collectionKey.removeRelationship(securityContext, treeElement, oldChild);

				// insert new node with position from old node
				PropertyMap properties = new PropertyMap();
				properties.put(positionKey, oldPosition);
				collectionKey.createRelationship(securityContext, treeElement, newChild, properties);

				// replace element in linked list as well
				linkedListManager.insertBefore(oldChild, newChild);
				linkedListManager.remove(oldChild);
				
				return null;
			}

		});
	}
	
	public T getFirstChild(final T treeElement) {
		return getChild(treeElement, 0);
	}
	
	public T getLastChild(final T treeElement) {
		
		int last = getChildCount(treeElement) - 1;
		if (last >= 0) {
			
			return getChild(treeElement, last);
		}
		
		return null;
	}
	
	public T getChild(final T treeElement, final int position) {
		
		for (AbstractRelationship rel : treeElement.getOutgoingRelationships(treeRelationship)) {
			
			Integer pos = rel.getProperty(positionKey);
			
			if (pos != null && pos.intValue() == position) {
				
				return (T)rel.getEndNode();
			}
		}

		
		return null;
	}
	
	public int getChildPosition(final T child) {
		
		List<AbstractRelationship> rels = child.getIncomingRelationships(treeRelationship);
		if (rels != null && rels.size() == 1) {
			
			// node should have only one parent
			AbstractRelationship rel = rels.get(0);
			
			Integer pos = rel.getProperty(positionKey);
			if (pos != null) {
				
				return pos.intValue();
			}
		}
		
		return -1;
	}
	
	public List<T> getChildren(final T treeElement) {
		
		List<T> children = new ArrayList<T>();
		
		for (AbstractRelationship rel : getChildRelationships(treeElement)) {
			
			children.add((T)rel.getEndNode());
		}
		
		return children;
	}
	
	public int getChildCount(final T treeElement) {
		return treeElement.getOutgoingRelationships(treeRelationship).size();
	}
	
	public LinkedListManager<T> getListManager() {
		return linkedListManager;
	}
	
	// ----- private methods -----

	/**
	 * Ensures that the position attributes of the children of this node
	 * are correct. Please note that this method needs to run in the same
	 * transaction as any modifiying operation that changes the order of
	 * child nodes, and therefore this method does _not_ create its own
	 * transaction. However, it will not raise a NotInTransactionException
	 * when called outside of modifying operations, because each setProperty
	 * call creates its own transaction.
	 */
	private void ensureCorrectChildPositions(final T treeElement) throws FrameworkException {
		
		final SecurityContext securityContext = treeElement.getSecurityContext();
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<AbstractRelationship> childRels = getChildRelationships(treeElement);
				int position                         = 0;

				for (AbstractRelationship childRel : childRels) {

					childRel.removeProperty(positionKey);
					childRel.setProperty(positionKey, position++);
				}

				return null;
			}

		});
	}
	
	public List<AbstractRelationship> getChildRelationships(final T treeElement) {

		// fetch all relationships
		List<AbstractRelationship> childRels = treeElement.getOutgoingRelationships(treeRelationship);
		
		// sort relationships by position
		Collections.sort(childRels, new Comparator<AbstractRelationship>() {

			@Override
			public int compare(AbstractRelationship o1, AbstractRelationship o2) {

				Integer pos1 = o1.getProperty(positionKey);
				Integer pos2 = o2.getProperty(positionKey);

				if (pos1 != null && pos2 != null) {
				
					return pos1.compareTo(pos2);	
				}
				
				return 0;
			}

		});

		return childRels;
	}
}
