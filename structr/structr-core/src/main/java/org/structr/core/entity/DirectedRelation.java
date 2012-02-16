/*
 *  Copyright (C) 2011 Axel Morgner
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



package org.structr.core.entity;

import org.apache.commons.lang.StringUtils;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.module.GetEntityClassCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.StructrNodeFactory;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.notion.Notion;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * A relationship with a direction and a cardinality.
 *
 * @author Christian Morgner
 */
public class DirectedRelation {

	private static final Logger logger = Logger.getLogger(DirectedRelation.class.getName());

	//~--- fields ---------------------------------------------------------

	private Cardinality cardinality  = null;
	private String destType          = null;
	private Direction direction      = null;
	private Notion notion            = null;
	private RelationshipType relType = null;

	//~--- constant enums -------------------------------------------------

	public enum Cardinality { OneToOne, OneToMany, ManyToOne, ManyToMany }

	//~--- constructors ---------------------------------------------------

	public DirectedRelation(String destType, RelationshipType relType, Direction direction, Cardinality cardinality, Notion notion) {

		this.cardinality = cardinality;
		this.direction   = direction;
		this.destType    = destType;
		this.relType     = relType;
		this.notion      = notion;
	}

	//~--- methods --------------------------------------------------------

	public void createRelationship(final SecurityContext securityContext, final AbstractNode sourceNode, final Object value) throws FrameworkException {
		createRelationship(securityContext, sourceNode, value, Collections.EMPTY_MAP);
	}

	public void createRelationship(final SecurityContext securityContext, final AbstractNode sourceNode, final Object value, final Map properties) throws FrameworkException {

		// create relationship if it does not already exist
		final Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);
		AbstractNode targetNode = null;

		if (value instanceof AbstractNode) {

			targetNode = (AbstractNode) value;

		} else {

			targetNode = (AbstractNode) Services.command(securityContext, FindNodeCommand.class).execute(value);

		}

