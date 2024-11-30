package org.structr.core.traits.operations.propertycontainer;

import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.OverwritableOperation;

import java.util.Set;

public abstract class GetPropertyKeys extends OverwritableOperation<GetPropertyKeys> {

	public abstract Set<PropertyKey> getPropertyKeys(final GraphObject graphObject, final String propertyView);
}
