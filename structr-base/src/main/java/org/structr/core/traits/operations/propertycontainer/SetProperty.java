package org.structr.core.traits.operations.propertycontainer;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.FrameworkMethod;

public abstract class SetProperty extends FrameworkMethod<SetProperty> {

	public abstract <T> Object setProperty(final GraphObject graphObject, final PropertyKey<T> key, T value, final boolean isCreation) throws FrameworkException;
}
