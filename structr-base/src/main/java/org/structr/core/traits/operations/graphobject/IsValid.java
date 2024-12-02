package org.structr.core.traits.operations.graphobject;

import org.structr.common.error.ErrorBuffer;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.LifecycleMethod;

public interface IsValid extends LifecycleMethod {

	Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer);
}
