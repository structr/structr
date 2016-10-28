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
package org.structr.core.graph;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.structr.api.graph.RelationshipType;
import org.structr.common.AccessPathCache;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 *
 *
 */
public class GraphObjectModificationState implements ModificationEvent {

	private static final Set<String> hiddenPropertiesInAuditLog = new HashSet<>(Arrays.asList(new String[] { "id", "type", "sessionIds", "localStorage", "salt", "password" } ));

	public static final int STATE_DELETED =                    1;
	public static final int STATE_MODIFIED =                   2;
	public static final int STATE_CREATED =                    4;
	public static final int STATE_DELETED_PASSIVELY =          8;
	public static final int STATE_OWNER_MODIFIED =            16;
	public static final int STATE_SECURITY_MODIFIED =         32;
	public static final int STATE_LOCATION_MODIFIED =         64;
	public static final int STATE_PROPAGATING_MODIFICATION = 128;
	public static final int STATE_PROPAGATED_MODIFICATION =  256;

	private final boolean changeLogEnabled       = "true".equals(StructrApp.getConfigurationValue(Services.APPLICATION_CHANGELOG_ENABLED, "false"));
	private final PropertyMap modifiedProperties = new PropertyMap();
	private final PropertyMap removedProperties  = new PropertyMap();
	private final PropertyMap newProperties      = new PropertyMap();
	private StringBuilder changeLog              = null;
	private RelationshipType relType             = null;
	private boolean isNode                       = false;
	private boolean modified                     = false;
	private GraphObject object                   = null;
	private String uuid                          = null;
	private int status                           = 0;
	private String callbackId                    = null;

	@Override
	public String getCallbackId() {
		return this.callbackId;
	}

	public void setCallbackId(final String callbackId) {
		this.callbackId = callbackId;
	}

	public enum Verb {
		create, change, delete, link, unlink
	}

	public GraphObjectModificationState(GraphObject object) {

		this.object = object;
		this.isNode = (object instanceof NodeInterface);

		if (!isNode) {
			this.relType = ((RelationshipInterface)object).getRelType();
		}

		// store uuid for later use
		this.uuid = object.getUuid();

		if (changeLogEnabled) {

			// create on demand
			changeLog = new StringBuilder();
		}
	}

	@Override
	public String toString() {
		return object.getClass().getSimpleName() + "(" + object + "); " + status;
	}

	@Override
	public String getChangeLog() {
		return changeLog.toString();
	}

	public void propagatedModification() {

		int statusBefore = status;

		status |= STATE_PROPAGATED_MODIFICATION;

		if (status != statusBefore) {
			modified = true;
		}
	}

	public void modifyLocation() {

		int statusBefore = status;

		status |= STATE_LOCATION_MODIFIED | STATE_PROPAGATING_MODIFICATION;

		if (status != statusBefore) {
			modified = true;
		}
	}

	public void modifySecurity() {

		int statusBefore = status;

		status |= STATE_SECURITY_MODIFIED | STATE_PROPAGATING_MODIFICATION;

		if (status != statusBefore) {
			modified = true;
		}
	}

	public void modifyOwner() {

		int statusBefore = status;

		status |= STATE_OWNER_MODIFIED | STATE_PROPAGATING_MODIFICATION;

		if (status != statusBefore) {
			modified = true;
		}
	}

	public void create() {

		int statusBefore = status;

		status |= STATE_CREATED | STATE_PROPAGATING_MODIFICATION;

		if (status != statusBefore) {
			modified = true;
		}

		updateCache();
	}

	public void modify(final Principal user, final PropertyKey key, final Object previousValue, final Object newValue) {

		int statusBefore = status;

		status |= STATE_MODIFIED | STATE_PROPAGATING_MODIFICATION;

		// store previous value
		if (key != null) {
			removedProperties.put(key, previousValue);
		}

		if (status != statusBefore) {

			if (key != null) {

				modifiedProperties.put(key, newValue);
				updateChangeLog(user, Verb.change, key, previousValue, newValue);
			}

			modified = true;

		} else {

			if (key != null) {
				newProperties.put(key, newValue);
				updateChangeLog(user, Verb.change, key, previousValue, newValue);
			}
		}

		// only update cache if key, prev and new values are null
		// because that's when a relationship has been created / removed
		if (key == null && previousValue == null && newValue == null) {
			updateCache();
		}
	}

