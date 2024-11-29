package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;

public interface OwnerModified {

	void ownerModified(final GraphObject graphObject, final SecurityContext securityContext);
}
