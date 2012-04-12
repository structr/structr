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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.node.NodeService;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

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
	
	
	public static List<AbstractRelationship> getChildRelationships(final AbstractNode node, final String resourceId, final String localComponentId) {
		
		List<AbstractRelationship> rels = new LinkedList<AbstractRelationship>();

		for (AbstractRelationship abstractRelationship : node.getOutgoingRelationships(RelType.CONTAINS)) {

			Relationship rel = abstractRelationship.getRelationship();

			if (rel.hasProperty(resourceId) || rel.hasProperty("*")) {

				AbstractNode endNode = abstractRelationship.getEndNode();

				if ((localComponentId != null) && ((endNode instanceof Content) || (endNode instanceof Component))) {

					// only add relationship if (nested) componentId matches
					if (rel.hasProperty(localComponentId)) {

						rels.add(abstractRelationship);

					}
					
				} else {

					rels.add(abstractRelationship);

				}

			}

		}

		Collections.sort(rels, new Comparator<AbstractRelationship>() {

			@Override
			public int compare(AbstractRelationship o1, AbstractRelationship o2) {

				Integer pos1 = getPosition(o1, resourceId);
				Integer pos2 = getPosition(o2, resourceId);

				return pos1.compareTo(pos2);
			}

		});

		return rels;
	}

	public static int getPosition(final AbstractRelationship relationship, final String resourceId) {

		final Relationship rel = relationship.getRelationship();
		Integer position       = 0;

		try {

			Map<Integer, Relationship> sortedRelationshipMap = new TreeMap<Integer, Relationship>();
			Object prop                                      = null;
			final String key;

			// "*" is a wildcard for "matches any resource id"
			// TOOD: use pattern matching here?
			if (rel.hasProperty("*")) {

				prop = rel.getProperty("*");
				key  = "*";

			} else if (rel.hasProperty(resourceId)) {

				prop = rel.getProperty(resourceId);
				key  = resourceId;

			} else {

				key = null;

			}

			if ((key != null) && (prop != null)) {

				if (prop instanceof Integer) {

					position = (Integer) prop;

				} else if (prop instanceof String) {

					position = Integer.parseInt((String) prop);

				} else {

					throw new java.lang.IllegalArgumentException("Expected Integer or String");

				}

				Integer originalPos = position;

				// find free slot
				while (sortedRelationshipMap.containsKey(position)) {

					position++;

				}

				sortedRelationshipMap.put(position, rel);

				if (originalPos != position) {

					final Integer newPos = position;

					Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {

						@Override
						public Object execute() throws FrameworkException {

							rel.setProperty(key, newPos);

							return null;
						}

					});

				}

			}

		} catch (Throwable t) {

			// fail fast, no check
			logger.log(Level.SEVERE, "While reading property " + resourceId, t);
		}

		return position;
	}
}









































































