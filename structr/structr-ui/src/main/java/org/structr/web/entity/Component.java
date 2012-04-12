/*
 *  Copyright (C) 2011 Axel Morgner
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



package org.structr.web.entity;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.node.NodeService;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a component. A component is an assembly of elements
 *
 * @author axel
 */
public class Component extends AbstractNode {

	private static final Logger logger = Logger.getLogger(Component.class.getName());
	private static final int MAX_DEPTH = 10;
	
	public enum UiKey implements PropertyKey {
		type, name, structrclass
	}

	static {

		EntityContext.registerPropertySet(Component.class,	PropertyView.All,	UiKey.values());
		EntityContext.registerPropertySet(Component.class,	PropertyView.Public,	UiKey.values());
		EntityContext.registerPropertySet(Component.class,	PropertyView.Ui,	UiKey.values());

		EntityContext.registerEntityRelation(Component.class,	Resource.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Component.class,	Element.class,	RelType.CONTAINS,	Direction.OUTGOING, Cardinality.ManyToMany);
		
		EntityContext.registerSearchablePropertySet(Component.class, NodeService.NodeIndex.fulltext.name(), UiKey.values());
		EntityContext.registerSearchablePropertySet(Component.class, NodeService.NodeIndex.keyword.name(),  UiKey.values());
	}

	private Map<String, AbstractNode> contentNodes = new WeakHashMap<String, AbstractNode>();
	private Set<String> subTypes                   = new LinkedHashSet<String>();

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "";
	}
	
	@Override
	public void onNodeInstantiation() {
		
		collectProperties(getStringProperty(AbstractNode.Key.uuid), this, 0, null);
	}
	
	@Override
	public Iterable<String> getPropertyKeys(final String propertyView) {

		Set<String> augmentedPropertyKeys = new LinkedHashSet<String>();
		
		for(String key : super.getPropertyKeys(propertyView)) {
			augmentedPropertyKeys.add(key);
		}
		
		augmentedPropertyKeys.addAll(contentNodes.keySet());
		
		for(String subType : subTypes) {
			augmentedPropertyKeys.add(subType.toLowerCase().concat("s"));
		}
		
		return augmentedPropertyKeys;
	}
	
	@Override
	public Object getProperty(String key) {
		
		// try local properties first
		if(contentNodes.containsKey(key)) {

			AbstractNode node = contentNodes.get(key);
			if(node != null && node != this) {
				return node.getStringProperty("content");
			}
			
		} else if(subTypes.contains(EntityContext.normalizeEntityName(key))) {
			
			List<Component> results = new LinkedList<Component>();
			
			String componentId = getStringProperty(AbstractNode.Key.uuid);
			
			System.out.println("Collecting children with componentId " + componentId + ")...");
			
			collectChildren(componentId, this, results, 0, null);
			
			return results;
			
		}
		
		return super.getProperty(key);
	}
	
	@Override
	public void setProperty(String key, Object value) throws FrameworkException {

		if(contentNodes.containsKey(key)) {
			
			AbstractNode node = contentNodes.get(key);
			if(node != null) {
				node.setProperty("content", value);
			}
			
		} else {
			
			super.setProperty(key, value);
		}
	}
	
	public Map<String, AbstractNode> getContentNodes() {
		return contentNodes;
	}
	
	// ----- private methods ----
	private void collectProperties(String componentId, AbstractNode startNode, int depth, AbstractRelationship ref) {
		
		if(depth > MAX_DEPTH) {
			return;
		}
		
		if(ref != null) {
		
			if(ref.getRelationship().hasProperty(componentId)) {

				String dataKey = startNode.getStringProperty("data-key");
				if(dataKey != null) {
					contentNodes.put(dataKey, startNode);
					return;
				}
			}
		}

		for(AbstractRelationship rel : startNode.getOutgoingRelationships(RelType.CONTAINS)) {

			AbstractNode endNode = rel.getEndNode();
			
			if(endNode instanceof Component) {
				
				String subType = endNode.getStringProperty(Component.UiKey.structrclass);
				if(subType != null) {
					subTypes.add(subType);
				}
				
			} else {
				
				collectProperties(componentId, endNode, depth+1, rel);
			}
		}
	}
	
	private void collectChildren(String componentId, AbstractNode startNode, List<Component> results, int depth, AbstractRelationship ref) {
		
		if(depth > MAX_DEPTH) {
			return;
		}

		if(ref != null && startNode instanceof Component) {
			results.add((Component)startNode);
			return;
		}

		// recurse
		for(AbstractRelationship rel : startNode.getOutgoingRelationships(RelType.CONTAINS)) {
			collectChildren(componentId, rel.getEndNode(), results, depth+1, rel);
		}
	}
}









































































