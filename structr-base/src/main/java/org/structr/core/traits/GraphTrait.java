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
package org.structr.core.traits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.UnknownClientException;
import org.structr.api.UnknownDatabaseException;
import org.structr.api.graph.Identity;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.search.SortOrder;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.*;
import org.structr.core.graph.search.DefaultSortOrder;
import org.structr.core.property.*;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.*;

public interface GraphTrait {

	String SYSTEM_CATEGORY     = "System";
	String VISIBILITY_CATEGORY = "Visibility";

	String getUuid();
	Identity getIdentity();
	Traits getTraits();

	default <T> T as(final Trait<T> trait) {
		return trait.getImplementation(this);
	}

	/**
	 * Returns the type of this graph object.
	 *
	 * @return the type
	 */
	public String getType();

	/**
	 * Sets the security context to be used by this entity.
	 *
	 * @param securityContext
	 */
	public void setSecurityContext(final SecurityContext securityContext);

	/**
	 * Returns the SecurityContext associated with this instance.
	 *
	 * @return the security context
	 */
	public SecurityContext getSecurityContext();

	/**
	 * Returns the underlying property container for this graph object.
	 *
	 * @return property container
	 */
	public PropertyContainer getPropertyContainer();

	/**
	 * Returns the property set for the given view as an Iterable.
	 *
	 * @param propertyView
	 * @return the property set for the given view
	 */
	public Set<PropertyKey> getPropertyKeys(final String propertyView);

	/**
	 * Returns the ID of the transaction in which this object was instantiated.
	 */
	long getSourceTransactionId();

	/**
	 * Sets the property with the given key to the given value.
	 *
	 * @param <T>
	 * @param key the property key to set
	 * @param value the value to set
	 * @return the relationship(s), if one or more are created
	 * @throws FrameworkException
	 */
	public <T> Object setProperty(final PropertyKey<T> key, T value) throws FrameworkException;
	public <T> Object setProperty(final PropertyKey<T> key, T value, final boolean isCreation) throws FrameworkException;

	void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException;
	void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException;

