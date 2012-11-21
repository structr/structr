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

package org.structr.core.converter;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeFactory;
import org.structr.core.notion.Notion;
import org.structr.core.traversal.TraverserInterface;

/**
 *
 * @author Christian Morgner
 */
public class TraversingConverter<T> extends PropertyConverter<Object, T> {

	private static final Logger logger = Logger.getLogger(TraversingConverter.class.getName());

	private TraverserInterface traverserInterface = null;
	
	public TraversingConverter(SecurityContext securityContext, GraphObject entity, TraverserInterface traverserInterface) {
		super(securityContext, entity);
		
		this.traverserInterface = traverserInterface;
	}
	
	@Override
	public Object revert(T source) throws FrameworkException {
		
		if(currentObject != null) {
			
			TraversalDescription description = traverserInterface.getTraversalDescription(securityContext, source);
			AbstractNode currentNode = (AbstractNode)currentObject;

			Comparator<AbstractNode> comparator = traverserInterface.getComparator();
			List<AbstractNode> nodes = getTraversalResults(comparator, description, currentNode);

			if (traverserInterface.collapseSingleResult() && nodes.isEmpty()) {
				return null;
			}

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

				return (T)((traverserInterface.collapseSingleResult() && results.size() == 1) ? results.get(0) : results);

			} else {

				return (T)transformedNodes;
			}
		}
		
		return null;
	}

	@Override
	public T convert(Object source) throws FrameworkException {
		return null;
	}
	
	// ----- private methods -----
	private List<AbstractNode> getTraversalResults(Comparator<AbstractNode> comparator, TraversalDescription traversalDescription, AbstractNode node) throws FrameworkException {

		// use traverser
		Iterable<Node> nodes = traversalDescription.traverse(node.getNode()).nodes();

		// collect results and convert nodes into structr nodes
		NodeFactory nodeFactory = new NodeFactory<AbstractNode>(securityContext);
		List<AbstractNode> nodeList = new LinkedList<AbstractNode>();

		for(Node n : nodes) {
			AbstractNode abstractNode = nodeFactory.createNode(n);
			if(abstractNode != null) {
				nodeList.add(abstractNode);
			}
		}

		// apply comparator
		if(comparator != null) {
			Collections.sort(nodeList, comparator);
		}

		return nodeList;
	}
}
