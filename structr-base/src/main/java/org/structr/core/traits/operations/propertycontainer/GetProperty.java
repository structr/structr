package org.structr.core.traits.operations.propertycontainer;

import org.structr.api.Predicate;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.OverwritableOperation;

public interface GetProperty extends OverwritableOperation<GetProperty> {

	<V> V getProperty(final GraphObject graphObject, final PropertyKey<V> propertyKey, final Predicate<GraphObject> filter);
}
