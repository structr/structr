package org.structr.rest.entity;

import org.structr.core.entity.ManyToMany;

/**
 *
 * @author Christian Morgner
 */
public class NineEightManyToMany extends ManyToMany<TestNine, TestEight> {

	@Override
	public Class<TestNine> getSourceType() {
		return TestNine.class;
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
