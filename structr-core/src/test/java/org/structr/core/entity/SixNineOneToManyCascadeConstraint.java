package org.structr.core.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.TestRelType;

/**
 *
 * @author Christian Morgner
 */
public class SixNineOneToManyCascadeConstraint extends OneToMany<TestSix, TestNine> {
	
	@Override
	public Class<TestSix> getSourceType() {
		return TestSix.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return TestRelType.ONE_TO_MANY;
	}

	@Override
	public Class<TestNine> getTargetType() {
		return TestNine.class;
	}
	
	@Override
	public int getCascadingDeleteFlag() {
		return Relation.CONSTRAINT_BASED;
	}
}
