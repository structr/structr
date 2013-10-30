package org.structr.rest.entity;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.OneToOne;

/**
 *
 * @author Christian Morgner
 */
public class GroupThreeOneOneToOne extends OneToOne<TestGroupPropThree, TestGroupPropOne> {

	@Override
	public Class<TestGroupPropThree> getSourceType() {
		return TestGroupPropThree.class;
	}

	@Override
	public Class<TestGroupPropOne> getTargetType() {
		return TestGroupPropOne.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.OWNS;
	}

}
