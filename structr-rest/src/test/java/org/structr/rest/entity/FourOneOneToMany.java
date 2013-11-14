package org.structr.rest.entity;

import org.structr.core.entity.OneToMany;

/**
 *
 * @author Christian Morgner
 */
public class FourOneOneToMany extends OneToMany<TestFour, TestOne> {

	@Override
	public Class<TestFour> getSourceType() {
		return TestFour.class;
	}

	@Override
	public Class<TestOne> getTargetType() {
		return TestOne.class;
	}

	@Override
	public String name() {
		return "ONE_TO_MANY";
	}

}
