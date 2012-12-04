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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;

/**
 *
 * @author Christian Morgner
 */
public class EntityProperty<T> extends AbstractRelationProperty<T> {

	private static final Logger logger = Logger.getLogger(EntityProperty.class.getName());

	private Notion notion              = null;
	
	/**
	 * Constructs an entity property with the given name, based on the given property,
	 * transformed by the given notion.
	 * 
	 * @param name
	 * @param base
	 * @param notion 
	 */
	public EntityProperty(String name, EntityProperty base, Notion notion) {
		this(name, base.getDestType(), base.getRelType(), base.getDirection(), notion, base.getCascadeDelete());
	}
	
	
	/**
	 * Constructs an entity property with the given name, based on the given property,
	 * transformed by the given notion, with the given delete cascade flag.
	 * 
	 * @param name
	 * @param base
	 * @param notion 
	 */
	public EntityProperty(String name, EntityProperty base, Notion notion, int deleteCascade) {
		this(name, base.getDestType(), base.getRelType(), base.getDirection(), notion, deleteCascade);
	}
	
	/**
	 * Constructs an entity property with the name of the declaring field, the given
	 * destination type and the given relationship type.
	 * 
	 * @param destType
	 * @param relType 
	 */
	public EntityProperty(Class destType, RelationshipType relType) {
		this(null, destType, relType);
	}
	
	/**
	 * Constructs an entity property with the given name, the given destination type
	 * and the given relationship type.
	 * 
	 * @param name
	 * @param destType
	 * @param relType 
	 */
	public EntityProperty(String name, Class destType, RelationshipType relType) {
		this(name, destType, relType, Direction.OUTGOING);
	}

	/**
	 * Constructs an entity property with the name of the declaring field, the
	 * given destination type, the given relationship type, the given direction and
	 * the given cascade delete flag.
	 * 
	 * @param destType
	 * @param relType
	 * @param direction
	 * @param cascadeDelete 
	 */
	public EntityProperty(Class destType, RelationshipType relType, Direction direction, int cascadeDelete) {
		this(null, destType, relType, direction, cascadeDelete);
	}

	/**
	 * Constructs an entity property with the given name, the given destination type,
	 * the given relationship type, the given direction and the given cascade delete
	 * flag.
	 * 
	 * @param name
	 * @param destType
	 * @param relType
	 * @param direction
	 * @param cascadeDelete 
	 */
	public EntityProperty(String name, Class destType, RelationshipType relType, Direction direction, int cascadeDelete) {
		this(name, destType, relType, direction, new ObjectNotion(), cascadeDelete);
	}

	/**
	 * Constructs an entity property with the name of the declaring field, the
	 * given destination type, the given relationship type and the given direction.
	 * 
	 * @param destType
	 * @param relType
	 * @param direction 
	 */
	public EntityProperty(Class destType, RelationshipType relType, Direction direction) {
		this(null, destType, relType, direction);
	}

	/**
	 * Constructs an entity property with the name of the declaring field, the
	 * given destination type, the given relationship type and the given notion.
	 * 
	 * @param destType
	 * @param relType
	 * @param notion
	 */
	public EntityProperty(Class destType, RelationshipType relType, Notion notion) {
		this(null, destType, relType, Direction.OUTGOING, notion);
	}

	/**
	 * Constructs an entity property with the name of the declaring field, the
	 * given destination type, the given relationship type and the given cascade
	 * delete flag.
	 * 
	 * @param destType
	 * @param relType
	 * @param cascadeDelete
	 */
	public EntityProperty(Class destType, RelationshipType relType, int cascadeDelete) {
		this(null, destType, relType, Direction.OUTGOING);
	}
	
	/**
	 * Constructs an entity property with the given name, the given destination type,
	 * the given relationship type and the given direction.
	 * 
	 * @param name
	 * @param destType
	 * @param relType
	 * @param direction 
	 */
	public EntityProperty(String name, Class destType, RelationshipType relType, Direction direction) {
		this(name, destType, relType, direction, new ObjectNotion());
	}
	
	/**
	 * Constructs an entity property with the name of the declaring field, the
	 * given destination type, the given relationship type, the given direction and
	 * the given notion.
	 * 
	 * @param destType
	 * @param relType
	 * @param direction
	 * @param notion 
	 */
	public EntityProperty(Class destType, RelationshipType relType, Direction direction, Notion notion) {
		this(null, destType, relType, direction, notion);
	}
	
	/**
	 * Constructs an entity property with the given name, the given destination type,
	 * the given relationship type, the given direction and the given notion.
	 * 
	 * @param name
	 * @param destType
	 * @param relType
	 * @param direction
	 * @param notion 
	 */
	public EntityProperty(String name, Class destType, RelationshipType relType, Direction direction, Notion notion) {
		this(name, destType, relType, direction, notion, Relation.DELETE_NONE);
	}
	
	/**
	 * Constructs an entity property with the name of the declaring field, the
	 * given destination type, the given relationship type, the given direction,
	 * the given notion and the given cascade delete flag.
	 * 
	 * @param destType
	 * @param relType
	 * @param direction
	 * @param notion
	 * @param cascadeDelete 
	 */
	public EntityProperty(Class destType, RelationshipType relType, Direction direction, Notion notion, int cascadeDelete) {
		this(null, destType, relType, direction, notion, cascadeDelete);
	}
	
	/**
	 * Constructs an entity property with the name of the declaring field, the
	 * given destination type, the given relationship type, the given direction,
	 * the given cascade delete flag, the given notion and the given cascade
	 * delete flag.
	 * 
	 * @param name
	 * @param destType
	 * @param relType
	 * @param direction
	 * @param notion
	 * @param cascadeDelete 
	 */
	public EntityProperty(String name, Class destType, RelationshipType relType, Direction direction, Notion notion, int cascadeDelete) {
		
		super(name, destType, relType, direction, Cardinality.OneToOne, cascadeDelete);

		this.notion = notion;
		this.notion.setType(destType);
		
	}
	
	@Override
	public String typeName() {
		return "Object";
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {

		Object value = null;
		
		try {
			value = notion.getAdapterForGetter(securityContext).adapt(getRelatedNode(securityContext, (AbstractNode)obj));
			
		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
		}
		
		if(applyConverter) {

			// apply property converters
			PropertyConverter converter = databaseConverter(securityContext, obj);
			if (converter != null) {

				try {
					value = converter.revert(value);

				} catch(Throwable t) {

					// CHM: remove debugging code later
					t.printStackTrace();

					logger.log(Level.WARNING, "Unable to convert property {0} of type {1}: {2}", new Object[] {
						dbName(),
						getClass().getSimpleName(),
						t.getMessage()
					});
				}
			}
		}
		
		
		return (T)value;
	}

	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, T value) throws FrameworkException {
		
		if (value != null) {

			GraphObject graphObject = (GraphObject)notion.getAdapterForSetter(securityContext).adapt(value);
			if (graphObject instanceof AbstractNode) {

				createRelationship(securityContext, (AbstractNode)obj, (AbstractNode)graphObject);
			}

		} else {

			// new value is null
			Object existingValue = getProperty(securityContext, obj, true);

			// do nothing if value is already null
			if (existingValue == null) {

				return;
			}

			GraphObject graphObject = (GraphObject)notion.getAdapterForSetter(securityContext).adapt(existingValue);
			if (graphObject instanceof AbstractNode) {

				removeRelationship(securityContext, (AbstractNode)obj, (AbstractNode)graphObject);
			}
		}
	}
	
	@Override
	public Class<? extends GraphObject> relatedType() {
		return this.getDestType();
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public Notion getNotion() {
		return notion;
	}
}
