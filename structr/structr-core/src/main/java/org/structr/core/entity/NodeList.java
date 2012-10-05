/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.entity;

import java.util.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Decorable;
import org.structr.core.Decorator;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.node.Evaluable;
import org.structr.core.node.NodeFactory;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Result;

//~--- classes ----------------------------------------------------------------

/**
 * A linked list implementation on StructrNodes. In contrast to the default List
 * implementations, this list does not permit null elements.
 *
 * <p>
 * This list can be decorated with instances of {@see org.structr.core.Decorator}
 * in order to set additional properties on the nodes that are added to this list.
 * </p>
 * <p>
 * This list can be filtered through the Neo4j Evaluator interface in order to
 * control the results of operations on this list. Note that this implementation
 * can return different results depending on the set of evaluators that are present.
 * Even the size of the list may vary, and this will alter the results of insert
 * and remove methods.
 * </p>
 *
 * @author Christian Morgner
 */
public class NodeList<T extends AbstractNode> extends AbstractNode implements Iterable<AbstractNode>, Decorable<AbstractNode>, Evaluable {

	private static final Logger logger = Logger.getLogger(NodeList.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(NodeList.class, PropertyView.All, Key.values());
	}

	//~--- fields ---------------------------------------------------------

	private Set<Decorator<AbstractNode>> decorators = new LinkedHashSet<Decorator<AbstractNode>>();
	private Command factory                         = null;
	private int maxLength                           = -1;
	private Command transaction                     = null;
	private Set<Evaluator> evaluators               = new LinkedHashSet<Evaluator>();

	//~--- constant enums -------------------------------------------------

//      public class NodeList<T extends AbstractNode> extends AbstractNode implements List<AbstractNode>, Decorable<AbstractNode>, Evaluable {
	public enum Key implements PropertyKey{ parent; }

	//~--- constructors ---------------------------------------------------

	public NodeList() {

		this(-1);

	}

	public NodeList(int maxLength) {

		this.maxLength   = maxLength;
		this.transaction = Services.command(securityContext, TransactionCommand.class);
		this.factory     = Services.command(securityContext, NodeFactoryCommand.class);;

	}

	//~--- methods --------------------------------------------------------

	// ----- interface List<T> -----

	/**
	 * Returns the size of this node list. Note that setting or removing evaluators can change
	 * the value returned by this method. This method will take time proportional to the number
	 * of elements in the list.
	 *
	 * @return the size of this list with the current set of evaluators
	 */
	public int size() {

		int ret = 0;

		for (Node node : getRawNodes()) {

			ret++;
		}

		return (ret);

	}

	/**
	 * Returns an iterator over the elements of this list (according to the current set
	 * of evaluators).
	 *
	 * @return the iterator
	 */
	@Override
	public Iterator<AbstractNode> iterator() {

		return (getNodes().iterator());

	}

//      /**
//       * Returns an array containing all the elements in this list according to the
//       * current set of evaluators.
//       *
//       * @return the array
//       */
//      @Override
//      public Object[] toArray() {
//          return (getNodeList().toArray());
//      }
//
//      /**
//       * Returns an array containing all the elements in this list according to the
//       * current set of evaluators.
//       *
//       * @param <T>
//       * @param a
//       * @return
//       */
//      @Override
//      public <AbstractList> AbstractList[] toArray(AbstractList[] a) {
//          return (getNodeList().toArray(a));
//      }

	/**
	 * Applies all decorators that are set on this list and adds the node
	 * to the end of this list. This method runs in constant time. Note that
	 * this method runs in a transaction. This transaction includes any
	 * decorator that is present on this list.
	 *
	 * <p>
	 * If a collection refuses to add a particular element for any reason
	 * other than that it already contains the element, it must throw an
	 * exception (rather than returning false). This preserves the
	 * invariant that a collection always contains the specified element
	 * after this call returns.
	 * </p>
	 *
	 * @param toRemove the node to add
	 * @return true if this collection changed as a result of this call
	 */
//      @Override
	public boolean add(final AbstractNode toAdd) {

		try {

			Boolean returnValue = (Boolean) transaction.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					// apply decorators (if any)
					for (Decorator<AbstractNode> decorator : decorators) {

						decorator.decorate(toAdd);
					}

					return (appendNodeToList(toAdd.getNode()));
				}

			});

			return (returnValue.booleanValue());

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to add node to this list", fex);

		}

		return false;

	}

	/**
	 * Removes the given node from this list.
	 *
	 * @param toRemove
	 * @return true if this list contained the given element
	 */
	public boolean remove(final Object node) {

		try {

			Boolean returnValue = (Boolean) transaction.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					return (removeNodeFromList((Node) node));

				}

			});

			return (returnValue.booleanValue());

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to remove node from this list", fex);

		}

		return false;

	}

	//
