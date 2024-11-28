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
import org.structr.api.UnknownClientException;
import org.structr.api.UnknownDatabaseException;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.search.SortOrder;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.*;
import org.structr.core.graph.search.DefaultSortOrder;
import org.structr.core.property.*;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.*;
import java.util.Map.Entry;


/**
 * A common base class for {@link AbstractNode} and {@link AbstractRelationship}.
 *
 *
 */
public interface GraphObject<T extends PropertyContainer> {


	/*

	public static final Property<String>  base                        = new StringProperty("base").partOfBuiltInSchema();
	public static final Property<String>  type                        = new TypeProperty().partOfBuiltInSchema().category(SYSTEM_CATEGORY);
	public static final Property<String>  id                          = new UuidProperty().partOfBuiltInSchema().category(SYSTEM_CATEGORY);

	public static final Property<Date>    createdDate                 = new ISO8601DateProperty("createdDate").readOnly().systemInternal().indexed().unvalidated().writeOnce().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly();
	public static final Property<String>  createdBy                   = new StringProperty("createdBy").readOnly().writeOnce().unvalidated().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly();

	public static final Property<Date>    lastModifiedDate            = new ISO8601DateProperty("lastModifiedDate").readOnly().systemInternal().passivelyIndexed().unvalidated().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly();
	public static final Property<String>  lastModifiedBy              = new StringProperty("lastModifiedBy").readOnly().systemInternal().unvalidated().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly();

	public static final Property<Boolean> visibleToPublicUsers        = new BooleanProperty("visibleToPublicUsers").passivelyIndexed().category(VISIBILITY_CATEGORY).partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly();
	public static final Property<Boolean> visibleToAuthenticatedUsers = new BooleanProperty("visibleToAuthenticatedUsers").passivelyIndexed().category(VISIBILITY_CATEGORY).partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly();

	 */

	Traits getTraits();

	String getType();
	String getUuid();
	void init(final SecurityContext securityContext, final T dbObject, final Class type, final long sourceTransactionId);
	void setSecurityContext(final SecurityContext securityContext);
	SecurityContext getSecurityContext();

	T getPropertyContainer();
	Set<PropertyKey> getPropertyKeys(String propertyView);
	long getSourceTransactionId();
	<T> Object setProperty(final PropertyKey<T> key, T value) throws FrameworkException;
	<T> Object setProperty(final PropertyKey<T> key, T value, final boolean isCreation) throws FrameworkException;
	void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException;
	void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException;
	boolean isGranted(final Permission permission, final SecurityContext securityContext, final boolean isCreation);

	boolean isNode();
	boolean isRelationship();

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

							if (this instanceof NodeInterface node) {

								final Traits traits = getTraits();

								TypeProperty.updateLabels(StructrApp.getInstance().getDatabaseService(), node, traits, true);
							}
						}

					} else if (isRelationship()) {

						if (!key.isUnvalidated()) {

							TransactionCommand.relationshipModified(securityContext.getCachedUser(), (AbstractRelationship)this, key, oldValue, value);
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

		for (PropertyKey key : getTraits().getPropertySet(PropertyView.All)) {

			if (key.isIndexed() && (key.isPassivelyIndexed() || key.isIndexedWhenEmpty())) {

				passiveIndexingKeys.add(key);
			}
		}

		addToIndex(passiveIndexingKeys);
	}

	default void addToIndex() {

		final Set<PropertyKey> indexKeys = new LinkedHashSet<>();

		for (PropertyKey key : getTraits().getPropertySet(PropertyView.All)) {

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

	default <T> T getProperty(final String propertyName) {

		final PropertyKey<T> key = getTraits().key(propertyName);
		if (key != null) {

			return getProperty(key);
		}

		throw new IllegalArgumentException("Invalid property key " + propertyName + " for type " + getClass().getSimpleName());
	}

	<V> V getProperty(final PropertyKey<V> propertyKey);
	<V> V getProperty(final PropertyKey<V> propertyKey, final Predicate<GraphObject> filter);
	void removeProperty(final PropertyKey key) throws FrameworkException;
	void unlockSystemPropertiesOnce();
	void unlockReadOnlyPropertiesOnce();
	String getPropertyWithVariableReplacement(final ActionContext renderContext, final PropertyKey<String> key) throws FrameworkException;
	Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, final EvaluationHints hints, final int row, final int column) throws FrameworkException;
	List<GraphObject> getSyncData() throws FrameworkException;

	NodeInterface getSyncNode();
	RelationshipInterface getSyncRelationship();

	// ----- static methods -----
	static SortOrder sorted(final PropertyKey key, final boolean sortDescending) {
		return new DefaultSortOrder(key, sortDescending);
	}

	boolean changelogEnabled();

	default boolean isFrontendNode() {
		return false;
	}
}
