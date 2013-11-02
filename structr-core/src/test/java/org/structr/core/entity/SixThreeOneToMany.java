package org.structr.core.entity;

/**
 *
 * @author Christian Morgner
 */
public class SixThreeOneToMany extends OneToMany<TestSix, TestThree> {
	
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
