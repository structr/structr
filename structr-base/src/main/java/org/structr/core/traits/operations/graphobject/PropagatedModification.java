package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.LifecycleMethod;

public interface PropagatedModification extends LifecycleMethod {

	void propagatedModification(final GraphObject graphObject, final SecurityContext securityContext);
}
