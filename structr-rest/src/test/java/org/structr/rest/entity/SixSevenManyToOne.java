package org.structr.rest.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.ManyToOne;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Christian Morgner
 */
public class SixSevenManyToOne extends ManyToOne<TestSix, TestSeven> {

	@Override
	public Class<TestSix> getSourceType() {
		return TestSix.class;
	}

	@Override
	public Class<TestSeven> getTargetType() {
		return TestSeven.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return TestRestRelType.HAS;
	}

}
