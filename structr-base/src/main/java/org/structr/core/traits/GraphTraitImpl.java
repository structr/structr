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

import org.apache.poi.ss.formula.functions.T;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.Predicate;
import org.structr.api.UnknownClientException;
import org.structr.api.UnknownDatabaseException;
import org.structr.api.graph.Identity;
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.*;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.*;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.*;
import java.util.stream.Collectors;

public abstract class GraphTraitImpl implements GraphTrait {

	private static final Logger logger = LoggerFactory.getLogger(GraphTrait.class);

	protected final PropertyKey<String> typeProperty;
	protected final PropertyKey<String> idProperty;
	protected boolean internalSystemPropertiesUnlocked;
	protected boolean readOnlyPropertiesUnlocked;
	protected SecurityContext securityContext;
	protected PropertyContainer obj;
	protected Traits traits;

	static {

		final Trait<GraphTrait> trait = Trait.create(GraphTrait.class, n -> new GraphTraitImpl(n) {});

		trait.registerProperty(new StringProperty("base").partOfBuiltInSchema());
		trait.registerProperty(new TypeProperty().partOfBuiltInSchema().category(SYSTEM_CATEGORY));
		trait.registerProperty(new UuidProperty().partOfBuiltInSchema().category(SYSTEM_CATEGORY));
		trait.registerProperty(new ISO8601DateProperty("createdDate").readOnly().systemInternal().indexed().unvalidated().writeOnce().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly());
		trait.registerProperty(new StringProperty("createdBy").readOnly().writeOnce().unvalidated().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly());
		trait.registerProperty(new ISO8601DateProperty("lastModifiedDate").readOnly().systemInternal().passivelyIndexed().unvalidated().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly());
		trait.registerProperty(new StringProperty("lastModifiedBy").readOnly().systemInternal().unvalidated().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly());
		trait.registerProperty(new BooleanProperty("visibleToPublicUsers").passivelyIndexed().category(VISIBILITY_CATEGORY).partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly());
	}

	protected GraphTraitImpl(final PropertyContainer obj) {

		this.obj = obj;

		traits = Traits.of(obj.getType());

		typeProperty = traits.get("GraphTrait").key("type");
		idProperty   = traits.get("GraphTrait").key("id");

	}

	protected <T extends GraphTrait> T as(final Class<T> type) {

		final Trait<T> trait = Trait.of(type);
		if (trait != null) {

			return trait.getImplementation(obj);
		}

		// should throw exception here
		return null;
	}

	protected <T> PropertyKey<T> key(final String name) {
		return traits.key(name);
	}

	@Override
	public Traits getTraits() {
		return traits;
	}

	@Override
	public String getType() {
		return obj.getType();
	}

