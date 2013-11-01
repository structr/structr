package org.structr.rest.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.OneToMany;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Christian Morgner
 */
public class ThreeFiveOneToMany extends OneToMany<TestThree, TestFive> {

	@Override
	public Class<TestThree> getSourceType() {
		return TestThree.class;
	}

	@Override
	public Class<TestFive> getTargetType() {
		return TestFive.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return TestRestRelType.ONE_TO_MANY;
	}
}