		if ((sourceNode != null) && (targetNode != null)) {

			final AbstractNode finalTargetNode = targetNode;
			StructrTransaction transaction     = new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					switch (cardinality) {

						case ManyToOne :
						case OneToOne :

						{
							String destType = finalTargetNode.getType();

							// delete previous relationships to nodes of the same destination type and direction
							List<StructrRelationship> rels = sourceNode.getRelationships(relType, direction);

							for (StructrRelationship rel : rels) {

								if (rel.getOtherNode(sourceNode).getType().equals(destType)) {

									rel.delete(securityContext);

								}

							}

							break;

						}

						case OneToMany :

						{
							// Here, we have a OneToMany with OUTGOING Rel, so we need to remove all relationships
							// of the same type incoming to the target node
							
							List<StructrRelationship> rels = finalTargetNode.getRelationships(relType, Direction.INCOMING);

							for (StructrRelationship rel : rels) {

								if (rel.getOtherNode(finalTargetNode).getType().equals(sourceNode.getType())) {

									rel.delete(securityContext);

								}

							}
						}
					}

					StructrRelationship newRel;

					if (direction.equals(Direction.OUTGOING)) {

						newRel = (StructrRelationship) createRel.execute(sourceNode, finalTargetNode, relType);

					} else {

						newRel = (StructrRelationship) createRel.execute(finalTargetNode, sourceNode, relType);

					}

					newRel.setProperties(properties);

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

			throw new FrameworkException(type, new IdNotFoundToken(value));

		}
	}

	public void removeRelationship(final SecurityContext securityContext, final AbstractNode sourceNode, final Object value) throws FrameworkException {

		AbstractNode targetNode = null;

		if (value instanceof AbstractNode) {

			targetNode = (AbstractNode) value;

		} else {

			targetNode = (AbstractNode) Services.command(securityContext, FindNodeCommand.class).execute(value);

		}

		if ((sourceNode != null) && (targetNode != null)) {

			final AbstractNode finalTargetNode = targetNode;
			StructrTransaction transaction     = new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					switch (cardinality) {

						case ManyToOne :
						case OneToOne :

						{
							String destType = finalTargetNode.getType();

							// delete previous relationships to nodes of the same destination type and direction
							List<StructrRelationship> rels = sourceNode.getRelationships(relType, direction);

							for (StructrRelationship rel : rels) {

								if (rel.getOtherNode(sourceNode).getType().equals(destType)) {

									rel.delete(securityContext);

								}

							}

							break;

						}

						case OneToMany :

						{
							// Here, we have a OneToMany with OUTGOING Rel, so we need to remove all relationships
							// of the same type incoming to the target node
							
							List<StructrRelationship> rels = finalTargetNode.getRelationships(relType, Direction.INCOMING);

							for (StructrRelationship rel : rels) {

								if (rel.getOtherNode(finalTargetNode).getType().equals(sourceNode.getType())) {

									rel.delete(securityContext);

								}

							}
						}

						case ManyToMany :

						{
							// In this case, remove exact the relationship of the given type
							// between source and target node
							
							List<StructrRelationship> rels = finalTargetNode.getRelationships(relType, Direction.BOTH);

							for (StructrRelationship rel : rels) {

								if (rel.getOtherNode(finalTargetNode).equals(sourceNode)) {

									rel.delete(securityContext);

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

			throw new FrameworkException(type, new IdNotFoundToken(value));

		}
	}

	//~--- get methods ----------------------------------------------------

	public String getDestType() {
		return destType;
	}

	public Direction getDirection() {
		return direction;
	}

	public Cardinality getCardinality() {
		return cardinality;
	}

	public RelationshipType getRelType() {
		return relType;
	}

	public Notion getNotion() {
		return notion;
	}

	// ----- public methods -----
	public List<AbstractNode> getRelatedNodes(final SecurityContext securityContext, final AbstractNode node) {

		if (cardinality.equals(Cardinality.OneToMany) || cardinality.equals(Cardinality.ManyToMany)) {

			// return getTraversalResults(securityContext, node, StringUtils.toCamelCase(type));
			return getTraversalResults(securityContext, node);
		} else {

			logger.log(Level.WARNING, "Requested related nodes with wrong cardinality {0} between {1} and {2}", new Object[] { cardinality.name(), node.getClass().getSimpleName(),
				destType });

		}

		return null;
	}

	public AbstractNode getRelatedNode(final SecurityContext securityContext, final AbstractNode node) {

		if (cardinality.equals(Cardinality.OneToOne) || cardinality.equals(Cardinality.ManyToOne)) {

			List<AbstractNode> nodes = getTraversalResults(securityContext, node);

			if ((nodes != null) && nodes.iterator().hasNext()) {

				return nodes.iterator().next();

			}

		} else {

			logger.log(Level.WARNING, "Requested related node with wrong cardinality {0} between {1} and {2}", new Object[] { cardinality.name(), node.getClass().getSimpleName(),
				destType });

		}

		return null;
	}

	// ----- private methods -----
	private List<AbstractNode> getTraversalResults(final SecurityContext securityContext, final AbstractNode node) {

		try {

			final Class realType                 = (Class) Services.command(securityContext, GetEntityClassCommand.class).execute(StringUtils.capitalize(destType));
			final StructrNodeFactory nodeFactory = new StructrNodeFactory<AbstractNode>(securityContext);
			final List<AbstractNode> nodeList    = new LinkedList<AbstractNode>();

			// use traverser
			Iterable<Node> nodes = Traversal.description().uniqueness(Uniqueness.NODE_PATH).breadthFirst().relationships(relType, direction).evaluator(new Evaluator() {

				@Override
				public Evaluation evaluate(Path path) {

					int len = path.length();

					if (len <= 1) {

						if (len == 0) {

							// do not include start node (which is the
							// index node in this case), but continue
							// traversal
							return Evaluation.EXCLUDE_AND_CONTINUE;
						} else {

							try {

								AbstractNode abstractNode = (AbstractNode) nodeFactory.createNode(securityContext, path.endNode());

								// use inheritance
								if ((realType != null) && realType.isAssignableFrom(abstractNode.getClass())) {

									nodeList.add(abstractNode);

									return Evaluation.INCLUDE_AND_CONTINUE;

								} else {

									return Evaluation.EXCLUDE_AND_CONTINUE;

								}

							} catch (FrameworkException fex) {
								logger.log(Level.WARNING, "Unable to instantiate node", fex);
							}

						}

					}

					return Evaluation.EXCLUDE_AND_PRUNE;
				}

			}).traverse(node.getNode()).nodes();

			// iterate nodes to evaluate traversal
			for (Node n : nodes) {}

			return nodeList;

		} catch (FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to get traversal results", fex);
		}

		return Collections.emptyList();
	}

	//~--- set methods ----------------------------------------------------

	public void setDestType(String destType) {
		this.destType = destType;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public void setCardinality(Cardinality cardinality) {
		this.cardinality = cardinality;
	}

	public void setRelType(RelationshipType relType) {
		this.relType = relType;
	}

	public void setNotion(Notion notion) {
		this.notion = notion;
	}
}
