package org.structr.rest.traits.wrappers;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractTraitWrapper;
import org.structr.rest.entity.LogEvent;

import java.util.Date;

public class LogEventTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements LogEvent {

	public LogEventTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	@Override
	public long getTimestamp() {

		final Date date = wrappedObject.getProperty(traits.key("timestampProperty"));
		if (date != null) {

			return date.getTime();
		}

		return 0L;
	}

	@Override
	public String getAction() {
		return wrappedObject.getProperty(traits.key("actionProperty"));
	}

	@Override
	public String getMessage() {
		return wrappedObject.getProperty(traits.key("messageProperty"));
	}

	@Override
	public String getSubjectId() {
		return wrappedObject.getProperty(traits.key("subjectProperty"));
	}

	@Override
	public String getObjectId() {
		return wrappedObject.getProperty(traits.key("objectProperty"));
	}
}
