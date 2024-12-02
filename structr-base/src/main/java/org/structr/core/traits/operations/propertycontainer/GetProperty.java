package org.structr.core.traits.operations.propertycontainer;

import org.structr.api.Predicate;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.FrameworkMethod;

public abstract class GetProperty extends FrameworkMethod<GetProperty> {

	public abstract <V> V getProperty(final GraphObject graphObject, final PropertyKey<V> propertyKey, final Predicate<GraphObject> filter);
}
