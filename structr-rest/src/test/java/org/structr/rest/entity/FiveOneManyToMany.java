package org.structr.rest.entity;

import org.structr.core.entity.ManyToMany;

/**
 *
 * @author Christian Morgner
 */
public class FiveOneManyToMany extends ManyToMany<TestFive, TestOne> {

	@Override
	public Class<TestFive> getSourceType() {
		return TestFive.class;
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
