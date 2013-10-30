package org.structr.rest.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.ManyToMany;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Christian Morgner
 */
public class NineEightManyToMany extends ManyToMany<TestNine, TestEight> {

	@Override
	public Class<TestNine> getSourceType() {
		return TestNine.class;
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
