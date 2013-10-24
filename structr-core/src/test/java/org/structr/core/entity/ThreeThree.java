package org.structr.core.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.TestRelType;
import org.structr.core.entity.test.OneToOne;

/**
 *
 * @author Christian Morgner
 */
public class ThreeThree extends OneToOne<TestThree, TestThree> {
	
	@Override
	public Class<TestOne> getSourceType() {
		return TestOne.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return TestRelType.ONE_TO_MANY;
	}

	@Override
	public Class<TestFour> getDestinationType() {
		return TestFour.class;
	}
}
