package org.structr.core.traits.operations.propertycontainer;

import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.OverwritableOperation;

import java.util.Set;

public interface GetPropertySet extends OverwritableOperation<GetPropertySet> {

	Set<PropertyKey> getPropertySet(final GraphObject graphObject, final String propertyView);
}
