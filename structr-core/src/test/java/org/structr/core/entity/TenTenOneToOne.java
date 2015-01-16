package org.structr.core.entity;

/**
 *
 * @author Christian Morgner
 */
public class TenTenOneToOne extends OneToOne<TestTen, TestTen> {

	@Override
	public Class<TestTen> getSourceType() {
		return TestTen.class;
	}

	@Override
	public Class<TestTen> getTargetType() {
		return TestTen.class;
	}

	@Override
	public String name() {
		return "CASCADE";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}