//      /**
//       * Indicates whether this list contains all of the elements in the
//       * given collection.
//       *
//       * @param nodes
//       * @return true or false
//       */
//      @Override
//      public boolean containsAll(Collection<?> c) {
//          return (getNodeList().containsAll(c));
//      }

	/**
	 * Adds all the elements in the given collection to this list, applying
	 * any decorator that is set on this list before addition.
	 *
	 * @param nodes
	 * @return
	 */
//      @Override
	public boolean addAll(final Collection<? extends AbstractNode> nodes) {

		try {

			Boolean returnValue = (Boolean) transaction.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					boolean ret = false;

					for (AbstractNode node : nodes) {

						ret |= appendNodeToList(node.getNode());
					}

					return (ret);

				}

			});

			return (returnValue.booleanValue());

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to add nodes to this list", fex);

		}

		return false;

	}

//      /**
//       * Inserts all the elements in the given collection at the given index, applying
//       * any decorator that is set on this list before addition, respecting any evaluator
//       * that is currently set on this list.
//       *
//       * @param index
//       * @param nodes
//       * @return
//       */
//      @Override
//      public boolean addAll(final int index, final Collection<? extends AbstractNode> nodes) {
//          Boolean returnValue = (Boolean) transaction.execute(new StructrTransaction() {
//
//              @Override
//              public Object execute() throws FrameworkException {
//                  Node startNode = getNodeAt(index);
//                  Node nextNode = null;
//                  boolean ret = false;
//
//                  for (AbstractNode toInsert : nodes) {
//                      nextNode = getRelatedNode(startNode, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);
//
//                      ret |= insertNodeBefore(nextNode, toInsert.getNode());
//                  }
//
//                  return (ret);
//              }
//          });
//
//          return (returnValue.booleanValue());
//      }
//
//      /**
//       * Removes all elements in the given collection from this list.
//       *
//       * @param nodes
//       * @return
//       */
//      @Override
//      public boolean removeAll(final Collection<?> nodes) {
//          Boolean returnValue = (Boolean) transaction.execute(new StructrTransaction() {
//
//              @Override
//              public Object execute() throws FrameworkException {
//                  boolean ret = false;
//
//                  for (Object obj : nodes) {
//                      // provoke ClassCastException according to List interface specification
//                      T structrNode = (T) obj;
//                      Node node = structrNode.getNode();
//
//                      ret |= removeNodeFromList(node);
//                  }
//
//                  return (ret);
//              }
//          });
//
//          return (returnValue.booleanValue());
//      }
//
//      /**
//       * Retains only the elements in this list that are contained in the given
//       * collection.
//       *
//       * @param nodes
//       * @return
//       */
//      @Override
//      public boolean retainAll(final Collection<?> nodes) {
//          Boolean returnValue = (Boolean) transaction.execute(new StructrTransaction() {
//
//              @Override
//              public Object execute() throws FrameworkException {
//                  boolean ret = false;
//
//                  for (AbstractNode node : getNodes()) {
//                      if (!nodes.contains(node)) {
//                          ret |= removeNodeFromList(node.getNode());
//                      }
//                  }
//
//                  return (ret);
//              }
//          });
//
//          return (returnValue.booleanValue());
//      }

	/**
	 * Clears this list. Due to the fact that this method has to remove all
	 * elements from the linked list, this method will take time proportional
	 * to the size of the list, with a relativley large constant factor.
	 */
	public void clear() {

		try {

			transaction.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					for (Node node : getRawNodes()) {

						removeNodeFromList(node);
					}

					return (null);

				}

			});

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to clear this list", fex);

		}

	}

