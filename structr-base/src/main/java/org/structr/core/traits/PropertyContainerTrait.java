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

import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

import java.util.Set;

public interface PropertyContainerTrait {

	PropertyContainer getPropertyContainer(final GraphObject graphObject);
	Set<PropertyKey> getPropertyKeys(final GraphObject graphObject, final String propertyView);

	<V> V getProperty(final GraphObject graphObject, final PropertyKey<V> propertyKey);
	<V> V getProperty(final GraphObject graphObject, PropertyKey<V> propertyKey, final Predicate<GraphObject> filter);

	<T> Object setProperty(final GraphObject graphObject, final PropertyKey<T> key, T value) throws FrameworkException;
	<T> Object setProperty(final GraphObject graphObject, final PropertyKey<T> key, T value, final boolean isCreation) throws FrameworkException;
	void setProperties(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException;
	void setProperties(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException;

	void removeProperty(final PropertyKey key) throws FrameworkException;
}
