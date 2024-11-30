package org.structr.core.traits.operations.propertycontainer;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.operations.OverwritableOperation;

public interface SetProperties extends OverwritableOperation<SetProperties> {

	void setProperties(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException;
}
