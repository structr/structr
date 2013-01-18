package org.structr.web.common;

import java.util.List;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;

/**
 * Utility class that provides methods to create, access and manage list
 * structures on top of existing nodes.
 * 
 * @author Christian Morgner
 */
public class LinkedListManager<T extends AbstractNode> {
	
	private RelationshipType relType = null;
	
	/**
	 * Constructs a new instance of this class with the given relationship type.
	 * 
	 * @param relType the relationship type the underlying list will use for its link relationships
	 */
	public LinkedListManager(RelationshipType relType) {
		this.relType   = relType;
	}
	
	/**
	 * Returns the predecessor of the given element in the list structure
	 * defined by this LinkedListManager.
	 * 
	 * @param currentElement
	 * @return 
	 */
	public T getPrevious(T currentElement) {
		
		List<AbstractRelationship> incomingRelationships = currentElement.getIncomingRelationships(relType);
		if (incomingRelationships != null && !incomingRelationships.isEmpty()) {

			int size = incomingRelationships.size();
			if (size == 1) {
				
				AbstractRelationship incomingRel = incomingRelationships.get(0);
				if (incomingRel != null) {
					
					return (T)incomingRel.getStartNode();
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
	public T getNext(T currentElement) {
		
		List<AbstractRelationship> outgoingRelationships = currentElement.getOutgoingRelationships(relType);
		if (outgoingRelationships != null && !outgoingRelationships.isEmpty()) {

			int size = outgoingRelationships.size();
			if (size == 1) {
				
				AbstractRelationship outgoingRel = outgoingRelationships.get(0);
				if (outgoingRel != null) {
					
					return (T)outgoingRel.getEndNode();
				}
			}
			
			throw new IllegalStateException("Given node is not a valid list node for the given relationship type.");
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
	public void insertBefore(final T currentElement, final T newElement) throws FrameworkException {
	
		if (currentElement.getId() == newElement.getId()) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}
		
		final SecurityContext securityContext = currentElement.getSecurityContext();
		final T previousElement = getPrevious(currentElement);
		
		if (previousElement == null) {

			// trivial: new node will become new head of existing list
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					Node startNode = newElement.getNode();
					Node endNode   = currentElement.getNode();

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
					Node currentNode  = currentElement.getNode();
					Node newNode      = newElement.getNode();

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
	public void insertAfter(final T currentElement, final T newElement) throws FrameworkException {
	
		if (currentElement.getId() == newElement.getId()) {
			throw new IllegalStateException("Cannot link a node to itself!");
		}
		
		final SecurityContext securityContext = currentElement.getSecurityContext();
		final T next = getNext(currentElement);
		
		if (next == null) {

			// trivial: new node will become new tail of existing list
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					Node startNode = currentElement.getNode();
					Node endNode   = newElement.getNode();

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

					Node nextNode    = next.getNode();
					Node currentNode = currentElement.getNode();
					Node newNode     = newElement.getNode();

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
	public void remove(final T currentElement) {
		
		T previousElement = getPrevious(currentElement);
		T nextElement     = getNext(currentElement);
		Node currentNode  = currentElement.getNode();
		
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
			Node nextNode     = nextElement.getNode();
			
			if (previousNode != null && nextNode != null) {
				
				previousNode.createRelationshipTo(nextNode, relType);
			}
			
		}
	}
	
	/**
	 * Removes the relationship of the type defined in this LinkedListManager
	 * between the two nodes. Please note that this method does not create its
	 * own transaction, so it needs to be wrapped into a transaction.
	 * 
	 * @param startNode
	 * @param endNode 
	 */
	private void removeRelationshipBetween(Node startNode, Node endNode) {
		
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
