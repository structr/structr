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
package org.structr.core.traits.wrappers;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.structr.core.traits.definitions.ResourceAccessTraitDefinition;

public class ResourceAccessTraitWrapper extends AbstractNodeTraitWrapper implements ResourceAccess {

	private static final Map<String, List<ResourceAccess>> permissionsCache = new ConcurrentHashMap<>();

	// non-static members
	private String cachedResourceSignature = null;
	private Long cachedFlags               = null;

	public ResourceAccessTraitWrapper(final Traits traits, final NodeInterface nodeInterface) {
		super(traits, nodeInterface);
	}

	@Override
	public boolean hasFlag(long flag) {
		return (getFlags() & flag) == flag;
	}

	@Override
	public void setFlag(final long flag) throws FrameworkException {

		cachedFlags = null;

		wrappedObject.setProperty(traits.key(ResourceAccessTraitDefinition.FLAGS_PROPERTY), getFlags() | flag);
	}

	@Override
	public void clearFlag(final long flag) throws FrameworkException {

		cachedFlags = null;

		wrappedObject.setProperty(traits.key(ResourceAccessTraitDefinition.FLAGS_PROPERTY), getFlags() & ~flag);
	}

	@Override
	public long getFlags() {

		if (cachedFlags == null) {

			cachedFlags = wrappedObject.getProperty(traits.key(ResourceAccessTraitDefinition.FLAGS_PROPERTY));
		}

		if (cachedFlags != null) {
			return cachedFlags;
		}

		return 0;
	}

	@Override
	public String getResourceSignature() {

		if (cachedResourceSignature == null) {
			cachedResourceSignature = wrappedObject.getProperty(traits.key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY));
		}

		return cachedResourceSignature;
	}

	// ----- public static methods -----
	public static void clearCache() {
		permissionsCache.clear();
	}

	public static List<ResourceAccess> findPermissions(final SecurityContext securityContext, final String signature) throws FrameworkException {

		final Traits traits = Traits.of(StructrTraits.RESOURCE_ACCESS);
		List<ResourceAccess> permissions = permissionsCache.get(signature);
		if (permissions == null) {

			permissions = new LinkedList<>();

			// Ignore securityContext here (so we can cache all permissions for a signature independent of a user)
			final List<NodeInterface> nodes = StructrApp.getInstance().nodeQuery(StructrTraits.RESOURCE_ACCESS).key(traits.key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY), signature).getAsList();

			for (final NodeInterface node : nodes) {

				permissions.add(node.as(ResourceAccess.class));
			}

			permissionsCache.put(signature, permissions);
		}

		return permissions;
	}
}
