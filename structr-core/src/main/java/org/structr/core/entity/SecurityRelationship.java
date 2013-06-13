/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.core.entity;


//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.ArrayUtils;
import org.neo4j.graphdb.Relationship;
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.StringProperty;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.graph.DeleteRelationshipCommand;
import org.structr.core.property.ArrayProperty;

//~--- classes ----------------------------------------------------------------

/**
 * A generic relationship entity that will be instantiated when an anonymous
 * relationship is encountered.
 * 
 * @author Axel Morgner
 *
 */
public class SecurityRelationship extends AbstractRelationship {

	private static final Logger logger = Logger.getLogger(SecurityRelationship.class.getName());
	
	public static final Property<String> principalId          = new StringProperty("principalId");
	public static final Property<String> accessControllableId = new StringProperty("accessControllableId");
	public static final Property<String[]> allowed            = new ArrayProperty("allowed", String.class);

	public static final View uiView = new View(SecurityRelationship.class, PropertyView.Ui,
		allowed
	);
	
	static {

		//EntityContext.registerSearchableProperty(SecurityRelationship.class, RelationshipIndex.rel_uuid.name(), AbstractRelationship.uuid);
		EntityContext.registerNamedRelation("security", SecurityRelationship.class, Principal.class, AccessControllable.class, RelType.SECURITY);
	}
	
	//~--- constructors ---------------------------------------------------

	public SecurityRelationship() {}

	public SecurityRelationship(SecurityContext securityContext, Relationship dbRelationship) {
		init(securityContext, dbRelationship);
	}

	@Override
	public PropertyKey getStartNodeIdKey() {
		return principalId;
	}

	@Override
	public PropertyKey getEndNodeIdKey() {
		return accessControllableId;
	}
		
	@Override
	public Iterable<PropertyKey> getPropertyKeys(String propertyView) {
		
		Set<PropertyKey> keys = new LinkedHashSet<PropertyKey>();
		
		keys.add(principalId);
		keys.add(accessControllableId);
		
		if (dbRelationship != null) {
			
			for (String key : dbRelationship.getPropertyKeys()) {
				keys.add(EntityContext.getPropertyKeyForDatabaseName(entityType, key));
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

	public void setAllowed(final List<String> allowed) {

		String[] allowedActions = (String[]) allowed.toArray(new String[allowed.size()]);

		setAllowed(allowedActions);

	}

	public void setAllowed(final Permission[] allowed) {

		List<String> allowedActions = new ArrayList<String>();

		for (Permission permission : allowed) {

			allowedActions.add(permission.name());
		}

		setAllowed(allowedActions);

	}

	public void setAllowed(final String[] allowed) {

		dbRelationship.setProperty(SecurityRelationship.allowed.dbName(), allowed);

	}
	
	public String[] getPermissions() {

		if (dbRelationship.hasProperty(SecurityRelationship.allowed.dbName())) {

			// StringBuilder result             = new StringBuilder();
			String[] allowedProperties = (String[]) dbRelationship.getProperty(SecurityRelationship.allowed.dbName());

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
			try {
				
				// No permissions anymore, remove security relationship
				Services.command(securityContext, DeleteRelationshipCommand.class).execute(this);
				
				
			} catch (FrameworkException ex) {
				logger.log(Level.SEVERE, "Could not remove security relationship!", ex);
			}
			
		}

	}
		
}
