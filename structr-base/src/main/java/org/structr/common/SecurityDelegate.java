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
package org.structr.common;

import org.structr.api.graph.PropertyContainer;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Delegate class that implements the permission handling functions for
 * Security relationships and Principals.
 *
 *
 */
public class SecurityDelegate {

	public static boolean isAllowed(final RelationshipInterface graphObject, final PropertyKey<List<String>> key, final Permission permission) {
		return getPermissions(graphObject, key).contains(permission.name());
	}

	public static void setAllowed(final RelationshipInterface graphObject, final PropertyKey<List<String>> key, final Set<String> allowed) {
		setAllowed(graphObject, key, new LinkedList<>(allowed));
	}

	public static void setAllowed(final RelationshipInterface graphObject, final PropertyKey<List<String>> key, final Permission... permissions) {

		Set<String> permissionSet = new HashSet<>();

		for (Permission permission : permissions) {

			permissionSet.add(permission.name());
		}

		setAllowed(graphObject, key, permissionSet);
	}

	private static void setAllowed(final RelationshipInterface graphObject, final PropertyKey<List<String>> key, final List<String> allowed) {

		if (allowed == null || allowed.isEmpty()) {

			StructrApp.getInstance().delete((RelationshipInterface)graphObject);

		} else {

			final PropertyContainer propertyContainer = graphObject.getPropertyContainer();
			propertyContainer.setProperty(key.dbName(), allowed);

		}
	}

	public static Set<String> getPermissions(final RelationshipInterface graphObject, final PropertyKey<List<String>> key) {

		final PropertyContainer propertyContainer = graphObject.getPropertyContainer();
		return getPermissionSet(propertyContainer, key);
	}

	public static void addPermission(final RelationshipInterface graphObject, final PropertyKey<List<String>> key, final Permission permission) {

		addPermissions(graphObject, key, Collections.singleton(permission));
	}

	public static void addPermissions(final RelationshipInterface graphObject, final PropertyKey<List<String>> key, final Set<Permission> permissions) {

		final Set<String> permissionSet = getPermissions(graphObject, key);

		boolean change = false;

		for (final Permission p : permissions) {

			if (!permissionSet.contains(p.name())) {

				change = true;
				permissionSet.add(p.name());
			}
		};

		if (change) {
			setAllowed(graphObject, key, permissionSet);
		}
	}

	public static void removePermission(final RelationshipInterface graphObject, final PropertyKey<List<String>> key, final Permission permission) {

		final Set<String> permissionSet = getPermissions(graphObject, key);

		if (!permissionSet.contains(permission.name())) {

			return;
		}

		permissionSet.remove(permission.name());
		setAllowed(graphObject, key, permissionSet);
	}

	public static void removePermissions(final RelationshipInterface graphObject, final PropertyKey<List<String>> key, final Set<Permission> permissions) {

		final Set<String> permissionSet = getPermissions(graphObject, key);

		boolean change = false;

		for (final Permission p : permissions) {

			if (permissionSet.contains(p.name())) {

				change = true;
				permissionSet.remove(p.name());
			}
		};

		if (change) {
			setAllowed(graphObject, key, permissionSet);
		}
	}

	public static Set<String> getPermissionSet(final PropertyContainer propertyContainer, final PropertyKey<List<String>> key) {

		final Set<String> permissionSet = new HashSet<>();

		if (propertyContainer.hasProperty(key.dbName())) {

			final List<String> permissions = (List<String>)propertyContainer.getProperty(key.dbName());
			if (permissions != null) {

				permissionSet.addAll(permissions);
			}
		}

		return permissionSet;
	}
}
