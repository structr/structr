/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.neo4j.graphdb.PropertyContainer;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;

/**
 * Delegate class that implements the permission handling functions for
 * Security relationships and Principals.
 *
 *
 */
public class SecurityDelegate {

	public static boolean isAllowed(final GraphObject graphObject, final PropertyKey<String[]> key, final Permission permission) {
		return getPermissions(graphObject, key).contains(permission.name());
	}

	public static void setAllowed(final GraphObject graphObject, final PropertyKey<String[]> key, final Set<String> allowed) {

		String[] permissions = (String[]) allowed.toArray(new String[allowed.size()]);
		setAllowed(graphObject, key, permissions);
	}

	public static void setAllowed(final GraphObject graphObject, final PropertyKey<String[]> key, final Permission... permissions) {

		Set<String> permissionSet = new HashSet<>();

		for (Permission permission : permissions) {

			permissionSet.add(permission.name());
		}

		setAllowed(graphObject, key, permissionSet);
	}

	public static void setAllowed(final GraphObject graphObject, final PropertyKey<String[]> key, final String[] allowed) {

		final PropertyContainer propertyContainer = graphObject.getPropertyContainer();
		propertyContainer.setProperty(key.dbName(), allowed);
	}

	public static Set<String> getPermissions(final GraphObject graphObject, final PropertyKey<String[]> key) {

		final PropertyContainer propertyContainer = graphObject.getPropertyContainer();
		return getPermissionSet(propertyContainer, key);
	}

	public static void addPermission(final GraphObject graphObject, final PropertyKey<String[]> key, final Permission permission) {

		Set<String> permissionSet = getPermissions(graphObject, key);

		if (permissionSet.contains(permission.name())) {

			return;
		}

		permissionSet.add(permission.name());
		setAllowed(graphObject, key, permissionSet);
	}

	public static void removePermission(final GraphObject graphObject, final PropertyKey<String[]> key, final Permission permission) {

		final Set<String> permissionSet = getPermissions(graphObject, key);

		if (!permissionSet.contains(permission.name())) {

			return;
		}

		permissionSet.remove(permission.name());
		setAllowed(graphObject, key, permissionSet);
	}

	public static Set<String> getPermissionSet(final PropertyContainer propertyContainer, final PropertyKey<String[]> key) {

		final Set<String> permissionSet = new HashSet<>();

		if (propertyContainer.hasProperty(key.dbName())) {

			final String[] permissions = (String[])propertyContainer.getProperty(key.dbName());
			if (permissions != null) {

				permissionSet.addAll(Arrays.asList(permissions));
			}
		}

		return permissionSet;
	}
}
