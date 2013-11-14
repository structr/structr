package org.structr.rest.entity;

import org.structr.core.entity.ManyToMany;

/**
 *
 * @author Christian Morgner
 */
public class SixEightManyToMany extends ManyToMany<TestSix, TestEight> {

	@Override
	public Class<TestSix> getSourceType() {
		return TestSix.class;
	}

	@Override
	public Class<TestEight> getTargetType() {
		return TestEight.class;
	}

	@Override
	public String name() {
		return "HAS";
	}

}
