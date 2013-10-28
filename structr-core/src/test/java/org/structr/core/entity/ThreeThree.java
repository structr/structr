package org.structr.core.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.TestRelType;

/**
 *
 * @author Christian Morgner
 */
public class ThreeThree extends OneToOne<TestThree, TestThree> {
	
	@Override
	public Class<TestThree> getSourceType() {
		return TestThree.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return TestRelType.ONE_TO_MANY;
	}

	@Override
	public Class<TestThree> getTargetType() {
		return TestThree.class;
	}
}
