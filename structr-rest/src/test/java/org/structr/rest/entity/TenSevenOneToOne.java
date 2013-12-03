package org.structr.rest.entity;

import org.structr.core.entity.OneToOne;

/**
 *
 * @author Christian Morgner
 */
public class TenSevenOneToOne extends OneToOne<TestTen, TestSeven> {

	@Override
	public Class<TestTen> getSourceType() {
		return TestTen.class;
	}

	@Override
	public Class<TestSeven> getTargetType() {
		return TestSeven.class;
	}

	@Override
	public String name() {
		return "HAS";
	}
}
