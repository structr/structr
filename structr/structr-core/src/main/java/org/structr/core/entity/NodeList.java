/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;
import org.structr.common.RelType;
import org.structr.core.Decorable;
import org.structr.core.Decorator;
import org.structr.core.Services;
import org.structr.core.node.Evaluable;
import org.structr.core.node.IterableAdapter;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.StructrNodeFactory;

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
public class NodeList extends StructrNode implements List<StructrNode>, Decorable<StructrNode>, Evaluable
{
	private static final Logger logger = Logger.getLogger(NodeList.class.getName());

	private static final String PARENT_KEY = "parent";
	private static final String ICON_SRC = "/images/application_view_list.png";

	private Set<Decorator<StructrNode>> decorators = new LinkedHashSet<Decorator<StructrNode>>();
	private Set<Evaluator> evaluators = new LinkedHashSet<Evaluator>();

	@Override
	public String getIconSrc()
	{
		return ICON_SRC;
	}

	/**
	 * Returns the first node of this list, or null if this list is empty.
	 *
	 * @return the first node of this list, or null
	 */
	public StructrNode getFirstNode()
	{
		return((StructrNode)Services.createCommand(NodeFactoryCommand.class).execute(getFirstRawNode()));
	}

	/**
	 * Returns the first raw node of this list, or null if this list is empty
	 *
	 * @return the first raw node of this list, or null
	 */
	public Node getFirstRawNode()
	{
		Node rootNode = getNode();
		return(getRelatedNode(rootNode, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING));
	}

	/**
	 * Returns the last node of this list, or null if this list is empty.
	 *
	 * @return the last node of this list, or null
	 */
	public StructrNode getLastNode()
	{
		return((StructrNode)Services.createCommand(NodeFactoryCommand.class).execute(getLastRawNode()));
	}

	/**
	 * Returns the last raw node of this list, or null if this list is empty
	 *
	 * @return the last raw node of this list, or null
	 */
	public Node getLastRawNode()
	{
		Node rootNode = getNode();
		return(getRelatedNode(rootNode, RelType.LAST_LIST_ENTRY, Direction.OUTGOING));
	}

	// ----- interface List<StructrNode> -----
	/**
	 * Returns the size of this node list. Note that setting or removing evaluators can change
	 * the value returned by this method. This method will take time proportional to the number
	 * of elements in the list.
	 *
	 * @return the size of this list with the current set of evaluators
	 */
	@Override
	public int size()
	{
		int ret = 0;

		for(Node node : getRawNodes())
		{
			ret++;
		}

		return(ret);
	}

	/**
	 * Indicates whether this list is empty. Note that in contrast to the size() methods,
	 * this is an O(1) operation and should be seen as the preferred way to check whether a list
	 * is empty or not.
	 *
	 * @return true if this list is empty
	 */
	@Override
	public boolean isEmpty()
	{
		Node startNode = this.getNode();
		boolean hasElements = (startNode != null && startNode.hasRelationship(RelType.NEXT_LIST_ENTRY, Direction.OUTGOING));

		return (!hasElements);
	}

	/**
	 * Indicates whether this list contains the given element. This method can take time
	 * proportional to the number of elements in the list.
	 *
	 * @param o
	 * @return
	 */
	@Override
	public boolean contains(Object o)
	{
		StructrNode n = (StructrNode)o;

		for(StructrNode node : getNodes())
		{
			if(node.equals(n))
			{
				return(true);
			}
		}

		return(false);

	}

	/**
	 * Returns an iterator over the elements of this list (according to the current set
	 * of evaluators).
	 *
	 * @return the iterator
	 */
	@Override
	public Iterator<StructrNode> iterator()
	{
		return(getNodes().iterator());
	}

	/**
	 * Returns an array containing all the elements in this list according to the
	 * current set of evaluators.
	 *
	 * @return the array
	 */
	@Override
	public Object[] toArray()
	{
		return(getNodeList().toArray());
	}

