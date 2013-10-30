package org.structr.core.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.TestRelType;

/**
 *
 * @author Christian Morgner
 */
public class SixOneManyToMany extends ManyToMany<TestSix, TestOne> {
	
	@Override
	public Class<TestSix> getSourceType() {
		return TestSix.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return TestRelType.MANY_TO_MANY;
	}

	@Override
	public Class<TestOne> getTargetType() {
		return TestOne.class;
	}
}
