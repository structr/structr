/**
 * Copyright (C) 2010-2015 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeFactory;
import org.structr.core.notion.Notion;
import org.structr.core.traversal.TraverserInterface;

/**
 * A property that uses a {@link TraverserInterface} to return a collection of entities.
 *
 *
 */
public class TraverserCollectionProperty<T extends AbstractNode> extends AbstractReadOnlyCollectionProperty<T> {

	private TraverserInterface traverserInterface = null;

	public TraverserCollectionProperty(String name, TraverserInterface traverser) {
		super(name);

		this.traverserInterface = traverser;

		// make us known to the entity context
		StructrApp.getConfiguration().registerConvertedProperty(this);
	}

	@Override
	public List<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public List<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final org.neo4j.helpers.Predicate<GraphObject> predicate) {

		TraversalDescription description = traverserInterface.getTraversalDescription(securityContext);
		AbstractNode currentNode = (AbstractNode)obj;

		Comparator<AbstractNode> comparator = traverserInterface.getComparator();
		List<T> nodes = getTraversalResults(securityContext, comparator, description, currentNode);

		if (traverserInterface.collapseSingleResult() && nodes.isEmpty()) {
			return null;
		}

		List<T> results = traverserInterface.transformResult(nodes);
		// important: remove results from this iteration
		traverserInterface.cleanup();

		return results;
	}

	// ----- private methods -----
	private List<T> getTraversalResults(SecurityContext securityContext, Comparator<AbstractNode> comparator, TraversalDescription traversalDescription, AbstractNode node) {

		// use traverser
		Iterable<Node> nodes = traversalDescription.traverse(node.getNode()).nodes();

		// collect results and convert nodes into structr nodes
		NodeFactory<T> nodeFactory = new NodeFactory<>(securityContext);
		List<T> nodeList           = new LinkedList<>();

		for(Node n : nodes) {

			T abstractNode = nodeFactory.instantiate(n);
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
	public Class valueType() {
		return relatedType();
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public Integer getSortType() {
		return null;
	}

	@Override
	public PropertyConverter<?, List<T>> inputConverter(SecurityContext securityContext) {

		Notion notion = traverserInterface.getNotion();
		if (notion != null) {

			return notion.getCollectionConverter(securityContext);
		}

		return null;
	}
}