	/**
	 * Returns an array containing all the elements in this list according to the
	 * current set of evaluators.
	 *
	 * @param <T>
	 * @param a
	 * @return
	 */
	@Override
	public <T> T[] toArray(T[] a)
	{
		return(getNodeList().toArray(a));
	}

	/**
	 * Applies all decorators that are set on this list and adds the node
	 * to the end of this list. This method runs in constant time.
	 * (Implementation note: keep LAST pointer!)
	 *
	 * <p>
	 * If a collection refuses to add a particular element for any reason
	 * other than that it already contains the element, it must throw an
	 * exception (rather than returning false). This preserves the
	 * invariant that a collection always contains the specified element
	 * after this call returns.
	 * </p>
	 *
	 * @param node the node to add
	 * @return true if this collection changed as a result of this call
	 */
	@Override
	public boolean add(StructrNode node)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Removes the given node from this list.
	 *
	 * @param node
	 * @return true if this list contained the given element
	 */
	@Override
	public boolean remove(Object node)
	{
		int index = indexOf(node);
		if(index >= 0)
		{
			return(deleteAt(index) != null);
		}

		return(false);
	}

	/**
	 * Indicates whether this list contains all of the elements in the
	 * given collection.
	 *
	 * @param c
	 * @return true or false
	 */
	@Override
	public boolean containsAll(Collection<?> c)
	{
		return(getNodeList().containsAll(c));
	}

	/**
	 * Adds all the elements in the given collection to this list, applying
	 * any decorator that is set on this list before addition.
	 *
	 * @param c
	 * @return
	 */
	@Override
	public boolean addAll(Collection<? extends StructrNode> c)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Inserts all the elements in the given collection at the given index, applying
	 * any decorator that is set on this list before addition, respecting any evaluator
	 * that is currently set on this list.
	 *
	 * @param index
	 * @param c
	 * @return
	 */
	@Override
	public boolean addAll(int index, Collection<? extends StructrNode> c)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Removes all elements in the given collection from this list.
	 *
	 * @param c
	 * @return
	 */
	@Override
	public boolean removeAll(Collection<?> c)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Retains only the elements in this list that are contained in the given
	 * collection.
	 *
	 * @param c
	 * @return
	 */
	@Override
	public boolean retainAll(Collection<?> c)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Clears this list.
	 */
	@Override
	public void clear()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Returns the element at the given index, or null if no element exists,
	 * with respect to the evaluators that are currently set on this list.
	 * Note that this behaviour differs from the default List behaviour, as
	 * no ArrayIndexOutOfBoundsException is thrown.
	 *
	 * @param index
	 * @return
	 */
	@Override
	public StructrNode get(int index)
	{
		Node node = getNodeAt(index);

		if(node != null)
		{
			return((StructrNode)Services.createCommand(NodeFactoryCommand.class).execute(node));
		}

		return(null);
	}

	/**
	 * Replaces the element at the given position with the given element, with
	 * respect to the evaluators that are currently set on this list. This
	 * method will only work when there already is an element at the given index.
	 *
	 *
	 * @param index
	 * @param element
	 * @return
	 */
	@Override
	public StructrNode set(int index, StructrNode element)
	{
		Node node = getNodeAt(index);
		replaceNode(node, element.getNode());

		return((StructrNode)Services.createCommand(NodeFactoryCommand.class).execute(node));
	}

	/**
	 * Inserts the given element at the given position (with respect to the
	 * evaluators that are currently set on this list).
	 *
	 * @param index
	 * @param element
	 */
	@Override
	public void add(int index, StructrNode element)
	{
		Node node = getNodeAt(index);
		if(node != null)
		{
			insertAt(index, element.getNode());
		}
	}

