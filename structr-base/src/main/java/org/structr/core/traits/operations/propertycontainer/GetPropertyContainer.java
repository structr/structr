package org.structr.core.traits.operations.propertycontainer;

import org.structr.api.graph.PropertyContainer;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.OverwritableOperation;

public interface GetPropertyContainer extends OverwritableOperation<GetPropertyContainer> {

	PropertyContainer getPropertyContainer(final GraphObject graphObject);
}
