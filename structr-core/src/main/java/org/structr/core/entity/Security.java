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
package org.structr.core.entity;


//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.api.graph.Relationship;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.SecurityDelegate;
import org.structr.common.View;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.SourceId;
import org.structr.core.property.TargetId;

/**
 * Relationship type class for SECURITY relationships.
 */
public class Security extends ManyToMany<Principal, NodeInterface> {

	public static final SourceId           principalId          = new SourceId("principalId");
	public static final TargetId           accessControllableId = new TargetId("accessControllableId");
	public static final Property<String[]> allowed              = new ArrayProperty("allowed", String.class);

	public static final View uiView = new View(Security.class, PropertyView.Ui,
		allowed
	);

	public Security() {}

	public Security(SecurityContext securityContext, Relationship dbRelationship) {
		init(securityContext, dbRelationship, Security.class);
	}

	@Override
	public Iterable<PropertyKey> getPropertyKeys(String propertyView) {

		Set<PropertyKey> keys = new LinkedHashSet<>();

		keys.addAll((Set<PropertyKey>) super.getPropertyKeys(propertyView));

		keys.add(principalId);
		keys.add(accessControllableId);

		if (dbRelationship != null) {

			for (String key : dbRelationship.getPropertyKeys()) {
				keys.add(StructrApp.getConfiguration().getPropertyKeyForDatabaseName(entityType, key));
			}
		}

		return keys;
	}

	public boolean isAllowed(final Permission permission) {
		return SecurityDelegate.getPermissionSet(dbRelationship, Security.allowed).contains(permission.name());

		/*
		final Set<String> permissionSet = SecurityDelegate.getPermissionSet(dbRelationship, Security.allowed);
		final Principal principal       = getSourceNode();

		if (principal != null) {

			final Set<String> allowedPermissions = SecurityDelegate.getPermissionSet(principal.getNode(), Principal.allowed);
			final Set<String> deniedPermissions  = SecurityDelegate.getPermissionSet(principal.getNode(), Principal.denied);

			if (allowedPermissions != null) {
				permissionSet.addAll(allowedPermissions);
			}

			if (deniedPermissions != null) {
				permissionSet.removeAll(deniedPermissions);
			}
		}

		return permissionSet.contains(permission.name());
		*/
	}

	public void setAllowed(final Set<String> allowed) {
		SecurityDelegate.setAllowed(this, Security.allowed, allowed);
	}

	public void setAllowed(final Permission... allowed) {
		SecurityDelegate.setAllowed(this, Security.allowed, allowed);
	}

	public Set<String> getPermissions() {
		return SecurityDelegate.getPermissions(this, Security.allowed);
	}

	public void addPermission(final Permission permission) {
		SecurityDelegate.addPermission(this, Security.allowed, permission);
	}

	public void removePermission(final Permission permission) {
		SecurityDelegate.removePermission(this, Security.allowed, permission);
	}

	// ----- class Relation -----
	@Override
	public Class<Principal> getSourceType() {
		return Principal.class;
	}

	@Override
	public String name() {
		return "SECURITY";
	}

	@Override
	public Class<NodeInterface> getTargetType() {
		return NodeInterface.class;
	}

	@Override
	public Property<String> getSourceIdProperty() {
		return principalId;
	}

	@Override
	public Property<String> getTargetIdProperty() {
		return accessControllableId;
	}

	@Override
	public boolean isInternal() {
		return true;
	}
}
