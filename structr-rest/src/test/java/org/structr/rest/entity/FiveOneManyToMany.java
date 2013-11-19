package org.structr.rest.entity;

import org.structr.core.entity.ManyToMany;
import org.structr.core.property.Property;

/**
 *
 * @author Christian Morgner
 */
public class FiveOneManyToMany extends ManyToMany<TestFive, TestOne> {

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
