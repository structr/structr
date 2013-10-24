package org.structr.core.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;

/**
 *
 * @author Christian Morgner
 */
public class OneTwo extends AbstractRelationship<TestOne, TestTwo> {
	
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
