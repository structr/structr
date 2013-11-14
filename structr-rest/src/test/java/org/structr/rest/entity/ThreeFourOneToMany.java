package org.structr.rest.entity;

import org.structr.core.entity.OneToMany;

/**
 *
 * @author Christian Morgner
 */
public class ThreeFourOneToMany extends OneToMany<TestThree, TestFour> {

	@Override
	public Class<TestThree> getSourceType() {
		return TestThree.class;
	}

	@Override
	public Class<TestFour> getTargetType() {
		return TestFour.class;
	}

	@Override
	public String name() {
		return "ONE_TO_MANY";
	}
}
