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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.kernel.Traversal;
import org.structr.common.SecurityContext;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.StructrNodeFactory;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 * A relationship with a direction and a cardinality.
 *
 * @author Christian Morgner
 */
public class DirectedRelationship {

	public enum Cardinality {
		OneToOne, OneToMany, ManyToOne, ManyToMany
	}

	private RelationshipType relType =  null;
	private Cardinality cardinality = null;
	private Direction direction = null;

	public DirectedRelationship(RelationshipType relType, Direction direction, Cardinality cardinality) {

		this.direction = direction;
		this.relType = relType;
	}

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public Cardinality getCardinality() {
		return cardinality;
	}

	public void setCardinality(Cardinality cardinality) {
		this.cardinality = cardinality;
	}

	public RelationshipType getRelType() {
		return relType;
	}

	public void setRelType(RelationshipType relType) {
		this.relType = relType;
	}

	// ----- public methods -----
	public Set<AbstractNode> getRelatedNodes(final SecurityContext securityContext, final AbstractNode node, String type) {

		if(cardinality.equals(Cardinality.OneToMany) || cardinality.equals(Cardinality.ManyToMany)) {
			return getTraversalResults(securityContext, node, StringUtils.capitalize(type));
		}

		return null;
	}

	public AbstractNode getRelatedNode(final SecurityContext securityContext, final AbstractNode node, final String type) {

		if(cardinality.equals(Cardinality.OneToOne) || cardinality.equals(Cardinality.ManyToOne)) {

			Set<AbstractNode> nodes = getTraversalResults(securityContext, node, StringUtils.capitalize(type));
			if(nodes != null && nodes.iterator().hasNext()) {
				return nodes.iterator().next();
			}
		}

		return null;
	}

	public void createRelationship(final SecurityContext securityContext, final AbstractNode sourceNode, final Object value) {

		// create relationship if it does not already exist
		final Command cmd = Services.command(securityContext, CreateRelationshipCommand.class);
		final AbstractNode targetNode;

		if(value instanceof AbstractNode) {

			targetNode = (AbstractNode)value;

		} else {

			targetNode = (AbstractNode)Services.command(securityContext, FindNodeCommand.class).execute(
				securityContext.getUser(),
				value
			);
		}

		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws Throwable {

				// remove relationships
				if(cardinality.equals(Cardinality.OneToOne) /* || FIXME */ ) {
					// delete relationships
					List<StructrRelationship> rels = sourceNode.getRelationships(relType, direction);
					for(StructrRelationship rel : rels) {
						rel.delete();
					}
				}

				if(direction.equals(Direction.OUTGOING)) {
					cmd.execute(sourceNode, targetNode, relType);
				} else {
					cmd.execute(targetNode, sourceNode, relType);
				}
				
				return null;
			}
		};
		
		// execute transaction
		Services.command(securityContext, TransactionCommand.class).execute(transaction);

	}

	// ----- private methods -----
	private Set<AbstractNode> getTraversalResults(final SecurityContext securityContext, AbstractNode node, final String type) {

		// use traverser
		Iterable<Node> nodes = Traversal.description().breadthFirst().relationships(relType, direction).evaluator(

			new Evaluator() {

				@Override
				public Evaluation evaluate(Path path) {

					int len = path.length();
					if(len <= 1) {

						if(len == 0) {

							// do not include start node (which is the
							// index node in this case), but continue
							// traversal
							return Evaluation.EXCLUDE_AND_CONTINUE;

						} else {

							Node currentNode = path.endNode();
							if(currentNode.hasProperty(AbstractNode.Key.type.name())) {

								String nodeType = (String)currentNode.getProperty(AbstractNode.Key.type.name());
								if(type.equals(nodeType)) {
									return Evaluation.INCLUDE_AND_CONTINUE;
								}
							}
						}
					}

					return Evaluation.EXCLUDE_AND_PRUNE;
				}

			}

		).traverse(node.getNode()).nodes();

		// collect results and convert nodes into structr nodes
		StructrNodeFactory nodeFactory = new StructrNodeFactory<AbstractNode>(securityContext);
		Set<AbstractNode> nodeList = new LinkedHashSet<AbstractNode>();
		for(Node n : nodes) {
			nodeList.add(nodeFactory.createNode(securityContext, n, type));
		}

		return nodeList;
	}
}
