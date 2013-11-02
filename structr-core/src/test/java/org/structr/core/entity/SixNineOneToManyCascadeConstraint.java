package org.structr.core.entity;

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
}
