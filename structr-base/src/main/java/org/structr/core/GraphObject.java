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
import org.structr.common.error.ErrorBuffer;
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
public interface GraphObject {


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

	void clearCaches();

	void init(final SecurityContext securityContext, final PropertyContainer dbObject, final Class type, final long sourceTransactionId);

	void setSecurityContext(final SecurityContext securityContext);
	SecurityContext getSecurityContext();

	// property container methods
	PropertyContainer getPropertyContainer();
	Set<PropertyKey> getPropertySet(final String propertyView);
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
	boolean isGranted(final Permission permission, final SecurityContext securityContext, final boolean isCreation);
	boolean isValid(final ErrorBuffer errorBuffer);

	void indexPassiveProperties();

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
}
