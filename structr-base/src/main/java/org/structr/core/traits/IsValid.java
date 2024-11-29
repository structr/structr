package org.structr.core.traits;

import org.structr.common.error.ErrorBuffer;
import org.structr.core.GraphObject;

public interface IsValid {

	boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer);
}
