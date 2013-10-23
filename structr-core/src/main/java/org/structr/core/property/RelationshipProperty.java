/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.property;

import java.util.List;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.Notion;

/**
 *
 * @author Christian Morgner
 */
public class RelationshipProperty<T extends AbstractRelationship> extends AbstractReadOnlyProperty<List<T>> {

	private Notion notion = null;
	private Class<T> type = null;
	
	public RelationshipProperty(String name, final Class<T> type) {
		this(name, type, null);
	}
	
	public RelationshipProperty(String name, final Class<T> type, final Notion notion) {
		
		super(name);
		
		this.type   = type;
		this.notion = notion;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public Object getValueForEmptyFields() {
		return null;
	}

	@Override
	public String typeName() {
		return "Relationship";
	}

	@Override
	public Class relatedType() {
		return AbstractRelationship.class;
	}

	@Override
	public PropertyConverter<?, List<T>> inputConverter(SecurityContext securityContext) {
		
		if (notion != null) {
			return notion.getCollectionConverter(securityContext);
		}
		
		return null;
	}

	@Override
	public List<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		NodeInterface node = (NodeInterface)obj;
		return Iterables.toList(node.getRelationships(type));
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
