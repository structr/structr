package org.structr.web.traits.operations;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;

public abstract class GetContextName extends FrameworkMethod<GetContextName> {

	public abstract String getContextName(final NodeInterface node);
}
