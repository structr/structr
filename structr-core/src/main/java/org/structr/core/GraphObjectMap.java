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
package org.structr.core;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;
import org.structr.cmis.CMISInfo;
import org.structr.common.PermissionResolutionMask;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;

/**
 * A dummy graph object that uses a map as its data store.
 *
 *
 */
public class GraphObjectMap extends PropertyMap implements GraphObject {

	@Override
	public long getId() {
		return -1;
	}

	@Override
	public String getUuid() {
		return getProperty(GraphObject.id);
	}

	@Override
	public String getType() {
		return getProperty(GraphObject.id);
	}

	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {
		return properties.keySet();
	}

	@Override
	public <T> T getProperty(final PropertyKey<T> propertyKey, Predicate<GraphObject> filter) {
		return (T)properties.get(propertyKey);
	}

	@Override
	public Object setProperty(final PropertyKey key, final Object value) throws FrameworkException {
		properties.put(key, value);
		return null;
	}

	@Override
	public <T> T getProperty(final PropertyKey<T> propertyKey) {
		return (T)properties.get(propertyKey);
	}

	@Override
	public <T> Comparable getComparableProperty(final PropertyKey<T> key) {
		return (Comparable)getProperty(key);
	}

	@Override
	public void removeProperty(final PropertyKey key) throws FrameworkException {
		properties.remove(key);
	}

	@Override
	public PropertyKey getDefaultSortKey() {
		return null;
	}

	@Override
	public String getDefaultSortOrder() {
		return "asc";
	}

	@Override
	public void unlockReadOnlyPropertiesOnce() {
	}

	@Override
	public void unlockSystemPropertiesOnce() {
	}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		return true;
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		return true;
	}

	@Override
	public boolean onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
		return true;
	}

	@Override
	public void afterCreation(final SecurityContext securityContext) {
	}

	@Override
	public void afterModification(final SecurityContext securityContext) {
	}

	@Override
	public void afterDeletion(final SecurityContext securityContext, PropertyMap properties) {
	}

	@Override
	public void ownerModified(final SecurityContext securityContext) {
	}

	@Override
	public void securityModified(final SecurityContext securityContext) {
	}

	@Override
	public void locationModified(final SecurityContext securityContext) {
	}

	@Override
	public void propagatedModification(final SecurityContext securityContext) {
	}

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {
		return true;
	}

	// ----- interface map -----
	@Override
	public int size() {
		return properties.size();
	}

	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public Object put(PropertyKey key, Object value) {
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
	public void addToIndex() {
	}

	@Override
	public void updateInIndex() {
	}

	@Override
	public void removeFromIndex() {
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
		return Scripting.replaceVariables(renderContext, this, getProperty(key));
	}

	@Override
	public Object evaluate(final SecurityContext securityContext, final String key, final String defaultValue) throws FrameworkException {

		for (final PropertyKey propertyKey : properties.keySet()) {

			if (key.equals(propertyKey.jsonName())) {

				return properties.get(propertyKey);
			}
		}

		return null;
	}

	@Override
	public Object invokeMethod(String methodName, Map<String, Object> parameters, final boolean throwExceptionForUnknownMethods) throws FrameworkException {
		throw new UnsupportedOperationException("Invoking a method is not supported as this is a property map.");
	}

	@Override
	public CMISInfo getCMISInfo() {
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
	public PermissionResolutionMask getPermissionResolutionMask() {
		return null;
	}
}
