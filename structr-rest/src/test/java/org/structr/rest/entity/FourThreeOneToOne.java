package org.structr.rest.entity;

import org.structr.core.entity.OneToOne;

/**
 *
 * @author Christian Morgner
 */
public class FourThreeOneToOne extends OneToOne<TestFour, TestThree> {

	@Override
	public Class<TestFour> getSourceType() {
		return TestFour.class;
	}

	@Override
	public Class<TestThree> getTargetType() {
		return TestThree.class;
	}

	@Override
	public String name() {
		return "ONE_TO_ONE";
	}
}
