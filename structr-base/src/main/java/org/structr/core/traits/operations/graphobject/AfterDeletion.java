package org.structr.core.traits.operations.graphobject;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyMap;

public interface AfterDeletion {

	void afterDeletion(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties);
}