//
//      /**
//       * Returns the element at the given index, or null if no element exists,
//       * with respect to the evaluators that are currently set on this list.
//       *
//       * @param index
//       * @return
//       */
//      @Override
//      public T get(int index) {
//          if (index < 0 || index >= size()) {
//              throw new ArrayIndexOutOfBoundsException();
//          }
//
//          Node node = getNodeAt(index);
//
//          if (node != null) {
//              return ((T) factory.execute(node));
//          }
//
//          return (null);
//      }
//
//      /**
//       * Replaces the element at the given position with the given element, with
//       * respect to the evaluators that are currently set on this list.
//       *
//       * @param index
//       * @param toAdd
//       * @return
//       */
//      @Override
//      public AbstractNode set(final int index, final AbstractNode toSet) {
//          transaction.execute(new StructrTransaction() {
//
//              @Override
//              public Object execute() throws FrameworkException {
//                  Node node = getNodeAt(index);
//                  if (node != null) {
//                      insertNodeBefore(node, toSet.getNode());
//                      removeNodeFromList(node);
//                  }
//
//                  return (null);
//              }
//          });
//
//          return (toSet);
//      }

	/**
	 * Inserts the given element at the given position (with respect to the
	 * evaluators that are currently set on this list).
	 *
	 * @param index
	 * @param toAdd
	 */
	// @Override
	public void add(final int index, final AbstractNode toAdd) {

		final int size = this.size();

		if ((index < 0) || (index > size)) {

			throw new ArrayIndexOutOfBoundsException();
		}

		try {

			transaction.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					// apply decorators (if any)
					for (Decorator<AbstractNode> decorator : decorators) {

						decorator.decorate(toAdd);
					}

					if (index == size) {

						appendNodeToList(toAdd.getNode());
					} else {

						insertNodeIntoList(index, toAdd.getNode());
					}

					return (null);
				}

			});

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to add node to this list", fex);

		}

	}

