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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.web.common.RenderContext;

import java.util.Date;

public interface NodeTrait {

	NodeInterface getWrappedNode();
	SecurityContext getSecurityContext();

	String getUuid();
	String getType();
	String getName();

	void setName(final String name) throws FrameworkException;

	boolean isVisibleToPublicUsers();
	boolean isVisibleToAuthenticatedUsers();

	Date getCreatedDate();
	Date getLastModifiedDate();

	void setVisibleToAuthenticatedUsers(final boolean visible) throws FrameworkException;
	void setVisibleToPublicUsers(final boolean visible) throws FrameworkException;

	default String getPropertyWithVariableReplacement(final RenderContext renderContext, final String key) throws FrameworkException {

		final NodeInterface node              = getWrappedNode();
		final Traits traits                   = node.getTraits();
		final PropertyKey<String> propertyKey = traits.key(key);

		return node.getPropertyWithVariableReplacement(renderContext, propertyKey);
	}

	default Traits getTraits() {
		return getWrappedNode().getTraits();
	}

	default <T extends NodeTrait> T as(final Class<T> type) {
		return getWrappedNode().as(type);
	}

	default boolean is(final String type) {
		return getTraits().contains(type);
	}

	default void unlockSystemPropertiesOnce() {
		getWrappedNode().unlockSystemPropertiesOnce();
	}

	default void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {
		setProperties(securityContext, properties, false);
	}

	default void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {
		getWrappedNode().setProperties(securityContext, properties, isCreation);
	}

	default <T> T getProperty(final PropertyKey<T> key) {
		return getWrappedNode().getProperty(key);
	}

	default <T> Object setProperty(final PropertyKey<T> key, final T value) throws FrameworkException {
		return getWrappedNode().setProperty(key, value);
	}
}
