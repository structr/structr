package org.structr.core.traits.operations.propertycontainer;

import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.FrameworkMethod;

import java.util.Set;

public abstract class GetPropertyKeys extends FrameworkMethod<GetPropertyKeys> {

	public abstract Set<PropertyKey> getPropertyKeys(final GraphObject graphObject, final String propertyView);
}
