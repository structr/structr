package org.structr.core.traits.operations.nodeinterface;

import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.traits.operations.OverwritableOperation;

public interface IsGranted extends OverwritableOperation {

	boolean isGranted(final GraphObject graphObject, final Permission permission, final SecurityContext securityContext, final boolean isCreation);
}
