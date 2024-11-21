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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.Traits;
import org.structr.api.UnknownClientException;
import org.structr.api.UnknownDatabaseException;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.search.SortOrder;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.*;
import org.structr.core.graph.search.DefaultSortOrder;
import org.structr.core.property.*;
import org.structr.schema.CodeSource;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.*;
import java.util.Map.Entry;


/**
 * A common base class for {@link AbstractNode} and {@link AbstractRelationship}.
 *
 *
 */
public interface GraphObject extends CodeSource {


	static final String SYSTEM_CATEGORY     = "System";
	static final String VISIBILITY_CATEGORY = "Visibility";

	public static final Property<String>  base                        = new StringProperty("base").partOfBuiltInSchema();
	public static final Property<String>  type                        = new TypeProperty().partOfBuiltInSchema().category(SYSTEM_CATEGORY);
	public static final Property<String>  id                          = new UuidProperty().partOfBuiltInSchema().category(SYSTEM_CATEGORY);

	public static final Property<Date>    createdDate                 = new ISO8601DateProperty("createdDate").readOnly().systemInternal().indexed().unvalidated().writeOnce().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly();
	public static final Property<String>  createdBy                   = new StringProperty("createdBy").readOnly().writeOnce().unvalidated().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly();

	public static final Property<Date>    lastModifiedDate            = new ISO8601DateProperty("lastModifiedDate").readOnly().systemInternal().passivelyIndexed().unvalidated().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly();
	public static final Property<String>  lastModifiedBy              = new StringProperty("lastModifiedBy").readOnly().systemInternal().unvalidated().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly();

	public static final Property<Boolean> visibleToPublicUsers        = new BooleanProperty("visibleToPublicUsers").passivelyIndexed().category(VISIBILITY_CATEGORY).partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly();
	public static final Property<Boolean> visibleToAuthenticatedUsers = new BooleanProperty("visibleToAuthenticatedUsers").passivelyIndexed().category(VISIBILITY_CATEGORY).partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly();

	// ----- methods common to both types -----
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

	public void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException;
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException;

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

		for (final Entry<PropertyKey, Object> attr : properties.entrySet()) {

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

							TransactionCommand.nodeModified(securityContext.getCachedUser(), (AbstractNode)this, key, oldValue, value);
						}

