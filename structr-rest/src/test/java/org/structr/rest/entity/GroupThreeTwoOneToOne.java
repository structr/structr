package org.structr.rest.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.OneToOne;

/**
 *
 * @author Christian Morgner
 */
public class GroupThreeTwoOneToOne extends OneToOne<TestGroupPropThree, TestGroupPropTwo> {

	@Override
	public Class<TestGroupPropThree> getSourceType() {
		return TestGroupPropThree.class;
	}

	@Override
	public Class<TestGroupPropTwo> getTargetType() {
		return TestGroupPropTwo.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.OWNS;
	}

}
