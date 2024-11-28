package org.structr.core.traits;

import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.*;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class PropertyContainerTraitImplementation implements PropertyContainerTrait {

	@Override
	public PropertyContainer getPropertyContainer(GraphObject graphObject) {
		return null;
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
	public <V> V getProperty(GraphObject graphObject, PropertyKey<V> propertyKey) {
		return null;
	}

	@Override
	public <V> V getProperty(GraphObject graphObject, PropertyKey<V> propertyKey, Predicate<GraphObject> filter) {
		return null;
	}

	@Override
	public <T> Object setProperty(GraphObject graphObject, PropertyKey<T> key, T value) throws FrameworkException {
		return null;
	}

	@Override
	public <T> Object setProperty(GraphObject graphObject, PropertyKey<T> key, T value, boolean isCreation) throws FrameworkException {
		return null;
	}

	@Override
	public void setProperties(GraphObject graphObject, SecurityContext securityContext, PropertyMap properties) throws FrameworkException {

	}

	@Override
	public void setProperties(GraphObject graphObject, SecurityContext securityContext, PropertyMap properties, boolean isCreation) throws FrameworkException {

	}

	@Override
	public void removeProperty(PropertyKey key) throws FrameworkException {

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
}
