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
import org.structr.core.function.UserChangelogFunction;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.core.traits.AccessControllableTrait;
import org.structr.core.traits.GraphObjectTrait;
import org.structr.core.traits.PropertyContainerTrait;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

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
	protected AccessControllableTrait accessControllableTrait = null;
	protected PropertyContainerTrait propertyContainerTrait   = null;
	protected GraphObjectTrait graphObjectTrait               = null;
	protected Traits traits                                   = null;
	protected Identity id                                     = null;

	@Override
	public Traits getTraits() {
		return traits;
	}

	@Override
	public void init(final SecurityContext securityContext, final PropertyContainer propertyContainer, final Class entityType, final long sourceTransactionId) {

		this.traits              = Traits.of(propertyContainer.getType());
		this.sourceTransactionId = sourceTransactionId;
		this.securityContext     = securityContext;
		this.id                  = propertyContainer.getId();

		// preload traits for faster access
		this.accessControllableTrait = this.traits.getAccessControllableTrait();
		this.propertyContainerTrait  = this.traits.getPropertyContainerTrait();
		this.graphObjectTrait        = this.traits.getGraphObjectTrait();
	}

	@Override
	public long getSourceTransactionId() {
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
	public final boolean isGranted(final Permission permission, SecurityContext securityContext, boolean isCreation) {
		return accessControllableTrait.isGranted(this, permission, securityContext, isCreation);
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		return graphObjectTrait.isValid(this, errorBuffer);
	}

	@Override
	public void indexPassiveProperties() {
		graphObjectTrait.indexPassiveProperties(this);
	}

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		graphObjectTrait.onCreation(this, securityContext, errorBuffer);
	}

	@Override
	public final void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		graphObjectTrait.onModification(this, securityContext, errorBuffer, modificationQueue);
	}

	@Override
	public final void onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
		graphObjectTrait.onDeletion(this, securityContext, errorBuffer, properties);
	}

	@Override
	public final void afterCreation(final SecurityContext securityContext) throws FrameworkException {
		graphObjectTrait.afterCreation(this, securityContext);
	}

	@Override
	public final void afterModification(final SecurityContext securityContext) throws FrameworkException {
		graphObjectTrait.afterModification(this, securityContext);
	}

	@Override
	public final void afterDeletion(final SecurityContext securityContext, final PropertyMap properties) {
		graphObjectTrait.afterDeletion(this, securityContext, properties);
	}

	@Override
	public final void ownerModified(final SecurityContext securityContext) {
		graphObjectTrait.ownerModified(this, securityContext);
	}

	@Override
	public final void securityModified(final SecurityContext securityContext) {
		graphObjectTrait.securityModified(this, securityContext);
	}

	@Override
	public final void locationModified(final SecurityContext securityContext) {
		graphObjectTrait.locationModified(this, securityContext);
	}

	@Override
	public final void propagatedModification(final SecurityContext securityContext) {
		graphObjectTrait.propagatedModification(this, securityContext);
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
	public boolean readOnlyPropertiesUnlocked() {
		return readOnlyPropertiesUnlocked;
	}

	@Override
	public final void removeProperty(final PropertyKey key) throws FrameworkException {
		propertyContainerTrait.removeProperty(this, key);
	}

	@Override
	public boolean systemPropertiesUnlocked() {
		return internalSystemPropertiesUnlocked;
	}

	/**
	 * Indicates whether this node is visible to public users.
	 *
	 * @return whether this node is visible to public users
	 */
	public final boolean getVisibleToPublicUsers() {
		return getProperty(traits.key("visibleToPublicUsers"));
	}

	/**
	 * Indicates whether this node is visible to authenticated users.
	 *
	 * @return whether this node is visible to authenticated users
	 */
	public final boolean getVisibleToAuthenticatedUsers() {
		return getProperty(traits.key("visibleToPublicUsers"));
	}

	/**
	 * Indicates whether this node is hidden.
	 *
	 * @return whether this node is hidden
	 */
	public final boolean getHidden() {
		return getProperty(traits.key("hidden"));
	}

	/**
	 * Returns the property set for the given view as an Iterable.
	 *
	 * If a custom view is set via header, this can only include properties that are also included in the current view!
	 *
	 * @param propertyView
	 * @return the property set for the given view
	 */
	@Override
	public final Set<PropertyKey> getPropertyKeys(final String propertyView) {
		return propertyContainerTrait.getPropertyKeys(this, propertyView);
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
		return propertyContainerTrait.getProperty(this, key, null);
	}

	@Override
	public final <T> T getProperty(final PropertyKey<T> key, final Predicate<GraphObject> predicate) {
		return propertyContainerTrait.getProperty(this, key, predicate);
	}

	@Override
	public final <T> Object setProperty(final PropertyKey<T> key, final T value) throws FrameworkException {
		return propertyContainerTrait.setProperty(this, key, value);
	}

	@Override
	public final <T> Object setProperty(final PropertyKey<T> key, final T value, final boolean isCreation) throws FrameworkException {
		return propertyContainerTrait.setProperty(this, key, value, isCreation);
	}

	@Override
	public final void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {
		propertyContainerTrait.setProperties(this, securityContext, properties);
	}

	@Override
	public final void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {
		propertyContainerTrait.setProperties(this, securityContext, properties, isCreation);
	}

	@Override
	public final boolean isNode() {
		return false;
	}

	@Override
	public final boolean isRelationship() {
		return false;
	}

	@Override
	public final String getPropertyWithVariableReplacement(final ActionContext renderContext, final PropertyKey<String> key) throws FrameworkException {

		final Object value = getProperty(key);
		String result      = null;

		try {

			result = Scripting.replaceVariables(renderContext, this, value, true, key.jsonName());

		} catch (Throwable t) {

			logger.warn("Scripting error in {} {}:\n{}", key.dbName(), getUuid(), value, t);

		}

		return result;
	}

	@Override
	public Object evaluate(ActionContext actionContext, String key, String defaultValue, EvaluationHints hints, int row, int column) throws FrameworkException {
		return null;
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
		return false;
	}
}
