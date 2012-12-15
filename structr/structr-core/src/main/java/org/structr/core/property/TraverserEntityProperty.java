/*
 *  Copyright (C) 2012 Axel Morgner
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
package org.structr.core.property;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.structr.common.SecurityContext;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeFactory;
import org.structr.core.notion.Notion;
import org.structr.core.traversal.TraverserInterface;

/**
 * A property that uses a {@see TraverserInterface} to return a single entity.
 * 
 * @author Christian Morgner
 */
public class TraverserEntityProperty<T extends AbstractNode> extends AbstractReadOnlyProperty<T> {

	private TraverserInterface<T> traverserInterface = null;
	
	public TraverserEntityProperty(String name, TraverserInterface traverser) {
		super(name);
		
		this.traverserInterface = traverser;
		
		// make us known to the entity context
		EntityContext.registerConvertedProperty(this);
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		
		TraversalDescription description = traverserInterface.getTraversalDescription(securityContext);
		AbstractNode currentNode = (AbstractNode)obj;

		Comparator<T> comparator = traverserInterface.getComparator();
		List<T> nodes = getTraversalResults(securityContext, comparator, description, currentNode);

		if (traverserInterface.collapseSingleResult() && nodes.isEmpty()) {
			return null;
		}

		List<T> results = traverserInterface.transformResult(nodes);
		// important: remove results from this iteration
		traverserInterface.cleanup();

		if (results.isEmpty()) {
			return null;
		}
		
		return results.get(0);
	}

	// ----- private methods -----
	private List<T> getTraversalResults(SecurityContext securityContext, Comparator<T> comparator, TraversalDescription traversalDescription, AbstractNode node) {

		// use traverser
		Iterable<Node> nodes = traversalDescription.traverse(node.getNode()).nodes();

		// collect results and convert nodes into structr nodes
		NodeFactory<T> nodeFactory = new NodeFactory<T>(securityContext);
		List<T> nodeList           = new LinkedList<T>();

		for(Node n : nodes) {
			
			T abstractNode = nodeFactory.createNode(n);
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
	
	@Override
	public Class relatedType() {
		return traverserInterface.getResultType();
	}
	
	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext) {
		
		Notion notion = traverserInterface.getNotion();
		if (notion != null) {
		
			return notion.getEntityConverter(securityContext);
		}
		
		return null;
	}
}
