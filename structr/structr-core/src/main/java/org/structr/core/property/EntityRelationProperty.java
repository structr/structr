/*
 *  Copyright (C) 2012 Axel Morgner
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
package org.structr.core.property;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.property.Property;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.notion.Notion;

/**
 *
 * @author Christian Morgner
 */
public class EntityRelationProperty<T> extends Property<T> {

	private Class destType           = null;
	private RelationshipType relType = null;
	private Direction direction      = null;
	private Cardinality cardinality  = null;
	private Notion notion            = null;
	private int cascadeDelete        = 0;
	
	public EntityRelationProperty(String name, Class destType, RelationshipType relType, Direction direction, Cardinality cardinality, Notion notion, int cascadeDelete) {
		
		super(name);
		
		this.destType      = destType;
		this.relType       = relType;
		this.direction     = direction;
		this.cardinality   = cardinality;
		this.notion        = notion;
		this.cascadeDelete = cascadeDelete;
	}
	
	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public String typeName() {
		return "Object";
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext, GraphObject entitiy) {
		return null;
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return null;
	}

	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, T value) throws FrameworkException {
		
	}
}
