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
import org.neo4j.graphdb.*;
import org.structr.common.GenericFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;
import org.structr.common.property.Property;
import org.structr.common.property.PropertyMap;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.graph.*;
import org.structr.core.notion.Notion;

/**
 * Abstract base class for all related node properties.
 *
 * @author Christian Morgner
 */
public abstract class AbstractRelationProperty<T> extends Property<T> {
	
	private static final Logger logger = Logger.getLogger(AbstractRelationProperty.class.getName());
	
	private Class destType           = null;
	private RelationshipType relType = null;
	private Direction direction      = null;
	private Cardinality cardinality  = null;
	private int cascadeDelete        = 0;
	
	public abstract Notion getNotion();
	
	public AbstractRelationProperty(String name, Class destType, RelationshipType relType, Direction direction, Cardinality cardinality, int cascadeDelete) {

		super(name);
		
		this.destType      = destType;
		this.relType       = relType;
		this.direction     = direction;
		this.cardinality   = cardinality;
		this.cascadeDelete = cascadeDelete;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext, GraphObject entitiy) {
		return null;
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext) {
		return null;
	}

	public void createRelationship(final SecurityContext securityContext, final AbstractNode sourceNode, final AbstractNode targetNode) throws FrameworkException {
		createRelationship(securityContext, sourceNode, targetNode, new PropertyMap());
	}

	public void createRelationship(final SecurityContext securityContext, final AbstractNode sourceNode, final AbstractNode targetNode, final PropertyMap properties) throws FrameworkException {

		// create relationship if it does not already exist
		final CreateRelationshipCommand<?> createRel = Services.command(securityContext, CreateRelationshipCommand.class);
		final DeleteRelationshipCommand deleteRel = Services.command(securityContext, DeleteRelationshipCommand.class);

		if ((sourceNode != null) && (targetNode != null)) {

			final AbstractNode finalTargetNode = targetNode;
			final AbstractNode finalSourceNode = (AbstractNode) sourceNode;
                        
			StructrTransaction transaction     = new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

                                        PropertyMap props           = new PropertyMap(properties);
					AbstractRelationship newRel = null;

					// set cascade delete value
					if (getCascadeDelete() > 0) {

						props.put(AbstractRelationship.cascadeDelete, new Integer(getCascadeDelete()));

					}

					if (getDirection().equals(Direction.OUTGOING)) {

						newRel = createRel.execute(sourceNode, finalTargetNode, getRelType(), props, true);

					} else {

						newRel = createRel.execute(finalTargetNode, sourceNode, getRelType(), props, true);

					}

					if (newRel != null) {

						GenericFactory genericFactory = EntityContext.getGenericFactory();
						Class newRelationshipClass    = newRel.getClass();
						
						switch (getCardinality()) {

							case ManyToOne :
							case OneToOne : {

								Class destType = finalTargetNode.getClass();

								// delete previous relationships to nodes of the same destination combinedType and direction
								List<AbstractRelationship> rels = finalSourceNode.getRelationships(getRelType(), getDirection());

								for (AbstractRelationship rel : rels) {

									if (rel.equals(newRel)) {
										continue;
									}
									
									Class relationshipClass = rel.getClass();
									boolean isGeneric = genericFactory.isGeneric(relationshipClass);
									
									if ((!isGeneric && newRelationshipClass.isAssignableFrom(relationshipClass) || destType.isAssignableFrom(rel.getOtherNode(finalSourceNode).getClass()))) {

										deleteRel.execute(rel);

									}
								}

								break;

							}

							case OneToMany : {

								Class sourceType = finalSourceNode.getClass();

								// Here, we have a OneToMany with OUTGOING Rel, so we need to remove all relationships
								// of the same combinedType incoming to the target node
								List<AbstractRelationship> rels = finalTargetNode.getRelationships(getRelType(), Direction.INCOMING);

								for (AbstractRelationship rel : rels) {

									if (rel.equals(newRel)) {
										continue;
									}
									
									Class relationshipClass = rel.getClass();
									boolean isGeneric = genericFactory.isGeneric(relationshipClass);
									    
									if ((!isGeneric && newRelationshipClass.isAssignableFrom(relationshipClass)) || sourceType.isAssignableFrom(rel.getOtherNode(finalTargetNode).getClass())) {

										deleteRel.execute(rel);

									}
								}

							}

						}

					}

					return newRel;
				}
			};

