package org.structr.rest.entity;

import org.structr.core.entity.ManyToMany;
import org.structr.core.property.Property;

/**
 *
 * @author Christian Morgner
 */
public class FourOneManyToMany extends ManyToMany<TestFour, TestOne> {

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
		return "MANY_TO_MANY";
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
