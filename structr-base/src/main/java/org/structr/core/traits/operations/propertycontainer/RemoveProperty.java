package org.structr.core.traits.operations.propertycontainer;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.OverwritableOperation;

public abstract class RemoveProperty extends OverwritableOperation<RemoveProperty> {

	public abstract void removeProperty(final GraphObject graphObject, final PropertyKey key) throws FrameworkException;
}
