package org.structr.core.traits.operations.propertycontainer;

import org.structr.api.graph.PropertyContainer;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.FrameworkMethod;

public abstract class GetPropertyContainer extends FrameworkMethod<GetPropertyContainer> {

	public abstract PropertyContainer getPropertyContainer(final GraphObject graphObject);
}
