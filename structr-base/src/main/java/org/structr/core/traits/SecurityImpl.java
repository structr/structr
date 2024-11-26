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
package org.structr.core.traits;

import org.structr.api.graph.PropertyContainer;
import org.structr.common.Permission;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.property.PropertyKey;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SecurityImpl extends ManyToManyTrait<Principal, NodeTrait> implements Security  {

	private final PropertyKey<String[]> key;

	public SecurityImpl(final PropertyContainer propertyContainer) {

		super(propertyContainer);

		key = traits.get("Security").key("allowed");
	}

	@Override
	public Trait<Principal> getSourceType() {
		return null;
	}

	@Override
	public Trait<NodeTrait> getTargetType() {
		return null;
	}

	@Override
	public Trait<Relation<Principal, NodeTrait, ManyStartpoint<Principal>, ManyEndpoint<NodeTrait>>> getTrait() {
		return null;
	}

	public boolean isAllowed(final Permission permission) {
		return getPermissions().contains(permission.name());
	}

	public void setAllowed(final Set<String> allowed) {

		String[] permissions = allowed.toArray(new String[allowed.size()]);
		setAllowed(permissions);
	}

	public void setAllowed(final Permission... permissions) {

		Set<String> permissionSet = new HashSet<>();

		for (Permission permission : permissions) {

			permissionSet.add(permission.name());
		}

		setAllowed(permissionSet);
	}

	private void setAllowed(final String[] allowed) {

		if (allowed.length == 0) {

			StructrApp.getInstance().delete(this);

		} else {

			final PropertyContainer propertyContainer = getPropertyContainer();
			propertyContainer.setProperty(key.dbName(), allowed);

		}
	}

	public Set<String> getPermissions() {
		return getPermissionSet();
	}

	public void addPermission(final Permission permission) {

		addPermissions(Collections.singleton(permission));
	}

	public void addPermissions(final Set<Permission> permissions) {

		final Set<String> permissionSet = getPermissions();

		boolean change = false;

		for (final Permission p : permissions) {

			if (!permissionSet.contains(p.name())) {

				change = true;
				permissionSet.add(p.name());
			}
		};

		if (change) {

			setAllowed(permissionSet);
		}
	}

	public void removePermission(final Permission permission) {

		final Set<String> permissionSet = getPermissions();

		if (!permissionSet.contains(permission.name())) {

			return;
		}

		permissionSet.remove(permission.name());
		setAllowed(permissionSet);
	}

	public void removePermissions(final Set<Permission> permissions) {

		final Set<String> permissionSet = getPermissions();

		boolean change = false;

		for (final Permission p : permissions) {

			if (permissionSet.contains(p.name())) {

				change = true;
				permissionSet.remove(p.name());
			}
		};

		if (change) {
			setAllowed(permissionSet);
		}
	}

	public Set<String> getPermissionSet() {

		final PropertyContainer propertyContainer = getPropertyContainer();
		final Set<String> permissionSet           = new HashSet<>();

		if (propertyContainer.hasProperty(key.dbName())) {

			final String[] permissions = (String[])propertyContainer.getProperty(key.dbName());
			if (permissions != null) {

				permissionSet.addAll(Arrays.asList(permissions));
			}
		}

		return permissionSet;
	}
}
