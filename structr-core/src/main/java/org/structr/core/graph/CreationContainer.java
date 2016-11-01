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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.structr.api.NotInTransactionException;
import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;
import org.structr.cmis.CMISInfo;
import org.structr.common.PermissionResolutionMask;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class CreationContainer implements GraphObject, PropertyContainer {

	private final Map<String, Object> data = new LinkedHashMap<>();
	private GraphObject         wrappedObj = null;

	public CreationContainer() {}

	public CreationContainer(final GraphObject obj) {
		this.wrappedObj = obj;
	}

	public GraphObject getWrappedObject() {
		return wrappedObj;
	}

	public Map<String, Object> getData() {
		return data;
	}

	// ----- interface GraphObject -----
	@Override
	public long getId() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String getUuid() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String getType() {
		if (wrappedObj == null) {
			return null;
		} else {
			return wrappedObj.getType();
		}
	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public SecurityContext getSecurityContext() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public PropertyContainer getPropertyContainer() {
		return this;
	}

	@Override
	public Iterable<PropertyKey> getPropertyKeys(String propertyView) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <T> Object setProperty(PropertyKey<T> key, T value) throws FrameworkException {

		data.put(key.dbName(), value);

		return null;
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <T> T getProperty(PropertyKey<T> propertyKey) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <T> T getProperty(PropertyKey<T> propertyKey, Predicate<GraphObject> filter) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <T> Comparable getComparableProperty(PropertyKey<T> key) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void removeProperty(PropertyKey key) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public PropertyKey getDefaultSortKey() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String getDefaultSortOrder() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void unlockSystemPropertiesOnce() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void unlockReadOnlyPropertiesOnce() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, ModificationQueue modificationQueue) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void afterCreation(SecurityContext securityContext) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void afterModification(SecurityContext securityContext) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void afterDeletion(SecurityContext securityContext, PropertyMap properties) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void ownerModified(SecurityContext securityContext) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void securityModified(SecurityContext securityContext) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void locationModified(SecurityContext securityContext) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void propagatedModification(SecurityContext securityContext) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void addToIndex() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void updateInIndex() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void removeFromIndex() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void indexPassiveProperties() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String getPropertyWithVariableReplacement(ActionContext renderContext, PropertyKey<String> key) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Object evaluate(SecurityContext securityContext, String key, String defaultValue) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Object invokeMethod(String methodName, Map<String, Object> parameters, boolean throwExceptionForUnknownMethods) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public PermissionResolutionMask getPermissionResolutionMask() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isNode() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isRelationship() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public NodeInterface getSyncNode() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public RelationshipInterface getSyncRelationship() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void updateFromPropertyMap(Map<String, Object> properties) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public CMISInfo getCMISInfo() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	// ----- interface PropertyContainer -----
	@Override
	public boolean hasProperty(String name) {
		return data.containsKey(name);
	}

	@Override
	public Object getProperty(String name) {
		return data.get(name);
	}

	@Override
	public Object getProperty(String name, Object defaultValue) {

		final Object value = getProperty(name);
		if (value != null) {

			return value;
		}

		return defaultValue;
	}

	@Override
	public void setProperty(String name, Object value) {
		data.put(name, value);
	}

	@Override
	public void setProperties(Map<String, Object> values) {
		data.putAll(values);
	}

	@Override
	public void removeProperty(String name) {
		data.remove(name);
	}

	@Override
	public Iterable<String> getPropertyKeys() {
		return data.keySet();
	}

	@Override
	public void delete() throws NotInTransactionException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isSpatialEntity() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}
}
