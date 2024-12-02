package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.LifecycleMethod;

public interface OnCreation extends LifecycleMethod {

	void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException;
}
