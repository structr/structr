package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;

public interface LocationModified {

	void locationModified(final GraphObject graphObject, final SecurityContext securityContext);
}
