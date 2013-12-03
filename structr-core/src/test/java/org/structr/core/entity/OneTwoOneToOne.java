package org.structr.core.entity;

/**
 *
 * @author Christian Morgner
 */
public class OneTwoOneToOne extends OneToOne<TestOne, TestTwo> {
	
	@Override
	public Class<TestOne> getSourceType() {
		return TestOne.class;
	}

	@Override
	public String name() {
		return "IS_AT";
	}

	@Override
	public Class<TestTwo> getTargetType() {
		return TestTwo.class;
	}
}
