package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.LifecycleMethod;

public interface LocationModified extends LifecycleMethod {

	void locationModified(final GraphObject graphObject, final SecurityContext securityContext);
}
