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
package org.structr.core.graph;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.graph.RelationshipType;
import org.structr.common.AccessPathCache;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.TypeProperty;

import java.util.*;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;

/**
 *
 *
 */
public class GraphObjectModificationState implements ModificationEvent {

	private static final Logger LOGGER                          = LoggerFactory.getLogger(GraphObjectModificationState.class);
	private static final Set<String> hiddenPropertiesInAuditLog = new HashSet<>(Arrays.asList(new String[] {
			GraphObjectTraitDefinition.ID_PROPERTY, PrincipalTraitDefinition.SESSION_IDS_PROPERTY, "localStorage", PrincipalTraitDefinition.SALT_PROPERTY, PrincipalTraitDefinition.PASSWORD_PROPERTY, PrincipalTraitDefinition.TWO_FACTOR_SECRET_PROPERTY
	}));

	public static final int STATE_DELETED =                    1;
	public static final int STATE_MODIFIED =                   2;
	public static final int STATE_CREATED =                    4;
	public static final int STATE_DELETED_PASSIVELY =          8;
	public static final int STATE_OWNER_MODIFIED =            16;
	public static final int STATE_SECURITY_MODIFIED =         32;

	private final long timestamp                              = System.nanoTime();
	private final Map<String, Object> addedRemoteProperties   = new HashMap<>();
	private final Map<String, Object> removedRemoteProperties = new HashMap<>();
	private final PropertyMap modifiedProperties              = new PropertyMap();
	private final PropertyMap removedProperties               = new PropertyMap();
	private final PropertyMap newProperties                   = new PropertyMap();
	private StringBuilder changeLog                           = null;
	private Map<String, StringBuilder> userChangeLogs         = null;
	private RelationshipType relType                          = null;
	private boolean isNode                                    = false;
	private boolean modified                                  = false;
	private GraphObject object                                = null;
	private String type                                       = null;
	private String uuid                                       = null;
	private int status                                        = 0;
	private String callbackId                                 = null;

	private long validationTime = 0;
	private long indexingTime = 0;

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

	public enum Direction {
		in, out
	}

	public GraphObjectModificationState(GraphObject object) {

		this.object = object;
		this.isNode = (object instanceof NodeInterface);

		if (!isNode) {
			this.relType = ((RelationshipInterface)object).getRelType();
		}

		// store UUID and type for later use
		this.uuid     = object.getUuid();
		this.type = object.getType();

		if (Settings.ChangelogEnabled.getValue()) {

			// create on demand
			changeLog = new StringBuilder();
		}

		if (Settings.UserChangelogEnabled.getValue()) {

			// create on demand
			userChangeLogs = new HashMap<>();
		}
	}

	@Override
	public String toString() {
		return object.getClass().getSimpleName() + "(" + object + "); " + status;
	}

	@Override
	public String getChangeLog() {

		if (changeLog != null) {
			return changeLog.toString();
		}

		return null;
	}

	@Override
	public Map getUserChangeLogs() {

		return userChangeLogs;

	}

	public void modifySecurity() {

		int statusBefore = status;

		status |= STATE_SECURITY_MODIFIED;

		if (status != statusBefore) {
			modified = true;
		}
	}

	public void modifyOwner() {

		int statusBefore = status;

		status |= STATE_OWNER_MODIFIED;

		if (status != statusBefore) {
			modified = true;
		}
	}

	public void create() {

		int statusBefore = status;

		status |= STATE_CREATED;

		if (status != statusBefore) {
			modified = true;
		}

		updateCache();
	}

