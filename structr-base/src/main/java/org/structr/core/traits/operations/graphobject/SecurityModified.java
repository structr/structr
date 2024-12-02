package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.LifecycleMethod;

public interface SecurityModified extends LifecycleMethod {

	void securityModified(final GraphObject graphObject, final SecurityContext securityContext);
}
