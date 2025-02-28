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
package org.structr.core.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.graph.Identity;
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
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AccessControllableTraitDefinition;
import org.structr.core.traits.operations.accesscontrollable.IsGranted;
import org.structr.core.traits.operations.graphobject.*;
import org.structr.core.traits.operations.propertycontainer.*;
import org.structr.schema.action.ActionContext;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for all node entities in Structr.
 */
public abstract class AbstractGraphObject<T extends PropertyContainer> implements GraphObject {

	private static final Logger logger = LoggerFactory.getLogger(AbstractGraphObject.class);

	protected Map<String, Object> tmpStorageContainer         = null;
	protected boolean readOnlyPropertiesUnlocked              = false;
	protected boolean internalSystemPropertiesUnlocked        = false;
	protected long sourceTransactionId                        = -1;
	protected String cachedUuid                               = null;
	protected SecurityContext securityContext                 = null;
	protected Traits typeHandler                              = null;
	protected Identity id                                     = null;

	public AbstractGraphObject(final SecurityContext securityContext, final PropertyContainer propertyContainer, final long sourceTransactionId) {

		String type = (String)propertyContainer.getProperty("type");
		if (type == null) {

			type = StructrTraits.NODE_INTERFACE;
		}

		this.typeHandler         = Traits.exists(type) ? Traits.of(type) : Traits.of(StructrTraits.NODE_INTERFACE);
		this.sourceTransactionId = sourceTransactionId;
		this.securityContext     = securityContext;
		this.id                  = propertyContainer.getId();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof AbstractGraphObject g) {

			return g.id.equals(this.id);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public final Traits getTraits() {
		return typeHandler;
	}

	@Override
	public <T> T as(final Class<T> type) {
		return typeHandler.as(type, this);
	}

	@Override
	public boolean is(final String type) {
		return typeHandler.contains(type);
	}

	@Override
	public String getUuid() {
		return getProperty(typeHandler.key("id"));
	}

	@Override
	public void clearCaches() {
		AccessControllableTraitDefinition.clearCaches();
	}

	@Override
	public final long getSourceTransactionId() {
		return sourceTransactionId;
	}

	@Override
	public final void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	@Override
	public final SecurityContext getSecurityContext() {
		return securityContext;
	}

	public String toString() {
		return getUuid();
	}

	/**
	 * Can be used to permit the setting of a read-only property once. The
	 * lock will be restored automatically after the next setProperty
	 * operation. This method exists to prevent automatic set methods from
	 * setting a read-only property while allowing a manual set method to
	 * override this default behaviour.
	 */
	@Override
	public final void unlockReadOnlyPropertiesOnce() {
		this.readOnlyPropertiesUnlocked = true;
	}

	@Override
	public final void lockReadOnlyProperties() {
		this.readOnlyPropertiesUnlocked = false;

	}

	@Override
	public final boolean isGranted(final Permission permission, SecurityContext securityContext) {
		return isGranted(permission, securityContext, false);
	}

	@Override
	public boolean isGranted(Permission permission, SecurityContext securityContext, boolean isCreation) {

		if (this instanceof NodeInterface node) {

			return typeHandler.getMethod(IsGranted.class).isGranted(node, permission, securityContext, isCreation);
		}

		return true;
	}

	@Override
	public final boolean isValid(final ErrorBuffer errorBuffer) {

		boolean andValue = true;

		for (final IsValid isValid : typeHandler.getMethods(IsValid.class)) {

			andValue &= isValid.isValid(this, errorBuffer);
		}

		return andValue;
	}

	@Override
	public final void addToIndex() {

		final AddToIndex callback = typeHandler.getMethod(AddToIndex.class);
		if (callback != null) {
			callback.addToIndex(this);
		}
	}

	@Override
	public final void indexPassiveProperties() {

		final IndexPassiveProperties callback = typeHandler.getMethod(IndexPassiveProperties.class);
		if (callback != null) {

			callback.indexPassiveProperties(this);
		}
	}

	@Override
	public final void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		for (final OnCreation callback : typeHandler.getMethods(OnCreation.class)) {
			callback.onCreation(this, securityContext, errorBuffer);
		}
	}

