package org.structr.rest.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.OneToOne;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Christian Morgner
 */
public class TenSevenOneToOne extends OneToOne<TestTen, TestSeven> {

	@Override
	public Class<TestTen> getSourceType() {
		return TestTen.class;
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
