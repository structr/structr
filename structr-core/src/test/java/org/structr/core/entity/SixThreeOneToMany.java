package org.structr.core.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.TestRelType;

/**
 *
 * @author Christian Morgner
 */
public class SixThreeOneToMany extends OneToMany<TestSix, TestThree> {
	
	@Override
	public Class<TestSix> getSourceType() {
		return TestSix.class;
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
