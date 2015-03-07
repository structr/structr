/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.neo4j.graphdb.Relationship;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.SourceId;
import org.structr.core.property.TargetId;

//~--- classes ----------------------------------------------------------------

/**
 * A generic relationship entity that will be instantiated when an anonymous
 * relationship is encountered.
 *
 * @author Axel Morgner
 *
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

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return true;
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return true;
	}

	@Override
	public boolean onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
		return true;
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		return true;
	}

	public boolean isAllowed(final Permission permission) {

		if (dbRelationship.hasProperty(allowed.dbName())) {

			String[] allowedProperties = (String[]) dbRelationship.getProperty(allowed.dbName());

			if (allowedProperties != null) {

				for (String p : allowedProperties) {

					if (p.equals(permission.name())) {

						return true;
					}

				}

			}

		}

		return false;

	}

	public void setAllowed(final Set<String> allowed) {

		String[] allowedActions = (String[]) allowed.toArray(new String[allowed.size()]);

		setAllowed(allowedActions);

	}

	public void setAllowed(final Permission[] allowed) {

		Set<String> allowedActions = new HashSet<>();

		for (Permission permission : allowed) {

			allowedActions.add(permission.name());
		}

		setAllowed(allowedActions);

	}

	public void setAllowed(final String[] allowed) {

		dbRelationship.setProperty(Security.allowed.dbName(), allowed);

	}

	public String[] getPermissions() {

		if (dbRelationship.hasProperty(Security.allowed.dbName())) {

			// StringBuilder result             = new StringBuilder();
			String[] allowedProperties = (String[]) dbRelationship.getProperty(Security.allowed.dbName());

			return allowedProperties;

//                      if (allowedProperties != null) {
//
//                              for (String p : allowedProperties) {
//
//                                      result.append(p).append("\n");
//
//                              }
//
//                      }
//
//                      return result.toString();
		} else {

			return null;
		}

	}

	public void addPermission(final Permission permission) {

		String[] _allowed = getPermissions();

		if (ArrayUtils.contains(_allowed, permission.name())) {

			return;
		}

		setAllowed((String[]) ArrayUtils.add(_allowed, permission.name()));

	}

	public void removePermission(final Permission permission) {

		String[] _allowed = getPermissions();

		if (!ArrayUtils.contains(_allowed, permission.name())) {

			return;
		}

		String[] newPermissions = (String[]) ArrayUtils.removeElement(_allowed, permission.name());

		if (newPermissions.length > 0) {

			setAllowed(newPermissions);

		} else {

			final App app = StructrApp.getInstance(securityContext);
			app.delete(this);
		}

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
}
