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

import org.structr.api.NotInTransactionException;
import org.structr.api.Predicate;
import org.structr.api.graph.Identity;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.action.Function;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class CreationContainer<T extends Comparable> implements GraphObject, PropertyContainer {

	private final Map<String, Object> data = new LinkedHashMap<>();
	private GraphObject         wrappedObj = null;
	private boolean isNode                 = true;

	public CreationContainer(final boolean isNode) {
		this.isNode = isNode;
	}

	public CreationContainer(final GraphObject obj) {
		this.wrappedObj = obj;
		this.isNode     = obj.isNode();
	}

	public GraphObject getWrappedObject() {
		return wrappedObj;
	}

	public Map<String, Object> getData() {
		return data;
	}

	// ----- interface GraphObject -----
	@Override
	public Identity getId() {
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
	public Set<PropertyKey> getPropertyKeys(String propertyView) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <T> Object setProperty(PropertyKey<T> key, T value) throws FrameworkException {
		return setProperty(key, value, false);
	}

	@Override
	public <T> Object setProperty(PropertyKey<T> key, T value, final boolean isCreation) throws FrameworkException {

		data.put(key.dbName(), value);

		return null;
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {
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
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, ModificationQueue modificationQueue) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void afterCreation(SecurityContext securityContext) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void afterModification(SecurityContext securityContext) throws FrameworkException {
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
	public void indexPassiveProperties() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String getPropertyWithVariableReplacement(ActionContext renderContext, PropertyKey<String> key) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, final EvaluationHints hints, final int row, final int column) throws FrameworkException {

		final Object value = data.get(key);
		if (value != null) {

			return value;
		}

		return Function.numberOrString(defaultValue);
	}

	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isNode() {
		return isNode;
	}

	@Override
	public boolean isRelationship() {
		return !isNode;
	}

	@Override
	public NodeInterface getSyncNode() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public RelationshipInterface getSyncRelationship() {
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
	public void delete(final boolean deleteRelationships) throws NotInTransactionException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isDeleted() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Class getEntityType() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public long getSourceTransactionId() {

		if (wrappedObj != null) {
			return wrappedObj.getSourceTransactionId();
		}

		return -1L;
	}

	@Override
	public boolean changelogEnabled() {

		if (wrappedObj != null) {
			return wrappedObj.changelogEnabled();
		}

		return true;
	}
}
