package org.structr.core.traits.operations.propertycontainer;

import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.FrameworkMethod;

import java.util.Set;

public abstract class GetPropertySet extends FrameworkMethod<GetPropertySet> {

	public abstract Set<PropertyKey> getPropertySet(final GraphObject graphObject, final String propertyView);
}