	/**
	 * Removes the element at the given position (with respect to the evaluators
	 * that are currently set on this list.)
	 *
	 * @param index
	 * @return
	 */
	@Override
	public StructrNode remove(int index)
	{
		Node node = deleteAt(index);
		if(node != null)
		{
			return((StructrNode)Services.createCommand(NodeFactoryCommand.class).execute(node));
		}
		
		return(null);
	}

	/**
	 * Returns the position of the given element in this list
	 * @param o
	 * @return
	 */
	@Override
	public int indexOf(Object o)
	{
		return(indexOf((Node)o));
	}

	/**
	 * Returns the last position of the given element in this list, with
	 * respect to the evaluators that are currently set on this list.
	 *
	 * (implementation note: keep LAST pointer!)
	 *
	 * @param o
	 * @return
	 */
	@Override
	public int lastIndexOf(Object o)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Returns a ListIterator containing all elements in this list, with
	 * respect to the evaluators that are currently set.
	 *
	 * @return
	 */
	@Override
	public ListIterator<StructrNode> listIterator()
	{
		throw new UnsupportedOperationException("Bi-directional iteration is not yet supported by this class.");
	}

	/**
	 * Returns a ListIterator containing all elements in this list, starting
	 * from the given index, with respect to the evaluators that are currently
	 * set on this list.
	 *
	 * @param index
	 * @return
	 */
	@Override
	public ListIterator<StructrNode> listIterator(int index)
	{
		throw new UnsupportedOperationException("Bi-directional iteration is not yet supported by this class.");
	}

	/**
	 * Returns a sublist of this list, starting at (including) fromIndex,
	 * ending at (not including) toIndex, with respect to the evaluators that are
	 * currently set on this list.
	 *
	 * @param fromIndex
	 * @param toIndex
	 * @return
	 */
	@Override
	public List<StructrNode> subList(int fromIndex, int toIndex)
	{
		// TODO: return a new NodeList instance with the given bounds

		throw new UnsupportedOperationException("Not supported yet.");
	}

	// ----- interface Decorable<StructrNode>
	@Override
	public void addDecorator(Decorator<StructrNode> d)
	{
		decorators.add(d);
	}

	@Override
	public void removeDecorator(Decorator<StructrNode> d)
	{
		decorators.remove(d);
	}

	// ----- interface Evaluable -----
	@Override
	public void addEvaluator(Evaluator e)
	{
		evaluators.add(e);
	}

	@Override
	public void removeEvaluator(Evaluator e)
	{
		evaluators.remove(e);
	}

	// ----- private methods -----
	private List<StructrNode> getNodeList()
	{
		List<StructrNode> ret = new LinkedList<StructrNode>();
		for(StructrNode node : getNodes())
		{
			ret.add(node);
		}

		return(ret);
	}

	private Iterable<StructrNode> getNodes()
	{
		return(
			new IterableAdapter<Node, StructrNode>(
				getRawNodes(),
				new StructrNodeFactory()
			)
		);
	}

	private Iterable<Node> getRawNodes()
	{
		// create traversal description
		TraversalDescription td = createTraversalDescription();
		Traverser traverser = td.traverse(getNode());

		return(traverser.nodes());
	}

	private TraversalDescription createTraversalDescription()
	{
		TraversalDescription ret = Traversal.description().depthFirst();
		ret = ret.relationships(RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);
		ret = ret.evaluator(new ParentIdEvaluator());

		// add list evaluators
		for(Evaluator evaluator : evaluators)
		{
			ret = ret.evaluator(evaluator);
		}

		return(ret);
	}

