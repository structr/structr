package org.structr.core.entity;

/**
 *
 * @author Christian Morgner
 */
public class OneThreeOneToOne extends OneToOne<TestOne, TestThree> {
	
	@Override
	public Class<TestOne> getSourceType() {
		return TestOne.class;
	}

	@Override
	public String name() {
		return "OWNS";
	}

	@Override
	public Class<TestThree> getTargetType() {
		return TestThree.class;
	}
}
