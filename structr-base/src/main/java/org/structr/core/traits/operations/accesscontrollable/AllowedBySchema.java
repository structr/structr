package org.structr.core.traits.operations.accesscontrollable;

import org.structr.common.Permission;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;

public abstract class AllowedBySchema extends FrameworkMethod<AllowedBySchema> {

	public abstract boolean allowedBySchema(final NodeInterface node, final Principal principal, Permission permission);
}
