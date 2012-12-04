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

import java.util.Collection;
import java.util.List;
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
public class CollectionProperty<T> extends AbstractRelationProperty<List<T>> {

	private static final Logger logger = Logger.getLogger(CollectionProperty.class.getName());

	private Notion notion              = null;
	
	/**
	 * Constructs a collection property with the given name, based on the given property, transformed by the given notion.
	 *
	 * @param name
	 * @param base
	 * @param notion
	 */
	public CollectionProperty(String name, CollectionProperty base, Notion notion) {
		this(name, base.getDestType(), base.getRelType(), base.getDirection(), notion, base.getCascadeDelete());
	}

	/**
	 * Constructs a collection property with the given name, based on the given property, transformed by the given notion, with the given delete cascade flag.
	 *
	 * @param name
	 * @param base
	 * @param notion
	 */
	public CollectionProperty(String name, CollectionProperty base, Notion notion, int deleteCascade) {
		this(name, base.getDestType(), base.getRelType(), base.getDirection(), notion, deleteCascade);
	}

	/**
	 * Constructs a collection property with the name of the declaring field, the given destination type and the given relationship type.
	 *
	 * @param destType
	 * @param relType
	 */
	public CollectionProperty(Class destType, RelationshipType relType) {
		this(null, destType, relType);
	}

	/**
	 * Constructs a collection property with the name of the declaring field, the given destination type and the given relationship type.
	 *
	 * @param destType
	 * @param relType
	 */
	public CollectionProperty(Class destType, RelationshipType relType, int deleteCascade) {
		this(null, destType, relType, Direction.OUTGOING, deleteCascade);
	}

	/**
	 * Constructs a collection property with the given name, the given destination type and the given relationship type.
	 *
	 * @param name
	 * @param destType
	 * @param relType
	 */
	public CollectionProperty(String name, Class destType, RelationshipType relType) {
		this(name, destType, relType, Direction.OUTGOING);
	}

	/**
	 * Constructs a collection property with the name of the declaring field, the given destination type, the given relationship type, the given direction and the given cascade delete flag.
	 *
	 * @param destType
	 * @param relType
	 * @param direction
	 * @param cascadeDelete
	 */
	public CollectionProperty(Class destType, RelationshipType relType, Direction direction, int cascadeDelete) {
		this(null, destType, relType, direction, cascadeDelete);
	}

	/**
	 * Constructs a collection property with the given name, the given destination type, the given relationship type, the given direction and the given cascade delete flag.
	 *
	 * @param name
	 * @param destType
	 * @param relType
	 * @param direction
	 * @param cascadeDelete
	 */
	public CollectionProperty(String name, Class destType, RelationshipType relType, Direction direction, int cascadeDelete) {
		this(name, destType, relType, direction, new ObjectNotion(), cascadeDelete);
	}

	/**
	 * Constructs a collection property with the name of the declaring field, the given destination type, the given relationship type and the given direction.
	 *
	 * @param destType
	 * @param relType
	 * @param direction
	 */
	public CollectionProperty(Class destType, RelationshipType relType, Direction direction) {
		this(null, destType, relType, direction);
	}

	/**
	 * Constructs a collection property with the given name, the given destination type, the given relationship type and the given direction.
	 *
	 * @param name
	 * @param destType
	 * @param relType
	 * @param direction
	 */
	public CollectionProperty(String name, Class destType, RelationshipType relType, Direction direction) {
		this(name, destType, relType, direction, new ObjectNotion());
	}

	/**
	 * Constructs a collection property with the name of the declaring field, the given destination type, the given relationship type, the given direction and the given notion.
	 *
	 * @param destType
	 * @param relType
	 * @param direction
	 * @param notion
	 */
	public CollectionProperty(Class destType, RelationshipType relType, Direction direction, Notion notion) {
		this(null, destType, relType, direction, notion);
	}

	/**
	 * Constructs a collection property with the given name, the given destination type, the given relationship type, the given direction and the given notion.
	 *
	 * @param name
	 * @param destType
	 * @param relType
	 * @param direction
	 * @param notion
	 */
	public CollectionProperty(String name, Class destType, RelationshipType relType, Direction direction, Notion notion) {
		this(name, destType, relType, direction, notion, Relation.DELETE_NONE);
	}

	/**
	 * Constructs a collection property with the name of the declaring field, the given destination type, the given relationship type, the given direction, the given notion and the given cascade
	 * delete flag.
	 *
	 * @param destType
	 * @param relType
	 * @param direction
	 * @param notion
	 * @param cascadeDelete
	 */
	public CollectionProperty(Class destType, RelationshipType relType, Direction direction, Notion notion, int cascadeDelete) {
		this(null, destType, relType, direction, notion, cascadeDelete);
	}

	/**
	 * Constructs a collection property with the name of the declaring field, the given destination type, the given relationship type, the given direction, the given cascade delete flag, the given
	 * notion and the given cascade delete flag.
	 *
	 * @param name
	 * @param destType
	 * @param relType
	 * @param direction
	 * @param notion
	 * @param cascadeDelete
	 */
	public CollectionProperty(String name, Class destType, RelationshipType relType, Direction direction, Notion notion, int cascadeDelete) {

		super(name, destType, relType, direction, Cardinality.ManyToMany, cascadeDelete);

		this.notion = notion;
		this.notion.setType(destType);
		
	}
	
	@Override
	public String typeName() {
		return "Object";
	}

	@Override
	public List<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {

		Object value = Notion.convertList(getRelatedNodes(securityContext, (AbstractNode)obj), notion.getAdapterForGetter(securityContext));
		
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
		
		
		return (List<T>)value;
	}

	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, List<T> value) throws FrameworkException {
		
		if (value != null) {

			Collection<GraphObject> collection = (Collection)notion.getCollectionAdapterForSetter(securityContext).adapt(value);
			for (GraphObject graphObject : collection) {

				if (graphObject instanceof AbstractNode) {

					createRelationship(securityContext, (AbstractNode)obj, (AbstractNode)graphObject);
				}
			}

		} else {

			// new value is null
			Object existingValue = getProperty(securityContext, obj, true);

			// do nothing if value is already null
			if (existingValue == null) {

				return;
			}
			
			for (Object val : ((Iterable) existingValue)) {

				GraphObject graphObject = (GraphObject)notion.getAdapterForSetter(securityContext).adapt(val);
				if (graphObject instanceof AbstractNode) {

					removeRelationship(securityContext, (AbstractNode)obj, (AbstractNode)graphObject);
				}

			}

		}
	}
	
	@Override
	public Class<? extends GraphObject> relatedType() {
		return this.getDestType();
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public Notion getNotion() {
		return notion;
	}
}
