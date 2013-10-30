package org.structr.core.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;

/**
 *
 * @author Christian Morgner
 */
public class OneThreeOneToOne extends OneToOne<TestOne, TestThree> {
	
	@Override
	public Class<TestOne> getSourceType() {
		return TestOne.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.OWNS;
	}

	@Override
	public Class<TestThree> getTargetType() {
		return TestThree.class;
	}
}
