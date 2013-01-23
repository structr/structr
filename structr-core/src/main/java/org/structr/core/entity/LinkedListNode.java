package org.structr.core.entity;

import java.util.List;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.CollectionProperty;

/**
 *
 * @author Christian Morgner
 */
public abstract class LinkedListNode extends AbstractNode {

	public abstract CollectionProperty<? extends LinkedListNode> getListProperty();
	
	/**
	 * Returns the predecessor of the given element in the list structure
	 * defined by this LinkedListManager.
	 *
	 * @param currentElement
	 * @return
	 */
	public LinkedListNode listGetPrevious(LinkedListNode currentElement) {

		final CollectionProperty<? extends LinkedListNode> listProperty = getListProperty();
		final RelationshipType relType                                  = listProperty.getRelType();
		List<AbstractRelationship> incomingRelationships                = currentElement.getIncomingRelationships(relType);
		
		if (incomingRelationships != null && !incomingRelationships.isEmpty()) {

			int size = incomingRelationships.size();
			if (size == 1) {

				AbstractRelationship incomingRel = incomingRelationships.get(0);
				if (incomingRel != null) {

					return (LinkedListNode)incomingRel.getStartNode();
				}
			}

			throw new IllegalStateException("Given node is not a valid list node for the given relationship type.");
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
	public LinkedListNode listGetNext(LinkedListNode currentElement) {
		
		final CollectionProperty<? extends LinkedListNode> listProperty = getListProperty();
		final RelationshipType relType                                  = listProperty.getRelType();
		List<AbstractRelationship> outgoingRelationships = currentElement.getOutgoingRelationships(relType);
		
		if (outgoingRelationships != null && !outgoingRelationships.isEmpty()) {

			int size = outgoingRelationships.size();
			if (size == 1) {

				AbstractRelationship outgoingRel = outgoingRelationships.get(0);
				if (outgoingRel != null) {

					return (LinkedListNode)outgoingRel.getEndNode();
				}
			}

			throw new IllegalStateException("Given node is not a valid list node for the given relationship type: found " + size + " outgoing relationships of type " + relType);
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
	public void listInsertBefore(final LinkedListNode currentElement, final LinkedListNode newElement) throws FrameworkException {

		if (currentElement.getId() == newElement.getId()) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}

		final CollectionProperty<? extends LinkedListNode> listProperty = getListProperty();
		final RelationshipType relType                                  = listProperty.getRelType();
		final LinkedListNode previousElement                            = listGetPrevious(currentElement);

		if (previousElement == null) {

			// trivial: new node will become new head of existing list
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					Node startNode = newElement.getNode();
					Node endNode = currentElement.getNode();

					if (startNode != null && endNode != null) {
						startNode.createRelationshipTo(endNode, relType);
					}

					return null;
				}
			});

		} else {

			// not-so-trivial: insert new element between predecessor and current node
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					Node previousNode = previousElement.getNode();
					Node currentNode = currentElement.getNode();
					Node newNode = newElement.getNode();

					if (previousNode != null && newNode != null && currentNode != null) {

						// delete old relationship
						removeRelationshipBetween(previousNode, currentNode);

						// create two new ones
						previousNode.createRelationshipTo(newNode, relType);
						newNode.createRelationshipTo(currentNode, relType);
					}

					return null;
				}
			});
		}
	}

	/**
	 * Inserts newElement after currentElement in the list defined by this
	 * LinkedListManager.
	 *
	 * @param currentElement the reference element
	 * @param newElement the new element
	 */
	public void listInsertAfter(final LinkedListNode currentElement, final LinkedListNode newElement) throws FrameworkException {

		if (currentElement.getId() == newElement.getId()) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}

		final CollectionProperty<? extends LinkedListNode> listProperty = getListProperty();
		final RelationshipType relType                                  = listProperty.getRelType();
		final LinkedListNode next                                       = listGetNext(currentElement);
		
		if (next == null) {

			// trivial: new node will become new tail of existing list
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					Node startNode = currentElement.getNode();
					Node endNode = newElement.getNode();

					if (startNode != null && endNode != null) {
						startNode.createRelationshipTo(endNode, relType);
					}

					return null;
				}
			});

		} else {

			// not-so-trivial: insert new element between current node and successor
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					Node nextNode = next.getNode();
					Node currentNode = currentElement.getNode();
					Node newNode = newElement.getNode();

					if (nextNode != null && newNode != null && currentNode != null) {

						// delete old relationship
						removeRelationshipBetween(currentNode, nextNode);

						// create two new ones
						currentNode.createRelationshipTo(newNode, relType);
						newNode.createRelationshipTo(nextNode, relType);
					}

					return null;
				}
			});
		}
	}

	/**
	 * Removes the current element from the list defined by this
	 * LinkedListManager.
	 *
	 * @param currentElement the element to be removed
	 */
	public void listRemove(final LinkedListNode currentElement) {
		
		final CollectionProperty<? extends LinkedListNode> listProperty = getListProperty();
		final RelationshipType relType                                  = listProperty.getRelType();
		final LinkedListNode previousElement                            = listGetPrevious(currentElement);
		final LinkedListNode nextElement                                = listGetNext(currentElement);
		final Node currentNode                                          = currentElement.getNode();

		if (previousElement != null) {

			Node previousNode = previousElement.getNode();
			if (previousNode != null && currentNode != null) {

				removeRelationshipBetween(previousNode, currentNode);
			}
		}

		if (nextElement != null) {

			Node nextNode = nextElement.getNode();
			if (nextNode != null && currentNode != null) {

				removeRelationshipBetween(currentNode, nextNode);
			}
		}

		if (previousElement != null && nextElement != null) {

			Node previousNode = previousElement.getNode();
			Node nextNode = nextElement.getNode();

			if (previousNode != null && nextNode != null) {

				previousNode.createRelationshipTo(nextNode, relType);
			}

		}
	}

	/**
	 * Removes the relationship of the type defined in this
	 * LinkedListManager between the two nodes. Please note that this method
	 * does not create its own transaction, so it needs to be wrapped into a
	 * transaction.
	 *
	 * @param startNode
	 * @param endNode
	 */
	private void removeRelationshipBetween(Node startNode, Node endNode) {

		final CollectionProperty<? extends LinkedListNode> listProperty = getListProperty();
		final RelationshipType relType                                  = listProperty.getRelType();

		if (startNode != null && endNode != null) {

			if (startNode.hasRelationship(relType, Direction.OUTGOING)) {

				Relationship rel = startNode.getSingleRelationship(relType, Direction.OUTGOING);
				if (rel.getEndNode().getId() == endNode.getId()) {

					rel.delete();
				}
			}
		}
	}
}
