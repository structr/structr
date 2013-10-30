package org.structr.rest.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.ManyToMany;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Christian Morgner
 */
public class FourOneManyToMany extends ManyToMany<TestFour, TestOne> {

	@Override
	public Class<TestFour> getSourceType() {
		return TestFour.class;
	}

	@Override
	public Class<TestOne> getTargetType() {
		return TestOne.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return TestRestRelType.MANY_TO_MANY;
	}
}
