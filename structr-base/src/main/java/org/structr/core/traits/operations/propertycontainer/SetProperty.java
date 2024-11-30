package org.structr.core.traits.operations.propertycontainer;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.OverwritableOperation;

public abstract class SetProperty extends OverwritableOperation<SetProperty> {

	public abstract <T> Object setProperty(final GraphObject graphObject, final PropertyKey<T> key, T value, final boolean isCreation) throws FrameworkException;
}