	/**
	 * Sets the given properties.
	 *
	 * @param securityContext
	 * @param properties
	 * @param isCreation
	 */
	default void setPropertiesInternal(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {

		final CreationContainer container = new CreationContainer(this);

		boolean atLeastOnePropertyChanged = false;

		for (final Map.Entry<PropertyKey, Object> attr : properties.entrySet()) {

			final PropertyKey key = attr.getKey();
			final Object value    = attr.getValue();

			if (value != null && key.isPropertyTypeIndexable() && key.relatedType() == null) {

				final Object oldValue = getProperty(key);
				if (!Objects.deepEquals(value, oldValue)) {

					atLeastOnePropertyChanged = true;

					// bulk set possible, store in container
					key.setProperty(securityContext, container, value);

					if (isNode()) {

						if (!key.isUnvalidated()) {

							TransactionCommand.nodeModified(securityContext.getCachedUser(), (NodeTrait)this, key, oldValue, value);
						}

						if (key instanceof TypeProperty) {

							if (this instanceof NodeTrait n) {

								TypeProperty.updateLabels(StructrApp.getInstance().getDatabaseService(), n, true);
							}
						}

					} else if (isRelationship()) {

						if (!key.isUnvalidated()) {

							TransactionCommand.relationshipModified(securityContext.getCachedUser(), (RelationshipTrait)this, key, oldValue, value);
						}

						if (key instanceof TypeProperty) {

							if (this instanceof NodeTrait n) {

								TypeProperty.updateLabels(StructrApp.getInstance().getDatabaseService(), n, true);
							}
						}
					}
				}

			} else {

				// bulk set NOT possible, set on entity
				if (key.isSystemInternal()) {
					unlockSystemPropertiesOnce();
				}

				setProperty(key, value, isCreation);
			}
		}

		if (atLeastOnePropertyChanged) {

			try {

				// set primitive values directly for better performance
				getPropertyContainer().setProperties(container.getData());

			} catch (UnknownClientException | UnknownDatabaseException e) {

				final Logger logger = LoggerFactory.getLogger(GraphObject.class);

				logger.warn("Unable to set properties of {} with UUID {}: {}", getType(), getUuid(), e.getMessage());
				logger.warn("Properties: {}", container.getData());
			}
		}
	}

	default void filterIndexableForCreation(final SecurityContext securityContext, final PropertyMap src, final CreationContainer indexable, final PropertyMap filtered) throws FrameworkException {

		for (final Iterator<Map.Entry<PropertyKey, Object>> iterator = src.entrySet().iterator(); iterator.hasNext();) {

			final Map.Entry<PropertyKey, Object> attr = iterator.next();
			final PropertyKey key                     = attr.getKey();
			final Object value                        = attr.getValue();

			if (key instanceof FunctionProperty) {
				continue;
			}

			if (key.isPropertyTypeIndexable() && !key.isReadOnly() && !key.isSystemInternal() && !key.isUnvalidated()) {

				// value can be set directly, move to creation container
				key.setProperty(securityContext, indexable, value);
				iterator.remove();

				// store value to do notifications later
				filtered.put(key, value);
			}
		}
	}

	/**
	 * Returns the (converted, validated, transformed, etc.) property for the given
	 * property key.
	 *
	 * @param <T>
	 * @param propertyName the property key to retrieve the value for
	 * @return the converted, validated, transformed property value
	 */
	default <T> T getProperty(final String propertyName) {

		final PropertyKey<T> key = StructrApp.getConfiguration().getPropertyKeyForJSONName(getTraits(), propertyName, false);
		if (key != null) {

			return getProperty(key);
		}

		throw new IllegalArgumentException("Invalid property key " + propertyName + " for type " + getClass().getSimpleName());
	}

	/**
	 * Returns the (converted, validated, transformed, etc.) property for the given
	 * property key.
	 *
	 * @param <T>
	 * @param propertyKey the property key to retrieve the value for
	 * @return the converted, validated, transformed property value
	 */
	public <T> T getProperty(final PropertyKey<T> propertyKey);

	/**
	 * Returns the (converted, validated, transformed, etc.) property for the given
	 * property key with the given filter applied to it.
	 *
	 * @param <T>
	 * @param propertyKey the property key to retrieve the value for
	 * @param filter the filter to apply to all properties
	 * @return the converted, validated, transformed property value
	 */
	public <T> T getProperty(final PropertyKey<T> propertyKey, final Predicate<GraphTrait> filter);

	/**
	 * Removes the property value for the given key from this graph object.
	 *
	 * @param key the key to remove the value for
	 * @throws FrameworkException
	 */
	void removeProperty(final PropertyKey key) throws FrameworkException;

	/**
	 * Unlock all system properties in this entity for a single <code>setProperty</code>
	 * call.
	 */
	void unlockSystemPropertiesOnce();

	/**
	 * Unlock all read-only properties in this entity for a single <code>setProperty</code>
	 * call.
	 */
	void unlockReadOnlyPropertiesOnce();

	boolean isValid(ErrorBuffer errorBuffer);

	/**
	 * Called when an entity of this type is created in the database. This method can cause
	 * the underlying transaction to be rolled back in case of an error, either by throwing
	 * an exception, or by returning false.
	 *
	 * @param securityContext the context in which the creation takes place
	 * @param errorBuffer the error buffer to put error tokens into
	 * @throws FrameworkException
	 */
	void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException;

	/**
	 * Called when an entity of this type is modified. This method can cause the underlying
	 * transaction to be rolled back in case of an error, either by throwing an exception,
	 * or by returning false.
	 *
	 * @param securityContext the context in which the modification takes place
	 * @param errorBuffer the error buffer to put error tokens into
	 * @param modificationQueue the modification queue that triggered this call to onModification
	 * @throws FrameworkException
	 */
	void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException;

	/**
	 * Called when an entity of this type is deleted. This method can cause the underlying
	 * transaction to be rolled back in case of an error, either by throwing an exception,
	 * or by returning false.
	 *
	 * @param securityContext the context in which the deletion takes place
	 * @param errorBuffer the error buffer to put error tokens into
	 * @param properties
	 * @throws FrameworkException
	 */
	void onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException;

	/**
	 * Called when an entity was successfully created. Please note that this method
	 * will need to create its own toplevel transaction and can NOT cause the creation
	 * transaction to be rolled back.
	 *
	 * @param securityContext the context in which the creation took place
	 */
	void afterCreation(final SecurityContext securityContext) throws FrameworkException;

	/**
	 * Called when an entity was successfully modified. Please note that this method
	 * will need to create its own toplevel transaction and can NOT cause the modification
	 * transaction to be rolled back.
	 *
	 * @param securityContext the context in which the modification took place
	 */
	void afterModification(final SecurityContext securityContext) throws FrameworkException;

	/**
	 * Called when an entity was successfully deleted. Please note that this method
	 * has no access to the database entity since it is called _after_ the successful
	 * deletion.
	 *
	 * @param securityContext the context in which the deletion took place
	 * @param properties
	 */
	void afterDeletion(final SecurityContext securityContext, final PropertyMap properties);

	/**
	 * Called when the owner of this entity was successfully modified. Please note
	 * that this method will run in its own toplevel transaction and can NOT prevent
	 * the owner modification.
	 *
	 * @param securityContext the context in which the owner modification took place
	 */
	void ownerModified(final SecurityContext securityContext);

	/**
	 * Called when the permissions of this entity were successfully modified. Please note
	 * that this method will run in its own toplevel transaction and can NOT prevent the
	 * permission modification.
	 *
	 * @param securityContext the context in which the permission modification took place
	 */
	void securityModified(final SecurityContext securityContext);

	/**
	 * Called when the location of this entity was successfully modified. Please note
	 * that this method will run in its own toplevel transaction and can NOT prevent the
	 * permission modification.
	 *
	 * @param securityContext the context in which the location modification took place
	 */
	void locationModified(final SecurityContext securityContext);

	/**
	 * Called when a non-local modification occurred in the neighbourhood of this node.
	 *
	 * @param securityContext
	 */
	void propagatedModification(final SecurityContext securityContext);

	String getPropertyWithVariableReplacement(final ActionContext renderContext, final PropertyKey<String> key) throws FrameworkException;
	Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, final EvaluationHints hints, final int row, final int column) throws FrameworkException;

	// ----- Cloud synchronization and replication -----
	/**
	 * Returns a list of objects that are part of this GraphObject's synchronization set.
	 * Caution: most implementations of this method may add null objects to the list.
	 *
	 * @return
	 * @throws FrameworkException
	 */
	List<GraphTrait> getSyncData() throws FrameworkException;

	boolean isNode();
	boolean isRelationship();

	NodeTrait getSyncNode();
	RelationshipTrait getSyncRelationship();

	// ----- static methods -----
	static SortOrder sorted(final PropertyKey key, final boolean sortDescending) {
		return new DefaultSortOrder(key, sortDescending);
	}

	boolean changelogEnabled();

	default boolean isFrontendNode() {
		return false;
	}
}
