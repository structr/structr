package org.structr.core.traits.operations.nodeinterface;

import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.ComposableOperation;

@FunctionalInterface
public interface IsGranted extends ComposableOperation {

	boolean isGranted(final IsGranted superImpl, final GraphObject graphObject, final Permission permission, final SecurityContext securityContext, final boolean isCreation);
}
