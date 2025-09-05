/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.traits.wrappers;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.rest.entity.LogEvent;

import java.util.Date;

public class LogEventTraitWrapper extends AbstractNodeTraitWrapper implements LogEvent {

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
