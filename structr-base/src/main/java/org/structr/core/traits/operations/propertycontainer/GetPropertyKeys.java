package org.structr.core.traits.operations.propertycontainer;

import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.OverwritableOperation;

import java.util.Set;

public interface GetPropertyKeys extends OverwritableOperation<GetPropertyKeys> {

	Set<PropertyKey> getPropertyKeys(final GraphObject graphObject, final String propertyView);
}
