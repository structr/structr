/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.property;

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
public class RelationshipEndNode<T extends AbstractNode> extends AbstractReadOnlyProperty<T> {

	private Notion notion            = null;
	
	public RelationshipEndNode(String name) {
		this(name, null);
	}
	
	public RelationshipEndNode(String name, final Notion notion) {
		
		super(name);
		
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
		return "Node";
	}

	@Override
	public Class relatedType() {
		return AbstractRelationship.class;
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext) {
		
		if (notion != null) {
			return notion.getEntityConverter(securityContext);
		}
		
		return null;
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return (T)((AbstractRelationship)obj).getEndNode();
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public Integer getSortType() {
		return null;
	}

}
