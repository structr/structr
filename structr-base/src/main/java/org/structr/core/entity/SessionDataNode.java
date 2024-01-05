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
package org.structr.core.entity;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.DateProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

import java.util.Date;

/**
 * Storage object for session data.
 */

public class SessionDataNode extends AbstractNode {

	public static final Property<String>               sessionId    = new StringProperty("sessionId").indexed();
	public static final Property<String>               contextPath  = new StringProperty("cpath");
	public static final Property<String>               vhost        = new StringProperty("vhost");
	public static final Property<Date>                 lastAccessed = new DateProperty("lastAccessed").indexed();
	public static final Property<Long>                 version      = new LongProperty("version");

	public static final View uiView = new View(SessionDataNode.class, PropertyView.Ui,
		sessionId, contextPath, vhost, lastAccessed, version
	);

	public static final View publicView = new View(SessionDataNode.class, PropertyView.Public,
		sessionId, contextPath, vhost, lastAccessed, version
	);

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		incrementVersion();
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		incrementVersion();
	}

	// ----- private methods -----
	private void incrementVersion() throws FrameworkException {

		// increment version on each change
		final Long version = getProperty(SessionDataNode.version);
		if (version == null) {

			setProperty(SessionDataNode.version, 1L);

		} else {
			
			setProperty(SessionDataNode.version,  version + 1);
		}
	}
}