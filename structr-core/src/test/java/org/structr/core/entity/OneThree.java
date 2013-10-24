package org.structr.core.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;

/**
 *
 * @author Christian Morgner
 */
public class OneThree extends AbstractRelationship<TestOne, TestThree> {
	
	@Override
	public Class<TestOne> getSourceType() {
		return TestOne.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.OWNS;
	}

	@Override
	public Class<TestThree> getDestinationType() {
		return TestThree.class;
	}
}
