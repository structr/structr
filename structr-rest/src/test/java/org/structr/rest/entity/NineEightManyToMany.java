package org.structr.rest.entity;

import org.structr.core.entity.ManyToMany;
import org.structr.core.property.Property;

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

	@Override
	public Property<String> getSourceIdProperty() {
		return null;
	}

	@Override
	public Property<String> getTargetIdProperty() {
		return null;
	}
}
