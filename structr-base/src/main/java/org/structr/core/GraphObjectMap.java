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
import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.core.traits.TraitDefinition;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
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
		return getProperty(Traits.of("NodeInterface").key("id"));
	}

	@Override
	public void clearCaches() {

	}

	@Override
	public Traits getTraits() {

		return new Traits() {

			@Override
			public Set<String> getLabels() {
				return Set.of("GraphObjectMap");
			}

			@Override
			public boolean contains(final String type) {
				return false;
			}

			@Override
			public <T> PropertyKey<T> key(final String name) {

				for (final PropertyKey key : properties.keySet()) {

					if (key.jsonName().equals(name)) {

						return key;
					}
				}

				return null;
			}

			@Override
			public boolean hasKey(final String name) {
				return key(name) != null;
			}

			@Override
			public String getName() {
				return "GraphObjectMap";
			}

			@Override
			public boolean isNodeType() {
				return false;
			}

			@Override
			public boolean isRelationshipType() {
				return false;
			}

			@Override
			public Set<PropertyKey> getAllPropertyKeys() {
				return properties.keySet();
			}

			@Override
			public Set<PropertyKey> getPropertyKeysForView(final String propertyView) {
				return Set.of();
			}

			@Override
			public <T extends LifecycleMethod> Set<T> getMethods(final Class<T> type) {
				return Set.of();
			}

			@Override
			public <T extends FrameworkMethod> T getMethod(final Class<T> type) {
				return null;
			}

			@Override
			public Map<String, AbstractMethod> getDynamicMethods() {
				return Map.of();
			}

			@Override
			public <T> T as(final Class<T> type, final GraphObject obj) {
				return null;
			}

			@Override
			public void registerImplementation(final TraitDefinition trait, final boolean isDynamic) {
			}

			@Override
			public Relation getRelation() {
				return null;
			}

			@Override
			public Set<TraitDefinition> getTraitDefinitions() {
				return Set.of();
			}

			@Override
			public boolean isInterface() {
				return false;
			}

			@Override
			public boolean isAbstract() {
				return false;
			}

			@Override
			public boolean isBuiltInType() {
				return true;
			}

			@Override
			public boolean changelogEnabled() {
				return false;
			}

			@Override
			public Set<String> getViewNames() {
				return Set.of();
			}

			@Override
			public Set<String> getAllTraits() {
				return Set.of();
			}

			@Override
			public Map<String, Map<String, PropertyKey>> removeDynamicTraits() {
				return Map.of();
			}
		};
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
		return getProperty(Traits.of("NodeInterface").key("type"));
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
	public boolean isGranted(Permission permission, SecurityContext securityContext, boolean isCreation) {
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
	public void setHidden(final boolean hidden) throws FrameworkException {
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
	public void setLastModifiedDate(final Date date) throws FrameworkException {
	}

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
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

			if (key.equals(propertyKey.jsonName())) {

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