//      /**
//       * Removes the element at the given position (with respect to the evaluators
//       * that are currently set on this list.)
//       *
//       * @param index
//       * @return
//       */
//      @Override
//      public T remove(int index) {
//          final Node node = getNodeAt(index);
//
//          if (node != null) {
//              transaction.execute(new StructrTransaction() {
//
//                  @Override
//                  public Object execute() throws FrameworkException {
//                      return (removeNodeFromList(node));
//                  }
//              });
//          }
//
//          return ((T) factory.execute(node));
//      }
//
//      /**
//       * Returns the position of the given element in this list
//       * @param o
//       * @return
//       */
//      @Override
//      public int indexOf(Object o) {
//          T node = (T) o;
//
//          return (indexOf(node.getNode()));
//      }
//
//      /**
//       * Returns the last position of the given element in this list, with
//       * respect to the evaluators that are currently set on this list.
//       *
//       * (implementation note: keep LAST pointer!)
//       *
//       * @param o
//       * @return
//       */
//      @Override
//      public int lastIndexOf(Object o) {
//          throw new UnsupportedOperationException("Not supported yet.");
//      }
//
//      /**
//       * Returns a ListIterator containing all elements in this list, with
//       * respect to the evaluators that are currently set.
//       *
//       * @return
//       */
//      @Override
//      public ListIterator<AbstractNode> listIterator() {
//          return getNodeList().listIterator();
//          //throw new UnsupportedOperationException("Bi-directional iteration is not yet supported by this class.");
//      }
//
//      /**
//       * Returns a ListIterator containing all elements in this list, starting
//       * from the given index, with respect to the evaluators that are currently
//       * set on this list.
//       *
//       * @param index
//       * @return
//       */
//      @Override
//      public ListIterator<AbstractNode> listIterator(int index) {
//          return getNodeList().listIterator(index);
//          //throw new UnsupportedOperationException("Bi-directional iteration is not yet supported by this class.");
//      }
//
//      /**
//       * Returns a sublist of this list, starting at (including) fromIndex,
//       * ending at (not including) toIndex, with respect to the evaluators that are
//       * currently set on this list.
//       *
//       * @param fromIndex
//       * @param toIndex
//       * @return
//       */
//      @Override
//      public List<AbstractNode> subList(int fromIndex, int toIndex) {
//          //return a new NodeList instance with the given bounds
//          Node startNode = getNodeAt(fromIndex);
//          NodeList ret = new NodeList(toIndex - fromIndex);
//
//          ret.init(startNode);
//
//          return (ret);
//      }
	// ----- interface Decorable<T>
	@Override
	public void addDecorator(Decorator<AbstractNode> d) {

		decorators.add(d);

	}

	@Override
	public void removeDecorator(Decorator<AbstractNode> d) {

		decorators.remove(d);

	}

	// ----- interface Evaluable -----
	@Override
	public void addEvaluator(Evaluator e) {

		evaluators.add(e);

	}

	@Override
	public void removeEvaluator(Evaluator e) {

		evaluators.remove(e);

	}

	private TraversalDescription createTraversalDescription() {

		TraversalDescription ret = Traversal.description().depthFirst();

		ret = ret.relationships(RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);
		ret = ret.evaluator(new ParentIdEvaluator());

		// add list evaluators
		for (Evaluator evaluator : evaluators) {

			ret = ret.evaluator(evaluator);
		}

		if (maxLength >= 0) {

			ret = ret.evaluator(new MaxLengthEvaluator());
		}

		return (ret);

	}

	/**
	 * Removes the given node from this list.
	 *
	 * @param toRemove
	 * @return true if the list was modified as a result of this operation
	 */
	private boolean removeNodeFromList(Node toRemove) {

		boolean listWasModified = false;

		if ((toRemove != null) && isMember(toRemove)) {

			// node is not null and part of this list
			Node rootNode     = getNode();
			Node previousNode = getRelatedNode(toRemove, RelType.NEXT_LIST_ENTRY, Direction.INCOMING);
			Node nextNode     = getRelatedNode(toRemove, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);

			// delete relationship from previousNode to toRemove
			deleteRelationship(previousNode, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);

			// delete relationship from toRemove to nextNode (if exists)
			deleteRelationship(toRemove, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);

			// if nextNode exists
			if (nextNode != null) {

				// create relationship to next node
				createRelationship(previousNode, nextNode, RelType.NEXT_LIST_ENTRY);
			} else {

				// delete LAST relationship from rootNode
				deleteRelationship(rootNode, RelType.LAST_LIST_ENTRY, Direction.OUTGOING);

				// create LAST relationship from rootNode to previousNode
				createRelationship(rootNode, previousNode, RelType.LAST_LIST_ENTRY);
			}

			listWasModified = true;
		}

		return (listWasModified);

	}

	/**
	 * Appends the given node to this list. Note that this method does not run
	 * in a transaction to enable bulk add methods to share a single transaction.
	 *
	 * @param toAdd
	 * @return true if this list was modified as a result of this call
	 */
	private boolean appendNodeToList(Node toAdd) {

		boolean listWasModified = false;

		if (!isMember(toAdd)) {

			// node is not null and not already a member of this list
			Node rootNode = getNode();
			Node lastNode = getRelatedNode(rootNode, RelType.LAST_LIST_ENTRY, Direction.OUTGOING);

			if (lastNode != null) {

				// create NEXT relationship from lastNode to toAdd
				createRelationship(lastNode, toAdd, RelType.NEXT_LIST_ENTRY);

				// delete LAST relationship from rootNode
				deleteRelationship(rootNode, RelType.LAST_LIST_ENTRY, Direction.OUTGOING);

				// create LAST relationship from rootNode to previousNode
				createRelationship(rootNode, toAdd, RelType.LAST_LIST_ENTRY);

				listWasModified = true;
			} else {

				// list is empty, add node as last node
				createRelationship(rootNode, toAdd, RelType.NEXT_LIST_ENTRY);
				createRelationship(rootNode, toAdd, RelType.LAST_LIST_ENTRY);

				listWasModified = true;
			}
		}

		return (listWasModified);

	}

	private boolean insertNodeIntoList(int index, Node toInsert) {

		Node node   = getNodeAt(index);
		boolean ret = false;

		if (node != null) {

			ret = insertNodeBefore(node, toInsert);
		}

		return (ret);

	}

	private boolean insertNodeBefore(Node node, Node toInsert) {

		boolean ret = false;

		if ((node != null) && (toInsert != null) &&!isMember(toInsert)) {

			Node previousNode = getRelatedNode(node, RelType.NEXT_LIST_ENTRY, Direction.INCOMING);

			// delete relationship from previousNode to node
			ret |= deleteRelationship(previousNode, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);

			// create relationship from previousNode to toInsert
			ret |= createRelationship(previousNode, toInsert, RelType.NEXT_LIST_ENTRY);

			// create relationship from toInsert to node
			ret |= createRelationship(toInsert, node, RelType.NEXT_LIST_ENTRY);

		}

		return (ret);

	}

	private int indexOf(Node node) {

		int ret = 0;

		for (Node n : getRawNodes()) {

			if (node.equals(n)) {

				return (ret);
			}

			ret++;

		}

		return (-1);

	}

	private boolean deleteRelationship(Node startNode, RelType relationshipType, Direction direction) {

		Iterable<Relationship> rels = startNode.getRelationships(relationshipType, direction);
		boolean ret                 = false;

		for (Relationship rel : rels) {

			if (rel.hasProperty(Key.parent.name())) {

				Object parent = rel.getProperty(Key.parent.name());

				if ((parent instanceof Long) && ((Long) parent).equals(getNodeId())) {

					rel.delete();

					ret = true;

				}

			}

		}

		return (ret);

	}

	private boolean createRelationship(Node startNode, Node endNode, RelType relationshipType) {

		if (!startNode.equals(endNode)) {

			Relationship rel = startNode.createRelationshipTo(endNode, relationshipType);

			rel.setProperty(Key.parent.name(), Long.valueOf(getNodeId()));

			return (true);

		}

		return (false);

	}

	//~--- get methods ----------------------------------------------------

	/**
	 * Returns the first node of this list, or null if this list is empty.
	 *
	 * @return the first node of this list, or null
	 */
	public AbstractNode getFirstNode() {

		Node node = getFirstRawNode();

		try {

			if (node != null) {

				return ((AbstractNode) factory.execute(node));
			}

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to instantiate node", fex);

		}

		return (null);

	}

	/**
	 * Returns the first raw node of this list, or null if this list is empty
	 *
	 * @return the first raw node of this list, or null
	 */
	public Node getFirstRawNode() {

		Node rootNode = getNode();

		return (getRelatedNode(rootNode, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING));

	}

	/**
	 * Returns the last node of this list, or null if this list is empty.
	 *
	 * @return the last node of this list, or null
	 */
	public AbstractNode getLastNode() {

		try {

			return ((AbstractNode) factory.execute(getLastRawNode()));

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to instantiate node", fex);

		}

		return null;

	}

	/**
	 * Returns the last raw node of this list, or null if this list is empty
	 *
	 * @return the last raw node of this list, or null
	 */
	public Node getLastRawNode() {

		Node rootNode = getNode();

		return (getRelatedNode(rootNode, RelType.LAST_LIST_ENTRY, Direction.OUTGOING));

	}

	/**
	 * Returns the maximum traversal depth of this list.
	 *
	 * @return
	 */
	public int getMaxLength() {

		return (maxLength);

	}

	// ----- private methods -----
	private List<AbstractNode> getNodeList() {

		List<AbstractNode> ret = new LinkedList<AbstractNode>();

		for (AbstractNode node : getNodes()) {

			ret.add(node);
		}

		return (ret);

	}

	private Iterable<AbstractNode> getNodes() {

		try {
			
			NodeFactory factory = new NodeFactory(securityContext);
			Result result = factory.createAllNodes(getRawNodes());
			
			return (List<AbstractNode>) result.getResults();
			
		} catch(FrameworkException fex) {
			
			logger.log(Level.WARNING, "Unable to instantiate nodes: {0}", fex.getMessage());
		}
		
		return Collections.emptyList();
	}

	private Iterable<Node> getRawNodes() {

		// create traversal description
		TraversalDescription td = createTraversalDescription();
		Traverser traverser     = td.traverse(getNode());

		return (traverser.nodes());
	}

	/**
	 * Returns a node, following the given relationship and direction from startNode.
	 *
	 * @param startNode
	 * @param relationshipType
	 * @param direction
	 * @return true if the given node is part of the list this node contains
	 */
	private Node getRelatedNode(Node startNode, RelType relationshipType, Direction direction) {

		if (startNode != null) {

			Iterable<Relationship> rels = startNode.getRelationships(relationshipType, direction);

			for (Relationship rel : rels) {

				if (rel.hasProperty(Key.parent.name())) {

					Object parent = rel.getProperty(Key.parent.name());

					if ((parent instanceof Long) && ((Long) parent).equals(getNodeId())) {

						if (direction.equals(Direction.INCOMING)) {

							return (rel.getStartNode());
						} else {

							return (rel.getEndNode());
						}

					}

				}

			}

		}

		return (null);

	}

	/**
	 * Returns the node at the given position, or null if no node is found.
	 *
	 * @param index
	 * @return the node at index or null
	 */
	private Node getNodeAt(int index) {

		int pos = 0;

		for (Node node : getRawNodes()) {

			if (pos++ == index) {

				return (node);
			}

		}

		return (null);

	}

	/**
	 * Indicates whether this list is empty. Note that in contrast to the size() methods,
	 * this is an O(1) operation and should be seen as the preferred way to check whether a list
	 * is empty or not.
	 *
	 * @return true if this list is empty
	 */
	public boolean isEmpty() {

		Node startNode      = this.getNode();
		boolean hasElements = ((startNode != null) && startNode.hasRelationship(RelType.NEXT_LIST_ENTRY, Direction.OUTGOING));

		return (!hasElements);

	}

