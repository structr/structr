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
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.core.entity.RelationClass.Cardinality;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a web element
 *
 * @author axel
 */
public class Element extends AbstractNode {

	protected static final String[] uiAttributes = {
		UiKey.name.name(), UiKey.tag.name(), UiKey.contents.name(), UiKey.elements.name(), UiKey.components.name(), UiKey.resource.name(), "data-class"
	};

	static {

		EntityContext.registerPropertySet(Element.class, PropertyView.All, uiAttributes);
		EntityContext.registerPropertySet(Element.class, PropertyView.Public, uiAttributes);
		EntityContext.registerPropertySet(Element.class, PropertyView.Ui, uiAttributes);
		EntityContext.registerEntityRelation(Element.class, Component.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Element.class, Resource.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Element.class, Element.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Element.class, Content.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);

		EntityContext.registerSearchablePropertySet(Element.class, NodeIndex.fulltext.name(), uiAttributes);
		EntityContext.registerSearchablePropertySet(Element.class, NodeIndex.keyword.name(), uiAttributes);

//              EntityContext.registerEntityRelation(Element.class,     Resource.class,         RelType.LINK,           Direction.OUTGOING, Cardinality.ManyToOne);

	}

	private Map<String, AbstractNode> contentNodes = new WeakHashMap<String, AbstractNode>();
	
	//~--- constant enums -------------------------------------------------

	public enum UiKey implements PropertyKey {
		name, tag, contents, elements, components, resource
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "";
	}
	
	@Override
	public void onNodeInstantiation() {
		
		collectProperties(this, 0, 10);
	}
	
	@Override
	public Iterable<String> getPropertyKeys(final String propertyView) {

		Set<String> augmentedPropertyKeys = new LinkedHashSet<String>();
		
		for(String key : super.getPropertyKeys(propertyView)) {
			augmentedPropertyKeys.add(key);
		}
		
		augmentedPropertyKeys.addAll(contentNodes.keySet());
		
		return augmentedPropertyKeys;
	}
	
	@Override
	public Object getProperty(String key) {
		
		if(contentNodes.containsKey(key)) {
			AbstractNode node = contentNodes.get(key);
			if(node != null && node != this) {
				return node.getStringProperty("content");
			}
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
	private void collectProperties(AbstractNode startNode, int depth, int maxDepth) {
		
		if(depth > maxDepth) {
			return;
		}
		
		String dataClass = startNode.getStringProperty("data-class");
		if(dataClass != null && !dataClass.isEmpty()) {
			contentNodes.put("data-class", this);
		}

		// recurse only if data-class is set
		if(contentNodes.containsKey("data-class")) {
			
			String dataKey = startNode.getStringProperty("data-key");
			if(dataKey != null) {
				contentNodes.put(dataKey, startNode);
			}

			for(AbstractRelationship rel : startNode.getOutgoingRelationships(RelType.CONTAINS)) {

				// type cast is safe her, as this will only work with Elements anyway
				AbstractNode endNode = rel.getEndNode();
				collectProperties(endNode, depth, maxDepth+1);
			}
		}
	}
}
