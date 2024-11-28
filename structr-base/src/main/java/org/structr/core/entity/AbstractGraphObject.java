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

import com.sun.xml.bind.v2.TODO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.graph.Identity;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.*;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract base class for all node entities in Structr.
 */
public abstract class AbstractGraphObject<T extends PropertyContainer> implements GraphObject<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractGraphObject.class);

	protected Map<String, Object> tmpStorageContainer  = null;
	protected boolean readOnlyPropertiesUnlocked       = false;
	protected boolean internalSystemPropertiesUnlocked = false;
	protected long sourceTransactionId                 = -1;
	protected String cachedUuid                        = null;
	protected SecurityContext securityContext          = null;
	protected Traits traits                            = null;
	protected Identity id                              = null;

	@Override
	public final void init(final SecurityContext securityContext, final T propertyContainer, final Class entityType, final long sourceTransactionId) {

		this.traits              = Traits.of(propertyContainer.getType());
		this.sourceTransactionId = sourceTransactionId;
		this.securityContext     = securityContext;
		this.id                  = propertyContainer.getId();
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
	public final void removeProperty(final PropertyKey key) throws FrameworkException {
		traits.removeProperty(key);
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
		return traits.getPropertyKeys(this, propertyView);
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
		return traits.getProperty(this, key, null);
	}

	@Override
	public final <T> T getProperty(final PropertyKey<T> key, final Predicate<GraphObject> predicate) {
		return traits.getProperty(this, key, predicate);
	}

	@Override
	public final <T> Object setProperty(final PropertyKey<T> key, final T value) throws FrameworkException {
		return traits.setProperty(this, key, value);
	}

	@Override
	public final <T> Object setProperty(final PropertyKey<T> key, final T value, final boolean isCreation) throws FrameworkException {
		return traits.setProperty(this, key, value, isCreation);
	}

	@Override
	public final void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {
		traits.setProperties(this, securityContext, properties);
	}

	@Override
	public final void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {
		traits.setProperties(this, securityContext, properties, isCreation);
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
}