	@Override
	public void setSecurityContext(final SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	@Override
	public SecurityContext getSecurityContext() {
		return securityContext;
	}

	@Override
	public PropertyContainer getPropertyContainer() {
		return obj;
	}

	@Override
	public String getUuid() {

		// all traits must have the GraphTrait trait
		return getProperty(idProperty);
	}

	@Override
	public Identity getIdentity() {
		return obj.getId();
	}

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = true;

		// the following two checks can be omitted in release 2.4 when Neo4j uniqueness constraints are live
		valid &= ValidationHelper.isValidStringNotBlank(this, idProperty, errorBuffer);

		if (securityContext != null && securityContext.uuidWasSetManually()) {
			valid &= ValidationHelper.isValidGloballyUniqueProperty(this, idProperty, errorBuffer);
		}

		valid &= ValidationHelper.isValidUuid(this, idProperty, errorBuffer);
		valid &= ValidationHelper.isValidStringNotBlank(this, typeProperty, errorBuffer);

		return valid;

	}

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
	}

	@Override
	public void onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
	}

	@Override
	public void afterCreation(final SecurityContext securityContext) throws FrameworkException {
	}

	@Override
	public void afterModification(final SecurityContext securityContext) throws FrameworkException {
	}

	@Override
	public void afterDeletion(final SecurityContext securityContext, final PropertyMap properties) {
	}

	@Override
	public void ownerModified(final SecurityContext securityContext) {
	}

	@Override
	public void securityModified(final SecurityContext securityContext) {
	}

	@Override
	public void locationModified(final SecurityContext securityContext) {
	}

	@Override
	public void propagatedModification(final SecurityContext securityContext) {
	}

	@Override
	public String getPropertyWithVariableReplacement(final ActionContext renderContext, final PropertyKey<String> key) throws FrameworkException {
		return "";
	}

	@Override
	public Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, final EvaluationHints hints, final int row, final int column) throws FrameworkException {
		return null;
	}

	@Override
	public List<GraphTrait> getSyncData() throws FrameworkException {
		return List.of();
	}

	@Override
	public boolean isNode() {
		return false;
	}

	@Override
	public boolean isRelationship() {
		return false;
	}

	@Override
	public NodeTrait getSyncNode() {
		return null;
	}

	@Override
	public RelationshipTrait getSyncRelationship() {
		return null;
	}

	@Override
	public boolean changelogEnabled() {
		return false;
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
	public <T> T getProperty(final PropertyKey<T> key) {
		return getProperty(key, null);
	}

	@Override
	public <T> T getProperty(final PropertyKey<T> key, final Predicate<GraphTrait> predicate) {
		return getProperty(key, true, predicate);
	}

	@Override
	public void unlockSystemPropertiesOnce() {
		internalSystemPropertiesUnlocked = true;
	}

	@Override
	public void unlockReadOnlyPropertiesOnce() {
		readOnlyPropertiesUnlocked = true;
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
	public Set<PropertyKey> getPropertyKeys(final String propertyView) {

		/*
		// check for custom view in content-type field
		if (securityContext != null && securityContext.hasCustomView()) {

			final String view            = securityContext.isSuperUser() ? PropertyView.All : propertyView;
			final Set<PropertyKey> keys  = new LinkedHashSet<>(StructrApp.getConfiguration().getPropertySet(traits, view));
			final Set<String> customView = securityContext.getCustomView();

			for (Iterator<PropertyKey> it = keys.iterator(); it.hasNext();) {
				if (!customView.contains(it.next().jsonName())) {

					it.remove();
				}
			}

			return keys;
		}

		// this is the default if no application/json; properties=[...] content-type header is present on the request
		return StructrApp.getConfiguration().getPropertySet(traits, propertyView);
		*/

		// this needs to be implemented based on traits
		return null;
	}

	@Override
	public final void removeProperty(final PropertyKey key) throws FrameworkException {

		if (!Trait.of(AccessControllable.class).getImplementation(obj).isGranted(Permission.write, securityContext)) {

			throw new FrameworkException(403, getModificationNotPermittedExceptionString(this, securityContext));
		}

		if (getPropertyContainer() != null) {

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

			getPropertyContainer().removeProperty(key.dbName());
		}

	}

	private <T> T getProperty(final PropertyKey<T> key, boolean applyConverter, final Predicate<GraphTrait> predicate) {

		// early null check, this should not happen...
		if (key == null || key.dbName() == null) {
			return null;
		}

		return key.getProperty(securityContext, this, applyConverter, predicate);
	}


	/**
	 * Set a property in database backend. This method needs to be wrappend
	 * into a StructrTransaction, otherwise Neo4j will throw a
	 * NotInTransactionException! Set property only if value has changed.
	 *
	 * @param <T>
	 * @param key
	 * @throws org.structr.common.error.FrameworkException
	 */
	@Override
	public <T> Object setProperty(final PropertyKey<T> key, final T value) throws FrameworkException {
		return setProperty(key, value, false);
	}

	@Override
	public <T> Object setProperty(final PropertyKey<T> key, final T value, final boolean isCreation) throws FrameworkException {

		// clear function property cache in security context since we are about to invalidate past results
		if (securityContext != null) {
			securityContext.getContextStore().clearFunctionPropertyCache();
		}

		// allow setting of ID without permissions
		if (!key.equals(idProperty)) {

			final AccessControllable accessControllable = this.as(AccessControllable.class);

			if (!accessControllable.isGranted(Permission.write, securityContext, isCreation)) {

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
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {
		setProperties(securityContext, properties, false);
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {

		if (!Trait.of(AccessControllable.class).getImplementation(obj).isGranted(Permission.write, securityContext, isCreation)) {

			internalSystemPropertiesUnlocked = false;
			readOnlyPropertiesUnlocked       = false;

			throw new FrameworkException(403, getModificationNotPermittedExceptionString(this, securityContext));
		}

		for (final PropertyKey key : properties.keySet()) {

			final Object oldValue = isCreation ? null : getProperty(key);
			final Object value    = properties.get(key);

			// no old value exists  OR  old value exists and is NOT equal => set property
			if (isCreation || ((oldValue == null) && (value != null)) || ((oldValue != null) && (!Objects.deepEquals(oldValue, value)) || (key instanceof FunctionProperty)) ) {

				if (!key.equals(idProperty)) {

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

		setPropertiesInternal(securityContext, properties, isCreation);
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
	public long getSourceTransactionId() {
		return 0;
	}

	void indexPassiveProperties() {

		final Set<PropertyKey> passiveIndexingKeys = new LinkedHashSet<>();

		for (PropertyKey key : StructrApp.getConfiguration().getPropertySet(getTraits(), PropertyView.All)) {

			if (key.isIndexed() && (key.isPassivelyIndexed() || key.isIndexedWhenEmpty())) {

				passiveIndexingKeys.add(key);
			}
		}

		addToIndex(passiveIndexingKeys);
	}

	public void addToIndex() {

		final Set<PropertyKey> indexKeys = new LinkedHashSet<>();

		for (PropertyKey key : StructrApp.getConfiguration().getPropertySet(getTraits(), PropertyView.All)) {

			if (key.isIndexed()) {

				indexKeys.add(key);
			}
		}

		addToIndex(indexKeys);
	}

	public void addToIndex(final Set<PropertyKey> indexKeys) {

		final Map<String, Object> values = new LinkedHashMap<>();

		for (PropertyKey key : indexKeys) {

			final PropertyConverter converter = key.databaseConverter(securityContext, this);

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
			obj.setProperties(values);

		} catch (UnknownClientException | UnknownDatabaseException e) {

			final Logger logger = LoggerFactory.getLogger(GraphObject.class);
			logger.warn("Unable to index properties of {} with UUID {}: {}", getType(), getUuid(), e.getMessage());
			logger.warn("Properties: {}", values);
		}
	}

	private static String getCurrentUserString (final SecurityContext ctx) {

		final Principal currentUser = ctx.getUser(false);
		String userString = "";

		if (currentUser == null) {
			userString = (ctx.isSuperUser() ? "superuser" : "anonymous");
		} else {
			userString = currentUser.getType() + "(" + currentUser.getUuid() + ")";
		}

		return userString;
	}

	protected String getAccessControlNotPermittedExceptionString(final String action, final Set<Permission> permissions, Principal principal, final SecurityContext ctx) {

		final String userString       = getCurrentUserString(ctx);
		final String thisNodeString   = this.getProperty(typeProperty)      + "(" + this.getProperty(idProperty)      + ")";
		final String principalString  = principal.getType() + "(" + principal.getUuid() + ")";
		final String permissionString = permissions.stream().map(p -> p.name()).collect(Collectors.joining(", "));

		return "Access control not permitted! " + userString + " can not " + action + " rights (" + permissionString + ") for " + principalString + " to node " + thisNodeString;
	}

	public static String getModificationNotPermittedExceptionString(final GraphTrait obj, final SecurityContext ctx) {

		final PropertyKey<String> typeProperty = Traits.of("GraphTrait").key("type");
		final PropertyKey<String> idProperty   = Traits.of("GraphTrait").key("id");

		final String userString     = getCurrentUserString(ctx);
		final String thisNodeString = obj.getProperty(typeProperty) + "(" + obj.getProperty(idProperty)      + ")";

		return "Modification of node " + thisNodeString + " by " + userString + "not permitted.";
	}
}
