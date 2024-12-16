package org.structr.web.traits.operations;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;

public abstract class RenderCustomAttributes extends FrameworkMethod<RenderCustomAttributes> {

	public abstract void renderCustomAttributes(final NodeInterface node, final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException;
}
