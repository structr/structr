package org.structr.core.entity;

import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public class Endpoint<A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target> {

	protected Relation<A, B, S, T> relType = null;
	
	public Endpoint(final Relation<A, B, S, T> relation) {
		this.relType = relation;
	}
	
	/*
	
	public NodeInterface createRelatedNode(final SecurityContext securityContext, final NodeInterface node) throws FrameworkException {

		return StructrApp.getInstance(securityContext).command(TransactionCommand.class).execute(new StructrTransaction<AbstractNode>() {

			@Override
			public AbstractNode execute() throws FrameworkException {

				AbstractNode relatedNode = StructrApp.getInstance(securityContext).command(CreateNodeCommand.class).execute(new NodeAttribute(AbstractNode.type, destType.getSimpleName()));
                                PropertyMap props        = new PropertyMap();

                                if (cascadeDelete > 0) {

					props.put(AbstractRelationship.cascadeDelete, new Integer(cascadeDelete));

				}
                                
				// create relationship
				StructrApp.getInstance(securityContext).command(CreateRelationshipCommand.class).execute(node, relatedNode, relType, props, false);

				return relatedNode;
			}

		});
	}

	// ----- private methods -----
	private void ensureManyToOne(AbstractNode sourceNode, AbstractNode targetNode, AbstractRelationship newRel, FactoryDefinition factoryDefinition, DeleteRelationshipCommand deleteRel) throws FrameworkException {
		
		Class newRelationshipClass = newRel.getClass();
		Class targetType           = targetNode.getClass();

		// ManyToOne: sourceNode may not have relationships to other nodes of the same type!
		
		for (AbstractRelationship rel : sourceNode.getRelationships(StartNode.this.relationClass)) {

			if (rel.equals(newRel)) {
				continue;
			}

			Class relationshipClass = rel.getClass();
			boolean isGeneric = factoryDefinition.isGeneric(relationshipClass);

			AbstractNode otherNode = rel.getOtherNode(sourceNode);
			Class otherClass = otherNode.getClass();
			boolean removeRel = targetType.isAssignableFrom(otherClass) || (!isGeneric && newRelationshipClass.isAssignableFrom(relationshipClass));
			
			if (!removeRel) {
				
				// Check interfaces
				for (Class iface : StructrApp.getConfiguration().getInterfacesForType(targetType)) {

					removeRel |= iface.isAssignableFrom(otherClass);
				}
			}
			
			if (removeRel) {

				deleteRel.execute(rel);
			}
		}

	}
	
	private void ensureOneToMany(AbstractNode sourceNode, AbstractNode targetNode, AbstractRelationship newRel, FactoryDefinition factoryDefinition, DeleteRelationshipCommand deleteRel) throws FrameworkException {
		
		Class newRelationshipClass = newRel.getClass();
		Class sourceType           = sourceNode.getClass();

		// ManyToOne: targetNode may not have relationships to other nodes of the same type!
		
		for (AbstractRelationship rel : targetNode.getReverseRelationships(StartNode.this.relationClass)) {

			if (rel.equals(newRel)) {
				continue;
			}

			Class relationshipClass = rel.getClass();
			boolean isGeneric = factoryDefinition.isGeneric(relationshipClass);

			AbstractNode otherNode = rel.getOtherNode(targetNode);
			Class otherClass = otherNode.getClass();
			boolean removeRel = sourceType.isAssignableFrom(otherClass) || (!isGeneric && newRelationshipClass.isAssignableFrom(relationshipClass));
			
			if (!removeRel) {
				
				// Check interfaces
				for (Class iface : StructrApp.getConfiguration().getInterfacesForType(sourceType)) {

					removeRel |= iface.isAssignableFrom(otherClass);
				}
			}
			
			if (removeRel) {

				deleteRel.execute(rel);
			}
		}

	}
	
	// ----- protected methods -----
	
	public List<S> getRelatedNodesReverse(SecurityContext securityContext, GraphObject obj, Class destinationType) {
		
		List<S> relatedNodes = new LinkedList<S>();
		
		if (obj instanceof AbstractNode) {

			AbstractNode node = (AbstractNode)obj;

			NodeFactory nodeFactory = new NodeFactory(securityContext);
			Node dbNode             = node.getNode();
			NodeInterface value     = null;

			try {

				for (Relationship rel : dbNode.getRelationships(getRelType(), getDirection().reverse())) {

					value = nodeFactory.instantiate(rel.getOtherNode(dbNode));

					// break on first hit of desired type
					if (value != null && destinationType.isInstance(value)) {
						relatedNodes.add((S)value);
					}
				}

			} catch (Throwable t) {

				logger.log(Level.WARNING, "Unable to fetch related node: {0}", t.getMessage());
			}

		} else {

			logger.log(Level.WARNING, "Property {0} is registered on illegal type {1}", new Object[] { this, obj.getClass() } );
		}

		return relatedNodes;
	}

	public AbstractRelationship createRelationship(final SecurityContext securityContext, final AbstractNode sourceNode, final AbstractNode targetNode) throws FrameworkException {
		return createRelationship(securityContext, sourceNode, targetNode, new PropertyMap());
	}

	public AbstractRelationship createRelationship(final SecurityContext securityContext, final AbstractNode sourceNode, final AbstractNode targetNode, final PropertyMap properties) throws FrameworkException {

		// create relationship if it does not already exist
		final CreateRelationshipCommand<?> createRel = StructrApp.getInstance(securityContext).command(CreateRelationshipCommand.class);
		final DeleteRelationshipCommand deleteRel    = StructrApp.getInstance(securityContext).command(DeleteRelationshipCommand.class);

		if ((sourceNode != null) && (targetNode != null)) {

			final AbstractNode finalTargetNode = targetNode;
			final AbstractNode finalSourceNode = (AbstractNode) sourceNode;
                        
			StructrTransaction<AbstractRelationship> transaction = new StructrTransaction<AbstractRelationship>() {

				@Override
				public AbstractRelationship execute() throws FrameworkException {

                                        PropertyMap props           = new PropertyMap(properties);
					AbstractRelationship newRel = null;

					// set cascade delete value
					if (getCascadeDelete() > 0) {

						props.put(AbstractRelationship.cascadeDelete, new Integer(getCascadeDelete()));

					}
					
					
					if (getDirection().equals(Direction.OUTGOING)) {

						// set combined type
						String tripleKey = StructrApp.getConfiguration().createCombinedRelationshipType(declaringClass.getSimpleName(), relType.name(), destType.getSimpleName());
						props.put(AbstractRelationship.combinedType, Search.clean(tripleKey));

						newRel = createRel.execute(sourceNode, finalTargetNode, getRelType(), props, false);
//						newRel = createRel.execute(sourceNode, finalTargetNode, getRelType(), props, true);

					} else {

						// set combined type
						String tripleKey = StructrApp.getConfiguration().createCombinedRelationshipType(destType.getSimpleName(), relType.name(), declaringClass.getSimpleName());
						props.put(AbstractRelationship.combinedType, Search.clean(tripleKey));

						newRel = createRel.execute(finalTargetNode, sourceNode, getRelType(), props, false);
//						newRel = createRel.execute(finalTargetNode, sourceNode, getRelType(), props, true);

					}

					if (newRel != null) {

						FactoryDefinition factoryDefinition = StructrApp.getConfiguration().getFactoryDefinition();
						
						switch (getCardinality()) {

							case OneToOne:

								ensureOneToMany(finalSourceNode, finalTargetNode, newRel, factoryDefinition, deleteRel);
								ensureManyToOne(finalSourceNode, finalTargetNode, newRel, factoryDefinition, deleteRel);
								break;
								
							case OneToMany:

								ensureOneToMany(finalSourceNode, finalTargetNode, newRel, factoryDefinition, deleteRel);
								break;

							case ManyToOne:
							
								ensureManyToOne(finalSourceNode, finalTargetNode, newRel, factoryDefinition, deleteRel);
								break;

						}

					}

					return newRel;
				}
			};

			// execute transaction
			return StructrApp.getInstance(securityContext).command(TransactionCommand.class).execute(transaction);

		} else {

			String type = "unknown";

			if (sourceNode != null) {

				type = sourceNode.getType();

			} else if (targetNode != null) {

				type = targetNode.getType();

			}

			if (sourceNode == null) {
				logger.log(Level.WARNING, "No source node!");
				throw new FrameworkException(type, new IdNotFoundToken(sourceNode));
			}
			
			if (targetNode == null) {
				logger.log(Level.WARNING, "No target node!");
				throw new FrameworkException(type, new IdNotFoundToken(targetNode));
			}

		}
		
		return null;
	}

	public void removeRelationship(final SecurityContext securityContext, final AbstractNode sourceNode, final AbstractNode targetNode) throws FrameworkException {

		final DeleteRelationshipCommand deleteRel = StructrApp.getInstance(securityContext).command(DeleteRelationshipCommand.class);

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
							for (Relation<S, T, ?, ?> rel : sourceNode.getRelationships(StartNode.this.relationClass)) {

								if (rel.getOtherNode(sourceNode).getType().equals(destType)) {

									deleteRel.execute(rel);

								}

							}

							break;

						}

						case OneToMany : {

							String sourceType = sourceNode.getType();
							
							// Here, we have a OneToMany with OUTGOING Rel, so we need to remove all relationships
							// of the same combinedType incoming to the target node (which should be exaclty one relationship!)
							for (AbstractRelationship rel : finalTargetNode.getIncomingRelationships(StartNode.this.relationClass)) {

								if (rel.getOtherNode(finalTargetNode).getType().equals(sourceType)) {

									deleteRel.execute(rel);

								}

							}
						}

						case ManyToMany : {

							// In this case, remove exact the relationship of the given combinedType
							// between source and target node
							for (AbstractRelationship rel : finalTargetNode.getAllRelationships(StartNode.this.relationClass)) {

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
			StructrApp.getInstance(securityContext).command(TransactionCommand.class).execute(transaction);

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
	
	// ----- protected methods -----
	

	
	// ----- private methods -----
	private void ensureManyToOne(AbstractNode sourceNode, AbstractNode targetNode, AbstractRelationship newRel, FactoryDefinition factoryDefinition, DeleteRelationshipCommand deleteRel) throws FrameworkException {
		
		Class newRelationshipClass = newRel.getClass();
		Class targetType           = targetNode.getClass();

		// ManyToOne: sourceNode may not have relationships to other nodes of the same type!
		
		for (AbstractRelationship rel : sourceNode.getRelationships(EndNode.this.relationClass)) {

			if (rel.equals(newRel)) {
				continue;
			}

			Class relationshipClass = rel.getClass();
			boolean isGeneric = factoryDefinition.isGeneric(relationshipClass);

			AbstractNode otherNode = rel.getOtherNode(sourceNode);
			Class otherClass = otherNode.getClass();
			boolean removeRel = targetType.isAssignableFrom(otherClass) || (!isGeneric && newRelationshipClass.isAssignableFrom(relationshipClass));
			
//			if (!removeRel) {
//				
//				// Check interfaces
//				for (Class iface : StructrApp.getConfiguration().getInterfacesForType(targetType)) {
//
//					removeRel |= iface.isAssignableFrom(otherClass);
//				}
//			}
			
			if (removeRel) {

				deleteRel.execute(rel);
			}
		}

	}
	
	private void ensureOneToMany(AbstractNode sourceNode, AbstractNode targetNode, AbstractRelationship newRel, FactoryDefinition factoryDefinition, DeleteRelationshipCommand deleteRel) throws FrameworkException {
		
		Class newRelationshipClass = newRel.getClass();
		Class sourceType           = sourceNode.getClass();

		// ManyToOne: targetNode may not have relationships to other nodes of the same type!
		
		for (AbstractRelationship rel : targetNode.getReverseRelationships(EndNode.this.relationClass)) {

			if (rel.equals(newRel)) {
				continue;
			}

			Class relationshipClass = rel.getClass();
			boolean isGeneric = factoryDefinition.isGeneric(relationshipClass);

			AbstractNode otherNode = rel.getOtherNode(targetNode);
			Class otherClass = otherNode.getClass();
			boolean removeRel = sourceType.isAssignableFrom(otherClass) || (!isGeneric && newRelationshipClass.isAssignableFrom(relationshipClass));
			
//			if (!removeRel) {
//				
//				// Check interfaces
//				for (Class iface : StructrApp.getConfiguration().getInterfacesForType(sourceType)) {
//
//					removeRel |= iface.isAssignableFrom(otherClass);
//				}
//			}
			
			if (removeRel) {

				deleteRel.execute(rel);
			}
		}

	}
	*/
}
