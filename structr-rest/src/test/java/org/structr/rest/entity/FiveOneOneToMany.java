package org.structr.rest.entity;

import org.structr.core.entity.OneToMany;
import org.structr.core.property.Property;

/**
 *
 * @author Christian Morgner
 */
public class FiveOneOneToMany extends OneToMany<TestFive, TestOne> {

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
		return "ONE_TO_MANY";
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
