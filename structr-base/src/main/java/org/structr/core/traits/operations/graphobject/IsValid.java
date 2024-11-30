package org.structr.core.traits.operations.graphobject;

import org.structr.common.error.ErrorBuffer;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.ComposableOperation;

public interface IsValid extends ComposableOperation {

	Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer);
}
