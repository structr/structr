package org.structr.rest.entity;

import org.structr.core.entity.OneToMany;

/**
 *
 * @author Christian Morgner
 */
public class SevenSixOneToMany extends OneToMany<TestSeven, TestSix> {

	@Override
	public Class<TestSeven> getSourceType() {
		return TestSeven.class;
	}

	@Override
	public Class<TestSix> getTargetType() {
		return TestSix.class;
	}

	@Override
	public String name() {
		return "HAS";
	}
}
