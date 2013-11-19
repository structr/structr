package org.structr.rest.entity;

import org.structr.core.entity.OneToMany;
import org.structr.core.property.Property;

/**
 *
 * @author Christian Morgner
 */
public class FourOneOneToMany extends OneToMany<TestFour, TestOne> {

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
