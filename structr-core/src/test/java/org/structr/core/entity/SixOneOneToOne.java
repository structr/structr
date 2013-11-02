package org.structr.core.entity;

/**
 *
 * @author Christian Morgner
 */
public class SixOneOneToOne extends OneToMany<TestSix, TestOne> {
	
	@Override
	public Class<TestSix> getSourceType() {
		return TestSix.class;
	}

	@Override
	public String name() {
		return "ONE_TO_ONE";
	}

	@Override
	public Class<TestOne> getTargetType() {
		return TestOne.class;
	}
}
