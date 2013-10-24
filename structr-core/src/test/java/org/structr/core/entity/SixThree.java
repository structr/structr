package org.structr.core.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.TestRelType;
import org.structr.core.entity.test.OneToOne;

/**
 *
 * @author Christian Morgner
 */
public class SixThree extends OneToOne<TestSix, TestThree> {
	
	@Override
	public Class<TestSix> getSourceType() {
		return TestSix.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return TestRelType.ONE_TO_ONE;
	}

	@Override
	public Class<TestThree> getDestinationType() {
		return TestThree.class;
	}
}
