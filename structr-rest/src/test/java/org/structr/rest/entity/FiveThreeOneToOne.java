package org.structr.rest.entity;

import org.structr.core.entity.OneToOne;
import org.structr.core.property.Property;

/**
 *
 * @author Christian Morgner
 */
public class FiveThreeOneToOne extends OneToOne<TestFive, TestThree> {

	@Override
	public Class<TestFive> getSourceType() {
		return TestFive.class;
	}

	@Override
	public Class<TestThree> getTargetType() {
		return TestThree.class;
	}

	@Override
	public String name() {
		return "ONE_TO_ONE";
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
