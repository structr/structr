/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.swing.JOptionPane;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Decorable;
import org.structr.core.Decorator;
import org.structr.core.Predicate;
import org.structr.core.Services;
import org.structr.core.node.Evaluable;
import org.structr.core.node.GraphDatabaseCommand;
import org.structr.core.node.IterableAdapter;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.StructrNodeFactory;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

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
	private int maxLength = -1;

	public NodeList()
	{
		this(-1);
	}

	public NodeList(int maxLength)
	{
		this.maxLength = maxLength;
	}

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

	/**
	 * Returns the maximum traversal depth of this list.
	 *
	 * @return
	 */
	public int getMaxLength()
	{
		return(maxLength);
	}

	/**
	 * Sets the maximum travesal depth of this list to the given value.
	 * 
	 * @param maxLength
	 */
	public void setMaxLength(int maxLength)
	{
		this.maxLength = maxLength;
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
	 * @param toRemove the node to add
	 * @return true if this collection changed as a result of this call
	 */
	@Override
	public boolean add(StructrNode toAdd)
	{
		boolean ret = false;

		if(toAdd != null)
		{
			ret = appendNodeToList(toAdd.getNode());
		}

		return(ret);
	}

	/**
	 * Removes the given node from this list.
	 *
	 * @param toRemove
	 * @return true if this list contained the given element
	 */
	@Override
	public boolean remove(Object node)
	{
		return(removeNodeFromList((Node)node));
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
		boolean ret = false;

		for(StructrNode node : c)
		{
			ret |= add(node);
		}

		return(ret);
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
		int internalIndex = index;
		boolean ret = true;	// how can we know if the collection changed??

		for(StructrNode node : c)
		{
			add(internalIndex, node);
			internalIndex++;
		}

		return(ret);
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
		boolean ret = false;

		for(Object node : c)
		{
			ret |= remove((StructrNode)node);
		}

		return(ret);
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
		boolean ret = false;

		for(StructrNode node : getNodes())
		{
			if(!c.contains(node))
			{
				remove(node);
				ret = true;
			}
		}

		return(ret);
	}

	/**
	 * Clears this list. Due to the fact that this method has to remove all
	 * elements from the linked list, this method will take time proportional
	 * to the size of the list, with a relativley large constant factor.
	 */
	@Override
	public void clear()
	{
		for(StructrNode node : getNodes())
		{
			remove(node);
		}

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
		throw new UnsupportedOperationException("Not supported yet.");
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
		int size = this.size();

		if(index >= size)
		{
			appendNodeToList(element.getNode());

		} else
		{
			insertNodeIntoList(index, element.getNode());
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
		System.out.println("removing node #" + index);

		Node node = getNodeAt(index);

		if(node != null)
		{
			removeNodeFromList(node);

		} else
		{
			System.out.println("node was null!");
		}

		return((StructrNode)Services.createCommand(NodeFactoryCommand.class).execute(node));
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
		//return a new NodeList instance with the given bounds
		Node startNode = getNodeAt(fromIndex);
		NodeList ret = new NodeList(toIndex - fromIndex);

		ret.init(startNode);

		return(ret);
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
		// ret = ret.evaluator(new ParentIdEvaluator());

		// add list evaluators
		for(Evaluator evaluator : evaluators)
		{
			ret = ret.evaluator(evaluator);
		}

		if(maxLength >= 0)
		{
			ret = ret.evaluator(new MaxLengthEvaluator());
		}

		return(ret);
	}


	/**
	 * Removes the given node from this list.
	 *
	 * @param toRemove
	 * @return true if the list was modified as a result of this operation
	 */
	private boolean removeNodeFromList(Node toRemove)
	{
		boolean listWasModified = false;

		if(toRemove != null && isMember(toRemove))
		{
			// node is not null and part of this list
			Node rootNode = getNode();
			Node previousNode = getRelatedNode(toRemove, RelType.NEXT_LIST_ENTRY, Direction.INCOMING);
			Node nextNode = getRelatedNode(toRemove, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);

			// delete relationship from previousNode to toRemove
			System.out.print("deleting relationship from previousNode: ");
			deleteRelationship(previousNode, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);

			// delete relationship from toRemove to nextNode (if exists)
			System.out.print("deleting relationship from toRemove: ");
			deleteRelationship(toRemove, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);

			// if nextNode exists
			if(nextNode != null)
			{
				// create relationship to next node
				createRelationship(previousNode, nextNode, RelType.NEXT_LIST_ENTRY);

			} else
			{
				// delete LAST relationship from rootNode
				deleteRelationship(rootNode, RelType.LAST_LIST_ENTRY, Direction.OUTGOING);

				// create LAST relationship from rootNode to previousNode
				createRelationship(rootNode, previousNode, RelType.LAST_LIST_ENTRY);
			}

			listWasModified = true;

		} else
		{
			System.out.println("toRemove was null or toRemove not member!");
		}


		return(listWasModified);
	}

	private boolean appendNodeToList(Node toAdd)
	{
		boolean listWasModified = false;

		if(toAdd != null && !isMember(toAdd))
		{
			// node is not null and not already a member of this list
			Node rootNode = getNode();
			Node lastNode = getRelatedNode(rootNode, RelType.LAST_LIST_ENTRY, Direction.OUTGOING);

			if(lastNode != null)
			{
				// create NEXT relationship from lastNode to toAdd
				createRelationship(lastNode, toAdd, RelType.NEXT_LIST_ENTRY);

				// delete LAST relationship from rootNode
				deleteRelationship(rootNode, RelType.LAST_LIST_ENTRY, Direction.OUTGOING);

				// create LAST relationship from rootNode to previousNode
				createRelationship(rootNode, toAdd, RelType.LAST_LIST_ENTRY);

				listWasModified = true;

			} else
			{
				// list is empty, add node as last node
				createRelationship(rootNode, toAdd, RelType.NEXT_LIST_ENTRY);
				createRelationship(rootNode, toAdd, RelType.LAST_LIST_ENTRY);

				listWasModified = true;
			}
		}

		return(listWasModified);
	}

	private void insertNodeIntoList(int index, Node toInsert)
	{
		Node node = getNodeAt(index);

		if(node != null)
		{
			insertNodeBefore(node, toInsert);
		}
	}

	private void insertNodeBefore(Node node, Node toInsert)
	{
		if(node != null && toInsert != null && !isMember(toInsert))
		{
			Node previousNode = getRelatedNode(node, RelType.NEXT_LIST_ENTRY, Direction.INCOMING);

			// delete relationship from previousNode to node
			deleteRelationship(previousNode, RelType.NEXT_LIST_ENTRY, Direction.OUTGOING);

			// create relationship from previousNode to toInsert
			createRelationship(previousNode, toInsert, RelType.NEXT_LIST_ENTRY);

			// create relationship from toInsert to node
			createRelationship(toInsert, node, RelType.NEXT_LIST_ENTRY);
		}
	}

	private boolean isMember(Node node)
	{
		return(getRelatedNode(node, RelType.NEXT_LIST_ENTRY, Direction.INCOMING) != null);
	}

	/**
	 * Returns a node, following the given relationship and direction from startNode.
	 *
	 * @param startNode
	 * @param relationshipType
	 * @param direction
	 * @return true if the given node is part of the list this node contains
	 */
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
						if(direction.equals(Direction.INCOMING))
						{
							return(rel.getStartNode());

						} else
						{
							return(rel.getEndNode());
						}
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
			System.out.println("trying to delete relationship " + startNode.getId() + " -> " + relationshipType);

			Iterable<Relationship> rels = startNode.getRelationships(relationshipType, direction);
			for(Relationship rel : rels)
			{
				if(rel.hasProperty(PARENT_KEY))
				{
					Object parent = rel.getProperty(PARENT_KEY);

					if(parent instanceof Long && ((Long)parent).equals(getNodeId()))
					{
						System.out.println("deleting relationship!");

						rel.delete();
					} else
					{
						System.out.println("NOT deleting relationship, parent id mismatch!");
					}
				} else
				{
					System.out.println("NOT deleting relationship, no parent id found!");

				}
			}
		} else
		{
			System.out.println("startNode was null!");
		}
	}

	private void createRelationship(Node startNode, Node endNode, RelType relationshipType)
	{
		if(startNode != null && endNode != null)
		{
			System.out.println("creating relationship..");

			Relationship rel = startNode.createRelationshipTo(endNode, relationshipType);
			rel.setProperty(PARENT_KEY, new Long(getNodeId()));

			System.out.println("NEW relationship: " + startNode.getId() + " -> " + relationshipType + " -> " + endNode.getId());
		}
	}

	// ----- nested classes -----
	private class ParentIdEvaluator implements Evaluator
	{
		@Override
		public Evaluation evaluate(Path path)
		{
			/*
			Relationship rel = path.lastRelationship();

			if(rel != null && rel.hasProperty(PARENT_KEY))
			{
				Object parent = rel.getProperty(PARENT_KEY);

				if(parent instanceof Long && ((Long)parent).equals(getNodeId()))
				{
					return(Evaluation.INCLUDE_AND_CONTINUE);
				}

			}

			// TODO: find out if EXCLUDE_AND_CONTINUE is the right choice here!

			return(Evaluation.EXCLUDE_AND_CONTINUE);
			 *
			 */

			return(Evaluation.INCLUDE_AND_CONTINUE);
		}
	}

	private class MaxLengthEvaluator implements Evaluator
	{
		@Override
		public Evaluation evaluate(Path path)
		{
			if(path.length() > maxLength)
			{
				return(Evaluation.EXCLUDE_AND_PRUNE);
			}

			return(Evaluation.INCLUDE_AND_CONTINUE);
		}
	}

	public static void main(String[] args)
	{
		Services.initialize(prepareStandaloneContext());

		Services.createCommand(TransactionCommand.class).execute(new StructrTransaction()
		{
			@Override
			public Object execute() throws Throwable
			{
				GraphDatabaseService graphDb = (GraphDatabaseService)Services.createCommand(GraphDatabaseCommand.class).execute();
				Command factory = Services.createCommand(NodeFactoryCommand.class);
				NodeList nodeList = null;

				for(Node node : graphDb.getAllNodes())
				{
					StructrNode n = (StructrNode)factory.execute(node);

					System.out.println("node: " + node);

					if(n instanceof NodeList)
					{
						nodeList = (NodeList)n;
						break;
					}
				}

				if(nodeList == null)
				{
					Node node = graphDb.createNode();
					node.setProperty(TYPE_KEY, "NodeList");
					graphDb.getReferenceNode().createRelationshipTo(node, RelType.HAS_CHILD);

					nodeList = (NodeList)factory.execute(node);
				}

				if(nodeList != null)
				{
					boolean exit = false;
					
					while(!exit)
					{
						try
						{
							System.out.println("#######################");
							System.out.println("list size: " + nodeList.size());
							for(StructrNode node : nodeList)
							{
								System.out.println(node.getId() + ": " + node);
								
								for(Relationship rel : node.getNode().getRelationships(Direction.OUTGOING))
								{
									System.out.println("          " + rel.getId() + ": " + rel.getType() + " -> " + rel.getEndNode());
								}
								System.out.println();

							}

							String line = JOptionPane.showInputDialog(null, "Kommando:");

							if("exit".equals(line))
							{
								exit = true;
							} else
							if(line.startsWith("add"))
							{
								Node nn = graphDb.createNode();
								nn.setProperty(TYPE_KEY, "PlainText");
								StructrNode newNode = (StructrNode)factory.execute(nn);

								int index = -1;
								try
								{
									index = Integer.parseInt(line.substring(line.indexOf(" ") + 1));

								} catch(Throwable t) {}

								if(index != -1)
								{
									System.out.println("adding node at " + index);
									nodeList.add(index, newNode);
								} else
								{
									System.out.println("appending node");
									nodeList.add(newNode);
								}

							} else
							if(line.startsWith("del"))
							{

								int index = -1;
								try
								{
									index = Integer.parseInt(line.substring(line.indexOf(" ") + 1));

								} catch(Throwable t) {}

								if(index != -1)
								{
									System.out.println("removing node #" + index);
									nodeList.remove(index);

								} else
								{
									System.out.println("removing last node");
									nodeList.remove(nodeList.size() - 1);
								}
							}

						} catch(Throwable t)
						{
							System.out.println(t.getMessage());
						}
					}
				}

				return(null);
			}
		});

		Services.shutdown();
	}

	private static Map<String, Object> prepareStandaloneContext()
	{
		Map<String, Object> context = new Hashtable<String, Object>();

		context.put(Services.DATABASE_PATH_IDENTIFIER, "/opt/structr/structr-tfs");
		context.put(Services.ENTITY_PACKAGES_IDENTIFIER, "org.structr.core.entity");

		// add predicate
		context.put(Services.STRUCTR_PAGE_PREDICATE, new Predicate()
		{
			@Override
			public boolean evaluate(Object obj)
			{
				return(false);
			}
		});

		try
		{
			Class.forName("javax.servlet.ServletContext");

		} catch(Throwable t)
		{
			t.printStackTrace();
		}

		// add synthetic ServletContext
		context.put(Services.SERVLET_CONTEXT, new ServletContext()
		{
			private Vector emptyList = new Vector();
			private Set emptySet = new LinkedHashSet();


			@Override
			public String getContextPath()
			{
				return("/dummy");
			}

			@Override
			public ServletContext getContext(String uripath)
			{
				return(this);
			}

			@Override
			public int getMajorVersion()
			{
				return(0);
			}

			@Override
			public int getMinorVersion()
			{
				return(0);
			}

			@Override
			public String getMimeType(String file)
			{
				return("application/octet-stream");
			}

			@Override
			public Set getResourcePaths(String path)
			{
				return(emptySet);
			}

			@Override
			public URL getResource(String path) throws MalformedURLException
			{
				return(null);
			}

			@Override
			public InputStream getResourceAsStream(String path)
			{
				return(null);
			}

			@Override
			public RequestDispatcher getRequestDispatcher(String path)
			{
				return(null);
			}

			@Override
			public RequestDispatcher getNamedDispatcher(String name)
			{
				return(null);
			}

			@Override
			public Servlet getServlet(String name) throws ServletException
			{
				return(null);
			}

			@Override
			public Enumeration getServlets()
			{
				return(emptyList.elements());
			}

			@Override
			public Enumeration getServletNames()
			{
				return(emptyList.elements());
			}

			@Override
			public void log(String msg)
			{
			}

			@Override
			public void log(Exception exception, String msg)
			{
			}

			@Override
			public void log(String message, Throwable throwable)
			{
			}

			@Override
			public String getRealPath(String path)
			{
				return("/temp/" + path);
			}

			@Override
			public String getServerInfo()
			{
				return("DummyServer");
			}

			@Override
			public String getInitParameter(String name)
			{
				return(null);
			}

			@Override
			public Enumeration getInitParameterNames()
			{
				return(emptyList.elements());
			}

			@Override
			public Object getAttribute(String name)
			{
				return(null);
			}

			@Override
			public Enumeration getAttributeNames()
			{
				return(emptyList.elements());
			}

			@Override
			public void setAttribute(String name, Object object)
			{
			}

			@Override
			public void removeAttribute(String name)
			{
			}

			@Override
			public String getServletContextName()
			{
				return("DummyContext");
			}

		});

		return(context);
	}
}
