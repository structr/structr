package org.structr.rest.entity;

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
	public String name() {
		return "OWNS";
	}
}
