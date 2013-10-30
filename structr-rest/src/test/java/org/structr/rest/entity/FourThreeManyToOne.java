package org.structr.rest.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.ManyToOne;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Christian Morgner
 */
public class FourThreeManyToOne extends ManyToOne<TestFour, TestThree> {

	@Override
	public Class<TestFour> getSourceType() {
		return TestFour.class;
	}

	@Override
	public Class<TestThree> getTargetType() {
		return TestThree.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return TestRestRelType.MANY_TO_ONE;
	}
}
