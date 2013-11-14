package org.structr.rest.entity;

import org.structr.core.entity.OneToMany;

/**
 *
 * @author Christian Morgner
 */
public class ThreeFiveOneToMany extends OneToMany<TestThree, TestFive> {

	@Override
	public Class<TestThree> getSourceType() {
		return TestThree.class;
	}

	@Override
	public Class<TestFive> getTargetType() {
		return TestFive.class;
	}

	@Override
	public String name() {
		return "ONE_TO_MANY";
	}
}
