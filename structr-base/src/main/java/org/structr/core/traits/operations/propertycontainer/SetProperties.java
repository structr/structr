package org.structr.core.traits.operations.propertycontainer;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.operations.FrameworkMethod;

public abstract class SetProperties extends FrameworkMethod<SetProperties> {

	public abstract void setProperties(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException;
}
