package org.structr.core.traits;

import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

import java.util.Set;

public class PropertyContainerTraitHandler implements PropertyContainerTrait {

	private final PropertyContainerTraitImplementation defaultHandler;

	public PropertyContainerTraitHandler(final Traits traits) {
		this.defaultHandler = new PropertyContainerTraitImplementation(traits);
	}

	@Override
	public PropertyContainer getPropertyContainer(final GraphObject graphObject) {
		return defaultHandler.getPropertyContainer(graphObject);
	}

	@Override
	public Set<PropertyKey> getPropertySet(GraphObject graphObject, String propertyView) {
		return defaultHandler.getPropertySet(graphObject, propertyView);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys(final GraphObject graphObject, final String propertyView) {
		return defaultHandler.getPropertyKeys(graphObject, propertyView);
	}

	@Override
	public <V> V getProperty(final GraphObject graphObject, final PropertyKey<V> propertyKey) {
		return defaultHandler.getProperty(graphObject, propertyKey);
	}

	@Override
	public <V> V getProperty(final GraphObject graphObject, final PropertyKey<V> propertyKey, final Predicate<GraphObject> filter) {
		return defaultHandler.getProperty(graphObject, propertyKey, filter);
	}

	@Override
	public <T> Object setProperty(final GraphObject graphObject, final PropertyKey<T> key, final T value) throws FrameworkException {
		return defaultHandler.setProperty(graphObject, key, value);
	}

	@Override
	public <T> Object setProperty(final GraphObject graphObject, final PropertyKey<T> key, final T value, final boolean isCreation) throws FrameworkException {
		return defaultHandler.setProperty(graphObject, key, value, isCreation);
	}

	@Override
	public void setProperties(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {
		defaultHandler.setProperties(graphObject, securityContext, properties);
	}

	@Override
	public void setProperties(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {
		defaultHandler.setProperties(graphObject, securityContext, properties, isCreation);
	}

	@Override
	public void removeProperty(final GraphObject graphObject, final PropertyKey key) throws FrameworkException {
		defaultHandler.removeProperty(graphObject, key);
	}
}
