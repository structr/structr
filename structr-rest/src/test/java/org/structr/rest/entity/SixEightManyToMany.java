package org.structr.rest.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.ManyToMany;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Christian Morgner
 */
public class SixEightManyToMany extends ManyToMany<TestSix, TestEight> {

	@Override
	public Class<TestSix> getSourceType() {
		return TestSix.class;
	}

	@Override
	public Class<TestEight> getTargetType() {
		return TestEight.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return TestRestRelType.HAS;
	}

}
