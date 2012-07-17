/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.traversal;

import java.util.LinkedList;
import java.util.List;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.core.Predicate;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.traversal.TraverserInterface;

/**
 *
 * @author Christian Morgner
 */
public abstract class CustomCollector implements TraverserInterface, Value<TraverserInterface> {

	private List<RelationshipType> relTypes = new LinkedList<RelationshipType>();
	private List<Predicate> predicates = new LinkedList<Predicate>();
	private List<Direction> directions = new LinkedList<Direction>();

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
	public TraversalDescription getTraversalDescription(final SecurityContext securityContext, Object sourceProperty) {

		TraversalDescription description = Traversal
			.description()
			.breadthFirst()
			.uniqueness(getUniqueness());

		// set evaluators
		for(Evaluator evaluator : getEvaluators()) {
			description = description.evaluator(evaluator);
		}
		
		// add predicates as evaluators
		for(final Predicate<Node> predicate : predicates) {
			description = description.evaluator(new Evaluator() {

				@Override
				public Evaluation evaluate(Path path) {
					Node endNode = path.endNode();
					if(predicate.evaluate(securityContext, endNode)) {
						return Evaluation.EXCLUDE_AND_PRUNE;
					}
					
					return Evaluation.INCLUDE_AND_CONTINUE;
				}
			});
		}
		
		// set rel type and direction
		int numRels = relTypes.size();
		for(int i=0; i<numRels; i++) {
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
		
		if(rel != null && rel.hasProperty(propertyKey.name())) {
			
			Object value = rel.getProperty(propertyKey.name());
			return value.equals(propertyValue);
		}
		
		return false;
	}

	protected boolean hasPropertyValue(Node node, PropertyKey propertyKey, Object propertyValue) {
		
		if(node != null && node.hasProperty(propertyKey.name())) {
			
			Object value = node.getProperty(propertyKey.name());
			return value.equals(propertyValue);
		}
		
		return false;
	}
	
	protected boolean hasType(Node node, Class type) {
		return hasPropertyValue(node, AbstractNode.Key.type, type.getSimpleName());
	}
}
