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
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 *
 * @author Christian Morgner
 */

public abstract class LinkedTreeNode extends LinkedListNode {

	public abstract CollectionProperty<? extends LinkedTreeNode> getTreeProperty();
	public abstract PropertyKey<Integer> getPositionProperty();
	
	public void treeAppendChild(final LinkedTreeNode childElement) throws FrameworkException {
		
		final CollectionProperty<? extends LinkedTreeNode> treeProperty = getTreeProperty();
		final PropertyKey<Integer> positionProperty                             = getPositionProperty();
		final LinkedTreeNode lastChild                                  = treeGetLastChild();
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				PropertyMap properties = new PropertyMap();
				properties.put(positionProperty, treeGetChildCount());

				// create child relationship
				treeProperty.createRelationship(securityContext, LinkedTreeNode.this, childElement, properties);
				
				// add new node to linked list
				if (lastChild != null) {
					LinkedTreeNode.super.listInsertAfter(lastChild, childElement);
				}

				return null;
			}

		});
	}
	
	public void treeInsertBefore(final LinkedTreeNode newChild, final LinkedTreeNode refChild) throws FrameworkException {
		
		final CollectionProperty<? extends LinkedTreeNode> treeProperty = getTreeProperty();
		final PropertyKey<Integer> positionProperty                             = getPositionProperty();

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<AbstractRelationship> rels = treeGetChildRelationships();
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
				LinkedTreeNode.super.listInsertBefore(refChild, newChild);

				
				return null;
			}

		});
	}
	
	public void treeInsertAfter(final LinkedTreeNode newChild, final LinkedTreeNode refChild) throws FrameworkException {
		
		final CollectionProperty<? extends LinkedTreeNode> treeProperty = getTreeProperty();
		final PropertyKey<Integer> positionProperty                             = getPositionProperty();

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<AbstractRelationship> rels = treeGetChildRelationships();
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
				LinkedTreeNode.super.listInsertAfter(refChild, newChild);
				
				return null;
			}

		});
	}

	public void treeRemoveChild(final LinkedTreeNode childToRemove) throws FrameworkException {
		
		final CollectionProperty<? extends LinkedTreeNode> treeProperty = getTreeProperty();

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {
				
				// remove element from linked list
				LinkedTreeNode.super.listRemove(childToRemove);

				treeProperty.removeRelationship(securityContext, LinkedTreeNode.this, childToRemove);
				
				ensureCorrectChildPositions();

				return null;
			}

		});
	}
	
	public void treeReplaceChild(final LinkedTreeNode newChild, final LinkedTreeNode oldChild) throws FrameworkException {
		
		final CollectionProperty<? extends LinkedTreeNode> treeProperty = getTreeProperty();
		final PropertyKey<Integer> positionProperty                             = getPositionProperty();

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				// save old position
				int oldPosition = treeGetChildPosition(oldChild);

				// remove old node
				treeProperty.removeRelationship(securityContext, LinkedTreeNode.this, oldChild);

				// insert new node with position from old node
				PropertyMap properties = new PropertyMap();
				properties.put(positionProperty, oldPosition);
				treeProperty.createRelationship(securityContext, LinkedTreeNode.this, newChild, properties);

				// replace element in linked list as well
				LinkedTreeNode.super.listInsertBefore(oldChild, newChild);
				LinkedTreeNode.super.listRemove(oldChild);
				
				return null;
			}

		});
	}
	
	public LinkedTreeNode treeGetFirstChild() {
		return treeGetChild(0);
	}
	
	public LinkedTreeNode treeGetLastChild() {
		
		int last = treeGetChildCount() - 1;
		if (last >= 0) {
			
			return treeGetChild(last);
		}
		
		return null;
	}
	
	public LinkedTreeNode treeGetChild(final int position) {
		
		final CollectionProperty<? extends LinkedTreeNode> treeProperty = getTreeProperty();
		final RelationshipType treeRelationship                                 = treeProperty.getRelType();
		final PropertyKey<Integer> positionProperty                             = getPositionProperty();
		
		for (AbstractRelationship rel : getOutgoingRelationships(treeRelationship)) {
			
			Integer pos = rel.getProperty(positionProperty);
			
			if (pos != null && pos.intValue() == position) {
				
				return (LinkedTreeNode)rel.getEndNode();
			}
		}

		
		return null;
	}
	
	public int treeGetChildPosition(final LinkedTreeNode child) {
		
		final CollectionProperty<? extends LinkedTreeNode> treeProperty = getTreeProperty();
		final PropertyKey<Integer> positionProperty                             = getPositionProperty();
		final RelationshipType treeRelationship                                 = treeProperty.getRelType();
		
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
	
	public List<LinkedTreeNode> treeGetChildren() {
		
		List<LinkedTreeNode> children = new ArrayList<LinkedTreeNode>();
		
		for (AbstractRelationship rel : treeGetChildRelationships()) {
			
			children.add((LinkedTreeNode)rel.getEndNode());
		}
		
		return children;
	}
	
	public int treeGetChildCount() {
		
		final CollectionProperty<? extends LinkedTreeNode> treeProperty = getTreeProperty();
		final RelationshipType treeRelationship                                 = treeProperty.getRelType();

		return getOutgoingRelationships(treeRelationship).size();
	}
	
	public List<AbstractRelationship> treeGetChildRelationships() {
		
		final CollectionProperty<? extends LinkedTreeNode> treeProperty = getTreeProperty();
		final RelationshipType treeRelationship                                 = treeProperty.getRelType();
		final PropertyKey<Integer> positionProperty                             = getPositionProperty();

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
	private void ensureCorrectChildPositions() throws FrameworkException {
		
		final CollectionProperty<? extends LinkedTreeNode> treeProperty = getTreeProperty();
		final PropertyKey<Integer> positionProperty                             = getPositionProperty();
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<AbstractRelationship> childRels = treeGetChildRelationships();
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
