package org.structr.core.entity;

import org.structr.core.property.SourceId;
import org.structr.core.property.TargetId;

/**
 *
 * @author Christian Morgner
 */
public class SixThreeOneToOne extends OneToOne<TestSix, TestThree> {
	
	@Override
	public Class<TestSix> getSourceType() {
		return TestSix.class;
	}

	@Override
	public String name() {
		return "ONE_TO_MANY";
	}

	@Override
	public Class<TestThree> getTargetType() {
		return TestThree.class;
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
