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

import java.util.LinkedList;
import java.util.List;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.core.Predicate;
import org.structr.core.Value;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;

/**
 * Abstract base class for custom node collectors. Extend this class to specify
 * uniqueness and evaluators for a traversal.
 *
 *
 */
public abstract class CustomCollector<T extends AbstractNode> implements TraverserInterface<T>, Value<TraverserInterface<T>> {

	private final List<RelationshipType> relTypes = new LinkedList<>();
	private final List<Predicate> predicates = new LinkedList<>();
	private final List<Direction> directions = new LinkedList<>();

	public abstract Uniqueness getUniqueness();
	public abstract List<Evaluator> getEvaluators();
	
	public CustomCollector(RelationshipType relType, Direction direction) {
		this.directions.add(direction);
		this.relTypes.add(relType);
	}
	
	public CustomCollector() {
	}

	@Override
	public void addPredicate(Predicate<Node> predicate) {
	}


	@Override
	public TraversalDescription getTraversalDescription(final SecurityContext securityContext) {

		TraversalDescription description = StructrApp.getInstance(securityContext).getGraphDatabaseService().traversalDescription()
			.breadthFirst()
			.uniqueness(getUniqueness());

		// set evaluators
		for (Evaluator evaluator : getEvaluators()) {
			description = description.evaluator(evaluator);
		}
		
		// add predicates as evaluators
		for (final Predicate<Node> predicate : predicates) {
			description = description.evaluator(new Evaluator() {

				@Override
				public Evaluation evaluate(Path path) {
					Node endNode = path.endNode();
					if (predicate.evaluate(securityContext, endNode)) {
						return Evaluation.EXCLUDE_AND_PRUNE;
					}
					
					return Evaluation.INCLUDE_AND_CONTINUE;
				}
			});
		}
		
		// set rel type and direction
		int numRels = relTypes.size();
		for (int i=0; i<numRels; i++) {
			RelationshipType relType = relTypes.get(i);
			Direction direction = directions.get(i);
			description = description.relationships(relType, direction);
		}

		return description;
	}

	@Override
	public void set(SecurityContext securityContext, TraverserInterface value) {
	}

	@Override
	public TraverserInterface get(SecurityContext securityContext) {
		return this;
	}
	
	// ----- protected methods -----
	protected boolean hasPropertyValue(Relationship rel, PropertyKey propertyKey, Object propertyValue) {
		
		if (rel != null && rel.hasProperty(propertyKey.dbName())) {
			
			Object value = rel.getProperty(propertyKey.dbName());
			return value.equals(propertyValue);
		}
		
		return false;
	}

	protected boolean hasPropertyValue(Node node, PropertyKey propertyKey, Object propertyValue) {
		
		if (node != null && node.hasProperty(propertyKey.dbName())) {
			
			Object value = node.getProperty(propertyKey.dbName());
			return value.equals(propertyValue);
		}
		
		return false;
	}
	
	protected boolean hasType(Node node, Class type) {
		return hasPropertyValue(node, AbstractNode.type, type.getSimpleName());
	}
}
