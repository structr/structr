/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.converter;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.GraphObject;
import org.structr.core.PropertyConverter;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.NodeFactory;
import org.structr.core.notion.Notion;
import org.structr.core.traversal.TraverserInterface;

/**
 *
 * @author Christian Morgner
 */
public class TraversingConverter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(TraversingConverter.class.getName());

	@Override
	public Object convertForGetter(Object source, Value value) {
		
		// source is not used here (can safely be null!)
		
		if(currentObject != null && value != null) {
			
			Object valueObject = value.get();
			if(valueObject != null) {
				
				if(valueObject instanceof TraverserInterface) {
					
					TraverserInterface traverserInterface = (TraverserInterface)valueObject;
					TraversalDescription description = traverserInterface.getTraversalDescription(source);
					AbstractNode currentNode = (AbstractNode)currentObject;

					Comparator<AbstractNode> comparator = traverserInterface.getComparator();

					try {
						List<AbstractNode> nodes = getTraversalResults(comparator, description, currentNode);
						List<GraphObject> transformedNodes = traverserInterface.transformResult(nodes);

						Notion notion = traverserInterface.getNotion();
						if(notion != null && !rawMode) {

							Adapter<GraphObject, Object> adapter = notion.getAdapterForGetter(securityContext);

							List<Object> results = new LinkedList<Object>();
							for(GraphObject obj : transformedNodes) {
								results.add(adapter.adapt(obj));
							}

							// important: remove results from this iteration
							traverserInterface.cleanup();

							return results;

						} else {

							return transformedNodes;
						}

					} catch(FrameworkException fex) {
						logger.log(Level.WARNING, "Error while converting property", fex);
					}
				}
			}
		}
		
		return null;
	}

	@Override
	public Object convertForSetter(Object source, Value value) {
		return source;
	}
	
	// ----- private methods -----
	private List<AbstractNode> getTraversalResults(Comparator<AbstractNode> comparator, TraversalDescription traversalDescription, AbstractNode node) throws FrameworkException {

		// use traverser
		Iterable<Node> nodes = traversalDescription.traverse(node.getNode()).nodes();

		// collect results and convert nodes into structr nodes
		NodeFactory nodeFactory = new NodeFactory<AbstractNode>(securityContext);
		List<AbstractNode> nodeList = new LinkedList<AbstractNode>();

		for(Node n : nodes) {
			String type = n.hasProperty(AbstractNode.Key.type.name()) ? (String)n.getProperty(AbstractNode.Key.type.name()) : "GenericNode";
			nodeList.add(nodeFactory.createNode(securityContext, n, type));
		}

		// apply comparator
		if(comparator != null) {
			Collections.sort(nodeList, comparator);
		}

		return nodeList;
	}
}
