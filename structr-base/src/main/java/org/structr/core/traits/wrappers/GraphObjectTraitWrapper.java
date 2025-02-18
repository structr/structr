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
package org.structr.core.traits.wrappers;

import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class GraphObjectTraitWrapper<T extends GraphObject> implements GraphObject {

	protected final T wrappedObject;
	protected final Traits traits;

	public GraphObjectTraitWrapper(final Traits traits, final T wrappedObject) {

		this.wrappedObject = wrappedObject;
		this.traits        = traits;
	}

	/**
	 * Implement standard toString() method
	 */
	@Override
	public String toString() {
		return getUuid();

	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof GraphObject o) {

			// equality based on uuid
			return getUuid().equals(o.getUuid());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return wrappedObject.hashCode();
	}

	public SecurityContext getSecurityContext() {
		return wrappedObject.getSecurityContext();
	}

	@Override
	public PropertyContainer getPropertyContainer() {
		return wrappedObject.getPropertyContainer();
	}

	@Override
	public Set<PropertyKey> getFullPropertySet() {
		return wrappedObject.getFullPropertySet();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys(final String propertyView) {
		return wrappedObject.getPropertyKeys(propertyView);
	}

	@Override
	public long getSourceTransactionId() {
		return wrappedObject.getSourceTransactionId();
	}

	@Override
	public <T> Object setProperty(final PropertyKey<T> key, final T value) throws FrameworkException {
		return wrappedObject.setProperty(key, value);
	}

	@Override
	public <T> Object setProperty(final PropertyKey<T> key, final T value, final boolean isCreation) throws FrameworkException {
		return wrappedObject.setProperty(key, value, isCreation);
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {
		wrappedObject.setProperties(securityContext, properties);
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {
		wrappedObject.setProperties(securityContext, properties, isCreation);
	}

	@Override
	public boolean isNode() {
		return wrappedObject.isNode();
	}

	@Override
	public boolean isRelationship() {
		return wrappedObject.isRelationship();
	}

	@Override
	public <V> V getProperty(final PropertyKey<V> propertyKey) {
		return wrappedObject.getProperty(propertyKey);
	}

	@Override
	public <V> V getProperty(final PropertyKey<V> propertyKey, final Predicate<GraphObject> filter) {
		return wrappedObject.getProperty(propertyKey, filter);
	}

	@Override
	public void removeProperty(final PropertyKey key) throws FrameworkException {
		wrappedObject.removeProperty(key);
	}

	@Override
	public boolean systemPropertiesUnlocked() {
		return wrappedObject.systemPropertiesUnlocked();
	}

	@Override
	public void unlockSystemPropertiesOnce() {
		wrappedObject.unlockSystemPropertiesOnce();
	}

	@Override
	public void lockSystemProperties() {
		wrappedObject.lockSystemProperties();
	}

	@Override
	public boolean readOnlyPropertiesUnlocked() {
		return wrappedObject.readOnlyPropertiesUnlocked();
	}

	@Override
	public void unlockReadOnlyPropertiesOnce() {
		wrappedObject.unlockReadOnlyPropertiesOnce();
	}

	@Override
	public void lockReadOnlyProperties() {
		wrappedObject.lockReadOnlyProperties();
	}

	@Override
	public boolean isGranted(final Permission permission, final SecurityContext securityContext) {
		return wrappedObject.isGranted(permission, securityContext);
	}

	@Override
	public boolean isGranted(final Permission permission, final SecurityContext securityContext, final boolean isCreation) {
		return wrappedObject.isGranted(permission, securityContext, isCreation);
	}

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {
		return wrappedObject.isValid(errorBuffer);
	}

	@Override
	public void indexPassiveProperties() {
		wrappedObject.indexPassiveProperties();
	}

	@Override
	public void addToIndex() {
		wrappedObject.addToIndex();
	}

	public String getUuid() {
		return wrappedObject.getUuid();
	}

	@Override
	public void clearCaches() {
		wrappedObject.clearCaches();
	}

	@Override
	public void setSecurityContext(final SecurityContext securityContext) {
		wrappedObject.setSecurityContext(securityContext);
	}

	@Override
	public Traits getTraits() {
		return traits;
	}

	@Override
	public <T> T as(Class<T> type) {
		return wrappedObject.as(type);
	}

	@Override
	public boolean is(final String type) {
		return wrappedObject.is(type);
	}

	public String getType() {
		return wrappedObject.getType();
	}

	public String getName() {
		return wrappedObject.getProperty(traits.key("name"));
	}

	public void setName(final String name) throws FrameworkException {
		wrappedObject.setProperty(traits.key("name"), name);
	}

	public Date getCreatedDate() {
		return wrappedObject.getProperty(traits.key("createdDate"));
	}

	public Date getLastModifiedDate() {
		return wrappedObject.getProperty(traits.key("lastModifiedDate"));
	}

	public void setLastModifiedDate(final Date date) throws FrameworkException {
		wrappedObject.setProperty(traits.key("lastModifiedDate"), date);
	}

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		wrappedObject.onCreation(securityContext, errorBuffer);
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		wrappedObject.onModification(securityContext, errorBuffer, modificationQueue);
	}

	@Override
	public void onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
		wrappedObject.onDeletion(securityContext, errorBuffer, properties);
	}

	@Override
	public void afterCreation(final SecurityContext securityContext) throws FrameworkException {
		wrappedObject.afterCreation(securityContext);
	}

	@Override
	public void afterModification(final SecurityContext securityContext) throws FrameworkException {
		wrappedObject.afterModification(securityContext);
	}

	@Override
	public void afterDeletion(final SecurityContext securityContext, final PropertyMap properties) {
		wrappedObject.afterDeletion(securityContext, properties);
	}

	@Override
	public void ownerModified(final SecurityContext securityContext) {
		wrappedObject.ownerModified(securityContext);
	}

	@Override
	public void securityModified(final SecurityContext securityContext) {
		wrappedObject.securityModified(securityContext);
	}

	@Override
	public void locationModified(final SecurityContext securityContext) {
		wrappedObject.locationModified(securityContext);
	}

	@Override
	public void propagatedModification(final SecurityContext securityContext) {
		wrappedObject.propagatedModification(securityContext);
	}

	@Override
	public String getPropertyWithVariableReplacement(final ActionContext renderContext, final PropertyKey<String> key) throws FrameworkException {
		return wrappedObject.getPropertyWithVariableReplacement(renderContext, key);
	}

	@Override
	public Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, final EvaluationHints hints, final int row, final int column) throws FrameworkException {
		return wrappedObject.evaluate(actionContext, key, defaultValue, hints, row, column);
	}

	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {
		return wrappedObject.getSyncData();
	}

	@Override
	public NodeInterface getSyncNode() {
		return wrappedObject.getSyncNode();
	}

	@Override
	public RelationshipInterface getSyncRelationship() {
		return wrappedObject.getSyncRelationship();
	}

	@Override
	public boolean changelogEnabled() {
		return wrappedObject.changelogEnabled();
	}

	public boolean isVisibleToPublicUsers() {
		return wrappedObject.getProperty(traits.key("visibleToPublicUsers"));
	}

	public boolean isVisibleToAuthenticatedUsers() {
	return wrappedObject.getProperty(traits.key("visibleToAuthenticatedUsers"));
	}

	@Override
	public boolean isHidden() {
		return wrappedObject.getProperty(traits.key("hidden"));
	}

	@Override
	public void setHidden(final boolean hidden) throws FrameworkException {
		wrappedObject.setProperty(traits.key("hidden"), hidden);
	}

	public void setVisibleToPublicUsers(final boolean visible) throws FrameworkException {
		wrappedObject.setProperty(traits.key("visibleToPublicUsers"), visible);
	}

	public void  setVisibleToAuthenticatedUsers(final boolean visible) throws FrameworkException {
		wrappedObject.setProperty(traits.key("visibleToAuthenticatedUsers"), visible);
	}
}
