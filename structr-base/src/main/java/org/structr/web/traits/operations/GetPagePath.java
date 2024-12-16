package org.structr.web.traits.operations;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;

public abstract class GetPagePath extends FrameworkMethod<GetPagePath> {

	public abstract String getPagePath(final NodeInterface node);
}
