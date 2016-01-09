/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.traversal;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.structr.common.SecurityContext;
import org.structr.core.Predicate;
import org.structr.core.Value;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.Notion;

/**
 * Abstract base class for node collector implementations that can be plugged into
 * TraversingConverter.
 * 
 * To use this collector, subclass it, add predicates
 * and an optional notion to it and use SchemaHelper to register it as the
 * value parameter of a TraversingConverter.
 *
 *
 * @param <T>
 */
public abstract class AbstractNodeCollector<T extends AbstractNode> implements TraverserInterface<T>, Value<TraverserInterface<T>> {

	private static final Logger logger = Logger.getLogger(AbstractNodeCollector.class.getName());

	private final List<Predicate<Node>> predicates = new LinkedList<>();
	private final List<RelationshipType> relTypes  = new LinkedList<>();
	private final List<Direction> directions       = new LinkedList<>();
	private Comparator<T> comparator         = null;
	private Notion notion                    = null;
	private int maxDepth                     = 1;

	public AbstractNodeCollector(RelationshipType relType, Direction direction, int maxDepth) {
		this.directions.add(direction);
		this.relTypes.add(relType);
		this.maxDepth = maxDepth;
	}

	@Override
	public TraversalDescription getTraversalDescription(final SecurityContext securityContext) {

		TraversalDescription description = StructrApp.getInstance(securityContext).getGraphDatabaseService().traversalDescription()
			.breadthFirst()
			.uniqueness(Uniqueness.NODE_RECENT)
			.evaluator(Evaluators.excludeStartPosition())
			.evaluator(new Evaluator() {

				@Override
				public Evaluation evaluate(Path path) {

					// prune path at depth maxDepth
					if(path.length() > maxDepth) {
						return Evaluation.EXCLUDE_AND_PRUNE;
					}

					Node endNode = path.endNode();

					if(!predicates.isEmpty()) {

						boolean include = true;

						for(Predicate<Node> predicate : predicates) {

							try {
								include &= predicate.evaluate(securityContext, endNode);

							} catch(PruneException p) {

								// catch PruneException to stop
								// evaluating this specific path
								// immediately.
								return Evaluation.EXCLUDE_AND_PRUNE;
							}
						}

						if(include) {

							return Evaluation.INCLUDE_AND_CONTINUE;
						}


					} else {

						logger.log(Level.WARNING, "No predicates to evaluate, this collector will not return any nodes!");
					}

					// node has wrong type: exclude and continue traversal
					return Evaluation.EXCLUDE_AND_CONTINUE;
				}

			})
		;

		int numRels = relTypes.size();
		for(int i=0; i<numRels; i++) {
			RelationshipType relType = relTypes.get(i);
			Direction direction = directions.get(i);
			description = description.relationships(relType, direction);
		}

		return description;
	}

	@Override
	public Notion getNotion() {
		return notion;
	}

	@Override
	public Comparator<T> getComparator() {
		return comparator;
	}

	public void addRelationship(RelationshipType relType, Direction direction) {
		relTypes.add(relType);
		directions.add(direction);
	}

	@Override
	public boolean collapseSingleResult() {
		return false;
	}
	
	/**
	 * Adds an evaluation predicate to this collector. Note that the
	 * predicates are parameterized with Node instead of AbstractNode.
	 * This is because evaluation might take place inside of an
	 * AbstractNode's getProperty method, and therefore the evaluation
	 * of the predicate may not call getProperty again, because that
	 * might eventually cause a stack overflow.
	 *
	 * @param predicate
	 */
	@Override
	public void addPredicate(Predicate<Node> predicate) {
		this.predicates.add(predicate);
	}

	// ----- protected methods -----
	protected void setNotion(Notion notion) {
		this.notion = notion;
	}

	protected void setComparator(Comparator<T> comparator) {
		this.comparator = comparator;
	}

	// ----- interface Value<TraverserInterface> ----
	@Override
	public void set(SecurityContext securityContext, TraverserInterface<T> value) {
	}

	@Override
	public TraverserInterface<T> get(SecurityContext securityContext) {
		return this;
	}
	
	// ----- nested classes -----
	/**
	 * A special exception that can be used to make the evaluator
	 * prune the path at the current position.
	 */
	public static class PruneException extends RuntimeException {}
}
