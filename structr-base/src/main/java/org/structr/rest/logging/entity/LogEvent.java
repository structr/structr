/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.rest.logging.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

import java.util.Date;

/**
 *
 *
 */
public class LogEvent extends AbstractNode {

	public static final Property<String> messageProperty   = new StringProperty("message");
	public static final Property<String> actionProperty    = new StringProperty("action").indexed();
	public static final Property<Date>   timestampProperty = new ISO8601DateProperty("timestamp").indexed();
	public static final Property<String> subjectProperty   = new StringProperty("subject").indexed();
	public static final Property<String> objectProperty    = new StringProperty("object").indexed();

	public static final View defaultView = new View(LogEvent.class, PropertyView.Public,
		actionProperty, messageProperty, timestampProperty, subjectProperty, objectProperty
	);

	public long getTimestamp() {

		final Date date = getProperty(LogEvent.timestampProperty);
		if (date != null) {

			return date.getTime();
		}

		return 0L;
	}

	public String getAction() {
		return getProperty(LogEvent.actionProperty);
	}

	public String getMessage() {
		return getProperty(LogEvent.messageProperty);
	}

	public String getSubjectId() {
		return getProperty(LogEvent.subjectProperty);
	}

	public String getObjectId() {
		return getProperty(LogEvent.objectProperty);
	}
}
