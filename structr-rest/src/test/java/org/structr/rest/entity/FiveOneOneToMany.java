package org.structr.rest.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.OneToMany;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Christian Morgner
 */
public class FiveOneOneToMany extends OneToMany<TestFive, TestOne> {

	@Override
	public Class<TestFive> getSourceType() {
		return TestFive.class;
	}

	@Override
	public Class<TestOne> getTargetType() {
		return TestOne.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return TestRestRelType.ONE_TO_MANY;
	}
	

}
