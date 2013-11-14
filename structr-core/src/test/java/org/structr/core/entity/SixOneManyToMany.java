package org.structr.core.entity;

/**
 *
 * @author Christian Morgner
 */
public class SixOneManyToMany extends ManyToMany<TestSix, TestOne> {
	
	@Override
	public Class<TestSix> getSourceType() {
		return TestSix.class;
	}

	@Override
	public String name() {
		return "MANY_TO_MANY";
	}

	@Override
	public Class<TestOne> getTargetType() {
		return TestOne.class;
	}
}
