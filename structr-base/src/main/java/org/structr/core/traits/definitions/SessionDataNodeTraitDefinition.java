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

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SessionDataNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
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

public class SessionDataNodeTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String SESSION_ID_PROPERTY    = "sessionId";
	public static final String CONTEXT_PATH_PROPERTY  = "cpath";
	public static final String VHOST_PROPERTY         = "vhost";
	public static final String LAST_ACCESSED_PROPERTY = "lastAccessed";
	public static final String VERSION_PROPERTY       = "version";

	public SessionDataNodeTraitDefinition() {
		super(StructrTraits.SESSION_DATA_NODE);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(
			OnCreation.class, new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					if (graphObject instanceof NodeInterface node && node.is(StructrTraits.SESSION_DATA_NODE)) {

						node.as(SessionDataNode.class).incrementVersion();
					}
				}
			},

			OnModification.class, new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					if (graphObject instanceof NodeInterface node && node.is(StructrTraits.SESSION_DATA_NODE)) {

						node.as(SessionDataNode.class).incrementVersion();
					}
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

		final Property<String>  sessionIdProperty    = new StringProperty(SESSION_ID_PROPERTY).indexed();
		final Property<String>  contextPathProperty  = new StringProperty(CONTEXT_PATH_PROPERTY);
		final Property<String>  vhostProperty        = new StringProperty(VHOST_PROPERTY);
		final Property<Date>    lastAccessedProperty = new DateProperty(LAST_ACCESSED_PROPERTY).indexed();
		final Property<Long>    versionProperty      = new LongProperty(VERSION_PROPERTY);

		return newSet(
			sessionIdProperty,
			contextPathProperty,
			vhostProperty,
			lastAccessedProperty,
			versionProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Ui,
			newSet(
					SESSION_ID_PROPERTY, CONTEXT_PATH_PROPERTY, VHOST_PROPERTY, LAST_ACCESSED_PROPERTY, VERSION_PROPERTY
			),
			PropertyView.Public,
			newSet(
					SESSION_ID_PROPERTY, CONTEXT_PATH_PROPERTY, VHOST_PROPERTY, LAST_ACCESSED_PROPERTY, VERSION_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}