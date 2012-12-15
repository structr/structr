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
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;

/**
 * A property that defines a relationship with the given parameters between two nodes.
 *
 * @author Christian Morgner
 */
public class EntityProperty<T extends GraphObject> extends AbstractRelationProperty<T> {

	private static final Logger logger = Logger.getLogger(EntityProperty.class.getName());

	private boolean manyToOne = false;
	private Notion notion     = null;
	
	/**
	 * Constructs an entity property with the given name, based on the given property,
	 * transformed by the given notion.
	 * 
	 * @param name
	 * @param base
	 * @param notion 
	 */
	public EntityProperty(String name, EntityProperty base, Notion notion) {
		this(name, base.getDestType(), base.getRelType(), base.getDirection(), notion, base.isManyToOne(), base.getCascadeDelete());
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
		this(name, base.getDestType(), base.getRelType(), base.getDirection(), notion, base.isManyToOne(), deleteCascade);
	}
	
	/**
	 * Constructs an entity property with the given name, the given destination type
	 * and the given relationship type.
	 * 
	 * @param name
	 * @param destType
	 * @param relType 
	 */
	public EntityProperty(String name, Class destType, RelationshipType relType, boolean manyToOne) {
		this(name, destType, relType, Direction.OUTGOING, manyToOne);
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
	public EntityProperty(String name, Class destType, RelationshipType relType, boolean manyToOne, int cascadeDelete) {
		this(name, destType, relType, Direction.OUTGOING, new ObjectNotion(), manyToOne, cascadeDelete);
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
	public EntityProperty(String name, Class destType, RelationshipType relType, Direction direction, boolean manyToOne, int cascadeDelete) {
		this(name, destType, relType, direction, new ObjectNotion(), manyToOne, cascadeDelete);
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
	public EntityProperty(String name, Class destType, RelationshipType relType, Direction direction, boolean manyToOne) {
		this(name, destType, relType, direction, new ObjectNotion(), manyToOne);
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
	public EntityProperty(String name, Class destType, RelationshipType relType, Notion notion, boolean manyToOne) {
		this(name, destType, relType, Direction.OUTGOING, notion, manyToOne, Relation.DELETE_NONE);
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
	public EntityProperty(String name, Class destType, RelationshipType relType, Direction direction, Notion notion, boolean manyToOne) {
		this(name, destType, relType, direction, notion, manyToOne, Relation.DELETE_NONE);
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
	public EntityProperty(String name, Class destType, RelationshipType relType, Direction direction, Notion notion, boolean manyToOne, int cascadeDelete) {
		
		super(name, destType, relType, direction, manyToOne ? Cardinality.ManyToOne : Cardinality.OneToOne, cascadeDelete);

		this.notion = notion;
		this.notion.setType(destType);
		
		EntityContext.registerConvertedProperty(this);		
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
		return notion.getEntityConverter(securityContext);
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {

		if (obj instanceof AbstractNode) {

			AbstractNode node = (AbstractNode)obj;

			if (cardinality.equals(Relation.Cardinality.OneToOne) || cardinality.equals(Relation.Cardinality.ManyToOne)) {

				NodeFactory nodeFactory = new NodeFactory(securityContext);
				Class destinationType   = getDestType();
				Node dbNode             = node.getNode();
				AbstractNode value      = null;

				try {

					for (Relationship rel : dbNode.getRelationships(getRelType(), getDirection())) {

						value = nodeFactory.createNode(rel.getOtherNode(dbNode));

						// break on first hit of desired type
						if (value != null && destinationType.isInstance(value)) {
							return (T)value;
						}
					}

				} catch (Throwable t) {

					logger.log(Level.WARNING, "Unable to fetch related node: {0}", t.getMessage());
				}

			} else {

				logger.log(Level.WARNING, "Requested related node with wrong cardinality {0} between {1} and {2}", new Object[] { getCardinality().name(), node.getClass().getSimpleName(), getDestType()});
			}

		} else {

			logger.log(Level.WARNING, "Property {0} is registered on illegal type {1}", new Object[] { this, obj.getClass() } );
		}

		return null;
	}

	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, T value) throws FrameworkException {
		
		if (value != null) {

			createRelationship(securityContext, (AbstractNode)obj, (AbstractNode)value);

		} else {

			// new value is null
			T existingValue = getProperty(securityContext, obj, true);

			// do nothing if value is already null
			if (existingValue == null) {

				return;
			}

			removeRelationship(securityContext, (AbstractNode)obj, (AbstractNode)existingValue);
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
	
	public boolean isManyToOne() {
		return manyToOne;
	}
	
	public AbstractNode createRelatedNode(final SecurityContext securityContext, final AbstractNode node) throws FrameworkException {

		return Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<AbstractNode>() {

			@Override
			public AbstractNode execute() throws FrameworkException {

				AbstractNode relatedNode = Services.command(securityContext, CreateNodeCommand.class).execute(new NodeAttribute(AbstractNode.type, getDestType().getSimpleName()));
                                PropertyMap props        = new PropertyMap();

                                if (cascadeDelete > 0) {

					props.put(AbstractRelationship.cascadeDelete, new Integer(cascadeDelete));

				}
                                
				// create relationship
				Services.command(securityContext, CreateRelationshipCommand.class).execute(node, relatedNode, getRelType(), props, false);

				return relatedNode;
			}

		});
	}
}
