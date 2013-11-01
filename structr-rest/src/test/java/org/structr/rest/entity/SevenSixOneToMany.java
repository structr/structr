package org.structr.rest.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.OneToMany;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Christian Morgner
 */
public class SevenSixOneToMany extends OneToMany<TestSeven, TestSix> {

	@Override
	public Class<TestSeven> getSourceType() {
		return TestSeven.class;
	}

	@Override
	public Class<TestSix> getTargetType() {
		return TestSix.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return TestRestRelType.HAS;
	}

}
