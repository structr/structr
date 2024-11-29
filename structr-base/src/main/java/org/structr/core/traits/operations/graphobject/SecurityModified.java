package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;

public interface SecurityModified {

	void securityModified(final GraphObject graphObject, final SecurityContext securityContext);
}
