package org.structr.rest.entity;

import org.structr.core.entity.OneToOne;
import org.structr.core.property.Property;

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

	@Override
	public Property<String> getSourceIdProperty() {
		return null;
	}

	@Override
	public Property<String> getTargetIdProperty() {
		return null;
	}
}
