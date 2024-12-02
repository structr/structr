package org.structr.core.traits.operations.propertycontainer;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.FrameworkMethod;

public abstract class RemoveProperty extends FrameworkMethod<RemoveProperty> {

	public abstract void removeProperty(final GraphObject graphObject, final PropertyKey key) throws FrameworkException;
}
