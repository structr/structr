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
package org.structr.core;

import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.*;
import java.util.Map.Entry;

/**
 * A dummy graph object that uses a map as its data store.
 *
 *
 */
public class GraphObjectMap extends PropertyMap implements GraphObject {

	@Override
	public String getUuid() {
		return getProperty(Traits.idProperty());
	}

	@Override
	public void clearCaches() {

	}

	@Override
	public Traits getTraits() {
		return null;
	}

	@Override
	public <T> T as(Class<T> type) {
		return null;
	}

	@Override
	public boolean is(String type) {
		return false;
	}

	@Override
	public String getType() {
		return getProperty(Traits.idProperty());
	}

	@Override
	public Set<PropertyKey> getPropertyKeys(final String propertyView) {
		return properties.keySet();
	}

	@Override
	public <T> T getProperty(final PropertyKey<T> propertyKey, Predicate<GraphObject> filter) {
		return (T)properties.get(propertyKey);
	}

	@Override
	public Object setProperty(final PropertyKey key, final Object value) throws FrameworkException {
		return setProperty(key, value, false);
	}

	@Override
	public Object setProperty(final PropertyKey key, final Object value, final boolean isCreation) throws FrameworkException {
		properties.put(key, value);
		return null;
	}

	@Override
	public <T> T getProperty(final PropertyKey<T> propertyKey) {
		return (T)properties.get(propertyKey);
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {
		setProperties(securityContext, properties, false);
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {
		properties.putAll(properties);
	}

	@Override
	public boolean isGranted(Permission permission, SecurityContext securityContext) {
		return false;
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		return false;
	}

	@Override
	public void removeProperty(final PropertyKey key) throws FrameworkException {
		properties.remove(key);
	}

	@Override
	public boolean systemPropertiesUnlocked() {
		return false;
	}

	@Override
	public void unlockReadOnlyPropertiesOnce() {
	}

	@Override
	public void lockReadOnlyProperties() {

	}

	@Override
	public void unlockSystemPropertiesOnce() {
	}

	@Override
	public void lockSystemProperties() {

	}

	@Override
	public boolean readOnlyPropertiesUnlocked() {
		return false;
	}

	public static GraphObjectMap fromMap(final Map<String, Object> map) {

		final GraphObjectMap newGraphObjectMap = new GraphObjectMap();

		for (final Map.Entry<String, Object> prop : map.entrySet()) {

			newGraphObjectMap.put(new GenericProperty(prop.getKey()), prop.getValue());
		}
		return newGraphObjectMap;
	}

	public Map<String, Object> toMap() {

		final Map<String, Object> newMap = new LinkedHashMap<>();

		for (final Entry<PropertyKey, Object> entry : properties.entrySet()) {

			final PropertyKey key = entry.getKey();
			final Object value    = entry.getValue();

			if (value instanceof GraphObjectMap) {

				newMap.put(key.jsonName(), ((GraphObjectMap)value).toMap());

			} else {

				newMap.put(key.jsonName(), value);
			}
		}

		return newMap;
	}

	// ----- interface Map -----
	@Override
	public int size() {
		return properties.size();
	}

	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public Object put(final PropertyKey key, final Object value) {
		return properties.put(key, value);
	}

	@Override
	public void clear() {
		properties.clear();
	}

	@Override
	public Set keySet() {
		return properties.keySet();
	}

	@Override
	public Collection values() {
		return properties.values();
	}

	@Override
	public Set entrySet() {
		return properties.entrySet();
	}

	@Override
	public PropertyContainer getPropertyContainer() {
		return null;
	}

	@Override
	public Set<PropertyKey> getFullPropertySet() {
		return properties.keySet();
	}

	@Override
	public void addToIndex() {
	}

	@Override
	public boolean isVisibleToPublicUsers() {
		return false;
	}

	@Override
	public boolean isVisibleToAuthenticatedUsers() {
		return false;
	}

	@Override
	public boolean isHidden() {
		return false;
	}

	@Override
	public Date getCreatedDate() {
		return null;
	}

	@Override
	public Date getLastModifiedDate() {
		return null;
	}

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, ModificationQueue modificationQueue) throws FrameworkException {
	}

	@Override
	public void onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
	}

	@Override
	public void afterCreation(SecurityContext securityContext) throws FrameworkException {
	}

	@Override
	public void afterModification(SecurityContext securityContext) throws FrameworkException {
	}

	@Override
	public void afterDeletion(SecurityContext securityContext, PropertyMap properties) {
	}

	@Override
	public void ownerModified(SecurityContext securityContext) {
	}

	@Override
	public void securityModified(SecurityContext securityContext) {
	}

	@Override
	public void locationModified(SecurityContext securityContext) {
	}

	@Override
	public void propagatedModification(SecurityContext securityContext) {
	}

	@Override
	public void indexPassiveProperties() {
	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
	}

	@Override
	public SecurityContext getSecurityContext() {
		return null;
	}

	@Override
	public String getPropertyWithVariableReplacement(ActionContext renderContext, PropertyKey<String> key) throws FrameworkException {
		return Scripting.replaceVariables(renderContext, this, getProperty(key), key.jsonName());
	}

	@Override
	public Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, EvaluationHints hints, final int row, final int column) throws FrameworkException {

		for (final PropertyKey propertyKey : properties.keySet()) {

			if (key.equals(propertyKey)) {

				return properties.get(propertyKey);
			}
		}

		return null;
	}

	// ----- Cloud synchronization and replication -----
	@Override
	public List<GraphObject> getSyncData() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public boolean isNode() {
		return false;
	}

	@Override
	public boolean isRelationship() {
		return false;
	}

	@Override
	public NodeInterface getSyncNode() {
		return null;
	}

	@Override
	public RelationshipInterface getSyncRelationship() {
		return null;
	}

	@Override
	public long getSourceTransactionId() {
		return TransactionCommand.getCurrentTransactionId();
	}

	@Override
	public boolean changelogEnabled() {
		return true;
	}
}
