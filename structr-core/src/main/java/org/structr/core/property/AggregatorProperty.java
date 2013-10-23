/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.Aggregation;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.Notion;

/**
 * A property that uses an {@link Aggregation} to return a list of entities.
 *
 * @author Christian Morgner
 */
public class AggregatorProperty<T> extends AbstractReadOnlyCollectionProperty<T> {
	
	private Aggregation aggregation = null;
	
	public AggregatorProperty(String name, Aggregation aggregator) {
		super(name);
		
		this.aggregation = aggregator;
	}
	
	@Override
	public List<T> getProperty(SecurityContext securityContext, GraphObject currentObject, boolean applyConverter) {
		
		if(currentObject != null && currentObject instanceof AbstractNode) {
			
			NodeInterface sourceNode  = (NodeInterface)currentObject;
			List<NodeInterface> nodes = new LinkedList<NodeInterface>();

			// 1. step: add all nodes
			for(CollectionProperty<?, ?> property : aggregation.getAggregationProperties()) {
				
				nodes.addAll(sourceNode.getProperty(property));
			}

			// 2. step: sort nodes according to comparator
			Comparator<NodeInterface> comparator = aggregation.getComparator();
			if(nodes.isEmpty() && comparator != null) {
				Collections.sort(nodes, comparator);
			}

			// 3. step: apply notions depending on type
			List results = new LinkedList();

			try {
				for(NodeInterface node : nodes) {

					Notion notion = aggregation.getNotionForType(node.getClass());
					if(notion != null) {

						results.add(notion.getAdapterForGetter(securityContext).adapt(node));

					} else {

						results.add(node);
					}
				}

			} catch(Throwable t) {
				t.printStackTrace();
			}

			return results;
		}
		
		return Collections.emptyList();
	}
	
	@Override
	public Class relatedType() {
		return AbstractNode.class;
	}
	
	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public Integer getSortType() {
		return null;
	}
}
