/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.property;

import java.util.List;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.notion.Notion;

/**
 *
 * @author Christian Morgner
 */
public class RelationshipProperty<T extends AbstractRelationship> extends AbstractReadOnlyProperty<List<T>> {

	private RelationshipType relType = null;
	private Direction direction      = null;
	private Notion notion            = null;
	
	public RelationshipProperty(String name, final RelationshipType relType, final Direction direction) {
		this(name, relType, direction, null);
	}
	
	public RelationshipProperty(String name, final RelationshipType relType, final Direction direction, final Notion notion) {
		
		super(name);

		this.relType   = relType;
		this.direction = direction;
		this.notion    = notion;
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
		
		AbstractNode node = (AbstractNode)obj;
		Iterable<T> rels  = (Iterable<T>)node.getRelationships(relType, direction);
		
		return Iterables.toList(rels);
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