	@Override
	public final void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		for (final OnModification callback : typeHandler.getMethods(OnModification.class)) {
			callback.onModification(this, securityContext, errorBuffer, modificationQueue);
		}
	}

	@Override
	public final void onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

		for (final OnDeletion callback : typeHandler.getMethods(OnDeletion.class)) {
			callback.onDeletion(this, securityContext, errorBuffer, properties);
		}
	}

	@Override
	public final void afterCreation(final SecurityContext securityContext) throws FrameworkException {

		for (final AfterCreation callback : typeHandler.getMethods(AfterCreation.class)) {
			callback.afterCreation(this, securityContext);
		}
	}

	@Override
	public final void afterModification(final SecurityContext securityContext) throws FrameworkException {

		for (final AfterModification callback : typeHandler.getMethods(AfterModification.class)) {
			callback.afterModification(this, securityContext);
		}
	}

	@Override
	public final void afterDeletion(final SecurityContext securityContext, final PropertyMap properties) {

		for (final AfterDeletion callback : typeHandler.getMethods(AfterDeletion.class)) {
			callback.afterDeletion(this, securityContext, properties);
		}
	}

	@Override
	public final void ownerModified(final SecurityContext securityContext) {

		for (final OwnerModified callback : typeHandler.getMethods(OwnerModified.class)) {
			callback.ownerModified(this, securityContext);
		}
	}

	@Override
	public final void securityModified(final SecurityContext securityContext) {

		for (final SecurityModified callback : typeHandler.getMethods(SecurityModified.class)) {
			callback.securityModified(this, securityContext);
		}
	}

	@Override
	public final void locationModified(final SecurityContext securityContext) {

		for (final LocationModified callback : typeHandler.getMethods(LocationModified.class)) {
			callback.locationModified(this, securityContext);
		}
	}

	@Override
	public final void propagatedModification(final SecurityContext securityContext) {

		for (final PropagatedModification callback : typeHandler.getMethods(PropagatedModification.class)) {
			callback.propagatedModification(this, securityContext);
		}
	}

	/**
	 * Can be used to permit the setting of a system property once. The
	 * lock will be restored automatically after the next setProperty
	 * operation. This method exists to prevent automatic set methods from
	 * setting a system property while allowing a manual set method to
	 * override this default behaviour.
	 */
	@Override
	public final void unlockSystemPropertiesOnce() {
		this.internalSystemPropertiesUnlocked = true;
		unlockReadOnlyPropertiesOnce();
	}

	@Override
	public final void lockSystemProperties() {
		this.internalSystemPropertiesUnlocked = true;
	}

	@Override
	public final boolean readOnlyPropertiesUnlocked() {
		return readOnlyPropertiesUnlocked;
	}

	@Override
	public final boolean systemPropertiesUnlocked() {
		return internalSystemPropertiesUnlocked;
	}

	/**
	 * Indicates whether this node is visible to public users.
	 *
	 * @return whether this node is visible to public users
	 */
	@Override
	public boolean isVisibleToPublicUsers() {
		return typeHandler.getMethod(GetVisibilityFlags.class).isVisibleToPublicUsers(this);
	}

	/**
	 * Indicates whether this node is visible to authenticated users.
	 *
	 * @return whether this node is visible to authenticated users
	 */
	@Override
	public boolean isVisibleToAuthenticatedUsers() {
		return typeHandler.getMethod(GetVisibilityFlags.class).isVisibleToAuthenticatedUsers(this);
	}

	/**
	 * Indicates whether this node is hidden.
	 *
	 * @return whether this node is hidden
	 */
	@Override
	public boolean isHidden() {
		return getProperty(typeHandler.key("hidden"));
	}

	@Override
	public void setHidden(final boolean hidden) throws FrameworkException {
		setProperty(typeHandler.key("hidden"), hidden);
	}

	@Override
	public final Date getCreatedDate() {
		return getProperty(typeHandler.key("createdDate"));
	}

	@Override
	public final Date getLastModifiedDate() {
		return getProperty(typeHandler.key("lastModifiedDate"));
	}

	@Override
	public final void setLastModifiedDate(final Date date) throws FrameworkException {
		setProperty(typeHandler.key("lastModifiedDate"), date);
	}

	/**
	 * Returns the property set for the given view as an Iterable.
	 * <p>
	 * If a custom view is set via header, this can only include properties that are also included in the current view!
	 *
	 * @param propertyView
	 * @return the property set for the given view
	 */
	@Override
	public final Set<PropertyKey> getPropertyKeys(final String propertyView) {
		return typeHandler.getMethod(GetPropertyKeys.class).getPropertyKeys(this, propertyView);
	}

	@Override
	public Set<PropertyKey> getFullPropertySet() {
		return typeHandler.getMethod(GetPropertySet.class).getAllPropertyKeys(this);
	}

	/**
	 * Returns the (converted, validated, transformed, etc.) property for
	 * the given property key.
	 *
	 * @param <T>
	 * @param key the property key to retrieve the value for
	 * @return the converted, validated, transformed property value
	 */
	@Override
	public final <T> T getProperty(final PropertyKey<T> key) {
		return getProperty(key, null);
	}

	@Override
	public final <T> T getProperty(final PropertyKey<T> key, final Predicate<GraphObject> predicate) {
		return typeHandler.getMethod(GetProperty.class).getProperty(this, key, predicate);
	}

	@Override
	public <T> Object setProperty(PropertyKey<T> key, T value) throws FrameworkException {
		return setProperty(key, value, false);
	}

	@Override
	public final <T> Object setProperty(final PropertyKey<T> key, final T value, final boolean isCreation) throws FrameworkException {
		return typeHandler.getMethod(SetProperty.class).setProperty(this, key, value, isCreation);
	}

	@Override
	public void setProperties(SecurityContext securityContext, PropertyMap properties) throws FrameworkException {
		setProperties(securityContext, properties, false);
	}

	@Override
	public final void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {
		typeHandler.getMethod(SetProperties.class).setProperties(this, securityContext, properties, isCreation);
	}

	@Override
	public final void removeProperty(final PropertyKey key) throws FrameworkException {
		typeHandler.getMethod(RemoveProperty.class).removeProperty(this, key);
	}

	@Override
	public final boolean isNode() {
		return typeHandler.isNodeType();
	}

	@Override
	public final boolean isRelationship() {
		return !typeHandler.isNodeType();
	}

	@Override
	public final String getPropertyWithVariableReplacement(final ActionContext renderContext, final PropertyKey<String> key) throws FrameworkException {

		final String value = getProperty(key);
		String result      = null;

		try {

			result = Scripting.replaceVariables(renderContext, this, value, true, key.jsonName());

		} catch (Throwable t) {

			logger.warn("Scripting error in {} {}:\n{}", key, getUuid(), value, t);

		}

		return result;
	}

	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {
		return List.of();
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
	public boolean changelogEnabled() {
		return typeHandler.changelogEnabled();
	}
}
