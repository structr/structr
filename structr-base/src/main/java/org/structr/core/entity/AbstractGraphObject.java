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

		if (!traits.isGranted(this, Permission.write, securityContext, false)) {

			throw new FrameworkException(403, getModificationNotPermittedExceptionString(this, securityContext));
		}

		if (traits.getPropertyContainer(this) != null) {

			if (key == null) {

				logger.error("Tried to set property with null key (action was denied)");

				return;

			}

			// check for read-only properties
			if (key.isReadOnly()) {

				// allow super user to set read-only properties
				if (readOnlyPropertiesUnlocked || securityContext.isSuperUser()) {

					// permit write operation once and
					// lock read-only properties again
					internalSystemPropertiesUnlocked = false;
				} else {

					throw new FrameworkException(404, "Property ‛" + key.jsonName() + "‛ is read-only", new ReadOnlyPropertyToken(getType(), key.jsonName()));
				}

			}

			// check for system properties - cannot be overriden with super-user rights
			if (key.isSystemInternal()) {

				// allow super user to set read-only properties
				if (internalSystemPropertiesUnlocked) {

					// permit write operation once and
					// lock read-only properties again
					internalSystemPropertiesUnlocked = false;
				} else {

					throw new FrameworkException(404, "Property ‛" + key.jsonName() + "‛ is read-only", new InternalSystemPropertyToken(getType(), key.jsonName()));
				}

			}

			traits.getPropertyContainer(this).removeProperty(key.dbName());
		}

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

		// check for custom view in content-type field
		if (securityContext != null && securityContext.hasCustomView()) {

			final String view                = securityContext.isSuperUser() ? PropertyView.All : propertyView;
			final Set<PropertyKey> keys      = new LinkedHashSet<>(traits.getPropertySet(view));
			final Set<String> customView     = securityContext.getCustomView();

			for (Iterator<PropertyKey> it = keys.iterator(); it.hasNext();) {
				if (!customView.contains(it.next().jsonName())) {

					it.remove();
				}
			}

			return keys;
		}

		// this is the default if no application/json; properties=[...] content-type header is present on the request
		return traits.getPropertySet(propertyView);
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
		return getProperty(key, true, predicate);
	}

	private <T> T getProperty(final PropertyKey<T> key, boolean applyConverter, final Predicate<GraphObject> predicate) {

		// early null check, this should not happen...
		if (key == null || key.dbName() == null) {
			return null;
		}

		return key.getProperty(securityContext, this, applyConverter, predicate);
	}





	TODO: implement PropertyContainerTraitImplementation with methods from AbstractGraphObject!















	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		/**
		 * Fixme: implement by trait
		 *
		 */



		boolean valid = true;

		// the following two checks can be omitted in release 2.4 when Neo4j uniqueness constraints are live
		valid &= ValidationHelper.isValidStringNotBlank(this, id, errorBuffer);

		if (securityContext != null && securityContext.uuidWasSetManually()) {
			valid &= ValidationHelper.isValidGloballyUniqueProperty(this, id, errorBuffer);
		}

		valid &= ValidationHelper.isValidUuid(this, id, errorBuffer);
		valid &= ValidationHelper.isValidStringNotBlank(this, type, errorBuffer);

		return valid;

	}

	@Override
	public final <T> Object setProperty(final PropertyKey<T> key, final T value) throws FrameworkException {
		return traits.setProperty(this, key, value);
		return setProperty(key, value, false);
	}

	@Override
	public final <T> Object setProperty(final PropertyKey<T> key, final T value, final boolean isCreation) throws FrameworkException {
		return traits.setProperty(this, key, value, isCreation);


		/**
		 * FIXME: all these methods must be implemented by trait(s)!
		 *
		 */

		// clear function property cache in security context since we are about to invalidate past results
		if (securityContext != null) {
			securityContext.getContextStore().clearFunctionPropertyCache();
		}

		// allow setting of ID without permissions
		if (!key.equals(traits.key("id"))) {

			if (!isGranted(Permission.write, securityContext, isCreation)) {

				internalSystemPropertiesUnlocked = false;
				readOnlyPropertiesUnlocked       = false;

				throw new FrameworkException(403, getModificationNotPermittedExceptionString(this, securityContext));
			}
		}

		try {

			// no need to check previous value when creating a node
			T oldValue = isCreation ? null : getProperty(key);

			// no old value exists  OR  old value exists and is NOT equal => set property
			if (isCreation || ((oldValue == null) && (value != null)) || ((oldValue != null) && (!oldValue.equals(value)) || (key instanceof FunctionProperty)) ) {

				return setPropertyInternal(key, value);
			}

		} finally {

			internalSystemPropertiesUnlocked = false;
			readOnlyPropertiesUnlocked       = false;
		}

		return null;
	}

	@Override
	public final void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {
		traits.setProperties(this, securityContext, properties);
	}

	@Override
	public final void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {
		traits.setProperties(this, securityContext, properties, isCreation);

		if (!isGranted(Permission.write, securityContext, isCreation)) {

			internalSystemPropertiesUnlocked = false;
			readOnlyPropertiesUnlocked       = false;

			throw new FrameworkException(403, getModificationNotPermittedExceptionString(this, securityContext));
		}

		for (final PropertyKey key : properties.keySet()) {

			final Object oldValue = isCreation ? null : getProperty(key);
			final Object value    = properties.get(key);

			// no old value exists  OR  old value exists and is NOT equal => set property
			if (isCreation || ((oldValue == null) && (value != null)) || ((oldValue != null) && (!Objects.deepEquals(oldValue, value)) || (key instanceof FunctionProperty)) ) {

				if (!key.equals(traits.key("id"))) {

					// check for system properties
					if (key.isSystemInternal() && !internalSystemPropertiesUnlocked) {

						throw new FrameworkException(422, "Property ‛" + key.jsonName() + "‛ is an internal system property", new InternalSystemPropertyToken(getClass().getSimpleName(), key.jsonName()));
					}

					// check for read-only properties
					if ((key.isReadOnly() || key.isWriteOnce()) && !readOnlyPropertiesUnlocked && !securityContext.isSuperUser()) {

						throw new FrameworkException(422, "Property ‛" + key.jsonName() + "‛ is read-only", new ReadOnlyPropertyToken(getClass().getSimpleName(), key.jsonName()));
					}
				}
			}
		}

		GraphObject.super.setPropertiesInternal(securityContext, properties, isCreation);
	}

	private <T> Object setPropertyInternal(final PropertyKey<T> key, final T value) throws FrameworkException {

		if (key == null) {

			logger.error("Tried to set property with null key (action was denied)");

			throw new FrameworkException(422, "Tried to set property with null key (action was denied)", new NullArgumentToken(getClass().getSimpleName(), "base"));

		}

		try {

			// check for system properties
			if (key.isSystemInternal() && !internalSystemPropertiesUnlocked) {

				throw new FrameworkException(422, "Property ‛" + key.jsonName() + "‛ is an internal system property", new InternalSystemPropertyToken(getClass().getSimpleName(), key.jsonName()));
			}

			// check for read-only properties
			if ((key.isReadOnly() || key.isWriteOnce()) && !readOnlyPropertiesUnlocked && !securityContext.isSuperUser()) {

				throw new FrameworkException(422, "Property ‛" + key.jsonName() + "‛ is read-only", new ReadOnlyPropertyToken(getClass().getSimpleName(), key.jsonName()));
			}

			return key.setProperty(securityContext, this, value);

		} finally {

			// unconditionally lock read-only properties after every write (attempt) to avoid security problems
			// since we made "unlock_readonly_properties_once" available through scripting
			internalSystemPropertiesUnlocked = false;
			readOnlyPropertiesUnlocked       = false;
		}

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

	protected String getCurrentUserString (final SecurityContext ctx) {

		final PrincipalInterface currentUser = ctx.getUser(false);
		String userString = "";

		if (currentUser == null) {
			userString = (ctx.isSuperUser() ? "superuser" : "anonymous");
		} else {
			userString = currentUser.getType() + "(" + currentUser.getUuid() + ")";
		}

		return userString;
	}

	protected String getAccessControlNotPermittedExceptionString(final String action, final Set<Permission> permissions, PrincipalInterface principal, final SecurityContext ctx) {

		final String userString       = getCurrentUserString(ctx);
		final String thisNodeString   = this.getType()      + "(" + this.getUuid()      + ")";
		final String principalString  = principal.getType() + "(" + principal.getUuid() + ")";
		final String permissionString = permissions.stream().map(p -> p.name()).collect(Collectors.joining(", "));

		return "Access control not permitted! " + userString + " can not " + action + " rights (" + permissionString + ") for " + principalString + " to node " + thisNodeString;
	}

	protected String getModificationNotPermittedExceptionString(final GraphObject obj, final SecurityContext ctx) {

		final String userString     = getCurrentUserString(ctx);
		final String thisNodeString = obj.getType() + "(" + obj.getUuid()      + ")";

		return "Modification of node " + thisNodeString + " by " + userString + "not permitted.";
	}
}
