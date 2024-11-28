package org.structr.core.traits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.UnknownClientException;
import org.structr.api.UnknownDatabaseException;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.*;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.graph.CreationContainer;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.TypeProperty;

import java.util.*;
import java.util.stream.Collectors;

public class PropertyContainerTraitImplementation implements PropertyContainerTrait {

	private static final Logger logger = LoggerFactory.getLogger(PropertyContainerTraitImplementation.class);

	@Override
	public PropertyContainer getPropertyContainer(final GraphObject graphObject) {
		return graphObject.getPropertyContainer();
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
	public final Set<PropertyKey> getPropertyKeys(final GraphObject object,  final String propertyView) {

		final SecurityContext securityContext = object.getSecurityContext();
		final Traits traits                   = object.getTraits();

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

	@Override
	public <V> V getProperty(final GraphObject graphObject, final PropertyKey<V> propertyKey) {
		return null;
	}

	@Override
	public <V> V getProperty(final GraphObject graphObject, final PropertyKey<V> propertyKey, final Predicate<GraphObject> filter) {
		return null;
	}

	@Override
	public <T> Object setProperty(final GraphObject graphObject, final PropertyKey<T> key, final T value) throws FrameworkException {
		return null;
	}

	@Override
	public <T> Object setProperty(final GraphObject graphObject, final PropertyKey<T> key, final T value, boolean isCreation) throws FrameworkException {

		final SecurityContext securityContext = graphObject.getSecurityContext();
		final Traits traits                   = graphObject.getTraits();

		// clear function property cache in security context since we are about to invalidate past results
		if (securityContext != null) {
			securityContext.getContextStore().clearFunctionPropertyCache();
		}

		// allow setting of ID without permissions
		if (!key.equals(traits.key("id"))) {

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
			if (isCreation || ((oldValue == null) && (value != null)) || ((oldValue != null) && (!oldValue.equals(value)) || (key instanceof FunctionProperty)) ) {

				return setPropertyInternal(graphObject, key, value);
			}

		} finally {

			graphObject.lockSystemProperties();
			graphObject.lockReadOnlyProperties();
		}

		return null;
	}

	@Override
	public void setProperties(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {
		setProperties(graphObject, securityContext, properties, false);
	}

	@Override
	public void setProperties(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties, boolean isCreation) throws FrameworkException {

		if (!graphObject.isGranted(Permission.write, securityContext, isCreation)) {

			graphObject.lockSystemProperties();
			graphObject.lockReadOnlyProperties();

			throw new FrameworkException(403, getModificationNotPermittedExceptionString(graphObject, securityContext));
		}

		final Traits traits = graphObject.getTraits();

		for (final PropertyKey key : properties.keySet()) {

			final Object oldValue = isCreation ? null : graphObject.getProperty(key);
			final Object value    = properties.get(key);

			// no old value exists  OR  old value exists and is NOT equal => set property
			if (isCreation || ((oldValue == null) && (value != null)) || ((oldValue != null) && (!Objects.deepEquals(oldValue, value)) || (key instanceof FunctionProperty)) ) {

				if (!key.equals(traits.key("id"))) {

					// check for system properties
					if (key.isSystemInternal() && !graphObject.systemPropertiesUnlocked()) {

						throw new FrameworkException(422, "Property ‛" + key.jsonName() + "‛ is an internal system property", new InternalSystemPropertyToken(getClass().getSimpleName(), key.jsonName()));
					}

					// check for read-only properties
					if ((key.isReadOnly() || key.isWriteOnce()) && !graphObject.readOnlyPropertiesUnlocked() && !securityContext.isSuperUser()) {

						throw new FrameworkException(422, "Property ‛" + key.jsonName() + "‛ is read-only", new ReadOnlyPropertyToken(getClass().getSimpleName(), key.jsonName()));
					}
				}
			}
		}

		setPropertiesInternal(graphObject, securityContext, properties, isCreation);
	}

	@Override
	public void removeProperty(final GraphObject graphObject, final PropertyKey key) throws FrameworkException {

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

	private <T> Object setPropertyInternal(final GraphObject graphObject, final PropertyKey<T> key, final T value) throws FrameworkException {

		final SecurityContext securityContext = graphObject.getSecurityContext();

		if (key == null) {

			logger.error("Tried to set property with null key (action was denied)");

			throw new FrameworkException(422, "Tried to set property with null key (action was denied)", new NullArgumentToken(getClass().getSimpleName(), "base"));

		}

		try {

			// check for system properties
			if (key.isSystemInternal() && !graphObject.systemPropertiesUnlocked()) {

				throw new FrameworkException(422, "Property ‛" + key.jsonName() + "‛ is an internal system property", new InternalSystemPropertyToken(getClass().getSimpleName(), key.jsonName()));
			}

			// check for read-only properties
			if ((key.isReadOnly() || key.isWriteOnce()) && !graphObject.readOnlyPropertiesUnlocked() && !securityContext.isSuperUser()) {

				throw new FrameworkException(422, "Property ‛" + key.jsonName() + "‛ is read-only", new ReadOnlyPropertyToken(getClass().getSimpleName(), key.jsonName()));
			}

			return key.setProperty(securityContext, graphObject, value);

		} finally {

			// unconditionally lock read-only properties after every write (attempt) to avoid security problems
			// since we made "unlock_readonly_properties_once" available through scripting
			graphObject.lockSystemProperties();
			graphObject.lockReadOnlyProperties();
		}

	}

	private void setPropertiesInternal(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {

		final CreationContainer container = new CreationContainer(graphObject);
		final Traits traits               = graphObject.getTraits();

		boolean atLeastOnePropertyChanged = false;

		for (final Map.Entry<PropertyKey, Object> attr : properties.entrySet()) {

			final PropertyKey key = attr.getKey();
			final Object value    = attr.getValue();

			if (value != null && key.isPropertyTypeIndexable() && key.relatedType() == null) {

				final Object oldValue = graphObject.getProperty(key);
				if (!Objects.deepEquals(value, oldValue)) {

					atLeastOnePropertyChanged = true;

					// bulk set possible, store in container
					key.setProperty(securityContext, container, value);

					if (graphObject.isNode()) {

						if (!key.isUnvalidated()) {

							TransactionCommand.nodeModified(securityContext.getCachedUser(), (AbstractNode)graphObject, key, oldValue, value);
						}

						if (key instanceof TypeProperty) {

							if (graphObject instanceof NodeInterface node) {


								TypeProperty.updateLabels(StructrApp.getInstance().getDatabaseService(), node, traits, true);
							}
						}

					} else if (graphObject.isRelationship()) {

						if (!key.isUnvalidated()) {

							TransactionCommand.relationshipModified(securityContext.getCachedUser(), (AbstractRelationship)graphObject, key, oldValue, value);
						}
					}
				}

			} else {

				// bulk set NOT possible, set on entity
				if (key.isSystemInternal()) {
					graphObject.unlockSystemPropertiesOnce();
				}

				graphObject.setProperty(key, value, isCreation);
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

	/*
	private void indexPassiveProperties() {

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

		for (final Iterator<Map.Entry<PropertyKey, Object>> iterator = src.entrySet().iterator(); iterator.hasNext();) {

			final Map.Entry<PropertyKey, Object> attr = iterator.next();
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
	*/

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

	protected String getAccessControlNotPermittedExceptionString(final GraphObject graphObject, final String action, final Set<Permission> permissions, PrincipalInterface principal, final SecurityContext ctx) {

		final String userString       = getCurrentUserString(ctx);
		final String thisNodeString   = graphObject.getType()      + "(" + graphObject.getUuid()      + ")";
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
