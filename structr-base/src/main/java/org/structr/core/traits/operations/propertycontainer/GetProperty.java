package org.structr.core.traits.operations.propertycontainer;

import org.structr.api.Predicate;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.OverwritableOperation;

public abstract class GetProperty extends OverwritableOperation<GetProperty> {

	public abstract <V> V getProperty(final GraphObject graphObject, final PropertyKey<V> propertyKey, final Predicate<GraphObject> filter);
}
