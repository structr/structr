package org.structr.core.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.test.OneToOne;

/**
 *
 * @author Christian Morgner
 */
public class OneTwo extends OneToOne<TestOne, TestTwo> {
	
	@Override
	public Class<TestOne> getSourceType() {
		return TestOne.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.IS_AT;
	}

	@Override
	public Class<TestTwo> getDestinationType() {
		return TestTwo.class;
	}
}
