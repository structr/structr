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
package org.structr.core.traits.definitions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.UnknownClientException;
import org.structr.api.UnknownDatabaseException;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InternalSystemPropertyToken;
import org.structr.common.error.NullArgumentToken;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.graph.CreationContainer;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.TypeProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.propertycontainer.*;

import java.util.*;

public final class PropertyContainerTraitDefinition extends AbstractNodeTraitDefinition {

	private static final Logger logger = LoggerFactory.getLogger(PropertyContainerTraitDefinition.class);

	public PropertyContainerTraitDefinition() {
		super(StructrTraits.PROPERTY_CONTAINER);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			GetPropertyContainer.class,
			new GetPropertyContainer() {

				@Override
				public PropertyContainer getPropertyContainer(final GraphObject graphObject) {
					return graphObject.getPropertyContainer();
				}
			},

			GetPropertySet.class,
			new GetPropertySet() {

				@Override
				public Set<PropertyKey> getAllPropertyKeys(final GraphObject graphObject) {

					final Traits traits         = graphObject.getTraits();
					final Set<PropertyKey> keys = new LinkedHashSet<>();

					for (final PropertyKey k : traits.getAllPropertyKeys()) {

						keys.add(k);
					}

					return keys;
				}
			},

			GetPropertyKeys.class,
			new GetPropertyKeys() {

				@Override
				public Set<PropertyKey> getPropertyKeys(final GraphObject graphObject, final String propertyView) {

					final SecurityContext securityContext = graphObject.getSecurityContext();
					final Traits traits                   = graphObject.getTraits();

					// check for custom view in content-type field
					if (securityContext != null && securityContext.hasCustomView()) {

						final String view            = securityContext.isSuperUser() ? PropertyView.All : propertyView;
						final Set<PropertyKey> keys  = new LinkedHashSet<>(traits.getPropertyKeysForView(view));
						final Set<String> customView = securityContext.getCustomView();

						for (Iterator<PropertyKey> it = keys.iterator(); it.hasNext();) {

							if (!customView.contains(it.next().jsonName())) {

								it.remove();
							}
						}

						return keys;
					}

					// this is the default if no application/json; properties=[...] content-type header is present on the request
					return traits.getPropertyKeysForView(propertyView);
				}
			},

			GetProperty.class,
			new GetProperty() {

				@Override
				public <V> V getProperty(final GraphObject graphObject, final PropertyKey<V> key, final Predicate<GraphObject> filter) {

					// early null check, this should not happen...
					if (key == null) {
						return null;
					}
					
					return key.getProperty(graphObject.getSecurityContext(), graphObject, true, filter);
				}
			},

			SetProperty.class,
			new SetProperty() {

				@Override
				public <T> Object setProperty(final GraphObject graphObject, final PropertyKey<T> key, final T value, final boolean isCreation) throws FrameworkException {

					final SecurityContext securityContext = graphObject.getSecurityContext();

					// clear function property cache in security context since we are about to invalidate past results
					if (securityContext != null) {
						securityContext.getContextStore().clearFunctionPropertyCache();
					}

					// allow setting of ID without permissions
					if (!"id".equals(key.jsonName())) {

						if (!graphObject.isGranted(Permission.write, securityContext, isCreation)) {

							graphObject.lockSystemProperties();
							graphObject.lockReadOnlyProperties();

							throw new FrameworkException(403, getModificationNotPermittedExceptionString(graphObject, securityContext));
						}
					}

					try {

						// no need to check previous value when creating a node
						T oldValue = isCreation ? null : (T)graphObject.getProperty(key);

						// no old value exists  OR  old value exists and is NOT equal => set property
						if (isCreation || ((oldValue == null) && (value != null)) || ((oldValue != null) && (!oldValue.equals(value)) || key instanceof FunctionProperty)) {

							return setPropertyInternal(graphObject, key, value);
						}

					} finally {

						graphObject.lockSystemProperties();
						graphObject.lockReadOnlyProperties();
					}

					return null;
				}
			},

			SetProperties.class,
			new SetProperties() {

				@Override
				public void setProperties(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {

					if (!graphObject.isGranted(Permission.write, securityContext, isCreation)) {

						graphObject.lockSystemProperties();
						graphObject.lockReadOnlyProperties();

						throw new FrameworkException(403, getModificationNotPermittedExceptionString(graphObject, securityContext));
					}

					final Traits traits   = graphObject.getTraits();

					for (final PropertyKey key : properties.keySet()) {

						final Object oldValue = isCreation ? null : graphObject.getProperty(key);
						final Object value    = properties.get(key);

						// no old value exists  OR  old value exists and is NOT equal => set property
						if (isCreation || ((oldValue == null) && (value != null)) || ((oldValue != null) && (!Objects.deepEquals(oldValue, value)) || (key instanceof FunctionProperty)) ) {

							if (!key.equals(traits.key(GraphObjectTraitDefinition.ID_PROPERTY))) {

								// check for system properties
								if (key.isSystemInternal() && !graphObject.systemPropertiesUnlocked()) {

									throw new FrameworkException(422, "Property ‛" + key.jsonName() + "‛ is an internal system property", new InternalSystemPropertyToken(graphObject.getType(), key.jsonName()));
								}

								// check for read-only properties
								if ((key.isReadOnly() || key.isWriteOnce()) && !graphObject.readOnlyPropertiesUnlocked() && !securityContext.isSuperUser()) {

									throw new FrameworkException(422, "Property ‛" + key.jsonName() + "‛ is read-only", new ReadOnlyPropertyToken(graphObject.getType(), key.jsonName()));
								}
							}
						}
					}

					setPropertiesInternal(graphObject, securityContext, properties, isCreation);
				}
			},

			RemoveProperty.class,
			new RemoveProperty() {

				@Override
				public <T> void removeProperty(final GraphObject graphObject, final PropertyKey<T> key) throws FrameworkException {

					final SecurityContext securityContext = graphObject.getSecurityContext();

					if (!graphObject.isGranted(Permission.write, securityContext, false)) {

						throw new FrameworkException(403, getModificationNotPermittedExceptionString(graphObject, securityContext));
					}

					if (graphObject.getPropertyContainer() != null) {

						if (key == null) {

							logger.error("Tried to set property with null key (action was denied)");

							return;

						}

						// check for read-only properties
						if (key.isReadOnly()) {

							// allow super user to set read-only properties
							if (graphObject.readOnlyPropertiesUnlocked() || securityContext.isSuperUser()) {

								// permit write operation once and
								// lock read-only properties again
								graphObject.lockReadOnlyProperties();

							} else {

								throw new FrameworkException(404, "Property ‛" + key.jsonName() + "‛ is read-only", new ReadOnlyPropertyToken(graphObject.getType(), key.jsonName()));
							}

						}

						// check for system properties - cannot be overriden with super-user rights
						if (key.isSystemInternal()) {

							// allow super user to set read-only properties
							if (graphObject.systemPropertiesUnlocked()) {

								// permit write operation once and
								// lock read-only properties again
								graphObject.lockSystemProperties();

							} else {

								throw new FrameworkException(404, "Property ‛" + key.jsonName() + "‛ is read-only", new InternalSystemPropertyToken(graphObject.getType(), key.jsonName()));
							}

						}

						graphObject.getPropertyContainer().removeProperty(key.dbName());
					}
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<String, Set<String>> getViews() {
		return Map.of();
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	private static <T> Object setPropertyInternal(final GraphObject graphObject, final PropertyKey<T> key, final T value) throws FrameworkException {

		final SecurityContext securityContext = graphObject.getSecurityContext();
		final String type                     = graphObject.getType();

		if (key == null) {

			logger.error("Tried to set property with null key (action was denied)");

			throw new FrameworkException(422, "Tried to set property with null key (action was denied)", new NullArgumentToken(type, "base"));

		}

		try {

			// check for system properties
			if (key.isSystemInternal() && !graphObject.systemPropertiesUnlocked()) {

				throw new FrameworkException(422, "Property ‛" + key.jsonName() + "‛ is an internal system property", new InternalSystemPropertyToken(type, key.jsonName()));
			}

			// check for read-only properties
			if ((key.isReadOnly() || key.isWriteOnce()) && !graphObject.readOnlyPropertiesUnlocked() && !securityContext.isSuperUser()) {

				throw new FrameworkException(422, "Property ‛" + key.jsonName() + "‛ is read-only", new ReadOnlyPropertyToken(type, key.jsonName()));
			}

			return key.setProperty(securityContext, graphObject, value);

		} finally {

			// unconditionally lock read-only properties after every write (attempt) to avoid security problems
			// since we made "unlock_readonly_properties_once" available through scripting
			graphObject.lockSystemProperties();
			graphObject.lockReadOnlyProperties();
		}

	}

	private static void setPropertiesInternal(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {

		final CreationContainer container = new CreationContainer(graphObject);
		final Traits traits               = graphObject.getTraits();

		boolean atLeastOnePropertyChanged = false;

		for (final Map.Entry<PropertyKey, Object> attr : properties.entrySet()) {

			final PropertyKey propertyKey = attr.getKey();
			final Object value            = attr.getValue();

			if (value != null && propertyKey.isPropertyTypeIndexable() && propertyKey.relatedType() == null) {

				final Object oldValue = graphObject.getProperty(propertyKey);
				if (!Objects.deepEquals(value, oldValue)) {

					atLeastOnePropertyChanged = true;

					// bulk set possible, store in container
					propertyKey.setProperty(securityContext, container, value);

					if (graphObject.isNode()) {

						if (!propertyKey.isUnvalidated()) {

							TransactionCommand.nodeModified(securityContext.getCachedUser(), (NodeInterface)graphObject, propertyKey, oldValue, value);
						}

						if (propertyKey instanceof TypeProperty) {

							if (graphObject instanceof NodeInterface node) {


								TypeProperty.updateLabels(StructrApp.getInstance().getDatabaseService(), node, traits, true);
							}
						}

					} else if (graphObject.isRelationship()) {

						if (!propertyKey.isUnvalidated()) {

							TransactionCommand.relationshipModified(securityContext.getCachedUser(), (AbstractRelationship)graphObject, propertyKey, oldValue, value);
						}
					}
				}

			} else {

				// bulk set NOT possible, set on entity
				if (propertyKey.isSystemInternal()) {
					graphObject.unlockSystemPropertiesOnce();
				}

				graphObject.setProperty(propertyKey, value, isCreation);
			}
		}

		if (atLeastOnePropertyChanged) {

			try {

				// set primitive values directly for better performance
				graphObject.getPropertyContainer().setProperties(container.getData());

			} catch (UnknownClientException | UnknownDatabaseException e) {

				final Logger logger = LoggerFactory.getLogger(GraphObject.class);

				logger.warn("Unable to set properties of {} with UUID {}: {}", graphObject.getType(), graphObject.getUuid(), e.getMessage());
				logger.warn("Properties: {}", container.getData());
			}
		}
	}

	protected static String getCurrentUserString (final SecurityContext ctx) {

		final Principal currentUser = ctx.getUser(false);
		String userString = "";

		if (currentUser == null) {
			userString = (ctx.isSuperUser() ? "superuser" : "anonymous");
		} else {
			userString = currentUser.getType() + "(" + currentUser.getUuid() + ")";
		}

		return userString;
	}

	public static String getModificationNotPermittedExceptionString(final GraphObject obj, final SecurityContext ctx) {

		final String userString     = PropertyContainerTraitDefinition.getCurrentUserString(ctx);
		final String thisNodeString = obj.getType() + "(" + obj.getUuid()      + ")";

		return "Modification of node " + thisNodeString + " by " + userString + " not permitted.";
	}
}