	public <T> void modify(final Principal user, final PropertyKey<T> key, final T previousValue, final T newValue) {

		int statusBefore = status;

		status |= STATE_MODIFIED;

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

	public void add(final PropertyKey key, final Object value) {
		addToCollection(addedRemoteProperties, key, value);
	}

	public void remove(final PropertyKey key, final Object value) {
		addToCollection(removedRemoteProperties, key, value);
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
	public boolean doInnerCallback(final ModificationQueue modificationQueue, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final CallbackCounter counter) throws FrameworkException {

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
				counter.onCreate();
				object.onCreation(securityContext, errorBuffer);
				break;

			case 5: // created, deleted => no callback
				break;

			case 4: // created => creation callback
				counter.onCreate();
				object.onCreation(securityContext, errorBuffer);
				break;

			case 3: // modified, deleted => deletion callback
				counter.onDelete();
				object.onDeletion(securityContext, errorBuffer, removedProperties);
				break;

			case 2: // modified => modification callback
				counter.onSave();
				object.onModification(securityContext, errorBuffer, modificationQueue);
				break;

			case 1: // deleted => deletion callback
				counter.onDelete();
				object.onDeletion(securityContext, errorBuffer, removedProperties);
				break;

			case 0:	// no action, no callback
				break;

			default:
				break;
		}

		// mark as finished
		modified = false;

		return !errorBuffer.hasError();
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
	public boolean doValidationAndIndexing(final ModificationQueue modificationQueue, final SecurityContext securityContext, final ErrorBuffer errorBuffer, boolean doValidation, final CallbackCounter counter) throws FrameworkException {

		boolean valid = true;

		// examine only the last 4 bits here
		switch (status & 0x000f) {

			case 6: // created, modified => only creation callback will be called
			case 4: // created => creation callback
			case 2: // modified => modification callback

				long t0 = System.currentTimeMillis();

				if (doValidation) {
					valid &= object.isValid(errorBuffer);
				}

				long t1 = System.currentTimeMillis();
				validationTime += t1 - t0;

				counter.indexing();
				object.indexPassiveProperties();

				long t2 = System.currentTimeMillis() - t1;
				indexingTime += t2;

				break;

			default:
				break;
		}

		return valid;
	}

	public long getValdationTime() {
		return validationTime;
	}

	public long getIndexingTime() {
		return indexingTime;
	}

	/**
	 * Call afterModification/Creation/Deletion methods.
	 *
	 * @param securityContext
	 */
	public void doOuterCallback(final SecurityContext securityContext, final CallbackCounter counter) throws FrameworkException {

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
				counter.afterCreate();
				object.afterCreation(securityContext);
				break;

			case  5: // created, deleted => no callback
				break;

			case  4: // created => creation callback
				counter.afterCreate();
				object.afterCreation(securityContext);
				break;

			case  3: // modified, deleted => deletion callback
				counter.afterDelete();
				object.afterDeletion(securityContext, removedProperties);
				break;

			case  2: // modified => modification callback
				counter.afterSave();
				object.afterModification(securityContext);
				break;

			case  1: // deleted => deletion callback
				counter.afterDelete();
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

	// Update changelog for Verb.change
	public void updateChangeLog(final Principal user, final Verb verb, final PropertyKey key, final Object previousValue, final Object newValue) {

		if ((Settings.ChangelogEnabled.getValue() || Settings.UserChangelogEnabled.getValue()) && key != null) {

			final String name = key.jsonName();

			if (!hiddenPropertiesInAuditLog.contains(name) && !(key.isUnvalidated() || key.isReadOnly()) || key instanceof TypeProperty) {

				final JsonObject obj = new JsonObject();

				obj.add("time",     toElement(System.currentTimeMillis()));
				obj.add("userId",   toElement((user == null) ? Principal.ANONYMOUS : user.getUuid()));
				obj.add("userName", toElement((user == null) ? Principal.ANONYMOUS : user.getName()));
				obj.add("verb",     toElement(verb));
				obj.add("key",      toElement(key.jsonName()));
				obj.add("prev",     toElement(previousValue));
				obj.add("val",      toElement(newValue));

				if (Settings.ChangelogEnabled.getValue()) {
					changeLog.append(obj.toString());
					changeLog.append("\n");
				}

				if (Settings.UserChangelogEnabled.getValue() && user != null) {

					// remove user for user-centric logging to reduce redundancy
					obj.remove("userId");
					obj.remove("userName");

					// add target to identify change event
					obj.add("target", toElement(getUuid()));

					appendUserChangelog(user.getUuid(), obj.toString());
				}
			}
		}
	}

	// Update *node* changelog for Verb.link
	public void updateChangeLog(final Principal user, final Verb verb, final String linkType, final String linkId, final String object, final Direction direction) {

		if (Settings.ChangelogEnabled.getValue()) {

			final JsonObject obj = new JsonObject();

			obj.add("time",     toElement(System.currentTimeMillis()));
			obj.add("userId",   toElement((user == null) ? Principal.ANONYMOUS : user.getUuid()));
			obj.add("userName", toElement((user == null) ? Principal.ANONYMOUS : user.getName()));
			obj.add("verb",     toElement(verb));
			obj.add("rel",      toElement(linkType));
			obj.add("relId",    toElement(linkId));
			obj.add("relDir",   toElement(direction));
			obj.add("target",   toElement(object));

			changeLog.append(obj.toString());
			changeLog.append("\n");
		}
	}

	// Update *relationship* changelog for Verb.create
	public void updateChangeLog(final Principal user, final Verb verb, final String linkType, final String linkId, final String sourceUuid, final String targetUuid) {

		if ((Settings.ChangelogEnabled.getValue() || Settings.UserChangelogEnabled.getValue())) {

			final JsonObject obj = new JsonObject();

			obj.add("time",     toElement(System.currentTimeMillis()));
			obj.add("userId",   toElement((user == null) ? Principal.ANONYMOUS : user.getUuid()));
			obj.add("userName", toElement((user == null) ? Principal.ANONYMOUS : user.getName()));
			obj.add("verb",     toElement(verb));
			obj.add("rel",      toElement(linkType));
			obj.add("relId",    toElement(linkId));
			obj.add("source",   toElement(sourceUuid));
			obj.add("target",   toElement(targetUuid));

			if (Settings.ChangelogEnabled.getValue()) {
				changeLog.append(obj.toString());
				changeLog.append("\n");
			}

			if (Settings.UserChangelogEnabled.getValue() && user != null) {

				// remove user for user-centric logging to reduce redundancy
				obj.remove("userId");
				obj.remove("userName");

				appendUserChangelog(user.getUuid(), obj.toString());
			}
		}
	}

	// Update changelog for Verb.create and Verb.delete
	public void updateChangeLog(final Principal user, final Verb verb, final NodeInterface node) {

		if ((Settings.ChangelogEnabled.getValue() || Settings.UserChangelogEnabled.getValue())) {

			final JsonObject obj = new JsonObject();

			obj.add("time",     toElement(System.currentTimeMillis()));
			obj.add("userId",   toElement((user == null) ? Principal.ANONYMOUS : user.getUuid()));
			obj.add("userName", toElement((user == null) ? Principal.ANONYMOUS : user.getName()));
			obj.add("verb",     toElement(verb));
			obj.add("target",   toElement(object));
			obj.add("type",     toElement(node.getType()));

			if (Settings.ChangelogEnabled.getValue()) {

				if (changeLog.length() > 0 && verb.equals(Verb.create)) {
					// ensure that node creation appears first in the log
					changeLog.insert(0, "\n");
					changeLog.insert(0, obj.toString());
				} else {
					changeLog.append(obj.toString());
					changeLog.append("\n");
				}
			}

			if (Settings.UserChangelogEnabled.getValue() && user != null) {

				// remove user for user-centric logging to reduce redundancy
				obj.remove("userId");
				obj.remove("userName");

				appendUserChangelog(user.getUuid(), obj.toString());
			}
		}
	}

	public long getTimestamp() {
		return timestamp;
	}

	// ----- private methods -----
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

	private StringBuilder getUserChangelogForUserId(final String uuid) {

		if (Settings.UserChangelogEnabled.getValue()) {

			if (!userChangeLogs.containsKey(uuid)) {
				userChangeLogs.put(uuid, new StringBuilder());
			}

			return userChangeLogs.get(uuid);
		}

		return null;
	}

	private void appendUserChangelog(final String userUUID, final String changelog) {

		if (Settings.UserChangelogEnabled.getValue()) {
			// write user-centric changelog
			getUserChangelogForUserId(userUUID).append(changelog).append("\n");
		}
	}

	private void updateCache() {

		if (uuid != null) {
			AccessPathCache.invalidateForId(uuid);
		}

		if (relType != null) {
			AccessPathCache.invalidateForRelType(relType.name());
		}
	}

	private void addToCollection(final Map<String, Object> properties, final PropertyKey key, final Object value) {

		if (key.isCollection()) {

			List list = (List)properties.get(key.jsonName());
			if (list == null) {

				list = new LinkedList<>();
				properties.put(key.jsonName(), list);
			}

			list.add(unwrap(value));

		} else {

			properties.put(key.jsonName(), unwrap(value));
		}
	}

	private Object unwrap(final Object src) {

		if (src instanceof GraphObject) {
			return ((GraphObject)src).getUuid();
		}

		return src;
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

	public Map<String, Object> getRemovedRemoteProperties() {
		return removedRemoteProperties;
	}

	public Map<String, Object> getAddedRemoteProperties() {
		return addedRemoteProperties;
	}

	@Override
	public Map<String, Object> getData(final SecurityContext securityContext) throws FrameworkException {
		return PropertyMap.javaTypeToInputType(securityContext, object.getType(), modifiedProperties);
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
