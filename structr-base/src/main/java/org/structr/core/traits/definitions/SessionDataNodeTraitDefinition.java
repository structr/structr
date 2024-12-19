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
package org.structr.core.traits.definitions;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SessionDataNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.wrappers.SessionDataNodeTraitWrapper;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Storage object for session data.
 */

public class SessionDataNodeTraitDefinition extends AbstractTraitDefinition {

	private static final Property<String>               sessionIdProperty    = new StringProperty("sessionId").indexed();
	private static final Property<String>               contextPathProperty  = new StringProperty("cpath");
	private static final Property<String>               vhostProperty        = new StringProperty("vhost");
	private static final Property<Date>                 lastAccessedProperty = new DateProperty("lastAccessed").indexed();
	private static final Property<Long>                 versionProperty      = new LongProperty("version");

	public SessionDataNodeTraitDefinition() {
		super("SessionDataNode");
	}

	/*
	public static final View uiView = new View(SessionDataNode.class, PropertyView.Ui,
		sessionId, contextPath, vhost, lastAccessed, version
	);

	public static final View publicView = new View(SessionDataNode.class, PropertyView.Public,
		sessionId, contextPath, vhost, lastAccessed, version
	);
	*/

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(
			OnCreation.class, new OnCreation() {

				@Override
				public void onCreation(GraphObject graphObject, SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
					incrementVersion(graphObject);
				}
			},

			OnModification.class, new OnModification() {

				@Override
				public void onModification(GraphObject graphObject, SecurityContext securityContext, ErrorBuffer errorBuffer, ModificationQueue modificationQueue) throws FrameworkException {
					incrementVersion(graphObject);
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			SessionDataNode.class, (traits, node) -> new SessionDataNodeTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return Set.of(
			sessionIdProperty,
			contextPathProperty,
			vhostProperty,
			lastAccessedProperty,
			versionProperty
		);
	}

	// ----- private methods -----
	private void incrementVersion(final GraphObject graphObject) throws FrameworkException {

		// increment version on each change
		final Long version = graphObject.getProperty(versionProperty);
		if (version == null) {

			graphObject.setProperty(versionProperty, 1L);

		} else {

			graphObject.setProperty(versionProperty,  version + 1);
		}
	}
}