	private Node deleteAt(int index)
	{
		Node node = getNodeAt(index);
		Node rootNode = getNode();
		Node previous = getRelatedNode(node, RelType.NEXT_LIST_ENTRY, Direction.INCOMING);
		Node next = getRelatedNode(node, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);

		if(previous != null && next != null)		// some node in the middle (may be the first node)
		{
			// delete relationship from previous node to current node
			deleteRelationship(previous, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);

			// delete relationship from current node to next node
			deleteRelationship(node, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);

			// create relationship between previous and next node
			createRelationship(previous, next, RelType.NEXT_LIST_ENTRY);

		} else
		if(previous != null && next == null)		// last node
		{
			// delete relationship from previous node to current node
			deleteRelationship(previous, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);

			// delete relationship from root node to last node (LAST_LIST_ENTRY
			deleteRelationship(rootNode, RelType.LAST_LIST_ENTRY, Direction.OUTGOING);

			// create relationship from root node to new last node (previous)
			createRelationship(rootNode, previous, RelType.LAST_LIST_ENTRY);
		}

		return(node);
	}

	private void insertAt(int index, Node toInsert)
	{
		Node node = getNodeAt(index - 1);
		Node next = getRelatedNode(node, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);

		// delete relationship from current node to next node
		deleteRelationship(node, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);

		// create relationship from current node to inserted node
		createRelationship(node, toInsert, RelType.NEXT_LIST_ENTRY);

		// create relationship from inserted node to old next node
		createRelationship(toInsert, next, RelType.NEXT_LIST_ENTRY);
	}

	private void replaceNode(Node node, Node replacement)
	{
		int pos = indexOf(node);
		if(pos >= 0)
		{
			deleteAt(pos);
			insertAt(pos, replacement);
		}
	}

	private Node getRelatedNode(Node startNode, RelType relationshipType, Direction direction)
	{
		if(startNode != null)
		{
			Iterable<Relationship> rels = startNode.getRelationships(relationshipType, direction);
			for(Relationship rel : rels)
			{
				if(rel.hasProperty(PARENT_KEY))
				{
					Object parent = rel.getProperty(PARENT_KEY);

					if(parent instanceof Long && ((Long)parent).equals(getNodeId()))
					{
						return(rel.getEndNode());
					}
				}
			}
		}

		return(null);
	}

	/**
	 * Returns the node at the given position, or null if no node is found.
	 *
	 * @param index
	 * @return the node at index or null
	 */
	private Node getNodeAt(int index)
	{
		int pos = 0;

		for(Node node : getRawNodes())
		{
			if(pos++ == index)
			{
				return(node);
			}
		}

		return(null);
	}

	private int indexOf(Node node)
	{
		int ret = 0;

		for(Node n : getRawNodes())
		{
			if(node.equals(n))
			{
				return(ret);
			}

			ret++;
		}

		return(-1);

	}

	private void deleteRelationship(Node startNode, RelType relationshipType, Direction direction)
	{
		if(startNode != null)
		{
			Iterable<Relationship> rels = startNode.getRelationships(relationshipType, direction);
			for(Relationship rel : rels)
			{
				if(rel.hasProperty(PARENT_KEY))
				{
					Object parent = rel.getProperty(PARENT_KEY);

					if(parent instanceof Long && ((Long)parent).equals(getNodeId()))
					{
						rel.delete();
					}
				}
			}
		}
	}

	private void createRelationship(Node startNode, Node endNode, RelType relationshipType)
	{
		if(startNode != null && endNode != null)
		{
			Relationship rel = startNode.createRelationshipTo(endNode, relationshipType);
			rel.setProperty(PARENT_KEY, new Long(getNodeId()));
		}
	}

	// ----- nested classes -----
	private class ParentIdEvaluator implements Evaluator
	{
		@Override
		public Evaluation evaluate(Path path)
		{
			Relationship rel = path.lastRelationship();
			if(rel.hasProperty(PARENT_KEY))
			{
				Object parent = rel.getProperty(PARENT_KEY);

				if(parent instanceof Long && ((Long)parent).equals(getNodeId()))
				{
					return(Evaluation.INCLUDE_AND_CONTINUE);
				}

			}

			// TODO: find out if EXCLUDE_AND_CONTINUE is the right choice here!

			return(Evaluation.EXCLUDE_AND_CONTINUE);
		}
	}
	
}
