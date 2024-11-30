package org.structr.core.traits.operations.propertycontainer;

import org.structr.api.graph.PropertyContainer;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.OverwritableOperation;

public abstract class GetPropertyContainer extends OverwritableOperation<GetPropertyContainer> {

	public abstract PropertyContainer getPropertyContainer(final GraphObject graphObject);
}
