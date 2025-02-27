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

import org.structr.api.graph.PropertyContainer;
import org.structr.common.Permission;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Security;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SecurityTraitWrapper extends GraphObjectTraitWrapper<RelationshipInterface> implements Security {

	private PropertyKey<String[]> allowedKey = null;

	public SecurityTraitWrapper(final Traits traits, final RelationshipInterface relationshipInterface) {

		super(traits, relationshipInterface);

		this.allowedKey = traits.key("allowed");
	}

	@Override
	public RelationshipInterface getRelationship() {
		return wrappedObject;
	}

	@Override
	public Principal getSourceNode() {
		return wrappedObject.getSourceNode().as(Principal.class);
	}

	@Override
	public NodeInterface getTargetNode() {
		return wrappedObject.getTargetNode();
	}

	@Override
	public boolean isAllowed(final Permission permission) {
		return isAllowed(wrappedObject, allowedKey, permission);
	}

	@Override
	public void setAllowed(final Set<String> allowed) {
		setAllowed(wrappedObject, allowedKey, allowed);
	}

	@Override
	public void setAllowed(final Permission... allowed) {
		setAllowed(wrappedObject, allowedKey, allowed);
	}

	@Override
	public Set<String> getPermissions() {
		return getPermissions(wrappedObject, allowedKey);
	}

	@Override
	public void addPermission(final Permission permission) {
		addPermission(wrappedObject, allowedKey, permission);
	}

	@Override
	public void addPermissions(final Set<Permission> permissions) {
		addPermissions(wrappedObject, allowedKey, permissions);
	}

	@Override
	public void removePermission(final Permission permission) {
		removePermission(wrappedObject, allowedKey, permission);
	}

	@Override
	public void removePermissions(final Set<Permission> permissions) {
		removePermissions(wrappedObject, allowedKey, permissions);
	}

	// ----- private methods -----
	private boolean isAllowed(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final Permission permission) {
		return getPermissions(graphObject, key).contains(permission.name());
	}

	private void setAllowed(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final Set<String> allowed) {

		String[] permissions = (String[]) allowed.toArray(new String[allowed.size()]);
		setAllowed(graphObject, key, permissions);
	}

	private void setAllowed(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final Permission... permissions) {

		Set<String> permissionSet = new HashSet<>();

		for (Permission permission : permissions) {

			permissionSet.add(permission.name());
		}

		setAllowed(graphObject, key, permissionSet);
	}

	private void setAllowed(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final String[] allowed) {

		if (allowed.length == 0) {

			StructrApp.getInstance().delete(graphObject);

		} else {

			final PropertyContainer propertyContainer = graphObject.getPropertyContainer();
			propertyContainer.setProperty(key.dbName(), allowed);

		}
	}

	private Set<String> getPermissions(final RelationshipInterface graphObject, final PropertyKey<String[]> key) {

		final PropertyContainer propertyContainer = graphObject.getPropertyContainer();
		return getPermissionSet(propertyContainer, key);
	}

	private void addPermission(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final Permission permission) {

		addPermissions(graphObject, key, Collections.singleton(permission));
	}

	private void addPermissions(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final Set<Permission> permissions) {

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

	private void removePermission(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final Permission permission) {

		final Set<String> permissionSet = getPermissions(graphObject, key);

		if (!permissionSet.contains(permission.name())) {

			return;
		}

		permissionSet.remove(permission.name());
		setAllowed(graphObject, key, permissionSet);
	}

	private void removePermissions(final RelationshipInterface graphObject, final PropertyKey<String[]> key, final Set<Permission> permissions) {

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

	private Set<String> getPermissionSet(final PropertyContainer propertyContainer, final PropertyKey<String[]> key) {

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
