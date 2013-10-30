package org.structr.rest.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.OneToOne;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Christian Morgner
 */
public class FiveThreeOneToOne extends OneToOne<TestFive, TestThree> {

	@Override
	public Class<TestFive> getSourceType() {
		return TestFive.class;
	}

	@Override
	public Class<TestThree> getTargetType() {
		return TestThree.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return TestRestRelType.ONE_TO_ONE;
	}
}
