package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;

public interface AfterCreation {

	void afterCreation(final GraphObject graphObject, final SecurityContext securityContext) throws FrameworkException;
}
