package org.structr.rest.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.OneToMany;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Christian Morgner
 */
public class ThreeFourOneToMany extends OneToMany<TestThree, TestFour> {

	@Override
	public Class<TestThree> getSourceType() {
		return TestThree.class;
	}

	@Override
	public Class<TestFour> getTargetType() {
		return TestFour.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return TestRestRelType.ONE_TO_MANY;
	}
}
