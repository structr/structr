/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.converter;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.structr.core.PropertyConverter;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.Notion;

/**
 * A read-only converter that returns an aggregated list of related nodes of different types.
 * 
 * @author Christian Morgner
 */
public class AggregatingConverter extends PropertyConverter {

	@Override
	public Object convertForSetter(Object source, Value value) {
		// read only
		return null;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {
		
		if(currentObject != null && value != null && currentObject instanceof AbstractNode) {
			
			Object aggregatorValue = value.get(securityContext);
			if(aggregatorValue != null && aggregatorValue instanceof Aggregation) {
				
				Aggregation aggregation  = (Aggregation)aggregatorValue;
				AbstractNode sourceNode  = (AbstractNode)currentObject;
				List<AbstractNode> nodes = new LinkedList<AbstractNode>();

				// 1. step: add all nodes
				for(Class type : aggregation.getAggregationTypes()) {
					nodes.addAll(sourceNode.getRelatedNodes(type));
				}

				// 2. step: sort nodes according to comparator
				Comparator<AbstractNode> comparator = aggregation.getComparator();
				if(nodes.isEmpty() && comparator != null) {
					Collections.sort(nodes, comparator);
				}
				
				// 3. step: apply notions depending on type
				List results = new LinkedList();
				
				try {
					for(AbstractNode node : nodes) {
						
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
		}
		
		return Collections.emptyList();
	}	
}
