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

import java.util.LinkedHashMap;
import java.util.Map;
import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
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

	static {

		EntityContext.registerPropertySet(Element.class, PropertyView.All, UiKey.values());
		EntityContext.registerPropertySet(Element.class, PropertyView.Public, UiKey.values());
		EntityContext.registerPropertySet(Element.class, PropertyView.Ui, UiKey.values());
		EntityContext.registerEntityRelation(Element.class, Component.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Element.class, Resource.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Element.class, Element.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Element.class, Content.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);

		EntityContext.registerSearchablePropertySet(Element.class, NodeIndex.fulltext.name(), UiKey.values());
		EntityContext.registerSearchablePropertySet(Element.class, NodeIndex.keyword.name(), UiKey.values());

//              EntityContext.registerEntityRelation(Element.class,     Resource.class,         RelType.LINK,           Direction.OUTGOING, Cardinality.ManyToOne);

	}

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
	public Iterable<String> getPropertyKeys(final String propertyView) {
		
		collectProperties(this, 0, 5);

		// test: add "other" properties & keys
		for(String key : super.getPropertyKeys(propertyView)) {
			test.put(key, super.getProperty(key));
		}
		
		return test.keySet();
	}
	
	@Override
	public Object getProperty(String key) {
		
		if(test.containsKey(key)) {
			return test.get(key);
		}
		
		return super.getProperty(key);
	}
	
	private Map<String, Object> test = new LinkedHashMap<String, Object>();

	// recursive structr-component-class collector
	private void collectProperties(AbstractNode startNode, int depth, int maxDepth) {
		
		if(depth > maxDepth) {
			return;
		}
		
		String dataClass = startNode.getStringProperty("_html_data-class");
		if(dataClass != null && !dataClass.isEmpty()) {
			test.put("data-class", dataClass);
		}

		// recurse only if data-class is set
		if(test.containsKey("data-class")) {
			
			String dataKey = startNode.getStringProperty("data-key");
			if(dataKey != null) {
				test.put(dataKey, getChildContent(startNode));
			}

			for(AbstractRelationship rel : startNode.getOutgoingRelationships(RelType.CONTAINS)) {

				AbstractNode endNode = rel.getEndNode();
				collectProperties(endNode, depth, maxDepth+1);
			}
		}
	}
	
	private String getChildContent(AbstractNode node) {

		if(node instanceof Content) {
		
			return node.getStringProperty("content");
			
		} else {

			for(AbstractRelationship rel : node.getOutgoingRelationships(RelType.CONTAINS)) {

				AbstractNode endNode = rel.getEndNode();

				if(endNode instanceof Content) {
					return endNode.getStringProperty("content");
				}
			}
		}
		
		return null;
	}
}
