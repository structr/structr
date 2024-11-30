package org.structr.core.traits.operations.propertycontainer;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.OverwritableOperation;

public interface RemoveProperty extends OverwritableOperation<RemoveProperty> {

	void removeProperty(final GraphObject graphObject, final PropertyKey key) throws FrameworkException;
}
