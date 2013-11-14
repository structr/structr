package org.structr.rest.entity;

import org.structr.core.entity.ManyToMany;

/**
 *
 * @author Christian Morgner
 */
public class FourOneManyToMany extends ManyToMany<TestFour, TestOne> {

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
		return "MANY_TO_MANY";
	}
}
