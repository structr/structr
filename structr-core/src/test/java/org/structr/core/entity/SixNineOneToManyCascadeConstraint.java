package org.structr.core.entity;

import org.structr.core.property.SourceId;
import org.structr.core.property.TargetId;

/**
 *
 * @author Christian Morgner
 */
public class SixNineOneToManyCascadeConstraint extends OneToMany<TestSix, TestNine> {
	
	@Override
	public Class<TestSix> getSourceType() {
		return TestSix.class;
	}

	@Override
	public String name() {
		return "ONE_TO_MANY";
	}

	@Override
	public Class<TestNine> getTargetType() {
		return TestNine.class;
	}
	
	@Override
	public int getCascadingDeleteFlag() {
		return Relation.CONSTRAINT_BASED;
	}

	@Override
	public SourceId getSourceIdProperty() {
		return null;
	}

	@Override
	public TargetId getTargetIdProperty() {
		return null;
	}
}
