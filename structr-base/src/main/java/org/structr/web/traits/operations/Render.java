package org.structr.web.traits.operations;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.common.RenderContext;

public abstract class Render extends FrameworkMethod<Render> {

	public abstract void render(final NodeInterface node, final RenderContext renderContext, final int depth) throws FrameworkException;
}
