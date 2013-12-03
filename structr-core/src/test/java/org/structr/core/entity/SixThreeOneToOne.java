package org.structr.core.entity;

/**
 *
 * @author Christian Morgner
 */
public class SixThreeOneToOne extends OneToOne<TestSix, TestThree> {
	
	@Override
	public Class<TestSix> getSourceType() {
		return TestSix.class;
	}

	@Override
	public String name() {
		return "ONE_TO_MANY";
	}

	@Override
	public Class<TestThree> getTargetType() {
		return TestThree.class;
	}
}
