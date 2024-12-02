package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.operations.LifecycleMethod;

public interface OnDeletion extends LifecycleMethod {

	void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException;
}
