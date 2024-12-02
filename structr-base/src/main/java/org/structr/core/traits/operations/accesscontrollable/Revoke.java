package org.structr.core.traits.operations.accesscontrollable;

import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;

import java.util.Set;

public abstract class Revoke extends FrameworkMethod<Revoke> {

	public abstract void revoke(final NodeInterface node, final Set<Permission> permissions, final Principal principal, final SecurityContext ctx) throws FrameworkException;
}