						if (key instanceof TypeProperty) {

							if (this instanceof NodeInterface) {

								final Class type = StructrApp.getConfiguration().getNodeEntityClass((String)value);

								TypeProperty.updateLabels(StructrApp.getInstance().getDatabaseService(), (NodeInterface)this, type, true);
							}
						}

					} else if (isRelationship()) {

						if (!key.isUnvalidated()) {

							TransactionCommand.relationshipModified(securityContext.getCachedUser(), (AbstractRelationship)this, key, oldValue, value);
						}

						if (key instanceof TypeProperty) {

							if (this instanceof NodeInterface) {

								final Class type = StructrApp.getConfiguration().getNodeEntityClass((String)value);

								TypeProperty.updateLabels(StructrApp.getInstance().getDatabaseService(), (NodeInterface)this, type, true);
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

	default void indexPassiveProperties() {

		final Set<PropertyKey> passiveIndexingKeys = new LinkedHashSet<>();

		for (PropertyKey key : StructrApp.getConfiguration().getPropertySet(getTraits(), PropertyView.All)) {

			if (key.isIndexed() && (key.isPassivelyIndexed() || key.isIndexedWhenEmpty())) {

				passiveIndexingKeys.add(key);
			}
		}

		addToIndex(passiveIndexingKeys);
	}

	default void addToIndex() {

		final Set<PropertyKey> indexKeys = new LinkedHashSet<>();

		for (PropertyKey key : StructrApp.getConfiguration().getPropertySet(getTraits(), PropertyView.All)) {

			if (key.isIndexed()) {

				indexKeys.add(key);
			}
		}

		addToIndex(indexKeys);
	}

	default void addToIndex(final Set<PropertyKey> indexKeys) {

		final Map<String, Object> values = new LinkedHashMap<>();

		for (PropertyKey key : indexKeys) {

			final PropertyConverter converter = key.databaseConverter(getSecurityContext(), this);

			if (converter != null) {

				try {

					final Object value = converter.convert(this.getProperty(key));
					if (key.isPropertyValueIndexable(value)) {

						values.put(key.dbName(), value);
					}

				} catch (FrameworkException ex) {

					final Logger logger = LoggerFactory.getLogger(GraphObject.class);
					logger.warn("Unable to convert property {} of type {}: {}", key.dbName(), getClass().getSimpleName(), ex.getMessage());
					logger.warn("Exception", ex);
				}


			} else {

				final Object value = this.getProperty(key);
				if (key.isPropertyValueIndexable(value)) {

					// index unconverted value
					values.put(key.dbName(), value);
				}
			}
		}

		try {

			// use "internal" setProperty for "indexing"
			getPropertyContainer().setProperties(values);

		} catch (UnknownClientException | UnknownDatabaseException e) {

			final Logger logger = LoggerFactory.getLogger(GraphObject.class);
			logger.warn("Unable to index properties of {} with UUID {}: {}", getType(), getUuid(), e.getMessage());
			logger.warn("Properties: {}", values);
		}
	}

	default void filterIndexableForCreation(final SecurityContext securityContext, final PropertyMap src, final CreationContainer indexable, final PropertyMap filtered) throws FrameworkException {

		for (final Iterator<Entry<PropertyKey, Object>> iterator = src.entrySet().iterator(); iterator.hasNext();) {

			final Entry<PropertyKey, Object> attr = iterator.next();
			final PropertyKey key                 = attr.getKey();
			final Object value                    = attr.getValue();

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
	default public <T> T getProperty(final String propertyName) {

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
	public <T> T getProperty(final PropertyKey<T> propertyKey, final Predicate<GraphObject> filter);

	/**
	 * Returns the property value for the given key as a Comparable
	 *
	 * @param <T>
	 * @param key the property key to retrieve the value for
	 * @return the property value for the given key as a Comparable
	 */
	public <T> Comparable getComparableProperty(final PropertyKey<T> key);

	/**
	 * Removes the property value for the given key from this graph object.
	 *
	 * @param key the key to remove the value for
	 * @throws FrameworkException
	 */
	public void removeProperty(final PropertyKey key) throws FrameworkException;

	/**
	 * Unlock all system properties in this entity for a single <code>setProperty</code>
	 * call.
	 */
	public void unlockSystemPropertiesOnce();

	/**
	 * Unlock all read-only properties in this entity for a single <code>setProperty</code>
	 * call.
	 */
	public void unlockReadOnlyPropertiesOnce();


	public boolean isValid(ErrorBuffer errorBuffer);


	/**
	 * Called when an entity of this type is created in the database. This method can cause
	 * the underlying transaction to be rolled back in case of an error, either by throwing
	 * an exception, or by returning false.
	 *
	 * @param securityContext the context in which the creation takes place
	 * @param errorBuffer the error buffer to put error tokens into
	 * @throws FrameworkException
	 */
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException;

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
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException;

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
	public void onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException;

	/**
	 * Called when an entity was successfully created. Please note that this method
	 * will need to create its own toplevel transaction and can NOT cause the creation
	 * transaction to be rolled back.
	 *
	 * @param securityContext the context in which the creation took place
	 */
	public void afterCreation(final SecurityContext securityContext) throws FrameworkException;

	/**
	 * Called when an entity was successfully modified. Please note that this method
	 * will need to create its own toplevel transaction and can NOT cause the modification
	 * transaction to be rolled back.
	 *
	 * @param securityContext the context in which the modification took place
	 */
	public void afterModification(final SecurityContext securityContext) throws FrameworkException;

	/**
	 * Called when an entity was successfully deleted. Please note that this method
	 * has no access to the database entity since it is called _after_ the successful
	 * deletion.
	 *
	 * @param securityContext the context in which the deletion took place
	 * @param properties
	 */
	public void afterDeletion(final SecurityContext securityContext, final PropertyMap properties);

	/**
	 * Called when the owner of this entity was successfully modified. Please note
	 * that this method will run in its own toplevel transaction and can NOT prevent
	 * the owner modification.
	 *
	 * @param securityContext the context in which the owner modification took place
	 */
	public void ownerModified(final SecurityContext securityContext);

	/**
	 * Called when the permissions of this entity were successfully modified. Please note
	 * that this method will run in its own toplevel transaction and can NOT prevent the
	 * permission modification.
	 *
	 * @param securityContext the context in which the permission modification took place
	 */
	public void securityModified(final SecurityContext securityContext);

	/**
	 * Called when the location of this entity was successfully modified. Please note
	 * that this method will run in its own toplevel transaction and can NOT prevent the
	 * permission modification.
	 *
	 * @param securityContext the context in which the location modification took place
	 */
	public void locationModified(final SecurityContext securityContext);

	/**
	 * Called when a non-local modification occurred in the neighbourhood of this node.
	 *
	 * @param securityContext
	 */
	public void propagatedModification(final SecurityContext securityContext);

	public String getPropertyWithVariableReplacement(final ActionContext renderContext, final PropertyKey<String> key) throws FrameworkException;
	public Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, final EvaluationHints hints, final int row, final int column) throws FrameworkException;

	Traits getTraits();

	// ----- Cloud synchronization and replication -----
	/**
	 * Returns a list of objects that are part of this GraphObject's synchronization set.
	 * Caution: most implementations of this method may add null objects to the list.
	 *
	 * @return
	 * @throws FrameworkException
	 */
	public List<GraphObject> getSyncData() throws FrameworkException;

	public boolean isNode();
	public boolean isRelationship();

	public NodeInterface getSyncNode();
	public RelationshipInterface getSyncRelationship();

	// ----- static methods -----
	public static SortOrder sorted(final PropertyKey key, final boolean sortDescending) {
		return new DefaultSortOrder(key, sortDescending);
	}

	boolean changelogEnabled();

	default boolean isFrontendNode() {
		return false;
	}
}
