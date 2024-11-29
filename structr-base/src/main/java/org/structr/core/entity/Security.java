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

import org.structr.common.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;

import java.util.Set;

/**
 * Relationship type class for SECURITY relationships.
 */
public class Security extends ManyToMany<Principal, NodeInterface> {

	public static final Property<String[]> allowed              = new ArrayProperty("allowed", String.class);

	/*
	public static final SourceId           principalId          = new SourceId("principalId");
	public static final TargetId           accessControllableId = new TargetId("accessControllableId");

	public static final View uiView = new View(Security.class, PropertyView.Ui,
		allowed
	);


	@Override
	public Set<PropertyKey> getPropertyKeys(String propertyView) {

		Set<PropertyKey> keys = new LinkedHashSet<>();

		keys.addAll((Set<PropertyKey>) super.getPropertyKeys(propertyView));

		keys.add(principalId);
		keys.add(accessControllableId);

		final Relationship dbRelationship = getRelationship();
		if (dbRelationship != null) {

			for (String key : dbRelationship.getPropertyKeys()) {
				keys.add(StructrApp.getConfiguration().getPropertyKeyForDatabaseName(entityType, key));
			}
		}

		return keys;
	}
	*/

	public boolean isAllowed(final Permission permission) {
		return SecurityDelegate.isAllowed(this, Security.allowed, permission);
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

	public void addPermissions(final Set<Permission> permissions) {
		SecurityDelegate.addPermissions(this, Security.allowed, permissions);
	}

	public void removePermission(final Permission permission) {
		SecurityDelegate.removePermission(this, Security.allowed, permission);
	}

	public void removePermissions(final Set<Permission> permissions) {
		SecurityDelegate.removePermissions(this, Security.allowed, permissions);
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
		//return principalId;
		return null;
	}

	@Override
	public Property<String> getTargetIdProperty() {
		//return accessControllableId;
		return null;
	}

	@Override
	public void setSourceProperty(PropertyKey source) {
	}

	@Override
	public void setTargetProperty(PropertyKey target) {

	}

	@Override
	public PropertyKey getSourceProperty() {
		return null;
	}

	@Override
	public PropertyKey getTargetProperty() {
		return null;
	}
}
