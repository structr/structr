package org.structr.web.traits.operations;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.common.RenderContext;

public abstract class RenderContent extends FrameworkMethod<RenderContent> {

	public abstract void renderContent(final NodeInterface node, final RenderContext renderContext, final int depth) throws FrameworkException;
}
