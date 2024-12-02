package org.structr.core.traits.operations.accesscontrollable;

import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;

public abstract class IsGranted extends FrameworkMethod {

	public abstract boolean isGranted(final NodeInterface graphObject, final Permission permission, final SecurityContext securityContext, final boolean isCreation);
}
