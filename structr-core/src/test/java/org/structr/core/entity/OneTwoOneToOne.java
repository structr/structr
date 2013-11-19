package org.structr.core.entity;

import org.structr.core.property.SourceId;
import org.structr.core.property.TargetId;

/**
 *
 * @author Christian Morgner
 */
public class OneTwoOneToOne extends OneToOne<TestOne, TestTwo> {
	
	@Override
	public Class<TestOne> getSourceType() {
		return TestOne.class;
	}

	@Override
	public String name() {
		return "IS_AT";
	}

	@Override
	public Class<TestTwo> getTargetType() {
		return TestTwo.class;
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
