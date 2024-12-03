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

import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.search.SortOrder;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.search.DefaultSortOrder;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.List;
import java.util.Set;


/**
 * A common base class for {@link AbstractNode} and {@link AbstractRelationship}.
 *
 */
public interface GraphObject {

	Traits getTraits();

	String getType();
	String getUuid();

	void clearCaches();

	void init(final SecurityContext securityContext, final PropertyContainer dbObject, final String type, final long sourceTransactionId);

	void setSecurityContext(final SecurityContext securityContext);
	SecurityContext getSecurityContext();

	// property container methods
	PropertyContainer getPropertyContainer();
	Set<String> getFullPropertySet(final String propertyView);
	Set<String> getPropertyKeys(final String propertyView);
	long getSourceTransactionId();
	<T> Object setProperty(final String key, T value) throws FrameworkException;
	<T> Object setProperty(final String key, T value, final boolean isCreation) throws FrameworkException;
	void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException;
	void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException;

	boolean isNode();
	boolean isRelationship();

	<V> V getProperty(final String propertyKey);
	<V> V getProperty(final String propertyKey, final Predicate<GraphObject> filter);
	void removeProperty(final String key) throws FrameworkException;

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
	void addToIndex();

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
	String getPropertyWithVariableReplacement(final ActionContext renderContext, final String key) throws FrameworkException;
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
