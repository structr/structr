/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.api.search.SortOrder;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.CreationContainer;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.search.DefaultSortOrder;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.*;


/**
 * A common base class for {@link NodeInterface} and {@link RelationshipInterface}.
 *
 */
public interface GraphObject {

	String SYSTEM_CATEGORY     = "System";
	String VISIBILITY_CATEGORY = "Visibility";

	Traits getTraits();
	<T> T as(final Class<T> type);
	boolean is(final String type);

	String getType();
	String getUuid();

	void clearCaches();

	void setSecurityContext(final SecurityContext securityContext);
	SecurityContext getSecurityContext();

	// property container methods
	PropertyContainer getPropertyContainer();
	Set<PropertyKey> getFullPropertySet();
	Set<PropertyKey> getPropertyKeys(final String propertyView);
	long getSourceTransactionId();
	<T> Object setProperty(final PropertyKey<T> key, T value) throws FrameworkException;
	<T> Object setProperty(final PropertyKey<T> key, T value, final boolean isCreation) throws FrameworkException;
	void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException;
	void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException;

	boolean isNode();
	boolean isRelationship();

	<V> V getProperty(final PropertyKey<V> propertyKey);
	<V> V getProperty(final PropertyKey<V> propertyKey, final Predicate<GraphObject> filter);
	void removeProperty(final PropertyKey key) throws FrameworkException;

	boolean systemPropertiesUnlocked();
	void unlockSystemPropertiesOnce();
	void lockSystemProperties();

	boolean readOnlyPropertiesUnlocked();
	void unlockReadOnlyPropertiesOnce();
	void lockReadOnlyProperties();

	// lifecycle methods
	boolean isGranted(final Permission permission, final SecurityContext securityContext);
	boolean isGranted(final Permission permission, final SecurityContext securityContext, final boolean isCreation);
	boolean isValid(final ErrorBuffer errorBuffer);

	void indexPassiveProperties();
	void addToIndex();

	// visibility
	boolean isVisibleToPublicUsers();
	boolean isVisibleToAuthenticatedUsers();
	boolean isHidden();
	void setHidden(final boolean hidden) throws FrameworkException;

	// access
	Date getCreatedDate();
	Date getLastModifiedDate();
	void setLastModifiedDate(final Date date) throws FrameworkException;

	void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException;
	void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException;
	void onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException;
	void afterCreation(final SecurityContext securityContext) throws FrameworkException;
	void afterModification(final SecurityContext securityContext) throws FrameworkException;
	void afterDeletion(final SecurityContext securityContext, final PropertyMap properties);
	void ownerModified(final SecurityContext securityContext);
	void securityModified(final SecurityContext securityContext);
	void locationModified(final SecurityContext securityContext);
	void propagatedModification(final SecurityContext securityContext);

	// misc. methods
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

	default void filterIndexableForCreation(final SecurityContext securityContext, final PropertyMap src, final CreationContainer indexable, final PropertyMap filtered) throws FrameworkException {

		for (final Iterator<Map.Entry<PropertyKey, Object>> iterator = src.entrySet().iterator(); iterator.hasNext();) {

			final Map.Entry<PropertyKey, Object> attr = iterator.next();
			final PropertyKey key                     = attr.getKey();
			final Object value                        = attr.getValue();

			if (key instanceof FunctionProperty) {
				continue;
			}

			if (key == null) {
				throw new RuntimeException("Key is null, value is " + value + ".");
			}

			if (key.isPropertyTypeIndexable() && !key.isReadOnly() && !key.isSystemInternal() && !key.isUnvalidated()) {

				try {
					// value can be set directly, move to creation container
					key.setProperty(securityContext, indexable, value);
					iterator.remove();

					// store value to do notifications later
					filtered.put(key, value);

				} catch (ClassCastException e) {

					throw new FrameworkException(422, "Invalid JSON input for key " + key.jsonName() + ", expected a JSON " + key.typeName() + ".");
				}
			}
		}
	}

	default Comparable getComparableProperty(final PropertyKey key) {

		if (key != null) {

			final Object propertyValue = getProperty(key);

			// check property converter
			PropertyConverter converter = key.databaseConverter(getSecurityContext(), this);
			if (converter != null) {

				try {
					return converter.convertForSorting(propertyValue);

				} catch (Throwable t) {
					t.printStackTrace();
				}
			}

			// conversion failed, may the property value itself is comparable
			if (propertyValue instanceof Comparable) {
				return (Comparable) propertyValue;
			}

			// last try: convertFromInput to String to make comparable
			if (propertyValue != null) {
				return propertyValue.toString();
			}
		}

		return null;
	}

	default void setVisibility(final boolean visibleToPublic, final boolean visibleToAuth) throws FrameworkException {
		setVisibleToPublicUsers(visibleToPublic);
		setVisibleToAuthenticatedUsers(visibleToAuth);
	}

	default void setVisibleToPublicUsers(final boolean visibleToPublic) throws FrameworkException {
		setProperty(getTraits().key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), visibleToPublic);
	}

	default void setVisibleToAuthenticatedUsers(final boolean visibleToAuth) throws FrameworkException {
		setProperty(getTraits().key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), visibleToAuth);
	}
}