//
//      /**
//       * Indicates whether this list contains the given element. This method can take time
//       * proportional to the number of elements in the list.
//       *
//       * @param o
//       * @return
//       */
//      @Override
//      public boolean contains(Object o) {
//         AbstractNode n = (AbstractNode) o;
//
//          for (AbstractNode node : getNodes()) {
//              if (node.equals(n)) {
//                  return (true);
//              }
//          }
//
//          return (false);
//
//      }
	private boolean isMember(Node node) {

		return (getRelatedNode(node, RelType.NEXT_LIST_ENTRY, Direction.INCOMING) != null);

	}

	//~--- set methods ----------------------------------------------------

	/**
	 * Sets the maximum travesal depth of this list to the given value.
	 *
	 * @param maxLength
	 */
	public void setMaxLength(int maxLength) {

		this.maxLength = maxLength;

	}

	//~--- inner classes --------------------------------------------------

	private class MaxLengthEvaluator implements Evaluator {

		@Override
		public Evaluation evaluate(Path path) {

			if (path.length() > maxLength) {

				return (Evaluation.EXCLUDE_AND_PRUNE);
			}

			return (Evaluation.INCLUDE_AND_CONTINUE);

		}

	}


	// ----- nested classes -----
	private class ParentIdEvaluator implements Evaluator {

		@Override
		public Evaluation evaluate(Path path) {

			// we're following NEXT_LIST_ENTRY rels, so we can trace our way back..
//                      Relationship rel = path.endNode().getSingleRelationship(RelType.NEXT_LIST_ENTRY, Direction.INCOMING);
			Iterable<Relationship> rels = path.endNode().getRelationships(RelType.NEXT_LIST_ENTRY, Direction.INCOMING);

			if (rels != null) {

				for (Relationship rel : rels) {

					if ((rel != null) && rel.hasProperty(Key.parent.name())) {

						Object parent = rel.getProperty(Key.parent.name());

						if ((parent instanceof Long) && ((Long) parent).equals(getNodeId())) {

							return (Evaluation.INCLUDE_AND_CONTINUE);
						}

					}

				}

			}

			// TODO: find out if EXCLUDE_AND_CONTINUE is the right choice here!
			return (Evaluation.EXCLUDE_AND_CONTINUE);
		}

	}

}