	public void delete(boolean passive) {

		int statusBefore = status;

		if (passive) {
			status |= STATE_DELETED_PASSIVELY;
		}

		status |= STATE_DELETED;

		if (status != statusBefore) {

			// copy all properties on deletion
			for (final PropertyKey key : object.getPropertyKeys(PropertyView.Public)) {
				removedProperties.put(key, object.getProperty(key));
			}

			modified = true;
		}

		updateCache();
	}

	private void updateCache() {

		if (uuid != null) {
			AccessPathCache.invalidateForId(uuid);
		}

		if (relType != null) {
			AccessPathCache.invalidateForRelType(relType.name());
		}
	}

	public boolean isPassivelyDeleted() {
		return (status & STATE_DELETED_PASSIVELY) == STATE_DELETED_PASSIVELY;
	}

	/**
	 * Call beforeModification/Creation/Deletion methods.
	 *
	 * @param modificationQueue
	 * @param securityContext
	 * @param errorBuffer
	 * @return valid
	 * @throws FrameworkException
	 */
	public boolean doInnerCallback(ModificationQueue modificationQueue, SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		boolean valid = true;

		// check for modification propagation along the relationships
		if ((status & STATE_PROPAGATING_MODIFICATION) == STATE_PROPAGATING_MODIFICATION && object instanceof AbstractNode) {

			Set<AbstractNode> nodes = ((AbstractNode)object).getNodesForModificationPropagation();
			if (nodes != null) {

				for (AbstractNode node : nodes) {

					modificationQueue.propagatedModification(node);
				}
			}

		}

		// examine only the last 4 bits here
		switch (status & 0x000f) {

			case 15:
			case 14:
			case 13:
			case 12:
			case 11:
			case 10:
			case  9:
			case  8: // since all values >= 8 mean that the object was passively deleted, no action has to be taken
				 // (no callback for passive deletion!)
				break;

			case 7:	// created, modified, deleted, poor guy => no callback
				break;

			case 6: // created, modified => only creation callback will be called
				valid &= object.onCreation(securityContext, errorBuffer);
				break;

			case 5: // created, deleted => no callback
				break;

			case 4: // created => creation callback
				valid &= object.onCreation(securityContext, errorBuffer);
				break;

			case 3: // modified, deleted => deletion callback
				valid &= object.onDeletion(securityContext, errorBuffer, removedProperties);
				break;

			case 2: // modified => modification callback
				valid &= object.onModification(securityContext, errorBuffer, modificationQueue);
				break;

			case 1: // deleted => deletion callback
				valid &= object.onDeletion(securityContext, errorBuffer, removedProperties);
				break;

			case 0:	// no action, no callback
				break;

			default:
				break;
		}

		// mark as finished
		modified = false;

		return valid;
	}

	/**
	 * Call beforeModification/Creation/Deletion methods.
	 *
	 * @param modificationQueue
	 * @param securityContext
	 * @param errorBuffer
	 * @param doValidation
	 * @return valid
	 * @throws FrameworkException
	 */
	public boolean doValidationAndIndexing(ModificationQueue modificationQueue, SecurityContext securityContext, ErrorBuffer errorBuffer, boolean doValidation) throws FrameworkException {

		boolean valid = true;

		// examine only the last 4 bits here
		switch (status & 0x000f) {

			case 6: // created, modified => only creation callback will be called
			case 4: // created => creation callback
			case 2: // modified => modification callback
				if (doValidation) {
					valid &= object.isValid(errorBuffer);
				}
				object.indexPassiveProperties();
				break;

			case 1: // deleted => deletion callback
				object.removeFromIndex();
				break;

			default:
				break;
		}

		return valid;
	}

	/**
	 * Call afterModification/Creation/Deletion methods.
	 *
	 * @param securityContext
	 */
	public void doOuterCallback(SecurityContext securityContext) {

		if ((status & (STATE_DELETED | STATE_DELETED_PASSIVELY)) == 0) {

			if ((status & STATE_PROPAGATED_MODIFICATION) == STATE_PROPAGATED_MODIFICATION) {
				object.propagatedModification(securityContext);
			}

			if ((status & STATE_LOCATION_MODIFIED) == STATE_LOCATION_MODIFIED) {
				object.locationModified(securityContext);
			}

			if ((status & STATE_SECURITY_MODIFIED) == STATE_SECURITY_MODIFIED) {
				object.securityModified(securityContext);
			}

			if ((status & STATE_OWNER_MODIFIED) == STATE_OWNER_MODIFIED) {
				object.ownerModified(securityContext);
			}
		}

		// examine only the last 4 bits here
		switch (status & 0x000f) {

			case 15:
			case 14:
			case 13:
			case 12:
			case 11:
			case 10:
			case  9:
			case  8: // since all values >= 8 mean that the object was passively deleted, no action has to be taken
				 // (no callback for passive deletion!)
				break;

			case  7: // created, modified, deleted, poor guy => no callback
				break;

			case  6: // created, modified => only creation callback will be called
				object.afterCreation(securityContext);
				break;

			case  5: // created, deleted => no callback
				break;

			case  4: // created => creation callback
				object.afterCreation(securityContext);
				break;

			case  3: // modified, deleted => deletion callback
				object.afterDeletion(securityContext, removedProperties);
				break;

			case  2: // modified => modification callback
				object.afterModification(securityContext);
				break;

			case  1: // deleted => deletion callback
				object.afterDeletion(securityContext, removedProperties);
				break;

			case  0: // no action, no callback
				break;

			default:
				break;
		}
	}

