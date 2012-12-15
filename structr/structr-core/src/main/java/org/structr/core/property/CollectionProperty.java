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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.graph.NodeFactory;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;

/**
 * A property that defines a relationship with the given parameters between a node and a collection of other nodes.
 *
 * @author Christian Morgner
 */
public class CollectionProperty<T extends GraphObject> extends AbstractRelationProperty<List<T>> {

	private static final Logger logger = Logger.getLogger(CollectionProperty.class.getName());

	private boolean oneToMany = false;
	private Notion notion     = null;
	
	/**
	 * Constructs a collection property with the given name, based on the given property, transformed by the given notion.
	 *
	 * @param name
	 * @param base
	 * @param notion
	 */
	public CollectionProperty(String name, CollectionProperty base, Notion notion) {
		this(name, base.getDestType(), base.getRelType(), base.getDirection(), notion, base.isOneToMany(), base.getCascadeDelete());
	}

	/**
	 * Constructs a collection property with the given name, based on the given property, transformed by the given notion, with the given delete cascade flag.
	 *
	 * @param name
	 * @param base
	 * @param notion
	 */
	public CollectionProperty(String name, CollectionProperty base, Notion notion, int deleteCascade) {
		this(name, base.getDestType(), base.getRelType(), base.getDirection(), notion, base.isOneToMany(), deleteCascade);
	}

	/**
	 * Constructs a collection property with the given name, the given destination type and the given relationship type.
	 *
	 * @param name
	 * @param destType
	 * @param relType
	 */
	public CollectionProperty(String name, Class destType, RelationshipType relType, boolean oneToMany) {
		this(name, destType, relType, Direction.OUTGOING, oneToMany);
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
	public CollectionProperty(String name, Class destType, RelationshipType relType, boolean oneToMany, int cascadeDelete) {
		this(name, destType, relType, Direction.OUTGOING, new ObjectNotion(), oneToMany, cascadeDelete);
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
	public CollectionProperty(String name, Class destType, RelationshipType relType, Direction direction, boolean oneToMany, int cascadeDelete) {
		this(name, destType, relType, direction, new ObjectNotion(), oneToMany, cascadeDelete);
	}

	/**
	 * Constructs a collection property with the given name, the given destination type, the given relationship type and the given direction.
	 *
	 * @param name
	 * @param destType
	 * @param relType
	 * @param direction
	 */
	public CollectionProperty(String name, Class destType, RelationshipType relType, Direction direction, boolean oneToMany) {
		this(name, destType, relType, direction, new ObjectNotion(), oneToMany);
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
	public CollectionProperty(String name, Class destType, RelationshipType relType, Direction direction, Notion notion, boolean oneToMany) {
		this(name, destType, relType, direction, notion, oneToMany, Relation.DELETE_NONE);
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
	public CollectionProperty(String name, Class destType, RelationshipType relType, Direction direction, Notion notion, boolean oneToMany, int cascadeDelete) {

		super(name, destType, relType, direction, oneToMany ? Cardinality.OneToMany : Cardinality.ManyToMany, cascadeDelete);

		this.notion    = notion;
		this.oneToMany = oneToMany;

		this.notion.setType(destType);
		
		EntityContext.registerConvertedProperty(this);
	}
	
	@Override
	public String typeName() {
		return "Object";
	}

	@Override
	public PropertyConverter<List<T>, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, List<T>> inputConverter(SecurityContext securityContext) {
		return getNotion().getCollectionConverter(securityContext);
	}

	@Override
	public List<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {

		if (obj instanceof AbstractNode) {

			AbstractNode node = (AbstractNode)obj;

			if (cardinality.equals(Relation.Cardinality.OneToMany) || cardinality.equals(Relation.Cardinality.ManyToMany)) {

				NodeFactory nodeFactory = new NodeFactory(securityContext);
				Class destinationType   = getDestType();
				List<T> nodes           = new LinkedList<T>();
				Node dbNode             = node.getNode();
				AbstractNode value      = null;

				try {

					for (Relationship rel : dbNode.getRelationships(getRelType(), getDirection())) {

						value = nodeFactory.createNode(rel.getOtherNode(dbNode));
						if (value != null && destinationType.isInstance(value)) {

							nodes.add((T)value);
						}
					}

					return nodes;

				} catch (Throwable t) {

					logger.log(Level.WARNING, "Unable to fetch related node: {0}", t.getMessage());
				}

			} else {

				logger.log(Level.WARNING, "Requested related nodes with wrong cardinality {0} between {1} and {2}", new Object[] { getCardinality().name(), node.getClass().getSimpleName(), getDestType()});
			}

		} else {

			logger.log(Level.WARNING, "Property {0} is registered on illegal type {1}", new Object[] { this, obj.getClass() } );
		}

		return Collections.emptyList();
	}

	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, List<T> collection) throws FrameworkException {

		if (obj instanceof AbstractNode) {
			
			AbstractNode sourceNode = (AbstractNode)obj;

			if (collection != null) {

				// FIXME: how are existing relationships handled if they need to be removed??
				
				for (GraphObject targetNode : collection) {

					createRelationship(securityContext, sourceNode, (AbstractNode)targetNode);
				}

			} else {

				// new value is null
				List<T> existingCollection = getProperty(securityContext, obj, true);

				// do nothing if value is already null
				if (existingCollection == null || (existingCollection != null && existingCollection.isEmpty())) {
					return;
				}

				for (GraphObject targetNode : existingCollection) {

					removeRelationship(securityContext, sourceNode, (AbstractNode)targetNode);
				}
			}
			
		} else {

			logger.log(Level.WARNING, "Property {0} is registered on illegal type {1}", new Object[] { this, obj.getClass() } );
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

	public boolean isOneToMany() {
		return oneToMany;
	}
}
