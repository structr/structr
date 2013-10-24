package org.structr.core.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.TestRelType;
import org.structr.core.entity.test.OneToMany;

/**
 *
 * @author Christian Morgner
 */
public class SixOne extends OneToMany<TestSix, TestOne> {
	
	@Override
	public Class<TestSix> getSourceType() {
		return TestSix.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return TestRelType.ONE_TO_ONE;
	}

	@Override
	public Class<TestOne> getDestinationType() {
		return TestOne.class;
	}
}