	public boolean wasModified() {
		return modified;
	}

	public void updateChangeLog(final Principal user, final Verb verb, final PropertyKey key, final Object previousValue, final Object newValue) {

		if (changeLogEnabled && changeLog != null && key != null) {

			final String name = key.jsonName();

			if (!hiddenPropertiesInAuditLog.contains(name) && !(key.isUnvalidated() || key.isReadOnly())) {

				final JsonObject obj = new JsonObject();

				obj.add("time", toElement(System.currentTimeMillis()));
				obj.add("userId", toElement(user.getUuid()));
				obj.add("userName", toElement(user.getName()));
				obj.add("verb", toElement(verb));
				obj.add("key", toElement(key.jsonName()));
				obj.add("prev", toElement(previousValue));
				obj.add("val", toElement(newValue));

				changeLog.append(obj.toString());
				changeLog.append("\n");
			}
		}
	}

	public void updateChangeLog(final Principal user, final Verb verb, final String linkType, final String object) {

		if (changeLogEnabled && changeLog != null) {

			final JsonObject obj = new JsonObject();

			obj.add("time", toElement(System.currentTimeMillis()));
			obj.add("userId", toElement(user.getUuid()));
			obj.add("userName", toElement(user.getName()));
			obj.add("verb", toElement(verb));
			obj.add("rel", toElement(linkType));
			obj.add("target", toElement(object));

			changeLog.append(obj.toString());
			changeLog.append("\n");
		}
	}

	public void updateChangeLog(final Principal user, final Verb verb, final String object) {

		if (changeLogEnabled && changeLog != null) {

			final JsonObject obj = new JsonObject();

			obj.add("time", toElement(System.currentTimeMillis()));
			obj.add("userId", toElement(user.getUuid()));
			obj.add("userName", toElement(user.getName()));
			obj.add("verb", toElement(verb));
			obj.add("target", toElement(object));

			changeLog.append(obj.toString());
			changeLog.append("\n");
		}
	}

	private JsonElement toElement(final Object value) {

		if (value != null) {

			if (value instanceof String) {

				return new JsonPrimitive((String)value);

			} else if (value instanceof Number) {

				return new JsonPrimitive((Number)value);

			} else if (value instanceof Boolean) {

				return new JsonPrimitive((Boolean)value);

			} else if (value.getClass().isArray()) {

				final JsonArray arr   = new JsonArray();
				final Object[] values = (Object[])value;

				for (final Object v : values) {
					arr.add(toElement(v));
				}

				return arr;

			} else {

				return new JsonPrimitive(value.toString());
			}
		}

		return JsonNull.INSTANCE;
	}

	// ----- interface ModificationEvent -----

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public boolean isCreated() {
		return (status & STATE_CREATED) == STATE_CREATED;
	}

	@Override
	public boolean isModified() {
		return (status & STATE_MODIFIED) == STATE_MODIFIED;
	}

	@Override
	public boolean isDeleted() {
		return (status & STATE_DELETED) == STATE_DELETED;
	}

	@Override
	public GraphObject getGraphObject() {
		return object;
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public PropertyMap getNewProperties() {
		return newProperties;
	}

	@Override
	public PropertyMap getModifiedProperties() {
		return modifiedProperties;
	}

	@Override
	public PropertyMap getRemovedProperties() {
		return removedProperties;
	}

	@Override
	public Map<String, Object> getData(final SecurityContext securityContext) throws FrameworkException {
		return PropertyMap.javaTypeToInputType(securityContext, object.getClass(), modifiedProperties);
	}

	@Override
	public boolean isNode() {
		return isNode;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return relType;
	}
}