			// execute transaction
			Services.command(securityContext, TransactionCommand.class).execute(transaction);

		} else {

			String type = "unknown";

			if (sourceNode != null) {

				type = sourceNode.getType();

			} else if (targetNode != null) {

				type = targetNode.getType();

			}

			throw new FrameworkException(type, new IdNotFoundToken(targetNode));

		}
	}

	public void removeRelationship(final SecurityContext securityContext, final AbstractNode sourceNode, final AbstractNode targetNode) throws FrameworkException {

		final DeleteRelationshipCommand deleteRel = Services.command(securityContext, DeleteRelationshipCommand.class);

		if ((sourceNode != null) && (targetNode != null)) {

			final AbstractNode finalTargetNode = targetNode;
			StructrTransaction transaction     = new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					switch (getCardinality()) {

						case ManyToOne :
						case OneToOne : {

							String destType = finalTargetNode.getType();

							// delete previous relationships to nodes of the same destination combinedType and direction
							List<AbstractRelationship> rels = sourceNode.getRelationships(getRelType(), getDirection());

							for (AbstractRelationship rel : rels) {

								if (rel.getOtherNode(sourceNode).getType().equals(destType)) {

									deleteRel.execute(rel);

								}

							}

							break;

						}

						case OneToMany : {

							String sourceType = sourceNode.getType();
							
							// Here, we have a OneToMany with OUTGOING Rel, so we need to remove all relationships
							// of the same combinedType incoming to the target node
							List<AbstractRelationship> rels = finalTargetNode.getRelationships(getRelType(), Direction.INCOMING);

							for (AbstractRelationship rel : rels) {

								if (rel.getOtherNode(finalTargetNode).getType().equals(sourceType)) {

									deleteRel.execute(rel);

								}

							}
						}

						case ManyToMany : {

							// In this case, remove exact the relationship of the given combinedType
							// between source and target node
							List<AbstractRelationship> rels = finalTargetNode.getRelationships(getRelType(), Direction.BOTH);

							for (AbstractRelationship rel : rels) {

								if (rel.getOtherNode(finalTargetNode).equals(sourceNode)) {

									deleteRel.execute(rel);

								}

							}
						}

					}

					return null;
				}
			};

			// execute transaction
			Services.command(securityContext, TransactionCommand.class).execute(transaction);

		} else {

			String type = "unknown";

			if (sourceNode != null) {

				type = sourceNode.getType();

			} else if (targetNode != null) {

				type = targetNode.getType();

			}

			throw new FrameworkException(type, new IdNotFoundToken(targetNode));

		}
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

	protected List<T> getRelatedNodes(final SecurityContext securityContext, final AbstractNode node) {

		
		if (getCardinality().equals(Relation.Cardinality.OneToMany) || getCardinality().equals(Relation.Cardinality.ManyToMany)) {

			NodeFactory nodeFactory = new NodeFactory(securityContext);
			List<T> nodes           = new LinkedList<T>();
			Node dbNode             = node.getNode();
			AbstractNode value      = null;
	
			try {

				for (Relationship rel : dbNode.getRelationships(getRelType(), getDirection())) {

					value = nodeFactory.createNode(rel.getOtherNode(dbNode));
					if (value != null && getDestType().isInstance(value)) {
						
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

		return Collections.emptyList();
	}

	protected T getRelatedNode(final SecurityContext securityContext, final AbstractNode node) {

		if (getCardinality().equals(Relation.Cardinality.OneToOne) || getCardinality().equals(Relation.Cardinality.ManyToOne)) {

			NodeFactory nodeFactory = new NodeFactory(securityContext);
			Node dbNode             = node.getNode();
			AbstractNode value      = null;

			try {

				for (Relationship rel : dbNode.getRelationships(getRelType(), getDirection())) {

					value = nodeFactory.createNode(rel.getOtherNode(dbNode));

					// break on first hit of desired type
					if (value != null && getDestType().isInstance(value)) {
						return (T)value;
					}
				}

			} catch (Throwable t) {

				logger.log(Level.WARNING, "Unable to fetch related node: {0}", t.getMessage());
			}

		} else {

			logger.log(Level.WARNING, "Requested related node with wrong cardinality {0} between {1} and {2}", new Object[] { getCardinality().name(), node.getClass().getSimpleName(), getDestType()});

		}

		return null;
	}

	public Class getDestType() {
		return destType;
	}

	public RelationshipType getRelType() {
		return relType;
	}

	public Direction getDirection() {
		return direction;
	}

	public Cardinality getCardinality() {
		return cardinality;
	}

	public int getCascadeDelete() {
		return cascadeDelete;
	}
}
